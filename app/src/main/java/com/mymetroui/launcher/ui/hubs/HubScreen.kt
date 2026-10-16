package com.mymetroui.launcher.ui.hubs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mymetroui.launcher.data.hub.HubKind

/** Full-screen frame shared by every hub: big title, close button, body. Set [scrollable] false when the body scrolls itself. */
@Composable
fun HubScreen(
    kind: HubKind,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) { detectTapGestures { } } // swallow touches meant for the page below
            .statusBarsPadding()
            .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            .padding(16.dp)
            .testTag("hub-screen"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(hubLabel(kind)), Modifier.weight(1f).testTag("hub-title"), style = MaterialTheme.typography.headlineLarge)
            Box(Modifier.size(48.dp).clickable(onClick = onClose).testTag("hub-close"), contentAlignment = Alignment.Center) {
                Text("×", style = MaterialTheme.typography.titleMedium)
            }
        }
        content()
    }
}
