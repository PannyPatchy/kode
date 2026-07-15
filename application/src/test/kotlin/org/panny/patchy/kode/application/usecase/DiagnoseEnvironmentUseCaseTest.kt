package org.panny.patchy.kode.application.usecase

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.panny.patchy.kode.domain.entity.CheckStatus
import org.panny.patchy.kode.domain.port.EnvironmentInspectionPort
import org.panny.patchy.kode.domain.port.JavaRuntimeStatus
import org.panny.patchy.kode.domain.port.LspInstallerPort
import org.panny.patchy.kode.domain.port.ProjectConfigStatus
import org.panny.patchy.kode.domain.valueobject.LspInstallation
import org.panny.patchy.kode.domain.valueobject.LspLocation
import org.panny.patchy.kode.domain.valueobject.LspSource
import java.nio.file.Path

class DiagnoseEnvironmentUseCaseTest {

    private val inspection = mockk<EnvironmentInspectionPort>()
    private val installer = mockk<LspInstallerPort>()
    private val useCase = DiagnoseEnvironmentUseCase(inspection, installer)

    private val cwd: Path = Path.of("/tmp/project")
    private val launcher: Path = Path.of("/home/user/.kode/lsp/1.0.0/kotlin-lsp.sh")

    private fun healthyEnvironment() {
        every { inspection.projectConfig(cwd) } returns ProjectConfigStatus.Found(cwd)
        every { inspection.locateLsp(cwd) } returns LspLocation(LspSource.PATH, launcher)
        every { inspection.javaRuntime() } returns JavaRuntimeStatus.Available(21)
    }

    private fun check(result: org.panny.patchy.kode.application.dto.DoctorResult, name: String) =
        result.report.checks.single { it.name == name }

    @Test
    fun `reports ok when everything is in place`() {
        healthyEnvironment()

        val result = useCase.execute(cwd)

        assertTrue(result.report.ok)
        assertEquals(3, result.report.checks.size)
        assertNull(result.installedNow)
        verify(exactly = 0) { installer.install(any()) }
    }

    @Test
    fun `reports missing config and lsp with hints`() {
        every { inspection.projectConfig(cwd) } returns ProjectConfigStatus.Missing
        every { inspection.locateLsp(cwd) } returns null
        every { inspection.javaRuntime() } returns JavaRuntimeStatus.NotFound

        val result = useCase.execute(cwd)

        assertFalse(result.report.ok)
        val config = check(result, DiagnoseEnvironmentUseCase.CHECK_PROJECT_CONFIG)
        assertEquals(CheckStatus.MISSING, config.status)
        assertTrue(config.hint.orEmpty().contains("kode init"))
        val lsp = check(result, DiagnoseEnvironmentUseCase.CHECK_LSP_BINARY)
        assertTrue(lsp.hint.orEmpty().contains("--install-lsp"))
    }

    @Test
    fun `flags an invalid config and a too-old java`() {
        every { inspection.projectConfig(cwd) } returns ProjectConfigStatus.Invalid("parse error")
        every { inspection.locateLsp(cwd) } returns LspLocation(LspSource.ENV, launcher)
        every { inspection.javaRuntime() } returns JavaRuntimeStatus.Available(11)

        val result = useCase.execute(cwd)

        assertEquals(CheckStatus.INVALID, check(result, DiagnoseEnvironmentUseCase.CHECK_PROJECT_CONFIG).status)
        assertEquals(CheckStatus.INVALID, check(result, DiagnoseEnvironmentUseCase.CHECK_JAVA_RUNTIME).status)
    }

    @Test
    fun `installs the lsp and re-checks when requested and missing`() {
        val installation = LspInstallation("1.0.0", launcher, checksumVerified = true)
        every { inspection.projectConfig(cwd) } returns ProjectConfigStatus.Found(cwd)
        every { inspection.locateLsp(cwd) } returnsMany listOf(
            null,
            LspLocation(LspSource.MANAGED, launcher, installedVersion = "1.0.0"),
        )
        every { inspection.javaRuntime() } returns JavaRuntimeStatus.Available(21)
        every { installer.install(null) } returns installation

        val result = useCase.execute(cwd, installLsp = true)

        assertEquals(installation, result.installedNow)
        assertEquals(LspSource.MANAGED, result.report.lsp?.source)
        assertEquals(CheckStatus.OK, check(result, DiagnoseEnvironmentUseCase.CHECK_LSP_BINARY).status)
    }

    @Test
    fun `never installs when an lsp already resolves`() {
        healthyEnvironment()

        val result = useCase.execute(cwd, installLsp = true)

        assertNull(result.installedNow)
        verify(exactly = 0) { installer.install(any()) }
    }

    @Test
    fun `passes the requested version to the installer`() {
        every { inspection.projectConfig(cwd) } returns ProjectConfigStatus.Missing
        every { inspection.locateLsp(cwd) } returnsMany listOf(null, LspLocation(LspSource.MANAGED, launcher))
        every { inspection.javaRuntime() } returns JavaRuntimeStatus.Available(21)
        every { installer.install("9.9.9") } returns LspInstallation("9.9.9", launcher)

        useCase.execute(cwd, installLsp = true, lspVersion = "9.9.9")

        verify { installer.install("9.9.9") }
    }
}
