import { StrictMode, type ComponentType } from 'react'
import { createRoot } from 'react-dom/client'

// Shows one visual: /?v=<folder name>, the README hero by default.
// The recorder (record.mjs) installs the kit's virtual clock first, then calls
// window.__mountVisual, so it can step the scene frame by frame.
const visuals = import.meta.glob<{ default: ComponentType }>('./*/index.tsx')

async function mount(name: string) {
  const load = visuals[`./${name}/index.tsx`]
  if (!load) throw new Error(`Unknown visual: ${name}`)
  const { default: Visual } = await load()
  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <div id="example">
        <Visual />
      </div>
    </StrictMode>,
  )
}

const params = new URLSearchParams(location.search)
const name = params.get('v') ?? 'dimenzy-hero'
if (params.has('record')) (window as unknown as { __mountVisual: () => Promise<void> }).__mountVisual = () => mount(name)
else void mount(name)
