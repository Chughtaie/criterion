import sys
import uharfbuzz as hb
print('uharfbuzz HarfBuzz', hb.version_string())
words = {'alkitabu': 'الْكِتٰبُ', 'bismi': 'بِسْمِ', 'rahman': 'الرَّحْمٰنِ'}
for path in sys.argv[1:]:
    blob = hb.Blob.from_file_path(path); face = hb.Face(blob); font = hb.Font(face)
    print('#', path.split('/')[-1], 'upem', face.upem)
    for k, w in words.items():
        row = []
        for v in (0, 1, 2, 3, 6):
            buf = hb.Buffer(); buf.add_str(w); buf.guess_segment_properties()
            feats = {'cv01': v} if v else {}
            hb.shape(font, buf, feats)
            row.append((v, sum(p.x_advance for p in buf.glyph_positions), len(buf.glyph_infos)))
        print(' ', k, row)
