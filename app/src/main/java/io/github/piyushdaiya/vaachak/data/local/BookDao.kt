package io.github.piyushdaiya.vaachak.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY lastRead DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBook(book: BookEntity)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBook(id: Long)

    @Query("UPDATE books SET progress = :progress WHERE uriString = :uriString")
    suspend fun updateProgress(uriString: String, progress: Double)

    // NEW: Fetch single book to get saved location
    @Query("SELECT * FROM books WHERE uriString = :uri LIMIT 1")
    suspend fun getBookByUri(uri: String): BookEntity?

    // NEW: Update the full locator JSON
    @Query("UPDATE books SET lastLocationJson = :locationJson WHERE uriString = :uriString")
    suspend fun updateLastLocation(uriString: String, locationJson: String)

    @Query("UPDATE books SET lastRead = :timestamp WHERE uriString = :uriString")
    suspend fun updateLastRead(uriString: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM books ORDER BY lastRead DESC")
    fun getAllBooksSortedByRecent(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books ORDER BY lastRead DESC LIMIT 1")
    fun getMostRecentBook(): Flow<BookEntity?>
}

