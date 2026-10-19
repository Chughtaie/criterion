# 02 — Competitor Landscape: Quran apps for Pakistan / South Asia

*Researcher key: r02-competitors. Data collected 2026-10-19. Store numbers come from Google Play (gl=pk, hl=en) via `google-play-scraper` 1.2.7. Reviews: 13,899 Play reviews across 19 apps (up to 400 "newest" + 400 "most relevant" per app), plus small Urdu-language samples.*

---

## 1. Executive summary

1. **The reference app's position is strong but fragile.** "Al QURAN – القرآن الكريم" (`free.alquran.holyquran`, 9D Muslim Apps) has a 4.64 rating, 1,135,804 ratings, 50M+ installs (Play's internal counter shows about 70.7M) and was updated 2026-10-14 (v4.4.46). It ranks **#1 in Pakistan** for "Quran Pak", "quran", "al quran", "holy quran urdu" and "quran sharif", and #2 for "Quran Majeed". However, **52% of its sampled ≤3★ reviews complain about ads.** Reviews from 4–18 Oct 2026 mention ads that can't be closed, betting ads and immodest ads. Since late 2024 it has also put **reciters and downloaded audio behind premium or rewarded ads.** [verified: Play listing + reviews]
2. **The reference app is a scanned-image, 16-line Indo-Pak mushaf**, plus an "other" (Madani) font. Users repeatedly ask for **13- and 15-line editions**, which it doesn't offer. They also ask for **ayah highlighting during audio** (the top review has 4,399 👍), zoom that persists across pages, a working orientation lock, and verse-level bookmarks. Its APK is small (about 26 MB). [verified: reviews; APKPure]
3. **Table stakes in Pakistan:** an Indo-Pak page mushaf, Surah/Para index, bookmarks with resume, night mode, zoom/landscape, several famous Qaris with offline download, background play and repeat, Urdu translation, and (for "all-in-one" apps) prayer times, Qibla, duas and tasbeeh.
4. **No app combines everything.** Nobody offers *all three Indo-Pak line editions (13/15/16) + audio synced to the page + Urdu translation/WBW + no ads* in one app. Taj Company ships separate ad-supported apps per line count. Fanzetech is 16-line only with no audio. Quran.com's Naskh app is 15-line only. Greentech is ad-free and has "Naskh Indopak" but is built for translation study, not a mushaf-first experience.
5. **Ads drive Pakistani users away from Quran apps.** In sampled ≤3★ reviews, ad complaints make up 50–53% for Muslim Pro, Quran Majeed, Fanzetech and Holy Quran Offline, versus **2–6% for ad-free Quran.com and Greentech**. Ad-free apps also have the best 1★ shares: Greentech 2.0%, Ayah 3.7%, Muslim Pro 10.7%. Google autocomplete for Pakistan suggests **"quran app without ads"** after "quran app". AdMob category blocking is only "best effort". [verified]
6. **Users accept some ways of paying and resent others.** They accept optional one-time "remove ads / support us" purchases, donations or "Sadqa-e-Jaria" framing, and premium for genuinely costly AI (Tarteel, which has a pay-what-you-can Alim program). They resent app-open and interstitial ads during recitation, gating reciters or downloads they already have, premium that fails to restore, surprise auto-renewals and promo notifications.
7. **The Pakistani audience has its own needs.** The 16/15/13-line "Hafizi" editions are a page-memory aid for huffaz. Users want alternating left/right margins and ruku "ع" placement exactly as in print (top Fanzetech review: 2,297 👍), a two-page spread, Para-wise audio and downloads, and Manzil/Ruku/Sajda navigation. They also need a choice of Urdu translator, because maslak matters: Islam360 offers Juna Garhi, Taqi Usmani, Tahir-ul-Qadri, Kanzul Iman, Maududi, Hasan Rizvi and others.
8. **The leading "global" apps leave the Pakistani gap open.** Quran for Android (GPL-3, open source, about 61M installs) has Madani image pages, highlighting, Quran.com sync, widgets and Android Auto. Its Indo-Pak edition lives in a **separate app** (Naskh, 15-line, last updated 2025-09). Quran.com also launched **"Quran Audio"** (`com.quranicaudio.app`) on 2026-08-18: audio-first, no ads, with word highlighting, widgets and Android Auto. [verified]
9. **Tarteel owns AI recitation** (10M+ installs). Its free tier includes follow-along and voice search. Mistake detection, goals and analytics are premium, and it needs internet. Its reviews complain about the paywall (25%) and about accuracy and lag. Copying this is a poor bet for v1.
10. **Text integrity drives 1★ reviews for low-quality clones.** "Full Quran Sharif" has 36% of negative reviews about zer/zabar or missing words, and "Quran with Urdu Translation" has 26%. Fanzetech had missing pages. Pakistan also has a legal angle: the **Punjab Holy Quran (Printing and Recording) Act 2011** requires certified error-free text. A Lahore High Court order (2019, pursued in 2021) required Quran Board approval before publishing "on the internet". How this applies to apps is untested. [verified: WIPO, Dawn]
11. **ASO in Pakistan:** high-volume terms are "Quran Pak", "Quran Majeed", "Quran Sharif", "al quran", "16 line quran", "para 1…30", "surah yaseen", "tilawat", "with urdu translation", "offline" and "without ads". Bilingual titles with القرآن الكريم are standard. Note that "Manzil" searches mostly mean the *Manzil dua* (protective verses), which has 1M+-install apps.
12. **Recommended positioning:** an ad-free, lightweight, **Indo-Pak-first mushaf + Qari app**, with all line editions, ayah-highlighted audio on the page, Para-wise download and play, Urdu with translator choice, and huffaz tools. Prayer times come later and are kept minimal. Monetize through optional support only, and never gate the Quran or its recitations.

---

## 2. Detailed findings

### 2.1 Method and caveats
- Store metadata was fetched per package from `https://play.google.com/store/apps/details?id=<pkg>&hl=en&gl=pk` using google-play-scraper. "Real installs" is the scraper's `realInstalls`, Play's internal approximate counter; treat it as indicative only.
- Keyword ranks come from Play search with country=pk (Section 2.9). Autocomplete comes from `suggestqueries.google.com` with gl=pk; these are web searches, used as a proxy for intent.
- Reviews: `lang='en', country='pk'`. Play returns language-filtered rather than strictly country-filtered reviews, so the sample is a South-Asia-heavy English sample, not a Pakistan-only one. Themes were counted with regexes, so percentages are directional (±10 pp). "Mistakes" for Tarteel mostly means AI mistake detection, not text errors.
- Play no longer exposes APK size, so sizes come from APKPure download listings (approximate; XAPK/base APK; may differ from what Play delivers).

### 2.2 Market snapshot (Pakistan storefront) [verified: Play listing for each package, gl=pk, 2026-10-19]

