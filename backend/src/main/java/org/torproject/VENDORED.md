# Vendored Tor metrics-lib

`org.torproject.descriptor` is a vendored copy of the Tor Project's
[metrics-lib](https://gitlab.torproject.org/tpo/network-health/metrics/library). It is not published to Maven Central,
so Renovate cannot update it. Check https://dist.torproject.org/metrics-lib/ for new releases.

## Source

| | |
|---|---|
| Version | 2.28.0 (2025-11-12) |
| Tarball | https://dist.torproject.org/metrics-lib/2.28.0/metrics-lib-2.28.0.tar.gz |
| SHA-256 | `b9eb1af0a32bea045b0c55a0f8259d68ef729896edc0ebf1832c664a3631a672` |
| Signing key | `8D89 195E 86FA 539F 8177 2E90 4552 01C4 C0F0 C238` (from `metrics-lib-2.28.0.tar.gz.asc`) |
| Copied | `src/main/java/org/torproject/descriptor` and the top-level `CHANGELOG.md`, `CONTRIB.md`, `LICENSE`, `README.md` |

The runtime dependencies upstream declares in `build.xml` (commons-codec, commons-compress, Jackson 2, slf4j, xz) are
declared in `backend/build.gradle.kts`, at newer versions. commons-lang3 and commons-math3 are listed there as well but
are not used by the main sources.

## Local patches

Keep this list current. Re-apply each patch after copying a new upstream release.

1. `index/DescriptorIndexCollector`: reject remote file names that contain `/`, `\`, `.` or `..`, and skip files whose
   target directory does not resolve inside the local download directory (real-path check). Use `File.separator` when
   indexing local files. See `DescriptorIndexCollectorTest`.
2. `index/IndexNode`, `onionperf/ParsedOnionPerfAnalysis`: use `PropertyNamingStrategies.SNAKE_CASE`, required by the
   Jackson 2 version pinned in `build.gradle.kts`.
3. `impl/ServerDescriptorImpl`: allow multiple `family-cert` lines (one per family) and keep the first one for
   `getFamilyCert()`. Upstream 2.28.0 allows it at most once and rejects such descriptors. See
   `ServerDescriptorFamilyCertTest`.
4. `index/DescriptorIndexCollector`: delete the temporary `.<filename>` download when its size does not match the index
   or the download fails. Upstream leaves it behind, and `DescriptorReaderImpl` then reads it like any other file on
   every run. See `DescriptorIndexCollectorTest`.
5. `index/DescriptorIndexCollector`: log a warning for fetched files above 2 GiB and do not store files above 10 GiB,
   checked against both the index size and the bytes actually received. See `DescriptorIndexCollectorTest`.

## Upgrading

1. Download the tarball and its `.asc`, verify the signature and record the checksum and signing key above.
2. Diff the new upstream `src/main/java` against the previous upstream release (not against this directory) and read the
   upstream `CHANGELOG.md`. Check `build.xml` for changed runtime dependencies.
3. Replace `descriptor/` and the top-level docs with the upstream copy in one commit, then re-apply the local patches in
   separate commits.
4. Parse a few days of recent consensuses and server descriptors with the old and new version and compare the fields
   TorMap stores (see `RelayDetails` and `RelayLocation`).
