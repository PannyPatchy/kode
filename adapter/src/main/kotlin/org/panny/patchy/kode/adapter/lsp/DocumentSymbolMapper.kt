package org.panny.patchy.kode.adapter.lsp

import org.eclipse.lsp4j.DocumentSymbol
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.SymbolInformation
import org.eclipse.lsp4j.SymbolKind
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.panny.patchy.kode.domain.entity.Symbol
import org.panny.patchy.kode.domain.valueobject.SymbolName

/**
 * Normalizes an LSP `documentSymbol` answer into kode's [Symbol] vocabulary
 * (design ch. 03 §6 / 04 §5).
 *
 * Type and mutability details are best effort: first from the symbol's `detail`
 * field, then from the declaration line in [sourceLines]; when neither yields
 * anything they stay `null` and the presenter omits the field. Member functions
 * are reported in the flat `functions` list with their simple names (v1 — the
 * output schema has no per-class methods slot).
 */
internal class DocumentSymbolMapper(private val sourceLines: List<String>) {

    fun map(symbols: List<Either<SymbolInformation, DocumentSymbol>>): List<Symbol> {
        val result = mutableListOf<Symbol>()
        symbols.filter { it.isRight }.forEach { mapNode(it.right, depth = 0, result) }
        mapFlat(symbols.filter { it.isLeft }.map { it.left }, result)
        return result
    }

    private fun mapNode(node: DocumentSymbol, depth: Int, sink: MutableList<Symbol>) {
        val children = node.children.orEmpty()
        when {
            node.kind in CLASS_LIKE -> {
                sink += Symbol.ClassSymbol(SymbolName(simpleName(node.name)), fieldsOf(children))
                children.filter { it.kind !in PROPERTY_LIKE }.forEach { mapNode(it, depth + 1, sink) }
            }
            node.kind in FUNCTION_LIKE -> {
                sink += functionSymbol(node)
                children.forEach { mapNode(it, depth + 1, sink) } // e.g. local classes
            }
            node.kind in PROPERTY_LIKE && depth == 0 -> sink += propertySymbol(node)
            else -> children.forEach { mapNode(it, depth + 1, sink) }
        }
    }

    private fun fieldsOf(children: List<DocumentSymbol>): List<Symbol.Field> =
        children.filter { it.kind in PROPERTY_LIKE }.map { child ->
            val (type, mutable) = propertyInfo(child)
            Symbol.Field(simpleName(child.name), type, mutable)
        }

    private fun functionSymbol(node: DocumentSymbol): Symbol.FunctionSymbol {
        val signature = listOfNotNull(node.detail, node.name, declarationAt(node.selectionRange?.start))
            .firstOrNull { it.contains('(') }
        return Symbol.FunctionSymbol(
            name = SymbolName(simpleName(node.name)),
            arguments = signature?.let(SignatureText::parseArguments).orEmpty(),
            returnType = returnTypeOf(node, signature),
        )
    }

    private fun returnTypeOf(node: DocumentSymbol, signature: String?): String? {
        val detail = node.detail?.trim()?.takeIf { it.isNotBlank() }
        // A parenless, colonless detail is the return type itself (e.g. "Boolean").
        if (detail != null && !detail.contains('(') && !detail.contains(':')) return detail
        val candidates = listOfNotNull(detail, signature, declarationAt(node.selectionRange?.start))
        return candidates.firstNotNullOfOrNull(SignatureText::returnTypeAfterParens)
    }

    private fun propertySymbol(node: DocumentSymbol): Symbol.PropertySymbol {
        val (type, mutable) = propertyInfo(node)
        return Symbol.PropertySymbol(SymbolName(simpleName(node.name)), type, mutable)
    }

    /** Best-effort `type to mutable` from the detail or the declaration line. */
    private fun propertyInfo(node: DocumentSymbol): Pair<String?, Boolean?> {
        val candidates = listOfNotNull(node.detail, declarationAt(node.selectionRange?.start))
        val declared = candidates.firstNotNullOfOrNull(SignatureText::declaredProperty)
        if (declared != null) return declared
        // A bare detail without a declaration keyword is the type itself.
        val detail = node.detail?.trim()?.takeIf { it.isNotBlank() && !it.contains('(') }
        return detail to null
    }

    private fun simpleName(name: String): String =
        name.substringBefore('(').trim().ifBlank { name }

    private fun declarationAt(position: Position?): String? =
        position?.let { sourceLines.getOrNull(it.line) }

    /** Flat (legacy `SymbolInformation`) fallback: classify by kind, no type info. */
    private fun mapFlat(infos: List<SymbolInformation>, sink: MutableList<Symbol>) {
        infos.forEach { info ->
            when {
                info.kind in CLASS_LIKE -> sink += Symbol.ClassSymbol(
                    SymbolName(simpleName(info.name)),
                    infos.filter { it.kind in PROPERTY_LIKE && it.containerName == info.name }
                        .map { Symbol.Field(simpleName(it.name), null, null) },
                )
                info.kind in FUNCTION_LIKE ->
                    sink += Symbol.FunctionSymbol(SymbolName(simpleName(info.name)), emptyList(), null)
                info.kind in PROPERTY_LIKE && info.containerName.isNullOrBlank() ->
                    sink += Symbol.PropertySymbol(SymbolName(simpleName(info.name)), null, null)
            }
        }
    }

    private companion object {
        val CLASS_LIKE = setOf(
            SymbolKind.Class,
            SymbolKind.Interface,
            SymbolKind.Enum,
            SymbolKind.Object,
            SymbolKind.Struct,
        )
        val FUNCTION_LIKE = setOf(SymbolKind.Function, SymbolKind.Method)
        val PROPERTY_LIKE = setOf(SymbolKind.Property, SymbolKind.Field, SymbolKind.Constant)
    }
}
