# Contributing to TorMap

Thanks for helping. Please keep pull requests small: each one should leave the project working on its own.

- **Build and test:** see [AGENTS.md](AGENTS.md#build-and-test). Run `scripts/install-git-hooks.sh` once per clone.
- **Pull requests:** Conventional Commit titles and labels as in [AGENTS.md](AGENTS.md#pull-requests). Whoever opens
  the PR sets the labels, AI agents included; they sort the release notes, so don't leave them to the maintainer. All
  required checks must pass before merge.
- **Secure development:** follow the rules in [PIPELINE.md](PIPELINE.md) and the review checklist in
  [THREAT_MODEL.md](THREAT_MODEL.md#review-checklist-for-changes). New dependencies follow the
  [dependency policy](.claude/skills/dependencies/SKILL.md).
- **Security issues:** never in public issues or pull requests. Report them as described in [SECURITY.md](SECURITY.md).

By opening a pull request you confirm that you have read these rules and that you may contribute the code under the
project's [GPL-3.0 license](LICENSE.txt).
