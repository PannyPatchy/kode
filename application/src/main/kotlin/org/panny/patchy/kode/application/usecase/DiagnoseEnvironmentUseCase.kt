package org.panny.patchy.kode.application.usecase

import org.panny.patchy.kode.application.dto.DoctorResult
import org.panny.patchy.kode.domain.entity.CheckStatus
import org.panny.patchy.kode.domain.entity.EnvironmentCheck
import org.panny.patchy.kode.domain.entity.EnvironmentReport
import org.panny.patchy.kode.domain.port.EnvironmentInspectionPort
import org.panny.patchy.kode.domain.port.JavaRuntimeStatus
import org.panny.patchy.kode.domain.port.LspInstallerPort
import org.panny.patchy.kode.domain.port.ProjectConfigStatus
import org.panny.patchy.kode.domain.valueobject.LspInstallation
import org.panny.patchy.kode.domain.valueobject.LspLocation
import java.nio.file.Path

/** The standalone kotlin-lsp launcher requires at least this Java major version. */
private const val MIN_JAVA_MAJOR = 17

/**
 * `kode doctor`: check the environment (project config, Kotlin LSP, Java
 * runtime) and report as structured JSON. Checks are read-only; only
 * `installLsp = true` may write, and only when no LSP resolves — an already
 * resolvable LSP is never replaced (design ch. 03).
 *
 * A failed check is still a successful `doctor` run (exit 0, status in the
 * JSON); only a failed installation raises an error.
 */
class DiagnoseEnvironmentUseCase(
    private val inspection: EnvironmentInspectionPort,
    private val installer: LspInstallerPort,
) {
    fun execute(cwd: Path, installLsp: Boolean = false, lspVersion: String? = null): DoctorResult {
        var lsp = inspection.locateLsp(cwd)
        var installedNow: LspInstallation? = null
        if (installLsp && lsp == null) {
            installedNow = installer.install(lspVersion) // throws on failure
            lsp = inspection.locateLsp(cwd)
        }
        val checks = listOf(
            projectConfigCheck(inspection.projectConfig(cwd)),
            lspCheck(lsp),
            javaRuntimeCheck(inspection.javaRuntime()),
        )
        return DoctorResult(EnvironmentReport(checks, lsp), installedNow)
    }

    private fun projectConfigCheck(status: ProjectConfigStatus): EnvironmentCheck = when (status) {
        is ProjectConfigStatus.Found -> EnvironmentCheck(
            name = CHECK_PROJECT_CONFIG,
            status = CheckStatus.OK,
            detail = ".kode.json found (project root: ${status.root})",
        )
        ProjectConfigStatus.Missing -> EnvironmentCheck(
            name = CHECK_PROJECT_CONFIG,
            status = CheckStatus.MISSING,
            detail = "No .kode.json found",
            hint = "Run 'kode init' at the project root.",
        )
        is ProjectConfigStatus.Invalid -> EnvironmentCheck(
            name = CHECK_PROJECT_CONFIG,
            status = CheckStatus.INVALID,
            detail = status.detail,
            hint = "Re-run 'kode init' to regenerate .kode.json.",
        )
    }

    private fun lspCheck(lsp: LspLocation?): EnvironmentCheck = if (lsp != null) {
        EnvironmentCheck(
            name = CHECK_LSP_BINARY,
            status = CheckStatus.OK,
            detail = "kotlin-lsp resolved via ${lsp.source.name.lowercase()}: ${lsp.path}",
        )
    } else {
        EnvironmentCheck(
            name = CHECK_LSP_BINARY,
            status = CheckStatus.MISSING,
            detail = "kotlin-lsp not found",
            hint = "Run 'kode doctor --install-lsp' to download it.",
        )
    }

    private fun javaRuntimeCheck(status: JavaRuntimeStatus): EnvironmentCheck = when (status) {
        is JavaRuntimeStatus.Available -> if (status.majorVersion >= MIN_JAVA_MAJOR) {
            EnvironmentCheck(
                name = CHECK_JAVA_RUNTIME,
                status = CheckStatus.OK,
                detail = "Java ${status.majorVersion} found",
            )
        } else {
            EnvironmentCheck(
                name = CHECK_JAVA_RUNTIME,
                status = CheckStatus.INVALID,
                detail = "Java ${status.majorVersion} found, but kotlin-lsp requires $MIN_JAVA_MAJOR or newer",
                hint = "Install a JRE $MIN_JAVA_MAJOR+ (required to run kotlin-lsp).",
            )
        }
        JavaRuntimeStatus.NotFound -> EnvironmentCheck(
            name = CHECK_JAVA_RUNTIME,
            status = CheckStatus.MISSING,
            detail = "No Java runtime found",
            hint = "Install a JRE $MIN_JAVA_MAJOR+ (required to run kotlin-lsp).",
        )
    }

    companion object {
        const val CHECK_PROJECT_CONFIG: String = "project_config"
        const val CHECK_LSP_BINARY: String = "lsp_binary"
        const val CHECK_JAVA_RUNTIME: String = "java_runtime"
    }
}
