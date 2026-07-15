package org.panny.patchy.kode.adapter.lsp

import org.panny.patchy.kode.domain.error.LspNotFoundException
import org.panny.patchy.kode.domain.valueobject.LspLocation
import org.panny.patchy.kode.domain.valueobject.LspSource
import java.nio.file.Files
import java.nio.file.Path

/**
 * Resolves the Kotlin LSP launcher path. `kode` never bundles the LSP; it is
 * located from the environment per the precedence in design ch. 04:
 *
 * 1. `KODE_LSP_PATH` environment variable
 * 2. The optional `lsp.path` field in `.kode.json` (passed via [configuredPath])
 * 3. `kotlin-lsp` discovered on the `PATH`
 * 4. The kode-managed install under `~/.kode/lsp` (`kode doctor --install-lsp`)
 *
 * The managed install is last so explicit user configuration always wins. If
 * none resolve to an existing, executable file an [LspNotFoundException] with
 * install guidance is thrown (design ch. 07).
 *
 * The OS interactions (env vars, `PATH`, managed-install lookup) are injected
 * so the resolver is unit testable without touching the real environment.
 */
class LspBinaryResolver(
    private val env: (String) -> String? = System::getenv,
    private val pathEntries: () -> List<Path> = ::defaultPathEntries,
    private val managedLauncher: () -> Path? = { LspInstallStore().load()?.launcher },
) {
    /**
     * @param configuredPath the `lsp.path` value from `.kode.json`, if any.
     * @return the first existing, executable candidate with its source, or `null`.
     */
    fun locate(configuredPath: String? = null): LspLocation? {
        val candidates = buildList {
            env(ENV_VAR)?.takeIf { it.isNotBlank() }?.let { add(LspLocation(LspSource.ENV, Path.of(it))) }
            configuredPath?.takeIf { it.isNotBlank() }?.let { add(LspLocation(LspSource.CONFIG, Path.of(it))) }
            addAll(pathEntries().map { LspLocation(LspSource.PATH, it.resolve(BINARY_NAME)) })
            managedLauncher()?.let { add(LspLocation(LspSource.MANAGED, it)) }
        }
        return candidates.firstOrNull { isExecutableFile(it.path) }
    }

    /**
     * @param configuredPath the `lsp.path` value from `.kode.json`, if any.
     * @return the absolute path to an existing, executable LSP launcher.
     * @throws LspNotFoundException when no candidate resolves.
     */
    fun resolve(configuredPath: String? = null): Path =
        locate(configuredPath)?.path ?: throw LspNotFoundException(details = NOT_FOUND_HINT)

    private fun isExecutableFile(path: Path): Boolean =
        Files.isRegularFile(path) && Files.isExecutable(path)

    companion object {
        const val ENV_VAR: String = "KODE_LSP_PATH"
        const val BINARY_NAME: String = "kotlin-lsp"

        private const val NOT_FOUND_HINT: String =
            "Run 'kode doctor --install-lsp' to download it, set $ENV_VAR, add lsp.path to " +
                ".kode.json, or install via 'brew install JetBrains/utils/kotlin-lsp'."

        private fun defaultPathEntries(): List<Path> =
            (System.getenv("PATH") ?: "")
                .split(java.io.File.pathSeparatorChar)
                .filter { it.isNotBlank() }
                .map { Path.of(it) }
    }
}
