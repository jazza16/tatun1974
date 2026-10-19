package com.example.model

data class VersionChangelog(
    val version: String,
    val releaseDate: String,
    val isCurrent: Boolean = false,
    val isLatest: Boolean = false,
    val highlights: List<String>
)

data class AppUpdateState(
    val currentVersion: String = "1.0.0",
    val latestVersion: String = "1.1.0",
    val developerName: String = "Jazzakallah",
    val developerRole: String = "Lead Mobile Developer & UI Engineer",
    val organization: String = "Pusdalops BPBD Kabupaten Mukomuko",
    val developerEmail: String = "jazzakallah@gmail.com",
    val isCheckingOnline: Boolean = false,
    val isUpdateAvailable: Boolean = true,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val isUpdated: Boolean = false,
    val lastCheckedTime: String = "Baru saja",
    val changelogs: List<VersionChangelog> = listOf(
        VersionChangelog(
            version = "1.1.0",
            releaseDate = "17 Oktober 2026",
            isLatest = true,
            highlights = listOf(
                "UI Toggle Mode Gelap & Mode Terang dengan kalibrasi kontras tinggi (WCAG AAA).",
                "Penyempurnaan kontras kartu peringatan cuaca oranye & gempa BMKG di semua kondisi pencahayaan.",
                "Menu pembaruan aplikasi daring (online update) dari versi sebelumnya.",
                "Pembaruan antarmuka dan peningkatan keandalan sistem mitigasi bencana.",
                "Optimalisasi rendering grafik interaktif gelombang pesisir Samudra Hindia."
            )
        ),
        VersionChangelog(
            version = "1.0.0",
            releaseDate = "16 Oktober 2026",
            isCurrent = true,
            highlights = listOf(
                "Prakiraan cuaca lengkap untuk 15 kecamatan se-Kabupaten Mukomuko.",
                "Sistem peringatan cuaca buruk berstatus AWAS, SIAGA, dan WASPADA.",
                "Notifikasi gempa bumi real-time berdasarkan rilis resmi BMKG Indonesia.",
                "Akses panggilan darurat cepat Call Center 117 Pusdalops BPBD Mukomuko.",
                "Visualisasi tinggi gelombang laut & kecepatan angin untuk keselamatan nelayan.",
                "Laporan cuaca langsung dari warga (Community Citizen Weather)."
            )
        ),
        VersionChangelog(
            version = "0.9.0",
            releaseDate = "Awal Oktober 2026",
            highlights = listOf(
                "Versi prarilis: integrasi data cuaca Open-Meteo dan pemetaan koordinat kecamatan Mukomuko.",
                "Pengujian awal notifikasi lokal cuaca ekstrem."
            )
        )
    )
)
