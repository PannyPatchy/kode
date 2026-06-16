package dev.kode.domain.port

import dev.kode.domain.entity.KotlinProject
import java.nio.file.Path

/**
 * Driven port for recognizing a Kotlin project from the file system / build tool.
 *
 * v1 uses a lightweight heuristic implementation (`adapter.fs`); a future
 * implementation may use the Gradle Tooling API (design ch. 05).
 */
interface BuildModelPort {
    /**
     * Locate the project root starting from [cwd] and introspect it into a
     * [KotlinProject].
     *
     * @throws dev.kode.domain.error.NoProjectRootException when no root is found.
     */
    fun introspect(cwd: Path): KotlinProject
}
