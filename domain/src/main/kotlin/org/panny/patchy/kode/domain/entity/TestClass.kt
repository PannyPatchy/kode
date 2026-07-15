package org.panny.patchy.kode.domain.entity

import org.panny.patchy.kode.domain.valueobject.FilePath
import org.panny.patchy.kode.domain.valueobject.SymbolName

/**
 * A test class associated with a production target by `kode test` (naming
 * heuristics, design ch. 05 §3). [functions] are the test methods found inside
 * the class; kode discovers tests statically and never runs them.
 */
data class TestClass(
    val name: SymbolName,
    val file: FilePath,
    val functions: List<SymbolName>,
)
