import math, struct, zlib

SIZE = 192
SS = 3

def bezier(p0, p1, p2, n=16):
    pts = []
    for i in range(n + 1):
        t = i / n
        x = (1-t)**2*p0[0] + 2*(1-t)*t*p1[0] + t**2*p2[0]
        y = (1-t)**2*p0[1] + 2*(1-t)*t*p1[1] + t**2*p2[1]
        pts.append((x, y))
    return pts

def leaf(cx, cy, alpha_deg, R=50, spread=86):
    """一个风车叶片：细长三角，尖端在中心，外缘是一条外凸的弧。"""
    # 叶片主体：从中心出发，尖端收拢，沿一条偏斜的外凸曲线展开
    a = math.radians(alpha_deg)
    # 外缘三点：靠内侧起点、顶点、靠外侧终点（构成外凸弧）
    # 用参数曲线：t 从 0(中心) 到 1(外缘末端)，角度从 alpha-40 扫到 alpha+40
    half = math.radians(spread / 2)
    pts = [(cx, cy)]
    n = 32
    for i in range(n + 1):
        t = i / n
        ang = a - half + 2 * half * t
        # 半径：中心 0 → 外缘；外缘半径随 t 先增后略收，形成弧
        rr = R * (t ** 0.62)
        pts.append((cx + rr * math.cos(ang), cy + rr * math.sin(ang)))
    return pts

def rot(pt, deg):
    a = math.radians(deg)
    dx, dy = pt[0]-50, pt[1]-50
    return (dx*math.cos(a)-dy*math.sin(a)+50, dx*math.sin(a)+dy*math.cos(a)+50)

def in_poly(x, y, poly):
    inside = False
    j = len(poly)-1
    for i in range(len(poly)):
        xi, yi = poly[i]; xj, yj = poly[j]
        if ((yi > y) != (yj > y)) and (x < (xj-xi)*(y-yi)/(yj-yi)+xi):
            inside = not inside
        j = i
    return inside

def dist_seg(px, py, x1, y1, x2, y2):
    dx, dy = x2-x1, y2-y1
    L2 = dx*dx+dy*dy
    if L2 == 0: return math.hypot(px-x1, py-y1)
    t = max(0, min(1, ((px-x1)*dx+(py-y1)*dy)/L2))
    return math.hypot(px-(x1+t*dx), py-(y1+t*dy))

# 四片叶片：中心 50,50，分别指向四个对角（参考图的风车排布）
POLYS = [
    leaf(50, 50, -45),   # 右上
    leaf(50, 50, 45),    # 右下
    leaf(50, 50, 135),   # 左下
    leaf(50, 50, 225),   # 左上
]

# 叶片顺序 [右上, 右下, 左下, 左上] → 参考图配色 [黄, 红, 蓝, 绿]
FOUR = [(0xFF,0xCC,0x00), (0xFF,0x3B,0x30), (0x0A,0x84,0xFF), (0x34,0xC7,0x59)]
# 科技蓝单色版：四档蓝
BLUE = [(0x5B,0x9B,0xD5), (0x0C,0x44,0x7C), (0x37,0x8A,0xDD), (0x18,0x5F,0xA5)]

def render_icon(ox, oy, colors, with_a=False):
    pix = [[(0,0,0,0) for _ in range(SIZE)] for _ in range(SIZE)]
    for oy2 in range(SIZE*SS):
        for ox2 in range(SIZE*SS):
            x = ox2/SS; y = oy2/SS
            col = None
            for poly, clr in zip(POLYS, colors):
                if in_poly(x, y, poly):
                    col = clr
                    break
            if col is None:
                continue
            # 中心 A 标
            if with_a:
                if math.hypot(x-50, y-50) < 11:
                    col = (255,255,255)
                    if (dist_seg(x,y,45.5,55,50,44) < 1.3 or
                        dist_seg(x,y,50,44,54.5,55) < 1.3 or
                        dist_seg(x,y,47,51,53,51) < 1.3):
                        col = (0x18,0x5F,0xA5)
            pix[oy2//SS][ox2//SS] = tuple(int(v) for v in col) + (255,)
    return pix

def blend(dst, src, ox, oy):
    for y in range(SIZE):
        for x in range(SIZE):
            s = src[y][x]
            if s[3] == 0: continue
            dst[oy+y][ox+x] = s

def write_png(path, w, h, pixels):
    rows = []
    for y in range(h):
        row = bytearray([0])
        for x in range(w):
            row += bytes(pixels[y][x])
        rows.append(bytes(row))
    raw = b''.join(rows)
    def chunk(tag, data):
        return struct.pack('>I',len(data))+tag+data+struct.pack('>I', zlib.crc32(tag+data)&0xffffffff)
    ihdr = struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0)
    open(path,'wb').write(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',ihdr)+chunk(b'IDAT',zlib.compress(raw,9))+chunk(b'IEND',b''))

if __name__ == '__main__':
    gap = 40
    W = SIZE*3 + gap*4
    H = SIZE + 40
    canvas = [[(255,255,255,255) for _ in range(W)] for _ in range(H)]
    blend(canvas, render_icon(0,0,FOUR,False), gap, 20)
    blend(canvas, render_icon(0,0,BLUE,False), SIZE+gap*2, 20)
    blend(canvas, render_icon(0,0,FOUR,True), SIZE*2+gap*3, 20)
    write_png('icon_options.png', W, H, canvas)
    write_png('ic_launcher_v2.png', SIZE, SIZE, render_icon(0,0,FOUR,True))
    print('rendered icon_options.png and ic_launcher_v2.png')
