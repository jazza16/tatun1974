package com.example.model

import androidx.annotation.Keep

@Keep
data class VersionChangelog(
    val version: String,
    val releaseDate: String,
    val isCurrent: Boolean = false,
    val isLatest: Boolean = false,
    val highlights: List<String>
)

val INITIAL_RELEASE_CHANGELOG = VersionChangelog(
    version = "1.0.0",
    releaseDate = "15 November 2026",
    isCurrent = false,
    isLatest = false,
    highlights = listOf(
        "Rilis Perdana SIPAGAR KITA v1.0.0: Aplikasi resmi pemantauan cuaca, maritim, dan kesiapsiagaan bencana Kabupaten Mukomuko.",
        "Prakiraan Cuaca & Peringatan Dini BMKG: Pemantauan cuaca 15 kecamatan, kualitas udara (ISPU), indeks UV, gelombang laut, dan gempa bumi InaTEWS real-time.",
        "Integrasi Peta Interaktif & SOP Evakuasi: Peta geospasial GIS titik rawan bencana, jalur evakuasi mandiri, serta panggilan cepat darurat Call Center 117 BPBD.",
        "Sinkronisasi Data Laporan Warga Real-time: Pelaporan kejadian bencana warga terverifikasi GPS beserta foto langsung yang terhubung ke Ruang Kerja Pusdalops BPBD.",
        "Login Biometrik & Keamanan Tingkat Tinggi: Perlindungan sesi petugas dengan autentikasi biometrik, enkripsi lokal AES-256-GCM, serta isolasi data cloud terproteksi."
    )
)

val LATEST_RELEASE_CHANGELOG = VersionChangelog(
    version = "1.0.10",
    releaseDate = "23 November 2026",
    isCurrent = true,
    isLatest = true,
    highlights = listOf(
        "Perbaikan Reset PIN & Login Cloud: Reset PIN kembali ke 6 digit terakhir NIP kini bebas notifikasi pencabutan sesi palsu dan langsung mengizinkan login ulang ke server tanpa logo loading menggantung.",
        "Optimalisasi Performa & Hemat Baterai Mode Administrator: Memutus siklus sinkronisasi berulang daftar personil, memindahkan pemrosesan keamanan ke latar belakang, dan meniadakan lag/freeze saat menggulir Ruang Kerja.",
        "Pemulihan Skema Login, Logout, Biometrik, & Isolasi Mode Warga: Relogin PIN pribadi Cloud dan Biometrik native Android kembali berfungsi optimal, serta Mode Warga selalu mengisolasi sesi agar Ruang Kerja tetap terkunci.",
        "Kompatibilitas Upgrade Langsung & Catatan Rilis Bahasa Indonesia: Pembaruan langsung (in-place update) dari seluruh versi v1.0.0–v1.0.9 tanpa menghapus data atau PIN Cloud, lengkap dengan otomatisasi catatan rilis GitHub berbahasa Indonesia."
    )
)

