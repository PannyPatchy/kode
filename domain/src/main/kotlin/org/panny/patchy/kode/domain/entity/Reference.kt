package org.panny.patchy.kode.domain.entity

import org.panny.patchy.kode.domain.valueobject.CodeSnippet
import org.panny.patchy.kode.domain.valueobject.FilePath
import org.panny.patchy.kode.domain.valueobject.SourcePosition

/**
 * One usage site of a class/function found by `kode refs`. [file] is relative
 * to the project root; references outside the project are not represented.
 */
data class Reference(
    val file: FilePath,
    val position: SourcePosition,
    val snippet: CodeSnippet,
)
