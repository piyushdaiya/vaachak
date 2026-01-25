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
}