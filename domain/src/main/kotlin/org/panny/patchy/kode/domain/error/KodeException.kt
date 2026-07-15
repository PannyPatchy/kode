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

/**
 * No `.kode.json` was found searching upward from the working directory. Every
 * command except `init` requires it (design ch. 03 §1).
 */
class NoProjectConfigException(
    details: String? = "Run 'kode init' at the project root first.",
) : KodeException(ErrorCode.NO_PROJECT_CONFIG, "No .kode.json found", details)

/** A CLI argument is malformed or points at something that does not exist. */
class InvalidArgumentException(
    message: String,
    details: String? = null,
) : KodeException(ErrorCode.INVALID_ARGUMENT, message, details)

/** `.kode.json` exists but is corrupt or missing required fields. */
class InvalidConfigException(
    details: String? = null,
    cause: Throwable? = null,
) : KodeException(ErrorCode.INVALID_CONFIG, "Invalid .kode.json", details, cause)

/**
 * The Kotlin LSP binary could not be located. `kode` does not bundle it; the
 * user must install it separately (design ch. 04). [details] carries install
 * guidance for the AI consumer.
 */
class LspNotFoundException(
    details: String? = null,
    cause: Throwable? = null,
) : KodeException(ErrorCode.LSP_NOT_FOUND, "Kotlin LSP binary not found", details, cause)

/** An LSP request or notification did not complete within its time budget. */
class LspTimeoutException(
    details: String? = null,
    cause: Throwable? = null,
) : KodeException(ErrorCode.LSP_TIMEOUT, "Kotlin LSP request timed out", details, cause)

/** The LSP server does not advertise a capability a command depends on. */
class LspCapabilityUnsupportedException(
    capability: String,
    details: String? = null,
) : KodeException(
    ErrorCode.LSP_CAPABILITY_UNSUPPORTED,
    "Kotlin LSP does not support required capability: $capability",
    details,
)

/** The LSP process exited unexpectedly or the JSON-RPC channel broke. */
class LspCrashedException(
    details: String? = null,
    cause: Throwable? = null,
) : KodeException(ErrorCode.LSP_CRASHED, "Kotlin LSP process crashed", details, cause)
