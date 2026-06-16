package dev.kode.application.usecase

import dev.kode.domain.entity.KotlinProject
import dev.kode.domain.error.NoProjectRootException
import dev.kode.domain.port.BuildModelPort
import dev.kode.domain.port.ProjectConfigRepository
import dev.kode.domain.valueobject.BuildTool
import dev.kode.domain.valueobject.FilePath
import dev.kode.domain.valueobject.KotlinVersion
import dev.kode.domain.valueobject.ProjectRoot
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.nio.file.Path

class InitProjectUseCaseTest {

    private val buildModel = mockk<BuildModelPort>()
    private val configRepo = mockk<ProjectConfigRepository>(relaxed = true)
    private val useCase = InitProjectUseCase(buildModel, configRepo)

    private val cwd: Path = Path.of("/tmp/project")
    private val project = KotlinProject(
        root = ProjectRoot(Path.of("/tmp/project")),
        buildTool = BuildTool.GRADLE,
        kotlinVersion = KotlinVersion("2.1.0"),
        sourceDirs = listOf(FilePath("src/main/kotlin")),
        testDirs = listOf(FilePath("src/test/kotlin")),
    )

    @Test
    fun `saves config on successful introspection`() {
        every { buildModel.introspect(cwd) } returns project

        val result = useCase.execute(cwd)

        assertEquals(project, result)
        verify(exactly = 1) { configRepo.save(project) }
    }

    @Test
    fun `does not save when introspection fails`() {
        every { buildModel.introspect(cwd) } throws NoProjectRootException()

        assertThrows(NoProjectRootException::class.java) { useCase.execute(cwd) }

        verify(exactly = 0) { configRepo.save(any()) }
    }
}
