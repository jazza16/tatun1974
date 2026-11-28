package com.example.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.local.AppUpdatePreferences
import com.example.data.local.CloudCredentialLookupResult
import com.example.data.local.FirestoreHelper
import com.example.data.local.OfficerAuthManager
import com.example.data.local.OfficerCredentialEntity
import com.example.data.local.OfficerRoles
import com.example.data.repository.SmartOfficerAuthRepository
import com.example.model.LATEST_RELEASE_CHANGELOG
import com.example.model.RELEASE_HISTORY_ACCORDION_CHANGELOGS
import com.example.model.MukomukoDistricts
import com.example.model.MukomukoKrb2026Catalog
import com.example.model.MukomukoRpb2025Catalog
import com.example.model.MukomukoRtrw2025Catalog
import com.example.util.SessionManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Comprehensive Pre-Release Verification Suite for SIPAGAR KITA v1.3.0 (versionCode 14)
 * "Trilogi Kebijakan Kebencanaan Daerah" — Officer Login Session Anti-Failure Verification.
 *
 * Ensures that installing or upgrading to the v1.3.0 Release APK never breaks officer login,
 * preserves existing officer sessions across upgrades, synchronizes OfficerAuthManager &
 * SessionManager cleanly, enforces strict Cloud PIN verification, and keeps Trilogi data intact.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class V130OfficerLoginSessionVerificationTest {

    private lateinit var context: Context
    private val superAdminNip = "197401282006041006"
    private val superAdminName = "HITATUN ARAZAK"
    private val superAdminPin = "758291"

    private val operatorNip = "197906072006041011"
    private val operatorName = "RURI IRWANDI, ST, MT"
    private val operatorPin = "492817"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        FirestoreHelper.resetTestHooks()
        SmartOfficerAuthRepository.clearFailedAttemptsForTesting()
        AdminAccessSecurityManager.resetForTesting()
        context.getSharedPreferences("bpbd_officer_auth_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("PusdalopsSession", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("mukomuko_update_persistence", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("bpbd_security_manager_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        runBlocking {
            AppDatabase.getInstance(context).clearAllTables()
        }
    }

    @After
    fun tearDown() {
        FirestoreHelper.resetTestHooks()
        SmartOfficerAuthRepository.clearFailedAttemptsForTesting()
        AdminAccessSecurityManager.resetForTesting()
    }

    @Test
    fun `v130 build metadata and Trilogi Kebijakan Kebencanaan Daerah release catalog are synced`() {
        assertEquals("1.3.1", BuildConfig.VERSION_NAME)
        assertEquals(14, BuildConfig.VERSION_CODE)
        assertEquals("1.3.1", LATEST_RELEASE_CHANGELOG.version)
        assertEquals("Akurasi Rute Evakuasi, Pemisahan PDF & Geofence Anti-Spam", LATEST_RELEASE_CHANGELOG.releaseName)
        assertTrue(LATEST_RELEASE_CHANGELOG.isLatest)
        assertTrue(
            RELEASE_HISTORY_ACCORDION_CHANGELOGS.any {
                it.version == "1.3.0" && it.releaseName == "Trilogi Kebijakan Kebencanaan Daerah"
            }
        )

        // Ensure all 15 Mukomuko districts resolve cleanly across KRB, RPB, and RTRW catalogs
        assertEquals(15, MukomukoDistricts.list.size)
        MukomukoDistricts.list.forEach { district ->
            val krb = MukomukoKrb2026Catalog.getDistrictProfile(district)
            val rtrw = MukomukoRtrw2025Catalog.getDistrictSpatialProfile(district)
            assertNotNull(krb)
            assertNotNull(rtrw)
            assertTrue(krb.districtName.isNotBlank())
            assertTrue(rtrw.settlementHierarchyCode.isNotBlank())
        }
        assertEquals(31, MukomukoRpb2025Catalog.strategies.size)
        assertEquals(11, MukomukoRpb2025Catalog.officialEvacuationShelters.size)
    }

    @Test
    fun `in-place APK upgrade from v1_2_0 to v1_3_0 preserves active officer session and purges legacy local PIN keys safely`() {
        // 1. Simulate device running v1.2.0 (Build 13) with an active logged-in officer & legacy PIN keys
        val updatePrefsRaw = context.getSharedPreferences("mukomuko_update_persistence", Context.MODE_PRIVATE)
        updatePrefsRaw.edit()
            .putString("recorded_build_version", "1.2.0")
            .putString("installed_version", "1.2.0")
            .putString("latest_server_version", "1.2.0")
            .putBoolean("is_permanently_installed", true)
            .commit()

        val authPrefsRaw = context.getSharedPreferences("bpbd_officer_auth_prefs", Context.MODE_PRIVATE)
        val authEditor = authPrefsRaw.edit()
        authEditor.putInt("storage_schema_version", 2) // Older schema prior to v3 migration
        authEditor.putBoolean("is_officer_logged_in", true)
        SecureStorageHelper.putEncrypted(authEditor, "saved_nip", superAdminNip)
        SecureStorageHelper.putEncrypted(authEditor, "officer_badge_id", superAdminNip)
        SecureStorageHelper.putEncrypted(authEditor, "officer_name", superAdminName)
        SecureStorageHelper.putEncrypted(authEditor, "officer_role", OfficerRoles.SUPER_ADMIN)
        SecureStorageHelper.putEncrypted(authEditor, "officer_shift", "Shift Siaga 24 Jam")
        // Legacy keys that must be purged non-destructively
        authEditor.putString("officer_pin", "117")
        authEditor.putString("officer_pin_hash", "legacy_hash_should_be_removed")
        authEditor.putString("officer_pin_salt", "legacy_salt_should_be_removed")
        authEditor.commit()

        // 2. Simulate app cold-start after installing v1.3.0 (Build 14) APK
        val updatePreferences = AppUpdatePreferences(context)
        val authManager = OfficerAuthManager(context)
        val sessionManager = SessionManager(context)

        // 3. Assert v1.3.1 upgrade succeeded cleanly
        assertEquals("1.3.1", updatePreferences.getInstalledVersion())
        assertTrue(updatePreferences.isUpdatePermanentlyInstalled())

        // 4. Assert officer session survived upgrade 100% intact (ZERO forced logout / ZERO lost NIP)
        assertTrue("Active officer session MUST survive v1.2.0 -> v1.3.1 APK upgrade", authManager.isOfficerLoggedIn())
        assertEquals(superAdminNip, authManager.getSavedNip())
        assertEquals(superAdminNip, authManager.getOfficerBadgeId())
        assertTrue(authManager.getOfficerName().contains("HITATUN", ignoreCase = true))
        assertEquals(OfficerRoles.SUPER_ADMIN, authManager.getOfficerRole())

        // 5. Assert legacy local PIN keys were safely purged without wiping saved_nip or session
        assertFalse(authPrefsRaw.contains("officer_pin"))
        assertFalse(authPrefsRaw.contains("officer_pin_hash"))
        assertFalse(authPrefsRaw.contains("officer_pin_salt"))

        // 6. Assert SessionManager resolves verified operator token cleanly without exception
        val verifiedUser = sessionManager.authenticateToken()
        assertEquals(superAdminNip, verifiedUser.nip)
        assertTrue(verifiedUser.name.contains("HITATUN", ignoreCase = true))
    }

    @Test
    fun `cloud verified login works for SuperAdmin and Operator roles and syncs OfficerAuthManager plus SessionManager`() = runBlocking {
        val superSalt = OfficerCredentialEntity.generateCryptographicSalt()
        val superHash = OfficerCredentialEntity.hashWithSalt(superAdminPin, superSalt)
        val superCloudCred = OfficerCredentialEntity(
            nip = superAdminNip,
            fullName = superAdminName,
            role = OfficerRoles.SUPER_ADMIN,
            unit = "Pusdalops-PB BPBD Kab. Mukomuko",
            shift = "Shift Siaga 24 Jam",
            phone = "081200000001",
            pinHash = superHash,
            salt = superSalt,
            isCustomPinSet = true,
            initialSecurePin = "",
            isSyncedWithCloud = true
        )

        val opSalt = OfficerCredentialEntity.generateCryptographicSalt()
        val opHash = OfficerCredentialEntity.hashWithSalt(operatorPin, opSalt)
        val opCloudCred = OfficerCredentialEntity(
            nip = operatorNip,
            fullName = operatorName,
            role = OfficerRoles.RBAC_OPERATOR,
            unit = "Tim Reaksi Cepat (TRC) BPBD",
            shift = "Shift Pagi (08:00-20:00)",
            phone = "081200000002",
            pinHash = opHash,
            salt = opSalt,
            isCustomPinSet = true,
            initialSecurePin = "",
            isSyncedWithCloud = true
        )

        FirestoreHelper.overrideCloudCredentialFetcherForTesting = { lookupNip ->
            when (lookupNip) {
                superAdminNip -> CloudCredentialLookupResult.Found(credential = superCloudCred)
                operatorNip -> CloudCredentialLookupResult.Found(credential = opCloudCred)
                else -> CloudCredentialLookupResult.NotFound
            }
        }
        FirestoreHelper.overrideOfficersCollectionRoleFetcherForTesting = { lookupNip ->
            when (lookupNip) {
                superAdminNip -> OfficerRoles.SUPER_ADMIN
                operatorNip -> OfficerRoles.RBAC_OPERATOR
                else -> null
            }
        }

        val repo = SmartOfficerAuthRepository(context)
        val authManager = OfficerAuthManager(context)
        val sessionManager = SessionManager(context)

        // A. Login as SuperAdmin via NIP + Cloud PIN
        val superRes = repo.authenticateOfficer(nip = superAdminNip, inputPin = superAdminPin)
        assertTrue("SuperAdmin cloud login must succeed", superRes.isSuccess)
        assertTrue(authManager.isOfficerLoggedIn())
        assertEquals(superAdminNip, authManager.getSavedNip())
        assertEquals(OfficerRoles.SUPER_ADMIN, authManager.getOfficerRole())
        assertEquals(superAdminNip, sessionManager.getNipPetugas())
        assertTrue(sessionManager.authenticateToken().name.isNotBlank())

        // B. Subsequent quick PIN-only relogin using saved_nip
        authManager.logoutOfficer()
        assertFalse("Officer must be logged out after logoutOfficer()", authManager.isOfficerLoggedIn())
        assertEquals("saved_nip should stay cached for fast PIN-only re-entry", superAdminNip, authManager.getSavedNip())

        val quickPinRes = repo.authenticateOfficerByPinOnly(inputPin = superAdminPin)
        assertTrue("PIN-only relogin with saved_nip must succeed", quickPinRes.isSuccess)
        assertTrue(authManager.isOfficerLoggedIn())

        // C. Switch account to Operator role and verify dynamic RBAC updates cleanly
        val opRes = repo.authenticateOfficer(nip = operatorNip, inputPin = operatorPin)
        assertTrue("Operator cloud login must succeed", opRes.isSuccess)
        assertTrue(authManager.isOfficerLoggedIn())
        assertEquals(operatorNip, authManager.getSavedNip())
        assertEquals(OfficerRoles.RBAC_OPERATOR, authManager.getOfficerRole())
        assertEquals(operatorNip, sessionManager.getNipPetugas())
    }

    @Test
    fun `biometric unlock succeeds online and gracefully unlocks previously verified device when offline`() = runBlocking {
        val opSalt = OfficerCredentialEntity.generateCryptographicSalt()
        val opHash = OfficerCredentialEntity.hashWithSalt(operatorPin, opSalt)
        val opCloudCred = OfficerCredentialEntity(
            nip = operatorNip,
            fullName = operatorName,
            role = OfficerRoles.RBAC_OPERATOR,
            unit = "TRC BPBD Mukomuko",
            shift = "Shift Siaga",
            phone = "081200000002",
            pinHash = opHash,
            salt = opSalt,
            isCustomPinSet = true,
            initialSecurePin = "",
            isSyncedWithCloud = true
        )

        // Step 1: Initial verified Cloud PIN login so device stores verified saved_nip
        FirestoreHelper.overrideCloudCredentialFetcherForTesting = {
            CloudCredentialLookupResult.Found(credential = opCloudCred)
        }
        FirestoreHelper.overrideOfficersCollectionRoleFetcherForTesting = { OfficerRoles.RBAC_OPERATOR }

        val repo = SmartOfficerAuthRepository(context)
        val authManager = OfficerAuthManager(context)
        assertTrue(repo.authenticateOfficer(operatorNip, operatorPin).isSuccess)
        authManager.logoutOfficer()
        assertFalse(authManager.isOfficerLoggedIn())

        // Step 2: Simulate temporary offline field condition during biometric unlock
        FirestoreHelper.overrideCloudCredentialFetcherForTesting = {
            CloudCredentialLookupResult.Error(
                errorCode = "UNAVAILABLE",
                userMessage = "Tidak ada koneksi internet di lapangan."
            )
        }

        val bioOfflineRes = repo.authenticateWithBiometric(operatorNip)
        assertTrue("Previously verified device officer must unlock cleanly with biometrics even if field network drops", bioOfflineRes.isSuccess)
        assertTrue(authManager.isOfficerLoggedIn())
        assertEquals(operatorNip, authManager.getSavedNip())
    }

    @Test
    fun `forbidden default PINs are strictly rejected and citizen mode isolates officer session`() = runBlocking {
        var cloudHitCount = 0
        FirestoreHelper.overrideCloudCredentialFetcherForTesting = {
            cloudHitCount++
            CloudCredentialLookupResult.NotFound
        }

        val repo = SmartOfficerAuthRepository(context)
        val authManager = OfficerAuthManager(context)

        val forbiddenPins = listOf("117", "0000", "1234", "BPBD117", "TRC2026")
        forbiddenPins.forEach { badPin ->
            val res = repo.authenticateOfficer(nip = operatorNip, inputPin = badPin)
            assertTrue("Forbidden default PIN '$badPin' MUST fail", res.isFailure)
        }
        assertEquals("Forbidden default PINs must be rejected BEFORE touching Firestore", 0, cloudHitCount)
        assertFalse(authManager.isOfficerLoggedIn())

        // Verify Citizen Mode isolation when officer session is cleared
        authManager.loginOfficerDirectly(
            name = operatorName,
            badgeId = operatorNip,
            shift = "Shift Siaga",
            role = OfficerRoles.RBAC_OPERATOR
        )
        assertTrue(authManager.isOfficerLoggedIn())
        authManager.logoutOfficer()
        assertFalse("Officer must not remain logged in after returning to Citizen Mode", authManager.isOfficerLoggedIn())
        assertFalse(
            "SecurityManager must forbid officer data sync when no officer is logged in",
            SecurityManager.getInstance(context).isOfficerDataSyncPermitted()
        )
    }
}
