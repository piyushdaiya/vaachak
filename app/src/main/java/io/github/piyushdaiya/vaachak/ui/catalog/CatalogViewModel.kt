package io.github.piyushdaiya.vaachak.ui.catalog

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.piyushdaiya.vaachak.data.local.OpdsDao
import io.github.piyushdaiya.vaachak.data.local.OpdsEntity
import io.github.piyushdaiya.vaachak.data.repository.GutendexRepository
import io.github.piyushdaiya.vaachak.data.repository.LibraryRepository
import io.github.piyushdaiya.vaachak.data.repository.OpdsRepository
import io.github.piyushdaiya.vaachak.data.repository.SettingsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.readium.r2.shared.publication.Metadata
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Contributor
import org.readium.r2.shared.publication.Manifest
import org.readium.r2.shared.publication.opds.images
import org.readium.r2.shared.opds.Feed
import org.readium.r2.shared.publication.LocalizedString
import org.readium.r2.shared.util.Try
import org.readium.r2.shared.util.Url
import org.readium.r2.shared.util.mediatype.MediaType
import java.io.File
import java.net.URL
import javax.inject.Inject

// --- UI MODELS ---
sealed class CatalogItem {
    data class Folder(val title: String, val url: String) : CatalogItem()
    data class Book(
        val title: String, val author: String, val imageUrl: String?,
        val format: String, val publication: Publication, val navigationUrl: String? = null,
        val existingBookUri: String? = null // NEW: Holds URI if downloaded, null otherwise
    ) : CatalogItem()
    data class Pagination(val nextUrl: String?, val prevUrl: String?) : CatalogItem()
}

// NEW: One-time events for Navigation/Snackbar
sealed interface CatalogUiEvent {
    data class ShowSnackbar(val message: String, val actionLabel: String? = null) : CatalogUiEvent
    data class NavigateToReader(val bookUri: String) : CatalogUiEvent
}

