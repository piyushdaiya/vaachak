package io.github.piyushdaiya.vaachak.ui.reader.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiBottomSheet(
    responseText: String?,
    isImage: Boolean,
    onExplain: () -> Unit,
    onWhoIsThis: () -> Unit,
    onVisualize: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                OutlinedButton(onClick = onExplain) { Text("Explain", color = Color.Black) }
                OutlinedButton(onClick = onWhoIsThis) { Text("Who is this?", color = Color.Black) }
                OutlinedButton(onClick = onVisualize) { Text("Visualize", color = Color.Black) }
            }

            HorizontalDivider()

            Spacer(modifier = Modifier.height(16.dp))

            // Response Area
            if (responseText != null) {
                if (isImage && !responseText.contains("Generating")) {
                    val imageBytes = Base64.decode(responseText, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "AI Visualization",
                        modifier = Modifier.fillMaxWidth().height(300.dp)
                    )
                } else {
                    Text(
                        text = responseText,
                        color = Color.Black,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                Text("Select an action above.", color = Color.Gray)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
