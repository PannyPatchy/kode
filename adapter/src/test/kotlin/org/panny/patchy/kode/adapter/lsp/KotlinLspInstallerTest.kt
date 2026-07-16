package org.panny.patchy.kode.adapter.lsp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.panny.patchy.kode.domain.error.LspDownloadFailedException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

/**
 * Installer tests run fully offline: the [LspArtifactFetcher] is faked with a
 * pre-built archive, and `KODE_HOME` is replaced by a temp dir via the injected
 * [LspInstallStore].
 */
class KotlinLspInstallerTest {

    private val version = "1.2.3"

    /** Build a real tar.gz containing `nested/kotlin-lsp.sh` (tests run on Linux CI). */
    private fun buildArchive(dir: Path): Path {
        val content = dir.resolve("content/nested")
        Files.createDirectories(content)
        Files.writeString(content.resolve("kotlin-lsp.sh"), "#!/bin/sh\necho lsp\n")
        val archive = dir.resolve("kotlin-server.tar.gz")
        val process = ProcessBuilder(
            "tar", "-czf", archive.toString(), "-C", dir.resolve("content").toString(), "nested",
        ).inheritIO().start()
        check(process.waitFor() == 0) { "failed to build test archive" }
        return archive
    }

    private fun sha256(file: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(Files.readAllBytes(file))
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private class FakeFetcher(
        private val archive: Path?,
        private val checksum: String? = null,
    ) : LspArtifactFetcher {
        val downloadedUrls = mutableListOf<String>()

        override fun download(url: String, target: Path) {
            downloadedUrls += url
            val source = archive ?: throw IOException("HTTP 403 for $url")
            Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        }

        override fun fetchText(url: String): String? = checksum
    }

    private fun installer(home: Path, fetcher: LspArtifactFetcher, progress: MutableList<String> = mutableListOf()) =
        KotlinLspInstaller(
            store = LspInstallStore(home),
            fetcher = fetcher,
            osName = "Linux",
            osArch = "amd64",
            onProgress = progress::add,
        )

    @Test
    fun `installs the launcher and records it in installed json`(@TempDir dir: Path) {
        val home = dir.resolve("home")
        val archive = buildArchive(dir.resolve("fixture"))
        val fetcher = FakeFetcher(archive, checksum = sha256(archive))

        val installation = installer(home, fetcher).install(version)

        assertEquals(version, installation.version)
        assertEquals(true, installation.checksumVerified)
        assertTrue(Files.isExecutable(installation.launcher))
        assertTrue(installation.launcher.startsWith(home.resolve("lsp").resolve(version)))
        assertEquals(installation.launcher, LspInstallStore(home).load()?.launcher)
        assertEquals(
            "https://download-cdn.jetbrains.com/language-server/kotlin-server/$version/kotlin-server-$version.tar.gz",
            fetcher.downloadedUrls.single(),
        )
    }

    @Test
    fun `proceeds unverified when no checksum is published`(@TempDir dir: Path) {
        val home = dir.resolve("home")
        val archive = buildArchive(dir.resolve("fixture"))
        val progress = mutableListOf<String>()

        val installation = installer(home, FakeFetcher(archive, checksum = null), progress).install(version)

        assertEquals(false, installation.checksumVerified)
        assertTrue(progress.any { it.contains("skipping verification") })
    }

    @Test
    fun `fails on checksum mismatch`(@TempDir dir: Path) {
        val home = dir.resolve("home")
        val archive = buildArchive(dir.resolve("fixture"))
        val fetcher = FakeFetcher(archive, checksum = "0".repeat(64))

        assertThrows<LspDownloadFailedException> { installer(home, fetcher).install(version) }
        assertNull(LspInstallStore(home).load())
    }

    @Test
    fun `wraps download failures with install guidance`(@TempDir dir: Path) {
        val home = dir.resolve("home")

        val ex = assertThrows<LspDownloadFailedException> {
            installer(home, FakeFetcher(archive = null)).install(version)
        }
        assertTrue(ex.details.orEmpty().contains("brew install"))
    }

    @Test
    fun `returns the existing installation without downloading again`(@TempDir dir: Path) {
        val home = dir.resolve("home")
        val archive = buildArchive(dir.resolve("fixture"))
        val fetcher = FakeFetcher(archive, checksum = sha256(archive))
        val installer = installer(home, fetcher)

        val first = installer.install(version)
        val second = installer.install(version)

        assertEquals(first.launcher, second.launcher)
        assertEquals(1, fetcher.downloadedUrls.size)
    }

    @Test
    fun `uses the aarch64 artifact on arm machines`(@TempDir dir: Path) {
        val home = dir.resolve("home")
        val archive = buildArchive(dir.resolve("fixture"))
        val fetcher = FakeFetcher(archive, checksum = sha256(archive))
        val installer = KotlinLspInstaller(
            store = LspInstallStore(home),
            fetcher = fetcher,
            osName = "Mac OS X",
            osArch = "aarch64",
        )

        installer.install(version)

        assertTrue(fetcher.downloadedUrls.single().endsWith("kotlin-server-$version-aarch64.tar.gz"))
    }

    @Test
    fun `rejects unsupported platforms with manual guidance`(@TempDir dir: Path) {
        val installer = KotlinLspInstaller(
            store = LspInstallStore(dir),
            fetcher = FakeFetcher(archive = null),
            osName = "Windows 11",
            osArch = "amd64",
        )

        val ex = assertThrows<LspDownloadFailedException> { installer.install(version) }
        assertTrue(ex.details.orEmpty().contains("Unsupported platform"))
    }
}
