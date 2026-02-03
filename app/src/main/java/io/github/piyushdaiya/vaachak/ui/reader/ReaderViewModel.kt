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

package io.github.piyushdaiya.vaachak.ui.reader

import android.graphics.Color
import android.net.Uri
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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.TextAlign
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.search.SearchService
import javax.inject.Inject

/**
 * ViewModel for the Reader screen.
 * Manages the state of the e-book reader, including loading publications,
 * handling navigation, managing settings, and interacting with AI features.
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalReadiumApi::class, FlowPreview::class)
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
    val isEinkEnabled: StateFlow<Boolean> = settingsRepo.isEinkEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isOfflineModeEnabled: StateFlow<Boolean> = settingsRepo.isOfflineModeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _bookAiEnabled = MutableStateFlow(true)
    val isAiEnabled = combine(isOfflineModeEnabled, _bookAiEnabled) { _, local -> local }
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    // --- 2. READER PREFERENCES ---
    //
    val epubPreferences: StateFlow<EpubPreferences> = combine(
        settingsRepo.readerTheme as Flow<Any?>,         // 0
        settingsRepo.readerFontFamily as Flow<Any?>,    // 1
        settingsRepo.readerFontSize as Flow<Any?>,      // 2
        settingsRepo.readerTextAlign as Flow<Any?>,     // 3
        settingsRepo.readerLineHeight as Flow<Any?>,    // 4
        settingsRepo.readerPublisherStyles as Flow<Any?>,// 5
        settingsRepo.readerLetterSpacing as Flow<Any?>, // 6
        settingsRepo.readerParaSpacing as Flow<Any?>,   // 7
        settingsRepo.readerMarginSide as Flow<Any?>     // 8
    ) { params ->
        buildEpubPreferences(params)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, EpubPreferences())


    // --- 3. READER STATE ---
    private val _publication = MutableStateFlow<Publication?>(null)
    val publication: StateFlow<Publication?> = _publication.asStateFlow()
    private val _currentLocator = MutableStateFlow<Locator?>(null)
    val currentLocator: StateFlow<Locator?> = _currentLocator.asStateFlow()
    private val _currentPageInfo = MutableStateFlow("Page 1")
    val currentPageInfo = _currentPageInfo.asStateFlow()
    private val _initialLocator = MutableStateFlow<Locator?>(null)
    val initialLocator: StateFlow<Locator?> = _initialLocator.asStateFlow()
    private val _showToc = MutableStateFlow(false)
    val showToc: StateFlow<Boolean> = _showToc.asStateFlow()
    private val _navigationEvent = MutableSharedFlow<Link>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    // --- OVERLAYS ---
    private val _showReaderSettings = MutableStateFlow(false)
    val showReaderSettings: StateFlow<Boolean> = _showReaderSettings.asStateFlow()

    private val _showSearch = MutableStateFlow(false)
    val showSearch: StateFlow<Boolean> = _showSearch.asStateFlow()
    private val _bookSearchQuery = MutableStateFlow("")
    val bookSearchQuery: StateFlow<String> = _bookSearchQuery.asStateFlow()
    private val _isBookSearching = MutableStateFlow(false)
    val isBookSearching: StateFlow<Boolean> = _isBookSearching.asStateFlow()
    private val _searchResults = MutableStateFlow<List<Locator>>(emptyList())
    val searchResults: StateFlow<List<Locator>> = _searchResults.asStateFlow()
    private val _showHighlights = MutableStateFlow(true)
    val showHighlights: StateFlow<Boolean> = _showHighlights.asStateFlow()
    private val _jumpEvent = MutableSharedFlow<Locator>()
    val jumpEvent = _jumpEvent.asSharedFlow()

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

    private val _showRecapConfirmation = MutableStateFlow(false)
    val showRecapConfirmation: StateFlow<Boolean> = _showRecapConfirmation.asStateFlow()
    private val _recapText = MutableStateFlow<String?>(null)
    val recapText: StateFlow<String?> = _recapText.asStateFlow()
    private val _isRecapLoading = MutableStateFlow(false)
    val isRecapLoading: StateFlow<Boolean> = _isRecapLoading.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage = _snackbarMessage.asStateFlow()

    private val _currentBookId = MutableStateFlow<String?>(null)
    private var currentSelectedText = ""
    private var pendingJumpLocator: String? = null
    private var pendingHighlightLocator: Locator? = null

    val bookmarksList: StateFlow<List<HighlightEntity>> = _currentBookId.filterNotNull()
        .flatMapLatest { id -> highlightDao.getHighlightsForBook(id) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Highlights Flow
    val currentBookHighlights: StateFlow<List<Decoration>> = combine(
        bookmarksList, isEinkEnabled
    ) { highlights, isEink ->
        highlights.mapNotNull { entity ->
            try {
                val locator = Locator.fromJSON(JSONObject(entity.locatorJson))
                if (locator != null) {
                    Decoration(
                        id = entity.id.toString(),
                        locator = locator,
                        style = Decoration.Style.Highlight(tint = if (isEink) Color.LTGRAY else entity.color)
                    )
                } else null
            } catch (_: Exception) { null }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())


    // --- INITIALIZATION ---

    fun setInitialLocation(json: String?) { this.pendingJumpLocator = json }

    fun onFileSelected(uri: Uri) {
        _bookSearchQuery.value = ""
        _searchResults.value = emptyList()
        _showSearch.value = false
        _isBookSearching.value = false
        _showHighlights.value = false
        _recapText.value = null
        _showRecapConfirmation.value = false
        _showReaderSettings.value = false

        viewModelScope.launch {
            val isGlobalOffline = settingsRepo.isOfflineModeEnabled.first()
            _bookAiEnabled.value = !isGlobalOffline
        }

        _currentBookId.value = uri.toString()
        viewModelScope.launch {
            bookDao.updateLastRead(uri.toString(), System.currentTimeMillis())
            val book = bookDao.getBookByUri(uri.toString())
            val targetJson = pendingJumpLocator ?: book?.lastLocationJson
            val locator = targetJson?.let { try { Locator.fromJSON(JSONObject(it)) } catch (_: Exception) { null } }
            _initialLocator.value = locator
            val pub = readiumManager.openEpubFromUri(uri)
            _publication.value = pub
            pendingJumpLocator = null
        }
    }

    // --- SETTINGS ACTIONS ---

    fun toggleReaderSettings() { _showReaderSettings.value = !_showReaderSettings.value }
    fun dismissReaderSettings() { _showReaderSettings.value = false }
    fun toggleBookAi(enabled: Boolean) { _bookAiEnabled.value = enabled }


    // --- NEW: Save All Preferences (For "Save" button) ---

    fun savePreferences(newPrefs: EpubPreferences) = viewModelScope.launch {
        settingsRepo.updateReaderPreferences(
            theme = newPrefs.theme?.toString()?.lowercase(),
            fontFamily = newPrefs.fontFamily?.toString(),
            fontSize = newPrefs.fontSize,
            textAlign = newPrefs.textAlign?.toString()?.lowercase(),
            publisherStyles = newPrefs.publisherStyles,
            letterSpacing = newPrefs.letterSpacing,
            lineHeight = newPrefs.lineHeight,
            paraSpacing = newPrefs.paragraphSpacing,
            marginSide = newPrefs.pageMargins
        )
    }

    // FIX: Using Array<Any?> to prevent intersection type errors
    // FIX: Using safe cast (as?) and defaults (?:) to prevent NullPointerException
    private fun buildEpubPreferences(params: Array<Any?>): EpubPreferences {
        // Safe unpacking with defaults
        val themeStr = (params[0] as? String) ?: "light"
        val fontStr = params[1] as? String // Nullable is okay
        val fontSizeVal = (params[2] as? Double) ?: 1.0
        val alignStr = (params[3] as? String) ?: "start"

        // Correct Index Mapping based on combine() order above:
        // 4 is LineHeight (Double), NOT PubStyles (Boolean)
        val lineht = (params[4] as? Double) ?: 1.2
        val pubStyles = (params[5] as? Boolean) ?: true
        val letterSp = (params[6] as? Double)
        val paraSp = (params[7] as? Double)
        val marginSide = (params[8] as? Double) ?: 1.0

        val rTheme = when(themeStr) { "dark" -> Theme.DARK; "sepia" -> Theme.SEPIA; else -> Theme.LIGHT }
        val rFont = fontStr?.let { FontFamily(it) }
        val rAlign = when(alignStr) { "left" -> TextAlign.LEFT; "justify" -> TextAlign.JUSTIFY; else -> TextAlign.START }

        return EpubPreferences(
            theme = rTheme,
            fontSize = fontSizeVal,
            publisherStyles = pubStyles,
            fontFamily = if (!pubStyles) rFont else null,
            textAlign = if (!pubStyles) rAlign else null,
            letterSpacing = if (!pubStyles) letterSp else null,
            paragraphSpacing = if (!pubStyles) paraSp else null,
            pageMargins = if (!pubStyles) marginSide else null,
            lineHeight = if (!pubStyles) lineht else null
        )
    }

    // --- RECAP & AI ACTIONS ---

    fun onRecapClicked() { _showRecapConfirmation.value = true }
    fun dismissRecapConfirmation() { _showRecapConfirmation.value = false }

    fun getQuickRecap() {
        if (!_bookAiEnabled.value) return
        _showRecapConfirmation.value = false
        _isRecapLoading.value = true

        viewModelScope.launch {
            val title = _publication.value?.metadata?.title ?: "Unknown Book"
            val currentContext = "Current Position: ${_currentPageInfo.value}"
            val highlights = highlightDao.getHighlightsForBook(_currentBookId.value ?: "")
                .first()
                .take(10)
                .joinToString("\n") { "- ${it.text}" }
            val summary = aiRepository.generateRecap(title, highlights, currentContext)
            _recapText.value = summary
            _isRecapLoading.value = false
        }
    }

    fun saveRecapAsHighlight() {
        val summary = _recapText.value ?: return
        val currentUri = _currentBookId.value ?: return
        val locator = _currentLocator.value ?: return
        val isEink = isEinkEnabled.value

        viewModelScope.launch {
            val highlight = HighlightEntity(
                publicationId = currentUri,
                locatorJson = locator.toJSON().toString(),
                text = "RECAP: $summary",
                color = if (isEink) Color.DKGRAY else Color.CYAN,
                tag = "recap"
            )
            highlightDao.insertHighlight(highlight)
            _recapText.value = null
            _snackbarMessage.value = "Recap saved to highlights."
        }
    }

    fun dismissRecapResult() { _recapText.value = null }


    // --- NAVIGATION ---

    fun toggleToc() { _showToc.value = !_showToc.value }

    fun onTocItemSelected(link: Link) {
        val currentHref = _currentLocator.value?.href.toString().substringBefore('#')
        val linkHref = link.href.toString().substringBefore('#')
        if (currentHref == linkHref && !link.href.toString().contains('#')) {
            _showToc.value = false
        } else {
            viewModelScope.launch { _navigationEvent.emit(link) }
            _showToc.value = false
        }
    }

    fun toggleSearch() { if (_showSearch.value) { _bookSearchQuery.value = ""; _searchResults.value = emptyList() }; _showSearch.value = !_showSearch.value }
    fun toggleHighlights() { _showHighlights.value = !_showHighlights.value }

    fun searchInBook(query: String) {
        val pub = _publication.value ?: return
        val sanitized = query.filter { !it.isISOControl() }.trim()
        if (sanitized.length > 100) { _snackbarMessage.value = "Query too long"; return }
        _bookSearchQuery.value = sanitized
        if (sanitized.isBlank()) { _searchResults.value = emptyList(); return }
        _isBookSearching.value = true; _searchResults.value = emptyList()
        viewModelScope.launch {
            try {
                val svc = pub.findService(SearchService::class)
                if (svc == null) { _snackbarMessage.value = "Search not supported"; _isBookSearching.value = false; return@launch }
                val iter = svc.search(sanitized)
                val res = mutableListOf<Locator>()
                while(res.size<50){ val c = iter.next().getOrNull()?:break; res.addAll(c.locators); if(c.locators.isEmpty()) break }
                _searchResults.value = res
            } catch(e:Exception){ _snackbarMessage.value = e.message } finally { _isBookSearching.value = false }
        }
    }

    fun onSearchResultClicked(l: Locator) { viewModelScope.launch { _showSearch.value=false; _jumpEvent.emit(l); _bookSearchQuery.value=""; _searchResults.value=emptyList() } }

    fun onHighlightClicked(h: HighlightEntity) { viewModelScope.launch { try{ val l=Locator.fromJSON(JSONObject(h.locatorJson)); if(l!=null){ _showHighlights.value=false; _jumpEvent.emit(l) } }catch(_:Exception){} } }

    fun updateProgress(l: Locator) {
        _currentLocator.value = l
        val pos = l.locations.position?:0; val prog = l.locations.totalProgression?:0.0; val pct = (prog*100).toInt()
        _currentPageInfo.value = if(pos>0) "Page $pos ($pct%)" else "$pct% completed"
        val u = _currentBookId.value?:return
        viewModelScope.launch { bookDao.updateLastLocation(u, l.toJSON().toString()); bookDao.updateProgress(u, prog) }
    }

    fun closeBook() { viewModelScope.launch { readiumManager.closePublication(); _publication.value=null; _currentLocator.value=null; _bookSearchQuery.value=""; _searchResults.value=emptyList(); _showHighlights.value=false } }

    fun prepareHighlight(l: Locator) { pendingHighlightLocator = l; _showTagSelector.value = true }

    fun saveHighlightWithTag(tag: String) {
        val l = pendingHighlightLocator?:return; val u = _currentBookId.value?:return; val e = isEinkEnabled.value
        viewModelScope.launch {
            highlightDao.insertHighlight(
                HighlightEntity(publicationId = u, locatorJson = l.toJSON().toString(), text = l.text.highlight?:"Selected", color = if(e) Color.DKGRAY else Color.YELLOW, tag = tag)
            )
            dismissTagSelector()
        }
    }

    fun deleteHighlight(id: Long) { viewModelScope.launch { highlightDao.deleteHighlightById(id) } }
    fun dismissTagSelector() { _showTagSelector.value = false; pendingHighlightLocator = null }

    fun onTextSelected(t: String) { currentSelectedText = t; _isBottomSheetVisible.value = true }

    fun onActionExplain() {
        _isDictionaryLookup.value = false
        _isDictionaryLoading.value = false
        if (!_bookAiEnabled.value) return
        viewModelScope.launch { performAiAction("Thinking...") { aiRepository.explainContext(currentSelectedText) } }
    }

    fun onActionWhoIsThis() {
        _isDictionaryLookup.value = false
        _isDictionaryLoading.value = false
        if (!_bookAiEnabled.value) return
        viewModelScope.launch { performAiAction("Investigating...") { aiRepository.whoIsThis(currentSelectedText, _publication.value?.metadata?.title?:"", "") } }
    }

    fun onActionVisualize() {
        _isDictionaryLookup.value = false
        _isDictionaryLoading.value = false
        if (!_bookAiEnabled.value) return
        viewModelScope.launch { performAiAction("Drawing...") { aiRepository.visualizeText(currentSelectedText) }; _isImageResponse.value = true }
    }
    private suspend fun performAiAction(m: String, a: suspend () -> String) { _aiResponse.value = m; try { _aiResponse.value = a() } catch(e:Exception){ _aiResponse.value = e.message?:"" } }

    fun dismissRecap() { _recapText.value = null }

    fun dismissBottomSheet() {
        _isBottomSheetVisible.value = false
        clearAiState()
    }

    private fun clearAiState() {
        _aiResponse.value = ""
        _isImageResponse.value = false
        _isDictionaryLookup.value = false
        _isDictionaryLoading.value = false
    }

    fun clearSnackbar() { _snackbarMessage.value = null }

    //Dictionary
    fun lookupWord(word: String) {
        val trimmedWord = word.trim()
        val wordCount = trimmedWord.split(Regex("\\s+")).size
        if (wordCount > 5 || trimmedWord.length > 50) {
            _isBottomSheetVisible.value = true
            _isDictionaryLookup.value = true
            _isDictionaryLoading.value = false
            _aiResponse.value = "Selection too long for dictionary. Please select a single word, or use 'Ask AI' for sentences."
            return
        }
        viewModelScope.launch {
            val isEmbedded = settingsRepo.getUseEmbeddedDictionary().first()
            _isDictionaryLookup.value = true
            var definition: String? = null
            if (isEmbedded) {
                _isBottomSheetVisible.value = true
                _aiResponse.value = "Searching device dictionaries..."
                _isDictionaryLoading.value = true
                try {
                    definition = dictionaryRepository.getDefinition(word)
                    if (definition != null) {
                        _aiResponse.value = definition
                    } else {
                        _aiResponse.value = "No definition found for '$word' in StarDict or local JSON dictionary."
                    }
                    _isDictionaryLoading.value = false
                } catch (e: Exception) {
                    _aiResponse.value = "Error searching local dictionaries. error is ${e.message}"
                } finally {
                    _isDictionaryLoading.value = false
                }
            } else {
                _isBottomSheetVisible.value = true
                _aiResponse.value = "Searching embedded directory..."
                _isDictionaryLoading.value = true
                definition = dictionaryRepository.getjsonDefinition(word)
                if (definition != null) {
                    _aiResponse.value = definition
                } else {
                    _aiResponse.value = "No definition found for '$word' in embedded json dictionary."
                }
                _isDictionaryLoading.value = false
            }
        }
    }
}