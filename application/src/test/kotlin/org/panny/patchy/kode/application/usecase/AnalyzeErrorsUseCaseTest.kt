package org.panny.patchy.kode.application.usecase

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.panny.patchy.kode.domain.entity.Diagnostic
import org.panny.patchy.kode.domain.entity.KotlinProject
import org.panny.patchy.kode.domain.port.DiagnosticsPort
import org.panny.patchy.kode.domain.port.ProjectConfigRepository
import org.panny.patchy.kode.domain.valueobject.BuildTool
import org.panny.patchy.kode.domain.valueobject.CodeSnippet
import org.panny.patchy.kode.domain.valueobject.FilePath
import org.panny.patchy.kode.domain.valueobject.ProjectRoot
import org.panny.patchy.kode.domain.valueobject.Severity
import org.panny.patchy.kode.domain.valueobject.SourcePosition
import java.nio.file.Files
import java.nio.file.Path

class AnalyzeErrorsUseCaseTest {

    @TempDir
    lateinit var root: Path

    private val configRepo = mockk<ProjectConfigRepository>()
    private val diagnosticsPort = mockk<DiagnosticsPort>()
    private val useCase = AnalyzeErrorsUseCase(configRepo, diagnosticsPort)

    private fun setUpProject(): FilePath {
        val file = root.resolve("src/main/kotlin/Foo.kt")
        Files.createDirectories(file.parent)
        Files.writeString(file, "val x: String = 42")
        every { configRepo.load(ProjectRoot(root)) } returns KotlinProject(
            root = ProjectRoot(root),
            buildTool = BuildTool.GRADLE,
            kotlinVersion = null,
            sourceDirs = listOf(FilePath("src/main/kotlin")),
            testDirs = emptyList(),
        )
        return FilePath("src/main/kotlin/Foo.kt")
    }

    @Test
    fun `returns the diagnostics reported for the file`() {
        val file = setUpProject()
        val diagnostic = Diagnostic(
            position = SourcePosition(1, 17),
            severity = Severity.ERROR,
            message = "Type mismatch: expected String, found Int",
            snippet = CodeSnippet("val x: String = 42"),
        )
        every { diagnosticsPort.diagnostics(ProjectRoot(root), file) } returns listOf(diagnostic)

        val result = useCase.execute(root, "src/main/kotlin/Foo.kt")

        assertEquals(file, result.file)
        assertEquals(listOf(diagnostic), result.diagnostics)
    }

    @Test
    fun `a clean file yields an empty diagnostics list`() {
        val file = setUpProject()
        every { diagnosticsPort.diagnostics(ProjectRoot(root), file) } returns emptyList()

        val result = useCase.execute(root, "src/main/kotlin/Foo.kt")

        assertTrue(result.diagnostics.isEmpty())
    }
}
