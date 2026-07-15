package org.panny.patchy.kode.adapter.lsp

import io.mockk.every
import io.mockk.mockk
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.DidChangeConfigurationParams
import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.DidChangeWatchedFilesParams
import org.eclipse.lsp4j.DidCloseTextDocumentParams
import org.eclipse.lsp4j.DidOpenTextDocumentParams
import org.eclipse.lsp4j.DidSaveTextDocumentParams
import org.eclipse.lsp4j.DocumentSymbol
import org.eclipse.lsp4j.DocumentSymbolParams
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.PublishDiagnosticsParams
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.ReferenceParams
import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.lsp4j.SymbolInformation
import org.eclipse.lsp4j.SymbolKind
import org.eclipse.lsp4j.TextDocumentSyncKind
import org.eclipse.lsp4j.WorkspaceSymbol
import org.eclipse.lsp4j.WorkspaceSymbolParams
import org.eclipse.lsp4j.Diagnostic as LspDiagnostic
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageServer
import org.eclipse.lsp4j.services.TextDocumentService
import org.eclipse.lsp4j.services.WorkspaceService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.panny.patchy.kode.domain.entity.Symbol
import org.panny.patchy.kode.domain.service.SnippetExtractor
import org.panny.patchy.kode.domain.service.SymbolMatcher
import org.panny.patchy.kode.domain.valueobject.FilePath
import org.panny.patchy.kode.domain.valueobject.ProjectRoot
import org.panny.patchy.kode.domain.valueobject.Severity
import org.panny.patchy.kode.domain.valueobject.SymbolName
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

/**
 * Round-trip tests for the three LSP port adapters against an in-JVM scripted
 * server (piped streams, no OS process), injected through [LspClientProvider].
 */
class LspPortAdapterTest {

    @TempDir
    lateinit var root: Path

    /** Canned responses, configurable per test. */
    private class ScriptedServer : LanguageServer, TextDocumentService, WorkspaceService {
        lateinit var client: LanguageClient
        var diagnosticsOnOpen: (String) -> List<LspDiagnostic> = { emptyList() }
        var documentSymbols: List<Either<SymbolInformation, DocumentSymbol>> = emptyList()
        var workspaceSymbols: List<SymbolInformation> = emptyList()
        var references: List<Location> = emptyList()

        override fun initialize(params: InitializeParams): CompletableFuture<InitializeResult> {
            val caps = ServerCapabilities().apply {
                setTextDocumentSync(TextDocumentSyncKind.Full)
                setReferencesProvider(true)
                setDocumentSymbolProvider(true)
                setWorkspaceSymbolProvider(true)
            }
            return CompletableFuture.completedFuture(InitializeResult(caps))
        }

        override fun shutdown(): CompletableFuture<Any> = CompletableFuture.completedFuture(null)
        override fun exit() = Unit
        override fun getTextDocumentService(): TextDocumentService = this
        override fun getWorkspaceService(): WorkspaceService = this

        override fun didOpen(params: DidOpenTextDocumentParams) {
            client.publishDiagnostics(
                PublishDiagnosticsParams(params.textDocument.uri, diagnosticsOnOpen(params.textDocument.uri)),
            )
        }

        override fun didChange(params: DidChangeTextDocumentParams) = Unit
        override fun didClose(params: DidCloseTextDocumentParams) = Unit
        override fun didSave(params: DidSaveTextDocumentParams) = Unit

        override fun references(params: ReferenceParams): CompletableFuture<MutableList<out Location>> =
            CompletableFuture.completedFuture(references.toMutableList())

        override fun documentSymbol(
            params: DocumentSymbolParams,
        ): CompletableFuture<MutableList<Either<SymbolInformation, DocumentSymbol>>> =
            CompletableFuture.completedFuture(documentSymbols.toMutableList())

        override fun symbol(
            params: WorkspaceSymbolParams,
        ): CompletableFuture<Either<MutableList<out SymbolInformation>, MutableList<out WorkspaceSymbol>>> =
            CompletableFuture.completedFuture(Either.forLeft(workspaceSymbols.toMutableList()))

        override fun didChangeConfiguration(params: DidChangeConfigurationParams) = Unit
        override fun didChangeWatchedFiles(params: DidChangeWatchedFilesParams) = Unit
    }

