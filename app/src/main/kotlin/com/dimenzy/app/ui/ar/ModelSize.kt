package com.dimenzy.app.ui.ar

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.node.ModelNode

/** Size limits for pinch and the slider, relative to the start size: 0.1 m to 1.6 m on the longest side. */
internal const val MIN_SIZE_FACTOR = 0.25f
internal const val MAX_SIZE_FACTOR = 4f

/**
 * Links the placed [ModelNode] to the size slider. [factor] is the size relative to the start size
 * (1 = [START_SIZE_METERS]). The slider sets it through [resize]; a pinch updates it when the pinch ends.
 */
@Stable
internal class ModelSize {
  var factor by mutableFloatStateOf(1f)
    private set

  private var node: ModelNode? = null
  private var startScale = 1f

  /** Called once per placement, right after `scaleToUnits` set the start scale. A new placement starts at 1x. */
  fun attach(node: ModelNode) {
    this.node = node
    startScale = node.scale.x
    factor = 1f
    // Pinch is limited to editableScaleRange, an absolute node scale (0.1 to 10 by default).
    // scaleToUnits sets that same scale, so a big model (scale 0.003) could never be pinched
    // and a tiny one hit the cap at once. Make the range relative to the start size instead.
    node.editableScaleRange = (startScale * MIN_SIZE_FACTOR)..(startScale * MAX_SIZE_FACTOR)
    node.onScaleEnd = { _, _ -> factor = (node.scale.x / startScale).coerceIn(MIN_SIZE_FACTOR, MAX_SIZE_FACTOR) }
  }

  fun resize(newFactor: Float) {
    val target = node ?: return
    factor = newFactor.coerceIn(MIN_SIZE_FACTOR, MAX_SIZE_FACTOR)
    target.scale = Float3(startScale * factor)
  }
}
