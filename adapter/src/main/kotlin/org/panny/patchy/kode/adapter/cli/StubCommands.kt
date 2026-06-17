package org.panny.patchy.kode.adapter.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.arguments.argument
import org.panny.patchy.kode.domain.error.NotImplementedCommandException

/**
 * Scaffold stubs for the commands not yet implemented. They declare their real
 * arguments so `kode <cmd> --help` documents the intended I/O, but currently
 * abort with NOT_IMPLEMENTED (exit 1).
 */

class ErrorsCommand : CliktCommand(name = "errors") {
    private val file by argument(help = "Source file to analyze")
    override fun help(context: Context): String =
        "Return errors/warnings and surrounding snippets for a file (JSON, via LSP)"

    override fun run(): Unit = throw NotImplementedCommandException("errors $file")
}

class RefsCommand : CliktCommand(name = "refs") {
    private val target by argument(help = "Class or function name, e.g. FooClass or FooClass.doSomething")
    override fun help(context: Context): String =
        "Return references to a class/function (JSON, via LSP)"

    override fun run(): Unit = throw NotImplementedCommandException("refs $target")
}

class TestCommand : CliktCommand(name = "test") {
    private val target by argument(help = "Class or function name")
    override fun help(context: Context): String =
        "Return related test classes/functions for a target (JSON, via Gradle)"

    override fun run(): Unit = throw NotImplementedCommandException("test $target")
}

class TreeCommand : CliktCommand(name = "tree") {
    override fun help(context: Context): String =
        "Return the whole-project file tree (JSON, via file system)"

    override fun run(): Unit = throw NotImplementedCommandException("tree")
}

class SymbolsCommand : CliktCommand(name = "symbols") {
    private val file by argument(help = "Source file to inspect")
    override fun help(context: Context): String =
        "Return classes/functions/properties in a file (JSON, via LSP)"

    override fun run(): Unit = throw NotImplementedCommandException("symbols $file")
}
