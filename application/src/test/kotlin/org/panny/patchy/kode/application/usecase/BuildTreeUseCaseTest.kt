package org.panny.patchy.kode.application.usecase

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.panny.patchy.kode.domain.entity.FileTreeNode
import org.panny.patchy.kode.domain.entity.KotlinProject
import org.panny.patchy.kode.domain.error.NoProjectConfigException
import org.panny.patchy.kode.domain.port.FileTreePort
import org.panny.patchy.kode.domain.port.ProjectConfigRepository
import org.panny.patchy.kode.domain.valueobject.BuildTool
import org.panny.patchy.kode.domain.valueobject.FilePath
import org.panny.patchy.kode.domain.valueobject.ProjectRoot
import java.nio.file.Path

class BuildTreeUseCaseTest {

    private val configRepo = mockk<ProjectConfigRepository>()
    private val fileTree = mockk<FileTreePort>()
    private val useCase = BuildTreeUseCase(configRepo, fileTree)

    private val cwd: Path = Path.of("/tmp/project")
    private val sourceDir = FilePath("src/main/kotlin")
    private val testDir = FilePath("src/test/kotlin")
    private val project = KotlinProject(
        root = ProjectRoot(cwd),
        buildTool = BuildTool.GRADLE,
        kotlinVersion = null,
        sourceDirs = listOf(sourceDir),
        testDirs = listOf(testDir),
    )

    @Test
    fun `walks each configured directory and skips missing ones`() {
        val sourceTree = FileTreeNode.Directory("kotlin", listOf(FileTreeNode.File("Foo.kt")))
        every { configRepo.load(project.root) } returns project
        every { fileTree.walk(project.root, sourceDir) } returns sourceTree
        every { fileTree.walk(project.root, testDir) } returns null // stale entry

        val result = useCase.execute(cwd)

        assertEquals(1, result.roots.size)
        assertEquals(sourceDir, result.roots[0].dir)
        assertEquals(sourceTree, result.roots[0].node)
    }

    @Test
    fun `deduplicates directories listed as both source and test`() {
        val shared = FilePath("src")
        val sharedProject = project.copy(sourceDirs = listOf(shared), testDirs = listOf(shared))
        every { configRepo.load(project.root) } returns sharedProject
        every { fileTree.walk(project.root, shared) } returns FileTreeNode.Directory("src", emptyList())

        val result = useCase.execute(cwd)

        assertEquals(1, result.roots.size)
    }

    @Test
    fun `fails with NO_PROJECT_CONFIG when no config is found`() {
        every { configRepo.load(project.root) } returns null

        assertThrows(NoProjectConfigException::class.java) { useCase.execute(cwd) }
    }
}
