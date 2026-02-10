package io.github.piyushdaiya.vaachak.ui.catalog

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.piyushdaiya.vaachak.data.local.OpdsDao
import io.github.piyushdaiya.vaachak.data.local.OpdsEntity
import io.github.piyushdaiya.vaachak.data.repository.LibraryRepository
import io.github.piyushdaiya.vaachak.data.repository.OpdsRepository
import io.github.piyushdaiya.vaachak.data.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.opds.images
import org.readium.r2.shared.opds.Feed
import org.readium.r2.shared.util.Try
import java.io.File
import java.net.URL
import javax.inject.Inject

// --- UI MODEL ---
sealed class CatalogItem {
    data class Folder(
        val title: String,
        val url: String
    ) : CatalogItem()

    data class Book(
        val title: String,
        val author: String,
        val imageUrl: String?,
        val format: String, // "EPUB" or "PDF"
        val publication: Publication,
        val navigationUrl: String? = null
    ) : CatalogItem()
}

@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val opdsRepository: OpdsRepository,
    private val settingsRepo: SettingsRepository,
    private val libraryRepository: LibraryRepository,
    private val opdsDao: OpdsDao,
    private val application: Application
) : ViewModel() {

    // --- STATE FLOWS ---

    // 1. Sidebar Catalogs
    val catalogs: StateFlow<List<OpdsEntity>> = opdsDao.getAllFeeds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 2. Main Content (Clean List)
    private val _feedItems = MutableStateFlow<List<CatalogItem>>(emptyList())
    val feedItems = _feedItems.asStateFlow()

    // 3. UI State
    private val _screenTitle = MutableStateFlow("Catalog Browser")
    val screenTitle = _screenTitle.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    // Settings
    val isEinkEnabled = settingsRepo.isEinkEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val isOfflineMode = settingsRepo.isOfflineModeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Internal Navigation State
    // Stores <URL, Title> pairs for history
    private val historyStack = ArrayDeque<Pair<String, String>>()
    // Tracks current base URL for resolving relative links correctly
    private var currentContextUrl: String? = null

    init {
        // Pre-populate DB if empty
        viewModelScope.launch {
            val default = opdsDao.getAllFeeds().first().firstOrNull()
            if (default != null) switchCatalog(default)
        }
    }

    // --- NAVIGATION ---

    fun switchCatalog(feed: OpdsEntity) {
        historyStack.clear()
        currentContextUrl = feed.url // Reset base to root
        loadFeed(feed.url, feed.title, addToHistory = true)
    }

    fun handleItemClick(item: CatalogItem) {
        when (item) {
            is CatalogItem.Folder -> loadFeed(item.url, item.title, addToHistory = true)
            is CatalogItem.Book -> downloadBook(item.publication)
        }
    }

    fun goBack(): Boolean {
        if (historyStack.size <= 1) return false
        historyStack.removeLast() // Remove current
        val previous = historyStack.last() // Peek previous

        // Load previous WITHOUT adding to history again
        loadFeed(previous.first, previous.second, addToHistory = false)
        return true
    }

    // --- CORE LOGIC ---

    private fun loadFeed(rawUrl: String, newTitle: String? = null, addToHistory: Boolean = true) {
        // 1. Resolve URL (Fixes the Relative Link Issue in ManyBooks)
        val finalUrl = try {
            val base = currentContextUrl ?: rawUrl
            if (rawUrl.startsWith("http")) rawUrl else URL(URL(base), rawUrl).toString()
        } catch (e: Exception) { rawUrl }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // Use Try.Success/Failure pattern for Readium
            when (val result = opdsRepository.parseFeed(finalUrl)) {
                is Try.Success -> {
                    val parseData = result.value

                    // Update Internal State
                    currentContextUrl = finalUrl
                    val displayTitle = newTitle ?: parseData.feed?.metadata?.title ?: "Catalog"

                    if (addToHistory) {
                        historyStack.addLast(finalUrl to displayTitle)
                    }

                    // Update UI Title
                    _screenTitle.value = displayTitle

                    // Process Feed into Clean Items
                    parseData.feed?.let { feed ->
                        _feedItems.value = processFeed(feed)
                    } ?: run {
                        _feedItems.value = emptyList()
                    }
                }
                is Try.Failure -> {
                    _error.value = "Error: ${result.value.message}"
                }
            }
            _isLoading.value = false
        }
    }

    /**
     * Filters "Ghost" links and determines Book Format (EPUB/PDF).
     */
    private fun processFeed(feed: Feed): List<CatalogItem> {
        val items = mutableListOf<CatalogItem>()

        // A. Folders (Navigation)
        feed.navigation.forEach { link ->
            val title = link.title
            val rel = link.rels.firstOrNull().toString()
            //Log.e("processFeed", "link title:$title + link url:${link.href} + link rel:$rel")
            // FILTER: Ignore System Links which cause empty rows
            val isSystem = rel.contains("search", true) ||
                    rel.contains("self", true) ||
                    rel.contains("start", true) ||
                    rel.contains("up", true) ||
                    rel.contains("next", true) ||
                    rel.contains("previous", true)

            if (!title.isNullOrBlank() && !isSystem) {
                items.add(CatalogItem.Folder(title, link.href.toString()))
            }
        }

        // B. Books (Publications)
        feed.publications.forEach { pub ->
            val isPdf = pub.links.any { it.mediaType?.toString()?.contains("pdf") == true }
            val isEpub = pub.links.any { it.mediaType?.toString()?.contains("epub") == true }

            // Check for Navigation Link (ManyBooks Detail Page)
            val navLink = pub.links.firstOrNull {
                val type = it.mediaType?.toString() ?: ""
                type.contains("application/atom+xml") || type.contains("application/xml")
            }?.href?.toString()

            val format = when {
                isEpub -> "EPUB"
                isPdf -> "PDF"
                navLink != null -> "DETAIL"
                else -> ""
            }

            // --- FIX FOR IMAGES ---
            // 1. Try standard Readium 'images' property
            var cover = pub.images.firstOrNull()?.href?.toString()

            // 2. If empty, manually search 'links' for image types or rels
            if (cover == null) {
                cover = pub.links.firstOrNull { link ->
                    val type = link.mediaType?.toString() ?: ""
                    val rel = link.rels.firstOrNull() ?: ""

                    type.startsWith("image/") ||
                            rel.contains("image") ||
                            rel.contains("thumbnail") ||
                            rel.contains("cover")
                }?.href?.toString()
            }
            // ----------------------

            val title = pub.metadata.title ?: "Unknown"
            val author = pub.metadata.authors.firstOrNull()?.name?.toString() ?: "Unknown"

            items.add(CatalogItem.Book(
                title = title,
                author = author,
                imageUrl = cover, // Now correctly populated
                format = format,
                publication = pub,
                navigationUrl = if (isEpub || isPdf) null else navLink
            ))
        }
        return items
    }

    // --- DOWNLOAD ---

    fun downloadBook(publication: Publication) {
        viewModelScope.launch {
            val title = publication.metadata.title ?: "Unknown"
            if (libraryRepository.isBookDuplicate(title)) {
                _error.value = "Duplicate: '$title' is already in your library."
                return@launch
            }

            val link = publication.links.firstOrNull {
                val m = it.mediaType?.toString() ?: ""
                m.contains("epub") || m.contains("pdf") || m.contains("application/epub+zip")
            }

            if (link == null) {
                _error.value = "No download link found."
                return@launch
            }

            _isLoading.value = true
            val ext = if (link.mediaType?.toString()?.contains("pdf") == true) "pdf" else "epub"
            val safeTitle = title.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
            val bookFile = File(application.filesDir, "$safeTitle.$ext")
            val coverFile = File(application.filesDir, "$safeTitle.jpg")

            // Use currentContextUrl as referrer/feed URL
            val success = opdsRepository.downloadPublication(link.href.toString(), bookFile, currentContextUrl)

            if (success) {
                // --- FIX: ROBUST COVER EXTRACTION ---
                // Same logic as processFeed: Check 'images' first, then fallback to 'links'
                var coverUrl = publication.images.firstOrNull()?.href?.toString()

                if (coverUrl == null) {
                    coverUrl = publication.links.firstOrNull { l ->
                        val type = l.mediaType?.toString() ?: ""
                        val rel = l.rels.firstOrNull() ?: ""
                        type.startsWith("image/") || rel.contains("image") || rel.contains("thumbnail") || rel.contains("cover")
                    }?.href?.toString()
                }
                // ------------------------------------
                var savedCover: File? = null
                if (coverUrl != null) {
                    if (opdsRepository.downloadPublication(coverUrl, coverFile, currentContextUrl)) {
                        savedCover = coverFile
                    }
                }
                libraryRepository.addDownloadedBook(bookFile, title,
                    publication.metadata.authors.firstOrNull()?.name?.toString() ?: "Unknown",
                    savedCover
                )
                _error.value = "Downloaded '$title'"
            } else {
                _error.value = "Download failed."
                if (bookFile.exists()) bookFile.delete()
            }
            _isLoading.value = false
        }
    }

    // --- CATALOG MANAGEMENT ---

    fun addCustomCatalog(title: String, url: String, user: String?, pass: String?, allowInsecure: Boolean) {
        viewModelScope.launch {
            var cleanUrl = url.trim()

            // Smart URL Fixer
            val isSpecificFile = cleanUrl.endsWith(".xml", true) || cleanUrl.endsWith(".opds", true) || cleanUrl.contains("/opds", true)
            if (!isSpecificFile) {
                cleanUrl = if (cleanUrl.endsWith("/")) "${cleanUrl}opds" else "${cleanUrl}/opds"
            }
            if (!cleanUrl.startsWith("http")) {
                cleanUrl = "http://$cleanUrl"
            }

            val newFeed = OpdsEntity(
                title = title.ifBlank { "Library" },
                url = cleanUrl,
                username = user?.ifBlank{null},
                password = pass?.ifBlank{null},
                allowInsecure = allowInsecure
            )
            opdsDao.insertFeed(newFeed)
            switchCatalog(newFeed)
        }
    }

    fun deleteCatalog(feed: OpdsEntity) { viewModelScope.launch { opdsDao.deleteFeed(feed) } }
    fun clearError() { _error.value = null }
}