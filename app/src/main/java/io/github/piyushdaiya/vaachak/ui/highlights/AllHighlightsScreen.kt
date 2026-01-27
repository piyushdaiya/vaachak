package io.github.piyushdaiya.vaachak.ui.highlights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.piyushdaiya.vaachak.data.local.HighlightEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllHighlightsScreen(
    onBack: () -> Unit,
    onHighlightClick: (String, String) -> Unit,
    viewModel: AllHighlightsViewModel = hiltViewModel()
) {
    val groupedHighlights by viewModel.groupedHighlights.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Highlights") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        if (groupedHighlights.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No highlights yet.", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).background(Color.White)) {
                groupedHighlights.forEach { (bookTitle, highlights) ->
                    item {
                        Text(
                            text = bookTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp),
                            color = Color.Black
                        )
                    }
                    items(highlights, key = { it.id }) { highlight ->
                        HighlightItem(
                            highlight = highlight,
                            onClick = {
                                // Pass URI and Locator to MainActivity
                                onHighlightClick(highlight.publicationId, highlight.locatorJson)
                            },
                            onDelete = { viewModel.deleteHighlight(highlight.id) }
                        )
                        HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun HighlightItem(
    highlight: HighlightEntity,
    onClick: () -> Unit, // New callback
    onDelete: () -> Unit
) {
    // Surface provides the ripple and click handling
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = highlight.text,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.DarkGray
                )
                // Optional: show a timestamp or "Jump to page" hint
                Text(
                    text = "Tap to jump to page",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Highlight", tint = Color.Red)
            }
        }
    }
}

