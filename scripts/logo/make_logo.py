"""Generate the EMI Remastered pixel-art logo (original art, see CREDITS.md).

Everything is drawn on small logical grids from a fixed palette, character maps and hand-made glyphs, then upscaled by
integer factors with nearest-neighbour only. Running ``py -3 scripts/logo/make_logo.py`` from the repository root writes:

- docs/logo/icon.png           32x32 grid x16 = 512x512, transparent
- docs/logo/icon-400.png       32x32 grid x12 = 384x384, centred on a transparent 400x400 canvas
- docs/logo/banner.png         192x48 grid x8 = 1536x384
- src/main/resources/icon.png  same as docs/logo/icon.png (the mod icon)

The output is deterministic (indexed PNGs, no timestamps). Only Pillow is needed.
"""
import os

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))

# One character per colour. Hue-shifted ramps: slate (violet shadows -> blue-grey lights), gem (blue -> teal -> mint),
# gold (rust -> orange -> yellow).
PALETTE = [
	('.', None),                # transparent
	('K', (0x16, 0x12, 0x22)),  # outline, violet-black
	('D', (0x2b, 0x28, 0x46)),  # slate shadow
	('S', (0x3c, 0x3b, 0x60)),  # slate base
	('M', (0x5a, 0x5e, 0x8c)),  # slate rim
	('L', (0x96, 0x9e, 0xc8)),  # slate light
	('w', (0xf2, 0xff, 0xf4)),  # white glint
	('a', (0x8c, 0xf5, 0xc4)),  # mint
	('b', (0x2e, 0xc6, 0xa4)),  # teal
	('c', (0x16, 0x88, 0x96)),  # sea blue
	('d', (0x15, 0x4a, 0x7a)),  # deep blue
	('y', (0xff, 0xef, 0x96)),  # gold light
	('g', (0xf4, 0xb6, 0x38)),  # gold
	('o', (0xc2, 0x68, 0x22)),  # orange
	('r', (0x76, 0x2e, 0x1e)),  # rust
	('G', (0xb6, 0xe4, 0xff)),  # glass
	('H', (0x6c, 0xa6, 0xe0)),  # glass shade
	('B', (0x1d, 0x1a, 0x2e)),  # banner background
	('C', (0x24, 0x21, 0x38)),  # banner background, lighter
]
INDEX = {ch: i for i, (ch, _) in enumerate(PALETTE)}


class Canvas:
	def __init__(self, w, h, fill='.'):
		self.w, self.h = w, h
		self.px = [[fill] * w for _ in range(h)]

	def get(self, x, y):
		if 0 <= x < self.w and 0 <= y < self.h:
			return self.px[y][x]
		return '.'

	def set(self, x, y, c):
		if 0 <= x < self.w and 0 <= y < self.h and c not in ' .':
			self.px[y][x] = c

	def rect(self, x0, y0, x1, y1, c):
		for y in range(y0, y1 + 1):
			for x in range(x0, x1 + 1):
				self.set(x, y, c)

	def stamp(self, rows, x0, y0):
		for j, row in enumerate(rows):
			for i, c in enumerate(row):
				self.set(x0 + i, y0 + j, c)

	def disc(self, cx, cy, r, c):
		for y in range(self.h):
			for x in range(self.w):
				if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
					self.set(x, y, c)

	def outline(self, c='K', empty='.'):
		"""Put a 1 px outline around every non-empty pixel (4-neighbourhood)."""
		add = [(x, y) for y in range(self.h) for x in range(self.w) if self.px[y][x] == empty and any(
			self.get(x + dx, y + dy) not in ('.', empty) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))]
		for x, y in add:
			self.px[y][x] = c

	def image(self, scale=1):
		im = Image.new('P', (self.w, self.h), 0)
		flat = []
		for _, rgb in PALETTE:
			flat += list(rgb or (0, 0, 0))
		im.putpalette(flat)
		im.putdata([INDEX[c] for row in self.px for c in row])
		im.info['transparency'] = 0
		if scale != 1:
			im = im.resize((self.w * scale, self.h * scale), Image.NEAREST)
			im.info['transparency'] = 0
		return im


# ---------------------------------------------------------------------------------------------------------- motifs

GEM = [
	"....KKKKKKK....",
	"...KwwwaaabK...",
	"..KawwaaaabbK..",
	".KaawaaaaabbcK.",
	"KaaaaaaaabbbccK",
	"KbbbbbbcccccddK",
	".KbbbbbccccddK.",
	"..KbbbbcccddK..",
	"...KbbbccddK...",
	"....KbbcddK....",
	".....KbcdK.....",
	"......KcK......",
	".......K.......",
]

