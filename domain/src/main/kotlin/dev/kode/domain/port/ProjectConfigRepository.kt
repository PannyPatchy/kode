package dev.kode.domain.port

import dev.kode.domain.entity.KotlinProject
import dev.kode.domain.valueobject.ProjectRoot

/**
 * Driven port for reading and writing `.kode.json`. Implemented in the adapter
 * layer (`adapter.config`).
 */
interface ProjectConfigRepository {
    /**
     * Search upward from [start] for `.kode.json` and load the first one found.
     * Returns `null` when no config file exists.
     */
    fun load(start: ProjectRoot): KotlinProject?

    /** Persist [project] to `.kode.json` at its root (atomic write). */
    fun save(project: KotlinProject)
}
