package org.panny.patchy.kode.adapter.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import org.panny.patchy.kode.adapter.presenter.JsonPresenter
import org.panny.patchy.kode.application.usecase.ListSymbolsUseCase
import java.nio.file.Path

/** `kode symbols <file>` — the classes/functions/properties declared in a file. */
class SymbolsCommand(
    private val listSymbols: ListSymbolsUseCase,
    private val presenter: JsonPresenter,
) : CliktCommand(name = "symbols") {

    private val file by argument(help = "Source file to inspect")

    override fun help(context: Context): String =
        "Return classes/functions/properties in a file (JSON, via LSP)"

    override fun run() {
        val cwd = Path.of("").toAbsolutePath()
        echo(presenter.renderSymbols(listSymbols.execute(cwd, file)))
    }
}
