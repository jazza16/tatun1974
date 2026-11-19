package com.example.siren

import com.example.data.api.BmkgEarthquakeApi
import com.example.data.api.BmkgNetworkClient
import com.example.data.api.NetworkClient
import com.example.data.api.OpenMeteoApi
import com.example.data.api.OpenMeteoForecastResponse
import com.example.model.BmkgGempaRaw
import com.example.model.MukomukoDistricts
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Rekaman telemetri cuaca ekstrem resmi untuk memverifikasi peringatan `EXTREME_WEATHER`.
 */
data class OfficialWeatherVerificationRecord(
    val observationTimeIso: String,
    val weatherCode: Int,
    val precipitationMm: Double,
    val windSpeedKmh: Double,
    val latitude: Double,
    val longitude: Double
)

/**
 * Keputusan akhir setelah memvalidasi sumber peringatan secara independen.
 */
data class SourceValidationDecision(
    val validatedPayload: SirenAlertPayload,
    val shouldAutoSoundSiren: Boolean,
    val showUnverifiedBanner: Boolean,
    val requiresOfficerConfirmation: Boolean,
    val reasonSummary: String
)

/**
 * Abstraksi penyedia feed resmi BMKG & telemetri meteorologi agar mudah diuji (unit test).
 */
interface OfficialBmkgFeedProvider {
    suspend fun fetchLatestAutoGempa(): BmkgGempaRaw?
    suspend fun fetchRecentGempaTerkini(): List<BmkgGempaRaw>
    suspend fun fetchFeltGempaDirasakan(): List<BmkgGempaRaw>
    suspend fun fetchExtremeWeatherVerification(latitude: Double, longitude: Double): OfficialWeatherVerificationRecord?
}

/**
 * Implementasi produksi yang memanggil endpoint HTTPS resmi BMKG (`data.bmkg.go.id/DataMKG/TEWS/`)
 * serta endpoint prakiraan/observasi meteorologi wilayah Kabupaten Mukomuko dengan `timezone = "Asia/Jakarta"`.
 */
