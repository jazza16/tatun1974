package com.example.data.sync

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.CitizenDisasterReportEntity
import com.example.model.CitizenDisasterReport
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
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
        val firestore = try { FirebaseFirestore.getInstance() } catch (_: Exception) { null }

        val localEntities = weatherDao.getAllCitizenDisasterReportsSync()
        val localChecksum = computeChecksum(localEntities.map { "${it.id}:${it.status}:${it.timestamp}" })

        if (firestore == null) {
            return@withContext IntegrityCheckResult(
                localCount = localEntities.size,
                remoteCount = 0,
                localChecksum = localChecksum,
                remoteChecksum = "OFFLINE_NO_FIRESTORE",
                isConsistent = true,
                discrepancies = listOf("Firestore offline / tidak dapat diakses untuk pembanding."),
                resolvedDiscrepancyCount = 0
            )
        }

        try {
            val snapshot = firestore.collection("citizen_disaster_reports").get().await()
            val remoteReports = mutableListOf<CitizenDisasterReport>()
            for (doc in snapshot.documents) {
                val id = doc.getString("id") ?: doc.id
                remoteReports.add(
                    CitizenDisasterReport(
                        id = id,
                        disasterType = doc.getString("disasterType") ?: "Kejadian",
                        incidentTime = doc.getString("incidentTime") ?: "",
                        incidentLocation = doc.getString("incidentLocation") ?: "",
                        district = doc.getString("district") ?: "Mukomuko",
                        village = doc.getString("village") ?: "",
                        latitude = doc.getDouble("latitude") ?: -2.58,
                        longitude = doc.getDouble("longitude") ?: 101.12,
                        gpsAccuracyMeters = doc.getDouble("gpsAccuracyMeters")?.toFloat(),
                        isGpsVerified = doc.getBoolean("isGpsVerified") ?: true,
                        casualtiesDead = doc.getLong("casualtiesDead")?.toInt() ?: 0,
                        casualtiesInjured = doc.getLong("casualtiesInjured")?.toInt() ?: 0,
                        casualtiesMissing = doc.getLong("casualtiesMissing")?.toInt() ?: 0,
                        photoUri = doc.getString("photoUri"),
                        reporterName = doc.getString("reporterName") ?: "Warga",
                        reporterPhone = doc.getString("reporterPhone") ?: "",
                        additionalNotes = doc.getString("additionalNotes") ?: "",
                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                        status = doc.getString("status") ?: "Menunggu Verifikasi"
                    )
                )
            }

            val remoteChecksum = computeChecksum(remoteReports.map { "${it.id}:${it.status}:${it.timestamp}" })
            val discrepancies = mutableListOf<String>()
            var resolvedCount = 0

            val localMap = localEntities.associateBy { it.id }
            val remoteMap = remoteReports.associateBy { it.id }

            // Check for missing in local or status differences
            for (remote in remoteReports) {
                val local = localMap[remote.id]
                if (local == null) {
                    discrepancies.add("Laporan ${remote.id} ada di cloud namun hilang di lokal Room. Memulihkan ke lokal.")
                    val resolved = conflictHandler.resolveReportConflict(null, remote, "IntegrityCheck")
                    weatherDao.insertCitizenDisasterReport(toEntity(resolved))
                    resolvedCount++
                } else if (local.status != remote.status || local.timestamp != remote.timestamp) {
                    discrepancies.add("Discrepancy pada ${remote.id}: Local(${local.status}, ${local.timestamp}) vs Remote(${remote.status}, ${remote.timestamp})")
                    val resolved = conflictHandler.resolveReportConflict(local, remote, "IntegrityCheck")
                    weatherDao.insertCitizenDisasterReport(toEntity(resolved))
                    resolvedCount++
                }
            }

            // Check for reports existing locally but missing in Firestore
            for (local in localEntities) {
                if (!remoteMap.containsKey(local.id)) {
                    discrepancies.add("Laporan lokal ${local.id} belum terunggah ke cloud. Mendorong ke cloud.")
                    val model = toModel(local)
                    com.example.data.local.FirestoreHelper.syncReportToCloud(model)
                    resolvedCount++
                }
            }

            val isConsistent = localChecksum == remoteChecksum && discrepancies.isEmpty()
            Log.i(TAG, "Integrity check finished. Consistent: $isConsistent, Discrepancies: ${discrepancies.size}, Resolved: $resolvedCount")

            IntegrityCheckResult(
                localCount = localEntities.size,
                remoteCount = remoteReports.size,
                localChecksum = localChecksum,
                remoteChecksum = remoteChecksum,
                isConsistent = isConsistent,
                discrepancies = discrepancies,
                resolvedDiscrepancyCount = resolvedCount
            )
        } catch (e: Exception) {
            Log.e(TAG, "Integrity check failed: ${e.message}")
            IntegrityCheckResult(
                localCount = localEntities.size,
                remoteCount = 0,
                localChecksum = localChecksum,
                remoteChecksum = "ERROR: ${e.message}",
                isConsistent = false,
                discrepancies = listOf("Gagal melakukan pengecekan integritas cloud: ${e.localizedMessage}"),
                resolvedDiscrepancyCount = 0
            )
        }
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
        status = r.status
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
        status = e.status
    )
}
