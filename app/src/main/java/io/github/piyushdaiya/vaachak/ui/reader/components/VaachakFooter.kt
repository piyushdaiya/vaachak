package io.github.piyushdaiya.vaachak.ui.reader.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
@Composable
fun VaachakFooter(pageInfo: String) {
    BottomAppBar(
        containerColor = Color.White,
        modifier = Modifier.height(56.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = pageInfo,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}
