package org.panny.patchy.kode.application.usecase

import org.panny.patchy.kode.application.dto.SymbolsResult
import org.panny.patchy.kode.domain.entity.Symbol
import org.panny.patchy.kode.domain.port.ProjectConfigRepository
import org.panny.patchy.kode.domain.port.SymbolPort
import java.nio.file.Path

/**
 * `kode symbols <file>`: list a file's declarations, classified into classes /
 * functions / top-level properties (design ch. 03 §6).
 */
class ListSymbolsUseCase(
    private val configRepo: ProjectConfigRepository,
    private val symbolPort: SymbolPort,
) {
    fun execute(cwd: Path, rawFile: String): SymbolsResult {
        val project = configRepo.loadOrThrow(cwd)
        val file = resolveFileInProject(project, cwd, rawFile)
        val symbols = symbolPort.symbols(project.root, file)
        return SymbolsResult(
            file = file,
            classes = symbols.filterIsInstance<Symbol.ClassSymbol>(),
            functions = symbols.filterIsInstance<Symbol.FunctionSymbol>(),
            topLevelProperties = symbols.filterIsInstance<Symbol.PropertySymbol>(),
        )
    }
}
