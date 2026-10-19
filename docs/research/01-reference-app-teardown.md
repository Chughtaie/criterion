# 01 — Reference app teardown: "Al QURAN - القرآن الكريم" (`free.alquran.holyquran`)

Researcher key: `r01-reference-app` · Date: 2026-10-19 · Scope: the feature and UX inventory needed to copy the app's **behaviour** (not its proprietary assets), plus what its users love and hate.

Method: I fetched the Play listing (en-US, en-PK, ur-PK) with `google-play-scraper` 1.2.7 and with raw HTML. I also fetched the Data safety page, the developer and app websites, the privacy policy, the sibling iOS listing (iTunes Lookup API), and 7 Play and 7 website screenshots plus 10 iOS screenshots, all reviewed visually. I scraped **2,991 unique Play reviews** (2020-04 to 2026-10-18) and clustered them by regex. For the APK, I downloaded the Play-stamped XAPK 4.4.46 from APKPure and inspected **only** the manifest (permissions and components), the bundled-SDK list (`*.properties` files and dex package prefixes) and the names and sizes of resource and asset files. I opened none of its content files.

Tags: **[verified: url]** means I fetched or inspected it in this session. **[inferred]** means my reasoning from verified evidence. **[unverified]** means it comes from memory.

---

## 1. Executive summary

1. **What the app is:** It is published by **9D TECH** (Islamabad, Pakistan; Play dev name "9D Muslim Apps"). It has **50M+ installs**, **4.7★ from 1.14M ratings**, version **4.4.46** (updated 14 Oct 2026), category Books & Reference, first released May 2019, free with in-app purchases at **$0.99–$24.99 (Rs 280–6,900)** [verified: Play listing]. At its core it is a **page-image mushaf reader plus a surah-MP3 player plus prayer times**.
2. **The mushaf is a scanned 16-line Indo-Pak edition, not a 13-line one.** The developer's own site says "lined up in **16 lines** on each page" [verified: quran.9dmuslim.com], and the Play screenshot the owner referenced shows 16 ruled lines. Juz *n* starts at about page 18n−16 (Juz 1 = p.2, Juz 2 = p.21 … Juz 9 = p.147), so the edition is about 548 pages [inferred from iOS screenshot]. There is a second, optional **"Usmani" (Madani 15-line) script** ("Recommend for Saudi Arabia, Dubai, Oman"). Script page sets are **downloaded after install** (about 50 MB according to one review).
3. **The pages are images with no ayah mapping.** This one fact explains most UX gaps. There is no ayah tap or long-press, no ayah highlighting, no audio-to-page sync, bookmarks work per page only, and "zoom" means pinching the bitmap (which users dislike). Users also report "text errors", because a scan of one printed edition differs from editions they own.
4. **Audio is one MP3 per surah, not per ayah.** You stream it or download it surah by surah. It plays in the background through a foreground media service, with a mini player and a full player (sleep timer, speed, repeat, playlist, audio bookmark). There are 20+ reciters, and the catalogue and naming ("Hafs A'n Assem – Murattal", "Maher Al Meaqli", "Abdulrahman Aloosi") match **mp3quran.net** exactly [inferred, strong]. It has **no** start-from-ayah, **no** highlight, **no** Para/Juz audio, **no** ayah range or repeat, and **no** "download all".
5. **Indexes:** a **Juz index** (30 paras with Indo-Pak names, e.g. "Alif Lam Meem · Page #2", "Sayaqool · Page #21"), a **Surah index** with Play and Read buttons per row and a reciter bar on top, **Go to Page**, **Bookmarks** and **Resume**. There is **no Manzil, Ruku or Hizb index**: the Manzil and ruku "ع" marks exist only as print on the scanned page. That makes the owner's Manzil request a clear differentiator.
6. **The listing claims more than the app has.** The listing advertises translations, word-by-word, tafsir, Qibla and azkar. The Android build shows no translation, tafsir or qibla screens (155 layout names, none of them for these features). Reviews from Sep–Oct 2026 still ask for translation, and one asks for Qibla to be added. Features that **are** present: prayer times with adhan, Hijri calendar with adjustment, Ramadan Sehri/Iftar calendar, a tasbeeh counter, a home-screen widget, a vertical auto-scroll mode, themes, and a multi-language UI.
7. **The #1 pain is monetisation.** It shows app-open, interstitial and video ads that are often **inappropriate for a Quran app** (dating, betting, loan/riba, music, immodest content). Reciters are paywalled, with a **rewarded ad unlocking one reciter for 24 h**. There are 6 paywall activities, a 3-day trial, and recurring "paid but premium lost / ads came back" bugs.
   - In the unbiased newest-600 sample, **28 of 53 one- and two-star reviews (53%) cite ads**.
   - Across the corpus, the ads theme has **649 reviews and about 39k "helpful" votes**.
8. **Privacy and compliance contradiction.** The Data safety page says "**No data collected, no data shared**" and the listing shows **no "Contains ads" label**. Yet the APK bundles the **Google Mobile Ads Next-Gen SDK 1.5.0**, UMP 4.0.0, **AppsFlyer**, Firebase (Analytics, Crashlytics, Performance, Remote Config, Messaging, Realtime DB, Auth), the AD_ID and AdServices permissions, and Play Billing 9.1.0. The developer's own privacy policy names AdMob, Facebook and Firebase Analytics [verified: Data safety page, APK, privacy policy].
9. **What users love:** the bold, clear Indo-Pak script that "matches my printed Quran"; fast navigation to Para/Surah/page; famous qaris; one-tap "Resume / Continue reading"; offline reading; prayer reminders and post-prayer Ayatul Kursi nudges; night mode; the screen staying on while reading.
10. **Top unmet asks, ranked by helpful votes:**
    - ayah highlighting and follow-along (≈5.5k votes)
    - start audio at any ayah or seek (≈1.8k)
    - Urdu/English translation (many requests, most persistent)
    - bigger font and better zoom (≈8.9k for the zoom/font theme)
    - an orientation lock that actually works (≈5k)
    - multiple bookmarks, a separate khatm progress and "last read" that bookmarks don't overwrite (≈10k for the bookmark/resume theme)
    - download-all (≈2.3k)
    - speed control
    - a 15-line option for huffaz
    - prayer alarms that are reliable and **do not override silent mode**.
11. **Positioning for us:** build an ad-free, ungated, offline-first app with:
    - an **ayah-aware** 16-line Indo-Pak page renderer
    - **ayah-timed** audio (any ayah / range / Surah / **Para** / **Manzil**, with repeat and highlight)
    - real **Para + Surah + Manzil + Ruku + Page** indexes
    - Urdu and English translation.

    This beats the reference precisely where its 1–3★ reviews concentrate.

---

## 2. Detailed findings

### 2.1 Store listing facts

