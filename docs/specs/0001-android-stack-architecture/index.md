# 0001. Native Android stack with SceneView AR and Stability AI Stable Fast 3D

**Date**: 2026-11-18
**Status**: Accepted

## Summary

Dimenzy is a native Android app written in Kotlin, with Jetpack Compose screens and SceneView (a 3D and AR library built on Google's ARCore) for the preview and the room placement. Photos go straight from the phone to Stability AI's Stable Fast 3D service, which returns a ready to use 3D file (GLB) in one request. There is no server of your own and no accounts. A bundled sample model keeps the AR demo working when credits or wifi run out.

## Decision

**Chosen option**: Option 1: Native Kotlin, Compose, SceneView, and Stable Fast 3D called directly from the app.

Build a single module Kotlin app (Compose and Material 3, light MVVM, Navigation Compose) that uses SceneView for both the 3D preview and ARCore placement, and calls Stability AI Stable Fast 3D directly over HTTPS with Retrofit and OkHttp.

**Implementation skills**: `sceneview` (`sceneview/sceneview`, `.claude/skills/sceneview/`, authoritative for every SceneView API call) · `agp-9-upgrade` · `edge-to-edge` · `android-permissions-security` · `android-intent-security` · `r8-analyzer` · `android-cli` · `jetpack-compose-performance` (all `android/skills`, `.claude/skills/<name>/`)

## Rationale

Reasoning and options: see [rationale.md](rationale.md).

## Proposed stack

| Layer | Choice | Reason |
|---|---|---|
| Platform | Android only, `minSdk 24`, `targetSdk` and `compileSdk` = latest stable that the chosen SceneView release supports | API 24 is ARCore's own floor, so every AR capable phone can install it. |
| Language | Kotlin, coroutines plus `StateFlow` | First class on Android, and coroutines fit a long network call cleanly. |
| UI | Jetpack Compose with Material 3, portrait locked | Least code for simple clean screens. SceneView ships Compose composables. Portrait avoids AR session restarts on rotation. |
| Architecture | One `app` module, light MVVM: one `ViewModel` per screen, a `ModelRepository`, a hand written `AppContainer` in the `Application` class (no DI framework) | Enough structure to keep state across rotation and process events, without Hilt setup time. |
| Navigation | Navigation Compose, routes in *Screen flow* below | Real back stack, and an easy place to add the library screen (feature 6). |
| Photo input | `ActivityResultContracts.TakePicture` (system camera, writing to a `FileProvider` URI, authority `${applicationId}.fileprovider`, `res/xml/file_paths.xml` with `<cache-path name="photos" path="photos/"/>`) plus `PickVisualMedia` (Photo Picker) | About 20 lines, and the gallery side needs no storage permission. |
| Image prep | Read the bounds only (`inJustDecodeBounds`), decode with `inSampleSize` to about 2048 px, apply EXIF rotation (`androidx.exifinterface`), scale so the long side is 1024 px, encode JPEG quality 90 to `cacheDir/upload.jpg` | Never fully decodes a 12 to 50 MP photo (no out of memory crash). Stays far under the 10 MiB limit. Retry reuses this file. |
| AI service | Stability AI Stable Fast 3D, `POST https://api.stability.ai/v2beta/3d/stable-fast-3d` | Sync call that returns the GLB bytes directly; 10 credits per successful run, failed runs are free. |
| HTTP | Retrofit plus OkHttp, no JSON converter. One `@Multipart @Streaming` call returns `ResponseBody`, which is copied to a file. Parse the rare error body with the built in `org.json` | Your chosen stack. `@Streaming` keeps the GLB out of memory, and skipping kotlinx.serialization drops a Gradle plugin that one endpoint doesn't need. |
| API key | `STABILITY_API_KEY` in `local.properties`, read in `app/build.gradle.kts` into `BuildConfig.STABILITY_API_KEY` | No server needed. The key is extractable from the APK, which is accepted for a college demo. |
| 3D preview | SceneView 4.x `SceneView { }` composable with `ModelNode(scaleToUnits = 1f)` and the default orbit camera | Same library as AR, and the same `rememberModelInstance` loader. |
| AR | SceneView 4.x `ARSceneView { }` composable (`io.github.sceneview:arsceneview:4.69.6`, which includes the 3D module), ARCore in **optional** mode. Tap to place is hand written: `onSingleTapConfirmed` → `frame.hitTest(event)` → `hit.createAnchor()` → `AnchorNode(anchor) { ModelNode(scaleToUnits = 0.4f, isEditable = true) }`. Keep one anchor, so a new tap replaces it | Plane rendering and pinch and rotate gestures (`isEditable`) are built in. Follow the `sceneview` skill's AR tap to place recipe and don't invent API names. Default 0.4 m size; feature 7 overrides it. |
| Model storage | `filesDir/models/<uuid>.glb` | Private, survives restarts, ready for the saved library (feature 6) with no migration. |
| Demo fallback | A small CC0 licensed GLB in `app/src/main/assets/sample.glb`, behind a "Try sample model" button | Protects the demo and lets you test AR without spending credits. |
| Persistence (index) | None now. Feature 6 decides how to list saved models | Nothing in Release 1 needs a database. |
| Observability | Logcat via `android.util.Log`, tag `Dimenzy`; log HTTP status and timing, never the key | Prototype tier; you watch Logcat over USB. No crash reporting service. |
| Build | Gradle Kotlin DSL with a version catalog (`gradle/libs.versions.toml`), JDK 21, Android Studio "Empty Activity" (Compose) template as the starting point, `buildFeatures { compose = true; buildConfig = true }` | Today's default template. AGP 8 and newer don't generate `BuildConfig` unless you ask. |
| Release | Signed release APK from a local keystore (feature 5), `isMinifyEnabled = false` | R8 shrinking can break Filament, Retrofit, and serialization without keep rules; skipping it removes a risk you have no time to debug. |

### Service contract (Stable Fast 3D)

- **Request**: `multipart/form-data`, header `authorization: Bearer <STABILITY_API_KEY>`. Field `image` (required; each side at least 64 px; whole request at most 10 MiB). Optional fields stay at their defaults: `texture_resolution` `1024`, `foreground_ratio` `0.85`, `remesh` `none`, `vertex_count` `-1`.
- **Success**: `200` with the GLB binary as the body. Stream it to `filesDir/models/<uuid>.glb`.
- **Errors** (map each to a clear message with Retry):

| Response | Meaning | Message intent |
|---|---|---|
| `400`, `422` | Bad or unusable image | "That photo couldn't be used. Try another." |
| `401` | Missing or wrong key | "API key problem. Check `local.properties`." |
| `402` | Out of credits | "Out of AI credits. Try the sample model." |
| `403` | Content moderation flag | "The service refused this image." |
| `413` | Over 10 MiB | Should not happen after image prep; show the generic error. |
| `429` | Over 150 requests per 10 s | "Too many requests. Wait a minute." |
| `5xx` | Service error | "The AI service had a problem. Retry." |
| `SocketTimeoutException` or `InterruptedIOException` (catch these first) | Over 90 s | "Generation timed out. Retry." |
| Other `IOException` | No network | "No internet connection." |
| Any other status | Unexpected | "Something went wrong. Retry." Log the code. |

### Screen flow and model handoff

- Routes: `capture` → `generate` → `preview/{modelId}` → `ar/{modelId}`. "Try sample model" on `capture` goes straight to `preview/sample`.
- `modelId` is the UUID of `filesDir/models/<modelId>.glb`, and it resolves to the location `file://<absolute path>`. The id `sample` resolves to the asset location `sample.glb`. Both go through the same `rememberModelInstance(modelLoader, location)` call.
- Only the `modelId` string crosses screens. Each 3D screen creates its own `rememberEngine` / `rememberModelLoader` / model instance. Never pass a `ModelInstance` or `Engine` between screens.
- `generate` receives nothing by route. It reads the prepared `cacheDir/upload.jpg` that `capture` wrote. Retry resends that same file.

### Key invariants

- The OkHttp client for the generate call sets connect, read, write, and call timeouts to 90 s. The read timeout defaults to 10 s and would otherwise fail every slow generation. The UI shows an indeterminate progress indicator while the call runs.
- The pending `TakePicture` URI is kept in `SavedStateHandle`, so the photo survives Android killing the app while the system camera is open.
- The generate call runs in `viewModelScope`, so rotation or a recomposition never starts a second paid request. Retry is the only way to resend.
- A GLB is written to a temp file and renamed into `filesDir/models/` only after the body is fully read, so a broken download never leaves a half file that looks valid.
- The manifest declares `android.permission.CAMERA` (needed by ARCore). Because it is declared, Android refuses the system camera intent until it is granted, so request `CAMERA` at runtime before launching `TakePicture`, not just before AR.
- ARCore is optional: manifest `<meta-data android:name="com.google.ar.core" android:value="optional" />` and `<uses-feature android:name="android.hardware.camera.ar" android:required="false" />`. Before opening AR, call `ArCoreApk.getInstance().checkAvailability(...)`, and check again about every 200 ms while it returns a transient result (`UNKNOWN_CHECKING`). Show "AR isn't supported on this phone" only for `UNSUPPORTED_DEVICE_NOT_CAPABLE`, and keep the 3D preview usable. If ARCore is supported but not installed or outdated, call `requestInstall` (handling the `onResume` re-entry). If the AR session still fails to start, show the same message instead of crashing.
- `STABILITY_API_KEY` is never logged and never committed. If it is blank at build time, the app still builds, and generation fails with the `401` message.
- Application ID and namespace: `com.dimenzy.app`.

### Package layout

```
com.dimenzy.app
  DimenzyApp.kt          Application + AppContainer
  MainActivity.kt        NavHost
  data/                  StabilityApi (Retrofit), ModelRepository, GenerateError
  image/                 ImagePrep (EXIF rotate, downscale, JPEG)
  ui/capture/            take or pick photo
  ui/generate/           progress, error, retry
  ui/preview/            SceneView Scene
  ui/ar/                 SceneView ARScene, AR availability check
```

### Configuration required

- `STABILITY_API_KEY` in `local.properties` (git ignored): your Stability AI key from the dashboard.
- Release signing values (feature 5): `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` in `local.properties`.

### Version pinning rule

Pin `io.github.sceneview:arsceneview:4.69.6`. It's the version the installed `sceneview` skill documents, so the skill's API names match. Set AGP, Kotlin, `compileSdk`, and the JDK to what 4.69.6 requires (its README reportedly says AGP 9.4 or newer and JVM 21, which is unconfirmed, so read the release notes, and use the `agp-9-upgrade` skill if needed). If your installed Android Studio can't build it, fall back to the newest SceneView release that is at least 2 weeks old and builds, and check the skill's `references/migration.md` for any API differences. Don't upgrade tools mid sprint.

## Consequences

**Positive**:
- One language and one 3D library cover preview and AR, and one loader call handles both asset and file models.
- No backend to deploy or keep alive; the whole system is the APK plus one HTTPS endpoint.
- The model storage layout and Navigation setup already fit features 6 and 7 without rework.

**Negative / tradeoffs**:
- The API key ships inside the APK. Anyone with the APK can extract and spend your credits. Rotate the key after the demo.
- Only about 2 generations fit the 25 free credits. You will likely add paid credits ($10 is about 100 runs), so use the sample model while testing AR.
- One provider, no fallback service. If Stability is down at demo time, only the sample model and previously generated files work.
- Android only. An iOS version would mean a rewrite.
- No R8 shrinking, so the release APK is larger than it needs to be.

**Neutral**:
- Model quality depends on a clean, single object photo with a plain background; there is no cutout step (deferred in scope).
- The OpenRouter credits you hold aren't used. OpenRouter's model list (checked 2026-11-18) had no image to 3D model.

## Follow-up

- [ ] Before scaffolding, confirm the AGP, Kotlin, and JDK requirements of `arsceneview:4.69.6` in its release notes.
- [ ] Install or update "Google Play Services for AR" on the demo phone before the demo. A clean phone needs it from the Play Store, over a network connection.
- [ ] Root `AGENTS.md` `## Agent skills` (written by `/audit` or `/sync`) should list the 8 installed skills in `.claude/skills/`, plus a `Declined:` line for the MCP servers offered on 2026-11-18 (android-mcp-server, mcp-stable-fast-3d).
- [ ] Create a Stability AI account, generate an API key, and decide whether to add paid credits before the demo.
- [ ] Pick the bundled CC0 sample GLB (for example a model from the Khronos glTF Sample Assets set whose license file says CC0) and confirm its license when downloading.
- [ ] Rotate or revoke the Stability API key after the demo, since it is extractable from the APK.
- [ ] Record the stack, the build and install commands (`./gradlew installDebug`), and the `local.properties` keys in root `AGENTS.md` (feature 2, `/audit`).
