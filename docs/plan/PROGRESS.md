# Progress ledger

One row per [doc 09](09-roadmap-backlog.md) story (§11.2). Doc 09 owns scope, estimates, dependencies and
sprints; this file only tracks delivery. Each story PR updates its own row (status, branch, PR link) in the
same PR.

- **Status:** `todo` · `in-progress` · `in-review` (PR open) · `done` (merged to `master`) · `blocked` (say why in the row).
- **Who:** `agent` = coding-agent story · `owner` = only the owner can do it (outreach, procurement, signing, Play Console, publishing) ·
  `shared` = the agent builds the tooling or checklist and the owner runs or approves it (devices, hafiz, keys, listing).
- **Sprint:** doc 09's "When" column. `S7*` = unfunded until lever 1 funds it; `L1`–`L9` = pre-cut ladder rungs; `cond.` = only if a gate fails.

## Chores (no story ID)

| Branch | What | Status | PR |
|---|---|---|---|
| `chore/agent-setup` | CLAUDE.md, `.claude/` settings + guard hook + commands, PR template, this ledger, `.gitignore` | done | [#2](https://github.com/Chughtaie/criterion/pull/2) |

## Stories

### E-01 Toolchain & project skeleton

| ID | Title | Sprint | Tier | Who | Status | Branch | PR |
|---|---|---|---|---|---|---|---|
| E-01-S01 | Toolchain upgrade PR | S0 | P0 | agent | done | `e01-s01-toolchain-upgrade` | [#3](https://github.com/Chughtaie/criterion/pull/3) |
| E-01-S02 | Convention plugins in `build-logic/convention` | S0 | P0 | agent | done | `e01-s02-convention-plugins` | [#4](https://github.com/Chughtaie/criterion/pull/4) |
| E-01-S03 | Lean module skeleton (doc 04 §3, A2) | S0 | P0 | agent | in-review | `e01-s03-module-skeleton` | [#6](https://github.com/Chughtaie/criterion/pull/6) |
| E-01-S04 | Variants, manifest & signing | S0 | P0 | agent | todo |  |  |
| E-01-S05 | Device smoke test + platform spikes | S0 | P0 | shared | todo |  |  |
| E-01-S06 | App shell & navigation | S2 | P1 | agent | todo |  |  |
| E-01-S07 | Test-device procurement (owner) | S0 | ADM | owner | todo |  |  |
| E-01-S08 | Module-boundary guard and data/DI/test conventions (split from E-01-S02) | S0 | P0 | agent | in-review | `e01-s08-boundaries-data-conventions` | [#5](https://github.com/Chughtaie/criterion/pull/5) |

### E-02 CI & quality gates

| ID | Title | Sprint | Tier | Who | Status | Branch | PR |
|---|---|---|---|---|---|---|---|
| E-02-S01 | PR pipeline | S0 | P0 | agent | todo |  |  |
| E-02-S02 | Content-DB CI | S2 | P1 | agent | todo |  |  |
| E-02-S03 | Mushaf golden infra + core set | S4 | P1 | agent | todo |  |  |
| E-02-S04 | Content-eligibility & licence gates | S5 (days 1–3) | F | agent | todo |  |  |
| E-02-S05 | Baseline profile + Macrobenchmark 1.5.0 | S7 | F | agent | todo |  |  |
| E-02-S06 | Release job | S7 | F | agent | todo |  |  |
| E-02-S07 | Full PR golden set + nightly 548 pages | S6 | C | agent | todo |  |  |

### E-03 Content pipeline & content DB (tools/)

| ID | Title | Sprint | Tier | Who | Status | Branch | PR |
|---|---|---|---|---|---|---|---|
| E-03-S01 | Pipeline skeleton & pinned inputs | S0 | P0 | agent | todo |  |  |
| E-03-S02 | DK text & Kufan keys | S0 | P0 | agent | todo |  |  |
| E-03-S03 | Font build | S0 | P0 | agent | todo |  |  |
| E-03-S04 | 16-line layout join | S1 | P1 | agent | todo |  |  |
| E-03-S05 | Indexing metadata | S1 | P1 | agent | todo |  |  |
| E-03-S06 | Justification precompute | S1 | P1 | agent | todo |  |  |
| E-03-S07 | Validation gates & proof PDF | S2 | P1 | agent | todo |  |  |
| E-03-S08 | Search columns | S5 | C | agent | todo |  |  |
| E-03-S09 | Two bundled translation packs | S2 | P1 | agent | todo |  |  |
| E-03-S10 | Downloadable-pack publishing & signed `index.json` | v1.1 | B | agent | todo |  |  |
| E-03-S11 | Clean-room tooling + supervision (only if G1 fails) | cond. | X | agent | todo |  |  |
| E-03-S12 | Mushaf pack writer (doc 05 schema) | S1 | P1 | agent | todo |  |  |

### E-04 Mushaf renderer (:core:mushaf-render)

| ID | Title | Sprint | Tier | Who | Status | Branch | PR |
|---|---|---|---|---|---|---|---|
| E-04-S01 | Renderer spike | S0 | P0 | agent | todo |  |  |
| E-04-S02 | PageGeometry, typeface load, PaintCache | S1 | P1 | agent | todo |  |  |
| E-04-S03 | Text layer | S1 | P1 | agent | todo |  |  |
| E-04-S04 | Furniture & ornament layer | S2 | P1 | agent | todo |  |  |
| E-04-S05 | Hit-testing & highlight layer | S2 | P1 | agent | todo |  |  |
| E-04-S06 | Mushaf themes | S3 | P1 | agent | todo |  |  |
| E-04-S07 | Width-drift guard | S7* | C (+X) | agent | todo |  |  |
| E-04-S08 | Page cache & performance | S7* | C | agent | todo |  |  |
| E-04-S09 | Final ornament integration | S5 | F | agent | todo |  |  |

### E-05 Reader UX

| ID | Title | Sprint | Tier | Who | Status | Branch | PR |
|---|---|---|---|---|---|---|---|
| E-05-S01 | RTL pager | S2 | P1 | agent | todo |  |  |
| E-05-S02 | Reader chrome | S3 | P1 | agent | todo |  |  |
| E-05-S03 | Ayah action sheet | S3 | P1 | agent | todo |  |  |
| E-05-S04 | Follow-along | S3 | P1 | agent | todo |  |  |
| E-05-S05 | Locks | S3 | P1 | agent | todo |  |  |
| E-05-S06 | Pinch zoom (floor: P1 Must, never cut) | S6 | C | agent | todo |  |  |
| E-05-S07 | Tafsir-summary sheet (Ahsan-ul-Bayan footnotes) | L1 | S | agent | todo |  |  |
| E-05-S08 | Vertical auto-scroll mode with speed | v1.1 | B | agent | todo |  |  |
| E-05-S09 | Page-curl animation option | v1.1 | B | agent | todo |  |  |

### E-06 Indexes & navigation

| ID | Title | Sprint | Tier | Who | Status | Branch | PR |
|---|---|---|---|---|---|---|---|
| E-06-S01 | Surah index (Read + Play) | S2 | P1 | agent | todo |  |  |
| E-06-S02 | Para index | S2 | P1 | agent | todo |  |  |
| E-06-S03 | Manzil index (Read + Play) | S2 | P1 | agent | todo |  |  |
| E-06-S04 | Go to page (labels) / surah:ayah | S4 | P1 | agent | todo |  |  |
| E-06-S05 | Quick access | S4 | P1 | agent | todo |  |  |
| E-06-S06 | Home | S3 | P1 | agent | todo |  |  |
| E-06-S07 | Ruku list + Play ruku | L9 | S | agent | todo |  |  |
| E-06-S08 | Sajda list | L9 | S | agent | todo |  |  |
| E-06-S09 | Para-quarter navigation list | L2 | S | agent | todo |  |  |

### E-07 Bookmarks, last-read & user data

| ID | Title | Sprint | Tier | Who | Status | Branch | PR |
|---|---|---|---|---|---|---|---|
| E-07-S01 | `user.db` v1 + settings | S2 | P1 | agent | todo |  |  |
| E-07-S02 | Auto last-read pointer (per mushaf, separate from bookmarks) | S3 | P1 | agent | todo |  |  |
| E-07-S03 | Ayah & page bookmarks + list screen + undo | S4 | P1 | agent | todo |  |  |
| E-07-S04 | Auto Backup rules (user.db + settings only) | S4 | P1 | agent | todo |  |  |
| E-07-S05 | Bookmark collections/colours (floor: part of the F-BM-02 Must) | S7 | C | agent | todo |  |  |
| E-07-S06 | Notes | v1.1 | B | agent | todo |  |  |
| E-07-S07 | JSON export/import via SAF | v1.1 | B | agent | todo |  |  |

### E-08 Audio engine (playback)

| ID | Title | Sprint | Tier | Who | Status | Branch | PR |
|---|---|---|---|---|---|---|---|
| E-08-S01 | Audio spike | S1 | P1 | agent | todo |  |  |
| E-08-S02 | Playback service & session | S3 | P1 | agent | todo |  |  |
| E-08-S03 | `PlanExpander` | S3 | P1 | agent | todo |  |  |
| E-08-S04 | Gapless timing + `PlaybackStateHub` | S3 | P1 | agent | todo |  |  |
| E-08-S05 | Speed 0.5–2× + sleep timer | S7* | C | agent | todo |  |  |
| E-08-S06 | Player UI | S3 | P1 | agent | todo |  |  |
| E-08-S07 | Streaming cache | S3 | P1 | agent | todo |  |  |
| E-08-S08 | Repeat ayah×N, range×M, pause-after-ayah | S7 | C | agent | todo |  |  |
| E-08-S09 | Gapped verse-by-verse mode | L9 | S | agent | todo |  |  |
| E-08-S10 | Arabic + Urdu translation-audio interleave (personal build only) | v1.1 | B | agent | todo |  |  |

### E-09 Reciter catalogue & timing packs

| ID | Title | Sprint | Tier | Who | Status | Branch | PR |
|---|---|---|---|---|---|---|---|
| E-09-S01 | Catalogue | S2 | P1 | agent | todo |  |  |
| E-09-S02 | Timing pipeline | S1 | P1 | agent | todo |  |  |
| E-09-S03 | Reciter list + favourite qari (preview, favourite) | S4 | P1 | agent | todo |  |  |
| E-09-S04 | Decoded-audio checks for structural-only reads | S5 | C | agent | todo |  |  |
| E-09-S05 | Weekly re-validation job (ETag/length diff → rebuild) | S7 | F | agent | todo |  |  |
| E-09-S06 | Speech-onset snapping | v1.1 | B | agent | todo |  |  |

### E-10 Downloads, storage & packs

| ID | Title | Sprint | Tier | Who | Status | Branch | PR |
|---|---|---|---|---|---|---|---|
| E-10-S01 | Downloader core | S3 | P1 | agent | todo |  |  |
| E-10-S02 | WorkManager long-running `dataSync` worker | S3 | P1 | agent | todo |  |  |
| E-10-S03 | Download UI + offline-first resolution | S4 | P1 | agent | todo |  |  |
| E-10-S04 | Storage manager (usage per reciter, delete, SD card) | S7* | C | agent | todo |  |  |
| E-10-S05 | UIDT job on API 34+ | S7 | F | agent | todo |  |  |
| E-10-S06 | Downloadable-pack installer | v1.1 | B | agent | todo |  |  |
| E-10-S07 | Bundled packs per tier & registry | S2 | P1 | agent | todo |  |  |

### E-11 Translation view & packs

| ID | Title | Sprint | Tier | Who | Status | Branch | PR |
|---|---|---|---|---|---|---|---|
| E-11-S01 | Translation view | S4 | P1 | agent | todo |  |  |
| E-11-S02 | Translation display settings (Urdu / English / both, size) | S7* | C | agent | todo |  |  |
| E-11-S03 | Downloadable translation packs + translator choice per language | v1.1 | B | agent | todo |  |  |

### E-12 Search

| ID | Title | Sprint | Tier | Who | Status | Branch | PR |
|---|---|---|---|---|---|---|---|
| E-12-S01 | SearchRepository | S5 | C | agent | todo |  |  |
| E-12-S02 | Search UI | S5 | C | agent | todo |  |  |
| E-12-S03 | Alias search (Yaseen, Ayatul Kursi, Manzil dua …) | S5 | C | agent | todo |  |  |

### E-13 Onboarding, settings, Urdu UI, accessibility & platform touches

| ID | Title | Sprint | Tier | Who | Status | Branch | PR |
|---|---|---|---|---|---|---|---|
| E-13-S01 | i18n infra | S2 | P1 | agent | todo |  |  |
| E-13-S02 | Urdu UI strings complete | S5 | C | agent | todo |  |  |
| E-13-S03 | Onboarding | S6 | C | agent | todo |  |  |
| E-13-S04 | Settings screen (reader, audio, downloads, language, theme, about) | S7* | C | agent | todo |  |  |
| E-13-S05 | Accessibility baseline | S7 | C | agent | todo |  |  |
| E-13-S06 | Gentle in-app review prompt | v1.1 | B | agent | todo |  |  |
| E-13-S07 | Launcher app shortcuts | v1.1 | B | agent | todo |  |  |

### E-14 Design system & ornaments (freelance designer)

| ID | Title | Sprint | Tier | Who | Status | Branch | PR |
|---|---|---|---|---|---|---|---|
| E-14-S01 | Design tokens & theme | S3 | P1 | agent | todo |  |  |
| E-14-S02 | Designer engagement (owner) | S0 | ADM | owner | todo |  |  |
| E-14-S03 | Core components consolidation | S7* | C | agent | todo |  |  |
| E-14-S04 | Icon & store-graphic integration | S6 | F | agent | todo |  |  |

### E-15 Prayer times, notifications & adhan

| ID | Title | Sprint | Tier | Who | Status | Branch | PR |
|---|---|---|---|---|---|---|---|
| E-15-S01 | `:core:prayer` (adhan2 wrapper) | S6 | A | agent | todo |  |  |
| E-15-S02 | Location | S6 | A | agent | todo |  |  |
| E-15-S03 | Prayer-times screen | S6 | A | agent | todo |  |  |
| E-15-S04 | Prayer/Sehri/Iftar notifications | v1.1 | B | agent | todo |  |  |
| E-15-S05 | Full adhan | v1.1 | B | agent | todo |  |  |
| E-15-S06 | Adhan health check + OEM guidance | v1.1 | B | agent | todo |  |  |
| E-15-S07 | OEM device-matrix test | v1.1 | B | agent | todo |  |  |

### E-16 Hijri, tasbeeh, Ramadan, Manzil dua, azkar, Qibla

| ID | Title | Sprint | Tier | Who | Status | Branch | PR |
|---|---|---|---|---|---|---|---|
| E-16-S01 | Hijri | S6 | A | agent | todo |  |  |
| E-16-S02 | Tasbeeh | L3 | S | agent | todo |  |  |
| E-16-S03 | Ramadan core | S6 | A | agent | todo |  |  |
| E-16-S04 | Ramadan extras | v1.1 | B | agent | todo |  |  |
| E-16-S05 | Manzil dua | L4 | S | agent | todo |  |  |
| E-16-S06 | Morning/evening azkar | v1.1 | B | agent | todo |  |  |
| E-16-S07 | Qibla | v1.1 | B | agent | todo |  |  |

### E-17 Widgets, reminders & share

| ID | Title | Sprint | Tier | Who | Status | Branch | PR |
|---|---|---|---|---|---|---|---|
| E-17-S01 | Glance widget: continue reading | v1.1 | B | agent | todo |  |  |
| E-17-S02 | Glance widgets: next prayer + ayah of the day | v1.1 | B | agent | todo |  |  |
| E-17-S03 | Smart reminders | v1.1 | B | agent | todo |  |  |
| E-17-S04 | Share ayah as image | v1.1 | B | agent | todo |  |  |
| E-17-S05 | Group khatam WhatsApp-shareable sheet | v1.1 | B | agent | todo |  |  |

### E-18 Khatam planner & habits

| ID | Title | Sprint | Tier | Who | Status | Branch | PR |
|---|---|---|---|---|---|---|---|
| E-18-S01 | Khatam planner & tracker | S6 | A | agent | todo |  |  |
| E-18-S02 | Private streaks & stats (freeze days) | v1.1 | B | agent | todo |  |  |

### E-19 Credits, compliance & governance

| ID | Title | Sprint | Tier | Who | Status | Branch | PR |
|---|---|---|---|---|---|---|---|
| E-19-S01 | Sources & Credits | S6 | F | agent | todo |  |  |
| E-19-S02 | Report text error | S6 | F | agent | todo |  |  |
| E-19-S03 | Privacy policy page + Data-safety answers | S5 | F | shared | todo |  |  |
| E-19-S04 | Content-governance policy + provenance log (`docs/legal/`) | S5 | ADM | owner | todo |  |  |
| E-19-S05 | Signed notices (F-PL-11) | S5 | F | agent | todo |  |  |

### E-20 Proofreading (hafiz engagement, G5)

| ID | Title | Sprint | Tier | Who | Status | Branch | PR |
|---|---|---|---|---|---|---|---|
| E-20-S01 | Recruit & contract hafiz/qari | S1 | ADM | owner | todo |  |  |
| E-20-S02 | Round-1 package & triage | S5 | F | shared | todo |  |  |
| E-20-S03 | Proofreading round 2 + G5 certificate | S7 | F | shared | todo |  |  |

### E-21 Permission outreach & legal (G1, G2, G3, G4, G6)

| ID | Title | Sprint | Tier | Who | Status | Branch | PR |
|---|---|---|---|---|---|---|---|
| E-21-S01 | G1 outreach (QUL + DigitalKhatt) | S0 | ADM | owner | todo |  |  |
| E-21-S02 | G2 outreach (audio rights + mp3quran evidence) | S0 | ADM | owner | todo |  |  |
| E-21-S03 | King Fahd Complex ZIPs + licence page | S1 | ADM | owner | todo |  |  |
| E-21-S04 | Translation & azkar permission emails | S1 | ADM | owner | todo |  |  |
| E-21-S05 | G3 trademark self-search + brand decision | S2 | ADM | owner | todo |  |  |
| E-21-S06 | G6 engage Pakistani lawyer | S2 | ADM | owner | todo |  |  |
| E-21-S07 | G4 code-licence memo + QF OAuth application | S6 | ADM | owner | todo |  |  |

### E-22 Play account, store listing & release

| ID | Title | Sprint | Tier | Who | Status | Branch | PR |
|---|---|---|---|---|---|---|---|
| E-22-S01 | Developer account | S0 | ADM | owner | todo |  |  |
| E-22-S02 | Play Console setup | S5 | F | owner | todo |  |  |
| E-22-S03 | Final store listing en + ur-PK | S7 | F | shared | todo |  |  |
| E-22-S04 | Closed test | S5–S6 | F | owner | todo |  |  |
| E-22-S05 | Production | S7 | F | owner | todo |  |  |
| E-22-S06 | Closed-test store listing | S5 | F | shared | todo |  |  |

### E-23 Hosting, domain & signed static files (R2)

| ID | Title | Sprint | Tier | Who | Status | Branch | PR |
|---|---|---|---|---|---|---|---|
| E-23-S01 | R2 bucket, domain, offline Ed25519 keys, publish script | S5 | F | shared | todo |  |  |
| E-23-S02 | App-side signed fetcher (`:core:remote`) | S5 | F | agent | todo |  |  |
| E-23-S03 | Neutral CDN domain + email routing | S0 | ADM | owner | todo |  |  |

### E-24 Hardening & release readiness

| ID | Title | Sprint | Tier | Who | Status | Branch | PR |
|---|---|---|---|---|---|---|---|
| E-24-S01 | Daily-use hardening | S4 | P1 | shared | todo |  |  |
| E-24-S02 | v1.0 release hardening | S7 | F | agent | todo |  |  |