class RetrofitOfficialBmkgFeedProvider(
    private val bmkgApi: BmkgEarthquakeApi = BmkgNetworkClient.api,
    private val weatherApi: OpenMeteoApi = NetworkClient.weatherApi
) : OfficialBmkgFeedProvider {
    override suspend fun fetchLatestAutoGempa(): BmkgGempaRaw? {
        return bmkgApi.getAutoGempa().infoGempa?.gempa
    }

    override suspend fun fetchRecentGempaTerkini(): List<BmkgGempaRaw> {
        return bmkgApi.getRecentEarthquakes().infoGempa?.gempaList.orEmpty()
    }

    override suspend fun fetchFeltGempaDirasakan(): List<BmkgGempaRaw> {
        return bmkgApi.getFeltEarthquakes().infoGempa?.gempaList.orEmpty()
    }

    override suspend fun fetchExtremeWeatherVerification(
        latitude: Double,
        longitude: Double
    ): OfficialWeatherVerificationRecord? {
        // Meminta forecastDays = 2 dengan timezone eksplisit "Asia/Jakarta" agar jika current.time
        // berada pada pukul 22:00-23:45 WIB, jendela 3 jam ke depan tetap tersedia lintas tengah malam.
        val response = weatherApi.getForecast(
            latitude = latitude,
            longitude = longitude,
            timezone = JAKARTA_TIMEZONE_ID,
            forecastDays = 2
        )
        return extractWeatherVerificationRecord(
            response = response,
            latitude = latitude,
            longitude = longitude,
            forwardWindowHours = DEFAULT_FORWARD_HOURLY_WINDOW
        )
    }

    companion object {
        const val JAKARTA_TIMEZONE_ID = "Asia/Jakarta"
        const val DEFAULT_FORWARD_HOURLY_WINDOW = 3
        private val JAKARTA_ZONE: ZoneId = ZoneId.of(JAKARTA_TIMEZONE_ID)

        /**
         * Mem-parsing `current.time` dan menyelaraskan indeks awal pada `hourly.time` (zona waktu `Asia/Jakarta`)
         * sehingga pengambilan `forwardWindowHours` (default 3 jam) selalu dimulai dari jam `current.time` ke depan
         * (bukan mengambil indeks 0..2 / pukul 00:00-02:00 WIB dari array `hourly`).
         */
        fun extractWeatherVerificationRecord(
            response: OpenMeteoForecastResponse,
            latitude: Double,
            longitude: Double,
            forwardWindowHours: Int = DEFAULT_FORWARD_HOURLY_WINDOW
        ): OfficialWeatherVerificationRecord? {
            val current = response.current ?: return null
            val currentTimeIso = current.time?.trim().orEmpty()
            val currentMillis = parseWeatherTimestampMillis(currentTimeIso)

            val currentFloorHourMillis = currentMillis?.let { millis ->
                Instant.ofEpochMilli(millis)
                    .atZone(JAKARTA_ZONE)
                    .truncatedTo(ChronoUnit.HOURS)
                    .toInstant()
                    .toEpochMilli()
            }

            val hourlyDto = response.hourly
            val hourlyTimes = hourlyDto?.time.orEmpty()

            // Cari indeks jam pertama pada hourly.time yang >= jam current.time (dibulatkan ke bawah per jam)
            val startIndex = if (currentFloorHourMillis != null && hourlyTimes.isNotEmpty()) {
                hourlyTimes.indexOfFirst { slotIso ->
                    val slotMillis = parseWeatherTimestampMillis(slotIso)
                    slotMillis != null && slotMillis >= currentFloorHourMillis
                }
            } else {
                -1
            }

            val windowSize = forwardWindowHours.coerceAtLeast(1)
            val forwardWindSlice: List<Double> = if (startIndex >= 0) {
                hourlyDto?.windSpeed10m
                    ?.drop(startIndex)
                    ?.take(windowSize)
                    ?.filterNotNull()
                    .orEmpty()
            } else {
                emptyList()
            }

            val forwardCodeSlice: List<Int> = if (startIndex >= 0) {
                hourlyDto?.weatherCode
                    ?.drop(startIndex)
                    ?.take(windowSize)
                    ?.filterNotNull()
                    .orEmpty()
            } else {
                emptyList()
            }

            val maxForwardWind = forwardWindSlice.maxOrNull() ?: 0.0
            val maxForwardCode = forwardCodeSlice.maxOrNull() ?: 0

            return OfficialWeatherVerificationRecord(
                observationTimeIso = currentTimeIso,
                weatherCode = maxOf(current.weatherCode ?: 0, maxForwardCode),
                precipitationMm = current.precipitation ?: 0.0,
                windSpeedKmh = maxOf(current.windSpeed10m ?: 0.0, maxForwardWind),
                latitude = latitude,
                longitude = longitude
            )
        }

        internal fun parseWeatherTimestampMillis(rawTime: String?): Long? {
            if (rawTime.isNullOrBlank()) return null
            val trimmed = rawTime.trim()
            return try {
                OffsetDateTime.parse(trimmed).toInstant().toEpochMilli()
            } catch (_: Exception) {
                try {
                    LocalDateTime.parse(trimmed)
                        .atZone(JAKARTA_ZONE)
                        .toInstant()
                        .toEpochMilli()
                } catch (_: Exception) {
                    null
                }
            }
        }
    }
}

