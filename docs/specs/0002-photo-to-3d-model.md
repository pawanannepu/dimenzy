# 0002. Photo to 3D model: confirm, generate, and preview flow

**Date**: 2026-11-18
**Status**: Accepted

## Summary

You take or pick a photo, see it on the capture screen, and tap Generate. The app then sends it once to Stable Fast 3D, saves the returned 3D file (GLB) on the phone, and shows it in a 3D preview you can drag to spin. The extra confirm tap is there because every generation spends paid credits. Every failure becomes a clear message with Retry. A bundled sample model lets you test the preview, and later the AR, without spending anything. This spec covers the three screens (capture, generate, preview). The service, libraries, and file layout come from [spec 0001](0001-android-stack-architecture/index.md).

## Context

Feature 3 is the first half of the demo loop: a photo goes in and a 3D model you can see comes out. Spec 0001 already fixed the hard technical parts: Stable Fast 3D called straight from the app over Retrofit and OkHttp, the image prep steps, 90 s timeouts, the error table, the routes `capture → generate → preview/{modelId}`, and GLB files in `filesDir/models/`. What's still open is the behavior: when the paid call fires, what you see while it runs, how you back out, what each failure lets you do next, and how the preview reports a file that won't load.

Credits drive most of these choices. The free tier covers about 2 generations (10 credits each, and failed runs are free). A flow that fires on every photo pick, or that resends the request after rotation or after Android restores a killed app, burns the demo budget without you noticing. The network call also takes 10 to 60 s, which is long enough that you will switch apps, rotate the phone, or want to cancel.

The build is a single developer prototype with about 2 hours in total, checked by hand on a USB phone. It has no tests, so each slice has to be easy to check by eye. There are no accounts and no server, so there is no authorization model beyond keeping the API key out of logs and keeping app files private. Photos go to a third party service. That is acceptable for your own demo photos and needs no compliance scope.

## Requirements

**User stories**:
- As the demo presenter, I want to take or pick a photo and see it before generating, so I never spend credits on the wrong shot.
- As the demo presenter, I want clear progress, a way to cancel, and a useful next step on every failure, so a bad network or a used up quota doesn't stall the demo.
- As the demo presenter, I want to spin the generated model on screen, and open a bundled sample without the network, so I can show the 3D result in any conditions.

**Acceptance criteria**:
- **AC-1**: The capture screen shows "Take photo", "Choose from gallery", "Try sample model", and the one line tip "One object, plain background, good light."
- **AC-2**: "Take photo" asks for the camera permission if it isn't granted, then opens the system camera. If you deny it, the screen explains why the camera is needed and offers "Try again" and "Open settings". The permission is checked again every time the screen resumes.
- **AC-3**: "Choose from gallery" opens the system Photo Picker for images only, with no storage permission.
- **AC-4**: After a photo is taken or picked, the capture screen shows a thumbnail of the prepared photo (upright, EXIF rotation applied) and a "Generate 3D model" button. Cancelling the camera or the picker leaves the screen as it was. A photo that can't be read shows "Couldn't read that photo. Try another." and keeps the take and pick buttons.
- **AC-5**: Tapping "Generate 3D model" opens the generate screen. It shows an indeterminate progress indicator, the text "Making your 3D model. This usually takes under a minute.", and a Cancel button. One tap sends exactly one request. A double tap, rotation, or recomposition never sends a second one.
- **AC-6**: Cancel or system back during generation stops the request and returns to the capture screen, with the photo still shown.
- **AC-7**: On success the GLB is saved as `filesDir/models/<modelId>.glb`, but only once fully downloaded. The prepared photo is copied to `filesDir/models/<modelId>.jpg`, and the app opens `preview/<modelId>`. Back from the preview returns to the capture screen, not to the generate screen.
- **AC-8**: The preview shows a loading indicator until the model is ready, then shows it. Drag spins it (orbit) and pinch zooms. "New photo" returns to the capture screen.
- **AC-9**: "Try sample model" opens `preview/sample` and shows the bundled `assets/sample.glb` with no network and no credits.
- **AC-10**: Each generation failure in the error table below shows its message with Retry. Retry resends the same prepared photo. "Choose another photo" returns to the capture screen. When you are out of credits (`402`), "Try sample model" is also shown and opens `preview/sample`.
- **AC-11**: If Android killed the app during generation and restores it on the generate screen, the screen shows "Generation was interrupted. Retry to send it again." with Retry. It never sends a new paid request without your tap.
- **AC-12**: A model that fails to load (a corrupt or missing file, or an unknown or malformed `modelId`) shows "Couldn't open this model." with Retry and "New photo", and the app doesn't crash.
- **AC-13**: The API key is never logged. Each generation logs its HTTP status, duration in ms, and byte count under the Logcat tag `Dimenzy`.

