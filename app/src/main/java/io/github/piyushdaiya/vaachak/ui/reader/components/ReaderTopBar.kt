package io.github.piyushdaiya.vaachak.ui.reader.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ReaderTopBar(
    bookTitle: String,
    isEink: Boolean,
    onBack: () -> Unit,
    onTocClick: () -> Unit,
    onSearchClick: () -> Unit,
    onHighlightsClick: () -> Unit,
    onRecapClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val containerColor = if (isEink) Color.White else MaterialTheme.colorScheme.surface
    val contentColor = if (isEink) Color.Black else MaterialTheme.colorScheme.onSurface
    val dividerColor = if (isEink) Color.Black else MaterialTheme.colorScheme.outlineVariant

    Surface(
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Back Button
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 2. Title
                Text(
                    text = bookTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = if (isEink) FontWeight.Bold else FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )

                // 3. Actions Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ReaderActionIcon(Icons.AutoMirrored.Filled.List, "TOC", onTocClick)
                    ReaderActionIcon(Icons.Default.Search, "Search", onSearchClick)
                    ReaderActionIcon(Icons.Default.Edit, "Highlights", onHighlightsClick)

                    // CHANGED: Use 'History' icon for "The Story So Far"
                    ReaderActionIcon(Icons.Default.History, "Recap", onRecapClick)

                    ReaderActionIcon(Icons.Default.Settings, "Settings", onSettingsClick)
                }
            }
            HorizontalDivider(thickness = if (isEink) 1.dp else 0.5.dp, color = dividerColor)
        }
    }
}

@Composable
fun ReaderActionIcon(icon: ImageVector, desc: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = desc,
            modifier = Modifier.size(20.dp)
        )
    }
}