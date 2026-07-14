package org.panny.patchy.kode.domain.entity

import org.panny.patchy.kode.domain.valueobject.SymbolName

/**
 * A declaration found in a file by `kode symbols`, normalized to `kode`'s
 * vocabulary (design ch. 03). Type and mutability information is best-effort:
 * `null` means the language server did not expose it, and the presenter omits
 * the field from the JSON output rather than guessing.
 */
sealed interface Symbol {
    val name: SymbolName

    /** A class/interface/object/enum declaration and its properties. */
    data class ClassSymbol(
        override val name: SymbolName,
        val fields: List<Field>,
    ) : Symbol

    /** A function declaration (top level or member). */
    data class FunctionSymbol(
        override val name: SymbolName,
        val arguments: List<Argument>,
        val returnType: String?,
    ) : Symbol

    /** A top-level property declaration. */
    data class PropertySymbol(
        override val name: SymbolName,
        val type: String?,
        val mutable: Boolean?,
    ) : Symbol

    /** A property belonging to a [ClassSymbol]. */
    data class Field(
        val name: String,
        val type: String?,
        val mutable: Boolean?,
    )

    /** A function parameter. */
    data class Argument(
        val name: String,
        val type: String?,
    )
}
