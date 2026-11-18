package com.dimenzy.app.data

/**
 * A model bundled in the APK under `assets/`, offered by "Try a sample model".
 * [animation] names the clip to loop; `null` plays the file's first clip (if it has any).
 */
data class SampleModel(
  val id: String,
  val name: String,
  val description: String,
  val asset: String,
  val animation: String? = null,
)

/**
 * The bundled showcase models, in picker order. Ids are fixed strings that never parse as a UUID,
 * so they can't collide with a generated model's id. The originals live in the repo's `assets/`;
 * these copies are shrunk with gltf-transform (Draco geometry, textures at most 2048 px).
 */
val SAMPLE_MODELS: List<SampleModel> = listOf(
  SampleModel("sample", "Avocado", "A small, simple object", "sample.glb"),
  SampleModel("sample-wolf", "Wolf", "Animated, breathing idle", "samples/wolf.glb", animation = "04_Idle"),
  SampleModel("sample-drone", "Mech drone", "Animated, hovering", "samples/drone.glb"),
  SampleModel("sample-fridge", "Fridge", "A household object", "samples/fridge.glb"),
  SampleModel("sample-rain", "After the rain", "A whole scene, slower to load", "samples/rain.glb"),
)
