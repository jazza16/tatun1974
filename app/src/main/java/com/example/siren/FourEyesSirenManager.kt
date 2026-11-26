package com.example.siren

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.Keep
import com.example.model.MukomukoDistricts
import com.example.security.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Alur state resmi Sirene Otoritas Berlapis (Four-Eyes Principle) sesuai Master PRD BAB 4:
 * `DRAFT -> MENUNGGU_PERSETUJUAN -> DIAKTIFKAN` (atau `DITOLAK` / `DINONAKTIFKAN`).
 */
enum class FourEyesSirenState(val displayLabel: String) {
    DRAFT("DRAFT"),
    MENUNGGU_PERSETUJUAN("MENUNGGU PERSETUJUAN"),
    DIAKTIFKAN("DIAKTIFKAN"),
    DITOLAK("DITOLAK"),
    DINONAKTIFKAN("DINONAKTIFKAN");

    companion object {
        fun fromName(raw: String?): FourEyesSirenState {
            return entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: DRAFT
        }
    }
}

@Keep
data class FourEyesSirenDraft(
    val draftId: String = "SRN-${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(4).uppercase()}",
    val alertType: SirenAlertType = SirenAlertType.TSUNAMI,
    val title: String,
    val message: String,
    val targetDistricts: List<String>,
    val isCoastalPriority: Boolean = false,
    val status: FourEyesSirenState = FourEyesSirenState.DRAFT,
    val createdByNip: String,
    val createdByName: String,
    val createdByRole: String,
    val createdAtMs: Long = System.currentTimeMillis(),
    val submittedAtMs: Long? = null,
    val approvedByNip: String? = null,
    val approvedByName: String? = null,
    val approvedByRole: String? = null,
    val decisionAtMs: Long? = null,
    val decisionNote: String = ""
) {
    fun toJsonObject(): JSONObject = JSONObject().apply {
        put("draftId", draftId)
        put("alertType", alertType.name)
        put("title", title)
        put("message", message)
        put("targetDistricts", JSONArray(targetDistricts))
        put("isCoastalPriority", isCoastalPriority)
        put("status", status.name)
        put("createdByNip", createdByNip)
        put("createdByName", createdByName)
        put("createdByRole", createdByRole)
        put("createdAtMs", createdAtMs)
        if (submittedAtMs != null) put("submittedAtMs", submittedAtMs)
        if (approvedByNip != null) put("approvedByNip", approvedByNip)
        if (approvedByName != null) put("approvedByName", approvedByName)
        if (approvedByRole != null) put("approvedByRole", approvedByRole)
        if (decisionAtMs != null) put("decisionAtMs", decisionAtMs)
        put("decisionNote", decisionNote)
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): FourEyesSirenDraft {
            val districtsArr = obj.optJSONArray("targetDistricts")
            val districts = buildList {
                if (districtsArr != null) {
                    for (i in 0 until districtsArr.length()) {
                        val v = districtsArr.optString(i, "").trim()
                        if (v.isNotBlank()) add(v)
                    }
                }
            }
            val alertType = runCatching {
                SirenAlertType.valueOf(obj.optString("alertType", SirenAlertType.TSUNAMI.name))
            }.getOrDefault(SirenAlertType.TSUNAMI)
            return FourEyesSirenDraft(
                draftId = obj.optString("draftId", ""),
                alertType = alertType,
                title = obj.optString("title", ""),
                message = obj.optString("message", ""),
                targetDistricts = districts,
                isCoastalPriority = obj.optBoolean("isCoastalPriority", false),
                status = FourEyesSirenState.fromName(obj.optString("status")),
                createdByNip = obj.optString("createdByNip", ""),
                createdByName = obj.optString("createdByName", ""),
                createdByRole = obj.optString("createdByRole", ""),
                createdAtMs = obj.optLong("createdAtMs", System.currentTimeMillis()),
                submittedAtMs = if (obj.has("submittedAtMs")) obj.optLong("submittedAtMs") else null,
                approvedByNip = obj.optString("approvedByNip", "").takeIf { it.isNotBlank() },
                approvedByName = obj.optString("approvedByName", "").takeIf { it.isNotBlank() },
                approvedByRole = obj.optString("approvedByRole", "").takeIf { it.isNotBlank() },
                decisionAtMs = if (obj.has("decisionAtMs")) obj.optLong("decisionAtMs") else null,
                decisionNote = obj.optString("decisionNote", "")
            )
        }
    }
}

