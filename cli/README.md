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

Each binary has a signed build provenance attestation, which shows that it was built from this repository by the
release workflow. Verify it with the [GitHub CLI](https://cli.github.com/):

```sh
gh attestation verify tormap-4.3.0-linux-x64 --repo TorMap/tormap
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
tormap relay MyRelay --history   # every month it was seen, with nickname, address, AS, platform and contact then

# Family members of a relay, autonomous systems, relay counts and Tor users per country
tormap family MyRelay
tormap as --name hetzner
tormap stats --from 2024-01-01 --to 2024-01-31
tormap usage --country DE,NL   # latest day with estimates, Tor Metrics publishes them 2 to 3 days later

# Everything over Tor
tormap --proxy socks5h://127.0.0.1:9050 relays --nickname 'tor*'

# Monitoring from cron: exit code 3 if the relay is not in today's consensus, 5 if it lacks a flag
tormap --quiet relay MyRelay --require-flag Running,Guard || echo "MyRelay needs attention"
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
| `--quiet` (`-q`): no results or hints, only errors and the exit code | | |

Downloaded days are cached as gzip files: recent days for an hour, older days for 30 days. After that, the CLI asks the
server whether the day changed (`If-None-Match`) and only downloads it again if it did.

### Shell completion

```sh
tormap completion bash > ~/.local/share/bash-completion/completions/tormap
tormap completion zsh > "${fpath[1]}/_tormap"
tormap completion fish > ~/.config/fish/completions/tormap.fish
```

## Exit codes

| Code | Meaning |
| --- | --- |
| 0 | Success |
| 1 | Error, e.g. the server is unreachable |
| 2 | Wrong usage, e.g. a malformed day |
| 3 | Nothing found |
| 4 | More than one relay has the nickname; the fingerprints are printed |
| 5 | `relay --require-flag`: the relay lacks one of the flags on that day |

## Privacy

The CLI sends no telemetry and never checks for updates. It only connects to the API URL (default
`api.tormap.org`), with the header `User-Agent: tormap-cli/<version>`, and keeps downloaded data in its cache
directory.

Without a proxy, the API server sees your IP address and which relays you look up. To hide both, run the CLI
through Tor with `--proxy socks5h://127.0.0.1:9050` (Tor Browser listens on port 9150), or set `TORMAP_PROXY`.
The CLI always lets the proxy resolve the API host name, so no DNS request leaves outside Tor. In Docker, the Tor proxy must be reachable from the container, e.g.
`--network host`. The cache is not encrypted; delete it, or use `--no-cache`, if others can read your files.

## Data and licences

The data comes from the [Tor Project](https://metrics.torproject.org/) (CollecTor descriptors and Tor Metrics user
estimates). Countries, coordinates and autonomous systems are looked up in the free databases of
[DB-IP](https://db-ip.com), licensed under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). If you publish
data exported with the CLI, credit DB-IP for the location and AS fields.


## Development

```sh
./gradlew test
```

The tests run the commands against a local fake API. `ContractTest` checks every field and path the CLI uses
against `api/openapi-v1.json`, which the backend keeps in sync with the server. The CLI shares the TorMap version
in `build.gradle.kts`.
