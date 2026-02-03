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
import io.github.piyushdaiya.vaachak.data.repository.AiRepository
import io.github.piyushdaiya.vaachak.data.repository.SettingsRepository
import io.github.piyushdaiya.vaachak.ui.reader.ReadiumManager
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
    /**
     * Indicates if E-ink optimization is enabled.
     */
    val isEinkEnabled: StateFlow<Boolean> = settingsRepo.isEinkEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // NEW: Offline Mode State
    /**
     * Indicates if offline mode is enabled.
     */
    val isOfflineModeEnabled: StateFlow<Boolean> = settingsRepo.isOfflineModeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // --- STATE: SNACKBAR ---
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    /**
     * A message to be displayed in a Snackbar.
     */
    val snackbarMessage = _snackbarMessage.asStateFlow()

    /**
     * Clears the current Snackbar message.
     */
    fun clearSnackbarMessage() { _snackbarMessage.value = null }

    // --- STATE: BOOKS ---
    /**
     * A list of all books in the library, sorted by recent activity.
     */
    val allBooks: StateFlow<List<BookEntity>> = bookDao.getAllBooksSortedByRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    /**
     * The current search query for filtering books.
     */
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DATE_ADDED)
    /**
     * The current sort order for the library.
     */
    val sortOrder = _sortOrder.asStateFlow()

    /**
     * A filtered and sorted list of books that have not been started (progress <= 0).
     */
    val filteredLibraryBooks: StateFlow<List<BookEntity>> =
        combine(allBooks, searchQuery, _sortOrder) { books, query, order ->
            val filtered = books.filter { book -> book.progress <= 0.0 && book.title.contains(query, ignoreCase = true) }
            when (order) {
                SortOrder.TITLE -> filtered.sortedBy { it.title }
                SortOrder.AUTHOR -> filtered.sortedBy { it.author }
                SortOrder.DATE_ADDED -> filtered.sortedByDescending { it.addedDate }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * A list of recently read books (progress > 0), sorted by last read time.
     */
    val recentBooks: StateFlow<List<BookEntity>> = allBooks.map { books ->
        books.filter { it.progress > 0.0 }.sortedByDescending { it.lastRead }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- RECAP STATE ---
    private val _recapState = MutableStateFlow<Map<String, String>>(emptyMap())
    /**
     * A map of book URIs to their generated recap summaries.
     */
    val recapState: StateFlow<Map<String, String>> = _recapState.asStateFlow()

    private val _isLoadingRecap = MutableStateFlow<String?>(null)
    /**
     * The URI of the book for which a recap is currently being generated, or null if none.
     */
    val isLoadingRecap: StateFlow<String?> = _isLoadingRecap.asStateFlow()

    // --- ACTIONS ---

    /**
     * Updates the search query.
     *
     * @param query The new search query.
     */
    fun updateSearchQuery(query: String) { _searchQuery.value = query }

    /**
     * Updates the sort order.
     *
     * @param order The new sort order.
     */
    fun updateSortOrder(order: SortOrder) { _sortOrder.value = order }

    /**
     * Imports a book from a URI into the library.
     * Extracts metadata and cover image.
     *
     * @param uri The URI of the book file.
     */
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

    /**
     * Deletes a book from the library.
     *
     * @param id The ID of the book to delete.
     */

    fun deleteBookByUri(uri: String) = viewModelScope.launch { bookDao.deleteBookByUri(uri) }

    /**
     * Generates a quick recap for a book using AI.
     *
     * @param book The book to generate a recap for.
     */
    fun getQuickRecap(book: BookEntity) {
        // FAILSAFE: Block if Offline
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

    /**
     * Clears the generated recap for a specific book.
     *
     * @param uri The URI of the book.
     */
    fun clearRecap(uri: String) { _recapState.value = _recapState.value - uri }
}