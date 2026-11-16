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
  fun `verify bpbd officer authentication rejects default pins and accepts custom pbkdf2 pin`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val officerAuth = com.example.data.local.OfficerAuthManager(context)

    // Test invalid pin and forbidden default pins
    val failed = officerAuth.loginOfficer("Bripka Hendra", "TRC-01", "Shift 1", "0000")
    org.junit.Assert.assertFalse(failed)

    val rejected117 = officerAuth.loginOfficer("Bripka Hendra", "TRC-01", "Shift Siaga 24 Jam", "117")
    org.junit.Assert.assertFalse("Default PIN 117 must be rejected", rejected117)

    val rejectedTrc = officerAuth.loginOfficer("Tim Reaksi Cepat", "TRC-MMK-117", "Regu Siaga", "TRC2026")
    org.junit.Assert.assertFalse("Default PIN TRC2026 must be rejected", rejectedTrc)

    // Configure custom PBKDF2 PIN and authenticate
    officerAuth.setCustomOfficerPin("849201")
    val successCustom = officerAuth.loginOfficer("Bripka Hendra", "TRC-01", "Shift Siaga 24 Jam", "849201")
    assertTrue(successCustom)
    assertTrue(officerAuth.isOfficerLoggedIn())
    assertEquals("Bripka Hendra", officerAuth.getOfficerName())

    // Test logout
    officerAuth.logoutOfficer()
    org.junit.Assert.assertFalse(officerAuth.isOfficerLoggedIn())
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

    // Verify default PIN 117 is rejected and configured custom PIN succeeds
    val defaultRejected = officerAuth.loginOfficer(
      name = yudha!!.fullName,
      badgeId = yudha.nip,
      shift = yudha.shift,
      pin = "117",
      role = yudha.role
    )
    org.junit.Assert.assertFalse(defaultRejected)

    officerAuth.setCustomOfficerPin("739150")
    val loginSuccess = officerAuth.loginOfficer(
      name = yudha.fullName,
      badgeId = yudha.nip,
      shift = yudha.shift,
      pin = "739150",
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

  @Test
  fun `verify smart officer auth harvests all personnel and saves unique credentials in database`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val smartRepo = com.example.data.repository.SmartOfficerAuthRepository.getInstance(context)

    // Harvest all officers from ASN personnel, Room, and roster
    smartRepo.harvestAndSyncAllOfficers()

    val db = com.example.data.local.AppDatabase.getInstance(context)
    val credList = db.credentialDao().getAllCredentialsList()
    assertTrue("Credentials table must have at least the 8 ASN personnel", credList.size >= 8)

    // Verify Yudha's credential is saved in Room database
    val yudhaCred = db.credentialDao().getCredentialByNip("199807202025061002")
    assertNotNull("Yudha must have a database credential", yudhaCred)
    assertEquals("YUDHA SOFRIANTO,S.A.P", yudhaCred?.fullName)
    assertEquals("Operator Pusdalops", yudhaCred?.role)
    assertTrue("Salt must not be empty", yudhaCred!!.salt.isNotBlank())
    assertTrue("PIN hash must not be empty", yudhaCred.pinHash.isNotBlank())
    assertEquals("061002", yudhaCred.initialSecurePin) // Last 6 digits of 199807202025061002 -> 061002
  }

  @Test
  fun `verify smart officer auth strictly rejects default PIN 117 and TRC2026`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val smartRepo = com.example.data.repository.SmartOfficerAuthRepository.getInstance(context)
    smartRepo.harvestAndSyncAllOfficers()

    val yudhaNip = "199807202025061002"

    // Attempt login with default PIN "117"
    val result117 = smartRepo.authenticateOfficer(yudhaNip, "117", "TestDevice")
    assertTrue("Login with default PIN 117 must fail", result117.isFailure)
    assertTrue(
      "Error must explicitly mention default PIN is disabled",
      result117.exceptionOrNull()?.message?.contains("PIN default", ignoreCase = true) == true
    )

    // Attempt login with default PIN "TRC2026"
    val resultTrc = smartRepo.authenticateOfficer(yudhaNip, "TRC2026", "TestDevice")
    assertTrue("Login with TRC2026 must fail", resultTrc.isFailure)

    // Attempt login with default PIN "BPBD117"
    val resultBpbd = smartRepo.authenticateOfficer(yudhaNip, "BPBD117", "TestDevice")
    assertTrue("Login with BPBD117 must fail", resultBpbd.isFailure)
  }

  @Test
  fun `verify smart officer auth succeeds with initial NIP secret on new unactivated device`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val smartRepo = com.example.data.repository.SmartOfficerAuthRepository.getInstance(context)
    smartRepo.harvestAndSyncAllOfficers()

    val db = com.example.data.local.AppDatabase.getInstance(context)
    val yudhaCred = db.credentialDao().getCredentialByNip("199807202025061002")!!

    // Login with initial deterministic NIP secret
    val loginResult = smartRepo.authenticateOfficer(yudhaCred.nip, yudhaCred.initialSecurePin, "Pixel 8 Pro")
    assertTrue("Login with initial NIP secret must succeed", loginResult.isSuccess)
    val profile = loginResult.getOrNull()
    assertNotNull(profile)
    assertEquals("YUDHA SOFRIANTO,S.A.P", profile?.fullName)

    // Check device was recorded
    val updated = db.credentialDao().getCredentialByNip(yudhaCred.nip)
    assertTrue(updated?.lastLoginDevice?.contains("Pixel 8 Pro") == true)
    assertTrue((updated?.lastLoginAt ?: 0L) > 0L)
  }

  @Test
  fun `verify changing personal PIN persists to database and rejects old PIN or default PIN`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val smartRepo = com.example.data.repository.SmartOfficerAuthRepository.getInstance(context)
    smartRepo.harvestAndSyncAllOfficers()

    val yudhaNip = "199807202025061002"
    val db = com.example.data.local.AppDatabase.getInstance(context)
    val cred = db.credentialDao().getCredentialByNip(yudhaNip)!!

    // Try setting new PIN to a forbidden default PIN (e.g. "117")
    val rejectDefaultChange = smartRepo.changePersonalPin(yudhaNip, cred.initialSecurePin, "117")
    assertTrue("Cannot change PIN to default PIN 117", rejectDefaultChange.isFailure)

    // Set a custom secure PIN (e.g. "889911")
    val changeResult = smartRepo.changePersonalPin(yudhaNip, cred.initialSecurePin, "889911")
    assertTrue("Changing to custom PIN 889911 must succeed", changeResult.isSuccess)

    // Now old initial NIP key should be rejected
    val loginOldKey = smartRepo.authenticateOfficer(yudhaNip, cred.initialSecurePin, "Pixel 8")
    assertTrue("Old initial NIP key must now fail", loginOldKey.isFailure)

    // Login with new custom PIN must succeed
    val loginNewPin = smartRepo.authenticateOfficer(yudhaNip, "889911", "Pixel 8")
    assertTrue("Login with new custom PIN must succeed", loginNewPin.isSuccess)

    // Credential isCustomPinSet must be true
    val updatedCred = db.credentialDao().getCredentialByNip(yudhaNip)
    assertTrue(updatedCred?.isCustomPinSet == true)

    // Reset to individual NIP key
    val resetResult = smartRepo.resetToInitialNipSecret(yudhaNip)
    assertTrue("Reset to NIP key must succeed", resetResult.isSuccess)
    val credAfterReset = db.credentialDao().getCredentialByNip(yudhaNip)
    org.junit.Assert.assertFalse(credAfterReset?.isCustomPinSet ?: true)
  }

  @Test
  fun `verify pbkdf2 hashing and legacy sha256 verification compatibility`() {
    val salt = com.example.data.local.OfficerCredentialEntity.generateCryptographicSalt()
    val hash = com.example.data.local.OfficerCredentialEntity.hashWithSalt("918273", salt)
    assertTrue("New hashes must use PBKDF2 format", hash.startsWith("pbkdf2\$10000\$"))
    assertTrue(com.example.data.local.OfficerCredentialEntity.verifyHash("918273", salt, hash))
    org.junit.Assert.assertFalse(com.example.data.local.OfficerCredentialEntity.verifyHash("000000", salt, hash))

    // Legacy SHA-256 hash format remains verifiable during migration
    val legacyMd = java.security.MessageDigest.getInstance("SHA-256")
      .digest("$salt:654321:mukomuko_bpbd_2026".toByteArray(Charsets.UTF_8))
      .joinToString("") { "%02x".format(it) }
    assertTrue(com.example.data.local.OfficerCredentialEntity.verifyHash("654321", salt, legacyMd))
  }

  @Test
  fun `verify admin password hardening blocks defaults enforces lockout and restricts resetPinWithAdmin`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    context.getSharedPreferences("bpbd_officer_auth_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    val officerAuth = com.example.data.local.OfficerAuthManager(context)

    org.junit.Assert.assertFalse(officerAuth.isAdminPasswordConfigured())
    org.junit.Assert.assertFalse("Hardcoded default password must be rejected", officerAuth.verifyAdminPassword("sipagarkita2026"))
    assertTrue(officerAuth.configureInitialAdminPassword("sipagarkita2026").isFailure)

    val setupResult = officerAuth.configureInitialAdminPassword("BpbdMmk#2026!")
    assertTrue(setupResult.isSuccess)
    assertTrue(officerAuth.isAdminPasswordConfigured())
    assertTrue(officerAuth.verifyAdminPassword("BpbdMmk#2026!"))

    // Ensure officer PIN cannot be used to authorize resetPinWithAdmin
    officerAuth.setCustomOfficerPin("849201")
    assertTrue(officerAuth.resetPinWithAdmin("849201").isFailure)
    assertTrue(officerAuth.resetPinWithAdmin("BpbdMmk#2026!").isSuccess)

    // Brute-force 5 wrong admin password attempts trigger temporary lockout
    repeat(5) {
      org.junit.Assert.assertFalse(officerAuth.verifyAdminPassword("WrongPassword$it"))
    }
    assertTrue(officerAuth.getAdminLockoutRemainingSeconds() > 0L)
    org.junit.Assert.assertFalse("Correct password must stay locked out during cooldown", officerAuth.verifyAdminPassword("BpbdMmk#2026!"))
  }

  @Test
  fun `verify sensitive shared preferences values are aes256gcm encrypted at rest`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val authPrefs = context.getSharedPreferences("bpbd_officer_auth_prefs", Context.MODE_PRIVATE)
    authPrefs.edit().clear().commit()

    val officerAuth = com.example.data.local.OfficerAuthManager(context)
    officerAuth.setCustomOfficerPin("918273")
    officerAuth.loginOfficerDirectly("Yudha Test", "199807202025061002", "Shift 1", "Operator Pusdalops")

    val rawName = authPrefs.getString("officer_name", null)
    val rawBadge = authPrefs.getString("officer_badge_id", null)
    val rawPinHash = authPrefs.getString("officer_pin_hash", null)
    assertTrue(com.example.security.SecureStorageHelper.isEncryptedValue(rawName))
    assertTrue(com.example.security.SecureStorageHelper.isEncryptedValue(rawBadge))
    assertTrue(com.example.security.SecureStorageHelper.isEncryptedValue(rawPinHash))
    assertEquals("Yudha Test", officerAuth.getOfficerName())
    assertEquals("199807202025061002", officerAuth.getOfficerBadgeId())

    val sessionMgr = com.example.util.SessionManager(context)
    sessionMgr.setPetugasLogin("Hitatun Test", "197401282006041006", "Manajer")
    val sessionPrefs = context.getSharedPreferences("PusdalopsSession", Context.MODE_PRIVATE)
    assertTrue(com.example.security.SecureStorageHelper.isEncryptedValue(sessionPrefs.getString("NAMA_PETUGAS", null)))
    assertEquals("Hitatun Test", sessionMgr.getNamaPetugas())
  }

  @Test
  fun `verify ota download url validation blocks http and untrusted domains`() {
    val httpRejected = com.example.data.api.AppUpdateRemoteService.validateDownloadUrl("http://github.com/evil/app.apk")
    assertEquals("https://t.me/takajuikgue", httpRejected)

    val untrustedRejected = com.example.data.api.AppUpdateRemoteService.validateDownloadUrl("https://evil-domain.example/update.apk")
    assertEquals("https://t.me/takajuikgue", untrustedRejected)

    val githubAccepted = com.example.data.api.AppUpdateRemoteService.validateDownloadUrl("https://github.com/tathoenz/sipagar-kita/releases/download/v3.7/app.apk")
    assertEquals("https://github.com/tathoenz/sipagar-kita/releases/download/v3.7/app.apk", githubAccepted)
  }

  @Test
  fun `verify device security session defaults fail closed`() {
    val session = com.example.security.DeviceSecuritySession()
    org.junit.Assert.assertFalse(session.isVerified)
    org.junit.Assert.assertFalse(session.accessGranted)
    assertEquals("UNVERIFIED", session.verdict)

    val fromEmptyMap = com.example.security.DeviceSecuritySession.fromMap(emptyMap())
    org.junit.Assert.assertFalse(fromEmptyMap.isVerified)
    org.junit.Assert.assertFalse(fromEmptyMap.accessGranted)
    assertEquals("UNVERIFIED", fromEmptyMap.verdict)
  }

  @Test
  fun `verify v1_0_0 production release defaults and apk upgrade sync`() {
    val state = com.example.model.AppUpdateState()
    assertEquals("1.0.0", state.currentVersion)
    assertEquals("1.0.0", state.latestVersion)
    assertEquals(1, state.changelogs.size)
    assertEquals("1.0.0", state.changelogs.first().version)
    assertTrue(state.changelogs.first().isCurrent)
    assertTrue(state.changelogs.first().isLatest)

    val context = ApplicationProvider.getApplicationContext<Context>()
    val rawPrefs = context.getSharedPreferences("mukomuko_update_persistence", Context.MODE_PRIVATE)
    rawPrefs.edit()
      .putString("recorded_build_version", "0.9.0")
      .putString("installed_version", "0.9.0")
      .putString("latest_server_version", "1.0.0")
      .putBoolean("is_permanently_installed", false)
      .putString("dismissed_dialog_version", "1.0.0")
      .commit()

    val prefs = com.example.data.local.AppUpdatePreferences(context)
    assertEquals("1.0.0", com.example.data.local.AppUpdatePreferences.DEFAULT_LATEST_VERSION)
    assertEquals(BuildConfig.VERSION_NAME, prefs.getInstalledVersion())
    assertTrue(prefs.isUpdatePermanentlyInstalled())
    org.junit.Assert.assertFalse(prefs.isDialogDismissedForVersion("1.0.0"))
  }

  @Test
  fun `verify device authenticity login falls back gracefully when play integrity unavailable`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val securityManager = com.example.security.SecurityManager.getInstance(context)
    val playIntegrity = com.example.security.PlayIntegritySecurityManager.getInstance(context)

    val loginRes = securityManager.verifyDeviceAuthenticityDuringLogin(
      officerNip = "199807202025061002",
      officerName = "YUDHA SOFRIANTO,S.A.P",
      officerRole = "Operator Pusdalops"
    )
    assertTrue("Login authenticity check must succeed with fallback warning", loginRes.isSuccess)
    val session = loginRes.getOrNull()
    assertNotNull(session)
    assertEquals("APP_CHECK_FALLBACK_AUTHORIZED", session!!.verdict)
    assertTrue(session.accessGranted)
    assertTrue(session.isVerified)
    assertTrue(session.integrityToken.isNotBlank())

    val cachedSessions = securityManager.fetchSecuritySessions()
    assertTrue("Logged-in device session must be returned by fetchSecuritySessions", cachedSessions.any { it.officerNip == "199807202025061002" })

    val accessResult = securityManager.verifySessionBeforeGrantingRuangKerja("199807202025061002")
    assertTrue("Ruang Kerja local session check must authorize after fallback login", accessResult.isAuthorized)

    val deleted = securityManager.deleteSecuritySession("199807202025061002")
    assertTrue("deleteSecuritySession must succeed", deleted)
    assertTrue(
      "Revoked session must immediately disappear from fetchSecuritySessions",
      securityManager.fetchSecuritySessions().none { it.officerNip == "199807202025061002" }
    )

    val criticalRes = playIntegrity.verifyCriticalAction("199807202025061002", "CHANGE_PIN")
    assertTrue("PlayIntegritySecurityManager critical action must fallback gracefully", criticalRes.isSecure)
    assertTrue(!criticalRes.token.isNullOrBlank())
  }
}



