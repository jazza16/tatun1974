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
        if (local == null) {
            // No local conflict, accept incoming remote
            return@withContext remote
        }

        val localTime = local.timestamp
        val remoteTime = remote.timestamp

        // Case 1: Identical status and same or older local, accept remote cleanly
        if (local.status == remote.status && localTime <= remoteTime) {
            return@withContext remote
        }

        // Case 2: Status conflict or timestamp discrepancy
        val conflictType = if (local.status != remote.status) {
            CONFLICT_CONCURRENT_STATUS
        } else {
            CONFLICT_TIMESTAMP_DRIFT
        }

        val resolvedReport: CitizenDisasterReport
        val resolution: String
        val details: String

        if (remoteTime > localTime) {
            // Remote is newer. If local has officer triage notes that remote lacks, merge them!
            val mergedNotes = when {
                local.additionalNotes.isNotBlank() && !remote.additionalNotes.contains(local.additionalNotes) -> {
                    if (remote.additionalNotes.isBlank()) local.additionalNotes
                    else "${remote.additionalNotes} | [Catatan Petugas Lokal: ${local.additionalNotes}]"
                }
                else -> remote.additionalNotes
            }

            resolvedReport = remote.copy(additionalNotes = mergedNotes)
            resolution = if (mergedNotes != remote.additionalNotes) RESOLUTION_MERGED_NOTES else RESOLUTION_REMOTE_ACCEPTED
            details = "Server lebih baru (Local: ${local.status} @ $localTime, Remote: ${remote.status} @ $remoteTime). Catatan digabungkan."
        } else if (localTime > remoteTime) {
            // Local is newer (e.g. officer locally updated report status during momentary network disconnect)
            resolvedReport = CitizenDisasterReport(
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
                photoUri = local.photoUri,
                reporterName = local.reporterName,
                reporterPhone = local.reporterPhone,
                additionalNotes = local.additionalNotes,
                timestamp = local.timestamp,
                status = local.status
            )
            resolution = RESOLUTION_LOCAL_PRESERVED
            details = "Data lokal lebih baru (Local: ${local.status} @ $localTime, Remote: ${remote.status} @ $remoteTime). Mempertahankan lokal dan sinkron ulang ke cloud."
        } else {
            // Same timestamp but conflicting status - prioritize progressed triage status
            val resolvedStatus = prioritizeStatus(local.status, remote.status)
            resolvedReport = remote.copy(status = resolvedStatus)
            resolution = "STATUS_PRIORITY_MERGED"
            details = "Waktu sama namun status berbeda. Dipilih status progres tertinggi: $resolvedStatus."
        }

        // Log conflict resolution event into Room database
        try {
            conflictLogDao.insertConflictLog(
                SyncConflictLogEntity(
                    reportId = remote.id,
                    conflictType = conflictType,
                    resolution = resolution,
                    localTimestamp = localTime,
                    remoteTimestamp = remoteTime,
                    localStatus = local.status,
                    remoteStatus = remote.status,
                    details = details,
                    officerBadge = currentOfficerBadge,
                    timestamp = System.currentTimeMillis()
                )
            )
            Log.i(TAG, "Sync Conflict recorded for ${remote.id}: $resolution ($details)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist conflict log: ${e.message}")
        }

        return@withContext resolvedReport
    }

    private fun prioritizeStatus(localStatus: String, remoteStatus: String): String {
        val hierarchy = listOf(
            "Menunggu Verifikasi",
            "Terverifikasi GPS",
            "Diterima PUSDALOPS",
            "Tim TRC Meluncur",
            "Dalam Penanganan",
            "Selesai Ditangani"
        )
        val localIdx = hierarchy.indexOf(localStatus)
        val remoteIdx = hierarchy.indexOf(remoteStatus)
        return if (localIdx >= remoteIdx) localStatus else remoteStatus
    }

    /**
     * Exports all conflict logs to a formatted CSV string for admin audit and troubleshooting.
     */
    suspend fun exportConflictLogsToCsv(): String = withContext(Dispatchers.IO) {
        val logs = conflictLogDao.getAllConflictLogsList()
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("ID,Waktu Event,ID Laporan,Tipe Konflik,Resolusi,Status Lokal,Status Server,Petugas,Keterangan\n")
        for (log in logs) {
            val dateStr = sdf.format(Date(log.timestamp))
            val cleanDetails = log.details.replace(",", ";").replace("\n", " ")
            sb.append("${log.id},\"$dateStr\",\"${log.reportId}\",\"${log.conflictType}\",\"${log.resolution}\",\"${log.localStatus}\",\"${log.remoteStatus}\",\"${log.officerBadge}\",\"$cleanDetails\"\n")
        }
        sb.toString()
    }
}
