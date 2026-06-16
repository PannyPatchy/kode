package dev.kode.adapter.config

import dev.kode.domain.entity.KotlinProject
import dev.kode.domain.valueobject.BuildTool
import dev.kode.domain.valueobject.FilePath
import dev.kode.domain.valueobject.KotlinVersion
import dev.kode.domain.valueobject.ProjectRoot
import java.nio.file.Path

/** Map a domain [KotlinProject] into its serializable DTO. */
fun KotlinProject.toDto(): KodeConfigDto =
    KodeConfigDto(
        project = ProjectDto(
            root = root.path.toString(),
            buildTool = buildTool.id,
            kotlinVersion = kotlinVersion.value,
            sourceDirs = sourceDirs.map { it.value },
            testDirs = testDirs.map { it.value },
        ),
    )

/** Map a serializable DTO back into a domain [KotlinProject]. */
fun KodeConfigDto.toDomain(): KotlinProject =
    KotlinProject(
        root = ProjectRoot(Path.of(project.root)),
        buildTool = BuildTool.fromId(project.buildTool),
        kotlinVersion = KotlinVersion(project.kotlinVersion),
        sourceDirs = project.sourceDirs.map(::FilePath),
        testDirs = project.testDirs.map(::FilePath),
    )
