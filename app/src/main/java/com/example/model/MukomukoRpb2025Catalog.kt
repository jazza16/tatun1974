package com.example.model

import androidx.annotation.Keep

/**
 * Katalog Data Resmi Dokumen Rencana Penanggulangan Bencana Daerah (RPB / RPBD)
 * Kabupaten Mukomuko Tahun 2025–2029 (Pemerintah Daerah Kabupaten Mukomuko, 2026).
 *
 * Melengkapi Trilogi Kebijakan Kebencanaan Kabupaten Mukomuko bersama:
 * 1. Dokumen Kajian Risiko Bencana (KRB) Kabupaten Mukomuko Tahun 2026–2030
 * 2. Dokumen Rencana Penanggulangan Bencana (RPB) Kabupaten Mukomuko Tahun 2025–2029
 * 3. Peraturan Daerah Kabupaten Mukomuko Nomor 12 Tahun 2025 tentang RTRW Tahun 2025–2045
 */

@Keep
data class RpbStrategicIssue(
    val number: Int,
    val title: String,
    val priorityScore: Int,
    val summary: String,
    val relatedSasaran: Int
)

@Keep
data class RpbImpactIndicator(
    val code: String,
    val sasaranNumber: Int,
    val sasaranTitle: String,
    val indicatorName: String,
    val unit: String,
    val baseline: String,
    val target2025: String,
    val target2026: String,
    val target2027: String,
    val target2028: String,
    val target2029: String,
    val methodologyNote: String = ""
)

@Keep
data class RpbStrategy(
    val code: String,
    val sasaranNumber: Int,
    val title: String,
    val focusTag: String
)

@Keep
data class RpbPriorityThreat(
    val rank: Int,
    val name: String,
    val riskClass: String,
    val trendLabel: String,
    val areaHa: Double,
    val districtScope: String,
    val exposedPop: Int,
    val economicPhysicalLossMillionRp: Double,
    val envDamageHa: Double
)

@Keep
data class RpbFiscalYearData(
    val year: Int,
    val fiscalCapacityClass: String,
    val disasterSpendingBillionRp: Double,
    val apbdSharePercent: Double,
    val bttMillionRp: Double
)

@Keep
data class RpbHistoricalDisasterStat(
    val hazardName: String,
    val eventCount: Int,
    val highlightNote: String
)

@Keep
data class RpbSpmAchievement(
    val spmCategory: String,
    val serviceName: String,
    val latestPercent: Double,
    val latestYear: Int,
    val previousTrend: String,
    val isCriticalGap: Boolean
)

@Keep
data class RpbIrbiIkdYear(
    val year: Int,
    val irbiScore: Double,
    val ikdScore: Double,
    val note: String
)

@Keep
data class RpbEvacuationShelter(
    val number: Int,
    val siteName: String,
    val districtId: String,
    val districtName: String,
    val facilityType: String
)

@Keep
data class RpbProgramSubActivity(
    val nomenklaturCode: String,
    val opdLead: String,
    val sasaranNumber: Int,
    val subActivityName: String,
    val targetAndBudgetHighlight: String,
    val sipagarSyncNote: String
)

@Keep
data class RpbMonevPhase(
    val phaseName: String,
    val frequency: String,
    val actors: String,
    val outputDeliverable: String
)

object MukomukoRpb2025Catalog {
    const val DOCUMENT_TITLE = "Rencana Penanggulangan Bencana (RPB) Kabupaten Mukomuko Tahun 2025–2029"
    const val DOCUMENT_PUBLISHER = "Pemerintah Daerah Kabupaten Mukomuko • BPBD Kab. Mukomuko (2026)"
    const val DOCUMENT_OFFICIAL = "Kepala Pelaksana BPBD Kab. Mukomuko: Ruri Irwandi, ST, MT."
    const val LEGAL_STATUS = "Dilegalkan melalui Peraturan Bupati tentang RPB Kab. Mukomuko 2025–2029 • Dokumen Induk bagi RPKB, Renkon, Renops, Pemulihan Prabencana & R3P"
    const val GOAL_STATEMENT = "Mewujudkan Kabupaten Mukomuko yang Tangguh, Aman, dan Berkelanjutan melalui Tata Ruang dan Lingkungan Berbasis Risiko Bencana."

    val legalBasisList: List<String> = listOf(
        "UU No. 24 Tahun 2007 tentang Penanggulangan Bencana (Pasal 35–36)",
        "PP No. 21 Tahun 2008 tentang Penyelenggaraan Penanggulangan Bencana (Pasal 6)",
        "Perpres No. 87 Tahun 2020 (RIPB 2020–2044) & Perpres No. 75 Tahun 2021 (Dana Bersama PB)",
        "Permendagri No. 101 Tahun 2018 (SPM Sub-Urusan Bencana) & Permendagri No. 18 Tahun 2025 (SOTK BPBD terbaru)",
        "Perka BNPB No. 1 Tahun 2025 (Renas PB 2025–2029) & Perka BNPB No. 3 Tahun 2026 (Pedoman Penyusunan RPBD)",
        "Kepmendagri No. 900.1-861 Tahun 2026 tentang Pemutakhiran Klasifikasi, Kodefikasi & Nomenklatur Perencanaan",
        "Perda Kab. Mukomuko No. 11 Tahun 2025 (RPJMD 2025–2029) & Perda No. 12 Tahun 2025 (RTRW 2025–2045)"
    )

