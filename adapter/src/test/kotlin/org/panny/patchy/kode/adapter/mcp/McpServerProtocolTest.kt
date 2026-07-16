package org.panny.patchy.kode.adapter.mcp

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.panny.patchy.kode.adapter.presenter.JsonPresenter
import org.panny.patchy.kode.application.dto.TreeResult
import org.panny.patchy.kode.application.usecase.AnalyzeErrorsUseCase
import org.panny.patchy.kode.application.usecase.BuildTreeUseCase
import org.panny.patchy.kode.application.usecase.FindReferencesUseCase
import org.panny.patchy.kode.application.usecase.FindTestsUseCase
import org.panny.patchy.kode.application.usecase.ListSymbolsUseCase
import org.panny.patchy.kode.domain.error.NoProjectConfigException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.StringReader
import java.io.StringWriter
import java.nio.file.Path

class McpServerProtocolTest {

    private val buildTree = mockk<BuildTreeUseCase>()
    private val analyzeErrors = mockk<AnalyzeErrorsUseCase>()
    private val listSymbols = mockk<ListSymbolsUseCase>()
    private val findReferences = mockk<FindReferencesUseCase>()
    private val findTests = mockk<FindTestsUseCase>()
    private val presenter = JsonPresenter()

    private val server = McpServer(
        tools = KodeTools(
            AnalysisUseCases(buildTree, analyzeErrors, listSymbols, findReferences, findTests),
            presenter,
            cwd = { Path.of("/tmp/project") },
        ).all(),
        presenter = presenter,
        serverVersion = "0.0-test",
    )

    private fun handle(line: String): JsonObject? = server.handleLine(line)

    private fun request(id: Int, method: String, params: String? = null): String =
        """{"jsonrpc":"2.0","id":$id,"method":"$method"${params?.let { ""","params":$it""" } ?: ""}}"""

    @Test
    fun `negotiates a supported protocol version`() {
        val response = handle(
            request(1, "initialize", """{"protocolVersion":"2025-03-26","capabilities":{}}"""),
        )

        val result = response!!["result"]!!.jsonObject
        assertEquals("2025-03-26", result["protocolVersion"]!!.jsonPrimitive.content)
        assertEquals("kode", result["serverInfo"]!!.jsonObject["name"]!!.jsonPrimitive.content)
    }

    @Test
    fun `answers an unknown protocol version with the latest supported`() {
        val response = handle(request(1, "initialize", """{"protocolVersion":"1999-01-01"}"""))

        assertEquals(
            McpServer.LATEST_PROTOCOL_VERSION,
            response!!["result"]!!.jsonObject["protocolVersion"]!!.jsonPrimitive.content,
        )
    }

    @Test
    fun `lists the five kode tools with schemas`() {
        val response = handle(request(2, "tools/list"))

        val tools = response!!["result"]!!.jsonObject["tools"]!!.jsonArray
        val names = tools.map { it.jsonObject["name"]!!.jsonPrimitive.content }
        assertEquals(
            listOf("kode_tree", "kode_errors", "kode_symbols", "kode_refs", "kode_test"),
            names,
        )
        tools.forEach {
            assertEquals("object", it.jsonObject["inputSchema"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        }
    }

    @Test
    fun `calls a tool and wraps the presenter JSON as text content`() {
        every { buildTree.execute(Path.of("/tmp/project")) } returns TreeResult(emptyList())

        val response = handle(request(3, "tools/call", """{"name":"kode_tree","arguments":{}}"""))

        val result = response!!["result"]!!.jsonObject
        assertEquals(JsonPrimitive(false), result["isError"])
        val text = result["content"]!!.jsonArray[0].jsonObject["text"]!!.jsonPrimitive.content
        assertEquals(presenter.renderTree(TreeResult(emptyList())), text)
    }

    @Test
    fun `returns a KodeException as an isError tool result, not a protocol error`() {
        every { analyzeErrors.execute(any(), any()) } throws NoProjectConfigException()

        val response = handle(request(4, "tools/call", """{"name":"kode_errors","arguments":{"file":"Foo.kt"}}"""))

        assertNull(response!!["error"])
        val result = response["result"]!!.jsonObject
        assertEquals(JsonPrimitive(true), result["isError"])
        val text = result["content"]!!.jsonArray[0].jsonObject["text"]!!.jsonPrimitive.content
        assertTrue(text.contains("NO_PROJECT_CONFIG"))
    }

    @Test
    fun `rejects a missing required argument with -32602`() {
        val response = handle(request(5, "tools/call", """{"name":"kode_errors","arguments":{}}"""))

        assertEquals(-32602, response!!["error"]!!.jsonObject["code"]!!.jsonPrimitive.content.toInt())
    }

    @Test
    fun `rejects an unknown tool with -32602`() {
        val response = handle(request(6, "tools/call", """{"name":"nope","arguments":{}}"""))

        assertEquals(-32602, response!!["error"]!!.jsonObject["code"]!!.jsonPrimitive.content.toInt())
    }

    @Test
    fun `answers an unknown method with -32601`() {
        val response = handle(request(7, "resources/list"))

        assertEquals(-32601, response!!["error"]!!.jsonObject["code"]!!.jsonPrimitive.content.toInt())
    }

    @Test
    fun `answers a garbage line with -32700`() {
        val response = handle("{ not json")

        assertEquals(-32700, response!!["error"]!!.jsonObject["code"]!!.jsonPrimitive.content.toInt())
    }

    @Test
    fun `ignores notifications`() {
        assertNull(handle("""{"jsonrpc":"2.0","method":"notifications/initialized"}"""))
        assertNull(handle("""{"jsonrpc":"2.0","method":"notifications/cancelled"}"""))
    }

    @Test
    fun `answers ping with an empty result`() {
        val response = handle(request(8, "ping"))

        assertEquals(JsonObject(emptyMap()), response!!["result"]!!.jsonObject)
    }

    @Test
    fun `serve writes exactly one line per response`() {
        every { buildTree.execute(Path.of("/tmp/project")) } returns TreeResult(emptyList())
        val input = listOf(
            request(1, "initialize", """{"protocolVersion":"2025-06-18"}"""),
            """{"jsonrpc":"2.0","method":"notifications/initialized"}""",
            request(2, "tools/list"),
            request(3, "tools/call", """{"name":"kode_tree","arguments":{}}"""),
        ).joinToString("\n")
        val output = StringWriter()

        server.serve(StringReader(input).buffered(), output)

        val lines = output.toString().trim().lines()
        assertEquals(3, lines.size) // the notification gets no response
        lines.forEach { line ->
            assertFalse(line.isBlank())
            assertEquals("2.0", mcpWireJson.parseToJsonElement(line).jsonObject["jsonrpc"]!!.jsonPrimitive.content)
        }
    }
}
