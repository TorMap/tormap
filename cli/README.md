# TorMap CLI

`tormap` looks up Tor relays and exports TorMap data from the command line. It reads the public API v1 of
[api.tormap.org](https://api.tormap.org) (or your own backend) and filters a day's relays locally, so filters do not
add load to the server.

## Install

Every [TorMap release](https://github.com/TorMap/tormap/releases) from 4.3.0 on has native binaries that need no
Java: `tormap-<version>-linux-x64`, `-linux-arm64`, `-macos-arm64` and `-windows-x64.exe`. Check the download against
`SHA256SUMS` from the same release, then put it on your `PATH`:

```sh
sha256sum --check --ignore-missing SHA256SUMS   # macOS: shasum -a 256 --check --ignore-missing SHA256SUMS
install -m 755 tormap-4.3.0-linux-x64 ~/.local/bin/tormap
tormap --help
```

The Linux binaries need glibc 2.35 or newer (Ubuntu 22.04, Debian 12 and later). The binaries are not signed: on
macOS, a binary downloaded with a browser has to be allowed once with `xattr -d com.apple.quarantine tormap`, and
Windows SmartScreen may ask for confirmation.

With Docker (`linux/amd64` and `linux/arm64`; `:edge` follows master):

```sh
docker run --rm tormap/cli relays --country DE --limit 10
# Keep the cache between runs
docker run --rm -v tormap-cache:/home/nonroot/.cache tormap/cli relays --flag Exit
```

### Build it yourself

With JDK 25, as a JVM application:

```sh
cd cli
./gradlew installDist
build/install/tormap/bin/tormap --help
```

As a native binary with [GraalVM](https://www.graalvm.org/) 25 (`build/native/tormap`):

```sh
GRAALVM_HOME=/path/to/graalvm ./gradlew nativeCompile
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
