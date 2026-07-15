package org.panny.patchy.kode.application.usecase

import org.panny.patchy.kode.application.dto.TestsResult
import org.panny.patchy.kode.domain.port.ProjectConfigRepository
import org.panny.patchy.kode.domain.port.TestDiscoveryPort
import org.panny.patchy.kode.domain.service.SymbolMatcher
import java.nio.file.Path

/**
 * `kode test <target>`: test classes/functions related to a production symbol
 * (design ch. 03 §2). Discovery is static — tests are never executed.
 */
class FindTestsUseCase(
    private val configRepo: ProjectConfigRepository,
    private val testDiscovery: TestDiscoveryPort,
    private val symbolMatcher: SymbolMatcher,
) {
    fun execute(cwd: Path, rawTarget: String): TestsResult {
        val project = configRepo.loadOrThrow(cwd)
        val target = symbolNameArgument(rawTarget)
        symbolMatcher.parse(target) // fail fast on malformed targets
        return TestsResult(target, testDiscovery.testsFor(project, target))
    }
}
