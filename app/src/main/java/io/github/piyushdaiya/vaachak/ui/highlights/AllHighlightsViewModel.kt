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

package io.github.piyushdaiya.vaachak.ui.highlights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.piyushdaiya.vaachak.data.local.BookDao
import io.github.piyushdaiya.vaachak.data.local.HighlightDao
import io.github.piyushdaiya.vaachak.data.local.HighlightEntity
import io.github.piyushdaiya.vaachak.data.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AllHighlightsViewModel @Inject constructor(
    private val highlightDao: HighlightDao,
    private val bookDao: BookDao,
    private val settingsRepo: SettingsRepository // Inject Settings
) : ViewModel() {

    private val _selectedTag = MutableStateFlow("All")
    val selectedTag: StateFlow<String> = _selectedTag.asStateFlow()

    // 1. Expose E-ink State for UI adaptation
    val isEinkEnabled: StateFlow<Boolean> = settingsRepo.isEinkEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // 2. Group Highlights by Book Title
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

    // 3. Chip Data
    data class TagWithCount(val name: String, val count: Int)

    val availableTags: StateFlow<List<TagWithCount>> = combine(
        highlightDao.getAllHighlights(),
        highlightDao.getAllUniqueTags()
    ) { highlights, uniqueTags ->
        val allChip = TagWithCount("All", highlights.size)
        val specificChips = uniqueTags.map { tag ->
            TagWithCount(tag, highlights.count { it.tag == tag })
        }
        listOf(allChip) + specificChips
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf(TagWithCount("All", 0)))

    fun updateFilter(tag: String) {
        _selectedTag.value = tag
    }

    fun deleteHighlight(id: Long) {
        viewModelScope.launch {
            highlightDao.deleteHighlightById(id)
        }
    }
}