package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Universal Google Maps Integration Helper.
 * Handles opening locations, route directions, navigation, and satellite views
 * directly in the Google Maps Android application with seamless fallback to Google Maps Web.
 */
object GoogleMapsHelper {

    /**
     * Opens Google Maps centered on the specified coordinates with an incident/location pin.
     */
    fun openLocation(context: Context, latitude: Double, longitude: Double, label: String = "Lokasi Kejadian Mukomuko") {
        val geoUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(label)})")
        val webFallback = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
        launchMapsIntent(context, geoUri, webFallback)
    }

    /**
     * Opens Google Maps Driving Directions / Navigation from current location to destination.
     */
    fun openNavigationRoute(context: Context, destLat: Double, destLon: Double, destName: String = "Titik Evakuasi Mukomuko") {
        val navUri = Uri.parse("google.navigation:q=$destLat,$destLon&mode=d")
        val dirUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$destLat,$destLon&travelmode=driving")
        launchMapsIntent(context, navUri, dirUri)
    }

    /**
     * Opens Google Maps showing all disaster & evacuation zones in Mukomuko Regency.
     */
    fun openMukomukoDisasterMap(context: Context) {
        val geoUri = Uri.parse("geo:-2.5833,101.1167?q=-2.5833,101.1167(${Uri.encode("Pusdalops BPBD Kabupaten Mukomuko")})")
        val webFallback = Uri.parse("https://www.google.com/maps/search/?api=1&query=-2.5833,101.1167")
        launchMapsIntent(context, geoUri, webFallback)
    }

    /**
     * Opens Google Maps for an earthquake epicenter with depth & magnitude info.
     */
    fun openEarthquakeEpicenter(context: Context, latitude: Double, longitude: Double, magnitude: String, wilayah: String) {
        val label = "Gempa M $magnitude - $wilayah"
        val geoUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(label)})")
        val webFallback = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
        launchMapsIntent(context, geoUri, webFallback)
    }

    private fun launchMapsIntent(context: Context, primaryUri: Uri, fallbackWebUri: Uri) {
        val mapsIntent = Intent(Intent.ACTION_VIEW, primaryUri).apply {
            setPackage("com.google.android.apps.maps")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(mapsIntent)
        } catch (_: Exception) {
            try {
                val genericGeoIntent = Intent(Intent.ACTION_VIEW, primaryUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(genericGeoIntent)
            } catch (_: Exception) {
                try {
                    val webIntent = Intent(Intent.ACTION_VIEW, fallbackWebUri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(webIntent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Tidak dapat membuka Google Maps: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
