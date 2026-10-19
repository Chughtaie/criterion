# 10 · Gap-04: AGP 9 toolchain build spike, plus kashida on old Android

Researcher key: `gap-agp9-toolchain-build-spike` · Date: 2026-10-19

**Scope.** Report 07 checked that every proposed version exists. Nobody had compiled them together. This spike checks four things:
- Does that exact combination **build, minify and run**?
- Settle two contradictions between reports: Room 3 + FTS5 (report 07) vs Room 2 + FTS4 (report 05), and OkHttp 5 (report 07) vs OkHttp 4.12 (report 06).
- Do the fallbacks work?
- Does kashida via `cv01` feature values work on API 24–30?

> **Method.**
> - **SDK.** Installed `cmdline-tools` 23.0, `platforms;android-37.0`, `android-36`, `android-36.1`, `build-tools` 37.0.0 and 36.1.0, and `platform-tools` 37.0.1 into my scratch directory. JDK 21 is the system JDK.
> - **Project.** Copied the empty Criterion project (it is untouched) and built a three-module skeleton (`:core:model`, `:core:content`, `:app`) on exactly the proposed stack.
> - **Builds.** Ran `buildEnvironment`, `assembleDebug`, `assembleRelease` (R8 on) and `testDebugUnitTest`.
> - **Variants.** Then built four variant copies: Kotlin 2.3.21, Kotlin 2.2.10, OkHttp 4.12 forced, and Room 2.8.5 + FTS4.
> - **Kashida.** Read the AOSP sources at the `android-7.0.0_r1` … `android-11.0.0_r1` tags. Measured `Paint.getRunAdvance` in Robolectric NATIVE graphics mode and checked the numbers against uharfbuzz.
> - **Artifacts.**
>   - Skeleton and scripts: `<scratch>/repos/gap-agp9-toolchain-build-spike/spike-skeleton.tgz`
>   - Build logs: `…/gap-agp9-toolchain-build-spike/logs/*.log`
>
> **Tags.**
> - **[verified: url]**: fetched or inspected in this session.
> - **[verified: build log NN]**: observed in local build log `logs/NN-*.log` in the scratch directory above.
> - **[measured]**: computed here.
> - **[inferred]**: reasoned from verified evidence.
> - **[unverified]**: from memory.

---

## 1. Executive summary

1. **The proposed stack builds and runs as specified. No library has to change.** The stack: AGP 9.4.1, Gradle 9.8.1, Kotlin 2.4.21, KSP 2.3.12, Compose BOM 2026.09.00, Navigation3 1.2.0, Hilt 2.60.1 + androidx.hilt 1.4.0, Room 3.0.3 + sqlite-bundled 2.7.1 (`@Fts5`, `createFromAsset`), Media3 1.11.1 + OkHttp 5.5.0 + Ktor 3.6.0, WorkManager 2.12.0 + hilt-work, Glance 1.2.0, adhan2 0.0.7 + desugaring, compileSdk 37 / targetSdk 36 / minSdk 24, R8 `optimization { enable = true }`.
   - `assembleDebug`, `assembleRelease` and `testDebugUnitTest` all pass: **12/12 JVM tests** [verified: build logs 10, 30].
   - **No ADR needs to change.**
2. **The root `alias(libs.plugins.kotlin.jvm) apply false` lift works.** `buildEnvironment` shows `kotlin-gradle-plugin:2.2.10 -> 2.4.21` and `symbol-processing-gradle-plugin:2.2.10-2.0.2 -> 2.3.12` [verified: build log 01].
   - **The lift is mandatory.** With AGP's bundled Kotlin 2.2.10, compilation fails: adhan2 0.0.7 and the stdlib 2.4.0 it pulls in have **metadata 2.4.0**, while "the compiler version 2.2.0 can read versions up to 2.3.0" [verified: build log 21].
   - **Kotlin 2.3.21 is a working fallback.** Full build and all 12 tests pass [verified: build log 20].
3. **Hilt 2.60.1 works on Kotlin 2.4.21 at compile time and at runtime.** It was built against Kotlin 2.3.21. Proven in Robolectric:
   - a `@HiltViewModel` runs an FTS query;
   - a `@HiltWorker` gets its dependencies through `@AssistedInject`;
   - an `@AndroidEntryPoint` `MediaLibraryService` gets the singleton `OkHttpClient` and builds a session [verified: build log 09].
4. **Settled: use Room 3.0.3 + FTS5 (BundledSQLiteDriver).** The prepackaged-DB pipeline works end to end. A Python script (`python3 -I`) executes the exported schema's `createSql` + `contentSyncTriggers` + `setupQueries`, loads all 6,236 Tanzil ayahs, then runs `rebuild`/`optimize`, `user_version` and `VACUUM`. `createFromAsset` + `BundledSQLiteDriver` then opens it in Robolectric 4.17:
   - FTS5 `MATCH` returns 45 hits for الرحمن;
   - trigram returns 201 hits for رحم;
   - prefix (`*`) and explicit `AND` work;
   - bundled SQLite is **3.50.1** with `ENABLE_FTS5` [verified: build log 05].
5. **Extra non-entity tables do not break Room validation, in either open path.** But beware:
   - With a matching `room_master_table`, Room checks **only the identity hash**. Schema validation is skipped [verified: `RoomConnectionManager.kt` source].
   - CI must therefore also open a no-master-table build to force `onValidateSchema`. That path is proven to accept extra tables and to reject an FTS tokenizer mismatch.
   - A wrong hash is rejected. `user_version = 0` is accepted [verified: build log 05].
6. **Room 2.8.5 + FTS4 is a proven fallback (6/6 tests), with the same pipeline.** Costs compared with Room 3 + FTS5:
   - **no explicit `AND`** (standard FTS4 syntax: `"a" AND "b"` → 0 hits, `a b` → 6) [verified: build log 25];
   - no trigram tokenizer;
   - platform SQLite differs per device (API 24 = **3.9.2**, FTS3/FTS4 + unicode61, no FTS5 [verified: AOSP `external/sqlite` @android-7.0.0_r1]).
   - It saves about 1.3 MB of `.so` per ABI and about 1.2 MB of trigram index. The universal release APK is 4.4 MB vs 10.2 MB [measured].
7. **Settled: OkHttp 5.5.0.**
   - Ktor 3.6.0's OkHttp engine **declares OkHttp 5.5.0** [verified: POM]. Gradle upgrades Media3's 4.12.0 to 5.5.0 automatically [verified: build log 12].
   - Media3's `OkHttpDataSource`, compiled against 4.12, does full and **Range** reads on OkHttp 5.5.0 at runtime [verified: build log 09].
   - Forcing 4.12.0 does build and run. But it fights Ktor's metadata and breaks our source: `Response.body` is nullable in 4.x [verified: build logs 22, 26].
8. **Use AGP 9.4.1, which is the ceiling for the stable IDE.** Android Studio **Rabbit 1 (2026.2.1)** is current stable and supports **AGP 7.1–9.4**. Quail 3/4 support 9.3 or 9.4. API 37 needs at least **Panda 3 Patch 1 / AGP 9.1.1** [verified: developer.android.com/studio/releases]. AGP 9.5 (alpha) would need a preview IDE.
9. **Release build health.**
   - **R8 passes** with no missing-class errors.
   - **Desugaring is real**: the release dex has **0 `java/time` references and 12,243 `j$/time` references**.
   - **16 KB alignment**: `zipalign -c -P 16` passes.
   - **adhan2 Karachi/Hanafi works**: for 2026-10-19 it returns Fajr 05:16, Sunrise 06:32, Dhuhr 12:18, Asr 16:24, Maghrib 18:02, Isha 19:18 PKT [verified: build logs 06, 09].
