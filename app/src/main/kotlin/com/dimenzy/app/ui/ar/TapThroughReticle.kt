package com.dimenzy.app.ui.ar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntSize
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.ARSceneScope
import io.github.sceneview.ar.PlacementReticleVisual
import io.github.sceneview.ar.ReticlePhase
import io.github.sceneview.ar.reticlePhaseFor
import io.github.sceneview.loaders.MaterialLoader
import io.github.sceneview.node.Node

/**
 * The placement ring, drawn so a tap on it reaches the surface underneath.
 *
 * SceneView 4.69.6's built-in ring (`PlacementScene(showReticle = true)`) is hittable 3D geometry, and
 * `PlacementScene` treats any tap that lands on a scene node as a gesture on that node and places nothing.
 * So a tap on the ring, exactly where people tap, never placed the model. This is the same ring (the
 * library's public `PlacementReticle` + `PlacementReticleVisual`, same look and searching/ready/locked
 * phases) with every node of it marked non-hittable. It appears once a surface is first hit, so it never
 * draws at the world origin before tracking starts (the built-in ring waits for tracking for that reason).
 */
@Composable
internal fun ARSceneScope.TapThroughReticle(materialLoader: MaterialLoader, viewport: IntSize) {
  if (viewport == IntSize.Zero) return
  var phase by remember { mutableStateOf(ReticlePhase.SEARCHING) }
  var surfaceSeen by remember { mutableStateOf(false) }
  // Plain holder, not state: filled once by `apply` when the ring's node is created.
  val ring = remember { arrayOfNulls<Node>(1) }
  PlacementReticle(
    xPx = viewport.width / 2f,
    yPx = viewport.height / 2f,
    onHitResultChanged = { hit ->
      phase = reticlePhaseFor(hasHit = hit != null, lockedOnPlane = hit.isTrackedPlane())
      if (hit != null) surfaceSeen = true
    },
    apply = { ring[0] = this },
  ) {
    PlacementReticleVisual(materialLoader = materialLoader, phase = phase)
    val visible = surfaceSeen
    // After every recomposition: the centre dot comes and goes with the phase, so mark the whole ring again.
    SideEffect {
      ring[0]?.let {
        it.isVisible = visible
        it.letTapsThrough()
      }
    }
  }
}

/** A tracked plane: what turns the ring from "ready" to "locked" (the library's own rule). */
private fun HitResult?.isTrackedPlane(): Boolean {
  val trackable = this?.trackable ?: return false
  return trackable is Plane && trackable.trackingState == TrackingState.TRACKING
}

/** Takes this node and everything under it out of touch hit tests, so taps fall through to the surface. */
private fun Node.letTapsThrough() {
  isHittable = false
  isTouchable = false
  childNodes.forEach { it.letTapsThrough() }
}
