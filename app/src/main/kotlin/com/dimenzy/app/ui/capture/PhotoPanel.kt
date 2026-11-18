package com.dimenzy.app.ui.capture

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dimenzy.app.ui.common.sharedPhoto
import com.dimenzy.app.ui.common.sharedPhotoCard

/** The square frame above the buttons: an empty hint, a spinner while preparing, or the prepared photo. */
@Composable
fun PhotoPanel(photo: PhotoState, modifier: Modifier = Modifier) {
  Surface(
    // Tapping Generate shrinks this panel into the generating card; New photo brings the preview back into it
    modifier = modifier.sharedPhotoCard().fillMaxWidth().aspectRatio(1f),
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
  ) {
    Box(contentAlignment = Alignment.Center) {
      when (photo) {
        PhotoState.Empty -> EmptyHint()
        PhotoState.Preparing -> CircularProgressIndicator(
          modifier = Modifier.semantics { contentDescription = "Preparing photo" },
        )
        is PhotoState.Ready -> {
          val thumbnail = photo.thumbnail
          if (thumbnail == null) {
            CircularProgressIndicator()
          } else {
            val image = remember(thumbnail) { thumbnail.asImageBitmap() }
            Image(
              bitmap = image,
              contentDescription = "Your photo",
              contentScale = ContentScale.Fit,
              // The same photo becomes the generating card's thumbnail
              modifier = Modifier.fillMaxSize().padding(12.dp).sharedPhoto(),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun EmptyHint() {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
    modifier = Modifier.padding(32.dp),
  ) {
    Text("No photo yet", style = MaterialTheme.typography.titleMedium)
    Text(
      "Take a photo or choose one from your gallery. You'll see it here before anything is sent.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}
