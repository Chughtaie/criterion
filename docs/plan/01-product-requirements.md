# 01 — Product Requirements (PRD)

This document defines **what** the Quran app (codename *Criterion*, package `com.chughtai.criterion`) must do and **how well**: the vision, the problem, who it serves, the principles that settle trade-offs, the full feature catalogue with phases and priorities, testable acceptance criteria for every P1 (personal MVP) and P2 (v1.0 public "Ramadan launch", v1.0-stretch and v1.1) feature, the non-functional and privacy requirements, success metrics that need no tracking SDK, and what is explicitly out of scope. The *how* lives in the sibling docs: [UX/UI](02-ux-ui-spec.md), [Content & licensing](03-content-data-licensing.md), [Architecture](04-architecture.md), [Mushaf renderer](05-mushaf-rendering-engine.md), [Audio engine](06-audio-engine.md), [Companion features](07-companion-engagement-features.md), [Data pipelines](08-data-pipelines.md), [Roadmap](09-roadmap-backlog.md), [Quality & CI](10-quality-testing-ci.md), [Release & compliance](11-release-compliance-business.md), [Decisions & risks](12-decisions-risks.md), [AI build playbook](13-ai-assisted-build-playbook.md).

**Status:** Draft v2 — 2026-10-19 (cross-doc review applied) · **Owner:** product (the app owner) · **Readers:** owner, implementing engineer / AI agent, reviewers.

**Key decisions in this doc**
- Product = an **ad-free, tracking-free, offline-first, typeset, ayah-aware 16-line Indo-Pak (Taj) mushaf + favourite-Qari player**, with Para/Surah/Manzil/Ruku indexes and Read + Play on every row (D-04, D-05, D-07, D-13, D-17, D-20).
- Native Android, Kotlin + Compose, minSdk 24 (D-01, D-02); English + Urdu UI from the first build (D-21).
- Page labels shown everywhere are the **printed labels 2–549** (= page_index + 1) (D-04).
- Para audio, indexing and progress use **our Indo-Pak Para table**, never Madani juz (D-07).
- Bundled translations: Urdu Junagarhi (+ Ahsan-ul-Bayan footnotes) and English Saheeh International; no word-by-word and no translation-only mode in v1 (D-09).
- Default audio = gapless per-surah files + validated timing packs; verse-by-verse (gapped) audio (`PENDING`) is limited to personal and closed-test builds, and some reciters (`PERSONAL_ONLY`) to the personal build, until permissions arrive (D-12, D-29, gate G2).
- Companion features are P2 work (D-25), but the plan of record splits them: **v1.0** (Ramadan 1448 launch) = Quran core + compliance floor + prayer times with Sehri/Iftar, Hijri + PK feed, Ramadan core and the khatam planner; tasbeeh, tafsir sheet, Para-quarter list and the Manzil-dua playlist are **v1.0-stretch**; adhan, Qibla, azkar, widgets, reminders, share-image and translation packs are **v1.1** (2027-03-22) unless pulled in ([Roadmap](09-roadmap-backlog.md) §5). The §6 "Release" column is authoritative.
- Free forever, no ads ever, nothing Quranic behind a paywall; Data safety "No data collected" (D-17, D-20).
- Public release is gated by G1–G6; a typeset page that is wrong is worse than no page (D-22).

---

## 0. Conventions

| Term | Meaning |
|---|---|
| **Phase** | P0 Foundations (2026-10-20 → 11-02) · **P1** Personal MVP (11-03 → 12-21) · **P2** v1.0 public Ramadan launch (12-22 → ~2027-02-01) · P3 v1.x (Feb → Jun 2027) · P4 v2 (H2 2027) (D-25). |
| **Release** | The release a feature is planned to ship in, per the plan of record in [Roadmap](09-roadmap-backlog.md) §5: **v1.0** = Ramadan launch (staged rollout from 2027-02-01; never-cut floor + Tier A); **v1.0-stretch** = pre-cut ladder rungs L1–L4, restored only if capacity allows; **v1.1** = 2027-03-22, cut from v1.0 by default but may be pulled in under doc 09 §5.2 (merged into the closed-test build by 2027-01-18); **v1.x** = later P3 releases (v1.2–v1.5); **v2** = P4. Phase records where the feature was specified (D-25); Release governs scheduling. |
| **Priority (MoSCoW)** | Relative to the feature's **Release**. **Must** = that release is blocked without it. **Should** = expected; cut only via the scope-cut ladder ([Roadmap](09-roadmap-backlog.md)). **Could** = first to cut. **Won't** = not in this product line. |
| **MH-n** | The owner's four must-haves (§1.4). |
| **Content tier** | Every build has `BuildConfig.CONTENT_TIER` ∈ {PERSONAL, BETA, PUBLIC} plus `MONETIZED` (D-29; wiring in [Architecture](04-architecture.md)). Every content item has a `licenceStatus` ∈ {PERMITTED, HOST_GRANT, PENDING, PERSONAL_ONLY, REVOKED} with a free-text `basis`. |
| **Personal build** (PERSONAL) | Owner's own devices (sideloaded `fossPersonalRelease`). May carry anything except REVOKED: `PENDING` items (gapped per-ayah audio), `PERSONAL_ONLY` items (Qari Sadaqat Ali, Shamshad Ali Khan Urdu voice, Husary Muallim) and the QUL layout before G1. |
| **Closed-test build** (BETA) | Public-tier build on the Play closed-testing track. Closed testers are third parties, so mushaf layout data must be **G1-cleared or clean-room**; recitations may be PERMITTED, HOST_GRANT or PENDING (never PERSONAL_ONLY); a "pre-release — proofreading in progress" notice shows until G5 (D-29). |
| **Public build** (PUBLIC) | Production. Only PERMITTED / HOST_GRANT content, G1 + G5 evidence for the shipped layout (D-10, D-17, D-29); CI enforces this ([Content](03-content-data-licensing.md), [Quality](10-quality-testing-ci.md)). In ACs below, "public build" covers BETA and PUBLIC alike, except that a BETA build may also list PENDING recitations. |
| **page_index / page_label** | page_index 1–548 = QUL index (storage); page_label = page_index + 1 (2–549), the printed number, the **only** number shown in UI (D-04). |
| **Verse key** | Kufan `surah:ayah` (6,236 ayahs; 1:1 = basmala) (D-06). |
| **AC** | Acceptance criterion in Given / When / Then form. An AC that names a number (page, ayah, count) is a test oracle for [Quality & CI](10-quality-testing-ci.md). |

New ID groups introduced by this doc (see §16): **MH-1..4** (owner must-haves), **PER-0..4** (personas), **PP-1..8** (principles), **NFR-01..18**, **PRV-01..14**, **SM-01..16**, **OQ-01..14**, plus feature IDs **F-PL-10** (gentle rating prompt), **F-PL-11** (signed remote notices) and **F-PL-12** (launcher shortcuts; designed in [UX/UI](02-ux-ui-spec.md)).

---

## 1. Vision and positioning

### 1.1 Vision
*"Read like your printed Quran. Listen to your Qari. Nothing else in the way."* A calm, dignified daily companion for the South-Asian Muslim: the exact 16-line page they grew up reading, every ayah tappable, their favourite Qari reciting any Surah, Para, Manzil, ruku or verse range with the ayah lit on the page, Urdu and English meaning one tap away, and the daily-routine essentials (prayer times, adhan, Ramadan) that make it the one app they open after Fajr. No ads, no tracking, no paywall between a believer and the Quran.

### 1.2 Positioning statement
> **For** Pakistani and South-Asian Muslims (at home and in the diaspora) who read from the 16-line Indo-Pak mushaf and listen to recitation every day, **<Brand>** (working name "Hamdam", gate G3) **is** an ad-free Quran app **that** shows the printed Taj 16-line page — typeset, crisp and ayah-aware — and plays your favourite Qari for any Surah, Para, Manzil or verse range with the ayah highlighted on the page. **Unlike** "Al Quran – القرآن الكريم" and other popular Pakistani Quran apps, it has **no ads, no gated reciters, no tracking**, real Para/Manzil/Ruku indexes, Urdu + English translation, and works fully offline.

Store title pattern: "<Brand>: Quran 16 Line & Audio" + bilingual القرآن الكريم (D-19). Never reuse the reference app's name, icon, screenshots, description or assets (D-19; r09 §1.1).

### 1.3 What we are / are not
| We are | We are not |
|---|---|
| Mushaf-first and audio-first, Indo-Pak-first | A super-app (no social feed, video, AI chat, shopping, live TV) |
| A faithful typeset of the Taj 16-line page | A scan viewer; a reflowed "text app" pretending to be a mushaf |
| Free, ad-free, offline, private | Freemium with Quranic content behind paywalls |
| Arabic always present; translations as aids | A translation-only reader (never) (D-09, D-22) |
| Retrieval of sourced content | A generator of tafsir, rulings or "AI answers" (r08 §2.14) |

