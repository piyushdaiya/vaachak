package io.github.piyushdaiya.vaachak.ui.reader.components

import android.text.format.DateFormat
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun ReaderSystemFooter(
    chapterTitle: String,
    isEink: Boolean
) {
    val context = LocalContext.current // FIX: Get Context here
    val containerColor = if (isEink) Color.White else MaterialTheme.colorScheme.surface
    val contentColor = if (isEink) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
    val dividerColor = if (isEink) Color.Black else MaterialTheme.colorScheme.outlineVariant

    // Logic for Clock
    var timeString by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val calendar = Calendar.getInstance()
            // FIX: Passed 'context' instead of 'null'
            val format = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
            timeString = DateFormat.format(format, calendar).toString()
            delay(1000 * 60) // Update every minute
        }
    }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            HorizontalDivider(thickness = if (isEink) 1.dp else 0.5.dp, color = dividerColor)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // LEFT: Chapter Name
                Text(
                    text = chapterTitle.ifEmpty { "Chapter 1" },
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (isEink) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f).padding(end = 16.dp)
                )

                // RIGHT: System Info Group
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("85%", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                        Icon(
                            imageVector = Icons.Default.BatteryFull,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}