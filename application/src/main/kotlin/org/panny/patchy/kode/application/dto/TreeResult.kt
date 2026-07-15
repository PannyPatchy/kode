package org.panny.patchy.kode.application.dto

import org.panny.patchy.kode.domain.entity.FileTreeNode
import org.panny.patchy.kode.domain.valueobject.FilePath

/** Result of `kode tree`: one walked tree per configured source/test directory. */
data class TreeResult(val roots: List<TreeRoot>) {
    /** [dir] is the configured directory (root-relative); [node] its walked tree. */
    data class TreeRoot(val dir: FilePath, val node: FileTreeNode.Directory)
}
