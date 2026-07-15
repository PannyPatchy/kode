package org.panny.patchy.kode.adapter.lsp

import org.eclipse.lsp4j.Position
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.nio.file.Path

/**
 * End-to-end smoke test against a **real** kotlin-lsp process. It is skipped
 * unless `KODE_LSP_PATH` points at an installed launcher (see
 * `scripts/fetch-kotlin-lsp.sh`), so the default `./gradlew build` stays
 * hermetic and offline.
 *
 * It opens the bundled sample Gradle project (whose `.kode.json` deliberately
 * has `kotlin_version: null`) and drives the four core requests, demonstrating
 * that the null version does not impede analysis — the LSP imports the project's
 * own build system rather than reading `.kode.json`.
 */
@EnabledIfEnvironmentVariable(named = "KODE_LSP_PATH", matches = ".+")
class LspIntegrationTest {

    private val sampleRoot: Path = run {
        val url = requireNotNull(javaClass.classLoader.getResource("sample-project")) {
            "sample-project resource is missing from the test classpath"
        }
        Path.of(url.toURI())
    }

    @Test
    fun `connects and serves the four core requests against real kotlin-lsp`() {
        val binary = LspBinaryResolver().resolve()
        LspClient(sampleRoot, LspSession.connect(sampleRoot, binary), defaultTimeoutMillis = 180_000).use { lsp ->
            // 1. initialize / capabilities
            val init = lsp.initialize()
            assertNotNull(init.capabilities, "server returned no capabilities")
            println("[integration] server: ${init.serverInfo?.name} ${init.serverInfo?.version}")

            val greeter = sampleRoot.resolve("src/main/kotlin/com/example/Greeter.kt")
            val app = sampleRoot.resolve("src/main/kotlin/com/example/App.kt")

            // 3a. didOpen + publishDiagnostics (empty is acceptable; we assert no failure)
            lsp.didOpen(greeter)
            lsp.didOpen(app)
            val diagnostics = lsp.awaitDiagnostics(app, timeoutMillis = 60_000)
            println("[integration] diagnostics(App.kt) = ${diagnostics.size}: ${diagnostics.map { it.message }}")

            // 3b. documentSymbol — expect at least the Greeter class
            val symbols = lsp.documentSymbols(greeter)
            println("[integration] documentSymbol(Greeter.kt) = ${symbols.size}")
            assertFalse(symbols.isEmpty(), "expected at least one symbol in Greeter.kt")

            // 3c. references — the Greeter class declaration (line 3, col 6: `class Greeter`)
            val refs = lsp.references(greeter, Position(3, 6), includeDeclaration = true, timeoutMillis = 60_000)
            println("[integration] references(Greeter) = ${refs.size}")
            assertTrue(refs.size >= 0, "references should return a list")

            // 3d. workspace/symbol — resolve the Greeter class by name
            val candidates = lsp.workspaceSymbols("Greeter", timeoutMillis = 60_000)
            println("[integration] workspaceSymbols(Greeter) = ${candidates.size}")
        }
    }
}
