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

package io.github.piyushdaiya.vaachak.ui.bookshelf

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import io.github.piyushdaiya.vaachak.data.local.BookEntity
import io.github.piyushdaiya.vaachak.ui.reader.components.VaachakHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookshelfScreen(
    onBookClick: (String) -> Unit,
    onRecallClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: BookshelfViewModel = hiltViewModel()
) {
    val allBooks by viewModel.allBooks.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val libraryBooks by viewModel.filteredLibraryBooks.collectAsState()
    val continueReadingBooks by viewModel.recentBooks.collectAsState()
    val isEink by viewModel.isEinkEnabled.collectAsState()

    // NEW: Observe Offline Mode
    val isOfflineMode by viewModel.isOfflineModeEnabled.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }
    val recapState by viewModel.recapState.collectAsState()
    val loadingUri by viewModel.isLoadingRecap.collectAsState()

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { message -> snackbarHostState.showSnackbar(message); viewModel.clearSnackbarMessage() }
    }

    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri: Uri? -> uri?.let { viewModel.importBook(it) } }
    val containerColor = if (isEink) Color.White else MaterialTheme.colorScheme.background

    Scaffold(
        topBar = {
            VaachakHeader(
                title = "My Bookshelf",
                onBack = {},
                showBackButton = false,
                isEink = isEink,
                actions = {
                    // CONDITION: Hide Recall if Offline
                    if (!isOfflineMode) {
                        IconButton(onClick = onRecallClick) { Icon(Icons.Default.AutoAwesome, "Recall", Modifier.size(22.dp)) }
                    }
                    IconButton(onClick = onSettingsClick) { Icon(Icons.Default.Settings, "Settings", Modifier.size(22.dp)) }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { launcher.launch(arrayOf("application/epub+zip")) },
                containerColor = if(isEink) Color.Black else MaterialTheme.colorScheme.primaryContainer,
                contentColor = if(isEink) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(0.dp)
            ) { Icon(Icons.Default.Add, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Add Book", fontWeight = FontWeight.Bold) }
        },
        containerColor = containerColor
    ) { padding ->
        if (allBooks.isEmpty()) {
            EmptyShelfPlaceholder(padding, isEink)
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 88.dp)) {
                if (continueReadingBooks.isNotEmpty() && searchQuery.isEmpty()) {
                    item {
                        BookshelfSectionLabel("Continue Reading", isEink)
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(continueReadingBooks, key = { it.id }) { book ->
                                BookCard(
                                    book = book, isCompact = true, isEink = isEink,
                                    showRecap = !isOfflineMode, // CONDITION
                                    isLoadingRecap = loadingUri == book.uriString,
                                    onClick = { onBookClick(book.uriString) },
                                    onDelete = { viewModel.deleteBook(book.id) },
                                    onRecapClick = { viewModel.getQuickRecap(book) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = if(isEink) 1.dp else 0.5.dp, color = if(isEink) Color.Black else MaterialTheme.colorScheme.outlineVariant)
                    }
                }

                item {
                    LibraryControls(
                        searchQuery = searchQuery, sortOrder = sortOrder, showSortMenu = showSortMenu, isEink = isEink,
                        onSearchChange = { viewModel.updateSearchQuery(it) },
                        onSortClick = { showSortMenu = true },
                        onSortDismiss = { showSortMenu = false },
                        onSortSelect = { viewModel.updateSortOrder(it); showSortMenu = false }
                    )
                }

                if (libraryBooks.isEmpty() && searchQuery.isNotEmpty()) {
                    item { SearchEmptyState(searchQuery, isEink) { viewModel.updateSearchQuery("") } }
                } else {
                    items(libraryBooks.chunked(3)) { rowBooks ->
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (book in rowBooks) {
                                Box(modifier = Modifier.weight(1f)) {
                                    BookCard(
                                        book = book, isCompact = true, isEink = isEink,
                                        showRecap = false, // Never show summary on main library list (design choice)
                                        isLoadingRecap = loadingUri == book.uriString,
                                        onClick = { onBookClick(book.uriString) },
                                        onDelete = { viewModel.deleteBook(book.id) },
                                        onRecapClick = { viewModel.getQuickRecap(book) }
                                    )
                                }
                            }
                            if (rowBooks.size < 3) repeat(3 - rowBooks.size) { Spacer(modifier = Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }

        // Recap Dialog
        continueReadingBooks.forEach { book ->
            recapState[book.uriString]?.let { recap ->
                AlertDialog(
                    onDismissRequest = { viewModel.clearRecap(book.uriString) },
                    title = { Text("Quick Recap: ${book.title}") },
                    text = { Column(modifier = Modifier.verticalScroll(rememberScrollState())) { Text(recap, style = MaterialTheme.typography.bodyMedium) } },
                    confirmButton = { Button(onClick = { viewModel.clearRecap(book.uriString); onBookClick(book.uriString) }) { Text("Resume Reading") } },
                    dismissButton = { TextButton(onClick = { viewModel.clearRecap(book.uriString) }) { Text("Close") } }
                )
            }
        }
    }
}



// --- COMPONENTS ---

@Composable
fun BookshelfSectionLabel(text: String, isEink: Boolean) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = if(isEink) Color.Black else MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 12.dp)
    )
}

@Composable
fun LibraryControls(
    searchQuery: String,
    sortOrder: SortOrder,
    showSortMenu: Boolean,
    isEink: Boolean,
    onSearchChange: (String) -> Unit,
    onSortClick: () -> Unit,
    onSortDismiss: () -> Unit,
    onSortSelect: (SortOrder) -> Unit
) {
    val contentColor = if (isEink) Color.Black else MaterialTheme.colorScheme.onBackground

    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Library",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )

            Box {
                IconButton(onClick = onSortClick) {
                    Icon(Icons.AutoMirrored.Filled.Sort, "Sort", tint = contentColor)
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = onSortDismiss,
                    modifier = Modifier.background(if(isEink) Color.White else MaterialTheme.colorScheme.surface)
                ) {
                    // FIX: Using SortOption helper
                    SortOption(SortOrder.TITLE, "Title", sortOrder, onSortSelect)
                    SortOption(SortOrder.AUTHOR, "Author", sortOrder, onSortSelect)
                    SortOption(SortOrder.DATE_ADDED, "Recent", sortOrder, onSortSelect)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { input ->
                if (input.length <= 50) {
                    val sanitized = input.filter { !it.isISOControl() }
                    onSearchChange(sanitized)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search title...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = contentColor) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Close, null, tint = contentColor)
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Search),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = contentColor,
                unfocusedBorderColor = if(isEink) Color.Black else Color.Gray,
                cursorColor = contentColor
            ),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

// FIX: Helper to show checkmarks
@Composable
private fun SortOption(
    targetOrder: SortOrder,
    label: String,
    currentOrder: SortOrder,
    onSelect: (SortOrder) -> Unit
) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = { onSelect(targetOrder) },
        trailingIcon = {
            if (currentOrder == targetOrder) {
                Icon(Icons.Default.Check, contentDescription = "Selected")
            }
        }
    )
}

@Composable
fun EmptyShelfPlaceholder(padding: PaddingValues, isEink: Boolean) {
    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                // FIX: Used AutoMirrored correctly
                Icons.AutoMirrored.Filled.MenuBook,
                null, modifier = Modifier.size(48.dp),
                tint = if(isEink) Color.LightGray else MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Your bookshelf is empty.\nTap + to add a book.",
                color = if(isEink) Color.Black else Color.Gray,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun SearchEmptyState(query: String, isEink: Boolean, onClear: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.SearchOff, null, Modifier.size(48.dp), Color.LightGray)
        Spacer(Modifier.height(16.dp))
        Text("No books found for \"$query\"", color = if(isEink) Color.Black else Color.Gray)
        TextButton(onClick = onClear) { Text("Clear search") }
    }
}

@Composable
fun BookCard(
    book: BookEntity,
    isCompact: Boolean = false,
    isLoadingRecap: Boolean = false,
    showRecap: Boolean = false,
    isEink: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRecapClick: () -> Unit
) {
    val cardBg = if (isEink) Color.White else MaterialTheme.colorScheme.surface
    val border = if (isEink) BorderStroke(1.dp, Color.Black) else null
    val textColor = if (isEink) Color.Black else MaterialTheme.colorScheme.onSurface

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = border,
        elevation = CardDefaults.cardElevation(if(isEink) 0.dp else 2.dp),
        modifier = Modifier.width(if (isCompact) 110.dp else 140.dp).aspectRatio(0.65f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f).background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    if (book.coverPath != null) {
                        AsyncImage(
                            model = book.coverPath,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = book.title.take(1).uppercase(),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = textColor
                    )
                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.labelSmall,
                        color = if(isEink) Color.DarkGray else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (book.progress > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${(book.progress * 100).toInt()}% read",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if(isEink) Color.Black else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        LinearProgressIndicator(
                            progress = { book.progress.toFloat() },
                            modifier = Modifier.fillMaxWidth().height(3.dp),
                            color = if(isEink) Color.Black else MaterialTheme.colorScheme.primary,
                            trackColor = Color.LightGray,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (showRecap) {
                    SmallIconButton(
                        icon = Icons.Default.History,
                        onClick = onRecapClick,
                        isLoading = isLoadingRecap,
                        isEink = isEink,
                        isDestructive = false // Safe
                    )
                }
                SmallIconButton(
                    icon = Icons.Default.Delete,
                    onClick = onDelete,
                    isEink = isEink,
                    isDestructive = true // Destructive
                )
            }
        }
    }
}

@Composable
fun SmallIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    isEink: Boolean,
    isDestructive: Boolean
) {
    // FIX: Optimized logic to remove duplication and use isDestructive properly
    val bgColor = when {
        isEink && isDestructive -> Color.Black
        isEink -> Color.White
        isDestructive -> MaterialTheme.colorScheme.errorContainer
        else -> Color.Black.copy(alpha=0.6f)
    }

    val iconColor = when {
        isEink && isDestructive -> Color.White
        isEink -> Color.Black
        isDestructive -> MaterialTheme.colorScheme.onErrorContainer
        else -> Color.White
    }

    val border = if(isEink) BorderStroke(1.dp, Color.Black) else null

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = bgColor,
        border = border,
        modifier = Modifier.size(24.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = iconColor)
            } else {
                Icon(icon, null, modifier = Modifier.size(14.dp), tint = iconColor)
            }
        }
    }
}