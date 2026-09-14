# Potrace-traced coloring templates — the correct process

This package exists because the first potrace-derived template ("Funny
Cat") broke the app's tap-to-color model four separate ways before it
actually worked, all stemming from one fact:

> **potrace's raw trace is pure ink-stroke line art. It has no enclosed
> fill shapes.** This app's coloring model (`RegionSpec` — a fillable
> `Path` + a tap hit-polygon) assumes enclosed shapes, the way every
> hand-authored template (`SunnyDayPaths`, `SleepyCatPaths`, etc.) is
> drawn. A raw potrace trace does not satisfy that assumption, no matter
> how it's wired up.

Follow this process for the next traced template. It is not optional
ceremony — every step below exists because skipping it broke something,
concretely, on-device, in this project's history.

## Pipeline

**1. PNG → traced SVG.** `python3 scripts/png_to_svg.py input.png traced.svg`
(installs `potrace` via brew if missing). This is pure ink, not usable
as-is — do not wire it into `TemplateCatalog` directly.

**2. Inspect what potrace actually produced.**
```
python3 scripts/potrace-template/inspect_traced_paths.py traced.svg out.html
```
Read the printed bounding boxes (nested/nearby boxes = probably the same
visual feature, e.g. an eye's ring + pupil) and/or open `out.html`. Decide:
- Which path index is the main connected ink network → this is always
  `outline` (index 0 in practice; potrace emits the biggest connected
  component first).
- Which small clusters of indices are named features that need a **solid**
  fill (eyes, a belly patch, anything a kid would want to tap and see fill
  in one color) — group their indices together.

**3. Write a config and generate.**
```
python3 scripts/potrace-template/generate_template.py config.json
```
See `example_config.json`. This derives every solid region (rasterize →
flood-fill from the border → re-trace the leftover silhouette with
potrace — see `potrace_lib.make_solid_region_svg`), computes a **correct**
hit polygon for every ink region (see "The `toHitPolygon()` bug" below —
do not let anything call `.toHitPolygon()` on a multi-subpath ink Path),
and writes one `.kt` file with everything normalized into the same 320×320
canvas.

**4. Wire it into `TemplateCatalog.kt`.** For every region:
- Solid regions (body/eyes/patches/etc): normal `RegionSpec(id, path,
  path.toHitPolygon())` — these ARE single-subpath (potrace re-traced
  them cleanly), so the shared util is safe here.
- Ink regions (`outline`, `detail1..N`): use the generated
  `{name}HitPolygon` val, **not** `.toHitPolygon()`. Pass `strokeWidth =
  0f` (the ink fill IS the line at its own width — an extra boundary
  stroke doubles/bloats it) and `fillsWithOutlineByDefault = true` (so it
  reads as a drawn line immediately, not only once tapped). Pass
  `isDecorative = true` on any ink region whose visual area is already
  covered by a solid region (the generated file's doc comment flags these
  as "`[isDecorative candidate]`") — otherwise a tap in the overlap
  recolors the thin ink instead of filling the solid region beneath it,
  *even with a correctly-computed small hit polygon* (draw order and
  tap-priority share one list; `isDecorative` decouples them — see
  `RegionCanvas.kt`'s tap handler).

**5. Build.**
```
./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:testDebugUnitTest
```

**6. Verify on a real device/emulator — every item, every time:**
1. Install, then **clear app data** or hit "Start over" (a stale cached
   fill from a previous test run hides default-appearance bugs).
2. Tap an empty background corner → only the canvas background recolors;
   the character stays visually intact on top.
3. Tap the open interior of the main silhouette → the solid `body`-style
   region fills, not `background` and not `outline`.
4. Tap each named solid feature (eye, patch, etc.) → it fills solid, not
   just its thin ink ring/boundary.
5. Look at line weight fresh (uncolored) — single clean stroke, matching
   every other template. No doubled/bold edges, no merged-together
   closely-spaced lines.
6. **Zoom into every small ink detail in the clean/uncolored state** —
   eyebrow, ring, crease, whatever — not just the main outline. A region
   can render fine in the gallery thumbnail while being genuinely
   invisible in the real coloring screen (see problem class C below); this
   only shows up if you actually zoom in, a full-size screenshot glance is
   not enough.
7. Re-check #3, #4 and #6 from a couple of different tap points within
   each region (not just dead center) — hit-polygon bugs are positional
   and a single lucky tap can pass by accident.

If any item fails, find the *root cause* (it's one of the three problem
classes below) and fix it there — don't patch the one visible symptom and
move on; the other checklist items can still be silently broken.

## The three problem classes, and their fixes

**A. Missing solid fill regions.** Ink alone can't be "filled in" the way
a kid expects — there's no enclosed shape to fill (i.e. `RegionSpec`s
"body"). Fix: `potrace_lib.make_solid_region_svg` (rasterize the ink →
flood-fill from the border → re-trace what's left with potrace).

**B. Hit-testing bugs, two distinct causes — check for BOTH:**
- **`toHitPolygon()`'s first-contour-only bug.** Compose's `PathMeasure`
  has no `nextContour()` (confirmed by decompiling
  `AndroidPathMeasure.class` — `setPath` delegates straight to
  `android.graphics.PathMeasure.setPath`, no contour loop). So
  `Path.toHitPolygon()` (in `geometry/PathSampling.kt`) silently only
  samples a Path's FIRST subpath. Every hand-authored template's regions
  happen to be single-subpath, so this never surfaced before potrace's
  multi-subpath ink. Fix: `potrace_lib.emit_hit_polygon_kotlin` computes a
  real hit polygon from the ink region's SMALLEST subpath instead — still
  honest and correctly-sized, just deliberately small.
- **Draw-order and tap-priority sharing one list.** Even with a correctly
  small hit polygon, ink decoration drawn *on top* of a solid fill (same
  list, later position = both "drawn on top" AND "wins ties in
  `lastOrNull`") will still steal a tap that lands in the overlap. Fix:
  `RegionSpec.isDecorative = true` — the tap handler in `RegionCanvas.kt`
  prefers any non-decorative match at the same point over a decorative
  one, without touching visual draw order.

**C. `RegionCanvas`'s default-fill resolution ignoring
`fillsWithOutlineByDefault`.** The non-interactive gallery preview
(`TemplatePreview`) calls `drawTemplate` directly with a sparse fills map,
so its own `fills[region.id] ?: (if fillsWithOutlineByDefault outlineColor
else unfilledColor)` fallback works correctly. The interactive
`RegionCanvas`, however, pre-resolves EVERY region through
`animateColorAsState(targetValue = fills[region.id] ?: unfilledColor)`
before calling `drawTemplate` — that fully-populated map means
`drawTemplate`'s own fallback branch never triggers, so any
`fillsWithOutlineByDefault` region silently renders as `unfilledColor`
(invisible against the page background) in the real screen while looking
correct in the gallery thumbnail. Fix (already applied in
`RegionCanvas.kt`): make the `animateColorAsState` target check
`fillsWithOutlineByDefault` too, matching `drawTemplate`'s own fallback.
This bug first hid Funny Cat's solid pupil dots (masked by their own ring
ink, easy to miss without zooming in) and was only caught for real on
Elephant, where it took out the eyebrow/ear-fold/eye ink entirely —
checklist item 6 exists because of this.

All three of these are real latent bugs in shared app code
(`RegionCanvas`/`PathSampling`), not something specific to any one
template — but A and B only ever manifest for genuinely multi-subpath
regions, which today means "potrace-derived ink," and C only manifests for
`fillsWithOutlineByDefault` regions, which today also means
"potrace-derived ink." If a template needs either of these for some other
reason later, the same fixes apply.
