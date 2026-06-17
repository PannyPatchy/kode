package org.panny.patchy.kode.domain.valueobject

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.nio.file.Path

class ValueObjectTest {

    @Test
    fun `FilePath rejects absolute paths`() {
        assertThrows(IllegalArgumentException::class.java) { FilePath("/abs/path") }
    }

    @Test
    fun `FilePath rejects blank`() {
        assertThrows(IllegalArgumentException::class.java) { FilePath("  ") }
    }

    @Test
    fun `FilePath accepts a relative path`() {
        assertEquals("src/main/kotlin", FilePath("src/main/kotlin").value)
    }

    @Test
    fun `ProjectRoot rejects relative paths`() {
        assertThrows(IllegalArgumentException::class.java) { ProjectRoot(Path.of("relative")) }
    }

    @Test
    fun `BuildTool maps from id and rejects unknown`() {
        assertEquals(BuildTool.GRADLE, BuildTool.fromId("gradle"))
        assertThrows(IllegalArgumentException::class.java) { BuildTool.fromId("maven") }
    }

    @Test
    fun `KotlinVersion rejects blank`() {
        assertThrows(IllegalArgumentException::class.java) { KotlinVersion("") }
    }
}
