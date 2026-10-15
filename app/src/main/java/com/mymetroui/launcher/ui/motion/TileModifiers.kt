package com.mymetroui.launcher.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.launch

/** Returns (rotationX, rotationY) in degrees; the pressed side tilts away from the viewer. */
fun tiltFor(touch: Offset, size: Size): Pair<Float, Float> {
    if (size.width <= 0f || size.height <= 0f) return 0f to 0f
    val nx = (touch.x / size.width * 2f - 1f).coerceIn(-1f, 1f)
    val ny = (touch.y / size.height * 2f - 1f).coerceIn(-1f, 1f)
    // + 0f folds -0f into 0f so equal tilts compare equal
    return (-ny * MotionSpec.TiltMaxDegrees + 0f) to (nx * MotionSpec.TiltMaxDegrees + 0f)
}

fun Modifier.tilt(onTap: () -> Unit, onLongPress: () -> Unit): Modifier = composed {
    val rx = remember { Animatable(0f) }
    val ry = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }
    val size = remember { mutableStateOf(Size.Zero) }
    val scope = rememberCoroutineScope()
    this
        .onSizeChanged { size.value = it.toSize() }
        .graphicsLayer {
            rotationX = rx.value
            rotationY = ry.value
            scaleX = scale.value
            scaleY = scale.value
            cameraDistance = 12f * density
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = { touch ->
                    val (x, y) = tiltFor(touch, size.value)
                    scope.launch { rx.animateTo(x, MotionSpec.TiltReturnSpring) }
                    scope.launch { ry.animateTo(y, MotionSpec.TiltReturnSpring) }
                    scope.launch { scale.animateTo(MotionSpec.TiltPressedScale, MotionSpec.TiltReturnSpring) }
                    tryAwaitRelease() // false when cancelled by drag-out or a parent scroll; either way spring back
                    scope.launch { rx.animateTo(0f, MotionSpec.TiltReturnSpring) }
                    scope.launch { ry.animateTo(0f, MotionSpec.TiltReturnSpring) }
                    scope.launch { scale.animateTo(1f, MotionSpec.TiltReturnSpring) }
                },
                onTap = { onTap() },
                onLongPress = { onLongPress() },
            )
        }
}
