import math, struct, zlib

SIZE = 192
SS = 3  # 超采样倍率，抗锯齿

def sd_round_box(px, py, cx, cy, hw, hh, r):
    dx = abs(px - cx) - (hw - r)
    dy = abs(py - cy) - (hh - r)
    return math.hypot(max(dx, 0.0), max(dy, 0.0)) + min(max(dx, dy), 0.0) - r

def dist_seg(px, py, x1, y1, x2, y2):
    dx, dy = x2 - x1, y2 - y1
    L2 = dx * dx + dy * dy
    if L2 == 0: return math.hypot(px - x1, py - y1)
    t = max(0.0, min(1.0, ((px - x1) * dx + (py - y1) * dy) / L2))
    return math.hypot(px - (x1 + t * dx), py - (y1 + t * dy))

def in_tri(px, py, a, b, c):
    def sign(p1, p2, p3): return (p1[0]-p3[0])*(p2[1]-p3[1]) - (p2[0]-p3[0])*(p1[1]-p3[1])
    d1, d2, d3 = sign((px,py), a, b), sign((px,py), b, c), sign((px,py), c, a)
    neg = (d1 < 0) or (d2 < 0) or (d3 < 0)
    pos = (d1 > 0) or (d2 > 0) or (d3 > 0)
    return not (neg and pos)

def render(x, y):
    s = SIZE * SS
    # 坐标缩放回 0..SIZE
    px = x / SS
    py = y / SS
    S = SIZE

    # 圆角方背景 + 线性渐变 (左上 #185FA5 -> 右下 #0C447C)
    d = sd_round_box(px, py, S/2, S/2, S/2, S/2, S*0.22)
    if d > 0:
        return (0, 0, 0, 0)
    t = (px / S * 0.5 + py / S * 0.5)
    c1 = (0x18, 0x5F, 0xA5); c2 = (0x0C, 0x44, 0x7C)
    base = tuple(int(c1[i] + (c2[i]-c1[i]) * t) for i in range(3))

    white = (255, 255, 255)
    soft = (125, 211, 252)  # #7DD3FC

    # 相框（白色圆角矩形描边）
    frame = sd_round_box(px, py, S*0.45, S*0.50, S*0.30, S*0.24, S*0.045)
    lw = S * 0.030
    if abs(frame) < lw:
        return white + (255,)

    # 太阳白点
    if math.hypot(px - S*0.355, py - S*0.425) < S*0.045:
        return white + (255,)

    # 山形折线（两段）
    segs = [(S*0.23, S*0.62, S*0.40, S*0.50),
            (S*0.40, S*0.50, S*0.53, S*0.60),
            (S*0.53, S*0.60, S*0.66, S*0.47)]
    for seg in segs:
        if dist_seg(px, py, *seg) < S*0.028:
            return white + (255,)

    # 循环箭头（浅蓝）：右上角圆弧 + 三角箭头
    ax, ay, ar = S*0.80, S*0.30, S*0.135
    ang = math.atan2(py - ay, px - ax)
    if math.hypot(px - ax, py - ay) is not None:
        rr = math.hypot(px - ax, py - ay)
        aw = S*0.024
        if abs(rr - ar) < aw and (-2.6 < ang < 1.0):
            return soft + (255,)
    # 箭头三角（在弧起点附近，指向上）
    tip = (S*0.80, S*0.145)
    tri = [tip, (S*0.735, S*0.205), (S*0.865, S*0.205)]
    if in_tri(px, py, *tri):
        return soft + (255,)

    return base + (255,)

def main():
    # 先渲染到超采样大图
    s = SIZE * SS
    buf = []
    for y in range(s):
        row = bytearray()
        for x in range(s):
            row += bytes(render(x, y))
        buf.append(bytes(row))

    # 降采样平均
    out_rows = []
    for oy in range(SIZE):
        row = bytearray()
        row.append(0)
        for ox in range(SIZE):
            acc = [0, 0, 0, 0]
            for dy in range(SS):
                for dx in range(SS):
                    p = buf[oy*SS+dy]
                    i = (ox*SS+dx) * 4
                    for k in range(4):
                        acc[k] += p[i+k]
            n = SS*SS
            row += bytes(int(a/n) for a in acc)
        out_rows.append(bytes(row))
    raw = b''.join(out_rows)

    def chunk(tag, data):
        c = struct.pack('>I', len(data)) + tag + data
        return c + struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff)
    ihdr = struct.pack('>IIBBBBB', SIZE, SIZE, 8, 6, 0, 0, 0)
    png = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', ihdr) + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b'')
    for path in ['res/mipmap-hdpi/ic_launcher.png', 'res/mipmap-anydpi-v26/ic_launcher.png']:
        with open(path, 'wb') as f:
            f.write(png)
    print('icon v2 generated', len(png), 'bytes')

main()
