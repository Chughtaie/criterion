# Toolchain spike (reference only — not part of the app build)

A throwaway Android project that showed the planned 2026 stack **compiles, runs and passes tests together** before we commit to it. The full write-up is in [`docs/research/10-gap-04-agp9-toolchain-build-spike.md`](../../docs/research/10-gap-04-agp9-toolchain-build-spike.md).

It is a separate Gradle build with its own `settings.gradle.kts`. The root project does not include it, so it never affects the real app build.

## What it showed (2026-10-19)

On a 4-vCPU Linux VM, `buildEnvironment assembleDebug assembleRelease testDebugUnitTest` passed with 12/12 JVM tests:

- **Toolchain:** AGP 9.4.1, Gradle 9.8.1 and Kotlin 2.4.21. Kotlin is raised above AGP's bundled 2.2.10 by `alias(libs.plugins.kotlin.jvm) apply false` in the root `build.gradle.kts`. KSP is 2.3.12.
- **SDK levels:** compileSdk 37, targetSdk 36, minSdk 24. Core-library desugaring and R8 are on.
- **UI:** Compose BOM 2026.09.00 and Navigation 3 1.2.0. Hilt 2.60.1 with androidx.hilt 1.4.0 handles `@HiltViewModel`, `@HiltWorker` and `@AndroidEntryPoint` on a `MediaLibraryService`.
- **Database:** Room 3.0.3 (`androidx.room3`) with `BundledSQLiteDriver` (sqlite-bundled 2.7.1). It opens a database **prepackaged with `createFromAsset`** that `scripts/build_content_db.py` generated from Room's exported schema JSON. That database has FTS5 unicode61 and trigram external-content tables.
- **Networking:** Media3 1.11.1's `OkHttpDataSource` runs on OkHttp 5.5.0 (full and Range reads). Ktor 3.6.0 uses the same client.
- **Other libraries:** WorkManager 2.12.0, Glance 1.2.0, and adhan2 0.0.7 (Karachi + Hanafi).
- **Kashida:** the `cv01` kashida probe matches HarfBuzz's widths under Robolectric NATIVE graphics, via `Paint.setFontFeatureSettings`.

## What is intentionally *not* committed

| Missing | Why | How to regenerate |
|---|---|---|
| `core/content/src/main/assets/content/quran-content.db` | Generated artefact | `python3 -I scripts/build_content_db.py core/content/schemas/.../1.json quran-uthmani.xml quran-simple-clean.xml core/content/src/main/assets/content/quran-content.db`. The two XML files are Tanzil downloads (CC BY 3.0) |
| `app/src/test/resources/kashida-fonts/*.ttf, *.otf` | Third-party font binaries (DigitalKhatt IndoPak, OFL 1.1) stay out of the repo until the pipeline pins them | Fetch `hb/indopak.otf` from the digitalkhatt.org repo at commit `4213fab`, then convert it with `scripts/cff2_to_glyf.py` |
| `local.properties`, `build/`, `.gradle/` | Machine-specific | Android Studio or `sdk.dir=...` |

## Scripts

These seed the real `tools/` pipelines described in [`docs/plan/08-data-pipelines.md`](../../docs/plan/08-data-pipelines.md).

- `scripts/build_content_db.py` builds the prepackaged Room 3 database by running the exported schema's `createSql`, triggers and `setupQueries` verbatim. It then sets `user_version` and VACUUMs.
- `scripts/cff2_to_glyf.py` converts the CFF2 DigitalKhatt font to TrueType glyf outlines, which Android 7–8 FreeType needs.
- `scripts/hb_widths.py` and `scripts/font_features.py` are uharfbuzz probes for `cv01` kashida advances and font feature inspection.

Run Python with `python3 -I`.
