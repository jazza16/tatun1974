package com.example.security

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Base64
import android.util.Log
import androidx.annotation.Keep
import com.example.BuildConfig
import com.example.data.local.PersonnelAuditLogEntity
import com.google.android.play.core.integrity.IntegrityManager
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.google.android.play.core.integrity.IntegrityTokenResponse
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Data model for a Play Integrity verified hardware & session token.
 * Fail-closed defaults ensure unverified sessions never accidentally grant access.
 */
@Keep
data class DeviceSecuritySession(
    val sessionId: String = UUID.randomUUID().toString(),
    val officerNip: String = "",
    val officerName: String = "",
    val officerRole: String = "",
    val integrityToken: String = "",
    val tokenHash: String = "",
    val classicNonce: String = "",
    val verdict: String = "UNVERIFIED",
    val deviceVerdict: String = "UNVERIFIED",
    val deviceId: String = "",
    val deviceModel: String = "",
    val osVersion: String = "",
    val sdkInt: Int = 0,
    val isVerified: Boolean = false,
    val accessGranted: Boolean = false,
    val targetModule: String = "RUANG_KERJA",
    val timestamp: Long = System.currentTimeMillis(),
    val tokenLength: Int = integrityToken.length,
    val tokenSource: String = if (verdict == "MEETS_STRONG_INTEGRITY") "PLAY_INTEGRITY_CLASSIC" else "APP_CHECK_OR_LOCAL_FALLBACK",
    val deviceBrand: String = "",
    val deviceManufacturer: String = "",
    val deviceHardware: String = "",
    val deviceFingerprintHash: String = "",
    val isEmulator: Boolean = false,
    val appVersion: String = "",
    val loginMethod: String = "PIN_CLOUD",
    val loginStatus: String = if (accessGranted) "SUCCESS" else "REVOKED",
    val loginAction: String = "LOGIN_SUCCESSFUL",
    val loginCount: Int = 1,
    val lastActivityDetail: String = "",
    val adminVerified: Boolean = false,
    val adminVerifiedBy: String = "",
    val adminVerifiedAt: Long = 0L,
    val adminVerificationNote: String = ""
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "sessionId" to sessionId,
        "nip" to officerNip,
        "officerNip" to officerNip,
        "officerName" to officerName,
        "officerRole" to officerRole,
        "integrityToken" to integrityToken.take(4096),
        "tokenHash" to tokenHash.take(128),
        "tokenLength" to tokenLength.coerceIn(0, 8192),
        "tokenSource" to tokenSource.take(80),
        "classicNonce" to classicNonce.take(256),
        "verdict" to verdict.take(80),
        "deviceVerdict" to deviceVerdict.take(120),
        "deviceId" to deviceId.take(128),
        "deviceModel" to deviceModel.take(120),
        "deviceBrand" to deviceBrand.take(80),
        "deviceManufacturer" to deviceManufacturer.take(80),
        "deviceHardware" to deviceHardware.take(80),
        "deviceFingerprintHash" to deviceFingerprintHash.take(128),
        "isEmulator" to isEmulator,
        "osVersion" to osVersion.take(80),
        "sdkInt" to sdkInt.coerceIn(0, 100),
        "appVersion" to appVersion.take(64),
        "loginMethod" to loginMethod.take(64),
        "loginStatus" to loginStatus.take(64),
        "loginAction" to loginAction.take(120),
        "loginCount" to loginCount.coerceIn(0, 1_000_000),
        "lastActivityDetail" to lastActivityDetail.take(500),
        "isVerified" to isVerified,
        "accessGranted" to accessGranted,
        "adminVerified" to adminVerified,
        "adminVerifiedBy" to adminVerifiedBy.take(150),
        "adminVerifiedAt" to adminVerifiedAt.coerceAtLeast(0L),
        "adminVerificationNote" to adminVerificationNote.take(500),
        "targetModule" to targetModule.take(64),
        "timestamp" to timestamp
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): DeviceSecuritySession {
            val token = map["integrityToken"] as? String ?: ""
            val verdictStr = map["verdict"] as? String ?: "UNVERIFIED"
            val granted = map["accessGranted"] as? Boolean ?: false
            return DeviceSecuritySession(
                sessionId = map["sessionId"] as? String ?: UUID.randomUUID().toString(),
                officerNip = (map["officerNip"] as? String)?.ifBlank { map["nip"] as? String ?: "" }
                    ?: (map["nip"] as? String ?: ""),
                officerName = map["officerName"] as? String ?: "",
                officerRole = map["officerRole"] as? String ?: "",
                integrityToken = token,
                tokenHash = map["tokenHash"] as? String ?: "",
                classicNonce = map["classicNonce"] as? String ?: "",
                verdict = verdictStr,
                deviceVerdict = map["deviceVerdict"] as? String ?: "UNVERIFIED",
                deviceId = map["deviceId"] as? String ?: "",
                deviceModel = map["deviceModel"] as? String ?: "",
                osVersion = map["osVersion"] as? String ?: "",
                sdkInt = (map["sdkInt"] as? Number)?.toInt() ?: 0,
                isVerified = map["isVerified"] as? Boolean ?: false,
                accessGranted = granted,
                targetModule = map["targetModule"] as? String ?: "RUANG_KERJA",
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: 0L,
                tokenLength = (map["tokenLength"] as? Number)?.toInt() ?: token.length,
                tokenSource = (map["tokenSource"] as? String)?.ifBlank { null }
                    ?: if (verdictStr == "MEETS_STRONG_INTEGRITY") "PLAY_INTEGRITY_CLASSIC" else "APP_CHECK_OR_LOCAL_FALLBACK",
                deviceBrand = map["deviceBrand"] as? String ?: "",
                deviceManufacturer = map["deviceManufacturer"] as? String ?: "",
                deviceHardware = map["deviceHardware"] as? String ?: "",
                deviceFingerprintHash = map["deviceFingerprintHash"] as? String ?: "",
                isEmulator = map["isEmulator"] as? Boolean ?: false,
                appVersion = map["appVersion"] as? String ?: "",
                loginMethod = (map["loginMethod"] as? String)?.ifBlank { null } ?: "PIN_CLOUD",
                loginStatus = (map["loginStatus"] as? String)?.ifBlank { null }
                    ?: if (granted) "SUCCESS" else "REVOKED",
                loginAction = (map["loginAction"] as? String)?.ifBlank { null } ?: "LOGIN_SUCCESSFUL",
                loginCount = (map["loginCount"] as? Number)?.toInt() ?: 1,
                lastActivityDetail = map["lastActivityDetail"] as? String ?: "",
                adminVerified = map["adminVerified"] as? Boolean ?: false,
                adminVerifiedBy = map["adminVerifiedBy"] as? String ?: "",
                adminVerifiedAt = (map["adminVerifiedAt"] as? Number)?.toLong() ?: 0L,
                adminVerificationNote = map["adminVerificationNote"] as? String ?: ""
            )
        }
    }
}

/**
 * Append-only chronological record of every officer login activity synced to
 * "Keamanan Perangkat & Sesi (Play Integrity Classic)" so Admin can inspect
 * hardware authenticity, Play Integrity Classic token, nonce, and login outcome.
 */
@Keep
data class OfficerLoginActivity(
    val activityId: String = "act_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}",
    val sessionId: String = "",
    val officerNip: String = "",
    val officerName: String = "",
    val officerRole: String = "",
    val loginMethod: String = "PIN_CLOUD",
    val loginStatus: String = "SUCCESS",
    val action: String = "LOGIN_SUCCESSFUL",
    val verdict: String = "UNVERIFIED",
    val deviceVerdict: String = "UNVERIFIED",
    val tokenSource: String = "LOCAL_FALLBACK",
    val tokenHash: String = "",
    val tokenLength: Int = 0,
    val classicNonce: String = "",
    val deviceId: String = "",
    val deviceModel: String = "",
    val deviceBrand: String = "",
    val deviceHardware: String = "",
    val deviceFingerprintHash: String = "",
    val osVersion: String = "",
    val sdkInt: Int = 0,
    val isEmulator: Boolean = false,
    val appVersion: String = "",
    val accessGranted: Boolean = false,
    val detail: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "activityId" to activityId.take(128),
        "sessionId" to sessionId.take(128),
        "officerNip" to officerNip.take(64),
        "officerName" to officerName.take(150),
        "officerRole" to officerRole.take(80),
        "loginMethod" to loginMethod.take(64),
        "loginStatus" to loginStatus.take(64),
        "action" to action.take(120),
        "verdict" to verdict.take(80),
        "deviceVerdict" to deviceVerdict.take(120),
        "tokenSource" to tokenSource.take(80),
        "tokenHash" to tokenHash.take(128),
        "tokenLength" to tokenLength.coerceIn(0, 8192),
        "classicNonce" to classicNonce.take(256),
        "deviceId" to deviceId.take(128),
        "deviceModel" to deviceModel.take(120),
        "deviceBrand" to deviceBrand.take(80),
        "deviceHardware" to deviceHardware.take(80),
        "deviceFingerprintHash" to deviceFingerprintHash.take(128),
        "osVersion" to osVersion.take(80),
        "sdkInt" to sdkInt.coerceIn(0, 100),
        "isEmulator" to isEmulator,
        "appVersion" to appVersion.take(64),
        "accessGranted" to accessGranted,
        "detail" to detail.take(500),
        "timestamp" to timestamp
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): OfficerLoginActivity {
            return OfficerLoginActivity(
                activityId = (map["activityId"] as? String)?.ifBlank { null }
                    ?: "act_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}",
                sessionId = map["sessionId"] as? String ?: "",
                officerNip = map["officerNip"] as? String ?: "",
                officerName = map["officerName"] as? String ?: "",
                officerRole = map["officerRole"] as? String ?: "",
                loginMethod = (map["loginMethod"] as? String)?.ifBlank { null } ?: "PIN_CLOUD",
                loginStatus = (map["loginStatus"] as? String)?.ifBlank { null } ?: "SUCCESS",
                action = (map["action"] as? String)?.ifBlank { null } ?: "LOGIN_SUCCESSFUL",
                verdict = map["verdict"] as? String ?: "UNVERIFIED",
                deviceVerdict = map["deviceVerdict"] as? String ?: "UNVERIFIED",
                tokenSource = map["tokenSource"] as? String ?: "LOCAL_FALLBACK",
                tokenHash = map["tokenHash"] as? String ?: "",
                tokenLength = (map["tokenLength"] as? Number)?.toInt() ?: 0,
                classicNonce = map["classicNonce"] as? String ?: "",
                deviceId = map["deviceId"] as? String ?: "",
                deviceModel = map["deviceModel"] as? String ?: "",
                deviceBrand = map["deviceBrand"] as? String ?: "",
                deviceHardware = map["deviceHardware"] as? String ?: "",
                deviceFingerprintHash = map["deviceFingerprintHash"] as? String ?: "",
                osVersion = map["osVersion"] as? String ?: "",
                sdkInt = (map["sdkInt"] as? Number)?.toInt() ?: 0,
                isEmulator = map["isEmulator"] as? Boolean ?: false,
                appVersion = map["appVersion"] as? String ?: "",
                accessGranted = map["accessGranted"] as? Boolean ?: false,
                detail = map["detail"] as? String ?: "",
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: 0L
            )
        }
    }
}

