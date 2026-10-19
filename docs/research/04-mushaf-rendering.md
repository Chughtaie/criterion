# 04 · Mushaf page rendering & assets

Researcher key: `r04-mushaf-rendering` · Date: 2026-10-19 · Scope: how "Criterion" renders print-faithful mushaf pages, especially the South-Asian **Indo-Pak** mushaf. The pages need ayah tap-selection, ayah highlighting synced to audio, night mode, and must run well on low-end phones.

> **Method.** I inspected QUL (qul.tarteel.ai) resource pages and layout previews and downloaded its public fonts. I shallow-cloned and read the code of `quran/quran_android`, `quran/quran.com-frontend-next`, `quran/quran.com-images`, `TarteelAI/quranic-universal-library`, `DigitalKhatt/mushaf-android`, `DigitalKhatt/digitalkhatt.org` and `marwan/quranwbw`. I read every font's `name` table (license strings) with fontTools. I downloaded quran_android's page PNGs and its `ayahinfo` glyph database and inspected both. I also **built a working prototype**: Python + HarfBuzz + FreeType renders an Indo-Pak 16-line page from QUL line data in three different fonts, with inter-word and with kashida justification (§2.5). The prototype is throwaway research code in the scratchpad, not app code.

---

## 1. Executive summary

1. **The reference mushaf is 16-line, not 13-line.** Report 01 verified that the reference app uses scans of a **16-line Taj Company Indo-Pak** mushaf. The matching digital layout exists: QUL "Indopak 16 lines – Taj company" has **548 pages and 16 lines**, Juz 2 starts on p.20 and Juz 30 on p.528 [verified: QUL]. In this layout each early para fills 18 pages (Juz *n* starts at 18n−16).
2. **Every Indo-Pak variant the owner could want exists as QUL line data:** 16-line Taj (548 pp), 15-line "Hafizi" Qudratullah (610 pp, Juz 2 = p.22, Juz 3 = p.42, 20 pp/para), 13-line Taj (847 pp), 13-line Qudratullah (849 pp), plus 14/17/9-line editions and Madani 604-page layouts [verified]. Line breaks reference **word_index ranges that are shared by every QUL script**, so any layout can be combined with any Indo-Pak text and font. I verified the tokens align 1:1 on a real page.
3. **Recommended primary approach: Unicode font + per-line layout data, justified at runtime (or precomputed), drawn on a Compose Canvas.** The prototype of 16-line p.50 (Āl-ʿImrān 40–49) is hard to tell apart in style from a Taj print. It includes ayah circles with numbers and the ruku "ع" sign. This approach gives exact tap and highlight boxes for free, perfect night mode, crisp zoom and about **1–3 MB** of assets per mushaf, against **48–81 MB** for an image set [verified sizes].
4. **Best licensing stack: DigitalKhatt IndoPak font (SIL OFL 1.1, © 2024 Amine Anane / Tarteel)** [verified name table]. It is paired with the DigitalKhatt Indo-Pak text, which ships in the **MIT-licensed** `digitalkhatt.org` repo (610 pages, 15-line Hafizi layout already included) [verified]. The font covers 100% of that text, with zero `.notdef` glyphs across 748k characters [verified by shaping].
5. **Other Indo-Pak fonts are legally weaker.**
   - **KFGQPC Nastaleeq** is free to *use, copy and distribute*, but "cannot be Sold, Modified…" [verified EULA in font].
   - **"AlQuran IndoPak by QuranWBW"** is the font quran.com uses for Indo-Pak and has the most familiar Taj look. Its license says "NOT FOR SALE, NOT FOR MODIFICATION, NOT FOR DISTRIBUTION OR NOT FOR DEVELOPMENT WITHOUT WRITTEN NOTICE BY QURANWBW.COM" [verified]. **We need written permission before shipping it.**
   - **me_quran** is "Free for non commercial use" [verified].
   - **PDMS Saleem** and **Al Qalam** have no license I could find [unverified].
6. **Do not use scanned Indo-Pak pages.** No openly licensed Indo-Pak scan set exists. archive.org has dozens of Taj/Qudratullah scans uploaded by random users with no provenance [verified search]. Scans also have **no ayah coordinates**, which is the root cause of the reference app's missing highlight and tap features, as well as its poor night mode and blurry zoom (Report 01).
7. **Justification on Android.** Use **per-word `Canvas.drawTextRun` (API 23)** with gaps computed by us. Do not use `StaticLayout` or `TextAlign.Justify`, because they never justify a paragraph's last line. **Never use letter-spacing:** Minikin disables `liga`/`clig` above 0.03 em [verified AOSP source].
   - The DigitalKhatt font stretches words with **kashida through OpenType `cv01`/`cv02` feature values** [verified with HarfBuzz]. Android passes feature *values* through to HarfBuzz via `Paint.setFontFeatureSettings("'cv01' 3")` [verified in Minikin `main`]. So print-like kashida justification is possible **without the NDK**. It still needs a device spike on API 24–30.
8. **Justification is resolution-independent, so precompute it offline.** The whole page scales with width, so we can compute each word's x-position (as a fraction of line width) and kashida values once in the data pipeline. The device then just draws them, with no measuring or shaping at runtime. That is ideal for low-end phones.
9. **Use no bitmaps in the default path.** The design is two Compose layers: a static text/ornament layer in its own `graphicsLayer` and a highlight layer underneath. Audio ticks only invalidate the highlight layer. Memory per page is kilobytes, against 2–10 MB for a page bitmap.
10. **Madani ("Usmani") as the second script.** Render it with the same engine, using a single Unicode font: DigitalKhatt New Madina (OFL) or KFGQPC Hafs v2.2 (free to use, no modification) with QUL's 604-page layout. The exact-replica QCF V2 per-page fonts can come later as an optional download: about **77 MB** as WOFF2 or **219 MB** as TTF for 604 fonts [verified by sampling]. Quran Foundation terms allow bundling them with attribution [verified].
11. **Fallback approach.** Pre-render page images (ALPHA_8 PNG/WebP) **with our own pipeline from the same fonts and data**, together with a generated word-coordinate DB, in the quran.com-images / quran_android style. Deliver them as an optional on-demand pack (~70 MB per layout) [estimate]. Licensing stays clean and highlighting stays exact.
12. **Platform and legal gotchas.**
    - With targetSdk 36, Android 16 **ignores orientation locks on screens ≥ sw600dp**. The temporary opt-out ends at API 37 [verified]. The reference app's "lock" icon will only work on phones.
    - QUL downloads need a free login, and QUL's *data* license is unstated [verified].
    - Pakistani provincial Quran acts require error-free text "certified" by a qualified person (Punjab Act 2011 s.5(3)) [verified via WIPO]. Budget for a hafiz proofreading pass.

---

## 2. Detailed findings

### 2.1 Mushaf variants (page counts, para boundaries, usage)

