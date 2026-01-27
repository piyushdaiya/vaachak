package io.github.piyushdaiya.vaachak.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val geminiKey by viewModel.geminiKey.collectAsState()
    val cfUrl by viewModel.cfUrl.collectAsState()
    val cfToken by viewModel.cfToken.collectAsState()
    // NEW: E-ink state from ViewModel
    val isEinkEnabled by viewModel.isEinkEnabled.collectAsState()
    var showSavedMessage by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    // ADD THIS LINE - You likely missed initializing the state variable
    var showResetDialog by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
            .imePadding() // FIX 2.2: Push content up when keyboard opens
            .verticalScroll(rememberScrollState()) // THE FIX: Allow scrolling
    ) {
        // --- DISPLAY SETTINGS ---
        Text("Display Settings", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))

        ListItem(
            headlineContent = { Text("E-ink Optimized Mode") },
            supportingContent = { Text("High contrast, no animations, and no ripples for E-paper displays.") },
            trailingContent = {
                Switch(
                    checked = isEinkEnabled,
                    onCheckedChange = {
                        viewModel.toggleEink(it)
                        // Settings are usually saved immediately for UI toggles
                        viewModel.saveSettings()
                    }
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), thickness = 0.5.dp)

        // --- AI SETTINGS ---
        Text("AI Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = geminiKey,
            onValueChange = { viewModel.updateGemini(it) },
            label = { Text("Gemini API Key") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = cfUrl,
            onValueChange = { viewModel.updateCfUrl(it) },
            label = { Text("Cloudflare Worker URL") },
            placeholder = { Text("https://your-worker.workers.dev/") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = cfToken,
            onValueChange = { viewModel.updateCfToken(it) },
            label = { Text("Cloudflare Auth Token") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack) { Text("Back to Reader") }
            Button(onClick = {
                keyboardController?.hide()
                viewModel.saveSettings()
                showSavedMessage = true
            }) { Text("Save Settings") }
        }

        if (showSavedMessage) {
            Text(
                "Settings Saved Successfully!",
                color = if (isEinkEnabled) Color.Black else Color(0xFF2E7D32),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp, bottom = 32.dp) // Added bottom padding
            )
            // Auto-hide message after 3 seconds
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(3000)
                showSavedMessage = false
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), thickness = 0.5.dp)
        Text("Danger Zone", style = MaterialTheme.typography.headlineSmall, color = Color.Red)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = { showResetDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
            border = BorderStroke(1.dp, Color.Red)
        ) {
            Text("Reset All Settings to Default")
        }

        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Reset Settings?") },
                text = { Text("This will clear your API keys and reset display preferences. This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.resetSettings()
                            showResetDialog = false
                        }
                    ) { Text("Reset", color = Color.Red) }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
                }
            )
        }
    }

}

