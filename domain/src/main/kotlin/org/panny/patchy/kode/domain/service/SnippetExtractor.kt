package org.panny.patchy.kode.domain.service

import org.panny.patchy.kode.domain.valueobject.CodeSnippet

/**
 * Pure snippet-cutting logic (design ch. 02 §6): given a file's content, cut
 * out the line a diagnostic or reference points at. No I/O — callers read the
 * file and pass its content in.
 */
class SnippetExtractor {

    /** The trimmed 1-based [line] of [source], or [CodeSnippet.EMPTY] when out of range. */
    fun extract(source: String, line: Int): CodeSnippet {
        val lines = source.lines()
        if (line < 1 || line > lines.size) return CodeSnippet.EMPTY
        return CodeSnippet(lines[line - 1].trim())
    }
}
