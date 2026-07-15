package org.panny.patchy.kode.application.usecase

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.panny.patchy.kode.domain.entity.KotlinProject
import org.panny.patchy.kode.domain.entity.Symbol
import org.panny.patchy.kode.domain.error.InvalidArgumentException
import org.panny.patchy.kode.domain.error.NoProjectConfigException
import org.panny.patchy.kode.domain.port.ProjectConfigRepository
import org.panny.patchy.kode.domain.port.SymbolPort
import org.panny.patchy.kode.domain.valueobject.BuildTool
import org.panny.patchy.kode.domain.valueobject.FilePath
import org.panny.patchy.kode.domain.valueobject.ProjectRoot
import org.panny.patchy.kode.domain.valueobject.SymbolName
import java.nio.file.Files
import java.nio.file.Path

class ListSymbolsUseCaseTest {

    @TempDir
    lateinit var root: Path

    private val configRepo = mockk<ProjectConfigRepository>()
    private val symbolPort = mockk<SymbolPort>()
    private val useCase = ListSymbolsUseCase(configRepo, symbolPort)

    private fun project() = KotlinProject(
        root = ProjectRoot(root),
        buildTool = BuildTool.GRADLE,
        kotlinVersion = null,
        sourceDirs = listOf(FilePath("src/main/kotlin")),
        testDirs = emptyList(),
    )

    private fun createFile(relative: String): FilePath {
        val file = root.resolve(relative)
        Files.createDirectories(file.parent)
        Files.writeString(file, "class Foo")
        return FilePath(relative)
    }

    @Test
    fun `classifies symbols into classes, functions and top-level properties`() {
        val file = createFile("src/main/kotlin/Foo.kt")
        val clazz = Symbol.ClassSymbol(SymbolName("Foo"), emptyList())
        val function = Symbol.FunctionSymbol(SymbolName("doIt"), emptyList(), "Unit")
        val property = Symbol.PropertySymbol(SymbolName("CONSTANT"), "String", mutable = false)
        every { configRepo.load(ProjectRoot(root)) } returns project()
        every { symbolPort.symbols(ProjectRoot(root), file) } returns listOf(clazz, function, property)

        val result = useCase.execute(root, "src/main/kotlin/Foo.kt")

        assertEquals(file, result.file)
        assertEquals(listOf(clazz), result.classes)
        assertEquals(listOf(function), result.functions)
        assertEquals(listOf(property), result.topLevelProperties)
    }

    @Test
    fun `fails with INVALID_ARGUMENT for a missing file`() {
        every { configRepo.load(ProjectRoot(root)) } returns project()

        assertThrows(InvalidArgumentException::class.java) {
            useCase.execute(root, "src/main/kotlin/Missing.kt")
        }
    }

    @Test
    fun `fails with NO_PROJECT_CONFIG when no config is found`() {
        every { configRepo.load(ProjectRoot(root)) } returns null

        assertThrows(NoProjectConfigException::class.java) { useCase.execute(root, "whatever.kt") }
    }
}
