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

        // Even when using 6-digit NIP on empty local storage when Cloud has no record -> must FAIL and never seed locally
        val resultSixDigit = repo.authenticateOfficer(nip = testNip, inputPin = testNip.takeLast(6))
        assertTrue("Unregistered NIP must fail without seeding default PIN locally", resultSixDigit.isFailure)
        assertTrue("Cloud verification must have been invoked", cloudCalled)
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
}
