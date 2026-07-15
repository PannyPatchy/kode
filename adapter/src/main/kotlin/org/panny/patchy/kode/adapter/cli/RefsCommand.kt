package org.panny.patchy.kode.adapter.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import org.panny.patchy.kode.adapter.presenter.JsonPresenter
import org.panny.patchy.kode.application.usecase.FindReferencesUseCase
import java.nio.file.Path

/** `kode refs <target>` — usage sites of a class/function across the project. */
class RefsCommand(
    private val findReferences: FindReferencesUseCase,
    private val presenter: JsonPresenter,
) : CliktCommand(name = "refs") {

    private val target by argument(help = "Class or function name, e.g. FooClass or FooClass.doSomething")

    override fun help(context: Context): String =
        "Return references to a class/function (JSON, via LSP)"

    override fun run() {
        val cwd = Path.of("").toAbsolutePath()
        echo(presenter.renderRefs(findReferences.execute(cwd, target)))
    }
}
