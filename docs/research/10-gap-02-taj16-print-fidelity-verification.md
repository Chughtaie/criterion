# 10 · Gap-02 — Taj 16-line print fidelity check (page labels, Fatiha numbering, ruku, quarters, sajda, manzil, page furniture)

Researcher key: `gap-taj16-print-fidelity-verification` · Date: 2026-10-19 · Scope: check the schema and UI claims in reports 01, 04 and 05 against a real scan of the **Taj Company Indo-Pak 16-line mushaf**. All claims below come from looking at the scan directly.

> **Method.**
> - **Scan.** I found candidate scans with the archive.org advanced search and identified the edition from its title page and colophon. I downloaded **about 170 individual page images** (full-resolution BookReader JPEGs, 2333×3466 px at 450 dpi) into my scratch directory and inspected them with crops, rotations and native-resolution zooms.
> - **Use of the scan.** The scans are used **only to check facts**. Nothing from them is copied into the repo or the app, and none of the images leave the scratch directory.
> - **Reference datasets.** I compared every observation with:
>   - QUL layout 11: live HTML previews, plus Waqar144's MIT copy of the same 548-page line table;
>   - QF API v4 per-verse metadata (all 6,236 verses fetched: `ruku_number`, `rub_el_hizb_number`, `sajdah_number`);
>   - Tanzil `quran-data.js`;
>   - Waqar144 `rukus.dart` and `salasa_nisf_ruba_data.dart` (cloned @b6470fb, 2026-10-14);
>   - the DigitalKhatt Indo-Pak text.
> - **Prediction then check.** A small Python model predicts, for any ayah, the QUL page and line its end falls on. I then checked that exact page and line in the print. Scan identifiers and page numbers are given for every observation.
>
> Tags: **[verified: url]** means I fetched and inspected it in this session. **[measured]** means computed from verified data. **[inferred]** means reasoning from verified evidence. **[unverified]** means from memory.

---

## 1. Executive summary

1. **The scan is the right edition.** `AlQuranAlKareem16LinesTajCompany` on archive.org is a **Taj Company Ltd (Lahore · Karachi · Rawalpindi · Peshawar)** 16-line print.
   - Title-page notice: «جملہ قانونی حقوق عکس وطباعت بشمول کاپی رائٹس بحق تاج کمپنی لمیٹڈ محفوظ ہیں» (all reproduction and printing rights, including copyright, belong to Taj Company Ltd). It also carries a Government of Pakistan copyright registration «11818-Copr. مورخہ 04.10.02».
   - The scan has 559 leaves: leaf 0 is the cover, leaf 1 the title page, leaves 2–549 the Quran (= **548 Quran pages**), and leaves 550–558 the back matter.
   - Two other uploads are the same scan: `al-quran-16-lines-taj-company` (cited by report 04; pixel-identical) and `48AlQuranAlKareem16Lines-TajCompany…` (identical PDF byte size) [verified].
2. **Printed page number = QUL page index + 1 on every page.** Leaf *n* carries printed number *n*. The rule is confirmed by:
   - page headers on about 25 scanned pages;
   - **all 114 entries** of the printed surah index (فہرست, leaves 555–556);
   - **all 15 entries** of the printed sajda table (leaf 553).

   Fatiha is printed p.**2** (QUL 1), Baqarah 2:1 is p.**3** (QUL 2), and the Paras start at printed **21, 39, 57, 399, 509 and 529** (QUL 20, 38, 56, 398, 508, 528). Printed p.1 is the title page, which has no QUL equivalent. Labels therefore run **2–549**. This settles the "01 vs 05" question: report 05's "+1" is correct.
3. **Report 01's "Page #2 shows ۵" is a stale toolbar label, not a numbering scheme.** The Play screenshot shows printed page **۵** (2:18–2:25 = QUL index 4) under a toolbar reading "Page #2, Juz #1". The reference app's own Juz index lists Para starts at 2, 21, 39, 57, 75, 93, 111, 129 and 147, which are exactly the printed labels. So the app **does use printed page numbers**, and the toolbar text is the entry page [verified screenshots].
   - The page in that screenshot is a **clean digital re-typesetting** with Taj line breaks, not the scan [inferred].
4. **Fatiha in the Taj print has seven ayah-end circles with no digits in them.**
   - The basmala ends in a circle, so it counts as ayah 1 (Kufan count).
   - «اَنْعَمْتَ عَلَيْهِمْ» carries the small **«۵» sign with «لا» above it**, not a circle.
   - The print's own key (leaf 552) defines «۵» as «اس موقع پر غیر کوفیین کے نزدیک آیت ہے» (an ayah end here according to the non-Kufans).
   - The same «۵» appears at 2:10 «عَذَابٌ اَلِيْمٌ», 2:255 «اَلْحَيُّ الْقَيُّوْمُ», 3:92 «تُحِبُّوْنَ» and 6:73 «كُنْ فَيَكُوْنُ».

   **QUL's encoding matches the print** (basmala gets an ayah-end; U+06EC is used for «۵»). **DigitalKhatt's does not**: it leaves the basmala unnumbered, numbers «الحمد» as ١, and puts «٦» at عليهم. The colour eQuranOnline 548-page edition numbers Fatiha the same way as DK. Editions differ here, so **we must override DK's Fatiha digits**.
5. **The 558-ruku set (QF/QUL) is confirmed; Tanzil's 556 is wrong for this print.**
   - The ع marks sit at **26:51 and 26:68**, not 26:52/26:69. Their counts are 18 and 17 ayahs.
   - Separate rukus end at **28:82|28:83** and **31:30|31:31**.
   - The surah headers say Shu'ara «احد عشر ركوعا» (11), Qasas «تسع ركوعات» (9) and Luqman «اربع ركوعات» (4) [verified, scan pp.331, 348, 371, 357–358, 374].