    // 3 Isu Strategis Prioritas (Subbab 3.6)
    val strategicIssues: List<RpbStrategicIssue> = listOf(
        RpbStrategicIssue(
            number = 1,
            title = "Pengembangan Infrastruktur & Sistem Pendukung Penanggulangan Bencana",
            priorityScore = 176,
            summary = "Penguatan EWS/FFWS, Pusdalops-PB, sistem informasi kebencanaan digital, jaringan jalan/jembatan evakuasi 681,37 Km, 11 titik penampungan pengungsi, dan sarana-prasarana tanggap darurat.",
            relatedSasaran = 1
        ),
        RpbStrategicIssue(
            number = 2,
            title = "Peningkatan Kapasitas Mitigasi, Kesiapsiagaan & Tanggap Darurat",
            priorityScore = 169,
            summary = "Menutup kesenjangan SPM peralatan (7,44%), respons cepat (36,14%), SAR & evakuasi (25%), pembentukan FPRB Kabupaten, Destana/KSB/SPAB inklusif GEDSI, serta TRC PB terlatih.",
            relatedSasaran = 2
        ),
        RpbStrategicIssue(
            number = 3,
            title = "Pengelolaan Pembangunan, Tata Ruang & Lingkungan Berbasis PRB",
            priorityScore = 166,
            summary = "Integrasi KRB–RPB ke dalam pengendalian RTRW/KKPR, rehabilitasi 14 DAS radial, perlindungan LP2B & ekosistem pesisir/mangrove, RTH, serta mitigasi perubahan iklim & penurunan emisi GRK.",
            relatedSasaran = 3
        )
    )

    // 5 Indikator Dampak RPBD 2025–2029 (Tabel 4.1)
    val impactIndicators: List<RpbImpactIndicator> = listOf(
        RpbImpactIndicator(
            code = "1.1",
            sasaranNumber = 1,
            sasaranTitle = "Sasaran 1: Meningkatnya ketersediaan infrastruktur & sistem pendukung PB",
            indicatorName = "Indeks Risiko Bencana (IRB)",
            unit = "Indeks (Turun)",
            baseline = "167,86",
            target2025 = "161,15",
            target2026 = "154,70",
            target2027 = "148,51",
            target2028 = "142,57",
            target2029 = "136,87",
            methodologyNote = "Catatan RPB (Gambar 3.15): IRBI 2025 terkoreksi metodologi terbaru BNPB menjadi 228,28 (dari 164,32 pada 2023–2024); target penurunan bertahap menuju 136,87 pada 2029."
        ),
        RpbImpactIndicator(
            code = "1.2",
            sasaranNumber = 1,
            sasaranTitle = "Sasaran 1: Meningkatnya ketersediaan infrastruktur & sistem pendukung PB",
            indicatorName = "Indeks Kabupaten/Kota Layak Huni",
            unit = "Skor (Naik)",
            baseline = "62,50",
            target2025 = "66,88",
            target2026 = "71,56",
            target2027 = "76,57",
            target2028 = "81,92",
            target2029 = "87,66"
        ),
        RpbImpactIndicator(
            code = "2.1",
            sasaranNumber = 2,
            sasaranTitle = "Sasaran 2: Meningkatnya kapasitas mitigasi, kesiapsiagaan & tanggap darurat",
            indicatorName = "Indeks Ketahanan Daerah (IKD)",
            unit = "Indeks (Naik)",
            baseline = "0,29",
            target2025 = "0,47",
            target2026 = "0,51",
            target2027 = "0,52",
            target2028 = "0,56",
            target2029 = "0,57",
            methodologyNote = "Lonjakan kapasitas hampir 2× lipat dari 0,29 (Rendah) menjadi 0,57 (Sedang–Tangguh) pada 2029."
        ),
        RpbImpactIndicator(
            code = "3.1",
            sasaranNumber = 3,
            sasaranTitle = "Sasaran 3: Meningkatnya penerapan PRB dalam pembangunan, tata ruang & lingkungan",
            indicatorName = "Indeks Kualitas Lingkungan Hidup (IKLH)",
            unit = "Skor (Naik)",
            baseline = "66,20",
            target2025 = "66,86",
            target2026 = "67,53",
            target2027 = "68,21",
            target2028 = "68,89",
            target2029 = "69,58"
        ),
        RpbImpactIndicator(
            code = "3.2",
            sasaranNumber = 3,
            sasaranTitle = "Sasaran 3: Meningkatnya penerapan PRB dalam pembangunan, tata ruang & lingkungan",
            indicatorName = "Penurunan Emisi Gas Rumah Kaca (GRK)",
            unit = "% (Naik)",
            baseline = "28,10%",
            target2025 = "30,35%",
            target2026 = "32,78%",
            target2027 = "35,40%",
            target2028 = "38,23%",
            target2029 = "41,29%"
        )
    )

