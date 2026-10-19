# 05 — Quran Text, Structural Metadata, Translations & Tafsir: Data Specification

**Researcher key:** r05-text-metadata-translations · **Date:** 2026-10-19
**Scope:** every textual dataset the Criterion Quran app needs: where to get it, its exact license, its format and size, and how to package it offline.

Tag legend: **[verified: url]** means fetched or inspected in this session. **[unverified]** means from memory or not confirmed live. **[measured]** means computed in this session by scripts over downloaded data (scratch: `/tmp/claude-0/-home-user-criterion/eece52d7-90a8-5227-b2fc-70627d430e45/scratchpad/work-r05/`).

---

## 1. Executive summary

1. **Do not build the offline database from the Quran.com / Quran Foundation (QF) API.** The QF Developer Terms (last updated 2026-10-11) ban storing QF content for more than one week. The only exception is the authenticated *Content Sync* API, which must re-sync every 7 days. The terms also say this "does not itself authorize distributing a prepackaged database or build-time bundle of QF Content". On top of that, a mobile app "must not embed a Content API `client_secret`", so any QF content use needs our own backend. [verified: api-docs.quran.foundation/legal/developer-terms, /docs/quickstart] The old unauthenticated `api.quran.com/api/v4` still answers today, but QF now documents it as the "older unauthenticated API" and publishes a migration guide away from it. [verified]
2. **Build the base DB from three sources that allow redistribution.**
   - **Tanzil** for the Quran text: Uthmani + Simple-Clean, under CC BY 3.0 with a "verbatim copies only, changing it is not allowed" condition.
   - **Tanzil `quran-data.js`** for the metadata skeleton (CC BY 3.0).
   - **QuranEnc.com** for the default translations. Its terms explicitly allow re-publishing under 7 conditions (no modification, attribution, version number, keep updated, no inappropriate ads). They do **not** limit use to non-commercial apps.
3. **Tanzil translations are "for non-commercial purposes only"** [verified: tanzil.net/trans]. They are fine for a free, ad-free app with attribution. Before any monetization we must get permission or switch sources. Several of the most-loved South-Asian Urdu translations are **public domain in Pakistan** (life + 50 years) because the translator died long ago: Ahmed Raza Khan *Kanz-ul-Iman* (d. 1921), Muhammad Junagarhi (d. 1941), Mahmud-ul-Hasan with Shabbir Usmani's notes (d. 1920/1949), and, almost certainly, Fateh Muhammad Jalandhari (b. 1864, first published 1900). That makes them the safest Urdu choices. Maududi, Taqi Usmani, Tahir-ul-Qadri, Israr Ahmad, Karam Shah and Wahiduddin Khan are copyrighted and need written permission.
4. **Indo-Pak Para boundaries differ from the Madani Juz boundaries in 7 places**, and QF/Tanzil `juz_number` is Madani. I verified this against QUL's "Indopak 16 lines (Taj Company)" layout: every Para from 2 to 28 starts at the top of page 18n−16. The Indo-Pak starts are:

   | Para | Indo-Pak start | Madani start |
   |---|---|---|
   | 4 | **3:92** | 3:93 |
   | 7 | **5:83** | 5:82 |
   | 11 | **9:94** | 9:93 |
   | 14 | **15:2** | 15:1 |
   | 20 | **27:60** | 27:56 |
   | 21 | **29:45** | 29:46 |
   | 23 | **36:22** | 36:28 |

   The traditional Para names (لن تنالوا، واذا سمعوا، يعتذرون، ربما، امّن خلق، اتل ما اوحي، وما لي…) are exactly the opening words at the Indo-Pak boundaries. [measured; corroborated by gtaf.org] The app must ship its own Para table and use it for indexing **and** for "play Para" audio ranges.
5. **The ruku count depends on the source.**
   - Tanzil, alquran.cloud and fawazahmed0 give **556**.
   - QF/Quran.com and QUL give **558**. That set has 9 rukus in Al-Qasas and 4 in Luqman, adds rukus at 28:83 and 31:31, and shifts two Shu'ara boundaries (26:52, 26:69).
   - "540" is a traditionally quoted figure (Wikipedia: "540 or 558").

   Use the 558 set (QUL), then have a human check it against the 16-line pages. Medium confidence.
6. **Sajdas: Hanafi Indo-Pak = 14.** QF's 14 includes 38:24 and excludes 22:77. Tanzil's 15 includes both, and its "obligatory/recommended" labels follow a non-Hanafi classification, so **do not show those labels**. Mark 22:77 as "سجدہ عند الشافعی" the way printed Indo-Pak mushafs do.
7. **Indo-Pak script text is bound to its font.** QF/QUL `text_indopak_nastaleeq` contains ~420 distinct code points:
   - Farsi Yeh U+06CC (24k occurrences)
   - waqf marks U+0615/0617/06D6–06DC/06EA–06EC
   - **Private-Use-Area glyphs U+F500–U+F69x for ayah-number medallions**

   It renders correctly only with `indopak-nastaleeq-waqf-lazim-v4.2.1`. Tanzil has **no** Indo-Pak text. Strip the PUA code points before search, copy and share.
