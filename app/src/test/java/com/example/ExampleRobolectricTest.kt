package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.MukomukoDistricts
import com.example.model.WeatherCodeHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Cuaca Mukomuko", appName)
  }

  @Test
  fun `verify mukomuko 15 subdistricts exist`() {
    assertEquals(15, MukomukoDistricts.list.size)
    val kota = MukomukoDistricts.list.find { it.id == "kota_mukomuko" }
    assertNotNull(kota)
    assertEquals("Kota Mukomuko", kota?.name)
  }

  @Test
  fun `verify weather code decoding`() {
    val info = WeatherCodeHelper.getWeatherInfo(0)
    assertEquals("Cerah", info.conditionText)
    val rain = WeatherCodeHelper.getWeatherInfo(65)
    assertTrue(rain.conditionText.contains("Hujan"))
  }

  @Test
  fun `verify bmkg earthquake notification channel initialization`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.util.EarthquakeNotificationHelper.initNotificationChannel(context)
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    val channel = notificationManager.getNotificationChannel(com.example.util.EarthquakeNotificationHelper.CHANNEL_ID)
    assertNotNull(channel)
    assertEquals("Peringatan Dini Gempa Bumi BMKG", channel.name)
  }

  @Test
  fun `verify bmkg earthquake distance and proximity calculation`() {
    val raw = com.example.model.BmkgGempaRaw(
      tanggal = "16 Okt 2026",
      jam = "12:00:00 WIB",
      coordinates = "-2.60,101.10",
      magnitude = "5.4",
      kedalaman = "10 km",
      wilayah = "45 km BaratDaya MUKOMUKO-BENGKULU",
      potensi = "Tidak berpotensi tsunami"
    )
    val eq = com.example.model.BmkgEarthquakeMapper.fromRaw(raw)
    assertTrue(eq.isNearMukomuko)
    assertTrue(eq.distanceKmToMukomuko < 100)
    assertEquals(com.example.model.EarthquakeUrgency.CRITICAL, eq.urgencyLevel)
  }

  @Test
  fun `verify marine wave history data points`() {
    val data7Days = com.example.ui.components.MockMukomukoWaveHistory.getHistory7Days()
    assertEquals(7, data7Days.size)
    val maxWave = data7Days.maxOf { it.waveHeight }
    assertTrue(maxWave > 2.0f)
  }

  @Test
  fun `verify persistent app update preferences and single prompt per release`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = com.example.data.local.AppUpdatePreferences(context)

    // Installed version is persisted
    assertNotNull(prefs.getInstalledVersion())
    assertTrue(prefs.isUpdatePermanentlyInstalled())

    // Simulating earlier installed version
    prefs.simulateEarlierInstalledVersion("1.0.0")
    assertEquals("1.0.0", prefs.getInstalledVersion())
    org.junit.Assert.assertFalse(prefs.isUpdatePermanentlyInstalled())

    // Testing dialog dismissal for version
    prefs.setDialogDismissedForVersion("1.3.0")
    assertTrue(prefs.isDialogDismissedForVersion("1.3.0"))

    // Testing permanent installation of latest version
    prefs.setUpdateInstalledPermanently("1.3.0")
    assertEquals("1.3.0", prefs.getInstalledVersion())
    assertTrue(prefs.isUpdatePermanentlyInstalled())
  }

  @Test
  fun `verify compass cardinal direction conversion and manual heading`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val compass = com.example.util.CompassSensorManager(context)

    val north = com.example.util.CompassSensorManager.getCardinal(0f)
    assertEquals("Utara", north.first)
    assertEquals("U", north.second)

    val east = com.example.util.CompassSensorManager.getCardinal(90f)
    assertEquals("Timur", east.first)
    assertEquals("T", east.second)

    val south = com.example.util.CompassSensorManager.getCardinal(180f)
    assertEquals("Selatan", south.first)
    assertEquals("S", south.second)

    val west = com.example.util.CompassSensorManager.getCardinal(270f)
    assertEquals("Barat", west.first)
    assertEquals("B", west.second)

    val northEast = com.example.util.CompassSensorManager.getCardinal(45f)
    assertEquals("Timur Laut", northEast.first)
    assertEquals("TL", northEast.second)

    compass.setManualHeading(45f)
    assertEquals(45f, compass.compassState.value.azimuthDegrees)
    assertEquals("Timur Laut", compass.compassState.value.cardinalDirection)
  }

  @Test
  fun `verify onboarding completion persistence`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = com.example.data.local.AppUpdatePreferences(context)

    prefs.resetOnboarding()
    org.junit.Assert.assertFalse(prefs.isOnboardingCompleted())

    prefs.setOnboardingCompleted(true)
    assertTrue(prefs.isOnboardingCompleted())
  }

  @Test
  fun `verify mukomuko temperature overlay thermal data and categories`() {
    val thermalData = com.example.ui.components.DistrictThermalData(
      district = MukomukoDistricts.list.first { it.id == "kota_mukomuko" },
      temperature = 31.8,
      feelsLike = 34.5,
      humidity = 78,
      windSpeed = 14.0,
      weatherDesc = "Cerah Berawan",
      isLiveBmkg = true
    )

    assertEquals("Panas Tropis Normal", thermalData.heatIndexCategory)
    assertTrue(thermalData.recommendation.contains("pesisir"))

    val hotThermalData = thermalData.copy(temperature = 33.5)
    assertEquals("Sangat Panas (Waspada Terik)", hotThermalData.heatIndexCategory)

    val coolThermalData = thermalData.copy(temperature = 26.5)
    assertEquals("Sejuk Segar Pegunungan", coolThermalData.heatIndexCategory)
  }

  @Test
  fun `verify wrs ntwc basemap types exist`() {
    val basemaps = com.example.ui.components.WrsBasemapType.values()
    assertEquals(3, basemaps.size)
    assertEquals("Streets", com.example.ui.components.WrsBasemapType.STREETS.displayName)
    assertEquals("Satelit", com.example.ui.components.WrsBasemapType.SATELLITE.displayName)
    assertEquals("Grayscale", com.example.ui.components.WrsBasemapType.GRAYSCALE.displayName)
  }
}
