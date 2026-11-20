package com.example.siren

import android.content.Context
import android.content.SharedPreferences

/**
 * Hasil evaluasi deduplikasi, batas kedaluwarsa (TTL), dan cooldown alarm.
 */
sealed class DeduplicationDecision {
    data object Allowed : DeduplicationDecision()

    data class RejectedDuplicateId(
        val alertId: String,
        val firstSeenAtMillis: Long
    ) : DeduplicationDecision()

    data class RejectedCooldownActive(
        val alertId: String,
        val incidentKey: String,
        val remainingCooldownMillis: Long,
        val previousSeverityScore: Int,
        val incomingSeverityScore: Int
    ) : DeduplicationDecision()

    data class RejectedExpired(
        val alertId: String,
        val eventTimestampMillis: Long,
        val expiresAtMillis: Long,
        val nowMillis: Long
    ) : DeduplicationDecision()
}

/**
 * Rekaman riwayat pemicuan per insiden (`incidentKey`) untuk keperluan evaluasi cooldown cerdas.
 */
data class IncidentCooldownRecord(
    val incidentKey: String,
    val lastAlertId: String,
    val lastTriggeredAtMillis: Long,
    val maxSeverityScoreSeen: Int,
    val maxTypeRankSeen: Int = 0,
    val maxVerificationRankSeen: Int = 0,
    val maxMagnitudeSeen: Double? = null,
    val weatherThresholdMaskSeen: Int = 0
)

/**
 * Mengelola pencegahan alarm ganda yang dijalankan SETELAH validasi sumber selesai:
 * 1. Menolak peringatan yang sudah melewati batas kedaluwarsa (`eventTimestampMillis` dari record BMKG terverifikasi).
 * 2. Mengabaikan pesan dengan `alertId` yang sama apabila tidak ada peningkatan keparahan bermakna/status verifikasi.
 * 3. Menerapkan cooldown per insiden (`incidentKey`), sehingga:
 *    - TIDAK memblokir kejadian berbeda (beda jenis bahaya, beda lokasi/koordinat, atau SIMULASI vs REAL).
 *    - TIDAK memblokir pembaruan yang naik tingkat secara bermakna (status verifikasi naik, tipe naik,
 *      magnitudo naik minimal 0,5, atau ambang cuaca ekstrem baru terlampaui), tetapi tetap menahan kenaikan angka kecil.
 */
