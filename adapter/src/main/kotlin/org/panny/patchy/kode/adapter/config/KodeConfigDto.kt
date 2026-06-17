package org.panny.patchy.kode.adapter.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Serializable representation of `.kode.json`. The [version] field prepares for
 * future schema evolution (design ch. 06).
 */
@Serializable
data class KodeConfigDto(
    val version: String = SCHEMA_VERSION,
    val project: ProjectDto,
    val lsp: LspDto? = null,
) {
    companion object {
        const val SCHEMA_VERSION: String = "1.0"
    }
}

@Serializable
data class ProjectDto(
    val root: String,
    @SerialName("build_tool") val buildTool: String,
    @SerialName("kotlin_version") val kotlinVersion: String,
    @SerialName("source_dirs") val sourceDirs: List<String>,
    @SerialName("test_dirs") val testDirs: List<String>,
)

@Serializable
data class LspDto(val path: String? = null)
