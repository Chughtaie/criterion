# 08 — Companion & Differentiating Features (Prayer, Qibla, Hijri, Azkar, Tasbeeh, Ramadan, Hifz, AI, Search, Khatam, Sync)

Researcher key: `r08-features-companion-differentiators` · Date: 2026-10-19 · Scope: Android (Kotlin + Compose, minSdk 24, targetSdk 36), Pakistani daily-use audience, open/openly-licensed building blocks preferred.

Cross-references: 01 (reference-app teardown), 02 (competitors), 03 (open-source codebases), 04 (mushaf rendering), 05 (text/metadata/translations), 06 (audio). Where those reports already settled a question (WBW licensing, Indo-Pak Para boundaries, audio sources, renderer), this report builds on them rather than repeating the research.

---

## 1. Executive summary

1. **Prayer times: use batoulapps Adhan (Kotlin Multiplatform "adhan2", MIT).** Coordinates are `com.batoulapps.adhan:adhan2:0.0.7` (published 2026-06-13). It ships Karachi 18°/18° (with a built-in +1 min Dhuhr adjustment), Hanafi Asr, high-latitude rules with `recommendedFor()`, Qibla, and Sunnah times.
   - **Build blocker:** 0.0.7 depends on `kotlin-stdlib 2.4.0`. Kotlin does not promise that a 2.2 compiler can read binaries two minor versions newer. Bump the project from Kotlin 2.2.10 to 2.4.x (latest 2.4.21), or pin adhan2 0.0.6 (stdlib 2.2.20).
   - Enable `coreLibraryDesugaring` for minSdk 24. [verified: Maven Central POM, repo, kotlinlang.org]
2. **Adhan reliability is the biggest engineering risk in this whole area, and it is the reference app's top prayer complaint** (missed adhan, silent mode overridden; 01). In Pakistan, Transsion (Infinix, Tecno, itel; 25.5%), Vivo, Xiaomi, Oppo and realme together hold about 62% of all mobile devices (Sep 2026). These are DontKillMyApp's worst-scored vendors.
   - Use `SCHEDULE_EXACT_ALARM`, not `USE_EXACT_ALARM`: Play restricts the latter to alarm-clock and calendar apps.
   - Schedule with `setAlarmClock()`, which "continue[s] to fire normally" in Doze.
   - Reschedule on every system event that can cancel alarms. Add a daily WorkManager safety net.
   - Add an OEM-specific onboarding using DontKillMyApp's CC-BY content, plus an in-app "Adhan health check".
   - Never touch DND or the ringer. [verified: Play policy, Android docs, StatCounter, DKMA]
3. **Android 17 "background audio hardening" changes how a full adhan must play.** Background playback "fail[s] silently" unless the app runs a foreground service. Apps targeting API 37 also need while-in-use (WIU) capability, "unless the app has the exact alarm permission and is interacting with `USAGE_ALARM` audio streams".
   - So play the full adhan from a `mediaPlayback` foreground service with `USAGE_ALARM`.
   - `USAGE_ALARM` ignores silent mode, so the app itself must check silent and DND state.
   - The short adhan or takbeer uses a notification-channel sound. [verified: d.android.com Android 17 pages]
4. **Location: request `ACCESS_COARSE_LOCATION` only.** A 3 km position error changes the Qibla bearing by less than 0.07° and prayer times by seconds. That is computed for PK cities.
   - Coarse-only also sidesteps Play's new rule (effective 2027-01-27) that one-time precise location must go through the system "location button".
   - Ship an offline city picker built from GeoNames `cities15000` (CC BY 4.0). It has 34,173 cities, 372 of them in PK, plus Urdu/Arabic-script alternate names. It builds to a 3.1 MB SQLite file (1.5 MB zipped). [verified]
5. **Qibla: Adhan's great-circle bearing + `TYPE_ROTATION_VECTOR` + `GeomagneticField` declination.**
   - Pakistan's declination is only +1.3° to +3.1° E. AOSP still ships WMM-2020, but extrapolated to today it is off by ≤0.14° in PK, which is negligible. [verified: AOSP source; computed with WMM2025]
   - Add a calibration and interference UX.
   - A cheap delighter: **sun-based Qibla** plus the two **Rasd al-Qibla** moments each year, when the sun is directly over the Kaaba. In 2027 these are 28 May 14:18 PKT and 16 Jul 14:27 PKT (computed).
6. **Hijri: `java.time.chrono.HijrahDate` (Umm al-Qura) is "Fully implemented" through core-library desugaring, so it works on minSdk 24.**
   - Pakistan follows the Central Ruet-e-Hilal Committee, and is **currently one day behind Umm al-Qura**: 1 Jumada I 1448 was 12 Oct 2026 in UQ and 13 Oct 2026 in Pakistan.
   - Ship a ±2-day user offset, a remotely published "PK month-start" override feed, and a Hijri date that rolls over at Maghrib. [verified]
7. **Azkar: no openly licensed Hisnul Muslim with Urdu exists.**
   - Usable now: Seen-Arabic Morning/Evening Adhkar DB (MIT; Arabic + English + transliteration + source; 34 items, but its English is taken from hisnmuslim.com/sunnah.com), `osamayy/azkar-db` (345 Arabic rows in 135 categories, with an explicit "no licence, do what you want" dedication), and fitrahive/dua-dhikr (MIT; en/id).
   - Urdu: HadeethEnc has a free API with Urdu, but no licence text, so ask permission. Otherwise commission a scholar-reviewed Urdu translation.
   - Do not bundle hisnmuslim.com (Arabic/English JSON API, no licence).
   - **"Manzil" dua** (the 33 protective verses) is a free win: it is pure Quran text and our existing audio engine. [verified]
8. **Hifz is the strongest differentiator for this audience, and it needs no AI.**
   - Demand: 15-line Hifz app 5M+ installs, Tarteel 10M+, Noorani Qaida apps 1M+.
   - Build the traditional **Sabaq / Sabqi / Manzil** tracker on the edition's page grid:
     - hide and reveal words, with first-letter hints
     - range repeat audio
     - self-recording playback
     - mistake tapping, which feeds a weak-spot heatmap
     - **mutashabihat alerts** (QUL: phrase-level similar-verse data, 5,277 entries)
     - **FSRS**-weighted Manzil prioritisation (FSRS-Kotlin, ~300 lines, MIT per README). Ship in v1.x. [verified]
9. **AI recitation checking is feasible on-device, but it belongs in v2 behind an evaluation spike.**
   - Prefer **CTC** acoustic models aligned against the expected text. Whisper fine-tunes tend to "auto-correct" mistakes.
   - Run them via **sherpa-onnx** (Apache-2.0, v1.13.8; supports Whisper, NeMo CTC/transducer, Zipformer, and Silero VAD on Android/Kotlin).
   - Candidates:
     - tarteel-ai/whisper-base/tiny-ar-quran (Apache-2.0; WER 5.75% / 7.05%; 290 / 151 MB fp32)
     - mohammed/fastconformer-quran-ar (CC-BY-4.0; 114.6M params; claims 0.14% WER on everyayah, which is likely optimistic)
     - **Muaalem**, a tajweed-level model: 2.4 GB, so server-only. Its GitHub licence (QML v1.0; repo HEAD 2026-10-19) **bans ads in the whole product** and adds Sharia-compliance and mandatory AI-notice conditions. [verified]
10. **Search.**
    - Arabic: diacritic-insensitive search via a normalized shadow column, built from Tanzil *simple-clean* text rather than Uthmani, plus FTS. The androidx bundled SQLite has FTS5; platform FTS4 is enough to start.
    - Urdu/English translation full-text search.
    - Voice search **by recitation** reuses the v2 ASR model.
    - "Semantic" search is **retrieval-only**: QUL's 2,512 topics plus optional on-device multilingual-e5-small (MIT, 118 MB int8). **Never generated tafsir or rulings.** Always cite sources. [verified]
11. **Backup and sync.**
    - **MVP:** Android Auto Backup (25 MB, E2E-encrypted on Android 9+) plus a JSON export/import file.
    - **v1.x:** optional **"Sign in with Quran.com"**. The QF User APIs allow a public mobile PKCE client with no secret and no own backend, and cover bookmarks, collections, notes, goals, streaks and reading sessions.
    - **Firebase (Spark: no pausing; 50k reads/day)** only for group features.
    - Avoid Supabase Free: it pauses after one week of inactivity. [verified]
12. **Family / group khatam (esal-e-sawab), Qaida and kids.**
    - **Group khatam:** ship the **WhatsApp-shareable khatam sheet** first (no backend), then real-time shared khatam on Firestore + FCM + App Links. Play has only tiny group-khatam apps (1,000+ installs), so the field is open.
    - **Noorani Qaida:** create our own content and audio (the market is 1M+ installs per app).
    - **Kids:** keep visuals age-neutral. Child-targeted imagery can pull the whole app into Play's Families policy, which Play states explicitly. [verified]

---

## 2. Detailed findings

### 2.1 Demand signals (Play Store search, country=PK, 2026-10-19)

Data from google-play-scraper search results [verified: Play search via google-play-scraper 1.2.7]:

