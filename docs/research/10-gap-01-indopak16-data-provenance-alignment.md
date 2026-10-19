# 10 · Gap 01: Indo-Pak 16-line (Taj) data stack: provenance, licensing and alignment

Researcher key: `gap-indopak16-data-provenance-alignment` · Date: 2026-10-19

**Scope.** The default reading mode needs three things: the Taj Company 16-line page layout (548 pages), a word text for the DigitalKhatt (DK) IndoPak font, and a mapping from the mushaf's ayah numbers to the Kufan verse keys used by audio and translations. This report establishes where each candidate source came from, measures how well the sources align across the whole Quran, and recommends which source to use.

> **Method.** Everything below was measured in this session. I did not rely on any earlier report's numbers.
> - **Clones (own scratch dir).** I cloned `Waqar144/quran_memorization_helper` (full history, 707 commits, HEAD `b6470fb`) and `DigitalKhatt/digitalkhatt.org` (full history, HEAD `4213fab`).
> - **Other repos inspected.** `DigitalKhatt/visualmetafont`, `DigitalKhatt/mushaf-android`, `DigitalKhatt/madinafont`, `AlfaazPlus/QuranApp`, `TarteelAI/quranic-universal-library` and `marwan/indopak-quran-text`.
> - **QUL previews.** I fetched **all 548** QUL 16-line previews (`/mushaf_layouts/7?page_number=1…548`) at ≤1 request/s: 76 MB of HTML, no errors. I also fetched a 25-page sample of the 15-line previews (`/mushaf_layouts/6`).
> - **Alignment.** I tokenised the MIT DK 15-line text and aligned DK, QUL, Waqar144, QuranApp and Tanzil token by token and line by line, with a skeleton normaliser that treats QUL's Private-Use-Area glyphs as wildcards.
> - **Re-break experiment.** I re-broke the DK text into 16-line pages with HarfBuzz and the DK font, to measure how much of the layout a self-derived approach could reproduce automatically.
> - **Hygiene.** All Python ran with `python3 -I`. No third-party build scripts were run, and no third-party data was copied into the project.
>
> Scripts are in `<scratch>/scripts-gap16/`, data and results in `<scratch>/work/gap-indopak16/`. The main result is `align_result.json`.

---

## 1. Executive summary

1. **No openly licensed 16-line Indo-Pak layout exists anywhere I could find.** I searched the web, QUL, all reachable DigitalKhatt repos, Waqar144, QuranApp and Quran.com. All of them lead back to one dataset: QUL mushaf 7 / resource 11. QUL's copyright page for that resource still reads "We don't have copyright information for this resource" [verified]. The same message appears for resources 12, 59, 313 and 565 [verified].
2. **Waqar144's MIT layout is derived from QUL or Quran.com, so its MIT licence does not cover the data.**
   - The author's own commit says so: *"the text is taken from Quranic Universal Library and then adapted for 16 line page format"* (`4b0e4bf`, 2025-04-28) [verified: git].
   - His layout matches QUL on **547 of 548 pages** and **8,742 of 8,744 lines**. On all 8,598 ayah lines, `(ayahIdx, wordIdx)` equals exactly (Kufan ayah index, QUL word position − 1) [verified: measured].
   - It also shares a QUL-only word segmentation of 83,668 tokens (Quran.com uses 83,665). His August 2023 version already matched QUL on all 8,598 line-start ayahs. That predates DK, and his June 2023 commit credits the "quran.com api" [verified: git].
3. **QuranApp (GPL-3.0) is the same QUL data plus copyleft.** It matches QUL on 8,726 of 8,742 rows and 543 of 548 pages. It differs only on pp.138, 382 and 435 (one-word line-break shifts) and pp.529–530 (basmala rows dropped), and it uses Quran.com's 83,665 word positions [verified: sqlite].
4. **The "open" DK text is itself QUL data, relicensed MIT by a Tarteel-sponsored author.**
   - **Text.** DK's generator (`visualmetafont`, `LayoutWindow::changeText`) converts QUL's *Indopak Nastaleeq* encoding into the DK text. That encoding uses PUA ayah numbers U+F500+n and specials such as U+F61E, U+F658 and U+F63C, and those exact code points appear in the QUL previews [verified: code + data]. This is the QuranWBW / Ayman Siddiqui text, whose README says *"DO NOT SELL, MANIPULATE, DISTRIBUTE WITHOUT CREDITS OR TAMPER…"* [verified].
   - **Line breaks.** The DK 15-line breaks match QUL layout 6 on 369 of 370 sampled lines. The only difference is p.472, which QUL "fixed" on 30 Sep 2026 [verified].
   - **What this means.** We should still use DK's MIT text, but credit the upstream sources and get it confirmed in writing (§2.8).
5. **The alignment problem is solved: DK and QUL map one-to-one with a single merge rule.**
   - DK has **83,669 tokens** (77,433 words + 6,236 ayah ends) and the QUL 16-line layout has **83,668** (77,432 + 6,236).
   - The only structural difference is **72:16:1**: QUL's single position "وَّاَنْ لَّوِ" corresponds to two DK tokens.
   - Only **3 of 77,432 words** differ in skeleton:
     - 4:112:3: normaliser artefact, same word;
     - 18:19:34: QUL uses one PUA glyph for the word;
     - **59:13:1**: a real spelling difference, لَاَانْتُمْ in QUL vs لَاَنْتُمْ in DK, which needs a hafiz to decide.
6. **Line-level fit is 99.99%.** Of the 8,598 word-bearing lines (548 × 16 = 8,768 slots, of which 114 are surah headers, 32 basmala lines and 24 empty), **8,598 map to a contiguous DK token range and 8,597 also match their first and last words**. The one exception is the 59:13 line. Over all 8,768 slots, that is 98.05% if header, basmala and empty lines count as non-mappable.
7. **Displayed numbering differs from Kufan numbering only in al-Fātiḥa.**
   - The basmala (Kufan 1:1) carries a bare ۝. Displayed 1–5 are Kufan 1:2–1:6. Displayed "٦" is a *disputed end* in the middle of 1:7, after عَلَيْهِمْ (1:7:4). Displayed 7 is the end of 1:7.
   - The other **117 U+08E2 marks are unnumbered, mid-ayah, and never adjacent to an ayah end**. QUL marks the same 118 positions.
   - All other 113 surahs number 1..n exactly as Tanzil does [verified: all 6,236 ends].
8. **QUL's 16-line data has quirks we must handle.**
   - **p.529 lists the basmala before the Surah 79 header**, a data bug. Waqar corrected it; QuranApp dropped the row.
   - **80 surahs have no separate basmala line**, so the renderer must draw the basmala inside the header band, as Waqar's renderer does.
   - **Paras.** Paras 1–28 begin at page 18n−16 using the **Indo-Pak para boundaries** (5:83, 9:94, 15:2, 27:60, 36:22). Paras 29 and 30 start at pp.508 and 528 [verified].
9. **Self-derivation is feasible but labour-heavy.** I re-broke the DK text into 16-line pages using only print-check anchors (page start, header and basmala slots) and an optimal-fit line breaker on DK font widths.
   - Only **42.9% of non-first line starts** matched the Taj layout. **76.8%** were within ±1 word and **91.6%** within ±2.
   - A clean-room Taj-16 therefore needs about 4,500 manual corrections: roughly **40–60 person-hours plus a 15–20 hour hafiz QA**.
