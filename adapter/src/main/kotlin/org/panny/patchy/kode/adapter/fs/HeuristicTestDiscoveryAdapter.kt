package org.panny.patchy.kode.adapter.fs

import org.panny.patchy.kode.domain.entity.KotlinProject
import org.panny.patchy.kode.domain.entity.TestClass
import org.panny.patchy.kode.domain.port.TestDiscoveryPort
import org.panny.patchy.kode.domain.service.SymbolMatcher
import org.panny.patchy.kode.domain.valueobject.FilePath
import org.panny.patchy.kode.domain.valueobject.SymbolName
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.readText

/**
 * Filesystem-heuristic implementation of [TestDiscoveryPort] (v1, design ch. 05
 * §3): test classes are associated by naming convention (`FooTest` / `FooTests`
 * / `FooSpec` for target `Foo`), matched against `class` declarations in the
 * configured test directories, and test functions are the `fun` declarations
 * following a test annotation. Tests are never executed.
 *
 * v1 limitation: test functions are collected per file, so nested classes are
 * not distinguished.
 */
class HeuristicTestDiscoveryAdapter(
    private val symbolMatcher: SymbolMatcher,
) : TestDiscoveryPort {

    override fun testsFor(project: KotlinProject, target: SymbolName): List<TestClass> {
        val spec = symbolMatcher.parse(target)
        val candidates = symbolMatcher.testClassCandidates(spec)
        val classRegex = classDeclarationRegex(candidates)
        return project.testDirs
            .flatMap { dir -> kotlinFilesUnder(project.root.resolve(dir.value)) }
            .flatMap { file -> testClassesIn(project.root.path, file, classRegex) }
            .sortedWith(compareBy({ it.file.value }, { it.name.value }))
    }

    private fun classDeclarationRegex(candidates: List<String>): Regex {
        val names = candidates.joinToString("|") { Regex.escape(it) }
        return Regex("""\bclass\s+($names)\b""")
    }

    private fun kotlinFilesUnder(dir: Path): List<Path> {
        if (!Files.isDirectory(dir)) return emptyList()
        val files = mutableListOf<Path>()
        Files.walkFileTree(
            dir,
            object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(sub: Path, attrs: BasicFileAttributes): FileVisitResult {
                    val name = sub.fileName?.toString().orEmpty()
                    return if (sub != dir && name.startsWith(".")) {
                        FileVisitResult.SKIP_SUBTREE
                    } else {
                        FileVisitResult.CONTINUE
                    }
                }

                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (file.fileName.toString().endsWith(KOTLIN_EXTENSION)) files.add(file)
                    return FileVisitResult.CONTINUE
                }
            },
        )
        return files
    }

    private fun testClassesIn(root: Path, file: Path, classRegex: Regex): List<TestClass> {
        val text = file.readText()
        val classNames = classRegex.findAll(text).map { it.groupValues[1] }.distinct().toList()
        if (classNames.isEmpty()) return emptyList()
        val relative = FilePath(root.relativize(file).invariantSeparatorsPathString)
        val functions = testFunctions(text)
        return classNames.map { TestClass(SymbolName(it), relative, functions) }
    }

    /** The name of each `fun` declaration that follows a test annotation. */
    private fun testFunctions(text: String): List<SymbolName> {
        val functions = mutableListOf<SymbolName>()
        var pendingAnnotation = false
        for (line in text.lines()) {
            if (TEST_ANNOTATION_REGEX.containsMatchIn(line)) pendingAnnotation = true
            if (pendingAnnotation) {
                val match = FUNCTION_REGEX.find(line) ?: continue
                functions += SymbolName(match.groupValues[1].trim('`'))
                pendingAnnotation = false
            }
        }
        return functions
    }

    private companion object {
        const val KOTLIN_EXTENSION = ".kt"
        val TEST_ANNOTATION_REGEX = Regex("""@(Test|ParameterizedTest|RepeatedTest|TestFactory)\b""")
        val FUNCTION_REGEX = Regex("""\bfun\s+(`[^`]+`|[A-Za-z_][A-Za-z0-9_]*)""")
    }
}
