# 03: Open-source Quran codebases (what to reuse, and what the licences mean)

Researcher key: `r03-oss-codebases` · Date: 2026-10-19 · Role: staff Android engineer evaluating code for reuse.

**Method.** I shallow-cloned and read nine repositories, without running any of their build scripts:

- `quran/quran_android` @`4a552ae` (2026-10-18)
- `AlfaazPlus/QuranApp` @`c496013` (2026-10-19)
- `quran/quran-ios` @`5bdd348` (2026-10-17)
- `quran/quran.com-frontend-next` @`aff1a03` (2026-03-03)
- `TarteelAI/quranic-universal-library` @`0aca51e` (2026-10-19)
- `batoulapps/adhan-kotlin` @`e23d143` (2026-08-20)
- `Waqar144/quran_memorization_helper` @`b6470fb` (2026-10-14)
- `quran/mobile-sync` @`0fbc3e9`
- `quran/quran.com-images` @`dbda568`

I also did the following:
- Opened the bundled SQLite databases with Python's `sqlite3`, read only.
- Read the licence strings embedded in font files with `fontTools`.
- Called the live quran_android data API once (the translation list, plus one Urdu translation DB).
- Scraped the QUL resource pages.
- Checked F-Droid listings and star counts on ecosyste.ms.

`quran/quran.com-api` does not exist publicly: the clone asked for credentials. `batoulapps/Adhan2` is not a separate repo; Adhan2 is the KMP artifact published from `batoulapps/adhan-kotlin`, and `adhan-java` redirects to the same repo.

Tags: **[verified: url]** means I fetched or inspected it in this session (for repos, "cloned @hash" means I read the file in the clone). **[unverified]** means it comes from memory. **[inferred]** means my reasoning from verified facts.

> Cross-reference: report 01 found that the reference app's mushaf is a **scanned 16-line Indo-Pak edition of about 548 pages**, not a 13-line one. That changes what we should clone. The QUL "Indopak 16 lines layout (Taj company)" has **548 pages** [verified: https://qul.tarteel.ai/resources/mushaf-layout/11]. In Waqar144's MIT app, the 16-line juz-start offsets fall every 18 pages (Juz 2 at index 19 → p.20, Juz 9 at index 145 → p.146) [verified: cloned `lib/quran_data/pages.dart`]. Report 01 has Juz 2 at p.21 and Juz 9 at p.147, so the reference app looks like the same Taj 16-line edition plus one front page [inferred].

---

## 1. Executive summary

1. **Do not fork `quran/quran_android`. Do a clean-room Kotlin/Compose build and use its code only as a reference.** Five reasons:
   - It is GPL-3.0, and its README explicitly asks for non-profit use only.
   - Its hosted page images are "typically CC BY-NC-ND".
   - Its public repo ships **only the Madani page type**. Naskh, Qaloon, Warsh and Shemerly page modules live in a git-ignored `extras/` folder.
   - It has **no Indo-Pak 16-line or 13-line edition**.
   - Its reading screen is a 2,029-line View/ViewPager `PagerActivity` built on RxJava. Re-skinning that to Compose costs about as much as writing it fresh. (High)
2. **Primary renderer: font-based Indo-Pak pages.** We draw them from **QUL's "Indopak 16 lines layout (Taj company)" (548 pages)** using the **DigitalKhatt IndoPak font, which is OFL-1.1** (licence string read from the font file). The same pipeline also gives 13-line (Taj, 847 pages) and 15-line (610 pages) editions from the same code.
   - The approach is proven: Waqar144's **MIT** Flutter app renders 13/15/16-line Indo-Pak this way, and AlfaazPlus/QuranApp (GPL, Compose) does too.
   - It gives us ayah tap, highlighting, audio follow-along and true night mode. Those are the reference app's #1 unmet asks, which scanned images cannot deliver. (High)
3. **`quran/quran-ios` (QuranEngine) is Apache-2.0** and is the one permissive codebase from the quran.com team.
   - Since April 2026 it has an **`indoPak` reading**: 611 pages, 15-line "line pages" with sidelines and dividers.
   - Its pure-model code can be **ported with attribution** into a closed or monetised Kotlin app: `QuranKit` (Sura/Juz/Hizb/Page model), `LinePageGeometryEngine`, and the IndoPak page tables. (High)
4. **quran_android is the best design reference for audio and downloads.** Read it; do not copy it.
   - `AudioService` runs Media3 ExoPlayer with an LRU stream cache and tracks the ayah by polling the playback position.
   - Gapless timings live in SQLite: `timings(sura, ayah, time[, words])`.
   - Gapped files are per-ayah MP3s named `%03d%03d.mp3`.
   - Downloads use `GappedDownloadStrategy` and `GaplessDownloadStrategy`, and an Android Auto `MediaLibraryService` exists. We should rebuild this on Media3 `MediaSessionService`. (High)
5. **AlfaazPlus/QuranApp (GPL-3.0, Kotlin + Compose + Room + Media3 `MediaLibraryService`) is the closest analogue to our target stack.**
   - Its `quranapp.db` shows a clean schema: `mushafs` with `indopak_13` (847), `indopak_15` (610) and `indopak_16` (548); `mushaf_map` with page, line, type and word ranges; and `ayahs` with **juz, hizb, rub, manzil, ruku** per ayah.
   - Its `QuranTextMeasurer` fits each line by shrinking, with a median scale per page.
   - Use it for ideas only. (High)
6. **QUL (Tarteel) is the data hub: its code is MIT, but each dataset has its own permissions.** QUL tracks `permission_to_host` and `permission_to_share` per resource (`unknown/requested/granted/rejected`), and the resource pages show **no licence text**. We must get written confirmation for the 16-line layout and the IndoPak text before publishing. (High)
7. **Font licences decide what we can ship:**

   | Font | Licence / terms | Verdict |
   |---|---|---|
   | DigitalKhatt IndoPak | **OFL-1.1** | ✅ |
   | KFGQPC fonts | "Use, Copy, Distribute… cannot be Sold, Modified… Reproduced" | ⚠ ship unmodified only; no subsetting |
   | quran.com's IndoPak font ("AlQuran IndoPak by QuranWBW") | "**NOT FOR SALE, NOT FOR MODIFICATION, NOT FOR DISTRIBUTION… WITHOUT WRITTEN NOTICE**" | ❌ |

   All three were read from the font name tables. (High)