/**
 * Validator Sumber Peringatan Dini berprinsip Zero-Trust terhadap payload FCM:
 * 1. Menangkap seluruh `Exception` (dan me-rethrow `CancellationException` non-timeout serta tidak menangkap `Error` JVM)
 *    lalu mengarahkannya ke `applyFailurePolicy`.
 * 2. Melakukan fetch paralel (`autogempa.json`, `gempaterkini.json`, `gempadirasakan.json`) menggunakan
 *    `supervisorScope` + `async` + `catchExceptionOnly`.
 * 3. Mengambil `eventTimestampMillis` otoritatif langsung dari record BMKG terverifikasi; apabila parse
 *    `DateTime` BMKG gagal pada `TSUNAMI`, tidak pernah fallback ke waktu payload melainkan diarahkan ke
 *    `applyFailurePolicy` dengan alasan jelas.
 * 4. Untuk `TSUNAMI`: mewajibkan `alertId` cocok persis (`${DateTime}_${Coordinates}`), mewajibkan `magnitude`
 *    dan `coordinates` ada di payload, membandingkan koordinat secara numerik dengan toleransi derajat, dan
 *    mewajibkan `Potensi` di feed BMKG menyatakan potensi tsunami.
 * 5. Untuk `EXTREME_WEATHER`: memberikan status verifikasi terpisah `VERIFIED_WEATHER_TELEMETRY` dan menggunakan
 *    ambang cuaca yang dikonfigurasi di `SirenRuntimeConfig`.
 */
