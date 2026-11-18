package com.dimenzy.app.ui.ar

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.dimenzy.app.ui.common.CameraAccess
import com.dimenzy.app.ui.common.CameraPermission

/** Where the AR screen stands before any camera view (spec 0003 state transitions). */
sealed interface ArReadiness {
  data object Checking : ArReadiness

  /** The Play Services for AR install screen is open; the next resume carries on. */
  data object Installing : ArReadiness

  /** ARCore is ready and the camera permission dialog is up. */
  data object NeedsCamera : ArReadiness

  data object Ready : ArReadiness

  data class Blocked(val error: ArError) : ArReadiness
}

/** The gate's current [state] plus the actions the blocked screens offer. */
class ArReadinessState internal constructor(
  val state: ArReadiness,
  private val restart: (fromInstallTap: Boolean) -> Unit,
  private val camera: CameraPermission,
) {
  fun retry() = restart(false)

  fun install() = restart(true)

  fun askCamera() = camera.request(onGranted = {})

  fun openSettings() = camera.openSettings()
}

/**
 * Runs the ARCore check and install flow on every resume and reads the camera permission live.
 * A resume never shows "Checking" again: the check runs silently and only a new failure changes the screen.
 */
@Composable
fun rememberArReadiness(check: ArCoreCheck, camera: CameraPermission): ArReadinessState {
  val activity = LocalActivity.current
  var arCore by remember { mutableStateOf<ArReadiness>(ArReadiness.Checking) }
  var runKey by remember { mutableIntStateOf(0) }
  // ARCore's documented flow: true on the first ask and after an Install tap, false on the resume after INSTALL_REQUESTED.
  var userRequestedInstall by remember { mutableStateOf(true) }
  // After a decline, a resume must not open the install prompt again; only the Install button does.
  var declined by remember { mutableStateOf(false) }

  LifecycleResumeEffect(Unit) {
    runKey++
    onPauseOrDispose { }
  }

  LaunchedEffect(runKey) {
    if (runKey == 0) return@LaunchedEffect // the first resume starts the first run
    arCore = when (check.availability()) {
      ArCoreStatus.Ready -> ArReadiness.Ready
      ArCoreStatus.Unsupported -> ArReadiness.Blocked(ArError.Unsupported)
      ArCoreStatus.Failed -> ArReadiness.Blocked(ArError.CheckFailed)
      ArCoreStatus.NeedsInstall -> when {
        declined -> ArReadiness.Blocked(ArError.InstallDeclined)
        activity == null -> ArReadiness.Blocked(ArError.CheckFailed)
        else -> when (check.requestInstall(activity, userRequestedInstall)) {
          InstallResult.Installed -> {
            userRequestedInstall = true
            ArReadiness.Ready
          }
          InstallResult.Requested -> {
            userRequestedInstall = false
            ArReadiness.Installing
          }
          InstallResult.Declined -> {
            userRequestedInstall = true
            declined = true
            ArReadiness.Blocked(ArError.InstallDeclined)
          }
          InstallResult.Failed -> ArReadiness.Blocked(ArError.CheckFailed)
        }
      }
    }
  }

  // The camera part is read live, so a grant in Settings or a revoke shows up on the next resume.
  val state = when {
    arCore != ArReadiness.Ready -> arCore
    camera.state == CameraAccess.Granted -> ArReadiness.Ready
    camera.state == CameraAccess.Denied -> ArReadiness.Blocked(ArError.CameraDenied)
    else -> ArReadiness.NeedsCamera
  }

  LaunchedEffect(state) {
    if (state == ArReadiness.NeedsCamera) camera.request(onGranted = {})
  }

  return ArReadinessState(
    state = state,
    restart = { fromInstallTap ->
      if (fromInstallTap) {
        declined = false
        userRequestedInstall = true
      }
      arCore = ArReadiness.Checking
      runKey++
    },
    camera = camera,
  )
}
