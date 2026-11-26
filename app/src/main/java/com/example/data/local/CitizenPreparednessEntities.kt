package com.example.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Entitas SQLite/Room lokal untuk Checklist Tas Siaga Bencana (Emergency Go-Bag) warga.
 * Tersimpan 100% lokal di perangkat agar tetap utuh dan dapat diakses tanpa internet saat darurat.
 */
@Entity(tableName = "emergency_go_bag_items")
data class EmergencyGoBagItemEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val isChecked: Boolean = false,
    val isCustom: Boolean = false,
    /** Format ISO tanggal kedaluwarsa opsional (`YYYY-MM-DD`) untuk makanan tahan lama, air minum, atau obat P3K. */
    val expiryDateIso: String? = null,
    val sortOrder: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Entitas SQLite/Room lokal untuk SOP Evakuasi Mandiri (100% Offline).
 * Di-seed otomatis ke SQLite lokal perangkat agar selalu tersedia tanpa sinyal.
 */
@Entity(tableName = "offline_evacuation_sop")
data class OfflineEvacuationSopEntity(
    @PrimaryKey val id: String,
    val namaBencana: String,
    val subJudul: String,
    val rumusUtama: String,
    val langkahJson: String,
    val catatanPenting: String,
    val sortOrder: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface CitizenPreparednessDao {
    @Query("SELECT * FROM emergency_go_bag_items ORDER BY sortOrder ASC, updatedAt ASC")
    fun observeGoBagItems(): Flow<List<EmergencyGoBagItemEntity>>

    @Query("SELECT * FROM emergency_go_bag_items ORDER BY sortOrder ASC, updatedAt ASC")
    suspend fun getGoBagItemsSnapshot(): List<EmergencyGoBagItemEntity>

    @Query("SELECT COUNT(*) FROM emergency_go_bag_items")
    suspend fun getGoBagItemCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoBagItem(item: EmergencyGoBagItemEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGoBagItemsIfAbsent(items: List<EmergencyGoBagItemEntity>)

    @Query("UPDATE emergency_go_bag_items SET isChecked = :isChecked, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateGoBagChecked(id: String, isChecked: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE emergency_go_bag_items SET expiryDateIso = :expiryDateIso, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateGoBagExpiry(id: String, expiryDateIso: String?, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM emergency_go_bag_items WHERE id = :id AND isCustom = 1")
    suspend fun deleteCustomGoBagItem(id: String)

    @Query("SELECT * FROM offline_evacuation_sop ORDER BY sortOrder ASC")
    fun observeEvacuationSops(): Flow<List<OfflineEvacuationSopEntity>>

    @Query("SELECT * FROM offline_evacuation_sop ORDER BY sortOrder ASC")
    suspend fun getEvacuationSopsSnapshot(): List<OfflineEvacuationSopEntity>

    @Query("SELECT COUNT(*) FROM offline_evacuation_sop")
    suspend fun getEvacuationSopCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEvacuationSopsIfAbsent(items: List<OfflineEvacuationSopEntity>)
}
