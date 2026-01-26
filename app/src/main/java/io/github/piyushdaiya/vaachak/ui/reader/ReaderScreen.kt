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

@OptIn(ExperimentalReadiumApi::class)
@Composable
fun ReaderScreen(
    initialUri: String?,
    onBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val activity = LocalContext.current as AppCompatActivity
    val publication by viewModel.publication.collectAsState()

    // UI State
    var showSettings by remember { mutableStateOf(false) }
    var showDeleteDialogId by remember { mutableStateOf<Long?>(null) }

    // NEW: Track when the fragment is safely attached to the activity
    var isNavigatorReady by remember { mutableStateOf(false) }

    // AI State
    val isBottomSheetVisible by viewModel.isBottomSheetVisible.collectAsState()
    val aiResponse by viewModel.aiResponse.collectAsState()
    val isImageResponse by viewModel.isImageResponse.collectAsState()

    // Highlights Data
    val savedHighlights by viewModel.currentBookHighlights.collectAsState()

    // Back Handler
    BackHandler(onBack = {
        if (showDeleteDialogId != null) {
            showDeleteDialogId = null
        } else if (showSettings) {
            showSettings = false
        } else if (isBottomSheetVisible) {
            viewModel.dismissBottomSheet()
        } else {
            onBack()
        }
    })

    val scope = rememberCoroutineScope()
    var currentNavigatorFragment by remember { mutableStateOf<EpubNavigatorFragment?>(null) }

    LaunchedEffect(initialUri) {
        initialUri?.let { uriString ->
            viewModel.onFileSelected(uriString.toUri())
        }
    }

    // 1. Paint Highlights (ONLY when ready)
    LaunchedEffect(savedHighlights, currentNavigatorFragment, isNavigatorReady) {
        val navigator = currentNavigatorFragment ?: return@LaunchedEffect
        if (!isNavigatorReady) return@LaunchedEffect // GUARD: Prevent crash

        val decorations = savedHighlights.mapNotNull { entity ->
            try {
                val locator = Locator.fromJSON(JSONObject(entity.locatorJson)) ?: return@mapNotNull null
                val transparentColor = ColorUtils.setAlphaComponent(entity.color, 80)

                Decoration(
                    id = entity.id.toString(),
                    locator = locator,
                    style = Decoration.Style.Highlight(tint = transparentColor)
                )
            } catch (e: Exception) {
                null
            }
        }
        navigator.applyDecorations(decorations, "user_highlights")
    }

    // 2. Define the Listener
    val decorationListener = remember {
        object : DecorableNavigator.Listener {
            override fun onDecorationActivated(event: DecorableNavigator.OnActivatedEvent): Boolean {
                val highlightId = event.decoration.id.toLongOrNull()
                if (highlightId != null) {
                    showDeleteDialogId = highlightId
                    return true
                }
                return false
            }
        }
    }

    // 3. FIX: Manage Lifecycle to prevent "Fragment not attached" crash
    DisposableEffect(currentNavigatorFragment) {
        val navigator = currentNavigatorFragment
        if (navigator == null) return@DisposableEffect onDispose { }

        val fm = activity.supportFragmentManager

        // Callback to detect when fragment is truly ready
        val lifecycleCallbacks = object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
                if (f == navigator) {
                    isNavigatorReady = true
                    // Safe to add listener now
                    navigator.addDecorationListener("user_highlights", decorationListener)
                }
            }
        }

        // If already added, set ready immediately. Otherwise, register callback.
        if (navigator.isAdded && !navigator.isDetached) {
            isNavigatorReady = true
            navigator.addDecorationListener("user_highlights", decorationListener)
        } else {
            fm.registerFragmentLifecycleCallbacks(lifecycleCallbacks, false)
        }

        onDispose {
            fm.unregisterFragmentLifecycleCallbacks(lifecycleCallbacks)
            if (navigator.isAdded) {
                // Try-catch block because accessing navigator during destroy can sometimes be flaky
                try {
                    navigator.removeDecorationListener(decorationListener)
                } catch (e: Exception) {
                    // Ignore cleanup errors
                }
            }
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
                    val selectedText = locator.text.highlight ?: ""
                    when (item?.itemId) {
                        101 -> {
                            viewModel.onTextSelected(selectedText)
                            mode?.finish()
                        }
                        102 -> {
                            viewModel.saveHighlight(locator, android.graphics.Color.YELLOW)
                            currentNavigatorFragment?.clearSelection()
                            mode?.finish()
                        }
                    }
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
                onSettingsClick = { showSettings = true }
            )
        },
        bottomBar = { if (publication != null && !showSettings && showDeleteDialogId == null) VaachakFooter() },
        containerColor = Color.White
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            if (publication == null) {
                CircularProgressIndicator(color = Color.Black)
            } else {
                AndroidView(
                    factory = { context ->
                        FrameLayout(context).apply {
                            id = View.generateViewId()
                            setBackgroundColor(android.graphics.Color.WHITE)
                            setLayerType(View.LAYER_TYPE_SOFTWARE, null)

                            val existingFragment = activity.supportFragmentManager.findFragmentByTag("EPUB_READER_FRAGMENT")

                            if (existingFragment == null) {
                                val navigatorFactory = EpubNavigatorFactory(publication!!)
                                val fragmentConfig = EpubNavigatorFragment.Configuration().apply {
                                    selectionActionModeCallback = aiSelectionCallback
                                }
                                val fragmentFactory = navigatorFactory.createFragmentFactory(
                                    initialLocator = null,
                                    listener = null,
                                    configuration = fragmentConfig
                                )
                                activity.supportFragmentManager.fragmentFactory = fragmentFactory
                                val fragment = activity.supportFragmentManager.fragmentFactory.instantiate(
                                    activity.classLoader,
                                    EpubNavigatorFragment::class.java.name
                                ) as EpubNavigatorFragment

                                currentNavigatorFragment = fragment
                                activity.supportFragmentManager.beginTransaction()
                                    .replace(this.id, fragment, "EPUB_READER_FRAGMENT")
                                    .commit()
                            } else {
                                currentNavigatorFragment = existingFragment as EpubNavigatorFragment
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (showSettings) {
                Surface(modifier = Modifier.fillMaxSize().zIndex(2f), color = Color.White) {
                    SettingsScreen(onBack = { showSettings = false })
                }
            }

            if (isBottomSheetVisible) {
                Box(modifier = Modifier.fillMaxSize().zIndex(3f)) {
                    AiBottomSheet(
                        responseText = aiResponse,
                        isImage = isImageResponse,
                        onExplain = { viewModel.onActionExplain() },
                        onWhoIsThis = { viewModel.onActionWhoIsThis() },
                        onVisualize = { viewModel.onActionVisualize() },
                        onDismiss = { viewModel.dismissBottomSheet() }
                    )
                }
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