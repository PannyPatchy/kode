package dev.kode.adapter.presenter

import dev.kode.domain.error.ErrorCode
import dev.kode.domain.error.KodeException
import kotlinx.serialization.Serializable

/** Machine-readable error envelope written to stderr on failure (design ch. 07). */
@Serializable
data class ErrorEnvelope(val error: ErrorBody) {
    @Serializable
    data class ErrorBody(
        val code: String,
        val message: String,
        val details: String? = null,
    )

    companion object {
        fun of(e: KodeException): ErrorEnvelope =
            ErrorEnvelope(ErrorBody(e.code.name, e.message, e.details))

        /** Round any unexpected throwable down to INTERNAL_ERROR (no sensitive data). */
        fun internalError(e: Throwable): ErrorEnvelope =
            ErrorEnvelope(
                ErrorBody(
                    code = ErrorCode.INTERNAL_ERROR.name,
                    message = "Unexpected internal error",
                    details = e.message,
                ),
            )
    }
}
