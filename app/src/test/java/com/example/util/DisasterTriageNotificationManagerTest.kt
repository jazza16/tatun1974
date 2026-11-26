package com.example.util

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.KarhutlaHotspotEntity
import com.example.data.local.OfficerAuthManager
import com.example.data.local.OfficerRoles
import com.example.data.repository.PoskoLogisticCommodity
import com.example.model.AirQuality
import com.example.model.BmkgEarthquake
import com.example.model.CitizenDisasterReport
import com.example.model.CurrentWeather
import com.example.model.DetailedEarlyWarning
import com.example.model.EarthquakeUrgency
import com.example.model.MarineWeather
import com.example.model.MukomukoDistricts
import com.example.model.TidalData
import com.example.model.WeatherFullReport
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DisasterTriageNotificationManagerTest {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager
    private lateinit var officerAuthManager: OfficerAuthManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        Shadows.shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
        officerAuthManager = OfficerAuthManager(context)
        officerAuthManager.clearSavedOfficerIdentity()
        DisasterTriageNotificationManager.clearCooldownHistory(context)
        EarthquakeNotificationHelper.clearNotifiedHistory(context)
        DisasterTriageNotificationManager.saveSettings(context, TriageNotificationSettings())
    }

    @After
    fun tearDown() {
        officerAuthManager.clearSavedOfficerIdentity()
        DisasterTriageNotificationManager.clearCooldownHistory(context)
        EarthquakeNotificationHelper.clearNotifiedHistory(context)
    }

    private fun formatFreshIso(epochMillis: Long): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+07:00", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("GMT+7")
        }
        return sdf.format(java.util.Date(epochMillis))
    }

    private fun formatFreshDate(epochMillis: Long): String {
        val sdf = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("GMT+7")
        }
        return sdf.format(java.util.Date(epochMillis))
    }

    private fun formatFreshTimeWib(epochMillis: Long): String {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss 'WIB'", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("GMT+7")
        }
        return sdf.format(java.util.Date(epochMillis))
    }

    @Test
    fun allFeatureEvaluators_mapDomainMetricsToExactColorTriageLevels() {
        val district = MukomukoDistricts.defaultDistrict

        // 1. Early Warnings: AWAS -> MERAH, SIAGA -> ORANYE, WASPADA -> KUNING, empty -> HIJAU
        val awasSignal = DisasterTriageNotificationManager.evaluateEarlyWarnings(
            warnings = listOf(
                DetailedEarlyWarning(
                    id = "w1",
                    severity = "AWAS",
                    category = "Badai Petir",
                    title = "Banjir Bandang & Angin Kencang",
                    description = "Curah hujan ekstrem",
                    affectedDistricts = listOf(district.name),
                    validUntil = "22:00 WIB",
                    safetyInstructions = listOf("Evakuasi ke tempat aman")
                )
            ),
            districtName = district.name
        )
        assertEquals(DisasterTriageLevel.MERAH_AWAS, awasSignal.level)
        assertEquals(4, awasSignal.resolvedTargetTab)

        val emptyWarningSignal = DisasterTriageNotificationManager.evaluateEarlyWarnings(emptyList(), district.name)
        assertEquals(DisasterTriageLevel.HIJAU_AMAN, emptyWarningSignal.level)

        // 2. Marine Waves: >= 4.0m MERAH, >= 2.5m ORANYE, >= 1.25m KUNING, < 1.25m HIJAU
        assertEquals(
            DisasterTriageLevel.MERAH_AWAS,
            DisasterTriageNotificationManager.evaluateMarineWave(
                MarineWeather(4.2, "Sangat Tinggi", 25.0, "Pasang", "Bahaya Melaut", "Larangan melaut"),
                district.name
            ).level
        )
        assertEquals(
            DisasterTriageLevel.ORANYE_SIAGA,
            DisasterTriageNotificationManager.evaluateMarineWave(
                MarineWeather(2.8, "Tinggi", 18.0, "Pasang", "Tunda Melaut", "Tunda berlayar"),
                district.name
            ).level
        )
        assertEquals(
            DisasterTriageLevel.KUNING_WASPADA,
            DisasterTriageNotificationManager.evaluateMarineWave(
                MarineWeather(1.5, "Sedang", 12.0, "Normal", "Waspada Perahu Kecil", "Hati-hati"),
                district.name
            ).level
        )
        assertEquals(
            DisasterTriageLevel.HIJAU_AMAN,
            DisasterTriageNotificationManager.evaluateMarineWave(
                MarineWeather(0.8, "Tenang", 6.0, "Surut", "Aman Melaut", "Kondisi kondusif"),
                district.name
            ).level
        )

        // 3. Tidal Rob: Awas -> MERAH, Siaga -> ORANYE, Waspada -> KUNING, Rendah -> HIJAU
        assertEquals(
            DisasterTriageLevel.ORANYE_SIAGA,
            DisasterTriageNotificationManager.evaluateTidalRob(
                TidalData(
                    stationName = "Pantai Mukomuko",
                    currentWaterLevelMeters = 2.2f,
                    waterLevelPercent = 0.9f,
                    isRisingTide = true,
                    tideMovementText = "Sedang Menuju Pasang Naik",
                    nextEventTime = "18:00 WIB",
                    nextEventType = "Pasang Tertinggi",
                    highTideMorningTime = "06:45 WIB",
                    highTideMorningHeight = "2.1 m",
                    highTideEveningTime = "19:15 WIB",
                    highTideEveningHeight = "2.4 m",
                    lowTideNoonTime = "13:10 WIB",
                    lowTideNoonHeight = "0.5 m",
                    lowTideNightTime = "01:25 WIB",
                    lowTideNightHeight = "0.4 m",
                    lunarPhaseName = "Purnama",
                    tidalCycleType = "Semi-diurnal",
                    estuarineCrossingAdvice = "Waspada muara",
                    robFloodRiskLevel = "Siaga",
                    hourlyLevels = emptyList()
                ),
                district.name
            ).level
        )

        // 4. Air Quality: AQI >= 201 MERAH, >= 151 ORANYE, >= 101 KUNING, < 101 HIJAU
        assertEquals(
            DisasterTriageLevel.MERAH_AWAS,
            DisasterTriageNotificationManager.evaluateAirQuality(
                AirQuality(aqi = 235, pm25 = 165.0, pm10 = 210.0, status = "Sangat Tidak Sehat", recommendation = "Gunakan N95"),
                district.name
            ).level
        )
        assertEquals(
            DisasterTriageLevel.ORANYE_SIAGA,
            DisasterTriageNotificationManager.evaluateAirQuality(
                AirQuality(aqi = 168, pm25 = 72.0, pm10 = 110.0, status = "Tidak Sehat", recommendation = "Kurangi aktivitas luar"),
                district.name
            ).level
        )
        assertEquals(
            DisasterTriageLevel.HIJAU_AMAN,
            DisasterTriageNotificationManager.evaluateAirQuality(
                AirQuality(aqi = 38, pm25 = 9.0, pm10 = 15.0, status = "Baik", recommendation = "Aman"),
                district.name
            ).level
        )

        // 5. Earthquake & Tsunami: Tsunami routes to Tab 6 (Evakuasi) with MERAH_AWAS
        val eqNow = System.currentTimeMillis()
        val tsunamiEventMs = eqNow - 10 * 60_000L
        val tsunamiEq = BmkgEarthquake(
            id = "eq_tsunami_1",
            date = formatFreshDate(tsunamiEventMs),
            timeWib = formatFreshTimeWib(tsunamiEventMs),
            dateTimeIso = formatFreshIso(tsunamiEventMs),
            magnitude = 7.4,
            depth = "10 km",
            region = "70 km Barat Daya Mukomuko",
            latitude = -2.8,
            longitude = 100.8,
            potentialTsunami = "BERPOTENSI TSUNAMI",
            feltDescription = "V-VI MMI Mukomuko",
            shakemapUrl = null,
            distanceKmToMukomuko = 70,
            isNearMukomuko = true,
            urgencyLevel = EarthquakeUrgency.CRITICAL
        )
        val tsunamiSignal = DisasterTriageNotificationManager.evaluateEarthquake(tsunamiEq, nowMillis = eqNow)
        assertEquals(DisasterTriageLevel.MERAH_AWAS, tsunamiSignal.level)
        assertEquals(6, tsunamiSignal.resolvedTargetTab)

        // 6. Karhutla Hotspots: ignores simulation, detects HIGH/matched -> MERAH_AWAS on Tab 5
        val simHotspot = KarhutlaHotspotEntity(
            sourceId = "SIM|1",
            sourceType = KarhutlaHotspotEntity.SOURCE_FIRMS,
            satellite = "VIIRS",
            acqDateTime = System.currentTimeMillis(),
            lat = -2.58,
            lon = 101.12,
            confidenceLabel = "HIGH",
            brightnessKelvin = 355.0,
            frpMw = 40.0,
            nearestSubDistrict = "Kota Mukomuko",
            isSimulation = true
        )
        assertEquals(
            DisasterTriageLevel.HIJAU_AMAN,
            DisasterTriageNotificationManager.evaluateKarhutlaHotspots(listOf(simHotspot)).level
        )

        val realHigh1 = simHotspot.copy(sourceId = "REAL|1", isSimulation = false, isMatchedWithCrossSource = true)
        val karhutlaSignal = DisasterTriageNotificationManager.evaluateKarhutlaHotspots(listOf(realHigh1))
        assertEquals(DisasterTriageLevel.MERAH_AWAS, karhutlaSignal.level)
        assertEquals(5, karhutlaSignal.resolvedTargetTab)

        // 7. Weather Telemetry: severe wind/thunderstorm -> MERAH_AWAS
        val stormReport = WeatherFullReport(
            district = district,
            current = CurrentWeather(
                temperature = 25.0,
                feelsLike = 24.0,
                humidity = 92,
                precipitation = 35.0,
                weatherCode = 96,
                surfacePressure = 1002.0,
                windSpeed = 65.0,
                windDirection = 220,
                time = "2026-11-25T14:00"
            ),
            hourly = emptyList(),
            daily = emptyList(),
            marine = MarineWeather(1.0, "Tenang", 6.0, "Normal", "Aman", "Aman"),
            airQuality = AirQuality(35, 8.0, 14.0, "Baik", "Aman"),
            alert = null
        )
        assertEquals(
            DisasterTriageLevel.MERAH_AWAS,
            DisasterTriageNotificationManager.evaluateWeatherTelemetry(stormReport).level
        )

        // 8. Citizen Disaster Report Surge: >= 3 critical active reports -> MERAH_AWAS
        val criticalReports = (1..3).map { idx ->
            CitizenDisasterReport(
                id = "rep_$idx",
                disasterType = "Banjir Bandang",
                incidentTime = "14:00 WIB",
                incidentLocation = "Jalan Lintas Desa $idx",
                district = district.name,
                village = "Desa $idx",
                latitude = -2.58,
                longitude = 101.12,
                casualtiesInjured = 4,
                reporterName = "Warga $idx",
                reporterPhone = "0812000000$idx",
                additionalNotes = "Kondisi darurat air naik cepat",
                status = "Diproses"
            )
        }
        assertEquals(
            DisasterTriageLevel.MERAH_AWAS,
            DisasterTriageNotificationManager.evaluateDisasterReportSurge(criticalReports).level
        )
    }

    @Test
    fun greenSafeSignal_neverPostsNotification() {
        val safeSignal = DisasterTriageNotificationManager.evaluateMarineWave(
            MarineWeather(0.7, "Tenang", 5.0, "Surut", "Aman Melaut", "Aman"),
            "Kota Mukomuko"
        )
        val result = DisasterTriageNotificationManager.evaluateAndNotify(context, safeSignal)
        assertEquals(TriageDispatchResult.SKIPPED_SAFE_GREEN, result)
        assertEquals(0, Shadows.shadowOf(notificationManager).allNotifications.size)
    }

    @Test
    fun thresholdFiltering_andEscalationBypassCooldown_workCorrectly() {
        val baseTime = 1_760_000_000_000L

        // Default threshold is ORANYE_SIAGA -> KUNING should be skipped
        val yellowSignal = DisasterTriageSignal(
            source = TriageFeatureSource.MARITIM_GELOMBANG,
            level = DisasterTriageLevel.KUNING_WASPADA,
            districtOrLocation = "Kota Mukomuko",
            headline = "Waspada Gelombang Sedang",
            metricSummary = "Gelombang 1.6 m",
            safetyAdvice = "Waspada",
            signatureKey = "marine_kuning_1"
        )
        assertEquals(
            TriageDispatchResult.SKIPPED_BELOW_THRESHOLD,
            DisasterTriageNotificationManager.evaluateAndNotify(context, yellowSignal, nowMillis = baseTime)
        )

        // Lower threshold to KUNING_WASPADA -> now KUNING posts!
        DisasterTriageNotificationManager.saveSettings(
            context,
            TriageNotificationSettings(minimumLevel = DisasterTriageLevel.KUNING_WASPADA)
        )
        assertEquals(
            TriageDispatchResult.POSTED,
            DisasterTriageNotificationManager.evaluateAndNotify(context, yellowSignal, nowMillis = baseTime)
        )

        // Duplicate within cooldown (5 minutes later) -> skipped
        assertEquals(
            TriageDispatchResult.SKIPPED_COOLDOWN_DUPLICATE,
            DisasterTriageNotificationManager.evaluateAndNotify(context, yellowSignal, nowMillis = baseTime + 5 * 60_000L)
        )

        // Escalation to ORANYE_SIAGA 1 minute later -> immediately bypasses cooldown and posts!
        val orangeSignal = yellowSignal.copy(
            level = DisasterTriageLevel.ORANYE_SIAGA,
            headline = "Siaga Gelombang Tinggi",
            metricSummary = "Gelombang 2.8 m",
            signatureKey = "marine_oranye_2"
        )
        assertEquals(
            TriageDispatchResult.POSTED,
            DisasterTriageNotificationManager.evaluateAndNotify(context, orangeSignal, nowMillis = baseTime + 6 * 60_000L)
        )

        // Escalation to MERAH_AWAS 1 minute later -> immediately bypasses cooldown and posts on RED channel!
        val redSignal = yellowSignal.copy(
            level = DisasterTriageLevel.MERAH_AWAS,
            headline = "Awas Gelombang Ekstrem",
            metricSummary = "Gelombang 4.3 m",
            signatureKey = "marine_merah_3"
        )
        assertEquals(
            TriageDispatchResult.POSTED,
            DisasterTriageNotificationManager.evaluateAndNotify(context, redSignal, nowMillis = baseTime + 7 * 60_000L)
        )

        val postedNotif = Shadows.shadowOf(notificationManager).getNotification(
            DisasterTriageNotificationManager.notificationIdForSource(TriageFeatureSource.MARITIM_GELOMBANG)
        )
        assertNotNull("Notifikasi Triase Merah harus terpasang di NotificationManager", postedNotif)
        assertEquals(DisasterTriageNotificationManager.CHANNEL_TRIAGE_MERAH, postedNotif.channelId)
        assertEquals(DisasterTriageLevel.MERAH_AWAS.colorInt, postedNotif.color)
        assertTrue(
            "Notifikasi Merah wajib menyertakan tombol darurat 117",
            postedNotif.actions?.any { it.title.toString().contains("117") } == true
        )
    }

    @Test
    fun poskoLogisticsTriageAlert_strictlyBlockedInCitizenMode_andPostedWhenOfficerLoggedIn() {
        val criticalStock = listOf(
            PoskoLogisticCommodity(
                code = "LOG-BERAS",
                name = "Beras Darurat",
                category = "PANGAN",
                unit = "Karung",
                currentStock = 4, // < 10 -> KRITIS (MERAH)
                minThreshold = 10,
                poskoCode = "POSKO-MM-001"
            )
        )
        val poskoSignal = DisasterTriageNotificationManager.evaluatePoskoLogistics(criticalStock)
        assertEquals(DisasterTriageLevel.MERAH_AWAS, poskoSignal.level)
        assertTrue(poskoSignal.source.requiresOfficerSession)

        // Citizen Mode (no officer logged in) -> MUST be blocked!
        assertEquals(
            TriageDispatchResult.SKIPPED_OFFICER_SESSION_REQUIRED,
            DisasterTriageNotificationManager.evaluateAndNotify(context, poskoSignal)
        )
        assertEquals(0, Shadows.shadowOf(notificationManager).allNotifications.size)

        // Officer Logged In -> MUST be posted!
        officerAuthManager.loginOfficerDirectly(
            name = "HITATUN ARAZAK",
            badgeId = "197401282006041006",
            shift = "Shift Siaga 24 Jam",
            role = OfficerRoles.MANAJER
        )
        assertEquals(
            TriageDispatchResult.POSTED,
            DisasterTriageNotificationManager.evaluateAndNotify(context, poskoSignal)
        )
        val posted = Shadows.shadowOf(notificationManager).getNotification(
            DisasterTriageNotificationManager.notificationIdForSource(TriageFeatureSource.POSKO_LOGISTIK_KRITIS)
        )
        assertNotNull(posted)
        assertEquals(DisasterTriageLevel.MERAH_AWAS.colorInt, posted.color)
    }

    @Test
    fun testTriagePreviewNotifications_postForKuningOranyeMerah_withExactChannelAndColor() {
        assertTrue(
            DisasterTriageNotificationManager.postTestTriageNotification(
                context = context,
                level = DisasterTriageLevel.KUNING_WASPADA,
                districtName = "Kota Mukomuko"
            )
        )
        val yellowNotif = Shadows.shadowOf(notificationManager).getNotification(
            DisasterTriageNotificationManager.notificationIdForSource(TriageFeatureSource.KUALITAS_UDARA)
        )
        assertNotNull(yellowNotif)
        assertEquals(DisasterTriageNotificationManager.CHANNEL_TRIAGE_KUNING, yellowNotif.channelId)
        assertEquals(DisasterTriageLevel.KUNING_WASPADA.colorInt, yellowNotif.color)

        assertTrue(
            DisasterTriageNotificationManager.postTestTriageNotification(
                context = context,
                level = DisasterTriageLevel.ORANYE_SIAGA,
                districtName = "Kota Mukomuko"
            )
        )
        val orangeNotif = Shadows.shadowOf(notificationManager).getNotification(
            DisasterTriageNotificationManager.notificationIdForSource(TriageFeatureSource.MARITIM_GELOMBANG)
        )
        assertNotNull(orangeNotif)
        assertEquals(DisasterTriageNotificationManager.CHANNEL_TRIAGE_ORANYE, orangeNotif.channelId)
        assertEquals(DisasterTriageLevel.ORANYE_SIAGA.colorInt, orangeNotif.color)

        assertTrue(
            DisasterTriageNotificationManager.postTestTriageNotification(
                context = context,
                level = DisasterTriageLevel.MERAH_AWAS,
                districtName = "Kota Mukomuko"
            )
        )
        val redNotif = Shadows.shadowOf(notificationManager).getNotification(
            DisasterTriageNotificationManager.notificationIdForSource(TriageFeatureSource.PERINGATAN_CUACA)
        )
        assertNotNull(redNotif)
        assertEquals(DisasterTriageNotificationManager.CHANNEL_TRIAGE_MERAH, redNotif.channelId)
        assertEquals(DisasterTriageLevel.MERAH_AWAS.colorInt, redNotif.color)
    }

    @Test
    fun globalIndonesiaEarthquakesOutsideBengkulu_areStrictlySuppressedAndNeverNotify() {
        val now = System.currentTimeMillis()
        val recentEventMs = now - 15 * 60_000L
        val outsideEarthquakes = listOf(
            // Gempa besar & berpotensi tsunami di Papua (luar Bengkulu) -> HARUS dibisukan
            BmkgEarthquake(
                id = "eq_papua_68",
                date = formatFreshDate(recentEventMs),
                timeWib = formatFreshTimeWib(recentEventMs),
                dateTimeIso = formatFreshIso(recentEventMs),
                latitude = -2.53,
                longitude = 140.71,
                magnitude = 6.8,
                depth = "10 km",
                region = "82 km Timur Laut JAYAPURA-PAPUA",
                potentialTsunami = "BERPOTENSI TSUNAMI",
                feltDescription = "V MMI Jayapura, IV MMI Sarmi",
                shakemapUrl = null,
                distanceKmToMukomuko = 4380,
                isNearMukomuko = false,
                urgencyLevel = EarthquakeUrgency.CRITICAL
            ),
            // Gempa M 6.2 di Maluku -> HARUS dibisukan
            BmkgEarthquake(
                id = "eq_maluku_62",
                date = formatFreshDate(recentEventMs),
                timeWib = formatFreshTimeWib(recentEventMs),
                dateTimeIso = formatFreshIso(recentEventMs),
                latitude = -3.69,
                longitude = 128.18,
                magnitude = 6.2,
                depth = "25 km",
                region = "95 km Barat Daya AMBON-MALUKU",
                potentialTsunami = "Tidak berpotensi tsunami",
                feltDescription = "IV MMI Ambon",
                shakemapUrl = null,
                distanceKmToMukomuko = 2990,
                isNearMukomuko = false,
                urgencyLevel = EarthquakeUrgency.CRITICAL
            ),
            // Gempa M 5.6 di Jawa Barat -> HARUS dibisukan
            BmkgEarthquake(
                id = "eq_jabar_56",
                date = formatFreshDate(recentEventMs),
                timeWib = formatFreshTimeWib(recentEventMs),
                dateTimeIso = formatFreshIso(recentEventMs),
                latitude = -7.35,
                longitude = 107.12,
                magnitude = 5.6,
                depth = "15 km",
                region = "52 km Barat Daya GARUT-JABAR",
                potentialTsunami = "Tidak berpotensi tsunami",
                feltDescription = "III-IV MMI Bandung, III MMI Garut",
                shakemapUrl = null,
                distanceKmToMukomuko = 850,
                isNearMukomuko = false,
                urgencyLevel = EarthquakeUrgency.WARNING
            ),
            // Gempa provinsi tetangga (Lubuklinggau Sumsel) yang TIDAK dirasakan di Bengkulu -> HARUS dibisukan
            BmkgEarthquake(
                id = "eq_sumsel_48",
                date = formatFreshDate(recentEventMs),
                timeWib = formatFreshTimeWib(recentEventMs),
                dateTimeIso = formatFreshIso(recentEventMs),
                latitude = -3.29,
                longitude = 102.86,
                magnitude = 4.8,
                depth = "12 km",
                region = "18 km Timur Laut LUBUKLINGGAU-SUMSEL",
                potentialTsunami = "Tidak berpotensi tsunami",
                feltDescription = "III MMI Lubuklinggau",
                shakemapUrl = null,
                distanceKmToMukomuko = 205,
                isNearMukomuko = true,
                urgencyLevel = EarthquakeUrgency.ADVISORY
            )
        )

        DisasterTriageNotificationManager.saveSettings(
            context,
            TriageNotificationSettings(minimumLevel = DisasterTriageLevel.KUNING_WASPADA)
        )

        outsideEarthquakes.forEach { eq ->
            org.junit.Assert.assertFalse(
                "Gempa luar Bengkulu (${eq.region}) tidak boleh ditandai sebagai terdampak Provinsi Bengkulu",
                eq.isBengkuluProvinceAffected
            )
            val signal = DisasterTriageNotificationManager.evaluateEarthquake(eq, nowMillis = now)
            assertEquals(DisasterTriageLevel.HIJAU_AMAN, signal.level)
            assertEquals(
                TriageDispatchResult.SKIPPED_SAFE_GREEN,
                DisasterTriageNotificationManager.evaluateAndNotify(context, signal, nowMillis = now)
            )
            org.junit.Assert.assertFalse(
                EarthquakeNotificationHelper.postEarthquakeNotification(context, eq, nowMillis = now)
            )
            org.junit.Assert.assertFalse(
                EarthquakeNotificationHelper.postTsunamiWarningNotification(context, eq, nowMillis = now)
            )
        }

        assertEquals(
            "Tidak boleh ada satu pun notifikasi gempa yang muncul untuk gempa luar Provinsi Bengkulu",
            0,
            Shadows.shadowOf(notificationManager).allNotifications.size
        )
    }

    @Test
    fun bengkuluLocalAndFeltEarthquakes_postTriageAndDirectNotificationsAccurately() {
        DisasterTriageNotificationManager.saveSettings(
            context,
            TriageNotificationSettings(minimumLevel = DisasterTriageLevel.KUNING_WASPADA)
        )

        val now = System.currentTimeMillis()
        val engganoEventMs = now - 20 * 60_000L
        val mentawaiEventMs = now - 10 * 60_000L
        val curupEventMs = now - 5 * 60_000L

        // 1. Gempa lokal di Pulau Enggano / Bengkulu Utara (M 5.3) -> ORANYE / MERAH dan memicu notifikasi
        val engganoEq = BmkgEarthquake(
            id = "eq_enggano_53",
            date = formatFreshDate(engganoEventMs),
            timeWib = formatFreshTimeWib(engganoEventMs),
            dateTimeIso = formatFreshIso(engganoEventMs),
            latitude = -5.35,
            longitude = 102.25,
            magnitude = 5.3,
            depth = "16 km",
            region = "24 km Barat Daya ENGGANO-BENGKULU",
            potentialTsunami = "Tidak berpotensi tsunami",
            feltDescription = "III-IV MMI Enggano, II-III MMI Kota Bengkulu",
            shakemapUrl = null,
            distanceKmToMukomuko = 330,
            isNearMukomuko = false,
            urgencyLevel = EarthquakeUrgency.WARNING
        )
        assertTrue(engganoEq.isBengkuluProvinceAffected)
        val engganoSignal = DisasterTriageNotificationManager.evaluateEarthquake(engganoEq, nowMillis = now)
        assertEquals(DisasterTriageLevel.ORANYE_SIAGA, engganoSignal.level)
        assertEquals(
            TriageDispatchResult.POSTED,
            DisasterTriageNotificationManager.evaluateAndNotify(context, engganoSignal, nowMillis = now)
        )
        assertTrue(EarthquakeNotificationHelper.postEarthquakeNotification(context, engganoEq, nowMillis = now))

        // 2. Gempa episenter di luar (Mentawai Sumbar) TETAPI dirasakan masyarakat di Mukomuko & Curup -> LOLOS!
        val feltFromMentawai = BmkgEarthquake(
            id = "eq_mentawai_felt_mmk",
            date = formatFreshDate(mentawaiEventMs),
            timeWib = formatFreshTimeWib(mentawaiEventMs),
            dateTimeIso = formatFreshIso(mentawaiEventMs),
            latitude = -2.05,
            longitude = 99.65,
            magnitude = 5.4,
            depth = "20 km",
            region = "65 km Tenggara KEP-MENTAWAI-SUMBAR",
            potentialTsunami = "Tidak berpotensi tsunami",
            feltDescription = "IV MMI Tuapejat, III MMI Mukomuko, II MMI Curup",
            shakemapUrl = null,
            distanceKmToMukomuko = 175,
            isNearMukomuko = true,
            urgencyLevel = EarthquakeUrgency.WARNING
        )
        assertTrue(feltFromMentawai.isBengkuluProvinceAffected)
        val feltSignal = DisasterTriageNotificationManager.evaluateEarthquake(feltFromMentawai, nowMillis = now)
        assertEquals(DisasterTriageLevel.MERAH_AWAS, feltSignal.level)

        // 3. Evaluasi daftar gabungan (autogempa Papua + gempa dirasakan lokal Rejang Lebong Curup M 4.1)
        val papuaEq = BmkgEarthquake(
            id = "eq_papua_latest",
            date = formatFreshDate(now - 2 * 60_000L),
            timeWib = formatFreshTimeWib(now - 2 * 60_000L),
            dateTimeIso = formatFreshIso(now - 2 * 60_000L),
            latitude = -2.53,
            longitude = 140.71,
            magnitude = 6.5,
            depth = "10 km",
            region = "70 km Timur Laut JAYAPURA-PAPUA",
            potentialTsunami = "Tidak berpotensi tsunami",
            feltDescription = "IV MMI Jayapura",
            shakemapUrl = null,
            distanceKmToMukomuko = 4380,
            isNearMukomuko = false,
            urgencyLevel = EarthquakeUrgency.CRITICAL
        )
        val curupLocalEq = BmkgEarthquake(
            id = "eq_curup_41",
            date = formatFreshDate(curupEventMs),
            timeWib = formatFreshTimeWib(curupEventMs),
            dateTimeIso = formatFreshIso(curupEventMs),
            latitude = -3.47,
            longitude = 102.52,
            magnitude = 4.1,
            depth = "10 km",
            region = "14 km Barat Daya REJANG LEBONG-BENGKULU",
            potentialTsunami = "Tidak berpotensi tsunami",
            feltDescription = "III MMI Curup, II MMI Kepahiang",
            shakemapUrl = null,
            distanceKmToMukomuko = 185,
            isNearMukomuko = false,
            urgencyLevel = EarthquakeUrgency.ADVISORY
        )
        val combinedSignal = DisasterTriageNotificationManager.evaluateEarthquakesForBengkulu(
            earthquakes = listOf(papuaEq, curupLocalEq),
            nowMillis = now
        )
        assertEquals(DisasterTriageLevel.KUNING_WASPADA, combinedSignal.level)
        assertTrue(combinedSignal.districtOrLocation.contains("REJANG LEBONG", ignoreCase = true))
    }

    @Test
    fun staleAndFallbackEarthquakes_areBlockedAndNeverNotify() {
        val now = System.currentTimeMillis()
        val staleEventMs = now - (3L * 60L * 60_000L) // 3 jam lalu (> jendela maks 2 jam)

        val fallbackMukomuko = BmkgEarthquake(
            id = "gempa_mukomuko_latest",
            date = "16 Okt 2026",
            timeWib = "17:42:10 WIB",
            dateTimeIso = "2026-10-16T10:42:10+00:00",
            latitude = -2.85,
            longitude = 101.02,
            magnitude = 5.2,
            depth = "18 km",
            region = "42 km BaratDaya MUKOMUKO-BENGKULU",
            potentialTsunami = "Tidak berpotensi tsunami",
            feltDescription = "III-IV Mukomuko",
            shakemapUrl = null,
            distanceKmToMukomuko = 42,
            isNearMukomuko = true,
            urgencyLevel = EarthquakeUrgency.CRITICAL,
            isFallbackData = true
        )

        val staleRealEvent = BmkgEarthquake(
            id = "eq_mmk_old_3h",
            date = formatFreshDate(staleEventMs),
            timeWib = formatFreshTimeWib(staleEventMs),
            dateTimeIso = formatFreshIso(staleEventMs),
            latitude = -2.85,
            longitude = 101.02,
            magnitude = 5.2,
            depth = "18 km",
            region = "42 km BaratDaya MUKOMUKO-BENGKULU",
            potentialTsunami = "Tidak berpotensi tsunami",
            feltDescription = "III-IV Mukomuko",
            shakemapUrl = null,
            distanceKmToMukomuko = 42,
            isNearMukomuko = true,
            urgencyLevel = EarthquakeUrgency.CRITICAL,
            isFallbackData = false
        )

        listOf(fallbackMukomuko, staleRealEvent).forEach { eq ->
            val signal = DisasterTriageNotificationManager.evaluateEarthquake(eq, nowMillis = now)
            assertEquals(DisasterTriageLevel.HIJAU_AMAN, signal.level)
            assertEquals(
                TriageDispatchResult.SKIPPED_SAFE_GREEN,
                DisasterTriageNotificationManager.evaluateAndNotify(context, signal, nowMillis = now)
            )
            org.junit.Assert.assertFalse(
                EarthquakeNotificationHelper.postEarthquakeNotification(context, eq, nowMillis = now)
            )
        }
        assertEquals(0, Shadows.shadowOf(notificationManager).allNotifications.size)
    }

    @Test
    fun freshBengkuluEarthquake_includesDate_has1To2HourTimeout_andNeverRepeats() {
        val now = System.currentTimeMillis()
        val eventMs = now - (15L * 60_000L) // 15 menit lalu
        val freshEq = BmkgEarthquake(
            id = "eq_mmk_fresh_52",
            date = formatFreshDate(eventMs),
            timeWib = formatFreshTimeWib(eventMs),
            dateTimeIso = formatFreshIso(eventMs),
            latitude = -2.85,
            longitude = 101.02,
            magnitude = 5.2,
            depth = "18 km",
            region = "42 km BaratDaya MUKOMUKO-BENGKULU",
            potentialTsunami = "Tidak berpotensi tsunami",
            feltDescription = "III-IV Mukomuko",
            shakemapUrl = null,
            distanceKmToMukomuko = 42,
            isNearMukomuko = true,
            urgencyLevel = EarthquakeUrgency.CRITICAL
        )

        val signal = DisasterTriageNotificationManager.evaluateEarthquake(freshEq, nowMillis = now)
        assertEquals(DisasterTriageLevel.MERAH_AWAS, signal.level)
        assertTrue(
            "Ringkasan metrik gempa harus mencantumkan tanggal kejadian",
            signal.metricSummary.contains(freshEq.date)
        )
        assertTrue(
            "Label tanggal & waktu event harus tersedia",
            signal.eventDateTimeLabel?.contains(freshEq.date) == true
        )
        val timeoutMs = signal.timeoutAfterMillis ?: 0L
        assertTrue(
            "Durasi tampil notifikasi gempa harus berada dalam rentang 1–2 jam (${timeoutMs} ms)",
            timeoutMs in com.example.model.BengkuluEarthquakeScope.MIN_EARTHQUAKE_DISPLAY_TIMEOUT_MS..
                com.example.model.BengkuluEarthquakeScope.MAX_EARTHQUAKE_NOTIFICATION_WINDOW_MS
        )

        // Dispatch pertama -> POSTED
        assertEquals(
            TriageDispatchResult.POSTED,
            DisasterTriageNotificationManager.evaluateAndNotify(context, signal, nowMillis = now)
        )
        val postedNotif = Shadows.shadowOf(notificationManager).getNotification(
            DisasterTriageNotificationManager.notificationIdForSource(TriageFeatureSource.GEMPA_TSUNAMI)
        )
        assertNotNull(postedNotif)
        val bigText = postedNotif.extras.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        assertTrue("BigText notifikasi gempa wajib mencantumkan tanggal kejadian", bigText.contains(freshEq.date))
        assertTrue(
            "Android notification timeoutAfter harus diset 1–2 jam",
            postedNotif.timeoutAfter in com.example.model.BengkuluEarthquakeScope.MIN_EARTHQUAKE_DISPLAY_TIMEOUT_MS..
                com.example.model.BengkuluEarthquakeScope.MAX_EARTHQUAKE_NOTIFICATION_WINDOW_MS
        )

        // Dispatch ulang setelah 20 menit & 75 menit (melewati cooldown 15m MERAH) -> TETAP SKIPPED!
        assertEquals(
            TriageDispatchResult.SKIPPED_COOLDOWN_DUPLICATE,
            DisasterTriageNotificationManager.evaluateAndNotify(context, signal, nowMillis = now + 20L * 60_000L)
        )
        assertEquals(
            TriageDispatchResult.SKIPPED_COOLDOWN_DUPLICATE,
            DisasterTriageNotificationManager.evaluateAndNotify(context, signal, nowMillis = now + 75L * 60_000L)
        )

        // Helper langsung juga tidak boleh mengulang event yang sama
        assertTrue(EarthquakeNotificationHelper.postEarthquakeNotification(context, freshEq, nowMillis = now))
        org.junit.Assert.assertFalse(
            EarthquakeNotificationHelper.postEarthquakeNotification(context, freshEq, nowMillis = now + 30L * 60_000L)
        )
    }

    @Test
    fun onlySelectedDistrictSetting_persistsAndFiltersEarlyWarnings() {
        val settings = TriageNotificationSettings(onlySelectedDistrict = true)
        DisasterTriageNotificationManager.saveSettings(context, settings)
        assertTrue(DisasterTriageNotificationManager.getSettings(context).onlySelectedDistrict)

        DisasterTriageNotificationManager.saveSettings(context, settings.copy(onlySelectedDistrict = false))
        org.junit.Assert.assertFalse(DisasterTriageNotificationManager.getSettings(context).onlySelectedDistrict)

        val ipuhOnlyWarning = DetailedEarlyWarning(
            id = "w_ipuh_only",
            severity = "SIAGA",
            category = "Hujan Lebat",
            title = "Siaga Hujan Lebat Kec. Ipuh",
            description = "Curah hujan tinggi terlokalisir di Ipuh",
            affectedDistricts = listOf("Ipuh"),
            validUntil = "21:00 WIB",
            safetyInstructions = listOf("Waspada genangan")
        )

        // Saat onlySelectedDistrict = true untuk Kota Mukomuko -> Hening (HIJAU_AMAN)
        val filteredOut = DisasterTriageNotificationManager.evaluateEarlyWarnings(
            warnings = listOf(ipuhOnlyWarning),
            districtName = "Kota Mukomuko",
            onlySelectedDistrict = true
        )
        assertEquals(DisasterTriageLevel.HIJAU_AMAN, filteredOut.level)

        // Saat onlySelectedDistrict = false (15 Kecamatan Kab. Mukomuko) -> ORANYE_SIAGA
        val countyWide = DisasterTriageNotificationManager.evaluateEarlyWarnings(
            warnings = listOf(ipuhOnlyWarning),
            districtName = "Kota Mukomuko",
            onlySelectedDistrict = false
        )
        assertEquals(DisasterTriageLevel.ORANYE_SIAGA, countyWide.level)
    }
}
