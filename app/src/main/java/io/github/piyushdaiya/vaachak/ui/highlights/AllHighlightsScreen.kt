package io.github.piyushdaiya.vaachak.ui.highlights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.piyushdaiya.vaachak.data.local.HighlightEntity
import androidx.compose.material.icons.filled.History


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllHighlightsScreen(
    onBack: () -> Unit,
    onHighlightClick: (String, String) -> Unit,
    viewModel: AllHighlightsViewModel = hiltViewModel()
) {
    val groupedHighlights by viewModel.groupedHighlights.collectAsState()
    val tags by viewModel.availableTags.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()

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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.White)
        ) {
            // --- TAG FILTER ROW ---
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tags) { tagData ->
                    FilterChip(
                        selected = selectedTag == tagData.name,
                        onClick = { viewModel.updateFilter(tagData.name) },
                        label = { Text("${tagData.name} (${tagData.count})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color.Black,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = Color.Black
                        )
                    )
                }
            }

            if (groupedHighlights.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedTag == "All") "No highlights yet." else "No highlights found for \"$selectedTag\"",
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    groupedHighlights.forEach { (bookTitle, highlights) ->
                        item {
                            Surface(
                                color = Color(0xFFF5F5F5),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = bookTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(16.dp, 8.dp),
                                    color = Color.Black
                                )
                            }
                        }
                        items(highlights, key = { it.id }) { highlight ->
                            HighlightItem(
                                highlight = highlight,
                                onClick = {
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
}

@Composable
fun HighlightItem(
    highlight: HighlightEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // TAG BADGE (SuggestionChip for a neat e-ink friendly tag)
                SuggestionChip(
                    onClick = { }, // Non-clickable badge
                    label = { Text(highlight.tag, fontSize = 10.sp) },
                    modifier = Modifier
                        .height(24.dp)
                        .padding(bottom = 4.dp)
                )

                Text(
                    text = highlight.text,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.DarkGray
                )
                Text(
                    text = "Tap to jump to page",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.Red
                )
            }
        }
    }
}