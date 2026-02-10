package io.github.piyushdaiya.vaachak.ui.catalog

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import io.github.piyushdaiya.vaachak.ui.reader.components.VaachakHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogBrowserScreen(
    onBack: () -> Unit,
    onReadBook: (String) -> Unit,        // NEW: Open Reader
    onGoToBookshelf: () -> Unit,         // NEW: Navigate to Bookshelf
    viewModel: CatalogViewModel = hiltViewModel()
) {
    val feedItems by viewModel.feedItems.collectAsState()
    val screenTitle by viewModel.screenTitle.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isEink by viewModel.isEinkEnabled.collectAsState()
    val isOffline by viewModel.isOfflineMode.collectAsState()

    val breadcrumbs by viewModel.breadcrumbs.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val paginationItem = feedItems.filterIsInstance<CatalogItem.Pagination>().firstOrNull()

    // NEW: Handle UI Events
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is CatalogUiEvent.ShowSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = event.actionLabel,
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        if (event.actionLabel == "View Library") {
                            onGoToBookshelf()
                        }
                    }
                }
                is CatalogUiEvent.NavigateToReader -> {
                    onReadBook(event.bookUri)
                }
            }
        }
    }

    BackHandler { if (!viewModel.goBack()) onBack() }

    val containerColor = if (isEink) Color.White else MaterialTheme.colorScheme.background
    val contentColor = if (isEink) Color.Black else MaterialTheme.colorScheme.onBackground

    Scaffold(
        topBar = {
            Column {
                VaachakHeader(
                    title = screenTitle,
                    onBack = { if (!viewModel.goBack()) onBack() },
                    showBackButton = true,
                    isEink = isEink
                )

                if (!isOffline && breadcrumbs.size > 1) {
                    BreadcrumbBar(
                        breadcrumbs = breadcrumbs,
                        isEink = isEink,
                        onBreadcrumbClick = { index -> viewModel.onBreadcrumbClick(index) }
                    )
                }

                if (paginationItem != null) {
                    CatalogPaginationItem(
                        item = paginationItem,
                        isEink = isEink,
                        onNext = { viewModel.handlePaginationClick(it) },
                        onPrev = { viewModel.handlePaginationClick(it) },
                        isCompact = true
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = containerColor,
        contentColor = contentColor
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                isOffline -> OfflinePlaceholder(isEink)
                isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = if(isEink) Color.Black else MaterialTheme.colorScheme.primary)
                feedItems.isEmpty() -> {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CloudOff, null, Modifier.size(64.dp), tint = Color.Gray)
                        Text("No items found.", color = contentColor)
                    }
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(feedItems) { item ->
                            when (item) {
                                is CatalogItem.Folder -> CatalogFolderItem(item, isEink) { viewModel.handleItemClick(item) }
                                is CatalogItem.Book -> CatalogBookItem(item, isEink) { viewModel.handleItemClick(item) }
                                is CatalogItem.Pagination -> {
                                    CatalogPaginationItem(
                                        item,
                                        isEink,
                                        onNext = { viewModel.handlePaginationClick(it) },
                                        onPrev = { viewModel.handlePaginationClick(it) }
                                    )
                                }
                            }
                            if (item !is CatalogItem.Pagination) {
                                HorizontalDivider(thickness = 0.5.dp, color = if(isEink) Color.Black else MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

// --- COMPONENTS ---

@Composable
fun CatalogPaginationItem(
    item: CatalogItem.Pagination,
    isEink: Boolean,
    onNext: (String) -> Unit,
    onPrev: (String) -> Unit,
    isCompact: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = if (isCompact) 8.dp else 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (item.prevUrl != null) {
            Button(
                onClick = { onPrev(item.prevUrl) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEink) Color.White else MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = if (isEink) Color.Black else MaterialTheme.colorScheme.onSecondaryContainer
                ),
                border = if (isEink) BorderStroke(1.dp, Color.Black) else null,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBackIos, null, Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Prev", style = MaterialTheme.typography.labelLarge)
            }
        } else {
            Spacer(Modifier.width(1.dp))
        }

        if (item.nextUrl != null) {
            Button(
                onClick = { onNext(item.nextUrl) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isEink) Color.White else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (isEink) Color.Black else MaterialTheme.colorScheme.onPrimaryContainer
                ),
                border = if (isEink) BorderStroke(1.dp, Color.Black) else null,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("Next", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, Modifier.size(14.dp))
            }
        }
    }
}

@Composable
fun BreadcrumbBar(
    breadcrumbs: List<Pair<String, String>>,
    isEink: Boolean,
    onBreadcrumbClick: (Int) -> Unit
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(breadcrumbs.size) { scrollState.animateScrollTo(scrollState.maxValue) }

    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState).padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        breadcrumbs.forEachIndexed { index, item ->
            val isLast = index == breadcrumbs.lastIndex
            val color = if (isLast) if (isEink) Color.Black else MaterialTheme.colorScheme.primary else Color.Gray
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(enabled = !isLast) { onBreadcrumbClick(index) }) {
                Text(item.second, style = MaterialTheme.typography.bodySmall, color = color, fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal)
                if (!isLast) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.Gray, modifier = Modifier.size(16.dp).padding(horizontal = 2.dp))
            }
        }
    }
    HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
}

