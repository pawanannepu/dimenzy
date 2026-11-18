package com.dimenzy.app.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dimenzy.app.data.SAMPLE_MODELS

/** A bottom sheet listing the bundled models. [onPick] gets the chosen model's id. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SamplePickerSheet(onPick: (modelId: String) -> Unit, onDismiss: () -> Unit) {
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
      Text(
        "Choose a sample model",
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
      )
      SAMPLE_MODELS.forEach { sample ->
        ListItem(
          headlineContent = { Text(sample.name) },
          supportingContent = { Text(sample.description) },
          modifier = Modifier.clickable(role = Role.Button) { onPick(sample.id) },
        )
      }
    }
  }
}
