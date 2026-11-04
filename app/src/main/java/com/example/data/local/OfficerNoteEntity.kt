package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.Note

/**
 * Room Entity untuk penyimpanan offline persisten Catatan Operasional Petugas Pusdalops.
 */
@Entity(tableName = "officer_notes")
data class OfficerNoteEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val content: String,
    val category: String,
    val priority: String,
    val authorName: String,
    val authorRole: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isPinned: Boolean,
    val tags: String,
    val district: String
) {
    fun toModel(): Note = Note(
        id = id,
        title = title,
        content = content,
        category = category,
        priority = priority,
        authorName = authorName,
        authorRole = authorRole,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isPinned = isPinned,
        tags = tags,
        district = district
    )

    companion object {
        fun fromModel(model: Note): OfficerNoteEntity = OfficerNoteEntity(
            id = model.id,
            title = model.title,
            content = model.content,
            category = model.category,
            priority = model.priority,
            authorName = model.authorName,
            authorRole = model.authorRole,
            createdAt = model.createdAt,
            updatedAt = model.updatedAt,
            isPinned = model.isPinned,
            tags = model.tags,
            district = model.district
        )
    }
}
