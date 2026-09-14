"""Shared library for turning a potrace-traced SVG into a correctly-behaving
BabyApp coloring template.

This exists because the *first* potrace-derived template (Funny Cat) broke
the app's tap-to-color model in several ways that all trace back to one
fact: potrace's raw output is pure ink-stroke line art, not the enclosed
fill shapes this app's `RegionSpec` model expects. See
`scripts/potrace-template/README.md` for the full story and the mandatory
verification checklist — read that before generating a new template with
this library, and follow it after wiring the template into
`TemplateCatalog.kt`, on a real device/emulator, every time.

Everything here is pure-Python (stdlib + Pillow), no Kotlin/Compose
dependency, so it can run standalone to generate `.kt` source text.
"""

from __future__ import annotations

import os
import re
import subprocess
from dataclasses import dataclass, field

# ---------------------------------------------------------------------------
# SVG path parsing (potrace only ever emits M/m, L/l, C/c, Z/z — relative
# commands after the first M — so this parser deliberately does not handle
# the rest of the SVG path grammar (arcs, quadratics, shorthand curves).)
# ---------------------------------------------------------------------------

_TOKEN_RE = re.compile(r"([MmLlCcZz])|(-?\d+(?:\.\d+)?)")


def _tokenize(d: str) -> list:
    return [(k1 if k1 else float(k2)) for k1, k2 in _TOKEN_RE.findall(d)]


def parse_subpaths(d: str) -> list[list[tuple]]:
    """Parses one <path d="..."> into a list of subpaths (contours); each
    subpath is a list of ('M'|'L'|'C'|'Z', *raw_untransformed_coords)."""
    toks = _tokenize(d)
    i = 0
    subpaths: list[list[tuple]] = []
    cur: list[tuple] | None = None
    cx = cy = 0.0
    cmd = None
    while i < len(toks):
        t = toks[i]
        if isinstance(t, str):
            cmd = t
            i += 1
            continue
        if cmd in ("M", "m"):
            x, y = toks[i], toks[i + 1]
            i += 2
            if cmd == "m" and cur is not None:
                x, y = cx + x, cy + y
            cx, cy = x, y
            cur = []
            subpaths.append(cur)
            cur.append(("M", x, y))
        elif cmd in ("L", "l"):
            x, y = toks[i], toks[i + 1]
            i += 2
            if cmd == "l":
                x, y = cx + x, cy + y
            cx, cy = x, y
            cur.append(("L", x, y))
        elif cmd in ("C", "c"):
            x1, y1, x2, y2, x, y = toks[i:i + 6]
            i += 6
            if cmd == "c":
                x1, y1 = cx + x1, cy + y1
                x2, y2 = cx + x2, cy + y2
                x, y = cx + x, cy + y
            cx, cy = x, y
            cur.append(("C", x1, y1, x2, y2, x, y))
        elif cmd in ("Z", "z"):
            cur.append(("Z",))
        else:
            raise ValueError(f"unexpected potrace path command: {cmd!r}")
    return subpaths


def get_transform(svg_text: str) -> tuple[float, float, float, float]:
    """Extracts (tx, ty, sx, sy) from potrace's
    `transform="translate(tx,ty) scale(sx,sy)"` group attribute."""
    m = re.search(r"translate\(([-\d.]+),([-\d.]+)\)\s*scale\(([-\d.]+),([-\d.]+)\)", svg_text)
    if not m:
        raise ValueError("no potrace translate/scale transform found in SVG")
    return tuple(float(x) for x in m.groups())  # type: ignore[return-value]


def transform_subpaths(subpaths, tx, ty, sx, sy):
    """Applies the potrace group transform to every point, returning
    (transformed_subpaths, flat_point_list_for_bbox) in page-space
    (matching the SVG's own viewBox units)."""
    def apply(x, y):
        return x * sx + tx, y * sy + ty

    out = []
    pts_for_bbox = []
    for sp in subpaths:
        tsp = []
        for seg in sp:
            if seg[0] in ("M", "L"):
                x, y = apply(seg[1], seg[2])
                tsp.append((seg[0], x, y))
                pts_for_bbox.append((x, y))
            elif seg[0] == "C":
                x1, y1 = apply(seg[1], seg[2])
                x2, y2 = apply(seg[3], seg[4])
                x, y = apply(seg[5], seg[6])
                tsp.append(("C", x1, y1, x2, y2, x, y))
                pts_for_bbox += [(x1, y1), (x2, y2), (x, y)]
            else:
                tsp.append(seg)
        out.append(tsp)
    return out, pts_for_bbox


