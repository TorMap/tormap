# Build and release pipeline

How code gets from a pull request to users, and the rules that keep that path safe. This is TorMap's secure
development policy for the pipeline. The application threat model is in [THREAT_MODEL.md](THREAT_MODEL.md), rules for
AI coding agents are in [AGENTS.md](AGENTS.md).

## Stages

```
pull request ──► required checks ──► review and merge to master ──► Release workflow ──► v* tag ──► publish
   (branch)      build, tests, SAST,     (maintainer)                 (maintainer)                  Docker Hub,
                 dependency + secret                                                                GitHub release
                 scanning                          └──► master push ──► tormap.org (Firebase Hosting), :edge images
```

| Workflow | Runs on | Does | Secrets | Publishes |
|---|---|---|---|---|
| `backend-test.yml`, `cli-test.yml` | PR, master | Build and test (Gradle) | `OSM_TOKEN` | Nothing |
| `frontend-pull-request.yml` | PR | Build and test (Yarn), preview deploy | `OSM_TOKEN`, Firebase | Firebase preview channel |
| `frontend-push.yml` | master | Build, test, deploy | Firebase (env `firebase-live`) | tormap.org |
| `codeql.yml`, `semgrep.yml`, `zizmor.yml` | PR, master, weekly | SAST of code and workflows | None | Code scanning results |
| `scorecard.yml` | master, weekly | OpenSSF Scorecard of the repository's supply-chain practices | None | Public score, Code scanning results |
| `release.yml` | Manual (master) | Version bump commit, `vX.Y.Z` tag, draft release | Release GitHub App (env `release`) | Tag, draft release |
| `backend-docker.yml` | PR, master, `v*` tags | Build the image without secrets, then push exactly that digest and attest it | Docker Hub (env `docker-hub`), push job only | `tormap/backend` |
| `frontend-docker.yml` | master, `v*` tags | Build and push the image | Docker Hub (env `docker-hub`) | `tormap/frontend` |
| `cli-native.yml` | PR, master, `v*` tags | Native binaries, end-to-end test, image | Docker Hub (env `docker-hub`) | CLI binaries on the release, `tormap/cli` |

Outside Actions: Renovate opens dependency PRs; the Snyk and Socket GitHub integrations and GitGuardian check PRs;
GitHub secret scanning with push protection checks every push.

## Rules

Mapped to the [OWASP Top 10 CI/CD Security Risks](https://owasp.org/www-project-top-10-ci-cd-security-risks/).

| Risk | Rule |
|---|---|
| CICD-SEC-1 Flow control | `master` only changes through pull requests with passing required checks. Only the Release workflow creates `v*` tags. Tags, releases and published images are never overwritten. |
| CICD-SEC-2 Identity and access | Every account with access uses 2FA (passkeys or security keys where offered). Bots and apps get the smallest scope that works. Access is reviewed once a year. |
| CICD-SEC-3 Dependency chain abuse | [Dependency policy](.claude/skills/dependencies/SKILL.md): approved registries only, exact pins and lockfiles (`yarn.lock`, Gradle `gradle.lockfile`), install scripts off, release-age gate, vetting before adding a dependency. |
| CICD-SEC-4 Poisoned pipeline execution | No `pull_request_target`, no `workflow_run` on PR code. Untrusted text (branch names, titles, inputs) reaches shell steps only through `env:`. |
| CICD-SEC-5 Pipeline-based access controls | Every job declares least-privilege `permissions:`. Release, publishing and production deploy secrets live in environments limited to `master` and `v*` tags. |
| CICD-SEC-6 Credential hygiene | See [Secrets](#secrets). |
| CICD-SEC-7 Insecure system configuration | GitHub-hosted, ephemeral runners only. Workflow changes are scanned by CodeQL, Semgrep and zizmor. |
| CICD-SEC-8 Ungoverned third-party services | Third-party actions and images are pinned by commit SHA or digest and updated by Renovate. Integrations are installed only on the repositories that need them. |
| CICD-SEC-9 Artifact integrity | Release assets have `SHA256SUMS` and signed build provenance; images carry provenance. Downloaded data is checked against pinned checksums. |
| CICD-SEC-10 Logging and visibility | Changes are traceable to a PR, releases to a workflow run and tag. |

## Secure coding

Code follows the review checklist in [THREAT_MODEL.md](THREAT_MODEL.md), the security rules in [AGENTS.md](AGENTS.md),
and the [OWASP ASVS](https://owasp.org/www-project-application-security-verification-standard/) for the backend and
TorMap Lite. Lint and type errors are fixed before merge.

## Secrets

- Secrets live only in GitHub Actions secrets (repository or environment), never in the repository, workflow files,
  logs or prompts for AI agents.
- Each secret has one purpose and the narrowest scope its service allows. Release, publishing and production deploy secrets are environment secrets.
- Secrets are rotated at least once a year, when someone with access leaves, and right away when a leak is suspected.
  On a suspected leak the old secret is revoked first, then replaced.
- Operators keep runtime secrets (database and admin passwords, metrics keys) in environment variables or Docker
  secrets readable only by the service user, never in compose files.

## Patching

Security updates of dependencies, base images and tools are merged within 7 days for critical and 30 days for high
severity, other updates in Renovate's normal flow. A fix that affects users ships in a patch release.

## Releases

A release is published only when all required checks passed on the released commit, no unresolved critical or high
code scanning alert applies to it, and the release assets and their checksums are attached. Security advisories fixed
in a release are published with it.

## AI coding agents

AI agents work under the same rules as people: changes only through pull requests and the same required checks, no
merging, releasing or changing repository settings, and no access to secrets. Their instructions (`AGENTS.md`,
`.claude/skills/`, `.github/copilot-review-instructions.md`) are reviewed like code. No pipeline step calls an AI
model.

## Development tools

IntelliJ IDEA or VS Code with marketplace extensions, kept up to date, with workspace trust prompts on. The pre-commit
hook (`scripts/install-git-hooks.sh`) scans staged changes for secrets and lint errors.

## Review

This policy is reviewed once a year and when the pipeline changes. Last reviewed: 2026-11.
