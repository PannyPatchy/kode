package org.panny.patchy.kode.adapter.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import org.panny.patchy.kode.adapter.presenter.JsonPresenter
import org.panny.patchy.kode.application.usecase.BuildTreeUseCase
import java.nio.file.Path

/** `kode tree` — the whole-project file tree from the configured source sets. */
class TreeCommand(
    private val buildTree: BuildTreeUseCase,
    private val presenter: JsonPresenter,
) : CliktCommand(name = "tree") {

    override fun help(context: Context): String =
        "Return the whole-project file tree (JSON, via file system)"

    override fun run() {
        val cwd = Path.of("").toAbsolutePath()
        echo(presenter.renderTree(buildTree.execute(cwd)))
    }
}
