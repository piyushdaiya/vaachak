package io.github.piyushdaiya.vaachak.ui.bookshelf

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import io.github.piyushdaiya.vaachak.data.local.BookEntity
import androidx.compose.ui.graphics.StrokeCap


@Composable
fun BookshelfScreen(
    onBookClick: (String) -> Unit,
    viewModel: BookshelfViewModel = hiltViewModel()
) {
    val books by viewModel.allBooks.collectAsState() // Renamed to allBooks for clarity
    val recentBook by viewModel.recentBook.collectAsState() // The logic we added to ViewModel
    val snackbarHostState = remember { SnackbarHostState() }
    val message by viewModel.snackbarMessage

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.snackbarMessage.value = null
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importBook(uri)
        }
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
        if (books.isEmpty()) {
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
            // Using LazyVerticalGrid with span logic to allow the Recent Card to take full width
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                // --- SECTION: CONTINUE READING ---
                recentBook?.let { book ->
                    item(span = { GridItemSpan(2) }) {
                        Column(modifier = Modifier.padding(bottom = 16.dp)) {
                            Text(
                                text = "Continue Reading",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            RecentBookCard(
                                book = book,
                                onClick = { onBookClick(book.uriString) }
                            )
                            Spacer(Modifier.height(16.dp))
                            Divider(color = Color.LightGray.copy(alpha = 0.5f))
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "Your Library",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }
                }

                // --- SECTION: ALL BOOKS ---
                items(books, key = { it.id }) { book ->
                    BookCard(
                        title = book.title,
                        author = book.author,
                        coverPath = book.coverPath,
                        progress = book.progress,
                        onClick = { onBookClick(book.uriString) },
                        onDelete = { viewModel.deleteBook(book.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun RecentBookCard(
    book: BookEntity,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .height(IntrinsicSize.Min)
        ) {
            // Smaller Cover Thumbnail
            Box(
                modifier = Modifier
                    .size(width = 60.dp, height = 90.dp)
                    .background(Color.LightGray, MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center
            ) {
                if (book.coverPath != null) {
                    AsyncImage(
                        model = book.coverPath,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(book.title.take(1), fontWeight = FontWeight.Bold)
                }
            }

            Column(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = book.progress.toFloat(),
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = Color(0xFF4CAF50),
                    trackColor = Color(0xFFE0E0E0),
                    strokeCap = StrokeCap.Round
                )
                Text(
                    text = "${(book.progress * 100).toInt()}% completed",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(top = 4.dp)
                )
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
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .aspectRatio(0.7f)
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
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
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Progress Bar Overlay at bottom of cover
                    LinearProgressIndicator(
                        progress = { progress.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.BottomCenter),
                        color = Color(0xFF4CAF50),
                        trackColor = Color.White.copy(alpha = 0.5f)
                    )
                }

                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
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

            // Delete Button Overlay
            Surface(
                onClick = onDelete,
                color = Color.Black.copy(alpha = 0.6f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = Color.White,
                    modifier = Modifier.padding(6.dp)
                )
            }
        }
    }
}