| # | App (package) | Developer | ★ | Ratings | Installs (band / ~real) | 1★ share | Last update | APK ~size† | Monetization (Play flags) |
|---|---|---|---|---|---|---|---|---|---|
| R | **Al QURAN – القرآن الكريم** (`free.alquran.holyquran`) | 9D Muslim Apps | 4.64 | 1,135,804 | 50M+ / 70.7M | 4.9% | 2026-10-14 (4.4.46) | 26.2 MB | IAP Rs 280–6,900. **Listing lacks the "Contains ads" label, but reviews through 2026-10-18 report heavy ads**‡ |
| 1 | Quran for Android (`com.quran.labs.androidquran`) | quran.com | 4.59 | 618,398 | 50M+ / 61.3M | 6.3% | 2026-10-07 | 13.5 MB | Free, no ads, no IAP; GPL-3 open source |
| 1b | Quran – Naskh (Indopak) (`com.quran.labs.androidquran.naskh`) | quran.com | 4.52 | 3,530 | 100K+ / 0.32M | — | 2025-09-21 | — | Free, no ads |
| 1c | Quran Audio (`com.quranicaudio.app`) — *Quran.com's newer app* | quran.com | 5.0 | 118 | 5K+ | — | 2026-10-10 (released 2026-08-18) | — | Free, no ads |
| 2 | Quran Majeed – القران الكريم (`com.pakdata.QuranMajeed`) | Pakdata (PDMS) | 4.67 | 1,112,720 | 10M+ / 41.4M | 3.8% | 2026-10-16 | 58.1 MB | Ads + IAP Rs 105–180,800 |
| 2b | Quran Pak 360 (`com.pakdata.quranpak`) | Pakdata | 4.69 | 1,176 | 100K+ / 0.46M | 3.8% | 2026-07-25 | — | Ads + IAP Rs 550–4,200 |
| 3 | Muslim Pro (`com.bitsmedia.android.muslimpro`) | Bitsmedia | 4.36 | 1,919,583 | 100M+ / 148.4M | 10.7% | 2026-10-12 | 97.5 MB | Ads + subscription (US $12.99/mo or $34.99/yr, 3-day trial) |
| 4 | Tarteel: AI Quran Memorization (`com.mmmoussa.iqra`) | Tarteel Inc. | 4.52 | 122,061 | 10M+ / 16.1M | 7.6% | 2026-10-15 | 256.9 MB | No ads; subscription IAP Rs 1,990–45,500; Alim pay-what-you-can |
| 5 | Ayah: Quran App (`com.ayah`) | Ayah App | 4.69 | 148,317 | 10M+ / 11.5M | 3.7% | 2025-08-03 | 142.3 MB | Free, no ads, no IAP |
| 6 | Al Quran (Tafsir & by Word) (`com.greentech.quran`) | Greentech Apps Foundation (UK charity 1178251) | **4.82** | 403,122 | 10M+ / 14.6M | **2.0%** | 2026-09-19 | 70.7 MB | Free, no ads, donations |
| 7 | iQuran Lite (`com.guidedways.iQuran`) / iQuran Pro (`…iQuranPro`) | Beehive Innovation | 4.65 | 215,349 / 44,668 | 10M+ / 100K+ | 4.5% | 2025-08-16 | — | Lite free with limits; Pro paid (Rs 310 one-time) |
| 8 | Islam360 (`com.islam360`) | Islam360 Foundation Trust | 4.73 | 289,544 | 10M+ / 22.4M | 3.7% | 2026-09-22 | 166.1 MB | Ads + IAP Rs 49–40,000 ("remove ads = Sadqa-e-Jaria") |
| 9 | Holy Quran (16 Lines per page) (`com.fanzetech.holyquran`) | Fanzetech | 4.51 | 67,027 | 5M+ / 8.5M | 8.1% | 2026-02-12 | 67.3 MB | Ads only |
| 9b | Holy Quran – Pakistan Edition (`com.fanzetech.quran.pak`) | Fanzetech | 4.52 | 5,480 | 1M+ / 2.4M | 8.1% | 2026-02-12 | — | Ads only |
| 10 | 16 Lines Quran (Tajweedi) Color (`quran.taj.tajquranapp`) | Taj Company Ltd | 4.40 | 4,526 | 1M+ / 1.0M | 11.9% | 2026-10-17 | 45.9 MB | Ads + IAP Rs 300–800 |
| 10b | Taj 13-line / 15-line / 16-line BW / 18-line (`quran.taj.tajquran13line`, `…15line`, `…16linebw`, `…18line`) | Taj Company Ltd | 4.54 / 3.99 / 4.34 / 4.32 | 434 / 3,764 / 852 / — | 100K–500K+ | 7.5–18.5% | 2026 | — | Ads + IAP |
| 11 | Al Quran-ul-Kareem (`com.dawateislami.alquranulkareem`) | IT dept. of Dawat-e-Islami | 4.74 | 43,578 | 1M+ / 4.6M | 4.0% | 2025-07-29 | 66.4 MB | Free, no ads (org-funded) |
| 11b | Sirat ul Jinan Quran & Tafsir (`com.dawateislami.AlQuran.Translation`) | Dawat-e-Islami | 4.75 | 53,420 | 1M+ / 3.1M | 3.5% | 2026-10-05 | — | Free, no ads |
| 12 | Athan Pro (`com.quanticapps.athan`) | Quanticapps | 4.70 | 98,553 | 1M+ / 4.4M | 3.3% | 2026-08-11 | — | Ads + IAP Rs 550–27,700 |
| 13 | Holy Quran Offline Reading (`quran.offline.alquran.holyquran.quranpak.quranmajeed…`) | JedyApps | 4.57 | 200,873 | 5M+ / 8.7M | 6.1% | 2026-09-27 | 95.8 MB | Ads + IAP Rs 500–4,200 |
| 14 | Al-Quran (Free) (`com.isysway.free.alquran`) | ISYSWAY | 4.49 | 409,539 | 50M+ / 68.4M | 8.7% | 2026-10-15 | — | Ads + IAP |
| 15 | Quran with Urdu Translation (`com.QuranReading.urduquran`) | Quran Reading | 4.48 | 114,368 | 10M+ / 11.7M | 9.3% | 2026-02-13 | — | Ads + IAP |
| 16 | Full Quran Sharif Offline (`com.fullquransharif.quranpak.translation.qibladirection`) | Dictionary World11 | 4.40 | 103,760 | 10M+ / 12.2M | 9.5% | 2026-10-15 | — | Ads + IAP |
| 17 | Urdu Quran (16 lines) (`com.iroshni.urduquran16lines`) | iroshni.com | 4.79 | 18,720 | 1M+ / 1.4M | 3.5% | **2020-07-23** (abandoned) | 94.6 MB | Free, no ads |
| 18 | Quraan-E-Karim (15 Lines) (`com.deeniyat.quraan15liner`) | Deeniyat Educational & Charitable Trust | 4.68 | 16,595 | 1M+ / 2.0M | 4.9% | 2026-02-28 | — | Free, no ads |
| 19 | Al Quran Kareem 16 lines (`com.quranmajeed.sixteenline`) | Sahal Apps | 4.33 | 3,936 | 1M+ / 2.8M | 11.7% | 2026-10-06 (released 2026-01-30) | — | Ads + IAP Rs 1,150–13,900 |
| 20 | AL Siraat: Learn Quran & Qaida (`com.alquran.aiquran.quranlearning`) | Darwin Technology | 4.53 | 7,223 | 1M+ / 4.9M | 7.0% | 2026-10-14 (released 2025-11) | — | No ads; IAP |
| 21 | Wemu / WeMuslim (`com.fyxtech.muslim`) | Fyxtech HK | 4.52 | 752,664 | 100M+ / 130M | 7.5% | 2026-09-03 | — | Ads + IAP (Indonesia-heavy) |

† APKPure listing sizes [verified: apkpure.com/x/&lt;pkg&gt;], approximate.
‡ The scraper's `containsAds=False`, and a grep of the listing HTML found no "Contains ads" string (Quran Majeed's listing does have it). Yet 20+ reviews dated 2026-09-15 to 2026-10-18 (v4.4.41–4.4.46) describe app-open ads, unclosable ads and betting ads. Treat the reference app as **ads + IAP/subscription**.

