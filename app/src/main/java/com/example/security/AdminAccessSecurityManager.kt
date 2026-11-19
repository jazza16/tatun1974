package com.example.security

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.BuildConfig
import com.example.R
import com.example.data.api.AdminAuthApi
import com.example.data.api.NetworkClient
import com.example.data.local.AppDatabase
import com.example.data.local.PersonnelAuditLogEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID

/**
 * Token akses sementara (Temporary Access Token) yang diterbitkan setelah verifikasi
 * sandi/PIN master admin berhasil.
 *
 * Masa berlaku dibatasi ketat antara 5 sampai 15 menit (default 10 menit) dan hanya disimpan
 * di memori (tidak ditulis plaintext ke penyimpanan lokal).
 */
data class AdminTemporaryAccessToken(
    val token: String,
    val issuedToNip: String,
    val issuedToName: String,
    val scope: String,
    val issuedAtMs: Long,
    val expiresAtMs: Long,
    val verificationSource: String = "FIRESTORE_ADMIN_CONFIG"
) {
    fun isValid(nowMs: Long = System.currentTimeMillis()): Boolean {
        return token.isNotBlank() && nowMs < expiresAtMs
    }

    fun isValidForOfficer(officerNip: String, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (!isValid(nowMs)) return false
        val normalizedTarget = officerNip.trim()
        val normalizedOwner = issuedToNip.trim()
        if (normalizedTarget.isBlank() || normalizedOwner.isBlank()) return false
        return normalizedTarget.equals(normalizedOwner, ignoreCase = true)
    }

    fun remainingSeconds(nowMs: Long = System.currentTimeMillis()): Long {
        val diff = expiresAtMs - nowMs
        return if (diff > 0L) (diff + 999L) / 1000L else 0L
    }
}

sealed class AdminVerificationResult {
    data class Authorized(
        val accessToken: AdminTemporaryAccessToken,
        val message: String
    ) : AdminVerificationResult()

    data class Denied(
        val message: String,
        val httpCode: Int? = null,
        val canRetry: Boolean = false,
        val errorCode: String? = null
    ) : AdminVerificationResult()
}

/**
 * Hasil pembacaan dokumen Firestore `app_settings/admin_config` field `masterPin`.
 */
sealed class FirestoreMasterPinLookup {
    data class Found(val masterPin: String, val sourceLabel: String) : FirestoreMasterPinLookup()
    data object DocumentMissing : FirestoreMasterPinLookup()
    data object FieldEmpty : FirestoreMasterPinLookup()
    data class DatabaseError(
        val userMessage: String,
        val detail: String,
        val errorCode: String,
        val canRetry: Boolean
    ) : FirestoreMasterPinLookup()
}

/**
 * Manajer Keamanan Otorisasi Menu Sensitif ("Kelola Personil", Database Explorer, dll.).
 *
 * Alur Verifikasi:
 * 1. **Inisialisasi Awal**: Memastikan `FirebaseApp.initializeApp(context)` dan instance
 *    `FirebaseFirestore.getInstance(app, firestore_database_id)` siap sebelum verifikasi dipanggil.
 * 2. **Force Server Read (`Source.SERVER`)**: Membaca dokumen `app_settings/admin_config` field
 *    `masterPin` langsung dari server Firestore (bukan cache) agar selalu sinkron.
 * 3. **Error Handling Spesifik & Fallback Coba Lagi**: Menangkap `FirebaseFirestoreException`
 *    (`UNAVAILABLE`, `DEADLINE_EXCEEDED`, `PERMISSION_DENIED`, dll.) dan memberikan pesan spesifik
 *    disertai sinyal `canRetry = true` agar UI menampilkan tombol **"Coba Lagi"**.
 * 4. **Temporary Access Token (TTL 5–15 menit)** + **Audit Log** (Room + Firestore).
 */
object AdminAccessSecurityManager {

    private const val TAG = "AdminAccessSecMgr"
    private const val COLLECTION_ADMIN_AUDIT_LOGS = "admin_access_audit_logs"
    private const val DEFAULT_FIRESTORE_DB_ID = "ai-studio-android-cuacamuk-a1e6cb6f-11e7-41ba-90f4-82c9adb67961"
    private const val API_TIMEOUT_MS = 10_000L

