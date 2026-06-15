# kode 設計書 / Design Document

`kode` — AIエージェントのためのKotlinコードベース理解CLI。
`kode` — A Kotlin codebase comprehension CLI for AI agents.

このディレクトリは `kode` の設計書です。トピックごとに分割し、日本語版（`ja/`）と英語版（`en/`）を提供します。
This directory contains the `kode` design document, split by topic and provided in Japanese (`ja/`) and English (`en/`).

> ⚠️ 本書は **実装着手前の設計** です。コード例は概念を示す擬似コードであり、最終的なAPIと一致しない場合があります。
> ⚠️ This is a **pre-implementation design**. Code samples are illustrative pseudo-code and may differ from the final API.

## 章構成 / Chapters

| # | トピック | 日本語 | English |
|---|----------|--------|---------|
| 00 | 概要 / Overview | [ja](ja/00-overview.md) | [en](en/00-overview.md) |
| 01 | アーキテクチャ（Clean Architecture） / Architecture | [ja](ja/01-architecture.md) | [en](en/01-architecture.md) |
| 02 | ドメインモデル（DDD） / Domain Model | [ja](ja/02-domain-model.md) | [en](en/02-domain-model.md) |
| 03 | 各コマンドの処理フロー / Command Flows | [ja](ja/03-commands.md) | [en](en/03-commands.md) |
| 04 | LSP接続・通信方式 / LSP Integration | [ja](ja/04-lsp.md) | [en](en/04-lsp.md) |
| 05 | Gradle Tooling API / Gradle Tooling API | [ja](ja/05-gradle-tooling.md) | [en](en/05-gradle-tooling.md) |
| 06 | .kode.json の読み書き / Config I/O | [ja](ja/06-config-kode-json.md) | [en](en/06-config-kode-json.md) |
| 07 | エラーハンドリング / Error Handling | [ja](ja/07-error-handling.md) | [en](en/07-error-handling.md) |
| 08 | GraalVM Native Image ビルド / Native Image Build | [ja](ja/08-native-image.md) | [en](en/08-native-image.md) |
| 09 | 依存関係・ライセンス精査 / Dependencies & Licenses | [ja](ja/09-dependencies-license.md) | [en](en/09-dependencies-license.md) |
| 10 | ディレクトリ構成 / Directory Layout | [ja](ja/10-directory-layout.md) | [en](en/10-directory-layout.md) |

## 設計原則 / Design Principles

- **JSON-only 出力**: すべての成果物はAI向けの構造化JSONで返す。
- **意味解釈はしない**: データの収集・構造化までが責務。判断はAI側。
- **Clean Architecture / DDD**: ドメインを中心に依存を内向きへ。
- **OSS公開前提**: 採用する依存はすべてパーミッシブ or コピーレフト非伝播。プロジェクト本体は Apache-2.0。
- **単一バイナリ配布**: GraalVM Native Image。

## ライセンス / License

本リポジトリは Apache License 2.0 です。詳細は [`/LICENSE`](../../LICENSE) を参照。
This repository is licensed under Apache License 2.0. See [`/LICENSE`](../../LICENSE).
