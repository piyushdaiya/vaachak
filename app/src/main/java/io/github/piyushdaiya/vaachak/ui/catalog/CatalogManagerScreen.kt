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

package io.github.piyushdaiya.vaachak.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.github.piyushdaiya.vaachak.data.local.OpdsEntity
import io.github.piyushdaiya.vaachak.ui.reader.components.VaachakHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogManagerScreen(
    onNavigateBack: () -> Unit,
    onCatalogSelected: () -> Unit, // Callback to navigate to Browser
    viewModel: CatalogViewModel = hiltViewModel()
) {
    val catalogs by viewModel.catalogs.collectAsState()
    val isEink by viewModel.isEinkEnabled.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingFeed by remember { mutableStateOf<OpdsEntity?>(null) }

    val containerColor = if (isEink) Color.White else MaterialTheme.colorScheme.background
    val contentColor = if (isEink) Color.Black else MaterialTheme.colorScheme.onBackground

    Scaffold(
        topBar = {
            VaachakHeader(
                title = "My Catalogs",
                onBack = onNavigateBack,
                showBackButton = true,
                isEink = isEink
            )
        },
        containerColor = containerColor,
        contentColor = contentColor,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = if(isEink) Color.Black else MaterialTheme.colorScheme.primary,
                contentColor = if(isEink) Color.White else MaterialTheme.colorScheme.onPrimary
            ) { Icon(Icons.Default.Add, "Add Catalog") }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (catalogs.isEmpty()) {
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LibraryBooks, null, Modifier.size(64.dp), tint = Color.Gray)
                    Spacer(Modifier.height(16.dp))
                    Text("No catalogs added yet.", style = MaterialTheme.typography.titleMedium, color = contentColor)
                    Text("Click + to add a library.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(catalogs) { feed ->
                        CatalogRow(
                            feed = feed,
                            isEink = isEink,
                            onClick = {
                                viewModel.openCatalog(feed)
                                onCatalogSelected()
                            },
                            onEdit = { editingFeed = feed },
                            onDelete = { viewModel.deleteCatalog(feed) }
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = if(isEink) Color.Black else MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        CatalogDialog(title = "Add Catalog", onDismiss = { showAddDialog = false }, onConfirm = { n, u, usr, p, i -> viewModel.addCatalog(n, u, usr, p, i); showAddDialog = false })
    }

    editingFeed?.let { feed ->
        CatalogDialog(
            title = "Edit Catalog",
            initialName = feed.title, initialUrl = feed.url, initialUser = feed.username, initialPass = feed.password, initialInsecure = feed.allowInsecure,
            onDismiss = { editingFeed = null },
            onConfirm = { n, u, usr, p, i -> viewModel.updateCatalog(feed, n, u, usr, p, i); editingFeed = null }
        )
    }
}

@Composable
fun CatalogRow(feed: OpdsEntity, isEink: Boolean, onClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    val textColor = if (isEink) Color.Black else MaterialTheme.colorScheme.onSurface

    ListItem(
        colors = ListItemDefaults.colors(containerColor = if (isEink) Color.White else MaterialTheme.colorScheme.surface, headlineColor = textColor),
        headlineContent = { Text(feed.title, fontWeight = FontWeight.Bold) },
        supportingContent = { Text(feed.url, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingContent = { Icon(Icons.Default.ImportContacts, null, tint = if(isEink) Color.Black else MaterialTheme.colorScheme.primary) },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "Menu", tint = if(isEink) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant) }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(if(isEink) Color.White else MaterialTheme.colorScheme.surface)) {
                    if (!feed.isPredefined) {
                        DropdownMenuItem(text = { Text("Edit", color = textColor) }, onClick = { showMenu = false; onEdit() }, leadingIcon = { Icon(Icons.Default.Edit, null, tint = textColor) })
                        DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, onClick = { showMenu = false; onDelete() }, leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) })
                    } else {
                        DropdownMenuItem(text = { Text("Built-in") }, onClick = {})
                    }
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun CatalogDialog(
    title: String,
    initialName: String = "",
    initialUrl: String = "",
    initialUser: String? = null,
    initialPass: String? = null,
    initialInsecure: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String?, String?, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var url by remember { mutableStateOf(initialUrl) }
    var user by remember { mutableStateOf(initialUser ?: "") }
    var pass by remember { mutableStateOf(initialPass ?: "") }
    var insecure by remember { mutableStateOf(initialInsecure) }
    var isCalibre by remember { mutableStateOf(false) }
    var showPass by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL") }, placeholder = { Text(if(isCalibre) "http://192.168.x.x:8080/opds" else "https://...") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { isCalibre = !isCalibre }) {
                    Checkbox(checked = isCalibre, onCheckedChange = { isCalibre = it })
                    Text("Calibre Helper", style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text("Username") }, singleLine = true)
                OutlinedTextField(
                    value = pass, onValueChange = { pass = it }, label = { Text("Password") }, singleLine = true,
                    visualTransformation = if(showPass) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { IconButton(onClick = { showPass = !showPass }) { Icon(if(showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility, null) } }
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { insecure = !insecure }) {
                    Checkbox(checked = insecure, onCheckedChange = { insecure = it })
                    Text("Allow Insecure (SSL)", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(name, url, user.ifBlank{null}, pass.ifBlank{null}, insecure) }, enabled = name.isNotBlank() && url.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