8. **Prayer times and Qibla: use `com.batoulapps.adhan:adhan2:0.0.7` (MIT, on Maven Central, released 2026-06-13).** It has `CalculationMethod.KARACHI`, `Madhab.HANAFI` and `Qibla`. At minSdk 24 it needs core-library desugaring (kotlinx-datetime). (High)
9. **No permissive Kotlin/Compose Indo-Pak app exists.** The MIT apps are Flutter: Quran Revision Companion and Aya. The F-Droid Kotlin/Java ones are GPL or AGPL: Noor-Ul-Huda (AGPL-3.0+), My Quran (GPL-3.0-only), Al-Quran Simple (AGPL-3.0-only). We are building the first one. (Medium)
10. **Never hotlink quran.com, quran.app or quranicaudio infrastructure from a monetised app.** quran_android's README says profiting from the project "increase[s] the bandwidth costs" and amounts to "stealing". Self-host everything we redistribute. (High)
11. **`quran.com-frontend-next` has an unclear licence.** `package.json` says MIT, but the `LICENSE.txt` the README links to returns 404 and GitHub detects no licence. Treat it as idea-only. It does confirm the API v4 mushaf IDs (`Indopak=3`, `Indopak15Lines=6`, `Indopak16Lines=7`) and the QCF per-page-font technique. (Medium)
12. **Net effect.** Data and libraries (QUL layouts, DigitalKhatt font, adhan2, Media3) plus ported Apache code (quran-ios) plus ideas from the GPL apps should save about **10–14 engineer-weeks**. The biggest saving is that we never have to hand-build 548–847 pages of Indo-Pak line layouts. (Medium)

---

## 2. Detailed findings

### 2.1 Candidate overview

