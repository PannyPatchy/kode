package org.panny.patchy.kode.domain.port

import org.panny.patchy.kode.domain.entity.KotlinProject
import org.panny.patchy.kode.domain.entity.TestClass
import org.panny.patchy.kode.domain.valueobject.SymbolName

/**
 * Driven port for `kode test`: test classes associated with a production
 * target. Implemented in the adapter layer; discovery is static — tests are
 * never executed (design ch. 05).
 */
interface TestDiscoveryPort {
    /** Test classes related to [target] in [project]'s test directories. */
    fun testsFor(project: KotlinProject, target: SymbolName): List<TestClass>
}
