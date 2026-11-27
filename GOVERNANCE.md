# Governance

TorMap is a small project with one maintainer, listed in the [README credits](README.md#credits). Decisions are made in
the open, in issues and pull requests.

## Roles

| Role | Responsibilities |
|---|---|
| Maintainer | Sets the direction ([ROADMAP.md](ROADMAP.md)), reviews and merges pull requests, runs releases, handles security reports ([SECURITY.md](SECURITY.md)) and enforces the [Code of Conduct](CODE_OF_CONDUCT.md). Has admin access to the GitHub organization, Docker Hub and the project's hosting. |
| Hosting operator | Runs the public instance behind tormap.org and api.tormap.org and applies the deployment notes of each release. |
| Contributor | Anyone who opens an issue or pull request, following [CONTRIBUTING.md](CONTRIBUTING.md). |

## Decisions

- Changes to `master` are made only through pull requests that pass the required checks ([PIPELINE.md](PIPELINE.md)).
- The maintainer decides whether a change is accepted, after discussion in the issue or pull request. Reasons for
  declining a change are given there.
- Contributors who make repeated, significant contributions can be invited as maintainers, with the same rules.

## Continuity

A second member of the GitHub organization can approve the release workflow. If the maintainer stops supporting the
project, that member can triage issues, merge pull requests and publish releases within a week, and can recover full
access to all project accounts after a safety delay.

## Changes to this document

Changes to governance are made by pull request like any other change.
