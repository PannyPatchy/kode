package org.panny.patchy.kode.adapter.fs

import org.panny.patchy.kode.domain.error.NoProjectRootException
import org.panny.patchy.kode.domain.error.UnsupportedBuildToolException
import org.panny.patchy.kode.domain.valueobject.BuildTool
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class HeuristicBuildModelAdapterTest {

    private val adapter = HeuristicBuildModelAdapter()

    @Test
    fun `finds the settings root upward and detects kotlin version from catalog`(@TempDir dir: Path) {
        dir.resolve("settings.gradle.kts").writeText("rootProject.name = \"demo\"")
        dir.resolve("gradle").createDirectories()
        dir.resolve("gradle/libs.versions.toml").writeText("[versions]\nkotlin = \"2.0.20\"\n")
        dir.resolve("src/main/kotlin").createDirectories()
        dir.resolve("src/test/kotlin").createDirectories()
        val sub = dir.resolve("sub/nested").also { it.createDirectories() }

        val project = adapter.introspect(sub)

        assertEquals(dir.toRealPath(), project.root.path.toRealPath())
        assertEquals(BuildTool.GRADLE, project.buildTool)
        assertEquals("2.0.20", project.kotlinVersion?.value)
        assertEquals(listOf("src/main/kotlin"), project.sourceDirs.map { it.value })
        assertEquals(listOf("src/test/kotlin"), project.testDirs.map { it.value })
    }

    @Test
    fun `aggregates source sets across modules and resolves to the true root`(@TempDir dir: Path) {
        dir.resolve("settings.gradle.kts").writeText("include(\":domain\", \":app\")")
        dir.resolve("build.gradle.kts").writeText("") // root has no sources of its own
        dir.resolve("domain/src/main/kotlin").createDirectories()
        dir.resolve("domain/src/test/kotlin").createDirectories()
        dir.resolve("app/src/main/kotlin").createDirectories()
        // build output below a module must be pruned, not reported
        dir.resolve("app/build/src/main/kotlin").createDirectories()
        val sub = dir.resolve("domain/src/main/kotlin").also { it.createDirectories() }

        val project = adapter.introspect(sub)

        assertEquals(dir.toRealPath(), project.root.path.toRealPath())
        assertEquals(listOf("app/src/main/kotlin", "domain/src/main/kotlin"), project.sourceDirs.map { it.value })
        assertEquals(listOf("domain/src/test/kotlin"), project.testDirs.map { it.value })
    }

    @Test
    fun `detects kotlin version from the root build script`(@TempDir dir: Path) {
        dir.resolve("settings.gradle.kts").writeText("")
        dir.resolve("build.gradle.kts").writeText("plugins { kotlin(\"jvm\") version \"1.9.24\" }")

        val project = adapter.introspect(dir)

        assertEquals("1.9.24", project.kotlinVersion?.value)
    }

    @Test
    fun `kotlin version is null when undetectable`(@TempDir dir: Path) {
        dir.resolve("settings.gradle").writeText("")

        val project = adapter.introspect(dir)

        assertNull(project.kotlinVersion)
    }

    @Test
    fun `omits source and test dirs that do not exist`(@TempDir dir: Path) {
        dir.resolve("settings.gradle.kts").writeText("")
        dir.resolve("src/main/kotlin").createDirectories()
        // no src/test/kotlin

        val project = adapter.introspect(dir)

        assertEquals(listOf("src/main/kotlin"), project.sourceDirs.map { it.value })
        assertTrue(project.testDirs.isEmpty())
    }

    @Test
    fun `throws NoProjectRootException when no settings file is found`(@TempDir dir: Path) {
        val empty = Files.createDirectories(dir.resolve("empty"))

        assertThrows(NoProjectRootException::class.java) { adapter.introspect(empty) }
    }

    @Test
    fun `throws UnsupportedBuildToolException for a Maven project`(@TempDir dir: Path) {
        dir.resolve("pom.xml").writeText("<project/>")

        assertThrows(UnsupportedBuildToolException::class.java) { adapter.introspect(dir) }
    }

    @Test
    fun `prefers the gradle settings root over a maven file at the same level`(@TempDir dir: Path) {
        dir.resolve("pom.xml").writeText("<project/>")
        dir.resolve("settings.gradle.kts").writeText("")

        val project = adapter.introspect(dir)

        assertEquals(BuildTool.GRADLE, project.buildTool)
    }
}
