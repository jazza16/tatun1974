package com.example.model

import androidx.annotation.Keep

/**
 * Dataset Resmi Peraturan Daerah Kabupaten Mukomuko Nomor 12 Tahun 2025
 * Tentang Rencana Tata Ruang Wilayah (RTRW) Kabupaten Mukomuko Tahun 2025–2045
 * Ditetapkan & Diundangkan di Mukomuko: 3 November 2025
 * Bupati Mukomuko: Choirul Huda | Sekretaris Daerah: Marjohan
 * Lembaran Daerah Kabupaten Mukomuko Tahun 2025 Nomor 12 (NOREG 12/49/2025)
 */
@Keep
data class RtrwSpecialDisasterRule(
    val id: String,
    val articleReference: String,
    val zoneTitle: String,
    val krbAlignmentNote: String,
    val allowedActivities: List<String>,
    val conditionalActivities: List<String>,
    val prohibitedActivities: List<String>
)

@Keep
data class RtrwSpatialPatternSummary(
    val category: String, // "Kawasan Lindung" atau "Kawasan Budi Daya"
    val zoneName: String,
    val articleReference: String,
    val areaHa: Int,
    val distributionSummary: String,
    val intensityRule: String
)

@Keep
data class RtrwBpbdProgramItem(
    val code: String,
    val programName: String,
    val location: String,
    val fundingSource: String,
    val executor: String,
    val period: String
)

@Keep
data class RtrwDistrictSpatialProfile(
    val districtId: String,
    val districtName: String,
    val settlementHierarchyCode: String, // "PKW", "PKL", "PPK", "PPL"
    val settlementHierarchyTitle: String,
    val urbanOrRuralCenterName: String,
    val article10Reference: String,
    val strategicAreaStatus: String, // Pasal 44-45
    val protectedZones: List<String>, // Pasal 30-35
    val cultivationZones: List<String>, // Pasal 36-43
    val vitalInfrastructureAndMitigation: List<String>, // Pasal 13-28
    val applicablePasal92And93Rules: List<String>,
    val spatialControlDirective: String
)

object MukomukoRtrw2025Catalog {
    const val PERDA_TITLE = "Peraturan Daerah Kabupaten Mukomuko Nomor 12 Tahun 2025"
    const val PERDA_SUBTITLE = "Rencana Tata Ruang Wilayah (RTRW) Kabupaten Mukomuko Tahun 2025–2045"
    const val ENACTMENT_INFO = "Ditetapkan 3 November 2025 • Bupati Mukomuko: Choirul Huda • Lembaran Daerah No. 12 Tahun 2025 (NOREG 12/49/2025)"
    const val REVOKED_PERDA_INFO = "Mencabut Perda Kab. Mukomuko No. 6 Tahun 2012 tentang RTRW Tahun 2012–2032 (Pasal 123)"
    const val ADMIN_AREA_HA = 413_752 // Pasal 2 ayat (1) - Kepmendagri No. 300.2.2-2138 Tahun 2025
    const val PLANNING_PATTERN_AREA_HA = 413_866 // Penjelasan Pasal 2 ayat (1)
    const val RTRW_VISION_PASAL_4 =
        "Mewujudkan Ruang Wilayah yang produktif berbasis agropolitan dan minapolitan yang didukung oleh Kawasan perkotaan dan perdesaan yang mandiri dalam sistem Wilayah yang terpadu dan berkelanjutan serta adaptif terhadap kebencanaan (Pasal 4)."

