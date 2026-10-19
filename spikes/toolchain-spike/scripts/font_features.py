import sys
from fontTools.ttLib import TTFont
f = TTFont(sys.argv[1])
print('tables:', sorted(f.keys()))
name = f['name']
for nid in (1, 2, 5, 13):
    r = name.getName(nid, 3, 1, 0x409) or name.getName(nid, 1, 0, 0)
    if r: print(f'name{nid}:', str(r)[:160])
print('upem', f['head'].unitsPerEm, 'glyphs', len(f.getGlyphOrder()))
for tag in ('GSUB', 'GPOS'):
    if tag not in f: continue
    t = f[tag].table
    feats = {}
    for fr in t.FeatureList.FeatureRecord:
        feats.setdefault(fr.FeatureTag, set()).update(fr.Feature.LookupListIndex)
    print(tag, 'features:', sorted(feats))
    for ft in sorted(k for k in feats if k.startswith('cv'))[:20]:
        types = []
        for li in sorted(feats[ft]):
            lk = t.LookupList.Lookup[li]
            st = lk.LookupType
            if tag == 'GSUB' and st == 7: st = f'7->{lk.SubTable[0].ExtSubTable.LookupType}'
            types.append((li, st))
        print(f'  {tag} {ft}: lookups(type)={types[:12]}')
