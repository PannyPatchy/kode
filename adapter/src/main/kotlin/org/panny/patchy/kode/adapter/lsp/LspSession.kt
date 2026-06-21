package org.panny.patchy.kode.adapter.lsp

import org.eclipse.lsp4j.jsonrpc.Launcher
import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.services.LanguageServer
import org.panny.patchy.kode.domain.error.LspNotFoundException
import java.io.IOException
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/**
 * Owns one Kotlin LSP child process and the JSON-RPC wiring to it. A session is
 * short-lived: one is created per command, used, then [close]d (design ch. 04 §3).
 *
 * Framing / JSON-RPC details are delegated to Eclipse LSP4J's [LSPLauncher]; we
 * do not roll our own protocol.
 */
class LspSession internal constructor(
    private val process: Process,
    val server: LanguageServer,
    val client: KodeLanguageClient,
) : AutoCloseable {

    /** Whether the child process is still alive. */
    val isAlive: Boolean get() = process.isAlive

    /**
     * Gracefully stop the session: best-effort `shutdown`/`exit`, then wait a
     * bounded time for the process to leave, force-killing it if it lingers.
     * Always releases the client's resources.
     */
    override fun close() {
        try {
            server.shutdown().orTimeout(SHUTDOWN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS).join()
            server.exit()
        } catch (_: Exception) {
            // The server may already be gone; fall through to forcible teardown.
        } finally {
            client.close()
            if (!process.waitFor(SHUTDOWN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly().waitFor(SHUTDOWN_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
            }
        }
    }

    companion object {
        private const val SHUTDOWN_TIMEOUT_MILLIS: Long = 3_000

        /**
         * Launch [binary] as `kotlin-lsp --stdio` rooted at [root] and connect a
         * [KodeLanguageClient] to it.
         *
         * @throws LspNotFoundException when the process cannot be started.
         */
        fun connect(root: Path, binary: Path): LspSession {
            val process = try {
                ProcessBuilder(binary.toString(), "--stdio")
                    .directory(root.toFile())
                    // Keep LSP logs out of stdout (our JSON channel); route to our stderr.
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start()
            } catch (e: IOException) {
                throw LspNotFoundException(
                    details = "Failed to start LSP process at $binary: ${e.message}",
                    cause = e,
                )
            }
            val client = KodeLanguageClient()
            val launcher: Launcher<LanguageServer> = LSPLauncher.createClientLauncher(
                client,
                process.inputStream,
                process.outputStream,
            )
            launcher.startListening()
            return LspSession(process, launcher.remoteProxy, client)
        }
    }
}
