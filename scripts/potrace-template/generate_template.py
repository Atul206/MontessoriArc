#!/usr/bin/env python3
"""Generates a BabyApp `content/generated/*.kt` file for a coloring
template traced from a PNG with potrace, following the region model that
fixed every bug found on the first such template ("Funny Cat"). Read
`scripts/potrace-template/README.md` first — this script encodes that
process, it doesn't replace understanding it.

Usage:
    python3 scripts/potrace-template/generate_template.py config.json

Config JSON shape (see scripts/potrace-template/example_config.json):
{
  "object_name": "FunnyCatPaths",       // Kotlin object name
  "traced_svg": "/path/to/traced.svg",  // potrace --svg output of the PNG
  "out_kt": "/path/to/FunnyCatPaths.kt",
  "work_dir": "/path/to/scratch/dir",   // where intermediate .pbm/.svg go
  "solids": [                           // named solid fill regions to derive
    {"name": "body", "indices": [0]},
    {"name": "eyeR", "indices": [1, 2, 3]},
    {"name": "eyeL", "indices": [4, 5, 6]},
    {"name": "bellyPatch", "indices": [8]}
  ]
}

`indices` are indices into the traced SVG's <path> elements in document
order — inspect them first with `inspect_traced_paths.py` (renders each
path index in isolation so you can see what it is before deciding which
solid group it belongs to).

Every raw path index NOT index 0 becomes `detail{i}` (index 0 is always
`outline`, potrace's main connected ink network). Any outline/detail region
whose index appears in some solid's `indices` is marked `isDecorative` in
the generated *comment* (the actual RegionSpec wiring happens in
TemplateCatalog.kt, which this script does not touch — see README step 5).
"""

import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from potrace_lib import (  # noqa: E402
    Normalizer,
    emit_hit_polygon_kotlin,
    emit_path_kotlin,
    load_single_path_subpaths,
    load_traced_paths,
    make_solid_region_svg,
)


def main(config_path: str) -> None:
    config = json.loads(Path(config_path).read_text())
    object_name = config["object_name"]
    traced_svg = config["traced_svg"]
    out_kt = config["out_kt"]
    work_dir = Path(config["work_dir"])
    work_dir.mkdir(parents=True, exist_ok=True)
    solids = config["solids"]

    raw_paths, all_transformed, all_pts, _ = load_traced_paths(traced_svg)
    norm = Normalizer.from_points(all_pts)

    names = ["outline"] + [f"detail{i}" for i in range(1, len(raw_paths))]
    covered_indices = {i for solid in solids for i in solid["indices"]}

    blocks = []
    solid_names = []
    for solid in solids:
        name = solid["name"]
        solid_names.append(name)
        mask_svg = str(work_dir / f"{name}_mask.svg")
        print(f"deriving solid region '{name}' from path indices {solid['indices']}...")
        make_solid_region_svg(traced_svg, solid["indices"], mask_svg)
        solid_subpaths, _ = load_single_path_subpaths(mask_svg)
        blocks.append(emit_path_kotlin(name, solid_subpaths, norm))

    ink_lines = []
    for i, (name, subpaths) in enumerate(zip(names, all_transformed)):
        decorative = i in covered_indices
        blocks.append(emit_path_kotlin(name, subpaths, norm))
        blocks.append(emit_hit_polygon_kotlin(name, subpaths, norm))
        ink_lines.append(f" *   {name} (path index {i}){' [isDecorative candidate]' if decorative else ''}")

    kt = f"""package com.calmcoloring.app.content.generated

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path

/**
 * Compose [Path] data generated from a potrace bitmap trace (see
 * `scripts/potrace-template/README.md` for the full process and the
 * mandatory on-device verification checklist — this file alone does not
 * guarantee a working template).
 *
 * Solid fill regions ({", ".join(solid_names)}) were derived by
 * rasterizing ink subpaths, flood-filling from the image border to find
 * true background, and re-tracing what's left with potrace — potrace's
 * raw ink trace alone has no enclosed fill shapes.
 *
 * Ink regions (outline, detail1..detail{len(raw_paths) - 1}):
{chr(10).join(ink_lines)}
 * Regions marked [isDecorative candidate] above visually overlap a solid
 * region and MUST be wired with `RegionSpec(isDecorative = true)` in
 * TemplateCatalog.kt, or a tap on that spot will recolor the thin ink
 * instead of the solid fill beneath it. All ink regions (decorative or
 * not) should be wired with `strokeWidth = 0f, fillsWithOutlineByDefault =
 * true`, and use `{{name}}HitPolygon` below (NOT `.toHitPolygon()` — see
 * README for why that util silently breaks on multi-subpath ink).
 */
object {object_name} {{
    val background: Path = backgroundPath()

{chr(10).join(chr(10) + b for b in blocks)}
}}
"""
    Path(out_kt).write_text(kt)
    print(f"\nwrote {out_kt}")
    print("\nNext steps (see README.md):")
    print("  1. Wire each region into TemplateCatalog.kt, marking isDecorative per the comments above.")
    print("  2. ./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:testDebugUnitTest")
    print("  3. Install on device/emulator, clear app data, and run the full verification checklist.")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print(__doc__)
        sys.exit(1)
    main(sys.argv[1])
