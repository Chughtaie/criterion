# 07 — Android tech stack & architecture (October 2026)

Researcher key: `r07-android-stack` · Date: 2026-10-19 · Role: principal Android architect.

Scope: the concrete toolchain, libraries, platform-policy constraints, module graph and architecture for the "Criterion" Quran app. The starting point is the generated project in `/home/user/criterion` (AGP 9.2.1, built-in Kotlin, Kotlin 2.2.10, Compose BOM 2026.02.01, compileSdk 36.1, targetSdk 36, minSdk 24, Gradle wrapper 9.4.1, daemon JVM toolchain 21).

**Method**
- **Versions.** I read every version from the live `maven-metadata.xml` on Google Maven (`dl.google.com/android/maven2`) and Maven Central (`repo1.maven.org/maven2`), or from the Gradle Plugin Portal.
- **BOM mapping.** I read Compose BOM contents from the BOM's `.pom` file.
- **minSdk.** I unzipped each library AAR's `AndroidManifest.xml` and read its minSdk.
- **API claims.** I checked Room 3 API claims against its `-sources.jar`. I checked native-library alignment by parsing the ELF program headers.
- **Policy and behaviour.** I read these from developer.android.com and the Play Console Help pages.
- **Reference project.** I shallow-cloned Now in Android (commit `a49ed25`, 2026-09-22) as a reference for an up-to-date AGP 9 build.

**Tags**
- **[verified: url]**: I fetched or inspected it in this session.
- **[inferred]**: my reasoning from verified facts.
- **[unverified]**: from memory.

Sibling reports this one builds on:
- 03: clean-room build, quran_android as reference only.
- 04: font-based Indo-Pak Canvas renderer, 16-line Taj layout, DigitalKhatt OFL font.
- 05: own Para table, Tanzil/QUL/QuranEnc data, ruku and sajda rules.
- 06: Media3 `MediaLibraryService` with gapped and gapless audio and `ResolvingDataSource`.

---

## 1. Executive summary

1. **Upgrade the toolchain first. It is one PR.**

   | Component | Now | Target |
   |---|---|---|
   | AGP | 9.2.1 | **9.4.1** (latest stable) |
   | Gradle | 9.4.1 | **9.8.1** (AGP 9.4 needs ≥ 9.6.0) |
   | Kotlin | 2.2.10 | **2.4.21** (released 2026-10-08) |
   | KSP | — | **2.3.12** |
   | compileSdk | 36.1 | **37** (SDK Platform 37.0, 37.1 and 37.2 are on the stable channel) |

   - **Kotlin:** AGP 9.x still pins KGP 2.2.10 at runtime, so we declare a higher KGP to lift built-in Kotlin.
   - **targetSdk stays 36.** It meets the Play rule that has applied since 2026-08-31.
   - **minSdk stays 24.**

   [verified: Google Maven, kotlinlang.org/docs/releases.html, AGP release notes, SDK repository2-3.xml]
2. **UI: Jetpack Compose with BOM 2026.09.00, Navigation 3 and the adaptive libraries.**
   - **BOM 2026.09.00** brings Compose 1.12.1, Material 3 1.4.0 and Adaptive 1.3.0.
   - **Material 3 Expressive** components are graduating to non-experimental in **1.5.0-beta01** (2026-10-07). Adopt them when 1.5.0 goes stable. Do not build on the beta.
   - **Use Navigation 3 1.2.0** (stable since 2025-11-19; 1.2.0 released 2026-09-23), not Navigation-Compose 2.x. Its back stack is a plain `@Serializable` list, which suits the reader/index/audio flows. It also gives predictive back and list-detail scenes on tablets.
   - **Use material3-adaptive and androidx.window 1.5.1** for the two-page mushaf spread on tablets and foldables.

   [verified]
3. **DI: Hilt 2.60.1 + androidx.hilt 1.4.0, run through KSP.**
   - Hilt gained AGP 9 support in 2.59, and 2.59.1 made AGP ≥ 9.0.0 a hard requirement.
   - It has first-class `hiltViewModel()` for Navigation 3 and WorkManager.
   - Koin (runtime DI) and Metro (compiler-plugin DI, very good but tied to Kotlin compiler versions) are the alternatives. Revisit Metro only if an iOS app is funded.

   [verified: github.com/google/dagger/releases, Hilt Jetpack docs]
4. **Persistence: Room 3.0.3 (`androidx.room3`, stable since 2026-07-01) with `BundledSQLiteDriver`.**
   - **Room 3:** KSP-only, Kotlin-only, coroutines-first, and KMP across Android, JVM, iOS and web. It supports **FTS5** and **`createFromAsset`** [verified in sources].
   - **The bundled driver** gives the same SQLite and FTS5 on API 24–37. It adds about 1.25 MB of `.so` per ABI, and that `.so` is 16 KB-aligned [verified: ELF headers].
   - **DataStore 1.2.1** holds settings, as a typed `DataStore<T>` with a kotlinx.serialization serializer.
5. **Use two databases: a read-only content DB and a read-write user DB.**
   - **Content DB** (`quran-content.db`): ships in assets. Updates work by "bump `@Database(version)` and Room re-copies the asset". I verified this copy-destructive migration path in the Room 3 source.
   - **User DB** (`user.db`): holds bookmarks, last-read position, khatm plans, downloads and notes. It references content only by stable natural keys (surah:ayah, page number, para number), never by content row IDs. It gets real, tested migrations.
   - **Optional packs** (translations, tafsir, alternative layouts, timings) are separate versioned SQLite or font files, described by a JSON manifest that carries sha256 and licence.
6. **Media and background work.**
   - **Media3 1.11.1:** `MediaLibraryService` + ExoPlayer, Compose `MiniController`, OkHttp data source.
   - **WorkManager 2.12.0:** downloads, run as a `dataSync` foreground service. Android 15 caps that service at **6 h per 24 h**, which is fine.
   - **Glance 1.2.0:** widgets.
   - **Networking:** Ktor 3.6.0 client on an OkHttp 5.5.0 engine, plus kotlinx.serialization 1.11.0.
   - **Coil 3.6.3:** only for reciter photos. Mushaf pages are drawn from fonts, not images (report 04).

   [verified]
7. **No Google Play Services in the core app.**
   - **Location:** `LocationManagerCompat.getCurrentLocation` (androidx.core) with *coarse* location, plus an offline manual city picker.
   - **Qibla:** rotation-vector sensor with `GeomagneticField` declination.
   - **Hijri dates:** `android.icu.util.IslamicCalendar` with `ISLAMIC_UMALQURA`. It is available since **API 24**, so we do *not* need minSdk 26 for Hijri [verified]. Use core-library desugaring for `java.time` and kotlinx-datetime.
   - This keeps an F-Droid/Huawei build possible with one code path. Play-only extras (in-app review and update) live in a `play` flavor.
8. **2026–27 platform rules change the reference app's UX.**
   - **Edge-to-edge** can't be opted out at targetSdk 36.
   - **Predictive back** is on by default.
   - **Orientation locks are ignored on screens ≥ sw600dp.** The opt-out disappears at targetSdk 37. So the "lock" icon must mean rotation lock *on phones only* and scroll/page-turn lock everywhere.
   - **Android 17 restricts background audio.** That affects adhan playback; use `USAGE_ALARM` with the exact-alarm permission, or a while-in-use foreground service.
   - **Exact alarms: request `SCHEDULE_EXACT_ALARM`.** Play limits `USE_EXACT_ALARM` to "alarm or timer" and "calendar" apps.

   [verified]
9. **Size and delivery are easy.**
   - The font-rendered mushaf is about 1–3 MB (report 04). Play's base-module limit is **500 MB**, and the cellular warning starts at 200 MB [verified].
   - **Ship the default mushaf, text and one translation in the base APK.** Self-host optional packs. Play Asset Delivery is Play-only, so use it only for something like QCF V2 fonts (~77 MB) if we ever go Play-only.
   - **Disable App Bundle language splits.** The app has an in-app Urdu/Arabic/English picker [verified].
10. **Modularise the Now-in-Android way, sized for a solo developer.**
    - About 22 modules: `:app`, about 14 `:core:*` modules, about 9 `:feature:*` modules, and `:baselineprofile`.
    - The build logic lives in `build-logic` convention plugins.
    - **Rule:** features never depend on features. All `NavKey`s live in `:core:navigation`.
    - **Be "KMP-ready, Android-only":** use only KMP-capable libraries in core and keep `:core:model` and `:core:domain` pure Kotlin. Do not pay the KMP build tax until iOS is real.
11. **Architecture: MVVM/UDF.**
    - Each ViewModel exposes one immutable `UiState` as a `StateFlow` (`stateIn(WhileSubscribed(5_000))`).
    - Repositories are offline-first. Content is always local, and the network is only used for packs and audio.
    - There is a typed `AppError` hierarchy, and dispatchers are injected.
    - The reader's hot path is precomputed `PageLayout` data from the content DB, rendered on Canvas, with an LRU of ±3 pages.
12. **Quality gates, all on GitHub Actions** (checkout v7, setup-java v6 with Temurin 21, gradle/actions v6):
    - **Profiling:** baseline profiles and Macrobenchmark 1.5.0.
    - **R8:** the AGP 9.3+ `optimization { enable = true }` DSL. Full mode is already the default.
    - **Static checks:** Spotless + ktlint 1.8.0 + compose-rules 0.6.8, Android Lint, and Slack compose-lint-checks 1.6.1.
    - **Screenshot tests:** **Roborazzi 1.76.0**. Google's Compose Preview Screenshot Testing is still "Experimental" at alpha16.
    - **Unit tests:** Robolectric 4.17 (an SDK 17 jar exists), Turbine 1.2.1, Truth 1.4.5, fakes over mocks.
    - **Memory:** LeakCanary 2.14 in debug builds only.
    - **Licences:** AboutLibraries 15.3.0, plus our own content-attribution screen. CC BY and OFL require attribution.

---

## 2. Detailed findings

### 2.1 Baseline → target (what changes in the existing project)

| Item | Current (`/home/user/criterion`) | Target | Why |
|---|---|---|---|
| AGP | 9.2.1 | **9.4.1** | Latest stable. 9.5.0 is at alpha10 [verified: Google Maven `com/android/tools/build/gradle`]. AGP 9.4 supports up to API 37 [verified: AGP release notes]. |
| Gradle wrapper | 9.4.1 | **9.8.1** | AGP 9.4.0 has "Gradle minimum 9.6.0" [verified]. 9.8.1 is current (built 2026-10-07) [verified: services.gradle.org/versions/current]. |
| Kotlin (KGP) | 2.2.10 (AGP-bundled) | **2.4.21** | Latest stable (2026-10-08). The 2.4 stdlib has security support until 2027-12-03 [verified: kotlinlang.org releases]. AGP 9.4.1's POM still `requires` KGP **2.2.10** [verified: gradle-9.4.1.module], so we must declare a higher KGP (§2.12). |
| KSP | — | **2.3.12** (2026-09-09) | 2.3.10 "works with Kotlin 2.4.0" and fixes "R-class resolution in KSP when AGP 9 built-in Kotlin is enabled". 2.3.12 sets min AGP to 8.12.0 [verified: github.com/google/ksp/releases]. |
| Compose BOM | 2026.02.01 | **2026.09.00** | Maps to Compose 1.12.1, material3 1.4.0, adaptive / adaptive-layout / adaptive-navigation3 1.3.0, material-icons-core 1.7.8 [verified: BOM pom]. |
| compileSdk | `release(36){minorApiLevel=1}` | **`release(37)`** | Platforms 37.0, 37.1 and 37.2 are on `channel-0` (stable) [verified: dl.google.com/android/repository/repository2-3.xml]. |
| targetSdk | 36 | **36 now, then 37 by mid-2027** | Play has required 36 since 2026-08-31, extendable to 2026-11-01 [verified]. Plan the 37 migration (§2.8). |
| minSdk | 24 | **24** (keep) | §2.9. Navigation3-UI 1.2.0 and WorkManager 2.12.0 AARs declare minSdk 24 [verified: AAR manifests], so 24 is also our floor. |
| Java/JVM target | 11 | **17** (bytecode), JDK 21 daemon | AGP 9 needs JDK 17 [verified]. The daemon toolchain is already 21 in `gradle-daemon-jvm.properties`. |
| R8 | `optimization { enable = false }` | **`enable = true`** for release | AGP 9.3+ DSL [verified]. Keep rules go in `src/main/keepRules/*.keep` (the template already has this). |
| Activity base | `ComponentActivity` | **`AppCompatActivity`** | Required for per-app language switching via `AppCompatDelegate` on API ≤ 32 with Compose [verified: app-languages doc]. |

