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
            FirebaseFirestore.getInstance()
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
}
