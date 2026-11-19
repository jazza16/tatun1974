package com.example.viewmodel

import android.content.Context
import android.content.SharedPreferences
import com.example.model.CasualtiesAndEvacueesData
import com.example.model.DalaAssessmentState
import com.example.model.DalaDummyDataFactory
import com.example.model.DamagesAndLossesData
import com.example.model.EvacuationSite
import com.example.model.UrgentNeedCategory
import com.example.model.UrgentNeedItem
import com.example.model.UrgentNeedStatus
import com.example.model.VulnerableGroupCategory
import com.example.model.VulnerableGroupEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Status indikator Autosave agar petugas lapangan selalu tahu data sudah aman tersimpan.
 */
sealed class DalaAutosaveStatus {
    data object Idle : DalaAutosaveStatus()
    data object Saving : DalaAutosaveStatus()
    data class Saved(val timestampMs: Long) : DalaAutosaveStatus()
    data class Submitted(val timestampMs: Long) : DalaAutosaveStatus()
}

/**
 * State Manager untuk Modul Pasca Bencana / DALA (Data dan Informasi Bencana).
 *
 * Fitur utama:
 * - Memuat draft terakhir dari Local Storage (SharedPreferences) jika aplikasi sebelumnya tertutup mendadak.
 * - Jika belum ada draft lokal, memuat dummy data simulasi BPBD agar form langsung siap diuji.
 * - Autosave otomatis (debounce 500ms) setiap ada perubahan input.
 * - Validasi ketat input numerik (hanya digit positif).
 */