## Options considered

### Option 1: Confirm on capture, three screens, file handoff (chosen)

The capture screen prepares the photo into `cacheDir/upload.jpg` and shows it, with Generate as an explicit tap. The generate screen starts the call once from its ViewModel and remembers in `SavedStateHandle` that it started. The preview screen loads the model by id through one small `ModelStore` that maps ids to files.

**Pros**:
- No credits are spent by accident: one tap equals one request, and a restored screen asks before resending.
- It follows spec 0001's routes and id only handoff, so feature 4 (AR) and feature 6 (library) plug into `ModelStore.locationFor` unchanged.

**Cons**:
- One extra tap before every generation.
- Three ViewModels for a small flow.

### Option 2: Single screen state machine

One screen and one ViewModel walk through photo, generating, and preview states, with no generate route.

**Pros**:
- Fewer files and no navigation edge cases (no double navigation, no popUpTo).

**Cons**:
- It breaks spec 0001's routes, so feature 4 would need a refactor to get a `modelId` route.
- One class grows past the 200 line rule, and system back would need hand written handling for each state.

### Option 3: Background generation with WorkManager

Generation runs as a WorkManager job that survives app close and process death, and the UI watches its status.

**Pros**:
- No interrupted state: the job finishes even if Android kills the app.

**Cons**:
- A new dependency, worker setup, and a foreground notification for a call that takes under a minute.
- The cancel and retry flow gets harder (job ids, work state mapping) inside a 2 hour budget.

## Decision

**Chosen option**: Option 1: Confirm on capture, three screens, file handoff.

Build the capture, generate, and preview screens as separate routes, each with one ViewModel. Only the `modelId` crosses screens. Generation starts once per explicit tap, and every result is a typed outcome mapped to a message.

**Implementation skills**: `sceneview` (`sceneview/sceneview`, `.claude/skills/sceneview/`, preview loading and the orbit camera) · `android-intent-security` (`android/skills`, `.claude/skills/android-intent-security/`, the `TakePicture` FileProvider) · `android-permissions-security` (`android/skills`, `.claude/skills/android-permissions-security/`, the camera runtime permission) · `edge-to-edge` (`android/skills`, `.claude/skills/edge-to-edge/`, insets on the three screens)

## Rationale

Credits are the scarcest resource in this project, so the design spends its complexity on stopping accidental requests. The confirm tap (Option 1) costs a second of your time. Option 2 saves a little code but breaks the route contract that features 4 and 6 depend on. Option 3 solves a problem (process death during a short call) that a cheap `SavedStateHandle` flag already handles well enough: you see "interrupted" and decide whether to pay again.

I settled these smaller calls myself:
- **Generation starts in `GenerateViewModel.init`, not on a second tap.** The capture screen's Generate button is already the confirm. Runner up: a start button on the generate screen, which would just be a duplicate tap.
- **The interrupted flag lives in `SavedStateHandle`.** It's set to `true` before the call, so a restored ViewModel sees it with no result and shows `Interrupted` instead of calling again. Runner up: a file marker in `cacheDir`, which is more code for the same effect.
- **Preview gets its own small `PreviewViewModel`.** It resolves `modelId` to a location through `ModelStore`, which follows the one ViewModel per screen rule and constructor injection. Runner up: resolving in the composable from the container, which breaks the injection rule.
- **The preview uses `rememberModelLoadState`, not `rememberModelInstance`.** It returns `Loading` / `Ready(instance)` / `Failed(error)` (checked in the 4.69.6 jar), so a broken file shows a message instead of an endless spinner. Runner up: `rememberModelInstance`, which returns `null` both while loading and after a failure.
- **No OkHttp logging interceptor.** It would print the `authorization` header. Log status, time, and size by hand instead. Runner up: the interceptor with `redactHeader`, which is one mistake away from leaking the key.
- **The thumbnail is decoded with `BitmapFactory`** at `inSampleSize = 2` from `upload.jpg`, off the main thread. Runner up: adding Coil, a new dependency for one image.
- **Error messages live in one `when` in `ui/generate/GenerateErrorText.kt`,** not in string resources. It's a prototype with English only. Runner up: `strings.xml`, which is better for translation but costs more files now.
- **The photo copy is best effort.** If copying to `<modelId>.jpg` fails, log it and still report success, because the GLB is the product.

