package org.panny.patchy.kode.adapter.lsp

import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.SymbolInformation
import org.eclipse.lsp4j.SymbolKind
import org.eclipse.lsp4j.WorkspaceSymbol

/** One `workspace/symbol` hit, normalized across the two LSP result shapes. */
data class WorkspaceSymbolCandidate(
    val name: String,
    val kind: SymbolKind?,
    val containerName: String?,
    val uri: String,
    val position: Position?,
)

internal fun SymbolInformation.toCandidate() = WorkspaceSymbolCandidate(
    name = name,
    kind = kind,
    containerName = containerName?.takeIf { it.isNotBlank() },
    uri = location.uri,
    position = location.range?.start,
)

/**
 * A `WorkspaceSymbolLocation` (right side) carries no range; such candidates
 * keep a null position and callers must skip them.
 */
internal fun WorkspaceSymbol.toCandidate(): WorkspaceSymbolCandidate {
    val loc = location
    return WorkspaceSymbolCandidate(
        name = name,
        kind = kind,
        containerName = containerName?.takeIf { it.isNotBlank() },
        uri = if (loc.isLeft) loc.left.uri else loc.right.uri,
        position = if (loc.isLeft) loc.left.range?.start else null,
    )
}
