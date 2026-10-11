#!/usr/bin/env python3
"""Check dependencies added or changed by a pull request against the OpenSourceMalware API.

Usage: osm-check.py {npm|maven} FILE [FILE...]

Compares each FILE at HEAD^1 (the base of the PR merge commit) with the working tree, so it must run on a
`pull_request` checkout with `fetch-depth: 2`. Only coordinates that are new or changed are looked up. If none of
the files changed, nothing is requested and the script exits successfully.

Exit codes: 0 = nothing flagged (or check skipped), 1 = a package is reported malicious.
The check is skipped with a warning when OSM_TOKEN is unset (e.g. PRs from forks) or the API is unreachable.
The token is only read from the environment and never printed.
"""
import json
import os
import re
import subprocess
import sys
import urllib.error
import urllib.parse
import urllib.request
from concurrent.futures import ThreadPoolExecutor

API = "https://api.opensourcemalware.com/functions/v1/check-malicious"


def read_base(path):
    try:
        return subprocess.run(
            ["git", "show", f"HEAD^1:{path}"], check=True, capture_output=True, text=True
        ).stdout
    except subprocess.CalledProcessError:
        return ""  # file did not exist in the base


def read_head(path):
    try:
        with open(path, encoding="utf-8") as f:
            return f.read()
    except FileNotFoundError:
        return ""


def npm_packages(lock):
    """Resolved (name, version) pairs from a Yarn berry lockfile."""
    found = set()
    for m in re.finditer(r'^\s+resolution: "(.+)"$', lock, re.MULTILINE):
        res = m.group(1)
        if "@npm:" not in res:
            continue  # workspace, patch:, git etc.
        name, version = res.rsplit("@npm:", 1)
        if "@" in version:
            continue  # alias to another package, resolved under its own entry
        found.add((name, version))
    return found


GRADLE_DEP = re.compile(r'"([A-Za-z0-9_.\-]+):([A-Za-z0-9_.\-]+):([A-Za-z0-9_.\-+]+)(?::[A-Za-z0-9_.\-]+)?"')
GRADLE_PLUGIN_ID = re.compile(r'id\("([A-Za-z0-9_.\-]+)"\)\s*version\s*"([A-Za-z0-9_.\-+]+)"')
GRADLE_PLUGIN_KOTLIN = re.compile(r'kotlin\("([A-Za-z0-9_.\-]+)"\)\s*version\s*"([A-Za-z0-9_.\-+]+)"')


def maven_packages(text):
    """Coordinates with an inline version from Gradle Kotlin DSL files (versionless BOM-managed ones are skipped)."""
    found = set()
    for g, a, v in GRADLE_DEP.findall(text):
        found.add((f"{g}:{a}", v))
    # Gradle plugins resolve through their plugin marker artifact
    for pid, v in GRADLE_PLUGIN_ID.findall(text):
        found.add((f"{pid}:{pid}.gradle.plugin", v))
    for k, v in GRADLE_PLUGIN_KOTLIN.findall(text):
        pid = f"org.jetbrains.kotlin.{k}"
        found.add((f"{pid}:{pid}.gradle.plugin", v))
    return found


def query(ecosystem, token, name, version):
    params = urllib.parse.urlencode(
        {"report_type": "package", "ecosystem": ecosystem, "resource_identifier": name, "version": version}
    )
    req = urllib.request.Request(f"{API}?{params}", headers={"Authorization": f"Bearer {token}"})
    last = None
    for _ in range(2):
        try:
            with urllib.request.urlopen(req, timeout=20) as resp:
                return name, version, json.load(resp), None
        except (urllib.error.URLError, TimeoutError, json.JSONDecodeError) as e:
            last = type(e).__name__
            if isinstance(e, urllib.error.HTTPError):
                last = f"HTTP {e.code}"
    return name, version, None, last


def main():
    if len(sys.argv) < 3 or sys.argv[1] not in ("npm", "maven"):
        print(__doc__)
        return 2
    ecosystem, files = sys.argv[1], sys.argv[2:]
    extract = npm_packages if ecosystem == "npm" else maven_packages

    base, head = set(), set()
    for path in files:
        base |= extract(read_base(path))
        head |= extract(read_head(path))
    changed = sorted(head - base)
    if not changed:
        print(f"No new or changed {ecosystem} dependencies in {', '.join(files)}, nothing to check.")
        return 0

    token = os.environ.get("OSM_TOKEN", "")
    if not token:
        print("::warning::OSM_TOKEN is not available (e.g. PR from a fork), skipping the OpenSourceMalware check.")
        return 0

    print(f"Checking {len(changed)} new or changed {ecosystem} dependencies against OpenSourceMalware...")
    flagged, failed = [], []
    with ThreadPoolExecutor(max_workers=8) as pool:
        for name, version, body, err in pool.map(lambda p: query(ecosystem, token, *p), changed):
            if err or not isinstance(body, dict):
                failed.append((name, version, err or "unexpected response"))
            elif body.get("malicious") is True:
                flagged.append((name, version))

    for name, version in flagged:
        print(f"::error::{name}@{version} is reported as malicious by OpenSourceMalware.")
    for name, version, err in failed:
        print(f"::warning::Could not check {name}@{version}: {err}")
    if failed and len(failed) == len(changed):
        print("::warning::OpenSourceMalware API unreachable or rejected the token, no dependency was verified.")
    if flagged:
        return 1
    print(f"OK: {len(changed) - len(failed)} dependencies checked, none reported as malicious.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