SPARK = [
	"..y..",
	"..y..",
	"yywyy",
	"..g..",
	"..o..",
]


def slot(cv, x0, y0, x1, y1):
	"""A raised inventory slot plate with an inset well."""
	cv.rect(x0, y0, x1, y1, 'K')
	cv.rect(x0 + 1, y0 + 1, x1 - 1, y1 - 1, 'M')
	cv.rect(x0 + 1, y0 + 1, x1 - 1, y0 + 1, 'L')
	cv.rect(x0 + 1, y0 + 1, x0 + 1, y1 - 1, 'L')
	cv.rect(x0 + 2, y1 - 1, x1 - 1, y1 - 1, 'D')
	cv.rect(x1 - 1, y0 + 2, x1 - 1, y1 - 1, 'D')
	cv.rect(x0 + 3, y0 + 3, x1 - 3, y1 - 3, 'S')
	cv.rect(x0 + 3, y0 + 3, x1 - 3, y0 + 3, 'D')
	cv.rect(x0 + 3, y0 + 3, x0 + 3, y1 - 3, 'D')
	cv.rect(x0 + 4, y1 - 3, x1 - 3, y1 - 3, 'M')
	cv.rect(x1 - 3, y0 + 4, x1 - 3, y1 - 3, 'M')


def magnifier(cv, cx, cy):
	"""Gold magnifying glass, lens centred on (cx, cy), handle towards the bottom-right corner."""
	cv.disc(cx, cy, 6.6, 'K')
	for y in range(cv.h):
		for x in range(cv.w):
			d2 = (x - cx) ** 2 + (y - cy) ** 2
			if d2 <= 5.6 ** 2:
				cv.set(x, y, 'o' if x + y > 2 * cx + 1 else 'g')
			if d2 <= 3.7 ** 2:
				cv.set(x, y, 'K')
			if d2 <= 2.9 ** 2:
				cv.set(x, y, 'H' if x + y > 2 * cx + 1 else 'G')
	cx, cy = int(cx), int(cy)
	for x, y in ((cx - 1, cy - 1), (cx - 2, cy), (cx, cy - 2)):
		cv.set(x, y, 'w')
	for i in range(4):
		x = y = cx + 5 + i
		cv.set(x, y, 'o')
		cv.set(x - 1, y, 'g')
		cv.set(x, y - 1, 'r')


def icon():
	cv = Canvas(32, 32)
	slot(cv, 1, 1, 25, 25)
	cv.stamp(GEM, 5, 6)
	magnifier(cv, 22.5, 22.5)
	cv.stamp(SPARK, 26, 1)
	cv.outline()
	return cv


# ----------------------------------------------------------------------------------------------------------- fonts

# Title font: 12 rows, 2 px stems.
TITLE_FONT = {
	'E': ["#######", "#######", "##.....", "##.....", "##.....", "######.", "######.", "##.....", "##.....", "##.....",
		"#######", "#######"],
	'M': ["##......##", "###....###", "####..####", "##.####.##", "##..##..##", "##......##", "##......##",
		"##......##", "##......##", "##......##", "##......##", "##......##"],
	'I': ["######", "######", "..##..", "..##..", "..##..", "..##..", "..##..", "..##..", "..##..", "..##..",
		"######", "######"],
	'R': ["#######.", "########", "##....##", "##....##", "##....##", "########", "#######.", "##..##..", "##...##.",
		"##...##.", "##....##", "##....##"],
	'e': [".......", ".......", ".......", ".#####.", "#######", "##...##", "#######", "#######", "##.....",
		"##.....", "#######", ".######"],
	'm': ["..........", "..........", "..........", "#########.", "##########", "##..##..##", "##..##..##",
		"##..##..##", "##..##..##", "##..##..##", "##..##..##", "##..##..##"],
	'a': [".......", ".......", ".......", ".#####.", ".######", ".....##", ".######", "#######", "##...##",
		"##...##", "#######", ".######"],
	's': [".......", ".......", ".......", ".######", "#######", "##.....", "######.", ".######", ".....##",
		".....##", "#######", "######."],
	't': ["......", ".##...", ".##...", "######", "######", ".##...", ".##...", ".##...", ".##...", ".##...",
		".#####", "..####"],
	'r': [".......", ".......", ".......", "##.###.", "#######", "###..##", "##.....", "##.....", "##.....",
		"##.....", "##.....", "##....."],
	'd': [".....##", ".....##", ".....##", ".######", "#######", "##...##", "##...##", "##...##", "##...##",
		"##...##", "#######", ".######"],
	' ': ["...."] * 12,
}