| Variant | Lines | Pages | Para/juz boundary facts | QUL layout | Typical use (Pakistan) |
|---|---|---|---|---|---|
| **Indo-Pak 16-line, Taj Co.** | 16 | **548** | Juz 2 p.20 (2:142), Juz 3 p.38 (2:253), Juz 4 p.56 (3:92), Juz 30 p.528 (78:1). Early paras are 18 pp and each starts at the top of a page. pp.1–2 are ornamental: on p.2 lines 1–9 are empty, followed by the Baqarah header, the bismillah and 5 lines [verified: QUL previews] | mushaf 7 / resource 11 | The **household mushaf** and the one the reference app scans [verified Report 01]. Default for us. |
| **Indo-Pak 15-line "Hafizi", Qudratullah (Lahore)** | 15 | **610** | Juz 2 p.22, Juz 3 p.42, so 20 pp per para. "Every page ends with an ayah" in Hafizi prints [verified: QUL, retailer listing] | mushaf 6 / resource 12 (also offers an "images" download) | **Huffaz and madrasas.** A hafiz in the reference app's reviews asked for 15-line (Report 01). |
| Indo-Pak 15-line, KFGQPC Nastaleeq | 15 | 610 | Same breaks as Qudratullah 15, typeset by KFGQPC | mushaf 14 / resource 8 | Saudi-printed Indo-Pak edition |
| QPC Nastaleeq 16-line | 16 | 548 | Same breaks as Taj 16, text in KFGQPC encoding | mushaf 15 | For the KFGQPC Nastaleeq font |
| **Indo-Pak 13-line, Taj Co.** | 13 | **847** | Juz 2 p.28 [verified] | mushaf 23 / resource 313 | Large print, popular in India, SA and UK, used for nazira classes and elderly readers [unverified] |
| Indo-Pak 13-line, Qudratullah | 13 | 849 | — | mushaf 17 / resource 236 | Same as above. archive.org has an 849-image scan of this edition. |
| Indo-Pak 14-line, Pak Co. | 14 | 731 | — | mushaf 8 | Pocket editions ("Pak script code 731p") |
| Indo-Pak 17-line, Taj | 17 | 487 | — | mushaf 18 | Compact |
| Indo-Pak 9-line, Gaba | 9 | 1,890 | — | mushaf 29 / resource 571 | Very large print |
| **Madani 15-line (KFGQPC 1405H/1421H/1441H)** | 15 | **604** | Juz *n* ≈ p.20n−18 (standard) | mushafs 1, 2, 5, 19, 20, 21, 27 | Gulf; the reference app's "Usmani" option |

All page and line counts above come from qul.tarteel.ai/mushaf_layouts [verified]. "Owner's screenshot shows ~13 lines": Report 01 established it is the 16-line edition. **We ship 16 (default), 15 and 13.** Each extra layout is only ~150–250 KB of data with this approach.

### 2.2 Approach A: Image-based (scans or pre-rendered pages)

**How quran_android does it** (GPL-3.0 code) [verified by reading code and downloading the data]:

| Item | Finding |
|---|---|
| Image source | `https://android.quran.com/data` redirects to `files.quran.app/hafs/madani/width_{W}/pageNNN.png`. The Madani pages are **rendered from the KFGQPC QCF V1 fonts** by `quran/quran.com-images`. Its README says: "The code is copyleft GPL… but the actual fonts and pages… belong to the King Fahd Quran Complex". |
| Widths | 320/480/800/1024/1260 px, chosen by screen size (`DefaultPageSizeCalculator`) |
| Format | 4-bit **indexed PNG** with transparency, 1260×2038 px. p.2 is 45 KB and p.50 is 126 KB. |
| Zip sizes | `images_800.zip` **48.2 MB**, `images_1024.zip` **63.4 MB**, `images_1260.zip` **81.1 MB** (HTTP Content-Length) |
| Decode | `BitmapFactory` with `inPreferredConfig = ALPHA_8`, i.e. 1 byte/px: about 2.6 MB per 1260×2038 page instead of 10.3 MB in ARGB |
| Tap/highlight data | `ayahinfo_1260.db`, 5.9 MB (2.2 MB zipped), table `glyphs(glyph_id, page_number, line_number, sura_number, ayah_number, position, min_x, max_x, min_y, max_y)` with **88,246 boxes**, one per word plus ayah markers |
| Night mode | `ColorMatrixColorFilter` inversion `[-1,0,0,0,b; 0,-1,0,0,b; 0,0,-1,0,b; 0,0,0,1,0]`, where `b` is a user brightness value (`HighlightingImageView.adjustNightMode`) |
| Newer "line-by-line" mode | Compose `Canvas.drawImage` draws **one PNG per line** with a `ColorFilter`, plus SQLDelight tables `ayah_highlights(page,line,left,right)`, `ayah_markers`, `sura_headers` (`feature/linebyline`). Lines reflow vertically. |

**Indo-Pak image sources and their licenses:**

| Source | What | License status |
|---|---|---|
| quran_android / quran.com | **Madani only** (open repos ship only `madani` and `warsh` providers) | Code is GPL-3. Images are derived from KFGQPC fonts. Quran Foundation terms allow bundling "Mushaf images obtained through QF APIs or documented CDN URLs" if we keep a Developer Console account, give attribution, and do not offer them separately [verified QF legal page]. |
| QUL resource 12 (15-line Qudratullah) | "Download images" button (login required) | Not stated [verified page] |
| archive.org | Dozens of Taj 16/17/18-line, 15-line and 13-line scans. Examples: `AlQuranAlKareem16LinesTajCompany`, `al-quran-16-lines-taj-company`, `quran-13-line` (849 images), `16-line-quran-tajwid-colored` (555 images) | Uploaded by individuals. Most have no license; a few *claim* CC BY-ND or PD, which uploaders cannot validly grant for publisher artwork [verified search API]. Taj Company calligraphy is copyrighted under Pakistan's Copyright Ordinance 1962 (life+50 or publication+50 depending on authorship) [term rules unverified; secondary sources disagree]. |

**Pros:** pixel-exact print (for scans) and a trivial renderer.
**Cons for Indo-Pak scans:**
- Licensing is the blocker.
- Each layout weighs 50–80 MB.
- **There are no word or ayah coordinates.** Building them means segmenting 548 pages × ~170 words and aligning them to text, with heavy manual QA.
- Night mode needs inversion, and grey paper and noise look bad (users complain about exactly this, Report 01).
- Zoom is bitmap zoom ("pages expand beyond your screen", Report 01).
- Every OCR or print error is frozen into the images.

### 2.3 Approach B: Glyph-per-word page fonts (QCF V1/V2/V4)

KFGQPC publishes one font per Madani page (604 fonts). Each word is one glyph, addressed by `code_v1`/`code_v2` PUA codepoints, so the print reproduces exactly. Measured on QUL's CDN (6-page sample × 604):

| Set | Avg/page | ≈ Total | License string in font |
|---|---|---|---|
| QCF V1 "optimized" TTF | 101 KB | **57 MB** | "King Fahad Complex, All rights reserved." |
| QCF V2 TTF | 381 KB | **219 MB** | "Test Font, KFGQPC" |
| QCF V2 WOFF2 | 135 KB | **77 MB** | – |
| QCF V4 Tajweed TTF / WOFF2 | 250 / 73 KB | **143 / 42 MB** | – |

