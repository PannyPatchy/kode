package org.panny.patchy.kode.adapter.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import org.panny.patchy.kode.adapter.presenter.JsonPresenter
import org.panny.patchy.kode.application.usecase.FindTestsUseCase
import java.nio.file.Path

/** `kode test <target>` — test classes/functions related to a production symbol. */
class TestCommand(
    private val findTests: FindTestsUseCase,
    private val presenter: JsonPresenter,
) : CliktCommand(name = "test") {

    private val target by argument(help = "Class or function name")

    override fun help(context: Context): String =
        "Return related test classes/functions for a target (JSON, via naming heuristics)"

    override fun run() {
        val cwd = Path.of("").toAbsolutePath()
        echo(presenter.renderTests(findTests.execute(cwd, target)))
    }
}
