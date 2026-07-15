package org.panny.patchy.kode.adapter.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import org.panny.patchy.kode.adapter.presenter.JsonPresenter
import org.panny.patchy.kode.application.usecase.DiagnoseEnvironmentUseCase
import java.nio.file.Path

/**
 * `kode doctor` — check the environment and report as JSON.
 *
 * Output: the readiness report JSON on stdout; progress and human-readable
 * notes on stderr (design ch. 07). Checks alone never modify anything; only
 * `--install-lsp` writes, and only when no LSP already resolves. A failed
 * check is reported in the JSON with exit 0; only a failed install errors.
 */
class DoctorCommand(
    private val diagnoseEnvironment: DiagnoseEnvironmentUseCase,
    private val presenter: JsonPresenter,
) : CliktCommand(name = "doctor") {

    private val installLsp by option(
        "--install-lsp",
        help = "Download the Kotlin LSP into ~/.kode/lsp when none is found",
    ).flag()

    private val lspVersion by option(
        "--lsp-version",
        help = "Kotlin LSP version for --install-lsp (defaults to the pinned version)",
    )

    override fun help(context: Context): String =
        "Check the environment (.kode.json, kotlin-lsp, Java runtime) and report as JSON"

    override fun run() {
        val cwd = Path.of("").toAbsolutePath()
        val result = diagnoseEnvironment.execute(cwd, installLsp = installLsp, lspVersion = lspVersion)
        echo(presenter.renderDoctor(result)) // stdout: the readiness report
        if (!result.report.ok) {
            echo("Some checks did not pass - see the hints in the JSON report.", err = true)
        }
    }
}
