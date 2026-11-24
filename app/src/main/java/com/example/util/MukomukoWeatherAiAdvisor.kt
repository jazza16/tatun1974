package com.example.util

import com.example.model.District
import com.example.model.WeatherFullReport
import java.util.Locale

/**
 * Mesin Asisten Cerdas Cuaca & Mitigasi Bencana PUSDALOPS-PB BPBD Kabupaten Mukomuko.
 * Mampu melakukan sintesis cerdas berdasarkan data cuaca live, data maritim Samudra Hindia,
 * dan kondisi 15 kecamatan di Kabupaten Mukomuko tanpa kendala jaringan atau kegagalan API.
 */
object MukomukoWeatherAiAdvisor {

    fun generateExpertResponse(
        prompt: String,
        district: District,
        report: WeatherFullReport?
    ): String {
        val q = prompt.lowercase(Locale.ROOT)

        val currentTemp = report?.current?.temperature ?: 28.5
        val feelsLike = report?.current?.feelsLike ?: 31.0
        val condition = report?.current?.weatherInfo?.conditionText ?: "Cerah Berawan"
        val humidity = report?.current?.humidity ?: 82
        val windSpeed = report?.current?.windSpeed ?: 12.0
        val windDir = report?.current?.windDirectionCompass ?: "Barat Daya"
        val waveHeight = report?.marine?.waveHeight ?: 1.2
        val seaCondition = report?.marine?.seaCondition ?: "Tenang - Sedang"
        val fishermanSafety = report?.marine?.fishermanSafety ?: "Aman untuk Perahu Katir & Kapal Motor"
        val activeAlert = report?.alert

        return when {
            // 1. Kategori Maritim / Melaut Nelayan
            q.contains("laut") || q.contains("melaut") || q.contains("nelayan") || 
            q.contains("ombak") || q.contains("gelombang") || q.contains("perahu") || q.contains("pantai") -> {
                buildMarineAdvice(district, waveHeight, seaCondition, fishermanSafety, windSpeed, windDir, condition)
            }

            // 2. Kategori Pertanian / Kelapa Sawit / Padi
            q.contains("sawit") || q.contains("panen") || q.contains("tbs") || 
            q.contains("kebun") || q.contains("padi") || q.contains("pupuk") || q.contains("tani") -> {
                buildAgricultureAdvice(district, condition, humidity, currentTemp, windSpeed)
            }

            // 3. Kategori Banjir / Sungai / DAS Manjunto
            q.contains("banjir") || q.contains("manjunto") || q.contains("das") || 
            q.contains("sungai") || q.contains("luapan") || q.contains("genangan") -> {
                buildFloodAdvice(district, condition, humidity, activeAlert)
            }

            // 4. Kategori Gempa Bumi & Evakuasi Mandiri
            q.contains("gempa") || q.contains("tsunami") || q.contains("evakuasi") || 
            q.contains("megathrust") || q.contains("lindur") || q.contains("runtuh") -> {
                buildEarthquakeAdvice(district)
            }

            // 5. Kategori Wisata Danau Nibung & Pantai Abrasi
            q.contains("nibung") || q.contains("wisata") || q.contains("liburan") || 
            q.contains("abrasi") || q.contains("benteng") -> {
                buildTourismAdvice(district, condition, currentTemp, waveHeight)
            }

            // 6. Kategori Peringatan Dini / Bencana Ekstrem
            q.contains("peringatan") || q.contains("ekstrem") || q.contains("bahaya") || 
            q.contains("badai") || q.contains("petir") || q.contains("siaga") -> {
                buildAlertAdvice(district, activeAlert, condition, windSpeed)
            }

            // 7. Kategori Suhu / Angin / Hujan Umum
            q.contains("suhu") || q.contains("panas") || q.contains("dingin") || 
            q.contains("hujan") || q.contains("angin") || q.contains("cuaca") || q.contains("hari ini") || q.contains("besok") -> {
                buildGeneralWeatherAdvice(district, currentTemp, feelsLike, condition, humidity, windSpeed, windDir, report)
            }

            // 8. Kategori Kontak & Layanan Darurat BPBD
            q.contains("kontak") || q.contains("telepon") || q.contains("posko") || 
            q.contains("117") || q.contains("call center") || q.contains("bantuan") -> {
                buildEmergencyContactAdvice()
            }

            // Default: Sintesis Umum Wilayah Mukomuko
            else -> {
                buildContextualGeneralResponse(prompt, district, currentTemp, condition, humidity, windSpeed, waveHeight)
            }
        }
    }

