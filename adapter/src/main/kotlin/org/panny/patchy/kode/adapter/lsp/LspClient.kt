package org.panny.patchy.kode.adapter.lsp

import org.eclipse.lsp4j.ClientCapabilities
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DidOpenTextDocumentParams
import org.eclipse.lsp4j.DocumentSymbol
import org.eclipse.lsp4j.DocumentSymbolCapabilities
import org.eclipse.lsp4j.DocumentSymbolParams
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.InitializedParams
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.PublishDiagnosticsCapabilities
import org.eclipse.lsp4j.ReferenceContext
import org.eclipse.lsp4j.ReferenceParams
import org.eclipse.lsp4j.ReferencesCapabilities
import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.lsp4j.SymbolCapabilities
import org.eclipse.lsp4j.SymbolInformation
import org.eclipse.lsp4j.TextDocumentClientCapabilities
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.eclipse.lsp4j.TextDocumentItem
import org.eclipse.lsp4j.WorkspaceClientCapabilities
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.panny.patchy.kode.domain.error.LspCapabilityUnsupportedException
import org.panny.patchy.kode.domain.error.LspCrashedException
import org.panny.patchy.kode.domain.error.LspTimeoutException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * High-level facade over an [LspSession], used by the `errors` / `refs` /
 * `symbols` commands. It owns capability negotiation, the four core requests,
 * per-request timeouts, and translation of LSP/transport failures into the
 * domain's `KodeException` hierarchy.
 *
 * One client wraps one short-lived session (design ch. 04 §3); call [close]
 * (or use it as an [AutoCloseable]) when done.
 */
class LspClient(
    private val root: Path,
    private val session: LspSession,
    private val defaultTimeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
) : AutoCloseable {

    private var serverCapabilities: ServerCapabilities? = null

    /** Send `initialize` (declaring our capabilities) followed by `initialized`. */
    fun initialize(): InitializeResult {
        @Suppress("DEPRECATION") // rootUri is still what kotlin-lsp consumes for the workspace root.
        val params = InitializeParams().apply {
            processId = ProcessHandle.current().pid().toInt()
            rootUri = root.toUri().toString()
            capabilities = clientCapabilities()
        }
        val result = awaitOrThrow(session.server.initialize(params), defaultTimeoutMillis, "initialize")
        serverCapabilities = result.capabilities
        session.server.initialized(InitializedParams())
        return result
    }

    /** Open [file] (`textDocument/didOpen`) so the server starts analyzing it. */
    fun didOpen(file: Path, languageId: String = KOTLIN_LANGUAGE_ID) {
        ensureAlive()
        val item = TextDocumentItem(file.toUri().toString(), languageId, 1, Files.readString(file))
        session.server.textDocumentService.didOpen(DidOpenTextDocumentParams(item))
    }

    /** Wait for `publishDiagnostics` for [file] to settle (empty list is valid). */
    fun awaitDiagnostics(
        file: Path,
        timeoutMillis: Long = defaultTimeoutMillis,
    ): List<Diagnostic> {
        ensureAlive()
        val future = session.client.awaitDiagnostics(file.toUri().toString(), timeoutMillis)
        // The client self-completes at timeoutMillis; allow a small grace beyond that.
        return awaitOrThrow(future, timeoutMillis + GRACE_MILLIS, "publishDiagnostics")
    }

    /** `textDocument/references` at [position] within [file]. */
    fun references(
        file: Path,
        position: Position,
        includeDeclaration: Boolean = false,
        timeoutMillis: Long = defaultTimeoutMillis,
    ): List<Location> {
        ensureAlive()
        requireCapability(isProvided(capabilities().referencesProvider), "textDocument/references")
        val params = ReferenceParams(
            TextDocumentIdentifier(file.toUri().toString()),
            position,
            ReferenceContext(includeDeclaration),
        )
        return awaitOrThrow(session.server.textDocumentService.references(params), timeoutMillis, "references")
            .orEmpty()
            .map { it }
    }

    /** `textDocument/documentSymbol` for [file] (hierarchical or flat). */
    fun documentSymbols(
        file: Path,
        timeoutMillis: Long = defaultTimeoutMillis,
    ): List<Either<SymbolInformation, DocumentSymbol>> {
        ensureAlive()
        requireCapability(isProvided(capabilities().documentSymbolProvider), "textDocument/documentSymbol")
        val params = DocumentSymbolParams(TextDocumentIdentifier(file.toUri().toString()))
        return awaitOrThrow(session.server.textDocumentService.documentSymbol(params), timeoutMillis, "documentSymbol")
            .orEmpty()
    }

    override fun close() = session.close()

    private fun capabilities(): ServerCapabilities =
        serverCapabilities ?: throw LspCrashedException(details = "initialize() was not called")

    private fun ensureAlive() {
        if (!session.isAlive) throw LspCrashedException(details = "LSP process is no longer running")
    }

    private fun requireCapability(provided: Boolean, name: String) {
        if (!provided) throw LspCapabilityUnsupportedException(name)
    }

    private fun <T> awaitOrThrow(future: CompletableFuture<T>, timeoutMillis: Long, what: String): T =
        try {
            future.get(timeoutMillis, TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            future.cancel(true)
            throw LspTimeoutException(details = "$what did not respond within ${timeoutMillis}ms", cause = e)
        } catch (e: ExecutionException) {
            throw LspCrashedException(details = "$what failed: ${e.cause?.message ?: e.message}", cause = e)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw LspCrashedException(details = "$what was interrupted", cause = e)
        }

    private companion object {
        const val KOTLIN_LANGUAGE_ID = "kotlin"
        const val DEFAULT_TIMEOUT_MILLIS = 30_000L
        const val GRACE_MILLIS = 2_000L

        fun clientCapabilities(): ClientCapabilities = ClientCapabilities().apply {
            textDocument = TextDocumentClientCapabilities().apply {
                publishDiagnostics = PublishDiagnosticsCapabilities(true)
                references = ReferencesCapabilities()
                documentSymbol = DocumentSymbolCapabilities().apply {
                    hierarchicalDocumentSymbolSupport = true
                }
            }
            workspace = WorkspaceClientCapabilities().apply {
                symbol = SymbolCapabilities()
            }
        }

        fun isProvided(either: Either<Boolean, *>?): Boolean = when {
            either == null -> false
            either.isLeft -> either.left == true
            else -> true
        }
    }
}
