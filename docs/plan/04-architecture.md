# 04 — System & App Architecture

**Purpose.** This document is the technical blueprint for the Criterion Android app (codename; public brand at gate G3). It covers the system context, module graph, layering and UDF conventions, the pure-Kotlin domain model, the repository and service interfaces, every on-device data store (with full DDL for `quran_content.db` and `user.db`), the signed pack manifest, navigation, DI, background work, the manifest permission set, build variants and content-eligibility enforcement, security, performance, update flows, diagnostics and the version catalogue. An engineer or AI agent should be able to create the P0 skeleton from this doc and the spike (`spikes/toolchain-spike/`) without redoing research. Doc 04 is the **owner** of module names, interface names, the content-DB schema and the user-DB schema. The mushaf-pack schema belongs to [05](05-mushaf-rendering-engine.md), and the audio catalogue and timing schema to [06](06-audio-engine.md). This doc only states the contracts it relies on. Doc 04 also publishes the shared types every other doc uses verbatim (§5, §6.1: identity types, `MushafId`, `PlayScope`, `PlayRequest`, `PlaybackState`), the NavKeys and deep-link grammar (§8), the build variants and eligibility rule (§11), the signing format (§7.6), the `notices.json` schema (§14.3) and the azkar pack schema (§7.3).

**Status: Draft v2 — 2026-10-19 (review fixes applied; implements the lead's FINAL SHARED-TYPES and VARIANTS rulings)**

**Key decisions in this doc**
- **D-01**: native Android, with pure-Kotlin `:core:model` and `:core:domain` ("KMP-ready, Android-only").
- **D-02**: toolchain as verified in the spike, plus the version catalogue (§16). No Ktor and no Coil in v1.
- **D-03**: a lean module graph of 9 modules in P0, 19 at P1 and 22 at v1.0. Repository interfaces live in `:core:domain`, and features never depend on features.
- **D-04/D-06/D-07**: `MushafId` is an enum carrying a code and a numeric id (`INDOPAK_16_TAJ("indopak16_taj", 1)`). `PageIndex` is kept separate from `PageLabel` (label = index + `pageLabelOffset`, which is 1 for Taj-16). We keep our own Indo-Pak Para/Manzil/Ruku/Sajda/quarter tables. The canonical word table is the DK token stream keyed to Kufan verse keys.
- **D-08/D-09/D-11**: Tanzil text is used for copy and search. Two translation packs ship bundled. Search uses FTS5 unicode61 + trigram over normalized columns.
- **D-10**: storage has three tiers: a read-only `quran_content.db` re-copied on version bump, a migrated `user.db`, and Ed25519-signed packs.
- **D-12/D-13/D-14**: `PlayScope`/`PlayRequest`/`PlaybackState` live in the pure core; `PlaybackStateHub` and `PlaybackController` are `:core:domain` interfaces implemented with Media3 in `:core:audio` (there is no `:core:audio-api` module). In P1, downloads run as WorkManager dataSync workers on every API. UIDT jobs on API 34+ land in S5, before the first BETA build.
- **D-15/D-16**: no GMS in core. Location is coarse only. Alarms use `SCHEDULE_EXACT_ALARM` and `setAlarmClock()`. The manifest has an explicit never-request list.
- **D-17/D-20/D-22/D-29**: `distribution` {play, foss} × `tier` {personal, public}. `BuildConfig.CONTENT_TIER` ∈ {PERSONAL, BETA, PUBLIC}: the public tier is BETA unless built with `-Pcriterion.release=production`. `BuildConfig.MONETIZED` comes from `-Pcriterion.monetized=true`. `verifyContentEligibility` blocks ineligible content, and PUBLIC also needs G1 and G5 evidence (§11). There are no SDKs and no secrets. Kill-switches and text-error notices arrive through a signed `notices.json` (F-PL-11).
- **D-21/D-23/D-24/D-32**: AppCompat per-app locales. Rotation lock on phones only. Static hosting on R2, with no app backend in v1.0. The one exception arrives in v1.1: a small Cloudflare Worker that receives opt-in crash reports (D-32).

---

## 1. System context

```mermaid
flowchart LR
  U((User)) --> APP["Criterion app<br/>(play / foss × personal / public)"]
  subgraph Ours["Operated by us (static, no backend)"]
    R2[("Cloudflare R2 + brand-neutral custom domain<br/>v1/{beta,public}/index.json · catalogue.json · notices.json<br/>v1/feeds/hijri-pk.json (+ detached .sig) · packs · timing packs · permitted audio")]
    CW["Cloudflare Worker (v1.1, D-32)<br/>opt-in crash reports → R2, 90-day retention"]
  end
  subgraph ThirdParty["Third-party audio hosts (mirrors)"]
    MP3Q["cdn.mp3quran.net<br/>gapless per-surah (public default)"]
    EA["everyayah.com<br/>gapped per-ayah (personal / after G2)"]
    ISN["cdn.islamic.network<br/>gapped mirror"]
  end
  APP -- "HTTPS GET, ETag; verify Ed25519 + sha256" --> R2
  APP -- "HTTPS Range GET (stream / download)" --> MP3Q
  APP -- "HTTPS Range GET" --> EA
  APP -- "HTTPS Range GET" --> ISN
  APP -. "v1.x, opt-in, PKCE public client" .-> QF["Quran Foundation User API<br/>(bookmarks/notes sync, F-BM-07)"]
  APP -. "v1.x, play flavor only" .-> FB["Firebase Firestore + FCM<br/>(group khatam, F-CP-11)"]
  APP -- "OS-level crash/ANR (no SDK)" --> VIT["Play Android vitals"]
  APP -. "v1.1, opt-in per report (ACRA, F-PL-09)" .-> CW
  OWN["Owner workstation<br/>tools/ pipelines (doc 08)<br/>offline Ed25519 key"] -- "sign + upload (rclone/wrangler)" --> R2
  CI["GitHub Actions (doc 10)"] -- "AAB (playPublicRelease: BETA → closed test; PUBLIC with -Pcriterion.release=production)" --> PLAY["Google Play"]
  OWN -- "fossPersonalRelease, built locally only (personalCheck);<br/>personal packs never enter git or CI" --> OWNERPHONE["Owner's phone (sideload)"]
```

| External endpoint | Used for | Phase | Failure behaviour |
|---|---|---|---|
| `${CDN_BASE_URL}/v1/<channel>/index.json` (+`.sig`) | Pack index (translation, tafsir, mushaf layout, timing, fonts, azkar, cities). `<channel>` is `beta` or `public` (§11.2) | P1 (v1.1 for downloadable packs) | Keep the last verified copy, and the bundled copy below that. Never block reading |
| `${CDN_BASE_URL}/v1/<channel>/catalogue.json` (+`.sig`) | Audio catalogue override (schema in [06](06-audio-engine.md)) | P1 | Fall back to the bundled `assets/audio/catalogue.json` |
| `${CDN_BASE_URL}/v1/<channel>/notices.json` (+`.sig`) | F-PL-11 signed notices: kill-switches, text-error notices, min-version nudge (§14.3) | P1 (v1.0 floor) | Keep the last value. Each notice expires at its own `until` |
| `${CDN_BASE_URL}/v1/feeds/hijri-pk.json` (+`.sig`) | PK month-start override (D-15, format in r08 §2.7). Shared by all channels | P2 | Fall back to Umm al-Qura plus the user offset |
| mp3quran / everyayah / islamic.network | Audio bytes (D-12), per the catalogue's hosts | P1 | Mirror failover inside `ResolvingDataSource` ([06](06-audio-engine.md)) |
| Crash-report Worker (`${CDN_BASE_URL}/crash`, Cloudflare Worker → R2, 90 days) | Opt-in ACRA reports (F-PL-09, D-32). The only server-side code we run | v1.1 | Report kept on device and retried later; never blocks the app |
| QF User API, Firebase | Sync and group khatam | P3 | Feature hidden when unavailable |

`CDN_BASE_URL` is a **brand-neutral domain the owner registers in P0**, so G3 (brand) never forces a URL change. It is a `BuildConfig` field, and the placeholder until then is `https://cdn.example.invalid`. Never hotlink quran.com, quran.app or quranicaudio from a published build (r03 §1.10). Public builds never use quranicaudio, QF/QDC or Tarteel audio hosts, or archive.org rips (D-28).

---

## 2. Architectural principles (non-negotiable)

1. **Offline-first.** Reading, indexes, search, bookmarks, translations (bundled and installed packs) and prayer times never need the network. The network serves only packs, audio and signed feeds.
2. **Stable keys.** User data references content only by a Kufan `VerseKey` (`surah`, `ayah`) or by (`mushafId`, `pageIndex`), never by a content-DB rowid (D-10).
3. **The device draws and the pipeline computes.** Justification, word positions, markers and normalized search text are all precomputed offline ([05](05-mushaf-rendering-engine.md), [08](08-data-pipelines.md)).
4. **Every content item carries its licence and eligibility.** Every item has `licenceStatus` (5 values), `basis`, `eligibleTiers`, `commercialOk`, `fossOk` and `enabled`. Nothing is bundled or offered unless `ContentPolicy.allows` accepts it for the build's `CONTENT_TIER` and `MONETIZED` (§11).
5. **Zero third-party SDKs in v1.0** (D-20). The only libraries are AndroidX, Kotlin, Dagger, OkHttp, adhan2 and AboutLibraries. No analytics, ads, or crash SDKs. The play flavor adds Play In-App Review (`review-ktx`, F-PL-10) from v1.0-stretch/v1.1. It is a Google Play library, not a tracking SDK. Opt-in ACRA (F-PL-09) follows in v1.1.
6. **Pure core.** `:core:model` and `:core:domain` are Kotlin/JVM modules with no Android, Room or DI annotations. This keeps them testable in milliseconds and KMP-convertible.
7. **Clean-room** (D-18). GPL apps are read for ideas only. Apache-2.0 ports (quran-ios QuranKit, `LinePageGeometryEngine`) and MIT ports keep their headers (§5.6).

---

## 3. Module graph

### 3.1 Graph (v1.0 target; phase in brackets)

```mermaid
flowchart TB
  APP[":app [P0]<br/>Application, MainActivity, NavDisplay host,<br/>flavors, Glance widgets, DI root"]
  BP[":baselineprofile [P1]"]
  subgraph Features[":feature:* (never depend on each other)"]
    FH[":feature:home [P1]<br/>home + surah/para/manzil/ruku/sajda index + go-to"]
    FR[":feature:reader [P1]<br/>mushaf pager, translation view, ayah sheet"]
    FA[":feature:audio [P1]<br/>player, reciters, downloads/storage"]
    FS[":feature:search [P1]"]
    FB[":feature:bookmarks [P1]<br/>bookmarks, collections; P2 notes, khatam, stats"]
    FST[":feature:settings [P1]<br/>settings, onboarding, packs, credits"]
    FP[":feature:prayer [P2]<br/>times, hijri, ramadan (v1.0);<br/>adhan, health check, qibla (v1.1)"]
    FC[":feature:companion [P2]<br/>tasbeeh (v1.0-stretch), azkar (v1.1)"]
  end
  subgraph CoreUI["UI-side core"]
    DS[":core:designsystem [P0]<br/>theme, typography, components, model-bound composables"]
    NAV[":core:navigation [P0]<br/>NavKeys, NavigationState, Navigator, DeepLinkParser"]
    MR[":core:mushaf-render [P1]<br/>Canvas page engine (doc 05)"]
  end
  subgraph CorePure["Pure Kotlin (JVM)"]
    DOM[":core:domain [P0]<br/>repository + service interfaces incl. PlaybackStateHub,<br/>PlaybackController; use cases; ResolvePlayScope, PlanExpander"]
    MOD[":core:model [P0]<br/>VerseKey, MushafId, PageIndex, PlayScope, PlayRequest,<br/>PlaybackState, QuranMeta, AppError/AppResult, TextNormalizer"]
  end
  subgraph CoreImpl["Implementation core (bound via Hilt)"]
    DATA[":core:data [P0]<br/>repository impls, PackManager, ContentPolicy"]
    CON[":core:content [P0]<br/>Room3 RO: QuranContentDb + pack DB classes"]
    DB[":core:database [P0]<br/>Room3 user.db + typed DataStore UserSettings"]
    REM[":core:remote [P1]<br/>OkHttp, signed fetch (Ed25519), downloader + downloads.db,<br/>Work host (P1), UIDT host (S5)"]
    AU[":core:audio [P1]<br/>Media3 QuranPlaybackService, PlaybackStateHubImpl,<br/>PlaybackControllerImpl, MediaItemMapper (doc 06)"]
    PR[":core:prayer [P2]<br/>adhan2, Hijri, qibla, location, alarms, adhan FGS, reminders"]
  end
  TEST[":core:testing [P0]<br/>fakes, MainDispatcherRule, fixture DBs"]

  APP --> Features
  APP --> DATA & AU & PR & REM & CON & DB
  BP -.-> APP
  Features --> DOM & MOD & DS & NAV
  FR --> MR
  MR --> MOD & DS
  DS --> MOD
  NAV --> MOD
  DATA --> DOM & MOD & CON & DB & REM
  AU --> DOM & MOD & REM
  PR --> DOM & MOD
  REM --> DOM & MOD
  CON --> MOD
  DB --> MOD
  DOM --> MOD
  TEST --> DOM & MOD
```

**Why the interfaces live in `:core:domain`, not `:core:data`.** Features compile only against `:core:domain` (interfaces and use cases) and `:core:model`. A change inside a repository implementation, Room or OkHttp therefore never recompiles a feature. `:core:testing` fakes implement the same interfaces, and `:app` pulls in the implementations through Hilt. This deliberately deviates from Now in Android (NiA), where interfaces sit in `core:data`.

### 3.2 Module table

| Module | Plugin(s) | Responsibility | Public API (exported) | May depend on | Phase |
|---|---|---|---|---|---|
| `:core:model` | `criterion.jvm.library` + serialization | Value types; the pure `ContentPolicy` rule (§11.2) shared with the eligibility Gradle plugin; the audio types `PlayScope`, `PlayRequest`, `PlaybackState`, `HighlightTarget` and `AudioError` (§5.4, §6.1); the static Quran structure (`QuranMeta`, ported from QuranKit); the error model; the text normalizer | Everything in §5 and §4.3 | kotlinx-serialization, kotlinx-datetime | P0 |
| `:core:domain` | `criterion.jvm.library` | Repository and service interfaces (§6), including `PlaybackStateHub` and `PlaybackController`; use cases (`ResolvePlayScope`, `PlanExpander` and `QuranStructure` (doc 06 §7), `ComputeKhatamPlan`, `ResolveGoTo`, `NextPrayer`); `AppDispatchers`, `AppScope`; injected `kotlin.time.Clock` | Interfaces, use-case classes (plain constructors) | model, coroutines-core | P0 |
| `:core:content` | `criterion.android.library` + `criterion.room3` | `QuranContentDb` (asset copy), Room pack DB classes (`TranslationPackDb`, `TafsirPackDb`, `MushafPackDb`, `AzkarPackDb`, `CitiesDb`), DAOs; `TimingPackReader` (plain `androidx.sqlite`, no Room; §7.3) | **Internal.** Exposes only the `ContentDataSource` façade and `PackDbOpener` to `:core:data` | model | P0 (content), P1 (packs) |
| `:core:database` | android.library + room3 + serialization | `UserDb` (user.db), DAOs, migrations, exported schemas; typed `DataStore<UserSettings>` | Internal. Exposes the `UserDataSource` and `SettingsDataSource` façades | model | P0 |
| `:core:remote` | android.library + hilt + serialization + room3 | Shared `OkHttpClient`; `SignedFileFetcher` (ETag, Ed25519, sha256); `Ed25519Verifier`; `HttpDownloader` (Range, `.part`, atomic rename, ZIP); `DownloadManagerImpl` with its device-local queue `downloads.db` (DDL in [06](06-audio-engine.md) §13.3); `DownloadWorker` (P1, every API) and UIDT `AudioDownloadJobService` (API 34+, from S5, job ID 4201); `RemoteRefreshWorker` | `OkHttpClient` binding; download and fetch façades | domain, model | P1 |
| `:core:data` | android.library + hilt | All repository implementations except audio and prayer; `PackManagerImpl`; `ContentFilter` (applies the pure `ContentPolicy` of §11.2 with `BuildInfo`); `BackupSerializer` (JSON); `DailyMaintenanceWorker` | Hilt `@Binds` only | domain, model, content, database, remote | P0 |
| `:core:audio` | android.library + hilt | `QuranPlaybackService : MediaLibraryService`, `MediaItemMapper` (expanded plan → MediaItems; `PlanExpander` itself is pure, in `:core:domain`), `ResolvingDataSource` chain, `PlaybackStateHubImpl` (implements the `:core:domain` `PlaybackStateHub`), `PlaybackControllerImpl` (MediaController wrapper), sleep timer, `DataStore<PlaybackResume>` | `PlaybackStateHub` and `PlaybackController` bindings | domain, model, remote | P1 |
| `:core:prayer` | android.library + hilt | adhan2 wrapper, `HijrahHijriCalendarProvider` (java.time `HijrahDate` + offset + Maghrib rollover + PK feed; ICU `IslamicCalendar` fallback after a device check, D-27), Qibla sensor fusion, `LocationProviderImpl` (LocationManagerCompat + cities DB via `CitiesRepository`), `AlarmSchedulerImpl`, `AdhanAlarmReceiver`, `SystemEventsReceiver`, `AdhanPlaybackService`, `ReminderScheduler`, notification channels | Bindings for `PrayerTimesRepository`, `LocationProvider`, `HijriCalendarProvider`, `QiblaSensor`, `AlarmScheduler`, `ReminderScheduler` | domain, model | P2 |
| `:core:designsystem` | `criterion.android.library.compose` | Theme (light/sepia/night/AMOLED, F-RD-03), typography (Noto Nastaliq Urdu UI, D-21), icons (vector drawables, no `material-icons-extended`), components, model-bound rows (`SurahRow`, `ParaRow`, `DownloadButton`), `AppError` → string mapping | Composables, `CriterionTheme` | model | P0 |
| `:core:navigation` | android.library.compose + serialization | Every `@Serializable` NavKey (§8), `NavigationState`, `Navigator`, `DeepLinkParser` | Keys and navigator | model | P0 |
| `:core:mushaf-render` | android.library.compose | Canvas renderer, hit-testing, highlight layer ([05](05-mushaf-rendering-engine.md)) | `MushafPage(...)` composable, `HitTester` | model, designsystem | P1 |
| `:core:testing` | android.library | Fakes for every §6 interface, `MainDispatcherRule`, a fixture content DB (Fatiha + Baqarah 1–20 + p.1–3 layout), `TestDispatchers` | Test helpers | domain, model | P0 |
| `:feature:*` | `criterion.android.feature` | Screens, ViewModels, `EntryProviderScope.<feature>Entries(navigator)` | One entry-provider extension per feature | domain, model, designsystem, navigation (+ reader → mushaf-render) | P1/P2 |
| `:app` | `criterion.android.application(.compose)` + `.flavors` + hilt + aboutlibraries | Wiring, `CriterionApp` (`Configuration.Provider`), `MainActivity : AppCompatActivity`, Glance widgets (P2, ships v1.1), launcher shortcuts (v1.1), flavor modules | — | everything | P0 |
| `:baselineprofile` | `criterion.android.test` + baselineprofile | Profile generator and macrobenchmarks (§13) | — | app | P1 |

**Counts.** 9 modules in P0 (`app`, `model`, `domain`, `content`, `database`, `data`, `designsystem`, `navigation`, `testing`). 19 at P1 (adds `remote`, `mushaf-render`, `audio`, six features and `baselineprofile`). 22 at v1.0 (adds `prayer`, `feature:prayer`, `feature:companion`). That matches D-03's 15–22 target.

**Split triggers** (do not split earlier):
- `:core:remote` becomes `:core:network` + `:core:download` when it exceeds about 4k LOC.
- `:core:prayer` becomes `:core:location` + `:core:notifications` when reminders grow beyond prayer.
- `:feature:bookmarks` loses `:feature:khatam` when the planner exceeds 3 screens.

**P3 additions:** `:feature:hifz` (F-CP-12), `:core:sync` (QF PKCE, F-BM-07), `:core:groupkhatam` (play flavor Firestore, F-CP-11). P4 adds `:core:asr`. Lint rules live in the non-shipping `:lint-rules` module.

**Names used in other drafts → doc 04 names.** Other docs must use the right-hand column. Rows marked "split" become their own module only when §3.2's split trigger fires.

| Name in another doc | Doc 04 module / type |
|---|---|
| `:core:audio-api` (doc 06) | none. `PlaybackStateHub` and `PlaybackController` interfaces are in `:core:domain`; `PlaybackState`, `HighlightTarget` and `AudioError` are in `:core:model` |
| `AudioController` (doc 06) | `PlaybackController` |
| `NowPlaying` (doc 06) | `PlaybackState` (§6.1) |
| `AyahKey` (doc 06) | `VerseKey` |
| `StopAt` (doc 06) | `Until` |
| `:core:download` (doc 06) | `:core:remote` (split) |
| `:core:network` | `:core:remote` (split) |
| `:core:ui` (doc 06) | `:core:designsystem` |
| `:core:common` (doc 07, for `Clock`) | `:core:domain` (injected `kotlin.time.Clock`) |
| `:core:location`, `:core:notifications` (doc 07) | `:core:prayer` (split) |
| `:feature:qibla` (doc 07) | `:feature:prayer` |
| `:feature:adhkar` (doc 07) | `:feature:companion` |
| `:feature:khatam` (doc 07) | `:feature:bookmarks` (split) |
| `:core:hifz` (doc 07, P3) | `:core:domain` (`HifzPlanner`) + `:feature:hifz` |
| `:core:mushaf`, `:core:mushaf-ui` (doc 05) | `:core:mushaf-render`. `MushafRepositoryImpl` is in `:core:data` |
| `HijriService`, `HijriCalendar` (earlier drafts) | `HijriCalendarProvider` (`:core:domain`, D-27; impl `HijrahHijriCalendarProvider`) |
| `PageModel` (earlier doc 05 draft) | `PageLayout` (fields owned by doc 05) |
| `WordIndex` (earlier doc 04 draft) | `WordId` |

### 3.3 Feature → module map

| Feature IDs | Module(s) |
|---|---|
| F-RD-01..08, F-RD-12, F-RD-13 | `:feature:reader`, `:core:mushaf-render`, `:core:data` (Mushaf/Content/Translation repos) |
| F-RD-09 | `:feature:reader` + `:core:mushaf-render` |
| F-RD-10/11 | `:core:mushaf-render` (P3) |
| F-IX-01..09 | `:feature:home` (index tabs, go-to, popular, continue reading) |
| F-BM-01 | `:core:data` `ReadingProgressRepository` (written by reader) |
| F-BM-02..05 | `:feature:bookmarks` |
| F-BM-06 | `:core:data` `BackupSerializer` + `:feature:settings` (SAF UI, P2) |
| F-AU-01..10 | `:core:audio`, `:core:remote` (downloads), `:feature:audio` |
| F-AU-11/12 | `:core:audio` (P3) |
| F-TR-01..03 | `:core:data` `TranslationRepository`, `PackManager`; `:feature:settings` (pack manager UI) |
| F-TR-04/05 | `:core:data` + packs (P3) |
| F-SR-01..03 | `:feature:search`, `:core:data` `SearchRepository` |
| F-SR-04/05 | `:feature:search`, `:core:data` (P3) |
| F-CP-01..04, F-CP-07 (times) | `:core:prayer`, `:feature:prayer` |
| F-CP-05, F-CP-06, F-CP-07 (duas, plan) | `:feature:companion` (khatam plan logic in `:core:domain`) |
| F-CP-08 | `:app` (Glance) |
| F-CP-09 | `:core:prayer` `ReminderScheduler` |
| F-CP-10 | `:feature:reader` (share sheet) + `:core:data` (`shareText`) |
| F-CP-11 | Text sheet: `:feature:bookmarks` (khatam screens). Realtime: `:core:groupkhatam` (play, P3) |
| F-CP-12..14 | `:feature:hifz` + `:core:domain` (P3); `:core:asr` (P4) |
| F-PL-01..07 | `:feature:settings` (onboarding, settings, credits, report error, packs/storage), `:core:designsystem` (a11y) |
| F-PL-08 | `:app` `src/play/` (Play Billing, v1.5) |
| F-PL-09 | `:app` ACRA, opt-in, posting to the crash-report Worker (v1.1, D-32) |
| F-PL-10 | `:app` `src/play/` `InAppReview` (review-ktx); foss = no-op |
| F-PL-11 | `:core:remote` `SignedFileFetcher` + `PackManager.notices()`; Home card in `:feature:home`, info chip in `:feature:reader` chrome (never over Quran text) |
| F-PL-12 | `:app` `res/xml/shortcuts.xml` → §8.3 deep links (`criterion://resume`, `/play?scope=resume`, `/qibla`, `/tasbeeh`) |

### 3.4 Dependency rules (enforced by the `criterion.root` check task in CI)

1. A `:feature:*` module must not depend on another `:feature:*`, on `:core:data`, `:core:content`, `:core:database`, `:core:remote`, `:core:audio` or `:core:prayer`, or on Room, OkHttp or Media3 artifacts. Exception: `:feature:audio` may use `media3-ui-compose-material3`, which only needs a `Player` handed out by `PlaybackController.playerForUi()`.
2. `:core:model` and `:core:domain` may not apply any Android plugin and may not depend on `javax.inject`, Dagger or Room.
3. Implementation classes are `internal`. Each implementation module exposes only Hilt modules and the façades listed above. This fixes g04 error #2: Room types never leak through `api(...)`.
4. Cycles are a build failure (Gradle enforces this). The `criterion.root` task also prints a dependency-graph diff in PRs (with CI, E-02-S01).
5. Every module with a build file applies a base convention plugin (`criterion.android.application`, `criterion.android.library` or `criterion.jvm.library`; `criterion.android.test` joins with `:baselineprofile`, E-02-S05), so its tests and lint reach `quickCheck`/`prCheck`.

`checkModuleBoundaries` enforces rules 1, 2, 3 (Room declared in an `api` configuration) and 5 on each module's **declared** dependencies and applied plugins; a dependency that arrives only transitively is not seen. "Android plugin" means any plugin that adds the `android` extension (the JVM-only `com.android.lint` is allowed).

---

## 4. Layering, UDF and threading

### 4.1 Layers

```
Compose screen (stateless)  ←  Route (collects state, owns callbacks)  ←  ViewModel (StateFlow<UiState>)
        ↓ onAction(UiAction)                                                  ↓
                                         use case (:core:domain, optional, pure)
                                                     ↓
                               repository interface (:core:domain)  ⇐ impl in :core:data / :core:audio / :core:prayer
                                                     ↓
                         data sources: Room (content, packs, user) · DataStore · files · OkHttp · Media3 controller · AlarmManager
```

### 4.2 Screen contract (every screen)

- Each screen has **one immutable `XxxUiState`**, either a data class or a sealed `Loading | Ready | Error`. Lists inside it are `kotlinx.collections.immutable.ImmutableList`.
- **`XxxAction`** is a sealed interface of user intents. Its single entry point is `fun onAction(action: XxxAction)`. "UiEvent" in older docs means the same thing; we name it `Action`.
- **One-off effects.** A message is a state field such as `userMessage: UserMessage?`, cleared by `onAction(MessageShown)`. Navigation is never held by the ViewModel: the Route receives lambdas (`onOpenReader: (ReaderKey) -> Unit`) from the feature's `entryProvider` and calls them. Never use `SharedFlow` or `Channel` for effects.
- **State production.** Use `combine(...)…stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)`. Expensive one-shot loads go through `flow { emit(repo.x()) }` inside the same `combine`, so they cancel with the subscriber.
- **Saved state.** NavKeys carry the arguments. `SavedStateHandle` is used only for transient UI input (a search query being typed).

```kotlin
@HiltViewModel(assistedFactory = SurahIndexViewModel.Factory::class)
class SurahIndexViewModel @AssistedInject constructor(
    @Assisted private val key: IndexKey,
    index: IndexRepository,
    progress: ReadingProgressRepository,
    private val playback: PlaybackController,
) : ViewModel() {
    private val tab = MutableStateFlow(key.tab)
    private val message = MutableStateFlow<UserMessage?>(null)

    private val lists = combine(index.surahs(), index.paras(), index.manzils()) { s, p, m -> Triple(s, p, m) }

    val uiState: StateFlow<IndexUiState> =
        combine(tab, lists, progress.lastRead(), message) { t, (s, p, m), last, msg ->
            IndexUiState.Ready(t, s.toImmutableList(), p.toImmutableList(), m.toImmutableList(), last, msg) as IndexUiState
        }
        .catch { emit(IndexUiState.Error(it.toAppError())) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), IndexUiState.Loading)

    fun onAction(a: IndexAction) {
        when (a) {
            is IndexAction.SelectTab -> tab.value = a.tab
            is IndexAction.Play -> viewModelScope.launch {
                (playback.play(PlayRequest(a.scope)) as? AppResult.Err)?.let { message.value = UserMessage.Error(it.error) }
            }
            IndexAction.MessageShown -> message.value = null
        }
    }
    @AssistedFactory interface Factory { fun create(key: IndexKey): SurahIndexViewModel }
}
```

The Route is a stateless screen plus a `@Preview`/Roborazzi target:

```kotlin
@Composable
fun IndexRoute(key: IndexKey, onOpenReader: (ReaderKey) -> Unit,
               vm: SurahIndexViewModel = hiltViewModel<SurahIndexViewModel, SurahIndexViewModel.Factory> { it.create(key) }) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    IndexScreen(state, vm::onAction, onOpenReader)
}
```

If assisted injection with `hiltViewModel(creationCallback)` fails on Nav3 in P0, fall back to `SavedStateHandle.toRoute`-style key passing via `navArgs` decorator; this is P0 spike item S-3 (§17).

### 4.3 Error model and result type (in `:core:model`)

```kotlin
sealed interface AppError {
    data object Offline : AppError
    data class Network(val httpCode: Int?, val host: String?) : AppError
    data class NoSpace(val neededBytes: Long, val availableBytes: Long) : AppError
    data class Integrity(val what: String) : AppError                  // sha256/signature/schema mismatch
    data class NotFound(val what: String) : AppError
    data class PermissionDenied(val permission: String) : AppError
    data class Unavailable(val reason: UnavailableReason) : AppError     // kill-switch, not eligible, not installed
    data class Unknown(val cause: Throwable) : AppError
}
enum class UnavailableReason { KILL_SWITCHED, NOT_ELIGIBLE_FOR_BUILD, NOT_INSTALLED, NEEDS_APP_UPDATE, LICENCE_PENDING }

sealed interface AppResult<out T> {
    data class Ok<out T>(val value: T) : AppResult<T>
    data class Err(val error: AppError) : AppResult<Nothing>
}
inline fun <T, R> AppResult<T>.map(f: (T) -> R): AppResult<R> = when (this) { is AppResult.Ok -> AppResult.Ok(f(value)); is AppResult.Err -> this }
suspend inline fun <T> appCatching(crossinline block: suspend () -> T): AppResult<T> =
    try { AppResult.Ok(block()) }
    catch (c: kotlinx.coroutines.CancellationException) { throw c }          // never swallow cancellation
    catch (t: Throwable) { AppResult.Err(t.toAppError()) }
fun Throwable.toAppError(): AppError                                         // maps UnknownHost/SocketTimeout→Offline, IOException(ENOSPC)→NoSpace, …
```

**Identity rules.** Public APIs take the value types above (`SurahId`, `ParaId`, `ManzilId`, `RukuId`, `VerseKey`, `MushafId`, `PageIndex`, `RecitationId`, `WordId`), never raw `Int` ids. Raw ints appear only inside entities and DAOs.

**Rules**
- One-shot repository calls that can fail for **environmental** reasons (network, disk, permission, licence) return `AppResult<T>`.
- Reads of bundled content return plain values. A failure there is a bug, and it throws.
- Observable data is a `Flow<T>`. Errors inside a Flow reach the UI through `.catch { }` and become `UiState.Error`.
- `:core:designsystem` maps every `AppError` to a localized en/ur message and a recovery action (Retry, Free space, Open settings).

### 4.4 Coroutines, dispatchers and threading

```kotlin
// :core:domain — injected as a type, so no qualifier annotations are needed in pure modules
data class AppDispatchers(val io: CoroutineDispatcher, val default: CoroutineDispatcher, val main: CoroutineDispatcher)
class AppScope(val scope: CoroutineScope)   // SupervisorJob() + dispatchers.default; process lifetime
```

| Work | Dispatcher / thread | Rule |
|---|---|---|
| Room (content, packs, user) | `setQueryCoroutineContext(dispatchers.io)` | DAOs are `suspend` or `Flow` only (Room 3) |
| DataStore | its own scope on `io` | One `DataStore<UserSettings>` instance per process |
| Page assembly (pack rows plus words into `PageLayout`) | `default` | Off the main thread, cached (§13) |
| Canvas drawing | main/RenderThread | No allocation and no DB access in `draw` |
| Media3 `MediaController`, session, player | **application main looper** | Media3 1.11 rule: getters throw off the main looper (r07 §2.6) |
| FTS search | `io`, debounced 250 ms, `flatMapLatest` | Cancel on new input. `LIMIT 200` per page |
| Downloads | `io.limitedParallelism(3)` overall, 2 per host | Inside the worker (P1) or UIDT job (S5+) only |
| Signature and sha256 checks | `default` | Streaming hash. Never on main |
| Sensors (Qibla) | sensor callback looper, then `conflate()` | Low-pass filter in `default` |
| Alarm receivers | `goAsync()` + `AppScope` | Finish in under 5 s; `rescheduleAll` is idempotent |

StrictMode is set in debug `Application.onCreate` (thread and VM policies, `penaltyLog`, plus `penaltyDeath` on CI instrumented runs). Tests use `MainDispatcherRule` with `runTest`, never `runBlocking` around `viewModelScope` (g04 §2.3 #5).

---

## 5. Domain model (`:core:model`)

### 5.1 Identifiers and keys

```kotlin
@Serializable @JvmInline value class SurahId(val value: Int) { init { require(value in 1..114) } }
@Serializable @JvmInline value class ParaId(val value: Int) { init { require(value in 1..30) } }    // Indo-Pak Para (D-07)
@Serializable @JvmInline value class ManzilId(val value: Int) { init { require(value in 1..7) } }
@Serializable @JvmInline value class RukuId(val value: Int) { init { require(value in 1..558) } }  // global ruku number
@Serializable @JvmInline value class AyahId(val value: Int) { init { require(value in 1..6236) } } // Kufan global index; 1 = 1:1
@Serializable @JvmInline value class WordId(val value: Int) { init { require(value in 1..83_669) } } // DK token id (D-06) = content word_index = pack word.id
@Serializable @JvmInline value class RecitationId(val value: String)   // doc 06 format, e.g. "alafasy.murattal.mp3q123"

/** Kufan verse key. Canonical key for user data, audio, translations, search, sync ("2:255"). There is no `AyahKey`. */
@Serializable
data class VerseKey(val surah: Int, val ayah: Int) : Comparable<VerseKey> {
    init { require(surah in 1..114 && ayah in 1..QuranMeta.ayahCount(surah)) { "bad key $surah:$ayah" } }
    val ayahId: AyahId get() = AyahId(QuranMeta.surahFirstAyahId(surah) + ayah - 1)
    fun id(): Int = ayahId.value                                       // Kufan global index 1..6236
    val surahId: SurahId get() = SurahId(surah)
    override fun compareTo(other: VerseKey) = compareValuesBy(this, other, VerseKey::surah, VerseKey::ayah)
    override fun toString() = "$surah:$ayah"
    companion object {
        fun parse(s: String): VerseKey? = s.split(':').takeIf { it.size == 2 }
            ?.let { (a, b) -> runCatching { VerseKey(a.trim().toInt(), b.trim().toInt()) }.getOrNull() }
        fun of(id: AyahId): VerseKey = QuranMeta.verseKeyOf(id)
    }
}

@Serializable
data class AyahRange(val from: VerseKey, val to: VerseKey) {
    init { require(from <= to) }
    operator fun contains(k: VerseKey) = k in from..to
    val ayahCount: Int get() = to.ayahId.value - from.ayahId.value + 1
    fun surahsTouched(): IntRange = from.surah..to.surah
}
```

### 5.2 Mushaf identity and pages

```kotlin
/** Serialized (NavKeys, settings, JSON export) as `code` via @SerialName. user.db stores `code` (TEXT);
 *  mushaf packs store the numeric `id` (doc 05 `pack_info`/`mushaf`) plus `mushaf_code`. */
@Serializable
enum class MushafId(val code: String, val id: Int) {
    @SerialName("indopak16_taj")    INDOPAK_16_TAJ("indopak16_taj", 1),       // default, 548 pages (D-04)
    @SerialName("indopak15_hafizi") INDOPAK_15_HAFIZI("indopak15_hafizi", 2), // P3, 610 pages
    @SerialName("indopak13_taj")    INDOPAK_13_TAJ("indopak13_taj", 3),       // P3, 847 pages
    @SerialName("madani15")         MADANI_15("madani15", 4);                 // P4, 604 pages
    companion object {
        fun ofCode(code: String): MushafId? = entries.firstOrNull { it.code == code }
        fun ofId(id: Int): MushafId? = entries.firstOrNull { it.id == id }
    }
}
/** Layout pack id = "mushaf.<code>.<script>", e.g. "mushaf.indopak16_taj.dk" (doc 05). */
fun MushafId.packId(script: String = "dk") = "mushaf.$code.$script"
/** 1-based storage index of a page in a layout (QUL index for Taj-16: 1..548). Never shown in UI. */
@Serializable @JvmInline value class PageIndex(val value: Int)
/** Printed page number shown everywhere in UI (Taj-16: index + 1 → 2..549). Never stored. */
@JvmInline value class PageLabel(val value: Int)

data class MushafSpec(
    val id: MushafId, val pageCount: Int, val linesPerPage: Int,
    val pageLabelOffset: Int,                 // Taj-16 = 1
    val packVersion: Int, val displayName: LocalizedText,
) {
    fun labelOf(p: PageIndex) = PageLabel(p.value + pageLabelOffset)
    fun indexOf(l: PageLabel): PageIndex? = PageIndex(l.value - pageLabelOffset).takeIf { it.value in 1..pageCount } // null → UI shows "2–549"
}
data class ReadingPosition(val mushaf: MushafId, val page: PageIndex, val anchor: VerseKey, val mode: ReaderMode, val at: Instant)
enum class ReaderMode { MUSHAF, TRANSLATION }
```

`PageLayout`, `PageLine`, `PlacedWord`, `PageFurniture`, `MarginMarker` and `MushafGeometry` are defined in `:core:model` with the fields from [05](05-mushaf-rendering-engine.md) §3.4. Doc 05 owns that field list. This doc only requires that `PageLayout` carries `mushaf: MushafId`, `page: PageIndex` and `verseRange: AyahRange`, plus the page's Para and Manzil. `labelOf`/`indexOf` above are the **only** place that converts between index and label.

**Para boundaries in the Taj-16 layout.** Para 1 starts at page index 1 (label 2). For n = 2…28, Para n starts at page index 18n − 16; the formula does not hold for n = 1 (g01 §2.8). Code never relies on the formula: start pages come from the pack's `ayah_page` table via `MushafRepository.startPage`.

### 5.3 Structure types

```kotlin
data class Surah(val id: SurahId, val nameAr: String, val nameUr: String, val nameRoman: String, val nameEn: String,
                 val meaningUr: String, val meaningEn: String, val revelation: Revelation, val revelationOrder: Int,
                 val ayahCount: Int, val rukuCount: Int, val basmala: BasmalaKind)
enum class Revelation { MAKKI, MADANI }
enum class BasmalaKind { IS_VERSE_1 /*1:1*/, NONE /*surah 9*/, HEADER /*all others: not a verse*/ }
data class Para(val id: ParaId, val nameAr: String, val nameRoman: String, val range: AyahRange, val madaniStart: VerseKey)
data class Manzil(val id: ManzilId, val range: AyahRange)
data class Ruku(val id: RukuId, val surah: SurahId, val rukuInSurah: Int, val para: ParaId, val rukuInPara: Int?, // null = Fatiha (D-07)
                val range: AyahRange)
data class Sajda(val key: VerseKey, val number: Int?, val kind: SajdaKind)   // 14 numbered HANAFI + 22:77 SHAFII_ONLY
enum class SajdaKind { HANAFI, SHAFII_ONLY }
data class ParaQuarter(val para: ParaId, val quarter: Quarter, val at: VerseKey, val afterWordPos: Int?)
enum class Quarter { RUB /*¼ ربع*/, NISF /*½ نصف*/, THULUTH /*¾ ثلٰثة*/ }
data class Passage(val code: String, val title: LocalizedText, val ranges: List<AyahRange>)  // Yaseen, Manzil dua, …
data class LocalizedText(val en: String, val ur: String, val ar: String? = null) { fun pick(lang: UiLanguage) = if (lang == UiLanguage.UR) ur else en }
```

### 5.4 Audio-facing domain types (`:core:model`; behaviour in [06](06-audio-engine.md))

These signatures are the shared contract. Docs 02, 05, 06, 07, 10 and 13 use them verbatim.

```kotlin
// RecitationId is declared in §5.1. Its string format is owned by doc 06 (e.g. "alafasy.murattal.mp3q123",
// "alafasy.murattal.ea", "huthaify.murattal.kfgqpc"); ids are permanent once published.

@Serializable
enum class Until { END_OF_SURAH, END_OF_PARA, CONTINUOUS }   // CONTINUOUS = "play from here" runs until stopped

@Serializable
sealed interface PlayScope {
    @Serializable data class Surah(val surah: SurahId) : PlayScope
    @Serializable data class Para(val para: ParaId) : PlayScope            // Indo-Pak boundaries (D-07, D-13)
    @Serializable data class Manzil(val manzil: ManzilId) : PlayScope
    @Serializable data class Ruku(val ruku: RukuId) : PlayScope
    @Serializable data class Page(val mushaf: MushafId, val page: PageIndex) : PlayScope  // from the first ayah STARTING on the page
    @Serializable data class Range(val range: AyahRange) : PlayScope
    @Serializable data class Ayah(val key: VerseKey) : PlayScope
    @Serializable data class FromHere(val start: VerseKey, val until: Until = Until.CONTINUOUS) : PlayScope
    /** Multi-range playlist, e.g. the Manzil dua (18 ranges / 79 ayahs). `titleRes` names the string resource
     *  used for the notification/now-playing title (e.g. "passage_manzil_dua"). Built from content-DB `passage_range`. */
    @Serializable data class Passage(val ranges: List<AyahRange>, val titleRes: String) : PlayScope
    /** The whole mushaf 1:1 → 114:6. The plan is windowed by surah (doc 06). */
    @Serializable data object Quran : PlayScope
}
@Serializable
data class RepeatSpec(val ayahTimes: Int = 1 /*1..20*/, val rangeTimes: Int = 1 /*1..20, 0 = ∞*/,
                      val pauseAfterAyah: PauseRule = PauseRule.None, val cumulative: Boolean = false) {
    init { require(ayahTimes in 1..20 && rangeTimes in 0..20) }
}
@Serializable sealed interface PauseRule {
    @Serializable data object None : PauseRule
    @Serializable data class Fixed(val ms: Long) : PauseRule
    @Serializable data class Proportional(val factor: Float) : PauseRule
}
@Serializable
data class PlayRequest(
    val scope: PlayScope,
    val recitation: RecitationId? = null,          // null = favourite reciter
    val repeat: RepeatSpec = RepeatSpec(),
    val speed: Float = 1f,                         // 0.5..2.0; defaults from settings.audio.speed
    val translationVoice: RecitationId? = null,    // F-AU-10 interleave
    val startAt: VerseKey? = null,                 // resume / start inside the scope (must lie inside it)
    val startOffsetMs: Long = 0,                   // resume offset into the startAt ayah
)
```

`:core:domain` contains `ResolvePlayScope(index, mushaf): suspend (PlayScope) -> List<AyahRange>`. It is pure and unit-tested (for example, "Para 4 = [3:92–4:23]", "Passage(manzil dua) = 18 ranges, 79 ayahs", "Page(taj16, p) starts at the first ayah that starts on p"). `Quran` and `FromHere(CONTINUOUS)` resolve to `[1:1–114:6]` and `[start–114:6]`; doc 06's `PlanExpander` (pure, in `:core:domain`) windows them by surah and starts the window at `startAt` when set. `:core:audio`'s `MediaItemMapper` turns the expanded plan into Media3 items. Deep links and reminders name passages by code (`passage:manzil-dua`). `DeepLinkParser` resolves the code through `IndexRepository.passages()` into `PlayScope.Passage(ranges, titleRes)`.

### 5.5 `QuranMeta` (static structure, pure Kotlin)

`QuranMeta` holds compile-time tables so that keys, ranges and Para/Manzil lookups work without a database. Every function is O(log n) or O(1).
- `ayahCounts: IntArray(114)` and the derived `surahFirstAyahId`.
- `paraStarts: Array<VerseKey>(30)`: the **Indo-Pak** table from D-07 (P4 = 3:92, P7 = 5:83, P11 = 9:94, P14 = 15:2, P20 = 27:60, P21 = 29:45, P23 = 36:22).
- `manzilStartSurahs = intArrayOf(1, 5, 10, 17, 26, 37, 50)`.
- Functions: `paraOf(key)`, `manzilOf(key)`, `verseKeyOf(AyahId)`, `next(key)`, `prev(key)`, `lastAyah(surah)`.

A JVM test asserts that `QuranMeta` equals the content DB's `surah`, `para` and `manzil` tables exactly. This guards against drift between the two copies. Ruku, sajda, quarter and page data stay in the DB only.

### 5.6 Ported code headers (D-18)

QuranKit-derived files (`QuranMeta`, `AyahRange` helpers, line-page geometry in `:core:mushaf-render`) start with this header:

```kotlin
/*
 * Portions ported from quran/quran-ios (QuranEngine) — https://github.com/quran/quran-ios
 * Copyright (c) Quran.com contributors. Licensed under the Apache License, Version 2.0.
 * Modifications (c) 2026 Criterion authors: rewritten in Kotlin; Indo-Pak Para/Manzil/Ruku added.
 */
```

The Apache-2.0 text is listed in the Credits/licences screen (F-PL-04) through AboutLibraries custom entries.

### 5.7 Text normalizer

`TextNormalizer.arabic(s)`, `.urdu(s)`, `.english(s)` and `.stripForCopy(s)` implement the r05 §2.7.5 spec:
- strip PUA and control characters (`stripForCopy`, D-08);
- strip harakat and Quranic marks;
- fold letter forms; Urdu keeps ے and ھ distinct;
- English is ASCII-folded and lowercased.

A Kotlin test consumes `normalization_vectors.json` generated by the Python pipeline ([08](08-data-pipelines.md)), so the two implementations cannot diverge. This replaces the spike's minimal `ArabicNormalizer`.

---

## 6. Key interfaces (`:core:domain`)

All interfaces are main-safe: implementations switch dispatchers internally. Flows are cold and re-emit when the underlying table changes.

```kotlin
interface ContentRepository {
    fun surahs(): Flow<List<Surah>>
    suspend fun surah(id: SurahId): Surah
    suspend fun ayah(key: VerseKey): AyahText                         // uthmani + imlaei (Tanzil, verbatim)
    suspend fun ayahs(range: AyahRange): List<AyahText>
    suspend fun words(range: AyahRange): List<Word>                   // DK tokens of the range, WordId order
    suspend fun ayahMarkers(range: AyahRange): List<AyahMarker>       // Kufan display numbers incl. 118 disputed marks;
                                                                      // layout overrides come from pack `ayah_display_override`
    suspend fun resolveDisplayNumber(surah: SurahId, displayNumber: Int): VerseKey? // "Fatiha 6" → 1:7
    suspend fun shareText(range: AyahRange, format: ShareFormat): String // sanitized, with attribution line (F-CP-10)
    suspend fun contentInfo(): ContentInfo                            // content_version, built_at, sources hash
    fun attributions(): Flow<List<Attribution>>                       // content + installed packs (F-PL-04), grouped by section
    suspend fun dailyAyah(date: LocalDate): AyahRange?                // v1.1 (F-CP-08/09); null while daily_ayah is empty
}

interface MushafRepository {
    fun available(): Flow<List<MushafSpec>>                           // installed layout packs, eligibility-filtered
    suspend fun spec(id: MushafId): MushafSpec
    suspend fun page(id: MushafId, page: PageIndex): PageLayout        // cached LRU; no runtime text overlay (§14.2)
    suspend fun pageOf(id: MushafId, key: VerseKey): PageIndex
    suspend fun verseRange(id: MushafId, page: PageIndex): AyahRange
    suspend fun startPage(id: MushafId, target: IndexTarget): PageIndex  // surah/para/manzil/ruku → page (from pack ayah_page)
    suspend fun ayahSpan(id: MushafId, key: VerseKey): AyahPageSpan   // ayah_page row: pages/lines of the ayah (follow-along, khatam pos)
    suspend fun geometry(id: MushafId): MushafGeometry                // pack `mushaf` row
    suspend fun headerPage(id: MushafId, surah: SurahId): PageIndex   // page of the surah's header/basmala line
    fun prefetch(id: MushafId, center: PageIndex, radius: Int = 3)
}

interface IndexRepository {
    fun surahs(): Flow<List<SurahSummary>>                            // + start page in current mushaf + progress
    fun paras(): Flow<List<ParaSummary>>                              // names, start page, khatam/read progress
    fun manzils(): Flow<List<ManzilSummary>>
    suspend fun rukus(surah: SurahId? = null, para: ParaId? = null): List<Ruku>
    suspend fun sajdas(): List<Sajda>
    suspend fun paraQuarters(para: ParaId? = null): List<ParaQuarter>
    suspend fun passages(): List<Passage>                             // quick access (F-IX-08)
    suspend fun paraOf(key: VerseKey): Para
    suspend fun rukuOf(key: VerseKey): Ruku
    suspend fun resolveGoTo(input: String, mushaf: MushafId): GoToTarget?  // "50", "2:255", "yaseen", "para 23"
}

interface TranslationRepository {
    fun installed(): Flow<List<TranslationInfo>>                      // bundled + downloaded, eligibility-filtered
    fun selected(): Flow<List<TranslationId>>                         // from settings (1 per language, F-TR-03)
    fun translations(range: AyahRange, ids: List<TranslationId>): Flow<Map<VerseKey, List<TranslationText>>>
    suspend fun footnotes(id: TranslationId, key: VerseKey): List<Footnote>
    suspend fun tafsirSummary(key: VerseKey): TafsirText?             // Ahsan-ul-Bayan = Junagarhi footnotes (F-RD-13)
}

interface SearchRepository {
    fun search(query: SearchQuery): Flow<SearchPage>                  // Arabic + selected translations + aliases
    suspend fun aliases(prefix: String, limit: Int = 8): List<AliasHit>
    fun history(): Flow<List<String>>; suspend fun clearHistory()
}
data class SearchQuery(val text: String, val scopes: Set<SearchScope>, val mode: MatchMode = MatchMode.WORDS, val page: Int = 0)
enum class SearchScope { ARABIC, TRANSLATION_UR, TRANSLATION_EN, SURAH_NAMES }
enum class MatchMode { WORDS /*unicode61, prefix on last token*/, PARTIAL /*trigram*/ }

interface BookmarkRepository {
    fun bookmarks(collection: Long? = null): Flow<List<Bookmark>>
    fun collections(): Flow<List<BookmarkCollection>>
    fun bookmarkedKeys(range: AyahRange): Flow<Set<VerseKey>>         // reader overlay
    suspend fun add(b: NewBookmark): Long
    suspend fun remove(id: Long): UndoToken                           // soft delete
    suspend fun undo(token: UndoToken)
    suspend fun update(id: Long, label: String?, color: Int?, collection: Long?)
    suspend fun upsertCollection(c: BookmarkCollection): Long; suspend fun deleteCollection(id: Long)
}
interface NoteRepository {                                            // P2 (F-BM-03)
    fun notes(): Flow<List<Note>>; fun notesFor(range: AyahRange): Flow<List<Note>>
    suspend fun save(note: Note): Long; suspend fun delete(id: Long): UndoToken
}

interface ReadingProgressRepository {
    fun lastRead(mushaf: MushafId? = null): Flow<ReadingPosition?>    // null = current mushaf (F-BM-01)
    /** Only caller: `LastReadTracker` in :feature:reader. It calls this once a settled page has been visible
     *  for ≥ 4 s (F-BM-01), or on ayah interaction or audio on that page. The reader never writes user.db directly. */
    suspend fun setLastRead(pos: ReadingPosition)
    fun recents(limit: Int = 10): Flow<List<RecentItem>>              // F-IX-09
    suspend fun recordOpen(item: RecentItem)
    suspend fun recordDwell(mushaf: MushafId, page: PageIndex, dwellMs: Long) // page_dwell (per Quran-day) → stats, khatam suggest
    fun readingDays(from: LocalDate, to: LocalDate): Flow<List<ReadingDay>>  // F-BM-05 (P2), reading_day
}

interface KhatamRepository {                                          // P2 (F-BM-04, F-CP-07 Taraweeh preset); algorithm doc 07 §14
    fun plans(): Flow<List<KhatamPlan>>; fun plan(id: Long): Flow<KhatamPlanWithProgress?>
    suspend fun create(plan: NewKhatamPlan): Long                     // writes khatam_plan + generated khatam_progress portions
    suspend fun markDone(planId: Long, seq: Int, done: Boolean, via: PortionVia = PortionVia.MANUAL)
    suspend fun replan(planId: Long, from: LocalDate)                 // re-date after a missed day or a Ramadan-start shift
    suspend fun archive(planId: Long)
}
interface TasbeehRepository {                                         // F-CP-05 (v1.0-stretch); doc 07 §8
    fun presets(): Flow<List<TasbeehPreset>>                          // seeded built-ins + user presets
    fun session(presetId: Long): Flow<TasbeehSession?>
    suspend fun increment(presetId: Long, by: Int = 1); suspend fun reset(presetId: Long)
    suspend fun upsertPreset(p: TasbeehPreset): Long
    fun dailyTotals(from: LocalDate, to: LocalDate): Flow<List<TasbeehDayTotal>>
}
interface AzkarRepository {                                           // F-CP-06 (v1.1; Manzil-dua part v1.0-stretch)
    fun categories(): Flow<List<DhikrCategory>>                       // from the installed `azkar` pack (§7.3)
    fun items(category: Int, lang: UiLanguage): Flow<List<DhikrItem>>
    fun progress(day: LocalDate): Flow<Map<Int, Int>>                 // dhikrId → count (azkar_progress)
    suspend fun increment(day: LocalDate, category: Int, dhikr: Int)
}

interface AudioCatalogRepository {                                    // schema: doc 06
    fun recitations(): Flow<List<Recitation>>                         // eligibility + kill-switch filtered
    suspend fun recitation(id: RecitationId): Recitation?
    fun favourite(): Flow<RecitationId>
    suspend fun timing(id: RecitationId, surah: SurahId): AyahTimings?  // null ⇒ no highlight for that surah
    suspend fun highlightEnabled(id: RecitationId, surah: SurahId): Boolean
    suspend fun refresh(force: Boolean = false): AppResult<Boolean>   // via SignedFileFetcher (doc 06 §3.1)
    fun fallbackFor(rec: Recitation): Recitation?                     // e.g. kill-switched timing → fallback reciter prompt
}

interface PlaybackController {                                        // impl :core:audio (MediaController wrapper)
    val state: StateFlow<PlaybackState>                               // == PlaybackStateHub.state (same instance)
    suspend fun play(request: PlayRequest): AppResult<Unit>           // user-initiated only (Android 17)
    fun pause(); fun resume(); fun stop()
    fun seekTo(key: VerseKey); fun nextAyah(); fun previousAyah()
    fun setSpeed(speed: Float); fun setRepeat(spec: RepeatSpec)
    fun setTranslationVoice(voice: RecitationId?)                     // F-AU-10 interleave; null = off
    fun setSleepTimer(timer: SleepTimer?)                             // minutes or end of ayah/surah/para/plan
    fun playerForUi(): Flow<Any?>                                     // androidx.media3.common.Player, typed Any in domain
}
```

#### 6.1 Playback state (one shape for reader, player UI, widgets, hifz and stats)

`PlaybackStateHub` is the single source of playback state. The interface lives in `:core:domain`, the types in `:core:model`, and `PlaybackStateHubImpl` (a Hilt `@Singleton`) in `:core:audio`. Field semantics and emission rules are in [06](06-audio-engine.md) §9.

```kotlin
interface PlaybackStateHub {
    val state: StateFlow<PlaybackState>
    val events: SharedFlow<PlaybackEvent>
}

data class PlaybackState(
    val status: Status = Status.IDLE,
    val planId: Long? = null,                 // changes on every new plan
    val request: PlayRequest? = null,
    val recitation: RecitationId? = null,
    val segment: Segment? = null,
    val highlight: HighlightTarget? = null,   // what the reader paints; kept while paused; null = nothing
    val highlightAvailable: Boolean = false,  // false → reader shows "highlighting off for this surah", no auto-turn
    val ayahStartMs: Long? = null,            // media time of the current ayah in the current source; null when untimed
    val ayahEndMs: Long? = null,
    val positionMs: Long = 0,                 // sampled at emission; the hub never emits on position ticks
    val durationMs: Long = 0,
    val ayahRep: Int = 0, val ayahRepOf: Int = 0,
    val rangePass: Int = 0, val rangeOf: Int = 0,   // rangeOf 0 = ∞
    val speed: Float = 1f,
    val sleep: SleepTimerState? = null,
    val error: AudioError? = null,
    val seq: Long = 0,                        // increments on every emission
) {
    enum class Status { IDLE, PREPARING, BUFFERING, PLAYING, PAUSED, ENDED, ERROR }
    val isPlaying: Boolean get() = status == Status.PLAYING
}
sealed interface Segment {
    data class Intro(val surah: SurahId) : Segment
    data class Basmala(val surah: SurahId) : Segment
    data class Ayah(val key: VerseKey, val rep: Int, val repOf: Int) : Segment
    data class Translation(val key: VerseKey) : Segment
    data class SurahOnly(val surah: SurahId) : Segment
}
sealed interface HighlightTarget {
    data class Ayah(val key: VerseKey) : HighlightTarget               // also while its translation item plays
    data class BasmalaLine(val surah: SurahId) : HighlightTarget
    data class Word(val key: VerseKey, val wordId: WordId) : HighlightTarget   // v1.x (F-AU-12)
}
sealed interface PlaybackEvent {
    data class AyahCompleted(val key: VerseKey, val recitation: RecitationId, val atEpochMs: Long) : PlaybackEvent
    data object PlanEnded : PlaybackEvent
    data class Error(val error: AudioError) : PlaybackEvent
}
// SleepTimer (request), SleepTimerState and the AudioError cases are defined by doc 06; they live in :core:model.
// :core:designsystem maps AudioError to a user message the same way as AppError.
```

Consumers: the reader paints `highlight` and owns auto page-turn, using `ayahStartMs`/`ayahEndMs`/`positionMs`/`speed`/`isPlaying` (doc 05). The player UI reads the repeat and sleep fields. Doc 07 uses `AyahCompleted` for stats and hifz. The listening pointer (Home "Continue listening" card, `onPlaybackResumption`) is doc 06's `DataStore<PlaybackResume>`, not a user.db table.

#### 6.2 Downloads, packs, settings and companion interfaces

```kotlin
interface DownloadManager {                                           // impl :core:remote; NOT android.app.DownloadManager
    suspend fun enqueue(request: DownloadRequest): AppResult<DownloadId>  // must be called while UI visible (UIDT rule)
    suspend fun cancel(id: DownloadId); suspend fun pauseAll(); suspend fun resumeAll()
    fun tasks(): Flow<List<DownloadTask>>
    fun audioStorage(): Flow<List<RecitationStorage>>                 // per-recitation bytes (F-AU-08 storage manager)
    suspend fun deleteAudio(id: RecitationId, surahs: Set<SurahId>? = null /*null = all*/)
    suspend fun cleanPartials(): Long
    fun storageVolumes(): List<StorageVolume>                         // getExternalFilesDirs choices
    suspend fun pause(id: DownloadId); suspend fun resume(id: DownloadId)            // per request (doc 06 §13.6)
    fun recitationStatus(id: RecitationId): Flow<RecitationDownloadStatus>          // per surah: NONE/PARTIAL/QUEUED/RUNNING(p)/DONE
    suspend fun deleteAllExcept(ids: Set<RecitationId>)
    fun estimate(request: DownloadRequest.Audio): SizeEstimate                      // catalogue surahBytes minus downloaded
    suspend fun selectVolume(uuid: String?, moveExisting: Boolean)                  // doc 06 §13.8
}
sealed interface DownloadRequest {
    data class Audio(val recitation: RecitationId, val unit: AudioUnit, val wifiOnly: Boolean) : DownloadRequest
    data class Pack(val packId: String, val version: Int, val wifiOnly: Boolean) : DownloadRequest
}
sealed interface AudioUnit { data class Surahs(val s: Set<SurahId>) : AudioUnit; data class Para(val p: ParaId) : AudioUnit
                             data class Manzil(val m: ManzilId) : AudioUnit; data object Full : AudioUnit
                             data class Range(val range: AyahRange) : AudioUnit              // gapped ayah ranges
                             data class Passage(val ranges: List<AyahRange>) : AudioUnit }   // e.g. popular passages pack

interface PackManager {                                               // impl :core:data
    fun index(): Flow<PackIndexState>                                 // verified index (remote ∪ bundled), filtered
    suspend fun refreshIndex(force: Boolean = false): AppResult<Unit>
    fun installed(): Flow<List<InstalledPack>>
    suspend fun install(packId: String): AppResult<DownloadId>        // via DownloadManager
    suspend fun uninstall(packId: String): AppResult<Unit>            // bundled packs: only "disable"
    suspend fun ensureBundledInstalled()                              // first run / content bump (§14)
    fun notices(): Flow<List<Notice>>                                 // F-PL-11: kill-switches, text-error notices, min-version nudge
}

interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun update(transform: (UserSettings) -> UserSettings)
}

interface PrayerTimesRepository {                                     // P2, impl :core:prayer (adhan2)
    fun today(): Flow<PrayerDay>                                      // ticks at midnight / location change
    suspend fun day(date: LocalDate): PrayerDay
    fun next(): Flow<NextPrayer>                                      // countdown source for widget/home
    suspend fun month(year: Int, month: Int): List<PrayerDay>         // Ramadan timetable (F-CP-07)
}
interface LocationProvider {                                          // P2; coarse only
    suspend fun currentCoarse(timeoutMs: Long = 10_000): AppResult<GeoPoint>  // LocationManagerCompat.getCurrentLocation
    suspend fun searchCities(query: String, limit: Int = 20): List<City>      // offline GeoNames cities15000
    suspend fun nearestCity(p: GeoPoint): City?
}
interface HijriCalendarProvider {                                     // D-27: HijrahDate (desugared) default, ICU IslamicCalendar fallback
    fun today(): Flow<HijriDate>; suspend fun convert(date: LocalDate, afterMaghrib: Boolean): HijriDate
}
interface QiblaSensor { fun readings(): Flow<QiblaReading> /* bearing, heading, accuracy */ }
interface AlarmScheduler {                                            // v1.1 (first alarm release)
    suspend fun rescheduleAll(reason: RescheduleReason); fun exactAlarmsAllowed(): Boolean; fun health(): Flow<AdhanHealth>
    suspend fun scheduleTest(delay: Duration = 60.seconds, mode: AdhanMode)   // health check "test in 1 minute", same path as real alarms
}
interface ReminderScheduler { suspend fun rescheduleReminders() }    // ≤3/day; tap-to-play (F-CP-09)
interface BackupManager { suspend fun export(out: OutputStreamProvider): AppResult<Int>; suspend fun import(input: InputStreamProvider, mode: MergeMode): AppResult<ImportReport> }
interface DiagnosticsLog { fun log(tag: String, msg: String); suspend fun bundle(): AppResult<FileRef> }
```

`Clock` is `kotlin.time.Clock`. It is injected (bound in `DispatchersModule`) so that schedulers, the Quran-day boundary (03:00, doc 07 §2.6) and the Hijri rollover can be tested.

---

## 7. Data stores

### 7.1 Overview

| Store | Location | Written by | Lifecycle | Backup |
|---|---|---|---|---|
| `quran_content.db` | `databases/quran_content.db` (copied from `assets/content/quran_content.db`) | Pipeline only (read-only in the app) | Room `createFromAsset` + `fallbackToDestructiveMigration(dropAllTables = true)`; re-copied when `@Database(version)` is bumped | Excluded |
| Bundled packs | Per tier: `src/{personal,public}/assets/packs/<type>/<id>@<ver>.db` (timing packs: `assets/audio/timing/<id>@<ver>.db.gz`) → `files/packs/<type>/<id>/<ver>/pack.db` | Pipeline | Copied (timing: gunzipped) by `PackManager.ensureBundledInstalled()`, sha256 from `bundled.json` | Excluded |
| Downloaded packs | `files/packs/<type>/<id>/<ver>/pack.db` | `PackManager` (verified) | Atomic install and uninstall (v1.1) | Excluded |
| `user.db` | `databases/user.db` | The app | Room migrations; **never destructive** in release | **Included** |
| `downloads.db` | `databases/downloads.db` | `DownloadManager` (`:core:remote`; DDL in [06](06-audio-engine.md) §13.3) | Device-local download queue + verified-file index | Excluded (restored rows without files would lie) |
| Settings | `files/datastore/settings.json` | `SettingsRepository` | Typed DataStore, versioned JSON | **Included** |
| Playback resume | `files/datastore/playback_resume.json` | `:core:audio` `DataStore<PlaybackResume>` ([06](06-audio-engine.md) §10.3) | Listening pointer + `onPlaybackResumption` | Excluded |
| Remote cache | `files/remote/<channel>/{index,catalogue,notices}.json`, `files/remote/feeds/hijri-pk.json` (+`.sig`, `.etag`) | `SignedFileFetcher` | Replaced only after verification | Excluded |
| Alarm snapshot | Device-protected storage: `alarm_snapshot.json`, `boot_log.jsonl` ([07](07-companion-engagement-features.md) §5.4) | `:core:prayer` | Readable in direct boot | Excluded |
| Audio | `<getExternalFilesDirs()[i]>/audio/<recitationId>/…` + `.nomedia` | `DownloadManager` | User-managed | Excluded |
| Stream cache | `cache/media3/` (SimpleCache LRU 256 MB, user-settable) | Media3 | OS may clear it | Excluded |
| Logs | `files/logs/app-{0,1}.log` (256 KB ring) | `DiagnosticsLog` | Rolling | Excluded |

### 7.2 `quran_content.db` (schema owned here, populated by [08](08-data-pipelines.md))

**Contract**
- Each table below is a Room 3 `@Entity`, or an `@Fts5` entity, in `:core:content`. The pipeline creates the file by executing Room's exported `createSql`, index SQL, `contentSyncTriggers` and `setupQueries` verbatim (g04 §2.10).
- The pipeline then sets `PRAGMA user_version = @Database.version` and runs `VACUUM`.
- **CHECK constraints are not expressible in Room.** The pipeline's validators enforce them instead (invariants in §7.2.1), and they appear below as comments.
- `content_meta` and `attribution` are entities too, so DAOs can read them.
- The SQL shows the logical schema. Room emits back-quoted identifiers and `NOT NULL` exactly as the entity declares.

```sql
-- ===== Structure =====
CREATE TABLE surah (
  id               INTEGER NOT NULL PRIMARY KEY,         -- 1..114
  name_ar          TEXT    NOT NULL,                     -- with Indo-Pak diacritics (curated)
  name_ur          TEXT    NOT NULL,
  name_roman       TEXT    NOT NULL,                     -- Pakistani spelling (Yaseen, Rehman …)
  name_en          TEXT    NOT NULL,                     -- standard transliteration
  meaning_ur       TEXT    NOT NULL,
  meaning_en       TEXT    NOT NULL,
  revelation       TEXT    NOT NULL,                     -- 'makki' | 'madani'
  revelation_order INTEGER NOT NULL,
  ayah_count       INTEGER NOT NULL,
  ruku_count       INTEGER NOT NULL,
  first_ayah_id    INTEGER NOT NULL,
  basmala_kind     TEXT    NOT NULL                      -- 'verse' (1) | 'none' (9) | 'header'
);

CREATE TABLE ayah (
  id               INTEGER NOT NULL PRIMARY KEY,         -- AyahId 1..6236 (Kufan)
  surah_id         INTEGER NOT NULL,
  ayah_no          INTEGER NOT NULL,
  text_uthmani     TEXT    NOT NULL,                     -- Tanzil Uthmani verbatim (copy/share, D-08)
  text_imlaei      TEXT    NOT NULL,                     -- Tanzil Simple-Clean verbatim
  para_id          INTEGER NOT NULL,                     -- Indo-Pak Para (D-07)
  manzil_id        INTEGER NOT NULL,
  ruku_id          INTEGER NOT NULL,
  juz_madani       INTEGER NOT NULL,                     -- Madani mode only (P4)
  rub_madani       INTEGER NOT NULL,                     -- 1..240, Madani mode only
  first_word_index INTEGER NOT NULL,                     -- DK token range of this ayah (incl. end token)
  last_word_index  INTEGER NOT NULL,
  letters          INTEGER NOT NULL                      -- simple-clean letter count (timing QA, reading stats; doc 08)
);
CREATE UNIQUE INDEX ix_ayah_key   ON ayah(surah_id, ayah_no);
CREATE INDEX        ix_ayah_para  ON ayah(para_id);
CREATE INDEX        ix_ayah_ruku  ON ayah(ruku_id);

CREATE TABLE para (
  id                  INTEGER NOT NULL PRIMARY KEY,      -- 1..30
  name_ar             TEXT    NOT NULL,                  -- الٓمّٓ، سَیَقُوْلُ، تِلْكَ الرُّسُلُ … (curated)
  name_ur             TEXT    NOT NULL,
  name_roman          TEXT    NOT NULL,                  -- Alif Lam Meem, Sayaqool …
  start_ayah_id       INTEGER NOT NULL,                  -- P4=3:92, P7=5:83, P11=9:94, P14=15:2, P20=27:60, P21=29:45, P23=36:22
  end_ayah_id         INTEGER NOT NULL,
  madani_start_ayah_id INTEGER NOT NULL                  -- informational ("Madani juz starts 3:93")
);
CREATE TABLE manzil (
  id            INTEGER NOT NULL PRIMARY KEY,            -- 1..7; starts at surahs 1,5,10,17,26,37,50
  start_ayah_id INTEGER NOT NULL,
  end_ayah_id   INTEGER NOT NULL
);
CREATE TABLE ruku (
  id            INTEGER NOT NULL PRIMARY KEY,            -- 1..558 (QF/QUL set, print-verified)
  surah_id      INTEGER NOT NULL,
  ruku_in_surah INTEGER NOT NULL,
  para_id       INTEGER NOT NULL,                        -- Para where the ruku ENDS (D-07)
  ruku_in_para  INTEGER,                                 -- NULL for Al-Fatiha (excluded from Para-1 numbering)
  start_ayah_id INTEGER NOT NULL,
  end_ayah_id   INTEGER NOT NULL,
  ayah_count    INTEGER NOT NULL                         -- middle number of the ع triple
);
CREATE INDEX ix_ruku_surah ON ruku(surah_id);
CREATE INDEX ix_ruku_para  ON ruku(para_id);
CREATE TABLE sajda (
  ayah_id  INTEGER NOT NULL PRIMARY KEY,
  number   INTEGER,                                      -- 1..14 Hanafi (incl. 38:24); NULL for 22:77
  kind     TEXT    NOT NULL,                             -- 'hanafi' | 'shafii_only'
  label_ar TEXT                                          -- 22:77: «السجدة عند الامام الشافعی رح»
);
CREATE TABLE para_quarter (
  para_id   INTEGER NOT NULL,
  quarter   INTEGER NOT NULL,                            -- 1=ربع 2=نصف 3=ثلٰثة
  ayah_id   INTEGER NOT NULL,                            -- e.g. P4 ¾ = 3:198; P7 ¼ = 5:113
  after_word_pos INTEGER,                                -- mid-ayah marks, e.g. P7 ¾ = 6:73 after word 10
  verified_source TEXT,                                  -- print/edition the mark was checked against; release CI requires non-NULL
  PRIMARY KEY (para_id, quarter)
);

-- ===== Script-level text (DK IndoPak token stream, D-06) =====
CREATE TABLE word (
  word_index INTEGER NOT NULL PRIMARY KEY,               -- 1..83,669 (77,433 words + 6,236 end tokens)
  ayah_id    INTEGER NOT NULL,
  position   INTEGER NOT NULL,                           -- 1..n within ayah (DK positions)
  qul_position INTEGER NOT NULL,                         -- QUL word position (differs only in 72:16: DK 1–2 → QUL 1)
  is_end     INTEGER NOT NULL,                           -- 1 = ayah-end token
  text       TEXT    NOT NULL                            -- DK IndoPak glyph text (render only; never copy/share)
);
CREATE INDEX ix_word_ayah ON word(ayah_id, position);

CREATE TABLE ayah_marker (                               -- property of the script, not of the layout (g01 §2.6)
  script_id      INTEGER NOT NULL,                       -- 1 = DK IndoPak
  word_index     INTEGER NOT NULL,                       -- end token, or the word carrying U+08E2
  surah          INTEGER NOT NULL,
  verse_ayah     INTEGER NOT NULL,                       -- Kufan ayah CONTAINING the token
  display_number INTEGER,                                -- Kufan number of the ayah it ends (1:1 = 1); NULL for the 118 disputed marks.
                                                         -- Layout-specific display (Taj Fatiha's 7 empty circles) = pack `ayah_display_override`
  is_kufan_end   INTEGER NOT NULL,                       -- exactly 6,236 rows = 1
  disputed_end   INTEGER NOT NULL,                       -- exactly 118 rows = 1 (glyph only; never a boundary)
  has_ruku_sign  INTEGER NOT NULL,                       -- marker carries the ع ruku sign
  has_sajda_sign INTEGER NOT NULL,                       -- marker carries a sajda sign
  PRIMARY KEY (script_id, word_index)
);
CREATE INDEX ix_marker_display ON ayah_marker(script_id, surah, display_number);

-- ===== Search (D-11) =====
CREATE TABLE ayah_search (                               -- FTS external content
  ayah_id      INTEGER NOT NULL PRIMARY KEY,
  norm_imlaei  TEXT    NOT NULL,                         -- TextNormalizer.arabic(simple-clean)
  norm_uthmani TEXT    NOT NULL,                         -- TextNormalizer.arabic(uthmani)
  norm_indopak TEXT    NOT NULL                          -- TextNormalizer.arabic(DK ayah text)
);
CREATE VIRTUAL TABLE ayah_fts     USING FTS5(norm_imlaei, norm_uthmani, norm_indopak,
                                   tokenize=`unicode61`, content=`ayah_search`, content_rowid=`ayah_id`);
CREATE VIRTUAL TABLE ayah_fts_tri USING FTS5(norm_imlaei, norm_uthmani, norm_indopak,
                                   tokenize=`trigram`,   content=`ayah_search`, content_rowid=`ayah_id`);

CREATE TABLE alias (                                     -- F-SR-03
  id          INTEGER NOT NULL PRIMARY KEY,
  term_norm   TEXT    NOT NULL,                          -- folded: yasin, yaseen→yasin, ayatulkursi, یس, آیت الکرسی
  lang        TEXT    NOT NULL,                          -- 'roman' | 'ur' | 'ar' | 'en'
  target_type TEXT    NOT NULL,                          -- 'surah' | 'ayah' | 'passage' | 'para' | 'manzil'
  target_id   INTEGER NOT NULL,                          -- surah id | ayah id | passage id | para id | manzil id
  weight      INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX ix_alias_term ON alias(term_norm);
CREATE TABLE passage (                                   -- F-IX-08 quick access + playlists (Manzil dua)
  id INTEGER NOT NULL PRIMARY KEY, code TEXT NOT NULL, title_en TEXT NOT NULL, title_ur TEXT NOT NULL,
  title_ar TEXT, sort_order INTEGER NOT NULL, is_quick_access INTEGER NOT NULL
);
CREATE UNIQUE INDEX ix_passage_code ON passage(code);
CREATE TABLE passage_range (                             -- Manzil dua = 18 rows / 79 ayahs → PlayScope.Passage
  passage_id INTEGER NOT NULL, seq INTEGER NOT NULL, from_ayah_id INTEGER NOT NULL, to_ayah_id INTEGER NOT NULL,
  PRIMARY KEY (passage_id, seq)
);
CREATE TABLE daily_ayah (                                -- F-CP-08 widget / F-CP-09 reminder (v1.1); may ship empty in v1.0
  day_of_year  INTEGER NOT NULL PRIMARY KEY,             -- 1..366
  from_ayah_id INTEGER NOT NULL,
  to_ayah_id   INTEGER NOT NULL                          -- curated list, reviewer recorded in the pipeline CSV (doc 08)
);

-- ===== Provenance =====
CREATE TABLE content_meta (key TEXT NOT NULL PRIMARY KEY, value TEXT NOT NULL);
-- keys: content_version, built_at, pipeline_git_sha, sources_lock_sha256, normalizer_version, dk_font_sha256, content_tier
CREATE TABLE source (                                    -- one row per dataset (DS-xx, doc 03) shipped inside this DB
  dataset_id   TEXT NOT NULL PRIMARY KEY,
  licence_json TEXT NOT NULL                             -- LicenceInfo JSON: licenceStatus, basis, eligibleTiers, commercialOk, fossOk, … (doc 03 §4.1)
);
CREATE TABLE attribution (                               -- drives the grouped Credits screen (F-PL-04); one row per credit string
  dataset_id TEXT NOT NULL PRIMARY KEY,                  -- credit-string id, e.g. 'ATTR-01' (doc 03)
  title TEXT NOT NULL, source_url TEXT NOT NULL, licence TEXT NOT NULL, version TEXT NOT NULL,
  notice_en TEXT NOT NULL, notice_ur TEXT,
  section TEXT NOT NULL,                                 -- 'quran_text'|'mushaf'|'metadata'|'translation'|'tafsir'|'audio'|'fonts_art'
                                                         -- |'companion'|'proofreading'|'disclaimer'|'code' (pipeline-validated)
  sort INTEGER NOT NULL,
  title_ur TEXT,
  licence_text_asset TEXT                                -- e.g. 'licenses/OFL-1.1.txt'
);
```

Entity examples (Room 3, matching the spike's verified FTS5 pattern):

```kotlin
@Entity(tableName = "ayah_search")
internal data class AyahSearchEntity(
    @PrimaryKey @ColumnInfo(name = "ayah_id") val ayahId: Int,
    @ColumnInfo(name = "norm_imlaei") val imlaei: String,
    @ColumnInfo(name = "norm_uthmani") val uthmani: String,
    @ColumnInfo(name = "norm_indopak") val indopak: String,
)
@Fts5(contentEntity = AyahSearchEntity::class, contentRowId = "ayah_id", tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "ayah_fts")
internal data class AyahFts(@ColumnInfo(name = "norm_imlaei") val imlaei: String,
                            @ColumnInfo(name = "norm_uthmani") val uthmani: String,
                            @ColumnInfo(name = "norm_indopak") val indopak: String)
// AyahFtsTri identical with FtsOptions.TOKENIZER_TRIGRAM and tableName "ayah_fts_tri"

@Database(entities = [SurahEntity::class, AyahEntity::class, ParaEntity::class, ManzilEntity::class, RukuEntity::class,
    SajdaEntity::class, ParaQuarterEntity::class, WordEntity::class, AyahMarkerEntity::class, AyahSearchEntity::class,
    AyahFts::class, AyahFtsTri::class, AliasEntity::class, PassageEntity::class, PassageRangeEntity::class,
    DailyAyahEntity::class, ContentMetaEntity::class, SourceEntity::class, AttributionEntity::class],
    version = CONTENT_DB_VERSION, exportSchema = true)
internal abstract class QuranContentDb : RoomDatabase() {
    companion object {
        const val ASSET = "content/quran_content.db"
        fun build(ctx: Context, io: CoroutineContext) = Room.databaseBuilder<QuranContentDb>(ctx, "quran_content.db")
            .createFromAsset(ASSET)
            .fallbackToDestructiveMigration(dropAllTables = true)      // copy-destructive re-copy on version bump (r07 §2.5)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(io)
            .build()
    }
}
```

**Search query construction** (`SearchRepositoryImpl`):
1. `q = TextNormalizer.arabic(text)` and tokenize on whitespace.
2. **WORDS mode:** build `"t1" "t2" … "tn"*`. Each token is quoted, quotes are escaped, and only the last token gets a prefix `*`. Match with `ayah_fts MATCH ?` ordered by `bm25(ayah_fts)`.
3. **PARTIAL mode:** requires every token to be at least 3 characters (trigram). Match with `ayah_fts_tri MATCH ?`.
4. The UI offers "partial matches" automatically when WORDS returns 0 hits.
5. Translation scopes query each installed pack's FTS in parallel (`async` per pack) and merge results by `AyahId` order.
6. Surah-name and alias hits always appear first.
7. Snippets are highlighted in Kotlin over the display text.

#### 7.2.1 Content invariants (CI, JVM test over the shipped asset; complete list in [08](08-data-pipelines.md), [10](10-quality-testing-ci.md))
- `surah` has 114 rows and `ayah` has 6,236. Per-surah counts equal Tanzil.
- `para` has 30 rows. They are contiguous and cover everything, and their starts equal `QuranMeta.paraStarts`.
- `manzil` has 7 rows, starting at surahs {1, 5, 10, 17, 26, 37, 50}.
- `ruku` has 558 rows, contiguous. The by-end counts per Para are exactly 16, 16, 17, 14, 17, 14, 19, 17, 18, 17, 16, 16, 19, 22, 21, 17, 17, 17, 19, 16, 19, 18, 17, 19, 20, 18, 20, 20, 22, 39. Fatiha's `ruku_in_para` is NULL.
- `sajda` has 15 rows: 14 numbered `hanafi` plus 22:77 as `shafii_only`.
- `para_quarter` has 90 rows, and every `verified_source` is non-NULL in a BETA or PUBLIC build (the 53 unverified quarters are a closed-test blocker, doc 03).
- `word` has 83,669 rows, and every ayah's `[first_word_index, last_word_index]` is contiguous and ends on an `is_end` token.
- `ayah_marker` has 6,354 rows (`is_kufan_end` XOR `disputed_end`). It lives here, not in the mushaf pack, because it belongs to the script, not the layout.
- `daily_ayah` has 0 rows (v1.0) or 366 rows (v1.1), each a valid range.
- Every `dataset_id` used by an `attribution` or content row has a `source` row.
- Opening the DB **without** `room_master_table` passes `onValidateSchema`. This exercises the full validation path that a hash-only open skips (g04 §1.5).

### 7.3 Pack databases (read-only; one Room `@Database` class per pack type in `:core:content`, except timing)

Every pack except timing packs is a single SQLite file built from **that pack type's exported Room schema**, with the same contract as §7.2. Every pack contains `pack_info(key TEXT PRIMARY KEY, value TEXT NOT NULL)`, which holds id, type, version, schema_version, lang, dir, title, licence, attribution, source_url, source_sha256, min_content_version, built_at, and the eligibility fields `licence_status`, `basis`, `eligible_tiers`, `commercial_ok`, `foss_ok` (§11).

**Exception: timing packs are plain SQLite.** They follow doc 06 §5.1 exactly (`pack_info`, `audio_file`, `ayah_timing`, `WITHOUT ROWID`, no `room_master_table`). They are shipped as `.db.gz` and read through `androidx.sqlite` with `BundledSQLiteDriver` by `TimingPackReader` (three fixed queries). Room is not used for them, so they are free of Room identity hashes.

Room packs are opened with `Room.databaseBuilder<T>(ctx, absolutePath)`. A name that is an absolute path "is used as is" (verified in Room 3 `Room.android.kt` docs; P0 spike S-1). The builder uses `BundledSQLiteDriver()` and has **no** migration fallback: a `schema_version` other than the app's supported version means the pack is never opened (`NEEDS_APP_UPDATE`). `PackDbOpener` keeps a reference-counted LRU of open pack DBs (max 6) and closes them on uninstall.

| Pack type | `type` | Tables (owner) | Bundled in APK |
|---|---|---|---|
| Translation | `translation` | `pack_info`; `translation(ayah_id INTEGER PK, text TEXT NOT NULL, footnotes TEXT)`; `translation_search(ayah_id PK, text_norm TEXT NOT NULL)`; `translation_fts` FTS5 unicode61 + `translation_fts_tri` FTS5 trigram, both external content over `translation_search` (**doc 04**) | `ur.junagarhi.qe` (with Ahsan-ul-Bayan footnotes), `en.saheeh.qe` (D-09), in `src/main/assets/packs/translation/` only if eligible in every tier, otherwise per tier |
| Tafsir | `tafsir` | `pack_info`; `tafsir(id INTEGER PK, from_ayah_id INTEGER NOT NULL, to_ayah_id INTEGER NOT NULL, text TEXT NOT NULL)` + index `(from_ayah_id, to_ayah_id)` (**doc 04**) | none (P3) |
| Mushaf layout | `mushaf_layout` | **Doc 05 owns the DDL**: `pack_info` (incl. `mushaf_id` numeric, `mushaf_code`, `proof_certificate_id`), `mushaf`, `page` (incl. `page_hash`), `line`, `word` (`word.id` = content-DB `word_index`), `ayah_page`, `surah_header`, `margin_marker`, `furniture_run`, `ornament_item`, `calibration`, `ayah_display_override` (Taj Fatiha's empty circles), `word_override` (**build-time** errata record, §14.2). Uses `word` and `ayah_marker` from the content DB; never duplicates them. Pack id `mushaf.<code>.<script>`, e.g. `mushaf.indopak16_taj.dk` | **Per tier, never `src/main`:** `src/personal/assets/packs/mushaf_layout/mushaf.indopak16_taj.dk@<ver>.db` (QUL-derived allowed); `src/public/assets/packs/mushaf_layout/` holds only a G1-cleared or clean-room pack, otherwise the public build fails (§11.3) |
| Timing | `timing` | **Doc 06 §5.1 owns the DDL**: `pack_info`, `audio_file`, `ayah_timing`. Plain SQLite (see above); one pack per gapless recitation, id `timing.<recitation-suffix>` (doc 06) | The 12 public timing packs in `assets/audio/timing/` (about 1–4 MB in total; highlight works offline from first launch). Updates via the index (v1.1) |
| Font | `font` | `pack_info` + font file beside it (`font.ttf`) | DK IndoPak glyf TTF ships in `assets/fonts/` (not a pack) |
| Azkar | `azkar` | **Doc 04** (content and review in doc 07 §9, rights in doc 03): `pack_info`; `dhikr_category(id INTEGER PK, slug TEXT NOT NULL, name_en TEXT NOT NULL, name_ur TEXT NOT NULL, sort INTEGER NOT NULL)`; `dhikr(id INTEGER PK, category_id INTEGER NOT NULL, sort INTEGER NOT NULL, arabic TEXT NOT NULL, repeat INTEGER NOT NULL, reference TEXT NOT NULL, quran_ranges TEXT, audio TEXT NOT NULL /*NONE|QURAN|CLIP*/, clip TEXT, source_dataset TEXT NOT NULL, licence TEXT NOT NULL, reviewed_by TEXT, reviewed_on TEXT)` + index `(category_id, sort)`; `dhikr_translation(dhikr_id INTEGER NOT NULL, lang TEXT NOT NULL /*en, ur, ur-Latn*/, text TEXT NOT NULL, transliteration TEXT, source TEXT NOT NULL, licence TEXT NOT NULL, PRIMARY KEY(dhikr_id, lang))`. Quranic items reference ayahs by `quran_ranges` ("2:255;112:1-4") and render from the content DB | `azkar.core` from v1.1 (F-CP-06). The Manzil dua is a content-DB `passage`, not an azkar row |
| Cities | `cities` | **Columns per doc 07 §3**: `city`, `city_alias`, `country`, `admin1` (GeoNames cities15000, CC BY 4.0). Search is a prefix range scan on the `city_alias` primary key; an FTS5 table is optional and not required | yes (v1.0 prayer times, about 1.5 MB compressed) |

Normalization for translation search: `TextNormalizer.urdu()` for `ur` and `TextNormalizer.english()` for `en` and Latin-script packs. There is no stemmer in v1 (FTS tokenizer settings are part of Room's schema validation, so one translation entity class serves every language).

### 7.4 `user.db` (schema owned here)

**Schema v1** is frozen on the day the P1 daily-use build ships (2026-12-21). It **already contains every P1 and P2 table** that docs 01, 02 and 07 need, including the tables for features that slipped to v1.1. Neither the Ramadan release nor v1.1 therefore needs a migration. **P3 tables arrive in v2** (the first migration): `hifz_plan`, `hifz_page`, `hifz_session`, `hifz_mistake`, `hifz_recording` (doc 07 §18.2), QF sync state (F-BM-07) and the realtime group-khatam cache (F-CP-11, play flavor).

Doc 07 uses the table names below. Where an earlier doc 07 draft used another name, the mapping is: `khatam_portion` → `khatam_progress`, `page_read_log` → `page_dwell`, `activity_day` → `reading_day`, `alarm_log` → `adhan_log` (it logs every alarm type, not only adhan). `tasbeeh_preset`, `tasbeeh_session`, `tasbeeh_day`, `azkar_progress`, `alarm_schedule`, `reminder_log`, `group_khatam` and `group_khatam_part` keep doc 07's names.

**Deliberately not tables.**
- The prayer location, Hijri user offset and other companion choices are single values in `UserSettings.companion` (§7.5). The PK month-start feed is a cached signed file (`files/remote/feeds/`).
- The listening pointer is doc 06's `DataStore<PlaybackResume>`. The download queue and the verified-audio index are doc 06's `downloads.db`. Both are device-local and excluded from backup, so a restore never claims files that are not there.

Conventions: `mushaf_id TEXT` holds `MushafId.code`. Verse references are Kufan `(surah, ayah)` integer pairs. `day` columns hold the local epochDay of the **Quran-day** (03:00 → 03:00, doc 07 §2.6). Times are epoch milliseconds.

`user.db` opens with `setJournalMode(JournalMode.TRUNCATE)` (the API exists in Room 3 `RoomDatabase.Builder`) so the file is self-contained for Auto Backup. It is a small, write-light DB, so the cost is negligible.

```sql
-- ===== P1: reading, bookmarks, recents, search =====
CREATE TABLE bookmark_collection (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  uuid TEXT NOT NULL, name TEXT NOT NULL, color INTEGER NOT NULL, icon TEXT,
  sort_order INTEGER NOT NULL DEFAULT 0, is_default INTEGER NOT NULL DEFAULT 0,
  created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, deleted_at INTEGER
);
CREATE UNIQUE INDEX ix_coll_uuid ON bookmark_collection(uuid);

CREATE TABLE bookmark (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  uuid TEXT NOT NULL,                       -- stable id for JSON export / QF sync
  kind TEXT NOT NULL,                       -- 'AYAH' | 'PAGE' | 'RANGE'
  surah INTEGER NOT NULL, ayah INTEGER NOT NULL,          -- anchor (PAGE: first ayah on page → remappable)
  end_surah INTEGER, end_ayah INTEGER,                    -- RANGE only
  mushaf_id TEXT, page_index INTEGER,                     -- PAGE only (layout-scoped)
  collection_id INTEGER REFERENCES bookmark_collection(id) ON DELETE SET NULL,
  label TEXT, color INTEGER,
  created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL,
  deleted_at INTEGER                        -- soft delete: undo + sync tombstone; purged after 30 days
);
CREATE UNIQUE INDEX ix_bm_uuid ON bookmark(uuid);
CREATE INDEX ix_bm_key  ON bookmark(surah, ayah);
CREATE INDEX ix_bm_page ON bookmark(mushaf_id, page_index);
CREATE INDEX ix_bm_coll ON bookmark(collection_id);

CREATE TABLE last_read (                    -- F-BM-01: one pointer per mushaf; written after a 4 s dwell; never touched by bookmarks
  mushaf_id TEXT PRIMARY KEY NOT NULL, page_index INTEGER NOT NULL,
  surah INTEGER NOT NULL, ayah INTEGER NOT NULL, mode TEXT NOT NULL, updated_at INTEGER NOT NULL
);
CREATE TABLE recent_item (                  -- F-IX-09
  kind TEXT NOT NULL,                       -- 'SURAH' | 'PARA' | 'MANZIL' | 'PAGE' | 'PASSAGE'
  ref TEXT NOT NULL,                        -- "36", "23", "indopak16_taj/50", "manzil-dua"
  opened_at INTEGER NOT NULL, PRIMARY KEY (kind, ref)
);
CREATE TABLE page_dwell (                   -- per Quran-day page reading (doc 07 "page_read_log"): khatam suggest, streaks, stats
  day INTEGER NOT NULL, mushaf_id TEXT NOT NULL, page_index INTEGER NOT NULL,
  dwell_ms INTEGER NOT NULL,                -- a page counts as read at ≥ 20 s (doc 07 §15)
  PRIMARY KEY (day, mushaf_id, page_index)
);
CREATE TABLE reading_day (                  -- F-BM-05 streaks/stats (doc 07 "activity_day"); local only (D-20)
  day INTEGER PRIMARY KEY NOT NULL,
  pages_read INTEGER NOT NULL, read_ms INTEGER NOT NULL, listen_ms INTEGER NOT NULL,
  ayahs_listened INTEGER NOT NULL, portion_done INTEGER NOT NULL DEFAULT 0, freeze_used INTEGER NOT NULL DEFAULT 0
);
CREATE TABLE search_history (query TEXT PRIMARY KEY NOT NULL, used_at INTEGER NOT NULL);  -- max 20, user-clearable
CREATE TABLE pref_kv (                      -- "settings not in DataStore": per-entity or unbounded prefs
  namespace TEXT NOT NULL,                  -- 'mushaf_view' (key 'zoom.<mushafCode>', 1.0–2.0), 'reciter_override' (per-surah reciter)
  key TEXT NOT NULL, value TEXT NOT NULL, updated_at INTEGER NOT NULL, PRIMARY KEY (namespace, key)
);
CREATE TABLE installed_pack (
  pack_id TEXT PRIMARY KEY NOT NULL, type TEXT NOT NULL, version INTEGER NOT NULL, schema_version INTEGER NOT NULL,
  path TEXT NOT NULL, bytes INTEGER NOT NULL, sha256 TEXT NOT NULL, source TEXT NOT NULL,  -- 'BUNDLED' | 'DOWNLOADED'
  state TEXT NOT NULL,                      -- 'ACTIVE' | 'DISABLED' | 'UPDATE_AVAILABLE' | 'BLOCKED'
  licence_json TEXT NOT NULL, attribution_json TEXT NOT NULL,
  eligibility_json TEXT NOT NULL,           -- {licenceStatus, basis, eligibleTiers, commercialOk, fossOk} (§11)
  installed_at INTEGER NOT NULL
);

-- ===== P2: notes, khatam, group sheet =====
CREATE TABLE note (                         -- F-BM-03
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, uuid TEXT NOT NULL,
  surah INTEGER NOT NULL, ayah INTEGER NOT NULL, end_surah INTEGER, end_ayah INTEGER,
  mushaf_id TEXT, page_index INTEGER,       -- where the note was written (optional)
  body TEXT NOT NULL,                       -- plain text, ≤ 10,000 chars
  color INTEGER,
  created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, deleted_at INTEGER
);
CREATE UNIQUE INDEX ix_note_uuid ON note(uuid);
CREATE INDEX ix_note_key ON note(surah, ayah);
CREATE INDEX ix_note_updated ON note(updated_at);

CREATE TABLE khatam_plan (                  -- F-BM-04, F-CP-07 Taraweeh preset; algorithm doc 07 §14
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, uuid TEXT NOT NULL,
  title TEXT NOT NULL, kind TEXT NOT NULL,  -- 'PERSONAL' | 'RAMADAN' | 'GROUP_SHEET'
  preset TEXT,                              -- e.g. 'TARAWEEH_27', NULL = custom
  mushaf_id TEXT NOT NULL, unit TEXT NOT NULL,            -- 'PAGE' | 'PARA' | 'RUKU' | 'MANZIL'
  snap_to_ruku INTEGER NOT NULL DEFAULT 1,
  start_page INTEGER NOT NULL, end_page INTEGER NOT NULL, -- page_index range
  start_day INTEGER NOT NULL, target_day INTEGER NOT NULL, rest_days_mask INTEGER NOT NULL DEFAULT 0, -- bit0 = Mon … bit6 = Sun
  cursor_surah INTEGER NOT NULL, cursor_ayah INTEGER NOT NULL, cursor_page INTEGER NOT NULL,     -- first unread Kufan key
  reminder_minute INTEGER,                  -- minute of day, NULL = none
  dedication TEXT, group_ref TEXT,          -- group sheet id / P3 Firestore id
  created_at INTEGER NOT NULL, completed_at INTEGER, archived_at INTEGER
);
CREATE TABLE khatam_progress (              -- one row per planned portion (doc 07 "khatam_portion")
  plan_id INTEGER NOT NULL REFERENCES khatam_plan(id) ON DELETE CASCADE,
  seq INTEGER NOT NULL,                     -- 1..n in plan order
  day INTEGER NOT NULL,                     -- planned Quran-day
  from_surah INTEGER NOT NULL, from_ayah INTEGER NOT NULL, to_surah INTEGER NOT NULL, to_ayah INTEGER NOT NULL,
  from_page INTEGER NOT NULL, to_page INTEGER NOT NULL,
  done_at INTEGER, via TEXT,                -- 'MANUAL' | 'SUGGESTED' | 'AUDIO'
  claimed_by TEXT,                          -- group sheet name (text only)
  PRIMARY KEY (plan_id, seq)
);
CREATE TABLE group_khatam (                 -- F-CP-11 text sheet (P2; ships v1.1). Realtime cache is P3 (v2)
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  sheet_id TEXT NOT NULL,                   -- 6 chars Crockford base32
  title TEXT NOT NULL, dedication TEXT, unit TEXT NOT NULL,  -- 'PARA' | 'MANZIL'
  copies INTEGER NOT NULL, deadline_day INTEGER, mushaf_id TEXT NOT NULL,
  created_at INTEGER NOT NULL, completed_at INTEGER, owner INTEGER NOT NULL
);
CREATE TABLE group_khatam_part (
  khatam_id INTEGER NOT NULL REFERENCES group_khatam(id) ON DELETE CASCADE,
  copy INTEGER NOT NULL, part INTEGER NOT NULL, name TEXT,
  status TEXT NOT NULL,                     -- 'FREE' | 'CLAIMED' | 'DONE'
  updated_at INTEGER NOT NULL, PRIMARY KEY (khatam_id, copy, part)
);

-- ===== P2: tasbeeh, azkar =====
CREATE TABLE tasbeeh_preset (               -- F-CP-05; built-ins seeded (Tasbih-e-Fatimah 33/33/34 …)
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  title_en TEXT NOT NULL, title_ur TEXT NOT NULL,
  phases_json TEXT NOT NULL,                -- [{"ar":"سُبْحَانَ اللّٰهِ","target":33}, …]
  builtin_key TEXT, sort INTEGER NOT NULL, loop INTEGER NOT NULL
);
CREATE TABLE tasbeeh_session (
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  preset_id INTEGER NOT NULL REFERENCES tasbeeh_preset(id) ON DELETE CASCADE,
  started_at INTEGER NOT NULL, updated_at INTEGER NOT NULL,
  phase INTEGER NOT NULL, count_in_phase INTEGER NOT NULL, total INTEGER NOT NULL, rounds INTEGER NOT NULL
);
CREATE TABLE tasbeeh_day (                  -- private daily totals ("history")
  day INTEGER NOT NULL, preset_id INTEGER NOT NULL REFERENCES tasbeeh_preset(id) ON DELETE CASCADE,
  total INTEGER NOT NULL, PRIMARY KEY (day, preset_id)
);
CREATE TABLE azkar_progress (               -- F-CP-06 (v1.1); ids refer to the `azkar` pack
  day INTEGER NOT NULL, category_id INTEGER NOT NULL, dhikr_id INTEGER NOT NULL, count INTEGER NOT NULL,
  PRIMARY KEY (day, category_id, dhikr_id)
);

-- ===== P2: alarms and reminders =====
CREATE TABLE alarm_schedule (               -- desired set written by rescheduleAll (doc 07 §5.2); rebuilt after restore
  request_code INTEGER PRIMARY KEY NOT NULL,-- (epochDay mod 10000) * 100 + slot
  type TEXT NOT NULL, prayer TEXT, fire_at INTEGER NOT NULL,
  api TEXT NOT NULL,                        -- 'CLOCK' | 'EXACT_IDLE' | 'INEXACT' | 'RTC' | 'WINDOW'
  mode TEXT, created_at INTEGER NOT NULL
);
CREATE TABLE adhan_log (                    -- every alarm firing incl. lateness (doc 07 "alarm_log"); keep newest 200
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  request_code INTEGER NOT NULL, type TEXT NOT NULL, prayer TEXT,
  scheduled_at INTEGER NOT NULL, fired_at INTEGER, lateness_ms INTEGER,
  outcome TEXT NOT NULL,                    -- 'PLAYED_FULL'|'PLAYED_SHORT'|'NOTIFIED_SILENT'|'SUPPRESSED_SILENT'|'SUPPRESSED_DND'
                                            -- |'IN_CALL'|'LATE_NOTIFIED'|'MISSED'|'FGS_DENIED'|'STOPPED_BY_USER'
  exact_allowed INTEGER NOT NULL, standby_bucket INTEGER, api TEXT NOT NULL
);
CREATE INDEX ix_adhan_log_sched ON adhan_log(scheduled_at);
CREATE TABLE reminder (                     -- F-CP-09 custom and khatam reminders; Kahf/Mulk/daily-ayah toggles live in settings
  id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
  kind TEXT NOT NULL,                       -- 'KAHF_FRIDAY' | 'MULK_NIGHT' | 'DAILY_AYAH' | 'KHATAM' | 'CUSTOM'
  enabled INTEGER NOT NULL, minute_of_day INTEGER NOT NULL, days_mask INTEGER NOT NULL,
  payload_json TEXT, next_at INTEGER
);
CREATE TABLE reminder_log (                 -- enforces ≤ 3/day and quiet hours (doc 07 §11)
  day INTEGER NOT NULL, type TEXT NOT NULL,
  scheduled_at INTEGER NOT NULL, delivered_at INTEGER,
  outcome TEXT NOT NULL,                    -- 'DELIVERED'|'SUPPRESSED_DONE'|'SUPPRESSED_CAP'|'SUPPRESSED_QUIET'|'DISMISSED_TODAY'
  PRIMARY KEY (day, type)
);
```

**Migration policy**
1. **Before 2026-12-21** (no real users): `fallbackToDestructiveMigration` is allowed in **debug only**. Schema JSON is exported from day 1.
2. **From v1** (2026-12-21):
   - Every schema change bumps the version and ships an `@AutoMigration` (with `AutoMigrationSpec` for renames and deletes) or a hand-written `Migration`.
   - Each migration has a `room3-testing` `MigrationTestHelper` test that runs from **every** earlier exported version, plus a data-preservation test with a seeded fixture.
   - Release builds never set a destructive fallback. A missing migration fails CI (the `criterion.room3` plugin checks that `schemas/` contains N-1 → N).
3. **Downgrade** (from a backup restored onto an older app) is refused with a clear error, and the user is offered JSON import instead.
4. **JSON export** (`BackupSerializer`) carries `exportVersion` independently of the DB version. Importers accept every older `exportVersion`.
5. Columns are never repurposed. Tables are deleted only after two releases of non-use.

### 7.5 Typed settings (`DataStore<UserSettings>` in `:core:database`)

```kotlin
@Serializable
data class UserSettings(
    val schema: Int = 1,
    val onboardingDone: Boolean = false,
    val ui: Ui = Ui(), val reader: Reader = Reader(), val translation: Translation = Translation(),
    val audio: Audio = Audio(), val downloads: Downloads = Downloads(),
    val companion: CompanionSettings = CompanionSettings(),   // prayer, adhan, hijri, reminders, ramadan, tasbeeh, streaks
    val privacy: Privacy = Privacy(),
) {
    @Serializable data class Ui(val language: UiLanguage = UiLanguage.SYSTEM, val theme: Theme = Theme.SYSTEM,
                                val fontScaleUrdu: Float = 1f, val haptics: Boolean = false,
                                val openTo: StartDestination = StartDestination.HOME)   // HOME | READER ("open straight into reader")
    @Serializable data class Reader(
        val mushaf: MushafId = MushafId.INDOPAK_16_TAJ,
        val keepScreenOn: Boolean = true, val keepScreenOnMinutes: Int = 15, // on while the reader is visible; off after 15 min idle
        val immersive: Boolean = true, val rotationLock: RotationLock = RotationLock.OFF, // phones only (D-23)
        val pageTurnLock: Boolean = false,           // blocks user swipes, never audio auto-turn
        val pageTurnHaptics: Boolean = false, val pageCurl: Boolean = false,            // F-RD-12 (v1.1)
        val numbering: Numbering = Numbering.INDOPAK,                                     // Fatiha display vs Kufan
        val followAlong: Boolean = true, val autoPageTurn: Boolean = true,
        val nightInk: NightInk = NightInk.WARM, val autoScrollSpeed: Int = 3,             // F-RD-09 (v1.1)
    )   // per-mushaf zoom (1.0–2.0) is in pref_kv('mushaf_view', 'zoom.<code>'); renderer keys in doc 05
    @Serializable data class Translation(val urdu: String? = "ur.junagarhi.qe", val english: String? = "en.saheeh.qe",
                                         val showUnderAyah: Boolean = true, val showFootnotes: Boolean = true)
    // NB: there is deliberately no "translation only" flag — Arabic is always shown (D-09, D-22).
    @Serializable data class Audio(val recitationId: RecitationId = RecitationId("alafasy.murattal.mp3q123"), // doc 06 id
                                   val speed: Float = 1f /*0.5..2.0*/, val defaultRepeat: RepeatSpec = RepeatSpec(),
                                   val duckInsteadOfPause: Boolean = false, val nextPrevUnit: NextPrevUnit = NextPrevUnit.AYAH,
                                   val streamOnCellular: CellularPolicy = CellularPolicy.ASK, // ASK = ask once, then remember
                                   val streamQuality: StreamQuality = StreamQuality.AUTO, val cacheMb: Int = 256)
    @Serializable data class Downloads(val wifiOnly: Boolean = true, val volumeIndex: Int = 0)
    @Serializable data class Privacy(val crashReportsOptIn: Boolean = false /*v1.1, F-PL-09*/, val keepSearchHistory: Boolean = true,
                                     val ratingPromptShownAt: Long? = null /*F-PL-10, play only*/)
}
// CompanionSettings and its parts (LocationSetting, PrayerSettings, AdhanSettings, HijriSettings, ReminderSettings,
// RamadanSettings, TasbeehSettings, StreakSettings) are defined in doc 07 §2.4 and live in :core:model. Doc 04 fixes
// these defaults: location = null (prayer features idle until set; coordinates rounded to 0.01°); calculation method
// and Asr school = region default (Karachi/Hanafi for PK); the adhan enable flow pre-selects FULL for all five fard
// prayers; playInSilent = false (silent/vibrate/DND respected unless opted in); Hijri userOffsetDays in −2..+2,
// followRegionFeed = true, maghribRollover = true; reminders ≤ 3/day with quiet hours 22:30–05:00.
enum class UiLanguage { SYSTEM, EN, UR }
enum class Theme { SYSTEM, LIGHT, SEPIA, NIGHT, AMOLED }
```

- The serializer is `Json { ignoreUnknownKeys = true; encodeDefaults = false }`, and `defaultValue = UserSettings()`.
- On `SerializationException`, the serializer backs the file up to `settings.corrupt.json` and returns defaults (`CorruptionHandler`).
- Settings schema bumps run a pure `migrate(old: JsonObject): JsonObject` chain.
- The UI language is **mirrored** into `AppCompatDelegate.setApplicationLocales` (the source of truth for the platform). DataStore keeps the choice so it survives backup.

### 7.6 Pack index (`index.json`) and signature verification

**Transport (the signing format for every remote JSON: index, catalogue, notices, Hijri feed).** Every signed file `X.json` has a sibling `X.json.sig` whose content is the base64 encoding of the 64-byte Ed25519 signature over the **exact bytes** of `X.json`. There is no envelope and no JSON canonicalization. Every signed JSON carries `schema`, `sequence` (monotonic, anti-rollback), `generatedAt`, `expiresAt` and an optional `kid` (which pinned key signed it). The app pins **two** public keys, active and standby, in `BuildConfig.TRUSTED_KEYS` (hex), and a file verifies if either key accepts it. The private keys stay offline with the owner and never enter CI ([08](08-data-pipelines.md)). Both files are uploaded with `Cache-Control: max-age=60`.

JSON Schema (2020-12, abridged to the normative fields):

```json
{
  "$id": "https://criterion.invalid/schema/pack-index-1.json",
  "type": "object",
  "required": ["schema", "sequence", "generatedAt", "expiresAt", "channel", "packs"],
  "properties": {
    "schema":      { "const": 1 },
    "kid":         { "type": "string", "description": "id of the signing key (informational; verification tries both pinned keys)" },
    "sequence":    { "type": "integer", "minimum": 1, "description": "monotonic; see the acceptance rule below (anti-rollback)" },
    "generatedAt": { "type": "string", "format": "date-time" },
    "expiresAt":   { "type": "string", "format": "date-time", "description": "after expiry: no new installs offered; installed packs keep working" },
    "channel":     { "enum": ["beta", "public"], "description": "must equal the channel the app fetched from" },
    "minAppVersionCode": { "type": "integer" },
    "packs": { "type": "array", "items": { "$ref": "#/$defs/pack" } }
  },
  "$defs": {
    "pack": {
      "type": "object",
      "required": ["id", "type", "version", "schemaVersion", "title", "url", "bytes", "sha256", "licence", "attribution",
                   "licenceStatus", "basis", "eligibleTiers", "commercialOk", "fossOk", "enabled"],
      "properties": {
        "id":            { "type": "string", "pattern": "^[a-z0-9]+([._-][a-z0-9]+)*$" },
        "type":          { "enum": ["translation", "tafsir", "mushaf_layout", "timing", "font", "azkar", "cities"] },
        "version":       { "type": "integer", "minimum": 1 },
        "schemaVersion": { "type": "integer", "minimum": 1 },
        "lang":          { "type": "string" }, "dir": { "enum": ["rtl", "ltr"] },
        "title":         { "type": "object", "required": ["en"], "properties": { "en": {"type":"string"}, "ur": {"type":"string"}, "ar": {"type":"string"} } },
        "translator":    { "type": "string" },
        "url":           { "type": "string", "format": "uri" },
        "mirrors":       { "type": "array", "items": { "type": "string", "format": "uri" } },
        "compression":   { "enum": ["zip", "gzip", "none"] },
        "bytes":         { "type": "integer" }, "unpackedBytes": { "type": "integer" },
        "sha256":        { "type": "string", "pattern": "^[0-9a-f]{64}$", "description": "of the downloaded (compressed) file" },
        "unpackedSha256":{ "type": "string", "pattern": "^[0-9a-f]{64}$" },
        "minContentVersion": { "type": "integer" }, "maxContentVersion": { "type": "integer" },
        "licence": { "type": "object", "required": ["name", "redistribution"],
          "properties": { "name": {"type":"string"}, "spdx": {"type":"string"}, "url": {"type":"string"},
                          "redistribution": {"type":"boolean"},
                          "conditions": {"type":"array", "items": {"type":"string"}} } },
        "attribution": { "type": "object", "required": ["en"], "properties": { "en": {"type":"string"}, "ur": {"type":"string"} } },
        "licenceStatus": { "enum": ["PERMITTED", "HOST_GRANT", "PENDING", "PERSONAL_ONLY", "REVOKED"], "description": "D-29" },
        "basis":         { "type": "string", "description": "free text: why the status holds (licence name, permission ref PERM-xx, 'clean-room')" },
        "eligibleTiers": { "type": "array", "items": { "enum": ["PERSONAL", "BETA", "PUBLIC"] }, "uniqueItems": true },
        "commercialOk":  { "type": "boolean", "description": "false for every Tanzil translation; required when MONETIZED" },
        "fossOk":        { "type": "boolean", "description": "may ship in fossPublic builds" },
        "enabled":       { "type": "boolean", "description": "kill-switch: false = hide + block opening" },
        "disabledReason": { "type": "object", "properties": { "en": {"type":"string"}, "ur": {"type":"string"} } },
        "autoUpdate":    { "type": "boolean", "description": "QuranEnc 'keep updated' condition → true" }
      }
    }
  }
}
```

Example entry:

```json
{ "id": "ur.jalandhari.tanzil", "type": "translation", "version": 1, "schemaVersion": 1, "lang": "ur", "dir": "rtl",
  "title": { "en": "Fateh Muhammad Jalandhari", "ur": "فتح محمد جالندھری" },
  "url": "https://cdn.example.invalid/v1/packs/translation/ur.jalandhari.tanzil/1/ur.jalandhari.tanzil-1.zip",
  "compression": "zip", "bytes": 731004, "sha256": "…", "unpackedBytes": 2201088, "unpackedSha256": "…",
  "minContentVersion": 1,
  "licence": { "name": "Tanzil translation terms (non-commercial, verbatim, with attribution)", "redistribution": true },
  "attribution": { "en": "Translation by Fateh Muhammad Jalandhari, via Tanzil.net", "ur": "…" },
  "licenceStatus": "PERMITTED", "basis": "Tanzil translation terms (DS-xx, doc 03)",
  "eligibleTiers": ["PERSONAL", "BETA", "PUBLIC"], "commercialOk": false, "fossOk": true, "enabled": true,
  "autoUpdate": false }
```

**Verification and acceptance algorithm** (`SignedFileFetcher`, used for index, catalogue, notices and feeds):

```
fetch(name):                                              // name = "<channel>/index.json", "feeds/hijri-pk.json", …
  resp = GET name with If-None-Match: cachedEtag          // OkHttp, 15 s connect / 30 s read
  if resp == 304: touch cache; return checkExpiry(cached)
  body = resp.bytes (cap 2 MB); sig = GET name + ".sig" (cap 1 KB) → base64 decode (must be 64 bytes)
  if not verifies(body, sig):
      // a publish can race the two GETs (new body, old sig): retry both once, bypassing caches
      body, sig = GET name, GET name + ".sig" with Cache-Control: no-cache
      if not verifies(body, sig): log; keep cached copy; return AppError.Integrity("sig")
  doc = Json.decode(body); require doc.schema == SUPPORTED (else NEEDS_APP_UPDATE, keep cached)
  if doc has channel and doc.channel != expectedChannel: return AppError.Integrity("channel")
  last = lastAccepted[name]                               // (sequence, sha256)
  accept only if doc.sequence > last.sequence
          or (doc.sequence == last.sequence and sha256(body) == last.sha256)   // same file re-served
  else: return AppError.Integrity("rollback")             // lower sequence, or same sequence with different bytes
  write body+sig to files/remote/<name>.tmp → fsync → atomic rename; store etag, sequence, sha256
  return checkExpiry(doc)

verifies(body, sig) = any(Ed25519.verify(pubKey, body, sig) for pubKey in TRUSTED_KEYS)
checkExpiry(doc): if now > doc.expiresAt → keep the doc for installed items and active kill-switches,
                  offer no new installs, and show "content list out of date" in the pack/reciter screens
```

**Pack install** (`PackManagerImpl.install`):
1. Check eligibility (§11) and `minContentVersion ≤ CONTENT_DB_VERSION ≤ maxContentVersion`, and check free space ≥ 2 × `unpackedBytes`.
2. `DownloadManager.enqueue(Pack)`. This downloads to `.part`, then streaming sha256 must equal `sha256`.
3. Unzip into `files/packs/.staging/<id>-<ver>/`. Zip-slip guard: reject entries with `..` or absolute paths, cap the uncompressed size at `unpackedBytes` + 1 %, and allow at most 4 entries.
4. sha256 of `pack.db` must equal `unpackedSha256`.
5. Open read-only once and check `pack_info.id`, `version`, `schema_version` and `type`. The row count must match the pack type (translation = 6,236). Run `PRAGMA quick_check`.
6. Atomically rename to `files/packs/<type>/<id>/<ver>/`, upsert `installed_pack`, and emit the invalidation.
7. Delete the previous version once `PackDbOpener` has released its refcount.

**Ed25519 implementation.** We vendor a verify-only Kotlin port of the public-domain TweetNaCl `crypto_sign_open` in `:core:remote/crypto/Ed25519.kt` (about 250 LOC), tested against the RFC 8032 §7.1 vectors and the Wycheproof `ed25519_test.json` set. This works identically on API 24–37, where platform Ed25519 availability varies. *Alternative considered:* BouncyCastle `bcprov-jdk18on` (version unverified; pin only if the port fails its vectors), which costs dex weight. Tink was rejected because it pulls in protobuf.

### 7.7 File-system layout and backup rules

```
/data/data/com.chughtai.criterion[.personal][.debug]/
  databases/quran_content.db            RO, re-copied on content bump      (backup: exclude)
  databases/user.db                     RW, TRUNCATE journal               (backup: INCLUDE)
  databases/downloads.db                download queue + verified audio index (doc 06)  (exclude)
  files/datastore/settings.json         typed DataStore                    (backup: INCLUDE)
  files/datastore/playback_resume.json  listening pointer (doc 06)         (exclude)
  files/packs/<type>/<id>/<ver>/pack.db (+ font.ttf for font packs)        (exclude)
  files/packs/.staging/                 install scratch, wiped on start    (exclude)
  files/remote/<channel>/*.json|*.sig|*.etag, files/remote/feeds/…  verified signed docs  (exclude)
  files/logs/app-0.log, app-1.log       256 KB ring                        (exclude)
  cache/media3/                         SimpleCache LRU                    (exclude, OS-clearable)
  cache/share/                          ayah images, diagnostics zip (FileProvider)
<getExternalFilesDirs(null)[volumeIndex]>/audio/<recitationId>/  (+ .nomedia)  layout: doc 06
src/main/assets/      content/quran_content.db · fonts/*.ttf · licenses/*.txt · audio/timing/*.db.gz (public timing packs)
src/<tier>/assets/    packs/<type>/<id>@<ver>.db · audio/catalogue.json(+.sig) · remote-seed/index.json(+.sig)
                      · content/bundled.json (eligibility manifest, §11) · (personal only) audio/personal-overlay.json
res/raw/              adhan, takbeer and Sehri clips (doc 07 §2.5; from v1.1)
```

`res/xml/data_extraction_rules.xml` (API 31+). The same includes go in `backup_rules.xml` for API ≤ 30:

```xml
<data-extraction-rules>
  <cloud-backup>
    <include domain="database" path="user.db"/>
    <include domain="file" path="datastore/settings.json"/>
  </cloud-backup>
  <device-transfer>
    <include domain="database" path="user.db"/>
    <include domain="file" path="datastore/settings.json"/>
  </device-transfer>
</data-extraction-rules>
```

Including only these paths implicitly excludes everything else: content DB, packs, audio and caches. After a restore, `PackManager.ensureBundledInstalled()` runs and downloaded packs are re-offered from `installed_pack` rows whose files are missing (state `UPDATE_AVAILABLE`). Total backup size is well under the 25 MB Auto Backup cap (r08 §2.17).

---

## 8. Navigation 3

### 8.1 Structure

- **Navigation 3 1.2.0.** NiA's `NavigationState` + `Navigator` pattern gives one sub-stack per top-level destination. `NavDisplay` uses `rememberSaveableStateHolderNavEntryDecorator()` + `rememberViewModelStoreNavEntryDecorator()` (both verified in the spike).
- **Tablets.** `adaptive-navigation3` list-detail scenes for Index → Reader and Bookmarks (P3 two-page spread is a Reader concern, [05](05-mushaf-rendering-engine.md)).
- **Sheets and dialogs** (ayah action sheet, reciter quick-pick, repeat dialog, sleep timer, notice sheet) are **screen-local state, not NavKeys**. They use `BackHandler`, so predictive back works. The one exception is the Play sheet / scope picker (`PlaySheetKey`), a bottom-sheet scene reachable from many screens and from external play links ([02](02-ux-ui-spec.md) §4.7.1).
- **Top level.** The set, labels and order are owned by [02](02-ux-ui-spec.md). The architecture supports any subset of `{HomeKey, IndexKey, ListenKey, PrayerKey (P2), MoreKey}`.

### 8.2 NavKey list (`:core:navigation`, all `@Serializable`, so they survive process death)

```kotlin
// Top-level
@Serializable data object HomeKey : NavKey                                         // F-IX-08/09
@Serializable data class IndexKey(val tab: IndexTab = IndexTab.SURAH) : NavKey     // F-IX-01..06
enum class IndexTab { SURAH, PARA, MANZIL, RUKU, PAGE, SAJDA, BOOKMARKS }          // order and labels: doc 02 §4.3
@Serializable data object ListenKey : NavKey                                       // reciter-first browse (F-AU-01/02)
@Serializable data object MoreKey : NavKey
// Reading
@Serializable data class ReaderKey(
    val mushaf: MushafId? = null,           // null = settings.reader.mushaf
    val page: PageIndex? = null,            // null = resolve from focus or last read
    val focus: VerseKey? = null,            // highlight/scroll target
    val mode: ReaderMode = ReaderMode.MUSHAF,
    val autoplay: Boolean = false,          // honoured only for in-app/own-PendingIntent origins (§8.3)
) : NavKey                                                                         // F-RD-01..09
@Serializable data class GoToKey(val initial: String = "") : NavKey                // F-IX-05
@Serializable data class SearchKey(val query: String = "") : NavKey                // F-SR-01..03
// Audio
@Serializable data object PlayerKey : NavKey                                       // full player
@Serializable data class PlaySheetKey(val draft: PlayRequest) : NavKey            // bottom-sheet scene: scope/reciter/repeat (doc 02 §4.7.1)
@Serializable data class ReciterPickerKey(val forScope: PlayScope? = null) : NavKey
@Serializable data object DownloadsKey : NavKey                                    // F-AU-08 storage manager
// Library
@Serializable data object BookmarksKey : NavKey                                    // F-BM-02
@Serializable data class CollectionKey(val id: Long) : NavKey
@Serializable data object NotesKey : NavKey                                        // P2 F-BM-03
@Serializable data object KhatamListKey : NavKey                                   // P2 F-BM-04
@Serializable data class KhatamKey(val id: Long) : NavKey
@Serializable data object KhatamCreateKey : NavKey                                 // create plan (doc 02 §4.11)
@Serializable data object StatsKey : NavKey                                        // P2 F-BM-05
// Platform
@Serializable data object OnboardingKey : NavKey                                   // F-PL-01
@Serializable data class SettingsKey(val section: SettingsSection? = null) : NavKey // F-PL-02
@Serializable data object PacksKey : NavKey                                        // F-PL-06 storage (v1.0) + packs (v1.1)
@Serializable data object TranslationsKey : NavKey                                 // v1.1 F-TR-02/03 (doc 02 §4.24)
@Serializable data object CreditsKey : NavKey                                      // P2 F-PL-04 (incl. AboutLibraries)
@Serializable data class ReportErrorKey(val key: VerseKey?, val mushaf: MushafId?, val page: PageIndex?) : NavKey // P2 F-PL-05
@Serializable data object DiagnosticsKey : NavKey
// Companion (P2)
@Serializable data object PrayerKey : NavKey                                       // F-CP-01
@Serializable data object AdhanSettingsKey : NavKey
@Serializable data object AdhanHealthKey : NavKey                                  // F-CP-02
@Serializable data object CityPickerKey : NavKey
@Serializable data object QiblaKey : NavKey                                        // F-CP-03
@Serializable data object HijriKey : NavKey                                        // F-CP-04
@Serializable data object TasbeehKey : NavKey                                      // F-CP-05
@Serializable data class AzkarKey(val category: String? = null) : NavKey           // F-CP-06
@Serializable data object RamadanKey : NavKey                                      // F-CP-07
// P3 placeholders (not compiled until needed): HifzKey, GroupKhatamKey(id), SyncAccountKey
```

**Reader targets.** Doc 05's `ReaderTarget` concept maps onto `ReaderKey`, which is the only reader key:
- *Resume*: every field null. The reader opens `last_read` for the current mushaf.
- *Page(label)*: `page = MushafSpec.indexOf(label)`. Label ↔ index conversion happens only there.
- *Ayah / Para / Manzil / Ruku*: the caller resolves the target to a `focus` VerseKey through `IndexRepository` (Para → its start key, and so on). The reader then finds the page with `MushafRepository.pageOf`.

Each feature exposes `fun EntryProviderScope<NavKey>.readerEntries(navigator: Navigator)` (and so on). `:app` concatenates them in one `entryProvider { }`.

### 8.3 Deep links

`DeepLinkParser` (pure, unit-tested) maps a `Uri` to `DeepLinkResult(backStack: List<NavKey>, play: PlayRequest?)`, with `HomeKey` at the root of the back stack. `ReaderKey` carries no scope or recitation, so a `play` link returns its `PlayRequest` separately. `MainActivity` hands that request to `PendingPlayRequest`, a one-shot holder in `:core:navigation`, and pushes `ReaderKey(focus = first ayah of the scope, autoplay = true)`. Once visible, the reader calls `PlaybackController.play(pending ?: PlayRequest(PlayScope.FromHere(focus)))`. Before G3 the scheme is the brand-neutral `criterion://`. HTTPS App Links on the CDN domain (`/l/...`, same paths) follow in v1.x, together with `assetlinks.json` hosted on R2. Other docs use these URIs, not their own (for example doc 07's `criterion://reader?page=` becomes `criterion://page/{mushafCode}/{label}`).

| URI | Back stack | Used by |
|---|---|---|
| `criterion://page/{mushafCode}/{label}` | Home → Reader(mushaf, indexOf(label)) | share "page 50", widgets |
| `criterion://ayah/{surah}/{ayah}` | Home → Reader(focus) | share text footer, notes |
| `criterion://surah/{n}` · `/para/{n}` · `/manzil/{n}` · `/ruku/{id}` | Home → Index(tab) → Reader(start page) | reminders, widgets |
| `criterion://resume` | Home → Reader() (resume last read) | F-PL-12 "Continue reading" shortcut, widget |
| `criterion://play?scope=surah:18` (`para:23`, `manzil:7`, `ruku:{id}`, `page:{mushafCode}/{label}`, `range:2:285-2:286`, `ayah:2:255`, `from:18:1`, `passage:manzil-dua`, `quran`, `resume`)`&rec={recitationId}` | Own origin: Home → Reader(focus, autoplay) + `PendingPlayRequest`. External origin: Home → Reader(focus) paused + `PlaySheetKey(request)` prefilled | Kahf/Mulk reminder "Play" action (F-CP-09); F-PL-12 "Play favourite qari" (`scope=resume`: the `PlaybackResume` plan with the favourite reciter) |
| `criterion://khatam/{id}` | Home → KhatamList → Khatam(id) | khatam reminder |
| `criterion://prayer` · `/hijri` · `/ramadan` · `/qibla` (v1.1) · `/adhan-health` (v1.1) · `/tasbeeh` · `/azkar/{category}` (v1.1) | Home → X | widgets, adhan notification, F-PL-12 shortcuts |
| `criterion://search?q={text}` · `criterion://settings/{section}` | Home → Search(query) / Settings(section) | adb, shortcuts, support replies |

**Autoplay safety (Android 17, D-13).** `autoplay=true` is honoured only when the intent is our own explicit `PendingIntent.getActivity(... FLAG_IMMUTABLE)` or our own launcher shortcut, marked with an internal extra. External `VIEW` intents open the reader **paused**, with the Play sheet prefilled from the parsed request. Notification "Play" actions open the activity, so playback starts from a visible activity. Reminders and widgets start playback only through this activity deep link; we never use broadcast trampolines, and adhan never uses the recitation session.

---

## 9. Dependency injection (Hilt 2.60.1 + androidx.hilt 1.4.0, KSP)

| Component | Module (where) | Provides |
|---|---|---|
| `SingletonComponent` | `DispatchersModule` (`:core:data`) | `AppDispatchers(Dispatchers.IO, Dispatchers.Default, Dispatchers.Main.immediate)`, `AppScope`, `Clock` |
| | `ContentModule` (`:core:content`, internal) | `QuranContentDb`, DAOs, `PackDbOpener`, `ContentDataSource` |
| | `DatabaseModule` (`:core:database`) | `UserDb` (TRUNCATE journal, migrations), DAOs, `DataStore<UserSettings>` |
| | `RemoteModule` (`:core:remote`) | the one `OkHttpClient` (shared by Media3 and downloads; UA `Criterion/<versionName>`; `Cache(cacheDir/http, 10 MB)` for JSON), `Json`, `SignedFileFetcher`, `Ed25519Verifier`, `DownloadManager` |
| | `DataModule` (`:core:data`, `@Binds`) | `ContentRepository`, `MushafRepository`, `IndexRepository`, `TranslationRepository`, `SearchRepository`, `BookmarkRepository`, `NoteRepository`, `ReadingProgressRepository`, `KhatamRepository`, `TasbeehRepository`, `AzkarRepository`, `PackManager`, `SettingsRepository`, `BackupManager`, `AudioCatalogRepository`, `ContentFilter`, `DiagnosticsLog` |
| | `AudioModule` (`:core:audio`) | `SimpleCache` (singleton; `LeastRecentlyUsedCacheEvictor(cacheMb)`), `PlaybackStateHub` → `PlaybackStateHubImpl` (`@Singleton`), `PlaybackController` (its `state` is the hub's `state`), `DataStore<PlaybackResume>` |
| | `PrayerModule` (`:core:prayer`, P2) | `PrayerTimesRepository`, `LocationProvider`, `HijriCalendarProvider`, `QiblaSensor`, `AlarmScheduler`, `ReminderScheduler`, `NotificationChannels` |
| | `FlavorModule` (`:app/src/{play,foss}`) | `InAppReview` (play: Play review API, F-PL-10, v1.0-stretch/v1.1; foss: no-op), `CrashReporter` (v1.1 ACRA → crash Worker, both flavors, opt-in), `SupportPurchases` (v1.5 play only; foss: absent) |
| | `BuildInfoModule` (`:app`) | `BuildInfo(distribution, contentTier: ContentTier, monetized, channel, versionCode, cdnBaseUrl, trustedKeys)` from `BuildConfig` |
| `ViewModelComponent` | `@HiltViewModel` (assisted with the NavKey) | — |
| `ServiceComponent` | `@AndroidEntryPoint QuranPlaybackService`, `AdhanPlaybackService`, `AudioDownloadJobService` | field injection |
| Receivers | `@AndroidEntryPoint AdhanAlarmReceiver`, `SystemEventsReceiver`, `ReminderReceiver` | — |
| Workers | `@HiltWorker` + `HiltWorkerFactory` via `CriterionApp : Configuration.Provider`. The default `WorkManagerInitializer` is removed (spike-verified) | `DownloadWorker`, `RemoteRefreshWorker`, `DailyMaintenanceWorker` |
| Glance (P2) | `EntryPointAccessors.fromApplication(ctx, WidgetEntryPoint::class.java)` | repositories for widgets |

**Tests.** `@TestInstallIn(components = [SingletonComponent::class], replaces = [DataModule::class])` binds the `:core:testing` fakes. Robolectric Hilt tests follow the spike pattern.

---

## 10. Background work, services and the manifest

### 10.1 Background work map

| Job | Mechanism | Trigger | Constraints / FGS type | Phase |
|---|---|---|---|---|
| Recitation playback | `QuranPlaybackService : MediaLibraryService` (Media3) | User action only | FGS `mediaPlayback` | P1 |
| Audio and pack downloads, P1 (every API) | `DownloadWorker` (WorkManager long-running, `setForeground(ForegroundInfo(…, FOREGROUND_SERVICE_TYPE_DATA_SYNC))`). Pack downloads use unique work `dl-<taskId>`. **Exception:** audio uses one unique drain worker `audio-downloads` (`ExistingWorkPolicy.KEEP`) that drains the `downloads.db` queue, with units de-duplicated across requests (doc 06 §13) | User taps Download | Network constraint as below; FGS `dataSync` | P1 |
| Audio and pack downloads, API 34+ from S5 | `AudioDownloadJobService` (UIDT, job ID 4201: `setUserInitiated(true)`, `setEstimatedNetworkBytes`, `setNotification`), scheduled **while the UI is visible**; not rescheduled while pending or draining (doc 06 §13.5). API ≤ 33 keeps `DownloadWorker` | User taps Download | `NETWORK_TYPE_UNMETERED` if Wi-Fi-only, else ANY; no FGS | S5 (P2), before the first BETA build |
| Signed-file refresh (index, catalogue, notices, hijri-pk) | `RemoteRefreshWorker` periodic 24 h (flex 6 h), plus on app foreground if older than 24 h | Periodic, plus `ProcessLifecycleOwner` ON_START | `CONNECTED`, battery not low; ETag makes it about 1 KB when nothing changed | P1 (hijri feed P2) |
| Auto-update of packs (`autoUpdate`) and timing packs | `RemoteRefreshWorker` enqueues `DownloadRequest.Pack` | After the index changes | Wi-Fi only. On API 34+ it waits for the next app-visible moment, because UIDT must be scheduled while visible; on ≤ 33 it runs immediately | P1 |
| Daily maintenance (reschedule alarms safety net, refresh ayah-of-the-day widget, purge soft-deleted rows > 30 d, trim `adhan_log` to 200, wipe `.staging` and `.part` older than 7 d) | `DailyMaintenanceWorker`, periodic 24 h near 00:30 local | Periodic | none | P1 (alarm part with the first alarm feature, v1.1) |
| Adhan and prayer notifications | `AlarmManager.setAlarmClock()` (audible) / `setExactAndAllowWhileIdle()` (silent, pre-reminders), with inexact `setAndAllowWhileIdle()` fallback when exact is denied → `AdhanAlarmReceiver` | Computed schedule (today + tomorrow) | — | v1.1 (v1.0 only if the freelancer lever pulls adhan in) |
| Full adhan audio | `AdhanPlaybackService`, started from the exact-alarm receiver; `AudioAttributes.USAGE_ALARM`; checks ringer mode and interruption filter itself (D-16); never uses the recitation session | Alarm | FGS `mediaPlayback` | v1.1 |
| Short adhan | Notification channel sound `adhan_short_<soundId>` | Alarm | — | v1.1 |
| Quran reminders (Kahf Friday, Mulk night, daily ayah, khatam) | `AlarmManager.setWindow()` inexact → `ReminderReceiver` posts a notification with an Open/Play `PendingIntent.getActivity` (never autoplay from a broadcast) | `ReminderScheduler` | at most 3 per day, quiet hours 22:30–05:00 (F-CP-09) | v1.1 |
| Re-arm after system events | `SystemEventsReceiver`: `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET`, `TIMEZONE_CHANGED`, `AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` → `AlarmScheduler.rescheduleAll(reason)` + `ReminderScheduler` | Broadcast | Enabled only while any alarm or reminder is configured (`setComponentEnabledSetting`); never starts an FGS from boot | v1.1 |
| Widgets | Glance `updateAll()` on data change (last read, prayer roll-over via the alarm receiver) and from the daily worker | Event | — | v1.1 (F-CP-08) |

**v1.0 prayer times are display-only** (F-CP-01, F-CP-04, F-CP-07 timetable): they are computed on demand and need no alarm, receiver or permission. The alarm rows above, their receivers and the alarm permissions in §10.2 are declared only from the first release that ships an alarm feature (adhan, Sehri alarm or reminders): v1.1 by default, v1.0 only if the 2026-11-30 freelancer lever pulls adhan in.

**Notification channels**
- Media3 default channel for playback;
- `downloads` (LOW);
- from v1.1 (first alarm release): `adhan_full` (HIGH, no sound); `adhan_short_<soundId>` (HIGH, sound; recreated when the sound changes); `prayer_silent` (DEFAULT); `pre_prayer` (DEFAULT); `quran_reminders` (DEFAULT); `sehri_alarm` (HIGH, `setAlarmClock()`, **no full-screen intent**, D-16);
- `notices` is not a channel: F-PL-11 notices never post notifications.

### 10.2 AndroidManifest permissions (merged result for `:app`)

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android" xmlns:tools="http://schemas.android.com/tools">
  <!-- Network: streaming, downloads, signed manifests -->
  <uses-permission android:name="android.permission.INTERNET"/>
  <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>         <!-- Wi-Fi-only constraint, offline UX (also merged by WorkManager) -->
  <!-- Notifications: downloads, adhan, reminders (runtime on 33+, asked in context) -->
  <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
  <!-- Foreground services -->
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK"/> <!-- recitation (+ full adhan from v1.1) -->
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC"/>      <!-- download worker (every API in P1; API ≤33 from S5) -->
  <uses-permission android:name="android.permission.RUN_USER_INITIATED_JOBS"/>           <!-- UIDT downloads, API 34+, from S5 -->
  <!-- Alarms: FROM v1.1 ONLY (first release with adhan / Sehri alarm / reminders); not in the v1.0 manifest -->
  <uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM"/>              <!-- v1.1; user-granted; explainer + inexact fallback -->
  <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>            <!-- v1.1; re-arm alarms; receiver disabled when unused -->
  <uses-permission android:name="android.permission.WAKE_LOCK"/>                         <!-- Media3 wake mode, WorkManager (merged) -->
  <uses-permission android:name="android.permission.VIBRATE"/>                           <!-- tasbeeh target pattern, silent-adhan vibration -->
  <!-- Location (v1.0, prayer times): one-shot, coarse only -->
  <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
  <!-- Defensive removals (never request; strip if a dependency merges them) -->
  <uses-permission android:name="com.google.android.gms.permission.AD_ID" tools:node="remove"/>
  <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" tools:node="remove"/>
  <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" tools:node="remove"/>
  <uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" tools:node="remove"/>
  <uses-permission android:name="android.permission.USE_EXACT_ALARM" tools:node="remove"/>
  <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" tools:node="remove"/>
  <uses-permission android:name="android.permission.ACCESS_NOTIFICATION_POLICY" tools:node="remove"/>
  <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" tools:node="remove"/>
  <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" tools:node="remove"/>
  <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" tools:node="remove"/>

  <application android:name=".CriterionApp" android:supportsRtl="true" android:allowBackup="true"
      android:dataExtractionRules="@xml/data_extraction_rules" android:fullBackupContent="@xml/backup_rules"
      android:networkSecurityConfig="@xml/network_security_config" android:usesCleartextTraffic="false"
      android:theme="@style/Theme.Criterion.Splash" android:icon="@mipmap/ic_launcher" android:label="@string/app_name">
    <activity android:name=".MainActivity" android:exported="true" android:launchMode="singleTask"
        android:windowSoftInputMode="adjustResize">
      <intent-filter><action android:name="android.intent.action.MAIN"/><category android:name="android.intent.category.LAUNCHER"/></intent-filter>
      <intent-filter><action android:name="android.intent.action.VIEW"/><category android:name="android.intent.category.DEFAULT"/>
        <category android:name="android.intent.category.BROWSABLE"/><data android:scheme="criterion"/></intent-filter>
    </activity>
    <!-- :core:audio -->
    <service android:name="com.chughtai.criterion.core.audio.QuranPlaybackService" android:exported="true"
        android:foregroundServiceType="mediaPlayback">
      <intent-filter><action android:name="androidx.media3.session.MediaLibraryService"/>
                     <action android:name="android.media.browse.MediaBrowserService"/></intent-filter>
    </service>
    <!-- Bluetooth/headset "play" after process death → onPlaybackResumption (doc 06 §12) -->
    <receiver android:name="androidx.media3.session.MediaButtonReceiver" android:exported="true">
      <intent-filter><action android:name="android.intent.action.MEDIA_BUTTON"/></intent-filter>
    </receiver>
    <!-- :core:remote (UIDT job service from S5) -->
    <service android:name="com.chughtai.criterion.core.remote.AudioDownloadJobService" android:exported="false"
        android:permission="android.permission.BIND_JOB_SERVICE"/>
    <service android:name="androidx.work.impl.foreground.SystemForegroundService"
        android:foregroundServiceType="dataSync" tools:node="merge"/>
    <provider android:name="androidx.startup.InitializationProvider" android:authorities="${applicationId}.androidx-startup"
        android:exported="false" tools:node="merge">
      <meta-data android:name="androidx.work.WorkManagerInitializer" android:value="androidx.startup" tools:node="remove"/>
    </provider>
    <!-- :core:prayer: alarm components FROM v1.1 ONLY (absent from the v1.0 manifest) -->
    <service android:name="com.chughtai.criterion.core.prayer.AdhanPlaybackService" android:exported="false"
        android:foregroundServiceType="mediaPlayback"/>
    <receiver android:name="com.chughtai.criterion.core.prayer.AdhanAlarmReceiver" android:exported="false"/>
    <receiver android:name="com.chughtai.criterion.core.prayer.ReminderReceiver" android:exported="false"/>
    <receiver android:name="com.chughtai.criterion.core.prayer.SystemEventsReceiver" android:exported="true" android:enabled="false">
      <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED"/>
        <action android:name="android.intent.action.MY_PACKAGE_REPLACED"/>
        <action android:name="android.intent.action.TIME_SET"/>
        <action android:name="android.intent.action.TIMEZONE_CHANGED"/>
        <action android:name="android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"/>
      </intent-filter>
    </receiver>
    <!-- per-app language on API ≤ 32 (AppCompat) -->
    <service android:name="androidx.appcompat.app.AppLocalesMetadataHolderService" android:enabled="false" android:exported="false">
      <meta-data android:name="autoStoreLocales" android:value="true"/>
    </service>
    <!-- sharing ayah images / diagnostics zip -->
    <provider android:name="androidx.core.content.FileProvider" android:authorities="${applicationId}.files"
        android:exported="false" android:grantUriPermissions="true">
      <meta-data android:name="android.support.FILE_PROVIDER_PATHS" android:resource="@xml/file_paths"/>
    </provider>
    <!-- Glance receivers (v1.1, F-CP-08): ContinueReadingWidgetReceiver, NextPrayerWidgetReceiver, AyahOfDayWidgetReceiver (exported=true, APPWIDGET_UPDATE) -->
    <!-- Launcher shortcuts (F-PL-12, v1.1): <meta-data android:name="android.app.shortcuts" android:resource="@xml/shortcuts"/> on MainActivity -->
  </application>
</manifest>
```

`SystemEventsReceiver` is exported because system broadcasts must reach it. Its handler only calls the idempotent `rescheduleAll`, so a spoofed broadcast is harmless.

**Release staging.** The v1.0 manifest is the one above without `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`, `AdhanPlaybackService`, the three `:core:prayer` receivers and the widget receivers. They are added from the first release that ships an alarm feature (v1.1 by default). CI's `verifyMergedManifest` (doc 10) compares the merged manifest of each release against the expected list for that release.

**Permissions added later, behind their features:**

| Permission | When | Notes |
|---|---|---|
| `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED` (+ `AdhanPlaybackService`, alarm receivers) | v1.1: first release with adhan, Sehri alarm or reminders | v1.0 prayer times are display-only |
| `com.android.vending.BILLING` | v1.5, play flavor only (F-PL-08) | — |
| `RECORD_AUDIO` | P3 hifz "record yourself" (F-CP-12), P4 AI recitation | Runtime permission, requested on first use |
| (none) | Android Auto (F-AU-11) | Needs `automotive_app_desc.xml` meta-data only |

**Never requested** (each with its reason):

| Permission | Reason |
|---|---|
| `USE_EXACT_ALARM` | Play restricts it to alarm and calendar apps (D-16) |
| `ACCESS_NOTIFICATION_POLICY` | We never flip DND or the ringer (D-16; the reference app's top complaint) |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Play policy. We deep-link to settings instead (D-16) |
| `USE_FULL_SCREEN_INTENT` | Not in v1 (D-16) |
| `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION` | Coarse only (D-15) |
| Storage and `READ_MEDIA_*` | App-specific dirs plus SAF |
| `AD_ID` | No tracking (D-20) |
| `QUERY_ALL_PACKAGES` and any `<queries>` block | OEM settings intents use try/catch |
| `READ_PHONE_STATE`, `CAMERA`, contacts, `SYSTEM_ALERT_WINDOW` | Never needed |
| `FOREGROUND_SERVICE_SPECIAL_USE`, `_SYSTEM_EXEMPTED`, `_LOCATION` | Never needed |

**Play Console declarations** ([11](11-release-compliance-business.md)): FGS `mediaPlayback` (recitation; adhan from v1.1) and `dataSync` (downloads) with videos; exact alarm (user-granted, from v1.1); Data safety "No data collected" (location is processed on-device only). The v1.1 opt-in crash reports must be reflected in Data safety for that release (doc 11).

---

## 11. Build variants and content eligibility

### 11.1 Variants and `CONTENT_TIER`

The flavor dimension `tier` says **which source set** is packaged. `BuildConfig.CONTENT_TIER` says **which content rules** apply. They differ only for the public tier, which builds as BETA (closed testing) unless the release is explicitly marked production.

| Flavor `tier` | Gradle properties | `CONTENT_TIER` | Use |
|---|---|---|---|
| `personal` | (`criterion.release` must be unset) | `PERSONAL` | Owner's daily build (`fossPersonalRelease`, sideloaded), family |
| `public` | default | `BETA` | Internal and closed testing. Shows the "proofreading in progress" notice (Home card + reader-chrome chip, never over Quran text) until G5 |
| `public` | `-Pcriterion.release=production` | `PUBLIC` | Production track. `verifyContentEligibility` also demands G1 and G5 evidence |

`BuildConfig.MONETIZED` is `true` only with `-Pcriterion.monetized=true`, which is allowed only for `play` + `public` from v1.5 (F-PL-08). `verifyContentEligibility` fails any other variant built with it, and any personal-tier variant built with `-Pcriterion.release=production`.

```kotlin
// build-logic: criterion.android.application.flavors
val release = providers.gradleProperty("criterion.release").orNull          // null | "production"
val monetized = providers.gradleProperty("criterion.monetized").orNull == "true"
require(release == null || release == "production") { "criterion.release must be 'production' or unset" }

android {
    flavorDimensions += listOf("distribution", "tier")
    productFlavors {
        create("play")     { dimension = "distribution"; buildConfigField("String", "DISTRIBUTION", "\"play\"") }
        create("foss")     { dimension = "distribution"; buildConfigField("String", "DISTRIBUTION", "\"foss\"") }
        create("public")   { dimension = "tier"
                             buildConfigField("String", "CONTENT_TIER", if (release == "production") "\"PUBLIC\"" else "\"BETA\"")
                             buildConfigField("String", "REMOTE_CHANNEL", if (release == "production") "\"public\"" else "\"beta\"") }
        create("personal") { dimension = "tier"; applicationIdSuffix = ".personal"; versionNameSuffix = "-personal"
                             resValue("string", "app_name_suffix", " (personal)")
                             buildConfigField("String", "CONTENT_TIER", "\"PERSONAL\"")
                             buildConfigField("String", "REMOTE_CHANNEL", "\"beta\"") }   // + bundled personal-overlay.json
    }
    buildTypes {
        debug { applicationIdSuffix = ".debug" }
        release { optimization { enable = true } }        // AGP 9.3+ DSL; R8 full mode
    }
    defaultConfig {
        buildConfigField("boolean", "MONETIZED", monetized.toString())   // honoured only for playPublic*; see below
        buildConfigField("String", "CDN_BASE_URL", "\"${providers.gradleProperty("criterion.cdn").getOrElse("https://cdn.example.invalid")}\"")
        buildConfigField("String", "TRUSTED_KEYS", "\"${providers.gradleProperty("criterion.trustedKeys").get()}\"") // 2 hex keys, comma-sep
    }
    androidResources { generateLocaleConfig = true }      // + res/resources.properties unqualifiedResLocale=en-US
    bundle { language { enableSplit = false } }           // in-app language switch (D-21)
}
androidComponents {
    beforeVariants(selector().withFlavor("distribution" to "play").withFlavor("tier" to "personal")) { it.enable = false }
    onVariants(selector().withBuildType("release")) { it.packaging.resources.excludes.add("DebugProbesKt.bin") } // release only (D-02)
    // verifyContentEligibility<Variant> (criterion.content.eligibility) fails a personal-tier variant when release != null,
    // and any non-playPublic variant when monetized == true, so misuse fails the build, not just configuration.
}
```

`enum class ContentTier { PERSONAL, BETA, PUBLIC }` lives in `:core:model`. `BuildInfo` maps the `BuildConfig` strings to it once, in `:app`.

| Variant | Goes to | `CONTENT_TIER` | Content allowed (§11.2) |
|---|---|---|---|
| `playPublicRelease` | Play internal / closed testing | BETA | Items with BETA in `eligibleTiers`. Mushaf layout only if G1-cleared or clean-room. PENDING recitations allowed |
| `playPublicRelease -Pcriterion.release=production` | Play production (staged rollout) | PUBLIC | Items with PUBLIC in `eligibleTiers`; no PENDING or PERSONAL_ONLY; G1 + G5 evidence; with `MONETIZED` (v1.5+) also `commercialOk` |
| `fossPublicRelease` | Optional APK / F-Droid later (G4) | BETA or PUBLIC (same rule) | As above, plus `fossOk`; never monetized |
| `fossPersonalRelease` | Owner's phone (sideload), `com.chughtai.criterion.personal`; built on the owner's machine only (`personalCheck`) | PERSONAL | Everything with PERSONAL in `eligibleTiers`, including PENDING and PERSONAL_ONLY items (QUL-derived layout before G1, Shamshad Ali Khan voice, Husary Muallim, Qari Sadaqat Ali, gapped everyayah, D-12) |
| `playPersonal*` | Disabled | — | — |
| `*Debug` | Developer devices | Same as the tier | Same as the tier |

### 11.2 Where eligibility lives

**Per-item fields (one vocabulary for packs, catalogue recitations, bundled items and datasets; D-29).**

| Field | Values | Meaning |
|---|---|---|
| `licenceStatus` | `PERMITTED`, `HOST_GRANT`, `PENDING`, `PERSONAL_ONLY`, `REVOKED` | PERMITTED = licence or written permission covers us. HOST_GRANT = the host's general grant (for example mp3quran). PENDING = asked, no answer yet. PERSONAL_ONLY = may never leave the owner's devices. REVOKED = withdrawn |
| `basis` | free text | Why the status holds: licence name, permission record (PERM-xx, doc 03), "clean-room" |
| `eligibleTiers` | subset of `PERSONAL`, `BETA`, `PUBLIC` | Tiers the pipeline cleared the item for |
| `commercialOk` | boolean | Required for MONETIZED builds. False for every Tanzil translation |
| `fossOk` | boolean | Required for `fossPublic` builds |
| `enabled` | boolean | Kill-switch (remote index or a `notices.json` `disable`) |

**One rule, shared by CI and the device.** `ContentPolicy` lives in `:core:model` as a pure function, so `verifyContentEligibility`, the pipeline's `publish.py` tests (via the shared JSON vectors) and every repository apply the same logic:

```kotlin
fun allows(item: Eligibility, tier: ContentTier, monetized: Boolean, foss: Boolean, type: ItemType): Boolean =
    item.enabled &&
    tier in item.eligibleTiers &&
    when (item.licenceStatus) {
        REVOKED       -> false
        PERSONAL_ONLY -> tier == PERSONAL
        PENDING       -> tier != PUBLIC && !(type == MUSHAF_LAYOUT && tier == BETA)   // PENDING audio is fine in BETA
        PERMITTED, HOST_GRANT -> true
    } &&
    (type != MUSHAF_LAYOUT || tier == PERSONAL || item.licenceStatus == PERMITTED || item.basis == "clean-room") &&
    (!monetized || item.commercialOk) &&
    (!(foss && tier != PERSONAL) || item.fossOk)
```

1. **Pipeline outputs** carry these fields on every item: packs (`index.json`, `pack_info`), catalogue recitations ([06](06-audio-engine.md)), datasets inside the content DB (`source.licence_json`), and the bundled layout pack.
2. **Bundled content** is split by source set:
   - `src/main/assets/`: the content DB, fonts, licence texts and the public timing packs. These must be eligible in every tier.
   - `src/public/assets/` and `src/personal/assets/`: `packs/`, `audio/catalogue.json` and `content/bundled.json`. `bundled.json` lists every bundled file (main and tier) as `{id, type, path, sha256, licenceStatus, basis, eligibleTiers, commercialOk, fossOk}`.
   - **`app/src/personal/assets/packs/` is gitignored.** QUL-derived (G1-pending) packs and any personal-only content are never committed to git or LFS. A Gradle task `copyPersonalAssets` (in `criterion.android.application.flavors`, wired before `mergeFossPersonal*Assets`) copies them from `~/criterion-data` (or `data/private/`). Only public-eligible generated assets (the Tanzil-based content DB, clean-room or G1-cleared packs, public timing packs) may be committed, via Git LFS.
3. **Remote channels.** The pipeline publishes `personal` (never uploaded to R2), `beta` and `public`. Each uploaded channel has its own signed `index.json`, `catalogue.json` and `notices.json` under `${CDN_BASE_URL}/v1/<channel>/`, listing only items whose `eligibleTiers` contain that tier. PUBLIC builds read `public`. BETA and PERSONAL builds read `beta`. Personal-only items are either bundled into personal APKs or fetched from their original host URLs listed in `src/personal/assets/audio/personal-overlay.json`. Monetized builds read `public` and filter on `commercialOk` on the device.
4. **Runtime.** Every repository filters through `ContentPolicy.allows(…)` with `BuildInfo.contentTier`, `monetized` and `distribution`. Kill-switches (`enabled = false`, plus notices) override everything.

### 11.3 CI enforcement (`verifyContentEligibility`)

**`verifyContentEligibility<Variant>`** comes from the `criterion.content.eligibility` plugin and is wired before `merge<Variant>Assets`. It parses the variant's merged `bundled.json` and `audio/catalogue.json` and recomputes the sha256 of each bundled file. It **fails** if:
- any bundled item or default-catalogue recitation fails `ContentPolicy.allows` for the variant's `CONTENT_TIER`, `MONETIZED` and distribution (the same function as on the device);
- a file exists under `assets/packs` or `assets/audio/timing` that is not listed, or a hash mismatches;
- a BETA or PUBLIC variant has no `mushaf_layout` pack, or its layout pack is neither G1-cleared (`licenceStatus = PERMITTED` and `data/legal/permissions.yml` → `qul_res_11: granted` with archived evidence, doc 03/08) nor `basis = "clean-room"`;
- **PUBLIC only (G5):** the layout pack's `pack_info.proof_certificate_id` is NULL, or any page's text-layer hash (`page.page_hash`: word positions + glyph runs, doc 05) is missing from or differs from `certified_pages.json` for that certificate. The certificate binds the text layer, not the ornament layer;
- **PUBLIC only:** any `para_quarter.verified_source` is NULL in the bundled content DB (§7.2.1).

Other gates:
- **`release.yml`** builds the production AAB only after this task passes for `playPublicRelease -Pcriterion.release=production`. It runs `apkanalyzer`/`aapt2 dump` to assert that no `src/personal` asset path is present in any public-tier artifact.
- **The pipeline's `publish.py`** refuses to upload to a channel any item whose `eligibleTiers` lack that channel's tier, and never uploads the `personal` channel ([08](08-data-pipelines.md)).
- **G1 linkage.** Until G1 is granted, `src/public/assets/packs/mushaf_layout/` contains either the clean-room layout pack (D-06 fallback) or nothing. Closed testers are third parties, so the BETA build is held to the same layout rule. This is how "go public only if gates cleared" (D-25) becomes mechanical.

## 12. Security

| Threat | Control |
|---|---|
| Tampered manifests or packs (CDN compromise, MITM) | Ed25519 detached signature on every JSON doc (two pinned keys); sha256 of every pack and timing file before install; anti-rollback `sequence` (same sequence must mean same bytes); `expiresAt`; channel check; schema-version gate; `PRAGMA quick_check`. The remote plane can disable or annotate content but never changes Quran text or layout (D-30) |
| Malicious ZIP | Zip-slip guard, entry-count and size caps, staging dir, atomic rename |
| Secrets in the APK | **None exist.** All endpoints are public and read-only. R2 write tokens live only in GitHub Actions secrets / the owner's machine; signing keys are offline. The QF client (P3) is a PKCE public client with no secret (D-20). `google-services.json` (P3, play flavor) is not secret |
| Transport | HTTPS only (`usesCleartextTraffic=false` plus a network-security-config `cleartextTrafficPermitted="false"`); system trust store. **No certificate pinning.** Content integrity comes from signatures, and pinning third-party CDNs would break on rotation |
| Exported components | `MainActivity` (launcher + `criterion://`), `QuranPlaybackService` (Media3 validates controllers; `onConnect` allows only own UI, system UI, and in v1.x Auto/Wear), `MediaButtonReceiver`, Glance receivers (v1.1), and the idempotent `SystemEventsReceiver` (v1.1). Everything else is `exported=false`. Every PendingIntent uses `FLAG_IMMUTABLE` |
| Deep-link abuse | Pure parser with range validation; never autoplay from external intents; no file or URL loading from links |
| Privacy leakage | No SDKs; no device IDs in UA or requests; location rounded to 0.01° and stored only in settings; logs contain no ayah-level reading history; diagnostics are shared only through a user-initiated share sheet showing the zip contents (D-20) |
| Supply chain | Version catalogue pins; Gradle dependency verification (`gradle/verification-metadata.xml`, sha256) enabled in P1; the `dependency-analysis` plugin |

---

## 13. Performance architecture

**Budgets.** These are CI or nightly macrobenchmark targets. Mushaf render budgets are owned by [05](05-mushaf-rendering-engine.md).

| Metric | Target | Measured by |
|---|---|---|
| Cold start to Home TTID (Pixel 6a class) | ≤ 500 ms (r07 §2.7) | `StartupBenchmark` (`CompilationMode.Partial(BaselineProfile)`) |
| Cold start to Home TTID (mid-range) | ≤ 1,200 ms | Device lab (doc 10) |
| Cold start to Home TTID (low-end, 3 GB, API 24-class, Infinix/Tecno tier) | ≤ 1,500 ms | Manual on the device lab (doc 10 QB) |
| Page-turn frame time p90 | ≤ 16 ms | `PageTurnBenchmark` `FrameTimingMetric` |
| Search, first results page (unicode61, 3 columns) | < 150 ms on mid-range | JVM benchmark + trace |
| First-run asset copy (content DB + bundled packs) | < 2 s total, during onboarding | Trace section `content.copy` |
| Download size (playPublic) | ≤ 20 MB at P1, ≤ 25 MB at v1.0; CI fails at budget + 10 % | `bundletool get-size total` |
| Total PSS in the reader | ≤ 150 MB (doc 01 NFR) | `dumpsys meminfo` in the nightly macrobenchmark |
| Java heap steady state in the reader | < 120 MB on 2 GB devices | LeakCanary (debug) + Memory profiler |

**Startup path.**
1. `CriterionApp.onCreate`: Hilt injection only. StrictMode is debug-only. No eager DB, network or WorkManager work: WorkManager initializes on demand through `Configuration.Provider`.
2. `MainActivity`: `installSplashScreen()` with `setKeepOnScreenCondition { !startupState.ready }`, capped at 800 ms. Then `enableEdgeToEdge()`.
3. `StartupViewModel` reads `UserSettings` (DataStore, about 1 KB) and `last_read`, and decides the route: onboarding, or home with the continue-reading card.
4. After the first frame, `ContentWarmup` runs in `AppScope` on `io`:
   - open `QuranContentDb` (which triggers the asset copy on first run or after a content bump);
   - `PackManager.ensureBundledInstalled()`;
   - load the DK Typeface;
   - `MushafRepository.prefetch(lastRead ± 1)`;
   - schedule periodic workers if they are not already enqueued.
5. The `RemoteRefreshWorker` check runs on ON_START only if the last refresh is older than 24 h.

**Baseline profile journeys** (`:baselineprofile`): cold start → home → open last page → swipe 10 pages → open Para index → open Para 23 → play ayah → open search with «رحمن» → open translation view. Profiles are regenerated nightly. `profileinstaller` ships in the app.

**Prefetch and caches**
- `PageLayout` LRU of ±3 pages per mushaf, sized by `ActivityManager.memoryClass` (min 7, max 15 entries).
- Typefaces and `Paint` objects are singletons.
- Translation rows are paged per screen through `ayahs(range)`.
- Reciter catalogue and timings are cached per surah in memory (about 300 entries for Baqarah).
- FTS queries use `LIMIT 200` per page with `OFFSET`.

**Database discipline.** Every query uses a primary key, a range or one of the listed indexes. No query runs on the main thread (StrictMode catches it in debug).

**APK hygiene**
- R8 full mode.
- Vector-drawable icons only; no `material-icons-extended`.
- Exclude `DebugProbesKt.bin`.
- Ship an AAB with per-ABI natives (`libsqliteJni.so` is about 1.28 MB on arm64).
- The CI checks `zipalign -c -P 16` (16 KB pages, required from 2027-02-01).

---

## 14. Content-version and update flows

### 14.1 App update with a new content DB

```mermaid
sequenceDiagram
  participant App as App (new versionCode, CONTENT_DB_VERSION=N+1)
  participant Room as Room (content)
  participant PM as PackManager
  participant U as user.db
  App->>Room: first query
  Room->>Room: user_version N ≠ N+1, no migration → delete file, re-copy asset (copy-destructive)
  App->>PM: ensureBundledInstalled()
  PM->>PM: copy bundled packs whose (id,version) not installed; verify sha256 from bundled.json
  PM->>U: upsert installed_pack (source=BUNDLED)
  PM->>PM: for each DOWNLOADED pack: minContentVersion ≤ N+1 ≤ maxContentVersion ? keep : state=UPDATE_AVAILABLE (blocked from opening)
  PM-->>App: emit installed(); UI shows "Update 1 translation" (auto on Wi-Fi if autoUpdate)
  Note over U: user.db untouched — keys are VerseKey / (mushafId,pageIndex)
```

### 14.2 Pack updates and text errata (D-30)

The flow for downloadable packs (v1.1) is `RemoteRefreshWorker` → verified `index.json` → for each installed pack with a higher `version`, enqueue it (when `autoUpdate`) or mark `UPDATE_AVAILABLE` → install as in §7.6 → repositories invalidate their caches on `installed()` change.

**The remote plane never changes Quran text or layout (D-30).** It may only revoke (`enabled = false`), disable or annotate (`notices.json`). Concretely:
- `index.json` never offers a new version of an installed `mushaf_layout` pack, and the default layout pack is never auto-updated. The index may offer *additional* layouts (P3), each with its own certificate.
- `word_override` ([05](05-mushaf-rendering-engine.md)) is a **build-time errata record**. The pipeline applies it when it builds the pack, after hafiz sign-off (D-22). The app applies no text overlay at runtime.
- Every text or layout fix, in the content DB or the mushaf pack, ships as an **app update**.

**Text-error SLA** (doc 12; the bundled content makes a text fix an app update): triage ≤ 24 h; signed `notices.json` warn or disable for the affected pages ≤ 48 h; corrected build submitted ≤ 72 h; reporter acknowledged ≤ 72 h. The `minVersion` nudge then points users at the fixed build.

### 14.3 Kill-switches and notices (`notices.json`, signed; F-PL-11, v1.0 floor)

```json
{ "schema": 1, "kid": "k1", "sequence": 12, "generatedAt": "2026-12-20T10:00:00Z", "expiresAt": "2027-03-31T00:00:00Z",
  "channel": "public",
  "minVersion": { "versionCode": 1000299, "message": { "en": "This update fixes a text issue.", "ur": "…" } },
  "notices": [
    { "id": "n-2026-12-20-01", "target": { "type": "page", "mushaf": "indopak16_taj", "pages": [412] },
      "action": "warn", "message": { "en": "A possible text issue on this page is under review.", "ur": "…" },
      "until": "2027-01-31T00:00:00Z" },
    { "id": "n-…", "target": { "type": "page", "mushaf": "indopak16_taj", "pages": [412, 413] }, "action": "disable",
      "message": { "en": "These pages are being corrected.", "ur": "…" } },
    { "id": "n-…", "target": { "type": "surahHighlight", "recitation": "husary.murattal.mp3q118", "surah": 2 },
      "action": "disable" },
    { "id": "n-…", "target": { "type": "pack", "id": "ur.example" }, "action": "disable" },
    { "id": "n-…", "target": { "type": "recitation", "id": "…" }, "action": "disable" },
    { "id": "n-…", "target": { "type": "feature", "id": "autoPageTurn" }, "action": "disable" },
    { "id": "n-…", "target": { "type": "feature", "id": "renderFallback", "device": { "model": "SM-A105F", "sdk": [28, 30] } },
      "action": "enable", "level": "L1" } ] }
```

- `target.type` ∈ `page | surahHighlight | pack | recitation | feature`. `action` ∈ `warn | disable` (plus `enable` for opt-in device workarounds such as doc 05's renderer fallback trigger T6, whose `level` is `L1` or `L2`).
- Optional `target.device` (`model`, `manufacturer`, `sdk` range) limits a notice to matching devices. It is evaluated locally; nothing is sent.
- Top-level `minVersion` drives the "Update available" card when `BuildConfig.VERSION_CODE` is lower. It is never a blocking dialog.
- `until` is optional per notice. The file as a whole is governed by `expiresAt` (§7.6), but active kill-switches stay in force after expiry.

`PackManager.notices()` feeds the following. Notices appear **only** as a dismissible Home card, an info chip in the reader chrome and a notice sheet ([02](02-ux-ui-spec.md) §4.23). There is no banner, toast or overlay on Quran text, and no push notification.
- `page`/`warn`: chip on the target pages; the page is drawn unchanged.
- `page`/`disable`: the target pages show the ruled placeholder plus an inline card offering the translation view. Nothing is drawn over text.
- `AudioCatalogRepository.highlightEnabled` and `recitations()` drop disabled targets.
- `TranslationRepository.installed()` hides disabled packs.
- Feature flags are read by the ViewModels.

### 14.4 Layout switch and remap

Page bookmarks and `last_read` are stored per `mushaf_id`, with an anchor `VerseKey`. When the user changes the mushaf (P3), the reader opens at `MushafRepository.pageOf(newMushaf, anchor)`. Page bookmarks show under their own layout, with a "show in current layout" action that uses the anchor.

---

## 15. Logging and diagnostics (no SDKs)

- **`DiagnosticsLog`** is an in-memory ring of 500 lines plus two rolling files of 256 KB in `files/logs/`.
  - Levels: `d`, `i`, `w`, `e`. `d` is stripped from release builds by R8 `-assumenosideeffects`.
  - **What gets logged:** timestamps, component tags, error classes, HTTP status and host, download and pack lifecycle, alarm scheduling and lateness, and content and pack versions.
  - **Never logged:** queries, ayahs opened, bookmarks, or exact location.
- **Settings → About → "Share diagnostics"** (`DiagnosticsKey`) builds `cache/share/diagnostics-<ts>.zip` containing:
  - the logs;
  - `build.json` with variant, versionCode, content version and installed packs;
  - `device.json` with model, API level, ABI, locale, memoryClass and the exact-alarm/notification/battery-optimization states;
  - the last 50 rows of `adhan_log`.

  The screen lists the contents first. Sharing then goes through the system share sheet via FileProvider, sent by the user's own choice of email or WhatsApp.
- **Crashes and ANRs** come from Play Android vitals in v1.0. Opt-in ACRA follows in **v1.1** as F-PL-09 (D-20, D-32): after a crash, a dialog asks once per report; an accepted report is POSTed to the crash-report Cloudflare Worker (`${CDN_BASE_URL}/crash`), which stores it in R2 for 90 days. Reports carry the `build.json`/`device.json` fields below and the stack trace, never reading history, queries or location. This Worker is the only server-side code (§1). `Thread.setDefaultUncaughtExceptionHandler` chains to the default after writing the stack trace to the ring file, so a crash is included in the next diagnostics share.
- **Adhan health check** (F-CP-02) reads `AlarmScheduler.health()`: exact-alarm permission, notification permission, channel importance, battery-optimization state (read-only), OEM guidance key and recent lateness from `adhan_log`.
- **Tracing.** `androidx.tracing` sections (`content.copy`, `reader.layout`, `search.query`, `pack.install`) appear in macrobenchmark traces.

---

## 16. Build configuration

### 16.1 Proposed `gradle/libs.versions.toml`

This starts from the spike's verified catalogue (g04 §2.8) and adds r07 §2.12 entries. **Removed versus the spike:** Ktor (D-02) and Coil (no reciter photos, D-12). The Room 2.8.5 fallback stays commented. Every version was verified in r07 or g04 unless it is marked `# UNVERIFIED`.

```toml
[versions]
# Toolchain (D-02, spike-verified)
agp = "9.4.1"
kotlin = "2.4.21"                 # fallback 2.3.21; never 2.2.10 with adhan2 0.0.7
ksp = "2.3.12"
# AndroidX
androidxActivity = "1.13.0"
androidxAppCompat = "1.8.0"
androidxCore = "1.19.1"
androidxCoreSplashscreen = "1.2.0"
androidxComposeBom = "2026.09.00" # Compose 1.12.1, material3 1.4.0, adaptive 1.3.0
androidxLifecycle = "2.11.0"
androidxNavigation3 = "1.2.0"
androidxHilt = "1.4.0"
androidxRoom3 = "3.0.3"
androidxRoom = "2.8.5"            # fallback only (FTS4)
androidxSqlite = "2.7.1"
androidxDataStore = "1.2.1"
androidxMedia3 = "1.11.1"
androidxWork = "2.12.0"
androidxGlance = "1.2.0"
androidxStartup = "1.2.0"
androidxWindow = "1.5.1"
androidxProfileInstaller = "1.4.1"
androidxBenchmark = "1.5.0"       # also androidx.baselineprofile plugin
androidxTest = "1.7.0"
androidxTestExtJunit = "1.3.0"
androidxEspresso = "3.7.0"
androidxUiAutomator = "2.4.0"
# DI
hilt = "2.60.1"
# Kotlin libs
kotlinxCoroutines = "1.11.0"
kotlinxSerialization = "1.11.0"
kotlinxDatetime = "0.8.0"
kotlinxCollectionsImmutable = "0.5.2"
# Network
okhttp = "5.5.0"                  # everywhere incl. Media3 datasource (g04 §2.5)
# Other
desugarJdkLibs = "2.1.5"
adhan2 = "0.0.7"                  # MIT (g04 §2.6)
aboutLibraries = "15.3.0"
playReview = "2.0.2"              # play flavor only, F-PL-10 (v1.0-stretch/v1.1)
# Test
junit4 = "4.13.2"
truth = "1.4.5"
turbine = "1.2.1"
robolectric = "4.17"
roborazzi = "1.76.0"
mockk = "1.14.11"                 # use sparingly; fakes first
leakcanary = "2.14"
# Quality plugins
spotless = "8.10.4"
ktlint = "1.8.0"
composeRules = "0.6.8"
slackComposeLint = "1.6.1"
kover = "0.9.11"
dependencyAnalysis = "3.19.2"
foojay = "1.0.0"
# Later phases (pin when the feature starts)
# playBilling = "…"               # UNVERIFIED — v1.5 play flavor (F-PL-08)
# acra = "…"                      # UNVERIFIED — opt-in crash reports to the crash Worker (F-PL-09, v1.1, D-32)
# appAuth = "…"                   # UNVERIFIED — QF PKCE sync (F-BM-07)
# firebaseBom = "…"               # UNVERIFIED — play flavor group khatam (F-CP-11)

[libraries]
# build-logic (compileOnly in build-logic/convention)
android-gradlePlugin = { group = "com.android.tools.build", name = "gradle", version.ref = "agp" }
kotlin-gradlePlugin = { group = "org.jetbrains.kotlin", name = "kotlin-gradle-plugin", version.ref = "kotlin" }
compose-gradlePlugin = { group = "org.jetbrains.kotlin", name = "compose-compiler-gradle-plugin", version.ref = "kotlin" }
ksp-gradlePlugin = { group = "com.google.devtools.ksp", name = "symbol-processing-gradle-plugin", version.ref = "ksp" }
room3-gradlePlugin = { group = "androidx.room3", name = "room3-gradle-plugin", version.ref = "androidxRoom3" }
spotless-gradlePlugin = { group = "com.diffplug.spotless", name = "spotless-plugin-gradle", version.ref = "spotless" }
android-desugarJdkLibs = { group = "com.android.tools", name = "desugar_jdk_libs", version.ref = "desugarJdkLibs" }
# AndroidX core/UI
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "androidxCore" }
androidx-core-splashscreen = { group = "androidx.core", name = "core-splashscreen", version.ref = "androidxCoreSplashscreen" }
androidx-appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "androidxAppCompat" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "androidxActivity" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "androidxComposeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-compose-foundation = { group = "androidx.compose.foundation", name = "foundation" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-compose-material3-adaptive = { group = "androidx.compose.material3.adaptive", name = "adaptive" }
androidx-compose-material3-adaptive-layout = { group = "androidx.compose.material3.adaptive", name = "adaptive-layout" }
androidx-compose-material3-adaptive-navigation3 = { group = "androidx.compose.material3.adaptive", name = "adaptive-navigation3" }
androidx-compose-material3-navigationSuite = { group = "androidx.compose.material3", name = "material3-adaptive-navigation-suite" }
androidx-window = { group = "androidx.window", name = "window", version.ref = "androidxWindow" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "androidxLifecycle" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "androidxLifecycle" }
androidx-lifecycle-viewmodel-navigation3 = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-navigation3", version.ref = "androidxLifecycle" }
androidx-lifecycle-service = { group = "androidx.lifecycle", name = "lifecycle-service", version.ref = "androidxLifecycle" }
androidx-lifecycle-process = { group = "androidx.lifecycle", name = "lifecycle-process", version.ref = "androidxLifecycle" }
androidx-navigation3-runtime = { group = "androidx.navigation3", name = "navigation3-runtime", version.ref = "androidxNavigation3" }
androidx-navigation3-ui = { group = "androidx.navigation3", name = "navigation3-ui", version.ref = "androidxNavigation3" }
# DI
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
hilt-android-testing = { group = "com.google.dagger", name = "hilt-android-testing", version.ref = "hilt" }
androidx-hilt-lifecycle-viewmodel-compose = { group = "androidx.hilt", name = "hilt-lifecycle-viewmodel-compose", version.ref = "androidxHilt" }
androidx-hilt-work = { group = "androidx.hilt", name = "hilt-work", version.ref = "androidxHilt" }
androidx-hilt-compiler = { group = "androidx.hilt", name = "hilt-compiler", version.ref = "androidxHilt" }
# Data
androidx-room3-runtime = { group = "androidx.room3", name = "room3-runtime", version.ref = "androidxRoom3" }
androidx-room3-compiler = { group = "androidx.room3", name = "room3-compiler", version.ref = "androidxRoom3" }
androidx-room3-testing = { group = "androidx.room3", name = "room3-testing", version.ref = "androidxRoom3" }
androidx-sqlite-bundled = { group = "androidx.sqlite", name = "sqlite-bundled", version.ref = "androidxSqlite" }
androidx-sqlite-bundled-jvm = { group = "androidx.sqlite", name = "sqlite-bundled-jvm", version.ref = "androidxSqlite" }
androidx-datastore = { group = "androidx.datastore", name = "datastore", version.ref = "androidxDataStore" }
# androidx-room-runtime / -compiler (2.8.5) — fallback only, add if Room 3 is abandoned
# Media & background
androidx-media3-exoplayer = { group = "androidx.media3", name = "media3-exoplayer", version.ref = "androidxMedia3" }
androidx-media3-session = { group = "androidx.media3", name = "media3-session", version.ref = "androidxMedia3" }
androidx-media3-datasource-okhttp = { group = "androidx.media3", name = "media3-datasource-okhttp", version.ref = "androidxMedia3" }
androidx-media3-ui-compose-material3 = { group = "androidx.media3", name = "media3-ui-compose-material3", version.ref = "androidxMedia3" }
androidx-media3-test-utils = { group = "androidx.media3", name = "media3-test-utils", version.ref = "androidxMedia3" }
androidx-media3-test-utils-robolectric = { group = "androidx.media3", name = "media3-test-utils-robolectric", version.ref = "androidxMedia3" }
androidx-work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "androidxWork" }
androidx-work-testing = { group = "androidx.work", name = "work-testing", version.ref = "androidxWork" }
androidx-glance-appwidget = { group = "androidx.glance", name = "glance-appwidget", version.ref = "androidxGlance" }
androidx-startup-runtime = { group = "androidx.startup", name = "startup-runtime", version.ref = "androidxStartup" }
androidx-profileinstaller = { group = "androidx.profileinstaller", name = "profileinstaller", version.ref = "androidxProfileInstaller" }
androidx-benchmark-macro = { group = "androidx.benchmark", name = "benchmark-macro-junit4", version.ref = "androidxBenchmark" }
# Kotlin
kotlinx-coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "kotlinxCoroutines" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "kotlinxCoroutines" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "kotlinxCoroutines" }
kotlinx-coroutines-guava = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-guava", version.ref = "kotlinxCoroutines" }  # Media3 session callbacks (doc 06)
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
kotlinx-datetime = { group = "org.jetbrains.kotlinx", name = "kotlinx-datetime", version.ref = "kotlinxDatetime" }
kotlinx-collections-immutable = { group = "org.jetbrains.kotlinx", name = "kotlinx-collections-immutable", version.ref = "kotlinxCollectionsImmutable" }
# Network
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-mockwebserver3 = { group = "com.squareup.okhttp3", name = "mockwebserver3", version.ref = "okhttp" }
# Other
adhan2 = { group = "com.batoulapps.adhan", name = "adhan2", version.ref = "adhan2" }
aboutlibraries-compose-m3 = { group = "com.mikepenz", name = "aboutlibraries-compose-m3", version.ref = "aboutLibraries" }
play-review-ktx = { group = "com.google.android.play", name = "review-ktx", version.ref = "playReview" }
# Test
junit4 = { group = "junit", name = "junit", version.ref = "junit4" }
truth = { group = "com.google.truth", name = "truth", version.ref = "truth" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }
roborazzi = { group = "io.github.takahirom.roborazzi", name = "roborazzi", version.ref = "roborazzi" }
roborazzi-compose = { group = "io.github.takahirom.roborazzi", name = "roborazzi-compose", version.ref = "roborazzi" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
leakcanary-android = { group = "com.squareup.leakcanary", name = "leakcanary-android", version.ref = "leakcanary" }
androidx-test-core = { group = "androidx.test", name = "core", version.ref = "androidxTest" }
androidx-test-runner = { group = "androidx.test", name = "runner", version.ref = "androidxTest" }
androidx-test-rules = { group = "androidx.test", name = "rules", version.ref = "androidxTest" }
androidx-test-ext-junit = { group = "androidx.test.ext", name = "junit", version.ref = "androidxTestExtJunit" }
androidx-test-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "androidxEspresso" }
androidx-test-uiautomator = { group = "androidx.test.uiautomator", name = "uiautomator", version.ref = "androidxUiAutomator" }
slack-compose-lint = { group = "com.slack.lint.compose", name = "compose-lint-checks", version.ref = "slackComposeLint" }
compose-rules-ktlint = { group = "io.nlopez.compose.rules", name = "ktlint", version.ref = "composeRules" }  # Spotless ktlint rule set

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
android-test = { id = "com.android.test", version.ref = "agp" }
android-lint = { id = "com.android.lint", version.ref = "agp" }
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }   # root apply-false: lifts AGP's bundled KGP 2.2.10 → 2.4.21
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
room3 = { id = "androidx.room3", version.ref = "androidxRoom3" }
baselineprofile = { id = "androidx.baselineprofile", version.ref = "androidxBenchmark" }
roborazzi = { id = "io.github.takahirom.roborazzi", version.ref = "roborazzi" }
spotless = { id = "com.diffplug.spotless", version.ref = "spotless" }
kover = { id = "org.jetbrains.kotlinx.kover", version.ref = "kover" }
dependency-analysis = { id = "com.autonomousapps.dependency-analysis", version.ref = "dependencyAnalysis" }
aboutlibraries-android = { id = "com.mikepenz.aboutlibraries.plugin.android", version.ref = "aboutLibraries" }
# convention plugins (no version)
criterion-android-application = { id = "criterion.android.application" }
criterion-android-application-compose = { id = "criterion.android.application.compose" }
criterion-android-application-flavors = { id = "criterion.android.application.flavors" }
criterion-android-library = { id = "criterion.android.library" }
criterion-android-library-compose = { id = "criterion.android.library.compose" }
criterion-android-feature = { id = "criterion.android.feature" }
criterion-jvm-library = { id = "criterion.jvm.library" }
criterion-hilt = { id = "criterion.hilt" }
criterion-room3 = { id = "criterion.room3" }
criterion-android-robolectric = { id = "criterion.android.robolectric" }
criterion-android-screenshot = { id = "criterion.android.screenshot" }
criterion-android-lint = { id = "criterion.android.lint" }
criterion-spotless = { id = "criterion.spotless" }
criterion-content-eligibility = { id = "criterion.content.eligibility" }
criterion-android-test = { id = "criterion.android.test" }
criterion-root = { id = "criterion.root" }
```

The root `build.gradle.kts` applies every plugin alias with `apply false`, including `kotlin.jvm`. That is the mandatory KGP lift (g04 §1.2). It also applies `dependency-analysis` and `criterion.root`. `settings.gradle.kts` uses `pluginManagement { includeBuild("build-logic") }`, foojay 1.0.0 and `FAIL_ON_PROJECT_REPOS` (as in the spike). `gradle.properties` keeps the spike's settings: configuration cache, build cache, parallel, `-Xmx4g`.

### 16.2 Convention plugins (`build-logic/convention`)

| Plugin | Applies / configures |
|---|---|
| `criterion.android.application` | AGP app; compileSdk `release(37)`, targetSdk 36, minSdk 24; Java 17; `isCoreLibraryDesugaringEnabled` + `coreLibraryDesugaring(desugar_jdk_libs)`; R8 `optimization { enable = true }` for release; packaging excludes; lint wiring |
| `criterion.android.application.compose` | Compose compiler plugin, BOM, `stabilityConfigurationFiles` (`:core:model` types), `-PcomposeReports` metrics |
| `criterion.android.application.flavors` | §11.1 dimensions and BuildConfig fields (`CONTENT_TIER`, `REMOTE_CHANNEL`, `MONETIZED` from `-Pcriterion.release` / `-Pcriterion.monetized`); disables `playPersonal*`; `copyPersonalAssets` from `~/criterion-data` into the gitignored `app/src/personal/assets/packs/`; applies `criterion.content.eligibility` |
| `criterion.android.library` / `.compose` | Same SDK/Java/desugaring for libraries; `resourcePrefix` per module; Compose as above |
| `criterion.android.feature` | library.compose + hilt + serialization; adds `:core:domain`, `:core:model`, `:core:designsystem`, `:core:navigation`, lifecycle-runtime-compose, lifecycle-viewmodel-navigation3, hilt-lifecycle-viewmodel-compose, navigation3-runtime, collections-immutable; testImplementation `:core:testing`, Turbine, Roborazzi |
| `criterion.jvm.library` | `org.jetbrains.kotlin.jvm`, JVM 17 toolchain/target; JUnit4 + Truth + coroutines-test |
| `criterion.hilt` | KSP + Hilt plugin; `ksp(hilt-compiler)`; for Android modules also `androidx.hilt` compiler if workers are present |
| `criterion.room3` | KSP + `androidx.room3`; `room3 { schemaDirectory("$projectDir/schemas") }`; room3-runtime + sqlite-bundled + `ksp(room3-compiler)`; room3-testing; a **migration-completeness check** for `user.db` (added with user.db, E-07-S01) |
| `criterion.android.robolectric` | g04 rec. 8: `--add-exports java.base/jdk.internal.access` + `--add-opens java.base/java.io`; host `libsqliteJni` extraction from `sqlite-bundled-jvm` (linux_x64/linux_arm64/osx_arm64/windows_x64); `isIncludeAndroidResources`; `-ProboSdks=N` path for NATIVE-graphics kashida goldens, one SDK per JVM; never a test-resource folder named `fonts/` |
| `criterion.android.screenshot` | Roborazzi plugin + `roborazzi-compose`; `verifyRoborazziDebug` / `recordRoborazziDebug`; the doc 10 screenshot matrix (4 themes × EN/UR × 100/130/200 % font scale) |
| `criterion.android.lint` | `warningsAsErrors` on CI, `abortOnError`, baseline, Slack compose-lint checks, rules from `:lint-rules` |
| `criterion.spotless` | Spotless + ktlint 1.8.0 + compose-rules 0.6.8; licence-header check (Apache headers on ported files; added with the first ported file, A3) |
| `criterion.content.eligibility` | `verifyContentEligibility<Variant>` (§11.3), using the shared `ContentPolicy`; G1/G5 checks for PUBLIC |
| `criterion.android.test` | `com.android.test` + baselineprofile for `:baselineprofile` |
| `criterion.root` | Guard tasks `checkModuleBoundaries` (rules in §3.4) and `verifyKotlinGradlePlugin` (asserts `buildEnvironment` resolves `kotlin-gradle-plugin` to 2.4.21, g04 rec. 1); aggregate `kover` (with the nightly job, E-02-S05). **Root aggregate tasks** (library modules have no flavors, so CI calls these instead of per-module names; exact membership owned by [10](10-quality-testing-ci.md), which this row mirrors): `quickCheck` = `spotlessCheck` + build-logic tests + every library `testDebugUnitTest` (JVM modules: `test`) + Roborazzi verify (`testGoldens`) + `:app:testPlayPublicDebugUnitTest` + `:app:testFossPersonalDebugUnitTest` (code paths only; the personal packs folder is empty in CI). `prCheck` = `quickCheck` + library `lintDebug` + `:app:lintPlayPublicDebug` + `:app:lintFossPersonalDebug` + guards (`verifyKotlinGradlePlugin`, `checkModuleBoundaries`, `verifyContentEligibility` for `playPublicRelease` and `fossPublicRelease`) + `:app:assemblePlayPublicDebug :app:assembleFossPublicDebug`. CI assembles and eligibility-checks the **public tier only**. `personalCheck` (owner's machine only) = `copyPersonalAssets` + `verifyContentEligibilityFossPersonalRelease` + `:app:assembleFossPersonalRelease`. The required GitHub status is `ci/check` |

---

## 17. Implementation sequence, spikes and acceptance criteria

| # | Task (owner doc for detail) | Phase | Estimate | Acceptance criteria |
|---|---|---|---|---|
| A1 | Promote spike toolchain into the root project; `build-logic` with the §16.2 plugins (incl. `quickCheck`/`prCheck`/`personalCheck`); wrapper 9.8.1 | P0 | 1.5 d | `./gradlew buildEnvironment` shows KGP 2.4.21; `assembleFossPersonalDebug` (locally) and `assemblePlayPublicRelease` succeed; `checkModuleBoundaries` passes; `./gradlew quickCheck` is the `ci/check` step |
| A2 | Module skeleton: the 9 P0 modules with packages `com.chughtai.criterion.core.*` / `.feature.*` | P0 | 0.5 d | Empty Hilt graph builds; `:core:model` and `:core:domain` have no Android plugin |
| A3 | `:core:model` types (§5) + `QuranMeta` port with Apache headers + `TextNormalizer` with vectors | P0 | 1.5 d | 100 % unit coverage on key math; normalizer vectors pass |
| A4 | `QuranContentDb` entities (§7.2) + exported schema; pipeline v0 creates the DB ([08](08-data-pipelines.md)) | P0 | 1.5 d | §7.2.1 invariants green; no-master open passes; FTS returns 45 hits for الرحمن (g04 baseline) |
| A5 | `UserDb` v0 + DAOs + `DataStore<UserSettings>` + repository impls for bookmarks/last-read/settings | P0–P1 | 2 d | Turbine tests for Flows; DataStore corruption test |
| A6 | Navigation shell (`NavigationState`, keys, `DeepLinkParser`) + `MainActivity : AppCompatActivity` + per-app locale switch | P0 | 1 d | Process-death test restores the back stack; Urdu switch works on API 24 and 33 |
| A7 | `:core:remote`: OkHttp, `SignedFileFetcher`, `Ed25519` port, `HttpDownloader`, `downloads.db`, `DownloadWorker`; UIDT host in S5 | P1 (UIDT S5) | 4 d | RFC 8032 + Wycheproof vectors pass; MockWebServer resume and 304 tests; lower sequence and same-sequence-different-bytes rejected; body/sig race retried once; expired index offers no installs |
| A8 | `PackManager` + per-tier bundled-pack install (incl. gunzipped timing packs) + `installed_pack` + `notices()` incl. `minVersion` and `device` targets | P1 | 2.5 d | Bundled packs install in < 2 s; tampered pack rejected; kill-switch hides pack; page notice shows only the chip (no pixel inside the page frame changes) |
| A9 | Variant wiring (`CONTENT_TIER`, `REMOTE_CHANNEL`, `MONETIZED`, `copyPersonalAssets`), eligibility plugin + `bundled.json` + CI release gate | P1 | 1.5 d | Fixtures fail `verifyContentEligibility`: an item without BETA in `eligibleTiers` in `playPublicRelease`; a PENDING layout pack in BETA; a PUBLIC build without `proof_certificate_id` or with one page hash differing from `certified_pages.json`; a MONETIZED build with a `commercialOk = false` item. A PENDING recitation passes in BETA and fails in PUBLIC |
| A10 | Remaining P1 modules per docs 05/06; P2 `:core:prayer` per doc 07 | P1/P2 | (in those docs) | — |
| A11 | `:baselineprofile` journeys + startup/page-turn benchmarks | P1 (late) | 1.5 d | Budgets in §13 reported nightly |

**P0 verification spikes** (each one hour or less; run on the owner's API 24/26 devices together with the g04 rec. 10 device smoke test):
- **S-1.** Room 3 opens an **absolute-path** pack DB built from an exported schema (`TranslationPackDb`), and two pack instances coexist.
- **S-2.** `setJournalMode(TRUNCATE)` produces a single `user.db` file. Auto Backup restore via `adb shell bmgr` restores bookmarks.
- **S-3.** `hiltViewModel` with an assisted NavKey factory inside a Nav3 entry. Fallback: pass the key through `SavedStateHandle`.
- **S-4.** A FTS5 trigram index over 3 Arabic columns is built and its size measured. If it adds more than 3 MB to the compressed AAB, restrict the trigram index to `norm_imlaei` (logged as an open issue, not a register change).
- **S-5.** The UIDT job scheduled from a visible activity on API 34 shows its notification and survives app swipe-away. (Run in S1 on the API 34 device; the UIDT host itself ships in S5.)

---

## 18. Risks and open issues (architecture-level; the register is owned by [12](12-decisions-risks.md))

1. **Room 3 youth** (stable since 2026-07-01). Both DBs depend on it. Mitigation: thin DAOs, a proven Room 2.8.5 + FTS4 fallback (g04 §2.4). Switching costs about 2 days of imports and query syntax.
2. **Pack-per-file Room instances.** S-1 must pass. Fallback: open packs through a raw `BundledSQLiteDriver().open(path)` connection with hand-written readers. This is small: 3 query shapes per pack type.
3. **Trigram index size** over 3 columns is unmeasured (S-4). The register (D-11) says 3 columns, and any narrowing goes to doc 12 as a proposal.
4. **UIDT scheduling rule** (it must be scheduled while visible). Auto-updates on API 34+ are deferred to the next foreground. This is acceptable. P1 avoids the question by using `DownloadWorker` on every API until S5.
5. **Assisted ViewModel injection on Nav3** (S-3). It affects ergonomics only.
6. **Ed25519 vendored port** is a security-sensitive component. Mitigation: test vectors in CI, verify-only scope, and BouncyCastle as fallback. A public-domain origin keeps both G4 licensing options open.
7. **`SystemEventsReceiver` exported.** Accepted, because the handler is idempotent. Re-check when targeting API 37.
8. **No backend in v1.0** means the kill-switch latency is the refresh interval (≤ 24 h, plus on foreground). That fits the text-error SLA (§14.2: signed notice ≤ 48 h). The v1.1 crash-report Worker (D-32) is write-only and does not change this.
9. **Hijri implementation choice.** r07 recommends ICU `IslamicCalendar`, but the register mandates `java.time.HijrahDate` via desugaring (D-15, D-27). HijrahDate on API 24–25 is unverified, so the P0 device smoke test compares both on 5 reference dates. Both sit behind `HijriCalendarProvider`, and the ICU implementation is the fallback.
10. **Personal assets outside git.** The personal build depends on `~/criterion-data`. A fresh clone cannot assemble `fossPersonal*` until the owner restores that folder. CI never assembles a personal build; the owner runs `personalCheck` locally before each sideload build.
