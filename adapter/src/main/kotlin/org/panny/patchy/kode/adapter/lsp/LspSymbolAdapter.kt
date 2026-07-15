package org.panny.patchy.kode.adapter.lsp

import org.panny.patchy.kode.domain.entity.Symbol
import org.panny.patchy.kode.domain.port.SymbolPort
import org.panny.patchy.kode.domain.valueobject.FilePath
import org.panny.patchy.kode.domain.valueobject.ProjectRoot
import java.io.IOException
import java.nio.file.Files

/**
 * LSP implementation of [SymbolPort] for `kode symbols`: fetch the
 * `documentSymbol` hierarchy and normalize it via [DocumentSymbolMapper].
 */
class LspSymbolAdapter(
    private val provider: LspClientProvider,
) : SymbolPort {

    override fun symbols(root: ProjectRoot, file: FilePath): List<Symbol> {
        val absolute = root.resolve(file.value)
        val sourceLines = try {
            Files.readString(absolute).lines()
        } catch (_: IOException) {
            emptyList()
        }
        return provider.withClient(root.path) { client ->
            client.didOpen(absolute)
            DocumentSymbolMapper(sourceLines).map(client.documentSymbols(absolute))
        }
    }
}
