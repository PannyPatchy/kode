package org.panny.patchy.kode.domain.valueobject

/**
 * A 1-based line/column position inside a source file, as exposed in `kode`'s
 * JSON output (design ch. 03). LSP positions are 0-based; adapters convert.
 */
data class SourcePosition(val line: Int, val column: Int) {
    init {
        require(line >= 1) { "line must be 1-based (was $line)" }
        require(column >= 1) { "column must be 1-based (was $column)" }
    }
}
