package com.dimenzy.app.image

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Turns any picked or captured photo into the one upload file (spec 0001 steps):
 * read bounds, subsample to about 2048 px, apply the EXIF rotation, scale the long side to 1024 px, JPEG 90.
 */
class ImagePrep(
  private val resolver: ContentResolver,
  /** `cacheDir/upload.jpg`, the only file Generate and Retry read. */
  val preparedPhoto: File,
) {

  fun hasPreparedPhoto(): Boolean = preparedPhoto.isFile

  suspend fun prepare(source: Uri): PhotoOutcome = withContext(Dispatchers.IO) {
    try {
      val bitmap = decodeUpright(source) ?: return@withContext PhotoOutcome.Failed(PhotoError.Unreadable)
      writeAtomically(bitmap)
      bitmap.recycle()
      PhotoOutcome.Ready
    } catch (e: IOException) {
      unreadable(e)
    } catch (e: SecurityException) {
      unreadable(e)
    } catch (e: IllegalArgumentException) {
      unreadable(e)
    }
  }

  private fun unreadable(e: Exception): PhotoOutcome {
    Log.w(TAG, "photo prep failed: ${e.javaClass.simpleName}")
    return PhotoOutcome.Failed(PhotoError.Unreadable)
  }

  /** The prepared photo at half size for the capture screen, or `null` if it's missing. */
  suspend fun loadThumbnail(): Bitmap? = withContext(Dispatchers.IO) {
    if (!preparedPhoto.isFile) return@withContext null
    BitmapFactory.decodeFile(preparedPhoto.path, BitmapFactory.Options().apply { inSampleSize = 2 })
  }

  private fun decodeUpright(source: Uri): Bitmap? {
    // A bounds only decode always returns null; the size comes back in `bounds`, so map the stream to Unit.
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds); Unit } ?: return null
    val longSide = maxOf(bounds.outWidth, bounds.outHeight)
    if (longSide <= 0) {
      Log.w(TAG, "photo prep: unreadable image bounds")
      return null
    }

    var sample = 1
    while (longSide / (sample * 2) >= DECODE_TARGET) sample *= 2
    val decoded = resolver.openInputStream(source)?.use {
      BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: return null

    val matrix = Matrix()
    val decodedLong = maxOf(decoded.width, decoded.height)
    if (decodedLong > UPLOAD_LONG_SIDE) {
      val scale = UPLOAD_LONG_SIDE.toFloat() / decodedLong
      matrix.postScale(scale, scale)
    }
    applyExifOrientation(matrix, readOrientation(source))
    if (matrix.isIdentity) return decoded

    val upright = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
    if (upright !== decoded) decoded.recycle()
    return upright
  }

  private fun readOrientation(source: Uri): Int =
    resolver.openInputStream(source)?.use {
      ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    } ?: ExifInterface.ORIENTATION_NORMAL

  private fun applyExifOrientation(matrix: Matrix, orientation: Int) {
    when (orientation) {
      ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
      ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
      ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
      ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
      ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
      ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
      ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
    }
  }

  /** Writes beside the target and renames, so a half written upload is never left behind. */
  private fun writeAtomically(bitmap: Bitmap) {
    val tmp = File(preparedPhoto.path + ".tmp")
    val ok = tmp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
    if (!ok || !tmp.renameTo(preparedPhoto)) {
      tmp.delete()
      throw IOException("Couldn't save the prepared photo")
    }
  }

  private companion object {
    const val TAG = "Dimenzy"
    const val DECODE_TARGET = 2048
    const val UPLOAD_LONG_SIDE = 1024
    const val JPEG_QUALITY = 90
  }
}