@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val opdsRepository: OpdsRepository,
    private val gutendexRepository: GutendexRepository,
    private val settingsRepo: SettingsRepository,
    private val libraryRepository: LibraryRepository,
    private val opdsDao: OpdsDao,
    private val application: Application
) : ViewModel() {

    // --- STATE FLOWS ---
    val catalogs: StateFlow<List<OpdsEntity>> = opdsDao.getAllFeeds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _feedItems = MutableStateFlow<List<CatalogItem>>(emptyList())
    val feedItems = _feedItems.asStateFlow()

    private val _screenTitle = MutableStateFlow("Catalog Browser")
    val screenTitle = _screenTitle.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _breadcrumbs = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val breadcrumbs = _breadcrumbs.asStateFlow()

    // NEW: Channel for UI Events (Snackbar, Navigation)
    private val _uiEvent = Channel<CatalogUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    val isEinkEnabled = settingsRepo.isEinkEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val isOfflineMode = settingsRepo.isOfflineModeEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val historyStack = ArrayDeque<Pair<String, String>>()
    private var currentContextUrl: String? = null
    private var isGutendexSession = false

    // --- INIT: SEED DEFAULTS ---
    init {
        viewModelScope.launch {
            opdsDao.getAllFeeds().firstOrNull()?.let { currentFeeds ->
                if (currentFeeds.isEmpty()) {
                    addCatalog("Project Gutenberg", "https://gutendex.com/books", null, null, false)
                    addCatalog("ManyBooks", "https://manybooks.net/opds", null, null, false)
                }
            }
        }
    }

    // --- ACTIONS ---

    fun openCatalog(feed: OpdsEntity) {
        historyStack.clear()
        _feedItems.value = emptyList()
        currentContextUrl = feed.url
        _screenTitle.value = feed.title

        isGutendexSession = feed.url.contains("gutenberg.org") ||
                feed.url.contains("gutendex") ||
                feed.url.endsWith("/books")

        if (isGutendexSession) {
            loadGutendexRoot()
        } else {
            loadFeed(feed.url, "Home", addToHistory = true)
        }
    }

    fun handleItemClick(item: CatalogItem) {
        when (item) {
            is CatalogItem.Folder -> {
                if (isGutendexSession) {
                    loadGutendexFeed(item.url, item.title, isPagination = false)
                } else {
                    loadFeed(item.url, item.title, addToHistory = true)
                }
            }
            is CatalogItem.Book -> {
                if (item.existingBookUri != null) {
                    // Book exists -> Read It
                    viewModelScope.launch { _uiEvent.send(CatalogUiEvent.NavigateToReader(item.existingBookUri)) }
                } else {
                    // Book missing -> Download It
                    if (item.navigationUrl != null) loadFeed(item.navigationUrl, item.title, addToHistory = true)
                    else downloadBook(item.publication)
                }
            }
            is CatalogItem.Pagination -> { /* UI Handles this */ }
        }
    }

    fun handlePaginationClick(url: String) {
        if (isGutendexSession) {
            loadGutendexFeed(url, null, isPagination = true)
        } else {
            loadFeed(url, null, addToHistory = false)
        }
    }

    fun goBack(): Boolean {
        if (historyStack.size <= 1) return false
        historyStack.removeLast()
        val previous = historyStack.last()

        if (isGutendexSession) {
            if (historyStack.size == 1 && previous.second == "Home") {
                loadGutendexRoot(addToHistory = false)
            } else {
                loadGutendexFeed(previous.first, previous.second, isPagination = false, addToHistory = false)
            }
        } else {
            loadFeed(previous.first, previous.second, addToHistory = false)
        }
        return true
    }

    fun onBreadcrumbClick(index: Int) {
        if (index < 0 || index >= historyStack.size) return
        while (historyStack.size > index + 1) historyStack.removeLast()

        val target = historyStack.last()
        if (isGutendexSession) {
            if (index == 0) loadGutendexRoot(addToHistory = false)
            else loadGutendexFeed(target.first, target.second, isPagination = false, addToHistory = false)
        } else {
            loadFeed(target.first, target.second, addToHistory = false)
        }
    }

    private fun rollbackBreadcrumb() {
        if (historyStack.size > 1) {
            historyStack.removeLast()
            updateBreadcrumbs()
        }
    }

    private fun updateBreadcrumbs() {
        _breadcrumbs.value = historyStack.toList()
    }

    // --- GUTENDEX LOGIC ---

    private fun loadGutendexRoot(addToHistory: Boolean = true) {
        viewModelScope.launch {
            if (addToHistory) {
                historyStack.clear()
                historyStack.addLast("" to "Home")
            }
            updateBreadcrumbs()

            val base = currentContextUrl?.trimEnd('/') ?: "https://gutendex.com/books"
            fun makeUrl(params: String): String {
                val sep = if (base.contains("?")) "&" else "?"
                return "$base$sep$params"
            }

            val rootItems = listOf(
                CatalogItem.Folder("Recent Uploads", makeUrl("sort=descending")),
                CatalogItem.Folder("Popular Books", makeUrl("sort=popular")),
                CatalogItem.Folder("Topic: Fiction", makeUrl("topic=fiction")),
                CatalogItem.Folder("Topic: Sci-Fi", makeUrl("topic=science%20fiction")),
                CatalogItem.Folder("Topic: Fantasy", makeUrl("topic=fantasy")),
                CatalogItem.Folder("Topic: History", makeUrl("topic=history")),
                CatalogItem.Folder("Topic: Science", makeUrl("topic=science")),
                CatalogItem.Folder("Topic: Philosophy", makeUrl("topic=philosophy")),
                CatalogItem.Folder("Topic: Children", makeUrl("topic=children"))
            )
            _feedItems.value = rootItems
        }
    }

    private fun loadGutendexFeed(url: String?, title: String?, isPagination: Boolean = false, addToHistory: Boolean = true) {
        viewModelScope.launch {
            _isLoading.value = true

            val actualUrl = url ?: currentContextUrl ?: "https://gutendex.com/books"

            if (isPagination && historyStack.isNotEmpty()) {
                val current = historyStack.removeLast()
                historyStack.addLast((actualUrl) to current.second)
            }
            else if (addToHistory) {
                historyStack.addLast((actualUrl) to (title ?: "Section"))
            }
            updateBreadcrumbs()

            val localBookMap = libraryRepository.getLocalBookMap()

            val result = gutendexRepository.fetchBooks(actualUrl)

            result.onSuccess { response ->
                val items = mutableListOf<CatalogItem>()

                response.results.forEach { gBook ->
                    val epubLink = gBook.formats.entries.firstOrNull { it.key.contains("application/epub+zip") }?.value
                    val coverLink = gBook.formats.entries.firstOrNull { it.key.contains("image/jpeg") }?.value

                    if (epubLink != null) {
                        val epubUrl = Url(epubLink)
                        val epubType = MediaType("application/epub+zip")
                        val coverUrl = if (coverLink != null) Url(coverLink) else null
                        val coverType = MediaType("image/jpeg")

                        if (epubUrl != null) {
                            val manifest = Manifest(
                                metadata = Metadata(
                                    localizedTitle = LocalizedString(gBook.title),
                                    authors = gBook.authors.map { Contributor(name = it.name) }
                                ),
                                links = listOf(Link(href = epubUrl, mediaType = epubType)),
                                resources = if (coverUrl != null && coverType != null) listOf(
                                    Link(href = coverUrl, mediaType = coverType, rels = setOf("image"))
                                ) else emptyList()
                            )

                            // Check local map for this title
                            val existingUri = localBookMap[gBook.title]

                            items.add(CatalogItem.Book(
                                title = gBook.title,
                                author = gBook.authors.firstOrNull()?.name ?: "Unknown",
                                imageUrl = coverLink,
                                format = "EPUB",
                                publication = Publication(manifest = manifest),
                                existingBookUri = existingUri
                            ))
                        }
                    }
                }

                if (response.next != null || response.previous != null) {
                    items.add(CatalogItem.Pagination(response.next, response.previous))
                }
                _feedItems.value = items

            }.onFailure {
                if (addToHistory && !isPagination) {
                    rollbackBreadcrumb()
                }
                _uiEvent.send(CatalogUiEvent.ShowSnackbar("Failed to load: ${it.message}"))
            }
            _isLoading.value = false
        }
    }

    // --- STANDARD OPDS LOGIC ---

    private fun loadFeed(rawUrl: String, breadcrumbTitle: String? = null, addToHistory: Boolean = true) {
        val finalUrl = try {
            val base = currentContextUrl ?: rawUrl
            if (rawUrl.startsWith("http")) rawUrl else URL(URL(base), rawUrl).toString()
        } catch (e: Exception) { rawUrl }

        viewModelScope.launch {
            _isLoading.value = true

            if (addToHistory) {
                val titleForStack = breadcrumbTitle ?: "Unknown"
                historyStack.addLast(finalUrl to titleForStack)
                updateBreadcrumbs()
            }

            val localBookMap = libraryRepository.getLocalBookMap()

            when (val result = opdsRepository.parseFeed(finalUrl)) {
                is Try.Success -> {
                    val parseData = result.value
                    currentContextUrl = finalUrl

                    if (addToHistory && breadcrumbTitle == null) {
                        val realTitle = parseData.feed?.metadata?.title ?: "Section"
                        historyStack.removeLast()
                        historyStack.addLast(finalUrl to realTitle)
                        updateBreadcrumbs()
                    }

                    parseData.feed?.let { _feedItems.value = processFeed(it, localBookMap) } ?: run { _feedItems.value = emptyList() }
                }
                is Try.Failure -> {
                    if (addToHistory) {
                        rollbackBreadcrumb()
                    }
                    _uiEvent.send(CatalogUiEvent.ShowSnackbar("Error: ${result.value.message}"))
                }
            }
            _isLoading.value = false
        }
    }

    private fun processFeed(feed: Feed, localBookMap: Map<String, String>): List<CatalogItem> {
        val items = mutableListOf<CatalogItem>()
        feed.navigation.forEach { link ->
            val title = link.title
            val rel = link.rels.firstOrNull().toString()
            val isSystem = rel.contains("search", true) || rel.contains("self", true) || rel.contains("start", true) || rel.contains("up", true) || rel.contains("next", true) || rel.contains("previous", true)
            if (!title.isNullOrBlank() && !isSystem) items.add(CatalogItem.Folder(title, link.href.toString()))
        }
        feed.publications.forEach { pub ->
            val isPdf = pub.links.any { it.mediaType?.toString()?.contains("pdf") == true }
            val isEpub = pub.links.any { it.mediaType?.toString()?.contains("epub") == true }
            val navLink = pub.links.firstOrNull { it.mediaType?.toString()?.contains("atom+xml") == true || it.mediaType?.toString()?.contains("xml") == true }?.href?.toString()

            val format = if (isEpub) "EPUB" else if (isPdf) "PDF" else if (navLink != null) "DETAIL" else ""

            var cover = pub.images.firstOrNull()?.href?.toString()
            if (cover == null) {
                cover = pub.links.firstOrNull { l -> l.mediaType?.toString()?.startsWith("image/") == true || l.rels.any { it.contains("image") || it.contains("thumbnail") || it.contains("cover") } }?.href?.toString()
            }

            val title = pub.metadata.title ?: "Unknown"
            val existingUri = localBookMap[title]

            items.add(CatalogItem.Book(title, pub.metadata.authors.firstOrNull()?.name?.toString() ?: "Unknown", cover, format, pub, if(isEpub||isPdf) null else navLink, existingUri))
        }
        return items
    }

    // --- DOWNLOADER ---

    fun downloadBook(publication: Publication) {
        viewModelScope.launch {
            val title = publication.metadata.title ?: "Unknown"
            if (libraryRepository.isBookDuplicate(title)) {
                _uiEvent.send(CatalogUiEvent.ShowSnackbar("Duplicate: '$title' exists."))
                return@launch
            }

            val link = publication.links.firstOrNull {
                it.mediaType?.toString()?.contains("epub") == true || it.mediaType?.toString()?.contains("pdf") == true
            }
            if (link == null) {
                _uiEvent.send(CatalogUiEvent.ShowSnackbar("No download link found."))
                return@launch
            }

            _isLoading.value = true
            val ext = if (link.mediaType?.toString()?.contains("pdf") == true) "pdf" else "epub"
            val safeTitle = title.replace("[^a-zA-Z0-9.-]".toRegex(), "_").take(50)
            val bookFile = File(application.filesDir, "$safeTitle.$ext")
            val coverFile = File(application.filesDir, "$safeTitle.jpg")

            val downloadUrl = link.href.toString()

            val success = if (isGutendexSession) {
                gutendexRepository.downloadBook(downloadUrl, bookFile)
            } else {
                opdsRepository.downloadPublication(downloadUrl, bookFile, currentContextUrl)
            }

            if (success) {
                var coverUrl = publication.images.firstOrNull()?.href?.toString()
                if (coverUrl == null && !isGutendexSession) {
                    coverUrl = publication.links.firstOrNull { it.mediaType?.toString()?.startsWith("image/") == true || it.rels.any { rel -> rel.contains("image") } }?.href?.toString()
                }

                if (coverUrl == null && isGutendexSession) {
                    coverUrl = publication.resources.firstOrNull { it.mediaType?.toString()?.startsWith("image/") == true }?.href?.toString()
                }

                var savedCover: File? = null
                if (coverUrl != null) {
                    if (isGutendexSession) {
                        if (gutendexRepository.downloadBook(coverUrl, coverFile)) savedCover = coverFile
                    } else {
                        if (opdsRepository.downloadPublication(coverUrl, coverFile, currentContextUrl)) savedCover = coverFile
                    }
                }

                libraryRepository.addDownloadedBook(bookFile, title, publication.metadata.authors.firstOrNull()?.name?.toString() ?: "Unknown", savedCover)

                // Refresh list logic
                val currentFeed = if (isGutendexSession) {
                    val currentBreadcrumb = breadcrumbs.value.lastOrNull()
                    loadGutendexFeed(currentBreadcrumb?.first ?: currentContextUrl, currentBreadcrumb?.second, isPagination = false, addToHistory = false)
                } else {
                    loadFeed(currentContextUrl ?: "", null, addToHistory = false)
                }

                // Show Snackbar with "Go to Bookshelf" Action
                _uiEvent.send(CatalogUiEvent.ShowSnackbar("Downloaded '$title'", "View Library"))

            } else {
                _uiEvent.send(CatalogUiEvent.ShowSnackbar("Download failed."))
                if(bookFile.exists()) bookFile.delete()
            }
            _isLoading.value = false
        }
    }

    // --- CRUD ---

    fun addCatalog(title: String, url: String, user: String?, pass: String?, allowInsecure: Boolean) {
        viewModelScope.launch {
            val cleanUrl = cleanUrl(url)
            val newFeed = OpdsEntity(title = title.ifBlank { "Library" }, url = cleanUrl, username = user?.ifBlank{null}, password = pass?.ifBlank{null}, allowInsecure = allowInsecure)
            opdsDao.insertFeed(newFeed)
        }
    }

    fun updateCatalog(feed: OpdsEntity, title: String, url: String, user: String?, pass: String?, allowInsecure: Boolean) {
        viewModelScope.launch {
            val cleanUrl = cleanUrl(url)
            val updatedFeed = feed.copy(title = title, url = cleanUrl, username = user?.ifBlank{null}, password = pass?.ifBlank{null}, allowInsecure = allowInsecure)
            opdsDao.updateFeed(updatedFeed)
        }
    }

    fun deleteCatalog(feed: OpdsEntity) { viewModelScope.launch { opdsDao.deleteFeed(feed) } }

    private fun cleanUrl(url: String): String {
        var clean = url.trim()
        val isSpecific = clean.endsWith(".xml") || clean.endsWith(".opds") || clean.contains("/opds")
        val isGutendex = clean.contains("/books")

        if (!isSpecific && !isGutendex) {
            clean = if (clean.endsWith("/")) "${clean}opds" else "${clean}/opds"
        }

        if (!clean.startsWith("http")) clean = "http://$clean"
        return clean
    }
}