# kode

AIエージェントのためのKotlinコードベース理解CLI。

**English: [README.md](README.md)**

📐 **設計書**: [`docs/design/`](docs/design/README.md)

---

## コンセプト

`kode` は、AIエージェントが Kotlin プロジェクトを効率的に理解するための CLI ツールです。LSP・Gradle・ファイルシステムなど信頼できるソースから必要な情報を集め、**JSON で一発返す**ことで、AIが `grep` / `find` を乱発したり自前の解析スクリプトを書いたりせずにコードベースを把握できるようにします。

意味解釈はAI側の責務であり、`kode` の役割は「**正確なデータを収集し、構造化してJSONで返す**」ことに限定されます。

**解決する課題:**

- AIが `grep` や `find` を乱発して非効率になる
- AIが自前でスクリプトを書いてコードベースを解析しようとしてしまう
- 本来1コマンドで返せる情報を手探りで集めている

---

## クイックスタート

```bash
# 1. CLI をビルド(JDK 21+。ビルド済みバイナリの配布は計画中)
./gradlew :app:installDist
export PATH="$PWD/app/build/install/kode/bin:$PATH"

# 2. 対象の Kotlin/Gradle プロジェクトで .kode.json を生成
cd /path/to/your/project
kode init

# 3. 環境チェック + 必要なら Kotlin LSP を自動インストール
kode doctor --install-lsp

# 4. 解析
kode tree
kode errors src/main/kotlin/Foo.kt
```

---

## MCPサーバーとして使う

`kode mcp` は kode を stdio 上の [MCP](https://modelcontextprotocol.io) サーバーとして起動し、解析コマンドをツール(`kode_tree`, `kode_errors`, `kode_symbols`, `kode_refs`, `kode_test`)として公開します。AIエージェントはシェル経由ではなくネイティブに kode を呼び出せます。

Claude Code への登録(サーバーは作業ディレクトリのプロジェクトを解析するため、プロジェクトルートで実行):

```bash
claude mcp add kode -- kode mcp
# チームで共有する場合はプロジェクトスコープで:
claude mcp add --scope project kode -- kode mcp
```

ツールの結果は対応するCLIコマンドの stdout とバイト単位で一致します。

---

## コマンド一覧

すべてのコマンドはJSONのみを出力します: 結果は **stdout**、エラーエンベロープは **stderr**、終了コードは機械可読です。

| コマンド | 説明 |
|---------|------|
| `kode init` | Gradleプロジェクトを認識し `.kode.json` を生成 |
| `kode doctor` | 環境チェック(`.kode.json`・kotlin-lsp・Java)。`--install-lsp` でLSPをダウンロード |
| `kode errors <file>` | エラー・警告+周辺スニペット(LSP) |
| `kode refs <target>` | クラス・関数への参照元一覧(LSP) |
| `kode symbols <file>` | クラス・関数・プロパティ一覧(LSP) |
| `kode test <target>` | 関連テストクラス・関数(命名規則) |
| `kode tree` | プロジェクト全体のファイルツリー |
| `kode mcp` | kode を stdio 上の MCP サーバーとして起動 |

### Kotlin LSP

`errors` / `refs` / `symbols` には [JetBrains Kotlin LSP](https://github.com/Kotlin/kotlin-lsp) バイナリが必要です。最も簡単なセットアップは:

```bash
kode doctor --install-lsp
```

これは JetBrains のスタンドアロンビルドを `~/.kode/lsp/` にダウンロードします(kode は何も同梱・再配布しません)。解決順序: 環境変数 `KODE_LSP_PATH` → `.kode.json` の `lsp.path` → `PATH` 上の `kotlin-lsp`(例: `brew install JetBrains/utils/kotlin-lsp`)→ kode 管理インストール。LSP の実行には Java 17+ ランタイムが必要で、これも `kode doctor` がチェックします。

---

## インストール

ビルド済みバイナリはまだ公開されていません(計画中 — [CHANGELOG.md](CHANGELOG.md) 参照)。現時点ではソースからビルドしてください:

```bash
git clone https://github.com/PannyPatchy/kode.git && cd kode
./gradlew :app:installDist        # JVM ランチャー: app/build/install/kode/bin/kode
# GraalVM ツールチェーンがあれば単一ネイティブバイナリも:
./gradlew :app:nativeCompile      # バイナリ: app/build/native/nativeCompile/kode
```

---

## Build & Run(開発者向け)

JDK 21+ が必要です。

```bash
./gradlew build            # コンパイル + detekt + ユニットテスト
./gradlew detekt           # 静的解析(detekt、設定は ./detekt.yml)
./gradlew test             # ユニットテスト
./gradlew koverHtmlReport  # カバレッジレポート(build/reports/kover/)

# JVM から CLI を実行:
./gradlew :app:installDist
./app/build/install/kode/bin/kode --help

# 単一ネイティブバイナリのビルド(GraalVM ツールチェーンが必要):
./gradlew :app:nativeCompile
./app/build/native/nativeCompile/kode --help
```

---

## プロジェクト構成

Clean Architecture に沿ったマルチモジュール Gradle ビルド:

```
:domain       エンティティ / 値オブジェクト / ポート(フレームワーク依存なし)
:application  ユースケース(:domain に依存)
:adapter      Clikt CLI、MCPサーバー、JSONプレゼンター、.kode.json リポジトリ、LSP、ファイルシステム
:app          Composition Root(Main.kt)+ GraalVM Native Image ビルド
```

---

## `.kode.json`

`kode init` で生成され、**`.gitignore` への追加を推奨**します(本リポジトリでは追加済み)。環境依存の絶対パスを含み、個人の開発環境向けであり、チーム共有は想定していません。

---

## 貢献

[CONTRIBUTING.md](CONTRIBUTING.md)([日本語版](CONTRIBUTING.ja.md))を参照してください。Issue・PR は英語・日本語のどちらでも歓迎します。

---

## ライセンス

Apache-2.0 — 詳細は [LICENSE](LICENSE) を参照してください。
