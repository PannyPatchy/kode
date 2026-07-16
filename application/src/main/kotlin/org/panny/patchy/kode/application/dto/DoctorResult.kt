package org.panny.patchy.kode.application.dto

import org.panny.patchy.kode.domain.entity.EnvironmentReport
import org.panny.patchy.kode.domain.valueobject.LspInstallation

/**
 * Outcome of `kode doctor`. [installedNow] is non-null only when this run
 * performed an LSP installation (`--install-lsp` with no LSP resolvable).
 */
data class DoctorResult(
    val report: EnvironmentReport,
    val installedNow: LspInstallation? = null,
)
