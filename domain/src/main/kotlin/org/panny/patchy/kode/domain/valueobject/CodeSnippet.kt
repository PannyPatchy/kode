package org.panny.patchy.kode.domain.valueobject

/**
 * A short excerpt of source code shown alongside a diagnostic or reference —
 * typically the trimmed line the finding points at. May be empty when the
 * source line could not be read.
 */
@JvmInline
value class CodeSnippet(val text: String) {
    companion object {
        val EMPTY = CodeSnippet("")
    }
}