### 2.2 Verified version table (October 2026)

All versions below come from live `maven-metadata.xml`. The "lastUpdated" stamp is the repository metadata timestamp, not necessarily the release date. Release dates come from release notes where I read them.

| Area | Artifact | Latest **stable** | Newest pre-release | Notes |
|---|---|---|---|---|
| Build | `com.android.tools.build:gradle` | **9.4.1** | 9.5.0-alpha10 | AGP 9.4.0 released Sep 2026 |
| Build | Gradle | **9.8.1** | 9.9.0-milestone-2 | |
| Build | `org.jetbrains.kotlin:kotlin-gradle-plugin` | **2.4.21** | 2.5.0-Beta1 | 2.5.0 is planned for Dec 2026 [verified: releases page] |
| Build | `com.google.devtools.ksp` | **2.3.12** | — | |
| Build | `foojay-resolver-convention` | **1.0.0** | — | Gradle Plugin Portal |
| UI | `androidx.compose:compose-bom` | **2026.09.00** | — | |
| UI | `androidx.compose.material3:material3` | **1.4.0** | 1.5.0-beta01 (2026-10-07) | Expressive graduations in 1.5.0 (§2.3) |
| UI | `androidx.compose.material3.adaptive:adaptive*` | **1.3.0** | 1.4.0-alpha03 | Includes `adaptive-navigation3` |
| UI | `androidx.navigation3:navigation3-runtime/-ui` | **1.2.0** (2026-09-23) | 1.3.0-alpha02 | |
| UI | `androidx.navigation:navigation-compose` | 2.10.2 | — | Not chosen |
| UI | `androidx.lifecycle:*` (incl. `lifecycle-viewmodel-navigation3`) | **2.11.0** | 2.12.0-alpha04 | |
| UI | `androidx.activity:activity-compose` | **1.13.0** | 1.14.0-alpha03 | |
| UI | `androidx.appcompat:appcompat` | **1.8.0** | — | minSdk 23 |
| UI | `androidx.core:core-ktx` | **1.19.1** | — | minSdk 23 |
| UI | `androidx.core:core-splashscreen` | **1.2.0** | — | |
| UI | `androidx.window:window` | **1.5.1** | 1.6.0-alpha05 | Fold posture (`FoldingFeature`) |
| DI | `com.google.dagger:hilt-*` | **2.60.1** (2026-07-06) | — | Hilt minSdk 23 since 2.60 |
| DI | `androidx.hilt:hilt-lifecycle-viewmodel-compose / hilt-work / hilt-compiler` | **1.4.0** | — | |
| DI (alt) | `io.insert-koin:koin-bom` | 4.2.2 | — | |
| DI (alt) | `dev.zacsweers.metro` | 1.4.5 | — | Kotlin 2.4.20 tested; 2.4.21 not listed [verified: Metro compat page] |
| Data | `androidx.room3:room3-*` | **3.0.3** (2026-09-09) | 3.1.0-alpha01 | 3.0.0 went stable 2026-07-01 |
| Data | `androidx.room:room-*` (2.x) | 2.8.5 | — | Not chosen (§2.5) |
| Data | `androidx.sqlite:sqlite-bundled` | **2.7.1** | 2.8.0-alpha01 | |
| Data | `androidx.datastore:datastore(-preferences)` | **1.2.1** | 1.3.0-rc01 | |
| Media | `androidx.media3:*` | **1.11.1** (2026-09-10) | — | Has `-datasource-okhttp`, `-datasource-ktor`, `-ui-compose-material3`, `-exoplayer-workmanager` [verified: group-index] |
| Background | `androidx.work:work-runtime(-ktx)` | **2.12.0** | — | minSdk 24 |
| Background | `androidx.startup:startup-runtime` | **1.2.0** | — | |
| Widgets | `androidx.glance:glance-appwidget` | **1.2.0** | 1.3.0-alpha02 | minSdk 23 since 1.2.0-beta01 |
| Serialization | `org.jetbrains.kotlinx:kotlinx-serialization-json` | **1.11.0** | 1.12.0-RC | |
| Async | `org.jetbrains.kotlinx:kotlinx-coroutines-*` | **1.11.0** | — | |
| Time | `org.jetbrains.kotlinx:kotlinx-datetime` | **0.8.0** | — | |
| Collections | `org.jetbrains.kotlinx:kotlinx-collections-immutable` | **0.5.2** | — | Compose-stable lists in UiState |
| Network | `io.ktor:ktor-*` | **3.6.0** | — | |
| Network | `com.squareup.okhttp3:okhttp` | **5.5.0** | — | `mockwebserver3` 5.5.0 for tests |
| Images | `io.coil-kt.coil3:coil-compose` | **3.6.3** | — | Optional |
| Location (play flavor only) | `com.google.android.gms:play-services-location` | 21.4.0 | — | Not needed (§2.6) |
| Play extras | `com.google.android.play:review-ktx` / `app-update-ktx` | 2.0.2 / 2.1.0 | — | `play` flavor only |
| Play extras | `com.google.android.play:asset-delivery-ktx` | 2.3.0 | — | Not used in v1 |
| Desugaring | `com.android.tools:desugar_jdk_libs` | **2.1.5** | — | |
| Perf | `androidx.benchmark:benchmark-macro-junit4` + `androidx.baselineprofile` plugin | **1.5.0** (2026-09-09) | 1.6.0-alpha01 | Since 1.5.0-alpha01, no need for `newDsl=false` on AGP 9 [verified] |
| Perf | `androidx.profileinstaller` | **1.4.1** | — | |
| Test | `androidx.test:core/runner/rules` · `ext:junit` · `espresso-core` | **1.7.0 / 1.7.0 / 1.7.0 · 1.3.0 · 3.7.0** | — | |
| Test | `androidx.test.uiautomator:uiautomator` | **2.4.0** | — | |
| Test | `org.robolectric:robolectric` | **4.17** | — | `android-all-instrumented:17-robolectric-15733970-i7` exists (SDK 37) |
| Test | `io.github.takahirom.roborazzi` | **1.76.0** | — | |
| Test | `app.cash.paparazzi` | 1.3.5 | 2.0.0-alpha05 | Not chosen |
| Test | Compose Preview Screenshot Testing plugin | — | 0.0.1-alpha16 | "Experimental" [verified] |
| Test | `app.cash.turbine:turbine` | **1.2.1** | — | |
| Test | `com.google.truth:truth` | **1.4.5** | — | |
| Test | `io.mockk:mockk` | 1.14.11 | — | Use sparingly |
| Debug | `com.squareup.leakcanary:leakcanary-android` | **2.14** | 3.0-alpha-9 | |
| Lint/format | `com.diffplug.spotless` | **8.10.4** | — | |
| Lint/format | `com.pinterest.ktlint:ktlint-cli` | **1.8.0** | — | |
| Lint/format | `io.nlopez.compose.rules:ktlint` | **0.6.8** | — | |
| Lint/format | `com.slack.lint.compose:compose-lint-checks` | **1.6.1** | — | |
| Lint/format | detekt | 1.23.8 (compiled with Kotlin 2.0.21) | **2.0.0-alpha.6** (Kotlin 2.4.10, AGP 9.3.1) | [verified: detekt.dev compat]. Optional |
| Coverage | `org.jetbrains.kotlinx.kover` | **0.9.11** | — | |
| Build health | `com.autonomousapps.dependency-analysis` | **3.19.2** | — | |
| Licences | `com.mikepenz.aboutlibraries.plugin.android` / `aboutlibraries-compose-m3` | **15.3.0** | — | |
| Prayer | `com.batoulapps.adhan:adhan2` (KMP) | 0.0.7 | — | Licence not verified here; see the prayer report |

### 2.3 Language & UI

**Kotlin 2.4.21 via built-in Kotlin.**
- **How AGP 9 works:** AGP 9 bundles a *runtime dependency* on KGP. "If you use a KGP version lower than 2.2.10, Gradle will automatically upgrade" it. To go higher, AGP documents adding `org.jetbrains.kotlin:kotlin-gradle-plugin:<ver>` and `symbol-processing-gradle-plugin:<ver>` to the root `buildscript` classpath [verified: AGP 9.0.0 release notes].
- **Alternative:** a root `plugins { alias(libs.plugins.kotlin.jvm) apply false }` that carries the version achieves the same thing through Gradle conflict resolution. This is how Now in Android does it [verified: NiA build.gradle.kts].
- **Check:** after either approach, run `./gradlew buildEnvironment | grep kotlin-gradle-plugin` and confirm 2.4.21.
- **Compose compiler:** stays `org.jetbrains.kotlin.plugin.compose`, with the same version as Kotlin.
- **Fallback:** if Hilt or Room KSP misbehaves on 2.4.x, pin Kotlin **2.3.21**. Dagger 2.60 was "Updated Kotlin version to 2.3.21" [verified], and Kotlin-metadata readers normally accept one newer minor version [unverified].
- **kapt:** do not use it. `kotlin-kapt` is incompatible with built-in Kotlin, and only `com.android.legacy-kapt` remains [verified]. Every processor we need (Hilt, Room 3) runs on KSP.

**Material 3 / Expressive.**
- **Use BOM 2026.09.00, which gives material3 1.4.0.**
- **1.5.0 cycle:** expressive Buttons, FAB/FAB Menu, SplitButton, **FloatingToolbar**, flexible TopAppBars, ButtonGroup, BottomAppBar and list items all graduated from `@ExperimentalMaterial3ExpressiveApi`.
- **Still unstable:** `MaterialShapes` and `LoadingIndicator` promotions were *reverted*, and `AppBarWithSearch` went back to experimental [verified: material3 release notes].
- **Recommendation:**
  - Build the design system on 1.4.0 stable.
  - Wrap any expressive pieces we want behind `:core:designsystem` components. A good fit is a **FloatingToolbar** for the reader's bottom controls (audio, bookmark, night, lock).
  - Flip to material3 1.5.0 once a BOM ships it stable. Our public release is months out, so this is low risk.
  - A Quran reader should be calm. Expressive motion is optional polish.
- **Icons:** `material-icons-extended` is frozen at **1.7.8** (metadata lastUpdated 2025-02-12) [verified]. Import the ~30 Material Symbols we need as vector drawables instead of pulling in the huge extended artifact [inferred].

**Navigation 3 (choose) vs Navigation-Compose 2.x.**

