package com.dimenzy.app.ui.common

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

enum class CameraAccess { Unknown, Granted, Denied }

/** The camera permission, checked again on every resume. [request] runs [onGranted] once it is granted. */
class CameraPermission internal constructor(
  private val access: MutableState<CameraAccess>,
  private val launch: () -> Unit,
  private val context: Context,
  private val pending: MutableState<Boolean>,
) {
  val state: CameraAccess get() = access.value

  /** Asks if needed; [onGranted] runs right away when the permission is already there. */
  fun request(onGranted: () -> Unit) {
    if (isCameraGranted(context)) {
      access.value = CameraAccess.Granted
      onGranted()
    } else {
      pending.value = true
      launch()
    }
  }

  fun openSettings() {
    context.startActivity(
      Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
    )
  }
}

private fun isCameraGranted(context: Context) =
  ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

/** [onGranted] opens the camera after the system dialog says yes, so one tap on "Take photo" is enough. */
@Composable
fun rememberCameraPermission(onGranted: () -> Unit): CameraPermission {
  val context = LocalContext.current
  val access = rememberSaveable { mutableStateOf(if (isCameraGranted(context)) CameraAccess.Granted else CameraAccess.Unknown) }
  val pending = rememberSaveable { mutableStateOf(false) }

  // The real permission wins over saved state: it can be granted or revoked in Settings while the app is away.
  LifecycleResumeEffect(Unit) {
    var current by access
    current = when {
      isCameraGranted(context) -> CameraAccess.Granted
      current == CameraAccess.Granted -> CameraAccess.Unknown
      else -> current
    }
    onPauseOrDispose { }
  }

  val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
    access.value = if (granted) CameraAccess.Granted else CameraAccess.Denied
    val wasPending = pending.value
    pending.value = false
    if (granted && wasPending) onGranted()
  }

  return CameraPermission(access, { launcher.launch(Manifest.permission.CAMERA) }, context, pending)
}
