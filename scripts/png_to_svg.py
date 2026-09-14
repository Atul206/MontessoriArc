#!/usr/bin/env python3
"""Convert a PNG (line-art / coloring-page style) into an SVG.

Uses Pillow to threshold the image to black/white, then shells out to
`potrace` to trace the bitmap into vector paths.

Usage:
    python3 scripts/png_to_svg.py input.png [output.svg] [--threshold 128]
"""

import argparse
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

from PIL import Image


def check_potrace() -> None:
    if shutil.which("potrace") is None:
        sys.exit(
            "potrace not found. Install it with:\n"
            "    brew install potrace"
        )


def png_to_svg(input_path: Path, output_path: Path, threshold: int = 128) -> None:
    check_potrace()

    img = Image.open(input_path).convert("L")
    bw = img.point(lambda p: 255 if p > threshold else 0, mode="L").convert("1")

    with tempfile.NamedTemporaryFile(suffix=".pbm", delete=False) as tmp:
        pbm_path = Path(tmp.name)
    bw.save(pbm_path)

    try:
        subprocess.run(
            ["potrace", "--svg", str(pbm_path), "-o", str(output_path)],
            check=True,
        )
    finally:
        pbm_path.unlink(missing_ok=True)

    print(f"Wrote {output_path}")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("input", type=Path, help="Path to the input PNG")
    parser.add_argument(
        "output", type=Path, nargs="?", help="Path to the output SVG (default: same name, .svg)"
    )
    parser.add_argument(
        "--threshold",
        type=int,
        default=128,
        help="Black/white threshold, 0-255 (default: 128)",
    )
    args = parser.parse_args()

    output = args.output or args.input.with_suffix(".svg")
    png_to_svg(args.input, output, args.threshold)


if __name__ == "__main__":
    main()
