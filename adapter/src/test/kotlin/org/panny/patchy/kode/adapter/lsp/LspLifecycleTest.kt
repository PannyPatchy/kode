package org.panny.patchy.kode.adapter.lsp

import io.mockk.every
import io.mockk.mockk
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.lsp4j.services.LanguageServer
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.panny.patchy.kode.domain.error.LspCapabilityUnsupportedException
import org.panny.patchy.kode.domain.error.LspCrashedException
import org.panny.patchy.kode.domain.error.LspNotFoundException
import org.panny.patchy.kode.domain.error.LspTimeoutException
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import kotlin.io.path.div
import kotlin.io.path.writeText

/**
 * Lifecycle and error-handling coverage: process start failure, request
 * timeout, unexpected exit (crash), missing capability, and graceful teardown.
 */
class LspLifecycleTest {

    @Test
    fun `connect to a missing binary throws LspNotFoundException`(@TempDir dir: Path) {
        assertThrows<LspNotFoundException> {
            LspSession.connect(dir, dir / "does-not-exist-kotlin-lsp")
        }
    }

    @Test
    fun `initialize against an unresponsive process times out`(@TempDir dir: Path) {
        // `sleep` never speaks LSP, so initialize never gets a response.
        val process = ProcessBuilder("sleep", "30").start()
        val session = sessionFor(process)
        val lsp = LspClient(dir, session, defaultTimeoutMillis = 700)

        assertThrows<LspTimeoutException> { lsp.initialize() }
        process.destroyForcibly()
    }

    @Test
    fun `close terminates the child process`() {
        val process = ProcessBuilder("sleep", "30").start()
        val session = sessionFor(process)

        session.close()

        assertFalse(process.isAlive)
    }

    @Test
    fun `requests on a dead session throw LspCrashedException`(@TempDir dir: Path) {
        val process = mockk<Process>(relaxed = true) { every { isAlive } returns false }
        val session = LspSession(process, mockk(relaxed = true), KodeLanguageClient())
        val lsp = LspClient(dir, session)

        assertThrows<LspCrashedException> { lsp.documentSymbols(dir / "X.kt") }
    }

    @Test
    fun `requesting before initialize throws LspCrashedException`(@TempDir dir: Path) {
        val process = mockk<Process>(relaxed = true) { every { isAlive } returns true }
        val session = LspSession(process, mockk(relaxed = true), KodeLanguageClient())
        val lsp = LspClient(dir, session)

        assertThrows<LspCrashedException> { lsp.references(dir / "X.kt", Position(0, 0)) }
    }

    @Test
    fun `missing server capability throws LspCapabilityUnsupportedException`(@TempDir dir: Path) {
        val file = (dir / "X.kt").also { it.writeText("class X") }
        val server = mockk<LanguageServer>(relaxed = true) {
            // Empty capabilities: referencesProvider is not advertised.
            every { initialize(any()) } returns
                CompletableFuture.completedFuture(InitializeResult(ServerCapabilities()))
        }
        val process = mockk<Process>(relaxed = true) { every { isAlive } returns true }
        val lsp = LspClient(dir, LspSession(process, server, KodeLanguageClient()))
        lsp.initialize()

        assertThrows<LspCapabilityUnsupportedException> { lsp.references(file, Position(0, 0)) }
    }

    private fun sessionFor(process: Process): LspSession {
        val client = KodeLanguageClient()
        val launcher = org.eclipse.lsp4j.launch.LSPLauncher.createClientLauncher(
            client,
            process.inputStream,
            process.outputStream,
        )
        launcher.startListening()
        return LspSession(process, launcher.remoteProxy, client)
    }
}
