package com.example.security

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.example.data.local.OfficerAuthManager

/**
 * Konfigurasi visibilitas menu Dashboard berdasarkan Role-Based Access Control (RBAC) dinamis.
 */
data class RbacDashboardMenuConfig(
    val canonicalRole: String, // "SuperAdmin" atau "Operator"
    val isSuperAdmin: Boolean,
    // Menu Khusus SuperAdmin (Visibility: false / tidak dirender sama sekali untuk Operator)
    val showKelolaPersonil: Boolean,
    val showPengaturanSistem: Boolean,
    val showKonfigurasiInti: Boolean,
    // Menu Operasional (Tampil untuk SuperAdmin maupun Operator)
    val showLaporanWarga: Boolean = true,
    val showDala: Boolean = true,
    val showFormHarian: Boolean = true
)

/**
 * Hasil evaluasi Route Guard ketika pengguna mencoba mengakses rute halaman (termasuk lewat deep link).
 */
data class RbacRouteGuardResult(
    val requestedRoute: String,
    val isAllowed: Boolean,
    val effectiveRoute: String,
    val denialMessage: String? = null
)

/**
 * Sistem Role-Based Access Control (RBAC) Dinamis untuk aplikasi SIPAGAR KITA.
 *
 * Aturan Dinamis (Zero Hardcode):
 * - TIDAK PERNAH melakukan hardcode (menulis mati) NIP atau Nama tertentu di dalam kode aplikasi.
 * - Membaca nilai field `role` secara dinamis dari koleksi `officers` di Cloud Firestore
 *   yang disimpan ke SharedPreferences/SecureStorage lokal saat login berhasil.
 * - Jika field `role` bernilai `'SuperAdmin'`, berikan akses penuh.
 * - Jika bernilai `'Operator'` (atau jika field `role` tidak ada/kosong), batasi aksesnya hanya ke menu operasional.
 */
object OfficerRbacManager {
    const val ROLE_SUPER_ADMIN = "SuperAdmin"
    const val ROLE_OPERATOR = "Operator"

    const val ROUTE_DASHBOARD = "/dashboard"
    const val ROUTE_KELOLA_PERSONIL = "/kelola-personil"
    const val ROUTE_PENGATURAN_SISTEM = "/pengaturan-sistem"
    const val ROUTE_KONFIGURASI_INTI = "/konfigurasi-inti"

    const val ACCESS_DENIED_NOT_SUPERADMIN_MESSAGE = "Akses Ditolak: Anda bukan SuperAdmin"

    private val SUPER_ADMIN_PROTECTED_ROUTES = setOf(
        ROUTE_KELOLA_PERSONIL,
        ROUTE_PENGATURAN_SISTEM,
        ROUTE_KONFIGURASI_INTI
    )

    /**
     * Menormalisasi nilai field `role` secara murni dan dinamis tanpa memeriksa NIP atau Nama.
     * - Jika `rawRole` adalah `'SuperAdmin'` -> mengembalikan `'SuperAdmin'`.
     * - Jika `rawRole` adalah `'Operator'`, kosong, `null` (field tidak ada), atau nilai lain -> mengembalikan `'Operator'`.
     */
    fun normalizeDynamicRole(rawRole: String?): String {
        val cleaned = rawRole?.trim().orEmpty()
        if (cleaned.isEmpty()) return ROLE_OPERATOR
        val compact = cleaned.replace(" ", "").replace("_", "").replace("-", "")
        return if (compact.equals(ROLE_SUPER_ADMIN, ignoreCase = true)) {
            ROLE_SUPER_ADMIN
        } else {
            ROLE_OPERATOR
        }
    }

    /**
     * Memeriksa apakah string role yang diberikan merupakan `'SuperAdmin'`.
     */
    fun isSuperAdminRole(rawRole: String?): Boolean {
        return normalizeDynamicRole(rawRole) == ROLE_SUPER_ADMIN
    }

    /**
     * Memeriksa apakah string role yang diberikan merupakan `'Operator'` (akses terbatas).
     */
    fun isOperatorRole(rawRole: String?): Boolean {
        return normalizeDynamicRole(rawRole) == ROLE_OPERATOR
    }

    /**
     * Mengambil role petugas yang sedang tersimpan di SharedPreferences / SecureStorage lokal,
     * lalu menormalisasinya menjadi `'SuperAdmin'` atau `'Operator'`.
     */
    fun getStoredRbacRole(context: Context): String {
        val authManager = OfficerAuthManager(context)
        val rawStoredRole = authManager.getRawStoredRoleFromPrefs()
        return normalizeDynamicRole(rawStoredRole)
    }

    /**
     * Memeriksa apakah sesi petugas yang tersimpan di SharedPreferences lokal memiliki role `'SuperAdmin'`.
     */
    fun isCurrentSessionSuperAdmin(context: Context): Boolean {
        return getStoredRbacRole(context) == ROLE_SUPER_ADMIN
    }

