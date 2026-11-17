package com.dimenzy.app.image

/** Why a photo couldn't be prepared. Shown inline on the capture screen. */
sealed interface PhotoError {
  data object Unreadable : PhotoError
}

sealed interface PhotoOutcome {
  /** `cacheDir/upload.jpg` now holds the prepared photo. */
  data object Ready : PhotoOutcome

  data class Failed(val error: PhotoError) : PhotoOutcome
}