class SirenSourceValidator(
    private val bmkgFeedProvider: OfficialBmkgFeedProvider = RetrofitOfficialBmkgFeedProvider()
) {

    suspend fun validateIncomingAlert(
        rawPayload: SirenAlertPayload,
        config: SirenRuntimeConfig = SirenRuntimeConfig()
    ): SourceValidationDecision {
        val safeConfig = config.clamped()

        // 1. Mode SIMULASI lokal (hanya dipicu dari dalam perangkat petugas yang terautentikasi)
        if (rawPayload.simulationMode || rawPayload.claimedSource == SirenSourceType.SIMULASI_LOKAL) {
            val simPayload = rawPayload.copy(
                claimedSource = SirenSourceType.SIMULASI_LOKAL,
                verificationStatus = SirenVerificationStatus.LOCAL_SIMULATION,
                verificationDetailMessage = "Uji coba lokal perangkat petugas (SIMULASI)."
            )
            return SourceValidationDecision(
                validatedPayload = simPayload,
                shouldAutoSoundSiren = true,
                showUnverifiedBanner = false,
                requiresOfficerConfirmation = false,
                reasonSummary = "Mode SIMULASI lokal petugas."
            )
        }

        // 2. Sumber Non-BMKG: tidak pernah bunyi otomatis kecuali sudah dikonfirmasi petugas (confirmed == true)
        if (rawPayload.claimedSource != SirenSourceType.BMKG_WRS_NTWC) {
            return if (rawPayload.confirmedByOfficer) {
                val confirmedPayload = rawPayload.copy(
                    verificationStatus = SirenVerificationStatus.CONFIRMED_BY_OFFICER,
                    verificationDetailMessage = "Dikonfirmasi oleh petugas: ${rawPayload.confirmedByOfficerName.ifBlank { "Petugas Pusdalops" }}"
                )
                SourceValidationDecision(
                    validatedPayload = confirmedPayload,
                    shouldAutoSoundSiren = true,
                    showUnverifiedBanner = false,
                    requiresOfficerConfirmation = false,
                    reasonSummary = "Sumber non-BMKG telah dikonfirmasi petugas di Firestore."
                )
            } else {
                val pendingPayload = rawPayload.copy(
                    verificationStatus = SirenVerificationStatus.PENDING_OFFICER_CONFIRMATION,
                    verificationDetailMessage = "Sumber non-BMKG wajib menunggu konfirmasi petugas Pusdalops (flag confirmed=true di Firestore)."
                )
                SourceValidationDecision(
                    validatedPayload = pendingPayload,
                    shouldAutoSoundSiren = false,
                    showUnverifiedBanner = false,
                    requiresOfficerConfirmation = true,
                    reasonSummary = "Menunggu konfirmasi petugas Pusdalops (flag confirmed di Firestore)."
                )
            }
        }

        // 3. Klaim Sumber BMKG -> Verifikasi Silang Independen dengan Penanganan Exception Lengkap
        return try {
            withTimeout(safeConfig.bmkgVerifyTimeoutMillis) {
                when (rawPayload.alertType) {
                    SirenAlertType.TSUNAMI -> verifyTsunamiParallel(rawPayload, safeConfig)
                    SirenAlertType.EXTREME_WEATHER -> verifyExtremeWeather(rawPayload, safeConfig)
                }
            }
        } catch (timeoutEx: TimeoutCancellationException) {
            applyFailurePolicy(
                rawPayload = rawPayload,
                status = SirenVerificationStatus.UNVERIFIED_TIMEOUT,
                detail = "Verifikasi ke server BMKG melewati batas waktu (${safeConfig.bmkgVerifyTimeoutMillis} ms): ${timeoutEx.message ?: "Timeout"}",
                policy = safeConfig.verificationFailurePolicy
            )
        } catch (cancelEx: CancellationException) {
            throw cancelEx
        } catch (socketTimeoutEx: SocketTimeoutException) {
            applyFailurePolicy(
                rawPayload = rawPayload,
                status = SirenVerificationStatus.UNVERIFIED_TIMEOUT,
                detail = "Koneksi ke server BMKG timeout: ${socketTimeoutEx.message ?: "SocketTimeoutException"}",
                policy = safeConfig.verificationFailurePolicy
            )
        } catch (ioEx: IOException) {
            applyFailurePolicy(
                rawPayload = rawPayload,
                status = SirenVerificationStatus.UNVERIFIED_NETWORK_ERROR,
                detail = "Gagal menghubungi server BMKG karena gangguan jaringan: ${ioEx.message ?: "IOException"}",
                policy = safeConfig.verificationFailurePolicy
            )
        } catch (ex: Exception) {
            applyFailurePolicy(
                rawPayload = rawPayload,
                status = SirenVerificationStatus.UNVERIFIED_NETWORK_ERROR,
                detail = "Kendala verifikasi server BMKG (${ex.javaClass.simpleName}): ${ex.message ?: "Error tidak diketahui"}",
                policy = safeConfig.verificationFailurePolicy
            )
        }
    }

    /**
     * Pembungkus yang HANYA menangkap `Exception` (tidak menangkap `Error` JVM seperti `OutOfMemoryError`)
     * dan langsung me-rethrow `CancellationException` non-timeout.
     */
    private suspend fun <T> catchExceptionOnly(block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (timeoutEx: TimeoutCancellationException) {
            Result.failure(timeoutEx)
        } catch (cancelEx: CancellationException) {
            throw cancelEx
        } catch (ex: Exception) {
            Result.failure(ex)
        }
    }

    /**
     * Fetch paralel ke 3 endpoint resmi BMKG (`autogempa.json`, `gempaterkini.json`, `gempadirasakan.json`)
     * sekaligus mengekstrak `eventTimestampMillis` dari record BMKG yang terverifikasi.
     * Jika parse `DateTime` BMKG gagal pada `TSUNAMI`, tidak fallback ke waktu payload melainkan melempar
     * `IllegalStateException` dengan alasan jelas agar diarahkan ke `applyFailurePolicy`.
     */
    private suspend fun verifyTsunamiParallel(
        rawPayload: SirenAlertPayload,
        config: SirenRuntimeConfig
    ): SourceValidationDecision = supervisorScope {
        val autoDeferred = async { catchExceptionOnly { bmkgFeedProvider.fetchLatestAutoGempa() } }
        val recentDeferred = async { catchExceptionOnly { bmkgFeedProvider.fetchRecentGempaTerkini() } }
        val feltDeferred = async { catchExceptionOnly { bmkgFeedProvider.fetchFeltGempaDirasakan() } }

        val autoResult = autoDeferred.await()
        val recentResult = recentDeferred.await()
        val feltResult = feltDeferred.await()

        // Tahap A: Cek kecocokan pada rilis utama (autogempa.json)
        val latestRaw = autoResult.getOrNull()
        if (latestRaw != null && matchesOfficialBmkgRecord(rawPayload, latestRaw, config)) {
            val verifiedTimestamp = parseBmkgIsoTimestampMillis(latestRaw.dateTime)
                ?: throw IllegalStateException(
                    "Gagal mem-parse DateTime BMKG pada autogempa.json: '${latestRaw.dateTime}'. Waktu payload tidak digunakan sebagai fallback."
                )
            val verifiedPrimary = rawPayload.copy(
                verificationStatus = SirenVerificationStatus.VERIFIED_BMKG_PRIMARY,
                verificationDetailMessage = "Cocok dengan rilis utama autogempa.json BMKG (${latestRaw.dateTime}) • Wilayah: ${latestRaw.wilayah ?: "-"} • Potensi: ${latestRaw.potensi}.",
                eventTimestampMillis = verifiedTimestamp,
                expiresAtMillis = verifiedTimestamp + config.maxAlertAgeMillis,
                magnitude = latestRaw.magnitude?.toDoubleOrNull() ?: rawPayload.magnitude,
                depthKm = latestRaw.kedalaman ?: rawPayload.depthKm,
                coordinates = latestRaw.coordinates ?: rawPayload.coordinates
            )
            return@supervisorScope SourceValidationDecision(
                validatedPayload = verifiedPrimary,
                shouldAutoSoundSiren = true,
                showUnverifiedBanner = false,
                requiresOfficerConfirmation = false,
                reasonSummary = "Terverifikasi langsung melalui feed utama BMKG (autogempa.json)."
            )
        }

        // Tahap B: Jika autogempa sudah tergeser oleh gempa lebih baru, cek hasil paralel gempaterkini & gempadirasakan
        val historyCandidates = recentResult.getOrDefault(emptyList()) + feltResult.getOrDefault(emptyList())
        val historicalMatch = historyCandidates.firstOrNull { candidate ->
            matchesOfficialBmkgRecord(rawPayload, candidate, config)
        }

        if (historicalMatch != null) {
            val verifiedTimestamp = parseBmkgIsoTimestampMillis(historicalMatch.dateTime)
                ?: throw IllegalStateException(
                    "Gagal mem-parse DateTime BMKG pada riwayat gempa resmi: '${historicalMatch.dateTime}'. Waktu payload tidak digunakan sebagai fallback."
                )
            val verifiedHistory = rawPayload.copy(
                verificationStatus = SirenVerificationStatus.VERIFIED_BMKG_HISTORY,
                verificationDetailMessage = "Rilis autogempa telah tergeser gempa baru, namun kejadian ini terverifikasi sah pada riwayat resmi BMKG (${historicalMatch.dateTime}) • Wilayah: ${historicalMatch.wilayah ?: "-"} • Potensi: ${historicalMatch.potensi}.",
                eventTimestampMillis = verifiedTimestamp,
                expiresAtMillis = verifiedTimestamp + config.maxAlertAgeMillis,
                magnitude = historicalMatch.magnitude?.toDoubleOrNull() ?: rawPayload.magnitude,
                depthKm = historicalMatch.kedalaman ?: rawPayload.depthKm,
                coordinates = historicalMatch.coordinates ?: rawPayload.coordinates
            )
            return@supervisorScope SourceValidationDecision(
                validatedPayload = verifiedHistory,
                shouldAutoSoundSiren = true,
                showUnverifiedBanner = false,
                requiresOfficerConfirmation = false,
                reasonSummary = "Terverifikasi melalui riwayat resmi BMKG (gempaterkini/gempadirasakan) meskipun autogempa telah tergeser."
            )
        }

        // Tahap C: Jika tidak ada yang cocok DAN salah satu endpoint mengalami kegagalan jaringan/timeout,
        // lempar exception tersebut agar ditangani secara eksplisit oleh applyFailurePolicy
        val firstFailure = autoResult.exceptionOrNull()
            ?: recentResult.exceptionOrNull()
            ?: feltResult.exceptionOrNull()
        if (firstFailure != null) {
            throw firstFailure
        }

        // Tahap D: Semua feed berhasil diakses tanpa error, tetapi data tidak ditemukan atau tidak berpotensi tsunami
        val notFoundPayload = rawPayload.copy(
            verificationStatus = SirenVerificationStatus.UNVERIFIED_NOT_IN_BMKG_FEED,
            verificationDetailMessage = "Peringatan mengklaim sumber BMKG namun TIDAK ditemukan potensi tsunami yang cocok persis di autogempa, gempaterkini, maupun gempadirasakan."
        )
        SourceValidationDecision(
            validatedPayload = notFoundPayload,
            shouldAutoSoundSiren = false,
            showUnverifiedBanner = true,
            requiresOfficerConfirmation = true,
            reasonSummary = "Ditolak otomatis: Kejadian atau potensi tsunami tidak ditemukan pada feed resmi BMKG."
        )
    }

    /**
     * Verifikasi independen untuk `SirenAlertType.EXTREME_WEATHER` menggunakan ambang cuaca
     * dari `SirenRuntimeConfig` dan status terpisah `VERIFIED_WEATHER_TELEMETRY`.
     */
    private suspend fun verifyExtremeWeather(
        rawPayload: SirenAlertPayload,
        config: SirenRuntimeConfig
    ): SourceValidationDecision {
        val (lat, lon) = resolveTargetCoordinates(rawPayload)
        val record = bmkgFeedProvider.fetchExtremeWeatherVerification(lat, lon)
            ?: throw IOException("Data verifikasi cuaca ekstrem kosong untuk koordinat ($lat, $lon)")

        if (matchesOfficialExtremeWeatherRecord(record, config)) {
            val verifiedTimestamp = parseWeatherIsoTimestampMillis(record.observationTimeIso)
                ?: throw IllegalStateException(
                    "Gagal mem-parse waktu observasi cuaca ekstrem: '${record.observationTimeIso}'. Waktu payload tidak digunakan sebagai fallback."
                )
            val verifiedWeather = rawPayload.copy(
                verificationStatus = SirenVerificationStatus.VERIFIED_WEATHER_TELEMETRY,
                verificationDetailMessage = "Terverifikasi telemetri cuaca ekstrem (WMO Code ${record.weatherCode}, Hujan ${record.precipitationMm} mm, Angin ${record.windSpeedKmh} km/jam).",
                eventTimestampMillis = verifiedTimestamp,
                expiresAtMillis = verifiedTimestamp + config.maxAlertAgeMillis,
                weatherCode = record.weatherCode,
                precipitationMm = record.precipitationMm,
                windSpeedKmh = record.windSpeedKmh
            )
            return SourceValidationDecision(
                validatedPayload = verifiedWeather,
                shouldAutoSoundSiren = true,
                showUnverifiedBanner = false,
                requiresOfficerConfirmation = false,
                reasonSummary = "Parameter cuaca ekstrem terverifikasi pada koordinat wilayah Mukomuko."
            )
        }

        val notExtremePayload = rawPayload.copy(
            verificationStatus = SirenVerificationStatus.UNVERIFIED_NOT_IN_BMKG_FEED,
            verificationDetailMessage = "Parameter cuaca saat ini (WMO ${record.weatherCode}, Hujan ${record.precipitationMm} mm, Angin ${record.windSpeedKmh} km/jam) belum memenuhi ambang konfigurasi cuaca ekstrem."
        )
        return SourceValidationDecision(
            validatedPayload = notExtremePayload,
            shouldAutoSoundSiren = false,
            showUnverifiedBanner = true,
            requiresOfficerConfirmation = true,
            reasonSummary = "Ambang cuaca ekstrem tidak terpenuhi pada feed verifikasi."
        )
    }

    /**
     * Menerapkan kebijakan gagal-verifikasi sesuai flag `VerificationFailurePolicy` tanpa menyembunyikan
     * status "BELUM TERVERIFIKASI".
     */
    internal fun applyFailurePolicy(
        rawPayload: SirenAlertPayload,
        status: SirenVerificationStatus,
        detail: String,
        policy: VerificationFailurePolicy
    ): SourceValidationDecision {
        if (rawPayload.confirmedByOfficer) {
            val confirmedPayload = rawPayload.copy(
                verificationStatus = SirenVerificationStatus.CONFIRMED_BY_OFFICER,
                verificationDetailMessage = "$detail • Telah dikonfirmasi manual oleh petugas ${rawPayload.confirmedByOfficerName}."
            )
            return SourceValidationDecision(
                validatedPayload = confirmedPayload,
                shouldAutoSoundSiren = true,
                showUnverifiedBanner = true,
                requiresOfficerConfirmation = false,
                reasonSummary = "Server BMKG tidak terjangkau, namun telah dikonfirmasi petugas."
            )
        }

        val unverifiedPayload = rawPayload.copy(
            verificationStatus = status,
            verificationDetailMessage = detail
        )

        return when (policy) {
            VerificationFailurePolicy.HOLD_FOR_OFFICER_CONFIRMATION -> SourceValidationDecision(
                validatedPayload = unverifiedPayload,
                shouldAutoSoundSiren = false,
                showUnverifiedBanner = true,
                requiresOfficerConfirmation = true,
                reasonSummary = "${status.badgeLabel}: Sirene ditahan menunggu konfirmasi petugas sesuai kebijakan aktif."
            )

            VerificationFailurePolicy.NOTIFY_WITH_UNVERIFIED_BADGE_NO_SIREN -> SourceValidationDecision(
                validatedPayload = unverifiedPayload,
                shouldAutoSoundSiren = false,
                showUnverifiedBanner = true,
                requiresOfficerConfirmation = false,
                reasonSummary = "${status.badgeLabel}: Ditampilkan sebagai peringatan belum terverifikasi tanpa sirene suara."
            )

            VerificationFailurePolicy.SOUND_SIREN_WITH_UNVERIFIED_BANNER -> SourceValidationDecision(
                validatedPayload = unverifiedPayload,
                shouldAutoSoundSiren = true,
                showUnverifiedBanner = true,
                requiresOfficerConfirmation = false,
                reasonSummary = "${status.badgeLabel}: Kebijakan Fail-Safe aktif, membunyikan sirene dengan label BELUM TERVERIFIKASI."
            )
        }
    }

    /**
     * Mencocokkan atribut inti peringatan dengan rekaman resmi gempa/tsunami BMKG:
     * 1. Mewajibkan kecocokan persis `payload.alertId` terhadap `${rawDateTime}_${rawCoords}`
     *    (TANPA fallback `alertId.contains(rawDateTime)`).
     * 2. Untuk `SirenAlertType.TSUNAMI`, `payload.magnitude` dan `payload.coordinates` WAJIB ada.
     * 3. Membandingkan koordinat lintang & bujur secara numerik dengan toleransi `config.coordinateToleranceDegrees`,
     *    bukan perbandingan string persis.
     * 4. Untuk `SirenAlertType.TSUNAMI`, field `Potensi` di feed resmi BMKG WAJIB menyatakan potensi tsunami.
     * 5. Tidak menyaring berdasarkan nama wilayah (`Wilayah`).
     */
    internal fun matchesOfficialBmkgRecord(
        payload: SirenAlertPayload,
        bmkgRaw: BmkgGempaRaw,
        config: SirenRuntimeConfig = SirenRuntimeConfig()
    ): Boolean {
        val safeConfig = config.clamped()
        val rawDateTime = bmkgRaw.dateTime?.trim().orEmpty()
        val rawCoords = bmkgRaw.coordinates?.trim().orEmpty()
        val rawPotensi = bmkgRaw.potensi?.trim().orEmpty()
        if (rawDateTime.isEmpty() || rawCoords.isEmpty()) {
            return false
        }

        // 1. Wajib kecocokan persis pada alertId (tidak boleh hanya mengandung rawDateTime)
        val constructedBmkgId = "${rawDateTime}_${rawCoords}"
        val idMatches = payload.alertId.trim().equals(constructedBmkgId, ignoreCase = true)
        if (!idMatches) {
            return false
        }

        // 2. Untuk TSUNAMI, magnitudo dan koordinat pada payload WAJIB ada
        if (payload.alertType == SirenAlertType.TSUNAMI) {
            if (payload.magnitude == null || payload.coordinates.isNullOrBlank()) {
                return false
            }
        }

        // 3. Perbandingan magnitudo secara numerik
        val magnitudeMatches = if (payload.magnitude != null) {
            val officialMag = bmkgRaw.magnitude?.trim()?.toDoubleOrNull() ?: return false
            abs(officialMag - payload.magnitude) <= safeConfig.magnitudeTolerance
        } else {
            true
        }
        if (!magnitudeMatches) {
            return false
        }

        // 4. Perbandingan koordinat secara numerik dengan toleransi derajat
        val coordsMatches = if (!payload.coordinates.isNullOrBlank()) {
            val payloadLatLon = parseLatLonPair(payload.coordinates) ?: return false
            val officialLatLon = parseLatLonPair(rawCoords) ?: return false
            abs(payloadLatLon.first - officialLatLon.first) <= safeConfig.coordinateToleranceDegrees &&
                    abs(payloadLatLon.second - officialLatLon.second) <= safeConfig.coordinateToleranceDegrees
        } else {
            true
        }
        if (!coordsMatches) {
            return false
        }

        // 5. Untuk peringatan TSUNAMI, field `Potensi` di feed resmi BMKG WAJIB menyatakan potensi tsunami
        val tsunamiPotentialMatches = if (payload.alertType == SirenAlertType.TSUNAMI) {
            rawPotensi.contains("tsunami", ignoreCase = true) &&
                    !rawPotensi.contains("tidak", ignoreCase = true)
        } else {
            true
        }

        return tsunamiPotentialMatches
    }

    internal fun parseLatLonPair(rawCoordinates: String?): Pair<Double, Double>? {
        if (rawCoordinates.isNullOrBlank()) return null
        val parts = rawCoordinates.split(",")
        if (parts.size != 2) return null
        val lat = parts[0].trim().toDoubleOrNull() ?: return null
        val lon = parts[1].trim().toDoubleOrNull() ?: return null
        if (lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) return null
        return Pair(lat, lon)
    }

    internal fun matchesOfficialExtremeWeatherRecord(
        record: OfficialWeatherVerificationRecord,
        config: SirenRuntimeConfig = SirenRuntimeConfig()
    ): Boolean {
        val safeConfig = config.clamped()
        val isSevereWmoCode = record.weatherCode in safeConfig.extremeWeatherWmoCodes
        val isExtremeRain = record.precipitationMm >= safeConfig.extremePrecipitationMmThreshold
        val isExtremeWind = record.windSpeedKmh >= safeConfig.extremeWindKmhThreshold
        return isSevereWmoCode || isExtremeRain || isExtremeWind
    }

    internal fun parseBmkgIsoTimestampMillis(rawDateTime: String?): Long? {
        if (rawDateTime.isNullOrBlank()) return null
        return try {
            OffsetDateTime.parse(rawDateTime.trim()).toInstant().toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }

    private fun parseWeatherIsoTimestampMillis(rawTime: String?): Long? {
        return RetrofitOfficialBmkgFeedProvider.parseWeatherTimestampMillis(rawTime)
    }

    private fun resolveTargetCoordinates(payload: SirenAlertPayload): Pair<Double, Double> {
        val parsed = parseLatLonPair(payload.coordinates)
        if (parsed != null) return parsed
        val matchedDistrict = MukomukoDistricts.list.firstOrNull {
            it.name.equals(payload.affectedDistrict.trim(), ignoreCase = true)
        }
        return if (matchedDistrict != null) {
            Pair(matchedDistrict.latitude, matchedDistrict.longitude)
        } else {
            Pair(-2.5833, 101.1167)
        }
    }
}