class DalaAssessmentStateManager(
    context: Context,
    initialOfficerName: String = "",
    initialOfficerNip: String = "",
    initialDistrict: String = "Kec. Kota Mukomuko"
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var autoSaveJob: Job? = null

    private val _state: MutableStateFlow<DalaAssessmentState>
    val state: StateFlow<DalaAssessmentState>

    private val _autosaveStatus = MutableStateFlow<DalaAutosaveStatus>(DalaAutosaveStatus.Idle)
    val autosaveStatus: StateFlow<DalaAutosaveStatus> = _autosaveStatus.asStateFlow()

    init {
        val restored = loadFromLocalStorage()
        val initial = if (restored != null) {
            restored.copy(
                officerName = restored.officerName.ifBlank { initialOfficerName },
                officerNip = restored.officerNip.ifBlank { initialOfficerNip }
            )
        } else {
            DalaDummyDataFactory.createSampleAssessment(
                officerName = initialOfficerName,
                officerNip = initialOfficerNip,
                district = initialDistrict
            )
        }
        _state = MutableStateFlow(initial)
        state = _state.asStateFlow()

        if (initial.lastAutoSavedAt > 0L) {
            _autosaveStatus.value = if (initial.submissionStatus == DalaAssessmentState.STATUS_SUBMITTED) {
                DalaAutosaveStatus.Submitted(initial.lastAutoSavedAt)
            } else {
                DalaAutosaveStatus.Saved(initial.lastAutoSavedAt)
            }
        }
    }

    private fun updateAndScheduleAutosave(transform: (DalaAssessmentState) -> DalaAssessmentState) {
        _state.update { current ->
            transform(current).copy(submissionStatus = DalaAssessmentState.STATUS_DRAFT)
        }
        scheduleAutosave()
    }

    private fun scheduleAutosave() {
        autoSaveJob?.cancel()
        _autosaveStatus.value = DalaAutosaveStatus.Saving
        autoSaveJob = scope.launch {
            delay(AUTOSAVE_DEBOUNCE_MS)
            persistNow(markSubmitted = false)
        }
    }

    fun persistNow(markSubmitted: Boolean = false) {
        val now = System.currentTimeMillis()
        val statusLabel = if (markSubmitted) {
            DalaAssessmentState.STATUS_SUBMITTED
        } else {
            DalaAssessmentState.STATUS_DRAFT
        }
        val updated = _state.value.copy(
            submissionStatus = statusLabel,
            lastAutoSavedAt = now
        )
        _state.value = updated
        prefs.edit().putString(KEY_DRAFT_JSON, updated.toJson()).apply()
        _autosaveStatus.value = if (markSubmitted) {
            DalaAutosaveStatus.Submitted(now)
        } else {
            DalaAutosaveStatus.Saved(now)
        }
    }

    fun updateHeaderInfo(disasterTitle: String? = null, district: String? = null) {
        updateAndScheduleAutosave { s ->
            s.copy(
                disasterTitle = disasterTitle ?: s.disasterTitle,
                district = district ?: s.district
            )
        }
    }

    // ==================== TAB 1: KORBAN & PENGUNGSI ====================

    fun updateCasualties(transform: (CasualtiesAndEvacueesData) -> CasualtiesAndEvacueesData) {
        updateAndScheduleAutosave { s ->
            s.copy(casualties = transform(s.casualties))
        }
    }

    fun addEvacuationSite(locationName: String, evacueeCount: Int, notes: String = "") {
        val cleanName = locationName.trim()
        if (cleanName.isBlank()) return
        updateCasualties { c ->
            val newSite = EvacuationSite(
                locationName = cleanName,
                evacueeCount = evacueeCount.coerceAtLeast(0),
                notes = notes.trim()
            )
            c.copy(evacuationSites = c.evacuationSites + newSite)
        }
    }

    fun removeEvacuationSite(siteId: String) {
        updateCasualties { c ->
            c.copy(evacuationSites = c.evacuationSites.filterNot { it.id == siteId })
        }
    }

    fun addOrUpdateVulnerableGroup(category: VulnerableGroupCategory, count: Int, notes: String = "") {
        val safeCount = count.coerceAtLeast(0)
        if (safeCount <= 0) return
        updateCasualties { c ->
            val existing = c.vulnerableGroups.firstOrNull { it.category == category }
            val updatedList = if (existing != null) {
                c.vulnerableGroups.map {
                    if (it.category == category) it.copy(count = safeCount, notes = notes.trim().ifBlank { it.notes })
                    else it
                }
            } else {
                c.vulnerableGroups + VulnerableGroupEntry(
                    category = category,
                    count = safeCount,
                    notes = notes.trim()
                )
            }
            c.copy(vulnerableGroups = updatedList)
        }
    }

    fun removeVulnerableGroup(entryId: String) {
        updateCasualties { c ->
            c.copy(vulnerableGroups = c.vulnerableGroups.filterNot { it.id == entryId })
        }
    }

    // ==================== TAB 2: KERUSAKAN & KERUGIAN ====================

    fun updateDamages(transform: (DamagesAndLossesData) -> DamagesAndLossesData) {
        updateAndScheduleAutosave { s ->
            s.copy(damages = transform(s.damages))
        }
    }

    // ==================== TAB 3: KEBUTUHAN MENDESAK ====================

    fun addUrgentNeed(
        category: UrgentNeedCategory,
        quantity: Int,
        unit: String,
        status: UrgentNeedStatus,
        notes: String
    ) {
        val safeQty = quantity.coerceAtLeast(1)
        val safeUnit = unit.trim().ifBlank { category.defaultUnit }
        updateAndScheduleAutosave { s ->
            val item = UrgentNeedItem(
                category = category,
                quantity = safeQty,
                unit = safeUnit,
                status = status,
                notes = notes.trim()
            )
            s.copy(urgentNeeds = s.urgentNeeds + item)
        }
    }

    fun toggleUrgentNeedStatus(itemId: String) {
        updateAndScheduleAutosave { s ->
            s.copy(
                urgentNeeds = s.urgentNeeds.map { item ->
                    if (item.id == itemId) {
                        val nextStatus = if (item.status == UrgentNeedStatus.MENDESAK) {
                            UrgentNeedStatus.TERSEDIA
                        } else {
                            UrgentNeedStatus.MENDESAK
                        }
                        item.copy(status = nextStatus)
                    } else {
                        item
                    }
                }
            )
        }
    }

    fun removeUrgentNeed(itemId: String) {
        updateAndScheduleAutosave { s ->
            s.copy(urgentNeeds = s.urgentNeeds.filterNot { it.id == itemId })
        }
    }

    // ==================== RESET / SIMULASI ====================

    fun loadDummySimulation() {
        val current = _state.value
        _state.value = DalaDummyDataFactory.createSampleAssessment(
            officerName = current.officerName,
            officerNip = current.officerNip,
            district = current.district
        )
        persistNow(markSubmitted = false)
    }

    fun clearFormToEmpty() {
        val current = _state.value
        _state.value = DalaAssessmentState(
            disasterTitle = "",
            district = current.district,
            officerName = current.officerName,
            officerNip = current.officerNip
        )
        persistNow(markSubmitted = false)
    }

    private fun loadFromLocalStorage(): DalaAssessmentState? {
        val json = prefs.getString(KEY_DRAFT_JSON, null) ?: return null
        return DalaAssessmentState.fromJson(json)
    }

    companion object {
        private const val PREFS_NAME = "bpbd_dala_autosave_prefs"
        private const val KEY_DRAFT_JSON = "dala_assessment_draft_json"
        private const val AUTOSAVE_DEBOUNCE_MS = 500L

        /**
         * Menyaring input agar hanya menerima karakter angka 0-9 dan mengembalikan nilai aman >= 0.
         */
        fun sanitizeDigitsOnly(raw: String, maxDigits: Int = 9): String {
            return raw.filter { it.isDigit() }.take(maxDigits)
        }

        fun parseNonNegativeInt(raw: String): Int {
            return sanitizeDigitsOnly(raw, maxDigits = 7).toIntOrNull()?.coerceAtLeast(0) ?: 0
        }

        fun parseNonNegativeLong(raw: String): Long {
            return sanitizeDigitsOnly(raw, maxDigits = 14).toLongOrNull()?.coerceAtLeast(0L) ?: 0L
        }
    }
}
