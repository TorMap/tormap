#!/usr/bin/env python3
"""Plan the next TorMap release for the Release workflow (.github/workflows/release.yml).

Usage: release-plan.py   (from the repository root, with full git history and tags)

Reads the latest vX.Y.Z tag reachable from HEAD and the pull requests merged since then, picks the next version from
their labels and checks that every CI check on HEAD passed. Writes a summary (to $GITHUB_STEP_SUMMARY, else stdout)
and the outputs version, tag, previous_tag and sha (to $GITHUB_OUTPUT when set).

Environment:
  BUMP               auto (default), patch, minor or major. auto: breaking -> major, enhancement or deployment -> minor,
                     anything else -> patch, matching the categories in .github/release.yml.
  GITHUB_REPOSITORY  owner/name, e.g. TorMap/tormap
  GITHUB_RUN_ID      the current workflow run, whose own checks are ignored (optional)
  GH_TOKEN           for the gh CLI, read only

Exit codes: 0 = ready to release, 1 = something blocks the release (the summary says what).
Pull request titles and labels are untrusted text: they only go into the Markdown summary, never into a command.
"""
import json
import os
import re
import subprocess
import sys

# Same order and grouping as .github/release.yml
CATEGORIES = [
    ("Breaking and deployment changes", {"breaking", "deployment"}),
    ("Security", {"security"}),
    ("Features", {"enhancement"}),
    ("Bug fixes", {"bug"}),
    ("Documentation", {"documentation"}),
]
CATEGORY_LABELS = set().union(*(labels for _, labels in CATEGORIES))
OK_CONCLUSIONS = {"success", "neutral", "skipped"}
TAG_PATTERN = re.compile(r"^v(\d+)\.(\d+)\.(\d+)$")


def run(*args):
    return subprocess.run(args, check=True, capture_output=True, text=True).stdout.strip()


def gh_api(path):
    return json.loads(run("gh", "api", "-H", "Accept: application/vnd.github+json", path))


def gh_api_pages(path, key):
    items = []
    page = 1
    while True:
        data = gh_api(f"{path}{'&' if '?' in path else '?'}per_page=100&page={page}")
        items += data[key]
        if len(data[key]) < 100:
            return items
        page += 1


def is_renovate(pr):
    return pr["author"].startswith("renovate") or "renovate" in pr["labels"]


def next_version(previous, bump):
    major, minor, patch = previous
    if bump == "major":
        return major + 1, 0, 0
    if bump == "minor":
        return major, minor + 1, 0
    return major, minor, patch + 1


def md(text):
    """Untrusted text inside a Markdown table cell."""
    return re.sub(r"[\r\n]+", " ", text).replace("|", "\\|").replace("<", "&lt;")


