package com.dimenzy.app.image

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * The one file the system camera writes into: `cacheDir/photos/capture.jpg`, overwritten each shot,
 * shared through the app's private FileProvider (only `cacheDir/photos/` is exposed).
 */
class CameraShots(private val context: Context) {

  fun outputUri(): Uri {
    val dir = File(context.cacheDir, "photos").apply { mkdirs() }
    val file = File(dir, "capture.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
  }
}
