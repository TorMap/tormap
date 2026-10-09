# AGENTS.md

Rules for AI coding agents (and a quick reference for humans) working in this repository.

## Layout
- `backend/`: Kotlin, Spring Boot, Gradle (Kotlin DSL). Image is built with Jib.
- `frontend/`: React, TypeScript, Vite, Yarn 4 (exact version pins).
- `.github/workflows/`: CI. Actions are pinned by commit SHA with a version comment.

## Build and test
- Backend: `cd backend && ./gradlew test` (needs a JDK matching `sourceCompatibility` in `backend/build.gradle.kts` and a running Docker daemon for Testcontainers).
- Local backend image: `./gradlew jibDockerBuild -Djib.to.image=tormap/backend:local -Djib.to.tags=local`.
- Frontend: `cd frontend && yarn install --immutable && yarn build && yarn lint`.

## Pull requests
- Use Conventional Commit titles, e.g. `chore(backend): ...`, `fix(frontend): ...`. The PR title becomes the release note line.
- Set labels when opening the PR (`gh pr create --label ...`). They group the generated release notes:
  - `breaking`: incompatible API or behavior change.
  - `deployment`: operators must do something (new env var, volume ownership, JDK or image change).
  - `security`: fixes a vulnerability.
  - `enhancement`, `bug`, `documentation`: as usual. Add `backend` and/or `frontend`.
- If a PR needs operator action, add a short "Deployment notes" section to the PR description.
- Keep PRs small: each one should leave the project working and mergeable on its own.

## Dependencies
- Before running a build or tests with a new dependency or a changed dependency name or version, ask the maintainer to verify it.
- Renovate manages updates. Respect the intentional pins and disabled updates in `renovate.json` (React, MUI, Tor metrics-lib support libraries).
- Gradle versions are inline in `backend/build.gradle.kts`. The project version is duplicated in `backend/build.gradle.kts` and `frontend/package.json`.

## Security scanning
- Run SCA whenever dependencies change (add, remove, bump, lockfile): `cd backend && ./gradlew snykTest` and/or `cd frontend && yarn snyk:test`. Fix or report new high/critical findings before opening the PR.
- Run SAST (`./gradlew snykCode`, `yarn snyk:code`) when changing security-relevant code (auth, input handling, networking, serialization).
- PRs are gated by the Snyk GitHub integration (PR status checks), not by a CI workflow. Don't add Snyk steps or `SNYK_TOKEN` to workflows. Keep shared flags in the Gradle task or yarn script.
- Never run `snyk auth` or handle tokens. If the CLI is not authenticated, ask the maintainer. Snyk uploads dependency and code data to snyk.io, so only run it once the maintainer has authenticated.
- Don't add `.snyk` ignores without a documented reason and an expiry date.

## Commits and secrets
- End commit messages written by an agent with the Co-Authored-By trailer for the agent, and PR descriptions with the generated-with note.
- Never commit secrets or `.env` files. New Relic and admin credentials come from environment variables.
