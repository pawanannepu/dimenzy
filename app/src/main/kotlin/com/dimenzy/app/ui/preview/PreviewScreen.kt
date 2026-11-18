package com.dimenzy.app.ui.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.dimenzy.app.ui.common.DimenzyOutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sceneview.ModelLoadState
import com.dimenzy.app.ui.common.rememberRouteSettled
import com.dimenzy.app.ui.common.sharedPhotoCard
import io.github.sceneview.SceneView
import io.github.sceneview.SurfaceType
import io.github.sceneview.math.Rotation
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberModelLoadState
import io.github.sceneview.rememberModelLoader

/** Preview: the model in a full screen 3D view. Drag to orbit, pinch to zoom. */
@Composable
fun PreviewScreen(viewModel: PreviewViewModel, onNewPhoto: () -> Unit, onPlaceInAr: () -> Unit) {
  val target by viewModel.target.collectAsStateWithLifecycle()
  // Starting the 3D engine holds the main thread for most of a second; started during the route's
  // transition, it would skip the card growing into this screen. So the screen arrives first.
  val settled = rememberRouteSettled()

  // The generating card grows into this screen (and it shrinks back into the photo panel on New photo)
  Surface(modifier = Modifier.sharedPhotoCard().fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    val location = target.location
    when {
      location == null -> LoadFailed(onRetry = viewModel::retry, onNewPhoto = onNewPhoto)
      !settled -> Box(modifier = Modifier.fillMaxSize()) { LoadingModel(Modifier.align(Alignment.Center)) }
      else -> PreviewScene(target, location, onRetry = viewModel::retry, onNewPhoto = onNewPhoto, onPlaceInAr = onPlaceInAr)
    }
  }
}

/** The 3D view with its model, and the actions once the model is in. */
@Composable
private fun PreviewScene(
  target: PreviewTarget,
  location: String,
  onRetry: () -> Unit,
  onNewPhoto: () -> Unit,
  onPlaceInAr: () -> Unit,
) {
  // Loaders live at the top of the scene, never inside the reloadable part.
  val engine = rememberEngine()
  val modelLoader = rememberModelLoader(engine)
  val environmentLoader = rememberEnvironmentLoader(engine)
  key(target.reloadKey) {
    when (val load = rememberModelLoadState(modelLoader, location)) {
      is ModelLoadState.Failed -> LoadFailed(onRetry = onRetry, onNewPhoto = onNewPhoto)
      else -> Box(modifier = Modifier.fillMaxSize()) {
        val swing = rememberModelSwing(ready = load is ModelLoadState.Ready)
        SceneView(
          modifier = Modifier.fillMaxSize(),
          // A see-through TextureView: the theme's background shows behind the model (as in the README hero),
          // and it draws inside the card's shared transition, where a SurfaceView would leave a hole
          surfaceType = SurfaceType.TextureSurface,
          isOpaque = false,
          engine = engine,
          modelLoader = modelLoader,
          environmentLoader = environmentLoader,
          // The model is scaled into a 1 m box; 3 m away keeps all of it on a portrait screen.
          cameraManipulator = rememberCameraManipulator(orbitRadius = 3f),
        ) {
          if (load is ModelLoadState.Ready) {
            ModelNode(
              modelInstance = load.instance,
              scaleToUnits = 1f,
              animationName = target.animation,
              rotation = Rotation(y = swing()),
            )
          }
        }
        if (load is ModelLoadState.Ready) {
          ReadyOverlay(
            onPlaceInAr = onPlaceInAr,
            onNewPhoto = onNewPhoto,
            modifier = Modifier.align(Alignment.BottomCenter),
          )
        } else {
          LoadingModel(Modifier.align(Alignment.Center))
        }
      }
    }
  }
}

@Composable
private fun LoadingModel(modifier: Modifier = Modifier) {
  CircularProgressIndicator(modifier = modifier.semantics { contentDescription = "Loading model" })
}

@Composable
private fun ReadyOverlay(onPlaceInAr: () -> Unit, onNewPhoto: () -> Unit, modifier: Modifier = Modifier) {
  Column(
    modifier = modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing).padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(
      "Drag to spin, pinch to zoom",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    // Shown on every phone; the AR screen explains it when a phone can't run AR (spec 0003).
    Button(onClick = onPlaceInAr, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Place in AR") }
    DimenzyOutlinedButton(onClick = onNewPhoto, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("New photo") }
  }
}

@Composable
private fun LoadFailed(onRetry: () -> Unit, onNewPhoto: () -> Unit) {
  Column(
    modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text("Couldn't open this model.", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
    Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Retry") }
    DimenzyOutlinedButton(onClick = onNewPhoto, modifier = Modifier.fillMaxWidth()) { Text("New photo") }
  }
}
