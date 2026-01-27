package io.github.piyushdaiya.vaachak.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String,
    val uriString: String, // Where the file is located
    val coverPath: String? = null, // Path to cached cover (optional for now)
    val progress: Double = 0.0,
    val lastLocationJson: String? = null,
    val addedDate: Long = System.currentTimeMillis(),
    val lastRead: Long = System.currentTimeMillis() // For sorting
)