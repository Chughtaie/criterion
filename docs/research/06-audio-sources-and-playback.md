# 06: Recitation audio sources and playback engine

Researcher key: `r06-audio` · Date: 2026-10-19 · Role: audio/media engineer (AndroidX Media3, Quran recitation catalogues).

**Method.** Everything below was checked live today unless it is tagged otherwise.
- **Catalogues.** I downloaded and parsed each provider's catalogue:
  - everyayah `recitations.js`, with MD5 manifests and ZIP sizes
  - quranicaudio `/api/qaris`, `/api/qaris/{id}/audio_files/mp3` and `/api/surahs`
  - quran.com `api/v4` and `api.qurancdn.com/api/qdc`
  - mp3quran `api/v3` (`reciters`, `ayat_timing`)
  - alquran.cloud `edition?format=audio` and the islamic.network CDN `by-surah/info.json`
  - QUL `api/v1/audio/*`
- **Reachability and size.** I sent HEAD requests to about 400 audio URLs to check that they exist, their size, and their CDN and CORS headers.
- **MP3 analysis.** I downloaded sample MP3s and parsed the frame headers with a small Python script to get the real bitrate, CBR/VBR status and Xing/Info headers.
- **Timing data.** I compared the timing data against the real file durations.
- **Media3 internals.** I read Media3 `release` branch sources (`Mp3Extractor`, `ClippingMediaSource`, `ExoPlayer`, `PlayerMessage`, `AudioFocusManager`, `MediaSessionService`, `MediaSession`, `DownloadService`) and RELEASENOTES.
- **Reference app.** I read the reference app's bundled `res/raw/qari_list.json` from the APK that r01 pulled.
- **Prior art.** I read the audio code in the `quran_android` and `AlfaazPlus/QuranApp` clones made by r03, and cloned `cpfair/quran-align`.

Tags:
- **[verified: url]** means I fetched or inspected it in this session.
- **[unverified]** means it comes from memory.
- **[inferred]** means reasoning from verified facts.

---

## 1. Executive summary

1. **No Quran recitation source is clearly licensed for redistribution.** The most permissive written terms are alquran.cloud / islamic.network: "You may stream, embed and download them for personal and educational use. You may bundle them into a commercial product, but … copyrights lie with the reciters and they may ask you to remove the con[t]ent" [verified: alquran.cloud/terms-and-conditions].
   - quranicaudio.com says its MP3s are for "personal use … you may not use these files for commercial purposes".
   - everyayah publishes no licence. Its only written term is a link-back requirement for its timing files.
   - mp3quran.net publishes no terms.
   - Quran Foundation (QF) API terms cap caching at 1 week, and the client secret must stay server-side.
   - **So:** for personal use, stream from them now. Before a public release, email for permission and build the catalogue as remote-swappable data. (High)
2. **The reference app hotlinks two third-party CDNs.** Its `qari_list.json` (20 qaris) points 13 qaris at `download.quranicaudio.com/quran/…` and 7 at `server*.mp3quran.net/…`, all as one MP3 per surah, and it paywalls reciters while doing so [verified: APK `res/raw/qari_list.json`]. This refines r01's "mp3quran only" inference. It also proves the "surah MP3, no timings" model is what we must beat.
3. **The engine supports both audio layouts from v1:**
   - **Gapped** (one MP3 per ayah, `SSSAAA.mp3`, 6,236 files plus a basmala file): exact ayah, range, Para and Manzil playback, hifz repeat, partial downloads, and highlighting with no timing data needed.
   - **Gapless** (one MP3 per surah plus a timing table): natural flow, and the only option for some favourites (Saad Al-Ghamdi exists per-ayah only at 40 kbps; Qari Sadaqat Ali exists only per surah).

   quran_android uses the same split. (High)
4. **Per-ayah audio:** everyayah (79 folders) is the primary source, mirrored byte-for-byte by the islamic.network CDN and `mirrors.quranicaudio.com/everyayah`. I confirmed identical sizes for 2:255 Alafasy (831,865 B) and Urdu 1:1 (42,482 B). everyayah also publishes **per-surah ZIPs** and **`000_checksum.md5`** (6,350 entries for Alafasy), which make bulk download and integrity checks cheap [verified]. (High)
5. **Gapless timing data, free and key-less:**
   - **mp3quran.net `ayat_timing`:** 115 timed reads, including Alafasy, Sudais, Shuraim, Ghamdi, Husary, Minshawi, Abdul Basit (both styles), Shatri, Ajmy, Yasser and Hani Rifai.
   - **QUL API:** 46 surah recitations with full word segments, audio on `audio-cdn.tarteel.ai`.
   - **QDC (`api.qurancdn.com`):** 14 reciters with word segments.

   Timing must always come **from the same source as the audio file**; recordings differ between sources (Alafasy 1:1 starts at 0 ms in QDC and at 6,420 ms in mp3quran). (High)
