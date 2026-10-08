#!/usr/bin/env python3
"""Deterministic 1024px transparent PNG of the approved hexagon + targeting reticle.
No third-party image packages or font files are needed for CI reproducibility.
The surrounding black canvas is TRANSPARENT; the inner black hexagonal panel remains opaque.
"""
import struct
import zlib
from pathlib import Path

GOLD = (255, 190, 0, 255)
WHITE = (255, 255, 255, 255)
BLACK = (0, 0, 0, 255)
CLEAR = (0, 0, 0, 0)


def _hex(x, y, half_width, flat_y, tip_y):
    ay = abs(y)
    if ay >= tip_y:
        return False
    limit = half_width if ay <= flat_y else half_width * (tip_y - ay) / (tip_y - flat_y)
    return abs(x) < limit


def _rgba(x, y):
    if not _hex(x, y, .83, .47, .96):
        return CLEAR
    if not _hex(x, y, .65, .37, .77):
        return GOLD
    dist = (x * x + y * y) ** .5
    rings = (.405 <= dist <= .467) or (.265 <= dist <= .322) or (.124 <= dist <= .180)
    cross = (abs(y) < .041 and abs(x) < .505) or (abs(x) < .041 and abs(y) < .548)
    return WHITE if rings or cross else BLACK


def _chunk(tag, data):
    return (struct.pack('>I', len(data)) + tag + data +
            struct.pack('>I', zlib.crc32(tag + data) & 0xffffffff))


def render_logo(size=1024):
    if size not in (256, 512, 1024, 2048):
        raise ValueError('supported sizes: 256/512/1024/2048')
    stride = size * 4
    raw = bytearray((stride + 1) * size)
    palette = {color: bytes(color) for color in (GOLD, WHITE, BLACK, CLEAR)}
    for row in range(size):
        offset = row * (stride + 1)
        raw[offset] = 0
        y = (2 * row + 1 - size) / size
        for col in range(size):
            x = (2 * col + 1 - size) / size
            raw[offset + 1 + 4 * col:offset + 5 + 4 * col] = palette[_rgba(x, y)]
    payload = zlib.compress(bytes(raw), level=9)
    return (b'\x89PNG\r\n\x1a\n' +
            _chunk(b'IHDR', struct.pack('>IIBBBBB', size, size, 8, 6, 0, 0, 0)) +
            _chunk(b'IDAT', payload) + _chunk(b'IEND', b''))


if __name__ == '__main__':
    target = Path(__file__).with_name('hazrob_hr069_r1_logo.png')
    target.write_bytes(render_logo())
    print(f'{target}: {target.stat().st_size} bytes')
