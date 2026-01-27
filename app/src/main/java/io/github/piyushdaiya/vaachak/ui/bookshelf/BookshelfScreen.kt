package io.github.piyushdaiya.vaachak.ui.bookshelf

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items // CRITICAL: This fixes "Unresolved reference items"
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.OutlinedTextField
// Ensure you have the collectors
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.material3.HorizontalDivider // Ensure you're using M3 Divider
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Check

@Composable
fun BookshelfScreen(
    onBookClick: (String) -> Unit,
    viewModel: BookshelfViewModel = hiltViewModel()
) {
    val allBooks by viewModel.allBooks.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val libraryBooks by viewModel.filteredLibraryBooks.collectAsState()
    val continueReadingBooks by viewModel.recentBooks.collectAsState() // Plural based on refactor
    val snackbarHostState = remember { SnackbarHostState() }
    val message by viewModel.snackbarMessage
    val sortOrder by viewModel.sortOrder.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.snackbarMessage.value = null
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importBook(it) }
    }

    Scaffold(
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
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize().background(Color.White),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                // --- CONTINUE READING ---
                // We only show this if the user isn't currently searching
                if (continueReadingBooks.isNotEmpty() && searchQuery.isEmpty()) {
                    item {
                        Text(
                            text = "Continue Reading",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(16.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(continueReadingBooks, key = { it.id }) { book ->
                                BookCard(
                                    title = book.title,
                                    author = book.author,
                                    coverPath = book.coverPath,
                                    progress = book.progress,
                                    isCompact = true,
                                    onClick = { onBookClick(book.uriString) },
                                    onDelete = { viewModel.deleteBook(book.id) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = Color.LightGray.copy(alpha = 0.5f))
                    }
                }

                // --- SEARCH BAR & HEADER (Wrapped in item) ---
                item {
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
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(
                                        Icons.Default.Sort,
                                        contentDescription = "Sort"
                                    )
                                }
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Title") },
                                        onClick = { viewModel.updateSortOrder(SortOrder.TITLE); showSortMenu = false },
                                        leadingIcon = { if(sortOrder == SortOrder.TITLE) Icon(Icons.Default.Check, null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Author") },
                                        onClick = { viewModel.updateSortOrder(SortOrder.AUTHOR); showSortMenu = false },
                                        leadingIcon = { if(sortOrder == SortOrder.AUTHOR) Icon(Icons.Default.Check, null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Recent Added") },
                                        onClick = { viewModel.updateSortOrder(SortOrder.DATE_ADDED); showSortMenu = false },
                                        leadingIcon = { if(sortOrder == SortOrder.DATE_ADDED) Icon(Icons.Default.Check, null) }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        // FIX: Added the OutlinedTextField here
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search by title...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium
                        )
                    }
                }

                // --- SEARCH RESULTS / LIBRARY GRID ---
                if (libraryBooks.isEmpty() && searchQuery.isNotEmpty()) {
                    // 1. NO RESULTS FOUND STATE
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 64.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No books found for \"$searchQuery\"",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray
                            )
                            TextButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Text("Clear search")
                            }
                        }
                    }
                } else {
                    // 2. ACTUAL LIBRARY GRID (Existing logic)
                    val bookRows = libraryBooks.chunked(3)
                    items(bookRows) { rowBooks ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (book in rowBooks) {
                                Box(modifier = Modifier.weight(1f)) {
                                    BookCard(
                                        title = book.title,
                                        author = book.author,
                                        coverPath = book.coverPath,
                                        progress = book.progress,
                                        isCompact = true,
                                        onClick = { onBookClick(book.uriString) },
                                        onDelete = { viewModel.deleteBook(book.id) }
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


    }
}

@Composable
fun BookCard(
    title: String,
    author: String,
    coverPath: String?,
    progress: Double,
    isCompact: Boolean = false, // Toggle between Hero and Grid view
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        modifier = Modifier
            .padding(4.dp) // Reduced padding for better density
            .width(if (isCompact) 110.dp else 150.dp) // Limits horizontal/grid size
            .wrapContentHeight()
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isCompact) 140.dp else 200.dp) // Fixed heights to limit scrolling
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    if (coverPath != null) {
                        AsyncImage(
                            model = coverPath,
                            contentDescription = "Cover of $title",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = title.take(1).uppercase(),
                            fontSize = if (isCompact) 24.sp else 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Progress Bar Overlay at bottom of cover
                    LinearProgressIndicator(
                        progress = { progress.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isCompact) 3.dp else 6.dp)
                            .align(Alignment.BottomCenter),
                        color = Color(0xFF4CAF50),
                        trackColor = Color.White.copy(alpha = 0.5f)
                    )
                }

                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isCompact) 12.sp else 14.sp,
                        maxLines = if (isCompact) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.Black
                    )
                    Text(
                        text = author,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "${(progress * 100).toInt()}% completed",
                        fontSize = 10.sp,
                        color = Color.DarkGray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Minimalist Delete Button for E-ink/Compact efficiency
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(if (isCompact) 24.dp else 32.dp)
                    .background(Color.Black.copy(alpha = 0.4f), shape = MaterialTheme.shapes.small)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = Color.White,
                    modifier = Modifier.size(if (isCompact) 14.dp else 18.dp)
                )
            }
        }
    }
}