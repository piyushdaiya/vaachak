package io.github.piyushdaiya.vaachak.ui.reader

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.piyushdaiya.vaachak.data.local.BookDao
import io.github.piyushdaiya.vaachak.data.local.HighlightDao
import io.github.piyushdaiya.vaachak.data.local.HighlightEntity
import io.github.piyushdaiya.vaachak.data.repository.AiRepository
import io.github.piyushdaiya.vaachak.data.repository.DictionaryRepository
import io.github.piyushdaiya.vaachak.data.repository.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.readium.r2.navigator.Decoration
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalReadiumApi::class)
@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val aiRepository: AiRepository,
    private val readiumManager: ReadiumManager,
    private val highlightDao: HighlightDao,
    private val settingsRepo: SettingsRepository,
    private val dictionaryRepository: DictionaryRepository,
    private val bookDao: BookDao
) : ViewModel() {

    // --- 1. SETTINGS & THEME STATE ---
    // Exposed so ReaderScreen can pass 'isEink' to Header/Footer
    val isEinkEnabled: StateFlow<Boolean> = settingsRepo.isEinkEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // --- 2. READER STATE ---
    private val _publication = MutableStateFlow<Publication?>(null)
    val publication: StateFlow<Publication?> = _publication.asStateFlow()

    private val _currentLocator = MutableStateFlow<Locator?>(null)
    val currentLocator: StateFlow<Locator?> = _currentLocator.asStateFlow()

    private val _currentPageInfo = MutableStateFlow("Page 1")
    val currentPageInfo = _currentPageInfo.asStateFlow()

    // Initial jump location (from Highlights or Last Read)
    private val _initialLocator = MutableStateFlow<Locator?>(null)
    val initialLocator: StateFlow<Locator?> = _initialLocator.asStateFlow()

    // --- 3. UI OVERLAY STATES ---
    private val _isBottomSheetVisible = MutableStateFlow(false)
    val isBottomSheetVisible = _isBottomSheetVisible.asStateFlow()

    private val _aiResponse = MutableStateFlow("")
    val aiResponse = _aiResponse.asStateFlow()

    private val _isImageResponse = MutableStateFlow(false)
    val isImageResponse = _isImageResponse.asStateFlow()

    private val _isDictionaryLookup = MutableStateFlow(false)
    val isDictionaryLookup = _isDictionaryLookup.asStateFlow()

    private val _isDictionaryLoading = MutableStateFlow(false)
    val isDictionaryLoading = _isDictionaryLoading.asStateFlow()

    private val _showTagSelector = MutableStateFlow(false)
    val showTagSelector = _showTagSelector.asStateFlow()

    private val _recapText = MutableStateFlow<String?>(null)
    val recapText: StateFlow<String?> = _recapText.asStateFlow()

    private val _isRecapLoading = MutableStateFlow(false)
    val isRecapLoading: StateFlow<Boolean> = _isRecapLoading.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage = _snackbarMessage.asStateFlow()

    // Internal Variables
    private var initialUri: String? = null
    private var currentSelectedText = ""
    private var pendingJumpLocator: String? = null
    private var pendingHighlightLocator: Locator? = null


    // --- 4. HIGHLIGHTS FLOW (Optimized for Theme) ---
    // Combines Publication + Highlights + E-ink Setting
    // In E-ink mode, we could technically change decoration styles (e.g. Underline vs Tint)
    val currentBookHighlights: StateFlow<List<Decoration>> = combine(
        _publication.filterNotNull(),
        isEinkEnabled
    ) { pub, isEink ->
        Pair(pub, isEink)
    }.flatMapLatest { (pub, isEink) ->
        highlightDao.getHighlightsForBook(initialUri ?: "")
            .map { entities ->
                entities.map { entity ->
                    Decoration(
                        id = entity.id.toString(),
                        locator = Locator.fromJSON(JSONObject(entity.locatorJson))
                            ?: throw Exception("Invalid Locator"),
                        // In E-ink, Yellow tint might fade. We keep it standard,
                        // but you could switch to Decoration.Style.Underline here if desired.
                        style = Decoration.Style.Highlight(
                            tint = if (isEink) Color.LTGRAY else entity.color
                        )
                    )
                }
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // --- 5. INITIALIZATION ---
    fun setInitialLocation(json: String?) {
        this.pendingJumpLocator = json
    }

    fun onFileSelected(uri: Uri) {
        initialUri = uri.toString()
        viewModelScope.launch {
            // A. Update timestamp
            bookDao.updateLastRead(uri.toString(), System.currentTimeMillis())

            // B. Resolve start location (Highlight Jump > Saved Progress > Start)
            val book = bookDao.getBookByUri(uri.toString())
            val targetJson = pendingJumpLocator ?: book?.lastLocationJson

            val locator = targetJson?.let {
                try { Locator.fromJSON(JSONObject(it)) } catch (e: Exception) { null }
            }
            _initialLocator.value = locator

            // C. Open Book (Optimized Manager)
            val pub = readiumManager.openEpubFromUri(uri)
            _publication.value = pub
            pendingJumpLocator = null
        }
    }

    // --- 6. NAVIGATION UPDATES ---
    fun updateProgress(locator: Locator) {
        val position = locator.locations.position ?: 0
        val progression = locator.locations.totalProgression ?: 0.0
        val percent = (progression * 100).toInt()

        _currentPageInfo.value = if (position > 0) "Page $position ($percent%)" else "$percent% completed"

        val uri = initialUri ?: return
        viewModelScope.launch {
            bookDao.updateLastLocation(uri, locator.toJSON().toString())
            bookDao.updateProgress(uri, progression)
        }
    }

    fun closeBook() {
        viewModelScope.launch {
            readiumManager.closePublication()
            _publication.value = null
            _currentLocator.value = null
        }
    }

    // --- 7. HIGHLIGHT ACTIONS ---
    fun prepareHighlight(locator: Locator) {
        pendingHighlightLocator = locator
        _showTagSelector.value = true
    }

    fun saveHighlightWithTag(tag: String) {
        val locator = pendingHighlightLocator ?: return
        val currentUri = initialUri ?: return
        val isEink = isEinkEnabled.value

        viewModelScope.launch {
            val highlight = HighlightEntity(
                publicationId = currentUri,
                locatorJson = locator.toJSON().toString(),
                text = locator.text.highlight ?: "Selected Text",
                // E-ink Optimization: Save as Dark Gray/Black for database consistency
                // Standard: Yellow
                color = if (isEink) Color.DKGRAY else Color.YELLOW,
                tag = tag
            )
            highlightDao.insertHighlight(highlight)
            dismissTagSelector()
        }
    }

    fun deleteHighlight(id: Long) {
        viewModelScope.launch { highlightDao.deleteHighlightById(id) }
    }

    fun dismissTagSelector() {
        _showTagSelector.value = false
        pendingHighlightLocator = null
    }

    // --- 8. AI & DICTIONARY ---
    fun onTextSelected(text: String) {
        _isDictionaryLookup.value = false
        currentSelectedText = text
        _aiResponse.value = ""
        _isBottomSheetVisible.value = true
    }

    fun onActionExplain() = viewModelScope.launch {
        performAiAction("Thinking...") {
            aiRepository.explainContext(currentSelectedText)
        }
    }

    fun onActionWhoIsThis() = viewModelScope.launch {
        performAiAction("Investigating...") {
            val title = _publication.value?.metadata?.title ?: ""
            val author = _publication.value?.metadata?.authors?.firstOrNull()?.name ?: ""
            aiRepository.whoIsThis(currentSelectedText, title, author)
        }
    }

    fun onActionVisualize() = viewModelScope.launch {
        _isDictionaryLookup.value = false
        _isDictionaryLoading.value = true
        _isImageResponse.value = true
        _isBottomSheetVisible.value = true
        _aiResponse.value = ""

        try {
            val result = aiRepository.visualizeText(currentSelectedText)
            if (result.startsWith("BASE64_IMAGE:")) {
                _aiResponse.value = result.removePrefix("BASE64_IMAGE:")
            } else {
                _isImageResponse.value = false
                _aiResponse.value = result
            }
        } catch (e: Exception) {
            _isImageResponse.value = false
            _aiResponse.value = "Error: ${e.localizedMessage}"
        } finally {
            _isDictionaryLoading.value = false
        }
    }

    // Helper to reduce boilerplate
    private suspend fun performAiAction(loadingMsg: String, action: suspend () -> String) {
        _isDictionaryLookup.value = false
        _isDictionaryLoading.value = true
        _isImageResponse.value = false
        _aiResponse.value = loadingMsg
        try {
            _aiResponse.value = action()
        } catch (e: Exception) {
            _aiResponse.value = "Error: ${e.message}"
        } finally {
            _isDictionaryLoading.value = false
        }
    }

    fun lookupWord(word: String, context: Context) {
        val trimmedWord = word.trim()
        if (trimmedWord.split(Regex("\\s+")).size > 5) {
            _isBottomSheetVisible.value = true
            _isDictionaryLookup.value = true
            _aiResponse.value = "Selection too long for dictionary."
            return
        }

        viewModelScope.launch {
            val isEmbedded = settingsRepo.getUseEmbeddedDictionary().first()
            _isDictionaryLookup.value = isEmbedded

            if (isEmbedded) {
                _isBottomSheetVisible.value = true
                _aiResponse.value = "Searching..."
                _isDictionaryLoading.value = true
                try {
                    val def = dictionaryRepository.getDefinition(word)
                    _aiResponse.value = def ?: "No definition found for '$word'."
                } catch (e: Exception) {
                    _aiResponse.value = "Error: ${e.message}"
                } finally {
                    _isDictionaryLoading.value = false
                }
            } else {
                launchExternalDictionary(word, context)
            }
        }
    }

    private fun launchExternalDictionary(word: String, context: Context) {
        try {
            val intent = android.content.Intent("colordict.intent.action.SEARCH").apply {
                putExtra("EXTRA_QUERY", word)
                putExtra("EXTRA_FULLSCREEN", false)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            _snackbarMessage.value = "Dictionary app not found."
        }
    }

    // --- 9. RECAP ---
    fun dismissRecap() { _recapText.value = null }
    fun dismissBottomSheet() { _isBottomSheetVisible.value = false }
}