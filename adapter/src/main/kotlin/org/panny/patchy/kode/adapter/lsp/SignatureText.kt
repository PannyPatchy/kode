package org.panny.patchy.kode.adapter.lsp

import org.panny.patchy.kode.domain.entity.Symbol

/**
 * Pure text helpers for pulling argument/return/property type information out
 * of LSP `detail` strings and Kotlin declaration lines. All parsing is best
 * effort: unknown shapes yield `null`s rather than guesses.
 */
internal object SignatureText {

    /** `(input: String, count: Int = 0)` → `[input: String, count: Int]`. */
    fun parseArguments(signature: String): List<Symbol.Argument> =
        parenContent(signature)?.let(::splitTopLevel).orEmpty().map { parameter ->
            val declaration = parameter.substringBefore('=').trim()
            val name = declaration.substringBefore(':').trim()
                .split(' ').last() // drop modifiers like vararg
            val type = declaration.substringAfter(':', missingDelimiterValue = "").trim()
            Symbol.Argument(name, type.takeIf { it.isNotBlank() })
        }

    /** `...): Boolean {` / `...): Boolean =` → `Boolean` */
    fun returnTypeAfterParens(text: String): String? =
        closingParenIndex(text)
            ?.let { close -> RETURN_TYPE_REGEX.find(text.substring(close + 1)) }
            ?.let { match -> trimTypeText(match.groupValues[1]).takeIf(String::isNotBlank) }

    /** `val x: String = ...` → `"String" to false`; no declaration → `null`. */
    fun declaredProperty(text: String): Pair<String?, Boolean?>? {
        val typed = TYPED_PROPERTY_REGEX.find(text)
        if (typed != null) {
            val type = trimTypeText(typed.groupValues[2]).takeIf(String::isNotBlank)
            return type to (typed.groupValues[1] == "var")
        }
        return VAL_OR_VAR_REGEX.find(text)?.let { null to (it.groupValues[1] == "var") }
    }

    /** The content of the first balanced `(...)` group, or `null`. */
    private fun parenContent(text: String): String? {
        val start = text.indexOf('(')
        val close = closingParenIndex(text)
        return if (start >= 0 && close != null) text.substring(start + 1, close) else null
    }

    private fun closingParenIndex(text: String): Int? {
        var depth = 0
        var result: Int? = null
        for ((index, ch) in text.withIndex()) {
            if (ch == '(') depth++
            if (ch == ')' && depth > 0 && --depth == 0) {
                result = index
                break
            }
        }
        return result
    }

    /** Split on commas that are not nested inside `()`, `<>` or `[]`. */
    private fun splitTopLevel(text: String): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var depth = 0
        for (ch in text) {
            when {
                ch in OPENERS -> depth++
                ch in CLOSERS -> depth--
                ch == ',' && depth == 0 -> {
                    parts += current.toString()
                    current.clear()
                    continue
                }
            }
            current.append(ch)
        }
        parts += current.toString()
        return parts.map(String::trim).filter(String::isNotEmpty)
    }

    /**
     * Cut a captured type text at the first token that cannot belong to the
     * type: an unbalanced closer (a constructor-parameter property ends with
     * `)`), a top-level comma, or a body brace.
     */
    private fun trimTypeText(text: String): String {
        var depth = 0
        val kept = StringBuilder()
        for (ch in text) {
            when {
                ch in OPENERS -> depth++
                ch in CLOSERS -> depth--
                (ch == ',' || ch == '{') && depth == 0 -> depth = -1
            }
            if (depth < 0) break
            kept.append(ch)
        }
        return kept.toString().trim()
    }

    private const val OPENERS = "(<["
    private const val CLOSERS = ")>]"
    private val RETURN_TYPE_REGEX = Regex("""^\s*:\s*([^={]+)""")
    private val TYPED_PROPERTY_REGEX = Regex("""\b(val|var)\b[^:=]*:\s*([^=]+)""")
    private val VAL_OR_VAR_REGEX = Regex("""\b(val|var)\b""")
}
