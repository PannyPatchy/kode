package org.panny.patchy.kode.domain.valueobject

/**
 * Diagnostic severity in `kode`'s vocabulary. Serialized lowercase in JSON
 * output (e.g. `"error"`, `"warning"`; design ch. 03).
 */
enum class Severity {
    ERROR,
    WARNING,
    INFORMATION,
    HINT,
}
