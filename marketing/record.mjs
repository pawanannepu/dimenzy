// Records one seamless loop of a visual into a GIF, an animated WebP, an MP4
// and a poster PNG, next to its source.
//
//   npm run record                     # the README hero
//   npm run record -- dimenzy-hero     # a visual by folder name
//
// It starts Vite, installs the stageshow kit's virtual clock before the scene
// mounts, and steps the clock one frame at a time, so every frame is exact
// and the loop length is measured, not guessed: from one `generate` mark
// (act.mark) to the next. Needs Google Chrome (or CHROME_PATH) and ffmpeg.
import { createServer } from 'vite'
import { chromium } from 'playwright-core'
import { execFileSync } from 'node:child_process'
import { mkdtempSync, rmSync, copyFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const here = dirname(fileURLToPath(import.meta.url))
const name = process.argv[2] ?? 'dimenzy-hero'
const LOOP_MARK = 'generate'
const FPS = 30
const WIDTH = 400 // the stage's design width, drawn 1:1
const CHROME = process.env.CHROME_PATH ?? '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome'

const server = await createServer({ root: here, logLevel: 'error', server: { host: '127.0.0.1', port: 0 } })
await server.listen()
const url = server.resolvedUrls.local[0]
const browser = await chromium.launch({ executablePath: CHROME })
const frames = mkdtempSync(join(tmpdir(), `${name}-frames-`))

try {
  const page = await browser.newPage({ viewport: { width: WIDTH + 32, height: 900 }, deviceScaleFactor: 2 })
  page.on('pageerror', (e) => console.error('[page]', e.message))
  await page.goto(`${url}?v=${name}&record`)
  await page.waitForFunction(() => '__mountVisual' in window)
  await page.evaluate(async (fps) => {
    const { installVirtualClock } = await import('/kit/clock.ts')
    installVirtualClock({ fps })
    await window.__mountVisual()
  }, FPS)
  // Images and the font load on real time; the scene waits on virtual time
  await page.waitForSelector('.ss-stage')
  await page.evaluate(async () => {
    await document.fonts.ready
    await Promise.all([...document.images].map((i) => i.decode().catch(() => {})))
    await window.__stageshow.clock.settle()
    if (window.__stageshow.clock.layoutPending) await window.__stageshow.clock.flushLayout()
  })
  const stage = await page.$('.ss-stage')

  const step = () =>
    page.evaluate(async () => {
      const c = window.__stageshow.clock
      await c.step(1)
      if (c.layoutPending) await c.flushLayout()
      return document.querySelector('.ss-stage')?.getAttribute('data-mark') ?? ''
    })

  let mark = ''
  const starts = []
  let poster = null
  for (let f = 0; f < FPS * 60; f++) {
    await stage.screenshot({ path: join(frames, `f${String(f).padStart(4, '0')}.png`) })
    const next = await step()
    if (next !== mark) {
      if (next === LOOP_MARK) starts.push(f + 1)
      // The payoff, held: the frame just before the scene leaves AR
      if (next === 'back' && poster === null) poster = f
      mark = next
    }
    if (starts.length === 2) break
  }
  if (starts.length < 2) throw new Error(`never saw the "${LOOP_MARK}" mark twice`)
  const loop = starts[1] - starts[0]
  console.log(`loop: ${loop} frames, ${(loop / FPS).toFixed(2)}s`)

  const out = (ext) => join(here, name, `${name}.${ext}`)
  const input = ['-y', '-loglevel', 'error', '-framerate', String(FPS), '-start_number', '0', '-i', join(frames, 'f%04d.png'), '-frames:v', String(loop)]
  // GIF: 15fps at 1x, one palette for the whole loop
  execFileSync('ffmpeg', [...input, '-vf', `fps=15,scale=${WIDTH}:-1:flags=lanczos,split[a][b];[a]palettegen=stats_mode=diff:max_colors=200[p];[b][p]paletteuse=dither=sierra2_4a:diff_mode=rectangle`, '-loop', '0', out('gif')])
  // Animated WebP: 30fps at 2x, much smaller than the GIF for the same look
  execFileSync('ffmpeg', [...input, '-c:v', 'libwebp_anim', '-quality', '78', '-loop', '0', out('webp')])
  // MP4: 30fps at 2x, for sites and slides
  execFileSync('ffmpeg', [...input, '-c:v', 'libx264', '-pix_fmt', 'yuv420p', '-crf', '20', '-movflags', '+faststart', out('mp4')])
  if (poster !== null) copyFileSync(join(frames, `f${String(poster).padStart(4, '0')}.png`), out('png'))
  console.log(`wrote ${name}.gif, .webp, .mp4, .png in ${join(here, name)}`)
} finally {
  await browser.close()
  await server.close()
  rmSync(frames, { recursive: true, force: true })
}
