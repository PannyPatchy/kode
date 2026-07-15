package org.panny.patchy.kode.adapter.fs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.panny.patchy.kode.domain.entity.KotlinProject
import org.panny.patchy.kode.domain.service.SymbolMatcher
import org.panny.patchy.kode.domain.valueobject.BuildTool
import org.panny.patchy.kode.domain.valueobject.FilePath
import org.panny.patchy.kode.domain.valueobject.ProjectRoot
import org.panny.patchy.kode.domain.valueobject.SymbolName
import java.nio.file.Files
import java.nio.file.Path

class HeuristicTestDiscoveryAdapterTest {

    @TempDir
    lateinit var root: Path

    private val adapter = HeuristicTestDiscoveryAdapter(SymbolMatcher())

    private fun project() = KotlinProject(
        root = ProjectRoot(root),
        buildTool = BuildTool.GRADLE,
        kotlinVersion = null,
        sourceDirs = listOf(FilePath("src/main/kotlin")),
        testDirs = listOf(FilePath("src/test/kotlin")),
    )

    private fun writeTestFile(relative: String, content: String) {
        val file = root.resolve(relative)
        Files.createDirectories(file.parent)
        Files.writeString(file, content)
    }

    @Test
    fun `finds a test class by naming convention and extracts test functions`() {
        writeTestFile(
            "src/test/kotlin/com/example/GreeterTest.kt",
            """
            package com.example

            import org.junit.jupiter.api.Test

            class GreeterTest {
                @Test
                fun greetsByName() {}

                @Test
                fun `greets the world by default`() {}

                fun helper() {}
            }
            """.trimIndent(),
        )

        val tests = adapter.testsFor(project(), SymbolName("Greeter"))

        assertEquals(1, tests.size)
        assertEquals(SymbolName("GreeterTest"), tests[0].name)
        assertEquals(FilePath("src/test/kotlin/com/example/GreeterTest.kt"), tests[0].file)
        assertEquals(
            listOf(SymbolName("greetsByName"), SymbolName("greets the world by default")),
            tests[0].functions,
        )
    }

    @Test
    fun `matches Tests and Spec suffixes and member targets`() {
        writeTestFile(
            "src/test/kotlin/GreeterSpec.kt",
            """
            class GreeterSpec {
                @Test
                fun checksGreeting() {}
            }
            """.trimIndent(),
        )

        assertEquals(1, adapter.testsFor(project(), SymbolName("Greeter")).size)
        assertEquals(1, adapter.testsFor(project(), SymbolName("Greeter.greet")).size)
    }

    @Test
    fun `matches by class declaration even when the file name differs`() {
        writeTestFile(
            "src/test/kotlin/AllTests.kt",
            """
            class FooTest {
                @Test
                fun one() {}
            }
            """.trimIndent(),
        )

        val tests = adapter.testsFor(project(), SymbolName("Foo"))

        assertEquals(1, tests.size)
        assertEquals(FilePath("src/test/kotlin/AllTests.kt"), tests[0].file)
    }

    @Test
    fun `annotation and function on the same line are detected`() {
        writeTestFile(
            "src/test/kotlin/BarTest.kt",
            "class BarTest { @Test fun inline() {} }",
        )

        assertEquals(listOf(SymbolName("inline")), adapter.testsFor(project(), SymbolName("Bar"))[0].functions)
    }

    @Test
    fun `unrelated classes and missing dirs yield an empty result`() {
        writeTestFile("src/test/kotlin/OtherTest.kt", "class OtherTest { @Test fun x() {} }")

        assertTrue(adapter.testsFor(project(), SymbolName("Greeter")).isEmpty())
        assertTrue(
            adapter.testsFor(project().copy(testDirs = listOf(FilePath("nope"))), SymbolName("Other")).isEmpty(),
        )
    }
}
