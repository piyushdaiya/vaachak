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

package io.github.piyushdaiya.vaachak.ui.reader.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
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
    showRecap: Boolean,
    isBookmarked: Boolean, // NEW: State of current page
    onBack: () -> Unit,
    onTocClick: () -> Unit,
    onSearchClick: () -> Unit,
    onHighlightsClick: () -> Unit,
    onBookmarksListClick: () -> Unit, // NEW: Open List
    onBookmarkToggleClick: () -> Unit, // NEW: Toggle current page
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
                    // Current Page Bookmark Toggle
                    IconButton(onClick = onBookmarkToggleClick, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Toggle Bookmark",
                            modifier = Modifier.size(20.dp),
                            tint = if(isBookmarked && !isEink) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }

                    ReaderActionIcon(Icons.AutoMirrored.Filled.List, "TOC", onTocClick)

                    // Bookmarks List Icon
                    ReaderActionIcon(Icons.Default.Bookmarks, "Bookmarks List", onBookmarksListClick)

                    ReaderActionIcon(Icons.Default.Search, "Search", onSearchClick)
                    ReaderActionIcon(Icons.Default.Edit, "Highlights", onHighlightsClick)

                    if (showRecap) {
                        ReaderActionIcon(Icons.Default.History, "Recap", onRecapClick)
                    }

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