| | Navigation 3 1.2.0 | Navigation-Compose 2.10.2 |
|---|---|---|
| Status | Stable since 1.0.0 (2025-11-19). Called "the AndroidX Compose first approach to navigation" [verified] | Stable, maintained |
| Back stack | Plain `SnapshotStateList` of `@Serializable NavKey`s. `NavBackStack` is `@Serializable`, so it survives process death [verified] | `NavController` graph DSL |
| Adaptive | **Scenes** (`SceneStrategy`). `adaptive-navigation3` 1.3.0 gives list-detail; Koin and Hilt both have Nav3 entry points | Needs extra glue |
| Deep links | 1.2.0 adds KMP `DeepLinkMatcher` [verified] | Mature |
| Fit for us | The reader is basically one entry that holds a page number. Index → reader → audio sheets are trivial to model. "Open Para 23" from a widget is a stack rewrite | Fine, but legacy direction |

Decision: **Navigation 3**, with `rememberViewModelStoreNavEntryDecorator()` and `hiltViewModel()` inside entries [verified: Hilt Jetpack doc]. Copy NiA's `NavigationState` + `Navigator` pattern: top-level tabs with a sub-stack each [verified: NiA `core/navigation`].

**Adaptive layouts and the two-page spread.**
- **When:** show two pages when `windowSizeClass` width is ≥ EXPANDED (or MEDIUM in landscape) **or** a vertical hinge is HALF_OPENED (book posture, from `androidx.window` 1.5.1 `FoldingFeature`).
- **What:** the pager pages over *spreads* (pairs) instead of single pages. In a mushaf the right-hand page has the lower number, so the spread is `[p+1 | p]` drawn in RTL order.
- **Pager direction:** wrap the reader in `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl)`. Swiping right then advances, exactly like the printed book, independent of the UI locale [inferred].
- **Large screens:** Android 16 forces resizability on ≥ sw600dp (§2.8), so this layout is *mandatory* there, not a nice-to-have.
- **Lists:** Index and Bookmarks use the Nav3 list-detail scene on tablets.

### 2.4 Dependency injection

| | **Hilt 2.60.1** (choose) | Koin 4.2.2 | Metro 1.4.5 |
|---|---|---|---|
| Graph validation | Compile time (KSP) | Runtime. A compiler plugin/annotations mode exists | Compile time (Kotlin compiler plugin) |
| AGP 9 / built-in Kotlin | Supported since 2.59. "minimum required AGP version to 9.0.0" in 2.59.1 [verified] | N/A (no codegen in DSL mode) | Kotlin-version-coupled ("N+.2" forward compat) [verified] |
| Jetpack integration | `hiltViewModel()` for Nav3, `HiltWorkerFactory`, `@AndroidEntryPoint` services (Media3) [verified] | `koin-compose-navigation3`, but "All APIs on this page are marked `@KoinExperimentalAPI`" [verified] | Via MetroX artefacts |
| KMP | No (Android/JVM) | Yes | Yes |
| Solo-dev ergonomics | Most docs, NiA reference, failures caught at build time | Least boilerplate | Fastest builds; newest |
| Risk | Slower KSP pass; Android-only | Runtime crashes on missing bindings | Breaks on Kotlin bumps until updated |

Decision: **Hilt** (High). Keep KMP portability by putting **no DI annotations in `:core:model`/`:core:domain`**. Those modules use plain constructors, and their bindings are written in Hilt `@Module`s inside the Android modules. If iOS ever happens, Metro can take over without rewriting the graph's shape [inferred].

### 2.5 Persistence

**Room 3 vs Room 2.8.**

