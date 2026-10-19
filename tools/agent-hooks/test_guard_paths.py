"""Tests for guard_paths.py. Run: python3 -I -m unittest discover -s tools/agent-hooks -v"""

import json
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

HOOK = Path(__file__).resolve().parent / "guard_paths.py"
REPO = HOOK.parents[2]


def run_hook(payload, project=REPO, extra_env=None):
    stdin = payload if isinstance(payload, str) else json.dumps(payload)
    env = dict(os.environ, CLAUDE_PROJECT_DIR=str(project), **(extra_env or {}))
    return subprocess.run(
        [sys.executable, "-I", str(HOOK)], input=stdin, capture_output=True, text=True, env=env, cwd=project
    )


def edit(file_path, tool="Edit", project=REPO, extra_env=None):
    payload = {"tool_name": tool, "cwd": str(project), "tool_input": {"file_path": str(file_path)}}
    return run_hook(payload, project=project, extra_env=extra_env)


def git(*args, cwd):
    subprocess.run(["git", "-c", "user.name=t", "-c", "user.email=t@t", *args], cwd=cwd, check=True, capture_output=True)


class GuardPathsTest(unittest.TestCase):
    def assertBlocked(self, file_path, **kwargs):
        result = edit(file_path, **kwargs)
        self.assertEqual(result.returncode, 2, f"{file_path} should be blocked: {result.stdout}{result.stderr}")
        self.assertIn("guard_paths: blocked", result.stderr)

    def assertAllowed(self, file_path, **kwargs):
        result = edit(file_path, **kwargs)
        self.assertEqual(result.returncode, 0, f"{file_path} should be allowed: {result.stderr}")

    def test_blocks_protected_repo_paths(self):
        for rel in [
            "spikes/toolchain-spike/README.md",
            "docs/research/01-reference-app-teardown.md",
            "core/content/src/main/assets/quran_content.db",
            "app/src/personal/assets/packs/translation.sqlite",
            "core/mushaf-render/src/main/assets/indopak.ttf",
            "tools/cache/indopak.OTF",
            "core/content/schemas/com.chughtai.criterion.core.content.QuranContentDb/1.json",
            "data/pins.lock",
            "data/keys/criterion-2026-a.pub",
            "out/proof/indopak16_taj/build/page-001.png",
        ]:
            with self.subTest(rel=rel):
                self.assertBlocked(REPO / rel)

    def test_matching_ignores_case(self):
        for rel in [
            "docs/RESEARCH/01-reference-app-teardown.md",
            "SPIKES/x.kt",
            "OUT/x",
            "Data/pins.lock",
            "data/PINS.LOCK",
            "data/Keys/k",
            "a/Schemas/1.json",
        ]:
            with self.subTest(rel=rel):
                self.assertBlocked(REPO / rel)

    def test_blocks_git_internals(self):
        self.assertBlocked(REPO / ".git" / "config")
        self.assertBlocked(REPO / "docs" / ".git")

    def test_a_planted_git_file_cannot_move_the_repo_root(self):
        with tempfile.TemporaryDirectory() as tmp:
            repo = Path(tmp).resolve() / "repo"
            (repo / "docs" / "research").mkdir(parents=True)
            git("init", "-q", cwd=repo)
            (repo / "docs" / ".git").write_text(f"gitdir: {repo / '.git' / 'worktrees' / 'fake'}\n")
            self.assertBlocked(repo / "docs" / "research" / "report.md", project=repo)

    def test_linked_worktrees_are_part_of_the_repo(self):
        with tempfile.TemporaryDirectory() as tmp:
            repo = Path(tmp).resolve() / "repo"
            repo.mkdir()
            git("init", "-q", cwd=repo)
            git("commit", "-q", "--allow-empty", "-m", "init", cwd=repo)
            worktree = Path(tmp).resolve() / "repo-story"
            git("worktree", "add", "-q", str(worktree), "-b", "story", cwd=repo)
            self.assertAllowed(worktree / "app" / "build.gradle.kts", project=repo)
            self.assertBlocked(worktree / "spikes" / "x.kt", project=repo)
            self.assertBlocked(Path(tmp).resolve() / "elsewhere" / "x.kt", project=repo)

    def test_blocks_paths_outside_the_repository(self):
        self.assertBlocked(Path.home() / "criterion-data" / "qul" / "2026-10-20" / "notes.md")
        self.assertBlocked("/etc/hosts")
        self.assertBlocked(REPO / "app" / ".." / ".." / "outside.txt")

    def test_ignores_git_environment_overrides(self):
        target = Path.home() / "criterion-data" / "x"
        self.assertBlocked(target, extra_env={"GIT_DIR": str(REPO / ".git"), "GIT_WORK_TREE": "/"})

    def test_allows_ordinary_repo_paths(self):
        for rel in ["app/build.gradle.kts", "docs/plan/README.md", "CLAUDE.md", "core/content/schema.json"]:
            with self.subTest(rel=rel):
                self.assertAllowed(REPO / rel)

    def test_resolves_relative_paths_against_cwd(self):
        self.assertAllowed("app/src/main/AndroidManifest.xml")
        self.assertBlocked("spikes/toolchain-spike/README.md")

    def test_allows_only_the_agent_memory_and_session_scratchpad(self):
        self.assertAllowed("/tmp/claude-501/session/scratchpad/notes.md")
        self.assertAllowed(Path.home() / ".claude" / "projects" / "criterion" / "memory" / "note.md")
        self.assertBlocked("/tmp/criterion-scratch/notes.md")
        self.assertBlocked(Path.home() / ".claude" / "settings.json")
        self.assertBlocked(Path.home() / ".claude" / "CLAUDE.md")

    def test_warns_on_curated_data(self):
        result = edit(REPO / "data" / "curated" / "para.csv")
        self.assertEqual(result.returncode, 0, result.stderr)
        output = json.loads(result.stdout)
        self.assertIn("content-review", output["systemMessage"])
        self.assertIn("content-review", output["hookSpecificOutput"]["additionalContext"])

    def test_write_and_multiedit_use_the_same_rule(self):
        for tool in ["Write", "MultiEdit"]:
            with self.subTest(tool=tool):
                self.assertEqual(edit(REPO / "spikes" / "x.kt", tool=tool).returncode, 2)

    def test_input_without_file_path_passes(self):
        self.assertEqual(run_hook({"tool_name": "Edit", "tool_input": {}}).returncode, 0)

    def test_unparseable_input_fails_closed(self):
        self.assertEqual(run_hook("not json").returncode, 2)


if __name__ == "__main__":
    unittest.main()