This approach is **Madani only**: no per-page glyph fonts exist for any Indo-Pak edition. It works for an optional "High-fidelity Madani" download. Android's `Typeface` loads TTF/OTF; WOFF2 support is [unverified], so ship TTF and compress it in transit.

### 2.4 Approach C (recommended): Unicode font + QUL line layout

**QUL layout data model** [verified: docs page, the exporter `lib/exporter/export_mushaf_layout.rb`, and the preview HTML]:

```sql
-- layout DB (one per mushaf), exported by QUL
CREATE TABLE info  (name TEXT, number_of_pages INT, lines_per_page INT, font_name TEXT);
CREATE TABLE pages (page_number INT, line_number INT,
                    line_type TEXT,          -- 'ayah' | 'surah_name' | 'basmallah'
                    is_centered INT,         -- 1 = centered, 0 = fully justified
                    first_word_id INT,       -- word_index range (inclusive) for 'ayah' lines
                    last_word_id INT,
                    surah_number INT);       -- for 'surah_name' lines
-- script DB (one per text encoding), exported by QUL
CREATE TABLE words (word_index INT PRIMARY KEY, word_key TEXT /* "s:a:w" */, surah INT, ayah INT, text TEXT);
```

- **Ayah-end markers are their own "words"** (`char-end` in the preview), e.g. `word_index 5553 = 2:5:9`. Waqf signs stay attached to their word.
- **Word indices are shared across scripts.** For p.50 of the 16-line layout, the DigitalKhatt Indo-Pak tokens split into the exact per-line word counts `[11,11,9,8,8,9,9,10,9,10,10,10,12,7,10,9]`, and every line's last word matches the QUL line [verified].
- Exports come as SQLite, JSON (one file per page) or DOCX. **Downloads require a (free) QUL login**: the button opens `/users/sign_in?…` [verified]. The QUL *code* is MIT ("Copyright (c) 2024-present Tarteel, Inc"). The *data* license is unstated, and the FAQ says to "check… dataset-specific licensing details before production use" [verified].
- Related QUL metadata: Juz (68), Hizb (67), Rub (63), **Ruku (65)**, **Manzil (66)**, Sajda (64), Surah names (70) [verified listing]. The quran.com v4 verse object also carries `ruku_number`, `manzil_number`, `juz_number`, `hizb_number`, `rub_el_hizb_number` and `sajdah_number` [verified API response].

