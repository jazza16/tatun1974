package com.example.siren

import android.content.SharedPreferences
import com.example.data.api.CurrentUnitsDto
import com.example.data.api.HourlyDto
import com.example.data.api.OpenMeteoApi
import com.example.data.api.OpenMeteoForecastResponse
import com.example.model.BmkgAutoGempaResponse
import com.example.model.BmkgGempaRaw
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.OffsetDateTime

class SirenDeduplicationAndValidationTest {

    companion object {
        const val SYNTHETIC_AUTOGEMPA_RESOURCE = "autogempa_synthetic.json"
        /**
         * Lokasi khusus yang disiapkan untuk file respons asli BMKG yang disimpan sendiri oleh pengguna:
         * `app/src/test/resources/autogempa_real.json`
         */
        const val REAL_AUTOGEMPA_RESOURCE = "autogempa_real.json"
    }

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private fun loadSyntheticAutoGempaFixture(): BmkgGempaRaw {
        val stream = checkNotNull(
            javaClass.classLoader?.getResourceAsStream(SYNTHETIC_AUTOGEMPA_RESOURCE)
        ) { "File $SYNTHETIC_AUTOGEMPA_RESOURCE tidak ditemukan di test resources" }
        val json = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val response = moshi.adapter(BmkgAutoGempaResponse::class.java).fromJson(json)
        return checkNotNull(response?.infoGempa?.gempa) {
            "Gagal mem-parse objek gempa dari $SYNTHETIC_AUTOGEMPA_RESOURCE"
        }
    }

