package org.panny.patchy.kode.application.usecase

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.panny.patchy.kode.domain.entity.KotlinProject
import org.panny.patchy.kode.domain.entity.TestClass
import org.panny.patchy.kode.domain.error.InvalidArgumentException
import org.panny.patchy.kode.domain.port.ProjectConfigRepository
import org.panny.patchy.kode.domain.port.TestDiscoveryPort
import org.panny.patchy.kode.domain.service.SymbolMatcher
import org.panny.patchy.kode.domain.valueobject.BuildTool
import org.panny.patchy.kode.domain.valueobject.FilePath
import org.panny.patchy.kode.domain.valueobject.ProjectRoot
import org.panny.patchy.kode.domain.valueobject.SymbolName
import java.nio.file.Path

class FindTestsUseCaseTest {

    private val configRepo = mockk<ProjectConfigRepository>()
    private val testDiscovery = mockk<TestDiscoveryPort>()
    private val useCase = FindTestsUseCase(configRepo, testDiscovery, SymbolMatcher())

    private val cwd: Path = Path.of("/tmp/project")
    private val project = KotlinProject(
        root = ProjectRoot(cwd),
        buildTool = BuildTool.GRADLE,
        kotlinVersion = null,
        sourceDirs = listOf(FilePath("src/main/kotlin")),
        testDirs = listOf(FilePath("src/test/kotlin")),
    )

    @Test
    fun `returns the test classes discovered for the target`() {
        val testClass = TestClass(
            name = SymbolName("FooClassTest"),
            file = FilePath("src/test/kotlin/FooClassTest.kt"),
            functions = listOf(SymbolName("testSomething")),
        )
        every { configRepo.load(project.root) } returns project
        every { testDiscovery.testsFor(project, SymbolName("FooClass")) } returns listOf(testClass)

        val result = useCase.execute(cwd, "FooClass")

        assertEquals(SymbolName("FooClass"), result.target)
        assertEquals(listOf(testClass), result.tests)
    }

    @Test
    fun `a target without tests yields an empty list`() {
        every { configRepo.load(project.root) } returns project
        every { testDiscovery.testsFor(project, SymbolName("Lonely")) } returns emptyList()

        assertTrue(useCase.execute(cwd, "Lonely").tests.isEmpty())
    }

    @Test
    fun `a malformed target fails before discovery runs`() {
        every { configRepo.load(project.root) } returns project

        assertThrows(InvalidArgumentException::class.java) { useCase.execute(cwd, "a.b.c") }

        verify(exactly = 0) { testDiscovery.testsFor(any(), any()) }
    }
}
