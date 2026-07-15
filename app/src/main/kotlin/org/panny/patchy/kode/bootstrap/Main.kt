package org.panny.patchy.kode.bootstrap

import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.core.subcommands
import org.panny.patchy.kode.adapter.cli.DoctorCommand
import org.panny.patchy.kode.adapter.cli.ErrorsCommand
import org.panny.patchy.kode.adapter.cli.InitCommand
import org.panny.patchy.kode.adapter.cli.KodeCli
import org.panny.patchy.kode.adapter.cli.RefsCommand
import org.panny.patchy.kode.adapter.cli.SymbolsCommand
import org.panny.patchy.kode.adapter.cli.TestCommand
import org.panny.patchy.kode.adapter.cli.TreeCommand
import org.panny.patchy.kode.adapter.config.JsonProjectConfigRepository
import org.panny.patchy.kode.adapter.fs.HeuristicBuildModelAdapter
import org.panny.patchy.kode.adapter.fs.HeuristicTestDiscoveryAdapter
import org.panny.patchy.kode.adapter.fs.NioFileTreeAdapter
import org.panny.patchy.kode.adapter.lsp.EnvironmentInspectionAdapter
import org.panny.patchy.kode.adapter.lsp.KotlinLspInstaller
import org.panny.patchy.kode.adapter.lsp.LspBinaryResolver
import org.panny.patchy.kode.adapter.lsp.LspDiagnosticsAdapter
import org.panny.patchy.kode.adapter.lsp.LspInstallStore
import org.panny.patchy.kode.adapter.lsp.LspReferenceAdapter
import org.panny.patchy.kode.adapter.lsp.LspSessionFactory
import org.panny.patchy.kode.adapter.lsp.LspSymbolAdapter
import org.panny.patchy.kode.adapter.presenter.ErrorEnvelope
import org.panny.patchy.kode.adapter.presenter.JsonPresenter
import org.panny.patchy.kode.application.usecase.AnalyzeErrorsUseCase
import org.panny.patchy.kode.application.usecase.BuildTreeUseCase
import org.panny.patchy.kode.application.usecase.DiagnoseEnvironmentUseCase
import org.panny.patchy.kode.application.usecase.FindReferencesUseCase
import org.panny.patchy.kode.application.usecase.FindTestsUseCase
import org.panny.patchy.kode.application.usecase.InitProjectUseCase
import org.panny.patchy.kode.application.usecase.ListSymbolsUseCase
import org.panny.patchy.kode.domain.error.KodeException
import org.panny.patchy.kode.domain.service.SnippetExtractor
import org.panny.patchy.kode.domain.service.SymbolMatcher
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
    val fileTree = NioFileTreeAdapter()

    // --- Domain services (pure) ---
    val snippetExtractor = SnippetExtractor()
    val symbolMatcher = SymbolMatcher()

    val testDiscovery = HeuristicTestDiscoveryAdapter(symbolMatcher)

    // LSP sessions are created lazily, one short-lived session per command,
    // inside the port adapters (design ch. 04 §3) — no lifecycle to manage here.
    val lspProvider = LspSessionFactory(configuredLspPath = configRepo::loadLspPath)
    val diagnosticsPort = LspDiagnosticsAdapter(lspProvider, snippetExtractor)
    val symbolPort = LspSymbolAdapter(lspProvider)
    val referencePort = LspReferenceAdapter(lspProvider, symbolMatcher, snippetExtractor)

    // Environment doctor: probes are read-only; the installer writes only under
    // ~/.kode on explicit --install-lsp. Progress goes to stderr (stdout is JSON).
    val installStore = LspInstallStore()
    val lspInstaller = KotlinLspInstaller(store = installStore, onProgress = System.err::println)
    val environmentInspection = EnvironmentInspectionAdapter(configRepo, LspBinaryResolver(), installStore)

    // --- Use cases (inner), with ports injected ---
    val initProject = InitProjectUseCase(buildModel, configRepo)
    val buildTree = BuildTreeUseCase(configRepo, fileTree)
    val findTests = FindTestsUseCase(configRepo, testDiscovery, symbolMatcher)
    val analyzeErrors = AnalyzeErrorsUseCase(configRepo, diagnosticsPort)
    val listSymbols = ListSymbolsUseCase(configRepo, symbolPort)
    val findReferences = FindReferencesUseCase(configRepo, referencePort, symbolMatcher)
    val diagnoseEnvironment = DiagnoseEnvironmentUseCase(environmentInspection, lspInstaller)

    // --- Driving adapter: the CLI ---
    val cli = KodeCli().subcommands(
        InitCommand(initProject, presenter),
        ErrorsCommand(analyzeErrors, presenter),
        RefsCommand(findReferences, presenter),
        TestCommand(findTests, presenter),
        TreeCommand(buildTree, presenter),
        SymbolsCommand(listSymbols, presenter),
        DoctorCommand(diagnoseEnvironment, presenter),
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