**Observations**
- **Three apps hold most of the Pakistani Quran-reader market:** 9D's Al Quran (~71M installs), Pakdata's Quran Majeed (~41M; Pakistani company with a "100M users" claim) and Quran for Android (~61M). Muslim Pro and Wemu are bigger, but they are prayer-time super-apps.
- **Ad-free apps have the lowest 1★ shares:** Greentech 2.0%, Athan (IslamicFinder) 1.8%, Ayah 3.7%, Dawat-e-Islami 4.0%. Ad-heavy apps sit around 8–12%, and Muslim Pro is at 10.7%.
- **Many Pakistani 16/15/13-line apps are tiny or neglected.** iroshni has had no update since 2020 yet still has 1.4M installs and a 4.79 rating. Taj's apps are fragmented by line count. This is an opening for a well-built, maintained app.

### 2.3 Competitor profiles

| App | Mushaf styles | Audio model | Translation / tafsir | Hifz / AI | Sync, widgets, other surfaces | Prayer / Qibla | Standout | Top complaints (from review sample) |
|---|---|---|---|---|---|---|---|---|
| **Al Quran (9D) — reference** | Scanned 16-line Indo-Pak pages + a Madani-style font ("both indo-pak font and the other one"); page-turn effect, haptic on turn, landscape | Mishary Alafasy, Sudais, Saad Al-Ghamdi, Maher Al-Muaiqly (+more); download, background, repeat, auto-scroll. Since late 2024 only 1 reciter free; others need premium or a rewarded ad | Listing claims multi-language translation, WBW and tafsir summaries; 2025 reviews say "no translation" in page mode | None | ? | Yes: prayer times + azan, Qibla, azkar, tasbeeh, Hijri, Ramadan | Familiar 16-line pages, simple UI, many Qaris | Ads (52% of negatives) including immodest and betting ads; audio paywalled behind ads; premium lost after update; slow load and long splash; azan unreliable; no 13/15-line; bookmarks only per page; orientation lock fails |
| **Quran for Android** (+ Naskh app) | Madani images (main app); 15-line Indo-Pak Naskh in a separate app (images "used with permission of SHL Info Systems"); Qaloon/Warsh apps | 15+ reciters, gapless, ayah highlight, custom repeat | 20+ languages of translation and tafsir (QuranEnc, KSU, Tanzil) | Repeat only | Quran.com account sync (`feature/sync`), bookmark and search widgets, Android Auto (`feature/autoquran`) | No | Open source (GPL-3), fast, trusted | Data re-download loops, audio stopping mid-surah, search quality; Urdu font changes |
| **Quran Audio (quran.com, 2026)** | Follow-along verses (not a mushaf) | Reciter catalog, word highlighting where timing exists, playlists, repeat range, speed, sleep timer | English/Arabic translation, tafsir | Khatma plan | Widgets, Android Auto, lock-screen controls | No | Audio-first, no ads | Too new to judge (118 ratings) |
| **Quran Majeed (Pakdata)** | Uthmanic, "Mushaf" and Indo-Pak scripts, all proofread by an Alim | 15 famous Qaris; HD Urdu/English translation audio; repeat, interval, speed | 45+ languages; 4 English; Arabic tafsirs; transliteration | Hifz with voice recording | Wear OS complications; prayer widget | Yes (+ Makkah/Madinah live, halal places, Quran TV) | The Pakistani all-rounder; strong brand | Ads (50%), paywall pressure (26%), ads breaking playback, notification spam, app weight (users want an old 16–18 MB lite version) |
| **Muslim Pro** | Quran text with translation | More reciters and downloads are **Premium-only** | Many languages | Tracker, "Quests" | Login; widget is Premium or "watch an ad"; AI "Ask AiDeen"; Qalbox video; community | Core | Prayer-time leader, huge brand | Ads with fake close buttons (53%), subscription restore failures, data loss, prayer-time accuracy |
| **Tarteel** | Madani or Indo-Pak; hide-verses mode | Free unlimited listening | Translations | **AI recitation follow-along (free), voice search "Shazam for Quran" (free); mistake detection, goals and analytics (Premium)** | Account | No | Only mature AI recitation engine | Paywall (25%), accuracy and lag, needs internet, Premium price in PKR |
| **Ayah** | Uthmani only | Gapless verse-by-verse, repeats, sleep timer | Reduced: English translation and tafsirs removed in 2024 updates | Single moving "khatmah" bookmark | Notes, favourites, share verse as image | No | Clean, fast, distraction-free | Removed features; Android lags iOS; no Urdu; no update since Aug 2025 |
| **Greentech Al Quran** | Mushaf mode: Madani, **Naskh Indopak**, Qaloon, Shemerly, Warsh + Unicode | 80+ reciters incl. WBW and translation audio; group repeat | 170+ translations in 60+ languages; **WBW in 15+ languages incl. Urdu**; 8 Arabic tafsirs; grammar/morphology | Planner (29/30-day), goals, streaks, badges | Cross-device sync, notes, collections, topics | No (sister app "Sadiq") | Best-rated (4.82); charity; offline | Navigation complexity, sync glitches, font changes, audio re-downloads |
| **iQuran** | Text + translation, tajweed colours | Verse-by-verse, group repeat | 1 translation in Lite | Repeat | Tags | No | Lite/Pro split | Lite limits (5 bookmarks, 1 reciter); stale |
| **Islam360** | Text mode + "15 line Quran" section | 12+ reciters; Urdu audio translation; audio tafsir (Dr Israr Ahmed, Mufti Sayeed) | **Urdu, English, Hindi, Roman Urdu**; ~10 Urdu translators across maslaks; Urdu WBW; tafsirs (Taqi Usmani, Maududi, Ibn Kathir) | — | Notes | Yes | Quran + 9 Hadith books + Qaida (27 lessons) — "Pakistani super-app" | Ads (37%), resume/WBW bugs, heavy (166 MB) |
| **Fanzetech 16-line** | Image 16-line Indo-Pak, page curl, 5 presets, colour overlay, brightness, sajdah highlight, Juz-start marker | **None** (only a page-turn sound) | None | Volume-key bookmark | — | No | Real-book feel; "single file, no external data" | Inappropriate ads (50%); no zoom (earlier); missing pages (187–188, 188–192); identical left/right pages (users want alternating margins); wants two-page view |
| **Taj Company** (13/15/16/18-line, BW and colour) | Official digital versions of Taj's printed mushafs; tajweed colour; "checked by proofreaders" | Mostly none (users request audio with Urdu) | None | Bookmarks | — | No | **Search by Para, Surah, Ayat, Ruku, Manzil, Sajda**; publisher provenance | Crashes and bugs (25% of negatives), blurry landscape, accidental page turn while zoomed, one app per line count |
| **Dawat-e-Islami Al Quran-ul-Kareem** | 16-line page images; Para-wise index | Multiple Qaris, repeat, para listening | In the separate Sirat ul Jinan app (Kanzul Iman / Barelvi) | — | — | Separate Namaz app | Free, no ads, trusted by Barelvi audience | Branding header/footer eats lines; landscape wastes space; wants 13/15-line, download-all-paras, media-player tab |
| **Athan Pro** | Quran module | MP3 Qaris | Many languages | — | Wear OS | **Core** (methods, auto-silent, Ramadan dashboard) | Prayer-first | Paywall (17%), ads (26%), azan/notification reliability |

### 2.4 Reference app: what its reviews teach a clone

