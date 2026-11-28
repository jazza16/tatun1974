package com.example.model

import androidx.annotation.Keep
import com.google.firebase.firestore.IgnoreExtraProperties
import org.json.JSONArray
import org.json.JSONObject

/**
 * Satu langkah ringkas dalam Panduan Penggunaan Aplikasi SIPAGAR KITA.
 * Dapat diperbarui dinamis dari Cloud Firestore (`app_settings/usage_guide`)
 * tanpa membongkar/menginstal ulang APK di perangkat pengguna.
 */
@Keep
@IgnoreExtraProperties
data class UsageGuideStep(
    val stepNumber: Int = 1,
    val title: String = "",
    val description: String = "",
    val moduleTag: String = "Umum"
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("stepNumber", stepNumber)
        put("title", title)
        put("description", description)
        put("moduleTag", moduleTag)
    }

    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "stepNumber" to stepNumber,
        "title" to title.trim().take(120),
        "description" to description.trim().take(600),
        "moduleTag" to moduleTag.trim().ifBlank { "Umum" }.take(60)
    )

    companion object {
        fun fromJsonObject(obj: JSONObject, fallbackIndex: Int): UsageGuideStep? {
            val title = obj.optString("title", "").trim()
            val description = obj.optString("description", "").trim()
            if (title.isBlank() || description.isBlank()) return null
            val stepNumber = obj.optInt("stepNumber", fallbackIndex).coerceIn(1, 20)
            val moduleTag = obj.optString("moduleTag", "Umum").trim().ifBlank { "Umum" }
            return UsageGuideStep(
                stepNumber = stepNumber,
                title = title,
                description = description,
                moduleTag = moduleTag
            )
        }

        fun fromFirestoreMap(map: Map<*, *>, fallbackIndex: Int): UsageGuideStep? {
            val title = (map["title"] as? String)?.trim().orEmpty()
            val description = (map["description"] as? String)?.trim().orEmpty()
            if (title.isBlank() || description.isBlank()) return null
            val stepNumber = ((map["stepNumber"] as? Number)?.toInt() ?: fallbackIndex).coerceIn(1, 20)
            val moduleTag = (map["moduleTag"] as? String)?.trim().orEmpty().ifBlank { "Umum" }
            return UsageGuideStep(
                stepNumber = stepNumber,
                title = title,
                description = description,
                moduleTag = moduleTag
            )
        }
    }
}

/**
 * Konten lengkap Panduan Penggunaan SIPAGAR KITA untuk Warga dan Petugas.
 */