10. **Kashida via `cv01=N` works on API 24 → 37 at the source level.** Every link in the chain was checked:
    - `Paint.setFontFeatureSettings` passes the string verbatim (7.0–11).
    - JNI passes it to Minikin's `addFeatures()` → `hb_feature_from_string` (value kept, identical code at 7.0/8.0/9/10/11).
    - The bundled HarfBuzz versions all parse `'cv01' 3` and pick the AlternateSubst glyph by mask value: **1.2.6 / 1.4.2 / 1.7.4 / 2.3.0 / 2.6.4**.
    - Minikin **bypasses its layout cache** whenever feature settings are non-empty (`skipCache()`), so cached widths never leak between calls [verified: AOSP tags].
    - **Measured on Robolectric NATIVE (HarfBuzz 2.6.4):** الْكِتٰبُ = 2060 → 2847 / 3615 / 4384 / 6691 units for cv01 = 1/2/3/6. This matches uharfbuzz 14.6.0 exactly on SDK 26, 28, 30, 33 and 36 [measured].
11. **Two kashida traps.**
    - **(a) Wrong font.** The task's font, `static-cdn.tarteel.ai/qul/fonts/dk/DigitalKhattIndoPak.otf`, **has no cv01–cv18 features**. `cv01` is a silent no-op there (2060 at every value) [measured]. Use the **digitalkhatt.org repo build** (`ClientApp/src/assets/fonts/hb/indopak.otf`, OFL 1.1): its `cv01` is GSUB lookup type 7 → **3 (AlternateSubst)** [verified].
    - **(b) Wrong outline format.** Android 7.0 and 8.0 ship **FreeType 2.6.0 / 2.7.1**, and CFF2 support arrived in FreeType **2.8** [verified]. So ship the **glyf TTF conversion**: 1.63 MB, identical widths [measured].
12. **Robolectric gotchas, each cost hours.**
    - SDK 36 needs `--add-exports java.base/jdk.internal.access` on JDK 21.
    - `BundledSQLiteDriver` needs the host `.so` from `sqlite-bundled-jvm`, wired through the loader's system properties.
    - **NATIVE graphics**:
      - aborts the JVM if two SDKs share it;
      - is shadowed by any test-resource folder named `fonts/`;
      - is SDK ≥ 26 only: SDK 25 silently falls back to fake metrics;
      - uses **one native build (HarfBuzz 2.6.4) for every SDK** [verified: build logs 04, 14–18].

---

## 2. Detailed findings

### 2.1 Environment and what was built

| Item | Value |
|---|---|
| SDK packages | `cmdline-tools;latest` = 23.0 (`commandlinetools-linux-16111833_latest.zip`, 181 MB). `platforms;android-37.0` r2 (there is **no** `platforms;android-37`, only 37.0 / 37.1 / 37.2 and betas). `build-tools;37.0.0`. `platform-tools` 37.0.1 [verified: repository2-3.xml]. `sdkmanager` prints "deprecated … `android sdk` is the replacement" and "The --licenses option is no longer needed" [verified: install log] |
| compileSdk mapping | `compileSdk { version = release(37) }` resolved to the installed `android-37.0`. AGP downloaded nothing extra [verified: sdk dir after build] |
| Host | 4 vCPU, 15 GB RAM, **no `/dev/kvm`**, so no emulator. Maven Central returned intermittent **HTTP 429** through the proxy. I worked around it with a retry loop plus `-Dorg.gradle.internal.repository.max.retries=8` |
| `:core:model` | `org.jetbrains.kotlin.jvm`, JVM 17: `AyahRef` and an `ArabicNormalizer` (tashkeel/tatweel strip, alef/ya/ta-marbuta/kaf folds) |
| `:core:content` | `com.android.library` + KSP + `androidx.room3`. Entity `ayah(id, surah, ayah, text_uthmani, text_norm)`. **Two external-content FTS5 tables** over `text_norm`: `ayah_fts` (unicode61) and `ayah_fts_tri` (trigram). `exportSchema = true`. Asset `content/quran-content.db` |
| `:app` | `AppCompatActivity` + Compose + Nav3 `NavDisplay` with `rememberSaveableStateHolderNavEntryDecorator()` + `rememberViewModelStoreNavEntryDecorator()` and `hiltViewModel()`. `@HiltViewModel`. `MediaLibraryService` with `OkHttpDataSource.Factory(okHttp5)` → `DefaultMediaSourceFactory`. `@HiltWorker` `CoroutineWorker` (OkHttp download). Glance widget + receiver. Ktor `HttpClient(OkHttp) { engine { preconfigured = okHttp } }`. adhan2 `PrayerTimes(Coordinates(24.8607, 67.0011), …, KARACHI.parameters.copy(madhab = HANAFI))`. Desugaring on. R8 on |
| Tests | 7 Robolectric Room tests, 3 Hilt runtime tests, 1 OkHttp5×Media3 test, 1 adhan2 test. The kashida and native-probe tests run separately, one SDK per JVM |

Build timings on this 4-core VM:
- clean `buildEnvironment assembleDebug assembleRelease testDebugUnitTest` takes **6 min 3 s** (207 tasks);
- `minifyReleaseWithR8` alone takes about 3 min;
- incremental `testDebugUnitTest` takes 16–24 s [verified: build logs 10, 06, 05, 09].

### 2.2 Compatibility matrix

| # | Combination | buildEnvironment | assembleDebug | assembleRelease (R8) | testDebugUnitTest | Error → fix |
|---|---|---|---|---|---|---|
| **A** | **Proposed:** AGP 9.4.1, Gradle 9.8.1, KGP 2.4.21, KSP 2.3.12, BOM 2026.09.00, Nav3 1.2.0, Hilt 2.60.1/1.4.0, Room 3.0.3 + sqlite-bundled 2.7.1 (FTS5), Media3 1.11.1, OkHttp 5.5.0, Ktor 3.6.0, Work 2.12.0, Glance 1.2.0, adhan2 0.0.7, desugar 2.1.5, compileSdk 37 | ✅ KGP 2.2.10→2.4.21, KSP→2.3.12 | ✅ | ✅ 10.2 MB universal APK | ✅ 12/12 | Four fixes in our own build/test code, none in libraries (§2.3) [verified: build logs 01–10, 30] |
| B | A with **Kotlin 2.3.21** | ✅ KGP 2.2.10→2.3.21 | ✅ | ✅ | ✅ 12/12 | None. 2.3 reads adhan2/stdlib 2.4 metadata (one version ahead) [verified: build log 20] |
| C | A with **Kotlin 2.2.10** (AGP default, no lift) | ✅ 2.2.10 | ❌ | ❌ | — | `Module was compiled with an incompatible version of Kotlin. The binary version of its metadata is 2.4.0, expected version is 2.2.0` for `adhan-jvm-0.0.7.jar` and `kotlin-stdlib-2.4.0.jar`. The fix is to lift KGP to ≥ 2.3 (or drop adhan2 0.0.7). `:core:model` and `:core:content` (Room 3, Hilt-free) **did** compile on 2.2.10 [verified: build log 21] |
| D | A with **OkHttp 4.12.0 forced** (`resolutionStrategy.force`) | n/a | ❌ then ✅ | ✅ (no R8 missing classes) | ✅ 3/3 (Media3 + Ktor GET on 4.12) | `Only safe (?.) … calls are allowed on a nullable receiver of type 'ResponseBody?'`. 4.x `Response.body` is nullable, 5.x is not. Fixed with `resp.body?.bytes()`. **Works, but it is an unsupported downgrade** against Ktor 3.6.0's declared `okhttp:5.5.0` [verified: build logs 22, 26] |
| E | A with **Room 2.8.5 + `@Fts4`** (framework SQLite, no bundled driver, no trigram) | n/a | ✅ | ✅ 4.4 MB universal APK | ✅ 11/11 (6 Room + 5 app) after two test-data fixes | (1) My mismatch fixture's `sed` didn't match Room 2's `createSql` (`text_norm TEXT NOT NULL`), so I regenerated it. (2) `"الرحمن" AND "الرحيم"` returns **0** under FTS4 standard syntax, while `الرحمن الرحيم` returns 6 [verified: build logs 24, 25] |
| F | Android Studio sync | — | — | — | — | Rabbit 1 (2026.2.1) supports AGP **7.1–9.4**, so A syncs. AGP 9.5.0-alpha needs a preview IDE [verified: studio/releases] |

### 2.3 Every error hit on the primary stack, and the fix

