package com.example.model

import androidx.annotation.Keep

/**
 * Dataset Resmi Dokumen Kajian Risiko Bencana (KRB) Kabupaten Mukomuko Tahun 2026-2030
 * Disusun oleh: BPBD Kabupaten Mukomuko & PS2DL Universitas Gadjah Mada (Januari 2026)
 * Penanggung Jawab: Ruri Irwandi, ST, MT (Kepala Pelaksana BPBD Kabupaten Mukomuko)
 */
@Keep
data class KrbHazardSummary(
    val number: Int,
    val hazardName: String,
    val hazardAreaHa: Double,
    val hazardClass: String, // "Tinggi", "Sedang", "Rendah"
    val exposedPopulation: Int,
    val totalLossMillionRp: Double, // dalam Juta Rupiah
    val envDamageHa: Double,
    val vulnerabilityClass: String, // "Tinggi", "Sedang", "Rendah"
    val capacityIndex: Double,
    val capacityClass: String, // "Tinggi", "Sedang", "Rendah"
    val riskClass: String, // "Tinggi", "Sedang", "Rendah"
    val historicalEvents2014To2024: Int,
    val historicalTrend: String, // "Meningkat", "Tetap", "Menurun"
    val priorityLevel: Int, // 1 = Prioritas 1, 2 = Prioritas 2, 3 = Prioritas 3
    val affectedDistrictsCount: Int,
    val keyHighlight: String,
    val structuralMitigation: String,
    val nonStructuralMitigation: String
)

@Keep
data class KrbIkdPriorityItem(
    val focusNumber: Int,
    val focusTitle: String,
    val score: Double,
    val categoryLabel: String,
    val strategicNote: String
)

@Keep
data class KrbDistrictHazardBadge(
    val hazardName: String,
    val riskLevel: String, // "Tinggi", "Sedang", "Rendah"
    val detailMetric: String
)

@Keep
data class KrbDistrictRiskProfile(
    val districtId: String,
    val districtName: String,
    val capitalName: String,
    val villagesCount: Int,
    val wardsCount: Int, // Kelurahan
    val areaHa: Double,
    val exposedPopulation: Int,
    val vulnerableAgePopulation: Int,
    val poorPopulationP3KE: Int,
    val disabilityPopulation: Int,
    val healthFacilitiesCount: Int,
    val ikdTransformed: Double, // IKD_T = 0.242
    val ikmScore: Double, // Indeks Kesiapsiagaan Masyarakat
    val multiHazardCapacityIndex: Double,
    val multiHazardCapacityClass: String, // "Rendah", "Sedang"
    val physicalLossMillionRp: Double,
    val economicLossMillionRp: Double,
    val totalLossMillionRp: Double,
    val envDamageHa: Double,
    val riskLowAreaHa: Double,
    val riskMediumAreaHa: Double,
    val riskHighAreaHa: Double,
    val multiHazardRiskClass: String, // "Tinggi"
    val isCoastalTsunamiZone: Boolean,
    val isFlashFloodZone: Boolean,
    val primaryHazardBadges: List<KrbDistrictHazardBadge>,
    val recommendationSummary: String
)

object MukomukoKrb2026Catalog {
    const val DOCUMENT_TITLE = "Dokumen Kajian Risiko Bencana (KRB) Kabupaten Mukomuko Tahun 2026–2030"
    const val DOCUMENT_PUBLISHER = "BPBD Kabupaten Mukomuko & PS2DL Universitas Gadjah Mada (Januari 2026)"
    const val DOCUMENT_RESPONSIBLE = "Ruri Irwandi, ST, MT (Kepala Pelaksana BPBD Kabupaten Mukomuko)"
    const val TOTAL_DISTRICTS = 15
    const val TOTAL_VILLAGES_AND_WARDS = 151 // 148 Desa + 3 Kelurahan
    const val TOTAL_AREA_HA = 414_320.37
    const val TOTAL_POPULATION = 205_602
    const val TOTAL_VULNERABLE_AGE = 34_034
    const val TOTAL_POOR_P3KE = 1_460
    const val TOTAL_DISABILITY = 296
    const val IRBI_2025_SCORE = 164.32
    const val IKD_2025_SCORE = 0.29
    const val IKD_TRANSFORMED_SCORE = 0.242
    const val IKM_COUNTY_AVERAGE = 0.348
    const val MULTI_HAZARD_CAPACITY_INDEX = 0.295
    const val MULTI_HAZARD_PHYSICAL_LOSS_MILLION_RP = 5_905_350.81
    const val MULTI_HAZARD_ECONOMIC_LOSS_MILLION_RP = 834_437.50
    const val MULTI_HAZARD_TOTAL_LOSS_MILLION_RP = 6_739_788.30 // Rp 6,739 Triliun
    const val MULTI_HAZARD_ENV_DAMAGE_HA = 115_231.14

    /**
     * Tren Nilai Indeks Risiko Bencana Indonesia (IRBI) Kabupaten Mukomuko (Gambar 1.1 KRB 2026-2030)
     */
    val irbiHistoricalTrend: List<Pair<String, Double>> = listOf(
        "2015" to 191.20,
        "2016" to 173.12,
        "2017–2020" to 166.02,
        "2021–2022" to 172.29,
        "2023–2025" to 164.32
    )

    /**
     * 7 Fokus Prioritas Indeks Ketahanan Daerah (IKD) Tahun 2025 (Tabel 3.102 KRB 2026-2030)
     */
    val ikdPriorities: List<KrbIkdPriorityItem> = listOf(
        KrbIkdPriorityItem(
            focusNumber = 1,
            focusTitle = "Perkuatan Kebijakan dan Kelembagaan",
            score = 0.42,
            categoryLabel = "Sedang",
            strategicNote = "Penyusunan RPB 2026–2030, Perda PB, dan penguatan kelembagaan Forum PRB Daerah."
        ),
        KrbIkdPriorityItem(
            focusNumber = 2,
            focusTitle = "Pengkajian Risiko dan Perencanaan Terpadu",
            score = 0.30,
            categoryLabel = "Rendah",
            strategicNote = "Integrasi Dokumen KRB 2026–2030 ke dalam RTRW, RPJMD, dan Renstra Perangkat Daerah."
        ),
        KrbIkdPriorityItem(
            focusNumber = 3,
            focusTitle = "Pengembangan Sistem Informasi, Diklat dan Logistik",
            score = 0.46,
            categoryLabel = "Tertinggi • Didukung SIPAGAR KITA",
            strategicNote = "Fokus IKD tertinggi Kabupaten Mukomuko — diperkuat melalui Pusdalops PB & aplikasi SIPAGAR KITA."
        ),
        KrbIkdPriorityItem(
            focusNumber = 4,
            focusTitle = "Penanganan Tematik Kawasan Rawan Bencana",
            score = 0.20,
            categoryLabel = "Terendah • Prioritas Intervensi",
            strategicNote = "Percepatan pembentukan Desa Tangguh Bencana (Destana), Satuan Pendidikan Aman Bencana (SPAB), dan RSAB."
        ),
        KrbIkdPriorityItem(
            focusNumber = 5,
            focusTitle = "Peningkatan Efektivitas Pencegahan dan Mitigasi Bencana",
            score = 0.22,
            categoryLabel = "Rendah",
            strategicNote = "Pengendalian tata ruang sempadan pantai/sungai, rehabilitasi DAS, dan pengawasan lahan gambut."
        ),
        KrbIkdPriorityItem(
            focusNumber = 6,
            focusTitle = "Perkuatan Kesiapsiagaan dan Penanganan Darurat Bencana",
            score = 0.29,
            categoryLabel = "Rendah",
            strategicNote = "Penyusunan Rencana Kontinjensi (Renkon), sistem peringatan dini (EWS) multirisiko, dan jalur/TES tsunami."
        ),
        KrbIkdPriorityItem(
            focusNumber = 7,
            focusTitle = "Pengembangan Sistem Pemulihan Bencana",
            score = 0.30,
            categoryLabel = "Rendah",
            strategicNote = "Standarisasi Jitupasna/DaLA untuk rehabilitasi dan rekonstruksi pascabencana prinsip Build Back Better."
        )
    )

