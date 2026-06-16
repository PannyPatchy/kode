package dev.kode.adapter.presenter

import dev.kode.adapter.config.kodeJson
import dev.kode.adapter.config.toDto
import dev.kode.domain.entity.KotlinProject
import kotlinx.serialization.encodeToString

/**
 * Converts use case results / errors into the JSON strings `kode` emits.
 * stdout is reserved for result JSON; error envelopes go to stderr (design ch. 07).
 */
class JsonPresenter {
    /** Render the `kode init` result (the `.kode.json` schema). */
    fun renderProject(project: KotlinProject): String = kodeJson.encodeToString(project.toDto())

    /** Render an error envelope. */
    fun renderError(envelope: ErrorEnvelope): String = kodeJson.encodeToString(envelope)
}
