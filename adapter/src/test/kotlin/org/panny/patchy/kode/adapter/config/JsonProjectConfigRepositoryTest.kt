package org.panny.patchy.kode.adapter.config

import org.panny.patchy.kode.domain.entity.KotlinProject
import org.panny.patchy.kode.domain.error.InvalidConfigException
import org.panny.patchy.kode.domain.valueobject.BuildTool
import org.panny.patchy.kode.domain.valueobject.FilePath
import org.panny.patchy.kode.domain.valueobject.KotlinVersion
import org.panny.patchy.kode.domain.valueobject.ProjectRoot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

class JsonProjectConfigRepositoryTest {

    private val repo = JsonProjectConfigRepository()

    private fun sampleProject(root: Path) = KotlinProject(
        root = ProjectRoot(root),
        buildTool = BuildTool.GRADLE,
        kotlinVersion = KotlinVersion("2.1.0"),
        sourceDirs = listOf(FilePath("src/main/kotlin")),
        testDirs = listOf(FilePath("src/test/kotlin")),
    )

    @Test
    fun `save then load round-trips`(@TempDir dir: Path) {
        val project = sampleProject(dir)
        repo.save(project)

        val loaded = repo.load(ProjectRoot(dir))

        assertEquals(project, loaded)
    }

    @Test
    fun `saved json uses snake_case keys and the schema`(@TempDir dir: Path) {
        repo.save(sampleProject(dir))

        val json = dir.resolve(".kode.json").readText()

        assertTrue(json.contains("\"build_tool\""), json)
        assertTrue(json.contains("\"kotlin_version\""), json)
        assertTrue(json.contains("\"source_dirs\""), json)
        assertTrue(json.contains("\"test_dirs\""), json)
        assertTrue(json.contains("\"version\": \"1.0\""), json)
        // optional null fields are omitted
        assertTrue(!json.contains("\"lsp\""), json)
    }

    @Test
    fun `load returns null when no config exists`(@TempDir dir: Path) {
        assertNull(repo.load(ProjectRoot(dir)))
    }

    @Test
    fun `load throws InvalidConfigException on corrupt json`(@TempDir dir: Path) {
        dir.resolve(".kode.json").writeText("{ not valid json")

        assertThrows(InvalidConfigException::class.java) { repo.load(ProjectRoot(dir)) }
    }
}
