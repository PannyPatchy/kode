package org.panny.patchy.kode.application.usecase

import org.panny.patchy.kode.application.dto.TreeResult
import org.panny.patchy.kode.domain.port.FileTreePort
import org.panny.patchy.kode.domain.port.ProjectConfigRepository
import java.nio.file.Path

/**
 * `kode tree`: walk the configured source/test directories and return the whole
 * project file tree (design ch. 03 §5). Directories that no longer exist (stale
 * `.kode.json` entries) are skipped silently.
 */
class BuildTreeUseCase(
    private val configRepo: ProjectConfigRepository,
    private val fileTree: FileTreePort,
) {
    fun execute(cwd: Path): TreeResult {
        val project = configRepo.loadOrThrow(cwd)
        val dirs = (project.sourceDirs + project.testDirs).distinct()
        val roots = dirs.mapNotNull { dir ->
            fileTree.walk(project.root, dir)?.let { TreeResult.TreeRoot(dir, it) }
        }
        return TreeResult(roots)
    }
}
