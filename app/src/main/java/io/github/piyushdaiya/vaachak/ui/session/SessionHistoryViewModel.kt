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

@HiltViewModel
class SessionHistoryViewModel @Inject constructor(
    private val bookDao: BookDao,
    private val highlightDao: HighlightDao,
    private val aiRepository: AiRepository,
    private val settingsRepository: SettingsRepository // FIXED: Injected here
) : ViewModel() {

    private val _recallMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val recallMap: StateFlow<Map<String, String>> = _recallMap.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _recentBooks = MutableStateFlow<List<BookEntity>>(emptyList())
    val recentBooks: StateFlow<List<BookEntity>> = _recentBooks.asStateFlow()

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