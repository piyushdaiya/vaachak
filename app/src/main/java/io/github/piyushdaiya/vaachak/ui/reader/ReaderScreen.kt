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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.graphics.ColorUtils
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.piyushdaiya.vaachak.ui.reader.components.VaachakFooter
import io.github.piyushdaiya.vaachak.ui.reader.components.VaachakHeader
import io.github.piyushdaiya.vaachak.ui.settings.SettingsScreen
import io.github.piyushdaiya.vaachak.ui.reader.components.AiBottomSheet
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.readium.r2.navigator.DecorableNavigator
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.AbsoluteUrl


@OptIn(ExperimentalReadiumApi::class)
@Composable
fun ReaderScreen(
    initialUri: String?,
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
    val savedHighlights by viewModel.currentBookHighlights.collectAsState()
// In the Scaffold...
    val pageInfo by viewModel.currentPageInfo.collectAsState()
    val scope = rememberCoroutineScope()
    var currentNavigatorFragment by remember { mutableStateOf<EpubNavigatorFragment?>(null) }

    // Readium 2.4.0+ Compliant Listener
    val navListener = remember {
        object : EpubNavigatorFragment.Listener {
            // This captures jumps (TOC, Page Jumps)
            override fun onJumpToLocator(locator: Locator) {
                viewModel.updateProgress(locator)
            }

            // REQUIRED for the interface: Handles external links
            override fun onExternalLinkActivated(url: AbsoluteUrl) {
                // Optional: Open in browser
            }
        }
    }

    BackHandler {
        if (showDeleteDialogId != null) showDeleteDialogId = null
        else if (showSettings) showSettings = false
        else if (isBottomSheetVisible) viewModel.dismissBottomSheet()
        else {
            viewModel.closeBook()
            onBack()
        }
    }

    LaunchedEffect(initialUri) {
        initialUri?.let { viewModel.onFileSelected(it.toUri()) }
    }

    // Decoration Listener
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
                try {
                    navigator.removeDecorationListener(decorationListener)
                } catch (e: Exception) {}
            }
            isNavigatorReady = false
        }
    }

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
                        102 -> viewModel.saveHighlight(locator, android.graphics.Color.YELLOW)
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

                            val existing = activity.supportFragmentManager.findFragmentByTag("EPUB_READER_FRAGMENT")
                            if (existing != null) {
                                activity.supportFragmentManager.beginTransaction().remove(existing).commitNow()
                            }

                            val factory = EpubNavigatorFactory(publication!!)
                            val fragment = factory.createFragmentFactory(
                                initialLocator = null,
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
                            activity.supportFragmentManager.beginTransaction()
                                .replace(this.id, fragment, "EPUB_READER_FRAGMENT")
                                .commit()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (showSettings) {
                Surface(modifier = Modifier.fillMaxSize().zIndex(2f)) {
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