8. **Page mappings exist for every target mushaf** (QUL layouts, login needed to download):
   - Indo-Pak 16-line Taj: **548** pages (the reference app's edition)
   - Indo-Pak 15-line: 610
   - Indo-Pak 13-line Taj: 847
   - 13-line Qudratullah: 849
   - Madani: 604

   QF mushaf IDs are 6 = 15-line and 7 = 16-line. QUL's page indices look **one lower than the printed/QF page labels** (Ayatul Kursi is p.38 in QUL and p.39 per QF docs). So store `page_index` and `page_label` separately.
9. **Word-by-word (WBW) Urdu/English has no clean open license.** QUL credits QuranWBW.com, whose site says copying is not allowed, and the Urdu WBW is reportedly Dr Farhat Hashmi's work. QF lists its WBW authors as "Quran.com" or "Unknown". Use WBW for personal builds, and get permission (QuranWBW/Al-Huda) or use QF Content Sync via a backend before publishing.
10. **Offline packaging plan.**
    - `quran_content.db` is bundled and read-only, rebuilt by a Python pipeline. It holds Tanzil Uthmani + imla'i + Indo-Pak text, all indexes, the 16-line layout and FTS4 search. It is about 11 MB raw, ~3.5 MB compressed in the APK.
    - Translation, tafsir and WBW content ship as **separate SQLite "packs"** (0.5–5 MB compressed each) with a signed manifest.
    - User data lives in a separate `user.db`.
11. **Search design (prototyped).** FTS4 (available in the framework SQLite on minSdk 24, which ships SQLite 3.9) over **three normalized Arabic columns** (imla'i, Uthmani-rasm, Indo-Pak). Normalization drops harakat, Quranic marks, dagger alef, tatweel and PUA, and folds alef/hamza/ya/ta-marbuta/kaf/heh. All 13 test queries hit, including الصلاة/الصلوة, السماوات/السموت, يأيها/يا أيها, مالك/ملك. Urdu and English get their own normalizers. A curated alias table covers "Yaseen", "Ayatul Kursi", "Rehman" and similar. FTS5 is only an option with the androidx `sqlite-bundled` driver, which compiles with `-DSQLITE_ENABLE_FTS5` [verified].
12. **Tafsir.**
    - **Bundle-safe Urdu tafsir:** *Tafsir as-Sa'di (Urdu)* from QuranEnc, and the Ahsan-ul-Bayan footnotes inside QuranEnc's Junagarhi edition (4.35 MB) as the "tafsir summary" feature.
    - **Need permission:** Ibn Kathir (Urdu/English), Ma'ariful Qur'an, Tafheem and Bayan-ul-Quran.
    - **Quranic Arabic Corpus morphology** is GPL with verbatim-only terms: optional pack, Phase 3.
    - **cpfair tajweed annotations** (CC BY 4.0) do **not** line up with the current Tanzil v1.1 text: 7,510 of 13,252 hamzat-wasl offsets are wrong [measured]. Re-run the classifier or defer tajweed.

---

## 2. Detailed findings

### 2.1 Source landscape at a glance

| Source | What it offers | Access | License / terms (decisive text) | Fit for us |
|---|---|---|---|---|
| **Tanzil** (tanzil.net) | Quran text v1.1 (Feb 2021), 6 types; metadata (`quran-data.js/xml` v1.0); ~115 translations incl. 8 Urdu | Open HTTP downloads (no login) | Text: *"CC BY 3.0 … Permission is granted to copy and distribute verbatim copies of this text, but CHANGING IT IS NOT ALLOWED … source (Tanzil Project) is clearly indicated, and a link is made to tanzil.net"*. Metadata: "License: Creative Commons Attribution 3.0". Translations: *"for non-commercial purposes only. If used otherwise, you need to obtain necessary permission from the translator or the publisher."* [verified] | **Primary** for Arabic text + metadata; translations OK for a free app |
| **QUL** (qul.tarteel.ai, Tarteel) | Largest catalogue: 30 scripts (incl. Indo-Pak Nastaleeq, Digital Khatt Indo-Pak, QPC), 11 mushaf layouts (Indo-Pak 9/13/15/16-line), metadata (juz/hizb/rub/manzil/ruku 558/sajda), 205 translations, 108 tafsirs, morphology, transliteration, surah info | JSON/SQLite downloads **require a free login** [verified: download links route to `/users/sign_in`]; no API [verified FAQ] | FAQ: *"Yes, you can use QUL data in commercial projects. However, please review the licensing terms for each resource."* Code is MIT [verified LICENSE]. Internally each resource has `permission_to_share` = unknown/requested/granted/rejected, and **"unknown" is still published** [verified: `app/models/resource_content.rb#allow_publish_sharing?`] | **Primary** for Indo-Pak script, 16-line layout, ruku 558. Per-resource licensing must be confirmed |
| **Quran Foundation / Quran.com API** | Content API v4 (text fields, 9 mushafs, ~126 translations, 20 tafsirs, WBW, audio); Content Sync; User APIs | OAuth2 client-credentials (backend only); legacy `api.quran.com/api/v4` still answers without auth | ≤1-week cache except Content Sync; no prepackaged bundle; no redistribution; fonts and mushaf images may be bundled with credit (see §2.6) [verified] | **Not for the bundled DB.** Later: User API sync; optional Content Sync via backend |
| **QuranEnc.com** (King Fahd Complex / Rowwad) | 76 vetted translations (Urdu Junagarhi with footnotes, Saheeh Intl., Hilali-Khan, Rowwad EN, Hindi Omari, Indonesian ×3, Turkish ×3, Pashto, Punjabi…), tafsir As-Sa'di (ar/ur/tr/id); SQLite/JSON/API | Open API + direct SQLite zips | *"Contents of the translations can be downloaded and re-published, with the following terms: 1. No modification, addition, or deletion… 2. Clearly referring to the publisher and the source (QuranEnc.com). 3. Mentioning the version number… 4. Keeping the transcript information… 5. Notifying the source… of any note… 6. Updating the translation according to the latest version… 7. Inappropriate advertisements must not be included…"* [verified: quranenc.com/en/home/api/] | **Primary** for bundled translations |
| **alquran.cloud** | REST API; Tanzil-derived editions, `quran-tajweed`, `quran-wordbyword`(-2), audio | Key-less, soft rate limit | Text: "reproduce… freely, for any non-commercial purpose"; "please attribute the translator by name" (updated 14 Jun 2026) [verified] | Reference only. Its translations are Tanzil's, so Tanzil's terms apply |
| **fawazahmed0/quran-api** | 492 editions (Urdu ×13, Roman-Urdu "-la"), metadata `info.json` | jsDelivr CDN | Repo is **Unlicense** [verified]. This does **not** clear rights in third-party translations it mirrors (e.g., "Mustafa Khattab", "The Study Quran") | Don't use as a rights source. Its Roman Urdu has a verse misalignment (see §2.5) |
| **spa5k/tafsir_api** | 123 tafsirs as static JSON | jsDelivr / self-host | Code MIT [verified]. Content scraped from quran.com/QUL/altafsir; no rights clearance | Don't use as a rights source |
| **Quranic Arabic Corpus** v0.4 | Morphology, roots, lemmas | Email-gated download | *"License: GNU General Public License … Permission is granted to copy and distribute verbatim copies of this file, but CHANGING IT IS NOT ALLOWED"* [verified: corpus.quran.com/download] | Optional pack (Phase 3) |
| **cpfair/quran-tajweed** | Tajweed rule spans (char offsets) for Uthmani text | GitHub | Data **CC BY 4.0** [verified README]; offsets tied to a 2017 Tanzil copy | Phase 3 (needs regeneration) |

### 2.2 Quran text: types, encodings, font pairing

#### 2.2.1 Available Unicode texts

| Text | Source / id | Script family | Distinct code points | UTF-8 size | Must pair with | Notes |
|---|---|---|---|---|---|---|
| Tanzil **Uthmani** v1.1 | tanzil.net download `quranType=uthmani` | Madani rasm | 70 | 1.34 MB (XML 1.54 MB; gz 0.29 MB) | Any full-Quranic Naskh font: me_quran, KFGQPC Uthmanic Hafs (Tanzil options mention "only for me_quran font") [verified]; Amiri Quran / Scheherazade New [unverified] | Identical code-point counts to QF `text_uthmani` [measured]. QF's Uthmani is Tanzil-derived |
| Tanzil **Uthmani-min** | `uthmani-min` | Madani, fewer marks | — | — | same | — |
| Tanzil **Simple** / Simple-Plain / Simple-Min | `simple*` | Imla'i (modern spelling) | — | 1.50 MB XML | Any Arabic font | Download options: pause marks, sajdah ۩, rub-el-hizb ۞, superscript alef, tatweel [verified] |
| Tanzil **Simple-Clean** | `simple-clean` | Imla'i, no diacritics | 46 | 0.94 MB XML | any | **Primary search column source** |
| QF/QUL **`text_indopak_nastaleeq`** | QUL "Indopak Nastaleeq script – Word by Word"; QF field | **Indo-Pak** | **419** (incl. ~300 PUA) | ~1.40 MB | **only** `indopak-nastaleeq-waqf-lazim-v4.2.1` (QF CDN `verses.quran.foundation/fonts/quran/hafs/nastaleeq/indopak/…`); *"with other fonts, waqf signs and verse-end markers will not render correctly"* [verified: QF font guide] | Recommended Indo-Pak display text (license to confirm with QUL) |
| QF `text_indopak` (old) | QF field (deprecated) | Indo-Pak | 90 | ~1.4 MB | old IndoPak font | Contains U+E0xx PUA, ZWSP ×4,747, RLM ×6,247, BOM ×1,614, U+06E1 as jazm ×62k [measured]. **Avoid** |
| QF `text_qpc_nastaleeq` | QF field / QUL "QPC Nastaleeq" | KFGQPC Nastaleeq (15-line, 610 pp) | 434 | ~1.4 MB | KFGQPC Nastaleeq font | Alternative Indo-Pak style |
| QF `text_qpc_hafs` | QF / QUL "KFGQPC Hafs" | Madani (KFGQPC) | 83 | ~1.3 MB | KFGQPC Uthmanic Hafs | Uses U+06E1 for sukun ×37,148, Indic digits for ayah numbers, U+00A0 [measured] |
| fawazahmed0 `ara-quranindopak` | KFGQPC Nastaleeq converted to Unicode | Indo-Pak | — | — | "Noto Sans Arabic … fonts with -full suffix" (per its comment) [verified editions.json] | Rights of the KFGQPC original are unclear (fonts.qurancomplex.gov.sa did not answer from this sandbox) |
| QUL **Digital Khatt Indopak** | QUL quran-script | Indo-Pak | — | — | DigitalKhatt font | Font/licensing is for the fonts researcher |

**Indo-Pak-specific marks to support** (from the inventory of `text_indopak_nastaleeq` [measured]):
- U+0615 small high Tah (ط waqf-e-mutlaq) ×3,540
- U+0617 small high Zain (ز) ×238
- U+06D6–06DB waqf ligatures (صلے/قلے/م/لا/ج/∴) ×~3,700
- U+06DF small high rounded zero (ayah-end anchor) ×6,237
- U+06E0 ×552
- U+06E4 small high madda ×3,025
- U+0656 subscript alef ×993
- U+0657 inverted damma (ulta pesh) ×1,257
- U+0670 superscript alef ×12,570
- U+06EA–06EC stop marks ×~350
- U+0614 takhallus ×41
- **U+06CC Farsi Yeh ×24,416** (Indo-Pak uses ی, not ي)
- **PUA U+F500…U+F69x**: U+F500 appears in all 114 surahs, U+F503 in 111 (three surahs have exactly 3 ayahs), and so on. That pattern shows they are ayah-number glyphs 1…n.

#### 2.2.2 Decisions on text

- **Display:** Indo-Pak Nastaleeq (QUL) is the default for the mushaf and verse-list modes. Tanzil Uthmani is an optional "Usmani" script, matching the reference app's second script.
- **Canonical key and search:** Tanzil Uthmani + Simple-Clean, pinned by SHA-256. Tanzil is the reference for ayah counts and ordering.
- **Tanzil verbatim rule:** never alter displayed Tanzil text. Normalized search columns are an internal index and are never displayed. Keep the Tanzil notice in the DB `attribution` table and on the About screen.
- **Cross-check:** after normalization, 4,818 of 6,236 Indo-Pak ayahs equal Uthmani exactly, and **97.6 %** of Indo-Pak words appear in the matching Uthmani ayah [measured]. The rest are expected rasm differences (الصراط vs الصرط, ابصارهم vs ابصرهم). Use this as a CI corruption detector with a frozen whitelist of known differences.

### 2.3 Structural metadata

#### 2.3.1 Counts by source

| Item | Tanzil `quran-data.js` | QF API v4 (live) | QUL metadata | alquran.cloud `/v1/meta` | fawazahmed0 `info.json` |
|---|---|---|---|---|---|
| Surahs / ayahs | 114 / 6,236 | 114 / 6,236 (+83,665 words in `quran_core:1`) | 114 / 6,236 | 114 / 6,236 | 114 / 6,236 |
| Juz | 30 (Madani) | 30 (Madani); `/juzs` returns **60** records (duplicates, API quirk) | 30 | 30 | 30 |
| Hizb quarters (rub) | 240 | 240 (rub 15:**50** vs Tanzil 15:**49**) | 240 | 240 | 240 ("maqras") |
| Manzil | 7 | 7 (identical) | 7 | 7 | 7 |
| **Ruku** | **556** | **558** (`by_ruku/558` → 114:1-6; `559` → 404) | **558** ("Jump to Ruku … 558"; page text says "about 540") | 556 | 556 |
| Pages | 604 (Madani) | 604 default; 548/610 for mushaf 7/6 via authenticated `pages/lookup` | per layout | 604 | 604 |
| Sajda | **15** (incl. 22:77) with "obligatory/recommended" types | **14** (excl. 22:77) | sajda resource | 15 | 15 |

[verified: tanzil quran-data.js; api.quran.com/api/v4/verses/by_ruku/558 and /559; /juzs; api.alquran.cloud/v1/meta; fawazahmed0 info.json; qul.tarteel.ai/resources/quran-metadata/65] [measured comparisons]

**Ruku differences, Tanzil (556) vs QF/QUL (558)** [measured]:

| | Tanzil | QF/QUL |
|---|---|---|
| Shu'ara rukus start at | 26:53, 26:70 | 26:52, 26:69 |
| Al-Qasas | 8 rukus | 9 rukus (extra ruku at 28:83) |
| Luqman | 3 rukus | 4 rukus (extra ruku at 31:31) |

All other boundaries are identical. Juz, manzil and page-604 boundaries are identical between Tanzil and QF.

#### 2.3.2 Indo-Pak Para table (bundle this; do not derive it from QF/Tanzil Juz)

Verified against QUL "Indopak 16 lines layout (Taj company)", which is para-aligned: each Para 2–28 starts at the top of page 18n−16 [verified: qul.tarteel.ai/resources/mushaf-layout/11?page=N]. GTAF lists the same 7 variant juz [verified: gtaf.org]. The Arabic opening words come from Tanzil Simple-Clean at the Indo-Pak start [measured]. Roman names are the common Pakistani spellings (first 9 confirmed in the reference app by 01-teardown; the rest [unverified]).

| # | Name (Arabic/Urdu) | Roman (PK) | Indo-Pak start | Madani start | 16-line page | Ayahs | Rukus (558 set) |
|---|---|---|---|---|---|---|---|
| 1 | الٓمّٓ | Alif Lam Meem | 1:1 | 1:1 | 1 (Fatiha) / 2 | 148 | 17 |
| 2 | سَیَقُوْلُ | Sayaqool | 2:142 | 2:142 | 20 | 111 | 17 |
| 3 | تِلْكَ الرُّسُلُ | Tilkar Rusul | 2:253 | 2:253 | 38 | 125 | 16 |
| 4 | لَنْ تَنَالُوْا | Lan Tanaloo | **3:92** | 3:93 | 56 | 132 | 15 |
| 5 | وَالْمُحْصَنٰتُ | Wal Mohsanat | 4:24 | 4:24 | 74 | 124 | 17 |
| 6 | لَا یُحِبُّ اللّٰهُ | La Yuhibbullah | 4:148 | 4:148 | 92 | 111 | 14 |
| 7 | وَاِذَا سَمِعُوْا | Wa Iza Samiu | **5:83** | 5:82 | 110 | 148 | 18 |
| 8 | وَلَوْ اَنَّنَا | Wa Lau Annana | 6:111 | 6:111 | 128 | 142 | 18 |
| 9 | قَالَ الْمَلَاُ | Qalal Malao | 7:88 | 7:88 | 146 | 159 | 18 |
| 10 | وَاعْلَمُوْا | Wa A'lamu | 8:41 | 8:41 | 164 | 128 | 17 |
| 11 | یَعْتَذِرُوْنَ | Yatazeroon | **9:94** | 9:93 | 182 | 150 | 16 |
| 12 | وَمَا مِنْ دَآبَّةٍ | Wa Mamin Da'abba | 11:6 | 11:6 | 200 | 170 | 16 |
| 13 | وَمَاۤ اُبَرِّئُ | Wa Ma Ubarri'u | 12:53 | 12:53 | 218 | 155 | 19 |
| 14 | رُبَمَا | Rubama | **15:2** | 15:1 | 236 | 226 | 21 |
| 15 | سُبْحٰنَ الَّذِیْۤ | Subhanallazi | 17:1 | 17:1 | 254 | 185 | 22 |
| 16 | قَالَ اَلَمْ | Qal Alam | 18:75 | 18:75 | 272 | 269 | 16 |
| 17 | اِقْتَرَبَ لِلنَّاسِ | Iqtaraba | 21:1 | 21:1 | 290 | 190 | 17 |
| 18 | قَدْ اَفْلَحَ | Qad Aflaha | 23:1 | 23:1 | 308 | 202 | 17 |
| 19 | وَقَالَ الَّذِیْنَ | Wa Qalallazina | 25:21 | 25:21 | 326 | 343 | 20 |
| 20 | اَمَّنْ خَلَقَ | Amman Khalaq | **27:60** | 27:56 | 344 | 166 | 15 |
| 21 | اُتْلُ مَاۤ اُوْحِیَ | Utlu Ma Oohi | **29:45** | 29:46 | 362 | 179 | 20 |
| 22 | وَمَنْ یَّقْنُتْ | Wa Manyaqnut | 33:31 | 33:31 | 380 | 163 | 18 |
| 23 | وَمَا لِیَ | Wa Mali | **36:22** | 36:28 | 398 | 363 | 16 |
| 24 | فَمَنْ اَظْلَمُ | Faman Azlam | 39:32 | 39:32 | 416 | 175 | 20 |
| 25 | اِلَیْهِ یُرَدُّ | Elahe Yuraddu | 41:47 | 41:47 | 434 | 246 | 19 |
| 26 | حٰمٓ | Ha Meem | 46:1 | 46:1 | 452 | 195 | 19 |
| 27 | قَالَ فَمَا خَطْبُكُمْ | Qala Fama Khatbukum | 51:31 | 51:31 | 470 | 399 | 19 |
| 28 | قَدْ سَمِعَ اللّٰهُ | Qad Sami Allah | 58:1 | 58:1 | 488 | 137 | 20 |
| 29 | تَبٰرَكَ الَّذِیْ | Tabarakallazi | 67:1 | 67:1 | **508** | 431 | 22 |
| 30 | عَمَّ | Amma | 78:1 | 78:1 | **528** | 564 | 39 |

Pages are QUL indices; printed labels may be +1 (see 2.3.6). Para 1 is named after 2:1 (الٓمّٓ), not 1:1. Rukus = rukus whose first ayah falls in the Para (558 set; total 558) [measured]. Paras 29–30 drift off the 18-page grid (508, 528) [verified]. Store Arabic names *with* Indo-Pak diacritics as hand-curated strings, not computed.

#### 2.3.3 Manzils (7, all sources agree) [verified Tanzil + QF; 16-line pages verified on QUL]

| Manzil | Start | Start surah (Urdu mnemonic **فمی بشوق**) | End | 16-line page |
|---|---|---|---|---|
| 1 | 1:1 | **ف** Al-Fatiha | 4:176 | 1 |
| 2 | 5:1 | **م** Al-Ma'idah | 9:129 | 96 |
| 3 | 10:1 | **ی** Yunus | 16:128 | 187 |
| 4 | 17:1 | **ب** Bani Isra'il (Al-Isra) | 25:77 | 254 |
| 5 | 26:1 | **ش** Ash-Shu'ara | 36:83 | 330 |
| 6 | 37:1 | **و** Was-Saffat | 49:18 | 401 |
| 7 | 50:1 | **ق** Qaf | 114:6 | 466 |

(The mnemonic is traditional [unverified]; the boundaries are [verified].) Manzil 2/3/5/6/7 start mid-page in the 16-line edition, so the "منزل ۱" foot marker is a page-level label.

#### 2.3.4 Rukus and the Indo-Pak "ع" marker

- **Data:** use the 558 set (QUL "Ruku" metadata 65: `ruku_number, surah_ruku_number, verses_count, first_verse_key, last_verse_key, verse_mapping` [verified sample JSON]).
- **Marker numbers:** the printed marginal ع carries three numbers: ruku-in-surah / ayahs-in-ruku / ruku-in-para (per the 01-teardown screenshot crop). Compute `ruku_in_para` against the **Indo-Pak Para table**. The marker sits at the ruku's **end**, so a ruku that straddles a Para start (Paras 3, 5, 6, 7, 9–14, 16, 20, 22, 23, 25 and 27 start mid-ruku in the 558 set [measured]) should be counted in the Para where it ends [unverified convention; check against print].
- **QA:** before release, a person checks the 4 Tanzil/QF differences (26:52/53, 26:69/70, 28:83, 31:31) against the 16-line Taj print.

#### 2.3.5 Sajdas

| Ayah | 7:206 | 13:15 | 16:50 | 17:109 | 19:58 | 22:18 | **22:77** | 25:60 | 27:26 | 32:15 | **38:24** | 41:38 | 53:62 | 84:21 | 96:19 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Tanzil (15) | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ "obligatory" | ✓ | ✓ "obligatory" | ✓ "obligatory" | ✓ | ✓ "obligatory" |
| QF (14) | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | – | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |

[verified both] The QF list equals the Hanafi set. Tanzil's four "obligatory" ones match the Shia *'aza'im* classification [unverified interpretation]. Store `sajda(ayah_id, kind ∈ {hanafi, shafii_only})` and render 22:77 with "عند الشافعی".

#### 2.3.6 Page mappings per mushaf

| Mushaf | Lines | Pages | QUL layout id | QF mushaf id | Notes |
|---|---|---|---|---|---|
| **Indo-Pak Taj Company** | **16** | **548** | 11 | 7 ("IndoPak 16-line, 548 pages") | **Reference app's edition** (01-teardown). Para-aligned. QUL p.38 holds 2:253–2:257, but QF docs place 2:255 on page 39 → probable +1 label offset |
| Indo-Pak Qudratullah | 15 | 610 | 12 | 6 ("IndoPak 15-line, 610") | "Hafizi" |
| KFGQPC Nastaleeq | 15 | 610 | 8 | — | Quran Complex Indo-Pak |
| Indo-Pak Taj Company | 13 | 847 | 313 | — | Common in India / South Africa |
| Indo-Pak Qudratullah | 13 | 849 | 236 | — | |
| Indo-Pak Gaba | 9 | ? | 571 | — | |
| KFGQPC V1 (1405H) / V2 (1421H) / V4 tajweed (1441H) | 15 | 604 | 15 / 10 / 19 | 2 / 1 / 19 | V4 fonts "disabled… proofreading" on QUL |
| Tanzil / QF default | 15 | 604 | — | — | Madani |

[verified: QUL layout pages; QF page-layout guide: `MUSHAF_IDS = { QCF_V2: 1, QCF_V1: 2, INDOPAK: 3, UTHMANI_HAFS: 4, KFGQPC_HAFS: 5, INDOPAK_15_LINES: 6, INDOPAK_16_LINES: 7, TAJWEED: 11, QCF_TAJWEED_V4: 19 }`]

The legacy `api.quran.com` **ignores `mushaf=`** (page 548 returned the same verses for ids 1–20) and returns 404 for `/pages/lookup` [verified]. Indo-Pak page mapping is therefore available only from QUL downloads or the authenticated QF API.

QUL layout schema [verified: QUL layout help]:
- `pages(page_number, line_number, line_type ∈ {ayah, surah_name, basmallah}, is_centered, first_word_id, last_word_id, surah_number)`
- plus a words table `words(word_index, word_key, surah, ayah, text)` from the matching script export.

#### 2.3.7 Hizb / Rub and Indo-Pak quarter markers (open)

- Madani: 60 hizb / 240 rub-el-hizb (Tanzil, QF).
- Indo-Pak mushafs print **الربع / النصف / الثلٰثة** quarter marks per Para. No dataset was found: QUL's 16-line layout HTML has no such markers [verified by grep].
- For the 23 Paras whose boundaries match Madani, ربع/نصف/ثلٰثة probably coincide with rub-el-hizb 2/4/6 of the Juz [unverified]. For the 7 differing Paras they may not.
- **Action:** capture the 90 positions by hand from the Taj 16-line print (≈1 hour), and keep the `para_quarter` table separate.

### 2.4 Translations

#### 2.4.1 Urdu (priority)

Copyright status uses Pakistan's term: literary work = life of author + 50 years from the end of the year of death (s.18) [verified: en.wikipedia.org/wiki/Copyright_law_of_Pakistan]. Under life + 70, the PD date moves 20 years later.

| Translator · work · school | Author dates | PK status (2026) | Tanzil | QF id | QUL id | QuranEnc | fawazahmed0 | Verdict |
|---|---|---|---|---|---|---|---|---|
| **Fateh Muhammad Jalandhari** · *Fath-ul-Hameed* (1900) · general/Deobandi-accepted | b. 1864 [verified: wikitia via search]; d. ? | **Almost certainly PD** | `ur.jalandhry` (1.45 MB txt) | 234 | 218 | – | `urd-fatehmuhammadja` | **Bundle** (Tanzil file + attribution); default Urdu |
| **Muhammad Junagarhi** · Ahl-e-Hadith; QuranEnc edition carries *Ahsan-ul-Bayan*-style footnotes (Salahuddin Yusuf, d. 2020 [unverified]) | 1890–1941 [verified: Wikipedia via search] | Translation **PD**; footnotes © but covered by QuranEnc permission | `ur.junagarhi` | 54 | 305 | **`urdu_junagarhi` v1.1.3** (sqlite 6.16 MB; zip 2.1 MB; footnotes 4.35 MB) [verified/measured] | `urd-muhammadjunagar` | **Bundle** (QuranEnc). Footnotes = "tafsir summary" pack |
| **Ahmed Raza Khan** · *Kanz-ul-Iman* (1911) · Barelvi | 1856–1921 [verified: Wikipedia summary] | **PD** | `ur.kanzuliman` | – | – | – | – | **Download pack** (Tanzil file) |
| **Mahmud-ul-Hasan** (+ *Tafsir-e-Usmani* notes by Shabbir Ahmad Usmani) · Deobandi | 1851–1920 [unverified] / 1887–1949 [verified] | **PD** | – | 151 | 180 | – | `urd-mahmoodulhassan` (src dailyayat.com) | Pack. Need a clean, proof-read transcription; the QF copy is QF-restricted |
| Ashraf Ali Thanwi · *Bayan-ul-Quran* (Thanwi) · Deobandi | 1863–1943 [unverified] | PD | – | – | – | – | – | Future (no digital source found) |
| Abul A'la Maududi · *Tafheem-ul-Quran* · JI | 1903–1979 [unverified] | © until 31-Dec-2029 | `ur.maududi` | 97 | 302 | – | `urd-abulaalamaududi` | Permission (Idara Tarjuman-ul-Quran / Islamic Publications) |
| Taqi Usmani · *Aasan Tarjuma* · Deobandi | living | © | – | (EN 84) | – | – | `urd-muhammadtaqiusm` (1.88 MB) | Permission (Maktaba Ma'ariful Quran) |
| Tahir-ul-Qadri · *Irfan-ul-Quran* · Barelvi/Minhaj | living | © | `ur.qadri` | – | – | – | `urd-muhammadtahirul` | Permission (Minhaj-ul-Quran) |
| Israr Ahmad · *Bayan-ul-Quran* | 1932–2010 [unverified] | © | – | 158 | 183 | – | – | Permission (Anjuman Khuddam-ul-Quran) |
| Karam Shah Al-Azhari · *Zia-ul-Quran* · Barelvi | 1918–1998 [unverified] | © | – | – | – | – | `urd-muhammadkaramsh` | Permission |
| Ahmed Ali (writer) | d. 1994 [unverified] | © | `ur.ahmedali` | – | – | – | `urd-ahmedali` | Skip |
| Wahiduddin Khan | d. 2021 [unverified] | © | – | 819 | 284 | – | – | Permission (CPS/Goodword) |
| Jawadi, Najafi (Shia) | — | © | `ur.jawadi`, `ur.najafi` | – | – | – | ✓ | Out of scope v1 |

[verified: Tanzil trans list + bio links; QF `/resources/translations`; QUL translation list; QuranEnc API list; fawazahmed0 editions.json]

#### 2.4.2 English

| Translation | Rights | Bundle-safe source | Other ids | Verdict |
|---|---|---|---|---|
| **Saheeh International** | © (now "Noor International Center" on QuranEnc) | **QuranEnc `english_saheeh` v1.1.2** (1.25 MB sqlite) [verified] | Tanzil `en.sahih` (n-c); QF 20; QUL 193 | **Bundle default** |
| **Pickthall** (1930) | Author d. 1936 [unverified] → PD under life+70 and PK; US: 1930 works PD since 2026 [unverified] | Tanzil `en.pickthall` | QF 19; QUL 145 | Bundle option (PD) |
| Yusuf Ali (1934) | d. 1953 [unverified] → PD in PK and life+70; edition provenance unclear | Tanzil `en.yusufali` | QF 22; QUL 124 | Pack (check which edition) |
| Hilali-Khan | KFGQPC | **QuranEnc `english_hilali_khan`** | QF 203; QUL 301 | Pack |
| Rowwad Translation Center | — | **QuranEnc `english_rwwad`** v1.0.19 | QUL 195 | Pack (modern, footnoted) |
| Taqi Usmani (EN) | © | – | QF 84; QUL 153 | Permission |
| Mustafa Khattab *The Clear Quran* | © Book of Signs Foundation [unverified] | – | QF 131 **not public** (`/quran/translations/131` returns empty) [verified]; fawaz mirrors it without a license | Permission |
| Abdel Haleem; Maududi (Tafhim EN); Wahiduddin Khan; Ahmed Ali; Asad; Arberry | © | – | QF 85/95; QUL… | No |

Note: QuranEnc's `english_saheeh` prefixes ayah numbers inside the text (e.g. "(2) [All] praise…") [measured]. QuranEnc's condition 1 forbids modification, so render it as-is or ask QuranEnc whether stripping the number prefix is acceptable.

#### 2.4.3 Other languages (Phase 2 packs)

| Language | Bundle-safe (QuranEnc) | Non-commercial only (Tanzil) | Other |
|---|---|---|---|
| Hindi | `hindi_omari` v1.1.5 | `hi.farooq`, `hi.hindi` | QF 122 |
| Bangla | — (none in QuranEnc API) [verified] | `bn.bengali` (Muhiuddin Khan), `bn.hoque` | QF 161/162/163/213 |
| Indonesian | `indonesian_affairs`, `indonesian_sabiq`, `indonesian_complex` | `id.indonesian`, `id.muntakhab` | |
| Turkish | `turkish_rwwad`, `turkish_shaban`, `turkish_shahin` | `tr.diyanet`, `tr.vakfi` +8 | |
| Pashto | `pashto_rwwad` | `ps.abdulwali` | |
| **Punjabi** | `punjabi_arif` (useful in PK) | — | |
| Sindhi | — | `sd.amroti` | |

### 2.5 WBW, transliteration, Roman Urdu, tafsir, tajweed, morphology

| Dataset | Sources (ids) | License status | Size | Recommendation |
|---|---|---|---|---|
| **WBW Urdu** | QF 104/129 (author "Quran.com"); QUL 93 | QUL credits **QuranWBW.com** for WBW data [verified]. QuranWBW pages state "data… belongs to their respective owners/authors and copying them is not allowed"; Urdu WBW attributed to Dr Farhat Hashmi [from search; unverified] | ~2–3 MB raw (77–84k words) [estimate] | Personal build: QUL. Public release: written permission (QuranWBW / Al-Huda) **or** QF Content Sync (`word_by_word_translations`) via backend |
| **WBW English** | QF 59 ("Unknown"); QUL 92; alquran.cloud `quran-wordbyword` / `-2` (Dr Shehnaz Shaikh) | Unclear | ~2 MB | Same as above |
| WBW Hindi/Bangla/Indonesian/Turkish | QUL 44/94/96/99; QF 551/99/100/555 | Unclear | ~2 MB each | Phase 3 |
| **English transliteration (ayah)** | Tanzil `en.transliteration` (1.04 MB) | Tanzil translation terms (n-c) | 1.0 MB | Bundle for a free app with attribution |
| English transliteration (word) | QF resource 60 (77,429 rows; Content Sync) [verified]; QUL 71/72/469/477 | QF-restricted / unclear | ~1.5 MB | Phase 2 via Content Sync |
| **Roman Urdu translation** | QF 831 / QUL 281 / fawaz `urd-abulaalamaududi-la` (Maududi, human-quality) | © Maududi | ~1.2 MB | Permission |
| Roman Urdu (machine) | fawaz `urd-muhammadjunagar-la` | Derived from Tanzil | — | **Reject**: Al-Fatiha is shifted by one verse (its "1:1" holds the meaning of 1:2) [verified] |
| Roman Arabic transliteration (South-Asian style, "Bismillaahir Rahmaanir Raheem") | fawaz `ara-quran-la1` (source quran411.com) | Unclear | — | Phase 3 / permission |
| **Tafsir As-Sa'di Urdu** | **QuranEnc `urdu_saadi`** v1.0.0; QUL 309 | **QuranEnc republish terms** | ~13 MB text (surah 2 = 1.06 MB at 8.04 % of the Quran) → ~3–4 MB compressed [measured/extrapolated] | **Pack (Phase 1–2)** |
| **Ahsan-ul-Bayan footnotes (Urdu)** | inside QuranEnc `urdu_junagarhi` | QuranEnc terms | 4.35 MB raw | **Pack: "Tafsir summary (Urdu)"** |
| Ibn Kathir Urdu | QF 160; QUL 30 | Unclear (translator probably Junagarhi → PD [unverified]); digitization QF/QUL | ~25–40 MB raw [estimate] | Verify translator; then pack |
| Bayan-ul-Quran (Israr) tafsir | QF 159; QUL 29 | © | — | Permission |
| Tafheem notes / Tafsir-e-Usmani notes | inside QF 97 / 151 | © / PD | — | Usmani: PD transcription later |
| Ibn Kathir English (Darussalam abridged) | QF 169; QUL 35 | © Darussalam | — | Permission |
| Ma'ariful Qur'an English | QF 168; QUL 34 | © | — | Permission |
| Tazkir-ul-Quran EN/UR | QF 817/818 | © | — | Permission |
| Al-Mukhtasar (EN etc.) | QUL 266… | Tafsir Center; license not stated | — | Ask |
| Arabic: Muyassar, Jalalayn | Tanzil `ar.muyassar`, `ar.jalalayn` (n-c); QF 16 | — | ~2–4 MB | Phase 3 |
| Arabic: As-Sa'di | QuranEnc `arabic_saadi` | QuranEnc terms | — | Phase 3 |
| **Tajweed annotations** | cpfair `tajweed.hafs.uthmani-pause-sajdah.json` (5.58 MB; gz 0.32 MB; 6,236 entries) | **CC BY 4.0** [verified] | 5.6 MB | Offsets require the **2017** Tanzil copy (README); on the current v1.1 text 7,510/13,252 hamzat-wasl spans are wrong [measured]. Re-run classifier (repo has no LICENSE file) or use alquran.cloud `quran-tajweed` markup / QF `text_uthmani_tajweed` (QF-restricted). **Madani-only. No Indo-Pak tajweed dataset found** → Phase 3 |
| **Morphology / roots** | QAC v0.4; `mustafa0x/quran-morphology` (130,030 segments, 6.32 MB, roots/lemmas in Arabic) [verified]; QUL 75/76/77 | **GPL** + verbatim-only terms (QAC) [verified] | 6.3 MB raw | Phase 3 optional pack with GPL notice. Needed only for root search |
| Surah info (Urdu/English intros) | QUL 4 (Urdu), 3 (English) | Likely from Tafheem (©) [unverified] | — | Write our own short intros or get permission |

### 2.6 Quran Foundation / Quran.com API (verified 2026-10-19)

**Content API** [verified: /docs/quickstart, /docs/quickstart/migration]
- Auth: `POST {auth}/oauth2/token`, `grant_type=client_credentials&scope=content`, HTTP Basic with client id and secret.
- Token lifetime 3600 s; no refresh token.
- Headers on every request: `x-auth-token`, `x-client-id`.
- Hosts: prelive `prelive-oauth2.quran.foundation` / `apis-prelive.quran.foundation` (data covers **only surahs 1–2**); production `oauth2.quran.foundation` / `apis.quran.foundation/content/api/v4`.
- *"A browser or mobile app must not embed a Content API `client_secret`."*
- No numeric rate limits are published. On 429: exponential backoff with jitter. Docs note that "rate-limit values" are confidential information.

**Key Developer-Terms clauses** (Last updated 2026-10-11) [verified]:
- *"Cache or store QF Content longer than 1 week unless it is obtained and maintained through the Content Sync APIs … Content Sync is the only permitted path for obtaining and maintaining an offline copy … perform a next sync at least every 7 days."*
- *"Prepackaged content. The Content Sync storage exception does not itself authorize distributing a prepackaged database or build-time bundle of QF Content."*
- *"A Developer may cache or bundle font files and Mushaf images obtained through QF APIs or documented CDN URLs … if the Developer maintains an active account in the Developer Console and credits Quran Foundation."*
- *"A Developer may charge for an Application, offer subscriptions or in-app purchases, display advertising, accept donations…"* provided content isn't redistributed as data.
- *"Derived Materials … indexes, embeddings … that include or permit reconstruction of QF Content remain subject to the same restrictions."*
- Breaking changes and deprecations get "typically 30 days" notice. Governing law: New York.
- FAQ attribution for Connected Apps: *"Quran data provided by Quran.Foundation"* plus per-resource credits.

**Content Sync** [verified]:
- Resource groups: `quran_core` (114 chapters, 6,236 verses, **83,665 words**, 30 juz, 60 hizb, 240 rub, with word fields `text_indopak_nastaleeq`, `text_uthmani_tajweed`, `text_imlaei`), `mushafs` (page mappings + positioned words, no fonts), `translations`, `word_by_word_translations`, `word_by_word_transliterations`, `tafsirs`, `recitations`, `chapter_recitations`, `articles`.
- Flow: `GET /resources/sync?bootstrap=true&resources=…`, then `snapshot_url`, then `next_sync_token`.

**Legacy API status:** `api.quran.com/api/v4/*` answered without auth today for `chapters`, `verses/by_{key,chapter,page,juz,hizb,rub_el_hizb,ruku,manzil}`, `quran/verses/indopak`, `quran/translations/{id}`, `resources/{translations,tafsirs,word_by_word_translations}` and `search`. It answered 404 for `pages/lookup` and 503 for `resources/chapter_reciters` [verified]. Treat it as deprecated.

**User APIs** [verified: /docs/user_related_apis_versioned/*, /docs/tutorials/oidc/mobile-apps/android]
- Features: Bookmarks, Reading Bookmarks, Collections, Notes, Goals, Streaks, Reading Sessions, Activity Days, Preferences.
- Auth: OAuth2/OIDC with PKCE through **AppAuth**. *"If you select Frontend or mobile app, use a fully public in-app token exchange flow without a client secret."*
- Scopes need approval; listing as a "Connected App" goes through a 4-gate review (content integrity, security/privacy, compliance, maintenance).
- **Benefit:** two-way bookmark and reading-progress sync with Quran.com. This is a strong Phase-2 feature that needs no backend.

### 2.7 Offline data packaging

#### 2.7.1 Architecture

```
APK assets/
  databases/quran_content.db      # read-only content, built by pipeline, PRAGMA user_version = CONTENT_VERSION
  packs/ur.jalandhari@1.sqlite    # pre-installed default packs (same format as downloadable)
  packs/en.saheeh-qe@1.1.2.sqlite
app data/
  databases/quran_content.db      # copied by Room createFromAsset; replaced (destructive) on CONTENT_VERSION bump
  databases/user.db               # bookmarks, notes, progress, settings – real Room migrations, never destructive
  packs/*.sqlite                  # downloaded packs, each opened by a Room @Database per pack *type*
```

Why separate pack files rather than one merged DB:
- A content-DB upgrade (destructive) never deletes the user's 300 MB of downloads.
- Packs can be deleted independently.
- Pack licenses stay isolated.
- One Room `@Database` class per pack *type* (Translation/Tafsir/WBW) can be opened on any file (`Room.databaseBuilder(ctx, TranslationPackDb::class.java, file.name)`).
- The UI joins translations to ayahs in Kotlin by `ayah_id` (1…6236). Cross-pack search fans out in parallel coroutines and merges.

#### 2.7.2 Build pipeline (`tools/data-pipeline/`, Python 3.12, runs in CI, never on device)

| Step | Script | Does | Key checks |
|---|---|---|---|
| 1 | `fetch.py` | Download pinned sources into `data-src/` (git-ignored): Tanzil `uthmani` + `simple-clean` XML (fixed option set: `marks=true&sajdah=true&rub=false&tatweel=true`), `quran-data.xml`, QuranEnc zips by version, Tanzil translation txt (`?transID=…&type=txt-2`). QUL exports are downloaded by hand after login into `data-src/qul/` | Writes `sources.lock.json` (url, version, retrieved_at, sha256). Fails if a hash changes without `--accept-update` |
| 2 | `validate.py` | Invariants | 114 surahs; 6,236 ayahs; per-surah counts = Tanzil; ayah order; 30 Paras monotonic and covering; 558 rukus contiguous; 7 manzils; sajda 14(+1); 548 pages, every word id exactly once, lines ≤ 16; every translation pack 6,236 non-empty rows; Indo-Pak vs Uthmani normalized-word agreement ≥ 97 % with frozen diff whitelist |
| 3 | `build_content_db.py` | Creates `quran_content.db` **from Room's exported schema JSON** (`app/schemas/…/N.json`, `createSql` per entity) so Room's prepackaged-schema validation passes; sets `PRAGMA user_version`; `VACUUM` | Re-opens and diffs `sqlite_master` against the Room JSON |
| 4 | `build_pack.py` | Builds a translation/tafsir/WBW pack, its `pack_info`, FTS, zip, sha256 | License fields required (build fails if empty) |
| 5 | `gen_test_vectors.py` | Emits `normalization_vectors.json` (input → expected) from the Python normalizer | Kotlin unit tests consume it, so both normalizers stay identical |
| 6 | `publish.py` | Uploads packs + `packs/index.json` to GitHub Releases or Cloudflare R2; signs the index (Ed25519); app pins the public key | Weekly CI job polls QuranEnc `translations/list` `last_update` to meet its "keep updated" condition |

#### 2.7.3 Content DB schema (sketch)

```sql
CREATE TABLE surah(id INTEGER PRIMARY KEY, name_ar TEXT NOT NULL, name_ur TEXT, name_roman TEXT, name_en TEXT,
  meaning_ur TEXT, meaning_en TEXT, revelation TEXT CHECK(revelation IN ('makki','madani')), revelation_order INT,
  ayah_count INT, ruku_count INT, first_ayah_id INT, bismillah_pre INT);
CREATE TABLE ayah(id INTEGER PRIMARY KEY,            -- 1..6236, canonical key everywhere (bookmarks, audio, packs)
  surah_id INT NOT NULL, ayah_no INT NOT NULL,
  text_uthmani TEXT NOT NULL,                        -- Tanzil verbatim
  text_imlaei  TEXT NOT NULL,                        -- Tanzil simple-clean verbatim
  text_indopak TEXT,                                 -- QUL Indo-Pak Nastaleeq (font-bound, contains PUA)
  para_id INT NOT NULL, juz_madani INT NOT NULL, rub_madani INT, manzil_id INT NOT NULL, ruku_id INT NOT NULL,
  UNIQUE(surah_id, ayah_no));
CREATE TABLE para(id INTEGER PRIMARY KEY, name_ar TEXT, name_roman TEXT, start_ayah_id INT, end_ayah_id INT,
  madani_start_ayah_id INT);
CREATE TABLE para_quarter(para_id INT, quarter INT CHECK(quarter IN (1,2,3)), ayah_id INT, PRIMARY KEY(para_id, quarter));
CREATE TABLE manzil(id INTEGER PRIMARY KEY, start_ayah_id INT, end_ayah_id INT);
CREATE TABLE ruku(id INTEGER PRIMARY KEY, surah_id INT, ruku_in_surah INT, para_id INT, ruku_in_para INT,
  start_ayah_id INT, end_ayah_id INT, ayah_count INT);
CREATE TABLE sajda(ayah_id INTEGER PRIMARY KEY, kind TEXT CHECK(kind IN ('hanafi','shafii_only')));
CREATE TABLE word(id INTEGER PRIMARY KEY, ayah_id INT NOT NULL, position INT NOT NULL, text_indopak TEXT, kind TEXT); -- QUL word_index
CREATE TABLE mushaf(id INTEGER PRIMARY KEY, code TEXT UNIQUE, name TEXT, lines INT, pages INT, page_label_offset INT DEFAULT 0, license TEXT);
CREATE TABLE mushaf_line(mushaf_id INT, page INT, line INT, line_type TEXT, is_centered INT,
  first_word_id INT, last_word_id INT, surah_id INT, PRIMARY KEY(mushaf_id, page, line)) WITHOUT ROWID;
CREATE TABLE mushaf_page(mushaf_id INT, page INT, first_ayah_id INT, last_ayah_id INT, para_id INT, manzil_id INT,
  PRIMARY KEY(mushaf_id, page)) WITHOUT ROWID;
CREATE TABLE alias(term_norm TEXT, target_type TEXT, target_id INT);   -- 'yasin'→surah 36, 'ayatulkursi'→ayah 262
CREATE TABLE attribution(dataset_id TEXT PRIMARY KEY, title TEXT, source_url TEXT, license TEXT, version TEXT, notice TEXT);
CREATE TABLE meta(key TEXT PRIMARY KEY, value TEXT);                  -- content_version, built_at, sources.lock hash
CREATE VIRTUAL TABLE ayah_fts USING fts4(content="", imlaei, uthmani, indopak, tokenize=unicode61, prefix="2,3"); -- docid = ayah.id
```

**Translation pack:**

```sql
CREATE TABLE pack_info(key TEXT PRIMARY KEY, value TEXT); -- id, type, lang, dir, title, translator, version, license, attribution, source_url, source_sha256, min_content_version
CREATE TABLE translation(ayah_id INTEGER PRIMARY KEY, text TEXT NOT NULL, footnotes TEXT);
CREATE VIRTUAL TABLE translation_fts USING fts4(content="", text_norm, tokenize=unicode61);
```

- **Tafsir pack:** `tafsir(id PK, from_ayah_id, to_ayah_id, text)` with an index on `(from_ayah_id, to_ayah_id)`. QuranEnc and QF tafsir rows cover verse ranges.
- **WBW pack:** `wbw(ayah_id, position, text, PRIMARY KEY(ayah_id, position))`. Keyed by position, not global word id, so it survives segmentation differences.

`packs/index.json` entry:
```json
{"id":"ur.junagarhi.qe","type":"translation","lang":"ur","dir":"rtl","title":"محمد جوناگڑھی","version":"1.1.3",
 "url":"https://…/ur.junagarhi.qe-1.1.3.sqlite.zip","bytes":2109171,"sha256":"…","license":"QuranEnc republish terms (no modification; cite QuranEnc + version; keep updated; no inappropriate ads)",
 "attribution":"Translated by Muhammad Ibrahim Junagarhi, developed under the supervision of the Rowwad Translation Center — QuranEnc.com v1.1.3",
 "has_footnotes":true,"min_content_version":1}
```

Room integration notes:
- `Room.databaseBuilder(...).createFromAsset("databases/quran_content.db").fallbackToDestructiveMigration(true)` for the content DB only; bump its version whenever content changes.
- `@Fts4(tokenizer = FtsOptions.TOKENIZER_UNICODE61, prefix = [2,3])` entities are supported by Room.
- A contentless FTS (`content=""`) is not expressible in Room annotations. Either declare an `@Fts4` entity whose content is stored, which costs ~+1.5 MB, or query a raw-SQL FTS table via `@RawQuery` / `SupportSQLiteDatabase`. Whether Room tolerates extra non-entity tables in a prepackaged DB is [unverified] and needs a spike test.

#### 2.7.4 Size budget (measured on a prototype built from the real sources)

| Component | Raw | Notes |
|---|---|---|
| Uthmani text (Tanzil) | 1.34 MB | measured |
| Indo-Pak Nastaleeq text | 1.40 MB | measured (QF/QUL text) |
| Imla'i clean text | ~0.9 MB | Tanzil XML size |
| Normalized FTS index (3 Arabic columns) | ~1.3 MB | measured: 0.43 MB for one column's segments |
| Metadata tables | < 0.3 MB | estimate |
| `word` table, 16-line layout (~84k words + 8.8k lines) | ~3 MB | estimate |
| **`quran_content.db` total** | **~9–11 MB** (~3–3.5 MB compressed in APK) | prototype with 1 Arabic FTS column + 2 translations w/o FTS dedup: 16.95 MB raw / 3.29 MB gz |
| Urdu translation pack (text only + FTS) | ~2.2 MB raw / ~0.7 MB zip | Jalandhari text 1.45 MB (gz 0.35 MB) measured |
| Urdu Junagarhi + footnotes pack | 6.2 MB raw / 2.1 MB zip | measured (QuranEnc file) |
| English Saheeh pack | 1.25 MB raw / 0.52 MB zip | measured |
| As-Sa'di Urdu tafsir pack | ~13–15 MB raw / ~3.5–4 MB zip | extrapolated |
| WBW Urdu pack | ~2–3 MB raw / ~0.8 MB zip | estimate |
| Morphology pack | 6.3 MB raw / ~1.5 MB zip | measured text file |

**APK impact:** about 5 MB compressed (content DB + 2 default packs). On-device storage after copy: about 15 MB.

#### 2.7.5 Full-text search design

**Arabic normalizer.** The same spec is implemented in Python (pipeline) and Kotlin (query), locked together by test vectors.

1. Map ZWSP, ZWNJ, ZWJ, LRM, RLM, BOM, NBSP, EN SPACE and EM SPACE to a space. Drop PUA (U+E000–U+F8FF).
2. Drop harakat and Quranic marks: U+0610–061A, U+064B–065F, **U+0670 (dagger alef, dropped, not mapped to alef)**, U+06D6–06ED, U+08D3–08FF. Drop ۞ ۩, Arabic-Indic and Extended digits, and tatweel U+0640.
3. Fold letters:
   - آ أ إ ٱ ٲ ٳ ٵ → ا
   - ى ی ے ئ ي → ي
   - ؤ → و
   - ة ۃ ہ ھ ە → ه
   - ک ڪ → ك
   - ٮ → ب
   - ء ٴ → removed
4. Apply NFKC and collapse whitespace.

**Index:** FTS4 `ayah_fts(imlaei, uthmani, indopak)`, each column the normalized form of its source. A query matches **any** column. Prototype results on all 6,236 ayahs [measured]:

| Query | Hits (any column) | Matching column |
|---|---|---|
| الصلاة | 55 | imla'i |
| الصلوة | 55 | Uthmani |
| السماوات | 180 | imla'i |
| السموت | 180 | Uthmani |
| يأيها الذين آمنوا | 90 | Uthmani only (imla'i writes يا أيها) |
| ابرهيم | 40 | Uthmani |
| إبراهيم | 56 | imla'i |
| مالك يوم الدين | 1 | — |
| ملك يوم الدين | 1 | — |
| إِيَّاكَ نَعْبُدُ | 1 | — (diacritics in the query are stripped) |

Indexing only one column loses 30–100 % of these hits.

**Urdu normalizer** (translations and Urdu UI search):
- Strip harakat (U+064B–065F, U+0670) and ZWNJ.
- Fold ي ى → ی; ك → ک; ه ة ۃ → ہ; أ إ آ → ا; ؤ → و; ئ → ی; Urdu digits → ASCII.
- **Keep ے and ھ distinct**: کی ≠ کے, and بھ ≠ بہ.

**English:**
- Pre-fold diacritics (Allāh → allah, ʿ ʾ removed) and lowercase in Kotlin/Python.
- Use FTS4 `tokenize=porter` for stemming (patience ⇄ patient). Porter is ASCII-only, which is why we pre-fold.
- **Do not rely on `remove_diacritics=2`.** It needs SQLite ≥ 3.27 (API 30+). Android API 24 ships SQLite **3.9**, API 26 3.18, API 30 3.28, API 35 3.44 [verified: developer.android.com android.database.sqlite].

**Roman / alias search:**
- A curated `alias` table for surah names and famous passages: yasin, yaseen, ya-sin, یس → 36; rahman, rehman → 55; mulk, tabarak → 67; kahf → 18; waqia, waqiah → 56; muzammil; ayatul kursi, ayat al kursi, آیت الکرسی → 2:255; last 2 of baqarah → 2:285–286; manzil dua → playlist.
- Folding before alias lookup: `aa→a`, `ee→i`, `oo→u`, drop `'`, `-` and spaces.

**Ranking:** exact phrase > all terms in one ayah > ayah order, computed in Kotlin from `matchinfo('pcx')`. Snippet highlighting is done in Kotlin, because contentless FTS has no `snippet()`.

**FTS5 alternative:** `androidx.sqlite:sqlite-bundled` compiles with `-DSQLITE_ENABLE_FTS5` (and FTS3/4, JSON1, MATH) [verified: androidx sqlite-bundled build.gradle]. It gives bm25 and a trigram tokenizer for substring search, at the cost of about 1 MB of native library per ABI and a driver switch. Not needed for v1.

### 2.8 Dataset catalogue

| Dataset | Source URL | Format | Size | License / terms | Bundle or Download | Notes |
|---|---|---|---|---|---|---|
| Quran text – Uthmani v1.1 | https://tanzil.net/download/ (`/pub/download/index.php?quranType=uthmani&outType=xml&marks=true&sajdah=true&tatweel=true&agree=true`) | XML/TXT/SQL | 1.54 MB XML (0.29 MB gz) | CC BY 3.0; **verbatim only**; credit + link tanzil.net; keep notice | **Bundle** | Canonical key; optional "Usmani" display |
| Quran text – Simple-Clean | same, `quranType=simple-clean` | XML | 0.94 MB | same | **Bundle** | Search column |
| Quran text – Indo-Pak Nastaleeq (word-by-word) | https://qul.tarteel.ai/resources/quran-script (Indopak Nastaleeq WBW) | JSON/SQLite (login) | ~1.4 MB text; ~3 MB with word rows | QUL per-resource (unspecified); commercial use allowed subject to resource license | **Bundle** (after confirming license with QUL) | Requires `indopak-nastaleeq-waqf-lazim-v4.2.1` font (fonts researcher) |
| Metadata skeleton (juz, hizb, manzil, ruku 556, page 604, sajda) | https://tanzil.net/res/text/metadata/quran-data.js (.xml) | JS/XML | 25 KB | CC BY 3.0 | **Bundle** (build input) | Madani juz and pages |
| Ruku 558 set | https://qul.tarteel.ai/resources/quran-metadata/65 | JSON/SQLite (login) | < 50 KB | QUL | **Bundle** | Indo-Pak-aligned (Qasas 9, Luqman 4) |
| Indo-Pak Para table (30) | Hand-curated; verified against QUL layout 11 | CSV in repo | 3 KB | Our own (facts) | **Bundle** | 7 boundaries differ from Madani |
| Para quarter marks (ربع/نصف/ثلٰثة) | Hand-capture from Taj 16-line print | CSV | 2 KB | Our own | **Bundle** | Open question |
| Surah names (ar/ur/roman/en) + meanings | Hand-curated (cross-check Tanzil, QF ur names) | CSV | 15 KB | Our own | **Bundle** | Avoid copying QF payloads |
| Indo-Pak 16-line layout (Taj) | https://qul.tarteel.ai/resources/mushaf-layout/11 | SQLite/DOCX (login) | ~0.3 MB est. | QUL per-resource | **Bundle** | 548 pages; page labels +1? |
| Indo-Pak 15-line / 13-line layouts | QUL layouts 12, 8, 313, 236 | SQLite (login) | ~0.3 MB each | QUL | Download (Phase 2) | Users ask for 13/15-line (02-report) |
| Madani 604 layout | QUL layout 10 / Tanzil pages | SQLite | ~0.3 MB | QUL / CC BY | Download (Phase 2) | |
| Urdu – Jalandhari | https://tanzil.net/trans/?transID=ur.jalandhry&type=txt-2 | TXT (`s\|a\|text`) | 1.45 MB | Tanzil: non-commercial; underlying work very likely PD in PK | **Bundle** (free app) | Default Urdu |
| Urdu – Junagarhi + footnotes | https://quranenc.com/downloads/sqlite/urdu_junagarhi.zip | SQLite (id, sura, aya, translation, footnotes) | 2.1 MB zip / 6.2 MB | QuranEnc republish terms | **Bundle text**; footnotes as Download | Updates required |
| Urdu – Kanz-ul-Iman | https://tanzil.net/trans/ (`ur.kanzuliman`) | TXT | ~1.5 MB | Tanzil n-c; PD work | Download | Barelvi audience |
| Urdu – Maududi / Taqi Usmani / Qadri / Israr / Karam Shah | Tanzil, fawazahmed0, QF | — | ~1.5–1.9 MB | **©, permission needed** | Download after permission | Maslak coverage |
| English – Saheeh International | https://quranenc.com/downloads/sqlite/english_saheeh.zip | SQLite | 0.52 MB zip | QuranEnc terms | **Bundle** | Ayah no. prefixed in text |
| English – Pickthall | https://tanzil.net/trans/ (`en.pickthall`) | TXT | ~0.9 MB | PD work; Tanzil n-c file | Download | |
| English – Hilali-Khan, Rowwad | QuranEnc `english_hilali_khan`, `english_rwwad` | SQLite | ~0.5 MB zip | QuranEnc terms | Download | |
| Hindi / Indonesian / Turkish / Pashto / Punjabi | QuranEnc API `translations/list` | SQLite | 0.4–0.8 MB zip each | QuranEnc terms | Download | |
| Bangla / Sindhi | Tanzil `bn.*`, `sd.amroti` | TXT | ~1–1.5 MB | Tanzil n-c | Download (free app) | |
| English transliteration (ayah) | Tanzil `en.transliteration` | TXT | 1.04 MB | Tanzil n-c | Download | |
| WBW Urdu / English | QUL 93 / 92 (via QuranWBW.com) | JSON/SQLite (login) | ~2–3 MB each | **Unclear; permission needed for publish** | Download (permissioned) | Or QF Content Sync |
| Tafsir summary – Ahsan-ul-Bayan (Urdu) | QuranEnc `urdu_junagarhi` footnotes | SQLite | 4.35 MB raw | QuranEnc terms | Download | Matches the reference app's "tafsir summaries" |
| Tafsir As-Sa'di (Urdu) | https://quranenc.com/api/v1/tafsir/sura/urdu_saadi/{n} | JSON (blocks: sura, from_aya, to_aya, type, text) | ~13 MB text | QuranEnc terms | Download | |
| Tafsir Ibn Kathir (ur/en), Ma'ariful Qur'an, Bayan-ul-Quran, Tafheem | QF ids 160/169/168/159/97; QUL 30/35/34/29 | — | 10–40 MB | **©/unclear** | Permission → Download | |
| Tajweed spans | https://github.com/cpfair/quran-tajweed | JSON | 5.58 MB (0.32 MB gz) | CC BY 4.0 (data) | Phase 3 | Needs regeneration for Tanzil v1.1 |
| Morphology / roots | https://corpus.quran.com/download/ ; github.com/mustafa0x/quran-morphology | TSV | 6.3 MB | GPL + verbatim | Phase 3 download | Root search |
| QF Content (any) | https://apis.quran.foundation/content/api/v4 | JSON | — | QF Terms: ≤7-day cache; Content Sync only; no prepackaged bundle | **Never bundle** | Optional backend-proxied sync later |

---

## 3. Recommendations

1. **Build the base offline DB only from Tanzil (text + metadata), QUL (Indo-Pak script, 16-line layout, ruku 558) and our own hand-curated tables. Never from QF/api.quran.com responses.** The QF terms forbid prepackaged bundles and caching beyond 7 days. Tanzil and QUL allow redistribution with attribution. *Confidence: High.*
2. **Default translations to bundle:** Urdu **Fateh Muhammad Jalandhari** (Tanzil file; work very likely PD) and **Muhammad Junagarhi** (QuranEnc, explicit republish permission; covers the Ahl-e-Hadith audience); English **Saheeh International** (QuranEnc). Offer Kanz-ul-Iman (PD, Barelvi) and Pickthall (PD) as first downloadable packs. Together this covers the main Pakistani maslaks without permission requests. *Confidence: High* for QuranEnc items, *Medium* for Tanzil-sourced files (their non-commercial clause).
3. **Ship the app ad-free in v1, or get Tanzil's written OK before adding ads or paid tiers.** Tanzil-sourced translation files are "non-commercial only", and QuranEnc forbids "inappropriate advertisements". *Confidence: High.*
4. **Make the Indo-Pak Para table (7 differing boundaries) the app's single source of truth for Para indexing, page headers, khatm progress and "play Para" audio ranges.** Keep `juz_madani` only for the optional Madani mode. Users will notice wrong Para starts immediately. *Confidence: High* (verified on the 16-line layout and by GTAF).
5. **Use the QUL/QF 558-ruku set, compute the ع-marker triple against Indo-Pak Paras, and have a person check the 4 disputed rukus against the Taj print.** It matches the Indo-Pak surah ruku counts better than Tanzil's 556. *Confidence: Medium.*
6. **Use the Hanafi 14 sajdas, plus 22:77 flagged "عند الشافعی", and never show Tanzil's obligatory/recommended labels.** *Confidence: High* (data), *Medium* (school attribution).
7. **Display Indo-Pak text from QUL's Indo-Pak Nastaleeq word-by-word export with its matching font. Strip PUA and control characters for copy, share, search and TTS. Store `page_index` and `page_label` separately** (QUL vs QF/print offset). Open a QUL GitHub issue to get the explicit license of the Indo-Pak script and 16-line layout resources before public release. *Confidence: Medium* (the license is the open item).
8. **Implement the build pipeline in Python (fetch → validate → build → pack → test-vectors → publish).** Pin sources with SHA-256 and generate the prepackaged DB from Room's exported schema JSON. That prevents both silent content corruption (Tanzil verbatim rule) and the common Room "invalid prepackaged schema" crash. *Confidence: High.*
9. **Use three database roles: read-only `quran_content.db` (destructive upgrades), `user.db` (real migrations) and independent pack files with a signed `index.json`.** Downloads survive content upgrades, licenses stay isolated per pack, and tiny diffs ship per pack. *Confidence: High.*
10. **Search: FTS4 on the framework SQLite with three normalized Arabic columns, separate Urdu and English normalizers, porter for English, and a curated alias table.** Defer FTS5 and the bundled driver. The prototype found every spelling variant only when all columns were indexed. FTS4 works on minSdk 24 (SQLite 3.9) without a native library. *Confidence: High.*
11. **For WBW, tafsir beyond As-Sa'di/Ahsan-ul-Bayan, and the copyrighted Urdu translations (Maududi, Taqi Usmani, Qadri, Israr):**
    - send permission emails early (they take weeks);
    - meanwhile ship them only in a personal/debug flavor;
    - alternatively, Phase 2: a tiny backend (Cloudflare Worker) holding QF client credentials to run Content Sync for those resources, with the mandatory ≥ weekly re-sync on device.

    *Confidence: Medium.*
12. **Phase 2: integrate QF User APIs through AppAuth/PKCE for two-way bookmark and reading-progress sync with Quran.com** (no backend needed for a public mobile client), and apply for Connected Apps listing. *Confidence: Medium* (scope approval is a review process).
13. **Comply with Punjab's Holy Quran (Printing and Recording) Act 2011 as a precaution:** never show a translation without its Arabic text (s.5(6)), and keep a proofreading sign-off by a qualified person (s.5(3)) in the release checklist. Whether it applies to apps is unclear, but compliance costs little. *Confidence: Medium.*
14. **Defer tajweed colouring and morphology/root search to Phase 3.** The tajweed data needs regenerating against our pinned text and is Madani-only. Morphology is GPL-verbatim. *Confidence: High.*

---

## 4. Risks & open questions

**Risks**
- **QUL licensing ambiguity:** QUL publishes resources whose `permission_to_share` is "unknown". The Indo-Pak script, the 16-line Taj layout and the WBW data may carry third-party rights (Taj Company layout; QuranWBW/Al-Huda WBW).
- **Tanzil "non-commercial" clause** on translation files: ads, IAP or a paid tier would breach it without permission, even for PD works (contract risk rather than copyright risk).
- **QuranEnc conditions:**
  - no modification, so we can't strip the "(n)" prefixes in Saheeh without asking;
  - mandatory updates to the latest version, so a CI job is needed;
  - "notify the source" of errors.
- **QF terms risk** if any developer "just caches" api.quran.com responses into the DB. Enforce with a lint/CI rule that pipeline inputs never include quran.com/QF hosts.
- **Page-label offset** between QUL (index) and QF/printed labels (2:255: p.38 vs p.39). Users quote printed page numbers.
- **Ruku dataset choice** (556 vs 558 vs printed "540") and the ruku-in-para convention may not match the user's physical mushaf.
- **Indo-Pak Para quarter marks:** no dataset exists, so manual capture risks errors.
- **Font-bound PUA text:** if the font ships late or the license fails, `text_indopak_nastaleeq` is unusable. The fallback is KFGQPC Nastaleeq text plus font (rights unclear), or Uthmani.
- **Religious sensitivity:** maslak balance in translator choice. Any typo in Quran text is unacceptable, which is why we pin hashes and run human proofreading QA.
- **GPL data** (QAC morphology) inside a closed-source APK. Keep it as a separately downloaded pack with notices, or align with the app's overall license decision (see 03-open-source report).
- **Data-quality traps in mirrors:** fawazahmed0 Roman-Urdu Junagarhi is off by one verse in Al-Fatiha; cpfair tajweed offsets don't line up with current Tanzil; QF `/juzs` returns 60 rows.

**Open questions**
1. Will QUL (Tarteel) confirm in writing that the Indo-Pak Nastaleeq script, the 16-line Taj layout and the ruku metadata may be bundled in a (possibly monetized) app? Is the Taj Company aware or consenting?
2. What is Fateh Muhammad Jalandhari's exact death year? It settles PD status beyond Pakistan.
3. What exactly are the Indo-Pak ربع/نصف/ثلٰثة positions in the Taj 16-line edition, and do they equal Madani rub-el-hizb 2/4/6 where boundaries coincide?
4. In the Taj 16-line print, do rukus start at 26:52/26:69 or 26:53/26:70, and is there a ruku at 28:83 and 31:31?
5. Is the printed page label = QUL index + 1 throughout, or only after the opening pages?
6. Will Tanzil grant commercial-use permission for its Jalandhari/Kanz-ul-Iman files, or should we commission our own proof-read transcription of these PD works?
7. Who translated the Urdu Ibn Kathir in QF 160 / QUL 30 (Junagarhi → PD?), and who digitized it?
8. Will QuranWBW.com / Al-Huda (Farhat Hashmi) license the Urdu WBW, or should we use QF Content Sync and accept a backend?
9. Does Room accept a prepackaged DB with extra raw-SQL FTS tables that aren't declared as entities? This needs a 1-hour spike.
10. Does the Punjab Holy Quran Act (registration, proofreading certificate) apply to mobile apps distributed from Pakistan? This is for the legal/publishing researcher.

---

## 5. Sources

| URL | What was verified |
|---|---|
| https://tanzil.net/docs/text_license | Verbatim Tanzil text license (CC BY 3.0, no changes, credit + link, keep notice) |
| https://tanzil.net/download/ | Text v1.1 (Feb 2021); terms of use; download options (marks, sajdah, rub, tatweel); form endpoint `/pub/download/index.php` |
| https://tanzil.net/docs/quran_text_types | Six text types (Uthmani, Uthmani-min, Simple, Simple-plain, Simple-min, Simple-clean) |
| https://tanzil.net/docs/quran_metadata · https://tanzil.net/res/text/metadata/quran-data.js | Metadata v1.0, CC BY 3.0; counts (30 juz, 240 quarters, 7 manzil, 556 ruku, 604 pages, 15 sajda with types); per-surah ruku field |
| https://tanzil.net/trans/ · `/trans/?transID=…&type=txt-2` | Translation terms ("non-commercial purposes only"); full list incl. 8 Urdu, 17 English; file format/size; bio links |
| https://api.quran.com/api/v4/… (chapters, verses/by_*, juzs, resources/translations, resources/tafsirs, resources/word_by_word_translations, quran/translations/131, pages/lookup) | Legacy API still answers; 558 rukus; 14 sajdas; Madani juz; translation and tafsir IDs; WBW authors; Clear Quran not public; mushaf param ignored; pages/lookup 404; `/juzs` 60 rows |
| https://api-docs.quran.foundation/docs/quickstart/ | client_credentials, scope=content, 3600 s tokens, headers, hosts, "must not embed client_secret", prelive surahs 1–2 |
| https://api-docs.quran.foundation/legal/developer-terms/ | Terms dated 2026-10-11: 1-week cache, Content Sync only, no prepackaged bundle, fonts/images exception, monetization rules, derived materials |
| https://api-docs.quran.foundation/docs/tutorials/content-sync/getting-started/ | Resource groups; quran_core counts (83,665 words); text_indopak_nastaleeq font requirement; sync flow |
| https://api-docs.quran.foundation/docs/tutorials/fonts/page-layout/ | MUSHAF_IDS (1,2,3,4,5,6,7,11,19); 610/548 pages; 2:255 → p.39 in mushaf 7 |
| https://api-docs.quran.foundation/docs/tutorials/fonts/font-rendering/ | indopak-nastaleeq-waqf-lazim-v4.2.1 pairing; field → font table |
| https://api-docs.quran.foundation/docs/quickstart/migration/ | api.quran.com described as "older unauthenticated API"; migration steps |
| https://api-docs.quran.foundation/docs/tutorials/faq/ · /docs/connected-apps/ · /docs/user_related_apis_versioned/scopes/ · /docs/user_related_apis_versioned/user-related-apis/ · /docs/tutorials/oidc/mobile-apps/android/ | Attribution line; Connected Apps gates; scopes; User API features; PKCE public mobile client via AppAuth |
| https://qul.tarteel.ai/faq · https://qul.tarteel.ai/credits | Commercial use subject to per-resource license; no API; Hafs only; WBW credited to QuranWBW.com |
| https://qul.tarteel.ai/resources/quran-script · /quran-metadata/63–70 · /translation · /tafsir · /transliteration · /morphology · /surah-info | Resource inventories; ruku 558 + JSON schema; login-gated downloads |
| https://qul.tarteel.ai/resources/mushaf-layout/{8,10,11,12,15,19,21,236,313,569,570,571} and `/11?page=N` (≈45 pages) | Layout descriptions (548/610/847/849/604 pages); layout schema; Indo-Pak Para starts at pages 18n−16; manzil and popular-surah pages |
| https://github.com/TarteelAI/quranic-universal-library (cloned) | MIT license; `resource_permission.rb` enums; `allow_publish_sharing?` includes "unknown" |
| https://quranenc.com/en/home/api/ · https://quranenc.com/api/v1/translations/list[/ur,/en,…] · /api/v1/tafsirs/list · /api/v1/tafsir/sura/urdu_saadi/2 · /downloads/sqlite/{urdu_junagarhi,english_saheeh,english_rwwad}.zip | Republish terms (7 conditions); 76 translations; versions; SQLite schema and sizes; As-Sa'di Urdu |
| https://api.alquran.cloud/v1/edition · /v1/meta · /v1/ayah/2:2/quran-tajweed · https://alquran.cloud/terms-and-conditions | 331 editions; 556 rukus / 15 sajdas; tajweed markup; terms (updated 14 Jun 2026) |
| https://cdn.jsdelivr.net/gh/fawazahmed0/quran-api@1/editions.json · /info.json · raw LICENSE · `editions/urd-muhammadjunagar-la/1.json` | 492 editions; Unlicense; metadata mirrors Tanzil; Roman-Urdu off-by-one in Al-Fatiha |
| https://raw.githubusercontent.com/spa5k/tafsir_api/main/README.md (+LICENSE) | MIT code; content sourced from quran.com/QUL/altafsir |
| https://corpus.quran.com/download/ | QAC v0.4 GPL + verbatim terms |
| https://raw.githubusercontent.com/mustafa0x/quran-morphology/master/README.md (+ data file) | QAC fork; 130,030 lines, 6.32 MB; no LICENSE file |
| https://raw.githubusercontent.com/cpfair/quran-tajweed/master/README.md · jsDelivr JSON | CC BY 4.0 data; 2017 Tanzil text dependency; 5.58 MB; misalignment measured |
| https://gtaf.org/support/why-do-the-starting-ayahs-of-some-juz-differ/ | 7 juz with alternative starts; Indo-Pak 36:22 vs Uthmani 36:28 |
| https://en.wikipedia.org/wiki/Ruku_(Quran) | "either 540 or 558 rukus, depending on the authority" |
| https://en.wikipedia.org/wiki/Copyright_law_of_Pakistan | Life + 50 years (s.18) |
| Wikipedia summaries: Ahmed_Raza_Khan_Barelvi, Shabbir_Ahmad_Usmani; search: Muhammad_Junagarhi; wikitia Fateh_Muhammad_Jalandhari | Life dates 1856–1921, 1887–1949, 1890–1941; Jalandhari b. 1864, first published 1900 |
| https://developer.android.com/reference/android/database/sqlite/package-summary | API → SQLite version table (API 24 = 3.9 … API 36.1 = 3.50) |
| https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/sqlite/sqlite-bundled/build.gradle | `-DSQLITE_ENABLE_FTS5`, FTS3/4, JSON1 compile flags |
| https://www.wipo.int/wipolex/en/text/497207 | Punjab Holy Quran (Printing and Recording) Act 2011: s.5(3) proofreading, s.5(6) no translation without Arabic text, Punjab scope |
| Search results: old.quranwbw.com; gittrend marwan/quranwbw | QuranWBW "copying… not allowed"; Urdu WBW credited to Dr Farhat Hashmi (secondary) |

Local measurement artifacts (scratch, not part of the repo): Tanzil downloads, QuranEnc SQLite files, quran.com verse dumps (`qcverses*/`), QUL 16-line page captures (`qul16/`), the prototype DBs (`proto/`) and scripts (`bin/proto_db.py`, `bin/proto_search.py`, `bin/qul_find.py`) under `/tmp/claude-0/-home-user-criterion/eece52d7-90a8-5227-b2fc-70627d430e45/scratchpad/work-r05/`.