| Query | Top results (installs) | Read-out |
|---|---|---|
| hifz quran | 15 Lines Hefz/Hafezi Quran (5M+), Tarteel (10M+), Memorize Quran (1M+), Easy Quran Hafiz (100k+), HifzPath (50k+, 4.8★) | Strong, under-served by Indo-Pak-first apps |
| noorani qaida | Qaida Noorania With Sound (1M+), Noorani Qaida Arabic Alphabets (1M+), Aasan Noorani Qaida (500k+) | Large, low-quality field |
| tasbeeh counter | Digital Tasbeeh Counter (10M+), Tasbeeh Counter (5M+), several 1M+ | Huge standalone demand; trivial to build |
| qibla compass | Several 10M+ (including the reference dev's own Qibla app) | Expected "all-in-one" feature |
| prayer times pakistan | Dawat-e-Islami Prayer Times (5M+, 4.79★), Athan (10M+), Muslim Pro (100M+) | Daily-retention driver |
| khatam quran group / tracker | Group Khatam Tracker (1k+), Khatam ul Quran (1k+), Khatam Quran: 30 Days (10k+) | **Open field**: the practice is common (WhatsApp para-distribution), but apps are weak |
| azkar urdu | Dua & Azkar (1M+), ME Azkar (10k+), Subah Sham ky Azkar Urdu (5k+) | Urdu azkar under-served |
| quran widget | Small apps only (≤100k) except Quranly (1M+) | Widgets are a cheap differentiator |

Pakistan device mix (StatCounter, Sep 2026) [verified: gs.statcounter.com CSV]:

- **Vendors:** Samsung 17.3%, **Infinix 15.1%**, Vivo 11.9%, Xiaomi 10.6%, Oppo 10.2%, Apple 10.1%, **Tecno 8.9%**, realme 4.0%, Google 1.7%, Huawei 1.7%, itel 1.5%.
- **Android versions:** 16 = 21.4%, 15 = 16.4%, 13 = 14.8%, 14 = 11.9%, 11 = 11.5%, 12 = 8.8%. So about 64% run Android 13 or later, where exact-alarm and notification-permission rules apply, and about 20% run Android 11–12.

### 2.2 Prayer times

#### 2.2.1 Library

| Item | Finding |
|---|---|
| Repo | `github.com/batoulapps/adhan-kotlin` (`adhan-java` now redirects to the same repo; head commit `e23d143`, 2026-08-20) [verified: git clone] |
| License | MIT, "Copyright (c) 2016 Batoul Apps" [verified: LICENSE] |
| Artifact | `com.batoulapps.adhan:adhan2:0.0.7` (latest; Maven metadata lastUpdated 2026-06-13). KMP; the Android consumer resolves `adhan2-jvm` [verified: repo1.maven.org] |
| Transitive deps (0.0.7) | `kotlinx-datetime-jvm 0.8.0`, `kotlin-stdlib 2.4.0` [verified: POM] |
| Transitive deps (0.0.6) | `kotlinx-datetime-jvm 0.7.1`, `kotlin-stdlib 2.2.20` [verified: POM] |
| Legacy pure-Java | `com.batoulapps.adhan:adhan:1.2.1` (last published 2021-03-06) [verified: Maven metadata]. Fallback only |
| API surface | `PrayerTimes(coordinates, DateComponents, CalculationParameters)` exposes `fajr, sunrise, dhuhr, asr, maghrib, isha` as `kotlin.time.Instant`. Also `currentPrayer()`, `nextPrayer()`, `timeForPrayer()`, `Qibla(coords).direction`, `SunnahTimes` (middle / last third of night) [verified: source] |
| Methods | MWL, Egyptian, **Karachi (18/18, `methodAdjustments = PrayerAdjustments(dhuhr = 1)`)**, Umm al-Qura (18.5 / 90 min; "add +30 min Isha in Ramadan"), Dubai, Qatar, Kuwait, MoonsightingCommittee (seasonal), Singapore, North America ("not recommended"), Turkey, Other [verified: README + `CalculationMethod.kt`] |
| Madhab | `SHAFI` (earlier Asr), `HANAFI` (later Asr) [verified] |
| High latitude | `MIDDLE_OF_THE_NIGHT`, `SEVENTH_OF_THE_NIGHT`, `TWILIGHT_ANGLE`. `HighLatitudeRule.recommendedFor(coords)` returns SEVENTH above 48° N, else MIDDLE [verified: source] |
| Extras | `Shafaq` (general / ahmer / abyad), `Rounding`, per-prayer `PrayerAdjustments` [verified: CHANGELOG 0.0.5] |

**Kotlin compatibility (load-bearing).** Kotlin's stated rule is: "the binary format is mostly forwards compatible with the next language release, but not later ones" [verified: kotlinlang.org/docs/kotlin-evolution-principles.html]. A Kotlin 2.2.10 compiler consuming `kotlin-stdlib 2.4.0` metadata is therefore unsupported. The latest Kotlin is 2.4.21 [verified: Maven Central]. AGP 9 "built-in Kotlin" takes the KGP version from the classpath. Bumping `kotlin` in `libs.versions.toml`, which drives the Compose compiler plugin, should upgrade it, but the build researcher must confirm this [unverified].

```toml
# gradle/libs.versions.toml (proposal)
[versions]
kotlin  = "2.4.21"      # needed by adhan2 0.0.7 (stdlib 2.4.0); else pin adhan2 = "0.0.6"
adhan2  = "0.0.7"
desugar = "2.1.5"       # latest com.android.tools:desugar_jdk_libs [verified: Google Maven]
[libraries]
adhan2           = { module = "com.batoulapps.adhan:adhan2", version.ref = "adhan2" }
desugar-jdk-libs = { module = "com.android.tools:desugar_jdk_libs", version.ref = "desugar" }
```

```kotlin
// app/build.gradle.kts
android { compileOptions { isCoreLibraryDesugaringEnabled = true } }
dependencies { coreLibraryDesugaring(libs.desugar.jdk.libs); implementation(libs.adhan2) }
```

The adhan2 README itself says kotlinx-datetime "uses `java.time`, which needs either a minimum api level of 26, or enabling of `coreLibraryDesugaring`" [verified].

#### 2.2.2 Defaults by region (auto-picked from the country of the coarse fix or city; always overridable)

| Region | Method | Asr | High-lat | Rationale / confidence |
|---|---|---|---|---|
| PK, IN, BD, AF | KARACHI | **HANAFI** | n/a | Hanafi majority. Expose an Ahl-e-Hadith / Shafi'i "earlier Asr" toggle in onboarding. High |
| GB, IE, EU diaspora | MOON_SIGHTING_COMMITTEE | HANAFI for South-Asian users | `recommendedFor` (SEVENTH > 48° N) | 18° Fajr/Isha does not occur in midsummer Manchester (Aladhan returns Fajr 02:34 / Isha 23:47 on 21 Jun 2027 with angle-based fallback) [verified: api.aladhan.com]. Medium; many UK users follow their mosque's timetable, so a v2 "mosque timetable import" is worth it |
| SA | UMM_AL_QURA (+30 min Isha in Ramadan) | SHAFI | n/a | Per Adhan README. High |
| AE / QA / KW / TR / SG | DUBAI / QATAR / KUWAIT / TURKEY / SINGAPORE | SHAFI | n/a | Library presets. High |
| US / CA | MOON_SIGHTING_COMMITTEE | user | recommended | README calls ISNA "not recommended". Medium |
| Else | MUSLIM_WORLD_LEAGUE | SHAFI | recommended | Medium |

Add per-prayer **ihtiyat** offsets (±minutes). Pakistani timetables commonly add precaution minutes [unverified convention]. Also offer a "Hanafi Isha" option that keeps 18°.

#### 2.2.3 Golden test vectors (cross-check, Karachi method + Hanafi, 2026-10-19, Asia/Karachi)

These come from Aladhan (an independent implementation). Expect ±1–2 min against Adhan because of Adhan's +1 min Dhuhr adjustment and rounding [verified: api.aladhan.com]. Qibla values are great-circle bearings computed here; they match Aladhan to 0.1°.

| City (lat, lon) | Fajr | Sunrise | Dhuhr | Asr (Hanafi) | Maghrib | Isha | Qibla ° (true) | Declination ° (WMM2025) |
|---|---|---|---|---|---|---|---|---|
| Karachi (24.8607, 67.0011) | 05:16 | 06:32 | 12:17 | 16:25 | 18:02 | 19:18 | 267.7 | +1.33 E |
| Lahore (31.5497, 74.3436) | 04:47 | 06:08 | 11:48 | 15:48 | 17:26 | 18:48 | 260.3 | +2.20 E |
| Islamabad (33.6844, 73.0479) | 04:52 | 06:16 | 11:53 | 15:51 | 17:30 | 18:53 | 255.9 | +2.84 E |
| Peshawar (34.0151, 71.5249) | 04:59 | 06:22 | 11:59 | 15:56 | 17:35 | 18:59 | 254.0 | +3.08 E |
| Quetta (30.1798, 66.9750) | 05:17 | 06:37 | 12:17 | 16:19 | 17:57 | 19:17 | 256.7 | +2.47 E |
| Manchester (53.4808, −2.2426), Europe/London | 05:47 | 07:43 | 12:54 | 16:11 | 18:04 | 20:00 | 118.5 | +0.32 E |

```kotlin
val coords = Coordinates(31.5497, 74.3436)
val params = CalculationMethod.KARACHI.parameters.copy(
    madhab = Madhab.HANAFI,
    highLatitudeRule = HighLatitudeRule.recommendedFor(coords),
    prayerAdjustments = PrayerAdjustments(fajr = s.fajrOff, dhuhr = s.dhuhrOff, asr = s.asrOff,
                                          maghrib = s.maghribOff, isha = s.ishaOff))
val d = LocalDate.now(cityZone)                                   // date in the *city's* zone
val pt = PrayerTimes(coords, DateComponents(d.year, d.monthValue, d.dayOfMonth), params)
val fajrLocal = java.time.Instant.ofEpochMilli(pt.fajr.toEpochMilliseconds()).atZone(cityZone)
```

### 2.3 Adhan notifications done right on Android 13–17

#### 2.3.1 Permission and policy decisions

| Permission / API | Decision | Evidence |
|---|---|---|
| `SCHEDULE_EXACT_ALARM` | **Use.** Request it in onboarding with an explanation and `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`. Check `canScheduleExactAlarms()` before every schedule | "not pre-granted to fresh installs of apps targeting Android 13 … and higher"; when revoked "your app stops, and all future exact alarms are canceled" [verified: d.android.com alarms] |
| `USE_EXACT_ALARM` | **Do not declare.** | Play: "must only declare this permission if their core functionality supports the need for an exact alarm… The app is an alarm or timer app. The app is a calendar app that shows event notifications… Apps that request this restricted permission are subject to review" [verified: Play policy 9888170] |
| `POST_NOTIFICATIONS` | Use (Android 13+ runtime) | [unverified: standard platform behaviour] |
| `RECEIVE_BOOT_COMPLETED` | Use. Enable the receiver only while alarms are set | "all alarms are canceled when a device shuts down"; receiver "only works if the app has already been launched by the user at least once" [verified] |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Use, for full adhan playback | mediaPlayback: manifest permission `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, runtime prerequisites "None" [verified: FGS types page] |
| FGS start from background | Allowed when "Your app invokes an exact alarm to complete an action that the user requests" | [verified: FGS restrictions page] |
| `FOREGROUND_SERVICE_SYSTEM_EXEMPTED` | Not needed. It is available to "Apps holding `SCHEDULE_EXACT_ALARM`", but mediaPlayback is the honest type | [verified] |
| `USE_FULL_SCREEN_INTENT` | **Only for the Sehri wake-up alarm, with explicit user consent.** Not for adhan | Android 14+: auto-granted only for "setting an alarm" or "receiving phone or video calls"; others "must request permission from the user" [verified: Play policy] |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | **Do not declare.** Deep-link to `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` instead | "Google Play policies prohibit apps from requesting direct exemption … unless the core function of the app is adversely affected"; the acceptable-use table lists chat/VoIP, safety, task automation and device companion apps [verified: Doze page]. A Quran app's core is not adhan |
| `ACCESS_NOTIFICATION_POLICY` (DND / ringer) | **Never.** | The reference app holds it, and reviews say it flips phones from silent to general (01, +334 helpful votes) |
| `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION` | **Never.** | §2.5 |

#### 2.3.2 Which AlarmManager call

| API | Doze behaviour | Permission | Use for |
|---|---|---|---|
| `setAlarmClock()` | "continue to fire normally. The system exits Doze shortly before those alarms fire." [verified] | Exact-alarm permission | **The 5 audible adhans** and the Sehri alarm (default). Shows an alarm icon / "next alarm", so offer a toggle |
| `setExactAndAllowWhileIdle()` | Fires in Doze; "cannot fire alarms more than once per nine minutes, per app" [verified] | Exact-alarm permission | Silent prayer notifications and pre-prayer reminders (≥9 min apart) |
| `setAndAllowWhileIdle()` / `setWindow()` | Inexact; "within one hour" on Android 12+ [verified] | none | Degraded fallback when exact alarms are denied (show a red banner); Quran reminders (Kahf, Mulk) |
| WorkManager (2.12.0 [verified]) | Deferrable | none | Daily 00:30 safety-net re-scheduler; widget refresh |

#### 2.3.3 Scheduling architecture

```text
PrayerScheduler.rescheduleAll(reason)            // idempotent, ~5 ms
  ├─ compute today+tomorrow times (Adhan) for active city
  ├─ for each enabled event (Fajr..Isha, pre-reminders, Sehri, post-prayer Ayatul Kursi nudge):
  │     PendingIntent(requestCode = stable hash(eventType, epochDay)) → AdhanReceiver
  │     setAlarmClock / setExactAndAllowWhileIdle / inexact per §2.3.2
  └─ persist "scheduledAt" for lateness diagnostics
Triggers: app start · settings change · city change · BOOT_COMPLETED · LOCKED_BOOT_COMPLETED (directBootAware receiver)
          · MY_PACKAGE_REPLACED · TIME_SET · TIMEZONE_CHANGED · DATE_CHANGED
          · ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED (re-check canScheduleExactAlarms()) [verified: docs]
          · each AdhanReceiver fire (chains the next one) · daily WorkManager job (safety net)
AdhanReceiver.onReceive → log lateness (now − scheduled) → mode:
   SILENT → notify(channel=prayer_silent)
   SHORT  → notify(channel=adhan_short_<soundId>)            // system plays the channel sound; respects DND/silent
   FULL   → if (ringerMode==NORMAL && interruptionFilter==ALL) || user.playInSilent
               startForegroundService(AdhanPlaybackService)  // mediaPlayback FGS, USAGE_ALARM
            else notify(channel=prayer_silent) + vibrate pattern
```

```kotlin
fun scheduleAdhan(ctx: Context, at: Long, event: PrayerEvent) {
    val am = ctx.getSystemService(AlarmManager::class.java)
    val fire = PendingIntent.getBroadcast(ctx, event.requestCode,
        Intent(ctx, AdhanReceiver::class.java).putExtra("event", event.id),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) {
        val show = PendingIntent.getActivity(ctx, 0, prayerScreenIntent(ctx), PendingIntent.FLAG_IMMUTABLE)
        if (event.audible && settings.useAlarmClock) am.setAlarmClock(AlarmManager.AlarmClockInfo(at, show), fire)
        else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, fire)
    } else {
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, fire)   // degraded; UI warns
    }
}
```

#### 2.3.4 Audio path and Android 17 hardening

- Android 17, all apps: "must have a visible activity or must be running a foreground service that is not of type `SHORT_SERVICE`". Otherwise playback "fail[s] silently" [verified: d.android.com/about/versions/17/changes/bg-audio].
- Targeting API 37: "the app must be running a foreground service that has … (WIU) capabilities … However, the requirement for WIU capabilities is waived if the app has been granted the exact alarm permission, and it is making changes to audio streams that have the `USAGE_ALARM` attribute" [verified]. An exact-alarm-started FGS is **not** in the WIU list. So a full adhan played with `USAGE_MEDIA` would be suppressed after we target 37.
- **Decision:** play the full adhan in `AdhanPlaybackService` (`mediaPlayback` FGS, Media3 or `MediaPlayer`) with `AudioAttributes.USAGE_ALARM`, `CONTENT_TYPE_SPEECH` or `MUSIC`.
  - Request transient audio focus. Show an ongoing notification with **Stop** and a volume hint ("uses alarm volume").
  - Stop on screen-on plus a volume-key press, and after the clip ends.
  - Because `USAGE_ALARM` bypasses silent mode [unverified: standard alarm-stream behaviour], **check `AudioManager.ringerMode` and `NotificationManager.currentInterruptionFilter` ourselves**. "Play adhan even when phone is silent" is opt-in and off by default.
  - Test with `adb shell cmd audio set-enable-hardening throw` and look for `AudioHardening` in logcat [verified: bg-audio page].
- **Notification channels.** Channel sound and importance cannot be changed after creation by the app [unverified: platform behaviour]. Create a channel per selected short sound (`adhan_short_<id>`), delete the old one on change, and keep the channel set small:

| Channel id | Importance | Sound | Purpose |
|---|---|---|---|
| `adhan_full` | HIGH | none (service plays) | Full adhan notification with Stop |
| `adhan_short_<soundId>` | HIGH | takbeer clip (≤30 s) | Short adhan that respects DND and silent automatically |
| `prayer_silent` | DEFAULT | none | Silent prayer-time notice |
| `pre_prayer` | DEFAULT | soft | "Asr in 15 min" |
| `quran_reminders` | DEFAULT | soft | Kahf, Mulk, daily ayah, khatam goal |
| `sehri_alarm` | HIGH (+ full-screen with consent) | alarm | Sehri wake-up (true alarm semantics) |

#### 2.3.5 OEM battery killers (Pakistan mix)

DontKillMyApp scores: Xiaomi 5, Samsung 5, OnePlus 5, Huawei 5, Oppo 4, Vivo 3, Tecno 3, realme 3 (higher is worse). There is no Infinix page; Tecno's HiOS guidance is the closest proxy for Transsion XOS [verified: dontkillmyapp.com; vendor list in repo]. The site content "is licenced under CC-BY … free to be shared, adapted, even commercially" with attribution [verified: dont-kill-my-app LICENCE/README]. A JSON API exists, e.g. `https://dontkillmyapp.com/api/v2/xiaomi.json` [verified].

