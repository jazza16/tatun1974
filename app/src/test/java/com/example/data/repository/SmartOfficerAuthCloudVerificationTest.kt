package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.CloudCredentialLookupResult
import com.example.data.local.FirestoreHelper
import com.example.data.local.OfficerAuthManager
import com.example.data.local.OfficerCredentialEntity
import com.example.data.local.OfficerRoles
import com.example.security.SecureStorageHelper
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SmartOfficerAuthCloudVerificationTest {

    private lateinit var context: Context
    private val testNip = "197906072006041011"
    private val testName = "RURI IRWANDI, ST, MT"
    private val testCustomPin = "849201"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        FirestoreHelper.resetTestHooks()
        SmartOfficerAuthRepository.clearFailedAttemptsForTesting()
        context.getSharedPreferences("bpbd_officer_auth_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("PusdalopsSession", Context.MODE_PRIVATE).edit().clear().commit()
        runBlocking {
            AppDatabase.getInstance(context).clearAllTables()
        }
    }

    @After
    fun tearDown() {
        FirestoreHelper.resetTestHooks()
        SmartOfficerAuthRepository.clearFailedAttemptsForTesting()
    }

    @Test
    fun `fresh install strictly rejects default PIN 117 and never seeds local default credentials`() = runBlocking {
        var cloudCalled = false
        FirestoreHelper.overrideCloudCredentialFetcherForTesting = {
            cloudCalled = true
            CloudCredentialLookupResult.NotFound
        }

        val repo = SmartOfficerAuthRepository(context)
        val authManager = OfficerAuthManager(context)

        val result117 = repo.authenticateOfficer(nip = testNip, inputPin = "117")
        assertTrue("Default PIN '117' must always be rejected", result117.isFailure)
        assertFalse("Default PIN '117' must be blocked before calling Cloud", cloudCalled)
        assertEquals("", authManager.getSavedNip())
        assertFalse(authManager.isOfficerLoggedIn())

        val unknownNip = "199901012026011999"
        FirestoreHelper.overrideCloudCredentialWriterForTesting = {
            Result.failure(IllegalStateException("Cloud offline / unauthorized"))
        }

        // Unregistered NIP with 6-digit suffix when Cloud has no record -> must FAIL and never seed locally
        val resultUnknownNip = repo.authenticateOfficer(nip = unknownNip, inputPin = unknownNip.takeLast(6))
        assertTrue("Unregistered NIP must fail without seeding default PIN locally", resultUnknownNip.isFailure)
        assertTrue("Cloud verification must have been invoked", cloudCalled)
        assertEquals("", authManager.getSavedNip())

        // Even official ASN NIP with 6-digit PIN when Cloud write rejects/offline -> must FAIL and never seed locally
        val resultOfficialBlocked = repo.authenticateOfficer(nip = testNip, inputPin = testNip.takeLast(6))
        assertTrue("Official NIP must fail if Cloud activation write fails", resultOfficialBlocked.isFailure)
        assertEquals("", authManager.getSavedNip())
        assertTrue(
            "Local Room officer_credentials must NOT be auto-seeded with default PINs",
            AppDatabase.getInstance(context).credentialDao().getAllCredentialsList().isEmpty()
        )
    }

    @Test
    fun `cloud verification failure never falls back to local default PIN`() = runBlocking {
        FirestoreHelper.overrideCloudCredentialFetcherForTesting = {
            CloudCredentialLookupResult.Error(
                userMessage = "Koneksi internet bermasalah (UNAVAILABLE).",
                errorCode = "UNAVAILABLE"
            )
        }

        val repo = SmartOfficerAuthRepository(context)
        val authManager = OfficerAuthManager(context)

        val result = repo.authenticateOfficer(nip = testNip, inputPin = testCustomPin)
        assertTrue("Login must fail when Cloud Firestore is unavailable", result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("UNAVAILABLE") == true)
        assertEquals("Failed Cloud verification must NOT save NIP locally", "", authManager.getSavedNip())
        assertFalse(authManager.isOfficerLoggedIn())
    }

    @Test
    fun `successful cloud verification saves ONLY NIP locally and NEVER stores raw PIN or hash on phone`() = runBlocking {
        val salt = OfficerCredentialEntity.generateCryptographicSalt()
        val hash = OfficerCredentialEntity.hashWithSalt(testCustomPin, salt)
        val cloudRecord = OfficerCredentialEntity(
            nip = testNip,
            fullName = testName,
            role = OfficerRoles.PENGAWAS,
            unit = "BPBD Kab. Mukomuko",
            shift = "Shift Siaga 24 Jam",
            pinHash = hash,
            salt = salt,
            isCustomPinSet = true,
            initialSecurePin = "",
            isSyncedWithCloud = true
        )

        FirestoreHelper.overrideCloudCredentialFetcherForTesting = { requestedNip ->
            if (requestedNip == testNip) {
                CloudCredentialLookupResult.Found(cloudRecord)
            } else {
                CloudCredentialLookupResult.NotFound
            }
        }
        FirestoreHelper.overrideCloudCredentialWriterForTesting = { Result.success(Unit) }

        val repo = SmartOfficerAuthRepository(context)
        val authManager = OfficerAuthManager(context)

        // Wrong PIN must fail and not save NIP
        val wrongResult = repo.authenticateOfficer(nip = testNip, inputPin = "999999")
        assertTrue("Wrong PIN must fail", wrongResult.isFailure)
        assertEquals("", authManager.getSavedNip())

        // Correct PIN verified against Cloud succeeds
        val okResult = repo.authenticateOfficer(nip = testNip, inputPin = testCustomPin)
        assertTrue("Valid Cloud PIN must succeed: ${okResult.exceptionOrNull()?.message}", okResult.isSuccess)
        assertEquals(testNip, authManager.getSavedNip())
        assertTrue(authManager.isOfficerLoggedIn())
        assertTrue(authManager.hasCustomPin())

        // CRITICAL SECURITY CHECK: NO PIN, PIN hash, or PIN salt may exist anywhere in SharedPreferences!
        val rawPrefs = context.getSharedPreferences("bpbd_officer_auth_prefs", Context.MODE_PRIVATE).all
        assertFalse("Raw PIN key must not exist", rawPrefs.containsKey("officer_pin"))
        assertFalse("PIN hash key must not exist", rawPrefs.containsKey("officer_pin_hash"))
        assertFalse("PIN salt key must not exist", rawPrefs.containsKey("officer_pin_salt"))
        rawPrefs.values.forEach { value ->
            val str = value?.toString().orEmpty()
            assertFalse("Raw PIN must NEVER appear in SharedPreferences", str.contains(testCustomPin))
            assertFalse("PIN hash must NEVER appear in SharedPreferences", str.contains(hash))
        }

        // CRITICAL SECURITY CHECK: Local Room cache must also NOT store PIN hash, salt, or initialSecurePin!
        val cachedRoom = AppDatabase.getInstance(context).credentialDao().getCredentialByNip(testNip)
        assertEquals("", cachedRoom?.pinHash.orEmpty())
        assertEquals("", cachedRoom?.salt.orEmpty())
        assertEquals("", cachedRoom?.initialSecurePin.orEmpty())
    }

    @Test
    fun `PIN change persists to Cloud and survives simulated reinstall without reverting to default`() = runBlocking {
        val initialPin = "551199"
        val newPin = "739145"
        val initialSalt = OfficerCredentialEntity.generateCryptographicSalt()
        var cloudDoc = OfficerCredentialEntity(
            nip = testNip,
            fullName = testName,
            role = OfficerRoles.OPERATOR,
            pinHash = OfficerCredentialEntity.hashWithSalt(initialPin, initialSalt),
            salt = initialSalt,
            isCustomPinSet = true,
            initialSecurePin = "",
            isSyncedWithCloud = true
        )

        FirestoreHelper.overrideCloudCredentialFetcherForTesting = { nip ->
            if (nip == testNip) CloudCredentialLookupResult.Found(cloudDoc) else CloudCredentialLookupResult.NotFound
        }
        FirestoreHelper.overrideCloudCredentialWriterForTesting = { updated ->
            cloudDoc = updated
            Result.success(Unit)
        }

        val repo = SmartOfficerAuthRepository(context)
        val changeRes = repo.changePersonalPin(nip = testNip, currentPin = initialPin, newPin = newPin)
        assertTrue("PIN change must succeed on Cloud: ${changeRes.exceptionOrNull()?.message}", changeRes.isSuccess)

        // Simulate app reinstall / full local wipe
        context.getSharedPreferences("bpbd_officer_auth_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        AppDatabase.getInstance(context).clearAllTables()

        // After reinstall: default '117' and old PIN MUST fail; ONLY the new Cloud PIN succeeds!
        val freshRepo = SmartOfficerAuthRepository(context)
        assertTrue(freshRepo.authenticateOfficer(testNip, "117").isFailure)
        assertTrue(freshRepo.authenticateOfficer(testNip, initialPin).isFailure)
        assertTrue(freshRepo.authenticateOfficer(testNip, testNip.takeLast(6)).isFailure)

        val loginNewPin = freshRepo.authenticateOfficer(testNip, newPin)
        assertTrue("New Cloud PIN must work immediately after reinstall", loginNewPin.isSuccess)
        assertEquals(testNip, OfficerAuthManager(context).getSavedNip())
    }

    @Test
    fun `app update migration preserves saved_nip and officer session while purging legacy local PIN keys`() {
        val prefs = context.getSharedPreferences("bpbd_officer_auth_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        // Simulate older app version state prior to update (schema version 0 with legacy local PIN keys)
        editor.putBoolean("is_officer_logged_in", true)
        SecureStorageHelper.putEncrypted(editor, "saved_nip", testNip)
        SecureStorageHelper.putEncrypted(editor, "officer_badge_id", testNip)
        SecureStorageHelper.putEncrypted(editor, "officer_name", testName)
        SecureStorageHelper.putEncrypted(editor, "officer_role", OfficerRoles.PENGAWAS)
        SecureStorageHelper.putEncrypted(editor, "officer_shift", "Shift Siaga 24 Jam")
        editor.putString("officer_pin", "117")
        editor.putString("officer_pin_hash", "legacy_hash_value")
        editor.putString("officer_pin_salt", "legacy_salt_value")
        editor.commit()

        // Instantiate updated OfficerAuthManager (simulates launching newly updated APK)
        val upgradedAuthManager = OfficerAuthManager(context)

        // Must PRESERVE saved_nip & active officer session intact!
        assertTrue("Officer session must survive app update", upgradedAuthManager.isOfficerLoggedIn())
        assertEquals("saved_nip must survive app update", testNip, upgradedAuthManager.getSavedNip())
        assertEquals("Officer name must survive app update", testName, upgradedAuthManager.getOfficerName())
        assertEquals("Officer role must survive app update", OfficerRoles.PENGAWAS, upgradedAuthManager.getOfficerRole())
        assertTrue("Custom PIN status boolean should be preserved", upgradedAuthManager.hasCustomPin())

        // Must PURGE all legacy local PIN keys!
        assertFalse("Legacy plaintext officer_pin must be purged", prefs.contains("officer_pin"))
        assertFalse("Legacy officer_pin_hash must be purged", prefs.contains("officer_pin_hash"))
        assertFalse("Legacy officer_pin_salt must be purged", prefs.contains("officer_pin_salt"))
    }

    @Test
    fun `combined Nama and NIP input variants resolve accurately and authenticate against Cloud`() = runBlocking {
        val yudhaNip = "199807202025061002"
        val yudhaPin = "664422"
        val salt = OfficerCredentialEntity.generateCryptographicSalt()
        val yudhaCloudDoc = OfficerCredentialEntity(
            nip = yudhaNip,
            fullName = "YUDHA SOFRIANTO,S.A.P",
            role = OfficerRoles.OPERATOR,
            pinHash = OfficerCredentialEntity.hashWithSalt(yudhaPin, salt),
            salt = salt,
            isCustomPinSet = true,
            initialSecurePin = "",
            isSyncedWithCloud = true
        )

        FirestoreHelper.overrideCloudCredentialFetcherForTesting = { reqNip ->
            if (reqNip == yudhaNip) CloudCredentialLookupResult.Found(yudhaCloudDoc)
            else CloudCredentialLookupResult.NotFound
        }
        FirestoreHelper.overrideCloudCredentialWriterForTesting = { Result.success(Unit) }

        val repo = SmartOfficerAuthRepository(context)

        // 1. Combined "Nama, NIP" input
        val combinedRes = repo.authenticateByNipOrNameAndPin("Yudha Sofrianto, $yudhaNip", yudhaPin)
        assertTrue("Combined 'Nama, NIP' input must authenticate: ${combinedRes.exceptionOrNull()?.message}", combinedRes.isSuccess)
        assertEquals(yudhaNip, combinedRes.getOrThrow().nip)

        // 2. Punctuation variant name ("Yudha Sofrianto, S.A.P" with space after comma)
        val nameVariantRes = repo.authenticateByNipOrNameAndPin("Yudha Sofrianto, S.A.P", yudhaPin)
        assertTrue("Punctuation-variant name must authenticate: ${nameVariantRes.exceptionOrNull()?.message}", nameVariantRes.isSuccess)
        assertEquals(yudhaNip, nameVariantRes.getOrThrow().nip)

        // 3. Spaced NIP input
        val spacedNipRes = repo.authenticateByNipOrNameAndPin("19980720 202506 1 002", yudhaPin)
        assertTrue("Spaced NIP must authenticate: ${spacedNipRes.exceptionOrNull()?.message}", spacedNipRes.isSuccess)
        assertEquals(yudhaNip, spacedNipRes.getOrThrow().nip)
    }

    @Test
    fun `first time Cloud activation for official ASN officer requires Cloud write and rejects default 117`() = runBlocking {
        var cloudWrittenCred: OfficerCredentialEntity? = null
        FirestoreHelper.overrideCloudCredentialFetcherForTesting = { reqNip ->
            val saved = cloudWrittenCred
            if (saved != null && saved.nip == reqNip) CloudCredentialLookupResult.Found(saved)
            else CloudCredentialLookupResult.NotFound
        }
        FirestoreHelper.overrideCloudCredentialWriterForTesting = { cred ->
            cloudWrittenCred = cred
            Result.success(Unit)
        }

        val repo = SmartOfficerAuthRepository(context)
        val authManager = OfficerAuthManager(context)

        // Default '117' must still be blocked
        assertTrue(repo.authenticateOfficer(testNip, "117").isFailure)

        // Official ASN officer activates on Cloud using unique 6-digit NIP when Cloud returns NotFound
        val activationRes = repo.authenticateByNipOrNameAndPin("Ruri Irwandi, $testNip", testNip.takeLast(6))
        assertTrue("Official ASN first-time Cloud activation should succeed: ${activationRes.exceptionOrNull()?.message}", activationRes.isSuccess)
        assertTrue("Credential hash must be persisted to Cloud Firestore", cloudWrittenCred != null && cloudWrittenCred!!.pinHash.isNotBlank())
        assertEquals("Only NIP is saved locally", testNip, authManager.getSavedNip())
        assertFalse("Raw PIN must not be stored in SharedPreferences", context.getSharedPreferences("bpbd_officer_auth_prefs", Context.MODE_PRIVATE).contains("officer_pin"))
    }

    @Test
    fun `biometric authentication resolves combined Nama or NIP and establishes session without local PIN`() = runBlocking {
        val salt = OfficerCredentialEntity.generateCryptographicSalt()
        val cloudRecord = OfficerCredentialEntity(
            nip = testNip,
            fullName = testName,
            role = OfficerRoles.PENGAWAS,
            pinHash = OfficerCredentialEntity.hashWithSalt(testCustomPin, salt),
            salt = salt,
            isCustomPinSet = true,
            initialSecurePin = "",
            isSyncedWithCloud = true
        )
        FirestoreHelper.overrideCloudCredentialFetcherForTesting = { reqNip ->
            if (reqNip == testNip) CloudCredentialLookupResult.Found(cloudRecord)
            else CloudCredentialLookupResult.NotFound
        }
        FirestoreHelper.overrideCloudCredentialWriterForTesting = { Result.success(Unit) }

        val repo = SmartOfficerAuthRepository(context)
        val authManager = OfficerAuthManager(context)

        val bioRes = repo.authenticateWithBiometric("Ruri Irwandi, $testNip")
        assertTrue("Biometric auth must succeed: ${bioRes.exceptionOrNull()?.message}", bioRes.isSuccess)
        assertTrue(authManager.isOfficerLoggedIn())
        assertEquals(testNip, authManager.getSavedNip())
        assertFalse(context.getSharedPreferences("bpbd_officer_auth_prefs", Context.MODE_PRIVATE).contains("officer_pin"))
    }

    @Test
    fun `HITATUN ARAZAK blank-hash Cloud doc activates with 041006, preserves ownerUid, and handles name variants`() = runBlocking {
        val hitatunNip = "197401282006041006"
        val hitatunPin6 = "041006"
        val adminUid = "admin_google_uid_bpbd"
        var syncedRecord: OfficerCredentialEntity? = null
        var preservedOwnerUidSent: String? = null

        // Simulate older Admin-seeded doc in Cloud with blank pinHash/salt and Admin ownerUid
        val blankSeedDoc = OfficerCredentialEntity(
            nip = hitatunNip,
            fullName = "HITATUN ARAZAK",
            role = OfficerRoles.MANAJER,
            unit = "Pusdalops BPBD Kab. Mukomuko",
            shift = "Manajerial 24 Jam",
            pinHash = "",
            salt = "",
            isCustomPinSet = false,
            initialSecurePin = "",
            isSyncedWithCloud = true
        )

        FirestoreHelper.overrideCloudCredentialFetcherForTesting = { reqNip ->
            if (reqNip == hitatunNip) {
                val active = syncedRecord ?: blankSeedDoc
                CloudCredentialLookupResult.Found(
                    credential = active,
                    ownerUid = preservedOwnerUidSent ?: adminUid
                )
            } else {
                CloudCredentialLookupResult.NotFound
            }
        }
        FirestoreHelper.overrideCloudCredentialWriterForTesting = { updated ->
            syncedRecord = updated
            Result.success(Unit)
        }

        val repo = SmartOfficerAuthRepository(context)
        val authManager = OfficerAuthManager(context)

        // Default '117' must always be rejected
        assertTrue(repo.authenticateByNipOrNameAndPin("HITATUN ARAZAK", "117").isFailure)

        // Variant 1: Minor typo "Hitatur Arazak" with 041006 should resolve & activate blank-hash Cloud doc
        val activateRes = repo.authenticateByNipOrNameAndPin("Hitatur Arazak", hitatunPin6)
        assertTrue("HITATUN ARAZAK blank-hash Cloud activation must succeed: ${activateRes.exceptionOrNull()?.message}", activateRes.isSuccess)
        assertEquals(hitatunNip, activateRes.getOrThrow().nip)
        assertEquals(hitatunNip, authManager.getSavedNip())
        assertTrue(authManager.isOfficerLoggedIn())
        assertTrue("Activated Cloud record must persist salted PBKDF2 hash", syncedRecord?.pinHash?.isNotBlank() == true)
        assertTrue("Activated Cloud record must persist cryptographic salt", syncedRecord?.salt?.isNotBlank() == true)

        // Variant 2: Spaced NIP & casing variants authenticate cleanly
        val spacedRes = repo.authenticateByNipOrNameAndPin("19740128 200604 1 006", hitatunPin6)
        assertTrue("Spaced NIP login for HITATUN ARAZAK must succeed: ${spacedRes.exceptionOrNull()?.message}", spacedRes.isSuccess)

        val titleCaseRes = repo.authenticateByNipOrNameAndPin("Hitatun Arazak", hitatunPin6)
        assertTrue("Title-case login for HITATUN ARAZAK must succeed: ${titleCaseRes.exceptionOrNull()?.message}", titleCaseRes.isSuccess)
    }

    @Test
    fun `HITATUN ARAZAK authenticates with Admin Console plaintext pin override even when stale pinHash exists`() = runBlocking {
        val hitatunNip = "197401282006041006"
        val consolePin = "280174"
        val staleSalt = OfficerCredentialEntity.generateCryptographicSalt()
        val staleHash = OfficerCredentialEntity.hashWithSalt("999111", staleSalt)

        val cloudDoc = OfficerCredentialEntity(
            nip = hitatunNip,
            fullName = "HITATUN ARAZAK",
            role = OfficerRoles.MANAJER,
            unit = "Pusdalops BPBD Kab. Mukomuko",
            shift = "Manajerial 24 Jam",
            pinHash = staleHash,
            salt = staleSalt,
            isCustomPinSet = true,
            initialSecurePin = "",
            isSyncedWithCloud = true
        )

        FirestoreHelper.overrideCloudCredentialFetcherForTesting = { reqNip ->
            if (reqNip == hitatunNip) {
                CloudCredentialLookupResult.Found(
                    credential = cloudDoc,
                    serverPlainPin = consolePin,
                    ownerUid = "admin_google_uid"
                )
            } else {
                CloudCredentialLookupResult.NotFound
            }
        }
        FirestoreHelper.overrideCloudCredentialWriterForTesting = { Result.success(Unit) }

        val repo = SmartOfficerAuthRepository(context)
        val authManager = OfficerAuthManager(context)

        // Old 6-digit NIP must fail once isPinChanged = true unless Console pin matches it
        assertTrue(repo.authenticateByNipOrNameAndPin("HITATUN ARAZAK", "041006").isFailure)

        // Admin Console pin override must succeed
        val consoleRes = repo.authenticateByNipOrNameAndPin("HITATUN ARAZAK", consolePin)
        assertTrue("Admin Console plaintext pin must authenticate HITATUN ARAZAK: ${consoleRes.exceptionOrNull()?.message}", consoleRes.isSuccess)
        assertEquals(hitatunNip, authManager.getSavedNip())

        // Biometric authentication for HITATUN ARAZAK must also succeed and avoid saving raw PIN locally
        val bioRes = repo.authenticateWithBiometric("HITATUN ARAZAK")
        assertTrue("Biometric login for HITATUN ARAZAK must succeed: ${bioRes.exceptionOrNull()?.message}", bioRes.isSuccess)
        assertTrue(authManager.isOfficerLoggedIn())
        assertEquals(hitatunNip, authManager.getSavedNip())
        assertFalse(context.getSharedPreferences("bpbd_officer_auth_prefs", Context.MODE_PRIVATE).contains("officer_pin"))
    }

    @Test
    fun `HITATUN ARAZAK simulation login records are completely purged and account is normalized to brand-new initial 041006 PIN state`() = runBlocking {
        val hitatunNip = "197401282006041006"
        val defaultOfficialPin = "041006"
        val customSalt = OfficerCredentialEntity.generateCryptographicSalt()
        val customHash = OfficerCredentialEntity.hashWithSalt("889900", customSalt)

        // 1. Simulate prior simulation state on emulator & real device:
        // - Cloud document had custom PIN, non-zero lastLoginAt, and emulator device name
        var cloudDocument = OfficerCredentialEntity(
            nip = hitatunNip,
            fullName = "HITATUN ARAZAK",
            role = OfficerRoles.MANAJER,
            unit = "Pusdalops BPBD Kab. Mukomuko",
            shift = "Manajerial 24 Jam",
            pinHash = customHash,
            salt = customSalt,
            lastLoginAt = 1775000000000L,
            lastLoginDevice = "Google sdk_gphone64_x86_64",
            isCustomPinSet = true,
            initialSecurePin = "",
            isSyncedWithCloud = true
        )

        FirestoreHelper.overrideCloudCredentialFetcherForTesting = { reqNip ->
            if (reqNip == hitatunNip) {
                CloudCredentialLookupResult.Found(
                    credential = cloudDocument,
                    serverPlainPin = null,
                    ownerUid = "officer_nip_$hitatunNip"
                )
            } else {
                CloudCredentialLookupResult.NotFound
            }
        }
        FirestoreHelper.overrideCloudCredentialWriterForTesting = { normalizedEntity ->
            cloudDocument = normalizedEntity
            Result.success(Unit)
        }

        val repo = SmartOfficerAuthRepository(context)
        val authManager = OfficerAuthManager(context)
        val db = AppDatabase.getInstance(context)

        // Trigger a failed login attempt & active session to populate audit logs, brute-force map, and saved_nip
        authManager.saveNipToSecureStorage(hitatunNip, "HITATUN ARAZAK", OfficerRoles.MANAJER)
        authManager.markCloudCustomPinStatus(true)
        assertTrue(repo.authenticateByNipOrNameAndPin("HITATUN ARAZAK", "000000").isFailure)

        val logsBefore = db.auditLogDao().getAllAuditLogsList().filter { log ->
            log.nip == hitatunNip || log.name.contains("HITATUN", ignoreCase = true)
        }
        assertTrue("Precondition: simulation audit logs must exist before normalization", logsBefore.isNotEmpty())

        // 2. Execute full normalization & purge for HITATUN ARAZAK
        val normRes = repo.normalizeAccountToBrandNewInitialState(hitatunNip, syncToCloud = true)
        assertTrue("Normalization of HITATUN ARAZAK must succeed: ${normRes.exceptionOrNull()?.message}", normRes.isSuccess)

        // 3. Verify all local audit logs for HITATUN ARAZAK are completely deleted
        val logsAfter = db.auditLogDao().getAllAuditLogsList().filter { log ->
            log.nip == hitatunNip || log.name.contains("HITATUN", ignoreCase = true)
        }
        assertTrue("All audit log traces for HITATUN ARAZAK must be deleted", logsAfter.isEmpty())

        // 4. Verify local credential cache & active session are cleared (treated as never logged in)
        assertEquals("Local credential cache must be deleted so account is treated as never logged in", null, db.credentialDao().getCredentialByNip(hitatunNip))
        assertEquals("Saved NIP must be cleared", "", authManager.getSavedNip())
        assertFalse("Officer must not be marked logged in after reset to brand-new state", authManager.isOfficerLoggedIn())
        assertFalse("Custom PIN flag must be false", authManager.hasCustomPin())

        // 5. Verify Cloud credential document is normalized to brand-new initial state with PIN 041006
        assertEquals("Cloud lastLoginAt must be 0L (never logged in)", 0L, cloudDocument.lastLoginAt)
        assertEquals("Cloud lastLoginDevice must be empty", "", cloudDocument.lastLoginDevice)
        assertFalse("Cloud isCustomPinSet must be false", cloudDocument.isCustomPinSet)
        assertFalse("Cloud isPinChanged must be false", cloudDocument.isPinChanged)
        assertTrue("Cloud PIN must verify against official initial PIN 041006", cloudDocument.verifyPin(defaultOfficialPin))

        // 6. Verify HITATUN ARAZAK can now cleanly log in as a brand-new officer using default PIN 041006
        val freshLoginRes = repo.authenticateByNipOrNameAndPin("HITATUN ARAZAK", defaultOfficialPin)
        assertTrue("Fresh login with reset initial PIN 041006 must succeed: ${freshLoginRes.exceptionOrNull()?.message}", freshLoginRes.isSuccess)
        assertEquals(hitatunNip, authManager.getSavedNip())
        assertTrue(authManager.isOfficerLoggedIn())
    }

    @Test
    fun `startup seeding on fresh install or app update NEVER reverts custom Cloud PINs or wipes saved_nip`() = runBlocking {
        val hitatunNip = "197401282006041006"
        val customPin = "918273"
        val customSalt = OfficerCredentialEntity.generateCryptographicSalt()
        val customHash = OfficerCredentialEntity.hashWithSalt(customPin, customSalt)
        var cloudWriteCount = 0
        var cloudDoc = OfficerCredentialEntity(
            nip = hitatunNip,
            fullName = "HITATUN ARAZAK",
            role = OfficerRoles.MANAJER,
            unit = "Pusdalops BPBD Kab. Mukomuko",
            shift = "Manajerial 24 Jam",
            pinHash = customHash,
            salt = customSalt,
            isCustomPinSet = true,
            initialSecurePin = "",
            isSyncedWithCloud = true
        )

        FirestoreHelper.overrideCloudCredentialFetcherForTesting = { reqNip ->
            if (reqNip == hitatunNip) CloudCredentialLookupResult.Found(cloudDoc) else CloudCredentialLookupResult.NotFound
        }
        FirestoreHelper.overrideCloudCredentialWriterForTesting = { updated ->
            cloudWriteCount++
            cloudDoc = updated
            Result.success(Unit)
        }

        // 1. Simulate fresh install (empty SharedPreferences & Room) + startup seeding
        com.example.data.local.seedDataAsnMukomuko2026(context)
        kotlinx.coroutines.delay(400L)

        // Startup seeding MUST NEVER write/reset Cloud credentials or seed local officer_credentials!
        assertEquals("Startup seeding must NEVER write or reset Cloud PINs", 0, cloudWriteCount)
        assertTrue("Custom PIN flag in Cloud must remain true", cloudDoc.isCustomPinSet)
        assertTrue("Custom PIN hash in Cloud must remain intact", cloudDoc.verifyPin(customPin))
        assertTrue(
            "Local Room officer_credentials table must remain empty after startup seeding",
            AppDatabase.getInstance(context).credentialDao().getAllCredentialsList().isEmpty()
        )

        // Default '117' and initial 6-digit NIP '041006' MUST fail; ONLY the Cloud custom PIN succeeds
        val repo = SmartOfficerAuthRepository(context)
        assertTrue(repo.authenticateOfficer(hitatunNip, "117").isFailure)
        assertTrue(repo.authenticateOfficer(hitatunNip, "041006").isFailure)
        val loginRes = repo.authenticateOfficer(hitatunNip, customPin)
        assertTrue("Custom Cloud PIN must succeed after fresh install: ${loginRes.exceptionOrNull()?.message}", loginRes.isSuccess)

        // 2. Simulate app update from legacy prefs carrying old simulation flags & PusdalopsSession
        val prefs = context.getSharedPreferences("bpbd_officer_auth_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("officer_storage_schema_version", 2)
            .putBoolean("hitatun_arazak_normalized_v1_clean", false)
            .putBoolean("hitatun_arazak_normalized_v1_cloud", false)
            .commit()

        val upgradedAuth = OfficerAuthManager(context)
        com.example.data.local.seedDataAsnMukomuko2026(context)
        kotlinx.coroutines.delay(300L)

        // saved_nip and active session MUST survive app update intact, and obsolete flags are cleaned
        assertEquals("saved_nip must survive app update", hitatunNip, upgradedAuth.getSavedNip())
        assertTrue("Active officer session must survive app update", upgradedAuth.isOfficerLoggedIn())
        assertFalse("Obsolete simulation flag must be cleaned", prefs.contains("hitatun_arazak_normalized_v1_clean"))
        assertFalse("Obsolete simulation cloud flag must be cleaned", prefs.contains("hitatun_arazak_normalized_v1_cloud"))
        assertFalse("Raw PIN must never be stored in SharedPreferences", prefs.contains("officer_pin"))
    }

    @Test
    fun `dynamic Multi-Tier RBAC resolves Manajer, Pengawas, and Operator from Firestore officers collection`() = runBlocking {
        // 1. Uji Tier 1 (Manajer), Tier 2 (Pengawas), dan Tier 3 (Operator / kosong)
        val manajerNip = "199912312026011099"
        val manajerPin = "884422"
        val salt1 = OfficerCredentialEntity.generateCryptographicSalt()
        val manajerCredDoc = OfficerCredentialEntity(
            nip = manajerNip,
            fullName = "MANAJER PUSDALOPS BPBD",
            role = "Operator", // bahkan jika di officer_credentials tertulis Operator, koleksi `officers` menjadi sumber utama RBAC
            pinHash = OfficerCredentialEntity.hashWithSalt(manajerPin, salt1),
            salt = salt1,
            isCustomPinSet = true,
            initialSecurePin = "",
            isSyncedWithCloud = true
        )

        val operatorNip = "197009122007011033"
        val operatorPin = "773311"
        val salt2 = OfficerCredentialEntity.generateCryptographicSalt()
        val operatorCredDoc = OfficerCredentialEntity(
            nip = operatorNip,
            fullName = "PETUGAS DENGAN ROLE KOSONG DI FIRESTORE",
            role = "",
            pinHash = OfficerCredentialEntity.hashWithSalt(operatorPin, salt2),
            salt = salt2,
            isCustomPinSet = true,
            initialSecurePin = "",
            isSyncedWithCloud = true
        )

        FirestoreHelper.overrideCloudCredentialFetcherForTesting = { reqNip ->
            when (reqNip) {
                manajerNip -> CloudCredentialLookupResult.Found(manajerCredDoc)
                operatorNip -> CloudCredentialLookupResult.Found(operatorCredDoc)
                else -> CloudCredentialLookupResult.NotFound
            }
        }
        FirestoreHelper.overrideCloudCredentialWriterForTesting = { Result.success(Unit) }

        val dynamicOfficersRoleMap = mapOf(
            manajerNip to "Manajer",
            operatorNip to "" // Field role kosong -> default ke 'Operator' (Tier 3)
        )
        FirestoreHelper.overrideOfficersCollectionRoleFetcherForTesting = { reqNip ->
            dynamicOfficersRoleMap[reqNip]
        }

        val repo = SmartOfficerAuthRepository(context)
        val authManager = OfficerAuthManager(context)

        // A. Login sebagai Manajer (Tier 1: Full Access)
        val loginManajer = repo.authenticateOfficer(manajerNip, manajerPin)
        assertTrue("Manajer login must succeed", loginManajer.isSuccess)
        assertEquals("Manajer", loginManajer.getOrNull()?.role)
        assertEquals("Manajer", authManager.getOfficerRole())
        assertTrue("isManajer must be true", com.example.security.UserRole.isManajer(authManager.getOfficerRole()))
        assertTrue("Manajer must see Kelola Personil", com.example.security.UserRole.canManagePersonnel(authManager.getOfficerRole()))
        assertTrue("Manajer must see Pengaturan Sistem", com.example.security.UserRole.canAccessSystemSettings(authManager.getOfficerRole()))
        assertTrue("Manajer must see Hapus Laporan", com.example.security.UserRole.canDeleteReports(authManager.getOfficerRole()))
        assertTrue("Manajer must see Validasi Laporan", com.example.security.UserRole.canValidateReports(authManager.getOfficerRole()))

        // B. Login sebagai petugas dengan field `role` kosong -> jatuh ke 'Operator' (Tier 3: Standard Operational)
        val loginOperator = repo.authenticateOfficer(operatorNip, operatorPin)
        assertTrue("Operator login must succeed", loginOperator.isSuccess)
        assertEquals("Missing role field in Firestore must default to Operator", "Operator", loginOperator.getOrNull()?.role)
        assertEquals("Operator", authManager.getOfficerRole())
        assertFalse("isManajer must be false for Operator", com.example.security.UserRole.isManajer(authManager.getOfficerRole()))
        assertFalse("Operator must NOT see Kelola Personil", com.example.security.UserRole.canManagePersonnel(authManager.getOfficerRole()))
        assertFalse("Operator must NOT see Pengaturan Sistem", com.example.security.UserRole.canAccessSystemSettings(authManager.getOfficerRole()))
        assertFalse("Operator must NOT see Hapus Laporan", com.example.security.UserRole.canDeleteReports(authManager.getOfficerRole()))
        assertFalse("Operator must NOT see Validasi Laporan", com.example.security.UserRole.canValidateReports(authManager.getOfficerRole()))
        assertTrue("Operator must see operational menus (Laporan Warga, Form Harian/DALA, Peta)", com.example.security.UserRole.canAccessOperationalMenus(authManager.getOfficerRole()))
    }
}
