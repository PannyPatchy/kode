package org.panny.patchy.kode.adapter.mcp

import kotlinx.serialization.json.JsonObject

/**
 * One MCP tool exposed by `kode mcp`. Implementations reuse the existing use
 * cases + [org.panny.patchy.kode.adapter.presenter.JsonPresenter], so a tool's
 * text content is byte-identical to the corresponding CLI command's stdout.
 */
interface McpTool {
    /** Tool name as advertised in `tools/list` (e.g. `kode_tree`). */
    val name: String

    val description: String

    /** JSON Schema (draft-07 style object schema) for the tool arguments. */
    val inputSchema: JsonObject

    /**
     * Run the tool and return the result JSON (the presenter output).
     *
     * @throws McpInvalidParamsException when a required argument is missing or
     *   malformed (mapped to JSON-RPC `-32602`).
     * @throws org.panny.patchy.kode.domain.error.KodeException on tool-execution
     *   failure (mapped to an `isError: true` tool result, not a protocol error).
     */
    fun call(arguments: JsonObject): String
}

/** A `tools/call` with missing/malformed arguments (JSON-RPC `-32602`). */
class McpInvalidParamsException(message: String) : RuntimeException(message)
