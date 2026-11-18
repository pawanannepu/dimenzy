package com.dimenzy.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Dimenzy's secondary action: an outlined button with its label in the primary color and an outline-colored
 * edge. Material's current default draws both in quieter grays, which read as disabled beside the filled button.
 */
@Composable
fun DimenzyOutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
  OutlinedButton(
    onClick = onClick,
    modifier = modifier,
    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    content = content,
  )
}
