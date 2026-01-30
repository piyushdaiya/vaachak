package io.github.piyushdaiya.vaachak.ui.reader.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
    onExplain: () -> Unit,
    onWhoIsThis: () -> Unit,
    onVisualize: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        contentColor = Color.Black
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ONLY show buttons if it's NOT a dictionary lookup
            if (!isDictionary) {
                // Action Buttons
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    OutlinedButton(onClick = onExplain) { Text("Explain", color = Color.Black) }
                    OutlinedButton(onClick = onWhoIsThis) { Text("Who is this?", color = Color.Black) }
                    OutlinedButton(onClick = onVisualize) { Text("Visualize", color = Color.Black) }
                }

                HorizontalDivider(color = Color.LightGray)
            }
            Spacer(modifier = Modifier.height(16.dp))

            when {
                isDictionaryLoading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 32.dp).fillMaxWidth()
                    ) {
                        CircularProgressIndicator(color = Color.Black)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isDictionary) "Searching dictionaries..." else "AI is thinking...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }

                !responseText.isNullOrBlank() -> {
                    if (isImage) {
                        // LOGIC STEP: Decode outside of the UI emitting functions
                        val bitmap: Bitmap? = remember(responseText) {
                            try {
                                val imageBytes = Base64.decode(responseText, Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            } catch (e: Exception) {
                                null
                            }
                        }

                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "AI Visualization",
                                modifier = Modifier.fillMaxWidth().wrapContentHeight()
                            )
                        } else {
                            // If bitmap is null, it means decoding failed (responseText is likely an error message)
                            Text(text = responseText, color = Color.Black, style = MaterialTheme.typography.bodyLarge)
                        }
                    } else if (isDictionary) {
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
                            }
                        )
                    } else {
                        Text(
                            text = responseText,
                            color = Color.Black,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth().align(Alignment.Start)
                        )
                    }
                }

                else -> {
                    Text(
                        text = if (isDictionary) "No definition found." else "Select an action above.",
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}