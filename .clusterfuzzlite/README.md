# Fuzzing

The backend's parsers of untrusted input are fuzzed with [Jazzer](https://github.com/CodeIntelligenceTesting/jazzer)
through [ClusterFuzzLite](https://google.github.io/clusterfuzzlite/). The workflow
[`fuzzing.yml`](../.github/workflows/fuzzing.yml) fuzzes the latest `master` every day for an hour and prunes the
corpus once a week. A crash fails the run and opens (or comments on) an issue labeled `fuzzing` with the stack trace and
the input.

| Fuzz target              | Input                                                                     |
|--------------------------|---------------------------------------------------------------------------|
| `DescriptorParserFuzzer` | Collector descriptors (vendored metrics-lib) and the entities built from them |
| `FamilyCertFuzzer`       | Ed25519 family certificates (`FamilyCertUtil`)                            |
| `UserStatsCsvFuzzer`     | Tor Metrics CSV of relay users per country                                |
| `LiteViewFuzzer`         | TorMap Lite query parameters and the links built from them                |

The targets are in [`backend/src/fuzz`](../backend/src/fuzz), seed inputs in `backend/src/fuzz/seeds/<target>/`.
A target is an `object` named `*Fuzzer` with a `@JvmStatic fun fuzzerTestOneInput(data: FuzzedDataProvider)`.
[`build.sh`](build.sh) builds them with Gradle (`installFuzzTargets`), the Jazzer API jar comes with the base image.

## Running locally

With Docker and a checkout of [OSS-Fuzz](https://github.com/google/oss-fuzz):

```sh
python3 infra/helper.py build_fuzzers --external <path to tormap> --sanitizer address
python3 infra/helper.py run_fuzzer --external <path to tormap> DescriptorParserFuzzer -- -max_total_time=300
```

## Reproducing a crash

Save the input from the issue (`base64 -d > crash-input`), then:

```sh
python3 infra/helper.py reproduce --external <path to tormap> <target> crash-input
```

Turn the input into a regular test next to the fixed code, so the bug stays fixed.
