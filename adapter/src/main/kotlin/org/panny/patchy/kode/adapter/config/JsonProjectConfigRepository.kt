package org.panny.patchy.kode.adapter.config

import org.panny.patchy.kode.domain.entity.KotlinProject
import org.panny.patchy.kode.domain.error.InvalidConfigException
import org.panny.patchy.kode.domain.port.ProjectConfigRepository
import org.panny.patchy.kode.domain.valueobject.ProjectRoot
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists
import kotlin.io.path.readText

private const val CONFIG_FILE_NAME = ".kode.json"

/**
 * `.kode.json` repository backed by the file system + kotlinx.serialization
 * (design ch. 06).
 */
class JsonProjectConfigRepository : ProjectConfigRepository {

    override fun load(start: ProjectRoot): KotlinProject? {
        val file = findUpwards(start.path) ?: return null
        val dto = try {
            kodeJson.decodeFromString<KodeConfigDto>(file.readText())
        } catch (e: SerializationException) {
            throw InvalidConfigException(details = "Failed to parse $file: ${e.message}", cause = e)
        }
        return dto.toDomain()
    }

    override fun save(project: KotlinProject) {
        val target = project.root.resolve(CONFIG_FILE_NAME)
        val content = kodeJson.encodeToString(project.toDto())
        val tmp = Files.createTempFile(target.parent, ".kode", ".json.tmp")
        Files.writeString(tmp, content)
        try {
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    /** Search [start] and its ancestors for `.kode.json`; return the first match. */
    private fun findUpwards(start: Path): Path? {
        var dir: Path? = start
        while (dir != null) {
            val candidate = dir.resolve(CONFIG_FILE_NAME)
            if (candidate.exists()) return candidate
            dir = dir.parent
        }
        return null
    }
}
