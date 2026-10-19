---
description: Read-only Play policy review (doc 13 §8.6, MA-5)
---
Run MA-5 (doc 13 §8.6) with parallel read-only agents. No agent edits files. Each finding is
`[blocker|major|minor] path:line — policy — evidence — exact fix`.

- Manifest: the merged manifests for `playPublicRelease`, `fossPublicRelease` and `fossPersonalRelease`
  match doc 04's allowed list exactly; nothing from the CLAUDE.md #12 never-list; FGS types declared;
  exported components justified; targetSdk 36.
- Data safety: no data-collecting SDK in the dependency tree; answers match doc 11's v1 draft ("No data
  collected"); backup rules exclude audio and packs.
- Behaviour: playback starts only from user actions; the exact-alarm explainer and inexact fallback exist;
  no full-screen intents; POST_NOTIFICATIONS requested in context.
- Listing: no reference-app name, icon, screenshots or text (D-19); content rating answers; target audience
  13+; no Families-policy triggers.
- Packaging: AAB with no language splits; 16 KB alignment; size under the doc 10 budget; R8 mapping kept.

Merge, dedupe and rank the findings for the release PR.
