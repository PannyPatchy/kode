package org.panny.patchy.kode.domain.entity

import org.panny.patchy.kode.domain.valueobject.CodeSnippet
import org.panny.patchy.kode.domain.valueobject.Severity
import org.panny.patchy.kode.domain.valueobject.SourcePosition

/**
 * One error/warning reported for a file (`kode errors`), including the source
 * line it points at so the AI consumer does not need to re-read the file.
 */
data class Diagnostic(
    val position: SourcePosition,
    val severity: Severity,
    val message: String,
    val snippet: CodeSnippet,
)
