# 10 · Gap 03: Audio rights per reciter, and gapless ayah-timing validation

Researcher key: `gap-audio-rights-and-gapless-timing` · Date: 2026-10-19

**Scope.** Must-have #2 is "favourite qari for a surah, Para or verse, with highlighting". It currently depends on audio hosts whose terms are "personal only" or not stated. Gapless highlighting for Ghamdi, Abdul Basit Mujawwad and gapless Alafasy depends on mp3quran timings that report 06 validated for one surah only. This report settles two things: which reciters a public, free, ad-free v1 can ship, and whether gapless highlighting is a v1 feature.

> **Method.** Everything below was measured or fetched in this session unless it is tagged otherwise.
> - **Rights.** I fetched the terms, contact and credits pages of every host:
>   - mp3quran.net
>   - alquran.cloud / islamic.network, including **8 permission threads on its community forum** with the maintainer's replies
>   - QUL: the copyright page of **all 130** recitation resources, plus Tarteel's terms
>   - quranicaudio, everyayah and the Quran Foundation (QF) developer terms
>   - Alafasy's official company site
>   - the Hi-Tech Music listings on Qobuz and Apple Music, and the label's Dailymotion API profile
>   - IslamHouse
>
>   qurancomplex.gov.sa returns 503 or connection reset from this egress. web.archive.org and archive.ph are blocked or rate-limited, so the Complex's wording comes from search-engine excerpts. Each such use is tagged.
> - **Timing data.** For mp3quran reads **123 (Alafasy), 30 (Ghamdi) and 51 (Abdul Basit Mujawwad)** I fetched all 114 surahs:
>   - `ayat_timing?surah=N&read=R`
>   - a `HEAD` request on each CDN file (Content-Length, ETag)
>   - a ranged GET of the **first 64 KB**, with my own MPEG frame-header parser
>   - a ranged GET of the **last 512 B**, for ID3v1 and APE tags
>
>   That is 342 timing files, 342 HEADs and 684 ranged GETs.
> - **Same checks on 13 more reads.** I ran the structural checks on 13 more timed reads that matter for the catalogue: 54, 31, 118, 112, 53, 4, 92, 89, 5, 74, 109, 1, 159. I also fetched surah 9 for **all 115** timed reads.
> - **Audio checks.** I decoded 9 full mp3quran files with ffmpeg: surahs 1, 36 and 67 for each of the three reads.
>   - I confirmed CBR by walking every frame, and compared decoded duration against frame count and the Content-Length estimate.
>   - I tested whether ayah boundaries fall in pauses, using 10 ms energy envelopes, with **ayah midpoints as a control**.
>   - For Ghamdi I compared against QUL segments (recitation 13) for 6 surahs, and cross-correlated the QUL and mp3quran audio for 3 of them to test whether they are the same recording.
>   - I ran an independent check over all 6,236 ayahs that catches off-by-one ayah labels: ayah duration correlated with letter count, under label shifts of −1, 0 and +1.
> - **Hygiene.** All Python ran with `python3 -I`. No third-party code was run.
>
> Scripts are in `<scratch>/gap-audio-rights-and-gapless-timing/scripts/` and data in `…/gap-audio-rights-and-gapless-timing/data/`: `validation_*.json` and `decode_report.txt`. Audio samples are in `…/audio/`.

Tags:
- **[verified: url]** means fetched or inspected in this session.
- **[search-excerpt: url]** means the page could not be fetched and the text comes from a search engine's index of it.
- **[unverified]** means from memory, or from a third party I could not check.
- **[inferred]** means reasoning from verified facts.

---

## 1. Executive summary

1. **No catalogue row has a written licence from a reciter or a label.** The most permissive written terms come from **mp3quran.net**, and reports 06 and 09 missed them.
   - Its privacy page says: *"Copyrights: All rights are available to everyone, and we allow any visitor or developer to copy any material or use any link on the websites"*.
   - Its contact page tells developers to *"share the following URL for Apple, google or any social website to a prove the copyrights is for all: https://mp3quran.net/eng/privacy"* [verified].
   - This is a host grant, written to serve as app-store evidence. It cannot pass on rights the host does not hold. **High.**
2. **Islamic Network adds no chain of title beyond everyayah.** Its maintainer posted on the forum in Sept–Oct 2026:
   - *"90%+ of the Quran recitations are sourced from the public domains and the remainder are licensed"*.
   - *"Our terms and conditions are not evidence of any explicit authorization"*.
   - *"you will have to reach out to the reciters directly… We do not have contact details"*.
   - *"we cannot provide individual confirmations"*.

   alquran.cloud's own credits list *"Every Ayah for the ayah audio files on the CDN"* and *"Quran Central for the surah audio files"* [verified]. **Downgrade islamic.network from "commercial bundling OK" to "free app OK by host terms, rights unknown."** **High.**
3. **QUL/Tarteel audio has no licence information.** All **130 of 130** QUL recitation copyright pages say *"We don't have copyright information for this resource."* QUL's "Terms of use" link goes to Tarteel's 2021 ToS, which bars copying or downloading "Service Content… audio" except as permitted [verified]. **Verdict: unknown.**
4. **The King Fahd Complex gives the cleanest licence, at producer level, but we could not reach it.**
   - It offers its recordings *"for free general use"* in apps, websites and broadcasts, inside and outside Saudi Arabia [search-excerpt].
   - It publishes **full per-ayah and per-surah downloads** for **Ali Al-Huthaify, Maher Al-Muaiqly, Muhammad Ayyub, Khalid Al-Muhanna and Ibrahim Al-Akhdar**, plus Huthaify in Shu'bah and Qaloon. Salah Al-Budair is not among them [search-excerpt].
   - The site is geo-blocked from our egress (503 or reset). The owner should fetch the ZIPs from Pakistan and self-host them.
5. **Alafasy's own company asserts rights, so it is the most valuable email.** Alafasy Berhad (Malaysia) runs the official app. Its ToS says the app's *"Qur'an text, translations, audio… are owned by Alafasy Berhad or licensed partners. Unauthorized use is strictly prohibited."* Contact: **ai@alafasy.co** [verified]. Alafasy is our default reciter, so this is the single most valuable permission email.
6. **The Shamshad Ali Khan Urdu voice is label-owned.** It comes from *"The Holy Quran (Complete with Urdu Translation)"* by Qari Waheed Zafar Qasmi, Fateh Muhammad Jalandhari and Shamshad Ali Khan: *"(C) 1998 Hi-Tech Music Ltd. (P) 1998 Hi-Tech Music Ltd"*, 115 tracks, 45:25:55, still sold on Qobuz and Apple Music [verified]. everyayah's per-ayah files carry the ID3 comment `www.AllahsQuran.com` [verified]. **Personal-only until Hi-Tech grants permission.** The Complex's Urdu meanings audio covers **only al-Fatiha and Juz 'Amma** [verified: IslamHouse].
7. **Timing validation passes for the three target reads, with one defect.** Across 114 surahs each, all 6,236 Kufan ayahs are present for reads 123 and 30. Starts increase strictly, there are 0 overlaps and 0 gaps, and **the last `end_time` is at or before the file duration in 342 of 342 files** (worst case −137 ms) [verified].
   - **Defect:** read 51, surah 9, is off by one. "ayah 0" is really 9:1, and 9:129 is missing.
   - **How it was found:** the correlation between duration and letter count is 0.08 as labelled and 0.96 when shifted by +1.
   - **The same surah-9 pattern** (ayah 0 present, 9:129 absent) appears in **Shuraim 31, Huthaify 74 and Minshawi 112** [verified].
8. **The timings match the audio.** In the 9 decoded files:
   - Every file is pure CBR with no Xing header. Decoded duration, frame count and the Content-Length estimate agree to ≤8 ms.
   - **95–100 % of ayah boundaries fall inside a pause** for Murattal surahs 36 and 67 and all Mujawwad surahs. For ayah midpoints (the control) the figure is 0–17 %.
   - mp3quran marks each start at the **beginning of the pause**. Speech starts +0.3–0.5 s later in Murattal and +2.1–3.4 s later in Mujawwad.
   - Two of Alafasy's 6 al-Fatiha boundaries are about 0.8 s early.