def load_traced_paths(svg_path: str):
    """Reads a potrace-produced SVG and returns
    (raw_d_strings, transformed_subpaths_per_path, tx, ty, sx, sy)."""
    data = open(svg_path).read()
    tx, ty, sx, sy = get_transform(data)
    raw_paths = re.findall(r'<path d="([^"]+)"', data, re.S)
    all_transformed = []
    all_pts = []
    for d in raw_paths:
        subpaths = parse_subpaths(d)
        tsub, pts = transform_subpaths(subpaths, tx, ty, sx, sy)
        all_transformed.append(tsub)
        all_pts += pts
    return raw_paths, all_transformed, all_pts, (tx, ty, sx, sy)


def load_single_path_subpaths(svg_path: str):
    """Like load_traced_paths but asserts exactly one <path> (the shape of
    output from `make_solid_region` / potrace on a single-color mask)."""
    raw_paths, all_transformed, all_pts, transform = load_traced_paths(svg_path)
    assert len(raw_paths) == 1, f"{svg_path}: expected exactly one <path>, found {len(raw_paths)}"
    return all_transformed[0], all_pts


# ---------------------------------------------------------------------------
# Curve flattening + bbox (used for the "smallest subpath" hit-polygon
# heuristic, and for rasterizing ink into a solid-region mask)
# ---------------------------------------------------------------------------

def flatten_subpath(sp, n: int = 12) -> list[tuple[float, float]]:
    """Samples one already page-space-transformed subpath into a flat
    (x, y) point list, straightening cubic Béziers into `n` segments each."""
    pts: list[tuple[float, float]] = []
    cur = None
    for seg in sp:
        if seg[0] in ("M", "L"):
            cur = (seg[1], seg[2])
            pts.append(cur)
        elif seg[0] == "C":
            x0, y0 = cur
            x1, y1, x2, y2, x3, y3 = seg[1:]
            for i in range(1, n + 1):
                t = i / n
                mt = 1 - t
                bx = mt**3 * x0 + 3 * mt**2 * t * x1 + 3 * mt * t**2 * x2 + t**3 * x3
                by = mt**3 * y0 + 3 * mt**2 * t * y1 + 3 * mt * t**2 * y2 + t**3 * y3
                pts.append((bx, by))
            cur = (x3, y3)
    return pts


def bbox_area(pts: list[tuple[float, float]]) -> float:
    xs = [p[0] for p in pts]
    ys = [p[1] for p in pts]
    return (max(xs) - min(xs)) * (max(ys) - min(ys))


def smallest_subpath(subpaths) -> list[tuple]:
    """Picks the subpath with the smallest bounding-box area.

    Why this exists: Compose's `PathMeasure` has no `nextContour()`
    (confirmed by decompiling `AndroidPathMeasure.class` — `setPath`
    delegates straight to `android.graphics.PathMeasure.setPath` with no
    contour loop), so the app's shared `Path.toHitPolygon()` util only ever
    samples a Path's *first* subpath. For a region whose Path has several
    disjoint subpaths (any potrace ink shape usually does), that silently
    produces an arbitrarily-sized, arbitrarily-positioned hit polygon —
    which is exactly what caused "tapping the body just recolors a chunk of
    the outline ink instead of filling the body" the first time around. Use
    the SMALLEST subpath instead: still a real, validly-sized, honestly
    computed hit target for that region, but deliberately small so it's
    very unlikely to accidentally shadow a bigger, more useful region drawn
    underneath it (which `RegionSpec.isDecorative` also guards against —
    use both; see the README checklist).
    """
    return min(subpaths, key=lambda sp: bbox_area(flatten_subpath(sp)))


# ---------------------------------------------------------------------------
# Deriving a *solid* region from ink: rasterize the chosen ink subpaths,
# flood-fill from the image border to find true background, keep everything
# else (ink + fully-enclosed holes), re-trace with potrace into one clean
# fill shape. This is how `body`/`eyeL`/`eyeR`/`bellyPatch` were derived for
# Funny Cat — potrace's raw ink alone has no enclosed fill regions at all.
# ---------------------------------------------------------------------------

