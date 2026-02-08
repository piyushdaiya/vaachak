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

package io.github.piyushdaiya.vaachak.ui.bookshelf

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.piyushdaiya.vaachak.data.local.BookDao
import io.github.piyushdaiya.vaachak.data.local.BookEntity
import io.github.piyushdaiya.vaachak.data.local.HighlightDao
import io.github.piyushdaiya.vaachak.data.local.HighlightEntity
import io.github.piyushdaiya.vaachak.data.repository.AiRepository
import io.github.piyushdaiya.vaachak.data.repository.SettingsRepository
import io.github.piyushdaiya.vaachak.ui.reader.ReadiumManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * Enum representing the sorting order for books.
 */
enum class SortOrder { TITLE, AUTHOR, DATE_ADDED }

/**
 * ViewModel for the Bookshelf screen.
 * Manages the list of books, importing new books, sorting, and generating recaps.
 */
@HiltViewModel
class BookshelfViewModel @Inject constructor(
    private val bookDao: BookDao,
    private val readiumManager: ReadiumManager,
    private val highlightDao: HighlightDao,
    private val aiRepository: AiRepository,
    private val settingsRepo: SettingsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // --- STATE: THEME ---
    val isEinkEnabled: StateFlow<Boolean> = settingsRepo.isEinkEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // NEW: Offline Mode State
    val isOfflineModeEnabled: StateFlow<Boolean> = settingsRepo.isOfflineModeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // --- STATE: SNACKBAR ---
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage = _snackbarMessage.asStateFlow()

    fun clearSnackbarMessage() { _snackbarMessage.value = null }

    // --- STATE: BOOKS ---
    val allBooks: StateFlow<List<BookEntity>> = bookDao.getAllBooksSortedByRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DATE_ADDED)
    val sortOrder = _sortOrder.asStateFlow()

    val filteredLibraryBooks: StateFlow<List<BookEntity>> =
        combine(allBooks, searchQuery, _sortOrder) { books, query, order ->
            val filtered = books.filter { book -> (book.progress <= 0.0 || book.progress >= 0.99) && book.title.contains(query, ignoreCase = true) }
            when (order) {
                SortOrder.TITLE -> filtered.sortedBy { it.title }
                SortOrder.AUTHOR -> filtered.sortedBy { it.author }
                SortOrder.DATE_ADDED -> filtered.sortedByDescending { it.addedDate }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentBooks: StateFlow<List<BookEntity>> = allBooks.map { books ->
        books.filter { it.progress > 0.0 && it.progress < 0.99 }.sortedByDescending { it.lastRead }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- RECAP STATE ---
    private val _recapState = MutableStateFlow<Map<String, String>>(emptyMap())
    val recapState: StateFlow<Map<String, String>> = _recapState.asStateFlow()

    private val _isLoadingRecap = MutableStateFlow<String?>(null)
    val isLoadingRecap: StateFlow<String?> = _isLoadingRecap.asStateFlow()

    // --- NEW: BOOKMARKS SHEET STATE ---
    private val _bookmarksSheetBookUri = MutableStateFlow<String?>(null)
    /**
     * The URI of the book currently selected for viewing bookmarks.
     * If null, the bookmarks sheet is hidden.
     */
    val bookmarksSheetBookUri = _bookmarksSheetBookUri.asStateFlow()

    /**
     * A filtered list of bookmarks (tag="BOOKMARK") for the currently selected book.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedBookBookmarks: StateFlow<List<HighlightEntity>> = _bookmarksSheetBookUri
        .flatMapLatest { uri ->
            if (uri == null) {
                flowOf(emptyList())
            } else {
                highlightDao.getHighlightsForBook(uri).map { list ->
                    list.filter { it.tag == "BOOKMARK" }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // --- ACTIONS ---

    fun updateSearchQuery(query: String) { _searchQuery.value = query }
    fun updateSortOrder(order: SortOrder) { _sortOrder.value = order }

    fun importBook(uri: Uri) {
        viewModelScope.launch {
            if (allBooks.value.any { it.uriString == uri.toString() }) {
                _snackbarMessage.value = "⚠️ Book is already in your library"
                return@launch
            }
            try { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) { }

            val publication = readiumManager.openEpubFromUri(uri)
            if (publication != null) {
                val title = publication.metadata.title ?: "Unknown Title"
                val author = publication.metadata.authors.firstOrNull()?.name ?: "Unknown Author"
                var savedCoverPath: String? = null
                try {
                    val bitmap = readiumManager.getPublicationCover(publication)
                    if (bitmap != null) savedCoverPath = saveCoverToInternalStorage(bitmap, title)
                } catch (_: Exception) { }

                val newBook = BookEntity(title = title, author = author, uriString = uri.toString(), coverPath = savedCoverPath, addedDate = System.currentTimeMillis(), lastRead = System.currentTimeMillis(), progress = 0.0)
                bookDao.insertBook(newBook)
                readiumManager.closePublication()
                _snackbarMessage.value = "Book added successfully"
            } else { _snackbarMessage.value = "Failed to parse book metadata" }
        }
    }

    private fun saveCoverToInternalStorage(bitmap: Bitmap, title: String): String {
        val filename = "cover_${title.hashCode()}.png"
        val file = File(context.filesDir, filename)
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        return file.absolutePath
    }

    fun deleteBookByUri(uri: String) = viewModelScope.launch { bookDao.deleteBookByUri(uri) }

    fun getQuickRecap(book: BookEntity) {
        if (isOfflineModeEnabled.value) {
            _snackbarMessage.value = "Offline Mode enabled. Connect to use Recall."
            return
        }
        viewModelScope.launch {
            _isLoadingRecap.value = book.uriString
            try {
                val contextHighlights = highlightDao.getHighlightsForBook(book.uriString).first().take(10).joinToString("\n") { it.text }
                val summary= aiRepository.getRecallSummary(book.title, contextHighlights)
                _recapState.value = _recapState.value + (book.uriString to summary)
            } finally { _isLoadingRecap.value = null }
        }
    }

    fun clearRecap(uri: String) { _recapState.value = _recapState.value - uri }

    // --- NEW: BOOKMARKS ACTIONS ---

    /**
     * Opens the bookmarks sheet for a specific book.
     */
    fun openBookmarksSheet(uri: String) {
        _bookmarksSheetBookUri.value = uri
    }

    /**
     * Closes the bookmarks sheet.
     */
    fun dismissBookmarksSheet() {
        _bookmarksSheetBookUri.value = null
    }
}