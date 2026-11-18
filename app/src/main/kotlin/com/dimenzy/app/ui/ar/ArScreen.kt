package com.dimenzy.app.ui.ar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.dimenzy.app.ui.common.DimenzyOutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dimenzy.app.R
import com.dimenzy.app.ui.common.rememberCameraPermission
import io.github.sceneview.ModelLoadState
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoadState
import io.github.sceneview.rememberModelLoader

/** AR: the readiness gate first, then the model placed on a floor or table (spec 0003). */
@Composable
fun ArScreen(viewModel: ArViewModel, arCoreCheck: ArCoreCheck, onBack: () -> Unit) {
  // Engine and loader live at the top of the screen, never inside the reloadable part.
  val engine = rememberEngine()
  val modelLoader = rememberModelLoader(engine)
  val target by viewModel.target.collectAsStateWithLifecycle()
  val camera = rememberCameraPermission(onGranted = {})
  val readiness = rememberArReadiness(arCoreCheck, camera)

  Box(modifier = Modifier.fillMaxSize()) {
    when (val state = readiness.state) {
      ArReadiness.Ready -> {
        val location = target.location
        if (location == null) {
          ArBlocked(ArError.ModelLoadFailed, readiness, onRetryModel = viewModel::retry, onBack = onBack)
        } else {
          key(target.reloadKey) {
            when (val load = rememberModelLoadState(modelLoader, location)) {
              is ModelLoadState.Failed ->
                ArBlocked(ArError.ModelLoadFailed, readiness, onRetryModel = viewModel::retry, onBack = onBack)
              // The camera view starts while the file loads, so scanning carries on.
              else -> ArPlacement(
                engine,
                modelLoader,
                instance = (load as? ModelLoadState.Ready)?.instance,
                animation = target.animation,
              )
            }
          }
        }
      }
      is ArReadiness.Blocked -> ArBlocked(state.error, readiness, onRetryModel = viewModel::retry, onBack = onBack)
      ArReadiness.Checking, ArReadiness.Installing, ArReadiness.NeedsCamera -> CheckingAr()
    }
    FilledTonalIconButton(
      onClick = onBack,
      modifier = Modifier
        .align(Alignment.TopStart)
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
        .padding(8.dp),
    ) {
      Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Back")
    }
  }
}

@Composable
private fun CheckingAr() {
  Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Column(
      modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(24.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      CircularProgressIndicator()
      Text("Checking AR support…", style = MaterialTheme.typography.bodyLarge)
    }
  }
}

/** A blocked state: the message from [ArError.message] and its next steps (spec 0003 error table). */
@Composable
private fun ArBlocked(error: ArError, readiness: ArReadinessState, onRetryModel: () -> Unit, onBack: () -> Unit) {
  Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Column(
      modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(24.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(error.message(), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
      when (error) {
        ArError.Unsupported -> Unit
        ArError.InstallDeclined -> PrimaryAction("Install", readiness::install)
        ArError.CheckFailed -> PrimaryAction("Retry", readiness::retry)
        ArError.CameraDenied -> {
          PrimaryAction("Try again", readiness::askCamera)
          DimenzyOutlinedButton(onClick = readiness::openSettings, modifier = Modifier.fillMaxWidth()) { Text("Open settings") }
        }
        ArError.ModelLoadFailed -> PrimaryAction("Retry", onRetryModel)
      }
      if (error == ArError.Unsupported) {
        PrimaryAction("Back to 3D view", onBack)
      } else {
        DimenzyOutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to 3D view") }
      }
    }
  }
}

@Composable
private fun PrimaryAction(label: String, onClick: () -> Unit) {
  Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text(label) }
}

/** The dark scrim behind text drawn over the camera view. */
internal val CameraScrim = Color.Black.copy(alpha = 0.55f)
