package org.panny.patchy.kode.application.dto

import org.panny.patchy.kode.domain.entity.TestClass
import org.panny.patchy.kode.domain.valueobject.SymbolName

/** Result of `kode test`: test classes associated with the target symbol. */
data class TestsResult(
    val target: SymbolName,
    val tests: List<TestClass>,
)
