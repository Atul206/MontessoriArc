# Publishing new coloring pages (OTA)

New artwork reaches the app without an app-store release. The pipeline:

1. **Draft the SVG.** Ask Claude for a new template by title/subject (e.g.
   "Montessori pouring jug") in the same style as the existing 8 files under
   `svg/`. The output must follow this exact subset of SVG — it's what
   `parseSvgDocument`/`parseSvgPathData` in
   `composeApp/src/commonMain/kotlin/com/calmcoloring/app/content/remote/`
   understand:
   - One `<svg viewBox="0 0 W H">` root, no nested groups or `transform`.
   - Direct `<rect>`, `<circle>`, and `<path>` children only, each with an
     `id` (region name — first element is conventionally `"background"`).
   - `<path d="...">` may use `M/m L/l H/h V/v Q/q C/c A/a Z/z`. Arc flags
     (the two `0`/`1` digits before the endpoint) must be space-separated —
     `"a30,30 0 0 1 58,-10"`, never digit-glued.
   - Save it as `svg/<id>.svg`.

2. **Add or bump its manifest entry** in `svg/manifest.json`: a new file
   needs a new `{ id, name, accent, file, contentVersion: 1 }` entry
   (`accent` is one of `SAGE`/`SKY`/`CLAY`/`SAND`/`LILAC`/`MOSS`); editing an
   existing file's art requires bumping that entry's `contentVersion` — the
   app only re-downloads a template when the manifest's `contentVersion`
   differs from what it has cached.

3. **Commit and push to `main`.** The app fetches
   `https://raw.githubusercontent.com/Atul206/MontessoriArc/main/svg/manifest.json`
   (and the `.svg` files it lists) on every launch
   (`ContentRepository.refresh()`, called from `App.kt`), diffs
   `contentVersion` against its local SQLDelight cache, downloads and caches
   whatever changed, and re-renders. No further action needed — nothing to
   run, deploy, or release.

Rendering itself always comes from the local cache (or, for the 8 launch
templates before any refresh has ever completed, the compiled-in
`TemplateCatalog.all`) — a refresh only updates what's cached for next time,
it never blocks what's on screen.

## Fixing a bad release

If a published piece is wrong but should still exist, **never** edit git
history or force-push — devices may have already fetched the bad commit, and
`raw.githubusercontent.com` has no CDN layer to invalidate anyway. Just fix
the `.svg` file and **bump its `contentVersion` upward** (never down — the
app only re-fetches when the manifest's number differs from what it has
cached, so a higher number is what makes every device pick up the fix on its
next launch).

## Removing content entirely

Use this when a piece shouldn't exist at all (wrong subject, quality issue,
whatever) rather than just needing a fix. Deleting its entry from
`templates` is **not enough** — a device that already cached it has no way to
learn it should stop being shown, since `ContentRepository.refresh()` never
prunes a cached id just because it disappeared from the manifest (that could
just as easily mean "no update available" as "delete this").

Instead, add the id to the top-level `removedIds` array:

```json
{
  "templates": [ /* ... entries for everything that should still exist ... */ ],
  "removedIds": ["some-bad-template-id"]
}
```

On next `refresh()`, every device deletes that id from its local
`TemplateCacheStore` and drops it from the gallery. If that id was one of
the 8 launch templates (i.e. it also exists in the compiled-in
`TemplateCatalog.all`), removing the cached override doesn't remove the
template entirely — it falls back to showing the original bundled version
again, since `ContentRepository` only hides a bundled entry while a cached
override for that same id exists. For a template that only ever existed as
OTA content (no bundled fallback), removing it makes it disappear from the
gallery, which is the correct "undo a bad publish" behavior.

Leave an id in `removedIds` permanently (don't clean it up later) — it costs
nothing on devices that never cached it, and removing it from the list would
let a device that's stuck offline for a long time re-download the entry if
it ever reappeared under `templates` by id collision with something new.
