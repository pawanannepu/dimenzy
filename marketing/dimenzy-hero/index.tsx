import { useEffect, useRef } from 'react'
import { Button, Expand, Phone, Stage, Text, bezier, clock, useReducedMotion, type Act } from '../kit'
import './dimenzy.css'

// Dimenzy's README hero: photo to 3D model to AR, on one Android phone.
//
// Beats (about 14s, looping):
// 1. Capture: the fridge photo sits in the photo panel. Tap "Generate 3D model".
// 2. The panel shrinks into the generating card: the photo becomes its
//    thumbnail, "Making your 3D model", progress fills.
// 3. The card opens into the 3D preview (one object, Expand with the card as
//    its face); the model turns.
// 4. Tap "Place in AR": the camera view, the floor's plane dots. Tap the
//    floor: the fridge stands on it, the dots fade, the size controls arrive.
//    The payoff holds.
// 5. Back (AR to preview), then New photo (preview to capture): the app's
//    real navigation is the loop's reset, so nothing rewinds without a cause.
//    The capture screen keeps its photo, as the app's ViewModel does.
//
// Every screen copies the app's Compose layout in dp (the Pixel frame lays
// the screen out at 412 x 915 dp). Real strings: Dimenzy, Generate 3D model,
// Take photo, Choose from gallery, Try a sample model, Making your 3D model,
// Usually under a minute, Cancel, Drag to spin, pinch to zoom, Place in AR,
// New photo, the AR hint, 100%, Reset.

type S = {
  gen: boolean // capture's panel has become the generating card
  progress: number // 0 to 1
  launch: boolean // the preview layer is up (closed, it is the card's face)
  open: boolean // the card has opened into the preview
  ar: boolean
  placed: boolean
  leaving: boolean // the preview fading away to capture (New photo)
  spin: boolean // the model turning; restarts each time the preview is shown, as the app recreates its 3D view
}

const START: S = { gen: false, progress: 0, launch: false, open: false, ar: false, placed: false, leaving: false, spin: false }
const POSTER: S = { gen: true, progress: 1, launch: true, open: true, ar: true, placed: true, leaving: false, spin: false }

const PHOTO = new URL('./assets/photo.webp', import.meta.url).href
const ROOM = new URL('./assets/room.webp', import.meta.url).href
const PLANE = new URL('./assets/plane.webp', import.meta.url).href
const PLACED = new URL('./assets/placed.webp', import.meta.url).href
const TURN = new URL('./assets/turntable.webp', import.meta.url).href

// Where the rendered floor spot lands on the 412 x 915 screen (from the render's camera)
const SPOT = { x: 223, y: 473 }
// Where the finger rests between loops (stage px)
const REST = { x: 200, y: 720 }

async function script(a: Act<S>) {
  await a.wait(1200)
  a.mark('generate')
  await a.click({ at: 'generate', x: 0.7, y: 0.6 })
  a.put({ gen: true })
  await a.wait(550)
  a.put({ progress: 1 })
  await a.wait(2750)
  a.mark('open')
  // The preview layer comes up closed: its face is the card, drawn exactly over it
  await a.snap({ launch: true, spin: true })
  a.put({ open: true })
  await a.wait(2500)
  a.mark('ar')
  await a.click({ at: 'place', x: 0.72, y: 0.6 })
  a.put({ ar: true })
  await a.wait(500)
  // The camera view covers the preview now: its 3D view stops, unseen
  await a.snap({ spin: false })
  await a.wait(600)
  a.mark('place')
  await a.click('floor')
  a.put({ placed: true })
  await a.wait(2600)
  a.mark('back')
  await a.click('back')
  a.put({ ar: false, spin: true })
  await a.wait(600)
  // The AR screen has faded: clear its placement unseen
  await a.snap({ placed: false })
  await a.wait(500)
  a.mark('newphoto')
  await a.click({ at: 'newphoto', x: 0.7, y: 0.6 })
  // Capture comes back under the preview, as the app keeps it: set it unseen, then uncover it
  await a.snap({ gen: false, progress: 0 })
  a.put({ leaving: true })
  await a.wait(500)
  await a.snap({ launch: false, open: false, leaving: false, spin: false })
  // The finger lifts away to where the loop began; touch draws nothing between taps
  await a.move(REST)
  await a.wait(300)
}

