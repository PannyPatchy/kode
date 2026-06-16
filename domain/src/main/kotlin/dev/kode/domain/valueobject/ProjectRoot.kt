package dev.kode.domain.valueobject

import java.nio.file.Path

/**
 * The absolute path to a project root. Absolute paths live only inside the domain
 * boundary; they are never serialized into `kode`'s JSON output (see [FilePath]).
 */
@JvmInline
value class ProjectRoot(val path: Path) {
    init {
        require(path.isAbsolute) { "ProjectRoot must be an absolute path: $path" }
    }

    fun resolve(relative: String): Path = path.resolve(relative)
}
