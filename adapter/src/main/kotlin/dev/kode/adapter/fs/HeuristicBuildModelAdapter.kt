package dev.kode.adapter.fs

import dev.kode.domain.entity.KotlinProject
import dev.kode.domain.error.NoProjectRootException
import dev.kode.domain.port.BuildModelPort
import dev.kode.domain.valueobject.BuildTool
import dev.kode.domain.valueobject.FilePath
import dev.kode.domain.valueobject.KotlinVersion
import dev.kode.domain.valueobject.ProjectRoot
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * Lightweight, heuristic implementation of [BuildModelPort] (v1).
 *
 * It locates the Gradle project root by walking upward for a settings file and
 * infers the Kotlin version from the version catalog or build script. It does not
 * use the Gradle Tooling API; richer introspection is deferred (design ch. 05).
 */
class HeuristicBuildModelAdapter : BuildModelPort {

    override fun introspect(cwd: Path): KotlinProject {
        val root = findProjectRoot(cwd)
            ?: throw NoProjectRootException(
                details = "No settings.gradle(.kts) found from $cwd upward. Run kode init inside a Gradle project.",
            )
        return KotlinProject(
            root = ProjectRoot(root),
            buildTool = BuildTool.GRADLE,
            kotlinVersion = KotlinVersion(detectKotlinVersion(root)),
            sourceDirs = DEFAULT_SOURCE_DIRS.map(::FilePath),
            testDirs = DEFAULT_TEST_DIRS.map(::FilePath),
        )
    }

    private fun findProjectRoot(start: Path): Path? {
        var dir: Path? = start.toAbsolutePath().normalize()
        while (dir != null) {
            if (SETTINGS_FILES.any { dir!!.resolve(it).exists() }) return dir
            dir = dir.parent
        }
        return null
    }

    /** Best-effort Kotlin version detection; falls back to [DEFAULT_KOTLIN_VERSION]. */
    private fun detectKotlinVersion(root: Path): String =
        detectFromCatalog(root) ?: detectFromBuildScript(root) ?: DEFAULT_KOTLIN_VERSION

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
        val SETTINGS_FILES = listOf("settings.gradle.kts", "settings.gradle")
        val BUILD_FILES = listOf("build.gradle.kts", "build.gradle")
        val DEFAULT_SOURCE_DIRS = listOf("src/main/kotlin")
        val DEFAULT_TEST_DIRS = listOf("src/test/kotlin")
        const val DEFAULT_KOTLIN_VERSION = "2.1.0"

        // kotlin = "2.1.0"  (version catalog)
        val CATALOG_KOTLIN_REGEX = Regex("""(?m)^\s*kotlin\s*=\s*"([^"]+)"""")

        // kotlin("jvm") version "2.1.0"  /  id("org.jetbrains.kotlin.jvm") version "2.1.0"
        val BUILD_SCRIPT_KOTLIN_REGEX = Regex("""kotlin(?:\([^)]*\)|[^v\n]*)\s*version\s*"([^"]+)"""")
    }
}
