package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Personnel entity in Room database.
 */
@Dao
interface PersonnelDao {
    @Query("SELECT * FROM personnel ORDER BY id DESC")
    fun getAllPersonnel(): Flow<List<Personnel>>

    @Query("SELECT * FROM personnel ORDER BY id DESC")
    suspend fun getAllPersonnelList(): List<Personnel>

    @Query("SELECT * FROM personnel WHERE id = :id LIMIT 1")
    suspend fun getPersonnelById(id: Long): Personnel?

    @Query("SELECT * FROM personnel WHERE LOWER(nip) = LOWER(:nip) LIMIT 1")
    suspend fun getPersonnelByNip(nip: String): Personnel?

    @Query("SELECT * FROM personnel WHERE role = :role ORDER BY name ASC")
    fun getPersonnelByRole(role: String): Flow<List<Personnel>>

    @Query("SELECT * FROM personnel WHERE shift = :shift ORDER BY name ASC")
    fun getPersonnelByShift(shift: String): Flow<List<Personnel>>

    @Query("SELECT * FROM personnel WHERE role = :role AND shift = :shift ORDER BY name ASC")
    fun getPersonnelByRoleAndShift(role: String, shift: String): Flow<List<Personnel>>

    @Query("SELECT * FROM personnel WHERE name LIKE '%' || :query || '%' OR nip LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchPersonnel(query: String): Flow<List<Personnel>>

    @Query("SELECT COUNT(*) FROM personnel")
    suspend fun getPersonnelCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPersonnel(personnel: Personnel): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPersonnel(personnelList: List<Personnel>)

    @Update
    suspend fun updatePersonnel(personnel: Personnel)

    @Delete
    suspend fun deletePersonnel(personnel: Personnel)

    @Query("DELETE FROM personnel WHERE id = :id")
    suspend fun deletePersonnelById(id: Long)

    @Query("DELETE FROM personnel")
    suspend fun clearAll()
}
