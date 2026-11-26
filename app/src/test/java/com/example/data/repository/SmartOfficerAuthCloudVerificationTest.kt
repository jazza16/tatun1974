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

    @Suppress("DEPRECATION")
    @Test
    fun `unilateral normalization to brand-new initial PIN state is permanently blocked and preserves custom Cloud PIN and audit logs`() = runBlocking {
        val hitatunNip = "197401282006041006"
        val customPin = "889900"
        val defaultOfficialPin = "041006"
        val customSalt = OfficerCredentialEntity.generateCryptographicSalt()
        val customHash = OfficerCredentialEntity.hashWithSalt(customPin, customSalt)

        var cloudDocument = OfficerCredentialEntity(
            nip = hitatunNip,
            fullName = "HITATUN ARAZAK",
            role = OfficerRoles.MANAJER,
            unit = "Pusdalops BPBD Kab. Mukomuko",
            shift = "Manajerial 24 Jam",
            pinHash = customHash,
            salt = customSalt,
            lastLoginAt = 1775000000000L,
            lastLoginDevice = "Samsung Galaxy S24",
            isCustomPinSet = true,
            initialSecurePin = "",
            isSyncedWithCloud = true
        )
        var cloudWriteAttempted = false

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
            cloudWriteAttempted = true
            cloudDocument = normalizedEntity
            Result.success(Unit)
        }

        val repo = SmartOfficerAuthRepository(context)
        val authManager = OfficerAuthManager(context)
        val db = AppDatabase.getInstance(context)

        authManager.saveNipToSecureStorage(hitatunNip, "HITATUN ARAZAK", OfficerRoles.MANAJER)
        authManager.markCloudCustomPinStatus(true)
        assertTrue(repo.authenticateByNipOrNameAndPin("HITATUN ARAZAK", "000000").isFailure)

        val logsBefore = db.auditLogDao().getAllAuditLogsList().filter { log ->
            log.nip == hitatunNip || log.name.contains("HITATUN", ignoreCase = true)
        }
        assertTrue("Precondition: audit logs must exist", logsBefore.isNotEmpty())

        // Attempt unilateral normalization -> MUST be blocked with SecurityException
        val normRes = repo.normalizeAccountToBrandNewInitialState(hitatunNip, syncToCloud = true)
        assertTrue("Unilateral normalization MUST fail closed", normRes.isFailure)
        assertTrue(normRes.exceptionOrNull() is SecurityException)
        assertFalse("Cloud credential writer MUST NOT be invoked", cloudWriteAttempted)

        // Audit logs, active identity, and Cloud custom PIN MUST remain 100% intact
        val logsAfter = db.auditLogDao().getAllAuditLogsList().filter { log ->
            log.nip == hitatunNip || log.name.contains("HITATUN", ignoreCase = true)
        }
        assertEquals("Audit logs must NOT be purged", logsBefore.size, logsAfter.size)
        assertEquals("Saved NIP must remain intact", hitatunNip, authManager.getSavedNip())
        assertTrue("Cloud isCustomPinSet must remain true", cloudDocument.isCustomPinSet)
        assertEquals("Cloud pinHash must remain unchanged", customHash, cloudDocument.pinHash)
        assertTrue("Custom PIN must still verify", cloudDocument.verifyPin(customPin))
        assertFalse("Default 6-digit NIP PIN must remain rejected", cloudDocument.verifyPin(defaultOfficialPin))
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

    @Test
    fun `manual officer input via Kelola Petugas (YAKI, NIP 123412 and new 18-digit ASN) can immediately log in by Name or NIP`() = runBlocking {
        val cloudCredentialsStore = mutableMapOf<String, OfficerCredentialEntity>()
        FirestoreHelper.overrideCloudCredentialFetcherForTesting = { reqNip ->
            val found = cloudCredentialsStore[reqNip]
            if (found != null) CloudCredentialLookupResult.Found(found) else CloudCredentialLookupResult.NotFound
        }
        FirestoreHelper.overrideCloudCredentialWriterForTesting = { entity ->
            cloudCredentialsStore[entity.nip] = entity
            Result.success(Unit)
        }

        val repo = SmartOfficerAuthRepository(context)

        // 1. Admin menginput petugas manual: Nama = "YAKI", NIP = "123412"
        val regYaki = repo.registerOrUpdateManualOfficer(
            fullName = "YAKI",
            nip = "123412",
            role = OfficerRoles.OPERATOR
        )
        assertTrue("Manual registration for YAKI must succeed", regYaki.isSuccess)
        assertEquals("123412", regYaki.getOrNull()?.effectiveInitialPin)
        assertTrue("Cloud credential must be provisioned for 123412", cloudCredentialsStore.containsKey("123412"))

        // 2. Petugas YAKI login menggunakan Nama "YAKI" dan PIN "123412" (sesuai screenshot user)
        val loginByName = repo.authenticateByNipOrNameAndPin("YAKI", "123412")
        assertTrue("Login by Name 'YAKI' with PIN '123412' must succeed: ${loginByName.exceptionOrNull()?.message}", loginByName.isSuccess)
        assertEquals("YAKI", loginByName.getOrNull()?.fullName)
        assertEquals("123412", loginByName.getOrNull()?.nip)

        // 3. Petugas YAKI login menggunakan NIP "123412" dan PIN "123412"
        val loginByNip = repo.authenticateByNipOrNameAndPin("123412", "123412")
        assertTrue("Login by NIP '123412' must succeed", loginByNip.isSuccess)

        // 4. Petugas YAKI login menggunakan gabungan "YAKI, 123412"
        val loginByCombined = repo.authenticateByNipOrNameAndPin("YAKI, 123412", "123412")
        assertTrue("Login by combined 'YAKI, 123412' must succeed", loginByCombined.isSuccess)

        // 5. Admin menginput ASN baru dengan NIP 18 digit standar: "Ramdani Pratama, S.T.", NIP "19910815 202012 1 007"
        val regAsn = repo.registerOrUpdateManualOfficer(
            fullName = "Ramdani Pratama, S.T.",
            nip = "19910815 202012 1 007",
            role = OfficerRoles.PETUGAS_JAGA
        )
        assertTrue("Manual registration for new 18-digit ASN must succeed", regAsn.isSuccess)
        assertEquals("121007", regAsn.getOrNull()?.effectiveInitialPin)

        val loginAsnByName = repo.authenticateByNipOrNameAndPin("Ramdani Pratama", "121007")
        assertTrue("New 18-digit ASN must be able to log in by Name + 6-digit NIP PIN", loginAsnByName.isSuccess)
        assertEquals("199108152020121007", loginAsnByName.getOrNull()?.nip)
    }

    @Test
    fun `cross-device login by Name succeeds for manually added officer fetched from Cloud officers directory`() = runBlocking {
        val cloudCredentialsStore = mutableMapOf<String, OfficerCredentialEntity>()
        FirestoreHelper.overrideCloudCredentialFetcherForTesting = { reqNip ->
            val found = cloudCredentialsStore[reqNip]
            if (found != null) CloudCredentialLookupResult.Found(found) else CloudCredentialLookupResult.NotFound
        }
        FirestoreHelper.overrideCloudCredentialWriterForTesting = { entity ->
            cloudCredentialsStore[entity.nip] = entity
            Result.success(Unit)
        }
        // Simulasikan petugas "YAKI" (NIP 123412) didaftarkan di perangkat Admin lain dan tersimpan di Cloud Firestore `officers`
        FirestoreHelper.overrideOfficersDirectoryFetcherForTesting = {
            Result.success(
                listOf(
                    com.example.data.local.OfficerProfile(
                        id = "officer_123412",
                        fullName = "YAKI",
                        nip = "123412",
                        role = OfficerRoles.OPERATOR,
                        unit = "Pusdalops BPBD Kab. Mukomuko",
                        shift = "Shift Siaga 24 Jam",
                        phone = "08123456789"
                    )
                )
            )
        }

        val repo = SmartOfficerAuthRepository(context)

        // Perangkat baru (belum ada YAKI di cache lokal) langsung login dengan Nama "YAKI" dan PIN "123412"
        val crossDeviceLogin = repo.authenticateByNipOrNameAndPin("YAKI", "123412")
        assertTrue(
            "Cross-device login by Name 'YAKI' must resolve from Cloud officers directory and succeed: ${crossDeviceLogin.exceptionOrNull()?.message}",
            crossDeviceLogin.isSuccess
        )
        assertEquals("YAKI", crossDeviceLogin.getOrNull()?.fullName)
        assertEquals("123412", crossDeviceLogin.getOrNull()?.nip)
    }

    @Test
    fun `official logout clears active officer session synchronously while preserving saved_nip for custom Cloud PIN relogin`() = runBlocking {
        val salt = OfficerCredentialEntity.generateCryptographicSalt()
        val cloudRecord = OfficerCredentialEntity(
            nip = testNip,
            fullName = testName,
            role = OfficerRoles.MANAJER,
            pinHash = OfficerCredentialEntity.hashWithSalt(testCustomPin, salt),
            salt = salt,
            isCustomPinSet = true,
            initialSecurePin = "",
            isSyncedWithCloud = true
        )
        FirestoreHelper.overrideCloudCredentialFetcherForTesting = { reqNip ->
            if (reqNip == testNip) CloudCredentialLookupResult.Found(cloudRecord) else CloudCredentialLookupResult.NotFound
        }
        FirestoreHelper.overrideCloudCredentialWriterForTesting = { Result.success(Unit) }

        val repo = SmartOfficerAuthRepository(context)
        val officerAuth = OfficerAuthManager(context)
        val securityManager = com.example.security.SecurityManager.getInstance(context)

        // 1. Login pertama dengan PIN custom Cloud
        val firstLogin = repo.authenticateOfficer(testNip, testCustomPin)
        assertTrue("Initial Cloud PIN login must succeed", firstLogin.isSuccess)
        assertTrue("Officer session must be active after login", officerAuth.isOfficerLoggedIn())
        assertTrue("Local verified security session must be active", securityManager.hasActiveVerifiedLocalSession(testNip))

        // 2. Petugas melakukan logout dari sesi resmi Ruang Kerja
        officerAuth.logoutOfficer()
        assertFalse("Officer session MUST be inactive immediately after logout", officerAuth.isOfficerLoggedIn())
        assertFalse("Local security session MUST be cleared after logout", securityManager.hasActiveVerifiedLocalSession(testNip))
        assertEquals("saved_nip must be preserved for quick relogin & biometric", testNip, officerAuth.getSavedNip())

        // 3. Relogin dengan PIN default / 6-digit NIP tetap DITOLAK (tidak pernah reset ke PIN default)
        assertTrue("Default PIN 117 must remain rejected after logout", repo.authenticateOfficer(testNip, "117").isFailure)
        assertTrue("Initial 6-digit NIP must remain rejected after logout", repo.authenticateOfficer(testNip, testNip.takeLast(6)).isFailure)
        assertFalse("Officer must remain logged out after rejected default PINs", officerAuth.isOfficerLoggedIn())

        // 4. Relogin dengan PIN custom Cloud berhasil kembali dan membentuk sesi baru yang valid
        val relogin = repo.authenticateOfficer(testNip, testCustomPin)
        assertTrue("Relogin with custom Cloud PIN must succeed: ${relogin.exceptionOrNull()?.message}", relogin.isSuccess)
        assertTrue("Officer session must be active after relogin", officerAuth.isOfficerLoggedIn())
        assertTrue("Verified security session must be re-established after relogin", securityManager.hasActiveVerifiedLocalSession(testNip))
        assertTrue("Local verified timestamp must be positive", securityManager.getLocalVerifiedSessionTimestamp(testNip) > 0L)
    }

    @Test
    fun `entering citizen guest mode terminates lingering officer session and keeps Ruang Kerja locked`() = runBlocking {
        val salt = OfficerCredentialEntity.generateCryptographicSalt()
        val cloudRecord = OfficerCredentialEntity(
            nip = testNip,
            fullName = testName,
            role = OfficerRoles.MANAJER,
            pinHash = OfficerCredentialEntity.hashWithSalt(testCustomPin, salt),
            salt = salt,
            isCustomPinSet = true,
            initialSecurePin = "",
            isSyncedWithCloud = true
        )
        FirestoreHelper.overrideCloudCredentialFetcherForTesting = { reqNip ->
            if (reqNip == testNip) CloudCredentialLookupResult.Found(cloudRecord) else CloudCredentialLookupResult.NotFound
        }
        FirestoreHelper.overrideCloudCredentialWriterForTesting = { Result.success(Unit) }

        val repo = SmartOfficerAuthRepository(context)
        val officerAuth = OfficerAuthManager(context)
        val securityManager = com.example.security.SecurityManager.getInstance(context)

        // Simulasikan petugas sempat aktif login sebelumnya
        assertTrue(repo.authenticateOfficer(testNip, testCustomPin).isSuccess)
        assertTrue(officerAuth.isOfficerLoggedIn())

        // Simulasikan pengguna memilih "Masuk Sebagai Tamu / Warga" di Landing Page
        securityManager.clearLocalSession()
        officerAuth.logoutOfficer()

        // Verifikasi isolasi ketat Mode Warga: tidak ada sesi petugas yang tersisa
        assertFalse("Citizen guest mode MUST NOT retain active officer session", officerAuth.isOfficerLoggedIn())
        assertFalse("Citizen guest mode MUST clear verified Ruang Kerja session", securityManager.hasActiveVerifiedLocalSession(testNip))
        assertEquals("Local verified timestamp must be 0L in guest mode", 0L, securityManager.getLocalVerifiedSessionTimestamp(testNip))
    }

    @Test
    fun `biometric relogin after official logout succeeds using preserved saved_nip without modifying Cloud PIN`() = runBlocking {
        val salt = OfficerCredentialEntity.generateCryptographicSalt()
        val expectedHash = OfficerCredentialEntity.hashWithSalt(testCustomPin, salt)
        var cloudDoc = OfficerCredentialEntity(
            nip = testNip,
            fullName = testName,
            role = OfficerRoles.PENGAWAS,
            pinHash = expectedHash,
            salt = salt,
            isCustomPinSet = true,
            initialSecurePin = "",
            isSyncedWithCloud = true
        )
        FirestoreHelper.overrideCloudCredentialFetcherForTesting = { reqNip ->
            if (reqNip == testNip) CloudCredentialLookupResult.Found(cloudDoc) else CloudCredentialLookupResult.NotFound
        }
        FirestoreHelper.overrideCloudCredentialWriterForTesting = { updated ->
            cloudDoc = updated
            Result.success(Unit)
        }

        val repo = SmartOfficerAuthRepository(context)
        val officerAuth = OfficerAuthManager(context)
        val securityManager = com.example.security.SecurityManager.getInstance(context)

        // 1. Login awal dengan PIN custom -> lalu Logout resmi
        assertTrue(repo.authenticateOfficer(testNip, testCustomPin).isSuccess)
        officerAuth.logoutOfficer()
        assertFalse(officerAuth.isOfficerLoggedIn())
        val preservedNip = officerAuth.getSavedNip()
        assertEquals(testNip, preservedNip)

        // 2. Relogin menggunakan Biometrik pada Landing Page / Dialog Login memakai preserved saved_nip
        val bioRelogin = repo.authenticateWithBiometric(preservedNip)
        assertTrue("Biometric relogin after logout must succeed: ${bioRelogin.exceptionOrNull()?.message}", bioRelogin.isSuccess)
        assertTrue("Officer session must be active after biometric relogin", officerAuth.isOfficerLoggedIn())
        assertTrue("Verified local session must be active after biometric relogin", securityManager.hasActiveVerifiedLocalSession(testNip))
        assertEquals("Cloud custom PIN hash must remain completely untouched", expectedHash, cloudDoc.pinHash)
        assertTrue("Custom Cloud PIN must still verify intact", cloudDoc.verifyPin(testCustomPin))
    }

    @Suppress("DEPRECATION")
    @Test
    fun `unilateral resetToInitialNipSecret is permanently blocked and never reverts custom Cloud PIN to 6-digit NIP`() = runBlocking {
        val salt = OfficerCredentialEntity.generateCryptographicSalt()
        val expectedHash = OfficerCredentialEntity.hashWithSalt(testCustomPin, salt)
        var cloudDoc = OfficerCredentialEntity(
            nip = testNip,
            fullName = testName,
            role = OfficerRoles.MANAJER,
            pinHash = expectedHash,
            salt = salt,
            isCustomPinSet = true,
            initialSecurePin = "",
            isSyncedWithCloud = true
        )
        var cloudWriteCalled = false
        FirestoreHelper.overrideCloudCredentialFetcherForTesting = { reqNip ->
            if (reqNip == testNip) CloudCredentialLookupResult.Found(cloudDoc) else CloudCredentialLookupResult.NotFound
        }
        FirestoreHelper.overrideCloudCredentialWriterForTesting = { updated ->
            cloudWriteCalled = true
            cloudDoc = updated
            Result.success(Unit)
        }

        val repo = SmartOfficerAuthRepository(context)
        val officerAuth = OfficerAuthManager(context)

        // 1. Login awal menggunakan PIN custom
        val initialLogin = repo.authenticateOfficer(testNip, testCustomPin)
        assertTrue("Initial login with custom PIN must succeed", initialLogin.isSuccess)
        assertTrue(officerAuth.isOfficerLoggedIn())

        // 2. Percobaan Reset PIN sepihak ke 6-digit NIP -> WAJIB DITOLAK permanen
        val resetRes = repo.resetToInitialNipSecret(testNip)
        assertTrue("resetToInitialNipSecret MUST fail closed", resetRes.isFailure)
        assertTrue(resetRes.exceptionOrNull() is SecurityException)
        assertTrue("Cloud isCustomPinSet must remain true", cloudDoc.isCustomPinSet)
        assertEquals("Cloud pinHash must remain untouched", expectedHash, cloudDoc.pinHash)
        assertTrue("Active officer session must remain intact", officerAuth.isOfficerLoggedIn())

        // 3. Login dengan 6-digit NIP default WAJIB tetap ditolak, sedangkan PIN custom tetap bekerja
        val initialSixDigitPin = OfficerCredentialEntity.generateInitialPinForNip(testNip)
        assertTrue("6-digit NIP default PIN must NOT work", repo.authenticateOfficer(testNip, initialSixDigitPin).isFailure)
        assertTrue("Custom PIN must continue working seamlessly", repo.authenticateOfficer(testNip, testCustomPin).isSuccess)
        // Only login metadata sync may have executed, never a hash overwrite
        assertEquals(expectedHash, cloudDoc.pinHash)
        assertTrue(cloudWriteCalled)
    }

    @Test
    fun `local-only batch roster sync updates cache cleanly and reconcileWithActiveHeartbeats stays pure in-memory`() {
        val officerAuth = OfficerAuthManager(context)
        val securityManager = com.example.security.SecurityManager.getInstance(context)
        val sampleRoster = listOf(
            com.example.data.local.OfficerProfile(
                id = "officer_197401282006041006",
                fullName = "HITATUN ARAZAK",
                nip = "197401282006041006",
                role = OfficerRoles.MANAJER,
                unit = "Pusdalops BPBD Kab. Mukomuko",
                shift = "Manajerial 24 Jam",
                phone = "081234567890"
            )
        )
        officerAuth.syncRosterLocallyWithoutCloudWrite(sampleRoster)
        val cachedRoster = officerAuth.getOfficerRoster()
        assertTrue("Synced officer must exist in local roster cache", cachedRoster.any { it.nip == "197401282006041006" })

        val beforeSessionsCount = securityManager.getLocalCachedSessions().size
        val reconciled = securityManager.reconcileWithActiveHeartbeats(
            sessions = emptyList(),
            heartbeats = listOf(
                com.example.data.sync.DeviceHeartbeat(
                    deviceId = "dev_test_001",
                    deviceModel = "Samsung SM-A546E",
                    officerName = "HITATUN ARAZAK",
                    lastSeen = System.currentTimeMillis(),
                    latencyMs = 45L,
                    status = "ONLINE"
                )
            ),
            persistToCache = false
        )
        assertEquals("Reconciled list should include active heartbeat session", 1, reconciled.size)
        assertEquals(
            "Pure in-memory reconciliation must NOT mutate SharedPreferences cache",
            beforeSessionsCount,
            securityManager.getLocalCachedSessions().size
        )
    }
}


