package org.panny.patchy.kode.application.usecase

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.panny.patchy.kode.domain.entity.KotlinProject
import org.panny.patchy.kode.domain.entity.Reference
import org.panny.patchy.kode.domain.error.InvalidArgumentException
import org.panny.patchy.kode.domain.port.ProjectConfigRepository
import org.panny.patchy.kode.domain.port.ReferencePort
import org.panny.patchy.kode.domain.service.SymbolMatcher
import org.panny.patchy.kode.domain.valueobject.BuildTool
import org.panny.patchy.kode.domain.valueobject.CodeSnippet
import org.panny.patchy.kode.domain.valueobject.FilePath
import org.panny.patchy.kode.domain.valueobject.ProjectRoot
import org.panny.patchy.kode.domain.valueobject.SourcePosition
import org.panny.patchy.kode.domain.valueobject.SymbolName
import java.nio.file.Path

class FindReferencesUseCaseTest {

    private val configRepo = mockk<ProjectConfigRepository>()
    private val referencePort = mockk<ReferencePort>()
    private val useCase = FindReferencesUseCase(configRepo, referencePort, SymbolMatcher())

    private val cwd: Path = Path.of("/tmp/project")
    private val project = KotlinProject(
        root = ProjectRoot(cwd),
        buildTool = BuildTool.GRADLE,
        kotlinVersion = null,
        sourceDirs = listOf(FilePath("src/main/kotlin")),
        testDirs = emptyList(),
    )

    @Test
    fun `returns the references reported for the target`() {
        val reference = Reference(
            FilePath("src/main/kotlin/Bar.kt"),
            SourcePosition(10, 5),
            CodeSnippet("val foo = FooClass()"),
        )
        every { configRepo.load(project.root) } returns project
        every { referencePort.references(project.root, SymbolName("FooClass")) } returns listOf(reference)

        val result = useCase.execute(cwd, "FooClass")

        assertEquals(SymbolName("FooClass"), result.target)
        assertEquals(listOf(reference), result.refs)
    }

    @Test
    fun `an unresolved target yields an empty refs list`() {
        every { configRepo.load(project.root) } returns project
        every { referencePort.references(project.root, SymbolName("NoSuchSymbol")) } returns emptyList()

        assertTrue(useCase.execute(cwd, "NoSuchSymbol").refs.isEmpty())
    }

    @Test
    fun `a malformed target fails before the reference port runs`() {
        every { configRepo.load(project.root) } returns project

        assertThrows(InvalidArgumentException::class.java) { useCase.execute(cwd, "a.b.c") }

        verify(exactly = 0) { referencePort.references(project.root, any()) }
    }
}
