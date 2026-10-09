# TorMap

This project visualizes current and past public Tor relays on a world map. The backend regularly downloads descriptors
from [TorProject Archive](https://metrics.torproject.org/collector/archive/) and saves a processed version in a
PostgreSQL database. The frontend displays the data on a world map and allows to filter and search for relays.

## Development

The project is split into a `backend` and `frontend` part. The backend is written in Kotlin and the frontend in TypeScript.
You can work on either part independently:
- [Backend](backend/README.md)
- [Frontend](frontend/README.md)

## Security scanning

[Snyk](https://snyk.io/) is used for dependency scanning (SCA) and static code analysis (SAST). Pull requests are
checked by the Snyk GitHub integration (PR status checks, configured in Snyk, no CI workflow involved). Run the same
scans locally before opening a PR:

| Part     | SCA (`snyk test`)                         | SAST (`snyk code test`)                  |
|----------|-------------------------------------------|------------------------------------------|
| Backend  | `cd backend && ./gradlew snykTest`        | `cd backend && ./gradlew snykCode`       |
| Frontend | `cd frontend && yarn snyk:test`           | `cd frontend && yarn snyk:code`          |

- Prerequisite: [install the Snyk CLI](https://docs.snyk.io/snyk-cli/install-or-update-the-snyk-cli) and run
  `snyk auth` once.
- Findings of severity `high` or above fail the command. Unlike the PR checks, which only report issues introduced by
  the PR, the local commands report all issues. Extra CLI options can be appended:
  `yarn snyk:test --json` or `./gradlew snykTest -PsnykArgs="--json"`.
- Run SCA whenever dependencies change. Snyk uploads dependency and source information to snyk.io.

## Releases

We use [Semantic Versioning](https://semver.org/) and try to keep the frontend,
backend, GitHub and Docker tags consistent. Releases can be found at:

- https://github.com/TorMap/tormap/releases
- https://hub.docker.com/u/tormap