10. **Licence posture of QUL is permissive in spirit but unconfirmed for this resource.**
    - The QUL FAQ says *"Yes, you can use QUL data in commercial projects. However, please review the licensing terms for each resource"* [verified]. Resource 11 lists no terms.
    - The Tarteel Terms linked from QUL's footer say *"You shall not (a) copy the Services or any part…"* [verified]. That makes **scraping the previews the worst option**.
11. **Decision.**
    - **Primary:** the QUL resource 11 SQLite export, downloaded by the owner's free account, **shipped only after written permission** from Tarteel/QUL, combined with the DK MIT text (or QUL resource 565 once permitted).
    - **Fallback:** a clean-room Taj-16 built from a purchased print with our pre-break and correction tool.
    - **Never ship:** Waqar, QuranApp or preview-scraped data.
12. **Contacts (verified):**
    - Tarteel: `support@tarteel.ai` (tarteel.ai JSON-LD contactPoint);
    - QUL maintainer Naveed Ahmad: `naveed.ahmad@tarteel.ai` (QUL `SECURITY.md`; credited as building and maintaining QUL);
    - QUL's public channel: GitHub issues;
    - DigitalKhatt author Amine Anane: `ananeamine@gmail.com` (git author email on 65 digitalkhatt.org commits) and @DigitalKhatt.

    Drafts of the emails are in §2.9.

---

## 2. Detailed findings

### 2.1 Licence evidence, quoted