| Signal (review evidence) | Implication for our build |
|---|---|
| "Highlight the AYAT being recited", "zoom a portion of the page" (👍4,399, 2022; dev: "working on such a feature") | Ayah highlight on Indo-Pak page images needs per-page ayah bounding boxes. This is the #1 clone gap. |
| "Please do not change the pages… we memorise through it" (👍2,542) | Page images or layout must stay **byte-stable across updates**; version the page sets. |
| "Bookmarks only for pages", recitation "starts from the very beginning again" (👍1,811) | Verse-level bookmarks; resume audio at the exact ayah. |
| "Lock icon… what does it do?" (👍1,408); "orientation lock… still rotates" (2026-02) | Clear lock affordance (rotation + accidental-swipe lock) with a tooltip. |
| "Vibration on every page turn" (👍695; made optional) | Haptics off by default. |
| "Splash screen long… want to read right away" (👍144) | Open straight to last page; no splash ads. |
| "Landscape enlarged-text mode removed" (👍262, 2025-08); "zoom resets every page" (👍565) | Persistent zoom and fit-width landscape. |
| "16-line… no option for 13-line" (2024-11); "Hafiz of 15-line can't use it" (2023-04); Urdu review: "حافظی قرآن کا نسخہ نہیں ہے" | Ship 13/15/16-line editions (licensing permitting). |
| "Go-to page changes my resume point" (👍214); "kids change the page — lock page" (2026-02) | Separate *last-read (khatm) marker* from *browsing position*; optional page lock. |
| "Downloaded audio asks to watch an ad" (👍440); "1 reciter free, rest premium" (2024-11) | **Never gate downloaded or offline audio.** It is the most resented pattern. |
| "Premium removed after update / restore not working" (several) | Use Play Billing properly: acknowledge purchases, restore, entitlement cache. |
| Azan "doesn't play anymore", switches phone off silent, timezone/DST issues | If prayer times ship, treat them as a reliability-critical subsystem (exact alarms, DND/silent respect, manual offsets). |

### 2.5 Feature matrix

Legend: **Y** = yes (free) · **P** = premium, paid or ad-gated · **~** = partial, limited or separate app · **–** = not offered or not found · **?** = could not verify. Columns: Ref = 9D Al Quran; Q4A = Quran for Android (+Naskh); QAud = Quran.com "Quran Audio"; QMj = Quran Majeed; MP = Muslim Pro; Tar = Tarteel; Aya = Ayah; GT = Greentech; iQ = iQuran; I360 = Islam360; F16 = Fanzetech 16-line; Taj = Taj Company apps; DeI = Dawat-e-Islami; AP = Athan Pro.
Class: **TS** = table stakes (expected in Pakistan); **D** = differentiator; **N** = niche.

