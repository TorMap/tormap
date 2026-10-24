#!/usr/bin/env bash
# Points git at the versioned hooks in .githooks/ (secret scan on commit).
set -euo pipefail

cd "$(git rev-parse --show-toplevel)"
git config core.hooksPath .githooks
chmod +x .githooks/pre-commit
echo "Git hooks enabled (core.hooksPath=.githooks)."

if ! command -v betterleaks >/dev/null 2>&1; then
  echo "Note: betterleaks is not installed yet; commits will warn and skip the scan until it is." >&2
  echo "      Install it (macOS: brew install betterleaks), see https://github.com/betterleaks/betterleaks" >&2
fi
