package org.panny.patchy.kode.adapter.lsp

import io.mockk.every
import io.mockk.mockk
import org.eclipse.lsp4j.DidChangeTextDocumentParams
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
import org.eclipse.lsp4j.DidChangeConfigurationParams
import org.eclipse.lsp4j.DidChangeWatchedFilesParams
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageServer
import org.eclipse.lsp4j.services.TextDocumentService
import org.eclipse.lsp4j.services.WorkspaceService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import kotlin.io.path.div
import kotlin.io.path.writeText

/**
 * Exercises [LspClient] end-to-end against an in-JVM fake [LanguageServer]
 * connected over piped streams (no OS process), covering the full JSON-RPC
 * round-trip for the four core operations.
 */
class LspClientProtocolTest {

    private lateinit var lsp: LspClient

    /** A minimal server that returns canned responses and pushes one diagnostic on open. */
    private class FakeServer : LanguageServer, TextDocumentService, WorkspaceService {
        lateinit var client: LanguageClient

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
            val diag = LspDiagnostic(range(), "unused symbol")
            client.publishDiagnostics(PublishDiagnosticsParams(params.textDocument.uri, listOf(diag)))
        }

        override fun didChange(params: DidChangeTextDocumentParams) = Unit
        override fun didClose(params: DidCloseTextDocumentParams) = Unit
        override fun didSave(params: DidSaveTextDocumentParams) = Unit

        override fun references(params: ReferenceParams): CompletableFuture<MutableList<out Location>> =
            CompletableFuture.completedFuture(mutableListOf(Location(params.textDocument.uri, range())))

        override fun documentSymbol(
            params: DocumentSymbolParams,
        ): CompletableFuture<MutableList<Either<SymbolInformation, DocumentSymbol>>> {
            val symbol = DocumentSymbol("Greeter", SymbolKind.Class, range(), range())
            return CompletableFuture.completedFuture(mutableListOf(Either.forRight(symbol)))
        }

        override fun didChangeConfiguration(params: DidChangeConfigurationParams) = Unit
        override fun didChangeWatchedFiles(params: DidChangeWatchedFilesParams) = Unit

        override fun symbol(
            params: WorkspaceSymbolParams,
        ): CompletableFuture<Either<MutableList<out SymbolInformation>, MutableList<out WorkspaceSymbol>>> {
            val info = SymbolInformation(
                params.query,
                SymbolKind.Class,
                Location("file:///tmp/Greeter.kt", range()),
                "com.example",
            )
            return CompletableFuture.completedFuture(Either.forLeft(mutableListOf(info)))
        }

        private fun range() = Range(Position(0, 0), Position(0, 1))
    }

    @BeforeEach
    fun setUp(@TempDir dir: Path) {
        val client = KodeLanguageClient()
        val toServer = PipedInputStream()
        val clientOut = PipedOutputStream(toServer)
        val toClient = PipedInputStream()
        val serverOut = PipedOutputStream(toClient)

        val clientLauncher = LSPLauncher.createClientLauncher(client, toClient, clientOut)
        val fakeServer = FakeServer()
        val serverLauncher = LSPLauncher.createServerLauncher(fakeServer, toServer, serverOut)
        fakeServer.client = serverLauncher.remoteProxy
        clientLauncher.startListening()
        serverLauncher.startListening()

        val process = mockk<Process>(relaxed = true) {
            every { isAlive } returns true
            every { waitFor(any(), any()) } returns true
        }
        val session = LspSession(process, clientLauncher.remoteProxy, client)
        lsp = LspClient(dir, session, defaultTimeoutMillis = 5_000)
    }

    @AfterEach
    fun tearDown() {
        lsp.close()
    }

    @Test
    fun `initialize reports server capabilities`() {
        val result = lsp.initialize()
        assertTrue(LspClientTestAccess.referencesProvided(result.capabilities))
    }

    @Test
    fun `didOpen triggers publishDiagnostics that awaitDiagnostics receives`(@TempDir dir: Path) {
        val file = (dir / "Greeter.kt").also { it.writeText("class Greeter") }
        lsp.initialize()
        lsp.didOpen(file)

        val diagnostics = lsp.awaitDiagnostics(file, timeoutMillis = 2_000)
        assertEquals(1, diagnostics.size)
        assertEquals("unused symbol", diagnostics.first().message)
    }

    @Test
    fun `references returns the server locations`(@TempDir dir: Path) {
        val file = (dir / "Greeter.kt").also { it.writeText("class Greeter") }
        lsp.initialize()

        val refs = lsp.references(file, Position(0, 0))
        assertEquals(1, refs.size)
    }

    @Test
    fun `workspaceSymbols normalizes the server hits into candidates`() {
        lsp.initialize()

        val candidates = lsp.workspaceSymbols("Greeter")

        assertEquals(1, candidates.size)
        val candidate = candidates.first()
        assertEquals("Greeter", candidate.name)
        assertEquals(SymbolKind.Class, candidate.kind)
        assertEquals("com.example", candidate.containerName)
        assertEquals("file:///tmp/Greeter.kt", candidate.uri)
        assertEquals(Position(0, 0), candidate.position)
    }

    @Test
    fun `documentSymbols returns the server symbols`(@TempDir dir: Path) {
        val file = (dir / "Greeter.kt").also { it.writeText("class Greeter") }
        lsp.initialize()

        val symbols = lsp.documentSymbols(file)
        assertEquals(1, symbols.size)
        assertEquals("Greeter", symbols.first().right.name)
    }
}

/** Small bridge so the test can assert on the capability helper without exposing it widely. */
private object LspClientTestAccess {
    fun referencesProvided(caps: ServerCapabilities): Boolean {
        val provider = caps.referencesProvider ?: return false
        return if (provider.isLeft) provider.left == true else true
    }
}
