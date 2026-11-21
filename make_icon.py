import struct, zlib, math

def make_png(path, size, draw_fn):
    def chunk(tag, data):
        c = struct.pack('>I', len(data)) + tag + data
        return c + struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff)
    w = h = size
    rows = []
    for y in range(h):
        row = bytearray()
        row.append(0)
        for x in range(w):
            r, g, b, a = draw_fn(x, y, size)
            row += bytes((r, g, b, a))
        rows.append(bytes(row))
    raw = b''.join(rows)
    ihdr = struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0)
    png = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', ihdr) + chunk(b'IDAT', zlib.compress(raw, 9)) + chunk(b'IEND', b'')
    with open(path, 'wb') as f:
        f.write(png)

# 科技蓝圆角方形 app 图标：蓝底 + 白色图片/相框符号
def icon_fn(x, y, s):
    # 圆角矩形背景
    m = int(s*0.10)
    r = int(s*0.22)
    # 圆角判定
    cx = min(max(x, m+r), m+s-2*m-r)
    cy = min(max(y, m+r), m+s-2*m-r)
    dist2 = (x-cx)**2 + (y-cy)**2
    inside = (m <= x < s-m and m <= y < s-m)
    if inside and dist2 > r*r:
        inside = False
    if not inside:
        return (0, 0, 0, 0)
    # 蓝色渐变底 (上浅下深)
    t = y / s
    base = (int(0x37+(0x18-0x37)*t), int(0x8A+(0x5F-0x8A)*t), int(0xDD+(0xA5-0xDD)*t))
    # 白色相框图形
    fx0, fy0, fx1, fy1 = int(s*0.30), int(s*0.30), int(s*0.70), int(s*0.70)
    lw = max(2, int(s*0.035))
    white = (255, 255, 255, 255)
    # 相框边框
    if fx0 <= x <= fx1 and fy0 <= y <= fy1:
        if (x-fx0 < lw or fx1-x < lw or y-fy0 < lw or fy1-y < lw):
            return white
    # 左上小圆点（太阳）
    dotx, doty, dotr = int(s*0.40), int(s*0.40), int(s*0.045)
    if (x-dotx)**2 + (y-doty)**2 <= dotr*dotr:
        return white
    # 山形斜线
    # 从 (fx0+lw, fy1-lw) 到 (fx1-lw, fy0+lw) 的折线
    ax, ay = fx0+lw, fy1-lw
    bx, by = int(s*0.52), int(s*0.50)
    cx, cy = fx1-lw, fy1-lw
    def near_seg(px, py, x1, y1, x2, y2, w):
        dx, dy = x2-x1, y2-y1
        L2 = dx*dx+dy*dy
        if L2 == 0: return False
        t = max(0, min(1, ((px-x1)*dx+(py-y1)*dy)/L2))
        ex, ey = x1+t*dx, y1+t*dy
        return (px-ex)**2+(py-ey)**2 <= (w/2)**2
    if near_seg(x,y,ax,ay,bx,by,lw*0.7) or near_seg(x,y,bx,by,cx,cy,lw*0.7):
        return white
    return (base[0], base[1], base[2], 255)

make_png("res/mipmap-anydpi-v26/ic_launcher.png", 192, icon_fn)
print("icon generated")
