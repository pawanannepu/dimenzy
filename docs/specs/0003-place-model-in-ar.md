# 0003. Place the model in AR with SceneView PlacementScene behind an AR readiness gate

**Date**: 2026-11-18
**Status**: Accepted

## Summary

A "Place in AR" button on the 3D preview opens a new AR screen. That screen first checks that the phone can run AR (ARCore support, Google Play Services for AR, and the camera permission), and explains any problem with a clear message instead of crashing. Then SceneView's ready made `PlacementScene` (a built in AR view with an aim ring, a scanning guide card, tap to place, and a ground shadow) puts one copy of the model, 0.4 m on its longest side, on a floor or table. Drag moves it, pinch resizes it, and a two finger twist rotates it. Nothing new is saved, and no new library or key is needed.

## Context

Feature 4 is the second half of the demo loop: the model from feature 3 has to stand in your real room. Spec 0001 already fixed the foundations: SceneView 4.69.6 (`io.github.sceneview:arsceneview`), ARCore in optional mode (phones without AR can still install the app and use the 3D preview), a route `ar/{modelId}` that only carries an id, a 0.4 m starting size, one placed model at a time, and a check of ARCore availability before AR opens. Spec 0002 built the preview and `ModelStore.locationFor`, the single mapping from a `modelId` to a model file, including the bundled `sample` model.

What's still open is the behavior and the shape of the AR screen: where you enter it, how placement looks and feels, which surfaces count, what a second tap does, which gestures you get, what's on screen, how the model is lit, and what you see for every way AR can fail to start or lose track of the room. AR has more failure points than any other screen: the phone may not support ARCore, the Play Services for AR app may be missing or outdated, you may deny the camera, the room may be too dark to find a surface, or tracking may drop as you walk around.

SceneView 4.69.6 ships more than spec 0001 knew about. Reading the library itself shows a `PlacementScene` composable (parameters: `modifier`, `engine`, `modelLoader`, `materialLoader`, `planeRenderer`, `planeFindingMode`, `instantPlacement`, `showReticle`, `reticleColor`, `reticleStyle`, `fadePlaneOnFirstPlacement`, `coaching`, `groundShadows`, `playbackDataset`, `sessionConfiguration`, `coachingBottomClearance`, `onPlaced: (Anchor)`, `content: (PlacementController)`). Its `PlacementController` exposes `anchors`, `count` and `clear()`. The library also has ready made ARCore availability and camera permission overlays. `PlacementScene` has no `onTrackingFailureChanged` or `onSessionUpdated` parameter, though; its coaching card shows the tracking hints itself.

The build is a single developer prototype with about 2 hours in total for the whole app, checked by hand on a USB phone with no automated tests. No data leaves the phone in this feature, there are no accounts, and the only sensitive surface is the camera permission. No compliance scope applies.

## Requirements

