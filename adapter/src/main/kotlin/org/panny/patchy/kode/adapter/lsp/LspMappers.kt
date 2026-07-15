package org.panny.patchy.kode.adapter.lsp

import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.Position
import org.panny.patchy.kode.domain.service.SnippetExtractor
import org.panny.patchy.kode.domain.valueobject.CodeSnippet
import org.panny.patchy.kode.domain.valueobject.FilePath
import org.panny.patchy.kode.domain.valueobject.Severity
import org.panny.patchy.kode.domain.valueobject.SourcePosition
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

/*
 * Pure LSP → domain translations (anti-corruption layer). LSP positions are
 * 0-based; kode's output is 1-based (design ch. 03).
 */

internal fun Position.toDomain(): SourcePosition = SourcePosition(line + 1, character + 1)

/** A missing severity is treated conservatively as an error. */
internal fun DiagnosticSeverity?.toDomain(): Severity = when (this) {
    DiagnosticSeverity.Error, null -> Severity.ERROR
    DiagnosticSeverity.Warning -> Severity.WARNING
    DiagnosticSeverity.Information -> Severity.INFORMATION
    DiagnosticSeverity.Hint -> Severity.HINT
}

/**
 * Map a document URI to a root-relative [FilePath], or `null` when the location
 * lies outside the project (e.g. a stdlib source) — such hits are dropped from
 * the output rather than leaking absolute paths.
 */
internal fun uriToFilePath(root: Path, uri: String): FilePath? {
    val path = uriToPath(uri)?.takeIf { it.startsWith(root) } ?: return null
    return root.relativize(path).joinToString("/").takeIf { it.isNotBlank() }?.let(::FilePath)
}

internal fun uriToPath(uri: String): Path? =
    runCatching { Path.of(URI(uri)).normalize() }.getOrNull()

/**
 * Per-request snippet source: caches each file's content so several findings in
 * the same file read it once. Unreadable files yield empty snippets.
 */
internal class FileSnippets(private val extractor: SnippetExtractor) {
    private val cache = mutableMapOf<Path, String?>()

    fun at(file: Path, line: Int): CodeSnippet {
        val content = cache.getOrPut(file) {
            try {
                Files.readString(file)
            } catch (_: IOException) {
                null
            }
        }
        return content?.let { extractor.extract(it, line) } ?: CodeSnippet.EMPTY
    }
}
