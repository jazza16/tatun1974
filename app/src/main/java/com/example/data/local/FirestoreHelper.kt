package com.example.data.local

import android.util.Log
import com.example.model.CitizenDisasterReport
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions

/**
 * Cloud Firestore Helper for real-time bidirectional synchronization
 * of Personnel (Officers) and Citizen Disaster Report statuses across all devices.
 */
object FirestoreHelper {
    private const val TAG = "FirestoreHelper"
    private const val COLLECTION_OFFICERS = "officers"
    private const val COLLECTION_CITIZEN_REPORTS = "citizen_disaster_reports"

    private val firestore: FirebaseFirestore? by lazy {
        try {
            val dbId = "ai-studio-android-cuacamuk-a1e6cb6f-11e7-41ba-90f4-82c9adb67961"
            try {
                FirebaseFirestore.getInstance(dbId)
            } catch (_: Exception) {
                FirebaseFirestore.getInstance()
            }
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseFirestore instance unavailable, operating in local offline mode: ${e.message}")
            null
        }
    }

    /**
     * Real-time listener for BPBD Officer / Personnel roster.
     * Fires whenever any device updates, inserts, or removes an officer.
     */
    fun listenOfficersRealtime(
        onUpdate: (List<OfficerProfile>) -> Unit,
        onError: ((Exception) -> Unit)? = null
    ): ListenerRegistration? {
        val db = firestore ?: return null
        return try {
            db.collection(COLLECTION_OFFICERS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Error listening to officers snapshot", error)
                        onError?.invoke(error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null && !snapshot.isEmpty) {
                        val list = mutableListOf<OfficerProfile>()
                        for (doc in snapshot.documents) {
                            val id = doc.getString("id") ?: doc.id
                            val fullName = doc.getString("fullName") ?: continue
                            val nip = doc.getString("nip") ?: ""
                            val role = doc.getString("role") ?: OfficerRoles.PETUGAS_JAGA
                            val unit = doc.getString("unit") ?: "Pusdalops BPBD Kab. Mukomuko"
                            val shift = doc.getString("shift") ?: "Shift Siaga 24 Jam"
                            val phone = doc.getString("phone") ?: ""
                            list.add(
                                OfficerProfile(
                                    id = id,
                                    fullName = fullName,
                                    nip = nip,
                                    role = role,
                                    unit = unit,
                                    shift = shift,
                                    phone = phone
                                )
                            )
                        }
                        onUpdate(list)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach officers listener", e)
            null
        }
    }

    /**
     * Pushes officer profile data to Firestore so other devices immediately sync.
     */
    fun syncOfficerToCloud(officer: OfficerProfile) {
        val db = firestore ?: return
        try {
            val docId = if (officer.id.isNotBlank()) officer.id else officer.nip
            val data = mapOf(
                "id" to officer.id,
                "fullName" to officer.fullName,
                "nip" to officer.nip,
                "role" to officer.role,
                "unit" to officer.unit,
                "shift" to officer.shift,
                "phone" to officer.phone,
                "updatedAt" to System.currentTimeMillis()
            )
            db.collection(COLLECTION_OFFICERS)
                .document(docId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Officer successfully synced to Firestore: ${officer.fullName}")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to sync officer to Firestore", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during officer cloud sync", e)
        }
    }

    /**
     * Removes officer profile from Firestore.
     */
    fun deleteOfficerFromCloud(officerId: String) {
        val db = firestore ?: return
        try {
            db.collection(COLLECTION_OFFICERS)
                .document(officerId)
                .delete()
                .addOnSuccessListener {
                    Log.d(TAG, "Officer $officerId deleted from Firestore")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to delete officer from Firestore", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during officer cloud delete", e)
        }
    }

    /**
     * Real-time listener for Citizen Disaster Reports.
     * Fires whenever any device posts a new report or changes a report's status.
     */
    fun listenCitizenReportsRealtime(
        onUpdate: (List<CitizenDisasterReport>) -> Unit,
        onError: ((Exception) -> Unit)? = null
    ): ListenerRegistration? {
        val db = firestore ?: return null
        return try {
            db.collection(COLLECTION_CITIZEN_REPORTS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Error listening to citizen reports snapshot", error)
                        onError?.invoke(error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val reports = mutableListOf<CitizenDisasterReport>()
                        for (doc in snapshot.documents) {
                            val id = doc.getString("id") ?: doc.id
                            val disasterType = doc.getString("disasterType") ?: "Lainnya"
                            val incidentTime = doc.getString("incidentTime") ?: ""
                            val incidentLocation = doc.getString("incidentLocation") ?: ""
                            val district = doc.getString("district") ?: "Mukomuko"
                            val village = doc.getString("village") ?: ""
                            val latitude = doc.getDouble("latitude") ?: -2.58
                            val longitude = doc.getDouble("longitude") ?: 101.12
                            val gpsAccuracy = doc.getDouble("gpsAccuracyMeters")?.toFloat()
                            val isGpsVerified = doc.getBoolean("isGpsVerified") ?: true
                            val dead = doc.getLong("casualtiesDead")?.toInt() ?: 0
                            val injured = doc.getLong("casualtiesInjured")?.toInt() ?: 0
                            val missing = doc.getLong("casualtiesMissing")?.toInt() ?: 0
                            val photoUri = doc.getString("photoUri")
                            val reporterName = doc.getString("reporterName") ?: "Warga"
                            val reporterPhone = doc.getString("reporterPhone") ?: ""
                            val notes = doc.getString("additionalNotes") ?: ""
                            val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                            val status = doc.getString("status") ?: "Menunggu Verifikasi"

                            reports.add(
                                CitizenDisasterReport(
                                    id = id,
                                    disasterType = disasterType,
                                    incidentTime = incidentTime,
                                    incidentLocation = incidentLocation,
                                    district = district,
                                    village = village,
                                    latitude = latitude,
                                    longitude = longitude,
                                    gpsAccuracyMeters = gpsAccuracy,
                                    isGpsVerified = isGpsVerified,
                                    casualtiesDead = dead,
                                    casualtiesInjured = injured,
                                    casualtiesMissing = missing,
                                    photoUri = photoUri,
                                    reporterName = reporterName,
                                    reporterPhone = reporterPhone,
                                    additionalNotes = notes,
                                    timestamp = timestamp,
                                    status = status
                                )
                            )
                        }
                        if (reports.isNotEmpty()) {
                            onUpdate(reports.sortedByDescending { it.timestamp })
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach citizen reports listener", e)
            null
        }
    }

    /**
     * Uploads or updates a citizen disaster report in Firestore.
     */
    fun syncReportToCloud(report: CitizenDisasterReport) {
        val db = firestore ?: return
        try {
            val data = mapOf(
                "id" to report.id,
                "disasterType" to report.disasterType,
                "incidentTime" to report.incidentTime,
                "incidentLocation" to report.incidentLocation,
                "district" to report.district,
                "village" to report.village,
                "latitude" to report.latitude,
                "longitude" to report.longitude,
                "gpsAccuracyMeters" to report.gpsAccuracyMeters,
                "isGpsVerified" to report.isGpsVerified,
                "casualtiesDead" to report.casualtiesDead,
                "casualtiesInjured" to report.casualtiesInjured,
                "casualtiesMissing" to report.casualtiesMissing,
                "photoUri" to report.photoUri,
                "reporterName" to report.reporterName,
                "reporterPhone" to report.reporterPhone,
                "additionalNotes" to report.additionalNotes,
                "timestamp" to report.timestamp,
                "status" to report.status
            )
            db.collection(COLLECTION_CITIZEN_REPORTS)
                .document(report.id)
                .set(data, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Report ${report.id} synced to Firestore")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to sync report to Firestore", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during report cloud sync", e)
        }
    }

    /**
     * Updates only the report status and optional response notes in Firestore.
     */
    fun updateReportStatusInCloud(reportId: String, newStatus: String, notes: String? = null) {
        val db = firestore ?: return
        try {
            val updateMap = mutableMapOf<String, Any>(
                "status" to newStatus,
                "updatedAt" to System.currentTimeMillis()
            )
            if (notes != null) {
                updateMap["additionalNotes"] = notes
            }
            db.collection(COLLECTION_CITIZEN_REPORTS)
                .document(reportId)
                .update(updateMap)
                .addOnSuccessListener {
                    Log.d(TAG, "Report status updated to $newStatus in Firestore")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to update report status in Firestore", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during status cloud update", e)
        }
    }

    private const val COLLECTION_BACKUPS = "database_backups"

    /**
     * Backs up the entire disaster report database to a secure cloud Firestore collection
     * to prevent data loss during major app updates or device replacements.
     */
    fun backupReportsToCloud(
        reports: List<CitizenDisasterReport>,
        officerName: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        val db = firestore
        if (db == null) {
            onComplete(false, "Firestore tidak tersedia. Pastikan koneksi internet aktif.")
            return
        }

        try {
            val backupId = "backup_reports_${System.currentTimeMillis()}"
            val serializedReports = reports.map { r ->
                mapOf(
                    "id" to r.id,
                    "disasterType" to r.disasterType,
                    "incidentTime" to r.incidentTime,
                    "incidentLocation" to r.incidentLocation,
                    "district" to r.district,
                    "village" to r.village,
                    "latitude" to r.latitude,
                    "longitude" to r.longitude,
                    "gpsAccuracyMeters" to r.gpsAccuracyMeters,
                    "isGpsVerified" to r.isGpsVerified,
                    "casualtiesDead" to r.casualtiesDead,
                    "casualtiesInjured" to r.casualtiesInjured,
                    "casualtiesMissing" to r.casualtiesMissing,
                    "photoUri" to r.photoUri,
                    "reporterName" to r.reporterName,
                    "reporterPhone" to r.reporterPhone,
                    "additionalNotes" to r.additionalNotes,
                    "timestamp" to r.timestamp,
                    "status" to r.status
                )
            }

            val backupPayload = mapOf(
                "backupId" to backupId,
                "backupDate" to System.currentTimeMillis(),
                "officerName" to officerName,
                "totalReports" to reports.size,
                "reports" to serializedReports
            )

            db.collection(COLLECTION_BACKUPS)
                .document(backupId)
                .set(backupPayload)
                .addOnSuccessListener {
                    // Also save as 'latest_backup' for immediate 1-click restore
                    db.collection(COLLECTION_BACKUPS)
                        .document("latest_backup")
                        .set(backupPayload)
                    onComplete(true, "Berhasil mencadangkan ${reports.size} laporan ke cloud ($backupId).")
                }
                .addOnFailureListener { e ->
                    onComplete(false, "Gagal mencadangkan ke cloud: ${e.localizedMessage}")
                }
        } catch (e: Exception) {
            onComplete(false, "Terjadi kesalahan: ${e.localizedMessage}")
        }
    }

    /**
     * Restores the latest disaster report database backup from cloud Firestore.
     */
    fun restoreReportsFromCloud(
        onComplete: (Boolean, List<CitizenDisasterReport>, String) -> Unit
    ) {
        val db = firestore
        if (db == null) {
            onComplete(false, emptyList(), "Firestore tidak tersedia.")
            return
        }

        try {
            db.collection(COLLECTION_BACKUPS)
                .document("latest_backup")
                .get()
                .addOnSuccessListener { doc ->
                    if (doc != null && doc.exists()) {
                        @Suppress("UNCHECKED_CAST")
                        val rawList = doc.get("reports") as? List<Map<String, Any?>> ?: emptyList()
                        val restoredReports = rawList.mapNotNull { map ->
                            val id = map["id"] as? String ?: return@mapNotNull null
                            val disasterType = map["disasterType"] as? String ?: "Lainnya"
                            val incidentTime = map["incidentTime"] as? String ?: ""
                            val incidentLocation = map["incidentLocation"] as? String ?: ""
                            val district = map["district"] as? String ?: "Mukomuko"
                            val village = map["village"] as? String ?: ""
                            val latitude = (map["latitude"] as? Number)?.toDouble() ?: -2.58
                            val longitude = (map["longitude"] as? Number)?.toDouble() ?: 101.12
                            val gpsAccuracy = (map["gpsAccuracyMeters"] as? Number)?.toFloat()
                            val isGps = map["isGpsVerified"] as? Boolean ?: true
                            val dead = (map["casualtiesDead"] as? Number)?.toInt() ?: 0
                            val injured = (map["casualtiesInjured"] as? Number)?.toInt() ?: 0
                            val missing = (map["casualtiesMissing"] as? Number)?.toInt() ?: 0
                            val photoUri = map["photoUri"] as? String
                            val repName = map["reporterName"] as? String ?: "Warga"
                            val repPhone = map["reporterPhone"] as? String ?: ""
                            val notes = map["additionalNotes"] as? String ?: ""
                            val timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                            val status = map["status"] as? String ?: "Menunggu Verifikasi"

                            CitizenDisasterReport(
                                id = id,
                                disasterType = disasterType,
                                incidentTime = incidentTime,
                                incidentLocation = incidentLocation,
                                district = district,
                                village = village,
                                latitude = latitude,
                                longitude = longitude,
                                gpsAccuracyMeters = gpsAccuracy,
                                isGpsVerified = isGpsVerified(isGps),
                                casualtiesDead = dead,
                                casualtiesInjured = injured,
                                casualtiesMissing = missing,
                                photoUri = photoUri,
                                reporterName = repName,
                                reporterPhone = repPhone,
                                additionalNotes = notes,
                                timestamp = timestamp,
                                status = status
                            )
                        }
                        val officerName = doc.getString("officerName") ?: "Pusdalops"
                        onComplete(true, restoredReports, "Berhasil memulihkan ${restoredReports.size} laporan dari cadangan cloud oleh $officerName.")
                    } else {
                        onComplete(false, emptyList(), "Tidak ditemukan berkas cadangan di cloud.")
                    }
                }
                .addOnFailureListener { e ->
                    onComplete(false, emptyList(), "Gagal mengunduh cadangan: ${e.localizedMessage}")
                }
        } catch (e: Exception) {
            onComplete(false, emptyList(), "Terjadi kesalahan: ${e.localizedMessage}")
        }
    }

    private fun isGpsVerified(value: Boolean): Boolean = value
}
