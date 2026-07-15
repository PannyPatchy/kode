# Contributing to kode

Thank you for your interest in contributing!

**日本語版: [CONTRIBUTING.ja.md](CONTRIBUTING.ja.md)** — Issues and PRs are welcome in English or Japanese.

---

## Branch Workflow

| Branch | Role |
|--------|------|
| `main` | Development integration |
| `release` | Production |

When a PR is merged into `main`, the workflow `.github/workflows/release-pr.yml` automatically opens a "Production release" PR from `main` into `release`. If an open release PR already exists, it does nothing — new commits ride along automatically.

---

## Coding Style

This project uses [detekt](https://detekt.dev/) for static analysis. See `detekt.yml` for the configuration. The CI enforces `maxIssues: 0`, so all detekt findings must be resolved before a PR can be merged.

---

## Before Opening a PR

Make sure the following commands pass locally:

```bash
./gradlew test     # unit tests
./gradlew detekt   # static analysis
```

---

## Commit Messages

There is no specific commit message convention. Write clear messages that describe what changed and why.

---

## Reporting Bugs & Requesting Features

Please use the GitHub Issue templates:

- [Bug Report](https://github.com/PannyPatchy/kode/issues/new?template=bug_report.md)
- [Feature Request](https://github.com/PannyPatchy/kode/issues/new?template=feature_request.md)

For security vulnerabilities, do **not** open a public issue — see [SECURITY.md](SECURITY.md).

---

## Code of Conduct

This project follows the [Contributor Covenant](CODE_OF_CONDUCT.md). By participating, you are expected to uphold it.