| # | Where | Error (verbatim) | Root cause | Fix (all build/test-side, kept in the snippets below) |
|---|---|---|---|---|
| 1 | Any resolution | `Received status code 429 from server: Too Many Requests` (repo.maven.apache.org) | Sandbox proxy rate-limit, not the stack | Retry. In CI, Gradle caching (`gradle/actions/setup-gradle`) avoids the cold-cache burst [inferred] |
| 2 | `:app:compileDebugKotlin` | `Cannot access 'androidx.room3.RoomDatabase' which is a supertype of 'com.chughtai.criterion.core.content.QuranContentDb'` (`MISSING_DEPENDENCY_SUPERCLASS`) | `:core:content` declared `room3-runtime` as `implementation` while exposing a `RoomDatabase` subclass | `api(libs.androidx.room3.runtime)`. Better: keep the DB `internal` and expose a repository interface (§3) |
| 3 | Robolectric, every test | `RuntimeException: Failed to interact with raw FileDescriptor internals; perhaps JRE has changed?` ← `IllegalAccessException … cannot access class jdk.internal.access.SharedSecrets … does not export jdk.internal.access to unnamed module` at `com.android.internal.os.ApplicationSharedMemory.create` | SDK 36's `ApplicationSharedMemory` plus JDK 21 strong encapsulation | `jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED", "--add-opens=java.base/java.io=ALL-UNNAMED")` on `Test` tasks |
| 4 | Robolectric + `BundledSQLiteDriver` | (prevented) the Android AAR only carries `jni/{arm64-v8a,armeabi-v7a,x86,x86_64}/libsqliteJni.so` | No host `.so` on the unit-test classpath | `sqlite-bundled-android`'s `NativeLoader` honours `androidx.sqlite.driver.bundled.path` + `.name` system properties [verified: sources jar]. Extract `natives/linux_x64/libsqliteJni.so` from `sqlite-bundled-jvm:2.7.1` with a `Sync` task and set the properties. JNI symbols match: all 7 Room tests pass |
| 5 | My Hilt VM test | Hang at `runBlocking` (jstack: `SDK 36 Main Thread` parked in `BlockingCoroutine.joinBlocking`) | `viewModelScope` resumes on `Dispatchers.Main` (the Robolectric main looper) while `runBlocking` blocks that looper | A test bug: pump `shadowOf(getMainLooper()).idle()` instead, or use `runTest` + `MainDispatcherRule` |
| 6 | Kotlin DSL | `The 'val name by creating { }' property delegate syntax has been deprecated … removed in Gradle 10` | My script | `configurations.create("sqliteNative") { … }` |
| 7 | AGP itself | `The Configuration.setVisible(boolean) method has been deprecated … removed in Gradle 11` from `com.android.build.gradle.internal.plugins.BasePlugin…createAndroidJdkImageConfiguration` / `SourceSetManager.createConfiguration` | AGP 9.4.1 on Gradle 9.8.1 | Nothing to do. This is an AGP-internal warning; watch it before Gradle 11 |
| 8 | Info only | `Unable to strip the following libraries, packaging them as they are: libandroidx.graphics.path.so, libsqliteJni.so` | No NDK installed | Harmless. Install the NDK on CI if we want AGP to strip |

No KSP, Hilt, Room-compiler, Compose-compiler, Nav3, Media3, Glance or R8 error appeared on Kotlin 2.4.21 [verified: build logs 03, 06, 10, 30].

### 2.4 Room 3 prepackaged-DB results (task 3)

**Exported schema (excerpt).** Room 3 emits FTS5 `createSql` and separate `contentSyncTriggers`, plus `setupQueries` holding the master table and identity hash [verified: `schemas/…/1.json`]:

```json
"createSql": "CREATE VIRTUAL TABLE IF NOT EXISTS `${TABLE_NAME}` USING FTS5(`text_norm`, tokenize=`unicode61`, content=`ayah`, content_rowid=`id`)",
"contentSyncTriggers": ["CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_ayah_fts_AFTER_INSERT AFTER INSERT ON `ayah` BEGIN INSERT INTO `ayah_fts`(`rowid`, `text_norm`) VALUES (NEW.`id`, NEW.`text_norm`); END", "…"],
"setupQueries": ["CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
                 "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '449638a4661380449957f25395c49e23')"]
```

**What Room actually validates** (generated `QuranContentDb_Impl.onValidateSchema` and `RoomConnectionManager.checkIdentity`) [verified: generated code and `room3-runtime` 3.0.3 sources]:
- It runs `TableInfo` equality for each **entity** table and `FtsTableInfo` (columns + options parsed from the stored SQL) for each **FTS** table. Nothing else is inspected, so non-entity tables, indices and triggers are ignored.
- If `room_master_table` exists, **only the identity hash is compared**. `onValidateSchema` runs only when the master table is absent, and then Room also runs `onPostMigrate`, which creates the FTS sync triggers, and writes the hash.

| Test (Robolectric 4.17, SDK 36, `BundledSQLiteDriver`) | DB variant (all 6,236 ayahs) | Result |
|---|---|---|
| `createFromAsset`, FTS5 + trigram queries | createSql + triggers + setupQueries + **extra tables `content_meta`, `page_line` (WITHOUT ROWID + index)**, `user_version=1`, 4,096,000 B | ✅ count 6236; الرَّحْمَٰنِ → normalised → **45 hits, first = 1:1**; trigram رحم → **201**; `"الرحمن" AND "الرحيم"` non-empty; `رحم*` non-empty |
| No master table + extra tables (forces full validation) | `--no-master --extra-tables` | ✅ validates and opens |
| No master table, no triggers | `--no-master --skip-triggers` | ✅ validates; Room adds the triggers |
| `user_version = 0` | | ✅ goes through `onCreate` (empty-schema check fails → validate) |
| Wrong identity hash | `--bad-hash` | ✅ rejected: `Room cannot verify the data integrity… Expected identity hash: 4496…9e23, found: 0000…` |
| FTS tokenizer mismatch (porter instead of unicode61), no master table | | ✅ rejected: `Pre-packaged database has an invalid schema: ayah_fts(…AyahFts). Expected: FtsTableInfo{… tokenize=\`unicode61\`}` |
| Bundled engine probe | | `sqlite_version()` = **3.50.1**; compile options include `ENABLE_FTS3, ENABLE_FTS3_PARENTHESIS, ENABLE_FTS4, ENABLE_FTS5, ENABLE_MATH_FUNCTIONS, ENABLE_RTREE, ENABLE_STAT4` |
| `:app` Hilt test: `QuranContentDb` from the **app-merged** library asset via `@HiltViewModel` | | ✅ 45 hits |

All rows are [verified: build logs 05, 09].

**Room 2.8.5 + FTS4 (variant E):** the same pipeline and the same seven cases (minus the bundled probe). All pass once the test data is corrected. Notes:
- DB is 2,887,680 B (one FTS table).
- FTS4 standard syntax rejects explicit `AND` (0 hits); implicit AND gives 6 hits.
- The query runs on Robolectric's SQLite.
- [verified: build logs 24, 25]

**API 24 platform SQLite** is 3.9.2, built with `-DSQLITE_ENABLE_FTS3 -DSQLITE_ENABLE_FTS3_BACKWARDS -DSQLITE_ENABLE_FTS4 -DSQLITE_ENABLE_ICU`, with **no FTS5 and no `FTS3_PARENTHESIS`**. unicode61 is compiled in unless `SQLITE_DISABLE_FTS3_UNICODE` is set, and it is not set [verified: AOSP `external/sqlite/dist/Android.mk`, `sqlite3.h`, `sqlite3.c` @android-7.0.0_r1].

**Size comparison** [measured]:

| | Room 3 + FTS5 (unicode61 + trigram) + bundled | Room 2 + FTS4 (platform) |
|---|---|---|
| Content DB (Uthmani + normalised) | 4.10 MB raw → 1.82 MB stored | 2.89 MB raw |
| Native | `libsqliteJni.so` 1.28 MB (arm64) / 1.25 MB (v7a) per device via AAB | 0 |
| Universal release APK (4 ABIs) | 10.2 MB | 4.4 MB |