| Field | Value | Source |
|---|---|---|
| Title | "Al QURAN - القرآن الكريم" | [verified: play.google.com/store/apps/details?id=free.alquran.holyquran&hl=en] |
| Short description | "Complete Al Quran app for android, offline Surah reading & MP3 Quran listening" (ur-PK: "اینڈرائیڈ کے لیے مکمل القرآن ایپ…") | [verified: scraper en/ur] |
| Developer (Play name) | 9D Muslim Apps - Quran, Qibla Direction & Prayers (dev id 7556925168098184106) | [verified: Play] |
| Legal entity / address | **9D TECH**, Hall, Floor 2, Walayat Complex, near Bahria Town Phase VII, Islamabad, Pakistan; +92 304 0536094; 9dtech.inc@gmail.com; support WhatsApp +92 343 0766856 (from developer review replies) | [verified: Play "About the developer"; review replies] |
| Website / privacy | http://9dmuslim.com · https://quran.9dmuslim.com/privacy-policy.html (effective 2023-02-02) | [verified] |
| Category / rating | Books & Reference · "Everyone" (US) / "Rated for 3+" (PK) | [verified] |
| Installs | 50,000,000+ (scraper `realInstalls` ≈ 70.68M) | [verified: scraper] |
| Ratings | 4.70 (US view) / 4.64 (PK view); **1,135,804 ratings**; histogram 1★ 44,842 · 2★ 13,389 · 3★ 26,869 · 4★ 65,511 · 5★ 984,830 (US) | [verified: scraper] |
| Price / IAP | Free; IAP **$0.99–$24.99 per item** (US), **Rs 280–6,900** (PK) | [verified] |
| Ads label | **No "Contains ads" label shown** (scraper `containsAds: False`; label absent in the HTML) even though the app shows ads (see §2.3 and §2.8) | [verified: Play HTML + APK] |
| Version / dates | **4.4.46**, versionCode 134, updated **Oct 14 2026**, released **May 1 2019** | [verified: Play + XAPK manifest.json] |
| Min / target Android | APKMirror lists three 4.4.46 bundles: **Android 7.0+**, arm64 **Android 10+**, **Android 12L+**. The XAPK I inspected: minSdk 32, **targetSdk 36** | [verified: apkmirror 4.4.46 release page; XAPK manifest] |
| Download size | Base APK 26.5 MB + config splits (total ≈27.3 MB for armeabi-v7a/mdpi). Mushaf page sets are downloaded on first use; one user mentions "expecting 5 MB … then downloading resources of 50 MB" | [verified: XAPK; review 2022-02-26] |
| "What's new" (4.4.46) | "🚫 Reduced ads for peaceful, distraction-free Quran reading · 📖 Full Quran offline with all 30 Juz · 🎧 20+ reciters added · ⚡ Faster performance and optimized offline access" | [verified: Play HTML] |
| Promo video | YouTube `tn5nQ_uORXc` ("Al QURAN - القرأن الكريم", uploader "Noureen Javed") | [verified: YouTube oEmbed] |
| Other apps by dev | Qibla Compass (`qibla.compass.finddirection.hijricalendar`, 10M+, 4.32★); Prayer Time 360 (`prayers.times.qibladirection.salat.time`, 1M+, 4.69★); Dua and Azkar (`dua.azkar.arabic.hisnalmuslim.daily.supplications`, 100K+, 4.7★) | [verified: Play dev page + scraper] |
| iOS sibling | "Al Quran - القران الكريم" by **9D TECHNOLOGIES**, v1.3.1 (2026-08-27), 4.67★ / 4,562 ratings, 92.8 MB, iOS 13+. Claims a "16 line display", "Arabic Word Zoom", Hijri adjustment, adhan short/long, 18 UI languages, 20+ reciters | [verified: itunes.apple.com/lookup?id=1669712591] |

