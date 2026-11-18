package com.dimenzy.app.ui.preview

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

private const val TURN_DEGREES = 360f
private const val TURN_MS = 4000
// Eases in and settles long, like a hand turning it and letting go
private val TurnEasing = CubicBezierEasing(0.45f, 0f, 0.25f, 1f)

/**
 * The model's yaw in degrees, as if someone turned it once to show it is 3D: when [ready], it makes one full
 * turn over four seconds and settles back on the model's own pose. A full turn, not a swing to a fixed angle,
 * because models face any direction (a generated one, or a sample like the fridge, whose authored front is not
 * the camera's): it shows every side and always ends on the view the screen had before. Dragging still orbits
 * the camera. It runs again each time the screen enters composition (back from AR, a reload). Returns a reader,
 * so only the node that reads it recomposes per frame. Follows the system animation scale.
 */
@Composable
internal fun rememberModelSwing(ready: Boolean): () -> Float {
  val swing = remember { Animatable(0f) }
  LaunchedEffect(ready) {
    if (ready) swing.animateTo(TURN_DEGREES, tween(TURN_MS, easing = TurnEasing))
  }
  return { swing.value }
}