# Small font: 5 rows, 1 px stems.
SMALL_FONT = {
	'A': [".#.", "#.#", "###", "#.#", "#.#"],
	'B': ["##.", "#.#", "##.", "#.#", "##."],
	'C': [".##", "#..", "#..", "#..", ".##"],
	'E': ["###", "#..", "##.", "#..", "###"],
	'F': ["###", "#..", "##.", "#..", "#.."],
	'I': ["###", ".#.", ".#.", ".#.", "###"],
	'R': ["##.", "#.#", "##.", "#.#", "#.#"],
	'1': [".#.", "##.", ".#.", ".#.", "###"],
	'2': ["##.", "..#", ".#.", "#..", "###"],
	'.': [".", ".", ".", ".", "#"],
	' ': ["..", "..", "..", "..", ".."],
}


def text_mask(font, text, gap):
	"""Return the rows of `text` as a list of strings of '#' and '.'."""
	h = len(next(iter(font.values())))
	rows = [''] * h
	for i, ch in enumerate(text):
		g = font[ch]
		for j in range(h):
			rows[j] += g[j] + ('.' * gap if i < len(text) - 1 else '')
	return rows


def paint_text(cv, rows, x0, y0, ramp):
	"""Paint a text mask, colouring each row from `ramp` (top to bottom)."""
	for j, row in enumerate(rows):
		for i, c in enumerate(row):
			if c == '#':
				cv.set(x0 + i, y0 + j, ramp[j])


# ---------------------------------------------------------------------------------------------------------- banner

DIM_SPARK = [
	".D.",
	"DMD",
	".D.",
]


def banner():
	w, h = 192, 48
	cv = Canvas(w, h, 'B')
	# faint inventory-grid texture, 12 px cells, and a 1 px outline frame
	for y in range(h):
		for x in range(w):
			if x % 12 == 0 or y % 12 == 0:
				cv.px[y][x] = 'C'
	for x in range(w):
		cv.px[0][x] = cv.px[h - 1][x] = 'K'
	for y in range(h):
		cv.px[y][0] = cv.px[y][w - 1] = 'K'
	for x, y in ((6, 41), (44, 4), (101, 39), (176, 6), (183, 37), (139, 41)):
		cv.stamp(DIM_SPARK, x - 1, y - 1)

	emi = text_mask(TITLE_FONT, 'EMI', 2)
	rem = text_mask(TITLE_FONT, 'Remastered', 2)
	title_w = len(emi[0]) + 6 + len(rem[0])
	x0 = (w - (32 + 9 + title_w + 2)) // 2
	ic = icon()
	cv.stamp([''.join(r) for r in ic.px], x0, 8)

	gold = ['y', 'y', 'g', 'g', 'g', 'g', 'g', 'o', 'o', 'o', 'r', 'r']
	mint = ['w', 'a', 'a', 'a', 'a', 'b', 'b', 'b', 'b', 'c', 'c', 'c']
	tx, ty = x0 + 32 + 9, 12
	title = Canvas(w, h)
	paint_text(title, emi, tx, ty, gold)
	paint_text(title, rem, tx + len(emi[0]) + 6, ty, mint)
	title.outline()
	for y in range(h):
		for x in range(w):
			if title.px[y][x] != '.':
				cv.set(x + 1, y + 1, 'K')  # drop shadow
	for y in range(h):
		for x in range(w):
			cv.set(x, y, title.px[y][x])
	paint_text(cv, text_mask(SMALL_FONT, 'FABRIC 1.21.1', 1), tx, 31, ['L'] * 5)
	return cv


# ------------------------------------------------------------------------------------------------------------ main

def save(im, rel, **kw):
	path = os.path.join(ROOT, rel)
	os.makedirs(os.path.dirname(path), exist_ok=True)
	im.save(path, optimize=True, **kw)


def main():
	ic = icon()
	big = ic.image(16)
	save(big, 'docs/logo/icon.png', transparency=0)
	save(big, 'src/main/resources/icon.png', transparency=0)
	pad = Image.new('P', (400, 400), 0)
	pad.putpalette(big.getpalette())
	pad.paste(ic.image(12), (8, 8))
	save(pad, 'docs/logo/icon-400.png', transparency=0)
	wide = banner().image(8)
	del wide.info['transparency']  # opaque background
	save(wide, 'docs/logo/banner.png')


if __name__ == '__main__':
	main()