| Item | Exact text / fact | Source |
|---|---|---|
| QUL res. 11 (16-line Taj), 12 (15-line Qudratullah), 59 (Indo-Pak Nastaleeq WBW), 313 (13-line Taj), 565 (DK Indo-Pak WBW) | "We don't have copyright information for this resource." In the code, the page shows this whenever `resource_permission.copyright_notice` is blank | [verified: https://qul.tarteel.ai/resources/{11,12,59,313,565}/copyright; `app/views/resources/copyright.html.erb`] |
| QUL permission model | `permission_to_host`, `permission_to_share` ∈ {unknown, requested, granted, rejected}; default `"unknown"` | [verified: `app/models/resource_permission.rb`] |
| QUL FAQ, commercial use | "Can I use the data in a commercial project? Yes, you can use QUL data in commercial projects. However, please review the licensing terms for each resource. Some data may have restrictions or require attribution, while others are freely available for commercial use." | [verified: https://qul.tarteel.ai/faq] |
| QUL FAQ, copyright | "The resources available on QUL vary in their copyright status. Some are in the public domain, while others may be subject to specific licenses. We recommend reviewing the licensing information provided by each resource's author before use." | [verified: https://qul.tarteel.ai/faq] |
| QUL docs FAQ (repo) | "Check repository license terms and dataset-specific licensing details before production use." | [verified: `app/views/docs/markdown/faq.md`] |
| QUL credits | "The majority of the resources currently available in QUL were created or curated by the community, not Tarteel." It credits Dr. Amin Anane (DigitalKhatt fonts), Ayman Siddiqui ("Indopak and tajweed fonts and script"), and Naveed Ahmad ("building and maintaining QUL") | [verified: https://qul.tarteel.ai/credits] |
| QUL download gate | The "Download sqlite" and "Download docx" buttons open `/users/sign_in?modal=true&user_return_to=%2Fresources%2Fmushaf-layout%2F11`. The sign-up form has no data-terms checkbox, only "I agree to signup for the Tarteel Mailing list." | [verified: https://qul.tarteel.ai/resources/mushaf-layout/11; `app/views/devise/registrations/_form.html.erb`] |
| Tarteel Terms (linked in QUL footer), last updated April 2, 2021 | "Tarteel grants you a limited, non-exclusive, non-transferable, license to access and use the Services. You shall not (a) copy the Services or any part, feature, function or user interface thereof…" | [verified: https://tarteel.ai/terms (Next.js payload)] |
| Tarteel support article | "We have released this full collection through our own project QUL" (no licence terms) | [verified: https://support.tarteel.ai/en/articles/12414474-…] |
| QUL launch blog | "designed to be open-source and community-driven"; "Anyone can sign up to access these resources"; no licence named | [verified via WebFetch: https://tarteel.ai/blog/qul-launch/] |
| Waqar144 | LICENSE "MIT License · Copyright (c) 2023 Waqar Ahmed" | [verified: clone] |
| Waqar144 commit `4b0e4bf` (2025-04-28) | "The new font is Digital Khatt and the text is taken from Quranic Universal Library and then adapted for 16 line page format." | [verified: git] |
| Waqar144 commit `5613349` (2023-06-28) | "The app now has full quran text inside it taken from quran.com api" | [verified: git] |
| DigitalKhatt/digitalkhatt.org | `LICENSE` is MIT, "Copyright (c) 2024 DigitalKhatt". The repo started as AGPL-3.0 (`cae8e29`, 2020) and changed to MIT in `abbb69c` "Update LICENSE" (2024-01-31). `quran_text_indopak_15.ts` was added in `ad9f3d7` (2024-12-23) and has no per-file header. **49 older files still carry AGPL headers**, but none of them is the text file | [verified: git] |
| DK about page | "DigitalKhatt is proudly sponsored by Tarteel" | [verified: `about.component.html`] |
| DigitalKhatt/visualmetafont | AGPL-3.0. The text generator is `LayoutWindow::changeText("indopak")` (see §2.2.3) | [verified: clone @8e9e5b1] |
| QuranWBW Indo-Pak text (= QUL res. 59) | "Made by Ayman Siddiqui and R. Siddiqua for www.QuranWBW.com and www.Quran.com for Sadaqa-e-Jaria purposes only. *DO NOT SELL, MANIPULATE, DISTRIBUTE WITHOUT CREDITS OR TAMPER IN ANY FORM OR MANNER.*" Downloads point to qul.tarteel.ai/resources/quran-script/59 | [verified: `marwan/indopak-quran-text` README] |
| AlfaazPlus/QuranApp | GPL-3.0; README credits tarteel.ai | [verified: clone @c496013] |
| Quran Foundation content | QF terms forbid prepackaged content databases (given fact f) | [unverified this session; verified by report 04 at https://api-docs.quran.foundation/legal/mushaf-fonts-and-images/] |
| Public Quran.com API | `api.quran.com/api/v4/verses/by_page/50?mushaf=7&words=true&word_fields=line_number…` returned the *Madani* page 50 (3:1–3:9), so the mushaf parameter is not honoured for pagination. This is not a usable 16-line source | [verified: request] |

**Search coverage for an openly licensed 16-line layout (none found):**
- **Web searches:** "16 line" indopak layout json github; QUL licence; Tarteel blog; DigitalKhatt 16 lines.
- **DigitalKhatt repos.** I probed 30 repo names with `git ls-remote`; only `mushaf-android`, `madinafont`, `visualmetafont` and `harfbuzz` exist. None contains Indo-Pak 16-line data. `visualmetafont` only references `indopak_15_lines_layout`, and its docs say "the bundled IndoPak layout contains 610" pages.
- **QUL docs.** No licence statements in `app/views/docs/markdown/*.md`.
- **QUL changelog.** No 16-line entries.

### 2.2 Provenance chain (what came from where)

```
Taj Company 16-line print (Lahore) ──(digitised by QUL community; author unknown)──► QUL mushaf 7 / res. 11 (no licence)
      │                                                                        │
      │                          Quran.com API mushaf 7 (QF) ◄─────────────────┤ (same CMS)
      │                                   │                                    │
      │            Waqar144 Aug-2023 JSON (line starts = QUL on 8,598/8,598)    │
      │            Waqar144 2025 Dart layout (= QUL on 547/548 pp)  ◄───────────┤
      │            QuranApp indopak_16 (= QUL on 543/548 pp, GPL)   ◄───────────┘
QuranWBW/Ayman Siddiqui Indo-Pak text ──► QUL res. 59 ──(DK changeText transform)──► DK quran_text_indopak_15.ts (MIT)
Qudratullah 15-line print ──► QUL mushaf 6 / res. 12 ──(snapshot before 30 Sep 2026)──────────┘ (line breaks)
```

#### 2.2.1 Waqar144: full git history

| Commit | Date | Message (verbatim, abridged) | What it shows |
|---|---|---|---|
| `5613349` | 2023-06-28 | "The app now has full quran text inside it taken from quran.com api" | Text source = Quran.com API |
| `963c31b` | 2023-08-15 | "Add new 16 line reading mode" (adds `assets/16line/1..30.json` + `PDMS_SALEEM_QURANFONTQESHIP_0.ttf`) | Its 16-line line starts **already equal QUL on all 8,598 ayah lines / 548 pages** [measured] |
| `7b1fe31`…`f0bc6f6` | 2025-04-14…22 | "juz 29 fixes" … "Juz 1 fixes", "mad fix…", "Add missing pause mark" | Hand text fixes; line structure unchanged |
| `4b0e4bf` | 2025-04-28 | "The new font is Digital Khatt and the text is taken from Quranic Universal Library and then adapted for 16 line page format." | Explicit QUL provenance |
| `628c0b3` | 2025-05-05 | "Rework how text data is stored … store layout information separately" | Creates `sixteen_line_indopak_layout.dart`, `text_indopak.dart` |
| `4bb3315` | 2025-05-16 | "16 line page fixes" | Last change to the 16-line layout |
| `810cc3d` | 2025-03-26 | "Show ruku data on tapping a ruku" | `rukus.dart` created; no source named |
| `263aea3` | 2025-09-25 | "Add salasa ruba markers" | `salasa_nisf_ruba_data.dart` (90 positions); no source named |

`git log -p` on the layout, ruku, quarter and text files contains **no URL, comment or credit** naming a source. The README names none either. The only provenance statements are the two commit messages above [verified].

**Measured equivalence with QUL (all 548 pages):**
- **Line starts.** 8,742 of 8,744 lines and 547 of 548 pages are identical. The only differing page is **p.529**, where QUL lists the basmala before the header and Waqar has the correct order.
- **Indexing.** Waqar's `(ayahIdx, wordStartInAyahIdx)` equals (Kufan ayah index 0..6235, QUL position − 1) on **8,598 of 8,598 lines**.
- **Text.** Waqar's `text_indopak.dart` has 6,236 ayahs and **83,668 ZWNJ tokens**, the QUL-16 segmentation. It is byte-identical to the DK MIT text in 6,008 ayahs. In 227 ayahs it differs only in combining-mark order / CGJ, so the two are equal after NFD and CGJ removal. In 72:16 it keeps QUL's merged "وَّاَنْ لَّوِ" token.
- **Rukus.** `rukus.dart` has 558 entries and equals DK's 558 U+08D6 ruku ends except **56:40 (Waqar) vs 56:38 (DK and Tanzil)**. That is probably a Waqar error.
- **Quarters.** `salasa_nisf_ruba_data.dart` has 90 positions (30 paras × ruba/nisf/salasa). The file contains 91 `=> PortionKind` arms, including the `_ => none` default.

#### 2.2.2 QuranApp (GPL-3.0)

- `quranapp.db` `mushaf_map` for `indopak_16` has **8,742 rows**: 8,598 ayah, 114 surah_name and 30 basmallah. There are no empty rows.
- Compared with QUL: 8,726 rows identical and 543 of 548 pages identical.
  - pp.138 (7:29–30), 382 (33:50) and 435 (42:9) each have a one-word line-break shift.
  - pp.529 and 530 drop the basmallah rows.
- Its `ayah_words` for `dk_indopak` has 83,665 tokens on Quran.com positions: it merges "بَعْدَ مَا" at 2:181, 8:6 and 13:37 and "وَّاَنْ لَّوِ" at 72:16.
- Against the DK MIT text it has 4 text differences:
  - 3:78 contains a Uthmani word, ٱللَّهِۚ;
  - 27:21 لَاَاذْبَحَنَّهٗٓ vs DK لَاَاَذْبَحَنَّهٗٓ;
  - 59:13 لَاَانْتُمْ vs DK لَاَنْتُمْ;
  - 61:8 is missing the ؕ mark.
- Conclusion: it is an older QUL/DK snapshot [verified: sqlite].

#### 2.2.3 DigitalKhatt text: derived from QUL "Indopak Nastaleeq" (res. 59)

`visualmetafont/src/Layout/LayoutWindow.cpp` builds the `dk_indopak` column from a column called `indopak` (`SELECT word_number_all,indopak FROM words` → `changeText("indopak", …)`). The transform's own lookup table identifies its input:

| Transform in `changeText` | Seen in QUL 16-line preview text |
|---|---|
| `[-]` → "۝" + Arabic-Indic digits (ayah = code − 0xF4FF) | QUL end tokens carry U+F500+n, e.g. 72:16 end `۟ۙ` (= 16). 6,207 of 6,236 QUL ends carry such a number, and all equal the Kufan ayah except Fātiḥa 1:2–1:6 (displayed 1–5). The other 29 use the special glyphs below |
| `` → `۝` (bare) | QUL 1:1:5 = `۟` |
| `ۙ࣢ۦ` → `‮٦࣢ۙ` (after U+06EC→U+08E2) | QUL 1:7:4 = `عَلَیْهِمْ ۙ۬ۦ` (U+06D9 U+06EC U+06E6), which becomes DK `عَلَيْهِمْ‮٦࣢ۙ` |
| `` → وَلْيَتَؔلَطَّفْ; `` → ۝١٧١…; `` → ۝٣١…; ``, ``, ``, ``, ``, ``, ``, `` (sajda ends) | QUL 18:19:34 = ``; 3:171 end ``; 5:31 ``; 6:36 ``; 7:206 ``; 13:15 ``; 16:50 ``; 17:109 ``; 19:58 ``; 22:18 ``; 22:77 `` |
| `   ` → U+06E2 (meem iqlab) | QUL `مُحِیْطٌ`, `اَلِیْمٌ … ` |

There is an exact code-point match on every special listed, and the skeleton matches on 77,429 of 77,432 words. So **the DK Indo-Pak text is a programmatic transform of QUL's Indopak Nastaleeq (QuranWBW) script** [verified: code + data]. **The DK 15-line line breaks are QUL mushaf 6:**
- they equal QUL on 369 of 370 lines across 25 sampled pages (pp.3–610);
- the one difference is p.472 line 2, where QUL's changelog says "Page 472 first line missed the ayah marker… fixed" on 30 Sep 2026;
- they equal QuranApp's `indopak_15` on **9,131 of 9,131 lines across 610 pages** [verified].

### 2.3 Token counts per source

| Source | Ayahs | Word tokens | Ayah-end tokens | **Total** | Notes |
|---|---|---|---|---|---|
| **DK 15-line text (MIT)** | 6,236 | 77,433 | 6,236 (6,235 numbered + 1 bare) | **83,669** | Excludes 114 `سُورَةُ …` header lines and 112 non-Fātiḥa basmala lines (5 tokens each). 610 pages, 9,131 lines, 84 code points |
| **QUL 16-line preview** (mushaf 7) | 6,236 | 77,432 | 6,236 | **83,668** | 72:16:1 = "وَّاَنْ لَّوِ"; "بَعْدَ مَا" split at 2:181, 8:6, 13:37; locations sorted and unique; every ayah = positions 1..n with the end token at n |
| Waqar144 `text_indopak.dart` | 6,236 | 77,432 | 6,236 | **83,668** | Same segmentation as QUL-16 |
| QuranApp `ayah_words` dk_indopak | 6,236 | 77,429 | 6,236 | **83,665** | Quran.com positions |
| Quran.com / QF word positions | 6,236 | 77,429 | 6,236 | 83,665 | [verified via QuranApp `uthmani` rows = 83,665] |

**DK special marks:**
- U+06DD: 6,348 in the raw file (6,235 numbered + 113 bare). The bare ones are 1 in Fātiḥa's 1:1 and 112 on skipped basmala lines.
- U+08E2 (disputed end): 118. U+202E (RLO): 1, at 1:7:4.
- U+08D6 (ruku "ع"): **558, all on end tokens**. U+08DB (sajda): **15**, all on end tokens: 7:206, 13:15, 16:50, 17:109, 19:58, 22:18, 22:77, 25:60, 27:26, 32:15, 38:24, 41:38, 53:62, 84:21, 96:19.

**Rukus.** DK has 558 rukus and Tanzil has 556. The differences are:
- ruku ends at **26:51/26:68 in DK vs 26:52/26:69 in Tanzil**;
- **extra DK ruku ends at 28:82 and 31:30**.

These make the Indo-Pak margin "ع" markers derivable from the MIT text [verified].

### 2.4 Alignment results (DK ↔ QUL ↔ Waqar)

**Mapping rule (complete).** For every token, DK key (s, a, pos) maps to QUL key (s, a, pos). The **only exception is 72:16**: DK tokens 1–2 (وَّاَنْ + لَّوِ) map to QUL position 1, and DK position p (p ≥ 3) maps to QUL position p − 1. Waqar `(ayahIdx, wordIdx)` maps to QUL (keys[ayahIdx], wordIdx + 1).

**Skeleton comparison.** I stripped marks, folded ی/ى→ي, ک→ك, ۃ→ة and hamza seats, and treated QUL PUA characters as `.{0,4}` wildcards. Mismatches per surah:

| Surah | Count | Example (QUL key: DK vs QUL) | Nature |
|---|---|---|---|
| 4 | 1 | 4:112:3 `خَطِيْ࢜ـٔ͏َةً` vs `خَطِیْٓئَةً` | Same word; hamza-seat folding artefact |
| 18 | 1 | 18:19:34 `وَلْيَتَؔلَطَّفْ` vs `` | QUL uses a single PUA glyph (mid-Quran word); same word |
| 59 | 1 | 59:13:1 `لَاَنْتُمْ` vs `لَاَانْتُمْ` | **Real orthographic difference. Hafiz to decide against the Taj print** |
| 72 | (count) | DK 9 tokens vs QUL 8 | Merge rule above |
| all others | 0 | — | 77,429 of 77,432 words identical in skeleton |

**Line-level result (548 pages × 16 slots).**

| Slot kind (QUL) | Count | Result |
|---|---|---|
| `ayah` lines (have a word range) | 8,598 | **8,598 contiguous DK ranges (100%); 8,597 also match first and last words (99.99%)**. The failure is p.493 l.13, the 59:13 spelling |
| `surah_name` | 114 | Map 1:1 to DK `سُورَةُ` header lines |
| `basmallah` | 32 | 31 real + 1 misplaced (p.529); DK has 112 basmala lines, so **80 surahs carry the basmala inside the header line** |
| empty | 24 | p.1 l.8–16, p.2 l.1–9 (ornamental opening pages), p.548 l.11–16 |
| **Total** | **8,768** | 8,597 of 8,768 = 98.05% if non-word slots count as failures; structurally 100% consistent |

**Other layout facts (QUL-16) [verified]:**
- 108 centred ayah lines (Fātiḥa p.1 l.2–7 and surah-final short lines).
- **Para starts** are on page 18n−16, line 1 (or the header line), for n = 2–28. They use Indo-Pak boundaries: **Para 7 = 5:83, 11 = 9:94, 14 = 15:2, 20 = 27:60, 23 = 36:22**, versus Tanzil's 5:82, 9:93, 15:1, 27:56, 36:28. This matches report 05.
- **Para lengths:** para 1 = pp.1–19, para 28 = pp.488–507, para 29 = pp.508–527 (starts at line 3, the header of 67), para 30 = pp.528–548.
- **Manzil starts** (p,line): (1,2), (96,3), (187,4), (254,2), (330,5), (401,14), (466,7). These are the same 7 boundaries as Tanzil.

### 2.5 Self-derivation experiment (DK 15-line text re-broken to 16 lines)

**Inputs.** The experiment used only what a print check would supply: each page's first token and the header, basmala and centred slots. It used DK token widths from HarfBuzz with `indopak.otf` from digitalkhatt.org, natural gap 0.25 em. Script: `rebreak2.py`.

**Algorithm.** Dynamic-programming line breaking per segment, minimising Σ(width − target)². The surah-final centred line is free, and no line may start with an ayah marker.

| Metric | gap 0.25 em | gap 0.40 em |
|---|---|---|
| Line starts identical to Taj/QUL (all 8,598) | 47.2% | 46.7% |
| Non-first line starts identical (7,960) | **42.9%** | 42.4% |
| Within ±1 word | **76.8%** | 76.3% |
| Within ±2 words | **91.6%** | 91.4% |
| Pages fully identical | 3 / 548 | 3 / 548 |

**Reading.** Print line breaks follow calligrapher choices (kashida, stacking) that a width model cannot predict. So a Taj-faithful self-derived layout needs **every line checked against the print and about 4,500 corrected**, mostly by one word. A good correction UI makes this about **5 minutes per page, roughly 40–60 hours**, plus hafiz QA. The alternative is to accept a "Criterion 16-line" layout: Taj page and para anchors, but our own line breaks, at the cost of hifz visual memory. That is not recommended as the default.

### 2.6 Numbering: DK/QUL display vs Kufan keys (Tanzil, everyayah, QuranEnc)

**Al-Fātiḥa** (the only surah where display numbering ≠ Kufan) [verified: DK tokens, QUL PUA numbers]:

| Kufan key (audio `001AAA.mp3`, translations) | Text span | DK display | QUL-16 display (PUA) | Notes |
|---|---|---|---|---|
| 1:1 | بِسْمِ اللّٰهِ … الرَّحِيْمِ | bare ۝ (no number) | U+F61E (bare) | Basmala **is** verse 1:1 (Kufan) |
| 1:2 | اَلْحَمْدُ … الْعٰلَمِيْنَ | ١ | 1 (U+F500) | display = Kufan − 1 |
| 1:3 | الرَّحْمٰنِ الرَّحِيْمِ | ٢ | 2 | |
| 1:4 | مٰلِكِ يَوْمِ الدِّيْنِ | ٣ | 3 | |
| 1:5 | اِيَّاكَ … نَسْتَعِيْنُ | ٤ | 4 | |
| 1:6 | اِهْدِنَا … الْمُسْتَقِيْمَ | ٥ | 5 | |
| 1:7 (word 4 = عَلَيْهِمْ) | صِرَاطَ … عَلَيْهِمْ | **٦ as disputed end**: `U+202E ٦ U+08E2` attached to the word, not a U+06DD | U+06D9 U+06EC U+06E6 on the word | **Not a Kufan verse end.** Taps and highlights belong to 1:7 |
| 1:7 end | غَيْرِ … الضَّا࢜لِّيْنَ | ٧ | 7 | display = Kufan |

**All other surahs (2–114).** The 6,229 numbered ends run 1..n, equal to Tanzil's counts for every surah [verified]. **There are 117 more U+08E2 disputed-end marks. All sit on words, none is followed directly by an ayah end, and none is numbered.** QUL marks the same 118 positions (U+06EC/PUA) [verified]. Full list (surah: ayah:word, QUL keys):

> 1: 7:4; 2: 10:9, 114:21, 215:3, 219:17, 235:26, 255:7, 257:9, 282:115; 3: 4:6, 49:4, 92:7, 97:5; 4: 173:17; 5: 1:5, 15:16, 23:15; 6: 1:9, 73:10, 161:7; 7: 29:13, 38:34, 137:18; 8: 36:16, 42:21; 9: 3:14, 36:21, 39:5, 70:10; 10: 22:34, 57:11; 11: 82:11, 86:7; 13: 5:11, 16:23, 16:28, 17:27, 18:23; 14: 1:10, 5:11, 9:10, 42:7; 18: 22:22, 86:13; 19: 41:4, 75:9; 20: 39:19, 40:17, 40:24, 40:29, 47:9, 77:4, 86:6, 86:13, 88:11, 89:6, 123:11, 131:12; 22: 78:20; 23: 45:5; 26: 49:14; 27: 33:7, 44:17; 28: 23:10; 29: 29:5, 65:9; 30: 4:3, 55:5; 31: 32:9; 32: 10:9; 34: 15:10; 35: 7:5, 41:7, 43:21; 39: 3:25, 20:14; 40: 16:3, 18:8, 58:4, 72:2; 43: 52:8; 47: 4:20, 15:22; 53: 29:4; 55: 35:5; 56: 8:2, 9:2, 18:2, 27:2, 41:2, 47:2, 50:1, 89:2; 57: 27:11; 65: 2:24, 10:9, 12:18; 67: 9:5; 69: 25:5; 71: 23:9, 24:3, 25:5; 72: 22:7; 73: 15:4; 78: 40:4; 85: 11:11; 89: 15:8, 16:7, 23:3; 91: 14:2; 96: 15:4; 97: 3:2; 98: 5:8; 99: 6:4; 103: 3:7; 106: 4:4; 112: 3:2; 114: 4:3

**Proposed schema** (one row per visible marker; this is a property of the *script*, not the line layout):

```sql
CREATE TABLE ayah_marker(
  script_id      INTEGER NOT NULL,   -- 1 = DK IndoPak
  word_index     INTEGER NOT NULL,   -- our token id: the end token, or the word carrying U+08E2
  surah          INTEGER NOT NULL,
  verse_ayah     INTEGER NOT NULL,   -- Kufan ayah that CONTAINS the token → verse_key = surah:verse_ayah
  display_number INTEGER,            -- number printed in the circle; NULL = bare marker (1:1)
  is_kufan_end   INTEGER NOT NULL,   -- 1 ⇔ token terminates verse_key (exactly 6,236 rows)
  disputed_end   INTEGER NOT NULL,   -- 1 ⇔ non-Kufan end sign U+08E2 (exactly 118 rows)
  PRIMARY KEY(script_id, word_index)
) WITHOUT ROWID;
CREATE INDEX ayah_marker_display ON ayah_marker(script_id, surah, display_number);
-- CI invariants: 6,354 rows; is_kufan_end XOR disputed_end; display_number = verse_ayah for every
-- is_kufan_end row except (1,1)→NULL and (1,2..6)→1..5; the only disputed row with a number is 1:7:4→6.
```

**Usage rules.**
- **Keys.** Bookmarks, audio, translations, search and notes always key on `verse_key` (Kufan).
- **Labels.** The Indo-Pak reader shows `display_number`. The translation pane shows "1:7 (Indo-Pak ٦–٧)" style labels in Fātiḥa only.
- **Typed numbers.** "Go to Fātiḥa 6" resolves through `ayah_marker_display` to 1:7. The app explains this with a one-line hint and offers a "standard numbering" toggle.
- **Taps.** Tapping a disputed sign selects its containing verse.

**Basmala handling.**

| Case | In data | Render | Tap / audio |
|---|---|---|---|
| Fātiḥa | Real verse **1:1**: 5 tokens, the last a bare ۝; QUL-16 p.1 l.2 (centred) | Normal text line, no number | Selectable as 1:1; audio `001001`; displayed numbering starts at 1:2 |
| Surahs 2–114 except 9 | **Not a verse and not in the word table.** QUL-16: a separate `basmallah` line in 31 surahs (2, 21, 22, 28, 30, 31, 34, 36, 37, 43, 46, 49, 50, 51, 56, 58–64, 67, 70, 78, 80, 104, 110, 111, 113, 114) plus 79 (misordered on p.529); **embedded in the header line in the other 80**. DK 15-line: always its own line | Header band with basmala inside (80) or as its own centred line (32). Typeset with DK `basm`/text, not as a verse | Tap plays the basmala (report 06 `BasmalaStrategy`); optional pseudo-key `s:0` for audio highlighting |
| Surah 9 | None | Header only | — |
| 27:30 | Part of the verse text | Inline | Normal |

### 2.7 Decision table: candidate sources for the 16-line Taj layout

| Candidate | Licence evidence (quoted) | Completeness | Alignment to DK (measured) | Effort | Legal / quality risk |
|---|---|---|---|---|---|
| **A. QUL res. 11 SQLite export, shipped after written permission** | "We don't have copyright information for this resource."; FAQ "Yes, you can use QUL data in commercial projects. However, please review the licensing terms for each resource." | Full: `pages(page,line,line_type,is_centered,first_word_id,last_word_id,surah_number)` + `info`. The exporter drops empty lines and renumbers (`each_with_index`) [verified exporter] | Same data as the previews: **8,597/8,598 ayah lines (99.99%)**; one merge rule (72:16) | 2–3 dev-days after download (join via the WBW script's `word_key`) | **Low once granted**; Medium while pending. QUL cannot license Taj Company's own rights (if any) |
| B. Derived from QUL previews (scraped) | Same as A, **plus** Tarteel Terms: "You shall not (a) copy the Services or any part…" | Same content (line types, centred flags, surah numbers, keys). Already fetched: 548 pages, 76 MB | 99.99% (measured) | ≈1 day (parser exists) | **High**: ToS breach, bypasses the login gate, no permission record. Use only as a local QA oracle |
| C. Waqar144 Dart (MIT) | "MIT License · Copyright (c) 2023 Waqar Ahmed"; but "the text is taken from Quranic Universal Library…" | 548 pages, 8,744 line starts; header/basmala sentinels; **no empty lines, centred flags or line ends**; plus 558 rukus and 90 quarters | 8,742/8,744 = QUL (99.98%); 100% of ayah lines → DK | ≈1 dev-day | **High**: MIT does not cover upstream QUL/QF data, so it has the same permission gap as A while looking clean. Ruku 56:40 error |
| D. QuranApp `quranapp.db` (GPL-3.0) | GPL-3.0 LICENSE; credits tarteel.ai | 8,742 rows with start/end and centred flags; basmala rows missing on pp.529–530 | 8,726/8,742 = QUL (99.82%); 543/548 pages; needs remapping from 83,665 positions | 1–2 dev-days | **Very high**: copyleft on our app plus the same upstream gap. Older snapshot |
| **E. Self-derived clean-room Taj-16** (DK text re-broken from a purchased print) | Our own observation of a print we own. DK text MIT (upstream QuranWBW/QUL res. 59; see risks) | Full; we choose the schema | Automated pre-break: **42.9% exact, 76.8% ±1, 91.6% ±2** (non-first lines) | **40–60 person-hours + 15–20 h hafiz QA** + 1 week of tooling | **Low legal**; Medium quality (human error, needs double entry). Independent of Tarteel's answer |
| (F. QF Content API / Content Sync) | QF terms forbid prepackaged bundles (given) | Online only | n/a | n/a | Not usable offline |

**Recommended: A as primary and E as fallback.** B, C and D are for QA only and are never shipped.

### 2.8 Exact pipeline

**Primary (A).** Lives in `tools/mushaf-pipeline/`. Python runs with `python3 -I`. Inputs are pinned by SHA-256.

0. **Legal gate.**
   - Send the emails in §2.9 and open a public GitHub issue on `TarteelAI/quranic-universal-library`.
   - Record the reply, its scope (resources 11, 565, 12, 313; free and commercial; attribution text) and the date in `docs/legal/data-permissions.md`.
   - Release builds fail CI unless `permissions.yml: qul_res_11: granted`.
1. **Acquire** (owner's free QUL account).
   - Download resource 11 (sqlite) and resource **565** (DK Indo-Pak WBW sqlite), or resource 59 if 565 lacks `word_key`.
   - Store them outside the public repo (`~/criterion-data/qul/<date>/`) and record their SHA-256.
   - Keep QUL update emails on: the FAQ says "you will receive an email notification whenever that resource is updated".
2. **Pin the DK text and font.**
   - Clone `digitalkhatt.org` at `4213fab` and take `ClientApp/src/app/services/quran_text_indopak_15.ts` and `ClientApp/src/assets/fonts/hb/indopak.otf`, recording SHA-256s.
   - Also pin QUL's Tanzil metadata (`quran-data.js`, CC BY 3.0) for counts.
3. **Parse and tokenise DK** (`parse_dk.py`, `tok_dk.py`).
   - Split on U+0020. Skip lines starting `سُورَةُ ` (expect 114).
   - Skip 5-token lines `بِسْمِ … ۝` (bare U+06DD) except in surah 1 (expect 112 skipped).
   - **Assert** 83,669 tokens, 77,433 words, 6,236 ends (6,235 numbered + 1 bare), 118 U+08E2, 1 U+202E, 558 U+08D6, 15 U+08DB, and 0 standalone-mark tokens.
4. **Kufan keys** (`dk_numbering.py`). Walk surahs, counting ends.
   - **Assert** that displayed numbers per surah are 1..n and equal Tanzil, except surah 1 = [∅, 1, 2, 3, 4, 5, 7].
   - Emit the `ayah_marker` rows (§2.6).
5. **Join the layout.**
   - Map `pages.first_word_id/last_word_id` through the downloaded script's `word_index → word_key (s:a:w)`.
   - Map `word_key` to DK tokens with the §2.4 rule (identity, except 72:16).
   - **Assert:** 83,668 QUL positions, every one mapped; 548 pages; every ayah line maps to a contiguous DK range; first and last skeletons match (with PUA wildcards), with a frozen whitelist {4:112:3, 18:19:34, 59:13:1}.
   - Re-run `align.py` against the preview oracle once, then delete the scraped HTML.
6. **Normalise QUL quirks.**
   - Swap header and basmala on p.529 and report it upstream.
   - Mark the 80 header lines that embed a basmala (`line.type = SURAH_NAME_WITH_BASMALA`).
   - Restore the 16-slot grid, so the empty lines on pp.1–2 and p.548 come back as `EMPTY` (the exporter renumbers lines).
   - Strip or keep U+202E after a device rendering test (§4).
7. **Metadata.**
   - **Rukus** from U+08D6 (558), with para-ruku and surah-ruku numbers computed.
   - **Sajda** from U+08DB, with 22:77 flagged as Shafi'i.
   - **Paras** from the Indo-Pak table (start pages 1, 20, …, 488, 508, 528).
   - **Manzils** (7).
   - **Quarters** (ربع/نصف/ثلاثة): 90 positions entered from the print. Waqar's list is a cross-check only.
   - Tables as in report 04 §2.10: `word`, `line`, `ayah_marker`, `margin_marker`, `page_meta`, `ayah_page`.
8. **Justification precompute.** As in report 04 §2.10 step 4.
9. **Validation gates (CI).**
   - Counts above; 548 × 16 slots; para starts at 18n−16 for n = 2..28, plus 508 and 528.
   - Manzil start pages [1, 96, 187, 254, 330, 401, 466].
   - Every word_index exactly once and in order; 6,354 `ayah_marker` rows.
   - Zero `.notdef`.
   - Render every page to PDF for the **hafiz proofreading sign-off**; the 59:13 and 27:21 spellings are decided there.
10. **Package and attribute.** Build `mushaf.db`. The licences screen shows:
    - "16-line page layout: Quranic Universal Library (Tarteel)";
    - "Indo-Pak text: DigitalKhatt (MIT), based on the Indo-Pak Nastaleeq script by Ayman Siddiqui / QuranWBW via QUL";
    - "Font: DigitalKhatt IndoPak (SIL OFL 1.1)";
    - "Metadata: Tanzil (CC BY 3.0)".

**Fallback (E), clean-room.** Start it if there is no grant within 30 days or a refusal.

1. Buy two copies of a Taj Company 16-line, 548-page print and record the edition and printing. Internal photos are for QA only and are never shipped.
2. **Clean-room rule:** the derivation operators do not open QUL, Waqar or QuranApp 16-line data.
3. **Anchor capture tool.** For each page, the operator clicks the first DK token and marks header, basmala and empty slots. Expect about 548 × 40 s ≈ 6 h.
4. **Pre-break** with `rebreak2.py` logic.
5. **Correction UI:** the photo beside the pre-broken page, with ← → keys to move each line start. Expect about 4,500 edits, 40–60 h.
6. **Double entry** on a 10% page sample. If disagreement exceeds 1%, re-review the whole para.
7. Run steps 3–10 above unchanged.

### 2.9 Permission-request emails (drafts)

**To:** support@tarteel.ai · **Cc:** naveed.ahmad@tarteel.ai · also posted as a GitHub issue on TarteelAI/quranic-universal-library
**Subject:** Permission to bundle QUL Indo-Pak 16-line layout (resource 11) and DK Indo-Pak script in an Android Quran app

> Assalamu alaikum,
>
> I am building "Criterion", an Android Quran reader for daily use in Pakistan. It renders the Indo-Pak mushaf page by page with the DigitalKhatt IndoPak font (OFL). We would like to bundle the following QUL data **offline inside the app** (a SQLite file in the APK). We would not redistribute it as a standalone dataset:
> - Mushaf layout **11** (Indopak 16 lines, Taj Company, 548 pp). Optionally also **313** (13-line Taj) and **12** (15-line Qudratullah).
> - Quran script **565** (Digital Khatt Indo-Pak word-by-word), or permission to rely on the MIT-licensed DK text in github.com/DigitalKhatt/digitalkhatt.org, which appears to be derived from resource 59.
>
> The resource pages say "We don't have copyright information for this resource", so we would like written confirmation before release:
> 1. May we ship these in a free app that may later carry optional donations or in-app purchases?
> 2. What attribution text do you want?
> 3. Is there an upstream rights holder we should also contact (e.g. whoever digitised the Taj layout, or Ayman Siddiqui for the script)?
>
> We will credit QUL and DigitalKhatt in the app and on the store page. As thanks, here are two data issues we found in layout 11:
> - On **p.529 the basmala line comes before the Surah 79 header**.
> - At **59:13:1** the Indo-Pak script has لَاَانْتُمْ, while DK has لَاَنْتُمْ.
>
> JazakAllahu khairan, [Name], [email], [Play developer name]

**To:** ananeamine@gmail.com (also @DigitalKhatt / GitHub @aminanan)
**Subject:** DigitalKhatt IndoPak text (MIT): confirming scope, and a 16-line question

> Assalamu alaikum Dr. Anane,
>
> Thank you for DigitalKhatt. We plan to ship the IndoPak font (OFL, converted to TrueType glyf and renamed, as OFL allows) with words from `quran_text_indopak_15.ts` (MIT) in an Android Quran app. Three quick questions:
> 1. Does the MIT licence cover the text **and** its 15-line page breaks? Was the relicensing of the underlying QUL data (Indopak Nastaleeq script / Qudratullah 15-line layout) agreed with Tarteel/QUL?
> 2. Do you have, or plan, a **16-line Taj Company** IndoPak layout under the same terms?
> 3. The 1:7:4 token uses U+202E + ٦ + U+08E2. Is the RLO required for correct shaping, or can a renderer drop it?
>
> We will credit DigitalKhatt prominently. JazakAllahu khairan, [Name]

*(Optional third email to QuranWBW / Ayman Siddiqui, via quranwbw.com or the `marwan/quranwbw` GitHub, address [unverified]: ask for consent and the attribution wording for the DK-transformed Indo-Pak script.)*

---

## 3. Recommendations

1. **Make the QUL resource 11 export the primary 16-line source, and ship it only after written permission from Tarteel/QUL. Send the §2.9 emails and open the GitHub issue this week.**
   - *Rationale:* it is the only complete, maintained, print-faithful dataset, and it aligns to the DK text at 99.99% with one merge rule. QUL's FAQ already allows commercial use in general. DK, a Tarteel-sponsored project, has already relicensed QUL-derived Indo-Pak data as MIT, so a grant is likely.
   - *Confidence:* **Medium-High** that permission is granted; **High** on technical fitness.
2. **Do not ship Waqar144 (MIT), QuranApp (GPL) or preview-scraped data.**
   - Waqar's MIT licence cannot cover data that his own commit says came from QUL, and the files match QUL on 547 of 548 pages.
   - QuranApp adds GPL on top of the same gap.
   - Scraping conflicts with Tarteel's "shall not copy the Services or any part" clause.
   - Keep the scraped JSON only as a local QA oracle until A arrives, then delete it.
   - *Confidence:* **High**.
3. **Adopt the DK token stream (83,669 tokens) as our canonical word table, keyed to Kufan verse keys, and map to QUL with the 72:16 rule.** Whitelist the three known skeleton differences, and send 59:13 and 27:21 to the hafiz review.
   - *Rationale:* it is proven on every token and every line, and it keeps us independent of which QUL script ID family the export uses.
   - *Confidence:* **High**.
4. **Implement the `ayah_marker` table (§2.6), Kufan-keyed everywhere, with Indo-Pak display labels. Treat al-Fātiḥa as the only display/Kufan divergence and treat the 118 U+08E2 marks as non-boundaries.**
   - *Rationale:* audio (everyayah `SSSAAA`), translations (QuranEnc) and Tanzil metadata stay exact, and the reader still shows what an Indo-Pak reader expects.
   - *Confidence:* **High**.
5. **Basmala rendering: support a "header with embedded basmala" line type (80 surahs) and a separate basmala line (32). Fix p.529. Keep Fātiḥa's 1:1 as a real, tappable verse.**
   - *Confidence:* **High** on the data. **Medium** that the Taj print really embeds the basmala in the band; check one physical copy.
6. **Derive the margin markers from the MIT DK text:** rukus from U+08D6 (558, Indo-Pak), sajda from U+08DB (15). Use the Indo-Pak para table (5:83 / 9:94 / 15:2 / 27:60 / 36:22) for "Play Para N" and the index. Enter the 90 quarter markers from a print and use Waqar's list only as a cross-check.
   - *Confidence:* **High** for ruku, sajda and paras; **Medium** for quarters.
7. **Set a 30-day decision gate.** If Tarteel refuses or has not answered after one follow-up (GitHub issue plus the QUL Discord), start fallback E: clean-room Taj-16 from a purchased print with the pre-break and correction tool. Budget about 2 calendar weeks: 40–60 person-hours plus 15–20 hours of hafiz QA. Until then, development builds may use the owner's QUL download privately.
   - *Confidence:* **Medium** (effort is extrapolated from the experiment).
8. **Get the text chain confirmed too.**
   - Ask the DK author to confirm that MIT covers the text and the 15-line breaks.
   - Credit Ayman Siddiqui / QuranWBW, QUL and DigitalKhatt in-app.
   - If Tarteel objects to the DK-derived text, the same pipeline can ingest QUL resource 565 or 59 under their permission.
   - *Confidence:* **Medium-High**.
9. **Pin and watch upstream versions.** QUL edits layouts:
   - the 15-line p.472 and p.443 fixes on 30 Sep 2026;
   - QuranApp's snapshot differs from today's QUL on 3 pages.

   Store SHA-256s and diff every new export with `align.py` before adopting it.
   - *Confidence:* **High**.
10. **Ask a Pakistani IP lawyer one narrow question:** do line breaks and pagination copied from a Taj Company print, re-typeset in a different font, infringe any right? Permission from Tarteel does not cover Taj's own rights, and neither does self-derivation.
    - *Confidence:* **Medium** that the risk is low. This is not a legal opinion.

---

## 4. Risks & open questions

**Risks**
- **Permission is refused or never comes.** Mitigation: fallback E (2–3 weeks), and ship the 15-line DK layout (MIT) first if needed.
- **The upstream text notice.** The QuranWBW text says "DO NOT SELL, MANIPULATE, DISTRIBUTE WITHOUT CREDITS OR TAMPER…". DK's transform may count as "manipulation". Mitigation: credits, plus the consent emails to DK, QUL and QuranWBW. The Quran's words themselves are not copyrightable, but the digital encoding may be claimed.
- **Taj Company layout rights** are unknown. They affect A and E equally; B, C and D are worse.
- **Data defects inherited from QUL:** p.529 order, the 59:13 spelling, and possible silent future edits. Mitigation: CI diffing and hafiz sign-off.
- **The U+202E token at 1:7:4** may shape differently under Android's Minikin per-word `drawTextRun` [unverified]. Test on API 24/30/35. If needed, strip the RLO and verify that the DK font still forms "٦ + U+08E2".
- **80 embedded-basmala headers** need header artwork that holds both the name and the basmala within one line height.
- **Self-derived quality.** Human line-entry errors. Mitigation: double entry and render diffs.

**Open questions**
1. Will Tarteel/QUL grant resources 11 (and 313, 12, 565) for free and commercial bundling, and with what attribution?
2. Which script's `word_index` family does the resource 11 export reference? Does it contain 83,668 positions, with "بَعْدَ مَا" split at 2:181, 8:6 and 13:37? [unverified: needs the login download]
3. Does the physical Taj 16-line print embed the basmala in the header band for those 80 surahs? Which spelling does it use at 59:13 and 27:21?
4. Does the Taj printing replicated by QUL number its pages the same way? Report 01 saw "p.21" for Juz 2 in the reference app, while QUL has p.20.
5. Did Tarteel authorise DK's MIT relicensing of QUL resource 59 and layout 6 data?
6. Who digitised QUL layout 11 (the "resource author" the FAQ refers to)?

---

## 5. Sources

| URL / artefact | What I verified there |
|---|---|
| https://qul.tarteel.ai/mushaf_layouts/7?page_number=1…548 (all 548) | Per-word `data-location`, `data-word-id`, `data-id`, line classes (`line--surah-name`, `line--bismillah`, `line--center`); 8,768 slots; 83,668 tokens; PUA ayah numbers; p.529 order bug; para and manzil start pages |
| https://qul.tarteel.ai/mushaf_layouts/6?page_number=(25 pages, 3…610) | DK 15-line = QUL 15-line on 369/370 lines; p.472 differs |
| https://qul.tarteel.ai/resources/{11,12,59,313,565}/copyright | "We don't have copyright information for this resource." |
| https://qul.tarteel.ai/resources/mushaf-layout/11 | "Indopak 16 lines layout published by Taj Company. This layout has 548 pages."; download buttons → `/users/sign_in` |
| https://qul.tarteel.ai/faq | Commercial-use and copyright Q&A (quoted) |
| https://qul.tarteel.ai/credits | Community-curated statement; credits for Anane, Siddiqui, Naveed Ahmad |
| https://qul.tarteel.ai/change_logs | "Fixed page 443 and 472 … September 30, 2026 … Indopak 15 lines layout - Qudratullah" |
| https://qul.tarteel.ai/docs | Docs index (no licence statements) |
| https://tarteel.ai/terms | Terms text, "Last updated: April 2, 2021", licence clause (a) quoted |
| https://tarteel.ai/ | JSON-LD `contactPoint.email = support@tarteel.ai` |
| https://tarteel.ai/blog/qul-launch/ (WebFetch) | "open-source and community-driven"; no licence named |
| https://support.tarteel.ai/en/articles/12414474-where-can-i-find-the-quranic-resources-that-you-use | "We have released this full collection through our own project QUL" |
| git clone github.com/TarteelAI/quranic-universal-library | `SECURITY.md` (naveed.ahmad@tarteel.ai); `lib/exporter/export_mushaf_layout.rb` (`word_index` ranges, line renumbering); `resource_permission.rb`; `copyright.html.erb`; docs `faq.md`; registration form |
| git clone github.com/Waqar144/quran_memorization_helper (full history @b6470fb) | LICENSE MIT; commits `5613349`, `963c31b`, `4b0e4bf`, `628c0b3`, `4bb3315`, `810cc3d`, `263aea3`; data files parsed (548 pp / 8,744 lines; 6,236 ayahs / 83,668 tokens; 558 rukus; 90 quarters); `read_quran.dart` embedded-basmala rendering |
| git clone github.com/DigitalKhatt/digitalkhatt.org (full history @4213fab) | MIT LICENSE (`abbb69c`, 2024-01-31, from AGPL); text file `ad9f3d7`; 49 legacy AGPL headers; `indopak.otf`; about page "sponsored by Tarteel"; author email |
| git clone github.com/DigitalKhatt/visualmetafont (@8e9e5b1, AGPL-3.0) | `LayoutWindow::createDataBase`/`changeText` transform table (QUL PUA → DK text); only a 15-line IndoPak layout |
| git clone github.com/DigitalKhatt/{mushaf-android, madinafont}; `git ls-remote` for 30 DigitalKhatt names | No Indo-Pak 16-line data; only 4 repos exist |
| git clone github.com/AlfaazPlus/QuranApp (@c496013) | GPL-3.0; `quranapp.db` `mushafs`, `mushaf_map` (indopak_16: 8,742 rows), `ayah_words` (83,665 per script) |
| git clone github.com/marwan/indopak-quran-text | README notice "DO NOT SELL, MANIPULATE, DISTRIBUTE…"; downloads → QUL resource 59 |
| https://tanzil.net/res/text/metadata/quran-data.js | CC BY 3.0; 6,236 ayahs; juz/manzil/ruku (556) tables used for checks |
| https://api.quran.com/api/v4/verses/by_page/50?mushaf=7&words=true… | Returns Madani p.50 (3:1–3:9); not a 16-line source |
| Web searches: "16 line indopak mushaf layout data json github"; "QUL … license permission redistribute"; "quranic-universal-library issue license"; "Tarteel blog QUL"; "DigitalKhatt repositories indopak 16 lines" | No openly licensed 16-line layout found |
| /home/user/criterion/docs/research/03, 04, 05 | Context: DK/QUL rendering plan; Indo-Pak para table (matches §2.4) |

Scratch artefacts (not committed):
- **Scripts:** `<scratch>/scripts-gap16/{fetch_qul16.sh, parse_qul.py, parse_dk.py, tok_dk.py, dk_numbering.py, parse_waqar.py, norm.py, align.py, rebreak2.py, tanzil_meta.py}`
- **Results:** `<scratch>/work/gap-indopak16/{align_result.json, align_maps.json, dk15_keyed.json, dk_disputed.json, qul16.json}`
