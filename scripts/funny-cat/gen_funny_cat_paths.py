import re

SCRATCH_DIR = "/private/tmp/claude-501/-Users-atul-Documents-dev-BabyApp/ae8178ae-15a2-40c4-a9a1-36ae2a2e50a1/scratchpad"
SRC = f"{SCRATCH_DIR}/cat-test.svg"
BODY_SRC = f"{SCRATCH_DIR}/body_mask.svg"
EYE_R_SRC = f"{SCRATCH_DIR}/eyeR_mask.svg"
EYE_L_SRC = f"{SCRATCH_DIR}/eyeL_mask.svg"
BELLY_SRC = f"{SCRATCH_DIR}/bellyPatch_mask.svg"

TOKEN_RE = re.compile(r'([MmLlCcZz])|(-?\d+(?:\.\d+)?)')

def tokenize(d):
    return [ (k1 if k1 else float(k2)) for k1, k2 in TOKEN_RE.findall(d) ]

def parse_subpaths(d):
    toks = tokenize(d); i=0; subpaths=[]; cur=None; cx=cy=0.0; cmd=None
    while i < len(toks):
        t = toks[i]
        if isinstance(t, str):
            cmd = t; i += 1; continue
        if cmd in ('M','m'):
            x,y = toks[i], toks[i+1]; i += 2
            if cmd=='m' and cur is not None: x,y = cx+x, cy+y
            cx,cy = x,y; cur=[]; subpaths.append(cur); cur.append(('M',x,y))
        elif cmd in ('L','l'):
            x,y = toks[i], toks[i+1]; i += 2
            if cmd=='l': x,y = cx+x, cy+y
            cx,cy = x,y; cur.append(('L',x,y))
        elif cmd in ('C','c'):
            x1,y1,x2,y2,x,y = toks[i:i+6]; i += 6
            if cmd=='c':
                x1,y1 = cx+x1, cy+y1; x2,y2 = cx+x2, cy+y2; x,y = cx+x, cy+y
            cx,cy = x,y; cur.append(('C',x1,y1,x2,y2,x,y))
        elif cmd in ('Z','z'):
            cur.append(('Z',))
    return subpaths

def get_transform(data):
    m = re.search(r'translate\(([-\d.]+),([-\d.]+)\)\s*scale\(([-\d.]+),([-\d.]+)\)', data)
    return tuple(float(x) for x in m.groups())

def transform_subpaths(subpaths, tx, ty, sx, sy):
    def apply(x, y):
        return x * sx + tx, y * sy + ty
    out = []
    pts_for_bbox = []
    for sp in subpaths:
        tsp = []
        for seg in sp:
            if seg[0] in ('M', 'L'):
                x, y = apply(seg[1], seg[2])
                tsp.append((seg[0], x, y))
                pts_for_bbox.append((x, y))
            elif seg[0] == 'C':
                x1, y1 = apply(seg[1], seg[2])
                x2, y2 = apply(seg[3], seg[4])
                x, y = apply(seg[5], seg[6])
                tsp.append(('C', x1, y1, x2, y2, x, y))
                pts_for_bbox += [(x1, y1), (x2, y2), (x, y)]
            else:
                tsp.append(seg)
        out.append(tsp)
    return out, pts_for_bbox

def load_single_path_subpaths(svg_path):
    data = open(svg_path).read()
    tx, ty, sx, sy = get_transform(data)
    raw_paths = re.findall(r'<path d="([^"]+)"', data, re.S)
    assert len(raw_paths) == 1, svg_path
    subpaths = parse_subpaths(raw_paths[0])
    return transform_subpaths(subpaths, tx, ty, sx, sy)

# ---- original 9 traced paths (outline + 8 details) ----
data = open(SRC).read()
tx, ty, sx, sy = get_transform(data)
raw_paths = re.findall(r'<path d="([^"]+)"', data, re.S)

