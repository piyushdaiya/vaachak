package io.github.piyushdaiya.vaachak

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import dagger.hilt.android.AndroidEntryPoint
import io.github.piyushdaiya.vaachak.ui.bookshelf.BookshelfScreen
import io.github.piyushdaiya.vaachak.ui.highlights.AllHighlightsScreen
import io.github.piyushdaiya.vaachak.ui.reader.ReaderScreen
import io.github.piyushdaiya.vaachak.ui.reader.components.VaachakHeader
import io.github.piyushdaiya.vaachak.ui.settings.AboutScreen
import io.github.piyushdaiya.vaachak.ui.settings.SettingsScreen
import io.github.piyushdaiya.vaachak.ui.settings.SettingsViewModel
import io.github.piyushdaiya.vaachak.ui.theme.VaachakTheme
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import androidx.activity.viewModels
import androidx.compose.foundation.LocalIndication
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.material.icons.automirrored.filled.List
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intentUriString = intent?.data?.toString()

        setContent {
            val isEinkEnabled by settingsViewModel.isEinkEnabled.collectAsState()

            CompositionLocalProvider(
                LocalIndication provides if (isEinkEnabled) NoIndication else LocalIndication.current
            ){
                VaachakTheme(isEinkMode = isEinkEnabled) {
                    var currentBookUri by remember { mutableStateOf(intentUriString) }
                    var selectedTab by remember { mutableIntStateOf(0) }
                    var showSettingsOnHome by remember { mutableStateOf(false) }

                    // NEW: State for Session Recall overlay
                    var showSessionHistory by remember { mutableStateOf(false) }

                    var targetLocator by remember { mutableStateOf<String?>(null) }
                    var targetHighlightLocator by remember { mutableStateOf<String?>(null) }

                    // Back button handling for overlays
                    if (showSettingsOnHome || showSessionHistory) {
                        BackHandler {
                            showSettingsOnHome = false
                            showSessionHistory = false
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (currentBookUri != null) {
                            ReaderScreen(
                                initialUri = currentBookUri,
                                initialLocatorJson = targetHighlightLocator ?: targetLocator,
                                onBack = {
                                    currentBookUri = null
                                    targetLocator = null
                                    targetHighlightLocator = null
                                }
                            )
                        } else {
                            Scaffold(
                                topBar = {
                                    VaachakHeader(
                                        title = "My Bookshelf",
                                        showBackButton = false,
                                        onBack = { /* No-op */ },
                                        onSettingsClick = { showSettingsOnHome = true }
                                    )
                                },
                                bottomBar = {
                                    NavigationBar(containerColor = Color.White) {
                                        NavigationBarItem(
                                            icon = { Icon(Icons.Default.Home, "Bookshelf") },
                                            label = { Text("Bookshelf") },
                                            selected = selectedTab == 0,
                                            onClick = { selectedTab = 0 }
                                        )
                                        NavigationBarItem(
                                            icon = { Icon(Icons.AutoMirrored.Filled.List, "Highlights") },
                                            label = { Text("Highlights") },
                                            selected = selectedTab == 1,
                                            onClick = { selectedTab = 1 }
                                        )
                                        NavigationBarItem(
                                            icon = { Icon(Icons.Default.Info, "About") },
                                            label = { Text("About") },
                                            selected = selectedTab == 2,
                                            onClick = { selectedTab = 2 }
                                        )
                                    }
                                }
                            ) { padding ->
                                Surface(modifier = Modifier.padding(padding), color = Color.White) {
                                    when (selectedTab) {
                                        0 -> BookshelfScreen(
                                            onBookClick = { uri -> currentBookUri = uri },
                                            onRecallClick = { showSessionHistory = true } // NEW TRIGGER
                                        )
                                        1 -> AllHighlightsScreen(
                                            onBack = { selectedTab = 0 },
                                            onHighlightClick = { uri, locator ->
                                                targetHighlightLocator = locator
                                                currentBookUri = uri
                                            }
                                        )
                                        2 -> AboutScreen(onBack = { selectedTab = 0 })
                                    }
                                }
                            }
                        }

                        // --- NEW: SESSION HISTORY OVERLAY ---
                        if (showSessionHistory) {
                            Surface(
                                modifier = Modifier.fillMaxSize().zIndex(6f),
                                color = Color.White
                            ) {
                                io.github.piyushdaiya.vaachak.ui.session.SessionHistoryScreen(
                                    onBack = { showSessionHistory = false },
                                    onLaunchBook = { uri ->
                                        showSessionHistory = false
                                        currentBookUri = uri // This triggers Reader mode
                                    }
                                )
                            }
                        }

                        // Settings Overlay
                        if (showSettingsOnHome) {
                            Surface(
                                modifier = Modifier.fillMaxSize().zIndex(5f),
                                color = Color.White
                            ) {
                                SettingsScreen(onBack = { showSettingsOnHome = false })
                            }
                        }
                    }
                }
            }
        }
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val isPageForward = keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_PAGE_DOWN
        val isPageBackward = keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_PAGE_UP

        if (isPageForward || isPageBackward) {
            val fragment = supportFragmentManager.findFragmentByTag("EPUB_READER_FRAGMENT")
                    as? EpubNavigatorFragment

            fragment?.let { navigator ->
                if (isPageForward) navigator.goForward(animated = false)
                else navigator.goBackward(animated = false)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
    private object NoIndication : IndicationNodeFactory {
        override fun create(interactionSource: InteractionSource): DelegatableNode {
            return object : Modifier.Node(), DelegatableNode {}
        }

        override fun hashCode(): Int = -1
        override fun equals(other: Any?): Boolean = other === this
    }

}