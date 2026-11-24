package com.example.data.repository

import android.content.Context
import com.example.data.local.OfficerAuthManager
import com.example.data.local.Personnel
import com.example.data.local.PersonnelAuditLogDao
import com.example.data.local.PersonnelAuditLogEntity
import com.example.data.local.PersonnelDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * Repository for Personnel entity, abstracting Room DAO operations from ViewModels & UI.
 * Implements input validation for NIP and Name, and manages Audit Logs for Staff Accountability.
 */
class PersonnelRepository(
    private val personnelDao: PersonnelDao,
    private val auditLogDao: PersonnelAuditLogDao? = null
) {

    val allPersonnel: Flow<List<Personnel>> = personnelDao.getAllPersonnel()

    val allAuditLogs: Flow<List<PersonnelAuditLogEntity>> =
        auditLogDao?.getAllAuditLogs() ?: flowOf(emptyList())

    suspend fun recordAuditLog(
        name: String,
        nip: String,
        role: String,
        shift: String,
        action: String,
        deviceInfo: String = "Pusdalops Terminal Mobile"
    ): Long {
        return auditLogDao?.insertAuditLog(
            PersonnelAuditLogEntity(
                name = name,
                nip = nip,
                role = role,
                shift = shift,
                action = action,
                deviceInfo = deviceInfo,
                timestamp = System.currentTimeMillis()
            )
        ) ?: -1L
    }

    suspend fun clearAuditLogs() {
        auditLogDao?.clearAuditLogs()
    }

    suspend fun getAllPersonnelList(): List<Personnel> = personnelDao.getAllPersonnelList()

    suspend fun insert(personnel: Personnel): Long = personnelDao.insertPersonnel(personnel)

    suspend fun insertAll(personnelList: List<Personnel>) = personnelDao.insertAllPersonnel(personnelList)

    suspend fun delete(personnel: Personnel) = personnelDao.deletePersonnel(personnel)

    suspend fun deleteById(id: Long) = personnelDao.deletePersonnelById(id)

    suspend fun deleteByNip(nip: String) {
        val cleanNip = nip.trim().replace(Regex("[\\s\\-]+"), "")
        if (cleanNip.isNotBlank()) {
            personnelDao.deletePersonnelByNip(cleanNip)
        }
    }

    suspend fun getByNip(nip: String): Personnel? = personnelDao.getPersonnelByNip(nip)

    fun getByRole(role: String): Flow<List<Personnel>> = personnelDao.getPersonnelByRole(role)

    fun getByShift(shift: String): Flow<List<Personnel>> = personnelDao.getPersonnelByShift(shift)

    fun search(query: String): Flow<List<Personnel>> = personnelDao.searchPersonnel(query)

    suspend fun getCount(): Int = personnelDao.getPersonnelCount()

    suspend fun clearAll() = personnelDao.clearAll()

    /**
     * Validation function in the Admin module to verify that both the NIP and Name fields
     * are filled and meet valid administrative requirements before adding a new personnel entry.
     */
    fun validatePersonnelInput(name: String, nip: String): String? {
        val trimmedName = name.trim()
        val trimmedNip = nip.trim()

        if (trimmedName.isEmpty() && trimmedNip.isEmpty()) {
            return "Nama lengkap dan NIP/No. Registrasi wajib diisi."
        }
        if (trimmedName.isEmpty()) {
            return "Nama lengkap personil tidak boleh kosong."
        }
        if (trimmedName.length < 3) {
            return "Nama personil minimal 3 karakter."
        }
        if (trimmedNip.isEmpty()) {
            return "NIP / Nomor Registrasi TRC tidak boleh kosong."
        }
        if (trimmedNip.length < 4) {
            return "NIP / No. Registrasi minimal 4 karakter."
        }
        return null
    }

    suspend fun update(personnel: Personnel) = personnelDao.updatePersonnel(personnel)

    /**
     * Pre-populates the official ASN BPBD Kabupaten Mukomuko personnel (leadership & operators) into Room database.
     * Respects Admin-deleted NIP tombstones so retired, transferred, or typo-corrected ASN never resurrect.
     */
    suspend fun seedDefaultAsnPersonnelIfNeeded(context: Context? = null) {
        val authManager = context?.let { OfficerAuthManager(it.applicationContext) }
        val existingCleanNips = personnelDao.getAllPersonnelList()
            .map { it.nip.trim().replace(Regex("[\\s\\-]+"), "").lowercase() }
            .toMutableSet()

        for (person in com.example.data.local.OfficialAsnBpbdPersonnel.ALL_OFFICIAL_PERSONNEL) {
            val cleanNip = person.nip.trim().replace(Regex("[\\s\\-]+"), "")
            if (cleanNip.isBlank()) continue
            if (authManager?.isNipDeleted(cleanNip) == true) continue
            if (existingCleanNips.add(cleanNip.lowercase())) {
                personnelDao.insertPersonnel(person.copy(nip = cleanNip))
            }
        }
    }

    /**
     * Streams the official Pusdalops operators from Room database for quick selection during officer login.
     */
    fun getOperatorsPusdalops(): Flow<List<Personnel>> {
        return personnelDao.getPersonnelByRole(com.example.data.local.OfficialAsnBpbdPersonnel.ROLE_OPERATOR)
    }

    /**
     * Streams all official leadership and operators for administration and assignment.
     */
    fun getAllAsnPersonnel(): Flow<List<Personnel>> {
        return personnelDao.getAllPersonnel()
    }

    /**
     * Automatically saves personnel data when an Administrator enters a new officer Name and NIP,
     * and synchronizes directly to Cloud Firestore so newly installed devices receive it immediately.
     */
    suspend fun autoSaveAndSyncNewPersonnel(
        name: String,
        nip: String,
        role: String = "Anggota TRC",
        unit: String = "Pusdalops BPBD Mukomuko",
        shift: String = "Siaga 24 Jam",
        phone: String = ""
    ): Long {
        val cleanName = name.trim()
        val cleanNip = nip.trim().replace(Regex("[\\s\\-]+"), "")
        if (cleanName.isBlank() || cleanNip.isBlank()) return -1L

        val existing = personnelDao.getPersonnelByNip(cleanNip)
        val id = if (existing != null) {
            val updated = existing.copy(
                name = cleanName,
                nip = cleanNip,
                role = role,
                unit = unit,
                shift = shift,
                phone = phone
            )
            personnelDao.updatePersonnel(updated)
            existing.id
        } else {
            val newPersonnel = Personnel(
                name = cleanName,
                nip = cleanNip,
                role = role,
                unit = unit,
                shift = shift,
                phone = phone
            )
            personnelDao.insertPersonnel(newPersonnel)
        }

        // Auto-sync to Cloud Firestore for cross-device & new device installation sync
        try {
            val cloudProfile = com.example.data.local.OfficerProfile(
                id = "officer_$cleanNip",
                fullName = cleanName,
                nip = cleanNip,
                role = role,
                unit = unit,
                shift = shift,
                phone = phone
            )
            com.example.data.local.FirestoreHelper.syncOfficerToCloud(cloudProfile)
        } catch (_: Exception) {}

        return id
    }

    /**
     * Starts background real-time sync from Cloud Firestore into local Room database.
     * Propagates cloud soft-deletes (tombstones) and respects local deletions so deleted ASN never resurrect.
     */
    fun startAutoSyncFromCloud(scope: CoroutineScope, context: Context? = null) {
        val authManager = context?.let { OfficerAuthManager(it.applicationContext) }
        var latestCloudUpdatedAt: Map<String, Long> = emptyMap()
        com.example.data.local.FirestoreHelper.listenOfficersRealtime(
            onDeletedNips = { deletedMap ->
                scope.launch(Dispatchers.IO) {
                    for ((delNip, delTs) in deletedMap) {
                        authManager?.markNipDeleted(delNip, delTs)
                        personnelDao.deletePersonnelByNip(delNip)
                    }
                    authManager?.syncRosterFromCloud(emptyList(), deletedCloudNips = deletedMap)
                }
            },
            onUpdatedAtByNip = { updatedMap ->
                latestCloudUpdatedAt = updatedMap
            },
            onUpdate = { remoteList ->
                scope.launch(Dispatchers.IO) {
                    val existingAll = personnelDao.getAllPersonnelList()
                    val existingByCleanNip = existingAll.associateBy {
                        it.nip.trim().replace(Regex("[\\s\\-]+"), "").lowercase()
                    }.toMutableMap()

                    for (officer in remoteList) {
                        val cleanNip = officer.nip.trim().replace(Regex("[\\s\\-]+"), "")
                        val cleanName = officer.fullName.trim()
                        if (cleanNip.isBlank() || cleanName.isBlank()) continue
                        val key = cleanNip.lowercase()

                        val localDeletedAt = authManager?.getDeletedTimestamp(cleanNip)
                        if (localDeletedAt != null) {
                            val cloudTs = latestCloudUpdatedAt[key] ?: 0L
                            if (cloudTs > localDeletedAt) {
                                authManager.unmarkNipDeleted(cleanNip)
                            } else {
                                // Tetap hormati penghapusan oleh Admin; jangan hidupkan kembali di Room
                                personnelDao.deletePersonnelByNip(cleanNip)
                                continue
                            }
                        }

                        val local = existingByCleanNip[key] ?: personnelDao.getPersonnelByNip(cleanNip)
                        if (local == null) {
                            val inserted = Personnel(
                                name = cleanName,
                                nip = cleanNip,
                                role = officer.role,
                                unit = officer.unit,
                                shift = officer.shift,
                                phone = officer.phone
                            )
                            val newId = personnelDao.insertPersonnel(inserted)
                            existingByCleanNip[key] = inserted.copy(id = newId)
                        } else if (
                            local.name != cleanName ||
                            local.role != officer.role ||
                            local.unit != officer.unit ||
                            local.shift != officer.shift ||
                            local.phone != officer.phone ||
                            local.nip != cleanNip
                        ) {
                            val updated = local.copy(
                                name = cleanName,
                                nip = cleanNip,
                                role = officer.role,
                                unit = officer.unit,
                                shift = officer.shift,
                                phone = officer.phone
                            )
                            personnelDao.updatePersonnel(updated)
                            existingByCleanNip[key] = updated
                        }
                    }
                    authManager?.syncRosterFromCloud(
                        cloudOfficers = remoteList,
                        cloudUpdatedAtByNip = latestCloudUpdatedAt
                    )
                }
            }
        )
    }
}
