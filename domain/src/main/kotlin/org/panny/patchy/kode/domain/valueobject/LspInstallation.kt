package org.panny.patchy.kode.domain.valueobject

import java.nio.file.Path

/**
 * A kode-managed Kotlin LSP installation. [checksumVerified] reflects whether
 * the downloaded artifact was verified against a published checksum; it is
 * `null` when the installation was merely loaded from disk (nothing to verify).
 */
data class LspInstallation(
    val version: String,
    val launcher: Path,
    val checksumVerified: Boolean? = null,
)
