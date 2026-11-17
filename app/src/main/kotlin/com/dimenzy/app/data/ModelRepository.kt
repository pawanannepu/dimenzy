package com.dimenzy.app.data

import android.os.SystemClock
import android.util.Log
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InterruptedIOException
import java.io.OutputStream
import java.net.SocketTimeoutException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

/**
 * Sends the prepared photo to Stable Fast 3D once and saves the GLB.
 * A `.glb` in `filesDir/models/` is always complete: the body streams into `.tmp` and is renamed at the end.
 */
class ModelRepository(
  private val api: StabilityApi,
  private val store: ModelStore,
  private val preparedPhoto: File,
  private val apiKey: String,
) {

  suspend fun generate(): GenerateOutcome = withContext(Dispatchers.IO) {
    if (!preparedPhoto.isFile) return@withContext GenerateOutcome.Failure(GenerateError.MissingPhoto)

    val files = store.newModelFiles()
    val startedAt = SystemClock.elapsedRealtime()
    var saved = false
    try {
      val image = MultipartBody.Part.createFormData(
        "image",
        "upload.jpg",
        preparedPhoto.asRequestBody("image/jpeg".toMediaType()),
      )
      val response = api.generate("Bearer $apiKey", image)
      val body = response.body()
      if (!response.isSuccessful || body == null) {
        response.errorBody()?.close()
        logResult("status=${response.code()}", startedAt, bytes = 0)
        return@withContext GenerateOutcome.Failure(errorFor(response.code()))
      }

      val bytes = body.use { source -> files.tmp.outputStream().use { sink -> copyCancellable(source.byteStream(), sink) } }
      if (!files.tmp.renameTo(files.glb)) throw IOException("Couldn't finish saving the model")
      saved = true
      logResult("status=${response.code()}", startedAt, bytes)

      copyPhotoBestEffort(files.jpg)
      GenerateOutcome.Success(files.modelId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: SocketTimeoutException) {
      fail(e, startedAt, GenerateError.Timeout)
    } catch (e: InterruptedIOException) {
      fail(e, startedAt, GenerateError.Timeout)
    } catch (e: IOException) {
      fail(e, startedAt, GenerateError.NoNetwork)
    } catch (e: Exception) {
      fail(e, startedAt, GenerateError.Unexpected(code = null))
    } finally {
      if (!saved) files.tmp.delete()
    }
  }

  /** Copies in chunks and stops as soon as the coroutine is cancelled, closing the connection via `use`. */
  private suspend fun copyCancellable(input: InputStream, output: OutputStream): Long {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
      currentCoroutineContext().ensureActive()
      val read = input.read(buffer)
      if (read < 0) return total
      output.write(buffer, 0, read)
      total += read
    }
  }

  /** The photo is only for feature 6 thumbnails, so a failed copy never fails the generation. */
  private fun copyPhotoBestEffort(target: File) {
    try {
      preparedPhoto.copyTo(target, overwrite = true)
    } catch (e: IOException) {
      Log.w(TAG, "photo copy failed: ${e.javaClass.simpleName}")
    }
  }

  private fun fail(e: Exception, startedAt: Long, error: GenerateError): GenerateOutcome {
    // Exception class only: messages are never needed here, and the key must never reach Logcat.
    logResult("error=${e.javaClass.simpleName}", startedAt, bytes = 0)
    return GenerateOutcome.Failure(error)
  }

  private fun logResult(what: String, startedAt: Long, bytes: Long) {
    Log.i(TAG, "generate $what ms=${SystemClock.elapsedRealtime() - startedAt} bytes=$bytes")
  }

  private fun errorFor(code: Int): GenerateError = when (code) {
    400, 422 -> GenerateError.BadImage
    401 -> GenerateError.BadKey
    402 -> GenerateError.OutOfCredits
    403 -> GenerateError.Refused
    429 -> GenerateError.RateLimited
    in 500..599 -> GenerateError.Server
    else -> GenerateError.Unexpected(code)
  }

  private companion object {
    const val TAG = "Dimenzy"
  }
}
