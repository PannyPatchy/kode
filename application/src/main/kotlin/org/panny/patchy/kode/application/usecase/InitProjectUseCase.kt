package org.panny.patchy.kode.application.usecase

import org.panny.patchy.kode.domain.entity.KotlinProject
import org.panny.patchy.kode.domain.port.BuildModelPort
import org.panny.patchy.kode.domain.port.ProjectConfigRepository
import java.nio.file.Path

/**
 * `kode init`: recognize the project and generate `.kode.json`.
 *
 * On any failure during introspection the config is **never** written, so a
 * partial/corrupt `.kode.json` is never left behind (design ch. 03 / 07).
 */
class InitProjectUseCase(
    private val buildModel: BuildModelPort,
    private val configRepo: ProjectConfigRepository,
) {
    fun execute(cwd: Path): KotlinProject {
        val project = buildModel.introspect(cwd) // throws before any write on failure
        configRepo.save(project) // only on success
        return project
    }
}
