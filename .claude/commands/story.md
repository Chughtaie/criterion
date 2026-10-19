---
description: Load a doc 09 story, fill the doc 13 §6.1 template and plan it before any code
argument-hint: <story-id, e.g. E-01-S02>
---
Plan story $ARGUMENTS. Do not write code yet.

1. Read `CLAUDE.md` and the story's row in `docs/plan/09-roadmap-backlog.md` §11.2: scope and key tasks,
   features, estimate, dependencies, sprint and tier.
2. Check the Definition of Ready (doc 09 §13): every dependency is merged on `master` (check
   `docs/plan/PROGRESS.md` and `git log origin/master`), no gate (G1–G6) blocks it, and it is not owner-only
   (tier ADM). If any check fails, stop and say why.
3. Find the doc 13 §7 prompt (AP-xx) that covers the story and every spec section the story and prompt cite.
   Read only those sections; grep research reports instead of reading them whole. README §13 rulings win
   over any single doc, and doc 04 owns shared type and module names.
4. Map the existing code the story touches.
5. Fill the doc 13 §6.1 template: CONTEXT, GOAL, CONSTRAINTS, ACCEPTANCE CRITERIA, VERIFICATION (the level
   doc 13 §5.1 requires for this story type), OUT OF SCOPE, DELIVERABLES.
6. Enter plan mode. The plan fits on one screen: goal; files to create or change; public interfaces with
   signatures; tests to add; verification commands; risks; open questions.
7. List the open questions before any code. Wait for the owner to approve the plan.
