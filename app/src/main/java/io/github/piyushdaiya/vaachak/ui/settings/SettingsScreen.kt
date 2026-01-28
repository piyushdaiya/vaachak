package io.github.piyushdaiya.vaachak.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val geminiKey by viewModel.geminiKey.collectAsState()
    val cfUrl by viewModel.cfUrl.collectAsState()
    val cfToken by viewModel.cfToken.collectAsState()
    val isEinkEnabled by viewModel.isEinkEnabled.collectAsState()
    val isAutoSaveEnabled by viewModel.isAutoSaveRecapsEnabled.collectAsState()
    var showSavedMessage by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // Respects Eink/Theme colors
            .padding(16.dp)
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        // --- TOP NAVIGATION ---
        TextButton(
            onClick = onBack,
            modifier = Modifier.padding(bottom = 8.dp),
            contentPadding = PaddingValues(0.dp) // Align with edge
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Back to My Bookshelf", style = MaterialTheme.typography.labelLarge)
            }
        }

        HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp), thickness = 0.5.dp)

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
                        viewModel.saveSettings()
                    }
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
        ListItem(
            headlineContent = { Text("Auto-Save AI Recaps") },
            supportingContent = { Text("Automatically save generated summaries to your highlights.") },
            trailingContent = {
                Switch(
                    checked = isAutoSaveEnabled,
                    onCheckedChange = { viewModel.toggleAutoSaveRecaps(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = Color.Gray
                    )
                )
            }
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), thickness = 0.5.dp)

        // --- AI SETTINGS ---
        Text("AI Settings", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = geminiKey,
            onValueChange = { viewModel.updateGemini(it) },
            label = { Text("Gemini API Key") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = cfUrl,
            onValueChange = { viewModel.updateCfUrl(it) },
            label = { Text("Cloudflare Worker URL") },
            placeholder = { Text("https://your-worker.workers.dev/") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = cfToken,
            onValueChange = { viewModel.updateCfToken(it) },
            label = { Text("Cloudflare Auth Token") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(24.dp))

        // --- SAVE ACTION ---
        Button(
            onClick = {
                keyboardController?.hide()
                viewModel.saveSettings()
                showSavedMessage = true
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Save All Settings")
        }

        if (showSavedMessage) {
            Text(
                "Settings Saved Successfully!",
                color = if (isEinkEnabled) MaterialTheme.colorScheme.onBackground else Color(0xFF2E7D32),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(3000)
                showSavedMessage = false
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), thickness = 0.5.dp)

        // --- DANGER ZONE ---
        Text("Danger Zone", style = MaterialTheme.typography.titleMedium, color = Color.Red)
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