# 09: Legal, Google Play policy, privacy and monetization

**Author:** research agent r09 (product lead: licensing, Play policy, Pakistan market) · **Date:** 2026-10-19
**Scope:** What we may clone and what we may not, and the brand. Content licensing and the attribution it requires. The Google Play compliance checklist for 2026. Privacy and analytics. Monetization and costs. Open-source strategy.
**Evidence convention:**
- **[verified: url]** means fetched or inspected in this session.
- **[unverified]** means from memory or from a secondary source not opened.
- "Report 0x" means a sibling report in `docs/research/`.

> Not legal advice. Pakistani and foreign copyright and religious-content law is applied here from primary texts where I could fetch them. Get a short review from a Pakistani IP lawyer before any monetized launch.

---

## 1. Executive summary

1. **Clone the *features and conventions*, never the *assets*.** We may freely re-implement the following, because ideas, functionality and traditional mushaf conventions are not ownable:
   - page-by-page Indo-Pak reading, Juz/Surah/Manzil indexes, Para names and ruku "ع" markers;
   - reciter playback, bookmarks, night mode, tasbeeh, prayer times and so on.

   We must **not** copy the name/trade dress "Al QURAN – القرآن الكريم", its icon, screenshots, description text, code, page scans or audio catalogue. Google Play's Impersonation, Intellectual Property and Repetitive Content policies are explicit: "We don't allow apps that merely provide the same experience as other apps already on Google Play." [verified: support.google.com/googleplay/android-developer/answer/9899034]
2. **Brand.** Keep **"Criterion"** as the internal codename and package (`com.chughtai.criterion`; package names are permanent). Ship under a distinct, ASO-friendly consumer brand with a descriptive suffix.
   - "Criterion" has zero search relevance in Pakistan and collides with several existing "Criterion" marks and apps.
   - Lead candidate: **"Hamdam" (ہمدم)**, with the title "Hamdam: Quran 13 Line & Audio" (29 chars). Play search found no Quran app using it [verified via google-play-scraper].
   - A formal trademark search at IPO-Pakistan, WIPO and USPTO is still required.
3. **The monetization model decides which content we may use. This is the single most important business-legal coupling.**
   - **Non-commercial only:** Tanzil *translations* [verified: tanzil.net/trans], QuranicAudio MP3s ("you may not use these files for commercial purposes") [verified: quranicaudio.com/about], and quran_android's ethos and data (CC BY-NC-ND "typically") [verified: quran_android README].
   - **Commercial use allowed:** Tanzil Quran *text* (CC BY 3.0, verbatim) [verified], QuranEnc translations (redistribution allowed, no NC clause, but "inappropriate advertisements must not be included") [verified], OFL fonts, and the QF API (monetized apps allowed, but with strict storage rules) [verified].
   - **So:** launch **v1 free, ad-free and IAP-free**, and design every content pipeline so it can be swapped to commercial-OK sources before any monetization.
4. **Mushaf rendering must not use scans.** Indo-Pak page scans are copyrighted publisher and calligrapher works: Taj Company runs its own monetized Play app [verified]. Use the **DigitalKhatt IndoPak font (SIL OFL 1.1, "© 2024 Amine Anane, © 2024 Tarteel Inc.")** [verified: font name table] with QUL line-layout data. QUL's data licence is unstated, so get Tarteel's written OK. This matches reports 03 and 04.
   - **KFGQPC fonts:** "cannot be Sold, Modified, Altered… Reproduced", so ship them unmodified and **never subset** them [verified: EULA in font file].
   - **"AlQuran IndoPak by QuranWBW":** "NOT FOR SALE, NOT FOR MODIFICATION, NOT FOR DISTRIBUTION… WITHOUT WRITTEN NOTICE". It is also derived from a competitor's (Pakdata's) Al Qalam fonts [verified: font name table]. Do not ship it without permission.
5. **Translations.** Default stack:
   - **QuranEnc:** Saheeh International, now under "Noor International Center" (`english_saheeh` v1.1.2); Hilali-Khan; Junagarhi (Urdu) [verified: quranenc.com API].
   - **Public-domain Urdu classics:** Kanz-ul-Iman, Mahmud-ul-Hasan/Usmani, Jalandhari. Rule: author died ≥51 years ago (PK life+50, s.18) **and** first published ≤1930 (US).

   **Needs written permission:** The Clear Quran (the publisher demands "prior written consent"), Taqi Usmani, Maududi (© until 2029 in Pakistan), Tahir-ul-Qadri and Israr Ahmad.
6. **Audio: no source grants clean redistribution rights.** Stream from the providers' public endpoints with attribution, and email them for permission before a public launch.
   - **Self-host on R2 only for reciters whose rights we have in writing.** Hotlinking QuranicAudio from a *monetized* app breaches its terms; the reference app does exactly this while paywalling reciters (report 06).
   - **QF Terms cap storage of QF Content at 1 week**, and "recitation rows contain audio URLs, not the underlying recording files" [verified]. Offline audio from QF therefore needs written clarification.
   - **No reciter photos.** "Professional images of public figures" is a listed IP violation [verified: answer/9888072].
7. **Pakistan religious-content law applies to digital copies in practice.** The Punjab Holy Quran (Printing and Recording) Act 2011 [verified: WIPO Lex] requires error-free text and translations "with[] Arabic text", bans non-Muslim interpretation "contrary to belief of Muslims", and sets penalties of up to **3 years' imprisonment**.
   - PTA has ordered Google to remove an "unauthentic version of the Holy Quran" app [verified: dawn.com/news/amp/1597848].
   - **Actions:** get a hafiz proofreading certificate, never offer a translation-only mode, exclude Ahmadi-published translations and tafsir, and keep a sect-balanced, documented content policy.
8. **Play compliance (2026) is mostly met by design.**
   - **Target API:** targetSdk 36 is already required (since Aug 31 2026) [verified].
   - **Exact alarms:** use **SCHEDULE_EXACT_ALARM, not USE_EXACT_ALARM**. The latter is only for apps whose *core* function is an alarm, timer or calendar [verified].
   - **Foreground services:** Media3 `mediaPlayback` needs the Play FGS declaration plus a video. Downloads go through **user-initiated data transfer jobs (UIDT)** rather than a `dataSync` FGS where possible [verified].
   - **Location:** **coarse location only**, computed on device, with no background access.
   - **Full-screen intent:** not used.
   - **Target audience:** 13+, to stay out of Families obligations [verified].
9. **Developer account: prefer an Organization account (D-U-N-S is free) [verified].**
   - Personal accounts created after Nov 13 2023 need a **12-tester × 14-day closed test** [verified].
   - A personal account that monetizes has its **full home address shown publicly** [verified].
   - Android developer verification starts enforcement on Sep 30 2026 in BR/ID/SG/TH, then goes global in 2027. Play apps are auto-registered; F-Droid/GitHub builds need key registration [verified].
10. **Privacy as a differentiator.** The reference app declares "No data collected" while bundling AdMob, AppsFlyer and Firebase (report 01).
    - We ship **zero third-party SDKs in v1** and use Play Console **Android vitals** for crashes and ANRs, which needs no SDK [verified]. Crash reports via ACRA (Apache-2.0) are opt-in.
    - We file an honest "No data collected" Data safety form.
    - Reading history and bookmarks are *religious (special-category) data*. They stay on device and are never sent to analytics.
11. **Money.** Fixed costs are tiny:
    - US$25 Play fee [verified]
    - a domain
    - $0 audio while streaming public CDNs
    - Cloudflare R2 when self-hosting: $0.015/GB-month after 10 GB free, **$0 egress** [verified]

    Recommended path:
    - **v1:** free, sadaqah-jariyah positioning.
    - **v1.5:** an optional Play Billing "Support" IAP (15% fee) [verified], *after* the content audit.
    - **v2:** premium only for cost-bearing features (cloud sync, AI hifz checking).
    - **Never:** ads in the reader or player.
    - **Donations:** in-app donation links are allowed only for verified tax-exempt organizations. AnkiDroid was blocked in Sep 2026 even with 501(c)(6) papers [verified: gigazine.net].
12. **Open source.** Recommended: **GPL-3.0-or-later** for the Android client, plus a trademark policy and a light CLA. It gives:
    - verifiable no-tracking trust;
    - legal reuse of GPL Kotlin/Compose Quran code (e.g. AlfaazPlus/QuranApp) and of Apache/MIT code;
    - F-Droid/IzzyOnDroid distribution;
    - DMCA leverage against closed, ad-stuffed reskins.

    Choose Apache-2.0 instead only if the owner wants proprietary white-label deals. Either way, follow report 03's advice not to fork quran_android wholesale (Medium).

---

## 2. Detailed findings

### 2.1 Cloning boundaries: what we may copy and what we may not

**Policy text that governs this (verbatim quotes):**

