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
import io.github.piyushdaiya.vaachak.ui.theme.ThemeMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.lerp
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val geminiKey by viewModel.geminiKey.collectAsState()
    val cfUrl by viewModel.cfUrl.collectAsState()
    val cfToken by viewModel.cfToken.collectAsState()
    val isAutoSaveEnabled by viewModel.isAutoSaveRecapsEnabled.collectAsState()
    var showSavedMessage by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    // NEW: Observe the persistent theme mode
    val currentTheme by viewModel.themeMode.collectAsState()
    val isEinkEnabled by viewModel.isEinkEnabled.collectAsState()
    val contrast by viewModel.einkContrast.collectAsState()
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

        // --- DISPLAY SETTINGS (THEME SELECTION) ---
        Text("Display Settings", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "App Theme",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Standardized Segmented Button Row for Theme Selection
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ThemeMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = currentTheme == mode,
                    onClick = { viewModel.updateTheme(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                    label = {
                        Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                )
            }
        }
        // NEW: E-ink Contrast Slider (Conditional)
        if (currentTheme == ThemeMode.E_INK) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Text(
                    text = "E-ink Contrast / Sharpness",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Using a standard icon that always exists
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )

                    Slider(
                        value = contrast,
                        onValueChange = { viewModel.updateContrast(it) },
                        // 3 steps + 2 ends = 5 positions (0, 0.25, 0.5, 0.75, 1.0)
                        steps = 3,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.Black,
                            activeTrackColor = Color.Black,
                            activeTickColor = Color.White.copy(alpha = 0.5f), // Marks on the black line
                            inactiveTickColor = Color.Black.copy(alpha = 0.3f) // Marks on the gray line
                        )
                    )

                    Text(
                        text = "${(contrast * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(32.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.LightGray.copy(alpha = 0.2f), MaterialTheme.shapes.small)
                .padding(8.dp)
        ) {
            Text(
                "Sharpness Preview",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().height(20.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // We create 5 boxes representing different gray intensities
                val grayShades = listOf(0.2f, 0.4f, 0.6f, 0.8f, 1.0f)

                grayShades.forEach { weight ->
                    // This mimics the 'lerp' logic in your Theme.kt
                    val baseColor = Color.Black.copy(alpha = weight)
                    val sharpenedColor = androidx.compose.ui.graphics.lerp(baseColor, Color.Black, contrast)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(sharpenedColor)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        ListItem(
            headlineContent = { Text("Auto-Save AI Recaps") },
            supportingContent = { Text("Automatically save generated summaries to your highlights.") },
            trailingContent = {
                Switch(
                    checked = isAutoSaveEnabled,
                    onCheckedChange = { viewModel.toggleAutoSaveRecaps(it) }
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
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
            modifier = Modifier.align(Alignment.End),
                    // High contrast for E-ink
            colors =if (isEinkEnabled) ButtonDefaults.buttonColors(containerColor = Color.Black) else ButtonDefaults.buttonColors()
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