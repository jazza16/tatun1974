package com.example.data.repository

import com.example.data.local.Personnel
import com.example.data.local.PersonnelDao
import kotlinx.coroutines.flow.Flow

/**
 * Repository for Personnel entity, abstracting Room DAO operations from ViewModels & UI.
 * Implements input validation for NIP and Name.
 */
class PersonnelRepository(private val personnelDao: PersonnelDao) {

    val allPersonnel: Flow<List<Personnel>> = personnelDao.getAllPersonnel()

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

    /**
     * Pre-populates the 8 official ASN BPBD Kabupaten Mukomuko operators into Room database.
     * Ensures each operator's name and NIP are preserved in separate columns.
     */
    suspend fun seedDefaultAsnPersonnelIfNeeded() {
        for (operator in com.example.data.local.OfficialAsnBpbdPersonnel.OPERATORS) {
            val existing = personnelDao.getPersonnelByNip(operator.nip)
            if (existing == null) {
                personnelDao.insertPersonnel(operator)
            }
        }
    }

    /**
     * Streams the official Pusdalops operators from Room database for quick selection during officer login.
     */
    fun getOperatorsPusdalops(): Flow<List<Personnel>> {
        return personnelDao.getPersonnelByRole(com.example.data.local.OfficialAsnBpbdPersonnel.ROLE_OPERATOR)
    }
}
