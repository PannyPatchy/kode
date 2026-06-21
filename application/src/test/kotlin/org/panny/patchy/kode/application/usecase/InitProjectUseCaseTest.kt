package org.panny.patchy.kode.application.usecase

import org.panny.patchy.kode.application.dto.InitResult
import org.panny.patchy.kode.domain.entity.KotlinProject
import org.panny.patchy.kode.domain.error.NoProjectRootException
import org.panny.patchy.kode.domain.port.BuildModelPort
import org.panny.patchy.kode.domain.port.ProjectConfigRepository
import org.panny.patchy.kode.domain.valueobject.BuildTool
import org.panny.patchy.kode.domain.valueobject.FilePath
import org.panny.patchy.kode.domain.valueobject.KotlinVersion
import org.panny.patchy.kode.domain.valueobject.ProjectRoot
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
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
    fun `saves config on successful introspection when none exists`() {
        every { buildModel.introspect(cwd) } returns project
        every { configRepo.existsAt(project.root) } returns false

        val result = useCase.execute(cwd)

        assertEquals(InitResult.Generated(project, overwritten = false), result)
        verify(exactly = 1) { configRepo.save(project) }
    }

    @Test
    fun `does not save when introspection fails`() {
        every { buildModel.introspect(cwd) } throws NoProjectRootException()

        assertThrows(NoProjectRootException::class.java) { useCase.execute(cwd) }

        verify(exactly = 0) { configRepo.save(any()) }
    }

    @Test
    fun `prompts and overwrites when config exists and user confirms`() {
        every { buildModel.introspect(cwd) } returns project
        every { configRepo.existsAt(project.root) } returns true

        val result = useCase.execute(cwd, force = false, confirmOverwrite = { true })

        assertEquals(InitResult.Generated(project, overwritten = true), result)
        verify(exactly = 1) { configRepo.save(project) }
    }

    @Test
    fun `aborts without saving when config exists and user declines`() {
        every { buildModel.introspect(cwd) } returns project
        every { configRepo.existsAt(project.root) } returns true

        val result = useCase.execute(cwd, force = false, confirmOverwrite = { false })

        assertInstanceOf(InitResult.Aborted::class.java, result)
        verify(exactly = 0) { configRepo.save(any()) }
    }

    @Test
    fun `fills in the kotlin version from the resolver when detection returns null`() {
        val detected = project.copy(kotlinVersion = null)
        every { buildModel.introspect(cwd) } returns detected
        every { configRepo.existsAt(detected.root) } returns false

        val result = useCase.execute(cwd, resolveKotlinVersion = { KotlinVersion("1.9.24") })

        val saved = (result as InitResult.Generated).project
        assertEquals("1.9.24", saved.kotlinVersion?.value)
        verify(exactly = 1) { configRepo.save(saved) }
    }

    @Test
    fun `keeps the kotlin version null when the resolver supplies nothing`() {
        val detected = project.copy(kotlinVersion = null)
        every { buildModel.introspect(cwd) } returns detected
        every { configRepo.existsAt(detected.root) } returns false

        val result = useCase.execute(cwd, resolveKotlinVersion = { null })

        assertNull((result as InitResult.Generated).project.kotlinVersion)
    }

    @Test
    fun `does not resolve the version when overwrite is declined`() {
        val detected = project.copy(kotlinVersion = null)
        every { buildModel.introspect(cwd) } returns detected
        every { configRepo.existsAt(detected.root) } returns true
        var resolverCalled = false

        val result = useCase.execute(
            cwd,
            confirmOverwrite = { false },
            resolveKotlinVersion = { resolverCalled = true; it },
        )

        assertInstanceOf(InitResult.Aborted::class.java, result)
        assertFalse(resolverCalled, "resolver must not run when the user aborts")
        verify(exactly = 0) { configRepo.save(any()) }
    }

    @Test
    fun `force overwrites without prompting`() {
        every { buildModel.introspect(cwd) } returns project
        every { configRepo.existsAt(project.root) } returns true
        var prompted = false

        val result = useCase.execute(cwd, force = true, confirmOverwrite = { prompted = true; false })

        assertFalse(prompted, "confirmOverwrite must not be invoked when force is set")
        assertTrue(result is InitResult.Generated && result.overwritten)
        verify(exactly = 1) { configRepo.save(project) }
    }
}
