package org.panny.patchy.kode.domain.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SnippetExtractorTest {

    private val extractor = SnippetExtractor()
    private val source = """
        package com.example

            val x: String = 42
        fun main() {}
    """.trimIndent()

    @Test
    fun `extracts the 1-based line, trimmed`() {
        assertEquals("val x: String = 42", extractor.extract(source, 3).text)
    }

    @Test
    fun `first and last lines are reachable`() {
        assertEquals("package com.example", extractor.extract(source, 1).text)
        assertEquals("fun main() {}", extractor.extract(source, 4).text)
    }

    @Test
    fun `out-of-range lines yield an empty snippet`() {
        assertEquals("", extractor.extract(source, 0).text)
        assertEquals("", extractor.extract(source, 99).text)
    }

    @Test
    fun `blank line yields an empty snippet`() {
        assertEquals("", extractor.extract(source, 2).text)
    }
}
