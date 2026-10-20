# Criterion — Quran app for Android (codename)

Ad-free, no-tracking Quran app for Pakistan. Must-haves: a **typeset Indo-Pak 16-line Taj mushaf**
(548 pages; page_index 1–548, printed labels 2–549) drawn by OUR Compose Canvas renderer from the
DigitalKhatt IndoPak font + precomputed line/word layout; recitation by a favourite qari for
Surah / Para / Manzil / Ruku / page / ayah range, with ayah follow-along highlight; Para / Surah /
Manzil / Ruku indexes with Read + Play on every row; Urdu + English UI and translations.
Package/namespace `com.chughtai.criterion`. "Criterion" is a codename — the public brand is undecided
(gate G3): put no brand name in code; only the `app_name` string resource.
Solo owner + AI agents. The owner approves plans, reviews diffs/goldens, tests on devices, merges.

## Where the plan lives (read the relevant doc BEFORE coding)
docs/plan/README.md master plan · 01 PRD (feature IDs F-xx-nn) · 02 UX/UI · 03 content & licences ·
04 architecture (modules, interfaces, DDL, NavKeys, manifest) · 05 mushaf renderer · 06 audio engine &
downloads · 07 companion/hifz · 08 data pipelines (tools/) · 09 roadmap & backlog (epics/stories, DoR/DoD) ·
10 quality, testing & CI · 11 release & Play compliance · 12 ADRs (D-xx), gates (G1–G6), risks (R-xx) ·
13 this playbook. docs/research/* = evidence (read-only). spikes/toolchain-spike/ = verified reference
build (read-only, not part of the root build).
If code and plan disagree: stop, tell the owner, and propose a plan edit in the same PR. Never silently diverge.

## Commands
```bash
./gradlew quickCheck        # spotlessCheck + library testDebugUnitTest + :app:testFossPersonalDebugUnitTest
                            # + verifyRoborazziDebug (local loop)
./gradlew prCheck           # quickCheck + lintFossPersonalDebug lintPlayPublicDebug + testPlayPublicDebugUnitTest
                            # + checkModuleBoundaries + verifyContentEligibilityPlayPublicRelease
                            # + assembleFossPersonalDebug assemblePlayPublicDebug (what CI's `ci/check` runs)
# explicit equivalents. Library modules have no flavors; :app has distribution {play, foss} × tier
# {personal, public}; playPersonal* is disabled (doc 04 §11):
./gradlew spotlessCheck testDebugUnitTest :app:testFossPersonalDebugUnitTest :app:testPlayPublicDebugUnitTest verifyRoborazziDebug :app:lintFossPersonalDebug :app:lintPlayPublicDebug
./gradlew spotlessApply                                  # format before committing
./gradlew :core:content:testDebugUnitTest                # content-DB invariants + schema-validation tests
./gradlew :app:assembleFossPersonalRelease               # R8 release build; the owner's daily sideloaded build
./gradlew :app:bundlePlayPublicRelease                   # closed-test build (CONTENT_TIER = BETA); PUBLIC only with
                                                         # -Pcriterion.release=production (owner + release workflow only)
./gradlew verifyKotlinGradlePlugin                       # Kotlin lift guard (asserts 2.2.10 -> 2.4.21)
./gradlew :app:installFossPersonalDebug && adb shell monkey -p com.chughtai.criterion.personal.debug -c android.intent.category.LAUNCHER 1
adb exec-out screencap -p > build/shots/<name>.png       # then OPEN the PNG and describe what you see
```
Goldens: `recordRoborazziDebug` (per module) ONLY when the story intends a visual change; see "Goldens" below.
Robolectric NATIVE-graphics tests run one SDK per JVM (`-ProboSdks=N`, spike pattern).
Python pipelines (tools/, doc 08): `cd tools && uv run --frozen python -I -m crit <stage> <cmd>`, or the
`just` aliases (`just content`, `just mushaf`, `just proof`, `just timing`, `just validate`, `just assets`,
`just ci`). Pipelines read only pinned inputs (data/pins.lock); never fetch upstream ad hoc.
`crit sign` / `crit publish` (`just sign`, `just publish`) are OWNER-ONLY: never run them.

## Toolchain (frozen — D-02; changing any version needs an ADR in doc 12 and owner approval)
AGP 9.4.1 · Gradle 9.8.1 · Kotlin 2.4.21 lifted via root `alias(libs.plugins.kotlin.jvm) apply false`
(fallback 2.3.21; NEVER 2.2.10 — adhan2 0.0.7 metadata) · KSP 2.3.12 (no kapt) · compileSdk 37 ·
targetSdk 36 · minSdk 24 + desugaring 2.1.5 · Java 17 · R8 on · Compose BOM 2026.09.00 · Nav3 1.2.0 ·
Hilt 2.60.1 · Room 3.0.3 (androidx.room3) + sqlite-bundled 2.7.1 · DataStore 1.2.1 · Media3 1.11.1 ·
WorkManager 2.12.0 · Glance 1.2.0 · OkHttp 5.5.0 · kotlinx.serialization 1.11.0 · coroutines 1.11.0 ·
adhan2 0.0.7. NO Ktor. AGP > 9.4 does not sync in stable Android Studio (Rabbit 1) — do not try.

## Module conventions (doc 04 is authoritative)
- `:app` → `:feature:*` → `:core:*`. Features NEVER depend on features (`criterion.root`'s `checkModuleBoundaries` fails the build; rules in doc 04 §3.4).
- All `NavKey`s live in `:core:navigation`. Cross-feature navigation = NavKeys only.
- `:core:model` and `:core:domain` are pure Kotlin (`criterion.jvm.library`), no Android, no DI annotations.
  Interfaces (repositories, `PlaybackController`, `PlaybackStateHub`, `HijriCalendarProvider`) live in `:core:domain`;
  implementations live in `:core:data`, `:core:audio`, `:core:prayer`, …
- Each core module exposes interfaces; implementations are `internal`, bound in that module's Hilt module.
  `QuranContentDb` and all Room types stay `internal` to their module (no `api(room3-runtime)` leaks).
- New module = a convention plugin from build-logic only; no copy-pasted android {} blocks.
- Add the new module to this file's module list (below) in the same PR.
Modules today (doc 04 §3.2 P0 set): :app :core:model :core:domain :core:content :core:database :core:data
:core:designsystem :core:navigation :core:testing
(AP-01 state = :app only. Clock/dispatchers go in :core:domain, DataStore<UserSettings> in :core:database,
model-bound rows in :core:designsystem. P1 adds :core:remote :core:mushaf-render :core:audio, the six
features and :baselineprofile; P2 adds :core:prayer :feature:prayer :feature:companion. Never invent a module
name that is not in doc 04 §3; the PR that adds/removes a module updates this list.)

## Coding standards
- Kotlin official style, enforced by Spotless + ktlint 1.8.0 + compose-rules 0.6.8; Android Lint + Slack
  compose-lint 1.6.1, warnings as errors on CI.
- MVVM/UDF: one immutable `UiState` per screen as `StateFlow` (`stateIn(viewModelScope,
  WhileSubscribed(5_000), …)`), intents via `onAction(Action)`, immutable collections, stateless
  `XxxScreen` composables with previews, `XxxRoute` wires the ViewModel.
- Errors: sealed `AppError`; one-shot repository calls return `Outcome<T>`; observables return `Flow<T>`.
- Inject dispatchers (`@IoDispatcher`); never hard-code `Dispatchers.IO` in classes. Media3 calls on main looper only.
- Offline-first: reading, indexes and search never touch the network.
- All user-visible strings in `strings.xml` (en + ur). No string concatenation for sentences; use
  placeholders. Wrap verse keys/numbers inside RTL text with bidi isolation.
- Tests: fakes over mocks (`:core:testing`); `runTest` + `MainDispatcherRule`, never `runBlocking`
  around `viewModelScope`; Turbine for flows; Truth assertions.
- Robolectric: the `criterion.android.robolectric` plugin sets `--add-exports java.base/jdk.internal.access`
  + `--add-opens java.base/java.io` and the host `libsqliteJni`; NEVER name a test-resource folder `fonts/`.
- No `TODO` without a doc 09 story ID. No commented-out code. No new dependency without updating
  libs.versions.toml AND the licence list (AboutLibraries) AND saying why in the PR.

## NON-NEGOTIABLE RULES (violations block the PR)
1. CLEAN-ROOM. Never copy, paste, translate or closely paraphrase code from GPL/AGPL projects
   (quran_android, QuranApp, Noor-Ul-Huda, …). Read for ideas only, close the file, write from the spec.
   Apache/MIT ports (quran-ios QuranKit, LinePageGeometryEngine; FSRS-Kotlin) keep the original
   licence header + a NOTICE entry + "Ported from <repo>@<commit>" comment.
2. NO QF/QURAN.COM CONTENT IN SHIPPED DATA. Never bundle or cache-as-database anything from api.quran.com /
   Quran Foundation content APIs. No client secret in the app, ever.
3. NO HOTLINKING OUTSIDE THE CATALOGUE. Audio/pack URLs come only from the signed catalogue.json /
   index.json entry (detached `.sig`, Ed25519 over the exact file bytes) whose buildEligibility includes
   BuildConfig.CONTENT_TIER (PERSONAL | BETA | PUBLIC) and whose kill-switch is enabled.
   No hard-coded third-party media URLs in Kotlin. Never pair a timing file and audio from different hosts.
4. LICENCE MANIFEST FOR EVERY PACK/ASSET. Every pack, font, dataset and audio source has licence,
   licenceStatus (PERMITTED | HOST_GRANT | PENDING | PERSONAL_ONLY | REVOKED) + free-text basis,
   attribution, sha256, size and eligibility flags (personal/beta/public/monetized) in the manifest;
   `verifyContentEligibility<Variant>` fails a build that contains content ineligible for its tier.
   QUL-derived layout data lives only in `src/personal/assets/` until G1; never in `src/main/assets/`.
5. QURAN TEXT IS PIPELINE-ONLY. Never type, paste, "fix", normalize or reformat Arabic Quran text
   (ayah text, tokens, layout lines) in any source, test, fixture or data file. Do not output Quran text
   from memory. Refer to text by verse key / token key and read it from the content DB or the pipeline.
   Corrections = reviewed pipeline transforms (doc 08) confirmed in the hafiz packet. Short
   tashkeel-free search-query literals in tests are the only exception.
6. GENERATED FILES ARE NEVER HAND-EDITED: *.db assets, packs, Room schema JSON, fonts, proofs.
   Regenerate via the pipeline/KSP.
7. KEEP GOLDENS GREEN. Never re-record a golden to make a failing test pass. Re-record only when the story
   intends the visual change; attach the diff images to the PR and explain each one.
8. KUFAN VERSE KEYS EVERYWHERE. `VerseKey(surah, ayah)` in Kufan numbering (id() 1..6236; 1:1 = basmala).
   Use doc 04 §5's identity types verbatim (VerseKey, SurahId, ParaId, ManzilId, RukuId, MushafId, PageIndex,
   PageLabel, AyahRange, RecitationId); no `AyahKey`, no raw Int ids in public APIs. Never DK/QUL display
   numbering, never database row ids in user.db or in the API between modules. Page references =
   (MushafId, PageIndex), e.g. INDOPAK_16_TAJ (code `indopak16_taj`); show page_label (= index + 1 for
   taj16) in all UI.
9. PARA = THE INDO-PAK TABLE (content DB `para`). Never derive Para from Madani juz (QuranKit Juz,
   quran.com juz, any API). 7 boundaries differ: P4 3:92, P7 5:83, P11 9:94, P14 15:2, P20 27:60,
   P21 29:45, P23 36:22. Madani juz/hizb/rub exist only for Madani mode (v2).
10. TYPESET, NEVER SCANS. No mushaf page images, no letter-spacing, no StaticLayout/TextAlign.Justify for
    mushaf lines; font = pinned DigitalKhatt IndoPak glyf TTF (never the QUL CDN DigitalKhattIndoPak.otf —
    it has no cv01). `setFontFeatureSettings("'cv01' N")` only on stretched words.
11. PRIVACY. Zero third-party SDKs (no analytics, ads, attribution, crash SDKs) in v1; no Google Play
    Services in core (play-only extras live in the `play` flavor). No reciter photos.
12. PERMISSIONS. Never add USE_EXACT_ALARM, ACCESS_NOTIFICATION_POLICY, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
    USE_FULL_SCREEN_INTENT, fine location, storage/media permissions. Playback starts only from user actions.
13. SECRETS. No keys, keystores, tokens or private signing keys in the repo, logs or prompts. The Ed25519
    private key is offline (owner); only the public key is in the app.
14. ARABIC IS ALWAYS SHOWN WITH A TRANSLATION. No translation-only mode anywhere (Punjab Act s.5(6)).
15. FROZEN TOOLCHAIN. No version bumps, no new Gradle plugins, no kapt, no Ktor without an ADR.

## Workflow (doc 13 §4)
One story per session. Start in plan mode: restate goal, list files, list tests, list verification.
Wait for approval. Implement in small commits. Then run the verification ladder required for the story
type (doc 13 §5) and paste the evidence block. Never say "done" without the commands' actual output.
Branch `<story-slug>-<slug>` from `master` (doc 09 ID lower-cased, e.g. E-05-S03 → `e05-s03-page-golden`);
Conventional Commits `type(scope): summary [<story-id>]` (ID verbatim, e.g. [E-05-S03]);
PR uses .github/pull_request_template.md. Never push to `master`, never force-push, never merge.

## Ask the owner (stop and ask) when
the spec is silent or contradictory · a dependency/version/permission would change · a licence is unclear ·
Quran text or curated Arabic metadata looks wrong · a golden changes unexpectedly · a test is flaky ·
you would need network data that is not pinned · you are about to delete or rewrite > 300 lines.
