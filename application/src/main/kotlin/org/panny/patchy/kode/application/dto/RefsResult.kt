package org.panny.patchy.kode.application.dto

import org.panny.patchy.kode.domain.entity.Reference
import org.panny.patchy.kode.domain.valueobject.SymbolName

/** Result of `kode refs`: usage sites of the target symbol. */
data class RefsResult(
    val target: SymbolName,
    val refs: List<Reference>,
)
