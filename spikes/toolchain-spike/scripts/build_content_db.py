"""Build the prepackaged Quran content DB from Room's exported schema JSON.

Run with: python3 -I build_content_db.py <schema.json> <uthmani.xml> <simple-clean.xml> <out.db>
          [--no-master] [--extra-tables] [--bad-hash] [--user-version N] [--skip-triggers]

Executes every entity's createSql (with ${TABLE_NAME} substituted), its index createSql, the
FTS contentSyncTriggers, and the database setupQueries (room_master_table + identity hash)
verbatim, inserts Tanzil text, rebuilds the FTS indexes, sets PRAGMA user_version, VACUUMs.
"""
import argparse, json, os, re, sqlite3, sys, unicodedata
import xml.etree.ElementTree as ET

MARKS = re.compile('[ؐ-ًؚ-ٰٟۖ-ۭـ]')
FOLD = str.maketrans({'أ': 'ا', 'إ': 'ا', 'آ': 'ا', 'ٱ': 'ا',
                      'ى': 'ي', 'ی': 'ي', 'ة': 'ه', 'ک': 'ك'})

def normalize(s: str) -> str:  # must match core/model ArabicNormalizer
    return MARKS.sub('', s).translate(FOLD)

def ayahs(path):
    root = ET.parse(path).getroot()
    for sura in root.iter('sura'):
        for aya in sura.iter('aya'):
            yield int(sura.get('index')), int(aya.get('index')), aya.get('text')

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('schema'); ap.add_argument('uthmani'); ap.add_argument('clean'); ap.add_argument('out')
    ap.add_argument('--no-master', action='store_true', help='omit setupQueries (forces Room schema validation)')
    ap.add_argument('--extra-tables', action='store_true', help='add non-entity tables content_meta, page_line')
    ap.add_argument('--bad-hash', action='store_true', help='write a wrong identity hash')
    ap.add_argument('--user-version', type=int, default=None)
    ap.add_argument('--skip-triggers', action='store_true')
    a = ap.parse_args()
    schema = json.load(open(a.schema, encoding='utf-8'))['database']
    if os.path.exists(a.out):
        os.remove(a.out)
    db = sqlite3.connect(a.out)
    db.execute('PRAGMA journal_mode=DELETE')
    for e in schema['entities']:
        t = e['tableName']
        db.execute(e['createSql'].replace('${TABLE_NAME}', t))
        for ix in e.get('indices', []):
            db.execute(ix['createSql'].replace('${TABLE_NAME}', t))
    for v in schema.get('views', []):
        db.execute(v['createSql'].replace('${VIEW_NAME}', v['viewName']))
    if not a.skip_triggers:
        for e in schema['entities']:
            for trg in e.get('contentSyncTriggers', []):
                db.execute(trg)
    if not a.no_master:
        for q in schema['setupQueries']:
            if a.bad_hash:
                q = q.replace(schema['identityHash'], '0' * 32)
            db.execute(q)
    clean = {(s, n): t for s, n, t in ayahs(a.clean)}
    rows = []
    for gid, (s, n, t) in enumerate(ayahs(a.uthmani), start=1):
        rows.append((gid, s, n, t, normalize(clean[(s, n)])))
    with db:
        db.executemany('INSERT INTO ayah(id, surah, ayah, text_uthmani, text_norm) VALUES (?,?,?,?,?)', rows)
    fts = [e['tableName'] for e in schema['entities'] if e.get('ftsVersion')]
    with db:
        for t in fts:  # idempotent; required when triggers are skipped
            db.execute(f"INSERT INTO `{t}`(`{t}`) VALUES('rebuild')")
            db.execute(f"INSERT INTO `{t}`(`{t}`) VALUES('optimize')")
    if a.extra_tables:
        with db:
            db.execute('CREATE TABLE content_meta (k TEXT PRIMARY KEY, v TEXT NOT NULL)')
            db.executemany('INSERT INTO content_meta VALUES (?,?)', [
                ('data_version', '1'), ('sources', 'Tanzil Uthmani v1.1 + Simple-Clean (CC BY 3.0, tanzil.net)')])
            db.execute('CREATE TABLE page_line (mushaf TEXT, page INTEGER, line INTEGER, first_word INTEGER, last_word INTEGER, PRIMARY KEY (mushaf, page, line)) WITHOUT ROWID')
            db.execute('CREATE INDEX ix_page_line_word ON page_line(first_word)')
    ver = schema['version'] if a.user_version is None else a.user_version
    db.execute(f'PRAGMA user_version = {ver}')
    db.commit()
    db.execute('VACUUM')
    n = db.execute('SELECT COUNT(*) FROM ayah').fetchone()[0]
    hit = db.execute("SELECT COUNT(*) FROM ayah_fts WHERE ayah_fts MATCH ?", (normalize('الرحمن'),)).fetchone()[0]
    tri = db.execute("SELECT COUNT(*) FROM ayah_fts_tri WHERE ayah_fts_tri MATCH ?", (normalize("رحم"),)).fetchone()[0] if "ayah_fts_tri" in fts else -1
    db.close()
    print(f'{a.out}: ayahs={n} fts(الرحمن)={hit} trigram(رحم)={tri} user_version={ver} bytes={os.path.getsize(a.out)} sqlite={sqlite3.sqlite_version}')

if __name__ == '__main__':
    main()
