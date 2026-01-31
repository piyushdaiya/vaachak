package io.github.piyushdaiya.vaachak.ui.reader.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.piyushdaiya.vaachak.data.local.HighlightEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookHighlightsOverlay(
    highlights: List<HighlightEntity>,
    onHighlightClick: (HighlightEntity) -> Unit,
    onDismiss: () -> Unit,
    isEink: Boolean
) {
    val containerColor = if (isEink) Color.White else MaterialTheme.colorScheme.background
    val contentColor = if (isEink) Color.Black else MaterialTheme.colorScheme.onBackground

    Scaffold(
        topBar = {
            Surface(
                color = containerColor,
                contentColor = contentColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Text(
                            text = "Highlights & Notes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                    HorizontalDivider(thickness = 1.dp, color = if(isEink) Color.Black else MaterialTheme.colorScheme.outlineVariant)
                }
            }
        },
        containerColor = containerColor
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (highlights.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Bookmark, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("No highlights yet.", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(highlights) { highlight ->
                        HighlightItem(highlight, isEink, onHighlightClick)
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = if(isEink) Color.LightGray else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HighlightItem(
    highlight: HighlightEntity,
    isEink: Boolean,
    onClick: (HighlightEntity) -> Unit
) {
    val textColor = if (isEink) Color.Black else MaterialTheme.colorScheme.onSurface

    // Tag Color Logic
    val tagBgColor = if (isEink) Color.Black else MaterialTheme.colorScheme.secondaryContainer
    val tagTextColor = if (isEink) Color.White else MaterialTheme.colorScheme.onSecondaryContainer

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(highlight) }
            .padding(16.dp)
    ) {
        // 1. Tag Label (if exists)
        if (highlight.tag.isNotEmpty()) {
            Surface(
                color = tagBgColor,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = highlight.tag.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = tagTextColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // 2. Highlighted Text (Simulating visual highlight with left border or bg)
        Row(modifier = Modifier.fillMaxWidth()) {
            // Visual accent line
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp) // Fixed min height or dynamic? Dynamic is hard in simple Row.
                    // Just a small accent
                    .background(Color(highlight.color).copy(alpha = if(isEink) 0.5f else 1f))
            )
            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = highlight.text,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

