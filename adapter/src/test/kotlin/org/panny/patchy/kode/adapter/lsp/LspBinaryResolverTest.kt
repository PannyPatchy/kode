package org.panny.patchy.kode.adapter.lsp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.panny.patchy.kode.domain.error.LspNotFoundException
import org.panny.patchy.kode.domain.valueobject.LspSource
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.div

class LspBinaryResolverTest {

    private fun executable(dir: Path, name: String): Path {
        val file = (dir / name).createFile()
        check(file.toFile().setExecutable(true)) { "could not mark $file executable" }
        return file
    }

    private fun resolver(
        env: (String) -> String? = { null },
        pathEntries: () -> List<Path> = { emptyList() },
        managedLauncher: () -> Path? = { null },
    ) = LspBinaryResolver(env, pathEntries, managedLauncher)

    @Test
    fun `prefers KODE_LSP_PATH over configured path and PATH`(@TempDir dir: Path) {
        val fromEnv = executable(dir, "from-env")
        val fromConfig = executable(dir, "from-config")
        val onPath = dir.resolve("path-dir").also { it.toFile().mkdirs() }
        executable(onPath, LspBinaryResolver.BINARY_NAME)

        val resolver = resolver(
            env = { name -> if (name == LspBinaryResolver.ENV_VAR) fromEnv.toString() else null },
            pathEntries = { listOf(onPath) },
        )

        assertEquals(fromEnv, resolver.resolve(configuredPath = fromConfig.toString()))
    }

    @Test
    fun `falls back to configured path when env var is unset`(@TempDir dir: Path) {
        val fromConfig = executable(dir, "from-config")

        assertEquals(fromConfig, resolver().resolve(configuredPath = fromConfig.toString()))
    }

    @Test
    fun `discovers kotlin-lsp on PATH before the managed install`(@TempDir dir: Path) {
        val onPath = executable(dir, LspBinaryResolver.BINARY_NAME)
        val managedDir = dir.resolve("managed").also { it.toFile().mkdirs() }
        val managed = executable(managedDir, "kotlin-lsp.sh")

        val resolver = resolver(pathEntries = { listOf(dir) }, managedLauncher = { managed })

        assertEquals(onPath, resolver.resolve())
    }

    @Test
    fun `falls back to the managed install as last resort`(@TempDir dir: Path) {
        val managed = executable(dir, "kotlin-lsp.sh")

        val location = resolver(managedLauncher = { managed }).locate()

        assertEquals(managed, location?.path)
        assertEquals(LspSource.MANAGED, location?.source)
    }

    @Test
    fun `locate reports which source resolved`(@TempDir dir: Path) {
        val fromEnv = executable(dir, "from-env")
        val resolver = resolver(env = { fromEnv.toString() })

        assertEquals(LspSource.ENV, resolver.locate()?.source)
    }

    @Test
    fun `locate returns null when nothing resolves`(@TempDir dir: Path) {
        assertNull(resolver(pathEntries = { listOf(dir) }).locate())
    }

    @Test
    fun `throws LspNotFoundException when nothing resolves`(@TempDir dir: Path) {
        val resolver = resolver(pathEntries = { listOf(dir) })

        val ex = assertThrows<LspNotFoundException> { resolver.resolve() }
        assertEquals("Kotlin LSP binary not found", ex.message)
    }

    @Test
    fun `ignores a non-executable candidate`(@TempDir dir: Path) {
        val notExecutable = (dir / LspBinaryResolver.BINARY_NAME).createFile()
        notExecutable.toFile().setExecutable(false)
        val resolver = resolver(pathEntries = { listOf(dir) })

        assertThrows<LspNotFoundException> { resolver.resolve() }
    }
}
