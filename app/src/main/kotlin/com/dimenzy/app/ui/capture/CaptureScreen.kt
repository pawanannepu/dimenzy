package com.dimenzy.app.ui.capture

import android.content.ActivityNotFoundException
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import com.dimenzy.app.ui.common.DimenzyOutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dimenzy.app.image.PhotoError
import com.dimenzy.app.ui.common.CameraAccess
import com.dimenzy.app.ui.common.SamplePickerSheet
import com.dimenzy.app.ui.common.rememberCameraPermission

/** Capture: take or pick a photo, see it, then confirm with "Generate 3D model" (the paid call). */
@Composable
fun CaptureScreen(
  viewModel: CaptureViewModel,
  onGenerate: () -> Unit,
  onTrySample: (modelId: String) -> Unit,
) {
  var showSamples by rememberSaveable { mutableStateOf(false) }
  val state by viewModel.state.collectAsStateWithLifecycle()

  val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture(), viewModel::onCameraResult)
  val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
    // null means the picker was cancelled: leave the screen as it was.
    if (uri != null) viewModel.onPhotoChosen(uri)
  }
  val openCamera: () -> Unit = {
    try {
      takePicture.launch(viewModel.newCameraUri())
    } catch (e: ActivityNotFoundException) {
      // No camera app: treat it like a cancel and leave the screen as it was.
      Log.w("Dimenzy", "no camera app")
    }
  }
  val camera = rememberCameraPermission(onGranted = openCamera)

  Scaffold { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Header()
      PhotoPanel(photo = state.photo)
      Text(
        "One object, plain background, good light.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      if (state.error == PhotoError.Unreadable) {
        Text(
          "Couldn't read that photo. Try another.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.error,
          textAlign = TextAlign.Center,
        )
      }

      val photo = state.photo
      if (photo is PhotoState.Ready || photo is PhotoState.Preparing) {
        Button(
          onClick = onGenerate,
          enabled = photo is PhotoState.Ready,
          modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text("Generate 3D model", style = MaterialTheme.typography.titleMedium) }
      }

      Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        DimenzyOutlinedButton(onClick = { camera.request(openCamera) }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
          Text("Take photo")
        }
        DimenzyOutlinedButton(
          onClick = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
          modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { Text("Choose from gallery") }
      }

      if (camera.state == CameraAccess.Denied) {
        CameraDenied(onTryAgain = { camera.request(openCamera) }, onOpenSettings = camera::openSettings)
      }

      TextButton(onClick = { showSamples = true }) { Text("Try a sample model") }
    }
  }

  if (showSamples) {
    SamplePickerSheet(
      onPick = { modelId ->
        showSamples = false
        onTrySample(modelId)
      },
      onDismiss = { showSamples = false },
    )
  }
}

@Composable
private fun Header() {
  Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text("Dimenzy", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
    Text(
      "Turn a photo of an object into a 3D model.",
      style = MaterialTheme.typography.bodyLarge,
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
private fun CameraDenied(onTryAgain: () -> Unit, onOpenSettings: () -> Unit) {
  Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      "Dimenzy needs the camera to take photos and show AR. You can still choose a photo from your gallery.",
      style = MaterialTheme.typography.bodyMedium,
      textAlign = TextAlign.Center,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      TextButton(onClick = onTryAgain) { Text("Try again") }
      TextButton(onClick = onOpenSettings) { Text("Open settings") }
    }
  }
}
