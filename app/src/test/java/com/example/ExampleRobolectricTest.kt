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
    assertEquals("SIPAGAR KITA", appName)
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
    prefs.setDialogDismissedForVersion("1.5.0")
    assertTrue(prefs.isDialogDismissedForVersion("1.5.0"))

    // Testing permanent installation of latest version
    val latestVer = com.example.data.local.AppUpdatePreferences.DEFAULT_LATEST_VERSION
    prefs.setUpdateInstalledPermanently(latestVer)
    assertEquals(latestVer, prefs.getInstalledVersion())
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

  @Test
  fun `verify citizen disaster report status values and hazard zones`() {
    val report = com.example.model.CitizenDisasterReport(
      id = "LAP-MMK-TEST-001",
      disasterType = "Banjir / Genangan Luapan Air",
      incidentTime = "25 Okt 2026, 10:00 WIB",
      incidentLocation = "Jl. Lintas Koto Jaya",
      district = "Kota Mukomuko",
      village = "Kelurahan Koto Jaya",
      latitude = -2.5833,
      longitude = 101.1167,
      gpsAccuracyMeters = 5.0f,
      isGpsVerified = true,
      casualtiesDead = 0,
      casualtiesInjured = 0,
      casualtiesMissing = 0,
      photoUri = null,
      reporterName = "Warga Test",
      reporterPhone = "08123456789",
      additionalNotes = "Catatan uji coba",
      timestamp = System.currentTimeMillis(),
      status = "Diproses"
    )
    assertEquals("Diproses", report.status)
    val validated = report.copy(status = "Divalidasi")
    assertEquals("Divalidasi", validated.status)
    val completed = report.copy(status = "Selesai")
    assertEquals("Selesai", completed.status)
    assertTrue(com.example.ui.components.MUKOMUKO_HAZARD_ZONES.isNotEmpty())
  }

  @Test
  fun `verify bpbd officer authentication and valid pins`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val officerAuth = com.example.data.local.OfficerAuthManager(context)

    // Test invalid pin
    val failed = officerAuth.loginOfficer("Bripka Hendra", "TRC-01", "Shift 1", "0000")
    org.junit.Assert.assertFalse(failed)

    // Test valid BPBD PIN 117
    val success117 = officerAuth.loginOfficer("Bripka Hendra", "TRC-01", "Shift Siaga 24 Jam", "117")
    assertTrue(success117)
    assertTrue(officerAuth.isOfficerLoggedIn())
    assertEquals("Bripka Hendra", officerAuth.getOfficerName())

    // Test logout
    officerAuth.logoutOfficer()
    org.junit.Assert.assertFalse(officerAuth.isOfficerLoggedIn())

    // Test valid TRC2026 PIN
    val successTrc = officerAuth.loginOfficer("Tim Reaksi Cepat", "TRC-MMK-117", "Regu Siaga", "TRC2026")
    assertTrue(successTrc)
    assertTrue(officerAuth.isOfficerLoggedIn())
  }

  @Test
  fun `verify room indexing and fast querying for personnel and citizen disaster reports`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = com.example.data.local.AppDatabase.getInstance(context)
    val personnelDao = database.personnelDao()
    val weatherDao = database.weatherDao()

    // Test Personnel indexed operations
    personnelDao.clearAll()
    val person1 = com.example.data.local.Personnel(
      name = "Agus TRC Pusdalops",
      nip = "TRC-MMK-9901",
      role = "Petugas Lapangan TRC",
      unit = "Pusdalops BPBD Kab. Mukomuko",
      shift = "Shift Siaga 24 Jam",
      phone = "081234567890"
    )
    personnelDao.insertPersonnel(person1)

    val fetched = personnelDao.getPersonnelByNip("TRC-MMK-9901")
    assertNotNull(fetched)
    assertEquals("Agus TRC Pusdalops", fetched?.name)

    val count = personnelDao.getPersonnelCount()
    assertTrue(count >= 1)

    // Test Reports indexed operations
    val reportEntity = com.example.data.local.CitizenDisasterReportEntity(
      id = "TEST-IDX-001",
      disasterType = "Banjir",
      incidentTime = "30 Okt 2026",
      incidentLocation = "Kota Mukomuko",
      district = "Kota Mukomuko",
      village = "Pasar Mukomuko",
      latitude = -2.58,
      longitude = 101.12,
      gpsAccuracyMeters = 3.5f,
      isGpsVerified = true,
      casualtiesDead = 0,
      casualtiesInjured = 0,
      casualtiesMissing = 0,
      photoUri = null,
      reporterName = "Uji Index",
      reporterPhone = "081111111",
      additionalNotes = "Data uji indexing ribuan record",
      timestamp = System.currentTimeMillis(),
      status = "Diproses"
    )
    weatherDao.insertCitizenDisasterReport(reportEntity)

    val reportCount = weatherDao.getCitizenReportCount()
    assertTrue(reportCount >= 1)

    val countDiproses = weatherDao.getCitizenReportCountByStatus("Diproses")
    assertTrue(countDiproses >= 1)
  }

  @Test
  fun `verify 8 ASN BPBD Pusdalops operators seeded with separate name and NIP columns`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = com.example.data.local.AppDatabase.getInstance(context)
    val personnelDao = database.personnelDao()
    val repo = com.example.data.repository.PersonnelRepository(personnelDao)

    // Seed official ASN BPBD personnel
    repo.seedDefaultAsnPersonnelIfNeeded()

    // Expected 8 ASN Pusdalops Operators
    val expectedAsnList = listOf(
      "YUDHA SOFRIANTO,S.A.P" to "199807202025061002",
      "ERI YANTO" to "198912102025211038",
      "MUHAMMAD RIGAN JASWARI" to "200201252025211014",
      "AINUN MARDIATI.S.Pd.SD" to "198812302025212141",
      "SATRIA AMANDA PUTRA" to "200003112025211067",
      "FIRMANSYAH" to "199305102025211171",
      "ARIF WIJOYO" to "198606282025211024",
      "DAVID HARIANTO" to "199303302025211135"
    )

    for ((expectedName, expectedNip) in expectedAsnList) {
      val found = personnelDao.getPersonnelByNip(expectedNip)
      assertNotNull("Operator with NIP $expectedNip must exist in Room Database", found)
      assertEquals("Name column must match separately", expectedName, found?.name)
      assertEquals("NIP column must match separately", expectedNip, found?.nip)
      assertEquals("Role must be Operator Pusdalops", "Operator Pusdalops", found?.role)
    }

    // Verify in OfficerAuthManager credentials roster
    val officerAuth = com.example.data.local.OfficerAuthManager(context)
    val roster = officerAuth.getOfficerRoster()
    assertTrue(roster.size >= 8)

    val yudha = roster.find { it.nip == "199807202025061002" }
    assertNotNull(yudha)
    assertEquals("YUDHA SOFRIANTO,S.A.P", yudha?.fullName)
    assertEquals("Operator Pusdalops", yudha?.role)

    // Verify login with selected ASN credential
    val loginSuccess = officerAuth.loginOfficer(
      name = yudha!!.fullName,
      badgeId = yudha.nip,
      shift = yudha.shift,
      pin = "117",
      role = yudha.role
    )
    assertTrue(loginSuccess)
    assertTrue(officerAuth.isOfficerLoggedIn())
    assertEquals("YUDHA SOFRIANTO,S.A.P", officerAuth.getOfficerName())
    assertEquals("199807202025061002", officerAuth.getOfficerBadgeId())
    assertEquals("Operator Pusdalops", officerAuth.getOfficerRole())
  }

  @Test
  fun `verify Manager Pusdalops is positioned directly below Pengawas in leadership and role rank`() {
    val leadership = com.example.data.local.OfficialAsnBpbdPersonnel.LEADERSHIP
    assertEquals("Pengawas must be first in leadership", com.example.data.local.OfficialAsnBpbdPersonnel.ROLE_PENGAWAS, leadership[0].role)
    assertEquals("Manajer Pusdalops must be directly below Pengawas (index 1)", com.example.data.local.OfficialAsnBpbdPersonnel.ROLE_MANAJER, leadership[1].role)

    // Role rank priority checks
    val rankPengawas = com.example.data.local.OfficerRoles.getRoleRank(com.example.data.local.OfficerRoles.PENGAWAS)
    val rankManajer = com.example.data.local.OfficerRoles.getRoleRank(com.example.data.local.OfficerRoles.MANAJER)
    val rankSupervisor = com.example.data.local.OfficerRoles.getRoleRank(com.example.data.local.OfficerRoles.SUPERVISOR)

    assertEquals(1, rankPengawas)
    assertEquals(2, rankManajer)
    assertTrue(rankManajer > rankPengawas)
    assertTrue(rankManajer < rankSupervisor)
  }
}

