#!/usr/bin/env bash
# Usage: check-release-version.sh TAG
#
# Fails unless TAG (e.g. v4.3.0) equals "v" + the version in backend/build.gradle.kts and in frontend/package.json.
# The backend image is named after the Gradle version and the frontend image after the tag, so a mismatch would
# publish a release under the wrong name or overwrite an older image. Run from the repository root.
set -euo pipefail

tag="${1:?usage: check-release-version.sh TAG}"
backend_version=$(sed -n 's/^version = "\(.*\)"$/\1/p' backend/build.gradle.kts)
frontend_version=$(jq -r .version frontend/package.json)

for version in "$backend_version" "$frontend_version"; do
  if [[ "v$version" != "$tag" ]]; then
    echo "::error::Tag $tag does not match backend version $backend_version (backend/build.gradle.kts) and frontend version $frontend_version (frontend/package.json). Release with the Release workflow, which bumps both versions before tagging."
    exit 1
  fi
done
echo "Tag $tag matches the backend and frontend versions."
