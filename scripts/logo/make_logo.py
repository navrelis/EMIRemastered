"""Generate the EMI Remastered pixel-art logo set (original art, see CREDITS.md).

Every image is hand-authored pixel by pixel as character maps on ONE small native grid, then exported only by integer
nearest-neighbour scaling (no anti-aliasing, no partial alpha). Icon and banner share one palette. Running
``py -3 scripts/logo/make_logo.py`` from the repository root writes:

- docs/logo/icon-native.png    40x40 native icon, transparent background
- docs/logo/icon.png           40x40 x10 = 400x400 (CurseForge project avatar, exact size, no padding)
- src/main/resources/icon.png  40x40 x4  = 160x160 (mod icon in the jar, shown by Mod Menu)
- docs/logo/banner.png         240x60 x6 = 1440x360 (description banner, opaque; icon and text on the same 1x grid)

The output is deterministic (indexed PNGs, no timestamps, byte-identical on a re-run). Only Pillow is needed.
"""
import os

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))

# One character per colour. Ramps are hue-shifted: shadows lean towards blue/violet, lights towards yellow.
PALETTE = [
	('.', None),                # transparent
	('K', (0x1f, 0x18, 0x30)),  # outline, deep violet (no pure black)
	('1', (0x2d, 0x28, 0x47)),  # slate 1, darkest
	('2', (0x3f, 0x3b, 0x63)),  # slate 2
	('3', (0x57, 0x5a, 0x86)),  # slate 3
	('4', (0x7f, 0x86, 0xad)),  # slate 4
	('5', (0xc2, 0xc4, 0xd6)),  # slate 5, light
	('W', (0xff, 0xf7, 0xd6)),  # warm white (glints)
	('e', (0x17, 0x3e, 0x57)),  # emerald 1, blue shadow
	('f', (0x1a, 0x7a, 0x68)),  # emerald 2, teal
	('g', (0x2b, 0xb3, 0x6a)),  # emerald 3, green
	('h', (0x7f, 0xe0, 0x7a)),  # emerald 4, yellow-green light
	('r', (0x5c, 0x24, 0x40)),  # gold 1, wine shadow
	('o', (0xa6, 0x4a, 0x2e)),  # gold 2, rust
	('y', (0xe3, 0x9a, 0x35)),  # gold 3, orange-gold
	('Y', (0xff, 0xd8, 0x66)),  # gold 4, yellow light
	('c', (0xa8, 0xe6, 0xf0)),  # lens glass
	('b', (0x5f, 0xa3, 0xc8)),  # lens glass shade
]
INDEX = {ch: i for i, (ch, _) in enumerate(PALETTE)}

# ------------------------------------------------------------------------------------------------------------ icon
# 40x40, hand-authored. An inventory slot panel (bevelled frame, inset well) holding a cut emerald (crown and pavilion
# facets, one glint), a gold magnifying glass in front of the panel's lower right corner (tube-shaded ring, glass with
# a highlight arc and a shade crescent, collar and grip) and a twinkle over the upper right corner. Light comes from the
# top left; outlines are deep violet, lighter (emerald 1) on the gem's lit top/left edges, none between ring and glass.
ICON = [
	".KKKKKKKKKKKKKKKKKKKKKKKKKKKKKKKK.......",
	"KK555555555555555555555555555555KK.K....",
	"K54444444444444444444444444444432KKYK...",
	"K54333333333333333333333333333322KKYK...",
	"K5431111111111111111111111111232KKKWKKK.",
	"K543122222222222222222222222243KYYWWyyoK",
	"K54312222222eeeeeeeee22222222432KKKyKKK.",
	"K5431222222ehhhWhggffK22222224322KKoK...",
	"K543122222ehhhghgggfffK2222224322KKoK...",
	"K54312222ehhhhgggggffffK222224322K.K....",
	"K5431222ehhhhhgggggfffffK22224322K......",
	"K543122ehhhhhhgggggffffffK2224322K......",
	"K54312ehhhhhhhgggggfffffffK224322K......",
	"K543122KgggggghhffffeeeeeK2224322K......",
	"K5431222KggggghhffffeeeeK22224322K......",
	"K54312222KgggghhffffeeeK222224322K......",
	"K543122222KgggghfffeeeK2222224322K......",
	"K5431222222KggghfffeeK22222224322K......",
	"K54312222222KgghfffeK222KKKKKK322K......",
	"K543122222222KghffeK22KKYYYYYyKK2K......",
	"K5431222222222KhfeK22KYYWooooyyyKK......",
	"K54312222222222KfK22KYYooccccyyyyK......",
	"K543122222222222K22KYYoccWWccccyyyK.....",
	"K543122222222222222KYoccWcccccccyyK.....",
	"K54312222222222222KYYocWcccccccbyyoK....",
	"K54312222222222222KYoccWccccccccbyoK....",
	"K54312222222222222KYocccccccccccbYoK....",
	"K54312222222222222KYoccccccccccbbYoK....",
	"K54312222222222222KyyccccccccccbbYoK....",
	"K54324444444444444KyyyccccccccbbYyoK....",
	"K543333333333333333KyycccccccbbbYoK.....",
	"K532222222222222222KyyycccbbbbbYorK.....",
	"KK222222222222222222KyyYYbbbbYYorK......",
	".KKKKKKKKKKKKKKKKKKKKKyyyYYYYoorKyYyK...",
	"......................KKyoooorKKKoyYyK..",
	"........................KKKKKK...KrooyK.",
	"..................................KrooyK",
	"...................................KrroK",
	"....................................KKK.",
	"........................................",
]