all_transformed = []
all_pts = []
for d in raw_paths:
    subpaths = parse_subpaths(d)
    tsub, pts = transform_subpaths(subpaths, tx, ty, sx, sy)
    all_transformed.append(tsub)
    all_pts += pts

xs = [p[0] for p in all_pts]; ys = [p[1] for p in all_pts]
minx, maxx = min(xs), max(xs)
miny, maxy = min(ys), max(ys)

target = 280.0
scale = target / max(maxx - minx, maxy - miny)
offx = (320.0 - (maxx - minx) * scale) / 2.0 - minx * scale
offy = (320.0 - (maxy - miny) * scale) / 2.0 - miny * scale

def norm(x, y):
    return x * scale + offx, y * scale + offy

def fmt(v):
    return f"{v:.2f}f"

def emit_path_kotlin(name, subpaths):
    lines = [f"    val {name}: Path = Path().apply {{"]
    for sp in subpaths:
        for seg in sp:
            if seg[0] == 'M':
                x, y = norm(seg[1], seg[2])
                lines.append(f"        moveTo({fmt(x)}, {fmt(y)})")
            elif seg[0] == 'L':
                x, y = norm(seg[1], seg[2])
                lines.append(f"        lineTo({fmt(x)}, {fmt(y)})")
            elif seg[0] == 'C':
                x1, y1 = norm(seg[1], seg[2])
                x2, y2 = norm(seg[3], seg[4])
                x, y = norm(seg[5], seg[6])
                lines.append(f"        cubicTo({fmt(x1)}, {fmt(y1)}, {fmt(x2)}, {fmt(y2)}, {fmt(x)}, {fmt(y)})")
            elif seg[0] == 'Z':
                lines.append("        close()")
    lines.append("    }")
    return "\n".join(lines)

def flatten_subpath(sp, n=12):
    """Sample a subpath (already page-space transformed) into a flat point list."""
    pts = []
    cur = None
    for seg in sp:
        if seg[0] in ('M', 'L'):
            cur = (seg[1], seg[2])
            pts.append(cur)
        elif seg[0] == 'C':
            x0, y0 = cur
            x1, y1, x2, y2, x3, y3 = seg[1:]
            for i in range(1, n + 1):
                t = i / n; mt = 1 - t
                bx = mt**3*x0 + 3*mt**2*t*x1 + 3*mt*t**2*x2 + t**3*x3
                by = mt**3*y0 + 3*mt**2*t*y1 + 3*mt*t**2*y2 + t**3*y3
                pts.append((bx, by))
            cur = (x3, y3)
    return pts

def bbox_area(pts):
    xs = [p[0] for p in pts]; ys = [p[1] for p in pts]
    return (max(xs) - min(xs)) * (max(ys) - min(ys))

def smallest_subpath_hit_polygon(subpaths):
    """Pick the smallest-bbox-area subpath (by design: these regions are
    potrace ink shapes with several disjoint subpaths — no single polygon
    represents the whole multi-part shape, and Compose's PathMeasure only
    ever samples the FIRST contour of a Path (no nextContour in its API),
    so Path.toHitPolygon() silently only covers subpath 0 today. Rather
    than let that arbitrarily-sized first subpath swallow taps that should
    hit the solid body/eye/bellyPatch regions beneath, deliberately use the
    smallest subpath: still a real, valid, appropriately-sized hit target
    for this decorative ink region, but unlikely to falsely shadow the
    larger solid regions it's layered on top of."""
    best = min(subpaths, key=lambda sp: bbox_area(flatten_subpath(sp)))
    return flatten_subpath(best, n=8)

def emit_hit_polygon_kotlin(name, subpaths):
    pts = smallest_subpath_hit_polygon(subpaths)
    offsets = ", ".join(f"Offset({fmt(x)}, {fmt(y)})" for x, y in (norm(px, py) for px, py in pts))
    return f"    val {name}HitPolygon: List<Offset> = listOf({offsets})"

names = ["outline"] + [f"detail{i}" for i in range(1, len(raw_paths))]

