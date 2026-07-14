package org.panny.patchy.kode.adapter.lsp

import java.nio.file.Path

/**
 * Runs a block against an initialized [LspClient]. The port adapters depend on
 * this seam instead of spawning processes directly, so tests can inject an
 * in-JVM fake server.
 */
interface LspClientProvider {
    fun <T> withClient(root: Path, block: (LspClient) -> T): T
}

/**
 * Production [LspClientProvider]: resolves the `kotlin-lsp` binary, spawns one
 * short-lived session per call (design ch. 04 §3), initializes it, and always
 * shuts it down — even when the block throws (design ch. 07 §5).
 */
class LspSessionFactory(
    private val binaryResolver: LspBinaryResolver = LspBinaryResolver(),
    /** Reads the optional `lsp.path` from `.kode.json` for the given root. */
    private val configuredLspPath: (Path) -> String? = { null },
) : LspClientProvider {

    override fun <T> withClient(root: Path, block: (LspClient) -> T): T {
        val binary = binaryResolver.resolve(configuredLspPath(root))
        return LspClient(root, LspSession.connect(root, binary)).use { client ->
            client.initialize()
            block(client)
        }
    }
}
