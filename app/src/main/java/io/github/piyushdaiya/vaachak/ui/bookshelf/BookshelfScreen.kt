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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import io.github.piyushdaiya.vaachak.data.local.BookEntity
import androidx.compose.material.icons.filled.AutoAwesome

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
    val message by viewModel.snackbarMessage
    val sortOrder by viewModel.sortOrder.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }

    // --- RECAP STATES ---
    val recapState by viewModel.recapState.collectAsState()
    val loadingUri by viewModel.isLoadingRecap.collectAsState()

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
        topBar = {
            TopAppBar(
                title = { Text("Vaachak", fontWeight = FontWeight.Bold) },
                actions = {
                    // NEW: GLOBAL RECALL BUTTON
                    IconButton(onClick = onRecallClick) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Session Recall",
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
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
                                    book = book,
                                    isCompact = true, // Hero style for recent books
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
                                    Icon(Icons.Default.Sort, contentDescription = "Sort")
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

                if (libraryBooks.isEmpty() && searchQuery.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Search, null, Modifier.size(64.dp), Color.LightGray)
                            Spacer(Modifier.height(16.dp))
                            Text("No books found for \"$searchQuery\"", color = Color.Gray)
                            TextButton(onClick = { viewModel.updateSearchQuery("") }) { Text("Clear search") }
                        }
                    }
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

        // --- GLOBAL RECAP DIALOG ---
        // This watches for any recap generated in the ViewModel map
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

@Composable
fun BookCard(
    book: BookEntity,
    isCompact: Boolean = false,
    isLoadingRecap: Boolean = false,
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
            .wrapContentHeight()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
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
                            fontSize = if (isCompact) 24.sp else 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                }

                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = book.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isCompact) 12.sp else 14.sp,
                        maxLines = if (isCompact) 1 else 2,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.Black
                    )
                    Text(
                        text = book.author,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // NEW: Persistent Progress Text
                    Text(
                        text = "${(book.progress * 100).toInt()}% read",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.DarkGray,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // --- ACTION BUTTONS OVERLAY ---
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // QUICK RECAP BUTTON
                IconButton(
                    onClick = onRecapClick,
                    modifier = Modifier
                        .size(if (isCompact) 26.dp else 32.dp)
                        .background(Color.White.copy(alpha = 0.8f), shape = CircleShape)
                ) {
                    if (isLoadingRecap) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.Black)
                    } else {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Recap",
                            tint = Color.Black,
                            modifier = Modifier.size(if (isCompact) 16.dp else 20.dp)
                        )
                    }
                }

                // DELETE BUTTON
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(if (isCompact) 26.dp else 32.dp)
                        .background(Color.Black.copy(alpha = 0.6f), shape = CircleShape)
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
}