| | Room 3.0.3 (choose) | Room 2.8.5 |
|---|---|---|
| Package | `androidx.room3.*`, artefacts `androidx.room3:room3-*`, Gradle plugin `androidx.room3` [verified] | `androidx.room.*` |
| Codegen | **KSP only, Kotlin only** [verified] | KSP/kapt/javac |
| API style | DAOs must be `suspend` or return `Flow`/custom types. `InvalidationTracker.Observer` replaced by `createFlow()`. `@TypeConverter` renamed `@ColumnTypeConverter` [verified] | Blocking DAOs allowed |
| Driver | **`SQLiteDriver` required**. SupportSQLite gone (an optional `room3-sqlite-wrapper` bridge exists) [verified] | SupportSQLite or driver |
| FTS | `@Fts3`, `@Fts4`, **`@Fts5`** (tokenizers unicode61/porter/ascii/**trigram**, external content). "For Android specifically the BundledSQLiteDriver supports FTS5" [verified: `Fts5.kt`] | FTS3/4 only |
| Prepackaged DB | **`createFromAsset` / `createFromFile` / `createFromInputStream` still exist** in `androidMain` [verified: `RoomDatabase.android.kt` L597–756] | Same |
| KMP | Android, JVM, iOS/macOS/…, plus **JS/WasmJs** [verified] | Android, JVM, native |
| Maturity | 3.0.0 stable 2026-07-01; three patch releases since | Very mature; NiA still on 2.8.3 |

Decision: **Room 3.0.3** (Medium-High). This is a greenfield, Kotlin-only, coroutine-first app, so Room 3's restrictions cost us nothing. We get FTS5 and full KMP. The risk is youth (about 3.5 months stable). The fallback is Room 2.8.5, which has the same concepts and changed imports.

**Driver: `BundledSQLiteDriver` (androidx.sqlite 2.7.1).**
- It is the only Android driver with FTS5, and it gives identical SQLite behaviour on API 24 through 37.
- **Cost:** `libsqliteJni.so` is 1,281,456 B (arm64-v8a) or 1,254,892 B (armeabi-v7a), uncompressed. App Bundles deliver only one ABI per device.
- **16 KB pages:** all four ABIs have `PT_LOAD` alignment `0x4000`, so they pass [verified: ELF program headers of `sqlite-bundled-android-2.7.1.aar`].

**Arabic and Urdu search design** (FTS gotcha found today):
- **The problem:** FTS5's `unicode61` treats only `L*`, `N*` and `Co` as token characters. "All other characters are separators", so Arabic harakat (`Mn`) would *split words*. `remove_diacritics` only handles **Latin** script [verified: sqlite.org/fts5.html].
- **The fix:** store a pipeline-normalised search column in which:
  - tashkeel, tatweel and Quranic annotation marks are stripped;
  - أ/إ/آ/ٱ fold to ا;
  - ى and ی fold to ي;
  - ة folds to ه;
  - ک folds to ك;
  - ۀ is normalised.
- **Indexes:**
  - `@Fts5(tokenizer = TOKENIZER_UNICODE61)` for whole-word search.
  - A second `@Fts5(tokenizer = TOKENIZER_TRIGRAM)` table for substring and partial-word search, which is good for Urdu and partial Arabic.
  - Both use `contentEntity` (external content) to save space.
- **Queries:** run the same normaliser over the query string in Kotlin [inferred]. Translation text gets its own unicode61 FTS table per language pack.

**Content DB build and versioning.** Room 3 exports schema JSON with `createSql`, `setupQueries` and `identityHash` [verified: `room3-migration` sources]. It validates a prepackaged DB, and when there is no `room_master_table` "this might an a pre-populated DB, we must validate" [verified: `RoomConnectionManager.kt`].

So:
1. `:core:content` declares entities and `exportSchema = true` (`room3 { schemaDirectory("$projectDir/schemas") }`).
2. `tools/content-pipeline/` (Python, pinned with uv, run with `python3 -I`) **creates the SQLite file by executing the exported `createSql` and `setupQueries` statements verbatim**, sets `PRAGMA user_version = <db version>`, inserts the data (Tanzil, QUL, our Para table and so on, per report 05), `VACUUM`s, and writes `content_meta(data_version, built_at, sources_json, sha256)`.
3. The DB opens with `Room.databaseBuilder<QuranContentDb>(ctx, "quran-content.db").createFromAsset("content/quran-content.db").fallbackToDestructiveMigration(dropAllTables = true).setDriver(BundledSQLiteDriver())`.
4. **To update content**, regenerate the DB, bump `@Database(version = N+1)` and ship. Room 3 compares the on-disk `user_version` with the new version, finds no migration path, deletes the file and **re-copies the asset**. This is the "copy destructive migration" in `PrePackagedCopySQLiteDriver.android.kt` [verified].
5. Never write user data into the content DB. Everything users create lives in `user.db`, keyed by `(surah, ayah)`, `page`, `para` and so on.

**User DB (`user.db`).** It uses normal Room 3 migrations with `@AutoMigration` where possible, and has migration tests (`room3-testing` 3.0.3 `MigrationTestHelper`). Sketch:

```kotlin
@Entity(tableName = "bookmark", indices = [Index("surah", "ayah"), Index("page")])
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: BookmarkKind,          // AYAH, PAGE, SURAH, PARA
    val surah: Int?, val ayah: Int?, // natural keys, never content rowids
    val page: Int?, val mushafId: String?, // page bookmarks are layout-specific ("indopak16")
    val folderId: Long?, val note: String?,
    val createdAt: Long, val updatedAt: Long,
)
@Entity(tableName = "reading_position") data class ReadingPositionEntity(
    @PrimaryKey val mushafId: String, val page: Int, val surah: Int, val ayah: Int, val at: Long)
@Entity(tableName = "khatm_plan") /* start, target date, daily pages */
@Entity(tableName = "reading_log")  /* day, pages read, seconds — for streaks */
@Entity(tableName = "download")     /* packId|reciterId, unit (surah), state, bytes, sha256, path */
```

Page bookmarks must store `mushafId`, because page 50 of the 16-line layout is not page 50 of the 15-line layout. To convert, map through the first ayah on that page.

**DataStore.**
- Use **one typed `DataStore<UserSettings>`** with a kotlinx.serialization JSON `Serializer`: theme, mushaf, translation IDs, reciter, repeat mode, prayer method, Hijri offset and so on. This needs no protobuf plugin and is KMP-capable [inferred].
- Use Preferences DataStore only for trivial flags.
- Expose settings through `SettingsRepository` as a `Flow<UserSettings>`.

**File storage layout** (no storage permissions needed anywhere):

```
databases/quran-content.db        read-only, re-copied on version bump (exclude from backup)
databases/user.db                 user data (INCLUDE in Auto Backup / device transfer)
files/datastore/settings.json     (INCLUDE in backup)
files/packs/<type>/<id>/<ver>/    translation/tafsir/word-by-word/timing DBs, alt-mushaf layout + fonts (exclude)
<getExternalFilesDir>/audio/<reciter>/<gapped|gapless>/SSSAAA.mp3 | SSS.mp3   (exclude; user can pick SD card via getExternalFilesDirs)
cache/media3/                     Media3 SimpleCache LRU for streaming (default 256 MB, user-settable)
```

Update `data_extraction_rules.xml` and `backup_rules.xml` (both already in the project) so that only `user.db` and settings are backed up. Also provide manual JSON export and import of bookmarks through SAF `ACTION_CREATE_DOCUMENT` / `ACTION_OPEN_DOCUMENT`. That avoids every storage permission [inferred].

**Optional packs: manifest contract.** The packs are hosted by us, for example on GitHub Releases or an object store. The manifest is plain JSON fetched by Ktor:

```json
{ "schema": 1, "packs": [
  { "id": "tr-ur-jalandhari", "type": "translation", "lang": "ur", "version": 3,
    "minAppContentVersion": 2, "url": "https://…/tr-ur-jalandhari-v3.db.zst",
    "bytes": 2140331, "sha256": "…", "license": "Public domain (PK)", "attribution": "…" } ] }
```

Each pack is an SQLite file opened by its own Room 3 database class. Room allows one class to open many files, for example one `TranslationDb` per language. Packs are installed atomically: download to `*.part`, verify sha256, rename, then flip the `installed_pack` row in `user.db` [inferred].

### 2.6 Media, background, network, images, widgets, location, sensors

| Concern | Decision | Key facts |
|---|---|---|
| Playback | **Media3 1.11.1**: `MediaLibraryService` (Android Auto) + ExoPlayer; `media3-ui-compose-material3` for the `MiniController` and progress slider | 1.10 made `MediaSessionService` a `LifecycleService`. **1.11: session methods must be called on the application looper** (getters throw otherwise); use `onConnectAsync()` [verified: media3 release notes]. minSdk 23. The gapped/gapless engine follows report 06 |
| Media HTTP | `media3-datasource-okhttp` sharing the app's single `OkHttpClient` | `media3-datasource-ktor` also exists in 1.11 [verified], but OkHttp's is the mature path |
| Downloads | **WorkManager 2.12.0** `CoroutineWorker` per (reciter, surah) unit; unique work names; HTTP Range resume; md5/sha256 verify (everyayah `000_checksum.md5`, report 06); `setForeground(… FOREGROUND_SERVICE_TYPE_DATA_SYNC)` | Android 15+: `dataSync` and `mediaProcessing` FGS may run "a total of 6 hours in a 24-hour period" [verified]. User-initiated data transfer jobs (`JobScheduler.setUserInitiated`) are "not currently available in WorkManager" [verified]; revisit if 6 h ever bites. We don't use Media3 `DownloadService` because report 06's `ResolvingDataSource` wants plain local files we can map, delete per surah and share between gapped and gapless |
| Networking | **Ktor 3.6.0 client** (OkHttp engine with `preconfigured = sharedOkHttp`) + `ktor-serialization-kotlinx-json` for manifests and catalogs; raw OkHttp 5.5.0 for byte streams | Ktor keeps `:core:network` KMP-portable; OkHttp is the engine and serves Media3 and downloads. Retrofit 3.0.0 isn't needed for about five JSON endpoints [inferred] |
| Serialization | kotlinx.serialization 1.11.0 (Nav keys, settings, manifests) | Required by Nav3 keys |
| Images | **Coil 3.6.3** only if reciter photos ship | Mushaf pages are fonts on Canvas (report 04). The fallback image pages (ALPHA_8) go through `BitmapFactory` directly, not Coil |
| Widgets | **Glance 1.2.0**: "Next prayer", "Ayah of the day", "Continue reading / khatm progress" | Override `providePreview` for generated previews (since 1.2.0-alpha01) [verified]. **Android 17 caps RemoteViews bitmap memory** at 1.5 × screen w × h × 4 [verified], so keep any rendered Arabic bitmap small |
| App Startup | `startup-runtime` 1.2.0, used only to disable WorkManager's default initializer (for `HiltWorkerFactory`) | Standard Hilt-Work pattern [unverified detail] |
| Splash | `core-splashscreen` 1.2.0 | |
| Location | **Platform `LocationManagerCompat.getCurrentLocation`** (in androidx.core 1.19.1 sources [verified]) with `ACCESS_COARSE_LOCATION` only; `FUSED_PROVIDER` on API 31+, else NETWORK/GPS; cache the last fix; **offline city picker** as a no-permission alternative | City-level accuracy is enough for prayer times and Qibla [inferred]. Avoiding GMS keeps F-Droid, Huawei and AOSP builds working. `play-services-location` 21.4.0 is only an optional `play`-flavor enhancement, and I recommend not adding it in v1 |
| Qibla | `TYPE_ROTATION_VECTOR` → `getRotationMatrixFromVector` → `remapCoordinateSystem` → `getOrientation`; fall back to `TYPE_GEOMAGNETIC_ROTATION_VECTOR` (no gyro) or accel+mag; true north = magnetic + `GeomagneticField(lat,lon,alt,t).declination`; great-circle bearing to the Kaaba; low-pass filter; a calibration prompt on `SENSOR_STATUS_ACCURACY_LOW` | Standard Android APIs [unverified, long-stable]. Cheap Pakistani phones often lack a gyroscope, so the fallback path is required [inferred] |
| Hijri | **`android.icu.util.IslamicCalendar`** + `setCalculationType(ISLAMIC_UMALQURA)`: class and method "Added in API level 24" [verified]; plus a **user offset of ±2 days** (Pakistan's moon-sighting date often differs from Umm al-Qura) [inferred] | Removes the main argument for minSdk 26 (`java.time.chrono.HijrahDate`) |
| Prayer times | `adhan2` 0.0.7 (KMP), wrapped in `:core:prayer` | Defaults and method choice belong to the prayer report |

### 2.7 Quality toolchain

| Concern | Decision | Notes |
|---|---|---|
| Baseline profiles | `:baselineprofile` (`com.android.test` + `androidx.baselineprofile` 1.5.0) producing profiles for `:app`; journeys: cold start → home → open last page → swipe 10 pages → open index → play ayah | Compose apps get large startup and jank wins from these [unverified magnitude]. Plugin 1.5.0 works with AGP 9's new DSL [verified] |
| Macrobenchmark | Same module: `StartupBenchmark` (cold/warm, `CompilationMode.Partial(BaselineProfile)`), `PageTurnBenchmark` (`FrameTimingMetric`) | Budget: cold start < 500 ms on a Pixel 6a-class device, page-turn p90 frame < 16 ms; also run on one low-end Android 11 device [inferred targets] |
| R8 | `optimization { enable = true }` (AGP ≥ 9.3 DSL); full mode is the default since AGP 8.0; optimized resource shrinking is always on in 9.3 [verified] | AGP 9 defaults: `strictFullModeForKeepRules=true`, only `proguard-android-optimize.txt` allowed [verified]. Add keep rules for `@Serializable` Nav keys only if needed (the library ships consumer rules) [unverified] |
| StrictMode | `StrictMode.setThreadPolicy(detectAll().penaltyLog())` + VM policy in debug `Application.onCreate` | Watch the `autoStoreLocales` main-thread read [verified warning in the app-languages doc] |
| LeakCanary | 2.14, `debugImplementation` | 3.0 is still alpha |
| Formatting | **Spotless 8.10.4 + ktlint 1.8.0 + `io.nlopez.compose.rules:ktlint` 0.6.8** | NiA uses Spotless+ktlint [verified] |
| Lint | Android Lint with `warningsAsErrors` on CI, `abortOnError`, a baseline file; **`com.slack.lint.compose:compose-lint-checks` 1.6.1** | |
| detekt | Optional, non-blocking: **2.0.0-alpha.6** is the first built against Kotlin 2.4.10 / AGP 9.3.1; 1.23.8 was built with Kotlin 2.0.21 [verified] | Skip until 2.0 is stable, unless you want complexity rules |
| Compose metrics | `composeCompiler { reportsDestination = …; metricsDestination = … }` behind a `-PcomposeReports` flag; `stabilityConfigurationFiles` listing `:core:model` types (classes from non-Compose modules are otherwise unstable) | NiA ships a `compose_compiler_config.conf` [verified file exists] |
| Screenshot tests | **Roborazzi 1.76.0** on Robolectric 4.17 (JVM, fast, CI-friendly) for the design system, the index rows and **mushaf page golden images** (critical: catches font and layout regressions) | Compose Preview Screenshot Testing is "Experimental…subject to change substantially", and from AGP 9.5.0-alpha03 it moves to "AGP test suites" [verified]. Re-evaluate in 2027 |
| Unit and integration tests | JUnit4 + **Truth 1.4.5** + **Turbine 1.2.1** + `kotlinx-coroutines-test` 1.11.0; **fakes over MockK** (NiA's `:core:testing`/`*-test` pattern); `room3-testing` for migrations; `work-testing` 2.12.0; `media3-test-utils(-robolectric)` 1.11.1; `mockwebserver3` 5.5.0 | Content-DB invariant tests on JVM: 6,236 ayahs; 114 surahs; Para starts match the report 05 table; 7 manzils; every page has 16 lines (16-line layout) |
| UI tests | Compose `ui-test-junit4` (BOM) + Hilt testing (`hilt-android-testing` 2.60.1); Gradle Managed Devices for API 24/30/36 | |
| Coverage | Kover 0.9.11 (aggregate), no hard gate early | |
| Build health | `dependency-analysis` 3.19.2 (unused/misdeclared deps); configuration cache **on** (already `true`), build cache on, parallel on | NiA also runs `org.gradle.isolated-projects=true` [verified]. Optional for us |

**GitHub Actions CI** (action versions from `git ls-remote --tags` [verified]: checkout v7.0.1, setup-java v6.1.0, gradle/actions v6.4.0, upload-artifact v7.0.2):

```yaml
name: ci
on: { pull_request: {}, push: { branches: [main] } }
concurrency: { group: ci-${{ github.ref }}, cancel-in-progress: true }
jobs:
  check:
    runs-on: ubuntu-latest
    timeout-minutes: 45
    steps:
      - uses: actions/checkout@v7
      - uses: actions/setup-java@v6
        with: { distribution: temurin, java-version: '21' }
      - uses: gradle/actions/setup-gradle@v6
        with: { cache-read-only: ${{ github.ref != 'refs/heads/main' }} }
      - run: ./gradlew spotlessCheck lintFossDebug lintPlayDebug testFossDebugUnitTest verifyRoborazziFossDebug --continue
      - run: ./gradlew :app:bundlePlayRelease :app:assembleFossRelease   # unsigned on PRs
      - uses: actions/upload-artifact@v7
        if: always()
        with: { name: reports, path: '**/build/reports/**', retention-days: 7 }
  # nightly (schedule): connectedCheck on Gradle Managed Device (KVM), :baselineprofile:generate, macrobenchmarks
  # release (tag v*): sign with upload key from secrets (Play App Signing), upload AAB artifact
```

### 2.8 Platform & Play requirements (2026 → 2027)

| Requirement | Rule (verified) | Impact on Criterion | Action |
|---|---|---|---|
| Target API | New apps and updates must target **API 36** since **2026-08-31**; extension to **2026-11-01** [verified] | Already 36 | Keep 36; schedule 37 for H1 2027 (no 2027 date published yet [verified absent]) |
| Edge-to-edge | Targeting 36: `windowOptOutEdgeToEdgeEnforcement` "deprecated and disabled" [verified] | Reader draws under the system bars | `enableEdgeToEdge()`; immersive reader (`WindowInsetsControllerCompat.hide(systemBars)`, transient swipe); apply insets in toolbars |
| Predictive back | Targeting 36 on Android 16+: back animations on by default; "`onBackPressed` is not called" [verified] | Nav3 handles back; sheets and dialogs must use `BackHandler`/`PredictiveBackHandler` | Never override `onBackPressed` |
| Large-screen orientation | Targeting 36: orientation, resizability and aspect-ratio restrictions "no longer apply on displays with smallest width >= 600dp"; the opt-out property "won't apply when targeting API level 37" [verified] | The reference app's **orientation lock can't work on tablets or unfolded foldables** | Lock = rotation lock on phones only (hide the toggle when sw ≥ 600dp) **plus** a "page-turn lock" (`userScrollEnabled=false`) everywhere. Don't rely on `PROPERTY_COMPAT_ALLOW_RESTRICTED_RESIZABILITY`. Make two-page landscape first-class |
| Android 17 background audio | Background playback, audio focus and volume changes need a running FGS with while-in-use capability, **or** "the exact alarm permission and … `USAGE_ALARM` audio streams" [verified: behavior-changes-17] | Adhan at prayer time while the app is backgrounded | Adhan player uses `AudioAttributes.USAGE_ALARM` + `SCHEDULE_EXACT_ALARM`, started from the alarm broadcast; recitation uses a `mediaPlayback` FGS (Media3) |
| Exact alarms | `SCHEDULE_EXACT_ALARM` not pre-granted for apps targeting 33+; check `canScheduleExactAlarms()`; deep-link `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`; revocation cancels all exact alarms [verified]. Play: `USE_EXACT_ALARM` only for "an alarm or timer app" or "a calendar app that shows event notifications", and it needs a Console declaration [verified] | Adhan alarms | Request **`SCHEDULE_EXACT_ALARM`** with an explainer screen; `setAlarmClock()` for adhan (never deferred) or `setExactAndAllowWhileIdle()`; fall back to inexact plus a notice if denied; re-schedule on `BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED` and `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` |
| Full-screen intent | Auto-granted only for "setting an alarm" or "receiving phone or video calls" apps [verified] | — | Don't use FSI; use a high-importance heads-up notification with the adhan sound |
| FGS types | Must declare types (API 34+) and declare them in Play Console → App content [verified]; Android 15: no `mediaPlayback`/`dataSync` FGS start from `BOOT_COMPLETED` [verified] | `mediaPlayback` (recitation), `dataSync` (downloads) | Declare both. On boot, only reschedule alarms |
| Notifications | `POST_NOTIFICATIONS` runtime permission on API 33+ [unverified, long-standing] | Adhan, reminders, download progress | Ask in context (on enabling adhan); channel per adhan sound (a channel's sound is immutable after creation [unverified]) |
| 16 KB pages | Apps targeting 35+ must support 16 KB; "Starting February 1, 2027, if your app updates don't support 16 KB … you won't be able to release these updates"; only native code matters [verified] | Only `libsqliteJni.so` | Already aligned `0x4000` [verified]. Add a CI check (`zipalign -c -P 16`) for future native libraries |
| App size | Base module **500 MB**, feature modules 500 MB, asset packs 1.5 GB each, 4 GB cumulative install-time, 30 GB on-demand, 100 packs; cellular warning > 200 MB [verified: Play Help 9859372] | Base about 15–25 MB including the content DB, one translation, fonts and the SQLite `.so` [inferred] | No PAD in v1; self-hosted packs (works for non-Play builds) |
| Per-app language | `generateLocaleConfig = true` + `res/resources.properties` `unqualifiedResLocale=en-US` (API 33+); for ≤ 32, `AppCompatDelegate.setApplicationLocales` + `autoStoreLocales`, and **Compose apps must extend `AppCompatActivity`** [verified] | English, Urdu, Arabic UI (RTL) | Use AppCompat 1.8.0 and an AppCompat DayNight NoActionBar theme parent; `android:supportsRtl="true"` (already set) |
| Language splits | Apps that switch language in-app must set `enableSplit = false` or download languages on demand, "to prevent crashes due to missing resources" [verified] | Urdu picked on an English phone | `bundle { language { enableSplit = false } }` |
| Photo/media permissions | — | None needed | No `READ_MEDIA_*` and no storage permissions; SAF for import and export |
| Android 17 misc. (when targeting 37) | ECH and Certificate Transparency on by default; `static final` reflection blocked; new `ACCESS_LOCAL_NETWORK` [verified] | Low impact | Re-test the CDN TLS chain at the 37 migration |

**Manifest permission set (v1):** `INTERNET`, `ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `FOREGROUND_SERVICE_DATA_SYNC`, `POST_NOTIFICATIONS`, `WAKE_LOCK`, `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`, `ACCESS_COARSE_LOCATION`. **Not** `USE_EXACT_ALARM`, `USE_FULL_SCREEN_INTENT`, fine location, storage or media permissions. The Data safety form then reports approximate location used on-device only, with no collection [inferred].

### 2.9 minSdk: keep 24 or raise to 26?

StatCounter, Pakistan mobile, **September 2026** [verified: gs.statcounter.com CSV export]:

| Version | Share | Version | Share |
|---|---|---|---|
| 16 | 21.39% | 9 | 3.44% |
| 15 | 16.44% | 8.1 | 2.17% |
| 13 | 14.74% | 17 | 1.22% |
| 14 | 11.95% | 6.0 | 0.77% |
| 11 | 11.46% | 8.0 | 0.66% |
| 12 | 8.82% | 5.x/4.4 | 0.45% |
| 10 | 5.71% | **7.1 + 7.0 (API 25/24)** | **0.58%** |

| Option | Gains | Costs |
|---|---|---|
| **Keep 24** (choose) | +0.58% of PK traffic. These are probably the oldest phones of elderly daily readers, who are exactly this app's audience [inferred] | Core-library desugaring for `java.time` and kotlinx-datetime (desugar_jdk_libs 2.1.5; NiA does this [verified]); QA on one API 24/25 emulator; the font-feature kashida spike from report 04 must pass on API 24–30 |
| Raise to 26 | No desugaring; `java.time` native; adaptive icons only | Loses ~0.6%. HijrahDate isn't needed anyway (ICU IslamicCalendar is API 24) [verified] |

The library floor is already 24 (Navigation3-UI 1.2.0 and WorkManager 2.12.0) [verified: AAR manifests], so 24 costs us nothing extra on dependencies.

**Decision: keep 24** (Medium-High). Re-evaluate at v1.0 against Play Console's device catalogue: if API 24–25 is below 0.3% of installs, raise to 26.

### 2.10 Modularisation blueprint

Principles:
1. **The dependency arrow points down.** `:app` → `:feature:*` → `:core:*`.
2. **Features never depend on features.** Cross-feature navigation goes through `NavKey`s defined in `:core:navigation`. This is simpler than NiA's per-feature `api`/`impl` split, which pays off only with many teams [verified: NiA uses `:feature:x:api` + `:feature:x:impl`].
3. **Pure-Kotlin modules where possible.** These are `:core:model`, `:core:common` and `:core:domain`. That gives fast unit tests and makes them KMP-convertible.
4. **Each `:core:*` module exposes interfaces.** Implementations stay `internal`, and Hilt modules bind them.

```
                                  :app  (Application, MainActivity, NavDisplay host, flavors play/foss, Glance receivers, DI root)
                                    │
   ┌──────────┬─────────┬──────────┬─┴────────┬───────────┬─────────┬──────────┬──────────┬───────────┐
:feature:home :feature:reader :feature:index :feature:audio :feature:search :feature:bookmarks :feature:prayer :feature:qibla :feature:adhkar :feature:settings
   └─────── all features may use: :core:ui :core:designsystem :core:navigation :core:model :core:domain :core:data (+ specific: reader→:core:mushaf-render, audio→:core:audio-api)
                                    │
                         :core:data (repositories, offline-first)
          ┌──────────────┬──────────┼────────────┬──────────────┬───────────┐
   :core:content   :core:database  :core:datastore  :core:network  :core:download  :core:prayer
   (Room3 RO,      (Room3 user)    (typed DS)       (Ktor/OkHttp)  (WorkManager)   (adhan2, ICU Hijri, qibla math)
    assets DB)
                         :core:audio (Media3 MediaLibraryService, playlist builder, ayah tracker)  ← exposes :core:audio-api (controller interface + state)
                         :core:notifications (channels, adhan alarm scheduler, reminders)
                         :core:location (LocationProvider + city DB)
   leaf: :core:model (pure Kotlin)  :core:common (dispatchers, AppError, Outcome)  :core:testing (fakes, MainDispatcherRule)
   :baselineprofile (com.android.test → :app)        build-logic/convention (included build)        tools/content-pipeline (Python, not Gradle)
```

| Module | Type (convention plugin) | Responsibility | May depend on |
|---|---|---|---|
| `:core:model` | `criterion.jvm.library` | `AyahKey`, `Surah`, `Para` (with Indo-Pak names), `Manzil`, `Ruku`, `MushafId`, `Page`, `PageLayout`, `Reciter`, `Bookmark`, `UserSettings` | none (kotlinx.serialization, immutable collections) |
| `:core:common` | jvm.library | `AppError`, `Outcome<T>`, dispatcher qualifiers, `Clock` | model |
| `:core:domain` | jvm.library | Use cases spanning repositories: `BuildPlaylist(range)`, `ResolveParaRange`, `ComputeKhatmPlan`, `NextPrayer` | model, common |
| `:core:content` | `criterion.android.library` + `criterion.room3` | Read-only content DB (text, normalised search text, FTS5, layouts, para/manzil/ruku/hizb/sajda tables), asset copy, `content_meta` | model, common |
| `:core:database` | android.library + room3 | User DB, DAOs, migrations, schema JSON | model, common |
| `:core:datastore` | android.library | `DataStore<UserSettings>` + serializer | model, common |
| `:core:network` | android.library | Ktor client, shared `OkHttpClient`, pack manifest API, CDN mirror list | model, common |
| `:core:download` | android.library + hilt | Workers, storage locations, checksum, pack installer, `DownloadRepository` | network, database, model, common |
| `:core:data` | android.library + hilt | `QuranRepository`, `MushafRepository`, `BookmarkRepository`, `RecitationRepository`, `PackRepository`, `SettingsRepository`, `SearchRepository` | content, database, datastore, network, download, model, common, domain |
| `:core:audio` / `:core:audio-api` | android.library + hilt | Service, `ResolvingDataSource`, timings, `MediaController` wrapper exposing `StateFlow<PlaybackUiState>` | data, model (api: model only) |
| `:core:mushaf-render` | `criterion.android.library.compose` | Report 04's Canvas engine: fonts, glyph runs, ornaments, hit-testing, highlight layer | model, designsystem |
| `:core:prayer` | android.library | Prayer-time calculation wrapper, `IslamicCalendar` Hijri with offset, Qibla bearing | model, common |
| `:core:location` | android.library + hilt | `LocationProvider` (platform impl), city DB | model, common |
| `:core:notifications` | android.library + hilt | Channels, adhan `AlarmManager` scheduler, reminder workers | prayer, data, model |
| `:core:designsystem` | library.compose | Theme (light/dark/sepia/night-mushaf), typography incl. Urdu Nastaliq UI font, components, icons | none |
| `:core:ui` | library.compose | Shared composables bound to model types (SurahRow, ParaBadge, AyahNumber, DownloadButton) | designsystem, model |
| `:core:navigation` | library.compose | All `@Serializable NavKey`s, `NavigationState`, `Navigator` (NiA pattern) | model |
| `:core:testing` | android.library | Fakes for every repository, `MainDispatcherRule`, test content DB fixture | data (interfaces), model |
| `:feature:*` | `criterion.android.feature` (library + compose + hilt + standard deps) | Screen(s) + ViewModel(s) + `EntryProviderScope.xxxEntries()` | core only |
| `:app` | `criterion.android.application(.compose)` + hilt + baselineprofile + aboutlibraries | Wiring, flavors, manifest, Glance widgets (or `:feature:widgets`) | everything |
| `:baselineprofile` | `criterion.android.test` | Profile generator + macrobenchmarks | app |

**Convention plugins** (`build-logic/convention`, modelled on NiA [verified]):

| Plugin | What it does |
|---|---|
| `criterion.android.application` / `.compose` | App module setup |
| `criterion.android.library` / `.compose` | Sets compileSdk 37, minSdk 24, Java 17, desugaring, Kotlin compiler options, lint |
| `criterion.android.feature` | Feature module setup |
| `criterion.jvm.library` | Pure-Kotlin module setup |
| `criterion.hilt` | Applies KSP + Hilt and adds `ksp(hilt-compiler)` |
| `criterion.room3` | Applies KSP + `androidx.room3`, sets the schema dir, adds `room3-runtime` + `sqlite-bundled` + `ksp(room3-compiler)` |
| `criterion.android.lint` | Shared lint configuration |
| `criterion.spotless` | Formatting |
| `criterion.root` | A check task that **fails if any `:feature:*` depends on another `:feature:*`** |

**KMP decision: "KMP-ready, Android-only"** (Medium-High).

| | Full KMP now | KMP-ready (choose) |
|---|---|---|
| Build | `com.android.kotlin.multiplatform.library` per core module; it can't coexist with `com.android.library` in a module [verified: built-in-Kotlin doc]; Xcode toolchain; slower builds | Plain Android/JVM modules |
| Libraries | Room 3, DataStore, Ktor, kotlinx-*, Navigation3 runtime, Coil 3 and Koin/Metro are all KMP [verified: Room 3, Nav3 notes] | **The same libraries**, picked now, so a later move is mechanical |
| UI | Compose Multiplatform 1.12.2 exists [verified: Maven Central]; the Nav3 UI artefact itself has no KMP targets (JetBrains publishes `org.jetbrains.androidx.navigation3`) [verified] | Android Compose |
| Payoff | Only if iOS ships | Avoids about 20–30% extra build and infrastructure work for a solo developer [inferred] |

A KMP lever worth noting [inferred]: report 04 proposes **precomputing word x-positions and kashida values offline**. That makes the mushaf renderer a pure "draw glyph run at (x, y)" problem, so it is portable to Compose Multiplatform or Skia on iOS. Android-specific Minikin tricks would not port.

### 2.11 Architecture pattern

**Layers.** UI (Compose + ViewModel) → optional domain use cases → repositories (`:core:data`) → data sources (Room, DataStore, network, files, Media3 controller).

**UDF contract (every screen):**

```kotlin
@HiltViewModel
class IndexViewModel @Inject constructor(quran: QuranRepository, settings: SettingsRepository) : ViewModel() {
    private val tab = MutableStateFlow(IndexTab.SURAH)
    val uiState: StateFlow<IndexUiState> =
        combine(tab, quran.surahs(), quran.paras(), quran.manzils(), settings.lastRead()) { t, s, p, m, last ->
            IndexUiState.Ready(t, s.toImmutableList(), p.toImmutableList(), m.toImmutableList(), last)
        }.catch { emit(IndexUiState.Error(it.toAppError())) }
         .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), IndexUiState.Loading)
    fun onAction(a: IndexAction) { when (a) { is IndexAction.SelectTab -> tab.value = a.tab } }
}
@Composable fun IndexRoute(onOpenPage: (MushafId, Int) -> Unit, vm: IndexViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    IndexScreen(state, vm::onAction, onOpenPage)   // stateless, previewable, screenshot-tested
}
```

**Rules:**
- **State:** one immutable `UiState` per screen, using `kotlinx.collections.immutable` lists so Compose skips recomposition. User intents arrive through `onAction`.
- **One-off events** (snackbar, navigation) are modelled as state or callbacks. Do not use `SharedFlow` or `Channel` for them [unverified: this is Google's architecture guidance from memory].
- **Errors:** sealed `AppError { Network, NoSpace, Integrity, NotFound, PermissionDenied, Unknown }`. Repositories return `Outcome<T>` for one-shot calls and `Flow<T>` for observables. The UI maps errors to localised strings in `:core:ui`.
- **Threading:** inject `@IoDispatcher`. Room 3 takes `setQueryCoroutineContext(ioDispatcher)` [verified API]. Media3 calls run on the main looper only [verified 1.11 rule].
- **Offline-first:**
  - Reading, index and search never touch the network.
  - Audio resolves local file → cache → CDN with mirror failover (report 06).
  - The pack manifest is cached and refreshed weekly by a periodic worker on unmetered networks.
- **Reader hot path:**
  - `MushafRepository.page(mushafId, n): PageLayout` reads precomputed lines and words from the content DB.
  - An LRU (±3 pages) of `PageLayout` and prepared text runs sits in a `@Singleton` cache.
  - `HorizontalPager(beyondViewportPageCount = 1)` draws pages via `drawWithCache`.
  - Highlights are a separate layer, invalidated by `PlaybackUiState.currentAyah` (report 04 §9).
  - The current page is persisted (debounced 1 s) to `reading_position`.
- **Content versioning summary:**

  | What | Where | How it updates |
  |---|---|---|
  | Content | asset DB `version` | Copy-destructive re-copy on version bump |
  | User data | `user.db` | Real migrations + tests |
  | Packs | Per-pack `version` + sha256 + `minAppContentVersion` | Atomic install |
  | Fonts | Under `assets/fonts/<name>-<ver>.ttf` | Ship a new file name |

  Any user-data reference to a page is layout-scoped (`mushafId`) and can be re-derived from `(surah, ayah)`.

### 2.12 Proposed `gradle/libs.versions.toml`

All versions are verified against live Maven metadata on 2026-10-19 unless marked otherwise.

```toml
[versions]
# Toolchain
agp = "9.4.1"
kotlin = "2.4.21"
ksp = "2.3.12"
# AndroidX
androidxActivity = "1.13.0"
androidxAppCompat = "1.8.0"
androidxCore = "1.19.1"
androidxCoreSplashscreen = "1.2.0"
androidxComposeBom = "2026.09.00"        # Compose 1.12.1, material3 1.4.0, adaptive 1.3.0
androidxLifecycle = "2.11.0"
androidxNavigation3 = "1.2.0"
androidxHilt = "1.4.0"
androidxRoom3 = "3.0.3"
androidxSqlite = "2.7.1"
androidxDataStore = "1.2.1"
androidxMedia3 = "1.11.1"
androidxWork = "2.12.0"
androidxGlance = "1.2.0"
androidxStartup = "1.2.0"
androidxWindow = "1.5.1"
androidxProfileInstaller = "1.4.1"
androidxBenchmark = "1.5.0"              # also androidx.baselineprofile plugin
androidxTest = "1.7.0"                   # core, runner, rules
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
# Network / images
ktor = "3.6.0"
okhttp = "5.5.0"
coil = "3.6.3"
# Other
desugarJdkLibs = "2.1.5"
adhan2 = "0.0.7"                         # licence: confirm in prayer report
aboutLibraries = "15.3.0"
playReview = "2.0.2"                     # play flavor only
playAppUpdate = "2.1.0"                  # play flavor only
# Test
junit4 = "4.13.2"
truth = "1.4.5"
turbine = "1.2.1"
robolectric = "4.17"
roborazzi = "1.76.0"
mockk = "1.14.11"
leakcanary = "2.14"
# Quality plugins
spotless = "8.10.4"
ktlint = "1.8.0"
composeRules = "0.6.8"
slackComposeLint = "1.6.1"
kover = "0.9.11"
dependencyAnalysis = "3.19.2"

[libraries]
# Build-logic (compileOnly in build-logic/convention; also used for classpath pinning)
android-gradlePlugin = { group = "com.android.tools.build", name = "gradle", version.ref = "agp" }
kotlin-gradlePlugin = { group = "org.jetbrains.kotlin", name = "kotlin-gradle-plugin", version.ref = "kotlin" }
compose-gradlePlugin = { group = "org.jetbrains.kotlin", name = "compose-compiler-gradle-plugin", version.ref = "kotlin" }  # verified (also published for 2.4.21 line)
ksp-gradlePlugin = { group = "com.google.devtools.ksp", name = "symbol-processing-gradle-plugin", version.ref = "ksp" }
room3-gradlePlugin = { group = "androidx.room3", name = "room3-gradle-plugin", version.ref = "androidxRoom3" }
android-desugarJdkLibs = { group = "com.android.tools", name = "desugar_jdk_libs", version.ref = "desugarJdkLibs" }

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
androidx-navigation3-runtime = { group = "androidx.navigation3", name = "navigation3-runtime", version.ref = "androidxNavigation3" }
androidx-navigation3-ui = { group = "androidx.navigation3", name = "navigation3-ui", version.ref = "androidxNavigation3" }

hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
hilt-android-testing = { group = "com.google.dagger", name = "hilt-android-testing", version.ref = "hilt" }
androidx-hilt-lifecycle-viewmodel-compose = { group = "androidx.hilt", name = "hilt-lifecycle-viewmodel-compose", version.ref = "androidxHilt" }
androidx-hilt-work = { group = "androidx.hilt", name = "hilt-work", version.ref = "androidxHilt" }
androidx-hilt-compiler = { group = "androidx.hilt", name = "hilt-compiler", version.ref = "androidxHilt" }

androidx-room3-runtime = { group = "androidx.room3", name = "room3-runtime", version.ref = "androidxRoom3" }
androidx-room3-compiler = { group = "androidx.room3", name = "room3-compiler", version.ref = "androidxRoom3" }
androidx-room3-testing = { group = "androidx.room3", name = "room3-testing", version.ref = "androidxRoom3" }
androidx-sqlite-bundled = { group = "androidx.sqlite", name = "sqlite-bundled", version.ref = "androidxSqlite" }
androidx-datastore = { group = "androidx.datastore", name = "datastore", version.ref = "androidxDataStore" }
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "androidxDataStore" }

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
androidx-test-core = { group = "androidx.test", name = "core", version.ref = "androidxTest" }
androidx-test-runner = { group = "androidx.test", name = "runner", version.ref = "androidxTest" }
androidx-test-rules = { group = "androidx.test", name = "rules", version.ref = "androidxTest" }
androidx-test-ext-junit = { group = "androidx.test.ext", name = "junit", version.ref = "androidxTestExtJunit" }
androidx-test-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "androidxEspresso" }
androidx-test-uiautomator = { group = "androidx.test.uiautomator", name = "uiautomator", version.ref = "androidxUiAutomator" }

kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "kotlinxCoroutines" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "kotlinxCoroutines" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
kotlinx-datetime = { group = "org.jetbrains.kotlinx", name = "kotlinx-datetime", version.ref = "kotlinxDatetime" }
kotlinx-collections-immutable = { group = "org.jetbrains.kotlinx", name = "kotlinx-collections-immutable", version.ref = "kotlinxCollectionsImmutable" }

ktor-client-core = { group = "io.ktor", name = "ktor-client-core", version.ref = "ktor" }
ktor-client-okhttp = { group = "io.ktor", name = "ktor-client-okhttp", version.ref = "ktor" }
ktor-client-content-negotiation = { group = "io.ktor", name = "ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-kotlinx-json = { group = "io.ktor", name = "ktor-serialization-kotlinx-json", version.ref = "ktor" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-mockwebserver3 = { group = "com.squareup.okhttp3", name = "mockwebserver3", version.ref = "okhttp" }
coil-compose = { group = "io.coil-kt.coil3", name = "coil-compose", version.ref = "coil" }
coil-network-okhttp = { group = "io.coil-kt.coil3", name = "coil-network-okhttp", version.ref = "coil" }   # verified 3.6.3

adhan2 = { group = "com.batoulapps.adhan", name = "adhan2", version.ref = "adhan2" }
aboutlibraries-compose-m3 = { group = "com.mikepenz", name = "aboutlibraries-compose-m3", version.ref = "aboutLibraries" }
play-review-ktx = { group = "com.google.android.play", name = "review-ktx", version.ref = "playReview" }
play-app-update-ktx = { group = "com.google.android.play", name = "app-update-ktx", version.ref = "playAppUpdate" }

junit4 = { group = "junit", name = "junit", version.ref = "junit4" }
truth = { group = "com.google.truth", name = "truth", version.ref = "truth" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }
roborazzi = { group = "io.github.takahirom.roborazzi", name = "roborazzi", version.ref = "roborazzi" }
roborazzi-compose = { group = "io.github.takahirom.roborazzi", name = "roborazzi-compose", version.ref = "roborazzi" }  # verified 1.76.0
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
leakcanary-android = { group = "com.squareup.leakcanary", name = "leakcanary-android", version.ref = "leakcanary" }
slack-compose-lint = { group = "com.slack.lint.compose", name = "compose-lint-checks", version.ref = "slackComposeLint" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
android-test = { id = "com.android.test", version.ref = "agp" }
android-lint = { id = "com.android.lint", version.ref = "agp" }
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }          # also lifts AGP's bundled KGP to 2.4.21
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
# project convention plugins (no version)
criterion-android-application = { id = "criterion.android.application" }
criterion-android-application-compose = { id = "criterion.android.application.compose" }
criterion-android-library = { id = "criterion.android.library" }
criterion-android-library-compose = { id = "criterion.android.library.compose" }
criterion-android-feature = { id = "criterion.android.feature" }
criterion-jvm-library = { id = "criterion.jvm.library" }
criterion-hilt = { id = "criterion.hilt" }
criterion-room3 = { id = "criterion.room3" }
criterion-android-test = { id = "criterion.android.test" }
criterion-root = { id = "criterion.root" }
```

Verification notes for the TOML:
- **Plugin markers verified:** `androidx.room3` (Google Maven), `io.github.takahirom.roborazzi`, `com.google.dagger.hilt.android`, `com.mikepenz.aboutlibraries.plugin.android` and foojay 1.0.0.
- **Artifacts verified after drafting:** `compose-compiler-gradle-plugin`, `coil-network-okhttp` 3.6.3, `roborazzi-compose` 1.76.0 and `lifecycle-service` 2.11.0 all exist on Maven Central / Google Maven. Every coordinate in the TOML now resolves.

**Root build and gradle.properties sketch:**

```kotlin
// settings.gradle.kts
pluginManagement { includeBuild("build-logic"); repositories { google(); mavenCentral(); gradlePluginPortal() } }
plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }
// include(":app", ":baselineprofile", ":core:model", … , ":feature:reader", …)

// build.gradle.kts (root) — loads every plugin in one classloader and lifts KGP/KSP above AGP's pinned 2.2.10
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.kotlin.jvm) apply false            // brings KGP 2.4.21 (NiA technique)
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room3) apply false
    alias(libs.plugins.baselineprofile) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.aboutlibraries.android) apply false
    alias(libs.plugins.dependency.analysis)
    alias(libs.plugins.criterion.root)
}
// Documented alternative (AGP 9.0 notes): buildscript { dependencies { classpath(libs.kotlin.gradlePlugin); classpath(libs.ksp.gradlePlugin) } }
// — then drop the version from those plugin aliases (a plugin already on the classpath cannot be requested with a version).
```

```properties
# gradle.properties additions
org.gradle.jvmargs=-Xmx4g -XX:+UseG1GC -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
android.experimental.enableScreenshotTest=false
```

**App module essentials:**

```kotlin
android {
    namespace = "com.chughtai.criterion"
    defaultConfig { applicationId = "com.chughtai.criterion"; targetSdk = 36 }   // compileSdk 37 & minSdk 24 come from convention
    androidResources { generateLocaleConfig = true }                             // + res/resources.properties: unqualifiedResLocale=en-US
    bundle { language { enableSplit = false } }                                  // in-app language picker
    buildTypes { release { optimization { enable = true } } }
    flavorDimensions += "distribution"
    productFlavors { create("foss") { dimension = "distribution" }; create("play") { dimension = "distribution" } }
}
dependencies { "playImplementation"(libs.play.review.ktx); "playImplementation"(libs.play.app.update.ktx) }
```

---

## 3. Recommendations

1. **Do the toolchain PR before any feature work.** Move to AGP 9.4.1, Gradle 9.8.1, Kotlin 2.4.21 (KGP lifted via the root plugins block), KSP 2.3.12, BOM 2026.09.00, compileSdk 37 (keep targetSdk 36), Java 17 bytecode and R8 on. Verify with `./gradlew buildEnvironment` and `assembleRelease`. *Rationale:* every library below assumes these versions, and a one-off upgrade on an empty project is the cheapest it will ever be. **Confidence: High.**
2. **Use Kotlin 2.4.21, with 2.3.21 as a documented fallback.** *Rationale:* 2.4 is the supported line (security support to Dec 2027). Dagger 2.60 was built against 2.3.21, which leaves a small metadata-compatibility risk. **Confidence: Medium.**
3. **Use Compose + Material 3 1.4.0 now, and Material 3 Expressive (1.5.0) when it goes stable.** Wrap expressive pieces such as the reader FloatingToolbar inside `:core:designsystem`. *Rationale:* expressive graduations are real in 1.5.0-beta01, but loading indicators and shapes were reverted, so beta churn remains. **Confidence: Medium-High.**
4. **Use Navigation 3 1.2.0 with NiA's `NavigationState`/`Navigator`, all `NavKey`s in `:core:navigation`, and list-detail scenes on tablets.** *Rationale:* stable, Compose-first, serialisable back stack, predictive back, adaptive scenes, deep links in 1.2.0. **Confidence: High.**
5. **Use Hilt 2.60.1 + androidx.hilt 1.4.0 over KSP. Do not use kapt, and keep DI annotations out of `:core:model`/`:core:domain`.** *Rationale:* compile-time safety, AGP 9 support, first-class Nav3/Work/Service integration. Pure modules keep the door open to Metro and KMP. **Confidence: High.**
6. **Use Room 3.0.3 + `BundledSQLiteDriver` with two databases: a read-only content DB re-copied on version bump, and a user DB with tested migrations. Build the content DB from Room's exported schema JSON in the Python pipeline.** *Rationale:* FTS5 (and trigram), coroutine-first, KMP, a verified prepackaged re-copy path, and a schema guaranteed to match Room's validation. The fallback is Room 2.8.5. **Confidence: Medium-High.**
7. **Index normalised Arabic/Urdu text, with FTS5 unicode61 for words and trigram for substrings. Never index raw tashkeel.** *Rationale:* unicode61 treats `Mn` marks as separators and removes diacritics only for Latin script [verified]. **Confidence: High.**
8. **Keep settings in a typed `DataStore<UserSettings>` (kotlinx.serialization). Store audio in `getExternalFilesDir/audio/...`, keep packs in `files/packs`, and back up only `user.db` and settings.** *Rationale:* no storage permissions, small backups, KMP-capable. **Confidence: High.**
9. **Use Media3 1.11.1 `MediaLibraryService` with OkHttp data source, WorkManager 2.12.0 downloads (dataSync FGS, resumable, checksummed), Ktor 3.6.0 for JSON, Glance 1.2.0 widgets, and Coil only if reciter photos ship.** *Rationale:* current stable, verified feature set (MiniController, LifecycleService, threading rules). The WorkManager file model matches report 06's `ResolvingDataSource`. **Confidence: High (Media3/Work), Medium (Ktor vs plain OkHttp).**
10. **Ship no GMS in the core app.** Use platform `LocationManagerCompat` with coarse location plus an offline city picker, the rotation-vector Qibla with a no-gyro fallback, and ICU `IslamicCalendar` (Umm al-Qura) with a ±2-day user offset. Put Play-only extras in a `play` flavor and also build a `foss` flavor. *Rationale:* one code path, F-Droid and Huawei viability, privacy, smaller Data safety surface. **Confidence: High.**
11. **Adhan and alarms:** request `SCHEDULE_EXACT_ALARM` (not `USE_EXACT_ALARM`), use `setAlarmClock()`, play adhan with `USAGE_ALARM`, don't use full-screen intents, and reschedule on boot, time and timezone changes. Declare `mediaPlayback` and `dataSync` FGS types in Play Console. *Rationale:* matches the Play policy wording and the Android 17 background-audio rules. **Confidence: High.**
12. **Redefine the reference app's "lock" control:** it is a rotation lock on phones (hidden on sw ≥ 600dp) plus a page-turn lock everywhere. Build two-page spreads for large screens and book posture. Do not use the API 36 resizability opt-out. *Rationale:* Android 16/17 rules. **Confidence: High.**
13. **Keep minSdk 24 with core-library desugaring, and revisit at 1.0 with Play Console data.** *Rationale:* 0.58% of PK traffic is on Android 7.x; the dependency floor is already 24; Hijri needs no API 26. **Confidence: Medium-High.**
14. **Per-app languages:** `AppCompatActivity` + `AppCompatDelegate.setApplicationLocales`, `generateLocaleConfig`, `enableSplit = false`. The mushaf is always RTL regardless of UI locale. **Confidence: High.**
15. **Modularise with the NiA blueprint, about 22 modules, convention plugins, and a build check that forbids feature→feature dependencies. Be KMP-ready but Android-only.** *Rationale:* fast incremental builds and enforced boundaries without KMP's tax. Precomputed glyph positions keep the renderer portable. **Confidence: Medium-High.**
16. **Quality gates from day 1:** Spotless/ktlint + compose-rules, Lint + Slack compose checks, Roborazzi mushaf golden images, JVM content-invariant tests, Room migration tests, a baseline profile + startup and page-turn macrobenchmarks (nightly), and LeakCanary in debug. CI runs on GitHub Actions with checkout v7, setup-java v6 and gradle/actions v6. *Rationale:* the mushaf must be pixel-stable and error-free (report 04's Punjab Act note). Screenshot diffs on real pages are the cheapest guard. **Confidence: High.**
17. **No Play Asset Delivery in v1.** Self-host optional packs with a sha256 manifest; base APK under 30 MB. *Rationale:* Play limits are far above our needs, and PAD would lock out non-Play builds. **Confidence: High.**
18. **Licence hygiene:** generate library licences with AboutLibraries 15.3.0, and add a hand-written content-attribution screen (fonts, text, translations, audio) driven by `content_meta.sources_json` and the pack manifests. *Rationale:* CC BY and OFL require attribution, and the sibling reports flag per-source conditions. **Confidence: High.**

---

## 4. Risks & open questions

**Risks**
- **Kotlin 2.4 and the processor stack.** Hilt 2.60.1 was compiled against Kotlin 2.3.21, and Metro does not list 2.4.21. A metadata-version mismatch would show up as a KSP error. *Mitigation:* the first PR proves `assembleRelease` with a `@HiltViewModel`, a Room 3 DAO and a worker. If it fails, fall back to Kotlin 2.3.21.
- **Room 3 is young** (stable 2026-07-01, now at 3.0.3), and community answers mostly cover Room 2. *Mitigation:* the APIs are close to Room 2.8 KMP; keep DAOs thin and covered by tests; switching back to 2.8.5 is mostly imports plus re-adding SupportSQLite-free code.
- **BundledSQLiteDriver adds about 1.25 MB per ABI** and changes behaviour slightly from platform SQLite (for example, no Android `LOCALIZED` collation) [unverified detail]. *Mitigation:* acceptable; it is needed for FTS5.
- **AGP 9.4 + Gradle 9.8 + Android Studio compatibility.** The current stable IDE is "Android Studio Rabbit 1 (2026.2.1)" [verified], but I did not verify which AGP versions it supports. If the IDE can't sync AGP 9.4, stay on 9.3.3.
- **Material 3 1.5.0** may shift expressive APIs again before stable. Wrapping them in the design system limits the blast radius.
- **Font-feature kashida on API 24–30** (report 04) is the riskiest Android-version-specific item. If it fails, precomputed glyph positions (or a HarfBuzz-free fallback) are needed. That affects minSdk only if old Minikin can't apply `cv01` values.
- **Android 17 background-audio rules plus OEM battery killers** (Xiaomi, Oppo, Vivo, Infinix and Tecno are common in Pakistan) may still delay adhan. *Mitigation:* `setAlarmClock`, an onboarding "disable battery optimisation" helper (the `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` policy needs review [unverified]), and test devices from these brands.
- **Play policy on `SCHEDULE_EXACT_ALARM`** is user-granted and fine. But if Google later narrows it, adhan becomes inexact. Watch the policy deadlines page.
- **Solo-developer bandwidth.** About 22 modules is the upper bound. Collapse `:core:location`, `:core:prayer` and `:core:notifications` into one `:core:prayer` module if it feels heavy.

**Open questions**
1. Will the product owner want an **iOS** version within 12–18 months? If yes, start `:core:model`, `:core:domain`, `:core:content` and `:core:data` as KMP modules now (Room 3 and Ktor already support it) and consider Metro instead of Hilt.
2. **Distribution:** Play only, or also F-Droid / Huawei AppGallery / APK website? This decides whether the `foss` flavor ships and whether PAD is ever acceptable.
3. **Crash reporting and analytics:** none (Play vitals only), privacy-friendly self-hosted (ACRA or Sentry), or Firebase? This changes the Data safety form and adds GMS-adjacent dependencies.
4. Should **downloads continue on metered data** by default? Pakistani mobile data is cheap but capped. Recommend "Wi-Fi only" default with a per-download override.
5. **Default content in the base APK:** one or two mushaf layouts (16-line plus 15-line), and which Urdu translation? This drives the base size estimate (15–25 MB).
6. **Hijri date source:** Umm al-Qura plus a manual offset, or a fetched Ruet-e-Hilal announcement feed? The latter needs a backend.
7. **Tablet two-page mode:** should the spread show two *physical* pages (true mushaf) or page + translation? This matters for the tablet UX decision.
8. **Wear OS / Android Auto:** Media3 `MediaLibraryService` gives Auto almost for free. Is Wear (tasbeeh/adhan) in scope?

---

## 5. Sources

**Google Maven (`maven-metadata.xml` read)**

- https://dl.google.com/android/maven2/ + group path: `com/android/tools/build/gradle`, `androidx/compose/compose-bom`, `androidx/compose/material3/material3`, `androidx/compose/{runtime,ui,foundation}`, `androidx/navigation3/navigation3-{runtime,ui}`, `androidx/navigation/navigation-compose`, `androidx/lifecycle/*`, `androidx/activity/activity-compose`, `androidx/core/core-ktx`, `androidx/room/*`, `androidx/room3/*` (group-index.xml), `androidx/sqlite/sqlite-bundled`, `androidx/datastore/*`, `androidx/media3/*` (group-index.xml), `androidx/work/*`, `androidx/glance/*`, `androidx/startup`, `androidx/profileinstaller`, `androidx/benchmark`, `androidx/baselineprofile`, `androidx/compose/material3/adaptive/*`, `androidx/window`, `androidx/hilt/*`, `androidx/appcompat`, `androidx/core/core-splashscreen`, `androidx/test/*`, `com/google/android/gms/play-services-location`, `com/google/android/play/*`, `com/android/tools/desugar_jdk_libs`, `com/android/compose/screenshot/*` — latest stable and pre-release versions in §2.2.
- https://dl.google.com/android/maven2/androidx/compose/compose-bom/2026.09.00/compose-bom-2026.09.00.pom — BOM to library version mapping.
- https://dl.google.com/android/maven2/com/android/tools/build/gradle/9.4.1/gradle-9.4.1.pom and `.module` — AGP 9.4.1 requires KGP 2.2.10 and KSP 2.2.10-2.0.2.

**Artifacts inspected on Google Maven**

- Each library AAR (`AndroidManifest.xml`) — minSdk values (Navigation3-UI and Work = 24; the others 23 or 21).
- `androidx/room3/room3-runtime-android/3.0.3/*-sources.jar` — `createFromAsset/createFromFile/createFromInputStream`, `PrePackagedCopySQLiteDriver` copy-destructive migration, `RoomConnectionManager` prepackaged validation.
- `androidx/room3/room3-common-jvm/3.0.3/*-sources.jar` — `@Fts5`, tokenizer constants, "BundledSQLiteDriver supports FTS5".
- `androidx/room3/room3-migration-jvm/3.0.3/*-sources.jar` — schema JSON `createSql`, `setupQueries`, `identityHash`.
- `androidx/sqlite/sqlite-bundled-android/2.7.1/*.aar` — `.so` sizes per ABI and ELF `PT_LOAD` alignment 0x4000.
- `androidx/core/core/1.19.1/core-1.19.1-sources.jar` — `LocationManagerCompat.getCurrentLocation` and `hasProvider`.

**Maven Central and Gradle**

- https://repo1.maven.org/maven2/ (metadata) — Kotlin, KSP, Dagger/Hilt (and plugin marker), Koin, Metro, kotlinx-*, Ktor, OkHttp, Retrofit, Coil 3, detekt, ktlint, Spotless, Roborazzi (and plugin marker), Paparazzi, Turbine, MockK, Robolectric (and `android-all-instrumented` SDK 17 jar), LeakCanary, Kover, Truth, dependency-analysis, Slack compose-lint, compose-rules, adhan2, AboutLibraries, Compose Multiplatform, JetBrains navigation3.
- https://repo1.maven.org/maven2/com/google/dagger/hilt-android-gradle-plugin/2.60.1/*.pom — Kotlin BOM 2.3.21.
- https://plugins.gradle.org/m2/ — foojay-resolver 1.0.0, AboutLibraries Android plugin 15.3.0.
- https://services.gradle.org/versions/current — Gradle 9.8.1 is current.

**Android build, library and platform documentation**

- https://developer.android.com/build/releases/gradle-plugin — AGP 9.4 needs Gradle min 9.6.0 and JDK 17; max API 37; AGP 10 mandates the new Variant API.
- https://developer.android.com/build/releases/agp-9-0-0-release-notes — KGP 2.2.10 runtime dependency; how to use a higher KGP/KSP; new DSL and other default changes; minimum Gradle 9.1.0.
- https://developer.android.com/build/migrate-to-built-in-kotlin — kapt incompatibility, `com.android.legacy-kapt`, opt-out flags, KMP plugin restrictions.
- https://kotlinlang.org/docs/releases.html — Kotlin 2.4.21 (2026-10-08), 2.4 support window, 2.5.0 planned Dec 2026.
- https://github.com/google/ksp/releases and https://raw.githubusercontent.com/google/ksp/main/README.md — KSP 2.3.8–2.3.12 notes (Kotlin 2.4 and AGP 9 built-in Kotlin fixes, min AGP 8.12.0).
- https://github.com/google/dagger/releases — Hilt AGP 9 support (2.59), min AGP 9.0.0 (2.59.1), Kotlin 2.3.21 and minSdk 23 (2.60).
- https://developer.android.com/training/dependency-injection/hilt-jetpack — `hiltViewModel` with Navigation 3 via `rememberViewModelStoreNavEntryDecorator`; assisted injection.
- https://insert-koin.io/docs/reference/koin-compose/navigation3 — Koin Nav3 APIs are `@KoinExperimentalAPI`.
- https://zacsweers.github.io/metro/latest/compatibility/ — Metro 1.4.x Kotlin support table.
- https://developer.android.com/jetpack/androidx/releases/room3 — Room 3 changes, KSP-only, drivers, FTS5, KMP JS/Wasm, version dates (3.0.0 on 2026-07-01, 3.0.3 on 2026-09-09).
- https://developer.android.com/jetpack/androidx/releases/navigation3 — 1.0.0 (2025-11-19), 1.1.0 (2026-04-08), 1.2.0 (2026-09-23), deep links, KMP.
- https://developer.android.com/jetpack/androidx/releases/compose-material3 — 1.4.0 stable, 1.5.0-beta01, expressive graduations and reverts.
- https://developer.android.com/jetpack/androidx/releases/media3 — 1.11.1 (2026-09-10), LifecycleService, threading, MiniController, ktor data source.
- https://developer.android.com/jetpack/androidx/releases/glance — 1.2.0 stable, generated previews, minSdk 23.
- https://developer.android.com/jetpack/androidx/releases/benchmark — 1.5.0 (2026-09-09), AGP 9 newDsl note.
- https://developer.android.com/topic/performance/app-optimization/enable-app-optimization — `optimization { enable = true }` (AGP 9.3+), keepRules, full mode default.
- https://developer.android.com/studio/preview/compose-screenshot-testing — "Experimental", alpha16, AGP test-suite migration.
- https://developer.android.com/studio/releases — Android Studio Rabbit 1 (2026.2.1) is the stable IDE.

**Android platform behaviour, Play policy and services**

- https://support.google.com/googleplay/android-developer/answer/11926878 — target API 36 by 2026-08-31, extension to 2026-11-01.
- https://support.google.com/googleplay/android-developer/answer/9888170 — `USE_EXACT_ALARM` allowed cases, declaration, full-screen intent eligibility.
- https://developer.android.com/develop/background-work/services/alarms — SCHEDULE vs USE exact alarm, defaults, revocation, `setAlarmClock`.
- https://developer.android.com/develop/background-work/services/fgs/service-types — mediaPlayback and dataSync types, BOOT_COMPLETED restriction, Play Console declaration.
- https://developer.android.com/develop/background-work/services/fgs/timeout — dataSync and mediaProcessing 6 h per 24 h.
- https://developer.android.com/about/versions/15/changes/datasync-migration — UIDT is not available in WorkManager.
- https://developer.android.com/guide/practices/page-sizes — 16 KB requirement and the 2027-02-01 deadline for updates.
- https://developer.android.com/about/versions/16/behavior-changes-16 — edge-to-edge opt-out disabled, predictive back, sw600dp orientation and resizability rule with an opt-out ending at API 37.
- https://developer.android.com/about/versions/17/behavior-changes-17 — background audio restrictions, widget bitmap memory limit, ECH, CT, local network permission.
- https://developer.android.com/about/versions/17/release-notes — platform stability at Beta 3 (2026-03-26).
- https://dl.google.com/android/repository/repository2-3.xml — SDK Platform 37.0, 37.1 and 37.2 on the stable channel; build-tools 37.0.0.
- https://support.google.com/googleplay/android-developer/answer/9859372 — Play size limits (base module 500 MB and the rest).
- https://developer.android.com/guide/playcore/asset-delivery — PAD delivery modes, served by Play.
- https://developer.android.com/guide/topics/resources/app-languages — `generateLocaleConfig`, `AppCompatDelegate`, `autoStoreLocales`, AppCompatActivity requirement for Compose.
- https://developer.android.com/guide/app-bundle/configure-base — language-split disable requirement for in-app language pickers.
- https://developer.android.com/reference/android/icu/util/IslamicCalendar and `.CalculationType` — API 24, ISLAMIC_UMALQURA.
- https://www.sqlite.org/fts5.html — unicode61 token categories, Latin-only diacritic removal, trigram tokenizer.
- https://gs.statcounter.com/android-version-market-share/mobile/pakistan (CSV export, Sep 2026) — Pakistan Android version shares.
- https://detekt.dev/docs/introduction/compatibility/ — detekt versions vs Kotlin, Gradle and AGP.

**Reference code and CI**

- https://github.com/android/nowinandroid (git clone, commit a49ed25, 2026-09-22) — modules, convention plugins, Nav3 `NavigationState`, `gradle.properties`, libs.versions.toml (AGP 9.3.2, Kotlin 2.3.0, Hilt 2.59, Room 2.8.3, Roborazzi, Spotless).
- `git ls-remote --tags` for https://github.com/actions/checkout, actions/setup-java, gradle/actions, actions/upload-artifact, actions/cache — latest major tags (v7, v6, v6, v7, v6).