**User stories**:
- As the demo presenter, I want to open the model I just made in AR with one tap and drop it on the floor or a table, so the audience sees it standing in the real room.
- As the demo presenter, I want to move, resize and turn the placed model with familiar gestures, so I can stage it well quickly.
- As the demo presenter, I want every AR problem (no ARCore, missing install, no camera access, a model that won't load, lost tracking) to show a plain message with a next step, so a bad phone or a dim room never crashes the demo.

**Acceptance criteria**:
- **AC-1**: When the preview's model is ready, the preview shows a "Place in AR" button (the primary button) above "New photo" (now an outlined button). Tapping it opens `ar/{modelId}` for that same model, including `sample`. A double tap opens the AR screen only once. The button is shown on every phone; the AR screen handles unsupported phones.
- **AC-2**: Before any camera view, the AR screen shows "Checking AR support…" with a progress indicator while ARCore availability is checked. The checks (ARCore, install, camera permission) run again every time the screen resumes. When they still pass, the AR view stays as it was, with no flash of the checking state.
- **AC-3**: On a phone ARCore doesn't support, the AR screen shows "AR isn't supported on this phone." with "Back to 3D view", and the app does not crash. The 3D preview keeps working.
- **AC-4**: If ARCore is supported but Google Play Services for AR is missing or too old, the system install prompt opens once. After installing and returning to the app, AR continues with no extra tap. If you decline, the screen shows "AR needs Google Play Services for AR." with "Install" (opens the prompt again) and "Back to 3D view". If the check itself fails (no network, ARCore error, or still undecided after 10 s), the screen shows "Couldn't check AR support. Check your internet and retry." with "Retry" and "Back to 3D view".
- **AC-5**: If the camera permission isn't granted, the AR screen asks for it. If you deny it, the screen shows "AR needs the camera to see your room." with "Try again", "Open settings" and "Back to 3D view". Granting it in Settings and returning continues into AR.
- **AC-6**: With all checks passed, the camera view shows SceneView's coaching card while it scans, then an aim ring (reticle) that locks onto detected floors and tables. Only horizontal surfaces are detected; walls are never offered.
- **AC-7**: Tapping while the reticle is on a surface places the model there, standing upright, with its longest side 0.4 m. After the first placement the plane grid fades, a ground shadow sits under the model, and the model is lit by the room's estimated light (ARCore environmental HDR).
- **AC-8**: The placed model stays put on its surface as you walk around it and look from other sides.
- **AC-9**: A one finger drag on the model moves it along the surface, pinch resizes it, and a two finger twist rotates it.
- **AC-10**: Only one model is ever shown. Tapping another spot on a surface moves the model there, fresh at the 0.4 m start size and default rotation. "Reset" removes the model, brings back the reticle and plane grid, and the next tap places it again.
- **AC-11**: The AR screen has a Back button at the top (content description "Back"). It and system back both return to the 3D preview. After the model is placed, one hint line shows near the bottom: "Drag to move, pinch to resize, twist to rotate." "Reset" shows only while a model is placed. No control is hidden under the status or navigation bar, and the coaching card sits above the bottom controls.
- **AC-12**: If tracking is lost after placement (camera covered, moving too fast, too dark, too little detail), the model stays on its anchor, and the coaching card names the cause (for example "Too dark, move to a brighter spot"). The hint goes away when tracking returns.
- **AC-13**: While the model file loads, a "Loading model" indicator shows over the camera view, and scanning carries on. A model that fails to load (corrupt or missing file, unknown or malformed `modelId`) shows "Couldn't open this model." with "Retry" and "Back to 3D view", and the app doesn't crash.
- **AC-14**: If the AR session can't start after the checks pass (for example another app holds the camera), you see a clear message with a way to retry or go back, never a crash. SceneView's built in "Couldn't start AR" card meets this.

## Options considered

### Option 1: PlacementScene behind your own readiness gate (chosen)

The AR screen runs its own small gate first: spec 0001's ARCore availability check, `requestInstall`, and your existing `CameraPermission` helper, each failure mapped to a sealed `ArError` with your own message. Only once all of them pass does it show SceneView's `PlacementScene`, configured for horizontal planes, coaching, a reticle, ground shadows, environmental HDR light, and one visible model.

**Pros**:
- Least scene code: reticle, coaching card, tracking hints, plane grid fade and ground shadow come built in.
- The gate uses only APIs already named in spec 0001 and code already in the repo, so nothing is guessed.
- Every failure follows the project's one error pattern (sealed type, a message, a next step).

**Cons**:
- `PlacementScene` exposes no tracking or session callbacks, so if its coaching card turns out not to show tracking hints after placement, getting AC-12 means switching to the manual assembly.
- It keeps every tapped anchor in its own list, so "one model" is done by drawing only the newest anchor, not by the library.

### Option 2: Hand written tap to place on ARSceneView (spec 0001's recipe)

`ARSceneView` with `onSingleTapConfirmed` → `frame.hitTest(event)` → `hit.createAnchor()`, a single anchor in screen state, `PlaneDiscoveryGuide` for onboarding, and `onTrackingFailureChanged` for hints. Same readiness gate as Option 1.

**Pros**:
- Full control: the single anchor rule and tracking hints are your own code, with direct callbacks.
- It's the exact pattern verified in the sceneview skill.

**Cons**:
- More code to write and check by hand within a 2 hour budget.
- No aim ring and no ground shadow unless you wire `PlacementReticle` and `ShadowReceiverPlane` yourself, and the skill warns that stacking a shadow receiver with the plane grid z fights (flickers) unless gated.

### Option 3: PlacementScene with the library handling prerequisites

`PlacementScene` alone, letting `ARSceneView` request the camera and the ARCore install itself, with SceneView's `ARCoreAvailabilityOverlay` and `ARCameraPermissionOverlay` for messages.

**Pros**:
- The fewest lines of all.
- The library's own wording and recovery actions.

**Cons**:
- The hooks that feed those overlays aren't documented in the sceneview skill, so the wiring would have to be worked out partway through the build.
- Its text is written for a demo app ("The other demos still work"), and failures bypass the project's sealed error pattern.

## Decision

**Chosen option**: Option 1: PlacementScene behind your own readiness gate.

The AR screen at `ar/{modelId}` runs an ARCore, install, and camera permission gate that maps every failure to a sealed `ArError`, then shows `PlacementScene` on horizontal planes with coaching, reticle, ground shadow, environmental HDR light, and only the newest anchor's model drawn with `isEditable = true` at 0.4 m.

**Implementation skills**: `sceneview` (`sceneview/sceneview`, `.claude/skills/sceneview/`, authoritative for `PlacementScene`, `AnchorNode`, `ModelNode`, and model loading) · `android-permissions-security` (`android/skills`, `.claude/skills/android-permissions-security/`, the camera runtime permission) · `edge-to-edge` (`android/skills`, `.claude/skills/edge-to-edge/`, insets for the controls over the camera view)

## Rationale

The scope gives the whole app about 2 hours and checks everything by hand, so the scene itself should be as little code as possible. `PlacementScene` is the sceneview skill's preferred path and delivers the reticle, coaching card, tracking hints, grid fade and ground shadow that make placement look right, which Option 2 would rebuild by hand. Spec 0001 chose the hand written recipe before this ready made scene was known. This spec replaces that one line of 0001; everything else in 0001's AR row (optional ARCore, one anchor, 0.4 m, `isEditable`) still holds.

The prerequisites are where Option 3 is weakest. AR fails to start more often than any other part of the app, and the scope's "done when" asks for a clear message instead of a crash. A gate built from `ArCoreApk` (spec 0001 already defines the polling and install flow) and the `CameraPermission` helper you already have uses no guessed API names. It also follows AGENTS.md's rules: a sibling sealed error type per area, and checking the real state again on resume. Because the gate passes before `PlacementScene` appears, the library's own permission and install handling finds everything already granted and stays quiet. Its built in "Couldn't start AR" card still covers the rare session failure.

Several smaller calls came out of the same reasoning. Horizontal planes only, because generated objects stand upright and wall placement needs its own scene. A tap elsewhere moves the model, keeping spec 0001's single model rule. Drag moves the model (the built in `isEditable` set) instead of rotating it, since a twist already rotates and custom gestures cost time. Environmental HDR light plus the ground shadow, because a model without a shadow reads as floating.

## Feature design

**Data model sketch**: None. Nothing is saved. The placed anchors live in `PlacementScene`'s `PlacementController` for as long as the AR screen is open. The model file is the one spec 0002 wrote (`filesDir/models/<modelId>.glb`) or the bundled `assets/sample.glb`.

**State transitions** (AR screen readiness, held in screen state, run again on every resume):
- `Checking` → `Installing` (supported, not installed or too old: `requestInstall` returned `INSTALL_REQUESTED`) → `Checking` (on resume)
- `Checking` → `NeedsCamera` (ARCore ready, camera not granted) → `Ready` (granted)
- `Checking` → `Ready` (ARCore ready and camera granted)
- Any state → `Blocked(ArError)` (unsupported, install declined, check failed, camera denied). Retry, Install or Try again go back to `Checking`.
- `Ready` on resume: check again silently; stay `Ready` (scene untouched) unless a check now fails.
- Inside `Ready`, placement: `Scanning` (no anchor) → `Placed` (≥1 anchor; the newest is drawn) → `Scanning` (Reset calls `controller.clear()`).

**API surface** (screens, routes and classes; there is no network API):

| Surface | Kind | Key inputs | Key outputs | Auth | Key errors |
|---|---|---|---|---|---|
| `ar/{modelId}` (`Routes.AR`, `Routes.ar(modelId)`) | Navigation route | `modelId: String` (UUID or `sample`) | AR screen | none | unknown or malformed id → `ArError.ModelLoadFailed` |
| `PreviewScreen(viewModel, onNewPhoto, onPlaceInAr)` | Composable, changed | `onPlaceInAr: () -> Unit` wrapped in `dropUnlessResumed`, navigates with `launchSingleTop` | "Place in AR" button when the model is `Ready` | none | none |
| `ArViewModel(savedState, store: ModelStore)` (`ui/ar/`) | ViewModel, same shape as `PreviewViewModel` | `modelId` from `SavedStateHandle` | `target: StateFlow<ArTarget(location: String?, reloadKey: Int)>`, `retry()` | none | `location == null` → `ModelLoadFailed` |
| `ArCoreCheck(context)` (`ui/ar/`) | Plain class wrapping `ArCoreApk` | `Context`; `Activity` for install | `suspend fun availability(): ArCoreStatus` (`Ready`, `NeedsInstall`, `Unsupported`, `Failed`); `fun requestInstall(activity, userRequested: Boolean): InstallResult` (`Installed`, `Requested`, `Declined`, `Failed`) | none | `UnavailableUserDeclinedInstallationException` → `Declined`; any other ARCore exception → `Failed` |
| `rememberArReadiness(check, cameraPermission)` (`ui/ar/`) | Composable state holder | `ArCoreCheck`, `CameraPermission` | `ArReadiness` state (above) plus `retry()`, `install()` | none | maps to `ArError` |
| `ArError` + `ArError.message()` (`ui/ar/ArError.kt`) | Sealed interface | | `Unsupported`, `InstallDeclined`, `CheckFailed`, `CameraDenied`, `ModelLoadFailed`, each with its message and actions (table below) | none | |
| `ArScreen(viewModel, onBack)` (`ui/ar/`) | Composable | gate state, model load state | blocked screen, or `PlacementScene` plus overlay (Back, Loading, hint, Reset) | none | |
| `CameraPermission` | Moved from `ui/capture/` to `ui/common/`, unchanged | | | | |

**Error messages and actions** (the one place `ArError` becomes words, English only):

| `ArError` | When | Message | Actions |
|---|---|---|---|
| `Unsupported` | `UNSUPPORTED_DEVICE_NOT_CAPABLE` | "AR isn't supported on this phone." | Back to 3D view |
| `InstallDeclined` | `requestInstall` threw `UnavailableUserDeclinedInstallationException` | "AR needs Google Play Services for AR." | Install · Back to 3D view |
| `CheckFailed` | `UNKNOWN_ERROR`, `UNKNOWN_TIMED_OUT`, still `UNKNOWN_CHECKING` after 10 s, or any other ARCore exception | "Couldn't check AR support. Check your internet and retry." | Retry · Back to 3D view |
| `CameraDenied` | the permission dialog returned denied | "AR needs the camera to see your room." | Try again · Open settings · Back to 3D view |
| `ModelLoadFailed` | `location == null` or `ModelLoadState.Failed` | "Couldn't open this model." | Retry · Back to 3D view |

The AR session failing to start after the gate is not an `ArError`. No exception reaches the app, and SceneView's built in "Couldn't start AR" card handles it (AC-14).

**`PlacementScene` configuration**:
- `engine = rememberEngine()`, `modelLoader = rememberModelLoader(engine)` at the top of `ArScreen`, never inside the reloadable part (same rule as the preview). Each screen owns its own engine; only the `modelId` crosses routes.
- `planeFindingMode = Config.PlaneFindingMode.HORIZONTAL`, `instantPlacement = false`, `showReticle = true`, `coaching = true`, `groundShadows = true`, `fadePlaneOnFirstPlacement = true`, `planeRenderer = true`.
- `sessionConfiguration = { _, config -> config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR }`.
- `coachingBottomClearance` = the measured height of the bottom controls (hint plus Reset) + 8 dp.
- The model loads once per screen with `rememberModelLoadState(modelLoader, location)` inside `key(target.reloadKey)`, as on the preview. `onPlaced = { anchor -> if (anchor == newestAnchor) AnchorNode(anchor = anchor) { ModelNode(modelInstance = instance, scaleToUnits = START_SIZE_METERS, isEditable = true) } }`, drawn only when the load state is `Ready`.
- `newestAnchor` comes from the `PlacementController` handed to `content`. The screen keeps that controller in its own state (for example set from a `SideEffect` in `content`), so Reset outside the scene can call `clear()` and the overlay can read `count`.
- Exact parameter names above were read from the 4.69.6 library. `/develop` confirms each against the sceneview skill before use and never invents one.

**Value sourcing**:

| Action | Value produced / displayed | Source |
|---|---|---|
| Tap "Place in AR" | the `modelId` for the route | the preview route's own `modelId` argument (spec 0002), read in `DimenzyNavHost` |
| Open AR screen | model location | `ModelStore.locationFor(modelId)` (spec 0002): `sample.glb` asset, `file://` path, or `null` |
| ARCore check | availability | `ArCoreApk.getInstance().checkAvailability(context)`, checked again every 200 ms while `UNKNOWN_CHECKING`, given up after 10 s (`CheckFailed`) |
| ARCore install | install result | `ArCoreApk.getInstance().requestInstall(activity, userRequested)`. `activity` from `LocalActivity.current` (`androidx.activity:activity-compose` 1.13.0, no cast); a `null` activity counts as `CheckFailed`. `userRequested` is `true` on the first ask and after tapping Install, and `false` on the resume after `INSTALL_REQUESTED` (ARCore's documented flow) |
| Camera check | granted or denied | `CameraPermission` (`ContextCompat.checkSelfPermission`, checked again on resume) |
| Show the scene | surfaces to detect | `Config.PlaneFindingMode.HORIZONTAL` (this spec) |
| Show the scene | room light | `Config.LightEstimationMode.ENVIRONMENTAL_HDR` in `sessionConfiguration` (this spec) |
| Tap to place | where the model goes | the anchor `PlacementScene` creates from its reticle hit, passed to `onPlaced` |
| Tap to place | which anchor is drawn | `controller.anchors.lastOrNull()` |
| Tap to place | start size | `START_SIZE_METERS = 0.4f` constant in `ui/ar/` (spec 0001), passed as `scaleToUnits`. Feature 7 replaces it |
| Gestures | move, resize, rotate | `ModelNode(isEditable = true)` built in gestures |
| Reset | cleared scene | `controller.clear()`; Reset visible while `controller.count > 0` |
| After placement | hint text | constant "Drag to move, pinch to resize, twist to rotate.", shown while `controller.count > 0` |
| Tracking lost | cause shown | `PlacementScene`'s coaching card, from ARCore's tracking failure reason (library strings: "Too dark", "Too fast", "Low detail", "Camera busy") |
| Any blocked state | message and actions | `ArError.message()` and the table above |
| Model loading | indicator | `ModelLoadState` not `Ready` and not `Failed` |

**Key invariants**:
- At most one model is drawn: only the newest anchor gets a `ModelNode`. Older anchors stay in the controller's list until Reset (a few taps in a demo cost nothing).
- No camera view and no ARCore session start until the gate is `Ready`. The gate runs again on every resume and never trusts a saved result (AGENTS.md).
- Every failure the app can see becomes an `ArError` with a message and a next step. No `!!`, no unchecked casts, and no raw exception reaches the UI.
- Only `modelId` crosses from preview to AR. The AR screen builds its own engine, loader and model instance.
- No new manifest entries: `CAMERA`, the optional `camera.ar` feature, and `com.google.ar.core` = `optional` are already declared (spec 0001).

**Security model**: No accounts and no roles. The only sensitive surface is the camera. It is requested at runtime, only on the AR screen or the capture screen, and checked again on resume. Camera frames stay on the phone; nothing in this feature uses the network except ARCore's own availability check and Play install. No compliance scope.

**Configuration required**: None. No new keys, properties, or dependencies (`arsceneview` already brings ARCore).

**Critical test scenarios** (manual, on the USB phone):
- Happy path: "Try sample model" → "Place in AR" → scan the floor → tap → the model stands there with a shadow; walk around it; drag, pinch, twist; tap elsewhere (it moves); Reset; tap again; Back returns to the preview. Verifies **AC-1**, **AC-6** to **AC-11**.
- Generated model: the same flow from a freshly generated model. Verifies **AC-1**, **AC-7**.
- Camera denied: revoke the camera permission in Settings, open AR, deny → message with Try again and Open settings; grant in Settings and return → AR starts. Verifies **AC-5**, **AC-2**.
- ARCore check failure: turn on airplane mode and clear the Google Play Services for AR app's data (or test on a phone without it) → "Couldn't check AR support" or the install prompt; decline → the install message. Verifies **AC-4**.
- Unsupported phone (if one is at hand; otherwise check the code path by reading it): the message and Back, no crash. Verifies **AC-3**.
- Tracking loss: after placing, cover the camera, then wave the phone fast → the coaching card names the cause, and the model is still there afterward. Verifies **AC-12**.
- Bad model: open `ar/` for a deleted model id (delete the GLB with `adb shell run-as`) → "Couldn't open this model." with Retry and Back. Verifies **AC-13**.
- Resume: place a model, switch apps and come back → no checking flash, AR resumes. Verifies **AC-2**.

## Build plan

Skateboard: the first slice is the thinnest usable whole (the sample model standing on your floor), then each slice adds the next most demo critical layer.

**Slice 1: the sample model stands on your floor**
1. Add `Routes.AR = "ar/{modelId}"` and `Routes.ar(modelId)`, register the `ArScreen` destination, and add "Place in AR" to the preview's ready overlay (primary button, "New photo" becomes outlined), wired through `dropUnlessResumed` and `launchSingleTop`. Satisfies **AC-1**, **AC-11** (Back).
2. Add `ArViewModel` and its factory in `AppContainer` (same shape as `PreviewViewModel`, using `ModelStore`). Satisfies **AC-1**.
3. Move `CameraPermission.kt` to `ui/common/` and request it on entering the AR screen (the rest of the gate comes in slice 3). Satisfies **AC-5** (granted path).
4. Build `ArScreen` with `PlacementScene` configured as above: horizontal planes, coaching, reticle, ground shadow, HDR light, the model loaded once, only the newest anchor drawn with `isEditable = true` at `START_SIZE_METERS`, plus a top Back button with safe drawing insets. Satisfies **AC-6**, **AC-7**, **AC-8**, **AC-9**, **AC-10** (move).
5. Phone check: the happy path with the sample model. Satisfies **AC-6** to **AC-9**.

**Slice 2: controls, hints and model states**
6. Keep the `PlacementController` in screen state, then add Reset (visible while `count > 0`), the hint line after placement, and `coachingBottomClearance` from the measured bottom bar. Satisfies **AC-10**, **AC-11**.
7. Add the "Loading model" indicator and the `ModelLoadFailed` screen with Retry (reloads via `reloadKey`) and Back. Satisfies **AC-13**.
8. Phone check: tracking loss hints after placement. If the coaching card shows nothing once a model is placed, stop and route back to `/architect` (the fix is Option 2's manual assembly, a design change). Satisfies **AC-12**.

**Slice 3: the readiness gate**
9. Add `ArError` with `message()` and `ArCoreCheck` (availability polling with the 10 s cap, `requestInstall` with exception mapping). Satisfies **AC-3**, **AC-4**.
10. Add `rememberArReadiness`: the state machine above, run again on every resume with no flash when still `Ready`, the install flow through `LocalActivity.current`, and the camera step through `CameraPermission`. Then show the checking and blocked screens in `ArScreen`. Satisfies **AC-2**, **AC-3**, **AC-4**, **AC-5**.
11. Phone check: camera denied, airplane mode check failure, resume, and a session start with another camera app open. Satisfies **AC-2**, **AC-4**, **AC-5**, **AC-14**.

**Slice 4: full loop**
12. Phone check of the whole loop with a freshly generated model, then every critical test scenario above. Satisfies **AC-1** to **AC-14**.

## Consequences

**Positive**:
- The AR scene is a single library composable plus a short overlay, and most of the placement polish comes free.
- Every startup failure has a message and a next step, which protects the demo on an unfamiliar phone.
- No new dependency, key, manifest entry or stored data, so feature 5 (release APK) gains nothing new to configure.
- Feature 7 (real size entry) has one hook: replace `START_SIZE_METERS` with the entered height.

**Negative / tradeoffs**:
- Tapping elsewhere moves the model but resets its size and rotation to the defaults. Dragging keeps them. That's the price of keeping exactly one anchor with no custom transform state.
- `PlacementScene` hides tracking and session callbacks. If its coaching card doesn't cover tracking loss after placement, AC-12 needs Option 2's manual assembly, which costs real time.
- Old anchors stay in the controller list until Reset (each extra tap leaves one tracked ARCore anchor behind). Fine for a demo, wrong for a long session.
- The library's built in cards (coaching hints, "Couldn't start AR") use SceneView's own English wording, so a little of the text isn't yours.
- Environmental HDR light estimation costs some GPU time on older ARCore phones.
- Drag moves instead of rotating, which differs from the scope's "twist or drag to rotate" wording. Twist still rotates.

**Neutral**:
- This spec replaces one line of spec 0001 (its AR row said tap to place is hand written). The rest of that row still holds.
- `CameraPermission` moves to `ui/common/`, since two screens now share it.
- Only one Filament engine runs at a time: the preview's scene leaves composition when the AR screen opens.

## Follow-up

- [x] Spec 0001's AR row ("Tap to place is hand written…") now points to this spec as the placement approach.
- [ ] Before the demo, install or update Google Play Services for AR on the demo phone (already in spec 0001's follow ups).