body_transformed, _ = load_single_path_subpaths(BODY_SRC)
eyeR_transformed, _ = load_single_path_subpaths(EYE_R_SRC)
eyeL_transformed, _ = load_single_path_subpaths(EYE_L_SRC)
belly_transformed, _ = load_single_path_subpaths(BELLY_SRC)

blocks = [
    emit_path_kotlin("body", body_transformed),
    emit_path_kotlin("eyeR", eyeR_transformed),
    emit_path_kotlin("eyeL", eyeL_transformed),
    emit_path_kotlin("bellyPatch", belly_transformed),
]
for name, subpaths in zip(names, all_transformed):
    blocks.append(emit_path_kotlin(name, subpaths))
    blocks.append(emit_hit_polygon_kotlin(name, subpaths))

kt = """package com.calmcoloring.app.content.generated

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path

/**
 * Compose [Path] data for the "Funny Cat" template, generated from a
 * potrace bitmap trace of a coloring-page PNG (see
 * `scripts/png_to_svg.py`), not hand-authored like the other templates.
 *
 * potrace's raw trace is pure ink-stroke line art with no enclosed
 * fill shapes, which breaks this app's tap-to-color model in two ways
 * fixed here by deriving *solid* regions separately (rasterize the
 * relevant ink subpaths, flood-fill from outside to find true
 * background, re-trace what's left with potrace — see
 * `scripts/funny-cat/gen_silhouette.py` and
 * `scripts/funny-cat/gen_solid_parts.py`):
 *  - `body`: the whole cat silhouette (ears/head/torso/limbs/tail all
 *    connect with no internal dividing line in this artwork). Without
 *    it, tapping anywhere inside the cat hits the full-canvas
 *    `background` region instead, since nothing else claims that area.
 *  - `eyeR`/`eyeL`/`bellyPatch`: solid fills merging each eye's ring +
 *    pupil ink shapes (and the belly patch's outline) into one filled
 *    blob, so tapping an eye colors a solid circle instead of just its
 *    thin ink boundary.
 *
 * `outline` is potrace's connected ink-stroke network (ears/head/body/
 * limbs/tail boundary + whiskers, 17 disjoint subpaths); `detail1..detail8`
 * are the ink blobs potrace split into separate components (eye
 * rings/pupils, nose/mouth, the belly patch's own thin outline; 2
 * subpaths each). These stay as thin ink decoration layered on top of
 * the solid fills above.
 *
 * `outlineHitPolygon`/`detail1HitPolygon`/etc: Compose's `PathMeasure`
 * has no `nextContour()` — `setPath`/`getLength`/`getPosition` only ever
 * see a Path's *first* contour (confirmed by decompiling
 * `AndroidPathMeasure.class`: `setPath` delegates straight to
 * `android.graphics.PathMeasure.setPath` with no contour loop). So the
 * shared `Path.toHitPolygon()` util every other region relies on would
 * silently sample only ONE of these regions' several disjoint subpaths
 * — and for `outline` specifically, whichever subpath happens to be
 * emitted first ends up an arbitrarily-sized false hit target that can
 * shadow the real `body`/`eyeR`/`eyeL`/`bellyPatch` fills underneath
 * (this is exactly what caused "tapping the body just recolors the
 * outline ink instead of filling"). Fix: for these regions specifically,
 * skip `toHitPolygon()` and use a hit polygon computed directly from
 * their *smallest* subpath instead (see `scripts/funny-cat/gen_funny_cat_paths.py`)
 * — still a real, validly-sized tap target, just deliberately small so
 * it doesn't falsely shadow the solid regions it's drawn on top of.
 */
object FunnyCatPaths {
    val background: Path = backgroundPath()

""" + "\n\n".join(blocks) + "\n}\n"

OUT = "/Users/atul/Documents/dev/BabyApp/composeApp/src/commonMain/kotlin/com/calmcoloring/app/content/generated/FunnyCatPaths.kt"
open(OUT, "w").write(kt)
print("wrote", OUT)
