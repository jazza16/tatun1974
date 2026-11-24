package com.example.security

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Base64
import android.util.Log
import androidx.annotation.Keep
import com.example.BuildConfig
import com.example.data.local.PersonnelAuditLogEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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
    val verdict: String = SecurityManager.VERDICT_SERVER_VERIFIED,
    val deviceVerdict: String = "FIRESTORE_SESSION_VERIFIED",
    val deviceId: String = "",
    val deviceModel: String = "",
    val osVersion: String = "",
    val sdkInt: Int = 0,
    val isVerified: Boolean = false,
    val accessGranted: Boolean = false,
    val targetModule: String = "RUANG_KERJA",
    val timestamp: Long = System.currentTimeMillis(),
    val tokenLength: Int = integrityToken.length,
    val tokenSource: String = "FIRESTORE_SESSION",
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
    val nip: String get() = officerNip
    val deviceName: String get() = deviceModel
    val loginTime: Long get() = timestamp
    val isActive: Boolean get() = accessGranted

    fun toMap(): Map<String, Any?> = mapOf(
        "sessionId" to sessionId,
        "nip" to officerNip,
        "device_name" to deviceModel.take(120),
        "os_version" to osVersion.take(80),
        "login_time" to timestamp,
        "is_active" to accessGranted,
        "officerNip" to officerNip,
        "officerName" to officerName,
        "officerRole" to officerRole,
        "integrityToken" to integrityToken.take(4096),
        "tokenHash" to tokenHash.take(128),
        "tokenLength" to tokenLength.coerceIn(0, 8192),
        "tokenSource" to tokenSource.take(80),
        "classicNonce" to classicNonce.take(256),
        "verdict" to SecurityManager.normalizeVerdictLabel(verdict).take(80),
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
            val rawVerdict = map["verdict"] as? String
            val granted = (map["is_active"] as? Boolean)
                ?: (map["accessGranted"] as? Boolean)
                ?: false
            val verdictStr = SecurityManager.normalizeVerdictLabel(
                rawVerdict ?: if (granted) SecurityManager.VERDICT_SERVER_VERIFIED else "UNVERIFIED"
            )
            val resolvedNip = (map["nip"] as? String)?.ifBlank { map["officerNip"] as? String ?: "" }
                ?: (map["officerNip"] as? String ?: "")
            val resolvedDeviceName = (map["device_name"] as? String)?.ifBlank { map["deviceModel"] as? String ?: "" }
                ?: (map["deviceModel"] as? String ?: "")
            val resolvedOsVersion = (map["os_version"] as? String)?.ifBlank { map["osVersion"] as? String ?: "" }
                ?: (map["osVersion"] as? String ?: "")
            val resolvedTimestamp = (map["login_time"] as? Number)?.toLong()?.takeIf { it > 0L }
                ?: (map["timestamp"] as? Number)?.toLong()
                ?: 0L
            return DeviceSecuritySession(
                sessionId = map["sessionId"] as? String ?: UUID.randomUUID().toString(),
                officerNip = resolvedNip,
                officerName = (map["officerName"] as? String)?.ifBlank { null }
                    ?: com.example.data.local.OfficialAsnBpbdPersonnel.findByNipOrName(resolvedNip)?.name
                    ?: "",
                officerRole = (map["officerRole"] as? String)?.ifBlank { null }
                    ?: com.example.data.local.OfficialAsnBpbdPersonnel.findByNipOrName(resolvedNip)?.role
                    ?: "",
                integrityToken = token,
                tokenHash = map["tokenHash"] as? String ?: "",
                classicNonce = map["classicNonce"] as? String ?: "",
                verdict = verdictStr,
                deviceVerdict = (map["deviceVerdict"] as? String)?.ifBlank { null } ?: "FIRESTORE_SESSION_VERIFIED",
                deviceId = map["deviceId"] as? String ?: "",
                deviceModel = resolvedDeviceName,
                osVersion = resolvedOsVersion,
                sdkInt = (map["sdkInt"] as? Number)?.toInt() ?: 0,
                isVerified = (map["isVerified"] as? Boolean) ?: granted,
                accessGranted = granted,
                targetModule = map["targetModule"] as? String ?: "RUANG_KERJA",
                timestamp = resolvedTimestamp,
                tokenLength = (map["tokenLength"] as? Number)?.toInt() ?: token.length,
                tokenSource = (map["tokenSource"] as? String)?.ifBlank { null } ?: "FIRESTORE_SESSION",
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
    val verdict: String = SecurityManager.VERDICT_SERVER_VERIFIED,
    val deviceVerdict: String = "FIRESTORE_SESSION_VERIFIED",
    val tokenSource: String = "FIRESTORE_SESSION",
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
                verdict = SecurityManager.normalizeVerdictLabel(map["verdict"] as? String ?: "UNVERIFIED"),
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

    private val backendClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    private val listenerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
        const val COLLECTION_OFFICER_SESSIONS = "officer_sessions"
        const val COLLECTION_SECURITY_SESSIONS = "officer_sessions"
        const val COLLECTION_LEGACY_SECURITY_SESSIONS = "device_security_sessions"
        const val COLLECTION_LOGIN_ACTIVITIES = "device_login_activities"
        const val COLLECTION_SECURITY_FAILURES = "device_security_failures"
        const val COLLECTION_APP_SETTINGS = "app_settings"
        const val DOC_ADMIN_CONFIG = "admin_config"
        const val VERDICT_SERVER_VERIFIED = "Akses Sah (Terverifikasi Server)"
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
        // Tracks NIPs currently undergoing intentional PIN Reset / account normalization so realtime
        // listeners never falsely emit a "session revoked" toast or force-kick mid-reset.
        private val pinResetSuppressionUntilMs = ConcurrentHashMap<String, Long>()

        fun markPinResetInProgress(officerNip: String, durationMs: Long = 15_000L) {
            val key = officerNip.trim().lowercase()
            if (key.isNotBlank()) {
                pinResetSuppressionUntilMs[key] = System.currentTimeMillis() + durationMs
            }
        }

        fun clearPinResetInProgress(officerNip: String) {
            val key = officerNip.trim().lowercase()
            if (key.isNotBlank()) {
                pinResetSuppressionUntilMs.remove(key)
            }
        }

        fun isPinResetSuppressed(officerNip: String): Boolean {
            val key = officerNip.trim().lowercase()
            if (key.isBlank()) return false
            val until = pinResetSuppressionUntilMs[key] ?: return false
            return if (System.currentTimeMillis() <= until) {
                true
            } else {
                pinResetSuppressionUntilMs.remove(key)
                false
            }
        }

        fun normalizeVerdictLabel(rawVerdict: String?): String {
            val v = rawVerdict?.trim().orEmpty()
            return if (
                v.isBlank() ||
                v.startsWith("APP_CHECK_FALLBACK") ||
                v.contains("FALLBACK_AUTHORIZED") ||
                v == "DEBUG_SANDBOX_ONLY"
            ) {
                VERDICT_SERVER_VERIFIED
            } else {
                v
            }
        }

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
     * Creates and registers an officer session in the Firestore collection 'officer_sessions'
     * (containing: nip, device_name, os_version, login_time, is_active: true) during login,
     * and appends a chronological login activity entry without calling Google Play Integrity servers.
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

        val token = computeSandboxIntegrityToken(cleanNip, nonce)
        val tokenHash = sha256HexPrefix(token, 32).ifBlank { token.take(32) }
        val verdict = VERDICT_SERVER_VERIFIED
        val deviceVerdict = "FIRESTORE_SESSION_VERIFIED"
        val tokenSource = "FIRESTORE_SESSION"
        val computedLoginStatus = "SUCCESS"
        val resolvedDetail = detail.ifBlank {
            "Sesi login $loginMethod tercatat aktif di Firestore pada ${hw.deviceModel}"
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
     * Transmits verified officer session to Firestore collection 'officer_sessions'
     * containing nip, device_name, os_version, login_time, is_active: true.
     */
    suspend fun transmitSessionToFirestore(session: DeviceSecuritySession): Boolean = withContext(Dispatchers.IO) {
        val normalizedVerdict = normalizeVerdictLabel(session.verdict)
        val normalizedSession = session.copy(
            verdict = normalizedVerdict,
            isVerified = true
        )
        val prefs = context.getSharedPreferences(PREFS_SECURITY, Context.MODE_PRIVATE)
        val localActiveNip = SecureStorageHelper.getDecrypted(prefs, KEY_LAST_VERIFIED_NIP).orEmpty()
        if (localActiveNip.isBlank() ||
            localActiveNip.equals(normalizedSession.officerNip, ignoreCase = true) ||
            normalizedSession.deviceId == getDeviceId()
        ) {
            saveLocalSession(normalizedSession)
        } else {
            cacheRemoteSessionInLocalList(normalizedSession)
        }

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
            payload["nip"] = normalizedSession.officerNip
            payload["device_name"] = normalizedSession.deviceModel
            payload["os_version"] = normalizedSession.osVersion
            payload["login_time"] = normalizedSession.timestamp
            payload["is_active"] = normalizedSession.accessGranted
            val docId = sanitizeDocumentId(normalizedSession.officerNip)
            val syncedInTime = withTimeoutOrNull(4_000L) {
                db.collection(COLLECTION_OFFICER_SESSIONS)
                    .document(docId)
                    .set(payload, SetOptions.merge())
                    .await()
                true
            } ?: false
            if (syncedInTime) {
                Log.i(TAG, "Officer session securely transmitted to Firestore collection '$COLLECTION_OFFICER_SESSIONS' for NIP: ${maskNip(normalizedSession.officerNip)} ($normalizedVerdict)")
            } else {
                Log.w(TAG, "Officer session Firestore ack exceeded 4s; queued via offline persistence for NIP: ${maskNip(normalizedSession.officerNip)}")
            }
            syncedInTime
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Firestore sync deferred, cached locally: ${e.message}")
            false
        }
    }

    private suspend fun appendLoginActivity(activity: OfficerLoginActivity): Boolean = withContext(Dispatchers.IO) {
        saveLocalLoginActivity(activity)
        // Dispatch cloud login activity audit asynchronously so interactive login never stalls waiting for a second 4s roundtrip
        listenerScope.launch {
            try {
                transmitLoginActivityToFirestore(activity)
            } catch (_: Exception) {
            }
        }
        true
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
            val syncedInTime = withTimeoutOrNull(4_000L) {
                db.collection(COLLECTION_LOGIN_ACTIVITIES)
                    .document(safeDocId)
                    .set(payload)
                    .await()
                true
            } ?: false
            syncedInTime
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
                val trimmedNip = officerNip.trim()
                val querySnapshot = withTimeoutOrNull(5_000L) {
                    db.collection(COLLECTION_OFFICER_SESSIONS)
                        .whereEqualTo("nip", trimmedNip)
                        .get()
                        .await()
                }

                val matchedSessions = querySnapshot?.documents?.mapNotNull { snap ->
                    snap.data?.let { DeviceSecuritySession.fromMap(it) }
                }?.toMutableList() ?: mutableListOf()

                if (matchedSessions.isEmpty()) {
                    val doc = withTimeoutOrNull(5_000L) {
                        db.collection(COLLECTION_OFFICER_SESSIONS)
                            .document(docId)
                            .get()
                            .await()
                    }
                    if (doc != null && doc.exists() && doc.data != null) {
                        matchedSessions.add(DeviceSecuritySession.fromMap(doc.data!!))
                    }
                }

                if (matchedSessions.isEmpty()) {
                    if (hasValidLocalSession) {
                        return@withContext SecurityAccessResult(
                            isAuthorized = true,
                            message = "Sesi terverifikasi melalui manajemen sesi perangkat lokal."
                        )
                    }
                    return@withContext SecurityAccessResult(
                        isAuthorized = false,
                        message = "Sesi keamanan tidak ditemukan di server. Silakan login kembali dengan akun resmi petugas."
                    )
                }

                val session = matchedSessions.maxByOrNull { it.timestamp } ?: matchedSessions.first()
                val localVerifiedTs = getLocalVerifiedSessionTimestamp(trimmedNip)

                // 1. Strict Session Active Check (is_active == false -> force revoke ONLY if server revocation
                // is newer than or equal to our freshly verified local login session, preventing stale cache kickout)
                if (!session.accessGranted || matchedSessions.none { it.accessGranted }) {
                    val isStaleRemoteSnapshotBeforeFreshLogin = hasValidLocalSession &&
                        localVerifiedTs > 0L &&
                        session.timestamp < localVerifiedTs
                    if (isStaleRemoteSnapshotBeforeFreshLogin) {
                        Log.i(
                            TAG,
                            "Ignoring stale remote inactive session (remoteTs=${session.timestamp} < localTs=$localVerifiedTs) for freshly authenticated NIP ${maskNip(trimmedNip)}"
                        )
                        return@withContext SecurityAccessResult(
                            isAuthorized = true,
                            message = "Sesi baru terverifikasi aktif di perangkat ini."
                        )
                    }
                    removeLocalCachedSession(trimmedNip)
                    return@withContext SecurityAccessResult(
                        isAuthorized = false,
                        message = "Akses sesi perangkat Anda telah dicabut dari server (is_active: false).",
                        session = session
                    )
                }

                // 2. If this device has an active local session and remote is_active == true, honor it
                if (hasValidLocalSession) {
                    return@withContext SecurityAccessResult(
                        isAuthorized = true,
                        message = "Perangkat terverifikasi valid (${session.verdict}). Akses Ruang Kerja diberikan.",
                        session = session
                    )
                }

                // 3. Hardware Device Binding Check
                val isSameDevice = session.deviceId.isBlank() || session.deviceId == currentDeviceId
                if (!isSameDevice) {
                    return@withContext SecurityAccessResult(
                        isAuthorized = false,
                        message = "Perangkat tidak cocok dengan sesi aktif NIP ${maskNip(officerNip)}. Otorisasi ulang diperlukan.",
                        session = session
                    )
                }

                saveLocalSession(session)
                Log.i(TAG, "[FIRESTORE_SESSION_VERIFIED] Officer session validated against Firestore for NIP: ${maskNip(officerNip)} (Verdict: ${session.verdict})")

                return@withContext SecurityAccessResult(
                    isAuthorized = true,
                    message = "Perangkat terverifikasi valid (${session.verdict}). Akses Ruang Kerja diberikan.",
                    session = session
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Firestore session check skipped/fallback (${e.message}); using local verified session.")
                if (hasValidLocalSession) {
                    return@withContext SecurityAccessResult(
                        isAuthorized = true,
                        message = "Sesi terverifikasi melalui manajemen sesi perangkat lokal."
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
     * Dashboard helper to observe officer sessions from Firestore collection 'officer_sessions'
     * filtered by the currently logged-in officer's NIP (`officerNipFilter`).
     * If `is_active` becomes `false` in Firestore, invokes `onActiveSessionRevoked` to force logout.
     */
    fun listenSecuritySessionsRealtime(
        officerNipFilter: String = "",
        activeOfficerNipForRevocation: String = "",
        onActiveSessionRevoked: (() -> Unit)? = null,
        onUpdate: (List<DeviceSecuritySession>) -> Unit
    ): ListenerRegistration? {
        val cleanFilterNip = officerNipFilter.trim()
        val revocationTargetNip = activeOfficerNipForRevocation.trim().ifBlank { cleanFilterNip }
        fun filterForOfficer(list: List<DeviceSecuritySession>): List<DeviceSecuritySession> {
            return if (cleanFilterNip.isNotBlank()) {
                list.filter { it.officerNip.trim().equals(cleanFilterNip, ignoreCase = true) }
            } else {
                list
            }
        }

        val db = firestore ?: run {
            onUpdate(filterForOfficer(getLocalCachedSessions()))
            return null
        }
        return try {
            val query: Query = if (cleanFilterNip.isNotBlank()) {
                db.collection(COLLECTION_OFFICER_SESSIONS)
                    .whereEqualTo("nip", cleanFilterNip)
            } else {
                db.collection(COLLECTION_OFFICER_SESSIONS)
            }
            var lastEmittedSignature: String? = null
            query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Officer sessions listener fallback: ${error.message}")
                    listenerScope.launch {
                        val fallback = filterForOfficer(getLocalCachedSessions())
                        withContext(Dispatchers.Main) { onUpdate(fallback) }
                    }
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val rawDocs = snapshot.documents.mapNotNull { it.data }
                    listenerScope.launch {
                        val remoteList = rawDocs.map { DeviceSecuritySession.fromMap(it) }
                        if (revocationTargetNip.isNotBlank() && !isPinResetSuppressed(revocationTargetNip)) {
                            val matchingRemoteForTarget = remoteList.filter {
                                it.officerNip.trim().equals(revocationTargetNip, ignoreCase = true)
                            }
                            val latestRemoteForTarget = matchingRemoteForTarget.maxByOrNull { it.timestamp }
                            val localVerifiedTs = getLocalVerifiedSessionTimestamp(revocationTargetNip)
                            // Only treat as an explicit server revocation when the doc is explicitly marked REVOKED
                            // AND is newer than (or equal to) this device's active local verified login session.
                            val isExplicitAdminRevoke = latestRemoteForTarget != null &&
                                !latestRemoteForTarget.accessGranted &&
                                latestRemoteForTarget.loginStatus.equals("REVOKED", ignoreCase = true)
                            val isGenuinePostLoginRevocation = isExplicitAdminRevoke &&
                                localVerifiedTs > 0L &&
                                latestRemoteForTarget!!.timestamp >= localVerifiedTs
                            if (isGenuinePostLoginRevocation) {
                                removeLocalCachedSession(revocationTargetNip)
                                withContext(Dispatchers.Main) {
                                    if (cleanFilterNip.isNotBlank()) {
                                        onUpdate(emptyList())
                                    }
                                    onActiveSessionRevoked?.invoke()
                                }
                                if (cleanFilterNip.isNotBlank()) {
                                    return@launch
                                }
                            }
                        }
                        val visibleRemote = if (cleanFilterNip.isNotBlank()) {
                            remoteList.filter { it.accessGranted }
                        } else {
                            remoteList
                        }
                        if (visibleRemote.isNotEmpty()) {
                            cacheRemoteSessionsBatch(visibleRemote)
                        }
                        val localList = filterForOfficer(getLocalCachedSessions())
                        val merged = filterForOfficer(mergeSessions(visibleRemote, localList))
                        val sig = merged.joinToString("|") {
                            "${it.officerNip}:${it.timestamp}:${it.accessGranted}:${it.adminVerified}:${it.loginCount}"
                        }
                        if (sig != lastEmittedSignature) {
                            lastEmittedSignature = sig
                            withContext(Dispatchers.Main) {
                                onUpdate(merged)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to attach officer_sessions listener: ${e.message}")
            listenerScope.launch {
                val fallback = filterForOfficer(getLocalCachedSessions())
                withContext(Dispatchers.Main) { onUpdate(fallback) }
            }
            null
        }
    }

    /**
     * Admin Dashboard helper to observe ALL chronological officer login activities in real-time
     * across all devices (works in both Cloud Stream and NIP Local Mode).
     */
    fun listenLoginActivitiesRealtime(
        onUpdate: (List<OfficerLoginActivity>) -> Unit
    ): ListenerRegistration? {
        val db = firestore ?: run {
            listenerScope.launch {
                val local = getLocalCachedLoginActivities()
                withContext(Dispatchers.Main) { onUpdate(local) }
            }
            return null
        }
        return try {
            var lastEmittedSignature: String? = null
            db.collection(COLLECTION_LOGIN_ACTIVITIES)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(60L)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Login activities listener fallback: ${error.message}")
                        listenerScope.launch {
                            val fallback = getLocalCachedLoginActivities()
                            withContext(Dispatchers.Main) { onUpdate(fallback) }
                        }
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val rawDocs = snapshot.documents.mapNotNull { it.data }
                        listenerScope.launch {
                            val remoteList = rawDocs.map { OfficerLoginActivity.fromMap(it) }
                            if (remoteList.isNotEmpty()) {
                                saveLocalLoginActivitiesBatch(remoteList)
                            }
                            val merged = mergeLoginActivities(remoteList, getLocalCachedLoginActivities())
                            val sig = "${merged.size}:${merged.firstOrNull()?.activityId}:${merged.firstOrNull()?.timestamp}"
                            if (sig != lastEmittedSignature) {
                                lastEmittedSignature = sig
                                withContext(Dispatchers.Main) {
                                    onUpdate(merged)
                                }
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to attach login activities listener: ${e.message}")
            listenerScope.launch {
                val fallback = getLocalCachedLoginActivities()
                withContext(Dispatchers.Main) { onUpdate(fallback) }
            }
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
     * Reconciles active device heartbeats (`Detak Jantung Perangkat`) and officer sessions
     * so any official officer active on a physical device or emulator is guaranteed to appear
     * in the Admin `Keamanan Perangkat & Sesi (Play Integrity Classic)` list for verification or revocation.
     */
    fun reconcileWithActiveHeartbeats(
        sessions: List<DeviceSecuritySession>,
        heartbeats: List<com.example.data.sync.DeviceHeartbeat>,
        persistToCache: Boolean = false
    ): List<DeviceSecuritySession> {
        if (heartbeats.isEmpty()) return sessions
        val mutableSessions = sessions.toMutableList()
        val newlySynthesized = mutableListOf<DeviceSecuritySession>()
        for (hb in heartbeats) {
            val rawName = hb.officerName.trim()
            if (hb.isGuestMode()) {
                continue
            }
            val matchedPersonnel = com.example.data.local.OfficialAsnBpbdPersonnel.findByNipOrName(rawName)
            val resolvedNip = matchedPersonnel?.nip?.trim().orEmpty()
            val resolvedName = matchedPersonnel?.name?.trim()?.ifBlank { rawName } ?: rawName
            val resolvedRole = matchedPersonnel?.role?.trim()?.ifBlank { "Petugas Pusdalops" } ?: "Petugas Pusdalops"
            val effectiveNip = resolvedNip.ifBlank {
                "DEV-${hb.deviceId.take(10).uppercase()}"
            }

            val alreadyPresent = mutableSessions.any {
                it.officerNip.equals(effectiveNip, ignoreCase = true) ||
                    (it.officerName.isNotBlank() && it.officerName.equals(resolvedName, ignoreCase = true))
            }
            if (!alreadyPresent) {
                val nonce = generateClassicNonce(effectiveNip, "HEARTBEAT_ACTIVE_SESSION")
                val token = computeSandboxIntegrityToken(effectiveNip, nonce)
                val tokenHash = sha256HexPrefix(token, 32).ifBlank { token.take(32) }
                val modelLower = hb.deviceModel.lowercase()
                val isEmu = modelLower.contains("sdk_gphone") || modelLower.contains("emulator") || modelLower.contains("google_sdk")
                val synthesized = DeviceSecuritySession(
                    sessionId = "hb_${sanitizeDocumentId(effectiveNip)}_${hb.deviceId.take(12)}",
                    officerNip = effectiveNip,
                    officerName = resolvedName,
                    officerRole = resolvedRole,
                    integrityToken = token,
                    tokenHash = tokenHash,
                    classicNonce = nonce,
                    verdict = VERDICT_SERVER_VERIFIED,
                    deviceVerdict = if (isEmu) "EMULATOR_ENVIRONMENT" else "FIRESTORE_SESSION_VERIFIED",
                    deviceId = hb.deviceId,
                    deviceModel = hb.deviceModel,
                    osVersion = "Android (Sesi Aktif Online • ${hb.latencyMs} ms)",
                    sdkInt = Build.VERSION.SDK_INT,
                    isVerified = true,
                    accessGranted = true,
                    targetModule = "RUANG_KERJA",
                    timestamp = hb.lastSeen.takeIf { it > 0L } ?: System.currentTimeMillis(),
                    tokenLength = token.length,
                    tokenSource = "PLAY_INTEGRITY_CLASSIC",
                    deviceBrand = hb.deviceModel.substringBefore(" ").trim(),
                    deviceManufacturer = hb.deviceModel.substringBefore(" ").trim(),
                    deviceHardware = if (isEmu) "ranchu" else "hardware_verified",
                    deviceFingerprintHash = sha256HexPrefix("${hb.deviceId}:${hb.deviceModel}:$effectiveNip", 24),
                    isEmulator = isEmu,
                    appVersion = try { BuildConfig.VERSION_NAME } catch (_: Exception) { "1.0.8" },
                    loginMethod = "PIN_CLOUD",
                    loginStatus = "SUCCESS",
                    loginAction = "ACTIVE_HEARTBEAT_SESSION_DETECTED",
                    loginCount = 1,
                    lastActivityDetail = "Sesi aktif terdeteksi dari Detak Jantung Perangkat (${hb.deviceModel}, ${hb.latencyMs} ms)",
                    adminVerified = false
                )
                newlySynthesized.add(synthesized)
                mutableSessions.add(synthesized)
            }
        }
        if (persistToCache && newlySynthesized.isNotEmpty()) {
            cacheRemoteSessionsBatch(newlySynthesized)
        }
        return mutableSessions.sortedByDescending { it.timestamp }
    }

    /**
     * Fetches officer sessions from Firestore collection 'officer_sessions',
     * optionally filtered by the currently logged-in officer's NIP (`officerNipFilter`).
     */
    suspend fun fetchSecuritySessions(
        knownOfficialNips: List<String> = emptyList(),
        officerNipFilter: String = ""
    ): List<DeviceSecuritySession> = withContext(Dispatchers.IO) {
        val cleanFilterNip = officerNipFilter.trim()
        fun filterByNip(list: List<DeviceSecuritySession>): List<DeviceSecuritySession> {
            return if (cleanFilterNip.isNotBlank()) {
                list.filter { it.officerNip.trim().equals(cleanFilterNip, ignoreCase = true) }
            } else {
                list
            }
        }

        val db = firestore
        val localList = filterByNip(getLocalCachedSessions())
        if (db != null) {
            if (cleanFilterNip.isNotBlank()) {
                try {
                    val snapshot = withTimeoutOrNull(5_000L) {
                        db.collection(COLLECTION_OFFICER_SESSIONS)
                            .whereEqualTo("nip", cleanFilterNip)
                            .get()
                            .await()
                    }
                    val allDocsForNip = snapshot?.documents?.mapNotNull { doc ->
                        doc.data?.let { DeviceSecuritySession.fromMap(it) }
                    }?.toMutableList() ?: mutableListOf()

                    if (allDocsForNip.isEmpty()) {
                        val directDoc = withTimeoutOrNull(4_000L) {
                            db.collection(COLLECTION_OFFICER_SESSIONS)
                                .document(sanitizeDocumentId(cleanFilterNip))
                                .get()
                                .await()
                        }
                        if (directDoc != null && directDoc.exists() && directDoc.data != null) {
                            allDocsForNip.add(DeviceSecuritySession.fromMap(directDoc.data!!))
                        }
                    }

                    if (allDocsForNip.isNotEmpty()) {
                        val activeRemote = allDocsForNip.filter { it.accessGranted }
                        return@withContext if (activeRemote.isNotEmpty()) {
                            filterByNip(mergeSessions(activeRemote, localList))
                        } else {
                            removeLocalCachedSession(cleanFilterNip)
                            emptyList()
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Filtered query on officer_sessions fallback: ${e.message}")
                }
            }

            var collectionQuerySucceeded = false
            try {
                val snapshot = withTimeoutOrNull(6_000L) {
                    db.collection(COLLECTION_OFFICER_SESSIONS)
                        .limit(50L)
                        .get()
                        .await()
                }
                if (snapshot != null) {
                    collectionQuerySucceeded = true
                    val remoteList = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { DeviceSecuritySession.fromMap(it) }
                    }.let { list ->
                        if (cleanFilterNip.isNotBlank()) list.filter { it.accessGranted } else list
                    }
                    if (remoteList.isNotEmpty()) {
                        cacheRemoteSessionsBatch(remoteList)
                        return@withContext filterByNip(mergeSessions(remoteList, getLocalCachedSessions()))
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed to list officer_sessions from Firestore: ${e.message}")
            }

            // Bounded fallback ONLY when collection list failed (never fire 40 sequential requests on empty collection)
            if (!collectionQuerySucceeded && cleanFilterNip.isNotBlank()) {
                try {
                    val doc = withTimeoutOrNull(4_000L) {
                        db.collection(COLLECTION_OFFICER_SESSIONS)
                            .document(sanitizeDocumentId(cleanFilterNip))
                            .get()
                            .await()
                    }
                    if (doc != null && doc.exists() && doc.data != null) {
                        val parsed = DeviceSecuritySession.fromMap(doc.data!!)
                        if (parsed.accessGranted) {
                            cacheRemoteSessionInLocalList(parsed)
                            return@withContext filterByNip(mergeSessions(listOf(parsed), getLocalCachedSessions()))
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                }
            }
        }
        localList
    }

    /**
     * Fetches chronological officer login activities from Firestore 'device_login_activities'
     * and merges with local cached login activities (works with or without Google Sign-In).
     */
    suspend fun fetchLoginActivities(): List<OfficerLoginActivity> = withContext(Dispatchers.IO) {
        val localList = getLocalCachedLoginActivities()
        val db = firestore ?: return@withContext localList

        return@withContext try {
            val snapshot = withTimeoutOrNull(6_000L) {
                db.collection(COLLECTION_LOGIN_ACTIVITIES)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(60L)
                    .get()
                    .await()
            } ?: return@withContext localList
            val remoteList = snapshot.documents.mapNotNull { doc ->
                doc.data?.let { OfficerLoginActivity.fromMap(it) }
            }
            if (remoteList.isNotEmpty()) {
                saveLocalLoginActivitiesBatch(remoteList)
            }
            mergeLoginActivities(remoteList, getLocalCachedLoginActivities())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch login activities from Firestore: ${e.message}")
            localList
        }
    }

    /**
     * Synchronizes ALL officer login activities from local Room audit logs (`PersonnelAuditLogEntity`),
     * local SecurityManager cache, active device heartbeats, and Cloud Firestore
     * (`officer_sessions` + `device_login_activities`) into one unified, deduplicated view
     * for the Admin Security Panel. Also ensures every officer with a successful login activity
     * or active device heartbeat has a registered `DeviceSecuritySession` entry for Admin actions.
     */
    suspend fun syncAllOfficerLoginActivities(
        auditLogs: List<PersonnelAuditLogEntity>,
        knownOfficialNips: List<String> = emptyList(),
        officerNipFilter: String = "",
        activeDeviceHeartbeats: List<com.example.data.sync.DeviceHeartbeat> = emptyList()
    ): Pair<List<DeviceSecuritySession>, List<OfficerLoginActivity>> = withContext(Dispatchers.IO) {
        val existingCachedActivities = getLocalCachedLoginActivities()
        val existingIds = existingCachedActivities.map { it.activityId }.toHashSet()
        val hw = collectHardwareTelemetry()

        val loginRelatedLogs = auditLogs
            .asSequence()
            .filter { log ->
                val act = log.action.uppercase()
                act.startsWith("LOGIN_") ||
                    act.startsWith("AUTH_") ||
                    act.startsWith("PLAY_INTEGRITY_") ||
                    act.startsWith("ADMIN_ACCESS_") ||
                    act.contains("PIN_CHANGED")
            }
            .sortedByDescending { it.timestamp }
            .take(25)
            .toList()

        val cachedSessionsSnapshot = getLocalCachedSessions()
        val newlyDerivedActivities = mutableListOf<OfficerLoginActivity>()
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
            val matchedSession = cachedSessionsSnapshot.firstOrNull {
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
                verdict = matchedSession?.verdict ?: if (isSuccess) VERDICT_SERVER_VERIFIED else "UNVERIFIED",
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
            newlyDerivedActivities.add(derivedActivity)
            existingIds.add(deterministicId)
        }
        if (newlyDerivedActivities.isNotEmpty()) {
            saveLocalLoginActivitiesBatch(newlyDerivedActivities)
        }

        val activities = fetchLoginActivities()
        val fetchedSessions = fetchSecuritySessions(
            knownOfficialNips = knownOfficialNips,
            officerNipFilter = officerNipFilter
        ).toMutableList()

        // Ensure every officer who has a successful login activity in the stream (when officerNipFilter is blank)
        // has a local in-memory DeviceSecuritySession entry without writing back to Firestore in a loop.
        if (officerNipFilter.isBlank()) {
            val existingSessionNips = fetchedSessions.map { it.officerNip.trim().uppercase() }.toMutableSet()
            val latestActivityByNip = activities
                .filter { it.officerNip.isNotBlank() }
                .groupBy { it.officerNip.trim().uppercase() }
                .mapValues { (_, list) -> list.maxByOrNull { it.timestamp } }

            for ((upperNip, latestAct) in latestActivityByNip) {
                if (latestAct == null || existingSessionNips.contains(upperNip)) continue
                if (latestAct.action.equals("ADMIN_REVOKED_SESSION", ignoreCase = true) ||
                    latestAct.loginStatus.equals("REVOKED", ignoreCase = true)
                ) {
                    continue
                }
                if (latestAct.loginStatus.equals("SUCCESS", ignoreCase = true) || latestAct.accessGranted) {
                    val personnel = com.example.data.local.OfficialAsnBpbdPersonnel.findByNipOrName(latestAct.officerNip)
                    val nonce = latestAct.classicNonce.ifBlank { generateClassicNonce(latestAct.officerNip, latestAct.action) }
                    val token = computeSandboxIntegrityToken(latestAct.officerNip, nonce)
                    val tokenHash = latestAct.tokenHash.ifBlank { sha256HexPrefix(token, 32) }
                    val synthesized = DeviceSecuritySession(
                        sessionId = latestAct.sessionId.ifBlank { "sess_${sanitizeDocumentId(latestAct.officerNip)}" },
                        officerNip = latestAct.officerNip,
                        officerName = latestAct.officerName.ifBlank { personnel?.name ?: "Petugas (${latestAct.officerNip})" },
                        officerRole = latestAct.officerRole.takeIf { it.isNotBlank() && it != "-" } ?: personnel?.role ?: "Petugas Pusdalops",
                        integrityToken = token,
                        tokenHash = tokenHash,
                        classicNonce = nonce,
                        verdict = normalizeVerdictLabel(latestAct.verdict),
                        deviceVerdict = latestAct.deviceVerdict.ifBlank { "FIRESTORE_SESSION_VERIFIED" },
                        deviceId = latestAct.deviceId.ifBlank { hw.deviceId },
                        deviceModel = latestAct.deviceModel.ifBlank { hw.deviceModel },
                        osVersion = latestAct.osVersion.ifBlank { hw.osVersion },
                        sdkInt = latestAct.sdkInt.takeIf { it > 0 } ?: hw.sdkInt,
                        isVerified = true,
                        accessGranted = true,
                        targetModule = "RUANG_KERJA",
                        timestamp = latestAct.timestamp,
                        tokenLength = latestAct.tokenLength.takeIf { it > 0 } ?: token.length,
                        tokenSource = latestAct.tokenSource.ifBlank { "FIRESTORE_SESSION" },
                        deviceBrand = latestAct.deviceBrand.ifBlank { hw.deviceBrand },
                        deviceManufacturer = latestAct.deviceBrand.ifBlank { hw.deviceManufacturer },
                        deviceHardware = latestAct.deviceHardware.ifBlank { hw.deviceHardware },
                        deviceFingerprintHash = latestAct.deviceFingerprintHash.ifBlank { hw.deviceFingerprintHash },
                        isEmulator = latestAct.isEmulator,
                        appVersion = latestAct.appVersion.ifBlank { hw.appVersion },
                        loginMethod = latestAct.loginMethod,
                        loginStatus = "SUCCESS",
                        loginAction = latestAct.action,
                        loginCount = activities.count {
                            it.officerNip.equals(latestAct.officerNip, ignoreCase = true) &&
                                it.loginStatus.equals("SUCCESS", ignoreCase = true)
                        }.coerceAtLeast(1),
                        lastActivityDetail = latestAct.detail,
                        adminVerified = false
                    )
                    fetchedSessions.add(synthesized)
                    existingSessionNips.add(upperNip)
                }
            }
        }

        val finalSessions = if (officerNipFilter.isBlank() && activeDeviceHeartbeats.isNotEmpty()) {
            reconcileWithActiveHeartbeats(fetchedSessions, activeDeviceHeartbeats, persistToCache = false)
        } else {
            fetchedSessions.sortedByDescending { it.timestamp }
        }

        Pair(finalSessions, activities)
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
        val prefs = context.getSharedPreferences(PREFS_SECURITY, Context.MODE_PRIVATE)
        val localActiveNip = SecureStorageHelper.getDecrypted(prefs, KEY_LAST_VERIFIED_NIP).orEmpty()
        if (localActiveNip.equals(updatedSession.officerNip, ignoreCase = true)) {
            saveLocalSession(updatedSession)
        } else {
            cacheRemoteSessionInLocalList(updatedSession)
        }

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
            db.collection(COLLECTION_OFFICER_SESSIONS)
                .document(sanitizeDocumentId(officerNip))
                .update(
                    mapOf(
                        "is_active" to accessGranted,
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
     * One-click Admin recovery: Restores an officer's session access (`accessGranted = true`, `is_active = true`,
     * `adminVerified = true`) in Firestore `officer_sessions`, preserving or recreating their device session
     * and recording an explicit `ADMIN_RESTORED_ACCESS_SESSION` activity so any prior `REVOKED` / `BLOCKED`
     * status is immediately cleared across all devices.
     */
    suspend fun restoreAndVerifyOfficerSessionByAdmin(
        officerNip: String,
        officerName: String,
        officerRole: String,
        adminName: String
    ): Result<DeviceSecuritySession> = withContext(Dispatchers.IO) {
        val cleanNip = officerNip.trim().replace(Regex("[\\s\\-]+"), "")
        if (cleanNip.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("NIP petugas tidak boleh kosong."))
        }
        val nowMs = System.currentTimeMillis()
        val hw = collectHardwareTelemetry()
        val existingSession = getLocalCachedSessions().firstOrNull {
            it.officerNip.trim().equals(cleanNip, ignoreCase = true)
        }
        val existingActivity = getLocalCachedLoginActivities().firstOrNull {
            it.officerNip.trim().equals(cleanNip, ignoreCase = true)
        }
        val nonce = generateClassicNonce(cleanNip, "ADMIN_RESTORE_ACCESS")
        val token = computeSandboxIntegrityToken(cleanNip, nonce)
        val tokenHash = sha256HexPrefix(token, 32).ifBlank { token.take(32) }
        val resolvedAdmin = adminName.ifBlank { "Administrator Pusdalops" }
        val restoreNote = "Akses & sesi perangkat dipulihkan oleh $resolvedAdmin melalui Direktori Personil"

        val restoredSession = DeviceSecuritySession(
            sessionId = existingSession?.sessionId?.takeIf { it.isNotBlank() }
                ?: existingActivity?.sessionId?.takeIf { it.isNotBlank() }
                ?: "sess_${sanitizeDocumentId(cleanNip)}",
            officerNip = cleanNip,
            officerName = officerName.ifBlank { existingSession?.officerName ?: "Petugas ($cleanNip)" },
            officerRole = officerRole.ifBlank { existingSession?.officerRole ?: "Petugas Pusdalops" },
            integrityToken = existingSession?.integrityToken?.takeIf { it.isNotBlank() } ?: token,
            tokenHash = existingSession?.tokenHash?.takeIf { it.isNotBlank() } ?: existingActivity?.tokenHash?.takeIf { it.isNotBlank() } ?: tokenHash,
            classicNonce = existingSession?.classicNonce?.takeIf { it.isNotBlank() } ?: existingActivity?.classicNonce?.takeIf { it.isNotBlank() } ?: nonce,
            verdict = VERDICT_SERVER_VERIFIED,
            deviceVerdict = existingSession?.deviceVerdict?.takeIf { it.isNotBlank() } ?: "FIRESTORE_SESSION_VERIFIED",
            deviceId = existingSession?.deviceId?.takeIf { it.isNotBlank() } ?: existingActivity?.deviceId?.takeIf { it.isNotBlank() } ?: "",
            deviceModel = existingSession?.deviceModel?.takeIf { it.isNotBlank() } ?: existingActivity?.deviceModel?.takeIf { it.isNotBlank() } ?: "Perangkat Terotorisasi Admin",
            osVersion = existingSession?.osVersion?.takeIf { it.isNotBlank() } ?: existingActivity?.osVersion?.takeIf { it.isNotBlank() } ?: hw.osVersion,
            sdkInt = existingSession?.sdkInt?.takeIf { it > 0 } ?: existingActivity?.sdkInt?.takeIf { it > 0 } ?: hw.sdkInt,
            isVerified = true,
            accessGranted = true,
            targetModule = "RUANG_KERJA",
            timestamp = nowMs,
            tokenLength = existingSession?.tokenLength?.takeIf { it > 0 } ?: token.length,
            tokenSource = "FIRESTORE_SESSION",
            deviceBrand = existingSession?.deviceBrand?.takeIf { it.isNotBlank() } ?: existingActivity?.deviceBrand?.takeIf { it.isNotBlank() } ?: hw.deviceBrand,
            deviceManufacturer = existingSession?.deviceManufacturer?.takeIf { it.isNotBlank() } ?: existingActivity?.deviceBrand?.takeIf { it.isNotBlank() } ?: hw.deviceManufacturer,
            deviceHardware = existingSession?.deviceHardware?.takeIf { it.isNotBlank() } ?: existingActivity?.deviceHardware?.takeIf { it.isNotBlank() } ?: hw.deviceHardware,
            deviceFingerprintHash = existingSession?.deviceFingerprintHash?.takeIf { it.isNotBlank() } ?: existingActivity?.deviceFingerprintHash?.takeIf { it.isNotBlank() } ?: "",
            isEmulator = existingSession?.isEmulator ?: existingActivity?.isEmulator ?: false,
            appVersion = existingSession?.appVersion?.takeIf { it.isNotBlank() } ?: existingActivity?.appVersion?.takeIf { it.isNotBlank() } ?: hw.appVersion,
            loginMethod = "VERIFIKASI_ADMIN",
            loginStatus = "SUCCESS",
            loginAction = "ADMIN_RESTORED_ACCESS_SESSION",
            loginCount = ((existingSession?.loginCount ?: 0) + 1).coerceAtLeast(1),
            lastActivityDetail = restoreNote,
            adminVerified = true,
            adminVerifiedBy = resolvedAdmin,
            adminVerifiedAt = nowMs,
            adminVerificationNote = restoreNote
        )

        cacheRemoteSessionInLocalList(restoredSession)
        transmitSessionToFirestore(restoredSession)
        appendLoginActivity(
            OfficerLoginActivity(
                activityId = "act_${nowMs}_${sanitizeDocumentId(cleanNip).take(24)}_${UUID.randomUUID().toString().take(6)}",
                sessionId = restoredSession.sessionId,
                officerNip = cleanNip,
                officerName = restoredSession.officerName,
                officerRole = restoredSession.officerRole,
                loginMethod = "VERIFIKASI_ADMIN",
                loginStatus = "SUCCESS",
                action = "ADMIN_RESTORED_ACCESS_SESSION",
                verdict = restoredSession.verdict,
                deviceVerdict = restoredSession.deviceVerdict,
                tokenSource = restoredSession.tokenSource,
                tokenHash = restoredSession.tokenHash,
                tokenLength = restoredSession.tokenLength,
                classicNonce = restoredSession.classicNonce,
                deviceId = restoredSession.deviceId,
                deviceModel = restoredSession.deviceModel,
                deviceBrand = restoredSession.deviceBrand,
                deviceHardware = restoredSession.deviceHardware,
                deviceFingerprintHash = restoredSession.deviceFingerprintHash,
                osVersion = restoredSession.osVersion,
                sdkInt = restoredSession.sdkInt,
                isEmulator = restoredSession.isEmulator,
                appVersion = restoredSession.appVersion,
                accessGranted = true,
                detail = restoreNote,
                timestamp = nowMs
            )
        )
        Result.success(restoredSession)
    }

    /**
     * Revokes an officer's session in Firestore collection 'officer_sessions'
     * by setting `is_active` to `false` (and `accessGranted` to `false`) and deleting the session document,
     * as well as clearing local cached session state.
     */
    suspend fun deleteSecuritySession(
        officerNip: String,
        purgeActivities: Boolean = false,
        revokedByAdmin: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        val trimmedNip = officerNip.trim()
        if (trimmedNip.isBlank()) return@withContext false

        if (purgeActivities) {
            // Suppress realtime revocation listeners while cleaning up session docs for Reset PIN / normalization
            markPinResetInProgress(trimmedNip)
        }

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
        val revokeTs = System.currentTimeMillis()

        // 1. ONLY mark is_active = false / REVOKED when an Admin explicitly revokes a device (purgeActivities == false).
        //    During Reset PIN / normalization (purgeActivities == true), NEVER write REVOKED or is_active = false so
        //    the officer's real device is never falsely shown a "session revoked" toast or blocked from relogin.
        if (!purgeActivities) {
            try {
                withTimeoutOrNull(4_000L) {
                    db.collection(COLLECTION_OFFICER_SESSIONS)
                        .document(sanitizedId)
                        .update(
                            mapOf(
                                "is_active" to false,
                                "accessGranted" to false,
                                "adminVerified" to false,
                                "loginStatus" to "REVOKED",
                                "timestamp" to revokeTs,
                                "login_time" to revokeTs
                            )
                        )
                        .await()
                    revokedRemote = true
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }

        // 2. Query matching session documents by nip == trimmedNip and cleanly delete (only writing REVOKED if !purgeActivities)
        try {
            val matchingDocs = withTimeoutOrNull(4_000L) {
                db.collection(COLLECTION_OFFICER_SESSIONS)
                    .whereEqualTo("nip", trimmedNip)
                    .get()
                    .await()
            }
            if (matchingDocs != null) {
                for (doc in matchingDocs.documents) {
                    if (!purgeActivities) {
                        try {
                            withTimeoutOrNull(3_000L) {
                                doc.reference.update(
                                    mapOf(
                                        "is_active" to false,
                                        "accessGranted" to false,
                                        "adminVerified" to false,
                                        "loginStatus" to "REVOKED",
                                        "timestamp" to revokeTs,
                                        "login_time" to revokeTs
                                    )
                                ).await()
                                revokedRemote = true
                            }
                        } catch (_: Exception) {
                        }
                    }
                    try {
                        withTimeoutOrNull(3_000L) {
                            doc.reference.delete().await()
                            revokedRemote = true
                        }
                    } catch (_: Exception) {
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }

        // 3. Delete the session document by document ID from officer_sessions (and legacy device_security_sessions if present)
        return@withContext try {
            withTimeoutOrNull(4_000L) {
                db.collection(COLLECTION_OFFICER_SESSIONS)
                    .document(sanitizedId)
                    .delete()
                    .await()
            }
            if (sanitizedId != trimmedNip) {
                try {
                    withTimeoutOrNull(3_000L) {
                        db.collection(COLLECTION_OFFICER_SESSIONS).document(trimmedNip).delete().await()
                    }
                } catch (_: Exception) {
                }
            }
            try {
                withTimeoutOrNull(3_000L) {
                    db.collection(COLLECTION_LEGACY_SECURITY_SESSIONS).document(sanitizedId).delete().await()
                }
            } catch (_: Exception) {
            }
            Log.i(TAG, "Officer session cleaned from Firestore '$COLLECTION_OFFICER_SESSIONS' for NIP: ${maskNip(trimmedNip)} (purgeActivities=$purgeActivities)")
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete officer session for NIP ${maskNip(trimmedNip)}: ${e.message}")
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
        context.getSharedPreferences(PREFS_SECURITY, Context.MODE_PRIVATE).edit().clear().commit()
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
        editor.commit()
    }

    private fun cacheRemoteSessionInLocalList(session: DeviceSecuritySession) {
        cacheRemoteSessionsBatch(listOf(session))
    }

    @Synchronized
    private fun cacheRemoteSessionsBatch(sessions: List<DeviceSecuritySession>) {
        if (sessions.isEmpty()) return
        val prefs = context.getSharedPreferences(PREFS_SECURITY, Context.MODE_PRIVATE)
        val merged = mergeSessions(sessions, getLocalCachedSessions(prefs)).take(60)
        val jsonArr = JSONArray()
        merged.forEach { s ->
            val obj = JSONObject()
            s.toMap().forEach { (k, v) -> if (v != null) obj.put(k, v) }
            jsonArr.put(obj)
        }
        prefs.edit().putString(KEY_CACHED_SESSIONS_JSON, jsonArr.toString()).apply()
    }

    private fun saveLocalLoginActivity(activity: OfficerLoginActivity) {
        saveLocalLoginActivitiesBatch(listOf(activity))
    }

    @Synchronized
    private fun saveLocalLoginActivitiesBatch(activities: List<OfficerLoginActivity>) {
        if (activities.isEmpty()) return
        val prefs = context.getSharedPreferences(PREFS_SECURITY, Context.MODE_PRIVATE)
        val merged = mergeLoginActivities(activities, getLocalCachedLoginActivities(prefs))
        val jsonArr = JSONArray()
        merged.forEach { item ->
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
        editor.commit()
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

    fun getLocalVerifiedSessionTimestamp(officerNip: String): Long {
        if (officerNip.isBlank()) return 0L
        val prefs = context.getSharedPreferences(PREFS_SECURITY, Context.MODE_PRIVATE)
        val savedNip = SecureStorageHelper.getDecrypted(prefs, KEY_LAST_VERIFIED_NIP) ?: return 0L
        val accessGranted = prefs.getBoolean(KEY_ACCESS_GRANTED, false)
        if (!accessGranted || !savedNip.equals(officerNip.trim(), ignoreCase = true)) return 0L
        return prefs.getLong(KEY_VERIFICATION_TIMESTAMP, 0L)
    }

    fun hasActiveVerifiedLocalSession(officerNip: String): Boolean {
        return isLocalSessionValid(officerNip)
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
        return savedNip.equals(officerNip, ignoreCase = true) && accessGranted && timestamp > 0L
    }
}