    // 31 Strategi Pelaksanaan RPB 2025–2029 (Tabel 4.2)
    val strategies: List<RpbStrategy> = listOf(
        // Sasaran 1 (5 Strategi: 1.1 – 1.5)
        RpbStrategy("1.1", 1, "Pembangunan & peningkatan jaringan infrastruktur pengendali banjir, drainase, polder, kolam retensi, serta pengaman pantai/abrasi", "Infrastruktur Mitigasi"),
        RpbStrategy("1.2", 1, "Pengembangan & modernisasi sistem peringatan dini multibahaya (EWS/FFWS, rambu & papan informasi bencana, serta Pusdalops-PB)", "EWS & Pusdalops"),
        RpbStrategy("1.3", 1, "Pembangunan & pemeliharaan aksesibilitas jalan, jembatan evakuasi (681,37 Km) dan 11 lokasi tempat penampungan pengungsi layak & inklusif", "Jalur & Shelter Evakuasi"),
        RpbStrategy("1.4", 1, "Penguatan infrastruktur sistem informasi kebencanaan terpadu (SIPAGAR KITA / Pusdalops digital) & integrasi data risiko multibahaya", "Sistem Informasi Digital"),
        RpbStrategy("1.5", 1, "Penyediaan infrastruktur air bersih, irigasi (Bendung Air Manjunto), sanitasi darurat & ketahanan utilitas dasar saat krisis bencana", "Utilitas & Air Bersih"),

        // Sasaran 2 (14 Strategi: 2.1 – 2.14)
        RpbStrategy("2.1", 2, "Penyusunan, pemutakhiran & legalisasi dokumen perencanaan kebencanaan daerah (KRB, RPB, RPKB, Renkon, Renops, R3P & Rencana Aksi SPM)", "Dokumen Perencanaan"),
        RpbStrategy("2.2", 2, "Pembentukan & penguatan kelembagaan Forum Pengurangan Risiko Bencana (FPRB) Kabupaten Mukomuko berbasis multipihak (Hexahelix)", "Kelembagaan FPRB"),
        RpbStrategy("2.3", 2, "Penyesuaian struktur organisasi, tata kerja & kapasitas SDM BPBD selaras Permendagri No. 18 Tahun 2025", "SOTK BPBD"),
        RpbStrategy("2.4", 2, "Peningkatan komunikasi, informasi & edukasi (KIE) kebencanaan bagi 1.000–2.500 warga/tahun serta integrasi SPAB/anak usia sekolah", "KIE & SPAB"),
        RpbStrategy("2.5", 2, "Pengembangan & penguatan Desa Tangguh Bencana (Destana), Kampung Siaga Bencana (KSB: 5→11 lokasi), dan Relawan Damkar (SKKL)", "Destana & KSB"),
        RpbStrategy("2.6", 2, "Peningkatan kapasitas aparatur, TRC PB (20→200 personel), dan pelatihan gladi kesiapsiagaan rutin (300–700 peserta/tahun)", "TRC PB & Gladi"),
        RpbStrategy("2.7", 2, "Pemenuhan sarana, prasarana & peralatan perlindungan kesiapsiagaan bencana untuk menutup kesenjangan SPM peralatan (7,44%)", "Sarpras & Peralatan SPM"),
        RpbStrategy("2.8", 2, "Optimalisasi aktivasi Sistem Komando Penanganan Darurat Bencana (SKPDB) & percepatan respons cepat darurat (≤1×24 jam)", "SKPDB & Respon Cepat"),
        RpbStrategy("2.9", 2, "Peningkatan kapasitas operasi pencarian, pertolongan & evakuasi korban bencana secara terpadu lintas instansi (Basarnas, TNI/Polri, Relawan)", "SAR & Evakuasi"),
        RpbStrategy("2.10", 2, "Penguatan sistem manajemen logistik, pergudangan darurat & distribusi bantuan kebutuhan dasar secara cepat, tepat & akuntabel", "Logistik Darurat"),
        RpbStrategy("2.11", 2, "Perlindungan khusus kelompok rentan (lansia, balita, ibu hamil, 1.460 warga miskin P3KE & 269 penyandang disabilitas) berbasis prinsip GEDSI", "Inklusi GEDSI"),
        RpbStrategy("2.12", 2, "Penguatan layanan kesehatan darurat, surveilans KLB penyakit pascabencana (DBD melonjak 36 kasus di 2024) & dukungan psikososial", "Kesehatan & Psikososial"),
        RpbStrategy("2.13", 2, "Pelaksanaan kaji cepat kebutuhan pascabencana (JITUPASNA) dan penyusunan Rencana Rehabilitasi & Rekonstruksi Pascabencana (R3P)", "JITUPASNA & R3P"),
        RpbStrategy("2.14", 2, "Diversifikasi & inovasi pendanaan kebencanaan daerah (APBD, BTT, Dana Bersama PB, CSR, hibah RR & sinergi pusat/provinsi)", "Inovasi Pendanaan"),

        // Sasaran 3 (12 Strategi: 3.1 – 3.12)
        RpbStrategy("3.1", 3, "Pengarusutamaan hasil KRB 2026–2030 & RPB 2025–2029 ke dalam RPJMD, RKPD, Renstra OPD & RKPDes di 151 desa/kelurahan", "Mainstreaming PRB"),
        RpbStrategy("3.2", 3, "Penyelarasan & pengendalian pemanfaatan ruang (KKPR, audit tata ruang & TURBINLAK) berbasis zonasi rawan bencana Perda RTRW No. 12/2025", "Pengendalian RTRW & KKPR"),
        RpbStrategy("3.3", 3, "Penegakan aturan sempadan pantai (100 m), sempadan sungai & kawasan lindung geologi untuk mencegah permukiman di zona merah", "Sempadan & Kawasan Lindung"),
        RpbStrategy("3.4", 3, "Penerapan standar bangunan gedung tahan gempa & ramah risiko bencana pada fasilitas publik, sekolah, rumah sakit & permukiman", "Bangunan Tahan Gempa"),
        RpbStrategy("3.5", 3, "Rehabilitasi hutan, lahan kritis & pengelolaan terpadu 14 Daerah Aliran Sungai (DAS) pola radial dari hulu Bukit Barisan hingga muara", "Rehabilitasi 14 DAS"),
        RpbStrategy("3.6", 3, "Perlindungan ekosistem pesisir, rehabilitasi mangrove & vegetasi pantai untuk meredam energi gelombang ekstrem, abrasi & tsunami", "Ekosistem Pesisir"),
        RpbStrategy("3.7", 3, "Pencegahan & pengendalian Karhutla terpadu (Zero Burning, sistem informasi SKIKP real-time & perlindungan lahan gambut/perkebunan)", "Pengendalian Karhutla"),
        RpbStrategy("3.8", 3, "Perlindungan Lahan Pertanian Pangan Berkelanjutan (LP2B/KP2B) & adaptasi sektor pertanian terhadap kekeringan dan dampak perubahan iklim (DPI)", "Ketahanan Pangan & LP2B"),
        RpbStrategy("3.9", 3, "Pengendalian penyakit hewan menular & zoonosis serta keamanan hayati peternakan dan perikanan rakyat", "Pengendalian Zoonosis"),
        RpbStrategy("3.10", 3, "Perluasan Ruang Terbuka Hijau (RTH) perkotaan & penguatan kualitas lingkungan hidup melalui alat pemantau kualitas lingkungan", "RTH & Kualitas Lingkungan"),
        RpbStrategy("3.11", 3, "Pelaksanaan aksi mitigasi perubahan iklim & penurunan emisi GRK daerah dari baseline 28,10% menuju 41,29% pada 2029", "Mitigasi Iklim & GRK"),
        RpbStrategy("3.12", 3, "Penguatan tata kelola pemantauan, evaluasi & pelaporan RPBD secara periodik (tahunan, tengah & akhir periode) selaras RIPB & Renas PB", "Monev RPBD")
    )

