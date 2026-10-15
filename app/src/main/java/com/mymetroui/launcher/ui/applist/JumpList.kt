package com.mymetroui.launcher.ui.applist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun JumpList(available: Set<Char>, onPick: (Char) -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val letters = (listOf('#') + ('A'..'Z') + available).distinct()
    Box(
        modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.92f)).testTag("jump-list"),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.fillMaxSize().clickable(onClick = onDismiss))
        LazyVerticalGrid(GridCells.Fixed(4), Modifier.padding(16.dp)) {
            items(letters) { c ->
                val enabled = c in available
                val cell = Modifier.padding(4.dp).size(48.dp)
                    .alpha(if (enabled) 1f else 0.3f)
                    .background(MaterialTheme.colorScheme.primary)
                    .testTag("jump-$c")
                Box(if (enabled) cell.clickable { onPick(c) } else cell, contentAlignment = Alignment.Center) {
                    Text(c.toString(), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Clip)
                }
            }
        }
    }
}