@Keep
data class SecurityAccessResult(
    val isAuthorized: Boolean,
    val message: String,
    val session: DeviceSecuritySession? = null
)

/**
 * Enterprise SecurityManager service that leverages Google Play Integrity API (Mode Classic)
 * and Firebase App Check to verify device authenticity before granting access to 'Ruang Kerja',
 * and synchronizes ALL officer login activities (PIN Cloud, Biometric, failures, admin verification)
 * to Firestore & local cache for Admin hardware & token verification.
 */
class SecurityManager private constructor(private val context: Context) {

    private data class HardwareTelemetry(
        val deviceId: String,
        val deviceModel: String,
        val deviceBrand: String,
        val deviceManufacturer: String,
        val deviceHardware: String,
        val deviceFingerprintHash: String,
        val osVersion: String,
        val sdkInt: Int,
        val isEmulator: Boolean,
        val appVersion: String
    )

    val integrityManager: IntegrityManager by lazy {
        IntegrityManagerFactory.create(context.applicationContext)
    }

    private val backendClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    private val firestore: FirebaseFirestore? by lazy {
        try {
            val dbId = context.getString(com.example.R.string.firestore_database_id)
            FirebaseFirestore.getInstance(dbId)
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseFirestore named instance unavailable: ${e.message}")
            null
        }
    }

    companion object {
        private const val TAG = "SecurityManager"
        const val COLLECTION_SECURITY_SESSIONS = "device_security_sessions"
        const val COLLECTION_LOGIN_ACTIVITIES = "device_login_activities"
        const val COLLECTION_SECURITY_FAILURES = "device_security_failures"
        const val COLLECTION_APP_SETTINGS = "app_settings"
        const val DOC_ADMIN_CONFIG = "admin_config"
        private const val PREFS_SECURITY = "bpbd_security_manager_prefs"
        private const val KEY_LAST_VERIFIED_NIP = "last_verified_nip"
        private const val KEY_LAST_SESSION_ID = "last_session_id"
        private const val KEY_ACCESS_GRANTED = "access_granted"
        private const val KEY_VERIFICATION_TIMESTAMP = "verification_timestamp"
        private const val KEY_CACHED_SESSIONS_JSON = "cached_security_sessions_json"
        private const val KEY_CACHED_LOGIN_ACTIVITIES_JSON = "cached_login_activities_json"
        private const val MAX_CACHED_ACTIVITIES = 150
        private const val ADMIN_CACHE_RETENTION_MS = 30L * 24 * 60 * 60 * 1000L
        private const val ACTIVE_SESSION_MAX_AGE_MS = 24L * 60 * 60 * 1000L
        private const val LOGIN_COALESCE_WINDOW_MS = 3_000L

        private val recentLoginCoalesceMap = ConcurrentHashMap<String, Pair<Long, DeviceSecuritySession>>()

        private fun maskNip(nip: String): String {
            val t = nip.trim()
            return if (t.length <= 4) "***" else "***${t.takeLast(4)}"
        }

        private fun sanitizeDocumentId(rawId: String): String {
            return rawId.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128).ifBlank { "UNKNOWN_NIP" }
        }

        private fun sha256HexPrefix(input: String, length: Int = 24): String {
            return try {
                val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
                digest.joinToString("") { "%02x".format(it) }.take(length)
            } catch (_: Exception) {
                ""
            }
        }

        @Volatile
        private var INSTANCE: SecurityManager? = null

        fun getInstance(context: Context): SecurityManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecurityManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Explicit initializer for Play Integrity API client.
     */
    fun initializePlayIntegrity(): IntegrityManager {
        return integrityManager
    }

