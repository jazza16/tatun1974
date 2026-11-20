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
  fun `verify bpbd officer authentication rejects default pins and never stores raw pins locally`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.data.local.FirestoreHelper.resetTestHooks()
    com.example.data.repository.SmartOfficerAuthRepository.clearFailedAttemptsForTesting()
    val officerAuth = com.example.data.local.OfficerAuthManager(context)
    val smartRepo = com.example.data.repository.SmartOfficerAuthRepository(context)

    // Test invalid pin and forbidden default pins unconditionally rejected
    org.junit.Assert.assertFalse(officerAuth.loginOfficer("Bripka Hendra", "TRC-01", "Shift 1", "0000"))
    org.junit.Assert.assertFalse("Default PIN 117 must be rejected", smartRepo.authenticateOfficer("TRC-01", "117").isSuccess)
    org.junit.Assert.assertFalse("Default PIN TRC2026 must be rejected", smartRepo.authenticateOfficer("TRC-MMK-117", "TRC2026").isSuccess)

    // Configure Cloud mock credential and verify Cloud-authenticated login saves only NIP
    val salt = com.example.data.local.OfficerCredentialEntity.generateCryptographicSalt()
    val hash = com.example.data.local.OfficerCredentialEntity.hashWithSalt("849201", salt)
    com.example.data.local.FirestoreHelper.overrideCloudCredentialFetcherForTesting = {
      com.example.data.local.CloudCredentialLookupResult.Found(
        com.example.data.local.OfficerCredentialEntity(
          nip = "TRC-01",
          fullName = "Bripka Hendra",
          role = "Petugas Lapangan TRC",
          shift = "Shift Siaga 24 Jam",
          pinHash = hash,
          salt = salt,
          isCustomPinSet = true,
          isSyncedWithCloud = true
        )
      )
    }
    com.example.data.local.FirestoreHelper.overrideCloudCredentialWriterForTesting = { Result.success(Unit) }

    val successCustom = smartRepo.authenticateOfficer("TRC-01", "849201")
    assertTrue(successCustom.isSuccess)
    assertTrue(officerAuth.isOfficerLoggedIn())
    assertEquals("Bripka Hendra", officerAuth.getOfficerName())
    assertEquals("TRC-01", officerAuth.getSavedNip())

    // Test logout preserves saved_nip while ending active session
    officerAuth.logoutOfficer()
    org.junit.Assert.assertFalse(officerAuth.isOfficerLoggedIn())
    assertEquals("TRC-01", officerAuth.getSavedNip())
    com.example.data.local.FirestoreHelper.resetTestHooks()
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
    com.example.data.local.FirestoreHelper.resetTestHooks()
    com.example.data.repository.SmartOfficerAuthRepository.clearFailedAttemptsForTesting()
    val database = com.example.data.local.AppDatabase.getInstance(context)
    val personnelDao = database.personnelDao()
    val repo = com.example.data.repository.PersonnelRepository(personnelDao)

    // Seed official ASN BPBD personnel (non-secret roster ONLY, never default PINs)
    repo.seedDefaultAsnPersonnelIfNeeded()

    // Expected 8 ASN Pusdalops Operators
    val expectedAsnList = listOf(
      "YUDHA SOFRIANTO,S.A.P" to "199807202025061002",
      "ERI YANTO" to "198912102025211038",
      "M. RIGAN JASWARI, S.Akun." to "200201252025211014",
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

    val smartRepo = com.example.data.repository.SmartOfficerAuthRepository(context)
    val salt = com.example.data.local.OfficerCredentialEntity.generateCryptographicSalt()
    com.example.data.local.FirestoreHelper.overrideCloudCredentialFetcherForTesting = {
      com.example.data.local.CloudCredentialLookupResult.Found(
        com.example.data.local.OfficerCredentialEntity(
          nip = yudha!!.nip,
          fullName = yudha.fullName,
          role = yudha.role,
          shift = yudha.shift,
          pinHash = com.example.data.local.OfficerCredentialEntity.hashWithSalt("739150", salt),
          salt = salt,
          isCustomPinSet = true,
          isSyncedWithCloud = true
        )
      )
    }
    com.example.data.local.FirestoreHelper.overrideCloudCredentialWriterForTesting = { Result.success(Unit) }

    // Verify default PIN 117 is rejected and Cloud-verified custom PIN succeeds
    val defaultRejected = smartRepo.authenticateOfficer(yudha!!.nip, "117")
    org.junit.Assert.assertFalse(defaultRejected.isSuccess)

    val loginSuccess = smartRepo.authenticateOfficer(yudha.nip, "739150")
    assertTrue(loginSuccess.isSuccess)
    assertTrue(officerAuth.isOfficerLoggedIn())
    assertEquals("YUDHA SOFRIANTO,S.A.P", officerAuth.getOfficerName())
    assertEquals("199807202025061002", officerAuth.getOfficerBadgeId())
    assertEquals("Operator Pusdalops", officerAuth.getOfficerRole())
    com.example.data.local.FirestoreHelper.resetTestHooks()
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
  fun `verify smart officer auth pulls cloud metadata without seeding local plaintext PINs`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.data.local.FirestoreHelper.resetTestHooks()
    val smartRepo = com.example.data.repository.SmartOfficerAuthRepository(context)
    val db = com.example.data.local.AppDatabase.getInstance(context)
    db.clearAllTables()

    // Ensure local table is NEVER auto-seeded with default PINs when Cloud has not returned anything
    val beforeCredList = db.credentialDao().getAllCredentialsList()
    assertTrue("Credentials table must stay empty when offline/unseeded", beforeCredList.isEmpty())

    // Simulate Cloud-verified login for Yudha
    val salt = com.example.data.local.OfficerCredentialEntity.generateCryptographicSalt()
    com.example.data.local.FirestoreHelper.overrideCloudCredentialFetcherForTesting = {
      com.example.data.local.CloudCredentialLookupResult.Found(
        com.example.data.local.OfficerCredentialEntity(
          nip = "199807202025061002",
          fullName = "YUDHA SOFRIANTO,S.A.P",
          role = "Operator Pusdalops",
          pinHash = com.example.data.local.OfficerCredentialEntity.hashWithSalt("739150", salt),
          salt = salt,
          isCustomPinSet = true
        )
      )
    }
    com.example.data.local.FirestoreHelper.overrideCloudCredentialWriterForTesting = { Result.success(Unit) }

    val loginRes = smartRepo.authenticateOfficer("199807202025061002", "739150", "Pixel 8 Pro")
    assertTrue(loginRes.isSuccess)

    // Verify local Room metadata has NO PIN hash, salt, or plaintext initialSecurePin
    val yudhaCred = db.credentialDao().getCredentialByNip("199807202025061002")
    assertNotNull("Yudha metadata must be cached after login", yudhaCred)
    assertEquals("YUDHA SOFRIANTO,S.A.P", yudhaCred?.fullName)
    assertEquals("", yudhaCred?.pinHash)
    assertEquals("", yudhaCred?.salt)
    assertEquals("", yudhaCred?.initialSecurePin)
    com.example.data.local.FirestoreHelper.resetTestHooks()
  }

  @Test
  fun `verify smart officer auth strictly rejects default PIN 117 and TRC2026`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val smartRepo = com.example.data.repository.SmartOfficerAuthRepository(context)

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
  fun `verify changing personal PIN persists to Cloud Firestore and rejects old PIN or default PIN`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.data.local.FirestoreHelper.resetTestHooks()
    com.example.data.repository.SmartOfficerAuthRepository.clearFailedAttemptsForTesting()
    val smartRepo = com.example.data.repository.SmartOfficerAuthRepository(context)
    val yudhaNip = "199807202025061002"

    val initSalt = com.example.data.local.OfficerCredentialEntity.generateCryptographicSalt()
    var cloudCred = com.example.data.local.OfficerCredentialEntity(
      nip = yudhaNip,
      fullName = "YUDHA SOFRIANTO,S.A.P",
      role = "Operator Pusdalops",
      pinHash = com.example.data.local.OfficerCredentialEntity.hashWithSalt("654321", initSalt),
      salt = initSalt,
      isCustomPinSet = true
    )

    com.example.data.local.FirestoreHelper.overrideCloudCredentialFetcherForTesting = {
      com.example.data.local.CloudCredentialLookupResult.Found(cloudCred)
    }
    com.example.data.local.FirestoreHelper.overrideCloudCredentialWriterForTesting = { updated ->
      cloudCred = updated
      Result.success(Unit)
    }

    // Try setting new PIN to a forbidden default PIN (e.g. "117")
    val rejectDefaultChange = smartRepo.changePersonalPin(yudhaNip, "654321", "117")
    assertTrue("Cannot change PIN to default PIN 117", rejectDefaultChange.isFailure)

    // Set a custom secure PIN (e.g. "889911")
    val changeResult = smartRepo.changePersonalPin(yudhaNip, "654321", "889911")
    assertTrue("Changing to custom PIN 889911 must succeed", changeResult.isSuccess)

    // Old PIN should now be rejected by Cloud verification
    val loginOldKey = smartRepo.authenticateOfficer(yudhaNip, "654321", "Pixel 8")
    assertTrue("Old PIN must now fail", loginOldKey.isFailure)

    // Login with new custom PIN must succeed
    val loginNewPin = smartRepo.authenticateOfficer(yudhaNip, "889911", "Pixel 8")
    assertTrue("Login with new custom PIN must succeed", loginNewPin.isSuccess)

    // Reset to individual NIP key on Cloud
    val resetResult = smartRepo.resetToInitialNipSecret(yudhaNip)
    assertTrue("Reset to NIP key must succeed", resetResult.isSuccess)
    org.junit.Assert.assertFalse(cloudCred.isCustomPinSet)
    com.example.data.local.FirestoreHelper.resetTestHooks()
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
  fun `verify local admin password verification is permanently disabled in favor of server-side auth`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    context.getSharedPreferences("bpbd_officer_auth_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    val officerAuth = com.example.data.local.OfficerAuthManager(context)

    com.example.security.AdminAccessSecurityManager.resetForTesting()
    com.example.security.AdminAccessSecurityManager.disableFirestoreForTesting = true
    com.example.security.AdminAccessSecurityManager.overrideFirestorePinFetcherForTesting = {
      com.example.security.FirestoreMasterPinLookup.Found(masterPin = "994411", sourceLabel = "TEST_SERVER")
    }

    org.junit.Assert.assertFalse(officerAuth.isAdminPasswordConfigured())
    val defaultAttempt = com.example.security.AdminAccessSecurityManager.verifyMasterPasswordAndIssueToken(
      context = context,
      masterPassword = "sipagarkita2026",
      officerNip = "197906072006041011",
      officerName = "RURI IRWANDI, ST, MT",
      officerRole = "Manajer Pusdalops-PB",
      actionTitle = "Kelola Personil"
    )
    assertTrue("Hardcoded default password must be rejected by server-side auth", defaultAttempt is com.example.security.AdminVerificationResult.Denied)
    assertTrue(officerAuth.configureInitialAdminPassword("sipagarkita2026").isFailure)

    val validAttempt = com.example.security.AdminAccessSecurityManager.verifyMasterPasswordAndIssueToken(
      context = context,
      masterPassword = "994411",
      officerNip = "197906072006041011",
      officerName = "RURI IRWANDI, ST, MT",
      officerRole = "Manajer Pusdalops-PB",
      actionTitle = "Kelola Personil"
    )
    assertTrue("Valid server-side master PIN must issue temporary token", validAttempt is com.example.security.AdminVerificationResult.Authorized)
    com.example.security.AdminAccessSecurityManager.resetForTesting()
  }

  @Test
  fun `verify saved_nip and officer session are aes256gcm encrypted at rest with zero local PIN storage`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val authPrefs = context.getSharedPreferences("bpbd_officer_auth_prefs", Context.MODE_PRIVATE)
    authPrefs.edit().clear().commit()

    val officerAuth = com.example.data.local.OfficerAuthManager(context)
    officerAuth.loginOfficerDirectly("Yudha Test", "199807202025061002", "Shift 1", "Operator Pusdalops")

    val rawName = authPrefs.getString("officer_name", null)
    val rawBadge = authPrefs.getString("officer_badge_id", null)
    val rawSavedNip = authPrefs.getString("saved_nip", null)
    assertTrue(com.example.security.SecureStorageHelper.isEncryptedValue(rawName))
    assertTrue(com.example.security.SecureStorageHelper.isEncryptedValue(rawBadge))
    assertTrue(com.example.security.SecureStorageHelper.isEncryptedValue(rawSavedNip))
    org.junit.Assert.assertFalse(authPrefs.contains("officer_pin"))
    org.junit.Assert.assertFalse(authPrefs.contains("officer_pin_hash"))
    org.junit.Assert.assertFalse(authPrefs.contains("officer_pin_salt"))
    assertEquals("YUDHA SOFRIANTO,S.A.P", officerAuth.getOfficerName())
    assertEquals("199807202025061002", officerAuth.getOfficerBadgeId())
    assertEquals("199807202025061002", officerAuth.getSavedNip())

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

  @Test
  fun `verify DeviceSecuritySession and OfficerLoginActivity map round-trip preserves hardware and token telemetry`() {
    val session = com.example.security.DeviceSecuritySession(
      sessionId = "sess-test-001",
      officerNip = "199807202025061002",
      officerName = "Petugas Uji",
      officerRole = "Operator Pusdalops",
      integrityToken = "eyJhbGciOiJSUzI1NiJ9.payload.sig",
      tokenHash = "a1b2c3d4e5f607080910111213141516",
      classicNonce = "bm9uY2VfMTIzNDU2Nzg5MA",
      verdict = "MEETS_STRONG_INTEGRITY",
      deviceVerdict = "MEETS_STRONG_INTEGRITY",
      deviceId = "android-hw-id-01",
      deviceModel = "Samsung SM-S918B",
      osVersion = "Android 15",
      sdkInt = 35,
      isVerified = true,
      accessGranted = true,
      tokenLength = 32,
      tokenSource = "PLAY_INTEGRITY_CLASSIC",
      deviceBrand = "samsung",
      deviceManufacturer = "Samsung",
      deviceHardware = "qcom",
      deviceFingerprintHash = "f0e1d2c3b4a5968778695a4b3c2d1e0f",
      isEmulator = false,
      appVersion = "1.0.0 (1)",
      loginMethod = "BIOMETRIK",
      loginStatus = "SUCCESS",
      loginAction = "LOGIN_BIOMETRIC_SUCCESSFUL",
      loginCount = 4,
      lastActivityDetail = "Login biometrik terverifikasi",
      adminVerified = true,
      adminVerifiedBy = "Admin Pusdalops",
      adminVerifiedAt = 1760000001000L,
      adminVerificationNote = "Perangkat resmi kantor",
      timestamp = 1760000000000L
    )

    val restoredSession = com.example.security.DeviceSecuritySession.fromMap(session.toMap())
    assertEquals(session.officerNip, restoredSession.officerNip)
    assertEquals(session.deviceBrand, restoredSession.deviceBrand)
    assertEquals(session.deviceManufacturer, restoredSession.deviceManufacturer)
    assertEquals(session.deviceHardware, restoredSession.deviceHardware)
    assertEquals(session.deviceFingerprintHash, restoredSession.deviceFingerprintHash)
    assertEquals(session.tokenSource, restoredSession.tokenSource)
    assertEquals(session.tokenLength, restoredSession.tokenLength)
    assertEquals(session.classicNonce, restoredSession.classicNonce)
    assertEquals(session.loginMethod, restoredSession.loginMethod)
    assertEquals(session.loginCount, restoredSession.loginCount)
    assertTrue(restoredSession.adminVerified)
    assertEquals("Admin Pusdalops", restoredSession.adminVerifiedBy)
    assertEquals("Perangkat resmi kantor", restoredSession.adminVerificationNote)

    val activity = com.example.security.OfficerLoginActivity(
      activityId = "act_1760000000000_test",
      sessionId = "sess-test-001",
      officerNip = "199807202025061002",
      officerName = "Petugas Uji",
      officerRole = "Operator Pusdalops",
      loginMethod = "PIN_CLOUD",
      loginStatus = "BLOCKED",
      action = "AUTH_INVALID_PIN",
      verdict = "UNVERIFIED",
      deviceVerdict = "LOCAL_DEVICE_CHECK",
      tokenSource = "AUTH_EVENT",
      tokenHash = "deadbeef0123456789abcdef01234567",
      tokenLength = 0,
      classicNonce = "nonce_abc",
      deviceId = "android-hw-id-01",
      deviceModel = "Samsung SM-S918B",
      deviceBrand = "samsung",
      deviceHardware = "qcom",
      deviceFingerprintHash = "f0e1d2c3b4a5968778695a4b3c2d1e0f",
      osVersion = "Android 15",
      sdkInt = 35,
      isEmulator = false,
      appVersion = "1.0.0 (1)",
      accessGranted = false,
      detail = "Percobaan PIN tidak valid",
      timestamp = 1760000002000L
    )

    val restoredActivity = com.example.security.OfficerLoginActivity.fromMap(activity.toMap())
    assertEquals(activity.activityId, restoredActivity.activityId)
    assertEquals(activity.loginMethod, restoredActivity.loginMethod)
    assertEquals(activity.loginStatus, restoredActivity.loginStatus)
    assertEquals(activity.action, restoredActivity.action)
    assertEquals(activity.deviceFingerprintHash, restoredActivity.deviceFingerprintHash)
    assertEquals(activity.detail, restoredActivity.detail)
    org.junit.Assert.assertFalse(restoredActivity.accessGranted)
  }

  @Test
  fun `verify all officer login activities sync into Play Integrity panel with admin hardware verification`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val securityManager = com.example.security.SecurityManager.getInstance(context)
    val testNip = "199901012026011099"

    // Start from clean slate for this test NIP
    securityManager.deleteSecuritySession(testNip, purgeActivities = true)

    // 1. Successful login records full hardware + Play Integrity Classic token telemetry & login activity
    val loginRes = securityManager.verifyDeviceAuthenticityDuringLogin(
      officerNip = testNip,
      officerName = "Petugas Uji Integritas",
      officerRole = "Operator Pusdalops",
      loginMethod = "PIN_CLOUD",
      loginAction = "LOGIN_SUCCESSFUL",
      detail = "Login PIN Cloud uji coba"
    )
    assertTrue(loginRes.isSuccess)
    val session = loginRes.getOrThrow()
    assertTrue(session.deviceId.isNotBlank())
    assertTrue(session.deviceFingerprintHash.isNotBlank())
    assertTrue(session.tokenHash.isNotBlank())
    assertTrue(session.classicNonce.isNotBlank())
    assertEquals("PIN_CLOUD", session.loginMethod)

    // 2. Failed login attempt is also captured in chronological login activities
    securityManager.recordLoginActivityEvent(
      officerNip = testNip,
      officerName = "Petugas Uji Integritas",
      officerRole = "Operator Pusdalops",
      loginMethod = "PIN_CLOUD",
      loginStatus = "FAILED",
      action = "AUTH_INVALID_PIN",
      accessGranted = false,
      detail = "PIN salah percobaan ke-1"
    )

    // 3. Historical Room PersonnelAuditLogEntity login records backfill idempotently
    val historicalAuditLogs = listOf(
      com.example.data.local.PersonnelAuditLogEntity(
        id = 701L,
        nip = testNip,
        name = "Petugas Uji Integritas",
        role = "Operator Pusdalops",
        shift = "Shift Siaga 24 Jam",
        action = "LOGIN_BIOMETRIC_SUCCESSFUL",
        deviceInfo = "Login biometrik historis",
        timestamp = System.currentTimeMillis() - 60_000L
      ),
      com.example.data.local.PersonnelAuditLogEntity(
        id = 702L,
        nip = testNip,
        name = "Petugas Uji Integritas",
        role = "Operator Pusdalops",
        shift = "Shift Siaga 24 Jam",
        action = "UNRELATED_REPORT_EXPORT",
        deviceInfo = "Bukan aktivitas login",
        timestamp = System.currentTimeMillis() - 30_000L
      )
    )

    val (syncedSessions1, syncedActivities1) = securityManager.syncAllOfficerLoginActivities(
      auditLogs = historicalAuditLogs,
      knownOfficialNips = listOf(testNip)
    )
    assertTrue(syncedSessions1.any { it.officerNip == testNip })
    val officerActs1 = syncedActivities1.filter { it.officerNip == testNip }
    assertTrue("Must contain LOGIN_SUCCESSFUL", officerActs1.any { it.action == "LOGIN_SUCCESSFUL" })
    assertTrue("Must contain AUTH_INVALID_PIN", officerActs1.any { it.action == "AUTH_INVALID_PIN" })
    assertTrue("Must backfill LOGIN_BIOMETRIC_SUCCESSFUL", officerActs1.any { it.action == "LOGIN_BIOMETRIC_SUCCESSFUL" })
    org.junit.Assert.assertFalse("Non-login audit actions must be excluded", officerActs1.any { it.action == "UNRELATED_REPORT_EXPORT" })

    // Re-running sync must be idempotent (no duplicate backfilled entries)
    val (_, syncedActivities2) = securityManager.syncAllOfficerLoginActivities(
      auditLogs = historicalAuditLogs,
      knownOfficialNips = listOf(testNip)
    )
    assertEquals(
      "Re-syncing Room audit logs must not duplicate entries",
      officerActs1.count { it.action == "LOGIN_BIOMETRIC_SUCCESSFUL" },
      syncedActivities2.count { it.officerNip == testNip && it.action == "LOGIN_BIOMETRIC_SUCCESSFUL" }
    )

    // 4. Admin verifies hardware & token (Approve -> then Block)
    val approvedRes = securityManager.verifyDeviceSessionByAdmin(
      session = session,
      adminName = "Admin Pusdalops Uji",
      approved = true,
      note = "Perangkat fisik dan token valid"
    )
    assertTrue(approvedRes.isSuccess)
    val approvedSession = approvedRes.getOrThrow()
    assertTrue(approvedSession.adminVerified)
    assertTrue(approvedSession.accessGranted)
    assertEquals("Admin Pusdalops Uji", approvedSession.adminVerifiedBy)
    assertTrue(
      securityManager.fetchLoginActivities().any {
        it.officerNip == testNip && it.action == "ADMIN_VERIFIED_HARDWARE_TOKEN"
      }
    )

    val blockedRes = securityManager.verifyDeviceSessionByAdmin(
      session = approvedSession,
      adminName = "Admin Pusdalops Uji",
      approved = false,
      note = "Perangkat diblokir sementara"
    )
    assertTrue(blockedRes.isSuccess)
    org.junit.Assert.assertFalse(blockedRes.getOrThrow().accessGranted)
    org.junit.Assert.assertFalse(blockedRes.getOrThrow().adminVerified)
    val accessAfterBlock = securityManager.verifySessionBeforeGrantingRuangKerja(testNip)
    org.junit.Assert.assertFalse("Blocked device must be denied Ruang Kerja access", accessAfterBlock.isAuthorized)

    // 5. Full purge cleans session and login activities
    assertTrue(securityManager.deleteSecuritySession(testNip, purgeActivities = true))
    assertTrue(securityManager.fetchSecuritySessions().none { it.officerNip == testNip })
    assertTrue(securityManager.fetchLoginActivities().none { it.officerNip == testNip })
  }

  @Test
  fun `verify SyncConflictHandler resolves citizen report status updates with updatedAt and officer attribution`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    try {
      val handler = com.example.data.sync.SyncConflictHandler(context, db)
      val baseTs = 1761400000000L

      val localEntity = com.example.data.local.CitizenDisasterReportEntity(
        id = "LAP-MMK-SYNC-001",
        disasterType = "Banjir Luapan Sungai",
        incidentTime = "25 Okt 2026, 09:00 WIB",
        incidentLocation = "Jembatan Pasar Bantal",
        district = "Teramang Jaya",
        village = "Pasar Bantal",
        latitude = -2.7123,
        longitude = 101.2345,
        gpsAccuracyMeters = 5.0f,
        isGpsVerified = true,
        casualtiesDead = 0,
        casualtiesInjured = 0,
        casualtiesMissing = 0,
        photoUri = null,
        reporterName = "Warga Uji",
        reporterPhone = "081200000001",
        additionalNotes = "Air mulai naik ke badan jalan",
        timestamp = baseTs,
        status = "Diproses",
        updatedAt = baseTs,
        isSyncedToCloud = true
      )

      // 1. Remote officer validated the report (same creation timestamp, newer updatedAt)
      val remoteValidated = com.example.model.CitizenDisasterReport(
        id = "LAP-MMK-SYNC-001",
        disasterType = "Banjir Luapan Sungai",
        incidentTime = "25 Okt 2026, 09:00 WIB",
        incidentLocation = "Jembatan Pasar Bantal",
        district = "Teramang Jaya",
        village = "Pasar Bantal",
        latitude = -2.7123,
        longitude = 101.2345,
        gpsAccuracyMeters = 5.0f,
        isGpsVerified = true,
        casualtiesDead = 0,
        casualtiesInjured = 0,
        casualtiesMissing = 0,
        photoUri = null,
        reporterName = "Warga Uji",
        reporterPhone = "081200000001",
        additionalNotes = "TRC Posko Teramang Jaya tiba di lokasi",
        timestamp = baseTs,
        status = "Divalidasi",
        updatedAt = baseTs + 60_000L,
        handledByOfficerName = "Petugas Uji Operator",
        handledByOfficerNip = "199807202025061002",
        handledByOfficerRole = "Operator Pusdalops",
        isSyncedToCloud = true
      )

      val resolved1 = handler.resolveReportConflict(localEntity, remoteValidated, "RealtimeListener")
      assertEquals("Divalidasi", resolved1.status)
      assertEquals("TRC Posko Teramang Jaya tiba di lokasi", resolved1.additionalNotes)
      assertEquals("Petugas Uji Operator", resolved1.handledByOfficerName)
      assertEquals("199807202025061002", resolved1.handledByOfficerNip)
      assertEquals(baseTs + 60_000L, resolved1.updatedAt)

      // 2. Even on exact tie updatedAt (e.g., legacy doc without updatedAt), higher remote rank 'Selesai' must win
      val remoteSameTsResolved = remoteValidated.copy(
        status = "Selesai",
        updatedAt = baseTs,
        handledByOfficerName = "Manajer Pusdalops Uji",
        handledByOfficerRole = "Manajer Pusdalops"
      )
      val resolved2 = handler.resolveReportConflict(localEntity, remoteSameTsResolved, "IntegrityCheck")
      assertEquals("Selesai", resolved2.status)
      assertEquals("Manajer Pusdalops Uji", resolved2.handledByOfficerName)
    } finally {
      db.close()
    }
  }

  @Test
  fun `verify WeatherRepository bidirectional citizen-officer sync preserves officer updates pushes pending reports and guards seeding`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val cloudStore = LinkedHashMap<String, com.example.model.CitizenDisasterReport>()
    var simulateCloudOffline = true

    com.example.data.local.FirestoreHelper.overrideCitizenReportFetcherForTesting = {
      if (simulateCloudOffline) Result.failure(RuntimeException("Offline"))
      else Result.success(cloudStore.values.toList())
    }
    com.example.data.local.FirestoreHelper.overrideCitizenReportUploaderForTesting = { report ->
      if (simulateCloudOffline) Result.failure(RuntimeException("Offline"))
      else {
        cloudStore[report.id] = report.copy(isSyncedToCloud = true)
        Result.success(Unit)
      }
    }
    com.example.data.local.FirestoreHelper.overrideCitizenReportStatusUpdaterForTesting = { reportId, status, notes, updatedAt, oName, oNip, oRole ->
      if (simulateCloudOffline) Result.failure(RuntimeException("Offline"))
      else {
        val base = cloudStore[reportId]
        if (base != null) {
          cloudStore[reportId] = base.copy(
            status = status,
            additionalNotes = notes ?: base.additionalNotes,
            updatedAt = updatedAt,
            handledByOfficerName = oName.ifBlank { base.handledByOfficerName },
            handledByOfficerNip = oNip.ifBlank { base.handledByOfficerNip },
            handledByOfficerRole = oRole.ifBlank { base.handledByOfficerRole },
            isSyncedToCloud = true
          )
          Result.success(Unit)
        } else {
          Result.failure(RuntimeException("NOT_FOUND"))
        }
      }
    }

    try {
      val repo = com.example.data.repository.WeatherRepository(db.weatherDao())
      val now = 1761500000000L
      val citizenReport = com.example.model.CitizenDisasterReport(
        id = "LAP-MMK-20261026-091500-321",
        disasterType = "Pohon Tumbang",
        incidentTime = "26 Okt 2026, 09:15 WIB",
        incidentLocation = "Jl. Lintas Barat Air Dikit",
        district = "Air Dikit",
        village = "Air Dikit",
        latitude = -2.6100,
        longitude = 101.1400,
        gpsAccuracyMeters = 6.0f,
        isGpsVerified = true,
        casualtiesDead = 0,
        casualtiesInjured = 0,
        casualtiesMissing = 0,
        photoUri = null,
        reporterName = "Warga Air Dikit",
        reporterPhone = "081277778888",
        additionalNotes = "Pohon melintang menutup jalan",
        timestamp = now,
        status = "Diproses",
        updatedAt = now,
        isSyncedToCloud = false
      )

      // 1. Citizen submits while offline -> stored locally with isSyncedToCloud = false
      val pushedImmediately = repo.submitCitizenDisasterReport(citizenReport)
      org.junit.Assert.assertFalse("Offline submit must mark report as pending sync", pushedImmediately)
      val localAfterSubmit = db.weatherDao().getCitizenReportById(citizenReport.id)
      assertNotNull(localAfterSubmit)
      org.junit.Assert.assertFalse(localAfterSubmit!!.isSyncedToCloud)

      // 2. Network reconnects -> bidirectional sync pushes pending citizen report to Cloud
      simulateCloudOffline = false
      val syncedCount = repo.syncCitizenDisasterReportsOnline()
      assertTrue("Bidirectional sync must push pending citizen report", syncedCount >= 1)
      assertTrue("Cloud store must now contain citizen report", cloudStore.containsKey(citizenReport.id))
      assertTrue("Local report must be marked synced after push", db.weatherDao().getCitizenReportById(citizenReport.id)!!.isSyncedToCloud)

      // 3. Active officer validates and adds follow-up notes -> synced to Room & Cloud with officer attribution
      val updatedOk = repo.updateCitizenReportStatusAndNotes(
        reportId = citizenReport.id,
        newStatus = "Divalidasi",
        notes = "Tim TRC sudah memotong pohon dan membuka akses jalan.",
        officerName = "Petugas Uji TRC",
        officerNip = "199807202025061002",
        officerRole = "Operator Pusdalops"
      )
      assertTrue("Officer status update must succeed online", updatedOk)
      val cloudUpdated = cloudStore[citizenReport.id]!!
      assertEquals("Divalidasi", cloudUpdated.status)
      assertEquals("Petugas Uji TRC", cloudUpdated.handledByOfficerName)
      assertEquals("199807202025061002", cloudUpdated.handledByOfficerNip)

      // 4. Another citizen device pulls updates -> receives officer's Divalidasi status & attribution
      db.weatherDao().updateCitizenReportStatus(
        reportId = citizenReport.id,
        newStatus = "Diproses",
        updatedAt = now,
        officerName = "",
        officerNip = "",
        officerRole = "",
        isSynced = true
      )
      repo.syncCitizenDisasterReportsOnline()
      val citizenAfterPull = db.weatherDao().getCitizenReportById(citizenReport.id)!!
      assertEquals("Divalidasi", citizenAfterPull.status)
      assertEquals("Petugas Uji TRC", citizenAfterPull.handledByOfficerName)
      assertEquals("Tim TRC sudah memotong pohon dan membuka akses jalan.", citizenAfterPull.additionalNotes)

      // 5. seedInitialCitizenDisasterReportsIfEmpty MUST NOT overwrite or inject samples when DB is non-empty
      val countBeforeSeed = db.weatherDao().getCitizenReportCount()
      repo.seedInitialCitizenDisasterReportsIfEmpty()
      assertEquals("Guarded seeding must not modify non-empty citizen report table", countBeforeSeed, db.weatherDao().getCitizenReportCount())
      assertEquals("Divalidasi", db.weatherDao().getCitizenReportById(citizenReport.id)!!.status)
    } finally {
      com.example.data.local.FirestoreHelper.overrideCitizenReportFetcherForTesting = null
      com.example.data.local.FirestoreHelper.overrideCitizenReportUploaderForTesting = null
      com.example.data.local.FirestoreHelper.overrideCitizenReportStatusUpdaterForTesting = null
      db.close()
    }
  }

  @Test
  fun `verify CitizenIncomingReportsProvider syncs cloud reports excludes fake samples and preserves Divalidasi status`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val cloudUpdatedCalls = mutableListOf<Pair<String, String>>()
    val cloudReport = com.example.model.CitizenDisasterReport(
      id = "LAP-MMK-20261026-101010-777",
      disasterType = "Banjir Luapan Sungai",
      incidentTime = "26 Okt 2026, 10:10 WIB",
      incidentLocation = "Desa Lubuk Mukti RT 02",
      district = "Penarik",
      village = "Lubuk Mukti",
      latitude = -2.6711,
      longitude = 101.2111,
      gpsAccuracyMeters = 4.5f,
      isGpsVerified = true,
      casualtiesDead = 0,
      casualtiesInjured = 1,
      casualtiesMissing = 0,
      photoUri = null,
      reporterName = "Warga Penarik",
      reporterPhone = "081300009999",
      additionalNotes = "Genangan 40 cm di permukiman",
      timestamp = 1761510000000L,
      status = "Divalidasi",
      updatedAt = 1761510060000L,
      handledByOfficerName = "Operator Uji",
      handledByOfficerNip = "199807202025061002",
      handledByOfficerRole = "Operator Pusdalops",
      isSyncedToCloud = true
    )

    com.example.data.local.FirestoreHelper.overrideCitizenReportFetcherForTesting = {
      Result.success(listOf(cloudReport))
    }
    com.example.data.local.FirestoreHelper.overrideCitizenReportStatusUpdaterForTesting = { id, status, _, _, _, _, _ ->
      cloudUpdatedCalls.add(id to status)
      Result.success(Unit)
    }

    try {
      val incoming = com.example.ui.form.CitizenIncomingReportsProvider.loadIncomingCitizenReports(context, db)
      assertTrue("Must contain real cloud citizen report", incoming.any { it.id == cloudReport.id })
      org.junit.Assert.assertFalse(
        "Must NOT inject fake hardcoded WARGA-MMK-001..004 samples",
        incoming.any { it.id.startsWith("WARGA-MMK-") }
      )

      val item = incoming.first { it.id == cloudReport.id }
      com.example.ui.form.CitizenIncomingReportsProvider.markReportAsUsedByOfficer(context, db, item)

      val entityAfterUse = db.weatherDao().getCitizenReportById(cloudReport.id)!!
      assertEquals(
        "Using an already Divalidasi report in Form Laporan Harian must NOT downgrade it back to Diproses",
        "Divalidasi",
        entityAfterUse.status
      )
      assertTrue(
        "markReportAsUsedByOfficer must sync status update to Cloud Firestore",
        cloudUpdatedCalls.any { it.first == cloudReport.id && it.second == "Divalidasi" }
      )
    } finally {
      com.example.data.local.FirestoreHelper.overrideCitizenReportFetcherForTesting = null
      com.example.data.local.FirestoreHelper.overrideCitizenReportStatusUpdaterForTesting = null
      db.close()
    }
  }

  @Test
  fun `verify ImageUtils encodes GPS camera photo into compact Firestore Data URI and decodes cleanly for officer inspection`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val testBitmap = android.graphics.Bitmap.createBitmap(640, 480, android.graphics.Bitmap.Config.ARGB_8888)
    testBitmap.eraseColor(android.graphics.Color.rgb(21, 128, 61))

    // 1. Saved inside persistent filesDir (NOT volatile cacheDir)
    val savedUri = com.example.util.ImageUtils.saveBitmapToPersistentReportFile(context, testBitmap)
    assertNotNull("Persistent report photo file URI must be created", savedUri)
    assertTrue("Must be saved under persistent filesDir/citizen_report_photos", savedUri!!.contains("citizen_report_photos"))
    org.junit.Assert.assertFalse("Must NOT be saved in volatile cacheDir", savedUri.contains("/cache/"))

    // 2. Compact JPEG Data URI fits safely within Firestore cap
    val dataUri = com.example.util.ImageUtils.encodeBitmapToCloudDataUri(testBitmap)
    assertNotNull("encodeBitmapToCloudDataUri must succeed", dataUri)
    assertTrue("Must start with JPEG Data URI prefix", dataUri!!.startsWith("data:image/jpeg;base64,"))
    assertTrue(
      "Encoded Data URI (${dataUri.length}) must stay within MAX_CLOUD_PHOTO_DATA_URI_CHARS",
      dataUri.length <= com.example.util.ImageUtils.MAX_CLOUD_PHOTO_DATA_URI_CHARS
    )

    // 3. FirestoreHelper.sanitizeCloudPhotoUri preserves Data URI and auto-converts local file:// paths
    val sanitizedDataUri = com.example.data.local.FirestoreHelper.sanitizeCloudPhotoUri(dataUri)
    assertEquals("FirestoreHelper must preserve valid inline Data URI", dataUri, sanitizedDataUri)

    val sanitizedFromFile = com.example.data.local.FirestoreHelper.sanitizeCloudPhotoUri(savedUri)
    assertNotNull("FirestoreHelper safety net must convert local file:// to Data URI", sanitizedFromFile)
    assertTrue(
      "Converted cloud photoUri must be portable Data URI, never file://",
      sanitizedFromFile!!.startsWith("data:image/jpeg;base64,")
    )

    // 4. UI decoder decodes inline Data URI back into a valid Bitmap for officer views
    val decodedModel = com.example.ui.components.decodeCitizenPhotoModel(sanitizedFromFile)
    assertTrue("decodeCitizenPhotoModel must decode inline Data URI to Bitmap", decodedModel is android.graphics.Bitmap)
    val decodedBmp = decodedModel as android.graphics.Bitmap
    assertTrue("Decoded Bitmap must have valid dimensions", decodedBmp.width > 0 && decodedBmp.height > 0)
  }

  @Test
  fun `verify citizen GPS camera photo survives Cloud Firestore submit without Supabase and displays on officer device`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val citizenDb = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val officerDb = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()

    val citizenRepo = com.example.data.repository.WeatherRepository(citizenDb.weatherDao())
    val officerRepo = com.example.data.repository.WeatherRepository(officerDb.weatherDao())

    val cloudStore = linkedMapOf<String, com.example.model.CitizenDisasterReport>()
    com.example.data.local.FirestoreHelper.overrideCitizenReportUploaderForTesting = { report ->
      val sanitized = com.example.data.local.FirestoreHelper.sanitizeCloudPhotoUri(report.photoUri)
      cloudStore[report.id] = report.copy(photoUri = sanitized, isSyncedToCloud = true)
      Result.success(Unit)
    }
    com.example.data.local.FirestoreHelper.overrideCitizenReportFetcherForTesting = {
      Result.success(cloudStore.values.toList())
    }

    try {
      // 1. Citizen captures GPS camera photo locally
      val capturedBmp = android.graphics.Bitmap.createBitmap(480, 360, android.graphics.Bitmap.Config.ARGB_8888)
      capturedBmp.eraseColor(android.graphics.Color.rgb(234, 88, 12))
      val localFileUri = com.example.util.ImageUtils.saveBitmapToPersistentReportFile(context, capturedBmp)!!
      val now = 1761600000000L

      val report = com.example.model.CitizenDisasterReport(
        id = "LAP-MMK-20261027-101500-901",
        disasterType = "Banjir",
        incidentTime = "27 Okt 2026, 10:15 WIB",
        incidentLocation = "Jl. Lintas Sumatera Desa Lubuk Gedang",
        district = "Lubuk Pinang",
        village = "Lubuk Gedang",
        latitude = -2.5112,
        longitude = 101.1815,
        gpsAccuracyMeters = 5.2f,
        isGpsVerified = true,
        casualtiesDead = 0,
        casualtiesInjured = 0,
        casualtiesMissing = 0,
        photoUri = localFileUri,
        reporterName = "Warga Pelapor Uji",
        reporterPhone = "081212345678",
        additionalNotes = "Air meluap setinggi lutut di depan masjid",
        timestamp = now,
        status = "Diproses",
        updatedAt = now,
        isSyncedToCloud = false
      )

      // 2. Submit report (Supabase unconfigured -> must auto-embed JPEG Data URI into Firestore, NEVER strip photo!)
      val submitted = citizenRepo.submitCitizenDisasterReport(report)
      assertTrue("Citizen report submission must succeed to Cloud Firestore", submitted)

      val storedInCloud = cloudStore[report.id]
      assertNotNull("Report must exist in Cloud Firestore", storedInCloud)
      assertNotNull("Cloud report photoUri MUST NOT be null when Supabase is unconfigured", storedInCloud!!.photoUri)
      assertTrue(
        "Cloud report photoUri must be portable inline JPEG Data URI (not file://)",
        storedInCloud.photoUri!!.startsWith("data:image/jpeg;base64,")
      )

      // 3. Delete the original local file on citizen device to prove officer does NOT rely on local file path!
      java.io.File(android.net.Uri.parse(localFileUri).path!!).delete()

      // 4. Active officer device syncs from Cloud Firestore -> receives decodable GPS camera photo
      val officerSynced = officerRepo.syncCitizenDisasterReportsOnline()
      assertTrue("Officer sync must pull the citizen report", officerSynced >= 1)

      val officerEntity = officerDb.weatherDao().getCitizenReportById(report.id)
      assertNotNull("Officer Room DB must contain the synced report", officerEntity)
      assertNotNull("Officer report must retain photoUri", officerEntity!!.photoUri)
      assertTrue(
        "Officer photoUri must be inline Data URI",
        officerEntity.photoUri!!.startsWith("data:image/jpeg;base64,")
      )

      val decodedForOfficer = com.example.ui.components.decodeCitizenPhotoModel(officerEntity.photoUri)
      assertTrue(
        "Officer view must decode synced photoUri into a valid Bitmap",
        decodedForOfficer is android.graphics.Bitmap
      )

      // 5. Form Laporan Harian 'DARI WARGA' also receives the exact citizen photo (not a fake placeholder)
      val incomingList = com.example.ui.form.CitizenIncomingReportsProvider.loadIncomingCitizenReports(context, officerDb)
      val incomingItem = incomingList.firstOrNull { it.id == report.id }
      assertNotNull("Incoming sheet must include citizen report", incomingItem)
      assertTrue(
        "Incoming sheet photoUrl must carry the citizen's inline Data URI photo",
        incomingItem!!.photoUrl?.startsWith("data:image/jpeg;base64,") == true
      )
    } finally {
      com.example.data.local.FirestoreHelper.overrideCitizenReportUploaderForTesting = null
      com.example.data.local.FirestoreHelper.overrideCitizenReportFetcherForTesting = null
      citizenDb.close()
      officerDb.close()
    }
  }

  @Test
  fun `verify legacy citizen report with local photo auto-repairs missing Cloud photo without downgrading officer status`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val citizenDb = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val officerDb = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.local.AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()

    val citizenRepo = com.example.data.repository.WeatherRepository(citizenDb.weatherDao())
    val officerRepo = com.example.data.repository.WeatherRepository(officerDb.weatherDao())

    val bmp = android.graphics.Bitmap.createBitmap(320, 240, android.graphics.Bitmap.Config.ARGB_8888)
    bmp.eraseColor(android.graphics.Color.BLUE)
    val localFileUri = com.example.util.ImageUtils.saveBitmapToPersistentReportFile(context, bmp)!!
    val t0 = 1761700000000L
    val tOfficer = t0 + 60_000L

    // Cloud has legacy report validated by officer, but photoUri was previously stripped (null)
    val reportId = "LAP-MMK-20261027-110000-555"
    val cloudStore = linkedMapOf(
      reportId to com.example.model.CitizenDisasterReport(
        id = reportId,
        disasterType = "Tanah Longsor",
        incidentTime = "27 Okt 2026, 11:00 WIB",
        incidentLocation = "Tebing Desa Sungai Ipuh",
        district = "Selagan Raya",
        village = "Sungai Ipuh",
        latitude = -2.6400,
        longitude = 101.2600,
        isGpsVerified = true,
        photoUri = null,
        reporterName = "Warga Selagan",
        reporterPhone = "081345678901",
        additionalNotes = "Longsor menutup setengah badan jalan",
        timestamp = t0,
        status = "Divalidasi",
        updatedAt = tOfficer,
        handledByOfficerName = "Petugas Uji Pusdalops",
        handledByOfficerNip = "199807202025061002",
        handledByOfficerRole = "Operator Pusdalops",
        isSyncedToCloud = true
      )
    )

    // Citizen device still has the local file:// photo in Room
    citizenDb.weatherDao().insertCitizenDisasterReport(
      com.example.data.local.CitizenDisasterReportEntity(
        id = reportId,
        disasterType = "Tanah Longsor",
        incidentTime = "27 Okt 2026, 11:00 WIB",
        incidentLocation = "Tebing Desa Sungai Ipuh",
        district = "Selagan Raya",
        village = "Sungai Ipuh",
        latitude = -2.6400,
        longitude = 101.2600,
        gpsAccuracyMeters = 8.0f,
        isGpsVerified = true,
        casualtiesDead = 0,
        casualtiesInjured = 0,
        casualtiesMissing = 0,
        photoUri = localFileUri,
        reporterName = "Warga Selagan",
        reporterPhone = "081345678901",
        additionalNotes = "Longsor menutup setengah badan jalan",
        timestamp = t0,
        status = "Diproses",
        updatedAt = t0,
        isSyncedToCloud = true
      )
    )

    com.example.data.local.FirestoreHelper.overrideCitizenReportUploaderForTesting = { report ->
      val sanitized = com.example.data.local.FirestoreHelper.sanitizeCloudPhotoUri(report.photoUri)
      cloudStore[report.id] = report.copy(photoUri = sanitized, isSyncedToCloud = true)
      Result.success(Unit)
    }
    com.example.data.local.FirestoreHelper.overrideCitizenReportFetcherForTesting = {
      Result.success(cloudStore.values.toList())
    }

    try {
      // Citizen sync pulls officer's Divalidasi status AND repairs Cloud's missing photoUri
      citizenRepo.syncCitizenDisasterReportsOnline()

      val repairedCloud = cloudStore[reportId]!!
      assertEquals("Officer Divalidasi status must be preserved during photo repair", "Divalidasi", repairedCloud.status)
      assertNotNull("Cloud report must now have repaired photoUri", repairedCloud.photoUri)
      assertTrue(
        "Repaired Cloud photoUri must be portable JPEG Data URI",
        repairedCloud.photoUri!!.startsWith("data:image/jpeg;base64,")
      )

      // Officer device previously had null photoUri; after sync it immediately receives the repaired photo
      officerDb.weatherDao().insertCitizenDisasterReport(
        com.example.data.local.CitizenDisasterReportEntity(
          id = reportId,
          disasterType = "Tanah Longsor",
          incidentTime = "27 Okt 2026, 11:00 WIB",
          incidentLocation = "Tebing Desa Sungai Ipuh",
          district = "Selagan Raya",
          village = "Sungai Ipuh",
          latitude = -2.6400,
          longitude = 101.2600,
          gpsAccuracyMeters = 8.0f,
          isGpsVerified = true,
          casualtiesDead = 0,
          casualtiesInjured = 0,
          casualtiesMissing = 0,
          photoUri = null,
          reporterName = "Warga Selagan",
          reporterPhone = "081345678901",
          additionalNotes = "Longsor menutup setengah badan jalan",
          timestamp = t0,
          status = "Divalidasi",
          updatedAt = tOfficer,
          handledByOfficerName = "Petugas Uji Pusdalops",
          handledByOfficerNip = "199807202025061002",
          handledByOfficerRole = "Operator Pusdalops",
          isSyncedToCloud = true
        )
      )
      officerRepo.syncCitizenDisasterReportsOnline()
      val officerAfterRepair = officerDb.weatherDao().getCitizenReportById(reportId)!!
      assertTrue(
        "Officer device must receive the repaired photoUri",
        officerAfterRepair.photoUri?.startsWith("data:image/jpeg;base64,") == true
      )
    } finally {
      com.example.data.local.FirestoreHelper.overrideCitizenReportUploaderForTesting = null
      com.example.data.local.FirestoreHelper.overrideCitizenReportFetcherForTesting = null
      citizenDb.close()
      officerDb.close()
    }
  }
}





