package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.FirestoreHelper
import com.example.model.AppUsageGuide
import com.example.model.DefaultAppUsageGuideCatalog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Hasil pembacaan Panduan Penggunaan SIPAGAR KITA (Cloud Firestore -> Cache Lokal -> Bawaan).
 */
data class UsageGuideSyncResult(
    val guide: AppUsageGuide,
    val syncedFromCloud: Boolean,
    val statusMessage: String
)

/**
 * Repository 3-Lapis untuk Panduan Penggunaan SIPAGAR KITA (Warga & Petugas):
 * 1. Cloud Firestore (`app_settings/usage_guide`) -> pembaruan langsung tampil tanpa update/reinstall APK.
 * 2. Cache Lokal (`SharedPreferences`) -> tetap terbuka instan saat perangkat sedang offline di lapangan.
 * 3. Katalog Bawaan (`DefaultAppUsageGuideCatalog`) -> siap pakai sejak pemasangan pertama.
 */
object AppUsageGuideRepository {

    private const val TAG = "AppUsageGuideRepo"
    private const val PREFS_NAME = "sipagar_usage_guide_cache"
    private const val KEY_GUIDE_JSON = "cached_usage_guide_json"
    private const val KEY_LAST_SYNCED_MS = "cached_usage_guide_synced_at_ms"
    private const val FETCH_TIMEOUT_MS = 4_000L

    @Volatile
    private var inMemoryGuide: AppUsageGuide? = null

    @Volatile
    var disableFirestoreForTesting: Boolean = false

    @Volatile
    var overrideCloudGuideFetcherForTesting: (suspend () -> Result<Map<String, Any>?>)? = null

    @Volatile
    var overrideCloudGuidePublisherForTesting: (suspend (Map<String, Any>) -> Result<Unit>)? = null

    fun resetForTesting() {
        inMemoryGuide = null
        disableFirestoreForTesting = false
        overrideCloudGuideFetcherForTesting = null
        overrideCloudGuidePublisherForTesting = null
    }

    fun getLastSyncedAtMs(context: Context): Long {
        return try {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getLong(KEY_LAST_SYNCED_MS, 0L)
        } catch (_: Exception) {
            0L
        }
    }

