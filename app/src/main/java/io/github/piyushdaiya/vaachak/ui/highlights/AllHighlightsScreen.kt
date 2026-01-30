package io.github.piyushdaiya.vaachak.ui.highlights

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MenuBook
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
import io.github.piyushdaiya.vaachak.ui.reader.components.VaachakHeader

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun AllHighlightsScreen(
    onBack: () -> Unit,
    onHighlightClick: (String, String) -> Unit,
    viewModel: AllHighlightsViewModel = hiltViewModel()
) {
    val groupedHighlights by viewModel.groupedHighlights.collectAsState()
    val tags by viewModel.availableTags.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    val isEink by viewModel.isEinkEnabled.collectAsState()
    var isFilterExpanded by remember { mutableStateOf(false) }

    // Colors
    val containerColor = if (isEink) Color.White else MaterialTheme.colorScheme.background
    val contentColor = if (isEink) Color.Black else MaterialTheme.colorScheme.onBackground

    Scaffold(
        topBar = {
            VaachakHeader(
                title = "Highlights (${groupedHighlights.values.sumOf { it.size }})",
                onBack = onBack,
                isEink = isEink
            )
        },
        containerColor = containerColor
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // --- 1. COMPACT FILTER BAR ---
            Surface(
                color = if (isEink) Color.White else MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isFilterExpanded = !isFilterExpanded }
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp), // Reduced Padding
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FilterList, null, modifier = Modifier.size(14.dp), tint = contentColor)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Filter: $selectedTag",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = contentColor
                            )
                        }
                        Icon(
                            imageVector = if (isFilterExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle",
                            tint = contentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    AnimatedVisibility(
                        visible = isFilterExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        FlowRow(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            tags.forEach { tagData ->
                                FilterChip(
                                    selected = selectedTag == tagData.name,
                                    onClick = {
                                        viewModel.updateFilter(tagData.name)
                                        isFilterExpanded = false
                                    },
                                    label = { Text("${tagData.name} (${tagData.count})", fontSize = 11.sp) },
                                    modifier = Modifier.height(28.dp), // Slimmer chips
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (isEink) Color.Black else MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = if (isEink) Color.White else MaterialTheme.colorScheme.onPrimary,
                                        containerColor = Color.Transparent,
                                        labelColor = contentColor
                                    ),
                                    border = if (isEink) BorderStroke(1.dp, Color.Black) else FilterChipDefaults.filterChipBorder(enabled = true, selected = selectedTag == tagData.name)
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = if (isEink) Color.Black else MaterialTheme.colorScheme.outlineVariant)
                }
            }

            // --- 2. LIST AREA ---
            if (groupedHighlights.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No highlights found.", color = if(isEink) Color.Black else Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    groupedHighlights.forEach { (bookTitle, highlights) ->
                        stickyHeader {
                            Surface(
                                color = if (isEink) Color.White else MaterialTheme.colorScheme.surfaceVariant,
                                border = if(isEink) BorderStroke(1.dp, Color.Black) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.MenuBook, null, modifier = Modifier.size(14.dp), tint = contentColor)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = bookTitle,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = contentColor
                                    )
                                }
                            }
                        }
                        items(highlights, key = { it.id }) { highlight ->
                            CompactHighlightItem(
                                highlight = highlight,
                                isEink = isEink,
                                onClick = { onHighlightClick(highlight.publicationId, highlight.locatorJson) },
                                onDelete = { viewModel.deleteHighlight(highlight.id) }
                            )
                            if (!isEink) HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.2f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompactHighlightItem(
    highlight: io.github.piyushdaiya.vaachak.data.local.HighlightEntity,
    isEink: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val modifier = if (isEink) Modifier.fillMaxWidth().border(0.5.dp, Color.Black) else Modifier.fillMaxWidth()

    Surface(onClick = onClick, color = Color.Transparent, modifier = modifier) {
        Row(
            // OPTIMIZATION: Tighter padding (12dp) allows more content
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (highlight.tag != "General") {
                    Text(
                        text = "#${highlight.tag.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp, // Smaller tag text
                        color = if (isEink) Color.Black else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = highlight.text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp, // Slightly more compact font
                    lineHeight = 20.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isEink) Color.Black else MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    modifier = Modifier.size(16.dp), // Smaller Icon
                    tint = if (isEink) Color.Black else MaterialTheme.colorScheme.error.copy(alpha=0.6f)
                )
            }
        }
    }
}