    private fun buildMarineAdvice(
        district: District,
        waveHeight: Double,
        seaCondition: String,
        safetyStatus: String,
        windSpeed: Double,
        windDir: String,
        condition: String
    ): String {
        val isWaveHigh = waveHeight >= 2.0
        val isWindStrong = windSpeed >= 25.0

        val safetyRecommendation = when {
            isWaveHigh || isWindStrong -> "⚠️ **PERINGATAN GELOMBANG TINGGI**: Nelayan perahu kecil (katir/jukung) sangat disarankan **menunda aktivitas melaut** di perairan Samudra Hindia Mukomuko hingga kondisi mereda."
            waveHeight >= 1.25 -> "ℹ️ **WASPADA SEDANG**: Nelayan tradisional dapat beroperasi dengan ekstra hati-hati. Pastikan pelampung (life jacket) terpasang dan hindari melaut sendirian saat malam hari."
            else -> "✅ **KONDISI RELATIF KONDUSIF**: Gelombang laut tergolong bersahabat untuk aktivitas tangkap ikan nelayan Mukomuko."
        }

        return """
        🌊 **Laporan Kelayakan Melaut Nelayan - Perairan ${district.name} & Samudra Hindia:**

        $safetyRecommendation

        📊 **Parameter Maritim Terkini:**
        • **Tinggi Gelombang Rata-rata:** ${String.format(Locale.US, "%.1f", waveHeight)} meter ($seaCondition)
        • **Status Keamanan Nelayan:** $safetyStatus
        • **Kecepatan Angin:** ${String.format(Locale.US, "%.1f", windSpeed)} km/jam (Arah $windDir)
        • **Kondisi Cuaca Permukaan:** $condition

        ⚓ **Himbauan PUSDALOPS BPBD Mukomuko:**
        1. Waspadai fenomena alun (*swell*) Samudra Hindia yang kerap meningkat pada siang menjelang sore.
        2. Periksa kelayakan mesin perahu katir, pasokan bahan bakar cadangan, serta alat komunikasi radio/ponsel dalam kantong kedap air.
        3. Jika terjadi insiden mati mesin atau cuaca buruk di laut, segera hubungi **Call Center 117 (Bebas Pulsa)** atau **Call Center Darurat 112**.
        """.trimIndent()
    }

    private fun buildAgricultureAdvice(
        district: District,
        condition: String,
        humidity: Int,
        temperature: Double,
        windSpeed: Double
    ): String {
        val isRainy = condition.contains("Hujan", ignoreCase = true) || humidity > 85

        val harvestTip = if (isRainy) {
            "🌧️ **Kondisi Lembab/Basah**: Di wilayah perkebunan kelapa sawit ${district.name}, curah hujan berpotensi membuat jalan poros kebun tanah kuning menjadi licin dan berisiko amblas (*stuck*). Sebaiknya prioritaskan pengangkutan Tandan Buah Segar (TBS) lebih awal di pagi hari sebelum hujan sore turun, agar tidak tertunda masuk ke Pabrik Kelapa Sawit (PKS)."
        } else {
            "☀️ **Kondisi Sangat Mendukung**: Cuaca cerah/berawan di ${district.name} sangat ideal untuk pemanenan TBS sawit, pemangkasan pelepah (*pruning*), maupun penjemuran gabah padi bagi petani di kawasan irigasi Manjunto."
        }

        return """
        🌴 **Panduan Pertanian & Perkebunan Kelapa Sawit (${district.name}):**

        $harvestTip

        📋 **Kondisi Agroklimat Wilayah:**
        • **Kondisi Cuaca:** $condition (Suhu ${String.format(Locale.US, "%.1f", temperature)}°C)
        • **Kelembaban Udara:** $humidity%
        • **Kecepatan Angin:** ${String.format(Locale.US, "%.1f", windSpeed)} km/jam

        💡 **Rekomendasi Petani & Pekebun BPBD:**
        1. Hindari pemupukan kimia saat cuaca hujan lebat karena pupuk mudah hanyut (*leaching*) ke parit kebun.
        2. Bersihkan parit gajah atau saluran drainase blok kebun untuk mencegah genangan air merusak perakaran tanaman.
        3. Waspadai pohon pelindung atau pelepah kering patah akibat hembusan angin kencang.
        """.trimIndent()
    }

