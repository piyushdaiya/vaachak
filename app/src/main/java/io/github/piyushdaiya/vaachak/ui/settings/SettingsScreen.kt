package io.github.piyushdaiya.vaachak.ui.settings

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
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val geminiKey by viewModel.geminiKey.collectAsState()
    val cfUrl by viewModel.cfUrl.collectAsState()
    val cfToken by viewModel.cfToken.collectAsState()

    var showSavedMessage by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()) // THE FIX: Allow scrolling
    ) {
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
                viewModel.saveSettings()
                showSavedMessage = true
            }) { Text("Save Settings") }
        }

        if (showSavedMessage) {
            Text(
                "Settings Saved Successfully!",
                color = Color.Green,
                modifier = Modifier.padding(top = 16.dp, bottom = 32.dp) // Added bottom padding
            )
        }
    }
}