val RELEASE_HISTORY_ACCORDION_CHANGELOGS: List<VersionChangelog> = listOf(
    LATEST_RELEASE_CHANGELOG,
    VersionChangelog(
        version = "1.0.9",
        releaseDate = "23 November 2026",
        isCurrent = false,
        isLatest = false,
        highlights = listOf(
            "Presisi Tombol Alarm Sirene Darurat: Menyelaraskan ukuran huruf dan tata letak tombol \"SEGERA CEK & MATIKAN ALARM\" dan \"MATIKAN SIRENE\" agar proporsional dan tidak terpotong.",
            "Pemindahan Info Lanskap & Wallpaper HD ke Info Aplikasi: Menyederhanakan tampilan utama dengan tetap mempertahankan akses penuh fitur wallpaper kebencanaan Mukomuko di menu Info Aplikasi.",
            "Perbaikan Stabilitas Audio Sirene (ToneGenerator): Menangani batas waktu penghentian nada alarm agar sirene darurat bekerja stabil tanpa error sistem."
        )
    ),
    VersionChangelog(
        version = "1.0.8",
        releaseDate = "21 November 2026",
        isCurrent = false,
        isLatest = false,
        highlights = listOf(
            "Autentikasi PIN Cloud Ketat (Server-First): Verifikasi PIN petugas langsung ke Cloud Firestore tanpa menyimpan PIN mentah maupun hash PIN di perangkat.",
            "Pendaftaran Personil Manual & Login Lintas Perangkat: Petugas baru yang ditambahkan Administrator melalui Kelola Personil dapat langsung login menggunakan Nama atau NIP dari perangkat lain.",
            "Kontrol Akses Berbasis Peran (RBAC Dinamis) & Penghapusan PIN Default: Pemisahan hak akses Manajer, Pengawas, dan Operator secara dinamis, serta pemblokiran permanen PIN default umum (117)."
        )
    ),
    VersionChangelog(
        version = "1.0.7",
        releaseDate = "20 November 2026",
        isCurrent = false,
        isLatest = false,
        highlights = listOf(
            "Panel Keamanan Perangkat & Sesi (Play Integrity Classic): Pemantauan perangkat aktif petugas, riwayat aktivitas login, verifikasi admin, dan pencabutan akses sesi real-time.",
            "Detak Jantung Perangkat & Alarm Darurat Real-Time: Pemantauan status koneksi perangkat aktif beserta peringatan merah dan sirene otomatis saat laporan bencana baru masuk.",
            "Otorisasi Sandi Master Admin Sisi Server: Perlindungan menu sensitif menggunakan verifikasi server, token sementara 5–15 menit, dan pencatatan Audit Log otomatis."
        )
    ),
    VersionChangelog(
        version = "1.0.6",
        releaseDate = "19 November 2026",
        isCurrent = false,
        isLatest = false,
        highlights = listOf(
            "Kop Laporan PDF Dua Logo Resmi: Penempatan logo BPBD dan lambang resmi Kabupaten Mukomuko (Kapuang Sakti Ratau Batuah) pada laporan PDF cuaca dan bencana.",
            "Catatan Operasional Adaptif & Pemisahan Otoritas Aksi: Tampilan list-detail responsif dengan bantuan ringkasan AI, serta pemisahan tombol Validasi (Operator) dan Selesai (Pimpinan).",
            "Tata Letak 8dp, Peta Responsif, & Badge Bulat Kontak Darurat: Penyeragaman jarak kartu Ruang Kerja, peta adaptif layar lebar, dan tombol panggilan darurat melingkar."
        )
    ),
    VersionChangelog(
        version = "1.0.5",
        releaseDate = "18 November 2026",
        isCurrent = false,
        isLatest = false,
        highlights = listOf(
            "Peningkatan Keamanan Sesi: Kolom Nama Operator dan ID Stasiun kini terkunci otomatis (Read-Only) berdasarkan akun yang login, mencegah manipulasi data petugas.",
            "Optimalisasi Form Laporan: Waktu Komunikasi pada Log BPBD kini terisi otomatis sesuai waktu perangkat saat input laporan.",
            "Perbaikan Fitur GPS: Tombol GPS pada form pelaporan telah diaktifkan. Koordinat (Lintang & Bujur) kini dapat diambil secara otomatis dan akurat."
        )
    ),
    VersionChangelog(
        version = "1.0.4",
        releaseDate = "17 November 2026",
        isCurrent = false,
        isLatest = false,
        highlights = listOf(
            "Pembaruan Kontak Darurat: Mengubah kontak bantuan dari \"Jajaran Polsek\" menjadi \"Polres Mukomuko\" pada menu Pusat Bantuan.",
            "Sinkronisasi Data: Perbaikan delay pada saat pengiriman laporan dari lapangan ke Pusdalops."
        )
    ),
    VersionChangelog(
        version = "1.0.3",
        releaseDate = "17 November 2026",
        isCurrent = false,
        isLatest = false,
        highlights = listOf(
            "Penyempurnaan UI/UX: Perbaikan tata letak formulir pelaporan agar lebih ramah pengguna di layar HP.",
            "Peta Geospasial: Pembaruan data titik rawan bencana."
        )
    ),
    VersionChangelog(
        version = "1.0.2",
        releaseDate = "16 November 2026",
        isCurrent = false,
        isLatest = false,
        highlights = listOf(
            "Perbaikan bug pada saat unggah foto laporan warga.",
            "Peningkatan performa aplikasi (aplikasi tidak mudah force close)."
        )
    ),
    VersionChangelog(
        version = "1.0.1",
        releaseDate = "16 November 2026",
        isCurrent = false,
        isLatest = false,
        highlights = listOf(
            "Perbaikan sistem login biometrik.",
            "Penyesuaian teks pada modul informasi cuaca BMKG."
        )
    ),
    INITIAL_RELEASE_CHANGELOG.copy(isCurrent = false, isLatest = false)
)

val DEFAULT_RELEASE_CHANGELOGS: List<VersionChangelog> = RELEASE_HISTORY_ACCORDION_CHANGELOGS

@Keep
data class AppUpdateState(
    val currentVersion: String = "1.0.10",
    val latestVersion: String = "1.0.10",
    val developerName: String = "tathoenz_arazak",
    val developerRole: String = "Lead Software Engineer & System Architect",
    val organization: String = "Pusdalops BPBD Kabupaten Mukomuko",
    val developerEmail: String = "jazzakallah@gmail.com",
    val developerTelegram: String = "@takajuikgue",
    val isCheckingOnline: Boolean = false,
    val isUpdateAvailable: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val isUpdated: Boolean = true,
    val isPermanentlyInstalled: Boolean = true,
    val lastCheckedVersion: String = "1.0.10",
    val lastCheckedTime: String = "Baru saja dicek daring",
    val updateStatusMessage: String = "Aplikasi SIPAGAR KITA menggunakan Rilis Resmi (Production Release) v1.0.10 terbaru secara permanen.",
    val packageSizeMb: Float = 20.4f,
    val downloadUrl: String = "https://github.com/jazza16/tatun1974/releases/latest",
    val isBannerDismissed: Boolean = false,
    val isSimulationActive: Boolean = false,
    val changelogs: List<VersionChangelog> = DEFAULT_RELEASE_CHANGELOGS
)
