# Scope: Dimenzy

An Android app where you take or pick a photo of an object, an AI turns its main object into a 3D model, and you place that model in your real room through the phone camera using AR. It's a college project with no accounts, and the deliverable is a release APK you can demo on your own phone.

**Build approach:** Skateboard (ship the thinnest working whole first, photo to 3D to AR, then grow it if time is left).
**Workflow:** Prototype (just `/develop`; you test each build yourself on your USB connected Android phone, with no formal verify or test suite). Any feature can carry its own tag to do more.

_These are recommendations to keep your build orderly, not requirements. Skip anything that does not fit: if you already know how to build a feature, use `/develop` and skip `/architect`. You decide when a feature is `done`._

**Constraints:** about 2 hours total; Android only; no auth; the AI service must be free tier (the app needs internet to generate); the phone must support AR (ARCore). The release APK gets built only after you've confirmed the core loop works on your phone.

## At a glance

| # | Feature | Phase | Status |
|---|---------|-------|--------|
| 1 | Stack & architecture | Foundation | done |
| 2 | Coding standards & tooling | Foundation | done |
| 3 | Photo to 3D model | Release 1 | done |
| 4 | Place the model in AR | Release 1 | done |
| 5 | Release APK | Release 1 | in-progress |
| 6 | Saved model library | Release 2 | planned |
| 7 | Real size entry | Release 2 | planned |

## Foundations

### 1. Stack & architecture
Pick the Android app framework, the AR approach, and how the app talks to a free tier image to 3D AI service. Then scaffold a project that runs on your phone over USB.
**Done when:** the choices are recorded in a spec, and an empty app installs and opens on your USB connected phone with the camera permission working.
spec [0001](../specs/0001-android-stack-architecture/index.md) · code in `app/`
- [x] Decide the stack (spec): `/architect stack & architecture`
- [x] Scaffold from the decision: `/develop stack & architecture`

### 2. Coding standards & tooling
A light pass that records conventions and the build and run commands in `AGENTS.md`, so later steps (and the APK build) stay consistent. It's quick, and you can skip it if you're short on time.
**Done when:** root `AGENTS.md` describes the real project and how to run it on the device.
- [x] Capture conventions + tooling choices: `/audit`

## Release 1: Photo to 3D to AR (the demo)

### 3. Photo to 3D model
Take a photo with the camera or pick one from the gallery, send it to the AI service, and get back a 3D model of the main object. Keep the screens simple and clean, with no separate design system.
**Done when:** you can pick or take a photo, you see a progress state while it generates, you get a 3D model you can preview and spin on screen, and a failure (no internet, service error, timeout) shows a clear message with a retry.
spec [0002](../specs/0002-photo-to-3d-model.md) · code in `app/src/main/kotlin/com/dimenzy/app/`
- [x] Design it (spec): `/architect photo to 3D model`
- [x] Build it: `/develop photo to 3D model`
  - [x] Slice 1: sample model preview with load failure handling, no credits needed (AC-1, AC-8, AC-9, AC-12)
  - [x] Slice 2: gallery pick, photo prep and thumbnail, generate, save, preview (AC-3, AC-4, AC-5, AC-6, AC-7, AC-11, AC-13)
  - [x] Slice 3: camera capture through the FileProvider, behind the permission (AC-2, AC-4)
  - [x] Slice 4: every generation error has its message, Retry, and next step, then a full phone check (AC-10, AC-11)

### 4. Place the model in AR
Open the camera in AR, find a floor or table surface, and tap to place the generated model. Pinch to scale, twist or drag to rotate.
**Done when:** on your phone, the model sits on a detected surface and stays put as you walk around it, pinch and rotate work, and a phone without AR support gets a clear message instead of a crash.
spec [0003](../specs/0003-place-model-in-ar.md) · code in `app/src/main/kotlin/com/dimenzy/app/ui/ar/`
- [x] Design it (spec): `/architect place the model in AR`
- [x] Build it: `/develop place the model in AR`
  - [x] Slice 1: the sample model stands on your floor, with Place in AR, PlacementScene, gestures, and tap to move (AC-1, AC-5, AC-6, AC-7, AC-8, AC-9, AC-10)
  - [x] Slice 2: Reset, the hint line, loading and load failure, and the tracking hint check (AC-10, AC-11, AC-12, AC-13)
  - [x] Slice 3: the readiness gate for ARCore support, install, and camera, checked again on resume (AC-2, AC-3, AC-4, AC-5, AC-14)
  - [x] Slice 4: a full phone check with a freshly generated model (AC-1 to AC-14)
  - Checked on a moto edge 50 fusion (placement, shadow, drag, pinch, rotate, tap to move, hint, Reset, Back, resume) and on two vivo phones without ARCore (clear message, no crash). Skipped, marked done by you: walk around (AC-8), tracking loss hint (AC-12), camera denied (AC-5), install prompt (AC-4), camera busy (AC-14), load failure (AC-13), a freshly generated model in AR.

### 5. Release APK
Build a signed release APK once you've confirmed features 3 and 4 work on your phone.
**Done when:** a signed release APK installs on a clean phone and runs the full photo to 3D to AR loop.
- [x] Build it: `/develop release APK`
- Code in `app/build.gradle.kts` (signing from `local.properties`); output `app/build/outputs/apk/release/app-release.apk`

## Release 2: Grow it (only if time is left)

### 6. Saved model library · needs a decision
Keep each generated model on the phone in a simple list, so you can place it again without regenerating. That protects your demo if the wifi is bad.
**Done when:** generated models show up in a list after an app restart, tapping one opens it in AR, and you can delete one.
- [ ] Design it (spec): `/architect saved model library`

### 7. Real size entry
On top of pinch to scale, let you type a real height (for example 1.5 m for a car) so the model snaps to life size in AR.
**Done when:** entering a height resizes the placed model to that real height, and pinch still works afterward.
- [ ] Build it: `/develop real size entry`

## Deferred
Out of scope for this build, kept so the plan stays honest.
- **Background cutout preview**: show and adjust the isolated object before generating · needs a decision
- **Share or export the model**: send the 3D file to another app
- **Several objects in one scene**: place more than one model at a time
- **AI size guess**: auto estimate the object's real size · needs a decision

## Legend

**The decision box.** Every feature carries exactly one, the sub task whose label ends with `(spec)`. Skills locate it by that `(spec)` suffix, never by an exact label. Every other box is an execution box.

- **Next step** = the first unticked box.
- **needs a decision** = run `/architect` first; otherwise go straight to `/develop` (or `/audit` for standards & tooling). The tag drops once the spec is captured.
- **Atomic build tasks live in the spec's `## Build plan`**, not here.
- **Status** `planned` → `in-progress` → `done`, plus `existing` (from before this workflow) and `dropped` (removed from scope, kept for history).
- **Workflow tier tag** beside a heading (e.g. `· Alpha`) changes that one feature's rigor; no tag inherits the project default.
- **Workflow Prototype** = nothing runs after `/develop`; you check it yourself on the phone, and you call it `done`.
- **Pointer line** (`spec <n> · code in <path>`): the spec link gets added by `/architect`, the code path by `/develop`.
