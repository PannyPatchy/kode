package org.panny.patchy.kode.application.usecase

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.panny.patchy.kode.domain.entity.KotlinProject
import org.panny.patchy.kode.domain.error.InvalidArgumentException
import org.panny.patchy.kode.domain.valueobject.BuildTool
import org.panny.patchy.kode.domain.valueobject.FilePath
import org.panny.patchy.kode.domain.valueobject.ProjectRoot
import java.nio.file.Files
import java.nio.file.Path

class ProjectSupportTest {

    @TempDir
    lateinit var root: Path

    private fun project() = KotlinProject(
        root = ProjectRoot(root),
        buildTool = BuildTool.GRADLE,
        kotlinVersion = null,
        sourceDirs = listOf(FilePath("src/main/kotlin")),
        testDirs = listOf(FilePath("src/test/kotlin")),
    )

    @Test
    fun `resolves a relative file against cwd to a root-relative path`() {
        val file = root.resolve("src/main/kotlin/Foo.kt")
        Files.createDirectories(file.parent)
        Files.writeString(file, "class Foo")

        val resolved = resolveFileInProject(project(), root, "src/main/kotlin/Foo.kt")

        assertEquals(FilePath("src/main/kotlin/Foo.kt"), resolved)
    }

    @Test
    fun `resolves from a subdirectory cwd`() {
        val file = root.resolve("src/main/kotlin/Foo.kt")
        Files.createDirectories(file.parent)
        Files.writeString(file, "class Foo")

        val resolved = resolveFileInProject(project(), root.resolve("src"), "main/kotlin/Foo.kt")

        assertEquals(FilePath("src/main/kotlin/Foo.kt"), resolved)
    }

    @Test
    fun `rejects a missing file`() {
        assertThrows(InvalidArgumentException::class.java) {
            resolveFileInProject(project(), root, "src/main/kotlin/Missing.kt")
        }
    }

    @Test
    fun `rejects a file outside the project root`() {
        val outside = Files.createTempFile("outside", ".kt")
        try {
            assertThrows(InvalidArgumentException::class.java) {
                resolveFileInProject(project(), root, outside.toString())
            }
        } finally {
            Files.deleteIfExists(outside)
        }
    }

    @Test
    fun `rejects a blank target`() {
        assertThrows(InvalidArgumentException::class.java) { symbolNameArgument("  ") }
    }
}
