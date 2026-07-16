package org.panny.patchy.kode.adapter.lsp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.panny.patchy.kode.domain.valueobject.LspInstallation
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createFile

class LspInstallStoreTest {

    @Test
    fun `round-trips an installation`(@TempDir home: Path) {
        val launcher = home.resolve("kotlin-lsp.sh").createFile()
        launcher.toFile().setExecutable(true)
        val store = LspInstallStore(home)

        store.save(LspInstallation("1.0.0", launcher))

        assertEquals(LspInstallation("1.0.0", launcher), store.load())
    }

    @Test
    fun `returns null when no record exists`(@TempDir home: Path) {
        assertNull(LspInstallStore(home).load())
    }

    @Test
    fun `ignores a corrupt record`(@TempDir home: Path) {
        val record = home.resolve("lsp").resolve("installed.json")
        Files.createDirectories(record.parent)
        Files.writeString(record, "{ not json")

        assertNull(LspInstallStore(home).load())
    }

    @Test
    fun `ignores a stale record whose launcher is gone`(@TempDir home: Path) {
        val launcher = home.resolve("kotlin-lsp.sh").createFile()
        launcher.toFile().setExecutable(true)
        val store = LspInstallStore(home)
        store.save(LspInstallation("1.0.0", launcher))

        Files.delete(launcher)

        assertNull(store.load())
    }
}
