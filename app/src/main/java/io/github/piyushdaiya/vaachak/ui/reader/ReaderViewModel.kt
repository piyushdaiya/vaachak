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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.readium.r2.navigator.Decoration
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.search.SearchService
import javax.inject.Inject

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

    // --- 2. READER STATE ---
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

    // --- SEARCH STATE ---
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

    // --- UI OVERLAY STATES ---
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

    // --- RECAP STATES ---
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
        .flatMapLatest {
            highlightDao.getHighlightsForBook(initialUri ?: "")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- METHODS ---
    fun setInitialLocation(json: String?) { this.pendingJumpLocator = json }

    fun onFileSelected(uri: Uri) {
        _bookSearchQuery.value = ""
        _searchResults.value = emptyList()
        _showSearch.value = false
        _isBookSearching.value = false
        _showHighlights.value = false
        _recapText.value = null
        _showRecapConfirmation.value = false

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

    // --- RECAP LOGIC ---
    fun onRecapClicked() { _showRecapConfirmation.value = true }
    fun dismissRecapConfirmation() { _showRecapConfirmation.value = false }

    fun generateRecap() {
        _showRecapConfirmation.value = false
        _isRecapLoading.value = true

        viewModelScope.launch {
            val title = _publication.value?.metadata?.title ?: "Unknown Book"
            val currentContext = "Current Position: ${_currentPageInfo.value}"
            val summary = aiRepository.getRecallSummary(title, currentContext)
            _recapText.value = summary
            _isRecapLoading.value = false
        }
    }

    // --- FIX: USE NAMED ARGUMENTS TO FIX TYPE MISMATCH ---
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

    fun dismissRecapResult() { _recapText.value = null }

    // ... (Existing Navigation & Helper Methods) ...
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
    fun onHighlightClicked(h: HighlightEntity) { viewModelScope.launch { try{ val l=Locator.fromJSON(JSONObject(h.locatorJson)); if(l!=null){ _showHighlights.value=false; _jumpEvent.emit(l) } }catch(e:Exception){} } }
    fun updateProgress(l: Locator) {
        _currentLocator.value = l
        val pos = l.locations.position?:0; val prog = l.locations.totalProgression?:0.0; val pct = (prog*100).toInt()
        _currentPageInfo.value = if(pos>0) "Page $pos ($pct%)" else "$pct% completed"
        val u = initialUri?:return
        viewModelScope.launch { bookDao.updateLastLocation(u, l.toJSON().toString()); bookDao.updateProgress(u, prog) }
    }
    fun closeBook() { viewModelScope.launch { readiumManager.closePublication(); _publication.value=null; _currentLocator.value=null; _bookSearchQuery.value=""; _searchResults.value=emptyList(); _showHighlights.value=false } }
    fun prepareHighlight(l: Locator) { pendingHighlightLocator = l; _showTagSelector.value = true }
    fun saveHighlightWithTag(tag: String) {
        val l = pendingHighlightLocator?:return; val u = initialUri?:return; val e = isEinkEnabled.value
        viewModelScope.launch {
            highlightDao.insertHighlight(
                HighlightEntity(
                    publicationId = u,
                    locatorJson = l.toJSON().toString(),
                    text = l.text.highlight?:"Selected",
                    color = if(e) Color.DKGRAY else Color.YELLOW,
                    tag = tag
                )
            )
            dismissTagSelector()
        }
    }
    fun deleteHighlight(id: Long) { viewModelScope.launch { highlightDao.deleteHighlightById(id) } }
    fun dismissTagSelector() { _showTagSelector.value = false; pendingHighlightLocator = null }
    fun onTextSelected(t: String) { currentSelectedText = t; _isBottomSheetVisible.value = true }
    fun onActionExplain() { viewModelScope.launch { performAiAction("Thinking...") { aiRepository.explainContext(currentSelectedText) } } }
    fun onActionWhoIsThis() { viewModelScope.launch { performAiAction("Investigating...") { aiRepository.whoIsThis(currentSelectedText, _publication.value?.metadata?.title?:"", "") } } }
    fun onActionVisualize() { viewModelScope.launch { performAiAction("Drawing...") { aiRepository.visualizeText(currentSelectedText) }; _isImageResponse.value = true } }
    private suspend fun performAiAction(m: String, a: suspend () -> String) { _aiResponse.value = m; try { _aiResponse.value = a() } catch(e:Exception){ _aiResponse.value = e.message?:"" } }
    fun lookupWord(w: String, c: Context) { /* ... */ }
    fun dismissRecap() { _recapText.value = null }
    fun dismissBottomSheet() { _isBottomSheetVisible.value = false }
    fun clearSnackbar() { _snackbarMessage.value = null }
}