package com.example.data.sync

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.CitizenDisasterReportEntity
import com.example.model.CitizenDisasterReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

data class IntegrityCheckResult(
    val timestamp: Long = System.currentTimeMillis(),
    val localCount: Int,
    val remoteCount: Int,
    val localChecksum: String,
    val remoteChecksum: String,
    val isConsistent: Boolean,
    val discrepancies: List<String>,
    val resolvedDiscrepancyCount: Int
)

/**
 * Performs automated checksum comparison between local Room database
 * and Firestore for critical disaster reports, flagging and resolving any discrepancies
 * via SyncConflictHandler.
 */
object IntegrityCheckWorker {
    private const val TAG = "IntegrityCheckWorker"

    suspend fun performCheck(
        context: Context,
        database: AppDatabase
    ): IntegrityCheckResult = withContext(Dispatchers.IO) {
        val weatherDao = database.weatherDao()
        val conflictHandler = SyncConflictHandler(context, database)

        val localEntities = weatherDao.getAllCitizenDisasterReportsSync()
        val localChecksum = computeChecksum(
            localEntities.map {
                "${it.id}:${com.example.model.CitizenReportStatusHelper.normalizeStatus(it.status)}:${it.effectiveUpdatedAt()}"
            }
        )

        val remoteResult = com.example.data.local.FirestoreHelper.fetchCitizenReportsFromServerSuspend()
        val remoteReports = remoteResult.getOrElse { error ->
            Log.e(TAG, "Integrity check failed: ${error.message}")
            return@withContext IntegrityCheckResult(
                localCount = localEntities.size,
                remoteCount = 0,
                localChecksum = localChecksum,
                remoteChecksum = "OFFLINE_OR_ERROR",
                isConsistent = false,
                discrepancies = listOf("Gagal menghubungi Cloud Firestore: ${error.localizedMessage ?: "Periksa koneksi internet"}"),
                resolvedDiscrepancyCount = 0
            )
        }

        val remoteChecksum = computeChecksum(
            remoteReports.map {
                "${it.id}:${com.example.model.CitizenReportStatusHelper.normalizeStatus(it.status)}:${it.effectiveUpdatedAt()}"
            }
        )
        val discrepancies = mutableListOf<String>()
        var resolvedCount = 0

        val localMap = localEntities.associateBy { it.id }
        val remoteMap = remoteReports.associateBy { it.id }

        // 1. Reconcile reports from Cloud Firestore -> Local Room
        for (remote in remoteReports) {
            val local = localMap[remote.id]
            val localNorm = local?.let { com.example.model.CitizenReportStatusHelper.normalizeStatus(it.status) }
            val remoteNorm = com.example.model.CitizenReportStatusHelper.normalizeStatus(remote.status)

            if (local == null) {
                discrepancies.add("Laporan ${remote.id} ada di Cloud namun belum masuk di perangkat ini. Disinkronkan ke lokal.")
                val resolved = conflictHandler.resolveReportConflict(null, remote, "IntegrityCheck")
                weatherDao.insertCitizenDisasterReport(toEntity(resolved))
                resolvedCount++
            } else if (
                localNorm != remoteNorm ||
                local.effectiveUpdatedAt() != remote.effectiveUpdatedAt() ||
                local.additionalNotes != remote.additionalNotes ||
                local.handledByOfficerNip != remote.handledByOfficerNip
            ) {
                discrepancies.add("Perbedaan pada ${remote.id}: Lokal($localNorm) vs Cloud($remoteNorm). Diselaraskan otomatis.")
                val resolved = conflictHandler.resolveReportConflict(local, remote, "IntegrityCheck")
                weatherDao.insertCitizenDisasterReport(toEntity(resolved))
                if (resolved.effectiveUpdatedAt() > remote.effectiveUpdatedAt() || !resolved.isSyncedToCloud) {
                    val pushRes = com.example.data.local.FirestoreHelper.syncReportToCloudSuspend(resolved)
                    if (pushRes.isSuccess) {
                        weatherDao.updateCitizenReportSyncState(resolved.id, true)
                    }
                }
                resolvedCount++
            }
        }

        // 2. Push local-only reports missing in Cloud Firestore
        for (local in localEntities) {
            if (!remoteMap.containsKey(local.id)) {
                discrepancies.add("Laporan lokal ${local.id} belum terunggah ke Cloud. Mendorong ke Cloud Firestore.")
                val model = toModel(local)
                val pushRes = com.example.data.local.FirestoreHelper.syncReportToCloudSuspend(model)
                if (pushRes.isSuccess) {
                    weatherDao.updateCitizenReportSyncState(local.id, true)
                }
                resolvedCount++
            }
        }

        val updatedLocal = weatherDao.getAllCitizenDisasterReportsSync()
        val finalLocalChecksum = computeChecksum(
            updatedLocal.map {
                "${it.id}:${com.example.model.CitizenReportStatusHelper.normalizeStatus(it.status)}:${it.effectiveUpdatedAt()}"
            }
        )
        val isConsistent = discrepancies.isEmpty() || finalLocalChecksum == remoteChecksum
        Log.i(TAG, "Integrity check finished. Consistent: $isConsistent, Discrepancies: ${discrepancies.size}, Resolved: $resolvedCount")

        IntegrityCheckResult(
            localCount = updatedLocal.size,
            remoteCount = remoteReports.size,
            localChecksum = finalLocalChecksum,
            remoteChecksum = remoteChecksum,
            isConsistent = isConsistent,
            discrepancies = discrepancies,
            resolvedDiscrepancyCount = resolvedCount
        )
    }

