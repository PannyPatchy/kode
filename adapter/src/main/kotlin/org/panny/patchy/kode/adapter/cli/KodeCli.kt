package org.panny.patchy.kode.adapter.cli

import com.github.ajalt.clikt.core.CliktCommand

/**
 * Root `kode` command. A no-op container; behaviour lives in the subcommands,
 * which are wired in the Composition Root (`bootstrap`).
 *
 * `kode --help` doubles as the manual for the AI consumer (design ch. 00).
 */
class KodeCli : CliktCommand(
    name = "kode",
    help = "A Kotlin codebase comprehension CLI for AI agents. All commands output JSON.",
) {
    override fun run() = Unit
}