def make_solid_region_svg(
    traced_svg_path: str,
    path_indices: list[int],
    out_svg_path: str,
    page_size: tuple[int, int] | None = None,
    turdsize: int = 2,
    opttolerance: float = 0.4,
) -> str:
    """Rasterizes the given <path> indices from `traced_svg_path` (potrace's
    raw trace), flood-fills to find true background, and re-traces the
    resulting silhouette mask with potrace, writing `out_svg_path`.

    Requires Pillow (`pip install pillow` / already used by
    `scripts/png_to_svg.py`) and the `potrace` CLI (`brew install potrace`).

    Returns `out_svg_path`. The output SVG has its OWN transform (from its
    own page_size) — normalize it into your target coordinate space with
    the SAME `load_single_path_subpaths` + your own scale/offset math used
    for the ink paths, so everything lands in one consistent canvas. See
    `generate_kotlin.py` for the full worked example.
    """
    from PIL import Image, ImageDraw, ImageOps

    raw_paths, all_transformed, _, _ = load_traced_paths(traced_svg_path)

    if page_size is None:
        # Infer from the traced SVG's own viewBox/width so the mask lines
        # up 1:1 with the ink's own page-space coordinates.
        data = open(traced_svg_path).read()
        m = re.search(r'viewBox="0 0 ([\d.]+) ([\d.]+)"', data)
        if not m:
            raise ValueError("could not infer page_size; pass it explicitly")
        page_size = (int(float(m.group(1))) + 1, int(float(m.group(2))) + 1)

    w, h = page_size
    img = Image.new("L", (w, h), 255)
    draw = ImageDraw.Draw(img)
    for idx in path_indices:
        for sp in all_transformed[idx]:
            pts = flatten_subpath(sp)
            if len(pts) >= 3:
                draw.polygon(pts, fill=0)

    for corner in [(0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)]:
        ImageDraw.floodfill(img, corner, 128, thresh=10)

    # Pixels the border flood never reached (ink OR fully-enclosed holes)
    # become the solid mask; invert so potrace (which traces BLACK regions)
    # traces the silhouette itself, not the surrounding page.
    mask = img.point(lambda p: 0 if p == 128 else 255)
    inv = ImageOps.invert(mask.convert("L"))

    pbm_path = out_svg_path.rsplit(".", 1)[0] + ".pbm"
    inv.convert("1").save(pbm_path)
    subprocess.run(
        [
            "potrace", "--svg", pbm_path, "-o", out_svg_path,
            "--turdsize", str(turdsize), "--opttolerance", str(opttolerance),
        ],
        check=True,
    )
    return out_svg_path


# ---------------------------------------------------------------------------
# Kotlin codegen
# ---------------------------------------------------------------------------

@dataclass
class Normalizer:
    """Maps page-space (x, y) into the app's `viewBoxWidth`x`viewBoxHeight`
    canvas, uniformly scaled and centered with `margin` px of breathing
    room — the same convention every hand-authored template in this app
    uses (see `SunnyDayPaths` etc: full-bleed art in a ~320x320 box)."""
    minx: float
    miny: float
    maxx: float
    maxy: float
    canvas: float = 320.0
    margin: float = 40.0  # total margin (both sides combined)

    target: float = field(init=False)
    scale: float = field(init=False)
    offx: float = field(init=False)
    offy: float = field(init=False)

    def __post_init__(self):
        self.target = self.canvas - self.margin
        self.scale = self.target / max(self.maxx - self.minx, self.maxy - self.miny)
        self.offx = (self.canvas - (self.maxx - self.minx) * self.scale) / 2.0 - self.minx * self.scale
        self.offy = (self.canvas - (self.maxy - self.miny) * self.scale) / 2.0 - self.miny * self.scale

    def __call__(self, x: float, y: float) -> tuple[float, float]:
        return x * self.scale + self.offx, y * self.scale + self.offy

    @classmethod
    def from_points(cls, pts: list[tuple[float, float]], **kwargs) -> "Normalizer":
        xs = [p[0] for p in pts]
        ys = [p[1] for p in pts]
        return cls(min(xs), min(ys), max(xs), max(ys), **kwargs)


def _fmt(v: float) -> str:
    return f"{v:.2f}f"


def emit_path_kotlin(name: str, subpaths, norm: Normalizer) -> str:
    lines = [f"    val {name}: Path = Path().apply {{"]
    for sp in subpaths:
        for seg in sp:
            if seg[0] == "M":
                x, y = norm(seg[1], seg[2])
                lines.append(f"        moveTo({_fmt(x)}, {_fmt(y)})")
            elif seg[0] == "L":
                x, y = norm(seg[1], seg[2])
                lines.append(f"        lineTo({_fmt(x)}, {_fmt(y)})")
            elif seg[0] == "C":
                x1, y1 = norm(seg[1], seg[2])
                x2, y2 = norm(seg[3], seg[4])
                x, y = norm(seg[5], seg[6])
                lines.append(f"        cubicTo({_fmt(x1)}, {_fmt(y1)}, {_fmt(x2)}, {_fmt(y2)}, {_fmt(x)}, {_fmt(y)})")
            elif seg[0] == "Z":
                lines.append("        close()")
    lines.append("    }")
    return "\n".join(lines)


def emit_hit_polygon_kotlin(name: str, subpaths, norm: Normalizer) -> str:
    """Emits `val {name}HitPolygon: List<Offset> = listOf(...)` from the
    SMALLEST subpath — see `smallest_subpath`'s docstring for why."""
    pts = flatten_subpath(smallest_subpath(subpaths), n=8)
    offsets = ", ".join(f"Offset({_fmt(x)}, {_fmt(y)})" for x, y in (norm(px, py) for px, py in pts))
    return f"    val {name}HitPolygon: List<Offset> = listOf({offsets})"
