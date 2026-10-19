# 11 — Release, Play Compliance & Business

**Purpose.** This doc covers everything needed to get the app from a working build to a trusted public listing on Google Play, keep it there, and keep it funded without ads or tracking. It covers: Play Console readiness (account, verification, signing, versioning); the brand and store listing in English and Urdu, with ASO; the Play policy checklist (Data safety, privacy policy, permission and FGS declarations, Families avoidance); Pakistani legal and religious compliance (gates G5 and G6); the launch plan and checklist; post-launch operations (text-error SLA, kill-switches, reviews, vitals, hotfixes); and the business model, with a cost budget, the G4 open-source licence framework, growth and KPIs that need no tracking. The audience is the owner (decisions, dates, money) and the engineer or AI agent who will implement the release pipeline and the compliance features.

**Status:** Draft v1 — 2026-10-19

**Key decisions in this doc**
- **D-17:** v1 is free with no ads, no IAP and no tracking SDKs. v1.5 adds an optional "Support" IAP (PKR 300 / 1,000 / 3,000, `play` flavor only) after a commercial-content audit. v2 makes premium only the features that have running costs, with a financial-aid path. Quran text, recitation, indexes and downloads are never gated.
- **D-19:** "Criterion" stays the codename and package (`com.chughtai.criterion`). The public brand is chosen at **G3**, after a trademark search; the working candidate is "Hamdam" (ہمدم). Title pattern: `<Brand>: Quran 16 Line & Audio`.
- **D-20:** Data safety says "No data collected" for v1. Opt-in ACRA in v1.1 is declared honestly. Reading history never leaves the device.
- **D-22:** Play **Organization** account via a free D-U-N-S, with a personal account plus a closed test (12 testers × 14 days) as the fallback. Target audience 13+. A proofreading certificate (G5) is required per layout. No translation-only mode. Content governance is written down. Pakistani legal advice is gate G6.
- **D-18:** The code licence decision is gate **G4** (GPL-3.0-or-later or proprietary). §10 gives the decision framework. The recommendation is GPL-3.0-or-later with DCO and `TRADEMARKS.md`, unless the owner commits to B2B white-labelling.
- **D-12 / D-10 / D-24:** Every content item carries eligibility flags (`personal` / `beta` / `public`, plus `commercialOk`). Per-pack and per-recitation **kill-switches** live in signed files on R2. Release builds are gated on these flags in CI.
- **D-25:** Closed test opens as early as **2026-12-28** (no later than 2027-01-11). Go/no-go is **2027-01-25**. Production staged rollout runs 2027-02-01 → 100 % by about 2027-02-05, before 1 Ramadan 1448 (≈ 8–9 Feb). If any gate is still open, the app stays in closed testing through Ramadan and goes public after Eid.
- **D-15 / D-16 / D-14 / D-13:** Permissions are exactly as in §4.4: `SCHEDULE_EXACT_ALARM` only (never `USE_EXACT_ALARM`), coarse location only, no full-screen intent, a `mediaPlayback` FGS, UIDT on API 34+ and a `dataSync` worker on API ≤ 33, and `AD_ID` stripped.

---

## 0. Release path at a glance

```mermaid
gantt
  dateFormat YYYY-MM-DD
  title Release path to Ramadan 1448 (owner timeline)
  section Account and brand
  Entity registration and D-U-N-S request     :a1, 2026-10-20, 2026-11-23
  Account-type decision point                 :milestone, m0, 2026-11-23, 0d
  Play Console account and app created        :a2, 2026-11-23, 2026-11-30
  G3 trademark search and brand pick          :a3, 2026-11-09, 2026-12-07
  Designer icon, feature graphic, frames      :a4, 2026-12-07, 2027-01-04
  section Gates
  G1 mushaf-data decision                     :milestone, g1, 2026-11-19, 0d
  G6 counsel engaged to written opinion       :g6, 2026-11-16, 2027-01-18
  G5 hafiz proofreading passes 1 and 2        :g5, 2026-12-07, 2027-01-25
  G4 licence decision                         :milestone, g4, 2027-01-11, 0d
  G2 public catalogue frozen                  :milestone, g2, 2027-01-18, 0d
  section Tracks
  Internal track (owner daily use)            :t1, 2026-12-21, 2027-02-01
  Closed test (beta channel, 20+ testers)     :t2, 2026-12-28, 2027-02-01
  Open test (public channel, only if gates clear) :t3, 2027-01-18, 2027-02-01
  Go/no-go                                    :milestone, gng, 2027-01-25, 0d
  Production staged rollout                   :t4, 2027-02-01, 2027-02-06
  Ramadan change freeze (hotfix only)         :t5, 2027-02-08, 2027-03-10
```

| Date | Milestone | Owner action | Gate |
|---|---|---|---|
| 2026-10-20 → 26 | Choose entity (§1.1); start SECP SMC-Pvt or sole-proprietor registration; request a D-U-N-S; buy the domain; set up Cloudflare Email Routing (`support@`, `privacy@`); generate the upload key (§1.4) | Owner | — |
| ≤ 2026-11-02 | All permission emails sent (Tarteel/QUL, DK author, Alafasy Berhad, QF, King Fahd Complex); mp3quran statement archived (PDF + SHA-256 + date) | Owner | G1, G2 |
| 2026-11-16 | Pakistani counsel engaged with the §5.2 brief | Owner | G6 |
| 2026-11-19 | G1 decision: Tarteel permission, or start the clean-room layout (D-06) | Owner | G1 |
| 2026-11-23 | **Account-type decision point:** if the D-U-N-S and entity are not both in hand, create a personal account the same day | Owner | — |
| 2026-11-30 | Play Console account verified; app created (package reserved); internal track configured | Owner | — |
| 2026-12-07 | G3 brand chosen; designer briefed; hafiz proofreading pass 1 starts on exported pages | Owner | G3, G5 |
| 2026-12-21 | Internal track live; the owner uses the app daily (end of P1) | Eng | — |
| 2026-12-23 | Privacy policy live; App content declarations complete (§4); closed-test listing drafted | Owner + Eng | — |
| 2026-12-28 | **Closed test opens** with ≥ 20 testers, beta channel (personal account: the 14-day clock starts) | Owner | — |
| 2027-01-04 | Final EN/UR listing copy, screenshots and feature graphic | Owner + designer | — |
| 2027-01-11 | G4 licence decided. Personal account: 14 days reached, apply for production access | Owner | G4 |
| 2027-01-18 | G6 written opinion received; G2 public catalogue frozen; open test starts if G1, G2, G3 and G6 are clear | Owner | G2, G6 |
| 2027-01-25 | G5 certificate signed for the release-candidate content hash. **Go/no-go** (§7.2). Production release submitted with managed publishing | Owner | G5 |
| 2027-02-01 | Production rollout starts (§7.3) | Owner | — |
| ≈ 2027-02-05 | 100 % rollout | Owner | — |
| ≈ 2027-02-08 → ≈ 2027-03-10 | Ramadan 1448 change freeze: hotfixes only (§8.6). Eid ≈ 2027-03-10 [estimate]. This is also the fallback public-launch window | — | — |

Gate dates G3–G6 above are this doc's proposal; [Decision log, gates & risks](12-decisions-risks.md) is authoritative.

---

## 1. Play Console readiness

### 1.1 Account type (D-22)

**Decision: Organization account**, owned by a registered legal entity with a free D-U-N-S number (r09 §2.6). Reasons:
- New personal accounts need **12 testers opted in continuously for 14 days** before production access (r09 §2.6). Our pre-Ramadan timeline cannot absorb that comfortably.
- A personal account that monetizes **shows the full home address publicly** (r09 §2.4). That would bite at v1.5 (IAP).
- Organization accounts display legal name, address, email and **phone**. Use a dedicated business number, not a personal one.