6. **Timing quirks to normalise:**
   - mp3quran sometimes emits an explicit `ayah: 0` intro (ta'awwudh plus basmala) and sometimes leaves a gap before ayah 1.
   - QDC merges the basmala into ayah 1.
   - Our schema stores `ayah = 0` as the intro, and play-from-ayah-1 starts at 0 ms. (High)
7. **Playback stack:** AndroidX **Media3 1.11.1** (latest stable, released 2026-09-10; minSdk 23 since 1.9) [verified: Google Maven, RELEASENOTES].
   - The service is a `MediaLibraryService` with `ExoPlayer`, so it also works with Android Auto.
   - `ResolvingDataSource` maps logical `quran-audio://` URIs to a local file, else the CDN with mirror failover, through `CacheDataSource` (LRU stream cache).
   - Gapless Para and range playback uses `ClippingConfiguration` per surah. Ayah tracking uses `PlayerMessage`s at ayah boundaries. Hifz pauses use `setPauseAtEndOfMediaItems`. (High)
8. **Para (Juz) playback is cheap in either layout:**
   - Gapped: at most **601 MediaItems** (Juz 30 = 564 ayahs plus 37 basmalas).
   - Gapless: at most **37 clipped surah items**.

   Manzils start on surah boundaries (1, 5, 10, 17, 26, 37, 50) [verified: tanzil quran-data.js]. Para playback was the reference app's most-requested missing audio feature. (High)
9. **Downloads use our own engine (OkHttp plus plain files), not Media3 `DownloadManager`.** The engine supports per-surah ZIPs, MD5 checks, exportable files and per-ayah granularity. It runs in two hosts:
   - Android 14+: a **user-initiated data transfer (UIDT) job**.
   - Android 7–13: a **WorkManager long-running worker (`dataSync`)**.

   Android 15 caps `dataSync` foreground services (FGS) at 6 h per 24 h in the background. Android 16 lets long-running workers exhaust the job quota and explicitly recommends UIDT for user-triggered downloads [verified: developer.android.com]. (Medium-High)
10. **Android 17 background-audio hardening** silences audio from apps without a visible activity or a non-`shortService` FGS. Apps targeting API 37 also need a foreground service with while-in-use (WIU) capability. Media3 `MediaSessionService` handles this.
    - **Never auto-start recitation from an alarm.** "Surah Kahf on Friday" or "Mulk at night" must be a notification the user taps [verified: developer.android.com/about/versions/17/changes/bg-audio]. (High)
11. **v1 catalogue: 18 Arabic recitations plus 2 Urdu tracks** (§2.8.8). Highlights:
    - Default reciter: Alafasy.
    - **"Arabic + Urdu (Shamshad Ali Khan)" interleaved per ayah**, which the reference app cannot do.
    - Muallim and kids-repeat for hifz.
    - Qari Sadaqat Ali (Pakistani) as surah-only until we generate timings.
    - Full-Quran sizes: about 0.4–1.2 GB at 32–64 kbps and 1.1–2.5 GB at 128 kbps (Mujawwad editions run 3.4–4.9 GB). Juz 'Amma is 29 MB at 64 kbps and 59 MB at 128 kbps (Alafasy). (High on data, Medium on the reciter picks)
12. **Self-hosting the curated catalogue is cheap once permission exists.** Cloudflare R2 costs $0.015/GB-month with free egress, so about 70 GB is roughly $1/month [verified: developers.cloudflare.com/r2/pricing]. It removes hotlink dependence; the remote catalogue keeps a kill-switch per recitation. (Medium)

---

## 2. Detailed findings

### 2.1 Source landscape (live, 2026-10-19)

| Source | Layout | Catalogue size | Timing data | URL pattern | CDN / headers | Terms (decisive text) |
|---|---|---|---|---|---|---|
| **everyayah.com** | Gapped: per-ayah `SSSAAA.mp3` (+ `bismillah.mp3`, `audhubillah.mp3`); per-surah `zips/NNN.zip`; full `000_versebyverse.zip` | **79 folders** (about 45 Arabic reciters, plus Warsh and translations incl. **Urdu Shamshad Ali Khan 46 kbps** and **Urdu Farhat Hashmi word-by-word 32 kbps**) | Per-ayah: none needed. Legacy split-timing ZIPs for about 25 reciters in `/data/timings_files/` | `https://everyayah.com/data/{folder}/{sss}{aaa}.mp3` | BunnyCDN, HTTP/2, `access-control-allow-origin: *`, `accept-ranges: bytes`, `cache-control: max-age=25600000`; `000_checksum.md5` per folder | **No site licence.** Timing files: "You must link back to our site from your product and web-site to use these timings." Contact goes to quran.zendesk.com (Quran.com/QF) [verified: everyayah.com/data/timings_files/000_disclaimer.txt, everyayah.com] |
| **islamic.network CDN** (alquran.cloud) | Gapped by **global ayah number 1–6236**; also per-surah | 190 API audio editions (32 `versebyverse` + 6 per-ayah `translation`-typed + 152 `surahbysurah`); the CDN's `by-surah/info.json` lists **159 per-surah** editions (incl. `ar.sadaqatali`, `ar.yasseraldossari`) | None | `https://cdn.islamic.network/quran/audio/{kbps}/{edition}/{n}.mp3` · `…/audio-surah/{kbps}/{edition}/{sura}.mp3` | nginx, Hetzner Ceph origin; per-ayah files byte-identical to everyayah | "Recitations are licensed to us by the reciters or their estates for free, non-commercial redistribution… You may stream, embed and download them for personal and educational use. You may bundle them into a commercial product, but please note that copyrights lie with the reciters and they may ask you to remove the conent." "The audio CDN has no per-client throttle, but please cache aggressively at your own edge." [verified: alquran.cloud/terms-and-conditions, alquran.cloud/cdn] |
| **quranicaudio.com** | Gapless: per-surah `NNN.mp3` | **177 entries** (109 recitations, 47 Haramain taraweeh, 10 non-Hafs, 11 with translation) incl. **Sadaqat Ali**, Husary/Minshawi **kids-repeat**, **Sudais & Shuraim + Urdu** | None on this API (QDC, below, has it) | `https://download.quranicaudio.com/quran/{relative_path}{sss}.mp3` | BunnyCDN, CORS `*`. **API file metadata is unreliable**: Sadaqat Ali's list carries Juhani's sizes and artist tags | "Mp3s on this site may be downloaded and used for personal use free of charge. However, you may not use these files for commercial purposes as many of these files have rules and regulations that prevent their sale except by the publishing companies." [verified: quranicaudio.com/about] |
| **quran.com legacy API v4 / QDC** | v4 `recitations`: gapped (12 recitations, `verses.quran.com/{path}`); QDC: gapless | 12 per-ayah; **14 gapless** (incl. **Minshawi kids-repeat id 168**, **Husary Muallim id 12**) | **Yes, ayah plus word segments**: `api.qurancdn.com/api/qdc/audio/reciters/{id}/audio_files?chapter=N&segments=true`, also `api.quran.com/api/v4/chapter_recitations/{id}/{ch}` | `https://download.quranicaudio.com/qdc/{reciter}/{style}/{n}.mp3` (no zero-pad) | Key-less today | **QF Developer Terms (updated 2026-10-11):** no caching over **1 week** unless through Content Sync; "not sold, sublicensed, or redistributed"; ads and IAP allowed; Content API secret "must not" be embedded in a mobile app [verified: api-docs.quran.foundation/legal/developer-terms, /docs/quickstart] |
| **mp3quran.net API v3** | Gapless per surah | **243 reciters / 289 moshafs** (203 complete), 12+ riwayat (Warsh 13, Qaloon 11…), Muallim and Mujawwad moshafs | **Yes, `ayat_timing?surah=N&read={moshafId}` for 115 reads** (ms, plus Madani-page polygons) | `{server}{sss}.mp3`, e.g. `https://cdn.mp3quran.net/audio/mishary-alafasy/r1/001.mp3`; old `server8.mp3quran.net/afs/` redirects with 301 | Cloudflare, CORS `*`, `cf-cache-status: HIT`. API host must be `www.` (apex returns 301) | **No published terms/licence found** (API page, about page; `/eng/terms` returns 404) [verified: www.mp3quran.net/eng/api, /eng/about] |
| **QUL (Tarteel)** | Gapless and gapped datasets | 133 recitations, **59 with segments**. API: `qul.tarteel.ai/api/v1/audio/surah_recitations` lists 119, **46 with full 6,236-ayah word segments** | **Yes, word-level** (`[word, start_ms, end_ms]`) via `/api/v1/audio/surah_segments/{id}?surah=N` (paginated) or SQLite/JSON download (login) | `https://audio-cdn.tarteel.ai/quran/surah/{path}/mp3/{sss}.mp3` (Cloudflare) | Key-less API | FAQ: "Yes, you can use QUL data in commercial projects. However, please review the licensing terms for each resource." Code is MIT [verified: qul.tarteel.ai/faq, QUL LICENSE] |
| **cpfair/quran-align** | Word timings for everyayah per-ayah files | Many everyayah qaris (Releases) | Word-level, per-ayah-file-relative | n/a | n/a | Code MIT; "These data files are licensed under a Creative Commons Attribution 4.0 International License" [verified: cloned README/LICENSE] |
| **King Fahd Complex (qurancomplex.gov.sa)** | Mixed | Its own reciters (e.g. Huthaify, Ayyub, Akhdar) [unverified list] | n/a | n/a | Site returned 503 / reset to us | Search excerpt: the Complex is "honoured to offer the digital copies of the voice recordings" for **free general use** in "computer apps, broadcasting channels, internet websites" [partially verified: search excerpt of qurancomplex.gov.sa/en/?p=3979; direct fetch failed] |
| **archive.org** | Arbitrary uploads | E.g. "Quran-e-Kareem With Urdu Translation (Fateh Muhammad Jalandhari)" (~100k downloads), Qari Waheed Zafar Qasmi + Urdu | None | n/a | n/a | Mostly **no licence** (`licenseurl` null). Treat as CD rips; do not use [verified: archive.org advancedsearch] |

### 2.2 What the reference app actually streams

`res/raw/qari_list.json` (4,445 B) maps each of 20 reciters to `base_address` + `*##*` (3-digit surah) [verified: APK]:

| Host | Reciters |
|---|---|
| `download.quranicaudio.com/quran/…` (13) | Sudais (`purchase:1`), Alafasy (default free), Saad Al-Ghamdi, Shuraym, Ajmy, Abdul Basit **Mujawwad**, Abdul Muhsin al-Qasim, Huthaify, Bandar Baleela, Khalid al-Qahtani, Minshawi, **Qari Sadaqat Ali ("Pakistan")**, Salah al-Budair |
| `serverN.mp3quran.net/…` (7) | Maher Al-Muaiqly, Yasser Al-Dosari, Abdulrahman Aloosi, Abdullah Al-Johany, Khalid Al-Mohanna, Saleh Al-Habdan, Noreen Siddiq (Doori) |

The app has no per-ayah files, no timing data, no download-all, and it hotlinks quranicaudio while paywalling reciters, which conflicts with quranicaudio's "no commercial purposes" [inferred].

### 2.3 Reciter availability matrix (South-Asian favourites)

Sizes are for the full Quran. "T" means a timing source exists for that exact file set. Every value below is verified unless marked.

| Reciter / style | everyayah per-ayah (folder · kbps · full ZIP) | islamic.network per-ayah | Gapless sources (size, hours) | Timing for gapless | Notes |
|---|---|---|---|---|---|
| **Mishary Alafasy** (Murattal) | `Alafasy_64kbps` 0.86 GB · `Alafasy_128kbps` 1.71 GB | `ar.alafasy` 64/128 | quranicaudio 1.72 GB / 29.9 h; mp3quran r1; QDC 7; QUL 7 | **T**: mp3quran 123, QDC 7, QUL 7 | Most popular; reference default |
| **Abdul Basit** Murattal | `Abdul_Basit_Murattal_64kbps` 0.91 GB · `_192kbps` 2.70 GB | `ar.abdulbasitmurattal` 64/192 | quranicaudio 2.69 GB / 31.2 h (192k); mp3quran r3; QDC 2 | **T**: mp3quran 53, QDC 2, QUL 2 | |
| **Abdul Basit** Mujawwad | `Abdul_Basit_Mujawwad_128kbps` 3.39 GB | n/a | quranicaudio 3.51 GB / 60.9 h; mp3quran r1; QDC 1 | **T**: mp3quran 51, QDC 1, QUL 1 | Very popular in Pakistan |
| **Minshawi** Murattal | `Minshawy_Murattal_128kbps` 1.67 GB · `Menshawi_32kbps` 0.41 GB | `ar.minshawi` 128 | quranicaudio 1.67 GB / 28.9 h; mp3quran r1; QDC 9 | **T**: mp3quran 112, QDC 9, QUL 9 | |
| **Minshawi** Mujawwad | `Minshawy_Mujawwad_64kbps` · `_192kbps` 4.93 GB | `ar.minshawimujawwad` 64 | quranicaudio 4.81 GB / 61.4 h; mp3quran r2 | QUL 395 (segments) | Large; stream-first |
| **Minshawi with children** (Muallim) | n/a | n/a | mp3quran r3 "Mo'lim" (114 surahs); QDC 168 "Kids repeat" | **T**: QDC 168, QUL 314 | Timing only via QF/QUL |
| **Husary** Murattal | `Husary_64kbps` 1.23 GB · `Husary_128kbps` 2.47 GB | `ar.husary` 64/128 | quranicaudio 2.47 GB / 42.8 h; mp3quran r1 | **T**: mp3quran 118, QDC 6, QUL 6 | |
| **Husary Muallim** | `Husary_Muallim_128kbps` **2.75 GB** | n/a | quranicaudio 141 (2.64 GB / 45.9 h); QDC 12 | **T**: QDC 12, QUL 12 | Gapped is ideal for hifz |
| **Husary with children** | n/a | n/a | quranicaudio 142 (0.93 GB / 64.6 h) | none | v1.1 |
| **Husary** Mujawwad | `Husary_128kbps_Mujawwad` 3.50 GB | `ar.husarymujawwad` 64/128 | mp3quran r2 | **T**: mp3quran 119, QUL 164 | |
| **Sudais** | `Abdurrahmaan_As-Sudais_64kbps` 0.61 GB · `_192kbps` 1.83 GB | `ar.abdurrahmaansudais` 64/192 | quranicaudio 1.77 GB / 21.4 h; mp3quran r1 | **T**: mp3quran 54, QDC 3, QUL 3 | |
| **Shuraim** | `Saood_ash-Shuraym_64kbps` · `_128kbps` 1.06 GB | `ar.saoodshuraym` 64 | quranicaudio 1.05 GB / 18.3 h; mp3quran r1 | **T**: mp3quran 31, QDC 10, QUL 10 | |
| **Saad Al-Ghamdi** | `Ghamadi_40kbps` **only** (0.45 GB) | n/a | quranicaudio 2.85 GB / 24.7 h (256k); mp3quran r1 | **T**: mp3quran 30, QUL 13 | **Must be gapless** for decent quality |
| **Maher Al-Muaiqly** | `Maher_AlMuaiqly_64kbps` 0.61 GB · `MaherAlMuaiqly128kbps` 1.21 GB | `ar.mahermuaiqly` 64/128 | quranicaudio 159 (96k, 1.41 GB / 32.5 h); mp3quran r1 (no timing), r3 Mujawwad | QUL 65 (Tarteel CDN); mp3quran only r3 | Gapped primary |
| **Abu Bakr Ash-Shatri** | `…_64kbps` · `Abu_Bakr_Ash-Shaatree_128kbps` 1.43 GB | `ar.shaatree` 64/128 | quranicaudio 1.26 GB / 21.7 h; mp3quran r1 | **T**: mp3quran 4, QDC 4, QUL 4 | |
| **Yasser Ad-Dosari** | `Yasser_Ad-Dussary_128kbps` 1.50 GB | n/a | quranicaudio 1.49 GB / 25.9 h; mp3quran r1; i.n. surah 128 | **T**: mp3quran 92, QDC 97, QUL 174 | |
| **Hani Ar-Rifai** | `Hani_Rifai_64kbps` · `_192kbps` 2.15 GB | `ar.hanirifai` 64/192 | quranicaudio 2.21 GB; mp3quran r1 | **T**: mp3quran 89, QDC 5, QUL 5 | |
| **Ahmed Al-Ajmy** | `ahmed_ibn_ali_al_ajamy_128kbps` 1.51 GB (+2 older) | `ar.ahmedajamy` 64/128 | quranicaudio 1.93 GB (160k); mp3quran r1 | **T**: mp3quran 5, QUL 19 | |
| **Qari Sadaqat Ali** (Pakistan) | n/a | n/a | quranicaudio `sadaqat_ali/` (~1.75 GB, 111 of 114 sizes summed); i.n. `ar.sadaqatali` 128 | **none anywhere** | Surah-only in v1; generate timing later |
| **Ali Al-Huthaify** | `Hudhaify_32/64/128kbps` | `ar.hudhaify` 32/64/128 | mp3quran r1 | **T**: mp3quran 74 | King Fahd Complex reciter; cleanest licence story [inferred] |
| **Urdu: Shamshad Ali Khan** (translation voice) | `translations/urdu_shamshad_ali_khan_46kbps` **0.51 GB** | `ur.khan` 64 (same bytes) | QuranApp built per-surah copies on GitHub Releases | n/a | **Per-ayah, so it interleaves with any Arabic gapped reciter** |
| **Urdu: Farhat Hashmi** (word-by-word tarjuma) | `translations/urdu_farhat_hashmi` 32 kbps, 1.50 GB (1:1 is about 2 min) | n/a | n/a | n/a | Al-Huda style lecture; popular in PK; v1.1 |
| **Sudais & Shuraim + Urdu** | n/a | n/a | quranicaudio 67 (136 files, split parts, 2.48 GB / 43 h) | none | Surah-only |
| Fateh Muhammad Jalandhari Urdu | n/a | n/a | archive.org only (no licence) | none | **Do not ship** |

The everyayah full-ZIP sizes come from HEAD requests on `000_versebyverse.zip`. The quranicaudio size and hours come from its `audio_files` API, except Sadaqat Ali, whose sizes come from HEAD [verified].

### 2.4 Timing data: formats and traps

| Source | Shape | Example (verified) | Trap |
|---|---|---|---|
| QDC | `verse_timings[{verse_key, timestamp_from, timestamp_to, segments[[word,start,end]]}]` | Alafasy 2:1 = 0–7,650 ms; word 1 segment `[1,0,7595]` | **Basmala is merged into ayah 1** (2:1 starts at 0). Some segments are malformed (`[1]`, `[2]` with no times) |
| mp3quran | `[{ayah, start_time, end_time, polygon, page}]` | Alafasy 112 starts ayah 1 at 7,539; Minshawi 112 has `ayah:0` 0–4,823 | Intro is **sometimes `ayah:0`, sometimes an implicit gap**. Last `end_time` can exceed the file by about 60 ms (Sudais 112) |
| QUL | `{"s:a": {time_from, time_to, segments}}` + `audio{url,duration,audio_size}` | Abdul Basit Mujawwad 1:1 = 0–6,180 | API paginates (surah 2 returned 10 keys per page) |
| quran-align | per-ayah file `[{surah, ayah, segments[[w0,w1,start,end]]}]` | n/a | Word indices are 0-based over Tanzil Uthmani words. We must map them to Indo-Pak word boundaries (r04/r05) |
| quran_android | SQLite `timings(sura, ayah, time[, words])` + `properties` | [verified: cloned `SuraTimingDatabaseHandler.kt`] | Stores start times only |

I checked mp3quran surah 112 for five reads against the real file duration (decoded frame count): last `end_time` ≤ duration + 60 ms in every case [verified]. Our build pipeline must repeat this for all 114 surahs per reciter, and record `content_length` and `etag` per file, so a silently re-encoded CDN file disables highlighting instead of drifting.

### 2.5 MP3 technical findings (frame-level parse, verified)

| File | Real encoding | Header | Implication |
|---|---|---|---|
| everyayah `Alafasy_128kbps/002255.mp3` | 128 kbps CBR, 44.1 kHz, 52.0 s | n/a | n/a |
| everyayah `Alafasy_128kbps/001001.mp3` | **192 kbps** CBR (replaced Jan 2025) | LAME `Info` | **Folder bitrate labels are nominal.** Compute sizes; don't trust names |
| everyayah `*_64kbps` | 64 kbps **MPEG-2, 22.05 kHz** | n/a | About half the size; fine for speech on phone speakers |
| quranicaudio `/quran/…/067.mp3` | 128 kbps CBR | ID3, **no Xing/Info** | ExoPlayer seeks via `ConstantBitrateSeeker`, which is accurate for CBR |
| QDC `/qdc/…/1.mp3` | 192 kbps CBR | ID3 + `Xing` | n/a |
| mp3quran `…/r1/001.mp3` | 128 kbps CBR | ID3 + encoder tag | n/a |

**Seeking accuracy matters for gapless ayah seeks.**
- Media3 `Mp3Extractor` uses Xing/VBRI metadata first, then CBR. The Xing TOC has 100 entries; on a 2-hour Al-Baqarah that is 72 s granularity [inferred].
- `FLAG_ENABLE_INDEX_SEEKING` builds an exact time-to-byte index "as the file is read" and "should only be used if … the file is small [or] the bitrate is variable" [verified: Mp3Extractor.java].
- **Decision:**
  - Prefer CBR sources.
  - Set `FLAG_ENABLE_CONSTANT_BITRATE_SEEKING` globally.
  - Turn on index seeking only for *downloaded* VBR files, through a per-recitation `isVbr` catalogue flag.
- Media3 1.11.0 also "Use[s] gapless-aware durations from Xing/Info headers" and fixed LAME delay/padding trimming [verified: RELEASENOTES]. Per-ayah sequences therefore play near-seamlessly.

### 2.6 Storage estimates

Rule of thumb: **MB ≈ kbps × hours × 0.45**. This matches Alafasy: 128 × 29.9 h × 0.45 ≈ 1,722 MB vs 1.72 GB measured.

| Scope | 32 kbps | 64 kbps | 128 kbps | 192 kbps | Source |
|---|---|---|---|---|---|
| Full Quran, Murattal (~20–30 h) | ~0.4 GB | 0.6–1.2 GB | 1.1–2.5 GB | 1.8–2.7 GB | everyayah ZIPs, quranicaudio API |
| Full Quran, Mujawwad / Muallim (45–65 h) | n/a | ~1.7 GB | 2.6–3.5 GB | 4.9 GB | same |
| One Juz (avg = full / 30) | ~14 MB | ~29 MB | ~57 MB | ~90 MB | derived |
| **Juz 'Amma (Alafasy)** | n/a | **28.8 MB** | **59.2 MB** | n/a | sum of 37 surah ZIPs [verified] |
| **Popular pack: Kahf + Yaseen + Rahman + Waqiah + Mulk (Alafasy)** | n/a | ~40 MB | **79.7 MB** | n/a | per-surah ZIPs [verified] |
| Timing pack per gapless recitation | n/a | n/a | n/a | n/a | ~100 KB (ayah only); ~0.5 MB gz with word segments (QuranApp Alafasy `.json.gz` = 541,693 B [verified]) |

Gapped full download is 6,236 files plus a basmala file (everyayah's MD5 list has 6,350 lines for Alafasy: 6,236 ayahs, 112 `SSS000` intros, audhubillah and bismillah). The `SSS000` files are inconsistent across reciters: Husary has 1 and Ghamadi has 0, so **don't depend on them** [verified].

### 2.7 Platform facts that shape the design

| Topic | Fact | Source |
|---|---|---|
| Media3 version | Latest **1.11.1** (2026-09-10); `media3-session`, `-ui-compose`, `-ui-compose-material3`, `-datasource-okhttp`, `-exoplayer-workmanager` all at 1.11.1. Session POM: Kotlin stdlib 2.2.10 (matches the project); okhttp module compiles against **OkHttp 4.12.0** | [verified: dl.google.com maven-metadata + POMs] |
| Media3 min SDK | "Update `minSdk` to `23`" (1.9.0). Project minSdk 24 is fine | [verified: RELEASENOTES] |
| Compose media UI | `MiniController` (ui-compose-material3), `ProgressSlider`, `PlaybackSpeedToggleButton`, `PlaybackSpeedControl` | [verified: RELEASENOTES 1.10–1.11] |
| Session | `MediaSessionService`/`MediaLibraryService` are now `LifecycleService`s; `setMediaButtonPreferences(List<CommandButton>)` (replaces `setCustomLayout`); `onPlaybackResumption(…, isForPlayback)` | [verified: RELEASENOTES, MediaSession.java] |
| Paused FGS | Session stays in foreground for `DEFAULT_FOREGROUND_SERVICE_TIMEOUT_MS = 600_000` after pause. Short hifz pauses don't drop the FGS | [verified: MediaSessionService.java] |
| Ducking | With `AUDIO_CONTENT_TYPE_SPEECH`, ExoPlayer **pauses instead of ducking** on `LOSS_TRANSIENT_CAN_DUCK` | [verified: AudioFocusManager.java] |
| Precise position callbacks | `ExoPlayer.createMessage(target).setPosition(mediaItemIndex, positionMs).setDeleteAfterDelivery(false)`; `setPauseAtEndOfMediaItems(true)`; `SilenceMediaSource(durationUs)` | [verified: ExoPlayer.java, PlayerMessage.java, SilenceMediaSource.java] |
| Clipping | `MediaItem.ClippingConfiguration` (start/end ms, `allowUnseekableMedia`, dynamic clipping updates) | [verified: ClippingMediaSource.java] |
| Metadata updates | `Player.replaceMediaItem(s)` + `MediaSource.updateMediaItem` lets us update the title per ayah without re-buffering | [verified: RELEASENOTES] |
| `dataSync` FGS (Android 15+) | "run for a total of 6 hours in a 24-hour period, after which the system calls … `Service.onTimeout(int,int)`"; foregrounding the app resets the timer. Media3 `DownloadService` already implements `onTimeout` | [verified: developer.android.com/…/fgs/timeout; DownloadService.java] |
| WorkManager (Android 16) | "long running workers … can exhaust your app's job quota… If you need to download data in response to a user action, consider using a user-initiated data transfer job." | [verified: developer.android.com/…/long-running] |
| UIDT | API 34+, `RUN_USER_INITIATED_JOBS`, `setUserInitiated(true)`, `setEstimatedNetworkBytes`, `setNotification(...)`; must be scheduled while the app is visible; "no Jetpack library supports UIDT" | [verified: developer.android.com/…/uidt] |
| Android 17 audio | Background audio needs a visible activity or a non-`SHORT_SERVICE` FGS (all apps). Target 37 also needs a WIU FGS. Otherwise "Playback is silenced". Media3 `MediaSessionService` recommended | [verified: developer.android.com/about/versions/17/changes/bg-audio] |
| Android 15 boot | Can't start `dataSync`/`mediaPlayback` FGS from `BOOT_COMPLETED` | [verified: fgs/service-types] |
| Play Console | FGS-type declaration form with justification for `mediaPlayback`/`dataSync` | [unverified] |

---

### 2.8 Design

#### 2.8.1 Component architecture

```
┌──────────────────────────── UI process (Compose) ─────────────────────────────┐
│ Reader (Indo-Pak pages, r04)      Index (Surah/Para/Manzil)   Player sheet     │
│   ▲ highlight(sura,ayah,word)        │ "Play Para 3"            MiniController │
│   │ autoTurn(page)                   ▼                          ProgressSlider │
│ FollowAlongController ◄─── PlaybackStateHub (StateFlow) ◄────┐  Speed / Repeat │
│   (ayah→page via MushafLayoutRepo)                           │       │         │
│                         MediaController (commands) ──────────┼───────┘         │
└──────────────────────────────────────┬────────────────────────┼────────────────┘
                                        │ binder                 │ same process
┌──────────────── QuranPlaybackService : MediaLibraryService ────┼────────────────┐
│ MediaLibrarySession ── Callback: onSetMediaItems / onGetChildren / onSearch      │
│   │ (Auto/Wear/BT/lock screen)          onPlaybackResumption / onCustomCommand   │
│   ▼                                                                             │
│ QuranForwardingPlayer (next/prev = ayah or surah, title per ayah)               │
│   ▼                                                                             │
│ ExoPlayer (AudioAttributes USAGE_MEDIA + SPEECH, handleAudioFocus, noisy,        │
│            WAKE_MODE_NETWORK, DefaultLoadControl 60–300 s, Mp3 CBR seeking)      │
│   ├── PlanExpander: PlaybackPlan → MediaItems (gapped items | clipped surahs)   │
│   ├── AyahTracker: PlayerMessages at ayah starts (gapless) / item index (gapped)│
│   ├── RepeatController + HifzPauseController (pauseAtEnd + delayed resume)       │
│   ├── SleepTimer (min / end-of-ayah / end-of-surah / end-of-juz, fade-out)      │
│   └── ResumeStore (DataStore: plan, index, position)                            │
│ DataSource chain:                                                               │
│   ResolvingDataSource(quran-audio://rec/s[/a]) ─► local file?  FileDataSource   │
│                                   └─► MirrorFailover(primary→mirror)            │
│                                         └─► CacheDataSource(SimpleCache LRU)    │
│                                               └─► OkHttpDataSource              │
└────────────────────────────────────────────────────────────────────────────────┘
┌──────── Data layer (Room/SQLDelight + files) ────────┐  ┌──── Downloads ─────────┐
│ Catalogue (bundled JSON + remote override, kill-switch)│  │ DownloadCoordinator   │
│ TimingRepo (timing.db per recitation)                 │  │  API34+: UIDT JobSvc  │
│ QuranMeta (juz/manzil/ruku/ayah counts, r05)          │  │  API<34: WorkManager  │
│ AudioFileStore: <extFiles>/audio/<rec>/{sss}/{sssaaa}.mp3│ │  OkHttp Range resume │
│                 or /{sss}.mp3, timing.db, .nomedia     │  │  ZIP unpack + MD5     │
└────────────────────────────────────────────────────────┘  └──────────────────────┘
```

#### 2.8.2 Data model

```kotlin
enum class AudioLayout { GAPPED_AYAH, GAPLESS_SURAH }
enum class RecitationStyle { MURATTAL, MUJAWWAD, MUALLIM, KIDS_REPEAT, TRANSLATION_ONLY, ARABIC_WITH_TRANSLATION }
enum class BasmalaStrategy { BISMILLAH_FILE, USE_FATIHA_1_1, EMBEDDED /*gapless intro*/, NONE }

data class Reciter(val id: String, val nameEn: String, val nameAr: String, val nameUr: String,
                   val country: String?, val photo: String?)

data class Recitation(                       // one playable edition
  val id: String,                            // "alafasy.murattal.gapped.128"
  val reciterId: String,
  val style: RecitationStyle,
  val riwayah: String = "hafs",
  val language: String = "ar",               // "ur" for translation voices
  val layout: AudioLayout,
  val bitrateKbps: Int,                      // measured, not the folder label
  val sources: List<AudioSourceRef>,         // ordered: primary, mirrors
  val basmala: BasmalaStrategy,
  val timing: TimingRef?,                    // null => no highlight (gapped needs none)
  val surahBytes: IntArray?,                 // 114 sizes for UI estimates (we measure them)
  val isVbr: Boolean = false,
  val attribution: String,                   // shown in Credits
  val licenseStatus: LicenseStatus,          // PERMITTED / PERSONAL_ONLY / PENDING
  val enabled: Boolean = true                // remote kill-switch
)

data class AudioSourceRef(val host: String, val template: String)
// Tokens: {sss}=%03d sura, {aaa}=%03d ayah, {s}=sura unpadded, {g}=global ayah 1..6236
//   everyayah:   https://everyayah.com/data/Alafasy_128kbps/{sss}{aaa}.mp3
//   i.network:   https://cdn.islamic.network/quran/audio/128/ar.alafasy/{g}.mp3
//   mp3quran:    https://cdn.mp3quran.net/audio/saad-ghamdi/r1/{sss}.mp3
//   QDC:         https://download.quranicaudio.com/qdc/mishari_al_afasy/murattal/{s}.mp3
//   QUL/Tarteel: https://audio-cdn.tarteel.ai/quran/surah/ghamadi/murattal/mp3/{sss}.mp3
data class TimingRef(val url: String, val version: Int, val sha256: String)
```

Timing pack (one SQLite per gapless recitation, built offline by our pipeline):

```sql
CREATE TABLE ayah_timing (
  sura     INTEGER NOT NULL,
  ayah     INTEGER NOT NULL,           -- 0 = intro (ta'awwudh/basmala) if present
  start_ms INTEGER NOT NULL,
  end_ms   INTEGER NOT NULL,
  words    BLOB,                       -- optional packed (wordIdx u16, start i32, end i32)
  PRIMARY KEY (sura, ayah)
) WITHOUT ROWID;
CREATE TABLE audio_file (              -- guards against CDN re-encodes
  sura INTEGER PRIMARY KEY, content_length INTEGER, etag TEXT, duration_ms INTEGER
);
CREATE TABLE properties (key TEXT PRIMARY KEY, value TEXT);   -- source, version, attribution
```

Play-scope model, which covers every must-have entry point:

```kotlin
sealed interface PlayScope {
  data class Ayahs(val from: VerseKey, val to: VerseKey) : PlayScope  // single ayah = from==to
  data class Surah(val sura: Int) : PlayScope
  data class Juz(val juz: Int) : PlayScope        // 1..30, mid-surah boundaries
  data class Manzil(val manzil: Int) : PlayScope  // 1..7, surah boundaries 1,5,10,17,26,37,50
  data class Ruku(val ruku: Int) : PlayScope      // Indo-Pak ruku table from r05
  data class Page(val page: Int, val edition: String) : PlayScope  // "play this page"
}
data class RepeatPlan(
  val ayahTimes: Int = 1,               // each ayah ×N
  val rangeTimes: Int = 1,              // whole scope ×M; 0 = infinite
  val pauseAfterAyah: PauseRule = PauseRule.None,   // None | Fixed(ms) | Proportional(×1.0)
  val cumulative: Boolean = false       // hifz chain: 1, 1-2, 1-3 …
)
data class PlaybackPlan(val recitationId: String, val scope: PlayScope, val repeat: RepeatPlan,
                        val translationVoiceId: String? = null, val speed: Float = 1f)
```

#### 2.8.3 Building playlists (Surah / Para / Manzil / ayah range)

**Basmala rules** [verified for data; rules are standard]:
- Surah 1: ayah 1 *is* the basmala.
- Surah 9: no basmala.
- Every other surah: play the basmala before ayah 1 only when the range includes ayah 1.

In gapped mode the basmala is the reciter's `bismillah.mp3` if present, else their `001001.mp3`. quran_android uses the same trick: it checks for `1/1.mp3`, and `doesRequireBasmallah` skips surahs 1 and 9 [verified: cloned `AudioUtils.kt`]. In gapless mode the basmala lives in the intro, so starting at ayah 1 means clip start = 0.

```kotlin
// Gapless: one clipped MediaItem per surah touched; Juz 3 = [2:253..2:286] + [3:1..3:92]
fun expandGapless(rec: Recitation, from: VerseKey, to: VerseKey, t: TimingRepo) =
  (from.sura..to.sura).map { s ->
    val a0 = if (s == from.sura) from.ayah else 1
    val a1 = if (s == to.sura) to.ayah else QuranMeta.ayahCount(s)
    val clip = MediaItem.ClippingConfiguration.Builder()
      .setStartPositionMs(if (a0 == 1) 0 else t.startMs(rec.id, s, a0))
      .setEndPositionMs(if (a1 == QuranMeta.ayahCount(s)) C.TIME_END_OF_SOURCE else t.endMs(rec.id, s, a1))
      .build()
    MediaItem.Builder().setMediaId("g|${rec.id}|$s|$a0|$a1")
      .setUri("quran-audio://${rec.id}/$s").setClippingConfiguration(clip)
      .setMediaMetadata(meta(rec, s, a0)).build()
  }

// Gapped: basmala + one item per ayah; Juz 30 = 564 ayahs + 37 basmalas = 601 items
fun expandGapped(rec: Recitation, from: VerseKey, to: VerseKey, voice: Recitation?) = buildList {
  for (s in from.sura..to.sura) {
    val a0 = if (s == from.sura) from.ayah else 1
    val a1 = if (s == to.sura) to.ayah else QuranMeta.ayahCount(s)
    if (a0 == 1 && s != 1 && s != 9) add(basmalaItem(rec, s))
    for (a in a0..a1) {
      add(ayahItem(rec, s, a))                       // quran-audio://rec/s/a
      if (voice != null) add(ayahItem(voice, s, a))  // Arabic → Urdu interleave
    }
  }
}
```

Para boundaries come from metadata. Tanzil `QuranData.Juz` starts at 1:1, 2:142, 2:253, 3:93, 4:24 … 78:1, and Manzils at 1, 5, 10, 17, 26, 37, 50 [verified: tanzil.net/res/text/metadata/quran-data.js]. Ayahs per Juz range from 110 (Juz 6) to 564 (Juz 30) [verified by computation]. These are small playlists, so we always materialise them fully. A full-Quran gapped plan is 6,348 items, and we materialise that lazily in 1-surah windows.

#### 2.8.4 Repeat and hifz engine

| Need | Gapped | Gapless |
|---|---|---|
| Ayah ×N | Duplicate the ayah item N times (deterministic, pre-buffers, notification shows "2/3") | In hifz mode re-expand into **per-ayah clipped items** of the same file, then duplicate |
| Range ×M / infinite | Duplicate the sequence M times if items < 2,000; else `REPEAT_MODE_ALL` + counter in `onPositionDiscontinuity(AUTO_TRANSITION)` | Same with clipped surah items |
| Pause after each ayah ("repeat after the qari") | `setPauseAtEndOfMediaItems(true)` → `onPlayWhenReadyChanged(END_OF_MEDIA_ITEM)` → `postDelayed(play, f(duration))`. The FGS survives the pause (10-min window) | Per-ayah clipped items, same mechanism |
| Cumulative chain (1, 1–2, 1–3 …) | Expand in `PlanExpander` | Same |
| Next/prev buttons | `QuranForwardingPlayer` overrides `seekToNext/Previous` to the next *distinct* ayah (setting: ayah vs surah) | Overrides to `seekTo(t.start(next))` |
| Speed | `setPlaybackSpeed(0.5–2.0)` (pitch-preserving); keep `skipSilence` **off** because it would cut natural waqf pauses | Same |

#### 2.8.5 Follow-along highlighting and auto page-turn

- **Current ayah, gapped.** Parse it from `currentMediaItem.mediaId` on `onMediaItemTransition`; this is exact and free.
- **Current ayah, gapless.** On each (re)prepare, schedule one `PlayerMessage` per ayah start with `setPosition(itemIndex, startMs - clipStart)`, `setLooper(mainLooper)` and `setDeleteAfterDelivery(false)`. That is about 300 messages for Al-Baqarah, delivered from the playback thread. After a seek, compute the ayah with a binary search over the timing table, as quran_android does with polling in `updateAudioPlayPosition` [verified: r03 notes].
- **Word-level highlight (v1.1).** Use a Compose `withFrameNanos` loop reading `player.currentPosition` only while the reader is visible, plus word segments from QUL or quran-align. Word indices must be mapped to the Indo-Pak glyph words (r04).
- **Publishing.** The service pushes `NowPlaying(recId, sura, ayah, word?, isPlaying, scope)` into a process-wide `PlaybackStateHub` StateFlow, and updates `MediaMetadata` title through `replaceMediaItem` (e.g. "Al-Baqarah · 255"). That way the lock screen, Auto and Wear show the ayah.
- **Auto page-turn.** Map ayah to page in the chosen Indo-Pak edition (16-line Taj, 548 pages, per r03/r04) through `MushafLayoutRepo`. Turn the page when the ayah's *start* is on the next page, but only if the user hasn't touched the pager in the last 8 s. Show a "Follow recitation" chip to re-attach.

#### 2.8.6 Session, notification, devices, timers

- **`QuranPlaybackService : MediaLibraryService`** with intent filters `androidx.media3.session.MediaLibraryService` and `android.media.browse.MediaBrowserService`, `foregroundServiceType="mediaPlayback"`, and `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permissions [verified: developer.android.com/media/media3/session/serve-content].
- **Browse tree** for Auto, Wear and Bluetooth AVRCP:
  - Root has Continue · Favourites · Surahs (114) · Paras (30) · Manzils (7) · Popular (Yaseen, Rahman, Mulk, Kahf, Waqiah, Fatiha, Ayat al-Kursi) · Downloads.
  - Media IDs are plan specs (`plan:juz=3;rec=alafasy…`), resolved in `onSetMediaItems`/`onAddMediaItems`.
  - Android Auto needs `automotive_app_desc.xml` (`<uses name="media"/>`) [unverified].
  - Wear shows phone sessions automatically [unverified].
- **Notification buttons** via `setMediaButtonPreferences`: ⟲ repeat-ayah toggle, previous/next ayah, play/pause, sleep. Headset and Bluetooth buttons work through the session. Set `setHandleAudioBecomingNoisy(true)`.
- **Audio attributes:** `USAGE_MEDIA` + `AUDIO_CONTENT_TYPE_SPEECH`, so recitation pauses rather than plays at 20 % under navigation prompts [verified: AudioFocusManager.java]. Offer a "duck instead" setting for drivers.
- **Sleep timer:**
  - Options: 5–90 min, end of ayah, end of surah, end of Para. The "end of …" options use a `PlayerMessage` at the boundary.
  - Fade out over the last 10 s via `player.volume`.
  - It lives in the service, so it survives the UI.
- **Resume:**
  - Persist `(plan, itemIndex, positionMs)` on pause and every 15 s.
  - `onPlaybackResumption` rebuilds the playlist for the system "resume" card.
  - The home screen shows "Continue: Para 12 · Hud 11:58 · Alafasy".
- **Android 17:** Start playback only from a user action (app UI, notification, widget, media key). Schedules such as Friday Kahf post a notification with a Play action, never auto-play.

#### 2.8.7 Downloads and caching

| Option | Pros | Cons | Verdict |
|---|---|---|---|
| Media3 `DownloadManager` + `DownloadService` (dataSync) + non-evicting `SimpleCache` | Built-in progress, requirements, `onTimeout` handled, offline plays through the cache | Opaque cache spans (no MD5, no export, no ZIP), one `Download` per ayah (6,236 rows per reciter), needs two caches chained (download + stream) | No |
| **Own engine: OkHttp Range-resume → plain files** | ZIP per surah (114 requests instead of 6,236), MD5 manifests, file-exists = downloaded, SD-card and export, the same model quran_android and QuranApp use | About 1–1.5 weeks of work | **Yes** |

- **Hosting:**
  - Android 14+: a **UIDT `JobService`** with `setUserInitiated(true)`, `setEstimatedNetworkBytes(catalogue size)` and `setNotification`. It has no 6 h `dataSync` cap and no Android 16 quota problem.
  - Android 7–13: a **WorkManager long-running worker** with `ForegroundInfo(…, FOREGROUND_SERVICE_TYPE_DATA_SYNC)`, merged into `SystemForegroundService` in the manifest.
  - Background refresh of the catalogue and timing packs uses plain WorkManager.
- **Units:** ayah range, surah, Para, Manzil, full recitation, and "Popular pack". In gapless mode a Para downloads every surah it touches; the UI must say so (e.g. "Para 2 needs Al-Baqarah 116 MB").
- **Sizes and integrity:**
  - Show sizes from our own measured `surahBytes`, never from provider APIs.
  - Integrity checks: everyayah `000_checksum.md5`; our pipeline's SHA-256 for other hosts; `Content-Length` and `ETag`; and a decode sanity check (first frame sync).
  - Write to `*.part`, then rename atomically.
- **Location:** `getExternalFilesDirs(null)`, so the user can pick internal or SD. No permission is needed. Add `.nomedia`. Show per-reciter usage, "delete reciter", "delete everything except favourites" and "clean partials".
- **Policy:** "Wi-Fi only" on by default (Pakistani mobile data is metered). Streaming quality is Auto: 64 kbps on cellular, 128 kbps on Wi-Fi, when both editions exist.
- **Stream cache:** `SimpleCache(cacheDir/media, LeastRecentlyUsedCacheEvictor(256 MB, adjustable to 2 GB), StandaloneDatabaseProvider)`.
  - Use `DefaultLoadControl` with 60–300 s buffers for flaky networks.
  - Downloaded files bypass the cache through `ResolvingDataSource`.
  - `MirrorFailoverDataSource` retries the next `AudioSourceRef` on HTTP or IO errors.

#### 2.8.8 Reciter catalogue (v1 proposal)

**Licence status of every row today: personal use OK; commercial use and redistribution unclear** (catalogue value `PERSONAL_ONLY`). The exception is islamic.network-hosted files, whose terms allow commercial bundling with a removal-on-request caveat. Before publishing, every row needs a permission email (§3, recommendation 2).

| # | Reciter | Style | v1 layout | Primary source (template) | Mirror / alt | kbps (stream · DL) | Full size | Highlight / timing | Why (PK) |
|---|---|---|---|---|---|---|---|---|---|
| 1 | **Mishary Alafasy** (default) | Murattal | Gapped | everyayah `Alafasy_{64,128}kbps/{sss}{aaa}` | i.n. `ar.alafasy/{g}` · mirrors.quranicaudio | 64 · 128 | 0.86 / 1.71 GB | item-exact | #1 in reference app |
| 2 | Mishary Alafasy | Murattal | Gapless | mp3quran `mishary-alafasy/r1/{sss}` | n/a | 128 | ~1.7 GB | mp3quran read 123 | "Continuous" listening |
| 3 | **Abdul Basit** | Murattal | Gapped | everyayah `Abdul_Basit_Murattal_{64,192}kbps` | i.n. `ar.abdulbasitmurattal` | 64 · 192 | 0.91 / 2.70 GB | item-exact | Classic in South Asia |
| 4 | **Abdul Basit** | Mujawwad | Gapless | mp3quran `abdulbasit-abdulsamad/r1` | everyayah 128 gapped (3.39 GB) | 128 | ~3.5 GB | mp3quran read 51 | Mujawwad favourite |
| 5 | **Al-Minshawi** | Murattal | Gapped | everyayah `Minshawy_Murattal_128kbps` | i.n. `ar.minshawi` | 32 · 128 | 0.41 / 1.67 GB | item-exact | n/a |
| 6 | **Al-Minshawi with children** | Kids repeat | Gapless | mp3quran `muhammad-minshawi/r3` | QDC 168 (timed, QF terms) | 128 | ~2+ GB [unverified] | v1: none; v1.1 QUL 314 | Kids and hifz |
| 7 | **Al-Husary** | Murattal | Gapped | everyayah `Husary_{64,128}kbps` | i.n. `ar.husary` | 64 · 128 | 1.23 / 2.47 GB | item-exact | n/a |
| 8 | **Al-Husary (Muallim)** | Muallim | Gapped | everyayah `Husary_Muallim_128kbps` | quranicaudio 141 | 128 | 2.75 GB | item-exact | Teaching / hifz |
| 9 | **As-Sudais** | Murattal | Gapped | everyayah `Abdurrahmaan_As-Sudais_{64,192}kbps` | i.n. `ar.abdurrahmaansudais` | 64 · 192 | 0.61 / 1.83 GB | item-exact | Haram imam |
| 10 | **Ash-Shuraim** | Murattal | Gapped | everyayah `Saood_ash-Shuraym_{64,128}kbps` | i.n. `ar.saoodshuraym` (64) | 64 · 128 | ~0.53 / 1.06 GB | item-exact | Haram imam |
| 11 | **Saad Al-Ghamdi** | Murattal | Gapless | mp3quran `saad-ghamdi/r1/{sss}` | QUL 13 (Tarteel CDN + segments) | 128 | ~1.4–2.9 GB | mp3quran read 30 | Very popular; per-ayah only 40 kbps |
| 12 | **Maher Al-Muaiqly** | Murattal | Gapped | everyayah `Maher_AlMuaiqly_64kbps`/`MaherAlMuaiqly128kbps` | i.n. `ar.mahermuaiqly` | 64 · 128 | 0.61 / 1.21 GB | item-exact | Haram imam |
| 13 | **Abu Bakr Ash-Shatri** | Murattal | Gapped | everyayah `Abu_Bakr_Ash-Shaatree_{64,128}kbps` | i.n. `ar.shaatree` | 64 · 128 | ~0.7 / 1.43 GB | item-exact | n/a |
| 14 | **Yasser Ad-Dosari** | Murattal | Gapped | everyayah `Yasser_Ad-Dussary_128kbps` | mp3quran r1 (timed 92) | 128 | 1.50 GB | item-exact | Rising favourite |
| 15 | **Hani Ar-Rifai** | Murattal | Gapped | everyayah `Hani_Rifai_{64,192}kbps` | i.n. `ar.hanirifai` | 64 · 192 | ~0.7 / 2.15 GB | item-exact | n/a |
| 16 | **Ahmed Al-Ajmy** | Murattal | Gapped | everyayah `ahmed_ibn_ali_al_ajamy_128kbps` | i.n. `ar.ahmedajamy` | 64 · 128 | 1.51 GB | item-exact | n/a |
| 17 | **Qari Sadaqat Ali** | Murattal | Gapless | i.n. `audio-surah/128/ar.sadaqatali/{s}` | quranicaudio `sadaqat_ali/{sss}` | 128 | ~1.8 GB | **none in v1** (surah-level seek bar) | Pakistani qari; in reference app |
| 18 | **Ali Al-Huthaify** | Murattal | Gapped | everyayah `Hudhaify_{32,64,128}kbps` | i.n. `ar.hudhaify` | 64 · 128 | n/a | item-exact | King Fahd Complex licence path |
| U1 | **Urdu: Shamshad Ali Khan** | Translation voice | Gapped | everyayah `translations/urdu_shamshad_ali_khan_46kbps` | i.n. `ur.khan` (64) | 46 | 0.51 GB | item-exact | **Arabic + Urdu interleave** |
| U2 | Urdu: Farhat Hashmi (v1.1) | Word-by-word tarjuma | Gapped | everyayah `translations/urdu_farhat_hashmi` | n/a | 32 | 1.50 GB | item-exact | Al-Huda audience |

`~` sizes are derived (the measured 128 kbps size halved for 64 kbps). Unmarked sizes were measured with HEAD requests or the provider's API.

**v1.1 candidates:**
- Minshawi Mujawwad (everyayah 64/192)
- Husary Mujawwad
- Husary with children (quranicaudio 142)
- Sudais & Shuraim + Urdu (quranicaudio 67)
- Abdullah Basfar
- Muhammad Ayyub
- Warsh/Qaloon editions (mp3quran has 13 Warsh and 11 Qaloon moshafs) for non-South-Asian users

---

## 3. Recommendations

1. **Build one Media3 engine that supports both GAPPED_AYAH and GAPLESS_SURAH from v1.** Use `MediaLibraryService` + `ExoPlayer` + `QuranForwardingPlayer` + a `PlanExpander` covering Surah, Para, Manzil, Ruku, page and range. Pin `media3 = "1.11.1"` and use `media3-datasource-okhttp` with OkHttp 4.12.x, which is what it compiles against.
   - *Rationale:* gapped covers every must-have exactly. Gapless is the only good option for Ghamdi and Sadaqat Ali and gives "continuous" listening. Media3 1.11 has every primitive we need: clipping, `PlayerMessage`, pause-at-end, Compose UI and session resumption.
   - **Confidence: High.**
2. **Treat licensing as a launch gate, not an afterthought.**
   - Ship a personal/beta build streaming from everyayah and islamic.network (gapped) and mp3quran (gapless) right away.
   - Before a public Play release, send permission emails to: QF via quran.zendesk (everyayah + quranicaudio), mp3quran.net, Islamic Network (they already allow commercial bundling with reciter caveat), and Tarteel/QUL.
   - Credit every source in an "Audio credits" screen with a link back. everyayah timing files require the link-back.
   - Keep the app free of ads around recitation. That avoids the "commercial purposes" problem and the reference app's top complaint.
   - **Confidence: High.**
3. **Make the catalogue remote data:** a bundled `catalogue.json` plus a signed remote override, with per-recitation `enabled`, ordered mirrors, `licenseStatus` and `attribution`. This lets us honour a takedown ("they may ask you to remove the content") or swap a dead host without an app update. **Confidence: High.**
4. **Build an offline catalogue/timing pipeline (Python, run by us, not in-app).** It should:
   - pull mp3quran `ayat_timing` (gapless) and later QUL segments;
   - normalise intro → `ayah 0`;
   - verify monotonicity and `last_end ≤ duration + 100 ms` for all 114 surahs;
   - record `content_length`/`etag`;
   - measure real bitrate and `surahBytes`;
   - emit `timing.db` packs plus SHA-256.

   Never pair timing from one host with audio from another. **Confidence: High.**
5. **Default reciter: Alafasy (gapped 64/128). Onboarding asks for a "favourite qari" from the 18-entry list.** Expose "Arabic + Urdu translation (per ayah)" as a playback toggle using Shamshad Ali Khan. *Rationale:* Alafasy is the reference app's default. Urdu interleave is a strong Pakistan-specific differentiator the reference app can't match. **Confidence: Medium-High.**
6. **Hifz features in v1:**
   - Ayah ×N, range ×M/∞, cumulative chain, and pause-after-ayah (fixed or ×duration). Gapped uses item duplication; gapless re-expands into per-ayah clips in hifz mode.
   - Speed 0.5–2.0×.
   - Next/prev by ayah, with a setting for surah.
   - Sleep timer with an "end of ayah/surah/Para" option and fade-out.

   **Confidence: High.**
7. **Follow-along:**
   - Gapped tracks the current ayah from the item `mediaId`. Gapless uses per-ayah `PlayerMessage`s plus a binary search after seeks.
   - The process-wide `PlaybackStateHub` drives highlight and auto page-turn, which pauses for 8 s after user interaction.
   - Update `MediaMetadata` per ayah via `replaceMediaItem`.
   - Word highlight is deferred to v1.1 (QUL / quran-align, mapped to Indo-Pak words).

   **Confidence: High** (ayah), **Medium** (word mapping).
8. **Downloads: own OkHttp engine with plain files, per-surah ZIPs (everyayah) and MD5/SHA verification.**
   - Host it in a UIDT job on API 34+ and a WorkManager long-running `dataSync` worker on API ≤33.
   - Default to Wi-Fi only, with a storage manager and SD-card choice.
   - Media3 `SimpleCache` (LRU, 256 MB default) is for streaming only.

   **Confidence: Medium-High.** UIDT has no Jetpack wrapper, so budget about 3 extra days.
9. **Set MP3 extractor flags:** `DefaultExtractorsFactory().setMp3ExtractorFlags(FLAG_ENABLE_CONSTANT_BITRATE_SEEKING)` globally. Turn on `FLAG_ENABLE_INDEX_SEEKING` only for downloaded files of recitations flagged `isVbr`. Prefer CBR sources (every sampled source was CBR). **Confidence: High.**
10. **Audio focus:** `USAGE_MEDIA` + `AUDIO_CONTENT_TYPE_SPEECH` (pause, don't duck) as the respectful default. Add an opt-in "Duck under navigation" for drivers. Never auto-start audio from alarms or boot (Android 15/17). Reminders post a tap-to-play notification. **Confidence: High.**
11. **Self-host on Cloudflare R2 once permission is granted** (~70 GB for 20 recitations in both layouts ≈ $1/month; free egress) behind our own domain. Keep the original hosts as mirrors. *Rationale:* reliability, no hotlinking, honours "cache aggressively at your own edge". **Confidence: Medium.** It depends on permission, and on Cloudflare's CDN terms on audio, which name "Paid Services (e.g., the Developer Platform…)". R2 being part of that is [inferred].
12. **Qari Sadaqat Ali and other Pakistani qaris:**
    - v1 ships surah-level playback with no highlight.
    - v1.2 generates ayah timings ourselves, either with a forced aligner (`cpfair/quran-align` approach, MIT) or by contributing a segmentation job to QUL. Then add Pakistani qaris like Qari Waheed Zafar Qasmi, but only from a licensed source; archive.org rips are not licensed.

    **Confidence: Medium.**

---

## 4. Risks and open questions

**Risks**
- **Licensing ambiguity (highest risk).** Reciters and publishers hold the copyright. Hosts range from "personal only" (quranicaudio) to silent (everyayah, mp3quran). QF terms forbid caching over a week outside Content Sync, which makes QDC timings and audio unsuitable for offline packs without a QF agreement.
- **Hotlink fragility.** Hosts change silently. Examples:
  - quranicaudio's API returned another qari's sizes and tags for Sadaqat Ali.
  - The everyayah Alafasy "128kbps" folder holds a 192 kbps file.
  - mp3quran moved `server8` to `cdn` with a 301.
  - A re-encode shifts timings.

  Mitigation: measured catalogue, `etag`/length guards, mirrors, remote kill-switch.
- **Timing accuracy.** mp3quran's timings are machine-made, and edge cases (merged ayahs, repeated phrases in Mujawwad) may drift. Gapped is the fallback for hifz.
- **Indo-Pak mapping.** Audio uses Hafs/Kufan 6,236 numbering. If r04/r05's Indo-Pak layout marks ayah ends differently anywhere, highlighting misaligns. Ruku numbering (Tanzil 556) may differ from Indo-Pak prints.
- **Platform churn.** Android 16 WorkManager quotas, the Android 17 audio hardening, and Play FGS declarations. Test with `adb shell cmd audio set-enable-hardening enable` and `device_config … data_sync_fgs_timeout_duration`.
- **Storage and data cost** for Pakistani users on 32–64 GB devices with metered data. Defaults must be conservative (64 kbps on cellular, Wi-Fi-only downloads, visible sizes).
- **Large playlists.** A full-Quran gapped plan is 6,348 items, more with repeats. Use a lazy window, or Auto/notification queues get heavy.

**Open questions**
1. Will the owner publish free and ad-free, or monetise? This decides whether quranicaudio/QF-hosted audio is ever acceptable and how hard to push for permissions.
2. Who emails QF, mp3quran.net, Islamic Network and Tarteel, and are we willing to self-host (R2 ≈ $1/month plus a domain)?
3. Is "Arabic + Urdu (Shamshad Ali Khan)" the right Urdu voice? Are there licensed alternatives (e.g., the King Fahd Complex's audio translations of meanings, p=4973, unreachable today), or is a Fateh Muhammad Jalandhari recording available from a rights-holder?
4. Which Pakistani qaris beyond Sadaqat Ali does the owner want, and can we get the recordings directly from the qari or publisher?
5. Should "next/previous" default to ayah or surah? Reference-app users think in surahs; hifz users think in ayahs.
6. Do we need Warsh/Qaloon at all for a South-Asian audience? I recommend no for v1.
7. Confirm with r04/r05 that the chosen Indo-Pak edition (16-line Taj, 548 pages) maps 1:1 onto 6,236 Hafs ayahs for highlighting.

---

## 5. Sources

| URL | What I verified there |
|---|---|
| https://everyayah.com/data/recitations.js | 79 folders, names, nominal bitrates, `ayahCount` |
| https://everyayah.com/data/Alafasy_128kbps/001001.mp3 (and 002255, 000_versebyverse.zip, zips/NNN.zip, 000_checksum.md5) | BunnyCDN headers, CORS `*`, sizes (1,707,811,817 B full ZIP), 6,350-line MD5, per-surah ZIPs, real bitrates via frame parse |
| https://everyayah.com/ , /old_index.html, /recitations_ayat.html, /recitations_pages.html | Site structure; ZIP/MD5 links; page-MP3 sets; contact → quran.zendesk.com |
| https://everyayah.com/data/timings_files/000_disclaimer.txt | "(C) VerseByVerseQuran.com … You must link back to our site…" (licence URL now 404) |
| HEAD `…/000_versebyverse.zip` for 24 folders | Full-recitation sizes in §2.3 |
| https://quranicaudio.com/api/qaris , /api/sections , /api/surahs , /api/qaris/{id}/audio_files/mp3 | 177 qaris, 4 sections, per-file size/duration/bitrate (and the Sadaqat Ali metadata error) |
| https://quranicaudio.com/about | "personal use … may not use these files for commercial purposes" |
| https://download.quranicaudio.com/quran/sadaqat_ali/NNN.mp3 (HEAD ×114) | Real sizes (~1.75 GB) |
| https://api.quran.com/api/v4/resources/recitations , /recitations/7/by_chapter/1 , /chapter_recitations/7/1 | 12 per-ayah recitations; QDC chapter URLs; key-less access |
| https://api.qurancdn.com/api/qdc/audio/reciters and …/reciters/{7,12,168}/audio_files?chapter=N&segments=true | 14 gapless reciters; timing/segment format; basmala merged into ayah 1 |
| https://verses.quran.com/Alafasy/mp3/001001.mp3 | Per-ayah CDN for v4 (same bytes as everyayah) |
| https://api-docs.quran.foundation/docs/quickstart/ | OAuth2 client-credentials, `x-auth-token`/`x-client-id`, secret must not be in mobile apps |
| https://api-docs.quran.foundation/legal/developer-terms/ | Licence scope, 1-week cache limit, no redistribution, ads/IAP allowed, last updated 2026-10-11 |
| https://www.mp3quran.net/api/v3/reciters?language=eng , /riwayat , /ayat_timing/reads , /ayat_timing?surah=N&read=R , /ayat_timing/soar?read=R , /radios | 243 reciters/289 moshafs; 115 timed reads; timing format incl. `ayah:0`; Urdu radio streams |
| https://www.mp3quran.net/eng/api , /eng/about (and /eng/terms → 404) | API docs; no licence text found |
| https://cdn.mp3quran.net/audio/…/NNN.mp3 (HEAD and samples) | Cloudflare, CORS `*`, CBR 128k; durations vs timing check |
| https://api.alquran.cloud/v1/edition?format=audio , /v1/surah/1/ar.sadaqatali , /v1/surah/112/ar.alafasy , /v1/surah/1/ur.khan | 190 audio editions; URL fields; `audioSecondary` |
| https://alquran.cloud/terms-and-conditions | Recitation terms (verbatim quotes in §2.1), CDN fair use |
| https://alquran.cloud/cdn ; https://cdn.islamic.network/quran/info/by-surah/info.json | CDN patterns, bitrates 192/128/64/48/40/32, 159 surah editions |
| https://cdn.islamic.network/quran/audio/{br}/{edition}/262.mp3 (HEAD ×70) | Per-edition bitrate availability; byte-identity with everyayah |
| https://qul.tarteel.ai/resources/recitation , /resources/recitation/418 , /faq , /credits | 133 recitations / 59 with segments; segment format; commercial-use FAQ; credits to everyayah/quranicaudio/Collin Fair |
| https://qul.tarteel.ai/api/v1/audio/surah_recitations , /surah_recitations/{1,7} , /surah_segments/{1,7}?surah=N , /ayah_recitations | Key-less API; 46 fully segmented gapless recitations; `audio-cdn.tarteel.ai` |
| https://audio-cdn.tarteel.ai/quran/surah/abdulBasit/mujawwad/mp3/001.mp3 | Cloudflare CDN, cache headers |
| git clone https://github.com/cpfair/quran-align | MIT code; CC BY 4.0 word-timing data; format |
| https://raw.githubusercontent.com/AlfaazPlus/QuranAppInventory/master/audio_timings/al_afasy.json.gz | Timing JSON format and size (541,693 B) |
| r03 clones: quran_android `MadaniPageProvider.kt`, `AudioUtils.kt`, `SuraTimingDatabaseHandler.kt`; QuranApp `available_recitations_info_v2.json`, `available_recitation_translations_info_v2.json` | Gapped/gapless qari model, basmala logic, timing schema; QuranApp hosts per-surah audio on GitHub Releases (`dabatase/*`) |
| r01 APK: `free.alquran.holyquran.apk` → `res/raw/qari_list.json` | Reference app's 20 reciters and hosts |
| https://tanzil.net/res/text/metadata/quran-data.js | Juz (30) and Manzil (7) start points; 556 rukus |
| https://dl.google.com/android/maven2/androidx/media3/media3-exoplayer/maven-metadata.xml (+ session, ui-compose(-material3), datasource-okhttp, exoplayer-workmanager, work-runtime-ktx, room) | Media3 1.11.1 latest; WorkManager 2.12.0; Room 2.8.5 |
| https://dl.google.com/android/maven2/androidx/media3/media3-datasource-okhttp/1.11.1/…pom , media3-session-1.11.1.pom | OkHttp 4.12.0 dependency; Kotlin stdlib 2.2.10 |
| https://repo1.maven.org/maven2/com/squareup/okhttp3/okhttp/maven-metadata.xml | OkHttp 5.5.0 latest |
| https://raw.githubusercontent.com/androidx/media/release/RELEASENOTES.md | 1.11.1/1.11.0/1.10.0 changes (MP3 gapless, Compose UI, session), minSdk 23 since 1.9.0 |
| raw androidx/media `Mp3Extractor.java`, `ClippingMediaSource.java`, `MediaItem.java`, `ExoPlayer.java`, `PlayerMessage.java`, `SilenceMediaSource.java`, `MediaSessionService.java`, `MediaSession.java`, `common/audio/AudioFocusManager.java`, `DownloadService.java`, `ResolvingDataSource.java`, `CacheDataSource.java`, `LeastRecentlyUsedCacheEvictor.java`, `WorkManagerScheduler.java`, `OkHttpDataSource.java`; demos `main`/`session_service` manifests | API existence and behaviour cited in §2.5, §2.7, §2.8 |
| https://developer.android.com/media/media3/session/serve-content | `MediaLibraryService` manifest and callbacks |
| https://developer.android.com/develop/background-work/services/fgs/service-types | `dataSync`/`mediaPlayback` types and permissions; Android 15 BOOT_COMPLETED restriction |
| https://developer.android.com/develop/background-work/services/fgs/timeout | 6 h / 24 h for `dataSync`; `onTimeout`; reset on foreground |
| https://developer.android.com/about/versions/15/changes/datasync-migration | Alternatives (UIDT, WorkManager) |
| https://developer.android.com/develop/background-work/background-tasks/uidt | UIDT API 34+, permission, scheduling rules, no Jetpack support |
| https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/long-running | Long-running workers; Android 16 job-quota warning; recommends UIDT |
| https://developer.android.com/about/versions/17/changes/bg-audio | Android 17 background audio hardening; Media3 recommendation; test commands |
| https://developers.cloudflare.com/r2/pricing/ | $0.015/GB-month, free egress, free tier |
| https://www.cloudflare.com/service-specific-terms-application-services/ | CDN large-file / audio restriction wording |
| https://archive.org/advancedsearch.php (queries for Jalandhari, Urdu translation, Sadaqat Ali, Minshawi children) | Availability and missing licences of Urdu/PK audio uploads |
| WebSearch excerpt of https://qurancomplex.gov.sa/en/?p=3979 (direct fetch: 503/reset) | King Fahd Complex free-use wording (partially verified) |
