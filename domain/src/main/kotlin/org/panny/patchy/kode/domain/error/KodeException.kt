package org.panny.patchy.kode.domain.error

/**
 * Base type for all errors that cross `kode`'s process boundary. At the boundary
 * they are converted into the error-envelope JSON written to stderr, and the
 * process exits with [ErrorCode.exitCode] (see design ch. 07).
 */
sealed class KodeException(
    val code: ErrorCode,
    override val message: String,
    val details: String? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {
    val exitCode: Int get() = code.exitCode
}

/** No project root (`build.gradle(.kts)`) could be found from the start directory. */
class NoProjectRootException(
    details: String? = null,
) : KodeException(ErrorCode.NO_PROJECT_ROOT, "No Gradle project root found", details)

/**
 * A build tool other than Gradle (e.g. Maven `pom.xml`) was detected. Only Gradle
 * is supported in v1; Maven support is out of scope and planned for the future.
 */
class UnsupportedBuildToolException(
    buildTool: String,
    details: String? = null,
) : KodeException(ErrorCode.UNSUPPORTED_BUILD_TOOL, "Unsupported build tool: $buildTool", details)

/** `.kode.json` exists but is corrupt or missing required fields. */
class InvalidConfigException(
    details: String? = null,
    cause: Throwable? = null,
) : KodeException(ErrorCode.INVALID_CONFIG, "Invalid .kode.json", details, cause)

/** A command's behaviour is not implemented yet (scaffold stub). */
class NotImplementedCommandException(
    command: String,
) : KodeException(
    ErrorCode.NOT_IMPLEMENTED,
    "Command not implemented yet: $command",
    "This command is a scaffold stub and will be implemented in a future iteration.",
)
