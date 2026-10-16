package com.mymetroui.launcher.ui.welcome

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mymetroui.launcher.R
import com.mymetroui.launcher.data.live.LiveFace
import com.mymetroui.launcher.ui.actioncenter.PanelDrag
import com.mymetroui.launcher.ui.motion.MotionSpec
import kotlinx.coroutines.launch

/** Display-only first page: clock, date, next event and a notification summary. Swipe up to reveal Start. */
@Composable
fun WelcomeScreen(
    clock: LiveFace.Text?,
    calendar: LiveFace?,
    summary: WelcomeSummary?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) } // 0 .. -height
    var heightPx by remember { mutableFloatStateOf(1f) }
    val bg = MaterialTheme.colorScheme.background
    val tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f).compositeOver(bg)

    val drag = rememberDraggableState { delta -> scope.launch { offset.snapTo((offset.value + delta).coerceIn(-heightPx, 0f)) } }
    Column(
        modifier.fillMaxSize()
            .onSizeChanged { heightPx = it.height.toFloat() }
            .graphicsLayer { translationY = offset.value }
            .background(Brush.verticalGradient(listOf(bg, tint)))
            .draggable(drag, Orientation.Vertical, onDragStopped = { velocity ->
                val away = PanelDrag.settle(-offset.value / heightPx, -velocity, wasOpen = false)
                scope.launch {
                    offset.animateTo(if (away) -heightPx else 0f, tween(MotionSpec.WelcomeDurationMs))
                    if (away) onDismiss()
                }
            })
            .statusBarsPadding().navigationBarsPadding().padding(24.dp)
            .testTag("welcome-screen"),
    ) {
        clock?.let {
            Text(it.title, Modifier.testTag("welcome-clock"), fontSize = 88.sp, style = MaterialTheme.typography.headlineLarge)
            it.lines.firstOrNull()?.let { date -> Text(date, Modifier.testTag("welcome-date"), style = MaterialTheme.typography.titleMedium) }
        }
        (calendar as? LiveFace.Text)?.let { event ->
            Column(Modifier.padding(top = 24.dp).testTag("welcome-calendar")) {
                Text(event.title, style = MaterialTheme.typography.titleMedium)
                event.lines.forEach { Text(it) }
            }
        }
        summary?.let { s ->
            Column(Modifier.padding(top = 24.dp).testTag("welcome-notifs")) {
                Text(pluralStringResource(R.plurals.welcome_notifications, s.count, s.count), style = MaterialTheme.typography.titleMedium)
                s.titles.forEachIndexed { i, t -> Text(t, Modifier.testTag("welcome-title-$i")) }
            }
        }
    }
}
