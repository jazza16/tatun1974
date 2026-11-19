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
    val maxSeverityScoreSeen: Int
)

/**
 * Mengelola pencegahan alarm ganda yang dijalankan SETELAH validasi sumber selesai:
 * 1. Menolak peringatan yang sudah melewati batas kedaluwarsa (`eventTimestampMillis` dari record BMKG terverifikasi).
 * 2. Mengabaikan pesan dengan `alertId` yang sama apabila tidak ada peningkatan keparahan/status verifikasi.
 * 3. Menerapkan cooldown per insiden (`incidentKey`), sehingga:
 *    - TIDAK memblokir kejadian berbeda (beda jenis bahaya atau beda lokasi/koordinat).
 *    - TIDAK memblokir pembaruan yang lebih berat (`incomingSeverity > previousSeverity`) pada insiden yang sama.
 */
class SirenDeduplicationManager(
    private val sharedPreferences: SharedPreferences? = null,
    private val defaultCooldownMillis: Long = SirenRuntimeConfig.DEFAULT_COOLDOWN_MILLIS,
    private val simulationCooldownMillis: Long = SirenRuntimeConfig.DEFAULT_SIMULATION_COOLDOWN_MILLIS,
    private val maxAlertAgeMillis: Long = SirenRuntimeConfig.DEFAULT_MAX_ALERT_AGE_MILLIS
) {
    constructor(context: Context) : this(
        sharedPreferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
        defaultCooldownMillis = SirenRuntimeConfig.load(context).cooldownWindowMillis,
        simulationCooldownMillis = SirenRuntimeConfig.load(context).simulationCooldownMillis,
        maxAlertAgeMillis = SirenRuntimeConfig.load(context).maxAlertAgeMillis
    )

    private val lock = Any()
    // Menyimpan alertId -> Pair(firstSeenMillis, maxSeverityScore)
    private val processedIdsMemory = LinkedHashMap<String, Pair<Long, Int>>()
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

        val incomingSeverity = validatedAlert.severityScore()
        val incidentKey = validatedAlert.incidentKey()

        // 2. Cek apakah alertId yang sama sudah pernah diproses.
        // Jika alertId sama DAN tingkat keparahannya tidak meningkat, tolak sebagai duplikat.
        // Namun jika merupakan pembaruan yang lebih berat (misal magnitudo direvisi naik atau
        // status naik dari belum terverifikasi menjadi dikonfirmasi petugas), izinkan lewat!
        val previousIdEntry = processedIdsMemory[normalizedId]
        if (previousIdEntry != null && incomingSeverity <= previousIdEntry.second) {
            return DeduplicationDecision.RejectedDuplicateId(
                alertId = normalizedId,
                firstSeenAtMillis = previousIdEntry.first
            )
        }

        // 3. Cek cooldown HANYA terhadap insiden yang sama (incidentKey).
        // Kejadian berbeda (incidentKey berbeda) atau pembaruan yang lebih berat (incomingSeverity > maxSeverityScoreSeen)
        // TIDAK diblokir oleh cooldown.
        val cooldownWindow = if (validatedAlert.simulationMode) {
            simulationCooldownMillis
        } else {
            defaultCooldownMillis
        }
        val previousIncident = incidentCooldownMemory[incidentKey]
        if (previousIncident != null && nowMillis >= previousIncident.lastTriggeredAtMillis) {
            val elapsed = nowMillis - previousIncident.lastTriggeredAtMillis
            val isMoreSevereUpdate = incomingSeverity > previousIncident.maxSeverityScoreSeen
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
        val recordedSeenAt = previousIdEntry?.first ?: nowMillis
        processedIdsMemory[normalizedId] = Pair(recordedSeenAt, incomingSeverity)
        incidentCooldownMemory[incidentKey] = IncidentCooldownRecord(
            incidentKey = incidentKey,
            lastAlertId = normalizedId,
            lastTriggeredAtMillis = nowMillis,
            maxSeverityScoreSeen = maxOf(incomingSeverity, previousIncident?.maxSeverityScoreSeen ?: 0)
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
            if (nowMillis - entry.value.first > ID_RETENTION_WINDOW_MILLIS) {
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
                if (parts.size == 3) {
                    val id = parts[0]
                    val ts = parts[1].toLongOrNull()
                    val severity = parts[2].toIntOrNull()
                    if (id.isNotBlank() && ts != null && severity != null) {
                        processedIdsMemory[id] = Pair(ts, severity)
                    }
                }
            }
        }

        val serializedIncidents = prefs.getString(KEY_INCIDENT_COOLDOWNS, null).orEmpty()
        if (serializedIncidents.isNotBlank()) {
            serializedIncidents.split(";").forEach { token ->
                val parts = token.split("|")
                if (parts.size == 4) {
                    val incKey = parts[0]
                    val lastId = parts[1]
                    val lastTs = parts[2].toLongOrNull()
                    val maxSev = parts[3].toIntOrNull()
                    if (incKey.isNotBlank() && lastTs != null && maxSev != null) {
                        incidentCooldownMemory[incKey] = IncidentCooldownRecord(
                            incidentKey = incKey,
                            lastAlertId = lastId,
                            lastTriggeredAtMillis = lastTs,
                            maxSeverityScoreSeen = maxSev
                        )
                    }
                }
            }
        }
    }

    private fun persistToPrefs() {
        val prefs = sharedPreferences ?: return
        val tailIds = processedIdsMemory.entries.toList().takeLast(MAX_STORED_IDS)
        val serializedIds = tailIds.joinToString(";") { (id, pair) ->
            "${sanitizeDelimiter(id)}|${pair.first}|${pair.second}"
        }

        val tailIncidents = incidentCooldownMemory.entries.toList().takeLast(MAX_STORED_INCIDENTS)
        val serializedIncidents = tailIncidents.joinToString(";") { (_, rec) ->
            "${sanitizeDelimiter(rec.incidentKey)}|${sanitizeDelimiter(rec.lastAlertId)}|${rec.lastTriggeredAtMillis}|${rec.maxSeverityScoreSeen}"
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
        private const val KEY_PROCESSED_IDS = "processed_alert_ids_v2"
        private const val KEY_INCIDENT_COOLDOWNS = "incident_cooldowns_v2"
        private const val ID_RETENTION_WINDOW_MILLIS = 24 * 60 * 60 * 1000L // 24 jam
        private const val MAX_STORED_IDS = 100
        private const val MAX_STORED_INCIDENTS = 50
    }
}
