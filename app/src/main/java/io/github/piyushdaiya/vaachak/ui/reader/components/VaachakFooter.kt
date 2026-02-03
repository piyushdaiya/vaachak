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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- 1. READER FOOTER (Unchanged) ---
@Composable
fun ReaderFooter(
    pageInfo: String,
    isEink: Boolean = false
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(26.dp),
        color = if (isEink) Color.White else MaterialTheme.colorScheme.surface,
        border = if (isEink) BorderStroke(1.dp, Color.Black) else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = pageInfo,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontWeight = if (isEink) FontWeight.Bold else FontWeight.Normal,
                color = if (isEink) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// --- 2. NAVIGATION FOOTER (Horizontal Row Layout) ---
@Composable
fun VaachakNavigationFooter(
    onBookshelfClick: () -> Unit,
    onHighlightsClick: () -> Unit,
    onAboutClick: () -> Unit,
    isEink: Boolean = false
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(), // Reduced from 56dp to 40dp
        color = if (isEink) Color.White else MaterialTheme.colorScheme.surface,
        border = if (isEink) BorderStroke(1.dp, Color.Black) else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth() // FIX: Changed from fillMaxSize()
                    .height(56.dp)  // FIX: Standard Bottom Nav Height
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FooterNavItem(
                    label = "Bookshelf",
                    icon = Icons.AutoMirrored.Filled.LibraryBooks,
                    onClick = onBookshelfClick,
                    isEink = isEink
                )

                if (isEink) VerticalDivider()

                FooterNavItem(
                    label = "Highlights",
                    icon = Icons.Default.CollectionsBookmark,
                    onClick = onHighlightsClick,
                    isEink = isEink
                )

                if (isEink) VerticalDivider()

                FooterNavItem(
                    label = "About",
                    icon = Icons.Default.Info,
                    onClick = onAboutClick,
                    isEink = isEink
                )
            }
            // This spacer pushes the row up above the Android navigation bar
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
fun FooterNavItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isEink: Boolean
) {
    val contentColor = if (isEink) Color.Black else MaterialTheme.colorScheme.onSurface

    // CHANGED: Column -> Row for horizontal layout
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp) // Slightly smaller icon
        )
        Spacer(modifier = Modifier.width(6.dp)) // Space between Icon and Text
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isEink) FontWeight.Bold else FontWeight.Medium,
            color = contentColor,
            fontSize = 12.sp // Slightly larger text for readability
        )
    }
}

@Composable
fun VerticalDivider() {
    Box(
        modifier = Modifier
            .fillMaxHeight(0.6f)
            .width(1.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {}
    }
}