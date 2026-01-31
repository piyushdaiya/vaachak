package io.github.piyushdaiya.vaachak.ui.reader.components

import androidx.compose.foundation.background // <--- FIXED: Added missing import
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.readium.r2.shared.publication.Link

@Composable
fun TableOfContents(
    toc: List<Link>,
    currentHref: String?,
    onLinkSelected: (Link) -> Unit,
    onDismiss: () -> Unit,
    isEink: Boolean
) {
    val flattenedToc = remember(toc) {
        flattenToc(toc)
    }

    val containerColor = if (isEink) Color.White else MaterialTheme.colorScheme.background
    val contentColor = if (isEink) Color.Black else MaterialTheme.colorScheme.onBackground

    Scaffold(
        topBar = {
            VaachakHeader(
                title = "Table of Contents",
                showBackButton = false,
                isEink = isEink,
                onBack = {},
                actions = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = contentColor)
                    }
                }
            )
        },
        containerColor = containerColor
    ) { padding ->
        if (flattenedToc.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No table of contents found.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize()
            ) {
                items(flattenedToc) { (depth, link) ->
                    // 1. VISUAL CHECK ONLY: Strip fragments for highlighting
                    val isActive = remember(currentHref, link.href) {
                        isCurrentChapter(currentHref, link.href.toString())
                    }

                    TocItem(
                        link = link,
                        depth = depth,
                        isActive = isActive,
                        isEink = isEink,
                        // 2. NAVIGATION: Pass original link with fragments intact
                        onClick = { onLinkSelected(link) }
                    )
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = if (isEink) Color.LightGray else MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.5f)
                    )
                }
            }
        }
    }
}

// --- HELPER: URL Normalization Logic ---
private fun isCurrentChapter(currentHref: String?, linkHref: String): Boolean {
    if (currentHref == null) return false

    // Strip fragments (e.g., "chapter1.html#p1" -> "chapter1.html") just for comparison
    val normalizedCurrent = currentHref.substringBefore('#')
    val normalizedLink = linkHref.substringBefore('#')

    return normalizedCurrent == normalizedLink
}

@Composable
fun TocItem(
    link: Link,
    depth: Int,
    isActive: Boolean,
    isEink: Boolean,
    onClick: () -> Unit
) {
    val textColor = if (isActive) {
        if (isEink) Color.Black else MaterialTheme.colorScheme.primary
    } else {
        if (isEink) Color.DarkGray else MaterialTheme.colorScheme.onSurface
    }

    val fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal

    // Visual Indicator for Active Item
    val backgroundColor = if (isActive) {
        if (isEink) Color.LightGray.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
    } else Color.Transparent

    val paddingStart = 16.dp + (24.dp * depth)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor) // Highlight background
            .clickable(onClick = onClick)
            .padding(start = paddingStart, end = 16.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = link.title ?: "Untitled Section",
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            fontWeight = fontWeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun flattenToc(links: List<Link>, depth: Int = 0): List<Pair<Int, Link>> {
    val result = mutableListOf<Pair<Int, Link>>()
    for (link in links) {
        result.add(depth to link)
        if (link.children.isNotEmpty()) {
            result.addAll(flattenToc(link.children, depth + 1))
        }
    }
    return result
}