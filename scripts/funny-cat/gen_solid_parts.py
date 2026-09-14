import re, os
from PIL import Image, ImageDraw, ImageOps

SCRATCH_DIR = "/private/tmp/claude-501/-Users-atul-Documents-dev-BabyApp/ae8178ae-15a2-40c4-a9a1-36ae2a2e50a1/scratchpad"
SRC = f"{SCRATCH_DIR}/cat-test.svg"

data = open(SRC).read()
m = re.search(r'translate\(([-\d.]+),([-\d.]+)\)\s*scale\(([-\d.]+),([-\d.]+)\)', data)
tx, ty, sx, sy = (float(x) for x in m.groups())

def apply_transform(x, y):
    return x * sx + tx, y * sy + ty

paths = re.findall(r'<path d="([^"]+)"', data, re.S)

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
    pts = []
    cur = None
    for seg in subpath:
        if seg[0] in ('M', 'L'):
            cur = (seg[1], seg[2])
            pts.append(apply_transform(*cur))
        elif seg[0] == 'C':
            x0,y0 = cur
            x1,y1,x2,y2,x3,y3 = seg[1:]
            for i in range(1, n+1):
                t = i / n; mt = 1 - t
                bx = mt**3*x0 + 3*mt**2*t*x1 + 3*mt*t**2*x2 + t**3*x3
                by = mt**3*y0 + 3*mt**2*t*y1 + 3*mt*t**2*y2 + t**3*y3
                pts.append(apply_transform(bx, by))
            cur = (x3, y3)
    return pts

W, H = 1160, 1345

def make_solid(indices, out_name):
    img = Image.new("L", (W, H), 255)
    draw = ImageDraw.Draw(img)
    for idx in indices:
        for sp in parse_subpaths(paths[idx]):
            pts = flatten(sp)
            if len(pts) >= 3:
                draw.polygon(pts, fill=0)
    # flood true background from corners
    for corner in [(0,0),(W-1,0),(0,H-1),(W-1,H-1)]:
        ImageDraw.floodfill(img, corner, 128, thresh=10)
    body = img.point(lambda p: 0 if p == 128 else 255)
    inv = ImageOps.invert(body.convert("L"))
    pbm_path = f"{SCRATCH_DIR}/{out_name}.pbm"
    inv.convert("1").save(pbm_path)
    svg_path = f"{SCRATCH_DIR}/{out_name}.svg"
    os.system(f'potrace --svg "{pbm_path}" -o "{svg_path}" --turdsize 2 --opttolerance 0.4')
    print("wrote", svg_path)

make_solid([1,2,3], "eyeR_mask")
make_solid([4,5,6], "eyeL_mask")
make_solid([8], "bellyPatch_mask")
