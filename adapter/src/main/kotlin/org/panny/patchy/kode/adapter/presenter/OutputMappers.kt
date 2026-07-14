package org.panny.patchy.kode.adapter.presenter

import org.panny.patchy.kode.application.dto.ErrorsResult
import org.panny.patchy.kode.application.dto.RefsResult
import org.panny.patchy.kode.application.dto.SymbolsResult
import org.panny.patchy.kode.application.dto.TestsResult
import org.panny.patchy.kode.application.dto.TreeResult
import org.panny.patchy.kode.domain.entity.FileTreeNode
import org.panny.patchy.kode.domain.entity.Symbol

private const val TYPE_DIRECTORY = "directory"
private const val TYPE_FILE = "file"

/** Map use case results into the serializable output DTOs (design ch. 03). */

internal fun TreeResult.toDto(): TreeOutput =
    TreeOutput(roots.associate { it.dir.value to it.node.toRootDto() })

/** The map key already names the root directory, so root nodes omit `name`. */
private fun FileTreeNode.Directory.toRootDto(): TreeNodeDto =
    TreeNodeDto(type = TYPE_DIRECTORY, children = children.map { it.toDto() })

private fun FileTreeNode.toDto(): TreeNodeDto = when (this) {
    is FileTreeNode.Directory -> TreeNodeDto(TYPE_DIRECTORY, name, children.map { it.toDto() })
    is FileTreeNode.File -> TreeNodeDto(TYPE_FILE, name)
}

internal fun SymbolsResult.toDto(): SymbolsOutput =
    SymbolsOutput(
        file = file.value,
        classes = classes.map { clazz ->
            SymbolsOutput.ClassDto(clazz.name.value, clazz.fields.map { it.toDto() })
        },
        functions = functions.map { it.toDto() },
        topLevelProperties = topLevelProperties.map {
            SymbolsOutput.PropertyDto(it.name.value, it.type, it.mutable)
        },
    )

private fun Symbol.Field.toDto(): SymbolsOutput.PropertyDto =
    SymbolsOutput.PropertyDto(name, type, mutable)

private fun Symbol.FunctionSymbol.toDto(): SymbolsOutput.FunctionDto =
    SymbolsOutput.FunctionDto(
        name = name.value,
        arguments = arguments.map { SymbolsOutput.ArgumentDto(it.name, it.type) },
        returnType = returnType,
    )

internal fun ErrorsResult.toDto(): ErrorsOutput =
    ErrorsOutput(
        file = file.value,
        diagnostics = diagnostics.map {
            ErrorsOutput.DiagnosticDto(
                line = it.position.line,
                column = it.position.column,
                severity = it.severity.name.lowercase(),
                message = it.message,
                snippet = it.snippet.text,
            )
        },
    )

internal fun RefsResult.toDto(): RefsOutput =
    RefsOutput(
        target = target.value,
        refs = refs.map {
            RefsOutput.RefDto(
                file = it.file.value,
                line = it.position.line,
                column = it.position.column,
                snippet = it.snippet.text,
            )
        },
    )

internal fun TestsResult.toDto(): TestsOutput =
    TestsOutput(
        target = target.value,
        tests = tests.map { test ->
            TestsOutput.TestDto(
                className = test.name.value,
                file = test.file.value,
                functions = test.functions.map { it.value },
            )
        },
    )
