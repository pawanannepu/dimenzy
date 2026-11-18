package com.dimenzy.app.ui.ar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dimenzy.app.R
import kotlin.math.roundToInt

/**
 * Turn left / turn right for the placed model, styled like [SizeSlider]. 0° (the facing it landed with)
 * sits in the middle; the ends are half a turn either way.
 */
@Composable
internal fun RotationSlider(degrees: Float, onDegreesChange: (Float) -> Unit, modifier: Modifier = Modifier) {
  val shown = degrees.roundToInt()
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    color = CameraScrim,
    contentColor = Color.White,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(painterResource(R.drawable.ic_rotate_left), contentDescription = null)
      Slider(
        value = degrees,
        onValueChange = onDegreesChange,
        valueRange = MIN_ROTATION_DEGREES..MAX_ROTATION_DEGREES,
        modifier = Modifier
          .weight(1f)
          .semantics {
            contentDescription = "Model rotation"
            stateDescription = "$shown degrees"
          },
        colors = SliderDefaults.colors(
          thumbColor = Color.White,
          activeTrackColor = Color.White,
          inactiveTrackColor = Color.White.copy(alpha = 0.3f),
        ),
      )
      Icon(painterResource(R.drawable.ic_rotate_right), contentDescription = null)
      Text(
        "$shown°",
        modifier = Modifier.widthIn(min = 44.dp),
        style = MaterialTheme.typography.labelLarge,
        textAlign = TextAlign.End,
      )
    }
  }
}