    /**
     * Ketentuan Khusus Kawasan Rawan Bencana (Pasal 92 ayat 1–9) & Kawasan Sempadan (Pasal 93 ayat 1–4)
     */
    val specialDisasterAndBufferRules: List<RtrwSpecialDisasterRule> = listOf(
        RtrwSpecialDisasterRule(
            id = "krb_tsunami",
            articleReference = "Pasal 92 ayat (8)",
            zoneTitle = "Ketentuan Khusus Kawasan Rawan Bencana Tsunami",
            krbAlignmentNote = "Selaras dengan Prioritas 1 KRB 2026–2030 pada 8 Kecamatan Pesisir (17.492,58 Ha; 31.122 jiwa terpapar).",
            allowedActivities = listOf(
                "Perlindungan dan penetapan vegetasi pantai, mangrove, gumuk/bukit pasir, dan sempadan pantai.",
                "Pendirian bangunan pengamanan pantai serta bangunan pemantauan ancaman bencana tsunami (EWS/InaTEWS).",
                "Pengembangan kawasan jalur hijau (green belt) dan pembangunan sarana prasarana sistem mitigasi bencana."
            ),
            conditionalActivities = listOf(
                "Pemanfaatan ruang pada kawasan rawan gelombang tsunami wajib melakukan Analisis Risiko Bencana Tsunami.",
                "Pembatasan kegiatan hunian, wisata, dan pendukung wisata pantai sesuai persyaratan mitigasi bencana.",
                "Bangunan gedung wajib dilaksanakan dengan konstruksi tahan tsunami serta menyediakan tempat dan jalur evakuasi.",
                "Pembatasan pendirian bangunan kecuali untuk pemantauan ancaman bencana dan kepentingan umum."
            ),
            prohibitedActivities = listOf(
                "Kegiatan yang meningkatkan risiko bencana tsunami.",
                "Kegiatan pengambilan terumbu karang dan pengerusakan ekosistem mangrove.",
                "Kegiatan yang mengganggu sarana, prasarana, serta jalur dan tempat evakuasi sistem mitigasi."
            )
        ),
        RtrwSpecialDisasterRule(
            id = "krb_banjir_bandang",
            articleReference = "Pasal 92 ayat (3)",
            zoneTitle = "Ketentuan Khusus Kawasan Rawan Bencana Banjir Bandang",
            krbAlignmentNote = "Selaras dengan Prioritas 1 KRB 2026–2030 pada 7 Kecamatan koridor sungai Bukit Barisan (6.582,78 Ha).",
            allowedActivities = listOf(
                "Membangun sistem jaringan prasarana mitigasi banjir bandang dan sistem jaringan evakuasi bencana.",
                "Wajib melaksanakan reboisasi dan pengerukan sekitar sungai hulu dan hilir.",
                "Penyediaan sistem atau alat peringatan dini (EWS) banjir bandang.",
                "Mempertahankan area Ruang Terbuka Hijau (RTH) di sekitar sempadan sungai hulu dan hilir."
            ),
            conditionalActivities = listOf(
                "Penyediaan struktur tanggul sebagai bangunan pengendali banjir bandang sesuai standar konstruksi.",
                "Kegiatan hunian diperbolehkan secara terbatas dengan syarat tidak menimbulkan/meningkatkan risiko banjir.",
                "Bangunan gedung wajib menyediakan tempat dan jalur evakuasi serta papan peringatan/rambu pengamanan."
            ),
            prohibitedActivities = listOf(
                "Kegiatan yang meningkatkan risiko bencana banjir bandang di koridor sungai hulu-hilir.",
                "Kegiatan yang mengganggu sarana dan prasarana sistem mitigasi bencana."
            )
        ),
        RtrwSpecialDisasterRule(
            id = "krb_banjir",
            articleReference = "Pasal 92 ayat (2)",
            zoneTitle = "Ketentuan Khusus Kawasan Rawan Bencana Banjir",
            krbAlignmentNote = "Selaras dengan Prioritas 1 KRB 2026–2030 di 15 Kecamatan (63.820,62 Ha; 63.030 jiwa terpapar).",
            allowedActivities = listOf(
                "Membangun sistem jaringan prasarana mitigasi banjir dan saluran drainase sesuai kemiringan daerah pengaliran.",
                "Penghijauan, reboisasi, pendirian tanggul, drainase, pintu air, sumur resapan, biopori, dan normalisasi muara sungai.",
                "Pengembangan sistem jaringan evakuasi bencana banjir."
            ),
            conditionalActivities = listOf(
                "Kegiatan hunian diperbolehkan terbatas dan pembangunan fasilitas umum dengan kepadatan rendah.",
                "Pembatasan pendirian bangunan kecuali untuk pemantauan ancaman bencana dan kepentingan umum.",
                "Bangunan gedung wajib menyediakan tempat dan jalur evakuasi."
            ),
            prohibitedActivities = listOf(
                "Kegiatan yang meningkatkan risiko bencana banjir atau menyumbat jaringan drainase/pengendali banjir.",
                "Kegiatan yang mengganggu sarana dan prasarana sistem mitigasi bencana."
            )
        ),
        RtrwSpecialDisasterRule(
            id = "krb_karhutla",
            articleReference = "Pasal 92 ayat (5) & Pasal 83 ayat (4)c",
            zoneTitle = "Ketentuan Khusus Kawasan Rawan Kebakaran Hutan & Lahan (Karhutla)",
            krbAlignmentNote = "Selaras dengan Prioritas 1 KRB 2026–2030 (404.993,70 Ha; AUC model 0,932; gambut & perkebunan).",
            allowedActivities = listOf(
                "Membangun sistem jaringan prasarana mitigasi bencana kebakaran hutan dan lahan.",
                "Pengembangan hutan kemasyarakatan sebagai zona penyangga (buffer zone).",
                "Penyediaan infrastruktur pemadaman yang memadai dan pengembangan jaringan evakuasi Karhutla."
            ),
            conditionalActivities = listOf(
                "Kegiatan budi daya dilaksanakan dengan memperhatikan aspek mitigasi kebakaran.",
                "Kegiatan hunian diperbolehkan terbatas dengan syarat tidak menimbulkan risiko kebakaran.",
                "Pemasangan papan penyuluhan, papan peringatan, dan rambu pengamanan Karhutla."
            ),
            prohibitedActivities = listOf(
                "Kegiatan pembukaan dan/atau mengolah lahan dengan cara pembakaran (Pasal 83 ayat 4 huruf c angka 4).",
                "Kegiatan yang meningkatkan risiko bencana kebakaran hutan dan lahan.",
                "Kegiatan yang mengganggu sarana dan prasarana sistem mitigasi dan pemadaman."
            )
        ),
        RtrwSpecialDisasterRule(
            id = "krb_longsor",
            articleReference = "Pasal 92 ayat (7) & Pasal 94 ayat (1)c",
            zoneTitle = "Ketentuan Khusus Kawasan Rawan Bencana Tanah Longsor",
            krbAlignmentNote = "Selaras dengan Prioritas 1 KRB 2026–2030 di 13 Kecamatan perbukitan (211.034,88 Ha).",
            allowedActivities = listOf(
                "Membangun sistem jaringan prasarana mitigasi longsor, rehabilitasi lahan, dan konservasi tanah.",
                "Membuat terasering, talud atau turap, reboisasi, dan penyediaan sistem peringatan dini (EWS) tanah longsor.",
                "Penanaman vegetasi asli dan berakar tunggang/kuat pada jaringan jalan dan lahan kritis."
            ),
            conditionalActivities = listOf(
                "Pembatasan pemanfaatan ruang hunian dengan intensitas bangunan rendah; diarahkan untuk hutan, RTH, atau perkebunan tanaman keras.",
                "Pembangunan wajib memperhatikan teknis stabilitas lereng, drainase lereng, tidak berada di bantaran sungai, serta melalui kajian geologi teknik dan analisis risiko bencana."
            ),
            prohibitedActivities = listOf(
                "DILARANG mengembangkan permukiman baru di Kawasan rawan bencana longsor (Pasal 92 ayat 7 huruf c angka 2).",
                "DILARANG melakukan penggalian/pertambangan pada lereng curam > 40% dengan kemantapan lereng kurang stabil (Pasal 94 ayat 1 huruf c angka 3).",
                "Kegiatan yang meningkatkan risiko gerakan tanah/longsor."
            )
        ),
        RtrwSpecialDisasterRule(
            id = "krb_gea",
            articleReference = "Pasal 92 ayat (9)",
            zoneTitle = "Ketentuan Khusus Kawasan Rawan Gelombang Ekstrem dan Abrasi",
            krbAlignmentNote = "Selaras dengan Prioritas 1 KRB 2026–2030 di 7 Kecamatan Pesisir (2.892,69 Ha; tinggi gelombang s/d 2,7 m).",
            allowedActivities = listOf(
                "Perlindungan dan penetapan vegetasi pantai, mangrove, gumuk/bukit pasir, dan sempadan pantai.",
                "Pemasangan pemecah gelombang (breakwater) dan/atau penahan gelombang (seawall/revetment).",
                "Pendirian bangunan pengamanan pantai, pemantauan gelombang ekstrem, dan kawasan jalur hijau."
            ),
            conditionalActivities = listOf(
                "Pemanfaatan ruang wajib melakukan Analisis Risiko Bencana Gelombang Ekstrem dan Abrasi.",
                "Pembatasan kegiatan hunian dan wisata pantai sesuai persyaratan mitigasi bencana.",
                "Bangunan gedung wajib menyediakan tempat dan jalur evakuasi."
            ),
            prohibitedActivities = listOf(
                "Kegiatan yang meningkatkan risiko gelombang ekstrem dan abrasi pantai.",
                "Kegiatan pengambilan terumbu karang dan pengerusakan ekosistem mangrove.",
                "Kegiatan yang mengganggu sarana dan prasarana sistem mitigasi pesisir."
            )
        ),
        RtrwSpecialDisasterRule(
            id = "krb_likuefaksi",
            articleReference = "Pasal 92 ayat (6)",
            zoneTitle = "Ketentuan Khusus Kawasan Rawan Bencana Likuefaksi",
            krbAlignmentNote = "Selaras dengan Prioritas 2 KRB 2026–2030 pada 7 Kecamatan pesisir/aluvial (11.962,71 Ha; 24.142 jiwa).",
            allowedActivities = listOf(
                "Membangun sistem jaringan prasarana mitigasi bencana likuefaksi.",
                "Pengembangan sistem jaringan evakuasi bencana likuefaksi."
            ),
            conditionalActivities = listOf(
                "Permukiman kepadatan sedang-rendah wajib mematuhi dokumen lingkungan serta penggunaan rekayasa teknik terhadap infrastruktur, struktur, dan pondasi bangunan tahan likuefaksi.",
                "Jaringan prasarana, fasilitas umum, dan fasilitas sosial wajib melalui kajian teknis instansi berwenang dan rekayasa pondasi tahan likuefaksi."
            ),
            prohibitedActivities = listOf(
                "Melakukan kegiatan yang meningkatkan risiko terdampak bencana likuefaksi.",
                "Kegiatan yang mengganggu fungsi Jalur Evakuasi Bencana dan Tempat Evakuasi Bencana."
            )
        ),
        RtrwSpecialDisasterRule(
            id = "krb_gempa",
            articleReference = "Pasal 92 ayat (4)",
            zoneTitle = "Ketentuan Khusus Kawasan Rawan Bencana Gempa Bumi",
            krbAlignmentNote = "Selaras dengan Prioritas 3 KRB 2026–2030 di seluruh 15 Kecamatan (414.320,37 Ha; potensi rugi Rp 3,182 T).",
            allowedActivities = listOf(
                "Membangun sistem jaringan prasarana mitigasi bencana gempa bumi.",
                "Pengembangan sistem jaringan evakuasi bencana gempa bumi di seluruh kecamatan."
            ),
            conditionalActivities = listOf(
                "Permukiman kepadatan sedang hingga rendah wajib menggunakan rekayasa teknik infrastruktur bangunan tahan gempa.",
                "Jaringan prasarana, fasilitas umum (sekolah/SPAB), dan fasilitas sosial (rumah sakit/RSAB) wajib memenuhi standar rekayasa bangunan tahan gempa."
            ),
            prohibitedActivities = listOf(
                "Melakukan kegiatan yang meningkatkan risiko terdampak bencana gempa bumi.",
                "Kegiatan yang mengganggu fungsi Jalur Evakuasi Bencana dan Tempat Evakuasi Bencana."
            )
        ),
        RtrwSpecialDisasterRule(
            id = "sempadan_pantai_sungai_danau",
            articleReference = "Pasal 93 ayat (1)–(4)",
            zoneTitle = "Ketentuan Khusus Kawasan Sempadan Pantai, Sungai, & Danau/Waduk",
            krbAlignmentNote = "Instrumen pengendali tata ruang utama untuk mencegah korban jiwa tsunami, abrasi, banjir, dan banjir bandang.",
            allowedActivities = listOf(
                "Sempadan Pantai (Pasal 93 ayat 2): Pemertahanan titik garis pangkal daratan dari abrasi, konservasi ekosistem pesisir, RTH, struktur alami & buatan pencegah abrasi.",
                "Sempadan Sungai (Pasal 93 ayat 3): Perlindungan sempadan sungai WS Teramang-Muar, prasarana mitigasi banjir, dan RTH.",
                "Sempadan Danau/Waduk (Pasal 93 ayat 4): Perlindungan sempadan Danau Nibung, Danau Lebar, dan Bendung Air Manjunto."
            ),
            conditionalActivities = listOf(
                "Permukiman eksisting sebelum Perda No. 12 Tahun 2025 ditetapkan hanya diperbolehkan dengan syarat TIDAK menambah intensitas bangunan.",
                "Pendirian bangunan baru dibatasi hanya untuk menunjang wisata alam/pantai, transportasi laut, serta prasarana/utilitas mitigasi bencana."
            ),
            prohibitedActivities = listOf(
                "Kegiatan budi daya yang menurunkan fungsi lindung, nilai ekologis, dan estetika kawasan sempadan.",
                "Membuang limbah secara langsung atau mengubah bentang alam yang menurunkan kualitas fisik pantai, sungai, dan danau/waduk."
            )
        )
    )

