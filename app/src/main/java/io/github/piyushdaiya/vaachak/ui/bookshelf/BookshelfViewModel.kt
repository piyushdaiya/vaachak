package io.github.piyushdaiya.vaachak.ui.bookshelf

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.piyushdaiya.vaachak.data.local.BookDao
import io.github.piyushdaiya.vaachak.data.local.BookEntity
import io.github.piyushdaiya.vaachak.ui.reader.ReadiumManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class BookshelfViewModel @Inject constructor(
    private val bookDao: BookDao,
    private val readiumManager: ReadiumManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // FIX: Explicitly type the MutableState to avoid inference errors
    val snackbarMessage: MutableState<String?> = mutableStateOf<String?>(null)

    // FIX: Explicitly type the StateFlow for better compiler stability
    val books: StateFlow<List<BookEntity>> = bookDao.getAllBooks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun importBook(uri: Uri) {
        viewModelScope.launch {
            // 1. Duplicate Check
            if (books.value.any { it.uriString == uri.toString() }) {
                snackbarMessage.value = "Book is already in your bookshelf"
                return@launch
            }

            try {
                // 2. Persist URI permission for long-term access
                val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (e: Exception) {
                // Permission might already be granted
            }

            // 3. Open Publication to extract metadata and cover
            val publication = readiumManager.openEpubFromUri(uri)

            if (publication != null) {
                val title = publication.metadata.title ?: "Unknown Title"
                val author = publication.metadata.authors.firstOrNull()?.name ?: "Unknown Author"

                // 4. Extract and Save Cover Image
                var savedCoverPath: String? = null
                try {
                    // Use 'runCatching' or a direct call as it is a suspending function
                    val bitmap = readiumManager.getPublicationCover(publication)
                    if (bitmap != null) {
                        savedCoverPath = saveCoverToInternalStorage(bitmap, title)
                    }
                } catch (e: Exception) {
                    // If cover extraction fails, we still want to save the book
                }

                // 5. Insert into Database
                val newBook = BookEntity(
                    title = title,
                    author = author,
                    uriString = uri.toString(),
                    coverPath = savedCoverPath
                )
                bookDao.insertBook(newBook)

                // 6. Mandatory Cleanup
                readiumManager.closePublication()
            } else {
                snackbarMessage.value = "Failed to open book metadata"
            }
        }
    }

    private fun saveCoverToInternalStorage(bitmap: Bitmap, title: String): String {
        val filename = "cover_${title.hashCode()}.png"
        val file = File(context.filesDir, filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file.absolutePath
    }

    fun deleteBook(id: Long) {
        viewModelScope.launch {
            // Optional: You could also delete the cover file here to save space
            bookDao.deleteBook(id)
        }
    }

}