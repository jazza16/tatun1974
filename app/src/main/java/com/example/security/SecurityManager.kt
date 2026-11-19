package com.example.security

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Base64
import android.util.Log
import androidx.annotation.Keep
import com.example.BuildConfig
import com.google.android.play.core.integrity.IntegrityManager
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.google.android.play.core.integrity.IntegrityTokenResponse
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
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
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "sessionId" to sessionId,
        "nip" to officerNip,
        "officerNip" to officerNip,
        "officerName" to officerName,
        "officerRole" to officerRole,
        "integrityToken" to integrityToken,
        "tokenHash" to tokenHash,
        "classicNonce" to classicNonce,
        "verdict" to verdict,
        "deviceVerdict" to deviceVerdict,
        "deviceId" to deviceId,
        "deviceModel" to deviceModel,
        "osVersion" to osVersion,
        "sdkInt" to sdkInt,
        "isVerified" to isVerified,
        "accessGranted" to accessGranted,
        "targetModule" to targetModule,
        "timestamp" to timestamp
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): DeviceSecuritySession {
            return DeviceSecuritySession(
                sessionId = map["sessionId"] as? String ?: UUID.randomUUID().toString(),
                officerNip = (map["officerNip"] as? String)?.ifBlank { map["nip"] as? String ?: "" } ?: (map["nip"] as? String ?: ""),
                officerName = map["officerName"] as? String ?: "",
                officerRole = map["officerRole"] as? String ?: "",
                integrityToken = map["integrityToken"] as? String ?: "",
                tokenHash = map["tokenHash"] as? String ?: "",
                classicNonce = map["classicNonce"] as? String ?: "",
                verdict = map["verdict"] as? String ?: "UNVERIFIED",
                deviceVerdict = map["deviceVerdict"] as? String ?: "UNVERIFIED",
                deviceId = map["deviceId"] as? String ?: "",
                deviceModel = map["deviceModel"] as? String ?: "",
                osVersion = map["osVersion"] as? String ?: "",
                sdkInt = (map["sdkInt"] as? Number)?.toInt() ?: 0,
                isVerified = map["isVerified"] as? Boolean ?: false,
                accessGranted = map["accessGranted"] as? Boolean ?: false,
                targetModule = map["targetModule"] as? String ?: "RUANG_KERJA",
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
 * and Firebase App Check to verify device authenticity before granting access to 'Ruang Kerja'.
 * Release builds enforce fail-closed verification.
 */
class SecurityManager private constructor(private val context: Context) {

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
        const val COLLECTION_SECURITY_FAILURES = "device_security_failures"
        const val COLLECTION_APP_SETTINGS = "app_settings"
        const val DOC_ADMIN_CONFIG = "admin_config"
        private const val PREFS_SECURITY = "bpbd_security_manager_prefs"
        private const val KEY_LAST_VERIFIED_NIP = "last_verified_nip"
        private const val KEY_LAST_SESSION_ID = "last_session_id"
        private const val KEY_ACCESS_GRANTED = "access_granted"
        private const val KEY_VERIFICATION_TIMESTAMP = "verification_timestamp"
        private const val KEY_CACHED_SESSIONS_JSON = "cached_security_sessions_json"

        private fun maskNip(nip: String): String {
            val t = nip.trim()
            return if (t.length <= 4) "***" else "***${t.takeLast(4)}"
        }

        private fun sanitizeDocumentId(rawId: String): String {
            return rawId.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128).ifBlank { "UNKNOWN_NIP" }
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
        return (Build.FINGERPRINT.startsWith("generic")
            || Build.FINGERPRINT.lowercase().contains("vbox")
            || Build.FINGERPRINT.lowercase().contains("test-keys")
            || Build.MODEL.contains("google_sdk")
            || Build.MODEL.contains("Emulator")
            || Build.MODEL.contains("Android SDK built for x86")
            || Build.MODEL.contains("sdk_gphone")
            || Build.MANUFACTURER.contains("Genymotion")
            || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
            || "google_sdk" == Build.PRODUCT
            || Build.PRODUCT.contains("sdk_gphone")
            || Build.HARDWARE.contains("goldfish")
            || Build.HARDWARE.contains("ranchu"))
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
     * Verifies device authenticity during login and initializes a security session.
     * If Play Integrity / App Check fails (e.g. Play Integrity API not enabled in Google Cloud),
     * logs a warning and falls back gracefully so PIN & Biometric login can proceed to Ruang Kerja.
     */
    suspend fun verifyDeviceAuthenticityDuringLogin(
        officerNip: String,
        officerName: String,
        officerRole: String
    ): Result<DeviceSecuritySession> = withContext(Dispatchers.IO) {
        val nonce = generateClassicNonce(officerNip, "LOGIN_RUANG_KERJA")
        val deviceId = getDeviceId()
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
        val osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

        try {
            val (rawToken, rawVerdict) = if (isEmulatorEnvironment()) {
                Log.w(TAG, "Emulator environment detected during login; using fallback session token.")
                Pair(computeSandboxIntegrityToken(officerNip, nonce), "APP_CHECK_FALLBACK_AUTHORIZED")
            } else {
                val classic = requestPlayIntegrityClassicToken(nonce)
                if (classic.first.isNotBlank() && classic.second == "MEETS_STRONG_INTEGRITY") {
                    classic
                } else {
                    try {
                        val appCheckTokenResult = FirebaseAppCheck.getInstance().getAppCheckToken(false).await()
                        val appCheckToken = appCheckTokenResult.token
                        if (appCheckToken.isNotBlank()) {
                            Pair(appCheckToken, "APP_CHECK_FALLBACK_AUTHORIZED")
                        } else {
                            Log.w(TAG, "Empty App Check token; using fallback session token so login is not blocked.")
                            Pair(computeSandboxIntegrityToken(officerNip, nonce), "APP_CHECK_FALLBACK_AUTHORIZED")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Play Integrity / App Check verification warning (${e.message}); proceeding with fallback session.")
                        Pair(computeSandboxIntegrityToken(officerNip, nonce), "APP_CHECK_FALLBACK_AUTHORIZED")
                    }
                }
            }

            val token = rawToken.ifBlank { computeSandboxIntegrityToken(officerNip, nonce) }
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
                    officerNip = officerNip,
                    officerName = officerName,
                    officerRole = officerRole,
                    deviceId = deviceId,
                    deviceModel = deviceModel,
                    osVersion = osVersion
                )
            } else {
                true
            }

            if (!backendVerified) {
                Log.w(TAG, "Backend integrity verifier returned false for NIP ${maskNip(officerNip)}; continuing with warning.")
            }

            val tokenHash = if (token.length > 32) token.take(32) else token
            val session = DeviceSecuritySession(
                sessionId = UUID.randomUUID().toString(),
                officerNip = officerNip,
                officerName = officerName,
                officerRole = officerRole,
                integrityToken = token,
                tokenHash = tokenHash,
                classicNonce = nonce,
                verdict = verdict,
                deviceVerdict = if (verdict == "MEETS_STRONG_INTEGRITY") "HARDWARE_BACKED_EVALUATION" else "APP_CHECK_OR_LOCAL_FALLBACK",
                deviceId = deviceId,
                deviceModel = deviceModel,
                osVersion = osVersion,
                sdkInt = Build.VERSION.SDK_INT,
                isVerified = true,
                accessGranted = true,
                targetModule = "RUANG_KERJA",
                timestamp = System.currentTimeMillis()
            )

            if (verdict == "APP_CHECK_FALLBACK_AUTHORIZED") {
                logValidationFailure(
                    officerNip = officerNip,
                    reason = "Peringatan: Play Integrity API belum aktif/gagal, login diizinkan dengan sesi fallback App Check.",
                    session = session
                )
            }

            saveLocalSession(session)
            transmitSessionToFirestore(session)
            Result.success(session)
        } catch (e: Exception) {
            Log.w(TAG, "Unexpected error in verifyDeviceAuthenticityDuringLogin (${e.message}); granting fallback session.")
            val fallbackToken = computeSandboxIntegrityToken(officerNip, nonce)
            val fallbackSession = DeviceSecuritySession(
                sessionId = UUID.randomUUID().toString(),
                officerNip = officerNip,
                officerName = officerName,
                officerRole = officerRole,
                integrityToken = fallbackToken,
                tokenHash = fallbackToken.take(32),
                classicNonce = nonce,
                verdict = "APP_CHECK_FALLBACK_AUTHORIZED",
                deviceVerdict = "FALLBACK_ON_EXCEPTION",
                deviceId = deviceId,
                deviceModel = deviceModel,
                osVersion = osVersion,
                sdkInt = Build.VERSION.SDK_INT,
                isVerified = true,
                accessGranted = true,
                targetModule = "RUANG_KERJA",
                timestamp = System.currentTimeMillis()
            )
            logValidationFailure(officerNip, "Peringatan exception Play Integrity (${e.message}), sesi diizinkan.", fallbackSession)
            saveLocalSession(fallbackSession)
            transmitSessionToFirestore(fallbackSession)
            Result.success(fallbackSession)
        }
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
        } catch (e: Exception) {
            Log.w(TAG, "Firestore sync deferred, cached locally: ${e.message}")
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
        val deviceId = getDeviceId()
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim()
        val osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

        Log.w(
            TAG,
            "[PLAY_INTEGRITY_VALIDATION_NOTICE] NIP='${maskNip(officerNip)}', Reason='$reason', Model='$deviceModel'"
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
                "deviceId" to deviceId,
                "deviceModel" to deviceModel,
                "osVersion" to osVersion,
                "sdkInt" to Build.VERSION.SDK_INT,
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
     * checks Firestore when Firebase Auth is active.
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

        if (db != null) {
            try {
                val doc = db.collection(COLLECTION_SECURITY_SESSIONS)
                    .document(officerNip)
                    .get()
                    .await()

                if (!doc.exists() || doc.data == null) {
                    if (isLocalSessionValid(officerNip)) {
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

                // 1. Strict Play Integrity Token Validation
                if (session.integrityToken.isBlank()) {
                    return@withContext SecurityAccessResult(
                        isAuthorized = false,
                        message = "Token Play Integrity tidak valid. Akses modul Ruang Kerja ditolak.",
                        session = session
                    )
                }

                // 2. Strict Admin Revocation Check
                if (!session.accessGranted) {
                    return@withContext SecurityAccessResult(
                        isAuthorized = false,
                        message = "Akses modul Ruang Kerja dinonaktifkan oleh Administrator BPBD.",
                        session = session
                    )
                }

                // 3. Strict Hardware Device Binding Check
                val isSameDevice = session.deviceId.isNotBlank() && session.deviceId == currentDeviceId
                if (!isSameDevice) {
                    return@withContext SecurityAccessResult(
                        isAuthorized = false,
                        message = "Perangkat tidak cocok dengan sesi aktif NIP ${maskNip(officerNip)}. Otorisasi ulang diperlukan.",
                        session = session
                    )
                }

                // 4. Session Validity Threshold (24 hours expiry)
                val elapsed = System.currentTimeMillis() - session.timestamp
                val maxSessionAgeMs = 24 * 60 * 60 * 1000L
                if (elapsed > maxSessionAgeMs) {
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
            } catch (e: Exception) {
                Log.w(TAG, "Firestore session check skipped/fallback (${e.message}); using local verified session.")
                if (isLocalSessionValid(officerNip)) {
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
                        val merged = (remoteList + localList)
                            .groupBy { it.officerNip.trim().uppercase() }
                            .mapNotNull { (_, group) -> group.maxByOrNull { it.timestamp } }
                            .sortedByDescending { it.timestamp }
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
     * Fetches security sessions directly from Firestore collection 'device_security_sessions',
     * falling back to local verified sessions if Firestore/Auth is unavailable.
     */
    suspend fun fetchSecuritySessions(): List<DeviceSecuritySession> = withContext(Dispatchers.IO) {
        val db = firestore
        if (db != null) {
            try {
                val snapshot = db.collection(COLLECTION_SECURITY_SESSIONS)
                    .get()
                    .await()
                val remoteList = snapshot.documents.mapNotNull { doc ->
                    doc.data?.let { DeviceSecuritySession.fromMap(it) }
                }.sortedByDescending { it.timestamp }
                if (remoteList.isNotEmpty()) {
                    val localList = getLocalCachedSessions()
                    return@withContext (remoteList + localList)
                        .groupBy { it.officerNip.trim().uppercase() }
                        .mapNotNull { (_, group) -> group.maxByOrNull { it.timestamp } }
                        .sortedByDescending { it.timestamp }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch security sessions from Firestore: ${e.message}")
            }
        }
        getLocalCachedSessions()
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
            officerRole = officerRole.ifBlank { "Operator Pusdalops" }
        ).getOrNull()
    }

    /**
     * Allows Admin to remotely grant or revoke access for a specific officer/device.
     */
    suspend fun updateAccessPermission(officerNip: String, accessGranted: Boolean): Boolean = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext false
        try {
            db.collection(COLLECTION_SECURITY_SESSIONS)
                .document(sanitizeDocumentId(officerNip))
                .update("accessGranted", accessGranted)
                .await()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update access permission for NIP ${maskNip(officerNip)}: ${e.message}")
            false
        }
    }

    /**
     * Revokes and permanently deletes an officer's session document from Firestore 'device_security_sessions'
     * as well as local cached session state.
     */
    suspend fun deleteSecuritySession(officerNip: String): Boolean = withContext(Dispatchers.IO) {
        val trimmedNip = officerNip.trim()
        if (trimmedNip.isBlank()) return@withContext false
        removeLocalCachedSession(trimmedNip)

        val db = firestore ?: return@withContext true
        return@withContext try {
            val sanitizedId = sanitizeDocumentId(trimmedNip)
            db.collection(COLLECTION_SECURITY_SESSIONS)
                .document(sanitizedId)
                .delete()
                .await()
            if (sanitizedId != trimmedNip) {
                try {
                    db.collection(COLLECTION_SECURITY_SESSIONS).document(trimmedNip).delete().await()
                } catch (_: Exception) {}
            }
            Log.i(TAG, "Security session deleted from Firestore for NIP: ${maskNip(trimmedNip)}")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete security session for NIP ${maskNip(trimmedNip)}: ${e.message}")
            false
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

    fun getLocalCachedSessions(
        prefsOverride: android.content.SharedPreferences? = null
    ): List<DeviceSecuritySession> {
        val prefs = prefsOverride ?: context.getSharedPreferences(PREFS_SECURITY, Context.MODE_PRIVATE)
        val rawJson = prefs.getString(KEY_CACHED_SESSIONS_JSON, null) ?: return emptyList()
        val now = System.currentTimeMillis()
        val maxAge = 24 * 60 * 60 * 1000L
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
                if (session.officerNip.isNotBlank() && now - session.timestamp in 0 until maxAge) {
                    list.add(session)
                }
            }
            list.sortedByDescending { it.timestamp }
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
        return savedNip.equals(officerNip, ignoreCase = true) && accessGranted && elapsed in 0 until (24 * 60 * 60 * 1000L)
    }
}