    /**
     * Rencana Pola Ruang Wilayah Kabupaten Mukomuko (BAB V Pasal 30–43) & Ketentuan Intensitas KDB/KDH/KLB (Pasal 79–88)
     */
    val spatialPatternSummaries: List<RtrwSpatialPatternSummary> = listOf(
        RtrwSpatialPatternSummary(
            category = "Kawasan Lindung",
            zoneName = "Taman Nasional Kerinci Seblat (TNKS)",
            articleReference = "Pasal 33 ayat (5) & Pasal 78",
            areaHa = 146_021,
            distributionSummary = "7 Kecamatan: Malin Deman, Penarik, Pondok Suguh, Selagan Raya, Sungai Rumbai, Teramang Jaya, V Koto.",
            intensityRule = "Fungsi lindung mutlak hulu DAS; dilarang keras pertambangan & perusakan ekosistem hutan."
        ),
        RtrwSpatialPatternSummary(
            category = "Kawasan Lindung",
            zoneName = "Taman Wisata Alam (TWA)",
            articleReference = "Pasal 33 ayat (6) & Pasal 78",
            areaHa = 2_321,
            distributionSummary = "6 Kecamatan: Air Rami, Ipuh, Kota Mukomuko, Pondok Suguh, Sungai Rumbai, XIV Koto.",
            intensityRule = "Konservasi & wisata alam terbatas tanpa mengubah bentang alam; berbasis mitigasi bencana."
        ),
        RtrwSpatialPatternSummary(
            category = "Kawasan Lindung",
            zoneName = "Badan Air (Sungai, Danau, Waduk)",
            articleReference = "Pasal 31 & Pasal 76",
            areaHa = 1_906,
            distributionSummary = "Seluruh 15 Kecamatan (Sungai WS Teramang-Muar, Danau Nibung, Danau Lebar, Bendung Manjunto).",
            intensityRule = "Dilarang mengurangi luas badan air atau mencemari kualitas air."
        ),
        RtrwSpatialPatternSummary(
            category = "Kawasan Lindung",
            zoneName = "Kawasan Ekosistem Mangrove",
            articleReference = "Pasal 35 & Pasal 80",
            areaHa = 179,
            distributionSummary = "5 Kecamatan Pesisir: Air Rami, Kota Mukomuko, Pondok Suguh, Sungai Rumbai, XIV Koto.",
            intensityRule = "Benteng alami tsunami & abrasi; dilarang menebang kayu mangrove atau mengurangi luas mangrove."
        ),
        RtrwSpatialPatternSummary(
            category = "Kawasan Lindung",
            zoneName = "Cagar Alam & Hutan Lindung (+ Gambut)",
            articleReference = "Pasal 32, Pasal 33 ayat (3) & Pasal 77",
            areaHa = 105, // 101 Ha Cagar Alam + 4 Ha Hutan Lindung
            distributionSummary = "Kecamatan XIV Koto (Cagar Alam 101 Ha & Hutan Lindung 4 Ha + Pelestarian Kawasan Bergambut).",
            intensityRule = "Perlindungan sistem tata air alami & gambut; dilarang menggunakan alat berat/mengubah fungsi lindung."
        ),
        RtrwSpatialPatternSummary(
            category = "Kawasan Budi Daya",
            zoneName = "Kawasan Perkebunan",
            articleReference = "Pasal 38 ayat (4) & Pasal 83 ayat (4)",
            areaHa = 143_560,
            distributionSummary = "Seluruh 15 Kecamatan.",
            intensityRule = "KDB maks 40% • KDH min 60% • KLB maks 1x KDB • Dilarang bakar lahan & tanaman boros air di hulu."
        ),
        RtrwSpatialPatternSummary(
            category = "Kawasan Budi Daya",
            zoneName = "Hutan Produksi Terbatas (HPT) & Tetap (HP) & Konversi (HPK)",
            articleReference = "Pasal 37 & Pasal 82",
            areaHa = 78_050, // 63.343 HPT + 11.837 HP + 2.870 HPK
            distributionSummary = "HPT (63.343 Ha di 9 Kec), HP (11.837 Ha di 8 Kec), HPK (2.870 Ha di Penarik, Selagan Raya, XIV Koto).",
            intensityRule = "KDB maks 10% • KDH min 90% • KLB maks 1x KDB • Wajib AMDAL & rehabilitasi kawasan resapan air."
        ),
        RtrwSpatialPatternSummary(
            category = "Kawasan Budi Daya",
            zoneName = "Kawasan Permukiman Perdesaan",
            articleReference = "Pasal 41 ayat (3) & Pasal 86 ayat (3)",
            areaHa = 28_560,
            distributionSummary = "Seluruh 15 Kecamatan.",
            intensityRule = "KDB maks 60% • KDH min 20% • KLB maks 2x KDB • Wajib memiliki lokasi & Jalur Evakuasi Bencana."
        ),
        RtrwSpatialPatternSummary(
            category = "Kawasan Budi Daya",
            zoneName = "Kawasan Permukiman Perkotaan",
            articleReference = "Pasal 41 ayat (2) & Pasal 86 ayat (2)",
            areaHa = 7_282,
            distributionSummary = "8 Kecamatan: Ipuh, Kota Mukomuko, Lubuk Pinang, Penarik, Pondok Suguh, Teramang Jaya, V Koto, XIV Koto.",
            intensityRule = "KDB maks 60% • KDH min 20% • KLB maks 4x KDB • Wajib RTH Perkotaan min 30% (20% publik, 10% privat)."
        ),
        RtrwSpatialPatternSummary(
            category = "Kawasan Budi Daya",
            zoneName = "Kawasan Tanaman Pangan & KP2B (LP2B + LCP2B)",
            articleReference = "Pasal 38 ayat (2)&(5), Pasal 83 & Pasal 91",
            areaHa = 5_554, // KP2B 5.554 Ha (LP2B 4.032 Ha + LCP2B 1.522 Ha); Tanaman Pangan 4.909 Ha
            distributionSummary = "11 Kecamatan (LP2B 4.032 Ha) & 4 Kecamatan LCP2B (1.522 Ha di Air Manjunto, Lubuk Pinang, V Koto, XIV Koto).",
            intensityRule = "KDB maks 15% • KDH min 85% • Dilarang alih fungsi LP2B kecuali kepentingan umum/bencana (wajib ganti lahan maks 2 thn)."
        ),
        RtrwSpatialPatternSummary(
            category = "Kawasan Budi Daya",
            zoneName = "Kawasan Peruntukan Industri (KPI)",
            articleReference = "Pasal 39 & Pasal 84",
            areaHa = 724,
            distributionSummary = "8 Kecamatan: Ipuh, Lubuk Pinang, Malin Deman, Penarik, Pondok Suguh, Selagan Raya, Teramang Jaya, Teras Terunjam.",
            intensityRule = "KDB maks 50% • KDH min 30% • KLB maks 3x KDB • Wajib IPAL terpadu, buffer zone, & dilarang tutup jalur evakuasi."
        )
    )

