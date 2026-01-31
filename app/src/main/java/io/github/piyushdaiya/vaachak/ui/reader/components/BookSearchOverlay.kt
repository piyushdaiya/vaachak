package io.github.piyushdaiya.vaachak.ui.reader.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import org.readium.r2.shared.publication.Locator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSearchOverlay(
    query: String,
    results: List<Locator>,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onResultClick: (Locator) -> Unit,
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
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }

                        TextField(
                            value = query,
                            onValueChange = onQueryChange,
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Search in book...") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = contentColor,
                                unfocusedTextColor = contentColor
                            ),
                            trailingIcon = {
                                if (query.isNotEmpty()) {
                                    IconButton(onClick = { onQueryChange("") }) {
                                        Icon(Icons.Default.Close, "Clear")
                                    }
                                }
                            }
                        )

                        IconButton(onClick = { onSearch(query) }) {
                            Icon(Icons.Default.Search, "Search")
                        }
                    }
                    HorizontalDivider()
                }
            }
        },
        containerColor = containerColor
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isSearching) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = contentColor
                )
            } else if (results.isEmpty() && query.isNotEmpty()) {
                Text(
                    text = "No results found.",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Gray
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(results) { locator ->
                        SearchResultItem(locator, query, isEink, onResultClick)
                        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultItem(
    locator: Locator,
    query: String,
    isEink: Boolean,
    onClick: (Locator) -> Unit
) {
    // Colors: Distinct highlighting based on mode
    val highlightBg = if(isEink) Color.LightGray else Color(0xFFFFEB3B) // Yellow for standard
    val textColor = if (isEink) Color.Black else MaterialTheme.colorScheme.onSurface

    val annotatedText = remember(locator, query) {
        val textObj = locator.text

        // 1. Combine all text parts (Readium sometimes splits them, sometimes doesn't)
        val rawText = (
                (textObj.before ?: "") +
                        (textObj.highlight ?: "") +
                        (textObj.after ?: "")
                ).replace("\n", " ").trim()

        buildAnnotatedString {
            if (rawText.isEmpty()) {
                append(locator.title ?: "Chapter Match")
            } else {
                // 2. Find the query (Case Insensitive)
                val startIndex = rawText.indexOf(query, ignoreCase = true)

                if (startIndex >= 0) {
                    // Match Found!

                    // A. Context Before (up to 20 chars)
                    val startContext = (startIndex - 25).coerceAtLeast(0)
                    if (startContext > 0) append("...")
                    append(rawText.substring(startContext, startIndex))

                    // B. The Highlighted Match
                    withStyle(SpanStyle(background = highlightBg, fontWeight = FontWeight.Bold, color = Color.Black)) {
                        val matchEnd = (startIndex + query.length).coerceAtMost(rawText.length)
                        append(rawText.substring(startIndex, matchEnd))
                    }

                    // C. Context After (up to 50 chars)
                    val endContext = (startIndex + query.length + 50).coerceAtMost(rawText.length)
                    append(rawText.substring(startIndex + query.length, endContext))
                    if (endContext < rawText.length) append("...")

                } else {
                    // Fallback: Query not found in snippet (rare edge case), show raw text
                    append(rawText.take(100))
                    if (rawText.length > 100) append("...")
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(locator) }
            .padding(16.dp)
    ) {
        Text(
            text = annotatedText,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Show Chapter Title as metadata
        Text(
            text = locator.title ?: "Section",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
    }
}