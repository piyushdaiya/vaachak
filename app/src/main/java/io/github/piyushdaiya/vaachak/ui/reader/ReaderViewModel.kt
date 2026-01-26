package io.github.piyushdaiya.vaachak.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.piyushdaiya.vaachak.data.local.HighlightDao
import io.github.piyushdaiya.vaachak.data.local.HighlightEntity
import io.github.piyushdaiya.vaachak.data.repository.AiRepository // Using YOUR existing repo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val readiumManager: ReadiumManager,
    private val highlightDao: HighlightDao,
    private val aiRepository: AiRepository
) : ViewModel() {

    val publication: StateFlow<Publication?> = readiumManager.publication

    // --- UI State ---
    private val _isBottomSheetVisible = MutableStateFlow(false)
    val isBottomSheetVisible = _isBottomSheetVisible.asStateFlow()

    private val _aiResponse = MutableStateFlow<String>("")
    val aiResponse = _aiResponse.asStateFlow()

    private val _isImageResponse = MutableStateFlow(false)
    val isImageResponse = _isImageResponse.asStateFlow()

    // Store the selected text
    private var currentSelectedText: String = ""

    // --- Highlights Stream ---
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentBookHighlights: StateFlow<List<HighlightEntity>> = readiumManager.publication
        .flatMapLatest { pub: Publication? ->
            if (pub == null) {
                flowOf(emptyList())
            } else {
                val bookId = pub.metadata.identifier ?: pub.metadata.title ?: "unknown_book"
                highlightDao.getHighlightsForBook(bookId)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- File Actions ---
    fun onFileSelected(uri: android.net.Uri) {
        viewModelScope.launch { readiumManager.openEpubFromUri(uri) }
    }

    fun saveHighlight(locator: Locator, color: Int) {
        viewModelScope.launch {
            val pub = publication.value ?: return@launch
            val bookId = pub.metadata.identifier ?: pub.metadata.title ?: "unknown_book"
            val entity = HighlightEntity(
                publicationId = bookId,
                locatorJson = locator.toJSON().toString(),
                text = locator.text.highlight ?: "",
                color = color
            )
            highlightDao.insertHighlight(entity)
        }
    }

    // --- AI Actions ---

    fun onTextSelected(text: String) {
        this.currentSelectedText = text
        _aiResponse.value = ""
        _isImageResponse.value = false
        _isBottomSheetVisible.value = true
    }

    fun dismissBottomSheet() {
        _isBottomSheetVisible.value = false
    }

    // 1. EXPLAIN
    fun onActionExplain() {
        if (currentSelectedText.isBlank()) return
        viewModelScope.launch {
            _aiResponse.value = "Thinking..."
            _isImageResponse.value = false

            val result = aiRepository.explainContext(currentSelectedText)
            _aiResponse.value = result
        }
    }

    // 2. WHO IS THIS
    fun onActionWhoIsThis() {
        if (currentSelectedText.isBlank()) return
        viewModelScope.launch {
            _aiResponse.value = "Reading context..."
            _isImageResponse.value = false

            // Get context from metadata
            val pub = publication.value
            val title = pub?.metadata?.title ?: "Unknown Book"
            // Readium authors is a list, we grab the first one
            val author = pub?.metadata?.authors?.firstOrNull()?.name ?: "Unknown Author"

            val result = aiRepository.whoIsThis(currentSelectedText, title, author)
            _aiResponse.value = result
        }
    }

    // 3. VISUALIZE
    fun onActionVisualize() {
        if (currentSelectedText.isBlank()) return
        viewModelScope.launch {
            _aiResponse.value = "Generating image..."
            _isImageResponse.value = true

            val result = aiRepository.visualizeText(currentSelectedText)

            // Logic to handle Base64 image
            if (result.startsWith("BASE64_IMAGE:")) {
                _aiResponse.value = result.removePrefix("BASE64_IMAGE:")
                // _isImageResponse is already true, so AiBottomSheet will render it
            } else {
                // If it failed and returned text (error or fallback), switch back to text mode
                _isImageResponse.value = false
                _aiResponse.value = result
            }
        }
    }
    // --- NEW FUNCTION ---
    fun deleteHighlight(highlightId: Long) {
        viewModelScope.launch {
            highlightDao.deleteHighlightById(highlightId)
            // The 'currentBookHighlights' flow will automatically emit the new list,
            // updating the UI and removing the visual decoration.
        }
    }
}