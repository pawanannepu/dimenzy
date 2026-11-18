# Rehearsal: dimenzy-hero

Gold: `phone-touch` for the frame (one phone pinned from the top, centered, off the bottom; a card opening into its screen with `Expand`) and `loading` for the generating beat (real chrome throughout, only the work in progress changes). Kept: the phone-led composition, the touch cursor, the card-to-screen open, a loop reset caused by on-screen taps. Changed: an Android (Pixel) frame, Dimenzy's Material 3 tokens and Roboto, every screen laid out in dp from the app's Compose code, rendered imagery instead of art gradients.

Beats: capture (photo in the panel), tap Generate 3D model … the panel shrinks into the "Making your 3D model" card and progress fills … the card opens into the 3D preview, the model swings through its front … tap Place in AR, tap the floor, the fridge lands and the size controls arrive (payoff, held 2.6s) … Back, New photo, capture again. Loop 17.5s (526 frames at 30fps, measured by `record.mjs`).

Captured: wide 400 and phone 390 (compact 358) loops, `--states`, the reduced-motion poster, zooms of the capture screen, the generating card and the open beat (every animation frame traced). Light only: the stage carries its own ground, so the README's light and dark themes both show the same picture.

Gate (filmed in the stageshow playground; the copy here differs only in its kit import path):
- wide loop: `rehearsal passed (necessary, not sufficient: references/verify.md and the fresh eye in review.md still stand)`
- `--width 390`: `rehearsal passed`
- `--states`: `rehearsal passed`

Film:
- `tight control` (Choose from gallery 5.8px under Take photo): the app's 8dp gap draws under 8px at this scale. Fixed: 12dp between the outlined buttons.
- Doubled card at the open beat: Expand's face moved off the card it covered and uncovered the original. Fixed: the capture card hides once the preview has taken it over (`data-handed`).
- The model showed its back coils as Place in AR was tapped and through the AR crossfade. Fixed: an eased swing from 65 degrees left of front to 65 right (`Turntable`, on the stage clock), restarted each time the preview shows, as the app recreates its 3D view.
- `lands on a transform` on the AR controls: fixed with a lasting layer (`will-change: transform`).
- `loop seam: cursor does not end where it starts`: fixed, the finger returns to its rest point.
- Stands: `jump` on the New photo button at 16.3s. That is its press releasing during the reset snap, while the preview is already faded to 0; it is never seen.

Review: a self-review against `references/review.md` and `verify.md`, not an independent fresh eye. Findings and responses:
- The phone framing is the same one `phone-touch` uses. Chosen on purpose (the brief named that gold and asked for a phone-led hero).
- The stage has a ground (a pale blue wash) where the skill prefers the page as ground. Kept: a README is shown on light and dark themes, and a GIF cannot be transparent cleanly.
- The generating screen is sparse (the card and Cancel) for about three seconds. That is the real Generate screen's layout; kept.
- Reset in the AR controls sits about 11px above the stage's bottom edge. Whole, real, and the phone's bottom must run 24px off the stage; kept.

Assumptions:
- Stage 400 x 692 (compact 358 x 694), not the 400 x 600 asked for: every action sits 40dp from the bottom of the real screens, and 600 cut New photo and the AR controls.
- No in-app shutter exists (Take photo opens the system camera), so the story opens on the capture screen with the photo already taken, which is also where the app returns after New photo.
- The model is `assets/house_props__fridge.glb` (no chair asset in the repo), rendered with three.js: the photo, the 72-frame turntable and the AR room are renders, not captures from the app or a camera.
- The app's preview does not spin on its own ("Drag to spin"); the swing stands in for that drag.
- Progress is a determinate bar; the real Generate screen shows an indeterminate spinner.
- Colors are the full Material 3 scheme in `Color.kt`. The app currently wires only primary, primaryContainer, secondary and tertiary in `Theme.kt`, so on a device its surfaces and the tonal back button fall back to Material's default lavender.

Unresolved: none blocking.

Not checked: live pacing at real speed in a real browser tab (sheets, frame traces and the stepped recording only); the GIF as GitHub renders it in light and dark; a touch device; the compact layout inside a real narrow column (filmed at 390 only).
