package dev.kode.bootstrap

import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.subcommands
import dev.kode.adapter.cli.ErrorsCommand
import dev.kode.adapter.cli.InitCommand
import dev.kode.adapter.cli.KodeCli
import dev.kode.adapter.cli.RefsCommand
import dev.kode.adapter.cli.SymbolsCommand
import dev.kode.adapter.cli.TestCommand
import dev.kode.adapter.cli.TreeCommand
import dev.kode.adapter.config.JsonProjectConfigRepository
import dev.kode.adapter.fs.HeuristicBuildModelAdapter
import dev.kode.adapter.presenter.ErrorEnvelope
import dev.kode.adapter.presenter.JsonPresenter
import dev.kode.application.usecase.InitProjectUseCase
import dev.kode.domain.error.KodeException
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
