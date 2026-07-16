package org.panny.patchy.kode.adapter.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import org.panny.patchy.kode.adapter.mcp.McpServer
import java.nio.file.Path

/**
 * `kode mcp` — run kode as an MCP server over stdio.
 *
 * stdout carries only JSON-RPC responses (newline-delimited); the startup note
 * goes to stderr like every other human-readable message (design ch. 07). The
 * server analyzes the project at the process's working directory, same as the
 * CLI commands.
 */
class McpCommand(
    private val server: McpServer,
) : CliktCommand(name = "mcp") {

    override fun help(context: Context): String =
        "Run kode as an MCP server over stdio " +
            "(tools: kode_tree, kode_errors, kode_symbols, kode_refs, kode_test)"

    override fun run() {
        echo("kode MCP server listening on stdio (project root: ${Path.of("").toAbsolutePath()})", err = true)
        server.serve(System.`in`.bufferedReader(), System.out.writer())
    }
}
