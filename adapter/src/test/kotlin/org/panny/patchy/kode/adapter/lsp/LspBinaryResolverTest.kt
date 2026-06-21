package org.panny.patchy.kode.adapter.lsp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.panny.patchy.kode.domain.error.LspNotFoundException
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.div

class LspBinaryResolverTest {

    private fun executable(dir: Path, name: String): Path {
        val file = (dir / name).createFile()
        check(file.toFile().setExecutable(true)) { "could not mark $file executable" }
        return file
    }

    @Test
    fun `prefers KODE_LSP_PATH over configured path and PATH`(@TempDir dir: Path) {
        val fromEnv = executable(dir, "from-env")
        val fromConfig = executable(dir, "from-config")
        val onPath = dir.resolve("path-dir").also { it.toFile().mkdirs() }
        executable(onPath, LspBinaryResolver.BINARY_NAME)

        val resolver = LspBinaryResolver(
            env = { name -> if (name == LspBinaryResolver.ENV_VAR) fromEnv.toString() else null },
            pathEntries = { listOf(onPath) },
        )

        assertEquals(fromEnv, resolver.resolve(configuredPath = fromConfig.toString()))
    }

    @Test
    fun `falls back to configured path when env var is unset`(@TempDir dir: Path) {
        val fromConfig = executable(dir, "from-config")
        val resolver = LspBinaryResolver(env = { null }, pathEntries = { emptyList() })

        assertEquals(fromConfig, resolver.resolve(configuredPath = fromConfig.toString()))
    }

    @Test
    fun `discovers kotlin-lsp on PATH as last resort`(@TempDir dir: Path) {
        val onPath = executable(dir, LspBinaryResolver.BINARY_NAME)
        val resolver = LspBinaryResolver(env = { null }, pathEntries = { listOf(dir) })

        assertEquals(onPath, resolver.resolve())
    }

    @Test
    fun `throws LspNotFoundException when nothing resolves`(@TempDir dir: Path) {
        val resolver = LspBinaryResolver(env = { null }, pathEntries = { listOf(dir) })

        val ex = assertThrows<LspNotFoundException> { resolver.resolve() }
        assertEquals("Kotlin LSP binary not found", ex.message)
    }

    @Test
    fun `ignores a non-executable candidate`(@TempDir dir: Path) {
        val notExecutable = (dir / LspBinaryResolver.BINARY_NAME).createFile()
        notExecutable.toFile().setExecutable(false)
        val resolver = LspBinaryResolver(env = { null }, pathEntries = { listOf(dir) })

        assertThrows<LspNotFoundException> { resolver.resolve() }
    }
}
