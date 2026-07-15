package org.panny.patchy.kode.adapter.fs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.panny.patchy.kode.domain.entity.FileTreeNode
import org.panny.patchy.kode.domain.valueobject.FilePath
import org.panny.patchy.kode.domain.valueobject.ProjectRoot
import java.nio.file.Files
import java.nio.file.Path

class NioFileTreeAdapterTest {

    @TempDir
    lateinit var root: Path

    private val adapter = NioFileTreeAdapter()

    @Test
    fun `walks a directory recursively, files first, sorted by name`() {
        val base = root.resolve("src/main/kotlin/com/example")
        Files.createDirectories(base.resolve("service"))
        Files.writeString(base.resolve("Foo.kt"), "")
        Files.writeString(base.resolve("Bar.kt"), "")
        Files.writeString(base.resolve("service/FooService.kt"), "")

        val tree = adapter.walk(ProjectRoot(root), FilePath("src/main/kotlin"))

        val expected = FileTreeNode.Directory(
            name = "kotlin",
            children = listOf(
                FileTreeNode.Directory(
                    name = "com",
                    children = listOf(
                        FileTreeNode.Directory(
                            name = "example",
                            children = listOf(
                                FileTreeNode.File("Bar.kt"),
                                FileTreeNode.File("Foo.kt"),
                                FileTreeNode.Directory(
                                    name = "service",
                                    children = listOf(FileTreeNode.File("FooService.kt")),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
        assertEquals(expected, tree)
    }

    @Test
    fun `skips hidden entries`() {
        val base = root.resolve("src")
        Files.createDirectories(base.resolve(".hidden"))
        Files.writeString(base.resolve(".DS_Store"), "")
        Files.writeString(base.resolve("Visible.kt"), "")

        val tree = adapter.walk(ProjectRoot(root), FilePath("src"))

        assertEquals(
            FileTreeNode.Directory("src", listOf(FileTreeNode.File("Visible.kt"))),
            tree,
        )
    }

    @Test
    fun `returns null for a missing directory`() {
        assertNull(adapter.walk(ProjectRoot(root), FilePath("does/not/exist")))
    }
}
