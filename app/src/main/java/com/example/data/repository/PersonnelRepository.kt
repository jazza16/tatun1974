package com.example.data.repository

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
     * Ensures each personnel's name and NIP are preserved in separate columns.
     */
    suspend fun seedDefaultAsnPersonnelIfNeeded() {
        for (person in com.example.data.local.OfficialAsnBpbdPersonnel.ALL_OFFICIAL_PERSONNEL) {
            val existing = personnelDao.getPersonnelByNip(person.nip)
            if (existing == null) {
                personnelDao.insertPersonnel(person)
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
     * Ensures newly installed devices or other devices instantly receive newly registered officers.
     */
    fun startAutoSyncFromCloud(scope: kotlinx.coroutines.CoroutineScope) {
        com.example.data.local.FirestoreHelper.listenOfficersRealtime(
            onUpdate = { remoteList ->
                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    for (officer in remoteList) {
                        val cleanNip = officer.nip.trim().replace(Regex("[\\s\\-]+"), "")
                        val cleanName = officer.fullName.trim()
                        if (cleanNip.isNotBlank() && cleanName.isNotBlank()) {
                            val local = personnelDao.getPersonnelByNip(cleanNip)
                            if (local == null) {
                                personnelDao.insertPersonnel(
                                    Personnel(
                                        name = cleanName,
                                        nip = cleanNip,
                                        role = officer.role,
                                        unit = officer.unit,
                                        shift = officer.shift,
                                        phone = officer.phone
                                    )
                                )
                            } else if (local.name != cleanName || local.role != officer.role || local.phone != officer.phone) {
                                personnelDao.updatePersonnel(
                                    local.copy(
                                        name = cleanName,
                                        nip = cleanNip,
                                        role = officer.role,
                                        unit = officer.unit,
                                        shift = officer.shift,
                                        phone = officer.phone
                                    )
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}
