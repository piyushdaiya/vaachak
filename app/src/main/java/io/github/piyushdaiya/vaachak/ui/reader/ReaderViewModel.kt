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

import android.content.Context
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
    val isAiEnabled: StateFlow<Boolean> = combine(isOfflineModeEnabled, _bookAiEnabled) { globalOffline, bookOverride ->
        bookOverride
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)


    // --- 2. READER PREFERENCES ---
    val epubPreferences: StateFlow<EpubPreferences> = combine(
        settingsRepo.readerTheme,
        settingsRepo.readerFontFamily,
        settingsRepo.readerFontSize,
        settingsRepo.readerTextAlign,
        settingsRepo.readerPublisherStyles,
        settingsRepo.readerLetterSpacing,
        settingsRepo.readerParaSpacing,
        settingsRepo.readerMarginSide,
        settingsRepo.readerMarginTop,
        settingsRepo.readerMarginBottom
    ) { params ->
        val themeStr = params[0] as String
        val fontStr = params[1] as? String
        val fontSizeVal = params[2] as Double
        val alignStr = params[3] as String
        val pubStyles = params[4] as Boolean
        val letterSp = params[5] as? Double
        val paraSp = params[6] as? Double
        val marginSide = params[7] as Double

        val rTheme = when(themeStr) {
            "dark" -> Theme.DARK
            "sepia" -> Theme.SEPIA
            else -> Theme.LIGHT
        }

        val rFont = fontStr?.let { FontFamily(it) }
        val rAlign = when(alignStr) {
            "left" -> TextAlign.LEFT
            "justify" -> TextAlign.JUSTIFY
            else -> TextAlign.START
        }

        EpubPreferences(
            theme = rTheme,
            fontSize = fontSizeVal,
            publisherStyles = pubStyles,
            fontFamily = if (!pubStyles) rFont else null,
            textAlign = if (!pubStyles) rAlign else null,
            letterSpacing = if (!pubStyles) letterSp else null,
            paragraphSpacing = if (!pubStyles) paraSp else null,
            pageMargins = if (!pubStyles) marginSide else null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EpubPreferences())


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
    private val _showHighlights = MutableStateFlow(false)
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

    private var initialUri: String? = null
    private var currentSelectedText = ""
    private var pendingJumpLocator: String? = null
    private var pendingHighlightLocator: Locator? = null

    // Highlights Flow
    val currentBookHighlights: StateFlow<List<Decoration>> = combine(
        _publication.filterNotNull(),
        isEinkEnabled
    ) { pub, isEink -> Pair(pub, isEink) }.flatMapLatest { (pub, isEink) ->
        highlightDao.getHighlightsForBook(initialUri ?: "")
            .map { entities ->
                entities.map { entity ->
                    Decoration(
                        id = entity.id.toString(),
                        locator = Locator.fromJSON(JSONObject(entity.locatorJson))!!,
                        style = Decoration.Style.Highlight(tint = if (isEink) Color.LTGRAY else entity.color)
                    )
                }
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarksList: StateFlow<List<HighlightEntity>> = _publication.filterNotNull()
        .flatMapLatest { highlightDao.getHighlightsForBook(initialUri ?: "") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- INITIALIZATION ---

    /**
     * Sets the initial location for the book to open at.
     *
     * @param json The JSON string representation of the Locator.
     */
    fun setInitialLocation(json: String?) { this.pendingJumpLocator = json }

    /**
     * Called when a file is selected to be opened.
     * Initializes the book, loads preferences, and sets up the initial state.
     *
     * @param uri The URI of the selected file.
     */
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

        initialUri = uri.toString()
        viewModelScope.launch {
            bookDao.updateLastRead(uri.toString(), System.currentTimeMillis())
            val book = bookDao.getBookByUri(uri.toString())
            val targetJson = pendingJumpLocator ?: book?.lastLocationJson
            val locator = targetJson?.let { try { Locator.fromJSON(JSONObject(it)) } catch (e: Exception) { null } }
            _initialLocator.value = locator
            val pub = readiumManager.openEpubFromUri(uri)
            _publication.value = pub
            pendingJumpLocator = null
        }
    }

    // --- SETTINGS ACTIONS ---

    /**
     * Toggles the visibility of the reader settings overlay.
     */
    fun toggleReaderSettings() { _showReaderSettings.value = !_showReaderSettings.value }

    /**
     * Dismisses the reader settings overlay.
     */
    fun dismissReaderSettings() { _showReaderSettings.value = false }

    /**
     * Toggles the AI features for the current book.
     *
     * @param enabled True to enable AI features, false otherwise.
     */
    fun toggleBookAi(enabled: Boolean) { _bookAiEnabled.value = enabled }

    /**
     * Updates the reader theme.
     *
     * @param theme The new theme identifier (e.g., "dark", "sepia", "light").
     */
    fun updateTheme(theme: String) = viewModelScope.launch { settingsRepo.updateReaderPreferences(theme = theme) }

    /**
     * Updates the font family used in the reader.
     *
     * @param font The name of the font family.
     */
    fun updateFontFamily(font: String) = viewModelScope.launch { settingsRepo.updateReaderPreferences(fontFamily = font) }

    /**
     * Updates the font size.
     *
     * @param size The new font size.
     */
    fun updateFontSize(size: Double) = viewModelScope.launch { settingsRepo.updateReaderPreferences(fontSize = size) }

    /**
     * Updates whether publisher styles should be respected.
     *
     * @param enabled True to use publisher styles, false to override them.
     */
    fun updatePublisherStyles(enabled: Boolean) = viewModelScope.launch { settingsRepo.updateReaderPreferences(publisherStyles = enabled) }

    /**
     * Updates the text alignment.
     *
     * @param align The alignment type (e.g., "left", "justify").
     */
    fun updateTextAlign(align: String) = viewModelScope.launch { settingsRepo.updateReaderPreferences(textAlign = align) }

    /**
     * Updates the letter spacing.
     *
     * @param value The new letter spacing value.
     */
    fun updateLetterSpacing(value: Double) = viewModelScope.launch { settingsRepo.updateReaderPreferences(letterSpacing = value) }

    /**
     * Updates the paragraph spacing.
     *
     * @param value The new paragraph spacing value.
     */
    fun updateParaSpacing(value: Double) = viewModelScope.launch { settingsRepo.updateReaderPreferences(paraSpacing = value) }

    /**
     * Updates the side margin.
     *
     * @param value The new side margin value.
     */
    fun updateMarginSide(value: Double) = viewModelScope.launch { settingsRepo.updateReaderPreferences(marginSide = value) }

    /**
     * Updates the top margin.
     *
     * @param value The new top margin value.
     */
    fun updateMarginTop(value: Double) = viewModelScope.launch { settingsRepo.updateReaderPreferences(marginTop = value) }

    /**
     * Updates the bottom margin.
     *
     * @param value The new bottom margin value.
     */
    fun updateMarginBottom(value: Double) = viewModelScope.launch { settingsRepo.updateReaderPreferences(marginBottom = value) }

    /**
     * Resets the reader layout to default settings.
     */
    fun resetLayout() = viewModelScope.launch { settingsRepo.resetReaderLayout() }

    // --- NEW: Save All Preferences (For "Save" button) ---

    /**
     * Saves all reader preferences at once.
     *
     * @param newPrefs The new EpubPreferences object containing the settings to save.
     */
    fun savePreferences(newPrefs: EpubPreferences) = viewModelScope.launch {
        settingsRepo.updateReaderPreferences(
            theme = newPrefs.theme?.toString()?.lowercase(),
            fontFamily = newPrefs.fontFamily?.toString(),
            fontSize = newPrefs.fontSize,
            textAlign = newPrefs.textAlign?.toString()?.lowercase(),
            publisherStyles = newPrefs.publisherStyles,
            letterSpacing = newPrefs.letterSpacing,
            paraSpacing = newPrefs.paragraphSpacing,
            marginSide = newPrefs.pageMargins
            // Add top/bottom here if your SettingsRepo implementation supports distinct keys
        )
    }

    // --- RECAP & AI ACTIONS ---

    /**
     * Shows the confirmation dialog for generating a recap.
     */
    fun onRecapClicked() { _showRecapConfirmation.value = true }

    /**
     * Dismisses the recap confirmation dialog.
     */
    fun dismissRecapConfirmation() { _showRecapConfirmation.value = false }

    /**
     * Generates a quick recap of the current book context using AI.
     */
    fun generateRecap() {
        if (!_bookAiEnabled.value) return
        _showRecapConfirmation.value = false
        _isRecapLoading.value = true

        viewModelScope.launch {
            val title = _publication.value?.metadata?.title ?: "Unknown Book"
            val currentContext = "Current Position: ${_currentPageInfo.value}"
            val quickrecap = aiRepository.getQuickRecap(title, currentContext)
            _recapText.value = quickrecap
            _isRecapLoading.value = false
        }
    }

    /**
     * Saves the generated recap as a highlight in the book.
     */
    fun saveRecapAsHighlight() {
        val summary = _recapText.value ?: return
        val currentUri = initialUri ?: return
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

    /**
     * Dismisses the recap result.
     */
    fun dismissRecapResult() { _recapText.value = null }


    // --- NAVIGATION ---

    /**
     * Toggles the visibility of the Table of Contents (TOC).
     */
    fun toggleToc() { _showToc.value = !_showToc.value }

    /**
     * Handles selection of an item from the Table of Contents.
     *
     * @param link The link associated with the selected TOC item.
     */
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

    /**
     * Toggles the visibility of the search interface.
     */
    fun toggleSearch() { if (_showSearch.value) { _bookSearchQuery.value = ""; _searchResults.value = emptyList() }; _showSearch.value = !_showSearch.value }

    /**
     * Toggles the visibility of the highlights list.
     */
    fun toggleHighlights() { _showHighlights.value = !_showHighlights.value }

    /**
     * Searches for a query string within the book.
     *
     * @param query The text to search for.
     */
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

    /**
     * Handles clicking on a search result.
     *
     * @param l The locator of the search result.
     */
    fun onSearchResultClicked(l: Locator) { viewModelScope.launch { _showSearch.value=false; _jumpEvent.emit(l); _bookSearchQuery.value=""; _searchResults.value=emptyList() } }

    /**
     * Handles clicking on a highlight.
     *
     * @param h The highlight entity clicked.
     */
    fun onHighlightClicked(h: HighlightEntity) { viewModelScope.launch { try{ val l=Locator.fromJSON(JSONObject(h.locatorJson)); if(l!=null){ _showHighlights.value=false; _jumpEvent.emit(l) } }catch(e:Exception){} } }

    /**
     * Updates the current reading progress.
     *
     * @param l The current locator.
     */
    fun updateProgress(l: Locator) {
        _currentLocator.value = l
        val pos = l.locations.position?:0; val prog = l.locations.totalProgression?:0.0; val pct = (prog*100).toInt()
        _currentPageInfo.value = if(pos>0) "Page $pos ($pct%)" else "$pct% completed"
        val u = initialUri?:return
        viewModelScope.launch { bookDao.updateLastLocation(u, l.toJSON().toString()); bookDao.updateProgress(u, prog) }
    }

    /**
     * Closes the current book and resets state.
     */
    fun closeBook() { viewModelScope.launch { readiumManager.closePublication(); _publication.value=null; _currentLocator.value=null; _bookSearchQuery.value=""; _searchResults.value=emptyList(); _showHighlights.value=false } }

    /**
     * Prepares to add a highlight at the specified locator.
     *
     * @param l The locator where the highlight should be added.
     */
    fun prepareHighlight(l: Locator) { pendingHighlightLocator = l; _showTagSelector.value = true }

    /**
     * Saves the pending highlight with the selected tag.
     *
     * @param tag The tag to associate with the highlight.
     */
    fun saveHighlightWithTag(tag: String) {
        val l = pendingHighlightLocator?:return; val u = initialUri?:return; val e = isEinkEnabled.value
        viewModelScope.launch {
            highlightDao.insertHighlight(
                HighlightEntity(publicationId = u, locatorJson = l.toJSON().toString(), text = l.text.highlight?:"Selected", color = if(e) Color.DKGRAY else Color.YELLOW, tag = tag)
            )
            dismissTagSelector()
        }
    }

    /**
     * Deletes a highlight by its ID.
     *
     * @param id The ID of the highlight to delete.
     */
    fun deleteHighlight(id: Long) { viewModelScope.launch { highlightDao.deleteHighlightById(id) } }

    /**
     * Dismisses the tag selector dialog.
     */
    fun dismissTagSelector() { _showTagSelector.value = false; pendingHighlightLocator = null }

    /**
     * Handles text selection in the reader.
     *
     * @param t The selected text.
     */
    fun onTextSelected(t: String) { currentSelectedText = t; _isBottomSheetVisible.value = true }

    /**
     * Triggers the "Explain" AI action for the selected text.
     */
    fun onActionExplain() {
        if (!_bookAiEnabled.value) return
        viewModelScope.launch { performAiAction("Thinking...") { aiRepository.explainContext(currentSelectedText) } }
    }

    /**
     * Triggers the "Who is this?" AI action for the selected text.
     */
    fun onActionWhoIsThis() {
        if (!_bookAiEnabled.value) return
        viewModelScope.launch { performAiAction("Investigating...") { aiRepository.whoIsThis(currentSelectedText, _publication.value?.metadata?.title?:"", "") } }
    }

    /**
     * Triggers the "Visualize" AI action for the selected text.
     */
    fun onActionVisualize() {
        if (!_bookAiEnabled.value) return
        viewModelScope.launch { performAiAction("Drawing...") { aiRepository.visualizeText(currentSelectedText) }; _isImageResponse.value = true }
    }

    private suspend fun performAiAction(m: String, a: suspend () -> String) { _aiResponse.value = m; try { _aiResponse.value = a() } catch(e:Exception){ _aiResponse.value = e.message?:"" } }

    /**
     * Looks up a word in the dictionary.
     *
     * @param w The word to lookup.
     * @param c The context.
     */
    fun lookupWord(w: String, c: Context) { /* ... */ }

    /**
     * Dismisses the recap view.
     */
    fun dismissRecap() { _recapText.value = null }

    /**
     * Dismisses the bottom sheet.
     */
    fun dismissBottomSheet() { _isBottomSheetVisible.value = false }

    /**
     * Clears the current snackbar message.
     */
    fun clearSnackbar() { _snackbarMessage.value = null }
}