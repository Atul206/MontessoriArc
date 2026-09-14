import re
from PIL import Image, ImageDraw

SRC = "/private/tmp/claude-501/-Users-atul-Documents-dev-BabyApp/ae8178ae-15a2-40c4-a9a1-36ae2a2e50a1/scratchpad/cat-test.svg"
data = open(SRC).read()

m = re.search(r'translate\(([-\d.]+),([-\d.]+)\)\s*scale\(([-\d.]+),([-\d.]+)\)', data)
tx, ty, sx, sy = (float(x) for x in m.groups())

def apply_transform(x, y):
    return x * sx + tx, y * sy + ty

paths = re.findall(r'<path d="([^"]+)"', data, re.S)
outline_d = paths[0]  # the connected ink-stroke network

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

def flatten(subpath, n=12):
    """Sample a subpath (in raw untransformed coords) to a flat point list, transformed to page space."""
    pts = []
    cur = None
    for seg in subpath:
        if seg[0] == 'M':
            cur = (seg[1], seg[2])
            pts.append(apply_transform(*cur))
        elif seg[0] == 'L':
            cur = (seg[1], seg[2])
            pts.append(apply_transform(*cur))
        elif seg[0] == 'C':
            x0,y0 = cur
            x1,y1,x2,y2,x3,y3 = seg[1:]
            for i in range(1, n+1):
                t = i / n
                mt = 1 - t
                bx = mt**3*x0 + 3*mt**2*t*x1 + 3*mt*t**2*x2 + t**3*x3
                by = mt**3*y0 + 3*mt**2*t*y1 + 3*mt*t**2*y2 + t**3*y3
                pts.append(apply_transform(bx, by))
            cur = (x3, y3)
        elif seg[0] == 'Z':
            pass
    return pts

subpaths = parse_subpaths(outline_d)

# page-space bbox (matches viewBox roughly 0..1159 x 0..1344)
W, H = 1160, 1345
img = Image.new("L", (W, H), 255)  # 255 = white
draw = ImageDraw.Draw(img)
for sp in subpaths:
    pts = flatten(sp)
    if len(pts) >= 3:
        draw.polygon(pts, fill=0)  # 0 = ink

img.save(f"{__import__('os').path.dirname(SRC)}/ink_raster.png")

# flood fill true background from the four corners with a distinct marker (128)
ImageDraw.floodfill(img, (0, 0), 128, thresh=10)
ImageDraw.floodfill(img, (W-1, 0), 128, thresh=10)
ImageDraw.floodfill(img, (0, H-1), 128, thresh=10)
ImageDraw.floodfill(img, (W-1, H-1), 128, thresh=10)

img.save(f"{__import__('os').path.dirname(SRC)}/flooded.png")

# body mask: anything NOT reached by the background flood (ink OR enclosed holes) = 1 (black)
body = img.point(lambda p: 0 if p == 128 else 255)  # keep PIL "L" convention: 0=black(ink-to-trace for potrace), 255=white
body = body.convert("1")
body.save(f"{__import__('os').path.dirname(SRC)}/body_mask.pbm")
body.save(f"{__import__('os').path.dirname(SRC)}/body_mask.png")
print("done", img.size)
