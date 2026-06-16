# 06. `.kode.json` の読み書き

> 対訳: [English](../en/06-config-kode-json.md) ／ [索引に戻る](../README.md)

`.kode.json` はプロジェクト認識結果を保持する設定ファイル。`ProjectConfigRepository` ポート（[02](02-domain-model.md)）の Infrastructure 実装が読み書きを担う。

## 1. スキーマとシリアライズ

`kotlinx.serialization` の `@Serializable` データクラスで表現する。`version` フィールドで将来のスキーマ進化に備える。

```kotlin
// 擬似コード: adapter/config/KodeConfigDto.kt
@Serializable
data class KodeConfigDto(
    val version: String = "1.0",
    val project: ProjectDto,
    val lsp: LspDto? = null,           // 任意: LSPバイナリパス上書き（04章）
)
@Serializable
data class ProjectDto(
    val root: String,
    @SerialName("build_tool") val buildTool: String,   // "gradle"
    @SerialName("kotlin_version") val kotlinVersion: String,
    @SerialName("source_dirs") val sourceDirs: List<String>,
    @SerialName("test_dirs") val testDirs: List<String>,
)
@Serializable data class LspDto(val path: String? = null)
```

JSONインスタンス（snake_case維持・整形出力）:
```kotlin
private val json = Json {
    prettyPrint = true
    encodeDefaults = true
    ignoreUnknownKeys = true   // 前方互換: 未知フィールドは無視
}
```

出力例:
```json
{
  "version": "1.0",
  "project": {
    "root": "/path/to/project",
    "build_tool": "gradle",
    "kotlin_version": "2.1.0",
    "source_dirs": ["src/main/kotlin"],
    "test_dirs": ["src/test/kotlin"]
  }
}
```

## 2. 読み込み（load）

`cwd` から上方向（親ディレクトリ）へ `.kode.json` を探索し、**最初に見つかったもの** を採用する（モノレポ／サブディレクトリからの実行に対応）。

```kotlin
// 擬似コード
fun load(start: ProjectRoot): KotlinProject? {
    val file = findUpwards(start, fileName = ".kode.json") ?: return null
    val dto = try { json.decodeFromString<KodeConfigDto>(file.readText()) }
              catch (e: SerializationException) { throw InvalidConfigException(file, e) }
    requireSchema(dto.version)          // 互換性チェック
    return dto.toDomain()
}
```

- **欠損**（ファイルが見つからない）: `null` を返す。呼び出し側（UseCase）が `NO_PROJECT_CONFIG`（exit 2）として扱う（[07](07-error-handling.md)）。
- **不正**（壊れたJSON／必須欠落）: `InvalidConfigException` → `INVALID_CONFIG`（exit 2系）。`init` のやり直しを案内。
- **バージョン不整合**: `requireSchema` で将来の非互換を検出。

## 3. 書き込み（save）

`kode init` の **成功時のみ** 書き込む。途中失敗で壊れた `.kode.json` を残さないよう **アトミック書き込み**（一時ファイル→`Files.move(..., ATOMIC_MOVE)`）を行う。

```kotlin
// 擬似コード
fun save(project: KotlinProject) {
    val target = project.root.resolve(".kode.json")
    val tmp = Files.createTempFile(target.parent, ".kode", ".json.tmp")
    Files.writeString(tmp, json.encodeToString(project.toDto()))
    try { Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE) }
    catch (e: AtomicMoveNotSupportedException) {
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING) // フォールバック
    }
}
```

> `init` が `introspect` 段階で失敗した場合、`save` は呼ばれない＝ファイルは一切生成されない（要件「エラー時は何も生成しない」を保証、[03](03-commands.md)）。

## 4. 運用方針

- **`.gitignore` 推奨**: `root` 等が環境依存の絶対パスを含むため。
- **チーム共有は非対象**: 個人開発環境のみを想定。README/`init` の出力で `.gitignore` への追加を案内する。
- **機微情報を持たない**: パス情報のみで、トークン等は保存しない（OSS公開・共有時の安全性）。

## 5. native-image 注意

`kotlinx.serialization` はコンパイル時にシリアライザを生成するためリフレクション不要で、native-imageと相性が良い（[08](08-native-image.md)）。`@Serializable` を付け忘れた型を実行時に直列化しないよう、DTOは必ずアノテーションを付与する。