6. **The three ruku numerals decode as (top) ruku-in-surah / (inside the ع) ayahs in the ruku / (bottom) ruku-in-Para.** This holds on **29 sampled marks, all of which match QF-558**. Two conventions are now verified on the print:
   - (a) A ruku that straddles a Para start is **numbered in the Para where it ends**, and its ayah count covers the whole ruku. Eight cases checked, e.g. 2:249–253 → ۳۳/۵/**۱**, and 15:1–15 → ۱/۱۵/**۱**.
   - (b) **Fatiha is excluded from Para 1's ruku numbering.** Fatiha's mark has only ۱/۷; Baqarah 2:1–7 is para-ruku ۱ and 2:8–20 is ۲.
7. **Report 05's "Rukus per Para" column is wrong for 18 of 30 Paras.** It counts by ruku *start* and includes Fatiha. The print counts by *end* and excludes Fatiha: Para 1 = **16**, Para 2 = **16** (print max at 2:248), Para 3 = **17** (print max at 3:91), and so on (corrected table in §2.3).
8. **Waqar144's MIT ruku data is almost exactly right.**
   - It has 558 entries keyed by each ruku's last ayah. Its (para, surah) numbering matches the print rule on all 557 non-Fatiha rukus except **one: it puts the first ruku of al-Waqi'a at 56:40 instead of 56:38**. The print has ع over ۳۸ and «۳۸» in the margin [verified, scan p.483].
9. **Para quarter marks (الربع / النصف / الثلثة) do NOT follow Madani rub-el-hizb.** Only 28 of 90 positions coincide with the end of Madani rub 2, 4 or 6 [measured]. Report 05's assumption that 23 Paras would match is false; for example Para 1 is 2:46/2:78/2:109 in the print versus 2:43/2:74/2:105 Madani.
   - Waqar's table has **90 entries, not 93** (3 of the 93 matches are glyph code).
   - I checked **37 of the 90** against the print. **34 match, 2 are off by one ayah** (Para 4 ثلثة: print 3:198, Waqar 3:199; Para 7 ربع: print 5:113, Waqar 5:114), and **1 sits mid-ayah**: Para 7 ثلثة is at the «۵» after «كُنْ فَيَكُوْنُ» inside 6:73.
10. **Sajda: 14 numbered + 22:77 as an extra.**
    - The print's table «سجدات التلاوة» numbers 14 sajdas and lists 22:77 as «۰۰ الحج (عند الشافعی)».
    - In the margin, 22:77 reads «السجدة عند الامام الشافعی رح» and the other 14 read «السجدة». 38:24 is a normal numbered sajda.
    - This **equals QF's 14 plus a Shafi'i-only flag on 22:77**.
11. **Page furniture rules:**
    - **Header.** Same on odd and even pages: Para opening words + Para number on the **right**, page number in the **centre**, surah name(s) + number(s) on the **left**.
    - **Footer.** «منزل N» on every page, giving the manzil of the page's last line.
    - **Margin notes alternate.** Ruku ع, quarter labels, sajda, «المنزل …», «الجزء …» and waqf notes sit in the **outer** margin: **right on even printed pages, left on odd**.
    - **16 ruled lines** per page.
    - **Printed pp.2–3 share a sar-lauh layout.** An ornamental panel holds the title, then a basmala box, then **5 text lines at the bottom**. QUL index 2 matches this; **QUL index 1 puts Fatiha at the top**, so it must be remapped.
12. **Surah headers follow QUL's line types exactly.**
    - `surah_name` followed directly by an ayah line = one three-part line (title · basmala · counts): 80 surahs.
    - `surah_name` + `basmallah` = a title line, then a basmala line: 31 surahs.
    - Surah 9 has a title only. Surahs 1–2 use the sar-lauh panel.
    - Header text: «سورة X مكية/مدنية وهي N آية و M ركوعات». Single-ruku surahs omit the ruku count (Fatiha «وهي سبع آيات», Falaq «وهي خمس آيات»).

---

## 2. Detailed findings

### 2.1 Which scan is which (title page / colophon)

| archive.org id | Leaves | Resolution | Identity (from title page / colophon) | Use |
|---|---|---|---|---|
| **`AlQuranAlKareem16LinesTajCompany`** (added 2016-01-20) | 559 | 2333×3466, 450 dpi | Leaf 1 is the title page. It has the «القرآن الكريم» medallion, «تاج کمپنی لمیٹڈ — لاہور، کراچی، راولپنڈی، پشاور», the all-rights notice, and the registration strip «حکومت پاکستان سینٹرل کاپی رائٹ آفس کراچی رجسٹریشن سرٹیفکیٹ نمبر 11818-Copr. مورخہ 04.10.02». The colophon on leaf 556 repeats «تاج کمپنی لمیٹڈ». Back matter: 551–552 «رموزِ اوقافِ قرآن مجید», 553 «سجدات التلاوة», 555–556 «قرآن مجید کی سورتوں کی فہرست». | **Primary source for this report** [verified] |
| `al-quran-16-lines-taj-company` (cited by report 04) | 559 | same | Pixel diff against the item above: mean \|Δ\| = 0.49–0.55 on leaves 2–4, i.e. **the same scan, re-encoded** | same [verified] |
| `48AlQuranAlKareem16Lines-TajCompany-Www.momeen.blogspot.com-` | — | — | PDF is 16,968,312 bytes, identical to the primary item's PDF | same [verified size; identity inferred] |
| `al-quran-16-lines-taj-company_202512` | 559 | — | creator "Taj Company"; same leaf count | probably same [inferred] |
| `quran-indopak` ("Complete 16-Line Mushaf…", 2026-09-10) | **548** | ~680–1200 px | Colour tajweed edition with "www.eQuranOnline.com" watermark. Same pagination: leaf 0 = p.۲ Fatiha, leaf 3 = p.۵. **Different Fatiha numbering** (الحمد = ۱ … «۶» under the «۵» at عليهم … ۷). No publisher stated. | secondary cross-check [verified] |
| `AlQuran16Lines`, `quran-tajwidi-16-line` (550), `16-line-quran-tajwid-colored` (555), `16-line-quran-with-large-font-size` (565) | various | — | Not needed once the primary scan was identified. Page counts differ, so they include extra front or back matter or are different editions. | not used |

**Edition year:** none is printed on the pages I sampled. The copyright registration is dated **04.10.02**, most likely 4 Oct 2002 in Pakistani dd.mm.yy [inferred].

### 2.2 Printed page labels vs QUL index (VERIFY 1)

| Checkpoint | QUL index (layout 11) | Printed number on scan | First line / evidence |
|---|---|---|---|
| Title page | — | (unnumbered, leaf 1) | title medallion |
| Al-Fatiha | 1 | **۲** | header «الفاتحة ۱ · ۲» |
| Baqarah 2:1 | 2 | **۳** | header «البقرة ۲ · ۳ · الٓمّٓ ۱» |
| Para 2, 2:142 | 20 | **۲۱** | header «سیقول ۲ · ۲۱ · البقرة ۲», L1 «سَيَقُوْلُ السُّفَهَآءُ…», margin «الجزء الثاني» |
| Para 3, 2:253 | 38 | **۳۹** | «تلك الرسل ۳ · ۳۹ · البقرة ۲», L1 «تِلْكَ الرُّسُلُ فَضَّلْنَا…» |
| Para 4, 3:92 | 56 | **۵۷** | «لن تنالوا ۴ · ۵۷ · آل عمرٰن ۳», L1 «لَنْ تَنَالُوا الْبِرَّ حَتّٰى تُنْفِقُوْا مِمَّا تُحِبُّوْنَ ۵ …» |
| Para 23, 36:22 | 398 | **۳۹۹** | «ومالی ۲۳ · ۳۹۹ · یٰسٓ ۳۶», L1 «وَمَالِيَ لَآ اَعْبُدُ … ۝۲۲», margin «الجزء الثالث والعشرون» |
| Para 29, 67:1 | 508 | **۵۰۹** | «تبٰرك الذی ۲۹ · ۵۰۹ · الملک ۶۷», L1 Mulk header, margin «الجزء التاسع والعشرون» (L3–4) |
| Para 30, 78:1 | 528 | **۵۲۹** | «عمّ ۳۰ · ۵۲۹ · النبا ۷۸», L1 Naba header |
| Last page | 548 | **۵۴۹** | «عم ۳۰ · ۵۴۹ · الفلق ۱۱۳ الناس ۱۱۴» |
| 114 surah starts (printed index, leaves 555–556) | layout-derived | **114/114 = index + 1** | e.g. Yunus 188, Shu'ara 331, Waqi'a 482, Mulk 509, Naba 529, Falaq 549 |
| 15 sajda pages (printed table, leaf 553) | 159…542 | **160, 227, 246, 265, 279, 302, 308, 329, 342, 376, 410, 433, 476, 536, 543 = index + 1** | — |

[verified: archive.org/download/AlQuranAlKareem16LinesTajCompany/page/n{k}.jpg; QUL layout 11 previews p.1, 2, 3, 38, 96, 254, 357, 373; Waqar `sixteen_line_indopak_layout.dart`]

**Rule:** `page_label = page_index + 1` for all 548 pages. There is no drift: the printed index's surah pages agree to the page across the whole book.

**Reference-app check (report 01 §2.5):**
- The Play screenshot (`play-lh…ua3Cjw…`, fetched at w2160) shows the toolbar "Page #2, Juz #1" over a page whose header reads «البقرة ۲ · ۵ · الٓمّٓ ۱». Its text runs from «صُمٌّۢ بُكْمٌ عُمْيٌ … ۝۱۸» to «كُلَّمَا رُزِقُوْا…», i.e. **printed p.5 = QUL index 4**, with «منزل ۱» at the foot.
- The iOS "Juz Index" screenshot lists Alif Lam Meem p.2, Sayaqool p.21, Tilkar Rusul p.39, Lan Tana Lo p.57, Wal Mohsanat p.75, La Yuhibbullah p.93, Wa Iza Sami p.111, Wa Lau Annana p.129, Qalal Malao p.147 [verified].
- **Conclusion:** the reference app's page numbers *are* the printed labels. "Page #2" in that screenshot is the entry page (Juz 1) left on the toolbar.
- Report 01's broader inference that "app page numbers ≠ printed numbers" should be withdrawn. The same screenshot also shows a **digitally re-typeset** page with Taj line breaks rather than a scan [inferred from glyph shapes].

Side note: **Waqar144's app shows QUL indices** to its users (`_para16LinePageOffsets` 0, 19, 37 … → p.1, 20, 38), so its page numbers read **one lower than the print** [verified code].

### 2.3 Ruku: positions, marginal numerals, Para convention (VERIFY 3)

**Disputed boundaries (Tanzil 556 vs QF/QUL 558):**

| Place | Tanzil | QF/QUL | Print (scan page, line, outer margin) | Verdict |
|---|---|---|---|---|
| Shu'ara ruku 3 end | 26:52 | 26:51 | p.333 L13 (left): ع **۳ / ۱۸ / ۷** (26:34–51 = 18 ayahs) | **QF** |
| Shu'ara ruku 4 end | 26:69 | 26:68 | p.334 L8 (right): ع **۴ / ۱۷ / ۸**; in-text ع over ۶۸ | **QF** |
| Qasas 28:82 / 28:83 | one ruku | split at 28:83 | p.357 L10: **۸ / ۷ / ۱۱** (28:76–82); p.358 L5: **۹ / ۶ / ۱۲** (28:83–88) + «الثلثة» + «وقف لازم» | **QF** |
| Luqman 31:30 / 31:31 | one ruku | split at 31:31 | p.374 L5: **۳ / ۱۱ / ۱۲**; L14: **۴ / ۴ / ۱۳** | **QF** |
| Surah-header counts | Qasas 8, Luqman 3 | 9, 4 | «…وتسع ركوعات» (p.348), «…واربع ركوعات» (p.371), Shu'ara «…واحد عشر ركوعا» (p.331) | **QF** |
| Waqi'a ruku 1 end | 56:38 | 56:38 | p.483 L2 (left): ع **۱ / ۳۸ / ۱۴**; in-text ع over ۳۸ | QF ✓; **Waqar 56:40 ✗** |

**Marginal triple decoded on 29 marks** (all = QF-558 with the rule below) [verified scan; measured comparison]:

| Ruku | Scan page/line/side | Printed (top / in ع / bottom) | QF-558 surah_ruku / ayahs / para_ruku |
|---|---|---|---|
| 1:1–7 (Fatiha) | p.2 L14 R | ۱ / ۷ / — | 1 / 7 / *excluded* |
| 2:1–7 | p.4 L4 R | ۱ / ۷ / ۱ | 1/7/1 |
| 2:8–20 | p.5 L6 L | ۲ / ۱۳ / ۲ | 2/13/2 |
| 2:40–46 | p.8 L5 R | ۵ / ۷ / ۵ (+«الربع») | 5/7/5 |
| 2:168–176 | p.25 L9 L | ۲۱ / ۹ / ۵ (+«الربع») | 21/9/5 |
| 2:229–231 | p.35 L2 L | ۲۹ / ۳ / ۱۳ | 29/3/13 |
| 2:243–248 | p.38 L5 R | ۳۲ / ۶ / ۱۶ | 32/6/16 |
| **2:249–253 (straddles Para 3)** | p.39 L6 L | ۳۳ / ۵ / **۱** | 33/5/1 |
| 2:267–273 | p.43 L5 L | ۳۷ / ۷ / ۵ | 37/7/5 |
| 3:42–54 | p.52 L10 R | ۵ / ۱۳ / ۱۳ | 5/13/13 |
| 3:81–91 (last of Para 3) | p.56 L16 R | ۹ / ۱۱ / **۱۷** | 9/11/17 |
| 3:92–101 | p.58 L3 R | ۱۰ / ۱۰ / ۱ | 10/10/1 |
| **4:23–25 (straddles Para 5)** | p.75 L12 L | ۴ / ۳ / **۱** | 4/3/1 |
| **5:78–86 (straddles Para 7)** | p.111 L7 L | ۱۱ / ۹ / **۱** | 11/9/1 |
| 7:189–206 | p.160 L9 R | ۲۴ / ۱۸ / ۱۴ (+«الثلثة», «السجدة») | 24/18/14 |
| **15:1–15 (straddles Para 14)** | p.237 L12 L | ۱ / ۱۵ / **۱** | 1/15/1 |
| 25:45–60 | p.329 L16 L | ۵ / ۱۶ / ۳ (+«السجدة») | 5/16/3 |
| 26:34–51, 26:52–68 | p.333, p.334 | see above | ✓ |
| **27:59–66 (straddles Para 20)** | p.345 L15 L | ۵ / ۸ / **۱** | 5/8/1 |
| 28:76–82, 28:83–88 | p.357, p.358 | see above | ✓ |
| 31:20–30, 31:31–34 | p.374 | see above | ✓ |
| **36:13–32 (straddles Para 23)** | p.399 L10 L | ۲ / ۲۰ / **۱** | 2/20/1 |
| 56:1–38 | p.483 L2 L | ۱ / ۳۸ / ۱۴ | 1/38/14 |
| 82:1–19, 88:1–26, 97:1–5 | p.534 L8, p.539 L6, p.544 L4 | ۱/۱۹/۷, ۱/۲۶/۱۳, ۱/۵/۲۲ | ✓ |

**Rules established:**
- **n1** = ruku number within the surah.
- **n2** = number of ayahs in the *whole* ruku.
- **n3** = ordinal of the ruku within the **Para in which the ruku ends**, with **Fatiha excluded** from Para 1's sequence.
- The mark sits in the outer margin beside the line holding the ruku's last ayah-end. A small ع is also printed above that ayah's circle.

**Corrected "rukus per Para" (by end, Fatiha excluded; total 557 + Fatiha = 558) vs report 05 §2.3.2 (by start, Fatiha included)** [measured]:

| Para | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 | 13 | 14 | 15 | 16 | 17 | 18 | 19 | 20 | 21 | 22 | 23 | 24 | 25 | 26 | 27 | 28 | 29 | 30 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| **Print rule** | **16** | **16** | **17** | **14** | 17 | 14 | **19** | **17** | 18 | 17 | 16 | 16 | 19 | **22** | **21** | **17** | 17 | 17 | **19** | **16** | **19** | 18 | **17** | **19** | **20** | **18** | **20** | 20 | 22 | 39 |
| Report 05 | 17 | 17 | 16 | 15 | 17 | 14 | 18 | 18 | 18 | 17 | 16 | 16 | 19 | 21 | 22 | 16 | 17 | 17 | 20 | 15 | 20 | 18 | 16 | 20 | 19 | 19 | 19 | 20 | 22 | 39 |

Bold = differs (18 Paras). The print directly confirms Para 2 = 16 (highest para-ruku printed in Para 2 is ۱۶ at 2:248) and Para 3 = 17 (۱۷ at 3:91).

**Waqar144 `rukus.dart`.** It holds 558 entries keyed by the 0-based global index of each ruku's **last** ayah, with 0-based `(paraRuku, surahRuku)`. It already uses the print convention (Fatiha and 2:7 both have paraRuku 0). **There is 1 error: 56:40 should be 56:38** [verified print p.483; measured diff].

### 2.4 Fatiha numbering and disputed ayah-ends (VERIFY 2)

| Place | Taj print (scan p.) | QUL Indo-Pak text (layout 11 HTML) | DigitalKhatt `quran_text_indopak_15.ts` | eQuranOnline 548p |
|---|---|---|---|---|
| Basmala of Fatiha | In a framed box, **ends in an empty circle ○** = ayah 1 (Kufan) (p.2) | word 1:1:5 = U+06DF + PUA ayah-end → basmala is 1:1 | ends «۝» **without a number** | no circle |
| 1:2–1:6 ends | **empty circles, no digits** (p.2, zoomed at native res) | U+06DF + PUA ends | ۝١ … ۝٥ (shifted: الحمد = ١) | ۱ … ۵ |
| «اَنْعَمْتَ عَلَيْهِمْ» | **«۵» with «لا» above**, no circle | `عَلَیْهِمْ ۙ۬` (U+06D9 + **U+06EC**), no ayah-end | `U+202E ٦ U+08E2 ۙ` (numbered disputed end **٦**) | «۵» + red «۶» + «لا» |
| End of 1:7 | empty circle with «ع» above | U+06DF + PUA | ۝٧ | ۷ |
| Header | «سورة الفاتحة مكية وهي سبع آيات» | — | — | — |
| 2:10 «عَذَابٌ اَلِيْمٌ» | «۵» + «لا», then «…يَكْذِبُوْنَ ۝۱۰» (p.4 L7) | `اَلِیْمٌ ۙ۬۟` (U+06EC) | `U+08E2 ۙ` (unnumbered) | — |
| 2:255 «اَلْحَيُّ الْقَيُّوْمُ» | «۵» with «ج» above (p.39 L9) | — | — | — |
| Others seen | 3:92 «تُحِبُّوْنَ ۵» (p.57 L1); 6:73 «كُنْ فَيَكُوْنُ ۵» (p.124 L9) | — | DK has **118** U+08E2 marks [measured] | — |

The print's legend (leaf 552) says: «۵ — یہ اس کی علامت ہے کہ اس موقع پر غیر کوفیین کے نزدیک آیت ہے۔ وقف کرے تو اعادہ کی ضرورت نہیں۔» (this sign marks a place that is an ayah end for the non-Kufans; if you stop there, you need not repeat) [verified]. It defines the circle as «جہاں بات پوری ہو جاتی ہے، وہاں چھوٹا سا دائرہ…اس کو آیت کہتے ہیں» (a small circle is written where the statement is complete; this is called an ayah).

**Conclusions:**
- Kufan keys (1:1 = basmala) are correct for data, audio and translation.
- In this Taj print Fatiha shows **no digits**.
- DK's Fatiha digits are a **display-only error relative to Taj**. They must be suppressed or rewritten.
- Any other Indo-Pak edition the owner owns may number Fatiha differently, as eQuranOnline does.

### 2.5 Para quarter marks (VERIFY 4)

**How they are printed:**
- The word «الربع», «النصف» or «الثلثة» appears, rotated, in the outer margin beside the line that holds the boundary.
- At some places the same word is also printed small above the ayah circle. I saw this at 15:93, 37:23, 37:144 and 38:53 [verified native-res crops].
- Waqar's convention is "the ayah at whose end the mark sits". The print agrees with it except where noted.

**Agreement statistics:**
- **Madani rub-el-hizb:** of Waqar's 90 positions, **28/90** equal the last ayah of Madani rub 2, 4 or 6 of the same Juz [measured; QF `rub_el_hizb_number`].
- **Ruku ends:** **43/90** coincide with a QF ruku end [measured].
- **Print sample:** 37 positions checked, 34 match Waqar exactly. Corrections:

| Para | ربع | نصف | ثلثة | Print check |
|---|---|---|---|---|
| 1 | 2:46 (p.8 L5) | 2:78 (p.12 L5) | 2:109 (p.16 L14) | ✓✓✓ (Madani would be 2:43 / 2:74 / 2:105) |
| 2 | 2:176 (p.25) | 2:201 (p.30 L1) | 2:231 (p.35) | ✓✓✓ |
| 3 | 2:273 (p.43) | 3:18 (p.48) | 3:54 (p.52) | ✓✓✓ |
| **4** (Indo-Pak start 3:92) | 3:125 (p.61 L3) | 3:164 (p.66 L1) | Waqar 3:199 → **print 3:198** | ✓✓✗: label beside p.70 **L7** («لِّلْاَبْرَارِ ۝۱۹۸ وَاِنَّ»); nothing at L11 (۱۹۹). eQuranOnline also has it at L7. |
| **7** (5:83) | Waqar 5:114 → **print 5:113** | 6:35 (p.120 L3) | 6:73 → **mid-ayah, after «كُنْ فَيَكُوْنُ ۵»** | ✗✓~: «الربع» beside p.115 **L9** (۱۱۳); «الثلثة» beside p.124 L9 (no ayah end on that line except the «۵»). Both also in eQuranOnline. |
| 9 | 7:137 | 7:163 | 7:206 (p.160) | –, –, ✓ |
| **11** (9:94) | 9:123 (p.187 L7) | 10:30 (p.192 L5) | 10:70 (p.196 L5) | ✓✓✓ |
| **14** (15:2) | 15:93 (p.241 L8, in-text «الربع» over ۹۳) | 16:44 (p.245 L16) | 16:86 (p.250 L10) | ✓✓✓ |
| **20** (27:60) | 28:13 (p.349) | 28:52 (p.354 L3) | 28:88 (p.358) | ✓✓✓ |
| **21** (29:45) | 30:27 (p.367 L16) | 31:14 (p.372 L10) | 32:28 (p.377 L4) | ✓✓✓ |
| **23** (36:22) | 37:23 (in-text) | 37:144 (in-text) | 38:53 (in-text) | ✓✓✓ |
| 26 | 46:35 | 48:17 | 49:10 | ✓✓✓ |
| 30 | 82:19 | 88:26 | 97:5 | ✓✓✓ |

Unverified so far: Paras 5, 6, 8, 10, 12, 13, 15–19, 22, 24, 25, 27–29, plus 2 marks of Para 9 (53 marks). The full 90-row seed, with the predicted printed page and line of each ayah-end, is in **Appendix A**. That page/line is where to look on the scan during the hand-check.

### 2.6 Sajda (VERIFY 5)

| # (print) | Para | Surah | Ruku | Trigger word / sajda point | Printed page | Ayah | Margin label |
|---|---|---|---|---|---|---|---|
| 1 | 9 | الاعراف | 24 | يسجدون / يسجدون | 160 | 7:206 | السجدة (+الثلثة) |
| 2 | 13 | الرعد | 2 | ولله يسجد / والآصال | 227 | 13:15 | السجدة (also in-text) |
| 3 | 14 | النحل | 6 | ولله يسجد / ما يؤمرون | 246 | 16:50 | السجدة |
| 4 | 15 | بنی اسرآءیل | 12 | يخرون للاذقان سجدا / خشوعا | 265 | 17:109 | السجدة |
| 5 | 16 | مريم | 4 | خروا سجدا / بكيا | 279 | 19:58 | السجدة |
| 6 | 17 | الحج | 2 | يسجد له / ما يشاء | 302 | 22:18 | السجدة |
| **۰۰** | 17 | الحج (عند الشافعی) | 10 | واسجدوا / تفلحون | 308 | **22:77** | **«السجدة عند الامام الشافعی رح»** |
| 7 | 19 | الفرقان | 5 | اسجدوا / نفورا | 329 | 25:60 | السجدة |
| 8 | 19 | النمل | 2 | الا يسجدوا لله / رب العرش العظيم | 342 | 27:26 | السجدة |
| 9 | 21 | السجدة | 2 | خروا سجدا / لا يستكبرون | 376 | 32:15 | السجدة |
| 10 | 23 | ص | 2 | وخر راكعا / اناب | 410 | **38:24** | السجدة (plain; no madhhab note) |
| 11 | 24 | حٰمٓ السجدة | 5 | واسجدوا لله / لا يسئمون | 433 | 41:38 | السجدة (also in-text over ۳۸) |
| 12 | 27 | النجم | 3 | فاسجدوا / واعبدوا | 476 | 53:62 | السجدة |
| 13 | 30 | الانشقاق | 1 | يسجدون / يسجدون | 536 | 84:21 | السجدة |
| 14 | 30 | العلق | 1 | واسجد / واقترب | 543 | 96:19 | السجدة |

[verified: leaf 553 table + margins on each page] The 14 numbered sajdas equal QF's `sajdah_number` list exactly [measured]. Tanzil's 15 adds 22:77.

### 2.7 Manzil (VERIFY 6)

| Manzil | Starts | Margin label (scan p., line, side) | Footer number change |
|---|---|---|---|
| 1 | 1:1 | — | p.2 «منزل ۱» |
| 2 | 5:1 | «المنزل الثاني» p.97 L2–3 (L) | p.96 «منزل» (۱) → p.97 «منزل ۲» |
| 3 | 10:1 | «المنزل الثالث» p.188 L3–4 (R) | p.187 «منزل ۲» → p.188 «منزل ۳» |
| 4 | 17:1 | «المنزل الرابع» p.255 L1 (L) | p.254 «منزل ۳» → p.255 «منزل ۴» |
| 5 | 26:1 | «المنزل الخامس» p.331 beside the header line (L) | p.331 «منزل ۵» |
| 6 | 37:1 | «المنزل السادس» p.402 L14–15 (R) | p.402 «منزل ۶» |
| 7 | 50:1 | «المنزل السابع» p.467 L6–7 (L) | p.467 «منزل ۷» (to p.549) |

**Rule:** `footer_manzil(page)` = the manzil of the page's last line. Where a manzil starts, there is also a margin label at the surah-header line. Report 05's manzil pages (QUL 1, 96, 187, 254, 330, 401, 466) are correct as indices, i.e. printed +1 [verified].

### 2.8 Page furniture (VERIFY 7)

| Element | Observation | Evidence |
|---|---|---|
| Header | **Above** the frame. Right slot = Para opening words + Para number (e.g. «سیقول ۲», «یعتذرون ۱۱», «سبحٰن الذی ۱۵», «امّن خلق ۲۰», «ومالی ۲۳», «قال فما خطبکم ۲۷», «قد سمع اللہ ۲۸», «تبٰرك الذی ۲۹», «عمّ ۳۰»). Centre = page number in Urdu-style Eastern digits. Left = surah name + number; **several surahs are listed** when a page has more than one («التكاثر ۱۰۲ العصر ۱۰۳ الهمزة ۱۰۴», «الفلق ۱۱۳ الناس ۱۱۴»). **Same layout on odd and even pages** (checked p.21, 39, 57, 188, 255, 358, 399, 482, 489, 496, 509, 529, 534, 546, 549). On p.2 the right slot reads «سورة», not a Para name. | scan [verified] |
| Footer | «منزل N» centred under the frame on every page | p.2, 3, 96, 97, 187, 188, 254, 255, 331, 402, 467, 549 |
| Body | **16 lines**, each between horizontal rules, inside a double-rule frame with a vine band | p.4, 8, 70, 115 … |
| Margins | **Outer margin only, alternating**: even printed page → right margin, odd → left. Holds ruku ع triples, الربع/النصف/الثلثة, السجدة, «المنزل …», «الجزء …» at Para starts, and waqf notes («وقف لازم», «وقف غفران», «وقف النبي صلی اللہ علیہ وسلم», «وقف منزل»). | 2, 4, 8, 38, 39, 56, 58, 75, 97, 111, 115, 188, 237, 333, 334, 345, 357, 358, 374, 399, 467, 483 |
| Printed pp.2–3 (QUL 1–2) | **Identical sar-lauh template.** An ornamental panel fills the top of the page with the title cartouche inside it (p.2 «سورة الفاتحة مكية وهي سبع آيات»; p.3 «سورة البقرة مدنية وهي مائتان وست وثمانون آية واربعون ركوعا»). A framed basmala box follows, then **5 text lines at the bottom**. QUL index 2 matches (L1–9 empty, L10 title, L11 basmala, L12–16 text). **QUL index 1 is top-aligned** (L1 title, L2 basmala, L3–7 text, L8–16 empty), unlike the print. | front sheet, p.2 crops; QUL p.1/p.2 HTML |
| Surah header | Three-part single line (title + makki/madani + counts · basmala · ayah/ruku counts in words) when QUL has `surah_name` followed directly by an ayah (80 surahs). Two lines (centred full title, then basmala) when QUL has `surah_name` + `basmallah` (31 surahs). Surah 9 has a title only. 1 ruku → no ruku count; 2 rukus → «وفيها ركوعان». | 112 header crops + QUL line types [measured] |
| Para start | Every Para starts at the top of a page (L1, or straight after a surah header) and carries a margin label «الجزء الثاني … الثلاثون» | p.21, 39, 57, 399, 509 |
| In-text ruku sign | Small «ع» above the circle of each ruku-ending ayah | p.2, 4, 8, 334, 483 … |

---

## 3. Recommendations

1. **Store `page_index` (QUL 1–548) and derive `page_label = page_index + 1` (2–549) for Taj 16.** Show and accept *labels* everywhere users see numbers: toolbar, Go-to-page, bookmarks, indexes and share text. Keep a per-mushaf `page_label_offset` (Taj 16 = +1). Also check the offset for every other layout before shipping it; 15-line and 13-line are unverified. *Rationale:* proven on 114 + 15 + ~25 independent print points, and the reference app uses labels too. **Confidence: High.**
2. **Fatiha display override.**
   - Taj-faithful default: seven empty circles (basmala included), «۵» + «لا» after «عليهم», «ع» over the last circle, header «سورة الفاتحة مكية وهي سبع آيات».
   - Keep data keys Kufan (1:1 = basmala).
   - If we offer "numbered Fatiha", number it Kufan (basmala = ۱ … ۷), never DK-style (الحمد = ۱).
   - Strip DK's `U+202E ٦ U+08E2` in 1:7 and its unnumbered «۝» basmala.

   *Rationale:* print evidence plus the print's own legend; a numbering mismatch confuses huffaz and breaks ayah-tap labels. **Confidence: High** for the data model; **Medium** for hiding digits, since some owners' copies number Fatiha.
3. **Render the «۵» disputed ayah-end sign (non-Kufan ayah end) from the text**, using DK U+08E2 (118 places) or QUL U+06EC. It is **not** an ayah boundary for audio, highlight or counting, but it is a valid quarter-mark anchor (6:73). **Confidence: High.**
4. **Ruku dataset = QF/QUL 558.** Store per ruku: `surah_ruku`, `first_ayah`, `last_ayah`, `ayah_count` (whole ruku), `para_id` = **Para containing `last_ayah`**, and `para_ruku` = ordinal within that Para with **Fatiha excluded (NULL)**. Draw the margin triple `surah_ruku / ayah_count / para_ruku` beside the line of `last_ayah`'s end, in the outer margin. Draw the in-text «ع» above that circle. Fix Waqar's 56:40 → 56:38 if you seed from Waqar. *Rationale:* 29/29 sampled marks match; the 4 Tanzil disputes go QF's way. **Confidence: High.**
5. **Replace report 05's "Rukus per Para" column** with the by-end, Fatiha-excluded counts in §2.3 (Para 1 = 16 … Para 30 = 39). The Para index screen should show these. **Confidence: High.**
6. **Para quarters: seed `para_quarter` from Waqar's 90 entries with three print corrections**:
   - Para 4 ثلثة = **3:198**;
   - Para 7 ربع = **5:113**;
   - Para 7 ثلثة = **6:73, word 10 («فَيَكُوْنُ»)**, i.e. mid-ayah. The word index is [inferred] from the standard word split; check it against QUL word keys.

   Add `word_pos` (nullable) and `verified_source` columns. **Hand-check the remaining 53 marks** against this scan (≈45 min with the crop scripts) before release. **Do not derive quarters from Madani rub** (only 28/90 coincide). Render them as an outer-margin label «الربع/النصف/الثلثة» beside the line, with an optional small in-text label above the circle. **Confidence: High** that the 3 corrections are right; **Medium** that the unverified 53 are all right (Waqar was 34/37).
7. **Sajda table.** Store `sajda(ayah_id, print_no 1–14 | NULL, kind ∈ {hanafi_and_shafii, shafii_only})`. Mark 22:77 as `shafii_only` with the label «السجدة عند الامام الشافعی رح»; the others get «السجدة». 38:24 is a normal numbered sajda in this print. **Confidence: High.**
8. **Manzil.** Footer «منزل N» = the manzil of the page's last ayah. Add a margin label «المنزل الثاني…السابع» on the start page, beside the header line of the surah that starts it. **Confidence: High.**
9. **Page templates.**
   - (a) `sarlauh` for indices 1–2: panel over slots 1–10, basmala box at slot 11, 5 text lines in slots 12–16. **Remap index 1** (QUL lines 1/2/3–7 → slots 10/11/12–16).
   - (b) `regular` for all others.
   - Header is non-mirrored: Para right · page centre · surah(s) left.
   - Margin marker side = `page_label % 2 == 0 ? RIGHT : LEFT`.
   - Header-line style follows QUL line types (`surah_name`→ayah = three-part line; `surah_name`+`basmallah` = two lines).

   **Confidence: High.**
10. **Surah header text is generated from data:** «سورة {name} {مكية|مدنية} وهي {ayah count in Arabic words} آية و{ruku count in words} ركوعات». Use «وفيها ركوعان» for 2 rukus and omit the ruku count for 1. Hand-curate the 114 strings and proof them against the scan. **Confidence: Medium** (the wording varies by count; I verified about 75 headers).
11. **Correct the earlier reports.** Report 01 §2.5 "app page numbers ≠ printed numbers" should be withdrawn. Report 05 §2.3.7's Madani-quarter hypothesis is falsified. Report 05 §2.3.4's "[unverified convention]" is now verified. **Confidence: High.**
12. **Legal hygiene.** Keep the scans out of the repo, assets and CI artefacts. The title page asserts Taj Company copyright over "عکس وطباعت" (reproduction and printing). Ask report 09's owner whether reproducing Taj **pagination and line breaks** (via QUL) carries any typographical-arrangement risk in PK, IN or UK. Likely low, since the line breaks are facts that QUL and other apps already publish, but it is unconfirmed [unverified]. **Confidence: Medium.**

### Recommended content-DB fields (delta to report 05 §2.7.3)

```sql
-- mushaf-level
ALTER TABLE mushaf ADD COLUMN page_label_offset INT NOT NULL DEFAULT 0;   -- taj16 = 1 (verified)
ALTER TABLE mushaf ADD COLUMN first_page_label INT;                        -- taj16 = 2 (p.1 = title page)

CREATE TABLE page (
  mushaf_id INT, page_index INT,                  -- QUL index 1..548
  page_label INT GENERATED ALWAYS AS (page_index + 1) VIRTUAL,  -- or materialised per mushaf
  template TEXT CHECK (template IN ('sarlauh','regular')),
  header_para_id INT, header_surah_ids TEXT,      -- e.g. '102,103,104'
  footer_manzil INT,                              -- manzil of last line
  outer_margin CHAR(1) CHECK (outer_margin IN ('R','L')),  -- R if label even
  PRIMARY KEY (mushaf_id, page_index));

CREATE TABLE ruku (
  id INT PRIMARY KEY,               -- 1..558 (QF/QUL)
  surah INT, surah_ruku INT,
  first_ayah INT, last_ayah INT, ayah_count INT,   -- whole ruku
  para_id INT,                      -- Indo-Pak Para containing last_ayah
  para_ruku INT NULL);              -- NULL for Fatiha; ordinal within para_id

CREATE TABLE para_quarter (
  para_id INT, quarter INT CHECK (quarter IN (1,2,3)),   -- ربع / نصف / ثلثة
  ayah_id INT, word_pos INT NULL,   -- mark after this word (NULL = after ayah end)
  verified_source TEXT,             -- e.g. 'taj16-scan p.70 L7'
  PRIMARY KEY (para_id, quarter));

CREATE TABLE sajda (ayah_id INT PRIMARY KEY, print_no INT NULL, kind TEXT CHECK (kind IN ('both','shafii_only')));

CREATE TABLE disputed_end (ayah_id INT, word_pos INT, PRIMARY KEY (ayah_id, word_pos));  -- «۵» signs (DK U+08E2 ×118)

CREATE TABLE ayah_display_override (mushaf_id INT, ayah_id INT, show_number INT /*0/1*/, PRIMARY KEY (mushaf_id, ayah_id));
-- taj16: rows for 1:1..1:7 with show_number = 0
```

---

## 4. Risks & open questions

- **Edition variance.** Taj, eQuranOnline and other 16-line prints share pagination but differ in Fatiha numbering (verified). They may also differ in quarter placement or waqf signs. The owner's own copy could be another printing. *Mitigation:* ask the owner to photograph 5 checkpoint pages (p.2, 39, 70, 115, 483) from his copy before freezing data.
- **53 unverified quarter marks.** Waqar was 34/37 against the print. Expect ~4 more errors among the 53. Budget the hand-check.
- **Margin-label reading.** Labels span about 1.2 lines. Where two ayahs end on the labelled line, I resolved it with in-text labels (37:23, 37:144, 38:53, 15:93), but not every place has one.
- **6:73 mid-ayah quarter.** Our schema needs `word_pos`, and audio and "jump to quarter" must start at word 11 of 6:73, or at the ayah start with a note. Are there other mid-ayah quarters? None in my sample of 37.
- **QF API page numbers for mushaf 7.** QF docs place 2:255 on page 39, which looks like printed labels, not QUL indices [inferred]. Never mix QF `page_number` (mushaf 7) with QUL indices without the offset.
- **Index-1 remap.** QUL's top-aligned Fatiha page differs from the print. Any word or line coordinate system (highlight boxes, precomputed justification) must use the remapped slots.
- **Header-text wording** for every surah (number words, «ركوعات» vs «ركوعا») needs a manual proof pass; I verified about 75 of 114.
- **Legal:** possible "typographical arrangement" claims by Taj Company over pagination and line breaks (see Rec. 12) [unverified].
- **Back matter not fully read:** leaves 550, 554, 557 and 558 (likely du'a/khatm and notices). Not needed for the reader.

---

## 5. Sources

| Source | What I verified there |
|---|---|
| https://archive.org/advancedsearch.php?q=(16+line)+AND+(quran+OR+qur'an)+AND+mediatype:texts&… | 82 candidate items; ids and image counts |
| https://archive.org/metadata/AlQuranAlKareem16LinesTajCompany (+ `…_scandata.xml`) | 559 leaves, 450 dpi, 2333×3466; files; uploader and date |
| https://archive.org/download/AlQuranAlKareem16LinesTajCompany/page/n{k}.jpg: 170 leaves, namely k = 0, 1 (reduced size) and full-res 2 3 4 5 8 12 16 21 25 30 35 38 39 43 46 48 52 56 57 58 61 66 70 75 96 97 111 115 116 120 124 137 160 169 183 187 188 192 196 200 213 225 227 231 236 237 241 245 246 250 254 255 265 276 279 282 291 300 302 308 309 316 325 329 330 331 333 334 335 340 342 345 348 349 354 357 358 363 365 367 371 372 374 376 377 386 392 397 399 401 402 403 407 409 410 412 413 421 430 433 435 441 447 449 453 457 461 463 464 466 467 469 472 474 476 479 482 483 485 489 492 496 498 500 501 503 505 507 509 511 513 515 517 519 521 522 524 525 527 529–558 | Title page and colophon, page labels, headers, footers, ruku triples, quarter labels, sajda labels and table, waqf legend, surah index, surah headers |
| https://archive.org/metadata/al-quran-16-lines-taj-company and …/page/n0–n4.jpg | Same scan (pixel diff) |
| https://archive.org/metadata/48AlQuranAlKareem16Lines-TajCompany-Www.momeen.blogspot.com- | Identical PDF byte size |
| https://archive.org/metadata/al-quran-16-lines-taj-company_202512 | creator "Taj Company", 559 leaves |
| https://archive.org/metadata/quran-indopak and …/page/n0–n4, n68, n113, n122.jpg | 548-page eQuranOnline colour edition; same labels; Fatiha numbered 1–7 with «۶» at عليهم; quarter labels at 3:198 / 5:113 / 6:73 lines |
| https://archive.org/metadata/{AlQuran16Lines, quran-tajwidi-16-line, 16-line-quran-tajwid-colored, 16-line-quran-with-large-font-size, quran-kareem-16-lines, 16-quran-majeed-16-line, four-color-tajwidi-quran-16-lines-like-taj-company-ltd} | Metadata only (leaf counts, descriptions) |
| https://qul.tarteel.ai/resources/mushaf-layout/11?page={1,2,3,38,96,254,357,373} | Line types and word locations; basmala as 1:1; U+06EC disputed mark at 1:7 and 2:10; header/basmala line structure |
| https://api.quran.com/api/v4/verses/by_chapter/{1…114}?per_page=50&words=false | `ruku_number` (558), `rub_el_hizb_number`, `sajdah_number` (14) for all 6,236 verses |
| https://tanzil.net/res/text/metadata/quran-data.js | 556 rukus, 15 sajdas, HizbQuarter, Manzil |
| `git clone --depth 1 https://github.com/Waqar144/quran_memorization_helper` @ b6470fb94e95 (2026-10-14), MIT | `rukus.dart` (558 entries; 56:40 error), `salasa_nisf_ruba_data.dart` (90 entries), `sixteen_line_indopak_layout.dart` (548 pages), `pages.dart`, `para_bounds.dart`, `widgets/read_quran.dart` (how marks render) |
| DigitalKhatt `ClientApp/src/app/services/quran_text_indopak_15.ts` (r04 clone @4213fab, MIT) | Fatiha encoding (`۝` basmala, ١–٥, `U+202E ٦ U+08E2`), 118 × U+08E2 |
| https://play.google.com/store/apps/details?id=free.alquran.holyquran&hl=en → screenshot `play-lh.googleusercontent.com/ua3Cjw_I0I60TAUFoE0s-…=w2160` | "Page #2, Juz #1" over printed p.۵ (2:18–25), «منزل ۱» footer, re-typeset page |
| iOS App Store screenshot (mzstatic `…5a6d27fd…_7.jpg`, 1242×2208) | Reference Juz index pages 2, 21, 39, 57, 75, 93, 111, 129, 147 |

