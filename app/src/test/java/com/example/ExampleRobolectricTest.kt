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
  fun `verify weather alert notification channel initialization`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.util.WeatherNotificationHelper.initNotificationChannel(context)
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
    val channel = notificationManager.getNotificationChannel(com.example.util.WeatherNotificationHelper.CHANNEL_ID)
    assertNotNull(channel)
    assertEquals("Peringatan Cuaca Buruk Mukomuko", channel.name)
    assertEquals(com.example.util.WeatherNotificationHelper.ORANGE_NOTIFICATION_COLOR, channel.lightColor)
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
}