**Listing feature claims vs evidence** (owner's list compared with the Android build):

| Listing claim | Evidence it exists in Android 4.4.46 | Verdict |
|---|---|---|
| Complete Quran, page-turn "like real mushaf", night mode, font size | Reader layouts (`pageviewing_activity2`, `fragment_view_pager1`, `activity_bookmark_curl`), screenshots | **Present** (font size means bitmap zoom only) |
| Bookmarks (ayahs and surahs) | `fragment_book_mark`, screenshots show **page-level** bookmarks | Present, **page-level only** ("can only be used on a page not on a verse", 2025-03-27) |
| MP3 audio, offline download, background play, repeat | Player layouts, `NewMusicService` (FG mediaPlayback), Download button | **Present** (per surah) |
| Auto-scroll | `layout_auto_scroll_animation`, `autoscroll.json`; 2026-03-01 review praises the new "vertical automatic scrolling" | **Present** (new in 2026) |
| Translations (Urdu, English…), word-by-word, tafsir, Arabic-only/+translation toggle | **No translation/tafsir layouts**; reviews from 2026-09-26, 2026-10-08 and 2026-10-16 still ask for translation | **Not evidenced** (marketing copy) [inferred, High] |
| Prayer times (auto location) | `CleanPrayerTimesMain`, `fragment_calculationmethod`, `cities_data.csv` (802 KB), adhan audio files, exact-alarm permission | **Present** |
| Qibla compass | No qibla layout. Review 2026-09-05: "if Qibla direction is added, would be great". The dev has a separate Qibla app | **Not evidenced** [inferred, Medium-High] |
| Daily azkar and duas | No azkar layouts. The dev has a separate Dua & Azkar app | **Not evidenced** [inferred, Medium] |
| Digital tasbeeh | `CounterActivtiy` | **Present** |
| Hijri calendar and reminders | `dialogfragment_hijricalendar`, `dialog_hijri_adjustment`, FCM push | **Present** |
| Ramadan (Sehri/Iftar) | `fragment_ramadan_calender`, `fragment_ramadan_kareem`; iOS screenshot of a Sehr/Iftar table | **Present** |

### 2.2 Data safety vs reality

| Data safety page says | Reality (APK and policy) |
|---|---|
| "The developer says that this app doesn't collect or share any user data." "No data shared with third parties." "No data collected." "Data is encrypted in transit." [verified: play.google.com/store/apps/datasafety?id=free.alquran.holyquran&hl=en] | The privacy policy lists "Google Analytics for Firebase, Facebook, AdMob, Firebase Analytics" and says it collects "Log Data … IP address, device name, OS version…" [verified: quran.9dmuslim.com/privacy-policy]. The APK bundles the ads SDK, AppsFlyer attribution, Firebase Analytics, Crashlytics and Performance, and requests `AD_ID`, `ACCESS_ADSERVICES_AD_ID`, `ACCESS_ADSERVICES_ATTRIBUTION`, `ACCESS_FINE_LOCATION` [verified: APK manifest]. |

**For us:** this is a cautionary example. Our app should carry **no ad or attribution SDKs** and should be able to truthfully declare "no data collected". If we add crash reporting, we declare it.

### 2.3 APK teardown (manifest, SDK list, file structure only)

Artifact: APKPure XAPK of 4.4.46. sha256 `ebb28a7a964f147f94f955226c7c9ba5a10b1f7ba0525239cfe33e6ae8ef39c6`. The manifest carries a Play source stamp (`com.android.stamp.source = https://play.google.com/store`, `STAMP_TYPE_DISTRIBUTION_APK`). It is R8-obfuscated [verified: inspected locally].

**Permissions (29):**
- Location: `ACCESS_FINE/COARSE_LOCATION`
- Network: `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `CHANGE_NETWORK_STATE`
- Notifications and alarms: `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`, `WAKE_LOCK`, `VIBRATE`
- Foreground services: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `FOREGROUND_SERVICE_DATA_SYNC`
- Audio and DND: `MODIFY_AUDIO_SETTINGS`, **`ACCESS_NOTIFICATION_POLICY`** (DND/ringer control). This likely explains the "app switches my phone from silent to general mode" reviews (2025-03-05, +334 helpful votes) [inferred].
- Storage: `READ/WRITE_EXTERNAL_STORAGE` (legacy)
- Billing: `com.android.vending.BILLING`
- Advertising IDs: `com.google.android.gms.permission.AD_ID`, `ACCESS_ADSERVICES_AD_ID`, `ACCESS_ADSERVICES_ATTRIBUTION`
- Other: `READ_BASIC_PHONE_STATE`, the install-referrer bind permission, `c2dm RECEIVE`, `READ_GSERVICES`, a Samsung maps-agent permission, and 2 app-defined permissions.

**App components (first-party):**

| Component | Meaning |
|---|---|
| `architecture.presentation.main.BaseActivity` | Single-activity shell (Navigation fragments) |
| `view.DisclaimerActivityNew` (exported, launcher) | Splash/disclaimer, which shows a hadith before entering (a 2023 review complains about its Arabic translation; a 2022 review says the "splash screen (intro) is long") |
| `view.boarding.BoardingActivity` | Onboarding |
| `view.language.updated.LanguagesActivity` (+ `…Old`) | UI language picker (Android language count not verified; iOS lists 18 UI languages) |
| `…features.prayertimes.CleanPrayerTimesMain` | Prayer times |
| `…features.counter.CounterActivtiy` | Tasbeeh counter |
| `SubscriptionScreen`, `paywallScreens.MainNewPaywallActivity`, `FreeTrialWorkPremiumActivity`, `ProIntroActivity`, `NewProXActivity`, `premium.Subscription31PercentOffer` | **Six paywall/upsell screens** |
| `…cleanmusicplayer.framework.NewMusicService` (FGS mediaPlayback) | Background audio |
| `receivers.AlarmReceiverAlQuran`, `receivers.BootAndTimeChangeReceiver` (BOOT_COMPLETED, TIME_SET, TIMEZONE_CHANGED, DATE_CHANGED) | Adhan alarm scheduling and re-scheduling |
| `widget.MyWidgetProvider` | Home-screen widget |
| `com.notifications.firebase.services.MessagingService` | FCM push (daily reminders, campaigns) |
| `com.hm.admanagerx.adsanalysis.MainActivity` | Third-party/in-house **ad-manager wrapper** |

**Bundled SDKs** (versions from `*.properties` files):

| Area | Libraries |
|---|---|
| Ads | **Google Mobile Ads Next-Gen SDK (`ads-mobile-sdk`) 1.5.0**; User Messaging Platform 4.0.0; `play-services-ads-identifier` 18.3.0; `play-services-appset`; privacy-sandbox `ads-adservices`; AppsFlyer (≈400 dex type references); `com.hm.admanagerx` |
| Firebase | Analytics 24.0.0, Crashlytics, Performance, Remote Config + A/B testing, Messaging, **Realtime Database**, **Auth 25.0.0** (+ reCAPTCHA 18.6.1, Credentials / Google ID 1.1.0), Installations, Sessions |
| Play | Billing **9.1.0**; In-app Review 2.0.2; In-app Update 2.1.0; **Asset Delivery 2.3.0** (possibly used for the mushaf page packs) [inferred]; Integrity 1.3.0; Location 21.4.0; **Maps 20.0.0** (location picker); Cronet |
| Android | AndroidX (Fragments + **Navigation**, ViewPager/ViewPager2, RecyclerView, ConstraintLayout, Material), **Hilt/Dagger**, **Room**, **WorkManager**, DataStore, Paging, Preference, Media (compat), Window, Webkit, Browser; only `compose.runtime-annotation`, so **the UI is View-based, not Compose** |
| 3rd-party | OkHttp, Glide, Picasso, Dexter (permissions), skydoves **Balloon** (tooltips), Lottie (by its JSON assets), ThreeTenBP/desugared `java.time`, an Islamic calendar package (`com.islamic.calendar`) |

**Notable resource files (names and sizes only):**
- `res/raw`:
  - adhan audio: `azan_full.mp3` 948 KB, `fajar_azan.mp3` 905 KB, `fajar_azan_short.mp3` 1.3 MB, `azan_default.wav` 544 KB, `silent.wav`
  - `cities_data.csv` **802 KB**, an offline city DB for prayer times
  - `qari_list.json` 4.4 KB (reciter catalogue)
  - `juz_quarters.json` 2.2 KB
  - Lottie animations: `autoscroll`, `download_quran_dark/light`, `script_download_*`, `widget_anim*`, `ramadan*`, `premium_anim`, `onboarding_alquran`
- `res/font`: Roboto and SN Pro only, **so there is no Quran font and the mushaf is not font-rendered** [inferred, High]
- No page images in the APK, so pages are downloaded at runtime (`layout_downloading_quranset`, `layout_internet_quranset`, `script_download_*.json`).

**Layout names (155) that reveal screens:**
- Reader:
  - `pageviewing_activity2`, `fragment_quran`, `fragment_view_pager1`
  - `activity_bookmark_curl` (page-curl effect)
  - `layout_reading_options`, `layout_auto_scroll_animation`, `fragment_quran_setting`
  - `fragment_quran_version` and `layout_quran_versions` (script picker)
  - `activity_goto_page__alert`, `dialog_decide_para_page`
- Indexes: `fragment_juzz_index`, `row_item_juz`, `fragment_surah_index`, `search_view`
- Bookmarks: `fragment_book_mark`, `bookmark_layout`, `activity_bookmark_dialogue`
- Audio:
  - `fragment_audioplayer`, `vertical_player`, `controls`, `surah_playlist`, `dialog_pauseplay`
  - `audio_item_with_bookmark` / `audio_item_without_bookmark`
  - `dialog_selectqari`, `item_qari`, `layout_qari_premium`
- Prayer:
  - `activity_prayer_times_main_contrained`, `row_azan`
  - `fragment_calculationmethod`, `item_calc_mthd`
  - `dialog_choose_location`, `layout_snap_time_picker_dialog`
  - `bottomsheet_battery_optimization`, `bottomsheet_permissions`, `dialog_trouble_shoot`
- Calendar and Ramadan: `dialogfragment_hijricalendar`, `dialog_hijri_adjustment`, `calendar_cell`, `fragment_ramadan_calender`, `fragment_ramadan_kareem`
- Other: `activity_counter_activtiy`, `widget_layout`, `dialogfragment_widget`, `dialog_theme`, `activity_languages`
- Growth:
  - `dialogfragment_invitefriend`, `dialogfragment_howtoinvite` (referral)
  - `rate_us_dialogue`, `rating_dialog1`, `dialog_exit_rate_us`, `dlg_update_app`
- Ads:
  - `ad_native_banner_new`, `layout_large_native`, `native_media_home`
  - `banner_cross_promo`, `fragment_native_ad`, `item_ad_tracker`
  - `layout_premium_banner`, `dialog_remove_ads`
- Only one landscape layout (`layout-land`) exists.

### 2.4 Reconstructed screen map and navigation

```
Launcher → Splash/Disclaimer (hadith) ─(first run)→ Language picker → Onboarding (Lottie)
        → Quran Scripts picker [Indo-Pak | Usmani] → download page set (needs internet)
        → (paywall: Pro intro / free-trial / 31%-off variants) → HOME
HOME (single Activity + Navigation)
 ├─ Toolbar: "Al Quran - القرآن الكريم" · 👑 premium · ⋮ (settings, theme, language, remove ads, invite, rate, share)
 ├─ Card: Gregorian date + Hijri date ("Jumada II 9, 1443 AH") + Now/next prayer ("Dhuhr 12:14 PM") → PRAYER TIMES
 ├─ Card: "Read Holy Quran — Resume" (shows last surah / Juz) → READER @ last page
 ├─ Tiles: Juz Index · Surah Index (Play Audio) · Go To Page · Bookmarks · (Ramadan, Tasbeeh, Calendar, Scripts… [partly unverified])
 └─ Native ads / cross-promo banners
JUZ INDEX: 30 rows "1. Alif Lam Meem · Page # 2", "2. Sayaqool · Page # 21" … → READER @ page
SURAH INDEX: top bar [reciter avatar + name ›] → SELECT RECITER sheet; 🔍 filter;
   rows: "2. Al Baqara / سُورَةُ البَقَرَة" + download-state icon (none / downloading ⏸ / error ⟳ / downloaded ✓)
         [PLAY] → mini player (equalizer anim in row)   [READ] → READER @ surah start page
READER (immersive) ─ swipe RTL pages (page-curl option) │ vertical auto-scroll mode │ landscape
   overlay toolbar: ‹ back · title = entry point ("Juz Index") · subtitle "Page #2, Juz #1" · 🔖 page bookmark · 🌙 night · 🔒 orientation lock
   bottom: mini player (surah, reciter, ⏮ ⏯ ⏭ ☆ ✕), swipe up → FULL PLAYER
FULL PLAYER: Haram/Nabawi photo · surah calligraphy card + [Download] · reciter ▾ · ⏰ sleep · 1.0x/x2 speed · 🔖 · seek bar (mm:ss of whole surah) · 🔁 repeat · ⏮ ⏯ ⏭ · playlist
BOOKMARKS: rows = name (surah/para) · Page # · timestamp · Arabic opening words · ribbon icon; (+) FAB (iOS)
PRAYER TIMES: location (Plus-code/address) · ‹ Hijri+Gregorian date › · "Asr in 2hr 8min 10s" · rows Fajr/Sunrise/Dhuhr/Asr/Maghrib/Isha each with 🔔 mode + ⚙ (adhan sound, manual minutes) · footer "Based on: Karachi Univ. of Islamic Sciences · GMT+05:00 · CHANGE"
RAMADAN CALENDAR: day · date · Sehr · Iftar (location based)   TASBEEH COUNTER   HIJRI CALENDAR (+/- adjustment)   WIDGET
```
Sources: Play screenshots (`HD Quran`, `Font Freedom`, `Search Index`, `Listen Quran`, `MP3 Audio`, `Bookmarks`), the feature graphic (home), quran.9dmuslim.com gallery (prayer, surah index with mini player, reader, player) and iOS screenshots (Ramadan calendar, scripts, juz index, prayer times, bookmarks, word zoom) [verified: visual review]. Component and layout names from the APK [verified].

### 2.5 Reader UX details

| Aspect | Reference behaviour | Evidence |
|---|---|---|
| Page source | Bitmap scans of a printed **16-line Indo-Pak** mushaf: ornamental double-rule border, horizontal rules between lines, header with **Para name (top-right, e.g. "الٓمّٓ ۱")**, **Surah name (top-left, "البقرة ۲")** and the printed page number in the centre | Play screenshot 2 (zoomed), quran.9dmuslim.com |
| App vs printed page numbering | App says "Page #2, Juz #1" while the printed page shows "۵", so **app page numbers ≠ printed numbers** | Play screenshot 2 (crop) |
| Margin marks | Indo-Pak **ruku "ع" marginal marker with three numerals** (ruku-in-surah / ayah count / ruku-in-para), an inline "ع" over the ayah end, and **"منزل ۱"** at the page foot | Play screenshot 2 (crop) |
| Second script | "Usmani" (Madani 15-line style, blue ornamental border, "سورة البقرة / الجزء الأول" header), "Recommend for Saudi Arabia, Dubai, Oman". Each script is downloaded separately ("Downloaded" badge) | Play screenshot 3, iOS screenshot |
| Navigation | Horizontal **RTL swipe**, with a **page-curl** option (`activity_bookmark_curl`); since 2026 a **vertical continuous auto-scroll** mode with Lottie hint | layouts; review 2026-03-01 |
| Toolbar | Translucent dark overlay on top of the page: back, entry-point title, "Page #N, Juz #M", bookmark toggle, night-mode toggle, **lock = orientation lock**. Users say the overlay "blocks the view" | screenshot; reviews 2024-01-08, 2024-06-06, 2026-02-24 |
| Full-screen | Immersive by default. A former toggle to keep status/nav bars visible was removed (Urdu review 2026-02-24 asks to restore it) | review |
| Zoom | Pinch-zoom on the bitmap ("pages expand beyond your screen"); "single tap" toggles page size; iOS has "Arabic Word Zoom", a magnified-line loupe on long-press | reviews 2025-03-02, 2024-06-06; iOS screenshot |
| Landscape | Landscape fit-width with vertical scroll. The "enlarged" landscape mode was removed in 2025 and users complained | reviews 2024-06-06, 2025-08-07 |
| Night mode | Dark theme (inverted scan); "if the phone is in night mode the pages become not clear" | review 2022-04-04; `drawable-night` |
| Keep awake | "the screen light will not get dim or turn off" | quran.9dmuslim.com |
| Haptics | Vibration on every page turn, at first with no toggle (requested 2023) | review 2023-10-26 (+695) |
| Resume | One global "last read" auto-saved and shown as the Home "Resume" card. Reading a bookmarked surah **overwrites** it; users ask for a separate khatm/juz resume | reviews 2026-05-17, 2026-01-08 (+907) |
| Bookmarks | One tap on 🔖 toggles a **page** bookmark (a ribbon overlay appears on the page). Tapping again removes it **without confirmation**. Names get truncated | reviews 2023-03-24, 2023-05-23, 2023-11-19 |
| Ayah interaction | **None**: no ayah long-press menu, no highlight, no copy/share/translate (pages are images) | inferred from reviews asking for highlight / start-from-ayah |
| Text accuracy | Many "mistake on page X" reports (spelling conventions, missing ayah numbers, index naming like Mu'min vs Ghafir, blank pages after p.447 in 2023) | 84 reviews in the "text/page errors" theme |

### 2.6 Audio UX details

| Aspect | Reference behaviour | Evidence |
|---|---|---|
| Granularity | **Per-surah MP3** (the seek bar spans the whole surah, e.g. 12:05/42:50). Not per ayah. No timing data | player screenshot; reviews asking for start-from-ayah |
| Source | 20+ reciters whose names and labels match mp3quran.net (`Rewayat Hafs A'n Assem - Murattal`, "Maher Al Meaqli", "Abdulrahman Aloosi", "Abdulaziz Alasiri", "Khaled Al-Qahtani"…) | [verified: mp3quran.net/api/v3/reciters] + screenshots → [inferred, High] |
| Reciter list shown | Sudais, Abdul-rahman Aloosi, Ahmed al-Ajmy, Ali al-Huthaify, Khalid al-Qahtani, Mishary Alafasy, Saud Shuraym, Saad al-Ghamdi, Maher al-Muaiqly, Yasser al-Dosari, Abdulaziz al-Asiri. iOS adds Abdul Basit, Abdullah al-Juhany, Abdul Muhsin al-Qasim, Bandar Baleela, Khalid al-Muhanna, Minshawi, **Qari Syed Sadaqat Ali** (Pakistani), Salah al-Budair, Saleh bin Habdan, Noreen Siddiq | screenshots; iOS description |
| Selection | A reciter bar on top of the Surah index and in the player; each row shows avatar, country and mosque ("Saudi Arabia – Imam Masjid Al-Haram (Makkah)") | screenshot 5 |
| Streaming / offline | Streams by default. "Download" one surah at a time from the player or the row (users struggled to find it; dev replies "play any surah… you will see the download option"). **No "download all"** (2026-10-17 review). Row icons show download state | reviews, dev replies, screenshot 4 |
| Controls | Mini player (reader and index) plus full player: sleep timer, speed (1.0x/x2), audio bookmark, repeat, prev/next surah, playlist, favourite ☆. Notification controls through a FG service | screenshots; `NewMusicService` |
| Missing | Ayah highlight and auto page-follow; start at an ayah or page ("I could play only from the starting of surah"); ayah/range repeat; **Para (Juz) audio** ("it would have been even better if Para had been recited in audio"); 5/10-second skip | reviews 2022-09-04 (+4,399), 2024-02-21, 2023-04-17, 2024-08-08 |
| Bugs | Stops 5–6 s before the end of a surah; hangs mid-verse; Android Auto/car glitches; double-tap restarts the surah; resume position lost | reviews 2026-03-10, 2022-09-23 (+791), 2024-01-06 |
| Gating | A free reciter set plus premium reciters; **watch a rewarded ad to unlock a reciter for 24 h**; offline playback of already-downloaded audio sometimes demanded an ad; "app automatically changes selected Qari… then asks to watch ad" | reviews 2025-07-17 (+1,134), 2024-10-31 (+440), 2025-03-19 |

### 2.7 Indexing (South-Asian conventions)

| Index | Reference | Gap vs owner's must-have |
|---|---|---|
| Juz / Para | 30 rows with Indo-Pak para names (Alif Lam Meem, Sayaqool, Tilkar Rusul, Lan Tanaloo, Wal Mohsanat, La Yuhibbullah, Wa Iza Samiu, Wa Lau Annana, Qalal Malao…) and **start page** | Parity is easy. Add Arabic names, a progress indicator and Para audio |
| Surah | 114 rows, transliterated + Arabic, Play/Read, search filter | Add makki/madani, ayah count, ruku count, start page, Para |
| Manzil | **None** (marker printed on the page only) | **Must build** (7 manzils: 1 Fatiha, 2 Ma'idah, 3 Yunus, 4 Bani Isra'il, 5 Shu'ara, 6 As-Saffat, 7 Qaf [unverified, standard]) |
| Ruku | **None** (printed marginal "ع" only) | Should build (Indo-Pak ruku dataset needed) |
| Page / Go-to | "Go To Page" dialog plus a Para/Page picker | Parity |
| Quarters | `juz_quarters.json` exists (probably for the Usmani script) | Hizb/Rub' for the Madani script, Could |
| Search | Surah-name filter only; "No search facility" (2026-05-22) | Should: Arabic/translation full-text search |

### 2.8 Monetisation and growth mechanics (observed)

- **Ad formats:**
  - an app-open ad on launch ("goes to an ad first", "ad of 30–40 seconds before I can recite")
  - interstitials on surah open, back press, menu and page navigation
  - native ads on home and in lists
  - rewarded ads that unlock reciters
  - cross-promo banners for the developer's other apps.

  Users stress how unacceptable the **ad content** is: dating, betting/casino, loan/riba, music, immodest imagery, "AliExpress ladies garments" and a Bible ad. The context makes it worse: users are often reading **in the mosque** or after wudu.
- **Paywalls:** 6 paywall activities, a 3-day free trial, a "31% off" offer, a lifetime purchase, and a 👑 entry on Home and Bookmarks. Recurring complaints:
  - premium lost after an update, with restore not working
  - ads come back for Pro users
  - unexpected charges ("11.99 SAR deducted")
  - no support response.
- **Retention and growth:**
  - FCM pushes ("start your journey with Holy Quran", Ayatul Kursi after prayer, Shab-e-Barat notices; the last was criticised as unauthentic)
  - a widget
  - in-app review prompts plus an exit "rate us" dialog
  - a referral "invite friends" flow
  - AppsFlyer attribution
  - Remote Config / A/B testing.
- **Rating inflation signal [inferred]:** in the newest 600 reviews, 84% are 5★ with an average length of **25 characters**, consistent with prompted ratings. Content-rich feedback is concentrated in 1–4★.

### 2.9 User reviews: themes, counts, quotes

**Corpus:** 2,991 unique reviews (en-PK newest 600 and most-relevant 300; score-filtered newest/relevant pulls for 1–4★; ur-PK 235; hi-IN 100). Dates run from 2020-04-25 to 2026-10-18 [verified: google-play-scraper]. The corpus is **deliberately enriched for 1–3★**, so read counts as relative weights, not prevalence.

**Unbiased slice:** the newest 600 (2026-09-20 to 2026-10-18) contain 504 5★ (84%) and 53 1–2★, of which **28 (53%) mention ads**. "Helpful votes" means the sum of thumbs-up across matching reviews (a proxy for how many users agree).

| Theme (regex-clustered) | Reviews | of which 1–2★ | Helpful votes | Representative quote |
|---|---:|---:|---:|---|
| **Ads: too many / intrusive** | 649 | 446 | 39,292 | "ads when launching the app, ads when opening the sura, ads when opening the menu" (2026-03-05) |
| **Ads: inappropriate content** | 91 | 77 | 3,064 | "I was sitting in a mosque… on the first page it showed an advertisement of tango, maybe a dating app" (2025-10-14) |
| **Monetisation: paywall / subscription** | 114 | 81 | 5,419 | "even before opening a verse, it is asking for payments. 3 day free trial for Quran???" (2026-03-14) |
| Monetisation: paid but premium lost / ads back | 35 | 24 | 2,837 | "purchased the premium version to remove ads but the ads are back after a recent update and there is no option… to restore purchases" (2023-12-12) |
| Audio: reciter gated (ad or premium, 24 h unlock) | 30 | 12 | 4,421 | "the ad which gives you one day to listen to the reciter. after the one day expires, you'll have to watch the ad again" (2025-07-17) |
| Audio: playback stops / hangs | 20 | 8 | 7,721 | "Quran while playing it is stopping in between the verses and getting stuck" (2022-09-23, +791) |
| Audio: internet needed / offline confusion | 145 | 80 | 14,433 | "No one can have internet connection all the time. Every time I try to play downloaded audio it says to watch an ad" (2024-10-31, +440) |
| Audio: start from chosen ayah / seek / resume | 25 | 10 | 3,383 | "choose from which ayat to start listening in long surah's" (2023-12-15) |
| Audio: highlight ayah / follow page | 8 | 3 | 5,508 | "it would be good if the AYAT being recited is highlighted in colour" (2022-09-04, **+4,399**; dev: "We are working on such a feature") |
| Reader: translation / meaning / transliteration | 88 | 29 | 14,065 | "there is no translation… couldn't even try it out first without paying" (2025-05-28); "Ma sha Allaah translation into Urdu required" (2026-09-26) |
| Reader: zoom / font size / eyesight | 29 | 10 | 8,911 | "If you try zooming in, the pages expand beyond your screen (not exactly increasing the font size)" (2025-03-02, +400) |
| Reader: orientation lock / landscape | 15 | 8 | 5,062 | "despite enabling 'Lock Orientation', the orientation doesn't get locked" (2026-02-24) |
| Reader: text / page errors | 84 | 53 | 3,362 | "in surah Ali Imran verses 71 till 79 are missing" (2023-04-27); "page 421 should show sura ghafir not mu'min" (2020-05-18) |
| Reader: wants 15-line | 3 | 0 | 94 | "the Quran itself is of 16 lines, and since I am a Hafiz of the 15 line Quran, I can't read through this app" (2023-04-14) |
| Reader: page-turn vibration / overlays | 5 | 1 | 884 | "when I turn the pages then vibrates slightly… how can I switch off" (2023-10-26, +695) |
| Bookmark / resume problems and requests | 42 | 12 | 10,345 | "separate bookmark for juzz reading so that if you want to recite something else, the juzz doesn't get disturbed" (2026-01-08, +907) |
| Prayer: inaccurate times / no manual / DST | 12 | 7 | 5,035 | "Timing of prayers available are not correct locally… no option for setting" (2021-04-18, **+4,501**) |
| Prayer: adhan not firing / silent-mode override | 23 | 10 | 4,837 | "Whenever i open the application, my phone automatically switches from silent mode to general mode" (2025-03-05, +334) |
| Hijri date wrong / unauthentic pushes | 11 | 5 | 120 | "Islamic dates on this app are usually incorrect" (2026-08-11) |
| Perf: slow start / splash / loading | 80 | 47 | 11,713 | "stuck on the opening green screen with 'Bismillah'" (2025-09-26) |
| Crash / not working | 28 | 15 | 1,477 | — |
| Regression: "old version was better" | 37 | 22 | 2,469 | "I regret updating it" (2026-08-23) |
| **Praise: easy, simple, beautiful UI** | 164 | 35 | 29,132 | "high performance and low memory requirement. The User interface is friendly, smooth and fast" (2024-03-10, **+6,045**) |
| **Praise: clear script that matches the printed Quran** | 91 | 36 | 8,829 | "this one is by far the best app I've found [whose writing matches the Quran I read]" (2023-04-01); "it has both indo-pak font and the other one" (2025-03-21) |
| **Praise: reciters / audio** | 219 | 71 | 41,676 | "many different people you can choose from to listen" (2024-04-05) |
| Praise / request: prayer times and reminders | 72 | 22 | 11,558 | "notification for prayer when it reminds me to recite aytul kursi after prayer" (2025-03-16) |
| Praise: night mode | 11 | 3 | 11,050 | "audio recitation and night mode are my favourite" |
| Praise: offline reading | 44 | 21 | 3,565 | "We can read Al Quran any time, even when we don't have mobile data" (2023-08-19) |

**Specific feature asks (keyword counts across the corpus):**
- reciter 24 h / ad unlock: 15
- search: 20
- qibla: 12
- Urdu translation: 11
- English translation: 9
- highlight: 8
- tafsir: 7
- ayah-specific play: 6
- speed: 6
- download-all: 3 (but +2,348 helpful votes)
- vibration toggle: 3
- 15-line: 2
- Manzil: **0**. Users don't ask for it, but no competitor offers it, so it is a cheap differentiator for the South-Asian audience.

**Developer support pattern:** only 27 of 2,991 reviews got a reply, nearly all in 2020–2024. Replies are templated ("We are working on such a feature", "contact WhatsApp +923430766856", "Please rate 5 stars").

---

## 3. Recommendations

1. **Make the 16-line Indo-Pak page view our default reading mode, and correct the brief.** The reference's mushaf is **16-line** (≈548 pages, Juz 1 at p.2, Juz 2 at p.21), not 13-line. Plan an optional **15-line Madani/Uthmani** mode (requested by huffaz, and the reference's second script). Treat 13-line (common in India and South Africa) as a later add-on. Show the **printed page numbers** of the edition we use, not a private numbering, because users quote mushaf page numbers. Confidence: **High** (16-line verified on the developer site and the screenshot).
2. **Render pages so the app knows where each ayah is.** That means one of:
   - a font/vector-rendered page layout (per-page line → word → ayah data plus a Quran font), or
   - licensed high-resolution page images **plus** per-ayah bounding-box data.

   This alone unlocks everything the reference cannot do: ayah long-press (bookmark, copy, share, translate, play from here), ayah highlight synced to audio, crisp zoom and a real font size, and a night mode that is not an inverted scan. Confidence: **High** on the need. **Medium** on finding an openly licensed 16-line Indo-Pak data source; this is the key open question for the data/licensing researchers.
3. **Make it ad-free with no gated Quran content, and say so loudly.**
   - No ad SDKs, no attribution SDKs, and every reciter and translation free.
   - If the owner later wants revenue, use a voluntary "support / sadaqah jariyah" tip and nothing in the reading or listening path.
   - Declare Data safety honestly: we can truthfully claim "no data collected" if we avoid analytics. An optional privacy-respecting crash reporter is fine if declared.

   This is the single biggest differentiator: about 650 reviews and 39k helpful votes are about ads, and 53% of current 1–2★ reviews cite them. Confidence: **High**.
4. **Use ayah-level audio with timings, and offer the playback scopes the reference lacks.**
   - Scopes: **Surah, Para (Juz), Manzil, page, ayah range, single ayah**.
   - Controls: repeat count per ayah and per range (hifz), speed 0.5–2.0x, sleep timer, gapless playback, follow-along page turning and highlight, Android Auto/MediaSession support.
   - Bulk download (per reciter, per surah set, per Para, "download all") with a storage manager.
   - Media3 + MediaSessionService for background play.

   Per-surah MP3 catalogues (like the mp3quran.net one the reference appears to use) can be a fallback for reciters without per-ayah files. Confidence: **High** (top requests by helpful votes; owner must-have #2).
5. **Index by Para + Surah + Manzil + Ruku + Page from day one.**
   - Para rows: number, Indo-Pak name (Arabic + transliteration), start page, khatm progress.
   - Manzil rows: 7, with ranges.
   - Ruku: per surah, with the marginal ruku numbers.
   - Hizb/Rub' for the Madani mode.
   - Every index row gets **Read** and **Play** actions, as the reference's Surah rows do.

   Confidence: **High** (owner must-have #3; Manzil and Ruku are absent in the reference).
6. **Match the reference's beloved reader affordances, minus its bugs:**
   - instant open to the last page (no long splash or hadith interstitial)
   - immersive full-screen with tap-to-toggle chrome that **doesn't cover text**
   - a persisted in-app orientation lock that actually works
   - RTL swipe with an optional page-curl
   - vertical continuous auto-scroll with adjustable speed
   - pinch-zoom plus a "word/line zoom" loupe
   - a haptics toggle
   - keep-screen-on while reading
   - landscape fit-width.

   Confidence: **High**.
7. **Bookmarks and progress done right:**
   - a separate **"Last read"** pointer
   - multiple named bookmark collections at ayah and page level, with confirmation before removal
   - a **Khatm tracker** (one or more khatm plans, Para checklist, daily goal, streak)
   - all of it stored in Room, with export/backup.

   Confidence: **High** (bookmark/resume theme ≈10k helpful votes).
8. **Translation is the most frequent unmet need.** Ship **Urdu + English** translation as a side-by-side or bottom-sheet view per ayah from MVP or MVP+1, plus an Arabic-only/with-translation toggle. Pick openly licensed translations (licenses still to be verified by the data researcher). Word-by-word and tafsir come later. Confidence: **High** on demand. **Medium** on license availability.
9. **Prayer times are a "Should", but they are a daily-retention driver for this audience.**
   - Use an open-source calculation library (e.g. Adhan; the license is to be verified by another researcher).
   - Default to **Karachi / Hanafi** for Pakistan and allow per-prayer manual offsets.
   - Use exact alarms with a built-in troubleshooting / battery-optimisation guide.
   - Adhan through the notification channel: **never toggle DND or ringer mode** (avoid `ACCESS_NOTIFICATION_POLICY`).
   - Add a home-screen widget (next prayer plus Resume reading), a Hijri date with ±day adjustment, and a Ramadan Sehri/Iftar table.

   Confidence: **Medium** (valuable, but not the owner's top three).
10. **Content integrity process:**
    - one authoritative source per script, with the edition and approval info shown in "About this mushaf"
    - an in-app "Report a text issue" flow (page, line, screenshot)
    - versioned data packs
    - a checksum test that each page's ayah ranges cover all 6,236 ayahs exactly once.

    Many 1★ reviews are alleged-error reports; the 2023 "blank pages after 447" bug would have been caught by an integrity check. Confidence: **High**.
11. **Offline-first packaging.**
    - Bundle or auto-fetch one mushaf on first run with a visible size and progress bar (the reference surprised users with about 50 MB post-install). Consider Play Asset Delivery for page packs.
    - Translations ship as small packs.
    - Audio is on-demand with a clear storage UI.

    Confidence: **Medium-High**.
12. **Defer the extras the reference only claims:** Qibla, Duas/Azkar and Tasbeeh are Phase 2 "Coulds". The reference itself lacks Qibla and Azkar in-app and pushes users to its separate apps. Confidence: **Medium**.

---

## 4. Risks and open questions

- **Licensing of a 16-line Indo-Pak page source (biggest risk).** The reference's page scans come from an undisclosed printed edition. **We must not copy them.** Open question for the data and licensing researchers: is there an **openly licensed 16-line Indo-Pak** page-image set or layout dataset (with ayah coordinates)? If not, can we typeset 16-line pages from an open Indo-Pak font and text, and would the result be acceptable to users who want it to "match my printed Quran"?
- **Regulatory approval.** Pakistan may require approval or certification for publishing Quran text and recordings; a Punjab Quran printing and recording law is often cited [unverified]. Legal check needed before publishing.
- **Audio rights.** The reference likely streams mp3quran.net files. We need the terms of use for whichever CDN or catalogue we choose (mp3quran, EveryAyah, Quran.com/QDC). Reciter-level permissions may differ.
- **Analysis limits.**
  - Review clustering is keyword-based on mostly English text. The 335 reviews pulled with the ur-PK and hi-IN locales were included, but regexes mostly miss their Urdu and Hindi text.
  - The corpus is enriched for low ratings.
  - Counts are indicative only.
  - I did **not** install or run the app. Feature absence (translation, qibla, azkar, Para audio, Manzil index) is inferred from layout names, reviews and screenshots, so it is high but not absolute confidence. Hands-on verification on a device is recommended.
- **Variant drift.** Play serves different builds per device (7.0+ / 10+ / 12L+). The one I inspected is the 12L+ build (minSdk 32). Older-device builds may differ.
- **Owner preference check.** The owner's description said "~13 lines", but the referenced screenshot is 16-line. Confirm whether they want 16-line (Pakistan-standard), 13-line, or both.
- **Scope creep vs MVP.** The reference's breadth (prayer, Ramadan, widget, calendar, tasbeeh) is part of its daily-habit pull. Decide which "Should" items enter v1 versus v1.1.

---

## 5. Sources

| URL | What I verified there |
|---|---|
| https://play.google.com/store/apps/details?id=free.alquran.holyquran&hl=en (+ `&gl=us`, scraper en-US/en-PK/ur-PK) | Title, full description, What's new, installs, rating counts and histogram, IAP range, developer address and phone, no "Contains ads" label, other apps, 8 screenshots, video |
| https://play.google.com/store/apps/datasafety?id=free.alquran.holyquran&hl=en | "doesn't collect or share any user data", encrypted in transit |
| https://play.google.com/store/apps/dev?id=7556925168098184106 | Developer's 4 apps (package ids) |
| Play screenshots (play-lh.googleusercontent.com, 7 unique) + feature graphic | Reader toolbar and 16-line page, Quran Scripts picker (Indo-Pak/Usmani), Surah index with Play/Read and reciter bar, Select Reciter, full player (sleep, speed, bookmark, repeat, playlist, download), Bookmarks, Home layout |
| http://9dmuslim.com | Company "MuslimApps"/9D, app portfolio, "100M downloads / 50M MAU / 130 countries" (self-claimed), phone 03430766856 |
| https://quran.9dmuslim.com/ (+ /assets/img/1–7.jpg) | "**16 lines on each page**", "10+ reciters (downloadable)", keep-screen-on, gallery screenshots (home tiles, prayer screen with countdown, mini player, reader, player "Hafs A'n Assem - Murattal") |
| https://quran.9dmuslim.com/privacy-policy.html | Third parties: Google Analytics for Firebase, Facebook, AdMob, Firebase Analytics; Log Data; effective 2023-02-02 |
| https://itunes.apple.com/lookup?id=1669712591&country=us (+ 10 iOS screenshots) | iOS sibling by 9D TECHNOLOGIES: v1.3.1, 4.67★/4,562, "16 line display", "Arabic Word Zoom", Hijri adjust, adhan short/long, 18 UI languages, reciter list (incl. Qari Syed Sadaqat Ali); Juz index with page numbers; Ramadan calendar; prayer screen "Karachi Univ. of Islamic Sciences" |
| https://www.apkmirror.com/apk/9d-muslim-apps-quran-qibla-direction-prayers/…-4-4-46-release/ | 4.4.46 bundles: Android 7.0+, arm64 Android 10+, Android 12L+ |
| https://d.apkpure.net/b/XAPK/free.alquran.holyquran?version=latest | XAPK 4.4.46 (sha256 ebb28a7a…). Inspected **only** the manifest (29 permissions, components), SDK `.properties` versions, dex package prefixes, and res/asset file names and sizes |
| https://www.mp3quran.net/api/v3/reciters?language=eng and /api/v3/riwayat | 243 reciters; exact name and label matches with the reference ("Rewayat Hafs A'n Assem - Murattal", "Maher Al Meaqli", "Abdulrahman Aloosi", "Abdulaziz Alasiri"); per-surah CDN folders |
| https://www.youtube.com/oembed?url=…tn5nQ_uORXc | Promo video title and uploader |
| https://al-quran-free.en.uptodown.com/android | Checked and **rejected**: a different app (ISYSWAY `com.isysway.free.alquran`) |
| google-play-scraper 1.2.7 (PyPI) | 2,991 unique reviews, 2020-04-25 → 2026-10-18 |

---

## 6. Clone checklist

| # | Feature | Reference behaviour | Priority for us | Notes |
|---|---|---|---|---|
| 1 | Mushaf page reader (Indo-Pak) | Scanned **16-line** Indo-Pak pages (~548), header with Para/Surah names, ruku "ع" margins, "منزل" foot | **Must** | Ayah-aware rendering (Rec. 2); our own licensed/open source of pages; show printed page numbers |
| 2 | Second script (Madani 15-line / Uthmani) | "Usmani" script, separate download | **Should** | Huffaz ask for 15-line; data is easier to source |
| 3 | 13-line Indo-Pak | Not offered | Could | Popular in India and South Africa |
| 4 | RTL swipe paging + page-curl | Yes, curl effect | **Must** (swipe) / Should (curl) | Compose `HorizontalPager` with RTL; curl optional |
| 5 | Vertical continuous / auto-scroll | Added 2026, Lottie hint | **Should** | Adjustable speed; pause on touch |
| 6 | Reader toolbar | Overlay: back, "Page #N, Juz #M", bookmark, night, orientation lock | **Must** | Don't cover text; show Surah, Para, Manzil, page |
| 7 | Immersive full-screen + tap toggle | Yes; bars toggle removed (users want it back) | **Must** | Setting: keep system bars visible |
| 8 | Orientation lock | Lock icon; buggy | **Must** | Persisted, reliable |
| 9 | Landscape fit-width scroll | Existed, "enlarged" mode removed in 2025 | **Should** | — |
| 10 | Zoom / font size | Bitmap pinch; iOS word loupe | **Must** | Real font-size scaling if text-rendered; loupe on long-press |
| 11 | Night mode / themes | Inverted scan; theme dialog | **Must** | True dark, sepia, high-contrast; follow system |
| 12 | Keep screen on while reading | Yes | **Must** | Toggle |
| 13 | Page-turn haptic | Always on (complaint) | Could | Off by default, toggle |
| 14 | Resume / last read | One global pointer, Home "Resume" card | **Must** | Separate from bookmarks; per-khatm pointers |
| 15 | Bookmarks | Page-level toggle, list with page #, name, time; no confirm on delete | **Must** | Ayah + page, named collections, confirm/undo, backup |
| 16 | Ayah long-press menu | None | **Must** | Bookmark, play from here, copy, share, translation, tafsir |
| 17 | Juz/Para index | 30 rows: number, Indo-Pak name, start page | **Must** | Add Arabic name, Read/Play, progress |
| 18 | Surah index | Transliterated + Arabic, Play/Read, download state, search filter | **Must** | Add revelation type, ayahs, rukus, Para |
| 19 | Manzil index | None | **Must** | 7 manzils, Read/Play |
| 20 | Ruku index / markers | Printed only | **Should** | Indo-Pak ruku dataset |
| 21 | Go to page / Para-page dialog | Yes | **Must** | Plus go to Surah:Ayah |
| 22 | Search | Surah-name filter only | **Should** | Arabic (diacritic-insensitive) + translation search |
| 23 | Audio playback scope | Per surah only | **Must** | Surah, Para, Manzil, page, ayah range, single ayah |
| 24 | Reciter selection | 20+ (mp3quran-style), avatar, country, mosque; premium-gated | **Must** | All free; favourites; include South-Asian qaris (e.g. Sadaqat Ali) subject to rights |
| 25 | Ayah highlight + follow-along | None (top request) | **Must** | Needs per-ayah timing data |
| 26 | Start from any ayah / seek | Not possible | **Must** | — |
| 27 | Repeat (ayah / range / surah) | Repeat button (surah) | **Must** | Hifz mode: N repeats, gap between repeats |
| 28 | Speed control | 1.0x / x2 | **Should** | 0.5–2.0x |
| 29 | Sleep timer | Yes | **Should** | — |
| 30 | Background play + notification + lock-screen | Yes (FG mediaPlayback service) | **Must** | Media3 MediaSessionService; Android Auto bonus |
| 31 | Mini player + full player | Yes (persistent mini player in reader and index) | **Must** | — |
| 32 | Offline audio download | Per surah, hard to discover; no bulk | **Must** | Per surah / Para / all; storage manager; resumable (WorkManager) |
| 33 | Streaming | Default | **Must** | Cache while streaming |
| 34 | Audio bookmark / playlist | Yes | Could | — |
| 35 | Translation (Urdu, English) | **Claimed, not present** | **Should** (MVP+1) | Top unmet ask; check licenses |
| 36 | Word-by-word / tafsir | Claimed, not present | Could | Phase 2 |
| 37 | Prayer times + adhan | Auto-location, calc methods, per-prayer mode, manual offset, adhan sounds; bugs (silent override, stale times) | **Should** | Open library; Karachi/Hanafi default for PK; never change DND/ringer |
| 38 | Hijri date + adjustment | Yes | **Should** | ±2 days |
| 39 | Ramadan Sehri/Iftar table | Yes | Could | Seasonal |
| 40 | Home-screen widget | Yes (prayer/Quran) | **Should** | Next prayer + Resume reading |
| 41 | Daily reminders / push | FCM campaigns, post-prayer Ayatul Kursi | **Should** | Local notifications only (no FCM); user-configurable, authentic content |
| 42 | Tasbeeh counter | Yes | Could | Trivial |
| 43 | Qibla compass | Claimed, not present (separate app) | Could | Phase 2 |
| 44 | Duas / Azkar | Claimed, not present (separate app) | Could | Phase 2, licensing of content |
| 45 | Multi-language UI | Language picker on first run (Android count unverified); 18 UI languages on iOS | **Should** | English + Urdu first; RTL-correct UI |
| 46 | Onboarding: language, script, download | Splash hadith, language, onboarding, script download | **Must** (minimal) | No long splash; show download size; skip-able |
| 47 | Ads / paywalls / referral / rating nags | 6 paywalls, rewarded/interstitial/native/app-open ads, rating dialogs | **Won't** | Core differentiator: no ads, no gating; at most one gentle rating ask |
| 48 | Analytics / attribution SDKs | AppsFlyer, Firebase suite, Ads SDK | **Won't** | Honest Data safety ("no data collected") |
| 49 | Khatm tracker / reading goals | None (requested) | **Should** | Para checklist, streaks, separate from last-read |
| 50 | Report a text issue | None (many error reviews) | **Should** | Page/line + screenshot; versioned data packs |
