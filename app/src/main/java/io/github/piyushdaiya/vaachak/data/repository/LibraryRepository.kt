package io.github.piyushdaiya.vaachak.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.piyushdaiya.vaachak.data.local.BookDao
import io.github.piyushdaiya.vaachak.data.local.BookEntity
import io.github.piyushdaiya.vaachak.ui.reader.ReadiumManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepository @Inject constructor(
    private val bookDao: BookDao,
    private val readiumManager: ReadiumManager,
    @ApplicationContext private val context: Context
) {

    suspend fun isBookDuplicate(title: String): Boolean {
        return bookDao.isBookExists(title)
    }

    /**
     * LOCAL IMPORT (From Device Storage)
     */
    suspend fun importBook(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        val existing = bookDao.getBookByUri(uri.toString())
        if (existing != null) return@withContext Result.failure(Exception("Book already in library (File match)"))

        try {
            context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) {}

        val publication = readiumManager.openEpubFromUri(uri)
            ?: return@withContext Result.failure(Exception("Failed to parse book"))

        try {
            val title = publication.metadata.title ?: "Unknown Title"

            // Duplicate Check
            if (bookDao.isBookExists(title)) {
                return@withContext Result.failure(Exception("Duplicate: '$title' is already in your library."))
            }

            val author = publication.metadata.authors.firstOrNull()?.name?.toString() ?: "Unknown Author"

            // Extract Cover
            var savedCoverPath: String? = null
            try {
                val bitmap = readiumManager.getPublicationCover(publication)
                if (bitmap != null) {
                    savedCoverPath = saveBitmapToStorage(bitmap, title)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val newBook = BookEntity(
                title = title,
                author = author,
                uriString = uri.toString(),
                coverPath = savedCoverPath,
                addedDate = System.currentTimeMillis(),
                lastRead = System.currentTimeMillis(),
                progress = 0.0
            )
            bookDao.insertBook(newBook)

            return@withContext Result.success("Book added successfully")

        } catch (e: Exception) {
            return@withContext Result.failure(e)
        } finally {
            readiumManager.closePublication()
        }
    }

    /**
     * OPDS SAVE (Now accepts a local path, does NOT download)
     */
    suspend fun addDownloadedBook(bookFile: File, title: String, author: String, coverFile: File?): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (bookDao.isBookExists(title)) {
                return@withContext Result.failure(Exception("Book already exists"))
            }

            val newBook = BookEntity(
                title = title,
                author = author,
                uriString = Uri.fromFile(bookFile).toString(),
                coverPath = coverFile?.absolutePath, // Use the downloaded cover path
                addedDate = System.currentTimeMillis(),
                lastRead = System.currentTimeMillis(),
                progress = 0.0
            )

            bookDao.insertBook(newBook)
            Result.success("Saved to Library")
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun saveBitmapToStorage(bitmap: Bitmap, title: String): String {
        val filename = "cover_${title.hashCode()}.png"
        val file = File(context.filesDir, filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file.absolutePath
    }
}