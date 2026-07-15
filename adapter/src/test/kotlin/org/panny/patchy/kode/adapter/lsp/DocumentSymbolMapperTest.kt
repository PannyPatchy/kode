package org.panny.patchy.kode.adapter.lsp

import org.eclipse.lsp4j.DocumentSymbol
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.SymbolInformation
import org.eclipse.lsp4j.SymbolKind
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.panny.patchy.kode.domain.entity.Symbol
import org.panny.patchy.kode.domain.valueobject.SymbolName

class DocumentSymbolMapperTest {

    private val source = """
        package com.example

        const val CONSTANT: String = "x"

        class FooClass(val bar: String) {
            var count: Int = 0

            fun doSomething(input: String, count: Int): Boolean = true
        }

        fun topLevel(items: List<Map<String, Int>>): Unit {}
    """.trimIndent().lines()

    private fun node(
        name: String,
        kind: SymbolKind,
        line: Int,
        detail: String? = null,
        children: List<DocumentSymbol> = emptyList(),
    ): DocumentSymbol {
        val range = Range(Position(line, 0), Position(line, 1))
        return DocumentSymbol(name, kind, range, range).apply {
            this.detail = detail
            this.children = children
        }
    }

    private fun map(vararg roots: DocumentSymbol): List<Symbol> =
        DocumentSymbolMapper(source).map(roots.map { Either.forRight(it) })

    @Test
    fun `maps a class with fields, member functions surface in the flat list`() {
        val clazz = node(
            "FooClass",
            SymbolKind.Class,
            line = 4,
            children = listOf(
                node("bar", SymbolKind.Property, line = 4, detail = "val bar: String"),
                node("count", SymbolKind.Property, line = 5),
                node("doSomething", SymbolKind.Method, line = 7),
            ),
        )

        val symbols = map(clazz)

        val classSymbol = symbols.filterIsInstance<Symbol.ClassSymbol>().single()
        assertEquals(SymbolName("FooClass"), classSymbol.name)
        assertEquals(
            listOf(
                Symbol.Field("bar", "String", mutable = false),
                Symbol.Field("count", "Int", mutable = true),
            ),
            classSymbol.fields,
        )
        val function = symbols.filterIsInstance<Symbol.FunctionSymbol>().single()
        assertEquals(SymbolName("doSomething"), function.name)
        assertEquals(
            listOf(Symbol.Argument("input", "String"), Symbol.Argument("count", "Int")),
            function.arguments,
        )
        assertEquals("Boolean", function.returnType)
    }

    @Test
    fun `parses arguments from the detail signature, nested generics intact`() {
        val function = node(
            "topLevel",
            SymbolKind.Function,
            line = 10,
            detail = "(items: List<Map<String, Int>>): Unit",
        )

        val symbol = map(function).filterIsInstance<Symbol.FunctionSymbol>().single()

        assertEquals(listOf(Symbol.Argument("items", "List<Map<String, Int>>")), symbol.arguments)
        assertEquals("Unit", symbol.returnType)
    }

    @Test
    fun `a bare detail is used as the return type`() {
        val function = node("noSourceLine", SymbolKind.Function, line = 99, detail = "Boolean")

        val symbol = map(function).filterIsInstance<Symbol.FunctionSymbol>().single()

        assertEquals("Boolean", symbol.returnType)
        assertEquals(emptyList<Symbol.Argument>(), symbol.arguments)
    }

    @Test
    fun `top-level property gets type and mutability from the source line`() {
        val property = node("CONSTANT", SymbolKind.Constant, line = 2)

        val symbol = map(property).filterIsInstance<Symbol.PropertySymbol>().single()

        assertEquals("String", symbol.type)
        assertEquals(false, symbol.mutable)
    }

    @Test
    fun `unknown details degrade to nulls`() {
        val property = node("mystery", SymbolKind.Property, line = 99)

        val symbol = map(property).filterIsInstance<Symbol.PropertySymbol>().single()

        assertNull(symbol.type)
        assertNull(symbol.mutable)
    }

    @Test
    fun `flat SymbolInformation fallback classifies by kind and container`() {
        val location = Location("file:///tmp/Foo.kt", Range(Position(0, 0), Position(0, 1)))
        val flat = listOf<Either<SymbolInformation, DocumentSymbol>>(
            Either.forLeft(SymbolInformation("FooClass", SymbolKind.Class, location)),
            Either.forLeft(SymbolInformation("bar", SymbolKind.Property, location, "FooClass")),
            Either.forLeft(SymbolInformation("topLevel", SymbolKind.Function, location)),
            Either.forLeft(SymbolInformation("CONSTANT", SymbolKind.Constant, location)),
        )

        val symbols = DocumentSymbolMapper(source).map(flat)

        val classSymbol = symbols.filterIsInstance<Symbol.ClassSymbol>().single()
        assertEquals(listOf(Symbol.Field("bar", null, null)), classSymbol.fields)
        assertEquals(SymbolName("topLevel"), symbols.filterIsInstance<Symbol.FunctionSymbol>().single().name)
        assertEquals(SymbolName("CONSTANT"), symbols.filterIsInstance<Symbol.PropertySymbol>().single().name)
    }
}