/**
 * Pengelola Sirene Otoritas Berlapis (*Four-Eyes Principle*) BPBD Kabupaten Mukomuko.
 *
 * Aturan Keamanan Ketat (BAB 4 Master PRD):
 * 1. Pembuatan draf (`DRAFT`) dan pengajuan (`MENUNGGU_PERSETUJUAN`) dilakukan oleh Operator Pusdalops / petugas berwenang.
 * 2. Persetujuan & Aktivasi (`DIAKTIFKAN`) HANYA boleh dilakukan oleh Pimpinan berwenang
 *    (`Kepala Pusdalops / Manajer` atau `Kalaksa / Pengawas / Supervisor`).
 * 3. **Penolakan Otomatis Self-Approval**: Pembuat draf DILARANG menyetujui drafnya sendiri
 *    (NIP penyetuju wajib berbeda dari NIP pembuat draf).
 */
class FourEyesSirenManager private constructor(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _drafts = MutableStateFlow<List<FourEyesSirenDraft>>(loadDraftsFromPrefs())
    val drafts: StateFlow<List<FourEyesSirenDraft>> = _drafts.asStateFlow()

    fun createDraft(
        alertType: SirenAlertType,
        title: String,
        message: String,
        targetDistricts: List<String>,
        creatorNip: String,
        creatorName: String,
        creatorRole: String,
        autoSubmitForApproval: Boolean = false
    ): Result<FourEyesSirenDraft> {
        val cleanNip = normalizeNip(creatorNip)
        if (cleanNip.isBlank()) {
            return Result.failure(SecurityException("Identitas NIP petugas pembuat draf tidak valid."))
        }
        if (!UserRole.canCreateSirenDraft(creatorRole)) {
            return Result.failure(
                SecurityException("Peran '$creatorRole' tidak memiliki wewenang membuat draf aktivasi sirene.")
            )
        }
        val cleanTitle = title.trim()
        val cleanMessage = message.trim()
        if (cleanTitle.isBlank() || cleanMessage.isBlank()) {
            return Result.failure(IllegalArgumentException("Judul dan pesan peringatan sirene wajib diisi."))
        }
        val resolvedDistricts = targetDistricts.map { it.trim() }.filter { it.isNotBlank() }.distinct()
            .ifEmpty {
                if (alertType == SirenAlertType.TSUNAMI) {
                    MukomukoDistricts.coastalTsunamiDistricts.map { it.name }
                } else {
                    listOf("Seluruh Kabupaten Mukomuko")
                }
            }
        val now = System.currentTimeMillis()
        val hasCoastal = resolvedDistricts.any { MukomukoDistricts.isCoastalTsunamiZone(it) }
        val draft = FourEyesSirenDraft(
            alertType = alertType,
            title = cleanTitle.take(120),
            message = cleanMessage.take(500),
            targetDistricts = resolvedDistricts,
            isCoastalPriority = alertType == SirenAlertType.TSUNAMI || hasCoastal,
            status = if (autoSubmitForApproval) FourEyesSirenState.MENUNGGU_PERSETUJUAN else FourEyesSirenState.DRAFT,
            createdByNip = cleanNip,
            createdByName = creatorName.trim().ifBlank { "Petugas Pusdalops" },
            createdByRole = creatorRole.trim().ifBlank { "Operator Pusdalops" },
            createdAtMs = now,
            submittedAtMs = if (autoSubmitForApproval) now else null
        )
        val updated = listOf(draft) + _drafts.value
        persistDrafts(updated)
        return Result.success(draft)
    }

    fun submitForApproval(draftId: String, actorNip: String): Result<FourEyesSirenDraft> {
        val cleanActor = normalizeNip(actorNip)
        val current = _drafts.value.firstOrNull { it.draftId == draftId }
            ?: return Result.failure(IllegalArgumentException("Draf sirene tidak ditemukan."))
        if (current.status != FourEyesSirenState.DRAFT) {
            return Result.failure(
                IllegalStateException("Hanya draf berstatus DRAFT yang dapat diajukan ke MENUNGGU PERSETUJUAN.")
            )
        }
        if (cleanActor.isBlank()) {
            return Result.failure(SecurityException("Identitas NIP petugas wajib valid."))
        }
        val updatedDraft = current.copy(
            status = FourEyesSirenState.MENUNGGU_PERSETUJUAN,
            submittedAtMs = System.currentTimeMillis()
        )
        replaceDraft(updatedDraft)
        return Result.success(updatedDraft)
    }

    /**
     * Menyetujui & mengaktifkan draf sirene dengan validasi Four-Eyes Principle ketat.
     */
    fun approveAndActivate(
        draftId: String,
        approverNip: String,
        approverName: String,
        approverRole: String,
        note: String = ""
    ): Result<FourEyesSirenDraft> {
        val cleanApproverNip = normalizeNip(approverNip)
        if (cleanApproverNip.isBlank()) {
            return Result.failure(SecurityException("NIP pejabat penyetuju wajib valid."))
        }
        val current = _drafts.value.firstOrNull { it.draftId == draftId }
            ?: return Result.failure(IllegalArgumentException("Draf sirene tidak ditemukan."))

        if (current.status != FourEyesSirenState.MENUNGGU_PERSETUJUAN) {
            return Result.failure(
                IllegalStateException(
                    "Aktivasi ditolak: status draf harus MENUNGGU PERSETUJUAN (status saat ini: ${current.status.displayLabel})."
                )
            )
        }

        // GUARD 1: FOUR-EYES PRINCIPLE — Pembuat draf DILARANG menyetujui drafnya sendiri!
        if (normalizeNip(current.createdByNip).equals(cleanApproverNip, ignoreCase = true)) {
            return Result.failure(
                SecurityException(
                    "DITOLAK (Four-Eyes Principle): Pembuat draf (${current.createdByName}) tidak diperbolehkan menyetujui dan mengaktifkan draf sirenenya sendiri. Wajib disetujui pejabat berwenang lain (Kepala Pusdalops / Kalaksa)."
                )
            )
        }

        // GUARD 2: Otoritas Jabatan Penyetuju (Kepala Pusdalops / Manajer / Kalaksa / Pengawas)
        if (!UserRole.canApproveSirenActivation(approverRole)) {
            return Result.failure(
                SecurityException(
                    "DITOLAK (Otoritas Tidak Cukup): Peran '$approverRole' tidak berwenang mengaktifkan sirene. Wajib disetujui Kepala Pusdalops / Manajer atau Kalaksa / Pengawas."
                )
            )
        }

        val activated = current.copy(
            status = FourEyesSirenState.DIAKTIFKAN,
            approvedByNip = cleanApproverNip,
            approvedByName = approverName.trim().ifBlank { "Pimpinan BPBD" },
            approvedByRole = approverRole.trim(),
            decisionAtMs = System.currentTimeMillis(),
            decisionNote = note.trim().ifBlank { "Disetujui sesuai protokol Four-Eyes Principle BPBD Mukomuko." }
        )
        replaceDraft(activated)
        return Result.success(activated)
    }

    fun rejectDraft(
        draftId: String,
        reviewerNip: String,
        reviewerName: String,
        reviewerRole: String,
        reason: String
    ): Result<FourEyesSirenDraft> {
        val cleanReviewer = normalizeNip(reviewerNip)
        val current = _drafts.value.firstOrNull { it.draftId == draftId }
            ?: return Result.failure(IllegalArgumentException("Draf sirene tidak ditemukan."))
        if (current.status != FourEyesSirenState.MENUNGGU_PERSETUJUAN) {
            return Result.failure(IllegalStateException("Hanya draf berstatus MENUNGGU PERSETUJUAN yang dapat ditolak."))
        }
        if (!UserRole.canApproveSirenActivation(reviewerRole)) {
            return Result.failure(
                SecurityException("Hanya Kepala Pusdalops / Manajer atau Kalaksa / Pengawas yang dapat menolak draf persetujuan.")
            )
        }
        val rejected = current.copy(
            status = FourEyesSirenState.DITOLAK,
            approvedByNip = cleanReviewer,
            approvedByName = reviewerName.trim().ifBlank { "Pimpinan BPBD" },
            approvedByRole = reviewerRole.trim(),
            decisionAtMs = System.currentTimeMillis(),
            decisionNote = reason.trim().ifBlank { "Ditolak oleh pejabat peninjau." }
        )
        replaceDraft(rejected)
        return Result.success(rejected)
    }

    fun deactivateActiveSiren(
        draftId: String,
        actorName: String,
        note: String = "Kondisi dinyatakan aman / sirene dihentikan petugas."
    ): Result<FourEyesSirenDraft> {
        val current = _drafts.value.firstOrNull { it.draftId == draftId }
            ?: return Result.failure(IllegalArgumentException("Draf sirene tidak ditemukan."))
        if (current.status != FourEyesSirenState.DIAKTIFKAN) {
            return Result.failure(IllegalStateException("Sirene ini tidak sedang dalam status DIAKTIFKAN."))
        }
        val stopped = current.copy(
            status = FourEyesSirenState.DINONAKTIFKAN,
            decisionAtMs = System.currentTimeMillis(),
            decisionNote = "$note (Dihentikan oleh: ${actorName.ifBlank { "Petugas Pusdalops" }})"
        )
        replaceDraft(stopped)
        return Result.success(stopped)
    }

    fun clearAllForTesting() {
        persistDrafts(emptyList())
    }

    private fun replaceDraft(updated: FourEyesSirenDraft) {
        val list = _drafts.value.map { if (it.draftId == updated.draftId) updated else it }
        persistDrafts(list)
    }

    private fun persistDrafts(list: List<FourEyesSirenDraft>) {
        val trimmed = list.take(MAX_STORED_DRAFTS)
        _drafts.value = trimmed
        val arr = JSONArray()
        trimmed.forEach { arr.put(it.toJsonObject()) }
        prefs.edit().putString(KEY_DRAFTS_JSON, arr.toString()).commit()
    }

    private fun loadDraftsFromPrefs(): List<FourEyesSirenDraft> {
        val raw = prefs.getString(KEY_DRAFTS_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    add(FourEyesSirenDraft.fromJsonObject(arr.getJSONObject(i)))
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun normalizeNip(nip: String): String =
        nip.trim().replace(Regex("[\\s\\-]+"), "")

    companion object {
        private const val PREFS_NAME = "sipagar_four_eyes_siren_prefs"
        private const val KEY_DRAFTS_JSON = "four_eyes_siren_drafts_json"
        private const val MAX_STORED_DRAFTS = 30

        @Volatile
        private var INSTANCE: FourEyesSirenManager? = null

        fun getInstance(context: Context): FourEyesSirenManager {
            val appCtx = context.applicationContext ?: context
            val existing = INSTANCE
            if (existing != null && existing.context === appCtx) {
                return existing
            }
            return synchronized(this) {
                val synced = INSTANCE
                if (synced != null && synced.context === appCtx) {
                    synced
                } else {
                    FourEyesSirenManager(appCtx).also { INSTANCE = it }
                }
            }
        }

        fun formatTimestamp(ms: Long): String {
            return try {
                SimpleDateFormat("dd MMM yyyy, HH:mm WIB", Locale.forLanguageTag("id-ID")).format(Date(ms))
            } catch (_: Exception) {
                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(Date(ms))
            }
        }
    }
}
