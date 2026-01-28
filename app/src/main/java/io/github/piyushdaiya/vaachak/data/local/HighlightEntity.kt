package io.github.piyushdaiya.vaachak.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "highlights",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["uriString"], // This matches BookEntity
            childColumns = ["publicationId"], // CHANGED from bookUri to publicationId
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["publicationId"])] // CHANGED from bookUri to publicationId
)
data class HighlightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val publicationId: String,  // Links the highlight to a specific book
    val locatorJson: String,    // Stores the exact position (chapter, page, location)
    val text: String,
    val color: Int,
    val tag: String = "General",
    val created: Long = System.currentTimeMillis()
)