    /**
     * Indikasi Program Utama Jangka Menengah Perwujudan Sistem Jaringan Evakuasi Bencana (Lampiran XII Program A.2.5.5)
     */
    val bpbdEvacuationPrograms: List<RtrwBpbdProgramItem> = listOf(
        RtrwBpbdProgramItem(
            code = "A.2.5.5.1",
            programName = "Penyusunan Kajian Risiko Bencana dan Rencana Penanggulangan Bencana",
            location = "Kabupaten Mukomuko (15 Kecamatan)",
            fundingSource = "APBD Kabupaten",
            executor = "BPBD Kab. Mukomuko, BPBD Provinsi, BNPB",
            period = "2025–2045 (Telah Terwujud melalui Dokumen KRB 2026–2030)"
        ),
        RtrwBpbdProgramItem(
            code = "A.2.5.5.2",
            programName = "Pengembangan Sistem Peringatan Dini dan Peningkatan Adaptasi pada Kawasan Rawan Bencana",
            location = "Kabupaten Mukomuko (15 Kecamatan)",
            fundingSource = "APBN, APBD Provinsi, APBD Kabupaten",
            executor = "BPBD Kab. Mukomuko, BPBD Provinsi, BNPB",
            period = "2025–2045 (Diimplementasikan melalui Sistem SIPAGAR KITA)"
        ),
        RtrwBpbdProgramItem(
            code = "A.2.5.5.3",
            programName = "Pembangunan, Pengembangan, dan Pemeliharaan Jalur Evakuasi Bencana",
            location = "Seluruh Kecamatan (15 Kecamatan)",
            fundingSource = "APBN, APBD Provinsi, APBD Kabupaten",
            executor = "BPBD Kab. Mukomuko, Dinas PUPR, BNPB",
            period = "2025–2045 (Pasal 27 ayat 2 & Pasal 72 ayat 2)"
        ),
        RtrwBpbdProgramItem(
            code = "A.2.5.5.5",
            programName = "Pembangunan, Pengembangan, dan Pemeliharaan Tempat Evakuasi Bencana (TES/TEA)",
            location = "Seluruh Kecamatan (15 Kecamatan)",
            fundingSource = "APBN, APBD Provinsi, APBD Kabupaten",
            executor = "BPBD Kab. Mukomuko, Dinas PUPR, BNPB",
            period = "2025–2045 (Pasal 27 ayat 3 & Pasal 72 ayat 3)"
        ),
        RtrwBpbdProgramItem(
            code = "A.2.5.5.6",
            programName = "Pengelolaan Sistem Penanggulangan Kawasan Rawan Bencana dengan Rekayasa Teknis",
            location = "Kabupaten Mukomuko (15 Kecamatan)",
            fundingSource = "APBN, APBD Provinsi, APBD Kabupaten",
            executor = "BPBD Kab. Mukomuko, Dinas PUPR, BNPB",
            period = "2025–2045"
        )
    )

