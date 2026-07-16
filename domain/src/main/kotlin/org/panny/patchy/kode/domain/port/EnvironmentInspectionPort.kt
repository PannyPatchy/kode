package org.panny.patchy.kode.domain.port

import org.panny.patchy.kode.domain.valueobject.LspLocation
import java.nio.file.Path

/** Result of probing for `.kode.json` upward from a directory. */
sealed interface ProjectConfigStatus {
    /** A valid `.kode.json` was found; [root] is the configured project root. */
    data class Found(val root: Path) : ProjectConfigStatus

    /** No `.kode.json` exists between the start directory and the filesystem root. */
    data object Missing : ProjectConfigStatus

    /** A `.kode.json` exists but could not be parsed. */
    data class Invalid(val detail: String) : ProjectConfigStatus
}

/** Result of probing for a Java runtime (required to launch the LSP). */
sealed interface JavaRuntimeStatus {
    data class Available(val majorVersion: Int) : JavaRuntimeStatus
    data object NotFound : JavaRuntimeStatus
}

/**
 * Driven port for the read-only environment probes behind `kode doctor`.
 * Implementations must not modify anything (design ch. 03: checks are pure
 * observation; only `--install-lsp` writes, via [LspInstallerPort]).
 */
interface EnvironmentInspectionPort {
    /** Probe `.kode.json` upward from [cwd]. */
    fun projectConfig(cwd: Path): ProjectConfigStatus

    /** Resolve the LSP launcher with the full precedence, or `null` when absent. */
    fun locateLsp(cwd: Path): LspLocation?

    /** Probe for a Java runtime on this machine. */
    fun javaRuntime(): JavaRuntimeStatus
}