    // 8 Ancaman Bencana Prioritas RPB (Subbab 3.3 & Tabel 3.13)
    val priorityThreats: List<RpbPriorityThreat> = listOf(
        RpbPriorityThreat(1, "Cuaca Ekstrem", "Tinggi", "Meningkat", 179_536.76, "15 Kec", 199_429, 6_629_612.14, 0.0),
        RpbPriorityThreat(2, "Kekeringan", "Tinggi", "Meningkat", 414_320.37, "15 Kec", 205_602, 594_963.73, 73_891.08),
        RpbPriorityThreat(3, "Banjir", "Sedang", "Meningkat", 63_820.62, "15 Kec", 63_030, 631_094.55, 987.03),
        RpbPriorityThreat(4, "Gelombang Ekstrem & Abrasi (GEA)", "Sedang", "Meningkat", 2_892.69, "7 Kec", 3_972, 131_691.60, 872.46),
        RpbPriorityThreat(5, "Tanah Longsor", "Sedang", "Meningkat", 211_034.88, "13 Kec", 4_634, 236_940.13, 113_517.27),
        RpbPriorityThreat(6, "Banjir Bandang", "Tinggi", "Tetap", 6_582.78, "7 Kec", 91, 7_156.71, 3_709.71),
        RpbPriorityThreat(7, "Karhutla", "Tinggi", "Tetap", 404_993.70, "15 Kec", 0, 834_437.50, 10_413.09),
        RpbPriorityThreat(8, "Tsunami", "Tinggi", "Tetap", 17_492.58, "8 Kec", 31_122, 2_273_832.42, 155.12)
    )

