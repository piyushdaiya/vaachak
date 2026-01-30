package io.github.piyushdaiya.vaachak.ui.reader.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VaachakHeader(
    title: String,
    onBack: () -> Unit,
    showBackButton: Boolean = true,
    isEink: Boolean = false,
    // NEW: Flexible slot for 1 or more action buttons
    actions: @Composable RowScope.() -> Unit = {}
) {
    val containerColor = if (isEink) Color.White else MaterialTheme.colorScheme.surface
    val contentColor = if (isEink) Color.Black else MaterialTheme.colorScheme.onSurface
    val dividerColor = if (isEink) Color.Black else MaterialTheme.colorScheme.outlineVariant

    Surface(
        color = containerColor,
        contentColor = contentColor, // Automatically tints icons inside
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Back Icon
                if (showBackButton) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(16.dp))
                }

                // 2. Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = if (isEink) FontWeight.Bold else FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )

                // 3. Flexible Actions (Settings, Recall, etc.)
                actions()
            }

            HorizontalDivider(thickness = if (isEink) 1.dp else 0.5.dp, color = dividerColor)
        }
    }
}