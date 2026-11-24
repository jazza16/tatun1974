package com.example.data.sync

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.CitizenDisasterReportEntity
import com.example.data.local.SyncConflictLogEntity
import com.example.model.CitizenDisasterReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * SyncConflictHandler manages automated resolution of data conflicts
 * between the local Room database and Firestore across all 5 field test devices.
 * Uses timestamps and officer triage notes as the primary source of truth,
 * and records every resolution event into the Room database and CSV log.
 */
class SyncConflictHandler(
    private val context: Context,
    private val database: AppDatabase
) {
    private val conflictLogDao = database.syncConflictLogDao()

    companion object {
        private const val TAG = "SyncConflictHandler"

        const val CONFLICT_TIMESTAMP_DRIFT = "TIMESTAMP_VERSION_DRIFT"
        const val CONFLICT_CONCURRENT_STATUS = "CONCURRENT_STATUS_UPDATE"
        const val CONFLICT_OFFLINE_MERGE = "OFFLINE_LOCAL_OVERWRITE"

        const val RESOLUTION_REMOTE_ACCEPTED = "REMOTE_ACCEPTED_NEWER"
        const val RESOLUTION_LOCAL_PRESERVED = "LOCAL_PRESERVED_NEWER"
        const val RESOLUTION_MERGED_NOTES = "MERGED_WITH_OFFICER_NOTES"
    }

    /**
     * Reconciles a local report with a newly incoming remote report from Firestore.
     * Returns the reconciled report to be stored in the local Room database,
     * or null if the local report is strictly newer and should overwrite Firestore.
     */
    suspend fun resolveReportConflict(
        local: CitizenDisasterReportEntity?,
        remote: CitizenDisasterReport,
        currentOfficerBadge: String = "Pusdalops"
    ): CitizenDisasterReport = withContext(Dispatchers.IO) {
        val normalizedRemoteStatus = com.example.model.CitizenReportStatusHelper.normalizeStatus(remote.status)
        val normalizedRemote = remote.copy(
            status = normalizedRemoteStatus,
            updatedAt = remote.effectiveUpdatedAt(),
            isSyncedToCloud = true
        )

        if (local == null) {
            return@withContext normalizedRemote
        }

        val normalizedLocalStatus = com.example.model.CitizenReportStatusHelper.normalizeStatus(local.status)
        val localEffectiveTime = local.effectiveUpdatedAt()
        val remoteEffectiveTime = normalizedRemote.effectiveUpdatedAt()

        // Case 1: Identical state (status, notes, officer) -> report is already confirmed in Firestore; always mark Synced!
        if (normalizedLocalStatus == normalizedRemoteStatus &&
            local.additionalNotes.trim() == normalizedRemote.additionalNotes.trim() &&
            local.handledByOfficerNip == normalizedRemote.handledByOfficerNip
        ) {
            return@withContext normalizedRemote.copy(
                photoUri = com.example.util.ImageUtils.selectBestPhotoUri(normalizedRemote.photoUri, local.photoUri),
                updatedAt = maxOf(localEffectiveTime, remoteEffectiveTime),
                isSyncedToCloud = true
            )
        }

        // Case 2: Remote update is strictly newer (e.g., active officer validated/completed/added follow-up notes)
        if (remoteEffectiveTime > localEffectiveTime) {
            val resolvedReport = normalizedRemote.copy(
                photoUri = com.example.util.ImageUtils.selectBestPhotoUri(normalizedRemote.photoUri, local.photoUri),
                handledByOfficerName = normalizedRemote.handledByOfficerName.ifBlank { local.handledByOfficerName },
                handledByOfficerNip = normalizedRemote.handledByOfficerNip.ifBlank { local.handledByOfficerNip },
                handledByOfficerRole = normalizedRemote.handledByOfficerRole.ifBlank { local.handledByOfficerRole },
                isSyncedToCloud = true
            )
            if (normalizedLocalStatus != normalizedRemoteStatus && !local.isSyncedToCloud) {
                logConflict(
                    reportId = normalizedRemote.id,
                    conflictType = CONFLICT_CONCURRENT_STATUS,
                    resolution = RESOLUTION_REMOTE_ACCEPTED,
                    localTime = localEffectiveTime,
                    remoteTime = remoteEffectiveTime,
                    localStatus = normalizedLocalStatus,
                    remoteStatus = normalizedRemoteStatus,
                    details = "Pembaruan server lebih baru (Local: $normalizedLocalStatus @ $localEffectiveTime, Remote: $normalizedRemoteStatus @ $remoteEffectiveTime). Menerima status petugas terbaru.",
                    officerBadge = currentOfficerBadge
                )
            }
            return@withContext resolvedReport
        }

        // Case 3: Local update is strictly newer AND unsynced (e.g. officer updated status locally while offline)
        if (localEffectiveTime > remoteEffectiveTime) {
            val resolvedReport = CitizenDisasterReport(
                id = local.id,
                disasterType = local.disasterType,
                incidentTime = local.incidentTime,
                incidentLocation = local.incidentLocation,
                district = local.district,
                village = local.village,
                latitude = local.latitude,
                longitude = local.longitude,
                gpsAccuracyMeters = local.gpsAccuracyMeters,
                isGpsVerified = local.isGpsVerified,
                casualtiesDead = local.casualtiesDead,
                casualtiesInjured = local.casualtiesInjured,
                casualtiesMissing = local.casualtiesMissing,
                photoUri = com.example.util.ImageUtils.selectBestPhotoUri(local.photoUri, normalizedRemote.photoUri),
                reporterName = local.reporterName,
                reporterPhone = local.reporterPhone,
                additionalNotes = local.additionalNotes,
                timestamp = local.timestamp,
                status = normalizedLocalStatus,
                updatedAt = localEffectiveTime,
                handledByOfficerName = local.handledByOfficerName.ifBlank { normalizedRemote.handledByOfficerName },
                handledByOfficerNip = local.handledByOfficerNip.ifBlank { normalizedRemote.handledByOfficerNip },
                handledByOfficerRole = local.handledByOfficerRole.ifBlank { normalizedRemote.handledByOfficerRole },
                isSyncedToCloud = local.isSyncedToCloud
            )
            logConflict(
                reportId = normalizedRemote.id,
                conflictType = if (normalizedLocalStatus != normalizedRemoteStatus) CONFLICT_CONCURRENT_STATUS else CONFLICT_TIMESTAMP_DRIFT,
                resolution = RESOLUTION_LOCAL_PRESERVED,
                localTime = localEffectiveTime,
                remoteTime = remoteEffectiveTime,
                localStatus = normalizedLocalStatus,
                remoteStatus = normalizedRemoteStatus,
                details = "Pembaruan lokal lebih baru (Local: $normalizedLocalStatus @ $localEffectiveTime, Remote: $normalizedRemoteStatus @ $remoteEffectiveTime). Mempertahankan lokal untuk disinkronkan ke cloud.",
                officerBadge = currentOfficerBadge
            )
            return@withContext resolvedReport
        }

        // Case 4: Exact same effective timestamp -> prefer higher canonical triage status; on tie prefer remote server truth
        val resolvedStatus = prioritizeStatus(normalizedLocalStatus, normalizedRemoteStatus)
        val useRemoteNotes = normalizedRemote.additionalNotes.isNotBlank() &&
            (resolvedStatus == normalizedRemoteStatus || local.additionalNotes.isBlank())
        val resolvedReport = normalizedRemote.copy(
            status = resolvedStatus,
            photoUri = com.example.util.ImageUtils.selectBestPhotoUri(normalizedRemote.photoUri, local.photoUri),
            additionalNotes = if (useRemoteNotes) normalizedRemote.additionalNotes else local.additionalNotes,
            handledByOfficerName = normalizedRemote.handledByOfficerName.ifBlank { local.handledByOfficerName },
            handledByOfficerNip = normalizedRemote.handledByOfficerNip.ifBlank { local.handledByOfficerNip },
            handledByOfficerRole = normalizedRemote.handledByOfficerRole.ifBlank { local.handledByOfficerRole },
            updatedAt = maxOf(localEffectiveTime, remoteEffectiveTime),
            isSyncedToCloud = true
        )
        if (normalizedLocalStatus != normalizedRemoteStatus) {
            logConflict(
                reportId = normalizedRemote.id,
                conflictType = CONFLICT_CONCURRENT_STATUS,
                resolution = "STATUS_PRIORITY_MERGED",
                localTime = localEffectiveTime,
                remoteTime = remoteEffectiveTime,
                localStatus = normalizedLocalStatus,
                remoteStatus = normalizedRemoteStatus,
                details = "Waktu pembaruan sama namun status berbeda (Local: $normalizedLocalStatus vs Remote: $normalizedRemoteStatus). Dipilih status progres tertinggi: $resolvedStatus.",
                officerBadge = currentOfficerBadge
            )
        }
        return@withContext resolvedReport
    }

    private suspend fun logConflict(
        reportId: String,
        conflictType: String,
        resolution: String,
        localTime: Long,
        remoteTime: Long,
        localStatus: String,
        remoteStatus: String,
        details: String,
        officerBadge: String
    ) {
        try {
            conflictLogDao.insertConflictLog(
                SyncConflictLogEntity(
                    reportId = reportId,
                    conflictType = conflictType,
                    resolution = resolution,
                    localTimestamp = localTime,
                    remoteTimestamp = remoteTime,
                    localStatus = localStatus,
                    remoteStatus = remoteStatus,
                    details = details,
                    officerBadge = officerBadge,
                    timestamp = System.currentTimeMillis()
                )
            )
            Log.i(TAG, "Sync Conflict recorded for $reportId: $resolution ($details)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist conflict log: ${e.message}")
        }
    }

    internal fun prioritizeStatus(localStatus: String, remoteStatus: String): String {
        val localCanonical = com.example.model.CitizenReportStatusHelper.normalizeStatus(localStatus)
        val remoteCanonical = com.example.model.CitizenReportStatusHelper.normalizeStatus(remoteStatus)
        val localRank = com.example.model.CitizenReportStatusHelper.statusRank(localCanonical)
        val remoteRank = com.example.model.CitizenReportStatusHelper.statusRank(remoteCanonical)
        // Prefer higher triage progress; on equal rank prefer remote server truth
        return if (remoteRank >= localRank) remoteCanonical else localCanonical
    }

    /**
     * Exports all conflict logs to a formatted CSV string for admin audit and troubleshooting.
     */
    suspend fun exportConflictLogsToCsv(): String = withContext(Dispatchers.IO) {
        val logs = conflictLogDao.getAllConflictLogsList()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder()
        val settings = com.example.data.repository.DocumentSettingsRepository.getCachedOrDefaultSettings(context)
        com.example.util.ReportExporter.buildStandardKopLines(settings).forEach { line ->
            sb.append("\"$line\"\n")
        }
        sb.append("\"DOKUMEN RESMI: LOG AUDIT KONFLIK SINKRONISASI DATA SIPAGAR KITA\"\n\n")
        sb.append("ID,Waktu Event,ID Laporan,Tipe Konflik,Resolusi,Status Lokal,Status Server,Petugas,Keterangan\n")
        for (log in logs) {
            val dateStr = sdf.format(Date(log.timestamp))
            val cleanDetails = log.details.replace(",", ";").replace("\n", " ")
            sb.append("${log.id},\"$dateStr\",\"${log.reportId}\",\"${log.conflictType}\",\"${log.resolution}\",\"${log.localStatus}\",\"${log.remoteStatus}\",\"${log.officerBadge}\",\"$cleanDetails\"\n")
        }
        sb.toString()
    }
}