    // Analisis Pendanaan Kebencanaan BPKAD 2020–2025 (Tabel 3.9)
    val fiscalHistory: List<RpbFiscalYearData> = listOf(
        RpbFiscalYearData(2020, "Sangat Rendah", 3.89, 0.43, 0.0),
        RpbFiscalYearData(2021, "Rendah", 3.56, 0.40, 0.0),
        RpbFiscalYearData(2022, "Sangat Rendah", 3.02, 0.35, 0.0),
        RpbFiscalYearData(2023, "Rendah", 3.03, 0.33, 0.0),
        RpbFiscalYearData(2024, "Sangat Rendah", 3.41, 0.34, 0.0),
        RpbFiscalYearData(2025, "Rendah", 5.13, 0.59, 100.0)
    )
    const val FISCAL_SUMMARY_NOTE =
        "Total Belanja APBD 2025: Rp 863,13 M • Belanja Sub-Urusan Bencana 2025: Rp 5,13 M (0,59% APBD, tumbuh +50,49%) • BTT 2025: Rp 100 Jt (0,01%) • DSP & Hibah RR 2020–2025: Rp 0. Bukti empiris kuat urgensi inovasi digital efisiensi anggaran (Zero APBD) melalui SIPAGAR KITA."

    // Rekapitulasi Sejarah Kejadian Bencana 2014–2024 (Tabel 3.10–3.12, Total 184 Kejadian)
    const val HISTORICAL_TOTAL_EVENTS = 184
    const val HISTORICAL_SOCIAL_IMPACT = "8.057 jiwa menderita • 4.680 jiwa mengungsi • 0 meninggal/hilang/luka (2014–2024)"
    const val HISTORICAL_PHYSICAL_IMPACT = "344 rumah rusak (61 RB, 60 RS, 223 RR; puncak gempa 2016: 301 rumah), 1 satuan pendidikan & 2 jembatan rusak • KLB DBD naik 0 (2020–2021) → 36 kasus (2024)"

    val historicalDisasters: List<RpbHistoricalDisasterStat> = listOf(
        RpbHistoricalDisasterStat("Gempabumi", 82, "Frekuensi tertinggi (44,6%); dampak terparah 2016 (301 rumah rusak)"),
        RpbHistoricalDisasterStat("Banjir", 49, "Tren meningkat signifikan: 11 kejadian (2022) & 12 kejadian (2024)"),
        RpbHistoricalDisasterStat("Karhutla", 20, "Rawan puncak kemarau di perkebunan & lahan gambut (404.993,70 Ha)"),
        RpbHistoricalDisasterStat("Tanah Longsor", 17, "Lonjakan pada 2024 (8 kejadian) di perbukitan hulu & tebing jalan"),
        RpbHistoricalDisasterStat("Cuaca Ekstrem", 12, "Angin puting beliung/hujan badai dengan potensi rugi tertinggi Rp 6,63 T"),
        RpbHistoricalDisasterStat("Gelombang Ekstrem & Abrasi", 3, "Tinggi gelombang Samudra Hindia hingga 2,7 m menggerus 7 kec. pesisir"),
        RpbHistoricalDisasterStat("Kekeringan", 1, "Berdampak luas pada 414.320,37 Ha & sektor pertanian (42,44% PDRB)")
    )

    // Capaian SPM Sub-Urusan Bencana (Subbab 3.4.1)
    val spmAchievements: List<RpbSpmAchievement> = listOf(
        RpbSpmAchievement("SPM 1 • Info Rawan", "KIE Rawan Bencana Masyarakat", 96.02, 2025, "Naik dari 90,14% (2022) & 0% (2023)", false),
        RpbSpmAchievement("SPM 2 • Pencegahan", "Dokumen Rencana Kontingensi (Renkon)", 99.65, 2023, "Capaian tinggi; KRB & RPBD kini telah tuntas disusun", false),
        RpbSpmAchievement("SPM 2 • Pencegahan", "Pengendalian Operasi & Sarpras Pusdalops", 95.86, 2025, "Diperkuat digitalisasi komando SIPAGAR KITA", false),
        RpbSpmAchievement("SPM 2 • Pencegahan", "Gladi & Pelatihan Pencegahan/Mitigasi", 87.00, 2025, "Gladi 88,37% (2023) • Pelatihan 87,00% (2025)", false),
        RpbSpmAchievement("SPM 2 • Pencegahan", "Peralatan Perlindungan & Kesiapsiagaan", 7.44, 2025, "GAP KRITIS: Anjlok dari 52,09% (2022) ke 7,44% (2025)", true),
        RpbSpmAchievement("SPM 3 • Evakuasi", "Aktivasi SKPDB Saat Darurat", 95.29, 2023, "Komando darurat berjalan baik saat kejadian", false),
        RpbSpmAchievement("SPM 3 • Evakuasi", "Respons Cepat Darurat Bencana", 36.14, 2025, "GAP KRITIS: Naik dari 13,24% (2022–2023), masih jauh dari 100%", true),
        RpbSpmAchievement("SPM 3 • Evakuasi", "Pencarian, Pertolongan & Evakuasi (SAR)", 25.00, 2025, "GAP KRITIS: Turun dari 62,30% (2022) ke 25,00% (2025)", true)
    )

