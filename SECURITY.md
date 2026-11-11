# Security Policy

## Reporting Security Issues

We take security bugs seriously and appreciate your efforts to responsibly disclose your findings. We will acknowledge
your contributions.

To report a security issue, please use the [GitHub security tab](https://github.com/TorMap/tormap/security) of the
repository and create a new report via the button "Report a Vulnerability"
([direct link](https://github.com/TorMap/tormap/security/advisories/new)). Please do not open public issues or pull
requests for vulnerabilities.

After receiving the report, we will keep you informed of the progress towards a fix and full announcement, and may ask
for additional information or guidance.

Report security bugs in third-party modules to the person or team maintaining the module. Issues in the Tor protocol or
the Tor network itself should be reported to the [Tor Project](https://www.torproject.org/contact/).

## Threat model

[THREAT_MODEL.md](THREAT_MODEL.md) describes the assets, trust boundaries, assumptions and severity calibration used to
triage reports. Please use it to judge the impact of a finding. Update it when a change adds or moves a trust boundary.

## Supported versions

Only the latest release receives security fixes. Releases and Docker images are published at:

- https://github.com/TorMap/tormap/releases
- https://hub.docker.com/u/tormap

## Security scanning

Static code analysis (SAST) runs in GitHub Actions on pull requests, pushes to `master` and weekly, with results in
the repository's Code scanning alerts:

- [CodeQL](https://codeql.github.com/) (`.github/workflows/codeql.yml`): `security-and-quality` queries for
  Kotlin, TypeScript and GitHub Actions.
- [Semgrep Community Edition](https://semgrep.dev/) (`.github/workflows/semgrep.yml`): Semgrep Registry rules for
  Kotlin, TypeScript/React, GitHub Actions and Dockerfiles. Findings are also listed in the job log and the
  `semgrep-results` artifact.

[Snyk](https://snyk.io/) is used for dependency scanning (SCA). Pull requests are checked by the Snyk GitHub
integration (PR status checks, configured in Snyk, no CI workflow involved). Snyk Code (SAST) is not used, because the
free plan's monthly test quota is too small for the number of pull requests. Run the dependency scan locally when
dependencies change:

- Backend: `cd backend && ./gradlew snykTest`
- Frontend: `cd frontend && yarn snyk:test`

- Prerequisite: [install the Snyk CLI](https://docs.snyk.io/snyk-cli/install-or-update-the-snyk-cli) and run
  `snyk auth` once.
- Findings of severity `high` or above fail the command. Unlike the PR checks, which only report issues introduced by
  the PR, the local commands report all issues. Extra CLI options can be appended:
  `yarn snyk:test --json` or `./gradlew snykTest -PsnykArgs="--json"`.
- Run SCA whenever dependencies change. Snyk uploads dependency information to snyk.io.
- For frontend dependency changes, also run `cd frontend && yarn npm audit --severity high` (add
  `--environment production` for runtime dependencies only).

## Supply chain

- Dependencies come only from Maven Central (plus the Gradle plugin portal) and the default npm registry.
- Frontend install scripts are disabled and package versions younger than 3 days are refused (`frontend/.yarnrc.yml`).
- Dependency updates are managed by Renovate.
- GitHub secret scanning and push protection are enabled.
