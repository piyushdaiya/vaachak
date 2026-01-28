package io.github.piyushdaiya.vaachak.ui.session

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionHistoryScreen(
    onBack: () -> Unit,
    onLaunchBook: (String) -> Unit,
    viewModel: SessionHistoryViewModel = hiltViewModel()
) {
    val recallMap by viewModel.recallMap.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
// We need the original book list to get URIs for the launch button
    val books by viewModel.recentBooks.collectAsState()
    // Trigger the recall automatically when the screen opens
    LaunchedEffect(Unit) {
        viewModel.triggerGlobalRecall()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Session Recall", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().background(Color.White)) {
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Black,
                    trackColor = Color.LightGray.copy(alpha = 0.3f)
                )
            }

            if (recallMap.isEmpty() && !isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No session summaries found.", color = Color.Gray)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(recallMap.entries.toList()) { entry ->
                    val bookTitle = entry.key
                    val summary = entry.value
                    val bookUri = books.find { it.title == bookTitle }?.uriString
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                        border = BorderStroke(0.5.dp, Color.LightGray)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = bookTitle, // Book Title
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = summary, // AI Summary
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.DarkGray
                            )
                            if (bookUri != null) {
                                Button(
                                    onClick = { onLaunchBook(bookUri) },
                                    modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                                ) {
                                    Text("Resume Reading", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

