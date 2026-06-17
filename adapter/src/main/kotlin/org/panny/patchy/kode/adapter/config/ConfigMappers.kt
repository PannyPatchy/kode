package org.panny.patchy.kode.adapter.config

import org.panny.patchy.kode.domain.entity.KotlinProject
import org.panny.patchy.kode.domain.valueobject.BuildTool
import org.panny.patchy.kode.domain.valueobject.FilePath
import org.panny.patchy.kode.domain.valueobject.KotlinVersion
import org.panny.patchy.kode.domain.valueobject.ProjectRoot
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