## Feature design

**Data model sketch** (files only, no database; confirmed):

| Entity | Location / key | Rules |
|---|---|---|
| CameraShot | `cacheDir/photos/capture.jpg`, exposed as a FileProvider URI (authority `${applicationId}.fileprovider`) | Raw `TakePicture` output, overwritten each shot. The URI string is kept in `CaptureViewModel`'s `SavedStateHandle` (key `pendingCameraUri`). |
| PreparedPhoto | `cacheDir/upload.jpg` (exactly one) | Output of `ImagePrep`: EXIF rotated, long side 1024 px, JPEG quality 90. Overwritten by each new photo. Generate and Retry read it. |
| GeneratedModel | `filesDir/models/<modelId>.glb`, key `modelId` | `modelId` = `UUID.randomUUID().toString()`, made by `ModelRepository`. Written to `<modelId>.glb.tmp`, renamed only after the body is fully read. Kept after you leave the preview. |
| ModelPhoto | `filesDir/models/<modelId>.jpg` | A copy of PreparedPhoto made on success, for feature 6 thumbnails. Optional (best effort). |
| SampleModel | `assets/sample.glb`, `modelId` = `sample` (reserved) | Read only, a CC0 licensed GLB. |

Relations: a CameraShot or a picked gallery URI produces one PreparedPhoto (1:1). One PreparedPhoto can produce many GeneratedModels (one per successful Generate or Retry). Each GeneratedModel has at most one ModelPhoto (1:1).

**State transitions**:
- Capture: `Empty → Preparing → PhotoReady`. `Preparing → Empty` (with a `PhotoError`) when the photo can't be read. `PhotoReady → Preparing` on a new photo. `photoReady` is saved in `SavedStateHandle` and trusted only if `upload.jpg` still exists.
- Generate: `Working → Done(modelId)` (navigates to preview, popping `generate`) · `Working → Failed(GenerateError)` · `Failed → Working` on Retry · restored with `started = true` and no result `→ Failed(Interrupted)`. Cancel or back from any state pops to capture, which cancels `viewModelScope` and the OkHttp call.
- Preview: `Loading → Ready` · `Loading → Failed` · `Failed → Loading` on Retry (bump a `reloadKey` and wrap the load in `key(reloadKey)`).

**API surface**:

| Endpoint / function | Method | Key inputs | Key outputs | Auth | Key errors |
|---|---|---|---|---|---|
| `https://api.stability.ai/v2beta/3d/stable-fast-3d` via `StabilityApi.generate` (`@Multipart @Streaming @POST`, `suspend`, returns `Response<ResponseBody>`) | POST | `authorization: Bearer <key>` header (req), `image` part, `image/jpeg` (req), the other fields left at their defaults | `200`, GLB bytes streamed | API key | `400`/`422`, `401`, `402`, `403`, `413`, `429`, `5xx`, timeout |
| `ImagePrep.prepare(source: Uri): PhotoOutcome` (`image/`, `Dispatchers.IO`) | fn | content or FileProvider URI (req) | `Ready` (writes `cacheDir/upload.jpg`) or `Failed(PhotoError.Unreadable)` | none | null decode, `IOException`, `SecurityException` |
| `ImagePrep.loadThumbnail(): Bitmap?` | fn | none (reads `upload.jpg`) | bitmap at `inSampleSize = 2`, or `null` | none | missing file gives `null` |
| `ModelRepository.generate(): GenerateOutcome` (`data/`, `Dispatchers.IO`) | fn | none (reads `cacheDir/upload.jpg`) | `Success(modelId)` or `Failure(GenerateError)` | key from `BuildConfig` | see the error table |
| `ModelStore.locationFor(modelId): String?` (`data/`) | fn | `modelId` (req) | `"sample.glb"` for `sample`; `"file://" + absolutePath` for a valid UUID whose `.glb` exists; else `null` | none | malformed id or missing file gives `null` |
| `ModelStore.newModelFiles(): ModelFiles` | fn | none | fresh `modelId`, `tmp`, `glb`, and `jpg` `File`s | none | none |

