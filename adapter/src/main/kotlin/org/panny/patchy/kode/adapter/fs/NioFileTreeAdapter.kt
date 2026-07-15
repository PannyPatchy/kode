package org.panny.patchy.kode.adapter.fs

import org.panny.patchy.kode.domain.entity.FileTreeNode
import org.panny.patchy.kode.domain.port.FileTreePort
import org.panny.patchy.kode.domain.valueobject.FilePath
import org.panny.patchy.kode.domain.valueobject.ProjectRoot
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name

/**
 * `java.nio` implementation of [FileTreePort] for `kode tree`. Hidden entries
 * (dotfiles) are skipped; children are listed files-first, each group sorted by
 * name, for a stable JSON output.
 */
class NioFileTreeAdapter : FileTreePort {

    override fun walk(root: ProjectRoot, dir: FilePath): FileTreeNode.Directory? {
        val start = root.resolve(dir.value)
        if (!Files.isDirectory(start)) return null
        return directoryNode(start)
    }

    private fun directoryNode(dir: Path): FileTreeNode.Directory {
        val entries = Files.newDirectoryStream(dir).use { it.toList() }
            .filterNot { it.name.startsWith(".") }
        val files = entries
            .filter { Files.isRegularFile(it) }
            .map { FileTreeNode.File(it.name) }
            .sortedBy(FileTreeNode.File::name)
        val subDirs = entries
            .filter { Files.isDirectory(it) }
            .sortedBy(Path::name)
            .map(::directoryNode)
        return FileTreeNode.Directory(dir.name, files + subDirs)
    }
}