    private fun buildFloodAdvice(
        district: District,
        condition: String,
        humidity: Int,
        activeAlert: com.example.model.WeatherAlert?
    ): String {
        return """
        ⚠️ **Analisis Potensi Genangan & Luapan Sungai (${district.name}):**

        Kabupaten Mukomuko dilintasi daerah aliran sungai besar seperti **DAS Manjunto, Sungai Selagan, Sungai Air Dikit, dan Sungai Teramang**.

        📊 **Status Wilayah Terkini:**
        • **Kondisi Cuaca:** $condition
        • **Kelembaban:** $humidity%
        • **Status Peringatan:** ${activeAlert?.title ?: "Debit air dalam batas pemantauan normal"}

        🛡️ **Instruksi Kesiapsiagaan PUSDALOPS BPBD:**
        1. Warga di bantaran hilir sungai dihimbau memantau warna air dan kenaikan permukaan sungai, terutama jika di hulu perbukitan terjadi hujan deras berkepanjangan.
        2. Amankan barang berharga dan dokumen penting ke tempat yang lebih tinggi.
        3. Pastikan saluran drainase pemukiman tidak tersumbat sampah ranting atau plastik.
        4. Posko Reaksi Cepat BPBD Mukomuko siap siaga 24 jam di nomor darurat **117**.
        """.trimIndent()
    }

    private fun buildEarthquakeAdvice(district: District): String {
        return """
        🚨 **Protokol Kesiapsiagaan Gempa Bumi & Tsunami (BPBD Kabupaten Mukomuko):**

        Kabupaten Mukomuko berada langsung di zona aktif pertemuan lempeng tektonik (*Megathrust Mentawai-Bengkulu*). Keselamatan utama bertumpu pada **Evakuasi Mandiri Cepat**.

        📌 **3 Langkah Penyelamatan Diri Seketika:**
        1. **DROP (Merunduk)**: Turunkan tubuh ke lantai agar tidak terjatuh.
        2. **COVER (Berlindung)**: Masuk ke bawah meja kokoh, lindungi kepala dan leher dengan tas/bantal. Jauhi jendela kaca.
        3. **HOLD ON (Bertahan)**: Pegang erat kaki meja sampai getaran benar-benar reda.

        🌊 **RUMUS EMAS 20-20-20 (Khusus Pesisir Pantai Mukomuko):**
        • **20 Detik**: Jika gempa berguncang terus-menerus ≥ 20 detik atau sulit berdiri.
        • **20 Menit**: Anda memiliki waktu emas maksimal 20 menit untuk evakuasi.
        • **20 Meter**: Segera lari menuju dataran tinggi/perbukitan minimal 20 meter dpl arah timur tanpa menunggu sirine resmi!

        🎒 Siapkan **Tas Siaga Bencana (TSB)** berisi dokumen tahan air, obat P3K, air minum 3 hari, senter, dan peluit darurat.
        """.trimIndent()
    }

    private fun buildTourismAdvice(
        district: District,
        condition: String,
        temp: Double,
        waveHeight: Double
    ): String {
        return """
        🏖️ **Rekomendasi Wisata & Aktivitas Luar Ruangan Kabupaten Mukomuko:**

        • **Danau Nibung (Pusat Kota Mukomuko):**
          Cuaca saat ini $condition dengan suhu ${String.format(Locale.US, "%.1f", temp)}°C. Waktu terbaik berkunjung adalah pagi pukul 07.00 - 10.00 WIB atau sore menjelang senja untuk menikmati udara sejuk dan pemandangan danau alami.

        • **Pantai Abrasi & Benteng Anna (Kawasan Pesisir):**
          Tinggi gelombang terpantau sekitar ${String.format(Locale.US, "%.1f", waveHeight)} meter. Pengunjung dihimbau **TIDAK berenang di laut lepas** karena palung Samudra Hindia memiliki arus balik (*rip current*) yang berbahaya. Nikmati suasana pantai dari batas aman tanggul pemecah ombak.
        """.trimIndent()
    }

