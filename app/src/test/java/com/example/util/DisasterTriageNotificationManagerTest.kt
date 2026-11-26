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
        DisasterTriageNotificationManager.saveSettings(context, TriageNotificationSettings())
    }

    @After
    fun tearDown() {
        officerAuthManager.clearSavedOfficerIdentity()
        DisasterTriageNotificationManager.clearCooldownHistory(context)
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
        val tsunamiEq = BmkgEarthquake(
            id = "eq_tsunami_1",
            date = "25 Nov 2026",
            timeWib = "14:00:00 WIB",
            dateTimeIso = "2026-11-25T14:00:00+07:00",
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
        val tsunamiSignal = DisasterTriageNotificationManager.evaluateEarthquake(tsunamiEq)
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
}
