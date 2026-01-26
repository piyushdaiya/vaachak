package io.github.piyushdaiya.vaachak.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "highlights")
data class HighlightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val publicationId: String,  // Links the highlight to a specific book
    val locatorJson: String,    // Stores the exact position (chapter, page, location)
    val text: String,           // The selected text itself
    val color: Int,             // The highlight color (Yellow, Green, etc.)
    val created: Long = System.currentTimeMillis()
)