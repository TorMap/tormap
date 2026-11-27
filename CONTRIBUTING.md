# Contributing to TorMap

Thanks for helping. Please keep pull requests small: each one should leave the project working on its own.

- **Build and test:** see [AGENTS.md](AGENTS.md#build-and-test). Run `scripts/install-git-hooks.sh` once per clone.
- **Pull requests:** Conventional Commit titles and labels as in [AGENTS.md](AGENTS.md#pull-requests). Whoever opens
  the PR sets the labels, AI agents included; they sort the release notes, so don't leave them to the maintainer. All
  required checks must pass before merge.
- **Secure development:** follow the rules in [PIPELINE.md](PIPELINE.md) and the review checklist in
  [THREAT_MODEL.md](THREAT_MODEL.md#review-checklist-for-changes). New dependencies follow the
  [dependency policy](.claude/skills/dependencies/SKILL.md).
- **Tests:** new functionality comes with automated tests, and a bug fix comes with a test that fails without the
  fix. Backend tests are JUnit (`backend/src/test`), frontend tests Vitest (`*.test.ts(x)` next to the code), CLI tests
  JUnit (`cli/src/test`); see [AGENTS.md](AGENTS.md#build-and-test) for how to run them.
- **Code style:** ktlint (`.editorconfig`) for Kotlin and ESLint for TypeScript, both enforced in CI without warnings.
- **Review:** the maintainer reviews every pull request for correctness, tests, the
  [review checklist](THREAT_MODEL.md#review-checklist-for-changes), the [dependency policy](.claude/skills/dependencies/SKILL.md)
  and readable, documented changes. Questions and requested changes are discussed in the pull request.
- **Security issues:** never in public issues or pull requests. Report them as described in [SECURITY.md](SECURITY.md).

By opening a pull request you confirm that you have read these rules and you certify the
[Developer Certificate of Origin 1.1](https://developercertificate.org/) for your contribution: you wrote it or have
the right to submit it under the project's [GPL-3.0 license](LICENSE.txt).

Conduct in the project follows the [Code of Conduct](CODE_OF_CONDUCT.md), decisions the [governance](GOVERNANCE.md).
