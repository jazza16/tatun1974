package com.example.security

/**
 * Sistem Role-Based Access Control (RBAC) Bertingkat (Multi-Tier)
 * yang disesuaikan dengan struktur jabatan resmi BPBD Kabupaten Mukomuko.
 *
 * Pemetaan Hak Akses:
 * - Tier 1 (FULL_ACCESS / Manajer):
 *   role == "Manajer" (termasuk "Manajer Pusdalops-PB").
 *   Akses penuh seluruh menu, termasuk "Kelola Personil", "Pengaturan Sistem",
 *   "Validasi Laporan", dan "Hapus Laporan".
 *
 * - Tier 2 (HIGH_ACCESS / Pengawas & Koordinator):
 *   role == "Pengawas" ATAU "Supervisor" ATAU "Koord. Administrasi"
 *   (termasuk "Pengawas BPBD", "Supervisor BPBD").
 *   Akses Dashboard, Laporan Warga, DALA, Statistik, dan tombol "Validasi Laporan".
 *   Menyembunyikan menu "Kelola Personil", "Pengaturan Sistem", dan "Hapus Laporan".
 *
 * - Tier 3 (STANDARD_ACCESS / Operator & Lapangan):
 *   role == "Operator" ATAU "Logistik" ATAU "TRC" ATAU role bernilai null / kosong.
 *   Hanya menampilkan menu operasional (Laporan Warga, Input Form Harian / DALA, Peta).
 *   Menyembunyikan menu Validasi Laporan, Statistik, Kelola Personil, Pengaturan Sistem, dan Hapus Laporan.
 */
