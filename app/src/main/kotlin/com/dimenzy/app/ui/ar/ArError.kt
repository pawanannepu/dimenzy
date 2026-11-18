package com.dimenzy.app.ui.ar

/** Every way the AR screen can fail to show the model in the room (spec 0003 error table). */
sealed interface ArError {
  /** ARCore says `UNSUPPORTED_DEVICE_NOT_CAPABLE`. */
  data object Unsupported : ArError

  /** The Google Play Services for AR install prompt was declined. */
  data object InstallDeclined : ArError

  /** The ARCore check errored, timed out, or never settled within 10 s. */
  data object CheckFailed : ArError

  /** The camera permission dialog returned denied. */
  data object CameraDenied : ArError

  /** Unknown or malformed `modelId`, a missing file, or a file that failed to load. */
  data object ModelLoadFailed : ArError
}

/** The one place AR errors become words (English only, spec 0003). */
fun ArError.message(): String = when (this) {
  ArError.Unsupported -> "AR isn't supported on this phone."
  ArError.InstallDeclined -> "AR needs Google Play Services for AR."
  ArError.CheckFailed -> "Couldn't check AR support. Check your internet and retry."
  ArError.CameraDenied -> "AR needs the camera to see your room."
  ArError.ModelLoadFailed -> "Couldn't open this model."
}
