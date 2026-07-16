# kode への貢献

ご貢献いただきありがとうございます！

**English: [CONTRIBUTING.md](CONTRIBUTING.md)** — Issue・PR は英語・日本語のどちらでも歓迎します。

---

## ブランチ運用

| ブランチ | 役割 |
|---------|------|
| `main` | 開発統合ブランチ |
| `release` | 本番リリースブランチ |

`main` ブランチへの PR がマージされると、`.github/workflows/release-pr.yml` によって `main → release` への "Production release" PR が自動的に作成されます(開いているリリース PR がすでに存在する場合は何もせず、新しいコミットは同じ PR に自動的に載ります)。

---

## コーディング規約

本プロジェクトは [detekt](https://detekt.dev/) で静的解析を行います。設定は `detekt.yml` を参照してください。CIでは `maxIssues: 0` のため、detekt の指摘がゼロになるまで PR はマージできません。

---

## PR を出す前に

PR を提出する前に、以下のコマンドがローカルで通ることを確認してください。

```bash
./gradlew test     # ユニットテスト
./gradlew detekt   # 静的解析
```

---

## コミットメッセージ

コミットメッセージの形式に特定の規約は設けていません。変更内容が伝わる明瞭なメッセージを書いてください。

---

## バグ報告・機能リクエスト

GitHub の Issue テンプレートをご利用ください:

- [Bug Report](https://github.com/PannyPatchy/kode/issues/new?template=bug_report.md)
- [Feature Request](https://github.com/PannyPatchy/kode/issues/new?template=feature_request.md)

セキュリティ脆弱性については公開 Issue を立て**ない**でください — [SECURITY.md](SECURITY.md) を参照してください。

---

## 行動規範

本プロジェクトは [Contributor Covenant](CODE_OF_CONDUCT.md) に従います。参加にあたってはこの行動規範の遵守をお願いします。
