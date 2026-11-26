package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.OfficerAuthManager
import com.example.data.repository.CitizenPreparednessRepository
import com.example.data.repository.GoBagExpiryStatus
import com.example.model.MukomukoDistricts
import com.example.security.PrdOperationalRole
import com.example.security.UserRole
import com.example.siren.FourEyesSirenManager
import com.example.siren.FourEyesSirenState
import com.example.siren.SirenAlertType
import com.example.ui.components.FourEyesSirenControlScreen
import com.example.ui.components.PanduanEvakuasiScreen
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PrdModulesCompletionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var inMemoryDb: AppDatabase
    private lateinit var preparednessRepo: CitizenPreparednessRepository
    private lateinit var fourEyesSirenManager: FourEyesSirenManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        inMemoryDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        preparednessRepo = CitizenPreparednessRepository(inMemoryDb.citizenPreparednessDao())
        fourEyesSirenManager = FourEyesSirenManager.getInstance(context)
        fourEyesSirenManager.clearAllForTesting()
    }

    @After
    fun tearDown() {
        fourEyesSirenManager.clearAllForTesting()
        inMemoryDb.close()
    }

    // ==================== BAB 1: MASTER WILAYAH & RBAC 8 ROLE ====================

    @Test
    fun `mukomuko 15 districts accurately flag 7 coastal tsunami zones and 8 inland zones`() {
        assertEquals(15, MukomukoDistricts.list.size)
        val coastal = MukomukoDistricts.coastalTsunamiDistricts
        assertEquals(7, coastal.size)
        val inlandCount = MukomukoDistricts.list.count { !it.isCoastalTsunamiZone }
        assertEquals(8, inlandCount)

        assertTrue(MukomukoDistricts.isCoastalTsunamiZone("Kota Mukomuko"))
        assertTrue(MukomukoDistricts.isCoastalTsunamiZone("ipuh"))
        assertTrue(MukomukoDistricts.isCoastalTsunamiZone("Air Rami"))
        assertFalse(MukomukoDistricts.isCoastalTsunamiZone("Lubuk Pinang"))
        assertFalse(MukomukoDistricts.isCoastalTsunamiZone("Selagan Raya"))
    }

    @Test
    fun `prd 8 operational roles and four-eyes siren authority mapping are strictly enforced`() {
        assertEquals(8, PrdOperationalRole.entries.size)
        assertEquals(PrdOperationalRole.WARGA, PrdOperationalRole.fromRoleString("Warga"))
        assertEquals(PrdOperationalRole.OPERATOR_PUSDALOPS, PrdOperationalRole.fromRoleString("Operator Pusdalops"))
        assertEquals(PrdOperationalRole.KEPALA_PUSDALOPS, PrdOperationalRole.fromRoleString("Manajer Pusdalops"))
        assertEquals(PrdOperationalRole.KALAK, PrdOperationalRole.fromRoleString("Pengawas / Kalaksa"))
        assertEquals(PrdOperationalRole.TRC, PrdOperationalRole.fromRoleString("Tim Reaksi Cepat (TRC)"))
        assertEquals(PrdOperationalRole.PETUGAS_POSKO, PrdOperationalRole.fromRoleString("Petugas Posko"))
        assertEquals(PrdOperationalRole.PETUGAS_DALA, PrdOperationalRole.fromRoleString("Petugas DaLA"))
        assertEquals(PrdOperationalRole.RELAWAN, PrdOperationalRole.fromRoleString("Relawan PB"))

        // Operator boleh membuat draf, tetapi TIDAK boleh menyetujui aktivasi sirene
        assertTrue(UserRole.canCreateSirenDraft("Operator Pusdalops"))
        assertFalse(UserRole.canApproveSirenActivation("Operator Pusdalops"))

        // Kepala Pusdalops / Manajer & Kalaksa / Pengawas boleh menyetujui aktivasi sirene
        assertTrue(UserRole.canApproveSirenActivation("Manajer Pusdalops"))
        assertTrue(UserRole.canApproveSirenActivation("Kepala Pusdalops"))
        assertTrue(UserRole.canApproveSirenActivation("Pengawas (Kalaksa)"))

        // Warga dan Relawan tidak boleh membuat draf atau menyetujui sirene
        assertFalse(UserRole.canCreateSirenDraft("Warga"))
        assertFalse(UserRole.canApproveSirenActivation("Warga"))
        assertFalse(UserRole.canApproveSirenActivation("Relawan"))
    }

    // ==================== BAB 2: SQLITE GO-BAG & OFFLINE SOP ====================

    @Test
    fun `sqlite auto-seeds 10 mandatory go-bag items and 4 offline evacuation SOPs`() = runBlocking {
        preparednessRepo.ensureSeeded()
        val goBag = preparednessRepo.getGoBagItemsSnapshot()
        val sops = preparednessRepo.getEvacuationSopsSnapshot()

        assertEquals(10, goBag.size)
        assertEquals(4, sops.size)
        assertTrue(sops.any { it.id == "gempa_bumi" })
        assertTrue(sops.any { it.id == "tsunami" && it.rumusUtama.contains("20 - 20 - 20") })
        assertTrue(sops.any { it.id == "banjir" })
        assertTrue(sops.any { it.id == "tanah_longsor" })

        val tsunamiSteps = CitizenPreparednessRepository.parseSopSteps(
            sops.first { it.id == "tsunami" }.langkahJson
        )
        assertEquals(4, tsunamiSteps.size)
    }

    @Test
    fun `go-bag checklist persists checked state, custom items, expiry updates, and custom deletion`() = runBlocking {
        preparednessRepo.ensureSeeded()

        // Toggle item wajib dan pastikan persisten di SQLite
        preparednessRepo.setItemChecked("default_1_air", true)
        assertTrue(preparednessRepo.getGoBagItemsSnapshot().first { it.id == "default_1_air" }.isChecked)

        // Validasi penolakan tanggal tidak valid & nama kosong
        assertTrue(preparednessRepo.addCustomItem("", "Kosong", null).isFailure)
        assertTrue(preparednessRepo.addCustomItem("Susu Bayi", "2 kaleng", "30-06-2027").isFailure)

        // Tambah barang kustom valid
        val added = preparednessRepo.addCustomItem(
            title = "Inhaler Asma",
            description = "Obat darurat pernapasan anak",
            expiryDateIso = "2027-08-15"
        ).getOrThrow()
        assertTrue(added.isCustom)
        assertEquals(11, preparednessRepo.getGoBagItemsSnapshot().size)

        // Perbarui tanggal kedaluwarsa
        assertTrue(preparednessRepo.updateItemExpiry(added.id, "2028-01-10").isSuccess)
        assertEquals("2028-01-10", preparednessRepo.getGoBagItemsSnapshot().first { it.id == added.id }.expiryDateIso)

        // Hapus barang kustom tidak boleh menghapus item default
        preparednessRepo.deleteCustomItem("default_1_air")
        assertEquals(11, preparednessRepo.getGoBagItemsSnapshot().size)

        preparednessRepo.deleteCustomItem(added.id)
        assertEquals(10, preparednessRepo.getGoBagItemsSnapshot().size)
    }

    @Test
    fun `go-bag expiry evaluator classifies expired, expiring soon, safe, and none accurately`() {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val now = System.currentTimeMillis()
        val pastDate = fmt.format(Date(now - TimeUnit.DAYS.toMillis(5)))
        val soonDate = fmt.format(Date(now + TimeUnit.DAYS.toMillis(15)))
        val safeDate = fmt.format(Date(now + TimeUnit.DAYS.toMillis(120)))

        assertEquals(GoBagExpiryStatus.EXPIRED, CitizenPreparednessRepository.evaluateExpiry(pastDate, now).status)
        assertEquals(GoBagExpiryStatus.EXPIRING_SOON, CitizenPreparednessRepository.evaluateExpiry(soonDate, now).status)
        assertEquals(GoBagExpiryStatus.SAFE, CitizenPreparednessRepository.evaluateExpiry(safeDate, now).status)
        assertEquals(GoBagExpiryStatus.NONE, CitizenPreparednessRepository.evaluateExpiry(null, now).status)
    }

    // ==================== BAB 4: SIRENE OTORITAS BERLAPIS (FOUR-EYES) ====================

    @Test
    fun `four-eyes siren enforces DRAFT to MENUNGGU_PERSETUJUAN to DIAKTIFKAN and rejects self-approval`() {
        val operatorNip = "198501012010011001"
        val kepalaNip = "197805122005011002"

        // Tahap 1: Operator membuat DRAFT
        val draft = fourEyesSirenManager.createDraft(
            alertType = SirenAlertType.TSUNAMI,
            title = "Peringatan Dini Tsunami Pesisir Mukomuko",
            message = "Segera evakuasi ke ketinggian minimal 20 meter.",
            targetDistricts = emptyList(),
            creatorNip = operatorNip,
            creatorName = "Operator A",
            creatorRole = "Operator Pusdalops",
            autoSubmitForApproval = false
        ).getOrThrow()

        assertEquals(FourEyesSirenState.DRAFT, draft.status)
        assertEquals(7, draft.targetDistricts.size) // otomatis 7 kecamatan pesisir tsunami
        assertTrue(draft.isCoastalPriority)

        // Tidak boleh langsung diaktifkan sebelum masuk MENUNGGU_PERSETUJUAN
        val prematureApprove = fourEyesSirenManager.approveAndActivate(
            draftId = draft.draftId,
            approverNip = kepalaNip,
            approverName = "Kepala Pusdalops",
            approverRole = "Manajer Pusdalops"
        )
        assertTrue(prematureApprove.isFailure)

        // Tahap 2: Ajukan ke MENUNGGU_PERSETUJUAN
        val submitted = fourEyesSirenManager.submitForApproval(draft.draftId, operatorNip).getOrThrow()
        assertEquals(FourEyesSirenState.MENUNGGU_PERSETUJUAN, submitted.status)

        // GUARD 1: Penolakan otomatis jika pembuat draf mencoba menyetujui drafnya sendiri (bahkan bila jabatannya tinggi)
        val selfApproveAttempt = fourEyesSirenManager.approveAndActivate(
            draftId = draft.draftId,
            approverNip = operatorNip,
            approverName = "Operator A",
            approverRole = "Pengawas / Kalaksa"
        )
        assertTrue(selfApproveAttempt.isFailure)
        assertTrue(selfApproveAttempt.exceptionOrNull() is SecurityException)

        // GUARD 2: Penolakan jika pejabat lain hanya berperan Operator (bukan Kepala Pusdalops / Kalaksa)
        val lowRoleApproveAttempt = fourEyesSirenManager.approveAndActivate(
            draftId = draft.draftId,
            approverNip = "199001012015011009",
            approverName = "Operator B",
            approverRole = "Operator Pusdalops"
        )
        assertTrue(lowRoleApproveAttempt.isFailure)
        assertTrue(lowRoleApproveAttempt.exceptionOrNull() is SecurityException)

        // Tahap 3: Pejabat berbeda dengan otoritas Kepala Pusdalops / Manajer menyetujui -> DIAKTIFKAN
        val activated = fourEyesSirenManager.approveAndActivate(
            draftId = draft.draftId,
            approverNip = kepalaNip,
            approverName = "Kepala Pusdalops",
            approverRole = "Manajer Pusdalops",
            note = "Terverifikasi BMKG & sensor pesisir."
        ).getOrThrow()
        assertEquals(FourEyesSirenState.DIAKTIFKAN, activated.status)
        assertEquals(kepalaNip, activated.approvedByNip)
        assertNotNull(activated.decisionAtMs)

        // Nonaktifkan sirene aktif -> DINONAKTIFKAN
        val stopped = fourEyesSirenManager.deactivateActiveSiren(draft.draftId, "Kepala Pusdalops").getOrThrow()
        assertEquals(FourEyesSirenState.DINONAKTIFKAN, stopped.status)
    }

    @Test
    fun `four-eyes siren draft can be rejected by authorized supervisor`() {
        val draft = fourEyesSirenManager.createDraft(
            alertType = SirenAlertType.EXTREME_WEATHER,
            title = "Peringatan Banjir & Cuaca Ekstrem DAS Selagan",
            message = "Debit naik signifikan.",
            targetDistricts = listOf("Selagan Raya", "Kota Mukomuko"),
            creatorNip = "198501012010011001",
            creatorName = "Operator A",
            creatorRole = "Operator Pusdalops",
            autoSubmitForApproval = true
        ).getOrThrow()

        val rejected = fourEyesSirenManager.rejectDraft(
            draftId = draft.draftId,
            reviewerNip = "197501012000011001",
            reviewerName = "Kalaksa BPBD",
            reviewerRole = "Pengawas",
            reason = "Debit masih batas waspada kuning."
        ).getOrThrow()

        assertEquals(FourEyesSirenState.DITOLAK, rejected.status)
        assertTrue(rejected.decisionNote.contains("batas waspada"))
    }

    // ==================== COMPOSE UI: ISOLASI & TTS / SIREN TEST ====================

    @Test
    fun `four-eyes siren screen is strictly locked in citizen mode`() {
        OfficerAuthManager(context).logoutOfficer()

        composeTestRule.setContent {
            FourEyesSirenControlScreen(
                officerName = "Tamu Warga",
                officerNip = "",
                officerRole = "Warga",
                onClose = {}
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("four_eyes_siren_locked").assertIsDisplayed()
    }

    @Test
    fun `panduan evakuasi screen renders local siren test button and tts controls`() {
        composeTestRule.setContent {
            PanduanEvakuasiScreen(onBackClick = {})
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("panduan_evakuasi_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_test_local_siren_sop").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_tts_all_gempa_bumi").performScrollTo().assertIsDisplayed()
    }
}