### 1.4 Owner must-haves
| ID | Must-have (owner's words, paraphrased) | Delivered by |
|---|---|---|
| **MH-1** | Read the Quran page-by-page like the printed mushaf (Indo-Pak page, toolbar "Para · Page", bookmark / night mode / lock) | F-RD-01..07, F-BM-01/02 |
| **MH-2** | Listen to a favourite Qari for a selected Surah, Parah (Juz) or verse / verse range | F-AU-01..09, F-RD-04/05 |
| **MH-3** | Indexing by Parah, Surah and Manzil | F-IX-01..06, F-IX-09 |
| **MH-4** | In-demand extra features, prefer open-source / openly-licensed components | Translation, search, companion, khatam, hifz (all others) |

---

## 2. The problem

### 2.1 Reference app snapshot (r01 §1, §2.1)
"Al QURAN – القرآن الكريم" (`free.alquran.holyquran`, 9D TECH, Islamabad): **50M+ installs** (≈70.7M internal counter), **4.7★ from 1,135,804 ratings**, v4.4.46 (2026-10-14), #1 in Pakistan for "Quran Pak", "quran", "al quran", "holy quran urdu", "quran sharif" (r02 §1.1). Core = **scanned 16-line Taj Indo-Pak pages (548) + per-surah MP3 player + prayer times**. Pages are bitmaps with **no ayah mapping**; audio is one MP3 per surah hotlinked from quranicaudio (13 qaris) and mp3quran (7 qaris) (r06 §1.2).

### 2.2 Pain points (r01 §2.9: 2,991 reviews, 2020-04 → 2026-10-18; corpus enriched for 1–3★, so counts are relative weights)

| # | Pain point | Evidence (reviews · helpful votes) | Root cause | Our answer |
|---|---|---|---|---|
| 1 | **Too many / intrusive ads** | 649 · 39,292; **28 of 53 (53%)** of the newest 1–2★ reviews cite ads; 52% of sampled ≤3★ (r02 §2.6) | App-open, interstitial, native, rewarded ads | No ads ever (D-17) |
| 2 | **Inappropriate ad content** (dating, betting, riba loans, music, immodest) — often seen *in the mosque* | 91 · 3,064 | AdMob category blocking is "best effort" (r02 §2.7) | No ad SDK at all |
| 3 | **Paywalls / gated reciters** (rewarded ad unlocks a reciter for 24 h; downloaded audio asks for an ad) | 114 · 5,419; gated reciter 30 · 4,421; "premium lost / ads back" 35 · 2,837 | 6 paywall activities, 3-day trial | Every reciter, download, translation free (PP-2) |
| 4 | **No ayah highlight / follow-along** | 8 · 5,508; top review **+4,399** ("We are working on such a feature", 2022) | Bitmap pages, no timings | F-RD-05 + F-AU-03 |
| 5 | **Can't start from an ayah / seek / resume** | 25 · 3,383 | Per-surah MP3, no timing | F-AU-02 play-from-here, F-BM-01 listening pointer |
| 6 | **No Para (Juz) audio**, no ayah-range repeat, no download-all | Para audio ask; download-all +2,348 votes | Surah-only catalogue | F-AU-02 Para/Manzil/range; F-AU-08 Para/full download |
| 7 | **No translation** (listed but absent in the APK) | 88 · 14,065; asks as recent as 2026-10-16 | Marketing claim > product | F-TR-01 bundled Urdu + English |
| 8 | **Bitmap zoom, small text** | 29 · 8,911 ("pages expand beyond your screen") | Scans | Vector zoom F-RD-06; large-text reflow P3 |
| 9 | **Orientation lock doesn't work** | 15 · 5,062 | Bug | F-RD-07, persisted |
| 10 | **Bookmark/resume conflicts** (page-only bookmarks, reading elsewhere overwrites "last read", delete without confirm) | 42 · 10,345 | One global pointer | F-BM-01 separate pointers, F-BM-02 ayah bookmarks + undo, F-BM-04 per-khatam pointer |
| 11 | **Offline/internet confusion** | 145 · 14,433 | Streams by default; ads on offline audio | Offline-first (PP-3), clear download state |
| 12 | **Text / page errors** | 84 · 3,362 (e.g. "verses 71–79 missing", blank pages after p.447 in 2023) | Unverified scans | CI integrity invariants + hafiz certificate + report flow (F-PL-05, G5) |
| 13 | **Prayer times wrong / adhan unreliable / silent mode overridden** | 12 · 5,035 (+4,501 top); 23 · 4,837 | `ACCESS_NOTIFICATION_POLICY`, OEM killing | F-CP-01/02 reliability kit; never touch DND/ringer (D-16) |
| 14 | **Slow start / long splash** | 80 · 11,713 | Hadith splash, ad-first launch | NFR-01, no splash interstitial |
| 15 | **Privacy contradiction** — Data safety says "no data collected", APK bundles Ads SDK, AppsFlyer, Firebase suite, AD_ID | r01 §2.2 | — | Zero third-party SDKs; truthful Data safety (PRV-01/02) |
| 16 | **No Manzil / Ruku index**; 15-line edition missing for huffaz | r01 §2.7; 15-line 3 · 94 | Scans only | F-IX-03/04; F-RD-11 (P3) |
| 17 | Page-turn vibration always on | 5 · 884 | — | Haptics off by default |
| 18 | Developer responsiveness | Only 27 of 2,991 reviews got a reply, templated | — | Report flow with SLA (F-PL-05) |

What users **love** and we must keep: the bold Indo-Pak script that "matches my printed Quran" (91 · 8,829), easy/fast UI (164 · 29,132; "+6,045 high performance, low memory"), famous qaris (219 · 41,676), one-tap Resume, offline reading, night mode, keep-screen-on, post-prayer Ayatul Kursi reminder (r01 §1.9).

### 2.3 Market white space (r02 §2.8)
No app combines a faithful Indo-Pak page mushaf + ayah-highlighted audio on that page + Para/Manzil-wise listening + Urdu translation + no ads. Gaps we target: W2 highlight on Indo-Pak pages, W3 Para-wise listening, W4 ad-free with dignity, W5 Urdu-first meaning layer, W8 lightweight offline, W9 verified integrity, W10 Manzil dua / Ayatul Kursi / Friday Kahf quick access (v1); W1 13/15/16-line in one app, W6 huffaz tools, W7 group khatam (v1.x).

---

## 3. Users, personas, jobs-to-be-done

Context: Pakistan's device mix is dominated by aggressive-background-killing vendors — Transsion (Infinix/Tecno/itel) 25.5%, and with Vivo, Xiaomi, Oppo, realme ≈62% of devices (Sep 2026, r08 §1.2). Many are 2–4 GB RAM phones with capped mobile data.

### PER-0 — The owner (design partner for P1)
Pakistani entrepreneur-developer; reads daily from the 16-line Indo-Pak mushaf; wants a favourite Qari for Surah / Para / verse range. P1 is built for his routine first; his daily use is the P1 exit criterion (SM-01..05).

### PER-1 — "Abdul Rehman", the Pakistani daily reader (primary)
55, Lahore; Infinix/Samsung A-series phone; Urdu UI; reads a ruku or a Para after Fajr from the 16-line Taj print he learned on; mosque-goer; eyesight declining; intermittent data.
- **JTBD-1a:** *When I sit after Fajr, I want to open exactly where I stopped, on a page identical to my printed Quran, so I can continue my daily portion without hunting.* → F-IX-09, F-BM-01, F-RD-01
- **JTBD-1b:** *When I read in the mosque, I want nothing on screen except the Quran, so I am never embarrassed or distracted.* → PP-1, PP-2
- **JTBD-1c:** *When my eyes are tired, I want bigger, crisp text and a dark page, so I can read comfortably.* → F-RD-03, F-RD-06
- **JTBD-1d (P2; adhan v1.1):** *When prayer time comes, I want a reliable adhan that respects my silent phone in the masjid.* → F-CP-01/02
- Pains: ads, overwritten resume, bitmap zoom, wrong prayer times.

### PER-2 — "Hafsa", the hifz student, with her ustad / parent
14, Karachi madrasa student memorising from the 15/16-line Hafizi page; uses her mother's phone for listening and revision. **Ustad/parent** "Qari Sahib"/"Ammi" checks sabaq daily. (Play target audience is 13+; visuals stay age-neutral, D-22.)
- **JTBD-2a:** *When I revise my sabaq, I want to repeat one ayah or a ruku N times with a pause to recite back, so I can memorise without fiddling with the player.* → F-AU-05, F-AU-02 (P1)
- **JTBD-2b:** *When I memorise, I need the page to never change, so my visual memory of the page holds.* → PP-4, NFR-16
- **JTBD-2c (P3):** *When I test myself, I want to hide the text, reveal word by word, mark mistakes and track sabaq/sabqi/manzil.* → F-CP-12
- **Ustad/parent JTBD (P3):** *When I check progress, I want a simple weekly record of what was memorised and where mistakes recur.* → F-CP-12

### PER-3 — "Bilal", the commuter listener
32, Karachi; rides a bike/bus 1 h a day with earbuds; listens to a Para per day to finish a khatam by listening; 10–20 GB/month data.
- **JTBD-3a:** *When I start my commute, I want one tap to continue my Para with my Qari from the last ayah, with lock-screen and headset controls.* → F-AU-02, F-AU-07, F-BM-01 listening pointer
- **JTBD-3b:** *When I'm on Wi-Fi at home, I want to download Paras ahead so the commute costs no data.* → F-AU-08
- **JTBD-3c:** *When I fall asleep listening at night, I want it to stop at the end of the surah.* → F-AU-06
- P3: Android Auto (F-AU-11).

### PER-4 — "Sana", the diaspora reader (UK / Gulf)
38, Manchester (British-Pakistani) — or a Pakistani worker in Dubai/Riyadh. English UI (Gulf workers often Urdu); wants the *same* Indo-Pak mushaf as at home, English (or Urdu) meaning, and correct local prayer times; teaches her children at home.
- **JTBD-4a:** *When I teach my children, I want the Indo-Pak page with English meaning per ayah, so they learn the script I learned.* → F-RD-01, F-RD-08, F-TR-01
- **JTBD-4b (P2):** *When I'm in the UK, I want prayer times that follow the UK method and handle summer high latitude, and a Ramadan timetable I can share.* → F-CP-01 region presets, F-CP-07
- **JTBD-4c (P2):** *When Ramadan or Eid is near, I want the Hijri date that matches my community (Saudi or Pakistani sighting).* → F-CP-04

### Anti-personas
People seeking a scholarly tafsir research tool (Greentech/Quran.com serve them); users wanting a Madani-only app in v1; anyone wanting social/competitive features.

---

## 4. Product principles

| ID | Principle | What it means in practice (tests for any design decision) |
|---|---|---|
| **PP-1** | **Quran-first and respectful** | Nothing competes with the page: no promos, no cross-promotion, no animated or human/animal imagery, no reciter photos (D-12), no music. Arabic is always shown with any translation (D-09). Notifications are devotional, sourced, capped (≤3/day non-prayer). No generated religious text. Copy is dignified, never guilt- or shame-based. |
| **PP-2** | **No ads, no tracking, no paywall on the Quran** | Zero ad/analytics/attribution SDKs (D-20). Every reciter, download, translation, index and reading feature is free forever (D-17). Optional support appears only in Settings (v1.5), never in the reading or listening path. |
| **PP-3** | **Offline-first** | First run needs no network. Reading, indexes, search, bookmarks, translations, downloaded audio, prayer times, qibla, Hijri work in airplane mode. Network is used only to fetch static signed files and audio (NFR-08). |
| **PP-4** | **Fidelity to the printed Taj mushaf** | Same words on the same 16 lines on the same printed page number; Taj furniture (header, «منزل», ruku ع triple, alternating margins); Indo-Pak Para boundaries and names; pages byte-stable across updates; every layout proofread by a qualified hafiz before public release (G5). When in doubt, the print wins. |
| **PP-5** | **Accessible and inclusive** | Urdu as a first-class UI language; elderly-friendly text sizes; TalkBack-usable; 200% font scale; no maslak bias in defaults (sect-balanced, documented content policy, D-22). |
| **PP-6** | **Fast and light** | Opens to Home in ≤ 0.5 s on a Pixel 6a-class phone (≤ 1.2 s mid-range, ≤ 1.5 s on a 3 GB low-end phone), smooth swipes on 2 GB phones, ≤ 25 MB download (NFR-01..07). Audio and extra content are opt-in downloads with visible sizes. |
| **PP-7** | **Honest and transparent** | Truthful Data safety; a Sources & Credits screen naming every source and licence; "About this mushaf" provenance; easy "Report text error" with a public fix SLA; never claim features we don't have (the reference's #1 trust failure). |
| **PP-8** | **Calm engagement** | Private streaks with freeze days, no leaderboards (riya), no notification spam, no dark patterns, at most one rating ask after a positive milestone. |

---

## 5. Release framing

| Phase | Audience | Content eligibility | Theme |
|---|---|---|---|
| P1 Personal MVP (daily use from **2026-12-21**) | Owner (personal build) | Personal-build content allowed | "Read + listen + find" — the Quran core |
| P2 v1.0 (closed test from 2027-01-11 at the latest — 2026-12-28 if G1 is granted and the build is ready; staged rollout from 2027-02-01, before Ramadan 1448 ≈ 8–9 Feb 2027) | Public — **only if G1, G2 (public catalogue), G3, G4, G5, G6 clear** and the 2026-11-30 checkpoint keeps the Ramadan public plan; else a closed-test/beta release during Ramadan and public v1.0 on 2027-03-22 merged with v1.1 (D-34; [Roadmap](09-roadmap-backlog.md) §4.2, §5.3) | Closed test: BETA tier (mushaf data G1-cleared or clean-room only, §0). Production: PUBLIC tier | Quran core + compliance + prayer times/Sehri-Iftar, Hijri, Ramadan core, khatam planner |
| v1.1 (2027-03-22, after the Ramadan hotfix-only freeze) | Public | PUBLIC tier + permitted packs | The P2 features labelled v1.1 in §6: adhan + health check, Qibla, azkar, widgets, reminders, share image, translation packs + translator choice, notes, streaks, JSON export, auto-scroll, page curl, rating prompt, launcher shortcuts, opt-in crash reports |
| P3 v1.x (Feb → Jun 2027) | Public | + permitted packs | Hifz, 15/13-line, Auto, spread, sync, word highlight, supporter IAP (v1.5) |
| P4 v2 (H2 2027) | Public | + permitted packs | AI follow-along, tajweed, Madani, WBW, Qaida, Wear OS |

**What the 2026-12-21 daily-use build contains.** Reader, indexes, audio scopes + highlight, downloads, translation view, last-read and bookmarks, and the Urdu switch (strings may be partial). Several P1 Musts/Shoulds are carried into S5–S7 and land before the closed test (doc 09 tier C): search (F-SR-01..03), repeat/pauses (F-AU-05), onboarding (F-PL-01), storage manager (F-PL-06), accessibility pass (F-PL-07), pinch zoom (F-RD-06), ruku/sajda lists (F-IX-04/06), bookmark collections + undo (F-BM-02), gapped mode (F-AU-04) and complete Urdu strings. The P1-exit metrics (§13.1) are judged against what has landed by each evaluation date.

**Scope ladder.** The cut and pull-in order is owned by [Roadmap](09-roadmap-backlog.md) §5.1–§5.2: every v1.1 feature is cut by default; v1.0-stretch items (F-RD-13, F-IX-07 list, F-CP-05, Manzil-dua playlist) are pre-cut and restored only with spare capacity; F-CP-02 adhan enters v1.0 only if the freelancer lever is pulled at the 2026-11-30 checkpoint and it clears the 2027-01-18 merge rule. **Never cut from v1.0 (the floor):** F-PL-04, F-PL-05, F-PL-11 and all P1 Musts — including F-RD-06 pinch zoom and F-BM-02 bookmark undo. If a floor item is late, the public date moves rather than the item. **Tier A** — F-CP-01 (with Sehri/Iftar), F-CP-04 (+ PK feed), F-CP-07 (core ACs) and F-BM-04 — is committed for v1.0; it may be dropped only through doc 09's last-resort rungs L5–L8 (Ramadan core first, khatam planner last) at a velocity checkpoint (doc 09 §4.2). Whether Ramadan gets a public launch or a closed-test release follows the pre-agreed launch rule (D-34: overrun ratio ≥ 1.3 at the 2026-11-30 checkpoint → Ramadan beta, public v1.0 on 2027-03-22).

---

## 6. Feature catalogue

Columns: **Phase** = where the feature was specified (D-25); **Release** = where it is planned to ship (§0; plan of record in [Roadmap](09-roadmap-backlog.md) §5 — a split cell such as "v1.0 / v1.1" names the parts in order); **MoSCoW** for that release; **MH** = owner must-have; **Depends on** = features, decisions, gates, data.

### 6.1 Reader
| ID | Feature | Description | Phase | Release | MoSCoW | MH | Depends on |
|---|---|---|---|---|---|---|---|
| F-RD-01 | Typeset 16-line Indo-Pak pager | 548 typeset pages (labels 2–549), RTL swipe, Taj furniture, ayah-aware word boxes | P1 | v1.0 | Must | MH-1 | D-05 font, D-06 layout (G1 for public), D-07 tables, G5 |
| F-RD-02 | Reader chrome | Toolbar "Para N · Page L · Surah", tap-to-toggle immersive, page scrubber, keep-screen-on, haptics toggle | P1 | v1.0 | Must | MH-1 | F-RD-01 |
| F-RD-03 | Themes | Light, sepia, night, AMOLED; follow system; vector recolour (never inverted image) | P1 | v1.0 | Must | MH-1 | F-RD-01 |
| F-RD-04 | Ayah action sheet | Long-press ayah / range → play from here, play/repeat, bookmark, copy, share, translation, tafsir summary, (P2) note, share image, report | P1 | v1.0 | Must | MH-1, MH-2 | F-RD-01, F-AU-02, F-BM-02, F-TR-01 |
| F-RD-05 | Audio follow-along | Ayah highlight synced to audio + auto page-turn (suspended 8 s after touch) | P1 | v1.0 | Must | MH-2 | F-AU-03 timing packs, D-13 |
| F-RD-06 | Zoom | Pinch zoom with crisp vector re-layout [P1]; "large text" reflow mode [P3] | P1 / P3 | v1.0 / v1.x | Must / Could | MH-1 | F-RD-01 |
| F-RD-07 | Locks | Rotation lock (phones only, hidden ≥ 600 dp) + page-turn lock (all devices) | P1 | v1.0 | Must | MH-1 | D-23 |
| F-RD-08 | Translation view | Ayah list: Arabic (Indo-Pak) + translation(s), synced position with mushaf | P1 | v1.0 | Must | MH-4 | F-TR-01 |
| F-RD-09 | Auto-scroll | Vertical continuous scroll with speed control, pause on touch | P2 | v1.1 | Should | MH-1 | F-RD-01 |
| F-RD-10 | Two-page spread | RTL spread on ≥ 600 dp / foldables | P3 | v1.x | Should | MH-1 | F-RD-01, D-23 |
| F-RD-11 | Extra layouts | 15-line Hafizi (610 pp) & 13-line Taj (847 pp) [P3]; Madani 15-line (604) [P4] | P3 / P4 | v1.x / v2 | Should | MH-1 | Per-layout data permission + G5 (D-04) |
| F-RD-12 | Page-curl animation | Optional curl transition | P2 | v1.1 | Could | MH-1 | F-RD-01 |
| F-RD-13 | Tafsir-summary sheet | Ahsan-ul-Bayan footnotes in a formatted sheet from mushaf or translation view | P2 | v1.0-stretch | Should | MH-4 | F-TR-01 |
| F-RD-14 | Word-by-word | Tap word → meaning (Urdu/English) | P4 | v2 | Could | MH-4 | WBW licence (D-09) |
| F-RD-15 | Tajweed colours | Colour-coded tajweed rules | P4 | v2 | Could | MH-4 | Data/licence; Madani first |

### 6.2 Index & navigation
| ID | Feature | Description | Phase | Release | MoSCoW | MH | Depends on |
|---|---|---|---|---|---|---|---|
| F-IX-01 | Surah index | 114 rows, Read + Play + download state, filter | P1 | v1.0 | Must | MH-3 | D-07 |
| F-IX-02 | Para index | 30 rows, Indo-Pak names, start ayah & page, rukus, progress, Read + Play | P1 | v1.0 | Must | MH-3 | D-07 Para table |
| F-IX-03 | Manzil index | 7 rows, ranges, Read + Play | P1 | v1.0 | Must | MH-3 | D-07 |
| F-IX-04 | Ruku navigation | 558 rukus: per-surah list, margin-marker tap, next/prev, Play ruku | P1 | v1.0 | Should | MH-3 | D-07 ruku table |
| F-IX-05 | Go to | Page label / surah:ayah / Para number | P1 | v1.0 | Must | MH-3 | — |
| F-IX-06 | Sajda list | 14 Hanafi sajdas + 22:77 (Shafi'i) labelled | P1 | v1.0 | Should | MH-3 | D-07 |
| F-IX-07 | Para quarters | 90 ربع / نصف / ثلٰثة markers on page + navigation | P2 | v1.0-stretch (list; page markers ship in v1.0) | Should | MH-3 | D-07 quarter table |
| F-IX-08 | Quick access | Yaseen, Rahman, Mulk, Waqiah, Kahf, Ayatul Kursi, Manzil dua, last two ayat of Baqarah | P1 | v1.0 | Should | MH-4 | F-AU-02 |
| F-IX-09 | Home | Continue-reading card, resume-listening card, recents, quick access, index entry | P1 | v1.0 | Must | MH-1, MH-3 | F-BM-01 |

### 6.3 Bookmarks & progress
| ID | Feature | Description | Phase | Release | MoSCoW | MH | Depends on |
|---|---|---|---|---|---|---|---|
| F-BM-01 | Last-read pointer | Auto pointer (dwell-based), separate from bookmarks; listening pointer | P1 | v1.0 | Must | MH-1 | user.db |
| F-BM-02 | Bookmarks | Ayah & page bookmarks, collections with colours, undo | P1 | v1.0 | Must | MH-1 | user.db |
| F-BM-03 | Notes | Plain-text note per ayah | P2 | v1.1 | Should | MH-4 | F-BM-02 |
| F-BM-04 | Khatam planner | Multiple khatams, Para checklist, daily goal, per-khatam pointer | P2 | v1.0 | Must | MH-4 | D-07 Para/ruku tables |
| F-BM-05 | Streaks & stats | Private, freeze days, weekly summary | P2 | v1.1 | Could | MH-4 | F-BM-04 |
| F-BM-06 | Backup | Auto Backup [P1] + JSON export/import via SAF [P2] | P1 / P2 | v1.0 (Auto Backup) / v1.1 (JSON) | Must / Should | MH-4 | D-20 |
| F-BM-07 | Cloud sync | "Sign in with Quran.com" (QF User APIs, PKCE) | P3 | v1.x | Could | MH-4 | QF client approval |

### 6.4 Audio
| ID | Feature | Description | Phase | Release | MoSCoW | MH | Depends on |
|---|---|---|---|---|---|---|---|
| F-AU-01 | Reciter catalogue | Signed remote catalogue + bundled fallback, favourite Qari, no photos | P1 | v1.0 | Must | MH-2 | D-12, G2 (public list) |
| F-AU-02 | Play scopes | Surah, Para, Manzil, Ruku, Page, AyahRange, Ayah, play-from-here | P1 | v1.0 | Must | MH-2 | D-07, D-13 PlanExpander |
| F-AU-03 | Gapless + highlight timings | Per-surah files + validated timing packs, per-surah highlight kill-switch | P1 | v1.0 | Must | MH-2 | Timing packs ([Pipelines](08-data-pipelines.md)) |
| F-AU-04 | Verse-by-verse (gapped) | Per-ayah files for exact ayah/hifz use | P1 personal; public after G2 | v1.0 personal + closed test; PUBLIC after G2 | Should | MH-2 | G2 |
| F-AU-05 | Repeat & pauses | Ayah × N, range × M / ∞, pause after ayah (fixed / proportional) | P1 | v1.0 | Must | MH-2 | F-AU-02 |
| F-AU-06 | Speed & sleep timer | 0.5–2.0×; minutes or end of ayah/surah/Para; fade-out | P1 | v1.0 | Should | MH-2 | — |
| F-AU-07 | Background playback & player | Mini player + full player, media notification, lock screen, headset/Bluetooth, audio focus | P1 | v1.0 | Must | MH-2 | Media3 (D-13) |
| F-AU-08 | Downloads | Surah / Para / Manzil / full Quran, resumable, verified, Wi-Fi-only default, storage manager | P1 | v1.0 | Must | MH-2 | D-14 |
| F-AU-09 | Streaming cache | 256 MB LRU cache for streamed audio | P1 | v1.0 | Must | MH-2 | D-14 |
| F-AU-10 | Translation-audio interleave | Arabic ayah then Urdu translation audio | P1 personal; P3 public if permitted | v1.1 personal; public v1.x if rights granted | Could | MH-2 | Shamshad Ali Khan rights (D-12) |
| F-AU-11 | Android Auto | Browse + play scopes in the car | P3 | v1.x | Should | MH-2 | MediaLibraryService |
| F-AU-12 | Word-level highlight | Word-by-word highlight for reciters with word segments | P3 | v1.x | Could | MH-2 | Word timing data |

### 6.5 Translation
| ID | Feature | Description | Phase | Release | MoSCoW | MH | Depends on |
|---|---|---|---|---|---|---|---|
| F-TR-01 | Bundled translations | Urdu Junagarhi (+ Ahsan-ul-Bayan footnotes), English Saheeh International | P1 | v1.0 | Must | MH-4 | D-09 |
| F-TR-02 | Translation packs | Download Jalandhari, Kanz-ul-Iman, Pickthall, Hilali-Khan, Hindi/Bangla/Indonesian/Turkish | P2 | v1.1 | Should | MH-4 | Signed pack index (D-10) |
| F-TR-03 | Translator choice | Pick 1–2 active translations per language | P2 | v1.1 | Should | MH-4 | F-TR-02 |
| F-TR-04 | Transliteration | English / Roman-Urdu | P3 | v1.x | Could | MH-4 | Licensed data |
| F-TR-05 | Tafsir packs | Longer tafsir (permission-based) | P3 | v1.x | Could | MH-4 | Permissions |

### 6.6 Search
| ID | Feature | Description | Phase | Release | MoSCoW | MH | Depends on |
|---|---|---|---|---|---|---|---|
| F-SR-01 | Arabic search | Diacritic- and spelling-insensitive, words + substrings | P1 | v1.0 | Must | MH-4 | D-11 |
| F-SR-02 | Translation search | Urdu & English full text | P1 | v1.0 | Should | MH-4 | D-11, F-TR-01 |
| F-SR-03 | Alias search | "Yaseen", "Ayatul Kursi", "Manzil dua", Roman spellings | P1 | v1.0 | Should | MH-3 | Alias table |
| F-SR-04 | Topic search | Curated topics (retrieval only) | P3 | v1.x | Could | MH-4 | QUL permission |
| F-SR-05 | Recite-to-search | Voice by recitation | P4 | v2 | Could | MH-4 | F-CP-14 model |

### 6.7 Companion
| ID | Feature | Description | Phase | Release | MoSCoW | MH | Depends on |
|---|---|---|---|---|---|---|---|
| F-CP-01 | Prayer times | adhan2, region presets (PK Karachi + Hanafi), offsets, monthly table, offline city picker | P2 | v1.0 | Must | MH-4 | D-15, D-16 |
| F-CP-02 | Adhan alarms + health check | Per-prayer mode, exact alarms, full/short adhan, silent/DND respect, OEM guidance | P2 | v1.1 | Must (for v1.1) | MH-4 | F-CP-01, commissioned adhan audio |
| F-CP-03 | Qibla | Bearing + rotation-vector compass + declination, calibration | P2 | v1.1 | Should | MH-4 | F-CP-01 location |
| F-CP-04 | Hijri calendar | HijrahDate (Umm al-Qura) ± offset, Maghrib rollover, PK month-start feed, events | P2 | v1.0 | Must | MH-4 | D-15, R2 feed |
| F-CP-05 | Tasbeeh | Counter, presets, targets, haptics, volume keys, history | P2 | v1.0-stretch | Should | MH-4 | — |
| F-CP-06 | Azkar & duas | Morning/evening azkar, Manzil dua, ~150 core duas, sourced | P2 | v1.0-stretch (Manzil-dua playlist) / v1.1 (azkar & duas) | Should | MH-4 | Curated dataset + Urdu translation |
| F-CP-07 | Ramadan | Sehri/Iftar timetable, countdown, Sehri alarm, sourced duas, Taraweeh khatam plan | P2 | v1.0 (core) / v1.1 (extras) | Must | MH-4 | F-CP-01, F-CP-04, F-BM-04 |
| F-CP-08 | Widgets | Continue reading, next prayer, ayah of the day (Glance) | P2 | v1.1 | Should | MH-4 | F-BM-01, F-CP-01 |
| F-CP-09 | Smart reminders | Kahf Friday, Mulk night, daily ayah, opt-in Ayatul Kursi; ≤ 3/day; tap-to-play | P2 | v1.1 | Should | MH-4 | F-CP-01, curated list |
| F-CP-10 | Share ayah | As image or text, with reference + translator attribution | P2 | v1.1 | Should | MH-4 | F-RD-01, F-TR-01 |
| F-CP-11 | Group khatam | WhatsApp-shareable sheet [P2] → realtime Firestore [P3] | P2 / P3 | v1.1 (sheet) / v1.x (realtime) | Could | MH-4 | F-BM-04; Firebase (play flavor) |
| F-CP-12 | Hifz mode | Sabaq/sabqi/manzil, hide/reveal, FSRS, mistakes heatmap, record-yourself | P3 | v1.x | Should | MH-4 | Word boxes, F-AU-04 |
| F-CP-13 | Noorani Qaida | Own content + recorded audio, age-neutral | P4 | v2 | Could | MH-4 | Recording budget |
| F-CP-14 | AI recitation follow-along | On-device ASR follow-along (spike in P3) | P4 | v2 (spike v1.x) | Could | MH-4 | sherpa-onnx spike |

### 6.8 Platform
| ID | Feature | Description | Phase | Release | MoSCoW | MH | Depends on |
|---|---|---|---|---|---|---|---|
| F-PL-01 | Onboarding | UI language, mushaf preview, favourite Qari, (P2) location + Asr + notifications | P1 | v1.0 | Must | MH-1/2 | F-AU-01 |
| F-PL-02 | Settings | All preferences (§7.7) | P1 | v1.0 | Must | — | DataStore |
| F-PL-03 | Urdu + English UI | Full parity, RTL, per-app language | P1 | v1.0 | Must | MH-4 | D-21 |
| F-PL-04 | Sources & Credits | Every content source + licence + attribution, OSS licences, About this mushaf | P2 | v1.0 | Must | MH-4 | D-10 metadata, AboutLibraries |
| F-PL-05 | Report text error | Structured report via email intent; fix SLA; remote kill-switch | P2 | v1.0 | Must | — | D-10 kill-switch, D-22 |
| F-PL-06 | Content-pack & storage manager | Bundled/downloaded content, audio per reciter, cache, location | P1 | v1.0 (storage) / v1.1 (packs) | Must | MH-2 | F-AU-08 |
| F-PL-07 | Accessibility baseline | TalkBack, 200% text, contrast, targets, custom actions | P1 | v1.0 | Must | — | NFR-12 |
| F-PL-08 | Supporter IAP | One-time "Support / Sadqa-e-Jaria" (play flavor) | P3 (v1.5) | v1.5 | Could | — | Commercial-content audit (D-17) |
| F-PL-09 | Opt-in crash reports | ACRA, explicit consent, declared; reports go to a small Cloudflare Worker that stores them in private R2 for 90 days (D-32) | P3 (v1.1) | v1.1 | Should | — | D-20 |
| **F-PL-10** *(new)* | Gentle rating prompt | Play In-App Review, play flavor only, once after a positive milestone | P2 | v1.1 | Could | — | Play In-App Review library |
| **F-PL-11** *(new)* | Signed remote notices | Signed `notices.json` ([Architecture](04-architecture.md) §14.3): text-error notices on affected pages, takedown/disable of a pack, recitation, surah highlight or feature, `minVersion` update nudge; expiry per notice; never changes Quran text or layout | P2 | v1.0 | Must | — | D-10, D-22, D-30, F-PL-05 |
| **F-PL-12** *(new)* | Launcher shortcuts | Static launcher shortcuts: Continue reading, Continue listening (favourite qari), Qibla, Tasbeeh — same deep links as in-app navigation ([UX/UI](02-ux-ui-spec.md) §2.5) | P2 | v1.1 | Could | — | F-BM-01, F-AU-02, F-CP-03, F-CP-05 |

---

## 7. Detailed requirements — P1 (Personal MVP, daily use from 2026-12-21; tier-C items land in S5–S7, §5)

### 7.1 Reader

#### F-RD-01 — Typeset 16-line Indo-Pak pager [P1 · Must · MH-1]
**Stories.** As PER-1, I want each screen to be the same page as my printed 16-line Taj mushaf (same words on the same lines, same page number, same markers) so that I read and memorise by page position. As PER-2, I want the page to stay identical across app updates.

**Acceptance criteria**
1. **Given** a fresh install with no network, **when** the user opens the reader, **then** the mushaf renders from bundled data (content DB + DigitalKhatt IndoPak glyf-TTF), with no download step.
2. **Given** page label L is shown, **when** the user drags the page to the right (the next page enters from the left, as in a printed mushaf), **then** page L+1 is shown; dragging left shows L−1; label 2 is the first page and 549 the last (no overscroll page).
3. **Given** any of the 548 pages, **when** it renders, **then** it has exactly 16 line slots and each line's word sequence equals the layout data (QUL resource 11 ↔ DigitalKhatt 83,669-token stream), verified by an automated per-page check; lines are fully justified from precomputed word positions (no letter-spacing, no `TextAlign.Justify`) (D-05).
4. **Given** any page, **then** the header shows Para opening words + Para number on the right, page label in the centre, surah name(s) + number(s) on the left; the footer shows «منزل N» = manzil of the page's last ayah; margin notes (ruku ع triple, quarter labels (F-IX-07 table; drawn from v1.0 even if the quarter list slips), sajda, «الجزء», «المنزل») sit in the **outer** margin — right on even printed pages, left on odd (g02 §1.11).
5. **Given** label 2 (Fatiha) and label 3 (2:1 …), **then** both use the sar-lauh layout (ornamental panel with the title, a basmala box, then 5 text lines at the bottom; QUL index 1 is remapped accordingly); Fatiha shows **7 empty ayah circles including the basmala** and the «۵» sign (with «لا») at «اَنْعَمْتَ عَلَيْهِمْ» (D-06, g02 §1.4).
6. **Given** a ruku ends on the page, **then** its margin ع shows the triple (top) ruku-in-surah / (inside) ayahs-in-ruku / (bottom) ruku-in-Para, numbered in the Para where the ruku ends, Fatiha excluded from Para-1 numbering (e.g. 2:1–7 → ۱/۷/۱; 2:249–253 → ۳۳/۵/۱) (D-07).
7. **Given** one of the 118 disputed-end «۵» marks, **then** it is drawn as a glyph only and is never an ayah boundary for selection, highlight, audio or counts (D-06).
8. **Given** a surah starts on the page, **then** its header follows the layout's line types (title line + basmala line, or three-part line); surah 9 has a title and no basmala; the p.529 (index 528) basmala/header order is the corrected one (D-06).
9. **Given** the whole dataset, **then** CI proves: 6,236 ayahs each appearing exactly once and in order across 548 pages; Para starts on the expected pages (Para 1 at label 2; Para n = 2–28 at label 18n−15, e.g. Para 2 = 21, Para 4 = 57, Para 23 = 399; Para 29 = 509; Para 30 = 529) (D-07; [Quality](10-quality-testing-ci.md)).
10. **Given** a phone in landscape, **then** the page fits the screen width and scrolls vertically; on ≥ 600 dp in P1–P2 a single page is centred at its max readable width.
11. **Given** screenshot tests on the golden page set owned by [Quality](10-quality-testing-ci.md) (about 60 rule-selected pages on every PR, all 548 nightly), which must include labels 2, 3, 21, 57, 237, 399, 509, 529, 549 and every page with two surah headers, **then** rendered output matches the approved goldens pixel-for-pixel at the reference size.

**Edge cases.** Pages with two or three surah names in the header; a page that starts mid-ayah; a sajda and a ruku mark on the same line; very narrow screens (320 dp) — the page scales down uniformly, never reflows; system font scale does **not** change mushaf size (zoom does); font file corrupted → integrity check at startup fails closed with a "reinstall" message (no garbled page shown). Before G1 clears, the QUL-derived layout is personal-build only; closed-test (BETA) and public builds use it only after written permission, else the clean-room layout (D-06, D-29).

#### F-RD-02 — Reader chrome [P1 · Must · MH-1]
**Story.** As PER-1, I want the page to fill the screen and the controls to appear only when I tap, without covering the text.

```
┌──────────────────────────────────────────┐
│ ‹   Para 4 · Page 57 · Āl-ʿImrān   🔖 🌙 🔒 ⋮ │  ← top bar (tap to show/hide)
│ ┌──────────────────────────────────────┐ │
│ │  لَنْ تَنَالُوْا ۴   ·   ۵۷   ·   اٰلِ عِمْرٰن ۳ │ │  ← printed header (part of page)
│ │  ═══════════ 16 typeset lines ═══════ │ │
│ │                          منزل ۱      │ │
│ └──────────────────────────────────────┘ │
│ ▶ mini-player (if audio active)           │
│ ◀━━━━━━━━━━━●━━━━━━━━━━━━━━━━━▶ 57 / 549   │  ← page scrubber (RTL)
└──────────────────────────────────────────┘
```

**Acceptance criteria**
1. **Given** the reader opens, **then** it is immersive (system bars hidden) unless "Keep system bars visible" is on; **when** the user single-taps a non-interactive point, **then** the top bar, bottom bar and system bars toggle together; a page swipe hides the chrome.
2. **Given** the chrome is visible, **then** the top bar shows "Para N · Page L · Surah" (Urdu UI: «پارہ ۴ · صفحہ ۵۷ · آل عمران»), where Surah = the surah of the first ayah on the page, Para from the Indo-Pak table, L = page label; actions: back, page bookmark toggle, theme quick-toggle (night ↔ last light theme), lock menu (F-RD-07), overflow (go to, translation view, rukus, search, settings).
3. **Given** the chrome toggles, **then** the page does not move or reflow (bars overlay the margins; the page's text area is never covered at default zoom on a 360 × 760 dp phone).
4. **Given** the user drags the page scrubber, **then** a preview label "Page 57 · Para 4 · Āl-ʿImrān" follows the thumb and release jumps there; scrubbing never updates the last-read pointer by itself (F-BM-01).
5. **Given** "Keep screen on while reading" (default **on**, releasing after **15 min** without touch or page turn; options 5 / 15 / 30 min / never), **when** the reader is visible, **then** the screen does not time out until that inactivity period has passed; it stays on while audio plays with the reader visible; leaving the reader restores the system timeout.
6. **Given** "Vibrate on page turn" (default **off**), **then** page turns produce no haptic unless enabled.

**Edge cases.** Display cutouts / gesture navigation: chrome respects insets; tap on an ayah with chrome hidden toggles chrome (selection needs long-press, F-RD-04); TalkBack on: chrome is always reachable (never auto-hidden while focused).

#### F-RD-03 — Themes [P1 · Must · MH-1]
**Acceptance criteria**
1. **Given** Settings → Theme, **then** options are System (default: Light/Night following the OS), Light, Sepia, Night, AMOLED black; the reader and app chrome switch without restart in < 200 ms.
2. **Given** any theme, **then** text, ornaments, markers and highlight are recoloured as vectors (never an inverted image); body text contrast ≥ 7:1 and highlight/selection vs background ≥ 3:1, and Arabic text over the highlight still ≥ 4.5:1 (NFR-12).
3. **Given** the toolbar night toggle, **then** it switches between Night and the last-used light theme and persists.

#### F-RD-04 — Ayah action sheet [P1 · Must · MH-1, MH-2]
**Stories.** As PER-1, I want to long-press an ayah to play from it, bookmark it, copy or share it, or read its meaning. As PER-2, I want to select a range and repeat it.

**Acceptance criteria**
1. **Given** a page, **when** the user long-presses any word, **then** the whole ayah (all its word boxes across lines and pages) is selected with the selection colour and the action sheet opens with: **Play from here**, **Play this ayah**, **Repeat…** (opens F-AU-05 presets), **Bookmark** (collection picker), **Copy**, **Share text**, **Translation** (Urdu/English sheet), **Tafsir summary** (Junagarhi footnotes for this ayah; shown in the translation sheet until the formatted sheet F-RD-13 lands, v1.0-stretch), **Report text error** (F-PL-05, v1.0), and from v1.1 **Note** (F-BM-03) and **Share as image** (F-CP-10).
2. **Given** an ayah is selected, **when** the user taps "Select range" and then taps another ayah (same or later page, any direction), **then** the inclusive range is selected and the sheet shows range actions: Play range, Repeat range, Bookmark range start, Copy range, Share range.
3. **Given** Copy on 2:255, **then** the clipboard holds Tanzil Uthmani text verbatim (D-08) with private-use and control characters stripped, followed by "— Al-Baqarah 2:255 · Para 3" (Urdu UI: «— البقرة ۲:۲۵۵ · پارہ ۳»), and, if "Include translation" is on, the active translation followed by the translator credit (e.g. "Saheeh International (Noor International Center)").
4. **Given** a long-press on a surah header or a non-Fatiha basmala line, **then** a surah sheet opens (Play surah, Read translation of surah, Download surah) — the basmala is not an ayah outside Fatiha.
5. **Given** a long-press on a margin ruku ع, **then** a ruku sheet opens: "Ruku 2 of Al-Baqarah · 13 ayahs · Ruku 2 of Para 1" with Play ruku / Repeat ruku (F-IX-04).
6. **Given** the sheet is dismissed, **then** the selection clears; **given** audio is playing, **then** the playback highlight (F-RD-05) remains visually distinct from the selection.

**Edge cases.** Long-press on a «۵» mark selects its containing ayah; an ayah spanning two pages selects across the page boundary (the sheet still opens on the current page); touch within 8 dp of a word box counts as that word; zoomed state keeps hit-testing exact.

#### F-RD-05 — Audio follow-along highlight + auto page-turn [P1 · Must · MH-2]
**Story.** As PER-1/PER-3, I want the ayah being recited to light up on the page and the page to turn by itself, so I can read along with my Qari.

**Acceptance criteria**
1. **Given** a recitation with a validated timing pack is playing, **when** playback crosses an ayah boundary, **then** the highlight moves to the new ayah's word boxes within ≤ 100 ms of the boundary in the timing data (engine latency; data accuracy is validated by the pipeline, [Pipelines](08-data-pipelines.md)).
2. **Given** the next ayah begins on the next page, **when** that ayah starts, **then** the pager turns to that page automatically — **unless** the user touched or swiped the pager in the last 8 s, in which case a "Follow audio" chip appears; tapping it (or 8 s without touch at the next boundary) returns to the playing page (D-13).
3. **Given** a surah 2–8 or 10–114 starts with its basmala intro (timing "ayah 0"), **then** the basmala line is highlighted during the intro; for Fatiha 1:1 the basmala is ayah 1; surah 9 has no basmala.
4. **Given** the catalogue/timing kill-switch disables highlight for a surah of a reciter, **then** audio plays normally, no highlight or auto-turn happens for that surah, and a one-line notice "Highlight isn't available for this surah with this Qari" appears once per session.
5. **Given** playback is paused, **then** the highlight stays on the current ayah; on stop it clears.
6. **Given** the translation view (F-RD-08) is open, **then** the playing ayah row is highlighted and auto-scrolled into view with the same 8 s touch suspension.
7. **Given** Settings → Audio → "Highlight while playing" or "Turn pages automatically" is off, **then** that behaviour is disabled (both default on).

**Edge cases.** User navigates to a different surah while audio plays: no forced jump until the 8 s rule; page-turn lock (F-RD-07) blocks user swipes but **not** audio auto-turn; app in background: highlight state resumes correctly when returning; repeat loops re-highlight from the loop start.

#### F-RD-06 — Pinch zoom [P1 · Must · MH-1] (large-text reflow in P3)
**Acceptance criteria**
1. **Given** a page, **when** the user pinches, **then** zoom ranges 1.0×–2.0×, panning is enabled above 1.0×, and on release the page re-lays out at the new size and is crisp (vector, no bitmap blur).
2. **Given** the user zooms then swipes to another page, **then** the zoom level persists across pages and app restarts (per orientation) — fixing "zoom resets every page" (r02 §2.4).
3. **Given** double-tap, **then** zoom toggles between 1.0× and the last non-1.0 zoom (default 1.5×).
4. **Given** zoom > 1.0×, **then** horizontal swipes pan first and turn the page only at the edge.
5. P3: above 1.6× an option "Large text mode" offers a reflowed ayah layout (F-RD-06 P3).

#### F-RD-07 — Rotation lock + page-turn lock [P1 · Must · MH-1]
**Acceptance criteria**
1. **Given** a phone (smallest width < 600 dp), **when** the user taps 🔒 → "Lock rotation", **then** the current orientation is locked and persists across sessions until unlocked; the lock state is shown by the icon and announced by TalkBack.
2. **Given** smallest width ≥ 600 dp, **then** "Lock rotation" is hidden (Android 16 ignores app orientation locks on large screens, D-23).
3. **Given** 🔒 → "Lock page", **then** swipes and scrubber are disabled (protects against accidental turns by children or in Taraweeh); a small "Page locked — tap to unlock" pill is shown on tap; audio auto-turn still works.
4. **Given** the first use of the lock icon, **then** a one-time tooltip explains both locks (r02 §2.4: "Lock icon… what does it do?").

#### F-RD-08 — Translation view [P1 · Must · MH-4]
**Story.** As PER-4, I want to read Arabic with English meaning ayah by ayah, and jump back to the page view at the same place.

**Acceptance criteria**
1. **Given** the reader, **when** the user taps "Translation view", **then** an ayah list opens at the first ayah of the current page; each row shows ayah number, Arabic (Indo-Pak script, wrapped, not page-justified), and the active translation(s): Urdu Junagarhi, English Saheeh, or both (setting).
2. **Given** the translation view, **when** the user switches back to mushaf view, **then** the page containing the top-visible ayah is shown.
3. **Given** a Junagarhi row with footnote markers, **when** tapped, **then** the Ahsan-ul-Bayan footnote expands inline.
4. **Given** the list, **then** surah boundaries show a sticky surah header (name, Makki/Madani, ayah count), basmala row where applicable, and rows are continuous across surahs.
5. **Given** row actions (tap ⋮ or long-press), **then** the same actions as F-RD-04 are available.
6. **Given** Settings, **then** Arabic and translation font sizes are separately adjustable (Arabic 18–40 sp, translation 14–28 sp; Urdu rendered in Noto Nastaliq Urdu at line-height 1.8–2.0×).
7. There is **no** translation-only mode: Arabic cannot be hidden (D-09).

### 7.2 Index & navigation

#### F-IX-01 — Surah index [P1 · Must · MH-3]
```
│ 2  Al-Baqarah  البقرة    Madani · 286 ayahs · 40 rukus · p.3 · Para 1–3   [Read] [▶] ⬇ │
```
**Acceptance criteria**
1. **Given** the Surah tab, **then** 114 rows show number, transliterated name (English UI) or Urdu name (Urdu UI), Arabic name, Makki/Madani, ayah count, ruku count, start page label, Para span, **Read** and **Play** buttons and a download-state icon (none / queued / % / downloaded / error) for the favourite Qari.
2. **Given** "Read" on Al-Baqarah, **then** the reader opens at label 3 with 2:1 briefly flashed (≤ 1 s) — the flash never changes the last-read pointer by itself.
3. **Given** "Play" on At-Tawbah (9), **then** playback starts at 9:1 with no basmala; on Al-Fatiha it starts at 1:1 (= basmala); on any other surah the basmala precedes ayah 1 (D-13).
4. **Given** the filter field, **when** the user types "yasin", "yaseen", "یس", "36", **then** Ya-Sin is listed first (aliases from F-SR-03).
5. **Given** long-press on a row, **then** options: Download surah (with size for the favourite Qari), Play with another Qari, Rukus of this surah, Bookmark surah start.

#### F-IX-02 — Para index [P1 · Must · MH-3]
```
│ 4  لَنْ تَنَالُوْا  Lan Tanaloo · 3:92 → 4:23 · p.57 · 15 rukus  ▓▓▓░░ 60%   [Read] [▶] ⬇ │
```
**Acceptance criteria**
1. **Given** the Para tab, **then** 30 rows show number, Arabic Para name with Indo-Pak diacritics (الٓمّٓ، سَیَقُوْلُ، تِلْكَ الرُّسُلُ …), Roman name (Alif Lam Meem, Sayaqool, Tilkar Rusul …), start → end ayah, start page label, rukus counted by end per the print (16, 16, 17, 14, 17, 14, 19, 17, 18, 17, 16, 16, 19, 22, 21, 17, 17, 17, 19, 16, 19, 18, 17, 19, 20, 18, 20, 20, 22, 39) (D-07), and progress (P1: "last read here" marker; P2: active khatam progress from F-BM-04).
2. **Given** "Read" on Para 4, **then** the reader opens at **label 57** with 3:92 at the top.
3. **Given** "Play" on **Para 4**, **then** the first ayah played is **3:92** (not Madani 3:93) and the last is **4:23**; the basmala of surah 4 plays before 4:1; with gapless audio the queue is surah-3 file clipped from 3:92 + surah-4 file clipped to 4:23 (D-13).
4. **Given** "Play" on the other Indo-Pak-specific Paras, **then** starts are P7 = 5:83, P11 = 9:94, P14 = 15:2, P20 = 27:60, P21 = 29:45, P23 = 36:22, and each ends one ayah before the next Para's start (e.g. Para 20 ends 29:44).
5. **Given** "Play" on Para 1, **then** it starts at 1:1 and ends at 2:141; "Read" on Para 1 opens label 2; Para 30 opens label 529 and plays 78:1 → 114:6.
6. **Given** long-press, **then** "Download Para" shows the download size for the favourite Qari (gapless: the surah files the Para spans; gapped/personal: only the Para's ayah files).

**Edge cases.** Para whose first ayah is mid-page (P29, P30 off the 18-page grid) — Read opens the page containing the start ayah and flashes it.

#### F-IX-03 — Manzil index [P1 · Must · MH-3]
**Acceptance criteria**
1. **Given** the Manzil tab, **then** 7 rows show: 1 Al-Fatiha → An-Nisa (1:1–4:176), 2 Al-Ma'idah → At-Tawbah (5:1–9:129), 3 Yunus → An-Nahl (10:1–16:128), 4 Bani Isra'il → Al-Furqan (17:1–25:77), 5 Ash-Shu'ara → Ya-Sin (26:1–36:83), 6 As-Saffat → Al-Hujurat (37:1–49:18), 7 Qaf → An-Nas (50:1–114:6), each with start page label, Read, Play, Download (D-07).
2. **Given** "Play" on Manzil 3, **then** playback runs 10:1 → 16:128 with basmala before each surah's ayah 1.
3. **Given** the Manzil tab header, **then** a link "Looking for the Manzil dua (protective verses)?" opens F-IX-08's Manzil dua — "Manzil" searches in Pakistan usually mean the dua (r02 §1.11).

#### F-IX-04 — Ruku navigation [P1 · Should · MH-3]
**Acceptance criteria**
1. **Given** a surah row → "Rukus", **then** a list of that surah's rukus shows ruku-in-surah number, ayah range, ayah count, page label, Para and ruku-in-Para; total across the Quran = 558 (D-07).
2. **Given** a ruku row or a margin ع (F-RD-04 AC 5), **then** Read opens its first ayah and Play/Repeat play exactly that range (e.g. Al-Baqarah ruku 1 = 2:1–2:7; ruku 2 = 2:8–2:20).
3. **Given** the reader overflow → "Next ruku / Previous ruku", **then** the reader jumps to the next/previous ruku start.

#### F-IX-05 — Go to [P1 · Must · MH-3]
**Acceptance criteria**
1. **Given** the Go-to dialog, **when** the user enters a page number 2–549 (Western or Urdu/Arabic-Indic digits), **then** the reader opens that label.
2. **Given** input "2:255", "2 255", «البقرة 255» or «۲:۲۵۵», **then** the reader opens the page containing 2:255 and flashes it.
3. **Given** "Para 23" or "p23", **then** the reader opens Para 23's start (label 399).
4. **Given** out-of-range input (page 1, 550, 2:287), **then** an inline error explains the valid range ("Pages are 2–549"; "Al-Baqarah has 286 ayahs"), with no navigation.

#### F-IX-06 — Sajda list [P1 · Should · MH-3]
**Acceptance criteria**
1. **Given** Index → Sajdas, **then** 15 rows: 14 numbered Hanafi sajdas (incl. 38:24) + 22:77 labelled «السجدة عند الامام الشافعی رح» and visually distinguished (D-07); each with page label, Read, Play ayah.
2. **Given** a sajda ayah's page, **then** the margin shows «السجدة» (or the Shafi'i label for 22:77).
3. **Given** audio reaches a numbered sajda ayah, **then** a non-blocking banner "Sajda tilawat" appears for 5 s (no auto-pause unless "Pause at sajda" is on; default off).

#### F-IX-08 — Quick access popular passages [P1 · Should · MH-4]
**Acceptance criteria**
1. **Given** Home, **then** chips show: Ya-Sin (36), Ar-Rahman (55), Al-Mulk (67), Al-Waqi'ah (56), Al-Kahf (18), Ayatul Kursi (2:255), Last two ayat of Al-Baqarah (2:285–286), Manzil dua; each chip offers Read and Play.
2. **Given** Read on Ayatul Kursi, **then** the reader opens at its page with 2:255 flashed; Play plays exactly 2:255 (no basmala).
3. **Given** Manzil dua, **then** a passage view lists its verses in order — Al-Fatiha; 2:1–5, 2:163, 2:255–257, 2:284–286; 3:18, 3:26–27; 7:54–56; 17:110–111; 23:115–118; 37:1–11; 55:33–40; 59:21–24; 72:1–4; Al-Kafirun; Al-Ikhlas; Al-Falaq; An-Nas (r08 §2.8) — with Arabic + active translation, and Play plays the segments in order as one queue (gapless clips).
4. The Manzil-dua verse list is part of the content-governance review (scholar sign-off before public release, D-22).

#### F-IX-09 — Home: continue reading + recents [P1 · Must · MH-1, MH-3]
```
┌ Continue reading ──────────────────────────┐
│ Para 4 · Page 61 · Āl-ʿImrān 3:116 · 2 h ago │  [Open]
└────────────────────────────────────────────┘
┌ Resume listening ─ Al-Kahf · Alafasy · 18:45 ┐ [▶]
Quick access: [Ya-Sin] [Rahman] [Mulk] [Waqi'ah] [Kahf] [Ayatul Kursi] [Manzil dua] [Baqarah 285–286]
Index: [Surah] [Para] [Manzil] [Sajda] [Go to]     Search 🔍     Bookmarks 🔖
Recent: Page 412 · Ya-Sin   |   Page 3 · Al-Baqarah   | …
```
**Acceptance criteria**
1. **Given** the app is launched, **then** Home appears (system splash only, no interstitial, no hadith screen) with the Continue-reading card first; one tap opens the reader at the last-read page (F-BM-01).
2. **Given** a listening pointer exists, **then** "Resume listening" shows surah, reciter and position; tapping ▶ resumes from that ayah (user-initiated, D-13).
3. **Given** the user has visited other positions, **then** "Recent" lists the last 10 distinct pages visited with surah name (deduplicated, newest first, clearable).
4. **Given** Settings → "Open the reader on launch" (default off), **then** launch goes straight to the last-read page.

### 7.3 Bookmarks & progress

#### F-BM-01 — Last-read pointer (separate from bookmarks) [P1 · Must · MH-1]
**Story.** As PER-1, I want my reading place kept automatically and never overwritten by browsing a bookmark or a quick-access surah.

**Acceptance criteria**
1. **Given** the reader is foreground on a page for ≥ 4 s (or the user interacts with an ayah / audio plays on it), **then** the last-read pointer becomes (mushafId, page_index, top ayah) and Home shows it.
2. **Given** the user jumps (index, search, go-to, bookmark, quick access) to a page and leaves within 4 s, **then** the pointer is unchanged.
3. **Given** a jump away from the last-read page, **then** a chip "Back to page 61" shows for the rest of the reading session (dismissable).
4. **Given** bookmarks are added/removed, **then** the pointer never changes.
5. **Given** audio playback, **then** a separate **listening pointer** (reciter, verse key, offset) updates on pause/stop and every ayah boundary; it is distinct from the reading pointer.
6. P2 adds per-khatam pointers (F-BM-04); P3 a hifz pointer — all independent (r08 §2.13).

#### F-BM-02 — Ayah & page bookmarks, collections, undo [P1 · Must · MH-1]
**Acceptance criteria**
1. **Given** the toolbar 🔖, **when** tapped, **then** the current page is bookmarked into the default collection ("Bookmarks") and the icon fills; tapping again removes it and shows **Undo** for ≥ 8 s.
2. **Given** F-RD-04 → Bookmark, **then** the ayah is bookmarked into a chosen collection; the user can create a collection with a name and one of 8 colours (each colour also has a distinct icon shape for colour-blind users).
3. **Given** Bookmarks screen, **then** rows show collection colour, surah name + verse key (or "Page 57"), Para, first words of the ayah/page, user label (wraps to 2 lines, never truncated to illegibility), created date; sort by recent or mushaf order; filter by collection.
4. **Given** a bookmark row tap, **then** the reader opens at it with a flash; delete/rename/move via swipe or long-press, each delete with Undo.
5. **Given** storage, **then** ayah bookmarks are keyed by verse key and page bookmarks by (mushafId, page_index), so they survive content updates and future layouts (D-10).

#### F-BM-06 (part 1) — Android Auto Backup [P1 · Must]
**Acceptance criteria**
1. **Given** Android backup is enabled on the device, **then** `user.db` and settings (DataStore) are included; audio, packs, content DB and caches are excluded via `dataExtractionRules` / `fullBackupContent` (D-20).
2. **Given** the app is reinstalled on a device with the same Google account and backup available, **then** bookmarks, pointers and settings are restored on first launch.
3. Total backup payload stays < 25 MB (Android's per-app limit, r08 §2.17).

### 7.4 Audio

#### F-AU-01 — Reciter catalogue + favourite Qari [P1 · Must · MH-2]
**Acceptance criteria**
1. **Given** the reciter picker, **then** each entry shows name (English/Urdu/Arabic), style (Murattal / Mujawwad / Muallim), audio type (gapless / verse-by-verse), a "Highlight" badge when timings exist, full-Quran download size, and source credit — **no photos** (D-12).
2. **Given** a public build, **then** the list contains only public-eligible recitations: Alafasy (default), Saad Al-Ghamdi, Abdul Basit Mujawwad, Sudais, Abu Bakr Ash-Shatri, Hani Ar-Rifai, Muhammad Ayyub, Khalid Al-Muhanna with highlight, plus Husary, Minshawi, Shuraim, Yasser Ad-Dosari with highlight off in faulty surahs (D-12) — final list subject to G2; **given** a closed-test (BETA) build, **then** PENDING recitations (gapped per-ayah sets) may also be listed; **given** a personal build, **then** PENDING and PERSONAL_ONLY items (gapped files, Qari Sadaqat Ali, Shamshad Ali Khan Urdu, Husary Muallim) are also listed with a "Personal build" tag (D-29).
3. **Given** onboarding or the picker, **when** the user stars a reciter, **then** it becomes the favourite used by every Play button; other reciters remain one tap away ("Play with…"). Multiple reciters can be starred; one is "default".
4. **Given** no network, **then** the bundled catalogue copy is used; **given** network, **then** the signed `catalogue.json` refreshes at most daily; an invalid signature is ignored and the previous copy kept (D-10).
5. **Given** "Preview" on a reciter with network, **then** a ≤ 15 s sample of Al-Fatiha streams; offline, the button explains it needs internet.

#### F-AU-02 — Play scopes [P1 · Must · MH-2]
**Story.** As the owner and PER-3, I want to play my Qari for a Surah, a Para, a Manzil, a ruku, a page, a verse range, one ayah, or "from here".

**Acceptance criteria** (scope → exact queue; basmala rules from F-IX-01 AC 3 apply whenever ayah 1 of surah 2–8, 10–114 is included)

| # | Given / When | Then the queue is |
|---|---|---|
| 1 | Play **Surah** 18 | 18:1 → 18:110 (+ basmala); stops at end unless "Continue to next surah" (default off) |
| 2 | Play **Para** 4 | 3:92 → 4:23 (Indo-Pak table, never Madani) |
| 3 | Play **Manzil** 7 | 50:1 → 114:6 |
| 4 | Play **Ruku** (Al-Baqarah r1) | 2:1 → 2:7 (+ basmala) |
| 5 | Play **Page** 57 | first ayah that starts on the page (3:92) → last ayah that ends on the page; if the page starts mid-ayah, that ayah is played from its start |
| 6 | Play **range** 2:255–2:257 | exactly those 3 ayahs, no basmala |
| 7 | Play **ayah** 2:255 | exactly 2:255 |
| 8 | **Play from here** on 36:12 | 36:12 → onward continuously through later surahs until the user stops or a sleep timer ends |

- **Given** any Play action, **then** playback starts within ≤ 1 s from a downloaded file and ≤ 3 s when streaming on a 4G connection (NFR-05), the mini player appears, and the reader (if open) follows (F-RD-05).
- **Given** a queue, **then** the full player shows the scope title ("Para 4 · Lan Tanaloo · Alafasy"), current ayah, and previous/next **ayah** buttons; long-press next/prev = next/previous surah.
- **Given** gapless audio, **then** ayah-level start/stop uses timing data (clipping); **given** a surah whose timing is kill-switched, **then** scopes that need mid-surah boundaries (range, ayah, ruku, Para start mid-surah, play-from-here) fall back to the nearest available reciter with timing *only after asking* ("Timing isn't available for this surah with Sudais — play with Alafasy?"), or play the whole surah.
- **Given** all scopes, **then** playback is only ever started by a user action (tap in app, notification action, headset, widget) — never automatically (D-13, Android 17).

#### F-AU-03 — Gapless + timings [P1 · Must · MH-2]
**Acceptance criteria**
1. **Given** a gapless recitation, **then** audio and timing always come from the same host and file set (mp3quran); timing packs are ETag/size-checked against the audio file before use (D-12).
2. **Given** a Para spanning surahs, **then** consecutive surah clips play with no added silence beyond the recording's own.
3. **Given** a seek inside a surah file, **then** seeking is accurate to ≤ 300 ms (constant-bitrate seeking, D-13); "previous ayah" restarts the current ayah if > 2 s into it.
4. **Given** a timing pack fails validation or is kill-switched for a surah, **then** F-RD-05 AC 4 applies.
5. Never use mp3quran reads 74 or 53 for timing; Abdul Basit Mujawwad (read 51) uses the surah-9 relabel fix (D-12; [Audio engine](06-audio-engine.md)).

#### F-AU-04 — Verse-by-verse (gapped) mode [P1 personal · Should · MH-2]
**Acceptance criteria**
1. **Given** a personal build and a gapped reciter, **then** each ayah is its own item (`SSSAAA.mp3`), the basmala is a separate file, and all scopes in F-AU-02 work with per-ayah precision without timing data.
2. **Given** a PUBLIC (production) build, **then** gapped recitations are hidden until their `licenceStatus` becomes PERMITTED (G2); a closed-test (BETA) build may list PENDING gapped sets (D-29).
3. **Given** Para 30 gapped, **then** the queue has ≤ 601 items (564 ayahs + 37 basmalas) and starts within ≤ 1 s (r06 §1.8).

#### F-AU-05 — Repeat & pauses (hifz basics) [P1 · Must · MH-2]
**Acceptance criteria**
1. **Given** the repeat panel, **then** options: repeat each ayah ×1–10 or ∞; repeat the whole range/scope ×1–10 or ∞; pause after each ayah: off / fixed 1–10 s / proportional (0.5×, 1×, 1.5×, 2× the ayah's duration, for recite-back).
2. **Given** "2:255–257, each ayah ×3, range ×2, pause 1×", **then** order is 255,255,255,256,256,256,257,257,257 then the same again, with a silence equal to each ayah's length after each play; the highlight follows each repetition.
3. **Given** ∞ on range, **then** it loops until stopped or the sleep timer fires; the notification shows "Repeat 3/∞".
4. **Given** the user changes repeat settings during playback, **then** they apply from the next ayah without restarting.
5. Repeat works for gapless (via clipping) and gapped audio.

#### F-AU-06 — Speed & sleep timer [P1 · Should · MH-2]
**Acceptance criteria**
1. **Given** the speed control, **then** speeds 0.5, 0.75, 1.0, 1.25, 1.5, 1.75, 2.0× are available, pitch preserved; the choice persists per reciter.
2. **Given** a sleep timer of 5, 10, 15, 30, 45, 60, 90 min or custom, **then** audio fades out over the last 10 s and stops; **given** "end of ayah / surah / Para", **then** it stops exactly at that boundary (fade over the final 3 s for surah/Para).
3. **Given** a sleep timer is running, **then** the full player and notification show the remaining time; shaking or tapping "+5 min" extends it.

#### F-AU-07 — Background playback, player surfaces, controls [P1 · Must · MH-2]
**Acceptance criteria**
1. **Given** audio playing, **when** the user leaves the app or locks the screen, **then** playback continues (Media3 `MediaLibraryService`, `mediaPlayback` FGS) with a media notification and lock-screen controls (play/pause, previous/next ayah, stop).
2. **Given** a wired/Bluetooth headset, **then** play/pause/next/previous buttons work; **when** headphones disconnect ("becoming noisy"), **then** playback pauses.
3. **Given** an incoming call or another app taking audio focus, **then** playback **pauses** (not ducks; speech content) and resumes after a transient loss ends; "Lower volume instead of pausing" is an opt-in (D-13).
4. **Given** the in-app UI, **then** a mini player (title, reciter, play/pause, next, close) is visible above the bottom bar on Home, indexes and reader; swiping it up opens the full player (scope, reciter switch, seek within ayah/surah, repeat, speed, sleep, download this scope, queue list).
5. **Given** a 2-hour Para/Manzil session, **then** playback never stops early (reference bug: "stops 5–6 s before the end") and never restarts the surah on double-tap (NFR-10).
6. **Given** the app process is killed by the OS while paused, **then** "Resume listening" on Home restores the exact ayah.

#### F-AU-08 — Downloads [P1 · Must · MH-2]
**Acceptance criteria**
1. **Given** a surah/Para/Manzil row or the full player, **when** the user taps Download, **then** a sheet shows the reciter, scope, size (from catalogue) and free space, with "Download" and, on mobile data, "Use mobile data this time"; Wi-Fi-only is the default (D-14).
2. **Given** "Download full Quran" for a reciter, **then** the size is shown (0.4–2.5 GB depending on bitrate, r06 §1.11) and the download is refused with an explanation if free space < size + 500 MB.
3. **Given** a running download, **then** it runs as a user-initiated data transfer job on API 34+ and a WorkManager long-running foreground worker on ≤ 33, shows progress in a notification and on the row, survives app exit, network loss (resumes via HTTP Range from the `.part` file) and reboot (D-14).
4. **Given** a completed file, **then** its MD5/SHA-256 is verified before it is marked downloaded; a mismatch is retried once then reported as error with "Retry".
5. **Given** downloaded audio, **then** playback uses it with no network and **never** asks for anything (no ad, no sign-in) — the reference's most resented pattern (r02 §2.4).
6. **Given** a removable SD card is chosen and later removed, **then** affected files show as "not available" with "Re-download" (no crash).
7. POST_NOTIFICATIONS (Android 13+) is requested in context at the first download; if denied, downloads still run and progress shows in-app.

#### F-AU-09 — Streaming cache [P1 · Must · MH-2]
**Acceptance criteria**
1. **Given** streaming playback, **then** audio is cached in a 256 MB LRU cache (D-14); replaying a recently streamed surah works offline while cached.
2. **Given** no network and a non-downloaded, non-cached item, **then** a clear message offers "Download when online" (queues it) — no generic error.
3. **Given** "Stream on mobile data" = **Ask** (default), **when** the first stream of a session starts on a metered network, **then** a dialog shows the approximate data use per hour and lets the user stream or not; **Allow** streams without asking; **Never** blocks streaming on metered networks with an explanation and a "Download on Wi-Fi" option.
4. Storage manager (F-PL-06) shows cache size and "Clear cache".

#### F-AU-10 — Arabic + Urdu translation-audio interleave [P1 personal · Could · MH-2]
**Acceptance criteria**
1. **Given** a personal build and the Shamshad Ali Khan Urdu recitation (gapped), **when** "Arabic + Urdu" is on, **then** each ayah plays in Arabic (chosen gapped reciter) followed by its Urdu translation audio; highlight stays on the ayah throughout.
2. **Given** a public build, **then** the option is hidden unless rights are granted (D-12; P3).

### 7.5 Translation

#### F-TR-01 — Bundled Urdu + English [P1 · Must · MH-4]
**Acceptance criteria**
1. **Given** a fresh install offline, **then** Urdu (Muhammad Junagarhi, with Ahsan-ul-Bayan footnotes) and English (Saheeh International, credited "Noor International Center") are available for all 6,236 ayahs (D-09).
2. **Given** a translation is displayed (sheet, translation view, copy, share), **then** its translator credit is shown/attached as required by its source terms (QuranEnc).
3. **Given** Settings → Translation, **then** the user chooses which bundled translation(s) show (Urdu, English, both; default by UI language: Urdu UI → Urdu, English UI → English).
4. Never QF/quran.com API content in bundled data (D-09).

### 7.6 Search

#### F-SR-01..03 — Arabic, translation and alias search [P1 · Must/Should · MH-3, MH-4]
**Stories.** As PER-1, I want to find an ayah by typing a few Arabic words without diacritics. As PER-4, I want to search the English meaning. As anyone, I want "Yaseen" or "Ayatul Kursi" to just work.

**Acceptance criteria**
1. **Given** the search screen, **when** the query is "2:255", "۲:۲۵۵" or a page number, **then** the first result is a direct "Go to" row.
2. **Given** an alias ("yaseen", "yasin", «یٰسین», "ayatul kursi", "ayat al kursi", «آیت الکرسی», "manzil dua", "surah rehman", "last two ayat baqarah"), **then** the matching surah / ayah / passage appears first (F-SR-03).
3. **Given** the Arabic query «الرحمن» (no diacritics), **then** results equal those for «الرَّحْمٰنِ» and include 1:1; «الصلاة» matches ayahs written «الصلوة» (e.g. 2:3); «يا أيها» matches «يٰٓاَيُّهَا» — same normalizer for index and query, three Arabic columns (imla'i, Uthmani, Indo-Pak) (D-11).
4. **Given** a query of ≥ 3 letters that is part of a word («رحم»), **then** substring (trigram) matches are returned after whole-word matches.
5. **Given** an Urdu query in Urdu script, **then** Junagarhi translation matches are returned (ے/ھ kept distinct per D-11); **given** an English query ("mercy", "Musa" / "Mūsā"), **then** ASCII-folded Saheeh matches are returned (F-SR-02).
6. **Given** results, **then** each row shows verse key, surah name, page label, Para, the Arabic ayah and/or translation snippet with matched terms emphasised; grouped as Go-to / Surahs & passages / Arabic / Urdu / English with counts; tapping opens the reader at the ayah with a flash.
7. **Given** typing, **then** results update as-you-type (250 ms debounce) and the p90 query time is ≤ 150 ms on the low-end reference device (NFR-04).
8. **Given** search history, **then** it is stored only on device and clearable; no query ever leaves the device (PRV-06).

**Edge cases.** One-letter queries show only aliases/surah names; diacritics-only or punctuation-only queries show a hint; zero results suggest alternative spellings and the alias list.

### 7.7 Platform

#### F-PL-01 — Onboarding [P1 · Must · MH-1, MH-2]
**Acceptance criteria**
1. **Given** first launch, **then** at most 3 screens in P1 (4 in P2), each skippable: (1) UI language — English / اردو (default from system locale); (2) Mushaf — preview of a 16-line Indo-Pak page with one-line description and "About this mushaf"; (3) Favourite Qari — public shortlist with Alafasy pre-selected and optional previews (network); (P2: 4) Prayer setup — city search or "Use my approximate location", Asr Hanafi (later) / Standard (earlier); the adhan-notification choice joins this step when F-CP-02 ships (v1.1).
2. **Given** onboarding completes or is skipped, **then** Home appears; total time from first launch to reading page 2 ≤ 60 s for a user who accepts defaults; no network needed.
3. No permission is requested during onboarding except P2's optional location and notifications, each with a one-sentence reason.

#### F-PL-02 — Settings [P1 · Must]
Required groups and defaults:

| Group | Settings (default) |
|---|---|
| Language | UI language (system / English / اردو) |
| Reader | Theme (System); keep screen on (on; releases after 15 min of no interaction — 5 / 15 / 30 min / never); keep system bars visible (off); vibrate on page turn (off); open reader on launch (off); page-turn animation (slide; curl from v1.1); reset zoom |
| Translation | Active translations (by UI language); Arabic size; translation size; show footnotes (on); include translation when copying (off) |
| Audio | Favourite / default Qari; highlight while playing (on); turn pages automatically (on); continue to next surah (off); repeat & pause defaults; speed (1.0×); lower volume instead of pausing (off); pause at sajda (off) |
| Downloads & storage | Wi-Fi only (on); stream on mobile data (Ask / Allow / Never; default Ask — once per session); storage location; → F-PL-06 |
| Backup | Auto Backup status (read-only explanation); (v1.1) export / import |
| P2 groups | v1.0: Prayer times, Hijri offset, Ramadan; v1.1: adhan, reminders |
| About | About this mushaf; (P2) Sources & Credits; privacy policy; app & content versions; contact |

**AC:** every setting persists across restarts (typed DataStore), is reachable with TalkBack, and has Urdu and English labels.

#### F-PL-03 — Urdu + English UI [P1 · Must · MH-4]
**Acceptance criteria**
1. **Given** the language switch, **then** the app switches between English and Urdu without reinstall (per-app language, `AppCompatDelegate.setApplicationLocales`, `generateLocaleConfig`; language splits disabled) and the choice also appears in Android 13+ system per-app language settings (D-21).
2. **Given** Urdu, **then** layouts mirror (RTL), Urdu text uses Noto Nastaliq Urdu with line-height 1.8–2.0×, and no string is truncated at 200% font scale on a 360 dp phone for core screens (Home, indexes, reader chrome, player, settings).
3. **Given** either language, **then** the mushaf is always RTL and unaffected by UI direction.
4. 100% of user-visible strings exist in both languages at every release (CI check).

#### F-PL-06 — Content-pack & storage manager [P1 · Must · MH-2]
**Acceptance criteria**
1. **Given** Storage screen, **then** it lists: bundled content (mushaf layout + version, translations + versions, sizes), downloaded audio per reciter (total size, expandable to surahs/Paras), stream cache size, free device space, storage location (internal / SD).
2. **Given** a reciter row, **then** the user can delete all, or by surah/Para, with confirmation and correct space reclaimed.
3. **Given** "Clear cache", **then** only the stream cache is cleared.
4. v1.1 (pack half): downloadable packs (translations) listed with size, licence and attribution, install/remove/update; a pack disabled by the remote kill-switch is hidden with a short notice (D-10).

#### F-PL-07 — Accessibility baseline [P1 · Must]
**Acceptance criteria**
1. **Given** TalkBack, **then** each mushaf ayah is a focusable node announced as "Surah Al-Baqarah, ayah 255" followed by the active translation; the Arabic Quran is **never** sent to TTS (r08 §2.16).
2. **Given** an ayah node, **then** custom actions "Play recitation", "Bookmark", "Translation" are available.
3. **Given** all controls, **then** they have labels, ≥ 48 dp targets, and logical focus order; chrome never auto-hides while TalkBack focus is inside it.
4. **Given** 200% system font scale (non-linear on Android 14+), **then** core screens remain usable without clipping essential text (mushaf excluded; it uses zoom).
5. **Given** "Remove animations" in system settings, **then** page-curl, auto-scroll easing and highlight transitions are reduced to instant changes.
6. Colour is never the only signal (bookmark collections, download states, highlight vs selection also differ by icon/shape).

---

## 8. Detailed requirements — P2 (v1.0 Ramadan launch, v1.0-stretch and v1.1)

Each heading carries the feature's **Release** (§0, §6). Only the v1.0 items (and v1.0-stretch items that are restored) gate the Ramadan launch; v1.1 items are specified here so they can be pulled in under [Roadmap](09-roadmap-backlog.md) §5.2 or ship on 2027-03-22. The store listing for v1.0 claims only v1.0 features ([Release](11-release-compliance-business.md)).

### 8.1 Reader & index extras

#### F-RD-09 — Vertical auto-scroll [P2 · v1.1 · Should · MH-1]
1. **Given** reader overflow → Auto-scroll, **then** pages stack vertically in mushaf order and scroll continuously; speed 1–10 (default 3) adjustable with +/− during scrolling; the screen stays on.
2. **Given** the user touches the screen, **then** scrolling pauses; it resumes on tapping ▶ (not automatically).
3. **Given** auto-scroll reaches label 549, **then** it stops; exiting returns to paged mode at the top-visible page; the last-read pointer follows the dwell rule.

#### F-RD-12 — Page-curl animation [P2 · v1.1 · Could · MH-1]
1. **Given** Settings → Page-turn animation = Curl, **then** page turns use a curl effect at ≥ 55 fps on the High reference device (Pixel 6a class, §11); on the Low device or with "Remove animations", the app falls back to slide.

#### F-RD-13 — Tafsir-summary sheet [P2 · v1.0-stretch · Should · MH-4]
1. **Given** F-RD-04 → Tafsir summary on an ayah with Ahsan-ul-Bayan notes, **then** a sheet shows the Junagarhi translation with numbered footnotes formatted as readable paragraphs, credited per source terms; ayahs without notes show "No note for this ayah" and the translation.
2. Next/previous ayah navigation inside the sheet; text size follows translation size.

#### F-IX-07 — Para quarter markers & navigation [P2 · v1.0-stretch (list) · Should · MH-3]
1. **Given** a page containing a quarter position, **then** the outer margin shows «الربع» / «النصف» / «الثلٰثة» per our curated 90-position table (never derived from Madani rub) — e.g. Para 1: 2:46 / 2:78 / 2:109; Para 4 ثلٰثة at 3:198; Para 7 ربع at 5:113; Para 7 ثلٰثة at 6:73 after word 10 (mid-ayah) (D-07).
2. **Given** the Para index row → expand, **then** the 3 quarters are listed with page labels; Read opens each, Play plays from the quarter to the next quarter/Para end.

### 8.2 Bookmarks & progress

#### F-BM-03 — Notes [P2 · v1.1 · Should · MH-4]
1. **Given** F-RD-04 → Note, **then** the user writes plain text (≤ 5,000 chars) attached to the verse key; an ayah with a note shows a small note indicator in translation view and in the action sheet.
2. Notes are listed in Bookmarks → Notes, searchable locally, included in Auto Backup and JSON export.

#### F-BM-04 — Khatam planner & tracker [P2 · v1.0 · Must · MH-4]
**Stories.** As PER-1, I want a 30-day khatam that tells me today's portion and keeps its own place. As PER-4, I want a Ramadan Taraweeh plan. As anyone, I want several khatams at once without them disturbing each other or my last-read.

**Acceptance criteria**
1. **Given** "New khatam", **then** the user picks a preset — 30 days (≈ 1 Para/day; 548 pages ÷ 30 ≈ 18.3 pages/day), Ramadan 29 / 27 nights (Taraweeh), 7 days (Manzil-wise), 60 days, custom (start, target date, rest days) — a unit (pages, Paras, rukus), an optional dedication (e.g. esal-e-sawab), and optional "stop at ruku ع" snapping (r08 §2.13).
2. **Given** an active khatam, **then** Home shows "Today: Page 57 → 74 (Para 4)" with Open; each khatam has its **own pointer**, independent of last-read and other khatams (fixes r01 pain #10).
3. **Given** the user reads the portion (dwell-based suggestion) or taps "Mark today's portion read", **then** progress updates; the Para checklist (30 boxes) can also be ticked directly.
4. **Given** missed days, **then** the remaining portion is redistributed over remaining active days (`goal = ceil(remaining / days)`, snapped to ayah or ruku end); copy is encouraging, never shaming.
5. **Given** completion, **then** a completion screen offers the dua-e-khatam (sourced, D-22) and, from v1.1, the optional F-PL-10 rating prompt; the khatam is archived with its dates.
6. **Given** multiple khatams (≥ 5 concurrent supported), **then** each shows separately in the Para index progress selector.

#### F-BM-05 — Private streaks & stats [P2 · v1.1 · Could · MH-4]
1. **Given** reading/listening days, **then** a private streak counts days with ≥ 1 page read or ≥ 1 ayah listened; 2 freeze days per month are applied automatically; no leaderboard, no sharing of stats by default (PP-8).
2. A weekly summary (pages read, minutes listened, Paras completed) is shown in-app only — never as a push unless the user opts in.

#### F-BM-06 (part 2) — JSON export / import [P2 · v1.1 · Should]
1. **Given** Settings → Backup → Export, **then** a versioned JSON (`<brand>-backup-YYYYMMDD.json`) containing bookmarks, collections, notes, pointers, khatams, stats and settings is written via the Storage Access Framework (works without Google services, foss flavor).
2. **Given** Import, **then** the user chooses Merge (dedupe by stable key + newest timestamp) or Replace; the file's schema version is validated; a summary ("42 bookmarks, 3 khatams imported") is shown.

### 8.3 Translation packs

#### F-TR-02 — Translation packs [P2 · v1.1 · Should · MH-4]
1. **Given** Storage → Translations, **then** downloadable packs from the signed `index.json` are listed with language, translator, size, licence and attribution: Urdu Fateh Muhammad Jalandhari (non-commercial-only flag), Kanz-ul-Iman, English Pickthall, Hilali-Khan, Hindi/Bangla/Indonesian/Turkish (QuranEnc) (D-09).
2. **Given** download, **then** sha256 and Ed25519-signed index are verified before activation; failed verification never activates the pack (D-10).
3. **Given** a pack flagged non-commercial and a monetized build, **then** CI excludes it (D-17).
4. Pack search indexes are built/attached so F-SR-02 covers installed packs.

#### F-TR-03 — Translator choice per language [P2 · v1.1 · Should · MH-4]
1. **Given** Settings → Translation, **then** the user selects up to 2 active translations total (e.g. Urdu Kanz-ul-Iman + English Saheeh), ordered; the default per language is the bundled one.
2. Translator names show maslak-neutral descriptions; Shia options (when added) are labelled and opt-in; no Ahmadi translations (D-22).

### 8.4 Companion

#### F-CP-01 — Prayer times [P2 · v1.0 · Must · MH-4]
**Acceptance criteria**
1. **Given** the city Karachi (24.8607, 67.0011), date 2026-10-19, KARACHI method + HANAFI Asr, **then** times are within ±2 min of: Fajr 05:16, Sunrise 06:32, Dhuhr 12:17, Asr 16:25, Maghrib 18:02, Isha 19:18 (golden vectors for Lahore, Islamabad, Peshawar, Quetta, Manchester in r08 §2.2.3 are CI tests).
2. **Given** location from a one-shot `ACCESS_COARSE_LOCATION` fix or from the offline city picker (GeoNames cities15000, Urdu names, search works offline), **then** region defaults apply: PK/IN/BD/AF → Karachi + Hanafi; GB/IE/EU → Moonsighting Committee + recommended high-latitude rule; SA → Umm al-Qura; AE/QA/KW/TR/SG → respective presets; US/CA → Moonsighting Committee; else MWL (r08 §2.2.2). All overridable.
3. **Given** settings, **then** per-prayer offsets −30…+30 min, Asr Hanafi/Standard, high-latitude rule, and 24 h / 12 h display are available.
4. **Given** the Prayer screen, **then** it shows today's six times, the next prayer with a countdown, Hijri + Gregorian date, location and method ("Karachi University of Islamic Sciences · Hanafi"), and a monthly table (shareable as image).
5. **Given** the city's time zone, **then** times are computed in the city's zone (correct across DST for diaspora cities).
6. Works offline; location is never sent anywhere (PRV-05).

#### F-CP-02 — Adhan alarms + health check [P2 · v1.1 · Must · MH-4]
**Acceptance criteria**
1. **Given** each prayer, **then** the user chooses Off / Notification (silent) / Short adhan (takbeer via channel sound) / Full adhan, plus an optional pre-prayer reminder (5/10/15/30 min).
2. **Given** audible adhan modes, **then** alarms use `setAlarmClock()` with `SCHEDULE_EXACT_ALARM` granted after an explainer screen; **given** the permission is denied or revoked, **then** inexact alarms are used and the Prayer screen shows a persistent warning "Adhan may be late — allow exact alarms" (D-16).
3. **Given** a full adhan, **then** it plays from a `mediaPlayback` foreground service with `USAGE_ALARM`, has Stop in the notification and lock screen, and **does not play audibly when the phone is on silent/vibrate or DND** (notification only) unless the user enabled the opt-in "Play adhan even when silent" (default off, with a warning). The app never changes ringer or DND state and never declares `ACCESS_NOTIFICATION_POLICY` (D-16).
4. **Given** boot, time change, time-zone change, app update or exact-alarm permission change, **then** all alarms are rescheduled; a daily WorkManager job verifies the next 24 h of alarms (D-16).
5. **Given** Settings → Adhan health check, **then** it shows: notification permission, exact-alarm permission, battery-optimisation status (read-only), device-maker-specific guidance (DontKillMyApp content, CC BY, attributed), "Test adhan in 1 minute", and a lateness log of the last 30 alarms (scheduled vs fired).
6. **Given** the release gate of the release that ships F-CP-02 (v1.1 by default), **then** adhan fires within 60 s of schedule in ≥ 95% of trials on each device in the test matrix, including an Infinix or Tecno device, with the health-check guidance applied (r08 §1.2; [Quality](10-quality-testing-ci.md)).
7. Adhan audio: commissioned Pakistani muezzin recordings (standard, Fajr, short takbeer, post-adhan dua) + 1–2 CC0 fallbacks, bundled for offline use (D-16).

#### F-CP-03 — Qibla [P2 · v1.1 · Should · MH-4]
1. **Given** a location, **then** the Qibla bearing equals the great-circle bearing to the Kaaba (Karachi 267.7°, Lahore 260.3°, Islamabad 255.9° true) and the compass corrects magnetic to true north with `GeomagneticField` declination (D-15).
2. **Given** sensors, **then** `TYPE_ROTATION_VECTOR` is used, with fallbacks to geomagnetic rotation vector or accelerometer + magnetometer when no gyroscope (common on low-end phones).
3. **Given** low sensor accuracy or magnetic interference, **then** a calibration prompt (figure-8 animation, respects reduced motion) and an interference warning are shown; the screen shows the accuracy state.
4. Works offline; no map, no Play Services.

#### F-CP-04 — Hijri calendar [P2 · v1.0 · Must · MH-4]
1. **Given** no feed and no offset, **then** the Hijri date = `HijrahDate` (Umm al-Qura); **given** the user offset (−2…+2 days), **then** it is applied everywhere (Home, Prayer, widgets, Ramadan).
2. **Given** the signed "PK month-start" feed declares 1 Jumada I 1448 = 13 Oct 2026, **when** the region is Pakistan (or the user chose "Pakistan (Ruet-e-Hilal)"), **then** 13 Oct 2026 shows 1 Jumada I 1448 (UQ shows it on 12 Oct) (r08 §1.6); invalid signatures are ignored.
3. **Given** the current time is after Maghrib, **then** the displayed Hijri date is the next day's, labelled "(from Maghrib)".
4. **Given** the monthly calendar, **then** it shows Gregorian + Hijri days and optional Islamic events (each a toggle, neutral naming, defaults per OQ-09).

#### F-CP-05 — Tasbeeh [P2 · v1.0-stretch · Should · MH-4]
1. **Given** the counter, **then** a full-screen tap area increments with a haptic tick (stronger at 33/99/target), volume keys count while the screen is on, and a reset requires a long-press or confirmation.
2. Presets: Tasbih-e-Fatimah (33/33/34 sequence), Istighfar 100, Durood, Kalima, custom with targets; daily totals and private history.

#### F-CP-06 — Azkar & duas [P2 · v1.0-stretch (Manzil dua) / v1.1 · Should · MH-4]
1. **Given** Morning / Evening azkar, **then** each item shows Arabic, Urdu and English (toggles), count with tap counter and auto-advance, and a source reference chip (e.g. "Bukhari 6306"); only items with a stated source ship (D-22).
2. **Given** the Manzil dua, **then** it reuses F-IX-08 (Quran text and audio).
3. **Given** core duas (~150), **then** they are grouped by occasion and searchable; Urdu translations are commissioned and scholar-reviewed (or HadeethEnc with permission); items without reviewed Urdu ship Arabic + English only (r08 §2.8).
4. Azkar content ships as an `azkar` pack (pack id `azkar.core`, bundled, updatable through the signed pack index; [Architecture](04-architecture.md) pack types), so no Quran-core asset changes when duas are revised. Content sources: osamayy/azkar-db Arabic (public-domain dedication, attributed), fitrahive/dua-dhikr English (MIT) or own; never bundle hisnmuslim.com ([Content](03-content-data-licensing.md)).

#### F-CP-07 — Ramadan [P2 · v1.0 core / v1.1 extras · Must · MH-4]
1. *[v1.0]* **Given** Hijri month 9 (with offset/feed), **then** Home shows a Ramadan card: today's Sehri end and Iftar time with a countdown; outside Ramadan the card is hidden (the timetable is still reachable from Prayer).
2. *[v1.0; share image v1.1]* **Given** the timetable, **then** 30 days list Sehri end (= Fajr − safety margin 0–10 min, default 0) and Iftar (= Maghrib + ihtiyat 0–3 min, default 0) for the chosen location; from v1.1 it is shareable as a WhatsApp-friendly image (geometric background, attribution line).
3. *[v1.1 — needs the F-CP-02 alarm scheduler; v1.0-stretch only if that scheduler is pulled in via the 2026-11-30 freelancer lever]* **Given** a Sehri alarm (time before Sehri end, default 45 min), **then** it uses alarm semantics (`setAlarmClock`, `USAGE_ALARM`), no full-screen intent (D-16).
4. *[v1.1]* **Given** duas, **then** only sourced duas (Iftar dua with reference) are shown; the popular Sehri "niyyah" formula ships only with scholar sign-off (r08 §2.9).
5. *[v1.0; reminders v1.1]* **Given** "Taraweeh plan", **then** a 27-night khatam (F-BM-04 preset) is created; optional odd-night reminders for the last ten nights arrive with F-CP-09 (v1.1).
6. The v1.0 ACs (1, 2 without sharing, 5 without reminders) are merged into the closed-test build by **2027-01-18** at the latest, giving ≥ 7 days of tester exposure before code-complete (D-25; [Roadmap](09-roadmap-backlog.md) S6).

#### F-CP-08 — Glance widgets [P2 · v1.1 · Should · MH-4]
1. **Continue reading (2×2):** surah, page label, Para, today's khatam goal; tap opens the reader at that page.
2. **Next prayer (2×1):** name, time, Hijri date; updates at each prayer boundary via the alarm chain and the daily job (no seconds countdown).
3. **Ayah of the day (4×2):** Arabic + translation + reference from a curated, scholar-reviewed 365-ayah list (never random); tap opens the ayah.
4. Widgets render correctly in light/dark, in Urdu/English, respect Android 17's RemoteViews bitmap memory cap, and never trigger playback without a tap.

#### F-CP-09 — Smart reminders [P2 · v1.1 · Should · MH-4]
1. **Given** defaults, **then**: Al-Kahf on Friday (Fajr + 1 h) **on**; Al-Mulk nightly (Isha + 30 min) **on**; daily ayah at a user time **on**; Ayatul Kursi after fard prayers **opt-in at onboarding**; morning/evening azkar opt-in (r08 §2.15).
2. **Given** any day, **then** non-prayer notifications are capped at **3/day** and suppressed during quiet hours (default 22:30–05:00 except opted-in Sehri/Fajr).
3. **Given** a reminder, **then** tapping it opens the exact page/ayah; its "Play" action starts the recitation (a user action, Android 17-compliant); no reminder ever auto-plays audio.
4. No promotional or "come back" notifications, ever (PP-1, PP-8).

#### F-CP-10 — Share ayah as image / text [P2 · v1.1 · Should · MH-4]
1. **Given** F-RD-04 → Share as image, **then** a 1080×1350 (feed) or 1080×1920 (status) PNG is rendered off-screen with the Arabic in the Indo-Pak font, the chosen translation with translator attribution, the reference ("Al-Baqarah 2:255 · Para 3") and a small app mark, on a plain or geometric background (no animate imagery); shared via `FileProvider` + `ACTION_SEND` (WhatsApp first in the chooser if installed).
2. "Share text" follows F-RD-04 AC 3 and appends the store link (public builds).

#### F-CP-11 — Group khatam (WhatsApp sheet) [P2 · v1.1 · Could · MH-4]
1. **Given** "Group khatam", **then** the user sets a dedication, deadline and unit (30 Paras or 7 Manzils for Shabeena) and the app generates WhatsApp-ready text: "1. Alif Lam Meem — ____ / 2. Sayaqool — ____ …".
2. **Given** a pasted updated list, **then** the app parses claims/completions back (tolerant of extra spaces, Urdu digits, names); when all parts are done, it opens the dua-e-khatam screen.
3. No backend, no account; personal names stay on device (PRV-06). Realtime Firestore version is P3 (play flavor only, D-24).

### 8.5 Platform

#### F-PL-04 — Sources & Credits [P2 · v1.0 · Must · MH-4]
1. **Given** About → Sources & Credits, **then** every shipped content item is listed from pack/content metadata with name, source, licence and the exact attribution text required — Tanzil text (CC BY 3.0), QuranEnc translations, DigitalKhatt IndoPak font (SIL OFL 1.1), mushaf layout source and permission, timing/audio hosts (mp3quran statement), GeoNames (CC BY 4.0), DontKillMyApp (CC BY), Noto Nastaliq Urdu (OFL), azkar sources — plus open-source software licences (AboutLibraries) (D-02, D-10).
2. **Given** About this mushaf, **then** it shows the edition description (16-line Indo-Pak, Taj page layout), data provenance, content version, the proofreading certificate holder and date once G5 clears, and how to report an error.
3. CI fails if any shipped item lacks attribution metadata ([Quality](10-quality-testing-ci.md)).

#### F-PL-05 — Report text error [P2 · v1.0 · Must]
1. **Given** F-RD-04 → Report text error, or reader overflow → Report a problem on this page, **then** a form captures automatically: layout id, page label, line number(s) (from selection), verse key(s), content version, app version; the user adds a description and optionally attaches a locally rendered image of the page.
2. **Given** Send, **then** an email intent (`ACTION_SENDTO`) opens pre-filled to the content-errors address — the user sees and can edit everything before sending; nothing is uploaded silently (PRV-07). If no email app exists, the report is copied to the clipboard with the address shown.
3. **Given** a report, **then** it is triaged within **24 h**; a confirmed error gets a signed notice or kill-switch flag on the affected page or ayah (F-PL-11) within **48 h**, and a corrected build is submitted to the store within **72 h** of confirmation; the reporter is acknowledged within **72 h** (D-22, D-30). Text fixes always ship as an app update — the content DB and layout are bundled, and the remote channel can only annotate or disable, never change Quran text or layout.
4. The process and SLA are stated in About this mushaf.

#### F-PL-10 — Gentle rating prompt (new) [P2 · v1.1 · Could]
1. **Given** a play-flavor build, **when** the user completes a khatam or finishes a full surah/Para listening session, and has used the app on ≥ 7 distinct days, **then** the Play In-App Review flow may be requested — at most once per 120 days, never on launch, never during reading/playback, never with pre-filtering ("Do you like us?") (r02 §2.6).
2. foss flavor: no prompt.

#### F-PL-11 — Signed remote notices (new) [P2 · v1.0 · Must]
**Story.** As PER-1, if a page I am reading has a reported text problem, I want to be told plainly and calmly, without the Quran text being hidden or changed by a remote file. As the owner, I want to warn users and switch off a faulty pack, recitation or highlight within hours, without a backend.
1. **Given** a `notices.json` whose detached `.sig` verifies (Ed25519 over the exact file bytes, against one of the two public keys compiled into the app), whose `sequence` is higher than the last accepted one and which has not passed `expires`, **then** it is accepted and stored; **given** a bad or missing signature, an unknown `kid`, a lower or equal `sequence`, or an expired file, **then** it is ignored silently and the last accepted copy stays in force (D-10, D-30; schema in [Architecture](04-architecture.md) §14.3).
2. **Given** an accepted notice with action `warn` targeting page P of the active layout, **then** the reader shows an info chip in its chrome on P ("A possible text issue on this page is under review", Urdu in Urdu UI) and Home shows a dismissible notice card, until the notice's `until`; the notice never covers, alters or replaces the Quran text ([UX/UI](02-ux-ui-spec.md)).
3. **Given** an accepted notice with action `disable` targeting a pack, recitation, a recitation's surah highlight or a feature, **then** that item is hidden or disabled on the next refresh with a one-line explanation where the user would have used it (F-RD-05 AC 4, F-PL-06 AC 4); nothing the user created (bookmarks, notes, khatams) is touched.
4. **Given** `minVersion` greater than the installed version code, **then** Home shows a non-blocking "Update available — this version has a known correction" card linking to the app's store page; the app keeps working.
5. **Given** no network, **then** the last accepted notices still apply and expired ones drop; notices are refreshed with the daily signed-file refresh (≤ 1/day, NFR-09), so a notice published within the 48 h SLA reaches online users within a day.
6. **Given** a closed-test (BETA) build before G5, **then** the "pre-release — proofreading in progress" notice uses the same Home card and chrome chip (D-29).

#### F-PL-12 — Launcher shortcuts (new) [P2 · v1.1 · Could]
1. **Given** a long-press on the launcher icon, **then** static shortcuts appear in the UI language: Continue reading, Continue listening (favourite qari), Qibla, Tasbeeh; each opens the same screen as the in-app path via the shared deep-link parser ([UX/UI](02-ux-ui-spec.md) §2.5).
2. **Given** "Continue listening", **then** it resumes from the listening pointer (F-BM-01 AC 5) as a user action, or opens the Listen tab if there is no pointer; it never starts playback without the tap.
3. Shortcuts for features not in the installed release (e.g. Qibla before it ships) are not shown.

---

## 9. P3 / P4 features (summary level)

| ID | Phase | Summary | Key dependency / gate |
|---|---|---|---|
| F-RD-06 (large text) | P3 | Reflowed ayah layout above 1.6× zoom, keeps ayah tap/highlight | Renderer reflow mode |
| F-RD-10 | P3 | Two-page RTL spread on ≥ 600 dp/foldables, pairing pages as in the print | D-23, [Renderer](05-mushaf-rendering-engine.md) |
| F-RD-11 | P3 / P4 | 15-line Hafizi (610) and 13-line Taj (847) in P3; Madani 604 (+ QCF option) in P4; bookmarks map across layouts by verse key | Per-layout permission + G5 each |
| F-RD-14 | P4 | Word-by-word tap meanings (Urdu/English) | QuranWBW or other permission |
| F-RD-15 | P4 | Tajweed colours (Madani first) | Data licence |
| F-BM-07 | P3 | Optional "Sign in with Quran.com" sync of bookmarks/notes/goals (PKCE public client) | QF client approval; Data safety update |
| F-AU-10 (public) | P3 | Arabic + Urdu translation audio | Hi-Tech Music rights |
| F-AU-11 | P3 | Android Auto: browse Surah/Para/Manzil, favourites, resume | Media3 library tree |
| F-AU-12 | P3 | Word-level highlight for reciters with word segments | Word timing data, renderer word boxes |
| F-TR-04 | P3 | English & Roman-Urdu transliteration | Licensed data |
| F-TR-05 | P3 | Tafsir packs (Ibn Kathir, Ma'ariful Qur'an…) | Publisher permissions |
| F-SR-04 | P3 | Topic search (curated topics, retrieval only, cited) | QUL permission |
| F-CP-11 (realtime) | P3 | Firestore group khatam with transactional claims, FCM, App Links; play flavor only | Firebase; Data safety update |
| F-CP-12 | P3 | Hifz mode: sabaq/sabqi/manzil planner on the page grid, hide/reveal with first-letter hints, range repeat, record-yourself (on-device), mistake marking + heatmap, FSRS manzil ordering, mutashabihat alerts, ustad/parent weekly report; validated with 2–3 ustads | Word boxes, gapped audio (G2), RECORD_AUDIO, QUL mutashabihat permission |
| F-PL-08 | P3 (v1.5) | One-time "Support / Sadqa-e-Jaria" IAP; Settings only; correct acknowledge/restore | Commercial-content audit (D-17) |
| F-PL-09 | P3 (v1.1) | Opt-in ACRA crash reports to our Cloudflare Worker → private R2, 90-day retention (the one exception to "no backend"); consent screen; Data safety updated | D-20, D-32 |
| F-CP-14 spike | P3 | 2-week sherpa-onnx evaluation (false alarms ≤ 0.5/min target) | Consented test recordings |
| F-CP-13 | P4 | Noorani Qaida: own lesson grid + ~1–2k recorded clips; age-neutral | Recording budget |
| F-CP-14 | P4 | On-device AI follow-along (auto page-turn while reciting), advisory notice, no tajweed grading claims | Spike pass |
| F-SR-05 | P4 | Recite-to-search | F-CP-14 model |

---

## 10. Reference-app parity matrix

Built from r01 §6 clone checklist. **Clone** = match behaviour; **Improve** = match and fix its defects; **New** = absent there, we add; **Skip** = deliberately not built; **Defer** = later phase.

| # | Reference feature | Verdict | Our ID(s) · phase (release) | Reason |
|---|---|---|---|---|
| 1 | Scanned 16-line Indo-Pak reader | **Improve** | F-RD-01 · P1 | Typeset, ayah-aware, crisp; scans are copyrighted (r09 §1.4) |
| 2 | Usmani / Madani 15-line script | Defer | F-RD-11 · P4 | Indo-Pak first; Hafizi 15-line comes earlier (P3) |
| 3 | 13-line Indo-Pak | Defer (New there) | F-RD-11 · P3 | Requested; needs data permission |
| 4 | RTL swipe + page curl | Clone / Improve | F-RD-01 · P1, F-RD-12 · P2 (v1.1) | Curl optional, slide default |
| 5 | Vertical auto-scroll | Improve | F-RD-09 · P2 (v1.1) | Adjustable speed, pause on touch |
| 6 | Overlay toolbar "Page #N, Juz #M" | Improve | F-RD-02 · P1 | Doesn't cover text; Para · Page · Surah |
| 7 | Immersive + tap toggle | Improve | F-RD-02 · P1 | Restores "keep bars visible" option |
| 8 | Orientation lock (buggy) | Improve | F-RD-07 · P1 | Persisted; + page-turn lock |
| 9 | Landscape fit-width | Clone | F-RD-01 · P1 | Fit-width vertical scroll |
| 10 | Bitmap zoom / iOS word loupe | Improve | F-RD-06 · P1/P3 | Vector zoom, persistent; large-text mode P3 |
| 11 | Night mode (inverted scan) | Improve | F-RD-03 · P1 | True vector themes incl. sepia, AMOLED. High-contrast (r01 §6 #11): covered by AMOLED (pure black, max-contrast ink) + mushaf text contrast ≥ 7:1; no separate theme |
| 12 | Keep screen on | Clone | F-RD-02 · P1 | Toggle, default on |
| 13 | Page-turn vibration always on | Improve | F-PL-02 · P1 | Off by default |
| 14 | One global Resume | Improve | F-BM-01, F-IX-09 · P1; F-BM-04 · P2 | Separate last-read, listening and khatam pointers |
| 15 | Page-only bookmarks, no confirm | Improve | F-BM-02 · P1 | Ayah + page, collections, undo |
| 16 | No ayah interaction | **New** | F-RD-04 · P1 | Ayah action sheet |
| 17 | Juz index (30, Indo-Pak names, page) | Improve | F-IX-02 · P1 | + Arabic names, ruku counts, progress, Play |
| 18 | Surah index with Play/Read | Improve | F-IX-01 · P1 | + Makki/Madani, ayahs, rukus, Para, filter aliases |
| 19 | No Manzil index | **New** | F-IX-03 · P1 | Owner must-have MH-3 |
| 20 | Ruku only printed | **New** | F-IX-04 · P1 | 558-ruku navigation + Play |
| 21 | Go to page / Para-page | Improve | F-IX-05 · P1 | + surah:ayah, Urdu digits |
| 22 | Surah-name filter only ("no search") | **New** | F-SR-01..03 · P1 | Arabic + translation + alias search |
| 23 | Audio per surah only | Improve | F-AU-02 · P1 | Surah, Para, Manzil, Ruku, Page, range, ayah, from-here |
| 24 | 20+ reciters, gated | Improve | F-AU-01 · P1 | All free; curated permitted list; no photos |
| 25 | No ayah highlight | **New** | F-RD-05, F-AU-03 · P1 | #1 request (+4,399) |
| 26 | No start-from-ayah | **New** | F-AU-02 · P1 | Play from here |
| 27 | Surah repeat only | Improve | F-AU-05 · P1 | Ayah × N, range × M, pauses |
| 28 | Speed 1.0 / 2.0 | Improve | F-AU-06 · P1 | 0.5–2.0× steps |
| 29 | Sleep timer | Improve | F-AU-06 · P1 | + end of ayah/surah/Para, fade |
| 30 | Background play + notification | Clone | F-AU-07 · P1 | Media3; Android Auto P3 |
| 31 | Mini + full player | Clone | F-AU-07 · P1 | — |
| 32 | Per-surah download, hidden, no bulk | Improve | F-AU-08 · P1 | Surah/Para/Manzil/full, resumable, verified |
| 33 | Streaming default | Clone | F-AU-09 · P1 | + LRU cache |
| 34 | Audio bookmark / playlist | Skip (v1) | — | Listening pointer + bookmarks cover the need |
| 35 | Translation (claimed, absent) | **New** | F-TR-01, F-RD-08 · P1 | Top unmet ask |
| 36 | WBW / tafsir (claimed, absent) | Defer | F-RD-13 · P2 (summary, v1.0-stretch), F-TR-05 · P3, F-RD-14 · P4 | Licences |
| 37 | Prayer times + adhan (buggy) | Improve | F-CP-01 · P2 (v1.0); F-CP-02 · P2 (v1.1) | Reliability kit; never touch DND/ringer |
| 38 | Hijri + adjustment | Improve | F-CP-04 · P2 (v1.0) | + PK feed, Maghrib rollover |
| 39 | Ramadan Sehri/Iftar | Improve | F-CP-07 · P2 (core v1.0; alarm, share, duas v1.1) | + countdown, alarm, share, Taraweeh plan |
| 40 | Home widget | Improve | F-CP-08 · P2 (v1.1) | Continue reading, next prayer, ayah of day |
| 41 | FCM campaign pushes | Improve | F-CP-09 · P2 (v1.1) | Local, curated, capped, no FCM |
| 42 | Tasbeeh | Clone | F-CP-05 · P2 (v1.0-stretch) | + presets, volume keys |
| 43 | Qibla (claimed, separate app) | **New** | F-CP-03 · P2 (v1.1) | Cheap with adhan2 |
| 44 | Duas / azkar (claimed, separate app) | **New** | F-CP-06 · P2 (v1.1; Manzil-dua playlist v1.0-stretch) | Sourced, Urdu reviewed |
| 45 | Multi-language UI (18 on iOS) | Improve (narrower) | F-PL-03 · P1 | English + Urdu done well; Roman Urdu P3 |
| 46 | Splash hadith + onboarding + script download | Improve | F-PL-01 · P1 | No splash interstitial; no post-install download |
| 47 | Ads, 6 paywalls, referral, rating nags | **Skip** | — (F-PL-10 one gentle ask, v1.1) | Core differentiator (PP-2) |
| 48 | Analytics / attribution SDKs | **Skip** | PRV-01 | Truthful Data safety |
| 49 | Khatam tracker (absent, requested) | **New** | F-BM-04 · P2 (v1.0) | Habit driver |
| 50 | Report text issue (absent) | **New** | F-PL-05 · P2 (v1.0) | Integrity + Pakistani law |
| 51 | Referral "invite friends" | Skip | — | Share ayah/app link covers it organically |
| 52 | In-app update nags / "old version better" regressions | Improve | NFR-16 | Never change pages silently; stable UX |

**v1.0 gaps against the reference.** At v1.0 (staged rollout from 2027-02-01) the reference app's adhan, home widget, Qibla, azkar, vertical auto-scroll and page curl are absent until v1.1 (2027-03-22) unless pulled in (doc 09 §5.2), and tasbeeh only if its stretch rung is restored. The v1.0 store listing must not claim them ([Release](11-release-compliance-business.md)).

---

## 11. Non-functional requirements

Reference devices (aligned with the [Quality](10-quality-testing-ci.md) QB budgets): **High** = Pixel 6a-class (Android 14+; doc 10 "Ref-high"); **Mid** = a mid-range 4–6 GB phone (e.g. Samsung Galaxy A-series); **Low** = a 3 GB low-end phone, Infinix/Tecno/Redmi tier with a Helio G-class SoC (doc 10 "Ref-low"). Functional, unbudgeted checks also run on a 2 GB phone and on one API 24 and one API 26 device (font/kashida). Measured with Macrobenchmark + baseline profile on release builds ([Quality](10-quality-testing-ci.md)).

| ID | Area | Requirement (target) |
|---|---|---|
| **NFR-01** | Cold start | Launch → Home first frame (TTID, p50) ≤ 500 ms High, ≤ 1.2 s Mid, ≤ 1.5 s Low (doc 10 QB-03); launch → last-read reader page fully drawn (TTFD) ≤ 800 ms High, ≤ 2.0 s Low (QB-04). No interstitial splash (system splash only). First-ever launch incl. content-DB copy ≤ 3 s Low. |
| **NFR-02** | Page turn | 10-page swipe: p90 frame time ≤ 16 ms High, p95 ≤ 25 ms Low; 0 frozen frames (> 700 ms); adjacent pages pre-composed (no blank page during swipe). |
| **NFR-03** | Highlight cost | Ayah-change redraws only the highlight layer; ≤ 4 ms main-thread work per change on Low; no page re-layout. |
| **NFR-04** | Search | p90 ≤ 150 ms per query on Low for single-word Arabic/Urdu/English; first results page ≤ 50 rows. |
| **NFR-05** | Responsiveness | Index screens open ≤ 300 ms; Play → first audio ≤ 1 s (downloaded) / ≤ 3 s (streaming, 4G); theme switch ≤ 200 ms. No ANR-prone work on main thread (StrictMode clean in debug). |
| **NFR-06** | Memory | No page bitmaps in the default render path; 3 resident pages ≤ 5 MB (r04); app PSS while reading + playing ≤ 150 MB on Low; Kotlin heap ≤ 64 MB; LeakCanary clean on core journeys (debug). |
| **NFR-07** | Size | Play per-device download (arm64) ≤ **20 MB** at P1 and ≤ **25 MB** at v1.0 (hard cap 30 MB); foss/universal APK ≤ 35 MB; installed footprint before user downloads ≤ 60 MB. Indicative budget: code + Compose 5–7 MB, content DB ~3–3.5 MB compressed, bundled translations ~2.6 MB, DK font 1.63 MB, SQLite native ~1.3 MB, Urdu UI font, GeoNames city DB ~1.5 MB (v1.0), adhan audio ≤ 2 MB (from v1.1, so the v1.0 budget has headroom) (r05 §2.7.4, g04). AAB only on Play. |
| **NFR-08** | Offline | With no network from first launch: reading, all indexes, search, translations, bookmarks, khatam, settings, downloaded audio, prayer times, Hijri (UQ + offset), and — from the release that ships each (§6) — adhan, qibla, tasbeeh, azkar all work. Network is needed only for streaming, downloads, pack/catalogue/feed refresh and sending reports. No screen blocks on a network call; all fetches fail soft to cached data. |
| **NFR-09** | Battery & background | Background work limited to: user-started playback (FGS), user-started downloads (UIDT / FGS worker), prayer/reminder alarms (from the release that ships F-CP-02/F-CP-09, v1.1 by default), one daily maintenance job (alarm verification, catalogue/feed refresh ≤ 1/day, prefer unmetered), widget updates at prayer boundaries. No polling, no FCM, no wakelocks outside playback/download. Keep-screen-on only while the reader is visible, released after the inactivity timeout (F-RD-02 AC 5). No Android vitals "excessive wakeups" or "stuck partial wake lock" flags. |
| **NFR-10** | Reliability | User-perceived crash rate ≤ 0.5% and ANR rate ≤ 0.2% of daily users (Play vitals); 2-hour playback session completes without interruption; downloads resume after network loss and process death; no user-data loss on any update (Room migrations tested for every schema version). |
| **NFR-11** | Content integrity | CI invariants on every content build: 6,236 ayahs; 83,669 tokens; 548 pages × 16 lines; each ayah exactly once, in order; Para (30), Manzil (7), ruku (558, per-Para counts), sajda (14 + 1), quarter (90) tables equal curated values; content DB and packs sha256-verified at install/activation; a mismatch fails closed. |
| **NFR-12** | Accessibility | TalkBack-complete (F-PL-07); 200% font on core screens; touch targets ≥ 48 dp; WCAG AA contrast (text 4.5:1, large text/UI 3:1) in all themes incl. highlight/selection; reduced motion honoured; haptics optional; never TTS the Arabic Quran. |
| **NFR-13** | Localization | English + Urdu at 100% string parity each release; RTL mirroring correct; mushaf always RTL; Urdu in Noto Nastaliq Urdu (line-height 1.8–2.0×); mushaf furniture uses Urdu-style Eastern Arabic-Indic digits; UI digits follow the locale (ur-PK default Western digits); plurals and date/time via ICU/`java.time`; no hard-coded strings (lint). |
| **NFR-14** | Device range | minSdk 24 (Android 7.0) → targetSdk 36, compileSdk 37 (D-02); ABIs arm64-v8a, armeabi-v7a, x86_64 (+x86 for emulators); 16 KB page-size compatible; widths 320–1280 dp; 2 GB RAM minimum; foldables and tablets usable (single centred page until F-RD-10). Test matrix includes API 24, 26, 30, 34 (UIDT), 36, Android 17 preview, Transsion + Xiaomi + Samsung A-series. |
| **NFR-15** | Security | HTTPS only (cleartext disabled); remote JSON (catalogue, pack index, feeds) accepted only with a valid Ed25519 signature; downloaded files hash-verified; no secrets/API keys in the app; only launcher, media service, widget and boot receivers exported; R8 enabled. |
| **NFR-16** | Stability of the page | A layout (mushafId) never changes silently: any correction is a versioned content update with a changelog entry in About this mushaf; page labels and line breaks are stable across releases ("we memorise through it", r02 §2.4 👍2,542). |
| **NFR-17** | Maintainability | Module boundaries per D-03 (features never depend on features); every P1/P2 AC that names data (pages, ayahs, counts) has an automated test; content pipeline reproducible from pinned sources ([Pipelines](08-data-pipelines.md)). |
| **NFR-18** | Store & policy | Play policy compliant at each release: target API, FGS type declarations (mediaPlayback, dataSync ≤ 33), exact-alarm usage via SCHEDULE_EXACT_ALARM, coarse location only, target audience 13+, accurate Data safety ([Release](11-release-compliance-business.md)). |

---

## 12. Privacy requirements

| ID | Requirement |
|---|---|
| **PRV-01** | **Zero third-party SDKs** that collect or transmit data in v1: no ads, analytics, attribution, A/B, crash, push, social SDKs. A CI dependency allowlist fails the build on any new transitive network/analytics library (D-20). |
| **PRV-02** | Play Data safety declares **"No data collected, no data shared"** truthfully; it is re-reviewed before each release and updated before shipping opt-in crash reports (P3), QF sync (P3) or Firebase group khatam (P3, play flavor). |
| **PRV-03** | Network requests go only to our static R2 domain and the catalogued audio hosts; they carry no user/device identifiers, cookies or tracking parameters; User-Agent is generic (app name + version). |
| **PRV-04** | No advertising ID: `AD_ID` and AdServices permissions are absent from the merged manifest (removed with `tools:node="remove"` if any library adds them). |
| **PRV-05** | Location: `ACCESS_COARSE_LOCATION` only, one-shot, foreground, computed on device; stored only as the chosen city/coordinates in local settings; never transmitted; the city picker works without any permission (D-15). No fine or background location, ever. |
| **PRV-06** | Reading/listening history, bookmarks, notes, khatams, streaks, search history and group-khatam names stay on the device; they leave it only via Android Auto Backup (E2E-encrypted on Android 9+) or a user-initiated JSON export (D-20). |
| **PRV-07** | Report-text-error and "Contact" use the user's own email app with a visible, editable message; no logs or identifiers are attached automatically. |
| **PRV-08** | Permission allowlist (v1.0): INTERNET, ACCESS_NETWORK_STATE, FOREGROUND_SERVICE, FOREGROUND_SERVICE_MEDIA_PLAYBACK, FOREGROUND_SERVICE_DATA_SYNC (API ≤ 33 path), RUN_USER_INITIATED_JOBS (API 34+), WAKE_LOCK, POST_NOTIFICATIONS, VIBRATE, ACCESS_COARSE_LOCATION (prayer times, v1.0); SCHEDULE_EXACT_ALARM and RECEIVE_BOOT_COMPLETED are declared from the first release that ships an alarm feature (F-CP-02 adhan, Sehri alarm, F-CP-09 reminders — v1.1 unless pulled in). **Denylist:** AD_ID, ACCESS_FINE_LOCATION, ACCESS_BACKGROUND_LOCATION, USE_EXACT_ALARM, ACCESS_NOTIFICATION_POLICY, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, USE_FULL_SCREEN_INTENT (v1), READ/WRITE_EXTERNAL_STORAGE, QUERY_ALL_PACKAGES, READ_PHONE_STATE; RECORD_AUDIO only from P3 hifz, requested on first use. CI checks the merged manifest. |
| **PRV-09** | Runtime permissions are requested only in context (first download → notifications; enabling adhan → notifications + exact alarms; "use my location" → coarse location), each with a one-sentence reason and a working path if denied. |
| **PRV-10** | No account or sign-in in v1. Future "Sign in with Quran.com" is opt-in, explains what syncs, and can be disconnected with data deletion on the remote side per QF terms. |
| **PRV-11** | A privacy policy (English + Urdu) is published on our domain and linked in the listing and app, stating PRV-01..10 in plain language. |
| **PRV-12** | Target audience 13+; no child-directed imagery or terminology (keeps the app out of Families policy) (D-22). |
| **PRV-13** | Microphone (P3/P4): recordings and AI processing stay on device; never uploaded; user can delete all recordings in one action. |
| **PRV-14** | The foss flavor contains no Google Play Services or Google libraries; the play flavor adds only Play In-App Review (v1.1, F-PL-10) and Play Billing (v1.5) — neither used for tracking (D-15). |

---

## 13. Success metrics (no tracking SDKs)

Sources: **Play Console** (Android vitals, ratings & reviews, acquisition and store-listing reports), **support email / report flow**, **GitHub/issue log**, and the **owner's own device** (private in-app stats, F-BM-05). No in-app telemetry.

### 13.1 P1 exit — owner daily-use criteria (evaluated 2026-12-21 → 2027-01-11)
| ID | Metric | Target |
|---|---|---|
| **SM-01** | Owner uses the build for daily reading/listening | ≥ 6 days/week for 3 consecutive weeks, without falling back to another Quran app |
| **SM-02** | P0 defects (crash, wrong page/text, wrong ayah audio, lost bookmark) | 0 open longer than 48 h |
| **SM-03** | Read + Play from every Surah (114), Para (30) and Manzil (7) row land on the right page/ayah | 100% (scripted test + owner spot-check of 20 rows) |
| **SM-04** | Highlight sync for the favourite Qari on 20 randomly chosen surahs | Judged correct by owner on ≥ 19/20; failures kill-switched |
| **SM-05** | Text discrepancies found by the owner vs his printed mushaf | 0 unresolved at P1 exit |
| **SM-06** | Owner can play "Para N with Qari X" from cold start | ≤ 3 taps, ≤ 5 s |

### 13.2 Public (v1.0 onward; first review 30 days after production rollout, then monthly)
| ID | Metric | Target |
|---|---|---|
| **SM-07** | Average rating (Play Console, PK + all countries) | ≥ 4.7 |
| **SM-08** | 1★ share of ratings | ≤ 3% (benchmarks: Greentech 2.0%, Ayah 3.7%, Muslim Pro 10.7%, r02 §1.5) |
| **SM-09** | ≤ 3★ reviews mentioning ads / paywall | 0% (any occurrence triaged as a bug) |
| **SM-10** | ≤ 3★ reviews mentioning text errors | ≤ 2% |
| **SM-11** | Android vitals user-perceived crash rate / ANR rate | ≤ 0.5% / ≤ 0.2% |
| **SM-12** | Text-error reports: SLA compliance (F-PL-05) | Triage ≤ 24 h; signed notice/kill-switch ≤ 48 h; corrected build submitted ≤ 72 h after confirmation; reporter acknowledged ≤ 72 h; 100% within SLA |
| **SM-13** | "Adhan didn't play / wrong time" share of support emails + reviews | ≤ 2% of reviews; every report answered with health-check steps |
| **SM-14** | Store-listing conversion (visitors → installers, Play Console) | ≥ Play's peer-group median for Books & Reference in PK |
| **SM-15** | 30-day retained installers / uninstall rate (Play Console acquisition & statistics) | Retained installers ≥ peer median; trend improving month over month |
| **SM-16** | Installs by end of Ramadan 1448 (informative, not a gate) | Tracked; no paid acquisition |

Monthly review: export Play reviews, tag themes with the r01 §2.9 regex set (manual, offline), compare against the baseline pain-point table (§2.2).

---

## 14. Out of scope

**Never (product line):** ads of any kind; tracking/analytics/attribution SDKs; selling or sharing data; paywalls on Quran text, recitation, downloads, translations or indexes; scanned page images from any publisher; a translation-only mode; generative AI tafsir, fatwas, chatbots or AI-written religious content; reciter photos or human/animal imagery; music; promotional push notifications; public leaderboards; Ahmadi translations/tafsir; changing the user's ringer/DND state; full-screen-intent alarms (v1).

**Not in v1.0 (may come later, see phase and the §6 Release column):** every feature whose Release is v1.1 (adhan, Qibla, azkar, widgets, reminders, share image, translation packs and translator choice, notes, streaks, JSON export, auto-scroll, page curl, rating prompt, launcher shortcuts); word-by-word (P4); tajweed colours (P4); Madani layout (P4); 15/13-line (P3); two-page spread (P3); hifz mode (P3); Android Auto (P3); cloud sync / accounts (P3); realtime group khatam (P3); tafsir packs beyond Ahsan-ul-Bayan footnotes (P3); transliteration and Roman-Urdu UI (P3); supporter IAP (v1.5); opt-in crash reports (v1.1); Noorani Qaida (P4); Wear OS (P4); AI recitation (P4); topic/semantic search (P3/P4).

**Not planned:** iOS and web apps (D-01, until traction); Android TV; hadith collections; Zakat/Fitrana calculator; salah/qaza tracker; mosque finder or maps; live Makkah/Madinah streams; radio; video content; kids mode inside this app (separate app if ever); ringtones/wallpapers; own app backend in v1 (D-24); Google Maps/Play Services location.

---

## 15. Open product questions

| ID | Question | Default until answered | Owner of answer |
|---|---|---|---|
| **OQ-01** | Confirm the owner's personal print is the 16-line Taj edition (count lines on a mid-Para page; r01/r02 found "~13 lines" vs a 16-line screenshot). | 16-line Taj (D-04) | Owner, P0 |
| **OQ-02** | Is Junagarhi (often associated with Ahl-e-Hadith readers) the right *default* Urdu for a mostly Barelvi/Deobandi audience, or should onboarding ask the user to pick from bundled-eligible options once packs exist? | Junagarhi bundled default (D-09); translator choice (F-TR-03) and Kanz-ul-Iman / Jalandhari packs (F-TR-02) in v1.1 (2027-03-22); the sectarian-default risk for 2027-02-01 → 03-22 is tracked as R-37 | Owner + content advisor |
| **OQ-03** | Default Asr: Hanafi with an onboarding choice "Hanafi (later) / Standard (earlier)" — acceptable, or ask maslak explicitly? | Hanafi + choice | Owner |
| **OQ-04** | Who maintains the monthly PK month-start (Ruet-e-Hilal) feed (≈ 5 min/month), and the escalation if it lapses? | Owner; lapse → UQ + offset | Owner |
| **OQ-05** | Budget for commissioned content: adhan recordings, ~150 Urdu dua translations + scholar review, mushaf ornaments (vector art), hafiz proofreading certificate (G5), legal advice (G6). | Ornaments, G5 certificate and G6 advice by P2 (v1.0); adhan recordings and dua translations by v1.1 | Owner |
| **OQ-06** | Which observances are on by default in the Islamic events calendar (12 Rabi-ul-Awwal, Shab-e-Barat, Shab-e-Mi'raj naming)? | Off by default except Ramadan, Eids, Ashura, Laylat al-Qadr nights | Owner + advisor |
| **OQ-07** | Is a small Firebase dependency acceptable for realtime group khatam in P3 (play flavor only, Data safety change)? | Text sheet only until decided | Owner, before P3 |
| **OQ-08** | Diaspora priority: should UK/Gulf presets and an English-first listing ship at v1.0, or later? | Presets ship (F-CP-01); listing PK-first | Owner |
| **OQ-09** | Apply now for a Quran Foundation OAuth client (lead time) for P3 sync? | Yes, apply in P2 | Owner |
| **OQ-10** | Launch destination: Home with Continue card (default) vs straight to last page — owner preference for his own routine? | Home; setting available | Owner, P1 feedback |
| **OQ-11** | Hifz edition priority for P3: 15-line Hafizi vs 16-line default (validate with 2–3 ustads)? | 15-line first (F-RD-11) | Owner + ustads |
| **OQ-12** | Content-errors and support address/domain for F-PL-05, privacy policy and listing (needs the public brand, G3). | Placeholder on R2 domain | Owner |
| **OQ-13** | ~~May a closed-test build carry G1/G2-pending content?~~ **Resolved (D-29):** closed testers are third parties, so a closed-test (BETA) build carries mushaf layout data only if G1-cleared or clean-room (no QUL-derived layout before written permission), with a "pre-release — proofreading in progress" notice until G5; recitations may be PERMITTED, HOST_GRANT or PENDING, never PERSONAL_ONLY. PUBLIC builds carry only PERMITTED / HOST_GRANT. | — | Closed |
| **OQ-14** | Should "Play adhan even when silent" exist at all (opt-in), given silent-mode override was a top reference complaint? | Opt-in, default off | Owner |

---

## 16. Appendix

### 16.1 New IDs introduced by this doc
- Features: **F-PL-10** Gentle rating prompt (P2, Could, v1.1); **F-PL-11** Signed remote notices (P2, Must, v1.0 floor); **F-PL-12** Launcher shortcuts (P2, Could, v1.1; UI in [UX/UI](02-ux-ui-spec.md) §2.5).
- Owner must-haves **MH-1..MH-4**; personas **PER-0..PER-4**; principles **PP-1..PP-8**; non-functional **NFR-01..NFR-18**; privacy **PRV-01..PRV-14**; success metrics **SM-01..SM-16**; open questions **OQ-01..OQ-14**.

### 16.2 Glossary
| Term | Meaning |
|---|---|
| Mushaf | A printed copy of the Quran; here, a page layout (mushafId) |
| Para / Parah (Juz) | One of 30 parts; Indo-Pak boundaries differ from Madani in 7 places (D-07) |
| Manzil | One of 7 weekly-reading parts (start surahs 1, 5, 10, 17, 26, 37, 50); also the "Manzil dua" (protective verses) — distinct |
| Ruku (ع) | One of 558 thematic sections; margin triple = ruku-in-surah / ayahs-in-ruku / ruku-in-Para |
| Ruba / Nisf / Thuluth | Para quarter marks (¼, ½, ¾), 90 positions |
| Sajda | Prostration verse; 14 Hanafi + 22:77 (Shafi'i) |
| «۵» mark | Non-Kufan ayah-end sign; glyph only, never a boundary |
| Sar-lauh | Ornamental opening layout of printed pp. 2–3 |
| Sabaq / Sabqi / Manzil (hifz) | New lesson / recent revision / long-term revision |
| Khatam | Complete reading of the Quran; esal-e-sawab = dedicating its reward |
| Qari; Murattal / Mujawwad / Muallim | Reciter; measured / melodic / teaching style |
| Gapless / Gapped | One file per surah + timings / one file per ayah |
| Kufan count | 6,236-ayah numbering used here; 1:1 = basmala |
| page_index / page_label | Storage index 1–548 / printed number 2–549 shown in UI |
