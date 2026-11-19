package com.example.model

import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Kategori Kelompok Rentan standar kaji cepat BNPB/BPBD.
 */
enum class VulnerableGroupCategory(val label: String) {
    BALITA("Balita"),
    IBU_HAMIL("Ibu Hamil"),
    LANSIA("Lansia"),
    DISABILITAS("Disabilitas");

    companion object {
        fun fromNameOrLabel(raw: String?): VulnerableGroupCategory {
            return entries.firstOrNull {
                it.name.equals(raw, ignoreCase = true) || it.label.equals(raw, ignoreCase = true)
            } ?: BALITA
        }
    }
}

/**
 * Titik lokasi pengungsian (petugas dapat menambah lebih dari 1 titik).
 */
data class EvacuationSite(
    val id: String = UUID.randomUUID().toString(),
    val locationName: String,
    val evacueeCount: Int = 0,
    val notes: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("locationName", locationName)
        put("evacueeCount", evacueeCount)
        put("notes", notes)
    }

    companion object {
        fun fromJson(obj: JSONObject): EvacuationSite = EvacuationSite(
            id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
            locationName = obj.optString("locationName"),
            evacueeCount = obj.optInt("evacueeCount", 0).coerceAtLeast(0),
            notes = obj.optString("notes")
        )
    }
}

/**
 * Data jumlah jiwa pada masing-masing kelompok rentan.
 */
data class VulnerableGroupEntry(
    val id: String = UUID.randomUUID().toString(),
    val category: VulnerableGroupCategory,
    val count: Int,
    val notes: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("category", category.name)
        put("count", count)
        put("notes", notes)
    }

    companion object {
        fun fromJson(obj: JSONObject): VulnerableGroupEntry = VulnerableGroupEntry(
            id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
            category = VulnerableGroupCategory.fromNameOrLabel(obj.optString("category")),
            count = obj.optInt("count", 0).coerceAtLeast(0),
            notes = obj.optString("notes")
        )
    }
}

/**
 * Tab 1: Korban & Pengungsi (Casualties & Evacuees).
 */
data class CasualtiesAndEvacueesData(
    val dead: Int = 0,
    val missing: Int = 0,
    val heavyInjured: Int = 0,
    val lightInjured: Int = 0,
    val totalEvacuees: Int = 0,
    val evacuationSites: List<EvacuationSite> = emptyList(),
    val vulnerableGroups: List<VulnerableGroupEntry> = emptyList()
) {
    val totalCasualties: Int
        get() = dead + missing + heavyInjured + lightInjured

    val totalVulnerable: Int
        get() = vulnerableGroups.sumOf { it.count }

    fun toJson(): JSONObject = JSONObject().apply {
        put("dead", dead)
        put("missing", missing)
        put("heavyInjured", heavyInjured)
        put("lightInjured", lightInjured)
        put("totalEvacuees", totalEvacuees)
        put("evacuationSites", JSONArray().apply { evacuationSites.forEach { put(it.toJson()) } })
        put("vulnerableGroups", JSONArray().apply { vulnerableGroups.forEach { put(it.toJson()) } })
    }

    companion object {
        fun fromJson(obj: JSONObject?): CasualtiesAndEvacueesData {
            if (obj == null) return CasualtiesAndEvacueesData()
            val sites = mutableListOf<EvacuationSite>()
            val sitesArr = obj.optJSONArray("evacuationSites")
            if (sitesArr != null) {
                for (i in 0 until sitesArr.length()) {
                    sitesArr.optJSONObject(i)?.let { sites.add(EvacuationSite.fromJson(it)) }
                }
            }
            val groups = mutableListOf<VulnerableGroupEntry>()
            val groupsArr = obj.optJSONArray("vulnerableGroups")
            if (groupsArr != null) {
                for (i in 0 until groupsArr.length()) {
                    groupsArr.optJSONObject(i)?.let { groups.add(VulnerableGroupEntry.fromJson(it)) }
                }
            }
            return CasualtiesAndEvacueesData(
                dead = obj.optInt("dead", 0).coerceAtLeast(0),
                missing = obj.optInt("missing", 0).coerceAtLeast(0),
                heavyInjured = obj.optInt("heavyInjured", 0).coerceAtLeast(0),
                lightInjured = obj.optInt("lightInjured", 0).coerceAtLeast(0),
                totalEvacuees = obj.optInt("totalEvacuees", 0).coerceAtLeast(0),
                evacuationSites = sites,
                vulnerableGroups = groups
            )
        }
    }
}