---

## Appendix A — 90-row Para-quarter seed (Waqar144 positions, print-corrected where checked)

Legend:
- Each cell gives the ayah at whose end the mark sits, then the predicted printed page and line of that ayah-end (QUL layout 11 + 1).
- **ᴿ** = the position is also a QF ruku end.
- **✓** = checked on the Taj scan.
- **✗→** = print differs, corrected value shown.
- No mark = not yet checked.

Generated from Waqar `salasa_nisf_ruba_data.dart` @b6470fb, QF ruku data and Waqar's 16-line layout [measured]. The "print" column values come from the scan [verified].

| Para | الربع (¼) | النصف (½) | الثلثة (¾) |
|---|---|---|---|
| 1 | 2:46ᴿ (p8 L5) ✓ | 2:78 (p12 L5) ✓ | 2:109 (p16 L14) ✓ |
| 2 | 2:176ᴿ (p25 L9) ✓ | 2:201 (p30 L1) ✓ | 2:231ᴿ (p35 L2) ✓ |
| 3 | 2:273ᴿ (p43 L5) ✓ | 3:18 (p48 L5) ✓ | 3:54ᴿ (p52 L10) ✓ |
| 4 | 3:125 (p61 L3) ✓ | 3:164 (p66 L1) ✓ | 3:199 (p70 L11) **✗→ 3:198 (p70 L7)** |
| 5 | 4:56 (p79 L15) | 4:86 (p84 L3) | 4:113 (p88 L10) |
| 6 | 5:2 (p97 L11) | 5:27 (p102 L9) | 5:53 (p106 L16) |
| 7 | 5:114 (p115 L11) **✗→ 5:113 (p115 L9)** | 6:35 (p120 L3) ✓ | 6:73 (p124 L10) **✗→ 6:73 word 10 «فَيَكُوْنُ ۵» (p124 L9)** |
| 8 | 6:140ᴿ (p132 L16) | 6:165ᴿ (p136 L16) | 7:43 (p141 L13) |
| 9 | 7:137 (p151 L8) | 7:163 (p155 L13) | 7:206ᴿ (p160 L9) ✓ |
| 10 | 8:75ᴿ (p169 L11) | 9:33 (p174 L6) | 9:62 (p178 L8) |
| 11 | 9:123 (p187 L7) ✓ | 10:30ᴿ (p192 L5) ✓ | 10:70ᴿ (p196 L5) ✓ |
| 12 | 11:44 (p205 L7) | 11:83ᴿ (p209 L5) | 12:17 (p214 L10) |
| 13 | 12:93ᴿ (p223 L3) | 13:18ᴿ (p227 L16) | 14:9 (p232 L6) |
| 14 | 15:93 (p241 L8) ✓ | 16:44 (p245 L16) ✓ | 16:86 (p250 L10) ✓ |
| 15 | 17:48 (p259 L12) | 17:97 (p264 L7) | 18:28 (p268 L14) |
| 16 | 19:20 (p277 L4) | 19:98ᴿ (p282 L5) | 20:73 (p286 L5) |
| 17 | 21:50ᴿ (p295 L6) | 21:112ᴿ (p299 L16) | 22:38ᴿ (p304 L10) |
| 18 | 23:73 (p313 L10) | 24:20ᴿ (p318 L6) | 24:50ᴿ (p322 L10) |
| 19 | 25:77ᴿ (p331 L3) | 26:117 (p336 L4) | 27:6 (p340 L13) |
| 20 | 28:13ᴿ (p349 L7) ✓ | 28:52 (p354 L3) ✓ | 28:88ᴿ (p358 L5) ✓ |
| 21 | 30:27ᴿ (p367 L16) ✓ | 31:14 (p372 L10) ✓ | 32:28 (p377 L4) ✓ |
| 22 | 33:62 (p385 L9) | 34:30ᴿ (p389 L16) | 35:14ᴿ (p394 L8) |
| 23 | 37:23 (p403 L11) ✓ | 37:144 (p407 L14) ✓ | 38:53 (p412 L6) ✓ |
| 24 | 39:75ᴿ (p421 L12) | 40:41 (p425 L16) | 41:5 (p430 L10) |
| 25 | 42:29ᴿ (p439 L6) | 43:25ᴿ (p443 L7) | 44:22 (p448 L7) |
| 26 | 46:35ᴿ (p457 L6) ✓ | 48:17ᴿ (p463 L1) ✓ | 49:10ᴿ (p466 L1) ✓ |
| 27 | 53:30 (p475 L12) | 55:25ᴿ (p480 L2) | 56:74ᴿ (p484 L6) |
| 28 | 59:10ᴿ (p494 L7) | 60:13ᴿ (p498 L11) | 64:10ᴿ (p504 L8) |
| 29 | 68:52ᴿ (p513 L11) | 71:28ᴿ (p519 L4) | 74:56ᴿ (p524 L7) |
| 30 | 82:19ᴿ (p534 L8) ✓ | 88:26ᴿ (p539 L6) ✓ | 97:5ᴿ (p544 L4) ✓ |

For contrast, here are the Madani rub 2/4/6 ends for Paras where the Para and Juz starts are identical. They do **not** match the print:
- Para 1: 2:43 / 2:74 / 2:105
- Para 3: 2:271 / 3:14 / 3:51
- Para 30: 81:29 / 86:17 / 93:11

Overall, only 28 of 90 coincide [measured].
