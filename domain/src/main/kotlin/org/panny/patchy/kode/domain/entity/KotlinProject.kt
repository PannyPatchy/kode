package org.panny.patchy.kode.domain.entity

import org.panny.patchy.kode.domain.valueobject.BuildTool
import org.panny.patchy.kode.domain.valueobject.FilePath
import org.panny.patchy.kode.domain.valueobject.KotlinVersion
import org.panny.patchy.kode.domain.valueobject.ProjectRoot

/**
 * The aggregate root: a Kotlin project recognized by `kode`.
 *
 * Created by `kode init` (project recognition) and persisted to / loaded from
 * `.kode.json`.
 */
data class KotlinProject(
    val root: ProjectRoot,
    val buildTool: BuildTool,
    val kotlinVersion: KotlinVersion,
    val sourceDirs: List<FilePath>,
    val testDirs: List<FilePath>,
)
