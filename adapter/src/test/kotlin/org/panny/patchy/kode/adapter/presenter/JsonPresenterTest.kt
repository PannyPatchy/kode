package org.panny.patchy.kode.adapter.presenter

import org.panny.patchy.kode.application.dto.ErrorsResult
import org.panny.patchy.kode.application.dto.RefsResult
import org.panny.patchy.kode.application.dto.SymbolsResult
import org.panny.patchy.kode.application.dto.TestsResult
import org.panny.patchy.kode.application.dto.TreeResult
import org.panny.patchy.kode.domain.entity.Diagnostic
import org.panny.patchy.kode.domain.entity.FileTreeNode
import org.panny.patchy.kode.domain.entity.Reference
import org.panny.patchy.kode.domain.entity.Symbol
import org.panny.patchy.kode.domain.entity.TestClass
import org.panny.patchy.kode.domain.error.NoProjectRootException
import org.panny.patchy.kode.domain.valueobject.CodeSnippet
import org.panny.patchy.kode.domain.valueobject.FilePath
import org.panny.patchy.kode.domain.valueobject.Severity
import org.panny.patchy.kode.domain.valueobject.SourcePosition
import org.panny.patchy.kode.domain.valueobject.SymbolName
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JsonPresenterTest {

    private val presenter = JsonPresenter()

    @Test
    fun `renders error envelope with code message and details`() {
        val json = presenter.renderError(
            ErrorEnvelope.of(NoProjectRootException(details = "no settings file")),
        )

        assertTrue(json.contains("\"code\": \"NO_PROJECT_ROOT\""), json)
        assertTrue(json.contains("\"message\": \"No Gradle project root found\""), json)
        assertTrue(json.contains("\"details\": \"no settings file\""), json)
    }

    @Test
    fun `omits details when absent`() {
        val json = presenter.renderError(ErrorEnvelope.of(NoProjectRootException()))

        assertTrue(!json.contains("\"details\""), json)
    }

    @Test
    fun `renders tree with directory keys, root nodes without name, files without children`() {
        val result = TreeResult(
            listOf(
                TreeResult.TreeRoot(
                    dir = FilePath("src/main/kotlin"),
                    node = FileTreeNode.Directory(
                        name = "kotlin",
                        children = listOf(
                            FileTreeNode.File("Foo.kt"),
                            FileTreeNode.Directory("service", listOf(FileTreeNode.File("FooService.kt"))),
                        ),
                    ),
                ),
            ),
        )

        val json = presenter.renderTree(result)

        assertTrue(json.contains("\"src/main/kotlin\":"), json)
        assertTrue(json.contains("\"type\": \"directory\""), json)
        assertTrue(json.contains("\"name\": \"service\""), json)
        assertFalse(json.contains("\"name\": \"kotlin\""), json)
        assertTrue(json.contains("\"name\": \"Foo.kt\""), json)
    }

    @Test
    fun `renders symbols with snake_case keys and omits unknown types`() {
        val result = SymbolsResult(
            file = FilePath("src/main/kotlin/Foo.kt"),
            classes = listOf(
                Symbol.ClassSymbol(
                    SymbolName("FooClass"),
                    listOf(Symbol.Field("bar", "String", mutable = false)),
                ),
            ),
            functions = listOf(
                Symbol.FunctionSymbol(
                    SymbolName("doSomething"),
                    listOf(Symbol.Argument("input", "String"), Symbol.Argument("count", null)),
                    returnType = null,
                ),
            ),
            topLevelProperties = listOf(Symbol.PropertySymbol(SymbolName("CONSTANT"), "String", mutable = false)),
        )

        val json = presenter.renderSymbols(result)

        assertTrue(json.contains("\"top_level_properties\""), json)
        assertTrue(json.contains("\"mutable\": false"), json)
        assertFalse(json.contains("\"return_type\""), json)
        assertTrue(json.contains("\"name\": \"count\"") && !json.contains("\"count\",\n            \"type\""), json)
    }

    @Test
    fun `renders errors with lowercase severity`() {
        val result = ErrorsResult(
            file = FilePath("src/main/kotlin/Foo.kt"),
            diagnostics = listOf(
                Diagnostic(
                    SourcePosition(42, 10),
                    Severity.ERROR,
                    "Type mismatch: expected String, found Int",
                    CodeSnippet("val x: String = 42"),
                ),
            ),
        )

        val json = presenter.renderErrors(result)

        assertTrue(json.contains("\"severity\": \"error\""), json)
        assertTrue(json.contains("\"line\": 42"), json)
        assertTrue(json.contains("\"snippet\": \"val x: String = 42\""), json)
    }

    @Test
    fun `renders refs and empty results as empty arrays`() {
        val json = presenter.renderRefs(
            RefsResult(
                SymbolName("FooClass"),
                listOf(Reference(FilePath("src/Bar.kt"), SourcePosition(10, 5), CodeSnippet("val foo = FooClass()"))),
            ),
        )
        assertTrue(json.contains("\"target\": \"FooClass\""), json)
        assertTrue(json.contains("\"column\": 5"), json)

        val empty = presenter.renderRefs(RefsResult(SymbolName("NoSuch"), emptyList()))
        assertTrue(empty.contains("\"refs\": []"), empty)
    }

    @Test
    fun `renders tests with class as the key`() {
        val json = presenter.renderTests(
            TestsResult(
                SymbolName("FooClass"),
                listOf(
                    TestClass(
                        SymbolName("FooClassTest"),
                        FilePath("src/test/kotlin/FooClassTest.kt"),
                        listOf(SymbolName("testSomething")),
                    ),
                ),
            ),
        )

        assertTrue(json.contains("\"class\": \"FooClassTest\""), json)
        assertTrue(json.contains("\"functions\": ["), json)
    }
}