enum class UserRole(
    val tierNumber: Int,
    val tierCode: String,
    val tierDisplayName: String,
    val badgeColorHex: Long,
    val canManagePersonnel: Boolean,
    val canAccessSystemSettings: Boolean,
    val canDeleteReports: Boolean,
    val canValidateReports: Boolean,
    val canCompleteReports: Boolean,
    val canViewStatistics: Boolean,
    val canExportReports: Boolean,
    val canAccessOperationalMenus: Boolean
) {
    MANAJER(
        tierNumber = 1,
        tierCode = "TIER 1 • FULL ACCESS",
        tierDisplayName = "Manajer (Full Access)",
        badgeColorHex = 0xFFDC2626,
        canManagePersonnel = true,
        canAccessSystemSettings = true,
        canDeleteReports = true,
        canValidateReports = true,
        canCompleteReports = true,
        canViewStatistics = true,
        canExportReports = true,
        canAccessOperationalMenus = true
    ),
    PENGAWAS_KOORDINATOR(
        tierNumber = 2,
        tierCode = "TIER 2 • HIGH ACCESS",
        tierDisplayName = "Pengawas / Koordinator (High Access)",
        badgeColorHex = 0xFFD97706,
        canManagePersonnel = false,
        canAccessSystemSettings = false,
        canDeleteReports = false,
        canValidateReports = true,
        canCompleteReports = true,
        canViewStatistics = true,
        canExportReports = true,
        canAccessOperationalMenus = true
    ),
    OPERATOR_LAPANGAN(
        tierNumber = 3,
        tierCode = "TIER 3 • STANDARD ACCESS",
        tierDisplayName = "Operator / Lapangan (Standard Operational)",
        badgeColorHex = 0xFF0284C7,
        canManagePersonnel = false,
        canAccessSystemSettings = false,
        canDeleteReports = false,
        canValidateReports = false,
        canCompleteReports = false,
        canViewStatistics = false,
        canExportReports = false,
        canAccessOperationalMenus = true
    );

    companion object {
        /**
         * Memetakan string field `role` (termasuk `null` atau jabatan resmi BPBD Mukomuko)
         * ke tier RBAC [UserRole].
         */
        fun fromRoleString(role: String?): UserRole {
            val normalized = role?.trim().orEmpty()
            if (normalized.isBlank()) {
                return OPERATOR_LAPANGAN
            }

            // Tier 1: Full Access (Khusus Manajer)
            if (isManajer(normalized)) {
                return MANAJER
            }

            // Tier 2: High Access (Pengawas, Supervisor, Koord. Administrasi)
            if (isPengawasOrKoordinator(normalized)) {
                return PENGAWAS_KOORDINATOR
            }

            // Tier 3: Standard Access (Operator, Logistik, TRC, atau fallback standar operasional)
            return OPERATOR_LAPANGAN
        }

        /**
         * Pengecekan sederhana Tier 1: Khusus Manajer.
         * Mengembalikan true jika role == "Manajer" (atau variasi resmi "Manajer Pusdalops-PB").
         */
        fun isManajer(role: String?): Boolean {
            val clean = role?.trim().orEmpty()
            if (clean.isBlank()) return false
            return clean.equals("Manajer", ignoreCase = true) ||
                clean.startsWith("Manajer ", ignoreCase = true) ||
                clean.startsWith("Manajer-", ignoreCase = true) ||
                clean.contains("Manajer", ignoreCase = true)
        }

        /**
         * Pengecekan sederhana Tier 2: Pengawas, Sekretaris, Kabid KL, Kabid RR, Supervisor, atau Koord. Administrasi.
         */
        fun isPengawasOrKoordinator(role: String?): Boolean {
            val clean = role?.trim().orEmpty()
            if (clean.isBlank() || isManajer(clean)) return false
            return clean.equals("Pengawas", ignoreCase = true) ||
                clean.equals("Sekretaris", ignoreCase = true) ||
                clean.equals("Kabid KL", ignoreCase = true) ||
                clean.equals("Kabid RR", ignoreCase = true) ||
                clean.equals("Supervisor", ignoreCase = true) ||
                clean.equals("Koord. Administrasi", ignoreCase = true) ||
                clean.contains("Pengawas", ignoreCase = true) ||
                clean.contains("Sekretaris", ignoreCase = true) ||
                clean.contains("Kabid", ignoreCase = true) ||
                clean.contains("Supervisor", ignoreCase = true) ||
                clean.contains("Koord. Administrasi", ignoreCase = true) ||
                clean.contains("Koordinator Administrasi", ignoreCase = true) ||
                clean.contains("Koord Administrasi", ignoreCase = true)
        }

        /**
         * Pengecekan sederhana Tier 3: Operator, Logistik, TRC, atau role null/kosong.
         */
        fun isOperatorOrFieldStaff(role: String?): Boolean {
            return fromRoleString(role) == OPERATOR_LAPANGAN
        }

        /**
         * Tombol & Menu "Kelola Personil" HANYA boleh tampil untuk Tier 1 (Manajer).
         */
        fun canManagePersonnel(role: String?): Boolean = fromRoleString(role).canManagePersonnel

        /**
         * Menu "Pengaturan Sistem" (Sync/Keamanan Perangkat/Database Explorer) HANYA untuk Tier 1 (Manajer).
         */
        fun canAccessSystemSettings(role: String?): Boolean = fromRoleString(role).canAccessSystemSettings

        /**
         * Tombol "Hapus Laporan" HANYA untuk Tier 1 (Manajer).
         */
        fun canDeleteReports(role: String?): Boolean = fromRoleString(role).canDeleteReports

        /**
         * Tombol "Validasi Laporan" tampil untuk Tier 1 (Manajer) & Tier 2 (Pengawas/Supervisor/Koord. Administrasi).
         * Disembunyikan untuk Tier 3 (Operator/Logistik/TRC/null).
         */
        fun canValidateReports(role: String?): Boolean = fromRoleString(role).canValidateReports

        /**
         * Tombol "Selesai" tampil untuk Tier 1 & Tier 2, disembunyikan untuk Tier 3.
         */
        fun canCompleteReports(role: String?): Boolean = fromRoleString(role).canCompleteReports

        /**
         * Menu Statistik / Tren Bulanan tampil untuk Tier 1 & Tier 2, disembunyikan untuk Tier 3.
         */
        fun canViewStatistics(role: String?): Boolean = fromRoleString(role).canViewStatistics

        /**
         * Ekspor laporan resmi (PDF/CSV) untuk Tier 1 & Tier 2.
         */
        fun canExportReports(role: String?): Boolean = fromRoleString(role).canExportReports

        /**
         * Menu operasional (Laporan Warga, Input Form Harian/DALA, Peta) tersedia untuk semua tier petugas.
         */
        fun canAccessOperationalMenus(role: String?): Boolean = fromRoleString(role).canAccessOperationalMenus

        /**
         * Hak membuat draf aktivasi Sirene Otoritas Berlapis (Four-Eyes Principle):
         * Tersedia untuk seluruh petugas BPBD (Operator Pusdalops, Manajer, Pengawas, Kalaksa).
         */
        fun canCreateSirenDraft(role: String?): Boolean {
            return PrdOperationalRole.fromRoleString(role).canCreateSirenDraft
        }

        /**
         * Hak menyetujui & mengaktifkan Sirene Otoritas Berlapis (Four-Eyes Principle):
         * HANYA untuk Kepala Pusdalops / Manajer (Tier 1) atau Kalaksa / Pengawas / Supervisor (Tier 2).
         * Operator / TRC / Logistik / Warga TIDAK dapat menyetujui aktivasi sirene.
         */
        fun canApproveSirenActivation(role: String?): Boolean {
            val tier = fromRoleString(role)
            if (tier == MANAJER || tier == PENGAWAS_KOORDINATOR) return true
            return PrdOperationalRole.fromRoleString(role).canApproveSirenActivation
        }
    }
}

