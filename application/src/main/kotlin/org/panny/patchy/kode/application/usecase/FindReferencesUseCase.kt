package org.panny.patchy.kode.application.usecase

import org.panny.patchy.kode.application.dto.RefsResult
import org.panny.patchy.kode.domain.port.ProjectConfigRepository
import org.panny.patchy.kode.domain.port.ReferencePort
import org.panny.patchy.kode.domain.service.SymbolMatcher
import java.nio.file.Path

/**
 * `kode refs <target>`: usage sites of a class/function (design ch. 03 §4).
 * The target's syntax is validated *before* the reference port runs so that a
 * malformed argument fails fast without paying the LSP startup cost.
 */
class FindReferencesUseCase(
    private val configRepo: ProjectConfigRepository,
    private val referencePort: ReferencePort,
    private val symbolMatcher: SymbolMatcher,
) {
    fun execute(cwd: Path, rawTarget: String): RefsResult {
        val project = configRepo.loadOrThrow(cwd)
        val target = symbolNameArgument(rawTarget)
        symbolMatcher.parse(target) // fail fast on malformed targets
        return RefsResult(target, referencePort.references(project.root, target))
    }
}
