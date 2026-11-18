package com.dimenzy.app.ui.ar

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Quaternion
import dev.romainguy.kotlin.math.inverse
import io.github.sceneview.node.ModelNode
import kotlin.math.atan2

/** Slider range: a full turn, with the start facing in the middle. */
internal const val MIN_ROTATION_DEGREES = -180f
internal const val MAX_ROTATION_DEGREES = 180f

private val UP = Float3(0f, 1f, 0f)

/**
 * Links the placed [ModelNode] to the rotation slider. [degrees] is the turn around the vertical axis,
 * relative to how the model faced when placed. The slider sets it through [rotate]; a twist updates it when the twist ends.
 */
@Stable
internal class ModelRotation {
  var degrees by mutableFloatStateOf(0f)
    private set

  private var node: ModelNode? = null
  private var startQuaternion = Quaternion()

  /** Called once per placement. A new placement starts facing the way it landed (0°). */
  fun attach(node: ModelNode) {
    this.node = node
    startQuaternion = node.quaternion
    degrees = 0f
    node.onRotateEnd = { _, _ -> degrees = yawFromStart(node.quaternion) }
  }

  fun rotate(newDegrees: Float) {
    val target = node ?: return
    degrees = newDegrees.coerceIn(MIN_ROTATION_DEGREES, MAX_ROTATION_DEGREES)
    target.quaternion = startQuaternion * Quaternion.fromAxisAngle(UP, degrees)
  }

  /** The turn around the vertical axis between the start facing and [current], in -180..180. */
  private fun yawFromStart(current: Quaternion): Float {
    val delta = inverse(startQuaternion) * current
    val yaw = Math.toDegrees(2.0 * atan2(delta.y.toDouble(), delta.w.toDouble())).toFloat()
    return normalize(yaw)
  }

  private fun normalize(angle: Float): Float {
    var a = angle % 360f
    if (a > 180f) a -= 360f
    if (a < -180f) a += 360f
    return a
  }
}
