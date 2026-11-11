"""Print Semgrep findings to the log and the job summary, so they can be read without Code scanning access.

Usage: python3 semgrep-summary.py semgrep.json
"""

import json
import os
import sys

SEVERITY_ORDER = {"ERROR": 0, "WARNING": 1, "INFO": 2}


def cell(text: str) -> str:
    """Keeps a value on one Markdown table row."""
    return " ".join(str(text).split()).replace("|", "\\|")


def main() -> None:
    with open(sys.argv[1], encoding="utf-8") as file:
        report = json.load(file)

    findings = sorted(
        report.get("results", []),
        key=lambda f: (SEVERITY_ORDER.get(f["extra"].get("severity"), 3), f["path"], f["start"]["line"]),
    )
    errors = report.get("errors", [])

    lines = [f"## Semgrep: {len(findings)} finding(s)", ""]
    if findings:
        lines += ["| Severity | Location | Rule | Message |", "| --- | --- | --- | --- |"]
        for finding in findings:
            extra = finding["extra"]
            location = f"{finding['path']}:{finding['start']['line']}"
            lines.append(
                f"| {extra.get('severity', '')} | `{location}` | `{finding['check_id']}` | {cell(extra.get('message', ''))} |"
            )
            # One line per finding in the log, e.g. for agents reading the job log
            print(f"{extra.get('severity', '')} {location} {finding['check_id']}: {cell(extra.get('message', ''))}")
    else:
        print("No Semgrep findings")
    if errors:
        lines += ["", f"{len(errors)} scan error(s), usually files that could not be fully parsed:", ""]
        print(f"{len(errors)} scan error(s):")
        for error in errors:
            error_type = error.get("type")
            if isinstance(error_type, list):  # e.g. ["PartialParsing", [locations]]
                error_type = error_type[0]
            text = f"{error.get('level', '')} {error_type} {error.get('path', '')}: {cell(error.get('message', ''))[:300]}"
            lines.append(f"- {text}")
            print(text)

    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary_path:
        with open(summary_path, "a", encoding="utf-8") as summary:
            summary.write("\n".join(lines) + "\n")


if __name__ == "__main__":
    main()