    // Tren IRBI & IKD 2021–2025 (Gambar 3.15 & 3.16, BNPB 2026)
    val irbiIkdTrend: List<RpbIrbiIkdYear> = listOf(
        RpbIrbiIkdYear(2021, 179.29, 0.32, "Risiko Tinggi • IKD Rendah"),
        RpbIrbiIkdYear(2022, 179.29, 0.00, "IKD belum terinput/0 pada 2022"),
        RpbIrbiIkdYear(2023, 164.32, 0.42, "IRBI membaik -14,97 poin"),
        RpbIrbiIkdYear(2024, 164.32, 0.00, "IKD belum terinput/0 pada 2024"),
        RpbIrbiIkdYear(2025, 228.28, 0.29, "IRBI terkoreksi pembaruan metodologi BNPB • IKD 0,29 (Rendah)")
    )

    // 11 Lokasi Resmi Tempat Penampungan Pengungsi (Kode 1.06.06.2.01.0007, BPBD, Tabel 5.1)
    val officialEvacuationShelters: List<RpbEvacuationShelter> = listOf(
        RpbEvacuationShelter(1, "Polres Mukomuko", "kota_mukomuko", "Kota Mukomuko", "Fasilitas Keamanan & Posko Utama"),
        RpbEvacuationShelter(2, "Lapangan SDN 08 Kota Mukomuko", "kota_mukomuko", "Kota Mukomuko", "Lapangan & Sarana Pendidikan"),
        RpbEvacuationShelter(3, "RSUD Mukomuko", "air_manjunto", "Air Manjunto", "Fasilitas Kesehatan Rujukan"),
        RpbEvacuationShelter(4, "Kantor Camat Air Dikit", "air_dikit", "Air Dikit", "Fasilitas Pemerintahan Kecamatan"),
        RpbEvacuationShelter(5, "Lapangan Desa Sido Makmur", "teramang_jaya", "Teramang Jaya", "Lapangan Terbuka Evakuasi Desa"),
        RpbEvacuationShelter(6, "SMPN 12 Mukomuko", "sungai_rumbai", "Sungai Rumbai", "Sarana Pendidikan / TEA Selatan"),
        RpbEvacuationShelter(7, "SMAN 11 Mukomuko", "malin_deman", "Malin Deman", "Sarana Pendidikan Dataran Tinggi"),
        RpbEvacuationShelter(8, "Lapangan SDN 06 Selagan Raya", "selagan_raya", "Selagan Raya", "Lapangan & Sarana Pendidikan"),
        RpbEvacuationShelter(9, "Lapangan Teras Terunjam", "teras_terunjam", "Teras Terunjam", "Lapangan Terbuka Evakuasi"),
        RpbEvacuationShelter(10, "Lapangan Sepak Bola V Koto", "v_koto", "V Koto", "Lapangan Olahraga / Hulu DAS"),
        RpbEvacuationShelter(11, "SMPN 07 Mukomuko", "lubuk_pinang", "Lubuk Pinang", "Sarana Pendidikan / Wilayah Utara")
    )

