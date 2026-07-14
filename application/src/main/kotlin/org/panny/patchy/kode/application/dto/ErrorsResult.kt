package org.panny.patchy.kode.application.dto

import org.panny.patchy.kode.domain.entity.Diagnostic
import org.panny.patchy.kode.domain.valueobject.FilePath

/** Result of `kode errors`: the diagnostics reported for one file. */
data class ErrorsResult(
    val file: FilePath,
    val diagnostics: List<Diagnostic>,
)
