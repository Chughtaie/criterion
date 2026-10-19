"""Tests for guard_paths.py. Run: python3 -I -m unittest discover -s tools/agent-hooks -v"""

import json
import os
import subprocess
import sys
import unittest
from pathlib import Path

HOOK = Path(__file__).resolve().parent / "guard_paths.py"
REPO = HOOK.parents[2]


def run_hook(payload):
    stdin = payload if isinstance(payload, str) else json.dumps(payload)
    env = dict(os.environ, CLAUDE_PROJECT_DIR=str(REPO))
    return subprocess.run(
        [sys.executable, "-I", str(HOOK)], input=stdin, capture_output=True, text=True, env=env, cwd=REPO
    )


def edit(file_path, tool="Edit"):
    return run_hook({"tool_name": tool, "cwd": str(REPO), "tool_input": {"file_path": str(file_path)}})


class GuardPathsTest(unittest.TestCase):
    def assertBlocked(self, file_path):
        result = edit(file_path)
        self.assertEqual(result.returncode, 2, f"{file_path} should be blocked: {result.stderr}")
        self.assertIn("guard_paths: blocked", result.stderr)

    def assertAllowed(self, file_path):
        result = edit(file_path)
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

    def test_blocks_paths_outside_the_repository(self):
        self.assertBlocked(Path.home() / "criterion-data" / "qul" / "2026-10-20" / "notes.md")
        self.assertBlocked("/etc/hosts")
        self.assertBlocked(REPO / "app" / ".." / ".." / "outside.txt")

    def test_allows_ordinary_repo_paths(self):
        for rel in ["app/build.gradle.kts", "docs/plan/README.md", "CLAUDE.md", "core/content/schema.json"]:
            with self.subTest(rel=rel):
                self.assertAllowed(REPO / rel)

    def test_resolves_relative_paths_against_cwd(self):
        self.assertAllowed("app/src/main/AndroidManifest.xml")
        self.assertBlocked("spikes/toolchain-spike/README.md")

    def test_allows_agent_scratch_and_memory_dirs(self):
        self.assertAllowed("/tmp/criterion-scratch/notes.md")
        self.assertAllowed(Path.home() / ".claude" / "projects" / "criterion" / "memory" / "note.md")

    def test_warns_on_curated_data(self):
        result = edit(REPO / "data" / "curated" / "para.csv")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("content-review", json.loads(result.stdout)["systemMessage"])

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
