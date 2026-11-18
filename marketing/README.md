# marketing

Product visuals for Dimenzy (README hero, landing page, Play Store graphics), built with the
[stageshow](../.claude/skills/stageshow/) skill. React + CSS, separate from the Android app in `app/`.

One folder per visual, e.g. `dimenzy-hero/`: the component, its assets, its rendered outputs and the
skill's `REHEARSAL.md`. `kit/` is stageshow's React kit, copied here and shared by every visual.

## Preview and record

Needs Node 20.19+, Google Chrome (or `CHROME_PATH`) and ffmpeg.

```sh
cd marketing
npm install
npm run dev                  # preview at the printed URL; ?v=<folder> picks a visual
npm run record               # re-records dimenzy-hero.{gif,webp,mp4,png}
npm run typecheck
```

`record.mjs` steps the kit's virtual clock frame by frame, so the recording is exact and loops
seamlessly. To check a visual with stageshow's film tool, run `npm run dev` here and point
`.claude/skills/stageshow/scripts/film.mjs` at the URL (`--gate`, `--width 390`, `--states`).

## The README hero

`dimenzy-hero/`: photo, then 3D model, then the model in AR, on one Android phone, in a 17.5s loop.

| File | Use |
|---|---|
| `dimenzy-hero.gif` | README (1.6 MB, 400px, 15fps) |
| `dimenzy-hero.webp` | Sharper and smaller (870 KB, 2x, 30fps); most browsers |
| `dimenzy-hero.mp4` | Sites and slides (640 KB, 2x, 30fps) |
| `dimenzy-hero.png` | The still (the AR payoff), for reduced motion or a thumbnail |

```md
<p align="center"><img src="marketing/dimenzy-hero/dimenzy-hero.gif" width="400" alt="Dimenzy turning a photo of a fridge into a 3D model and placing it on a floor in AR"></p>
```

Its images are renders of a `.glb` model made with `tools/render-model/` (three.js in headless
Chrome); see that script's header to re-render with another model.

## Credits

- Fridge model: "House Props : Fridge" by Whitend, [Sketchfab](https://sketchfab.com/3d-models/house-props-fridge-2fdaa56bbd85404cb4206dcaedc16658), CC BY 4.0. Credit it wherever the hero is published.
- Roboto: Google, SIL Open Font License 1.1 (`dimenzy-hero/assets/roboto-latin.woff2`).
- stageshow kit: benjitaylor/skills, MIT (`kit/LICENSE.stageshow`).
