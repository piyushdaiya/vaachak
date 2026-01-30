package io.github.piyushdaiya.vaachak.ui.reader.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.text.method.LinkMovementMethod
import android.util.Base64
import android.widget.TextView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape // <--- FIXED: Added missing import
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiBottomSheet(
    responseText: String?,
    isImage: Boolean,
    isDictionary: Boolean = false,
    isDictionaryLoading: Boolean = false,
    isEink: Boolean = false,
    onExplain: () -> Unit,
    onWhoIsThis: () -> Unit,
    onVisualize: () -> Unit,
    onDismiss: () -> Unit
) {
    // Dynamic Theme Colors
    val containerColor = if (isEink) Color.White else MaterialTheme.colorScheme.surfaceContainer
    val contentColor = if (isEink) Color.Black else MaterialTheme.colorScheme.onSurface
    val dividerColor = if (isEink) Color.Black else MaterialTheme.colorScheme.outlineVariant

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = containerColor,
        contentColor = contentColor,
        // E-ink: Sharp rectangular look (optional)
        shape = if (isEink) MaterialTheme.shapes.extraSmall else BottomSheetDefaults.ExpandedShape
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- 1. ACTION BUTTONS (Only if not Dictionary) ---
            if (!isDictionary) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AiActionButton(
                        text = "Explain",
                        icon = Icons.Default.AutoAwesome,
                        onClick = onExplain,
                        isEink = isEink,
                        modifier = Modifier.weight(1f)
                    )
                    AiActionButton(
                        text = "Who?",
                        icon = Icons.Default.PersonSearch,
                        onClick = onWhoIsThis,
                        isEink = isEink,
                        modifier = Modifier.weight(1f)
                    )
                    AiActionButton(
                        text = "Visualize",
                        icon = Icons.Default.Brush,
                        onClick = onVisualize,
                        isEink = isEink,
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = dividerColor, thickness = if(isEink) 1.dp else 0.5.dp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 2. CONTENT AREA ---
            when {
                isDictionaryLoading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            color = if(isEink) Color.Black else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isDictionary) "Consulting Dictionary..." else "AI is thinking...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if(isEink) Color.DarkGray else MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                !responseText.isNullOrBlank() -> {
                    if (isImage) {
                        val bitmap: Bitmap? = remember(responseText) {
                            try {
                                val imageBytes = Base64.decode(responseText, Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            } catch (e: Exception) { null }
                        }

                        if (bitmap != null) {
                            Card(
                                border = if(isEink) BorderStroke(1.dp, Color.Black) else null,
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "AI Visualization",
                                    modifier = Modifier.fillMaxWidth().wrapContentHeight()
                                )
                            }
                        } else {
                            Text(
                                text = "⚠️ Failed to decode image.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else if (isDictionary) {
                        // Dictionary HTML Rendering
                        AndroidView(
                            modifier = Modifier.fillMaxWidth(),
                            factory = { context ->
                                TextView(context).apply {
                                    textSize = 18f
                                    setTextColor(android.graphics.Color.BLACK)
                                    movementMethod = LinkMovementMethod.getInstance()
                                    setLineSpacing(2f, 1.2f)
                                }
                            },
                            update = { textView ->
                                textView.text = HtmlCompat.fromHtml(
                                    responseText,
                                    HtmlCompat.FROM_HTML_MODE_COMPACT
                                )
                                val textColor = if(isEink) android.graphics.Color.BLACK else android.graphics.Color.DKGRAY
                                textView.setTextColor(textColor)
                            }
                        )
                    } else {
                        // Standard AI Text Response
                        Text(
                            text = responseText,
                            color = contentColor,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2
                            ),
                            modifier = Modifier.fillMaxWidth().align(Alignment.Start)
                        )
                    }
                }

                else -> {
                    // Empty State
                    Text(
                        text = if (isDictionary) "No definition found." else "Select an AI action above.",
                        color = if(isEink) Color.Gray else MaterialTheme.colorScheme.outline,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// --- HELPER: Consistent Buttons ---
@Composable
fun AiActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isEink: Boolean,
    modifier: Modifier = Modifier
) {
    val contentColor = if (isEink) Color.Black else MaterialTheme.colorScheme.primary
    val borderColor = if (isEink) Color.Black else MaterialTheme.colorScheme.outline

    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = contentColor,
            containerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp) // Compact
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isEink) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}