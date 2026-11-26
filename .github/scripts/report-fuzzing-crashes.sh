#!/usr/bin/env bash
# Reports the crashes of a ClusterFuzzLite run (.github/workflows/fuzzing.yml) in GitHub issues labeled "fuzzing":
# one open issue per fuzz target, later crashes of the same target are added as comments.
# The stack trace and the input (base64) are in the issue, so a crash can be reproduced without the workflow artifact.
# Usage: report-fuzzing-crashes.sh <directory with the run's out/artifacts>
# Needs GH_TOKEN, GH_REPO, RUN_URL and COMMIT.
set -euo pipefail

crash_dir=${1:?crash directory}
max_input_bytes=8192
max_trace_lines=150

gh label create fuzzing --description "Found by scheduled fuzzing" --color B60205 2>/dev/null || true

report() {
  local title=$1 body_file=$2 number
  number=$(gh issue list --label fuzzing --state open --search "\"$title\" in:title" --json number,title \
    --jq "map(select(.title == \"$title\")) | first | .number // empty")
  if [ -n "$number" ]; then
    gh issue comment "$number" --body-file "$body_file"
  else
    gh issue create --title "$title" --label fuzzing --body-file "$body_file"
  fi
}

body=$(mktemp)
found=0
while IFS= read -r -d '' summary; do
  found=1
  testcase=${summary%.summary}
  [ -f "$testcase-minimized" ] && testcase="$testcase-minimized"
  target=$(basename "$(dirname "$(dirname "$summary")")")
  {
    echo "The scheduled fuzzing run $RUN_URL found a crash in \`$target\` on commit $COMMIT."
    echo
    echo '<details><summary>Stack trace</summary>'
    echo
    echo '```'
    head -n "$max_trace_lines" "$summary"
    echo '```'
    echo '</details>'
    echo
    if [ -f "$testcase" ] && [ "$(stat -c %s "$testcase")" -le "$max_input_bytes" ]; then
      echo "Input ($(stat -c %s "$testcase") bytes, base64):"
      echo
      echo '```'
      base64 -w 76 "$testcase"
      echo '```'
    else
      echo "The input is too large for this issue, download \`$(basename "$testcase")\` from the run's \`fuzzing-crashes\` artifact."
    fi
    echo
    echo 'Reproduce: see "Reproducing a crash" in .clusterfuzzlite/README.md.'
  } > "$body"
  report "Fuzzing: $target crashed" "$body"
done < <(find "$crash_dir" -name '*.summary' -print0 2>/dev/null)

if [ "$found" -eq 0 ]; then
  echo "The scheduled fuzzing run $RUN_URL failed on commit $COMMIT without a crash, e.g. because the fuzz targets don't build." > "$body"
  report "Fuzzing: run failed" "$body"
fi
