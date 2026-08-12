#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

SKIP_BUILD=false
if [[ "${1:-}" == "--skip-build" ]]; then
  SKIP_BUILD=true
fi

find_browser() {
  if command -v chromium-browser >/dev/null 2>&1; then
    command -v chromium-browser
    return
  fi
  if command -v chromium >/dev/null 2>&1; then
    command -v chromium
    return
  fi
  if command -v google-chrome >/dev/null 2>&1; then
    command -v google-chrome
    return
  fi
  if command -v google-chrome-stable >/dev/null 2>&1; then
    command -v google-chrome-stable
    return
  fi
  echo "No Chromium-based browser binary found for smoke test." >&2
  exit 1
}

BROWSER_BIN="$(find_browser)"

wait_for_server() {
  local url="$1"
  for _ in {1..60}; do
    if curl -fs "$url" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  echo "Server did not become ready at $url" >&2
  return 1
}

assert_app_rendered() {
  local url="$1"
  local mode="$2"
  local dom_file
  local chromium_log
  dom_file="$(mktemp)"
  chromium_log="$(mktemp)"
  "$BROWSER_BIN" --headless --disable-gpu --no-sandbox --virtual-time-budget=15000 --dump-dom "$url" >"$dom_file" 2>"$chromium_log"

  if grep -q "ReferenceError: L is not defined" "$chromium_log"; then
    echo "$mode smoke test failed: detected 'L is not defined' runtime error." >&2
    cat "$chromium_log" >&2
    rm -f "$dom_file" "$chromium_log"
    return 1
  fi

  if grep -q '<div id="root"></div>' "$dom_file"; then
    echo "$mode smoke test failed: React root stayed empty." >&2
    cat "$dom_file" >&2
    rm -f "$dom_file" "$chromium_log"
    return 1
  fi

  if ! grep -q 'leaflet-container' "$dom_file"; then
    echo "$mode smoke test failed: Leaflet container not rendered." >&2
    cat "$dom_file" >&2
    rm -f "$dom_file" "$chromium_log"
    return 1
  fi

  rm -f "$dom_file" "$chromium_log"
}

cleanup() {
  if [[ -n "${DEV_PID:-}" ]]; then
    kill "$DEV_PID" >/dev/null 2>&1 || true
    wait "$DEV_PID" 2>/dev/null || true
  fi
  if [[ -n "${PREVIEW_PID:-}" ]]; then
    kill "$PREVIEW_PID" >/dev/null 2>&1 || true
    wait "$PREVIEW_PID" 2>/dev/null || true
  fi
}

trap cleanup EXIT

yarn start --host 127.0.0.1 --port 4173 >/tmp/tormap-dev-server.log 2>&1 &
DEV_PID=$!
wait_for_server "http://127.0.0.1:4173"
assert_app_rendered "http://127.0.0.1:4173" "dev"
kill "$DEV_PID" >/dev/null 2>&1 || true
wait "$DEV_PID" 2>/dev/null || true
unset DEV_PID

if [[ "$SKIP_BUILD" != true ]]; then
  yarn build
fi

yarn preview --host 127.0.0.1 --port 4174 >/tmp/tormap-preview-server.log 2>&1 &
PREVIEW_PID=$!
wait_for_server "http://127.0.0.1:4174"
assert_app_rendered "http://127.0.0.1:4174" "prod"
