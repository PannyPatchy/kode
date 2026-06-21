package org.panny.patchy.kode.application.usecase

import org.panny.patchy.kode.application.dto.InitResult
import org.panny.patchy.kode.domain.port.BuildModelPort
import org.panny.patchy.kode.domain.port.ProjectConfigRepository
import org.panny.patchy.kode.domain.valueobject.KotlinVersion
import java.nio.file.Path

/**
 * `kode init`: recognize the project and generate `.kode.json`.
 *
 * On any failure during introspection the config is **never** written, so a
 * partial/corrupt `.kode.json` is never left behind (design ch. 03 / 07).
 *
 * When a `.kode.json` already exists, the user is asked to confirm an overwrite
 * via [confirmOverwrite] — unless [force] is set, in which case the file is
 * replaced unconditionally. When the Kotlin version cannot be auto-detected,
 * [resolveKotlinVersion] gets a chance to supply one (e.g. from a flag or an
 * interactive prompt).
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
     * @param resolveKotlinVersion given the auto-detected version (`null` when it
     *   could not be determined), returns the version to record. Defaults to
     *   keeping the detected value as-is.
     */
    fun execute(
        cwd: Path,
        force: Boolean = false,
        confirmOverwrite: () -> Boolean = { true },
        resolveKotlinVersion: (detected: KotlinVersion?) -> KotlinVersion? = { it },
    ): InitResult {
        val detected = buildModel.introspect(cwd) // throws before any write on failure
        val alreadyExists = configRepo.existsAt(detected.root)
        if (alreadyExists && !force && !confirmOverwrite()) {
            return InitResult.Aborted(detected)
        }
        val project = detected.copy(kotlinVersion = resolveKotlinVersion(detected.kotlinVersion))
        configRepo.save(project) // only on success / after confirmation
        return InitResult.Generated(project, overwritten = alreadyExists)
    }
}
