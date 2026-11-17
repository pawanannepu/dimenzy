# 0001. Rationale: native Android stack with SceneView AR and Stable Fast 3D

Decision record for [index.md](index.md). The build doesn't need this file.

## Context

Dimenzy turns one photo of an object into a 3D model and places that model in a real room through the phone camera. It is a college project with about 2 hours of total build time, a single developer, Android only, no accounts, and a release APK as the deliverable demoed on your own phone. The build approach is Skateboard: the thinnest working photo to 3D to AR loop first, then grow it.

Three forces shape the stack. First, AR is the riskiest part: plane detection, anchoring, and gestures must work on day one, so the framework must have mature, maintained ARCore support. Second, image to 3D must be a hosted AI service with a usable free or cheap tier, callable from a phone over plain HTTPS, ideally returning GLB (the binary glTF 3D format) that the AR library loads directly. Third, the time budget rules out anything that adds a second deployable or a long setup.

Not deciding means scaffolding on a framework whose AR support may fail late, or wiring a service whose free tier blocks API calls, and both would surface only after much of the 2 hours is gone.

## Options considered

### Option 1: Native Kotlin, Compose, SceneView, Stable Fast 3D direct (chosen)

Kotlin with Jetpack Compose. SceneView (built on Filament and ARCore) handles both the 3D preview and AR. The app calls Stability AI Stable Fast 3D directly, with the key in `BuildConfig`.

**Pros**:
- First class ARCore access, plus SceneView's Compose composables for tap to place, pinch, and rotate.
- One sync HTTP call returns the GLB. No polling and no server.

**Cons**:
- The API key is extractable from the APK.
- Android only, and Kotlin and Gradle setup is slower than JS tooling if you're new to them.

### Option 2: Flutter with an AR plugin, same service

Dart UI with a community AR plugin (for example `ar_flutter_plugin`) that wraps ARCore.

**Pros**:
- Fast UI iteration with hot reload.
- Cross platform in principle.

**Cons**:
- Flutter's AR plugins are thinly maintained and lag behind ARCore releases. The AR feature is the one most likely to break, and it's the one you can't afford to debug.

### Option 3: Unity with AR Foundation, same service

A Unity scene with AR Foundation for placement, plus a UI built in Unity.

**Pros**:
- The most capable AR and 3D tooling, including runtime glTF loading through packages.

**Cons**:
- Camera, gallery, HTTP, and UI flows are clunky in Unity. The build and editor setup, plus runtime GLB import, are heavy for 2 hours.

### Option 4: Option 1 plus a small proxy server for the key

The same app, but calls go through a free serverless function (for example a Cloudflare Worker) that holds the key.

**Pros**:
- The key never ships in the APK.

**Cons**:
- A second thing to write, deploy, and keep alive, roughly 20 to 30 minutes, plus one more failure point during the demo.

## Rationale

Option 1 wins because it puts the most mature AR path (ARCore through SceneView) behind the least setup, and AR is the highest risk part of the 2 hour budget. Flutter saves UI time but moves the risk into the AR plugin, where it hurts most. Unity is excellent at AR but loses the time back on the plain app parts (photo input, HTTP, screens), which make up most of Dimenzy's code.

Calling the service directly (over Option 4) is a conscious trade: for a demo on your own phone, the leaked key risk is small and can be fixed by rotating the key afterward. A proxy is about 25 minutes of a 2 hour budget for protection the demo doesn't need. If the APK will ever be shared publicly, add the proxy first.

Stable Fast 3D was chosen after a landscape check on 2026-11-18. Meshy's API returns `402` on its free plan, and Tripo's API is billed per use and leans on webhooks, which need a server. Hugging Face Spaces are free but best effort (a 5 minute daily GPU quota on a free account, plus queueing and sleeping Spaces), which is fragile for a live demo. Stable Fast 3D is the only option with a documented sync request that returns GLB bytes directly: the least code and the fewest failure states. Its free trial is only about 2 runs, which the bundled sample model and a small credit top up cover. OpenRouter was checked because you hold credits there, but its model list had no image to 3D model.

The smaller calls go the same way. API 24 is ARCore's floor, so it costs nothing. Light MVVM without Hilt keeps one paid request alive across rotation without DI setup. The system camera plus Photo Picker avoids building CameraX. `filesDir` storage and Navigation Compose are what features 6 and 7 need anyway, so nothing gets redone later.

## Landscape evidence (checked 2026-11-18)

| Service | API on free tier | Output | Flow | Notes |
|---|---|---|---|---|
| Stability AI Stable Fast 3D | 25 trial credits (about 2 runs at 10 credits) | GLB binary | Sync POST | 1 credit = $0.01. Failed runs are not charged. 150 requests per 10 s limit. |
| Stability AI SPAR3D | Same trial credits (about 6 runs at 4 credits) | Not confirmed | Not confirmed | A runner up, with the same account and key. |
| Meshy | No (`402` on free) | GLB, FBX, OBJ, USDZ | Async poll | Needs a paid plan. |
| Tripo | Not on free (per third party sources) | GLB, FBX, OBJ, USDZ | Webhook | About $0.20 to $0.30 per task. Free plan output is CC BY 4.0. |
| Hugging Face Spaces (TRELLIS, Hunyuan3D, and others) | Yes, best effort | Varies by Space | Gradio queue | ZeroGPU quota: 2 min anonymous, 5 min free account, 40 min PRO, per day. |
| OpenRouter | n/a | No 3D output modality | n/a | The live model list had 474 models, with outputs limited to text, image, audio, and video. |

SceneView: v4.69.6 released 2026-11-17 per the GitHub releases page. The artifact `io.github.sceneview:arsceneview` includes the 3D module, supports Compose, and has `minSdk 24`. The AGP 9.4 and JVM 21 requirements come from the README via search and aren't confirmed.

## References

**Project sources**:
- `docs/scope/scope.md`, feature 1 (Stack & architecture), plus the constraints line (2 hours, Android only, free tier AI, ARCore) and the Skateboard approach.
- Installed `sceneview` skill (`.claude/skills/sceneview/`): SceneView 4.69.6 composable names, `rememberModelInstance` for asset and `file://` locations, the AR tap to place recipe.
- The same model cross check of this spec on 2026-11-18. Its accepted fixes covered timeouts, `BuildConfig`, the screen handoff, AR scale, camera URI survival, and `@Streaming`.

**Practices & standards**:
- Monolith first, boring technology: one module, no backend.
- Atomic file writes (temp file then rename) for downloaded artifacts.
- Never commit secrets: keep the key in `local.properties`, and rotate it after exposure.

**Links** (web verified during this design session):
- Stability AI, Stable Fast 3D API: https://docs.stability.ai/api-reference/3d/stable-fast-3d.md
- Stability AI, API reference (auth, errors, rate limits): https://docs.stability.ai/api-reference
- Stability AI, pricing: https://docs.stability.ai/pricing
- Meshy, image to 3D API: https://docs.meshy.ai/en/api/image-to-3d
- Tripo developer platform: https://developers.tripo3d.ai/
- Hugging Face, ZeroGPU Spaces: https://huggingface.co/docs/hub/spaces-zerogpu
- SceneView Android releases: https://github.com/SceneView/sceneview-android/releases
- OpenRouter model list API (queried live): https://openrouter.ai/api/v1/models