| Policy | Decisive text | Source |
|---|---|---|
| Impersonation | "We don't allow apps that mislead users by impersonating someone else … or another app. Don't imply that your app is related to or authorized by someone that it isn't. Be careful not to use app icons, descriptions, titles, or in-app elements that could mislead users…" Listed violation: "App titles and icons that are so similar to those of existing products or services that users may be misled." | [verified: support.google.com/googleplay/android-developer/answer/9888374] |
| Intellectual Property | "Modifying copyrighted content may still lead to a violation." Example violations include "Professional images of public figures", "Full reproductions or translations of books that are not in the public domain" and "Streaming apps that allow users to download a local copy of copyrighted content without authorization." | [verified: …/answer/9888072] |
| Repetitive Content (Spam) | "We don't allow apps that merely provide the same experience as other apps already on Google Play. Apps should provide value to users through the creation of unique content or services." Violations: "Copying content from other apps without adding any original content or value" and "Creating multiple apps with highly similar functionality…" | [verified: …/answer/9899034] |
| Minimum functionality | "Apps that are static without app-specific functionalities, for example, text only or PDF file apps" | [verified: …/answer/9898783] |
| Metadata | Title ≤ 30 chars. No emojis or ALL CAPS (unless brand). No "#1", "Best", price or promo text. No "unattributed or anonymous user testimonials". "Don't use a celebrity's name or a brand's logo without permission." | [verified: …/answer/9898842] |

**Boundary table:**

| ✅ May copy or re-implement | ⚠️ Gray: do it our own way | ❌ Must not copy |
|---|---|---|
| Feature set: mushaf paging, Juz/Surah/Manzil/Ruku index, Para names (traditional), resume, bookmarks, night mode, font size, reciter player, repeat, background play, offline downloads, prayer times, Qibla, tasbeeh, Hijri, azkar | **Toolbar layout and icon set:** the generic Material icons are fine, but don't pixel-match their screens | Name/title "Al QURAN – القرآن الكريم" and the developer brand "9D Muslim Apps" |
| Traditional Indo-Pak conventions: 13/15/16-line editions, ruku "ع" margin marks, "منزل" foot markers, ornamental border (*draw our own*) | **Exact 16-line Taj pagination:** layout *data* is factual/traditional (low risk), but QUL's data licence is unstated → get written OK | Their **icon**, colour/trade dress combination, **screenshots**, promo art and **Play description text** (copyright in the text itself) |
| "Quick access" list of popular surahs (Yaseen, Rahman, Mulk…); these are facts | **Using "Al Quran" or "Quran" as a descriptor in our title:** generic and allowed, but never their exact title string or Arabic logotype | Their **APK code**: decompiling and copying is copyright infringement. Report 01's teardown is analysis only |
| Reciter *names* listed factually in-app and in the description | **Reciter names in the *title*** (e.g. "Sudais Quran MP3"): risky under "celebrity's name … without permission" | Their **page scans** (Taj Company copyrighted images), **audio catalogue JSON**, **translations DB** |
| General UX ideas (swipe paging, long-press ayah menu) | **"Official" wording, Saudi/Haramain/KFGQPC logos, government emblems:** do not use | **Reciter photos** ("professional images of public figures") |

**What gets Quran apps rejected or removed** (from policy text plus market observation):
- **Flooding the store with near-identical apps.** Example: one-app-per-surah sets, such as 123Muslim's "Surah Furqan", "Surah Furqan + Urdu" and "Surah Furqan Audio" [verified: Play search via google-play-scraper]. This is the "multiple apps with highly similar functionality" pattern, so **ship one app**.
- **Static "PDF/text-only" mushaf viewers** with no app-specific functions (minimum functionality).
- **Webview wrappers of quran.com or other sites** (Webview spam) [verified: answer/9899034].
- **Keyword-stuffed metadata.** The category is full of package names like `quran.offline.alquran.holyquran.quranpak.quranmajeed.readquranoffline…` [verified: Play search]. The *title and description* must stay clean.
- **Misdeclared Data safety or ads labels.** The reference app declares "no data collected" and shows no "Contains ads" label while bundling AdMob and AppsFlyer (report 01). That is enforcement exposure we should not replicate.

**Our "unique value" statement for the Play review and listing (mitigating Repetitive Content):**
1. An ayah-aware, typeset (not scanned) Indo-Pak 16/15/13-line mushaf with tap, highlight and audio follow-along.
2. Para, Surah, **Manzil** and **Ruku** indexes with Para/Manzil playback.
3. Gapless and gapped recitation with hifz repeat.
4. Zero tracking, no ads, open source.

### 2.2 Name and brand direction

**Assessment of "Criterion":**
- **Meaning:** a lovely story, since Al-Furqān (25:1) = "The Criterion".
- **Search relevance:** none for Urdu/English Quran queries in Pakistan.
- **Collisions on Play** [verified: google-play-scraper, country=pk]:
  - Criterion HCM / Criterion Time / Criterion Kiosk (Criterion Inc.)
  - "Criterion Life App"
  - "New Criterion"
  - Criterion Ticketing
- **Collisions off Play** [unverified]: well-known marks such as The Criterion Collection/Channel (film streaming) and EA's Criterion Games.
- **Risk:** moderate-low legal risk, because the goods differ. But it is weak for ASO and invites "Criterion Channel" confusion.

**Verdict:** keep "Criterion" for the repo, codename and package ID (`com.chughtai.criterion` is neutral and permanent). Optionally use it as an English tagline ("Al-Furqan · The Criterion"). Choose a separate consumer brand.

| Candidate | Meaning | Play collision check (pk, Quran context) | Title example (≤30) | Notes |
|---|---|---|---|---|
| **Hamdam (ہمدم)** | intimate companion (Urdu/Persian) | No Quran app named Hamdam found [verified] | `Hamdam: Quran 13 Line & Audio` (29) | Strong Urdu resonance, easy to say, fits "daily routine" |
| Quran Saathi | companion (Hindi/Urdu) | No direct hit [verified] | `Quran Saathi: Mushaf & Audio` (28) | "Saathi" reads more Indian than Pakistani |
| Furqan / Al-Furqan | = Criterion | **Crowded:** ≥8 apps ("Furqan: Modern Quran App", "مصحف الفرقان", "Al Furqan: Quran, Prayer Times"…) [verified] | — | Avoid as the sole brand |
| Rafiq | friend | **Taken:** "Rafiq: Quran, Prayer & Qibla", "Rafiq Al Muslim", "Rafiq: Quran Athan Prayer" [verified] | — | Avoid |
| Noor | light | **Very crowded** ("Noor Quran…", "NOOR: Quran, Prayer & Qibla", "مصحف النور") [verified] | — | Avoid |
| Tilawah | recitation | "Tilawah – Quran & Prayer Time" exists (500k+) [verified] | — | Avoid |

**ASO formula:** `<Brand>: Quran <edition> & <hook>`.
- Put Urdu keywords (قرآن پاک، 16 سطری، منزل، پارہ) in the short and long descriptions, and localize the listing to `ur-PK`.
- Never put a reciter's name or "Official" in the title or icon.
- **Before committing:** search IPO-Pakistan (Class 9/41), the WIPO Global Brand Database and USPTO; register the domain and Play developer name; and file a Pakistani trademark once traction is proven [unverified fees].

### 2.3 Content licensing matrix

Legend:
- **NC** = non-commercial only.
- **Free-v1** = OK for a free, ad-free, IAP-free app.
- **Monetized** = OK once the app has IAP, ads or a subscription.

