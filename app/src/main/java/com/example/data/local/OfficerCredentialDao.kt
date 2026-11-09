package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Officer Credentials in Room database.
 */
@Dao
interface OfficerCredentialDao {

    @Query("SELECT * FROM officer_credentials ORDER BY fullName ASC")
    fun getAllCredentials(): Flow<List<OfficerCredentialEntity>>

    @Query("SELECT * FROM officer_credentials ORDER BY fullName ASC")
    suspend fun getAllCredentialsList(): List<OfficerCredentialEntity>

    @Query("SELECT * FROM officer_credentials WHERE LOWER(nip) = LOWER(:nip) LIMIT 1")
    suspend fun getCredentialByNip(nip: String): OfficerCredentialEntity?

    @Query("SELECT COUNT(*) FROM officer_credentials")
    suspend fun getCredentialCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCredential(credential: OfficerCredentialEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(credentials: List<OfficerCredentialEntity>)

    @Update
    suspend fun updateCredential(credential: OfficerCredentialEntity)

    @Query("UPDATE officer_credentials SET lastLoginAt = :timestamp, lastLoginDevice = :device, updatedAt = :timestamp WHERE LOWER(nip) = LOWER(:nip)")
    suspend fun updateLoginSession(nip: String, timestamp: Long, device: String)

    @Query("UPDATE officer_credentials SET pinHash = :newHash, salt = :newSalt, isCustomPinSet = 1, initialSecurePin = '', isSyncedWithCloud = 0, updatedAt = :timestamp WHERE LOWER(nip) = LOWER(:nip)")
    suspend fun updatePersonalPin(nip: String, newHash: String, newSalt: String, timestamp: Long)

    @Query("UPDATE officer_credentials SET isSyncedWithCloud = :synced, updatedAt = :timestamp WHERE LOWER(nip) = LOWER(:nip)")
    suspend fun markSyncedWithCloud(nip: String, synced: Boolean, timestamp: Long)

    @Query("DELETE FROM officer_credentials WHERE LOWER(nip) = LOWER(:nip)")
    suspend fun deleteByNip(nip: String)

    @Query("DELETE FROM officer_credentials")
    suspend fun clearAll()
}
