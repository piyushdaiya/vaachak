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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

// Define the sorting options
enum class SortOrder {
    TITLE, AUTHOR, DATE_ADDED
}
@HiltViewModel
class BookshelfViewModel @Inject constructor(
    private val bookDao: BookDao,
    private val readiumManager: ReadiumManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val snackbarMessage: MutableState<String?> = mutableStateOf<String?>(null)

    // 1. Full stream of books sorted by recent
    val allBooks: StateFlow<List<BookEntity>> = bookDao.getAllBooksSortedByRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 2. Search Query State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()


    // 3. Filtered Library Logic
    // This combines allBooks and the search query to produce the grid list

    private val _sortOrder = MutableStateFlow(SortOrder.DATE_ADDED)
    val sortOrder = _sortOrder.asStateFlow()

    fun updateSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }
    val filteredLibraryBooks: StateFlow<List<BookEntity>> =
        combine(allBooks, searchQuery, _sortOrder) { books, query, order ->
            val filtered = books.filter { book ->
                book.progress <= 0.0 && book.title.contains(query, ignoreCase = true)
            }

            when (order) {
                SortOrder.TITLE -> filtered.sortedBy { it.title }
                SortOrder.AUTHOR -> filtered.sortedBy { it.author }
                SortOrder.DATE_ADDED -> filtered.sortedByDescending { it.addedDate }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 4. Update Search Query
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // 5. Hero Card Logic (Continue Reading)
    val recentBooks: StateFlow<List<BookEntity>> = allBooks.map { books ->
        books.filter { it.progress > 0.0 }
            .sortedByDescending { it.lastRead }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Book Management Methods ---

    fun importBook(uri: Uri) {
        viewModelScope.launch {
            if (allBooks.value.any { it.uriString == uri.toString() }) {
                snackbarMessage.value = "Book is already in your bookshelf"
                return@launch
            }

            try {
                val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (e: Exception) { /* Log error or ignore if already granted */ }

            val publication = readiumManager.openEpubFromUri(uri)
            if (publication != null) {
                val title = publication.metadata.title ?: "Unknown Title"
                val author = publication.metadata.authors.firstOrNull()?.name ?: "Unknown Author"

                var savedCoverPath: String? = null
                try {
                    val bitmap = readiumManager.getPublicationCover(publication)
                    if (bitmap != null) {
                        savedCoverPath = saveCoverToInternalStorage(bitmap, title)
                    }
                } catch (e: Exception) { /* Handle cover failure */ }

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
            bookDao.deleteBook(id)
        }
    }
}