/**
 * Pemetaan 8 Role Operasional sesuai Master PRD SIPAGAR KITA (BAB 1):
 * `warga`, `operator_pusdalops`, `kepala_pusdalops`, `kalak`, `trc`,
 * `petugas_posko`, `petugas_dala`, dan `relawan`.
 *
 * Setiap role operasional dipetakan ke salah satu tier RBAC [UserRole] agar
 * kompatibel penuh dengan kontrol akses ruang kerja serta otoritas Four-Eyes.
 */
enum class PrdOperationalRole(
    val code: String,
    val displayName: String,
    val isOfficerTier: Boolean,
    val mappedTier: UserRole?,
    val canCreateSirenDraft: Boolean,
    val canApproveSirenActivation: Boolean,
    val canManagePoskoLogistics: Boolean,
    val canManageDalaAssessment: Boolean
) {
    WARGA(
        code = "warga",
        displayName = "Warga / Masyarakat Umum",
        isOfficerTier = false,
        mappedTier = null,
        canCreateSirenDraft = false,
        canApproveSirenActivation = false,
        canManagePoskoLogistics = false,
        canManageDalaAssessment = false
    ),
    OPERATOR_PUSDALOPS(
        code = "operator_pusdalops",
        displayName = "Operator Pusdalops-PB",
        isOfficerTier = true,
        mappedTier = UserRole.OPERATOR_LAPANGAN,
        canCreateSirenDraft = true,
        canApproveSirenActivation = false,
        canManagePoskoLogistics = true,
        canManageDalaAssessment = true
    ),
    KEPALA_PUSDALOPS(
        code = "kepala_pusdalops",
        displayName = "Kepala / Manajer Pusdalops-PB",
        isOfficerTier = true,
        mappedTier = UserRole.MANAJER,
        canCreateSirenDraft = true,
        canApproveSirenActivation = true,
        canManagePoskoLogistics = true,
        canManageDalaAssessment = true
    ),
    KALAK(
        code = "kalak",
        displayName = "Kalaksa / Pengawas BPBD",
        isOfficerTier = true,
        mappedTier = UserRole.PENGAWAS_KOORDINATOR,
        canCreateSirenDraft = true,
        canApproveSirenActivation = true,
        canManagePoskoLogistics = true,
        canManageDalaAssessment = true
    ),
    TRC(
        code = "trc",
        displayName = "Tim Reaksi Cepat (TRC)",
        isOfficerTier = true,
        mappedTier = UserRole.OPERATOR_LAPANGAN,
        canCreateSirenDraft = true,
        canApproveSirenActivation = false,
        canManagePoskoLogistics = true,
        canManageDalaAssessment = true
    ),
    PETUGAS_POSKO(
        code = "petugas_posko",
        displayName = "Petugas Posko & Logistik",
        isOfficerTier = true,
        mappedTier = UserRole.OPERATOR_LAPANGAN,
        canCreateSirenDraft = false,
        canApproveSirenActivation = false,
        canManagePoskoLogistics = true,
        canManageDalaAssessment = false
    ),
    PETUGAS_DALA(
        code = "petugas_dala",
        displayName = "Petugas Kaji Cepat (DaLA)",
        isOfficerTier = true,
        mappedTier = UserRole.OPERATOR_LAPANGAN,
        canCreateSirenDraft = false,
        canApproveSirenActivation = false,
        canManagePoskoLogistics = false,
        canManageDalaAssessment = true
    ),
    RELAWAN(
        code = "relawan",
        displayName = "Relawan Kebencanaan",
        isOfficerTier = false,
        mappedTier = null,
        canCreateSirenDraft = false,
        canApproveSirenActivation = false,
        canManagePoskoLogistics = false,
        canManageDalaAssessment = false
    );

    companion object {
        fun fromRoleString(rawRole: String?): PrdOperationalRole {
            val clean = rawRole?.trim().orEmpty().lowercase()
            if (clean.isBlank()) return OPERATOR_PUSDALOPS
            return when {
                clean.contains("warga") || clean.contains("tamu") || clean.contains("masyarakat") -> WARGA
                clean.contains("relawan") || clean.contains("volunteer") -> RELAWAN
                clean == "kepala_pusdalops" || clean.contains("manajer") || clean.contains("kepala pusdalops") -> KEPALA_PUSDALOPS
                clean == "kalak" || clean.contains("kalaksa") || clean.contains("pengawas") ||
                    clean.contains("sekretaris") || clean.contains("kabid") || clean.contains("supervisor") ||
                    clean.contains("koord") -> KALAK
                clean == "petugas_posko" || clean.contains("posko") || clean.contains("logistik") -> PETUGAS_POSKO
                clean == "petugas_dala" || clean.contains("dala") || clean.contains("kaji cepat") -> PETUGAS_DALA
                clean == "trc" || clean.contains("reaksi cepat") -> TRC
                else -> OPERATOR_PUSDALOPS
            }
        }
    }
}