@Keep
@IgnoreExtraProperties
data class AppUsageGuide(
    val guideVersion: String = DEFAULT_GUIDE_VERSION,
    val releaseTitle: String = "Panduan Penggunaan SIPAGAR KITA",
    val updatedAtLabel: String = "27 November 2026",
    val updatedBy: String = "Pusdalops BPBD Kab. Mukomuko",
    val updatedAt: Long = 0L,
    val citizenSummary: String = DEFAULT_CITIZEN_SUMMARY,
    val officerSummary: String = DEFAULT_OFFICER_SUMMARY,
    val citizenSteps: List<UsageGuideStep> = DefaultAppUsageGuideCatalog.citizenSteps,
    val officerSteps: List<UsageGuideStep> = DefaultAppUsageGuideCatalog.officerSteps,
    val source: String = SOURCE_BUNDLED
) {
    fun toJsonString(): String {
        val root = JSONObject().apply {
            put("guideVersion", guideVersion)
            put("releaseTitle", releaseTitle)
            put("updatedAtLabel", updatedAtLabel)
            put("updatedBy", updatedBy)
            put("updatedAt", updatedAt)
            put("citizenSummary", citizenSummary)
            put("officerSummary", officerSummary)
            put("citizenSteps", JSONArray().apply {
                citizenSteps.forEach { put(it.toJsonObject()) }
            })
            put("officerSteps", JSONArray().apply {
                officerSteps.forEach { put(it.toJsonObject()) }
            })
        }
        return root.toString()
    }

    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "guideVersion" to guideVersion.trim().ifBlank { DEFAULT_GUIDE_VERSION }.take(64),
        "releaseTitle" to releaseTitle.trim().ifBlank { "Panduan Penggunaan SIPAGAR KITA" }.take(140),
        "updatedAtLabel" to updatedAtLabel.trim().ifBlank { "Terbaru" }.take(80),
        "updatedBy" to updatedBy.trim().ifBlank { "Admin BPBD Mukomuko" }.take(120),
        "updatedAt" to updatedAt.coerceAtLeast(0L),
        "citizenSummary" to citizenSummary.trim().ifBlank { DEFAULT_CITIZEN_SUMMARY }.take(500),
        "officerSummary" to officerSummary.trim().ifBlank { DEFAULT_OFFICER_SUMMARY }.take(500),
        "citizenSteps" to citizenSteps.take(12).mapIndexed { index, step ->
            step.copy(stepNumber = index + 1).toFirestoreMap()
        },
        "officerSteps" to officerSteps.take(12).mapIndexed { index, step ->
            step.copy(stepNumber = index + 1).toFirestoreMap()
        }
    )

    companion object {
        const val SOURCE_BUNDLED = "BUNDLED"
        const val SOURCE_CACHE = "CACHE"
        const val SOURCE_FIRESTORE = "FIRESTORE"

        const val DEFAULT_GUIDE_VERSION = "v1.3.0-live"
        const val DEFAULT_CITIZEN_SUMMARY =
            "Akses publik tanpa login untuk memantau cuaca 15 kecamatan, peringatan dini bencana, kirim laporan warga ber-GPS, serta kompas & SOP evakuasi offline."
        const val DEFAULT_OFFICER_SUMMARY =
            "Panduan khusus personil Pusdalops, TRC, Posko, & Admin untuk verifikasi laporan warga, pelaporan harian PDF resmi, logistik/relawan, dan kontrol kebijakan KRB–RPB–RTRW."

        fun fromJson(json: String, source: String = SOURCE_CACHE): AppUsageGuide? {
            if (json.isBlank()) return null
            return try {
                val obj = JSONObject(json)
                val citizenArray = obj.optJSONArray("citizenSteps")
                val officerArray = obj.optJSONArray("officerSteps")

                val parsedCitizen = buildList {
                    if (citizenArray != null) {
                        for (i in 0 until citizenArray.length()) {
                            val item = citizenArray.optJSONObject(i) ?: continue
                            UsageGuideStep.fromJsonObject(item, i + 1)?.let { add(it) }
                        }
                    }
                }.ifEmpty { DefaultAppUsageGuideCatalog.citizenSteps }

                val parsedOfficer = buildList {
                    if (officerArray != null) {
                        for (i in 0 until officerArray.length()) {
                            val item = officerArray.optJSONObject(i) ?: continue
                            UsageGuideStep.fromJsonObject(item, i + 1)?.let { add(it) }
                        }
                    }
                }.ifEmpty { DefaultAppUsageGuideCatalog.officerSteps }

                AppUsageGuide(
                    guideVersion = obj.optString("guideVersion", DEFAULT_GUIDE_VERSION).ifBlank { DEFAULT_GUIDE_VERSION },
                    releaseTitle = obj.optString("releaseTitle", "Panduan Penggunaan SIPAGAR KITA").ifBlank { "Panduan Penggunaan SIPAGAR KITA" },
                    updatedAtLabel = obj.optString("updatedAtLabel", "27 November 2026").ifBlank { "27 November 2026" },
                    updatedBy = obj.optString("updatedBy", "Pusdalops BPBD Kab. Mukomuko").ifBlank { "Pusdalops BPBD Kab. Mukomuko" },
                    updatedAt = obj.optLong("updatedAt", 0L).coerceAtLeast(0L),
                    citizenSummary = obj.optString("citizenSummary", DEFAULT_CITIZEN_SUMMARY).ifBlank { DEFAULT_CITIZEN_SUMMARY },
                    officerSummary = obj.optString("officerSummary", DEFAULT_OFFICER_SUMMARY).ifBlank { DEFAULT_OFFICER_SUMMARY },
                    citizenSteps = parsedCitizen,
                    officerSteps = parsedOfficer,
                    source = source
                )
            } catch (_: Exception) {
                null
            }
        }

        fun fromFirestoreMap(map: Map<String, Any>, source: String = SOURCE_FIRESTORE): AppUsageGuide? {
            return try {
                val rawCitizen = map["citizenSteps"] as? List<*> ?: emptyList<Any>()
                val rawOfficer = map["officerSteps"] as? List<*> ?: emptyList<Any>()

                val parsedCitizen = rawCitizen.mapIndexedNotNull { idx, item ->
                    (item as? Map<*, *>)?.let { UsageGuideStep.fromFirestoreMap(it, idx + 1) }
                }
                val parsedOfficer = rawOfficer.mapIndexedNotNull { idx, item ->
                    (item as? Map<*, *>)?.let { UsageGuideStep.fromFirestoreMap(it, idx + 1) }
                }

                if (parsedCitizen.isEmpty() && parsedOfficer.isEmpty()) return null

                AppUsageGuide(
                    guideVersion = (map["guideVersion"] as? String)?.trim().orEmpty().ifBlank { DEFAULT_GUIDE_VERSION },
                    releaseTitle = (map["releaseTitle"] as? String)?.trim().orEmpty().ifBlank { "Panduan Penggunaan SIPAGAR KITA" },
                    updatedAtLabel = (map["updatedAtLabel"] as? String)?.trim().orEmpty().ifBlank { "Terbaru" },
                    updatedBy = (map["updatedBy"] as? String)?.trim().orEmpty().ifBlank { "Admin BPBD Mukomuko" },
                    updatedAt = ((map["updatedAt"] as? Number)?.toLong() ?: 0L).coerceAtLeast(0L),
                    citizenSummary = (map["citizenSummary"] as? String)?.trim().orEmpty().ifBlank { DEFAULT_CITIZEN_SUMMARY },
                    officerSummary = (map["officerSummary"] as? String)?.trim().orEmpty().ifBlank { DEFAULT_OFFICER_SUMMARY },
                    citizenSteps = parsedCitizen.ifEmpty { DefaultAppUsageGuideCatalog.citizenSteps },
                    officerSteps = parsedOfficer.ifEmpty { DefaultAppUsageGuideCatalog.officerSteps },
                    source = source
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}

/**
 * Katalog bawaan (offline fallback) Panduan Penggunaan SIPAGAR KITA untuk Warga & Petugas.
 */
object DefaultAppUsageGuideCatalog {
    val citizenSteps: List<UsageGuideStep> = listOf(
        UsageGuideStep(
            stepNumber = 1,
            title = "Pantau Cuaca, Kualitas Udara & Maritim",
            description = "Pilih salah satu dari 15 kecamatan di bagian atas layar untuk memantau suhu, curah hujan, prakiraan 7 hari, kualitas udara, hingga tinggi gelombang pesisir Mukomuko.",
            moduleTag = "Beranda & Maritim"
        ),
        UsageGuideStep(
            stepNumber = 2,
            title = "Cek Peringatan Dini, Gempa & Karhutla",
            description = "Buka tab Peringatan, WRS-BMKG, atau Karhutla untuk memantau info gempa terbaru dan potensi bahaya cuaca ekstrem 24 jam tanpa perlu login.",
            moduleTag = "Peringatan Dini"
        ),
        UsageGuideStep(
            stepNumber = 3,
            title = "Lapor Kejadian Bencana + Foto & GPS",
            description = "Masuk ke tab Laporan Warga atau ketuk tombol cepat Darurat 117. Ambil foto kondisi lapangan dan kunci lokasi GPS agar laporan langsung diterima Pusdalops.",
            moduleTag = "Laporan Warga"
        ),
        UsageGuideStep(
            stepNumber = 4,
            title = "Gunakan Kompas Digital & SOP Evakuasi",
            description = "Aktifkan Kompas Digital dan SOP Evakuasi (lengkap dengan Audio Suara/TTS) untuk panduan penyelamatan diri dan arah titik aman meski tanpa sinyal internet.",
            moduleTag = "Kompas & SOP Offline"
        ),
        UsageGuideStep(
            stepNumber = 5,
            title = "Cek Peta Shelter & Kebijakan KRB–RPB–RTRW",
            description = "Lihat peta risiko bencana, 11 lokasi resmi shelter pengungsi, serta ringkasan kebijakan kerawanan dan tata ruang wilayah di kecamatan Anda.",
            moduleTag = "Peta & Trilogi"
        )
    )

    val officerSteps: List<UsageGuideStep> = listOf(
        UsageGuideStep(
            stepNumber = 1,
            title = "Masuk Sesi Petugas Pusdalops / TRC",
            description = "Buka Pusat Akun Petugas lalu masuk memakai NIP dan PIN Cloud resmi (atau Biometrik). Mode Warga terisolasi otomatis ketika Anda keluar sesi.",
            moduleTag = "Autentikasi Petugas"
        ),
        UsageGuideStep(
            stepNumber = 2,
            title = "Verifikasi & Tindak Lanjut Laporan Warga",
            description = "Periksa laporan warga ber-GPS pada tab Laporan Warga, lakukan cross-check ke Kades/Lurah/Camat, lalu perbarui status: Diproses → Divalidasi → Selesai.",
            moduleTag = "Verifikasi Lapangan"
        ),
        UsageGuideStep(
            stepNumber = 3,
            title = "Input Laporan Harian & Cetak PDF Resmi",
            description = "Gunakan Form Laporan Harian Pusdalops & DALA untuk mencatat kronologi, korban/kerusakan, serta cetak PDF berkop BPBD/Kabupaten lengkap dengan QR.",
            moduleTag = "Laporan & PDF"
        ),
        UsageGuideStep(
            stepNumber = 4,
            title = "Kelola Posko, Logistik & Relawan TRC",
            description = "Catat stok logistik bantuan, pantau absensi check-in/out relawan TRC berbasis NIK, serta terbitkan Surat Tugas resmi secara cepat.",
            moduleTag = "Posko & Relawan"
        ),
        UsageGuideStep(
            stepNumber = 5,
            title = "Acuan Trilogi Kebijakan KRB, RPB & RTRW",
            description = "Manfaatkan matriks 12 bahaya KRB 2026–2030, 31 strategi RPB 2025–2029, dan ketentuan KKPR Perda RTRW sebagai rujukan kajian lapangan & penanganan darurat.",
            moduleTag = "KRB • RPB • RTRW"
        ),
        UsageGuideStep(
            stepNumber = 6,
            title = "Sinkronisasi Multi-Perangkat & Update Panduan",
            description = "Pantau status perangkat siaga di Ruang Kerja dan cadangkan data ke Cloud. Khusus Administrator, isi panduan ini dapat diperbarui langsung dari Cloud tanpa update APK.",
            moduleTag = "Ruang Kerja & Admin"
        )
    )

    fun defaultGuide(): AppUsageGuide = AppUsageGuide(
        guideVersion = AppUsageGuide.DEFAULT_GUIDE_VERSION,
        releaseTitle = "Panduan Penggunaan SIPAGAR KITA",
        updatedAtLabel = "27 November 2026",
        updatedBy = "Pusdalops BPBD Kab. Mukomuko",
        updatedAt = 0L,
        citizenSummary = AppUsageGuide.DEFAULT_CITIZEN_SUMMARY,
        officerSummary = AppUsageGuide.DEFAULT_OFFICER_SUMMARY,
        citizenSteps = citizenSteps,
        officerSteps = officerSteps,
        source = AppUsageGuide.SOURCE_BUNDLED
    )
}