| Vendor (PK share) | Typical blocker | Our in-app guidance (deep link where possible) |
|---|---|---|
| Infinix / Tecno / itel (25.5%) | Power Marathon / auto-start manager | Settings → Battery → app → "Unrestricted"; Phone Master → Auto-start ON; lock app in Recents |
| Vivo (11.9%) | Background power consumption, auto-start | "Allow high background power consumption"; i Manager → Auto-start |
| Xiaomi (10.6%) | MIUI/HyperOS autostart, battery saver | Security → Autostart ON; Battery saver → "No restrictions"; lock in Recents [verified: DKMA Xiaomi] |
| Oppo / realme (14.1%) | Auto-launch, "Sleep standby optimization" | Allow auto-launch; battery → allow background activity |
| Samsung (17.3%) | "Sleeping / deep sleeping apps" | Battery → Background usage limits → remove from sleeping lists; "Never sleeping apps" |

Implementation:

- Maintain our own vendor-intent table, seeded from `judemanutd/AutoStarter`. It is MIT but its last commit was 2021-07-11, so it is stale: vendor it, don't depend on it [verified: git clone].
- Use `PowerManager.isIgnoringBatteryOptimizations()` and `ActivityManager.isBackgroundRestricted()` for diagnostics [unverified API detail].
- Add an **"Adhan health check"** screen showing:
  - each permission's state
  - battery state
  - the last 10 alarms with their lateness
  - **"Test adhan in 1 minute"**
  - vendor-specific steps
  - a "Report problem" export (log as text)

**QA matrix (must pass before release):**

- Devices: Infinix Hot/Note, Tecno Spark, Vivo Y-series, Redmi Note, Oppo A, Samsung A-series, Pixel. Android 11 / 13 / 14 / 15 / 16, plus the 17 beta.
- Scenarios:
  - Doze (`adb shell dumpsys deviceidle force-idle`)
  - `appops` deny SCHEDULE_EXACT_ALARM
  - standby bucket `rare`
  - reboot with no unlock (direct boot)
  - timezone change
  - app update
  - silent, vibrate and DND states
  - Bluetooth headset connected

### 2.4 Adhan audio: sources and licences

| Source | Licence | Duration | Verdict |
|---|---|---|---|
| Commons `File:Adhan.ogg` (Aishatu98) | CC0 | 42 s | Usable fallback for the short sound; provenance (who recited) unknown [verified: Commons API] |
| Commons `File:Beautiful adhan.ogg` (Adam-synagda) | CC0 | 154 s | Usable fallback full adhan; quality not yet audited [verified] |
| Commons `File:Azan.ogg` (Andrewler) | CC BY-SA 4.0 | 183 s | OK with attribution. ShareAlike binds the audio file only [interpretation] |
| Commons `File:Islamic call to worship.oga` | CC BY-SA 4.0 | 210 s | As above [verified] |
| Commons `File:Adhan wiki.oga` | CC BY-SA 3.0 | 27 s | Short sample [verified] |
| Makkah / Madinah / Alafasy adhans seen in many apps | No licence found; broadcast or commercial recordings | — | **Avoid** [unverified] |
| **Commissioned Pakistani muezzin** (work-for-hire, written assignment) | Ours | — | **Recommended primary:** a standard adhan, a Fajr adhan (with "as-salatu khayrun minan-nawm"), a short takbeer clip (≤30 s, for channel sound), and the post-adhan dua. Plus 1–2 CC0 alternatives |

The reference app ships `azan_full.mp3` (948 KB), `fajar_azan.mp3` and `fajar_azan_short.mp3` (01). We target ≤1 MB per clip as Opus/AAC, bundled (not downloaded), so that adhan works offline from first launch.

### 2.5 Location and offline city database

- **Coarse only.** Play announced (2026-04-15, effective 2027-01-27) "the location button as the recommended minimum scope for precise location". It will "require apps to use this button for one-time precise location access unless they require persistent, always-on location". Apps targeting Android 17 use `onlyForLocationButton` [verified: Play policy deadlines page; android-developers blog 2026-04].
  - Prayer times and Qibla need only km accuracy. A 3 km error shifts the Qibla bearing by ≤0.061° (Karachi; computed) and prayer times by under ~10 s.
- **Provider.** Use the platform `LocationManager` through `LocationManagerCompat.getCurrentLocation()` with the network/fused provider: one-shot, foreground only.
  - This avoids a Play-services dependency, so it works on Huawei (1.7% PK) and stays F-Droid-friendly.
  - `play-services-location` 21.4.0 is an optional flavour [verified: Google Maven].
  - Re-fix on app open when the last fix is older than 24 h. Prompt to switch city if the user moved more than 25 km.
- **Manual city is a first-class path**, not a fallback. Many users deny location, and travellers want to pick a city.
  - GeoNames dump "is licensed under a Creative Commons Attribution 4.0 License" [verified: download.geonames.org/export/dump/readme.txt].
  - `cities15000.zip` is 3.36 MB and holds 34,173 rows: PK 372, IN 3,783, GB 866, SA 98, AE 63. `cities5000` has 69,848 rows, PK 493 [verified: downloaded and counted 2026-10-19].
  - `alternatenames` contains Urdu forms such as لاہور, کراچی, گوجرانوالہ, so **Urdu city search works offline**.
  - Columns kept: id, name, ascii, Arabic-script alternate names, lat, lon, cc, admin1, tz, population. Result: a **3.14 MB SQLite file, 1.49 MB zipped** (built and measured).
  - Use the city's IANA `tz`. All PK rows are `Asia/Karachi`.
  - Attribution "GeoNames, CC BY 4.0" goes on the About screen.

### 2.6 Qibla compass

- **Bearing.** Use `Qibla(Coordinates).direction` from Adhan (great circle to Kaaba 21.4225241, 39.8261818) [verified: source]. Cross-checked with independent great-circle maths and Aladhan (§2.2.3).
- **Sensors.**
  - Prefer `TYPE_ROTATION_VECTOR` (gyro + accel + mag fusion), then `getRotationMatrixFromVector` → `remapCoordinateSystem` (by display rotation and flat/upright pose) → `getOrientation`.
  - Fall back to `TYPE_GEOMAGNETIC_ROTATION_VECTOR`, then to accelerometer + magnetometer with a circular low-pass filter (average sin/cos, not angles).
  - Feature-detect `PackageManager.FEATURE_SENSOR_COMPASS`. Some budget phones lack a magnetometer; for those, use Sun mode. [unverified: API details from platform knowledge]
- **True north.** True azimuth = magnetic azimuth + `GeomagneticField(lat, lon, alt, now).declination`.
  - AOSP `main` still says "This class currently uses WMM-2020 which is valid until 2025" [verified: AOSP GeomagneticField.java].
  - Extrapolated WMM-2020 differs from WMM2025 by only 0.06–0.14° across PK cities (computed with pygeomag). That is irrelevant next to phone-compass error. **Do not bundle WMM2025.**
- **Calibration UX.**
  - Read `onAccuracyChanged` (UNRELIABLE / LOW, then show a figure-8 animation).
  - Show an interference warning when the field magnitude is outside about 25–65 µT [unverified: physics range].
  - Show "remove magnetic case" tips and a stable/unstable indicator.
  - Haptic tick when within ±3° of Qibla. Keep screen-on while the screen is open.
- **Sun mode (no magnetometer, or interference).** Compute the solar azimuth (NOAA algorithm) and show "Qibla is X° to the right of the Sun". This uses the same maths as prayer times, so it is S effort.
- **Rasd al-Qibla.** The sun reaches zenith over the Kaaba on **2027-05-28 09:18 UTC (14:18 PKT)** and **2027-07-16 09:27 UTC (14:27 PKT)**. At those moments every shadow worldwide points away from the Qibla. Computed with a NOAA-approximation solar model (±1–2 min). Push a once-a-year "verify your Qibla" notification.
- **Effort:** M (sensor plumbing, remapping, calibration, testing on cheap phones).

```kotlin
override fun onSensorChanged(e: SensorEvent) {
    SensorManager.getRotationMatrixFromVector(r, e.values)
    SensorManager.remapCoordinateSystem(r, axisFor(display.rotation).x, axisFor(display.rotation).y, out)
    SensorManager.getOrientation(out, o)
    val trueAz = (Math.toDegrees(o[0].toDouble()) + geo.declination + 360) % 360
    needle = smoothCircular((qiblaBearing - trueAz + 360) % 360)    // rotate Kaaba marker by this
}
```

### 2.7 Hijri calendar

- **API.** The desugaring table lists `java.time.chrono.HijrahChronology` and `HijrahDate` as "Fully implemented class" [verified: developer.android.com/studio/write/java8-support-table], so `HijrahDate.from(LocalDate)` works on minSdk 24 with `desugar_jdk_libs`. The default variant is Umm al-Qura, valid roughly 1300–1600 AH [unverified].
- **Pakistan reality.** For 1 Jumada al-Ula 1448:
  - Umm al-Qura: **Mon 12 Oct 2026** [verified: Wikipedia Jumada I table, via search]
  - Central Ruet-e-Hilal Committee (Chairman Maulana Abdul Khabir Azad): **Tue 13 Oct 2026** [verified: thepakistanconnect.com]
  - So Pakistan is **UQ − 1 day** right now. The offset varies month to month.
