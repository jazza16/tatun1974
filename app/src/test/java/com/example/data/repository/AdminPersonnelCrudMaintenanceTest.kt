package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.CloudCredentialLookupResult
import com.example.data.local.FirestoreHelper
import com.example.data.local.OfficerAuthManager
import com.example.data.local.OfficerCredentialEntity
import com.example.data.local.OfficerProfile
import com.example.data.local.OfficerRoles
import com.example.data.local.OfficialAsnBpbdPersonnel
import com.example.data.local.seedDataAsnMukomuko2026
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AdminPersonnelCrudMaintenanceTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var authManager: OfficerAuthManager
    private lateinit var personnelRepo: PersonnelRepository
    private lateinit var smartAuthRepo: SmartOfficerAuthRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        FirestoreHelper.resetTestHooks()
        SmartOfficerAuthRepository.clearFailedAttemptsForTesting()
        context.getSharedPreferences("bpbd_officer_auth_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        context.getSharedPreferences("PusdalopsSession", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        db = AppDatabase.getInstance(context)
        runBlocking {
            db.clearAllTables()
        }

        // Prevent live network hangs during JVM Robolectric tests
        FirestoreHelper.overrideCloudCredentialFetcherForTesting = { CloudCredentialLookupResult.NotFound }
        FirestoreHelper.overrideCloudCredentialWriterForTesting = { Result.success(Unit) }

        authManager = OfficerAuthManager(context)
        personnelRepo = PersonnelRepository(db.personnelDao())
        smartAuthRepo = SmartOfficerAuthRepository(context)
    }

    @After
    fun tearDown() {
        FirestoreHelper.resetTestHooks()
        SmartOfficerAuthRepository.clearFailedAttemptsForTesting()
    }

    @Test
    fun `admin can input new ASN personnel with full fields across OfficerAuthManager and Room`() = runBlocking {
        personnelRepo.seedDefaultAsnPersonnelIfNeeded(context)
        val initialCount = db.personnelDao().getPersonnelCount()
        assertEquals(OfficialAsnBpbdPersonnel.ALL_OFFICIAL_PERSONNEL.size, initialCount)

        val newNip = "199504122026011009"
        val res = smartAuthRepo.registerOrUpdateManualOfficer(
            fullName = "BUDI SANTOSO, S.Kom",
            nip = "19950412 202601 1 009",
            role = OfficerRoles.OPERATOR,
            unit = "Pusdalops PB Kab. Mukomuko",
            shift = "Regu Siaga A",
            phone = "081277889900"
        )
        assertTrue("Admin input new ASN must succeed: ${res.exceptionOrNull()?.message}", res.isSuccess)

        val rosterMatch = authManager.getOfficerRoster().find { it.nip == newNip }
        assertNotNull("New ASN must appear in OfficerAuthManager roster", rosterMatch)
        assertEquals("BUDI SANTOSO, S.Kom", rosterMatch?.fullName)
        assertEquals(OfficerRoles.OPERATOR, rosterMatch?.role)
        assertEquals("Pusdalops PB Kab. Mukomuko", rosterMatch?.unit)
        assertEquals("Regu Siaga A", rosterMatch?.shift)
        assertEquals("081277889900", rosterMatch?.phone)

        val roomMatch = db.personnelDao().getPersonnelByNip(newNip)
        assertNotNull("New ASN must be persisted in Room personnel table", roomMatch)
        assertEquals("BUDI SANTOSO, S.Kom", roomMatch?.name)
        assertEquals(OfficerRoles.OPERATOR, roomMatch?.role)
        assertEquals("Pusdalops PB Kab. Mukomuko", roomMatch?.unit)
        assertEquals("Regu Siaga A", roomMatch?.shift)
        assertEquals("081277889900", roomMatch?.phone)
        assertEquals(initialCount + 1, db.personnelDao().getPersonnelCount())
    }

    @Test
    fun `admin can edit official ASN to fix Name and NIP typos plus Unit and Shift without duplicates or resurrection`() = runBlocking {
        personnelRepo.seedDefaultAsnPersonnelIfNeeded(context)
        val targetOfficial = OfficialAsnBpbdPersonnel.ALL_OFFICIAL_PERSONNEL.last()
        val oldNip = targetOfficial.nip
        val correctedNip = "198905212026011777"
        val roomBefore = db.personnelDao().getPersonnelByNip(oldNip)
        assertNotNull("Precondition: official ASN must exist in Room", roomBefore)

        val editRes = smartAuthRepo.registerOrUpdateManualOfficer(
            fullName = "NAMA KOREKSI TYPO, S.Sos",
            nip = correctedNip,
            role = OfficerRoles.SUPERVISOR,
            unit = "Bidang Kedaruratan & Logistik",
            shift = "Regu Siaga B",
            phone = "085266554433",
            existingId = "officer_$oldNip",
            previousNip = oldNip,
            existingRoomId = roomBefore!!.id
        )
        assertTrue("Admin edit official ASN must succeed: ${editRes.exceptionOrNull()?.message}", editRes.isSuccess)

        // Old typo NIP must be tombstoned and removed from both Room and OfficerAuthManager
        assertTrue("Old NIP must be tombstoned so default seeder won't resurrect it", authManager.isNipDeleted(oldNip))
        assertNull("Old NIP must not exist in Room personnel", db.personnelDao().getPersonnelByNip(oldNip))
        assertNull("Old NIP must not exist in OfficerAuthManager roster", authManager.getOfficerRoster().find { it.nip == oldNip })

        // Updated record must preserve all edited fields with zero duplicates
        val updatedRoom = db.personnelDao().getPersonnelByNip(correctedNip)
        assertNotNull("Corrected NIP must exist in Room personnel", updatedRoom)
        assertEquals("NAMA KOREKSI TYPO, S.Sos", updatedRoom?.name)
        assertEquals(OfficerRoles.SUPERVISOR, updatedRoom?.role)
        assertEquals("Bidang Kedaruratan & Logistik", updatedRoom?.unit)
        assertEquals("Regu Siaga B", updatedRoom?.shift)
        assertEquals("085266554433", updatedRoom?.phone)
        assertEquals(
            "Total personnel count must remain unchanged (no duplicate)",
            OfficialAsnBpbdPersonnel.ALL_OFFICIAL_PERSONNEL.size,
            db.personnelDao().getPersonnelCount()
        )

        val updatedRoster = authManager.getOfficerRoster().filter { it.nip == correctedNip }
        assertEquals("Corrected NIP must appear exactly once in roster", 1, updatedRoster.size)
        assertEquals("Bidang Kedaruratan & Logistik", updatedRoster.first().unit)
        assertEquals("Regu Siaga B", updatedRoster.first().shift)

        // Re-running startup seeders must NOT resurrect the old typo NIP
        personnelRepo.seedDefaultAsnPersonnelIfNeeded(context)
        seedDataAsnMukomuko2026(context)
        delay(300L)

        assertNull("Old typo NIP must NEVER resurrect in Room after seeding", db.personnelDao().getPersonnelByNip(oldNip))
        assertNull("Old typo NIP must NEVER resurrect in roster after seeding", authManager.getOfficerRoster().find { it.nip == oldNip })
        assertEquals(OfficialAsnBpbdPersonnel.ALL_OFFICIAL_PERSONNEL.size, db.personnelDao().getPersonnelCount())
    }

    @Test
    fun `admin deleting an official ASN for transfer or retirement persists permanently and never resurrects`() = runBlocking {
        personnelRepo.seedDefaultAsnPersonnelIfNeeded(context)
        val retiredAsn = OfficialAsnBpbdPersonnel.ALL_OFFICIAL_PERSONNEL[5]
        val roomEntity = db.personnelDao().getPersonnelByNip(retiredAsn.nip)
        assertNotNull(roomEntity)

        // Seed a local credential cache entry too to verify full cleanup
        db.credentialDao().insertCredential(
            OfficerCredentialEntity(
                nip = retiredAsn.nip,
                fullName = retiredAsn.name,
                role = retiredAsn.role,
                unit = retiredAsn.unit,
                shift = retiredAsn.shift,
                pinHash = "",
                salt = ""
            )
        )

        val deleteRes = smartAuthRepo.deleteOfficerPermanently(
            nip = retiredAsn.nip,
            officerId = "officer_${retiredAsn.nip}",
            roomId = roomEntity!!.id,
            fullName = retiredAsn.name,
            role = retiredAsn.role,
            shift = retiredAsn.shift,
            reason = "Pensiun Tugas BPBD"
        )
        assertTrue("Admin permanent delete must succeed: ${deleteRes.exceptionOrNull()?.message}", deleteRes.isSuccess)

        // Must be gone immediately from OfficerAuthManager, Room personnel, and local credentials
        assertTrue("Deleted NIP must be tombstoned", authManager.isNipDeleted(retiredAsn.nip))
        assertNull("Deleted ASN must not appear in OfficerAuthManager roster", authManager.getOfficerRoster().find { it.nip == retiredAsn.nip })
        assertNull("Deleted ASN must be removed from Room personnel", db.personnelDao().getPersonnelByNip(retiredAsn.nip))
        assertNull("Deleted ASN local credential cache must be removed", db.credentialDao().getCredentialByNip(retiredAsn.nip))
        assertEquals(OfficialAsnBpbdPersonnel.ALL_OFFICIAL_PERSONNEL.size - 1, db.personnelDao().getPersonnelCount())

        // Re-running startup seeders must NOT resurrect the deleted ASN
        personnelRepo.seedDefaultAsnPersonnelIfNeeded(context)
        seedDataAsnMukomuko2026(context)
        delay(300L)
        assertNull("Deleted ASN must NEVER resurrect in Room after startup seed", db.personnelDao().getPersonnelByNip(retiredAsn.nip))
        assertNull("Deleted ASN must NEVER resurrect in roster after startup seed", authManager.getOfficerRoster().find { it.nip == retiredAsn.nip })

        // Stale Cloud snapshot (older timestamp than deletion) must NOT resurrect the deleted ASN
        val deletedTs = authManager.getDeletedTimestamp(retiredAsn.nip) ?: System.currentTimeMillis()
        val staleTimestamp = deletedTs - 10_000L
        authManager.syncRosterFromCloud(
            cloudOfficers = listOf(
                OfficerProfile(
                    id = "officer_${retiredAsn.nip}",
                    fullName = retiredAsn.name,
                    nip = retiredAsn.nip,
                    role = retiredAsn.role,
                    unit = retiredAsn.unit,
                    shift = retiredAsn.shift,
                    phone = retiredAsn.phone
                )
            ),
            deletedCloudNips = emptyMap(),
            cloudUpdatedAtByNip = mapOf(retiredAsn.nip.lowercase() to staleTimestamp)
        )
        assertNull("Stale cloud snapshot must NEVER resurrect locally deleted ASN", authManager.getOfficerRoster().find { it.nip == retiredAsn.nip })

        // Deleted official ASN must also fail first-time activation login
        val loginAttempt = smartAuthRepo.authenticateOfficer(retiredAsn.nip, retiredAsn.nip.takeLast(6))
        assertTrue("Deleted ASN must not activate or authenticate", loginAttempt.isFailure)
    }

    @Test
    fun `re-onboarding previously deleted ASN clears tombstone and maintenance reset restores all defaults`() = runBlocking {
        personnelRepo.seedDefaultAsnPersonnelIfNeeded(context)
        val targetAsn = OfficialAsnBpbdPersonnel.ALL_OFFICIAL_PERSONNEL[3]
        val roomEntity = db.personnelDao().getPersonnelByNip(targetAsn.nip)!!

        // 1. Delete ASN (e.g. pindah tugas sementara)
        val deleteRes = smartAuthRepo.deleteOfficerPermanently(
            nip = targetAsn.nip,
            officerId = "officer_${targetAsn.nip}",
            roomId = roomEntity.id,
            fullName = targetAsn.name,
            role = targetAsn.role,
            shift = targetAsn.shift,
            reason = "ASN Pindah Tugas / Mutasi"
        )
        assertTrue(deleteRes.isSuccess)
        assertTrue(authManager.isNipDeleted(targetAsn.nip))
        assertNull(db.personnelDao().getPersonnelByNip(targetAsn.nip))

        // 2. Admin re-inputs the same NIP (ASN kembali bertugas) -> must clear tombstone cleanly
        val reAddRes = smartAuthRepo.registerOrUpdateManualOfficer(
            fullName = "${targetAsn.name} (Aktif Kembali)",
            nip = targetAsn.nip,
            role = OfficerRoles.OPERATOR,
            unit = "Pusdalops BPBD Kab. Mukomuko",
            shift = "Shift Siaga 24 Jam",
            phone = "081200001111"
        )
        assertTrue("Re-onboarding previously deleted NIP must succeed: ${reAddRes.exceptionOrNull()?.message}", reAddRes.isSuccess)
        assertFalse("Tombstone must be cleared after re-onboarding", authManager.isNipDeleted(targetAsn.nip))
        assertNotNull("Re-onboarded ASN must appear in roster", authManager.getOfficerRoster().find { it.nip == targetAsn.nip })
        assertNotNull("Re-onboarded ASN must appear in Room", db.personnelDao().getPersonnelByNip(targetAsn.nip))

        // 3. Delete another ASN, then trigger Maintenance Reset -> must restore all 37 official ASNs
        val secondAsn = OfficialAsnBpbdPersonnel.ALL_OFFICIAL_PERSONNEL[4]
        val secondRoom = db.personnelDao().getPersonnelByNip(secondAsn.nip)!!
        assertTrue(
            smartAuthRepo.deleteOfficerPermanently(
                nip = secondAsn.nip,
                officerId = "officer_${secondAsn.nip}",
                roomId = secondRoom.id,
                fullName = secondAsn.name,
                role = secondAsn.role,
                shift = secondAsn.shift,
                reason = "Maintenance Aplikasi"
            ).isSuccess
        )
        assertTrue(authManager.isNipDeleted(secondAsn.nip))

        authManager.resetRosterToDefault()
        db.personnelDao().clearAll()
        personnelRepo.seedDefaultAsnPersonnelIfNeeded(context)

        assertFalse("Maintenance reset must clear tombstones", authManager.isNipDeleted(secondAsn.nip))
        assertEquals(OfficialAsnBpbdPersonnel.ALL_OFFICIAL_PERSONNEL.size, db.personnelDao().getPersonnelCount())
        assertEquals(OfficialAsnBpbdPersonnel.ALL_OFFICIAL_PERSONNEL.size, authManager.getOfficerRoster().size)
    }
}
