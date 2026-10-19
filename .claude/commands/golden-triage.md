---
description: Triage failing Roborazzi goldens (doc 13 §8.4, MA-3)
---
Run MA-3 (doc 13 §8.4) on the current Roborazzi failures. Do not re-record anything.

1. Collect: list the failing goldens and the commits since the last green `master`. If the mushaf pack
   changed, also collect the pipeline's `--changed-since` page list and `delta-<build>.pdf` (doc 08 §5.6).
2. Group: bucket by component (page template, furniture, markers, text layer, highlight, chrome, theme).
3. Fan out one read-only triager per bucket. Each opens the `*_compare.png` images and classifies every
   diff as intended, regression or environment, with the likely cause (file or commit) and a confidence.
4. Bisect regressions only: `git bisect run ./gradlew :core:mushaf-render:verifyRoborazziDebug` over the
   suspect range.
5. Report a table of golden, class, cause and action. Intended diffs go into one re-record commit with the
   compare images embedded in the PR for owner approval; pages with text changes go into the next AP-25
   hafiz packet. Environment diffs: stop and ask the owner (the toolchain is frozen).
