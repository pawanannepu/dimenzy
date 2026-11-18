package com.dimenzy.app.ui.preview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.dimenzy.app.data.ModelStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What to load: [location] is `null` for an unknown id or a missing file. [reloadKey] restarts the load.
 * [animation] is the clip to loop (`null` plays the first one, if any). */
data class PreviewTarget(val location: String?, val reloadKey: Int, val animation: String? = null)

/** Resolves the route's `modelId` to a file through [ModelStore]; Retry resolves it again and reloads. */
class PreviewViewModel(
  savedState: SavedStateHandle,
  private val store: ModelStore,
) : ViewModel() {

  private val modelId: String = savedState.get<String>(ARG_MODEL_ID).orEmpty()

  private val _target = MutableStateFlow(PreviewTarget(store.locationFor(modelId), reloadKey = 0, store.animationFor(modelId)))
  val target: StateFlow<PreviewTarget> = _target.asStateFlow()

  fun retry() {
    _target.value = PreviewTarget(store.locationFor(modelId), _target.value.reloadKey + 1, store.animationFor(modelId))
  }

  companion object {
    const val ARG_MODEL_ID = "modelId"
  }
}
