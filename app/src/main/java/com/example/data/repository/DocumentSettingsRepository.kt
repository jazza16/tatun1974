package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.api.DocumentTemplateApi
import com.example.data.api.NetworkClient
import com.example.data.local.DistribusiLogistikEntity
import com.example.data.local.FirestoreHelper
import com.example.data.local.LaporanHarianEntity
import com.example.data.local.LogKomunikasiEntity
import com.example.data.local.OfficerAuthManager
import com.example.data.local.OfficerProfile
import com.example.model.DocumentSettings
import com.example.model.RemoteOfficerRecord
import com.example.model.RemoteSignatoryConfig
import com.example.model.ResolvedSignatory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Identitas petugas hasil resolusi Data-Driven (API/Server -> Roster Tersinkronisasi -> Format Gelar).
 */
data class ResolvedDocumentOfficer(
    val formattedName: String,
    val nip: String,
    val nipLine: String,
    val role: String
)

/**
 * Hasil permintaan PDF dari server template jarak jauh (Backend Rendering).
 */
data class RemotePdfFetchResult(
    val pdfBytes: ByteArray,
    val source: String
)

/**
 * Repository Data-Driven untuk sistem pencetakan dokumen PDF BPBD Kabupaten Mukomuko.
 *
 * Arsitektur 3-Lapis:
 * 1. **Remote Template PDF (Utama saat Online)**: Meminta PDF jadi langsung dari Backend
 *    (`GET /reports/{reportId}/pdf` atau `POST /reports/render-pdf`) sehingga perubahan layout di server
 *    langsung berlaku ke seluruh petugas tanpa rilis ulang aplikasi.
 * 2. **Konfigurasi Dinamis (`GET /document-settings` & Firestore `app_settings/document_settings`)**:
 *    Saat PDF harus di-generate lokal (offline/backend belum terpasang), aplikasi membaca JSON konfigurasi
 *    terbaru (`fontSize`, `minFontSize`, `maxLengthBeforeTruncate`, `gelarFormat`, `signatories`, `officers`)
 *    dan menyimpannya di cache lokal untuk ketahanan offline.
 * 3. **Pemisahan Data & Tampilan**: Nama, gelar, jabatan, dan NIP petugas diambil dari API/Server
 *    (atau roster tersinkronisasi), bukan di-hardcode di kelas renderer PDF.
 */
object DocumentSettingsRepository {

    private const val TAG = "DocumentSettingsRepo"
    private const val PREFS_NAME = "bpbd_document_settings_cache"
    private const val KEY_SETTINGS_JSON = "cached_document_settings_json"
    private const val KEY_LAST_SYNCED_AT = "cached_document_settings_synced_at"
    private const val SETTINGS_FETCH_TIMEOUT_MS = 4_000L

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    @Volatile
    private var inMemorySettings: DocumentSettings? = null

    /**
     * Hook opsional untuk injeksi API mock/stub pada pengujian unit (Robolectric/JUnit).
     */
    @Volatile
    var overrideApiForTesting: DocumentTemplateApi? = null

    /**
     * Hook opsional untuk menonaktifkan panggilan Firestore selama unit test JVM terisolasi.
     */
    @Volatile
    var disableFirestoreForTesting: Boolean = false

    /**
     * Hook opsional untuk menimpa base URL selama unit test.
     */
    @Volatile
    var overrideBaseUrlForTesting: String? = null

    fun resetForTesting() {
        inMemorySettings = null
        overrideApiForTesting = null
        disableFirestoreForTesting = false
        overrideBaseUrlForTesting = null
    }

    /**
     * Memvalidasi bahwa byte array benar-benar berkas PDF valid (memiliki magic header `%PDF-`).
     */
    fun isValidPdfBytes(bytes: ByteArray?): Boolean {
        if (bytes == null || bytes.size < 16) return false
        return bytes[0] == '%'.code.toByte() &&
            bytes[1] == 'P'.code.toByte() &&
            bytes[2] == 'D'.code.toByte() &&
            bytes[3] == 'F'.code.toByte() &&
            bytes[4] == '-'.code.toByte()
    }

