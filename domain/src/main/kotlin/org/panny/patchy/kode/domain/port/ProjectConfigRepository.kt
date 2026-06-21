package org.panny.patchy.kode.domain.port

import org.panny.patchy.kode.domain.entity.KotlinProject
import org.panny.patchy.kode.domain.valueobject.ProjectRoot

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

    /** Whether a `.kode.json` already exists directly at [root] (no upward search). */
    fun existsAt(root: ProjectRoot): Boolean

    /** Persist [project] to `.kode.json` at its root (atomic write). */
    fun save(project: KotlinProject)
}
