# 00. 概要

> 対訳: [English](../en/00-overview.md) ／ [索引に戻る](../README.md)

## 1. コンセプト

**AIエージェントのためのKotlinコードベース理解CLI `kode`。**

AIが `grep` / `find` を乱発する代わりに、LSP・Gradle Tooling API など複数ソースから必要な情報を **JSONで一発返す** ツール。意味解釈はAI側の責務であり、このツールの役割ではない。

`kode` の責務は「**正確なデータを集め、構造化してJSONで返す**」ことに限定される。何が正しい修正か、どのテストを足すべきかといった判断はしない。

## 2. 解決する課題

- AIが `grep` や `find` を乱発して非効率。
- AIが自前でPython等のスクリプトを書いて頑張ってしまう。
- 本来1コマンドで返せる情報を手探りで集めている。

## 3. ユーザーストーリー

| シーン | AIが詰まるポイント | kodeが解決すること |
|--------|--------------------|----------------------|
| バグ修正 | エラー箇所の型情報・影響範囲が掴めない | エラー＋周辺スニペットを一括返却（`kode errors`） |
| リファクタリング | 参照元・影響テストを網羅的に把握できない | 参照元を返却（`kode refs`）・関連テストを返却（`kode test`） |
| 新機能追加 | 既存コードの構造把握に grep を乱発 | ツリー構造・シンボル一覧を返却（`kode tree` / `kode symbols`） |
| テスト追加 | 関連テストを探すのが辛い | 関連テストクラス・関数を返却（`kode test`） |

## 4. 設計方針

- 出力は **常に JSON**（AI向け）。人間向け装飾はしない。
- **意味解釈はしない**。データ提供までが責務。
- `kode --help` の出力がそのまま **AIへの説明書** になる設計（Cliktの自動ヘルプを各コマンドのJSONスキーマ説明に活用）。
- **Clean Architecture / DDD** を採用し、ドメインを外部技術（LSP/Gradle/FS）から隔離する（[01](01-architecture.md) / [02](02-domain-model.md)）。
- **OSS公開前提**。依存はパーミッシブまたはコピーレフト非伝播のみ（[09](09-dependencies-license.md)）。

## 5. コマンド一覧

| コマンド | 概要 | 主な情報源 |
|----------|------|------------|
| `kode init` | プロジェクト認識・`.kode.json` 生成 | Gradle Tooling API |
| `kode errors <file>` | エラー・警告＋周辺スニペット | LSP |
| `kode refs <class/function>` | 参照元一覧 | LSP |
| `kode test <class/function>` | 関連テストクラス・関数 | Gradle Tooling API |
| `kode tree` | プロジェクト全体のファイルツリー | FileSystem |
| `kode symbols <file>` | クラス・関数・プロパティ一覧 | LSP |

各コマンドの入出力スキーマと処理フローは [03. 各コマンドの処理フロー](03-commands.md) を参照。

## 6. 技術スタック

| 用途 | 技術 | ライセンス |
|------|------|-----------|
| 言語 | Kotlin | Apache-2.0 |
| CLIフレームワーク | Clikt | Apache-2.0 |
| JSON | kotlinx.serialization | Apache-2.0 |
| コード解析 | JetBrains公式 Kotlin Language Server（外部プロセス） | Apache-2.0（一部クローズド） |
| LSPクライアント | Eclipse LSP4J | EPL-2.0 / EDL-1.0 |
| Gradle連携 | Gradle Tooling API | Apache-2.0 |
| 配布 | GraalVM Native Image（単一バイナリ） | GPLv2+CE（toolchain） |

詳細とライセンス精査は [09. 依存関係・ライセンス精査](09-dependencies-license.md)。

## 7. プロジェクト認識（`.kode.json`）

- `kode init` で生成。
- 環境依存のため `.gitignore` 推奨。
- チーム共有は想定しない（個人開発環境のみ）。

詳細は [06. .kode.json の読み書き](06-config-kode-json.md)。

## 8. スコープ外

- Kotlin以外の言語対応。
- Maven対応（将来対応）。
- チーム共有・クラウド連携。
- 意味解釈・判断（AIの責務）。
- カバレッジ取得。