@Composable
fun CatalogFolderItem(item: CatalogItem.Folder, isEink: Boolean, onClick: () -> Unit) {
    val textColor = if (isEink) Color.Black else MaterialTheme.colorScheme.onSurface
    ListItem(
        colors = ListItemDefaults.colors(containerColor = if (isEink) Color.White else MaterialTheme.colorScheme.surface, headlineColor = textColor),
        headlineContent = { Text(item.title, fontWeight = FontWeight.SemiBold) },
        leadingContent = { Icon(Icons.Default.Folder, null, tint = if(isEink) Color.Black else MaterialTheme.colorScheme.primary) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun CatalogBookItem(item: CatalogItem.Book, isEink: Boolean, onClick: () -> Unit) {
    val textColor = if (isEink) Color.Black else MaterialTheme.colorScheme.onSurface
    val subColor = if (isEink) Color.DarkGray else MaterialTheme.colorScheme.onSurfaceVariant
    val isNavigable = item.format == "DETAIL"

    // Check if book exists using the new existingBookUri property
    val isDownloaded = item.existingBookUri != null

    val icon = when {
        isDownloaded -> Icons.AutoMirrored.Filled.MenuBook // "Read" icon
        isNavigable -> Icons.AutoMirrored.Filled.ArrowForwardIos
        else -> Icons.Default.CloudDownload
    }

    // Green tint if downloaded
    val iconTint = if (isEink) Color.Black else if (isDownloaded) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary

    ListItem(
        colors = ListItemDefaults.colors(containerColor = if (isEink) Color.White else MaterialTheme.colorScheme.surface, headlineColor = textColor, supportingColor = subColor),
        headlineContent = { Text(item.title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Column {
                Text(item.author.ifBlank { "Unknown Author" }, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                if (item.format.isNotEmpty() && item.format != "DETAIL") {
                    Spacer(Modifier.height(4.dp))
                    Surface(color = if(isEink) Color.LightGray else MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(4.dp)) {
                        Text(item.format, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), color = if(isEink) Color.Black else MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 10.sp)
                    }
                }
            }
        },
        leadingContent = {
            Surface(shape = RoundedCornerShape(4.dp), border = if (isEink) BorderStroke(1.dp, Color.Black) else null, color = Color.LightGray, modifier = Modifier.size(45.dp, 70.dp)) {
                if (item.imageUrl != null) {
                    AsyncImage(model = item.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, colorFilter = if(isEink) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null)
                } else {
                    Box(contentAlignment = Alignment.Center) { Text(item.title.take(1).uppercase(), fontWeight = FontWeight.Bold, color = Color.Black) }
                }
            }
        },
        trailingContent = {
            IconButton(onClick = onClick) {
                Icon(icon, "Action", tint = iconTint, modifier = Modifier.size(24.dp))
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun OfflinePlaceholder(isEink: Boolean) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.WifiOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))
        Text("You are in Offline Mode", style = MaterialTheme.typography.titleMedium, color = if(isEink) Color.Black else MaterialTheme.colorScheme.onBackground)
        Text("Disable Offline Mode in Settings to browse catalogs.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
    }
}