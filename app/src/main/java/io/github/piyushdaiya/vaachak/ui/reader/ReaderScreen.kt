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

import android.app.Activity
import android.util.Log
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.piyushdaiya.vaachak.ui.reader.components.AiBottomSheet
import io.github.piyushdaiya.vaachak.ui.reader.components.BookHighlightsOverlay
import io.github.piyushdaiya.vaachak.ui.reader.components.BookSearchOverlay
import io.github.piyushdaiya.vaachak.ui.reader.components.ReaderSettingsSheet
import io.github.piyushdaiya.vaachak.ui.reader.components.ReaderSystemFooter
import io.github.piyushdaiya.vaachak.ui.reader.components.ReaderTopBar
import io.github.piyushdaiya.vaachak.ui.reader.components.TableOfContents
import kotlinx.coroutines.launch
import org.readium.r2.navigator.DecorableNavigator
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.AbsoluteUrl
// --- FIX: Correct Import ---
import org.readium.r2.navigator.epub.EpubPreferences

@OptIn(ExperimentalMaterial3Api::class, ExperimentalReadiumApi::class)
@Composable
fun ReaderScreen(
    initialUri: String?,
    initialLocatorJson: String?,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val activity = LocalContext.current as AppCompatActivity
    val view = LocalView.current

    val publication by viewModel.publication.collectAsState()
    val isEink by viewModel.isEinkEnabled.collectAsState()
    val isOfflineMode by viewModel.isOfflineModeEnabled.collectAsState()
    val isAiEnabled by viewModel.isAiEnabled.collectAsState()

    val showToc by viewModel.showToc.collectAsState()
    val currentLocator by viewModel.currentLocator.collectAsState()

    val showReaderSettings by viewModel.showReaderSettings.collectAsState()
    // Type is now known due to import
    val epubPreferences by viewModel.epubPreferences.collectAsState()

    val showSearch by viewModel.showSearch.collectAsState()
    val bookSearchQuery by viewModel.bookSearchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isBookSearching by viewModel.isBookSearching.collectAsState()

    val showHighlights by viewModel.showHighlights.collectAsState()
    val highlightsList by viewModel.bookmarksList.collectAsState()

    val showRecapConfirmation by viewModel.showRecapConfirmation.collectAsState()
    val recapText by viewModel.recapText.collectAsState()
    val isRecapLoading by viewModel.isRecapLoading.collectAsState()

    var showDeleteDialogId by remember { mutableStateOf<Long?>(null) }
    var isNavigatorReady by remember { mutableStateOf(false) }

    val isBottomSheetVisible by viewModel.isBottomSheetVisible.collectAsState()
    val aiResponse by viewModel.aiResponse.collectAsState()
    val isImageResponse by viewModel.isImageResponse.collectAsState()
    val isDictionaryLookup by viewModel.isDictionaryLookup.collectAsState()
    val isDictionaryLoading by viewModel.isDictionaryLoading.collectAsState()

    val showTagSelector by viewModel.showTagSelector.collectAsState()
    val pageInfo by viewModel.currentPageInfo.collectAsState()
    val initialLocatorState by viewModel.initialLocator.collectAsState()
    val savedHighlights by viewModel.currentBookHighlights.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    val scope = rememberCoroutineScope()
    var currentNavigatorFragment by remember { mutableStateOf<EpubNavigatorFragment?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            onDispose { insetsController.show(WindowInsetsCompat.Type.systemBars()) }
        } else {
            onDispose { }
        }
    }

    LaunchedEffect(Unit) { viewModel.navigationEvent.collect { link -> currentNavigatorFragment?.go(link, animated = false) } }
    LaunchedEffect(Unit) { viewModel.jumpEvent.collect { locator -> currentNavigatorFragment?.go(locator, animated = false) } }

    // Apply Preferences
    LaunchedEffect(epubPreferences, currentNavigatorFragment) {
        currentNavigatorFragment?.submitPreferences(epubPreferences)
    }

    LaunchedEffect(savedHighlights, isNavigatorReady) {
        val navigator = currentNavigatorFragment ?: return@LaunchedEffect
        if (isNavigatorReady) navigator.applyDecorations(savedHighlights, "user_highlights")
    }
    LaunchedEffect(initialUri, initialLocatorJson) {
        if (initialUri != null) {
            viewModel.setInitialLocation(initialLocatorJson)
            viewModel.onFileSelected(initialUri.toUri())
        }
    }

    val navListener = remember {
        object : EpubNavigatorFragment.Listener {
            override fun onJumpToLocator(locator: Locator) { viewModel.updateProgress(locator) }
            override fun onExternalLinkActivated(url: AbsoluteUrl) {}
        }
    }

    BackHandler {
        if (showDeleteDialogId != null) showDeleteDialogId = null
        else if (showTagSelector) viewModel.dismissTagSelector()
        else if (showReaderSettings) viewModel.dismissReaderSettings()
        else if (showToc) viewModel.toggleToc()
        else if (showSearch) viewModel.toggleSearch()
        else if (showHighlights) viewModel.toggleHighlights()
        else if (showRecapConfirmation) viewModel.dismissRecapConfirmation()
        else if (recapText != null) viewModel.dismissRecapResult()
        else if (isBottomSheetVisible) viewModel.dismissBottomSheet()
        else { viewModel.closeBook(); onBack() }
    }

    val decorationListener = remember {
        object : DecorableNavigator.Listener {
            override fun onDecorationActivated(event: DecorableNavigator.OnActivatedEvent): Boolean {
                if (event.group == "user_highlights") {
                    event.decoration.id.toLongOrNull()?.let { showDeleteDialogId = it; return true }
                }
                return false
            }
        }
    }

    DisposableEffect(currentNavigatorFragment) {
        val navigator = currentNavigatorFragment ?: return@DisposableEffect onDispose {}
        val fm = activity.supportFragmentManager
        val callbacks = object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
                if (f == navigator) { isNavigatorReady = true; navigator.addDecorationListener("user_highlights", decorationListener) }
            }
        }
        if (navigator.isAdded && !navigator.isDetached) {
            isNavigatorReady = true
            navigator.addDecorationListener("user_highlights", decorationListener)
        } else { fm.registerFragmentLifecycleCallbacks(callbacks, false) }
        onDispose {
            fm.unregisterFragmentLifecycleCallbacks(callbacks)
            if (navigator.isAdded) { try { navigator.removeDecorationListener(decorationListener) } catch (e: Exception) {} }
            isNavigatorReady = false
        }
    }

    val aiSelectionCallback = remember(isAiEnabled) {
        object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                if (isAiEnabled) {
                    menu?.add(Menu.NONE, 101, 0, "Ask AI")
                    menu?.add(Menu.NONE, 103, 2, "Define")
                }
                menu?.add(Menu.NONE, 102, 1, "Highlight")
                return true
            }
            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = false
            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                scope.launch {
                    val selection = currentNavigatorFragment?.currentSelection()
                    val locator = selection?.locator ?: return@launch
                    val text = locator.text.highlight ?: ""
                    when (item?.itemId) {
                        101 -> viewModel.onTextSelected(text)
                        102 -> viewModel.prepareHighlight(locator)
                        103 -> viewModel.lookupWord(text, activity)
                    }
                    mode?.finish()
                }
                return true
            }
            override fun onDestroyActionMode(mode: ActionMode?) {}
        }
    }

    Scaffold(
        topBar = {
            ReaderTopBar(
                bookTitle = publication?.metadata?.title ?: "Loading...",
                isEink = isEink,
                showRecap = isAiEnabled,
                onBack = { viewModel.closeBook(); onBack() },
                onTocClick = { viewModel.toggleToc() },
                onSearchClick = { viewModel.toggleSearch() },
                onHighlightsClick = { viewModel.toggleHighlights() },
                onRecapClick = { viewModel.onRecapClicked() },
                onSettingsClick = { viewModel.toggleReaderSettings() }
            )
        },
        bottomBar = {
            if (publication != null && !showReaderSettings && !showToc && !showSearch && !showHighlights) {
                ReaderSystemFooter(chapterTitle = pageInfo, isEink = isEink)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
            if (publication == null) {
                CircularProgressIndicator(color = Color.Black)
            } else {
                AndroidView(
                    factory = { context ->
                        FrameLayout(context).apply {
                            id = View.generateViewId()
                            val fm = activity.supportFragmentManager
                            val existing = fm.findFragmentByTag("EPUB_READER_FRAGMENT")
                            if (existing != null) fm.beginTransaction().remove(existing).commitNow()

                            val factory = EpubNavigatorFactory(publication!!)
                            val fragment = factory.createFragmentFactory(
                                initialLocator = initialLocatorState,
                                configuration = EpubNavigatorFragment.Configuration().apply {
                                    selectionActionModeCallback = aiSelectionCallback
                                },
                                listener = navListener
                            ).instantiate(activity.classLoader, EpubNavigatorFragment::class.java.name) as EpubNavigatorFragment

                            scope.launch { fragment.currentLocator.collect { locator -> viewModel.updateProgress(locator) } }
                            currentNavigatorFragment = fragment
                            fm.beginTransaction().replace(this.id, fragment, "EPUB_READER_FRAGMENT").commit()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (isRecapLoading) {
                AlertDialog(onDismissRequest = {}, title = { Text("Generating Recap...") }, text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }, confirmButton = {}, modifier = Modifier.zIndex(20f))
            }

            if (showRecapConfirmation) {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissRecapConfirmation() },
                    title = { Text("Quick Recap") },
                    text = { Text("Would you like to generate a quick recap of the book so far?") },
                    confirmButton = { TextButton(onClick = { viewModel.generateRecap() }) { Text("Yes", fontWeight = FontWeight.Bold) } },
                    dismissButton = { TextButton(onClick = { viewModel.dismissRecapConfirmation() }) { Text("No", color = Color.Gray) } },
                    modifier = Modifier.zIndex(20f)
                )
            }

            recapText?.let { text ->
                AlertDialog(
                    onDismissRequest = { viewModel.dismissRecapResult() },
                    title = { Text("The Story So Far") },
                    text = { Column(modifier = Modifier.verticalScroll(rememberScrollState())) { Text(text, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray) } },
                    confirmButton = { TextButton(onClick = { viewModel.saveRecapAsHighlight() }) { Text("Save to Highlights", fontWeight = FontWeight.Bold) } },
                    dismissButton = { TextButton(onClick = { viewModel.dismissRecapResult() }) { Text("Dismiss", color = Color.Gray) } },
                    modifier = Modifier.zIndex(20f)
                )
            }

            if (showSearch) {
                Surface(modifier = Modifier.fillMaxSize().zIndex(15f)) {
                    BookSearchOverlay(query = bookSearchQuery, results = searchResults, isSearching = isBookSearching, onQueryChange = { viewModel.searchInBook(it) }, onSearch = { viewModel.searchInBook(it) }, onResultClick = { viewModel.onSearchResultClicked(it) }, onDismiss = { viewModel.toggleSearch() }, isEink = isEink)
                }
            }

            if (showHighlights) {
                Surface(modifier = Modifier.fillMaxSize().zIndex(15f)) {
                    BookHighlightsOverlay(highlights = highlightsList, onHighlightClick = { viewModel.onHighlightClicked(it) }, onDismiss = { viewModel.toggleHighlights() }, isEink = isEink)
                }
            }

            if (showToc && publication != null) {
                Surface(modifier = Modifier.fillMaxSize().zIndex(15f)) {
                    TableOfContents(toc = publication!!.tableOfContents, currentHref = currentLocator?.href?.toString(), onLinkSelected = { link -> viewModel.onTocItemSelected(link) }, onDismiss = { viewModel.toggleToc() }, isEink = isEink)
                }
            }

            if (showReaderSettings) {
                Surface(modifier = Modifier.fillMaxSize().zIndex(10f)) {
                    ReaderSettingsSheet(
                        viewModel = viewModel,
                        isEink = isEink,
                        onDismiss = { viewModel.dismissReaderSettings() }
                    )
                }
            }

            if (isBottomSheetVisible) {
                AiBottomSheet(responseText = aiResponse, isImage = isImageResponse, isDictionary = isDictionaryLookup, isDictionaryLoading = isDictionaryLoading, isEink = isEink, onExplain = { viewModel.onActionExplain() }, onWhoIsThis = { viewModel.onActionWhoIsThis() }, onVisualize = { viewModel.onActionVisualize() }, onDismiss = { viewModel.dismissBottomSheet() })
            }

            if (showTagSelector) {
                TagSelectorDialog(onTagSelected = { tag -> viewModel.saveHighlightWithTag(tag) }, onDismiss = { viewModel.dismissTagSelector() })
            }

            if (showDeleteDialogId != null) {
                AlertDialog(onDismissRequest = { showDeleteDialogId = null }, title = { Text("Delete Highlight?") }, text = { Text("This action cannot be undone.") }, confirmButton = { TextButton(onClick = { viewModel.deleteHighlight(showDeleteDialogId!!); showDeleteDialogId = null }) { Text("Delete", color = Color.Red) } }, dismissButton = { TextButton(onClick = { showDeleteDialogId = null }) { Text("Cancel") } }, modifier = Modifier.zIndex(4f))
            }
        }
    }
}

// Helper Function
@Composable
fun TagSelectorDialog(
    onTagSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val tags = listOf("General", "Research", "Quotes", "Characters")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Highlight As...") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tags.forEach { tag ->
                    OutlinedButton(
                        onClick = { onTagSelected(tag) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                    ) { Text(tag) }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        modifier = Modifier.zIndex(5f)
    )
}