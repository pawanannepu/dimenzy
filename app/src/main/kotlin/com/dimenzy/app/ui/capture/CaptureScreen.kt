package com.dimenzy.app.ui.capture

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

private enum class CameraAccess { Unknown, Granted, Denied }

/**
 * Scaffold version of the capture screen: proves the CAMERA runtime permission works.
 * Feature 3 adds TakePicture, the Photo Picker, and the "Try sample model" button here.
 */
@Composable
fun CaptureScreen() {
  val context = LocalContext.current
  fun isGranted() =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

  var access by rememberSaveable { mutableStateOf(if (isGranted()) CameraAccess.Granted else CameraAccess.Unknown) }

  // The real permission wins over saved state: it can be granted or revoked in Settings while the app is away.
  LifecycleResumeEffect(Unit) {
    access = when {
      isGranted() -> CameraAccess.Granted
      access == CameraAccess.Granted -> CameraAccess.Unknown
      else -> access
    }
    onPauseOrDispose { }
  }

  val requestCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
    access = if (granted) CameraAccess.Granted else CameraAccess.Denied
  }

  Scaffold { innerPadding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text("Dimenzy", style = MaterialTheme.typography.headlineLarge)
      Text(
        "Turn a photo of an object into a 3D model and place it in your room.",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
      )
      when (access) {
        CameraAccess.Granted -> Text("Camera ready", style = MaterialTheme.typography.titleMedium)
        CameraAccess.Unknown -> Button(onClick = { requestCamera.launch(Manifest.permission.CAMERA) }) {
          Text("Allow camera")
        }
        CameraAccess.Denied -> {
          Text(
            "Dimenzy needs the camera to take photos and show AR.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
          )
          Button(onClick = { requestCamera.launch(Manifest.permission.CAMERA) }) { Text("Try again") }
          OutlinedButton(onClick = {
            context.startActivity(
              Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
            )
          }) { Text("Open settings") }
        }
      }
    }
  }
}
