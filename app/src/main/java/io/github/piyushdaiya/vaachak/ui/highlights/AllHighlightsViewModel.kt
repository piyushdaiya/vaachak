package io.github.piyushdaiya.vaachak.ui.highlights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.piyushdaiya.vaachak.data.local.BookDao
import io.github.piyushdaiya.vaachak.data.local.HighlightDao
import io.github.piyushdaiya.vaachak.data.local.HighlightEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AllHighlightsViewModel @Inject constructor(
    private val highlightDao: HighlightDao,
    private val bookDao: BookDao // Added to map URIs to Titles
) : ViewModel() {

    // Combine highlights and books to show Titles instead of URLs
    val groupedHighlights: StateFlow<Map<String, List<HighlightEntity>>> =
        combine(
            highlightDao.getAllHighlights(),
            bookDao.getAllBooks()
        ) { highlights, books ->
            // Create a lookup map: URI -> Title
            val titleMap = books.associate { it.uriString to it.title }

            // Group highlights by their mapped title, falling back to the ID if not found
            highlights.groupBy { entity ->
                titleMap[entity.publicationId] ?: "Unknown Book"
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    fun deleteHighlight(id: Long) {
        viewModelScope.launch {
            highlightDao.deleteHighlightById(id)
        }
    }
}