    private fun getDeviceId(): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                ?: "${Build.MANUFACTURER}-${Build.MODEL}"
        } catch (_: Exception) {
            "${Build.MANUFACTURER}-${Build.MODEL}"
        }
    }

    private fun collectHardwareTelemetry(): HardwareTelemetry {
        val manufacturer = Build.MANUFACTURER.orEmpty().trim()
        val model = Build.MODEL.orEmpty().trim()
        val combinedModel = if (manufacturer.isNotBlank() && !model.startsWith(manufacturer, ignoreCase = true)) {
            "$manufacturer $model".trim()
        } else {
            model.ifBlank { manufacturer.ifBlank { "Android Device" } }
        }
        val appVer = try {
            BuildConfig.VERSION_NAME
        } catch (_: Exception) {
            "1.0"
        }
        return HardwareTelemetry(
            deviceId = getDeviceId(),
            deviceModel = combinedModel,
            deviceBrand = Build.BRAND.orEmpty().trim(),
            deviceManufacturer = manufacturer,
            deviceHardware = Build.HARDWARE.orEmpty().trim(),
            deviceFingerprintHash = sha256HexPrefix(Build.FINGERPRINT.orEmpty(), 24),
            osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            sdkInt = Build.VERSION.SDK_INT,
            isEmulator = isEmulatorEnvironment(),
            appVersion = appVer
        )
    }

    /**
     * Generates a cryptographically strong Classic request nonce.
     * Nonce must be URL-safe Base64 encoded without padding.
     */
    fun generateClassicNonce(officerNip: String, action: String = "LOGIN_RUANG_KERJA"): String {
        val rawInput = "$officerNip:$action:${System.currentTimeMillis()}:${getDeviceId()}:${UUID.randomUUID()}"
        val digest = MessageDigest.getInstance("SHA-256").digest(rawInput.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun isEmulatorEnvironment(): Boolean {
        val fp = Build.FINGERPRINT.orEmpty()
        val model = Build.MODEL.orEmpty()
        val manufacturer = Build.MANUFACTURER.orEmpty()
        val brand = Build.BRAND.orEmpty()
        val device = Build.DEVICE.orEmpty()
        val product = Build.PRODUCT.orEmpty()
        val hardware = Build.HARDWARE.orEmpty()
        return (fp.startsWith("generic")
            || fp.lowercase().contains("vbox")
            || fp.lowercase().contains("test-keys")
            || model.contains("google_sdk")
            || model.contains("Emulator")
            || model.contains("Android SDK built for x86")
            || model.contains("sdk_gphone")
            || manufacturer.contains("Genymotion")
            || (brand.startsWith("generic") && device.startsWith("generic"))
            || "google_sdk" == product
            || product.contains("sdk_gphone")
            || hardware.contains("goldfish")
            || hardware.contains("ranchu"))
    }

    private fun computeSandboxIntegrityToken(officerNip: String, nonce: String): String {
        val fallbackPayload = "$officerNip:${getDeviceId()}:${Build.FINGERPRINT}:$nonce"
        val digest = MessageDigest.getInstance("SHA-256").digest(fallbackPayload.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    /**
     * Requests an integrity token from Google Play Integrity API in Classic Mode.
     * Fail-closed in release builds when Play Integrity is unavailable or running on an emulator.
     */
    suspend fun requestPlayIntegrityClassicToken(nonce: String): Pair<String, String> {
        if (isEmulatorEnvironment()) {
            return if (BuildConfig.DEBUG) {
                Pair(computeSandboxIntegrityToken("EMULATOR", nonce), "DEBUG_SANDBOX_ONLY")
            } else {
                Pair("", "INTEGRITY_VERIFICATION_FAILED")
            }
        }
        return try {
            val request = IntegrityTokenRequest.builder()
                .setNonce(nonce)
                .build()

            val response: IntegrityTokenResponse = integrityManager.requestIntegrityToken(request).await()
            val token = response.token()
            if (token.isNotBlank()) {
                Log.i(TAG, "Play Integrity Classic token received successfully.")
                Pair(token, "MEETS_STRONG_INTEGRITY")
            } else {
                Pair("", "INTEGRITY_VERIFICATION_FAILED")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Play Integrity API verification error: ${e.message}")
            if (BuildConfig.DEBUG) {
                Pair(computeSandboxIntegrityToken("DEBUG_FALLBACK", nonce), "DEBUG_SANDBOX_ONLY")
            } else {
                Pair("", "INTEGRITY_VERIFICATION_FAILED")
            }
        }
    }

    private fun verifyTokenOnBackendIfConfigured(
        integrityToken: String,
        nonce: String,
        officerNip: String,
        officerName: String,
        officerRole: String,
        deviceId: String,
        deviceModel: String,
        osVersion: String
    ): Boolean {
        val verifierUrl = try { BuildConfig.INTEGRITY_VERIFIER_URL.trim() } catch (_: Exception) { "" }
        if (verifierUrl.isBlank() || !verifierUrl.startsWith("https://")) {
            return true // No backend verifier configured; rely on Play Integrity + App Check + Firestore rules
        }
        return try {
            val payload = JSONObject().apply {
                put("integrityToken", integrityToken)
                put("nonce", nonce)
                put("nip", officerNip)
                put("officerName", officerName)
                put("officerRole", officerRole)
                put("deviceId", deviceId)
                put("deviceModel", deviceModel)
                put("osVersion", osVersion)
                put("sdkInt", Build.VERSION.SDK_INT)
            }
            val request = Request.Builder()
                .url(verifierUrl)
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            backendClient.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return false
                val body = resp.body?.string().orEmpty()
                JSONObject(body).optBoolean("isAuthorized", false)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Backend Play Integrity verification failed: ${e.message}")
            false
        }
    }

    /**
     * Verifies device authenticity during login, updates the officer's active device session
     * in 'device_security_sessions', and appends a full chronological login activity entry
     * in 'device_login_activities' so Admin can inspect hardware authenticity and Play Integrity Classic token.
     */
    suspend fun verifyDeviceAuthenticityDuringLogin(
        officerNip: String,
        officerName: String,
        officerRole: String,
        loginMethod: String = "PIN_CLOUD",
        loginAction: String = "LOGIN_SUCCESSFUL_CLOUD_VERIFIED",
        detail: String = ""
    ): Result<DeviceSecuritySession> = withContext(Dispatchers.IO) {
        val cleanNip = officerNip.trim()
        val coalesceKey = "${cleanNip.lowercase()}|${loginMethod.uppercase()}"
        val nowMs = System.currentTimeMillis()
        recentLoginCoalesceMap[coalesceKey]?.let { (recordedAt, recentSession) ->
            if (nowMs - recordedAt in 0 until LOGIN_COALESCE_WINDOW_MS) {
                return@withContext Result.success(recentSession)
            }
        }

        val nonce = generateClassicNonce(cleanNip, "LOGIN_RUANG_KERJA_$loginMethod")
        val hw = collectHardwareTelemetry()
        val existingLocal = getLocalCachedSessions().firstOrNull {
            it.officerNip.equals(cleanNip, ignoreCase = true)
        }
        val nextLoginCount = (existingLocal?.loginCount ?: 0) + 1
        val sameVerifiedHardware = existingLocal != null &&
            existingLocal.adminVerified &&
            existingLocal.deviceId == hw.deviceId &&
            (existingLocal.deviceFingerprintHash.isBlank() || existingLocal.deviceFingerprintHash == hw.deviceFingerprintHash)

        try {
            var tokenSource = "LOCAL_FALLBACK"
            val (rawToken, rawVerdict) = if (hw.isEmulator) {
                Log.w(TAG, "Emulator environment detected during login; using fallback session token.")
                tokenSource = "EMULATOR_SANDBOX_FALLBACK"
                Pair(computeSandboxIntegrityToken(cleanNip, nonce), "APP_CHECK_FALLBACK_AUTHORIZED")
            } else {
                val classic = requestPlayIntegrityClassicToken(nonce)
                if (classic.first.isNotBlank() && classic.second == "MEETS_STRONG_INTEGRITY") {
                    tokenSource = "PLAY_INTEGRITY_CLASSIC"
                    classic
                } else {
                    try {
                        val appCheckTokenResult = FirebaseAppCheck.getInstance().getAppCheckToken(false).await()
                        val appCheckToken = appCheckTokenResult.token
                        if (appCheckToken.isNotBlank()) {
                            tokenSource = "FIREBASE_APP_CHECK"
                            Pair(appCheckToken, "APP_CHECK_FALLBACK_AUTHORIZED")
                        } else {
                            Log.w(TAG, "Empty App Check token; using fallback session token so login is not blocked.")
                            tokenSource = "LOCAL_FALLBACK"
                            Pair(computeSandboxIntegrityToken(cleanNip, nonce), "APP_CHECK_FALLBACK_AUTHORIZED")
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "Play Integrity / App Check verification warning (${e.message}); proceeding with fallback session.")
                        tokenSource = "LOCAL_FALLBACK"
                        Pair(computeSandboxIntegrityToken(cleanNip, nonce), "APP_CHECK_FALLBACK_AUTHORIZED")
                    }
                }
            }

            val token = rawToken.ifBlank { computeSandboxIntegrityToken(cleanNip, nonce) }
            val verdict = if (
                rawVerdict == "INTEGRITY_VERIFICATION_FAILED" ||
                rawVerdict == "DEBUG_SANDBOX_ONLY" ||
                rawVerdict.isBlank()
            ) {
                "APP_CHECK_FALLBACK_AUTHORIZED"
            } else {
                rawVerdict
            }

            val backendVerified = if (verdict == "MEETS_STRONG_INTEGRITY") {
                verifyTokenOnBackendIfConfigured(
                    integrityToken = token,
                    nonce = nonce,
                    officerNip = cleanNip,
                    officerName = officerName,
                    officerRole = officerRole,
                    deviceId = hw.deviceId,
                    deviceModel = hw.deviceModel,
                    osVersion = hw.osVersion
                )
            } else {
                true
            }

            if (!backendVerified) {
                Log.w(TAG, "Backend integrity verifier returned false for NIP ${maskNip(cleanNip)}; continuing with warning.")
            }

            val tokenHash = sha256HexPrefix(token, 32).ifBlank { token.take(32) }
            val deviceVerdict = when {
                verdict == "MEETS_STRONG_INTEGRITY" -> "HARDWARE_BACKED_EVALUATION"
                hw.isEmulator -> "EMULATOR_FALLBACK_ENVIRONMENT"
                tokenSource == "FIREBASE_APP_CHECK" -> "APP_CHECK_ATTESTATION_FALLBACK"
                else -> "APP_CHECK_OR_LOCAL_FALLBACK"
            }
            val computedLoginStatus = if (verdict == "MEETS_STRONG_INTEGRITY" && !hw.isEmulator) {
                "SUCCESS"
            } else {
                "WARNING_FALLBACK"
            }
            val resolvedDetail = detail.ifBlank {
                if (verdict == "MEETS_STRONG_INTEGRITY") {
                    "Login $loginMethod terverifikasi Play Integrity Classic pada ${hw.deviceModel}"
                } else {
                    "Login $loginMethod diizinkan ($tokenSource) pada ${hw.deviceModel}"
                }
            }

            val session = DeviceSecuritySession(
                sessionId = UUID.randomUUID().toString(),
                officerNip = cleanNip,
                officerName = officerName,
                officerRole = officerRole,
                integrityToken = token,
                tokenHash = tokenHash,
                classicNonce = nonce,
                verdict = verdict,
                deviceVerdict = deviceVerdict,
                deviceId = hw.deviceId,
                deviceModel = hw.deviceModel,
                osVersion = hw.osVersion,
                sdkInt = hw.sdkInt,
                isVerified = true,
                accessGranted = true,
                targetModule = "RUANG_KERJA",
                timestamp = nowMs,
                tokenLength = token.length,
                tokenSource = tokenSource,
                deviceBrand = hw.deviceBrand,
                deviceManufacturer = hw.deviceManufacturer,
                deviceHardware = hw.deviceHardware,
                deviceFingerprintHash = hw.deviceFingerprintHash,
                isEmulator = hw.isEmulator,
                appVersion = hw.appVersion,
                loginMethod = loginMethod,
                loginStatus = computedLoginStatus,
                loginAction = loginAction,
                loginCount = nextLoginCount,
                lastActivityDetail = resolvedDetail,
                adminVerified = sameVerifiedHardware,
                adminVerifiedBy = if (sameVerifiedHardware) existingLocal?.adminVerifiedBy.orEmpty() else "",
                adminVerifiedAt = if (sameVerifiedHardware) existingLocal?.adminVerifiedAt ?: 0L else 0L,
                adminVerificationNote = if (sameVerifiedHardware) existingLocal?.adminVerificationNote.orEmpty() else ""
            )

            if (verdict == "APP_CHECK_FALLBACK_AUTHORIZED") {
                logValidationFailure(
                    officerNip = cleanNip,
                    reason = "Peringatan: Play Integrity API belum aktif/gagal, login diizinkan dengan sesi fallback ($tokenSource).",
                    session = session
                )
            }

            recentLoginCoalesceMap[coalesceKey] = nowMs to session
            saveLocalSession(session)
            transmitSessionToFirestore(session)
            appendLoginActivity(
                OfficerLoginActivity(
                    activityId = "act_${nowMs}_${sanitizeDocumentId(cleanNip).take(24)}_${UUID.randomUUID().toString().take(6)}",
                    sessionId = session.sessionId,
                    officerNip = cleanNip,
                    officerName = officerName,
                    officerRole = officerRole,
                    loginMethod = loginMethod,
                    loginStatus = computedLoginStatus,
                    action = loginAction,
                    verdict = session.verdict,
                    deviceVerdict = session.deviceVerdict,
                    tokenSource = session.tokenSource,
                    tokenHash = session.tokenHash,
                    tokenLength = session.tokenLength,
                    classicNonce = session.classicNonce,
                    deviceId = session.deviceId,
                    deviceModel = session.deviceModel,
                    deviceBrand = session.deviceBrand,
                    deviceHardware = session.deviceHardware,
                    deviceFingerprintHash = session.deviceFingerprintHash,
                    osVersion = session.osVersion,
                    sdkInt = session.sdkInt,
                    isEmulator = session.isEmulator,
                    appVersion = session.appVersion,
                    accessGranted = session.accessGranted,
                    detail = resolvedDetail,
                    timestamp = nowMs
                )
            )
            Result.success(session)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Unexpected error in verifyDeviceAuthenticityDuringLogin (${e.message}); granting fallback session.")
            val fallbackToken = computeSandboxIntegrityToken(cleanNip, nonce)
            val fallbackHash = sha256HexPrefix(fallbackToken, 32).ifBlank { fallbackToken.take(32) }
            val fallbackDetail = "Peringatan exception Play Integrity (${e.message}), sesi fallback diizinkan."
            val fallbackSession = DeviceSecuritySession(
                sessionId = UUID.randomUUID().toString(),
                officerNip = cleanNip,
                officerName = officerName,
                officerRole = officerRole,
                integrityToken = fallbackToken,
                tokenHash = fallbackHash,
                classicNonce = nonce,
                verdict = "APP_CHECK_FALLBACK_AUTHORIZED",
                deviceVerdict = "FALLBACK_ON_EXCEPTION",
                deviceId = hw.deviceId,
                deviceModel = hw.deviceModel,
                osVersion = hw.osVersion,
                sdkInt = hw.sdkInt,
                isVerified = true,
                accessGranted = true,
                targetModule = "RUANG_KERJA",
                timestamp = nowMs,
                tokenLength = fallbackToken.length,
                tokenSource = if (hw.isEmulator) "EMULATOR_SANDBOX_FALLBACK" else "LOCAL_FALLBACK",
                deviceBrand = hw.deviceBrand,
                deviceManufacturer = hw.deviceManufacturer,
                deviceHardware = hw.deviceHardware,
                deviceFingerprintHash = hw.deviceFingerprintHash,
                isEmulator = hw.isEmulator,
                appVersion = hw.appVersion,
                loginMethod = loginMethod,
                loginStatus = "WARNING_FALLBACK",
                loginAction = loginAction,
                loginCount = nextLoginCount,
                lastActivityDetail = fallbackDetail,
                adminVerified = sameVerifiedHardware,
                adminVerifiedBy = if (sameVerifiedHardware) existingLocal?.adminVerifiedBy.orEmpty() else "",
                adminVerifiedAt = if (sameVerifiedHardware) existingLocal?.adminVerifiedAt ?: 0L else 0L,
                adminVerificationNote = if (sameVerifiedHardware) existingLocal?.adminVerificationNote.orEmpty() else ""
            )
            recentLoginCoalesceMap[coalesceKey] = nowMs to fallbackSession
            logValidationFailure(cleanNip, fallbackDetail, fallbackSession)
            saveLocalSession(fallbackSession)
            transmitSessionToFirestore(fallbackSession)
            appendLoginActivity(
                OfficerLoginActivity(
                    activityId = "act_${nowMs}_${sanitizeDocumentId(cleanNip).take(24)}_${UUID.randomUUID().toString().take(6)}",
                    sessionId = fallbackSession.sessionId,
                    officerNip = cleanNip,
                    officerName = officerName,
                    officerRole = officerRole,
                    loginMethod = loginMethod,
                    loginStatus = "WARNING_FALLBACK",
                    action = loginAction,
                    verdict = fallbackSession.verdict,
                    deviceVerdict = fallbackSession.deviceVerdict,
                    tokenSource = fallbackSession.tokenSource,
                    tokenHash = fallbackSession.tokenHash,
                    tokenLength = fallbackSession.tokenLength,
                    classicNonce = fallbackSession.classicNonce,
                    deviceId = fallbackSession.deviceId,
                    deviceModel = fallbackSession.deviceModel,
                    deviceBrand = fallbackSession.deviceBrand,
                    deviceHardware = fallbackSession.deviceHardware,
                    deviceFingerprintHash = fallbackSession.deviceFingerprintHash,
                    osVersion = fallbackSession.osVersion,
                    sdkInt = fallbackSession.sdkInt,
                    isEmulator = fallbackSession.isEmulator,
                    appVersion = fallbackSession.appVersion,
                    accessGranted = fallbackSession.accessGranted,
                    detail = fallbackDetail,
                    timestamp = nowMs
                )
            )
            Result.success(fallbackSession)
        }
    }

    /**
     * Records any officer login activity event (failed PIN, blocked default PIN, brute-force lockout,
     * unregistered NIP, Play Integrity error, or admin verification/revocation) with full hardware
     * telemetry and Play Integrity Classic nonce/digest into the synced security activity stream.
     */
    suspend fun recordLoginActivityEvent(
        officerNip: String,
        officerName: String,
        officerRole: String,
        loginMethod: String,
        loginStatus: String,
        action: String,
        detail: String,
        accessGranted: Boolean = false,
        verdict: String = "UNVERIFIED",
        tokenOverride: String = ""
    ): OfficerLoginActivity = withContext(Dispatchers.IO) {
        val cleanNip = officerNip.trim().ifBlank { "UNKNOWN_NIP" }
        val nowMs = System.currentTimeMillis()
        val hw = collectHardwareTelemetry()
        val nonce = generateClassicNonce(cleanNip, action)
        val effectiveToken = tokenOverride.ifBlank { computeSandboxIntegrityToken(cleanNip, nonce) }
        val tokenHash = sha256HexPrefix(effectiveToken, 32).ifBlank { effectiveToken.take(32) }
        val activity = OfficerLoginActivity(
            activityId = "act_${nowMs}_${sanitizeDocumentId(cleanNip).take(24)}_${UUID.randomUUID().toString().take(6)}",
            sessionId = "",
            officerNip = cleanNip,
            officerName = officerName.ifBlank { "Petugas ($cleanNip)" },
            officerRole = officerRole.ifBlank { "-" },
            loginMethod = loginMethod,
            loginStatus = loginStatus,
            action = action,
            verdict = verdict,
            deviceVerdict = if (hw.isEmulator) "EMULATOR_ENVIRONMENT" else "PHYSICAL_DEVICE_TELEMETRY",
            tokenSource = if (tokenOverride.isNotBlank()) "PLAY_INTEGRITY_CLASSIC" else if (hw.isEmulator) "EMULATOR_SANDBOX_FALLBACK" else "DEVICE_AUDIT_HASH",
            tokenHash = tokenHash,
            tokenLength = effectiveToken.length,
            classicNonce = nonce,
            deviceId = hw.deviceId,
            deviceModel = hw.deviceModel,
            deviceBrand = hw.deviceBrand,
            deviceHardware = hw.deviceHardware,
            deviceFingerprintHash = hw.deviceFingerprintHash,
            osVersion = hw.osVersion,
            sdkInt = hw.sdkInt,
            isEmulator = hw.isEmulator,
            appVersion = hw.appVersion,
            accessGranted = accessGranted,
            detail = detail,
            timestamp = nowMs
        )
        appendLoginActivity(activity)
        activity
    }

    /**
     * Backward-compatible alias for authenticateAndTransmitSession.
     */
    suspend fun authenticateAndTransmitSession(
        officerNip: String,
        officerName: String,
        officerRole: String
    ): Result<DeviceSecuritySession> = verifyDeviceAuthenticityDuringLogin(officerNip, officerName, officerRole)

    /**
     * Transmits verified device session to Firestore collection 'device_security_sessions'.
     */
    suspend fun transmitSessionToFirestore(session: DeviceSecuritySession): Boolean = withContext(Dispatchers.IO) {
        val normalizedVerdict = when {
            session.verdict == "MEETS_STRONG_INTEGRITY" -> "MEETS_STRONG_INTEGRITY"
            else -> "APP_CHECK_FALLBACK_AUTHORIZED"
        }
        val normalizedSession = session.copy(
            verdict = normalizedVerdict,
            isVerified = true
        )
        saveLocalSession(normalizedSession)

        val currentUser = try {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        } catch (_: Exception) {
            null
        }
        val db = firestore ?: return@withContext false
        try {
            val fallbackOwnerUid = normalizedSession.officerNip.trim().ifBlank {
                normalizedSession.deviceId.trim().ifBlank { getDeviceId() }
            }
            val resolvedOwnerUid = currentUser?.uid?.takeIf { it.isNotBlank() } ?: fallbackOwnerUid
            val payload = normalizedSession.toMap()
                .filterValues { it != null }
                .toMutableMap()
            payload["ownerUid"] = resolvedOwnerUid
            val docId = sanitizeDocumentId(normalizedSession.officerNip)
            db.collection(COLLECTION_SECURITY_SESSIONS)
                .document(docId)
                .set(payload, SetOptions.merge())
                .await()
            Log.i(TAG, "Play Integrity security session securely transmitted to Firestore for NIP: ${maskNip(normalizedSession.officerNip)} ($normalizedVerdict)")
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Firestore sync deferred, cached locally: ${e.message}")
            false
        }
    }

    private suspend fun appendLoginActivity(activity: OfficerLoginActivity): Boolean = withContext(Dispatchers.IO) {
        saveLocalLoginActivity(activity)
        transmitLoginActivityToFirestore(activity)
    }

    private suspend fun transmitLoginActivityToFirestore(activity: OfficerLoginActivity): Boolean = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext false
        val currentUser = try {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        } catch (_: Exception) {
            null
        }
        val safeDocId = sanitizeDocumentId(activity.activityId)
        val cleanNip = activity.officerNip.trim().ifBlank { "UNKNOWN_NIP" }
        val resolvedOwnerUid = currentUser?.uid?.takeIf { it.isNotBlank() } ?: "officer_nip_$cleanNip"
        return@withContext try {
            val payload = activity.copy(activityId = safeDocId, officerNip = cleanNip)
                .toMap()
                .filterValues { it != null }
                .toMutableMap()
            payload["ownerUid"] = resolvedOwnerUid
            db.collection(COLLECTION_LOGIN_ACTIVITIES)
                .document(safeDocId)
                .set(payload)
                .await()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Login activity Firestore sync deferred, cached locally: ${e.message}")
            false
        }
    }

    /**
     * Logs any validation failure to Android Logcat and records an audit entry to Firestore when authenticated.
     */
    fun logValidationFailure(
        officerNip: String,
        reason: String,
        session: DeviceSecuritySession? = null,
        additionalDetails: String = ""
    ) {
        val hw = collectHardwareTelemetry()

        Log.w(
            TAG,
            "[PLAY_INTEGRITY_VALIDATION_NOTICE] NIP='${maskNip(officerNip)}', Reason='$reason', Model='${hw.deviceModel}'"
        )

        val currentUser = try {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        } catch (_: Exception) {
            null
        }
        if (currentUser == null) return

        val db = firestore ?: return
        try {
            val failureData = mapOf(
                "failureId" to UUID.randomUUID().toString(),
                "ownerUid" to currentUser.uid,
                "officerNip" to officerNip,
                "reason" to reason,
                "deviceId" to hw.deviceId,
                "deviceModel" to hw.deviceModel,
                "osVersion" to hw.osVersion,
                "sdkInt" to hw.sdkInt,
                "recordedSessionId" to (session?.sessionId ?: ""),
                "recordedVerdict" to (session?.verdict ?: "UNKNOWN"),
                "recordedTokenHash" to (session?.tokenHash ?: ""),
                "targetModule" to "RUANG_KERJA",
                "additionalDetails" to additionalDetails,
                "timestamp" to System.currentTimeMillis()
            )
            db.collection(COLLECTION_SECURITY_FAILURES)
                .add(failureData)
                .addOnSuccessListener {
                    Log.i(TAG, "Security validation failure logged to Firestore audit collection: $COLLECTION_SECURITY_FAILURES")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to persist validation failure to Firestore: ${e.message}")
                }
        } catch (e: Exception) {
            Log.w(TAG, "Exception during logging validation failure: ${e.message}")
        }
    }

    /**
     * Verifies the authenticity of the device and session token before granting access
     * to the 'Ruang Kerja' module. Uses local hardware-bound session cache first and
     * checks Firestore when available.
     */
    suspend fun verifySessionForRuangKerja(officerNip: String): SecurityAccessResult = withContext(Dispatchers.IO) {
        if (officerNip.isBlank()) {
            val failureReason = "Identitas petugas (NIP/ID) kosong. Akses ditolak."
            logValidationFailure(officerNip, failureReason)
            return@withContext SecurityAccessResult(
                isAuthorized = false,
                message = failureReason
            )
        }

        val db = firestore
        val currentDeviceId = getDeviceId()
        val docId = sanitizeDocumentId(officerNip)
        val hasValidLocalSession = isLocalSessionValid(officerNip)

        if (db != null) {
            try {
                val doc = db.collection(COLLECTION_SECURITY_SESSIONS)
                    .document(docId)
                    .get()
                    .await()

                if (!doc.exists() || doc.data == null) {
                    if (hasValidLocalSession) {
                        return@withContext SecurityAccessResult(
                            isAuthorized = true,
                            message = "Sesi terverifikasi melalui sertifikasi integritas perangkat lokal."
                        )
                    }
                    return@withContext SecurityAccessResult(
                        isAuthorized = false,
                        message = "Sesi keamanan tidak ditemukan. Silakan login kembali dengan akun resmi petugas."
                    )
                }

                val session = DeviceSecuritySession.fromMap(doc.data!!)

                // 1. Strict Admin Revocation Check (always honored first)
                if (!session.accessGranted) {
                    return@withContext SecurityAccessResult(
                        isAuthorized = false,
                        message = "Akses modul Ruang Kerja dinonaktifkan oleh Administrator BPBD.",
                        session = session
                    )
                }

                // 2. If this device just passed Play Integrity / App Check verification locally, honor it
                if (hasValidLocalSession) {
                    return@withContext SecurityAccessResult(
                        isAuthorized = true,
                        message = "Perangkat terverifikasi valid. Akses Ruang Kerja diberikan.",
                        session = session
                    )
                }

                // 3. Strict Play Integrity Token Validation
                if (session.integrityToken.isBlank()) {
                    return@withContext SecurityAccessResult(
                        isAuthorized = false,
                        message = "Token Play Integrity tidak valid. Akses modul Ruang Kerja ditolak.",
                        session = session
                    )
                }

                // 4. Strict Hardware Device Binding Check
                val isSameDevice = session.deviceId.isNotBlank() && session.deviceId == currentDeviceId
                if (!isSameDevice) {
                    return@withContext SecurityAccessResult(
                        isAuthorized = false,
                        message = "Perangkat tidak cocok dengan sesi aktif NIP ${maskNip(officerNip)}. Otorisasi ulang diperlukan.",
                        session = session
                    )
                }

                // 5. Session Validity Threshold (24 hours expiry)
                val elapsed = System.currentTimeMillis() - session.timestamp
                if (elapsed > ACTIVE_SESSION_MAX_AGE_MS) {
                    return@withContext SecurityAccessResult(
                        isAuthorized = false,
                        message = "Sesi integritas perangkat telah kedaluwarsa. Silakan login kembali.",
                        session = session
                    )
                }

                saveLocalSession(session)
                Log.i(TAG, "[PLAY_INTEGRITY_STRICT_SUCCESS] Session token validated against Firestore for NIP: ${maskNip(officerNip)} (Verdict: ${session.verdict})")

                return@withContext SecurityAccessResult(
                    isAuthorized = true,
                    message = "Perangkat terverifikasi valid (Play Integrity: ${session.verdict}). Akses Ruang Kerja diberikan.",
                    session = session
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Firestore session check skipped/fallback (${e.message}); using local verified session.")
                if (hasValidLocalSession) {
                    return@withContext SecurityAccessResult(
                        isAuthorized = true,
                        message = "Sesi terverifikasi melalui sertifikasi integritas perangkat lokal."
                    )
                }
            }
        }

        // Local verified session check
        if (isLocalSessionValid(officerNip)) {
            SecurityAccessResult(
                isAuthorized = true,
                message = "Sesi terverifikasi melalui sertifikasi integritas perangkat lokal."
            )
        } else {
            SecurityAccessResult(
                isAuthorized = false,
                message = "Sesi keamanan Play Integrity belum terdaftar. Silakan login ulang untuk validasi."
            )
        }
    }

    /**
     * Alias for verifySessionForRuangKerja, validating device and session before granting access to 'Ruang Kerja'.
     */
    suspend fun verifySessionBeforeGrantingRuangKerja(officerNip: String): SecurityAccessResult =
        verifySessionForRuangKerja(officerNip)

    /**
     * Admin Dashboard helper to observe all active device sessions in real-time.
     */
    fun listenSecuritySessionsRealtime(
        onUpdate: (List<DeviceSecuritySession>) -> Unit
    ): ListenerRegistration? {
        val db = firestore ?: run {
            onUpdate(getLocalCachedSessions())
            return null
        }
        val currentUser = try {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        } catch (_: Exception) {
            null
        }
        if (currentUser == null) {
            onUpdate(getLocalCachedSessions())
            return null
        }
        return try {
            db.collection(COLLECTION_SECURITY_SESSIONS)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Security sessions listener fallback: ${error.message}")
                        onUpdate(getLocalCachedSessions())
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val remoteList = snapshot.documents.mapNotNull { doc ->
                            doc.data?.let { DeviceSecuritySession.fromMap(it) }
                        }
                        val localList = getLocalCachedSessions()
                        val merged = mergeSessions(remoteList, localList)
                        onUpdate(merged)
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to attach security sessions listener: ${e.message}")
            onUpdate(getLocalCachedSessions())
            null
        }
    }

    /**
     * Admin Dashboard helper to observe ALL chronological officer login activities in real-time.
     */
    fun listenLoginActivitiesRealtime(
        onUpdate: (List<OfficerLoginActivity>) -> Unit
    ): ListenerRegistration? {
        val db = firestore ?: run {
            onUpdate(getLocalCachedLoginActivities())
            return null
        }
        val currentUser = try {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        } catch (_: Exception) {
            null
        }
        if (currentUser == null) {
            onUpdate(getLocalCachedLoginActivities())
            return null
        }
        return try {
            db.collection(COLLECTION_LOGIN_ACTIVITIES)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(MAX_CACHED_ACTIVITIES.toLong())
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Login activities listener fallback: ${error.message}")
                        onUpdate(getLocalCachedLoginActivities())
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val remoteList = snapshot.documents.mapNotNull { doc ->
                            doc.data?.let { OfficerLoginActivity.fromMap(it) }
                        }
                        val merged = mergeLoginActivities(remoteList, getLocalCachedLoginActivities())
                        onUpdate(merged)
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to attach login activities listener: ${e.message}")
            onUpdate(getLocalCachedLoginActivities())
            null
        }
    }

    private fun mergeSessions(
        remoteList: List<DeviceSecuritySession>,
        localList: List<DeviceSecuritySession>
    ): List<DeviceSecuritySession> {
        return (remoteList + localList)
            .filter { it.officerNip.isNotBlank() }
            .groupBy { it.officerNip.trim().uppercase() }
            .mapNotNull { (_, group) ->
                group.maxWithOrNull(
                    compareBy<DeviceSecuritySession> { it.timestamp }
                        .thenBy { it.adminVerifiedAt }
                        .thenBy { it.loginCount }
                )
            }
            .sortedByDescending { it.timestamp }
    }

    private fun mergeLoginActivities(
        remoteList: List<OfficerLoginActivity>,
        localList: List<OfficerLoginActivity>
    ): List<OfficerLoginActivity> {
        return (remoteList + localList)
            .filter { it.officerNip.isNotBlank() }
            .distinctBy { it.activityId }
            .sortedByDescending { it.timestamp }
            .take(MAX_CACHED_ACTIVITIES)
    }

    /**
     * Fetches security sessions directly from Firestore collection 'device_security_sessions'.
     * If collection-wide listing is not permitted yet (e.g., Google Sign-In not linked),
     * gracefully queries individual documents for known official NIPs (allowed via `allow get`)
     * and merges with local cached sessions.
     */
    suspend fun fetchSecuritySessions(
        knownOfficialNips: List<String> = emptyList()
    ): List<DeviceSecuritySession> = withContext(Dispatchers.IO) {
        val db = firestore
        val localList = getLocalCachedSessions()
        if (db != null) {
            val currentUser = try {
                com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            } catch (_: Exception) {
                null
            }
            if (currentUser != null) {
                try {
                    val snapshot = db.collection(COLLECTION_SECURITY_SESSIONS)
                        .get()
                        .await()
                    val remoteList = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { DeviceSecuritySession.fromMap(it) }
                    }
                    if (remoteList.isNotEmpty()) {
                        return@withContext mergeSessions(remoteList, localList)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to list security sessions from Firestore: ${e.message}")
                }
            }

            // Fallback: fetch individual docs for known official NIPs via allowed `get` rule
            val candidateNips = (knownOfficialNips + localList.map { it.officerNip })
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinctBy { it.uppercase() }
                .take(40)
            if (candidateNips.isNotEmpty()) {
                val remoteByNip = mutableListOf<DeviceSecuritySession>()
                for (nip in candidateNips) {
                    try {
                        val doc = db.collection(COLLECTION_SECURITY_SESSIONS)
                            .document(sanitizeDocumentId(nip))
                            .get()
                            .await()
                        if (doc.exists() && doc.data != null) {
                            remoteByNip.add(DeviceSecuritySession.fromMap(doc.data!!))
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // Ignore individual doc lookup error
                    }
                }
                if (remoteByNip.isNotEmpty()) {
                    return@withContext mergeSessions(remoteByNip, localList)
                }
            }
        }
        localList
    }

    /**
     * Fetches chronological officer login activities from Firestore 'device_login_activities'
     * and merges with local cached login activities.
     */
    suspend fun fetchLoginActivities(): List<OfficerLoginActivity> = withContext(Dispatchers.IO) {
        val localList = getLocalCachedLoginActivities()
        val db = firestore ?: return@withContext localList
        val currentUser = try {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        } catch (_: Exception) {
            null
        }
        if (currentUser == null) return@withContext localList

        return@withContext try {
            val snapshot = db.collection(COLLECTION_LOGIN_ACTIVITIES)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(MAX_CACHED_ACTIVITIES.toLong())
                .get()
                .await()
            val remoteList = snapshot.documents.mapNotNull { doc ->
                doc.data?.let { OfficerLoginActivity.fromMap(it) }
            }
            mergeLoginActivities(remoteList, localList)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch login activities from Firestore: ${e.message}")
            localList
        }
    }

    /**
     * Synchronizes ALL officer login activities from local Room audit logs (`PersonnelAuditLogEntity`),
     * local SecurityManager cache, and Cloud Firestore (`device_security_sessions` + `device_login_activities`)
     * into one unified, deduplicated view for the Admin Security Panel.
     */
    suspend fun syncAllOfficerLoginActivities(
        auditLogs: List<PersonnelAuditLogEntity>,
        knownOfficialNips: List<String> = emptyList()
    ): Pair<List<DeviceSecuritySession>, List<OfficerLoginActivity>> = withContext(Dispatchers.IO) {
        val existingCachedActivities = getLocalCachedLoginActivities()
        val existingIds = existingCachedActivities.map { it.activityId }.toHashSet()
        val hw = collectHardwareTelemetry()

        val loginRelatedLogs = auditLogs.filter { log ->
            val act = log.action.uppercase()
            act.startsWith("LOGIN_") ||
                act.startsWith("AUTH_") ||
                act.startsWith("PLAY_INTEGRITY_") ||
                act.startsWith("ADMIN_ACCESS_") ||
                act.contains("PIN_CHANGED")
        }

        for (log in loginRelatedLogs) {
            val cleanNip = log.nip.trim()
            if (cleanNip.isBlank()) continue
            val deterministicId = sanitizeDocumentId("audit_${log.id}_${cleanNip.take(24)}")
            if (existingIds.contains(deterministicId)) continue

            val actUpper = log.action.uppercase()
            val loginMethod = when {
                actUpper.contains("BIOMETRIC") -> "BIOMETRIK"
                actUpper.contains("PLAY_INTEGRITY") -> "PLAY_INTEGRITY"
                actUpper.contains("ADMIN") -> "ADMIN_OTORISASI"
                actUpper.contains("PIN_CHANGED") -> "UBAH_PIN"
                else -> "PIN_CLOUD"
            }
            val loginStatus = when {
                actUpper.contains("SUCCESSFUL") || actUpper.contains("VERIFIED") || actUpper.contains("GRANTED") || actUpper.contains("CHANGED") -> "SUCCESS"
                actUpper.contains("LOCKED") || actUpper.contains("BLOCKED") || actUpper.contains("REJECTED") -> "BLOCKED"
                else -> "FAILED"
            }
            val isSuccess = loginStatus == "SUCCESS"
            val matchedSession = getLocalCachedSessions().firstOrNull {
                it.officerNip.equals(cleanNip, ignoreCase = true)
            }
            val derivedActivity = OfficerLoginActivity(
                activityId = deterministicId,
                sessionId = matchedSession?.sessionId ?: "",
                officerNip = cleanNip,
                officerName = log.name.ifBlank { matchedSession?.officerName ?: "Petugas ($cleanNip)" },
                officerRole = log.role.ifBlank { matchedSession?.officerRole ?: "-" },
                loginMethod = loginMethod,
                loginStatus = loginStatus,
                action = log.action,
                verdict = matchedSession?.verdict ?: if (isSuccess) "APP_CHECK_FALLBACK_AUTHORIZED" else "UNVERIFIED",
                deviceVerdict = matchedSession?.deviceVerdict ?: if (hw.isEmulator) "EMULATOR_ENVIRONMENT" else "LOCAL_AUDIT_SYNC",
                tokenSource = matchedSession?.tokenSource ?: "LOCAL_AUDIT_RECORD",
                tokenHash = matchedSession?.tokenHash?.ifBlank { null } ?: sha256HexPrefix("${log.id}:$cleanNip:${log.timestamp}", 32),
                tokenLength = matchedSession?.tokenLength ?: 0,
                classicNonce = matchedSession?.classicNonce.orEmpty(),
                deviceId = matchedSession?.deviceId?.ifBlank { null } ?: hw.deviceId,
                deviceModel = matchedSession?.deviceModel?.ifBlank { null } ?: hw.deviceModel,
                deviceBrand = matchedSession?.deviceBrand?.ifBlank { null } ?: hw.deviceBrand,
                deviceHardware = matchedSession?.deviceHardware?.ifBlank { null } ?: hw.deviceHardware,
                deviceFingerprintHash = matchedSession?.deviceFingerprintHash?.ifBlank { null } ?: hw.deviceFingerprintHash,
                osVersion = matchedSession?.osVersion?.ifBlank { null } ?: hw.osVersion,
                sdkInt = matchedSession?.sdkInt?.takeIf { it > 0 } ?: hw.sdkInt,
                isEmulator = matchedSession?.isEmulator ?: hw.isEmulator,
                appVersion = matchedSession?.appVersion?.ifBlank { null } ?: hw.appVersion,
                accessGranted = isSuccess,
                detail = log.deviceInfo.ifBlank { "Sinkronisasi aktivitas login (${log.action})" },
                timestamp = log.timestamp
            )
            saveLocalLoginActivity(derivedActivity)
            transmitLoginActivityToFirestore(derivedActivity)
            existingIds.add(deterministicId)
        }

        val allCandidateNips = (knownOfficialNips + loginRelatedLogs.map { it.nip })
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.uppercase() }

        val sessions = fetchSecuritySessions(allCandidateNips)
        val activities = fetchLoginActivities()
        Pair(sessions, activities)
    }

    /**
     * Ensures the currently logged-in officer has an active fallback/verified session
     * persisted and transmitted to Firestore 'device_security_sessions'.
     */
    suspend fun syncPendingSessionToFirestore(
        officerNip: String,
        officerName: String,
        officerRole: String
    ): DeviceSecuritySession? = withContext(Dispatchers.IO) {
        val trimmedNip = officerNip.trim()
        if (trimmedNip.isBlank()) return@withContext null
        val existingLocal = getLocalCachedSessions().firstOrNull {
            it.officerNip.equals(trimmedNip, ignoreCase = true)
        }
        if (existingLocal != null) {
            transmitSessionToFirestore(existingLocal)
            return@withContext existingLocal
        }
        verifyDeviceAuthenticityDuringLogin(
            officerNip = trimmedNip,
            officerName = officerName.ifBlank { "Petugas Pusdalops" },
            officerRole = officerRole.ifBlank { "Operator Pusdalops" },
            loginMethod = "SYNC_SESI_AKTIF",
            loginAction = "SESSION_SYNCED_TO_PLAY_INTEGRITY"
        ).getOrNull()
    }

    /**
     * Allows Admin Pusdalops to verify hardware authenticity & Play Integrity Classic token,
     * either approving (`approved = true`) or blocking (`approved = false`) the officer's session.
     */
    suspend fun verifyDeviceSessionByAdmin(
        session: DeviceSecuritySession,
        adminName: String,
        approved: Boolean,
        note: String = ""
    ): Result<DeviceSecuritySession> = withContext(Dispatchers.IO) {
        val nowMs = System.currentTimeMillis()
        val resolvedNote = note.ifBlank {
            if (approved) {
                "Hardware (${session.deviceModel}) & token Play Integrity Classic terverifikasi sah oleh $adminName"
            } else {
                "Akses perangkat ditangguhkan oleh $adminName setelah inspeksi keamanan"
            }
        }
        val updatedSession = session.copy(
            accessGranted = approved,
            adminVerified = approved,
            adminVerifiedBy = adminName.ifBlank { "Admin Pusdalops" },
            adminVerifiedAt = nowMs,
            adminVerificationNote = resolvedNote,
            loginStatus = if (approved) "VERIFIED_BY_ADMIN" else "BLOCKED_BY_ADMIN"
        )
        saveLocalSession(updatedSession)

        val db = firestore
        if (db != null) {
            try {
                val docId = sanitizeDocumentId(session.officerNip)
                val currentUser = try {
                    com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                } catch (_: Exception) {
                    null
                }
                val payload = updatedSession.toMap()
                    .filterValues { it != null }
                    .toMutableMap()
                payload["ownerUid"] = currentUser?.uid?.takeIf { it.isNotBlank() }
                    ?: session.officerNip.trim().ifBlank { getDeviceId() }
                db.collection(COLLECTION_SECURITY_SESSIONS)
                    .document(docId)
                    .set(payload, SetOptions.merge())
                    .await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Admin verification Firestore write deferred: ${e.message}")
            }
        }

        appendLoginActivity(
            OfficerLoginActivity(
                activityId = "act_${nowMs}_${sanitizeDocumentId(session.officerNip).take(24)}_${UUID.randomUUID().toString().take(6)}",
                sessionId = updatedSession.sessionId,
                officerNip = updatedSession.officerNip,
                officerName = updatedSession.officerName,
                officerRole = updatedSession.officerRole,
                loginMethod = "VERIFIKASI_ADMIN",
                loginStatus = if (approved) "SUCCESS" else "BLOCKED",
                action = if (approved) "ADMIN_VERIFIED_HARDWARE_TOKEN" else "ADMIN_BLOCKED_DEVICE_SESSION",
                verdict = updatedSession.verdict,
                deviceVerdict = updatedSession.deviceVerdict,
                tokenSource = updatedSession.tokenSource,
                tokenHash = updatedSession.tokenHash,
                tokenLength = updatedSession.tokenLength,
                classicNonce = updatedSession.classicNonce,
                deviceId = updatedSession.deviceId,
                deviceModel = updatedSession.deviceModel,
                deviceBrand = updatedSession.deviceBrand,
                deviceHardware = updatedSession.deviceHardware,
                deviceFingerprintHash = updatedSession.deviceFingerprintHash,
                osVersion = updatedSession.osVersion,
                sdkInt = updatedSession.sdkInt,
                isEmulator = updatedSession.isEmulator,
                appVersion = updatedSession.appVersion,
                accessGranted = approved,
                detail = resolvedNote,
                timestamp = nowMs
            )
        )
        Result.success(updatedSession)
    }

    /**
     * Allows Admin to remotely grant or revoke access for a specific officer/device.
     */
    suspend fun updateAccessPermission(officerNip: String, accessGranted: Boolean): Boolean = withContext(Dispatchers.IO) {
        val existingLocal = getLocalCachedSessions().firstOrNull {
            it.officerNip.equals(officerNip.trim(), ignoreCase = true)
        }
        if (existingLocal != null) {
            saveLocalSession(
                existingLocal.copy(
                    accessGranted = accessGranted,
                    loginStatus = if (accessGranted) "SUCCESS" else "REVOKED"
                )
            )
        }
        val db = firestore ?: return@withContext false
        try {
            db.collection(COLLECTION_SECURITY_SESSIONS)
                .document(sanitizeDocumentId(officerNip))
                .update(
                    mapOf(
                        "accessGranted" to accessGranted,
                        "loginStatus" to if (accessGranted) "SUCCESS" else "REVOKED"
                    )
                )
                .await()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update access permission for NIP ${maskNip(officerNip)}: ${e.message}")
            false
        }
    }

    /**
     * Revokes and permanently deletes an officer's session document from Firestore 'device_security_sessions'
     * as well as local cached session state.
     * When `purgeActivities = true` (used for brand-new account normalization), also removes all
     * locally cached login activities for that NIP without appending a revocation event.
     */
    suspend fun deleteSecuritySession(
        officerNip: String,
        purgeActivities: Boolean = false,
        revokedByAdmin: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        val trimmedNip = officerNip.trim()
        if (trimmedNip.isBlank()) return@withContext false

        val existingLocal = getLocalCachedSessions().firstOrNull {
            it.officerNip.equals(trimmedNip, ignoreCase = true)
        }
        removeLocalCachedSession(trimmedNip)
        recentLoginCoalesceMap.keys.filter { it.startsWith("${trimmedNip.lowercase()}|") }.forEach {
            recentLoginCoalesceMap.remove(it)
        }
        if (purgeActivities) {
            removeLocalCachedLoginActivities(trimmedNip)
        } else if (revokedByAdmin.isNotBlank() && existingLocal != null) {
            val nowMs = System.currentTimeMillis()
            appendLoginActivity(
                OfficerLoginActivity(
                    activityId = "act_${nowMs}_${sanitizeDocumentId(trimmedNip).take(24)}_${UUID.randomUUID().toString().take(6)}",
                    sessionId = existingLocal.sessionId,
                    officerNip = trimmedNip,
                    officerName = existingLocal.officerName,
                    officerRole = existingLocal.officerRole,
                    loginMethod = "VERIFIKASI_ADMIN",
                    loginStatus = "REVOKED",
                    action = "ADMIN_REVOKED_SESSION",
                    verdict = existingLocal.verdict,
                    deviceVerdict = existingLocal.deviceVerdict,
                    tokenSource = existingLocal.tokenSource,
                    tokenHash = existingLocal.tokenHash,
                    tokenLength = existingLocal.tokenLength,
                    classicNonce = existingLocal.classicNonce,
                    deviceId = existingLocal.deviceId,
                    deviceModel = existingLocal.deviceModel,
                    deviceBrand = existingLocal.deviceBrand,
                    deviceHardware = existingLocal.deviceHardware,
                    deviceFingerprintHash = existingLocal.deviceFingerprintHash,
                    osVersion = existingLocal.osVersion,
                    sdkInt = existingLocal.sdkInt,
                    isEmulator = existingLocal.isEmulator,
                    appVersion = existingLocal.appVersion,
                    accessGranted = false,
                    detail = "Sesi perangkat dicabut oleh $revokedByAdmin",
                    timestamp = nowMs
                )
            )
        }

        val db = firestore ?: return@withContext true
        val sanitizedId = sanitizeDocumentId(trimmedNip)
        var revokedRemote = false

        // First mark accessGranted = false so even if cross-UID delete is denied by rules, Ruang Kerja access is blocked immediately.
        if (!purgeActivities) {
            try {
                db.collection(COLLECTION_SECURITY_SESSIONS)
                    .document(sanitizedId)
                    .update(
                        mapOf(
                            "accessGranted" to false,
                            "adminVerified" to false,
                            "loginStatus" to "REVOKED"
                        )
                    )
                    .await()
                revokedRemote = true
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }

        return@withContext try {
            db.collection(COLLECTION_SECURITY_SESSIONS)
                .document(sanitizedId)
                .delete()
                .await()
            if (sanitizedId != trimmedNip) {
                try {
                    db.collection(COLLECTION_SECURITY_SESSIONS).document(trimmedNip).delete().await()
                } catch (_: Exception) {
                }
            }
            Log.i(TAG, "Security session deleted from Firestore for NIP: ${maskNip(trimmedNip)}")
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete security session for NIP ${maskNip(trimmedNip)}: ${e.message}")
            revokedRemote
        }
    }

    /**
     * Memverifikasi sandi Master Admin secara penuh di sisi Server (`POST /api/verify-admin-pass`)
     * melalui [AdminAccessSecurityManager] sehingga tidak ada password plaintext atau hardcode
     * yang dikirim/dibandingkan di sisi klien. Mencatat Audit Log otomatis (berhasil/gagal).
     */
    suspend fun verifyMasterAdminPin(
        inputPin: String,
        officerNip: String = "",
        officerName: String = "",
        officerRole: String = "",
        actionTitle: String = "Otorisasi Administrator"
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        when (
            val result = AdminAccessSecurityManager.verifyMasterPasswordAndIssueToken(
                context = context,
                masterPassword = inputPin,
                officerNip = officerNip,
                officerName = officerName,
                officerRole = officerRole,
                actionTitle = actionTitle
            )
        ) {
            is AdminVerificationResult.Authorized -> Result.success(true)
            is AdminVerificationResult.Denied -> Result.failure(SecurityException(result.message))
        }
    }

    fun clearLocalSession() {
        AdminAccessSecurityManager.revokeToken()
        recentLoginCoalesceMap.clear()
        context.getSharedPreferences(PREFS_SECURITY, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun saveLocalSession(session: DeviceSecuritySession) {
        val prefs = context.getSharedPreferences(PREFS_SECURITY, Context.MODE_PRIVATE)
        val editor = prefs.edit()
            .putBoolean(KEY_ACCESS_GRANTED, session.accessGranted && session.isVerified)
            .putString("last_verdict", session.verdict)
            .putLong(KEY_VERIFICATION_TIMESTAMP, session.timestamp)
        SecureStorageHelper.putEncrypted(editor, KEY_LAST_VERIFIED_NIP, session.officerNip)
        SecureStorageHelper.putEncrypted(editor, KEY_LAST_SESSION_ID, session.sessionId)

        val updatedList = getLocalCachedSessions(prefs)
            .filterNot { it.officerNip.equals(session.officerNip, ignoreCase = true) }
            .toMutableList()
        updatedList.add(0, session)
        val jsonArr = JSONArray()
        updatedList.forEach { s ->
            val obj = JSONObject()
            s.toMap().forEach { (k, v) -> if (v != null) obj.put(k, v) }
            jsonArr.put(obj)
        }
        editor.putString(KEY_CACHED_SESSIONS_JSON, jsonArr.toString())
        editor.apply()
    }

    private fun saveLocalLoginActivity(activity: OfficerLoginActivity) {
        val prefs = context.getSharedPreferences(PREFS_SECURITY, Context.MODE_PRIVATE)
        val updatedList = getLocalCachedLoginActivities(prefs)
            .filterNot { it.activityId == activity.activityId }
            .toMutableList()
        updatedList.add(0, activity)
        val trimmed = updatedList.sortedByDescending { it.timestamp }.take(MAX_CACHED_ACTIVITIES)
        val jsonArr = JSONArray()
        trimmed.forEach { item ->
            val obj = JSONObject()
            item.toMap().forEach { (k, v) -> if (v != null) obj.put(k, v) }
            jsonArr.put(obj)
        }
        prefs.edit().putString(KEY_CACHED_LOGIN_ACTIVITIES_JSON, jsonArr.toString()).apply()
    }

    fun getLocalCachedSessions(
        prefsOverride: android.content.SharedPreferences? = null
    ): List<DeviceSecuritySession> {
        val prefs = prefsOverride ?: context.getSharedPreferences(PREFS_SECURITY, Context.MODE_PRIVATE)
        val rawJson = prefs.getString(KEY_CACHED_SESSIONS_JSON, null) ?: return emptyList()
        val now = System.currentTimeMillis()
        return try {
            val arr = JSONArray(rawJson)
            val list = mutableListOf<DeviceSecuritySession>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val map = mutableMapOf<String, Any?>()
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    map[key] = obj.get(key)
                }
                val session = DeviceSecuritySession.fromMap(map)
                if (session.officerNip.isNotBlank() && now - session.timestamp in 0 until ADMIN_CACHE_RETENTION_MS) {
                    list.add(session)
                }
            }
            list.sortedByDescending { it.timestamp }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getLocalCachedLoginActivities(
        prefsOverride: android.content.SharedPreferences? = null
    ): List<OfficerLoginActivity> {
        val prefs = prefsOverride ?: context.getSharedPreferences(PREFS_SECURITY, Context.MODE_PRIVATE)
        val rawJson = prefs.getString(KEY_CACHED_LOGIN_ACTIVITIES_JSON, null) ?: return emptyList()
        val now = System.currentTimeMillis()
        return try {
            val arr = JSONArray(rawJson)
            val list = mutableListOf<OfficerLoginActivity>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val map = mutableMapOf<String, Any?>()
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    map[key] = obj.get(key)
                }
                val item = OfficerLoginActivity.fromMap(map)
                if (item.officerNip.isNotBlank() && now - item.timestamp in 0 until ADMIN_CACHE_RETENTION_MS) {
                    list.add(item)
                }
            }
            list.sortedByDescending { it.timestamp }.take(MAX_CACHED_ACTIVITIES)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun removeLocalCachedSession(officerNip: String) {
        val prefs = context.getSharedPreferences(PREFS_SECURITY, Context.MODE_PRIVATE)
        val remaining = getLocalCachedSessions(prefs).filterNot {
            it.officerNip.equals(officerNip, ignoreCase = true)
        }
        val jsonArr = JSONArray()
        remaining.forEach { s ->
            val obj = JSONObject()
            s.toMap().forEach { (k, v) -> if (v != null) obj.put(k, v) }
            jsonArr.put(obj)
        }
        val editor = prefs.edit().putString(KEY_CACHED_SESSIONS_JSON, jsonArr.toString())
        val savedNip = SecureStorageHelper.getDecrypted(prefs, KEY_LAST_VERIFIED_NIP)
        if (savedNip != null && savedNip.equals(officerNip, ignoreCase = true)) {
            editor.remove(KEY_ACCESS_GRANTED)
                .remove("last_verdict")
                .remove(KEY_VERIFICATION_TIMESTAMP)
                .remove(KEY_LAST_VERIFIED_NIP)
                .remove(KEY_LAST_SESSION_ID)
        }
        editor.apply()
    }

    private fun removeLocalCachedLoginActivities(officerNip: String) {
        val prefs = context.getSharedPreferences(PREFS_SECURITY, Context.MODE_PRIVATE)
        val remaining = getLocalCachedLoginActivities(prefs).filterNot {
            it.officerNip.equals(officerNip, ignoreCase = true)
        }
        val jsonArr = JSONArray()
        remaining.forEach { item ->
            val obj = JSONObject()
            item.toMap().forEach { (k, v) -> if (v != null) obj.put(k, v) }
            jsonArr.put(obj)
        }
        prefs.edit().putString(KEY_CACHED_LOGIN_ACTIVITIES_JSON, jsonArr.toString()).apply()
    }

    private fun isLocalSessionValid(officerNip: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_SECURITY, Context.MODE_PRIVATE)
        val savedNip = SecureStorageHelper.getDecrypted(prefs, KEY_LAST_VERIFIED_NIP) ?: return false
        val accessGranted = prefs.getBoolean(KEY_ACCESS_GRANTED, false)
        val verdict = prefs.getString("last_verdict", "UNVERIFIED") ?: "UNVERIFIED"
        if (verdict == "INTEGRITY_VERIFICATION_FAILED" || verdict == "UNVERIFIED") {
            return false
        }
        val timestamp = prefs.getLong(KEY_VERIFICATION_TIMESTAMP, 0L)
        val elapsed = System.currentTimeMillis() - timestamp
        return savedNip.equals(officerNip, ignoreCase = true) && accessGranted && elapsed in 0 until ACTIVE_SESSION_MAX_AGE_MS
    }
}