    /**
     * Rekapitulasi 12 Jenis Ancaman Bencana & Matriks Prioritas Penanganan KRB 2026-2030
     * (Tabel 3.97–3.105 & Tabel 3.112–3.113)
     */
    val twelveHazardsMatrix: List<KrbHazardSummary> = listOf(
        KrbHazardSummary(
            number = 1,
            hazardName = "Cuaca Ekstrem",
            hazardAreaHa = 179_536.76,
            hazardClass = "Tinggi",
            exposedPopulation = 199_429,
            totalLossMillionRp = 6_629_612.14,
            envDamageHa = 0.0,
            vulnerabilityClass = "Sedang",
            capacityIndex = 0.281,
            capacityClass = "Rendah",
            riskClass = "Tinggi",
            historicalEvents2014To2024 = 12,
            historicalTrend = "Meningkat",
            priorityLevel = 1,
            affectedDistrictsCount = 15,
            keyHighlight = "Potensi kerugian tertinggi Rp 6,629 Triliun & 199.429 jiwa terpapar di 15 kecamatan (AUC model = 0,813).",
            structuralMitigation = "Penguatan struktur atap bangunan publik, peremajaan pohon pelindung jalan lintas barat, dan penataan jaringan drainase.",
            nonStructuralMitigation = "Pemantauan nowcasting cuaca ekstrem via SIPAGAR KITA, pemangkasan pohon berkala, dan sosialisasi kesiapsiagaan angin kencang."
        ),
        KrbHazardSummary(
            number = 2,
            hazardName = "Kekeringan",
            hazardAreaHa = 414_320.37,
            hazardClass = "Sedang",
            exposedPopulation = 205_602,
            totalLossMillionRp = 594_963.73,
            envDamageHa = 73_891.08,
            vulnerabilityClass = "Tinggi",
            capacityIndex = 0.291,
            capacityClass = "Rendah",
            riskClass = "Tinggi",
            historicalEvents2014To2024 = 1,
            historicalTrend = "Meningkat",
            priorityLevel = 1,
            affectedDistrictsCount = 15,
            keyHighlight = "Berdampak pada seluruh 205.602 jiwa dan 73.891,08 Ha lahan pertanian/perkebunan saat fase El Nino.",
            structuralMitigation = "Optimalisasi jaringan irigasi Bendung Air Manjunto, pembangunan embung desa, dan sumur bor cadangan air bersih.",
            nonStructuralMitigation = "Kalender tanam adaptif iklim, manajemen distribusi air bersih tanggap darurat, dan konservasi daerah tangkapan air."
        ),
        KrbHazardSummary(
            number = 3,
            hazardName = "Banjir Bandang",
            hazardAreaHa = 6_582.78,
            hazardClass = "Tinggi",
            exposedPopulation = 91,
            totalLossMillionRp = 7_156.71,
            envDamageHa = 3_709.71,
            vulnerabilityClass = "Sedang",
            capacityIndex = 0.260,
            capacityClass = "Rendah",
            riskClass = "Tinggi",
            historicalEvents2014To2024 = 0,
            historicalTrend = "Tetap",
            priorityLevel = 1,
            affectedDistrictsCount = 7,
            keyHighlight = "Mengancam 7 kecamatan koridor sungai berhulu di Bukit Barisan (Malin Deman, Pondok Suguh, Selagan Raya, V Koto, Sungai Rumbai, Teramang Jaya, Penarik).",
            structuralMitigation = "Pembangunan sabo dam/check dam penahan sedimen hulu sungai dan perkuatan tebing sungai di 7 kecamatan rawan.",
            nonStructuralMitigation = "Restorasi tutupan hutan hulu WS Teramang-Muar, larangan hunian di sempadan sungai aktif, dan EWS hulu-hilir."
        ),
        KrbHazardSummary(
            number = 4,
            hazardName = "Kebakaran Hutan dan Lahan (Karhutla)",
            hazardAreaHa = 404_993.70,
            hazardClass = "Tinggi",
            exposedPopulation = 0,
            totalLossMillionRp = 834_437.50,
            envDamageHa = 10_413.09,
            vulnerabilityClass = "Tinggi",
            capacityIndex = 0.292,
            capacityClass = "Rendah",
            riskClass = "Tinggi",
            historicalEvents2014To2024 = 20,
            historicalTrend = "Tetap",
            priorityLevel = 1,
            affectedDistrictsCount = 15,
            keyHighlight = "Akurasi model tertinggi (AUC = 0,932); rawan di lahan gambut XIV Koto (Rawa Mulya), Penarik, Malin Deman, dan Pondok Suguh.",
            structuralMitigation = "Sekat kanal (canal blocking) dan sumur bor pembasahan lahan gambut di Kec. XIV Koto, Penarik, dan Teramang Jaya.",
            nonStructuralMitigation = "Deteksi dini hotspot VIIRS/FIRMS via SIPAGAR KITA, patroli terpadu Masyarakat Peduli Api (MPA), dan penegakan Perda larangan bakar lahan."
        ),
        KrbHazardSummary(
            number = 5,
            hazardName = "Tsunami",
            hazardAreaHa = 17_492.58,
            hazardClass = "Tinggi",
            exposedPopulation = 31_122,
            totalLossMillionRp = 2_273_832.42,
            envDamageHa = 155.12,
            vulnerabilityClass = "Sedang",
            capacityIndex = 0.375,
            capacityClass = "Sedang",
            riskClass = "Tinggi",
            historicalEvents2014To2024 = 0,
            historicalTrend = "Tetap",
            priorityLevel = 1,
            affectedDistrictsCount = 8,
            keyHighlight = "Skenario Megathrust Mentawai-Pagai/Enggano Mw 8,5–9,0 mengancam 8 kecamatan pesisir (31.122 jiwa; kerugian Rp 2,273 Triliun).",
            structuralMitigation = "Pembangunan Tempat Evakuasi Sementara (TES/Shelter Vertikal), pengerasan jalan evakuasi tsunami, dan sabuk hijau pantai (mangrove/cemara laut).",
            nonStructuralMitigation = "Integrasi InaTEWS BMKG di SIPAGAR KITA, pemasangan rambu jalur evakuasi di 8 kecamatan pesisir, dan drill evakuasi mandiri 20-20-20."
        ),
        KrbHazardSummary(
            number = 6,
            hazardName = "Banjir",
            hazardAreaHa = 63_820.62,
            hazardClass = "Tinggi",
            exposedPopulation = 63_030,
            totalLossMillionRp = 631_094.55,
            envDamageHa = 987.03,
            vulnerabilityClass = "Tinggi",
            capacityIndex = 0.316,
            capacityClass = "Rendah",
            riskClass = "Sedang",
            historicalEvents2014To2024 = 49,
            historicalTrend = "Meningkat",
            priorityLevel = 1,
            affectedDistrictsCount = 15,
            keyHighlight = "Bencana hidrometeorologi paling sering terjadi (49 kejadian 2014–2024), memapar 63.030 jiwa terutama di Kota Mukomuko, Teramang Jaya, dan Lubuk Pinang.",
            structuralMitigation = "Normalisasi sungai, pembangunan tanggul penahan banjir, kolam retensi, dan perbaikan drainase kawasan permukiman padat.",
            nonStructuralMitigation = "Pemantauan tinggi muka air & curah hujan ekstrem, penataan ruang dataran banjir, serta pembentukan posko siaga banjir desa."
        ),
        KrbHazardSummary(
            number = 7,
            hazardName = "Gelombang Ekstrem dan Abrasi (GEA)",
            hazardAreaHa = 2_892.69,
            hazardClass = "Tinggi",
            exposedPopulation = 3_972,
            totalLossMillionRp = 131_691.60,
            envDamageHa = 872.46,
            vulnerabilityClass = "Sedang",
            capacityIndex = 0.364,
            capacityClass = "Sedang",
            riskClass = "Sedang",
            historicalEvents2014To2024 = 3,
            historicalTrend = "Meningkat",
            priorityLevel = 1,
            affectedDistrictsCount = 7,
            keyHighlight = "Tinggi gelombang signifikan mencapai 2,7 meter mengancam jalan lintas barat & permukiman di 7 kecamatan pesisir.",
            structuralMitigation = "Pembangunan seawall, revetment, groin/pemecah gelombang di Pantai Abrasi Kota Mukomuko, Ipuh, Teramang Jaya, dan Air Dikit.",
            nonStructuralMitigation = "Peringatan dini gelombang tinggi maritim BMKG untuk nelayan, zonasi sempadan pantai minimal 100 meter, dan rehabilitasi vegetasi pantai."
        ),
        KrbHazardSummary(
            number = 8,
            hazardName = "Tanah Longsor",
            hazardAreaHa = 211_034.88,
            hazardClass = "Tinggi",
            exposedPopulation = 4_634,
            totalLossMillionRp = 236_940.13,
            envDamageHa = 113_517.27,
            vulnerabilityClass = "Sedang",
            capacityIndex = 0.265,
            capacityClass = "Rendah",
            riskClass = "Sedang",
            historicalEvents2014To2024 = 17,
            historicalTrend = "Meningkat",
            priorityLevel = 1,
            affectedDistrictsCount = 13,
            keyHighlight = "Luas bahaya 211.034,88 Ha di 13 kecamatan perbukitan timur (Malin Deman, Pondok Suguh, Selagan Raya, Sungai Rumbai, V Koto).",
            structuralMitigation = "Dinding penahan tanah (retaining wall), bronjong, drainase lereng, dan bioengineering vegetasi akar dalam pada lereng curam.",
            nonStructuralMitigation = "Rambu rawan longsor pada ruas jalan perbukitan, pemantauan hujan pemicu longsor, dan pembatasan pembukaan lahan miring > 40%."
        ),
        KrbHazardSummary(
            number = 9,
            hazardName = "Likuefaksi",
            hazardAreaHa = 11_962.71,
            hazardClass = "Sedang",
            exposedPopulation = 24_142,
            totalLossMillionRp = 550_553.94,
            envDamageHa = 707.49,
            vulnerabilityClass = "Sedang",
            capacityIndex = 0.380,
            capacityClass = "Sedang",
            riskClass = "Sedang",
            historicalEvents2014To2024 = 0,
            historicalTrend = "Tetap",
            priorityLevel = 2,
            affectedDistrictsCount = 7,
            keyHighlight = "Berpotensi terjadi pada endapan aluvial pasir jenuh air di 7 kecamatan pesisir/muara (Kota Mukomuko 12.975 jiwa & Ipuh 8.330 jiwa).",
            structuralMitigation = "Perbaikan daya dukung tanah (soil compaction/deep foundation) dan drainase bawah permukaan untuk infrastruktur vital pesisir.",
            nonStructuralMitigation = "Persyaratan uji geoteknik/mikorzonasi likuefaksi dalam perizinan PBG bangunan bertingkat dan fasilitas kritis."
        ),
        KrbHazardSummary(
            number = 10,
            hazardName = "Penyakit Berpotensi KLB / Wabah (DBD)",
            hazardAreaHa = 4_652.37,
            hazardClass = "Rendah",
            exposedPopulation = 205_602,
            totalLossMillionRp = 0.0,
            envDamageHa = 0.0,
            vulnerabilityClass = "Sedang",
            capacityIndex = 0.392,
            capacityClass = "Sedang",
            riskClass = "Rendah",
            historicalEvents2014To2024 = 55,
            historicalTrend = "Meningkat",
            priorityLevel = 2,
            affectedDistrictsCount = 15,
            keyHighlight = "Tercatat 55 kasus DBD (2020–2024) dengan lonjakan 36 kasus pada tahun 2024; didukung 71 fasilitas kesehatan.",
            structuralMitigation = "Sanitasi lingkungan permukiman, pengelolaan drainase bebas genangan air, dan penguatan kapasitas 71 faskes.",
            nonStructuralMitigation = "Gerakan PSN 3M Plus, surveilans epidemiologi terpadu Dinkes-BPBD, dan pengendalian vektor pascabanjir."
        ),
        KrbHazardSummary(
            number = 11,
            hazardName = "Gempabumi",
            hazardAreaHa = 414_320.37,
            hazardClass = "Sedang",
            exposedPopulation = 205_602,
            totalLossMillionRp = 3_182_251.77,
            envDamageHa = 0.0,
            vulnerabilityClass = "Sedang",
            capacityIndex = 0.366,
            capacityClass = "Sedang",
            riskClass = "Sedang",
            historicalEvents2014To2024 = 82,
            historicalTrend = "Menurun",
            priorityLevel = 3,
            affectedDistrictsCount = 15,
            keyHighlight = "Dipengaruhi Zona Subduksi Megathrust Sumatera & Sesar Sumatera (Segmen Seblat/Muko-Muko); potensi kerugian fisik Rp 3,182 Triliun.",
            structuralMitigation = "Penerapan standar bangunan tahan gempa (SNI 1726) dan retrofitting fasilitas publik, sekolah (SPAB), serta rumah sakit (RSAB).",
            nonStructuralMitigation = "Notifikasi gempa real-time BMKG di SIPAGAR KITA, audit kerentanan bangunan publik, dan simulasi Drop-Cover-Hold On."
        ),
        KrbHazardSummary(
            number = 12,
            hazardName = "Kegagalan Teknologi",
            hazardAreaHa = 722.07,
            hazardClass = "Sedang",
            exposedPopulation = 1_119,
            totalLossMillionRp = 0.0,
            envDamageHa = 0.0,
            vulnerabilityClass = "Rendah",
            capacityIndex = 0.275,
            capacityClass = "Rendah",
            riskClass = "Rendah",
            historicalEvents2014To2024 = 0,
            historicalTrend = "Tetap",
            priorityLevel = 3,
            affectedDistrictsCount = 10,
            keyHighlight = "Radius bahaya di sekitar pabrik pengolahan kelapa sawit (PKS), SPBU, dan instalasi energi di 10 kecamatan.",
            structuralMitigation = "Sistem pengaman instalasi industri, kolam IPAL standar, bundwall tangki bahan bakar, dan sistem pemadam otomatis.",
            nonStructuralMitigation = "Audit K3 berkala, SOP tanggap darurat bahan berbahaya (B3), dan sosialisasi zona aman masyarakat sekitar kawasan industri."
        )
    )

