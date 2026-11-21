#!/usr/bin/env bash
# End-to-end test of the CLI against a real backend with the relays of fixture.sql.
# Usage: run.sh <tormap binary> <API URL>
set -uo pipefail

bin=$1
api=$2
cache=$(mktemp -d)
failures=0

tormap() {
    "$bin" --api-url "$api" --cache-dir "$cache" "$@"
}

# check <description> <expected exit code> <expected output (grep -E pattern, empty: any)> <command...>
check() {
    local description=$1 code=$2 pattern=$3
    shift 3
    local output status
    output=$("$@" 2>&1)
    status=$?
    if [[ $status != "$code" ]] || { [[ -n $pattern ]] && ! grep -Eq -- "$pattern" <<< "$output"; }; then
        echo "FAIL: $description (exit code $status, expected $code; output must match: $pattern)"
        echo "$output" | head -20
        failures=$((failures + 1))
    else
        echo "ok: $description"
    fi
}

check "prints its version" 0 '^tormap version [0-9]' tormap --version
check "lists the days" 0 '"2024-04-10"' tormap days --format json
check "lists the relays of a day as CSV" 0 '^AAAA.*,e2eAlpha,DE,' tormap relays --day 2024-03-05 --format csv
check "filters relays by country and flag" 0 '"nickname": ?"e2eAlpha"' tormap relays --day 2024-03-05 --country DE --flag Exit --format json
check "no matching relays is exit code 3" 3 '' tormap relays --day 2024-03-05 --country FR
check "shows a relay by nickname" 0 '"contact": ?"alpha@example.org"' tormap relay e2eAlpha --day 2024-03-05 --format json
check "shows a relay by fingerprint for a month" 0 '"platform": ?"Tor 0.4.7.16 on Linux"' \
    tormap relay 'AAAA AAAA AAAA AAAA AAAA AAAA AAAA AAAA AAAA AAAA' --month 2024-02 --format json
check "lists the months of a relay" 0 '^2024-02,2024-02-20,e2eAlphaOld,127.0.0.1' tormap relay e2eAlpha --history --format csv
check "the history has every month" 0 '2024-04' tormap relay "$(printf 'A%.0s' {1..40})" --history --format csv
check "an unknown fingerprint is exit code 3" 3 '' tormap relay "$(printf 'D%.0s' {1..40})"
check "a relay with the required flags is exit code 0" 0 '' tormap -q relay e2eBravo --day 2024-03-05 --require-flag Running,Guard
check "a missing flag is exit code 5" 5 'lacks Exit' tormap relay e2eBravo --day 2024-03-05 --require-flag Exit
check "a relay missing from the consensus is exit code 3" 3 'not in the consensus on 2024-04-10' \
    tormap relay "$(printf 'B%.0s' {1..40})" --month 2024-03 --require-flag Running
check "lists a family" 0 'e2eBravo' tormap family e2eAlpha --month 2024-03 --format csv
check "lists the autonomous systems of a month" 0 '24940' tormap as --month 2024-03 --format csv
check "counts the relays per day" 0 '2024-03-05,3,1,1,1' tormap stats --from 2024-03-04 --to 2024-03-05 --format csv
check "shows Tor users of a day" 0 '^DE,250000' tormap usage --day 2024-03-05 --format csv
check "falls back to the latest day with estimates" 0 'Estimates for 2024-04-08' tormap usage --format csv
check "a malformed day is exit code 2" 2 '' tormap relays --day 2024-3-5
check "an unreachable proxy is exit code 1" 1 'proxy' tormap --no-cache --proxy socks5h://127.0.0.1:1 days

# Conditional requests: an expired day is revalidated with its ETag and kept
day_url="$api/v1/days/2024-03-05/relays"
etag=$(curl -sf -D - -o /dev/null "$day_url" | tr -d '\r' | sed -n 's/^[Ee][Tt][Aa][Gg]: //p')
check "the server sends an ETag for a day" 0 '^W/"' echo "$etag"
check "the server answers a known ETag with 304" 0 '^304$' curl -s -o /dev/null -w '%{http_code}' -H "If-None-Match: $etag" "$day_url"
# Its own cache, so only the day is cached there
cache=$(mktemp -d)
tormap relays --day 2024-03-05 --format csv > /dev/null
etag_files=("$cache"/*.etag)
check "the CLI keeps the ETag of a cached day" 0 "$etag" cat "${etag_files[@]}"
find "$cache" -name '*.json.gz' -exec touch -d '40 days ago' {} +
check "an expired day is revalidated" 0 'e2eCharlie' tormap relays --day 2024-03-05 --format csv
check "a revalidated day is fresh again" 0 '^$' find "$cache" -name '*.json.gz' -mtime +1

if [[ $failures -gt 0 ]]; then
    echo "$failures checks failed"
    exit 1
fi
echo "All checks passed"
