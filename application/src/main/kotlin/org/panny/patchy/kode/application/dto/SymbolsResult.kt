package org.panny.patchy.kode.application.dto

import org.panny.patchy.kode.domain.entity.Symbol
import org.panny.patchy.kode.domain.valueobject.FilePath

/** Result of `kode symbols`: a file's declarations, already classified. */
data class SymbolsResult(
    val file: FilePath,
    val classes: List<Symbol.ClassSymbol>,
    val functions: List<Symbol.FunctionSymbol>,
    val topLevelProperties: List<Symbol.PropertySymbol>,
)
