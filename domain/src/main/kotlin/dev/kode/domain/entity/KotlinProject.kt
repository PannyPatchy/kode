package dev.kode.domain.entity

import dev.kode.domain.valueobject.BuildTool
import dev.kode.domain.valueobject.FilePath
import dev.kode.domain.valueobject.KotlinVersion
import dev.kode.domain.valueobject.ProjectRoot

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
