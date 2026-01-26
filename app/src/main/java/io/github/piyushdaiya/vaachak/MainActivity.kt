package io.github.piyushdaiya.vaachak

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dagger.hilt.android.AndroidEntryPoint
import io.github.piyushdaiya.vaachak.ui.highlights.AllHighlightsScreen
import io.github.piyushdaiya.vaachak.ui.reader.ReaderScreen
import io.github.piyushdaiya.vaachak.ui.reader.components.VaachakHeader
import io.github.piyushdaiya.vaachak.ui.settings.SettingsScreen
import io.github.piyushdaiya.vaachak.ui.theme.VaachakTheme
import org.readium.r2.navigator.epub.EpubNavigatorFragment

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intentUriString = intent?.data?.toString()

        setContent {
            VaachakTheme {
                var bookUriString by remember { mutableStateOf(intentUriString) }
                // Navigation States
                var showSettingsOnHome by remember { mutableStateOf(false) }
                // NEW: State for showing the highlights screen
                var showHighlightsScreen by remember { mutableStateOf(false) }

                val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri: Uri? ->
                    if (uri != null) {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        bookUriString = uri.toString()
                    }
                }

                when {
                    // CASE A: Book is Open
                    bookUriString != null -> {
                        ReaderScreen(
                            initialUri = bookUriString,
                            onBack = { bookUriString = null }
                        )
                    }
                    // CASE B: Highlights Screen is Open
                    showHighlightsScreen -> {
                        AllHighlightsScreen(onBack = { showHighlightsScreen = false })
                    }
                    // CASE C: Home Screen
                    else -> {
                        Scaffold(
                            topBar = {
                                VaachakHeader(
                                    title = "Vaachak Library",
                                    onSettingsClick = { showSettingsOnHome = true }
                                )
                            }
                        ) { padding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(padding),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Button(
                                        onClick = { launcher.launch(arrayOf("application/epub+zip")) },
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    ) {
                                        Text("Open eBook")
                                    }
                                    // NEW BUTTON: My Highlights
                                    OutlinedButton(onClick = { showHighlightsScreen = true }) {
                                        Icon(Icons.Default.List, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("My Highlights")
                                    }
                                }

                                // Show Settings Overlay if clicked
                                if (showSettingsOnHome) {
                                    Surface(
                                        modifier = Modifier.fillMaxSize().zIndex(1f),
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
        }
    }

    // Hardware Buttons Logic (Preserved)
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
}