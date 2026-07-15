package org.panny.patchy.kode.domain.entity

import org.panny.patchy.kode.domain.valueobject.LspLocation

/** Outcome of a single `kode doctor` environment check. */
enum class CheckStatus { OK, MISSING, INVALID }

/**
 * One named environment check. [hint] carries remediation guidance for the AI
 * consumer when the check did not pass (design ch. 07: errors explain how to
 * recover, not just that something failed).
 */
data class EnvironmentCheck(
    val name: String,
    val status: CheckStatus,
    val detail: String,
    val hint: String? = null,
) {
    val passed: Boolean get() = status == CheckStatus.OK
}

/**
 * The full `kode doctor` report: every check that ran, plus the resolved LSP
 * location when one was found.
 */
data class EnvironmentReport(
    val checks: List<EnvironmentCheck>,
    val lsp: LspLocation? = null,
) {
    val ok: Boolean get() = checks.all(EnvironmentCheck::passed)
}
