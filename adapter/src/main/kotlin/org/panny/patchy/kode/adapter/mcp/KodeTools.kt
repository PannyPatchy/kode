package org.panny.patchy.kode.adapter.mcp

import org.panny.patchy.kode.adapter.presenter.JsonPresenter
import org.panny.patchy.kode.application.usecase.AnalyzeErrorsUseCase
import org.panny.patchy.kode.application.usecase.BuildTreeUseCase
import org.panny.patchy.kode.application.usecase.FindReferencesUseCase
import org.panny.patchy.kode.application.usecase.FindTestsUseCase
import org.panny.patchy.kode.application.usecase.ListSymbolsUseCase
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.nio.file.Path

/** The analysis use cases exposed as MCP tools (one per read-only CLI command). */
data class AnalysisUseCases(
    val buildTree: BuildTreeUseCase,
    val analyzeErrors: AnalyzeErrorsUseCase,
    val listSymbols: ListSymbolsUseCase,
    val findReferences: FindReferencesUseCase,
    val findTests: FindTestsUseCase,
)

/**
 * Builds the MCP tools from the same use cases + presenter the CLI commands
 * use, so tool output is byte-identical to the CLI's stdout. Like the CLI, the
 * project root is resolved from the server process's working directory —
 * register the server per-project (documented in the README).
 */
class KodeTools(
    private val useCases: AnalysisUseCases,
    private val presenter: JsonPresenter,
    private val cwd: () -> Path = { Path.of("").toAbsolutePath() },
) {
    fun all(): List<McpTool> = listOf(
        tool(
            name = "kode_tree",
            description = "Return the whole-project file tree of the Kotlin project (JSON, via file system)",
            schema = objectSchema(),
        ) { _ -> presenter.renderTree(useCases.buildTree.execute(cwd())) },
        tool(
            name = "kode_errors",
            description = "Return errors/warnings and surrounding snippets for a Kotlin file (JSON, via LSP)",
            schema = objectSchema(FILE_PROPERTY),
        ) { args -> presenter.renderErrors(useCases.analyzeErrors.execute(cwd(), args.requiredString("file"))) },
        tool(
            name = "kode_symbols",
            description = "Return classes/functions/properties in a Kotlin file (JSON, via LSP)",
            schema = objectSchema(FILE_PROPERTY),
        ) { args -> presenter.renderSymbols(useCases.listSymbols.execute(cwd(), args.requiredString("file"))) },
        tool(
            name = "kode_refs",
            description = "Return references to a Kotlin class/function (JSON, via LSP)",
            schema = objectSchema(TARGET_PROPERTY to "Class or function name to find references to"),
        ) { args -> presenter.renderRefs(useCases.findReferences.execute(cwd(), args.requiredString("target"))) },
        tool(
            name = "kode_test",
            description = "Return related test classes/functions for a Kotlin class/function " +
                "(JSON, via naming heuristics)",
            schema = objectSchema(TARGET_PROPERTY to "Class or function name to find tests for"),
        ) { args -> presenter.renderTests(useCases.findTests.execute(cwd(), args.requiredString("target"))) },
    )

    private fun tool(
        name: String,
        description: String,
        schema: JsonObject,
        handler: (JsonObject) -> String,
    ): McpTool = object : McpTool {
        override val name: String = name
        override val description: String = description
        override val inputSchema: JsonObject = schema
        override fun call(arguments: JsonObject): String = handler(arguments)
    }

    private fun JsonObject.requiredString(key: String): String =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
            ?: throw McpInvalidParamsException("Missing required string argument: $key")

    /** Draft-07 style object schema from `name -> description` string properties. */
    private fun objectSchema(vararg properties: Pair<String, String>): JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            properties.forEach { (name, description) ->
                putJsonObject(name) {
                    put("type", "string")
                    put("description", description)
                }
            }
        }
        if (properties.isNotEmpty()) {
            putJsonArray("required") { properties.forEach { add(JsonPrimitive(it.first)) } }
        }
    }

    private companion object {
        val FILE_PROPERTY = "file" to "Path to a Kotlin file, relative to the project root (or absolute)"
        const val TARGET_PROPERTY = "target"
    }
}
