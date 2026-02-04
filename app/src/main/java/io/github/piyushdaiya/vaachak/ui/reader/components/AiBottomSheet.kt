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
import android.util.Log
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiBottomSheet(
    responseText: String? = null,
    isImage: Boolean = false,
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
            // --- 1. ACTION BUTTONS ---
            if (!isDictionary) {
                AiActionRow(
                    onExplain = onExplain,
                    onWhoIsThis = onWhoIsThis,
                    onVisualize = onVisualize,
                    isEink = isEink
                )
                HorizontalDivider(color = dividerColor, thickness = if (isEink) 1.dp else 0.5.dp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 2. CONTENT AREA ---
            AiContentState(
                responseText = responseText,
                isImage = isImage,
                isDictionary = isDictionary,
                isLoading = isDictionaryLoading,
                isEink = isEink,
                contentColor = contentColor
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
// --- SUB-COMPONENTS ---

@Composable
private fun AiActionRow(
    onExplain: () -> Unit,
    onWhoIsThis: () -> Unit,
    onVisualize: () -> Unit,
    isEink: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AiActionButton("Explain", Icons.Default.AutoAwesome, onExplain, isEink, Modifier.weight(1f))
        AiActionButton("Who?", Icons.Default.PersonSearch, onWhoIsThis, isEink, Modifier.weight(1f))
        AiActionButton("Visualize", Icons.Default.Brush, onVisualize, isEink, Modifier.weight(1f))
    }
}

@Composable
private fun AiContentState(
    responseText: String?,
    isImage: Boolean,
    isDictionary: Boolean,
    isLoading: Boolean,
    isEink: Boolean,
    contentColor: Color
) {
    when {
        isLoading -> {
            AiLoadingState(isDictionary, isEink)
        }
        !responseText.isNullOrBlank() -> {
            when {
                isImage -> AiImageResult(responseText, isEink)
                isDictionary -> DictionaryResult(responseText, isEink)
                else -> StandardAiResult(responseText, contentColor)
            }
        }
        else -> {
            // Empty State
            Text(
                text = if (isDictionary) "No definition found." else "Select an AI action above.",
                color = if (isEink) Color.Gray else MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        }
    }
}

@Composable
private fun AiLoadingState(isDictionary: Boolean, isEink: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth()
    ) {
        CircularProgressIndicator(
            color = if (isEink) Color.Black else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isDictionary) "Consulting Dictionary..." else "AI is thinking...",
            style = MaterialTheme.typography.bodyMedium,
            color = if (isEink) Color.DarkGray else MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun AiImageResult(responseText: String, isEink: Boolean) {
    // FIX A: Check for error strings before attempting to decode.
    // These prefixes match the error strings returned in AiRepository.visualizeText
    val isErrorMessage = responseText.startsWith("Error", ignoreCase = true) ||
            responseText.startsWith("Timeout", ignoreCase = true) ||
            responseText.startsWith("Cloudflare", ignoreCase = true) ||
            responseText == "Empty Body"

    if (isErrorMessage) {
        // Display the error message returned by the repo
        Text(
            text = "⚠️ $responseText",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        // Attempt to decode
        val bitmap: Bitmap? = remember(responseText) {
            try {
                val imageBytes = Base64.decode(responseText, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            } catch (e: Exception) {
                Log.e("AiBottomSheet", "Bitmap decode failed: ${e.message}")
                null
            }
        }

        if (bitmap != null) {
            Card(
                border = if (isEink) BorderStroke(1.dp, Color.Black) else null,
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "AI Visualization",
                    modifier = Modifier.fillMaxWidth().wrapContentHeight()
                )
            }
        } else {
            // Fallback if it wasn't a standard error message but still failed to decode (e.g., corrupt base64)
            Text(
                text = "⚠️ Failed to decode image response.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun DictionaryResult(htmlText: String, isEink: Boolean) {
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
                htmlText,
                HtmlCompat.FROM_HTML_MODE_COMPACT
            )
            val textColor = if (isEink) android.graphics.Color.BLACK else android.graphics.Color.DKGRAY
            textView.setTextColor(textColor)
        }
    )
}

@Composable
private fun StandardAiResult(text: String, contentColor: Color) {
    Text(
        text = text,
        color = contentColor,
        style = MaterialTheme.typography.bodyLarge.copy(
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2
        ),
        modifier = Modifier.fillMaxWidth()
    )
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