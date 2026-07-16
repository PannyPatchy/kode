package org.panny.patchy.kode.domain.valueobject

import java.nio.file.Path

/** Which step of the LSP resolution precedence produced a launcher (design ch. 04). */
enum class LspSource { ENV, CONFIG, PATH, MANAGED }

/**
 * A resolved Kotlin LSP launcher: where it was found and by which precedence
 * step. [installedVersion] is known only for [LspSource.MANAGED] installs.
 */
data class LspLocation(
    val source: LspSource,
    val path: Path,
    val installedVersion: String? = null,
)
