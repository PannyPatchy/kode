package org.panny.patchy.kode.adapter.fs

import org.panny.patchy.kode.domain.entity.KotlinProject
import org.panny.patchy.kode.domain.error.NoProjectRootException
import org.panny.patchy.kode.domain.error.UnsupportedBuildToolException
import org.panny.patchy.kode.domain.port.BuildModelPort
import org.panny.patchy.kode.domain.valueobject.BuildTool
import org.panny.patchy.kode.domain.valueobject.FilePath
import org.panny.patchy.kode.domain.valueobject.KotlinVersion
import org.panny.patchy.kode.domain.valueobject.ProjectRoot
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.readText

/**
 * Lightweight, heuristic implementation of [BuildModelPort] (v1).
 *
 * It locates the Gradle project root by walking upward for a `build.gradle(.kts)`
 * file and infers the Kotlin version from the version catalog or build script.
 * It does not use the Gradle Tooling API; richer introspection is deferred
 * (design ch. 05).
 */
class HeuristicBuildModelAdapter : BuildModelPort {

    override fun introspect(cwd: Path): KotlinProject {
        val root = resolveProjectRoot(cwd)
        return KotlinProject(
            root = ProjectRoot(root),
            buildTool = BuildTool.GRADLE,
            kotlinVersion = detectKotlinVersion(root),
            sourceDirs = existingDirs(root, DEFAULT_SOURCE_DIRS),
            testDirs = existingDirs(root, DEFAULT_TEST_DIRS),
        )
    }

    /**
     * Walk upward from [start] until a Gradle build file is found. If a Maven
     * `pom.xml` is encountered first, abort with [UnsupportedBuildToolException]
     * (out of scope). If nothing is found, abort with [NoProjectRootException].
     */
    private fun resolveProjectRoot(start: Path): Path {
        var dir: Path? = start.toAbsolutePath().normalize()
        while (dir != null) {
            val current = dir
            if (BUILD_FILES.any { current.resolve(it).exists() }) return current
            if (current.resolve(MAVEN_FILE).exists()) throw mavenUnsupported(current)
            dir = current.parent
        }
        throw NoProjectRootException(
            details = "No build.gradle(.kts) found from $start upward. Run kode init inside a Gradle project.",
        )
    }

    private fun mavenUnsupported(dir: Path) = UnsupportedBuildToolException(
        buildTool = "maven",
        details = "Found $MAVEN_FILE at $dir. Only Gradle is supported in v1; " +
            "Maven support is out of scope and planned for a future release.",
    )

    /** Keep only the conventional [candidates] that actually exist as directories. */
    private fun existingDirs(root: Path, candidates: List<String>): List<FilePath> =
        candidates.filter { root.resolve(it).isDirectory() }.map(::FilePath)

    /** Best-effort Kotlin version detection; `null` when it cannot be determined. */
    private fun detectKotlinVersion(root: Path): KotlinVersion? =
        (detectFromCatalog(root) ?: detectFromBuildScript(root))?.let(::KotlinVersion)

    private fun detectFromCatalog(root: Path): String? =
        root.resolve("gradle/libs.versions.toml")
            .takeIf { it.exists() }
            ?.let { kotlinVersionFrom(it.readText(), CATALOG_KOTLIN_REGEX) }

    private fun detectFromBuildScript(root: Path): String? =
        BUILD_FILES
            .map { root.resolve(it) }
            .firstOrNull { it.exists() }
            ?.let { kotlinVersionFrom(it.readText(), BUILD_SCRIPT_KOTLIN_REGEX) }

    private fun kotlinVersionFrom(text: String, regex: Regex): String? =
        regex.find(text)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }

    private companion object {
        val BUILD_FILES = listOf("build.gradle.kts", "build.gradle")
        const val MAVEN_FILE = "pom.xml"
        val DEFAULT_SOURCE_DIRS = listOf("src/main/kotlin")
        val DEFAULT_TEST_DIRS = listOf("src/test/kotlin")

        // kotlin = "2.1.0"  (version catalog)
        val CATALOG_KOTLIN_REGEX = Regex("""(?m)^\s*kotlin\s*=\s*"([^"]+)"""")

        // kotlin("jvm") version "2.1.0"  /  id("org.jetbrains.kotlin.jvm") version "2.1.0"
        val BUILD_SCRIPT_KOTLIN_REGEX = Regex("""kotlin(?:\([^)]*\)|[^v\n]*)\s*version\s*"([^"]+)"""")
    }
}
