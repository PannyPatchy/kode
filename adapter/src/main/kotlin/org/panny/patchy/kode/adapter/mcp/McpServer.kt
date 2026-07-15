package org.panny.patchy.kode.adapter.mcp

import org.panny.patchy.kode.adapter.presenter.ErrorEnvelope
import org.panny.patchy.kode.adapter.presenter.JsonPresenter
import org.panny.patchy.kode.domain.error.KodeException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.BufferedReader
import java.io.Writer

/**
 * Compact wire encoding for MCP stdio: messages are newline-delimited JSON, so
 * the shared pretty-printing `kodeJson` must never touch the wire. (The pretty
 * presenter JSON is fine *inside* a `content[].text` string value.)
 */
internal val mcpWireJson: Json = Json {
    encodeDefaults = false
    explicitNulls = false
    ignoreUnknownKeys = true
}

/**
 * A minimal MCP server over stdio: newline-delimited JSON-RPC 2.0 handling
 * exactly what a tools-only server needs (`initialize`, `ping`, `tools/list`,
 * `tools/call`; other notifications are ignored, other requests get `-32601`).
 *
 * Hand-rolled on kotlinx.serialization instead of the official MCP SDK to keep
 * kode's dependency footprint and native-image config at zero extra (design
 * ch. 09); the SDK can be swapped in behind `kode mcp` if resources/prompts
 * are ever needed.
 *
 * Tool-execution failures are returned as `isError: true` tool *results* (the
 * MCP spec requires this so the model can see them), carrying the same error
 * envelope JSON the CLI writes to stderr (design ch. 07).
 */
class McpServer(
    private val tools: List<McpTool>,
    private val presenter: JsonPresenter,
    private val serverVersion: String,
) {
    /** Blocks reading [input] line-by-line until EOF; responses go to [output]. */
    fun serve(input: BufferedReader, output: Writer) {
        input.lineSequence().filter { it.isNotBlank() }.forEach { line ->
            handleLine(line)?.let { response ->
                output.write(response.toString()) // JsonElement.toString() is single-line
                output.write("\n")
                output.flush()
            }
        }
    }

    /** @return the response for one wire line, or `null` for notifications. */
    internal fun handleLine(line: String): JsonObject? = when (val parsed = parseMessage(line)) {
        is Parsed.Failure -> parsed.response
        is Parsed.Message -> dispatch(parsed.message)
    }

    private sealed interface Parsed {
        data class Message(val message: JsonObject) : Parsed
        data class Failure(val response: JsonObject) : Parsed
    }

    private fun parseMessage(line: String): Parsed = try {
        when (val element = mcpWireJson.parseToJsonElement(line)) {
            is JsonObject -> Parsed.Message(element)
            else -> Parsed.Failure(errorResponse(JsonNull, INVALID_REQUEST, "Request must be a JSON object"))
        }
    } catch (e: SerializationException) {
        Parsed.Failure(errorResponse(JsonNull, PARSE_ERROR, "Parse error: ${e.message}"))
    }

    private fun dispatch(message: JsonObject): JsonObject? {
        val id = message["id"]
        val method = (message["method"] as? JsonPrimitive)?.contentOrNull
        val params = message["params"] as? JsonObject
        return when {
            method == null -> id?.let { errorResponse(it, INVALID_REQUEST, "Missing method") }
            method.startsWith("notifications/") -> null
            id == null -> null // a request without an id cannot be answered; treat as a notification
            method == "initialize" -> resultResponse(id, initializeResult(params))
            method == "ping" -> resultResponse(id, JsonObject(emptyMap()))
            method == "tools/list" -> resultResponse(id, toolsListResult())
            method == "tools/call" -> toolsCall(id, params)
            else -> errorResponse(id, METHOD_NOT_FOUND, "Method not found: $method")
        }
    }

    private fun initializeResult(params: JsonObject?): JsonObject {
        val requested = (params?.get("protocolVersion") as? JsonPrimitive)?.contentOrNull
        val negotiated = if (requested in SUPPORTED_PROTOCOL_VERSIONS) requested else LATEST_PROTOCOL_VERSION
        return buildJsonObject {
            put("protocolVersion", negotiated)
            putJsonObject("capabilities") {
                putJsonObject("tools") { put("listChanged", false) }
            }
            putJsonObject("serverInfo") {
                put("name", "kode")
                put("version", serverVersion)
            }
        }
    }

    private fun toolsListResult(): JsonObject = buildJsonObject {
        putJsonArray("tools") {
            tools.forEach { tool ->
                addJsonObject {
                    put("name", tool.name)
                    put("description", tool.description)
                    put("inputSchema", tool.inputSchema)
                }
            }
        }
    }

    private fun toolsCall(id: JsonElement, params: JsonObject?): JsonObject {
        val name = (params?.get("name") as? JsonPrimitive)?.contentOrNull
        val tool = name?.let { wanted -> tools.find { it.name == wanted } }
        val arguments = params?.get("arguments") as? JsonObject ?: JsonObject(emptyMap())
        return when {
            name == null -> errorResponse(id, INVALID_PARAMS, "Missing tool name")
            tool == null -> errorResponse(id, INVALID_PARAMS, "Unknown tool: $name")
            else -> invokeTool(id, tool, arguments)
        }
    }

    private fun invokeTool(id: JsonElement, tool: McpTool, arguments: JsonObject): JsonObject = try {
        resultResponse(id, toolResult(tool.call(arguments), isError = false))
    } catch (e: McpInvalidParamsException) {
        errorResponse(id, INVALID_PARAMS, e.message ?: "Invalid params")
    } catch (e: KodeException) {
        resultResponse(id, toolResult(presenter.renderError(ErrorEnvelope.of(e)), isError = true))
    } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
        // The server must keep running whatever a tool throws; round the error
        // down to INTERNAL_ERROR like the CLI's top-level safety net (design ch. 07).
        resultResponse(id, toolResult(presenter.renderError(ErrorEnvelope.internalError(e)), isError = true))
    }

    companion object {
        const val LATEST_PROTOCOL_VERSION: String = "2025-06-18"
        val SUPPORTED_PROTOCOL_VERSIONS: Set<String> = setOf("2025-06-18", "2025-03-26")

        private const val PARSE_ERROR = -32700
        private const val INVALID_REQUEST = -32600
        private const val METHOD_NOT_FOUND = -32601
        private const val INVALID_PARAMS = -32602
    }
}

private const val JSONRPC_VERSION = "2.0"

private fun toolResult(text: String, isError: Boolean): JsonObject = buildJsonObject {
    putJsonArray("content") {
        addJsonObject {
            put("type", "text")
            put("text", text)
        }
    }
    put("isError", isError)
}

private fun resultResponse(id: JsonElement, result: JsonObject): JsonObject = buildJsonObject {
    put("jsonrpc", JSONRPC_VERSION)
    put("id", id)
    put("result", result)
}

private fun errorResponse(id: JsonElement, code: Int, message: String): JsonObject = buildJsonObject {
    put("jsonrpc", JSONRPC_VERSION)
    put("id", id)
    putJsonObject("error") {
        put("code", code)
        put("message", message)
    }
}
