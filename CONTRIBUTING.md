# Contributing to kode / kode への貢献

Thank you for your interest in contributing!  
ご貢献いただきありがとうございます！

---

## Branch Workflow / ブランチ運用

| Branch | Role / 役割 |
|--------|-------------|
| `main` | Development integration / 開発統合ブランチ |
| `release` | Production / 本番リリースブランチ |

**日本語:**  
`main` ブランチへの PR がマージされると、`.github/workflows/release-pr.yml` によって `main → release` への本番リリース PR が自動的に作成されます（同名の PR がすでに存在する場合は何もしません）。

**English:**  
When a PR is merged into `main`, the workflow `.github/workflows/release-pr.yml` automatically opens a production release PR from `main` into `release`. If an open release PR already exists, it does nothing — new commits ride along automatically.

---

## Coding Style / コーディング規約

**日本語:**  
本プロジェクトは [detekt](https://detekt.dev/) で静的解析を行います。設定は `detekt.yml` を参照してください。  
CIでは `maxIssues: 0` のため、detekt の指摘がゼロになるまで PR はマージできません。

**English:**  
This project uses [detekt](https://detekt.dev/) for static analysis. See `detekt.yml` for the configuration.  
The CI enforces `maxIssues: 0`, so all detekt findings must be resolved before a PR can be merged.

---

## Before Opening a PR / PR を出す前に

**日本語:**  
PR を提出する前に、以下のコマンドがローカルで通ることを確認してください。

```bash
./gradlew test     # ユニットテスト
./gradlew detekt   # 静的解析
```

**English:**  
Before opening a PR, make sure the following commands pass locally:

```bash
./gradlew test     # unit tests
./gradlew detekt   # static analysis
```

---

## Commit Messages / コミットメッセージ

**日本語:**  
コミットメッセージの形式に特定の規約は設けていません。変更内容が伝わる明瞭なメッセージを書いてください。

**English:**  
There is no specific commit message convention. Write clear messages that describe what changed and why.

---

## Reporting Bugs & Requesting Features

Please use the GitHub Issue templates:

- [Bug Report](https://github.com/PannyPatchy/kode/issues/new?template=bug_report.md)
- [Feature Request](https://github.com/PannyPatchy/kode/issues/new?template=feature_request.md)
