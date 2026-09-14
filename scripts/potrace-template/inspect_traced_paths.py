#!/usr/bin/env python3
"""Renders each <path> in a potrace-traced SVG in isolation, in a grid, so
you can see what each index actually is before writing a
`generate_template.py` config — e.g. "index 1/2/3 are the right eye's ring,
ring, and pupil" (see scripts/potrace-template/README.md step 2).

Usage:
    python3 scripts/potrace-template/inspect_traced_paths.py traced.svg out.html
    # then open out.html in a browser (or the Claude Code Browser tool)

Also prints each path index's page-space bounding box, which is usually
enough on its own to guess groupings (nearby/nested boxes = same feature)
without even opening the HTML.
"""

import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from potrace_lib import flatten_subpath, load_traced_paths  # noqa: E402


def main(svg_path: str, out_html: str) -> None:
    data = Path(svg_path).read_text()
    transform_attr = re.search(r'(transform="[^"]+")', data).group(1)
    view_box = re.search(r'viewBox="([^"]+)"', data).group(1)

    raw_paths, all_transformed, _, _ = load_traced_paths(svg_path)

    cells = []
    for i, (d, subpaths) in enumerate(zip(raw_paths, all_transformed)):
        pts = [p for sp in subpaths for p in flatten_subpath(sp)]
        xs = [p[0] for p in pts]
        ys = [p[1] for p in pts]
        bbox = (min(xs), min(ys), max(xs), max(ys))
        print(f"index {i}: {len(subpaths)} subpath(s), bbox={tuple(round(v, 1) for v in bbox)}")
        cells.append(f"""
        <div style="display:inline-block;text-align:center;margin:8px;vertical-align:top">
          <svg width="220" height="220" viewBox="0 0 {view_box.split()[2]} {view_box.split()[3]}"
               style="background:#dde;border:1px solid #000">
            <g {transform_attr} fill="#000"><path d="{d}"/></g>
          </svg>
          <div>index {i}</div>
        </div>""")

    Path(out_html).write_text(f"<html><body>{''.join(cells)}</body></html>")
    print(f"\nwrote {out_html}")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        print(__doc__)
        sys.exit(1)
    main(sys.argv[1], sys.argv[2])
