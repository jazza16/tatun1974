package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.CitizenPreparednessDao
import com.example.data.local.EmergencyGoBagItemEntity
import com.example.data.local.OfflineEvacuationSopEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

enum class GoBagExpiryStatus {
    NONE,
    SAFE,
    EXPIRING_SOON,
    EXPIRED
}

data class GoBagExpiryEvaluation(
    val status: GoBagExpiryStatus,
    val daysRemaining: Long? = null,
    val label: String = ""
)

data class OfflineSopStepDto(
    val nomor: Int,
    val judulLangkah: String,
    val deskripsi: String
)

/**
 * Repository 100% Offline (SQLite Room lokal) untuk kesiapsiagaan warga:
 * 1. Checklist Tas Siaga Bencana (Emergency Go-Bag) persisten + `+ Tambah Barang Kustom` + tanggal kedaluwarsa.
 * 2. SOP Evakuasi Mandiri (Gempa Bumi, Tsunami 20-20-20, Banjir, Tanah Longsor) ter-seed di SQLite lokal.
 */
class CitizenPreparednessRepository(
    private val dao: CitizenPreparednessDao
) {
    val goBagItemsFlow: Flow<List<EmergencyGoBagItemEntity>> = dao.observeGoBagItems()
    val evacuationSopsFlow: Flow<List<OfflineEvacuationSopEntity>> = dao.observeEvacuationSops()

    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        if (dao.getGoBagItemCount() == 0) {
            dao.insertGoBagItemsIfAbsent(DEFAULT_GO_BAG_ITEMS)
        }
        if (dao.getEvacuationSopCount() == 0) {
            dao.insertEvacuationSopsIfAbsent(DEFAULT_OFFLINE_SOPS)
        }
    }

    suspend fun getGoBagItemsSnapshot(): List<EmergencyGoBagItemEntity> = withContext(Dispatchers.IO) {
        ensureSeeded()
        dao.getGoBagItemsSnapshot()
    }

    suspend fun getEvacuationSopsSnapshot(): List<OfflineEvacuationSopEntity> = withContext(Dispatchers.IO) {
        ensureSeeded()
        dao.getEvacuationSopsSnapshot()
    }

    suspend fun setItemChecked(itemId: String, checked: Boolean) = withContext(Dispatchers.IO) {
        ensureSeeded()
        dao.updateGoBagChecked(id = itemId, isChecked = checked, updatedAt = System.currentTimeMillis())
    }

    suspend fun addCustomItem(
        title: String,
        description: String,
        expiryDateIso: String?
    ): Result<EmergencyGoBagItemEntity> = withContext(Dispatchers.IO) {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Nama barang Tas Siaga tidak boleh kosong."))
        }
        val cleanExpiry = normalizeIsoDateOrNull(expiryDateIso)
        if (!expiryDateIso.isNullOrBlank() && cleanExpiry == null) {
            return@withContext Result.failure(
                IllegalArgumentException("Format tanggal kedaluwarsa harus YYYY-MM-DD (contoh: 2027-06-30).")
            )
        }
        ensureSeeded()
        val existing = dao.getGoBagItemsSnapshot()
        val nextOrder = (existing.maxOfOrNull { it.sortOrder } ?: 10) + 1
        val entity = EmergencyGoBagItemEntity(
            id = "custom_${UUID.randomUUID().toString().take(8)}",
            title = cleanTitle.take(80),
            description = description.trim().ifBlank { "Perlengkapan darurat tambahan keluarga." }.take(200),
            isChecked = false,
            isCustom = true,
            expiryDateIso = cleanExpiry,
            sortOrder = nextOrder,
            updatedAt = System.currentTimeMillis()
        )
        dao.insertGoBagItem(entity)
        Result.success(entity)
    }

    suspend fun updateItemExpiry(itemId: String, expiryDateIso: String?): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanExpiry = normalizeIsoDateOrNull(expiryDateIso)
        if (!expiryDateIso.isNullOrBlank() && cleanExpiry == null) {
            return@withContext Result.failure(
                IllegalArgumentException("Format tanggal kedaluwarsa harus YYYY-MM-DD (contoh: 2027-06-30).")
            )
        }
        ensureSeeded()
        dao.updateGoBagExpiry(id = itemId, expiryDateIso = cleanExpiry, updatedAt = System.currentTimeMillis())
        Result.success(Unit)
    }

    suspend fun deleteCustomItem(itemId: String) = withContext(Dispatchers.IO) {
        dao.deleteCustomGoBagItem(itemId)
    }

    companion object {
        private val ISO_REGEX = Regex("^\\d{4}-\\d{2}-\\d{2}$")

        fun fromContext(context: Context): CitizenPreparednessRepository {
            val db = AppDatabase.getInstance(context.applicationContext ?: context)
            return CitizenPreparednessRepository(db.citizenPreparednessDao())
        }

        fun normalizeIsoDateOrNull(raw: String?): String? {
            val trimmed = raw?.trim().orEmpty()
            if (trimmed.isBlank()) return null
            if (!ISO_REGEX.matches(trimmed)) return null
            return try {
                val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
                fmt.parse(trimmed) ?: return null
                trimmed
            } catch (_: Exception) {
                null
            }
        }

        fun evaluateExpiry(
            expiryDateIso: String?,
            nowMillis: Long = System.currentTimeMillis()
        ): GoBagExpiryEvaluation {
            val clean = normalizeIsoDateOrNull(expiryDateIso)
                ?: return GoBagExpiryEvaluation(GoBagExpiryStatus.NONE)
            return try {
                val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
                val expDate: Date = fmt.parse(clean) ?: return GoBagExpiryEvaluation(GoBagExpiryStatus.NONE)
                val todayStr = fmt.format(Date(nowMillis))
                val todayDate = fmt.parse(todayStr) ?: Date(nowMillis)
                val diffMs = expDate.time - todayDate.time
                val days = TimeUnit.MILLISECONDS.toDays(diffMs)
                when {
                    days < 0L -> GoBagExpiryEvaluation(
                        status = GoBagExpiryStatus.EXPIRED,
                        daysRemaining = days,
                        label = "KEDALUWARSA ($clean) • Ganti Segera"
                    )
                    days <= 30L -> GoBagExpiryEvaluation(
                        status = GoBagExpiryStatus.EXPIRING_SOON,
                        daysRemaining = days,
                        label = "Segera Kedaluwarsa (${days} hari • $clean)"
                    )
                    else -> GoBagExpiryEvaluation(
                        status = GoBagExpiryStatus.SAFE,
                        daysRemaining = days,
                        label = "Berlaku s.d. $clean"
                    )
                }
            } catch (_: Exception) {
                GoBagExpiryEvaluation(GoBagExpiryStatus.NONE)
            }
        }

        fun parseSopSteps(langkahJson: String): List<OfflineSopStepDto> {
            return try {
                val arr = JSONArray(langkahJson)
                buildList {
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        add(
                            OfflineSopStepDto(
                                nomor = obj.optInt("nomor", i + 1),
                                judulLangkah = obj.optString("judulLangkah", ""),
                                deskripsi = obj.optString("deskripsi", "")
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

        private fun encodeSopSteps(steps: List<OfflineSopStepDto>): String {
            val arr = JSONArray()
            steps.forEach { step ->
                arr.put(
                    JSONObject().apply {
                        put("nomor", step.nomor)
                        put("judulLangkah", step.judulLangkah)
                        put("deskripsi", step.deskripsi)
                    }
                )
            }
            return arr.toString()
        }

        /** 10 Barang Wajib Tas Siaga Bencana (Emergency Go-Bag) sesuai Master PRD BAB 2. */
        val DEFAULT_GO_BAG_ITEMS: List<EmergencyGoBagItemEntity> = listOf(
            EmergencyGoBagItemEntity(
                id = "default_1_air",
                title = "Air Bersih Minum",
                description = "Minimal 3 liter per orang untuk kebutuhan darurat 72 jam.",
                isChecked = false,
                isCustom = false,
                expiryDateIso = "2027-06-30",
                sortOrder = 1
            ),
            EmergencyGoBagItemEntity(
                id = "default_2_p3k",
                title = "Kotak P3K & Obat Rutin",
                description = "Antiseptik, perban, plester, parasetamol, dan obat pribadi keluarga.",
                isChecked = false,
                isCustom = false,
                expiryDateIso = "2026-12-20",
                sortOrder = 2
            ),
            EmergencyGoBagItemEntity(
                id = "default_3_dokumen",
                title = "Dokumen Penting (Kedap Air)",
                description = "KTP, KK, Akta Lahir, Surat Tanah, dan Ijazah dalam plastik tahan air.",
                isChecked = false,
                isCustom = false,
                expiryDateIso = null,
                sortOrder = 3
            ),
            EmergencyGoBagItemEntity(
                id = "default_4_senter",
                title = "Senter & Baterai Cadangan",
                description = "Lampu senter LED atau lampu kepala darurat beserta baterai ekstra.",
                isChecked = false,
                isCustom = false,
                expiryDateIso = null,
                sortOrder = 4
            ),
            EmergencyGoBagItemEntity(
                id = "default_5_peluit",
                title = "Peluit Darurat",
                description = "Sinyal bunyi nyaring untuk memandu regu penolong jika terjebak.",
                isChecked = false,
                isCustom = false,
                expiryDateIso = null,
                sortOrder = 5
            ),
            EmergencyGoBagItemEntity(
                id = "default_6_makanan",
                title = "Makanan Tahan Lama",
                description = "Biskuit gandum, kurma, abon, rendang kering, atau makanan kaleng siap santap.",
                isChecked = false,
                isCustom = false,
                expiryDateIso = "2027-03-15",
                sortOrder = 6
            ),
            EmergencyGoBagItemEntity(
                id = "default_7_powerbank",
                title = "Powerbank Penuh & Kabel",
                description = "Menjaga ponsel tetap menyala untuk memantau peringatan BMKG & BPBD.",
                isChecked = false,
                isCustom = false,
                expiryDateIso = null,
                sortOrder = 7
            ),
            EmergencyGoBagItemEntity(
                id = "default_8_uang",
                title = "Uang Tunai Pecahan Kecil",
                description = "Uang kas secukupnya saat jaringan ATM dan pembayaran digital padam.",
                isChecked = false,
                isCustom = false,
                expiryDateIso = null,
                sortOrder = 8
            ),
            EmergencyGoBagItemEntity(
                id = "default_9_pakaian",
                title = "Pakaian Ganti, Selimut & Jas Hujan",
                description = "1–2 set pakaian hangat, selimut ringan, masker, dan jas hujan ponco.",
                isChecked = false,
                isCustom = false,
                expiryDateIso = null,
                sortOrder = 9
            ),
            EmergencyGoBagItemEntity(
                id = "default_10_radio",
                title = "Radio Portabel Baterai / HT",
                description = "Penerima informasi resmi BPBD/BMKG jika jaringan seluler dan internet terputus.",
                isChecked = false,
                isCustom = false,
                expiryDateIso = null,
                sortOrder = 10
            )
        )

        /** 4 SOP Evakuasi Mandiri (Gempa Bumi, Tsunami 20-20-20, Banjir, Tanah Longsor) ter-seed ke SQLite lokal. */
        val DEFAULT_OFFLINE_SOPS: List<OfflineEvacuationSopEntity> = listOf(
            OfflineEvacuationSopEntity(
                id = "gempa_bumi",
                namaBencana = "Gempa Bumi",
                subJudul = "Prosedur Penyelamatan Diri Saat Guncangan",
                rumusUtama = "Prinsip Utama: DROP • COVER • HOLD ON",
                langkahJson = encodeSopSteps(
                    listOf(
                        OfflineSopStepDto(1, "DROP (Merunduk)", "Segera jatuhkan tubuh ke posisi merangkak/bertumpu pada tangan dan lutut sebelum guncangan gempa menjatuhkan Anda."),
                        OfflineSopStepDto(2, "COVER (Lindungi Kepala & Leher)", "Berlindung di bawah meja yang kokoh. Jika tidak ada meja, lindungi kepala dan leher dengan kedua tangan/bantal serta merapat ke dinding bagian dalam yang jauh dari kaca."),
                        OfflineSopStepDto(3, "HOLD ON (Bertahan)", "Pegang erat kaki meja atau pelindung Anda hingga guncangan benar-benar berhenti. Jangan berlari keluar saat bangunan masih berguncang."),
                        OfflineSopStepDto(4, "Evakuasi Setelah Guncangan Berhenti", "Keluar dengan tertib melalui jalur evakuasi menuju Titik Kumpul terbuka. Jauhi kaca, tiang listrik, pohon tinggi, dan papan reklame. Gunakan tangga darurat, jangan gunakan lift.")
                    )
                ),
                catatanPenting = "Waspadai gempa susulan. Jika berada di wilayah pesisir Kabupaten Mukomuko dan gempa terasa kuat atau berlangsung lama, segera bersiap melakukan evakuasi tsunami.",
                sortOrder = 1
            ),
            OfflineEvacuationSopEntity(
                id = "tsunami",
                namaBencana = "Tsunami",
                subJudul = "Evakuasi Cepat Wilayah Pesisir Pantai Mukomuko",
                rumusUtama = "Rumus BNPB/BPBD: 20 - 20 - 20",
                langkahJson = encodeSopSteps(
                    listOf(
                        OfflineSopStepDto(1, "20 Detik Pertama (Kenali Guncangan)", "Jika merasakan gempa bumi yang berlangsung selama 20 detik atau lebih (atau guncangan sangat kuat hingga sulit berdiri), JANGAN tunggu peringatan sirine resmi."),
                        OfflineSopStepDto(2, "20 Menit Waktu Evakuasi", "Anda memiliki waktu sekitar 20 menit sebelum gelombang tsunami berpotensi tiba di garis pantai. Segera tinggalkan pantai dan lari menuju tempat tinggi dengan membawa Tas Siaga Bencana."),
                        OfflineSopStepDto(3, "20 Meter Ketinggian Minimal", "Evakuasi diri dan keluarga ke dataran tinggi atau bukit evakuasi dengan ketinggian minimal 20 meter di atas permukaan laut (MDPL), atau menjauh minimal 2–3 km dari bibir pantai."),
                        OfflineSopStepDto(4, "Waspadai Tanda Alam Air Laut Surut", "Jika melihat air laut surut secara tiba-tiba atau terdengar suara gemuruh keras dari arah laut, segera berlari ke dataran tinggi dan jauhi aliran sungai yang bermuara ke laut.")
                    )
                ),
                catatanPenting = "Utamakan evakuasi berjalan kaki atau berlari mengikuti rambu jalur evakuasi agar tidak terjebak kemacetan kendaraan. Tetap di tempat tinggi hingga ada pernyataan aman resmi dari BMKG/BPBD.",
                sortOrder = 2
            ),
            OfflineEvacuationSopEntity(
                id = "banjir",
                namaBencana = "Banjir",
                subJudul = "Langkah Penyelamatan Saat Luapan Sungai & Genangan",
                rumusUtama = "Prioritas: Matikan Listrik • Amankan Dokumen • Evakuasi",
                langkahJson = encodeSopSteps(
                    listOf(
                        OfflineSopStepDto(1, "Matikan Aliran Listrik (MCB) & Gas", "Segera matikan sakelar utama listrik (MCB) di rumah dan lepaskan regulator tabung gas LPG untuk mencegah risiko tersengat listrik atau kebakaran saat air mulai naik."),
                        OfflineSopStepDto(2, "Amankan Dokumen & Tas Siaga Bencana", "Masukkan dokumen penting (KTP, KK, Ijazah, Surat Tanah), obat-obatan pribadi, pakaian ganti, dan senter ke dalam wadah plastik kedap air di Tas Siaga Bencana."),
                        OfflineSopStepDto(3, "Evakuasi Sebelum Air Terlalu Tinggi", "Segera mengungsi ke posko evakuasi atau dataran yang lebih tinggi sebelum ketinggian air menyulitkan pergerakan. Prioritaskan lansia, anak-anak, ibu hamil, dan penyandang disabilitas."),
                        OfflineSopStepDto(4, "Hindari Arus Deras & Genangan Dalam", "Jangan nekat menerobos jalanan atau jembatan yang tergenang arus deras (baik berjalan kaki maupun berkendara). Gunakan tongkat untuk mengecek kedalaman jalan di depan Anda.")
                    )
                ),
                catatanPenting = "Pantau terus kenaikan debit air sungai (seperti DAS Selagan, Manjuto, dan Air Dikit) saat hujan lebat berlangsung lebih dari 2 jam di wilayah hulu dan hilir.",
                sortOrder = 3
            ),
            OfflineEvacuationSopEntity(
                id = "tanah_longsor",
                namaBencana = "Tanah Longsor",
                subJudul = "Kewaspadaan Daerah Tebing & Perbukitan",
                rumusUtama = "Prinsip: Kenali Retakan • Menjauh Tegak Lurus Jalur Longsor",
                langkahJson = encodeSopSteps(
                    listOf(
                        OfflineSopStepDto(1, "Kenali Tanda Awal Longsoran", "Waspadai munculnya retakan baru di tanah/dinding rumah, pohon atau tiang listrik yang mulai miring, mata air yang mendadak keruh, atau suara gemuruh dari atas tebing saat/setelah hujan lebat."),
                        OfflineSopStepDto(2, "Segera Keluar & Menjauh Secara Tegak Lurus", "Jika terdengar gemuruh longsor, segera keluar dari bangunan dan berlari ke arah samping (tegak lurus) menjauhi jalur luncuran material longsor, bukan searah dengan turunan tebing."),
                        OfflineSopStepDto(3, "Posisi Melingkar (Bila Terjebak)", "Apabila tidak sempat menghindar dari jangkauan material longsor, segera gulung tubuh seperti bola dengan melindungi kepala menggunakan kedua tangan."),
                        OfflineSopStepDto(4, "Jangan Kembali ke Lokasi Longsoran", "Setelah longsor pertama berhenti, jangan langsung masuk kembali ke area longsoran karena tanah masih labil dan sangat berisiko terjadi longsor susulan.")
                    )
                ),
                catatanPenting = "Warga di kawasan perbukitan atau dekat lereng tebing diimbau melakukan evakuasi mandiri lebih awal apabila turun hujan deras terus-menerus pada malam hari.",
                sortOrder = 4
            )
        )
    }
}
