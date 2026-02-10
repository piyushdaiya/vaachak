package io.github.piyushdaiya.vaachak.ui.catalog

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import io.github.piyushdaiya.vaachak.ui.reader.components.VaachakHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    onBack: () -> Unit,
    viewModel: CatalogViewModel = hiltViewModel()
) {
    // 1. Observe Clean Data
    val feedItems by viewModel.feedItems.collectAsState()
    val screenTitle by viewModel.screenTitle.collectAsState() // Dynamic Title!
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val catalogs by viewModel.catalogs.collectAsState()

    val isEink by viewModel.isEinkEnabled.collectAsState()
    val isOffline by viewModel.isOfflineMode.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showMenu by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    BackHandler {
        if (!viewModel.goBack()) onBack()
    }

    val containerColor = if (isEink) Color.White else MaterialTheme.colorScheme.background
    val contentColor = if (isEink) Color.Black else MaterialTheme.colorScheme.onBackground

    Scaffold(
        topBar = {
            VaachakHeader(
                title = screenTitle, // UPDATED: Shows "Science Fiction", "History" etc.
                onBack = { if (!viewModel.goBack()) onBack() },
                showBackButton = true,
                isEink = isEink,
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.AddLink, contentDescription = "Add Library", tint = contentColor)
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.Menu, contentDescription = "Switch Catalog", tint = contentColor)
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(if(isEink) Color.White else MaterialTheme.colorScheme.surface)
                        ) {
                            Text(
                                "My Libraries",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                color = Color.Gray
                            )

                            if (catalogs.isEmpty()) {
                                DropdownMenuItem(text = { Text("No catalogs found") }, onClick = {})
                            }

                            catalogs.forEach { catalog ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(catalog.title, fontWeight = FontWeight.Bold, color = if(isEink) Color.Black else MaterialTheme.colorScheme.onSurface)
                                            Text(catalog.url, style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    },
                                    trailingIcon = if (!catalog.isPredefined) {
                                        { IconButton(onClick = { viewModel.deleteCatalog(catalog) }) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) } }
                                    } else null,
                                    onClick = {
                                        viewModel.switchCatalog(catalog)
                                        showMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = containerColor,
        contentColor = contentColor
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {

            when {
                isOffline -> OfflinePlaceholder(isEink)

                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = if(isEink) Color.Black else MaterialTheme.colorScheme.primary
                    )
                }

                feedItems.isEmpty() && !isLoading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CloudOff, null, Modifier.size(64.dp), tint = Color.Gray)
                        Spacer(Modifier.height(16.dp))
                        Text("No items found here.", style = MaterialTheme.typography.titleMedium, color = contentColor)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = {
                            val first = catalogs.firstOrNull()
                            if (first != null) viewModel.switchCatalog(first)
                        }) { Text("Reload / Home") }
                    }
                }

                // 2. UNIFIED LIST (Folders + Books)
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(feedItems) { item ->
                            when (item) {
                                is CatalogItem.Folder -> CatalogFolderItem(item, isEink) { viewModel.handleItemClick(item) }
                                is CatalogItem.Book -> CatalogBookItem(item, isEink) { viewModel.handleItemClick(item) }
                            }
                            HorizontalDivider(thickness = 0.5.dp, color = if(isEink) Color.Black else MaterialTheme.colorScheme.outlineVariant)
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddCatalogDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { title, url, user, pass, allowInsecure ->
                    viewModel.addCustomCatalog(title, url, user, pass, allowInsecure)
                    showAddDialog = false
                }
            )
        }
    }
}

// --- COMPONENTS ---

@Composable
fun CatalogFolderItem(item: CatalogItem.Folder, isEink: Boolean, onClick: () -> Unit) {
    val textColor = if (isEink) Color.Black else MaterialTheme.colorScheme.onSurface
    val iconColor = if (isEink) Color.Black else MaterialTheme.colorScheme.primary

    ListItem(
        colors = ListItemDefaults.colors(containerColor = if (isEink) Color.White else MaterialTheme.colorScheme.surface, headlineColor = textColor),
        headlineContent = { Text(item.title, fontWeight = FontWeight.SemiBold) },
        leadingContent = { Icon(Icons.Default.Folder, null, tint = iconColor) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun CatalogBookItem(item: CatalogItem.Book, isEink: Boolean, onDownload: () -> Unit) {
    val textColor = if (isEink) Color.Black else MaterialTheme.colorScheme.onSurface
    val subTextColor = if (isEink) Color.DarkGray else MaterialTheme.colorScheme.onSurfaceVariant

    ListItem(
        colors = ListItemDefaults.colors(
            containerColor = if (isEink) Color.White else MaterialTheme.colorScheme.surface,
            headlineColor = textColor,
            supportingColor = subTextColor
        ),
        headlineContent = {
            Text(item.title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Column {
                Text(item.author.ifBlank { "Unknown Author" }, maxLines = 1, style = MaterialTheme.typography.bodySmall)

                // FORMAT BADGE (EPUB/PDF)
                if (item.format.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = if(isEink) Color.LightGray else MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = item.format,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            color = if(isEink) Color.Black else MaterialTheme.colorScheme.onSecondaryContainer,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        },
        leadingContent = {
            Surface(
                shape = RoundedCornerShape(4.dp),
                border = if (isEink) BorderStroke(1.dp, Color.Black) else null,
                color = Color.LightGray,
                modifier = Modifier.size(45.dp, 70.dp)
            ) {
                if (item.imageUrl != null) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        colorFilter = if(isEink) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text(item.title.take(1).uppercase(), fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        },
        trailingContent = {
            IconButton(onClick = onDownload) {
                Icon(
                    Icons.Default.CloudDownload,
                    contentDescription = "Download",
                    tint = if(isEink) Color.Black else MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}

// --- DIALOG & PLACEHOLDER (Unchanged) ---
@Composable
fun AddCatalogDialog(onDismiss: () -> Unit, onAdd: (String, String, String?, String?, Boolean) -> Unit) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isCalibre by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var allowInsecure by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Catalog") },
        text = {
            Column(modifier = Modifier.verticalScroll(scrollState).padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("OPDS URL") }, placeholder = { Text(if(isCalibre) "http://192.168.1.x:8080/opds" else "https://...") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { isCalibre = !isCalibre }) {
                        Checkbox(checked = isCalibre, onCheckedChange = { isCalibre = it })
                        Text("Calibre Server Helper", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { allowInsecure = !allowInsecure }) {
                        Checkbox(checked = allowInsecure, onCheckedChange = { allowInsecure = it })
                        Column {
                            Text("Allow Self-Signed Certificates", style = MaterialTheme.typography.bodyMedium)
                            Text("Use for local servers with SSL errors", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, singleLine = true, visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = { IconButton(onClick = { showPassword = !showPassword }) { Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null) } }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { onAdd(title, url, username.ifBlank { null }, password.ifBlank { null }, allowInsecure) }, enabled = title.isNotBlank() && url.isNotBlank()) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
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