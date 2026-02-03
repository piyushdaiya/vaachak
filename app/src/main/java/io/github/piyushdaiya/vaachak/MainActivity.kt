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

package io.github.piyushdaiya.vaachak

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import io.github.piyushdaiya.vaachak.ui.bookshelf.BookshelfScreen
import io.github.piyushdaiya.vaachak.ui.highlights.AllHighlightsScreen
import io.github.piyushdaiya.vaachak.ui.reader.ReaderScreen
import io.github.piyushdaiya.vaachak.ui.reader.components.VaachakNavigationFooter
import io.github.piyushdaiya.vaachak.ui.settings.AboutScreen
import io.github.piyushdaiya.vaachak.ui.settings.SettingsScreen
import io.github.piyushdaiya.vaachak.ui.settings.SettingsViewModel
import io.github.piyushdaiya.vaachak.ui.theme.ThemeMode
import io.github.piyushdaiya.vaachak.ui.theme.VaachakTheme
import org.readium.r2.navigator.epub.EpubNavigatorFragment

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intentUriString = intent?.data?.toString()
        // FIX 1: Ensure edge-to-edge is enabled properly
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val currentTheme by settingsViewModel.themeMode.collectAsState()
            val einkContrastValue by settingsViewModel.einkContrast.collectAsState()
            val isEinkActive = currentTheme == ThemeMode.E_INK

            CompositionLocalProvider(
                LocalIndication provides if (isEinkActive) NoIndication else LocalIndication.current
            ) {
                VaachakTheme(
                    themeMode = currentTheme,
                    contrast = einkContrastValue
                ) {
                    var currentBookUri by remember { mutableStateOf(intentUriString) }
                    var selectedTab by remember { mutableIntStateOf(0) }

                    var showSettingsOnHome by remember { mutableStateOf(false) }
                    var showSessionHistory by remember { mutableStateOf(false) }

                    var targetLocator by remember { mutableStateOf<String?>(null) }
                    var targetHighlightLocator by remember { mutableStateOf<String?>(null) }

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
                                bottomBar = {
                                    VaachakNavigationFooter(
                                        onBookshelfClick = { selectedTab = 0 },
                                        onHighlightsClick = { selectedTab = 1 },
                                        onAboutClick = { selectedTab = 2 },
                                        isEink = isEinkActive
                                    )
                                },
                                containerColor = if (isEinkActive) Color.White else MaterialTheme.colorScheme.background
                            ) { padding ->
                                Surface(
                                    modifier = Modifier
                                        .padding(padding)
                                        .fillMaxSize(),
                                    color = if (isEinkActive) Color.White else MaterialTheme.colorScheme.background
                                ) {
                                    when (selectedTab) {
                                        0 -> BookshelfScreen(
                                            onBookClick = { uri -> currentBookUri = uri },
                                            // FIX: Correct wiring for Recall and Settings
                                            onRecallClick = { showSessionHistory = true },
                                            onSettingsClick = { showSettingsOnHome = true }
                                        )
                                        1 -> AllHighlightsScreen(
                                            onBack = { selectedTab = 0 },
                                            onHighlightClick = { uri, locator ->
                                                targetHighlightLocator = locator
                                                currentBookUri = uri
                                            }
                                        )
                                        2 -> AboutScreen(
                                            onBack = { selectedTab = 0 }
                                        )
                                    }
                                }
                            }
                        }

                        if (showSessionHistory) {
                            Surface(
                                modifier = Modifier.fillMaxSize().zIndex(6f),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                io.github.piyushdaiya.vaachak.ui.session.SessionHistoryScreen(
                                    onBack = { showSessionHistory = false },
                                    onLaunchBook = { uri ->
                                        showSessionHistory = false
                                        currentBookUri = uri
                                    }
                                )
                            }
                        }

                        if (showSettingsOnHome) {
                            Surface(
                                modifier = Modifier.fillMaxSize().zIndex(5f),
                                color = MaterialTheme.colorScheme.surface
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