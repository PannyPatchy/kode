package org.panny.patchy.kode.domain.port

import org.panny.patchy.kode.domain.entity.Diagnostic
import org.panny.patchy.kode.domain.valueobject.FilePath
import org.panny.patchy.kode.domain.valueobject.ProjectRoot

/**
 * Driven port for `kode errors`: the errors/warnings currently reported for a
 * file. Implemented in the adapter layer (`adapter.lsp`).
 */
interface DiagnosticsPort {
    /** Diagnostics for [file] (relative to [root]). Empty when the file is clean. */
    fun diagnostics(root: ProjectRoot, file: FilePath): List<Diagnostic>
}