9. **mp3quran's Ghamdi is the same recording as QUL's.** Envelope correlation is 0.993–0.996, the lag is constant (3.83, 5.45 and 6.01 s) and there is no drift across 13 one-minute windows.
   - QUL's files are trimmed to start at ayah 1, with no isti'adha or basmala. They are 128 kbps / 44.1 kHz; mp3quran's are 96 kbps / 22.05 kHz.
   - mp3quran's ayah starts are 190–365 ms (median) earlier than QUL's.
   - For **Alafasy and Abdul Basit Mujawwad, QUL and mp3quran serve different edits**, so timings must never be mixed. **High.**
10. **Gapless highlighting is a v1 feature, with conditions.** It applies to reads that pass the offline pipeline. The pipeline must:
    - move the intro to ayah 0;
    - detect and fix label shifts;
    - clamp end overshoots of 1 s or less;
    - disable highlighting for each surah that fails;
    - optionally snap each start to the speech onset.

    Of 16 reads checked, **8 pass clean** (123, 30, 54 after clamping, 4, 89, 109, 159, and 53 structurally, though 53 is too low quality at 32 kbps / 11 kHz). **Read 51 passes after one fix. Six fail in 1–6 surahs. Read 74 fails everywhere** (it drops the last ayah in 112 surahs). **High.**
11. **Shortlist to ship publicly now** (free, ad-free, credited, remote kill-switch), all gapless from mp3quran with highlighting:
    - Alafasy (123)
    - Saad Al-Ghamdi (30)
    - Abdul Basit Mujawwad (51, fixed)
    - Sudais (54)
    - Abu Bakr Ash-Shatri (4)
    - Hani Ar-Rifai (89)
    - Muhammad Ayyub (109)
    - Khalid Al-Muhanna (159)

    Plus, with highlighting turned off in the bad surahs: Husary 118, Minshawi 112 and Shuraim 31.

    Verse-by-verse (gapped) everyayah/islamic.network files can ship in the free app under the islamic.network terms, marked `PENDING`. **Not public yet:** Shamshad Ali Khan Urdu, Husary Muallim, Qari Sadaqat Ali (both stay in the personal build). **Medium-High.**
12. **Minimum permission emails: 3 core, plus 2 if their content ships.**
    - **Core:** Alafasy Berhad `ai@alafasy.co`; Quran Foundation `developers@quran.com` (for everyayah, quranicaudio and QDC); King Fahd Complex `info@qurancomplex.gov.sa` [unverified].
    - **Conditional:** Hi-Tech Music `info@hi-techmusic.com` / `h.azmat@hi-techmusic.com` for the Urdu voice; Qari Sadaqat Ali `qarisadaqat@hotmail.com` [unverified].
    - **No email:** mp3quran (archive its statement and send a courtesy form message); Islamic Network (it declines to confirm).

---

## 2. Detailed findings

### 2.1 Written terms by host (quoted)