    // Matriks Program & Nomenklatur Sub-Kegiatan Prioritas 2025–2029 (Tabel 5.1, Kepmendagri 900.1-861/2026)
    val prioritySubActivities: List<RpbProgramSubActivity> = listOf(
        RpbProgramSubActivity(
            nomenklaturCode = "1.05.03.2.04.0004",
            opdLead = "BPBD",
            sasaranNumber = 1,
            subActivityName = "Pengembangan Sistem Informasi Kebencanaan Daerah",
            targetAndBudgetHighlight = "Pagu indikatif Rp 500 Jt (2026) + Rp 100 Jt/thn (2027–2029)",
            sipagarSyncNote = "Mandat langsung aplikasi SIPAGAR KITA sebagai tulang punggung Sistem Informasi Kebencanaan BPBD."
        ),
        RpbProgramSubActivity(
            nomenklaturCode = "1.05.03.2.02.0026",
            opdLead = "BPBD",
            sasaranNumber = 1,
            subActivityName = "Pengendalian Operasi & Penyediaan Sarana Prasarana Kesiapsiagaan (Pusdalops-PB)",
            targetAndBudgetHighlight = "Capaian SPM 95,86% (2025) • Pemeliharaan Pusdalops 24/7",
            sipagarSyncNote = "Selaras Ruang Kerja Komando Pusdalops, Sirene Four-Eyes & Laporan Harian PDF/Excel."
        ),
        RpbProgramSubActivity(
            nomenklaturCode = "1.06.06.2.01.0007",
            opdLead = "BPBD",
            sasaranNumber = 1,
            subActivityName = "Penyediaan Tempat Penampungan Pengungsi Bencana",
            targetAndBudgetHighlight = "11 lokasi resmi tersebar di 10 kecamatan prioritas",
            sipagarSyncNote = "Tersinkron pada Peta & SOP Evakuasi serta Kompas Titik Kumpul SIPAGAR KITA."
        ),
        RpbProgramSubActivity(
            nomenklaturCode = "1.05.03.2.01.0008 / .0027",
            opdLead = "BPBD",
            sasaranNumber = 2,
            subActivityName = "Penyusunan KRB, RPB, Renkon (.0022), RPKB (.0023), Renops (.0007) & Rencana Aksi SPM (.0016)",
            targetAndBudgetHighlight = "KRB 2026–2030 & RPB 2025–2029 tuntas; pemutakhiran Renkon & Renops berkala",
            sipagarSyncNote = "Tersaji lengkap pada Tab Khusus KRB, RPB & RTRW sebagai referensi legal & lapangan."
        ),
        RpbProgramSubActivity(
            nomenklaturCode = "1.05.03.2.01.0007 / .0018",
            opdLead = "BPBD",
            sasaranNumber = 2,
            subActivityName = "KIE Rawan Bencana (1.000–2.500 org/thn), Gladi (300–700/thn) & TRC PB (.0021: 20→200 org)",
            targetAndBudgetHighlight = "Penguatan kapasitas warga, relawan & TRC di 15 kecamatan",
            sipagarSyncNote = "Didukung Mode Warga Tanpa Login, SOP Evakuasi 20-20-20 & Mode Sandbox Simulasi."
        ),
        RpbProgramSubActivity(
            nomenklaturCode = "1.05.03.2.03.0002 / .0003",
            opdLead = "BPBD",
            sasaranNumber = 2,
            subActivityName = "Respon Cepat Darurat (≤1×24 jam), SAR & Evakuasi (50→150 org/thn), Logistik (.0009) & SKPDB (.0012)",
            targetAndBudgetHighlight = "Menutup gap SPM Respons Cepat (36,14%) & SAR (25,00%)",
            sipagarSyncNote = "Dipercepat oleh Notifikasi Laporan Warga 24 Jam, GPS Presisi, Logistik FEFO & Call 117/112."
        ),
        RpbProgramSubActivity(
            nomenklaturCode = "1.05.03.2.04.0015",
            opdLead = "BPBD",
            sasaranNumber = 2,
            subActivityName = "Pengkajian Kebutuhan Pascabencana (JITUPASNA) & Penyusunan R3P",
            targetAndBudgetHighlight = "Pemulihan terukur berbasis kerusakan fisik, ekonomi & sosial",
            sipagarSyncNote = "Terintegrasi langsung dengan Modul Pasca Bencana (DALA) & rekapitulasi PDF/CSV."
        ),
        RpbProgramSubActivity(
            nomenklaturCode = "1.03.02.2.01.0090",
            opdLead = "DPUPR",
            sasaranNumber = 1,
            subActivityName = "Pembangunan Flood Forecasting & Warning System (FFWS), Polder/Kolam Retensi (.0082/.0097) & Drainase (.0012)",
            targetAndBudgetHighlight = "2 unit FFWS (@Rp 750 Jt), polder Lubuk Pinang/XIV Koto/Ipuh, drainase 1.000 m/thn",
            sipagarSyncNote = "Selaras peringatan dini curah hujan ekstrem, banjir DAS & pemantauan cuaca 15 kecamatan."
        ),
        RpbProgramSubActivity(
            nomenklaturCode = "1.03.10.2.01.0043 / .0059",
            opdLead = "DPUPR",
            sasaranNumber = 1,
            subActivityName = "Pembangunan & Pemeliharaan Jalan/Jembatan Evakuasi & Bendung Air Manjunto (.02.0054)",
            targetAndBudgetHighlight = "Jaringan jalan/jembatan evakuasi 681,37 Km & irigasi V Koto/Air Manjunto",
            sipagarSyncNote = "Rujukan rute akses TRC, distribusi logistik & jalur evakuasi warga."
        ),
        RpbProgramSubActivity(
            nomenklaturCode = "1.03.12.2.04.0012 / .0006",
            opdLead = "DPUPR",
            sasaranNumber = 3,
            subActivityName = "Penilaian Pelaksanaan RTR, Audit Tata Ruang, TURBINLAK (.0011) & Pengendalian KKPR",
            targetAndBudgetHighlight = "Penegakan Perda RTRW No. 12/2025 Pasal 92, 93 & Sanksi KKPR Pasal 95–108",
            sipagarSyncNote = "Terhubung pada Sub-Tab Perda RTRW & Sanksi KKPR dalam Tab KRB, RPB & RTRW."
        ),
        RpbProgramSubActivity(
            nomenklaturCode = "1.05.04.2.01.0008 / .0011",
            opdLead = "Disdamkarmat",
            sasaranNumber = 2,
            subActivityName = "Sistem Informasi Kebakaran Real-Time (SKIKP), RISPKP & Relawan Damkar SKKL (.04.0002)",
            targetAndBudgetHighlight = "Deteksi dini & respon cepat Karhutla pada 404.993,70 Ha rawan",
            sipagarSyncNote = "Sinergi penuh dengan Tab Monitoring Karhutla (Satelit NASA FIRMS & BMKG)."
        ),
        RpbProgramSubActivity(
            nomenklaturCode = "1.06.06.2.02.0001 / .0004",
            opdLead = "Dinsos",
            sasaranNumber = 2,
            subActivityName = "Kampung Siaga Bencana (5→11 KSB), Perlindungan Kelompok Rentan & Psikososial (.01.0005)",
            targetAndBudgetHighlight = "Fokus inklusi GEDSI: 1.460 warga miskin P3KE & 269 disabilitas",
            sipagarSyncNote = "Memperkuat pendataan korban rentan pada Laporan Warga & Posko Pengungsian."
        ),
        RpbProgramSubActivity(
            nomenklaturCode = "2.11.03.2.01.0009 / .0002",
            opdLead = "DLH",
            sasaranNumber = 3,
            subActivityName = "Pemantauan Kualitas Lingkungan, RTH (.04.0004), Mitigasi Perubahan Iklim & GRK (.0010)",
            targetAndBudgetHighlight = "Target IKLH 66,20 → 69,58 & penurunan GRK 28,10% → 41,29%",
            sipagarSyncNote = "Selaras Tab Kualitas Udara (AQI/PM2.5) & pemantauan kabut asap Karhutla."
        ),
        RpbProgramSubActivity(
            nomenklaturCode = "1.06.06.2.02.0008 / 2.15.02",
            opdLead = "Disdikbud & Dishub",
            sasaranNumber = 2,
            subActivityName = "Edukasi Anak Usia Sekolah (1.000→2.500/thn) & Rambu/Papan Informasi Bencana (20–25 unit/thn)",
            targetAndBudgetHighlight = "SPAB di sekolah rawan & penguatan rambu jalur evakuasi pesisir/hulu",
            sipagarSyncNote = "Melengkapi panduan SOP Evakuasi 20-20-20 & peta TES/TEA digital."
        ),
        RpbProgramSubActivity(
            nomenklaturCode = "3.27.03.2.01.0014 / .05.0002",
            opdLead = "Distan & DKP",
            sasaranNumber = 3,
            subActivityName = "Perlindungan LP2B/KP2B (.0015), Pengendalian Dampak Perubahan Iklim (DPI) & Zoonosis (.0004)",
            targetAndBudgetHighlight = "Menjaga sektor pertanian (42,44% PDRB) dari kekeringan & banjir",
            sipagarSyncNote = "Selaras Tab Prakiraan Cuaca, Cuaca Historis & Peringatan Dini Agroklimat."
        )
    )

