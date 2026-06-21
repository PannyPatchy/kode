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
import org.panny.patchy.kode.domain.valueobject.KotlinVersion
import java.nio.file.Path

/**
 * `kode init` — recognize the project and generate `.kode.json`.
 *
 * Output: the `.kode.json` schema JSON on stdout. Prompts and the human-readable
 * summary are written to stderr so stdout stays JSON-only (design ch. 07). On
 * error nothing is generated (the use case never saves on failure).
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

    private val kotlinVersionOverride by option(
        "--kotlin-version",
        "-k",
        metavar = "VERSION",
        help = "Kotlin version to record; used when it cannot be auto-detected (also overrides detection)",
    )

    override fun help(context: Context): String =
        "Recognize the Kotlin/Gradle project and generate .kode.json"

    override fun run() {
        val cwd = Path.of("").toAbsolutePath()
        val result = initProject.execute(
            cwd = cwd,
            force = force,
            confirmOverwrite = ::askOverwrite,
            resolveKotlinVersion = ::resolveKotlinVersion,
        )
        when (result) {
            is InitResult.Generated -> reportGenerated(result)
            is InitResult.Aborted -> echo("Aborted: kept the existing .kode.json.", err = true)
        }
    }

    private fun askOverwrite(): Boolean =
        YesNoPrompt(".kode.json already exists. Overwrite?", terminal, default = false).ask() == true

    /**
     * Resolve the Kotlin version to record: an explicit `--kotlin-version` wins,
     * then the auto-detected value, and finally an interactive prompt. Returns
     * `null` when none of those yield a value (the field is then omitted).
     */
    private fun resolveKotlinVersion(detected: KotlinVersion?): KotlinVersion? {
        val raw = kotlinVersionOverride ?: detected?.value ?: promptForKotlinVersion()
        return raw?.trim()?.takeIf { it.isNotEmpty() }?.let(::KotlinVersion)
    }

    /** Ask on stderr (keeping stdout clean) and read one line; `null` on EOF / no input. */
    private fun promptForKotlinVersion(): String? {
        echo(
            "Kotlin version could not be detected. Enter it (leave blank to skip): ",
            err = true,
            trailingNewline = false,
        )
        return readlnOrNull()
    }

    private fun reportGenerated(result: InitResult.Generated) {
        echo(presenter.renderProject(result.project)) // stdout: the .kode.json schema
        val root = result.project.root.path
        val verb = if (result.overwritten) "Updated" else "Created"
        echo("$verb .kode.json at $root", err = true)
        if (result.project.kotlinVersion == null) {
            echo("Note: Kotlin version is unknown (kotlin_version omitted).", err = true)
        }
        echo("Tip: add .kode.json to .gitignore - it holds machine-specific absolute paths.", err = true)
    }
}
