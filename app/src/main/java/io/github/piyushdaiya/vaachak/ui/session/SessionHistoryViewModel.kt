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

package io.github.piyushdaiya.vaachak.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.piyushdaiya.vaachak.data.local.BookDao
import io.github.piyushdaiya.vaachak.data.local.HighlightDao
import io.github.piyushdaiya.vaachak.data.local.BookEntity
import io.github.piyushdaiya.vaachak.data.local.HighlightEntity
import io.github.piyushdaiya.vaachak.data.repository.AiRepository
import io.github.piyushdaiya.vaachak.data.repository.SettingsRepository // Ensure this import is correct
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Session History screen.
 * Manages the retrieval and generation of recall summaries for recently read books.
 */
@HiltViewModel
class SessionHistoryViewModel @Inject constructor(
    private val bookDao: BookDao,
    private val highlightDao: HighlightDao,
    private val aiRepository: AiRepository,
    private val settingsRepository: SettingsRepository // FIXED: Injected here
) : ViewModel() {

    private val _recallMap = MutableStateFlow<Map<String, String>>(emptyMap())
    /**
     * A map of book titles to their generated recall summaries.
     */
    val recallMap: StateFlow<Map<String, String>> = _recallMap.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    /**
     * State flow indicating whether a recall generation is in progress.
     */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _recentBooks = MutableStateFlow<List<BookEntity>>(emptyList())
    /**
     * List of recently read books considered for recall generation.
     */
    val recentBooks: StateFlow<List<BookEntity>> = _recentBooks.asStateFlow()

    /**
     * Triggers the generation of recall summaries for the top 5 recently read books.
     * Fetches highlights, generates a summary using AI, and optionally saves it as a highlight.
     */
    fun triggerGlobalRecall() {
        viewModelScope.launch {
            _isLoading.value = true
            val books = bookDao.getAllBooks().first()
                .filter { it.progress > 0.0 }
                .sortedByDescending { it.id }
                .take(5)

            _recentBooks.value = books

            books.forEach { book ->
                launch {
                    try {
                        val highlights = highlightDao.getHighlightsForBook(book.uriString)
                            .first()
                            .take(10)
                            .joinToString("\n") { it.text }

                        val summary = aiRepository.getRecallSummary(
                            bookTitle = book.title,
                            highlightsContext = highlights
                        )

                        // Check setting before auto-saving
                        if (settingsRepository.isAutoSaveRecapsEnabled.first()) {
                            saveRecapAsHighlight(book, summary)
                        }

                        _recallMap.update { it + (book.title to summary) }
                    } catch (e: Exception) {
                        // Log error or update UI state
                    }
                }
            }
            _isLoading.value = false
        }
    }

    private fun saveRecapAsHighlight(book: BookEntity, summary: String) {
        viewModelScope.launch {
            val recapHighlight = HighlightEntity(
                publicationId = book.uriString,
                locatorJson = book.lastLocationJson ?: "",
                text = summary,
                color = -0x333334, // Gray for E-ink
                tag = "Recaps",
                created = System.currentTimeMillis()
            )
            highlightDao.insertHighlight(recapHighlight)
        }
    }
}