    fun getConfiguredBaseUrl(): String? {
        val raw = (overrideBaseUrlForTesting ?: BuildConfig.DOCUMENT_API_BASE_URL).trim()
        if (raw.isBlank()) return null
        if (raw.contains("YOUR_DOCUMENT_API_BASE_URL", ignoreCase = true)) return null
        if (!raw.startsWith("https://", ignoreCase = true) && !raw.startsWith("http://localhost", ignoreCase = true)) {
            return null
        }
        return raw.trimEnd('/')
    }

    private fun resolveApiClient(timeoutMs: Long = SETTINGS_FETCH_TIMEOUT_MS): DocumentTemplateApi? {
        overrideApiForTesting?.let { return it }
        val baseUrl = getConfiguredBaseUrl() ?: return null
        return try {
            NetworkClient.createDocumentTemplateApi(baseUrl = baseUrl, timeoutMs = timeoutMs)
        } catch (e: Exception) {
            Log.w(TAG, "Gagal membuat DocumentTemplateApi untuk $baseUrl: ${e.message}")
            null
        }
    }

    /**
     * Mengambil konfigurasi dokumen terbaru secara asynchronous setiap kali mencetak dokumen:
     * 1. REST `GET /document-settings` (+ `GET /officers` bila daftar officers belum tertanam).
     * 2. Fallback Cloud Firestore `app_settings/document_settings` (+ `officers`).
     * 3. Fallback Cache Lokal (SharedPreferences) untuk operasi lapangan 100% offline.
     * 4. Default aman berbasis roster tersinkronisasi bila perangkat belum pernah online.
     */
    suspend fun fetchDocumentSettings(context: Context): DocumentSettings = withContext(Dispatchers.IO) {
        val cachedBefore = loadCachedSettings(context)

        // 1. Coba endpoint REST `GET /document-settings`
        val api = resolveApiClient(SETTINGS_FETCH_TIMEOUT_MS)
        if (api != null) {
            val fromApi = withTimeoutOrNull(SETTINGS_FETCH_TIMEOUT_MS) {
                try {
                    val response = api.getDocumentSettings()
                    if (response.isSuccessful) {
                        val bodyStr = response.body()?.string().orEmpty()
                        var parsed = DocumentSettings.fromJson(bodyStr, source = DocumentSettings.SOURCE_API)
                        if (parsed != null && parsed.officers.isEmpty()) {
                            // Tarik data Nama & Gelar Petugas murni dari endpoint `GET /officers`
                            val officersFromApi = fetchOfficersFromApiSafe(api)
                            if (officersFromApi.isNotEmpty()) {
                                parsed = parsed.copy(officers = officersFromApi)
                            }
                        }
                        parsed
                    } else {
                        Log.w(TAG, "GET /document-settings HTTP ${response.code()}")
                        null
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "GET /document-settings gagal (fallback ke Firestore/Cache): ${e.message}")
                    null
                }
            }
            if (fromApi != null) {
                val merged = mergeWithRosterIfOfficersEmpty(context, fromApi)
                saveCachedSettings(context, merged)
                inMemorySettings = merged
                return@withContext merged
            }
        }

        // 2. Fallback ke Firestore `app_settings/document_settings` & koleksi `officers`
        if (!disableFirestoreForTesting) {
            val fromFirestore = withTimeoutOrNull(3_000L) {
                try {
                    val docMap = FirestoreHelper.fetchDocumentSettingsFromCloudSuspend().getOrNull()
                    val parsed = docMap?.let { DocumentSettings.fromFirestoreMap(it) }
                    val cloudOfficers = FirestoreHelper.fetchOfficersDirectoryFromCloudSuspend()
                        .getOrNull()
                        .orEmpty()
                        .map { it.toRemoteOfficerRecord() }

                    when {
                        parsed != null && parsed.officers.isEmpty() && cloudOfficers.isNotEmpty() ->
                            parsed.copy(officers = cloudOfficers)
                        parsed != null -> parsed
                        cloudOfficers.isNotEmpty() -> (cachedBefore ?: DocumentSettings())
                            .copy(officers = cloudOfficers, source = DocumentSettings.SOURCE_FIRESTORE)
                        else -> null
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Firestore document_settings tidak tersedia (offline): ${e.message}")
                    null
                }
            }
            if (fromFirestore != null) {
                val merged = mergeWithRosterIfOfficersEmpty(context, fromFirestore)
                saveCachedSettings(context, merged)
                inMemorySettings = merged
                return@withContext merged
            }
        }

        // 3. Fallback ke cache lokal perangkat (offline) atau konfigurasi default tersinkronisasi
        val resolvedOffline = cachedBefore?.copy(source = DocumentSettings.SOURCE_CACHE)
            ?: buildDefaultSettingsFromSyncedRoster(context)
        inMemorySettings = resolvedOffline
        resolvedOffline
    }

    /**
     * Pembaca sinkron untuk metode Canvas yang tidak suspend (mis. `ReportExporter.drawKopSuratResmi`).
     * Mengembalikan cache di memori / SharedPreferences terbaru, atau default berbasis roster tersinkronisasi.
     */
    fun getCachedOrDefaultSettings(context: Context?): DocumentSettings {
        inMemorySettings?.let { return it }
        if (context == null) return DocumentSettings()
        val cached = loadCachedSettings(context)
        val resolved = cached ?: buildDefaultSettingsFromSyncedRoster(context)
        inMemorySettings = resolved
        return resolved
    }

    /**
     * Meminta file PDF yang sudah di-render langsung oleh Backend (Remote Template PDF)
     * berdasarkan ID laporan (`GET /reports/{reportId}/pdf`) atau payload laporan (`POST /reports/render-pdf`).
     *
     * Mengembalikan `null` bila fitur remote dinonaktifkan, URL backend belum dikonfigurasi,
     * perangkat offline, atau respons bukan file PDF yang valid -> memicu fallback lokal yang mulus.
     */
    suspend fun fetchRemotePdfIfAvailable(
        context: Context,
        laporan: LaporanHarianEntity,
        listLog: List<LogKomunikasiEntity>,
        listLogistik: List<DistribusiLogistikEntity>,
        settings: DocumentSettings
    ): RemotePdfFetchResult? = withContext(Dispatchers.IO) {
        if (!settings.useRemotePdfTemplate) return@withContext null
        val api = resolveApiClient(settings.remotePdfTimeoutMs) ?: return@withContext null

        withTimeoutOrNull(settings.remotePdfTimeoutMs) {
            // 1. Jika laporan sudah punya ID persisted, coba GET /reports/{reportId}/pdf lebih dulu
            if (laporan.id > 0) {
                try {
                    val getResp = api.downloadReportPdf(laporan.id.toString())
                    if (getResp.isSuccessful) {
                        val bytes = getResp.body()?.bytes()
                        if (isValidPdfBytes(bytes)) {
                            Log.i(TAG, "Remote PDF berhasil diunduh via GET /reports/${laporan.id}/pdf (${bytes!!.size} bytes)")
                            return@withTimeoutOrNull RemotePdfFetchResult(
                                pdfBytes = bytes,
                                source = "REMOTE_GET_BY_ID"
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "GET /reports/${laporan.id}/pdf gagal, mencoba POST /reports/render-pdf: ${e.message}")
                }
            }

            // 2. Coba POST /reports/render-pdf dengan payload lengkap laporan agar draft baru juga memakai template server
            try {
                val payloadJson = buildRemoteRenderPayloadJson(
                    context = context,
                    laporan = laporan,
                    listLog = listLog,
                    listLogistik = listLogistik,
                    settings = settings
                )
                val body = payloadJson.toRequestBody(JSON_MEDIA_TYPE)
                val postResp = api.renderReportPdf(body)
                if (postResp.isSuccessful) {
                    val bytes = postResp.body()?.bytes()
                    if (isValidPdfBytes(bytes)) {
                        Log.i(TAG, "Remote PDF berhasil dirender via POST /reports/render-pdf (${bytes!!.size} bytes)")
                        return@withTimeoutOrNull RemotePdfFetchResult(
                            pdfBytes = bytes,
                            source = "REMOTE_POST_RENDER"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "POST /reports/render-pdf gagal (fallback ke generator lokal): ${e.message}")
            }
            null
        }
    }

    /**
     * Menyelesaikan identitas petugas (Nama + Gelar terformat sesuai `DocumentSettings.gelarFormat` & NIP)
     * murni dari data server (`settings.officers`), fallback ke roster tersinkronisasi `OfficerAuthManager`.
     */
    fun resolveOfficerIdentity(
        context: Context?,
        rawName: String = "",
        rawNip: String = "",
        settings: DocumentSettings
    ): ResolvedDocumentOfficer {
        val cleanRawName = rawName.takeUnless { it.startsWith("enc_v1:") }?.trim().orEmpty()
        val cleanRawNip = rawNip.trim()

        // 1. Cari di daftar petugas hasil API/Server (`settings.officers`) terlebih dahulu
        val serverMatch = findOfficerInServerList(
            officers = settings.officers,
            rawName = cleanRawName,
            rawNip = cleanRawNip
        )
        if (serverMatch != null) {
            val formatted = if (serverMatch.gelarDepan.isNotBlank() || serverMatch.gelarBelakang.isNotBlank()) {
                settings.formatNameWithGelar(
                    nama = serverMatch.nama,
                    gelarDepan = serverMatch.gelarDepan,
                    gelarBelakang = serverMatch.gelarBelakang
                )
            } else {
                settings.formatRawFullName(serverMatch.fullName.ifBlank { serverMatch.nama })
            }
            return ResolvedDocumentOfficer(
                formattedName = formatted,
                nip = serverMatch.nip.ifBlank { "-" },
                nipLine = settings.formatNipLine(serverMatch.nip),
                role = serverMatch.role
            )
        }

        // 2. Fallback ke roster lokal tersinkronisasi (OfficerAuthManager) agar tetap akurat saat offline
        val synced = context?.let {
            OfficerAuthManager(it).resolveSyncedOfficer(rawName = cleanRawName, rawNip = cleanRawNip)
        }
        val finalRawName = synced?.fullName?.takeIf { it.isNotBlank() }
            ?: cleanRawName.ifBlank { "-" }
        val finalNip = synced?.nip?.takeIf { it.isNotBlank() }
            ?: cleanRawNip.ifBlank { "-" }

        return ResolvedDocumentOfficer(
            formattedName = settings.formatRawFullName(finalRawName),
            nip = finalNip,
            nipLine = settings.formatNipLine(finalNip),
            role = synced?.role.orEmpty()
        )
    }

    /**
     * Menyelesaikan 3 penanda tangan Tata Naskah Dinas (Koord. Administrasi, Manajer Pusdalops, Pengawas)
     * secara data-driven tanpa hardcode nama pejabat di dalam renderer PDF.
     *
     * Prioritas resolusi untuk setiap slot:
     * 1. Konfigurasi eksplisit pada `settings.signatories` dari `GET /document-settings`.
     * 2. Personil yang cocok jabatannya pada `settings.officers` dari `GET /officers`.
     * 3. Roster tersinkronisasi dari `OfficerAuthManager` berdasarkan kata kunci jabatan.
     */
    fun resolveSignatories(
        context: Context?,
        settings: DocumentSettings
    ): Map<String, ResolvedSignatory> {
        val slotSpecs = listOf(
            Triple(DocumentSettings.SLOT_KOORD_ADMIN, "Koord", Pair("Koord. Administrasi", "Pusdalops-PB")),
            Triple(DocumentSettings.SLOT_MANAJER_PUSDALOPS, "Manajer", Pair("Manajer Pusdalops PB", "BPBD Mukomuko")),
            Triple(DocumentSettings.SLOT_PENGAWAS, "Pengawas", Pair("Pengawas", "Kepala Pelaksana BPBD Mukomuko"))
        )

        val officerAuth = context?.let { OfficerAuthManager(it) }
        val result = LinkedHashMap<String, ResolvedSignatory>()

        for ((slotKey, roleKeyword, defaultRoleLines) in slotSpecs) {
            val remoteSig: RemoteSignatoryConfig? = settings.signatories.firstOrNull {
                it.slotKey.equals(slotKey, ignoreCase = true)
            }
            val roleLine1 = remoteSig?.roleLine1?.takeIf { it.isNotBlank() } ?: defaultRoleLines.first
            val roleLine2 = remoteSig?.roleLine2?.takeIf { it.isNotBlank() } ?: defaultRoleLines.second

            // Prioritas 1: Nama/Gelar eksplisit dari JSON `signatories` server
            if (remoteSig != null && remoteSig.nama.isNotBlank()) {
                val formattedName = settings.formatNameWithGelar(
                    nama = remoteSig.nama,
                    gelarDepan = remoteSig.gelarDepan,
                    gelarBelakang = remoteSig.gelarBelakang
                )
                val nip = remoteSig.nip.ifBlank { "-" }
                result[slotKey] = ResolvedSignatory(
                    slotKey = slotKey,
                    roleLine1 = roleLine1,
                    roleLine2 = roleLine2,
                    formattedName = formattedName,
                    nip = nip,
                    nipLine = settings.formatNipLine(nip)
                )
                continue
            }

            // Prioritas 2: Cari berdasarkan jabatan pada daftar `settings.officers` dari server
            val serverOfficerByRole = settings.officers.firstOrNull {
                it.role.contains(roleKeyword, ignoreCase = true)
            }
            if (serverOfficerByRole != null) {
                val formattedName = if (serverOfficerByRole.gelarDepan.isNotBlank() || serverOfficerByRole.gelarBelakang.isNotBlank()) {
                    settings.formatNameWithGelar(
                        nama = serverOfficerByRole.nama,
                        gelarDepan = serverOfficerByRole.gelarDepan,
                        gelarBelakang = serverOfficerByRole.gelarBelakang
                    )
                } else {
                    settings.formatRawFullName(serverOfficerByRole.fullName.ifBlank { serverOfficerByRole.nama })
                }
                val nip = serverOfficerByRole.nip.ifBlank { "-" }
                result[slotKey] = ResolvedSignatory(
                    slotKey = slotKey,
                    roleLine1 = roleLine1,
                    roleLine2 = roleLine2,
                    formattedName = formattedName,
                    nip = nip,
                    nipLine = settings.formatNipLine(nip)
                )
                continue
            }

            // Prioritas 3: Roster tersinkronisasi lokal (offline fallback tanpa hardcode di PdfGenerator/ReportExporter)
            val rosterMatch = officerAuth?.getOfficerRoster()?.firstOrNull {
                it.role.contains(roleKeyword, ignoreCase = true)
            }
            val formattedName = settings.formatRawFullName(rosterMatch?.fullName ?: "-")
            val nip = rosterMatch?.nip?.ifBlank { "-" } ?: "-"
            result[slotKey] = ResolvedSignatory(
                slotKey = slotKey,
                roleLine1 = roleLine1,
                roleLine2 = roleLine2,
                formattedName = formattedName,
                nip = nip,
                nipLine = settings.formatNipLine(nip)
            )
        }
        return result
    }

    /**
     * Menyimpan cache konfigurasi JSON secara eksplisit (berguna untuk sinkronisasi latar belakang atau pengujian).
     */
    fun saveCachedSettings(context: Context, settings: DocumentSettings) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_SETTINGS_JSON, settings.clamped().toJson())
                .putLong(KEY_LAST_SYNCED_AT, System.currentTimeMillis())
                .apply()
            inMemorySettings = settings.clamped()
        } catch (e: Exception) {
            Log.w(TAG, "Gagal menyimpan cache DocumentSettings: ${e.message}")
        }
    }

    fun loadCachedSettings(context: Context): DocumentSettings? {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_SETTINGS_JSON, null) ?: return null
            DocumentSettings.fromJson(json, source = DocumentSettings.SOURCE_CACHE)
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun fetchOfficersFromApiSafe(api: DocumentTemplateApi): List<RemoteOfficerRecord> {
        return try {
            val resp = api.getOfficers()
            if (resp.isSuccessful) {
                DocumentSettings.parseOfficersJson(resp.body()?.string().orEmpty())
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun mergeWithRosterIfOfficersEmpty(
        context: Context,
        settings: DocumentSettings
    ): DocumentSettings {
        if (settings.officers.isNotEmpty()) return settings.clamped()
        val rosterRecords = try {
            OfficerAuthManager(context).getOfficerRoster().map { it.toRemoteOfficerRecord() }
        } catch (_: Exception) {
            emptyList()
        }
        return settings.copy(officers = rosterRecords).clamped()
    }

    private fun buildDefaultSettingsFromSyncedRoster(context: Context): DocumentSettings {
        val rosterRecords = try {
            OfficerAuthManager(context).getOfficerRoster().map { it.toRemoteOfficerRecord() }
        } catch (_: Exception) {
            emptyList()
        }
        return DocumentSettings(
            officers = rosterRecords,
            source = DocumentSettings.SOURCE_DEFAULT
        ).clamped()
    }

    private fun findOfficerInServerList(
        officers: List<RemoteOfficerRecord>,
        rawName: String,
        rawNip: String
    ): RemoteOfficerRecord? {
        if (officers.isEmpty()) return null
        if (rawNip.isNotBlank() && rawNip != "-") {
            officers.firstOrNull { it.nip.equals(rawNip, ignoreCase = true) }?.let { return it }
        }
        if (rawName.isNotBlank() && rawName != "-") {
            officers.firstOrNull {
                it.fullName.equals(rawName, ignoreCase = true) ||
                    it.nama.equals(rawName, ignoreCase = true)
            }?.let { return it }

            val base = rawName.substringBefore(",").trim()
            if (base.length >= 4) {
                officers.firstOrNull {
                    it.nama.substringBefore(",").trim().equals(base, ignoreCase = true) ||
                        it.fullName.substringBefore(",").trim().equals(base, ignoreCase = true)
                }?.let { return it }
            }
        }
        return null
    }

    private fun OfficerProfile.toRemoteOfficerRecord(): RemoteOfficerRecord {
        return RemoteOfficerRecord(
            id = id,
            nama = fullName,
            fullName = fullName,
            nip = nip,
            role = role,
            unit = unit,
            shift = shift,
            phone = phone
        )
    }

    private fun buildRemoteRenderPayloadJson(
        context: Context,
        laporan: LaporanHarianEntity,
        listLog: List<LogKomunikasiEntity>,
        listLogistik: List<DistribusiLogistikEntity>,
        settings: DocumentSettings
    ): String {
        val operator = resolveOfficerIdentity(
            context = context,
            rawName = laporan.operatorName,
            settings = settings
        )
        val root = JSONObject().apply {
            put("reportId", laporan.id)
            put("settingsVersion", settings.version)
            put("gelarFormat", settings.gelarFormat)
            put("laporan", JSONObject().apply {
                put("jenisBencana", laporan.jenisBencana)
                put("tanggalKejadian", laporan.tanggalKejadian)
                put("waktuKejadian", laporan.waktuKejadian)
                put("lokasiBencana", laporan.lokasiBencana)
                put("koordinat", laporan.koordinat)
                put("cuacaSaatKejadian", laporan.cuacaSaatKejadian.ifBlank { laporan.kondisiCuaca })
                put("kedalaman", laporan.kedalaman)
                put("cakupanDampak", laporan.cakupanDampak)
                put("upayaDilakukan", laporan.upayaDilakukan)
                put("kondisiMutakhir", laporan.kondisiMutakhir)
                put("korbanLuka", laporan.korbanLuka)
                put("korbanMeninggal", laporan.korbanMeninggal)
                put("kerusakan", laporan.kerusakan)
                put("penyebab", laporan.penyebab)
                put("sumberInformasi", laporan.sumberInformasi)
                put("stationId", laporan.stationId)
                put("tanggalSurat", laporan.tanggalSurat)
                put("tanggalTerima", laporan.tanggalTerima)
                put("operatorName", operator.formattedName)
                put("operatorNip", operator.nip)
            })
            val logsArr = JSONArray()
            listLog.forEach { log ->
                logsArr.put(JSONObject().apply {
                    put("waktu", log.waktu)
                    put("stationIdFrom", log.stationIdFrom)
                    put("stationIdTo", log.stationIdTo)
                    put("subjek", log.subjek)
                })
            }
            put("komunikasi", logsArr)

            val logistikArr = JSONArray()
            listLogistik.forEach { item ->
                logistikArr.put(JSONObject().apply {
                    put("namaBarang", item.namaBarang)
                    put("jumlah", item.jumlah)
                    put("satuan", item.satuan)
                    put("keterangan", item.keterangan)
                })
            }
            put("logistik", logistikArr)
        }
        return root.toString()
    }
}
