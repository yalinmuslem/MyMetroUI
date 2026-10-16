package com.mymetroui.launcher.ui.actioncenter

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.mymetroui.launcher.data.notif.NotifItem
import com.mymetroui.launcher.data.quick.QuickKind
import com.mymetroui.launcher.ui.motion.MotionSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Open fraction of the panel (0 closed .. 1 open); drag updates are synchronous, settling is animated. */
@Stable
class ActionCenterHostState(private val scope: CoroutineScope, private val onOpening: () -> Unit) {
    var fraction by mutableFloatStateOf(0f)
        private set
    val isOpen: Boolean get() = fraction > 0f
    private var job: Job? = null

    fun dragBy(delta: Float) {
        job?.cancel()
        fraction = (fraction + delta).coerceIn(0f, 1f)
    }

    fun settle(open: Boolean) {
        job?.cancel()
        job = scope.launch {
            if (open) onOpening()
            animate(fraction, if (open) 1f else 0f, animationSpec = tween(MotionSpec.ActionCenterDurationMs)) { v, _ -> fraction = v }
        }
    }

    fun open() = settle(true)
    fun close() = settle(false)
}

@Composable
fun rememberActionCenterHostState(controller: ActionCenterController): ActionCenterHostState {
    val scope = rememberCoroutineScope()
    return remember(controller) { ActionCenterHostState(scope) { controller.refresh() } }
}

@Composable
fun ActionCenterHost(
    host: ActionCenterHostState,
    controller: ActionCenterController,
    canOpen: Boolean,
    onOpenNotification: (NotifItem) -> Unit,
    onQuick: (QuickKind) -> Unit,
    onRequestWriteSettings: () -> Unit,
    onGrantAccess: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by controller.state.collectAsState()
    BoxWithConstraints(modifier.fillMaxSize()) {
        val estimate = with(LocalDensity.current) { maxHeight.toPx() * 0.6f } // until the panel is measured
        var panelPx by remember { mutableFloatStateOf(0f) }
        val height = if (panelPx > 0f) panelPx else estimate
        var wasOpen by remember { mutableStateOf(false) }

        val dragState = rememberDraggableState { host.dragBy(it / height) }
        val dragModifier = Modifier.draggable(
            dragState, Orientation.Vertical,
            onDragStarted = {
                wasOpen = host.fraction > 0.5f
                if (!wasOpen) controller.refresh()
            },
            onDragStopped = { velocity -> host.settle(PanelDrag.settle(host.fraction, velocity, wasOpen)) },
        )

        // If a drag is cut short (edit mode, turnstile), settle instead of leaving the panel half-way.
        LaunchedEffect(canOpen) {
            if (!canOpen && host.fraction > 0f) host.settle(PanelDrag.settle(host.fraction, 0f, host.fraction > 0.5f))
        }

        if (canOpen) {
            // kept composed while the panel is open so a drag that started here is never cancelled
            Box(
                Modifier.align(Alignment.TopCenter).fillMaxWidth().statusBarsPadding().height(24.dp)
                    .testTag("action-center-edge").then(dragModifier),
            )
        }

        if (host.isOpen) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f * host.fraction))
                    .pointerInput(Unit) { detectTapGestures { host.close() } }
                    .testTag("action-center-scrim"),
            )
            Box(
                Modifier.align(Alignment.TopCenter)
                    .onSizeChanged { panelPx = it.height.toFloat() }
                    .graphicsLayer {
                        translationY = -(1f - host.fraction) * height
                        alpha = if (panelPx > 0f) 1f else 0f
                    }
                    .then(dragModifier)
                    .testTag("action-center"),
            ) {
                ActionCenter(
                    state,
                    ActionCenterActions(
                        onQuick = onQuick,
                        onTorch = controller::setTorch,
                        onBrightness = controller::setBrightness,
                        onRequestWriteSettings = onRequestWriteSettings,
                        onDismiss = controller::dismiss,
                        onClearAll = controller::clearAll,
                        onOpen = { onOpenNotification(it); host.close() },
                        onGrantAccess = onGrantAccess,
                        onClose = host::close,
                    ),
                )
            }
        }
    }
}
