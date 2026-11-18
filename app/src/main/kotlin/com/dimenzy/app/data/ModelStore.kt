package com.dimenzy.app.data

import java.io.File
import java.util.UUID

/** The files one generation writes: the download lands in [tmp] and is renamed to [glb] only when complete. */
data class ModelFiles(val modelId: String, val tmp: File, val glb: File, val jpg: File)

/**
 * The single mapping from a `modelId` to a model file (spec 0002).
 * Features 4 (AR) and 6 (library) resolve models through [locationFor] too.
 */
class ModelStore(private val modelsDir: File) {

  fun newModelFiles(): ModelFiles {
    val id = UUID.randomUUID().toString()
    val dir = modelsDir.apply { mkdirs() }
    return ModelFiles(
      modelId = id,
      tmp = File(dir, "$id.glb.tmp"),
      glb = File(dir, "$id.glb"),
      jpg = File(dir, "$id.jpg"),
    )
  }

  /**
   * An asset path for a bundled sample ([SAMPLE_MODELS]), a `file://` location for a saved model,
   * or `null` when the id is malformed or the file is missing. Only a canonical UUID is accepted
   * for saved models, so a route argument can never point outside `filesDir/models/`.
   */
  fun locationFor(modelId: String): String? {
    sampleFor(modelId)?.let { return it.asset }
    if (!isCanonicalUuid(modelId)) return null
    val file = File(modelsDir, "$modelId.glb")
    return if (file.isFile) "file://${file.absolutePath}" else null
  }

  /** The clip to loop for [modelId]; `null` means the file's first clip (generated models have none). */
  fun animationFor(modelId: String): String? = sampleFor(modelId)?.animation

  private fun sampleFor(modelId: String): SampleModel? = SAMPLE_MODELS.firstOrNull { it.id == modelId }

  private fun isCanonicalUuid(id: String): Boolean =
    try {
      UUID.fromString(id).toString() == id
    } catch (e: IllegalArgumentException) {
      false
    }
}
