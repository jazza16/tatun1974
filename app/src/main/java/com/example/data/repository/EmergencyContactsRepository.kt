package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.api.EmergencyContactsApi
import com.example.data.api.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject

/**
 * Model dinamis untuk 1 item Kontak Instansi Darurat Publik.
 * Dikelola oleh server (API `GET /emergency-contacts`), bebas dari data pribadi/NIP personil ASN.
 */
data class PublicEmergencyContact(
    val id: String,
    val agencyName: String,
    val category: String,
    val phoneNumber: String,
    val displayPhone: String,
    val subtitle: String,
    val address: String = "",
    val iconKey: String = "emergency",
    val accentHex: String = "#EA580C",
    val isTollFree: Boolean = false,
    val priority: Int = 100
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("agencyName", agencyName)
        put("category", category)
        put("phoneNumber", phoneNumber)
        put("displayPhone", displayPhone)
        put("subtitle", subtitle)
        put("address", address)
        put("iconKey", iconKey)
        put("accentHex", accentHex)
        put("isTollFree", isTollFree)
        put("priority", priority)
    }

    companion object {
        fun fromJson(obj: JSONObject, fallbackIndex: Int = 0): PublicEmergencyContact? {
            val agencyName = obj.optString("agencyName").ifBlank { obj.optString("name") }.trim()
            val rawPhone = obj.optString("phoneNumber").ifBlank { obj.optString("phone") }.trim()
            val dialablePhone = rawPhone.filter { it.isDigit() || it == '+' }
            if (agencyName.isBlank() || dialablePhone.isBlank()) return null

            // Guard keamanan privasi: pastikan tidak ada entri personil ASN / NIP yang bocor ke direktori publik
            val category = obj.optString("category").ifBlank { "Darurat" }.trim()
            if (category.contains("Personil", ignoreCase = true) ||
                agencyName.contains("NIP.", ignoreCase = true) ||
                agencyName.contains("NIP ", ignoreCase = true)
            ) {
                return null
            }

            val displayPhone = obj.optString("displayPhone").ifBlank { rawPhone }.trim()
            return PublicEmergencyContact(
                id = obj.optString("id").ifBlank { "contact_${fallbackIndex}_$dialablePhone" },
                agencyName = agencyName,
                category = category,
                phoneNumber = dialablePhone,
                displayPhone = displayPhone,
                subtitle = obj.optString("subtitle").ifBlank { obj.optString("description") }.trim(),
                address = obj.optString("address").trim(),
                iconKey = obj.optString("iconKey").ifBlank { obj.optString("icon") }.ifBlank { "emergency" }.trim().lowercase(),
                accentHex = obj.optString("accentHex").ifBlank { "#EA580C" }.trim(),
                isTollFree = obj.optBoolean("isTollFree", false),
                priority = obj.optInt("priority", fallbackIndex + 1)
            )
        }
    }
}

enum class EmergencyContactsSource(val label: String) {
    REMOTE_API("Tersinkron Server API"),
    LOCAL_CACHE("Cache Lokal Terbaru"),
    DEFAULT_FALLBACK("Direktori Standar Darurat")
}

data class EmergencyContactsLoadResult(
    val contacts: List<PublicEmergencyContact>,
    val source: EmergencyContactsSource,
    val lastSyncedAtMs: Long = 0L
)

/**
 * Repository Dinamis untuk Tab "Kontak Penting" pada halaman Laporan Warga.
 *
 * Alur Pengambilan Data (Server-Driven):
 * 1. Mengambil JSON terbaru dari `GET /emergency-contacts` pada server API agar admin bisa
 *    menambah/mengubah nomor darurat publik kapan saja tanpa update aplikasi.
 * 2. Menyimpan respons sukses ke cache lokal (SharedPreferences) untuk akses instan saat offline.
 * 3. Jika offline & cache kosong, menampilkan 5 instansi darurat publik wajib (BPBD/117,
 *    Kepolisian/110, Damkar/113, Ambulans/RSUD, Basarnas/115).
 */
object EmergencyContactsRepository {

    private const val TAG = "EmergencyContactsRepo"
    private const val PREFS_NAME = "bpbd_public_emergency_contacts_cache"
    private const val KEY_CONTACTS_JSON = "cached_public_contacts_json"
    private const val KEY_SYNCED_AT = "cached_public_contacts_synced_at"
    private const val API_TIMEOUT_MS = 4_500L

    @Volatile
    var overrideApiForTesting: EmergencyContactsApi? = null

    @Volatile
    var overrideBaseUrlForTesting: String? = null

    fun resetForTesting() {
        overrideApiForTesting = null
        overrideBaseUrlForTesting = null
    }