`GenerateError` (sealed, `data/GenerateError.kt`), mapped in this order:

| Case | Trigger | Message | Extra actions |
|---|---|---|---|
| `MissingPhoto` | `upload.jpg` doesn't exist | "The photo is gone. Choose another." | Choose another photo |
| `BadImage` | `400`, `422` | "That photo couldn't be used. Try another." | Choose another photo |
| `BadKey` | `401` | "API key problem. Check `local.properties`." | Choose another photo |
| `OutOfCredits` | `402` | "Out of AI credits. Try the sample model." | Choose another photo, Try sample model |
| `Refused` | `403` | "The service refused this image." | Choose another photo |
| `RateLimited` | `429` | "Too many requests. Wait a minute." | Choose another photo |
| `Server` | `5xx` | "The AI service had a problem. Retry." | Choose another photo |
| `Timeout` | `SocketTimeoutException` or `InterruptedIOException` (caught first) | "Generation timed out. Retry." | Choose another photo |
| `NoNetwork` | any other `IOException` | "No internet connection." | Choose another photo |
| `Interrupted` | restored ViewModel with `started = true` and no result | "Generation was interrupted. Retry to send it again." | Choose another photo |
| `Unexpected(code: Int?)` | `413`, any other status, any other `Exception` | "Something went wrong. Retry." (log the code) | Choose another photo |

Retry is shown on every case. `CancellationException` is always rethrown and never mapped. `PhotoError` (sealed, `image/PhotoError.kt`) has one case, `Unreadable`, shown inline on the capture screen.

**Screens**:
- **Capture** (`ui/capture/`): the title, the tip line, "Take photo", "Choose from gallery", "Try sample model". When a photo is ready: a thumbnail above, plus "Generate 3D model" as the primary button (disabled while `Preparing`, with a small progress indicator). Keep the existing permission denied UI ("Try again", "Open settings").
- **Generate** (`ui/generate/`): `Working` shows the progress indicator, the progress text, and Cancel. `Failed` shows the message, Retry, "Choose another photo", and "Try sample model" for `OutOfCredits` only.
- **Preview** (`ui/preview/`): a full screen `SceneView` with `cameraManipulator = rememberCameraManipulator()` and `ModelNode(modelInstance = instance, scaleToUnits = 1f)` on `Ready`. A centered progress indicator on `Loading`. On `Failed`: the message, Retry, and "New photo". A "New photo" button below the model when `Ready`. No AR button (feature 4 adds it).

**Navigation**:
- Route constants in `Routes`: `CAPTURE = "capture"`, `GENERATE = "generate"`, `PREVIEW = "preview/{modelId}"`, and `fun preview(id: String) = "preview/$id"`.
- Capture to generate: `navigate(GENERATE) { launchSingleTop = true }`, which stops a double tap from stacking two generate screens.
- Generate to preview on `Done`: `navigate(preview(id)) { popUpTo(GENERATE) { inclusive = true } }`, triggered from state collected with `collectAsStateWithLifecycle`, so it happens only while the screen is visible.
- Cancel and "Choose another photo": `popBackStack()`. "Try sample model" from the error screen: `navigate(preview("sample")) { popUpTo(GENERATE) { inclusive = true } }`.
- "New photo" in the preview: `popBackStack(CAPTURE, inclusive = false)`.

**Value sourcing**:

| Action | Value produced / displayed | Source |
|---|---|---|
| Take photo | camera output URI | `FileProvider.getUriForFile` on `cacheDir/photos/capture.jpg`, saved in `SavedStateHandle["pendingCameraUri"]` |
| Pick photo | source URI | `PickVisualMedia(ImageOnly)` result (`null` means cancelled) |
| Prepare | upright, downscaled JPEG | `ImagePrep` steps from spec 0001 (bounds, `inSampleSize` to about 2048, `ExifInterface` rotation, scale to a 1024 long side, quality 90) |
| Show thumbnail | bitmap | `ImagePrep.loadThumbnail()` from `upload.jpg` |
| Show Generate button | `photoReady` | `SavedStateHandle["photoReady"]` AND `upload.jpg` exists |
| Generate | `authorization` header | `"Bearer " + BuildConfig.STABILITY_API_KEY` (spec 0001) |
| Generate | `image` part | `cacheDir/upload.jpg` as `RequestBody`, `image/jpeg`, filename `upload.jpg` |
| Generate | `modelId` | `UUID.randomUUID()` in `ModelStore.newModelFiles()` |
| Generate | which error and message | the HTTP status or exception class, through the `GenerateError` table above |
| Generate | interrupted state | `SavedStateHandle["started"]`, set `true` before the call |
| Generate | log line (status, ms, bytes) | `Response.code()`, `SystemClock.elapsedRealtime()` before and after, bytes copied |
| Error screen | show "Try sample model" | `error is GenerateError.OutOfCredits` |
| Preview | model location | `ModelStore.locationFor(modelId)` from the route argument |
| Preview | load state | `rememberModelLoadState(modelLoader, location)` (`Loading`, `Ready.instance`, `Failed.error`) |

