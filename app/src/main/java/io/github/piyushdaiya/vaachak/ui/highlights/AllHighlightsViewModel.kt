package io.github.piyushdaiya.vaachak.ui.highlights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.piyushdaiya.vaachak.data.local.HighlightDao
import io.github.piyushdaiya.vaachak.data.local.HighlightEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AllHighlightsViewModel @Inject constructor(
    private val highlightDao: HighlightDao
) : ViewModel() {

    // Get all highlights and group them by book title (publicationId)
    val groupedHighlights: StateFlow<Map<String, List<HighlightEntity>>> = highlightDao.getAllHighlights()
        .map { list -> list.groupBy { it.publicationId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun deleteHighlight(id: Long) {
        viewModelScope.launch {
            highlightDao.deleteHighlightById(id)
        }
    }
}