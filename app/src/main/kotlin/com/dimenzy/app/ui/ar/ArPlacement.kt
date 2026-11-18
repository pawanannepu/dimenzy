package com.dimenzy.app.ui.ar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.google.android.filament.Engine
import com.google.android.filament.gltfio.FilamentInstance
import com.google.ar.core.Config
import io.github.sceneview.ar.PlacementController
import io.github.sceneview.ar.PlacementScene
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.rememberMaterialLoader

/** Start size of the longest side, in metres (spec 0001). Feature 7 replaces it with the entered height. */
internal const val START_SIZE_METERS = 0.4f

private const val HINT = "Drag to move, pinch to resize, twist to rotate."

/**
 * The camera view: `PlacementScene` on floors and tables, with one model drawn at the newest anchor.
 * [instance] is `null` while the file loads; scanning runs meanwhile. [animation] is the clip to loop.
 */
@Composable
internal fun ArPlacement(engine: Engine, modelLoader: ModelLoader, instance: FilamentInstance?, animation: String?) {
  // PlacementScene creates its controller; we keep it so Reset and the overlay can reach it from outside the scene.
  var controller by remember { mutableStateOf<PlacementController?>(null) }
  var bottomBarHeight by remember { mutableStateOf(0.dp) }
  // Plain holder, not state: it reaches into the placed node from the slider outside the scene.
  val size = remember { ModelSize() }
  val rotation = remember { ModelRotation() }
  val density = LocalDensity.current
  val placed = (controller?.count ?: 0) > 0
  // Shared by the scene and our ring; the ring is centred in the measured viewport.
  val materialLoader = rememberMaterialLoader(engine)
  var viewport by remember { mutableStateOf(IntSize.Zero) }

  Box(modifier = Modifier.fillMaxSize().onSizeChanged { viewport = it }) {
    PlacementScene(
      modifier = Modifier.fillMaxSize(),
      engine = engine,
      modelLoader = modelLoader,
      materialLoader = materialLoader,
      planeRenderer = true,
      planeFindingMode = Config.PlaneFindingMode.HORIZONTAL,
      instantPlacement = false,
      // The built-in ring swallows taps (see TapThroughReticle); ours is drawn in `content` below.
      showReticle = false,
      fadePlaneOnFirstPlacement = true,
      coaching = true,
      groundShadows = true,
      sessionConfiguration = { _, config ->
        config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
      },
      coachingBottomClearance = bottomBarHeight + 8.dp,
      onPlaced = { anchor ->        // Every tap adds an anchor; only the newest gets the model, so a tap elsewhere moves it.
        if (instance != null && anchor == controller?.anchors?.lastOrNull()) {
          // A drag on the model is handed up to its AnchorNode, which moves only when it is editable
          // with position editing on (both off by default): it then slides along detected surfaces.
          AnchorNode(anchor = anchor, apply = { isEditable = true; isPositionEditable = true }) {
            ModelNode(
              modelInstance = instance,
              scaleToUnits = START_SIZE_METERS,
              animationName = animation,
              isEditable = true,
              // Runs once per placement, after scaleToUnits set the start scale: links pinch and slider.
              apply = { size.attach(this); rotation.attach(this) },
            )
          }
        }
      },
      content = { placement ->
        SideEffect { if (controller !== placement) controller = placement }
        TapThroughReticle(materialLoader, viewport)
      },
    )
    if (instance == null) {
      LoadingPill(
        modifier = Modifier
          .align(Alignment.TopCenter)
          .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
          .padding(top = 12.dp),
      )
    }
    BottomControls(
      placed = placed,
      size = size,
      rotation = rotation,
      onReset = { controller?.clear() },
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .onSizeChanged { bottomBarHeight = with(density) { it.height.toDp() } },
    )
  }
}

@Composable
private fun LoadingPill(modifier: Modifier = Modifier) {
  Surface(modifier = modifier, shape = RoundedCornerShape(50), color = CameraScrim, contentColor = Color.White) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
      Text("Loading model", style = MaterialTheme.typography.labelLarge)
    }
  }
}

/**
 * The size and rotation sliders, hint and Reset, shown only while a model is placed.
 * Its measured height keeps the coaching card above it.
 */
@Composable
private fun BottomControls(
  placed: Boolean,
  size: ModelSize,
  rotation: ModelRotation,
  onReset: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // Once the model lands, the controls rise in one after another, top to bottom.
    // Reads size.factor here, so moving the slider recomposes only these controls, not the scene.
    Rise(placed, order = 0) { SizeSlider(factor = size.factor, onFactorChange = size::resize) }
    Rise(placed, order = 1) { RotationSlider(degrees = rotation.degrees, onDegreesChange = rotation::rotate) }
    Rise(placed, order = 2) {
      Surface(shape = RoundedCornerShape(12.dp), color = CameraScrim, contentColor = Color.White) {
        Text(
          HINT,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
          style = MaterialTheme.typography.bodyMedium,
          textAlign = TextAlign.Center,
        )
      }
    }
    Rise(placed, order = 3) { FilledTonalButton(onClick = onReset) { Text("Reset") } }
  }
}

// A beat after the model lands, then 70ms apart; a short rise that settles without bounce
private const val RISE_DELAY_MS = 180
private const val RISE_STAGGER_MS = 70
private val RiseEasing = CubicBezierEasing(0.25f, 0.8f, 0.25f, 1f)

/** One control arriving: it fades in while rising 8dp into place, [order] staggers it after the ones above. */
@Composable
private fun ColumnScope.Rise(visible: Boolean, order: Int, content: @Composable () -> Unit) {
  val delay = RISE_DELAY_MS + order * RISE_STAGGER_MS
  val rise = with(LocalDensity.current) { 8.dp.roundToPx() }
  AnimatedVisibility(
    visible = visible,
    enter = fadeIn(tween(320, delay)) + slideInVertically(tween(420, delay, RiseEasing)) { rise },
    exit = fadeOut(tween(150)),
  ) { content() }
}
