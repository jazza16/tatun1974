package com.example.ui

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.OfficerAuthManager
import com.example.data.local.OfficerRoles
import com.example.model.MukomukoDistricts
import com.example.ui.components.CitizenEmergencyQuickActionPanel
import com.example.ui.components.MonthlyDisasterAnalyticsSection
import com.example.ui.components.MukomukoDisasterWallpaperDialog
import com.example.ui.components.PoskoLogisticsManagementScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xhdpi")
class LargeFontScaleAndPanicHomeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var officerAuthManager: OfficerAuthManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        officerAuthManager = OfficerAuthManager(context)
        officerAuthManager.clearSavedOfficerIdentity()
    }

    @After
    fun tearDown() {
        officerAuthManager.clearSavedOfficerIdentity()
    }

    @Test
    fun citizenEmergencyQuickActionPanel_atFontScale2x_rendersAll4Tiles_minTouchTarget_andHandlesClicks() {
        var called117 = false
        var openedReport = false
        var openedWarning = false
        var openedGuide = false

        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 2f, fontScale = 2.0f)) {
                MyApplicationTheme {
                    Box(
                        modifier = Modifier
                            .width(360.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        CitizenEmergencyQuickActionPanel(
                            activeAlertCount = 2,
                            hasNearEarthquake = true,
                            onCall117 = { called117 = true },
                            onOpenQuickReport = { openedReport = true },
                            onOpenEarlyWarning = { openedWarning = true },
                            onOpenEvacuationGuide = { openedGuide = true }
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("citizen_emergency_quick_action_panel").assertIsDisplayed()

        val tags = listOf(
            "quick_action_emergency_117",
            "quick_action_report_disaster",
            "quick_action_early_warning",
            "quick_action_evacuation_guide"
        )
        tags.forEach { tag ->
            composeRule.onNodeWithTag(tag)
                .performScrollTo()
                .assertIsDisplayed()
                .assertHeightIsAtLeast(48.dp)
                .performClick()
        }

        assertTrue("Aksi cepat telepon 117 harus terpanggil", called117)
        assertTrue("Aksi cepat lapor bencana harus terpanggil", openedReport)
        assertTrue("Aksi cepat peringatan dini harus terpanggil", openedWarning)
        assertTrue("Aksi cepat panduan evakuasi harus terpanggil", openedGuide)
    }

    @Test
    fun monthlyDisasterAnalyticsSection_atFontScale2x_defaultsToDataRiil_andTogglesBaselineSafely() {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 2f, fontScale = 2.0f)) {
                MyApplicationTheme {
                    Column(
                        modifier = Modifier
                            .width(360.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        MonthlyDisasterAnalyticsSection(
                            reports = emptyList(),
                            currentDistrict = MukomukoDistricts.list.first()
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("analytics_data_source_badge").assertIsDisplayed()
        composeRule.onNodeWithText("DATA RIIL").assertIsDisplayed()
        assertEquals(
            "Label LIVE DATA tidak boleh tampil saat sumber hanya data riil/kosong",
            0,
            composeRule.onAllNodesWithText("LIVE DATA").fetchSemanticsNodes().size
        )
        composeRule.onNodeWithTag("analytics_real_empty_notice").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("analytics_baseline_toggle_chip")
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("BASELINE HISTORIS").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("analytics_baseline_notice_banner").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun mukomukoDisasterPosterDialog_atFontScale2x_rendersZeroPermissionSaveAndShareActions() {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 2f, fontScale = 2.0f)) {
                MyApplicationTheme {
                    MukomukoDisasterWallpaperDialog(onDismiss = {})
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("mukomuko_disaster_wallpaper_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("Poster Edukasi & Lanskap HD").assertIsDisplayed()
        composeRule.onNodeWithTag("btn_save_disaster_poster")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag("btn_share_landscape_info")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun poskoLogisticsManagementScreen_atFontScale2x_rendersDenseSummaryAndStockOpnameControls() {
        officerAuthManager.loginOfficerDirectly(
            name = "HITATUN ARAZAK",
            badgeId = "197401282006041006",
            shift = "Shift Siaga 24 Jam",
            role = OfficerRoles.MANAJER
        )

        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 2f, fontScale = 2.0f)) {
                MyApplicationTheme {
                    Box(modifier = Modifier.width(360.dp)) {
                        PoskoLogisticsManagementScreen(
                            officerName = "HITATUN ARAZAK",
                            officerNip = "197401282006041006",
                            officerRole = OfficerRoles.MANAJER,
                            onClose = {}
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("card_summary_total_refugees").assertIsDisplayed()
        composeRule.onNodeWithTag("card_summary_critical_logistics").assertIsDisplayed()
        composeRule.onNodeWithTag("list_posko_logistics_stock")
            .performScrollToNode(hasTestTag("btn_open_stock_opname_export_dialog"))
        composeRule.onNodeWithTag("btn_open_stock_opname_export_dialog")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
    }
}
