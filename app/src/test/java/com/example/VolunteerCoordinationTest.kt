package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.InitialDummyVolunteers
import com.example.data.local.MukomukoVolunteerPoskoCatalog
import com.example.data.local.VolunteerDatabase
import com.example.data.local.VolunteerPrivacyHelper
import com.example.data.local.VolunteerSkills
import com.example.data.local.VolunteerStatus
import com.example.data.repository.VolunteerRegistrationResult
import com.example.data.repository.VolunteerRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VolunteerCoordinationTest {

    @Test
    fun legalCommunityOrganizations_areRegisteredInPresetsAndRecognizedAsLegalEntities() {
        val presets = MukomukoVolunteerPoskoCatalog.ORGANIZATION_PRESETS
        assertTrue(presets.contains("YAKI INDONESIA"))
        assertTrue(presets.contains("KPS Selagan-Manjunto"))
        assertTrue(presets.contains("FPL (Forum Peduli Lingkungan)"))

        assertTrue(MukomukoVolunteerPoskoCatalog.isLegalIncorporatedOrganization("YAKI INDONESIA"))
        assertTrue(MukomukoVolunteerPoskoCatalog.isLegalIncorporatedOrganization("KPS Selagan-Manjunto"))
        assertTrue(MukomukoVolunteerPoskoCatalog.isLegalIncorporatedOrganization("FPL (Forum Peduli Lingkungan)"))
    }

    @Test
    fun dataPrivacy_masksNikAndPhoneInPublicMode_andRevealsInAdminMode() {
        val rawNik = "1706011408910001"
        val rawPhone = "081278904411"

        val maskedNik = VolunteerPrivacyHelper.maskNik(rawNik, isAdminLoggedIn = false)
        assertEquals("1706********", maskedNik)

        val maskedPhone = VolunteerPrivacyHelper.maskPhone(rawPhone, isAdminLoggedIn = false)
        assertEquals("0812****11", maskedPhone)

        assertEquals(rawNik, VolunteerPrivacyHelper.maskNik(rawNik, isAdminLoggedIn = true))
        assertEquals(rawPhone, VolunteerPrivacyHelper.maskPhone(rawPhone, isAdminLoggedIn = true))
    }

    @Test
    fun nikPrimaryKey_blocksDuplicateNikWithRequiredAlertMessage() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = VolunteerRepository.getInstance(context)
        repository.ensureDummyDataSeeded()

        // Coba daftar dengan NIK yang sudah ada di dummy data (dr. Rahmat Hidayatullah: 1706011408910001)
        val duplicateDummyResult = repository.registerVolunteerValidated(
            fullName = "Pendaftar Duplikat Dummy",
            nik = "1706011408910001",
            phoneNumber = "081299990000",
            organization = "Relawan Individu",
            selectedSkills = listOf(VolunteerSkills.MEDIS),
            homeDistrict = "Kota Mukomuko",
            selfiePhotoUri = null,
            certificatePhotoUri = null
        )

        assertTrue(duplicateDummyResult is VolunteerRegistrationResult.DuplicateNik)
        val alertMessage = (duplicateDummyResult as VolunteerRegistrationResult.DuplicateNik).message
        assertEquals("NIK sudah terdaftar, silakan hubungi admin.", alertMessage)
    }

    @Test
    fun fullLifecycle_registration_verification_assignmentPdf_checkInCheckOut_andDone() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = VolunteerDatabase.getInstance(context)
        val dao = db.volunteerDao()
        val repository = VolunteerRepository.getInstance(context)

        repository.ensureDummyDataSeeded()
        val initialList = dao.getAllVolunteersOnce()
        assertTrue("Minimal 5 relawan dummy harus tersedia", initialList.size >= 5)

        val dummyTemplates = InitialDummyVolunteers.getDummyVolunteers()
        assertEquals(5, dummyTemplates.size)

        // 1. Registrasi Offline-First ke SQLite dengan NIK Primary Key 16 digit unik
        val uniqueNik = "1706072209980077"
        val registered = repository.registerVolunteer(
            fullName = "Rizky Pratama Sikumbang",
            nik = uniqueNik,
            phoneNumber = "081377889900",
            organization = "PMI Kab. Mukomuko",
            selectedSkills = listOf(VolunteerSkills.MEDIS, VolunteerSkills.SAR),
            homeDistrict = "Air Manjunto",
            selfiePhotoUri = "file:///mock/selfie.jpg",
            certificatePhotoUri = "file:///mock/cert.jpg"
        )
        assertEquals(uniqueNik, registered.nik)
        assertEquals(VolunteerStatus.PENDING.code, registered.status)

        // Pastikan pendaftaran kedua dengan NIK yang sama ditolak (Anti Data Ganda)
        val secondAttempt = repository.registerVolunteerValidated(
            fullName = "Rizky Duplikat",
            nik = uniqueNik,
            phoneNumber = "081300001111",
            organization = "Tagana Dinsos",
            selectedSkills = listOf(VolunteerSkills.LOGISTIK),
            homeDistrict = "Penarik",
            selfiePhotoUri = null,
            certificatePhotoUri = null
        )
        assertTrue(secondAttempt is VolunteerRegistrationResult.DuplicateNik)
        assertEquals(
            "NIK sudah terdaftar, silakan hubungi admin.",
            (secondAttempt as VolunteerRegistrationResult.DuplicateNik).message
        )

        // 2. Verifikasi oleh Admin Pusdalops (Pending -> Verified)
        repository.verifyVolunteer(uniqueNik, "HITATUN ARAZAK")
        val fetchedVerified = dao.getVolunteerByNik(uniqueNik)
        assertNotNull(fetchedVerified)
        assertEquals(VolunteerStatus.VERIFIED, fetchedVerified!!.statusEnum())

        // 3. Penugasan ke Posko & Shift -> Otomatis Generate PDF "SURAT TUGAS RELAWAN"
        val pdfFile = repository.assignVolunteerToPosko(
            volunteerId = uniqueNik,
            district = "Ipuh",
            poskoName = "Posko Taktis Pantai Pasar Ipuh",
            shift = "Shift Pagi (08:00 - 16:00 WIB)",
            officerName = "HITATUN ARAZAK"
        )
        assertNotNull("File PDF Surat Tugas harus otomatis dibuat saat Simpan Penugasan", pdfFile)
        assertTrue("File PDF Surat Tugas harus ada di storage", pdfFile!!.exists() && pdfFile.length() > 50)
        val headerString = pdfFile.readBytes().take(8).toByteArray().toString(Charsets.UTF_8)
        assertTrue("File harus berformat PDF valid (%PDF)", headerString.contains("%PDF"))

        val fetchedAssigned = dao.getVolunteerByNik(uniqueNik)
        assertNotNull(fetchedAssigned)
        assertEquals(VolunteerStatus.ASSIGNED, fetchedAssigned!!.statusEnum())
        assertEquals("Ipuh", fetchedAssigned.assignedDistrict)
        assertEquals("Posko Taktis Pantai Pasar Ipuh", fetchedAssigned.assignedPoskoName)
        assertTrue(fetchedAssigned.suratTugasNumber.contains("ST-REL/BPBD-MMK"))
        assertNotNull(fetchedAssigned.suratTugasPdfPath)

        // 4. Klik [ CHECK-IN POSKO ] -> Mencatat timestamp waktu_checkin & status kehadiran real-time
        val checkInTimestamp = 1795222800000L
        val afterCheckIn = repository.checkInPosko(uniqueNik, checkInTimestamp)
        assertNotNull(afterCheckIn)
        assertEquals(checkInTimestamp, afterCheckIn!!.waktuCheckIn)
        assertTrue(afterCheckIn.isCurrentlyCheckedInAtPosko())
        assertTrue(
            afterCheckIn.realtimeAttendanceStatusText()
                .contains("Sedang Bertugas di Posko Taktis Pantai Pasar Ipuh sejak")
        )

        // 5. Klik [ CHECK-OUT POSKO ] -> Mencatat timestamp waktu_checkout
        val checkOutTimestamp = checkInTimestamp + 3_600_000L * 8
        val afterCheckOut = repository.checkOutPosko(uniqueNik, checkOutTimestamp)
        assertNotNull(afterCheckOut)
        assertEquals(checkOutTimestamp, afterCheckOut!!.waktuCheckOut)
        assertFalse(afterCheckOut.isCurrentlyCheckedInAtPosko())
        assertTrue(
            afterCheckOut.realtimeAttendanceStatusText()
                .contains("Telah Check-out dari Posko Taktis Pantai Pasar Ipuh")
        )

        // 6. Selesai Bertugas (Assigned -> Done)
        repository.markAssignmentDone(uniqueNik, "HITATUN ARAZAK")
        val fetchedDone = dao.getVolunteerByNik(uniqueNik)
        assertNotNull(fetchedDone)
        assertEquals(VolunteerStatus.DONE, fetchedDone!!.statusEnum())
    }
}
