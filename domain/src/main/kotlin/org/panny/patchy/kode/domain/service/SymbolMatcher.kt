package org.panny.patchy.kode.domain.service

import org.panny.patchy.kode.domain.error.InvalidArgumentException
import org.panny.patchy.kode.domain.valueobject.SymbolName

/**
 * Interprets the `<target>` argument of `kode refs` / `kode test` (design
 * ch. 02 §6, 04 §4): either a type name (`FooClass`) or a member of a type
 * (`FooClass.doSomething`).
 */
class SymbolMatcher {

    /** A parsed `<target>`: the type name plus an optional member name. */
    data class TargetSpec(val type: String, val member: String?)

    /**
     * Split [target] into a [TargetSpec]. Accepts `Foo` and `Foo.bar`;
     * anything else (empty segments, deeper nesting) is rejected.
     */
    fun parse(target: SymbolName): TargetSpec {
        val segments = target.value.split('.')
        val valid = segments.size in 1..2 && segments.all { it.isNotBlank() }
        if (!valid) {
            throw InvalidArgumentException(
                "Invalid target: ${target.value}",
                "Expected a type name like 'FooClass' or a member like 'FooClass.doSomething'.",
            )
        }
        return TargetSpec(type = segments[0], member = segments.getOrNull(1))
    }

    /** Conventional test-class names for [spec]'s type (design ch. 05 §3). */
    fun testClassCandidates(spec: TargetSpec): List<String> =
        TEST_CLASS_SUFFIXES.map { suffix -> spec.type + suffix }

    /**
     * Whether a workspace-symbol candidate ([name] within [containerName]) is
     * the definition [spec] asks for. For `Foo.bar` the container must refer to
     * `Foo`; a missing container is accepted leniently because not all servers
     * report one (callers rank candidates afterwards).
     */
    fun matchesDefinition(spec: TargetSpec, name: String, containerName: String?): Boolean {
        val member = spec.member ?: return name == spec.type
        return name == member && containerMatches(containerName, spec.type)
    }

    private fun containerMatches(containerName: String?, type: String): Boolean =
        containerName.isNullOrBlank() || containerName == type || containerName.endsWith(".$type")

    private companion object {
        val TEST_CLASS_SUFFIXES = listOf("Test", "Tests", "Spec")
    }
}