# ----------------------------------------------------------------------------------------------------------- fonts
# Title font: 12 rows, 2 px stems, caps 12 px, x-height 9 px (rows 3..11), ascenders from row 0.
TITLE_FONT = {
	'E': ["#######", "#######", "##.....", "##.....", "##.....", "######.", "######.", "##.....", "##.....", "##.....",
		"#######", "#######"],
	'M': ["##......##", "###....###", "####..####", "##.####.##", "##..##..##", "##......##", "##......##",
		"##......##", "##......##", "##......##", "##......##", "##......##"],
	'I': ["######", "######", "..##..", "..##..", "..##..", "..##..", "..##..", "..##..", "..##..", "..##..",
		"######", "######"],
	'R': ["#######.", "########", "##....##", "##....##", "##....##", "########", "#######.", "##..##..", "##...##.",
		"##....##", "##....##", "##....##"],
	'e': [".......", ".......", ".......", ".#####.", "#######", "##...##", "##...##", "#######", "#######",
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
	' ': ["....."] * 12,
}

# Small font: 7 rows, 1 px stems, caps 7 px, x-height 5 px (rows 2..6).
SMALL_FONT = {
	'F': ["####", "#...", "#...", "###.", "#...", "#...", "#..."],
	'a': ["....", "....", ".##.", "...#", ".###", "#..#", ".###"],
	'b': ["#...", "#...", "###.", "#..#", "#..#", "#..#", "###."],
	'r': ["...", "...", "#.#", "##.", "#..", "#..", "#.."],
	'i': ["#", ".", "#", "#", "#", "#", "#"],
	'c': ["...", "...", ".##", "#..", "#..", "#..", ".##"],
	'1': [".#.", "##.", ".#.", ".#.", ".#.", ".#.", "###"],
	'2': [".##.", "#..#", "...#", "..#.", ".#..", "#...", "####"],
	'.': [".", ".", ".", ".", ".", ".", "#"],
	' ': ["..."] * 7,
}


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

	def stamp(self, rows, x0, y0):
		for j, row in enumerate(rows):
			for i, c in enumerate(row):
				self.set(x0 + i, y0 + j, c)

	def image(self, scale=1):
		im = Image.new('P', (self.w, self.h), 0)
		flat = []
		for _, rgb in PALETTE:
			flat += list(rgb or (0, 0, 0))
		im.putpalette(flat)
		im.putdata([INDEX[c] for row in self.px for c in row])
		if scale != 1:
			im = im.resize((self.w * scale, self.h * scale), Image.NEAREST)
		return im


def icon():
	assert len(ICON) == 40 and all(len(r) == 40 for r in ICON), 'ICON must be 40x40'
	cv = Canvas(40, 40)
	cv.stamp(ICON, 0, 0)
	return cv


def text_mask(font, text, gap):
	"""The rows of `text` as strings of '#' and '.', glyphs separated by `gap` empty columns."""
	h = len(next(iter(font.values())))
	rows = [''] * h
	for i, ch in enumerate(text):
		for j in range(h):
			rows[j] += font[ch][j] + ('.' * gap if i < len(text) - 1 else '')
	return rows


def paint_text(cv, rows, x0, y0, ramp):
	"""Paint a text mask; row j gets colour ramp[j] (flat horizontal colour bands)."""
	for j, row in enumerate(rows):
		for i, c in enumerate(row):
			if c == '#':
				cv.set(x0 + i, y0 + j, ramp[j])


def outline_under(cv, rows, x0, y0, c, drop=1):
	"""1 px outline (4-neighbourhood) around a text mask, plus `drop` extra rows of it below as a drop shadow."""
	h, w = len(rows), len(rows[0])

	def on(i, j):
		return 0 <= j < h and 0 <= i < w and rows[j][i] == '#'
	for j in range(-1, h + 1 + drop):
		for i in range(-1, w + 1):
			if on(i, j):
				continue
			if any(on(i + dx, j + dy - k) for k in range(drop + 1) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
				cv.set(x0 + i, y0 + j, c)


# ---------------------------------------------------------------------------------------------------------- banner

def banner():
	w, h = 240, 60
	cv = Canvas(w, h, '1')
	# faint inventory-grid backdrop: 20 px cells divided by 1 px grooves
	for y in range(h):
		for x in range(w):
			if x % 20 == 0 or y % 20 == 0:
				cv.px[y][x] = 'K'
	for x in range(w):
		cv.px[0][x] = cv.px[h - 1][x] = 'K'
	for y in range(h):
		cv.px[y][0] = cv.px[y][w - 1] = 'K'

	emi = text_mask(TITLE_FONT, 'EMI', 2)
	rem = text_mask(TITLE_FONT, 'Remastered', 2)
	space = 7
	title_w = len(emi[0]) + space + len(rem[0])
	gap = 10
	x0 = (w - (40 + gap + title_w)) // 2
	cv.stamp(ICON, x0, 10)

	tx, ty = x0 + 40 + gap, 16
	rx = tx + len(emi[0]) + space
	outline_under(cv, emi, tx, ty, 'K')
	outline_under(cv, rem, rx, ty, 'K')
	gold = ['Y', 'Y', 'y', 'y', 'y', 'y', 'y', 'y', 'y', 'y', 'o', 'o']
	green = ['h', 'h', 'h', 'h', 'g', 'g', 'g', 'g', 'g', 'g', 'f', 'f']
	paint_text(cv, emi, tx, ty, gold)
	paint_text(cv, rem, rx, ty, green)

	# subtitle on a small rounded plate
	sub = text_mask(SMALL_FONT, 'Fabric 1.21.1', 1)
	bx0, by0, bx1, by1 = tx, ty + 17, tx + len(sub[0]) + 5, ty + 17 + 10
	for y in range(by0, by1 + 1):
		for x in range(bx0, bx1 + 1):
			edge = x in (bx0, bx1) or y in (by0, by1)
			corner = x in (bx0, bx1) and y in (by0, by1)
			if not corner:
				cv.px[y][x] = 'K' if edge else ('3' if y == by0 + 1 else '2')
	paint_text(cv, sub, bx0 + 3, by0 + 2, ['5'] * 7)
	return cv


# ------------------------------------------------------------------------------------------------------------ main

def save(im, rel, transparent):
	path = os.path.join(ROOT, rel)
	os.makedirs(os.path.dirname(path), exist_ok=True)
	if transparent:
		im.save(path, optimize=True, transparency=0)
	else:
		im.save(path, optimize=True)


def main():
	ic = icon()
	save(ic.image(1), 'docs/logo/icon-native.png', True)
	save(ic.image(10), 'docs/logo/icon.png', True)
	save(ic.image(4), 'src/main/resources/icon.png', True)
	save(banner().image(6), 'docs/logo/banner.png', False)


if __name__ == '__main__':
	main()