| Host | Decisive text (verbatim) | What it permits | Verdict class | Source |
|---|---|---|---|---|
| **mp3quran.net** (CDN `cdn.mp3quran.net`, API v3) | Privacy page: *"Copyrights: All rights are available to everyone, and we allow any visitor or developer to copy any material or use any link on the websites… All above policies apllied on all other websites thats owned by mp3quran.net: mp3quran.org qurango.net"*. Arabic: *"الحقوق: جميع الحقوق متاحة للجميع و يحق لأي زائر أو مطور استخدام اي مادة أو رابط من الموقع"*. Contact page: *"Important Note: All rights are available to everyone, and we allow copying any material on the site or using any URL on the website you can share the following URL for Apple, google or any social website to a prove the copyrights is for all: https://mp3quran.net/eng/privacy"* | Copying, hotlinking and redistribution, with no commercial restriction | **public-OK (host grant)** | [verified: www.mp3quran.net/eng/privacy, /eng/contact-us] |
| **alquran.cloud / cdn.islamic.network** | *"Recitations are licensed to us by the reciters or their estates for free, non-commercial redistribution at the bitrates we publish. You may stream, embed and download them for personal and educational use. You may bundle them into a commercial product, but please note that copyrights lie with the reciters and they may ask you to remove the conent."* (Last updated 14 June 2026). Also: *"The Quran text and audio provided has been originally retrieved from GlobalQuran.com. All audio files used on this website and third party libraries own and retain their respective copyrights."* Credits: *"Quran Central for the surah audio files on the CDN · Every Ayah for the ayah audio files on the CDN"* | Streaming and per-user downloads; "commercial bundling" with a takedown caveat | **free-app-OK (host terms; no rights evidence)** | [verified: alquran.cloud/terms-and-conditions, /contributors] |
| ″ maintainer replies on the forum (user `meezaan`) | d/296 (2026-09-30): *"90%+ of the Quran recitations are sourced from the public domains and the remainder are licensed."* d/303 (2026-10-08): *"From Islamic Network terms, there is no restriction… you will have to reach out to the reciters directly if you want their written consent. We do not have contact details."* d/307 (2026-10-14): *"Mishary Alfasy's recitation is a public domain recitation and is available online abundantly everywhere, including everyayah.com. Our terms and conditions are not evidence of any explicit authorization… if you start making money from his recitations, I think he would be well within his rights to ask you to remove it."* d/291 (2026-09-30): *"we cannot provide individual confirmations."* d/310 (2026-10-14), on a per-user download of all 6,236 files for one reciter: *"There are no restrictions for option b, but please be mindful of the rate limits."* d/304: *"Please stick to this website for Islamic Network related questions."* | "Public domain" here means "publicly available" [inferred]. Per-user full-reciter downloads are explicitly OK | Same as above. **Do not email them** | [verified: community.islamic.network/api/discussions/{291,296,303,304,307,310}] |
| **everyayah.com** | No site licence. Its only written term is on timing files: *"(C) VerseByVerseQuran.com · You must link back to our site from your product and web-site to use these timings."* "Contact Us" links to `https://quran.zendesk.com/hc/en-us` (QF support) | Nothing explicit | **unknown** | [verified: everyayah.com, /data/timings_files/000_disclaimer.txt] |
| **quranicaudio.com** (QF) | *"Mp3s on this site may be downloaded and used for personal use free of charge. However, you may not use these files for commercial purposes as many of these files have rules and regulations that prevent their sale except by the publishing companies. Files on QuranicAudio.com come from many different sources. Many of them are hand ripped from cds. Others have been downloaded from… mp3quran, tvquran, and others."* | Personal use | **personal-only** | [verified: quranicaudio.com/about] |
| **QF API / QDC** (`api.quran.com`, `api.qurancdn.com`) | Developer Terms (last updated 2026-10-11): developers must not *"Cache or store QF Content longer than 1 week unless it is obtained and maintained through the Content Sync APIs…"*. QF Content includes *"audio"*. Contact: *"you can email developers@quran.com"* | Online use via an API client; no offline packs beyond 7 days | **free-app-OK online only (needs our backend)** | [verified: api-docs.quran.foundation/legal/developer-terms; email decoded from page] |
| **QUL** (`qul.tarteel.ai`, `audio-cdn.tarteel.ai`) | FAQ: *"Yes, you can use QUL data in commercial projects. However, please review the licensing terms for each resource."* Every recitation resource (130 of 130) shows *"We don't have copyright information for this resource."* Tarteel ToS (Last updated April 2, 2021): *"All content available through the Service, including… audio and other files… are the proprietary property of Tarteel or its licensors. No Service Content may be modified, copied, distributed… downloaded, scraped… other than as expressly permitted"* | Unclear | **unknown** | [verified: qul.tarteel.ai/faq, /resources/{id}/copyright ×130, www.tarteel.ai/terms] |
| **King Fahd Complex** (qurancomplex.gov.sa) | Search excerpts of its pages: *"يتشرف المجمع بإتاحة النسخ الرقمية من التسجيل الصوتي… للاستخدام العام مجاناً"*, *"والمجمع ليس مسؤولاً عن الأخطاء الفنية، أو البرمجية التي تحدُثُ من استخدام النسخة المذكورة"*. English summary of /en/?p=3979: free general use "in computer apps, broadcasting channels, internet websites, and government and private sectors, inside and outside Saudi Arabia", issued "under approval from the Minister of Islamic Affairs". Privacy policy: content is protected *"باستثناء… الملفات الحاسوبية… التي يصرح المجمع بإتاحتها للاستخدام العام"* | Free use in apps (producer grant) | **public-OK (producer), pending a direct read** | [search-excerpt: qurancomplex.gov.sa/en/?p=3979, /books-audios/, /privacypolicy/; direct fetch 503 or reset, Wayback blocked] |
| **Alafasy Berhad** (official Alafasy app company) | ToS §6: *"All content and materials in the App, including Qur'an text, translations, audio, UI/UX design, logos, and documentation, are owned by Alafasy Berhad or licensed partners. Unauthorized use is strictly prohibited."* Company: ALAFASY BERHAD 202501010149 (1611563-T), Subang Jaya, Malaysia; email `ai@alafasy.co`, +603 8682 3105 | Nothing for third parties. This asserts rights over the app's audio | Rights-holder signal → **ask** | [verified: www.alafasy.co/terms-of-use, /contact-us] |
| **Hi-Tech Music Ltd** (Urdu album label) | *"(C) 1998 Hi-Tech Music Ltd. (P) 1998 Hi-Tech Music Ltd"*. Credits: Qari Waheed Zafar Qasmi; Maulana Fateh Mohd. Jalandri (composer credit, i.e. the translation); Shamshad Ali Khan (producer / main artist); 115 tracks, 45:25:55. Contact (label's own profile): *"U.K. Office: info@hi-techmusic.com · Pakistan Office: h.azmat@hi-techmusic.com"* | Commercial catalogue | **personal-only** for Shamshad Ali Khan audio | [verified: qobuz.com album iivc35k5nlpza, music.apple.com/…/830178975, api.dailymotion.com/user/hitechmusicltd] |

### 2.2 Provenance chain (who copied from whom)

| Object | Evidence | Consequence |
|---|---|---|
| islamic.network per-ayah files | Credits: "Every Ayah for the ayah audio files on the CDN". Report 06 found the files byte-identical (2:255 Alafasy = 831,865 B) | i.n. holds no licence beyond everyayah's, which has none [inferred] |
| islamic.network surah files (e.g. `ar.sadaqatali`) | ID3 `COMM` = `https://www.qurancentral.com`. Credits: "Quran Central for the surah audio files" | QuranCentral's terms could not be read (JS app) |
| everyayah Urdu Shamshad Ali Khan | ID3: `artist=Urdu - Shamshad Ali Khan`, `album=Noble Quran`, `comment=www.AllahsQuran.com` | Ripped or split from the ℗1998 Hi-Tech album [inferred] |
| quranicaudio | "hand ripped from cds… downloaded from… mp3quran, tvquran" | Personal-only, as stated |
| mp3quran files | ID3 `TSSE=Lavf59.27.100`, `TXXX comment www.mp3quran.net`, `TDRC 2007`. **Every file of reads 123, 30 and 51 has Last-Modified 27 Sep 2026** (16:55–20:01 GMT) | Bulk re-encode or re-upload 3 weeks ago, yet timings still align (§2.5). **CDN files change silently, so pin ETag and Content-Length per file** |
| QUL Ghamdi (`audio-cdn.tarteel.ai/…/ghamadi/murattal`) | Same performance as mp3quran read 30 (§2.5.4). 128 kbps / 44.1 kHz with a LAME `Info` tag; lead-in trimmed; Last-Modified 29 Mar 2026 | HQ alternative. Same underlying rights question |

### 2.3 King Fahd Complex: what it publishes

All rows below are [search-excerpt]; the pages were indexed by search engines but cannot be fetched from here.

| Reciter (riwayah) | Complex page | Full per-ayah download | Full per-surah download | Also available elsewhere (unknown if the same recording) |
|---|---|---|---|---|
| Ali Al-Huthaify (Hafs) | `qurancomplex.gov.sa/quran-audio-hafs-huthify/`, `/sounds-hafs-huthify/` | 803.55 MB | 802.21 MB | everyayah `Hudhaify_*`, i.n. `ar.hudhaify`, mp3quran r1 (timing read 74 **fails**) |
| Ali Al-Huthaify (Shu'bah) | listed on `/sounds/` | ✓ | ✓ | mp3quran r3 (read 305) |
| Ali Al-Huthaify (Qaloon) | `/en/sounds-qaloon-huthify/` | 760.66 MB | ✓ | mp3quran r2 (read 75) |
| Maher Al-Muaiqly (Hafs) | `/sounds-hafs-muaiqly/` | 724.25 MB | 1.23 GB | everyayah/i.n. `ar.mahermuaiqly`; mp3quran r1 (no timing) |
| Muhammad Ayyub (Hafs) | `/quran-audio-hafs-ayyoub/` | 916.75 MB | 870 MB | i.n. `ar.muhammadayyoub`; mp3quran r1 (read 109 **passes**) |
| Khalid Al-Muhanna (Hafs) | `/sounds-hafs-muhanna/` | 1.04 GB | 1.01 GB | mp3quran r1 (read 159 **passes**) |
| Ibrahim Al-Akhdar (Hafs) | `/en/sounds-hafs-akhdar/` | 1.18 GB | 1.12 GB | i.n. `ar.ibrahimalakhdar` (surah); mp3quran r1 (read 1: 3 bad surahs) |
| Urdu meanings audio | IslamHouse item 385256 (Complex as source) | n/a | **Fatiha + Juz 'Amma only** (`d1.islamhouse.com/data/ur/ih_sounds/chain/Translation_of_alFatihahs_and_Ammah_Part_meanings/ur_001_AlFatiha.mp3` …) | [verified: islamhouse.com/ar/audios/385256/] |

- Salah Al-Budair, Sudais, Shuraim and Ghamdi are **not** Complex publications in any result.
- The direct ZIP URLs could not be extracted, because the pages are unreachable. I could not verify that the mp3quran, everyayah or i.n. copies are the Complex's recordings; that needs an audio fingerprint once the Complex files are in hand.

### 2.4 Per-reciter rights and timing table (v1 catalogue from 06 §2.8.8, plus the Complex reciters)

How to read this table:
- **Verdicts:** "public-OK" means a written host or producer grant that covers app use. "free-app-OK" means host terms allow a free, non-commercial app. "personal-only" means the terms or the rights-holder exclude public redistribution. "unknown" means there are no terms.
- **Timing verdicts** are mine (§2.5).
- **"Pkg"** means everyayah or i.n. per-ayah (gapped) files.

| # | Reciter / style | Best host and quoted basis | Verdict | Permission contact | Timing source | Validation (this session) | **Recommended v1 layout** |
|---|---|---|---|---|---|---|---|
| 1 | **Mishary Alafasy** (Murattal, default) | mp3quran r1 (*"All rights are available to everyone"*). Gapped: i.n. `ar.alafasy` (*"may bundle…"*). Rights-holder Alafasy Berhad says *"owned by Alafasy Berhad or licensed partners"* | public-OK (host) · rights-holder unconfirmed | **ai@alafasy.co** | mp3quran read 123 | **PASS.** 6,236/6,236; 0 overlaps, 0 gaps; no ayah 0 (intro is an implicit gap, ayah 1 at 5.7–14.9 s, median 7.0 s); last end −513 to −4,632 ms vs duration; 128 kbps / 44.1 kHz CBR; 1.72 GB / 29.8 h. Audio: 97–99 % of boundaries in pauses (s36, s67), 2 of 6 al-Fatiha boundaries ~0.8 s early | **Gapless primary** (mp3quran) with highlight. Verse-by-verse (Pkg) secondary for hifz, marked `PENDING` |
| 2 | Alafasy gapless | (= row 1) | | | | | Merge rows 1 and 2 into one reciter with two layouts |
| 3 | **Abdul Basit** Murattal | i.n. `ar.abdulbasitmurattal` (Pkg); mp3quran r3 | free-app-OK (Pkg) / public-OK (mp3quran) | QF (everyayah) | mp3quran read 53 | Structure PASS, but the file is **32 kbps / 11.025 kHz MPEG-2.5** (0.45 GB) → quality fail | **Pkg gapped** (64/192 kbps), item-exact. Do not use mp3quran r3 |
| 4 | **Abdul Basit** Mujawwad | mp3quran r1 | public-OK (host) | — | mp3quran read 51 | **PASS after 1 fix.** Ayah 0 present in all 114 surahs (al-Fatiha ayah 0 = isti'adha, correct). **Surah 9 is off by one** (ayah 0 = real 9:1; 9:129 missing; corr 0.08 → 0.96 after +1). Last end −137 to −16,607 ms; 128 kbps CBR; 3.49 GB / 60.6 h. Audio: 99–100 % in pause; speech onset 2.1–3.4 s after the marked start | **Gapless primary** + surah-9 relabel + onset snap |
| 5 | **Minshawi** Murattal | i.n. `ar.minshawi` (Pkg); mp3quran r1 | free-app-OK / public-OK | QF | mp3quran read 112 | **PARTIAL.** Surah 9 off by one, plus end overshoots in surahs 9 (+8,155 ms), 52 (+2,578 ms) and 53 (+1,843 ms; 192 kbps replacement file) | Gapless, highlighting off in s52 and s53, s9 relabelled; or Pkg |
| 6 | **Minshawi with children** | mp3quran r3 "Mo'lim" (114 surahs) | public-OK (host) | — | none on mp3quran; QDC 168 (QF terms), QUL 314 (unknown) | n/a | **Surah-level, no highlight** in v1 (or hold for v1.1) |
| 7 | **Husary** Murattal | i.n. `ar.husary` (Pkg); mp3quran r1 | free-app-OK / public-OK | QF | mp3quran read 118 | **PARTIAL.** al-Fatiha mislabelled (ayah 0 = 0–11.7 s swallows the basmala; 1:7 missing). Surah 21 file is 48 kHz and timing overshoots by +23.8 s (file swapped) | Gapless, highlighting off in s1 and s21; or Pkg |
| 8 | **Husary Muallim** | everyayah only (Pkg); quranicaudio 141; QDC 12. Not on mp3quran or i.n. | unknown / personal-only | QF `developers@quran.com` | QDC 12 (online only) | n/a | **Personal build only** until QF replies |
| 9 | **Sudais** | i.n. `ar.abdurrahmaansudais` (Pkg); mp3quran r1 | free-app-OK / public-OK | — | mp3quran read 54 | **PASS (structural).** 6,236/6,236; no ayah 0; 7 gaps of 20 ms; s94 and s111 end overshoot +341/+330 ms (clamp); s109 file is 320 kbps | **Gapless** + clamp |
| 10 | **Shuraim** | i.n. `ar.saoodshuraym` (Pkg); mp3quran r1 | free-app-OK / public-OK | — | mp3quran read 31 | **PARTIAL.** s9 off by one; 65:12 missing; s91 label shift; overshoots s12 +4,030, s40 +6,820, s80 +2,500, s83 +725 ms | Gapless with highlighting off in s12, s40, s65, s80, s83 and s91 (s9 relabel); or Pkg |
| 11 | **Saad Al-Ghamdi** | mp3quran r1 | public-OK (host) | (Tarteel GitHub only if QUL HQ is used) | mp3quran read 30 | **PASS.** 6,236/6,236; no ayah 0 (ayah 1 at 4.0–14.3 s, median 5.7 s); last end −939 to −6,431 ms; **96 kbps / 22.05 kHz** (not 128 as in 06); 1.07 GB / 24.8 h. Audio: 95–100 % of boundaries in pauses. **Same recording as QUL 13**; QUL starts are ~0.2–0.37 s later | **Gapless primary** (mp3quran). QUL 128 kbps HQ only after permission |
| 12 | **Maher Al-Muaiqly** | i.n. `ar.mahermuaiqly` (Pkg); Complex (producer grant); mp3quran r1 (no timing) | free-app-OK / public-OK (Complex) | Complex | QUL 65 (unknown); none on mp3quran for r1 | n/a | **Pkg gapped** in v1. Switch to the Complex per-ayah ZIP when it is obtained |
| 13 | **Abu Bakr Ash-Shatri** | i.n. `ar.shaatree` (Pkg); mp3quran r1 | free-app-OK / public-OK | — | mp3quran read 4 | **PASS (structural).** 6,236/6,236; 3 gaps of 20 ms; last end −240 to −18,223 ms; 128 kbps; 1.50 GB | **Gapless** |
| 14 | **Yasser Ad-Dosari** | everyayah 128 (Pkg, no i.n. per-ayah); mp3quran r1; i.n. `ar.yasseraldossari` (surah) | unknown (Pkg) / public-OK | QF | mp3quran read 92 | **PARTIAL.** s4 file replaced (24 kbps / 16 kHz VBR; timing ends 584 s before the file); overshoots ≤ +424 ms in 5 short surahs (clamp); mixed 44.1/48 kHz and 192 kbps files | Gapless with highlighting off in s4 |
| 15 | **Hani Ar-Rifai** | i.n. `ar.hanirifai` (Pkg); mp3quran r1 | free-app-OK / public-OK | — | mp3quran read 89 | **PASS (structural).** 6,236/6,236; 13 gaps of 20 ms; last end −1.2 to −13.2 s | **Gapless** |
| 16 | **Ahmed Al-Ajmy** | i.n. `ar.ahmedajamy` (Pkg); mp3quran r1 | free-app-OK / public-OK | — | mp3quran read 5 | **PARTIAL.** Mostly 160 kbps, but s4 is a 64 kbps replacement (−34.4 s) and s42 is not CBR in its first 64 KB. s70/s74 end 37 s before the file. Needs audio checks | Pkg gapped in v1; gapless after the audio check |
| 17 | **Qari Sadaqat Ali** (PK) | i.n. `ar.sadaqatali` (surah; tag qurancentral.com); quranicaudio `sadaqat_ali/` (personal); QUL 61 (no info) | free-app-OK (host) · living reciter, not asked | `qarisadaqat@hotmail.com` [unverified: search excerpt of his official Facebook page] | none anywhere | n/a | **Surah-level, no highlight**, `PENDING` (personal build now; public after his OK) |
| 18 | **Ali Al-Huthaify** | i.n. `ar.hudhaify` (Pkg); **Complex** (producer grant) | free-app-OK / public-OK (Complex) | Complex | mp3quran read 74 | **FAIL.** The last ayah is missing in 112 surahs (113 ayahs; timing stops at a median of 20.6 s before the end); al-Fatiha mislabelled; 64 kbps / 24 kHz | **Pkg gapped** in v1, then the Complex per-ayah ZIP. Never mp3quran-74 timing |
| U1 | **Urdu: Shamshad Ali Khan** | everyayah / i.n. `ur.khan` (host terms), but the content is ℗ 1998 Hi-Tech Music Ltd | **personal-only** | `info@hi-techmusic.com`, `h.azmat@hi-techmusic.com` | n/a (per-ayah) | n/a | **Personal build only.** Public after Hi-Tech's OK. Alternative: the Complex's Urdu audio, Juz 'Amma only |
| U2 | Urdu: Farhat Hashmi (v1.1) | everyayah only | unknown (Al-Huda International) | not researched | n/a | n/a | v1.1, after permission |
| C1 | **Muhammad Ayyub** (Complex reciter) | mp3quran r1; i.n. `ar.muhammadayyoub` (Pkg); Complex | public-OK | Complex (courtesy) | mp3quran read 109 | **PASS (structural).** 6,236/6,236; one 20 ms gap; last end −0.5 to −6.3 s; 96 kbps / 22.05 kHz; 1.40 GB | **Add to v1:** gapless + Pkg |
| C2 | **Khalid Al-Muhanna** (Complex reciter) | mp3quran r1; Complex | public-OK | Complex (courtesy) | mp3quran read 159 | **PASS (structural).** 6,236/6,236; ayah 0 in 112 surahs (none in 1 and 9, which is correct); last end = duration in every surah; 48 kbps / 22.05 kHz (0.64 GB) | Optional v1, low bitrate |
| C3 | Ibrahim Al-Akhdar (Complex reciter) | mp3quran r1; i.n. surah; Complex | public-OK | Complex | mp3quran read 1 | **PARTIAL.** Garbage last rows in s19 and s36 (end < start); a zero-length ayah 93:10; 48 kbps | v1.1 |

### 2.5 Timing validation detail

#### 2.5.1 Structural checks, all 114 surahs per read

The "last end − duration" column uses duration computed as (Content-Length − ID3v2 − ID3v1) × 8 / bitrate. CBR was confirmed from frame headers in the first 64 KB, and the tail tags from the last 512 B.

| read | rows | real ayahs present | missing | ayah 0 rows | non-positive | overlaps | non-monotonic | last end − duration, min/median/max (ms) | > +100 ms | kbps / kHz | total GB / h | label shifts (letter-count test) |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| **123** Alafasy | 6,236 | 6,236 | 0 | 0 | 0 | 0 | 0 | −4,632 / −2,062 / −513 | 0 | 128 / 44.1 | 1.72 / 29.8 | none |
| **30** Ghamdi | 6,236 | 6,236 | 0 | 0 | 0 | 0 | 0 | −6,431 / −2,469 / −939 | 0 | 96 / 22.05 | 1.07 / 24.8 | none |
| **51** ABM Mujawwad | 6,349 | 6,235 | 9:129 | 114 | 0 | 0 | 0 | −16,607 / −4,000 / −137 | 0 | 128 / 44.1 | 3.49 / 60.6 | **s9 (+1)** |
| 54 Sudais | 6,236 | 6,236 | 0 | 0 | 0 | 0 | 0 | −10,986 / −951 / +341 | 2 | 128 (s109: 320) | 1.22 / 21.2 | none |
| 31 Shuraim | 6,354 | 6,234 | 9:129, 65:12 | 114 | 0 | 0 | 0 | −9,595 / −431 / +6,820 | 4 | 128 | 1.05 / 18.3 | s9, s91 |
| 118 Husary | 6,349 | 6,235 | 1:7 | 114 | 0 | 0 | 0 | −20,254 / −2,933 / +23,751 | 1 | 128 (s21: 48 kHz) | 2.47 / 42.8 | none |
| 112 Minshawi | 6,349 | 6,235 | 9:129 | 114 | 0 | 0 | 0 | −9,413 / −532 / +8,155 | 3 | 128 (s53: 192) | 1.67 / 28.9 | s9 |
| 53 AB Murattal | 6,350 | 6,236 | 0 | 114 | 0 | 0 | 0 | −1,464 / −775 / −234 | 0 | **32 / 11.025** | 0.45 / 31.2 | none |
| 4 Shatri | 6,236 | 6,236 | 0 | 0 | 0 | 0 | 0 | −18,223 / −2,829 / −240 | 0 | 128 | 1.50 / 26.1 | none |
| 92 Yasser | 6,236 | 6,236 | 0 | 0 | 0 | 0 | 0 | −584,260 / −1,442 / +424 | 5 | 128 (s4: 24 / 16 VBR; others 48 kHz / 192) | 1.44 / 25.9 | none |
| 89 Hani Rifai | 6,236 | 6,236 | 0 | 0 | 0 | 0 | 0 | −13,198 / −8,414 / −1,215 | 0 | 128 | 1.48 / 25.7 | none |
| 5 Ajmy | 6,350 | 6,236 | 0 | 114 | 0 | 0 | 0 | −37,668 / −434 / +6 | 0 | 160 (s4: 64 / 22.05) | 1.85 / 26.4 | none |
| 74 Huthaify | 6,237 | 6,123 | **113 ayahs in 112 surahs (the last ayah)** | 114 | 0 | 0 | 0 | −106,266 / −20,628 / −1,568 | 0 | 64 / 24 | 0.86 / 30.0 | none |
| 109 Ayyub | 6,236 | 6,236 | 0 | 0 | 0 | 0 | 0 | −6,274 / −1,927 / −527 | 0 | 96 / 22.05 | 1.40 / 32.5 | none |
| 1 Akhdar | 6,348 | 6,236 | 0 | 112 | 3 | 0 | s93 | −15,024 / 0 / 0 | 0 | 48 / 22.05 | 0.73 / 33.5 | none |
| 159 Muhanna | 6,348 | 6,236 | 0 | 112 | 0 | 0 | 0 | 0 / 0 / 0 | 0 | 48 / 22.05 | 0.64 / 29.8 | none |

**Surah 9 across all 115 timed reads.** 4 reads (31, 51, 74, 112) have an `ayah 0` row and lack 9:129. 10 reads have `ayah 0` plus all 129 ayahs (130 rows). The other 101 have no `ayah 0` and all 129. The letter-count test shows that 51, 31 and 112 are shifted by +1, but 74 is not: in 74 the last ayah is simply dropped. **The repair must therefore be chosen from evidence, not from the pattern alone** [verified].

**Intro and basmala conventions.** These differ per read, so the pipeline must normalise them.

| Convention | Reads | Fatiha handling (1:1 = basmala) |
|---|---|---|
| Implicit gap (no ayah 0; ayah 1 starts after the isti'adha/basmala) | 123, 30, 54, 4, 92, 89, 109 | Correct. 1:1 starts after an isti'adha-only intro: Ghamdi intro is one speech burst at 0.2–3.06 s, then 1:1 = 3.90–6.56 s; Alafasy 1:1 = 6.42–11.12 s |
| `ayah 0` in every surah including 1 and 9 | 51, 31, 118, 112, 53, 5, 74 | 51: ayah 0 = 0–6.06 s isti'adha, then 1:1 = 6.06–14.82 s, **correct**. 118 and 74: ayah 0 swallows the basmala, **wrong** |
| `ayah 0` except in surahs 1 and 9 | 1, 159 | Correct |

#### 2.5.2 Decoded-audio checks (3 full surahs per read)

Columns:
- *frames*: a full frame walk; 0 resyncs in every file.
- *dur*: decoded / frame-count / Content-Length estimate.
- *in pause*: share of ayah starts that fall inside a run of ≥ 120 ms below a noise-relative threshold.
- *control*: the same test at ayah midpoints.
- *onset*: median time from the marked start to the next speech onset.

| file | kbps / kHz, header | frames | dur (ms) dec / frames / CL | last end (margin) | in pause | ≤ 150 ms | control (midpoints) | onset |
|---|---|---|---|---|---|---|---|---|
| mp3quran 123 / 001 | 128 / 44.1, no Xing | 1,988 | 51,931 / 51,931 / 51,931 | 51,200 (−731) | 0.50 | 0.67 | 0.33 | +405 |
| mp3quran 123 / 036 | ″ | 40,571 | 1,059,814 ×3 | 1,057,580 (−2,234) | **0.99** | 0.99 | 0.11 | +370 |
| mp3quran 123 / 067 | ″ | 17,435 | 455,445 ×3 | 451,260 (−4,185) | **0.97** | 0.97 | 0.17 | +420 |
| mp3quran 30 / 001 | 96 / 22.05, no Xing | 1,841 | 48,091 ×3 | 41,660 (−6,431) | 0.83 | **1.00** | 0.00 | +295 |
| mp3quran 30 / 036 | ″ | 30,284 | 791,092 ×3 | 788,060 (−3,032) | **0.95** | 0.95 | 0.09 | +285 |
| mp3quran 30 / 067 | ″ | 14,780 | 386,090 ×3 | 383,740 (−2,350) | **1.00** | 1.00 | 0.14 | +460 |
| mp3quran 51 / 001 | 128 / 44.1, ID3v1 | 3,808 | 99,474 / 99,474 / 99,482* | 97,752 (−1,722) | **1.00** | 1.00 | 0.00 | +2,731 |
| mp3quran 51 / 036 | ″ | 80,746 | 2,109,283 / ″ / 2,109,291* | 2,107,163 (−2,120) | **0.99** | 1.00 | 0.06 | +3,383 |
| mp3quran 51 / 067 | ″ | 32,667 | 853,342 / ″ / 853,350* | 850,724 (−2,618) | **1.00** | 1.00 | 0.13 | +2,132 |
| QUL 13 / 001 (Ghamdi) | 128 / 44.1, LAME `Info` | 1,470 | 38,323 / 38,400 / 38,400 | 38,374 (−26 vs frames) | 1.00 | 1.00 | 0.00 | +60 |
| QUL 13 / 036 | ″ | 30,004 | 783,708 / 783,778 | 783,752 (−26) | 0.95 | 0.95 | 0.10 | +100 |
| QUL 13 / 067 | ″ | 14,481 | 378,224 / 378,279 | 378,253 (−26) | 1.00 | 1.00 | 0.03 | +110 |

\* The 8 ms difference is the 128-byte ID3v1 tag, which `validate.py` subtracts.

- **CBR duration from Content-Length is exact** for these files, so `HEAD` alone gives `duration_ms` for the guard table [verified].
- **Weak spots:**
  - Alafasy al-Fatiha: boundaries 1:2 (11,120 ms) and 1:4 (21,280 ms) sit 870 and 750 ms before the real pauses (11,990–12,420 and 22,030–22,500 ms).
  - Ghamdi 36:44: 4.4 s; it is present identically in QUL, so it is probably a mid-ayah breath.
  - Ghamdi 36:1/36:2: 1.6–1.8 s; "Yā-Sīn" is a two-letter ayah.
- **The onset offsets are a convention, not drift.** For Mujawwad, highlighting moves to the next ayah about 2–3 s before the qari starts it. Our pipeline should snap each start to `onset − 150 ms` (§2.6).

#### 2.5.3 Off-by-one detector (no audio needed)

On a 6,236-ayah scale, an ayah's duration correlates strongly with its letter count. The Pearson r of duration vs simple-clean letters, per read:

| Read | Median r | 10th percentile |
|---|---|---|
| 123 | 0.93 | 0.75 |
| 30 | 0.94 | 0.76 |
| 51 | 0.95 | 0.84 |

A correct label shift k maximises r.

| Case | k = 0 | k = +1 | Other evidence |
|---|---|---|---|
| ABM 51, surah 9 | r = 0.08 | r = 0.96 | Against QUL-1 durations: r = −0.26 to 0.35 at k = 0 vs 1.00 at k = +1 in every window of 11–30 ayahs. Against QUL word counts: 0.06 vs 0.95 |
| Alafasy 123 and Ghamdi 30, surah 9 | 0.92 / 0.90 | 0.02 / 0.01 | (control: correct labels) |

After the relabel, no ayah in reads 123, 30 or 51 is more than 3× slower or faster per letter than its read's median [verified].

#### 2.5.4 Ghamdi: mp3quran (read 30) vs QUL (recitation 13)

"Timing offset" is mp3quran start minus QUL start.

| Surah | Ayahs | mp3quran bytes | QUL bytes | **Audio lag** (envelope xcorr) | Timing offset, median (min–max) | Residual after median, p50 / p95 / max (ayah) | Drift (ms per s) |
|---|---|---|---|---|---|---|---|
| 1 | 7 | 577,295 | 614,591 | **3,830 ms, r = 0.996** | 3,595 (3,440–3,900) | 40 / 155 / 305 (1:1) | — (too short) |
| 9 | 129 | 35,221,304 | 46,871,386 | n/a | 3,680 (3,500–4,040) | 45 / 160 / 360 (9:1) | 0.02 |
| 18 | 110 | 21,097,312 | 27,950,373 | n/a | 9,347 (8,820–9,960) | 67 / 267 / 612 (18:1) | 0.00 |
| 36 | 83 | 9,493,301 | 12,540,635 | **5,450 ms, r = 0.993, constant across 13 one-minute windows** | 5,260 (3,649–5,560) | 45 / 280 / 1,611 (36:2) | 0.21 |
| 67 | 30 | 4,633,273 | 6,052,656 | **6,010 ms, r = 0.994, constant across 6 windows** | 5,645 (5,180–6,000) | 60 / 245 / 465 (67:27) | −0.26 |
| 112 | 4 | 217,431 | 166,957 | n/a | 6,105 (6,068–6,371) | 37 / 37 / 266 | — |

- **Same recording:** yes. The lag is constant and there is no tempo drift.
- **After aligning by the true audio lag,** mp3quran starts are earlier than QUL's by a median of 240 ms (s1), 192 ms (s36) and 365 ms (s67).
  - mp3quran marks the start of the pause; QUL marks 60–110 ms before the onset.
  - 83 % / 87 % / 24 % of starts agree within ±300 ms.
- **QUL files are trimmed to start at ayah 1.** Its 67:1 begins at audio t = 0, which equals mp3quran t = 6.01 s, after the basmala. So QUL Ghamdi needs `BasmalaStrategy.USE_FATIHA_1_1`, while mp3quran Ghamdi needs `EMBEDDED`.
- **Controls:** Ghamdi vs Alafasy (different reciters) gives r = 0.10. QUL vs mp3quran for Alafasy s67 gives windowed r = 0.60–0.84 with the lag drifting 13.05 → 11.95 s, so it is a different edit. Abdul Basit Mujawwad s1 gives r = 0.46 with durations of 99.5 vs 78.3 s, so it is a different edit. **Never pair timings across hosts.**

### 2.6 Pipeline rules derived from the defects (offline Python, run by us)

```python
def normalise(read, surah, rows, file):            # rows from mp3quran ayat_timing
    rows = sorted(rows, key=lambda r: r.ayah)
    k = best_label_shift(rows, letters[surah])      # argmax_k corr(duration, letters[a+k]), k ∈ {-1,0,+1}
    if k != 0 and gain >= 0.25 and r_k >= 0.6:      # read 51/31/112 s9, 31 s91
        rows = [r._replace(ayah=r.ayah + k) for r in rows]
    if surah == 1 and has_row(rows, 0) and not has_row(rows, 7):
        return DISABLE                              # 118, 74: basmala swallowed by the intro
    intro = pop_row(rows, 0)                        # store as ayah 0 (play-from-1 starts at 0 ms)
    expect = set(range(1, AYAH_COUNT[surah] + 1))
    if {r.ayah for r in rows} != expect: return DISABLE     # 74 (last ayah dropped), 31 s65
    if any(r.end <= r.start for r in rows) or not strictly_increasing(rows): return DISABLE  # Akhdar s19/36/93
    over = rows[-1].end - file.duration_ms          # duration = (CL - id3v2 - id3v1) * 8 / kbps
    if over > 1000: return DISABLE                  # swapped files: 118 s21, 112 s9/52/53, 31 s12/40/80, 92 s4
    rows[-1] = rows[-1]._replace(end=min(rows[-1].end, file.duration_ms))   # clamp ≤1 s (54, 92)
    if not all_frames_same_bitrate(file.first64k): return DISABLE           # 92 s4, 5 s42
    rows = snap_starts_to_onset(rows, envelope(file), max_shift_ms=1500)    # optional; needs download
    return Pack(rows, guard=(file.content_length, file.etag, file.duration_ms))
```

- A `DISABLE` disables highlighting for that surah only. Playback still works, with a surah-level seek bar.
- The app checks `guard` against `HEAD` or the downloaded file. A changed `ETag` or length disables highlighting until the pack is rebuilt. This matters because every mp3quran file of reads 123, 30 and 51 was re-uploaded on 2026-09-27 [verified].

### 2.7 Permission emails: minimum set and real addresses

| # | To | Address (verification) | Covers | Needed for public v1? |
|---|---|---|---|---|
| 1 | **Alafasy Berhad** (official Alafasy company) | **ai@alafasy.co**, +603 8682 3105 [verified: www.alafasy.co/contact-us, /terms-of-use] | Default reciter: gapless (mp3quran) and per-ayah (everyayah/i.n.); permission to cache offline | **Yes** (highest exposure) |
| 2 | **Quran Foundation** (runs everyayah's support desk, quranicaudio and QDC) | **developers@quran.com** [verified: QF Developer Terms §12]; info@quran.foundation [verified: quran.foundation]; desk https://quran.zendesk.com/hc/en-us [verified: everyayah "Contact Us"] | everyayah per-ayah sets (Husary Muallim, Yasser 128 and all Pkg rows), quranicaudio-only sets, QDC timings offline | **Yes** (for the verse-by-verse layer) |
| 3 | **King Fahd Complex** | **info@qurancomplex.gov.sa** [unverified: search summary of qurancomplex.gov.sa/contact-us/; site unreachable] | Confirm the free-use statement in writing; get the per-ayah/surah ZIP links for Huthaify, Maher, Ayyub, Muhanna, Akhdar; permission to mirror on our CDN | Recommended (cheap, strengthens 5 reciters) |
| 4 | **Hi-Tech Music Ltd** (℗1998 Urdu album) | **info@hi-techmusic.com** (UK), **h.azmat@hi-techmusic.com** (Pakistan) [verified: label's Dailymotion profile via api.dailymotion.com; self-reported, undated] | Shamshad Ali Khan Urdu voice (U1), interleaved per ayah | Only if U1 ships publicly |
| 5 | **Qari Syed Sadaqat Ali** | qarisadaqat@hotmail.com [unverified: search excerpt of facebook.com/QariSyedSadaqatAli] | His full recitation; permission to make ayah timings | Only if row 17 ships publicly |
| — | mp3quran.net | No email. Form at https://www.mp3quran.net/eng/contact-us (POSTs to `/contact`) [verified] | Already granted in writing. Send a courtesy notice, and **archive /eng/privacy and /eng/contact-us** (PDF + SHA-256 + date) for Play review | No |
| — | Islamic Network | Forum only: https://community.islamic.network [verified] | Declines individual confirmations (d/291, d/307) | No |
| — | Tarteel / QUL | https://github.com/TarteelAI/quranic-universal-library/issues [verified: QUL FAQ] | Only if we use QUL audio (Ghamdi HQ) or word segments | v1.1 |

**Template** (send as plain text, in English, and in Arabic for the Complex):

> Subject: Permission request: [Reciter] recitation in a free, ad-free Quran app (Android)
> Assalamu alaikum. We are building **[App]**, a free Quran app for Android with no ads, no in-app purchases and no accounts, aimed mainly at Pakistan. We would like to let users **stream and save for offline listening** the recitation of **[Reciter]** (source: [host + URL pattern]), with ayah highlighting. The audio files will not be altered, sold or offered as a separate download. The reciter and source are credited on every playback screen and in an Audio Credits page, and we will remove the recitation within 48 hours on request. Could you confirm in writing that this use is permitted, and tell us the attribution wording you prefer? If you can, please also confirm whether we may mirror the files on our own CDN to reduce load on yours. JazakAllahu khairan. [Name, email, Play developer name, website]

---

## 3. Recommendations

1. **Switch the public v1 default layout to gapless mp3quran for every reciter that passes the pipeline.** Keep everyayah/i.n. gapped as an optional "verse-by-verse" layer (hifz, Urdu interleave), marked `licenseStatus = PENDING`.
   - *Why:* mp3quran is the only host with a written grant, and it is explicitly meant as app-store evidence. The timings validate on 342 of 342 files for the three priority reads. Gapless plus `ClippingConfiguration` already covers ayah, range, Para and repeat (report 06 §2.8.3).
   - *Trade-off:* downloads are per surah instead of per ayah. Per-ayah files stay available for hifz.
   - **Confidence: Medium-High.** The host grant does not bind reciters.
2. **Ship gapless highlighting in v1 only through the offline timing pipeline (§2.6), with a per-surah `DISABLE` switch and an ETag/length guard.**
   - Ship with highlighting: 123, 30, 51 (s9 relabel), 54, 4, 89, 109, 159.
   - Ship with some surahs disabled: 118 (s1, s21), 112 (s52, s53, and s9 relabel), 31 (6 surahs), 92 (s4).
   - Do not use 74 for timing. Do not use 53 (32 kbps).
   - Reads 54, 4, 89, 109 and 159 were validated structurally only. Before release, run the §2.5.2 decode check on three surahs for each, plus one listening pass.
   - **Confidence: High** for 123, 30 and 51 (audio-validated). **Medium-High** for the structural-only reads.
3. **Add speech-onset snapping and a clamp to the pipeline.** Snap only for downloaded or pipeline-side files, within ±1.5 s, because mp3quran marks pause starts: Mujawwad highlighting otherwise leads by 2–3 s and al-Fatiha boundaries can be 0.8 s early. Clamp last-ayah overshoots of 1 s or less. **Confidence: Medium.** I validated the energy method here on 12 files; it needs one listening pass per reciter.
4. **Correct the catalogue data from report 06.**
   - Ghamdi via mp3quran is **96 kbps / 22.05 kHz, 1.07 GB**, not 128 kbps.
   - Use QUL 13 (same recording, 128 kbps / 44.1 kHz) only after Tarteel answers. If it is used, apply `BasmalaStrategy.USE_FATIHA_1_1`, because QUL's files have no basmala.
   - Never mix timings across hosts: Alafasy and Abdul Basit Mujawwad are different edits on QUL.

   **Confidence: High.**
5. **Treat islamic.network as "free-app-OK, no rights evidence", not as licensed.** Keep using it as the per-ayah CDN, since full per-user reciter downloads are explicitly OK. Do not cite it to Play or Apple as authorization; the maintainer says its terms are "not evidence of any explicit authorization". **Confidence: High.**
6. **Have the owner download the King Fahd Complex per-ayah and per-surah ZIPs from Pakistan** (they are geo-blocked from our egress).
   - Reciters: Huthaify, Maher, Ayyub, Muhanna, Akhdar.
   - Save the licence page as PDF, email info@qurancomplex.gov.sa, and self-host on R2 with credit "Recording: King Fahd Glorious Qur'an Printing Complex".
   - This gives producer-licensed **gapped** audio (no timing needed) for 5 reciters. It is the cleanest verse-by-verse layer we can get.

   **Confidence: Medium.** The statement is search-verified only.
7. **Send emails 1–3 (§2.7) before the Play release, and 4–5 only if U1 or row 17 is to go public.** Do not email Islamic Network. Archive mp3quran's statement. Record each reply in the catalogue's `attribution` and `licenseStatus` fields. **Confidence: High.**
8. **Keep Shamshad Ali Khan Urdu, Husary Muallim and Qari Sadaqat Ali in the personal or beta build until their owners reply.** For a public Urdu option before then, offer the Complex's Urdu meanings audio for al-Fatiha and Juz 'Amma, and show a text translation for the rest. The Punjab Act requires a translation to be shown with its Arabic text (report 09 §2.4). **Confidence: Medium-High.**
9. **Add Muhammad Ayyub (109) to the v1 shortlist.** He is a Complex reciter, his timings pass structurally, he is at 96 kbps, and he is a classic voice in South Asia. Treat Khalid Al-Muhanna (159, 48 kbps) as optional. **Confidence: Medium.**
10. **Re-run the whole validation in CI weekly.** It costs about 1,400 requests per read and is cheap. Diff ETags, and rebuild any pack whose guard changed. mp3quran re-uploaded every file of three reads on 2026-09-27. **Confidence: High.**

---

## 4. Risks & open questions

**Risks**
- **A host grant is not a reciter licence.** mp3quran and islamic.network re-host CD rips and other sites' files. A reciter or label (e.g. Alafasy Berhad) can still demand removal. *Mitigation:* free and ad-free, credits, a 48-hour removal promise, and the remote kill-switch. Email 1 first.
- **The Complex statement is unverified at source** (503 or reset; archives blocked). Its wording might add conditions such as "no modification" or "attribution", or mark an asterisk we could not see.
- **Silent CDN changes.** The 27 Sep 2026 re-upload, the swapped files (118 s21 at 48 kHz; 92 s4 at 24 kbps; 5 s4 at 64 kbps) and the timing overshoots show that file and timing can drift apart. The ETag guard is mandatory.
- **The energy-based pause test is a proxy.** It cannot catch a boundary placed in the wrong pause, e.g. a waqf inside an ayah. That needs listening or ASR. The letter-count test bounds this at the label level only.
- **Indo-Pak display numbering vs Kufan keys.** Report 10-gap-01 found that al-Fatiha's displayed numbers differ (the basmala carries ۝). Highlighting must map through the Kufan keys.
- **Hi-Tech Music contact details are self-reported** on a 2014-era profile and may be stale. The Sadaqat Ali email is from a search excerpt.

**Open questions**
1. Does the Complex site load from Pakistan? Its ZIP links and exact licence text are the key missing evidence.
2. Are the everyayah/i.n. `Hudhaify_*`, `Muhammad_Ayyoub_*` and `MaherAlMuaiqly*` files the Complex's recordings? This needs an audio fingerprint against the Complex ZIPs. If they are, the producer grant also covers the existing Pkg rows.
3. Will Alafasy Berhad permit third-party apps at all? Its ToS suggests it treats audio as company content.
4. Should the 6 PARTIAL reads ship with per-surah highlight gaps, or wait for repaired timings? We could generate our own timings with forced alignment (report 06 rec. 12).
5. Who owns the rights to Abdul Basit, Minshawi and Husary (all deceased, mostly Egyptian radio or label recordings)? No host names a rights-holder, and none was found.
6. Is a TTS Urdu voice over a public-domain translation (e.g. Jalandhari) an acceptable licence-clean fallback for the Arabic+Urdu feature? The question is religious acceptability, not licensing.

---

## 5. Sources

| URL | What I verified there |
|---|---|
| https://www.mp3quran.net/eng/privacy | "Copyrights: All rights are available to everyone…" (EN + AR); applies to mp3quran.org and qurango.net |
| https://www.mp3quran.net/eng/contact-us | "share the following URL for Apple, google… https://mp3quran.net/eng/privacy"; contact form only, no email |
| https://www.mp3quran.net/eng/about , /eng/api , /eng/terms (404) | No other terms |
| https://www.mp3quran.net/api/v3/ayat_timing/reads | 115 timed reads with `folder_url` (123, 30, 51 and the 13 others) |
| https://www.mp3quran.net/api/v3/ayat_timing?surah=N&read=R | 16 reads × 114 surahs, plus surah 9 for all 115 reads |
| https://www.mp3quran.net/api/v3/reciters?language=eng | 243 reciters / 289 moshafs; moshafs for the catalogue reciters (no Sadaqat Ali, no Husary Muallim, no Urdu) |
| https://cdn.mp3quran.net/audio/{mishary-alafasy,saad-ghamdi,abdulbasit-abdulsamad,…}/rN/NNN.mp3 | HEAD (Content-Length, ETag, Last-Modified 2026-09-27), ranged first 64 KB / last 512 B, 9 full downloads, ID3 tags (Lavf59.27.100) |
| https://alquran.cloud/terms-and-conditions | Section IV recitation terms; "originally retrieved from GlobalQuran.com… retain their respective copyrights"; last updated 14 June 2026 |
| https://alquran.cloud/contributors | "Quran Central for the surah audio files on the CDN · Every Ayah for the ayah audio files on the CDN" |
| https://community.islamic.network/ and /api/discussions/{291,296,303,304,307,310,316,318} | Maintainer replies quoted in §2.1 |
| https://api.alquran.cloud/v1/edition?format=audio | 190 audio editions; identifiers incl. `ar.muhammadayyoub`, `ar.ibrahimalakhdar`, `ar.yasseraldossari`, `ur.khan`, `ar.sadaqatali` |
| https://api.alquran.cloud/v1/quran/quran-simple-clean | Ayah letter counts for the label-shift test |
| https://cdn.islamic.network/quran/audio-surah/128/ar.sadaqatali/1.mp3 | ID3 COMM `https://www.qurancentral.com`; Last-Modified 2025-03-17 |
| https://everyayah.com/ , /data/timings_files/000_disclaimer.txt , /data/translations/urdu_shamshad_ali_khan_46kbps/{001001,002255}.mp3 | Contact → quran.zendesk.com; timing link-back term; Urdu ID3 `www.AllahsQuran.com` |
| https://quranicaudio.com/about | Personal-use terms; "hand ripped from cds… downloaded from… mp3quran, tvquran" |
| https://download.quranicaudio.com/quran/sadaqat_ali/001.mp3 | ID3 `COMM quranicaudio.com`; size 979,072 B |
| https://api-docs.quran.foundation/legal/developer-terms/ | 1-week cache limit; QF Content includes audio; contact developers@quran.com (decoded); last updated 2026-10-11 |
| https://quran.foundation/ | info@quran.foundation |
| https://feedback.quran.com/translation-and-reciter-requests/p/request-for-permission-to-use-quran-recitation-audio-1 | Example audio-permission request, unanswered since 2025-01-20 |
| https://qul.tarteel.ai/resources/recitation and /resources/{id}/copyright for all 130 ids | "We don't have copyright information for this resource." ×130 |
| https://qul.tarteel.ai/faq | Commercial use subject to per-resource terms; GitHub issues as contact |
| https://www.tarteel.ai/terms | "Service Content… audio… No Service Content may be… copied, distributed… downloaded"; last updated April 2, 2021 |
| https://qul.tarteel.ai/api/v1/audio/surah_recitations ; /surah_segments/{13,1,7}?surah=N&page=P | Ghamdi = id 13 (`quran/surah/ghamadi/murattal/mp3`); segments for s1, 9, 18, 36, 67, 112; ABM id 1 s1, 9, 112; Alafasy id 7 s1, 67, 112 |
| https://audio-cdn.tarteel.ai/quran/surah/{ghamadi,abdulBasit/mujawwad,alafasy}/…/NNN.mp3 | Downloads for xcorr (Ghamdi 1, 36, 67; ABM 1; Alafasy 1, 67); LAME Info 128 kbps / 44.1 kHz; Last-Modified 2026-03-29 |
| https://www.alafasy.co/ , /terms-of-use , /contact-us , /about-us | Alafasy Berhad identity, IP clause §6, ai@alafasy.co (decoded), phone |
| https://www.qobuz.com/us-en/album/the-holy-quran-complete-with-urdu-translation-qari-waheed-zafar-qasmi-maulana-fateh-mohd-jalandri-shamshad-ali-khan/iivc35k5nlpza | (C)/(P) 1998 Hi-Tech Music Ltd; credits incl. Shamshad Ali Khan; 115 tracks; 45:25:55 |
| https://music.apple.com/us/album/the-holy-quran-complete-with-urdu-translation/830178975 | ℗ 1998 Hi-Tech Music Ltd; 115 songs, 45 h 26 min |
| https://api.dailymotion.com/user/hitechmusicltd?fields=description | "U.K. Office: info@hi-techmusic.com · Pakistan Office: h.azmat@hi-techmusic.com" |
| https://islamhouse.com/ar/author/8722/audios/showall/1/ , https://islamhouse.com/ar/audios/385256/ | Complex audio publications; Urdu meanings audio for al-Fatiha + Juz 'Amma only (per-surah MP3s on d1.islamhouse.com) |
| https://tanzil.net/res/text/metadata/quran-data.js | Kufan ayah counts (114 surahs, 6,236 ayahs) |
| qurancomplex.gov.sa/en/?p=3979, /books-audios/, /privacypolicy/, /quran-audios/, /sounds/, /quran-audio-hafs-huthify/, /sounds-hafs-muaiqly/, /quran-audio-hafs-ayyoub/, /sounds-hafs-muhanna/, /en/sounds-hafs-akhdar/, /en/sounds-qaloon-huthify/, /contact-us/ | **Search-excerpt only.** Free-use wording, reciter list, download sizes, info@ address. Direct fetch: 503 or connection reset; web.archive.org reset; archive.ph 429; r.jina.ai timeout |
| facebook.com/QariSyedSadaqatAli (via search) | **Search-excerpt only.** "OFFICIAL Page", qarisadaqat@hotmail.com, (571) 242-2786 |
