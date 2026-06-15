# 09. 依存関係・ライセンス精査

> 対訳: [English](../en/09-dependencies-license.md) ／ [索引に戻る](../README.md)

本リポジトリは将来 **public OSS** として公開する予定であり、採用する依存はすべて **OSSとして公開・配布して問題ないもの** に限定する。本章は各依存のライセンスを精査し、採否理由と代替検討を記す。

> 調査時点: 2026-06。ライセンスは各プロジェクトの最新を都度確認すること（特にKotlin LSPはAlphaで変化が速い）。

## 1. 結論サマリ

| 用途 | 採用 | SPDX | 公開可否 | 判定理由 |
|------|------|------|----------|----------|
| CLI | **Clikt** | `Apache-2.0` | ✅ | 無リフレクションでnative-image相性◎ |
| JSON | **kotlinx.serialization** | `Apache-2.0` | ✅ | コンパイル時生成、native-image◎ |
| Gradle連携 | **Gradle Tooling API** | `Apache-2.0` | ✅ | 公式・安定。内包配布可 |
| LSPクライアント | **Eclipse LSP4J** | `EPL-2.0 OR EDL-1.0` | ✅ | EDL(=BSD-3)選択でパーミッシブ |
| Kotlin LSP本体 | **JetBrains Kotlin LSP** | `Apache-2.0`（**一部クローズド**） | ⚠️ **同梱しない** | 独自部分クローズド＋Alpha。外部プロセスとして利用 |
| Native Imageツール | **GraalVM CE** | `GPLv2 WITH Classpath-exception-2.0` | ✅ | 出力バイナリは非伝播 |
| Native Imageプラグイン | **org.graalvm.buildtools.native** | `Apache-2.0` | ✅ | ビルドのみで使用 |
| DI | **Pure DI（手書き）** | — | ✅ | 依存なし。native-image安全 |
| テスト | **JUnit 5 / MockK** | `EPL-2.0` / `Apache-2.0` | ✅ | テストはJVM実行、配布物に含めない |

**プロジェクト本体ライセンス: Apache-2.0**（既存 `/LICENSE` を踏襲）。

## 2. 各依存の詳細

### Clikt（Apache-2.0）
- KotlinネイティブなCLIフレームワーク。アノテーション/リフレクションに依存せず、native-imageで設定が最小で済む。
- **代替検討**:
  - *picocli*（Apache-2.0）: Java製で高機能だが、アノテーション＋リフレクション中心。native-imageでは専用処理が要り、型エラーが実行時化しやすい。→不採用。
  - *kotlinx-cli*（Apache-2.0）: 公式だが開発停滞・機能限定。→不採用。
- **結論**: Clikt を維持。

### kotlinx.serialization（Apache-2.0）
- `@Serializable` でコンパイル時にシリアライザを生成。リフレクション不要でnative-imageに最適。
- **代替検討**: Jackson/Gson（リフレクション多用、native-imageで設定増）→不採用。
- **結論**: 採用。

### Gradle Tooling API（Apache-2.0）
- Gradleプロジェクトの内省・モデル取得の公式API。`kode` バイナリに内包して配布可能。
- 留意: native-imageとの相性（[08](08-native-image.md) §5 のフォールバックで担保）。
- **結論**: 採用。

### Eclipse LSP4J（EPL-2.0 OR EDL-1.0）
- LSPのJSON-RPCフレーミング・型を提供する成熟ライブラリ。自前実装よりバグが少ない。
- **デュアルライセンス**: 受領者は **EDL-1.0（Revised BSDスタイル、パーミッシブ）** を選択可能。OSS配布に支障なし。
- **代替検討**: JSON-RPCを自前実装（依存削減だが保守コスト増・バグ温床）→初版では不採用。
- **結論**: 採用（配布時はEDL-1.0を選択する旨をNOTICEに明記）。

### JetBrains Kotlin LSP（Apache-2.0 だが一部クローズド／Alpha）⚠️
- 公式リポジトリ `Kotlin/kotlin-lsp` は Apache-2.0 だが、READMEに「IntelliJ IDEA／Fleet／Air の独自部分に基づき **partially closed-source**」と明記。ステージは **Alpha**。
- 配布形態: VS Code拡張、スタンドアロンZIP、Homebrew（`brew install JetBrains/utils/kotlin-lsp`）。
- **方針**: クローズド部分と安定性リスクを `kode` の配布物に取り込まないため、**同梱・再配布しない**。ユーザーが導入したものを **外部プロセス** として起動する（[04](04-lsp.md) §1）。これによりOSS配布の権利関係が明快になる。

### GraalVM Native Image（CE: GPLv2 + Classpath Exception）
- **ビルドツールとしてのみ** 使用。GraalVMの規約上、native-imageの **出力バイナリは「未改変のProgram」扱い** で、GPLが成果物に伝播しない。
- よって生成物 `kode` を **Apache-2.0** として配布可能。
- Oracle GraalVM（GFTC）も無償だが再配布に条件があるため、OSSビルドでは **CE** を基準とする。
- **結論**: CE採用。

### DI: Pure DI（手書きコンポジションルート）
- 外部DIフレームワークを使わず、`bootstrap` で手動配線（[01](01-architecture.md) §4）。
- 理由: native-imageでのリフレクション/動的プロキシ回避、依存削減、配線の明示化。
- **代替検討**: Koin（実行時解決でnative-imageに追加対応が必要）／Dagger（Java/KAPT, native相性は可だがCleanArchの小規模配線には過剰）→不採用。

### テスト: JUnit 5（EPL-2.0）/ MockK（Apache-2.0）
- テストはJVM上で実行し、**配布バイナリには含まれない** ため、ライセンス区分が異なっても公開物に影響なし。
- 任意で Kotest（Apache-2.0）併用可。

## 3. OSS公開チェックリスト

- [ ] すべての**配布物に含まれる**依存がパーミッシブ or コピーレフト非伝播である（上表）。
- [ ] **JetBrains Kotlin LSP本体を再配布しない**（外部プロセス利用に徹する）。
- [ ] Native Image 生成物のライセンスが Apache-2.0 と整合する（GraalVM CEのClasspath Exception確認）。
- [ ] `NOTICE` / `THIRD-PARTY-LICENSES` に各依存の著作権表記とライセンス（LSP4JはEDL-1.0選択を明記）を記載。
- [ ] `gradle/libs.versions.toml` のバージョン更新時にライセンス変更がないか確認（CIでライセンスチェックを推奨）。
- [ ] `.kode.json` 等に機微情報を含めない（[06](06-config-kode-json.md)）。

## 4. 参考（一次情報）

- Kotlin LSP: `Kotlin/kotlin-lsp`（GitHub）／ kotlinlang.org の Kotlin LSP ドキュメント。
- Clikt: ajalt.github.io/clikt。
- Eclipse EDL-1.0: eclipse.org の Eclipse Distribution License。
- GraalVM ライセンス: graalvm.org／oracle/graal の LICENSE、Native Image出力に関する条項。

> ライセンスは変わり得る。CIに **ライセンススキャン**（例: 依存のSPDX抽出と許可リスト照合）を組み込み、公開前に再検証することを推奨。
