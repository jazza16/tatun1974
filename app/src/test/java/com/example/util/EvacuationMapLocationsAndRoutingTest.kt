package com.example.util

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.model.MukomukoDistricts
import com.example.ui.MukomukoMapMarkersCatalog
import com.example.ui.components.DisasterCategory
import com.example.ui.components.MukomukoDisasterMapCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EvacuationMapLocationsAndRoutingTest {

    // Diagonal coastline and mountain corridor bounds for Kabupaten Mukomuko road network.
    // West of oceanBoundaryLon(lat) is Indian Ocean water; east of tnksBoundaryLon(lat) is roadless TNKS jungle.
    private fun oceanBoundaryLon(lat: Double): Double = 101.02 + ((-lat - 2.48) * 0.82)
    private fun tnksBoundaryLon(lat: Double): Double = 101.28 + ((-lat - 2.48) * 0.82)

    private fun assertVerifiedMukomukoRoadCoordinate(label: String, lat: Double, lon: Double) {
        assertTrue("$label must pass GoogleMapsHelper.isValidCoordinate", GoogleMapsHelper.isValidCoordinate(lat, lon))
        assertTrue(
            "$label lat=$lat must be within Kabupaten Mukomuko [-3.12..-2.42]",
            lat in -3.12..-2.42
        )
        assertTrue(
            "$label lon=$lon must be within Kabupaten Mukomuko [101.07..101.65]",
            lon in 101.07..101.65
        )
        val minLandLon = oceanBoundaryLon(lat)
        assertTrue(
            "$label ($lat, $lon) falls into Indian Ocean (lon must be >= %.4f)".format(Locale.US, minLandLon),
            lon >= minLandLon
        )
        val maxRoadCorridorLon = tnksBoundaryLon(lat)
        assertTrue(
            "$label ($lat, $lon) falls deep into roadless TNKS mountains (lon must be <= %.4f)".format(Locale.US, maxRoadCorridorLon),
            lon <= maxRoadCorridorLon
        )
    }

    @Test
    fun allDisasterMapPointsAndEvacuationTargetsAreOnVerifiedMukomukoRoads() {
        val points = MukomukoDisasterMapCatalog.mapPoints
        assertEquals("Expected 8 disaster map points", 8, points.size)

        points.forEach { point ->
            assertTrue("${point.id} roadReference must not be blank", point.roadReference.isNotBlank())
            assertVerifiedMukomukoRoadCoordinate("DisasterMapPoint ${point.id} (${point.name})", point.latitude, point.longitude)

            // Verify proximity to its declared nearestDistrict
            val district = MukomukoDistricts.list.firstOrNull {
                it.name.equals(point.nearestDistrict, ignoreCase = true)
            }
            assertNotNull("${point.id} nearestDistrict '${point.nearestDistrict}' must exist in MukomukoDistricts", district)
            val distKm = MukomukoDisasterMapCatalog.haversineKm(
                point.latitude, point.longitude,
                district!!.latitude, district.longitude
            )
            assertTrue(
                "${point.id} (${point.name}) is ${"%.2f".format(Locale.US, distKm)} km from ${district.name} (max 15 km)",
                distKm <= 15.0
            )

            // All danger zones (TSUNAMI / FLOOD_LANDSLIDE) must have a verified safe evacuation target
            if (point.category == DisasterCategory.TSUNAMI || point.category == DisasterCategory.FLOOD_LANDSLIDE) {
                assertNotNull("${point.id} danger zone must define evacuationTargetName", point.evacuationTargetName)
                assertNotNull("${point.id} danger zone must define evacuationTargetLat", point.evacuationTargetLat)
                assertNotNull("${point.id} danger zone must define evacuationTargetLon", point.evacuationTargetLon)
                assertVerifiedMukomukoRoadCoordinate(
                    "EvacuationTarget for ${point.id} (${point.evacuationTargetName})",
                    point.evacuationTargetLat!!,
                    point.evacuationTargetLon!!
                )
            }
        }
    }

    @Test
    fun allEvacuationRoutesHaveVerifiedStartAndDestinationCoordinates() {
        val routes = MukomukoDisasterMapCatalog.evacuationRoutes
        assertEquals("Expected 4 evacuation routes", 4, routes.size)

        routes.forEach { route ->
            assertTrue("${route.id} roadCorridor must not be blank", route.roadCorridor.isNotBlank())
            assertVerifiedMukomukoRoadCoordinate("RouteStart ${route.id} (${route.name})", route.startLat, route.startLon)
            assertVerifiedMukomukoRoadCoordinate("RouteEnd ${route.id} (${route.destinationName})", route.endLat, route.endLon)
            val straightLineKm = MukomukoDisasterMapCatalog.haversineKm(
                route.startLat, route.startLon,
                route.endLat, route.endLon
            )
            assertTrue("${route.id} start and destination must be distinct (> 0.5 km)", straightLineKm > 0.5)
        }
    }

    @Test
    fun all18MapActivityMarkersAnd15DistrictsAreOnVerifiedMukomukoLand() {
        val districts = MukomukoDistricts.list
        assertEquals("Expected 15 Mukomuko districts", 15, districts.size)
        districts.forEach { d ->
            assertVerifiedMukomukoRoadCoordinate("District ${d.id} (${d.name})", d.latitude, d.longitude)
        }

        val markers = MukomukoMapMarkersCatalog.allMarkers
        assertEquals("Expected 18 MapActivity markers", 18, markers.size)
        markers.forEach { marker ->
            assertVerifiedMukomukoRoadCoordinate("MapMarker ${marker.id} (${marker.name})", marker.latitude, marker.longitude)
            val district = districts.firstOrNull { it.name.equals(marker.subDistrict, ignoreCase = true) }
            assertNotNull("${marker.id} subDistrict '${marker.subDistrict}' must exist", district)
            val distKm = MukomukoDisasterMapCatalog.haversineKm(
                marker.latitude, marker.longitude,
                district!!.latitude, district.longitude
            )
            assertTrue(
                "${marker.id} (${marker.name}) is ${"%.2f".format(Locale.US, distKm)} km from ${district.name} (max 15 km)",
                distKm <= 15.0
            )
        }
    }

    @Test
    fun googleMapsHelperUsesLocaleUsDotDecimalsEvenOnIndonesianLocale() {
        val previousLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale("in", "ID"))

            val dirUri = GoogleMapsHelper.buildDirectionsUri(-3.0033, 101.4988)
            assertEquals(
                "https://www.google.com/maps/dir/?api=1&destination=-3.003300,101.498800&travelmode=driving",
                dirUri.toString()
            )
            // Only the separator comma between lat and lon should exist
            assertEquals(1, dirUri.getQueryParameter("destination")!!.count { it == ',' })

            val navUri = GoogleMapsHelper.buildTurnByTurnNavigationUri(-3.0033, 101.4988)
            assertEquals("google.navigation:q=-3.003300,101.498800", navUri.toString())

            val geoUri = GoogleMapsHelper.buildLocationGeoUri(-2.5565, 101.1145, "Pusdalops BPBD")
            assertTrue(geoUri.toString().startsWith("geo:-2.556500,101.114500?q=-2.556500,101.114500("))
        } finally {
            Locale.setDefault(previousLocale)
        }
    }

    @Test
    fun everyRecommendedLocationLaunchesValidGoogleMapsDirectionsIntent() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val shadowApp = Shadows.shadowOf(app)

        // 1. All 8 InteractiveDisasterMapSection points + their safe evacuation targets
        MukomukoDisasterMapCatalog.mapPoints.forEach { point ->
            GoogleMapsHelper.openNavigationRoute(app, point.latitude, point.longitude, point.name)
            val intent = shadowApp.nextStartedActivity
            assertNotNull("Intent must launch for ${point.id} (${point.name})", intent)
            assertEquals(Intent.ACTION_VIEW, intent.action)
            assertEquals(GoogleMapsHelper.GOOGLE_MAPS_PACKAGE, intent.`package`)
            assertEquals(
                GoogleMapsHelper.buildDirectionsUri(point.latitude, point.longitude).toString(),
                intent.data.toString()
            )

            if (point.evacuationTargetLat != null && point.evacuationTargetLon != null) {
                GoogleMapsHelper.openNavigationRoute(
                    app,
                    point.evacuationTargetLat,
                    point.evacuationTargetLon,
                    point.evacuationTargetName ?: point.name
                )
                val evacIntent = shadowApp.nextStartedActivity
                assertNotNull("Evacuation intent must launch for ${point.id}", evacIntent)
                assertEquals(
                    GoogleMapsHelper.buildDirectionsUri(point.evacuationTargetLat, point.evacuationTargetLon).toString(),
                    evacIntent.data.toString()
                )
            }
        }

        // 2. All 4 Evacuation Routes
        MukomukoDisasterMapCatalog.evacuationRoutes.forEach { route ->
            GoogleMapsHelper.openNavigationRoute(app, route.endLat, route.endLon, route.destinationName)
            val intent = shadowApp.nextStartedActivity
            assertNotNull("Intent must launch for route ${route.id}", intent)
            assertEquals(
                GoogleMapsHelper.buildDirectionsUri(route.endLat, route.endLon).toString(),
                intent.data.toString()
            )
        }

        // 3. All 18 MapActivity Markers
        MukomukoMapMarkersCatalog.allMarkers.forEach { marker ->
            GoogleMapsHelper.openNavigationRoute(app, marker.latitude, marker.longitude, marker.name)
            val intent = shadowApp.nextStartedActivity
            assertNotNull("Intent must launch for marker ${marker.id}", intent)
            assertEquals(
                GoogleMapsHelper.buildDirectionsUri(marker.latitude, marker.longitude).toString(),
                intent.data.toString()
            )
        }
    }

    @Test
    fun nearestPointLookupAndInvalidCoordinateGuardWorkReliably() {
        val airRami = MukomukoDistricts.list.first { it.id == "air_rami" }
        val nearestToAirRami = MukomukoDisasterMapCatalog.findNearestPointForDistrict(airRami)
        assertEquals("p5", nearestToAirRami.id)

        val penarik = MukomukoDistricts.list.first { it.id == "penarik" }
        val nearestToPenarik = MukomukoDisasterMapCatalog.findNearestPointForDistrict(penarik)
        assertEquals("p3", nearestToPenarik.id)

        assertFalse(GoogleMapsHelper.isValidCoordinate(0.0, 0.0))
        assertFalse(GoogleMapsHelper.isValidCoordinate(Double.NaN, 101.1))
        assertFalse(GoogleMapsHelper.isValidCoordinate(-95.0, 101.1))
        assertFalse(GoogleMapsHelper.isValidCoordinate(-2.5, 190.0))

        val app = ApplicationProvider.getApplicationContext<Application>()
        val shadowApp = Shadows.shadowOf(app)
        GoogleMapsHelper.openNavigationRoute(app, 0.0, 0.0, "Invalid Point")
        assertNull("Invalid coordinate must not launch Maps intent", shadowApp.nextStartedActivity)
    }
}
