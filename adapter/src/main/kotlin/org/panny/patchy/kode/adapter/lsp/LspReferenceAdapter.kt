package org.panny.patchy.kode.adapter.lsp

import org.panny.patchy.kode.domain.entity.Reference
import org.panny.patchy.kode.domain.port.ReferencePort
import org.panny.patchy.kode.domain.service.SnippetExtractor
import org.panny.patchy.kode.domain.service.SymbolMatcher
import org.panny.patchy.kode.domain.valueobject.ProjectRoot
import org.panny.patchy.kode.domain.valueobject.SymbolName
import java.nio.file.Path

/**
 * LSP implementation of [ReferencePort] for `kode refs` (design ch. 04 §4):
 * resolve the target name to its definition via `workspace/symbol`, then ask
 * for `textDocument/references` (declaration excluded), all inside one
 * short-lived session.
 *
 * An unresolvable target yields an empty list — "empty is success" (ch. 07 §3).
 * Definitions and references outside the project root are dropped.
 */
class LspReferenceAdapter(
    private val provider: LspClientProvider,
    private val symbolMatcher: SymbolMatcher,
    private val snippetExtractor: SnippetExtractor,
) : ReferencePort {

    override fun references(root: ProjectRoot, target: SymbolName): List<Reference> {
        val spec = symbolMatcher.parse(target)
        return provider.withClient(root.path) { client ->
            val definition = resolveDefinition(client, spec, root.path)
            if (definition == null) {
                emptyList()
            } else {
                collectReferences(client, root, definition)
            }
        }
    }

    private fun resolveDefinition(
        client: LspClient,
        spec: SymbolMatcher.TargetSpec,
        root: Path,
    ): WorkspaceSymbolCandidate? =
        client.workspaceSymbols(spec.member ?: spec.type)
            .asSequence()
            .filter { it.position != null }
            .filter { candidate -> uriToPath(candidate.uri)?.startsWith(root) == true }
            .filter { symbolMatcher.matchesDefinition(spec, simpleName(it.name), it.containerName) }
            // Prefer the least-nested candidate (shortest container) for stability.
            .sortedBy { it.containerName.orEmpty().length }
            .firstOrNull()

    private fun collectReferences(
        client: LspClient,
        root: ProjectRoot,
        definition: WorkspaceSymbolCandidate,
    ): List<Reference> {
        val definitionFile = uriToPath(definition.uri) ?: return emptyList()
        client.didOpen(definitionFile)
        val locations = client.references(
            definitionFile,
            checkNotNull(definition.position),
            includeDeclaration = false,
        )
        val snippets = FileSnippets(snippetExtractor)
        return locations
            .mapNotNull { location ->
                val file = uriToFilePath(root.path, location.uri) ?: return@mapNotNull null
                val position = location.range.start.toDomain()
                Reference(file, position, snippets.at(root.resolve(file.value), position.line))
            }
            .sortedWith(compareBy({ it.file.value }, { it.position.line }, { it.position.column }))
    }

    private fun simpleName(name: String): String = name.substringBefore('(').trim()
}
