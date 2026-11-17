package com.dimenzy.app.ui.generate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dimenzy.app.data.GenerateError

/** Generate: progress with Cancel while the call runs, or the error with its next steps. */
@Composable
fun GenerateScreen(
  viewModel: GenerateViewModel,
  onDone: (modelId: String) -> Unit,
  onBack: () -> Unit,
  onTrySample: () -> Unit,
) {
  // Collected with the lifecycle, so the jump to the preview only happens while this screen is visible.
  val state by viewModel.state.collectAsStateWithLifecycle()
  LaunchedEffect(state) {
    val done = state as? GenerateUiState.Done
    if (done != null) onDone(done.modelId)
  }

  Scaffold { innerPadding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      when (val current = state) {
        GenerateUiState.Working, is GenerateUiState.Done -> Working(onCancel = onBack)
        is GenerateUiState.Failed -> Failed(current.error, viewModel::retry, onBack, onTrySample)
      }
    }
  }
}

@Composable
private fun Working(onCancel: () -> Unit) {
  CircularProgressIndicator(modifier = Modifier.size(56.dp))
  Text(
    "Making your 3D model. This usually takes under a minute.",
    style = MaterialTheme.typography.titleMedium,
    textAlign = TextAlign.Center,
  )
  Spacer(Modifier.height(8.dp))
  OutlinedButton(onClick = onCancel) { Text("Cancel") }
}

@Composable
private fun Failed(
  error: GenerateError,
  onRetry: () -> Unit,
  onChooseAnother: () -> Unit,
  onTrySample: () -> Unit,
) {
  Text("Couldn't make the model", style = MaterialTheme.typography.headlineSmall)
  Text(
    error.message(),
    style = MaterialTheme.typography.bodyLarge,
    textAlign = TextAlign.Center,
    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
  )
  Spacer(Modifier.height(8.dp))
  Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Retry") }
  OutlinedButton(onClick = onChooseAnother, modifier = Modifier.fillMaxWidth()) { Text("Choose another photo") }
  if (error == GenerateError.OutOfCredits) {
    TextButton(onClick = onTrySample) { Text("Try sample model") }
  }
}