    /**
     * Daftar fallback standar 5 instansi darurat publik utama (tanpa nama/NIP personil ASN).
     */
    fun getDefaultPublicContacts(): List<PublicEmergencyContact> = listOf(
        PublicEmergencyContact(
            id = "bpbd_pusdalops_117",
            agencyName = "BPBD / Pusdalops",
            category = "Bencana",
            phoneNumber = "117",
            displayPhone = "117",
            subtitle = "Call Center Bencana 24 Jam • Evakuasi Banjir, Longsor & Gempa",
            address = "Posko Pusdalops-PB BPBD Kabupaten Mukomuko",
            iconKey = "bpbd",
            accentHex = "#EA580C",
            isTollFree = true,
            priority = 1
        ),
        PublicEmergencyContact(
            id = "kepolisian_110",
            agencyName = "Kepolisian (Polres)",
            category = "Kepolisian",
            phoneNumber = "110",
            displayPhone = "110",
            subtitle = "Layanan Darurat Kepolisian 24 Jam • Keamanan & Lalu Lintas",
            address = "SPKT Polres Mukomuko, Bandar Ratu, Kota Mukomuko",
            iconKey = "police",
            accentHex = "#1D4ED8",
            isTollFree = true,
            priority = 2
        ),
        PublicEmergencyContact(
            id = "damkar_113",
            agencyName = "Pemadam Kebakaran (Damkar)",
            category = "Damkar",
            phoneNumber = "113",
            displayPhone = "113",
            subtitle = "Penanganan Kebakaran, Karhutla & Penyelamatan Darurat",
            address = "Dinas Pemadam Kebakaran & Penyelamatan Kab. Mukomuko",
            iconKey = "fire",
            accentHex = "#DC2626",
            isTollFree = true,
            priority = 3
        ),
        PublicEmergencyContact(
            id = "ambulans_rsud_119",
            agencyName = "Ambulans / RSUD",
            category = "Medis",
            phoneNumber = "119",
            displayPhone = "119",
            subtitle = "PSC 119 & IGD RSUD Mukomuko (0737) 71018 • Gawat Darurat Medis",
            address = "RSUD Kabupaten Mukomuko, Jl. Danau Nibung Km. 4",
            iconKey = "ambulance",
            accentHex = "#15803D",
            isTollFree = true,
            priority = 4
        ),
        PublicEmergencyContact(
            id = "basarnas_115",
            agencyName = "Basarnas",
            category = "SAR",
            phoneNumber = "115",
            displayPhone = "115",
            subtitle = "Pencarian & Pertolongan (SAR) Korban Hilang, Laut & Sungai",
            address = "Pos SAR Mukomuko / Kantor SAR Bengkulu",
            iconKey = "sar",
            accentHex = "#D97706",
            isTollFree = true,
            priority = 5
        )
    )

    fun getCachedOrDefault(context: Context): EmergencyContactsLoadResult {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cachedJson = prefs.getString(KEY_CONTACTS_JSON, null)
        val syncedAt = prefs.getLong(KEY_SYNCED_AT, 0L)
        if (!cachedJson.isNullOrBlank()) {
            val parsed = parseContactsPayload(cachedJson)
            if (parsed.isNotEmpty()) {
                return EmergencyContactsLoadResult(
                    contacts = parsed.sortedBy { it.priority },
                    source = EmergencyContactsSource.LOCAL_CACHE,
                    lastSyncedAtMs = syncedAt
                )
            }
        }
        return EmergencyContactsLoadResult(
            contacts = getDefaultPublicContacts(),
            source = EmergencyContactsSource.DEFAULT_FALLBACK,
            lastSyncedAtMs = 0L
        )
    }

    suspend fun fetchPublicContacts(context: Context): EmergencyContactsLoadResult = withContext(Dispatchers.IO) {
        val api = resolveApiClient()
        if (api != null) {
            try {
                val response = withTimeoutOrNull(API_TIMEOUT_MS) { api.getEmergencyContacts() }
                val rawBody = if (response != null && response.isSuccessful) {
                    response.body()?.string()
                } else {
                    null
                }
                if (!rawBody.isNullOrBlank()) {
                    val parsed = parseContactsPayload(rawBody)
                    if (parsed.isNotEmpty()) {
                        val sorted = parsed.sortedBy { it.priority }
                        val now = System.currentTimeMillis()
                        saveToCache(context, sorted, now)
                        return@withContext EmergencyContactsLoadResult(
                            contacts = sorted,
                            source = EmergencyContactsSource.REMOTE_API,
                            lastSyncedAtMs = now
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gagal sinkronisasi GET /emergency-contacts, memakai cache/fallback: ${e.message}")
            }
        }
        getCachedOrDefault(context)
    }

    fun parseContactsPayload(rawJson: String): List<PublicEmergencyContact> {
        val trimmed = rawJson.trim()
        if (trimmed.isBlank()) return emptyList()
        return try {
            val array = when {
                trimmed.startsWith("[") -> JSONArray(trimmed)
                trimmed.startsWith("{") -> {
                    val root = JSONObject(trimmed)
                    root.optJSONArray("contacts")
                        ?: root.optJSONArray("data")
                        ?: root.optJSONArray("items")
                        ?: JSONArray()
                }
                else -> JSONArray()
            }
            val list = mutableListOf<PublicEmergencyContact>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                PublicEmergencyContact.fromJson(obj, fallbackIndex = i)?.let { list.add(it) }
            }
            list
        } catch (e: Exception) {
            Log.w(TAG, "Format JSON emergency-contacts tidak valid: ${e.message}")
            emptyList()
        }
    }

    private fun saveToCache(context: Context, contacts: List<PublicEmergencyContact>, timestampMs: Long) {
        val array = JSONArray().apply { contacts.forEach { put(it.toJson()) } }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CONTACTS_JSON, array.toString())
            .putLong(KEY_SYNCED_AT, timestampMs)
            .apply()
    }

    private fun resolveApiClient(): EmergencyContactsApi? {
        overrideApiForTesting?.let { return it }
        val baseUrl = (overrideBaseUrlForTesting ?: BuildConfig.DOCUMENT_API_BASE_URL).trim()
        if (baseUrl.isBlank() || baseUrl.contains("YOUR_DOCUMENT_API_BASE_URL", ignoreCase = true)) {
            return null
        }
        if (!baseUrl.startsWith("https://", ignoreCase = true) && !baseUrl.startsWith("http://localhost", ignoreCase = true)) {
            return null
        }
        return NetworkClient.createEmergencyContactsApi(baseUrl, API_TIMEOUT_MS)
    }
}
