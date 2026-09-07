"""Read-only validation of the initial, lossless SAN repository import."""
import json
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]


def git(*args):
    return subprocess.check_output(["git", "-C", str(ROOT), *args], text=True).strip()


def main():
    manifest = json.loads((ROOT / "integration/sources.json").read_text())
    failures = []
    for source in manifest["sources"]:
        directory, commit = source["directory"], source["commit"]
        ancestor = subprocess.run(
            ["git", "-C", str(ROOT), "merge-base", "--is-ancestor", commit, "HEAD"],
            check=False,
        ).returncode == 0
        same_tree = git("rev-parse", f"{commit}^{{tree}}") == git(
            "rev-parse", f"HEAD:{directory}"
        )
        count = int(git("rev-list", "--count", commit))
        unchanged = not git("status", "--porcelain", "--", directory)
        no_gitlinks = not any(
            line.startswith("160000 ")
            for line in git("ls-tree", "-r", "HEAD", "--", directory).splitlines()
        )
        result = {
            "directory": directory, "original_commit_is_ancestor": ancestor,
            "tree_identical": same_tree, "original_commit_count": count,
            "expected_count_matches": count == source["commit_count"],
            "working_tree_clean": unchanged, "no_submodules": no_gitlinks,
        }
        print(json.dumps(result))
        if not all((ancestor, same_tree, unchanged, no_gitlinks,
                    count == source["commit_count"])):
            failures.append(directory)
    forbidden = ["docs", "engine", "san-engine", ".github"]
    for directory in forbidden:
        if (ROOT / directory).exists():
            failures.append(f"unexpected root directory: {directory}")
    if failures:
        print("FAIL: " + ", ".join(failures), file=sys.stderr)
        return 1
    print("PASS: all three source trees and original main histories preserved")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
