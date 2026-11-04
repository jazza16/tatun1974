package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM officer_notes ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotesFlow(): Flow<List<OfficerNoteEntity>>

    @Query("SELECT * FROM officer_notes WHERE id = :id")
    suspend fun getNoteById(id: String): OfficerNoteEntity?

    @Query("SELECT * FROM officer_notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%' ORDER BY isPinned DESC, updatedAt DESC")
    fun searchNotesFlow(query: String): Flow<List<OfficerNoteEntity>>

    @Query("SELECT * FROM officer_notes WHERE category = :category ORDER BY isPinned DESC, updatedAt DESC")
    fun getNotesByCategoryFlow(category: String): Flow<List<OfficerNoteEntity>>

    @Query("SELECT COUNT(*) FROM officer_notes")
    suspend fun getNotesCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: OfficerNoteEntity)

    @Update
    suspend fun updateNote(note: OfficerNoteEntity)

    @Delete
    suspend fun deleteNote(note: OfficerNoteEntity)

    @Query("DELETE FROM officer_notes WHERE id = :id")
    suspend fun deleteNoteById(id: String)

    @Query("UPDATE officer_notes SET isPinned = :isPinned WHERE id = :id")
    suspend fun updatePinStatus(id: String, isPinned: Boolean)
}
