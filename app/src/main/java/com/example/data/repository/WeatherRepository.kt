package com.example.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.data.api.NetworkClient
import com.example.data.api.OpenMeteoAirQualityResponse
import com.example.data.api.OpenMeteoForecastResponse
import com.example.data.local.CitizenDisasterReportEntity
import com.example.data.local.CommunityReportEntity
import com.example.model.CitizenDisasterReport
import com.example.data.local.HistoricalWeatherEntity
import com.example.data.local.WeatherCacheEntity
import com.example.data.local.WeatherDao
import com.example.model.AirQuality
import com.example.model.CommunityWeatherReport
import com.example.model.CurrentWeather
import com.example.model.DailyItem
import com.example.model.DetailedEarlyWarning
import com.example.model.District
import com.example.model.HistoricalWeatherRecord
import com.example.model.HourlyItem
import com.example.model.MarineWeather
import com.example.model.WeatherAlert
import com.example.model.WeatherFullReport
import com.example.training.SandboxTrainingManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

class WeatherRepository(
    private val weatherDao: WeatherDao
) {
    // Serialized Dispatcher for all database write operations to prevent write-ahead logging (WAL) collisions
    val databaseWriteDispatcher = Dispatchers.IO.limitedParallelism(1)

    private val forecastApi = NetworkClient.weatherApi
    private val airQualityApi = NetworkClient.airQualityApi
    private val archiveApi = NetworkClient.archiveApi
    private val bmkgApi = com.example.data.api.BmkgNetworkClient.api

    fun getFavoriteDistrictIds(): Flow<List<String>> = weatherDao.getFavoriteDistrictIds()

    suspend fun toggleFavorite(districtId: String, currentIsFavorite: Boolean) {
        withContext(Dispatchers.IO) {
            if (currentIsFavorite) {
                weatherDao.removeFavorite(districtId)
            } else {
                weatherDao.setFavorite(com.example.data.local.FavoriteDistrictEntity(districtId, true))
            }
        }
    }

    // Community Reports
    fun getCommunityReports(): Flow<List<CommunityWeatherReport>> {
        return weatherDao.getAllCommunityReports().map { entities ->
            entities.map { entity ->
                CommunityWeatherReport(
                    id = entity.id,
                    districtId = entity.districtId,
                    districtName = entity.districtName,
                    reporterName = entity.reporterName,
                    weatherCondition = entity.weatherCondition,
                    notes = entity.notes,
                    timestamp = entity.timestamp,
                    upvotes = entity.upvotes
                )
            }
        }
    }

    suspend fun seedInitialCommunityReportsIfEmpty() = withContext(Dispatchers.IO) {
        val initialList = listOf(
            CommunityReportEntity(
                id = "comm_1",
                districtId = "kota_mukomuko",
                districtName = "Kota Mukomuko",
                reporterName = "Nelayan Pantai Pasar",
                weatherCondition = "Ombak Mulai Pasang",
                notes = "Angin pesisir bertiup lumayan kencang sore ini dari arah barat daya. Nelayan tepi pantai sudah menarik perahu ke darat.",
                timestamp = System.currentTimeMillis() - 25 * 60 * 1000,
                upvotes = 8
            ),
            CommunityReportEntity(
                id = "comm_2",
                districtId = "ipuh",
                districtName = "Ipuh",
                reporterName = "Warga Pasar Ipuh",
                weatherCondition = "Cerah Berawan",
                notes = "Cuaca di seputaran jembatan Ipuh cukup panas berawan, jalan lintas lancar kering.",
                timestamp = System.currentTimeMillis() - 48 * 60 * 1000,
                upvotes = 5
            ),
            CommunityReportEntity(
                id = "comm_3",
                districtId = "penarik",
                districtName = "Penarik",
                reporterName = "Pakde Tani Sawit",
                weatherCondition = "Gerimis Rintik",
                notes = "Mulai gerimis tipis di area perkebunan simpang SP1 Penarik, langit mendung tebal ke arah perbukitan.",
                timestamp = System.currentTimeMillis() - 75 * 60 * 1000,
                upvotes = 4
            ),
            CommunityReportEntity(
                id = "comm_4",
                districtId = "lubuk_pinang",
                districtName = "Lubuk Pinang",
                reporterName = "Komunitas Petani Manjunto",
                weatherCondition = "Hujan Sedang",
                notes = "Debit air saluran primer irigasi Air Manjunto cukup tinggi, cuaca mendung dingin sejuk.",
                timestamp = System.currentTimeMillis() - 110 * 60 * 1000,
                upvotes = 11
            ),
            CommunityReportEntity(
                id = "comm_5",
                districtId = "air_dikit",
                districtName = "Air Dikit",
                reporterName = "Pengendara Lintas Barat",
                weatherCondition = "Angin Kencang",
                notes = "Angin laut agak kencang melintasi jalan raya pantai Air Dikit, hati-hati bagi pengendara roda dua.",
                timestamp = System.currentTimeMillis() - 140 * 60 * 1000,
                upvotes = 6
            )
        )
        weatherDao.insertAllCommunityReports(initialList)
    }

    suspend fun submitCommunityReport(
        districtId: String,
        districtName: String,
        reporterName: String,
        weatherCondition: String,
        notes: String
    ) = withContext(Dispatchers.IO) {
        val entity = CommunityReportEntity(
            id = UUID.randomUUID().toString(),
            districtId = districtId,
            districtName = districtName,
            reporterName = reporterName.ifBlank { "Warga Mukomuko" },
            weatherCondition = weatherCondition,
            notes = notes,
            timestamp = System.currentTimeMillis(),
            upvotes = 0
        )
        weatherDao.insertCommunityReport(entity)
    }

    suspend fun upvoteReport(reportId: String) = withContext(Dispatchers.IO) {
        weatherDao.upvoteReport(reportId)
    }

    private fun mapCitizenEntityToModel(entity: CitizenDisasterReportEntity): CitizenDisasterReport {
        return CitizenDisasterReport(
            id = entity.id,
            disasterType = entity.disasterType,
            incidentTime = entity.incidentTime,
            incidentLocation = entity.incidentLocation,
            district = entity.district,
            village = entity.village,
            latitude = entity.latitude,
            longitude = entity.longitude,
            gpsAccuracyMeters = entity.gpsAccuracyMeters,
            isGpsVerified = entity.isGpsVerified,
            casualtiesDead = entity.casualtiesDead,
            casualtiesInjured = entity.casualtiesInjured,
            casualtiesMissing = entity.casualtiesMissing,
            photoUri = entity.photoUri,
            reporterName = entity.reporterName,
            reporterPhone = entity.reporterPhone,
            additionalNotes = entity.additionalNotes,
            timestamp = entity.timestamp,
            status = com.example.model.CitizenReportStatusHelper.normalizeStatus(entity.status),
            updatedAt = entity.effectiveUpdatedAt(),
            handledByOfficerName = entity.handledByOfficerName,
            handledByOfficerNip = entity.handledByOfficerNip,
            handledByOfficerRole = entity.handledByOfficerRole,
            isSyncedToCloud = entity.isSyncedToCloud
        )
    }

    private fun mapCitizenModelToEntity(
        report: CitizenDisasterReport,
        isSyncedOverride: Boolean = report.isSyncedToCloud
    ): CitizenDisasterReportEntity {
        return CitizenDisasterReportEntity(
            id = report.id,
            disasterType = report.disasterType,
            incidentTime = report.incidentTime,
            incidentLocation = report.incidentLocation,
            district = report.district,
            village = report.village,
            latitude = report.latitude,
            longitude = report.longitude,
            gpsAccuracyMeters = report.gpsAccuracyMeters,
            isGpsVerified = report.isGpsVerified,
            casualtiesDead = report.casualtiesDead,
            casualtiesInjured = report.casualtiesInjured,
            casualtiesMissing = report.casualtiesMissing,
            photoUri = report.photoUri,
            reporterName = report.reporterName,
            reporterPhone = report.reporterPhone,
            additionalNotes = report.additionalNotes,
            timestamp = report.timestamp,
            status = com.example.model.CitizenReportStatusHelper.normalizeStatus(report.status),
            updatedAt = report.effectiveUpdatedAt(),
            handledByOfficerName = report.handledByOfficerName,
            handledByOfficerNip = report.handledByOfficerNip,
            handledByOfficerRole = report.handledByOfficerRole,
            isSyncedToCloud = isSyncedOverride
        )
    }

    private fun shouldAcceptRemoteCitizenReport(
        existing: CitizenDisasterReportEntity?,
        remote: CitizenDisasterReport
    ): Boolean {
        if (existing == null) return true
        val localUpdatedAt = existing.effectiveUpdatedAt()
        val remoteUpdatedAt = remote.effectiveUpdatedAt()
        val localNorm = com.example.model.CitizenReportStatusHelper.normalizeStatus(existing.status)
        val remoteNorm = com.example.model.CitizenReportStatusHelper.normalizeStatus(remote.status)
        val localRank = com.example.model.CitizenReportStatusHelper.statusRank(localNorm)
        val remoteRank = com.example.model.CitizenReportStatusHelper.statusRank(remoteNorm)

        val hasGenuineUnpushedLocalMutation = !existing.isSyncedToCloud &&
            localUpdatedAt > remoteUpdatedAt &&
            (localNorm != remoteNorm || existing.additionalNotes.trim() != remote.additionalNotes.trim())

        // Jangan pernah menimpa perubahan lokal yang benar-benar baru (status/catatan berbeda) dan belum tersinkron ke Cloud
        if (hasGenuineUnpushedLocalMutation) {
            return false
        }
        // Terima remote bila waktunya lebih baru atau statusnya lebih maju
        if (remoteUpdatedAt > localUpdatedAt) return true
        if (remoteRank > localRank) return true
        // Jika laporan sudah ada di server Firestore sedangkan lokal masih bertanda belum sync tanpa mutasi lokal berbeda,
        // tandai langsung sebagai Synced dari server!
        if (!existing.isSyncedToCloud) return true
        // Jika remote memiliki foto bukti portabel sedangkan lokal belum punya foto portabel, terima pembaruan foto
        val remoteBringsPortablePhoto = com.example.util.ImageUtils.isCloudPortablePhotoUri(remote.photoUri) &&
            !com.example.util.ImageUtils.isCloudPortablePhotoUri(existing.photoUri)
        if (remoteBringsPortablePhoto && remoteRank >= localRank) return true

        // Jika waktu sama (tie), utamakan kebenaran server apabila ada perubahan data/petugas/status
        if (remoteUpdatedAt == localUpdatedAt) {
            val differs = localNorm != remoteNorm ||
                existing.additionalNotes != remote.additionalNotes ||
                existing.handledByOfficerName != remote.handledByOfficerName ||
                existing.handledByOfficerNip != remote.handledByOfficerNip ||
                existing.handledByOfficerRole != remote.handledByOfficerRole ||
                (remote.photoUri != null && existing.photoUri.isNullOrBlank())
            if (differs && remoteRank >= localRank) return true
        }
        return false
    }

    // Citizen Disaster Reports (Anti-Hoax GPS Verified)
    fun getCitizenDisasterReports(): Flow<List<CitizenDisasterReport>> {
        return combine(
            weatherDao.getAllCitizenDisasterReports(),
            SandboxTrainingManager.isSandboxEnabled,
            SandboxTrainingManager.sandboxReports
        ) { entities, isSandbox, sandboxList ->
            if (isSandbox) {
                sandboxList
            } else {
                entities.map { mapCitizenEntityToModel(it) }
            }
        }
    }

    fun getCitizenReportsByStatus(status: String): Flow<List<CitizenDisasterReport>> {
        return combine(
            weatherDao.getCitizenReportsByStatus(status),
            SandboxTrainingManager.isSandboxEnabled,
            SandboxTrainingManager.sandboxReports
        ) { entities, isSandbox, sandboxList ->
            if (isSandbox) {
                sandboxList.filter { com.example.model.CitizenReportStatusHelper.matchesFilter(it.status, status) }
            } else {
                entities.map { mapCitizenEntityToModel(it) }
            }
        }
    }

    suspend fun getCitizenReportCount(): Int = withContext(Dispatchers.IO) {
        weatherDao.getCitizenReportCount()
    }

    suspend fun getCitizenReportCountByStatus(status: String): Int = withContext(Dispatchers.IO) {
        weatherDao.getCitizenReportCountByStatus(status)
    }

    suspend fun deleteReportsOlderThan(cutoffTimestamp: Long): Int = withContext(Dispatchers.IO) {
        weatherDao.deleteReportsOlderThan(cutoffTimestamp)
    }

    /**
     * Automated report database cleanup service:
     * Purges disaster reports older than 90 days from the local Room database
     * to maintain high query efficiency and avoid storage bloat while keeping
     * recent records and cloud backups intact.
     */
    suspend fun runAutomatedCleanup90Days(): Int = withContext(databaseWriteDispatcher) {
        val ninetyDaysMs = 90L * 24 * 60 * 60 * 1000L
        val cutoff = System.currentTimeMillis() - ninetyDaysMs
        val deletedCount = weatherDao.deleteReportsOlderThan(cutoff)
        if (deletedCount > 0) {
            android.util.Log.i("WeatherRepository", "Automated cleanup: Deleted $deletedCount expired disaster reports (>90 days).")
        }
        deletedCount
    }

    /**
     * Restores a list of disaster reports into the local Room database,
     * merging with existing records using OnConflictStrategy.REPLACE.
     */
    suspend fun restoreReportsToRoom(reports: List<CitizenDisasterReport>) = withContext(databaseWriteDispatcher) {
        val entities = reports.map { mapCitizenModelToEntity(it) }
        weatherDao.insertAllCitizenDisasterReports(entities)
    }

    /**
     * Mengunggah Bitmap foto laporan warga ke Supabase Storage (bucket `reports`)
     * dan mengembalikan URL publik `https://`.
     */
    suspend fun uploadPhotoToSupabase(bitmap: Bitmap, reportId: String): Result<String> =
        com.example.data.api.SupabaseClient.uploadPhotoToSupabase(bitmap, reportId)

    /**
     * Menjalankan diagnostik jalur unggah foto Supabase (bucket `reports`), memverifikasi
     * integritas byte JPEG (ukuran + header + SHA-256), serta memeriksa policy akses
     * public vs authenticated dan menulis log diagnostik ke Logcat (`SupabaseStorageDiag`).
     */
    suspend fun diagnoseSupabaseImageUploadPath(
        sampleBitmap: Bitmap? = null,
        reportId: String = "diag_probe",
        authBearerToken: String? = null,
        cleanupProbeObject: Boolean = true
    ): com.example.data.api.SupabaseUploadDiagnosticReport =
        com.example.data.api.SupabaseClient.diagnoseImageUploadPath(
            sampleBitmap = sampleBitmap,
            reportId = reportId,
            authBearerToken = authBearerToken,
            cleanupProbeObject = cleanupProbeObject
        )

    /**
     * Memeriksa aksesibilitas (public vs authenticated) untuk URL/path foto laporan yang sudah tersimpan.
     */
    suspend fun diagnoseSupabaseReportImageUrl(
        imageUrlOrPath: String,
        authBearerToken: String? = null
    ): com.example.data.api.SupabaseUploadDiagnosticReport =
        com.example.data.api.SupabaseClient.diagnoseExistingReportImageUrl(
            imageUrlOrPath = imageUrlOrPath,
            authBearerToken = authBearerToken
        )

    /**
     * Mendekode sumber foto lokal (file://, path absolut, atau Base64) menjadi Bitmap
     * agar siap diunggah ke Firebase Storage.
     */
    private fun decodeLocalPhotoBitmap(photoSource: String?): Bitmap? {
        if (photoSource.isNullOrBlank()) return null
        val trimmed = photoSource.trim()
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return null
        }
        return try {
            when {
                trimmed.startsWith("file://", ignoreCase = true) -> {
                    val path = Uri.parse(trimmed).path ?: return null
                    val file = File(path)
                    if (file.exists() && file.length() > 0L) BitmapFactory.decodeFile(file.absolutePath) else null
                }
                trimmed.startsWith("/") -> {
                    val file = File(trimmed)
                    if (file.exists() && file.length() > 0L) BitmapFactory.decodeFile(file.absolutePath) else null
                }
                trimmed.startsWith("data:image", ignoreCase = true) || trimmed.contains("base64,", ignoreCase = true) -> {
                    val base64Part = trimmed.substringAfter("base64,", trimmed)
                    val bytes = Base64.decode(base64Part, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    data class CitizenReportSubmitResult(
        val reportId: String,
        val savedLocally: Boolean,
        val syncedToCloud: Boolean,
        val hasPhoto: Boolean,
        val cloudPhotoUrl: String? = null,
        val deferredOffline: Boolean = false,
        val message: String = ""
    )

    suspend fun submitCitizenDisasterReportWithResult(
        report: CitizenDisasterReport,
        context: android.content.Context? = null
    ): CitizenReportSubmitResult = withContext(databaseWriteDispatcher) {
        if (SandboxTrainingManager.isSandboxModeActive() || SandboxTrainingManager.isSandboxReportId(report.id)) {
            val savedSandbox = SandboxTrainingManager.submitSandboxCitizenReport(
                context = context,
                report = report,
                triggerRealtimeAlertBanner = true
            )
            return@withContext CitizenReportSubmitResult(
                reportId = savedSandbox.id,
                savedLocally = true,
                syncedToCloud = true,
                hasPhoto = !savedSandbox.photoUri.isNullOrBlank(),
                cloudPhotoUrl = savedSandbox.photoUri,
                deferredOffline = false,
                message = "[MODE LATIHAN SANDBOX] Laporan simulasi (${savedSandbox.id}) berhasil diterima di lingkungan latihan tanpa menyentuh database produksi."
            )
        }

        val rawPhoto = report.photoUri?.trim()?.takeIf { it.isNotBlank() }
        val hasPhoto = rawPhoto != null
        val normalizedStatus = com.example.model.CitizenReportStatusHelper.normalizeStatus(report.status)
        val effectiveUpdatedAt = maxOf(report.timestamp, report.updatedAt)

        // Pastikan Firestore Offline Persistence & Connectivity Monitor terdaftar
        com.example.data.local.FirestoreHelper.ensureOfflinePersistenceEnabled(context)
        com.example.data.sync.OfflineReportConnectivityMonitor.ensureRegistered(context)

        // Pastikan baseline detektor sesi petugas terkunci pada laporan yang sudah ada sebelum laporan baru dimasukkan
        val existingBeforeSubmit = runCatching {
            weatherDao.getAllCitizenDisasterReportsSync().map { mapCitizenEntityToModel(it) }
        }.getOrDefault(emptyList())
        com.example.ui.components.OfficerRealtimeNewReportDetector.sharedSessionDetector
            .initializeBaselineIfNeeded(existingBeforeSubmit)

        // 1. Simpan segera ke Room SQLite lokal (teks + path foto lokal) dengan isSyncedToCloud = false
        //    agar jika sinyal putus atau aplikasi ditutup di tengah jalan, data + foto warga TIDAK pernah hilang.
        val initialLocalReport = report.copy(
            photoUri = rawPhoto,
            status = normalizedStatus,
            updatedAt = effectiveUpdatedAt,
            isSyncedToCloud = false
        )
        weatherDao.insertCitizenDisasterReport(mapCitizenModelToEntity(initialLocalReport, isSyncedOverride = false))

        // 2. Validasi koneksi & tunggu upload foto ke Firebase Storage selesai 100% (dalam blok try-catch aman) sebelum menulis Firestore.
        //    ATURAN KETAT OFFLINE: Jika perangkat offline atau upload foto gagal karena jaringan,
        //    simpan path lokalnya di Room SQLite dan kirim otomatis saat koneksi kembali.
        val photoResolution = try {
            com.example.util.ImageUtils.resolveCloudPhotoForUpload(
                rawPhotoUri = rawPhoto,
                reportId = report.id,
                context = context,
                supabaseUploader = { bitmap -> uploadPhotoToSupabase(bitmap, report.id).getOrNull() }
            )
        } catch (e: Throwable) {
            com.example.util.ImageUtils.CloudPhotoResolution.DeferredOffline(
                com.example.data.local.FirestoreHelper.OFFLINE_REPORT_SAVED_MESSAGE
            )
        }

        val resolvedCloudPhotoUrl: String? = when (photoResolution) {
            is com.example.util.ImageUtils.CloudPhotoResolution.NoPhoto -> {
                if (!com.example.data.api.FirebaseStoragePhotoUploader.isNetworkOnline(context)) {
                    android.util.Log.w(
                        "WeatherRepository",
                        "Perangkat offline — laporan ${report.id} disimpan di perangkat sampai sinyal kembali."
                    )
                    com.example.data.sync.CitizenReportSyncWorker.enqueueWhenConnected(context)
                    return@withContext CitizenReportSubmitResult(
                        reportId = report.id,
                        savedLocally = true,
                        syncedToCloud = false,
                        hasPhoto = false,
                        deferredOffline = true,
                        message = com.example.data.local.FirestoreHelper.OFFLINE_REPORT_SAVED_MESSAGE
                    )
                }
                null
            }
            is com.example.util.ImageUtils.CloudPhotoResolution.DeferredOffline -> {
                android.util.Log.w(
                    "WeatherRepository",
                    "Upload foto laporan ${report.id} tertunda (${photoResolution.reason}) — path lokal disimpan di Room SQLite dan akan dikirim otomatis saat sinyal tersedia."
                )
                com.example.data.sync.CitizenReportSyncWorker.enqueueWhenConnected(context)
                return@withContext CitizenReportSubmitResult(
                    reportId = report.id,
                    savedLocally = true,
                    syncedToCloud = false,
                    hasPhoto = true,
                    cloudPhotoUrl = null,
                    deferredOffline = true,
                    message = com.example.data.local.FirestoreHelper.OFFLINE_REPORT_SAVED_MESSAGE
                )
            }
            is com.example.util.ImageUtils.CloudPhotoResolution.ReadyForCloud -> {
                photoResolution.cloudPhotoUrl
            }
        }

        // 3. Setelah upload foto 100% selesai & download URL didapat, simpan ke Room & Firestore (fotoUrl + fotoLampiran + photoUri)
        val finalCloudReport = initialLocalReport.copy(
            photoUri = resolvedCloudPhotoUrl,
            isSyncedToCloud = false
        )
        if (resolvedCloudPhotoUrl != null && resolvedCloudPhotoUrl != rawPhoto) {
            weatherDao.insertCitizenDisasterReport(mapCitizenModelToEntity(finalCloudReport, isSyncedOverride = false))
        }

        val cloudResult = try {
            com.example.data.local.FirestoreHelper.syncReportToCloudSuspend(finalCloudReport)
        } catch (e: Exception) {
            Result.failure(e)
        }

        if (cloudResult.isSuccess) {
            weatherDao.updateCitizenReportSyncState(report.id, true)
            com.example.ui.components.OfficerRealtimeNewReportDetector.sharedSessionDetector
                .recordNewCitizenReport(finalCloudReport.copy(isSyncedToCloud = true), context)
            CitizenReportSubmitResult(
                reportId = report.id,
                savedLocally = true,
                syncedToCloud = true,
                hasPhoto = hasPhoto,
                cloudPhotoUrl = resolvedCloudPhotoUrl,
                deferredOffline = false,
                message = if (hasPhoto) {
                    "Laporan dan foto bukti GPS berhasil diunggah 100% ke server PUSDALOPS BPBD."
                } else {
                    "Laporan berhasil dikirim ke server PUSDALOPS BPBD."
                }
            )
        } else {
            android.util.Log.w(
                "WeatherRepository",
                "Laporan ${report.id} tersimpan lokal (menunggu sync Cloud): ${cloudResult.exceptionOrNull()?.message}"
            )
            com.example.ui.components.OfficerRealtimeNewReportDetector.sharedSessionDetector
                .recordNewCitizenReport(finalCloudReport.copy(isSyncedToCloud = false), context)
            com.example.data.sync.CitizenReportSyncWorker.enqueueWhenConnected(context)
            CitizenReportSubmitResult(
                reportId = report.id,
                savedLocally = true,
                syncedToCloud = false,
                hasPhoto = hasPhoto,
                cloudPhotoUrl = resolvedCloudPhotoUrl,
                deferredOffline = true,
                message = com.example.data.local.FirestoreHelper.OFFLINE_REPORT_SAVED_MESSAGE
            )
        }
    }

    suspend fun submitCitizenDisasterReport(
        report: CitizenDisasterReport,
        context: android.content.Context? = null
    ): Boolean = submitCitizenDisasterReportWithResult(report, context).syncedToCloud

    suspend fun updateCitizenReportStatus(
        reportId: String,
        newStatus: String,
        officerName: String = "",
        officerNip: String = "",
        officerRole: String = ""
    ): Boolean = withContext(databaseWriteDispatcher) {
        if (SandboxTrainingManager.isSandboxModeActive() || SandboxTrainingManager.isSandboxReportId(reportId)) {
            SandboxTrainingManager.updateSandboxReportStatus(
                reportId = reportId,
                newStatus = newStatus,
                notes = null,
                officerName = officerName,
                officerNip = officerNip,
                officerRole = officerRole
            )
            return@withContext true
        }
        val normalizedStatus = com.example.model.CitizenReportStatusHelper.normalizeStatus(newStatus)
        val now = System.currentTimeMillis()
        weatherDao.updateCitizenReportStatus(
            reportId = reportId,
            newStatus = normalizedStatus,
            updatedAt = now,
            officerName = officerName.trim(),
            officerNip = officerNip.trim(),
            officerRole = officerRole.trim(),
            isSynced = false
        )

        val localEntity = weatherDao.getCitizenReportById(reportId)
        val fallbackReport = localEntity?.let { mapCitizenEntityToModel(it) }

        val cloudResult = try {
            com.example.data.local.FirestoreHelper.updateReportStatusInCloudSuspend(
                reportId = reportId,
                newStatus = normalizedStatus,
                notes = null,
                updatedAt = now,
                officerName = officerName.trim(),
                officerNip = officerNip.trim(),
                officerRole = officerRole.trim(),
                fallbackReport = fallbackReport
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
        if (cloudResult.isSuccess) {
            weatherDao.updateCitizenReportSyncState(reportId, true)
            true
        } else {
            android.util.Log.w(
                "WeatherRepository",
                "Pembaruan status $reportId tersimpan lokal (menunggu sync Cloud): ${cloudResult.exceptionOrNull()?.message}"
            )
            false
        }
    }

    suspend fun updateCitizenReportStatusAndNotes(
        reportId: String,
        newStatus: String,
        notes: String,
        officerName: String = "",
        officerNip: String = "",
        officerRole: String = ""
    ): Boolean = withContext(databaseWriteDispatcher) {
        if (SandboxTrainingManager.isSandboxModeActive() || SandboxTrainingManager.isSandboxReportId(reportId)) {
            SandboxTrainingManager.updateSandboxReportStatus(
                reportId = reportId,
                newStatus = newStatus,
                notes = notes,
                officerName = officerName,
                officerNip = officerNip,
                officerRole = officerRole
            )
            return@withContext true
        }
        val normalizedStatus = com.example.model.CitizenReportStatusHelper.normalizeStatus(newStatus)
        val now = System.currentTimeMillis()
        weatherDao.updateCitizenReportStatusAndNotes(
            reportId = reportId,
            newStatus = normalizedStatus,
            newNotes = notes,
            updatedAt = now,
            officerName = officerName.trim(),
            officerNip = officerNip.trim(),
            officerRole = officerRole.trim(),
            isSynced = false
        )

        val localEntity = weatherDao.getCitizenReportById(reportId)
        val fallbackReport = localEntity?.let { mapCitizenEntityToModel(it) }

        val cloudResult = try {
            com.example.data.local.FirestoreHelper.updateReportStatusInCloudSuspend(
                reportId = reportId,
                newStatus = normalizedStatus,
                notes = notes,
                updatedAt = now,
                officerName = officerName.trim(),
                officerNip = officerNip.trim(),
                officerRole = officerRole.trim(),
                fallbackReport = fallbackReport
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
        if (cloudResult.isSuccess) {
            weatherDao.updateCitizenReportSyncState(reportId, true)
            true
        } else {
            android.util.Log.w(
                "WeatherRepository",
                "Pembaruan status & catatan $reportId tersimpan lokal (menunggu sync Cloud): ${cloudResult.exceptionOrNull()?.message}"
            )
            false
        }
    }

    /**
     * Menghapus laporan warga dari Room lokal & Cloud Firestore.
     * Dilindungi RBAC Multi-Tier: HANYA diizinkan untuk Tier 1 (Manajer / Full Access).
     */
    suspend fun deleteCitizenReport(
        reportId: String,
        officerRole: String?
    ): Boolean = withContext(databaseWriteDispatcher) {
        if (SandboxTrainingManager.isSandboxModeActive() || SandboxTrainingManager.isSandboxReportId(reportId)) {
            SandboxTrainingManager.deleteSandboxReport(reportId)
            return@withContext true
        }
        if (!com.example.security.UserRole.canDeleteReports(officerRole)) {
            android.util.Log.w(
                "WeatherRepository",
                "RBAC Ditolak: role='$officerRole' tidak memiliki izin Hapus Laporan (khusus Tier 1 Manajer)."
            )
            return@withContext false
        }
        val deletedLocal = weatherDao.deleteCitizenReportById(reportId)
        val cloudResult = try {
            com.example.data.local.FirestoreHelper.deleteReportFromCloudSuspend(reportId)
        } catch (e: Exception) {
            Result.failure(e)
        }
        deletedLocal > 0 || cloudResult.isSuccess
    }

    /**
     * Sinkronisasi dua arah (Pull + Push) antara Room lokal dan Cloud Firestore (Source.SERVER):
     * 1. Menarik seluruh laporan terbaru dari Firestore dan menerapkan pembaruan petugas aktif
     *    tanpa menimpa perubahan lokal yang belum terunggah dan lebih baru.
     * 2. Mendorong seluruh laporan/perubahan lokal yang belum tersinkron atau belum ada di Cloud
     *    sehingga perangkat petugas dan warga selalu 100% sinkron.
     */
    suspend fun syncCitizenDisasterReportsOnline(): Int = withContext(databaseWriteDispatcher) {
        if (SandboxTrainingManager.isSandboxModeActive()) {
            return@withContext 0
        }
        var syncedOrUpdatedCount = 0

        val fetchResult = com.example.data.local.FirestoreHelper.fetchCitizenReportsFromServerSuspend()
        val remoteReports = fetchResult.getOrElse { error ->
            android.util.Log.e(
                "WeatherRepository",
                "Gagal menarik citizen_disaster_reports dari server Firestore: ${error.localizedMessage}",
                error
            )
            // Meskipun tarik gagal, tetap coba dorong laporan lokal yang tertunda bila foto siap (jangan pernah kirim teks tanpa foto!)
            val pendingLocals = weatherDao.getUnsyncedCitizenReports()
            for (pending in pendingLocals) {
                val photoResolution = com.example.util.ImageUtils.resolveCloudPhotoForUpload(
                    rawPhotoUri = pending.photoUri,
                    reportId = pending.id,
                    supabaseUploader = { bmp -> uploadPhotoToSupabase(bmp, pending.id).getOrNull() }
                )
                val portablePhoto = when (photoResolution) {
                    is com.example.util.ImageUtils.CloudPhotoResolution.NoPhoto -> null
                    is com.example.util.ImageUtils.CloudPhotoResolution.ReadyForCloud -> photoResolution.cloudPhotoUrl
                    is com.example.util.ImageUtils.CloudPhotoResolution.DeferredOffline -> {
                        android.util.Log.w("WeatherRepository", "Tunda push pending ${pending.id}: ${photoResolution.reason}")
                        continue
                    }
                }
                val modelToPush = mapCitizenEntityToModel(pending).copy(
                    photoUri = portablePhoto
                )
                val pushRes = com.example.data.local.FirestoreHelper.syncReportToCloudSuspend(modelToPush)
                if (pushRes.isSuccess) {
                    if (portablePhoto != null && portablePhoto != pending.photoUri) {
                        weatherDao.insertCitizenDisasterReport(mapCitizenModelToEntity(modelToPush, isSyncedOverride = true))
                    } else {
                        weatherDao.updateCitizenReportSyncState(pending.id, true)
                    }
                    syncedOrUpdatedCount++
                }
            }
            return@withContext syncedOrUpdatedCount
        }

        val localEntities = weatherDao.getAllCitizenDisasterReportsSync()
        if (localEntities.isNotEmpty()) {
            com.example.ui.components.OfficerRealtimeNewReportDetector.sharedSessionDetector
                .initializeBaselineIfNeeded(localEntities.map { mapCitizenEntityToModel(it) })
        }
        val localReportsMap = localEntities.associateBy { it.id }
        val remoteReportsMap = remoteReports.associateBy { it.id }

        // 1. PULL: Terapkan data terbaru dari Cloud Firestore -> Room lokal
        for (remote in remoteReports) {
            val existing = localReportsMap[remote.id]
            if (shouldAcceptRemoteCitizenReport(existing, remote)) {
                val mergedReport = remote.copy(
                    photoUri = com.example.util.ImageUtils.selectBestPhotoUri(remote.photoUri, existing?.photoUri),
                    handledByOfficerName = remote.handledByOfficerName.ifBlank { existing?.handledByOfficerName.orEmpty() },
                    handledByOfficerNip = remote.handledByOfficerNip.ifBlank { existing?.handledByOfficerNip.orEmpty() },
                    handledByOfficerRole = remote.handledByOfficerRole.ifBlank { existing?.handledByOfficerRole.orEmpty() },
                    isSyncedToCloud = true
                )
                weatherDao.insertCitizenDisasterReport(mapCitizenModelToEntity(mergedReport, isSyncedOverride = true))
                if (existing == null && localEntities.isNotEmpty()) {
                    com.example.ui.components.OfficerRealtimeNewReportDetector.sharedSessionDetector
                        .recordNewCitizenReport(mergedReport)
                }
                syncedOrUpdatedCount++
            }
        }

        // 2. PUSH: Dorong laporan lokal yang belum ada di Cloud, memiliki perubahan lokal lebih baru,
        //    ATAU memiliki foto bukti lokal yang belum terunggah ke Cloud Firestore (perbaikan otomatis).
        //    ATURAN KETAT: Jangan pernah mendorong teks tanpa foto jika laporan memiliki lampiran foto lokal!
        val refreshedLocals = weatherDao.getAllCitizenDisasterReportsSync()
        for (local in refreshedLocals) {
            val remote = remoteReportsMap[local.id]
            val localHasPhotoButCloudMissing = remote != null &&
                !com.example.util.ImageUtils.isCloudPortablePhotoUri(remote.photoUri) &&
                !local.photoUri.isNullOrBlank()
            val needsPush = when {
                remote == null -> !local.isSyncedToCloud || !local.id.startsWith("LAP-MMK-2026102")
                !local.isSyncedToCloud && local.effectiveUpdatedAt() >= remote.effectiveUpdatedAt() -> true
                localHasPhotoButCloudMissing -> true
                else -> false
            }
            if (needsPush) {
                val photoResolution = com.example.util.ImageUtils.resolveCloudPhotoForUpload(
                    rawPhotoUri = local.photoUri,
                    reportId = local.id,
                    supabaseUploader = { bmp -> uploadPhotoToSupabase(bmp, local.id).getOrNull() }
                )
                val portablePhoto = when (photoResolution) {
                    is com.example.util.ImageUtils.CloudPhotoResolution.NoPhoto -> null
                    is com.example.util.ImageUtils.CloudPhotoResolution.ReadyForCloud -> photoResolution.cloudPhotoUrl
                    is com.example.util.ImageUtils.CloudPhotoResolution.DeferredOffline -> {
                        android.util.Log.w("WeatherRepository", "Tunda sync ${local.id} (foto belum terunggah): ${photoResolution.reason}")
                        continue
                    }
                }
                // Jika hanya memperbaiki foto yang hilang di Cloud, jangan pernah menurunkan status petugas terbaru dari server
                val baseModel = if (localHasPhotoButCloudMissing && remote != null && remote.effectiveUpdatedAt() > local.effectiveUpdatedAt()) {
                    remote.copy(
                        photoUri = portablePhoto,
                        updatedAt = maxOf(remote.effectiveUpdatedAt(), System.currentTimeMillis())
                    )
                } else {
                    mapCitizenEntityToModel(local).copy(
                        photoUri = portablePhoto
                    )
                }
                if (localHasPhotoButCloudMissing && portablePhoto == null) {
                    continue
                }
                val pushResult = com.example.data.local.FirestoreHelper.syncReportToCloudSuspend(baseModel)
                if (pushResult.isSuccess) {
                    if (portablePhoto != null && portablePhoto != local.photoUri) {
                        weatherDao.insertCitizenDisasterReport(mapCitizenModelToEntity(baseModel, isSyncedOverride = true))
                    } else {
                        weatherDao.updateCitizenReportSyncState(local.id, true)
                    }
                    syncedOrUpdatedCount++
                }
            }
        }

        syncedOrUpdatedCount
    }

    suspend fun upsertRealtimeCitizenReportsFromCloud(remoteReports: List<CitizenDisasterReport>) = withContext(databaseWriteDispatcher) {
        if (SandboxTrainingManager.isSandboxModeActive()) {
            return@withContext
        }
        val localEntities = weatherDao.getAllCitizenDisasterReportsSync()
        if (localEntities.isNotEmpty()) {
            com.example.ui.components.OfficerRealtimeNewReportDetector.sharedSessionDetector
                .initializeBaselineIfNeeded(localEntities.map { mapCitizenEntityToModel(it) })
        }
        val localReportsMap = localEntities.associateBy { it.id }
        for (remote in remoteReports) {
            val existing = localReportsMap[remote.id]
            if (shouldAcceptRemoteCitizenReport(existing, remote)) {
                val mergedReport = remote.copy(
                    photoUri = com.example.util.ImageUtils.selectBestPhotoUri(remote.photoUri, existing?.photoUri),
                    handledByOfficerName = remote.handledByOfficerName.ifBlank { existing?.handledByOfficerName.orEmpty() },
                    handledByOfficerNip = remote.handledByOfficerNip.ifBlank { existing?.handledByOfficerNip.orEmpty() },
                    handledByOfficerRole = remote.handledByOfficerRole.ifBlank { existing?.handledByOfficerRole.orEmpty() },
                    isSyncedToCloud = true
                )
                weatherDao.insertCitizenDisasterReport(mapCitizenModelToEntity(mergedReport, isSyncedOverride = true))
                if (existing == null && localEntities.isNotEmpty()) {
                    com.example.ui.components.OfficerRealtimeNewReportDetector.sharedSessionDetector
                        .recordNewCitizenReport(mergedReport)
                }
            } else if (existing != null && !existing.isSyncedToCloud) {
                val localNorm = com.example.model.CitizenReportStatusHelper.normalizeStatus(existing.status)
                val remoteNorm = com.example.model.CitizenReportStatusHelper.normalizeStatus(remote.status)
                if (localNorm == remoteNorm && existing.additionalNotes.trim() == remote.additionalNotes.trim()) {
                    weatherDao.updateCitizenReportSyncState(remote.id, true)
                }
            }
        }
    }

    suspend fun seedInitialCitizenDisasterReportsIfEmpty() = withContext(databaseWriteDispatcher) {
        // PENTING: Hanya isi data contoh apabila tabel lokal benar-benar kosong setelah sinkronisasi Cloud.
        // Jangan pernah menimpa laporan atau pembaruan status petugas aktif yang sudah ada!
        if (weatherDao.getCitizenReportCount() > 0) return@withContext

        val fixedSeedBaseTs = 1761355800000L // 25 Okt 2025 (timestamp historis tetap agar tidak menimpa data nyata)
        val initialList = listOf(
            CitizenDisasterReportEntity(
                id = "LAP-MMK-20261025-001",
                disasterType = "Gelombang Pasang / Rob",
                incidentTime = "25 Okt 2026, 08:30 WIB",
                incidentLocation = "Pesisir Pantai Pasar Ipuh, Dekat TPI Nelayan",
                district = "Ipuh",
                village = "Pasar Ipuh",
                latitude = -3.0083,
                longitude = 101.4912,
                gpsAccuracyMeters = 6.4f,
                isGpsVerified = true,
                casualtiesDead = 0,
                casualtiesInjured = 0,
                casualtiesMissing = 0,
                photoUri = "android.resource://com.example/drawable/citizen_report_coastal_surge",
                reporterName = "Nelayan Siaga Ipuh (Bpk. Hendra)",
                reporterPhone = "0812-7890-4412",
                additionalNotes = "Ombak pasang mencapai bibir tanggul pengaman, perahu nelayan dievakuasi ke dataran lebih tinggi.",
                timestamp = fixedSeedBaseTs,
                status = "Divalidasi",
                updatedAt = fixedSeedBaseTs,
                isSyncedToCloud = true
            ),
            CitizenDisasterReportEntity(
                id = "LAP-MMK-20261025-002",
                disasterType = "Cuaca Ekstrem / Badai",
                incidentTime = "25 Okt 2026, 06:15 WIB",
                incidentLocation = "Jl. Lintas Barat Sumatera Km 7, Depan SPBU",
                district = "Kota Mukomuko",
                village = "Koto Jaya",
                latitude = -2.5833,
                longitude = 101.1167,
                gpsAccuracyMeters = 4.8f,
                isGpsVerified = true,
                casualtiesDead = 0,
                casualtiesInjured = 1,
                casualtiesMissing = 0,
                photoUri = "android.resource://com.example/drawable/citizen_report_fallen_tree",
                reporterName = "Warga RT 03 Koto Jaya (Ibu Rina)",
                reporterPhone = "0852-6712-8891",
                additionalNotes = "Dahan pohon trembesi patah melintang di badan jalan lintas, arus lalu lintas sempat terhambat. Tim BPBD sudah tiba memotong kayu.",
                timestamp = fixedSeedBaseTs - 70 * 60 * 1000L,
                status = "Divalidasi",
                updatedAt = fixedSeedBaseTs - 70 * 60 * 1000L,
                isSyncedToCloud = true
            ),
            CitizenDisasterReportEntity(
                id = "LAP-MMK-20261024-003",
                disasterType = "Banjir Luapan Sungai",
                incidentTime = "24 Okt 2026, 17:45 WIB",
                incidentLocation = "Dekat Jembatan Irigasi Manjunto",
                district = "Lubuk Pinang",
                village = "Aie Manjunto",
                latitude = -2.4833,
                longitude = 101.1833,
                gpsAccuracyMeters = 8.1f,
                isGpsVerified = true,
                casualtiesDead = 0,
                casualtiesInjured = 0,
                casualtiesMissing = 0,
                photoUri = "android.resource://com.example/drawable/hero_banner_bpbd",
                reporterName = "Kelompok Petani Manjunto (Bpk. Darmawan)",
                reporterPhone = "0813-9021-3304",
                additionalNotes = "Air saluran primer meluap sekitar 20 cm ke pekarangan jalan tani akibat hujan lebat perbukitan.",
                timestamp = fixedSeedBaseTs - 155 * 60 * 1000L,
                status = "Diproses",
                updatedAt = fixedSeedBaseTs - 155 * 60 * 1000L,
                isSyncedToCloud = true
            )
        )
        weatherDao.insertAllCitizenDisasterReports(initialList)
    }

    // Historical Weather Data Query
    suspend fun fetchHistoricalWeather(
        district: District,
        dateString: String // YYYY-MM-DD
    ): HistoricalWeatherRecord = withContext(Dispatchers.IO) {
        // Check cache first
        val cached = weatherDao.getHistoricalCache(district.id, dateString)
        if (cached != null) {
            return@withContext HistoricalWeatherRecord(
                date = cached.date,
                districtName = district.name,
                tempMean = cached.tempMean,
                tempMax = cached.tempMax,
                tempMin = cached.tempMin,
                precipitationSum = cached.precipitationSum,
                weatherCode = cached.weatherCode,
                windSpeedMax = cached.windSpeedMax,
                summaryText = generateHistoricalSummary(cached.weatherCode, cached.precipitationSum, cached.tempMean),
                comparisonWithAverage = generateHistoricalComparison(cached.tempMean, cached.precipitationSum)
            )
        }

        try {
            val response = archiveApi.getHistoricalWeather(
                latitude = district.latitude,
                longitude = district.longitude,
                startDate = dateString,
                endDate = dateString
            )

            val daily = response.daily
            val tempMean = daily?.temperature2mMean?.firstOrNull() ?: 28.5
            val tempMax = daily?.temperature2mMax?.firstOrNull() ?: (tempMean + 3.2)
            val tempMin = daily?.temperature2mMin?.firstOrNull() ?: (tempMean - 4.1)
            val precip = daily?.precipitationSum?.firstOrNull() ?: 4.2
            val code = daily?.weatherCode?.firstOrNull() ?: (if (precip > 5.0) 61 else 1)
            val windMax = daily?.windSpeed10mMax?.firstOrNull() ?: 14.8

            // Save to cache
            weatherDao.insertHistoricalCache(
                HistoricalWeatherEntity(
                    id = "${district.id}_$dateString",
                    districtId = district.id,
                    date = dateString,
                    tempMean = tempMean,
                    tempMax = tempMax,
                    tempMin = tempMin,
                    precipitationSum = precip,
                    weatherCode = code,
                    windSpeedMax = windMax
                )
            )

            HistoricalWeatherRecord(
                date = dateString,
                districtName = district.name,
                tempMean = tempMean,
                tempMax = tempMax,
                tempMin = tempMin,
                precipitationSum = precip,
                weatherCode = code,
                windSpeedMax = windMax,
                summaryText = generateHistoricalSummary(code, precip, tempMean),
                comparisonWithAverage = generateHistoricalComparison(tempMean, precip)
            )
        } catch (_: Exception) {
            // Calibrated realistic historical calculation for Mukomuko tropical rainforest climate
            val dayHash = Math.abs((district.id + dateString).hashCode())
            val tempMean = 27.5 + (dayHash % 30) / 10.0
            val precip = when (dayHash % 5) {
                0 -> 0.0
                1 -> 2.4
                2 -> 14.8
                3 -> 38.5
                else -> 8.0
            }
            val code = when {
                precip > 30.0 -> 95 // Hujan Petir
                precip > 10.0 -> 63 // Hujan Sedang
                precip > 0.0 -> 61  // Hujan Ringan
                (dayHash % 2 == 0) -> 1 // Cerah Berawan
                else -> 2 // Berawan
            }
            val tempMax = tempMean + 3.5
            val tempMin = tempMean - 4.2
            val windMax = 12.0 + (dayHash % 10)

            HistoricalWeatherRecord(
                date = dateString,
                districtName = district.name,
                tempMean = tempMean,
                tempMax = tempMax,
                tempMin = tempMin,
                precipitationSum = precip,
                weatherCode = code,
                windSpeedMax = windMax,
                summaryText = generateHistoricalSummary(code, precip, tempMean),
                comparisonWithAverage = generateHistoricalComparison(tempMean, precip)
            )
        }
    }

    private fun generateHistoricalSummary(code: Int, precip: Double, meanTemp: Double): String {
        return when {
            precip > 30.0 -> "Curah hujan tinggi ($precip mm) mengguyur wilayah ini disertai hembusan angin laut dan petir."
            precip > 10.0 -> "Hari basah dengan intensitas hujan sedang ($precip mm), suhu rata-rata sejuk ${String.format(Locale.US, "%.1f", meanTemp)}°C."
            precip > 0.0 -> "Kondisi cerah berawan di pagi hari dengan hujan rintik ringan ($precip mm) pada siang hingga petang."
            else -> "Cuaca kering dan cerah sepanjang hari dengan suhu rata-rata ${String.format(Locale.US, "%.1f", meanTemp)}°C."
        }
    }

    private fun generateHistoricalComparison(meanTemp: Double, precip: Double): String {
        val tempDiff = meanTemp - 28.0
        val tempDesc = when {
            tempDiff > 0.8 -> "${String.format(Locale.US, "+%.1f", tempDiff)}°C lebih hangat dari rata-rata klimatologis Mukomuko"
            tempDiff < -0.8 -> "${String.format(Locale.US, "%.1f", tempDiff)}°C lebih sejuk dari rata-rata klimatologis Mukomuko"
            else -> "Sesuai dengan suhu normal klimatologis tropis Mukomuko (28°C)"
        }
        val rainDesc = if (precip > 20.0) "Curah hujan di atas rata-rata bulanan" else "Curah hujan dalam rentang aman"
        return "$tempDesc • $rainDesc"
    }

    // Detailed Early Warnings List
    fun getDetailedEarlyWarnings(currentDistrict: District): List<DetailedEarlyWarning> {
        return listOf(
            DetailedEarlyWarning(
                id = "warn_marine_1",
                title = "Peringatan Dini Gelombang Tinggi Samudra Hindia Barat Bengkulu",
                severity = "SIAGA",
                category = "Gelombang Tinggi Pesisir",
                affectedDistricts = listOf("Kota Mukomuko", "Ipuh", "Teramang Jaya", "Air Dikit", "Pondok Suguh", "Air Rami", "Sungai Rumbai"),
                validUntil = "Berlaku hingga 3 hari ke depan (Pukul 23:59 WIB)",
                description = "Prakiraan BMKG Maritim: Tinggi gelombang laut mencapai 2.5 hingga 4.0 meter di perairan Samudra Hindia sebelah barat Bengkulu dan Mukomuko, dipicu pola sirkulasi angin siklonik.",
                safetyInstructions = listOf(
                    "Perahu Nelayan tradisional (kecepatan angin >15 knot & tinggi gelombang >1.25 m) diimbau tidak melaut.",
                    "Kapal Tongkang (kecepatan angin >16 knot & tinggi gelombang >1.5 m) agar meningkatkan kewaspadaan.",
                    "Masyarakat dan wisatawan di Pantai Abrasi, Pantai Muara Sebakul, dan Pantai Air Rami dilarang berenang di bibir pantai."
                ),
                bpbdContact = "Pusdalops BPBD Mukomuko: Call Center 117 / Call Center Darurat 112"
            ),
            DetailedEarlyWarning(
                id = "warn_rain_2",
                title = "Waspada Hujan Lebat Disertai Petir & Angin Kencang",
                severity = "WASPADA",
                category = "Cuaca Ekstrem Tropis",
                affectedDistricts = listOf("Kota Mukomuko", "V Koto", "XIV Koto", "Lubuk Pinang", "Selagan Raya", "Penarik"),
                validUntil = "Siang hingga malam hari (Pukul 21:00 WIB)",
                description = "Terpantau pertumbuhan awan Cumulonimbus (CB) intensif di sepanjang kaki Bukit Barisan dan aliran Sungai Manjunto serta Sungai Selagan.",
                safetyInstructions = listOf(
                    "Waspadai potensi pohon tumbang dan papan reklame roboh di sepanjang jalur lintas barat Sumatera.",
                    "Masyarakat di bantaran sungai Manjunto & Selagan diimbau memantau kenaikan debit air permukaan.",
                    "Cabut colokan listrik dan hindari berada di tempat terbuka saat terjadi petir petang hari."
                ),
                bpbdContact = "Tim Reaksi Cepat (TRC) BPBD Kabupaten Mukomuko"
            ),
            DetailedEarlyWarning(
                id = "warn_river_3",
                title = "Siaga Luapan Daerah Aliran Sungai (DAS) Manjunto",
                severity = "WASPADA",
                category = "Potensi Genangan & Luapan DAS",
                affectedDistricts = listOf("Lubuk Pinang", "V Koto", "Air Manjunto", "XIV Koto"),
                validUntil = "Hingga debit air bendungan normal kembali",
                description = "Tingginya curah hujan di hulu perbatasan Kerinci dan V Koto berpotensi meningkatkan volume air pada pintu intake Bendungan Air Manjunto.",
                safetyInstructions = listOf(
                    "Petani sawah irigasi teknis agar mengatur saluran pembuangan air tersier.",
                    "Hindari memancing atau beraktivitas di dekat bantaran tanggul sungai yang rawan longsor."
                ),
                bpbdContact = "Balai Wilayah Sungai (BWS) & BPBD Mukomuko"
            )
        )
    }

    /**
     * Memastikan tabel Room `weather_cache` memiliki data awal untuk seluruh 15 kecamatan
     * di Kabupaten Mukomuko sehingga saat aplikasi dibuka pertama kali dalam kondisi offline,
     * data cuaca 15 kecamatan beserta timestamp terakhir tetap tersedia dari Room Database.
     */
    suspend fun seedInitial15DistrictsWeatherCacheIfEmpty() = withContext(databaseWriteDispatcher) {
        val existingIds = weatherDao.getAllCached().map { it.districtId }.toSet()
        val now = System.currentTimeMillis()
        val missingEntities = com.example.model.MukomukoDistricts.list
            .filter { it.id !in existingIds }
            .mapIndexed { idx, dist ->
                val baseTemp = 28.5 + (idx % 4) * 0.6
                val code = when (idx % 4) {
                    0 -> 1 // Cerah Berawan
                    1 -> 2 // Berawan
                    2 -> 61 // Hujan Ringan
                    else -> 0 // Cerah
                }
                val condText = com.example.model.WeatherCodeHelper.getWeatherInfo(code).conditionText
                WeatherCacheEntity(
                    districtId = dist.id,
                    districtName = dist.name,
                    temperature = baseTemp,
                    feelsLike = baseTemp + 2.2,
                    weatherCode = code,
                    conditionText = condText,
                    humidity = 75 + (idx % 10),
                    precipitation = if (code == 61) 2.4 else 0.0,
                    windSpeed = 11.0 + (idx % 6),
                    windDirection = 210 + (idx * 10) % 60,
                    surfacePressure = 1010.5,
                    time = "Tersimpan Lokal",
                    cachedAt = now
                )
            }
        if (missingEntities.isNotEmpty()) {
            weatherDao.insertAllCache(missingEntities)
        }
    }

    /**
     * Mengamati perubahan cache cuaca 15 kecamatan secara reaktif dari Room Database (`Flow`).
     */
    fun observeAllDistrictsWeatherCache(): Flow<List<WeatherFullReport>> {
        return weatherDao.observeAllCachedWeather().map { cachedList ->
            val cacheMap = cachedList.associateBy { it.districtId }
            com.example.model.MukomukoDistricts.list.map { dist ->
                val cached = cacheMap[dist.id]
                if (cached != null) {
                    buildFallbackReportFromCache(dist, cached)
                } else {
                    buildDefaultOfflineReport(dist)
                }
            }
        }
    }

    /**
     * Mengambil daftar ringkasan cuaca 15 kecamatan yang tersimpan di Room Database beserta timestamp-nya.
     */
    suspend fun getCachedAll15DistrictsSummary(): List<WeatherFullReport> = withContext(Dispatchers.IO) {
        val cachedList = weatherDao.getAllCached()
        val cacheMap = cachedList.associateBy { it.districtId }
        com.example.model.MukomukoDistricts.list.map { dist ->
            val cached = cacheMap[dist.id]
            if (cached != null) {
                buildFallbackReportFromCache(dist, cached)
            } else {
                buildDefaultOfflineReport(dist)
            }
        }
    }

    suspend fun fetchWeatherReport(district: District): WeatherFullReport = withContext(Dispatchers.IO) {
        try {
            val forecastResp = forecastApi.getForecast(
                latitude = district.latitude,
                longitude = district.longitude
            )

            val aqiResp = try {
                airQualityApi.getAirQuality(
                    latitude = district.latitude,
                    longitude = district.longitude
                )
            } catch (_: Exception) {
                null
            }

            val now = System.currentTimeMillis()
            val report = mapToDomainReport(district, forecastResp, aqiResp, isFromCache = false)
                .copy(lastUpdated = now, isFromCache = false)

            // Simpan data cuaca terbaru beserta timestamp ke Room Database (weather_cache)
            withContext(databaseWriteDispatcher) {
                weatherDao.insertCache(
                    WeatherCacheEntity(
                        districtId = district.id,
                        districtName = district.name,
                        temperature = report.current.temperature,
                        feelsLike = report.current.feelsLike,
                        weatherCode = report.current.weatherCode,
                        conditionText = report.current.weatherInfo.conditionText,
                        humidity = report.current.humidity,
                        precipitation = report.current.precipitation,
                        windSpeed = report.current.windSpeed,
                        windDirection = report.current.windDirection,
                        surfacePressure = report.current.surfacePressure,
                        time = report.current.time,
                        cachedAt = now
                    )
                )
            }

            report
        } catch (e: Exception) {
            val cached = weatherDao.getCachedWeather(district.id)
            if (cached != null) {
                buildFallbackReportFromCache(district, cached)
            } else {
                val defaultReport = buildDefaultOfflineReport(district)
                withContext(databaseWriteDispatcher) {
                    weatherDao.insertCache(
                        WeatherCacheEntity(
                            districtId = district.id,
                            districtName = district.name,
                            temperature = defaultReport.current.temperature,
                            feelsLike = defaultReport.current.feelsLike,
                            weatherCode = defaultReport.current.weatherCode,
                            conditionText = defaultReport.current.weatherInfo.conditionText,
                            humidity = defaultReport.current.humidity,
                            precipitation = defaultReport.current.precipitation,
                            windSpeed = defaultReport.current.windSpeed,
                            windDirection = defaultReport.current.windDirection,
                            surfacePressure = defaultReport.current.surfacePressure,
                            time = defaultReport.current.time,
                            cachedAt = defaultReport.lastUpdated
                        )
                    )
                }
                defaultReport
            }
        }
    }

    private fun mapToDomainReport(
        district: District,
        forecast: OpenMeteoForecastResponse,
        aqiResp: OpenMeteoAirQualityResponse?,
        isFromCache: Boolean
    ): WeatherFullReport {
        val currentDto = forecast.current
        val temp = currentDto?.temperature2m ?: 30.5
        val feelsLike = currentDto?.apparentTemperature ?: (temp + 2.0)
        val humidity = currentDto?.relativeHumidity2m ?: 78
        val precip = currentDto?.precipitation ?: 0.0
        val code = currentDto?.weatherCode ?: 1
        val pressure = currentDto?.surfacePressure ?: 1010.0
        val windSpeed = currentDto?.windSpeed10m ?: 12.0
        val windDir = currentDto?.windDirection10m ?: 220
        val timeNow = currentDto?.time ?: "Sekarang"

        val current = CurrentWeather(
            temperature = temp,
            feelsLike = feelsLike,
            humidity = humidity,
            precipitation = precip,
            weatherCode = code,
            surfacePressure = pressure,
            windSpeed = windSpeed,
            windDirection = windDir,
            time = timeNow
        )

        val hourlyList = mutableListOf<HourlyItem>()
        val hourlyDto = forecast.hourly
        if (hourlyDto != null && !hourlyDto.time.isNullOrEmpty()) {
            val total = minOf(
                24,
                hourlyDto.time.size,
                hourlyDto.temperature2m?.size ?: 0
            )
            for (i in 0 until total) {
                val rawTime = hourlyDto.time[i]
                val hourLabel = formatHourLabel(rawTime, i)
                val hTemp = hourlyDto.temperature2m?.getOrNull(i) ?: temp
                val hCode = hourlyDto.weatherCode?.getOrNull(i) ?: code
                val hRainProb = hourlyDto.precipitationProbability?.getOrNull(i) ?: 15
                val hHumid = hourlyDto.relativeHumidity2m?.getOrNull(i) ?: humidity
                val hWind = hourlyDto.windSpeed10m?.getOrNull(i) ?: windSpeed

                hourlyList.add(
                    HourlyItem(
                        time = rawTime,
                        hourLabel = hourLabel,
                        temperature = hTemp,
                        weatherCode = hCode,
                        rainProbability = hRainProb,
                        humidity = hHumid,
                        windSpeed = hWind
                    )
                )
            }
        }

        if (hourlyList.isEmpty()) {
            hourlyList.addAll(generateMockHourly(temp, code))
        }

        val dailyList = mutableListOf<DailyItem>()
        val dailyDto = forecast.daily
        if (dailyDto != null && !dailyDto.time.isNullOrEmpty()) {
            val count = minOf(7, dailyDto.time.size)
            for (i in 0 until count) {
                val rawDate = dailyDto.time[i]
                val dayName = formatDayName(rawDate, i)
                val dCode = dailyDto.weatherCode?.getOrNull(i) ?: code
                val maxT = dailyDto.temperature2mMax?.getOrNull(i) ?: (temp + 2.0)
                val minT = dailyDto.temperature2mMin?.getOrNull(i) ?: (temp - 5.0)
                val rainProb = dailyDto.precipitationProbabilityMax?.getOrNull(i) ?: 30
                val uvMax = dailyDto.uvIndexMax?.getOrNull(i) ?: 8.5
                val windMax = dailyDto.windSpeed10mMax?.getOrNull(i) ?: (windSpeed + 4.0)
                val sunrise = dailyDto.sunrise?.getOrNull(i)?.takeLast(5) ?: "06:05"
                val sunset = dailyDto.sunset?.getOrNull(i)?.takeLast(5) ?: "18:15"

                dailyList.add(
                    DailyItem(
                        date = rawDate,
                        dayName = dayName,
                        weatherCode = dCode,
                        maxTemp = maxT,
                        minTemp = minT,
                        rainProbabilityMax = rainProb,
                        precipitationSum = dailyDto.precipitationSum?.getOrNull(i) ?: 0.0,
                        uvIndexMax = uvMax,
                        windSpeedMax = windMax,
                        sunrise = sunrise,
                        sunset = sunset
                    )
                )
            }
        }

        if (dailyList.isEmpty()) {
            dailyList.addAll(generateMockDaily(temp, code))
        }

        val windKnots = windSpeed * 0.539957
        val waveHeight = when {
            windSpeed > 30.0 -> 2.8
            windSpeed > 20.0 -> 1.8
            windSpeed > 10.0 -> 1.2
            else -> 0.7
        }

        val seaCond = when {
            waveHeight >= 2.5 -> "Gelombang Tinggi (2.5 - 3.5 m)"
            waveHeight >= 1.5 -> "Sedang (1.25 - 2.5 m)"
            else -> "Tenang - Rendah (0.5 - 1.25 m)"
        }

        val fishermanSafety = when {
            waveHeight >= 2.5 || code in listOf(95, 96, 99) -> "Bahaya Bagi Semua Perahu"
            waveHeight >= 1.5 -> "Waspada Perahu Nelayan Kecil"
            else -> "Aman Melaut & Aktivitas Pesisir"
        }

        val fishermanAdvice = when {
            fishermanSafety.startsWith("Bahaya") ->
                "BMKG Stasiun Maritim mengimbau seluruh perahu katir nelayan dan tongkang di perairan Mukomuko menunda aktivitas melaut karena ancaman squall line dan swell Samudra Hindia."
            fishermanSafety.startsWith("Waspada") ->
                "Kecepatan angin lepas pantai mencapai ${String.format(Locale.US, "%.1f", windKnots)} knot. Nelayan tradisional Ipuh, Air Dikit, dan Pantai Pasar Mukomuko disarankan tidak terlalu jauh ke tengah laut."
            else ->
                "Kondisi perairan pesisir barat Mukomuko relatif kondusif. Cocok untuk nelayan melaut dan wisata bahari di Pantai Abrasi atau Danau Nibung."
        }

        val marine = MarineWeather(
            waveHeight = waveHeight,
            seaCondition = seaCond,
            windSpeedKnots = windKnots,
            tideStatus = "Pasang Tertinggi 1.6m (Pukul 17:30 WIB)",
            fishermanSafety = fishermanSafety,
            fishermanAdvice = fishermanAdvice,
            waterTemp = 29.5
        )

        val pm25 = aqiResp?.current?.pm2_5 ?: 14.2
        val pm10 = aqiResp?.current?.pm10 ?: 22.8
        val usAqi = aqiResp?.current?.usAqi ?: 35

        val aqiStatus = when {
            usAqi <= 50 -> "Baik"
            usAqi <= 100 -> "Sedang"
            else -> "Tidak Sehat"
        }

        val aqiAdvice = when (aqiStatus) {
            "Baik" -> "Kualitas udara Mukomuko sangat segar dan bersih, terlindungi oleh hutan lindung dan hembusan angin samudra."
            "Sedang" -> "Kualitas udara cukup baik, aman untuk aktivitas luar ruangan dan olahraga."
            else -> "Partikel debu meningkat, kelompok sensitif disarankan memakai masker saat melintasi jalur lintas barat."
        }

        val airQuality = AirQuality(
            aqi = usAqi,
            pm25 = pm25,
            pm10 = pm10,
            status = aqiStatus,
            recommendation = aqiAdvice
        )

        val alert = if (code in listOf(95, 96, 99) || windSpeed > 25.0 || waveHeight > 2.0) {
            WeatherAlert(
                id = "alert_extreme_${district.id}",
                title = "Peringatan Dini Cuaca & Gelombang Pesisir",
                severity = "Waspada",
                message = "Waspada potensi cuaca berawan tebal disertai kilat/petir dan peningkatan gelombang tinggi di pesisir Mukomuko dan sekitarnya.",
                validUntil = "Hingga malam hari (22:00 WIB)"
            )
        } else {
            null
        }

        return WeatherFullReport(
            district = district,
            current = current,
            hourly = hourlyList,
            daily = dailyList,
            marine = marine,
            airQuality = airQuality,
            alert = alert,
            lastUpdated = System.currentTimeMillis(),
            isFromCache = isFromCache
        )
    }

    private fun formatHourLabel(rawIsoTime: String, index: Int): String {
        return try {
            val ldt = LocalDateTime.parse(rawIsoTime)
            ldt.format(DateTimeFormatter.ofPattern("HH:mm"))
        } catch (_: Exception) {
            val h = (index % 24)
            String.format(Locale.US, "%02d:00", h)
        }
    }

    private fun formatDayName(rawIsoDate: String, index: Int): String {
        if (index == 0) return "Hari Ini"
        if (index == 1) return "Besok"
        return try {
            val ld = LocalDate.parse(rawIsoDate)
            when (ld.dayOfWeek.value) {
                1 -> "Senin"
                2 -> "Selasa"
                3 -> "Rabu"
                4 -> "Kamis"
                5 -> "Jumat"
                6 -> "Sabtu"
                else -> "Minggu"
            }
        } catch (_: Exception) {
            "H+${index}"
        }
    }

    private fun buildFallbackReportFromCache(district: District, cache: WeatherCacheEntity): WeatherFullReport {
        val current = CurrentWeather(
            temperature = cache.temperature,
            feelsLike = cache.feelsLike,
            humidity = cache.humidity,
            precipitation = cache.precipitation,
            weatherCode = cache.weatherCode,
            surfacePressure = cache.surfacePressure,
            windSpeed = cache.windSpeed,
            windDirection = cache.windDirection,
            time = cache.time
        )
        return WeatherFullReport(
            district = district,
            current = current,
            hourly = generateMockHourly(cache.temperature, cache.weatherCode),
            daily = generateMockDaily(cache.temperature, cache.weatherCode),
            marine = MarineWeather(
                waveHeight = 1.1,
                seaCondition = "Sedang (1.0 - 1.5 m)",
                windSpeedKnots = cache.windSpeed * 0.54,
                tideStatus = "Pasang Sore",
                fishermanSafety = "Aman Melaut",
                fishermanAdvice = "Kondisi perairan dari data tersimpan. Perhatikan perubahan cuaca mendadak di laut.",
                waterTemp = 29.0
            ),
            airQuality = AirQuality(
                aqi = 32,
                pm25 = 12.0,
                pm10 = 20.0,
                status = "Baik",
                recommendation = "Udara pesisir Mukomuko tergolong segar dan sehat."
            ),
            alert = null,
            lastUpdated = cache.cachedAt,
            isFromCache = true
        )
    }

    private fun buildDefaultOfflineReport(district: District): WeatherFullReport {
        val baseTemp = 30.0
        val code = 1
        val current = CurrentWeather(
            temperature = baseTemp,
            feelsLike = 32.5,
            humidity = 76,
            precipitation = 0.0,
            weatherCode = code,
            surfacePressure = 1011.0,
            windSpeed = 12.0,
            windDirection = 210,
            time = "Prakiraan Terbaru"
        )
        return WeatherFullReport(
            district = district,
            current = current,
            hourly = generateMockHourly(baseTemp, code),
            daily = generateMockDaily(baseTemp, code),
            marine = MarineWeather(
                waveHeight = 1.0,
                seaCondition = "Tenang (0.75 - 1.25 m)",
                windSpeedKnots = 7.0,
                tideStatus = "Pasang Menjelang Sore",
                fishermanSafety = "Aman Melaut & Wisata",
                fishermanAdvice = "Perairan pesisir barat Mukomuko terpantau kondusif bagi nelayan tradisional.",
                waterTemp = 29.5
            ),
            airQuality = AirQuality(
                aqi = 28,
                pm25 = 10.5,
                pm10 = 18.0,
                status = "Baik",
                recommendation = "Kualitas udara sangat bersih dan nyaman."
            ),
            alert = null,
            lastUpdated = System.currentTimeMillis(),
            isFromCache = true
        )
    }

    private fun generateMockHourly(temp: Double, baseCode: Int): List<HourlyItem> {
        val list = mutableListOf<HourlyItem>()
        val hours = listOf("06:00", "08:00", "10:00", "12:00", "14:00", "16:00", "18:00", "20:00", "22:00", "00:00", "02:00", "04:00")
        hours.forEachIndexed { i, label ->
            val offset = when (i) {
                0 -> -3.0
                1 -> -1.5
                2 -> 1.0
                3 -> 2.5
                4 -> 2.0
                5 -> 0.5
                6 -> -1.0
                else -> -3.0
            }
            list.add(
                HourlyItem(
                    time = label,
                    hourLabel = label,
                    temperature = temp + offset,
                    weatherCode = if (i in 3..5) 2 else baseCode,
                    rainProbability = (15 + (i * 3) % 40),
                    humidity = (70 + (i * 2) % 20),
                    windSpeed = 10.0 + (i % 6)
                )
            )
        }
        return list
    }

    suspend fun fetchLatestEarthquake(): com.example.model.BmkgEarthquake {
        return withContext(Dispatchers.IO) {
            try {
                val response = bmkgApi.getAutoGempa()
                val raw = response.infoGempa?.gempa
                if (raw != null) {
                    com.example.model.BmkgEarthquakeMapper.fromRaw(raw)
                } else {
                    getFallbackMukomukoEarthquake()
                }
            } catch (e: Exception) {
                getFallbackMukomukoEarthquake()
            }
        }
    }

    suspend fun fetchRecentEarthquakes(): List<com.example.model.BmkgEarthquake> {
        return withContext(Dispatchers.IO) {
            try {
                val response = bmkgApi.getRecentEarthquakes()
                val list = response.infoGempa?.gempaList
                if (!list.isNullOrEmpty()) {
                    list.map { com.example.model.BmkgEarthquakeMapper.fromRaw(it) }
                } else {
                    getFallbackRecentEarthquakes()
                }
            } catch (e: Exception) {
                getFallbackRecentEarthquakes()
            }
        }
    }

    fun getFallbackMukomukoEarthquake(): com.example.model.BmkgEarthquake {
        return com.example.model.BmkgEarthquake(
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
            feltDescription = "III-IV Mukomuko, II-III Teras Terunjam, II Air Rami",
            shakemapUrl = null,
            distanceKmToMukomuko = 42,
            isNearMukomuko = true,
            urgencyLevel = com.example.model.EarthquakeUrgency.CRITICAL
        )
    }

    private fun getFallbackRecentEarthquakes(): List<com.example.model.BmkgEarthquake> {
        return listOf(
            getFallbackMukomukoEarthquake(),
            com.example.model.BmkgEarthquake(
                id = "gempa_bengkulu_selatan",
                date = "15 Okt 2026",
                timeWib = "04:15:22 WIB",
                dateTimeIso = "2026-10-15T21:15:22+00:00",
                latitude = -4.22,
                longitude = 102.65,
                magnitude = 5.0,
                depth = "24 km",
                region = "53 km BaratDaya BENGKULU SELATAN",
                potentialTsunami = "Tidak berpotensi tsunami",
                feltDescription = "II-III Manna, II Mukomuko",
                shakemapUrl = null,
                distanceKmToMukomuko = 210,
                isNearMukomuko = true,
                urgencyLevel = com.example.model.EarthquakeUrgency.WARNING
            ),
            com.example.model.BmkgEarthquake(
                id = "gempa_pesisir_selatan",
                date = "13 Okt 2026",
                timeWib = "11:08:44 WIB",
                dateTimeIso = "2026-10-13T04:08:44+00:00",
                latitude = -1.98,
                longitude = 100.75,
                magnitude = 4.8,
                depth = "15 km",
                region = "32 km BaratDaya PESISIR SELATAN-SUMBAR",
                potentialTsunami = "Tidak berpotensi tsunami",
                feltDescription = "II-III Painan, II Mukomuko Utara",
                shakemapUrl = null,
                distanceKmToMukomuko = 115,
                isNearMukomuko = true,
                urgencyLevel = com.example.model.EarthquakeUrgency.ADVISORY
            ),
            com.example.model.BmkgEarthquake(
                id = "gempa_enggano",
                date = "11 Okt 2026",
                timeWib = "20:30:19 WIB",
                dateTimeIso = "2026-10-11T13:30:19+00:00",
                latitude = -5.45,
                longitude = 102.10,
                magnitude = 5.6,
                depth = "10 km",
                region = "68 km BaratDaya ENGGANO-BENGKULU",
                potentialTsunami = "Tidak berpotensi tsunami",
                feltDescription = "III Enggano, II Kota Bengkulu",
                shakemapUrl = null,
                distanceKmToMukomuko = 340,
                isNearMukomuko = false,
                urgencyLevel = com.example.model.EarthquakeUrgency.WARNING
            )
        )
    }

    private fun generateMockDaily(temp: Double, baseCode: Int): List<DailyItem> {
        val days = listOf("Hari Ini", "Besok", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
        return days.mapIndexed { idx, name ->
            DailyItem(
                date = "2026-10-${16 + idx}",
                dayName = name,
                weatherCode = when (idx) {
                    2 -> 61
                    4 -> 95
                    else -> baseCode
                },
                maxTemp = temp + 2.0 + (idx % 2),
                minTemp = temp - 4.5 - (idx % 2),
                rainProbabilityMax = when (idx) {
                    2 -> 60
                    4 -> 75
                    else -> 20
                },
                uvIndexMax = 8.0 + (idx % 3),
                windSpeedMax = 14.0 + (idx * 2),
                sunrise = "06:06",
                sunset = "18:14"
            )
        }
    }

    // Hazard Risk Zones (Offline Room Persistence)
    suspend fun ensureHazardZonesSeeded() {
        withContext(Dispatchers.IO) {
            val existing = weatherDao.getAllHazardZonesSync()
            if (existing.isEmpty()) {
                val seedList = listOf(
                    com.example.data.local.HazardRiskZoneEntity(
                        id = "hz_pesisir_mukomuko",
                        name = "Zona Pesisir Pantai Mukomuko - Abrasi & Gelombang Pasang",
                        type = "Gelombang & Abrasi",
                        riskLevel = "TINGGI",
                        centerLat = -2.5833,
                        centerLon = 101.1167,
                        radiusKm = 7.5f,
                        colorHex = "#DC2626",
                        description = "Garis pantai rawan abrasi & pasang air laut Samudra Hindia (Koto Jaya - Teramang Jaya).",
                        evacuationSop = "Evakuasi ke arah timur menjauhi garis pantai menuju dataran tinggi SP3 / Gedung Daerah."
                    ),
                    com.example.data.local.HazardRiskZoneEntity(
                        id = "hz_das_manjunto",
                        name = "Daerah Aliran Sungai (DAS) Manjunto - Rawan Banjir Luapan",
                        type = "Banjir DAS",
                        riskLevel = "TINGGI",
                        centerLat = -2.4833,
                        centerLon = 101.1833,
                        radiusKm = 8.0f,
                        colorHex = "#2563EB",
                        description = "Kawasan bantaran sungai dan irigasi primer Lubuk Pinang - XIV Koto.",
                        evacuationSop = "Pantau tinggi muka air bendung Manjunto. Evakuasi segera jika status Siaga 1 ke balai desa terdekat."
                    ),
                    com.example.data.local.HazardRiskZoneEntity(
                        id = "hz_das_selagan",
                        name = "DAS Selagan - Dataran Rendah Rawan Genangan",
                        type = "Banjir DAS",
                        riskLevel = "SEDANG",
                        centerLat = -2.6500,
                        centerLon = 101.2500,
                        radiusKm = 6.5f,
                        colorHex = "#0284C7",
                        description = "Kawasan luapan sungai Selagan Raya saat intensitas hujan tinggi di hulu perbukitan.",
                        evacuationSop = "Amankan barang berharga dan ternak ke titik kumpul tanggul tinggi."
                    ),
                    com.example.data.local.HazardRiskZoneEntity(
                        id = "hz_longsor_bukit",
                        name = "Zona Tebing Perbukitan V Koto & Malin Deman - Rawan Longsor",
                        type = "Tanah Longsor",
                        riskLevel = "TINGGI",
                        centerLat = -2.7500,
                        centerLon = 101.5500,
                        radiusKm = 9.0f,
                        colorHex = "#B45309",
                        description = "Kontur perbukitan curam rawan amblas dan guguran material saat curah hujan lebat berkelanjutan.",
                        evacuationSop = "Hindari melintas di bawah tebing curam dan relokasi sementara ke posko balai kecamatan."
                    ),
                    com.example.data.local.HazardRiskZoneEntity(
                        id = "hz_pesisir_ipuh",
                        name = "Zona Pesisir Ipuh & Air Rami - Potensi Gelombang Ekstrem",
                        type = "Gelombang & Abrasi",
                        riskLevel = "TINGGI",
                        centerLat = -3.0083,
                        centerLon = 101.4912,
                        radiusKm = 7.0f,
                        colorHex = "#EA580C",
                        description = "Pesisir dermaga dan sentra nelayan Ipuh langsung berhadapan dengan Samudra Hindia terbuka.",
                        evacuationSop = "Tarik perahu ke daratan kering berjarak minimal 50 meter dari bibir pantai."
                    ),
                    com.example.data.local.HazardRiskZoneEntity(
                        id = "hz_tes_gedung_daerah",
                        name = "TES Gedung Daerah Mukomuko",
                        type = "Titik Kumpul (TES)",
                        riskLevel = "ZONA AMAN (TES)",
                        centerLat = -2.5800,
                        centerLon = 101.1250,
                        radiusKm = 1.0f,
                        colorHex = "#16A34A",
                        description = "Tempat Evakuasi Sementara konstruksi beton bertingkat dengan logistik darurat BPBD.",
                        evacuationSop = "Ikuti rambu jalur evakuasi hijau dan prioritaskan kelompok rentan."
                    )
                )
                weatherDao.insertAllHazardZones(seedList)
            }
        }
    }

    fun getHazardZones(): Flow<List<com.example.data.local.HazardRiskZoneEntity>> = weatherDao.getAllHazardZones()
}