/**
 * Tab 2: Kerusakan & Kerugian (Damages & Losses).
 */
data class DamagesAndLossesData(
    // Rumah (Unit)
    val houseHeavy: Int = 0,
    val houseModerate: Int = 0,
    val houseLight: Int = 0,
    // Fasilitas Umum (Unit)
    val schoolUnits: Int = 0,
    val puskesmasUnits: Int = 0,
    val officeUnits: Int = 0,
    // Infrastruktur (Meter / Unit)
    val roadMeters: Int = 0,
    val bridgeUnits: Int = 0,
    val irrigationMeters: Int = 0,
    // Estimasi Kerugian & Narasi
    val estimatedLossIdr: Long = 0L,
    val damageDescription: String = ""
) {
    val totalHousesDamaged: Int
        get() = houseHeavy + houseModerate + houseLight

    val totalPublicFacilitiesDamaged: Int
        get() = schoolUnits + puskesmasUnits + officeUnits

    fun formatEstimatedLossRupiah(): String {
        val nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID"))
        return "Rp ${nf.format(estimatedLossIdr)}"
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("houseHeavy", houseHeavy)
        put("houseModerate", houseModerate)
        put("houseLight", houseLight)
        put("schoolUnits", schoolUnits)
        put("puskesmasUnits", puskesmasUnits)
        put("officeUnits", officeUnits)
        put("roadMeters", roadMeters)
        put("bridgeUnits", bridgeUnits)
        put("irrigationMeters", irrigationMeters)
        put("estimatedLossIdr", estimatedLossIdr)
        put("damageDescription", damageDescription)
    }

    companion object {
        fun fromJson(obj: JSONObject?): DamagesAndLossesData {
            if (obj == null) return DamagesAndLossesData()
            return DamagesAndLossesData(
                houseHeavy = obj.optInt("houseHeavy", 0).coerceAtLeast(0),
                houseModerate = obj.optInt("houseModerate", 0).coerceAtLeast(0),
                houseLight = obj.optInt("houseLight", 0).coerceAtLeast(0),
                schoolUnits = obj.optInt("schoolUnits", 0).coerceAtLeast(0),
                puskesmasUnits = obj.optInt("puskesmasUnits", 0).coerceAtLeast(0),
                officeUnits = obj.optInt("officeUnits", 0).coerceAtLeast(0),
                roadMeters = obj.optInt("roadMeters", 0).coerceAtLeast(0),
                bridgeUnits = obj.optInt("bridgeUnits", 0).coerceAtLeast(0),
                irrigationMeters = obj.optInt("irrigationMeters", 0).coerceAtLeast(0),
                estimatedLossIdr = obj.optLong("estimatedLossIdr", 0L).coerceAtLeast(0L),
                damageDescription = obj.optString("damageDescription")
            )
        }
    }
}

/**
 * Jenis Kebutuhan Mendesak Pasca Bencana.
 */
enum class UrgentNeedCategory(val label: String, val defaultUnit: String) {
    LOGISTIK("Logistik", "Paket"),
    MEDIS("Medis", "Box"),
    AIR_BERSIH("Air Bersih", "Liter"),
    TENDA("Tenda", "Unit"),
    ALAT_BERAT("Alat Berat", "Unit");

    companion object {
        fun fromNameOrLabel(raw: String?): UrgentNeedCategory {
            return entries.firstOrNull {
                it.name.equals(raw, ignoreCase = true) || it.label.equals(raw, ignoreCase = true)
            } ?: LOGISTIK
        }
    }
}

/**
 * Status ketersediaan kebutuhan di lapangan.
 */
enum class UrgentNeedStatus(val label: String) {
    MENDESAK("Mendesak"),
    TERSEDIA("Tersedia");

    companion object {
        fun fromNameOrLabel(raw: String?): UrgentNeedStatus {
            return entries.firstOrNull {
                it.name.equals(raw, ignoreCase = true) || it.label.equals(raw, ignoreCase = true)
            } ?: MENDESAK
        }
    }
}

/**
 * Tab 3: Baris item pada daftar dinamis Kebutuhan Mendesak.
 */
