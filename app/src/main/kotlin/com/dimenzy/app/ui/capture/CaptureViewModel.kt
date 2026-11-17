package com.dimenzy.app.ui.capture

import android.graphics.Bitmap
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dimenzy.app.image.CameraShots
import com.dimenzy.app.image.ImagePrep
import com.dimenzy.app.image.PhotoError
import com.dimenzy.app.image.PhotoOutcome
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PhotoState {
  data object Empty : PhotoState

  data object Preparing : PhotoState

  data class Ready(val thumbnail: Bitmap?) : PhotoState
}

data class CaptureUiState(val photo: PhotoState = PhotoState.Empty, val error: PhotoError? = null)

/** Capture: `Empty → Preparing → PhotoReady`, with the photo prepared into `cacheDir/upload.jpg`. */
class CaptureViewModel(
  private val savedState: SavedStateHandle,
  private val imagePrep: ImagePrep,
  private val cameraShots: CameraShots,
) : ViewModel() {

  private val _state = MutableStateFlow(CaptureUiState())
  val state: StateFlow<CaptureUiState> = _state.asStateFlow()

  private var prepareJob: Job? = null

  init {
    // The saved flag is trusted only if the file really is still there.
    if (savedState.get<Boolean>(KEY_PHOTO_READY) == true && imagePrep.hasPreparedPhoto()) {
      showPreparedPhoto()
    } else {
      savedState[KEY_PHOTO_READY] = false
    }
  }

  /** A fresh output URI for the system camera, remembered so a restored app can still read the shot. */
  fun newCameraUri(): Uri = cameraShots.outputUri().also { savedState[KEY_PENDING_CAMERA_URI] = it.toString() }

  fun onCameraResult(saved: Boolean) {
    val uri = savedState.get<String>(KEY_PENDING_CAMERA_URI)
    savedState[KEY_PENDING_CAMERA_URI] = null
    if (saved && uri != null) onPhotoChosen(uri.toUri())
  }

  fun onPhotoChosen(source: Uri) {
    prepareJob?.cancel()
    _state.value = CaptureUiState(photo = PhotoState.Preparing)
    prepareJob = viewModelScope.launch {
      when (val outcome = imagePrep.prepare(source)) {
        PhotoOutcome.Ready -> {
          savedState[KEY_PHOTO_READY] = true
          _state.value = CaptureUiState(photo = PhotoState.Ready(imagePrep.loadThumbnail()))
        }
        is PhotoOutcome.Failed -> {
          savedState[KEY_PHOTO_READY] = false
          _state.value = CaptureUiState(photo = PhotoState.Empty, error = outcome.error)
        }
      }
    }
  }

  private fun showPreparedPhoto() {
    _state.value = CaptureUiState(photo = PhotoState.Ready(thumbnail = null))
    viewModelScope.launch {
      _state.value = CaptureUiState(photo = PhotoState.Ready(imagePrep.loadThumbnail()))
    }
  }

  private companion object {
    const val KEY_PHOTO_READY = "photoReady"
    const val KEY_PENDING_CAMERA_URI = "pendingCameraUri"
  }
}
