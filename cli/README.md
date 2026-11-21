# TorMap CLI

`tormap` looks up Tor relays and exports TorMap data from the command line. It reads the public API v1 of
[api.tormap.org](https://api.tormap.org) (or your own backend) and filters a day's relays locally, so filters do not
add load to the server.

## Install

Native binaries and the `tormap/cli` Docker image will be attached to TorMap releases. Until then, build it with
JDK 25:

```sh
cd cli
./gradlew installDist
build/install/tormap/bin/tormap --help
```

## Examples

```sh
# Exit relays in Germany on the latest day, largest first
tormap relays --country DE --flag Exit --limit 20

# All relays of a day as CSV, e.g. for a spreadsheet or pandas
tormap relays --day 2024-05-01 --format csv --output relays-2024-05-01.csv

# Details of a relay, by fingerprint (spaces and a leading $ are fine) or by nickname
tormap relay '$A1B2 C3D4 ...'
tormap relay MyRelay --month 2024-05

# Family members of a relay, autonomous systems, relay counts and Tor users per country
tormap family MyRelay
tormap as --name hetzner
tormap stats --from 2024-01-01 --to 2024-01-31
tormap usage --country DE,NL   # latest day with estimates, Tor Metrics publishes them 2 to 3 days later

# Everything over Tor
tormap --proxy socks5h://127.0.0.1:9050 relays --nickname 'tor*'
```

Options such as `--format` can be given before or after the command. The output is a table in a terminal and JSON
when piped. JSON and NDJSON contain all fields of the API, named as in [api/openapi-v1.json](../api/openapi-v1.json).

| Option | Environment variable | Default |
| --- | --- | --- |
| `--api-url` | `TORMAP_API_URL` | `https://api.tormap.org` |
| `--proxy` (`socks5h://`, `http://`) | `TORMAP_PROXY` | none |
| `--format` (`table`, `json`, `ndjson`, `csv`) | | table in a terminal, otherwise json |
| `--cache-dir` | `TORMAP_CACHE_DIR` | `$XDG_CACHE_HOME/tormap`, `%LOCALAPPDATA%\tormap\cache` or `~/.cache/tormap` |
| `--no-cache`, `--timeout`, `--output` | | |

Downloaded days are cached as gzip files: recent days for an hour, older days for 30 days.

## Exit codes

| Code | Meaning |
| --- | --- |
| 0 | Success |
| 1 | Error, e.g. the server is unreachable |
| 2 | Wrong usage, e.g. a malformed day |
| 3 | Nothing found |
| 4 | More than one relay has the nickname; the fingerprints are printed |

## Development

```sh
./gradlew test
```

The tests run the commands against a local fake API. `ContractTest` checks every field and path the CLI uses
against `api/openapi-v1.json`, which the backend keeps in sync with the server. The CLI shares the TorMap version
in `build.gradle.kts`.