    const val FIRESTORE_COLLECTION_APP_SETTINGS = "app_settings"
    const val FIRESTORE_DOC_ADMIN_CONFIG = "admin_config"
    const val FIRESTORE_FIELD_MASTER_PIN = "masterPin"
    const val FIRESTORE_ADMIN_CONFIG_PATH = "$FIRESTORE_COLLECTION_APP_SETTINGS/$FIRESTORE_DOC_ADMIN_CONFIG"

    const val MIN_TOKEN_TTL_SECONDS = 5 * 60L       // 5 menit
    const val DEFAULT_TOKEN_TTL_SECONDS = 10 * 60L  // 10 menit
    const val MAX_TOKEN_TTL_SECONDS = 15 * 60L      // 15 menit

    const val ACTION_GRANTED = "ADMIN_ACCESS_GRANTED"
    const val ACTION_DENIED = "ADMIN_ACCESS_DENIED"
    const val ACTION_TOKEN_REUSED = "ADMIN_ACCESS_TOKEN_REUSED"
    const val ACTION_RBAC_GRANTED = "ADMIN_ACCESS_RBAC_GRANTED"
    const val ACTION_TOKEN_REVOKED = "ADMIN_ACCESS_TOKEN_REVOKED"

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    @Volatile
    private var activeToken: AdminTemporaryAccessToken? = null

    @Volatile
    var overrideApiForTesting: AdminAuthApi? = null

    @Volatile
    var overrideBaseUrlForTesting: String? = null

    @Volatile
    var overrideFirestorePinFetcherForTesting: (suspend () -> FirestoreMasterPinLookup)? = null

    @Volatile
    var disableFirestoreForTesting: Boolean = false

    @Volatile
    var clockProvider: () -> Long = { System.currentTimeMillis() }

    fun resetForTesting() {
        activeToken = null
        overrideApiForTesting = null
        overrideBaseUrlForTesting = null
        overrideFirestorePinFetcherForTesting = null
        disableFirestoreForTesting = false
        clockProvider = { System.currentTimeMillis() }
    }

