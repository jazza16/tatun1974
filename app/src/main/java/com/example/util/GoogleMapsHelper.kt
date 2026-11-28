package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.util.Locale

/**
 * Universal Google Maps Integration Helper.
 * Handles opening locations, route directions, turn-by-turn navigation, and earthquake epicenters
 * using locale-safe US decimal coordinates and a resilient multi-stage fallback chain so
 * emergency routing never fails silently on citizen or officer devices.
 */
object GoogleMapsHelper {

    const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"

    // Verified road-accessible Pusdalops-PB BPBD Kabupaten Mukomuko (Jl. Imam Bonjol, Bandar Ratu)
    const val PUSDALOPS_MUKOMUKO_LAT = -2.5565
    const val PUSDALOPS_MUKOMUKO_LON = 101.1145

    private val ALLOWED_TRAVEL_MODES = setOf("driving", "walking", "bicycling", "transit", "two-wheeler")

    /**
     * Validates that coordinates are finite, non-zero, and within WGS84 geographic bounds.
     */
    fun isValidCoordinate(latitude: Double, longitude: Double): Boolean {
        if (!latitude.isFinite() || !longitude.isFinite()) return false
        if (latitude == 0.0 && longitude == 0.0) return false
        return latitude in -90.0..90.0 && longitude in -180.0..180.0
    }

    /**
     * Formats coordinates strictly with Locale.US decimal dots (never commas on Indonesian locale).
     */
    fun formatCoordinate(value: Double): String = String.format(Locale.US, "%.6f", value)

    /**
     * Builds the official Google Maps cross-platform Directions URL.
     * Opening this in Google Maps displays the full route preview, ETA, Car/Motor/Walk mode
     * switcher, and snaps cleanly to the nearest accessible road even before GPS locks.
     */
    fun buildDirectionsUri(
        destLat: Double,
        destLon: Double,
        travelMode: String = "driving"
    ): Uri {
        val safeMode = travelMode.lowercase(Locale.US).takeIf { it in ALLOWED_TRAVEL_MODES } ?: "driving"
        val latStr = formatCoordinate(destLat)
        val lonStr = formatCoordinate(destLon)
        return Uri.parse(
            "https://www.google.com/maps/dir/?api=1&destination=$latStr,$lonStr&travelmode=$safeMode"
        )
    }

    /**
     * Builds direct Google Maps turn-by-turn navigation URI (fallback when Directions URL cannot open).
     */
    fun buildTurnByTurnNavigationUri(destLat: Double, destLon: Double): Uri {
        val latStr = formatCoordinate(destLat)
        val lonStr = formatCoordinate(destLon)
        return Uri.parse("google.navigation:q=$latStr,$lonStr")
    }

    /**
     * Builds a standard geo: URI with human-readable destination label for any installed maps app.
     */
    fun buildLocationGeoUri(latitude: Double, longitude: Double, label: String): Uri {
        val latStr = formatCoordinate(latitude)
        val lonStr = formatCoordinate(longitude)
        val safeLabel = label.trim().ifEmpty { "Lokasi Mukomuko" }
        return Uri.parse("geo:$latStr,$lonStr?q=$latStr,$lonStr(${Uri.encode(safeLabel)})")
    }

    /**
     * Builds a Google Maps web search fallback URL.
     */
    fun buildWebSearchUri(latitude: Double, longitude: Double): Uri {
        val latStr = formatCoordinate(latitude)
        val lonStr = formatCoordinate(longitude)
        return Uri.parse("https://www.google.com/maps/search/?api=1&query=$latStr,$lonStr")
    }

    /**
     * Opens Google Maps centered on the specified coordinates with an incident/location pin.
     */
    fun openLocation(
        context: Context,
        latitude: Double,
        longitude: Double,
        label: String = "Lokasi Kejadian Mukomuko"
    ) {
        if (!isValidCoordinate(latitude, longitude)) {
            Toast.makeText(context, "Koordinat lokasi tidak valid untuk ditampilkan di peta.", Toast.LENGTH_SHORT).show()
            return
        }
        val geoUri = buildLocationGeoUri(latitude, longitude, label)
        val webFallback = buildWebSearchUri(latitude, longitude)
        launchIntentChain(
            context = context,
            intents = listOf(
                Intent(Intent.ACTION_VIEW, geoUri).apply {
                    setPackage(GOOGLE_MAPS_PACKAGE)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                },
                Intent(Intent.ACTION_VIEW, geoUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                },
                Intent(Intent.ACTION_VIEW, webFallback).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        )
    }

    /**
     * Opens Google Maps Directions from current location to verified destination coordinates.
     * Uses Universal Directions URL in Google Maps app first (reliable route preview + road snap
     * + Car/Motor/Walking selector), then falls back to turn-by-turn, geo: pin, and web browser.
     */
    fun openNavigationRoute(
        context: Context,
        destLat: Double,
        destLon: Double,
        destName: String = "Titik Evakuasi Mukomuko",
        travelMode: String = "driving"
    ) {
        if (!isValidCoordinate(destLat, destLon)) {
            Toast.makeText(context, "Koordinat tujuan belum valid untuk penunjuk arah.", Toast.LENGTH_SHORT).show()
            return
        }
        val directionsUri = buildDirectionsUri(destLat, destLon, travelMode)
        val turnByTurnUri = buildTurnByTurnNavigationUri(destLat, destLon)
        val geoFallbackUri = buildLocationGeoUri(destLat, destLon, destName)

        launchIntentChain(
            context = context,
            intents = listOf(
                // 1. Primary: Google Maps app Directions Preview (works reliably across all roads & travel modes)
                Intent(Intent.ACTION_VIEW, directionsUri).apply {
                    setPackage(GOOGLE_MAPS_PACKAGE)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                },
                // 2. Fallback: Direct Google Maps turn-by-turn navigation
                Intent(Intent.ACTION_VIEW, turnByTurnUri).apply {
                    setPackage(GOOGLE_MAPS_PACKAGE)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                },
                // 3. Fallback: Any installed maps app with labeled destination pin
                Intent(Intent.ACTION_VIEW, geoFallbackUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                },
                // 4. Final fallback: Browser Google Maps Directions
                Intent(Intent.ACTION_VIEW, directionsUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            )
        )
    }

    /**
     * Opens Google Maps centered on Pusdalops-PB BPBD Kabupaten Mukomuko.
     */
    fun openMukomukoDisasterMap(context: Context) {
        openLocation(
            context = context,
            latitude = PUSDALOPS_MUKOMUKO_LAT,
            longitude = PUSDALOPS_MUKOMUKO_LON,
            label = "Pusdalops-PB BPBD Kabupaten Mukomuko"
        )
    }

    /**
     * Opens Google Maps for an earthquake epicenter with depth & magnitude info.
     */
    fun openEarthquakeEpicenter(
        context: Context,
        latitude: Double,
        longitude: Double,
        magnitude: String,
        wilayah: String
    ) {
        val label = "Gempa M $magnitude - $wilayah"
        openLocation(context, latitude, longitude, label)
    }

    private fun launchIntentChain(context: Context, intents: List<Intent>) {
        var lastError: Exception? = null
        for (intent in intents) {
            try {
                context.startActivity(intent)
                return
            } catch (e: Exception) {
                lastError = e
            }
        }
        Toast.makeText(
            context,
            "Tidak dapat membuka Google Maps: ${lastError?.message ?: "Aplikasi peta tidak tersedia"}",
            Toast.LENGTH_SHORT
        ).show()
    }
}
