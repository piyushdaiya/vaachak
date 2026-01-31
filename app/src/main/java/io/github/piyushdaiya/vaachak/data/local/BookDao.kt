/*
 *  Copyright (c) 2026 Piyush Daiya
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 */

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

