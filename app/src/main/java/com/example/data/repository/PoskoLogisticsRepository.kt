package com.example.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class PoskoRefugeeUnit(
    val poskoId: String,
    val poskoCode: String,
    val poskoName: String,
    val district: String,
    val incidentId: String,
    val maxCapacity: Int,
    val totalJiwa: Int,
    val lansia: Int,
    val ibuHamil: Int,
    val balita: Int,
    val disabilitas: Int,
    val updatedAtIso: String,
    val updatedByOfficer: String
) {
    val totalKelompokRentan: Int
        get() = lansia + ibuHamil + balita + disabilitas

    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("poskoId", poskoId)
        put("poskoCode", poskoCode)
        put("poskoName", poskoName)
        put("district", district)
        put("incidentId", incidentId)
        put("maxCapacity", maxCapacity)
        put("totalJiwa", totalJiwa)
        put("lansia", lansia)
        put("ibuHamil", ibuHamil)
        put("balita", balita)
        put("disabilitas", disabilitas)
        put("updatedAtIso", updatedAtIso)
        put("updatedByOfficer", updatedByOfficer)
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): PoskoRefugeeUnit = PoskoRefugeeUnit(
            poskoId = obj.optString("poskoId", ""),
            poskoCode = obj.optString("poskoCode", ""),
            poskoName = obj.optString("poskoName", ""),
            district = obj.optString("district", ""),
            incidentId = obj.optString("incidentId", "INC-202611-MM-0001"),
            maxCapacity = obj.optInt("maxCapacity", 500),
            totalJiwa = obj.optInt("totalJiwa", 0),
            lansia = obj.optInt("lansia", 0),
            ibuHamil = obj.optInt("ibuHamil", 0),
            balita = obj.optInt("balita", 0),
            disabilitas = obj.optInt("disabilitas", 0),
            updatedAtIso = obj.optString("updatedAtIso", ""),
            updatedByOfficer = obj.optString("updatedByOfficer", "Petugas Posko")
        )
    }
}

data class PoskoLogisticCommodity(
    val code: String,
    val name: String,
    val category: String, // "PANGAN", "HUNIAN", "MEDIS", "AIR_SANITASI"
    val unit: String,
    val currentStock: Int,
    val minThreshold: Int = 10,
    val expiryDateIso: String? = null, // YYYY-MM-DD (FEFO tracking)
    val poskoCode: String = "POSKO-MM-001",
    val updatedAtIso: String = ""
) {
    /**
     * Aturan PRD SIPAGAR KITA: Jika stok < 10, status otomatis KRITIS (Merah Menyala).
     */
    val isCritical: Boolean
        get() = currentStock < minThreshold

    val daysUntilExpiry: Long?
        get() {
            val raw = expiryDateIso?.trim().orEmpty()
            if (raw.isEmpty()) return null
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val target = sdf.parse(raw) ?: return null
                val diffMs = target.time - System.currentTimeMillis()
                TimeUnit.MILLISECONDS.toDays(diffMs)
            } catch (_: Exception) {
                null
            }
        }

    val isExpired: Boolean
        get() = (daysUntilExpiry ?: 999L) < 0L

    val isExpiringSoonFefo: Boolean
        get() {
            val days = daysUntilExpiry ?: return false
            return days in 0L..30L
        }

    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("code", code)
        put("name", name)
        put("category", category)
        put("unit", unit)
        put("currentStock", currentStock)
        put("minThreshold", minThreshold)
        put("expiryDateIso", expiryDateIso ?: "")
        put("poskoCode", poskoCode)
        put("updatedAtIso", updatedAtIso)
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): PoskoLogisticCommodity {
            val rawExp = obj.optString("expiryDateIso", "").trim()
            return PoskoLogisticCommodity(
                code = obj.optString("code", ""),
                name = obj.optString("name", ""),
                category = obj.optString("category", "PANGAN"),
                unit = obj.optString("unit", "Paket"),
                currentStock = obj.optInt("currentStock", 0),
                minThreshold = obj.optInt("minThreshold", 10),
                expiryDateIso = rawExp.ifEmpty { null },
                poskoCode = obj.optString("poskoCode", "POSKO-MM-001"),
                updatedAtIso = obj.optString("updatedAtIso", "")
            )
        }
    }
}