    /** Builds a fresh piped client/server pair per call, mirroring production. */
    private class PipedProvider(private val server: ScriptedServer) : LspClientProvider {
        override fun <T> withClient(root: Path, block: (LspClient) -> T): T {
            val kodeClient = KodeLanguageClient()
            val toServer = PipedInputStream()
            val clientOut = PipedOutputStream(toServer)
            val toClient = PipedInputStream()
            val serverOut = PipedOutputStream(toClient)
            val clientLauncher = LSPLauncher.createClientLauncher(kodeClient, toClient, clientOut)
            val serverLauncher = LSPLauncher.createServerLauncher(server, toServer, serverOut)
            server.client = serverLauncher.remoteProxy
            clientLauncher.startListening()
            serverLauncher.startListening()
            val process = mockk<Process>(relaxed = true) {
                every { isAlive } returns true
                every { waitFor(any(), any()) } returns true
            }
            val session = LspSession(process, clientLauncher.remoteProxy, kodeClient)
            return LspClient(root, session, defaultTimeoutMillis = 5_000).use { client ->
                client.initialize()
                block(client)
            }
        }
    }

    private val server = ScriptedServer()
    private val provider = PipedProvider(server)

    private fun writeFile(relative: String, content: String): Path {
        val file = root.resolve(relative)
        Files.createDirectories(file.parent)
        Files.writeString(file, content)
        return file
    }

    private fun range(line: Int, column: Int) = Range(Position(line, column), Position(line, column + 1))

    @Test
    fun `diagnostics adapter maps LSP diagnostics to 1-based positions with snippets`() {
        writeFile("src/Foo.kt", "package x\nval x: String = 42\n")
        server.diagnosticsOnOpen = { uri ->
            listOf(
                LspDiagnostic(range(1, 16), "Type mismatch").apply { severity = DiagnosticSeverity.Error },
            )
        }
        val adapter = LspDiagnosticsAdapter(provider, SnippetExtractor())

        val diagnostics = adapter.diagnostics(ProjectRoot(root), FilePath("src/Foo.kt"))

        assertEquals(1, diagnostics.size)
        val diagnostic = diagnostics.first()
        assertEquals(2, diagnostic.position.line)
        assertEquals(17, diagnostic.position.column)
        assertEquals(Severity.ERROR, diagnostic.severity)
        assertEquals("Type mismatch", diagnostic.message)
        assertEquals("val x: String = 42", diagnostic.snippet.text)
    }

    @Test
    fun `symbol adapter maps the documentSymbol hierarchy`() {
        writeFile(
            "src/Greeter.kt",
            "class Greeter(val name: String) {\n    fun greet(who: String): String = who\n}\n",
        )
        val clazz = DocumentSymbol("Greeter", SymbolKind.Class, range(0, 6), range(0, 6)).apply {
            children = listOf(
                DocumentSymbol("name", SymbolKind.Property, range(0, 18), range(0, 18)).apply {
                    detail = "val name: String"
                },
                DocumentSymbol("greet", SymbolKind.Method, range(1, 8), range(1, 8)).apply {
                    detail = "(who: String): String"
                },
            )
        }
        server.documentSymbols = listOf(Either.forRight(clazz))
        val adapter = LspSymbolAdapter(provider)

        val symbols = adapter.symbols(ProjectRoot(root), FilePath("src/Greeter.kt"))

        val classSymbol = symbols.filterIsInstance<Symbol.ClassSymbol>().single()
        assertEquals(listOf(Symbol.Field("name", "String", mutable = false)), classSymbol.fields)
        val function = symbols.filterIsInstance<Symbol.FunctionSymbol>().single()
        assertEquals("String", function.returnType)
        assertEquals(listOf(Symbol.Argument("who", "String")), function.arguments)
    }

    @Test
    fun `reference adapter resolves the definition then maps in-project references`() {
        val definition = writeFile("src/Greeter.kt", "class Greeter\n")
        writeFile("src/Main.kt", "val g = Greeter()\n")
        server.workspaceSymbols = listOf(
            SymbolInformation("Greeter", SymbolKind.Class, Location(definition.toUri().toString(), range(0, 6))),
        )
        server.references = listOf(
            Location(root.resolve("src/Main.kt").toUri().toString(), range(0, 8)),
            Location("file:///outside/Other.kt", range(0, 0)), // outside the root: dropped
        )
        val adapter = LspReferenceAdapter(provider, SymbolMatcher(), SnippetExtractor())

        val references = adapter.references(ProjectRoot(root), SymbolName("Greeter"))

        assertEquals(1, references.size)
        val reference = references.first()
        assertEquals(FilePath("src/Main.kt"), reference.file)
        assertEquals(1, reference.position.line)
        assertEquals(9, reference.position.column)
        assertEquals("val g = Greeter()", reference.snippet.text)
    }

    @Test
    fun `reference adapter returns empty when the target cannot be resolved`() {
        server.workspaceSymbols = emptyList()
        val adapter = LspReferenceAdapter(provider, SymbolMatcher(), SnippetExtractor())

        assertTrue(adapter.references(ProjectRoot(root), SymbolName("NoSuchSymbol")).isEmpty())
    }
}
