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
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Zoom out / zoom in for the placed model. The track is logarithmic, so 100% sits in the middle and
 * halving or doubling the size takes the same travel. [factor] is relative to the start size.
 */
@Composable
internal fun SizeSlider(factor: Float, onFactorChange: (Float) -> Unit, modifier: Modifier = Modifier) {
  val percent = (factor * 100).roundToInt()
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
      Icon(painterResource(R.drawable.ic_zoom_out), contentDescription = null)
      Slider(
        value = log2(factor),
        onValueChange = { onFactorChange(2f.pow(it)) },
        valueRange = log2(MIN_SIZE_FACTOR)..log2(MAX_SIZE_FACTOR),
        modifier = Modifier
          .weight(1f)
          .semantics {
            contentDescription = "Model size"
            stateDescription = "$percent percent"
          },
        colors = SliderDefaults.colors(
          thumbColor = Color.White,
          activeTrackColor = Color.White,
          inactiveTrackColor = Color.White.copy(alpha = 0.3f),
        ),
      )
      Icon(painterResource(R.drawable.ic_zoom_in), contentDescription = null)
      Text(
        "$percent%",
        modifier = Modifier.widthIn(min = 44.dp),
        style = MaterialTheme.typography.labelLarge,
        textAlign = TextAlign.End,
      )
    }
  }
}