    /**
     * Memuat file respons asli `app/src/test/resources/autogempa_real.json` jika sudah diletakkan oleh pengguna.
     */
    private fun loadOptionalUserRealAutoGempaFixture(): BmkgGempaRaw? {
        val stream = javaClass.classLoader?.getResourceAsStream(REAL_AUTOGEMPA_RESOURCE) ?: return null
        val json = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }.trim()
        if (json.isEmpty()) return null
        val response = moshi.adapter(BmkgAutoGempaResponse::class.java).fromJson(json)
        return response?.infoGempa?.gempa
    }

    private fun sampleTsunamiPayload(
        alertId: String = "2026-11-18T06:45:12+07:00_-2.58,101.11",
        source: SirenSourceType = SirenSourceType.BMKG_WRS_NTWC,
        confirmed: Boolean = false,
        simulation: Boolean = false,
        eventTimeMillis: Long = 1_700_000_000_000L,
        expiresAtMillis: Long = 1_700_000_000_000L + 30 * 60 * 1000L,
        magnitude: Double? = 7.2,
        coordinates: String? = "-2.58,101.11",
        district: String = "Kota Mukomuko"
    ) = SirenAlertPayload(
        alertId = alertId,
        alertType = SirenAlertType.TSUNAMI,
        title = "Peringatan Dini Tsunami Mukomuko",
        spokenMessageId = "Peringatan dini tsunami untuk pesisir Kabupaten Mukomuko. Segera evakuasi ke tempat tinggi.",
        affectedDistrict = district,
        claimedSource = source,
        verificationStatus = SirenVerificationStatus.VERIFIED_BMKG_PRIMARY,
        confirmedByOfficer = confirmed,
        simulationMode = simulation,
        eventTimestampMillis = eventTimeMillis,
        expiresAtMillis = expiresAtMillis,
        magnitude = magnitude,
        coordinates = coordinates
    )

    @Test
    fun `1 - deduplication runs after validation, allows distinct incidents and more severe updates during cooldown`() {
        val manager = SirenDeduplicationManager(
            sharedPreferences = null,
            defaultCooldownMillis = 180_000L,
            simulationCooldownMillis = 5_000L,
            maxAlertAgeMillis = 1_800_000L
        )
        val baseTime = 1_700_000_000_000L
        val firstAlert = sampleTsunamiPayload(
            alertId = "ALERT-001",
            eventTimeMillis = baseTime,
            magnitude = 6.8,
            coordinates = "-2.58,101.11"
        )

        val firstDecision = manager.evaluateAndRecordAfterValidation(firstAlert, nowMillis = baseTime + 1_000L)
        assertTrue(firstDecision is DeduplicationDecision.Allowed)

        val duplicateDecision = manager.evaluateAndRecordAfterValidation(firstAlert, nowMillis = baseTime + 300_000L)
        assertTrue(duplicateDecision is DeduplicationDecision.RejectedDuplicateId)

        val sameIncidentMinorUpdate = sampleTsunamiPayload(
            alertId = "ALERT-001-REPEAT",
            eventTimeMillis = baseTime + 30_000L,
            magnitude = 6.8,
            coordinates = "-2.58,101.11"
        )
        val cooldownDecision = manager.evaluateAndRecordAfterValidation(sameIncidentMinorUpdate, nowMillis = baseTime + 30_000L)
        assertTrue(cooldownDecision is DeduplicationDecision.RejectedCooldownActive)

        val moreSevereUpdate = sampleTsunamiPayload(
            alertId = "ALERT-001-UPGRADE",
            eventTimeMillis = baseTime + 45_000L,
            magnitude = 7.5,
            coordinates = "-2.58,101.11"
        )
        val severeDecision = manager.evaluateAndRecordAfterValidation(moreSevereUpdate, nowMillis = baseTime + 45_000L)
        assertTrue(severeDecision is DeduplicationDecision.Allowed)

        val distinctIncident = sampleTsunamiPayload(
            alertId = "ALERT-IPUH-002",
            eventTimeMillis = baseTime + 50_000L,
            magnitude = 6.9,
            coordinates = "-3.15,101.55",
            district = "Ipuh"
        )
        val distinctDecision = manager.evaluateAndRecordAfterValidation(distinctIncident, nowMillis = baseTime + 50_000L)
        assertTrue(distinctDecision is DeduplicationDecision.Allowed)

        val expiredAlert = sampleTsunamiPayload(
            alertId = "ALERT-EXPIRED",
            eventTimeMillis = baseTime,
            expiresAtMillis = baseTime + 600_000L
        )
        val expiredDecision = manager.evaluateAndRecordAfterValidation(expiredAlert, nowMillis = baseTime + 601_000L)
        assertTrue(expiredDecision is DeduplicationDecision.RejectedExpired)
    }

    @Test
    fun `2 - validator verifies displaced earthquake in parallel, extracts BMKG timestamp, and does not filter by region name`() = runTest {
        val officialIso = "2026-11-18T06:45:00+07:00"
        val expectedMillis = OffsetDateTime.parse(officialIso).toInstant().toEpochMilli()
        val displacedEventId = "${officialIso}_-5.35,102.27"

        val fakeProvider = object : OfficialBmkgFeedProvider {
            override suspend fun fetchLatestAutoGempa(): BmkgGempaRaw {
                return BmkgGempaRaw(
                    dateTime = "2026-11-18T06:55:00+07:00",
                    coordinates = "-6.10,105.40",
                    magnitude = "5.1",
                    wilayah = "Selat Sunda",
                    potensi = "Tidak berpotensi tsunami"
                )
            }

            override suspend fun fetchRecentGempaTerkini(): List<BmkgGempaRaw> {
                return listOf(
                    BmkgGempaRaw(
                        dateTime = officialIso,
                        coordinates = "-5.35,102.27",
                        magnitude = "7.4",
                        wilayah = "42 km BaratDaya ENGGANO-BENGKULU",
                        potensi = "Berpotensi TSUNAMI untuk diteruskan pada masyarakat"
                    )
                )
            }

            override suspend fun fetchFeltGempaDirasakan(): List<BmkgGempaRaw> = emptyList()
            override suspend fun fetchExtremeWeatherVerification(
                latitude: Double,
                longitude: Double
            ): OfficialWeatherVerificationRecord? = null
        }

        val validator = SirenSourceValidator(fakeProvider)
        val payload = sampleTsunamiPayload(
            alertId = displacedEventId,
            eventTimeMillis = 12345L,
            magnitude = 7.4,
            coordinates = "-5.352, 102.268",
            district = "Pesisir Samudra Hindia"
        )
        val decision = validator.validateIncomingAlert(payload)

        assertTrue(decision.shouldAutoSoundSiren)
        assertEquals(SirenVerificationStatus.VERIFIED_BMKG_HISTORY, decision.validatedPayload.verificationStatus)
        assertEquals(expectedMillis, decision.validatedPayload.eventTimestampMillis)
    }

    @Test
    fun `3 - validator rejects tsunami alert if BMKG record says Tidak berpotensi tsunami even with high magnitude`() = runTest {
        val iso = "2026-11-18T07:00:00+07:00"
        val nonTsunamiHighMagProvider = object : OfficialBmkgFeedProvider {
            override suspend fun fetchLatestAutoGempa(): BmkgGempaRaw {
                return BmkgGempaRaw(
                    dateTime = iso,
                    coordinates = "-2.58,101.11",
                    magnitude = "6.8",
                    wilayah = "Mukomuko",
                    potensi = "Tidak berpotensi tsunami"
                )
            }
            override suspend fun fetchRecentGempaTerkini(): List<BmkgGempaRaw> = emptyList()
            override suspend fun fetchFeltGempaDirasakan(): List<BmkgGempaRaw> = emptyList()
            override suspend fun fetchExtremeWeatherVerification(
                latitude: Double,
                longitude: Double
            ): OfficialWeatherVerificationRecord? = null
        }

        val validator = SirenSourceValidator(nonTsunamiHighMagProvider)
        val decision = validator.validateIncomingAlert(sampleTsunamiPayload(alertId = "${iso}_-2.58,101.11", magnitude = 6.8))

        assertFalse(decision.shouldAutoSoundSiren)
        assertEquals(SirenVerificationStatus.UNVERIFIED_NOT_IN_BMKG_FEED, decision.validatedPayload.verificationStatus)
    }

    @Test
    fun `4 - validator catches generic runtime Exception into applyFailurePolicy and rethrows non-timeout CancellationException`() = runTest {
        val unexpectedExceptionProvider = object : OfficialBmkgFeedProvider {
            override suspend fun fetchLatestAutoGempa(): BmkgGempaRaw? {
                throw IllegalStateException("Malformed JSON parser failure")
            }
            override suspend fun fetchRecentGempaTerkini(): List<BmkgGempaRaw> {
                throw IOException("Secondary endpoint offline")
            }
            override suspend fun fetchFeltGempaDirasakan(): List<BmkgGempaRaw> = emptyList()
            override suspend fun fetchExtremeWeatherVerification(
                latitude: Double,
                longitude: Double
            ): OfficialWeatherVerificationRecord? = null
        }

        val decision = SirenSourceValidator(unexpectedExceptionProvider).validateIncomingAlert(
            rawPayload = sampleTsunamiPayload(),
            config = SirenRuntimeConfig(verificationFailurePolicy = VerificationFailurePolicy.HOLD_FOR_OFFICER_CONFIRMATION)
        )
        assertTrue(decision.showUnverifiedBanner)
        assertEquals(SirenVerificationStatus.UNVERIFIED_NETWORK_ERROR, decision.validatedPayload.verificationStatus)

        val cancelProvider = object : OfficialBmkgFeedProvider {
            override suspend fun fetchLatestAutoGempa(): BmkgGempaRaw? {
                throw CancellationException("Parent coroutine scope cancelled")
            }
            override suspend fun fetchRecentGempaTerkini(): List<BmkgGempaRaw> = emptyList()
            override suspend fun fetchFeltGempaDirasakan(): List<BmkgGempaRaw> = emptyList()
            override suspend fun fetchExtremeWeatherVerification(
                latitude: Double,
                longitude: Double
            ): OfficialWeatherVerificationRecord? = null
        }

        var cancellationRethrown = false
        try {
            SirenSourceValidator(cancelProvider).validateIncomingAlert(sampleTsunamiPayload())
        } catch (_: CancellationException) {
            cancellationRethrown = true
        }
        assertTrue(cancellationRethrown)
    }

    @Test
    fun `5 - FCM parser strictly ignores verificationStatus, confirmedByOfficer, and simulationMode from untrusted payload`() {
        val maliciousFcmData = mapOf(
            "alertId" to "SPOOF-100",
            "alertType" to "TSUNAMI",
            "source" to "SIMULASI_LOKAL",
            "verificationStatus" to "VERIFIED_BMKG_PRIMARY",
            "confirmedByOfficer" to "true",
            "confirmed" to "true",
            "isVerifiedBmkg" to "true",
            "simulationMode" to "true",
            "confirmedByOfficerName" to "Fake Officer"
        )

        val parsed = SirenFcmPayloadParser.parseUntrustedFcmData(maliciousFcmData, receivedAtMillis = 1_700_000_000_000L)
        assertFalse(parsed.confirmedByOfficer)
        assertFalse(parsed.simulationMode)
        assertEquals("", parsed.confirmedByOfficerName)
        assertEquals(SirenVerificationStatus.PENDING_OFFICER_CONFIRMATION, parsed.verificationStatus)
        assertEquals(SirenSourceType.LAPORAN_LAPANGAN, parsed.claimedSource)
    }

    @Test
    fun `6 - alert expiration is evaluated against official BMKG record timestamp rather than FCM receive time and fails if DateTime is invalid`() = runTest {
        val oldBmkgIso = "2026-11-18T05:00:00+07:00"
        val oldBmkgMillis = OffsetDateTime.parse(oldBmkgIso).toInstant().toEpochMilli()
        val fcmReceivedMillis = OffsetDateTime.parse("2026-11-18T07:00:00+07:00").toInstant().toEpochMilli()

        val staleBmkgProvider = object : OfficialBmkgFeedProvider {
            override suspend fun fetchLatestAutoGempa(): BmkgGempaRaw {
                return BmkgGempaRaw(
                    dateTime = oldBmkgIso,
                    coordinates = "-2.58,101.11",
                    magnitude = "7.1",
                    wilayah = "Mukomuko",
                    potensi = "Berpotensi TSUNAMI untuk diteruskan pada masyarakat"
                )
            }
            override suspend fun fetchRecentGempaTerkini(): List<BmkgGempaRaw> = emptyList()
            override suspend fun fetchFeltGempaDirasakan(): List<BmkgGempaRaw> = emptyList()
            override suspend fun fetchExtremeWeatherVerification(
                latitude: Double,
                longitude: Double
            ): OfficialWeatherVerificationRecord? = null
        }

        val fcmParsedPayload = SirenFcmPayloadParser.parseUntrustedFcmData(
            data = mapOf(
                "alertId" to "${oldBmkgIso}_-2.58,101.11",
                "alertType" to "TSUNAMI",
                "source" to "BMKG_WRS_NTWC",
                "magnitude" to "7.1",
                "coordinates" to "-2.58,101.11"
            ),
            receivedAtMillis = fcmReceivedMillis
        )
        assertFalse(fcmParsedPayload.isExpired(nowMillis = fcmReceivedMillis))

        val validationDecision = SirenSourceValidator(staleBmkgProvider).validateIncomingAlert(fcmParsedPayload)
        assertEquals(oldBmkgMillis, validationDecision.validatedPayload.eventTimestampMillis)
        assertTrue(validationDecision.validatedPayload.isExpired(nowMillis = fcmReceivedMillis))

        val dedupManager = SirenDeduplicationManager(sharedPreferences = null)
        val dedupDecision = dedupManager.evaluateAndRecordAfterValidation(
            validatedAlert = validationDecision.validatedPayload,
            nowMillis = fcmReceivedMillis
        )
        assertTrue(dedupDecision is DeduplicationDecision.RejectedExpired)

        val malformedDateProvider = object : OfficialBmkgFeedProvider {
            override suspend fun fetchLatestAutoGempa(): BmkgGempaRaw {
                return BmkgGempaRaw(
                    dateTime = "FORMAT-TANGGAL-RUSAK",
                    coordinates = "-2.58,101.11",
                    magnitude = "7.1",
                    wilayah = "Mukomuko",
                    potensi = "Berpotensi TSUNAMI untuk diteruskan pada masyarakat"
                )
            }
            override suspend fun fetchRecentGempaTerkini(): List<BmkgGempaRaw> = emptyList()
            override suspend fun fetchFeltGempaDirasakan(): List<BmkgGempaRaw> = emptyList()
            override suspend fun fetchExtremeWeatherVerification(
                latitude: Double,
                longitude: Double
            ): OfficialWeatherVerificationRecord? = null
        }
        val malformedPayload = sampleTsunamiPayload(
            alertId = "FORMAT-TANGGAL-RUSAK_-2.58,101.11",
            magnitude = 7.1,
            coordinates = "-2.58,101.11"
        )
        val malformedDecision = SirenSourceValidator(malformedDateProvider).validateIncomingAlert(malformedPayload)
        assertFalse(malformedDecision.shouldAutoSoundSiren)
        assertTrue(malformedDecision.showUnverifiedBanner)
        assertEquals(SirenVerificationStatus.UNVERIFIED_NETWORK_ERROR, malformedDecision.validatedPayload.verificationStatus)
        assertTrue(malformedDecision.validatedPayload.verificationDetailMessage.contains("Gagal mem-parse DateTime BMKG"))
    }

    @Test
    fun `7 - EXTREME_WEATHER verification uses VERIFIED_WEATHER_TELEMETRY status and configurable thresholds`() = runTest {
        val weatherObsIso = "2026-11-18T07:15:00+07:00"
        val expectedObsMillis = OffsetDateTime.parse(weatherObsIso).toInstant().toEpochMilli()

        val extremeWeatherAlert = SirenAlertPayload(
            alertId = "CUACA-EKSTREM-MMK-01",
            alertType = SirenAlertType.EXTREME_WEATHER,
            title = "Peringatan Cuaca Ekstrem Pesisir Mukomuko",
            spokenMessageId = "Peringatan dini cuaca ekstrem badai petir dan angin kencang di Kota Mukomuko.",
            affectedDistrict = "Kota Mukomuko",
            claimedSource = SirenSourceType.BMKG_WRS_NTWC,
            verificationStatus = SirenVerificationStatus.PENDING_OFFICER_CONFIRMATION,
            eventTimestampMillis = 1000L
        )

        val matchingExtremeProvider = object : OfficialBmkgFeedProvider {
            override suspend fun fetchLatestAutoGempa(): BmkgGempaRaw? = null
            override suspend fun fetchRecentGempaTerkini(): List<BmkgGempaRaw> = emptyList()
            override suspend fun fetchFeltGempaDirasakan(): List<BmkgGempaRaw> = emptyList()
            override suspend fun fetchExtremeWeatherVerification(
                latitude: Double,
                longitude: Double
            ): OfficialWeatherVerificationRecord {
                return OfficialWeatherVerificationRecord(
                    observationTimeIso = weatherObsIso,
                    weatherCode = 95,
                    precipitationMm = 24.0,
                    windSpeedKmh = 46.0,
                    latitude = latitude,
                    longitude = longitude
                )
            }
        }
        val extremeDecision = SirenSourceValidator(matchingExtremeProvider).validateIncomingAlert(extremeWeatherAlert)
        assertTrue(extremeDecision.shouldAutoSoundSiren)
        assertEquals(SirenVerificationStatus.VERIFIED_WEATHER_TELEMETRY, extremeDecision.validatedPayload.verificationStatus)
        assertEquals(expectedObsMillis, extremeDecision.validatedPayload.eventTimestampMillis)

        val strictConfig = SirenRuntimeConfig(
            extremeWeatherWmoCodes = setOf(99),
            extremePrecipitationMmThreshold = 50.0,
            extremeWindKmhThreshold = 80.0
        )
        val strictDecision = SirenSourceValidator(matchingExtremeProvider).validateIncomingAlert(extremeWeatherAlert, strictConfig)
        assertFalse(strictDecision.shouldAutoSoundSiren)
        assertEquals(SirenVerificationStatus.UNVERIFIED_NOT_IN_BMKG_FEED, strictDecision.validatedPayload.verificationStatus)

        val missingWeatherFeedProvider = object : OfficialBmkgFeedProvider {
            override suspend fun fetchLatestAutoGempa(): BmkgGempaRaw? = null
            override suspend fun fetchRecentGempaTerkini(): List<BmkgGempaRaw> = emptyList()
            override suspend fun fetchFeltGempaDirasakan(): List<BmkgGempaRaw> = emptyList()
            override suspend fun fetchExtremeWeatherVerification(
                latitude: Double,
                longitude: Double
            ): OfficialWeatherVerificationRecord? = null
        }
        val missingFeedDecision = SirenSourceValidator(missingWeatherFeedProvider).validateIncomingAlert(extremeWeatherAlert)
        assertFalse(missingFeedDecision.shouldAutoSoundSiren)
        assertTrue(missingFeedDecision.showUnverifiedBanner)
        assertEquals(SirenVerificationStatus.UNVERIFIED_NETWORK_ERROR, missingFeedDecision.validatedPayload.verificationStatus)
    }

    @Test
    fun `8 - synthetic autogempa_synthetic json and optional autogempa_real json verify exact match and reject partial alertId`() = runTest {
        val syntheticGempa = loadSyntheticAutoGempaFixture()
        assertNotNull(syntheticGempa.dateTime)

        val syntheticProvider = object : OfficialBmkgFeedProvider {
            override suspend fun fetchLatestAutoGempa(): BmkgGempaRaw = syntheticGempa
            override suspend fun fetchRecentGempaTerkini(): List<BmkgGempaRaw> = emptyList()
            override suspend fun fetchFeltGempaDirasakan(): List<BmkgGempaRaw> = emptyList()
            override suspend fun fetchExtremeWeatherVerification(
                latitude: Double,
                longitude: Double
            ): OfficialWeatherVerificationRecord? = null
        }
        val validator = SirenSourceValidator(syntheticProvider)

        // A. Kecocokan persis dengan autogempa_synthetic.json ("2026-11-18T06:45:12+07:00_-2.58,101.11") -> LULUS
        val exactAlert = sampleTsunamiPayload(
            alertId = "2026-11-18T06:45:12+07:00_-2.58,101.11",
            magnitude = 7.2,
            coordinates = "-2.580, 101.110"
        )
        val exactDecision = validator.validateIncomingAlert(exactAlert)
        assertTrue(exactDecision.shouldAutoSoundSiren)
        assertEquals(SirenVerificationStatus.VERIFIED_BMKG_PRIMARY, exactDecision.validatedPayload.verificationStatus)

        // B. alertId yang HANYA mengandung waktu DateTime BMKG (tanpa format persis "${DateTime}_${Coordinates}") -> DITOLAK
        val partialIdAlert = sampleTsunamiPayload(
            alertId = "BMKG_2026-11-18T06:45:12+07:00_PARTIAL",
            magnitude = 7.2,
            coordinates = "-2.58,101.11"
        )
        val partialIdDecision = validator.validateIncomingAlert(partialIdAlert)
        assertFalse(partialIdDecision.shouldAutoSoundSiren)
        assertEquals(SirenVerificationStatus.UNVERIFIED_NOT_IN_BMKG_FEED, partialIdDecision.validatedPayload.verificationStatus)

        // C. Payload TSUNAMI tanpa magnitude atau tanpa coordinates -> DITOLAK
        val missingMagAlert = exactAlert.copy(magnitude = null)
        assertFalse(validator.validateIncomingAlert(missingMagAlert).shouldAutoSoundSiren)

        val missingCoordsAlert = exactAlert.copy(coordinates = null)
        assertFalse(validator.validateIncomingAlert(missingCoordsAlert).shouldAutoSoundSiren)

        // D. Jika pengguna telah meletakkan file respons asli di `app/src/test/resources/autogempa_real.json`,
        //    uji parsing dan pencocokan terhadap file asli tersebut secara otomatis.
        val optionalRealGempa = loadOptionalUserRealAutoGempaFixture()
        if (optionalRealGempa != null) {
            assertNotNull("Field DateTime pada autogempa_real.json tidak boleh null", optionalRealGempa.dateTime)
            assertNotNull("Field Coordinates pada autogempa_real.json tidak boleh null", optionalRealGempa.coordinates)
            assertNotNull("Field Magnitude pada autogempa_real.json tidak boleh null", optionalRealGempa.magnitude)
            assertNotNull("Field Potensi pada autogempa_real.json tidak boleh null", optionalRealGempa.potensi)
        }
    }

    @Test
    fun `9 - OpenMeteoApi passes Asia_Jakarta timezone and slices 3-hour window starting from current_time instead of 00_00-02_00`() = runTest {
        val hourlyTimes = (0..23).map { hour ->
            String.format("2026-11-18T%02d:00", hour)
        }

        // Kasus A: Jam 00:00-02:00 ekstrem (WMO 95, angin 62 km/jam), tetapi current.time adalah "2026-11-18T14:30"
        // dan jam 14:00, 15:00, 16:00 cerah/tenang (WMO 1, angin 9 km/jam).
        // Harus mengambil 14:00-16:00 (BUKAN 00:00-02:00) sehingga TIDAK memicu sirene cuaca ekstrem!
        val codesMorningStormAfternoonCalm = MutableList(24) { 1 }.apply {
            this[0] = 95
            this[1] = 95
            this[2] = 95
        }
        val windsMorningStormAfternoonCalm = MutableList(24) { 9.0 }.apply {
            this[0] = 62.0
            this[1] = 58.0
            this[2] = 55.0
        }

        var capturedTimezoneCaseA: String? = null
        val fakeWeatherApiCaseA = object : OpenMeteoApi {
            override suspend fun getForecast(
                latitude: Double,
                longitude: Double,
                current: String,
                hourly: String,
                daily: String,
                timezone: String,
                forecastDays: Int
            ): OpenMeteoForecastResponse {
                capturedTimezoneCaseA = timezone
                return OpenMeteoForecastResponse(
                    current = CurrentUnitsDto(
                        temperature2m = 29.0,
                        relativeHumidity2m = 75,
                        apparentTemperature = 32.0,
                        precipitation = 0.0,
                        weatherCode = 1,
                        surfacePressure = 1010.0,
                        windSpeed10m = 9.0,
                        windDirection10m = 180,
                        time = "2026-11-18T14:30"
                    ),
                    hourly = HourlyDto(
                        time = hourlyTimes,
                        temperature2m = List(24) { 29.0 },
                        relativeHumidity2m = List(24) { 75 },
                        precipitationProbability = List(24) { 10 },
                        weatherCode = codesMorningStormAfternoonCalm,
                        windSpeed10m = windsMorningStormAfternoonCalm,
                        uvIndex = List(24) { 5.0 }
                    ),
                    daily = null
                )
            }
        }

        val providerCaseA = RetrofitOfficialBmkgFeedProvider(weatherApi = fakeWeatherApiCaseA)
        val weatherAlert = SirenAlertPayload(
            alertId = "WEATHER-1430-TEST",
            alertType = SirenAlertType.EXTREME_WEATHER,
            title = "Peringatan Cuaca Ekstrem",
            spokenMessageId = "Waspada cuaca ekstrem di Kota Mukomuko.",
            affectedDistrict = "Kota Mukomuko",
            claimedSource = SirenSourceType.BMKG_WRS_NTWC,
            verificationStatus = SirenVerificationStatus.PENDING_OFFICER_CONFIRMATION
        )
        val decisionCaseA = SirenSourceValidator(providerCaseA).validateIncomingAlert(weatherAlert)
        assertEquals("Asia/Jakarta", capturedTimezoneCaseA)
        assertFalse(
            "Cuaca tenang pukul 14:30 tidak boleh terpicu oleh badai masa lalu pukul 00:00-02:00",
            decisionCaseA.shouldAutoSoundSiren
        )
        assertEquals(SirenVerificationStatus.UNVERIFIED_NOT_IN_BMKG_FEED, decisionCaseA.validatedPayload.verificationStatus)

        // Kasus B: Jam 00:00-02:00 tenang (WMO 1, angin 7 km/jam), sedangkan current.time adalah "2026-11-18T14:30"
        // dan pada jam 15:00 (dalam jendela 3 jam ke depan: 14:00, 15:00, 16:00) terjadi badai ekstrem (WMO 96, angin 52 km/jam).
        // Harus mendeteksi badai pada pukul 15:00 dan meloloskan verifikasi cuaca ekstrem!
        val codesMorningCalmAfternoonStorm = MutableList(24) { 1 }.apply {
            this[15] = 96
        }
        val windsMorningCalmAfternoonStorm = MutableList(24) { 7.0 }.apply {
            this[15] = 52.0
        }

        var capturedTimezoneCaseB: String? = null
        val fakeWeatherApiCaseB = object : OpenMeteoApi {
            override suspend fun getForecast(
                latitude: Double,
                longitude: Double,
                current: String,
                hourly: String,
                daily: String,
                timezone: String,
                forecastDays: Int
            ): OpenMeteoForecastResponse {
                capturedTimezoneCaseB = timezone
                return OpenMeteoForecastResponse(
                    current = CurrentUnitsDto(
                        temperature2m = 27.0,
                        relativeHumidity2m = 84,
                        apparentTemperature = 30.0,
                        precipitation = 2.0,
                        weatherCode = 3,
                        surfacePressure = 1006.0,
                        windSpeed10m = 12.0,
                        windDirection10m = 210,
                        time = "2026-11-18T14:30"
                    ),
                    hourly = HourlyDto(
                        time = hourlyTimes,
                        temperature2m = List(24) { 27.0 },
                        relativeHumidity2m = List(24) { 84 },
                        precipitationProbability = List(24) { 80 },
                        weatherCode = codesMorningCalmAfternoonStorm,
                        windSpeed10m = windsMorningCalmAfternoonStorm,
                        uvIndex = List(24) { 3.0 }
                    ),
                    daily = null
                )
            }
        }

        val providerCaseB = RetrofitOfficialBmkgFeedProvider(weatherApi = fakeWeatherApiCaseB)
        val decisionCaseB = SirenSourceValidator(providerCaseB).validateIncomingAlert(weatherAlert)
        assertEquals("Asia/Jakarta", capturedTimezoneCaseB)
        assertTrue(
            "Badai pada pukul 15:00 (dalam jendela 3 jam ke depan dari 14:30) wajib terdeteksi",
            decisionCaseB.shouldAutoSoundSiren
        )
        assertEquals(SirenVerificationStatus.VERIFIED_WEATHER_TELEMETRY, decisionCaseB.validatedPayload.verificationStatus)
        assertEquals(96, decisionCaseB.validatedPayload.weatherCode)
        assertEquals(52.0, decisionCaseB.validatedPayload.windSpeedKmh ?: 0.0, 0.001)
    }

    @Test
    fun `10 - SirenRuntimeConfig clamps out-of-range values and saveRuntimeParameters preserves unspecified values while saving simulationCooldownMillis`() {
        val prefs = InMemorySharedPreferences()

        // 1. Simpan seluruh parameter awal yang valid
        SirenRuntimeConfig.saveRuntimeParametersToPreferences(
            prefs = prefs,
            cooldownWindowMillis = 240_000L,
            simulationCooldownMillis = 12_000L,
            maxAlertAgeMillis = 1_200_000L,
            bmkgVerifyTimeoutMillis = 9_000L,
            verificationFailurePolicy = VerificationFailurePolicy.NOTIFY_WITH_UNVERIFIED_BADGE_NO_SIREN,
            extremeWeatherWmoCodes = setOf(95, 99),
            extremePrecipitationMmThreshold = 25.0,
            extremeWindKmhThreshold = 55.0,
            coordinateToleranceDegrees = 0.08,
            magnitudeTolerance = 0.30
        )

        // 2. Panggil saveRuntimeParameters HANYA untuk mengubah bmkgVerifyTimeoutMillis & simulationCooldownMillis
        // Nilai lain (cooldownWindowMillis, extremePrecipitationMmThreshold, coordinateToleranceDegrees, dll.) TIDAK boleh tertimpa!
        SirenRuntimeConfig.saveRuntimeParametersToPreferences(
            prefs = prefs,
            simulationCooldownMillis = 15_000L,
            bmkgVerifyTimeoutMillis = 11_000L
        )

        val partialReload = SirenRuntimeConfig.loadFromPreferences(prefs)
        assertEquals(240_000L, partialReload.cooldownWindowMillis)
        assertEquals(15_000L, partialReload.simulationCooldownMillis)
        assertEquals(1_200_000L, partialReload.maxAlertAgeMillis)
        assertEquals(11_000L, partialReload.bmkgVerifyTimeoutMillis)
        assertEquals(VerificationFailurePolicy.NOTIFY_WITH_UNVERIFIED_BADGE_NO_SIREN, partialReload.verificationFailurePolicy)
        assertEquals(setOf(95, 99), partialReload.extremeWeatherWmoCodes)
        assertEquals(25.0, partialReload.extremePrecipitationMmThreshold, 0.001)
        assertEquals(55.0, partialReload.extremeWindKmhThreshold, 0.001)
        assertEquals(0.08, partialReload.coordinateToleranceDegrees, 0.0001)
        assertEquals(0.30, partialReload.magnitudeTolerance, 0.0001)

        // 3. Uji validasi/clamping terhadap nilai ekstrem/tidak wajar di SharedPreferences
        prefs.edit()
            .putLong("cooldown_window_millis", -500L) // di bawah MIN_COOLDOWN_MILLIS (10_000L)
            .putLong("simulation_cooldown_millis", 99_999_999L) // di atas MAX_SIMULATION_COOLDOWN_MILLIS (600_000L)
            .putLong("max_alert_age_millis", 100L) // di bawah MIN_MAX_ALERT_AGE_MILLIS (60_000L)
            .putLong("bmkg_verify_timeout_millis", 900_000L) // di atas MAX_BMKG_VERIFY_TIMEOUT_MILLIS (30_000L)
            .putFloat("coordinate_tolerance_degrees", 45.0f) // di atas MAX_COORDINATE_TOLERANCE_DEGREES (0.5)
            .putFloat("magnitude_tolerance", 0.0f) // di bawah MIN_MAGNITUDE_TOLERANCE (0.01)
            .putFloat("extreme_precipitation_mm_threshold", -10.0f) // di bawah MIN_EXTREME_PRECIPITATION_MM (1.0)
            .putFloat("extreme_wind_kmh_threshold", 999.0f) // di atas MAX_EXTREME_WIND_KMH (250.0)
            .putString("extreme_wmo_codes", "-5,999,abc") // kode WMO di luar 0..99 -> fallback ke DEFAULT_EXTREME_WMO_CODES
            .apply()

        val clampedConfig = SirenRuntimeConfig.loadFromPreferences(prefs)
        assertEquals(SirenRuntimeConfig.MIN_COOLDOWN_MILLIS, clampedConfig.cooldownWindowMillis)
        assertEquals(SirenRuntimeConfig.MAX_SIMULATION_COOLDOWN_MILLIS, clampedConfig.simulationCooldownMillis)
        assertEquals(SirenRuntimeConfig.MIN_MAX_ALERT_AGE_MILLIS, clampedConfig.maxAlertAgeMillis)
        assertEquals(SirenRuntimeConfig.MAX_BMKG_VERIFY_TIMEOUT_MILLIS, clampedConfig.bmkgVerifyTimeoutMillis)
        assertEquals(SirenRuntimeConfig.MAX_COORDINATE_TOLERANCE_DEGREES, clampedConfig.coordinateToleranceDegrees, 0.0001)
        assertEquals(SirenRuntimeConfig.MIN_MAGNITUDE_TOLERANCE, clampedConfig.magnitudeTolerance, 0.0001)
        assertEquals(SirenRuntimeConfig.MIN_EXTREME_PRECIPITATION_MM, clampedConfig.extremePrecipitationMmThreshold, 0.001)
        assertEquals(SirenRuntimeConfig.MAX_EXTREME_WIND_KMH, clampedConfig.extremeWindKmhThreshold, 0.001)
        assertEquals(SirenRuntimeConfig.DEFAULT_EXTREME_WMO_CODES, clampedConfig.extremeWeatherWmoCodes)
    }

    @Test
    fun `11a - wind 41 then 42 kmh does not sound siren twice while newly exceeded threshold does`() {
        val manager = SirenDeduplicationManager(
            sharedPreferences = null,
            defaultCooldownMillis = 180_000L,
            simulationCooldownMillis = 5_000L,
            maxAlertAgeMillis = 1_800_000L
        )
        val baseTime = 1_700_000_000_000L

        val wind41Alert = SirenAlertPayload(
            alertId = "WEATHER-WIND-41",
            alertType = SirenAlertType.EXTREME_WEATHER,
            title = "Peringatan Angin Kencang",
            spokenMessageId = "Waspada angin kencang di Kota Mukomuko.",
            affectedDistrict = "Kota Mukomuko",
            claimedSource = SirenSourceType.BMKG_WRS_NTWC,
            verificationStatus = SirenVerificationStatus.VERIFIED_WEATHER_TELEMETRY,
            eventTimestampMillis = baseTime,
            expiresAtMillis = baseTime + 1_800_000L,
            coordinates = "-2.58, 101.11",
            weatherCode = 3,
            windSpeedKmh = 41.0,
            precipitationMm = 2.0
        )

        val firstDecision = manager.evaluateAndRecordAfterValidation(wind41Alert, nowMillis = baseTime + 1_000L)
        assertTrue("Angin 41 km/jam pertama wajib membunyikan sirene", firstDecision is DeduplicationDecision.Allowed)

        // Kenaikan kecil angin dari 41 km/jam menjadi 42 km/jam (sama-sama hanya melampaui ambang angin >= 40 km/jam)
        // TIDAK boleh membunyikan sirene dua kali selama masa cooldown (baik dengan ID baru maupun ID sama).
        val wind42DifferentId = wind41Alert.copy(
            alertId = "WEATHER-WIND-42",
            windSpeedKmh = 42.0
        )
        val secondDecision = manager.evaluateAndRecordAfterValidation(wind42DifferentId, nowMillis = baseTime + 20_000L)
        assertTrue(
            "Angin 41 lalu 42 km/jam tidak boleh membunyikan sirene dua kali dalam cooldown",
            secondDecision is DeduplicationDecision.RejectedCooldownActive
        )

        val wind42SameId = wind41Alert.copy(
            alertId = "WEATHER-WIND-41",
            windSpeedKmh = 42.0
        )
        val sameIdDecision = manager.evaluateAndRecordAfterValidation(wind42SameId, nowMillis = baseTime + 25_000L)
        assertTrue(
            "ID sama dengan kenaikan kecil 41 -> 42 km/jam harus ditolak sebagai duplikat",
            sameIdDecision is DeduplicationDecision.RejectedDuplicateId
        )

        // Namun jika ambang cuaca ekstrem BARU terlampaui (misal curah hujan naik melampaui 15 mm menjadi 20 mm),
        // maka dianggap naik tingkat bermakna dan diizinkan lewat.
        val newlyExceededRainThreshold = wind42DifferentId.copy(
            alertId = "WEATHER-WIND-42-PLUS-EXTREME-RAIN",
            precipitationMm = 20.0
        )
        val newThresholdDecision = manager.evaluateAndRecordAfterValidation(newlyExceededRainThreshold, nowMillis = baseTime + 30_000L)
        assertTrue(
            "Ambang hujan ekstrem yang baru terlampaui wajib diizinkan membunyikan sirene",
            newThresholdDecision is DeduplicationDecision.Allowed
        )
    }

    @Test
    fun `11b - small BMKG coordinate revision and spacing differences normalize to same incidentKey and do not sound siren twice`() {
        val manager = SirenDeduplicationManager(
            sharedPreferences = null,
            defaultCooldownMillis = 180_000L,
            simulationCooldownMillis = 5_000L,
            maxAlertAgeMillis = 1_800_000L
        )
        val baseTime = 1_700_000_000_000L

        // Koordinat awal "-2.58,101.11" -> dibulatkan 1 desimal menjadi "-2.6,101.1"
        val initialBmkgAlert = sampleTsunamiPayload(
            alertId = "2026-11-18T06:45:12+07:00_-2.58,101.11",
            eventTimeMillis = baseTime,
            magnitude = 7.1,
            coordinates = "-2.58,101.11"
        )
        // Revisi kecil koordinat BMKG dengan tambahan spasi " -2.62 , 101.14 " -> dibulatkan 1 desimal tetap "-2.6,101.1"
        // dan kenaikan magnitudo kecil 7.1 -> 7.3 (< 0.5)
        val minorCoordAndMagRevision = sampleTsunamiPayload(
            alertId = "2026-11-18T06:46:00+07:00_-2.62,101.14",
            eventTimeMillis = baseTime + 20_000L,
            magnitude = 7.3,
            coordinates = "  -2.62 ,  101.14  "
        )

        assertEquals("REAL_TSUNAMI_-2.6,101.1", initialBmkgAlert.incidentKey())
        assertEquals(initialBmkgAlert.incidentKey(), minorCoordAndMagRevision.incidentKey())

        val firstDecision = manager.evaluateAndRecordAfterValidation(initialBmkgAlert, nowMillis = baseTime + 1_000L)
        assertTrue(firstDecision is DeduplicationDecision.Allowed)

        val secondDecision = manager.evaluateAndRecordAfterValidation(minorCoordAndMagRevision, nowMillis = baseTime + 25_000L)
        assertTrue(
            "Revisi koordinat kecil BMKG (-2.58,101.11 -> -2.62,101.14) dan magnitudo +0.2 tidak boleh membunyikan sirene dua kali",
            secondDecision is DeduplicationDecision.RejectedCooldownActive
        )
    }

    @Test
    fun `11c - upgrade from PENDING_OFFICER_CONFIRMATION or UNVERIFIED to CONFIRMED_BY_OFFICER passes deduplication and cooldown`() {
        assertTrue(
            "CONFIRMED_BY_OFFICER wajib memiliki verificationRank lebih tinggi dari PENDING_OFFICER_CONFIRMATION",
            SirenVerificationStatus.CONFIRMED_BY_OFFICER.verificationRank >
                SirenVerificationStatus.PENDING_OFFICER_CONFIRMATION.verificationRank
        )
        assertTrue(
            "CONFIRMED_BY_OFFICER wajib memiliki verificationRank lebih tinggi dari UNVERIFIED_TIMEOUT",
            SirenVerificationStatus.CONFIRMED_BY_OFFICER.verificationRank >
                SirenVerificationStatus.UNVERIFIED_TIMEOUT.verificationRank
        )
        assertTrue(
            "CONFIRMED_BY_OFFICER wajib memiliki verificationRank lebih tinggi dari UNVERIFIED_NETWORK_ERROR",
            SirenVerificationStatus.CONFIRMED_BY_OFFICER.verificationRank >
                SirenVerificationStatus.UNVERIFIED_NETWORK_ERROR.verificationRank
        )
        assertTrue(
            "CONFIRMED_BY_OFFICER wajib memiliki verificationRank lebih tinggi dari UNVERIFIED_NOT_IN_BMKG_FEED",
            SirenVerificationStatus.CONFIRMED_BY_OFFICER.verificationRank >
                SirenVerificationStatus.UNVERIFIED_NOT_IN_BMKG_FEED.verificationRank
        )

        val manager = SirenDeduplicationManager(
            sharedPreferences = null,
            defaultCooldownMillis = 180_000L,
            simulationCooldownMillis = 5_000L,
            maxAlertAgeMillis = 1_800_000L
        )
        val baseTime = 1_700_000_000_000L

        val pendingAlert = sampleTsunamiPayload(
            alertId = "FIELD-REPORT-001",
            source = SirenSourceType.LAPORAN_LAPANGAN,
            confirmed = false,
            eventTimeMillis = baseTime,
            magnitude = 7.0,
            coordinates = "-2.58,101.11"
        ).copy(
            verificationStatus = SirenVerificationStatus.PENDING_OFFICER_CONFIRMATION
        )

        val firstDecision = manager.evaluateAndRecordAfterValidation(pendingAlert, nowMillis = baseTime + 1_000L)
        assertTrue(firstDecision is DeduplicationDecision.Allowed)

        // Skenario 1 (alertId sama): Saat petugas mengonfirmasi peringatan yang sama (alertId sama dan masih dalam masa cooldown),
        // status naik dari PENDING_OFFICER_CONFIRMATION -> CONFIRMED_BY_OFFICER sehingga WAJIB lolos!
        val confirmedSameIdAlert = pendingAlert.copy(
            verificationStatus = SirenVerificationStatus.CONFIRMED_BY_OFFICER,
            confirmedByOfficer = true,
            confirmedByOfficerName = "RURI IRWANDI, ST, MT"
        )
        val confirmedSameIdDecision = manager.evaluateAndRecordAfterValidation(confirmedSameIdAlert, nowMillis = baseTime + 10_000L)
        assertTrue(
            "Kenaikan status dari PENDING_OFFICER_CONFIRMATION ke CONFIRMED_BY_OFFICER wajib lolos meskipun alertId sama & dalam masa cooldown",
            confirmedSameIdDecision is DeduplicationDecision.Allowed
        )

        // Skenario 2 (alertId berbeda pada insiden/lokasi yang sama):
        // Peringatan pertama masuk dengan status PENDING_OFFICER_CONFIRMATION (alertId = "FIELD-REPORT-IPUH-PENDING"),
        // lalu 15 detik kemudian (masih dalam masa cooldown 180 detik) masuk pembaruan dengan alertId BERBEDA
        // ("FIELD-REPORT-IPUH-CONFIRMED-02") yang berstatus CONFIRMED_BY_OFFICER pada lokasi & magnitudo yang sama.
        val pendingIpuhAlert = sampleTsunamiPayload(
            alertId = "FIELD-REPORT-IPUH-PENDING",
            source = SirenSourceType.LAPORAN_LAPANGAN,
            confirmed = false,
            eventTimeMillis = baseTime + 20_000L,
            magnitude = 6.9,
            coordinates = "-3.15,101.55",
            district = "Ipuh"
        ).copy(
            verificationStatus = SirenVerificationStatus.PENDING_OFFICER_CONFIRMATION
        )
        val pendingIpuhDecision = manager.evaluateAndRecordAfterValidation(pendingIpuhAlert, nowMillis = baseTime + 21_000L)
        assertTrue(pendingIpuhDecision is DeduplicationDecision.Allowed)

        val confirmedDifferentIdAlert = pendingIpuhAlert.copy(
            alertId = "FIELD-REPORT-IPUH-CONFIRMED-02",
            verificationStatus = SirenVerificationStatus.CONFIRMED_BY_OFFICER,
            confirmedByOfficer = true,
            confirmedByOfficerName = "RURI IRWANDI, ST, MT"
        )
        val confirmedDifferentIdDecision = manager.evaluateAndRecordAfterValidation(
            confirmedDifferentIdAlert,
            nowMillis = baseTime + 35_000L
        )
        assertTrue(
            "Kenaikan status dari PENDING_OFFICER_CONFIRMATION ke CONFIRMED_BY_OFFICER dengan alertId berbeda wajib menembus cooldown insiden",
            confirmedDifferentIdDecision is DeduplicationDecision.Allowed
        )
    }

    @Test
    fun `11d - SIMULASI alarm never blocks subsequent REAL alarm at the same location`() {
        val manager = SirenDeduplicationManager(
            sharedPreferences = null,
            defaultCooldownMillis = 180_000L,
            simulationCooldownMillis = 60_000L,
            maxAlertAgeMillis = 1_800_000L
        )
        val baseTime = 1_700_000_000_000L

        val simulationAlert = sampleTsunamiPayload(
            alertId = "SIM-TSUNAMI-001",
            source = SirenSourceType.SIMULASI_LOKAL,
            simulation = true,
            eventTimeMillis = baseTime,
            magnitude = 7.5,
            coordinates = "-2.58,101.11",
            district = "Kota Mukomuko"
        ).copy(
            verificationStatus = SirenVerificationStatus.LOCAL_SIMULATION
        )

        val realAlert = sampleTsunamiPayload(
            alertId = "REAL-TSUNAMI-001",
            source = SirenSourceType.BMKG_WRS_NTWC,
            simulation = false,
            eventTimeMillis = baseTime + 2_000L,
            magnitude = 7.0,
            coordinates = "-2.58,101.11",
            district = "Kota Mukomuko"
        ).copy(
            verificationStatus = SirenVerificationStatus.VERIFIED_BMKG_PRIMARY
        )

        val simDecision = manager.evaluateAndRecordAfterValidation(simulationAlert, nowMillis = baseTime + 1_000L)
        assertTrue("Alarm SIMULASI pertama harus diizinkan", simDecision is DeduplicationDecision.Allowed)

        // Hanya 2 detik setelah SIMULASI (masih di dalam simulationCooldownMillis = 60 detik maupun defaultCooldownMillis = 180 detik),
        // alarm REAL masuk di koordinat yang persis sama (bahkan dengan magnitudo lebih kecil 7.0 vs 7.5).
        // Alarm REAL TIDAK BOLEH terhalang oleh alarm SIMULASI!
        val realDecision = manager.evaluateAndRecordAfterValidation(realAlert, nowMillis = baseTime + 3_000L)
        assertTrue(
            "Alarm SIMULASI tidak boleh menghalangi alarm REAL pada lokasi dan waktu yang berdekatan",
            realDecision is DeduplicationDecision.Allowed
        )
    }

    /**
     * Implementasi SharedPreferences in-memory ringan untuk menguji persistensi & clamping secara deterministik di JVM.
     */
    private class InMemorySharedPreferences : SharedPreferences {
        private val map = LinkedHashMap<String, Any?>()

        override fun getAll(): MutableMap<String, *> = LinkedHashMap(map)
        override fun getString(key: String?, defValue: String?): String? = map[key] as? String ?: defValue
        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            (map[key] as? Set<String>)?.toMutableSet() ?: defValues
        override fun getInt(key: String?, defValue: Int): Int = map[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = map[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = map[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = map.containsKey(key)
        override fun edit(): SharedPreferences.Editor = EditorImpl()
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        private inner class EditorImpl : SharedPreferences.Editor {
            private val staged = LinkedHashMap<String, Any?>()
            private var clearRequested = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply { if (key != null) staged[key] = value }
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = apply { if (key != null) staged[key] = values?.toSet() }
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply { if (key != null) staged[key] = value }
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply { if (key != null) staged[key] = value }
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply { if (key != null) staged[key] = value }
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply { if (key != null) staged[key] = value }
            override fun remove(key: String?): SharedPreferences.Editor = apply { if (key != null) staged[key] = null }
            override fun clear(): SharedPreferences.Editor = apply { clearRequested = true }
            override fun commit(): Boolean { apply(); return true }
            override fun apply() {
                if (clearRequested) map.clear()
                for ((k, v) in staged) {
                    if (v == null) map.remove(k) else map[k] = v
                }
            }
        }
    }
}
