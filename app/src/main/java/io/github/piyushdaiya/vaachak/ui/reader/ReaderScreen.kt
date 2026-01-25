package io.github.piyushdaiya.vaachak.ui.reader

import android.graphics.Color as AndroidColor
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.piyushdaiya.vaachak.ui.reader.components.AiBottomSheet
import io.github.piyushdaiya.vaachak.ui.reader.components.VaachakFooter
import io.github.piyushdaiya.vaachak.ui.reader.components.VaachakHeader
import kotlinx.coroutines.launch
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment

@Composable
fun ReaderScreen(
    onNavigateToSettings: () -> Unit, // ADDED: Navigation parameter
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val activity = LocalContext.current as AppCompatActivity
    val coroutineScope = rememberCoroutineScope()

    val showSheet by viewModel.isBottomSheetVisible.collectAsState()
    val aiResponse by viewModel.aiResponse.collectAsState()
    val isImage by viewModel.isImageResponse.collectAsState()
    val publication by viewModel.publication.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.onFileSelected(it) }
    }

    var currentNavigatorFragment: EpubNavigatorFragment? by remember { mutableStateOf(null) }

    // --- AI SELECTION INTERCEPTOR ---
    // ... inside ReaderScreen.kt ...
    val aiSelectionCallback = remember {
        object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                menu?.add(Menu.NONE, 101, 0, "Ask AI")
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = false

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                if (item?.itemId == 101) {
                    coroutineScope.launch {
                        val selection = currentNavigatorFragment?.currentSelection()
                        val locator = selection?.locator ?: return@launch
                        val selectedText = locator.text.highlight ?: ""

                        // SIMPLIFIED: Only need the text now.
                        viewModel.onTextSelected(selectedText)
                        mode?.finish()
                    }
                    return true
                }
                return false
            }
            override fun onDestroyActionMode(mode: ActionMode?) {}
        }
    }

    Scaffold(
        topBar = {
            VaachakHeader(
                title = publication?.metadata?.title ?: "Vaachak",
                onSettingsClick = onNavigateToSettings
            ) },
        bottomBar = { if (publication != null) VaachakFooter() },
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
                Button(onClick = { filePickerLauncher.launch(arrayOf("application/epub+zip")) }) {
                    Text("Open EPUB File")
                }
            } else {
                AndroidView(
                    factory = { context ->
                        FrameLayout(context).apply {
                            id = View.generateViewId()
                            setBackgroundColor(AndroidColor.WHITE)
                            setLayerType(View.LAYER_TYPE_SOFTWARE, null)

                            val existingFragment = activity.supportFragmentManager.findFragmentByTag("reader_fragment")
                            if (existingFragment == null) {
                                // 1. Set up the ENGINE Factory (Defaults)
                                val navigatorFactory = EpubNavigatorFactory(publication!!)

                                // 2. THE FIX: Set up the UI Fragment Configuration for the Text Selection
                                val fragmentConfig = EpubNavigatorFragment.Configuration().apply {
                                    selectionActionModeCallback = aiSelectionCallback
                                }

                                // 3. Pass the UI config into the create method
                                val fragmentFactory = navigatorFactory.createFragmentFactory(
                                    initialLocator = null,
                                    listener = null,
                                    configuration = fragmentConfig // <-- Inserted here!
                                )

                                activity.supportFragmentManager.fragmentFactory = fragmentFactory

                                val fragment = activity.supportFragmentManager.fragmentFactory.instantiate(
                                    activity.classLoader,
                                    EpubNavigatorFragment::class.java.name
                                ) as EpubNavigatorFragment

                                currentNavigatorFragment = fragment

                                activity.supportFragmentManager.beginTransaction()
                                    .replace(this.id, fragment, "reader_fragment")
                                    .commit()
                            } else {
                                currentNavigatorFragment = existingFragment as EpubNavigatorFragment
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (showSheet) {
        AiBottomSheet(
            responseText = aiResponse,
            isImage = isImage,
            onExplain = { viewModel.onActionExplain() },
            onWhoIsThis = { viewModel.onActionWhoIsThis() },
            onVisualize = { viewModel.onActionVisualize() },
            onDismiss = { viewModel.dismissBottomSheet() }
        )
    }
}