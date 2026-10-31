package com.example.model

data class VersionChangelog(
    val version: String,
    val releaseDate: String,
    val isCurrent: Boolean = false,
    val isLatest: Boolean = false,
    val highlights: List<String>
)

data class AppUpdateState(
    val currentVersion: String = "2.0.0",
    val latestVersion: String = "2.0.0",
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
    val lastCheckedVersion: String = "2.0.0",
    val lastCheckedTime: String = "Baru saja dicek daring",
    val updateStatusMessage: String = "Aplikasi SIPAGAR KITA menggunakan rilis resmi v2.0.0 Enterprise Multi-Device Edition terbaru secara permanen.",
    val packageSizeMb: Float = 18.2f,
    val isBannerDismissed: Boolean = false,
    val isSimulationActive: Boolean = false,
    val changelogs: List<VersionChangelog> = listOf(
        VersionChangelog(
            version = "2.0.0",
            releaseDate = "31 Oktober 2026",
            isCurrent = true,
            isLatest = true,
            highlights = listOf(
                "Enterprise Multi-Device Synchronization: Integrasi SyncController terpusat yang menghubungkan hingga 5 perangkat ponsel piket Pusdalops serentak menerima notifikasi audio-alarm dan getaran seketika saat laporan bencana masuk.",
                "Presisi Desain Badge & Status: Standardisasi geometri pill mikro-presisi, kontras optimal, border stroke halus, dan indikator status sinkronisasi (Synced vs Pending) pada seluruh kartu laporan bencana.",
                "Lencana Peran Resmi ASN & Personil: Kode warna peran resmi (Pengawas, Supervisor, Koord. Administrasi, Manajer Pusdalops PB, Operator, Staff Logistik TRC, Piket Posko) lengkap dengan manajemen personil (Tambah, Ubah, Hapus) dan ganti PIN terenkripsi.",
                "Pemantauan Latensi Firestore Real-time: Pengukuran roundtrip latensi dengan ambang batas peringatan jaringan lambat (>3000ms) di dashboard konsol dan status strip.",
                "Antrean Otomatis & Progress Bar Retry: Pemulihan koneksi otomatis dengan exponential backoff dan progress bar visual saat sinkronisasi tertunda.",
                "Pemeriksaan Integritas SHA-256 Harian: Worker verifikasi checksum otomatis antara Room SQLite lokal dan Cloud Firestore untuk mendeteksi serta menyelesaikan inkonsistensi data secara instan.",
                "Database Explorer Room SQLite: Modul inspeksi data tabel Room (personnel, citizen_disaster_reports, personnel_audit_logs, sync_conflict_logs) untuk troubleshooting mandiri administrator BPBD.",
                "Kompatibilitas Lintas Versi Android: Dukungan penuh Android 7.0 hingga Android 16 (API 24 - API 36) dengan pengelolaan saluran notifikasi darurat, izin dinamis POST_NOTIFICATIONS, dan WakeLock siaga."
            )
        ),
        VersionChangelog(
            version = "1.8.0",
            releaseDate = "29 Oktober 2026",
            isCurrent = false,
            isLatest = false,
            highlights = listOf(
                "Penguncian Skala Font Universal: Penerapan fontScale = 1.0f pada seluruh komponen antarmuka aplikasi, mencegah distorsi dan tumpang tindih teks akibat pengaturan skala font sistem ponsel.",
                "Sinkronisasi Basis Data Real-Time Antar-Ponsel: Pembaruan status dan tindak lanjut petugas BPBD langsung tampil secara instan di layar warga tanpa perlu refresh manual.",
                "Kartu Wajib Formulir Laporan: Penambahan kartu terdedikasi 'Deskripsi Kejadian / Jenis Bencana Lainnya' dengan validasi wajib diisi sebelum mengirim laporan.",
                "Proteksi Mode Tamu & Toast Notifikasi: Kartu dan modul khusus petugas membatasi akses pada mode tamu dengan pemberitahuan 'Login dengan ID petugas untuk akses ini'.",
                "Semboyan Polres Mukomuko: Penambahan semboyan resmi 'POLISI BERSAMA MASYARAKAT' pada daftar kontak darurat kepolisian.",
                "Penyederhanaan Layar Login: Tampilan login warga dan petugas dibuat bersih, ringkas, dan fokus tanpa wallpaper berat atau kartu analisis berlebih.",
                "Daftar Kontributor Resmi: Penambahan daftar kontributor aplikasi (M@mi, Afid, Ran_NAM, Erhy, Erick P, Bg-Fir) pada kartu info pengembang.",
                "Peran Resmi Staff Logistik TRC: Pembaruan label peran personil menjadi 'Staff Logistik TRC' dan optimasi presisi badge pada seluruh kartu."
            )
        ),
        VersionChangelog(
            version = "1.7.0",
            releaseDate = "28 Oktober 2026",
            isCurrent = false,
            isLatest = false,
            highlights = listOf(
                "Akses Langsung Tab Konsol Admin BPBD: Penambahan tab terdedikasi 'Konsol Admin' di bilah navigasi utama dan tombol pintasan di bilah atas aplikasi untuk akses tanpa hambatan.",
                "Penyimpanan Permanen Room Database: Perbaikan menyeluruh persistensi personil sehingga seluruh data pendaftaran nama petugas tersimpan secara permanen di Room Database SQLite lokal.",
                "Sinkronisasi Roster Dua Arah Real-time: Integrasi pendaftaran personil baik dari dialog login maupun dashboard konsol dengan pembaruan otomatis tanpa perlu refresh aplikasi.",
                "Login Administrator Master PIN: Akses cepat login administrator menggunakan PIN Master resmi (117 / BPBD117 / ADMIN117) untuk manajemen personil dan penanganan kedaruratan.",
                "Badge Warna Peran Resmi: Tampilan lencana berkode warna untuk Operator (Biru), Piket Posko (Kuning Amber), Staff Logistik TRC (Hijau), dan Manajer Pusdalops PB (Ungu).",
                "Timer Keamanan Auto-Logout 30 Menit: Pemutusan sesi dinas petugas otomatis saat tidak aktif selama 30 menit untuk perlindungan data kedaruratan daerah."
            )
        ),
        VersionChangelog(
            version = "1.6.1",
            releaseDate = "27 Oktober 2026",
            isCurrent = false,
            isLatest = false,
            highlights = listOf(
                "Ekspor Digital Laporan Bencana (PDF & CSV): Ekspor arsip resmi dalam format dokumen PDF bertanda tangan Pusdalops BPBD serta berkas CSV untuk analisis data spreadsheet Excel.",
                "Peta Mini Lokasi Kejadian (Auto-Pin): Peninjauan lokasi presisi laporan warga dengan penanda otomatis dan navigasi rute armada di Petugas Dashboard.",
                "Penyaringan Multi-Kategori Bencana: Filter laporan berdasarkan kategori bencana spesifik (Banjir, Cuaca Ekstrem, Keamanan Pesisir, Longsor, Gempa, dll.).",
                "Visualisasi Grafik Batang Tren Bulanan: Grafik statistik tren bulanan kejadian bencana di Kabupaten Mukomuko.",
                "Modul Arsip Penanganan Tuntas: Pemisahan data laporan berstatus 'Selesai' untuk manajemen arsip tanggap darurat yang efisien.",
                "Info Pasang Surut & Pasang Naik Dinamis: Integrasi data pasang surut maritim pesisir Mukomuko dengan animasi gelombang dinamis dan panduan interaktif nelayan.",
                "Indikator Visual Cuaca Komprehensif: Indikator visual kelembapan udara, kecepatan angin, dan panah putar kompas arah angin di kartu cuaca utama.",
                "Optimalisasi R8/ProGuard & Keamanan Jaringan: Pengurangan ukuran APK dan penguatan konfigurasi keamanan jaringan endpoint BMKG."
            )
        ),
        VersionChangelog(
            version = "1.6.0",
            releaseDate = "26 Oktober 2026",
            isCurrent = false,
            isLatest = false,
            highlights = listOf(
                "Konsol Khusus Petugas Pusdalops BPBD & TRC: Antarmuka terdedikasi bagi petugas jaga Pusdalops dan Tim Reaksi Cepat (TRC) untuk validasi, verifikasi, dan pengelolaan laporan bencana warga secara terpusat.",
                "Dasbor KPI & Metrik Siaga Bencana Real-Time: Pemantauan statistik operasional interaktif (Total Masuk, Menunggu Validasi, Tim Lapangan Diterjunkan, Tuntas Ditangani, dan Akumulasi Korban Jiwa).",
                "Log Tindak Lanjut & Catatan Lapangan TRC: Fitur pencatatan resmi aksi penanganan, pengerahan armada/logistik (perahu karet, tenda darurat, genset), yang tersimpan permanen dan tersinkronisasi daring.",
                "Pemanduan Langsung Google Maps & Kontak Pelapor Cepat: Satu ketukan untuk rute navigasi armada darurat ke koordinat satelit GPS pelapor, disertai panggilan telepon dan WhatsApp instan.",
                "Disposisi Laporan Format Resmi Pusdalops BPBD: Ekspor dan bagikan laporan situasi (Sitrep) lengkap dengan koordinat dan tautan Google Maps langsung ke posko siaga atau grup WhatsApp komando.",
                "Sinkronisasi Multi-Device Cloud Real-Time: Sinkronisasi instan status laporan (Diproses -> Divalidasi -> Selesai) antar perangkat ponsel warga, relawan, dan posko induk BPBD Kabupaten Mukomuko.",
                "Autentikasi Keamanan PIN Petugas Jaga: Perlindungan akses perubahan status dan log TRC menggunakan PIN resmi terverifikasi (117 / BPBD117 / TRC2026)."
            )
        ),
        VersionChangelog(
            version = "1.5.0",
            releaseDate = "25 Oktober 2026",
            isCurrent = false,
            isLatest = false,
            highlights = listOf(
                "Tab Laporan Warga Bersebelahan dengan Prakiraan Cuaca: Navigasi cepat tepat di samping tab Prakiraan Cuaca untuk pelaporan insiden darurat dan bencana secara instan.",
                "Formulir Pelaporan Bencana Terpadu & Geotag Anti-Hoax: Pencatatan data bencana lengkap (jenis bencana, waktu WIB, lokasi detail, 15 kecamatan & desa/kelurahan Mukomuko, data korban jiwa meninggal/luka/hilang) dengan kunci koordinat GPS satelit ponsel.",
                "Peluncur Kamera Langsung (Live Camera Launcher): Ambil foto kondisi kejadian langsung di lokasi dengan stempel digital watermark GPS resmi BPBD Mukomuko.",
                "Peta Spasial GIS Interaktif Laporan Warga: Pemetaan pin kejadian real-time dengan filter kategori (Banjir, Angin Kencang, Gelombang/Abrasi, Longsor, Infrastruktur) dan tombol navigasi langsung ke Google Maps.",
                "Layer Overlay Titik Rawan Bencana BPBD Mukomuko: Pemantauan korelasi sebaran aduan warga dengan zona risiko tinggi (Zona Pesisir Abrasi Koto Jaya - Teramang Jaya, DAS Manjunto & Selagan, Perbukitan V Koto & Malin Deman, Pesisir Ipuh & Air Rami).",
                "Sistem Pelacakan Status Laporan & Notifikasi Toast: Indikator status berwarna (Diproses, Divalidasi, Selesai) pada tabel riwayat laporan dengan aksi pembaruan status interaktif dan notifikasi toast konfirmasi.",
                "Direktori 'Kontak Penting' Darurat Mukomuko: Akses panggilan cepat satu ketukan ke Call Center Bebas Pulsa 117 BPBD, Pusdalops BPBD (0737-71112), Polres 110, RSUD Mukomuko IGD 24 Jam, Damkar 113, Basarnas 115, PMI, dan Polsek se-Mukomuko.",
                "Optimalisasi Tombol Aksi Cepat (Speed Dial FAB): Tombol aksi cepat Material 3 yang 100% responsif terhadap ketukan tanpa konflik gestur, dilengkapi tombol alih posisi jempol (kiri/kanan) dan pintasan ke Laporan Warga.",
                "Caching Offline Room Database: Dukungan penyimpanan offline prakiraan cuaca dan riwayat aduan warga untuk kelancaran akses di daerah pesisir Mukomuko dengan sinyal terbatas."
            )
        ),
        VersionChangelog(
            version = "1.4.0",
            releaseDate = "24 Oktober 2026",
            isCurrent = false,
            isLatest = false,
            highlights = listOf(
                "Identitas Baru SIPAGAR KITA: Resmi berganti nama dari Cuaca Mukomuko menjadi SIPAGAR KITA (SIMAK, PAGAR, KITA) sebagai benteng pertahanan & kesiapsiagaan masyarakat Mukomuko.",
                "Edukasi Akronim SIPAGAR KITA: Penjelasan rinci makna SIMAK (Sistem Informasi Maritim, Atmosfer & Kebencanaan Mukomuko), PAGAR (Peringatan Awal, Gelombang, Air & Resiko Mukomuko), dan KITA (Kesiapsiagaan, Iklim, dan Tanggap Bencana Mukomuko) di layar utama dengan tipografi oranye khas.",
                "Sistem Notifikasi Popup Peringatan Dini: Popup otomatis aktif saat ada peringatan cuaca buruk BMKG, gelombang tinggi (>= 2m), atau gempa bumi BMKG InaTEWS WRS-NTWC dengan aksi panggilan cepat Call Center 117 BPBD dan SOP Evakuasi.",
                "Penyegaran Tampilan Wallpaper Startup: Tampilan wallpaper pembuka lebih bersih, fokus, dan modern tanpa elemen header berlebih.",
                "Aksi Cepat Portabel & Kompak: Tombol Aksi Cepat kini berukuran minimalis (48dp), dapat digeser (drag & drop) secara bebas ke posisi mana pun di layar, dilengkapi preset docking (Kanan/Kiri Bawah/Tengah), serta urutan aksi yang dapat disesuaikan.",
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
