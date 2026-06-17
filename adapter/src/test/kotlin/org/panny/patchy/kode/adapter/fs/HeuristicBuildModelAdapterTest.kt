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
    fun `finds root upward by build file and detects kotlin version from catalog`(@TempDir dir: Path) {
        dir.resolve("build.gradle.kts").writeText("plugins {}")
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
    fun `detects kotlin version from the build script`(@TempDir dir: Path) {
        dir.resolve("build.gradle.kts").writeText("plugins { kotlin(\"jvm\") version \"1.9.24\" }")

        val project = adapter.introspect(dir)

        assertEquals("1.9.24", project.kotlinVersion?.value)
    }

    @Test
    fun `kotlin version is null when undetectable`(@TempDir dir: Path) {
        dir.resolve("build.gradle").writeText("")

        val project = adapter.introspect(dir)

        assertNull(project.kotlinVersion)
    }

    @Test
    fun `omits source and test dirs that do not exist`(@TempDir dir: Path) {
        dir.resolve("build.gradle.kts").writeText("")
        dir.resolve("src/main/kotlin").createDirectories()
        // no src/test/kotlin

        val project = adapter.introspect(dir)

        assertEquals(listOf("src/main/kotlin"), project.sourceDirs.map { it.value })
        assertTrue(project.testDirs.isEmpty())
    }

    @Test
    fun `throws NoProjectRootException when no build file is found`(@TempDir dir: Path) {
        val empty = Files.createDirectories(dir.resolve("empty"))

        assertThrows(NoProjectRootException::class.java) { adapter.introspect(empty) }
    }

    @Test
    fun `throws UnsupportedBuildToolException for a Maven project`(@TempDir dir: Path) {
        dir.resolve("pom.xml").writeText("<project/>")

        assertThrows(UnsupportedBuildToolException::class.java) { adapter.introspect(dir) }
    }

    @Test
    fun `prefers gradle over maven when both are present`(@TempDir dir: Path) {
        dir.resolve("pom.xml").writeText("<project/>")
        dir.resolve("build.gradle.kts").writeText("")

        val project = adapter.introspect(dir)

        assertEquals(BuildTool.GRADLE, project.buildTool)
    }
}
