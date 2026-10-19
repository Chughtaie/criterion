---
description: Climb the doc 13 §5.1 verification ladder up to a level and print the evidence block
argument-hint: <level V0..V4>
---
Run the doc 13 §5.1 verification ladder up to level $ARGUMENTS for the current branch. V5 is owner-only:
write its checklist instead of running it.

- V0: `./gradlew spotlessCheck :app:assembleFossPersonalDebug` (until those tasks exist, the narrowest
  task that compiles and formats the changed code).
- V1: `./gradlew quickCheck`, or the module's `testDebugUnitTest` / JVM `test` task. New logic has new tests;
  show red → green where practical.
- V2: the Robolectric / Room / Media3 / WorkManager / MockWebServer integration tests for the change.
- V3: `verifyRoborazziDebug`. Open every `*_compare.png` and `*_actual.png`; classify each diff as intended,
  regression or environment (doc 13 §5.3). Never re-record to make a test pass.
- V4: install and launch on the API 36 AVD (plus API 24/26 AVDs if available), exercise the story's flow,
  `adb exec-out screencap -p > build/shots/<name>.png`, open every screenshot and write one line per shot
  on what it shows, and check `adb logcat -d` for crashes and StrictMode violations.

Capture the commands' actual output; never summarise from memory. Then print the evidence block in the
doc 13 §5.2 format, ending with a "NOT verified (owner to do)" line.