def main():
    repo = os.environ["GITHUB_REPOSITORY"]
    bump_input = os.environ.get("BUMP", "auto")
    if bump_input not in {"auto", "patch", "minor", "major"}:
        sys.exit(f"Unknown BUMP {bump_input!r}")
    errors, warnings, lines = [], [], []

    sha = run("git", "rev-parse", "HEAD")
    previous_tag = run("git", "describe", "--tags", "--abbrev=0", "--match", "v[0-9]*.[0-9]*.[0-9]*", "HEAD")
    match = TAG_PATTERN.match(previous_tag)
    if not match:
        sys.exit(f"Latest tag {previous_tag!r} is not vX.Y.Z")
    previous = tuple(int(part) for part in match.groups())

    # Pull requests merged since the previous release, found through their commits
    commits = run("git", "rev-list", f"{previous_tag}..HEAD").split()
    prs, direct_commits = {}, []
    for commit in commits:
        merged = [
            pr for pr in gh_api(f"repos/{repo}/commits/{commit}/pulls")
            if pr.get("merged_at") and pr["base"]["ref"] == "master"
        ]
        if not merged:
            direct_commits.append(commit)
        for pr in merged:
            prs[pr["number"]] = {
                "number": pr["number"],
                "title": pr["title"],
                "author": pr["user"]["login"],
                "labels": [label["name"] for label in pr["labels"]],
            }
    if not commits:
        errors.append(f"Nothing was committed since {previous_tag}.")
    listed = [pr for pr in sorted(prs.values(), key=lambda pr: pr["number"]) if not is_renovate(pr)]
    renovate_count = len(prs) - len(listed)

    labels = {label for pr in listed for label in pr["labels"]}
    if "breaking" in labels:
        auto_bump, reason = "major", "a PR is labeled breaking"
    elif labels & {"enhancement", "deployment"}:
        auto_bump, reason = "minor", "a PR is labeled enhancement or deployment"
    else:
        auto_bump, reason = "patch", "no PR is labeled breaking, enhancement or deployment"
    bump = auto_bump if bump_input == "auto" else bump_input
    if bump_input != "auto" and bump_input != auto_bump:
        warnings.append(f"The labels suggest a {auto_bump} release ({reason}), but {bump_input} was chosen.")
    version = "%d.%d.%d" % next_version(previous, bump)
    tag = f"v{version}"
    if run("git", "tag", "--list", tag):
        errors.append(f"Tag {tag} already exists.")

    unlabeled = [pr for pr in listed if not CATEGORY_LABELS & set(pr["labels"])]
    if unlabeled:
        warnings.append(
            "These PRs have no category label and will be listed under Other changes: "
            + ", ".join(f"#{pr['number']}" for pr in unlabeled)
        )
    if direct_commits:
        warnings.append(
            "Commits without a merged PR (not in the generated notes): "
            + ", ".join(commit[:7] for commit in direct_commits)
        )

    # Every check on HEAD must have passed, except the ones of this workflow run
    own_suite = None
    if os.environ.get("GITHUB_RUN_ID"):
        own_suite = gh_api(f"repos/{repo}/actions/runs/{os.environ['GITHUB_RUN_ID']}")["check_suite_id"]
    checks = [
        check for check in gh_api_pages(f"repos/{repo}/commits/{sha}/check-runs", "check_runs")
        if check["check_suite"]["id"] != own_suite
    ]
    pending = sorted({check["name"] for check in checks if check["status"] != "completed"})
    failed = sorted({
        check["name"] for check in checks
        if check["status"] == "completed" and check["conclusion"] not in OK_CONCLUSIONS
    })
    statuses = gh_api(f"repos/{repo}/commits/{sha}/status")["statuses"]
    failed += sorted(status["context"] for status in statuses if status["state"] in {"failure", "error"})
    pending += sorted(status["context"] for status in statuses if status["state"] == "pending")
    if not checks:
        errors.append(f"No CI checks ran on {sha[:7]}.")
    if pending:
        errors.append("CI is still running on master: " + ", ".join(pending) + ". Run the workflow again when it finished.")
    if failed:
        errors.append("CI failed on master: " + ", ".join(failed) + ".")

    lines.append(f"## Release {tag}")
    lines.append("")
    lines.append(f"**{previous_tag} → {tag}** ({bump}; {'labels: ' + reason if bump_input == 'auto' else 'chosen by hand'})"
                 f" from master at `{sha[:7]}`.")
    lines.append(f"CI on `{sha[:7]}`: {len(checks)} checks, {'all passed' if not failed and not pending and checks else 'not ready'}.")
    lines.append("")
    for title, category_labels in CATEGORIES + [("Other changes", None)]:
        if category_labels is None:
            members = unlabeled
        else:
            members = [pr for pr in listed if category_labels & set(pr["labels"])]
            # Like release.yml, a PR goes into the first category that matches
            members = [pr for pr in members if not any(
                set(pr["labels"]) & earlier for _, earlier in CATEGORIES[:[c for c, _ in CATEGORIES].index(title)]
            )]
        if not members:
            continue
        lines.append(f"### {title}")
        lines.append("")
        lines.append("| PR | Title | Labels |")
        lines.append("| --- | --- | --- |")
        for pr in members:
            lines.append(f"| #{pr['number']} | {md(pr['title'])} | {md(', '.join(pr['labels']))} |")
        lines.append("")
    lines.append(f"{renovate_count} Renovate PRs are left out of the notes, like in the generated release notes.")
    lines.append("")
    for warning in warnings:
        lines.append(f"> [!WARNING]\n> {md(warning)}\n")
    for error in errors:
        lines.append(f"> [!CAUTION]\n> {md(error)}\n")

    summary = "\n".join(lines) + "\n"
    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary_path:
        with open(summary_path, "a", encoding="utf-8") as file:
            file.write(summary)
    print(summary)
    for message in warnings:
        print(f"::warning::{message}")
    for message in errors:
        print(f"::error::{message}")

    output_path = os.environ.get("GITHUB_OUTPUT")
    if output_path:
        with open(output_path, "a", encoding="utf-8") as file:
            file.write(f"version={version}\ntag={tag}\nprevious_tag={previous_tag}\nsha={sha}\n")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
