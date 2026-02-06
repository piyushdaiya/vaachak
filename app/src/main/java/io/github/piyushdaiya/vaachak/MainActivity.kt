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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.github.piyushdaiya.vaachak.ui.bookshelf.BookshelfScreen
import io.github.piyushdaiya.vaachak.ui.bookshelf.BookshelfViewModel
import io.github.piyushdaiya.vaachak.ui.highlights.AllHighlightsScreen
import io.github.piyushdaiya.vaachak.ui.reader.ReaderScreen
import io.github.piyushdaiya.vaachak.ui.reader.components.VaachakNavigationFooter
import io.github.piyushdaiya.vaachak.ui.settings.AboutScreen
import io.github.piyushdaiya.vaachak.ui.settings.SettingsScreen
import io.github.piyushdaiya.vaachak.ui.settings.SettingsViewModel
import io.github.piyushdaiya.vaachak.ui.theme.ThemeMode
import io.github.piyushdaiya.vaachak.ui.theme.VaachakTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.util.zip.ZipFile

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val bookshelfViewModel: BookshelfViewModel by viewModels()

    // State to control the UI
    private val appState = mutableStateOf(AppState())

    data class AppState(
        val currentBookUri: String? = null,
        val isLoading: Boolean = false,
        val readerKey: Int = 0 // Used to force-recreate Reader
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle "Open With" on cold start
        if (savedInstanceState == null) {
            processExternalIntent(intent)
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val currentTheme by settingsViewModel.themeMode.collectAsState()
            val einkContrastValue by settingsViewModel.einkContrast.collectAsState()
            val isEinkActive = currentTheme == ThemeMode.E_INK

            val state by appState

            CompositionLocalProvider(
                LocalIndication provides if (isEinkActive) NoIndication else LocalIndication.current
            ) {
                VaachakTheme(themeMode = currentTheme, contrast = einkContrastValue) {

                    var selectedTab by remember { mutableIntStateOf(0) }
                    var showSettingsOnHome by remember { mutableStateOf(false) }
                    var showSessionHistory by remember { mutableStateOf(false) }

                    var targetLocator by remember { mutableStateOf<String?>(null) }
                    var targetHighlightLocator by remember { mutableStateOf<String?>(null) }

                    // Handle Back Press
                    if (state.currentBookUri != null || showSettingsOnHome || showSessionHistory) {
                        BackHandler {
                            if (showSettingsOnHome) showSettingsOnHome = false
                            else if (showSessionHistory) showSessionHistory = false
                            else closeReader()
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {

                        // READER VIEW
                        if (state.currentBookUri != null) {
                            key(state.readerKey) {
                                ReaderScreen(
                                    initialUri = state.currentBookUri!!,
                                    initialLocatorJson = targetHighlightLocator ?: targetLocator,
                                    onBack = { closeReader() }
                                )
                            }
                        } else {
                            // MAIN APP VIEW
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
                                    modifier = Modifier.padding(padding).fillMaxSize(),
                                    color = if (isEinkActive) Color.White else MaterialTheme.colorScheme.background
                                ) {
                                    when (selectedTab) {
                                        0 -> BookshelfScreen(
                                            onBookClick = { uri -> openBook(uri) },
                                            onRecallClick = { showSessionHistory = true },
                                            onSettingsClick = { showSettingsOnHome = true }
                                        )
                                        1 -> AllHighlightsScreen(
                                            onBack = { selectedTab = 0 },
                                            onHighlightClick = { uri, locator ->
                                                targetHighlightLocator = locator
                                                openBook(uri)
                                            }
                                        )
                                        2 -> AboutScreen(onBack = { selectedTab = 0 })
                                    }
                                }
                            }
                        }

                        // LOADING SPINNER
                        if (state.isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .zIndex(99f)
                                    .padding(bottom = 50.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    shadowElevation = 6.dp
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                                }
                            }
                        }

                        // OVERLAYS
                        if (showSessionHistory) {
                            Surface(modifier = Modifier.fillMaxSize().zIndex(6f)) {
                                io.github.piyushdaiya.vaachak.ui.session.SessionHistoryScreen(
                                    onBack = { showSessionHistory = false },
                                    onLaunchBook = { uri ->
                                        showSessionHistory = false
                                        openBook(uri)
                                    }
                                )
                            }
                        }

                        if (showSettingsOnHome) {
                            Surface(modifier = Modifier.fillMaxSize().zIndex(5f)) {
                                SettingsScreen(onBack = { showSettingsOnHome = false })
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        processExternalIntent(intent)
    }

    private fun openBook(uri: String) {
        val nextKey = appState.value.readerKey + 1
        appState.value = appState.value.copy(
            currentBookUri = uri,
            readerKey = nextKey,
            isLoading = false
        )
    }

    private fun closeReader() {
        appState.value = appState.value.copy(currentBookUri = null)
    }

    private fun processExternalIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            val contentUri = intent.data!!

            appState.value = appState.value.copy(isLoading = true)

            lifecycleScope.launch {
                try {
                    // 1. Copy to App-Specific External Storage (WebView accessible)
                    val localFile = withContext(Dispatchers.IO) {
                        copyToExternalStorage(contentUri)
                    }

                    if (localFile != null && localFile.exists() && localFile.length() > 0) {

                        // 2. Extract Title from the temp file
                        val tempTitle = withContext(Dispatchers.IO) { getEpubTitle(localFile) } ?: ""

                        // 3. WAIT for Library to Load (Fixes startup race condition)
                        // We wait up to 2 seconds for the database to return results.
                        waitForDatabaseToLoad()

                        // 4. CHECK BOTH LISTS for duplicates
                        // Combine 'allBooks' and 'recentBooks' (if available) to be absolutely sure
                        val allKnownBooks = bookshelfViewModel.allBooks.value +
                                (try { bookshelfViewModel.recentBooks.value } catch(e: Exception) { emptyList() })

                        val existingBook = allKnownBooks.find { book ->
                            // Fuzzy match title (case insensitive, trimmed)
                            book.title.trim().equals(tempTitle.trim(), ignoreCase = true)
                        }

                        if (existingBook != null) {
                            // CASE: DUPLICATE FOUND
                            Log.d("VaachakMain", "Duplicate found: '${existingBook.title}'. Opening existing.")

                            // Delete the duplicate temp file
                            withContext(Dispatchers.IO) { localFile.delete() }

                            // Open the EXISTING book
                            openBook(existingBook.uriString)

                        } else {
                            // CASE: NEW BOOK
                            Log.d("VaachakMain", "New book. Importing.")
                            val localUri = Uri.fromFile(localFile)

                            // Import into DB
                            try {
                                bookshelfViewModel.importBook(localUri)
                            } catch (e: Exception) { e.printStackTrace() }

                            // Small delay to ensure DB write commits before Reader tries to access metadata
                            delay(300)
                            openBook(localUri.toString())
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Failed to load book file", Toast.LENGTH_SHORT).show()
                        appState.value = appState.value.copy(isLoading = false)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@MainActivity, "Error opening book", Toast.LENGTH_SHORT).show()
                    appState.value = appState.value.copy(isLoading = false)
                }
            }
        }
    }

    // Helper: Wait until database has data OR 2 seconds pass
    private suspend fun waitForDatabaseToLoad() {
        withTimeoutOrNull(2000) {
            while (bookshelfViewModel.allBooks.value.isEmpty()) {
                delay(100)
            }
        }
    }

    private fun copyToExternalStorage(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val booksDir = getExternalFilesDir("imported_books") ?: return null
            if (!booksDir.exists()) booksDir.mkdirs()

            val fileName = "book_${System.currentTimeMillis()}_${UUID.randomUUID()}.epub"
            val destFile = File(booksDir, fileName)

            val outputStream = FileOutputStream(destFile)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                    output.flush()
                    output.fd.sync()
                }
            }
            destFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Helper: Extract Title directly from EPUB
    private fun getEpubTitle(epubFile: File): String? {
        return try {
            ZipFile(epubFile).use { zip ->
                val containerEntry = zip.getEntry("META-INF/container.xml") ?: return null
                val containerXml = zip.getInputStream(containerEntry).bufferedReader().use { it.readText() }

                val opfPathMatch = Regex("full-path=\"([^\"]+)\"").find(containerXml)
                val opfPath = opfPathMatch?.groupValues?.get(1) ?: return null

                val opfEntry = zip.getEntry(opfPath) ?: return null
                val opfXml = zip.getInputStream(opfEntry).bufferedReader().use { it.readText() }

                val titleMatch = Regex("<dc:title[^>]*>(.*?)</dc:title>", RegexOption.DOT_MATCHES_ALL).find(opfXml)
                titleMatch?.groupValues?.get(1)
            }
        } catch (e: Exception) {
            Log.e("VaachakMain", "Failed to parse title", e)
            null
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