| Content | Source | Terms (decisive text) | Free-v1 | Monetized | Bundle offline | Attribution needed |
|---|---|---|---|---|---|---|
| Quran Arabic text (Uthmani / Simple) | Tanzil | "Tanzil Quran Text Copyright (C) 2007-2021 Tanzil Project / License: Creative Commons Attribution 3.0 … Permission is granted to copy and distribute verbatim copies of this text, but CHANGING IT IS NOT ALLOWED … provided that its source (Tanzil Project) is clearly indicated, and a link is made to tanzil.net…" [verified: tanzil.net/docs/text_license] | ✅ | ✅ | ✅ (verbatim; keep notice in data files) | Notice + link to tanzil.net |
| Metadata (juz, hizb, **manzil**, **ruku**, sajda, pages) | Tanzil quran-data.xml | `copyright="(C) 2008-2009 Tanzil.info" license="cc-by"` [verified: tanzil.net/res/text/metadata/quran-data.xml] | ✅ | ✅ | ✅ | CC-BY credit. Indo-Pak Para table is our own (report 05) |
| Indo-Pak script text | DigitalKhatt IndoPak text (digitalkhatt.org repo, MIT) / QF `text_indopak_nastaleeq` | DigitalKhatt repo LICENSE = MIT [verified: github.com/DigitalKhatt/digitalkhatt.org]. QF text is under QF Terms (no prepackaging; Content Sync only) [verified] | ✅ DK | ✅ DK / ⚠️ QF | DK ✅ / QF ❌ (sync only) | DK copyright; "Quran data provided by Quran.Foundation" if QF |
| Indo-Pak **font** (primary) | DigitalKhatt IndoPak | "licensed under the SIL Open Font License, Version 1.1" [verified: name table of QUL CDN file and GitHub copy] | ✅ | ✅ | ✅ (OFL: include licence text; don't sell font alone) | © Amine Anane / Tarteel + OFL text |
| KFGQPC fonts (Hafs, Nastaleeq, surah-name) | KFGQPC | "Permission is hereby granted, Free of Cost … to Use, Copy, Distribute … 1. The Font Software cannot be Sold, Modified, Altered, Translated, Reverse Engineered…" [verified: KFGQPCNastaleeq-Regular.ttf name table]. Older Uthmanic text: "may not be reproduced, modified without the express written approval" [verified: scancode-licensedb] | ✅ unmodified | ⚠️ ("cannot be Sold": keep fonts free and never sold separately) | ✅ unmodified; **no subsetting or WOFF2 re-encoding** | KFGQPC credit + EULA |
| AlQuran IndoPak by QuranWBW (quran.com's font) | QUL CDN | "NOT FOR SALE, NOT FOR MODIFICATION, NOT FOR DISTRIBUTION OR NOT FOR DEVELOPMENT WITHOUT WRITTEN NOTICE BY QURANWBW.COM". Based on "Al Qalam Quran Majeed" (Pakdata) [verified: name table] | ❌ w/o permission | ❌ | ❌ | — |
| Mushaf **layout** data (13/15/16-line) | QUL (Tarteel) | Resource pages show **no licence**. QUL FAQ: "you can use QUL data in commercial projects. However, please review the licensing terms for each resource" and "Some are in the public domain, while others may be subject to specific licenses" [verified: qul.tarteel.ai/faq]. QUL *code* is MIT (not the data) [verified: raw LICENSE] | ⚠️ get email OK | ⚠️ get written OK | ✅ after OK | Credit QUL/Tarteel + edition (e.g. "Taj Company 16-line layout") |
| Indo-Pak **page scans** | Taj / Qudratullah / Pak Co. | Publisher and calligrapher copyright. Taj Company runs its own ad+IAP app `quran.taj.tajquran13line` [verified: Play]. quran_android's Naskh images are "used with permission of SHL Info Systems" [verified: quran_android strings] | ❌ | ❌ | ❌ | — |
| Translations: QuranEnc (Saheeh/Noor Int'l, Hilali-Khan, Rowwad, Junagarhi Urdu, Pashto, Punjabi, Hindi…) | quranenc.com | "Contents of the translations can be downloaded and re-published, with the following terms: 1. No modification… 2. Clearly referring to the publisher and the source (QuranEnc.com). 3. Mentioning the version number… 4. Keeping the transcript information… 5. Notifying the source… of any note… 6. Updating the translation according to the latest version… 7. Inappropriate advertisements must not be included…" [verified: quranenc.com/en/home/api] | ✅ | ✅ (no NC clause; **no inappropriate ads**) | ✅ | Publisher + QuranEnc + version |
| Translations: Tanzil collection (Jalandhry, Kanzul Iman, Maududi, Ahmed Ali, Qadri, Jawadi, Najafi, Pickthall, Yusuf Ali…) | tanzil.net/trans | "The translations provided at this page are for **non-commercial purposes only**. If used otherwise, you need to obtain necessary permission from the translator or the publisher. If you are using more than three … we require you to put a link back to this page" [verified] | ✅ (PD ones even monetized) | ❌ unless PD or permitted | ✅ | Tanzil link (>3), translator |
| Public-domain translations | Kanz-ul-Iman (Ahmed Raza Khan), Mahmud-ul-Hasan + Usmani notes, Jalandhari, Junagarhi (original), Pickthall (1930) | PK term = life + 50 years: "until fifty years from the beginning of the calendar year next following the year in which the author dies" (s.18) [verified: WIPO Lex Copyright Ordinance 1962]. US: published ≤1930 ⇒ PD [unverified rule]. Death dates per report 05 [unverified by me] | ✅ | ✅ | ✅ | Translator credit, edition note |
| The Clear Quran (Khattab) | Furqaan Institute / Book of Signs | "reproducing any verses or excerpts … requires prior written consent" (search summary of theclearquran.org) [unverified: page not opened] | ❌ w/o permission | ❌ | ❌ | — |
| Taqi Usmani (Urdu/English), Maududi Tafheem, Tahir-ul-Qadri, Israr Ahmad, Ahmed Ali | Living or recently deceased authors | Copyright subsists (Maududi d.1979 ⇒ PK PD 1 Jan 2030; Ahmed Ali d.1994 ⇒ 2045) [s.18 verified; death dates unverified] | ❌ w/o permission | ❌ | ❌ | — |
| Tafsir (classical Arabic: Ibn Kathir, Jalalayn, Sa'di…) | QF, QUL, KSU | Text PD, but *digital edition* terms vary. Modern translations (e.g. Darussalam's abridged Ibn Kathir) are copyrighted [unverified] | ✅ PD editions | ✅ PD editions | ✅ | Edition + source |
| Any QF content (text, translations, tafsir, audio URLs) | Quran Foundation API | Monetized apps are explicitly OK: "A Developer may charge for an Application, offer subscriptions or in-app purchases, display advertising, accept donations…" But: "Cache or store QF Content longer than 1 week" is banned unless obtained via Content Sync. "does not itself authorize distributing a prepackaged database", and "recitation rows contain audio URLs, not the underlying recording files" [verified: api-docs.quran.foundation/legal/developer-terms + /docs/tutorials/faq]. A client secret must stay server-side (report 05) | ⚠️ needs our backend | ✅ (with Terms) | ❌ prepackaged; Content Sync only | "Quran data provided by Quran.Foundation" + each edition |
| Audio: QuranicAudio.com | quranicaudio.com | "Mp3s on this site may be downloaded and used for personal use free of charge. However, you may not use these files for commercial purposes…" [verified] | ✅ (stream, attribute) | ❌ (unless via QF Terms → see QF row) | ⚠️ personal | Source + reciter |
| Audio: EveryAyah (per-ayah) | everyayah.com | No licence published. Contact link goes to quran.zendesk.com (Quran.com support) [verified] | ✅ stream, ask | ⚠️ ask | ⚠️ ask | Source + reciter |
| Audio: MP3Quran.net (243 reciters / 289 moshafs via public API v3) | mp3quran.net | Public developer API but **no terms or licence** on the API, About or listing pages [verified: mp3quran.net/eng/api, /eng/about; API count verified] | ✅ stream, ask | ⚠️ ask | ⚠️ ask | Source + reciter |
| Audio: alquran.cloud / islamic.network CDN | alquran.cloud | "Recitations are licensed to us by the reciters or their estates for free, non-commercial redistribution… You may stream, embed and download them for personal and educational use. You may bundle them into a commercial product, but please note that copyrights lie with the reciters and they may ask you to remove the con[t]ent." [verified] | ✅ | ⚠️ (self-contradictory; takedown risk) | ✅ personal | Source + reciter |
| Prayer-time engine | Adhan (batoulapps) | MIT [verified: raw LICENSE adhan-java/adhan-kotlin] | ✅ | ✅ | ✅ | MIT notice |
| quran_android code / data | quran/quran_android | GPL-3.0. README: "Please keep use of this code for non-profit purposes only… the data is licensed under … (typically, this is CC BY-NC-ND…)" [verified] | ✅ (GPL) | ⚠️ legal under GPL, but against the authors' explicit request; data NC-ND | — | GPL notices |

**The coupling (why v1 must be free):**

| If the app has… | Content we lose or must re-license |
|---|---|
| Any IAP, subscription or ads | Tanzil translations (except PD ones), QuranicAudio direct, quran_android data, alquran.cloud audio (moral and takedown risk) |
| Ads of any kind | QuranEnc's "inappropriate advertisements must not be included" is a live constraint. QF "Avoid intrusive advertising" (Connected-Apps guidance) [verified: api-docs.quran.foundation/docs/connected-apps] |
| Our own backend | We can use QF's API legally (a secret server-side) but must implement 7-day Content Sync and their privacy-policy requirements |

### 2.4 Pakistan-specific law and market

**Punjab Holy Quran (Printing and Recording) Act 2011 (Act XIII of 2011)** [verified: wipo.int/wipolex/en/text/497207]:
- **s.2(h):** "record of the Holy Quran" means the Quran "recited in human voice and recorded by any modern means of recording". **Audio is in scope.**
- **s.5(3):** the specimen copy or record must be compared with the standard copy by a qualified person ("Moulvi Fazil or a qualified Qari who is also a Hafiz…"), who records a certificate "that the text and references do not contain any error".
- **s.5(6):** "**No translation of the Holy Quran shall be published or recorded without its Arabic text.**" So there must be **no translation-only reading mode** (the Arabic+translation toggle is fine; "translation only" is not).
- **s.6 (literal distortion)** and **s.7 (translation or interpretation "by a non-Muslim contrary to the belief of Muslims")** apply "whether or not registered".
- **s.9:** "imprisonment for a term which may extend to three years, or with fine of not less than twenty thousand rupees, or with both".
- **Applicability to apps:** the Punjab Quran Board's mandate includes disposal of "electronic digital devices" [verified: auqaf.punjab.gov.pk/node/421]. A reported 2019 Lahore High Court order required Quran Board approval before "uploading onto the internet" [unverified: ProPakistani returned 403]. Treat the app as in scope.

**PTA enforcement precedent.** In Dec 2020 PTA directed Google to remove an "unauthentic version of the Holy Quran uploaded by the Ahmadiyya Community on Google Play Store". It acted under the Removal and Blocking of Unlawful Online Content Rules 2020 and threatened PECA 2016 action [verified: dawn.com/news/amp/1597848].
- **Content-governance rule:** only translations and tafsir by recognized Muslim scholars.
- Offer a sect-balanced Urdu set: Deobandi (Mahmud-ul-Hasan/Usmani), Barelvi (Kanz-ul-Iman), Ahl-e-Hadith (Junagarhi), plus neutral Jalandhari.
- Optionally offer Shia translations (Tanzil hosts Jawadi and Najafi [verified]), clearly labelled and never as default.
- Never offer Ahmadi translations or tafsir.
- Log the provenance of every content file.

**Copyright term in Pakistan:**
- Literary works: life + 50 (s.18).
- Records (sound recordings): "fifty years from the beginning of the calendar year next following the year in which the record is published" (s.20(2)) [verified: WIPO Lex 129350].
- Play distributes globally, so a work that is PD in Pakistan may still be protected in the EU or US. **Ship only works safe in all target markets, or geo-restrict.**

**Data protection.** Pakistan has *no enacted* comprehensive data-protection act as of the latest secondary review. The 2023 bill was withdrawn, and a 2026 bill was reported as "finalized" [unverified: offlist.me aggregator]. Follow GDPR-lite principles anyway, since the EU, UK and Gulf audiences are in scope.

**Payments and payouts:**
- **Merchant support:** Pakistan supports Play developer *and merchant* registration, with default currency PKR, but "Developers in these countries will be paid in USD or EUR" [verified: answer/9306917].
- **Carrier billing:** offered by Jazz (PKR 2,000/month cap reported in 2018), Zong and Ufone [unverified: 2018–2019 news].
- **Tax:** IT-export remittance tax treatment and PSEB registration: consult an accountant [unverified].

**Identity exposure.** For personal accounts: "Google will display your legal name, your country … and developer email … If you decide to monetize on Google Play then Google will display your full address." Organization accounts display legal name, address, email and **phone** [verified: answer/13628312].

### 2.5 Attribution and credits screen: requirements list

Build a **"Sources & Credits"** screen, reachable from Settings → About and from each translation/reciter picker. Drive it from `content_meta.sources_json`, as report 07 proposes.

| # | Item | Exact requirement |
|---|---|---|
| 1 | Tanzil Quran text | Show: "Tanzil Quran Text Copyright (C) 2007-2021 Tanzil Project. License: CC BY 3.0", plus a **link to tanzil.net** and the note "verbatim, unmodified". Keep the notice inside the shipped text file or DB [verified] |
| 2 | Tanzil metadata | "(C) 2008-2009 Tanzil.info, CC BY" [verified] |
| 3 | Tanzil translations (if any, v1 only) | Translator name. If >3 are used, a **link back to tanzil.net/trans** [verified] |
| 4 | QuranEnc translations | Publisher name, "Source: QuranEnc.com", **version number** (e.g. `english_saheeh v1.1.2`). Keep the translator/transcript info. An in-app "report a note" path that we forward to QuranEnc. An update mechanism [verified] |
| 5 | DigitalKhatt IndoPak font | "© 2024 Amine Anane, © 2024 Tarteel Inc." plus the full **SIL OFL 1.1** text [verified] |
| 6 | KFGQPC fonts (if used) | "© King Fahd Glorious Quran Printing Complex", plus the EULA text and "used unmodified" [verified] |
| 7 | QUL layout data | "Mushaf layout data: Quranic Universal Library (Tarteel), <edition name>", per their written OK |
| 8 | Quran Foundation (if used) | "Quran data provided by Quran.Foundation" **wherever QF content is surfaced**. Credit each translation, tafsir and recitation by name [verified: QF FAQ]. Maintain an active Developer Console account if bundling QF fonts or images [verified] |
| 9 | Each reciter | "Recitation: <Qari name> · Source: <QuranicAudio / EveryAyah / MP3Quran / islamic.network>" in the player info sheet and in Credits. **Text only, no photos** |
| 10 | Tafsir editions | Author, edition, translator, source |
| 11 | Open-source libraries | Auto-generated list (AboutLibraries, report 07), including Apache-2.0 NOTICE texts and MIT notices (Adhan, DigitalKhatt) |
| 12 | Proofreading certificate | "Arabic text proof-read by <Hafiz/Qari name, credentials>, <date>" (Punjab Act s.5(3) practice) |
| 13 | Disclaimers | Prayer times are calculated (method and madhab shown) and users should verify with their local masjid. Qibla accuracy depends on the sensor. Translations are human interpretations, not the Quran |
| 14 | City database (if offline) | e.g. GeoNames: CC BY 4.0 [unverified licence], credit as required |

### 2.6 Google Play compliance checklist (as of 2026-10-19)

| Area | Requirement (source) | Our design / action |
|---|---|---|
| **Target API** | "Starting August 31 2026: New apps and app updates must target Android 16 (API level 36) or higher". Existing apps need API 35+. Extension to Nov 1 2026 [verified: developer.android.com/google/play/requirements/target-sdk, updated 2026-10-16] | Already `targetSdk 36` ✅. Plan API 37 for Aug 2027 [unverified forecast] |
| **Android 16 large screens** | For targetSdk 36, "orientation, resizability, and aspect ratio restrictions no longer apply on displays with smallest width >= 600dp" [verified: behavior-changes-16] | The "orientation lock" icon works on phones only. Tablets and foldables get a responsive two-page spread |
| **New personal account testing** | Closed test with "a minimum of 12 testers who have been opted in continuously for at least 14 days" before production access (accounts after Nov 13 2023) [verified: answer/14151465] | Prefer an Organization account. Otherwise recruit ≥15 testers (family, masjid, WhatsApp group) and run 14+ days |
| **Account info** | Personal or Organization; D-U-N-S is free; the monetization address is displayed [verified: answer/13628312]. Fee US$25 one-time [verified: answer/6112435] | Register a sole-proprietorship or SMC company, get a D-U-N-S, open an Organization account |
| **Android developer verification** | Protections start Sep 30 2026 in Brazil, Indonesia, Singapore and Thailand, globally in 2027. 99% of Play apps are auto-registered. Register packages and keys for off-Play distribution [verified: developer.android.com/developer-verification] | Check Play Console Home → app registered. Register the F-Droid/GitHub signing key SHA-256 under the same package |
| **Privacy policy** | Required for *all* apps, in the Console **and in-app**, at an "active, publicly accessible and non-geofenced URL (no PDFs)" [verified: answer/10144311] | Static page on GitHub Pages or our domain; in-app link in Settings and onboarding |
| **Data safety form** | Mandatory even if nothing is collected. Includes SDK data. "On-device access/processing … does not need to be disclosed". Ephemeral processing still goes in the form. "Approximate location that is inferred, such as via IP address … must be disclosed" [verified: answer/10787469] | v1: "No data collected / No data shared" (§2.8). Keep an SDK inventory in the repo |
| **Location** | "request the minimum scope necessary (for example, coarse instead of fine, and foreground instead of background)". "Don't request device location for apps directed at children" [verified: answer/9888170]. **Upcoming 2027-01-27:** "location button as the recommended minimum scope for precise location" [verified: answer/12253906] | `ACCESS_COARSE_LOCATION` only, one-shot and foreground, plus a manual city picker. Prayer times and Qibla are computed on device (Adhan) |
| **Notifications** | Runtime `POST_NOTIFICATIONS` on Android 13+ [unverified detail; standard platform rule] | Ask in context (when the user enables an adhan or daily reminder), not at first launch |
| **Exact alarms** | "USE_EXACT_ALARM is a restricted permission … only if their core functionality supports the need for an exact alarm" (alarm/timer/calendar apps) [verified: answer/9888170]. `SCHEDULE_EXACT_ALARM` "is not pre-granted to fresh installs of apps targeting Android 13" and the user must grant it [verified: developer.android.com/develop/background-work/services/alarms] | Declare **`SCHEDULE_EXACT_ALARM` only**. Check `canScheduleExactAlarms()`. If denied, fall back to inexact `setWindow` (±a few minutes) with a banner explaining it |
| **Full-screen intent** | Auto-granted only to calling or alarm core apps, with a declaration [verified: answer/13392821] | Don't declare `USE_FULL_SCREEN_INTENT`. Use a high-priority notification for the adhan |
| **FGS: media playback** | Android 14+ declares the type plus permission. In the Play Console declaration, per type, "Include a link to a video demonstrating each foreground service feature". For mediaPlayback, "Show a user starting video or audio playback. Playback must be user-initiated" [verified: answer/13392821] | Media3 `MediaSessionService` with `foregroundServiceType="mediaPlayback"`. Record a 30-s unlisted YouTube demo |
| **FGS: downloads** | dataSync FGS runs at most "6 hours in a 24-hour period" on Android 15+ [verified: …/fgs/timeout]. Google recommends UIDT for user-requested network transfers [verified: answer/13392821]. UIDT requires `RUN_USER_INITIATED_JOBS` [verified: Android 14 UIDT doc] | Android 14+: UIDT, no FGS declaration. API 24–33: WorkManager long-running worker with `dataSync`, declared with a video showing the user tapping "Download surah/para" (report 06) |
| **Geofencing FGS** | Being removed as an approved FGS use case on 2027-01-27 [verified: answer/12253906] | Not used |
| **Families** | If the target audience includes children: certified ad SDKs only; no AAID, location or non-approved SDKs for children [verified: answer/9893335] | Target audience **13+**. No "ideal for kids" copy (the reference app says "Ideal for kids, adults…" [verified: Play description]). A Noorani Qaida/kids product should be a *separate* Families-compliant app later |
| **Ads policy** | "Full screen interstitial ads … that show unexpectedly … are not allowed". No ads "during the beginning of a content segment". Close within 15 s. No repeated interstitials [verified: answer/9857753] | **No ads.** If ever: house or sponsor cards only, never in the reader or player |
| **Payments** | Digital goods and app functionality must use Play Billing. Billing "must not be used" for "tax exempt donations". Apps "may not lead users to a payment method other than Google Play's billing system" unless an exception applies [verified: answer/9858738]. Enforcement: AnkiDroid's donation link was refused despite 501(c)(6) proof (Sep 2026) [verified: gigazine.net] | v1: no payments. Later: Play Billing "Support" IAP. Donation links only to an FBR-approved NPO, with documents sent to Play support before release |
| **Service fee** | 15% on the first $1M per year, 15% on auto-renew subscriptions [verified: answer/112622] | Model revenue net of 15% |
| **Account deletion** | Required only "If your app allows users to create an account": in-app plus a web link [verified: answer/13327111] | v1 has no accounts ⇒ N/A. v2 sync accounts need both paths |
| **Metadata** | ≤30-char title, no emojis/caps/ranking claims, no testimonials [verified: answer/9898842] | Clean listing, localized `ur-PK` and `en` |
| **Content rating, target audience, ads declaration, Advertising-ID declaration, financial features declaration, government-apps declaration** | App content page items [unverified: from memory, check in Console] | Ads: "No". AD_ID: not used (strip the permission). Financial features: none |
| **Contacts** | New Contacts Permissions policy from 2027-01-27 [verified: answer/12253906] | Not used |

**Manifest permission plan (v1):**

```xml
<manifest xmlns:tools="http://schemas.android.com/tools">
  <uses-permission android:name="android.permission.INTERNET"/>
  <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
  <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>            <!-- runtime, API 33+ -->
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK"/> <!-- Play FGS declaration + video -->
  <uses-permission android:name="android.permission.RUN_USER_INITIATED_JOBS"/>       <!-- UIDT downloads, API 34+ -->
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC"/>  <!-- only for API<34 worker; declare + video -->
  <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM"/>          <!-- user-granted; NOT USE_EXACT_ALARM -->
  <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>        <!-- re-arm prayer alarms -->
  <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>        <!-- no FINE, no BACKGROUND -->
  <!-- defensive: strip anything a transitive SDK might merge -->
  <uses-permission android:name="com.google.android.gms.permission.AD_ID" tools:node="remove"/>
  <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" tools:node="remove"/>
  <uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" tools:node="remove"/>
</manifest>
```

### 2.7 Privacy and analytics options

| Option | Data leaving device | Play Data safety impact | F-Droid | Cost | Verdict |
|---|---|---|---|---|---|
| **None + Play Console Android vitals** | None from our app. Google collects crash/ANR data from users "who have opted in to automatically share usage and diagnostics data" [verified: answer/9844486] | "No data collected" | ✅ | $0 | **v1 default** |
| **ACRA (Apache-2.0)** [verified: raw LICENSE] with user-confirmed dialog → email or self-hosted endpoint | Stack trace, device model, app version (opt-in) | Crash logs + Diagnostics: *collected, optional*, not shared, encrypted in transit | ✅ | $0 | **v1.1 add-on**: "Send crash report?" dialog |
| Sentry SaaS (Developer plan: 1 user, 5k errors/mo free) [verified: sentry.io/pricing] or self-hosted | Crash and perf data to Sentry | Crash logs, Diagnostics, possibly Device IDs | ⚠️ needs FOSS flavor | $0 → paid | Only if ACRA proves insufficient. Opt-in only |
| Firebase Crashlytics (no-cost) [verified: firebase.google.com/pricing] | Stack traces, app state, device metadata, **Crashlytics installation UUID**; pulls in Firebase installations and sessions SDKs [verified: firebase.google.com/docs/android/play-data-disclosure] | Crash logs, Diagnostics, Device or other IDs | ❌ forbidden ("Firebase and Crashlytics … strictly forbidden") [verified: f-droid.org Inclusion Policy] | $0 | Avoid: needs a Play-only flavor, and erodes the "no tracking" claim |
| Firebase/Google Analytics | Events, app-instance ID, inferred location | App interactions, Device IDs, approximate location | ❌ | $0 | **Do not use.** Reading which surahs or pages someone opens is *religious data* (special-category under GDPR). QF's privacy rules require explicit, separate consent for religious data [verified: api-docs.quran.foundation/legal/developer-privacy] |

**Principles to write into the privacy policy and the code:**
1. **Location stays on device.** It is used once for prayer and Qibla computation and stored as a rounded city coordinate. We prefer an offline city DB over the platform Geocoder, so no location goes to a network service.
2. **Bookmarks, last-read, khatam progress and tasbeeh counts are local only** (Room). Android Auto Backup is allowed but disclosed. Cloud sync (v2) is opt-in with explicit consent for "religious practice data".
3. **No advertising ID** and no fingerprinting. **Content CDNs** (QuranEnc, EveryAyah, MP3Quran and so on) are listed in the policy with links to their privacy policies, because they see the user's IP address.
4. **A child-safe posture without being a children's app:** no user-generated content, no chat, no social features in v1.
5. **Ethics guidance (from QF's Connected-Apps rules):**
   - Avoid "hasanat counters and similar features that assign numerical spiritual rewards".
   - Streaks must not "shame users, or pressure them".
   - Avoid "manipulative countdowns, push notifications, scarcity claims, and social comparison around acts of worship".
   - AI must "not rewrite or replace Quranic Arabic, named translations, tafsir quotations", must label generated output, and must "not present AI output as binding religious authority, fatwas" [verified: api-docs.quran.foundation/docs/connected-apps].

   Adopt these as product principles even without using QF.

**Data safety draft:**

| Version | Data types | Collected? | Shared? | Optional? | Purpose | Encrypted in transit |
|---|---|---|---|---|---|---|
| v1 (no SDKs, no accounts) | — | No | No | — | — | — (all HTTPS anyway) |
| v1.1 (+ ACRA opt-in) | Crash logs; Diagnostics | Yes | No | **Optional** | Analytics (crash diagnosis) | Yes |
| v2 (+ opt-in cloud sync) | Email or user ID; App activity (bookmarks, notes, reading progress) | Yes | No (processor only) | **Optional** | App functionality (sync) | Yes. Add account deletion in-app and on the web |

### 2.8 Monetization and sustainability

**Market reality** (Play, country = PK) [verified: google-play-scraper app()]:

| App | Installs | Ads | IAP | Model signal |
|---|---|---|---|---|
| Reference: Al QURAN – القرآن الكريم (9D) | 50M+ (70.7M real), 4.64★, 1.14M ratings | Listing says no; APK has AdMob and AppsFlyer (report 01) | Rs 280–6,900 | Paywalled reciters plus ads. **53% of recent 1–2★ reviews cite ads** (report 01) |
| Quran for Android (quran.com) | 50M+ | No | No | Donation-funded nonprofit |
| Al Quran (Tafsir & by Word), Greentech | 10M+, **4.82★** | No | No | Nonprofit foundation |
| Quran Majeed (Pakdata) | 10M+ | Yes | Rs 105–180,800 | Ads plus heavy IAP |
| Islam360 | 10M+ | Yes | Rs 49–40,000 | Ads plus IAP |
| Holy Quran 13 Line Taj Company | 100K+ | Yes | Rs 300–800 | Publisher monetizing its own scans |

The highest-rated apps (Greentech 4.82★, Quran for Android) are **free with no ads or IAP**. The biggest complaint about the reference app is ads. **Ad-free is the wedge.**

| Option | Play policy fit | Content-rights impact | Revenue potential (PK) | Trust/ethics | Verdict |
|---|---|---|---|---|---|
| **Fully free, ad-free (sadaqah jariyah)** | Simplest; no billing | Unlocks NC sources (Tanzil translations, personal-use audio) | $0 | Best | **v1** |
| **Play Billing "Support" consumable IAP** (e.g. PKR 300 / 1,000 / 3,000), optional cosmetic thanks (themes) | Allowed (digital goods via Play Billing) [verified: payments policy] | Makes the app commercial ⇒ drop NC sources first (§2.3) | Low–medium (Pakistani willingness to pay is low; diaspora is higher) [unverified] | Good if never gating Quran or audio | **v1.5**, after the content audit |
| **External donation link** (JazzCash, Easypaisa, bank, Patreon) | ❌ unless the recipient is a *verified tax-exempt* org. AnkiDroid precedent [verified] | — | Medium | Good | Only via an FBR-approved NPO (s.2(36) ITO 2001 [unverified]). Pre-clear with Play support |
| **Freemium: premium = cost-bearing features** (cloud sync across devices, AI tilawat/hifz mistake detection, teacher dashboards for hifz classes) | Play Billing subscription (15%) [verified] | Commercial ⇒ commercial-OK content only. QF content needs Terms compliance | Medium–high (hifz parents and madrasas) | OK if reading and listening stay 100% free | **v2** |
| **Sponsorship** (masjid, trust or company sponsors a reciter pack, translation or Ramadan feature; "Sponsored by X" in Credits) | Not an ad network; must not be misleading. QF: "Avoid … unclear sponsorship" [verified] | Commercial-ish ⇒ treat as monetized | Medium (B2B, Ramadan) | OK with clear labelling | v2 opportunistic |
| **House or ethical ads** (only own or sponsor cards, outside the reader) | Allowed. Must not be interstitial or intrusive [verified] | QuranEnc: no inappropriate ads [verified] | Low | Risky perception | Avoid unless needed |
| **Ad networks (AdMob etc.)** | Allowed but Families, AD_ID and Data safety burdens | Breaks NC sources; QuranEnc ad clause; QF discourages | Medium | Poor (the reference app's #1 complaint) | **Never** |

**Cost model** (monthly, unless noted):

| Item | Free v1 (stream public CDNs) | Self-hosted audio (licensed reciters) | v2 with sync and AI |
|---|---|---|---|
| Play Console | US$25 one-time [verified] | same | same |
| D-U-N-S | $0 [verified] | — | — |
| Domain plus privacy-policy hosting (GitHub or Cloudflare Pages) | ~$10–15/yr [unverified] | same | same |
| Audio storage | $0 | **R2 Standard:** $0.015/GB-mo after **10 GB free**; e.g. 20 reciters × ~1 GB gapless ≈ 20 GB ⇒ ≈ $0.15/mo. Surah 2 by Ghamdi on mp3quran is 84.7 MB, so a full Quran at 128 kbps is ≈ 1 GB [verified HEAD; extrapolated] | same |
| Audio egress | $0 (their bandwidth; ethically, cache aggressively) | **$0 egress** on R2 [verified]. Class B reads: 10M/mo free, then $0.36/M [verified]. Must use a **custom domain** (r2.dev is "for non-production use cases") [verified] | same |
| Backend | none | none | Supabase Free: 50k MAU, 500 MB DB, "paused after 1 week of inactivity"; **Pro $25/mo** [verified: supabase.com/pricing]. Or Firebase Spark: Firestore 1 GiB, 50K reads/day, 20K writes/day [verified: firebase.google.com/pricing] |
| Crash reporting | $0 (vitals / ACRA) | $0 | Sentry free 5k errors/mo [verified] |
| AI hifz checker | — | — | On-device model preferred (≈$0 marginal). A server ASR is a per-minute cost [unverified] |
| **Total** | **≈ $25 + ~$1/mo** | **≈ $1–5/mo** | **≈ $25–60/mo** |

### 2.9 Open-source strategy

| Option | Trust | Code reuse allowed | F-Droid / IzzyOnDroid | Protection vs reskins | Commercial flexibility |
|---|---|---|---|---|---|
| **GPL-3.0-or-later** (recommended) | High: no-tracking claims are verifiable | GPL-3 (quran_android, AlfaazPlus/QuranApp per report 03), Apache-2.0 (quran-ios QuranEngine), MIT (DigitalKhatt, Adhan) | ✅ with a FOSS build flavor (no Play Billing / GMS). Non-free assets (KFGQPC fonts) ⇒ "Non-Free Assets" anti-feature. DigitalKhatt OFL is fine [verified: f-droid.org Anti-Features, Inclusion Policy] | Copyleft gives **DMCA leverage** against closed, ad-stuffed clones (Play "will respond to clear notices of alleged copyright infringement" [verified]). Trademark blocks name and icon reuse | IAP and subscriptions remain legal. Server code can stay proprietary. White-label needs a CLA |
| Apache-2.0 | High | Apache/MIT only. **Cannot** absorb GPL code | ✅ | Weak: anyone may close-source a reskin; only trademark protects | Maximum (proprietary forks, licensing to orgs) |
| Closed source | Low–medium | Apache/MIT only | ❌ | Copyright only | Maximum |

Notes:
- **quran_android's README asks** that its code be used "for non-profit purposes only", and calls monetized reuse "stealing from the work of the contributors" [verified]. This is not a licence term, but it is a reputational one. If we ever reuse its code, stay ad-free and IAP-free, or rewrite.
- **F-Droid also forbids** "Google Play Services and Firebase and Crashlytics" in builds [verified]. Keep a `foss` product flavor without Play Billing (Play Billing is GMS-bound).
- **Trademark policy file** (`TRADEMARKS.md`): the code is GPL, but the brand name, logo and store listing are *not* licensed. Forks must rename.
- **Contributions:** use DCO sign-off if staying pure GPL. Use a short CLA (contributor grants relicensing rights) if the owner wants to keep the option of dual-licensing or Apache relicensing later.

---

## 3. Recommendations

1. **Positioning: "Typeset, ayah-aware Indo-Pak mushaf · no ads · no tracking · free (sadaqah jariyah)".** Ship one app, never per-surah clones. *Rationale:* it directly answers the Repetitive Content and minimum-functionality policies and the reference app's #1 complaint (ads, 53% of negative reviews). The top-rated competitors are the ad-free nonprofits. **Confidence: High.**
2. **Brand.** Keep `Criterion` as codename and package. Launch publicly as **"Hamdam"** (or another name that passes a trademark search) with the title pattern `Hamdam: Quran 13 Line & Audio`. Use our own icon, art, screenshots and copy. Never use "Official", reciter names or Saudi/KFGQPC logos in the title or icon. *Rationale:* Impersonation and Metadata policy; ASO in Urdu/English; "Criterion" collisions and zero relevance. **Confidence: Medium** (brand choice is subjective; the search is still to be done).
3. **v1 is free, ad-free and IAP-free, with no accounts and no third-party SDKs.** Design every content source as a swappable, manifest-driven pack with a `license` field (report 07's manifest) so we can re-license before monetizing. *Rationale:* NC sources are usable now, and there is no Data safety, Families, ads or billing burden. **Confidence: High.**
4. **Mushaf rendering.**
   - **Do:** DigitalKhatt IndoPak (OFL) plus QUL 16-line Taj layout (and 13/15-line). Email Tarteel/QUL for written permission for the layout and text resources.
   - **Avoid:** scans; the QuranWBW font unless permission is granted.
   - **If KFGQPC fonts are used:** ship them byte-identical, with no subsetting.

   *Rationale:* clean licences and better UX (reports 03 and 04). **Confidence: High.**
5. **Default translations.**
   - **English:** Saheeh/Noor International (QuranEnc), Pickthall (PD).
   - **Urdu:** Junagarhi (QuranEnc), Jalandhari, Kanz-ul-Iman and Mahmud-ul-Hasan (PD).

   Treat Tanzil-only translations as v1-only. Send permission requests now (template in §4) to Furqaan Institute (Clear Quran), Maktaba Ma'ariful Quran (Taqi Usmani), Idara Tarjuman-ul-Quran (Maududi) and Minhaj (Qadri). *Rationale:* §2.3 matrix. **Confidence: High** for QuranEnc/PD; **Medium** for the exact PD status (verify death and publication dates per edition).
6. **Audio sourcing.**
   - **v1:** stream from EveryAyah (gapped), MP3Quran and alquran.cloud (gapless with timings) with on-screen attribution. Cache for personal offline use.
   - **Before public launch:** email each provider for written permission to stream and download from a free app.
   - **Self-host on R2** only reciters we have in writing (approach the reciters' offices and Pakistani qaris directly, e.g. Qari Sadaqat Ali).
   - **Never** hotlink QuranicAudio or QF from a monetized build without a QF Terms-compliant backend and written clarification on offline audio.
   - **No reciter photos.**

   **Confidence: Medium** (the providers' stance is unknown).
7. **Pakistan compliance pack.**
   - (a) A qualified Hafiz/Qari proofreads the Arabic per layout, and a dated certificate goes in Credits.
   - (b) No translation-only mode; Arabic is always shown with any translation (Punjab Act s.5(6)).
   - (c) A written content-governance policy: recognized Muslim scholars only; sect-balanced Urdu set; Shia options labelled and opt-in; Ahmadi translations and tafsir excluded; provenance log.
   - (d) An in-app "Report a text error" flow with a 48-hour fix SLA.

   *Rationale:* Punjab Act 2011 ss.5–9 (up to 3 years' imprisonment) and the PTA precedent. **Confidence: High.**
8. **Developer account.** Create a Play **Organization** account, after registering a sole-proprietorship or SMC and getting a free D-U-N-S. Otherwise use a personal account and plan the 12-tester × 14-day closed test, keeping the app unmonetized so the home address is not displayed. Verify Android developer verification registration, and register the F-Droid/GitHub key. **Confidence: High.**
9. **Permissions and declarations exactly as in §2.6:**
   - `SCHEDULE_EXACT_ALARM` with inexact fallback, not `USE_EXACT_ALARM`;
   - coarse location only;
   - no full-screen intent;
   - Media3 `mediaPlayback` FGS with a demo video;
   - UIDT downloads on API 34+, WorkManager with `dataSync` below;
   - target audience 13+;
   - strip `AD_ID`.

   **Confidence: High.**
10. **Privacy stack.** Use no SDK plus Android vitals in v1, add ACRA opt-in crash reports in v1.1, and never use product analytics on reading content. Publish a plain-language privacy policy (outline below) on our domain, linked in-app and in the Console. File the Data safety form as "No data collected". *Rationale:* honest Data safety is a differentiator against the reference app's contradiction, and religious data is special-category. **Confidence: High.**
11. **Monetization roadmap.**
    - v1: free.
    - v1.5: an optional Play Billing "Support development" IAP with cosmetic thanks, only after the commercial-content audit.
    - v2: a premium subscription only for features that cost money (cloud sync, AI hifz checker, teacher/madrasa tools). Reading, listening, indexing and downloads stay free forever.
    - **Never** ads in the reader or player, and never ad networks.
    - Donations only through a partner FBR-approved NPO, pre-cleared with Play.

    **Confidence: Medium** (willingness to pay in Pakistan is untested).
12. **Licence: GPL-3.0-or-later for the client,** with `TRADEMARKS.md` and DCO or a light CLA. Add a `foss` flavor for F-Droid and IzzyOnDroid. Keep any backend under AGPL or proprietary. Consider Apache-2.0 only if B2B white-labelling is a real goal. Still follow report 03: build clean-room and take the GPL code only where it saves real time. **Confidence: Medium.**
13. **Credits screen** as specified in §2.5, generated from pack manifests plus AboutLibraries. Add a CI check that fails the build if any shipped pack lacks `license` and `attribution` fields. **Confidence: High.**
14. **Re-check policy deadlines quarterly** (the Play "Policy Deadlines" page). Known items: location-button policy and contacts policy on 2027-01-27; target API 37 around Aug 2027 [unverified]. **Confidence: High.**

**Privacy policy outline (v1):**
1. Who we are (legal name, contact).
2. Summary: "We don't collect, sell or share personal data. No ads, no trackers."
3. Data on your device: bookmarks, progress, settings, rounded location for prayer times; Android backup.
4. Network requests: content and audio CDNs (list them with their privacy-policy links); they receive your IP address and the file requested.
5. Permissions and why (location, notifications, exact alarms).
6. Optional crash reports (v1.1): what is sent and how to turn it off.
7. Children (not directed to under-13s).
8. Your rights and contact.
9. Changes and effective date.

**Permission-request email template (content owners):**

> Subject: Permission request: <work> in a free, ad-free Quran app (<Brand>, Android)
> Assalamu alaikum. We are building <Brand>, a free, ad-free, open-source Quran reader for South-Asian Muslims. We request permission to (a) display <work/recitation> verbatim alongside the Arabic text, (b) let users download it for offline use within the app, (c) credit you as: "<credit line>". We will not modify, sell or separately redistribute it, and will apply your corrections. [If applicable: the app may later offer optional paid features unrelated to your content; please indicate whether your permission extends to that.] JazakAllah khair.

---

## 4. Risks and open questions

**Risks:**
1. **Audio takedown risk.** Recordings are owned by reciters and labels. Play's IP policy lists "Streaming apps that allow users to download a local copy of copyrighted content without authorization" as a violation. A single DMCA notice could pull the app. *Mitigation:* written permissions, swappable catalogue, fast takedown process.
2. **QUL layout and text licences are unstated.** If Tarteel declines, we fall back to the DigitalKhatt MIT text with its own 15-line layout, or derive our own pagination from public-domain print editions (effort risk).
3. **Punjab Act scope ambiguity.** The Act may be read to require Quran Board approval or registration for digital publishers. A complaint (sectarian or competitor-driven) could trigger PTA action. *Mitigation:* proofreading certificate, governance policy, optionally a voluntary Quran Board review.
4. **The donations policy is opaque** (AnkiDroid case). Even a genuine NPO partner may be rejected, so don't build the business on in-app donation links.
5. **"Commercial" ambiguity:** whether a voluntary support IAP makes the app "commercial" under NC terms (Tanzil, QuranicAudio). Treat it as yes.
6. **KFGQPC "cannot be Sold" plus a paid tier:** low but non-zero risk if KFGQPC fonts are bundled in a monetized app. DigitalKhatt avoids it.
7. **Trademark:** "Hamdam" or any brand may conflict with an existing Pakistani registration (not yet searched).
8. **Policy drift:** Play changes policies about quarterly. Android developer verification goes global in 2027, and F-Droid flags this as a threat to sideloading ("F-Droid is under threat") [verified: f-droid.org banner].
9. **Sectarian backlash:** the choice of default translation can trigger 1★ campaigns. Use neutral defaults (Jalandhari or Junagarhi) and let users choose at onboarding.
10. **Reference-app retaliation:** a trademark or copyright complaint from 9D Muslim Apps is unlikely if we copy nothing protectable. Keep a design-provenance log (our own sketches, fonts, data sources) as evidence.

**Open questions:**
1. Will Tarteel/QUL confirm in writing that we may bundle the "Indopak 16 lines layout (Taj company)" data and the DigitalKhatt IndoPak text in a free app, and in a monetized one?
2. Will EveryAyah (Quran.com support), MP3Quran.net and islamic.network grant written permission for streaming and offline download in a free app? Which reciters allow self-hosting?
3. Does the QF Terms' 1-week storage cap apply to *audio files* downloaded via QF recitation URLs? (Ask QF developer relations.)
4. Exact death and first-publication dates for each "public-domain" Urdu translation edition we ship (Jalandhari, Kanz-ul-Iman, Mahmud-ul-Hasan, Junagarhi original) and their digital-text provenance (Tanzil NC versus our own OCR or PD scans).
5. Is a Punjab Quran Board / Muttahida Ulema Board review available, or required, for apps? Fees and timeline? (Contact Auqaf Punjab.)
6. Business entity: sole proprietorship versus SMC-Pvt for a D-U-N-S and Organization account; tax treatment of Play USD payouts in Pakistan.
7. Does the owner want the B2B or white-label option (which favours Apache-2.0 or a CLA), or community trust (pure GPL)?
8. Is a kids product (Noorani Qaida) wanted? If yes, plan it as a separate Families-compliant app.

---

## 5. Sources

| URL | What I verified there |
|---|---|
| https://tanzil.net/docs/text_license | Full Tanzil text terms: CC BY 3.0, verbatim only, link to tanzil.net, keep notice |
| https://tanzil.net/trans/ | "non-commercial purposes only" translation terms; link-back if >3; list of Urdu/English translations incl. Shia (Jawadi, Najafi) |
| https://tanzil.net/res/text/metadata/quran-data.xml | Metadata header `copyright="(C) 2008-2009 Tanzil.info" license="cc-by"` |
| https://qul.tarteel.ai/faq | QUL commercial-use answer; "resources … vary in their copyright status"; no API |
| https://qul.tarteel.ai/credits | Contributors (KFGQPC, Tanzil, EveryAyah, QuranicAudio, Ayman Siddiqui, DigitalKhatt) |
| https://qul.tarteel.ai/resources/mushaf-layout (+ /12, /15, /236, /313) | Indo-Pak layouts (Taj 13-line 847 pp, Qudratullah 13-line 849 pp, 15-line 610 pp); no licence text |
| https://raw.githubusercontent.com/TarteelAI/quranic-universal-library/main/LICENSE | QUL code is MIT (Tarteel, Inc.) |
| https://static-cdn.tarteel.ai/qul/fonts/dk/DigitalKhattIndoPak.otf | Name table: © 2024 Amine Anane, © 2024 Tarteel Inc., SIL OFL 1.1 |
| https://static-cdn.tarteel.ai/qul/fonts/nastaleeq/Hanafi/normal-v4.2.2/with-waqf-lazmi/font.ttf | Name table: "AlQuran IndoPak by QuranWBW … NOT FOR SALE, NOT FOR MODIFICATION, NOT FOR DISTRIBUTION…" |
| https://static-cdn.tarteel.ai/qul/fonts/nastaleeq/KFGQPCNastaleeq-Regular.ttf | KFGQPC EULA: use, copy, distribute; cannot be sold or modified |
| https://scancode-licensedb.aboutcode.org/kfgqpc-uthmanic-script-hafs.LICENSE | KFGQPC Uthmanic licence text |
| github.com/DigitalKhatt/mushaf-android (git clone) | MIT repo; digitalkhatt.otf is OFL 1.1 |
| github.com/DigitalKhatt/digitalkhatt.org (git clone) | MIT LICENSE; IndoPak fonts OFL 1.1 |
| github.com/quran/quran_android (git clone) | GPL-3.0; README "non-profit purposes only", data "typically CC BY-NC-ND"; credits strings; manifest FGS types (mediaPlayback, dataSync) |
| https://raw.githubusercontent.com/quran/quran.com-images/master/README.md | Fonts and pages belong to KFGQPC; code GPL |
| https://api-docs.quran.foundation/legal/developer-terms/ (updated 2026-10-11) | QF licence grant; monetized apps allowed; no resale or redistribution; 1-week cache; prepackaging; font and image bundling with attribution |
| https://api-docs.quran.foundation/legal/developer-privacy/ | Privacy requirements incl. religious-data explicit consent, sub-processor disclosure |
| https://api-docs.quran.foundation/docs/tutorials/content-sync/getting-started/ | Content Sync groups; recitations rows |
| https://api-docs.quran.foundation/docs/tutorials/faq/ | "recitation rows contain audio URLs, not the underlying recording files"; attribution text |
| https://api-docs.quran.foundation/docs/connected-apps/ | Attribution; ethics (hasanat counters, streaks, ads, AI rules) |
| https://api-docs.quran.foundation/docs/content_apis_versioned/4.0.0/chapter-reciter-audio-file/ | QF audio_url hosted on download.quranicaudio.com |
| https://quranicaudio.com/about | "personal use … may not use these files for commercial purposes" |
| https://everyayah.com/ and /old_index.html | No licence; contact = quran.zendesk.com; recitations.js |
| https://mp3quran.net/eng/api, https://www.mp3quran.net/eng/about, https://www.mp3quran.net/api/v3/reciters?language=eng | No terms; 243 reciters / 289 moshafs; Ghamdi 002.mp3 = 84,673,609 bytes (HEAD) |
| https://alquran.cloud/terms-and-conditions | Recitation terms (personal/educational; may bundle commercially; reciters may request removal) |
| https://quranenc.com/en/home/api, https://quranenc.com/api/v1/translations/list/en, …/ur | 7 republishing conditions; english_saheeh = Noor International v1.1.2; urdu_junagarhi v1.1.3 |
| https://support.google.com/googleplay/android-developer/answer/9888374 | Impersonation policy text |
| https://support.google.com/googleplay/android-developer/answer/9888072 | IP policy text and examples |
| https://support.google.com/googleplay/android-developer/answer/9899034 | Spam: Repetitive Content, Webview spam |
| https://support.google.com/googleplay/android-developer/answer/9898783 | Minimum functionality (text-only or PDF apps) |
| https://support.google.com/googleplay/android-developer/answer/9898842 | Metadata policy (30 chars, no emojis or rank claims) |
| https://support.google.com/googleplay/android-developer/answer/9888170 | Exact Alarm and Location permission policies |
| https://support.google.com/googleplay/android-developer/answer/13392821 | FGS declaration, video requirements, UIDT recommendation, full-screen intent |
| https://support.google.com/googleplay/android-developer/answer/12253906 | Policy deadlines (2027-01-27 location button, contacts, geofencing FGS; 2026-09-30 app registration) |
| https://support.google.com/googleplay/android-developer/answer/10787469 | Data safety rules (SDKs, ephemeral, on-device, IP-inferred location) |
| https://support.google.com/googleplay/android-developer/answer/10144311 | Privacy policy must be in-app and in the Console, non-geofenced, not PDF; prominent disclosure |
| https://support.google.com/googleplay/android-developer/answer/13327111 | Account deletion requirement |
| https://support.google.com/googleplay/android-developer/answer/9893335 | Families requirements (AAID, location, certified ad SDKs) |
| https://support.google.com/googleplay/android-developer/answer/9857753 | Ads policy (interstitials, 15 s, content segments) |
| https://support.google.com/googleplay/android-developer/answer/9858738 | Payments policy (Play Billing; tax-exempt donations excluded; no steering) |
| https://support.google.com/googleplay/android-developer/answer/9306917 | Pakistan supports merchant registration; PKR; paid in USD/EUR |
| https://support.google.com/googleplay/android-developer/answer/112622 | 15% / 30% service fees |
| https://support.google.com/googleplay/android-developer/answer/6112435 | US$25 registration fee |
| https://support.google.com/googleplay/android-developer/answer/13628312 | Personal vs organization info; address display when monetizing; D-U-N-S free |
| https://support.google.com/googleplay/android-developer/answer/14151465 | 12 testers × 14 days |
| https://support.google.com/googleplay/android-developer/answer/9844486 | Android vitals data from opted-in users |
| https://developer.android.com/google/play/requirements/target-sdk | API 36 from Aug 31 2026; extension to Nov 1 2026 |
| https://developer.android.com/developer-verification (+ /guides/open-source-app-registration) | Enforcement timeline; open-source app key registration |
| https://developer.android.com/develop/background-work/services/fgs/timeout | dataSync 6 h per 24 h |
| https://developer.android.com/develop/background-work/services/alarms | SCHEDULE_EXACT_ALARM not pre-granted (API 33+) |
| https://developer.android.com/about/versions/14/changes/user-initiated-data-transfers | RUN_USER_INITIATED_JOBS permission |
| https://developer.android.com/about/versions/16/behavior-changes-16 | Orientation and resizability ignored at sw ≥ 600dp |
| https://f-droid.org/en/docs/Inclusion_Policy/, https://f-droid.org/en/docs/Anti-Features/ | Firebase and Crashlytics forbidden; asset licences; Non-Free Assets anti-feature |
| https://www.wipo.int/wipolex/en/text/497207 | Punjab Holy Quran (Printing and Recording) Act 2011 full text (ss.2, 5, 6, 7, 9) |
| https://www.wipo.int/wipolex/en/text/129350 | Pakistan Copyright Ordinance 1962 ss.18, 20 (life+50; records 50 yrs) |
| https://auqaf.punjab.gov.pk/node/421 | Punjab Quran Board objectives (incl. electronic digital devices) |
| https://www.dawn.com/news/amp/1597848 | PTA notice to Google re "unauthentic version of the Holy Quran" app (Dec 2020) |
| https://gigazine.net/gsc_news/en/20260902-ankidroid-google-play-policy-violation/ | AnkiDroid donation-link enforcement (Sep 2026) |
| https://developers.cloudflare.com/r2/pricing/, https://developers.cloudflare.com/r2/buckets/public-buckets/ | R2 prices, free tier, zero egress; r2.dev non-production |
| https://supabase.com/pricing | Free tier limits, pause after 1 week, Pro $25 |
| https://firebase.google.com/pricing, https://firebase.google.com/docs/android/play-data-disclosure | Spark limits; Crashlytics collected data |
| https://sentry.io/pricing/ | Developer plan 1 user / 5k errors |
| https://raw.githubusercontent.com/ACRA/acra/master/LICENSE | ACRA Apache-2.0 |
| https://raw.githubusercontent.com/batoulapps/adhan-kotlin/master/LICENSE | Adhan MIT |
| https://quran.9dmuslim.com/privacy-policy.html | Reference app privacy policy names Firebase Analytics, third-party services |
| google-play-scraper 1.2.7 (search/app, country=pk) | Reference app stats (70.7M installs, 4.64★, IAP Rs 280–6,900); competitor ads/IAP flags; name-collision searches |
| Sibling reports 01, 03, 04, 05, 06, 07 (docs/research) | 16-line Taj edition; reference APK SDKs and ads contradiction; renderer and font decisions; PD translation dates; audio-source terms; manifest/pack design |
