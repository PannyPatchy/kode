package org.panny.patchy.kode.domain.port

import org.panny.patchy.kode.domain.entity.Symbol
import org.panny.patchy.kode.domain.valueobject.FilePath
import org.panny.patchy.kode.domain.valueobject.ProjectRoot

/**
 * Driven port for `kode symbols`: the declarations found in a file. Implemented
 * in the adapter layer (`adapter.lsp`).
 */
interface SymbolPort {
    /** All symbols declared in [file] (relative to [root]), in declaration order. */
    fun symbols(root: ProjectRoot, file: FilePath): List<Symbol>
}
