#!/usr/bin/env bash
# Usage: check-release-version.sh TAG
#
# Fails unless TAG (e.g. v4.3.0) equals "v" + the version in backend/build.gradle.kts, frontend/package.json and
# cli/build.gradle.kts. The backend image is named after its Gradle version and the frontend and CLI images after the
# tag, so a mismatch would publish a release under the wrong name or overwrite an older image. Run from the
# repository root. Only bash and sed, so it also runs on the macOS and Windows runners.
set -euo pipefail

tag="${1:?usage: check-release-version.sh TAG}"
# tr: Git on Windows may check files out with CRLF line endings
gradle_version() { tr -d '\r' < "$1" | sed -n 's/^version = "\(.*\)"$/\1/p'; }
backend_version=$(gradle_version backend/build.gradle.kts)
cli_version=$(gradle_version cli/build.gradle.kts)
frontend_version=$(tr -d '\r' < frontend/package.json | sed -n 's/^[[:space:]]*"version":[[:space:]]*"\([^"]*\)".*/\1/p' | head -n 1)

for version in "$backend_version" "$frontend_version" "$cli_version"; do
  if [[ "v$version" != "$tag" ]]; then
    echo "::error::Tag $tag does not match backend version $backend_version (backend/build.gradle.kts), frontend version $frontend_version (frontend/package.json) and CLI version $cli_version (cli/build.gradle.kts). Release with the Release workflow, which bumps all three versions before tagging."
    exit 1
  fi
done
echo "Tag $tag matches the backend, frontend and CLI versions."
