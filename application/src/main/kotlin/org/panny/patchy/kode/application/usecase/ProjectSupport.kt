package org.panny.patchy.kode.application.usecase

import org.panny.patchy.kode.domain.entity.KotlinProject
import org.panny.patchy.kode.domain.error.InvalidArgumentException
import org.panny.patchy.kode.domain.error.NoProjectConfigException
import org.panny.patchy.kode.domain.port.ProjectConfigRepository
import org.panny.patchy.kode.domain.valueobject.FilePath
import org.panny.patchy.kode.domain.valueobject.ProjectRoot
import org.panny.patchy.kode.domain.valueobject.SymbolName
import java.nio.file.Files
import java.nio.file.Path

/**
 * Common preprocessing shared by every command except `init` (design ch. 03 §1):
 * resolve `.kode.json` upward from the working directory or fail with
 * NO_PROJECT_CONFIG (exit 2).
 */
internal fun ProjectConfigRepository.loadOrThrow(cwd: Path): KotlinProject =
    load(ProjectRoot(cwd)) ?: throw NoProjectConfigException()

/**
 * Resolve a `<file>` CLI argument (relative to [cwd], or absolute) to a
 * root-relative [FilePath]. The file must exist and live under the project
 * root; `kode`'s JSON output never leaks paths outside the project.
 */
internal fun resolveFileInProject(project: KotlinProject, cwd: Path, raw: String): FilePath {
    val absolute = cwd.resolve(raw).normalize()
    val root = project.root.path
    val problem = when {
        raw.isBlank() -> InvalidArgumentException("File must not be blank")
        !Files.isRegularFile(absolute) -> InvalidArgumentException("File not found: $raw")
        !absolute.startsWith(root) ->
            InvalidArgumentException("File is outside the project: $raw", "Project root: $root")
        else -> null
    }
    if (problem != null) throw problem
    return FilePath(root.relativize(absolute).joinToString("/"))
}

/** Parse a `<target>` CLI argument into a [SymbolName], rejecting blanks upfront. */
internal fun symbolNameArgument(raw: String): SymbolName {
    if (raw.isBlank()) throw InvalidArgumentException("Target must not be blank")
    return SymbolName(raw)
}