**Key invariants**:
- One Generate tap, one request. Retry is the only way to resend, and a restored generate screen never calls on its own.
- `ModelRepository` catches `CancellationException` first and rethrows it, then `SocketTimeoutException` / `InterruptedIOException`, then `IOException`, then `Exception`. The UI only ever sees `GenerateOutcome`.
- A `.glb` in `filesDir/models/` is always complete. Delete the `.tmp` file on any failure or cancel (in `finally`, when the rename didn't happen).
- `ModelStore.locationFor` accepts only `sample` or a string that survives `UUID.fromString(id).toString() == id`. Anything else returns `null`, so a route argument can never point outside `filesDir/models/`.
- The OkHttp client used for generation keeps spec 0001's 90 s connect, read, write, and call timeouts.
- Loaders (`rememberEngine`, `rememberModelLoader`, `rememberEnvironmentLoader`) are created at the top of `PreviewScreen`, and only `modelId` comes in by route.

**Security model**: There are no accounts and no roles. The API key comes from `BuildConfig` and is never logged, never shown, and never sent anywhere except the `authorization` header to `api.stability.ai`. The FileProvider is `android:exported="false"` with `android:grantUriPermissions="true"` and exposes only `cacheDir/photos/` (`<cache-path name="photos" path="photos/"/>`). Models and photos live in app private storage. `modelId` is validated before it becomes a file path. Photos are sent to Stability AI, which is fine for your own demo photos (no compliance scope).

**Configuration required**:
- `STABILITY_API_KEY` in `local.properties` (already wired into `BuildConfig` by the scaffold). Required before slice 2.
- New libraries in `gradle/libs.versions.toml`: Retrofit, OkHttp, and `androidx.exifinterface`. Pin each to its current stable release that is at least 2 weeks old when you build. No JSON converter (spec 0001).

**Critical test scenarios** (manual, on the USB phone):
- Happy path: pick a gallery photo, see the thumbnail, tap Generate, see progress, land on the preview, spin it, press back to reach capture. Verifies **AC-3**, **AC-4**, **AC-5**, **AC-7**, **AC-8**.
- Camera path: with the permission revoked, tap Take photo, grant it, shoot a portrait photo, and check the thumbnail is upright. Verifies **AC-2**, **AC-4**.
- Sample: in airplane mode, tap Try sample model, and the sample shows and spins. Verifies **AC-9**.
- Failure case: in airplane mode, tap Generate, see "No internet connection.", turn the network back on, tap Retry, and it succeeds. Then use a blank key and see the `401` message. Verifies **AC-10**.
- Cancel: tap Generate, then Cancel within 2 s, and you are back on capture with the photo still shown. Logcat shows no completed request. Verifies **AC-6**.
- Interrupted: tap Generate, go home, run `adb shell am kill com.dimenzy.app`, then reopen. The screen shows the interrupted message with Retry, and no request is logged before you tap. Verifies **AC-11**.
- Broken model: `adb shell run-as com.dimenzy.app sh -c 'echo x > files/models/<id>.glb'`, then open that id. The screen shows "Couldn't open this model." Verifies **AC-12**.
- Logs: `adb logcat -s Dimenzy` shows status, ms, and bytes, and never the key. Verifies **AC-13**.
- Auth/permission: a denied camera permission shows the explanation and Open settings, and the gallery still works. Verifies **AC-2**, **AC-3**.

## Build plan

Skateboard: each slice ends with `./gradlew installDebug` and a check on the phone. Slice 2 is already the thinnest working whole (gallery to 3D to preview). Later slices widen it.

**Slice 1: the preview works without credits**
1. Add a CC0 `app/src/main/assets/sample.glb`. Add `ModelStore` (paths, `newModelFiles`, `locationFor` with UUID validation) to `data/` and wire it in `AppContainer`. Satisfies **AC-9**, **AC-12**.
2. Add `PreviewViewModel` and `PreviewScreen`: `rememberModelLoadState`, loading, ready with orbit and "New photo", failed with Retry and "New photo". Register `Routes.PREVIEW`, and add "Try sample model" plus the tip line to the capture screen. Satisfies **AC-1**, **AC-8**, **AC-9**, **AC-12**.

**Slice 2: gallery to 3D to preview (the whole loop)**
3. Add Retrofit, OkHttp, and `exifinterface` to the catalog and `app/build.gradle.kts`. In `AppContainer`, build the 90 s `OkHttpClient`, Retrofit (`baseUrl("https://api.stability.ai/")`), and `StabilityApi`. Satisfies **AC-7**.
4. Add `ImagePrep` and `PhotoError` to `image/`. Add `CaptureViewModel` (with a `SavedStateHandle` factory from `AppContainer`): Photo Picker, prepare on `Dispatchers.IO`, thumbnail, the `photoReady` check, the Generate button, and the inline unreadable message. Satisfies **AC-3**, **AC-4**.
5. Add `GenerateError`, `GenerateOutcome`, and `ModelRepository.generate()`: stream to `.tmp`, rename, copy the photo, the `Dimenzy` log line, and the ordered catches. Satisfies **AC-7**, **AC-13**.
6. Add `GenerateViewModel` (starts in `init`, uses the `started` flag) and `GenerateScreen` (progress text, Cancel). Add the navigation rules (`launchSingleTop`, `popUpTo(GENERATE)`). Satisfies **AC-5**, **AC-6**, **AC-7**, **AC-11**.

**Slice 3: camera input**
7. Add the FileProvider to the manifest and `res/xml/file_paths.xml`. Wire `TakePicture` behind the existing camera permission flow, with `pendingCameraUri` in `SavedStateHandle`, then hand the result to the same prepare path. Satisfies **AC-2**, **AC-4**.

**Slice 4: every failure has a next step**
8. Build the error state of `GenerateScreen`: each `GenerateError` message from `GenerateErrorText.kt`, Retry, "Choose another photo", and "Try sample model" for `OutOfCredits`. Include the `Interrupted` and `MissingPhoto` cases. Satisfies **AC-10**, **AC-11**.
9. Run the critical test scenarios above on the phone, and fix what fails. Satisfies **AC-1** to **AC-13**.

## Consequences

**Positive**:
- Credits are spent only on purpose: one tap is one request, and an interrupted run asks before it resends.
- `ModelStore.locationFor` is the single id to file mapping that features 4 (AR) and 6 (library) reuse, and models already carry a thumbnail photo for feature 6.
- Each slice can be checked on the phone. Slice 1 needs no key and no network.

**Negative / tradeoffs**:
- One extra tap before every generation.
- If Android kills the app mid call, the service may still have charged you, and Retry pays again. You accept this instead of adding WorkManager.
- Cancelling after the service finished on its side still spends the credits.
- A disk write error during download is reported as "No internet connection." because both are `IOException`. This is very unlikely for a file of a few MB, and you accept it.
- Error text is hard coded in English in one Kotlin file, not in string resources.
- Generated models pile up in `filesDir/models/` with no delete until feature 6.

**Neutral**:
- Three new libraries (Retrofit, OkHttp, exifinterface), all chosen in spec 0001.
- The scaffold's capture screen permission logic is reused, and its "Camera ready" placeholder text goes away.

## Follow-up

- [ ] Download a CC0 sample GLB (for example Avocado or WaterBottle from the Khronos glTF Sample Assets repo), confirm its license file says CC0, and keep it under about 10 MB.
- [ ] Put your `STABILITY_API_KEY` in `local.properties` before slice 2. Do slice 1 and slice 3 checks with the sample to save credits.
- [ ] Feature 4 adds "Place in AR" to the preview and registers `ar/{modelId}`, resolving the model through `ModelStore.locationFor`.
- [ ] Feature 6 can list `filesDir/models/*.glb` with the matching `<modelId>.jpg` thumbnails, and should add delete (removing both files).
