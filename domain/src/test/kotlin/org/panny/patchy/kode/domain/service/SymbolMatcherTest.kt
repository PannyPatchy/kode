package org.panny.patchy.kode.domain.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.panny.patchy.kode.domain.error.InvalidArgumentException
import org.panny.patchy.kode.domain.valueobject.SymbolName

class SymbolMatcherTest {

    private val matcher = SymbolMatcher()

    @Test
    fun `parses a plain type name`() {
        assertEquals(SymbolMatcher.TargetSpec("FooClass", null), matcher.parse(SymbolName("FooClass")))
    }

    @Test
    fun `parses a member target`() {
        assertEquals(
            SymbolMatcher.TargetSpec("FooClass", "doSomething"),
            matcher.parse(SymbolName("FooClass.doSomething")),
        )
    }

    @Test
    fun `rejects malformed targets`() {
        assertThrows(InvalidArgumentException::class.java) { matcher.parse(SymbolName("a.b.c")) }
        assertThrows(InvalidArgumentException::class.java) { matcher.parse(SymbolName("Foo.")) }
        assertThrows(InvalidArgumentException::class.java) { matcher.parse(SymbolName(".bar")) }
    }

    @Test
    fun `test class candidates follow naming conventions`() {
        val spec = matcher.parse(SymbolName("FooClass"))
        assertEquals(listOf("FooClassTest", "FooClassTests", "FooClassSpec"), matcher.testClassCandidates(spec))
    }

    @Test
    fun `type target matches by exact name`() {
        val spec = matcher.parse(SymbolName("Foo"))
        assertTrue(matcher.matchesDefinition(spec, "Foo", containerName = null))
        assertFalse(matcher.matchesDefinition(spec, "FooBar", containerName = null))
    }

    @Test
    fun `member target requires matching container when reported`() {
        val spec = matcher.parse(SymbolName("Foo.bar"))
        assertTrue(matcher.matchesDefinition(spec, "bar", containerName = "Foo"))
        assertTrue(matcher.matchesDefinition(spec, "bar", containerName = "com.example.Foo"))
        assertTrue(matcher.matchesDefinition(spec, "bar", containerName = null))
        assertFalse(matcher.matchesDefinition(spec, "bar", containerName = "Baz"))
        assertFalse(matcher.matchesDefinition(spec, "baz", containerName = "Foo"))
    }
}
