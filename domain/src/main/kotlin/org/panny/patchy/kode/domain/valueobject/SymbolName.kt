package org.panny.patchy.kode.domain.valueobject

/**
 * The name of a class, function or property as it appears in `kode`'s JSON
 * output or CLI arguments (e.g. `FooClass`, `doSomething`, `FooClass.doSomething`).
 */
@JvmInline
value class SymbolName(val value: String) {
    init {
        require(value.isNotBlank()) { "SymbolName must not be blank" }
    }
}
