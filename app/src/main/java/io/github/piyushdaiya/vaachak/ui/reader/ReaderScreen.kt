package io.github.piyushdaiya.vaachak.ui.reader

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.piyushdaiya.vaachak.ui.reader.components.VaachakFooter
import io.github.piyushdaiya.vaachak.ui.reader.components.VaachakHeader
import io.github.piyushdaiya.vaachak.ui.settings.SettingsScreen
import io.github.piyushdaiya.vaachak.ui.reader.components.AiBottomSheet
import kotlinx.coroutines.launch
import org.readium.r2.navigator.DecorableNavigator
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.AbsoluteUrl
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontWeight
@OptIn(ExperimentalMaterial3Api::class, ExperimentalReadiumApi::class)
@Composable
fun ReaderScreen(
    initialUri: String?,
    initialLocatorJson: String?,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val activity = LocalContext.current as AppCompatActivity
    val publication by viewModel.publication.collectAsState()
    val currentLocator by viewModel.currentLocator.collectAsState()

    var showSettings by remember { mutableStateOf(false) }
    var showDeleteDialogId by remember { mutableStateOf<Long?>(null) }
    var isNavigatorReady by remember { mutableStateOf(false) }

    val isBottomSheetVisible by viewModel.isBottomSheetVisible.collectAsState()
    val aiResponse by viewModel.aiResponse.collectAsState()
    val isImageResponse by viewModel.isImageResponse.collectAsState()

    // NEW: Tag selection state
    val showTagSelector by viewModel.showTagSelector.collectAsState()

    val pageInfo by viewModel.currentPageInfo.collectAsState()
    val scope = rememberCoroutineScope()
    var currentNavigatorFragment by remember { mutableStateOf<EpubNavigatorFragment?>(null) }
    val initialLocator by viewModel.initialLocator.collectAsState()
    val savedHighlights by viewModel.currentBookHighlights.collectAsState()

    //recap
    val recapText by viewModel.recapText.collectAsState()
    val isRecapLoading by viewModel.isRecapLoading.collectAsState()

    LaunchedEffect(savedHighlights, isNavigatorReady) {
        val navigator = currentNavigatorFragment ?: return@LaunchedEffect
        if (isNavigatorReady) {
            // This tells Readium to paint the yellow backgrounds
            navigator.applyDecorations(savedHighlights, "user_highlights")
        }
    }

    LaunchedEffect(initialUri, initialLocatorJson) {
        if (initialUri != null) {
            viewModel.setInitialLocation(initialLocatorJson)
            viewModel.onFileSelected(initialUri.toUri())
        }
    }

    val navListener = remember {
        object : EpubNavigatorFragment.Listener {
            override fun onJumpToLocator(locator: Locator) {
                viewModel.updateProgress(locator)
            }
            override fun onExternalLinkActivated(url: AbsoluteUrl) {}
        }
    }

    BackHandler {
        if (showDeleteDialogId != null) showDeleteDialogId = null
        else if (showTagSelector) viewModel.dismissTagSelector()
        else if (showSettings) showSettings = false
        else if (isBottomSheetVisible) viewModel.dismissBottomSheet()
        else {
            viewModel.closeBook()
            onBack()
        }
    }

    val decorationListener = remember {
        object : DecorableNavigator.Listener {
            override fun onDecorationActivated(event: DecorableNavigator.OnActivatedEvent): Boolean {
                if (event.group == "user_highlights") {
                    event.decoration.id.toLongOrNull()?.let {
                        showDeleteDialogId = it
                        return true
                    }
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
                if (f == navigator) {
                    isNavigatorReady = true
                    navigator.addDecorationListener("user_highlights", decorationListener)
                }
            }
        }
        if (navigator.isAdded && !navigator.isDetached) {
            isNavigatorReady = true
            navigator.addDecorationListener("user_highlights", decorationListener)
        } else {
            fm.registerFragmentLifecycleCallbacks(callbacks, false)
        }
        onDispose {
            fm.unregisterFragmentLifecycleCallbacks(callbacks)
            if (navigator.isAdded) {
                try { navigator.removeDecorationListener(decorationListener) } catch (e: Exception) {}
            }
            isNavigatorReady = false
        }
    }

    // --- UPDATED ACTION MODE CALLBACK ---
    val aiSelectionCallback = remember {
        object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                menu?.add(Menu.NONE, 101, 0, "Ask AI")
                menu?.add(Menu.NONE, 102, 1, "Highlight")
                return true
            }
            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = false
            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                scope.launch {
                    val selection = currentNavigatorFragment?.currentSelection()
                    val locator = selection?.locator ?: return@launch
                    when (item?.itemId) {
                        101 -> viewModel.onTextSelected(locator.text.highlight ?: "")
                        // Intercept Highlight to show Tag Selector
                        102 -> viewModel.prepareHighlight(locator)
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
            VaachakHeader(
                title = publication?.metadata?.title ?: "Loading...",
                onBack = {
                    viewModel.closeBook()
                    onBack()
                },
                onSettingsClick = { showSettings = true }
            )
        },
        bottomBar = {
            if (publication != null && !showSettings) {
                VaachakFooter(pageInfo = pageInfo)
            }
        }
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
                            if (existing != null) {
                                fm.beginTransaction().remove(existing).commitNow()
                            }

                            val factory = EpubNavigatorFactory(publication!!)
                            val fragment = factory.createFragmentFactory(
                                initialLocator = initialLocator,
                                configuration = EpubNavigatorFragment.Configuration().apply {
                                    selectionActionModeCallback = aiSelectionCallback
                                },
                                listener = navListener
                            ).instantiate(activity.classLoader, EpubNavigatorFragment::class.java.name) as EpubNavigatorFragment

                            scope.launch {
                                fragment.currentLocator.collect { locator ->
                                    viewModel.updateProgress(locator)
                                }
                            }
                            currentNavigatorFragment = fragment
                            fm.beginTransaction()
                                .replace(this.id, fragment, "EPUB_READER_FRAGMENT")
                                .commit()
                        }
                    },
                    modifier = Modifier.fillMaxSize().padding(padding)
                )
            }

            // --- OVERLAYS ---

            if (showSettings) {
                Surface(modifier = Modifier.fillMaxSize()
                    .zIndex(10f)) {
                    SettingsScreen(onBack = { showSettings = false })
                }
            }

            if (isBottomSheetVisible) {
                AiBottomSheet(
                    responseText = aiResponse,
                    isImage = isImageResponse,
                    onExplain = { viewModel.onActionExplain() },
                    onWhoIsThis = { viewModel.onActionWhoIsThis() },
                    onVisualize = { viewModel.onActionVisualize() },
                    onDismiss = { viewModel.dismissBottomSheet() }
                )
            }

            if (isRecapLoading) {
                AlertDialog(
                    onDismissRequest = { },
                    confirmButton = { },
                    title = { Text("Analyzing your journey...") },
                    text = {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.Black)
                        }
                    },
                    modifier = Modifier.zIndex(10f)
                )
            }
            // 3. Recap Result Dialog
            recapText?.let { text ->
                AlertDialog(
                    onDismissRequest = { viewModel.dismissRecap() },
                    title = { Text("The Story So Far") },
                    text = {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            Text(text, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel.dismissRecap() }) {
                            Text("Back to Reading", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        // Existing Close Button
                        TextButton(onClick = { viewModel.dismissRecap() }) {
                            Text("Dismiss", color = Color.Gray)
                        }
                    },
                    modifier = Modifier.zIndex(10f)
                )
            }
            // --- NEW: TAG SELECTOR ---
            if (showTagSelector) {
                TagSelectorDialog(
                    onTagSelected = { tag -> viewModel.saveHighlightWithTag(tag) },
                    onDismiss = { viewModel.dismissTagSelector() }
                )
            }

            if (showDeleteDialogId != null) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialogId = null },
                    title = { Text("Delete Highlight?") },
                    text = { Text("This action cannot be undone.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.deleteHighlight(showDeleteDialogId!!)
                                showDeleteDialogId = null
                            }
                        ) { Text("Delete", color = Color.Red) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialogId = null }) { Text("Cancel") }
                    },
                    modifier = Modifier.zIndex(4f)
                )
            }
        }
    }
}

// --- HELPER COMPONENT ---
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
                    ) {
                        Text(tag)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        modifier = Modifier.zIndex(5f)
    )
}