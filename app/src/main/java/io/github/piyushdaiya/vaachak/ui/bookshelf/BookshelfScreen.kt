package io.github.piyushdaiya.vaachak.ui.bookshelf

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import io.github.piyushdaiya.vaachak.data.local.BookEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookshelfScreen(
    onBookClick: (String) -> Unit,
    onRecallClick: () -> Unit,
    viewModel: BookshelfViewModel = hiltViewModel()
) {
    val allBooks by viewModel.allBooks.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val libraryBooks by viewModel.filteredLibraryBooks.collectAsState()
    val continueReadingBooks by viewModel.recentBooks.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val sortOrder by viewModel.sortOrder.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }

    val recapState by viewModel.recapState.collectAsState()
    val loadingUri by viewModel.isLoadingRecap.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importBook(it) }
    }

    Scaffold(
        topBar = {
            // Standardized v1.7 Header
            BookshelfHeader(
                onRecallClick = onRecallClick,
                onSettingsClick = { /* Handle global settings if needed */ }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { launcher.launch(arrayOf("application/epub+zip")) },
                containerColor = Color.Black,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add Book")
            }
        }
    ) { padding ->
        if (allBooks.isEmpty()) {
            EmptyShelfPlaceholder(padding)
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize().background(Color.White),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                // SECTION: CONTINUE READING
                if (continueReadingBooks.isNotEmpty() && searchQuery.isEmpty()) {
                    item {
                        BookshelfSectionLabel("Continue Reading")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(continueReadingBooks, key = { it.id }) { book ->
                                BookCard(
                                    book = book,
                                    isCompact = true,
                                    showRecap = true,
                                    isLoadingRecap = loadingUri == book.uriString,
                                    onClick = { onBookClick(book.uriString) },
                                    onDelete = { viewModel.deleteBook(book.id) },
                                    onRecapClick = { viewModel.getQuickRecap(book) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = Color.LightGray.copy(alpha = 0.5f)
                        )
                    }
                }

                // SECTION: LIBRARY SEARCH & SORT
                item {
                    LibraryControls(
                        searchQuery = searchQuery,
                        sortOrder = sortOrder,
                        showSortMenu = showSortMenu,
                        onSearchChange = { viewModel.updateSearchQuery(it) },
                        onSortClick = { showSortMenu = true },
                        onSortDismiss = { showSortMenu = false },
                        onSortSelect = {
                            viewModel.updateSortOrder(it)
                            showSortMenu = false
                        }
                    )
                }

                // SECTION: LIBRARY GRID
                if (libraryBooks.isEmpty() && searchQuery.isNotEmpty()) {
                    item { SearchEmptyState(searchQuery) { viewModel.updateSearchQuery("") } }
                } else {
                    val bookRows = libraryBooks.chunked(3)
                    items(bookRows) { rowBooks ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (book in rowBooks) {
                                Box(modifier = Modifier.weight(1f)) {
                                    BookCard(
                                        book = book,
                                        isCompact = true,
                                        showRecap = false,
                                        isLoadingRecap = loadingUri == book.uriString,
                                        onClick = { onBookClick(book.uriString) },
                                        onDelete = { viewModel.deleteBook(book.id) },
                                        onRecapClick = { viewModel.getQuickRecap(book) }
                                    )
                                }
                            }
                            if (rowBooks.size < 3) {
                                repeat(3 - rowBooks.size) { Spacer(modifier = Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }

        // Recap Dialogs
        continueReadingBooks.forEach { book ->
            recapState[book.uriString]?.let { recap ->
                AlertDialog(
                    onDismissRequest = { viewModel.clearRecap(book.uriString) },
                    title = { Text("Quick Recap: ${book.title}") },
                    text = {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            Text(recap, style = MaterialTheme.typography.bodyMedium)
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            viewModel.clearRecap(book.uriString)
                            onBookClick(book.uriString)
                        }) { Text("Resume Reading") }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.clearRecap(book.uriString) }) { Text("Close") }
                    }
                )
            }
        }
    }
}

// --- STANDARDIZED COMPONENTS (v1.7) ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookshelfHeader(
    onRecallClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                "My Bookshelf",
                style = MaterialTheme.typography.titleMedium, // Standardized Size
                fontWeight = FontWeight.Bold
            )
        },
        actions = {
            IconButton(onClick = onRecallClick) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "Recall", tint = Color.Black)
            }
                  },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
    )
}

@Composable
fun BookshelfSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge, // Standardized Size
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
fun LibraryControls(
    searchQuery: String,
    sortOrder: SortOrder,
    showSortMenu: Boolean,
    onSearchChange: (String) -> Unit,
    onSortClick: () -> Unit,
    onSortDismiss: () -> Unit,
    onSortSelect: (SortOrder) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Library",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Box {
                IconButton(onClick = onSortClick) {
                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                }
                DropdownMenu(expanded = showSortMenu, onDismissRequest = onSortDismiss) {
                    DropdownMenuItem(
                        text = { Text("Title") },
                        onClick = { onSortSelect(SortOrder.TITLE) },
                        leadingIcon = { if(sortOrder == SortOrder.TITLE) Icon(Icons.Default.Check, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Author") },
                        onClick = { onSortSelect(SortOrder.AUTHOR) },
                        leadingIcon = { if(sortOrder == SortOrder.AUTHOR) Icon(Icons.Default.Check, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Recent Added") },
                        onClick = { onSortSelect(SortOrder.DATE_ADDED) },
                        leadingIcon = { if(sortOrder == SortOrder.DATE_ADDED) Icon(Icons.Default.Check, null) }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search by title...") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Close, null)
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )
    }
}

@Composable
fun EmptyShelfPlaceholder(padding: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Your bookshelf is empty.\nTap + to add a book.",
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SearchEmptyState(query: String, onClear: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Search, null, Modifier.size(64.dp), Color.LightGray)
        Spacer(Modifier.height(16.dp))
        Text("No books found for \"$query\"", color = Color.Gray)
        TextButton(onClick = onClear) { Text("Clear search") }
    }
}

// BookCard remains largely the same but uses theme-consistent text sizes
@Composable
fun BookCard(
    book: BookEntity,
    isCompact: Boolean = false,
    isLoadingRecap: Boolean = false,
    showRecap: Boolean = false, // NEW FLAG
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onRecapClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        modifier = Modifier
            .padding(4.dp)
            .width(if (isCompact) 110.dp else 150.dp)
    ) {
        Box {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isCompact) 140.dp else 200.dp)
                        .background(Color.LightGray),
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
                            style = MaterialTheme.typography.headlineMedium,
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
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        maxLines = 1
                    )
                    Text(
                        text = "${(book.progress * 100).toInt()}% read",
                        style = MaterialTheme.typography.labelSmall, // Clean E-ink style
                        color = Color.DarkGray
                    )
                }
            }
            // Overlays (Recap/Delete)
            Row(modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                // ONLY SHOW RECAP IF FLAG IS TRUE
                if (showRecap) {
                    IconButton(
                        onClick = onRecapClick,
                        modifier = Modifier.size(26.dp).background(Color.White.copy(0.8f), CircleShape)
                    ) {
                        if (isLoadingRecap) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = Color.Black)
                        } else {
                            Icon(Icons.Default.History, null, Modifier.size(16.dp))
                        }
                    }
                }
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(26.dp).background(Color.Black.copy(0.6f), CircleShape)
                ) {
                    Icon(Icons.Default.Delete, null, Modifier.size(14.dp), Color.White)
                }
            }
        }
    }
}