data class NikAidDistributionRecord(
    val distributionId: String,
    val recipientNik: String, // CHAR(16) Primary Key Anti-Data Ganda
    val recipientNoKk: String = "",
    val recipientName: String,
    val vulnerabilityGroup: String,
    val commodityCode: String,
    val commodityName: String,
    val quantity: Int,
    val unit: String,
    val poskoCode: String,
    val distributedByOfficer: String,
    val timestampIso: String,
    val isSyncedOfflineFirst: Boolean = false
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("distributionId", distributionId)
        put("recipientNik", recipientNik)
        put("recipientNoKk", recipientNoKk)
        put("recipientName", recipientName)
        put("vulnerabilityGroup", vulnerabilityGroup)
        put("commodityCode", commodityCode)
        put("commodityName", commodityName)
        put("quantity", quantity)
        put("unit", unit)
        put("poskoCode", poskoCode)
        put("distributedByOfficer", distributedByOfficer)
        put("timestampIso", timestampIso)
        put("isSyncedOfflineFirst", isSyncedOfflineFirst)
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): NikAidDistributionRecord = NikAidDistributionRecord(
            distributionId = obj.optString("distributionId", ""),
            recipientNik = obj.optString("recipientNik", ""),
            recipientNoKk = obj.optString("recipientNoKk", ""),
            recipientName = obj.optString("recipientName", ""),
            vulnerabilityGroup = obj.optString("vulnerabilityGroup", "Lansia"),
            commodityCode = obj.optString("commodityCode", ""),
            commodityName = obj.optString("commodityName", ""),
            quantity = obj.optInt("quantity", 1),
            unit = obj.optString("unit", "Paket"),
            poskoCode = obj.optString("poskoCode", "POSKO-MM-001"),
            distributedByOfficer = obj.optString("distributedByOfficer", "Petugas Posko"),
            timestampIso = obj.optString("timestampIso", ""),
            isSyncedOfflineFirst = obj.optBoolean("isSyncedOfflineFirst", false)
        )
    }
}

data class PoskoDalaLockedSnapshot(
    val incidentId: String,
    val activePoskoCount: Int,
    val totalJiwa: Int,
    val lansia: Int,
    val ibuHamil: Int,
    val balita: Int,
    val disabilitas: Int,
    val lockedAtIso: String,
    val lockedByOfficer: String,
    val isLocked: Boolean = true
) {
    val totalKelompokRentan: Int
        get() = lansia + ibuHamil + balita + disabilitas

    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("incidentId", incidentId)
        put("activePoskoCount", activePoskoCount)
        put("totalJiwa", totalJiwa)
        put("lansia", lansia)
        put("ibuHamil", ibuHamil)
        put("balita", balita)
        put("disabilitas", disabilitas)
        put("lockedAtIso", lockedAtIso)
        put("lockedByOfficer", lockedByOfficer)
        put("isLocked", isLocked)
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): PoskoDalaLockedSnapshot = PoskoDalaLockedSnapshot(
            incidentId = obj.optString("incidentId", "INC-202611-MM-0001"),
            activePoskoCount = obj.optInt("activePoskoCount", 3),
            totalJiwa = obj.optInt("totalJiwa", 0),
            lansia = obj.optInt("lansia", 0),
            ibuHamil = obj.optInt("ibuHamil", 0),
            balita = obj.optInt("balita", 0),
            disabilitas = obj.optInt("disabilitas", 0),
            lockedAtIso = obj.optString("lockedAtIso", ""),
            lockedByOfficer = obj.optString("lockedByOfficer", ""),
            isLocked = obj.optBoolean("isLocked", true)
        )
    }
}

/**
 * Repository Offline-First untuk Pengelolaan Pengungsi & Logistik Posko (Master PRD SIPAGAR KITA).
 * Menggunakan org.json.JSONObject/JSONArray bawaan Android agar 100% bebas dari SerializationException
 * pada saat sesi login Petugas aktif.
 */