function ArrowBack() {
  return (
    <svg viewBox="0 0 24 24" width="24" height="24" aria-hidden>
      <path fill="currentColor" d="M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z" />
    </svg>
  )
}

function ZoomOut() {
  return (
    <svg viewBox="0 0 24 24" width="24" height="24" aria-hidden>
      <path fill="currentColor" d="M15.5,14h-0.79l-0.28,-0.27C15.41,12.59 16,11.11 16,9.5 16,5.91 13.09,3 9.5,3S3,5.91 3,9.5 5.91,16 9.5,16c1.61,0 3.09,-0.59 4.23,-1.57l0.27,0.28v0.79l5,4.99L20.49,19l-4.99,-5zM9.5,14C7.01,14 5,11.99 5,9.5S7.01,5 9.5,5 14,7.01 14,9.5 11.99,14 9.5,14zM7,9h5v1L7,10z" />
    </svg>
  )
}

function ZoomIn() {
  return (
    <svg viewBox="0 0 24 24" width="24" height="24" aria-hidden>
      <path fill="currentColor" d="M15.5,14h-0.79l-0.28,-0.27C15.41,12.59 16,11.11 16,9.5 16,5.91 13.09,3 9.5,3S3,5.91 3,9.5 5.91,16 9.5,16c1.61,0 3.09,-0.59 4.23,-1.57l0.27,0.28v0.79l5,4.99L20.49,19l-4.99,-5zM9.5,14C7.01,14 5,11.99 5,9.5S7.01,5 9.5,5 14,7.01 14,9.5 11.99,14 9.5,14zM12,10h-2v2L9,12v-2L7,10L7,9h2L9,7h1v2h2v1z" />
    </svg>
  )
}

// The model, as if dragged: 72 rendered frames 5 degrees apart (frame 0
// faces front), 9 across and 8 down. Each time the preview is shown it eases
// from 65 degrees left of front to 65 right, then rests, so the front is in
// view whenever Place in AR is tapped. Timed on the stage's clock.
const COLS = 9
const ROWS = 8
const FRAMES = 72
const SWING = { from: -65, to: 65, ms: 3000 }
const swingEase = bezier([0.45, 0, 0.25, 1])
function frameAt(deg: number) {
  const i = (((Math.round(deg / 5) % FRAMES) + FRAMES) % FRAMES)
  return `${((i % COLS) / (COLS - 1)) * 100}% ${(Math.floor(i / COLS) / (ROWS - 1)) * 100}%`
}
function Turntable({ spin }: { spin: boolean }) {
  const ref = useRef<HTMLSpanElement>(null)
  const reduced = useReducedMotion()
  useEffect(() => {
    const el = ref.current
    if (!el) return
    if (!spin || reduced) {
      el.style.backgroundPosition = frameAt(spin ? SWING.to : SWING.from)
      return
    }
    const t0 = clock.now()
    let id = 0
    const tick = () => {
      const p = Math.min(1, (clock.now() - t0) / SWING.ms)
      el.style.backgroundPosition = frameAt(SWING.from + (SWING.to - SWING.from) * swingEase(p))
      if (p < 1) id = requestAnimationFrame(tick)
    }
    tick()
    return () => cancelAnimationFrame(id)
  }, [spin, reduced])
  return <span ref={ref} className="dz-turn" style={{ backgroundImage: `url(${TURN})`, backgroundPosition: frameAt(SWING.from) }} />
}

// The photo panel, and the generating card it becomes: one element. Its photo
// shrinks into the thumbnail, the card's words and progress fade in after.
// `handed`: the preview has taken the card over as its face, so this one hides.
function Panel({ gen, progress, face, handed }: { gen: boolean; progress: number; face?: boolean; handed?: boolean }) {
  return (
    <div className="dz-panel" data-gen={gen || undefined} data-face={face || undefined} data-handed={handed || undefined} data-target={face ? undefined : 'card'}>
      <img className="dz-panel__photo" src={PHOTO} alt="" />
      <div className="dz-panel__text">
        <span className="dz-panel__title">Making your 3D model</span>
        <span className="dz-panel__sub">Usually under a minute</span>
      </div>
      <span className="dz-progress">
        <span className="dz-progress__fill" style={{ transform: `scaleX(${progress})` }} />
      </span>
    </div>
  )
}