**Indo-Pak fonts compared** (license text read from each font's `name` table):

| Font (QUL id) | Origin / version | Size raw → deflated | License (quoted) | Look | Verdict |
|---|---|---|---|---|---|
| **DigitalKhatt IndoPak** (568; `digitalkhatt.org/…/hb/indopak.otf`) | Amine Anane & Tarteel, v0.1, 2024. CFF2 outlines. The repo version has **`cv01…cv18` justification features**; QUL's 0.49 MB build lacks them. | 2.57 → 0.61 MB (converted to glyf TTF: 1.63 → 0.34 MB) | "This Font Software is licensed under the **SIL Open Font License, Version 1.1**" | Bold, Taj-like | **Primary** |
| KFGQPC Nastaleeq (462) | King Fahd Complex, v0.10 (2018), by Alif Lam Mim Tech | 0.25 → 0.12 MB | "Permission is hereby granted, Free of Cost… to Use, Copy, Distribute, subject to… The Font Software cannot be **Sold, Modified**, Altered…" | Thinner, Saudi Indo-Pak | **Alternate** (no modifying or subsetting) |
| AlQuran IndoPak by QuranWBW (242; quran.com's `IndoPak` font v4.2.x) | Ayman Siddiqui, based on Al Qalam Quran Majeed, v2.100 | 0.31 → 0.11 MB | "Made only for Sadaqa-e-Jaria… **NOT FOR SALE, NOT FOR MODIFICATION, NOT FOR DISTRIBUTION OR NOT FOR DEVELOPMENT WITHOUT WRITTEN NOTICE BY QURANWBW.COM**" | Most familiar (quran.com Indo-Pak) | Only with **written permission** |
| PDMS Saleem QuranFont (244) | Pak Data Management Services | – | Nothing stated; the pakdata.com page is "All rights reserved" (site) [unverified] | Classic Pakistani | Avoid unless licensed |
| Al Qalam Quran Majeed | Al Qalam | – | Not found [unverified] | – | Avoid |
| me_quran (243) | Meor Ridzuan, 2005 | 0.46 MB | "**Free for non commercial use.** Other use, contact…" | Madani-ish | Avoid |
| DigitalKhatt New Madina (for Madani) | Anane & Tarteel, v0.1 | 2.4 MB otf | **OFL 1.1** | Madinah | Madani option |
| KFGQPC HAFS Uthmanic v2.2 (245) | KFGQPC | 0.30 → 0.15 MB | Same KFGQPC EULA as above | Madinah | Madani option |

**How quran.com renders Indo-Pak** (MIT, `quran.com-frontend-next`) [verified]:
- `fonts.scss` declares `font-family:'IndoPak'` from `indopak-nastaleeq-waqf-lazim-v4.2.1.woff2` (the QuranWBW font).
- Each mushaf line is a flex row with `justify-content: space-between`. That is **inter-word justification at a fixed line width**: per-font-scale `--line-width` values such as `text_indopak_16_lines` (tablet `55vh…190vh`).
- Centered lines are hard-coded only for Madani pages (`CENTER_ALIGNED_PAGES = [1,2]` plus a per-page map). Indo-Pak never centres.
- Above font scale 3 or with word-by-word on, it falls back to a "big text layout" that wraps naturally.

**DigitalKhatt engine** [verified]:
- `DigitalKhatt/mushaf-android` (MIT, 2023–24) renders through **NDK + a forked HarfBuzz** (`hb_buffer_set_justify`, per-glyph `lefttatweel/righttatweel` written into variation coords) + Skia.
- `digitalkhatt.org` (MIT) justifies in JS by applying `cv01`/`cv02`/`cv10…cv18` feature values per word range.
- Too heavy for us as-is: native toolchain and a non-upstream HarfBuzz. The **feature-value technique itself works with stock HarfBuzz**, which is what Android uses. See the `cv01` widths below.

### 2.5 Prototype results (verified in this session)

`render_page.py` / `render_page_k.py` take a 16-line page's word ranges, shape each word with HarfBuzz, justify each line, and rasterize with FreeType. These are throwaway research scripts in the session scratchpad: `…/scratchpad/scripts/`, with outputs in `…/scratchpad/repos/r04-mushaf-rendering/proto/`.

| Run | Font + text | Result |
|---|---|---|
| p.50, inter-word gaps, per-line shrink | QuranWBW + QUL Indo-Pak Nastaleeq text | Very Taj-like. The ayah circles show numbers, and the ruku "ع" appears over 3:41. |
| p.50, same | KFGQPC Nastaleeq + QPC Nastaleeq text (PUA waqf marks) | Clean, thinner strokes, also correct |
| p.50, **one font size per page** (fit widest line) + inter-word gaps | **DigitalKhatt IndoPak + DK text (OFL/MIT)** | Gaps range **0.18–0.52 em** (mean ≈ 0.33) |
| p.50, one size per page + **whole-word `cv01` kashida (≤3 steps)** + gaps | DK IndoPak | Gaps evened out to **0.18–0.31 em**; stretched words (e.g. اصطفٰك) look like print |
| Same at **720 px** width (low-end phone) using a **CFF2→TrueType conversion** (fontTools cu2qu) | DK IndoPak glyf TTF 1.63 MB | Legible. Advances and `cv01` widths are identical to the original (2060/2060 and 4384/4384 units) |

HarfBuzz word-width deltas from `cv01` on DK IndoPak (font units, upem 1000): الْكِتٰبُ 2060 → +787 (cv01=1), +2324 (=3), +4631 (=6). The coverage check over the whole 610-page DK text: 84 distinct code points, **0 `.notdef`** after shaping.

### 2.6 Android text APIs relevant to the engine (verified on developer.android.com / AOSP)

| API | Level | Use |
|---|---|---|
| `Canvas.drawTextRun(CharSequence, start, end, ctxStart, ctxEnd, x, y, isRtl, Paint)` | **23** | Draw one shaped word at our computed x (works on minSdk 24) |
| `Paint.getRunAdvance(…)` | 23 | Measure a word on-device (only if offline metrics are missing or mismatched) |
| `Paint.setFontFeatureSettings(String)` | 21 | CSS syntax, e.g. `"'cv01' 2"`. Minikin parses it with `hb_feature_from_string` and **keeps the value** [AOSP `FontFeatureUtils.cpp`, main] |
| `Paint.setLetterSpacing` | 21 | **Do not use.** Above 0.03 em Minikin forces `liga=0, clig=0`, which breaks Quran ligatures [AOSP] |
| `TextRunShaper.shapeTextRun` → `PositionedGlyphs`, `Canvas.drawGlyphs` | **31** | Optional fast path: cache glyph arrays per page on Android 12+ |
| `Paint.setFontVariationSettings` | 26 | Not needed (the DK IndoPak build has no `fvar`) |
| `Bitmap.Config.ALPHA_8` (1 B/px) / `HARDWARE` | 1 / 26 | For the image fallback only |

`StaticLayout`'s `JUSTIFICATION_MODE_INTER_WORD` (API 26) and Compose `TextAlign.Justify` never justify the last line of a paragraph, and they choose their own line breaks. Mushaf lines must be laid out by hand.

### 2.7 Hybrid elements (frame, headers, markers)

| Element | Plan | Data |
|---|---|---|
| Page frame (double rule + floral corners), page header (para name right, surah name left, page no. centre), footer | **Original vector art** we commission or draw, as `ImageVector`/VectorDrawable tinted by theme. No open-licensed Indo-Pak frame exists. QUL's surah-name and juz-name fonts carry no license, and the QCF surah header says "King Fahad Complex, All rights reserved". | page → juz, surah (from layout) |
| Surah header line (`surah_name`) | Our vector cartouche plus the name typeset in the Indo-Pak font. Indo-Pak headers also show the ayah count and ruku count. | QUL surah names (70) + counts |
| Bismillah line | Typeset from the text (DK has a `basm` feature) or as a ligature | layout `basmallah` |
| Ayah-end circle with number | **The font draws it** (`۝` + Arabic-Indic digits in DK; `۟` in QuranWBW; digits + PUA in QPC) | in text |
| In-text ruku sign over ayah end | In the DK/QWBW text (seen at 3:41) | in text |
| **Margin ruku marker "ع"** with 3 numerals (ruku in surah / ayahs in ruku / ruku in para) | Drawn in the outer margin at the y of the line containing the ruku-ending ayah word | QUL Ruku (65), derived counts |
| **"منزل ۱–۷"** marker | Margin or foot glyph at the line where the manzil starts | QUL Manzil (66) |
| Para quarter markers (ربع / نصف / ثلاثة) and sajda "سجدہ" | Margin text | Sajda: QUL (64). **Indo-Pak quarter positions: no verified dataset.** Hand-enter 90 points from the print. |

### 2.8 Compose implementation sketch

**Architecture**

```
:core:mushaf-data  Room (prepackaged mushaf.db) → MushafRepository (LRU<PageKey, PageModel>)
:core:mushaf-render  PageGeometry (pure Kotlin: fractions → px), PaintCache, Ornaments
:feature:reader    MushafPager, MushafPage, AyahActionSheet, ReaderViewModel(StateFlow)
:core:audio        Media3 player → StateFlow<AyahKey?> (+ optional word index)
```

**Data the renderer consumes** (precomputed offline, §2.10):

```kotlin
data class WordRun(
    val wordIndex: Int, val surah: Int, val ayah: Int, val text: String,
    val leftFrac: Float,      // left edge as fraction of line width (RTL handled offline)
    val widthFrac: Float,
    val features: String?,    // e.g. "'cv01' 2" for kashida, null otherwise
    val isAyahEnd: Boolean,
)
data class LineRun(val no: Int, val type: LineType, val centered: Boolean, val words: List<WordRun>, val surah: Int?)
data class PageModel(val mushafId: Int, val page: Int, val juz: Int, val lines: List<LineRun>,
                     val emPerLineWidth: Float,             // font size = emPerLineWidth * lineWidthPx
                     val margin: List<MarginMarker>)
```

**Pager (RTL, page 1 on the right, prefetch neighbours)**

```kotlin
@Composable
fun MushafPager(mushafId: Int, pageCount: Int, startPage: Int, vm: ReaderViewModel) {
    val pager = rememberPagerState(initialPage = startPage - 1) { pageCount }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {   // next page enters from the left
        HorizontalPager(state = pager, beyondViewportPageCount = 1, key = { it }) { index ->
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                val model by produceState<PageModel?>(null, mushafId, index) {
                    value = withContext(Dispatchers.IO) { vm.repo.page(mushafId, index + 1) }
                }
                model?.let { MushafPage(it, vm) } ?: PagePlaceholder()
            }
        }
    }
    // follow audio: flip page when the recited ayah moves off-page (unless user is browsing)
    LaunchedEffect(mushafId) {
        vm.playingAyah.filterNotNull().collect { key ->
            val target = vm.repo.pageOf(mushafId, key) - 1
            if (vm.followAudio.value && target != pager.currentPage) pager.animateScrollToPage(target)
        }
    }
}
```

**Page: two layers so audio ticks never re-record the text**

```kotlin
@Composable
fun MushafPage(model: PageModel, vm: ReaderViewModel) {
    val theme = LocalMushafTheme.current          // paper, ink, highlight, ornament colours (light/sepia/night/AMOLED)
    val typeface = LocalMushafTypeface.current    // Typeface.createFromAsset(…, "fonts/dk_indopak.ttf")
    val highlighted by vm.highlightFor(model.page).collectAsStateWithLifecycle(emptySet())
    BoxWithConstraints(Modifier.fillMaxSize().background(theme.paper)) {
        val geo = remember(model, constraints) { PageGeometry(model, constraints.maxWidth, constraints.maxHeight) }
        val paints = remember(typeface, theme, geo.fontPx) { PaintCache(typeface, theme.ink.toArgb(), geo.fontPx) }
        // Layer 1 – dynamic highlight (cheap to invalidate on every ayah/word change)
        Canvas(Modifier.matchParentSize()) {
            highlighted.forEach { key -> geo.rectsFor(key).forEach {
                drawRoundRect(theme.highlight, it.topLeft, it.size, CornerRadius(6.dp.toPx())) } }
        }
        // Layer 2 – static text + ornaments recorded once into its own RenderNode
        Canvas(
            Modifier.matchParentSize().graphicsLayer()
                .pointerInput(geo) {
                    detectTapGestures(onTap = { vm.onTap(geo.hit(it)) }, onLongPress = { geo.hit(it)?.let(vm::openAyahSheet) })
                }
                .semantics { /* per-ayah nodes: "Al-Baqarah, ayah 255", custom actions Play / Bookmark */ }
        ) {
            drawOrnaments(geo, theme)                         // ImageVector frame, headers, margin markers
            drawIntoCanvas { c ->
                val nc = c.nativeCanvas
                for (w in geo.words) nc.drawTextRun(w.text, 0, w.text.length, 0, w.text.length,
                                                    w.xPx, w.baselinePx, /* isRtl = */ true, paints[w.features])
            }
        }
    }
}
```

`geo.hit(offset)` works like this: line = ⌊(y − top)/lineH⌋, then the word whose `[left − gap/2, right + gap/2]` contains x, which gives `AyahKey`. `geo.rectsFor(key)` unions the word boxes per line, which gives 1–3 rounded rects. Word-level highlight (for hifz) is the same with one word.

**Other reader behaviours**

| Concern | Implementation |
|---|---|
| Prefetch / cache | `beyondViewportPageCount = 1` plus repository `LruCache` of ~12 `PageModel`s (≈10–20 KB each). Warm the cache for ±2 pages on page change. |
| Dual-page landscape | `BoxWithConstraints`: if `maxWidth > maxHeight && maxWidth ≥ 600.dp`, each pager item is a **spread (odd page right, even page left)**, so spread *k* = pages (2k+1, 2k+2). That matches print (Fātiḥa right, Baqarah left). Phones in landscape show one page fitted to width inside a vertical scroll, as quran_android does. |
| Zoom | During the pinch: `graphicsLayer` scale/translate (cheap). On release: re-lay out at the new `fontPx`, which is crisp because it is vector. Above about 1.6×, offer a **"Large text" reflow mode** (quran.com's Big Text Layout) instead of panning. `telephoto` (Apache-2.0, 0.19.0 / 1.0.0-alpha02) is only for the image fallback. |
| Page turn | Default: slide. Optional "book" effect with `graphicsLayer { rotationY; cameraDistance }`. A true curl via `io.github.oleksandrbalan:pagecurl` 1.5.1 (Apache-2.0, last release Feb 2024 [verified]) or AGSL (API 33+) can come later. |
| Vertical continuous mode | `LazyColumn` of the same `MushafPage` (the reference app added this in 2026) |
| Immersive | `WindowCompat.getInsetsController(window, view)`: `systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE; hide(systemBars())`. A tap toggles a **translucent, auto-hiding** toolbar (users complain the reference overlay blocks text). Edge-to-edge is mandatory at targetSdk 36 [verified]. |
| Keep screen on | `LocalView.current.keepScreenOn = true` in a `DisposableEffect` while the reader is visible (user setting) |
| Orientation lock | `requestedOrientation = SCREEN_ORIENTATION_LOCKED`. **Ignored on ≥ sw600dp for targetSdk 36.** The opt-out `android.window.PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY` is temporary and gone at API 37 [verified], so hide the lock icon on large screens. |
| Themes | Paper and ink tokens: Light (#FFFDF7 / #1A1A1A), Sepia (#F5ECD7 / #3B2F2F), Night (#121212 / #E6E6E6 with a text-brightness slider like quran_android's), AMOLED (#000 / #CFCFCF). Ornaments are tinted, and highlight alpha is about 0.25–0.35. Optional per-word tajweed colouring later (text spans by rule). |
| Audio sync | Media3 ExoPlayer (latest 1.11.1 [verified Maven]). For **gapless surah files**, the ayah timing table (QUL segments) plus `player.createMessage{…}.setPosition(ms)` at each ayah start, with position polling at ~100 ms as a fallback [API details unverified]. For **per-ayah files**, `onMediaItemTransition` gives the ayah directly. Both emit `StateFlow<AyahKey>` to the reader. QUL word-level segments allow word highlight for some reciters. |
| Accessibility | Semantics node per ayah (bounds = union of rects) with label "Surah, ayah N" and custom actions (Play from here, Bookmark, Translation). Large-text reflow mode respects font scale. Contrast ≥ 4.5:1 in all themes. |
| Memory budget (2 GB device) | Default path: **no page bitmaps**. Text is GPU glyph-cache plus display lists, and three resident pages cost well under 5 MB. Typeface is about 1.6 MB, memory-mapped. Image fallback: ALPHA_8 at 1080×1750 ≈ 1.9 MB/page × 5 = ~10 MB. |
| Very large glyphs | hwui draws very large text as paths rather than from the atlas, which is slower. If zoom shows jank, cache the zoomed page into an ALPHA_8 bitmap off-thread [unverified threshold]. |

### 2.9 App-size math

| Asset | Raw | Compressed (deflate-9) | Bundled? |
|---|---|---|---|
| DK IndoPak font (glyf TTF with cvNN) | 1.63 MB | 0.34 MB | Yes |
| KFGQPC Nastaleeq (alternate font) | 0.25 MB | 0.12 MB | Yes (unmodified) |
| Indo-Pak word text (~84.5k tokens) + one 16-line layout (8,768 rows) | 2.64 MB | 0.93 MB | Yes |
| Each extra layout (15-line 9,150 rows; 13-line ~11k rows; Madani 9,060 rows) | ~0.15–0.25 MB | ~0.06–0.1 MB | Yes |
| Offline metrics (x-fraction + width + kashida per word per layout, ~4 B × 84.5k) | ~0.35 MB/layout | ~0.2 MB | Yes |
| Madani text (Hafs/DK V2) + font | ~2.5 MB + 0.3–2.4 MB | ~1–1.5 MB | Yes |
| **Total for 16/15/13 Indo-Pak + Madani** | **≈ 9–11 MB on disk** | **≈ 3.5–4.5 MB of download** | – |
| Optional: QCF V2 Madani HQ fonts | 219 MB TTF / 77 MB WOFF2 | – | On-demand |
| Optional: pre-rendered image fallback, per layout | ~548 × 120 KB ≈ 65–75 MB [estimate from Madani 81 MB / 604] | – | On-demand |
| *Comparison: reference app* | Base 26.5 MB, plus a ~50 MB page set downloaded after install (Report 01) | | |

Play limits [verified]:
- Base module ≤ 500 MB; each asset pack ≤ 1.5 GB; on-demand plus fast-follow packs ≤ 30 GB in total.
- On-demand and fast-follow packs **don't count toward the listed app size**.
- The PAD docs are written for games. Our optional downloads can equally come from our own CDN through WorkManager, which also works for sideloaded builds.

### 2.10 Data pipeline → on-device `mushaf.db`

This lives under `tools/mushaf-pipeline/`: Python with `uharfbuzz` and `fontTools`. It runs on a dev machine or in CI, never in the app.

1. **Acquire.**
   - QUL SQLite exports (owner signs up for free): layouts 7 (16 Taj), 6 (15 Qudratullah), 23 (13 Taj) and 5 or 20 (Madani); scripts 565 (DK Indo-Pak word-by-word), 52 (QPC Nastaleeq), 312 (KFGQPC Hafs) or 48 (DK V2); metadata 63–70.
   - DK Indo-Pak 15-line text from `digitalkhatt.org` (MIT, 610 pages, already line-broken). This is an independent cross-check of QUL layout 6.
   - Tanzil Uthmani text, used **only for verification**.
2. **Normalize** into one `words` table keyed by `word_index`, with one text column per script.
3. **Fonts.**
   - Convert DK IndoPak CFF2 to TrueType glyf (fontTools cu2qu). OFL allows this, and DK declares no Reserved Font Name. Rename it "Criterion IndoPak" and ship OFL.txt.
   - Do **not** touch the KFGQPC fonts (their EULA forbids modification).
4. **Metrics and justification (offline, resolution-independent).**
   - For each layout, choose `emPerLineWidth` per page: shape every line with HarfBuzz, take the widest line with a minimum 0.18 em gap, and fit it.
   - For each justified line, add `cv01` steps (≤3, longest words first, skipping ayah markers) until the remaining gap is ≤0.3 em, then spread the rest evenly between words.
   - Store each word's `left_frac`, `width_frac` and `features`.
   - Centered lines, `surah_name` and `basmallah` lines are centered.
5. **Markers.** `page_meta(juz_start, manzil_start, sajda)` and `margin_marker(page, line, kind, n1, n2, n3)`, computed by locating each metadata ayah's last `word_index` in the layout.
6. **Validate.** CI fails the build if any check fails.
   - Every word_index appears exactly once per layout, in order.
   - Page counts are 548/610/847/604.
   - Juz start pages match a table hand-verified against print.
   - The skeleton of each script (diacritics stripped, Indo-Pak letter forms normalized) matches Tanzil ayah by ayah.
   - Zero `.notdef` after shaping.
   - No line overflows after fitting.
   - Render all pages to PNG/PDF for **human proofreading by a hafiz** and as **screenshot-test baselines** (Roborazzi/Paparazzi). The on-device instrumentation test compares `Paint.getRunAdvance` against the offline widths (tolerance 0.5%).
7. **Package.** Write `app/src/main/assets/databases/mushaf.db`, opened with Room `createFromAsset` and versioned. Fonts go in `assets/fonts/`.

```sql
CREATE TABLE mushaf(id INTEGER PRIMARY KEY, name TEXT, lines_per_page INT, pages INT, script TEXT, font TEXT);
CREATE TABLE word(word_index INTEGER PRIMARY KEY, surah INT, ayah INT, pos INT, is_end INT,
                  text_dk_indopak TEXT, text_qpc_nastaleeq TEXT, text_hafs TEXT);
CREATE TABLE line(mushaf_id INT, page INT, line INT, type INT, centered INT,
                  first_word INT, last_word INT, surah INT, PRIMARY KEY(mushaf_id, page, line)) WITHOUT ROWID;
CREATE TABLE word_pos(mushaf_id INT, font_id INT, word_index INT, left_frac REAL, width_frac REAL, features TEXT,
                  PRIMARY KEY(mushaf_id, font_id, word_index)) WITHOUT ROWID;
CREATE TABLE page_meta(mushaf_id INT, page INT, juz INT, em_per_line_width REAL, manzil_start INT, PRIMARY KEY(mushaf_id, page)) WITHOUT ROWID;
CREATE TABLE margin_marker(mushaf_id INT, page INT, line INT, kind INT /*ruku,manzil,quarter,sajda*/, n1 INT, n2 INT, n3 INT);
CREATE TABLE ayah_page(mushaf_id INT, surah INT, ayah INT, page INT, PRIMARY KEY(mushaf_id, surah, ayah)) WITHOUT ROWID;
```

### 2.11 Decision matrix

Scores run from 1 (bad) to 5 (best). Effort: 5 = least effort.

| Approach | Fidelity to Indo-Pak print | Licensing | Size | Night mode | Tap/highlight accuracy | Zoom crispness | Effort | Low-end perf | **Total** |
|---|---|---|---|---|---|---|---|---|---|
| A1. Scanned Taj pages + hand-built coordinates | 5 | **1** | 2 (50–80 MB) | 2 (inverted scans) | 2 | 2 | 2 | 4 (ALPHA_8) | 20 |
| A2. **Self-rendered page images + generated coordinates** (fallback) | 4 | 4 | 2 (~70 MB) | 4 (alpha mask tint) | 5 | 3 | 3 | 4 | 29 |
| B. QCF per-page glyph fonts | n/a for Indo-Pak (Madani: 5) | 3 (KFGQPC rights, QF terms) | 1–2 (77–219 MB) | 5 | 5 | 5 | 3 | 3 | Madani HQ option |
| **C1. Unicode font + QUL layout + inter-word justification** | 4 | 4–5 (DK OFL; QUL data TBC) | **5 (<5 MB)** | **5** | **5** | **5** | 3 | 4 | **35** |
| **C2. C1 + precomputed `cv01` kashida** (target) | **4.5** | 4–5 | 5 | 5 | 5 | 5 | 3 | 4 | **35.5** |
| D. DigitalKhatt native engine (NDK + forked HarfBuzz + Skia) | 5 | 4 | 4 | 5 | 4 | 5 | **1** | 3 | 31 |

---

## 3. Recommendations

1. **Adopt approach C (Unicode font + QUL line layout, our own Canvas renderer) as the only v1 renderer, for both Indo-Pak and Madani.**
   - *Rationale:* it is the only option that scores well on everything the owner cares about (print look, ayah tap and audio highlight, night mode, zoom, size, low-end speed). The 16-line prototype proves the look.
   - *Confidence:* **High.**
2. **Default mushaf = Indo-Pak 16-line Taj (548 pp, QUL layout 7). Also ship 15-line Hafizi (610 pp) and 13-line Taj (847 pp) in the APK, plus Madani 15-line (604 pp).**
   - *Rationale:* 16-line matches the reference app and the most common household mushaf. Huffaz need 15-line, and 13-line serves large-print readers. Each extra layout costs about 0.1 MB.
   - *Confidence:* **High** for 16 and 15; **Medium** for 13 being worth it in v1.
3. **Default font = DigitalKhatt IndoPak (OFL), converted to TrueType glyf and renamed. The "font style" setting offers KFGQPC Nastaleeq (unmodified) as an alternative. Email quranwbw.com now for written permission to ship "AlQuran IndoPak", which is the most familiar look.**
   - *Rationale:* DK is the only Indo-Pak Quran font with an open license that allows modification. Its text sits in an MIT repo, and coverage is complete.
   - *Confidence:* **High** on licensing. **Medium** on DK v0.1 visual polish, which needs a hafiz's review of all pages.
4. **Precompute justification offline: x-fractions per word plus `cv01` kashida. The device only draws.**
   - *Rationale:* layout scales linearly with width, so one offline solution holds for every screen. That removes runtime shaping and measuring on low-end phones and makes rendering deterministic for screenshot tests.
   - Run a **1-day device spike first**: check that `setFontFeatureSettings("'cv01' 2")` changes advances on API 24, 26, 30 and 35, using the converted TTF. If not, ship inter-word only (C1), which already looks good.
   - *Confidence:* **Medium-High.**
5. **Render with per-word `Canvas.drawTextRun` inside a dedicated `graphicsLayer`, with a separate highlight layer underneath. Never use letter-spacing, `StaticLayout` justification or `TextAlign.Justify` for mushaf lines.**
   - *Rationale:* we get exact boxes for hit-testing and highlighting, audio updates stay cheap, and it works from minSdk 24. Verified API levels and Minikin behaviour back this.
   - *Confidence:* **High.**
6. **Build the data pipeline (`tools/mushaf-pipeline`) with the validation gates in §2.10 and produce one versioned `mushaf.db` (≈3–4 MB), shipped through Room `createFromAsset`.**
   - The owner should **create a QUL account and download the layout, script and metadata SQLite files**. Also **open a GitHub issue on TarteelAI/quranic-universal-library to confirm the data license** for bundling in a free app (commercial if IAP or ads are planned).
   - *Confidence:* **High** on the pipeline; **Medium** until the QUL data terms are confirmed.
7. **Fallback = approach A2: page images generated by our own pipeline (HarfBuzz + FreeType → 4-bit/ALPHA_8 PNG or WebP at about 1260 px, plus a generated word-box DB). Offer them as an optional download only if field data shows font-rendering problems on some devices.** **Do not use third-party scans.**
   - *Rationale:* the licensing is clean, the coordinates are exact, and it reuses the pipeline.
   - *Confidence:* **High** (as a fallback).
8. **Madani mode: v1 uses one Unicode font (DK New Madina, OFL, or KFGQPC Hafs v2.2) with QUL's 604-page layout. Add the "High-fidelity Madani" QCF V2 font pack (~77–219 MB) later as an on-demand download, under Quran Foundation terms** (Developer Console account, attribution, no separate redistribution).
   - *Confidence:* **Medium-High.**
9. **Ornaments: commission original vector art** for the frame, header bar, surah cartouche and the margin "ع", manzil and quarter badges, all tinted by theme. **Hand-enter the 90 Indo-Pak para-quarter positions and the Hanafi sajda list from a print** if QUL lacks them.
   - *Confidence:* **High.**
10. **Reader UX on top of the renderer:**
    - follow-audio page flipping with a "browsing" override;
    - ayah long-press sheet;
    - word highlight for hifz where QUL word segments exist;
    - dual-page spreads at ≥ 600 dp;
    - "Large text" reflow mode instead of bitmap zoom;
    - keep-screen-on;
    - immersive mode with an auto-hiding translucent bar;
    - orientation lock hidden on large screens (Android 16 rule).

    *Confidence:* **High.**
11. **Avoid GPL code reuse unless the owner decides to open-source the app.** quran_android and quran.com-images are GPL-3: use them as references, don't copy them. The MIT code (quran.com frontend, QUL, DigitalKhatt) can be borrowed with attribution.
    - *Confidence:* **High.**
12. **Plan a proofreading and compliance step.** A qualified hafiz/qari reviews every page of every shipped layout from the pipeline's PDF and signs a certificate (mirroring Punjab Act 2011 s.5(3)). Ask a Pakistani lawyer whether app distribution needs registration or Quran Board involvement.
    - *Confidence:* **Medium**, because applying the acts to apps is unclear.

---

## 4. Risks & open questions

**Risks**
- **QUL data license is unstated, and downloads sit behind a login.** If Tarteel objects, the 16-line and 13-line layouts would need another source. The 15-line Hafizi layout is safe because the DK text in the MIT repo is already line-broken.
- **DK IndoPak is v0.1** ("Test layout" label on QUL layout 24). There may be glyph or mark-placement bugs in rare words. Mitigations: full-page proofreading, KFGQPC Nastaleeq as a selectable fallback, and an upstream bug-report channel.
- **Quran text correctness:** any error is serious, both for user trust and legally. A pipeline skeleton check against Tanzil plus human proofreading are mandatory before release.
- **Kashida via feature values on old Android:** the Minikin `main` behaviour is verified, but API 24–30 behaviour is not. The fallback is inter-word spacing (C1).
- **CFF2 fonts on API 24–25** [unverified]. Mitigated by converting to TrueType glyf (done and verified identical metrics).
- **Offline-versus-device width drift** (hinting, rounding, font-scale settings). Mitigations: an instrumentation test, `Paint.isSubpixelText = true`, and a runtime re-measure fallback per line when drift exceeds 0.5%.
- **Visual expectations:** users who know the reference app's *scanned* Taj pages may notice glyph differences (e.g. kashida shapes, ayah-circle style). We can offer the QuranWBW font if permission is granted, since it is the closest match.
- **Android 16 large-screen orientation rules** make "lock" impossible on tablets and foldables from targetSdk 36.
- **Ornament artwork** has to be original. Copying Taj or KFGQPC borders would reintroduce copyright risk.

**Open questions**
1. Does the reference app's printed numbering match QUL's 16-line Taj pagination? Report 01 has Juz 2 at "p.21"; QUL has p.20, so there may be a cover-page offset. Check against a physical Taj 16-line copy.
2. Will quranwbw.com grant written permission for the AlQuran IndoPak font, and on what terms (attribution, no modification)?
3. What does Tarteel/QUL say about bundling exported layouts and scripts, commercially and non-commercially?
4. Which Indo-Pak para-quarter (ربع/نصف/ثلاثة) positions do Taj prints use, and does QUL "Rub" (63) match them?
5. Does the owner want an image-exact "scan look" at all, given the licensing? If yes, Taj Company must license it.
6. Does an app distributed in Punjab or other provinces need publisher registration or a Quran Board certificate under the provincial Holy Quran (Printing & Recording) Acts?
7. Do we want tajweed colouring for Indo-Pak in v1? DK IndoPak has no tajweed feature. QUL has tajweed-tagged scripts for Madani only (layouts 19/21).

---

## 5. Sources

| URL | What I verified there |
|---|---|
| https://qul.tarteel.ai/mushaf_layouts | Full layout catalogue: ids, names, page counts and lines (548/610/847/849/731/487/1890/604) |
| https://qul.tarteel.ai/mushaf_layouts/7?page_number=… (pp. 1, 2, 3, 20, 21, 37, 38, 39, 40, 50, 56, 57, 528, 529, 530, 547, 548); /6 (pp. 21, 22, 23, 42, 582, 590); /23 (pp. 26–30, 816, 817); /15 (pp. 2, 3, 50, 547) | Ayah ranges per page, juz start pages, 16-line structure, empty ornamental lines on p.2, word spans with `data-word-id` / `data-location`, ayah-end "char-end" words |
| https://qul.tarteel.ai/resources/mushaf-layout , /11 , /12 | Resource list, descriptions ("Taj Company… 548 pages", "Qudratullah Company based in Lahore… 610 pages"), download buttons gated by `/users/sign_in` |
| https://qul.tarteel.ai/docs/mushaf-layout | `pages`/`words` schema, line types, `is_centered` semantics |
| https://qul.tarteel.ai/docs/faq , /docs/downloading-data , /docs/datasets , /credits , /docs/indopak | No data license stated, Indo-Pak resource list, credits (DigitalKhatt, Ayman Siddiqui, KFGQPC) |
| https://qul.tarteel.ai/resources/font (+ /242, /243, /245, /249, /238, /462, /568, /455–459) | Font list and CDN URLs (static-cdn.tarteel.ai/qul/fonts/…). Fonts downloaded and `name` tables read |
| https://qul.tarteel.ai/resources/quran-script , /59 , /52 , /565 ; /resources/quran-metadata | Script ids (Indo-Pak Nastaleeq, QPC Nastaleeq, DK Indo-Pak), metadata ids 63–70 |
| https://static-cdn.tarteel.ai/qul/fonts/quran_fonts/{v1-optimized,v2,v4-tajweed}/{ttf,woff2}/pN.* | Content-Length sampling for the QCF set size estimates |
| github.com/TarteelAI/quranic-universal-library (clone) | MIT license; `lib/exporter/export_mushaf_layout.rb` export format |
| github.com/quran/quran.com-frontend-next (clone) | MIT; `fonts.scss` (IndoPak = QuranWBW v4.2.1); `Line.tsx`, `VerseText.module.scss` (`justify-content: space-between`); `_utility.scss` line widths; `pageUtils.ts` centered lines |
| https://api-docs.quran.foundation/docs/tutorials/fonts/page-layout/ | Mushaf ids (6 = Indo-Pak 15-line, 610; 7 = Indo-Pak 16-line, 548), `text_indopak_nastaleeq`, big-text layout advice |
| https://api-docs.quran.foundation/legal/mushaf-fonts-and-images/ | Terms for bundling fonts and mushaf images (attribution, Developer Console, no separate redistribution, no prepackaged DB of content) |
| https://api.quran.com/api/v4/verses/by_page/50?… | Verse fields `ruku_number`, `manzil_number`, `juz_number`, etc. |
| github.com/quran/quran_android (clone) | GPL-3; `MadaniPageProvider`, `DefaultPageSizeCalculator`, ALPHA_8 decode, night-mode `ColorMatrix`, ayahinfo schema, line-by-line Compose renderer and SQLDelight schema |
| https://android.quran.com/data/zips/images_{800,1024,1260}.zip , /databases/ayahinfo/ayahinfo_1260.zip , /width_1260/page002.png | Sizes (48.2/63.4/81.1 MB), PNG format (1260×2038, 4-bit indexed), glyph table with 88,246 rows; redirect to files.quran.app |
| github.com/quran/quran.com-images (clone) | README: images and glyph bounds generated from KFGQPC fonts; "fonts and pages… belong to the King Fahd Quran Complex" |
| github.com/DigitalKhatt/mushaf-android (clone) | MIT; NDK + HarfBuzz fork (`hb_buffer_set_justify`, tatweel coords) + Skia |
| github.com/DigitalKhatt/digitalkhatt.org (clone) | MIT; Indo-Pak 15-line text (610 pages, Juz 2 p.22); `hb/indopak.otf` (OFL, cv01–cv18); JS justification via cvNN features |
| github.com/marwan/quranwbw (clone) | No LICENSE file in the QuranWBW repo |
| https://developer.android.com/about/versions/16/behavior-changes-16 | sw600dp orientation/resizability ignored; temporary opt-out property; edge-to-edge opt-out disabled; predictive back |
| https://developer.android.com/develop/ui/compose/layouts/pager | `beyondViewportPageCount`, `pageSize`, fling behaviour; HorizontalPager parameter types |
| https://developer.android.com/reference/android/graphics/text/TextRunShaper , /graphics/Canvas , /graphics/Paint , /graphics/Bitmap.Config | API levels: shapeTextRun 31, drawGlyphs 31, drawTextRun(CharSequence) 23, getRunAdvance 23, setFontFeatureSettings 21, setFontVariationSettings 26, HARDWARE 26, ALPHA_8 |
| https://android.googlesource.com/platform/frameworks/minikin/+/refs/heads/main/libs/minikin/FontFeatureUtils.cpp | Feature values passed to HarfBuzz; letter-spacing > 0.03 disables liga/clig |
| https://developer.android.com/guide/playcore/asset-delivery ; https://support.google.com/googleplay/android-developer/answer/9859372 | PAD delivery modes; size limits (500 MB base, 1.5 GB per pack, 30 GB on-demand total) |
| https://repo1.maven.org/…/io/github/oleksandrbalan/pagecurl , …/me/saket/telephoto/zoomable , dl.google.com …/media3-exoplayer | Versions: pagecurl 1.5.1 (Feb 2024), telephoto 0.19.0 / 1.0.0-alpha02, Media3 1.11.1; licenses read from repos (Apache-2.0) |
| https://archive.org/advancedsearch.php (query for Taj/13/15/16-line) | 111 hits; user-uploaded scans with absent or dubious license fields |
| https://www.wipo.int/wipolex/en/text/497207 | Punjab Holy Quran (Printing and Recording) Act 2011: definitions, s.5(3) certificate by a qualified person, s.9 penalties; no explicit electronic or app coverage |
| https://www.albalaghbooks.com/… (15-line Hafizi listings) | 15-line Hafizi editions where "every page ends with an ayah" |
| https://pakdata.com/?p=637 ; Adobe community threads | No license found for PDMS Saleem or Al Qalam fonts |
| https://quranfonts.com/font/digital-khatt-indopak/ | DK IndoPak described as "Not a King Fahd Complex font"; license not stated there (the font's own name table states OFL) |
| /home/user/criterion/docs/research/01-reference-app-teardown.md | The reference app uses a 16-line Taj scan with no ayah mapping, inverted-scan night mode, and bitmap zoom (user complaints) |

Prototype artifacts (scratch, not committed):
- `<scratch>/repos/r04-mushaf-rendering/proto/` — `taj16_p50_quranwbw.png`, `taj16_p50_kfgqpc.png`, `taj16_p50_dk.png`, `taj16_p50_dk_kashida.png`, `compare_spacing_vs_kashida.png`, `taj16_p50_dk_glyf_kashida.png`
- Scripts: `<scratch>/scripts/render_page.py`, `render_page_k.py`, `otf2ttf.py`, `fontinfo.py`