data class UrgentNeedItem(
    val id: String = UUID.randomUUID().toString(),
    val category: UrgentNeedCategory,
    val quantity: Int,
    val unit: String,
    val status: UrgentNeedStatus = UrgentNeedStatus.MENDESAK,
    val notes: String = ""
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("category", category.name)
        put("quantity", quantity)
        put("unit", unit)
        put("status", status.name)
        put("notes", notes)
    }

    companion object {
        fun fromJson(obj: JSONObject): UrgentNeedItem = UrgentNeedItem(
            id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
            category = UrgentNeedCategory.fromNameOrLabel(obj.optString("category")),
            quantity = obj.optInt("quantity", 1).coerceAtLeast(1),
            unit = obj.optString("unit").ifBlank { "Unit" },
            status = UrgentNeedStatus.fromNameOrLabel(obj.optString("status")),
            notes = obj.optString("notes")
        )
    }
}

/**
 * State utama lengkap modul Pasca Bencana / DALA (Data dan Informasi Bencana).
 */
data class DalaAssessmentState(
    val assessmentId: String = "DALA-${System.currentTimeMillis().toString().takeLast(6)}",
    val disasterTitle: String = "Banjir Bandang & Tanah Longsor",
    val district: String = "Kec. Kota Mukomuko",
    val assessmentDate: String = SimpleDateFormat("dd MMM yyyy, HH:mm 'WIB'", Locale.forLanguageTag("id-ID")).format(Date()),
    val officerName: String = "",
    val officerNip: String = "",
    val casualties: CasualtiesAndEvacueesData = CasualtiesAndEvacueesData(),
    val damages: DamagesAndLossesData = DamagesAndLossesData(),
    val urgentNeeds: List<UrgentNeedItem> = emptyList(),
    val submissionStatus: String = STATUS_DRAFT,
    val lastAutoSavedAt: Long = 0L
) {
    val urgentNeedsPendingCount: Int
        get() = urgentNeeds.count { it.status == UrgentNeedStatus.MENDESAK }

    fun toJson(): String = JSONObject().apply {
        put("assessmentId", assessmentId)
        put("disasterTitle", disasterTitle)
        put("district", district)
        put("assessmentDate", assessmentDate)
        put("officerName", officerName)
        put("officerNip", officerNip)
        put("casualties", casualties.toJson())
        put("damages", damages.toJson())
        put("urgentNeeds", JSONArray().apply { urgentNeeds.forEach { put(it.toJson()) } })
        put("submissionStatus", submissionStatus)
        put("lastAutoSavedAt", lastAutoSavedAt)
    }.toString()

    companion object {
        const val STATUS_DRAFT = "DRAFT"
        const val STATUS_SUBMITTED = "TERKIRIM_PUSDALOPS"

        fun fromJson(rawJson: String): DalaAssessmentState? {
            if (rawJson.isBlank()) return null
            return try {
                val obj = JSONObject(rawJson)
                val needs = mutableListOf<UrgentNeedItem>()
                val needsArr = obj.optJSONArray("urgentNeeds")
                if (needsArr != null) {
                    for (i in 0 until needsArr.length()) {
                        needsArr.optJSONObject(i)?.let { needs.add(UrgentNeedItem.fromJson(it)) }
                    }
                }
                DalaAssessmentState(
                    assessmentId = obj.optString("assessmentId").ifBlank { "DALA-001" },
                    disasterTitle = obj.optString("disasterTitle").ifBlank { "Bencana Hidrometeorologi" },
                    district = obj.optString("district").ifBlank { "Kec. Kota Mukomuko" },
                    assessmentDate = obj.optString("assessmentDate"),
                    officerName = obj.optString("officerName"),
                    officerNip = obj.optString("officerNip"),
                    casualties = CasualtiesAndEvacueesData.fromJson(obj.optJSONObject("casualties")),
                    damages = DamagesAndLossesData.fromJson(obj.optJSONObject("damages")),
                    urgentNeeds = needs,
                    submissionStatus = obj.optString("submissionStatus").ifBlank { STATUS_DRAFT },
                    lastAutoSavedAt = obj.optLong("lastAutoSavedAt", 0L)
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}

/**
 * Dummy Data realistis untuk simulasi Kaji Cepat DALA BPBD Kabupaten Mukomuko.
 */
object DalaDummyDataFactory {
    fun createSampleAssessment(
        officerName: String = "",
        officerNip: String = "",
        district: String = "Kec. Kota Mukomuko"
    ): DalaAssessmentState {
        return DalaAssessmentState(
            assessmentId = "DALA-MM-2026-01",
            disasterTitle = "Banjir Luapan Sungai Manjuto & Longsor Tebing",
            district = district.ifBlank { "Kec. Kota Mukomuko" },
            officerName = officerName,
            officerNip = officerNip,
            casualties = CasualtiesAndEvacueesData(
                dead = 0,
                missing = 1,
                heavyInjured = 3,
                lightInjured = 12,
                totalEvacuees = 185,
                evacuationSites = listOf(
                    EvacuationSite(
                        id = "site-1",
                        locationName = "Balai Desa Tanah Rekah",
                        evacueeCount = 110,
                        notes = "Dapur umum aktif, butuh tambahan matras"
                    ),
                    EvacuationSite(
                        id = "site-2",
                        locationName = "Masjid Agung Baitul Huda Mukomuko",
                        evacueeCount = 75,
                        notes = "Akses air bersih terbatas"
                    )
                ),
                vulnerableGroups = listOf(
                    VulnerableGroupEntry(id = "vg-1", category = VulnerableGroupCategory.BALITA, count = 24, notes = "Butuh susu formula & popok"),
                    VulnerableGroupEntry(id = "vg-2", category = VulnerableGroupCategory.IBU_HAMIL, count = 6, notes = "Pemantauan bidan Puskesmas"),
                    VulnerableGroupEntry(id = "vg-3", category = VulnerableGroupCategory.LANSIA, count = 19, notes = "Butuh obat hipertensi & selimut"),
                    VulnerableGroupEntry(id = "vg-4", category = VulnerableGroupCategory.DISABILITAS, count = 3, notes = "Prioritas tenda aksesibilitas")
                )
            ),
            damages = DamagesAndLossesData(
                houseHeavy = 14,
                houseModerate = 28,
                houseLight = 53,
                schoolUnits = 2,
                puskesmasUnits = 1,
                officeUnits = 1,
                roadMeters = 350,
                bridgeUnits = 1,
                irrigationMeters = 220,
                estimatedLossIdr = 2_450_000_000L,
                damageDescription = "Genangan setinggi 80–140 cm merusak dinding rumah semi permanen di bantaran sungai, menggerus oprit jembatan penghubung desa sepanjang 12m, serta menimbun saluran irigasi primer."
            ),
            urgentNeeds = listOf(
                UrgentNeedItem(
                    id = "need-1",
                    category = UrgentNeedCategory.LOGISTIK,
                    quantity = 200,
                    unit = "Paket",
                    status = UrgentNeedStatus.MENDESAK,
                    notes = "Sembako & makanan siap saji untuk 3 hari"
                ),
                UrgentNeedItem(
                    id = "need-2",
                    category = UrgentNeedCategory.AIR_BERSIH,
                    quantity = 10000,
                    unit = "Liter",
                    status = UrgentNeedStatus.MENDESAK,
                    notes = "2 unit tangki air bersih untuk 2 titik pengungsian"
                ),
                UrgentNeedItem(
                    id = "need-3",
                    category = UrgentNeedCategory.TENDA,
                    quantity = 6,
                    unit = "Unit",
                    status = UrgentNeedStatus.TERSEDIA,
                    notes = "Tenda keluarga BPBD sudah terpasang di Balai Desa"
                ),
                UrgentNeedItem(
                    id = "need-4",
                    category = UrgentNeedCategory.MEDIS,
                    quantity = 15,
                    unit = "Box",
                    status = UrgentNeedStatus.MENDESAK,
                    notes = "Obat diare, salep kulit, P3K, dan vitamin balita"
                ),
                UrgentNeedItem(
                    id = "need-5",
                    category = UrgentNeedCategory.ALAT_BERAT,
                    quantity = 1,
                    unit = "Unit",
                    status = UrgentNeedStatus.MENDESAK,
                    notes = "Excavator mini untuk pembersihan material longsor jalan desa"
                )
            ),
            submissionStatus = DalaAssessmentState.STATUS_DRAFT,
            lastAutoSavedAt = System.currentTimeMillis()
        )
    }
}