**Settling the contradiction.** **Room 3.0.3 + FTS5 on `BundledSQLiteDriver`** wins on four counts:
- the same SQLite (3.50.1) on every OEM image from API 24 up;
- a trigram tokenizer for Urdu and partial-word search;
- full boolean/NEAR syntax and bm25;
- KMP-ready, with a verified pipeline.

The cost is about 1.3 MB of `.so` per device, plus 1.2 MB for the optional trigram index. **Report 05's FTS4 schema becomes the documented fallback**, proven to work with Room 2.8.5. Switching means changing imports, `@Fts4`, the builder and the query syntax.

### 2.5 OkHttp 4 vs 5 (task 5)

| Evidence | Finding |
|---|---|
| `media3-datasource-okhttp-1.11.1.pom` | `okhttp:4.12.0` compile scope [verified] |
| `ktor-client-okhttp-jvm-3.6.0.pom` | `okhttp-jvm:5.5.0`, `okio-jvm:3.18.2` [verified]. In the app the Gradle-metadata variant resolves to `okhttp-android:5.5.0` |
| Resolved `releaseRuntimeClasspath` | `com.squareup.okhttp3:okhttp:4.12.0 -> 5.5.0` → `okhttp-android:5.5.0`, okio 3.18.2 [verified: build log 12] |
| Runtime: Media3 `OkHttpDataSource` on 5.5.0 | `OkHttp.VERSION = 5.5.0`. 64 KiB full read is byte-identical. `DataSpec(position=1000, length=500)` sends `Range: bytes=1000-1499` and reads the right 500 bytes [verified: build log 09] |
| R8 | No missing classes or members reported [verified: build log 06] |
| OkHttp 5.0.0 changelog | Separate JVM/Android artifacts handled by Gradle metadata. `mockwebserver3` has a new coordinate [verified: CHANGELOG.md] |
| okhttp-android 5.5.0 AAR | `minSdkVersion="21"` [verified] |
| Forced 4.12.0 | Builds, R8 passes, Media3 and Ktor GET work. But our code needed `body?.`, and the downgrade contradicts Ktor's declared dependency [verified: build log 26] |

**Decision: OkHttp 5.5.0, plus `mockwebserver3` 5.5.0 for tests.** Report 06's "pin 4.12" advice is superseded. Media3 being compiled against 4.12 is harmless, as shown at runtime.

### 2.6 Toolchain facts confirmed

