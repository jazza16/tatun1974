package com.example.util

import com.example.model.MukomukoDistricts
import com.example.ui.MukomukoMapMarkersCatalog
import com.example.ui.components.MukomukoDisasterMapCatalog
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MukomukoGeofenceValidatorTest {

    @Test
    fun all15MukomukoDistrictsAndAllVerifiedMapPointsAreAcceptedInsideCoverage() {
        MukomukoDistricts.list.forEach { d ->
            val check = MukomukoGeofenceValidator.evaluate(d.latitude, d.longitude)
            assertTrue(
                "District ${d.name} (${d.latitude}, ${d.longitude}) must be inside Mukomuko coverage",
                check.isInsideMukomuko
            )
            assertTrue("Accepted location must have empty rejectionMessage", check.rejectionMessage.isBlank())
        }

        MukomukoDisasterMapCatalog.mapPoints.forEach { p ->
            assertTrue(
                "DisasterMapPoint ${p.name} (${p.latitude}, ${p.longitude}) must be inside coverage",
                MukomukoGeofenceValidator.isInsideMukomukoCoverage(p.latitude, p.longitude)
            )
        }

        MukomukoMapMarkersCatalog.allMarkers.forEach { m ->
            assertTrue(
                "MapMarker ${m.name} (${m.latitude}, ${m.longitude}) must be inside coverage",
                MukomukoGeofenceValidator.isInsideMukomukoCoverage(m.latitude, m.longitude)
            )
        }
    }

    @Test
    fun outsideKabupatenMukomukoLocationsAreRejectedWithClearToastMessage() {
        val outsideLocations = listOf(
            "Kota Bengkulu" to (-3.7928 to 102.2608),
            "Arga Makmur (Bengkulu Utara)" to (-3.4400 to 102.1800),
            "Painan (Pesisir Selatan Sumbar)" to (-1.3500 to 100.5700),
            "Kota Padang (Sumbar)" to (-0.9471 to 100.4172),
            "Sungai Penuh / Kerinci (Jambi)" to (-2.0600 to 101.3900),
            "Jakarta" to (-6.2088 to 106.8456),
            "Emulator Default (Mountain View)" to (37.4220 to -122.0840),
            "Samudra Hindia Lepas Pantai" to (-3.0500 to 101.1500),
            "Luar Batas Timur Jambi" to (-2.3500 to 101.8000),
            "Null Island (0,0)" to (0.0 to 0.0)
        )

        outsideLocations.forEach { (label, coords) ->
            val check = MukomukoGeofenceValidator.evaluate(coords.first, coords.second)
            assertFalse("$label (${coords.first}, ${coords.second}) MUST be rejected outside Mukomuko", check.isInsideMukomuko)
            assertTrue(
                "$label must return Indonesian rejection Toast message mentioning Kabupaten Mukomuko",
                check.rejectionMessage.contains("Kabupaten Mukomuko", ignoreCase = true) &&
                    check.rejectionMessage.contains("ditolak", ignoreCase = true)
            )
        }
    }
}
