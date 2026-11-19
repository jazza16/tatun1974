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
    isCurrent = true,
    isLatest = true,
    highlights = listOf(
        "Rilis Perdana SIPAGAR KITA v1.0.0: Aplikasi resmi pemantauan cuaca, maritim, dan kesiapsiagaan bencana Kabupaten Mukomuko.",
        "Prakiraan Cuaca & Peringatan Dini BMKG: Pemantauan cuaca 15 kecamatan, kualitas udara (ISPU), indeks UV, gelombang laut, dan gempa bumi InaTEWS real-time.",
        "Integrasi Peta Interaktif & SOP Evakuasi: Peta geospasial GIS titik rawan bencana, jalur evakuasi mandiri, serta panggilan cepat darurat Call Center 117 BPBD.",
        "Sinkronisasi Data Laporan Warga Real-time: Pelaporan kejadian bencana warga terverifikasi GPS beserta foto langsung yang terhubung ke Ruang Kerja Pusdalops BPBD.",
        "Login Biometrik & Keamanan Tingkat Tinggi: Perlindungan sesi petugas dengan autentikasi biometrik, enkripsi lokal AES-256-GCM, serta isolasi data cloud terproteksi."
    )
)

val RELEASE_HISTORY_ACCORDION_CHANGELOGS: List<VersionChangelog> = listOf(
    VersionChangelog(
        version = "1.0.5",
        releaseDate = "18 November 2026",
        isCurrent = true,
        isLatest = true,
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

val DEFAULT_RELEASE_CHANGELOGS: List<VersionChangelog> = listOf(INITIAL_RELEASE_CHANGELOG)

@Keep
data class AppUpdateState(
    val currentVersion: String = "1.0.0",
    val latestVersion: String = "1.0.0",
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
    val lastCheckedVersion: String = "1.0.0",
    val lastCheckedTime: String = "Baru saja dicek daring",
    val updateStatusMessage: String = "Aplikasi SIPAGAR KITA menggunakan Rilis Resmi (Production Release) v1.0.0 terbaru secara permanen.",
    val packageSizeMb: Float = 20.4f,
    val downloadUrl: String = "https://t.me/takajuikgue",
    val isBannerDismissed: Boolean = false,
    val isSimulationActive: Boolean = false,
    val changelogs: List<VersionChangelog> = DEFAULT_RELEASE_CHANGELOGS
)
