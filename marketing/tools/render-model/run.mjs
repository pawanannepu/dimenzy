// Renders a .glb into the hero's image layers with three.js in headless Chrome.
// Usage (from marketing/): node tools/render-model/run.mjs <model.glb> <outDir> [photo] [room] [turn]
// Writes photo.jpg, room.jpg, placed.png, plane.png, spot.json (where the floor spot lands, 0..1)
// and turn/NN.png (72 frames, 5 degrees apart). Pack them as dimenzy-hero/assets/*.webp with ffmpeg.
import { chromium } from 'playwright-core'
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const here = dirname(fileURLToPath(import.meta.url))
const [glb, out, ...what] = process.argv.slice(2)
mkdirSync(out, { recursive: true })
const want = (k) => !what.length || what.includes(k)

const browser = await chromium.launch({
  executablePath: process.env.CHROME_PATH ?? '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
  args: ['--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--ignore-gpu-blocklist'],
})
const page = await browser.newPage()
page.on('console', (m) => console.log('[page]', m.text()))
page.on('pageerror', (e) => console.log('[error]', e.message))
await page.route('http://render.local/**', (route) => {
  const url = new URL(route.request().url())
  if (url.pathname === '/fridge.glb') return route.fulfill({ body: readFileSync(glb), contentType: 'model/gltf-binary' })
  return route.fulfill({ body: readFileSync(join(here, 'render.html')), contentType: 'text/html' })
})
await page.goto('http://render.local/')
await page.waitForFunction(() => window.ready === true, null, { timeout: 60000 })

const save = (name, dataUrl) => writeFileSync(join(out, name), Buffer.from(dataUrl.split(',')[1], 'base64'))

if (want('photo')) save('photo.jpg', await page.evaluate(() => window.photo(720)))
if (want('room')) {
  save('room.jpg', await page.evaluate(() => window.room(824, 1830)))
  save('placed.png', await page.evaluate(() => window.placed(824, 1830)))
  save('plane.png', await page.evaluate(() => window.plane(824, 1830)))
  writeFileSync(join(out, 'spot.json'), JSON.stringify(await page.evaluate(() => window.spot())))
}
if (want('turn')) {
  const frames = await page.evaluate(() => window.turntable(72, 360, 520))
  mkdirSync(join(out, 'turn'), { recursive: true })
  frames.forEach((f, i) => save(`turn/${String(i).padStart(2, '0')}.png`, f))
}
await browser.close()
console.log('done')