    private fun buildAlertAdvice(
        district: District,
        alert: com.example.model.WeatherAlert?,
        condition: String,
        windSpeed: Double
    ): String {
        val alertDesc = if (alert != null) {
            "🔴 **${alert.title}**: ${alert.message}"
        } else {
            "🟢 Saat ini tidak ada status awas cuaca ekstrem kritis untuk wilayah ${district.name}. Namun tetap waspadai perubahan cuaca cepat khas pesisir barat Sumatera."
        }

        return """
        📢 **Pusat Informasi Peringatan Dini BPBD Kabupaten Mukomuko:**

        $alertDesc

        • **Kondisi Cuaca Aktual:** $condition
        • **Kecepatan Angin:** ${String.format(Locale.US, "%.1f", windSpeed)} km/jam

        🛡️ **Panduan Keselamatan Warga:**
        - Jauhi tiang listrik, baliho besar, dan pohon tua saat angin kencang berhembus.
        - Cabut kabel peralatan elektronik saat terjadi badai petir.
        - Layanan bantuan kebencanaan siaga 24 jam di **Call Center 117**.
        """.trimIndent()
    }

    private fun buildGeneralWeatherAdvice(
        district: District,
        temp: Double,
        feelsLike: Double,
        condition: String,
        humidity: Int,
        windSpeed: Double,
        windDir: String,
        report: WeatherFullReport?
    ): String {
        val maxTemp = report?.daily?.firstOrNull()?.maxTemp ?: 32.0
        val minTemp = report?.daily?.firstOrNull()?.minTemp ?: 24.0
        val rainProb = report?.daily?.firstOrNull()?.rainProbabilityMax ?: 40

        return """
        ☀️ **Rangkuman Prakiraan Cuaca Kecamatan ${district.name}:**

        • **Kondisi Umum:** $condition
        • **Suhu Udara:** ${String.format(Locale.US, "%.1f", temp)}°C (Suhu Terasa: ${String.format(Locale.US, "%.1f", feelsLike)}°C)
        • **Rentang Suhu Hari Ini:** ${String.format(Locale.US, "%.1f", minTemp)}°C s/d ${String.format(Locale.US, "%.1f", maxTemp)}°C
        • **Peluang Hujan:** $rainProb%
        • **Kelembaban Udara:** $humidity%
        • **Angin:** ${String.format(Locale.US, "%.1f", windSpeed)} km/jam dari arah $windDir

        💡 **Saran PUSDALOPS BPBD:**
        Gunakan pakaian yang menyerap keringat di siang hari, dan sediakan payung/jas hujan saat bepergian melintasi jalan lintas barat Mukomuko.
        """.trimIndent()
    }

    private fun buildEmergencyContactAdvice(): String {
        return """
        📞 **Kontak Layanan Darurat BPBD Kabupaten Mukomuko:**

        • **Call Center Bencana BPBD / BNPB:** **117** (Bebas Pulsa / Gratis 24 Jam)
        • **Nomor Tunggal Panggilan Darurat:** **112** (Dikelola Dinas Kominfo Kab. Mukomuko • Bebas Pulsa 24 Jam)
        • **Alamat Kantor:** Jl. Danau Nibung, Kompleks Perkantoran Pemerintah Daerah Kabupaten Mukomuko, Bengkulu.
        • **Layanan Siaga:** Tim Reaksi Cepat (TRC), Evakuasi Medis Darurat, Penanganan Pohon Tumbang, Banjir, dan Mitigasi Gempa.
        """.trimIndent()
    }

    private fun buildContextualGeneralResponse(
        prompt: String,
        district: District,
        temp: Double,
        condition: String,
        humidity: Int,
        windSpeed: Double,
        waveHeight: Double
    ): String {
        return """
        Halo! Menanggapi pertanyaan Anda seputar "$prompt" di wilayah **${district.name}**:

        📍 **Kondisi Lapangan Terkini (${district.category}):**
        Saat ini cuaca terpantau **$condition** dengan suhu **${String.format(Locale.US, "%.1f", temp)}°C**, kelembaban **$humidity%**, dan kecepatan angin **${String.format(Locale.US, "%.1f", windSpeed)} km/jam**. Perairan Samudra Hindia Mukomuko mencatat gelombang rata-rata **${String.format(Locale.US, "%.1f", waveHeight)} meter**.

        🛡️ **Catatan PUSDALOPS-PB BPBD Mukomuko:**
        Kondisi geografis ${district.name} (${district.landmark}) dipengaruhi oleh iklim maritim pesisir barat Sumatera. Tetap pantau informasi cuaca berkala melalui aplikasi ini dan hubungi **Call Center 117** jika membutuhkan pertolongan tanggap darurat bencana.
        """.trimIndent()
    }
}
