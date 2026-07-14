package org.panny.patchy.kode.adapter.presenter

import org.panny.patchy.kode.adapter.config.kodeJson
import org.panny.patchy.kode.adapter.config.toDto
import org.panny.patchy.kode.application.dto.ErrorsResult
import org.panny.patchy.kode.application.dto.RefsResult
import org.panny.patchy.kode.application.dto.SymbolsResult
import org.panny.patchy.kode.application.dto.TestsResult
import org.panny.patchy.kode.application.dto.TreeResult
import org.panny.patchy.kode.domain.entity.KotlinProject
import kotlinx.serialization.encodeToString

/**
 * Converts use case results / errors into the JSON strings `kode` emits.
 * stdout is reserved for result JSON; error envelopes go to stderr (design ch. 07).
 */
class JsonPresenter {
    /** Render the `kode init` result (the `.kode.json` schema). */
    fun renderProject(project: KotlinProject): String = kodeJson.encodeToString(project.toDto())

    /** Render the `kode tree` result. */
    fun renderTree(result: TreeResult): String = kodeJson.encodeToString(result.toDto())

    /** Render the `kode symbols` result. */
    fun renderSymbols(result: SymbolsResult): String = kodeJson.encodeToString(result.toDto())

    /** Render the `kode errors` result. */
    fun renderErrors(result: ErrorsResult): String = kodeJson.encodeToString(result.toDto())

    /** Render the `kode refs` result. */
    fun renderRefs(result: RefsResult): String = kodeJson.encodeToString(result.toDto())

    /** Render the `kode test` result. */
    fun renderTests(result: TestsResult): String = kodeJson.encodeToString(result.toDto())

    /** Render an error envelope. */
    fun renderError(envelope: ErrorEnvelope): String = kodeJson.encodeToString(envelope)
}
