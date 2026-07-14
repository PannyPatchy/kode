package org.panny.patchy.kode.adapter.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import org.panny.patchy.kode.adapter.presenter.JsonPresenter
import org.panny.patchy.kode.application.usecase.AnalyzeErrorsUseCase
import java.nio.file.Path

/** `kode errors <file>` — the errors/warnings currently reported for a file. */
class ErrorsCommand(
    private val analyzeErrors: AnalyzeErrorsUseCase,
    private val presenter: JsonPresenter,
) : CliktCommand(name = "errors") {

    private val file by argument(help = "Source file to analyze")

    override fun help(context: Context): String =
        "Return errors/warnings and surrounding snippets for a file (JSON, via LSP)"

    override fun run() {
        val cwd = Path.of("").toAbsolutePath()
        echo(presenter.renderErrors(analyzeErrors.execute(cwd, file)))
    }
}
