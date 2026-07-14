package org.panny.patchy.kode.domain.entity

/**
 * One node of the project file tree returned by `kode tree` (design ch. 02 §4).
 */
sealed interface FileTreeNode {
    val name: String

    data class Directory(
        override val name: String,
        val children: List<FileTreeNode>,
    ) : FileTreeNode

    data class File(
        override val name: String,
    ) : FileTreeNode
}
