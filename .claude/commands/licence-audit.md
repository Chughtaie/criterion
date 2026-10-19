---
description: Read-only licence audit (doc 13 §8.5, MA-4)
---
Run MA-4 (doc 13 §8.5) with five parallel read-only agents. No agent edits files. Each finding is
`[blocker|major|minor] path:line or manifest id — rule violated — evidence — exact fix`.

- Code provenance: repo source, NOTICE, file headers → missing Apache/MIT port headers; any file that names
  a GPL repo as a source (CLAUDE.md #1).
- Content eligibility: `index.json`, `catalogue.json`, `bundled.json` per tier source set, `data/pins.lock`,
  `docs/legal/permissions.yml`, the assets listing and the doc 03 matrix → an item × tier (PERSONAL / BETA /
  PUBLIC [+MONETIZED]) matrix with licenceStatus + basis. Every shipped item must be eligible for its tier;
  non-commercial items must be absent from any monetized build (D-17, D-29).
- Fonts & assets: `*.ttf/otf` name tables (fontTools), ornament provenance → OFL strings for DigitalKhatt and
  Noto Nastaliq; no KFGQPC modifications; no QuranWBW or quran.com fonts.
- Libraries: AboutLibraries report vs `gradle/libs.versions.toml` → unknown or copyleft libraries.
- Attribution: credits screen strings vs doc 03's required strings (Tanzil CC BY 3.0, QuranEnc, Noor
  International Center, GeoNames CC BY 4.0, DontKillMyApp CC-BY, QUL/DigitalKhatt).

Merge, dedupe and rank the findings. The pass criterion is zero blockers.
