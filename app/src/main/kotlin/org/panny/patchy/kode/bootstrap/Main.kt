package org.panny.patchy.kode.bootstrap

import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.core.subcommands
import org.panny.patchy.kode.adapter.cli.ErrorsCommand
import org.panny.patchy.kode.adapter.cli.InitCommand
import org.panny.patchy.kode.adapter.cli.KodeCli
import org.panny.patchy.kode.adapter.cli.RefsCommand
import org.panny.patchy.kode.adapter.cli.SymbolsCommand
import org.panny.patchy.kode.adapter.cli.TestCommand
import org.panny.patchy.kode.adapter.cli.TreeCommand
import org.panny.patchy.kode.adapter.config.JsonProjectConfigRepository
import org.panny.patchy.kode.adapter.fs.HeuristicBuildModelAdapter
import org.panny.patchy.kode.adapter.presenter.ErrorEnvelope
import org.panny.patchy.kode.adapter.presenter.JsonPresenter
import org.panny.patchy.kode.application.usecase.InitProjectUseCase
import org.panny.patchy.kode.domain.error.KodeException
import kotlin.system.exitProcess

private const val EXIT_INTERNAL_ERROR = 1

/**
 * The single Composition Root. All implementations are wired manually (Pure DI —
 * no DI framework, native-image friendly; design ch. 01).
 */
fun main(args: Array<String>) {
    exitProcess(runCli(args))
}

internal fun runCli(args: Array<String>): Int {
    // --- Adapters / Infrastructure (outer) ---
    val configRepo = JsonProjectConfigRepository()
    val buildModel = HeuristicBuildModelAdapter()
    val presenter = JsonPresenter()

    // --- Use cases (inner), with ports injected ---
    val initProject = InitProjectUseCase(buildModel, configRepo)

    // --- Driving adapter: the CLI ---
    val cli = KodeCli().subcommands(
        InitCommand(initProject, presenter),
        ErrorsCommand(),
        RefsCommand(),
        TestCommand(),
        TreeCommand(),
        SymbolsCommand(),
    )

    return try {
        cli.parse(args)
        0
    } catch (e: KodeException) {
        System.err.println(presenter.renderError(ErrorEnvelope.of(e)))
        e.exitCode
    } catch (e: CliktError) {
        // Usage errors / --help: let Clikt format the message, keep its status code.
        cli.echoFormattedHelp(e)
        e.statusCode
    } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
        // Top-level safety net: any uncaught error is rounded to INTERNAL_ERROR
        // so stdout stays JSON-only and the AI always gets a machine-readable result
        // (design ch. 07).
        System.err.println(presenter.renderError(ErrorEnvelope.internalError(e)))
        EXIT_INTERNAL_ERROR
    }
}
