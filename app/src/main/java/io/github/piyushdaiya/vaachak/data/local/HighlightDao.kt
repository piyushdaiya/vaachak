package io.github.piyushdaiya.vaachak.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HighlightDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHighlight(highlight: HighlightEntity): Long

    @Query("SELECT * FROM highlights WHERE publicationId = :bookId")
    fun getHighlightsForBook(bookId: String): Flow<List<HighlightEntity>>

    // --- NEW FUNCTIONS TO ADD ---

    // 1. Delete a single highlight by its unique ID
    @Query("DELETE FROM highlights WHERE id = :id")
    suspend fun deleteHighlightById(id: Long)

    // 2. Get ALL highlights, ordered by book title, then by newest first
    @Query("SELECT * FROM highlights ORDER BY publicationId ASC, id DESC")
    fun getAllHighlights(): Flow<List<HighlightEntity>>
}

