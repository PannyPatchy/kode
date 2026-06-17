package org.panny.patchy.kode.domain.valueobject

/** A Kotlin version string such as `"2.1.0"`. */
@JvmInline
value class KotlinVersion(val value: String) {
    init {
        require(value.isNotBlank()) { "KotlinVersion must not be blank" }
    }
}
