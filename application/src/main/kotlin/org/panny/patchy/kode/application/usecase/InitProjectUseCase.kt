package org.panny.patchy.kode.application.usecase

import org.panny.patchy.kode.application.dto.InitResult
import org.panny.patchy.kode.domain.port.BuildModelPort
import org.panny.patchy.kode.domain.port.ProjectConfigRepository
import java.nio.file.Path

/**
 * `kode init`: recognize the project and generate `.kode.json`.
 *
 * On any failure during introspection the config is **never** written, so a
 * partial/corrupt `.kode.json` is never left behind (design ch. 03 / 07).
 *
 * When a `.kode.json` already exists, the user is asked to confirm an overwrite
 * via [confirmOverwrite] — unless [force] is set, in which case the file is
 * replaced unconditionally.
 */
class InitProjectUseCase(
    private val buildModel: BuildModelPort,
    private val configRepo: ProjectConfigRepository,
) {
    /**
     * @param cwd directory to start project recognition from.
     * @param force skip the overwrite confirmation and replace any existing config.
     * @param confirmOverwrite invoked only when a config already exists and [force]
     *   is `false`; return `true` to overwrite. Never invoked otherwise.
     */
    fun execute(
        cwd: Path,
        force: Boolean = false,
        confirmOverwrite: () -> Boolean = { true },
    ): InitResult {
        val project = buildModel.introspect(cwd) // throws before any write on failure
        val alreadyExists = configRepo.existsAt(project.root)
        if (alreadyExists && !force && !confirmOverwrite()) {
            return InitResult.Aborted(project)
        }
        configRepo.save(project) // only on success / after confirmation
        return InitResult.Generated(project, overwritten = alreadyExists)
    }
}