| Feature | Ref | Q4A | QAud | QMj | MP | Tar | Aya | GT | iQ | I360 | F16 | Taj | DeI | AP | Class |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Page-mode mushaf (printed-page feel) | Y | Y | – | Y | ? | Y | Y | Y | – | ~ | Y | Y | Y | ? | **TS** |
| Indo-Pak script | Y | ~ (separate 15-line app) | ? | Y | ? | Y | – | Y | – | Y | Y | Y | Y | ? | **TS (PK)** |
| Several Indo-Pak line editions (13/15/16) in **one** app | – (16 only) | – | – | ? | – | ? | – | ? | – | ~ (15-line section) | – | ~ (one app per edition) | – | – | **D (white space)** |
| Faithful left/right asymmetry, two-page spread | ? | – (requested) | – | ? | – | – | – | ? | – | – | – (requested) | ? | – | – | **D** |
| Tajweed colour-coding | ? | – | – | ? | ? | ? | – | Y | P | ? | – | Y | – | ? | N/D |
| Night mode | Y | Y | ? | Y | ? | ? | Y | Y | ? | Y | Y | ? | ? | Y | **TS** |
| Zoom / font size / landscape | ~ (zoom resets) | Y | – | Y | ? | ? | ? | Y | ? | ~ | ~ | ~ | ~ | ? | **TS** |
| Surah + Para/Juz index (Para names) | Y | Y | Y | Y | Y | Y | Y | Y | Y | Y | Y | Y | Y | ? | **TS** |
| Manzil / Ruku / Sajda navigation | ? | ? | – | ? | – | – | – | ? | – | ? | ~ (sajdah highlight) | **Y** | ? | – | **D (PK)** |
| Bookmarks + last read + resume | Y (page only) | Y | Y | Y | Y | Y | Y (single moving) | Y | P (5 max in Lite) | Y | Y | Y | Y | ? | **TS** |
| Notes / tags / collections | ? | Y (tags) | ~ (playlists) | ? | Y (journal) | ? | Y | Y | ~ | Y | – | – | – | – | D |
| Search (Arabic / translation) | ? | Y | ? | Y | ? | Y (voice) | Y | Y | ~ | **Y (Arabic, Urdu, Roman Urdu, roots)** | – | Y | Y | ? | TS |
| Multiple famous Qaris | P (1 free) | Y (15+) | Y | Y (15) | P | Y | Y | Y (80+) | P (1 in Lite) | Y (12+) | – | – | Y | Y | **TS** |
| Offline audio download | P (rewarded ads) | Y | Y | Y | **P** | ? | ~ | Y | Y | ? | – | – | Y | Y | **TS** |
| Background play + lock-screen controls | Y | Y | Y | Y | ? | Y | Y | Y | ? | ? | – | – | ? | ? | **TS** |
| Repeat ayah / range | Y | Y | Y | Y | ? | ? | Y | Y | Y | ? | – | – | ~ | ? | **TS** |
| Ayah highlight synced to audio | ? (top request) | Y | Y (word) | Y | ? | Y | ? | ? | ? | ? | – | – | ? | ? | **D → TS** |
| Para-wise play / bulk download | ? | ~ | ? | ? | – | – | – | ? | – | ? | – | – | ~ (requested) | – | **D (PK)** |
| Speed control, sleep timer | ? | Y (speed) | Y | Y (speed) | ? | ? | Y (timer) | ? | ? | ? | – | – | ? | ? | N → TS |
| Android Auto | ? | Y | Y | ? | ? | ? | ? | ? | ? | ? | – | – | ? | ? | N |
| Urdu translation | ~ | Y | – | Y | ? | ? | – | Y | ? | Y | – | – | ~ (separate app) | Y | **TS (PK)** |
| Choice of Urdu translators (maslak) | – | ~ | – | ? (Pakdata's Quran Pak 360 has 4) | – | – | – | ? | – | **Y (~10)** | – | – | – (Kanzul Iman only) | – | **D (PK)** |
| Urdu word-by-word | ? | ? | – | ? | – | – | – | Y | – | Y | – | – | – | – | **D** |
| Urdu translation audio | – | – | – | Y | – | – | – | Y | – | Y | – | – (requested) | – | – | D |
| Tafsir | ~ (summaries) | Y | Y | Y | ? | ? | ~ | Y | ? | Y (+ audio) | – | – | ~ (separate app) | ? | D |
| Roman Urdu / transliteration | – (requested) | ? | – | Y (English) | ? | ? | – | ? | – (requested) | Y (Roman Urdu) | – | – | – (requested) | ? | N/D |
| Hifz tools (mistake marking, hide verses, revision) | – | ~ (repeat) | ~ | Y (voice record) | – | P | – | ~ | ~ | – (requested) | – (requested) | – (requested) | – (requested) | – | **D** |
| AI recitation feedback | – | – | – | – | – | Y / P | – | – | – | – | – | – | – | – | D (expensive) |
| Khatm planner / goals / streaks | – | – | Y | ~ | Y | P | ~ | Y | – | ? | – | – | – | Y (Ramadan) | D |
| Account sync / backup | ? | Y | ? | ? | Y | Y | ~ | Y | – | ? | – | – | – | ? | D |
| Home-screen widgets | ? | Y | Y | Y | P | ? | ? | ? | ? | ? | – | – | – | ? | N |
| Prayer times + azan | Y | – | – | Y | Y | – | – | – | – | Y | – | – | – (separate app) | Y | TS for all-in-one; N for pure Quran |
| Qibla, duas/azkar, tasbeeh, Hijri | Y | – | – | Y | Y | – | – | – | – | Y | – | – | – | Y | Same as above |
| Hadith | – | – | – | – | ~ | – | – | – | – | Y | – | – | – | – | N |
| Noorani Qaida (kids) | – | – | – | – | – | – | – | – | – | Y | – | – | – | – | N (separate 1M+ apps exist) |
| Wear OS | ? | – | – | Y | ? | – | – | – | – | – | – | – | – | Y | N |
| **No ads** | – | Y | Y | – | – | Y | Y | Y | Y | – | – | – | Y | – | **D** |
| Open source | – | **Y (GPL-3)** | ? | – | – | – | – | – | – | – | – | – | – | – | — |

### 2.6 Review mining: what hurts (≤3★ reviews, share mentioning each theme)

| App | n reviews | n ≤3★ | Ads | Paywall / subscription | Text mistakes / missing | Audio / download |
|---|---|---|---|---|---|---|
| Al Quran (9D, reference) | 776 | 146 | **52%** | 12% | 2% | 18% |
| Quran for Android | 785 | 61 | 2% | 0% | 13% | 34% |
| Quran Majeed | 751 | 217 | **50%** | **26%** | 3% | 19% |
| Muslim Pro | 795 | 251 | **53%** | **29%** | 1% | 8% |
| Greentech | 795 | 52 | 6% | 0% | 2% | 25% |
| Fanzetech 16-line | 775 | 205 | **50%** | 1% | 7% | 17% |
| Islam360 | 756 | 160 | 37% | 12% | 6% | 16% |
| Tarteel | 749 | 173 | 0% | **25%** | 20%* | 35% |
| Ayah | 779 | 97 | 1% | 0% | 0% | 10% |
| Taj 16-line | 648 | 170 | 8% | 1% | 5% | 12% (crashes dominate) |
| Holy Quran Offline (Jedy) | 771 | 184 | **53%** | 6% | 7% | 9% |
| Dawat-e-Islami | 769 | 88 | 2% | 1% | 1% | 22% |
| Al-Quran Free (ISYSWAY) | 755 | 146 | 32% | 3% | 7% | 18% |
| Quran with Urdu Translation | 780 | 343 | 46% | 7% | **26%** | 18% |
| Full Quran Sharif | 769 | 196 | 26% | 2% | **36%** | 23% |
| iQuran | 763 | 108 | 3% | 13% | 8% | 33% |
| Athan Pro | 739 | 317 | 26% | 17% | 0% | 5% (azan/notification issues dominate) |

\* For Tarteel, "mistakes" is mostly about the AI's mistake detection.

Representative quotes (abridged):
- *"When I opened the app and clicked on the Quran reading button I got a NSFW ad…"* (Ref, 1★, 2026-09-10)
- *"Every time I try to play downloaded audio it says to watch an ad… App automatically changes selected Qari to someone else and then asks to watch ad."* (Ref, 1★, 👍440)
- *"Its free because of ads but the developers should provide an option of buying an ad free version…"* (Fanzetech, 3★, 👍2,198)
- *"In actual 16 lines Quran [odd/even pages differ] like placement of Rukuh marks…"* (Fanzetech, 5★, 👍2,297)
- *"Can you please add the ability to view two pages side by side (like a real quran) in landscape…"* (Ayah, 5★, 👍152)
- *"Please add download button to download audio of all the paras at once."* / *"a separate media player tab… during driving"* (Dawat-e-Islami)
- *"Almost all the features require a premium subscription, which is unacceptable in a Quran program."* (Tarteel, 1★, 2026-02)
- *"We spend 1000s on clothes every year and this purchase costs less than an unbranded t-shirt."* (Ref, 5★ on paying for premium). Willingness to pay a small one-time amount exists.

**Feature requests** (916 request-style reviews out of 13,899): hifz/memorisation tools 69 · tafsir 64 · Urdu translation/audio 46 · zoom/font 46 · specific Qaris 34 · ayah highlight 33 · repeat/range 27 · tajweed colours 25 · khatm/goal tracking 25 · speed 25 · sync/backup 24 · two-page/landscape 21 · WBW 14 · Roman Urdu/transliteration 11 · resume audio position 10 · Para-wise audio 7.

**Praise drivers** (5★ text): Qaris/recitation, "easy/simple", "like a real Quran/page-turn", bookmarks/resume, clear font. Pakistan-specific apps (Fanzetech, Dawat-e-Islami) are also praised for "16 lines" and being "good for Hafiz".

**Rating noise:** many 1★ ratings carry purely positive text ("very nice", "بہت خوبصورت", "MashaAllah"). Pakistani users often misuse the star scale, so prompt for reviews only after positive moments (e.g., finishing a surah or a khatm) via the Play In-App Review API.

### 2.7 Monetization patterns

| Model | Who | User sentiment | Notes |
|---|---|---|---|
| Ads only | Fanzetech, Qamar 13-line, Golap 15-line | Most resented; "fear Allah" reviews | No way to pay to escape ads amplifies anger |
| Ads + "remove ads" IAP or subscription | Ref (Rs 280–6,900), Quran Majeed (Rs 105–180,800), Islam360 ("Sadqa-e-Jaria"), Taj (Rs 300–800), JedyApps, ISYSWAY, QuranReading | Tolerated when ads are light; fury when ads hit recitation or audio, or when premium doesn't restore | AdMob category blocks (Dating, Reference to sex, etc.) are **"best effort"**; Gambling is blocked by default but betting ads still reach the Ref app via other networks or creatives [verified: AdMob help 3150953] |
| Ads + subscription super-app | Muslim Pro ($12.99/mo or $34.99/yr US), Athan Pro, Wemu | Lowest ratings in the set (4.36); paywalled widgets and downloads resented | Feature sprawl: AI chat, video streaming, community, giving |
| Subscription, no ads, AI value | Tarteel (Rs 1,990–45,500 IAP range; **Alim program = pay-what-you-can**, reviewed within 48h) | Accepted for AI, but "everything premium" draws 1★ | Financial aid is a good-faith pattern for Muslim audiences [verified: support.tarteel.ai] |
| Freemium lite + one-time Pro | iQuran Lite / Pro (Rs 310) | Fine, but Lite limits annoy | Dated |
| Free, donation or charity | Greentech (UK charity 1178251, "Sadaqa Jariah" donate button), Quran.com (volunteer-funded servers), Dawat-e-Islami, Deeniyat Trust, iroshni, Ayah | **Highest ratings and loyalty** | Sustainability relies on a sponsoring org or donors |

**What users accept:** small one-time support or remove-ads purchases; donation and Sadqa-e-Jaria framing; premium for clearly expensive extras (AI, cloud) that leave the Quran, recitation and downloads free; financial-aid options.
**What users resent:** app-open ads; interstitials between pages or surahs; ads that interrupt or pause recitation; immodest, dating or betting creatives; rewarded ads to unlock *already-downloaded* audio; gating reciters; premium nags at launch; broken restore; surprise auto-renew ("I got a subscription I didn't opt for"); promotional notifications; app bloat.

### 2.8 White space: what Pakistani users ask for that nobody does well

| # | Opportunity | Evidence | Who comes closest | Gap |
|---|---|---|---|---|
| W1 | **All Indo-Pak editions (13/15/16-line) in one app**, switchable, with faithful layout (alternating margins, ruku "ع", manzil, sajdah, para-start marks) | Ref users ask for 13/15-line; Urdu "no Hafizi edition"; Dawat-e-Islami users ask for 13/15-line; Fanzetech users want alternating pages | Taj (separate apps, ads); Islam360 (15-line section) | No single, ad-free, maintained app |
| W2 | **Ayah-highlighted audio on Indo-Pak pages**, tap-ayah-to-play, follow-along page turning | Ref top review (👍4,399); Islam360 users want highlight in the 15-line section | Quran for Android Naskh (15-line only, separate app) | 16- and 13-line with highlight: none found |
| W3 | **Para-wise listening**: play or download a whole Para per Qari, a "media player" tab, Android Auto | Dawat-e-Islami reviews; "quran sharif para 1 to 30" autocomplete; tiny single-para apps (123Muslim "Para 1/2/3…", Jabir Ali "Para 30 with audio" at 100K+ each) | Quran Audio (surah-based), Dawat-e-Islami (partial) | Para/Manzil-based audio UX is absent |
| W4 | **Ad-free with dignified monetization** | "quran app without ads" autocomplete; ad complaints 50%+; ad-free apps have the best 1★ shares | Greentech, Quran.com, Dawat-e-Islami | None of these is Indo-Pak-mushaf-first plus Pakistani-audio-first |
| W5 | **Urdu-first understanding layer**: Urdu WBW, Urdu translation audio, translator choice by maslak, Roman Urdu | Urdu requests (46), Roman/transliteration (11), WBW (14); Islam360 shows demand for many translators | Islam360 (ads, 166 MB), Greentech (no Pakistani page focus) | Clean, light, mushaf-integrated Urdu layer |
| W6 | **Huffaz tools for South-Asian madrasa practice**: mark mistakes ("nishani") on the page, hide-text test, sabaq / sabqi / manzil revision scheduler, per-page memorisation status | Taj/Fanzetech/Dawat-e-Islami hifz requests; iroshni (abandoned 2020) had mistake marking | Tarteel (AI, Madani-centric, premium, online) | Offline, Indo-Pak, madrasa-vocabulary hifz tracker |
| W7 | **Group khatm / Quran Khwani** (assign paras to family or WhatsApp group for Isal-e-Sawab, track completion) | Very common Pakistani practice [unverified as a market metric]; Play search shows only tiny apps (Group Khatam Tracker 1K+, Khatam ul Quran 1K+) | — | Clear white space; needs a lightweight backend or share links |
| W8 | **Lightweight and offline-first** (low-end phones, expensive data) | Quran Majeed user wants a 16–18 MB lite version; Fanzetech praised for "single file"; offline in autocomplete | Q4A (13.5 MB), Ref (26 MB) | Per-edition on-demand packs + reciter packs per Para |
| W9 | **Verified text integrity** (no missing pages, proofread, "report an error") | Fanzetech missing pages; Full Quran Sharif 36% mistakes; Taj and Pakdata market proofreading | Taj (publisher), Pakdata ("proofread by Alim") | Transparent provenance page + checksum-verified page sets |
| W10 | **Manzil (protective verses) + Ayatul Kursi + Friday Kahf quick access** | Manzil dua apps 1M+ installs (123Muslim, Vibrant Solutions); Ref highlights popular surahs | Ref (popular surahs list) | Cheap to add; strong ASO term |

### 2.9 ASO insights for Pakistan

**Play search ranks (country=pk), top results [verified]**

| Query | #1 | #2 | #3 | Notes |
|---|---|---|---|---|
| Quran Pak | **Ref** | Fanzetech 16-line | Dawat-e-Islami | Quran Majeed #6, Greentech #8, Q4A #15 |
| Quran Majeed | Quran Majeed | **Ref** | Fanzetech | |
| quran / al quran / quran sharif | **Ref** | Quran Majeed / Greentech | — | Ref dominates generic terms |
| holy quran urdu | **Ref** | Quran Urdu (Dalfindo) | Greentech | |
| 16 lines Quran | Parenthesis "Quran, 16 Lines, Colored" | Fanzetech | iroshni | Ref absent from top 15 despite being 16-line, so its metadata doesn't target "16 line" |
| 15 line quran / 13 line quran | Nakagosoft / Qamar Apps | Deeniyat / Deeniyat | Islamian / Taj | Fragmented, mid-size apps |
| quran with urdu translation | Quran-Online | Sibtain Foundation | Dalfindo | |
| Para / parah | single-para apps (Jabir Ali, 123Muslim) | — | Ref #3 for "parah" | Low competition |
| quran mp3 | Mishary Full Quran MP3 | MP3 Quran | **Ref** | Single-reciter apps reach 1–5M installs |
| noorani qaida | many 100K–1M apps | | | Separate niche |
| hifz quran | Nakagosoft 15-line Hafezi | Tarteel | | |

**Google autocomplete, Pakistan (gl=pk) [verified]:**
- "quran p…": quran pak, quran pdf, quran pak pdf, quran pak download, quran pak ki tilawat, quran pak with urdu translation
- "quran app…": download, for pc, **with urdu translation**, **offline**, **without ads**, apk
- "quran majeed…": download, pdf, **30 para**, ki tilawat, with urdu translation, app
- "16 line quran…": pdf, download, pdf colour, online, color, pak, with color tajweed, app
- "para …": para 1, 2, 3, 30, 30 pdf, 4, 29, 5…
- "surah yaseen…": with urdu translation, ki tilawat, rehman · "tilawat…": surah rahman, quran pak, surah yaseen, surah baqarah, best voice, mp3, surah mulk

**ASO takeaways**
1. A bilingual title with Arabic is the norm among leaders ("Al QURAN – القرآن الكريم", "Quran Majeed – القران الكريم"). Pair "Quran Pak", "Quran Majeed" or "16 Line" with القرآن الكريم. Exact title length limits should be checked against current Play policy [unverified: 30-char title].
2. The Ref app ranks on generic terms but **not on "16 lines Quran"**. An app whose metadata covers *13, 15 and 16-line*, *Para 1–30*, *Hafizi*, *Tajweedi*, *with Urdu translation*, *offline*, *no ads* and *tilawat* can contest several mid-competition SERPs at once.
3. Put **Para names and numbers** ("Para 1 Alif Lam Meem… Para 30 Amma") and popular surahs (Yaseen, Rahman, Mulk, Kahf, Waqiah, Ayatul Kursi, Manzil) in the long description. Single-para apps prove the search demand.
4. Localise the listing to Urdu (ur-PK) with Nastaliq screenshots. Pakdata's Quran Pak 360 already uses a fully Urdu short description.
5. Screenshots should show the actual Indo-Pak page with ع markers, the Para index and the Qari picker. "No ads" should be a screenshot caption and the first line of the description.
6. Avoid keyword-stuffed package names (JedyApps style) and anything that mimics the Ref app's name or icon (Play impersonation policy).

### 2.10 Trust, regulatory and licensing notes relevant to competition
- **Indo-Pak page images are licensed assets, not commons.** Quran for Android credits: *"naskh images used with permission of SHL Info Systems"* and *"qaloon images used with permission of Nous Memes Editions"*. Its README says the data is typically *"CC BY-NC-ND"* and asks: *"Please keep use of this code for non-profit purposes only… the project is under the GPL 3 license, which requires that modifications to this code be open sourced as well."* [verified: raw.githubusercontent.com/quran/quran_android/main/README.md, LICENSE = GPL-3]. Taj Company sells its own digital editions, so its print layouts are its IP. **A clone must source Indo-Pak pages it is licensed to use** (to be resolved by the data/licensing researcher).
- **Pakistan law:** the Punjab Holy Quran (Printing and Recording) Act 2011 requires registration of publishers and recording companies and certification of specimens against the Quran Board's standard copy. Penalties are up to 3 years' imprisonment and/or a fine of at least Rs 20,000. The Act does not expressly mention apps or digital media [verified: wipo.int/wipolex/en/text/497207]. A 2019 LHC judgment ordered that copies be *"duly approved by the Quran Board before the publication in press and on the internet"*; in Nov 2021 the court pressed the government on compliance, and petitioners cited Google Play [verified: dawn.com/news/amp/1656850]. No evidence was found of apps being certified or blocked (a Malaysia precedent exists).
- Competitors use trust signals: "proofread by a Muslim Islamic scholar (Alim)" (Pakdata), "checked by certified Quran Proofreaders and found no errors" (Taj). Users punish errors hard.
- **Sectarian sensitivity:** Dawat-e-Islami's apps (Kanzul Iman / Sirat ul Jinan) serve a Barelvi audience. Islam360 deliberately offers translators across schools. A neutral default plus explicit choice avoids alienating segments.

---

## 3. Recommendations

1. **Position as "the ad-free Indo-Pak Quran: read like your printed Quran, listen to your Qari".** Make the mushaf and recitation experience better than the reference app, and do not compete as a super-app. *Rationale:* the reference app's #1 weakness is monetization behaviour (52% of complaints), and the ad-free incumbents (Greentech, Quran.com) are not Indo-Pak-mushaf-first. *Confidence: High.*
2. **v1 table stakes (must ship):**
   - 16-line Indo-Pak page mushaf (plus 15- and 13-line as licensing allows), with Para/Surah/Manzil/Ruku/Sajda/page/ayah navigation and Para names (Alif Lam Meem…)
   - Separate "khatm marker" vs browsing position; verse- and page-level bookmarks
   - Night/sepia modes; persistent zoom and landscape fit-width; rotation and page lock; keep-screen-on
   - Haptics off by default; open straight to the last page (no splash)
   - 4–8 famous Qaris (Alafasy, Sudais, Shuraim, Ghamdi, Muaiqly, Minshawi, Husary, Abdul Basit) for Surah, Para, ayah-range playback, with gapless background play, repeat counts, speed and sleep timer, and offline download per Surah or Para

   *Confidence: High.*
3. **v1.x differentiators, in order:**
   - (a) Ayah highlight and tap-to-play on Indo-Pak pages (needs per-page ayah coordinates)
   - (b) Para-wise "download all / play Para" plus a media-player tab and Android Auto
   - (c) A single app with 13/15/16-line switch and faithful alternating margins; two-page landscape spread for tablets and foldables
   - (d) Huffaz tools: on-page mistake marking, hide-text test, sabaq/sabqi/manzil revision tracker

   *Rationale:* each maps to repeated, high-👍 requests (Sections 2.4 and 2.8). *Confidence: Medium-High.*
4. **Urdu layer:** at least two Urdu translations from different schools with a neutral default (final pick subject to licensing), Urdu WBW, and an optional Roman Urdu/English transliteration toggle. Arabic+translation must remain an *optional* view; the mushaf page stays pure. *Confidence: Medium* (data licences gate this).
5. **Monetization: no ads, ever, and nothing Quranic behind a paywall.** Offer an optional one-time "Supporter / Sadqa-e-Jaria" purchase (e.g., Rs 300–1,000 tiers; validate via Play Billing price experiments) and later an optional low-cost "Plus" for cloud sync, extra themes and hifz analytics. Implement restore and entitlement caching correctly. Never auto-enrol trials. If a paid AI feature ever ships, add a Tarteel-style financial-aid path. *Confidence: High (no ads); Medium (exact tiers).*
6. **Keep it light:** target an APK under 25 MB and deliver mushaf editions and reciter audio as on-demand packs per edition, Para or Surah. Show storage per pack and allow moving or deleting it. *Rationale:* low-end Pakistani devices; the "lite version" request; Islam360 is 166 MB and Tarteel 257 MB. *Confidence: High.*
7. **Prayer times / Qibla / azkar: Phase 2, kept minimal and reliable.**
   - Defaults: Karachi method and Hanafi Asr for Pakistan [unverified: confirm against the prayer-times researcher]
   - Manual minute offsets, exact alarms, respect for silent/DND
   - No promo notifications
   - Quick-access "Daily" cards for Yaseen, Rahman, Mulk, Waqiah, Friday Kahf, Ayatul Kursi and Manzil

   *Rationale:* daily-routine value and parity with the reference app, but azan reliability is a top complaint generator. *Confidence: Medium.*
8. **Habit features:** khatm planner (30-day / Ramadan), streaks without shame, a resume widget, and a "Continue Para X" card. Later, **Group Khatm / Quran Khwani** via share link (assign 30 paras, track completion). *Confidence: Medium* (Group Khatm is unique but needs a small backend).
9. **Trust and integrity:**
   - Publish a provenance/credits screen (page set source, proofreading, licences, audio sources)
   - Verify page-set checksums on download; never silently change page images
   - Add an in-app "report a text error"
   - Before public launch in Pakistan, get legal advice on Punjab Quran Board approval

   *Confidence: Medium-High.*
10. **ASO plan:**
    - Title along the lines of "Quran Pak 16 Line – القرآن الكريم" (final copy to be tested)
    - Short description leading with "No ads · Offline · 13/15/16 line · Urdu"
    - ur-PK listing with Nastaliq screenshots; Para 1–30 names and popular surahs in the description
    - Target "16 line quran", "15 line quran", "13 line quran", "quran majeed 30 para", "quran pak with urdu translation", "quran app without ads", "tilawat"
    - Prompt for in-app review only after a completed surah or khatm milestone

    *Confidence: Medium.*
11. **Defer AI recitation recognition** (Tarteel-class) until after product-market fit. If explored, start with offline "follow-along page turning" rather than mistake grading. *Rationale:* high cost, accuracy complaints, and Tarteel's moat. *Confidence: Medium.*
12. **Use Quran for Android as a reference, not a code base, for a commercial app.** It is GPL-3 and its authors explicitly request non-profit use. Study its architecture and features (sync, highlight, Android Auto). Copying code would force GPL on the app and conflict with the authors' stated wishes if it is ever monetized. *Confidence: High.*

---

## 4. Risks and open questions

**Risks**
- **Page-image licensing (highest risk).** Indo-Pak 13/15/16-line layouts are publisher assets (Taj Company, SHL Info Systems via quran.com, Dawat-e-Islami's own). "Cloning" the reference app's scans would almost certainly infringe. Mitigations: license a page set, or render Indo-Pak pages from openly licensed text and fonts that mimic the line breaks (fidelity risk).
- **Quran-text and data licences** for translations, WBW and audio may be non-commercial or no-derivatives (CC BY-NC-ND is common), which constrains monetization.
- **Pakistani regulatory exposure:** the Punjab Quran Board approval requirement and the LHC "internet" order are ambiguous for apps; enforcement is unknown.
- **Sustainability without ads** depends on supporter purchases; there is a risk of under-funding audio CDN costs if we self-host. Prefer established CDNs (to be confirmed by the audio researcher).
- **Incumbent response:** the reference app (~71M installs) could drop ads or add 13/15-line editions; Quran.com could merge Naskh and add Urdu.
- **Measurement bias:** the review sample is English-language, regex-coded and partly "most relevant" (older) reviews. Theme percentages are directional.
- **Listing discrepancy:** the reference app's Play listing lacks the "Contains ads" label despite many ad reports. This is unexplained (possibly a declaration issue). Our app must declare accurately.

**Open questions (for the product owner)**
1. Which edition does the owner personally read: 16-line (what reviews say the reference app uses) or 13-line (the screenshot described as "~13 lines")? Page 2 of a 16-line mushaf has an ornamental opening and fewer lines. Please count the lines on a mid-Juz page.
2. Personal use first, or a public, monetized launch? This decides which CC BY-NC-ND data and GPL code are usable.
3. Which Urdu translators matter to you and your audience (e.g., Fateh Muhammad Jalandhari, Taqi Usmani, Kanzul Iman, Juna Garhi)? How should sectarian neutrality be handled?
4. Are prayer times and azan in scope for v1 (you use the app "daily"), or should the app stay pure Quran?
5. What budget is there for page-set licensing, audio hosting or CDN, and proofreading or Quran Board certification?
6. Is a Group Khatm / Quran Khwani feature (needs a small backend) desirable?

---

## 5. Sources

| URL | What was verified |
|---|---|
| https://play.google.com/store/apps/details?id=free.alquran.holyquran&hl=en&gl=pk | Reference app metadata (rating 4.64, 1,135,804 ratings, 50M+, updated 2026-10-14, v4.4.46, IAP Rs 280–6,900, description/feature list); listing HTML has no "Contains ads" string; reviews |
| https://play.google.com/store/apps/details?id=com.quran.labs.androidquran&hl=en&gl=pk | Q4A metadata, description, reviews |
| https://play.google.com/store/apps/details?id=com.quran.labs.androidquran.naskh&hl=en&gl=pk | 15-line Naskh (Indopak) app by quran.com; updated 2025-09-21 |
| https://play.google.com/store/apps/details?id=com.quranicaudio.app&hl=en&gl=pk | Quran.com "Quran Audio" (released 2026-08-18; features; no ads) |
| https://play.google.com/store/apps/developer?id=quran.com&hl=en | quran.com developer's app list (madani, naskh, qaloon, warsh, quranicaudio, haramain) |
| https://play.google.com/store/apps/details?id=com.pakdata.QuranMajeed&hl=en&gl=pk | Quran Majeed metadata, "Contains ads" label present, features (scripts, 15 Qaris, Wear OS, Hifz) |
| https://play.google.com/store/apps/details?id=com.pakdata.quranpak | Quran Pak 360 (Indo-Pak, Tafsir Usmani, 4 Urdu translations, AI search) |
| https://play.google.com/store/apps/details?id=com.bitsmedia.android.muslimpro | Muslim Pro features; Premium US $12.99/mo or $34.99/yr; downloads and widgets premium |
| https://play.google.com/store/apps/details?id=com.mmmoussa.iqra | Tarteel features, free vs Premium |
| https://play.google.com/store/apps/details?id=com.ayah | Ayah features, last update 2025-08-03 |
| https://play.google.com/store/apps/details?id=com.greentech.quran | Greentech features (Mushaf scripts incl. Naskh Indopak, WBW Urdu, 170+ translations, 80+ reciters, sync, planner) |
| https://play.google.com/store/apps/details?id=com.guidedways.iQuran and …iQuranPro | iQuran Lite limits; Pro paid Rs 310 |
| https://play.google.com/store/apps/details?id=com.islam360 | Islam360 features (Roman Urdu, translators, Qaida, Hadith), "Why Ads" statement |
| https://play.google.com/store/apps/details?id=com.fanzetech.holyquran and com.fanzetech.quran.pak | Fanzetech 16-line feature set; ads; "single file" |
| https://play.google.com/store/apps/details?id=quran.taj.tajquranapp (+ tajquran13line, 15line, 16linebw) | Taj Company apps; search by Para/Surah/Ayat/Ruku/Manzil/Sajda; proofreading claim; IAP |
| https://play.google.com/store/apps/details?id=com.dawateislami.alquranulkareem | Dawat-e-Islami app; no ads; para-wise index |
| https://play.google.com/store/apps/details?id=com.quanticapps.athan | Athan Pro features and monetization |
| Other Play listings (com.iroshni.*, com.deeniyat.*, quran.offline…, com.isysway.free.alquran, com.QuranReading.urduquran, com.fullquransharif…, com.quranmajeed.sixteenline, com.alquran.aiquran.quranlearning, com.fyxtech.muslim, com.athan, sa.edu.ksu.Ayat, com.golap.hefzquran, com.qamarapps.quran13line, com.paranthesis.quranpaktajweedi, com.tos.hafeziquran, Noorani Qaida apps) | Ratings, installs, update dates, ads/IAP flags |
| Play search, gl=pk (via google-play-scraper): "Quran Pak", "Quran Majeed", "16 lines Quran", "15 line quran", "13 line quran", "Para", "parah", "quran", "al quran", "holy quran urdu", "quran with urdu translation", "noorani qaida", "quran mp3", "hifz quran", "roman urdu quran", "khatam quran tracker", "sabaq sabqi manzil" | Keyword rankings in Pakistan |
| https://suggestqueries.google.com/complete/search?client=firefox&hl=en&gl=pk&q=… | Google autocomplete for "quran p", "quran app", "quran majeed", "16 line quran", "quran with urdu", "para", "surah yaseen", "quran sharif", "tilawat" |
| https://raw.githubusercontent.com/quran/quran_android/main/LICENSE | GPL-3.0 license |
| https://raw.githubusercontent.com/quran/quran_android/main/README.md | Image credits (SHL Info Systems for naskh), CC BY-NC-ND data note, non-profit request |
| git clone https://github.com/quran/quran_android (depth 1) | `feature/sync` (Quran.com account), `feature/autoquran` (Android Auto), bookmarks/search widget XML, `feature/recitation` |
| https://gtaf.org/ | Greentech is a UK charity (No. 1178251), donation-funded "Sadaqa Jariah" |
| https://support.tarteel.ai/en/articles/12414383-i-can-t-afford-the-premium-subscription-what-can-i-do | Tarteel Alim pay-what-you-can program, 48h review |
| https://support.google.com/admob/answer/3150953?hl=en | AdMob sensitive-category blocking is "best effort"; Dating etc. allowed by default; Gambling restricted by default |
| https://www.wipo.int/wipolex/en/text/497207 | Punjab Holy Quran (Printing and Recording) Act 2011: registration, certification against the standard copy, penalties; no explicit digital clause |
| https://www.dawn.com/news/amp/1656850 | LHC 2019 order: Quran Board approval "before the publication in press and on the internet"; 2021 compliance hearing; Google Play cited by petitioner |
| https://apkpure.com/x/&lt;package&gt; (redirects to app pages) | Approximate APK download sizes (Ref 26.2 MB, Q4A 13.5 MB, Quran Majeed 58.1 MB, Greentech 70.7 MB, Ayah 142.3 MB, Islam360 166.1 MB, Muslim Pro 97.5 MB, Tarteel 256.9 MB, Taj 16-line 45.9 MB, Dawat-e-Islami 66.4 MB, Fanzetech 67.3 MB, JedyApps 95.8 MB, iroshni 94.6 MB) |
| https://www.apkmirror.com (search result snippets) | Q4A release cadence 3.5.0 (Jun 2025) to 3.6.3 (Feb 2026) — search-snippet only |

*Raw working data (not part of the deliverable): /tmp/claude-0/-home-user-criterion/eece52d7-90a8-5227-b2fc-70627d430e45/scratchpad/r02-work/ (details_main.json, details_pk.json, reviews.json, searches.txt, requests.json).*