- **AGP 9.4.1 POM:** `kotlin-gradle-plugin:2.2.10` (runtime) and `symbol-processing-gradle-plugin:2.2.10-2.0.2` [verified: gradle-9.4.1.pom].
- **Gradle `services.gradle.org/versions/current`:** 9.8.1, built 2026-10-07 [verified].
- **Hilt Gradle plugin, Room 3 Gradle plugin and KSP all work with AGP built-in Kotlin.** Room 3's plugin needs AGP ≥ 8.10 and fails fatally without `room3 { schemaDirectory(...) }` [verified: room3-gradle-plugin sources].
- **adhan2 0.0.7** is MIT [verified: POM + LICENSE]. It has KMP variants (jvm/js/wasm/native) but **no Android variant**, so the `jvm` variant resolves on Android. Its API uses `kotlin.time.Instant` and needs stdlib 2.4.0 [verified: .module, sources].
- **Merged manifest.** WorkManager, Media3 and Glance add `ACCESS_NETWORK_STATE`, `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED` and `<pkg>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. These are consistent with report 07's permission list [verified: aapt2 badging].
- **Release APK composition** [measured]:
  - `classes.dex` 4.29 MB raw / 2.17 MB stored;
  - assets 4.24 MB / 1.82 MB;
  - `lib/*` 1.15–1.29 MB per ABI (stored uncompressed);
  - `res` 0.52 MB;
  - `DebugProbesKt.bin` (kotlinx-coroutines) is present and can be excluded via `packaging`.

### 2.7 Old-Android kashida (task 7)

**Font facts** [verified: fontTools 4.67 on the files; measured: uharfbuzz 0.56.3 / HarfBuzz 14.6.0]:

| File | Source | GSUB features | `cv01` lookup | الْكِتٰبُ width (upem 1000) at cv01 = 0/1/2/3/6 |
|---|---|---|---|---|
| `DigitalKhattIndoPak.otf` (494,828 B, 1,141 glyphs, CFF2) | static-cdn.tarteel.ai/qul/fonts/dk/ | basm ccmp fina init medi rclt rlig ss01 | **none** | 2060 / 2060 / 2060 / 2060 / 2060 (**no-op**) |
| `indopak.otf` (2,569,560 B, 3,999 glyphs, CFF2, OFL 1.1) | DigitalKhatt/digitalkhatt.org @4213fab (2026-01-14) `ClientApp/src/assets/fonts/hb/` | + **cv01 cv02 cv03 cv10–cv18** | lookup 78, type **7→3 (AlternateSubst)** | 2060 / 2847 / 3615 / 4384 / 6691 |
| `dk-indopak-cv-glyf.ttf` (1,628,084 B, sha256 `428ae074…`) | my CFF2→glyf conversion (fontTools `Cu2QuPen`, max_err 1.0) | same as above | same | **identical** |

**AOSP chain at each tag** [verified: android.googlesource.com `?format=TEXT` at each tag]:

| Release (API) | `Paint.java setFontFeatureSettings` | JNI `Paint.cpp` | Minikin `addFeatures` | Layout cache with features | HarfBuzz (bundled) | `'cv01' 3` parse | AlternateSubst by value | FreeType (CFF2?) | **Verdict** |
|---|---|---|---|---|---|---|---|---|---|
| 7.0 (24/25) | stores, then `nSetFontFeatureSettings(mNativePaint, settings)` | `ScopedUtfChars` → `paint->setFontFeatureSettings(std::string)`, then `MinikinUtils` copies to `minikinPaint->fontFeatureSettings` | `Layout.cpp` L684: split on `,`, `hb_feature_from_string`, keep if universal range | `skipCache()` = `!fontFeatureSettings.empty()` → bypass | **1.2.6** | `parse_feature_tag` accepts `'…'` quotes (4 bytes), `parse_feature_value_postfix` reads the uint | `alt_index = (lookup_mask & glyph_mask) >> shift`; map allocates `_hb_bit_storage(max_value)` bits; nested lookups keep `lookup_mask` | 2.6.0 (**no CFF2**) | ✅ works with glyf TTF (source-verified) |
| 8.0 (26/27) | same | same | L687 identical | same | **1.4.2** | same code | same | 2.7.1 (**no CFF2**) | ✅ glyf TTF; measured on Robolectric SDK 26 Java path |
| 9 (28) | same | same | `SplitIterator` version, same semantics | `LayoutCache.h`: `if (paint.skipCache())` | **1.7.4** (NEWS/README.version; `hb-version.h` is stale at 1.4.2) | same | same | 2.9.0 (CFF2 OK) | ✅ |
| 10 (29) | same | same | `LayoutCore.cpp` L192 identical | `skipCache() \|\| length ≥ LENGTH_LIMIT_CACHE` | **2.3.0** | same | same | 2.9.1 | ✅ |
| 11 (30) | same | `libs/hwui/jni/Paint.cpp`, same | identical | same | **2.6.4** | same | same | 2.10.1 | ✅ **measured** (Robolectric native = HB 2.6.4) |
| 12+ (31–37) | — | — | `FontFeatureUtils.cpp` keeps values (report 04) | — | newer | — | — | — | ✅ (report 04 + Robolectric SDK 33/36) |

In every row, letter-spacing above 0.03 em adds `liga=0, clig=0` before user features, so **never use letter-spacing** (as report 04 said) [verified: 7.0 `Layout.cpp` L715–727].

**Robolectric NATIVE measurement** (`Paint.textSize = 1000`, `getRunAdvance(…, isRtl = true, …)`, the same three fonts) [measured: build logs 17-kashida-26…36]:

| SDK (Java framework) | cv-glyf TTF, الْكِتٰبُ cv01 = 0/1/2/3/6 | بِسْمِ cv01 = 0…6 | `cv01=3` (equals syntax) | QUL TTF | CFF2 OTF |
|---|---|---|---|---|---|
| 26, 28, 30, 33, 36 | **2060 / 2847 / 3615 / 4384 / 6691** | 1216 / 1405 / 1594 / 1783 / 2350 | 4384 | 2060 at every value | identical to TTF (the native runtime's FreeType is modern) |

**Caveats on this measurement** [verified: `nativeruntime-dist-compat-1.0.19.jar`, `objdump hb_version` → 2.6.4; logs 15, 18]:
- Robolectric ships **one** host build of HWUI, Minikin, HarfBuzz and Skia for all SDKs. It contains HarfBuzz **2.6.4** (`hb_version` disassembled), ICU 68 (`icudt68l.dat`) and a `3.32.2` version string that is probably its SQLite [inferred].
- So the SDK axis only varies the Java framework path. The 1.2.6 / 1.4.2 / 1.7.4 behaviour rests on the source reading above.
- NATIVE mode is unavailable below SDK 26. SDK 25 returned `measureText("Hello") = 5.0` (legacy fake metrics). The SDK 24 `android-all` jar fetch failed in this sandbox.

**Verdict per API level:**

| API | `cv01=N` kashida via `Paint.setFontFeatureSettings` | Confidence | Condition |
|---|---|---|---|
| 24–25 | Works | Medium-High (source only) | glyf TTF (FreeType 2.6.0 has no CFF2) |
| 26–27 | Works | Medium-High (source + Java path measured) | glyf TTF (FreeType 2.7.1) |
| 28–29 | Works | High | — |
| 30 | Works | High (HarfBuzz 2.6.4 measured) | — |
| 31–37 | Works | High | — |

There is **no need for NDK/HarfBuzz or for a minSdk raise**. The only remaining unknown is a real-device smoke test on API 24/26: the FreeType/Skia advance path on actual 7.x images.

### 2.8 Compiling `gradle/libs.versions.toml` (exactly as built)

```toml
[versions]
agp = "9.4.1"
kotlin = "2.4.21"                 # fallback "2.3.21" verified; "2.2.10" FAILS (adhan2 0.0.7 metadata 2.4)
ksp = "2.3.12"
androidxActivity = "1.13.0"
androidxAppCompat = "1.8.0"
androidxCore = "1.19.1"
androidxComposeBom = "2026.09.00"
androidxLifecycle = "2.11.0"
androidxNavigation3 = "1.2.0"
androidxHilt = "1.4.0"
androidxRoom3 = "3.0.3"
androidxRoom = "2.8.5"            # fallback only (FTS4)
androidxSqlite = "2.7.1"
androidxMedia3 = "1.11.1"
androidxWork = "2.12.0"
androidxGlance = "1.2.0"
androidxStartup = "1.2.0"
androidxTest = "1.7.0"
androidxTestExtJunit = "1.3.0"
hilt = "2.60.1"
kotlinxCoroutines = "1.11.0"
kotlinxSerialization = "1.11.0"
kotlinxDatetime = "0.8.0"
ktor = "3.6.0"
okhttp = "5.5.0"
desugarJdkLibs = "2.1.5"
adhan2 = "0.0.7"
junit4 = "4.13.2"
truth = "1.4.5"
robolectric = "4.17"

[libraries]
android-desugarJdkLibs = { group = "com.android.tools", name = "desugar_jdk_libs", version.ref = "desugarJdkLibs" }
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "androidxCore" }
androidx-appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "androidxAppCompat" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "androidxActivity" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "androidxComposeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "androidxLifecycle" }
androidx-lifecycle-viewmodel-navigation3 = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-navigation3", version.ref = "androidxLifecycle" }
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
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "androidxRoom" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "androidxRoom" }
androidx-sqlite-bundled = { group = "androidx.sqlite", name = "sqlite-bundled", version.ref = "androidxSqlite" }
androidx-sqlite-bundled-jvm = { group = "androidx.sqlite", name = "sqlite-bundled-jvm", version.ref = "androidxSqlite" }  # host .so for Robolectric
androidx-media3-exoplayer = { group = "androidx.media3", name = "media3-exoplayer", version.ref = "androidxMedia3" }
androidx-media3-session = { group = "androidx.media3", name = "media3-session", version.ref = "androidxMedia3" }
androidx-media3-datasource-okhttp = { group = "androidx.media3", name = "media3-datasource-okhttp", version.ref = "androidxMedia3" }
androidx-media3-ui-compose-material3 = { group = "androidx.media3", name = "media3-ui-compose-material3", version.ref = "androidxMedia3" }
androidx-work-runtime-ktx = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "androidxWork" }
androidx-work-testing = { group = "androidx.work", name = "work-testing", version.ref = "androidxWork" }
androidx-glance-appwidget = { group = "androidx.glance", name = "glance-appwidget", version.ref = "androidxGlance" }
androidx-startup-runtime = { group = "androidx.startup", name = "startup-runtime", version.ref = "androidxStartup" }
androidx-test-core = { group = "androidx.test", name = "core", version.ref = "androidxTest" }
androidx-test-ext-junit = { group = "androidx.test.ext", name = "junit", version.ref = "androidxTestExtJunit" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "kotlinxCoroutines" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "kotlinxCoroutines" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
kotlinx-datetime = { group = "org.jetbrains.kotlinx", name = "kotlinx-datetime", version.ref = "kotlinxDatetime" }
ktor-client-core = { group = "io.ktor", name = "ktor-client-core", version.ref = "ktor" }
ktor-client-okhttp = { group = "io.ktor", name = "ktor-client-okhttp", version.ref = "ktor" }
ktor-client-content-negotiation = { group = "io.ktor", name = "ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-kotlinx-json = { group = "io.ktor", name = "ktor-serialization-kotlinx-json", version.ref = "ktor" }
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-mockwebserver3 = { group = "com.squareup.okhttp3", name = "mockwebserver3", version.ref = "okhttp" }
adhan2 = { group = "com.batoulapps.adhan", name = "adhan2", version.ref = "adhan2" }
junit4 = { group = "junit", name = "junit", version.ref = "junit4" }
truth = { group = "com.google.truth", name = "truth", version.ref = "truth" }
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }   # root `apply false` lifts AGP's KGP 2.2.10
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
room3 = { id = "androidx.room3", version.ref = "androidxRoom3" }
room = { id = "androidx.room", version.ref = "androidxRoom" }               # fallback only
```

Report 07's full TOML (§2.12) remains valid. It is a superset of this one: quality plugins, Coil, Play libraries and so on, none of which were compiled here. Add `sqlite-bundled-jvm` and `hilt-android-testing` (already listed there).

### 2.9 Build snippets that compiled

Root `build.gradle.kts`, plus `gradle-wrapper.properties` → `gradle-9.8.1-bin.zip`:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false            // KGP 2.2.10 -> 2.4.21 (buildEnvironment-verified)
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room3) apply false
}
```

`:core:content`, with the Robolectric/bundled-SQLite block to move into a `criterion.android.robolectric` convention plugin:

```kotlin
plugins { alias(libs.plugins.android.library); alias(libs.plugins.ksp); alias(libs.plugins.room3) }
android {
    namespace = "com.chughtai.criterion.core.content"
    compileSdk { version = release(37) }
    defaultConfig { minSdk = 24 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    testOptions { unitTests { isIncludeAndroidResources = true } }
}
room3 { schemaDirectory("$projectDir/schemas") }
dependencies {
    api(project(":core:model"))
    api(libs.androidx.room3.runtime)            // or keep the DB internal behind a repository (preferred)
    implementation(libs.androidx.sqlite.bundled)
    ksp(libs.androidx.room3.compiler)
    testImplementation(libs.robolectric); testImplementation(libs.androidx.test.core)
    testImplementation(libs.truth); testImplementation(libs.kotlinx.coroutines.test); testImplementation(libs.junit4)
}
val sqliteNative: Configuration = configurations.create("sqliteNative") { isTransitive = false }
dependencies { sqliteNative(libs.androidx.sqlite.bundled.jvm) }
val hostNativeDir = run {                                   // natives/{linux_x64,linux_arm64,osx_arm64,windows_x64}
    val os = System.getProperty("os.name").lowercase(); val arm = System.getProperty("os.arch") in setOf("aarch64", "arm64")
    when { os.contains("mac") -> "osx_arm64"; os.contains("win") -> "windows_x64"; else -> if (arm) "linux_arm64" else "linux_x64" }
}
val hostNativeName = when { hostNativeDir.startsWith("osx") -> "libsqliteJni.dylib"; hostNativeDir.startsWith("win") -> "sqliteJni.dll"; else -> "libsqliteJni.so" }
val extractSqliteNative = tasks.register<Sync>("extractSqliteNative") {
    from({ zipTree(sqliteNative.singleFile) }) { include("natives/$hostNativeDir/**"); eachFile { path = name }; includeEmptyDirs = false }
    into(layout.buildDirectory.dir("sqlite-native"))
}
tasks.withType<Test>().configureEach {
    dependsOn(extractSqliteNative)
    systemProperty("androidx.sqlite.driver.bundled.path", layout.buildDirectory.dir("sqlite-native").get().asFile.absolutePath)
    systemProperty("androidx.sqlite.driver.bundled.name", hostNativeName)
    jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED", "--add-opens=java.base/java.io=ALL-UNNAMED")
}
```

`:app`, essentials:

```kotlin
plugins {
    alias(libs.plugins.android.application); alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization); alias(libs.plugins.ksp); alias(libs.plugins.hilt)
}
android {
    compileSdk { version = release(37) }
    defaultConfig { minSdk = 24; targetSdk = 36 }
    buildTypes { release { optimization { enable = true } } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17; isCoreLibraryDesugaringEnabled = true }
    buildFeatures { compose = true }
    testOptions { unitTests { isIncludeAndroidResources = true } }
}
dependencies {
    coreLibraryDesugaring(libs.android.desugarJdkLibs)
    implementation(libs.hilt.android); ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.work); ksp(libs.androidx.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    testImplementation(libs.hilt.android.testing); kspTest(libs.hilt.compiler)
    testImplementation(libs.okhttp.mockwebserver3); testImplementation(libs.androidx.work.testing)
    // … Compose, Nav3, Media3, OkHttp, Ktor, Work, Glance, adhan2 as in the TOML
}
// NATIVE-graphics tests (kashida goldens) must run one SDK per JVM; keep them out of the default task:
val roboSdks = providers.gradleProperty("roboSdks")
tasks.withType<Test>().configureEach {
    if (roboSdks.isPresent) systemProperty("robolectric.enabledSdks", roboSdks.get())
    else filter { excludeTestsMatching("*KashidaNativeGraphicsTest*") }
}
```

Manifest pieces that compiled and ran:
- `<service android:name=".audio.PlaybackService" android:exported="true" android:foregroundServiceType="mediaPlayback">` with both `androidx.media3.session.MediaLibraryService` and `android.media.browse.MediaBrowserService` actions;
- the Glance `<receiver>` with `@xml/…widget_info` (`initialLayout="@layout/glance_default_loading_layout"`);
- `androidx.startup.InitializationProvider` with `androidx.work.WorkManagerInitializer` `tools:node="remove"`. Paired with `Application : Configuration.Provider` returning `Configuration.Builder().setWorkerFactory(hiltWorkerFactory)`, this ran the `@HiltWorker` successfully.

### 2.10 Content-DB pipeline script (ran with `python3 -I`; seed for `tools/content-pipeline/`)

```python
"""python3 -I build_content_db.py <schema.json> <uthmani.xml> <simple-clean.xml> <out.db>
   [--no-master] [--extra-tables] [--bad-hash] [--user-version N] [--skip-triggers]"""
import argparse, json, os, re, sqlite3, xml.etree.ElementTree as ET
MARKS = re.compile('[ؐ-ًؚ-ٰٟۖ-ۭـ]')
FOLD = str.maketrans({'أ':'ا','إ':'ا','آ':'ا','ٱ':'ا',
                      'ى':'ي','ی':'ي','ة':'ه','ک':'ك'})
def normalize(s): return MARKS.sub('', s).translate(FOLD)          # must equal :core:model ArabicNormalizer
def ayahs(p):
    for sura in ET.parse(p).getroot().iter('sura'):
        for aya in sura.iter('aya'): yield int(sura.get('index')), int(aya.get('index')), aya.get('text')
ap = argparse.ArgumentParser()
for a in ('schema','uthmani','clean','out'): ap.add_argument(a)
for f in ('--no-master','--extra-tables','--bad-hash','--skip-triggers'): ap.add_argument(f, action='store_true')
ap.add_argument('--user-version', type=int); a = ap.parse_args()
sch = json.load(open(a.schema, encoding='utf-8'))['database']
if os.path.exists(a.out): os.remove(a.out)
db = sqlite3.connect(a.out)
for e in sch['entities']:
    db.execute(e['createSql'].replace('${TABLE_NAME}', e['tableName']))
    for ix in e.get('indices', []): db.execute(ix['createSql'].replace('${TABLE_NAME}', e['tableName']))
for v in sch.get('views', []): db.execute(v['createSql'].replace('${VIEW_NAME}', v['viewName']))
if not a.skip_triggers:
    for e in sch['entities']:
        for t in e.get('contentSyncTriggers', []): db.execute(t)
if not a.no_master:
    for q in sch['setupQueries']: db.execute(q.replace(sch['identityHash'], '0'*32) if a.bad_hash else q)
clean = {(s, n): t for s, n, t in ayahs(a.clean)}
with db:
    db.executemany('INSERT INTO ayah(id,surah,ayah,text_uthmani,text_norm) VALUES (?,?,?,?,?)',
                   [(i, s, n, t, normalize(clean[(s, n)])) for i, (s, n, t) in enumerate(ayahs(a.uthmani), 1)])
    for t in [e['tableName'] for e in sch['entities'] if e.get('ftsVersion')]:
        db.execute(f"INSERT INTO `{t}`(`{t}`) VALUES('rebuild')"); db.execute(f"INSERT INTO `{t}`(`{t}`) VALUES('optimize')")
if a.extra_tables:
    with db:
        db.execute('CREATE TABLE content_meta (k TEXT PRIMARY KEY, v TEXT NOT NULL)')
        db.execute('CREATE TABLE page_line (mushaf TEXT, page INTEGER, line INTEGER, first_word INTEGER, last_word INTEGER, '
                   'PRIMARY KEY (mushaf, page, line)) WITHOUT ROWID')
db.execute(f"PRAGMA user_version = {sch['version'] if a.user_version is None else a.user_version}"); db.commit(); db.execute('VACUUM')
```

`'rebuild'` and `'optimize'` are special commands in both FTS4 and FTS5, so the script built the Room 3 and Room 2 DBs unchanged [verified: logs 23, 25]. For host-native extraction, note that `sqlite-bundled-jvm` 2.7.1 ships only `linux_x64`, `linux_arm64`, `osx_arm64` and `windows_x64`. There is no Intel-mac binary [verified: jar listing].

---

## 3. Recommendations

1. **Freeze the toolchain ADR as proposed:** AGP 9.4.1, Gradle 9.8.1, Kotlin 2.4.21 lifted through the root `kotlin-jvm` alias, KSP 2.3.12, compileSdk `release(37)`, targetSdk 36, minSdk 24, R8 on. Add a CI guard: `./gradlew buildEnvironment | grep 'kotlin-gradle-plugin:2.2.10 -> 2.4.21'`.
   - *Rationale:* proven end to end; it is the newest AGP that stable Android Studio Rabbit 1 syncs (7.1–9.4).
   - **Confidence: High.**
2. **Keep Kotlin 2.3.21 as the documented fallback. Never drop to AGP's bundled 2.2.10 while adhan2 0.0.7 is in the graph.**
   - *Rationale:* 2.3.21 passed everything; 2.2.10 cannot read 2.4.0 metadata.
   - **Confidence: High.**
3. **Room ADR: Room 3.0.3 + `BundledSQLiteDriver` + FTS5.** Use unicode61 for words and trigram for substrings, both external-content over normalised columns. **Room 2.8.5 + FTS4 (report 05's schema) is the named fallback.**
   - *Rationale:* identical SQLite 3.50.1 on all devices (API 24 has 3.9.2 with no FTS5), boolean and trigram search, KMP-ready, and a verified prepackaged path. The fallback is proven, with limits: no explicit AND, no trigram, smaller APK.
   - Re-evaluate the trigram index (about 1.2 MB) once real search UX is tested. Dropping it keeps FTS5.
   - **Confidence: Medium-High.**
4. **Content-DB pipeline contract.**
   - Execute `createSql`, index SQL, `contentSyncTriggers` and `setupQueries` verbatim from the exported schema.
   - Populate, run `rebuild` + `optimize`, set `PRAGMA user_version = @Database.version`, then `VACUUM`. Extra tables (`content_meta`, page/line layout tables) are fine.
   - **Ship with `room_master_table`**, and in CI **also open a `--no-master` build in Robolectric**. The shipped file only gets a hash check, so this is the only test that exercises `onValidateSchema`.
   - Keep the asset-version bump rule from report 07 (bump `@Database(version)` → copy-destructive re-copy).
   - **Confidence: High.**
5. **OkHttp ADR: 5.5.0 everywhere** (Media3 data source, downloads, Ktor engine, `mockwebserver3` in tests). Supersedes report 06's "pin 4.12".
   - *Rationale:* Ktor 3.6.0 declares 5.5.0; Media3's 4.12-compiled module works on 5 (Range reads verified); 4.12 needs a forced downgrade plus source changes.
   - **Confidence: High.**
6. **Keep Ktor 3.6.0 for JSON over the shared OkHttp client.** It compiles and runs. It is optional: if JSON stays at about 5 endpoints, plain OkHttp + kotlinx.serialization removes Ktor's dex weight (part of the 4.3 MB `classes.dex`).
   - **Confidence: Medium.**
7. **Keep `QuranContentDb` `internal` to `:core:content`.** Expose a `ContentRepository` and provide the Hilt binding there. This avoids the `api(room3-runtime)` leak hit in error #2 and keeps Room types out of features.
   - **Confidence: Medium-High.**
8. **Create a `criterion.android.robolectric` convention plugin.** It should:
   - set the `jdk.internal.access` / `java.io` JVM flags;
   - add host `libsqliteJni` extraction from `sqlite-bundled-jvm` (multi-OS);
   - set `isIncludeAndroidResources = true`;
   - add a separate `-ProboSdks=N` path for NATIVE-graphics goldens, one SDK per JVM.
   - **Never name a test-resource folder `fonts/`.**
   - Use `runTest` + `MainDispatcherRule`, never `runBlocking`, around `viewModelScope`.
   - **Confidence: High.**
9. **Kashida ADR: `Paint.setFontFeatureSettings("'cv01' N")` per stretched word, with offline-precomputed N and positions, for API 24+. Use no NDK and no minSdk change.**
   - **Font source: the digitalkhatt.org repo `hb/indopak.otf`** (cv01–cv18, OFL 1.1), pinned to commit `4213fab9…` / sha256 `8110e0c3…`.
   - **Shipped as the CFF2→glyf TTF** (1.63 MB, sha256 `428ae074…` for my conversion; regenerate in the pipeline).
   - **Do not use the QUL CDN `DigitalKhattIndoPak.otf`.** It has no cv features.
   - Set feature strings only on the few stretched words: Minikin bypasses its layout cache for any paint with features.
   - **Confidence: Medium-High (API 24–27), High (28+).**
10. **Run a 1-hour device smoke test before the reader sprint.** Use one API 24 or 25 device (or a KVM-enabled CI emulator) plus one API 26–27. Check:
    - `getRunAdvance` of الْكِتٰبُ = 2060 / 2847 / 4384 at cv01 = 0/1/3 at textSize 1000 with the glyf TTF;
    - `BundledSQLiteDriver` loads and returns FTS5 hits;
    - desugared `java.time` works through adhan2.
    - This sandbox had no `/dev/kvm`.
    - **Confidence: High** (that the test is needed).
11. **CI hygiene.**
    - Use the Gradle cache (`setup-gradle`) and retry settings for Maven Central 429s.
    - `cmdline-tools` 23 deprecates `sdkmanager` in favour of `android sdk`; keep `sdkmanager` for now, but expect a change.
    - Ship an AAB. Universal APKs carry 4 × 1.2 MB of SQLite natives.
    - Exclude `DebugProbesKt.bin` from release packaging.
    - **Confidence: Medium-High.**

---

## 4. Risks & open questions

**Risks**
- **Nothing ran on a real device or emulator.** There was no KVM. Everything runtime-related ran on Robolectric (JVM): bundled SQLite (host `.so`), Media3/OkHttp, Hilt, desugaring (dex inspected, not executed) and kashida (host native runtime). Device-only failure modes remain possible but unlikely, especially on API 24/25, which Robolectric NATIVE cannot even emulate.
  - **Mitigation:** recommendation 10.
- **The Robolectric native runtime is one build (HarfBuzz 2.6.4, ICU 68) for all SDKs.** Golden tests catch our regressions, not OEM or old-platform shaping differences.
- **Room 3 youth.** 3.0.3 is about 3.5 months stable. A Room 3 regression would hit both DBs. The fallback (Room 2.8.5) is proven but costs FTS5 features.
- **The "hash-only" fast path hides schema drift in the shipped DB.** If CI forgets the `--no-master` open test, a pipeline bug could ship and only fail at query time.
- **OkHttp 5 vs future Media3.** If Media3 ever pins `strictly 4.x` (unlikely), Gradle will refuse resolution. Watch Media3 release notes.
- **Kashida behaviour depends on the upstream DK repo font**, which is version 0.1 and not on QUL's CDN. Upstream changes to lookup order or alternate counts would change widths. Mitigations: pin the commit and hash, and run golden tests.
- **AGP 9.4.1 emits Gradle-11 deprecations** (`Configuration.setVisible`). This is harmless now; it would matter if we jump to Gradle 11 before an AGP update.

**Open questions**
1. Ship the FTS5 **trigram** index in v1 (+about 1.2 MB, better Urdu and partial search), or start with unicode61 + prefix only?
2. Is Ktor worth its dex cost for about 5 JSON endpoints, or should we use plain OkHttp + kotlinx.serialization (report 07 rated this Medium)?
3. Can Tarteel/QUL publish the cv-enabled DigitalKhatt IndoPak build? Or should we confirm with DigitalKhatt (Amine Anane) that redistributing the converted TTF under OFL is fine? OFL allows modified versions, but the Reserved Font Name status needs checking.
4. Which physical API 24/25 device will the owner use for the smoke test? Pakistani market phones from 2016–2017 (Samsung J-series, QMobile) are the realistic targets.
5. Should CI get a KVM runner (GitHub-hosted Linux runners support it) so that Gradle Managed Devices can run API 24/30/36 instrumented tests nightly?

---

## 5. Sources

**SDK and IDE**
- https://dl.google.com/android/repository/repository2-3.xml — cmdline-tools 23.0 = `commandlinetools-linux-16111833_latest.zip`; platforms 37.0/37.1/37.2 (+betas), 36, 36.1; build-tools 37.0.0; platform-tools 37.0.1; all on the stable channel.
- https://dl.google.com/android/repository/commandlinetools-linux-16111833_latest.zip — installed; `sdkmanager` deprecation notice.
- https://developer.android.com/studio/releases — Rabbit 1 2026.2.1 current stable; AGP compatibility table (Rabbit 1 / Quail 4 → 7.1–9.4, Quail 3/2 → 7.1–9.3, …); minimum Studio/AGP per API (37.0 → Panda 3 2025.3.3 Patch 1 / AGP 9.1.1).
- https://services.gradle.org/versions/current — Gradle 9.8.1 (2026-10-07).

**Maven artifacts inspected**
- https://dl.google.com/android/maven2/com/android/tools/build/gradle/maven-metadata.xml and `/9.4.1/gradle-9.4.1.pom` — AGP 9.4.1 latest stable; runtime KGP 2.2.10, KSP 2.2.10-2.0.2.
- Google Maven `maven-metadata.xml` for room3-runtime (3.0.3; 3.1.0-alpha01 newest), room-runtime 2.8.5, sqlite-bundled (2.7.1; 2.8.0-alpha01), media3-datasource-okhttp 1.11.1, navigation3-ui, hilt-work 1.4.0, compose-bom 2026.09.00, glance-appwidget, work-runtime 2.12.0.
- Maven Central `maven-metadata.xml` for hilt-android 2.60.1, okhttp 5.5.0, adhan2 0.0.7, KSP 2.3.12, kotlin-gradle-plugin (2.3.21 / 2.4.21 / 2.5.0-Beta1), robolectric 4.17; Kotlin JVM plugin markers 2.3.21 and 2.4.21 on Central and the Plugin Portal.
- https://dl.google.com/android/maven2/androidx/media3/media3-datasource-okhttp/1.11.1/media3-datasource-okhttp-1.11.1.pom — okhttp 4.12.0.
- https://repo1.maven.org/maven2/io/ktor/ktor-client-okhttp-jvm/3.6.0/ktor-client-okhttp-jvm-3.6.0.pom — okhttp-jvm 5.5.0, okio 3.18.2.
- https://repo1.maven.org/maven2/com/squareup/okhttp3/okhttp/5.5.0/okhttp-5.5.0.module — android/jvm variants; `okhttp-android-5.5.0.aar` minSdk 21.
- https://repo1.maven.org/maven2/com/batoulapps/adhan/adhan2/0.0.7/adhan2-0.0.7.module and `.pom` — stdlib 2.4.0, kotlinx-datetime 0.8.0, MIT, no Android variant.
- `androidx/room3/room3-common-jvm/3.0.3`, `room3-runtime-android/3.0.3`, `room3-gradle-plugin/3.0.3` sources jars — `@Fts5`, `createFromAsset`/`createFromInputStream`, `RoomConnectionManager` validation and hash logic, `PrePackagedCopySQLiteDriver`, plugin AGP ≥ 8.10 and schemaDirectory requirement.
- `androidx/sqlite/sqlite-bundled-android/2.7.1` (AAR + sources) and `sqlite-bundled-jvm/2.7.1` jar — device ABIs only in the AAR; `NativeLibraryLoader` system properties; host natives for linux_x64/arm64, osx_arm64, windows_x64; minSdk 23.
- `androidx/media3/media3-session/1.11.1` sources/AAR — `ControllerInfo.createTestOnlyControllerInfo(…, isPackageNameVerified)`; minSdk 23. `room3-runtime-android-3.0.3.aar` minSdk 23.
- `org.robolectric:nativeruntime-dist-compat:1.0.19` (via Gradle) — single host `librobolectric-nativeruntime.so`; `hb_version` = 2.6.4; a `3.32.2` version string (probably SQLite); ICU 68; fonts/fonts.xml. `org.robolectric:nativeruntime:4.17` — `DefaultNativeRuntimeLoader` copies `fonts/` from the classpath.

**Changelogs, docs, code**
- https://raw.githubusercontent.com/square/okhttp/master/CHANGELOG.md — 5.0.0 (2025-07-02) notes, 5.5.0 (2026-08-16).
- https://gitlab.freedesktop.org/freetype/freetype/-/raw/master/docs/CHANGES — "CHANGES BETWEEN 2.7.1 and 2.8 (2017-May-13) … Support for the new CFF2 font format".
- `git clone --depth 1 https://github.com/batoulapps/adhan-kotlin` — README API (`PrayerTimes`, `CalculationMethod.KARACHI`, `Madhab.HANAFI`), MIT LICENSE, `kotlin.time.Instant`.
- `git clone --filter=blob:none https://github.com/DigitalKhatt/digitalkhatt.org` @4213fab9892774f74f7164c70a70dd84a14f0215 (2026-01-14) — `ClientApp/src/assets/fonts/hb/indopak.otf` (cv01–cv18, OFL name-table string), `coretext/DigitalKhattIndoPak.ttf` (no cv); repo MIT.
- https://static-cdn.tarteel.ai/qul/fonts/dk/DigitalKhattIndoPak.otf — 494,828 B, OFL, no cvNN features.
- https://tanzil.net/pub/download/index.php (uthmani and simple-clean XML, 6,236 ayahs each, "Tanzil Quran Text (Uthmani, Version 1.1) … CC BY 3.0 … CHANGING IT IS NOT ALLOWED") — used only as local test data.
- Web search on the Robolectric FileDescriptor error — no Robolectric issue found. The `--add-exports jdk.internal.access` approach is used by AOSP's Ravenwood config ([gitlab.e.foundation commit 91412af](https://gitlab.e.foundation/e/os/android_build/-/commit/91412af6e2c6f56ff29c5ccf002bf547bdcbe8ac), [gerrit.omnirom.org](https://gerrit.omnirom.org/plugins/gitiles/android_build/+/91412af6e2c6f56ff29c5ccf002bf547bdcbe8ac%5E%21)); [robolectric.org/simulator](https://robolectric.org/simulator/) lists other `--add-exports`. The fix was then confirmed locally (build log 05).

**AOSP (all via `https://android.googlesource.com/<project>/+/refs/tags/<tag>/<path>?format=TEXT`)**
- `platform/frameworks/minikin`:
  - `libs/minikin/Layout.cpp` @7.0.0_r1 (addFeatures L684, cache key without features, letter-spacing liga/clig), @8.0.0_r1 (L687), @9.0.0_r1 (L430);
  - `libs/minikin/LayoutCore.cpp` @10.0.0_r1, @11.0.0_r1 (L192, L373);
  - `include/minikin/MinikinFont.h` @7/8/9 and `MinikinPaint.h` @10/11 (`skipCache()`);
  - `include/minikin/LayoutCache.h` @9/10/11.
- `platform/frameworks/base`:
  - `graphics/java/android/graphics/Paint.java` @7.0/8.0/9/10/11;
  - `core/jni/android/graphics/Paint.cpp` @7.0/8.0/9/10;
  - `libs/hwui/jni/Paint.cpp` @11;
  - `libs/hwui/hwui/MinikinUtils.cpp` @7.0/8.0.
- `platform/external/harfbuzz_ng`:
  - `src/hb-version.h`, `NEWS`, `README.version` @7.0–11 (1.2.6, 1.4.2, 1.7.4, 2.3.0, 2.6.4);
  - `src/hb-shape.cc` @7.0 (`parse_feature_tag` quotes, value postfix);
  - `src/hb-ot-layout-gsub-table.hh` @7.0–11 (`alt_index`);
  - `src/hb-ot-map.cc` @7.0 (`max_value`, `bits_needed`);
  - `src/hb-ot-layout-gsubgpos-private.hh` @7.0 (`recurse`, `lookup_mask`).
- `platform/external/freetype`: `include/freetype.h` @7.0 (2.6.0); `include/freetype/freetype.h` @8.0/9/10/11 (2.7.1, 2.9.0, 2.9.1, 2.10.1).
- `platform/external/sqlite`: `dist/Android.mk`, `dist/sqlite3.h`, `dist/sqlite3.c` @7.0.0_r1 (3.9.2; FTS3/FTS4/ICU flags; `SQLITE_DISABLE_FTS3_UNICODE` guard).

**Local build evidence**
- In `<scratch>/repos/gap-agp9-toolchain-build-spike/logs/`:
  - `01-buildEnvironment`, `02/03-assembleDebug`, `04/05-content-test`, `06-assembleRelease`, `07–09-app-test`, `10-full-clean`, `11-deprec`, `12-deps`, `13–18` kashida and native probes (`kashida-results.txt`);
  - `20-k23`, `21-k22`, `22-ok4` (+ `-attempt1`), `23/24/25-room2`, `26-ok4`, `30-final`.
- Skeleton and scripts: `…/gap-agp9-toolchain-build-spike/spike-skeleton.tgz`; variants in `…/var-{k23,k22,ok4,room2}/`.
- Font measurements: `…/scripts/hb_widths.py`, `font_features.py`, `cff2_to_glyf.py`; fonts in `…/fonts/`.