    /**
     * Profil Resmi KRB 2026-2030 untuk Seluruh 15 Kecamatan di Kabupaten Mukomuko
     * (Tabel 2.1, 3.32, 3.85–3.96, 3.106–3.111)
     */
    val districtProfiles: List<KrbDistrictRiskProfile> = listOf(
        KrbDistrictRiskProfile(
            districtId = "kota_mukomuko",
            districtName = "Kota Mukomuko",
            capitalName = "Pasar Mukomuko",
            villagesCount = 6,
            wardsCount = 3, // 6 Desa + 3 Kelurahan = 9
            areaHa = 14_511.17,
            exposedPopulation = 19_732,
            vulnerableAgePopulation = 3_117,
            poorPopulationP3KE = 146,
            disabilityPopulation = 29,
            healthFacilitiesCount = 7,
            ikdTransformed = 0.242,
            ikmScore = 0.492,
            multiHazardCapacityIndex = 0.382,
            multiHazardCapacityClass = "Sedang",
            physicalLossMillionRp = 1_167_898.93,
            economicLossMillionRp = 55_197.40,
            totalLossMillionRp = 1_223_096.33,
            envDamageHa = 552.42,
            riskLowAreaHa = 804.60,
            riskMediumAreaHa = 9_484.90,
            riskHighAreaHa = 4_221.67,
            multiHazardRiskClass = "Tinggi",
            isCoastalTsunamiZone = true,
            isFlashFloodZone = false,
            primaryHazardBadges = listOf(
                KrbDistrictHazardBadge("Tsunami Pesisir", "Tinggi", "6.628,05 Ha • 15.898 Jiwa • Rp 1,484 T"),
                KrbDistrictHazardBadge("Banjir", "Tinggi", "11.203,65 Ha (Terluas) • 16.768 Jiwa"),
                KrbDistrictHazardBadge("Cuaca Ekstrem", "Tinggi", "18.894 Jiwa • Rp 1,158 T"),
                KrbDistrictHazardBadge("Gelombang Ekstrem & Abrasi", "Tinggi", "735,12 Ha (Terluas) • 1.320 Jiwa"),
                KrbDistrictHazardBadge("Likuefaksi", "Sedang", "6.272,01 Ha • 12.975 Jiwa"),
                KrbDistrictHazardBadge("Karhutla", "Tinggi", "13.095,69 Ha")
            ),
            recommendationSummary = "Prioritas utama perlindungan aset ibu kota kabupaten (kerugian tertinggi Rp 1,223 Triliun): pembangunan shelter vertikal tsunami (TES), pemecah gelombang Pantai Abrasi, dan normalisasi drainase perkotaan."
        ),
        KrbDistrictRiskProfile(
            districtId = "ipuh",
            districtName = "Ipuh",
            capitalName = "Medan Jaya",
            villagesCount = 16,
            wardsCount = 0,
            areaHa = 16_262.81,
            exposedPopulation = 19_810,
            vulnerableAgePopulation = 3_071,
            poorPopulationP3KE = 152,
            disabilityPopulation = 34,
            healthFacilitiesCount = 7,
            ikdTransformed = 0.242,
            ikmScore = 0.451,
            multiHazardCapacityIndex = 0.357,
            multiHazardCapacityClass = "Sedang",
            physicalLossMillionRp = 518_786.00,
            economicLossMillionRp = 48_587.55,
            totalLossMillionRp = 567_373.55,
            envDamageHa = 158.58,
            riskLowAreaHa = 696.60,
            riskMediumAreaHa = 2_386.25,
            riskHighAreaHa = 13_179.96,
            multiHazardRiskClass = "Tinggi",
            isCoastalTsunamiZone = true,
            isFlashFloodZone = false,
            primaryHazardBadges = listOf(
                KrbDistrictHazardBadge("Tsunami Pesisir", "Tinggi", "2.538,90 Ha • 4.434 Jiwa • Rp 257,6 M"),
                KrbDistrictHazardBadge("Cuaca Ekstrem", "Tinggi", "19.555 Jiwa • Rp 509,7 M"),
                KrbDistrictHazardBadge("Gelombang Ekstrem & Abrasi", "Tinggi", "697,77 Ha • 811 Jiwa"),
                KrbDistrictHazardBadge("Likuefaksi", "Sedang", "2.122,02 Ha • 8.330 Jiwa"),
                KrbDistrictHazardBadge("Karhutla", "Tinggi", "15.729,87 Ha"),
                KrbDistrictHazardBadge("Kekeringan", "Tinggi", "19.810 Jiwa")
            ),
            recommendationSummary = "Prioritas kesiapsiagaan tsunami & evakuasi inklusif (jumlah penyandang disabilitas tertinggi: 34 jiwa), perlindungan pantai Muara Sebakul dari abrasi, dan penguatan konstruksi tahan likuefaksi."
        ),
        KrbDistrictRiskProfile(
            districtId = "penarik",
            districtName = "Penarik",
            capitalName = "Lubuk Mukti",
            villagesCount = 14,
            wardsCount = 0,
            areaHa = 35_176.23,
            exposedPopulation = 26_303,
            vulnerableAgePopulation = 4_604,
            poorPopulationP3KE = 232,
            disabilityPopulation = 10,
            healthFacilitiesCount = 9,
            ikdTransformed = 0.242,
            ikmScore = 0.251,
            multiHazardCapacityIndex = 0.237,
            multiHazardCapacityClass = "Rendah",
            physicalLossMillionRp = 652_748.50,
            economicLossMillionRp = 70_923.84,
            totalLossMillionRp = 723_672.34,
            envDamageHa = 7_576.47,
            riskLowAreaHa = 205.47,
            riskMediumAreaHa = 14_300.40,
            riskHighAreaHa = 20_670.36,
            multiHazardRiskClass = "Tinggi",
            isCoastalTsunamiZone = false,
            isFlashFloodZone = true,
            primaryHazardBadges = listOf(
                KrbDistrictHazardBadge("Cuaca Ekstrem", "Tinggi", "25.550 Jiwa (Terbanyak) • Rp 710,3 M"),
                KrbDistrictHazardBadge("Karhutla", "Tinggi", "34.677,72 Ha • Kerusakan Lingkungan 2.103,5 Ha"),
                KrbDistrictHazardBadge("Banjir Bandang", "Tinggi", "397,89 Ha • Koridor Sungai"),
                KrbDistrictHazardBadge("Tanah Longsor", "Sedang", "17.421,03 Ha • 716 Jiwa"),
                KrbDistrictHazardBadge("Kekeringan", "Tinggi", "26.303 Jiwa • 5.839,5 Ha Lahan")
            ),
            recommendationSummary = "Kecamatan dengan penduduk terpapar tertinggi se-Kabupaten (26.303 jiwa) namun kapasitas masih Rendah (0,237): prioritas pembentukan Destana, patroli Karhutla, dan mitigasi angin kencang."
        ),
        KrbDistrictRiskProfile(
            districtId = "lubuk_pinang",
            districtName = "Lubuk Pinang",
            capitalName = "Lubuk Pinang",
            villagesCount = 7,
            wardsCount = 0,
            areaHa = 7_880.25,
            exposedPopulation = 16_024,
            vulnerableAgePopulation = 2_675,
            poorPopulationP3KE = 129,
            disabilityPopulation = 28,
            healthFacilitiesCount = 3,
            ikdTransformed = 0.242,
            ikmScore = 0.408,
            multiHazardCapacityIndex = 0.331,
            multiHazardCapacityClass = "Rendah",
            physicalLossMillionRp = 436_006.27,
            economicLossMillionRp = 24_348.08,
            totalLossMillionRp = 460_354.35,
            envDamageHa = 0.0,
            riskLowAreaHa = 213.75,
            riskMediumAreaHa = 1_077.46,
            riskHighAreaHa = 6_589.04,
            multiHazardRiskClass = "Tinggi",
            isCoastalTsunamiZone = false,
            isFlashFloodZone = false,
            primaryHazardBadges = listOf(
                KrbDistrictHazardBadge("Karhutla", "Tinggi", "7.381,98 Ha"),
                KrbDistrictHazardBadge("Cuaca Ekstrem", "Sedang", "15.869 Jiwa • Rp 454,5 M"),
                KrbDistrictHazardBadge("Banjir", "Sedang", "5.541,30 Ha • 9.188 Jiwa Terpapar"),
                KrbDistrictHazardBadge("Kekeringan", "Tinggi", "16.024 Jiwa • Sentra Irigasi Padi"),
                KrbDistrictHazardBadge("Likuefaksi", "Sedang", "248,67 Ha • 246 Jiwa")
            ),
            recommendationSummary = "Perlindungan lumbung padi irigasi dari risiko banjir luapan dan kekeringan El Nino, serta peningkatan kapasitas kesiapsiagaan masyarakat desa perbatasan Sumatera Barat."
        ),
        KrbDistrictRiskProfile(
            districtId = "pondok_suguh",
            districtName = "Pondok Suguh",
            capitalName = "Pondok Kandang",
            villagesCount = 11,
            wardsCount = 0,
            areaHa = 62_346.83,
            exposedPopulation = 14_967,
            vulnerableAgePopulation = 2_430,
            poorPopulationP3KE = 107,
            disabilityPopulation = 22,
            healthFacilitiesCount = 5,
            ikdTransformed = 0.242,
            ikmScore = 0.209,
            multiHazardCapacityIndex = 0.212,
            multiHazardCapacityClass = "Rendah",
            physicalLossMillionRp = 450_100.00,
            economicLossMillionRp = 98_383.70,
            totalLossMillionRp = 548_483.70,
            envDamageHa = 21_692.48,
            riskLowAreaHa = 374.04,
            riskMediumAreaHa = 33_209.06,
            riskHighAreaHa = 28_763.73,
            multiHazardRiskClass = "Tinggi",
            isCoastalTsunamiZone = true,
            isFlashFloodZone = true,
            primaryHazardBadges = listOf(
                KrbDistrictHazardBadge("Tsunami Pesisir", "Tinggi", "1.095,39 Ha • 83 Jiwa"),
                KrbDistrictHazardBadge("Banjir Bandang", "Tinggi", "1.327,41 Ha • 51 Jiwa (Tertinggi)"),
                KrbDistrictHazardBadge("Cuaca Ekstrem", "Tinggi", "14.872 Jiwa • Rp 546,2 M"),
                KrbDistrictHazardBadge("Karhutla", "Tinggi", "62.037,72 Ha • Rugi Rp 98,4 M"),
                KrbDistrictHazardBadge("Tanah Longsor", "Sedang", "41.256,81 Ha • 20.692 Ha Hutan"),
                KrbDistrictHazardBadge("Gelombang Ekstrem & Abrasi", "Sedang", "268,92 Ha")
            ),
            recommendationSummary = "Wilayah kompleks hulu-pesisir dengan kapasitas sangat rendah (0,212): prioritas EWS banjir bandang DAS Teramang, mitigasi longsor perbukitan, dan penguatan Destana pesisir."
        ),
        KrbDistrictRiskProfile(
            districtId = "teramang_jaya",
            districtName = "Teramang Jaya",
            capitalName = "Pasar Bantal",
            villagesCount = 13,
            wardsCount = 0,
            areaHa = 34_366.52,
            exposedPopulation = 13_500,
            vulnerableAgePopulation = 2_139,
            poorPopulationP3KE = 83,
            disabilityPopulation = 18,
            healthFacilitiesCount = 4,
            ikdTransformed = 0.242,
            ikmScore = 0.332,
            multiHazardCapacityIndex = 0.286,
            multiHazardCapacityClass = "Rendah",
            physicalLossMillionRp = 306_282.73,
            economicLossMillionRp = 68_075.90,
            totalLossMillionRp = 374_358.63,
            envDamageHa = 9_375.84,
            riskLowAreaHa = 594.90,
            riskMediumAreaHa = 16_193.22,
            riskHighAreaHa = 17_578.40,
            multiHazardRiskClass = "Tinggi",
            isCoastalTsunamiZone = true,
            isFlashFloodZone = true,
            primaryHazardBadges = listOf(
                KrbDistrictHazardBadge("Tsunami Pesisir", "Sedang", "2.513,16 Ha • 3.922 Jiwa • Rp 171,6 M"),
                KrbDistrictHazardBadge("Gelombang Ekstrem & Abrasi", "Sedang", "462,60 Ha • Rugi Rp 40,27 M (Tertinggi)"),
                KrbDistrictHazardBadge("Cuaca Ekstrem", "Tinggi", "12.905 Jiwa • Rp 342,3 M"),
                KrbDistrictHazardBadge("Banjir", "Sedang", "9.671,40 Ha • 12.888 Jiwa Terpapar"),
                KrbDistrictHazardBadge("Banjir Bandang", "Tinggi", "430,38 Ha"),
                KrbDistrictHazardBadge("Karhutla", "Tinggi", "33.309,54 Ha")
            ),
            recommendationSummary = "Memiliki potensi kerugian Gelombang Ekstrem & Abrasi tertinggi (Rp 40,27 Miliar) dan 12.888 jiwa terpapar banjir muara Sungai Teramang: prioritas perlindungan pantai & tanggul sungai."
        ),
        KrbDistrictRiskProfile(
            districtId = "air_dikit",
            districtName = "Air Dikit",
            capitalName = "Dusun Baru V Koto",
            villagesCount = 7,
            wardsCount = 0,
            areaHa = 8_085.07,
            exposedPopulation = 7_556,
            vulnerableAgePopulation = 1_137,
            poorPopulationP3KE = 68,
            disabilityPopulation = 10,
            healthFacilitiesCount = 4,
            ikdTransformed = 0.242,
            ikmScore = 0.550,
            multiHazardCapacityIndex = 0.416,
            multiHazardCapacityClass = "Sedang",
            physicalLossMillionRp = 169_830.88,
            economicLossMillionRp = 26_743.30,
            totalLossMillionRp = 196_574.18,
            envDamageHa = 79.61,
            riskLowAreaHa = 699.48,
            riskMediumAreaHa = 5_850.59,
            riskHighAreaHa = 1_535.00,
            multiHazardRiskClass = "Tinggi",
            isCoastalTsunamiZone = true,
            isFlashFloodZone = false,
            primaryHazardBadges = listOf(
                KrbDistrictHazardBadge("Tsunami Pesisir", "Sedang", "1.295,28 Ha • 1.450 Jiwa • Rp 92,0 M"),
                KrbDistrictHazardBadge("Karhutla", "Tinggi", "7.768,53 Ha"),
                KrbDistrictHazardBadge("Cuaca Ekstrem", "Sedang", "7.479 Jiwa • Rp 195,9 M"),
                KrbDistrictHazardBadge("Gelombang Ekstrem & Abrasi", "Sedang", "396,18 Ha • Koridor Jalinbar"),
                KrbDistrictHazardBadge("Likuefaksi", "Sedang", "979,20 Ha • 1.438 Jiwa")
            ),
            recommendationSummary = "Kecamatan dengan Indeks Kapasitas tertinggi se-Kabupaten (IKM = 0,550; Indeks = 0,416): fokus pada pengamanan Jalur Lintas Barat Sumatera dari abrasi pantai dan tsunami."
        ),
        KrbDistrictRiskProfile(
            districtId = "xiv_koto",
            districtName = "XIV Koto",
            capitalName = "Lubuk Sanai",
            villagesCount = 8,
            wardsCount = 0,
            areaHa = 7_485.30,
            exposedPopulation = 14_661,
            vulnerableAgePopulation = 2_667,
            poorPopulationP3KE = 34,
            disabilityPopulation = 31,
            healthFacilitiesCount = 1,
            ikdTransformed = 0.242,
            ikmScore = 0.530,
            multiHazardCapacityIndex = 0.405,
            multiHazardCapacityClass = "Sedang",
            physicalLossMillionRp = 337_509.88,
            economicLossMillionRp = 34_137.21,
            totalLossMillionRp = 371_647.09,
            envDamageHa = 52.11,
            riskLowAreaHa = 853.11,
            riskMediumAreaHa = 3_242.33,
            riskHighAreaHa = 3_389.86,
            multiHazardRiskClass = "Tinggi",
            isCoastalTsunamiZone = true,
            isFlashFloodZone = false,
            primaryHazardBadges = listOf(
                KrbDistrictHazardBadge("Karhutla Lahan Gambut", "Tinggi", "7.005,87 Ha • Rawan Gambut Rawa Mulya"),
                KrbDistrictHazardBadge("Tsunami Pesisir", "Sedang", "2.275,11 Ha • 4.053 Jiwa • Rp 218,3 M"),
                KrbDistrictHazardBadge("Cuaca Ekstrem", "Tinggi", "14.432 Jiwa • Rp 369,0 M"),
                KrbDistrictHazardBadge("Likuefaksi", "Sedang", "2.337,66 Ha • 1.151 Jiwa"),
                KrbDistrictHazardBadge("Gelombang Ekstrem & Abrasi", "Sedang", "135,09 Ha")
            ),
            recommendationSummary = "Fokus pengendalian kebakaran lahan gambut (kasus Desa Rawa Mulya) melalui sekat kanal & sumur bor gambut, serta jalur evakuasi tsunami bagi 4.053 warga pesisir."
        ),
        KrbDistrictRiskProfile(
            districtId = "v_koto",
            districtName = "V Koto",
            capitalName = "Lalang Luas",
            villagesCount = 10,
            wardsCount = 0,
            areaHa = 39_428.49,
            exposedPopulation = 8_200,
            vulnerableAgePopulation = 1_252,
            poorPopulationP3KE = 52,
            disabilityPopulation = 11,
            healthFacilitiesCount = 3,
            ikdTransformed = 0.242,
            ikmScore = 0.230,
            multiHazardCapacityIndex = 0.224,
            multiHazardCapacityClass = "Rendah",
            physicalLossMillionRp = 117_324.63,
            economicLossMillionRp = 81_599.13,
            totalLossMillionRp = 198_923.75,
            envDamageHa = 8_013.29,
            riskLowAreaHa = 259.65,
            riskMediumAreaHa = 20_992.85,
            riskHighAreaHa = 18_175.99,
            multiHazardRiskClass = "Tinggi",
            isCoastalTsunamiZone = false,
            isFlashFloodZone = true,
            primaryHazardBadges = listOf(
                KrbDistrictHazardBadge("Cuaca Ekstrem", "Tinggi", "18.286,83 Ha (Terluas) • 7.211 Jiwa"),
                KrbDistrictHazardBadge("Karhutla", "Tinggi", "37.651,86 Ha • Rugi Ekonomi Rp 81,6 M"),
                KrbDistrictHazardBadge("Banjir Bandang", "Tinggi", "917,01 Ha • Hulu Sungai Manjunto"),
                KrbDistrictHazardBadge("Tanah Longsor", "Sedang", "19.199,52 Ha • 7.775 Ha Hutan"),
                KrbDistrictHazardBadge("Kekeringan", "Tinggi", "8.200 Jiwa")
            ),
            recommendationSummary = "Wilayah hulu Sungai Manjunto dengan bahaya cuaca ekstrem terluas (18.286,83 Ha) dan kapasitas rendah (0,224): prioritas konservasi DAS hulu, sabo dam banjir bandang, dan Destana."
        ),
        KrbDistrictRiskProfile(
            districtId = "teras_terunjam",
            districtName = "Teras Terunjam",
            capitalName = "Teras Terunjam",
            villagesCount = 8,
            wardsCount = 0,
            areaHa = 8_592.74,
            exposedPopulation = 8_576,
            vulnerableAgePopulation = 1_409,
            poorPopulationP3KE = 61,
            disabilityPopulation = 4,
            healthFacilitiesCount = 2,
            ikdTransformed = 0.242,
            ikmScore = 0.413,
            multiHazardCapacityIndex = 0.334,
            multiHazardCapacityClass = "Sedang",
            physicalLossMillionRp = 297_889.75,
            economicLossMillionRp = 25_593.13,
            totalLossMillionRp = 323_482.88,
            envDamageHa = 89.91,
            riskLowAreaHa = 49.05,
            riskMediumAreaHa = 4_744.06,
            riskHighAreaHa = 3_799.63,
            multiHazardRiskClass = "Tinggi",
            isCoastalTsunamiZone = false,
            isFlashFloodZone = false,
            primaryHazardBadges = listOf(
                KrbDistrictHazardBadge("Cuaca Ekstrem", "Tinggi", "8.374 Jiwa • Rp 322,3 M"),
                KrbDistrictHazardBadge("Karhutla", "Tinggi", "8.526,96 Ha • Rugi Rp 25,6 M"),
                KrbDistrictHazardBadge("Banjir", "Sedang", "1.737,18 Ha • 1.296 Jiwa"),
                KrbDistrictHazardBadge("Tanah Longsor", "Sedang", "1.391,22 Ha • 181 Jiwa"),
                KrbDistrictHazardBadge("Kekeringan", "Sedang", "8.576 Jiwa")
            ),
            recommendationSummary = "Penguatan ketahanan kawasan perkebunan dan permukiman terhadap cuaca ekstrem, kebakaran lahan, serta penyediaan embung air baku."
        ),
        KrbDistrictRiskProfile(
            districtId = "selagan_raya",
            districtName = "Selagan Raya",
            capitalName = "Sungai Ipuh",
            villagesCount = 12,
            wardsCount = 0,
            areaHa = 39_482.65,
            exposedPopulation = 11_372,
            vulnerableAgePopulation = 1_864,
            poorPopulationP3KE = 90,
            disabilityPopulation = 27,
            healthFacilitiesCount = 4,
            ikdTransformed = 0.242,
            ikmScore = 0.131,
            multiHazardCapacityIndex = 0.165,
            multiHazardCapacityClass = "Rendah",
            physicalLossMillionRp = 217_184.00,
            economicLossMillionRp = 66_512.58,
            totalLossMillionRp = 283_696.58,
            envDamageHa = 15_033.02,
            riskLowAreaHa = 59.13,
            riskMediumAreaHa = 19_255.96,
            riskHighAreaHa = 20_167.56,
            multiHazardRiskClass = "Tinggi",
            isCoastalTsunamiZone = false,
            isFlashFloodZone = true,
            primaryHazardBadges = listOf(
                KrbDistrictHazardBadge("Kapasitas Terendah", "Tinggi", "IKM 0,131 • Indeks Kapasitas 0,165 (Prioritas #1)"),
                KrbDistrictHazardBadge("Cuaca Ekstrem", "Tinggi", "11.097 Jiwa • Rp 280,1 M"),
                KrbDistrictHazardBadge("Banjir Bandang", "Tinggi", "1.004,94 Ha • Lembah Sungai Selagan"),
                KrbDistrictHazardBadge("Tanah Longsor", "Sedang", "25.752,42 Ha • 14.940 Ha Hutan"),
                KrbDistrictHazardBadge("Karhutla", "Tinggi", "39.007,62 Ha")
            ),
            recommendationSummary = "Memiliki Indeks Kapasitas Masyarakat terendah se-Kabupaten Mukomuko (IKM = 0,131): prioritas intervensi pembentukan Destana, SPAB, serta EWS banjir bandang & tanah longsor."
        ),
        KrbDistrictRiskProfile(
            districtId = "malin_deman",
            districtName = "Malin Deman",
            capitalName = "Talang Arah",
            villagesCount = 7,
            wardsCount = 0,
            areaHa = 78_000.50,
            exposedPopulation = 7_457,
            vulnerableAgePopulation = 1_183,
            poorPopulationP3KE = 64,
            disabilityPopulation = 24,
            healthFacilitiesCount = 6,
            ikdTransformed = 0.242,
            ikmScore = 0.432,
            multiHazardCapacityIndex = 0.346,
            multiHazardCapacityClass = "Sedang",
            physicalLossMillionRp = 171_044.13,
            economicLossMillionRp = 102_707.68,
            totalLossMillionRp = 273_751.80,
            envDamageHa = 37_189.22,
            riskLowAreaHa = 0.0,
            riskMediumAreaHa = 60_513.59,
            riskHighAreaHa = 17_486.91,
            multiHazardRiskClass = "Tinggi",
            isCoastalTsunamiZone = false,
            isFlashFloodZone = true,
            primaryHazardBadges = listOf(
                KrbDistrictHazardBadge("Tanah Longsor", "Sedang", "58.482,36 Ha (Terluas) • 1.303 Jiwa"),
                KrbDistrictHazardBadge("Banjir Bandang", "Tinggi", "1.789,47 Ha (Terluas) • Hulu Sungai"),
                KrbDistrictHazardBadge("Karhutla", "Tinggi", "77.434,38 Ha • Rugi Ekonomi Rp 102,7 M (Tertinggi)"),
                KrbDistrictHazardBadge("Cuaca Ekstrem", "Tinggi", "6.271 Jiwa • Rp 231,4 M"),
                KrbDistrictHazardBadge("Kekeringan", "Tinggi", "32.164,17 Ha Kerusakan Lingkungan")
            ),
            recommendationSummary = "Kecamatan terluas (78.000,50 Ha) dengan potensi kerugian ekonomi (Rp 102,7 M) & kerusakan lingkungan tertinggi (37.189 Ha): fokus perlindungan hutan hulu, mitigasi longsor, dan banjir bandang."
        ),
        KrbDistrictRiskProfile(
            districtId = "sungai_rumbai",
            districtName = "Sungai Rumbai",
            capitalName = "Gajah Mati",
            villagesCount = 9,
            wardsCount = 0,
            areaHa = 35_699.61,
            exposedPopulation = 10_222,
            vulnerableAgePopulation = 1_700,
            poorPopulationP3KE = 69,
            disabilityPopulation = 27,
            healthFacilitiesCount = 4,
            ikdTransformed = 0.242,
            ikmScore = 0.351,
            multiHazardCapacityIndex = 0.297,
            multiHazardCapacityClass = "Rendah",
            physicalLossMillionRp = 323_967.00,
            economicLossMillionRp = 55_991.82,
            totalLossMillionRp = 379_958.82,
            envDamageHa = 14_547.15,
            riskLowAreaHa = 147.87,
            riskMediumAreaHa = 28_118.47,
            riskHighAreaHa = 7_433.27,
            multiHazardRiskClass = "Tinggi",
            isCoastalTsunamiZone = true,
            isFlashFloodZone = true,
            primaryHazardBadges = listOf(
                KrbDistrictHazardBadge("Cuaca Ekstrem", "Tinggi", "10.056 Jiwa • Rp 376,7 M"),
                KrbDistrictHazardBadge("Karhutla", "Tinggi", "35.514,09 Ha • Rugi Rp 56,0 M"),
                KrbDistrictHazardBadge("Tanah Longsor", "Sedang", "21.496,14 Ha • 13.702 Ha Lingkungan"),
                KrbDistrictHazardBadge("Banjir Bandang", "Tinggi", "715,68 Ha • 16 Jiwa"),
                KrbDistrictHazardBadge("Tsunami Pesisir", "Sedang", "608,94 Ha • 37 Jiwa")
            ),
            recommendationSummary = "Mitigasi terpadu koridor selatan (perbatasan Bengkulu Utara): penanganan lereng rawan longsor, pengendalian banjir bandang, dan peningkatan kapasitas desa."
        ),
        KrbDistrictRiskProfile(
            districtId = "air_rami",
            districtName = "Air Rami",
            capitalName = "Arga Jaya",
            villagesCount = 12,
            wardsCount = 0,
            areaHa = 21_698.43,
            exposedPopulation = 14_402,
            vulnerableAgePopulation = 2_379,
            poorPopulationP3KE = 64,
            disabilityPopulation = 2,
            healthFacilitiesCount = 5,
            ikdTransformed = 0.242,
            ikmScore = 0.342,
            multiHazardCapacityIndex = 0.292,
            multiHazardCapacityClass = "Rendah",
            physicalLossMillionRp = 420_977.75,
            economicLossMillionRp = 58_801.59,
            totalLossMillionRp = 479_779.34,
            envDamageHa = 871.06,
            riskLowAreaHa = 179.91,
            riskMediumAreaHa = 5_942.16,
            riskHighAreaHa = 15_576.36,
            multiHazardRiskClass = "Tinggi",
            isCoastalTsunamiZone = true,
            isFlashFloodZone = false,
            primaryHazardBadges = listOf(
                KrbDistrictHazardBadge("Cuaca Ekstrem", "Tinggi", "14.084 Jiwa • Rp 472,4 M"),
                KrbDistrictHazardBadge("Karhutla", "Tinggi", "21.471,57 Ha • Rugi Rp 58,8 M"),
                KrbDistrictHazardBadge("Tsunami Pesisir", "Sedang", "537,75 Ha • 1.245 Jiwa • Rp 40,0 M"),
                KrbDistrictHazardBadge("Gelombang Ekstrem & Abrasi", "Sedang", "197,01 Ha • 774 Jiwa"),
                KrbDistrictHazardBadge("Tanah Longsor", "Sedang", "7.068,78 Ha • 878 Jiwa")
            ),
            recommendationSummary = "Penguatan jalur evakuasi tsunami & perlindungan permukiman nelayan Pantai Air Rami dari gelombang ekstrem, serta stabilisasi lereng di desa perbukitan."
        ),
        KrbDistrictRiskProfile(
            districtId = "air_manjunto",
            districtName = "Air Manjunto",
            capitalName = "Manjunto Jaya",
            villagesCount = 8,
            wardsCount = 0,
            areaHa = 5_303.78,
            exposedPopulation = 12_820,
            vulnerableAgePopulation = 2_407,
            poorPopulationP3KE = 109,
            disabilityPopulation = 19,
            healthFacilitiesCount = 6,
            ikdTransformed = 0.242,
            ikmScore = 0.289,
            multiHazardCapacityIndex = 0.260,
            multiHazardCapacityClass = "Rendah",
            physicalLossMillionRp = 317_800.38,
            economicLossMillionRp = 16_834.59,
            totalLossMillionRp = 334_634.96,
            envDamageHa = 0.0,
            riskLowAreaHa = 163.35,
            riskMediumAreaHa = 1_893.60,
            riskHighAreaHa = 3_246.83,
            multiHazardRiskClass = "Tinggi",
            isCoastalTsunamiZone = false,
            isFlashFloodZone = false,
            primaryHazardBadges = listOf(
                KrbDistrictHazardBadge("Cuaca Ekstrem", "Tinggi", "12.780 Jiwa • Rp 334,1 M"),
                KrbDistrictHazardBadge("Karhutla", "Tinggi", "4.380,48 Ha"),
                KrbDistrictHazardBadge("Banjir", "Sedang", "1.464,12 Ha • 2.085 Jiwa"),
                KrbDistrictHazardBadge("Kekeringan", "Sedang", "12.820 Jiwa • Kawasan Bendung Manjunto"),
                KrbDistrictHazardBadge("Gempabumi", "Sedang", "5.303,78 Ha • Keamanan Bendungan")
            ),
            recommendationSummary = "Pengamanan infrastruktur strategis Bendung Air Manjunto, pemeliharaan jaringan drainase irigasi, dan peningkatan kapasitas kesiapsiagaan warga terhadap cuaca ekstrem."
        )
    )

    fun getDistrictProfile(district: District): KrbDistrictRiskProfile {
        return getDistrictProfileByIdOrName(district.id, district.name)
    }

    fun getDistrictProfileByIdOrName(id: String?, name: String?): KrbDistrictRiskProfile {
        val cleanId = id?.trim().orEmpty()
        val cleanName = name?.trim().orEmpty()
        return districtProfiles.firstOrNull {
            it.districtId.equals(cleanId, ignoreCase = true) ||
                it.districtName.equals(cleanName, ignoreCase = true) ||
                it.districtName.equals(cleanId, ignoreCase = true)
        } ?: districtProfiles.first()
    }
}
