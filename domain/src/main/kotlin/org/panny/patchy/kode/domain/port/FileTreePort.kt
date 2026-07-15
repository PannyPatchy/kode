package org.panny.patchy.kode.domain.port

import org.panny.patchy.kode.domain.entity.FileTreeNode
import org.panny.patchy.kode.domain.valueobject.FilePath
import org.panny.patchy.kode.domain.valueobject.ProjectRoot

/**
 * Driven port for `kode tree`: the file tree under a configured source/test
 * directory. Implemented in the adapter layer (`adapter.fs`).
 */
interface FileTreePort {
    /**
     * Walk [dir] (relative to [root]) recursively. Returns `null` when the
     * directory does not exist (e.g. a stale `.kode.json` entry).
     */
    fun walk(root: ProjectRoot, dir: FilePath): FileTreeNode.Directory?
}
