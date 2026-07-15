package org.panny.patchy.kode.domain.port

import org.panny.patchy.kode.domain.valueobject.LspInstallation

/**
 * Driven port for managing the kode-managed Kotlin LSP installation.
 *
 * `kode` never bundles the LSP (design ch. 04); an installer implementation
 * downloads JetBrains' own artifact into a user-local directory on the user's
 * explicit request (`kode doctor --install-lsp`).
 */
interface LspInstallerPort {
    /** The current managed installation, or `null` when none exists. */
    fun currentInstallation(): LspInstallation?

    /**
     * Download and install the Kotlin LSP.
     *
     * @param version the LSP version to install; `null` uses the pinned default.
     * @throws org.panny.patchy.kode.domain.error.LspDownloadFailedException on
     *   download, verification, or extraction failure.
     */
    fun install(version: String? = null): LspInstallation
}
