package org.panny.patchy.kode.adapter.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.terminal.YesNoPrompt
import org.panny.patchy.kode.adapter.presenter.JsonPresenter
import org.panny.patchy.kode.application.dto.InitResult
import org.panny.patchy.kode.application.usecase.InitProjectUseCase
import java.nio.file.Path

/**
 * `kode init` — recognize the project and generate `.kode.json`.
 *
 * Output: the `.kode.json` schema JSON on stdout. A short human-readable summary
 * is written to stderr so stdout stays JSON-only (design ch. 07). On error nothing
 * is generated (the use case never saves on failure).
 */
class InitCommand(
    private val initProject: InitProjectUseCase,
    private val presenter: JsonPresenter,
) : CliktCommand(name = "init") {

    private val force by option(
        "--force",
        "-f",
        help = "Overwrite an existing .kode.json without asking for confirmation",
    ).flag()

    override fun help(context: Context): String =
        "Recognize the Kotlin/Gradle project and generate .kode.json"

    override fun run() {
        val cwd = Path.of("").toAbsolutePath()
        when (val result = initProject.execute(cwd, force = force, confirmOverwrite = ::askOverwrite)) {
            is InitResult.Generated -> reportGenerated(result)
            is InitResult.Aborted -> echo("Aborted: kept the existing .kode.json.", err = true)
        }
    }

    private fun askOverwrite(): Boolean =
        YesNoPrompt(".kode.json already exists. Overwrite?", terminal, default = false).ask() == true

    private fun reportGenerated(result: InitResult.Generated) {
        echo(presenter.renderProject(result.project)) // stdout: the .kode.json schema
        val root = result.project.root.path
        val verb = if (result.overwritten) "Updated" else "Created"
        echo("$verb .kode.json at $root", err = true)
        if (result.project.kotlinVersion == null) {
            echo("Note: Kotlin version could not be detected (kotlin_version omitted).", err = true)
        }
        echo("Tip: add .kode.json to .gitignore - it holds machine-specific absolute paths.", err = true)
    }
}
