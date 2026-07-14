package org.panny.patchy.kode.application.usecase

import org.panny.patchy.kode.application.dto.ErrorsResult
import org.panny.patchy.kode.domain.port.DiagnosticsPort
import org.panny.patchy.kode.domain.port.ProjectConfigRepository
import java.nio.file.Path

/**
 * `kode errors <file>`: the errors/warnings currently reported for a file,
 * with surrounding snippets (design ch. 03 §3). A clean file yields an empty
 * `diagnostics` array — that is success, not an error.
 */
class AnalyzeErrorsUseCase(
    private val configRepo: ProjectConfigRepository,
    private val diagnosticsPort: DiagnosticsPort,
) {
    fun execute(cwd: Path, rawFile: String): ErrorsResult {
        val project = configRepo.loadOrThrow(cwd)
        val file = resolveFileInProject(project, cwd, rawFile)
        return ErrorsResult(file, diagnosticsPort.diagnostics(project.root, file))
    }
}
