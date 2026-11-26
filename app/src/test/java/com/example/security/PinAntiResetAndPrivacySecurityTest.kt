package com.example.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.CloudCredentialLookupResult
import com.example.data.local.FirestoreHelper
import com.example.data.local.OfficerAuthManager
import com.example.data.local.OfficerCredentialEntity
import com.example.data.local.OfficerRoles
import com.example.data.repository.SmartOfficerAuthRepository
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

/**
 * Security regression suite verifying:
 * 1. Zero unilateral PIN reversion to default during profile edits, network errors, NotFound, or login/biometric metadata sync.
 * 2. Strict fail-closed rejection of all unilateral PIN reset APIs and default-PIN overrides during personnel edit.
 * 3. Strict Citizen Mode isolation in SecurityManager.isOfficerDataSyncPermitted().
 * 4. Zero plaintext PIN exposure in AdminAccessSecurityManager log callbacks.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PinAntiResetAndPrivacySecurityTest {

    private lateinit var context: Context
    private val officerNip = "198605122015031004"
    private val defaultNipPin = "031004"
    private val personalPin = "742915"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("bpbd_officer_auth_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("bpbd_security_manager_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("bpbd_admin_access_security", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("officer_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        AppDatabase.getInstance(context).clearAllTables()
        SmartOfficerAuthRepository.clearFailedAttemptsForTesting()
        FirestoreHelper.resetTestHooks()
        AdminAccessSecurityManager.resetForTesting()
        SecurityManager.clearPinResetInProgress(officerNip)
    }

    @After
    fun tearDown() {
        FirestoreHelper.resetTestHooks()
        AdminAccessSecurityManager.resetForTesting()
        SecurityManager.clearPinResetInProgress(officerNip)
    }

    @Test
    fun `profile edit during Cloud Error or blank-PIN Found NEVER overwrites or reverts custom PIN to default`() = runBlocking {
        val customSalt = OfficerCredentialEntity.generateCryptographicSalt()
        val customHash = OfficerCredentialEntity.hashWithSalt(personalPin, customSalt)
        var cloudCred = OfficerCredentialEntity(
            nip = officerNip,
            fullName = "PETUGAS UJI KEAMANAN",
            role = OfficerRoles.OPERATOR,
            unit = "Pusdalops BPBD Kab. Mukomuko",
            shift = "Regu A",
            phone = "08120000111",
            pinHash = customHash,
            salt = customSalt,
            isCustomPinSet = true,
            initialSecurePin = "",
            isSyncedWithCloud = true
        )
        var cloudWrites = 0
        var writtenEntity: OfficerCredentialEntity? = null

        // Scenario 1: Cloud lookup fails (network Error) during blank-PIN profile edit
        FirestoreHelper.overrideCloudCredentialFetcherForTesting = {
            CloudCredentialLookupResult.Error(
                userMessage = "Timeout jaringan",
                errorCode = "UNAVAILABLE",
                cause = RuntimeException("UNAVAILABLE")
            )
        }
        FirestoreHelper.overrideCloudCredentialWriterForTesting = { entity ->
            cloudWrites++
            writtenEntity = entity
            Result.success(Unit)
        }

        val repo = SmartOfficerAuthRepository(context)
        val errorEditRes = repo.registerOrUpdateManualOfficer(
            fullName = "PETUGAS UJI KEAMANAN (EDIT)",
            nip = officerNip,
            role = OfficerRoles.SUPERVISOR,
            unit = "Pusdalops BPBD Kab. Mukomuko",
            shift = "Regu B",
            phone = "08120000999",
            customInitialPin = null,
            existingId = "officer_$officerNip",
            previousNip = officerNip
        )
        assertTrue("Profile edit during transient Cloud error preserves local profile", errorEditRes.isSuccess)
        assertFalse("Cloud credential sync must be reported false on Error", errorEditRes.getOrThrow().isCloudCredentialSynced)
        assertEquals("Cloud writer MUST NOT be called when lookup fails", 0, cloudWrites)

        // Scenario 2: Explicit PIN change during Cloud Error MUST fail closed
        val errorPinEditRes = repo.registerOrUpdateManualOfficer(
            fullName = "PETUGAS UJI KEAMANAN (EDIT)",
            nip = officerNip,
            role = OfficerRoles.SUPERVISOR,
            unit = "Pusdalops BPBD Kab. Mukomuko",
            shift = "Regu B",
            phone = "08120000999",
            customInitialPin = "951357",
            existingId = "officer_$officerNip",
            previousNip = officerNip
        )
        assertTrue("Setting PIN while Cloud lookup errors must fail closed", errorPinEditRes.isFailure)
        assertEquals("Cloud writer MUST NOT overwrite credential on Error", 0, cloudWrites)

        // Scenario 3: Editing existing officer with blank PIN when Cloud returns Found preserves existing pinHash/salt/isCustomPinSet
        FirestoreHelper.overrideCloudCredentialFetcherForTesting = {
            CloudCredentialLookupResult.Found(cloudCred)
        }
        val foundEditRes = repo.registerOrUpdateManualOfficer(
            fullName = "PETUGAS UJI KEAMANAN SINKRON",
            nip = officerNip,
            role = OfficerRoles.SUPERVISOR,
            unit = "Pusdalops BPBD Kab. Mukomuko",
            shift = "Regu C",
            phone = "08120000777",
            customInitialPin = null,
            existingId = "officer_$officerNip",
            previousNip = officerNip
        )
        assertTrue(foundEditRes.isSuccess)
        assertTrue(foundEditRes.getOrThrow().preservedExistingCustomPin)
        assertEquals("Existing custom hash must remain identical", customHash, writtenEntity?.pinHash)
        assertEquals("Existing custom salt must remain identical", customSalt, writtenEntity?.salt)
        assertTrue("isCustomPinSet must remain true", writtenEntity?.isCustomPinSet == true)

        // Scenario 4: Editing existing officer with NotFound and blank PIN refuses to synthesize default PIN
        FirestoreHelper.overrideCloudCredentialFetcherForTesting = {
            CloudCredentialLookupResult.NotFound
        }
        val writesBeforeNotFoundEdit = cloudWrites
        val notFoundBlankEdit = repo.registerOrUpdateManualOfficer(
            fullName = "PETUGAS UJI KEAMANAN OFFLINE",
            nip = officerNip,
            role = OfficerRoles.OPERATOR,
            unit = "Pusdalops",
            shift = "Regu A",
            phone = "",
            customInitialPin = null,
            existingId = "officer_$officerNip",
            previousNip = officerNip
        )
        assertTrue(notFoundBlankEdit.isSuccess)
        assertEquals("NotFound on existing officer edit MUST NOT fabricate default PIN in Cloud", writesBeforeNotFoundEdit, cloudWrites)

        // Scenario 5: Editing existing officer with forbidden default or 6-digit NIP PIN is explicitly rejected
        val revertToNipRes = repo.registerOrUpdateManualOfficer(
            fullName = "PETUGAS UJI KEAMANAN",
            nip = officerNip,
            role = OfficerRoles.OPERATOR,
            unit = "Pusdalops",
            shift = "Regu A",
            phone = "",
            customInitialPin = defaultNipPin,
            existingId = "officer_$officerNip",
            previousNip = officerNip
        )
        assertTrue("Reverting edited officer PIN back to 6-digit NIP MUST fail closed", revertToNipRes.isFailure)
        assertTrue(revertToNipRes.exceptionOrNull() is SecurityException)
    }

    @Test
    fun `authenticateOfficer refuses needsHashActivation when doc already has hash or isCustomPinSet true`() = runBlocking {
        val customSalt = OfficerCredentialEntity.generateCryptographicSalt()
        val customHash = OfficerCredentialEntity.hashWithSalt(personalPin, customSalt)
        // Malformed edge case: isCustomPinSet = true but someone probes with 6-digit NIP default PIN
        val cloudDoc = OfficerCredentialEntity(
            nip = officerNip,
            fullName = "PETUGAS UJI",
            role = OfficerRoles.MANAJER,
            pinHash = customHash,
            salt = customSalt,
            isCustomPinSet = true,
            initialSecurePin = "",
            isSyncedWithCloud = true
        )
        var writerInvoked = false
        FirestoreHelper.overrideCloudCredentialFetcherForTesting = {
            CloudCredentialLookupResult.Found(cloudDoc)
        }
        FirestoreHelper.overrideCloudCredentialWriterForTesting = {
            writerInvoked = true
            Result.success(Unit)
        }

        val repo = SmartOfficerAuthRepository(context)
        val wrongDefaultAttempt = repo.authenticateOfficer(officerNip, defaultNipPin, "TestPhone")
        assertTrue("Default 6-digit NIP PIN must be rejected when custom PIN is active", wrongDefaultAttempt.isFailure)
        assertFalse("Cloud credential writer must NEVER overwrite pinHash on rejected attempt", writerInvoked)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `all legacy unilateral PIN reset APIs fail closed and preserve Cloud credentials`() = runBlocking {
        val authManager = OfficerAuthManager(context)
        val repo = SmartOfficerAuthRepository(context)

        assertFalse("resetPinToDefault must return false", authManager.resetPinToDefault("123456"))
        assertTrue("resetPinWithAdmin must fail closed", authManager.resetPinWithAdmin("any").isFailure)
        assertTrue(
            "normalizeAccountToBrandNewInitialState must fail with SecurityException",
            repo.normalizeAccountToBrandNewInitialState(officerNip, true).exceptionOrNull() is SecurityException
        )
        assertTrue(
            "resetToInitialNipSecret must fail with SecurityException",
            repo.resetToInitialNipSecret(officerNip).exceptionOrNull() is SecurityException
        )
    }

    @Test
    fun `Citizen Mode blocks SecurityManager officer data sync even if stale local verified session exists`() {
        val authManager = OfficerAuthManager(context)
        val secMgr = SecurityManager.getInstance(context)

        // Ensure officer is NOT logged in (Citizen Mode), even if stale verified flag exists in security prefs
        authManager.clearSavedOfficerIdentity()
        context.getSharedPreferences("bpbd_security_manager_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("local_verified_$officerNip", true)
            .commit()

        assertFalse("OfficerAuthManager must report not logged in", authManager.isOfficerLoggedIn())
        assertFalse(
            "Citizen Mode MUST strictly deny officer data sync regardless of stale local prefs",
            secMgr.isOfficerDataSyncPermitted()
        )
    }

    @Test
    fun `AdminAccessSecurityManager never exposes plaintext user PIN or master PIN in debug logs`() = runBlocking {
        val originalOut = System.out
        val capturedBytes = java.io.ByteArrayOutputStream()
        System.setOut(java.io.PrintStream(capturedBytes))
        try {
            AdminAccessSecurityManager.disableFirestoreForTesting = true
            AdminAccessSecurityManager.overrideFirestorePinFetcherForTesting = {
                FirestoreMasterPinLookup.Found(masterPin = "SECRET_MASTER_7492", sourceLabel = "TEST_SERVER")
            }

            // Wrong PIN attempt
            AdminAccessSecurityManager.verifyMasterPasswordAndIssueToken(
                context = context,
                masterPassword = "WRONG_USER_PIN_5521",
                officerNip = officerNip,
                officerName = "Petugas Uji",
                officerRole = OfficerRoles.MANAJER,
                actionTitle = "Uji Redaksi Log"
            )
            // Correct PIN attempt
            AdminAccessSecurityManager.verifyMasterPasswordAndIssueToken(
                context = context,
                masterPassword = "SECRET_MASTER_7492",
                officerNip = officerNip,
                officerName = "Petugas Uji",
                officerRole = OfficerRoles.MANAJER,
                actionTitle = "Uji Redaksi Log"
            )
        } finally {
            System.setOut(originalOut)
        }

        val joined = capturedBytes.toString(Charsets.UTF_8.name())
        assertTrue("Redacted debug marker must be logged", joined.contains("[REDACTED"))
        assertFalse("Log output MUST NOT contain plaintext master PIN", joined.contains("SECRET_MASTER_7492"))
        assertFalse("Log output MUST NOT contain plaintext user input PIN", joined.contains("WRONG_USER_PIN_5521"))
    }
}
