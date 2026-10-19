"""Convert a static CFF/CFF2 OpenType font to a glyf (TrueType-outline) TTF, keeping GSUB/GPOS/GDEF."""
import sys
from fontTools.ttLib import TTFont, newTable
from fontTools.pens.cu2quPen import Cu2QuPen
from fontTools.pens.ttGlyphPen import TTGlyphPen

src, dst = sys.argv[1], sys.argv[2]
f = TTFont(src)
gs = f.getGlyphSet()
order = f.getGlyphOrder()
glyf = newTable('glyf'); glyf.glyphOrder = order; glyf.glyphs = {}
for name in order:
    pen = TTGlyphPen(gs)
    gs[name].draw(Cu2QuPen(pen, max_err=1.0, reverse_direction=True))
    glyf.glyphs[name] = pen.glyph()
for t in ('CFF ', 'CFF2', 'VORG', 'HVAR', 'MVAR', 'STAT'):
    if t in f: del f[t]
f['glyf'] = glyf
f['loca'] = newTable('loca')
maxp = f['maxp']; maxp.tableVersion = 0x00010000
for a in ('maxZones','maxTwilightPoints','maxStorage','maxFunctionDefs','maxInstructionDefs','maxStackElements','maxSizeOfInstructions','maxComponentElements'):
    setattr(maxp, a, 0)
maxp.maxZones = 1; maxp.maxComponentDepth = 0
post = f['post']; post.formatType = 2.0; post.extraNames = []; post.mapping = {}; post.glyphOrder = order
f['head'].glyphDataFormat = 0; f['head'].indexToLocFormat = 1
f.sfntVersion = '\x00\x01\x00\x00'
f.save(dst)
g = TTFont(dst)
print(dst, 'glyphs', len(g.getGlyphOrder()), 'tables', sorted(g.keys()))