    // Kerangka Pemantauan & Evaluasi RPBD (Bab VI, Tabel 6.1)
    val monevPhases: List<RpbMonevPhase> = listOf(
        RpbMonevPhase(
            phaseName = "Evaluasi Tahunan",
            frequency = "Setiap Akhir Tahun (2025–2029)",
            actors = "BPBD, Bapperida, & seluruh OPD pelaksana sub-kegiatan RPB",
            outputDeliverable = "Laporan capaian tahunan indikator SPM, realisasi sub-kegiatan & bahan penyusunan RKPD tahun berikutnya."
        ),
        RpbMonevPhase(
            phaseName = "Evaluasi Tengah Periode",
            frequency = "Pertengahan Periode (2027)",
            actors = "BPBD, Bapperida, Inspektorat, DPRD & FPRB multipihak",
            outputDeliverable = "Penilaian tren IRB, IKD, IKLH & penyesuaian strategi bila terjadi perubahan risiko atau regulasi."
        ),
        RpbMonevPhase(
            phaseName = "Evaluasi Akhir Periode",
            frequency = "Akhir Periode (2029)",
            actors = "Pemkab Mukomuko, BNPB, Pemprov Bengkulu & pemangku kepentingan Hexahelix",
            outputDeliverable = "Laporan dampak menyeluruh RPB 2025–2029 sebagai dasar penyusunan RPB periode berikutnya selaras RIPB 2020–2044."
        )
    )

    const val INSTITUTIONAL_NOTE =
        "Tata kelola multipihak Hexahelix (Pemerintah, Akademisi, Dunia Usaha, Masyarakat, Media & Komunitas Relawan) dengan prinsip inklusif GEDSI (Gender, Disabilitas & Inklusi Sosial). Prioritas kelembagaan mendesak: pembentukan Forum PRB (FPRB) Kabupaten Mukomuko & penyesuaian SOTK BPBD sesuai Permendagri No. 18 Tahun 2025."

    fun getSheltersForDistrict(district: District): List<RpbEvacuationShelter> {
        return officialEvacuationShelters.filter {
            it.districtId.equals(district.id, ignoreCase = true) ||
                it.districtName.equals(district.name, ignoreCase = true)
        }
    }
}
