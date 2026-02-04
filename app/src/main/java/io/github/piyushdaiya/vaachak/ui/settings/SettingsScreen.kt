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

package io.github.piyushdaiya.vaachak.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.piyushdaiya.vaachak.ui.theme.ThemeMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    // --- STATE ---
    val geminiKey by viewModel.geminiKey.collectAsState()
    val cfUrl by viewModel.cfUrl.collectAsState()
    val cfToken by viewModel.cfToken.collectAsState()
    val isAutoSaveEnabled by viewModel.isAutoSaveRecapsEnabled.collectAsState()
    val currentTheme by viewModel.themeMode.collectAsState()
    val isEinkEnabled by viewModel.isEinkEnabled.collectAsState()
    val contrast by viewModel.einkContrast.collectAsState()
    val useEmbeddedDictionary by viewModel.useEmbeddedDictionary.collectAsState()
    val dictionaryFolder by viewModel.dictionaryFolder.collectAsState()

    // NEW: Offline Mode
    val isOfflineMode by viewModel.isOfflineModeEnabled.collectAsState()

    // --- UI STATE ---
    var showResetDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            viewModel.updateDictionaryFolder(it.toString())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // 1. HEADER
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Preferences", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Customize your reading engine", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                FilledTonalButton(
                    onClick = {
                        keyboardController?.hide()
                        scope.launch {
                            try { viewModel.saveSettings(); snackbarHostState.showSnackbar("✅ Settings saved successfully") }
                            catch (e: Exception) { snackbarHostState.showSnackbar("❌ Error: ${e.localizedMessage}") }
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = if (isEinkEnabled) ButtonDefaults.filledTonalButtonColors(containerColor = Color.Black, contentColor = Color.White) else ButtonDefaults.filledTonalButtonColors()
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save Settings", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save")
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // 2. CONNECTIVITY (Offline Mode)
            SettingsSection(title = "Connectivity", icon = Icons.Default.WifiOff) {
                SettingsToggleRow(
                    label = "Offline Mode",
                    description = "Hide AI features for distraction-free reading",
                    checked = isOfflineMode,
                    onCheckedChange = { viewModel.toggleOfflineMode(it) }
                )
            }

            // 3. DISPLAY
            SettingsSection(title = "Display", icon = Icons.Default.Face) {
                Text("Theme Selection", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = currentTheme == mode,
                            onClick = { viewModel.updateTheme(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                            label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                if (currentTheme == ThemeMode.E_INK) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("E-ink Sharpness", style = MaterialTheme.typography.labelMedium)
                    }
                    var sliderPosition by remember(contrast) { mutableFloatStateOf(contrast) }
                    Slider(
                        value = sliderPosition,
                        onValueChange = { sliderPosition = it; viewModel.updateContrast(it) },
                        steps = 3,
                        modifier = Modifier.padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(thumbColor = Color.Black, activeTrackColor = Color.Black)
                    )
                }
            }

            // 4. READING
            SettingsSection(title = "Reading", icon = Icons.Default.Info) {
                SettingsToggleRow(
                    label = "Use External Dictionary",
                    description = "Use external Dictionary with local StarDict files. Else use Embedded dictionary with limited words",
                    checked = useEmbeddedDictionary,
                    onCheckedChange = { viewModel.toggleEmbeddedDictionary(it) }
                )
                if (useEmbeddedDictionary) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(onClick = { launcher.launch(null) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (dictionaryFolder.isEmpty()) "Select Dictionary Folder" else "Change Folder")
                    }
                    if (dictionaryFolder.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f), RoundedCornerShape(4.dp)).padding(8.dp)) {
                            Text("📂 Selected Location:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text(dictionaryFolder, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // HIDE RECAP TOGGLE IF OFFLINE
                if (!isOfflineMode) {
                    SettingsToggleRow(
                        label = "Auto-Save Recaps",
                        description = "Save AI summaries to highlights",
                        checked = isAutoSaveEnabled,
                        onCheckedChange = { viewModel.toggleAutoSaveRecaps(it) }
                    )
                } else {
                    Text("Auto-Save Recaps disabled in Offline Mode", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }

            // 5. INTELLIGENCE (Hidden if Offline)
            AnimatedVisibility(visible = !isOfflineMode, enter = expandVertically(), exit = shrinkVertically()) {
                SettingsSection(title = "Intelligence", icon = Icons.Default.Share) {
                    SettingsTextField(value = geminiKey, onValueChange = { viewModel.updateGemini(it) }, label = "Gemini API Key", icon = Icons.Default.Lock, isSensitive = true)
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingsTextField(value = cfUrl, onValueChange = { viewModel.updateCfUrl(it) }, label = "Cloudflare URL", icon = Icons.Default.Info, isUrl = true)
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingsTextField(value = cfToken, onValueChange = { viewModel.updateCfToken(it) }, label = "Auth Token", icon = Icons.Default.Person, isSensitive = true)
                }
            }
            if (isOfflineMode) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f)), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WifiOff, null, tint = Color.Gray)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("AI features are currently hidden.", color = Color.Gray)
                    }
                }
            }

            // 6. DANGER ZONE
            SettingsSection(title = "Danger Zone", icon = Icons.Default.Warning, borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f), titleColor = MaterialTheme.colorScheme.error) {
                OutlinedButton(onClick = { showResetDialog = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error), border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))) {
                    Text("Reset All Settings")
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Factory Reset?") },
                text = { Text("This will erase all API keys and preferences.") },
                confirmButton = { TextButton(onClick = { viewModel.resetSettings(); showResetDialog = false; scope.launch { snackbarHostState.showSnackbar("Settings reset") } }) { Text("Reset", color = Color.Red) } },
                dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("Cancel") } }
            )
        }
    }
}

// Helper Composables
@Composable
fun SettingsSection(title: String, icon: ImageVector, borderColor: Color = MaterialTheme.colorScheme.outlineVariant, titleColor: Color = MaterialTheme.colorScheme.onSurface, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().border(1.dp, borderColor, RoundedCornerShape(12.dp)).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = titleColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = titleColor)
        }
        content()
    }
}

@Composable
fun SettingsToggleRow(label: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.scale(0.8f))
    }
}

@Composable
fun SettingsTextField(value: String, onValueChange: (String) -> Unit, label: String, icon: ImageVector, placeholder: String = "", isSensitive: Boolean = false, isUrl: Boolean = false) {
    var passwordVisible by remember { mutableStateOf(false) }
    val sanitize: (String) -> String = { input -> if (isSensitive || isUrl) input.filter { !it.isWhitespace() }.take(512) else input.take(512) }
    OutlinedTextField(
        value = value, onValueChange = { onValueChange(sanitize(it)) }, label = { Text(label) }, placeholder = { if (placeholder.isNotEmpty()) Text(placeholder) },
        leadingIcon = { Icon(icon, null, modifier = Modifier.size(18.dp)) },
        visualTransformation = if (isSensitive && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (isUrl) KeyboardType.Uri else if (isSensitive) KeyboardType.Password else KeyboardType.Text),
        trailingIcon = if (isSensitive) { { IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, contentDescription = null) } } } else null,
        modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(8.dp), textStyle = MaterialTheme.typography.bodyMedium
    )
}