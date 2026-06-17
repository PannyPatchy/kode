package org.panny.patchy.kode.adapter.fs

import org.panny.patchy.kode.domain.entity.KotlinProject
import org.panny.patchy.kode.domain.error.NoProjectRootException
import org.panny.patchy.kode.domain.error.UnsupportedBuildToolException
import org.panny.patchy.kode.domain.port.BuildModelPort
import org.panny.patchy.kode.domain.valueobject.BuildTool
import org.panny.patchy.kode.domain.valueobject.FilePath
import org.panny.patchy.kode.domain.valueobject.KotlinVersion
import org.panny.patchy.kode.domain.valueobject.ProjectRoot
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.exists
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.readText

/**
 * Lightweight, heuristic implementation of [BuildModelPort] (v1).
 *
 * The project root is the nearest ancestor that holds a `settings.gradle(.kts)`
 * file (the root of the Gradle build), so multi-module / monorepo layouts resolve
 * to the true root rather than the closest sub-module. The Kotlin version is
 * inferred from the version catalog or root build script, and the conventional
 * `src/main/kotlin` / `src/test/kotlin` source sets are aggregated across every
 * module. It does not use the Gradle Tooling API; richer introspection is deferred
 * (design ch. 05).
 */
class HeuristicBuildModelAdapter : BuildModelPort {

    override fun introspect(cwd: Path): KotlinProject {
        val root = resolveProjectRoot(cwd)
        val sourceSets = collectSourceSets(root)
        return KotlinProject(
            root = ProjectRoot(root),
            buildTool = BuildTool.GRADLE,
            kotlinVersion = detectKotlinVersion(root),
            sourceDirs = sourceSets.sources,
            testDirs = sourceSets.tests,
        )
    }

    /**
     * Walk upward from [start] until the Gradle build root (`settings.gradle(.kts)`)
     * is found. If a Maven `pom.xml` is encountered first, abort with
     * [UnsupportedBuildToolException] (out of scope). If nothing is found, abort
     * with [NoProjectRootException].
     */
    private fun resolveProjectRoot(start: Path): Path {
        var dir: Path? = start.toAbsolutePath().normalize()
        while (dir != null) {
            val current = dir
            if (SETTINGS_FILES.any { current.resolve(it).exists() }) return current
            if (current.resolve(MAVEN_FILE).exists()) throw mavenUnsupported(current)
            dir = current.parent
        }
        throw NoProjectRootException(
            details = "No settings.gradle(.kts) found from $start upward. Run kode init inside a Gradle project.",
        )
    }

    private fun mavenUnsupported(dir: Path) = UnsupportedBuildToolException(
        buildTool = "maven",
        details = "Found $MAVEN_FILE at $dir. Only Gradle is supported in v1; " +
            "Maven support is out of scope and planned for a future release.",
    )

    /**
     * Aggregate the conventional Kotlin source sets across the whole project tree,
     * skipping build outputs and hidden/VCS directories. Only directories that
     * actually exist are reported, as relative POSIX paths from [root].
     */
    private fun collectSourceSets(root: Path): SourceSets {
        val sources = sortedSetOf<String>()
        val tests = sortedSetOf<String>()
        Files.walkFileTree(
            root,
            object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    val name = dir.fileName?.toString().orEmpty()
                    val rel = root.relativize(dir).invariantSeparatorsPathString
                    return when {
                        dir != root && (name in PRUNED_DIRS || name.startsWith(".")) -> FileVisitResult.SKIP_SUBTREE
                        matches(rel, MAIN_KOTLIN) -> { sources += rel; FileVisitResult.SKIP_SUBTREE }
                        matches(rel, TEST_KOTLIN) -> { tests += rel; FileVisitResult.SKIP_SUBTREE }
                        else -> FileVisitResult.CONTINUE
                    }
                }
            },
        )
        return SourceSets(sources.map(::FilePath), tests.map(::FilePath))
    }

    private fun matches(rel: String, suffix: String): Boolean = rel == suffix || rel.endsWith("/$suffix")

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

    private data class SourceSets(val sources: List<FilePath>, val tests: List<FilePath>)

    private companion object {
        val SETTINGS_FILES = listOf("settings.gradle.kts", "settings.gradle")
        val BUILD_FILES = listOf("build.gradle.kts", "build.gradle")
        const val MAVEN_FILE = "pom.xml"
        const val MAIN_KOTLIN = "src/main/kotlin"
        const val TEST_KOTLIN = "src/test/kotlin"
        val PRUNED_DIRS = setOf("build", "node_modules")

        // kotlin = "2.1.0"  (version catalog)
        val CATALOG_KOTLIN_REGEX = Regex("""(?m)^\s*kotlin\s*=\s*"([^"]+)"""")

        // kotlin("jvm") version "2.1.0"  /  id("org.jetbrains.kotlin.jvm") version "2.1.0"
        val BUILD_SCRIPT_KOTLIN_REGEX = Regex("""kotlin(?:\([^)]*\)|[^v\n]*)\s*version\s*"([^"]+)"""")
    }
}
