package org.panny.patchy.kode.adapter.lsp

import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.MessageActionItem
import org.eclipse.lsp4j.MessageParams
import org.eclipse.lsp4j.PublishDiagnosticsParams
import org.eclipse.lsp4j.ShowMessageRequestParams
import org.eclipse.lsp4j.services.LanguageClient
import java.io.PrintStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * The client half of the LSP session. LSP4J delivers server-initiated messages
 * here. Two concerns are handled:
 *
 * - **Diagnostics are push-based** (`textDocument/publishDiagnostics`), so they
 *   are buffered per-URI and exposed through [awaitDiagnostics], which resolves
 *   once a file's diagnostics have stayed quiet for a short *settle* window
 *   (capped by an overall timeout). See design ch. 04 §4.
 * - **Server logs/messages** are routed strictly to `stderr` so `kode`'s stdout
 *   stays reserved for JSON results (design ch. 07).
 */
class KodeLanguageClient(
    private val errStream: PrintStream = System.err,
) : LanguageClient, AutoCloseable {

    private val latestByUri = ConcurrentHashMap<String, List<Diagnostic>>()
    private val waiters = ConcurrentHashMap<String, Waiter>()
    private val scheduler = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "kode-lsp-diagnostics").apply { isDaemon = true }
    }

    private class Waiter(
        val future: CompletableFuture<List<Diagnostic>>,
        val settleMillis: Long,
    ) {
        @Volatile var settleTask: ScheduledFuture<*>? = null
    }

    /**
     * Wait for the diagnostics of [uri] to settle.
     *
     * @param uri the document URI (as the server reports it).
     * @param timeoutMillis upper bound; on expiry resolves with whatever is
     *   currently known (possibly empty) rather than failing — absence of
     *   diagnostics is a valid result.
     * @param settleMillis quiet window after the latest notification before the
     *   result is considered stable.
     */
    fun awaitDiagnostics(
        uri: String,
        timeoutMillis: Long,
        settleMillis: Long = DEFAULT_SETTLE_MILLIS,
    ): CompletableFuture<List<Diagnostic>> {
        val waiter = Waiter(CompletableFuture(), settleMillis)
        waiters[uri] = waiter

        // If a notification already arrived before the await was registered,
        // start the settle countdown immediately.
        if (latestByUri.containsKey(uri)) {
            rescheduleSettle(uri, waiter)
        }
        scheduler.schedule({ resolve(uri, waiter) }, timeoutMillis, TimeUnit.MILLISECONDS)
        return waiter.future
    }

    override fun publishDiagnostics(params: PublishDiagnosticsParams) {
        val uri = params.uri
        latestByUri[uri] = params.diagnostics.orEmpty()
        waiters[uri]?.let { rescheduleSettle(uri, it) }
    }

    private fun rescheduleSettle(uri: String, waiter: Waiter) {
        synchronized(waiter) {
            waiter.settleTask?.cancel(false)
            waiter.settleTask = scheduler.schedule(
                { resolve(uri, waiter) },
                waiter.settleMillis,
                TimeUnit.MILLISECONDS,
            )
        }
    }

    private fun resolve(uri: String, waiter: Waiter) {
        if (waiter.future.complete(latestByUri[uri].orEmpty())) {
            waiters.remove(uri, waiter)
            waiter.settleTask?.cancel(false)
        }
    }

    override fun telemetryEvent(`object`: Any?) = Unit

    override fun showMessage(params: MessageParams) {
        errStream.println("[kotlin-lsp] ${params.type}: ${params.message}")
    }

    override fun showMessageRequest(
        params: ShowMessageRequestParams,
    ): CompletableFuture<MessageActionItem> {
        errStream.println("[kotlin-lsp] ${params.type}: ${params.message}")
        return CompletableFuture.completedFuture(null)
    }

    override fun logMessage(params: MessageParams) {
        errStream.println("[kotlin-lsp] ${params.type}: ${params.message}")
    }

    override fun close() {
        scheduler.shutdownNow()
        waiters.values.forEach { it.future.complete(emptyList()) }
        waiters.clear()
    }

    companion object {
        const val DEFAULT_SETTLE_MILLIS: Long = 800
    }
}