class PoskoLogisticsRepository internal constructor(
    private val prefs: android.content.SharedPreferences?
) {

    private val _poskoUnits = MutableStateFlow<List<PoskoRefugeeUnit>>(emptyList())
    val poskoUnits: StateFlow<List<PoskoRefugeeUnit>> = _poskoUnits.asStateFlow()

    private val _logisticStocks = MutableStateFlow<List<PoskoLogisticCommodity>>(emptyList())
    val logisticStocks: StateFlow<List<PoskoLogisticCommodity>> = _logisticStocks.asStateFlow()

    private val _distributionLedger = MutableStateFlow<List<NikAidDistributionRecord>>(emptyList())
    val distributionLedger: StateFlow<List<NikAidDistributionRecord>> = _distributionLedger.asStateFlow()

    private val _dalaLockedSnapshot = MutableStateFlow<PoskoDalaLockedSnapshot?>(null)
    val dalaLockedSnapshot: StateFlow<PoskoDalaLockedSnapshot?> = _dalaLockedSnapshot.asStateFlow()

    init {
        loadFromLocalStorage()
    }

    private fun nowIsoString(): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            sdf.format(Date())
        } catch (_: Exception) {
            "2026-11-25 08:00"
        }
    }

    private fun loadFromLocalStorage() {
        // 1. Load Posko Units
        val poskoRaw = prefs?.getString(KEY_POSKO_UNITS, null)
        val loadedPosko = if (!poskoRaw.isNullOrBlank()) {
            runCatching {
                val arr = JSONArray(poskoRaw)
                buildList {
                    for (i in 0 until arr.length()) {
                        val itemObj = arr.optJSONObject(i)
                        if (itemObj != null) add(PoskoRefugeeUnit.fromJsonObject(itemObj))
                    }
                }
            }.getOrNull()
        } else null

        if (loadedPosko.isNullOrEmpty()) {
            _poskoUnits.value = defaultPoskoSeed()
            savePoskoUnits()
        } else {
            _poskoUnits.value = loadedPosko
        }

        // 2. Load Logistic Stocks
        val stockRaw = prefs?.getString(KEY_LOGISTIC_STOCKS, null)
        val loadedStocks = if (!stockRaw.isNullOrBlank()) {
            runCatching {
                val arr = JSONArray(stockRaw)
                buildList {
                    for (i in 0 until arr.length()) {
                        val itemObj = arr.optJSONObject(i)
                        if (itemObj != null) add(PoskoLogisticCommodity.fromJsonObject(itemObj))
                    }
                }
            }.getOrNull()
        } else null

        if (loadedStocks.isNullOrEmpty()) {
            _logisticStocks.value = defaultLogisticsSeed()
            saveLogisticStocks()
        } else {
            _logisticStocks.value = loadedStocks
        }

        // 3. Load Distribution Ledger
        val ledgerRaw = prefs?.getString(KEY_DISTRIBUTION_LEDGER, null)
        val loadedLedger = if (!ledgerRaw.isNullOrBlank()) {
            runCatching {
                val arr = JSONArray(ledgerRaw)
                buildList {
                    for (i in 0 until arr.length()) {
                        val itemObj = arr.optJSONObject(i)
                        if (itemObj != null) add(NikAidDistributionRecord.fromJsonObject(itemObj))
                    }
                }
            }.getOrNull()
        } else null

        if (loadedLedger.isNullOrEmpty()) {
            _distributionLedger.value = defaultDistributionSeed()
            saveDistributionLedger()
        } else {
            _distributionLedger.value = loadedLedger
        }

        // 4. Load Locked DaLA Snapshot
        val snapshotRaw = prefs?.getString(KEY_DALA_SNAPSHOT, null)
        if (!snapshotRaw.isNullOrBlank()) {
            _dalaLockedSnapshot.value = runCatching {
                PoskoDalaLockedSnapshot.fromJsonObject(JSONObject(snapshotRaw))
            }.getOrNull()
        }
    }

    private fun savePoskoUnits() {
        runCatching {
            val arr = JSONArray()
            _poskoUnits.value.forEach { arr.put(it.toJsonObject()) }
            prefs?.edit()?.putString(KEY_POSKO_UNITS, arr.toString())?.apply()
        }
    }

    private fun saveLogisticStocks() {
        runCatching {
            val arr = JSONArray()
            _logisticStocks.value.forEach { arr.put(it.toJsonObject()) }
            prefs?.edit()?.putString(KEY_LOGISTIC_STOCKS, arr.toString())?.apply()
        }
    }

    private fun saveDistributionLedger() {
        runCatching {
            val arr = JSONArray()
            _distributionLedger.value.forEach { arr.put(it.toJsonObject()) }
            prefs?.edit()?.putString(KEY_DISTRIBUTION_LEDGER, arr.toString())?.apply()
        }
    }

    /**
     * Validasi & Simpan Data Pengungsi Posko.
     * Menolak input jika (Lansia + Ibu Hamil + Balita + Disabilitas) > Total Jiwa.
     */
    fun validateAndUpdatePoskoRefugees(
        poskoId: String,
        totalJiwa: Int,
        lansia: Int,
        ibuHamil: Int,
        balita: Int,
        disabilitas: Int,
        officerName: String
    ): Result<PoskoRefugeeUnit> {
        if (totalJiwa <= 0) {
            return Result.failure(IllegalArgumentException("Validasi Ditolak: Total Jiwa harus lebih dari 0."))
        }
        val sumVulnerable = lansia + ibuHamil + balita + disabilitas
        if (sumVulnerable > totalJiwa) {
            return Result.failure(
                IllegalArgumentException(
                    "VALIDASI DITOLAK: Jumlah Kelompok Rentan (Lansia $lansia + Ibu Hamil $ibuHamil + Balita $balita + Disabilitas $disabilitas = $sumVulnerable jiwa) TIDAK BOLEH melebihi Total Jiwa ($totalJiwa jiwa)!"
                )
            )
        }

        val currentList = _poskoUnits.value.toMutableList()
        val index = currentList.indexOfFirst { it.poskoId == poskoId }
        if (index == -1) {
            return Result.failure(IllegalArgumentException("Posko tidak ditemukan."))
        }

        val updated = currentList[index].copy(
            totalJiwa = totalJiwa,
            lansia = lansia,
            ibuHamil = ibuHamil,
            balita = balita,
            disabilitas = disabilitas,
            updatedAtIso = nowIsoString(),
            updatedByOfficer = officerName.ifBlank { "Petugas Posko" }
        )
        currentList[index] = updated
        _poskoUnits.value = currentList
        savePoskoUnits()
        return Result.success(updated)
    }

    /**
     * Update stok komoditas logistik & tanggal kedaluwarsa FEFO.
     */
    fun updateCommodityStock(
        code: String,
        newStock: Int,
        newExpiryDateIso: String? = null
    ) {
        val sanitizedStock = newStock.coerceAtLeast(0)
        _logisticStocks.value = _logisticStocks.value.map { item ->
            if (item.code == code) {
                item.copy(
                    currentStock = sanitizedStock,
                    expiryDateIso = newExpiryDateIso ?: item.expiryDateIso,
                    updatedAtIso = nowIsoString()
                )
            } else {
                item
            }
        }
        saveLogisticStocks()
    }

    fun addCustomCommodity(
        name: String,
        category: String,
        unit: String,
        initialStock: Int,
        expiryDateIso: String?,
        poskoCode: String
    ) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        val newCode = "LOG-${System.currentTimeMillis().toString().takeLast(5)}"
        val newItem = PoskoLogisticCommodity(
            code = newCode,
            name = cleanName,
            category = category,
            unit = unit.ifBlank { "Paket" },
            currentStock = initialStock.coerceAtLeast(0),
            minThreshold = 10,
            expiryDateIso = expiryDateIso?.takeIf { it.isNotBlank() },
            poskoCode = poskoCode,
            updatedAtIso = nowIsoString()
        )
        _logisticStocks.value = _logisticStocks.value + newItem
        saveLogisticStocks()
    }

    /**
     * Distribusi bantuan kepada warga/pengungsi dengan validasi NIK 16 Digit & Anti-Data Ganda.
     */
    fun distributeLogisticToNik(
        recipientNik: String,
        recipientNoKk: String,
        recipientName: String,
        vulnerabilityGroup: String,
        commodityCode: String,
        quantity: Int,
        poskoCode: String,
        officerName: String
    ): Result<NikAidDistributionRecord> {
        val cleanNik = recipientNik.trim()
        if (!Regex("^[0-9]{16}$").matches(cleanNik)) {
            return Result.failure(
                IllegalArgumentException("Validasi NIK Gagal: NIK Penerima wajib 16 digit angka murni sesuai standar Dukcapil.")
            )
        }
        if (recipientName.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Nama lengkap penerima bantuan wajib diisi.")
            )
        }
        if (quantity <= 0) {
            return Result.failure(
                IllegalArgumentException("Jumlah distribusi harus minimal 1 unit.")
            )
        }

        // Cek Anti-Data Ganda: Apakah NIK ini sudah pernah menerima komoditas yang sama?
        val duplicateRecord = _distributionLedger.value.firstOrNull {
            it.recipientNik == cleanNik && it.commodityCode == commodityCode
        }
        if (duplicateRecord != null) {
            return Result.failure(
                IllegalStateException(
                    "ANTI-DATA GANDA (NIK PK): NIK $cleanNik (${duplicateRecord.recipientName}) SUDAH MENERIMA ${duplicateRecord.commodityName} di ${duplicateRecord.poskoCode} pada ${duplicateRecord.timestampIso}!"
                )
            )
        }

        val commodity = _logisticStocks.value.firstOrNull { it.code == commodityCode }
            ?: return Result.failure(IllegalArgumentException("Komoditas logistik tidak ditemukan."))

        if (commodity.currentStock < quantity) {
            return Result.failure(
                IllegalStateException(
                    "Stok Tidak Cukup: Sisa stok ${commodity.name} hanya ${commodity.currentStock} ${commodity.unit}."
                )
            )
        }

        // Kurangi stok posko secara otomatis
        updateCommodityStock(
            code = commodityCode,
            newStock = commodity.currentStock - quantity
        )

        val record = NikAidDistributionRecord(
            distributionId = "DIST-${System.currentTimeMillis().toString().takeLast(6)}",
            recipientNik = cleanNik,
            recipientNoKk = recipientNoKk.trim(),
            recipientName = recipientName.trim(),
            vulnerabilityGroup = vulnerabilityGroup,
            commodityCode = commodity.code,
            commodityName = commodity.name,
            quantity = quantity,
            unit = commodity.unit,
            poskoCode = poskoCode,
            distributedByOfficer = officerName.ifBlank { "Petugas Posko" },
            timestampIso = nowIsoString(),
            isSyncedOfflineFirst = false
        )

        _distributionLedger.value = listOf(record) + _distributionLedger.value
        saveDistributionLedger()
        return Result.success(record)
    }

    /**
     * Mengambil data agregat live dari seluruh Posko Aktif untuk Auto-Populate ke DaLA.
     */
    fun getLiveAggregatedSnapshot(): PoskoDalaLockedSnapshot {
        val list = _poskoUnits.value
        return PoskoDalaLockedSnapshot(
            incidentId = list.firstOrNull()?.incidentId ?: "INC-202611-MM-0001",
            activePoskoCount = list.size,
            totalJiwa = list.sumOf { it.totalJiwa },
            lansia = list.sumOf { it.lansia },
            ibuHamil = list.sumOf { it.ibuHamil },
            balita = list.sumOf { it.balita },
            disabilitas = list.sumOf { it.disabilitas },
            lockedAtIso = "LIVE-FEED POSKO (Belum Dikunci)",
            lockedByOfficer = "-",
            isLocked = false
        )
    }

    /**
     * Mengunci Snapshot Data Posko agar laporan DaLA tidak berubah meskipun Posko mengupdate data.
     */
    fun captureAndLockDalaSnapshot(officerName: String): PoskoDalaLockedSnapshot {
        val live = getLiveAggregatedSnapshot()
        val locked = live.copy(
            lockedAtIso = nowIsoString(),
            lockedByOfficer = officerName.ifBlank { "Petugas DaLA" },
            isLocked = true
        )
        _dalaLockedSnapshot.value = locked
        runCatching {
            prefs?.edit()?.putString(KEY_DALA_SNAPSHOT, locked.toJsonObject().toString())?.apply()
        }
        return locked
    }

    private fun defaultPoskoSeed(): List<PoskoRefugeeUnit> = listOf(
        PoskoRefugeeUnit(
            poskoId = "posko_mm_01",
            poskoCode = "POSKO-MM-001",
            poskoName = "Posko Utama Koto Jaya (Kota Mukomuko)",
            district = "Kota Mukomuko",
            incidentId = "INC-202611-MM-0001",
            maxCapacity = 500,
            totalJiwa = 185,
            lansia = 28,
            ibuHamil = 9,
            balita = 34,
            disabilitas = 6,
            updatedAtIso = nowIsoString(),
            updatedByOfficer = "Operator Pusdalops"
        ),
        PoskoRefugeeUnit(
            poskoId = "posko_mm_02",
            poskoCode = "POSKO-MM-002",
            poskoName = "Posko Lapangan Pasar Bantal (Teramang Jaya)",
            district = "Teramang Jaya",
            incidentId = "INC-202611-MM-0001",
            maxCapacity = 350,
            totalJiwa = 112,
            lansia = 19,
            ibuHamil = 5,
            balita = 21,
            disabilitas = 4,
            updatedAtIso = nowIsoString(),
            updatedByOfficer = "Petugas Posko Teramang"
        ),
        PoskoRefugeeUnit(
            poskoId = "posko_mm_03",
            poskoCode = "POSKO-MM-003",
            poskoName = "Posko Shelter Tsunami Medan Jaya (Ipuh)",
            district = "Ipuh",
            incidentId = "INC-202611-MM-0001",
            maxCapacity = 400,
            totalJiwa = 94,
            lansia = 14,
            ibuHamil = 4,
            balita = 18,
            disabilitas = 3,
            updatedAtIso = nowIsoString(),
            updatedByOfficer = "Petugas Posko Ipuh"
        )
    )

    private fun defaultLogisticsSeed(): List<PoskoLogisticCommodity> = listOf(
        PoskoLogisticCommodity(
            code = "LOG-SEMBAKO",
            name = "Paket Sembako & Pangan Keluarga",
            category = "PANGAN",
            unit = "Paket",
            currentStock = 42,
            minThreshold = 10,
            expiryDateIso = "2027-04-15",
            poskoCode = "POSKO-MM-001",
            updatedAtIso = nowIsoString()
        ),
        PoskoLogisticCommodity(
            code = "LOG-TENDA",
            name = "Tenda Pengungsi Regu & Keluarga",
            category = "HUNIAN",
            unit = "Unit",
            currentStock = 6, // < 10 -> STATUS KRITIS (MERAH)
            minThreshold = 10,
            expiryDateIso = null,
            poskoCode = "POSKO-MM-001",
            updatedAtIso = nowIsoString()
        ),
        PoskoLogisticCommodity(
            code = "LOG-OBAT",
            name = "Paket Obat Darurat & Kotak P3K",
            category = "MEDIS",
            unit = "Kotak",
            currentStock = 8, // < 10 -> STATUS KRITIS (MERAH)
            minThreshold = 10,
            expiryDateIso = "2026-12-10", // <= 30 hari -> FEFO Warning
            poskoCode = "POSKO-MM-001",
            updatedAtIso = nowIsoString()
        ),
        PoskoLogisticCommodity(
            code = "LOG-AIR",
            name = "Air Bersih & Mineral Kemasan",
            category = "AIR_SANITASI",
            unit = "Galon",
            currentStock = 65,
            minThreshold = 10,
            expiryDateIso = "2027-08-20",
            poskoCode = "POSKO-MM-001",
            updatedAtIso = nowIsoString()
        )
    )

    private fun defaultDistributionSeed(): List<NikAidDistributionRecord> = listOf(
        NikAidDistributionRecord(
            distributionId = "DIST-890112",
            recipientNik = "1706021985030004",
            recipientNoKk = "1706020101100001",
            recipientName = "Rahmat Hidayat",
            vulnerabilityGroup = "Lansia",
            commodityCode = "LOG-SEMBAKO",
            commodityName = "Paket Sembako & Pangan Keluarga",
            quantity = 1,
            unit = "Paket",
            poskoCode = "POSKO-MM-001",
            distributedByOfficer = "Petugas Posko Koto Jaya",
            timestampIso = nowIsoString(),
            isSyncedOfflineFirst = true
        )
    )

    companion object {
        private const val PREFS_NAME = "sipagar_posko_logistics_offline_prefs"
        private const val KEY_POSKO_UNITS = "posko_refugee_units_json"
        private const val KEY_LOGISTIC_STOCKS = "posko_logistic_stocks_json"
        private const val KEY_DISTRIBUTION_LEDGER = "posko_nik_distribution_ledger_json"
        private const val KEY_DALA_SNAPSHOT = "posko_dala_locked_snapshot_json"

        @Volatile
        private var INSTANCE: PoskoLogisticsRepository? = null

        fun getInstance(context: Context): PoskoLogisticsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PoskoLogisticsRepository(
                    context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                ).also { INSTANCE = it }
            }
        }

        fun createForInMemoryTest(): PoskoLogisticsRepository {
            return PoskoLogisticsRepository(prefs = null)
        }
    }
}