class SirenDeduplicationManager(
    private val sharedPreferences: SharedPreferences? = null,
    private val defaultCooldownMillis: Long = SirenRuntimeConfig.DEFAULT_COOLDOWN_MILLIS,
    private val simulationCooldownMillis: Long = SirenRuntimeConfig.DEFAULT_SIMULATION_COOLDOWN_MILLIS,
    private val maxAlertAgeMillis: Long = SirenRuntimeConfig.DEFAULT_MAX_ALERT_AGE_MILLIS,
    private val runtimeConfig: SirenRuntimeConfig = SirenRuntimeConfig()
) {
    constructor(context: Context) : this(
        sharedPreferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
        defaultCooldownMillis = SirenRuntimeConfig.load(context).cooldownWindowMillis,
        simulationCooldownMillis = SirenRuntimeConfig.load(context).simulationCooldownMillis,
        maxAlertAgeMillis = SirenRuntimeConfig.load(context).maxAlertAgeMillis,
        runtimeConfig = SirenRuntimeConfig.load(context)
    )

    private data class ProcessedIdState(
        val firstSeenAtMillis: Long,
        val maxSeverityScoreSeen: Int,
        val maxTypeRankSeen: Int,
        val maxVerificationRankSeen: Int,
        val maxMagnitudeSeen: Double?,
        val weatherThresholdMaskSeen: Int
    )

    private val lock = Any()
    // Menyimpan alertId -> ProcessedIdState
    private val processedIdsMemory = LinkedHashMap<String, ProcessedIdState>()
    // Menyimpan incidentKey -> IncidentCooldownRecord
    private val incidentCooldownMemory = LinkedHashMap<String, IncidentCooldownRecord>()

    init {
        loadFromPrefs()
    }

    /**
     * Dieksekusi SETELAH `SirenSourceValidator.validateIncomingAlert()` selesai dan menghasilkan
     * `validatedPayload` (dengan `eventTimestampMillis` resmi dari record BMKG).
     *
     * Mencatat riwayat deduplikasi & cooldown HANYA jika peringatan diizinkan membunyikan sirene.
     */
    fun evaluateAndRecordAfterValidation(
        validatedAlert: SirenAlertPayload,
        nowMillis: Long = System.currentTimeMillis()
    ): DeduplicationDecision = synchronized(lock) {
        val normalizedId = validatedAlert.alertId.trim()
        require(normalizedId.isNotEmpty()) { "alertId tidak boleh kosong" }

        // 1. Cek batas kedaluwarsa menggunakan eventTimestampMillis yang telah terverifikasi
        val effectiveExpiresAt = minOf(
            validatedAlert.expiresAtMillis,
            validatedAlert.eventTimestampMillis + maxAlertAgeMillis
        )
        if (validatedAlert.isExpired(nowMillis = nowMillis, maxAgeMillis = maxAlertAgeMillis)) {
            return DeduplicationDecision.RejectedExpired(
                alertId = normalizedId,
                eventTimestampMillis = validatedAlert.eventTimestampMillis,
                expiresAtMillis = effectiveExpiresAt,
                nowMillis = nowMillis
            )
        }

        pruneOldEntries(nowMillis)

        val incomingSeverity = validatedAlert.severityScore(runtimeConfig)
        val incomingTypeRank = validatedAlert.alertType.baseSeverityRank
        val incomingVerifRank = validatedAlert.verificationStatus.verificationRank
        val incomingMag = validatedAlert.magnitude
        val incomingWeatherMask = validatedAlert.extremeWeatherThresholdBitmask(runtimeConfig)
        val incidentKey = validatedAlert.incidentKey()

        // 2. Cek apakah alertId yang sama sudah pernah diproses.
        // Jika alertId sama DAN TIDAK naik tingkat secara bermakna (status verifikasi naik, tipe naik,
        // magnitudo naik >= 0.5, atau ambang cuaca ekstrem baru terlampaui), tolak sebagai duplikat.
        val previousIdEntry = processedIdsMemory[normalizedId]
        if (previousIdEntry != null) {
            val isMeaningfulIdUpgrade = validatedAlert.isMeaningfullyMoreSevereThan(
                prevTypeRank = previousIdEntry.maxTypeRankSeen,
                prevVerificationRank = previousIdEntry.maxVerificationRankSeen,
                prevMaxMagnitude = previousIdEntry.maxMagnitudeSeen,
                prevWeatherThresholdMask = previousIdEntry.weatherThresholdMaskSeen,
                config = runtimeConfig
            )
            if (!isMeaningfulIdUpgrade) {
                return DeduplicationDecision.RejectedDuplicateId(
                    alertId = normalizedId,
                    firstSeenAtMillis = previousIdEntry.firstSeenAtMillis
                )
            }
        }

        // 3. Cek cooldown HANYA terhadap insiden yang sama (incidentKey).
        // Kejadian berbeda (incidentKey berbeda) atau pembaruan yang naik tingkat secara bermakna
        // TIDAK diblokir oleh cooldown.
        val cooldownWindow = if (validatedAlert.simulationMode) {
            simulationCooldownMillis
        } else {
            defaultCooldownMillis
        }
        val previousIncident = incidentCooldownMemory[incidentKey]
        if (previousIncident != null && nowMillis >= previousIncident.lastTriggeredAtMillis) {
            val elapsed = nowMillis - previousIncident.lastTriggeredAtMillis
            val isMoreSevereUpdate = validatedAlert.isMeaningfullyMoreSevereThan(
                prevTypeRank = previousIncident.maxTypeRankSeen,
                prevVerificationRank = previousIncident.maxVerificationRankSeen,
                prevMaxMagnitude = previousIncident.maxMagnitudeSeen,
                prevWeatherThresholdMask = previousIncident.weatherThresholdMaskSeen,
                config = runtimeConfig
            )
            if (elapsed < cooldownWindow && !isMoreSevereUpdate) {
                return DeduplicationDecision.RejectedCooldownActive(
                    alertId = normalizedId,
                    incidentKey = incidentKey,
                    remainingCooldownMillis = cooldownWindow - elapsed,
                    previousSeverityScore = previousIncident.maxSeverityScoreSeen,
                    incomingSeverityScore = incomingSeverity
                )
            }
        }

        // 4. Lolos seluruh pemeriksaan -> catat ke memori dan simpan ke SharedPreferences
        val recordedSeenAt = previousIdEntry?.firstSeenAtMillis ?: nowMillis
        val mergedIdMaxMag = when {
            incomingMag != null && previousIdEntry?.maxMagnitudeSeen != null ->
                maxOf(incomingMag, previousIdEntry.maxMagnitudeSeen)
            incomingMag != null -> incomingMag
            else -> previousIdEntry?.maxMagnitudeSeen
        }
        processedIdsMemory[normalizedId] = ProcessedIdState(
            firstSeenAtMillis = recordedSeenAt,
            maxSeverityScoreSeen = maxOf(incomingSeverity, previousIdEntry?.maxSeverityScoreSeen ?: 0),
            maxTypeRankSeen = maxOf(incomingTypeRank, previousIdEntry?.maxTypeRankSeen ?: 0),
            maxVerificationRankSeen = maxOf(incomingVerifRank, previousIdEntry?.maxVerificationRankSeen ?: 0),
            maxMagnitudeSeen = mergedIdMaxMag,
            weatherThresholdMaskSeen = (previousIdEntry?.weatherThresholdMaskSeen ?: 0) or incomingWeatherMask
        )

        val mergedIncidentMaxMag = when {
            incomingMag != null && previousIncident?.maxMagnitudeSeen != null ->
                maxOf(incomingMag, previousIncident.maxMagnitudeSeen)
            incomingMag != null -> incomingMag
            else -> previousIncident?.maxMagnitudeSeen
        }
        incidentCooldownMemory[incidentKey] = IncidentCooldownRecord(
            incidentKey = incidentKey,
            lastAlertId = normalizedId,
            lastTriggeredAtMillis = nowMillis,
            maxSeverityScoreSeen = maxOf(incomingSeverity, previousIncident?.maxSeverityScoreSeen ?: 0),
            maxTypeRankSeen = maxOf(incomingTypeRank, previousIncident?.maxTypeRankSeen ?: 0),
            maxVerificationRankSeen = maxOf(incomingVerifRank, previousIncident?.maxVerificationRankSeen ?: 0),
            maxMagnitudeSeen = mergedIncidentMaxMag,
            weatherThresholdMaskSeen = (previousIncident?.weatherThresholdMaskSeen ?: 0) or incomingWeatherMask
        )
        persistToPrefs()
        return DeduplicationDecision.Allowed
    }

    fun isAlreadyProcessed(alertId: String): Boolean = synchronized(lock) {
        processedIdsMemory.containsKey(alertId.trim())
    }

    fun clearHistory() = synchronized(lock) {
        processedIdsMemory.clear()
        incidentCooldownMemory.clear()
        sharedPreferences?.edit()?.clear()?.apply()
    }

    private fun pruneOldEntries(nowMillis: Long) {
        val idIterator = processedIdsMemory.entries.iterator()
        while (idIterator.hasNext()) {
            val entry = idIterator.next()
            if (nowMillis - entry.value.firstSeenAtMillis > ID_RETENTION_WINDOW_MILLIS) {
                idIterator.remove()
            }
        }
        val incidentIterator = incidentCooldownMemory.entries.iterator()
        while (incidentIterator.hasNext()) {
            val entry = incidentIterator.next()
            if (nowMillis - entry.value.lastTriggeredAtMillis > ID_RETENTION_WINDOW_MILLIS) {
                incidentIterator.remove()
            }
        }
    }

    private fun loadFromPrefs() {
        val prefs = sharedPreferences ?: return
        val serializedIds = prefs.getString(KEY_PROCESSED_IDS, null).orEmpty()
        if (serializedIds.isNotBlank()) {
            serializedIds.split(";").forEach { token ->
                val parts = token.split("|")
                if (parts.size >= 3) {
                    val id = parts[0]
                    val ts = parts[1].toLongOrNull()
                    val severity = parts[2].toIntOrNull()
                    val typeRank = parts.getOrNull(3)?.toIntOrNull() ?: 0
                    val verifRank = parts.getOrNull(4)?.toIntOrNull() ?: 0
                    val mag = parts.getOrNull(5)?.toDoubleOrNull()
                    val weatherMask = parts.getOrNull(6)?.toIntOrNull() ?: 0
                    if (id.isNotBlank() && ts != null && severity != null) {
                        processedIdsMemory[id] = ProcessedIdState(
                            firstSeenAtMillis = ts,
                            maxSeverityScoreSeen = severity,
                            maxTypeRankSeen = typeRank,
                            maxVerificationRankSeen = verifRank,
                            maxMagnitudeSeen = mag,
                            weatherThresholdMaskSeen = weatherMask
                        )
                    }
                }
            }
        }

        val serializedIncidents = prefs.getString(KEY_INCIDENT_COOLDOWNS, null).orEmpty()
        if (serializedIncidents.isNotBlank()) {
            serializedIncidents.split(";").forEach { token ->
                val parts = token.split("|")
                if (parts.size >= 4) {
                    val incKey = parts[0]
                    val lastId = parts[1]
                    val lastTs = parts[2].toLongOrNull()
                    val maxSev = parts[3].toIntOrNull()
                    val maxTypeRank = parts.getOrNull(4)?.toIntOrNull() ?: 0
                    val maxVerifRank = parts.getOrNull(5)?.toIntOrNull() ?: 0
                    val maxMag = parts.getOrNull(6)?.toDoubleOrNull()
                    val weatherMask = parts.getOrNull(7)?.toIntOrNull() ?: 0
                    if (incKey.isNotBlank() && lastTs != null && maxSev != null) {
                        incidentCooldownMemory[incKey] = IncidentCooldownRecord(
                            incidentKey = incKey,
                            lastAlertId = lastId,
                            lastTriggeredAtMillis = lastTs,
                            maxSeverityScoreSeen = maxSev,
                            maxTypeRankSeen = maxTypeRank,
                            maxVerificationRankSeen = maxVerifRank,
                            maxMagnitudeSeen = maxMag,
                            weatherThresholdMaskSeen = weatherMask
                        )
                    }
                }
            }
        }
    }

    private fun persistToPrefs() {
        val prefs = sharedPreferences ?: return
        val tailIds = processedIdsMemory.entries.toList().takeLast(MAX_STORED_IDS)
        val serializedIds = tailIds.joinToString(";") { (id, state) ->
            val magToken = state.maxMagnitudeSeen?.toString().orEmpty()
            "${sanitizeDelimiter(id)}|${state.firstSeenAtMillis}|${state.maxSeverityScoreSeen}|${state.maxTypeRankSeen}|${state.maxVerificationRankSeen}|$magToken|${state.weatherThresholdMaskSeen}"
        }

        val tailIncidents = incidentCooldownMemory.entries.toList().takeLast(MAX_STORED_INCIDENTS)
        val serializedIncidents = tailIncidents.joinToString(";") { (_, rec) ->
            val magToken = rec.maxMagnitudeSeen?.toString().orEmpty()
            "${sanitizeDelimiter(rec.incidentKey)}|${sanitizeDelimiter(rec.lastAlertId)}|${rec.lastTriggeredAtMillis}|${rec.maxSeverityScoreSeen}|${rec.maxTypeRankSeen}|${rec.maxVerificationRankSeen}|$magToken|${rec.weatherThresholdMaskSeen}"
        }

        prefs.edit()
            .putString(KEY_PROCESSED_IDS, serializedIds)
            .putString(KEY_INCIDENT_COOLDOWNS, serializedIncidents)
            .apply()
    }

    private fun sanitizeDelimiter(raw: String): String {
        return raw.replace("|", "_").replace(";", "_")
    }

    companion object {
        private const val PREFS_NAME = "sipagar_siren_dedup_prefs"
        private const val KEY_PROCESSED_IDS = "processed_alert_ids_v3"
        private const val KEY_INCIDENT_COOLDOWNS = "incident_cooldowns_v3"
        private const val ID_RETENTION_WINDOW_MILLIS = 24 * 60 * 60 * 1000L // 24 jam
        private const val MAX_STORED_IDS = 100
        private const val MAX_STORED_INCIDENTS = 50
    }
}
