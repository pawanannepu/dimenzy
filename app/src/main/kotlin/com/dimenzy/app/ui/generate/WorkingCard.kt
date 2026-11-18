package com.dimenzy.app.ui.generate

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dimenzy.app.ui.common.sharedPhoto
import com.dimenzy.app.ui.common.sharedPhotoCard

/**
 * The generating card: the photo being sent as a thumbnail, what is happening, and progress.
 * Progress is indeterminate: the API reports none, so the bar never pretends to know how far along it is.
 */
@Composable
internal fun WorkingCard(thumbnail: Bitmap?, modifier: Modifier = Modifier) {
  Surface(
    // The capture screen's photo panel shrinks into this card, and this card grows into the preview
    modifier = modifier.sharedPhotoCard().fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Thumbnail(thumbnail)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text("Making your 3D model", style = MaterialTheme.typography.titleMedium)
          Text(
            "Usually under a minute",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      LinearProgressIndicator(
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Making your 3D model" },
      )
    }
  }
}

/** 72dp, rounded; a plain tile while the photo loads, so the text never shifts. The photo is the panel's, shrunk. */
@Composable
private fun Thumbnail(thumbnail: Bitmap?) {
  val shape = RoundedCornerShape(12.dp)
  if (thumbnail == null) {
    Box(Modifier.size(72.dp).clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHighest))
  } else {
    val image = remember(thumbnail) { thumbnail.asImageBitmap() }
    Image(
      bitmap = image,
      contentDescription = null,
      contentScale = ContentScale.Crop,
      modifier = Modifier.size(72.dp).sharedPhoto().clip(shape),
    )
  }
}
