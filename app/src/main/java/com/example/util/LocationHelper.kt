package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.example.model.District
import com.example.model.MukomukoDistricts
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object LocationHelper {

    fun hasLocationPermission(context: Context): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineLocation || coarseLocation
    }

    fun isLocationServiceEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        return isGpsEnabled || isNetworkEnabled
    }

    /**
     * Calculates distance between two GPS coordinates in kilometers using Haversine formula.
     */
    fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Finds the nearest Mukomuko district given arbitrary latitude & longitude.
     */
    fun findNearestDistrict(latitude: Double, longitude: Double): Pair<District, Double> {
        var minDistance = Double.MAX_VALUE
        var closest = MukomukoDistricts.defaultDistrict

        for (district in MukomukoDistricts.list) {
            val distKm = calculateDistanceKm(latitude, longitude, district.latitude, district.longitude)
            if (distKm < minDistance) {
                minDistance = distKm
                closest = district
            }
        }
        return Pair(closest, minDistance)
    }

    /**
     * Attempts to acquire current user location using Google Play Services FusedLocationProviderClient.
     * Implements multi-tier fallback:
     * 1. High Accuracy fresh GPS fix via getCurrentLocation(PRIORITY_HIGH_ACCURACY)
     * 2. Cached FusedLocationProviderClient.lastLocation
     * 3. Balanced Power Accuracy via getCurrentLocation(PRIORITY_BALANCED_POWER_ACCURACY)
     * 4. System LocationManager (GPS_PROVIDER & NETWORK_PROVIDER)
     */
    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation(
        context: Context,
        onSuccess: (latitude: Double, longitude: Double, nearestDistrict: District, distanceKm: Double, accuracyMeters: Float?) -> Unit,
        onError: (message: String) -> Unit
    ) {
        if (!hasLocationPermission(context)) {
            onError("Izin lokasi belum diberikan. Mohon izinkan akses lokasi agar cuaca kecamatan Anda dapat dideteksi secara otomatis.")
            return
        }

        if (!isLocationServiceEnabled(context)) {
            onError("GPS / Layanan lokasi perangkat Anda sedang dinonaktifkan. Mohon aktifkan GPS di pengaturan perangkat.")
            return
        }

        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()

            // Step 1: Request fresh high accuracy location fix
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val (nearest, distKm) = findNearestDistrict(location.latitude, location.longitude)
                        val accuracy = if (location.hasAccuracy()) location.accuracy else null
                        onSuccess(location.latitude, location.longitude, nearest, distKm, accuracy)
                    } else {
                        // Step 2: Fallback to lastLocation if available
                        fusedClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                            if (lastLoc != null) {
                                val (nearest, distKm) = findNearestDistrict(lastLoc.latitude, lastLoc.longitude)
                                val accuracy = if (lastLoc.hasAccuracy()) lastLoc.accuracy else null
                                onSuccess(lastLoc.latitude, lastLoc.longitude, nearest, distKm, accuracy)
                            } else {
                                // Step 3: Try balanced power accuracy
                                tryBalancedFusedLocation(context, fusedClient, onSuccess, onError)
                            }
                        }.addOnFailureListener {
                            tryBalancedFusedLocation(context, fusedClient, onSuccess, onError)
                        }
                    }
                }
                .addOnFailureListener {
                    tryBalancedFusedLocation(context, fusedClient, onSuccess, onError)
                }
        } catch (e: Exception) {
            fallbackLocationManager(context, onSuccess, onError)
        }
    }

    @SuppressLint("MissingPermission")
    private fun tryBalancedFusedLocation(
        context: Context,
        fusedClient: com.google.android.gms.location.FusedLocationProviderClient,
        onSuccess: (latitude: Double, longitude: Double, nearestDistrict: District, distanceKm: Double, accuracyMeters: Float?) -> Unit,
        onError: (message: String) -> Unit
    ) {
        try {
            val cts = CancellationTokenSource()
            fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val (nearest, distKm) = findNearestDistrict(location.latitude, location.longitude)
                        val accuracy = if (location.hasAccuracy()) location.accuracy else null
                        onSuccess(location.latitude, location.longitude, nearest, distKm, accuracy)
                    } else {
                        fallbackLocationManager(context, onSuccess, onError)
                    }
                }
                .addOnFailureListener {
                    fallbackLocationManager(context, onSuccess, onError)
                }
        } catch (e: Exception) {
            fallbackLocationManager(context, onSuccess, onError)
        }
    }

    @SuppressLint("MissingPermission")
    private fun fallbackLocationManager(
        context: Context,
        onSuccess: (latitude: Double, longitude: Double, nearestDistrict: District, distanceKm: Double, accuracyMeters: Float?) -> Unit,
        onError: (message: String) -> Unit
    ) {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locationManager == null) {
                onError("Layanan lokasi sistem tidak tersedia pada perangkat.")
                return
            }

            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER
            )
            var bestLocation: Location? = null

            for (provider in providers) {
                if (locationManager.isProviderEnabled(provider)) {
                    val loc = locationManager.getLastKnownLocation(provider)
                    if (loc != null) {
                        if (bestLocation == null || (loc.hasAccuracy() && loc.accuracy < (bestLocation?.accuracy ?: Float.MAX_VALUE))) {
                            bestLocation = loc
                        }
                    }
                }
            }

            if (bestLocation != null) {
                val (nearest, distKm) = findNearestDistrict(bestLocation.latitude, bestLocation.longitude)
                val accuracy = if (bestLocation.hasAccuracy()) bestLocation.accuracy else null
                onSuccess(bestLocation.latitude, bestLocation.longitude, nearest, distKm, accuracy)
            } else {
                onError("Tidak dapat mendeteksi koordinat GPS saat ini. Pastikan GPS aktif dan berada di area dengan jangkauan sinyal yang cukup.")
            }
        } catch (e: Exception) {
            onError("Gagal mengambil lokasi GPS: ${e.localizedMessage ?: "Terjadi kesalahan"}")
        }
    }
}
