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
   * `"sample.glb"` (an asset path) for [SAMPLE_ID], a `file://` location for a saved model,
   * or `null` when the id is malformed or the file is missing. Only a canonical UUID is accepted,
   * so a route argument can never point outside `filesDir/models/`.
   */
  fun locationFor(modelId: String): String? {
    if (modelId == SAMPLE_ID) return SAMPLE_ASSET
    if (!isCanonicalUuid(modelId)) return null
    val file = File(modelsDir, "$modelId.glb")
    return if (file.isFile) "file://${file.absolutePath}" else null
  }

  private fun isCanonicalUuid(id: String): Boolean =
    try {
      UUID.fromString(id).toString() == id
    } catch (e: IllegalArgumentException) {
      false
    }

  companion object {
    const val SAMPLE_ID = "sample"
    private const val SAMPLE_ASSET = "sample.glb"
  }
}