    /**
     * Mengevaluasi visibilitas menu Dashboard berdasarkan `role` dengan logika `if-else`:
     * - Jika `Role == 'SuperAdmin'`: Tampilkan semua menu (termasuk "Kelola Personil", "Pengaturan Sistem", "Konfigurasi Inti",
     *   serta menu operasional "Laporan Warga", "DALA", "Form Harian").
     * - Jika `Role == 'Operator'`: Sembunyikan (jangan dirender) tombol/menu konfigurasi inti, pengaturan sistem, dan "Kelola Personil".
     *   Hanya tampilkan menu operasional ("Laporan Warga", "DALA", "Form Harian").
     */
    fun evaluateDashboardMenus(rawRole: String?): RbacDashboardMenuConfig {
        val normalizedRole = normalizeDynamicRole(rawRole)
        return if (normalizedRole == ROLE_SUPER_ADMIN) {
            RbacDashboardMenuConfig(
                canonicalRole = ROLE_SUPER_ADMIN,
                isSuperAdmin = true,
                showKelolaPersonil = true,
                showPengaturanSistem = true,
                showKonfigurasiInti = true,
                showLaporanWarga = true,
                showDala = true,
                showFormHarian = true
            )
        } else {
            RbacDashboardMenuConfig(
                canonicalRole = ROLE_OPERATOR,
                isSuperAdmin = false,
                showKelolaPersonil = false,
                showPengaturanSistem = false,
                showKonfigurasiInti = false,
                showLaporanWarga = true,
                showDala = true,
                showFormHarian = true
            )
        }
    }

    /**
     * Mengekstrak path rute kanonik dari string rute atau URI deep link
     * (misal `app://sipagarkita/kelola-personil` atau `/kelola-personil` -> `/kelola-personil`).
     */
    fun normalizeRoutePath(rawRouteOrUri: String?): String {
        val trimmed = rawRouteOrUri?.trim().orEmpty()
        if (trimmed.isBlank()) return ROUTE_DASHBOARD
        val extractedPath = try {
            if (trimmed.contains("://")) {
                val uri = Uri.parse(trimmed)
                val hostPart = uri.host?.takeIf {
                    it.equals("kelola-personil", ignoreCase = true) ||
                        it.equals("pengaturan-sistem", ignoreCase = true) ||
                        it.equals("konfigurasi-inti", ignoreCase = true) ||
                        it.equals("dashboard", ignoreCase = true)
                }?.let { "/$it" }.orEmpty()
                val pathPart = uri.path.orEmpty()
                when {
                    pathPart.isNotBlank() && pathPart != "/" -> pathPart
                    hostPart.isNotBlank() -> hostPart
                    else -> ROUTE_DASHBOARD
                }
            } else {
                trimmed
            }
        } catch (_: Exception) {
            trimmed
        }
        val withLeadingSlash = if (extractedPath.startsWith("/")) extractedPath else "/$extractedPath"
        return withLeadingSlash.lowercase().trimEnd('/').ifBlank { ROUTE_DASHBOARD }
    }

    /**
     * Proteksi Halaman (Route Guard):
     * Jika `Operator` (atau pengguna non-SuperAdmin) mencoba mengakses rute `/kelola-personil`
     * (atau rute konfigurasi inti/pengaturan sistem) secara paksa (misal lewat deep link),
     * tendang kembali ke `/dashboard` dengan pesan `"Akses Ditolak: Anda bukan SuperAdmin"`.
     */
    fun evaluateRouteGuard(
        targetRouteOrDeepLink: String,
        rawRole: String?
    ): RbacRouteGuardResult {
        val normalizedPath = normalizeRoutePath(targetRouteOrDeepLink)
        val normalizedRole = normalizeDynamicRole(rawRole)
        val requiresSuperAdmin = SUPER_ADMIN_PROTECTED_ROUTES.any {
            normalizedPath == it || normalizedPath.startsWith("$it/")
        }

        return if (requiresSuperAdmin && normalizedRole != ROLE_SUPER_ADMIN) {
            RbacRouteGuardResult(
                requestedRoute = normalizedPath,
                isAllowed = false,
                effectiveRoute = ROUTE_DASHBOARD,
                denialMessage = ACCESS_DENIED_NOT_SUPERADMIN_MESSAGE
            )
        } else {
            RbacRouteGuardResult(
                requestedRoute = normalizedPath,
                isAllowed = true,
                effectiveRoute = normalizedPath,
                denialMessage = null
            )
        }
    }

    /**
     * Mengeksekusi Route Guard menggunakan role yang tersimpan di SharedPreferences lokal,
     * dan otomatis menampilkan Toast penolakan apabila akses ditolak.
     */
    fun enforceRouteGuardWithToast(
        context: Context,
        targetRouteOrDeepLink: String,
        overrideRole: String? = null
    ): RbacRouteGuardResult {
        val roleToCheck = overrideRole ?: getStoredRbacRole(context)
        val guardResult = evaluateRouteGuard(targetRouteOrDeepLink, roleToCheck)
        if (!guardResult.isAllowed && !guardResult.denialMessage.isNullOrBlank()) {
            Toast.makeText(context, guardResult.denialMessage, Toast.LENGTH_LONG).show()
        }
        return guardResult
    }
}
