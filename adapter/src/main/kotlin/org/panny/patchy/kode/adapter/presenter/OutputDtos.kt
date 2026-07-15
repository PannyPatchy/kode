package org.panny.patchy.kode.adapter.presenter

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Serializable output schemas for the analysis commands (design ch. 03).
 * Field names are snake_case via @SerialName; nullable fields are omitted from
 * the JSON when null (explicitNulls = false in kodeJson).
 */

/** `kode tree` output: one entry per configured source/test directory. */
@Serializable
data class TreeOutput(val tree: Map<String, TreeNodeDto>)

/** One node of the tree. Root nodes carry no [name]; files carry no [children]. */
@Serializable
data class TreeNodeDto(
    val type: String,
    val name: String? = null,
    val children: List<TreeNodeDto>? = null,
)

/** `kode symbols <file>` output. */
@Serializable
data class SymbolsOutput(
    val file: String,
    val classes: List<ClassDto>,
    val functions: List<FunctionDto>,
    @SerialName("top_level_properties") val topLevelProperties: List<PropertyDto>,
) {
    @Serializable
    data class ClassDto(val name: String, val fields: List<PropertyDto>)

    @Serializable
    data class FunctionDto(
        val name: String,
        val arguments: List<ArgumentDto>,
        @SerialName("return_type") val returnType: String? = null,
    )

    @Serializable
    data class ArgumentDto(val name: String, val type: String? = null)

    @Serializable
    data class PropertyDto(
        val name: String,
        val type: String? = null,
        val mutable: Boolean? = null,
    )
}

/** `kode errors <file>` output. */
@Serializable
data class ErrorsOutput(val file: String, val diagnostics: List<DiagnosticDto>) {
    @Serializable
    data class DiagnosticDto(
        val line: Int,
        val column: Int,
        val severity: String,
        val message: String,
        val snippet: String,
    )
}

/** `kode refs <target>` output. */
@Serializable
data class RefsOutput(val target: String, val refs: List<RefDto>) {
    @Serializable
    data class RefDto(
        val file: String,
        val line: Int,
        val column: Int,
        val snippet: String,
    )
}

/** `kode test <target>` output. */
@Serializable
data class TestsOutput(val target: String, val tests: List<TestDto>) {
    @Serializable
    data class TestDto(
        @SerialName("class") val className: String,
        val file: String,
        val functions: List<String>,
    )
}

/**
 * `kode doctor` output. Unlike the analysis outputs this may carry absolute,
 * machine-specific paths — doctor reports on the machine, not the project.
 */
@Serializable
data class DoctorOutput(
    val ok: Boolean,
    val checks: List<CheckDto>,
    val lsp: LspDto? = null,
    val installed: InstalledDto? = null,
) {
    @Serializable
    data class CheckDto(
        val name: String,
        val status: String,
        val detail: String,
        val hint: String? = null,
    )

    @Serializable
    data class LspDto(
        val source: String,
        val path: String,
        @SerialName("installed_version") val installedVersion: String? = null,
    )

    /** Present only when this run installed the LSP (`--install-lsp`). */
    @Serializable
    data class InstalledDto(
        val version: String,
        val launcher: String,
        val checksum: String,
    )
}