    /**
     * Profil Tata Ruang & Mitigasi Bencana Berdasarkan Perda RTRW No. 12 Tahun 2025 (2025–2045) untuk 15 Kecamatan
     */
    val districtSpatialProfiles: List<RtrwDistrictSpatialProfile> = listOf(
        RtrwDistrictSpatialProfile(
            districtId = "kota_mukomuko",
            districtName = "Kota Mukomuko",
            settlementHierarchyCode = "PKW",
            settlementHierarchyTitle = "Pusat Kegiatan Wilayah (PKW) Skala Provinsi/Regional",
            urbanOrRuralCenterName = "Kawasan Perkotaan Mukomuko",
            article10Reference = "Pasal 10 ayat (2)",
            strategicAreaStatus = "Kawasan Strategis Kabupaten (KSK) Fungsi & Daya Dukung Lingkungan Hidup Kawasan Perkotaan Mukomuko yang Adaptif terhadap Ancaman Bencana Alam (Pasal 44 ayat 6 & Pasal 45 ayat 2).",
            protectedZones = listOf(
                "Kawasan Ekosistem Mangrove (Pasal 35 huruf b)",
                "Taman Wisata Alam / TWA (Pasal 33 ayat 6 huruf c)",
                "Kawasan Cagar Budaya Benteng Anna (Pasal 34)",
                "Badan Air & Sempadan Pantai/Sungai/Danau Lebar & Danau Nibung (Pasal 31 & 93)"
            ),
            cultivationZones = listOf(
                "Kawasan Permukiman Perkotaan (Pasal 41 ayat 2 huruf b — Wajib RTH min 30%)",
                "Kawasan Transportasi Bandar Udara Pengumpan Mukomuko 44 Ha (Pasal 42)",
                "Kawasan Pariwisata 16 Ha & Pantai Indah/Air Patah/Batu Bedoro/Pandan Wangi/Pasir Putih (Pasal 40)",
                "Kawasan Pertahanan & Keamanan: Kodim 0428, Polres Mukomuko, Pos TNI AL, Subdenpom (Pasal 43)"
            ),
            vitalInfrastructureAndMitigation = listOf(
                "Jalur & Tempat Evakuasi Bencana Terpadu (Pasal 27 & Pasal 72)",
                "Jaringan & Bangunan Pengendalian Banjir serta Jaringan Irigasi Primer (Pasal 21 ayat 4, 6, 7)",
                "Pelabuhan Penyeberangan Kelas I (Lintas Mukomuko–Mentawai), Pelabuhan Pengumpan Regional, PPI Padang Penaik (Pasal 15–16)",
                "Bandar Udara Pengumpan Mukomuko & Ketentuan Khusus KKOP (Pasal 17 & Pasal 90)",
                "PLTD, SUTT, SUTM, SPAL Domestik, TPS & TPA, Drainase Primer & Sekunder (Pasal 19–28)"
            ),
            applicablePasal92And93Rules = listOf(
                "Pasal 92 ayat (8) & (9): Wajib Analisis Risiko Tsunami & GEA, shelter evakuasi vertikal, pelarangan perusakan mangrove.",
                "Pasal 92 ayat (6): Wajib rekayasa pondasi bangunan tahan likuefaksi pada gedung bertingkat & fasilitas kritis.",
                "Pasal 93 ayat (2): Pengendalian ketat sempadan pantai & pemasangan pemecah gelombang Pantai Abrasi."
            ),
            spatialControlDirective = "Sebagai PKW & KSK Adaptif Bencana, penyusunan RDTR Kawasan Perkotaan Mukomuko wajib mengunci penyediaan RTH minimal 30%, KKOP Bandara, jalur evakuasi tsunami, dan pengendalian perumahan di zona rawan bencana tinggi (Lampiran XII B.2.5.1.6)."
        ),
        RtrwDistrictSpatialProfile(
            districtId = "ipuh",
            districtName = "Ipuh",
            settlementHierarchyCode = "PKL",
            settlementHierarchyTitle = "Pusat Kegiatan Lokal (PKL) Skala Kabupaten/Beberapa Kecamatan",
            urbanOrRuralCenterName = "Kawasan Perkotaan Ipuh",
            article10Reference = "Pasal 10 ayat (3)",
            strategicAreaStatus = "Pusat pertumbuhan ekonomi dan pelayanan kelautan/perikanan koridor selatan Kabupaten Mukomuko (Pasal 49 ayat 2 huruf a angka 2m).",
            protectedZones = listOf(
                "Taman Wisata Alam / TWA (Pasal 33 ayat 6 huruf b)",
                "Badan Air & Sempadan Pantai/Sungai (Pasal 31 & Pasal 93)"
            ),
            cultivationZones = listOf(
                "Kawasan Permukiman Perkotaan (Pasal 41 ayat 2 huruf a — Wajib RTH min 30%)",
                "Kawasan Peruntukan Industri / KPI (Pasal 39 huruf a)",
                "Hutan Produksi Terbatas (HPT) & Hutan Produksi Tetap (HP) (Pasal 37 ayat 2a & 3a)",
                "Kawasan Tanaman Pangan & LP2B (Pasal 38 ayat 2d & 5a) serta Kawasan Perkebunan",
                "Lokasi Pariwisata Pantai Batu Kumbang, Air Buluh, Danau Darngut, Ipuh Sakti, TPI & Air Terjun Teluak (Pasal 40)"
            ),
            vitalInfrastructureAndMitigation = listOf(
                "Jalur & Tempat Evakuasi Bencana (Pasal 27)",
                "Bangunan Pengendalian Banjir (Pasal 21 ayat 7 huruf a)",
                "Jalan Arteri Primer Ruas Bantal–Ipuh & Ipuh–Seblat serta Terminal Penumpang Tipe C (Pasal 13)",
                "Unit Air Baku, Unit Produksi, SPAL Domestik, Pengelolaan Limbah B3, TPS & TPA, Drainase Primer & Sekunder (Pasal 23–28)"
            ),
            applicablePasal92And93Rules = listOf(
                "Pasal 92 ayat (8) & (9): Wajib jalur/tempat evakuasi tsunami & perlindungan sempadan pantai dari abrasi.",
                "Pasal 92 ayat (6): Wajib rekayasa teknik pondasi tahan likuefaksi pada permukiman & fasilitas publik PKL Ipuh.",
                "Pasal 84 huruf c angka 4: Kawasan Peruntukan Industri dilarang menghalangi/menutup Jalur Evakuasi Bencana."
            ),
            spatialControlDirective = "Prioritas penetapan RDTR Kawasan Perkotaan Ipuh (Lampiran XII A.1.2.1) dengan integrasi jalur evakuasi tsunami inklusif, bangunan pengendali banjir, dan pengendalian limbah industri pengolahan."
        ),
        RtrwDistrictSpatialProfile(
            districtId = "penarik",
            districtName = "Penarik",
            settlementHierarchyCode = "PPK",
            settlementHierarchyTitle = "Pusat Pelayanan Kawasan (PPK) Skala Kecamatan",
            urbanOrRuralCenterName = "Kawasan Perkotaan Lubuk Mukti",
            article10Reference = "Pasal 10 ayat (5) huruf a",
            strategicAreaStatus = "Simpul agribisnis, perkebunan, industri pengolahan, dan koridor penghubung Penarik–Lempur Kerinci (Lampiran XII A.2.1.1.10).",
            protectedZones = listOf(
                "Taman Nasional Kerinci Seblat / TNKS (Pasal 33 ayat 5 huruf b)",
                "Badan Air & Sempadan Sungai (Pasal 31 & Pasal 93)"
            ),
            cultivationZones = listOf(
                "Kawasan Permukiman Perkotaan (Pasal 41 ayat 2 huruf d) & Perdesaan",
                "Kawasan Peruntukan Industri / KPI (Pasal 39 huruf d)",
                "Hutan Produksi Terbatas (HPT), Hutan Produksi Tetap (HP), & Hutan Produksi Konversi (HPK) (Pasal 37)",
                "Kawasan Tanaman Pangan, LP2B, Hortikultura, & Perkebunan (Pasal 38)"
            ),
            vitalInfrastructureAndMitigation = listOf(
                "Jalur & Tempat Evakuasi Bencana (Pasal 27)",
                "Terminal Penumpang Tipe C & Pembangkit Listrik Tenaga Minihidro (Pasal 13 ayat 9d & Pasal 19 ayat 6b)",
                "Unit Air Baku, Unit Produksi, SPAL Domestik, Pengelolaan Limbah B3, TPS, Drainase Primer & Sekunder (Pasal 23–28)"
            ),
            applicablePasal92And93Rules = listOf(
                "Pasal 92 ayat (3): Ketentuan khusus KRB Banjir Bandang (reboisasi hulu-hilir, EWS, RTH sempadan sungai).",
                "Pasal 92 ayat (5) & Pasal 83 ayat (4)c: Larangan keras pembukaan lahan dengan pembakaran (mitigasi Karhutla).",
                "Pasal 92 ayat (7): Pengendalian lereng rawan tanah longsor dan larangan hunian baru di zona longsor tinggi."
            ),
            spatialControlDirective = "Penyusunan RDTR Kawasan Perkotaan Lubuk Mukti (Lampiran XII A.1.3.1), perlindungan kawasan penyangga TNKS dari alih fungsi lahan, dan pengawasan ketat tata ruang industri kelapa sawit (KPI)."
        ),
        RtrwDistrictSpatialProfile(
            districtId = "lubuk_pinang",
            districtName = "Lubuk Pinang",
            settlementHierarchyCode = "PPK",
            settlementHierarchyTitle = "Pusat Pelayanan Kawasan (PPK) Skala Kecamatan",
            urbanOrRuralCenterName = "Kawasan Perkotaan Lubuk Pinang",
            article10Reference = "Pasal 10 ayat (5) huruf b",
            strategicAreaStatus = "Kawasan Strategis Kabupaten (KSK) Pertumbuhan Ekonomi Kawasan Agropolitan Lubuk Pinang – Air Manjunto (Pasal 44 ayat 5a & Pasal 45 ayat 1a).",
            protectedZones = listOf(
                "Badan Air & Sempadan Sungai/Saluran Irigasi (Pasal 31 & Pasal 93)"
            ),
            cultivationZones = listOf(
                "Kawasan Pertanian Pangan Berkelanjutan (KP2B): LP2B & LCP2B Lumbung Pangan (Pasal 38 ayat 5a & 5b)",
                "Kawasan Permukiman Perkotaan (Pasal 41 ayat 2 huruf c) & Perdesaan",
                "Kawasan Peruntukan Industri / KPI (Pasal 39 huruf b)",
                "Hutan Produksi Terbatas (HPT) & Hutan Produksi Tetap (HP) (Pasal 37 ayat 2b & 3b)"
            ),
            vitalInfrastructureAndMitigation = listOf(
                "Jaringan Irigasi Primer, Jaringan Pengendalian Banjir, & Bangunan Pengendalian Banjir (Pasal 21 ayat 4c, 6c, 7c)",
                "Jalan Arteri Primer Ruas Batas Prov. Sumbar–Mukomuko, Terminal Tipe C, & Jembatan Timbang (Pasal 13 & Lampiran XII)",
                "Pembangkit Listrik Tenaga Gas / PLTG (Pasal 19 ayat 3), SPAL Domestik, TPS, Drainase Primer & Sekunder"
            ),
            applicablePasal92And93Rules = listOf(
                "Pasal 91 ayat (3): Larangan ketat alih fungsi Lahan Pertanian Pangan Berkelanjutan (LP2B) kecuali kepentingan umum/bencana.",
                "Pasal 92 ayat (2) & Pasal 66 ayat (5): Perlindungan jaringan irigasi primer dan bangunan pengendali banjir dari sedimentasi/sampah."
            ),
            spatialControlDirective = "Sebagai KSK Agropolitan dan pintu gerbang utara Sumatera Barat, tata ruang wajib mengamankan lahan sawah beririgasi teknis (KP2B) dan jaringan pengendalian banjir."
        ),
        RtrwDistrictSpatialProfile(
            districtId = "pondok_suguh",
            districtName = "Pondok Suguh",
            settlementHierarchyCode = "PPK",
            settlementHierarchyTitle = "Pusat Pelayanan Kawasan (PPK) Skala Kecamatan",
            urbanOrRuralCenterName = "Kawasan Perkotaan Pondok Kandang",
            article10Reference = "Pasal 10 ayat (5) huruf c",
            strategicAreaStatus = "Pusat Pelayanan Kawasan penghubung koridor pesisir dan hulu DAS Teramang.",
            protectedZones = listOf(
                "Taman Nasional Kerinci Seblat / TNKS (Pasal 33 ayat 5 huruf c)",
                "Taman Wisata Alam / TWA (Pasal 33 ayat 6 huruf d)",
                "Kawasan Ekosistem Mangrove (Pasal 35 huruf c)",
                "Badan Air & Sempadan Pantai/Sungai (Pasal 31 & Pasal 93)"
            ),
            cultivationZones = listOf(
                "Kawasan Permukiman Perkotaan (Pasal 41 ayat 2 huruf e) & Perdesaan",
                "Kawasan Peruntukan Industri / KPI (Pasal 39 huruf e)",
                "Hutan Produksi Terbatas (HPT) & Hutan Produksi Tetap (HP) (Pasal 37 ayat 2e & 3e)",
                "Kawasan Hortikultura & Perkebunan serta Wisata Air Terjun Lubuk Bento/Panjang/Makpuang/Mandi Angin & Pantai Teluk Bakung"
            ),
            vitalInfrastructureAndMitigation = listOf(
                "Jalur & Tempat Evakuasi Bencana (Pasal 27)",
                "PLTD & Pembangkit Listrik Tenaga Minihidro (Pasal 19 ayat 2b & 6b)",
                "SPAM Bak Penampungan Air Hujan (Pasal 23 ayat 8), Unit Air Baku/Produksi, SPAL Domestik, TPS & TPA, Drainase Primer & Sekunder"
            ),
            applicablePasal92And93Rules = listOf(
                "Pasal 92 ayat (3): Wajib EWS banjir bandang, reboisasi hulu-hilir sungai, dan tanggul pengendali banjir bandang.",
                "Pasal 92 ayat (8) & Pasal 80: Perlindungan ekosistem mangrove dan sempadan pantai dari tsunami & abrasi.",
                "Pasal 92 ayat (7): Pengendalian ketat kemiringan lereng rawan longsor pada perbukitan timur."
            ),
            spatialControlDirective = "Penetapan RDTR Kawasan Perkotaan Pondok Kandang wajib mengintegrasikan perlindungan bentang hulu-hilir: konservasi TNKS di timur dan sabuk hijau mangrove/evakuasi tsunami di pesisir barat."
        ),
        RtrwDistrictSpatialProfile(
            districtId = "teramang_jaya",
            districtName = "Teramang Jaya",
            settlementHierarchyCode = "PPK",
            settlementHierarchyTitle = "Pusat Pelayanan Kawasan (PPK) Skala Kecamatan",
            urbanOrRuralCenterName = "Kawasan Perkotaan Pasar Bantal",
            article10Reference = "Pasal 10 ayat (5) huruf d",
            strategicAreaStatus = "Kawasan Strategis Kabupaten (KSK) Sentra Kelautan dan Perikanan Teramang Jaya & KSP Kampung Sejahtera Bantal (Pasal 44 ayat 3 & 5b).",
            protectedZones = listOf(
                "Taman Nasional Kerinci Seblat / TNKS (Pasal 33 ayat 5 huruf f)",
                "Badan Air & Sempadan Pantai/Sungai Teramang-Bantal (Pasal 31 & Pasal 93)"
            ),
            cultivationZones = listOf(
                "Kawasan Permukiman Perkotaan (Pasal 41 ayat 2 huruf f) & Perdesaan",
                "Kawasan Peruntukan Industri / KPI (Pasal 39 huruf g)",
                "Hutan Produksi Terbatas (HPT) (Pasal 37 ayat 2 huruf h)",
                "Kawasan Tanaman Pangan, LP2B, & Perkebunan (Pasal 38)"
            ),
            vitalInfrastructureAndMitigation = listOf(
                "Pangkalan Pendaratan Ikan (PPI) Pasar Bantal & Rencana Terminal Khusus Pelabuhan CPO Teramang Jaya (Pasal 16 ayat 4a & 5)",
                "Gardu Induk Listrik Mukomuko, PLTD, & Pembangkit Listrik Tenaga Biomassa (Pasal 19 ayat 2c, 6c, 10)",
                "Pos Babinpotmar TNI AL Bantal & Teramang Jaya (Pasal 43 huruf x & y)",
                "Unit Air Baku/Produksi, SPAL Domestik, TPS, Drainase Primer & Sekunder, Jalur & Tempat Evakuasi Bencana"
            ),
            applicablePasal92And93Rules = listOf(
                "Pasal 92 ayat (9): Perlindungan KSK Sentra Kelautan & PPI Pasar Bantal dengan pemecah gelombang/pengaman pantai (risiko GEA tertinggi).",
                "Pasal 92 ayat (2) & (3): Mitigasi banjir muara & banjir bandang DAS Teramang.",
                "Pasal 64 ayat (12): Pengamanan zona penyangga dan RTH di sekitar Gardu Induk Mukomuko."
            ),
            spatialControlDirective = "Pengembangan KSK Sentra Kelautan & Perikanan Teramang Jaya dan Terminal Khusus CPO wajib dilengkapi bangunan pengaman pantai (seawall/breakwater) dan jalur evakuasi tsunami."
        ),
        RtrwDistrictSpatialProfile(
            districtId = "air_dikit",
            districtName = "Air Dikit",
            settlementHierarchyCode = "PPL",
            settlementHierarchyTitle = "Pusat Pelayanan Lingkungan (PPL) Skala Antardesa",
            urbanOrRuralCenterName = "Kawasan Perdesaan Dusun Baru V Koto",
            article10Reference = "Pasal 10 ayat (6) huruf b",
            strategicAreaStatus = "Bagian dari Kawasan Strategis Kabupaten (KSK) Fungsi & Daya Dukung Lingkungan Hidup Kawasan Perkotaan Mukomuko Adaptif Bencana (Pasal 44 ayat 6 huruf a).",
            protectedZones = listOf(
                "Badan Air & Sempadan Pantai/Sungai (Pasal 31 & Pasal 93)"
            ),
            cultivationZones = listOf(
                "Kawasan Tanaman Pangan, LP2B, Hortikultura, & Perkebunan (Pasal 38)",
                "Kawasan Permukiman Perdesaan (Pasal 41 ayat 3)",
                "Lokasi Pariwisata Pantai Abrasi & Telaga Biru/Danau Biru (Pasal 40 huruf b angka 30 & 49)"
            ),
            vitalInfrastructureAndMitigation = listOf(
                "Pembangkit Listrik Tenaga Panas Bumi (PLTP) & Pembangkit Listrik Tenaga Surya (PLTS) (Pasal 19 ayat 4 & 5a)",
                "Jaringan Transmisi SUTT Tapan–Mukomuko & SUTM (Pasal 19 ayat 8a & 9a)",
                "Pengelolaan Limbah B3, Drainase Primer & Sekunder, Jalur & Tempat Evakuasi Bencana (Pasal 25–28)"
            ),
            applicablePasal92And93Rules = listOf(
                "Pasal 92 ayat (8) & (9): Perlindungan koridor Jalan Arteri Primer & Pantai Abrasi dengan pemecah gelombang dan sabuk hijau pantai.",
                "Pasal 92 ayat (6): Rekayasa konstruksi tahan likuefaksi pada zona aluvial pesisir.",
                "Pasal 64 ayat (6) & (7): Ketentuan jarak aman dan RTH di sekitar instalasi energi terbarukan PLTP & PLTS."
            ),
            spatialControlDirective = "Sebagai bagian dari KSK Perkotaan Mukomuko Adaptif Bencana, koridor pesisir Air Dikit difokuskan pada pengamanan jalan lintas barat dari abrasi dan pengembangan energi bersih (PLTP/PLTS)."
        ),
        RtrwDistrictSpatialProfile(
            districtId = "xiv_koto",
            districtName = "XIV Koto",
            settlementHierarchyCode = "PPL",
            settlementHierarchyTitle = "Pusat Pelayanan Lingkungan (PPL) Skala Antardesa",
            urbanOrRuralCenterName = "Kawasan Perdesaan Lubuk Sanai",
            article10Reference = "Pasal 10 ayat (6) huruf e",
            strategicAreaStatus = "Bagian dari KSK Fungsi & Daya Dukung Lingkungan Hidup Kawasan Perkotaan Mukomuko Adaptif Bencana (Pasal 44 ayat 6 huruf e).",
            protectedZones = listOf(
                "Kawasan Hutan Lindung 4 Ha & Pelestarian Kawasan Bergambut (Pasal 32 ayat 2 & Lampiran XII B.1.2)",
                "Kawasan Cagar Alam 101 Ha (Pasal 33 ayat 3)",
                "Taman Wisata Alam / TWA (Pasal 33 ayat 6 huruf f)",
                "Kawasan Ekosistem Mangrove (Pasal 35 huruf e)"
            ),
            cultivationZones = listOf(
                "Kawasan Permukiman Perkotaan (Pasal 41 ayat 2 huruf h) & Perdesaan",
                "Kawasan Pertanian Pangan Berkelanjutan (KP2B): LP2B & LCP2B (Pasal 38 ayat 5a & 5b)",
                "Hutan Produksi yang dapat Dikonversi / HPK (Pasal 37 ayat 4 huruf c) & Kawasan Perkebunan"
            ),
            vitalInfrastructureAndMitigation = listOf(
                "Jaringan Irigasi Primer, Jaringan Pengendalian Banjir, & Bangunan Pengendalian Banjir (Pasal 21 ayat 4e, 6e, 7d)",
                "Jaringan Drainase Primer & Sekunder, Jaringan Tetap Telekomunikasi, Jalur & Tempat Evakuasi Bencana"
            ),
            applicablePasal92And93Rules = listOf(
                "Pasal 49 ayat (3)a.2.f & Pasal 92 ayat (5): Pelestarian kawasan bergambut untuk menjaga tata air alami & pencegahan Karhutla gambut.",
                "Pasal 77 & Pasal 78: Larangan keras merusak Hutan Lindung (4 Ha) dan Cagar Alam (101 Ha) atau menggunakan alat berat.",
                "Pasal 92 ayat (8) & Pasal 80: Perlindungan ekosistem mangrove dan jalur evakuasi tsunami bagi 4.053 warga pesisir."
            ),
            spatialControlDirective = "Kecamatan dengan fungsi konservasi terlengkap di Kabupaten Mukomuko (Hutan Lindung, Cagar Alam, TWA, Mangrove, Lahan Gambut, dan KP2B): wajib pengendalian ketat alih fungsi lahan dan tata air gambut."
        ),
        RtrwDistrictSpatialProfile(
            districtId = "v_koto",
            districtName = "V Koto",
            settlementHierarchyCode = "PPL",
            settlementHierarchyTitle = "Pusat Pelayanan Lingkungan (PPL) Skala Antardesa",
            urbanOrRuralCenterName = "Kawasan Perdesaan Lalang Luas",
            article10Reference = "Pasal 10 ayat (6) huruf d",
            strategicAreaStatus = "Kawasan strategis hulu sumber daya air utama Kabupaten Mukomuko (lokasi Bendung Air Manjunto — Pasal 21 ayat 8).",
            protectedZones = listOf(
                "Taman Nasional Kerinci Seblat / TNKS (Pasal 33 ayat 5 huruf g)",
                "Badan Air & Sempadan Sungai/Waduk Bendung Air Manjunto (Pasal 31 & Pasal 93 ayat 4)"
            ),
            cultivationZones = listOf(
                "Kawasan Permukiman Perkotaan (Pasal 41 ayat 2 huruf g) & Perdesaan",
                "Kawasan Pertanian Pangan Berkelanjutan (KP2B): LP2B & LCP2B (Pasal 38 ayat 5a & 5b)",
                "Hutan Produksi Terbatas (HPT) & Hutan Produksi Tetap (HP) (Pasal 37 ayat 2i & 3h)",
                "Wisata Bendungan Air Manjunto, Danau Talang Sepakat, Air Terjun Sosang Bukit Landak & Sungai Kuyung (Pasal 40)"
            ),
            vitalInfrastructureAndMitigation = listOf(
                "Bangunan Sumber Daya Air Bendung Air Manjunto (Pasal 21 ayat 8)",
                "Jaringan Irigasi Primer & Jaringan Pengendalian Banjir (Pasal 21 ayat 4d & 6d)",
                "Unit Air Baku & Unit Produksi SPAM, Pengelolaan Limbah B3, Drainase Primer, Jalur & Tempat Evakuasi Bencana"
            ),
            applicablePasal92And93Rules = listOf(
                "Pasal 66 ayat (7) & Pasal 93 ayat (4): Perlindungan sempadan dan fungsi konservasi sumber daya air Bendung Air Manjunto.",
                "Pasal 92 ayat (3) & (7): Mitigasi banjir bandang dan tanah longsor di hulu DAS Manjunto serta larangan tanaman boros air di hulu (Pasal 83 ayat 4c.5)."
            ),
            spatialControlDirective = "Perlindungan mutlak daerah tangkapan air (catchment area) Bendung Air Manjunto dan TNKS untuk menjamin ketahanan irigasi pertanian dan mencegah banjir bandang."
        ),
        RtrwDistrictSpatialProfile(
            districtId = "teras_terunjam",
            districtName = "Teras Terunjam",
            settlementHierarchyCode = "PPL",
            settlementHierarchyTitle = "Pusat Pelayanan Lingkungan (PPL) Skala Antardesa",
            urbanOrRuralCenterName = "Kawasan Perdesaan Teras Terunjam",
            article10Reference = "Pasal 10 ayat (6) huruf i",
            strategicAreaStatus = "Bagian dari KSK Fungsi & Daya Dukung Lingkungan Hidup Kawasan Perkotaan Mukomuko Adaptif Bencana (Pasal 44 ayat 6 huruf d).",
            protectedZones = listOf(
                "Badan Air & Sempadan Sungai (Pasal 31 & Pasal 93)"
            ),
            cultivationZones = listOf(
                "Kawasan Peruntukan Industri / KPI (Pasal 39 huruf h)",
                "Kawasan Hortikultura & Kawasan Perkebunan (Pasal 38 ayat 3f & 4)",
                "Kawasan Permukiman Perdesaan (Pasal 41 ayat 3) & Wisata Air Terjun Sungai Bungin"
            ),
            vitalInfrastructureAndMitigation = listOf(
                "Pembangkit Listrik Tenaga Biogas (Pasal 19 ayat 6 huruf a) & Jaringan SUTT Tapan–Mukomuko",
                "Jalan Kolektor Primer Mukomuko–Tanah Rekah–Sp. IV Setia Budi–Teras Terunjam (Pasal 13 ayat 4b)",
                "Unit Air Baku & Unit Produksi SPAM, Pengelolaan Limbah B3, Drainase Primer, Jalur & Tempat Evakuasi Bencana"
            ),
            applicablePasal92And93Rules = listOf(
                "Pasal 84: Kewajiban zona penyangga (buffer zone) dan pengelolaan limbah terpadu pada Kawasan Peruntukan Industri kelapa sawit.",
                "Pasal 92 ayat (5): Pengendalian bahaya Karhutla dan cuaca ekstrem pada kawasan perkebunan."
            ),
            spatialControlDirective = "Penataan koridor penyangga timur Perkotaan Mukomuko dengan pengawasan ketat IPAL industri pengolahan kelapa sawit (KPI) dan pemanfaatan energi biogas."
        ),
        RtrwDistrictSpatialProfile(
            districtId = "selagan_raya",
            districtName = "Selagan Raya",
            settlementHierarchyCode = "PPL",
            settlementHierarchyTitle = "Pusat Pelayanan Lingkungan (PPL) Skala Antardesa",
            urbanOrRuralCenterName = "Kawasan Perdesaan Sungai Ipuh",
            article10Reference = "Pasal 10 ayat (6) huruf g",
            strategicAreaStatus = "Kawasan penyangga Taman Nasional Kerinci Seblat (TNKS) dan lembah pertanian/industri Sungai Selagan.",
            protectedZones = listOf(
                "Taman Nasional Kerinci Seblat / TNKS (Pasal 33 ayat 5 huruf d)",
                "Badan Air & Sempadan Sungai Selagan (Pasal 31 & Pasal 93)"
            ),
            cultivationZones = listOf(
                "Kawasan Tanaman Pangan, LP2B, Hortikultura, & Perkebunan (Pasal 38)",
                "Kawasan Peruntukan Industri / KPI (Pasal 39 huruf f)",
                "Hutan Produksi Terbatas (HPT), Hutan Produksi Tetap (HP), & Hutan Produksi Konversi (HPK) (Pasal 37)",
                "Wisata Air Terjun Sepunggur, Sungai Enau, Talang Buai, Sungai Aur, & Telun Lubuk Bangko (Pasal 40)"
            ),
            vitalInfrastructureAndMitigation = listOf(
                "Unit Air Baku, Unit Produksi, & Jaringan Air Baku SPAM (Pasal 23 ayat 3f, 4h, 5f)",
                "Rencana Jalan Baru Mukomuko–Selagan Jaya (Lampiran XII A.2.1.1.10)",
                "Pengelolaan Limbah B3, Drainase Primer, Jalur & Tempat Evakuasi Bencana"
            ),
            applicablePasal92And93Rules = listOf(
                "Pasal 92 ayat (3) & (7): Prioritas pemasangan EWS banjir bandang & tanah longsor serta reboisasi sempadan Sungai Selagan.",
                "Pasal 7 ayat (2)d: Pemberdayaan masyarakat sadar risiko bencana (prioritas #1 karena IKM terendah 0,131 di KRB 2026–2030)."
            ),
            spatialControlDirective = "Sinergi pengendalian ruang hulu Sungai Selagan (TNKS & Hutan Produksi) dengan percepatan infrastruktur jalan evakuasi dan pembentukan Desa Tangguh Bencana."
        ),
        RtrwDistrictSpatialProfile(
            districtId = "malin_deman",
            districtName = "Malin Deman",
            settlementHierarchyCode = "PPL",
            settlementHierarchyTitle = "Pusat Pelayanan Lingkungan (PPL) Skala Antardesa",
            urbanOrRuralCenterName = "Kawasan Perdesaan Talang Arah",
            article10Reference = "Pasal 10 ayat (6) huruf h",
            strategicAreaStatus = "Kawasan konservasi hulu Bukit Barisan terluas (TNKS) dan lumbung energi surya/agribisnis selatan.",
            protectedZones = listOf(
                "Taman Nasional Kerinci Seblat / TNKS (Pasal 33 ayat 5 huruf a)",
                "Badan Air & Sempadan Sungai Hulu (Pasal 31 & Pasal 93)"
            ),
            cultivationZones = listOf(
                "Hutan Produksi Terbatas (HPT) & Hutan Produksi Tetap (HP) (Pasal 37 ayat 2c & 3c)",
                "Kawasan Tanaman Pangan, LP2B, & Perkebunan (Pasal 38)",
                "Kawasan Peruntukan Industri / KPI (Pasal 39 huruf c)",
                "Wisata Air Terjun Panas, Air Terjun Sipai, Batu Balago, Batu Punjung, Gelanggang (Pasal 40)"
            ),
            vitalInfrastructureAndMitigation = listOf(
                "Pembangkit Listrik Tenaga Surya / PLTS (Pasal 19 ayat 5 huruf b)",
                "Unit Air Baku, Unit Produksi, Jaringan Air Baku & Jaringan Produksi SPAM (Pasal 23)",
                "Pengelolaan Limbah B3, Drainase Primer, Jalur & Tempat Evakuasi Bencana"
            ),
            applicablePasal92And93Rules = listOf(
                "Pasal 92 ayat (7) & Pasal 94 ayat (1)c.3: Larangan permukiman baru di zona rawan longsor tinggi (58.482 Ha) & larangan galian pada lereng > 40%.",
                "Pasal 92 ayat (3): Mitigasi banjir bandang terluas (1.789,47 Ha) melalui konservasi hulu TNKS & EWS sungai.",
                "Pasal 78 ayat (4): Larangan perambahan dan pertambangan di dalam kawasan Taman Nasional Kerinci Seblat."
            ),
            spatialControlDirective = "Pengendalian ketat alih fungsi hutan hulu (TNKS, HPT, HP) untuk menekan risiko tanah longsor dan banjir bandang yang berdampak ke kecamatan hilir."
        ),
        RtrwDistrictSpatialProfile(
            districtId = "sungai_rumbai",
            districtName = "Sungai Rumbai",
            settlementHierarchyCode = "PPL",
            settlementHierarchyTitle = "Pusat Pelayanan Lingkungan (PPL) Skala Antardesa",
            urbanOrRuralCenterName = "Kawasan Perdesaan Gajah Mati",
            article10Reference = "Pasal 10 ayat (6) huruf c",
            strategicAreaStatus = "Koridor strategis perbatasan selatan Kabupaten Mukomuko – Kabupaten Bengkulu Utara.",
            protectedZones = listOf(
                "Taman Nasional Kerinci Seblat / TNKS (Pasal 33 ayat 5 huruf e)",
                "Taman Wisata Alam / TWA (Pasal 33 ayat 6 huruf e)",
                "Kawasan Ekosistem Mangrove (Pasal 35 huruf d)",
                "Badan Air & Sempadan Pantai/Sungai (Pasal 31 & Pasal 93)"
            ),
            cultivationZones = listOf(
                "Hutan Produksi Terbatas (HPT) & Hutan Produksi Tetap (HP) (Pasal 37 ayat 2g & 3g)",
                "Kawasan Hortikultura & Kawasan Perkebunan (Pasal 38 ayat 3e & 4)",
                "Kawasan Permukiman Perdesaan (Pasal 41 ayat 3)"
            ),
            vitalInfrastructureAndMitigation = listOf(
                "Jalan Arteri Primer Ruas Ipuh–Seblat & Jaringan SUTT Mukomuko–Arga Makmur (Pasal 13 & Pasal 19)",
                "Jaringan Air Baku SPAM, Jaringan Tetap Telekomunikasi, Drainase Primer & Sekunder",
                "Jalur & Tempat Evakuasi Bencana (Pasal 27)"
            ),
            applicablePasal92And93Rules = listOf(
                "Pasal 92 ayat (7) & (3): Mitigasi lereng rawan longsor (21.496 Ha) dan koridor banjir bandang (715,68 Ha).",
                "Pasal 92 ayat (8) & Pasal 80: Pelestarian ekosistem mangrove dan sempadan pantai zona bahaya tsunami."
            ),
            spatialControlDirective = "Penataan ruang terpadu dari pesisir mangrove hingga perbukitan TNKS di perbatasan Bengkulu Utara dengan penguatan drainase primer-sekunder dan jalur evakuasi."
        ),
        RtrwDistrictSpatialProfile(
            districtId = "air_rami",
            districtName = "Air Rami",
            settlementHierarchyCode = "PPL",
            settlementHierarchyTitle = "Pusat Pelayanan Lingkungan (PPL) Skala Antardesa",
            urbanOrRuralCenterName = "Kawasan Perdesaan Arga Jaya",
            article10Reference = "Pasal 10 ayat (6) huruf a",
            strategicAreaStatus = "Pusat pelayanan perdesaan agribisnis dan pesisir selatan.",
            protectedZones = listOf(
                "Taman Wisata Alam / TWA (Pasal 33 ayat 6 huruf a)",
                "Kawasan Ekosistem Mangrove (Pasal 35 huruf a)",
                "Badan Air & Sempadan Pantai/Sungai (Pasal 31 & Pasal 93)"
            ),
            cultivationZones = listOf(
                "Kawasan Tanaman Pangan, LP2B, & Perkebunan (Pasal 38 ayat 2c, 4, 5a)",
                "Kawasan Permukiman Perdesaan (Pasal 41 ayat 3)",
                "Lokasi Pariwisata Pantai Agam, Pantai Air Rami, & Wahana Air Arga Jaya (Pasal 40)"
            ),
            vitalInfrastructureAndMitigation = listOf(
                "Unit Air Baku, Unit Produksi, Jaringan Air Baku & Jaringan Produksi SPAM (Pasal 23)",
                "Jalan Kolektor Primer Simp. PUSTU Air Rami–Bukit Mulya–Rami Mulya–Talang Rio–Arga Jaya (Pasal 13 ayat 4m)",
                "Jaringan SUTT Mukomuko–Arga Makmur, Pengelolaan Limbah B3, Drainase Primer & Sekunder, Jalur Evakuasi Bencana"
            ),
            applicablePasal92And93Rules = listOf(
                "Pasal 92 ayat (8) & (9): Perlindungan sempadan Pantai Air Rami & ekosistem mangrove dari tsunami dan abrasi.",
                "Pasal 92 ayat (7): Stabilisasi lereng dan pengendalian permukiman pada zona rawan tanah longsor perbukitan."
            ),
            spatialControlDirective = "Penguatan konektivitas jalan kolektor Arga Jaya sebagai koridor distribusi ekonomi sekaligus jalur evakuasi tsunami dan longsor."
        ),
        RtrwDistrictSpatialProfile(
            districtId = "air_manjunto",
            districtName = "Air Manjunto",
            settlementHierarchyCode = "PPL",
            settlementHierarchyTitle = "Pusat Pelayanan Lingkungan (PPL) Skala Antardesa",
            urbanOrRuralCenterName = "Kawasan Perdesaan Manjunto Jaya",
            article10Reference = "Pasal 10 ayat (6) huruf f",
            strategicAreaStatus = "Kawasan Strategis Kabupaten (KSK) Agropolitan Lubuk Pinang – Air Manjunto DAN KSK Kawasan Perkotaan Mukomuko Adaptif Bencana (Pasal 44 ayat 5a & 6b).",
            protectedZones = listOf(
                "Badan Air & Sempadan Sungai/Saluran Irigasi Primer Manjunto (Pasal 31 & Pasal 93)"
            ),
            cultivationZones = listOf(
                "Kawasan Pertanian Pangan Berkelanjutan (KP2B): LP2B & LCP2B Sentra Pangan (Pasal 38 ayat 5a & 5b)",
                "Kawasan Tanaman Pangan & Perkebunan (Pasal 38)",
                "Kawasan Permukiman Perdesaan (Pasal 41 ayat 3)"
            ),
            vitalInfrastructureAndMitigation = listOf(
                "Jaringan Irigasi Primer & Jaringan Pengendalian Banjir (Pasal 21 ayat 4a & 6a)",
                "Tempat Pemrosesan Akhir (TPA) Sampah Regional/Kabupaten (Pasal 26 ayat 3 huruf a)",
                "Jaringan SUTT Tapan–Mukomuko & SUTM, Jalur & Tempat Evakuasi Bencana (Pasal 19 & Pasal 27)"
            ),
            applicablePasal92And93Rules = listOf(
                "Pasal 91 ayat (3): Perlindungan ketat lahan sawah beririgasi teknis (LP2B & LCP2B) dari konversi non-pertanian.",
                "Pasal 66 ayat (3) & (5): Pengamanan Jaringan Irigasi Primer dan Jaringan Pengendalian Banjir.",
                "Pasal 71 ayat (3): Pengelolaan TPA wajib menerapkan sanitary landfill, instalasi pengumpul lindi, studi AMDAL, dan jarak aman permukiman."
            ),
            spatialControlDirective = "Memegang peran ganda sebagai KSK Agropolitan dan KSK Perkotaan Adaptif Bencana: fokus pada modernisasi jaringan irigasi primer, pengendalian banjir, dan perlindungan lahan pangan."
        )
    )

    fun getDistrictSpatialProfile(district: District): RtrwDistrictSpatialProfile {
        val cleanId = district.id.trim()
        val cleanName = district.name.trim()
        return districtSpatialProfiles.firstOrNull {
            it.districtId.equals(cleanId, ignoreCase = true) ||
                it.districtName.equals(cleanName, ignoreCase = true)
        } ?: districtSpatialProfiles.first()
    }
}
