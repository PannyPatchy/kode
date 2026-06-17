package org.panny.patchy.kode.adapter.cli

import com.github.ajalt.clikt.core.CliktCommand
import org.panny.patchy.kode.adapter.presenter.JsonPresenter
import org.panny.patchy.kode.application.usecase.InitProjectUseCase
import java.nio.file.Path

/**
 * `kode init` — recognize the project and generate `.kode.json`.
 *
 * Output: the `.kode.json` schema JSON on stdout. On error nothing is generated
 * (the use case never saves on failure).
 */
class InitCommand(
    private val initProject: InitProjectUseCase,
    private val presenter: JsonPresenter,
) : CliktCommand(
    name = "init",
    help = "Recognize the Kotlin/Gradle project and generate .kode.json",
) {
    override fun run() {
        val project = initProject.execute(Path.of("").toAbsolutePath())
        echo(presenter.renderProject(project))
    }
}
