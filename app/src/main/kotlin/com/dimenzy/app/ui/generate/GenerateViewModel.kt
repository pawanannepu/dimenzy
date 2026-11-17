package com.dimenzy.app.ui.generate

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dimenzy.app.data.GenerateError
import com.dimenzy.app.data.GenerateOutcome
import com.dimenzy.app.data.ModelRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface GenerateUiState {
  data object Working : GenerateUiState

  data class Done(val modelId: String) : GenerateUiState

  data class Failed(val error: GenerateError) : GenerateUiState
}

/**
 * Starts the paid call once, in `init` (the capture screen's button was the confirm).
 * Leaving the screen clears `viewModelScope`, which cancels the request.
 */
class GenerateViewModel(
  private val savedState: SavedStateHandle,
  private val repository: ModelRepository,
) : ViewModel() {

  private val _state = MutableStateFlow<GenerateUiState>(GenerateUiState.Working)
  val state: StateFlow<GenerateUiState> = _state.asStateFlow()

  private var job: Job? = null

  init {
    val result = savedState.get<String>(KEY_RESULT)
    when {
      // Finished while the app was in the background, then Android killed it: the model is already saved.
      result != null -> _state.value = GenerateUiState.Done(result)
      // Restored mid call: never resend a paid request without a tap.
      savedState.get<Boolean>(KEY_STARTED) == true -> _state.value = GenerateUiState.Failed(GenerateError.Interrupted)
      else -> start()
    }
  }

  /** The only way to resend: same prepared photo, one new request. */
  fun retry() {
    if (_state.value is GenerateUiState.Failed) start()
  }

  private fun start() {
    if (job?.isActive == true) return
    savedState[KEY_STARTED] = true
    _state.value = GenerateUiState.Working
    job = viewModelScope.launch {
      _state.value = when (val outcome = repository.generate()) {
        is GenerateOutcome.Success -> {
          savedState[KEY_RESULT] = outcome.modelId
          GenerateUiState.Done(outcome.modelId)
        }
        // `started` stays true: a restored error screen must ask again, never resend on its own.
        is GenerateOutcome.Failure -> GenerateUiState.Failed(outcome.error)
      }
    }
  }

  private companion object {
    const val KEY_STARTED = "started"
    const val KEY_RESULT = "result"
  }
}