- **Design.**
  1. `effectiveHijri = override ?: HijrahDate.from(civilDate + userOffset)`, with `userOffset ∈ {−2…+2}`.
  2. **The Islamic day starts at Maghrib.** After today's Maghrib, show the next Hijri date, with "(from Maghrib)" in the UI. This toggle is on by default for events like "tonight is Shab-e-…".
  3. **The remote override feed** is a tiny static JSON on GitHub Pages or Firebase Hosting, signed and fetched daily, with the last-known value cached. The maintainer updates it after each 29th-evening announcement, which is about 12 × 5 min of ops per year:

```json
{ "schema": 1, "region": "PK", "authority": "Central Ruet-e-Hilal Committee",
  "months": [ { "hijri": "1448-05", "start": "2026-10-13", "source": "MoRA notification", "announced": "2026-10-12T15:30:00Z" } ],
  "sig": "ed25519:…" }
```

- **Events table** (Hijri-keyed, region-aware, each one a toggle; neutral labels; no fadha'il claims without a graded source):

| Hijri | Event | Note |
|---|---|---|
| 1 Muharram | Islamic New Year | |
| 9–10 Muharram | Ashura (fast 9 + 10) | PK public holidays [unverified] |
| 12 Rabi al-Awwal | 12 Rabi-ul-Awwal / Eid Milad-un-Nabi | PK public holiday. Observance differs by maslak, so neutral naming and a toggle [verified: propakistani holiday notice via search] |
| 27 Rajab | Shab-e-Mi'raj (traditional) | Toggle |
| 15 Sha'ban | Shab-e-Barat | Toggle |
| 1 Ramadan; odd nights 21–29 | Ramadan; Laylat al-Qadr reminders | |
| 1 Shawwal | Eid al-Fitr (+ 6 Shawwal fasts) | |
| 1–10 Dhul Hijjah | First ten days; 9 = Arafah; 10 = Eid al-Adha; 11–13 Tashreeq | |
| 13–15 every month | Ayyam al-Bid fasting (optional) | |

### 2.8 Azkar and duas: datasets and licences

| Dataset | Content | Licence (as stated) | Use |
|---|---|---|---|
| **Seen-Arabic/Morning-And-Evening-Adhkar-DB** (last commit 2026-02-14) | 34 morning/evening items; ar.json + en.json with `content, translation, transliteration, count, count_description, fadl, source, hadith_text, audio` | MIT [verified: LICENSE]. **But** the README says the English and transliteration come "from Hisn Muslim (hisnmuslim.com) and Sunnah (sunnah.com)", so upstream rights are unclear [verified: README_EN] | Arabic + structure + `source` refs: yes. English: ask sunnah.com, or replace |
| **osamayy/azkar-db** (2024-09-19) | 345 Arabic rows in 135 categories (morning 31, evening 30, ruqyah 33, sleep 13, after-salah 8 …); columns `category, zekr, description, count, reference, search` | README: "هذه الملفات بدون رخصة استخدام، يحق لك فعل ما تريد بها مجانا وبدون أي مسائلة قانونية" ("no usage licence; you may do whatever you want with them, free and without legal liability") [verified] | **Primary Arabic corpus.** Treat as a public-domain dedication; still add attribution |
| **fitrahive/dua-dhikr** (2025-11-29) | morning, evening, after-salah, daily-dua, selected-dua; **en + id** only | MIT [verified] | English fallback |
| rn0x/hisn_almuslim_json (2022) | Full Hisn al-Muslim Arabic, 134 chapters | No licence file [verified] | Reference only |
| hisnmuslim.com API | `https://www.hisnmuslim.com/api/husn.json` lists **only Arabic + English**; per-chapter JSON with audio URLs | No licence or terms found [verified: fetched] | **Do not bundle**; reference only |
| **HadeethEnc.com API** | Hadith encyclopedia incl. **Urdu**: categories "صبح شام کے اذکار" (7), "نماز کے اذکار" (23), "ماثور دعائیں" (32) … | "providing them for free through all available means", no explicit licence text [verified: API + about page] | **Ask permission** to bundle Urdu (same org family as QuranEnc, whose terms allow republishing; see 05) |
| Urdu Hisn-ul-Muslim (Darussalam print, 267 duas) | Urdu | Copyrighted publisher edition [verified: listing via search] | No |
| **Commissioned Urdu translation** of about 150 core duas | Urdu (+ Roman Urdu) | Ours | **Recommended**, with a scholar review |

The Hisn al-Muslim *compilation* by Sa'id al-Qahtani is copyrighted. The underlying Qur'an and hadith Arabic is centuries old. Building our own curated list with per-item hadith references is the clean route.

**"Manzil" (protective verses).** Al-Fatiha; Al-Baqarah 1–5, 163, 255–257, 284–286; Al-Imran 18, 26–27; Al-A'raf 54–56; Al-Isra 110–111; Al-Mu'minun 115–118; As-Saffat 1–11; Ar-Rahman 33–40; Al-Hashr 21–24; Al-Jinn 1–4; Al-Kafirun; Al-Ikhlas; Al-Falaq; An-Nas. Compiled by Shaykh Zakariyya Kandhlawi [verified: al-fathulmubin / muftionline via search]. Report 02 notes that "Manzil" searches in PK mostly mean this dua. The index must disambiguate "7 Manzils" from "Manzil dua".

**Azkar UX:**

- Card-per-dhikr with a big tap counter (count → auto-advance).
- Arabic in Indo-Pak or Naskh font, Urdu, English and transliteration toggles.
- Reference chip (e.g. "Bukhari 6306").
- Optional audio. A gapped per-ayah reciter covers Quranic items; hadith items need our own recordings.
- Morning reminder after Fajr (+30 min) and evening reminder after Asr, both opt-in.
- Progress ring per session.

### 2.9 Tasbeeh and Ramadan

**Tasbeeh (S):**

- Full-screen tap area; `HapticFeedbackConstants` tick on each tap, stronger at 33/99/target.
- Volume-key counting while the screen is in the foreground; keep-screen-on; reset lock.
- Presets: Tasbih-e-Fatimah 33/33/34, Istighfar 100, Durood, Kalima, custom; with targets.
- Daily totals and private history.
- A Glance widget with a "+1" action is a cheap delighter. A Wear OS tile comes in v2.

**Ramadan (M, ship before Ramadan 1448 ≈ Feb 2027):**

- 30-day Sehri/Iftar timetable from prayer times. Sehri ends at Fajr with an optional −0…−10 min safety margin; Iftar is Maghrib with a +0…+3 min ihtiyat.
- **Share timetable as image** (WhatsApp-friendly).
- Countdown card.
- Sehri wake alarm (alarm semantics, `USAGE_ALARM`, full-screen intent with consent).
- Iftar dua with source.
- Taraweeh khatam plan (27-night preset; §2.13).
- Laylat al-Qadr odd-night reminders.
- Optional Fitrana amount via remote config (PK authorities announce it yearly) [unverified].
- Show only duas that have a stated source. The popular Sehri "niyyah" formula has no hadith basis [unverified; religious]. Get scholar sign-off before including it.

### 2.10 Differentiator evaluation (value for the PK daily user × feasibility)

Effort scale: S < 3 dev-days, M = 1–2 weeks, L = 3–6 weeks, XL > 6 weeks (one experienced Android dev).

| Feature | User value (PK) | Feasibility / open assets | Effort | Phase |
|---|---|---|---|---|
| Hifz mode (hide/reveal, repeat, Sabaq/Sabqi/Manzil, mutashabihat) | **Very high** (huffaz, madrasa students, parents) | Our renderer gives word boxes (04); gapped audio (06); QUL mutashabihat; FSRS-Kotlin | L | v1.x |
| AI recitation follow-along / checking | High, but Tarteel exists (paywalled, online) | sherpa-onnx + Apache/CC-BY models; accuracy unproven on Pakistani learners | XL | v2 (spike in v1.x) |
| Tajweed colour (Madani view) | Medium | cpfair data CC BY 4.0, Madani-only, unmaintained | M | v2 |
| Tajweed colour (Indo-Pak) | Medium–High | **No open data**; rule engine needed | XL | Won't (now) |
| WBW tap popup (Urdu/English + word audio) | High | Licence blocked for public release (05) | M | v1.x (after permission) |
| Root/morphology explorer | Low–Medium | QAC GPL "verbatim" (05) | M | v2 |
| Arabic diacritic-insensitive + translation search | High | Tanzil simple-clean (05), FTS | M | MVP / v1.0 |
| Voice search by recitation | Medium–High ("which surah is this?") | Same ASR as AI | M (after AI) | v2 |
| Semantic/topic search | Medium | QUL topics (2,512), e5-small MIT | S (topics) / L (embeddings) | v1.x / v2 |
| Khatam planner, streaks, stats | **High** (01: ≈10k votes on resume/khatm) | Pure app logic + Indo-Pak Para table (05) | M | v1.0 |
| Multiple bookmarks / folders / colours, notes, highlights | High | Pure app logic | M | MVP / v1.0 |
| Smart reminders (Kahf Friday, Mulk, Yaseen, daily ayah) | High (habit) | Alarm infra from §2.3 | S | v1.0 |
| Widgets (next prayer, continue reading, ayah of day) | High | Glance 1.2.0 [verified] | M | v1.0 |
| Share ayah as image/text | High (WhatsApp culture) | Compose offscreen render | S–M | v1.0 |
| Noorani Qaida | High (1M+ apps) | **Own content + recording** | L (content) | v1.x |
| Kids mode | Medium | Families-policy risk | M | Separate app (v2) |
| Urdu UI / RTL / per-app language | **Must** | Noto Nastaliq Urdu (OFL) [verified] | M | MVP |
| Roman Urdu UI | Medium | Our own strings (`b+ur+Latn`) [unverified qualifier] | S–M | v1.x |
| Accessibility (TalkBack, large text) | Must (elderly users) | Compose semantics | M | MVP baseline |
| Android Auto | Medium (car tilawat) | Media3 `MediaLibraryService` (06) | S | v1.x |
| Wear OS | Low–Medium | Tiles 1.6.2 [verified] | M | v2 |
| Tablet two-page spread | Medium | Renderer (04); Android 16 ignores orientation lock on ≥sw600dp (04) | M | v1.x |
| Backup / sync | High (phone changes are common) | Auto Backup; QF User APIs; Firebase | S → M | MVP → v1.x |
| Family / group khatam | **High cultural fit** (esal-e-sawab, Shabeena, WhatsApp para lists) | Text sheet (no backend) → Firestore | S → L | v1.0 → v1.x |

### 2.11 Hifz mode: the South-Asian system as a product

The traditional cycle [unverified: domain knowledge; validate with a local qari/ustad]:

- **Sabaq:** today's new lesson, a few lines up to a page. It is repeated many times and recited to the ustad.
- **Sabqi:** recent portion, roughly the last quarter juz to one juz, revised daily.
- **Manzil:** old portion, cycled 1–5 paras per day.

Huffaz memorise **by page of a specific edition**: the 15-line "Hafizi" is common, and 16-line and 13-line are also used (02/04). So hifz state must be keyed by `(mushafId, page)` and also stored as ayah ranges, so that switching edition can migrate it.

| Tool | Detail | Depends on |
|---|---|---|
| Plan builder | Edition, start point (or "already memorised" paras), sabaq size (lines / ½ page / page), sabqi window (pages), manzil quota (paras/day), rest day | Page↔ayah map (04/05) |
| Daily dashboard | Three cards: Sabaq (p. 212 lines 1–5), Sabqi (pp. 200–211), Manzil (Paras 3–4). Each has Read / Listen / Test | — |
| Hide / reveal | Modes: all hidden; first letter of each word; first word of each ayah; every other line. Tap a word to reveal (counts as a "peek") | Word boxes from renderer (04) |
| Listen & repeat | Ayah or range × N repeats, gap between repeats, speed 0.5–1.25×, "repeat each ayah N then whole range M" (classic hifz) | Gapped audio (06) |
| Record yourself | Local `MediaRecorder`; play back against the qari; no upload | — |
| Mistake marking | Tap the words you got wrong (self or ustad). Builds a **weak-spot heatmap** per page and ayah | Word ids |
| Grading → scheduling | Mistakes → FSRS rating (0 → Easy, 1–2 → Good, 3–5 → Hard, >5 → Again). FSRS (`stability`, `difficulty`, `due`) **orders Manzil pages within the traditional cycle**; it does not replace the cycle | FSRS-Kotlin (MIT per README; 299 + 38 lines; Rating Again/Hard/Good/Easy) [verified] |
| Mutashabihat alerts | When testing 2:112, flag "فَلَا خَوْفٌ عَلَيْهِمْ وَلَا … repeated 13× in 13 ayahs across 7 surahs" and offer a compare view | QUL Mutashabihat (5,277 phrases; JSON, login-gated; per-resource permission) [verified: qul.tarteel.ai/resources/mutashabihat/73] |
| Juz grid | 30 paras × pages; colour = strength (FSRS retrievability) | — |
| Ustad / parent report | Weekly summary as text or image for WhatsApp; optional PDF | Share infra |

```kotlin
@Entity(primaryKeys = ["mushafId", "page"])
data class HifzPage(val mushafId: String, val page: Int, val firstVerse: String, val lastVerse: String,
    val status: HifzStatus /*NEW,SABAQ,SABQI,MANZIL*/, val memorizedOn: LocalDate?,
    val stability: Double, val difficulty: Double, val due: LocalDate?, val lapses: Int)
@Entity data class HifzReview(@PrimaryKey(autoGenerate = true) val id: Long = 0, val mushafId: String,
    val page: Int, val kind: HifzStatus, val on: Instant, val mistakes: Int, val peeks: Int,
    val rating: Int /*1..4*/, val method: String /*SELF,USTAD,AI*/, val mistakeWordIds: String /*csv*/)
```

### 2.12 AI recitation checking (Tarteel-like)

**Model landscape** (HuggingFace API, 2026-10-19) [verified unless noted]:

| Model | Arch / base | Licence | Size | Reported metric | Notes |
|---|---|---|---|---|---|
| tarteel-ai/whisper-base-ar-quran | Whisper base FT | Apache-2.0 | 290 MB fp32 (`pytorch_model.bin`) | WER 5.75% (own eval set) | 44.9k downloads; ONNX community ports exist (e.g. `YunusZJ/whisper-base-ar-quran-ONNX`, Apache-2.0) |
| tarteel-ai/whisper-tiny-ar-quran | Whisper tiny FT | Apache-2.0 | 151 MB fp32 | WER 7.05% | whisper.cpp tiny is about 75 MiB on disk in ggml f16 [verified: whisper.cpp README]; q5/q8 is smaller [unverified] |
| mohammed/fastconformer-quran-ar | NVIDIA FastConformer Hybrid Large FT | **CC-BY-4.0** | 114.6M params; 459 MB `.nemo` | "0.14% WER on everyayah validation" | Offline + cache-aware streaming. Same-reciter validation is probably optimistic for learners |
| Muno459/fastconformer-quran | FastConformer + pronunciation head | "npl-1.1" (gated) | `model.q8.onnx` 131.8 MB | — | Gated, custom licence: avoid |
| Quran-Lab/zipformer_p-arabic-v3 | Zipformer (phonemes) | "quran-lab-npl-1.2" (gated) | int8 ONNX 72.7 MB | — | Gated, custom licence: avoid unless licensed |
| TBOGamer22/wav2vec2-quran-phonetics | wav2vec2-base, phonetic CTC | Apache-2.0 | 377.6 MB | — | Phoneme-level, useful for tajweed research |
| obadx/muaalem-model-v3_2 | w2v-BERT 2.0, multi-level CTC (Quran Phonetic Script) | HF card: MIT; **GitHub repo: QML v1.0** | 2,423 MB | PER 0.21% test / 1.94% qdat_bench; **Tajweed F1 75.8%** (arXiv 2509.00094) | Only open tajweed-error model. Server-only size. Licence: see below |

**QML (Quran Muaalem License) v1.0** (repo commit 2026-10-19). It is Apache-2.0 plus Additional Conditions [verified: LICENSE text]:

- **2.1 AI Advisory Notice.** Every End User must see, "before first use" and always reachable from the menu/About, in each UI language "and always also in Arabic": *"This Quranic Teacher is an artificial-intelligence model that may make mistakes. It is intended for assistance only and must not be relied upon. It is not a replacement for a proficient, authorized teacher…"*
- **2.2** "Every use … must adhere to the rulings of Islamic Sharia. Any use not adhering … is unlicensed."
- **2.3(a) NO ADVERTISEMENTS.** "The Work, Derivative Works, and every Covered Product must never include Advertisements, and must never include or run advertisement-serving code…". A Covered Product is the whole app that "includes, runs, or is built upon the Work". Donations are allowed.
- **2.3(b)** requires a free-access path at the same model quality (text truncated in our read; see "Essential Functionality").
- **Consequence:** usable only if the owner commits to **no ads, forever**, which 02 recommends anyway. Also note the HF/GitHub licence mismatch: get written clarification.

**Why CTC + alignment rather than Whisper for *checking*:** Whisper is a sequence-to-sequence model with a strong internal language model. Fine-tuned on Quran, it tends to emit the canonical verse even when the reciter errs, which hides mistakes [reasoned; Medium]. It is fine for **search and follow-along**. Checking needs per-token posteriors from a CTC model, aligned to the *expected* text.

```text
mic 16 kHz → Silero VAD (sherpa-onnx) → CTC model (int8 ONNX, sherpa-onnx OfflineRecognizer / streaming)
  → greedy tokens (letters/phonemes, **no LM bias**) → normalise (§2.14) → banded Needleman-Wunsch vs expected window
  → events {MATCH advance, SUBSTITUTION, OMISSION, INSERTION, HESITATION(>3 s)} → UI (highlight / hint next word / mark)
Modes: Follow-along (tolerant, auto page-turn) · Test (strict, per-word marks) · Search (no expected text → n-gram match over corpus)
```

- **Runtime.**
  - sherpa-onnx: Apache-2.0, version 1.13.8 in CMakeLists. Supports Kotlin/Java, Android and Wear OS; Whisper, Zipformer, NeMo CTC/transducer; Silero VAD [verified: repo README/LICENSE].
  - Alternatives: ONNX Runtime Android 1.31.0 [verified: Maven]; LiteRT 2.3.0 [verified: Google Maven]; whisper.cpp (MIT) [verified].
- **On-device budget.**
  - Model as a Play on-demand asset pack or CDN download (100–150 MB int8 for a ~100M-param CTC model; ~40–60 MB for quantised whisper-tiny/base) [estimate].
  - RAM ≤ 400 MB.
  - Target RTF ≤ 0.3 on Helio G99 / Snapdragon 6-series. **Unmeasured:** the only public figure found is a third-party claim of "5–10× realtime" for int8 Zipformer on Snapdragon 7-series [unverified].
- **Data provenance risk.** `tarteel-ai/everyayah`, which most models train on, has **no licence** on HF [verified]. `tarteel-ai/tlog` is CC-BY-4.0 but gated [verified]. `obadx/muaalem-annotated-v3` is MIT [verified].
- **Gate before building (v1.x spike, ~2 weeks).** Collect 50–100 consented recordings from Pakistani adults and children, including deliberate mistakes.
  - Metrics: word-mistake recall and precision; **false alarms per minute ≤ 0.5** (falsely flagging correct recitation is the main religious-UX harm); latency.
  - Build only if a permissively licensed model passes.

### 2.13 Khatam planner, streaks, bookmarks, notes

**Planner algorithm (pages / para / ruku units; Indo-Pak Para table from 05):**

```text
remaining = totalUnits(plan) − doneUnits
days      = activeDaysBetween(today, target, restDaysMask)
goal      = ceil(remaining / days) → snap end to nearest ruku/ayah boundary (setting: "stop at ruku ع")
catch-up  = recompute daily (missed days redistribute; never "shame" copy)
presets   = 30 days (≈1 para/day; 548 pp ÷ 30 ≈ 18.3 pp/day in 16-line), Ramadan 29/27 nights (Taraweeh), 7 days (Manzil-wise), 60 days, custom
progress  = explicit "Mark today's portion read" + auto-suggest when pages dwelt ≥ N s
```

- **Pointers.** Keep several independent pointers: Last read (auto), one per khatam plan, Hifz, and Listening position. This fixes the reference app's "bookmark overwrites resume" complaint (01).
- **Streaks.** Private by default. Offer 2 "freeze" days a month, and **no public leaderboards**, to avoid encouraging riya (showing off). Weekly summary: pages, minutes, paras completed.
- **Bookmarks.** Folders with colours, ayah or page targets, notes and highlight ranges (verse key + word range). Export them in the JSON backup.

```kotlin
@Entity data class KhatamPlan(@PrimaryKey(autoGenerate = true) val id: Long = 0, val title: String,
    val mushafId: String, val unit: String /*PAGE,PARA,RUKU*/, val startVerse: String, val endVerse: String,
    val startDate: LocalDate, val targetDate: LocalDate, val restDaysMask: Int, val dedication: String?,
    val groupId: String? /*remote khatam*/, val completedAt: Instant?)
@Entity(primaryKeys = ["planId", "unitIndex"]) data class KhatamProgress(val planId: Long, val unitIndex: Int, val doneAt: Instant)
@Entity data class Bookmark(@PrimaryKey(autoGenerate = true) val id: Long = 0, val verseKey: String?,
    val mushafId: String?, val page: Int?, val folderId: Long?, val color: Int, val note: String?, val createdAt: Instant)
```

### 2.14 Search

**Normalisation.** Index Tanzil *simple-clean* text (05). Do not index Uthmani or Indo-Pak display text, because rasm differences (الصلوة / الصلاة, يٰٓأَيُّهَا) break matching. Apply the same function to queries:

```kotlin
private val MARKS = Regex("[ؐ-ًؚ-ٰٟۖ-ۭ࣓-ࣿـ]") // harakat, Quranic marks, tatweel
fun norm(s: String) = MARKS.replace(s, "")
    .replace(Regex("[آأإٱٲٳ]"), "ا")      // آ أ إ ٱ → ا
    .replace('ى', 'ي').replace('ی', 'ي').replace('ے', 'ي') // ى ی ے → ي
    .replace('ک', 'ك')                                              // ک → ك
    .replace('ة', 'ه').replace('ہ', 'ه').replace('ھ', 'ه') // ة ہ ھ → ه
    .replace('ؤ', 'و').replace('ئ', 'ي')                  // ؤ → و, ئ → ي
// plus a light clitic stripper for tokens: leading و ف ب ك ل and ال
```

- **Engine.**
  - Room `@Fts4` over normalised columns on platform SQLite. FTS4's `unicode61` tokenizer does not strip Arabic marks, so normalise first [unverified detail].
  - For infix and trigram matching, switch to the AndroidX bundled SQLite driver. Its build enables `SQLITE_ENABLE_FTS5`, FTS3/4, JSON1, RTREE and MATH_FUNCTIONS [verified: androidx-main `sqlite-bundled/build.gradle`; latest stable 2.7.1 per Google Maven].
- **Translation search.** Urdu/English FTS per installed translation pack (05 pack design). Urdu uses the same character folding plus removal of zer/zabar/pesh.
- **Voice search.** Android `SpeechRecognizer` (online, Arabic) is quick to add but recognises MSA, not tilawat [unverified]. Better: v2 "recite to find" with the Quran ASR model (§2.12) plus n-gram lookup.
- **Topic / "semantic" search, retrieval-only.**
  1. QUL "Topics and concepts" (2,512 topics) and "Ayah theme" (1,049) as a curated topical index [verified: QUL resources; per-resource permission needed].
  2. Optional v2 on-device embeddings: `intfloat/multilingual-e5-small` (MIT, 94 languages incl. Urdu, `model_qint8` ONNX 118 MB) [verified: HF API]. Precompute ayah-translation vectors offline (6,236 × 384 int8 ≈ 2.4 MB per language).
  3. EmbeddingGemma-300m is under the Gemma licence, gated, LiteRT builds 176–252 MB [verified]. Heavier, with non-OSI terms: skip.
- **Guardrails (non-negotiable).**
  - No generative tafsir, fatwa or "AI answers".
  - Results are only Qur'an ayahs or licensed tafsir passages, each with source and translator attribution.
  - Show a "meaning-based results may be imperfect" note.
  - Everything runs offline, and queries are not logged remotely.
  - If an LLM is ever added, it may only rerank retrieved passages, never write religious content.

### 2.15 Reminders, widgets, share

| Reminder | Default | When | Mechanism |
|---|---|---|---|
| Ayatul Kursi after fard salah | Opt-in at onboarding (loved in reference reviews, 01) | Prayer + 10–15 min | Exact-while-idle |
| Surah Al-Kahf | On | Friday, Fajr + 1 h (window till Maghrib) | Inexact window |
| Surah Al-Mulk | On | Isha + 30 min | Inexact |
| Yaseen / Waqiah / Rahman | Off | User time | Inexact |
| Morning / evening azkar | Prompt | Fajr + 30 / Asr + 15 | Inexact |
| Daily ayah | On | User time | **Curated 365-ayah list** (scholar-reviewed). Never random verses that read wrongly out of context |
| Khatam / Hifz goal | With plan | User time | Inexact |

Cap non-prayer notifications at 3/day, with quiet hours. Every notification deep-links to the exact page or ayah.

**Widgets** (Glance `glance-appwidget` 1.2.0 stable [verified]):

- **Next prayer, 2×1:** name, time, Hijri date. Updated at each prayer boundary by the alarm chain and by the daily job. Shows the time rather than a seconds countdown. A platform `RemoteViews` chronometer could be embedded [unverified].
- **All prayers, 4×2.**
- **Continue reading, 2×2:** surah, page, para, plus today's khatam goal.
- **Ayah of the day, 4×2:** Arabic, translation, reference.
- Optional **Tasbeeh +1**.

**Share ayah:**

- Offscreen-render a Compose card (1080×1350 and 1080×1920 "status") with Arabic (OFL DigitalKhatt per 04), translation **with translator attribution** (required by Tanzil and QuranEnc terms, 05), reference "Al-Baqarah 2:255 · Para 3", and a small app mark.
- Backgrounds use geometric or plain themes only: no animate imagery.
- Use the PNG via `FileProvider`, then `ACTION_SEND` (WhatsApp first).
- "Copy as text" includes the reference, strips PUA characters (05), and appends the app link.

### 2.16 Noorani Qaida, kids, Urdu, Roman Urdu, accessibility

- **Noorani Qaida.** No open-source Qaida app or openly licensed Qaida audio was found [verified: search]. The Arabic "Al-Qaida An-Nooraniya" variant from Qamar Apps is reportedly CC BY-SA 4.0, but its layout differs from the Pakistani edition [unverified].
  - Plan: typeset **our own** lesson grid. Letters and harakat combinations are facts, not copyrightable [interpretation]. Record about 1–2k short clips with a local qari under work-for-hire.
  - Features: tap-to-hear, listen-and-repeat, per-takhti progress, teacher mode.
  - Effort L, mostly content.
- **Kids.** Play: "if you choose to include imagery and terminology in your app that could be considered targeting children, this may impact Google Play's assessment of your declared target audience". Ads shown to children "or to users of unknown age" must use Families Self-Certified SDKs [verified: Play Families policy].
  - Keep Qaida styling age-neutral (adults learn Qaida too). If a cartoon kids mode is wanted, build it as a separate app.
- **Urdu UI.**
  - `supportsRtl`; per-app language via `localeConfig` plus `AppCompatDelegate.setApplicationLocales` [unverified API detail].
  - Noto Nastaliq Urdu (OFL; "Copyright 2022 The Noto Project Authors") for Urdu text, with a line-height of about 1.8–2.0×. Noto Naskh Arabic (OFL) for compact UI, and Gulzar (OFL) as an alternative Nastaliq [verified: google/fonts METADATA/OFL].
  - Jameel Noori Nastaleeq is popular, but no licence was found [unverified]. Avoid it.
- **Roman Urdu.** A separate string set (`values-b+ur+Latn`) [unverified], written by a native speaker. It is not a transliteration of the Quran text.
- **Accessibility.**
  - Each ayah node exposes `contentDescription = "Surah Al-Baqarah, ayah 255"` plus the active translation.
  - Custom accessibility actions: "Play recitation", "Bookmark", "Translation".
  - **Do not TTS the Arabic Quran.** Tilawat via TTS is inappropriate and garbles diacritics [religious / UX judgement].
  - UI scales to 200% font (nonlinear scaling on Android 14+). Touch targets ≥ 48 dp. WCAG AA contrast in night and sepia themes. Reduce-motion respected. Haptics optional.

### 2.17 Backup and sync options

| Option | Effort | Cost / limits | Pros | Cons | Verdict |
|---|---|---|---|---|---|
| **Android Auto Backup** | S | Free; "limited to 25 MB per app"; runs "roughly every night" when idle and on Wi-Fi; "end-to-end encrypted on devices running Android 9 or higher using the device's PIN" [verified] | Zero UI; restores at install | Android-only; user may have backup off; restores only at install | **MVP.** Include Room DB and DataStore; exclude audio and mushaf packs via `dataExtractionRules` |
| **JSON export / import (SAF)** | S | Free | User-owned; shareable over WhatsApp or Drive; works without GMS | Manual | **MVP** |
| **Quran.com account (QF User APIs)** | M | Free; needs OAuth client registration ("Request Access") | Sync with Quran.com web/apps: bookmarks, collections, notes, goals, streaks, reading sessions, activity days, preferences. "If you select Frontend or mobile app, use a fully public in-app token exchange flow without a client secret" (AppAuth recommended). Terms allow ads/IAP; forbid using User Data "to build advertising profiles" [verified: api-docs.quran.foundation] | Dependency on QF; their data model is verse-key based (fine) | **v1.x optional** "Sign in with Quran.com" |
| **Firebase** (Auth + Firestore + FCM) | M | Spark: 50k reads/day, 20k writes/day, 1 GiB, 10 GiB egress/month, 50k MAU [verified: firebase.google.com/pricing] | Offline cache, realtime, FCM, no pausing | Google lock-in; Data-safety disclosure | **v1.x for group khatam only** |
| **Supabase** | M | Free: 500 MB DB, 50k MAU; "**Free projects are paused after 1 week of inactivity**"; Pro $25/mo [verified: supabase.com/pricing] | Postgres, RLS, OSS | Pausing breaks a low-traffic hobby app | Not on Free |
| Google Drive `appDataFolder` | M | Free | User's own storage | OAuth consent/verification work [unverified] | No (Auto Backup covers it) |

### 2.18 Family / group khatam (esal-e-sawab)

- **v1.0 (no backend): "Khatam sheet".**
  - Pick a dedication (e.g. "Esal-e-sawab: Marhoom Muhammad Aslam"), a deadline and a unit (30 Paras, or 7 Manzils for Shabeena).
  - The app generates WhatsApp-ready text: "1. Alif Lam Meem — Ahmed [done] / 2. Sayaqool — ____ …".
  - "Paste updated list" parses claims back in.
  - The dua-e-khatam screen opens when the khatam is complete.
  - S effort. It matches existing behaviour exactly.
- **v1.x (realtime).**
  - Firestore `khatams/{id}` holds title, dedication, unit, mushaf, deadline, createdBy, inviteCode and status.
  - `khatams/{id}/parts/{n}` holds `claimedBy, claimedName, claimedAt, completedAt`; claims run in a **transaction**.
  - `members/{uid}` holds displayName and role.
  - Firebase Auth: anonymous, upgradeable to Google via Credential Manager (`androidx.credentials` 1.6.0 stable [verified]).
  - App Links invite URL; an FCM topic per khatam announces "Para 12 completed" and "Khatam complete — make dua".
  - Privacy: display names only, auto-delete 90 days after completion, report/leave controls.
  - L effort.

---

### 2.19 Prioritised feature backlog (companion + differentiators)

Phases:

- **MVP:** the owner's personal daily driver, with the Quran core from 01/04/05/06.
- **v1.0:** first public Play release; parity-plus with the reference app.
- **v1.x:** about 1–4 months after launch.
- **v2:** six months or more.

MoSCoW is relative to v1.0.

| ID | Feature | MoSCoW | Phase | Effort | Depends on |
|---|---|---|---|---|---|
| C1 | Prayer times screen (Adhan2, region defaults, Hanafi toggle, ihtiyat offsets, today/tomorrow, monthly table) | Must | MVP | M | Kotlin 2.4 bump, desugaring, C6 |
| C2 | Prayer notifications: per-prayer Off/Silent/Short/Full, pre-prayer reminder, channel set | Must | MVP | M | C1, exact-alarm flow |
| C3 | Full adhan service (mediaPlayback FGS, USAGE_ALARM, silent/DND respect, Stop, Fajr adhan) | Must | MVP | M | C2, C3a |
| C3a | Adhan audio assets (commissioned + CC0 fallbacks) | Must | MVP | S (+ recording) | — |
| C4 | Adhan reliability kit (OEM guides via DKMA, health check, test adhan, lateness log, daily safety-net job) | Must | MVP | M | C2 |
| C5 | Post-prayer Ayatul Kursi nudge; Jumu'ah/Kahf combo | Should | v1.0 | S | C2 |
| C6 | Coarse location + offline GeoNames city search (Urdu names) | Must | MVP | S–M | — |
| C7 | Qibla compass (rotation vector, declination, calibration, interference) | Should | v1.0 | M | C6 |
| C8 | Sun-mode Qibla + Rasd al-Qibla notification | Could | v1.x | S | C7 |
| C9 | Hijri date (UQ via HijrahDate, ±2 offset, Maghrib rollover) | Must | MVP | S | Desugaring |
| C10 | PK moon-sighting override feed (signed JSON) + monthly ops | Should | v1.0 | S | C9, static hosting |
| C11 | Islamic events calendar (toggles, neutral labels) | Should | v1.0 | S | C9/C10 |
| C12 | Ramadan: timetable + share image, countdown, Sehri alarm, sourced duas, Taraweeh plan | Should | v1.0 (before Feb 2027) | M | C1, C9, D3 |
| C13 | Morning/evening azkar with counter, reminders, Urdu | Should | v1.0 | M | Azkar dataset + Urdu translation |
| C14 | Full dua library (categories, search, audio) | Could | v1.x | M | Content licensing |
| C15 | "Manzil" dua reader + audio | Should | v1.0 | S | Quran engine |
| C16 | Tasbeeh (haptics, presets, targets, volume keys, history) | Should | MVP | S | — |
| C17 | Salah log / qaza tracker (private) | Could | v1.x | S–M | C1 |
| C18 | Zakat / Fitrana calculator | Won't (v1) | v2 | M | Live gold/silver prices |
| D1 | Multiple independent pointers (last-read, per-khatam, hifz, listening) | Must | MVP | S | Core reader |
| D2 | Bookmarks with folders/colours; notes; highlights | Must (bookmarks) / Should (notes, highlights) | MVP / v1.0 | M | Core reader |
| D3 | Khatam planner (units, presets, catch-up, ruku snap) | Must | v1.0 | M | Para / ruku tables (05) |
| D4 | Private streaks + stats (freeze days) | Should | v1.0 | S | D3 |
| D5 | Smart Quran reminders (Kahf, Mulk, daily curated ayah) | Should | v1.0 | S | C2 infra, curated list |
| D6 | Widgets: next prayer, all prayers, continue reading, ayah of day | Should | v1.0 | M | C1, D1 |
| D7 | Share ayah as image/text with attribution | Should | v1.0 | S–M | Renderer (04), translations (05) |
| D8 | Hifz core: hide/reveal, peeks, range repeat, record-yourself, mistake marking | Should | v1.x | L | Word boxes (04), gapped audio (06) |
| D9 | Sabaq/Sabqi/Manzil planner + FSRS manzil ordering + juz grid | Should | v1.x | M | D8 |
| D10 | Mutashabihat alerts | Could | v1.x | S–M | QUL permission |
| D11 | Ustad/parent weekly report | Could | v1.x | S | D9 |
| D12 | AI evaluation spike (harness, 50–100 PK recordings) | Should | v1.x | M | sherpa-onnx |
| D13 | AI follow-along (auto page-turn while reciting) | Could | v2 | L | D12 pass |
| D14 | AI hifz test (word-level mistakes) | Could | v2 | XL | D13 |
| D15 | Tajweed-level feedback (Muaalem-class, server) | Won't (now) | v2+ | XL | No-ads commitment, licence |
| D16 | Tajweed colouring, Madani view | Could | v2 | M | cpfair CC BY 4.0 regeneration (05) |
| D17 | Tajweed colouring, Indo-Pak | Won't (now) | — | XL | No data |
| D18 | WBW tap popup (Urdu/English + word audio) | Should | v1.x | M | WBW licence (05) |
| D19 | Root / morphology explorer | Could | v2 | M | QAC GPL pack (05) |
| D20 | Diacritic-insensitive Arabic search + Urdu/English FTS | Must | MVP (Arabic) / v1.0 (translations) | M | Tanzil simple-clean (05) |
| D21 | Topic search (QUL topics / themes) | Could | v1.x | S–M | QUL permission |
| D22 | Recite-to-search | Could | v2 | M | D13 model |
| D23 | Embedding semantic search (retrieval-only) | Could | v2 | L | e5-small pack |
| D24 | Noorani Qaida module (own content + audio) | Should | v1.x | L | Recording budget |
| D25 | Kids mode | Won't (in this app) | Separate app | M | Families policy |
| D26 | Urdu UI, RTL, per-app language | Must | MVP | M | — |
| D27 | Roman Urdu UI | Could | v1.x | S–M | D26 |
| D28 | Accessibility baseline (semantics, 200% text, contrast, custom actions) | Must | MVP | M | — |
| D29 | Android Auto | Could | v1.x | S | Media3 service (06) |
| D30 | Wear OS (prayer tile/complication, tasbeeh) | Could | v2 | M | C1, C16 |
| D31 | Tablet two-page spread | Should | v1.x | M | Renderer (04) |
| D32 | Auto Backup + JSON export/import | Must | MVP | S | — |
| D33 | Sign in with Quran.com sync | Could | v1.x | M | QF client approval |
| D34 | Group khatam: WhatsApp sheet | Should | v1.0 | S | D3 |
| D35 | Group khatam: realtime (Firebase) | Could | v1.x | L | Firebase, App Links |

**MVP companion budget:** C1–C4, C6, C9, C16, D1, D2 (bookmarks), D20 (Arabic), D26, D28, D32. That is about 7–9 dev-weeks on top of the Quran core.

**v1.0 adds:** C5, C7, C10–C13, C15, D2 (notes), D3–D7, D20 (translations), D34. That is about 8–10 dev-weeks.

---

## 3. Recommendations

1. **Adopt Adhan2 0.0.7 and bump Kotlin to 2.4.x; enable core-library desugaring.** If the bump is blocked, pin adhan2 0.0.6.
   - *Rationale:* it is MIT, accurate (Meeus), has Karachi/Hanafi and high-latitude rules, is offline, and includes Qibla and Sunnah times. 0.0.7's stdlib 2.4.0 is outside Kotlin 2.2's forward-compatibility window. Add the §2.2.3 golden tests in CI.
   - *Confidence:* High (library) / Medium (exact AGP built-in Kotlin upgrade mechanics; confirm with the build researcher).
2. **Region-aware defaults: PK = Karachi + Hanafi.** Show an onboarding choice "Asr: Hanafi (later) / Standard (earlier)", per-prayer ihtiyat offsets, and UK → MoonsightingCommittee + `recommendedFor` high-latitude rule.
   - *Rationale:* this matches local timetables, avoids alienating Ahl-e-Hadith users, and handles the large UK diaspora correctly.
   - *Confidence:* High (PK) / Medium (diaspora).
3. **Implement adhan per §2.3.**
   - `SCHEDULE_EXACT_ALARM` with a clear explanation screen.
   - `setAlarmClock` for audible adhans (with a toggle) and `setExactAndAllowWhileIdle` for silent ones.
   - Full rescheduling on all system triggers plus a daily WorkManager net.
   - Full adhan via a `mediaPlayback` FGS with `USAGE_ALARM`, and app-enforced silent/DND respect (off by default).
   - Short adhan via a notification-channel sound.
   - Never declare `USE_EXACT_ALARM`, `ACCESS_NOTIFICATION_POLICY` or `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
   - *Rationale:* it is policy-compliant on Play, survives Doze, and stays correct when we move to targetSdk 37 under Android 17 audio hardening. It also fixes the reference app's #1 prayer complaint.
   - *Confidence:* High.
4. **Ship an "Adhan health check" plus OEM-specific setup from DontKillMyApp (CC-BY, attributed) as a launch requirement.** Gate release on the §2.3.5 device matrix, including Infinix/Tecno.
   - *Rationale:* about 62% of PK mobile devices are from vendors with aggressive background killing. No API fixes this, so only guidance and diagnostics help.
   - *Confidence:* High.
5. **Commission adhan recordings from a Pakistani muezzin** (standard, Fajr, short takbeer, post-adhan dua) under a written rights assignment. Bundle 1–2 Commons CC0 alternatives as fallbacks.
   - *Rationale:* no famous adhan recording has a clear licence, and bundled audio keeps adhan working offline from day 1.
   - *Confidence:* High.
6. **Coarse location only, plus offline GeoNames city search with Urdu names (CC BY 4.0, ~1.5 MB zipped).** No fine or background location, ever.
   - *Rationale:* the accuracy is sufficient (computed), privacy is better, Play's location-button policy can't touch us, and it works without GMS.
   - *Confidence:* High.
7. **Qibla in v1.0** using Adhan bearing, rotation-vector fusion, `GeomagneticField` declination, and calibration/interference UX. Add Sun mode and Rasd al-Qibla in v1.x.
   - *Rationale:* it is expected in all-in-one apps; the reference app lacks it in-app (01); it is cheap given Adhan.
   - *Confidence:* High.
8. **Hijri from `HijrahDate` + ±2 offset + Maghrib rollover + a signed "PK month-start" remote feed** that the owner updates monthly. Islamic events are toggles with neutral naming.
   - *Rationale:* Pakistan is UQ −1 right now, and a wrong Hijri date around Ramadan or Eid destroys trust.
   - *Confidence:* High (approach) / Medium (ops commitment).
9. **Azkar.**
   - Build our own curated dataset: Arabic from `osamayy/azkar-db` (public-domain dedication) plus per-item hadith references; English from fitrahive (MIT), or from sunnah.com with permission.
   - **Commission a scholar-reviewed Urdu translation** of about 150 core duas, or obtain HadeethEnc permission.
   - Ship morning/evening azkar and the "Manzil" dua in v1.0.
   - *Rationale:* there is no openly licensed Urdu Hisn al-Muslim; the azkar-db Arabic is clean to use; Manzil is pure Quran and highly searched in PK.
   - *Confidence:* Medium–High.
10. **Tasbeeh in MVP (S); Ramadan bundle (M) before Ramadan 1448 (≈Feb 2027).** Include a shareable timetable image and only duas with stated sources.
    - *Rationale:* huge standalone demand (5–10M-install tasbeeh apps), trivial effort, and Ramadan is the peak acquisition window.
    - *Confidence:* High.
11. **Make Hifz the headline differentiator in v1.x, without AI.**
    - Sabaq/Sabqi/Manzil planner on the edition's pages.
    - Hide/reveal with peeks; range repeat; record-yourself; mistake marking with a heatmap.
    - FSRS ordering of Manzil; mutashabihat alerts; ustad report.
    - *Rationale:* it serves a large, loyal PK segment no Indo-Pak-first app serves well. It relies only on assets we already need (word boxes, gapped audio) plus small open data.
    - *Confidence:* High (value) / Medium (exact pedagogy: validate with 2–3 ustads).
12. **Treat AI recitation checking as a gated v2 bet.**
    - Run a 2-week evaluation spike in v1.x with sherpa-onnx and permissively licensed CTC models (FastConformer CC-BY-4.0; Tarteel Whisper Apache-2.0 for follow-along and search).
    - Build only if false alarms are ≤0.5/min and word-mistake recall is acceptable on Pakistani learners.
    - Always show an AI advisory notice. Never claim tajweed grading unless it is validated.
    - Consider Muaalem (server) only if the app is permanently ad-free and the QML terms are accepted.
    - *Rationale:* Tarteel shows the demand, but wrongly flagging correct recitation is a real religious-UX harm. The licences and dataset provenance are messy.
    - *Confidence:* Medium.
13. **Search.**
    - Normalised Tanzil simple-clean Arabic plus FTS in MVP. Urdu/English translation FTS in v1.0.
    - QUL topics in v1.x. Optional on-device e5-small semantic retrieval in v2.
    - **Ban generative religious content.** Retrieval only, with citations.
    - *Rationale:* high utility at low cost. Generative tafsir carries unacceptable accuracy and sectarian risk.
    - *Confidence:* High.
14. **Khatam planner, independent pointers, private streaks, curated reminders and widgets in v1.0.** Glance widgets: next prayer, continue reading, ayah of the day. Smart reminders are capped at 3/day.
    - *Rationale:* daily-habit drivers that also directly fix the reference app's bookmark/resume complaints. Low effort.
    - *Confidence:* High.
15. **Share-as-image with mandatory reference and translator attribution, geometric backgrounds only.**
    - *Rationale:* WhatsApp-status culture means free distribution, and attribution is a licence condition (05).
    - *Confidence:* High.
16. **Group khatam.** The WhatsApp text sheet in v1.0 (no backend). Realtime Firestore khatam with transactions, FCM and App Links in v1.x.
    - *Rationale:* a perfect cultural fit (esal-e-sawab, Shabeena) with almost no competition. The text sheet validates demand with zero infrastructure.
    - *Confidence:* Medium–High.
17. **Sync.** Auto Backup plus JSON export in MVP. Optional "Sign in with Quran.com" via QF User APIs (public PKCE client, no own backend) in v1.x. Firebase Spark only for group features. Not Supabase Free.
    - *Rationale:* lowest ops burden, cross-app sync for free, and avoids free-tier pausing.
    - *Confidence:* Medium (QF client approval needed).
18. **Noorani Qaida as our own content in v1.x (age-neutral design). Any cartoon kids mode as a separate app.**
    - *Rationale:* 1M+-install demand. Child-targeted imagery risks Families-policy classification of the whole app.
    - *Confidence:* Medium.
19. **Urdu-first UI from MVP.** Noto Nastaliq Urdu (OFL), RTL, per-app language, an accessibility baseline (ayah semantics, custom "Play recitation" action, no Arabic Quran TTS, 200% text). Roman Urdu in v1.x.
    - *Rationale:* core audience and elderly users; the cost is lowest when built in from the start.
    - *Confidence:* High.
20. **Defer** Wear OS, Zakat, Indo-Pak tajweed colouring, morphology explorer and embedding search to v2. Android Auto (S, via the 06 Media3 service) and the tablet spread go in v1.x.
    - *Rationale:* lower value per effort for the PK daily user.
    - *Confidence:* Medium.

---

## 4. Risks & open questions

### Risks

- **Kotlin upgrade ripple.** Kotlin 2.2.10 → 2.4.x affects the Compose compiler plugin, AGP 9.2.1 built-in Kotlin and other libraries. Mitigation: pin adhan2 0.0.6 temporarily. The library is small enough to vendor if needed (MIT).
- **Exact-alarm denial.** Android 14+ fresh installs start without the permission. If the user declines, adhan becomes inexact (up to 1 h late) and the UI must say so loudly. The user or the system can also revoke it later; `canScheduleExactAlarms()` is checked every time.
- **OEM killing** cannot be fully solved. Expect a residual "adhan didn't play" rate on Transsion, Vivo and Xiaomi. The health check and lateness log make it diagnosable.
- **Android 17 audio hardening plus `USAGE_ALARM` semantics.** The adhan plays at alarm volume and bypasses silent mode, so our own silent/DND checks must be correct. Play's target-API deadline will push us to API 37 within about a year [unverified date].
- **Adhan audio provenance.** Commons "CC0" uploads may not be the uploader's own recordings, so prefer a commissioned recording.
- **Hijri and maslak sensitivity.** Wrong dates around Ramadan or Eid, and naming of 12 Rabi-ul-Awwal, Shab-e-Barat and Shab-e-Mi'raj. The monthly feed is an ops commitment; if it lapses, users see UQ±offset.
- **Azkar translation rights and accuracy.** Seen-Arabic's MIT label sits over English text taken from hisnmuslim.com and sunnah.com. Urdu needs commissioning or permission, and scholar review is mandatory.
- **AI.**
  - Muaalem's QML bans ads app-wide and has a Sharia clause, and its HF card says MIT while the repo says QML.
  - The everyayah training data has no licence.
  - Accuracy on child and learner voices is unknown.
  - Microphone permission and privacy expectations apply; audio stays on-device.
  - The models are large (100–150 MB) for low-storage phones.
- **Families policy** if Qaida or kids visuals look child-directed.
- **Data-safety accuracy.** Firebase, FCM, location and microphone must be declared correctly. 01 found the reference app's declaration contradicts its SDKs; we must not repeat that.
- **Group khatam.** Spam or abuse of invite links, personal names of the deceased in dedications (sensitive data), and retention. Mitigate with display-name-only design, auto-expiry and report/leave controls.
- **QF dependency.** Client approval, API changes, and Terms updates (last updated 2026-10-11).
- **Notification fatigue** will hurt retention if the reminder caps are ignored.

### Open questions (for the product owner)

1. Is the app **permanently ad-free**? This decides Muaalem/QML eligibility, Tanzil translation use (05) and Families exposure.
2. What is the primary **hifz edition**: 15-line Hafizi or the 16-line default reader? Can we interview 2–3 local ustads to validate the Sabaq/Sabqi/Manzil defaults?
3. Default Asr: Hanafi with an onboarding choice. Is that acceptable, or should the onboarding ask the user's maslak explicitly?
4. Who maintains the **monthly Ruet-e-Hilal feed**, and is a 5-minute monthly ops task acceptable?
5. What is the budget for commissioned content: adhan recordings, about 150 Urdu dua translations plus scholar review, Noorani Qaida audio (~1–2k clips)?
6. Is a small Firebase backend acceptable for v1.x group khatam, given the Google dependency and privacy disclosure? Or should we stay text-sheet-only?
7. Is diaspora (UK/Gulf) a target? If yes, prioritise mosque-timetable import and Gulf method presets.
8. Should we apply for a Quran Foundation OAuth client now (for "Sign in with Quran.com"), given lead time?
9. Which observances should be on by default in the events calendar (neutrality policy)?
10. Should the AI spike start in v1.x, with recruitment of consented test reciters (children need guardian consent)?

---

## 5. Sources

| URL | What was verified |
|---|---|
| `git clone https://github.com/batoulapps/adhan-kotlin` (also `adhan-java` → same repo) | MIT licence; README methods table, Karachi 18/18, Hanafi, high-latitude rules, desugaring note; source: `recommendedFor` (>48° → SEVENTH), Karachi `methodAdjustments dhuhr = 1`, `kotlin.time.Instant`, Qibla/Sunnah API; CHANGELOG 0.0.5–0.0.7; libs.versions.toml (Kotlin 2.4.0, kotlinx-datetime 0.8.0) |
| https://repo1.maven.org/maven2/com/batoulapps/adhan/adhan2/maven-metadata.xml ; …/adhan2-jvm/0.0.7/*.pom ; …/0.0.6/*.pom ; …/adhan/adhan/maven-metadata.xml | adhan2 0.0.7 latest (2026-06-13); deps stdlib 2.4.0 / 2.2.20; legacy adhan 1.2.1 (2021) |
| https://repo1.maven.org/maven2/org/jetbrains/kotlin/kotlin-stdlib/maven-metadata.xml | Kotlin 2.4.21 latest stable; 2.5.0-Beta1 |
| https://kotlinlang.org/docs/kotlin-evolution-principles.html | Forward compatibility "next language release, but not later ones" |
| https://developer.android.com/build/migrate-to-built-in-kotlin | Built-in Kotlin; no KGP-version guidance on page |
| https://developer.android.com/develop/background-work/services/alarms/schedule | SCHEDULE_EXACT_ALARM not pre-granted (13+ fresh installs); revocation cancels alarms; permission-state broadcast; boot receiver; Doze behaviour; inexact "within one hour" |
| https://support.google.com/googleplay/android-developer/answer/9888170 | USE_EXACT_ALARM restricted to alarm/timer/calendar apps; USE_FULL_SCREEN_INTENT auto-grant only alarms/calls |
| https://support.google.com/googleplay/android-developer/answer/12253906 | Policy deadlines: location-button policy (2027-01-27), FGS geofencing removal, developer verification (2026-09-30) |
| https://android-developers.googleblog.com/2026/04/giving-users-clearer-choice-and-everyone-a-safer-more-trusted-app-ecosystem.html (via search) | Location button required for one-time precise location; `onlyForLocationButton` for Android 17 targets |
| https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start | FGS-from-background exemption for exact alarms; SYSTEM_ALERT_WINDOW note |
| https://developer.android.com/develop/background-work/services/fgs/service-types | mediaPlayback permission; systemExempted criteria incl. exact-alarm holders; shortService limits |
| https://developer.android.com/training/monitoring-device-state/doze-standby | setAlarmClock fires in Doze; 9-minute limit; battery-exemption policy and acceptable-use table |
| https://developer.android.com/about/versions/17/behavior-changes-all ; https://developer.android.com/about/versions/17/changes/bg-audio | Background audio hardening; WIU requirement for target 37; exact-alarm + USAGE_ALARM waiver; adb test commands |
| https://dontkillmyapp.com/ ; /xiaomi ; /api/v2/xiaomi.json ; `git clone urbandroid-team/dont-kill-my-app` | Vendor ranking/scores; Xiaomi steps; JSON API; CC-BY content licence; vendor list (no Infinix page) |
| `git clone judemanutd/AutoStarter` | MIT; last commit 2021-07-11 |
| https://gs.statcounter.com/vendor-market-share/mobile/pakistan (+ CSV) ; …/android-version-market-share/mobile/pakistan | PK vendor and Android-version shares, Sep 2026 |
| https://commons.wikimedia.org/w/api.php (imageinfo extmetadata) | Adhan audio licences and durations (CC0 / CC BY-SA) |
| https://download.geonames.org/export/dump/readme.txt ; cities15000.zip, cities5000.zip (downloaded) | CC BY 4.0; sizes; row counts; PK counts; Urdu alternate names; built DB size |
| https://api.aladhan.com/v1/timings/… ; /v1/qibla/… | Cross-check prayer times (Karachi + Hanafi) and qibla for PK cities and Manchester |
| https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/hardware/GeomagneticField.java | Still WMM-2020 "valid until 2025" |
| pygeomag 1.1.0 (PyPI; WMM_2025.COF, WMM_2020.COF) | Declination values and WMM2020-vs-2025 differences for PK cities (computed) |
| https://developer.android.com/studio/write/java8-support-table | HijrahChronology / HijrahDate "Fully implemented class" via desugaring |
| https://dl.google.com/dl/android/maven2/… (desugar_jdk_libs, glance-appwidget, work-runtime, credentials, wear tiles, play-services-location, sqlite-bundled, room-runtime, litert) | Latest versions: desugar 2.1.5, Glance 1.2.0, WorkManager 2.12.0, Credentials 1.6.0, Tiles 1.6.2, location 21.4.0, sqlite-bundled 2.7.1, LiteRT 2.3.0 |
| https://repo1.maven.org/maven2/com/microsoft/onnxruntime/onnxruntime-android/maven-metadata.xml | ORT Android 1.31.0 |
| https://thepakistanconnect.com/?p=139268 | Ruet-e-Hilal: 1 Jumada-ul-Awwal 1448 = Tue 13 Oct 2026 |
| Web search → en.wikipedia.org/wiki/Jumada_I, miqatona.com | Umm al-Qura 1 Jumada I 1448 = 12 Oct 2026 |
| https://www.hisnmuslim.com/api/husn.json, /api/ar/husn_ar.json, /api/en/husn_en.json, /api/en/27.json | API exists; Arabic + English only; no licence found |
| `git clone Seen-Arabic/Morning-And-Evening-Adhkar-DB`, `osamayy/azkar-db`, `fitrahive/dua-dhikr`, `rn0x/hisn_almuslim_json`, `nawafalqari/azkar-api` | Licences (MIT / dedication / none), contents, counts, fields, upstream sources |
| https://hadeethenc.com/api/v1/languages ; /api/v1/categories/list/?language=ur ; /en/home/about | Urdu availability; dhikr categories; "free through all available means"; no licence text |
| Web search (al-fathulmubin.yolasite.com/manzil.php, muftionline.co.za) | Manzil dua verse list; Kandhlawi attribution |
| https://huggingface.co/api/models?author=tarteel-ai ; model cards/eval_results (whisper-base/tiny-ar-quran) | Apache-2.0; WER 5.75 / 7.05; fp32 sizes 290 / 151 MB |
| https://huggingface.co/api/models?search=quran (+ model cards: mohammed/fastconformer-quran-ar, Muno459/fastconformer-quran, Quran-Lab/zipformer_p-arabic-v3, TBOGamer22/wav2vec2-quran-phonetics, mostafanewir70/quran-recitation-ctc-onnx, obadx/muaalem-model-v3_2) | Licences, sizes, claimed metrics, gating |
| https://export.arxiv.org/api/query?id_list=2509.00094 | Muaalem paper abstract: 848 h data, PER 0.21% / 1.94%, Tajweed F1 75.8% |
| `git clone obadx/quran-muaalem` (LICENSE, NOTICE; commit 2026-10-19) | QML v1.0: AI notice, Sharia clause, no-ads clause |
| https://huggingface.co/api/datasets/tarteel-ai/everyayah, …/tarteel-ai/tlog, …/obadx/muaalem-annotated-v3 | Dataset licences (none / CC-BY-4.0 gated / MIT) |
| https://raw.githubusercontent.com/k2-fsa/sherpa-onnx/master/{LICENSE,CMakeLists.txt,README.md} | Apache-2.0; v1.13.8; Kotlin/Android; Whisper / NeMo / Zipformer / Silero VAD |
| https://raw.githubusercontent.com/ggml-org/whisper.cpp/master/{LICENSE,README.md} | MIT; model disk/memory table |
| Web search (HF model card "STT-streaming-zipformer-indian-en") | Third-party "5–10× realtime" claim (unverified) |
| https://huggingface.co/api/models/intfloat/multilingual-e5-small, google/embeddinggemma-300m, litert-community/embeddinggemma-300m, sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2 | Licences, Urdu support, ONNX/LiteRT sizes |
| `git clone cpfair/quran-tajweed` | CC BY 4.0 data; Tanzil Uthmani-indexed; "not actively maintained"; rule list |
| https://qul.tarteel.ai/resources ; /resources/mutashabihat/73 | Resource catalogue (topics 2,512, mutashabihat 5,277, similar ayahs 4,001, ayah themes 1,049); mutashabihat example 2:112 |
| `git clone open-spaced-repetition/FSRS-Kotlin`, `awesome-fsrs`, `fsrs-rs` | FSRS v6 Kotlin, MIT per README (no LICENSE file), ~337 lines; ecosystem list |
| https://api-docs.quran.foundation/docs/user_related_apis_versioned/user-related-apis ; /docs/tutorials/oidc/mobile-apps/android ; /legal/developer-terms | User API feature list; public mobile PKCE client allowed; terms (ads allowed; no ad profiling from User Data; last updated 2026-10-11) |
| https://supabase.com/pricing | Free-tier limits and 1-week inactivity pause |
| https://firebase.google.com/pricing | Firestore and Auth no-cost quotas |
| https://developer.android.com/identity/data/autobackup | 25 MB, cadence, E2E on Android 9+, dataExtractionRules, restore behaviour |
| https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/sqlite/sqlite-bundled/build.gradle | Bundled SQLite compile flags incl. FTS5 |
| https://raw.githubusercontent.com/google/fonts/main/ofl/{notonastaliqurdu,notonaskharabic,gulzar}/ | OFL licences |
| https://support.google.com/googleplay/android-developer/answer/9893335 | Families policy: target-audience assessment from imagery; ads to children / unknown age |
| google-play-scraper 1.2.7 `search(country="pk")` | Demand signals table §2.1 |
| Web search: Noorani Qaida author / open-source | No open Qaida app or audio found; Qamar Apps CC BY-SA claim (unverified) |
| Reports 01–06 in `/home/user/criterion/docs/research/` | Reference-app complaints (silent mode, resume), Indo-Pak Para table, WBW/QAC licensing, renderer word boxes, audio engine |
