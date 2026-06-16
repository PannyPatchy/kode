package dev.kode.domain.valueobject

/**
 * A file or directory path, always **relative to the project root**.
 *
 * `kode`'s JSON output never leaks absolute paths, so this value object rejects
 * absolute paths at construction time.
 */
@JvmInline
value class FilePath(val value: String) {
    init {
        require(value.isNotBlank()) { "FilePath must not be blank" }
        require(!value.startsWith("/")) { "FilePath must be relative to the project root: $value" }
    }
}
