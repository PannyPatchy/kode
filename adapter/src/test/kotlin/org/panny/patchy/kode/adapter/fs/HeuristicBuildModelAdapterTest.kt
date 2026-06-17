package org.panny.patchy.kode.adapter.fs

import org.panny.patchy.kode.domain.error.NoProjectRootException
import org.panny.patchy.kode.domain.valueobject.BuildTool
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class HeuristicBuildModelAdapterTest {

    private val adapter = HeuristicBuildModelAdapter()

    @Test
    fun `finds root upward and detects kotlin version from version catalog`(@TempDir dir: Path) {
        dir.resolve("settings.gradle.kts").writeText("rootProject.name = \"demo\"")
        dir.resolve("gradle").createDirectories()
        dir.resolve("gradle/libs.versions.toml").writeText("[versions]\nkotlin = \"2.0.20\"\n")
        val sub = dir.resolve("sub/nested").also { it.createDirectories() }

        val project = adapter.introspect(sub)

        assertEquals(dir.toRealPath(), project.root.path.toRealPath())
        assertEquals(BuildTool.GRADLE, project.buildTool)
        assertEquals("2.0.20", project.kotlinVersion.value)
        assertEquals(listOf("src/main/kotlin"), project.sourceDirs.map { it.value })
        assertEquals(listOf("src/test/kotlin"), project.testDirs.map { it.value })
    }

    @Test
    fun `falls back to default kotlin version when undetectable`(@TempDir dir: Path) {
        dir.resolve("settings.gradle").writeText("")

        val project = adapter.introspect(dir)

        assertEquals("2.1.0", project.kotlinVersion.value)
    }

    @Test
    fun `throws NoProjectRootException when no settings file is found`(@TempDir dir: Path) {
        val empty = Files.createDirectories(dir.resolve("empty"))

        assertThrows(NoProjectRootException::class.java) { adapter.introspect(empty) }
    }
}
