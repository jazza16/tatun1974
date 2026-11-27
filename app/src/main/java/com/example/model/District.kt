package com.example.model

import androidx.annotation.Keep

@Keep
data class District(
    val id: String,
    val name: String,
    val category: String, // "Ibukota", "Pesisir", "Perkebunan", "Lembah"
    val latitude: Double,
    val longitude: Double,
    val description: String,
    val landmark: String,
    val isCoastalTsunamiZone: Boolean = false
)

object MukomukoDistricts {
    val list = listOf(
        District(
            id = "kota_mukomuko",
            name = "Kota Mukomuko",
            category = "Ibukota & Pesisir",
            latitude = -2.5833,
            longitude = 101.1167,
            description = "Pusat pemerintahan dan pelabuhan, berbatasan langsung dengan Samudra Hindia.",
            landmark = "Pantai Abrasi & Benteng Anna",
            isCoastalTsunamiZone = true
        ),
        District(
            id = "ipuh",
            name = "Ipuh",
            category = "Pesisir Selatan",
            latitude = -3.0033,
            longitude = 101.4988,
            description = "Pusat perikanan dan perdagangan utama bagian selatan Mukomuko.",
            landmark = "Pantai Muara Sebakul & Pasar Ipuh",
            isCoastalTsunamiZone = true
        ),
        District(
            id = "penarik",
            name = "Penarik",
            category = "Perkebunan & Sentra Tani",
            latitude = -2.6950,
            longitude = 101.2950,
            description = "Wilayah perkebunan kelapa sawit dan pertanian hortikultura.",
            landmark = "Kawasan Hijau Penarik",
            isCoastalTsunamiZone = false
        ),
        District(
            id = "lubuk_pinang",
            name = "Lubuk Pinang",
            category = "Pertanian & Perbatasan",
            latitude = -2.4820,
            longitude = 101.1090,
            description = "Lumbung padi Mukomuko, dekat perbatasan Pesisir Selatan (Sumbar).",
            landmark = "Sentra Persawahan Irigasi",
            isCoastalTsunamiZone = false
        ),
        District(
            id = "pondok_suguh",
            name = "Pondok Suguh",
            category = "Pesisir & Sungai",
            latitude = -2.8580,
            longitude = 101.4010,
            description = "Muara sungai subur dengan aktivitas perikanan air tawar dan laut.",
            landmark = "Muara Sungai Teramang",
            isCoastalTsunamiZone = true
        ),
        District(
            id = "teramang_jaya",
            name = "Teramang Jaya",
            category = "Pesisir & Nelayan",
            latitude = -2.7680,
            longitude = 101.3520,
            description = "Desa-desa nelayan dengan hembusan angin laut pesisir barat.",
            landmark = "Pesisir Nelayan Bantal",
            isCoastalTsunamiZone = true
        ),
        District(
            id = "air_dikit",
            name = "Air Dikit",
            category = "Pesisir",
            latitude = -2.6350,
            longitude = 101.1780,
            description = "Kawasan jalur lintas barat Sumatera yang berdampingan dengan laut.",
            landmark = "Pantai Air Dikit",
            isCoastalTsunamiZone = true
        ),
        District(
            id = "xiv_koto",
            name = "XIV Koto",
            category = "Pesisir, Danau & Gambut",
            latitude = -2.5400,
            longitude = 101.1350,
            description = "Kawasan pesisir, danau alami Danau Nibung, dan lahan gambut Rawa Mulya.",
            landmark = "Danau Nibung & Pesisir XIV Koto",
            isCoastalTsunamiZone = true
        ),
        District(
            id = "v_koto",
            name = "V Koto",
            category = "Perbukitan & Hulu",
            latitude = -2.4950,
            longitude = 101.1890,
            description = "Kaki bukit barisan dengan curah hujan yang cenderung lebih tinggi.",
            landmark = "Hulu Air Manjunto",
            isCoastalTsunamiZone = false
        ),
        District(
            id = "teras_terunjam",
            name = "Teras Terunjam",
            category = "Perkebunan",
            latitude = -2.6450,
            longitude = 101.2400,
            description = "Kawasan agraris penghasil karet dan kelapa sawit rakyat.",
            landmark = "Jalur Perkebunan Teras Terunjam",
            isCoastalTsunamiZone = false
        ),
        District(
            id = "selagan_raya",
            name = "Selagan Raya",
            category = "Lembah & Pertanian",
            latitude = -2.6010,
            longitude = 101.2750,
            description = "Lembah subur diapit aliran Sungai Selagan.",
            landmark = "Lembah Sungai Selagan",
            isCoastalTsunamiZone = false
        ),
        District(
            id = "malin_deman",
            name = "Malin Deman",
            category = "Dataran Tinggi Selatan",
            latitude = -3.0780,
            longitude = 101.5890,
            description = "Daerah perbukitan dengan kebun kopi dan sawit di selatan.",
            landmark = "Perbukitan Malin Deman",
            isCoastalTsunamiZone = false
        ),
        District(
            id = "sungai_rumbai",
            name = "Sungai Rumbai",
            category = "Pesisir & Perbatasan",
            latitude = -3.1450,
            longitude = 101.6620,
            description = "Pesisir ujung selatan Kabupaten Mukomuko berbatasan dengan Bengkulu Utara.",
            landmark = "Muara Sungai Rumbai",
            isCoastalTsunamiZone = true
        ),
        District(
            id = "air_rami",
            name = "Air Rami",
            category = "Pesisir Selatan",
            latitude = -3.2080,
            longitude = 101.7300,
            description = "Wilayah pantai berpasir putih luas dan pusat nelayan pancing.",
            landmark = "Pantai Air Rami",
            isCoastalTsunamiZone = true
        ),
        District(
            id = "air_manjunto",
            name = "Air Manjunto",
            category = "Irigasi & Bendungan",
            latitude = -2.5200,
            longitude = 101.1550,
            description = "Ikon pengairan Mukomuko dengan bendungan teknis terbesar di Bengkulu.",
            landmark = "Bendungan Air Manjunto",
            isCoastalTsunamiZone = false
        )
    )

    val defaultDistrict = list.first()

    val coastalTsunamiDistricts: List<District>
        get() = list.filter { it.isCoastalTsunamiZone }

    fun isCoastalTsunamiZone(districtNameOrId: String?): Boolean {
        val q = districtNameOrId?.trim().orEmpty()
        if (q.isBlank()) return false
        return list.any {
            it.isCoastalTsunamiZone &&
                (it.id.equals(q, ignoreCase = true) || it.name.equals(q, ignoreCase = true))
        }
    }
}
