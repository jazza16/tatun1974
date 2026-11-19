package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.core.app.ApplicationProvider
import com.example.model.DalaAssessmentState
import com.example.model.DalaDummyDataFactory
import com.example.model.UrgentNeedCategory
import com.example.model.UrgentNeedStatus
import com.example.model.VulnerableGroupCategory
import com.example.viewmodel.DalaAssessmentStateManager
import com.example.viewmodel.DalaAutosaveStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DalaAssessmentTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("bpbd_dala_autosave_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun numericSanitizer_rejectsNonDigitsAndNegatives() {
        assertEquals("12509", DalaAssessmentStateManager.sanitizeDigitsOnly("-12ab50.9"))
        assertEquals("1250", DalaAssessmentStateManager.sanitizeDigitsOnly("-12ab50"))
        assertEquals(1250, DalaAssessmentStateManager.parseNonNegativeInt("Rp 1.250 jiwa"))
        assertEquals(0, DalaAssessmentStateManager.parseNonNegativeInt("abc"))
        assertEquals(2_450_000_000L, DalaAssessmentStateManager.parseNonNegativeLong("Rp 2.450.000.000"))
    }

    @Test
    fun autosavePersistence_restoresStateAcrossRestart() {
        val manager1 = DalaAssessmentStateManager(
            context = context,
            initialOfficerName = "AINUN MARDIATI CANIAGO SPd",
            initialOfficerNip = "198501012010012001",
            initialDistrict = "Kec. Kota Mukomuko"
        )
        manager1.clearFormToEmpty()
        manager1.updateHeaderInfo(disasterTitle = "Banjir Bandang Air Dikit", district = "Kec. Air Dikit")
        manager1.updateCasualties { it.copy(dead = 1, missing = 2, heavyInjured = 4, lightInjured = 9, totalEvacuees = 140) }
        manager1.addEvacuationSite("GOR Desa Air Dikit", 140, "Butuh dapur umum")
        manager1.addOrUpdateVulnerableGroup(VulnerableGroupCategory.BALITA, 18, "Susu formula")
        manager1.updateDamages {
            it.copy(houseHeavy = 7, houseModerate = 15, houseLight = 31, estimatedLossIdr = 1_750_000_000L)
        }
        manager1.addUrgentNeed(UrgentNeedCategory.AIR_BERSIH, 5000, "Liter", UrgentNeedStatus.MENDESAK, "Tangki PDAM")
        manager1.persistNow(markSubmitted = true)

        assertTrue(manager1.autosaveStatus.value is DalaAutosaveStatus.Submitted)

        // Simulasikan aplikasi tertutup & dibuka ulang
        val manager2 = DalaAssessmentStateManager(context = context)
        val restored = manager2.state.value
        assertEquals("Banjir Bandang Air Dikit", restored.disasterTitle)
        assertEquals("Kec. Air Dikit", restored.district)
        assertEquals(16, restored.casualties.totalCasualties)
        assertEquals(140, restored.casualties.totalEvacuees)
        assertEquals(1, restored.casualties.evacuationSites.size)
        assertEquals("GOR Desa Air Dikit", restored.casualties.evacuationSites.first().locationName)
        assertEquals(18, restored.casualties.totalVulnerable)
        assertEquals(53, restored.damages.totalHousesDamaged)
        assertEquals(1_750_000_000L, restored.damages.estimatedLossIdr)
        assertEquals(1, restored.urgentNeeds.size)
        assertEquals(DalaAssessmentState.STATUS_SUBMITTED, restored.submissionStatus)
    }

    @Test
    fun urgentNeeds_toggleStatusAndDeleteWorkCorrectly() {
        val manager = DalaAssessmentStateManager(context = context)
        manager.clearFormToEmpty()
        manager.addUrgentNeed(UrgentNeedCategory.TENDA, 4, "Unit", UrgentNeedStatus.MENDESAK, "Tenda pengungsi")
        val addedId = manager.state.value.urgentNeeds.first().id

        assertEquals(1, manager.state.value.urgentNeedsPendingCount)

        manager.toggleUrgentNeedStatus(addedId)
        assertEquals(UrgentNeedStatus.TERSEDIA, manager.state.value.urgentNeeds.first().status)
        assertEquals(0, manager.state.value.urgentNeedsPendingCount)

        manager.removeUrgentNeed(addedId)
        assertTrue(manager.state.value.urgentNeeds.isEmpty())
    }

    @Test
    fun dalaPdfExportWrapper_rendersBnpbReportPageOnA4Canvas() {
        val sample = DalaDummyDataFactory.createSampleAssessment(
            officerName = "AINUN MARDIATI CANIAGO SPd",
            officerNip = "198501012010012001",
            district = "Kec. Kota Mukomuko"
        )
        val bitmap = Bitmap.createBitmap(595, 842, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        DalaPdfExportWrapper.drawDalaBnpbReportPage(
            context = context,
            canvas = canvas,
            assessment = sample
        )

        assertEquals(595, bitmap.width)
        assertEquals(842, bitmap.height)
    }
}
