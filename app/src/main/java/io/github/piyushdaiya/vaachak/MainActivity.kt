package io.github.piyushdaiya.vaachak

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity // <-- FIXED IMPORT
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import io.github.piyushdaiya.vaachak.ui.reader.ReaderScreen
import io.github.piyushdaiya.vaachak.ui.settings.SettingsScreen
//v2.0 additions
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import android.view.KeyEvent
@AndroidEntryPoint
// THE FIX: Changed from ComponentActivity to AppCompatActivity
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "reader") {

                        composable("reader") {
                            ReaderScreen(
                                onNavigateToSettings = { navController.navigate("settings") }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
    // THE ADDITION: Native Hardware Button Support
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // 1. Define what keys act as "Page Turns" (Volume Keys + Page Keys)
        val isPageForward = keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_PAGE_DOWN
        val isPageBackward = keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_PAGE_UP

        if (isPageForward || isPageBackward) {
            // 2. Look for the fragment we tagged in ReaderScreen.kt
            val fragment = supportFragmentManager.findFragmentByTag("EPUB_READER_FRAGMENT")
                    as? EpubNavigatorFragment

            // 3. If the fragment is visible, turn the page natively
            fragment?.let { navigator ->
                if (isPageForward) {
                    navigator.goForward(animated = false) // False = Instant E-Ink turn
                } else {
                    navigator.goBackward(animated = false)
                }
                return true // Consume the event so volume doesn't actually change
            }
        }

        return super.onKeyDown(keyCode, event)
    }
}