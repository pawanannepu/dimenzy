# Dimenzy

Android app: take or pick a photo, an AI service turns the object into a 3D model, then you place it in your room with AR.

## Stack

- **Language / Runtime**: Kotlin 2.4.21 (coroutines, `StateFlow`), JDK 21, `minSdk 24`, `compileSdk`/`targetSdk 37`
- **Framework**: Jetpack Compose + Material 3, Navigation Compose, single `app` module, light MVVM (one ViewModel per screen, `ModelRepository`, hand written `AppContainer` in `DimenzyApp`, no DI framework)
- **Key dependencies**: SceneView `io.github.sceneview:arsceneview:4.69.6` (3D preview and ARCore, AR optional) · Retrofit + OkHttp to Stability AI Stable Fast 3D (added in feature 3)
- **Build**: Gradle 9.8.2 wrapper, AGP 9.4.1 (built in Kotlin), version catalog in `gradle/libs.versions.toml`
- **Package**: `com.dimenzy.app`. Full decision: [spec 0001](docs/specs/0001-android-stack-architecture/index.md)

## Build approach

**Skateboard**: ship the thinnest working whole first (photo to 3D to AR), then grow it if time is left.

## Commands

```bash
# Gradle needs JDK 21 (Android Studio's bundled one works)
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

./gradlew assembleDebug      # build
./gradlew installDebug       # build + install on the USB phone (USB debugging on)
./gradlew lint               # Android Lint, run when you want it
~/Library/Android/sdk/platform-tools/adb devices -l   # is the phone visible?
```

No automated tests. A feature works when `installDebug` succeeds and you check it on the phone.

## Configuration

`local.properties` (git ignored, never commit): `sdk.dir`, `STABILITY_API_KEY` (read into `BuildConfig`), release signing values `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`. Never log the key.

## Specs

Stored in `docs/specs/`. Format: `docs/specs/NNNN-title.md` (or `NNNN-title/index.md`). Scope lives in `docs/scope/scope.md`.

## Rules

- SOLID OOP: small classes with one job (aim under 200 lines), composition over inheritance, interfaces only where a second implementation or a fake is real.
- Dependencies come in through constructors, wired in `AppContainer`. No service locator, no global singletons beyond the container.
- Folders by feature: `ui/<screen>/` holds `XxxScreen` + `XxxViewModel`; `data/` for network and storage; `image/` for photo prep.
- Null safety (gradual): no `!!` and no unchecked casts in app code. Compiler warnings are allowed.
- One error pattern: every failure becomes a sealed `GenerateError` (or a sibling sealed type per area) mapped to a user message with Retry. The UI never sees raw exceptions. Catch `SocketTimeoutException` before `IOException`.
- Screens pass only ids (`modelId`) between routes, never engines, models, or bitmaps.
- Check real state on resume (permissions, ARCore availability), never trust a saved value.
- Follow the `sceneview` skill for every SceneView API; don't guess names.

## Tooling

Android Lint only (built in), formatting with Android Studio's Kotlin style. No pre-commit hook, no CI, no test suite (Prototype workflow).

## Git

- integration: off (you handle git and GitHub yourself)

## Agent skills

- [sceneview](.claude/skills/sceneview/): `sceneview/sceneview`, SceneView 4.69.6 Compose APIs for 3D preview and AR placement (authoritative)
- [android-permissions-security](.claude/skills/android-permissions-security/): `android/skills`, runtime permissions and manifest component security
- [android-intent-security](.claude/skills/android-intent-security/): `android/skills`, safe Intent handling (camera, FileProvider)
- [edge-to-edge](.claude/skills/edge-to-edge/): `android/skills`, insets and system bars in Compose
- [jetpack-compose-performance](.claude/skills/jetpack-compose-performance/): `android/skills`, recomposition and jank fixes
- [agp-9-upgrade](.claude/skills/agp-9-upgrade/): `android/skills`, AGP 9 build file changes
- [r8-analyzer](.claude/skills/r8-analyzer/): `android/skills`, keep rules if you ever turn on R8
- [android-cli](.claude/skills/android-cli/): `android/skills`, the `android` CLI (devices, screenshots, SDK)

Declined: android-mcp-server, mcp-stable-fast-3d

## Context files

<!-- Nested AGENTS.md files are listed here as they are created -->

_Drafted by /audit from the repo, worth a quick human pass. Edit freely: once a line stops matching this draft, later runs treat it as curated and will flag rather than overwrite it._
