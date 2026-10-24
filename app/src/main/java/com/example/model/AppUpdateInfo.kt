package com.example.model

data class VersionChangelog(
    val version: String,
    val releaseDate: String,
    val isCurrent: Boolean = false,
    val isLatest: Boolean = false,
    val highlights: List<String>
)

data class AppUpdateState(
    val currentVersion: String = "1.4.0",
    val latestVersion: String = "1.4.0",
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
    val lastCheckedVersion: String = "1.4.0",
    val lastCheckedTime: String = "Baru saja dicek daring",
    val updateStatusMessage: String = "Aplikasi SIPAGAR KITA menggunakan rilis resmi v1.4.0 terbaru secara permanen.",
    val packageSizeMb: Float = 15.8f,
    val isBannerDismissed: Boolean = false,
    val isSimulationActive: Boolean = false,
    val changelogs: List<VersionChangelog> = listOf(
        VersionChangelog(
            version = "1.4.0",
            releaseDate = "24 Oktober 2026",
            isCurrent = true,
            isLatest = true,
            highlights = listOf(
                "Identitas Baru SIPAGAR KITA: Resmi berganti nama dari Cuaca Mukomuko menjadi SIPAGAR KITA (SIMAK, PAGAR, KITA) sebagai benteng pertahanan & kesiapsiagaan masyarakat Mukomuko.",
                "Edukasi Akronim SIPAGAR KITA: Penjelasan rinci makna SIMAK (Sistem Informasi Maritim, Atmosfer & Kebencanaan Mukomuko), PAGAR (Peringatan Awal, Gelombang, Air & Resiko Mukomuko), dan KITA (Kesiapsiagaan, Iklim, dan Tanggap Bencana Mukomuko) di layar utama dengan tipografi oranye khas.",
                "Sistem Notifikasi Popup Peringatan Dini: Popup otomatis aktif saat ada peringatan cuaca buruk BMKG, gelombang tinggi (>= 2m), atau gempa bumi BMKG InaTEWS WRS-NTWC dengan aksi panggilan cepat Call Center 117 BPBD dan SOP Evakuasi.",
                "Penyegaran Tampilan Wallpaper Startup: Tampilan wallpaper pembuka lebih bersih, fokus, dan modern tanpa elemen header berlebih.",
                "Pembaruan Informasi Pengembang: Nama pengembang resmi diperbarui menjadi tathoenz_arazak dengan kontak Telegram @takajuikgue pada menu informasi aplikasi.",
                "Optimalisasi Desain & Shadow Modern: Tata letak kartu cuaca dipercantik dengan sudut rounded 20-24dp, bayangan lembut elegan, serta standardisasi hierarki status badge."
            )
        ),
        VersionChangelog(
            version = "1.3.0",
            releaseDate = "22 Oktober 2026",
            isCurrent = false,
            isLatest = false,
            highlights = listOf(
                "Konsol Gempa WRS-NTWC InaTEWS BMKG: Tampilan peta geospasial GIS interaktif (Streets, Satelit, Grayscale) dengan animasi gelombang seismik P/S, patahan Megathrust Sunda, status ancaman tsunami nasional, dan tautan resmi inatews.bmkg.go.id/wrs.",
                "Integrasi Tab Kualitas Udara (ISPU): Pemantauan PM2.5, PM10, SO2, CO, O3, NO2 dan himbauan kesehatan masyarakat resmi BMKG.",
                "Peta Shake Map Gempa Terkini Interaktif: Fitur zoom in, zoom out, pan, dan penampil gambar peta guncangan gempa BMKG layar penuh.",
                "Dynamic Weather Icon: Ikon cuaca beranimasi dinamis yang menyesuaikan kondisi cuaca real-time (cerah, berawan, hujan, badai, kabut).",
                "Grafik Kurva Indeks UV Harian: Visualisasi kurva paparan UV dengan penanda waktu paparan sinar UV puncak (10.00 - 14.00 WIB) dan rekomendasi perlindungan diri.",
                "Deteksi Otomatis Lokasi GPS Kecamatan: Deteksi akurat 15 kecamatan di Kabupaten Mukomuko melalui GPS perangkat dengan izin lokasi runtime.",
                "Optimalisasi Mode Pengguna Ponsel Portrait: Kunci orientasi portrait penuh dan tata letak satu kolom vertikal yang responsif & ergonomis.",
                "Animasi Transisi Tab Halus: Transisi navigasi antar tab cuaca, maritim, dan kualitas udara yang lebih lembut dan nyaman."
            )
        ),
        VersionChangelog(
            version = "1.2.0",
            releaseDate = "20 Oktober 2026",
            isCurrent = false,
            isLatest = false,
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
            isCurrent = false,
            isLatest = false,
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
            isCurrent = false,
            isLatest = false,
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
