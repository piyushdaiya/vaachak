package io.github.piyushdaiya.vaachak.ui.highlights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.piyushdaiya.vaachak.data.local.BookDao
import io.github.piyushdaiya.vaachak.data.local.HighlightDao
import io.github.piyushdaiya.vaachak.data.local.HighlightEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AllHighlightsViewModel @Inject constructor(
    private val highlightDao: HighlightDao,
    private val bookDao: BookDao,
   ) : ViewModel() {

    private val _selectedTag = MutableStateFlow("All")
    val selectedTag: StateFlow<String> = _selectedTag.asStateFlow()

    // Fetch unique tags and prepend "All" for filtering

    // Reactive pipeline combining Highlights, Books (for titles), and Filter selection
    val groupedHighlights: StateFlow<Map<String, List<HighlightEntity>>> =
        combine(
            highlightDao.getAllHighlights(),
            bookDao.getAllBooks(),
            _selectedTag
        ) { highlights, books, tag ->
            val titleMap = books.associate { it.uriString to it.title }

            val filteredHighlights = if (tag == "All") {
                highlights
            } else {
                highlights.filter { it.tag == tag }
            }

            filteredHighlights.groupBy { entity ->
                titleMap[entity.publicationId] ?: "Unknown Book"
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    fun updateFilter(tag: String) {
        _selectedTag.value = tag
    }

    fun deleteHighlight(id: Long) {
        viewModelScope.launch {
            highlightDao.deleteHighlightById(id)
        }
    }
    // 1. Create a simple data class for the chip data
    data class TagWithCount(val name: String, val count: Int)

    val availableTags: StateFlow<List<TagWithCount>> = combine(
        highlightDao.getAllHighlights(),
        highlightDao.getAllUniqueTags()
    ) { highlights, uniqueTags ->
        // Calculate total for "All"
        val allChip = TagWithCount("All", highlights.size)

        // Calculate counts for each specific tag
        val specificChips = uniqueTags.map { tag ->
            TagWithCount(tag, highlights.count { it.tag == tag })
        }

        listOf(allChip) + specificChips
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf(TagWithCount("All", 0)))




}