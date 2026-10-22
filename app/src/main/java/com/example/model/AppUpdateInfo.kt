package com.example.model

data class VersionChangelog(
    val version: String,
    val releaseDate: String,
    val isCurrent: Boolean = false,
    val isLatest: Boolean = false,
    val highlights: List<String>
)

data class AppUpdateState(
    val currentVersion: String = "1.2.0",
    val latestVersion: String = "1.2.0",
    val developerName: String = "Jazzakallah",
    val developerRole: String = "Lead Mobile Developer & UI Engineer",
    val organization: String = "Pusdalops BPBD Kabupaten Mukomuko",
    val developerEmail: String = "jazzakallah@gmail.com",
    val isCheckingOnline: Boolean = false,
    val isUpdateAvailable: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val isUpdated: Boolean = true,
    val isPermanentlyInstalled: Boolean = true,
    val lastCheckedVersion: String = "1.2.0",
    val lastCheckedTime: String = "Baru saja dicek daring",
    val updateStatusMessage: String = "Aplikasi menggunakan rilis resmi v1.2.0 terbaru secara permanen.",
    val packageSizeMb: Float = 14.8f,
    val isBannerDismissed: Boolean = false,
    val isSimulationActive: Boolean = false,
    val changelogs: List<VersionChangelog> = listOf(
        VersionChangelog(
            version = "1.2.0",
            releaseDate = "20 Oktober 2026",
            isLatest = true,
            highlights = listOf(
                "Sistem Penerimaan & Instalasi Pembaruan Daring Otomatis (OTA In-App Engine) tanpa perlu update offline.",
                "Prakiraan Cuaca 3 Hari terperinci untuk 15 kecamatan di Kabupaten Mukomuko.",
                "Modul peta interaktif titik rawan bencana dan rute evakuasi tsunami/banjir.",
                "Sistem peringatan cuaca buruk waktu nyata berbasis push notification lokal.",
                "Tema terang sebagai tema default dengan palet oranye hangat dan rasio kontras tinggi.",
                "Tata letak LazyVerticalGrid adaptif yang seimbang pada perangkat tablet dan ponsel.",
                "Migrasi otomatis database Room lokal tanpa risiko force close atau kehilangan data."
            )
        ),
        VersionChangelog(
            version = "1.1.0",
            releaseDate = "17 Oktober 2026",
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
