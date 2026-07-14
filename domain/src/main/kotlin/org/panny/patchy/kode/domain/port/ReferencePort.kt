package org.panny.patchy.kode.domain.port

import org.panny.patchy.kode.domain.entity.Reference
import org.panny.patchy.kode.domain.valueobject.ProjectRoot
import org.panny.patchy.kode.domain.valueobject.SymbolName

/**
 * Driven port for `kode refs`: usage sites of a class/function. Implemented in
 * the adapter layer (`adapter.lsp`).
 */
interface ReferencePort {
    /**
     * References to [target] within the project at [root], excluding the
     * declaration itself. Empty when the target cannot be resolved or has no
     * usages ("empty is success", design ch. 07 §3).
     */
    fun references(root: ProjectRoot, target: SymbolName): List<Reference>
}
