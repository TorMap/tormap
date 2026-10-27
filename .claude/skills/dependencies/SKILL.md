---
name: dependencies
description: TorMap policy and vetting steps for adding, upgrading, removing or overriding a backend (Gradle) or frontend (Yarn) dependency, including Gradle plugins, lockfile changes and fixing Renovate update PRs. Use before proposing, installing or building with any dependency change.
---

# Dependency changes

Applies to direct and transitive dependencies, Gradle plugins and build tools. Docker base images and GitHub Actions are pinned by digest/SHA and bumped by Renovate, not here. For a Renovate PR, Renovate picked the version: apply steps 4 and 6 only.

## 1. Is a library needed?
- Prefer the standard library, platform APIs (JDK, Spring, browser) or libraries the project already uses.
- For simple, narrow functionality, a small local implementation can be easier to maintain and verify than a dependency.
- Never hand-roll security-sensitive code (cryptography, authentication, complex parsing): use or keep established libraries or vetted platform APIs. Also keep specialized libraries whose replacement would add security, correctness or maintenance risk.
- Prefer options with fewer transitive dependencies. Don't pull in a large tree for functionality the project barely uses.
- When changing existing dependency usage, list unused or redundant libraries and unneeded optional integrations as removal candidates. Remove them only within the task's scope, after checking code, reflection and dynamic loading, Spring auto-configuration, plugins, configuration, scripts, tests and build tooling. Preserve behavior and supported runtimes (JDK from `sourceCompatibility`, Node from `engines`), and explain anything you keep.

## 2. Pin down the exact package and version
- Never take package names or versions from memory. Get the coordinates from the project's own documentation or repository, then check the live registry entry: `curl -s https://registry.npmjs.org/<name>` (`maintainers`, per-version `_npmUser`, `repository`) or `https://repo1.maven.org/maven2/<group/as/path>/<artifact>/maven-metadata.xml` (and the POM's `scm`).
- Registry metadata alone is not proof: the source repository must point back to the package. Investigate lookalike names, unexpected ownership or maintainer changes, and unfamiliar new packages before going on.
- Use the latest stable, supported release that is compatible and older than the release-age gate: 3 days (7 for majors), matching Renovate. Yarn enforces 3 days via `npmMinimalAgeGate`; Gradle has no gate, so check `publishedAt` yourself. Explain any older choice. Scrutinize very recent releases and unexpected release changes: no advisory does not mean safe.

## 3. Vet
Query with `curl`, sending package names and versions only. Percent-encode `:` `@` `/` (`org.postgresql%3Apostgresql`, `%40mui%2Fmaterial`).
- **deps.dev (required)**: `https://api.deps.dev/v3/systems/{maven|npm}/packages/{name}/versions/{version}`. Check `publishedAt`, `isDeprecated`, `licenses` (must stay compatible with GPL-3.0), `links`/`relatedProjects` (source repo) and `advisoryKeys` (resolve each with `/v3/advisories/{id}`; report affected versions and whether it applies here). Append `:dependencies` for the resolved transitive graph before installing; `.../packages/{name}` lists all versions. Also check the project's own security advisories (e.g. its GitHub Security tab).
- **OpenSourceMalware (best-effort)**: `curl -s -H "Authorization: Bearer $OSM_TOKEN" "https://api.opensourcemalware.com/functions/v1/check-malicious?report_type=package&ecosystem={maven|npm}&resource_identifier={name}&version={version}"`. `"malicious": true` blocks the change; `false` only means no verified report. Never print or log `OSM_TOKEN`, don't use `curl -v`, and don't read `.env` or the environment to find it. If it is unset, skip.
- **endoflife.date (best-effort)** for runtimes and frameworks: `https://endoflife.date/api/v1/products/{product}/releases/{cycle}` (`spring-boot`, `kotlin`, `nodejs`, `postgresql`, `gradle`, `tomcat`, `yarn`). Don't move to or stay on an EOL cycle without telling the maintainer.
- **Maintenance**: support status, unresolved issues, security responsiveness, runtime compatibility. Few releases can be fine for a mature library. Reject archived, deprecated or abandoned projects.
- Avoid versions with known applicable vulnerabilities. If no advisory source (deps.dev, or Snyk in step 6) can be checked, stop and report; never call an unchecked version vulnerability-free. Unreachable best-effort sources are reported, not blocking.

## 4. Assess impact and keep scope
- Inspect the resolved transitive tree (deps.dev `:dependencies`; after install `./gradlew dependencies --configuration runtimeClasspath`, `./gradlew dependencyInsight --dependency <name>`, `yarn why <name>`). Justify any substantial growth and its update burden.
- For upgrades, read release notes and migration guides between the current and target version. Name the concrete effects on this code and the migration work.
- No unrelated upgrades, forced overrides (`resolutions`, Gradle `force`/constraints) or hand-edited `yarn.lock`; let the package manager resolve. Fix a vulnerable transitive dependency with a compatible parent update first, and use `dependencyInsight` before any override.
- Respect the pins and disabled updates in `renovate.json` (Tor metrics-lib support libraries).
- If the change needs a broader migration or hits a compatibility blocker, stop and report the evidence and options. Don't retry speculative upgrades or fixes; resume only with new evidence or an agreed plan.

## 5. Report and wait for approval
Before the first install, build or test with a new or changed dependency name or version, give the maintainer a short report and wait for approval:
- package and version, why it is needed, alternatives considered (including no dependency), transitive additions and update burden
- each source with the date checked and its result, skipped sources and why, advisories with affected versions and applicability
- breaking changes and migration work, removal candidates

Anything you could not verify is a blocker: proceed only if the maintainer explicitly accepts that risk.

## 6. After approval (removals: right away)
- Install through the package manager, then build and test as in `AGENTS.md`.
- SCA: `cd backend && ./gradlew snykTest` and/or `cd frontend && yarn snyk:test` (only if the maintainer authenticated the Snyk CLI). For frontend changes also `cd frontend && yarn npm audit --severity high` (`--environment production` for runtime only; run outside the sandbox). Fix or report new high/critical findings before opening the PR. Don't add other scanner plugins without maintainer approval.
- CI's OpenSourceMalware check (`.github/scripts/osm-check.py`) only covers new or changed `yarn.lock` entries and inline Gradle versions, not BOM-managed or transitive Maven artifacts: those rely on your step 3.
- Put the step 5 report in the PR description. Don't claim vetting from a skipped source.
