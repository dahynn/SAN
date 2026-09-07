"""Read-only, limited secret-pattern scan; reports metadata, never matching values.

This is not a comprehensive secret detector or a credential validity check.
Scans all unique reachable text blobs from the imported main histories.
"""
import json
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[1]
PATTERNS = {
    "private_key": re.compile(rb"-----BEGIN (?:RSA |EC |DSA |OPENSSH )?PRIVATE KEY-----"),
    "github_token": re.compile(rb"\b(?:gh[pousr]_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{50,})\b"),
    "aws_access_key_id": re.compile(rb"\b(?:AKIA|ASIA)[A-Z0-9]{16}\b"),
    "openai_style_key": re.compile(rb"\bsk-(?:proj-|svcacct-)?[A-Za-z0-9_-]{32,}\b"),
    "slack_token": re.compile(rb"\bxox[baprs]-[A-Za-z0-9-]{20,}\b"),
    "google_api_key": re.compile(rb"\bAIza[A-Za-z0-9_-]{35}\b"),
}


def main():
    sources = json.loads((ROOT / "integration/sources.json").read_text())["sources"]
    commits = [source["commit"] for source in sources]
    listing = subprocess.check_output(
        ["git", "-C", str(ROOT), "rev-list", "--objects", *commits], text=True
    ).splitlines()
    paths = {}
    for line in listing:
        sha, _, path = line.partition(" ")
        paths.setdefault(sha, path)
    proc = subprocess.Popen(
        ["git", "-C", str(ROOT), "cat-file", "--batch"],
        stdin=subprocess.PIPE, stdout=subprocess.PIPE,
    )
    findings = []
    scanned = binary = large = 0
    for sha, path in paths.items():
        proc.stdin.write((sha + "\n").encode())
        proc.stdin.flush()
        metadata = proc.stdout.readline().decode().split()
        if len(metadata) != 3:
            raise RuntimeError("Unexpected git object response")
        kind, size = metadata[1], int(metadata[2])
        data = proc.stdout.read(size)
        proc.stdout.read(1)
        if kind != "blob":
            continue
        if size > 2 * 1024 * 1024:
            large += 1
            continue
        if b"\0" in data:
            binary += 1
            continue
        scanned += 1
        for rule, pattern in PATTERNS.items():
            for match in pattern.finditer(data):
                findings.append({"rule": rule, "object": sha, "path": path,
                                 "line": data.count(b"\n", 0, match.start()) + 1})
    proc.stdin.close()
    if proc.wait() != 0:
        raise RuntimeError("git cat-file failed")
    print(json.dumps({"text_blobs_scanned": scanned, "binary_blobs_skipped": binary,
                      "large_blobs_skipped": large, "findings": findings}, indent=2))
    return 1 if findings else 0


if __name__ == "__main__":
    raise SystemExit(main())