    /**
     * Memastikan FirebaseApp telah diinisialisasi (`FirebaseApp.initializeApp`) dan
     * instance `FirebaseFirestore` terhubung ke Named Database ID proyek (`firestore_database_id`)
     * sebelum operasi verifikasi dijalankan.
     */
    fun ensureFirestoreReady(context: Context): Result<FirebaseFirestore> {
        return try {
            val appContext = context.applicationContext
            val firebaseApp = if (FirebaseApp.getApps(appContext).isEmpty()) {
                logDebug("FirebaseApp belum terinisialisasi -> memanggil FirebaseApp.initializeApp(context)")
                FirebaseApp.initializeApp(appContext)
                    ?: return Result.failure(
                        IllegalStateException("FirebaseApp gagal diinisialisasi. Periksa konfigurasi google-services.json.")
                    )
            } else {
                FirebaseApp.getInstance()
            }

            val databaseId = try {
                appContext.getString(R.string.firestore_database_id).trim().ifBlank { DEFAULT_FIRESTORE_DB_ID }
            } catch (_: Exception) {
                DEFAULT_FIRESTORE_DB_ID
            }

            logDebug("Menggunakan Firestore Database ID: '$databaseId'")
            val firestore = FirebaseFirestore.getInstance(firebaseApp, databaseId)
            Result.success(firestore)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal inisialisasi Firebase/Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Memetakan `FirebaseFirestoreException` ke pesan error spesifik & status `canRetry`.
     */
    fun mapFirestoreException(e: FirebaseFirestoreException): FirestoreMasterPinLookup.DatabaseError {
        val codeName = e.code.name
        val detail = "$codeName: ${e.localizedMessage ?: e.message ?: "Firestore error"}"
        return when (e.code) {
            FirebaseFirestoreException.Code.UNAVAILABLE -> FirestoreMasterPinLookup.DatabaseError(
                userMessage = "Koneksi internet bermasalah atau server Firestore tidak terjangkau (UNAVAILABLE). Periksa jaringan Anda lalu ketuk Coba Lagi.",
                detail = detail,
                errorCode = codeName,
                canRetry = true
            )
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> FirestoreMasterPinLookup.DatabaseError(
                userMessage = "Koneksi ke server database habis waktu (DEADLINE_EXCEEDED). Periksa sinyal internet lalu ketuk Coba Lagi.",
                detail = detail,
                errorCode = codeName,
                canRetry = true
            )
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> FirestoreMasterPinLookup.DatabaseError(
                userMessage = "Akses membaca '$FIRESTORE_ADMIN_CONFIG_PATH' ditolak oleh aturan keamanan database (PERMISSION_DENIED).",
                detail = detail,
                errorCode = codeName,
                canRetry = false
            )
            FirebaseFirestoreException.Code.NOT_FOUND -> FirestoreMasterPinLookup.DatabaseError(
                userMessage = "Database atau dokumen '$FIRESTORE_ADMIN_CONFIG_PATH' tidak ditemukan di server (NOT_FOUND).",
                detail = detail,
                errorCode = codeName,
                canRetry = true
            )
            else -> FirestoreMasterPinLookup.DatabaseError(
                userMessage = "Gagal terhubung ke database Firestore ($codeName). Periksa koneksi internet Anda lalu ketuk Coba Lagi.",
                detail = detail,
                errorCode = codeName,
                canRetry = true
            )
        }
    }

    fun hasRbacAdminRole(officerRole: String): Boolean {
        val normalized = officerRole.trim().lowercase()
        return normalized == "admin" ||
            normalized == "super admin" ||
            normalized == "administrator"
    }

    fun getActiveTokenForOfficer(officerNip: String): AdminTemporaryAccessToken? {
        val current = activeToken ?: return null
        val now = clockProvider()
        if (!current.isValid(now)) {
            activeToken = null
            return null
        }
        return if (current.isValidForOfficer(officerNip, now)) current else null
    }

    fun revokeToken() {
        activeToken = null
    }

    suspend fun recordTokenReuseAttempt(
        context: Context,
        officerNip: String,
        officerName: String,
        officerRole: String,
        actionTitle: String,
        token: AdminTemporaryAccessToken
    ) {
        val remainingSec = token.remainingSeconds(clockProvider())
        recordAuditLog(
            context = context,
            officerNip = officerNip,
            officerName = officerName,
            officerRole = officerRole,
            actionCode = ACTION_TOKEN_REUSED,
            actionTitle = actionTitle,
            success = true,
            detail = "Akses menggunakan Temporary Access Token aktif (sisa ${remainingSec} detik)"
        )
    }

    /**
     * Memverifikasi PIN/Sandi Master Admin:
     * - Menggunakan `POST /api/verify-admin-pass` jika REST API dikonfigurasi.
     * - Jika tidak, menginisialisasi Firebase/Firestore (Named DB), memaksa pembacaan dari server
     *   (`Source.SERVER`) pada `app_settings/admin_config` -> `masterPin`, dan membandingkan teks asli.
     */
    suspend fun verifyMasterPasswordAndIssueToken(
        context: Context,
        masterPassword: String,
        officerNip: String,
        officerName: String,
        officerRole: String,
        actionTitle: String
    ): AdminVerificationResult = withContext(Dispatchers.IO) {
        val safeNip = officerNip.trim().ifBlank { "UNKNOWN_NIP" }
        val safeName = officerName.trim().ifBlank { "Petugas Pusdalops" }
        val safeRole = officerRole.trim().ifBlank { "Petugas" }
        val trimmedPassword = masterPassword.trim()

        if (trimmedPassword.isEmpty()) {
            recordAuditLog(
                context = context,
                officerNip = safeNip,
                officerName = safeName,
                officerRole = safeRole,
                actionCode = ACTION_DENIED,
                actionTitle = actionTitle,
                success = false,
                detail = "Input PIN Master Admin kosong"
            )
            return@withContext AdminVerificationResult.Denied(
                message = "Masukkan PIN Master Admin terlebih dahulu.",
                canRetry = false
            )
        }

        // Jalur 1: Jika REST Server API (`POST /api/verify-admin-pass`) terkonfigurasi secara eksplisit
        val api = resolveApiClient()
        if (api != null) {
            return@withContext verifyViaRemoteRestApi(
                context = context,
                api = api,
                trimmedPassword = trimmedPassword,
                safeNip = safeNip,
                safeName = safeName,
                safeRole = safeRole,
                actionTitle = actionTitle
            )
        }

        // Jalur 2: Verifikasi langsung terhadap Cloud Firestore `app_settings/admin_config` -> `masterPin` (Force Source.SERVER)
        return@withContext verifyViaFirestoreAdminConfig(
            context = context,
            inputPin = trimmedPassword,
            safeNip = safeNip,
            safeName = safeName,
            safeRole = safeRole,
            actionTitle = actionTitle
        )
    }

    private suspend fun verifyViaFirestoreAdminConfig(
        context: Context,
        inputPin: String,
        safeNip: String,
        safeName: String,
        safeRole: String,
        actionTitle: String
    ): AdminVerificationResult {
        val debugPath = "$FIRESTORE_ADMIN_CONFIG_PATH (field: $FIRESTORE_FIELD_MASTER_PIN)"
        logDebug("Path Firestore diakses : $debugPath (Force Source.SERVER)")
        logDebug("PIN diketik pengguna   : '$inputPin'")

        val lookupResult = fetchMasterPinFromFirestoreServer(context)

        when (lookupResult) {
            is FirestoreMasterPinLookup.Found -> {
                val firestoreMasterPin = lookupResult.masterPin
                logDebug("Nilai masterPin Firestore (${lookupResult.sourceLabel}) : '$firestoreMasterPin'")
                logDebug("Metode verifikasi : Perbandingan teks asli (plaintext) tanpa mengubah data Firestore")

                val isPlaintextMatch = inputPin == firestoreMasterPin

                if (isPlaintextMatch) {
                    val now = clockProvider()
                    val expiresAtMs = now + (DEFAULT_TOKEN_TTL_SECONDS * 1000L)
                    val issuedToken = AdminTemporaryAccessToken(
                        token = "tmp_fs_${UUID.randomUUID().toString().replace("-", "").take(16)}",
                        issuedToNip = safeNip,
                        issuedToName = safeName,
                        scope = "KELOLA_PERSONIL_ADMIN",
                        issuedAtMs = now,
                        expiresAtMs = expiresAtMs,
                        verificationSource = "FIRESTORE:$FIRESTORE_ADMIN_CONFIG_PATH"
                    )
                    activeToken = issuedToken

                    logDebug("Hasil verifikasi : COCOK (BERHASIL) • Token sementara berlaku ${DEFAULT_TOKEN_TTL_SECONDS / 60} menit")
                    recordAuditLog(
                        context = context,
                        officerNip = safeNip,
                        officerName = safeName,
                        officerRole = safeRole,
                        actionCode = ACTION_GRANTED,
                        actionTitle = actionTitle,
                        success = true,
                        detail = "Verifikasi Firestore SERVER ($FIRESTORE_ADMIN_CONFIG_PATH) berhasil • TTL ${DEFAULT_TOKEN_TTL_SECONDS / 60} mnt"
                    )
                    return AdminVerificationResult.Authorized(
                        accessToken = issuedToken,
                        message = "Verifikasi PIN Admin berhasil. Token sementara aktif ${DEFAULT_TOKEN_TTL_SECONDS / 60} menit."
                    )
                } else {
                    activeToken = null
                    logDebug("Hasil verifikasi : TIDAK COCOK (PIN SALAH)")
                    recordAuditLog(
                        context = context,
                        officerNip = safeNip,
                        officerName = safeName,
                        officerRole = safeRole,
                        actionCode = ACTION_DENIED,
                        actionTitle = actionTitle,
                        success = false,
                        detail = "PIN salah saat verifikasi terhadap $FIRESTORE_ADMIN_CONFIG_PATH"
                    )
                    return AdminVerificationResult.Denied(
                        message = "PIN salah. Pastikan PIN Master Admin sesuai dengan pengaturan database.",
                        canRetry = false,
                        errorCode = "INVALID_PIN"
                    )
                }
            }

            is FirestoreMasterPinLookup.DocumentMissing -> {
                activeToken = null
                logDebug("Nilai masterPin Firestore : <DOKUMEN TIDAK DITEMUKAN pada $FIRESTORE_ADMIN_CONFIG_PATH>")
                recordAuditLog(
                    context = context,
                    officerNip = safeNip,
                    officerName = safeName,
                    officerRole = safeRole,
                    actionCode = ACTION_DENIED,
                    actionTitle = actionTitle,
                    success = false,
                    detail = "Dokumen $FIRESTORE_ADMIN_CONFIG_PATH tidak ditemukan di Firestore"
                )
                return AdminVerificationResult.Denied(
                    message = "Gagal verifikasi: Dokumen '$FIRESTORE_ADMIN_CONFIG_PATH' tidak ditemukan di server Firestore.",
                    canRetry = true,
                    errorCode = "DOCUMENT_NOT_FOUND"
                )
            }

            is FirestoreMasterPinLookup.FieldEmpty -> {
                activeToken = null
                logDebug("Nilai masterPin Firestore : <FIELD '$FIRESTORE_FIELD_MASTER_PIN' KOSONG / NULL>")
                recordAuditLog(
                    context = context,
                    officerNip = safeNip,
                    officerName = safeName,
                    officerRole = safeRole,
                    actionCode = ACTION_DENIED,
                    actionTitle = actionTitle,
                    success = false,
                    detail = "Field '$FIRESTORE_FIELD_MASTER_PIN' kosong pada $FIRESTORE_ADMIN_CONFIG_PATH"
                )
                return AdminVerificationResult.Denied(
                    message = "Gagal verifikasi: Field '$FIRESTORE_FIELD_MASTER_PIN' kosong atau belum diatur di '$FIRESTORE_ADMIN_CONFIG_PATH'.",
                    canRetry = true,
                    errorCode = "FIELD_EMPTY"
                )
            }

            is FirestoreMasterPinLookup.DatabaseError -> {
                activeToken = null
                logDebug("Nilai masterPin Firestore : <ERROR ${lookupResult.errorCode}: ${lookupResult.detail}>")
                recordAuditLog(
                    context = context,
                    officerNip = safeNip,
                    officerName = safeName,
                    officerRole = safeRole,
                    actionCode = ACTION_DENIED,
                    actionTitle = actionTitle,
                    success = false,
                    detail = "Error Firestore (${lookupResult.errorCode}): ${lookupResult.detail}"
                )
                return AdminVerificationResult.Denied(
                    message = lookupResult.userMessage,
                    canRetry = lookupResult.canRetry,
                    errorCode = lookupResult.errorCode
                )
            }
        }
    }

    /**
     * Memastikan Firebase + Firestore siap, mengaktifkan jaringan Firestore, lalu memaksa
     * pembacaan dari server (`Source.SERVER`) pada `app_settings/admin_config`.
     */
    private suspend fun fetchMasterPinFromFirestoreServer(context: Context): FirestoreMasterPinLookup {
        overrideFirestorePinFetcherForTesting?.let { return it.invoke() }

        if (disableFirestoreForTesting) {
            return FirestoreMasterPinLookup.DatabaseError(
                userMessage = "Koneksi internet bermasalah atau Firestore dinonaktifkan pada mode pengujian.",
                detail = "Firestore disabled in test mode",
                errorCode = "UNAVAILABLE",
                canRetry = true
            )
        }

        val db = ensureFirestoreReady(context).getOrElse { initErr ->
            return FirestoreMasterPinLookup.DatabaseError(
                userMessage = "Layanan Firebase/Firestore belum siap. Ketuk Coba Lagi beberapa saat lagi.",
                detail = initErr.localizedMessage ?: "Firebase init error",
                errorCode = "FIREBASE_NOT_INITIALIZED",
                canRetry = true
            )
        }

        return try {
            try {
                FirebaseAppCheck.getInstance().getAppCheckToken(false).await()
            } catch (_: Exception) {
                // Lanjutkan walaupun App Check token belum siap
            }

            try {
                withTimeoutOrNull(2_500L) { db.enableNetwork().await() }
            } catch (_: Exception) {
                // Abaikan jika network sudah aktif
            }

            val docRef = db.collection(FIRESTORE_COLLECTION_APP_SETTINGS)
                .document(FIRESTORE_DOC_ADMIN_CONFIG)

            // Force Server Read (Source.SERVER) — memastikan data diambil langsung dari server, bukan cache
            val snapshot = withTimeoutOrNull(API_TIMEOUT_MS) {
                docRef.get(Source.SERVER).await()
            } ?: return FirestoreMasterPinLookup.DatabaseError(
                userMessage = "Koneksi internet bermasalah atau server Firestore lambat merespons (TIMEOUT). Periksa jaringan lalu ketuk Coba Lagi.",
                detail = "Timeout ${API_TIMEOUT_MS}ms saat get(Source.SERVER) pada $FIRESTORE_ADMIN_CONFIG_PATH",
                errorCode = "UNAVAILABLE",
                canRetry = true
            )

            if (!snapshot.exists()) {
                return FirestoreMasterPinLookup.DocumentMissing
            }

            val rawMasterPin = (snapshot.getString(FIRESTORE_FIELD_MASTER_PIN)
                ?: snapshot.get(FIRESTORE_FIELD_MASTER_PIN)?.toString())
                ?.trim()
                .orEmpty()

            if (rawMasterPin.isEmpty()) {
                return FirestoreMasterPinLookup.FieldEmpty
            }

            FirestoreMasterPinLookup.Found(masterPin = rawMasterPin, sourceLabel = "Source.SERVER")
        } catch (e: FirebaseFirestoreException) {
            mapFirestoreException(e)
        } catch (e: Exception) {
            val fsCause = e.cause as? FirebaseFirestoreException
            if (fsCause != null) {
                mapFirestoreException(fsCause)
            } else {
                FirestoreMasterPinLookup.DatabaseError(
                    userMessage = "Koneksi internet bermasalah saat menghubungi database Firestore. Periksa jaringan lalu ketuk Coba Lagi.",
                    detail = e.localizedMessage ?: "Unknown Firestore error",
                    errorCode = "UNAVAILABLE",
                    canRetry = true
                )
            }
        }
    }

    private suspend fun verifyViaRemoteRestApi(
        context: Context,
        api: AdminAuthApi,
        trimmedPassword: String,
        safeNip: String,
        safeName: String,
        safeRole: String,
        actionTitle: String
    ): AdminVerificationResult {
        logDebug("Path verifikasi : REST API POST /api/verify-admin-pass")
        logDebug("PIN diketik pengguna : '$trimmedPassword'")

        val requestPayload = JSONObject().apply {
            put("officerNip", safeNip)
            put("officerName", safeName)
            put("officerRole", safeRole)
            put("actionTitle", actionTitle)
            put("password", trimmedPassword)
            put("requestedTtlMinutes", 10)
            put("timestamp", clockProvider())
            put("deviceModel", "${Build.MANUFACTURER} ${Build.MODEL}".trim())
        }.toString()

        return try {
            val response = withTimeoutOrNull(API_TIMEOUT_MS) {
                api.verifyAdminPassword(requestPayload.toRequestBody(JSON_MEDIA_TYPE))
            }

            if (response == null) {
                recordAuditLog(
                    context = context,
                    officerNip = safeNip,
                    officerName = safeName,
                    officerRole = safeRole,
                    actionCode = ACTION_DENIED,
                    actionTitle = actionTitle,
                    success = false,
                    detail = "Timeout menghubungi POST /api/verify-admin-pass"
                )
                return AdminVerificationResult.Denied(
                    message = "Koneksi internet bermasalah (timeout menghubungi server verifikasi). Ketuk Coba Lagi.",
                    canRetry = true,
                    errorCode = "UNAVAILABLE"
                )
            }

            val rawBody = if (response.isSuccessful) {
                response.body()?.string().orEmpty()
            } else {
                response.errorBody()?.string().orEmpty()
            }
            val json = try {
                if (rawBody.isNotBlank()) JSONObject(rawBody) else JSONObject()
            } catch (_: Exception) {
                JSONObject()
            }

            if (response.isSuccessful) {
                val authorized = json.optBoolean("authorized", false) ||
                    json.optBoolean("success", false) ||
                    json.optBoolean("valid", false)
                val tokenStr = json.optString("accessToken")
                    .ifBlank { json.optString("temporaryToken") }
                    .ifBlank { json.optString("token") }
                    .trim()

                if (authorized && tokenStr.isNotBlank()) {
                    val now = clockProvider()
                    val serverTtlSeconds = when {
                        json.has("expiresInSeconds") -> json.optLong("expiresInSeconds", DEFAULT_TOKEN_TTL_SECONDS)
                        json.has("expiresInMinutes") -> json.optLong("expiresInMinutes", 10L) * 60L
                        json.has("expiresAtMs") -> ((json.optLong("expiresAtMs", 0L) - now) / 1000L)
                        else -> DEFAULT_TOKEN_TTL_SECONDS
                    }
                    val clampedTtlSeconds = serverTtlSeconds.coerceIn(MIN_TOKEN_TTL_SECONDS, MAX_TOKEN_TTL_SECONDS)
                    val expiresAtMs = now + (clampedTtlSeconds * 1000L)

                    val issuedToken = AdminTemporaryAccessToken(
                        token = tokenStr,
                        issuedToNip = safeNip,
                        issuedToName = safeName,
                        scope = json.optString("scope").ifBlank { "KELOLA_PERSONIL_ADMIN" },
                        issuedAtMs = now,
                        expiresAtMs = expiresAtMs,
                        verificationSource = "POST /api/verify-admin-pass"
                    )
                    activeToken = issuedToken

                    val serverMessage = json.optString("message")
                        .ifBlank { "Otorisasi server berhasil. Token akses sementara berlaku ${clampedTtlSeconds / 60} menit." }

                    recordAuditLog(
                        context = context,
                        officerNip = safeNip,
                        officerName = safeName,
                        officerRole = safeRole,
                        actionCode = ACTION_GRANTED,
                        actionTitle = actionTitle,
                        success = true,
                        detail = "Verifikasi server berhasil • TTL ${clampedTtlSeconds / 60} mnt • Scope: ${issuedToken.scope}"
                    )

                    return AdminVerificationResult.Authorized(
                        accessToken = issuedToken,
                        message = serverMessage
                    )
                }
            }

            activeToken = null
            val denialReason = json.optString("message")
                .ifBlank { json.optString("error") }
                .ifBlank { "PIN salah atau ditolak server (HTTP ${response.code()})." }

            recordAuditLog(
                context = context,
                officerNip = safeNip,
                officerName = safeName,
                officerRole = safeRole,
                actionCode = ACTION_DENIED,
                actionTitle = actionTitle,
                success = false,
                detail = "Ditolak server (HTTP ${response.code()}): $denialReason"
            )

            AdminVerificationResult.Denied(
                message = denialReason,
                httpCode = response.code(),
                canRetry = response.code() >= 500
            )
        } catch (e: Exception) {
            Log.e(TAG, "Gagal verifikasi POST /api/verify-admin-pass: ${e.message}", e)
            recordAuditLog(
                context = context,
                officerNip = safeNip,
                officerName = safeName,
                officerRole = safeRole,
                actionCode = ACTION_DENIED,
                actionTitle = actionTitle,
                success = false,
                detail = "Kesalahan jaringan/server: ${e.localizedMessage ?: "unknown"}"
            )
            AdminVerificationResult.Denied(
                message = "Koneksi internet bermasalah saat menghubungi server: ${e.localizedMessage ?: "Periksa jaringan Anda."}",
                canRetry = true,
                errorCode = "UNAVAILABLE"
            )
        }
    }

    /**
     * Mencatat setiap percobaan akses menu admin (berhasil maupun gagal) ke:
     * 1. Database lokal Room (`personnel_audit_logs`)
     * 2. Database Cloud Firestore (`admin_access_audit_logs`) pada Named DB yang tepat
     * 3. Endpoint server opsional (`POST /api/admin-audit-log`)
     */
    suspend fun recordAuditLog(
        context: Context,
        officerNip: String,
        officerName: String,
        officerRole: String,
        actionCode: String,
        actionTitle: String,
        success: Boolean,
        detail: String
    ) = withContext(Dispatchers.IO) {
        val now = clockProvider()
        val safeNip = officerNip.trim().ifBlank { "UNKNOWN_NIP" }
        val safeName = officerName.trim().ifBlank { "Petugas Pusdalops" }
        val safeRole = officerRole.trim().ifBlank { "Petugas" }
        val statusLabel = if (success) "BERHASIL" else "GAGAL"
        val deviceSummary = "${Build.MANUFACTURER} ${Build.MODEL}".trim().ifBlank { "Android Mobile" }
        val auditDetail = "[$statusLabel] Menu: $actionTitle • $detail • Device: $deviceSummary"

        try {
            val dao = AppDatabase.getInstance(context).auditLogDao()
            dao.insertAuditLog(
                PersonnelAuditLogEntity(
                    nip = safeNip,
                    name = safeName,
                    role = safeRole,
                    shift = actionTitle,
                    action = actionCode,
                    timestamp = now,
                    deviceInfo = auditDetail
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "Gagal menyimpan audit log ke Room: ${e.message}")
        }

        if (!disableFirestoreForTesting) {
            try {
                val db = ensureFirestoreReady(context).getOrNull()
                if (db != null) {
                    val docData = hashMapOf(
                        "officerNip" to safeNip,
                        "officerName" to safeName,
                        "officerRole" to safeRole,
                        "actionCode" to actionCode,
                        "actionTitle" to actionTitle,
                        "success" to success,
                        "detail" to detail,
                        "timestamp" to now,
                        "deviceModel" to deviceSummary
                    )
                    db.collection(COLLECTION_ADMIN_AUDIT_LOGS).add(docData)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gagal menyinkronkan audit log ke Firestore: ${e.message}")
            }
        }

        val api = resolveApiClient()
        if (api != null) {
            try {
                val body = JSONObject().apply {
                    put("officerNip", safeNip)
                    put("officerName", safeName)
                    put("officerRole", safeRole)
                    put("actionCode", actionCode)
                    put("actionTitle", actionTitle)
                    put("success", success)
                    put("detail", detail)
                    put("timestamp", now)
                    put("deviceModel", deviceSummary)
                }.toString()
                withTimeoutOrNull(2_500L) {
                    api.recordAdminAuditLog(body.toRequestBody(JSON_MEDIA_TYPE))
                }
            } catch (_: Exception) {
                // Best-effort forward
            }
        }
    }

    private fun logDebug(message: String) {
        val formatted = "[AdminPinDebug] $message"
        println(formatted)
        try {
            Log.d(TAG, formatted)
        } catch (_: Exception) {
            // Safe on pure JVM tests
        }
    }

    private fun resolveApiClient(): AdminAuthApi? {
        overrideApiForTesting?.let { return it }
        val baseUrl = (overrideBaseUrlForTesting ?: BuildConfig.DOCUMENT_API_BASE_URL).trim()
        if (baseUrl.isBlank() || baseUrl.contains("YOUR_DOCUMENT_API_BASE_URL", ignoreCase = true)) {
            return null
        }
        if (!baseUrl.startsWith("https://", ignoreCase = true) && !baseUrl.startsWith("http://localhost", ignoreCase = true)) {
            return null
        }
        return NetworkClient.createAdminAuthApi(baseUrl, API_TIMEOUT_MS)
    }
}