| Step | Detail | Done by |
|---|---|---|
| 1 | **Entity:** register an **SMC-Private Limited** with SECP (one-member company; separates the app's liability and payouts from the person). Fallback: a sole proprietorship with an FBR NTN in the business name. Have an accountant confirm before filing (r09 open Q6) | 2026-10-26 (filed) |
| 2 | Request a **D-U-N-S** for the entity (free). Lead time is unknown; assume up to ~30 days [unverified] | 2026-10-26 (requested) |
| 3 | Business bank account (USD receipts later), business email `support@<domain>`, a dedicated phone number | 2026-11-16 |
| 4 | Create the **Play Console Organization account** (US$25 one-time, r09 §2.6), then verify identity, D-U-N-S and website | 2026-11-23 → 30 |
| 5 | **Fallback (triggered 2026-11-23 if steps 1–2 are incomplete):** create a **personal** account. Recruit ≥ 20 closed testers (buffer over 12), open the closed test by 2026-12-28, and apply for production access on day 15. Keep it unmonetized. **Before v1.5**, transfer the app to an Organization account (Play supports app transfers [unverified procedure; check the Console help at the time]) | 2026-11-23 |

**Developer name:** the brand (e.g. "Hamdam Apps") after G3, never anything resembling "9D Muslim Apps", "Quran.com", "Official" or a reciter's name (r09 §2.1). **Contact on the listing:** `support@<domain>`, website `https://<domain>`, and the business phone (Organization only).

### 1.2 Domain, email and web presence (D-24)

| Item | Decision |
|---|---|
| Domain | The brand's `.com` (or `.app` if `.com` is taken), bought the day G3 clears. Until then, buy a neutral holding domain only if needed for the Console. Register the matching `.pk` if available [cost unverified] |
| Website | Static site on **Cloudflare Pages** at `https://<domain>/`: landing page, `/privacy`, `/ur/privacy`, `/credits`, `/report` (how to report a text error), `/delete-data` (from v1.x) |
| CDN | R2 bucket on `https://cdn.<domain>/`: `catalogue.json`, `index.json`, packs, timing packs, the PK month-start feed and `notices.json`, all Ed25519-signed (D-10). **No request logging, no Logpush, no Web Analytics** on either hostname. The aggregate R2 request counts in the dashboard are fine (§12) |
| Email | Cloudflare Email Routing forwards `support@`, `privacy@`, `permissions@` and `abuse@` to the owner's inbox. The owner's personal Gmail is never shown publicly |

### 1.3 Android developer verification (r09 §2.6)

| Date | What happens | Our action |
|---|---|---|
| 2026-09-30 | Enforcement starts in Brazil, Indonesia, Singapore and Thailand | None for Play installs |
| 2027 (global; exact date unknown) | Apps installed outside Play must come from a verified developer with a registered package and key | If the `foss` flavor is distributed (G4), register `com.chughtai.criterion` plus the **foss signing-key SHA-256** in the Android Developer Console before global enforcement. Check quarterly (§8.7) |
| Ongoing | Play apps are auto-registered (≈ 99 %) | After creating the app: Play Console → Home → confirm that the developer-verification status shows the app as registered. Screenshot this into the release log |

### 1.4 App signing, keys and versioning

**Play App Signing with a Google-generated app signing key** (the default for new apps: it allows key upgrade, and a lost upload key can be reset).

| Key | Spec | Storage | Used by |
|---|---|---|---|
| Upload key `upload.jks` | RSA 4096, validity 30 years, alias `upload` | (1) Password-manager attachment, (2) encrypted USB kept offline, (3) GitHub Actions secrets `UPLOAD_KEYSTORE_B64`, `UPLOAD_KEYSTORE_PASSWORD`, `UPLOAD_KEY_ALIAS`, `UPLOAD_KEY_PASSWORD`, in a protected `release` environment that needs manual approval | CI on `v*` tags → `bundlePlayRelease` |
| App signing key | Google-held | Play | Play delivery |
| Foss key `foss.jks` | RSA 4096, 30 years | Same three places, separate secrets `FOSS_*` | `assembleFossRelease` (only if G4 distributes foss) |
| Content signing key (Ed25519) | For `index.json`, `catalogue.json`, `notices.json`, the PK month-start feed (D-10) | Offline. The private key **never** enters CI; files are signed on the owner's machine by a `tools/` script ([Data pipelines](08-data-pipelines.md)) | Owner |

Consequence: Play and foss builds have different signatures, so a user cannot switch channels in place. They switch by reinstalling and moving their data with JSON export/import (F-BM-06).

**Versioning** (cross-doc assumption for [Quality, testing & CI](10-quality-testing-ci.md)):
- `versionName` = SemVer `MAJOR.MINOR.PATCH`, with a `-beta.N` suffix for beta-channel builds.
- `versionCode = MAJOR*1_000_000 + MINOR*10_000 + PATCH*100 + BUILD` (BUILD 0–99). So 1.0.0 RC builds are 1000000–1000099. Each Play upload needs a fresh code, and this leaves room for many uploads per version.
- The same `versionCode` is used for `play` and `foss` builds of one release.
- R8 mapping files travel inside the AAB, so Play vitals stack traces are deobfuscated.
- Every release tag records: the `quran_content.db` content version and SHA-256, the font SHA-256, and the G5 certificate ID that covers them (§5.4).

**Release channel** is a compile-time `BuildConfig.RELEASE_CHANNEL ∈ {personal, beta, public}`, set by CI per artifact (§6). **Never promote a beta-channel AAB to production.** Production candidates are built as `public` and soak on the closed track for at least 3 days first.

### 1.5 App content declarations (Play Console → Policy → App content)

| Declaration | v1.0 answer | Changes at |
|---|---|---|
| Privacy policy URL | `https://<domain>/privacy` (HTML, public, not geofenced, no PDF; r09 §2.6) | — |
| App access | "All functionality is available without special access" (no login) | v1.x sync: still yes (login optional) |
| Ads | **No, my app does not contain ads** | Never changes (D-17) |
| Content rating | IARC questionnaire per §3.7 → expected Everyone / PEGI 3 / "Rated for 3+" | Re-submit at v1.5 (digital purchases) and v1.x (group khatam user interaction) |
| Target audience and content | Age groups **13–15, 16–17, 18+** only; "appeals to children unintentionally": **No** | — |
| News app | No | — |
| Data safety | §4.2 v1 form: **No data collected** | v1.1 (ACRA), v1.x (sync, group khatam) |
| Advertising ID | **No** (and `AD_ID` is stripped in the manifest) | Never |
| Government app | No | — |
| Financial features | None | — |
| Health | None | — |
| Foreground service permissions | `mediaPlayback` + `dataSync`, with videos (§4.4.2) | Android Auto (P3) adds nothing new |
| Exact alarm | No declaration form: we use `SCHEDULE_EXACT_ALARM`, not the restricted `USE_EXACT_ALARM` (r09 §2.6) | — |
| Full-screen intent | Not declared or used | — |
| Account deletion | N/A: no accounts in v1 (r09 §2.6) | v1.x (§4.6) |

---

## 2. Brand and gate G3 (D-19)

**Rules** (r09 §2.1–2.2):
- Never use the reference app's name "Al QURAN – القرآن الكريم", its icon, colours or trade dress, screenshots or description text.
- Never put "Official", "#1" or "Best", a reciter's name, Saudi/Haramain/KFGQPC logos or a government emblem in the title or icon.
- Use "Quran" and "القرآن الكريم" only as generic descriptors.
- "Criterion" is not a public brand (collides with Criterion Collection/Games and others; zero search relevance in Pakistan). It may appear as an English tagline ("Al-Furqan · The Criterion") on the About screen.

**G3 process** (target decision 2026-12-07; owner runs it with a trademark agent):

| # | Step | Pass criterion | Evidence kept |
|---|---|---|---|
| 1 | Shortlist 3 names: **Hamdam (ہمدم)** first, plus 2 alternates. Avoid Furqan, Rafiq, Noor and Tilawah, which are crowded (r09 §2.2) | Easy to say in Urdu and English; no unintended meaning (check with 2 Urdu speakers and 1 alim) | Notes |
| 2 | Play search collision check (country pk, us, gb, sa, ae) for `<name>`, `<name> quran` and the Urdu spelling | No Quran or Islamic app with the same or a confusingly similar name | Screenshots + date |
| 3 | **IPO-Pakistan** trademark search, classes **9** (software) and **41** (education/religious content services); 42 optional | No identical or similar live mark in 9/41 | Agent's search report |
| 4 | **WIPO Global Brand Database** search (Madrid filings covering PK, GCC, UK, US) | No conflicting mark in classes 9/41 | Export |
| 5 | **USPTO** trademark search | No conflicting live mark in 9/41 | Export |
| 6 | Domain (`.com` / `.app` / `.pk`) and social handles (YouTube, Facebook, X, Instagram) available | At least the `.com` or `.app` | Registrar receipt |
| 7 | Record the decision as a new ADR in [Decision log](12-decisions-risks.md) | — | ADR |
| 8 | **File** an IPO-Pakistan application in classes 9 + 41 at launch + 90 days, or at 10k installs, whichever comes first. File immediately if a conflicting app appears | — | Filing receipt |

If Hamdam fails steps 2–5, take the first alternate that passes. The doc set uses `<Brand>` everywhere until G3 clears.

---

## 3. Store listing (EN default + Urdu)

Listing languages: **English (en-US, default)** and **Urdu (`ur`, served to ur-PK users)**. Do not add an Arabic listing in v1. Every claim in the listing must map to a feature ID that is **shipped in the build being released** (§3.8). This is the reverse of the reference app, whose listing claims translation, WBW, tafsir, Qibla and azkar that its APK lacks (r01 §2.1).

### 3.1 Title (≤ 30 characters)

| Option | Text | Chars | Use |
|---|---|---|---|
| **A (decision)** | `Hamdam: Quran 16 Line & Audio` | 29 | en-US title (D-19 pattern) |
| A-ur | `ہمدم – القرآن الكريم 16 سطری` | 28 | Urdu title: bilingual القرآن الكريم + "16 line" |
| B | `ہمدم: قرآن پاک 16 سطری و تلاوت` | 30 | Urdu alternate if "Quran Pak" outperforms in a store-listing experiment |
| C | `Hamdam – القرآن الكريم 16 Line` | 30 | en-US experiment arm (bilingual, as the leaders do; r02 §2.9) |

After launch, run Play **store listing experiments** (no SDK needed): A vs C on en-US, A-ur vs B on Urdu, 50/50, for at least 14 days. Pick the winner on retained first-time installers (or installers, if retention isn't shown). Substitute the G3 brand for "Hamdam" in every option.

### 3.2 Short description (≤ 80 characters)

| Lang | Text | Chars |
|---|---|---|
| EN | `No ads, no tracking. 16-line Quran Pak, Qari audio with highlight, Para & Urdu` | 78 |
| EN alt | `Ad-free 16-line Quran Pak: read page by page, listen by Surah, Para or Ayah` | 75 |
| UR | `بغیر اشتہار: 16 سطری قرآن پاک، قاری کی تلاوت، پارہ و منزل فہرست، اردو ترجمہ` | 75 |

### 3.3 Full description: English (3,174 chars; limit 4,000)

Replace `<Brand>` and the proofreader placeholder at release. Drop the "DAILY COMPANION" block if the scope-cut ladder ([Roadmap](09-roadmap-backlog.md)) removes companion features from v1.0. Generate the Para-name line from the D-07 Para table (`name_roman`) so spellings match the app.

```text
No ads. No tracking. Free forever.

<Brand> is an Indo-Pak Quran Pak (Quran Majeed / Quran Sharif) for your daily tilawat. Read the 16-line mushaf page by page, just like your printed Quran, and listen to your favourite Qari by Surah, by Para, by Manzil or from any ayah.

READ LIKE YOUR PRINTED QURAN
• 16-line Indo-Pak mushaf, 548 pages, with the same page numbers as the common printed 16-line Quran (pages 2 to 549)
• Typeset in the open-licence DigitalKhatt IndoPak font: sharp at any zoom, no blurry scans
• Ruku (ع), Manzil, Sajda and Para markers where you expect them
• Tap any ayah to play from there, bookmark, copy, share or see its translation
• Light, sepia, night and black themes; screen stays on; rotation lock and page lock

LISTEN TO YOUR FAVOURITE QARI
• Play a whole Surah, a whole Para (Indo-Pak Para boundaries), a Manzil, a Ruku, a page or any range of ayahs
• The ayah being recited is highlighted on the page, and the page turns by itself
• Repeat an ayah or a range for hifz, pause between ayahs, speed 0.5x to 2x, sleep timer
• Plays in the background, with lock-screen and headset controls
• Download a Surah, a Para or the whole Quran for offline listening (Wi-Fi only by default)
• Reciters include Mishary Alafasy, Saad Al-Ghamdi, Abdul Basit Abdus Samad (Mujawwad), Abdur-Rahman As-Sudais, Abu Bakr Ash-Shatri, Hani Ar-Rifai, Muhammad Ayyub and more. Every reciter is free.

FIND ANY PLACE QUICKLY
• Para index with start page and progress: Alif Lam Meem, Sayaqool, Tilkar Rusul, Lan Tanaloo, Wal Muhsanat, La Yuhibbullah, Wa Iza Samiu, Wa Lau Annana, Qalal Malao, Wa'lamu, Ya'tazirun, Wa Ma Min Dabbah, Wa Ma Ubarri'u, Rubama, Subhanallazi, Qal Alam, Iqtaraba, Qad Aflaha, Wa Qalallazina, Amman Khalaq, Utlu Ma Uhiya, Wa Man Yaqnut, Wa Maliya, Faman Azlamu, Ilaihi Yuraddu, Ha Meem, Qala Fama Khatbukum, Qad Sami Allah, Tabarakallazi, Amma (Para 1 to 30)
• Surah index, Manzil index (7 manzils), Ruku and Sajda lists, go to any page or Surah:Ayah
• Quick access: Surah Yaseen, Rahman, Mulk, Waqiah, Kahf, Ayatul Kursi, Manzil dua, last two ayat of Al-Baqarah
• Search in Arabic without harakat, and in Urdu or English

UNDERSTAND
• Urdu translation by Maulana Muhammad Junagarhi with Ahsan-ul-Bayan notes, and English Saheeh International, always shown with the Arabic text
• App in Urdu or English

DAILY COMPANION
• Prayer times (Karachi method, Hanafi Asr by default) with adhan, Qibla direction and Hijri date
• Sehri and Iftar timetable for Ramadan, tasbeeh, morning and evening azkar
• Khatam planner, private reading progress, continue-reading widget

YOUR PRIVACY
No ads, no analytics, no trackers, no account. Bookmarks and reading history stay on your phone. Works offline.

SOURCES AND ACCURACY
Quran text: Tanzil Project (CC BY 3.0). Font: DigitalKhatt IndoPak (SIL Open Font License). Translations: QuranEnc.com. Full credits are inside the app. The Arabic text was proofread by <Hafiz/Qari name, credentials>. If you find a mistake, tap "Report text error" in the app; we aim to correct confirmed text errors within 48 hours.

<Brand> is an independent app and is not affiliated with any other Quran app, publisher or reciter.
```

Listing rules:
- **No publisher names** (e.g. "Taj Company") in the listing; the in-app Credits name the layout source (r09 §2.5 item 7). This avoids implied affiliation (Impersonation).
- **Reciter names** appear only in the description (factual, allowed), only for public-shortlist reciters (D-12), and never in the title or graphics.
- **15- and 13-line editions are not mentioned** until F-RD-11 ships (P3). Then they are added to the title and keywords experiment (§3.5).

### 3.4 Full description: Urdu (2,852 chars)

```text
کوئی اشتہار نہیں۔ کوئی ٹریکنگ نہیں۔ ہمیشہ مفت۔

<برانڈ> روزانہ تلاوت کے لیے انڈو پاک قرآن پاک (قرآن مجید / قرآن شریف) ہے۔ 16 سطری مصحف کو بالکل اپنے چھپے ہوئے قرآن کی طرح صفحہ بہ صفحہ پڑھیں، اور اپنے پسندیدہ قاری کی تلاوت سورت، پارہ، منزل یا کسی بھی آیت سے سنیں۔

چھپے ہوئے قرآن جیسا مطالعہ
• 16 سطری انڈو پاک مصحف، 548 صفحات، صفحہ نمبر وہی جو عام چھپے ہوئے 16 سطری قرآن میں ہیں (صفحہ 2 تا 549)
• کھلے لائسنس والے DigitalKhatt IndoPak فونٹ میں کمپوز شدہ: ہر زوم پر صاف، دھندلی اسکین تصاویر نہیں
• رکوع (ع)، منزل، سجدہ اور پارہ کی علامات اپنی جگہ پر
• کسی بھی آیت پر ٹیپ کریں: یہاں سے سنیں، بک مارک، کاپی، شیئر یا ترجمہ
• لائٹ، سیپیا، نائٹ اور سیاہ تھیم؛ اسکرین آن رہتی ہے؛ اسکرین گھمانے اور صفحہ پلٹنے کا لاک

اپنے پسندیدہ قاری کی تلاوت
• پوری سورت، پورا پارہ (انڈو پاک پاروں کے مطابق)، منزل، رکوع، صفحہ یا آیات کی کوئی بھی حد
• جو آیت پڑھی جا رہی ہو وہ صفحے پر نمایاں ہوتی ہے اور صفحہ خود بخود پلٹ جاتا ہے
• حفظ کے لیے آیت یا حصہ دہرائیں، آیات کے درمیان وقفہ، رفتار 0.5x سے 2x، سلیپ ٹائمر
• بیک گراؤنڈ میں تلاوت، لاک اسکرین اور ہیڈسیٹ سے کنٹرول
• سورت، پارہ یا پورا قرآن آف لائن سننے کے لیے ڈاؤن لوڈ کریں (ڈیفالٹ: صرف وائی فائی)
• قراء میں مشاری العفاسی، سعد الغامدی، عبدالباسط عبدالصمد (مجوّد)، عبدالرحمٰن السدیس، ابوبکر الشاطری، ہانی الرفاعی، محمد ایوب اور دیگر شامل ہیں۔ تمام قراء مفت ہیں۔

فوری رسائی
• پاروں کی فہرست، ابتدائی صفحہ اور پیش رفت کے ساتھ: الم، سیقول، تلک الرسل، لن تنالوا، والمحصنٰت، لا یحب اللہ، واذا سمعوا، ولو اننا، قال الملا، واعلموا، یعتذرون، وما من دآبۃ، وما ابرئ، ربما، سبحٰن الذی، قال الم، اقترب، قد افلح، وقال الذین، امن خلق، اتل ما اوحی، ومن یقنت، ومالی، فمن اظلم، الیہ یرد، حٰم، قال فما خطبکم، قد سمع اللہ، تبٰرک الذی، عمّ (پارہ 1 تا 30)
• سورتوں کی فہرست، منازل کی فہرست (7 منزلیں)، رکوع اور سجدات، کسی بھی صفحے یا سورت:آیت پر جائیں
• فوری رسائی: سورہ یٰسین، رحمٰن، ملک، واقعہ، کہف، آیت الکرسی، منزل، سورہ بقرہ کی آخری دو آیات
• عربی (اعراب کے بغیر)، اردو اور انگریزی میں تلاش

ترجمہ
• مولانا محمد جوناگڑھی کا اردو ترجمہ مع احسن البیان حواشی، اور انگریزی میں صحیح انٹرنیشنل، ہمیشہ عربی متن کے ساتھ
• ایپ اردو یا انگریزی زبان میں

روزمرہ ساتھی
• نماز کے اوقات (کراچی طریقہ، حنفی عصر بطور ڈیفالٹ) اذان کے ساتھ، قبلہ رخ اور ہجری تاریخ
• رمضان کے لیے سحری و افطاری نظام الاوقات، تسبیح، صبح و شام کے اذکار
• ختم القرآن منصوبہ، نجی پیش رفت، "پڑھنا جاری رکھیں" ویجیٹ

آپ کی پرائیویسی
کوئی اشتہار، اینالیٹکس، ٹریکر یا اکاؤنٹ نہیں۔ بک مارکس اور مطالعے کی تاریخ صرف آپ کے فون پر رہتی ہے۔ ایپ آف لائن کام کرتی ہے۔

ماخذ اور درستی
قرآنی متن: Tanzil Project (CC BY 3.0)۔ فونٹ: DigitalKhatt IndoPak (SIL Open Font License)۔ تراجم: QuranEnc.com۔ مکمل حوالہ جات ایپ میں موجود ہیں۔ عربی متن کی تصحیح <حافظ/قاری کا نام، اسناد> نے کی ہے۔ کوئی غلطی نظر آئے تو ایپ میں "متن کی غلطی رپورٹ کریں" پر ٹیپ کریں؛ تصدیق شدہ غلطی 48 گھنٹوں میں درست کرنے کی کوشش کی جاتی ہے۔

<برانڈ> ایک آزاد ایپ ہے اور کسی دوسری قرآن ایپ، ناشر یا قاری سے منسلک نہیں۔
```

A native Urdu editor proofreads this text, and every Urdu screenshot caption, before 2027-01-04. The Para names must match `para.name_ur` in the content DB character for character.

### 3.5 ASO keyword plan (Pakistan)

Play indexes the title, short description and full description; there is no keyword field. Put each target term in naturally, **2–5 times at most** (Play metadata policy forbids repetitive keywords; r09 §2.1). Ranks below are the competitive picture on 2026-10-19 (r02 §2.9).

| Priority | Keyword cluster (EN / Roman Urdu / Urdu) | Intent / competition (r02) | Placement | Honest from |
|---|---|---|---|---|
| P1 | **16 line quran**, 16 lines quran, ۱۶ سطری قرآن | Reference app **absent** from top 15; mid competition | Title (both), short (both), full ×2 | v1.0 |
| P1 | **quran pak**, quran majeed, quran sharif, قرآن پاک، قرآن مجید | Reference #1; high volume | Title (A-ur/B), first line of full | v1.0 |
| P1 | **without ads / no ads / ad free**, بغیر اشتہار | Autocomplete "quran app without ads" (r02 §2.9); our wedge | Short (both), first line of full | v1.0 |
| P1 | **para 1 … para 30**, quran majeed 30 para, پارہ | Single-para apps prove demand; low competition | Full (Para-name list), short ("Para") | v1.0 |
| P1 | **quran pak with urdu translation**, اردو ترجمہ | Autocomplete; mid competition | Short (UR), full | v1.0 |
| P2 | **offline** quran, آف لائن | Autocomplete | Full ×2 | v1.0 |
| P2 | **tilawat**, quran pak ki tilawat, تلاوت | Autocomplete ("tilawat surah rahman/yaseen") | Full, Urdu title B | v1.0 |
| P2 | surah yaseen / rahman / mulk / kahf / waqiah, ayatul kursi | High-volume surah queries | Full (quick-access line) | v1.0 |
| P2 | **manzil** (mostly means the Manzil dua: 1M+ apps), منزل | We honestly offer both the Manzil index and the Manzil dua quick access | Full ×2 | v1.0 |
| P2 | quran mp3 / audio / qari | Single-reciter apps reach 1–5M | Title ("Audio"), full | v1.0 |
| P3 | **15 line quran, 13 line quran, hafizi** | Fragmented, mid-size apps | Title experiment + full | When F-RD-11 ships (P3) |
| P3 | hifz quran | Nakagosoft, Tarteel | Full | When F-CP-12 ships (P3) |
| Never | Reciter names in the title; "official"; competitor names; keyword-stuffed strings | Policy (r09 §2.1) | — | — |

Measure monthly with the Play Console acquisition report: search terms by installers and store-listing conversion [check the report names in the Console]. No SDK is needed.

### 3.6 Graphics

**Specs** (Play; re-check in the Console when uploading):
- App icon: 512×512 PNG, 32-bit.
- Feature graphic: **1024×500**, PNG/JPEG, no alpha.
- Phone screenshots: 2–8 per language. Each side 320–3840 px, and the long side at most twice the short side, so export at **1080×1920 (9:16)**. Do **not** use 1080×2400 (ratio 2.22).

**Icon brief** (designer, original work, provenance logged):
- An abstract geometric or arabesque motif, or the Urdu brand initial, on a deep green or teal field. This is our own palette, chosen to differ from the reference app's trade dress.
- No photos, no Kaaba, no open-book stock art, no text other than the brand initial.
- Adaptive icon (foreground + background) plus a monochrome layer for themed icons.

**Feature graphic** (ASCII layout):

```
+--------------------------------------------------------------------------------+
|  [icon]  <Brand>                         +-------------------------------+     |
|          القرآن الكريم · 16 سطری           |  crop of OUR rendered page   |     |
|          Quran 16 Line & Audio           |  (Surah Yaseen opening,     |     |
|                                          |  ruku ع in margin, one ayah |     |
|          No ads · Offline · Urdu         |  highlighted)                |     |
|                                          +-------------------------------+     |
+--------------------------------------------------------------------------------+
```

**Screenshot storyboard** (8 frames). These are real app renders from a scripted screenshot run: an instrumented test with a demo seed, on an emulator at 1080×1920, light theme except frame 2. Every Arabic page shown must come from the G5-certified content hash. There are no reciter photos and no fake ratings or testimonials. Urdu-listing screenshots use the Urdu UI.

| # | Screen and state | EN caption (headline / sub) | UR caption | Features |
|---|---|---|---|---|
| 1 | Reader, opening page of Surah Yaseen; toolbar "Para 22 · Page L · Yaseen"; ruku ع in margin | Read like your printed Quran / 16-line Indo-Pak · 548 pages · No ads | چھپے ہوئے قرآن جیسا / 16 سطری انڈو پاک · 548 صفحات · بغیر اشتہار | F-RD-01, F-RD-02 |
| 2 | Night theme; ayah highlighted mid-recitation; mini-player "Alafasy · Surah Mulk" (text only) | Your favourite Qari, ayah by ayah / Highlight follows the recitation | پسندیدہ قاری، آیت بہ آیت / تلاوت کے ساتھ آیت نمایاں | F-RD-05, F-AU-03 |
| 3 | Para index with Indo-Pak names, start pages, progress rings, Read + Play buttons | Para 1–30 with names / Read or play any Para | پارہ 1 تا 30 ناموں کے ساتھ / کوئی بھی پارہ پڑھیں یا سنیں | F-IX-02 |
| 4 | Play-scope sheet: Surah / Para / Manzil / Range, repeat ×3, pause between ayahs | Play a Surah, Para or any ayahs / Repeat for hifz · speed · sleep timer | سورت، پارہ یا آیات سنیں / حفظ کے لیے دہرائیں · رفتار · ٹائمر | F-AU-02, F-AU-05, F-AU-06 |
| 5 | Translation view: Arabic + Urdu Junagarhi + English Saheeh | Urdu & English translation / Always with the Arabic text | اردو اور انگریزی ترجمہ / ہمیشہ عربی متن کے ساتھ | F-RD-08, F-TR-01 |
| 6 | Manzil index + quick-access chips (Yaseen, Mulk, Ayatul Kursi, Manzil dua) | Manzil, Ruku, Sajda and quick access / Yaseen · Mulk · Kahf · Ayatul Kursi | منزل، رکوع، سجدہ اور فوری رسائی / یٰسین · ملک · کہف · آیت الکرسی | F-IX-03, F-IX-04, F-IX-06, F-IX-08 |
| 7 | Prayer times card + Sehri/Iftar countdown (Ramadan demo date), Qibla thumbnail | Prayer times, Qibla, Sehri & Iftar / Karachi method · Hanafi Asr | نماز کے اوقات، قبلہ، سحری و افطاری / کراچی طریقہ · حنفی عصر | F-CP-01, F-CP-03, F-CP-07 |
| 8 | Downloads/storage screen + "No ads. No tracking." banner + Credits/proofread line | Offline, private, free forever / No ads · No tracking · Proofread text | آف لائن، محفوظ، ہمیشہ مفت / بغیر اشتہار · بغیر ٹریکنگ · مصحح متن | F-AU-08, F-PL-04 |

If companion features are cut from v1.0, frame 7 becomes the bookmarks/khatam planner (F-BM-02/F-BM-04) or search (F-SR-01).

### 3.7 Category, content rating and target audience

- **Category:** Books & Reference (the same as the reference app and most Quran readers; r01 §2.1). Tags: pick the closest to "Religion" and "Reference" from the Console list.
- **Contains ads:** No. **In-app purchases:** No (v1.0). The IAP label appears automatically at v1.5.
- **IARC content rating questionnaire** (category "Reference, News, or Educational"; question wording from memory, so answer what the Console actually asks):

| Topic | v1.0 answer |
|---|---|
| Violence, blood, fear/horror | No |
| Sexuality, nudity | No |
| Profanity / crude humour | No |
| Controlled substances (drugs, alcohol, tobacco) | No |
| Gambling (real or simulated) | No |
| Hate speech / discrimination | No. Religious scripture and translations only, under a content-governance policy (§5.5) |
| Users can interact or exchange content | **No** (v1.0 has no UGC, chat or social features; sharing goes through the Android share sheet). **Yes** from realtime group khatam (F-CP-11, P3) |
| Shares the user's current location with other users | No (location is used on the device only) |
| Purchase of digital goods | **No** in v1.0; **Yes** at v1.5 (F-PL-08) |
| Unrestricted internet access (web browser) | No |

  Expected result: Everyone / PEGI 3 / USK 0 / "Rated for 3+" in PK (matches the reference app).
- **Target audience:** 13–15, 16–17, 18+. Under-13 groups stay **unticked** so the app is outside the Families programme (D-22; r09 §2.6). See §4.5.

### 3.8 Listing integrity rules (release-blocking)

1. **Claims audit:** CI keeps `store/listing-claims.yaml`, a map from each listing bullet to its feature IDs. A release script checks that every listed feature ID is marked shipped in that build's feature manifest (cross-doc assumption with [Roadmap](09-roadmap-backlog.md) and [CI](10-quality-testing-ci.md)). Any mismatch blocks promotion.
2. Store the listing text in the repo under `store/` (`fastlane/metadata/android/{en-US,ur}/` layout, which F-Droid can reuse), reviewed in PRs.
3. No emojis, ALL CAPS words, rankings, prices, promotions, testimonials or competitor names (r09 §2.1).
4. Write "What's new" text in both languages (≤ 500 chars each) for every production release.

---

## 4. Play policy compliance

### 4.1 Policy checklist

| Policy | Requirement (source) | Our implementation | Evidence / owner |
|---|---|---|---|
| Impersonation | Don't mislead with a similar name, icon or description (r09 §2.1) | G3 brand; original icon; independence statement in the listing | G3 record; designer provenance file |
| Intellectual property | No copyrighted scans, audio or text without rights; no "professional images of public figures" (r09 §2.1) | Typeset mushaf (D-05); content provenance log; **no reciter photos**; mp3quran grant archived; permission emails logged | `content/provenance.csv` (§5.5); permissions log |
| Repetitive content / spam | No "same experience as other apps"; **one app only**, never per-surah clones (r09 §2.1) | Unique value: ayah-aware typeset Indo-Pak mushaf, Para/Manzil/Ruku playback, highlight, no tracking | Review notes field (§7.4) |
| Minimum functionality | Not a static text or PDF viewer | Interactive reader + audio + indexes + search | — |
| Webview spam | No wrapper of quran.com etc. | Native Compose; no WebView for content | — |
| Metadata | ≤ 30-char title; no emojis, caps, rankings or testimonials (r09 §2.1) | §3 | `store/` PR review |
| User Data / privacy policy | Policy URL in the Console **and in the app** (r09 §2.6) | Settings → About → Privacy; onboarding footer link | §4.3 |
| Data safety | Mandatory, accurate, including SDKs (r09 §2.6) | §4.2; SDK inventory + network-host allowlist test (§4.7) | CI |
| Permissions | Minimum scope; contextual requests | §4.4 | Manifest review |
| Foreground services | Declare types + video per type (r09 §2.6) | §4.4.2 | YouTube unlisted links |
| Exact alarms | `USE_EXACT_ALARM` only for core alarm/calendar apps (r09 §2.6) | `SCHEDULE_EXACT_ALARM` only, with an inexact fallback | Manifest; [Companion](07-companion-engagement-features.md) |
| Full-screen intent | Only for call/alarm core apps | Not used; `tools:node="remove"` | Manifest |
| Families | If children are targeted: certified SDKs only, no AAID or location | Target 13+; no child-directed copy or art (§4.5) | Target-audience form |
| Ads | Interstitial rules etc. | **No ads ever** (D-17) | Ads declaration "No" |
| Payments | Digital goods via Play Billing; no tax-exempt donation via Billing; no external payment links (r09 §2.6) | v1: none. v1.5: Play Billing "Support" consumables. **No donation links** unless the recipient is an FBR-approved NPO pre-cleared with Play | §9.2 |
| Account deletion | Only if accounts can be created (r09 §2.6) | N/A in v1; §4.6 for v1.x | — |
| Target API | API 36 since 2026-08-31 (r07 §2.8) | targetSdk 36; plan 37 in P3/P4 | Gradle |
| 16 KB page size | From **2027-02-01**, updates must support 16 KB pages if they have native code (r07 §2.8) | Only `libsqliteJni.so`, already aligned `0x4000`; CI `zipalign -c -P 16` | CI (release-blocking, falls in our launch week) |
| Location button policy, contacts, geofencing FGS removal | Effective 2027-01-27 (r09 §2.6) | Not affected (coarse one-shot, no contacts, no geofencing). Re-read the final texts on 2027-01-27 | §8.7 |

### 4.2 Data safety form

**v1.0 (P2; no SDKs, no accounts).** Play Console → Data safety:

| Question | Answer | Why it is true |
|---|---|---|
| Does your app collect or share any of the required user data types? | **No** | No analytics, ads, attribution, crash or Firebase SDKs (D-20). Location, bookmarks, reading history, tasbeeh and khatam data are processed **on device only**, and on-device processing is not disclosed (r09 §2.6). Content downloads send no identifiers (see below). |
| Result shown on listing | "No data collected · No data shared" | — |

Engineering conditions that keep the v1 answer true. They are release-blocking and tested in [CI](10-quality-testing-ci.md):
1. **No third-party SDK** that transmits data. The `play` flavor's only Play libraries are in-app review (F-PL-10) and in-app update, which run through the Play Store app.
2. **Requests carry no identifiers:**
   - no installation ID, advertising ID or device ID;
   - no account or user-specific query parameters;
   - the User-Agent is `<Brand>/<versionName> (Android <sdk>)` only.
3. **A host allowlist test** (debug OkHttp interceptor + unit test) fails if the app contacts any host outside this list: `cdn.<domain>`, mp3quran audio hosts, the everyayah / islamic.network per-ayah hosts (beta channel only until G2), and the Play Store (via the Play libraries). The privacy policy lists exactly these hosts.
4. **No server-side logs:** R2 and Pages have no access logging, Logpush or Web Analytics. Cloudflare's own security processing is disclosed in the policy as an infrastructure provider.
5. **Text-error reports and support** use `ACTION_SENDTO` (the user's own email app) or the Android share sheet, so the user sends them; our app transmits nothing (F-PL-05). The privacy policy says that if you email us, we receive what you send.
6. **Android Auto Backup** covers only `user.db` + settings (D-20). This is the platform backup, not collection by us; it is disclosed in the policy. [Re-verify the Play guidance for Auto Backup when filling the form.]

**v1.1 (P3; opt-in ACRA, F-PL-09).** ACRA shows a "Send crash report?" dialog on each crash. On consent, it POSTs via `HttpSender` to `https://crash.<domain>/r`, a Cloudflare Worker that drops the client IP and stores the JSON in a private R2 bucket with **90-day retention**. The dialog shows a report ID that the user can quote to request deletion.

| Data type | Collected | Shared | Optional | Purpose | Encrypted in transit | Deletion |
|---|---|---|---|---|---|---|
| App info & performance → **Crash logs** | Yes | No | **Yes** (per-crash consent) | Analytics (crash diagnosis) | Yes | By email with the report ID; auto-deleted after 90 days |
| App info & performance → **Diagnostics** (device model, OS, app version, content version) | Yes | No | Yes | Analytics | Yes | Same |

Fields that must never appear in a crash payload are enforced by an ACRA `ReportField` allowlist: reading position, bookmarks, location, account data, logcat (unless filtered of all of these).

**v1.x / v2 (forward look; final answers belong to [Companion](07-companion-engagement-features.md)):**

| Feature | Data types to declare | Notes |
|---|---|---|
| F-BM-07 QF sync (PKCE, data stored by Quran Foundation) | App activity (bookmarks, notes, reading sessions); User IDs | Optional, app functionality. Explicit separate consent for "religious practice data" (r09 §2.7). QF privacy requirements apply |
| F-CP-11 realtime group khatam (`play` flavor, Firestore) | Name (display name only); User IDs (Firebase anonymous UID); App activity (khatam progress) | Optional. Firebase acts as a service provider (not "shared"). Auto-expiry; report/leave |
| v2 premium (subscription) | Purchase history (via Play) | Play Billing handles payment data |

### 4.3 Privacy policy (outline; publish at `/privacy` and `/ur/privacy` by 2026-12-23)

1. **Who we are:** the legal entity name, address (Organization account), `privacy@<domain>`, and the effective date.
2. **Summary:** "We don't collect, sell or share your personal data. No ads, no analytics, no trackers, no accounts."
3. **Data on your device:**
   - bookmarks, notes, last-read pointer, khatam plans, tasbeeh counts, settings;
   - a rounded city coordinate for prayer times and Qibla, computed on device (adhan2, offline GeoNames city list).
   - None of it leaves the device, except through Android backup (item 6) or a file you export yourself.
4. **Network requests:** content and audio come from:
   - our CDN `cdn.<domain>` (Cloudflare R2);
   - mp3quran.net audio servers;
   - (beta) everyayah.com and cdn.islamic.network.

   These servers see your IP address and the file requested, as any web server does. Link each host's privacy policy. We keep no request logs. Requests carry no identifiers.
5. **Permissions and why:**
   - approximate location: once, when you tap "Use my location" (or pick a city instead);
   - notifications: adhan, reminders, download progress;
   - alarms and reminders: exact adhan times;
   - foreground services: recitation playback and downloads;
   - user-initiated data transfer jobs: downloads on Android 14+;
   - boot completed: re-arming alarms.
6. **Backups:** Android Auto Backup of `user.db` and settings to your Google account, controlled by your device settings; exclusions; JSON export/import (F-BM-06).
7. **Contacting us:** if you email a text-error report or a support request, we receive your email address and what you send. We use it only to reply and fix the issue, and delete it on request.
8. **Optional crash reports (v1.1):** what is sent, consent per crash, 90-day retention, deletion by report ID.
9. **Children:** the app is not directed to children under 13.
10. **Your rights:** access and deletion on request (we hold nothing by default). We follow GDPR-style principles for EU, UK and Gulf users (r09 §2.4).
11. **Changes:** versioned; changes are announced in "What's new".

Update triggers: any new host, SDK, permission or data type. The policy is updated **before** the build that introduces the change is released.

### 4.4 Permissions and declarations

#### 4.4.1 Manifest (v1.0; extends r09 §2.6 / r07 §2.8)

```xml
<manifest xmlns:tools="http://schemas.android.com/tools">
  <uses-permission android:name="android.permission.INTERNET"/>
  <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
  <uses-permission android:name="android.permission.WAKE_LOCK"/>                       <!-- Media3 playback -->
  <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>              <!-- runtime, API 33+, contextual -->
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK"/><!-- recitation + full adhan (D-13, D-16) -->
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC"/>    <!-- WorkManager download worker, API <= 33 only (D-14) -->
  <uses-permission android:name="android.permission.RUN_USER_INITIATED_JOBS"/>         <!-- UIDT downloads, API 34+ -->
  <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM"/>            <!-- user-granted; NOT USE_EXACT_ALARM -->
  <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>          <!-- re-arm adhan/reminders only -->
  <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>          <!-- one-shot, foreground -->
  <!-- defensive removals: fail CI if any of these reappear in the merged manifest -->
  <uses-permission android:name="com.google.android.gms.permission.AD_ID" tools:node="remove"/>
  <uses-permission android:name="android.permission.ACCESS_ADSERVICES_AD_ID" tools:node="remove"/>
  <uses-permission android:name="android.permission.ACCESS_ADSERVICES_ATTRIBUTION" tools:node="remove"/>
  <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" tools:node="remove"/>
  <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" tools:node="remove"/>
  <uses-permission android:name="android.permission.USE_EXACT_ALARM" tools:node="remove"/>
  <uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" tools:node="remove"/>
  <uses-permission android:name="android.permission.ACCESS_NOTIFICATION_POLICY" tools:node="remove"/>
  <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" tools:node="remove"/>
  <application>
    <!-- WorkManager's FGS host only ever runs dataSync on API <= 33 -->
    <service android:name="androidx.work.impl.foreground.SystemForegroundService"
             android:foregroundServiceType="dataSync" tools:node="merge"/>
  </application>
</manifest>
```

CI check `verifyMergedManifest` (assumption for doc 10):
- Parse `build/intermediates/merged_manifest/*/AndroidManifest.xml` for `playRelease` and `fossRelease`.
- Fail on any permission outside the allowlist above, or on any removed permission reappearing.

| Permission | When it is requested | User-facing explainer (EN; Urdu in the UI spec) | If denied |
|---|---|---|---|
| `POST_NOTIFICATIONS` | When the user turns on an adhan, a reminder (F-CP-09) or the first download. **Not** at first launch, and not for playback (media-session notifications are exempt) | "Allow notifications so we can play the adhan and show download progress." | Adhan and reminders are off, with a banner; downloads still run |
| `SCHEDULE_EXACT_ALARM` | After the user enables an adhan; deep-link `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` | "To call the adhan at the exact minute, allow 'Alarms & reminders'. Without it, Android may delay it by several minutes." | Inexact `setWindow` fallback plus a persistent "Adhan may be late" banner (D-16) |
| `ACCESS_COARSE_LOCATION` | Tap "Use my location" in onboarding or prayer settings. A city picker is always offered first or alongside | "Your approximate location is used once, on this phone, to calculate prayer times and the Qibla. It is never sent anywhere." | City picker |
| `RUN_USER_INITIATED_JOBS` | Install-time (normal permission) | — | — |
| FGS types | Install-time | — | — |

#### 4.4.2 Foreground-service declarations (Play Console → App content → Foreground service permissions)

| Type | Feature description (paste) | Impact if deferred or interrupted (paste) | Video |
|---|---|---|---|
| `mediaPlayback` | "Plays Quran recitation that the user starts from the mushaf page, the Surah/Para/Manzil indexes or the player (Surah, Para, ayah range, repeat). Playback continues with the screen off and is controlled from the media notification, lock screen and headset. The same service plays the full adhan at the prayer time the user enabled; the user can stop it from the notification." | "Recitation would stop when the screen turns off or the user switches apps, breaking listening and memorisation sessions; the adhan the user configured would not play at prayer time." | Script A |
| `dataSync` | "Downloads Quran recitation audio that the user explicitly requests (a Surah, a Para or the whole Quran) for offline listening, on Android 13 and lower only. A progress notification with Cancel is shown. On Android 14+ the app uses user-initiated data transfer jobs instead of this service." | "A user-requested download of tens to hundreds of MB would stall when the app goes to the background, wasting the user's mobile data and leaving incomplete files." | Script B |

**Demo video scripts.** Each is 30–45 s, recorded on a real device or emulator with screen-record and touches visible, using the release-candidate build. Upload to YouTube as **unlisted**. No commentary is needed; captions are burned in English.

*Script A — mediaPlayback (Android 15/16 device):*
1. (0–5 s) Open the app → Surah index.
2. (5–10 s) Tap ▶ on Surah Al-Mulk. The caption reads "User starts recitation".
3. (10–15 s) The page shows the ayah highlight. Press Home and the media notification appears.
4. (15–22 s) Lock the screen, show lock-screen controls, pause and resume.
5. (22–30 s) Unlock → Settings → Prayer times → enable adhan for a test prayer time set 1 minute ahead. The caption reads "User enables adhan".
6. (30–42 s) At the set time the adhan notification appears and the adhan plays. Tap Stop. The caption reads "Adhan stopped by user".

*Script B — dataSync (Android 13 device, API 33):*
1. (0–6 s) Open Para index → Para 30 → "Download".
2. (6–10 s) The caption reads "User starts download". Choose reciter → Download.
3. (10–20 s) Press Home. The progress notification shows % and a Cancel action.
4. (20–28 s) Reopen the app → Storage manager shows the download in progress.
5. (28–35 s) The download completes. The notification is dismissed and the Para shows "Downloaded".

Re-record when the flow changes visibly, and at minimum once per major version.

### 4.5 Families policy avoidance

- Target audience 13+ only. No "ideal for kids" copy (the reference app has it; r09 §2.6).
- No cartoon characters, mascots or child-focused imagery in the icon, screenshots or UI.
- No under-13 testimonials.
- Ages are never asked for or inferred.
- **Noorani Qaida (F-CP-13, P4):** design it for "learners of all ages" with adult-neutral visuals. If the product wants child-directed design, ship it as a **separate Families-compliant app** (r09 §2.6). This is decided in P4 planning.
- Hifz record-yourself (F-CP-12, P3) keeps audio on the device. Any AI spike with children's voices needs guardian consent (r08 open Q10) and stays out of the shipped app until reviewed.

### 4.6 Account deletion (from v1.x)

v1 has no accounts, so this is N/A. When F-BM-07 (QF sign-in) or F-CP-11 (Firebase anonymous auth) ships:
- (a) in-app Settings → Account → "Disconnect and delete synced data" (QF: revoke tokens + link to QF account deletion; Firebase: delete the UID's documents + `deleteUser()`);
- (b) a web page `https://<domain>/delete-data` explaining both paths, with an email request option. Both links are entered in the Console (r09 §2.6).

### 4.7 SDK and dependency inventory

`compliance/sdk-inventory.yaml` lists every runtime dependency with: licence, whether it does network I/O, whether it handles identifiers, and flavors. CI diffs it against `./gradlew :app:dependencies` for `playRelease` and `fossRelease`. Any new artifact fails the build until the inventory (and, where needed, the Data safety answers and privacy policy) are updated in the same PR. **Banned outright:**
- ad SDKs, AppsFlyer/Adjust and other attribution SDKs, Firebase Analytics/Crashlytics/Performance, Facebook SDK (all flavors);
- any GMS/Firebase artifact in `foss`;
- play-services-location (D-15).

---

## 5. Pakistan legal and religious compliance

> Not legal advice. Gate **G6** obtains a written opinion from Pakistani counsel before public launch.

### 5.1 Punjab Holy Quran (Printing and Recording) Act 2011: what we do

| Provision (r09 §2.4) | Our action | Where |
|---|---|---|
| s.2(h): a "record of the Holy Quran" includes recorded recitation, so **audio is in scope** | Use only recordings from recognised reciters via documented sources (D-12). Keep provenance. Ask counsel whether streaming makes us a "recording company" (§5.2 Q4) | [Audio engine](06-audio-engine.md), provenance log |
| s.5(3): comparison with the standard copy by a qualified person (Moulvi Fazil, or a qualified Qari who is a Hafiz), with a certificate of no error | **G5 proofreading certificate per layout** (§5.4), shown in Credits and the listing | §5.4 |
| s.5(6): no translation without its Arabic text | **No translation-only mode** anywhere (D-09). Translation view, copy and share (F-RD-08, F-RD-04, F-CP-10) always include the Arabic. Translation audio (F-AU-10) interleaves with the Arabic recitation | [UX spec](02-ux-ui-spec.md); release test |
| s.6: literal distortion; s.7: translation or interpretation by a non-Muslim contrary to Muslim belief (applies "whether or not registered") | Content-governance policy (§5.5): recognised Muslim scholars only; **no Ahmadi translations or tafsir**; sect-balanced Urdu set; Shia options labelled and opt-in | [Content & licensing](03-content-data-licensing.md) |
| s.9: up to 3 years' imprisonment and/or a fine of at least Rs 20,000 | Treat the app as in scope. Quran Board approval is a counsel question (§5.2 Q2) | G6 |
| Quran Board mandate covers "electronic digital devices"; a reported 2019 LHC order required approval before "uploading onto the internet" (r09 §2.4, r02 §2.10) | Ask counsel whether to seek a **voluntary Punjab Quran Board review** before or soon after launch | G6 |

### 5.2 G6: counsel brief (engage by 2026-11-16, written opinion by 2027-01-18)

Send counsel: this section, the content provenance log, the Credits screen draft, the content-governance policy, the privacy policy and the listing text. Questions:

1. Does the Punjab Act 2011 apply to a free mobile app published on Google Play by an entity registered in `<owner's province>`, and used by people in Punjab? Do **publisher or recording-company registration** duties apply?
2. Is **Quran Board approval** required before publishing a digital mushaf (the 2019 LHC order)? What are the process, fee, timeline and required specimen (APK, page PDFs, audio)? Is a voluntary review advisable, and is it possible after launch?
3. Does our certificate (§5.4) satisfy s.5(3)? What qualifications must the proofreader hold (Moulvi Fazil, Hafiz-Qari, institution)? Is a madrasa or Board stamp needed?
4. Audio: does streaming or offering downloads of third-party recordings make us a "recording company" under s.2? Do the recordings need certification?
5. Translations: does always showing the Arabic satisfy s.5(6) in the translation view, copy, share-as-image and translation-audio features?
6. Do equivalent laws exist in Sindh, KP, Balochistan or ICT, or federally?
7. **PECA 2016 / Removal and Blocking of Unlawful Online Content Rules:**
   - our exposure if a complaint alleges an "unauthentic" text (PTA's 2020 removal precedent, r09 §2.4);
   - the correct response procedure to a PTA or Google notice.
8. Exposure for **unintentional textual errors** under the Pakistan Penal Code's Quran-related offences [section numbers unverified; counsel to confirm]. Do the certificate, the 48-hour correction SLA and the kill-switch amount to adequate diligence?
9. Copyright:
   - public-domain status in Pakistan (life + 50, Copyright Ordinance 1962 s.18) of the Junagarhi, Kanz-ul-Iman, Jalandhari and Mahmud-ul-Hasan editions (death and publication dates per r05);
   - the risk from foreign terms, given that Play distributes globally;
   - the evidentiary weight of mp3quran's written host grant.
10. Entity and money:
    - SMC-Pvt vs sole proprietor for liability;
    - tax treatment of Play USD payouts (from v1.5);
    - PSEB registration;
    - whether a "Support / Sadqa-e-Jaria" IAP raises any charity or donation regulation.
11. Trademark: registrability of the G3 name in classes 9/41.
12. Data protection: any enacted Pakistani data-protection law at launch time, and the obligations of a "no data collected" app.
13. A terms-of-use and disclaimer review: prayer-time accuracy, Qibla, and "translations are human interpretations".

**G6 passes** when there is a written opinion with no "must not launch" finding, or with findings that we have implemented. If counsel says Quran Board approval is mandatory before launch, the public launch waits for approval (§7.2 fallback).

### 5.3 PTA risk and response playbook

| Trigger | Response | SLA |
|---|---|---|
| Google Play policy or legal-removal notice citing PTA or "unauthentic Quran" | (1) Acknowledge; (2) send counsel the notice; (3) if the claim names specific text, apply an errata banner or layout kill-switch (§8.2) and fix; (4) reply to Google with the G5 certificate, provenance log and governance policy | Acknowledge 24 h; substantive reply 72 h |
| Public complaint (social media, review) alleging sectarian bias or wrong text | Treat as a text-error report (§8.1) if specific; otherwise reply with the governance policy and the translator-choice explanation (templates §8.4) | 48 h |
| App blocked in Pakistan | Counsel-led response; keep the website up with the certificate and provenance; no circumvention advice to users | — |

Preventive measures:
- Neutral defaults (Junagarhi + Saheeh; translator choice from P2, F-TR-03).
- No sectarian content in notifications or events.
- Hijri observances follow the neutrality list in [Companion](07-companion-engagement-features.md).
- Keep a design-provenance log (our own sketches, fonts, data) as evidence against impersonation claims (r09 risk 10).

### 5.4 G5: proofreading certificate (per layout)

**Who:** a Hafiz who is also a qualified Qari, or a Moulvi Fazil (s.5(3) practice), with a sanad or institutional affiliation. The owner recruits locally (madrasa or masjid network) by 2026-11-30. An **honorarium** is paid (§9.6); the proofreader is credited by name in Credits with their consent.

**What is proofread:** the exact pages the app renders:
- The pipeline exports all 548 pages from our renderer at 2× (PNG + PDF booklet), stamped with the page label, content-DB SHA-256 and font SHA-256 ([Mushaf renderer](05-mushaf-rendering-engine.md), [Data pipelines](08-data-pipelines.md)).
- The reference copy is a purchased printed 16-line Taj Company mushaf (2 copies).

| Pass | Dates | Scope | Output |
|---|---|---|---|
| 1 | 2026-12-07 → 2027-01-04 | All 548 pages, line by line: every word, harakat, waqf mark, ayah marker (incl. «۵» marks), ruku triples, sajda labels, Para/Manzil/quarter markers, surah headers, basmala, page labels | `proofreading/errata-taj16.csv` (page_label, line, word_pos, found, expected, severity) |
| Fix | rolling | Engineering fixes; re-export affected pages | New content hash |
| 2 | 2027-01-05 → 2027-01-25 | (a) every page changed since pass 1; (b) a random 10 % of unchanged pages; (c) Fatiha, p.529 (QUL misorder fix), the 72:16 and whitelisted words (D-06) | Signed certificate |

Effort estimate: 548 pages at ~5–8 min/page ≈ 45–75 h for pass 1, and 10–20 h for pass 2 [estimate]. The proofreader commits about 15–20 h/week in Dec–Jan.

**Certificate template** (bilingual, signed and stamped, scanned to PDF; the SHA-256 is recorded in the release log):

> **Certificate of Proofreading / تصدیق نامۂ تصحیح**
> I, <name>, <Hafiz / Qari / Moulvi Fazil>, <institution, sanad>, certify that I have compared the Arabic text of the **Indo-Pak 16-line mushaf (548 pages, labels 2–549)** as rendered by <Brand> version <x.y.z> (content `<sha256-12>`, font `<sha256-12>`) against the printed 16-line Taj Company edition, and that, after the corrections listed in the attached errata, the text, ayah markers, ruku, sajda, Para and Manzil references do not contain any error known to me.
> Date / Signature / Stamp / Contact.

**Validity rule (release-blocking CI check):**
- The pipeline emits a per-page render hash.
- `certified_pages.json` lists the hashes covered by the current certificate.
- Any page whose hash differs blocks the `public` channel build until it is re-proofread and covered by a **certificate addendum**.
- Font, layout or DB changes that touch no page hash need no addendum.

Each further layout (15-line, 13-line in P3; Madani in P4) needs its own certificate before public release (D-04, D-22).

### 5.5 Content governance (summary; full policy in [Content & licensing](03-content-data-licensing.md))

- Translations and tafsir come only from recognised Muslim scholars.
- The Urdu set is sect-balanced:
  - Ahl-e-Hadith: Junagarhi (default);
  - Barelvi: Kanz-ul-Iman;
  - Deobandi: Mahmud-ul-Hasan/Usmani;
  - neutral: Jalandhari.
- Shia translations are labelled and opt-in, never the default. No Ahmadi translations or tafsir.
- No AI-generated tafsir or fatwa.
- No hasanat counters, shaming streaks or manipulative countdowns (r09 §2.7, QF connected-apps ethics).
- **Provenance log** `content/provenance.csv` (assumption for doc 03/08), one row per shipped content file:
  `file, source_url, licence, version, retrieved_at, sha256, eligibility, commercialOk, attribution, reviewer, notes`.
- **A named content reviewer (an alim)** signs off new translation, tafsir and azkar packs before `public` eligibility. The hafiz (G5) covers the Arabic mushaf.

---

## 6. Content eligibility and channel gating (D-10, D-12, D-17)

Every pack, recitation and bundled content file carries flags. They are set in the content pipeline and repeated in the signed `index.json` / `catalogue.json` (field names are an assumption for [Architecture](04-architecture.md) and [Data pipelines](08-data-pipelines.md)):

```kotlin
enum class Eligibility { PERSONAL, BETA, PUBLIC }        // ordered: PERSONAL < BETA < PUBLIC
enum class ReleaseChannel { PERSONAL, BETA, PUBLIC }     // BuildConfig.RELEASE_CHANNEL

data class ContentFlags(
    val eligibility: Eligibility,      // minimum audience the content is cleared for
    val commercialOk: Boolean,         // usable in a monetized build (v1.5+ play flavor)
    val fossOk: Boolean,               // allowed in the foss flavor (no GMS-bound delivery)
    val licence: String,               // SPDX-like id or "QuranEnc-terms", "mp3quran-host-grant", ...
    val attribution: String,           // exact credit line (Credits screen, player info)
    val killSwitch: Boolean = false,   // remote disable (signed index/catalogue)
)

/** PUBLIC channel accepts only PUBLIC content; BETA accepts BETA + PUBLIC; PERSONAL accepts all. */
fun ContentFlags.allowedIn(channel: ReleaseChannel, monetized: Boolean, foss: Boolean): Boolean {
    if (killSwitch) return false
    val channelOk = when (channel) {
        ReleaseChannel.PUBLIC -> eligibility == Eligibility.PUBLIC
        ReleaseChannel.BETA -> eligibility != Eligibility.PERSONAL
        ReleaseChannel.PERSONAL -> true
    }
    return channelOk && (!monetized || commercialOk) && (!foss || fossOk)
}
```

The same predicate runs in two places: in CI over bundled assets and the default catalogue (build time), and on the device over the remote `index.json` / `catalogue.json` (runtime filter, so a PUBLIC build never lists BETA packs).

| Channel | Distribution | May include | Examples (from D-06/D-09/D-12) |
|---|---|---|---|
| `PERSONAL` | Sideload to the owner's devices, or the internal track **with a tester list of only the owner's own accounts** | Everything with a provenance row | Shamshad Ali Khan Urdu voice, Husary Muallim, Qari Sadaqat Ali; QUL layout before G1 |
| `BETA` | Internal + closed tracks (invited testers) | `BETA` + `PUBLIC` | Gapped everyayah / islamic.network per-ayah sets (`PENDING`); QUL layout while G1 has **no reply** (closed testers see a "pre-release, not yet proofread" banner until G5) |
| `PUBLIC` | Open testing + production; foss distribution | `PUBLIC` only | mp3quran gapless shortlist (D-12), Tanzil text, QuranEnc Junagarhi + Saheeh, DK font, our metadata tables, QUL layout **after G1 permission** |

Rules:
1. CI task `checkContentEligibility` fails if bundled assets or the default catalogue contain content below the artifact's channel, or `commercialOk=false` content in a monetized build (D-17).
2. If **G1 is declined** (not just unanswered), the QUL layout drops to `PERSONAL` immediately and the clean-room layout path starts (D-06).
3. Promotion rule: `PUBLIC` artifacts only go to open/production.

---

## 7. Launch plan

### 7.1 Tracks

| Track | Channel | Testers | Dates | Purpose / exit criteria |
|---|---|---|---|---|
| Internal testing | `PERSONAL` (owner-only list) or `BETA` | Owner (+ family on a separate `BETA` internal release if needed) | From 2026-12-21 | Daily use; no blocker for 7 consecutive days |
| Closed testing "Ramadan beta" | `BETA`, then `PUBLIC` RCs from 2027-01-18 | **≥ 20** (family, masjid contacts, a WhatsApp group; a Google Group as the tester list). At least 3 low-end devices (API 24–28) and 2 Transsion/Xiaomi/Vivo devices for adhan | Opens **2026-12-28** (no later than 2027-01-11); runs until production | Personal account: 12+ testers opted in for 14 continuous days. Pre-launch report clean (no crashes, accessibility issues triaged). Adhan health-check feedback from 5+ OEMs |
| Open testing | `PUBLIC` RC | Public opt-in, capped at 1,000 | 2027-01-18 → 2027-02-01, **only if G1, G2, G3 and G6 are clear** (and production access exists for a personal account) | Device diversity; crash rate ≤ 0.5 % |
| Production | `PUBLIC` | Everyone (countries: all where Play is available, except any that counsel advises against) | 2027-02-01 → ≈ 02-05 | §7.3 |

### 7.2 Go/no-go (2027-01-25) and fallback

**Go to production only if all hold:**
- G1 permission received (or the clean-room layout is complete and certified).
- G2: the public catalogue is frozen with only `PUBLIC` items (mp3quran grant archived; `PENDING` items excluded).
- G3: brand cleared.
- G4: licence decided.
- G5: certificate signed for the RC content hash.
- G6: opinion received with no blocking finding.
- The launch checklist (§7.4) is complete.
- No open S0/S1 defect.
- The closed test shows crash rate ≤ 0.5 % and ANR ≤ 0.3 %.

**Fallback (D-25):** if any gate is open on 2027-01-25:
1. Stay in **closed testing through Ramadan**. Grow the tester list via Google Groups (closed tracks allow large lists), with no public listing.
2. Ship weekly closed builds with Ramadan features (F-CP-07).
3. Clear the gates.
4. Go public in the first week after Eid (≈ mid-March 2027), using the same staged plan.
5. Even if G3 is the only open gate, do **not** launch under a temporary title: renaming after reviews accumulate costs ASO and trust. Wait for G3.

The scope-cut ladder (which features drop to hold the date) belongs to [Roadmap](09-roadmap-backlog.md). This doc only requires the listing to follow the cuts (§3.8).

### 7.3 Production staged rollout

| Step | Date (target) | % | Hold for | Advance if |
|---|---|---|---|---|
| Submit | 2027-01-25 | — | Review (new apps can take several days [unverified]); **managed publishing** on | Approved |
| 1 | 2027-02-01 | 10 % | ≥ 48 h | User-perceived crash ≤ 0.5 %, ANR ≤ 0.3 %, no S0, no adhan-failure cluster |
| 2 | 2027-02-03 | 25 % | ≥ 24 h | Same |
| 3 | 2027-02-04 | 50 % | ≥ 24 h | Same, and the 1★ share of new reviews ≤ 5 % |
| 4 | 2027-02-05 | 100 % | — | — |

- If the Console does not offer a percentage rollout for the **very first** production release [unverified], release 1.0.0 at 100 % as the RC already soaked in closed and open testing, and use 10/25/50/100 from 1.0.1 onwards.
- **Halt rollout** (Console "Halt") on any of: crash > 1.0 %, ANR > 0.4 % (our triggers sit below Play's bad-behaviour thresholds, believed to be 1.09 % / 0.47 % [unverified]); a confirmed S0 text error; an adhan regression; a data-loss bug. Then follow the hotfix process (§8.6).

### 7.4 Launch checklist (release-blocking unless marked *)

**Gates and legal**
- [ ] G1 permission email archived (or clean-room certified); DK author confirmation archived
- [ ] G2 catalogue frozen; mp3quran statement PDF + SHA-256 + date archived; permission-email log current
- [ ] G3 brand decision record; domain live
- [ ] G4 licence decided; if GPL: public repo prepared (§10.4)
- [ ] G5 certificate PDF for the RC content hash; `certified_pages.json` matches
- [ ] G6 written opinion; blocking items implemented

**Content and app**
- [ ] `checkContentEligibility` passes for `PUBLIC`
- [ ] Credits screen (F-PL-04) lists every item from r09 §2.5: Tanzil text notice + link; Tanzil metadata; QuranEnc publisher + version; DK font © + OFL text; QUL layout credit per permission wording; each reciter + source (text only); GeoNames CC BY 4.0; AboutLibraries; proofreading line; disclaimers
- [ ] Report-text-error flow (F-PL-05) works offline (composes the email); `notices.json` errata banner verified (F-PL-11)
- [ ] Kill-switch drill done on closed track: disable one recitation and one pack remotely, then restore (§8.2)
- [ ] No translation-only path: UI test asserts Arabic is present in translation view, copy and share output
- [ ] Adhan health check (F-CP-02) verified on 5 OEMs; inexact fallback banner verified with the permission denied
- [ ] Merged-manifest allowlist, SDK inventory and host-allowlist tests green; 16 KB alignment check green
- [ ] Baseline profile included; cold start ≤ target in [CI](10-quality-testing-ci.md) *
- [ ] Accessibility baseline (F-PL-07) passed; pre-launch report triaged

**Play Console**
- [ ] App content declarations (§1.5) complete; FGS videos uploaded (§4.4.2)
- [ ] Data safety "No data collected" submitted; privacy policy URL live (EN + UR)
- [ ] Listing EN + UR final; claims audit (§3.8) green; screenshots from the certified build; feature graphic; icon
- [ ] Content rating certificate received; target audience 13+
- [ ] Contact email/website/phone; developer page text
- [ ] Review notes for the Play reviewer: unique-value statement (r09 §2.1), "no login required", how to trigger adhan for review, mp3quran grant URL
- [ ] Release notes EN + UR; managed publishing on; rollout plan set

**Ops readiness**
- [ ] `support@` monitored; review-reply templates loaded (§8.4)
- [ ] On-call calendar for Ramadan (owner + backup reviewer for text errors)
- [ ] Hotfix branch procedure rehearsed once (§8.6)
- [ ] KPI sheet set up (§12) *

---

## 8. Post-launch operations

### 8.1 Text-error SLA (D-22, F-PL-05)

| Severity | Definition | Acknowledge | Mitigate | Fix shipped |
|---|---|---|---|---|
| **S0** | Arabic Quran text wrong: missing, extra or wrong word or harakat; wrong ayah marker or numbering; missing or duplicated page content; wrong Surah/Para boundary in the mushaf | ≤ 12 h | ≤ 24 h: signed `notices.json` errata banner on the affected pages ("Correction in progress at line N: …"). If several pages are affected, apply the layout kill-switch and fall back to the translation-view Arabic text (Tanzil) for that range | **≤ 48 h** production hotfix at 100 %, plus a G5 certificate addendum for changed pages (the proofreader is on call) |
| **S1** | Translation or tafsir text error or wrong attribution; wrong ruku/quarter/sajda marker; audio highlight misaligned; reciter or surah mislabelled | ≤ 48 h | ≤ 72 h: highlight kill-switch for that surah/recitation, or pack banner | Next release, ≤ 14 days. QuranEnc errors are also forwarded to QuranEnc (its condition 5) |
| **S2** | Cosmetic: ornament, spacing, kashida, typography | ≤ 7 d | — | Next scheduled release |

Intake:
- in-app email (prefilled `mushafId`, `page_label`, line, ayah key, word position, app version, content hash, optional screenshot chosen by the user);
- Play reviews;
- `support@`;
- the GitHub issue template (if the repo is public).

Every report is logged in `ops/text-errors.csv`. A report is "confirmed" only after the hafiz or alim reviewer checks it against the print. Reporters are thanked by reply.

### 8.2 Kill-switches and takedown

| What | Mechanism (signed, on R2) | Effect on device | Time to effect |
|---|---|---|---|
| A recitation (reciter × host) | `catalogue.json` → `recitations[id].killSwitch=true` | Hidden from pickers; streaming blocked; downloaded files kept but unplayable with a notice; the user may delete them | Next catalogue refresh (on app start + every 24 h) |
| Highlight for one surah of a recitation | `catalogue.json` → `timing[id].disabledSurahs[]` (D-12) | Audio plays; no highlight or auto page-turn | Same |
| A pack (translation, tafsir, layout, timing, font) | `index.json` → `packs[id].killSwitch=true` | Pack disabled; the user is shown why; the bundled default is used instead | Same |
| Errata / notices | `notices.json` → `errata[] {mushafId, pageIndexFrom, pageIndexTo, ayahKeys[], severity, message_en, message_ur, until}`; `minVersion` nudge (F-PL-11) | Banner on affected pages; an "update available" nudge (play flavor uses in-app update) | Same |

**Takedown policy:** on a rights-holder request (reciter, label, publisher, translator):
1. Flip the kill-switch within **24 h**.
2. Confirm in writing within **48 h** (our permission-email promise, g03 §2.7).
3. Remove the content from R2 and the next app release.
4. Log it in `ops/takedowns.csv`.
5. Never argue before complying. Resolve afterwards via the permission process.

**Drill:** once before launch and quarterly afterwards.

### 8.3 Content and release cadence

| Item | Cadence | Owner / tooling |
|---|---|---|
| App releases | Weekly closed builds during P2; production every 2–4 weeks after launch; **Ramadan freeze** ≈ 2027-02-08 → Eid (hotfixes only) | Eng |
| mp3quran timing / ETag validation | **Weekly** CI run; rebuild any timing pack whose guard changed (g03 rec 10; files were re-uploaded on 2026-09-27) | Pipeline |
| QuranEnc translation versions | Monthly check; update packs to the latest version (QuranEnc condition 6) | Pipeline |
| PK month-start Hijri feed | Monthly, on the evening of the 29th of each Hijri month after the Ruet-e-Hilal announcement (D-15) | Owner (5-minute task); backup person named |
| Catalogue changes | When a permission reply arrives: update `licenseStatus`, `eligibility` and `attribution` | Owner |
| Policy watch | Quarterly + on announced dates (§8.7) | Owner |
| Privacy policy | On any data-handling change, before the release | Owner |

### 8.4 Reviews and support

Rules:
- Reply to every 1–3★ review with text, and to 4–5★ reviews that ask a question. Reply in the language of the review (Urdu, Roman Urdu or English).
- Never argue. Never offer incentives for ratings.
- Pakistani users often give 1★ with positive text (r02 §2.6). Reply kindly and explain the stars.
- **In-app review prompt (F-PL-10, play flavor):** the Play In-App Review API, triggered only after a positive milestone: a completed Surah of ≥ 10 pages read in one session, a completed Para, or a khatam. At most once per 120 days. Never at launch, never after an error, never with an "exit rate us" dialog (r01 §2.8, r02 §2.6).

Templates (EN / UR):

| Case | EN | UR |
|---|---|---|
| Ads question / praise | "JazakAllah khair. <Brand> has no ads and never will, and every reciter is free. Please tell family and friends." | "جزاک اللہ خیر۔ <برانڈ> میں کوئی اشتہار نہیں اور نہ کبھی ہوگا، تمام قراء مفت ہیں۔ براہِ کرم اپنے عزیزوں کو بھی بتائیں۔" |
| Text error reported | "JazakAllah khair for telling us. Please use Menu > Report text error on that page so we get the exact place. We correct confirmed errors within 48 hours." | "اطلاع کا شکریہ، جزاک اللہ۔ براہِ کرم اسی صفحے پر مینو > متن کی غلطی رپورٹ کریں استعمال کریں تاکہ درست جگہ معلوم ہو سکے۔ تصدیق شدہ غلطی 48 گھنٹوں میں درست کی جاتی ہے۔" |
| Feature request (e.g. 15-line, WBW) | "Thank you. <Feature> is on our plan; we'll announce it in What's new. We only add content whose licence allows it." | "شکریہ۔ <فیچر> ہمارے منصوبے میں شامل ہے، آنے پر 'نیا کیا ہے' میں بتائیں گے۔ ہم صرف وہ مواد شامل کرتے ہیں جس کی اجازت موجود ہو۔" |
| Adhan didn't play | "Sorry about this. Please open Prayer times > Adhan health check; it shows the phone settings (battery, alarms) that block the adhan on your model." | "معذرت۔ براہِ کرم نماز کے اوقات > اذان ہیلتھ چیک کھولیں؛ یہ آپ کے فون کی وہ سیٹنگز دکھاتا ہے جو اذان روکتی ہیں۔" |
| 1★ with positive text | "JazakAllah khair for your kind words! If you're happy with the app, you can change the rating to more stars. If something is wrong, please tell us at support@<domain>." | "آپ کے اچھے الفاظ کا شکریہ! اگر ایپ پسند ہے تو آپ ریٹنگ بدل سکتے ہیں۔ اگر کوئی مسئلہ ہے تو support@<domain> پر بتائیں۔" |
| Translator / sect concern | "We offer translations from several recognised scholars and you can choose yours in Settings > Translation. The Arabic text is always shown." | "ہم کئی مستند علماء کے تراجم پیش کرتے ہیں؛ سیٹنگز > ترجمہ میں اپنا پسندیدہ منتخب کریں۔ عربی متن ہمیشہ ساتھ دکھایا جاتا ہے۔" |

Support email SLA: first reply ≤ 72 h (≤ 24 h during Ramadan).

### 8.5 Vitals monitoring (Play Console Android vitals; no SDK)

| Metric | Internal target | Alert (halt or hotfix) | Cadence |
|---|---|---|---|
| User-perceived crash rate | ≤ 0.5 % | > 1.0 % | Daily during rollout and Ramadan; weekly afterwards |
| User-perceived ANR rate | ≤ 0.25 % | > 0.4 % | Same |
| Per-device crash/ANR outliers | — | Any top-10 PK device > 3× average | Weekly |
| Excessive wake-ups / stuck partial wake locks | 0 flags | Any flag | Weekly |
| Slow cold start | ≤ 5 % sessions | > 10 % | Weekly |
| Store listing → installers | §12 | — | Monthly |

Crash clusters are triaged with deobfuscated stack traces (mapping in the AAB). v1.1 adds opt-in ACRA reports for detail.

### 8.6 Hotfix process

1. Branch `hotfix/x.y.z+1` from tag `vx.y.z`.
2. Fix, add a regression test, and bump PATCH/versionCode.
3. If content changed, re-proof the changed pages (certificate addendum) before the `PUBLIC` build.
4. CI (full suite) → sign → upload to internal for a 1-hour smoke test on 2 devices.
5. Production: 100 % for S0 text fixes, crash loops and adhan regressions; 20 % → 100 % within 24 h for other fixes.
6. Merge back to `main`, write "What's new" EN/UR, and close the incident note in `ops/incidents/`.
7. During the Ramadan freeze, only hotfixes are allowed.

### 8.7 Policy watch

- Check the Play "Policy deadlines" page and Android developer verification status quarterly: Jan, Apr, Jul and Oct.
- Known dates:
  - 2027-01-27: location button, contacts and geofencing-FGS policies (r09 §2.6);
  - 2027-02-01: 16 KB pages (r07 §2.8);
  - global developer verification in 2027;
  - target API 37 expected around Aug 2027 [unverified forecast].
- Record every finding in the [Decision log](12-decisions-risks.md).

---

## 9. Business

### 9.1 Positioning

**One-liner:** "The ad-free Indo-Pak Quran: read like your printed Quran, listen to your Qari." (r02 rec. 1)

**Pillars** (in this order, everywhere: listing, website, replies):

| # | Pillar | Proof |
|---|---|---|
| 1 | **Like your printed Quran:** typeset, ayah-aware 16-line Indo-Pak, the same page numbers as print | F-RD-01, F-RD-04, G5 certificate |
| 2 | **Your Qari, your way:** Surah, Para, Manzil, Ruku, any ayah; highlight; repeat for hifz | F-AU-02, F-AU-03, F-AU-05 |
| 3 | **No ads. No tracking. Free forever.** | Data safety "No data collected"; §9.4 |
| 4 | **Trustworthy:** proofread, sourced, corrected fast | Credits, 48-h SLA |

**Why the wedge holds** (r01 §1.7, r02 §1.5):
- About 52–53 % of the reference app's recent negative reviews cite ads.
- The best-rated apps (Greentech 4.82★, 2.0 % 1★) are free with no ads.
- Nobody combines an Indo-Pak page mushaf, ayah-highlighted audio and no ads.

The listing never disparages competitors.

### 9.2 Monetization roadmap (D-17)

| Phase | Model | Details | Preconditions |
|---|---|---|---|
| **v1.0 (P2)** | Free, no ads, no IAP, no accounts | "Sadqa-e-Jaria" framing: the *intention* is the user's own; we make no reward claims | — |
| **v1.5 (P3, earliest Apr 2027)** | **Optional "Support" IAP**, `play` flavor only (F-PL-08) | Three **consumable** in-app products: `support_300` = **PKR 300**, `support_1000` = **PKR 1,000**, `support_3000` = **PKR 3,000**. Set PKR prices explicitly; other countries use USD $0.99 / $2.99 / $9.99. **No perks, no badges that need restoring, nothing gated**: a thank-you screen only, so there is nothing to restore or break (r02 §2.7 "premium lost after update"). Entry point: Settings → Support, plus once in "What's new". Never a launch nag, never a pop-up mid-reading. Model revenue net of Play's 15 % fee (r09 §2.6) | (1) Commercial-content audit (§9.3) passed; (2) Organization account (address privacy); (3) Play merchant profile (PK supported, paid in USD; r09 §2.4); (4) accountant's tax advice; (5) content rating and Data safety re-checked |
| **v2 (P4, H2 2027)** | **Premium only for features with running costs** | Candidates: AI recitation follow-along / mistake checking (F-CP-14), our own hosted cloud backup if added, teacher/madrasa hifz dashboards. QF sync (F-BM-07) stays free because QF bears its cost. Subscription via Play Billing; **financial-aid path**: a pay-what-you-can / free request form reviewed within 48 h (the Tarteel Alim pattern, r02 §2.7); madrasas free on request. No auto-enrolled trials. Clear cancellation. Prices set from Play price experiments in P4 planning | Running cost is real and measured; reading, listening, indexes and downloads remain free |
| Donations | **No in-app donation links** | Only via an FBR-approved NPO partner, with documents pre-cleared with Play support (the AnkiDroid refusal, Sep 2026; r09 §2.6) | Optional, v2+ |
| Sponsorship | §9.5 | | v2 opportunistic |

### 9.3 Commercial-content audit (gate before v1.5; also before any sponsorship)

Rule: a monetized build is treated as **commercial** for every licence (r09 risk 5). The audit sets `commercialOk` for each provenance row; then `checkContentEligibility(monetized=true)` must pass for the `play` flavor.

| Content | Expected status | Action |
|---|---|---|
| Tanzil Quran text + metadata (CC BY 3.0, verbatim) | ✅ commercialOk | — |
| QuranEnc Junagarhi, Saheeh, Hilali-Khan, others ("no inappropriate ads") | ✅ | Keep versions current |
| DigitalKhatt IndoPak font (OFL) | ✅ | Never sold separately |
| QUL 16-line layout | Depends on the G1 permission wording | The G1 email asks for both free and monetized use; if only free, the `play` flavor cannot monetize until re-permissioned or clean-room |
| Our curated tables (Para, ruku, quarters, sajda, aliases) | ✅ (own) | — |
| Tanzil translation files (Jalandhari, and any PD text taken from Tanzil files) | ❌ NC ("non-commercial purposes only", r05 §1.3) | Hide in monetized builds, or replace with our own digitisation of a PD print edition, or get permission |
| mp3quran gapless audio (host grant, no NC clause) | ✅ by host terms; reciter rights caveat | Keep credits and the kill-switch |
| everyayah / islamic.network per-ayah audio ("free, non-commercial redistribution") | ❌ | Monetized builds use King Fahd Complex self-hosted gapped sets (D-12) or permissions |
| GeoNames cities (CC BY 4.0) | ✅ | Credit |
| Commissioned adhan recordings (work-for-hire assignment) | ✅ | Keep the written assignment |
| Azkar/dua texts | Per [Companion](07-companion-engagement-features.md) | Audit each source |

### 9.4 What we never do

- Ads of any kind, including in-house ads, ad networks and rewarded ads.
- Tracking, analytics or attribution SDKs; selling or sharing data; fingerprinting; an advertising ID.
- Gating Quran text, recitations, reciters, downloads, indexes or search behind payment or ads.
- Reciter photos; "Official"; impersonating any app, publisher, reciter or institution.
- Dark patterns:
  - hasanat or reward counters;
  - shaming streaks;
  - manipulative countdowns or scarcity claims;
  - promotional push notifications;
  - surprise auto-renewals or auto-enrolled trials;
  - guilt copy (r09 §2.7).
- Translation-only mode; unlabelled sectarian defaults; Ahmadi translations or tafsir; AI-generated tafsir or fatwas.
- Multiple near-identical apps (per-surah or per-edition clones); keyword stuffing; incentivised or fake reviews.
- External payment links (outside Play Billing) or donation links to unverified organisations.

### 9.5 Sponsorship credits (v2, opportunistic)

- **Eligible sponsors:** masjids, trusts, madrasas and halal businesses. Not riba-based finance, gambling, music/entertainment, tobacco or political parties. The owner and the content reviewer vet each one.
- **What a sponsor can fund:** hosting of a named reciter pack (after permission), a translation-permission fee, the proofreading of a new layout, or adhan recordings.
- **Display:** "Sponsored by <Name>" in Credits and on the sponsored item's info sheet only. Never in the reader, player, notifications or listing graphics. No sponsor links with tracking parameters.
- **Sponsors cannot influence** defaults, translation choice, content or features. A written sponsorship note states this.
- Sponsorship makes the build **commercial** (r09 §2.8). Run the §9.3 audit first.

### 9.6 Cost budget

Planning exchange rate **PKR 280 = US$1** (derived from Play's own PKR ladder for the reference app's IAP: $0.99–$24.99 = Rs 280–6,900; r01 §2.1). Items marked *[estimate]* are planning figures, not quotes. The owner gets quotes in P0/P1.

| Item | Type | PKR | USD | Basis |
|---|---|---|---|---|
| Google Play developer registration | One-time | 7,000 | 25 | Verified (r09 §2.6) |
| D-U-N-S number | One-time | 0 | 0 | Verified (r09 §2.6) |
| SECP SMC-Pvt incorporation + NTN (incl. professional help) | One-time | 15,000–40,000 | 54–143 | *[estimate]* |
| Domain (`.com`/`.app`) | Yearly | 2,800–4,200 | 10–15 | r09 §2.8 [unverified] |
| Cloudflare Pages, Email Routing, Workers (free plans) | Monthly | 0 | 0 | Free tiers |
| Cloudflare R2: packs + self-hosted permitted audio | Monthly | 0–1,400 | 0–5 | $0.015/GB-month after 10 GB free, $0 egress (r09 §2.8); ≈ 70 GB ≈ $1/month (r06 §1.12); D-24 |
| Designer: icon (adaptive + monochrome), feature graphic, screenshot frames, website hero | One-time | 60,000–150,000 | 215–535 | *[estimate]* |
| Mushaf ornament vector set (frame, surah cartouche, ruku/manzil/quarter/sajda markers; D-05; spec in doc 05) | One-time | 40,000–120,000 | 145–430 | *[estimate]* |
| Hafiz/Qari proofreading honorarium (G5), Taj 16-line, ≈ 55–95 h | One-time per layout | 40,000–100,000 | 145–360 | *[estimate]* |
| Printed 16-line Taj mushaf × 2 (proofreading reference; clean-room fallback) | One-time | 3,000–6,000 | 11–21 | *[estimate]* |
| Commissioned adhan (standard, Fajr, short takbeer ≤ 30 s, post-adhan dua) + studio, with written assignment (D-16, r08 §2.4) | One-time | 25,000–60,000 | 90–215 | *[estimate]* |
| Pakistani legal opinion (G6) | One-time | 50,000–150,000 | 180–535 | *[estimate]* |
| Trademark search + IPO-Pakistan filing, classes 9 + 41, via agent (G3) | One-time | 30,000–80,000 | 107–285 | *[estimate; check the IPO fee schedule]* |
| Low-end test phones × 2 (API 24–28 class, used; shared with doc 10) | One-time | 20,000–40,000 | 71–143 | *[estimate]* |
| Native Urdu copy editor (listing, UI strings) | One-time | 10,000–25,000 | 36–89 | *[estimate]* |
| **Subtotal, one-time to v1.0** | | **≈ 300,000–780,000** | **≈ 1,075–2,795** | |
| **Contingency 15 %** | | ≈ 45,000–117,000 | ≈ 160–420 | |
| **Running cost, v1.0** | Monthly | **≈ 250–1,750** | **≈ 1–6** | Domain amortised + R2 |
| P3 additions: proofreading 15-line (610 pp) and 13-line (847 pp) | One-time | 45,000–110,000 and 65,000–160,000 | 160–390 and 230–570 | *[estimate]*, scaled by pages |
| v1.1 crash Worker + R2 bucket | Monthly | 0 | 0 | Free tiers |
| v1.x group khatam (Firebase Spark) | Monthly | 0 (Blaze only if exceeded) | 0 | r09 §2.8 |
| v1.5 accountant / tax setup | One-time | 15,000–40,000 | 54–143 | *[estimate]* |
| v1.5+ Play service fee | % of IAP | 15 % | 15 % | Verified (r09 §2.6) |

**Budget owner rule:** a single item above its range by > 25 % needs the owner's explicit OK. Spending on permissions (translation fees) is decided case by case at the reply.

### 9.7 Entity, payouts, tax

- The entity (§1.1) owns the Play account, domain, trademarks, the adhan assignment and the designer's IP assignment. **All commissioned work uses a written work-for-hire or assignment agreement** (designer, ornaments, adhan, Urdu copy). The proofreader signs a credit-consent note.
- Payouts (v1.5+): Play pays Pakistani merchants in USD/EUR (r09 §2.4). Tax treatment and PSEB registration go to the accountant [unverified].

---

## 10. Open-source licence decision framework (gate G4, decide by 2027-01-11)

### 10.1 Options and criteria

| Criterion (weight) | **GPL-3.0-or-later** | Apache-2.0 | Proprietary (closed) |
|---|---|---|---|
| Trust: "no ads, no tracking" is verifiable (25 %) | ✅ High | ✅ High | ⚠️ Claims only |
| Protection against ad-stuffed reskins (20 %) | ✅ Copyleft + DMCA leverage; trademark covers name and icon (r09 §2.9) | ⚠️ Trademark only; closed forks are legal | ✅ Copyright only, but code is not public anyway |
| Monetization flexibility (IAP, subscriptions) (15 %) | ✅ Legal | ✅ | ✅ |
| B2B white-label / proprietary licensing (10 %) | ⚠️ Only with a CLA (relicensing rights) | ✅ | ✅ |
| F-Droid / IzzyOnDroid distribution (10 %) | ✅ (foss flavor) | ✅ | ❌ |
| Community contributions and credibility in the Quran-app ecosystem (10 %) | ✅ Strongest | ✅ | ❌ |
| Effort and risk at launch (10 %) | ⚠️ History scrub, licence headers, CI checks | ⚠️ Same | ✅ None |

Clean-room (D-18) keeps all options open: there is no GPL code inside, and Apache/MIT ports carry headers.

**Recommendation:** **GPL-3.0-or-later** for the Android client, with **DCO** sign-off, `TRADEMARKS.md` and REUSE-style licence files. Choose **proprietary** only if, by 2027-01-11, the owner has a concrete B2B or white-label plan. (Apache-2.0 offers no advantage over proprietary for that plan and weakens reskin protection.) If GPL is chosen but the owner wants to keep a relicensing option, use a short **CLA** instead of DCO and accept fewer outside contributions.

### 10.2 Implications by choice

| Topic | GPL chosen | Proprietary chosen |
|---|---|---|
| Repo | Public repo at launch (§10.4) | Stays private; "all rights reserved" |
| `foss` flavor | Distributed: IzzyOnDroid first (accepts our signed APK), then F-Droid main repo (builds from source; reproducible builds keep our signature) | Not distributed (it stays as a build-hygiene flavor, D-15) |
| F-Droid anti-features | Expect **NonFreeAssets** (Tanzil "verbatim only" text, QuranEnc "no modification" translations) and possibly **NonFreeNet** (third-party audio hosts). This is acceptable and disclosed (r09 §2.9) | — |
| Developer verification | Register the foss key (§1.3) | — |
| Content | Content is **not** under the code licence. Each file keeps its own licence (`LICENSES/`, provenance log). Our curated metadata tables are published under **CC BY 4.0**. QUL-derived layout data, personal/beta catalogues and raw exports are **never** committed to the public repo; CI fetches them from private storage | All private |
| Play listing | Add "Open source (GPL-3.0)" to the privacy section of the listing and website | — |

### 10.3 `TRADEMARKS.md` (outline)

1. The code is GPL-3.0-or-later. The **name "<Brand>", the logo, the icon, store listing text and screenshots are not licensed** under it.
2. Forks must change the app name, icon and `applicationId` and must not imply endorsement.
3. Unmodified redistribution of official APKs is allowed with the name.
4. Report misuse to `abuse@<domain>`.
5. The Play listing and assets are © the entity.

### 10.4 Going public (if GPL)

1. Create a **fresh public repo from a squashed snapshot**. Do not make the private repo public, because its history contains research teardowns of the reference app, restricted data paths and personal-build catalogue details.
2. Exclude `docs/research/` and all restricted data.
3. Run gitleaks or another secret scan, add SPDX headers, add `NOTICE` entries for Apache/MIT ports (quran-ios QuranKit, LinePageGeometryEngine, FSRS-Kotlin; D-18), and add the AboutLibraries config.
4. Add `CONTRIBUTING.md` (DCO 1.1 `Signed-off-by:`, enforced by a DCO check action), `SECURITY.md` and issue templates (including "Text error").
5. Mirror the `store/` metadata in the F-Droid fastlane layout.
6. Development continues in the public repo after launch. Embargoed work (permissions-pending content) lives only in private data storage, not in code.

---

## 11. Growth (no tracking, no ads)

| Lever | Mechanism | Feature | Phase |
|---|---|---|---|
| Word of mouth | A product people recommend: ad-free, no gated reciters, "matches my printed Quran". Reply to every review | — | P2 |
| WhatsApp khatam sheet | Para-distribution text: "1. Alif Lam Meem — Ahmed ✓ / 2. Sayaqool — ____ …". Shared via the share sheet with an optional "via <Brand>" footer and Play link (user can toggle it off) (r08 §2.18) | F-CP-11 | P2/P3 |
| Share ayah image/text | Arabic + translation + reference + attribution + small brand mark; WhatsApp-first (r08 §2.15) | F-CP-10 | P2 |
| Ramadan | Shareable Sehri/Iftar timetable image per city, Taraweeh khatam plan, "Para a day" plan | F-CP-07, F-BM-04 | P2 |
| Masjid partnerships | A printable A4 Ramadan timetable for a masjid's own city (generated by the app) with a QR code to the Play listing. Offered free to 10–20 local masjids before Ramadan; imam announcement optional. No endorsements quoted in the listing (metadata policy) | F-CP-07 | P2 (Jan 2027) |
| Madrasa partnerships | Hifz mode is free for madrasas; ustad weekly report as an image/text for WhatsApp. Interview 2–3 ustads in P3 to validate sabaq/sabqi/manzil (r08 open Q2) | F-CP-12 | P3 |
| Quick-access ASO surfaces | Manzil dua, Yaseen, Mulk, Kahf on Fridays: listing keywords plus home chips | F-IX-08 | P1/P2 |
| In-app review | Milestone-triggered only (§8.4) | F-PL-10 | P2 |
| Store listing experiments | Title/short-description A/B (§3.1) | — | Post-launch |
| Open source (if G4 = GPL) | IzzyOnDroid/F-Droid listings; trust posts in Muslim developer communities | — | P2/P3 |
| Diaspora (UK/Gulf) | English listing quality; regional prayer presets (r08 open Q7) | F-CP-01 | P3 |

No referral codes, install-referrer SDK, UTM-tracked deep links or push campaigns.

---

## 12. KPIs without tracking

Sources: the Play Console (installs, uninstalls, listing conversion, search terms, ratings, vitals, pre-launch reports); Cloudflare's aggregate R2/Pages request counts (counts, not users); our own ops logs. Nothing is measured on the device or sent from the app.

| KPI | Source | Target (first 90 days) | Cadence |
|---|---|---|---|
| Store listing visitors → installers (PK) | Play Console acquisition | ≥ 30 % *[target, unvalidated]* | Monthly |
| Top search terms bringing installers | Play Console acquisition [check report name] | 16 line / para / quran pak present in the top 10 | Monthly |
| Average rating / rating count | Play Console | ≥ 4.7 after 500 ratings | Weekly |
| Share of new reviews that are 1★ | Play Console reviews | ≤ 4 % (ad-free peers 2–4 %, r02 §2.2) | Weekly |
| Review theme mix | Manual monthly coding of ≤ 3★ reviews (themes: text, audio, adhan, UI, missing feature) | Ads-complaint share ≈ 0; no theme > 30 % | Monthly |
| User-perceived crash / ANR | Android vitals | ≤ 0.5 % / ≤ 0.25 % | Weekly |
| Installs and uninstall ratio | Play Console | Aspirational: 10,000 installs by Eid ul-Fitr 1448; uninstalls/installs ≤ 35 % *[targets, not forecasts]* | Weekly in Ramadan |
| Retained installers (D30) | Play Console [if offered] | Trend only | Monthly |
| Downloads per reciter / pack | R2 aggregate object request counts | Informs which reciters to self-host | Monthly |
| Text-error reports per 10k installs; S0 SLA compliance | `ops/text-errors.csv` | S0 SLA 100 % | Monthly |
| Support first-reply time | Inbox | ≤ 72 h (≤ 24 h in Ramadan) | Monthly |
| Permission replies received / pending | Permissions log | All G1/G2 core emails answered by 2027-01-18 | Weekly until done |
| Adhan health-check guidance effectiveness | Review/support mentions of "adhan didn't play" | Falling trend | Monthly |

An optional opt-in feedback survey (an external form link in "What's new", at most twice a year) is allowed. It is disclosed in the privacy policy as an external service.

---

## 13. Acceptance criteria for this doc's deliverables

| Deliverable | Done when |
|---|---|
| Play account | Organization (or personal fallback) verified; app created; developer-verification status screenshot logged |
| Signing | Upload key in 3 locations; CI signs only on tags in the protected environment; a test AAB uploaded to internal |
| Listing | EN + UR text in `store/`, each ≤ limits (title ≤ 30, short ≤ 80, full ≤ 4,000), claims audit green, 8 screenshots × 2 languages from the certified build, feature graphic, icon |
| Privacy | `/privacy` and `/ur/privacy` live; in-app links; host allowlist test green; Data safety submitted |
| Declarations | §1.5 all submitted; FGS videos linked; content rating certificate issued |
| Compliance | G5 certificate + `certified_pages.json` CI check; G6 opinion; governance policy published; provenance log complete |
| Eligibility | `checkContentEligibility` runs for every artifact; the kill-switch drill is logged |
| Launch | §7.4 checklist complete; staged rollout executed or fallback invoked per §7.2 |
| Ops | SLA tables, templates and the hotfix procedure in `ops/`; first quarterly policy check scheduled |

---

## 14. Cross-doc dependencies and open issues

**Assumptions other docs must match:**
1. **Eligibility flags** (`eligibility ∈ {personal, beta, public}`, `commercialOk`, `fossOk`, `licence`, `attribution`, `killSwitch`) on every pack and recitation in `index.json` / `catalogue.json`, plus `BuildConfig.RELEASE_CHANNEL`. These are owned by [Architecture](04-architecture.md), [Data pipelines](08-data-pipelines.md) and [Audio engine](06-audio-engine.md). The CI tasks `checkContentEligibility`, `verifyMergedManifest`, the SDK inventory diff and the host-allowlist test belong to [CI](10-quality-testing-ci.md).
2. **`notices.json`** (errata banners, `minVersion`) as a signed static file: new feature **F-PL-11**. The schema is defined by doc 04/08.
3. **Per-page render hashes** and `certified_pages.json` from the pipeline/renderer (doc 05/08), used by the G5 validity check.
4. **Copy and share always include Arabic** (doc 02/07). There is no "copy translation only" action.
5. **Report-text-error (F-PL-05)** composes an email via `ACTION_SENDTO` with prefilled location fields. There is no network call from the app.
6. **v1.1 crash reports** go to a Cloudflare Worker endpoint, not an email sender. This is a small exception to "no backend" for v1.x; doc 04/12 should record it.
7. **Versioning scheme** (§1.4) and "never promote a beta AAB to production" (doc 10).
8. Gate target dates G3 2026-12-07, G4 2027-01-11, G6 2027-01-18, G5 2027-01-25, go/no-go 2027-01-25, closed test opening 2026-12-28 (doc 09/12 are authoritative; these are proposed).
9. Listing claims follow the roadmap's scope-cut ladder (doc 09). Frame 7 and the "DAILY COMPANION" block are conditional.

**Open issues:**
- **First production release and staged rollout:** whether Play allows a % rollout on an app's first production release is unverified. §7.3 covers both cases.
- **Data safety edge cases:** confirm when filling the form that (a) Android Auto Backup and (b) user-initiated emails/shares are not "collection" by us. If Play's current help text says otherwise, declare them as optional. **D-20's "No data collected" may then need a qualifier.**
- **Closed testing with G1 unanswered:** §6 lets closed testers see the QUL layout (`BETA`) while Tarteel has not replied. If doc 03/12 read D-06 as "no distribution of any kind before permission", closed testers need the clean-room layout or must wait. This needs the owner's ruling.
- **v2 "AI hifz checker" as premium:** D-17 lists it as a running-cost feature. If F-CP-14 ends up fully on-device (sherpa-onnx), its marginal cost is near zero, and putting it behind premium needs re-justification (model hosting and updates) or a revised D-17.
- **Noorani Qaida (F-CP-13):** in-app vs a separate Families-compliant app is undecided (P4).
- **All PKR cost figures except the Play fee, D-U-N-S and R2 pricing are estimates.** Quotes are needed in P0/P1. The trademark (IPO) and SECP fee schedules are unverified.
- **Personal-account fallback timing:** the 14-day clock plus Google's production-access review plus first app review may not fit before 2027-02-01. Opening the closed test on 2026-12-28 is what makes it fit.
- **Shamshad Ali Khan Urdu audio (F-AU-10):** stays `PERSONAL` until Hi-Tech Music replies (D-12). The public Urdu option is text translation only, plus the King Fahd Complex Urdu meanings for al-Fatiha and Juz 'Amma if permission is confirmed (g03 rec. 8).
- **r09 used "Quran 13 Line" in its sample title.** This doc follows D-19 ("16 Line"), and 13/15-line wording waits for F-RD-11.

**New IDs introduced in this doc**
- **F-PL-10** In-app review prompt, milestone-triggered, `play` flavor only [P2].
- **F-PL-11** Signed remote notices (`notices.json`: text-errata banners, takedown notices, `minVersion` update nudge) [P2].
