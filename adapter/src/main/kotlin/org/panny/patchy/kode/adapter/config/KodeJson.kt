package org.panny.patchy.kode.adapter.config

import kotlinx.serialization.json.Json

/**
 * Shared JSON instance for `kode`. snake_case keys, pretty output, and
 * forward-compatible decoding (unknown keys ignored). kotlinx.serialization
 * generates serializers at compile time, so this is native-image friendly.
 */
internal val kodeJson: Json = Json {
    prettyPrint = true
    encodeDefaults = true
    explicitNulls = false // omit null fields (e.g. optional `lsp`, error `details`)
    ignoreUnknownKeys = true
}
