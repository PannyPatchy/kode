package org.panny.patchy.kode.domain.error

private const val EXIT_GENERAL = 1
private const val EXIT_CONFIG = 2
private const val EXIT_LSP = 3
private const val EXIT_GRADLE = 4

/**
 * Stable, machine-readable error identifiers and their associated process exit
 * codes (see design ch. 07). The AI consumer branches coarsely on the exit code
 * and precisely on the [name].
 */
enum class ErrorCode(val exitCode: Int) {
    // exit 1 — general errors
    INTERNAL_ERROR(EXIT_GENERAL),
    INVALID_ARGUMENT(EXIT_GENERAL),
    NOT_IMPLEMENTED(EXIT_GENERAL),

    // exit 2 — missing / invalid config or project
    NO_PROJECT_CONFIG(EXIT_CONFIG),
    INVALID_CONFIG(EXIT_CONFIG),
    NO_PROJECT_ROOT(EXIT_CONFIG),
    UNSUPPORTED_BUILD_TOOL(EXIT_CONFIG),

    // exit 3 — LSP failures
    LSP_NOT_FOUND(EXIT_LSP),
    LSP_TIMEOUT(EXIT_LSP),
    LSP_CAPABILITY_UNSUPPORTED(EXIT_LSP),
    LSP_CRASHED(EXIT_LSP),
    LSP_DOWNLOAD_FAILED(EXIT_LSP),

    // exit 4 — Gradle failures (reserved for future command implementations)
    GRADLE_FAILURE(EXIT_GRADLE),
    GRADLE_TIMEOUT(EXIT_GRADLE),
}