    private fun computeChecksum(items: List<String>): String {
        val sorted = items.sorted().joinToString(";")
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(sorted.toByteArray(Charsets.UTF_8))
        return hash.take(8).joinToString("") { "%02x".format(it) }
    }

    private fun toEntity(r: CitizenDisasterReport) = CitizenDisasterReportEntity(
        id = r.id,
        disasterType = r.disasterType,
        incidentTime = r.incidentTime,
        incidentLocation = r.incidentLocation,
        district = r.district,
        village = r.village,
        latitude = r.latitude,
        longitude = r.longitude,
        gpsAccuracyMeters = r.gpsAccuracyMeters,
        isGpsVerified = r.isGpsVerified,
        casualtiesDead = r.casualtiesDead,
        casualtiesInjured = r.casualtiesInjured,
        casualtiesMissing = r.casualtiesMissing,
        photoUri = r.photoUri,
        reporterName = r.reporterName,
        reporterPhone = r.reporterPhone,
        additionalNotes = r.additionalNotes,
        timestamp = r.timestamp,
        status = com.example.model.CitizenReportStatusHelper.normalizeStatus(r.status),
        updatedAt = r.effectiveUpdatedAt(),
        handledByOfficerName = r.handledByOfficerName,
        handledByOfficerNip = r.handledByOfficerNip,
        handledByOfficerRole = r.handledByOfficerRole,
        isSyncedToCloud = r.isSyncedToCloud
    )

    private fun toModel(e: CitizenDisasterReportEntity) = CitizenDisasterReport(
        id = e.id,
        disasterType = e.disasterType,
        incidentTime = e.incidentTime,
        incidentLocation = e.incidentLocation,
        district = e.district,
        village = e.village,
        latitude = e.latitude,
        longitude = e.longitude,
        gpsAccuracyMeters = e.gpsAccuracyMeters,
        isGpsVerified = e.isGpsVerified,
        casualtiesDead = e.casualtiesDead,
        casualtiesInjured = e.casualtiesInjured,
        casualtiesMissing = e.casualtiesMissing,
        photoUri = e.photoUri,
        reporterName = e.reporterName,
        reporterPhone = e.reporterPhone,
        additionalNotes = e.additionalNotes,
        timestamp = e.timestamp,
        status = com.example.model.CitizenReportStatusHelper.normalizeStatus(e.status),
        updatedAt = e.effectiveUpdatedAt(),
        handledByOfficerName = e.handledByOfficerName,
        handledByOfficerNip = e.handledByOfficerNip,
        handledByOfficerRole = e.handledByOfficerRole,
        isSyncedToCloud = e.isSyncedToCloud
    )
}