Stars and push dates come from ecosyste.ms (a cached snapshot) [verified: https://repos.ecosyste.ms/api/v1/hosts/GitHub/repositories/…]. Licences come from the cloned LICENSE files.

| Repo | SPDX | Last commit | ★ / forks | Stack | Indo-Pak? | Verdict |
|---|---|---|---|---|---|---|
| quran/quran_android | **GPL-3.0** | 2026-10-18 | 2,416 / 1,010 | Kotlin + Java, View/ViewPager reader with Compose islands (86 of 728 .kt files use `@Composable`), Metro DI, RxJava3 + coroutines, SQLDelight, Media3 1.11.1, OkHttp/Retrofit/Moshi; minSdk 24, compileSdk 37 | No. Only Madani is public; Naskh is SHL-licensed images in private `extras/` | **Reference only** |
| AlfaazPlus/QuranApp | **GPL-3.0** | 2026-10-19 | 395 / 60 | Kotlin + Compose (169 files), Room, DataStore, Media3 1.5.1 plus legacy ExoPlayer 2.19.1, Retrofit; minSdk 24, target 36 | **Yes.** `dk_indopak` script; 13 (847), 15 (610) and 16 (548) line maps in its DB (13-line map rows not yet populated) | **Reference only** (best UI and data-model reference) |
| quran/quran-ios (QuranEngine) | **Apache-2.0** | 2026-10-17 | 594 / 207 | Swift, SPM, GRDB, modular Model/Data/Domain/Features | **Yes.** `Reading.indoPak`, 611 pages, 15-line line-images | **Port code with attribution** |
| quran/quran.com-frontend-next | **Unclear** (package.json "MIT", no LICENSE file) | 2026-03-03 | 1,927 / 631 | Next.js/TS | Yes, via API (`mushaf=6/7`) using the QuranWBW IndoPak font (restrictive) | Ideas only |
| TarteelAI/quranic-universal-library | **MIT** (code) | 2026-10-19 | 1,058 / 135 | Rails CMS + exporters | **Yes.** Layouts: IndoPak 9 (Gaba), 13 Taj (847), 13 Qudratullah (849), 15 Qudratullah (610), 16 Taj (548) | **Use data** (per-resource permission) |
| batoulapps/adhan-kotlin (Adhan2) | **MIT** | 2026-08-20 | 222 / 59 | Kotlin Multiplatform, no deps beyond kotlinx-datetime | n/a | **Use library** |
| Waqar144/quran_memorization_helper ("Quran Revision Companion") | **MIT** | 2026-10-14 | 46 / 9 | Flutter/Dart | **Yes.** 13/15/16-line Indo-Pak with the DigitalKhatt IndoPak font | **Port code and data (MIT)**, after checking data provenance |
| quran/mobile-sync | GPL-3.0 (POM `project-license-name`) | 2026-10-16 | n/a | KMP, SQLDelight, OIDC | n/a | Skip (tied to quran.com accounts) |
| quran/quran.com-images | GPL (README: "code is copyleft GPL"; KFGQPC owns fonts and pages) | 2026-02-21 | 490 / 142 | Perl + MySQL image generator | No | Skip |
| mirfatif/NoorUlHuda | AGPL-3.0+ | 2026-09-01 | 104 / 15 | Java | No | Skip |

### 2.2 `quran/quran_android`: architecture and reference notes

**Licence and the authors' wishes.** `LICENSE` is the GNU GPL v3 text [verified: cloned @4a552ae]. The README says:

> "people planning on taking this project and profiting from it (by way of ads, in app purchases, etc) are in fact stealing from the work of the contributors of this project, and from the people who volunteer to pay for the servers … Please keep use of this code for non-profit purposes only. … the data is licensed under the various licenses of the data's authors (typically, this is CC BY-NC-ND …)" [verified: https://raw.githubusercontent.com/quran/quran_android/main/README.md via clone]

**Module map** (`settings.gradle.kts`) [verified: clone]:

| Area | Modules / key files |
|---|---|
| App shell | `:app` (flavours by `pageType`: only `madani` is in OSS; `app/src/{naskh,qaloon,warsh}` source sets exist but their `:pages:*` modules sit in git-ignored `extras/`, see `.gitignore` → `extras`) |
| Page data | `:pages:madani` → `MadaniPageProvider` (base URL `https://android.quran.com/data`, image version 8, ayahinfo at `/databases/ayahinfo/`); `:pages:data:madani` → `MadaniDataSource` (604 pages; sura/juz/quarter arrays; **`manzilPageArray = emptyArray()`**) |
| Core model | `common/data/.../QuranDataSource.kt` (interface: `numberOfPages`, `pageForSuraArray`, `pageForJuzArray`, `quarterStartByPage`, `manzilPageArray`, `haveSidelines`, `pagesToSkip`); `QuranInfo.kt` (`manzilForPage`, `getJuzFromPage`, `getRub3FromPage`, `getPageFromSuraAyah`, dual-page mapping) |
| Image page rendering | `app/.../view/HighlightingImageView.java`, `HighlightsDrawer.kt`, `data/AyahInfoDatabaseHandler.java`, `GlyphsBuilder.kt`, `ui/util/ImageAyahUtils.getAyahFromCoordinates()`, `presenter/quran/ayahtracker/AyahImageTrackerItem.kt` |
| Line-by-line (Compose) renderer | `common/linebyline/{data,ui}`, `feature/linebyline` (`QuranLineByLinePresenter`, `QuranPage.kt`, `QuranLine.kt`, `Sidelines.kt`, `SelectionHelper.kt`) |
| Audio | `app/.../service/AudioService.kt` (1,412 lines), `database/SuraTimingDatabaseHandler.kt`, `common/audio` (`QariUtil`, `AudioRequest`, gapless/gapped info commands), `feature/audio` (`AudioUpdater`, MD5 checks, `/data/audio_updates.php`) |
| Downloads | `service/QuranDownloadService.kt`, `service/download/{Gapped,Gapless,SingleFile}DownloadStrategy.kt`, `feature/downloadmanager` (Compose sheikh download UI) |
| Android Auto | `feature/autoquran/.../QuranBrowsableAudioPlaybackService : MediaLibraryService` |
| Bookmarks | `common/bookmark/src/main/sqldelight/.../{Bookmark,Tag,BookmarkTag,LastPage}.sq`; `feature/ayahbookmark` (reading-bookmark sheet in Compose) |
| Translations / search | `common/translation/.../translations.sq` (list of installed translations); `app/.../database/DatabaseHandler.kt` (tables `verses`, `arabic_text`, `share_text`, `properties`); `common/search/{DefaultSearcher,ArabicSearcher}.kt` (FTS `MATCH` with a `LIKE` fallback, Arabic normalisation in `ArabicCharacterHelper.kt`) |
| Sync | `feature/sync` plus `com.quran:mobile-sync` 0.1.36 (GPL, quran.com OAuth) |
| Widgets | `widget/BookmarksWidget.kt` |
| Build | `build-logic/convention` (Android app, library and Compose convention plugins); `AndroidCommon.kt` sets compileSdk 37, minSdk 24, Java 17 |

**How image pages and ayah tapping work.** Each page is a PNG (one per screen-width bucket). A per-width SQLite file, "ayahinfo", holds glyph bounding boxes [verified: `AyahInfoDatabaseHandler.java`]:

```sql
-- ayahinfo_<width>.db  (columns read by AyahInfoDatabaseHandler)
glyphs(page_number, line_number, sura_number, ayah_number, position,
       min_x, min_y, max_x, max_y, glyph_type)   -- glyph_type: word / ayah marker / ...
```

Tapping maps the touch point through the image matrix and runs `ImageAyahUtils.getAyahFromCoordinates(bounds, x, y)`. Highlights (selection, audio, bookmark) are rectangles drawn over the bitmap by `HighlightsDrawer`. This only works if someone has measured the glyph boxes for **every page image**. For scanned Indo-Pak pages nobody has published that data, which is why the reference app cannot highlight ayahs.

**Line-by-line renderer (Compose).** The newer quran.com page types use one transparent PNG per line, stored at `<pageDir>/<page>/<line>.png` plus `<page>/sidelines/*.png`. Positions come from a SQLDelight DB with **normalised** horizontal coordinates [verified: `common/linebyline/data/src/main/sqldelight/...`]:

```sql
ayah_glyphs(id, page, line, sura, ayah, glyph_position, left REAL, right REAL, glyph_type)
ayah_highlights(ayah_id, page, sura, ayah, line, left REAL, right REAL)
ayah_markers(ayah_id, page, sura, ayah, line, code_point TEXT, center_x REAL, center_y REAL)
sura_headers(sura, page, line, center_x REAL, center_y REAL)
```

This is a very good **pattern**: it scales cleanly at any width and keeps highlight geometry per line. It is the right fallback if we ever license Indo-Pak line images.

**Audio** [verified: `AudioService.kt`, `SuraTimingDatabaseHandler.kt`, `GappedDownloadStrategy.kt`, `AudioUtils.kt`, `MadaniPageProvider.kt`]:

- The engine is a Media3 `ExoPlayer` inside a classic `Service` with a `MediaSessionCompat` (legacy session). It streams through `CacheDataSource` and a `SimpleCache` with `LeastRecentlyUsedCacheEvictor`, and runs on a `HandlerThread`.
- **Gapless qaris** use one MP3 per sura plus a timing DB:

  ```sql
  timings(sura INTEGER, ayah INTEGER, time INTEGER /*ms*/, words TEXT /*schema_version>1*/)
  properties(property, value)  -- version, schema_version
  ```

  `updateAudioPlayPosition()` polls the position to find the current ayah. `getSeekPosition()` and `playAudio(…, timings)` handle range start and end, ayah and range repeat, and an optional word highlight.
- **Gapped qaris** use per-ayah files from a URL format `…/%03d%03d.mp3` (sura+ayah, everyayah-style), plus an extra basmala file. An `AudioRequest(start, end, qari, repeatInfo, rangeRepeatInfo, enforceBounds, playbackSpeed, shouldStream, …)` models exactly the owner's "surah / para / verse range" requirement.
- The public qari list is only three entries, hosted on `download.quranicaudio.com` and `mirrors.quranicaudio.com/everyayah`. The full list is in private `extras`.

**Live data API** [verified: `https://quran.app/data/translations.php?v=5`]:
- It returns 79 entries, including Urdu (Junagarhi #90, Maududi #91, Bayan-ul-Quran/Israr Ahmad #121), Hindi, Pashto, Bengali, 9 English and 16 Arabic tafsirs. Files are served from `android.quran.com/data/getTranslation.php?id=…`.
- A downloaded file (`quran.urdu.junagarhi.db`) is an **FTS3 virtual table** `verses(sura, ayah, text)` plus `properties(schema_version=5, text_version=16)`. Footnotes are inline `[[…]]` [verified: downloaded and inspected].
- The data licence is per source (QuranEnc, KSU, Tanzil per the README). **We must not use this endpoint in our app.**

**Page-type preview server.** `https://quran.app/data/pagetypes/snips/<type>.png` redirects to `files.quran.app/preview/`. `madani` and `shemerly` exist; `naskh`, `qaloon`, `warsh` and `indopak` return 404 [verified: curl].

**Quality.** The codebase is high quality: it is tested (Robolectric, Turbine, Truth, Kover), lint-strict, and sensibly modularised. It also carries 15 years of history: a View-based pager, RxJava next to coroutines, Java leftovers (35 .java files), and a legacy media session. It is **not** a clean Compose base to fork.

### 2.3 `AlfaazPlus/QuranApp`: the closest modern-stack analogue (GPL-3.0)

[verified: cloned @c496013]

- **Rendering.** `compose/components/reader/Mushaf.kt` uses a `HorizontalPager` of pages. Each page is a list of `QuranPageLineItem` (`Title`, `Bismillah`, `Text(words, centered, layout)`). `utils/reader/QuranTextMeasurer.kt` does two things:
  - It computes a **page-wide font scale**: the median of `contentWidth/measuredWidth` over "wide" lines (fill ratio ≥ 0.82).
  - It **shrinks individual lines** that overflow and pads centred lines with gaps.

  Text is laid out with `rememberTextMeasurer(cacheSize = 2048)`. KFQPC per-page fonts are kept in a small LRU (`FontResolver.kt`).
- **Scripts** (`QuranScriptUtils`): `uthmani`, `dk_indopak` (Digital Khatt IndoPak), `nastaleeq_indopak`, `kfqpc_v1`, `kfqpc_v2`, `kfqpc_v4_tajweed`, with variants `INDOPAK_15` and `INDOPAK_16`.
- **Bundled DB** `assets/db/quranapp.db` (27 MB) [verified: sqlite3]:

  ```sql
  surahs(surah_no, ayah_count, revelation_order, rukus_count, revelation_type)
  ayahs(ayah_id /*sura*1000+ayah*/, surah_no, ayah_no, juz_no, hizb_no, rub_no,
        manzil_no /*1..7*/, ruku_no /*1..558*/, sajdah_type)
  ayah_words(ayah_id, script_id, word_index, text)          -- 4 scripts × 83,665 words
  mushafs(mushaf_id, mushaf_code, no_of_pages, lines_per_page)
     -- (1,'qpc',604,15) (2,'indopak_13',847,13) (3,'indopak_15',610,15) (4,'indopak_16',548,16) (5,'kfqpc_v1',604,15)
  mushaf_map(mushaf_id, page_number, line_number, line_type /*surah_name|ayah|basmallah*/,
             is_centered, start_ayah_id, start_word_index, end_ayah_id, end_word_index, surah_no)
  navigation_ranges(type /*juz|hizb|rub|manzil*/, unit_no, surah_no, start_ayah, end_ayah)
  arabic_search USING fts4(ayah_id UNINDEXED, text, tokenize=unicode61)
  similar_verses(...), mutashabihat_phrases(...), surah_search_aliases(+fts4)
  ```

  The row counts are 9,131 for indopak_15 and 8,742 for indopak_16. indopak_13 has **0 rows** so far: it is declared but not populated.
- **Audio.** `utils/mediaplayer/RecitationService : MediaLibraryService` (Media3). The catalogue (`inventory/recitations/available_recitations_info_v2.json`) uses `url_template` values like `https://download.quranicaudio.com/qdc/abdul_baset/mujawwad/{chapNo:%01d}.mp3` plus gzipped timing JSON. It has no DI framework (manual wiring), Room plus kapt, and Glance widgets.
- **Credits** (README): QuranEnc, Tanzil, quran.com, qurandb.com, tarteel.ai, quranicaudio.com, everyayah.com.
- **Verdict.** It is the best reference for the **data model** (manzil and ruku per ayah, the mushaf_map shape) and the **Compose line-fitting approach**. Because it is GPL, copy nothing verbatim.

### 2.4 `quran/quran-ios` (QuranEngine): Apache-2.0, portable

[verified: cloned @5bdd348; LICENSE = Apache License 2.0]

- **Layering worth mirroring in Kotlin:** `Model/` (QuranKit, QuranGeometry, QuranAudio, QuranText, QuranAnnotations), `Data/` (SQLite, GRDB persistences, BatchDownloader), `Domain/` (QueuePlayer, QuranAudioKit, ReadingService, AudioTimingService, WordFrameService), `Features/` (QuranView, Search, Bookmarks, ReciterList, AudioDownloads, …), and `UI/` (NoorUI design system).
- **`Model/QuranKit/Sources/Reading.swift`** defines `case indoPak = 6`:
  - `linePageMetrics = .indoPakLinePages` (15 lines, width parameter 1342, line-height ratio 148/1342, no overlap)
  - `usesLinePageDividers` and `usesLinePageSidelines` are true
  - `usesInvertedQuranImageRenderingInDarkMode` is true
  - `supportsWordPositions` is false
- **`IndoPakQuranReadingInfoRawData.swift`** has `numberOfPages = 611`, `pagesToSkip = 1`, plus start-page and start-sura arrays. The file header says "Created by OpenAI on 2026-04-26".
- **`QuranGeometry/LinePageGeometryEngine(+Sidelines).swift`** (520 lines) holds the line-page layout maths, which is portable to Compose.
- **Gaps:** there is no manzil or ruku model in QuranKit (grep found none). The IndoPak line images come from quran.com servers under unknown terms, so do not use them.
- **Apache-2.0 obligations if we port code:** keep the copyright and licence notice for the ported files, state our changes, and ship the licence text in the in-app "Open-source licences" screen. The repo has no NOTICE file. Porting does **not** force us to open our own code.

### 2.5 `quran/quran.com-frontend-next`: how QCF fonts and line layouts work (idea-only)

[verified: cloned @aff1a03]

- `types/QuranReader.ts` defines:
  - `QuranFont`: `code_v1`, `code_v2`, `tajweed_v4`, `text_uthmani`, `text_indopak`, `qpc_uthmani_hafs`
  - `Mushaf`: `QCFV2=1, QCFV1=2, Indopak=3, UthmaniHafs=4, KFGQPCHAFS=5, Indopak15Lines=6, Indopak16Lines=7, Tajweed=11, QCFTajweedV4=19`
- `src/utils/api.ts#getMushafId` maps `text_indopak` plus `15_lines`/`16_lines` to mushaf 6 or 7.
- **QCF technique** (`src/utils/fontFaceHelper.ts`): each of the 604 pages has its own font (`/fonts/quran/hafs/v1|v2/woff2/p{N}.woff2`, family `QCF_P###` or `QCF2###`). The API returns per-word `code_v1` glyph codes and `line_number`, and `ReadingView/groupLinesByVerses.ts` groups words by `Page{n}-Line{m}`. Each line is a flex row of glyph strings.
- **IndoPak on the web** uses `indopak-nastaleeq-waqf-lazim-v4.2.1.woff2`. Its name table reads "AlQuran IndoPak by QuranWBW … **NOT FOR SALE, NOT FOR MODIFICATION, NOT FOR DISTRIBUTION OR NOT FOR DEVELOPMENT WITHOUT WRITTEN NOTICE BY QURANWBW.COM**" [verified: fontTools]. **Do not use it.**
- QCF v1 page fonts say "King Fahad Complex, All rights reserved" [verified: `p2.ttf` name table].

### 2.6 QUL, the Quranic Universal Library (Tarteel): data and tooling

- **Code licence:** MIT ("Copyright (c) 2024-present Tarteel, Inc") [verified: cloned LICENSE].
- **Layouts available** [verified: https://qul.tarteel.ai/resources/mushaf-layout and each `/mushaf-layout/{id}` page]:

| ID | Layout | Pages | Formats |
|---|---|---|---|
| 11 | **Indopak 16 lines (Taj company)** | **548** | sqlite, docx |
| 313 | **Indopak 13 lines (Taj company, "based in Lahore Pakistan")** | **847** | sqlite, docx |
| 236 | Indopak 13 lines (Qudratullah) | 849 | sqlite, docx |
| 12 | Indopak 15 lines (Qudratullah) | 610 | json, sqlite, docx, images |
| 8 | KFGQPC Nastaleeq 15 lines | 610 | sqlite, docx |
| 571 | Indopak 9 lines (Gaba) | n/a | sqlite, docx |
| 15/10/19/21 | KFGQPC V1 (1405H) / V2 (1421H) / V4 (1441H) / DigitalKhatt | 604 | sqlite, docx |

- **Export schema** [verified: `lib/exporter/export_mushaf_layout.rb`]:

  ```sql
  pages(page_number INTEGER, line_number INTEGER, line_type TEXT /*ayah|surah_name|basmallah*/,
        is_centered INTEGER, first_word_id INTEGER, last_word_id INTEGER, surah_number INTEGER)
  info(name TEXT, number_of_pages INTEGER, lines_per_page INTEGER, font_name TEXT)
  ```

  A JSON variant writes one file per page plus `info.json` (issue #257). Word IDs index into a separate QUL **word-by-word script** dataset (e.g. "Indopak Nastaleeq script – Word by Word"). The layout, the text and the font must be the **same family**, or words will not line up.
- **Fonts listed** [verified: https://qul.tarteel.ai/resources/font]: 26 fonts, including **Digital Khatt Indopak font** (otf), KFGQPC Nastaleeq, Indopak Nastaleeq, Surah-name and Juz-name header fonts, and QPC V1/V2/V4.
- **Permissions model** [verified: `app/models/resource_permission.rb`]: `permission_to_host` and `permission_to_share` take the values `unknown | requested | granted | rejected`, alongside `copyright_notice` and `source_info`. The FAQ only says "Check repository license terms and dataset-specific licensing details before production use" [verified: https://qul.tarteel.ai/docs/faq]. **No per-resource licence is shown on the pages**, so we must ask.
- Other useful QUL datasets: mutashabihat, surah info, audio segments (ayah/word timings), and morphology.

### 2.7 Waqar144 "Quran Revision Companion": the permissive Indo-Pak proof

[verified: cloned @b6470fb; LICENSE: MIT, © 2023 Waqar Ahmed; F-Droid listing says MIT, v1.8.2, Jun 24 2026 — https://f-droid.org/en/packages/com.wqar.quran_mem_helper/]

- **Layouts as Dart constants:**

  | File | Pages | Lines |
  |---|---|---|
  | `thirteen_line_indopak_layout.dart` | 847 | 12,693 |
  | `fifteen_line_indopak_layout.dart` | 610 | 10,355 |
  | `sixteen_line_indopak_layout.dart` | 548 | 9,844 |

  Each page is `Page(n, [Line(ayahIdx, wordStartIdx), …])`; `-1` and `-999` are header and basmala sentinels.
- **Supporting data:**
  - `rukus.dart` maps each ayah to its **para-ruku and surah-ruku** indices (this drives the margin "ع" markers).
  - `para_bounds.dart` and `pages.dart` hold the para start pages per edition (16-line: every 18 pages).
  - `text_indopak.dart` holds 6,236 ayahs with **ZWNJ (U+200C) as the word separator** and U+06DD end-of-ayah.
- **Fonts** (`fonts/`): `DigitalKhattIndoPak.otf`, `Digital Khatt V2 Font.otf`, `Mehr_Nastaliq_Web v2.0.ttf` (Urdu translation), `surah_names.ttf`, `bismillah.ttf`.
- **Justification:** `FittedBox(scaleDown)` plus a computed `wordSpacing` per line (`widgets/read_quran.dart`). The commit log notes that "Many other lines are also small, especially in the 13 line mushaf", a known quality issue.
- **Provenance:** the commits don't name a source ("Add 13 line mushaf" 2025-05-19, "Update layouts" 2026-05-18). The page counts match QUL's Taj 13/16 and Qudratullah 15 layouts exactly, so they are probably QUL-derived [inferred]. MIT covers Waqar's code, **not necessarily the upstream data**, so source the data from QUL directly and use Waqar's files only to cross-check.

### 2.8 Fonts: licences read from the font files

[verified: fontTools name table, IDs 0/13]

| Font | Name-table licence | Use in a monetised app |
|---|---|---|
| **DigitalKhatt IndoPak** v0.1 ("Copyright (c) 2024 Amine Anane, Copyright © 2024 Tarteel Inc.") | "licensed under the **SIL Open Font License, Version 1.1**" | ✅ Bundle it, subset it, and convert it freely; keep the OFL text. You may not sell the font by itself. |
| DigitalKhatt New Madina (V2) | OFL-1.1 | ✅ (Madani option) |
| KFGQPC HAFS Uthmanic Script v2.2 | "rights to Use, Copy, Distribute … The Font Software cannot be Sold, **Modified**, Altered … Reproduced" | ⚠ Ship it unmodified only (no subsetting or WOFF conversion); bundling in a paid app is a grey zone |
| KFGQPC Uthman Taha Naskh | same KFGQPC EULA | ⚠ same as above |
| QCF V1 page fonts (`p2.ttf`) | "King Fahad Complex, All rights reserved" | ⚠ Avoid unless we get permission |
| AlQuran IndoPak by QuranWBW 4.2.1 | "NOT FOR SALE, NOT FOR MODIFICATION, NOT FOR DISTRIBUTION OR NOT FOR DEVELOPMENT WITHOUT WRITTEN NOTICE" | ❌ |

### 2.9 Adhan2 (prayer times and Qibla)

[verified: cloned README, `CalculationMethod.kt`; Maven metadata https://repo1.maven.org/maven2/com/batoulapps/adhan/adhan2/maven-metadata.xml → latest 0.0.7, lastUpdated 2026-06-13]

```kotlin
// libs.versions.toml: adhan2 = "0.0.7"  →  implementation("com.batoulapps.adhan:adhan2:0.0.7")
val params = CalculationMethod.KARACHI.parameters.copy(madhab = Madhab.HANAFI)
val times = PrayerTimes(Coordinates(31.5204, 74.3587), DateComponents(2026, 10, 19), params)
val qibla = Qibla(Coordinates(31.5204, 74.3587)).direction  // degrees from true north
```

The README says that on Android kotlinx-datetime "needs either a minimum api level of 26, or enabling of `coreLibraryDesugaring`". Our minSdk is 24, so enable desugaring. The old Java artifact `com.batoulapps.adhan:adhan` stops at 1.2.1 (2021).

### 2.10 Other Quran apps on F-Droid

[verified: https://search.f-droid.org/?q=quran&lang=en plus the package pages]

| App | Licence | Stack | Relevance |
|---|---|---|---|
| Quran Revision Companion (`com.wqar.quran_mem_helper`) | MIT | Flutter | 13/15/16-line Indo-Pak (see §2.7) |
| Aya (`com.quran.aya`, src `Cancelllls/Aya`) | MIT | Flutter | 10 qira'at, prayer times, azkar, FTS5 hadith; v1.4.0 (2026-10-12). Feature reference only |
| Noor-Ul-Huda (`com.mirfatif.noorulhuda`) | AGPL-3.0+ | Java | Page mode, prayer times; v1.03 (2023). Skip |
| My Quran (`com.dmouayad.my_quran`) | GPL-3.0-only | multiplatform | Skip |
| Al-Quran Simple (`com.github.meypod.al_quran`) | AGPL-3.0-only | n/a | Skip |

I found no Kotlin + Compose Quran app under a permissive licence with Indo-Pak support, either on F-Droid or through web search (see §5).

### 2.11 Licensing strategy analysis

| Option | What it means | Monetisation | Proprietary SDKs (AdMob, Play Billing, Firebase) | iOS later | Effort | Ethics / reputation |
|---|---|---|---|---|---|---|
| **A. Fork quran_android and re-skin** | The whole app becomes **GPL-3.0**: every release must offer Corresponding Source; no added restrictions (GPLv3 §10: "You may not impose any further restrictions") | Legally allowed ("You may charge any price or no price", GPLv3 §4), but anyone can rebuild it without ads or paywall | **Problematic.** Corresponding Source "includes … the source code for shared libraries and dynamically linked subprograms that the work is specifically designed to require" (GPLv3 §1); closed SDKs are not System Libraries, and we cannot grant a §7 exception for upstream code we don't own | GPL vs App Store terms is a known conflict [unverified]; quran-ios is Apache, but the fork isn't | High: legacy View/Rx code, IndoPak missing, Madani-only data | **Bad.** It goes against the authors' explicit non-profit request and uses their data servers |
| **B. Clean-room Kotlin/Compose** (recommended) | Our own code under any licence we choose; GPL repos are read for ideas only (no copying or line-by-line transliteration); facts and data from permissive or permissioned sources | Unrestricted (subject to data licences) | No conflict | Free choice (KMP later) | Medium, and offset by QUL data plus Apache ports | Clean |
| **C. Voluntarily GPL our app and borrow from quran_android/QuranApp** | Fine for a non-commercial or donation-only app | Weakened (see A) | Same SDK problem | Same as A | Medium | OK if non-profit |
| **D. Port Apache-2.0 quran-ios pieces into B** | Attribution plus licence text only | Unrestricted | No conflict | Ideal | Saves work on model and geometry | Clean |

Two more factors apply to every option:
- **Data dominates.** quran_android's own page data is "typically CC BY-NC-ND", so even a perfect GPL fork could not legally sell those pages.
- **Reputation is commercially material.** The audience is religious and community-minded, and the GPL authors publicly call commercial reuse "stealing". That risk matters for a Pakistani consumer brand.

**Firm recommendation: B + D.** Write our own Kotlin/Compose code, keep it proprietary (or Apache-2.0 if the owner later wants to open-source it), port selected Apache-2.0 quran-ios model and geometry code with attribution, use MIT/OFL libraries and fonts, and source data from QUL and other permissioned sources. Keep a clean-room log: an ADR plus a "sources consulted" note per module, and no GPL file is ever opened in the IDE project. This is not legal advice; get a one-hour review from a lawyer before launch.

---

## 3. Recommendations

1. **Adopt clean-room plus permissive-port (B + D) as the licensing policy, and record it in an ADR.** Allowed:
   - Apache-2.0, MIT, BSD and OFL code and fonts, with attribution.
   - Reading GPL code for ideas, never copying it.

   Banned: quran.com, quran.app and quranicaudio endpoints in production, and fonts or data that say "not for distribution" or "non-commercial". *Rationale:* this keeps every monetisation path open (ads, IAP, subscriptions), avoids the GPL vs proprietary SDK conflict, respects the quran_android authors' wishes, and keeps iOS open. **Confidence: High.**

2. **Build the mushaf as a font-rendered, layout-driven Compose pager, not as scanned images. Default to "Indo-Pak 16-line (Taj), 548 pages" and offer 13-line Taj (847) and 15-line (610) as switchable layouts.**
   - *Data:* QUL layout SQLite (`pages`, `info`), the matching QUL IndoPak word-by-word text, and the DigitalKhatt IndoPak font (OFL).
   - *Algorithm*, an idea taken from QuranApp and Waqar144 and implemented fresh:
     1. Load the page's lines.
     2. Build each line's words from the word-ID range.
     3. Measure with `TextMeasurer`.
     4. Take a page-wide scale (median fit of the full lines).
     5. Justify each non-centred line by distributing the extra width into the inter-word gaps (fall back to shrinking).
     6. Centre the `surah_name` and `basmallah` lines.
     7. Draw the ornamental frame, the ruled lines (16-line look), margin ruku "ع" markers (from the ruku table) and juz/manzil labels in a `Canvas` layer.
   - *Why:* it matches the reference edition, is about 1–3 MB instead of hundreds of MB of images, gives per-ayah and per-word hit-testing from `TextLayoutResult.getBoundingBox`, gives true night mode and crisp zoom, and needs no image licence.
   - **Confidence: High** on the approach; **Medium** on visual fidelity, so prototype pages 1, 2, 3 and one dense page in week 1.

3. **Model the indices on the QuranApp and QUL shape, written fresh:**
   - `ayahs(ayah_id, sura, ayah, juz, hizb, rub, manzil, ruku_in_para, ruku_in_surah, sajdah)`
   - `mushaf_pages(mushaf_id, page, line, type, centered, first_word_id, last_word_id, sura)`
   - `nav_ranges(type, unit, sura, start_ayah, end_ayah)`

   Para names (Alif Lam Meem, Sayaqool, …) and the 7 Manzils come from this table, so they work for every edition. *Rationale:* manzil and ruku per ayah is what makes the "Parah / Surah / Manzil" indexing and the margin ع marks trivial. quran_android leaves `manzilPageArray` empty for Madani, and quran-ios has no manzil support. **Confidence: High.**

4. **Port QuranKit-style domain types from quran-ios (Apache-2.0) into a pure-Kotlin `:core:model` module:** `Sura`, `Juz`, `Hizb`, `Quarter`, `Page`, `AyahNumber`, the `LastAyahFinder` family, and the `QuranReadingInfoRawData` pattern (one raw-data object per mushaf). Add `Manzil` and `Ruku`. Keep Apache headers on the ported files. *Rationale:* this is battle-tested navigation logic under a permissive licence and saves about 1–2 weeks. **Confidence: High.**

5. **Audio: Media3 `MediaSessionService` (or `MediaLibraryService` for Android Auto) with an `AudioRequest(start, end, qari, ayahRepeat, rangeRepeat, speed, stream)` model.**
   - Support both **gapless** (per-sura MP3 plus `timings(sura, ayah, time_ms[, words])` SQLite; seek and clip with `ClippingConfiguration`, or poll position as quran_android does) and **gapped** (per-ayah `SSSAAA.mp3` playlist).
   - Cache streams with `SimpleCache` plus an LRU evictor.
   - The design follows quran_android `AudioService` and QuranApp `RecitationService`, written fresh.
   - The audio *source and licence* is another researcher's call; self-host or use a provider that permits it.

   **Confidence: High.**

6. **Downloads: WorkManager + OkHttp workers with resumable range requests and MD5 or size verification.** Follow quran_android's strategy split (`Gapped`, `Gapless`, `SingleFile`) and its "verify files, then mark complete" pattern (quran-ios writes a success marker after unzip). **Confidence: High.**

7. **Translations and tafsir: store each as its own SQLite file with an FTS table, `verses(sura, ayah, text)` plus `properties(schema_version, text_version)`.** This is the same shape as quran_android's format, which is easy to generate ourselves. Arabic search uses FTS5 or FTS4 (`unicode61`) over a normalised `text_search` column (strip harakat, unify alef/ya/ta-marbuta; idea from `ArabicCharacterHelper`). Never download from `android.quran.com`. **Confidence: High.**

8. **User data in Room** (or SQLDelight if we go KMP): bookmarks with an optional ayah plus tags, last-read positions per mushaf, ayah notes, and reading history. Export and import as JSON. Skip `quran/mobile-sync` (GPL, quran.com accounts). **Confidence: Medium** (Room vs SQLDelight is a team-preference call).

9. **Prayer times and Qibla: `com.batoulapps.adhan:adhan2:0.0.7`** with the defaults `KARACHI` + `Madhab.HANAFI` for Pakistan; enable `coreLibraryDesugaring`. **Confidence: High.**

10. **Fonts:**
    - Bundle DigitalKhatt IndoPak (OFL) for the mushaf and an OFL Urdu Nastaliq font for translations (e.g. Noto Nastaliq Urdu, OFL [unverified]; QuranApp bundles `noto_nastaliq_urdu_regular.ttf`).
    - **Do not** ship the QuranWBW IndoPak font.
    - Ship KFGQPC fonts only unmodified and only if a Madani option is added.

    **Confidence: High.**

11. **Get written permissions before launch.**
    - Email QUL/Tarteel to confirm `permission_to_share` for layout 11 (16-line Taj), 313 (13-line Taj) and the matching IndoPak word text, and to ask how attribution should look.
    - Ask the DigitalKhatt author whether v0.1 is production-ready.
    - Self-host the bundled data (Play Asset Delivery, or our own CDN or R2).

    **Confidence: High.**

12. **Proposed module layout** (mirrors quran-ios layering and quran_android's feature split):
    - `:core:model` (pure Kotlin)
    - `:core:database` (prepackaged read-only Quran DBs plus Room user DB)
    - `:core:audio`, `:core:download`, `:core:designsystem`
    - `:feature:mushaf`, `:feature:index` (Para, Surah, Manzil, Ruku)
    - `:feature:player`, `:feature:bookmarks`, `:feature:search`, `:feature:translation`, `:feature:prayer`, `:feature:settings`
    - `build-logic` convention plugins

    **Confidence: Medium.** Final architecture belongs in the architecture report.

13. **Fallback if the owner insists on the exact "scanned print" look:** the only legal route is licensing Taj Company (or a similar publisher) page scans. In that case, reuse the **ayahinfo glyph-bounds pattern** (quran_android) or the **line-image pattern** (`ayah_highlights(page, line, left, right)` normalised), and budget for manual bounding-box annotation (548 pages, roughly 10–15k ayah segments). **Confidence: Medium.**

### Reuse map

Effort saved is a rough engineer-week estimate against building the same thing without any open-source help [inferred].

| Component | Source | License | Reuse mode | Effort saved |
|---|---|---|---|---|
| Indo-Pak 16/13/15-line page layouts (line → word ranges) | QUL layouts #11, #313, #12 (cross-check with Waqar144 Dart tables) | QUL code MIT; **data permission per resource (confirm)**; Waqar144 MIT | **Use data** | **Very high** (months of manual line-break entry) |
| Indo-Pak word-by-word Arabic text matching the layout | QUL "Indopak Nastaleeq / DigitalKhatt IndoPak" scripts | Per resource (confirm) | Use data | High |
| Mushaf font | DigitalKhatt IndoPak (via QUL or Waqar144) | **OFL-1.1** | Use asset | High (an Indo-Pak Quran font is years of work) |
| Line-fit / justify renderer | QuranApp `QuranTextMeasurer` + `Mushaf.kt`; Waqar144 `read_quran.dart` | GPL-3.0 / MIT | Port idea (QuranApp); may port code (Waqar144, MIT) | 2–3 wk |
| Line-page geometry (sidelines, dividers, line-height ratios) | quran-ios `LinePageGeometryEngine`, `LinePageMetrics` | **Apache-2.0** | **Fork/port code** with attribution | 1–2 wk |
| Quran navigation model (Sura/Juz/Hizb/Page, last-ayah finders) | quran-ios `QuranKit` | Apache-2.0 | Port code | 1–2 wk |
| Juz/Hizb/Rub/**Manzil/Ruku** per ayah | QUL metadata; QuranApp `ayahs` schema (idea); Waqar144 `rukus.dart` (MIT) | data / GPL idea / MIT | Use data + port idea | 1 wk |
| Ayah hit-testing and highlight overlay | quran_android `ImageAyahUtils`, `HighlightsDrawer`; QuranApp word boxes | GPL-3.0 | Port idea | 1 wk |
| Audio playback (range, repeat, speed, background, notification) | quran_android `AudioService`, `AudioRequest`; QuranApp `RecitationService`; AndroidX Media3 | GPL (idea) / Apache-2.0 (Media3) | Port idea + use library | 2 wk |
| Gapless ayah timings format | quran_android `SuraTimingDatabaseHandler` schema; QUL audio segments | GPL (schema is trivial) / per-resource | Port idea + use data | 1 wk |
| Audio and data download manager | quran_android `*DownloadStrategy`, `QuranDownloadService`; quran-ios `BatchDownloader` | GPL / Apache-2.0 | Port idea (Apache: may port code) | 1 wk |
| Android Auto browse tree | quran_android `feature/autoquran` | GPL-3.0 | Port idea | 0.5 wk |
| Translations DB format and FTS search | quran_android `DatabaseHandler`, `DefaultSearcher`, `ArabicCharacterHelper` | GPL-3.0 | Port idea | 1 wk |
| Bookmarks, tags, last page schema | quran_android `.sq` files; QuranApp Room entities | GPL-3.0 | Port idea (generic schema) | 0.5 wk |
| Mutashabihat / similar verses (hifz feature) | QUL datasets; QuranApp tables (idea) | per-resource / GPL | Use data | 1 wk |
| Prayer times and Qibla | `com.batoulapps.adhan:adhan2:0.0.7` | **MIT** | **Use library** | 2–3 wk |
| Gradle convention plugins | quran_android `build-logic` | GPL-3.0 | Port idea (generic Gradle) | 0.5 wk |
| Mushaf ID mapping for an optional online mode | quran.com-frontend-next `types/QuranReader.ts` | unclear | Port idea | small |

---

## 4. Risks and open questions

**Risks**

1. **QUL data permissions are not stated publicly.** Per-resource `permission_to_share` may be `unknown` for the Taj layouts. *Mitigation:* email Tarteel/QUL before building on it. The fallback is Waqar144's MIT tables, whose upstream provenance is also unconfirmed.
2. **Typographic-arrangement rights in the Taj Company print.** Line breaks that mirror a copyrighted print edition might be claimed by the publisher in some jurisdictions [unverified, needs legal opinion]. *Mitigation:* attribution, and contacting Taj Company, which could also become a partnership.
3. **DigitalKhatt IndoPak is v0.1** (font `nameID 5 = "Version 0.1"`). Glyph coverage or shaping bugs may exist, and the look may differ from the scanned print users love ("matches my printed Quran", report 01). *Mitigation:* a week-1 visual spike, side-by-side review with a hafiz, and keeping the font pluggable.
4. **Justification quality.** Waqar144 notes short lines look bad in the 13-line edition. Compose has no kashida justification, so we only get inter-word spacing and scaling. *Mitigation:* tune per-edition minimum and maximum gaps; consider HarfBuzz via the NDK later.
5. **Text accuracy.** Any digital Quran text must be proofread. Pakistan may require certification of printed or recorded Quran content [unverified; the regulatory researcher should confirm]. Text errors are the most damaging failure for this app category.
6. **Performance.** Measuring 16 lines × about 20 words per page at large font sizes on low-end devices. *Mitigation:* pre-measure off the main thread, cache a `TextLayoutResult` per (page, width, fontScale), and keep only ±2 pages resident.
7. **Clean-room discipline.** Engineers who have read GPL code must not transliterate it. *Mitigation:* the ADR plus code review; when real code is needed, take it from the Apache quran-ios.
8. **KFGQPC "cannot be Modified".** Subsetting or WOFF conversion of KFGQPC fonts likely breaches the EULA. Keep them out of v1.

**Open questions for the owner**

1. Which edition should be the default: **16-line Taj (what the reference app actually shows, per report 01)** or 13-line? The recommended renderer supports both, so this only decides the default and the QA focus.
2. Will v1 carry ads or IAP? This confirms the clean-room choice. If the app will always be free and donation-funded, GPL reuse (option C) becomes viable and could save a few more weeks.
3. Is iOS on the roadmap? If yes, consider writing `:core:model` and data in Kotlin Multiplatform from day 1, so quran-ios ideas map 1:1.
4. Can we budget roughly $X/month to self-host audio and data (CDN or R2), so we don't lean on the quran.com/quranicaudio infrastructure?
5. Should we approach Taj Company and Tarteel/QUL for formal permission or partnership? This is cheap insurance for a published app.

---

## 5. Sources

| URL | What I verified there |
|---|---|
| https://github.com/quran/quran_android (cloned @4a552ae, 2026-10-18) | GPL-3.0 LICENSE; README non-profit request and "CC BY-NC-ND" data note; `settings.gradle.kts` modules; `libs.versions.toml` (Media3 1.11.1, Metro 1.4.5, SQLDelight 2.4.1, RxJava 3.1.12, Compose BOM 2026.09.00); `AndroidCommon.kt` (minSdk 24, compileSdk 37); `MadaniPageProvider`, `MadaniDataSource` (`manzilPageArray` empty); `AyahInfoDatabaseHandler` glyph schema; linebyline `.sq` schemas and `<page>/<line>.png`; `AudioService`, `SuraTimingDatabaseHandler`, `GappedDownloadStrategy`, `AudioUtils`; `.gitignore` → `extras`; `QuranInfo` API; bookmark and translation `.sq` |
| https://quran.app/data/translations.php?v=5 | Live list of 79 entries; Urdu IDs 90, 91, 121; `fileUrl` on android.quran.com |
| https://android.quran.com/data/getTranslation.php?id=90&ext=zip | Translation DB = FTS3 `verses(sura, ayah, text)` plus `properties(schema_version=5, text_version=16)` |
| https://quran.app/data/pagetypes/snips/{type}.png → files.quran.app/preview | madani and shemerly 200; naskh, qaloon, warsh, indopak 404 |
| https://github.com/AlfaazPlus/QuranApp (cloned @c496013) | GPL-3.0; README and FEATURES (IndoPak, KFQPC v1/v2, "Mushaf mode with variable lines"); `QuranScriptUtils`, `MushafEntity`, `MushafMapEntity`, `Mushaf.kt`, `QuranTextMeasurer.kt`, `RecitationService : MediaLibraryService`; `quranapp.db` schema and row counts; recitation inventory JSON |
| https://github.com/quran/quran-ios (cloned @5bdd348) | Apache-2.0; module tree; `Reading.swift` (`indoPak`, line pages, sidelines); `IndoPakQuranReadingInfoRawData` (611 pages, skip 1); `LinePageMetrics`; `LinePageAssetService`; no manzil or ruku |
| https://github.com/quran/quran.com-frontend-next (cloned @aff1a03) | package.json `"license": "MIT"`, no LICENSE file (raw `LICENSE.txt` 404); `types/QuranReader.ts` enums; `fontFaceHelper.ts`; `groupLinesByVerses.ts`; IndoPak WOFF2 font licence string |
| https://github.com/TarteelAI/quranic-universal-library (cloned @0aca51e) | MIT LICENSE; `export_mushaf_layout.rb` schema; `resource_permission.rb` enums |
| https://qul.tarteel.ai/resources/mushaf-layout and `/mushaf-layout/{8,10,11,12,15,19,21,236,313,569,570,571}` | Layout names; 16-line Taj = 548 pages; 13-line Taj (Lahore) = 847 pages; 13-line Qudratullah = 849; 15-line = 610; no licence text on the pages |
| https://qul.tarteel.ai/resources/font | 26 fonts incl. "Digital Khatt Indopak font", KFGQPC Nastaleeq, Indopak Nastaleeq |
| https://qul.tarteel.ai/docs/faq | "Check repository license terms and dataset-specific licensing details before production use"; JSON vs SQLite advice |
| https://www.tarteel.ai/terms | Fetch returned only a title (no terms text visible); unresolved |
| https://github.com/Waqar144/quran_memorization_helper (cloned @b6470fb plus blob-less history) | MIT; layout files (847/610/548 pages); `rukus.dart`; `pages.dart` para offsets; ZWNJ word separator; fonts; commit history |
| https://f-droid.org/en/packages/com.wqar.quran_mem_helper/ | MIT, v1.8.2 (2026-06-24), 13/15/16-line IndoPak listing |
| https://f-droid.org/en/packages/com.mirfatif.noorulhuda/ | AGPL-3.0+, v1.03 (2023-11-24) |
| https://f-droid.org/en/packages/com.quran.aya/ | MIT, Flutter, v1.4.0 (2026-10-12) |
| https://f-droid.org/en/packages/com.dmouayad.my_quran/ | GPL-3.0-only, v1.8.2 (2026-10-05) |
| https://f-droid.org/en/packages/com.github.meypod.al_quran/ | AGPL-3.0-only, v1.1.1 (2026-07-10) |
| https://search.f-droid.org/?q=quran&lang=en | 11 Quran-related apps listed |
| https://github.com/batoulapps/adhan-kotlin (cloned @e23d143) | MIT; README (artifact `com.batoulapps.adhan:adhan2:0.0.7`, desugaring note); `CalculationMethod.KARACHI`; `Qibla.kt` |
| https://repo1.maven.org/maven2/com/batoulapps/adhan/adhan2/maven-metadata.xml | latest 0.0.7, lastUpdated 20260613 |
| https://github.com/quran/mobile-sync (cloned @0fbc3e9) | POM licence "GNU General Public License v3.0" |
| https://github.com/quran/quran.com-images (cloned @dbda568) | README: "code is copyleft GPL … fonts and pages … belong to the King Fahd Quran Complex" |
| https://repos.ecosyste.ms/api/v1/hosts/GitHub/repositories/{owner}/{repo} | Star and fork counts, detected licence, push dates for the 10 repos above |
| Font files (fontTools name table): `DigitalKhattIndoPak.otf`, `Digital Khatt V2 Font.otf` (Waqar144 repo); `uthmanic_hafs.ttf` (QuranApp); `UthmanTN1Ver10.otf` (quran_android); `indopak-nastaleeq-waqf-lazim-v4.2.1.woff2` and `hafs/v1/ttf/p2.ttf` (quran.com-frontend-next) | Licence strings quoted in §2.8 |
| Web searches: "indopak quran android jetpack compose open source github"; "quran android kotlin compose open source MIT license mushaf page github 2026"; "github \"13 lines\" indopak mushaf …"; "DigitalKhatt IndoPak font github OFL …"; KMP Islamic app search | No permissive Kotlin/Compose Indo-Pak app found; led to the Waqar144 repo; DigitalKhatt repo licence not found via search (OFL confirmed from the font file instead) |
