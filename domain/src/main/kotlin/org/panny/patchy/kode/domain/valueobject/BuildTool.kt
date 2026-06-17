package org.panny.patchy.kode.domain.valueobject

/** The build tool that defines a Kotlin project. Only Gradle is supported in v1. */
enum class BuildTool(val id: String) {
    GRADLE("gradle"),
    ;

    companion object {
        fun fromId(id: String): BuildTool =
            entries.firstOrNull { it.id == id }
                ?: throw IllegalArgumentException("Unknown build tool: $id")
    }
}