export default function DimenzyHero() {
  return (
    <Stage<S>
      width={400}
      height={692}
      compact={{ below: 400, width: 358, height: 694 }}
      initial={START}
      poster={POSTER}
      script={script}
      cursor={{ at: REST, style: 'touch' }}
      className="dz"
    >
      {(s, rect, layout) => (
        <>
          <div className="dz-wash" />
          <Phone frame="android" width={layout === 'compact' ? 326 : 312} dark={s.ar} className="dz-phone">
            <div className="dz-screens">
              {/* Capture, then generating: the same screen, its panel becoming the card */}
              <div className="dz-capture" data-gen={s.gen || undefined}>
                <div className="dz-head dz-fades">
                  <div className="dz-title">Dimenzy</div>
                  <Text filler w={268} h={9} className="dz-sub">Turn a photo of an object into a 3D model.</Text>
                </div>
                <Panel gen={s.gen} progress={s.progress} handed={s.launch && s.gen} />
                <div className="dz-hint dz-fades">
                  <Text filler w={238} h={8}>One object, plain background, good light.</Text>
                </div>
                <div className="dz-actions dz-fades">
                  <Button variant="primary" className="dz-btn dz-btn--filled dz-btn--tall" data-target="generate">Generate 3D model</Button>
                  <Button className="dz-btn dz-btn--outlined" data-target="take">Take photo</Button>
                  <Button className="dz-btn dz-btn--outlined">Choose from gallery</Button>
                  <Button variant="ghost" className="dz-btn dz-btn--text">Try a sample model</Button>
                </div>
                <div className="dz-cancel">
                  <Button className="dz-btn dz-btn--outlined dz-btn--hug">Cancel</Button>
                </div>
              </div>

              {/* The 3D preview, opened out of the card */}
              <div className="dz-preview" data-launch={s.launch || undefined} data-spin={s.spin || undefined} data-leaving={s.leaving || undefined}>
                <Expand
                  className="dz-expand"
                  open={s.open}
                  from={rect('card')}
                  radius={24}
                  duration={640}
                  face={<Panel gen progress={1} face />}
                >
                  <div className="dz-view">
                    <Turntable spin={s.spin} />
                    <div className="dz-ready">
                      <span className="dz-ready__hint">Drag to spin, pinch to zoom</span>
                      <Button variant="primary" className="dz-btn dz-btn--filled dz-btn--tall" data-target="place">Place in AR</Button>
                      <Button className="dz-btn dz-btn--outlined" data-target="newphoto">New photo</Button>
                    </div>
                  </div>
                </Expand>
              </div>

              {/* AR: the camera view of the room */}
              <div className="dz-ar" data-on={s.ar || undefined} data-placed={s.placed || undefined}>
                <img className="dz-ar__room" src={ROOM} alt="" />
                <img className="dz-ar__plane" src={PLANE} alt="" />
                <img className="dz-ar__model" src={PLACED} alt="" style={{ transformOrigin: `${SPOT.x}px ${SPOT.y + 30}px` }} />
                <span className="dz-ar__floor" data-target="floor" style={{ left: SPOT.x - 40, top: SPOT.y - 20 }} />
                <span className="dz-back" data-target="back" data-cursor="pointer">
                  <ArrowBack />
                </span>
                <div className="dz-ar__controls">
                  <div className="dz-slider dz-ar__rise" style={{ ['--i' as string]: 0 }}>
                    <ZoomOut />
                    <span className="dz-slider__track">
                      <span className="dz-slider__active" />
                      <span className="dz-slider__thumb" />
                      <span className="dz-slider__rest" />
                    </span>
                    <ZoomIn />
                    <span className="dz-slider__value">100%</span>
                  </div>
                  <span className="dz-ar__hint dz-ar__rise" style={{ ['--i' as string]: 1 }}>Drag to move, pinch to resize, twist to rotate.</span>
                  <span className="dz-ar__rise" style={{ ['--i' as string]: 2 }}>
                    <Button className="dz-btn dz-btn--tonal dz-btn--hug">Reset</Button>
                  </span>
                </div>
              </div>
            </div>
          </Phone>
        </>
      )}
    </Stage>
  )
}
