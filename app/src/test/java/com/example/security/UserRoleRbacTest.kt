package com.example.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pengujian unit menyeluruh untuk Sistem Role-Based Access Control (RBAC) Bertingkat (Multi-Tier)
 * sesuai struktur jabatan resmi BPBD Kabupaten Mukomuko.
 */
class UserRoleRbacTest {

    @Test
    fun tier1_manajer_hasFullAccessIncludingPersonnelSettingsAndDelete() {
        val manajerRoles = listOf("Manajer", "manajer", "  Manajer  ", "Manajer Pusdalops-PB")

        manajerRoles.forEach { role ->
            assertEquals("Role '$role' harus dipetakan ke MANAJER", UserRole.MANAJER, UserRole.fromRoleString(role))
            assertEquals(1, UserRole.fromRoleString(role).tierNumber)
            assertTrue("isManajer('$role') harus true", UserRole.isManajer(role))
            assertFalse("isPengawasOrKoordinator('$role') harus false", UserRole.isPengawasOrKoordinator(role))
            assertFalse("isOperatorOrFieldStaff('$role') harus false", UserRole.isOperatorOrFieldStaff(role))

            // Hak akses penuh Tier 1
            assertTrue("Manajer harus bisa Kelola Personil", UserRole.canManagePersonnel(role))
            assertTrue("Manajer harus bisa Pengaturan Sistem", UserRole.canAccessSystemSettings(role))
            assertTrue("Manajer harus bisa Hapus Laporan", UserRole.canDeleteReports(role))
            assertTrue("Manajer harus bisa Validasi Laporan", UserRole.canValidateReports(role))
            assertTrue("Manajer harus bisa Selesai Laporan", UserRole.canCompleteReports(role))
            assertTrue("Manajer harus bisa lihat Statistik", UserRole.canViewStatistics(role))
            assertTrue("Manajer harus bisa Ekspor Laporan", UserRole.canExportReports(role))
            assertTrue("Manajer harus bisa akses menu operasional", UserRole.canAccessOperationalMenus(role))
        }
    }

    @Test
    fun tier2_pengawasSupervisorKoordAdministrasi_hasHighAccessWithoutPersonnelSettingsOrDelete() {
        val tier2Roles = listOf(
            "Pengawas",
            "Pengawas BPBD",
            "Supervisor",
            "Supervisor BPBD",
            "Koord. Administrasi",
            "Koordinator Administrasi"
        )

        tier2Roles.forEach { role ->
            assertEquals("Role '$role' harus dipetakan ke PENGAWAS_KOORDINATOR", UserRole.PENGAWAS_KOORDINATOR, UserRole.fromRoleString(role))
            assertEquals(2, UserRole.fromRoleString(role).tierNumber)
            assertFalse("isManajer('$role') harus false", UserRole.isManajer(role))
            assertTrue("isPengawasOrKoordinator('$role') harus true", UserRole.isPengawasOrKoordinator(role))
            assertFalse("isOperatorOrFieldStaff('$role') harus false", UserRole.isOperatorOrFieldStaff(role))

            // Menu tersembunyi untuk Tier 2 (eksklusif Manajer)
            assertFalse("Tier 2 ('$role') TIDAK boleh melihat Kelola Personil", UserRole.canManagePersonnel(role))
            assertFalse("Tier 2 ('$role') TIDAK boleh melihat Pengaturan Sistem", UserRole.canAccessSystemSettings(role))
            assertFalse("Tier 2 ('$role') TIDAK boleh melihat Hapus Laporan", UserRole.canDeleteReports(role))

            // Menu aktif untuk Tier 2
            assertTrue("Tier 2 ('$role') HARUS bisa Validasi Laporan", UserRole.canValidateReports(role))
            assertTrue("Tier 2 ('$role') HARUS bisa Selesai Laporan", UserRole.canCompleteReports(role))
            assertTrue("Tier 2 ('$role') HARUS bisa melihat Statistik", UserRole.canViewStatistics(role))
            assertTrue("Tier 2 ('$role') HARUS bisa Ekspor Laporan", UserRole.canExportReports(role))
            assertTrue("Tier 2 ('$role') HARUS bisa akses menu operasional (DALA, Laporan Warga, Peta)", UserRole.canAccessOperationalMenus(role))
        }
    }

    @Test
    fun tier3_operatorLogistikTrcAndNull_hasOperationalOnlyAccess() {
        val tier3Roles: List<String?> = listOf(
            "Operator",
            "Operator Pusdalops",
            "Logistik",
            "Staf Logistik/TRC",
            "TRC",
            "Petugas Lapangan TRC / Logistik",
            "Petugas Jaga Siaga / Piket",
            null,
            "",
            "   ",
            "JabatanTidakDikenal"
        )

        tier3Roles.forEach { role ->
            assertEquals("Role '$role' harus dipetakan ke OPERATOR_LAPANGAN (Tier 3)", UserRole.OPERATOR_LAPANGAN, UserRole.fromRoleString(role))
            assertEquals(3, UserRole.fromRoleString(role).tierNumber)
            assertFalse("isManajer('$role') harus false", UserRole.isManajer(role))
            assertFalse("isPengawasOrKoordinator('$role') harus false", UserRole.isPengawasOrKoordinator(role))
            assertTrue("isOperatorOrFieldStaff('$role') harus true", UserRole.isOperatorOrFieldStaff(role))

            // Menu yang WAJIB disembunyikan untuk Tier 3
            assertFalse("Tier 3 ('$role') TIDAK boleh melihat Kelola Personil", UserRole.canManagePersonnel(role))
            assertFalse("Tier 3 ('$role') TIDAK boleh melihat Pengaturan Sistem", UserRole.canAccessSystemSettings(role))
            assertFalse("Tier 3 ('$role') TIDAK boleh melihat Hapus Laporan", UserRole.canDeleteReports(role))
            assertFalse("Tier 3 ('$role') TIDAK boleh melihat Validasi Laporan", UserRole.canValidateReports(role))
            assertFalse("Tier 3 ('$role') TIDAK boleh melihat Selesai Laporan", UserRole.canCompleteReports(role))
            assertFalse("Tier 3 ('$role') TIDAK boleh melihat Statistik", UserRole.canViewStatistics(role))
            assertFalse("Tier 3 ('$role') TIDAK boleh Ekspor Laporan", UserRole.canExportReports(role))

            // Menu operasional tetap aktif untuk Tier 3
            assertTrue("Tier 3 ('$role') HARUS bisa akses menu operasional (Laporan Warga, Form Harian/DALA, Peta)", UserRole.canAccessOperationalMenus(role))
        }
    }

    @Test
    fun kelolaPersonilButton_strictlyVisibleOnlyForManajer() {
        val allRoles = listOf(
            "Manajer",
            "Pengawas",
            "Supervisor",
            "Koord. Administrasi",
            "Operator",
            "Logistik",
            "TRC",
            null,
            ""
        )

        val rolesWithPersonnelAccess = allRoles.filter { UserRole.canManagePersonnel(it) }
        assertEquals(listOf("Manajer"), rolesWithPersonnelAccess)
    }
}