    fun loadCachedGuide(context: Context): AppUsageGuide? {
        return try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_GUIDE_JSON, null).orEmpty()
            AppUsageGuide.fromJson(json, source = AppUsageGuide.SOURCE_CACHE)
        } catch (_: Exception) {
            null
        }
    }

    fun saveCachedGuide(context: Context, guide: AppUsageGuide) {
        try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_GUIDE_JSON, guide.toJsonString())
                .putLong(KEY_LAST_SYNCED_MS, System.currentTimeMillis())
                .apply()
            inMemoryGuide = guide
        } catch (e: Exception) {
            Log.w(TAG, "Gagal menyimpan cache panduan lokal: ${e.message}")
        }
    }

    fun clearCache(context: Context) {
        try {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
            inMemoryGuide = null
        } catch (_: Exception) {}
    }

    /**
     * Mengembalikan panduan paling segar yang tersedia secara sinkron (memori -> cache -> bawaan).
     */
    fun getCachedOrDefaultGuide(context: Context): AppUsageGuide {
        inMemoryGuide?.let { return it }
        val cached = loadCachedGuide(context)
        val resolved = cached ?: DefaultAppUsageGuideCatalog.defaultGuide()
        inMemoryGuide = resolved
        return resolved
    }

    /**
     * Mengambil pembaruan panduan langsung dari Cloud Firestore `app_settings/usage_guide`.
     * Jika dokumen belum diterbitkan di Cloud atau perangkat offline, tetap aman kembali ke cache/bawaan.
     */
    suspend fun fetchLatestGuide(context: Context): UsageGuideSyncResult = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val fallbackGuide = loadCachedGuide(appContext) ?: DefaultAppUsageGuideCatalog.defaultGuide()

        if (disableFirestoreForTesting && overrideCloudGuideFetcherForTesting == null) {
            inMemoryGuide = fallbackGuide
            return@withContext UsageGuideSyncResult(
                guide = fallbackGuide,
                syncedFromCloud = false,
                statusMessage = if (fallbackGuide.source == AppUsageGuide.SOURCE_CACHE) {
                    "Memakai panduan cache offline (${fallbackGuide.guideVersion})."
                } else {
                    "Memakai panduan bawaan (${fallbackGuide.guideVersion})."
                }
            )
        }

        val cloudResult: Result<Map<String, Any>?>? = withTimeoutOrNull(FETCH_TIMEOUT_MS) {
            try {
                overrideCloudGuideFetcherForTesting?.invoke()
                    ?: FirestoreHelper.fetchUsageGuideFromCloudSuspend(appContext)
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        val dataMap = cloudResult?.getOrNull()
        val parsed = dataMap?.let { AppUsageGuide.fromFirestoreMap(it, source = AppUsageGuide.SOURCE_FIRESTORE) }

        if (parsed != null) {
            saveCachedGuide(appContext, parsed)
            inMemoryGuide = parsed
            return@withContext UsageGuideSyncResult(
                guide = parsed,
                syncedFromCloud = true,
                statusMessage = "Tersinkron Cloud (${parsed.guideVersion}) • Diperbarui ${parsed.updatedAtLabel}."
            )
        }

        val status = when {
            cloudResult == null ->
                "Koneksi lambat — menampilkan panduan offline (${fallbackGuide.guideVersion})."
            cloudResult.isSuccess && dataMap == null ->
                "Menggunakan panduan standar (${fallbackGuide.guideVersion}). Admin dapat menerbitkan pembaruan Cloud kapan saja."
            else ->
                "Mode offline aktif (${fallbackGuide.guideVersion}). Akan sinkron otomatis saat terhubung."
        }

        inMemoryGuide = fallbackGuide
        UsageGuideSyncResult(
            guide = fallbackGuide,
            syncedFromCloud = false,
            statusMessage = status
        )
    }

    /**
     * Menerbitkan panduan versi terbaru ke Cloud Firestore `app_settings/usage_guide`
     * sehingga seluruh ponsel Warga & Petugas langsung menerimanya tanpa update APK.
     */
    suspend fun publishGuideToCloud(
        context: Context,
        guide: AppUsageGuide
    ): Result<AppUsageGuide> = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val sanitized = guide.copy(
            guideVersion = guide.guideVersion.trim().ifBlank { AppUsageGuide.DEFAULT_GUIDE_VERSION }.take(64),
            releaseTitle = guide.releaseTitle.trim().ifBlank { "Panduan Penggunaan SIPAGAR KITA" }.take(140),
            updatedAtLabel = guide.updatedAtLabel.trim().ifBlank { "Terbaru" }.take(80),
            updatedBy = guide.updatedBy.trim().ifBlank { "Admin BPBD Mukomuko" }.take(120),
            updatedAt = System.currentTimeMillis(),
            citizenSummary = guide.citizenSummary.trim().ifBlank { AppUsageGuide.DEFAULT_CITIZEN_SUMMARY }.take(500),
            officerSummary = guide.officerSummary.trim().ifBlank { AppUsageGuide.DEFAULT_OFFICER_SUMMARY }.take(500),
            citizenSteps = guide.citizenSteps
                .filter { it.title.isNotBlank() && it.description.isNotBlank() }
                .take(12)
                .mapIndexed { idx, step -> step.copy(stepNumber = idx + 1) }
                .ifEmpty { DefaultAppUsageGuideCatalog.citizenSteps },
            officerSteps = guide.officerSteps
                .filter { it.title.isNotBlank() && it.description.isNotBlank() }
                .take(12)
                .mapIndexed { idx, step -> step.copy(stepNumber = idx + 1) }
                .ifEmpty { DefaultAppUsageGuideCatalog.officerSteps },
            source = AppUsageGuide.SOURCE_FIRESTORE
        )

        val payload = sanitized.toFirestoreMap()
        val writeResult = try {
            overrideCloudGuidePublisherForTesting?.invoke(payload)
                ?: FirestoreHelper.saveUsageGuideToCloudSuspend(appContext, payload)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Result.failure(e)
        }

        writeResult.map {
            saveCachedGuide(appContext, sanitized)
            inMemoryGuide = sanitized
            sanitized
        }
    }
}
