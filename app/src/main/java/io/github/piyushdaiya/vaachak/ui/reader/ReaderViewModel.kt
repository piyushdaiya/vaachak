package io.github.piyushdaiya.vaachak.ui.reader

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.piyushdaiya.vaachak.data.local.BookDao
import io.github.piyushdaiya.vaachak.data.local.HighlightDao
import io.github.piyushdaiya.vaachak.data.local.HighlightEntity
import io.github.piyushdaiya.vaachak.data.repository.AiRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.readium.r2.shared.publication.Locator // ONLY THIS LOCATOR
import org.readium.r2.shared.publication.Publication
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val aiRepository: AiRepository,
    private val readiumManager: ReadiumManager,
    private val highlightDao: HighlightDao,
    private val bookDao: BookDao
) : ViewModel() {
    private var initialUri: String? = null
    private var currentSelectedText = ""

    private val _publication = MutableStateFlow<Publication?>(null)
    val publication: StateFlow<Publication?> = _publication

    private val _currentPageInfo = MutableStateFlow("Page 1")
    val currentPageInfo = _currentPageInfo.asStateFlow()

    private val _currentLocator = MutableStateFlow<Locator?>(null)
    val currentLocator: StateFlow<Locator?> = _currentLocator.asStateFlow()

    // AI and UI States... (Keep your existing AI code here)
    private val _isBottomSheetVisible = MutableStateFlow(false)
    val isBottomSheetVisible = _isBottomSheetVisible.asStateFlow()
    private val _aiResponse = MutableStateFlow("")
    val aiResponse = _aiResponse.asStateFlow()
    private val _isImageResponse = MutableStateFlow(false)
    val isImageResponse = _isImageResponse.asStateFlow()
    // Add a state to hold the initial locator for the navigator
    private val _initialLocator = MutableStateFlow<Locator?>(null)
    // Highlights for the current book
    val currentBookHighlights: StateFlow<List<HighlightEntity>> = _publication
        .filterNotNull()
        .flatMapLatest { pub ->
            highlightDao.getHighlightsForBook(initialUri ?: "")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val initialLocator: StateFlow<Locator?> = _initialLocator.asStateFlow()
    fun onFileSelected(uri: Uri) {
        initialUri = uri.toString()
        viewModelScope.launch {
            // 1. Update lastRead timestamp in DB
            bookDao.updateLastRead(uri.toString(), System.currentTimeMillis())

            // 2. Fetch the book to get saved location
            val book = bookDao.getBookByUri(uri.toString())
            val savedLocator = book?.lastLocationJson?.let {
                Locator.fromJSON(org.json.JSONObject(it))
            }
            _initialLocator.value = savedLocator

            // 3. Open the publication
            val pub = readiumManager.openEpubFromUri(uri)
            _publication.value = pub
        }
    }

    // FIX: Combined progress logic to avoid Pagination reference
    fun updateProgress(locator: Locator) {
        // Update live state for Footer
        val position = locator.locations.position ?: 0
        // totalProgression is the preferred 0.0 - 1.0 for the whole book
        val progression = locator.locations.totalProgression ?: locator.locations.progression ?: 0.0
        val percent = (progression * 100).toInt()

        _currentPageInfo.value = if (position > 0) "Page $position ($percent%)" else "$percent% completed"

        val uri = initialUri ?: return
        viewModelScope.launch {
            bookDao.updateLastLocation(uri, locator.toJSON().toString())
            bookDao.updateProgress(uri, progression)
        }
    }

    fun saveHighlight(locator: Locator, color: Int) {
        val uri = initialUri ?: return
        viewModelScope.launch {
            val entity = HighlightEntity(
                publicationId = uri,
                locatorJson = locator.toJSON().toString(),
                color = color,
                text = locator.text.highlight ?: ""
            )
            highlightDao.insertHighlight(entity)
        }
    }

    fun deleteHighlight(id: Long) {
        viewModelScope.launch {
            highlightDao.deleteHighlightById(id)
        }
    }

    fun onTextSelected(text: String) {
        currentSelectedText = text
        _aiResponse.value = ""
        _isBottomSheetVisible.value = true
    }

    fun dismissBottomSheet() {
        _isBottomSheetVisible.value = false
    }

    fun closeBook() {
        viewModelScope.launch {
            readiumManager.closePublication()
            _publication.value = null
            _currentLocator.value = null
        }
    }

    // AI Actions (Unchanged)
    fun onActionExplain() = viewModelScope.launch {
        _isImageResponse.value = false
        _aiResponse.value = "Thinking..."
        _aiResponse.value = aiRepository.explainContext(currentSelectedText)
    }
    fun onActionWhoIsThis() = viewModelScope.launch {
        _isImageResponse.value = false
        _aiResponse.value = "Investigating character..."
        val bookTitle = _publication.value?.metadata?.title ?: "Unknown Book"
        val bookAuthor = _publication.value?.metadata?.authors?.firstOrNull()?.name ?: "Unknown Author"
        _aiResponse.value = aiRepository.whoIsThis(currentSelectedText, bookTitle, bookAuthor)
    }
    fun onActionVisualize() = viewModelScope.launch {
        _aiResponse.value = "Generating image..."
        val result = aiRepository.visualizeText(currentSelectedText)
        if (result.startsWith("BASE64_IMAGE:")) {
            _isImageResponse.value = true
            _aiResponse.value = result.removePrefix("BASE64_IMAGE:")
        } else {
            _isImageResponse.value = false
            _aiResponse.value = result
        }
    }
}