# 06. Reading & Writing `.kode.json`

> Translation: [日本語](../ja/06-config-kode-json.md) ／ [Back to index](../README.md)

`.kode.json` holds the project-recognition result. The Infrastructure implementation of the `ProjectConfigRepository` port ([02](02-domain-model.md)) handles reading and writing.

## 1. Schema and Serialization

Represented as `@Serializable` data classes using `kotlinx.serialization`. The `version` field prepares for future schema evolution.

```kotlin
// pseudo-code: adapter/config/KodeConfigDto.kt
@Serializable
data class KodeConfigDto(
    val version: String = "1.0",
    val project: ProjectDto,
    val lsp: LspDto? = null,           // optional: override LSP binary path (ch. 04)
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

JSON instance (keep snake_case, pretty output):
```kotlin
private val json = Json {
    prettyPrint = true
    encodeDefaults = true
    ignoreUnknownKeys = true   // forward-compat: ignore unknown fields
}
```

Output example:
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

## 2. Loading

Search upward from `cwd` (parent directories) for `.kode.json` and use the **first one found** (supports running from a subdirectory / monorepo).

```kotlin
// pseudo-code
fun load(start: ProjectRoot): KotlinProject? {
    val file = findUpwards(start, fileName = ".kode.json") ?: return null
    val dto = try { json.decodeFromString<KodeConfigDto>(file.readText()) }
              catch (e: SerializationException) { throw InvalidConfigException(file, e) }
    requireSchema(dto.version)          // compatibility check
    return dto.toDomain()
}
```

- **Missing** (file not found): return `null`. The caller (use case) treats it as `NO_PROJECT_CONFIG` (exit 2) ([07](07-error-handling.md)).
- **Invalid** (corrupt JSON / missing required fields): `InvalidConfigException` → `INVALID_CONFIG` (exit-2 family). Advise re-running `init`.
- **Version mismatch**: `requireSchema` detects future incompatibilities.

## 3. Saving

Write **only on success** of `kode init`. To avoid leaving a corrupt `.kode.json` on partial failure, use an **atomic write** (temp file → `Files.move(..., ATOMIC_MOVE)`).

```kotlin
// pseudo-code
fun save(project: KotlinProject) {
    val target = project.root.resolve(".kode.json")
    val tmp = Files.createTempFile(target.parent, ".kode", ".json.tmp")
    Files.writeString(tmp, json.encodeToString(project.toDto()))
    try { Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE) }
    catch (e: AtomicMoveNotSupportedException) {
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING) // fallback
    }
}
```

> If `init` fails at the `introspect` stage, `save` is never called — no file is generated at all (guaranteeing the requirement "generate nothing on error", [03](03-commands.md)).

## 4. Operational Policy

- **Recommended to `.gitignore`**: `root` and friends contain environment-dependent absolute paths.
- **Not for team sharing**: assumes a personal dev environment only. The README / `init` output advises adding it to `.gitignore`.
- **No sensitive data**: only path information; no tokens are stored (safe for OSS publication/sharing).

## 5. native-image Note

`kotlinx.serialization` generates serializers at compile time, so no reflection is needed and it pairs well with native-image ([08](08-native-image.md)). To avoid serializing a type at runtime that forgot `@Serializable`, always annotate DTOs.
