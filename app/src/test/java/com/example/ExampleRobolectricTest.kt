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
}
