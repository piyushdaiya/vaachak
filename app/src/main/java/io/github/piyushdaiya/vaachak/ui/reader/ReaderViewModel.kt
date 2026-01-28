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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import org.readium.r2.navigator.Decoration
import org.readium.r2.shared.ExperimentalReadiumApi
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class, ExperimentalReadiumApi::class)
@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val aiRepository: AiRepository,
    private val readiumManager: ReadiumManager,
    private val highlightDao: HighlightDao,
    private val bookDao: BookDao
) : ViewModel() {
    private var pendingJumpLocator: String?=null
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
    val currentBookHighlights: StateFlow<List<Decoration>> = viewModelScope.launch {
        // We use a backing property or the existing _publication flow
    }.let {
        publication
            .filterNotNull()
            .flatMapLatest { pub ->
                // Explicitly fetch highlights for the current book URI
                highlightDao.getHighlightsForBook(initialUri ?: "")
            }
            .map { entities: List<HighlightEntity> ->
                entities.map { entity ->
                    Decoration(
                        id = entity.id.toString(),
                        locator = Locator.fromJSON(JSONObject(entity.locatorJson))
                            ?: throw Exception("Invalid Locator JSON"),
                        style = Decoration.Style.Highlight(tint = android.graphics.Color.YELLOW)
                    )
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }
    // Explicitly typed StateFlows to resolve "Cannot infer type" errors
    private val _showTagSelector = MutableStateFlow<Boolean>(false)
    val showTagSelector: StateFlow<Boolean> = _showTagSelector.asStateFlow()

    private var pendingHighlightLocator: Locator? = null

    val initialLocator: StateFlow<Locator?> = _initialLocator.asStateFlow()
    fun setInitialLocation(json: String?) {
        // Store this so when Readium is ready, you can call navigator.go(locator)
        this.pendingJumpLocator = json
    }
    fun onFileSelected(uri: Uri) {
        initialUri = uri.toString()
        viewModelScope.launch {
            // 1. Update lastRead timestamp in DB
            bookDao.updateLastRead(uri.toString(), System.currentTimeMillis())

            // 2. Fetch the book to get saved location
            val book = bookDao.getBookByUri(uri.toString())
            // PRIORITY LOGIC: Use pending jump (from highlight) OR saved location
            val targetJson = pendingJumpLocator ?: book?.lastLocationJson

            val locator = targetJson?.let {
                try {
                    Locator.fromJSON(org.json.JSONObject(it))
                } catch (e: Exception) { null }
            }

            _initialLocator.value = locator

            // 3. Open the publication
            val pub = readiumManager.openEpubFromUri(uri)
            _publication.value = pub
            // Clear pending jump so it doesn't affect future sessions
            pendingJumpLocator = null
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

    fun saveHighlightWithTag(tag: String) {
        val locator = pendingHighlightLocator ?: return
        val currentUri = initialUri ?: return // Ensure you have the book URI stored

        viewModelScope.launch {
            val highlight = HighlightEntity(
                publicationId = currentUri,
                locatorJson = locator.toJSON().toString(),
                text = locator.text.highlight ?: "Selected Text",
                color = android.graphics.Color.YELLOW,
                tag = tag
            )
            highlightDao.insertHighlight(highlight)
            dismissTagSelector()
        }
    }

    fun deleteHighlight(id: Long) {
        viewModelScope.launch {
            highlightDao.deleteHighlightById(id)
        }
    }

    fun prepareHighlight(locator: Locator) {
        pendingHighlightLocator = locator
        _showTagSelector.value = true
    }

    fun dismissTagSelector() {
        _showTagSelector.value = false
        pendingHighlightLocator = null
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
//Recap functionality
private val _recapText = MutableStateFlow<String?>(null)
    val recapText: StateFlow<String?> = _recapText.asStateFlow()

    private val _isRecapLoading = MutableStateFlow(false)
    val isRecapLoading: StateFlow<Boolean> = _isRecapLoading.asStateFlow()

    fun generateRecap(currentPageText: String) {
        val pub = publication.value ?: return
        val title = pub.metadata.title ?: "this book"

        viewModelScope.launch {
            _isRecapLoading.value = true
            try {
                // 1. Fetch the last 10 highlights for context
                val highlights = highlightDao.getHighlightsForBook(initialUri ?: "")
                    .first() // Get current snapshot
                    .take(10)
                    .joinToString("\n") { "- ${it.text}" }

                // 2. Call the AI Repository
                val response = aiRepository.generateRecap(
                    bookTitle = title,
                    highlightsContext = highlights,
                    currentPageText = currentPageText
                )

                _recapText.value = response
            } catch (e: Exception) {
                _recapText.value = "Failed to generate recap: ${e.localizedMessage}"
            } finally {
                _isRecapLoading.value = false
            }
        }
    }

    fun dismissRecap() {
        _recapText.value = null
    }
    fun saveRecapToHighlights(recapContent: String) {
        val pub = publication.value ?: return
        val locator = currentLocator.value ?: return

        viewModelScope.launch {
            val recapHighlight = HighlightEntity(
                publicationId = initialUri ?: "",
                // Use the current page location so clicking the recap takes you there
                locatorJson = locator.toJSON().toString(),
                text = "[RECAP]: $recapContent",
                color = android.graphics.Color.LTGRAY, // Distinct color for recaps
                tag = "Recaps", // The specific tag you requested
                created = System.currentTimeMillis()
            )
            highlightDao.insertHighlight(recapHighlight)
            dismissRecap() // Close the dialog after saving
        }
    }

}