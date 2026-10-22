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
     * Attempts to acquire current location using FusedLocationProviderClient with LocationManager fallback.
     */
    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation(
        context: Context,
        onSuccess: (latitude: Double, longitude: Double, nearestDistrict: District, distanceKm: Double) -> Unit,
        onError: (message: String) -> Unit
    ) {
        if (!hasLocationPermission(context)) {
            onError("Izin lokasi belum diberikan. Mohon izinkan akses lokasi.")
            return
        }

        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()

            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val (nearest, distKm) = findNearestDistrict(location.latitude, location.longitude)
                        onSuccess(location.latitude, location.longitude, nearest, distKm)
                    } else {
                        // Fallback to last location
                        fusedClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                            if (lastLoc != null) {
                                val (nearest, distKm) = findNearestDistrict(lastLoc.latitude, lastLoc.longitude)
                                onSuccess(lastLoc.latitude, lastLoc.longitude, nearest, distKm)
                            } else {
                                fallbackLocationManager(context, onSuccess, onError)
                            }
                        }.addOnFailureListener {
                            fallbackLocationManager(context, onSuccess, onError)
                        }
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
        onSuccess: (latitude: Double, longitude: Double, nearestDistrict: District, distanceKm: Double) -> Unit,
        onError: (message: String) -> Unit
    ) {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locationManager == null) {
                onError("Layanan lokasi sistem tidak tersedia.")
                return
            }

            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            var bestLocation: Location? = null

            for (provider in providers) {
                if (locationManager.isProviderEnabled(provider)) {
                    val loc = locationManager.getLastKnownLocation(provider)
                    if (loc != null) {
                        if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                            bestLocation = loc
                        }
                    }
                }
            }

            if (bestLocation != null) {
                val (nearest, distKm) = findNearestDistrict(bestLocation.latitude, bestLocation.longitude)
                onSuccess(bestLocation.latitude, bestLocation.longitude, nearest, distKm)
            } else {
                onError("Tidak dapat mendeteksi koordinat GPS saat ini. Pastikan GPS aktif.")
            }
        } catch (e: Exception) {
            onError("Gagal mengambil lokasi: ${e.localizedMessage ?: "Terjadi kesalahan"}")
        }
    }
}
