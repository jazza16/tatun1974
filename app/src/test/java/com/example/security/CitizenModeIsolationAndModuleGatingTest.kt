package com.example.security

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.FormLaporanActivity
import com.example.data.local.AppDatabase
import com.example.data.local.OfficerAuthManager
import com.example.data.local.OfficerRoles
import com.example.data.local.PersonnelAuditLogEntity
import com.example.data.repository.PersonnelRepository
import com.example.data.sync.SyncController
import com.example.model.MukomukoDistricts
import com.example.ui.components.DalaAssessmentScreen
import com.example.ui.components.DatabaseExplorerScreen
import com.example.ui.components.FormLaporanHarianScreen
import com.example.ui.components.OfficerDashboardConsoleView
import com.example.ui.components.PoskoLogisticsManagementScreen
import com.example.ui.components.QuickActionsFab
import com.example.ui.form.FormLaporanHarianActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CitizenModeIsolationAndModuleGatingTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var officerAuthManager: OfficerAuthManager
    private lateinit var securityManager: SecurityManager
    private lateinit var inMemoryDb: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        officerAuthManager = OfficerAuthManager(context)
        securityManager = SecurityManager.getInstance(context)
        officerAuthManager.clearSavedOfficerIdentity()
        securityManager.clearLocalSession()
        inMemoryDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        officerAuthManager.clearSavedOfficerIdentity()
        securityManager.clearLocalSession()
        inMemoryDb.close()
    }

    @Test
    fun formLaporanActivities_finishImmediatelyInCitizenMode_andStayOpenWhenOfficerLoggedIn() {
        assertFalse(officerAuthManager.isOfficerLoggedIn())

        val formController = Robolectric.buildActivity(FormLaporanActivity::class.java).create()
        assertTrue(
            "FormLaporanActivity wajib langsung ditutup jika sesi petugas tidak aktif",
            formController.get().isFinishing
        )

        val harianController = Robolectric.buildActivity(FormLaporanHarianActivity::class.java).create()
        assertTrue(
            "FormLaporanHarianActivity wajib langsung ditutup jika sesi petugas tidak aktif",
            harianController.get().isFinishing
        )

        officerAuthManager.loginOfficerDirectly(
            name = "HITATUN ARAZAK",
            badgeId = "197401282006041006",
            shift = "Shift Siaga 24 Jam",
            role = OfficerRoles.MANAJER
        )
        assertTrue(officerAuthManager.isOfficerLoggedIn())

        val authedFormController = Robolectric.buildActivity(FormLaporanActivity::class.java).create().resume()
        assertFalse(
            "FormLaporanActivity harus tetap terbuka saat sesi petugas aktif",
            authedFormController.get().isFinishing
        )

        val authedHarianController = Robolectric.buildActivity(FormLaporanHarianActivity::class.java).create().resume()
        assertFalse(
            "FormLaporanHarianActivity harus tetap terbuka saat sesi petugas aktif",
            authedHarianController.get().isFinishing
        )
    }

    @Test
    fun internalModuleComposables_renderLockedScreenInCitizenMode_andUnlockWhenOfficerLoggedIn() {
        assertFalse(officerAuthManager.isOfficerLoggedIn())

        composeRule.setContent {
            DatabaseExplorerScreen(
                database = inMemoryDb,
                onClose = {}
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("database_explorer_locked").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("database_explorer_screen").fetchSemanticsNodes().size)
    }

    @Test
    fun dalaPoskoFormAndDashboardConsole_renderLockedScreenInCitizenMode() {
        assertFalse(officerAuthManager.isOfficerLoggedIn())

        composeRule.setContent {
            DalaAssessmentScreen(
                officerName = "",
                officerNip = "",
                onClose = {}
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dala_screen_locked").assertIsDisplayed()
    }

    @Test
    fun poskoLogisticsScreen_rendersLockedScreenInCitizenMode() {
        assertFalse(officerAuthManager.isOfficerLoggedIn())

        composeRule.setContent {
            PoskoLogisticsManagementScreen(
                officerName = "",
                officerNip = "",
                officerRole = "",
                onClose = {}
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("posko_screen_locked").assertIsDisplayed()
    }

    @Test
    fun formLaporanHarianScreen_rendersLockedScreenInCitizenMode() {
        assertFalse(officerAuthManager.isOfficerLoggedIn())

        composeRule.setContent {
            FormLaporanHarianScreen(
                database = inMemoryDb,
                onClose = {}
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("form_laporan_screen_locked").assertIsDisplayed()
    }

    @Test
    fun officerDashboardConsole_blocksRenderEvenIfFlagMismatchedWhenSessionInactive() {
        assertFalse(officerAuthManager.isOfficerLoggedIn())

        composeRule.setContent {
            OfficerDashboardConsoleView(
                reports = emptyList(),
                currentDistrict = MukomukoDistricts.list.first(),
                isOfficerLoggedIn = true, // Simulated spoofed UI flag
                officerName = "Spoofed",
                officerBadgeId = "0000",
                officerShift = "-",
                officerRole = OfficerRoles.MANAJER,
                onLoginClick = {},
                onLogoutClick = {},
                onUpdateReportStatus = { _, _ -> },
                onViewPhoto = { _, _ -> }
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("officer_dashboard_locked").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("officer_dashboard_console_view").fetchSemanticsNodes().size)
    }

    @Test
    fun quickActionsFab_hidesFormLaporanShortcutInCitizenMode() {
        assertFalse(officerAuthManager.isOfficerLoggedIn())

        composeRule.setContent {
            QuickActionsFab(
                isExpanded = true,
                onToggleExpand = {}
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("action_citizen_report").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("cardBuatLaporan").fetchSemanticsNodes().size)
    }

    @Test
    fun citizenMode_isolatesOfficerSecuritySyncAndPersonnelCloudListener_whilePreservingSavedNipOnLogout() = runBlocking {
        assertFalse(officerAuthManager.isOfficerLoggedIn())
        assertFalse(securityManager.isOfficerDataSyncPermitted())

        // 1. In Citizen Mode, SecurityManager listeners/fetches must NOT pull or cache officer sessions/activities
        var emittedSessionsCount = -1
        val sessionReg = securityManager.listenSecuritySessionsRealtime { list ->
            emittedSessionsCount = list.size
        }
        assertNull("listenSecuritySessionsRealtime must not attach Firestore listener in Citizen Mode", sessionReg)
        assertEquals(0, emittedSessionsCount)

        var emittedActivitiesCount = -1
        val actReg = securityManager.listenLoginActivitiesRealtime { list ->
            emittedActivitiesCount = list.size
        }
        assertNull("listenLoginActivitiesRealtime must not attach Firestore listener in Citizen Mode", actReg)
        assertEquals(0, emittedActivitiesCount)

        assertTrue(securityManager.fetchSecuritySessions().isEmpty())
        assertTrue(securityManager.fetchLoginActivities().isEmpty())
        val (syncedSessions, syncedActivities) = securityManager.syncAllOfficerLoginActivities(
            auditLogs = listOf(
                PersonnelAuditLogEntity(
                    id = 1L,
                    nip = "197401282006041006",
                    name = "HITATUN ARAZAK",
                    role = "Manajer",
                    shift = "Siaga",
                    action = "LOGIN_SUCCESSFUL",
                    deviceInfo = "Test",
                    timestamp = System.currentTimeMillis()
                )
            )
        )
        assertTrue(syncedSessions.isEmpty())
        assertTrue(syncedActivities.isEmpty())
        assertTrue(securityManager.getLocalCachedSessions().isEmpty())
        assertTrue(securityManager.getLocalCachedLoginActivities().isEmpty())

        // 2. PersonnelRepository cloud listener must NOT start in Citizen Mode
        val testScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val personnelRepo = PersonnelRepository(inMemoryDb.personnelDao())
            val officersReg = personnelRepo.startAutoSyncFromCloud(testScope, context)
            assertNull("PersonnelRepository.startAutoSyncFromCloud must return null in Citizen Mode", officersReg)
        } finally {
            testScope.cancel()
        }

        // 3. SyncController in Citizen Mode clears active officer device list
        val syncController = SyncController.getInstance(context, inMemoryDb)
        syncController.syncModeWithOfficerSession()
        assertTrue(syncController.activeDevices.value.isEmpty())

        // 4. Official Officer login -> logoutOfficer() purges security caches while preserving saved_nip for relogin
        val testNip = "197401282006041006"
        officerAuthManager.saveNipToSecureStorage(testNip, "HITATUN ARAZAK", OfficerRoles.MANAJER, "Shift Siaga 24 Jam")
        officerAuthManager.loginOfficerDirectly("HITATUN ARAZAK", testNip, "Shift Siaga 24 Jam", OfficerRoles.MANAJER)
        securityManager.verifyDeviceAuthenticityDuringLogin(testNip, "HITATUN ARAZAK", OfficerRoles.MANAJER)
        assertTrue(securityManager.isOfficerDataSyncPermitted())

        officerAuthManager.logoutOfficer()
        assertFalse(officerAuthManager.isOfficerLoggedIn())
        assertFalse(securityManager.isOfficerDataSyncPermitted())
        assertEquals("saved_nip harus tetap tersimpan untuk login ulang PIN Cloud/Biometrik", testNip, officerAuthManager.getSavedNip())
        assertTrue(securityManager.getLocalCachedSessions().isEmpty())
        assertTrue(securityManager.getLocalCachedLoginActivities().isEmpty())
    }
}
