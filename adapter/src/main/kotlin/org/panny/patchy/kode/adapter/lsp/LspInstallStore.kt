package org.panny.patchy.kode.adapter.lsp

import org.panny.patchy.kode.adapter.config.kodeJson
import org.panny.patchy.kode.domain.valueobject.LspInstallation
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * The kode home directory (`~/.kode` by default, `KODE_HOME` to override —
 * useful for tests and sandboxed environments).
 */
fun kodeHome(env: (String) -> String? = System::getenv): Path =
    env("KODE_HOME")?.takeIf { it.isNotBlank() }?.let(Path::of)
        ?: Path.of(System.getProperty("user.home"), ".kode")

/** On-disk record of the managed LSP install (`~/.kode/lsp/installed.json`). */
@Serializable
internal data class LspInstallRecordDto(val version: String, val launcher: String)

/**
 * Persists which kode-managed Kotlin LSP is installed and where its launcher
 * lives, so resolution is a single file read instead of a directory scan.
 * A corrupt or stale record (launcher gone) is treated as "not installed".
 */
class LspInstallStore(private val home: Path = kodeHome()) {

    private val recordFile: Path get() = home.resolve(LSP_DIR).resolve(RECORD_FILE_NAME)

    /** Directory a given LSP version is extracted into. */
    fun lspDir(version: String): Path = home.resolve(LSP_DIR).resolve(version)

    fun load(): LspInstallation? {
        val dto = recordFile.takeIf { Files.isRegularFile(it) }?.let(::parseRecord) ?: return null
        val launcher = Path.of(dto.launcher)
        val usable = Files.isRegularFile(launcher) && Files.isExecutable(launcher)
        return if (usable) LspInstallation(dto.version, launcher) else null
    }

    private fun parseRecord(file: Path): LspInstallRecordDto? = try {
        kodeJson.decodeFromString<LspInstallRecordDto>(Files.readString(file))
    } catch (_: SerializationException) {
        null
    }

    /** Write the record atomically (same temp-file + move pattern as `.kode.json`). */
    fun save(installation: LspInstallation) {
        Files.createDirectories(recordFile.parent)
        val dto = LspInstallRecordDto(installation.version, installation.launcher.toString())
        val tmp = Files.createTempFile(recordFile.parent, ".installed", ".json.tmp")
        Files.writeString(tmp, kodeJson.encodeToString(dto))
        try {
            Files.move(tmp, recordFile, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(tmp, recordFile, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    companion object {
        private const val LSP_DIR = "lsp"
        private const val RECORD_FILE_NAME = "installed.json"
    }
}
