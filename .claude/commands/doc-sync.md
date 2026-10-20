---
description: Compare this branch with docs/plan and propose doc edits (doc 13 §10.3)
argument-hint: <story-id>
---
Compare this branch's diff (`git diff origin/master...HEAD`) with `docs/plan/`. Do not change code.

1. List every place where the code now differs from docs 01–12 (file:section → what differs).
2. For each: propose the exact doc edit (replacement text), or say "needs ADR" if it touches a D-xx decision.
3. Update story $ARGUMENTS's row in `docs/plan/PROGRESS.md` (status, branch, PR link). Add any split or new
   stories to doc 09 with the next IDs, each with a PROGRESS.md row.
4. Update `CLAUDE.md` if commands, modules or rules changed.
5. Output a checklist of the doc edits made, for the PR description.
