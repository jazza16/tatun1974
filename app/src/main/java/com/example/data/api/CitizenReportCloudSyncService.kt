package com.example.data.api

import android.util.Log
import com.example.model.CitizenDisasterReport

/**
 * Legacy HTTP cloud-object sync stub.
 * Unauthenticated third-party REST synchronization has been disabled for zero-trust security;
 * multi-device citizen disaster report synchronization is handled via authenticated Cloud Firestore.
 */
object CitizenReportCloudSyncService {
    private const val TAG = "CitizenReportSync"

    suspend fun fetchReportsFromCloud(): List<CitizenDisasterReport>? {
        Log.d(TAG, "Public REST report sync disabled; relying on authenticated Cloud Firestore.")
        return null
    }

    suspend fun pushReportsToCloud(reports: List<CitizenDisasterReport>): Boolean {
        Log.d(TAG, "Public REST report push skipped (${reports.size} local records); Firestore handles sync.")
        return true
    }
}
