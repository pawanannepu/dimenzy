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
import androidx.compose.material3.OutlinedButton
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
import io.github.sceneview.SceneView
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberModelLoadState
import io.github.sceneview.rememberModelLoader

/** Preview: the model in a full screen 3D view. Drag to orbit, pinch to zoom. */
@Composable
fun PreviewScreen(viewModel: PreviewViewModel, onNewPhoto: () -> Unit) {
  // Loaders live at the top of the screen, never inside the reloadable part.
  val engine = rememberEngine()
  val modelLoader = rememberModelLoader(engine)
  val environmentLoader = rememberEnvironmentLoader(engine)
  val target by viewModel.target.collectAsStateWithLifecycle()

  Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    val location = target.location
    if (location == null) {
      LoadFailed(onRetry = viewModel::retry, onNewPhoto = onNewPhoto)
      return@Surface
    }
    key(target.reloadKey) {
      when (val load = rememberModelLoadState(modelLoader, location)) {
        is ModelLoadState.Failed -> LoadFailed(onRetry = viewModel::retry, onNewPhoto = onNewPhoto)
        else -> Box(modifier = Modifier.fillMaxSize()) {
          SceneView(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            environmentLoader = environmentLoader,
            // The model is scaled into a 1 m box; 3 m away keeps all of it on a portrait screen.
            cameraManipulator = rememberCameraManipulator(orbitRadius = 3f),
          ) {
            if (load is ModelLoadState.Ready) ModelNode(modelInstance = load.instance, scaleToUnits = 1f)
          }
          if (load is ModelLoadState.Ready) {
            ReadyOverlay(onNewPhoto = onNewPhoto, modifier = Modifier.align(Alignment.BottomCenter))
          } else {
            CircularProgressIndicator(
              modifier = Modifier.align(Alignment.Center).semantics { contentDescription = "Loading model" },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ReadyOverlay(onNewPhoto: () -> Unit, modifier: Modifier = Modifier) {
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
    Button(onClick = onNewPhoto, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("New photo") }
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
    OutlinedButton(onClick = onNewPhoto, modifier = Modifier.fillMaxWidth()) { Text("New photo") }
  }
}
