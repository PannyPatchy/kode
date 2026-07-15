package org.panny.patchy.kode.adapter.lsp

import org.panny.patchy.kode.domain.entity.Diagnostic
import org.panny.patchy.kode.domain.port.DiagnosticsPort
import org.panny.patchy.kode.domain.service.SnippetExtractor
import org.panny.patchy.kode.domain.valueobject.FilePath
import org.panny.patchy.kode.domain.valueobject.ProjectRoot

/**
 * LSP implementation of [DiagnosticsPort] for `kode errors`: open the file and
 * wait for the push-based `publishDiagnostics` to settle (design ch. 04 §4).
 */
class LspDiagnosticsAdapter(
    private val provider: LspClientProvider,
    private val snippetExtractor: SnippetExtractor,
) : DiagnosticsPort {

    override fun diagnostics(root: ProjectRoot, file: FilePath): List<Diagnostic> {
        val absolute = root.resolve(file.value)
        return provider.withClient(root.path) { client ->
            client.didOpen(absolute)
            val snippets = FileSnippets(snippetExtractor)
            client.awaitDiagnostics(absolute).map { diagnostic ->
                val position = diagnostic.range.start.toDomain()
                Diagnostic(
                    position = position,
                    severity = diagnostic.severity.toDomain(),
                    message = diagnostic.message.orEmpty(),
                    snippet = snippets.at(absolute, position.line),
                )
            }
        }
    }
}
