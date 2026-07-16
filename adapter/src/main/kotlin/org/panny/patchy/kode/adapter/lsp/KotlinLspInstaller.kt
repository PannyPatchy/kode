package org.panny.patchy.kode.adapter.lsp

import org.panny.patchy.kode.domain.error.LspDownloadFailedException
import org.panny.patchy.kode.domain.port.LspInstallerPort
import org.panny.patchy.kode.domain.valueobject.LspInstallation
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/** Fetches LSP release artifacts. Injected so installer tests stay offline. */
interface LspArtifactFetcher {
    /**
     * Download [url] into [target], replacing any existing file.
     * @throws IOException on network failure or a non-200 response.
     */
    fun download(url: String, target: Path)

    /** Fetch a small text resource; `null` when it does not exist. */
    fun fetchText(url: String): String?
}

/** [LspArtifactFetcher] backed by the JDK HTTP client (no extra dependency). */
class HttpLspArtifactFetcher : LspArtifactFetcher {
    private val client: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    override fun download(url: String, target: Path) {
        val response = client.send(request(url), HttpResponse.BodyHandlers.ofFile(target))
        if (response.statusCode() != HTTP_OK) throw IOException("HTTP ${response.statusCode()} for $url")
    }

    override fun fetchText(url: String): String? {
        val response = client.send(request(url), HttpResponse.BodyHandlers.ofString())
        return if (response.statusCode() == HTTP_OK) response.body() else null
    }

    private fun request(url: String): HttpRequest = HttpRequest.newBuilder(URI.create(url)).GET().build()

    private companion object {
        const val HTTP_OK = 200
    }
}

/**
 * Downloads JetBrains' standalone kotlin-lsp build into `~/.kode/lsp/<version>/`
 * (design ch. 04: `kode` never bundles or redistributes the LSP; this fetches
 * JetBrains' own artifact on the user's explicit request, exactly like
 * `scripts/fetch-kotlin-lsp.sh`).
 *
 * The artifact checksum is verified when the CDN publishes a `.sha256` sidecar;
 * otherwise the install proceeds with a warning ([LspInstallation.checksumVerified]
 * records which happened).
 */
class KotlinLspInstaller(
    private val store: LspInstallStore = LspInstallStore(),
    private val fetcher: LspArtifactFetcher = HttpLspArtifactFetcher(),
    private val osName: String = System.getProperty("os.name"),
    private val osArch: String = System.getProperty("os.arch"),
    /** Human-readable progress, routed to stderr by the CLI (stdout stays JSON-only). */
    private val onProgress: (String) -> Unit = {},
) : LspInstallerPort {

    override fun currentInstallation(): LspInstallation? = store.load()

    override fun install(version: String?): LspInstallation {
        val v = version ?: DEFAULT_LSP_VERSION
        store.load()?.takeIf { it.version == v }?.let { return it }

        val url = "$BASE_URL/$v/${artifactName(v)}"
        val destDir = store.lspDir(v)
        Files.createDirectories(destDir)
        val archive = Files.createTempFile(destDir, "kotlin-lsp", ".tar.gz")
        try {
            onProgress("Downloading $url")
            downloadArchive(url, archive)
            val verified = verifyChecksum(url, archive)
            extract(archive, destDir)
            val launcher = findLauncher(destDir)
                ?: throw LspDownloadFailedException("$LAUNCHER_NAME not found in the downloaded archive ($url)")
            launcher.toFile().setExecutable(true)
            val installation = LspInstallation(v, launcher, checksumVerified = verified)
            store.save(installation)
            onProgress("Installed kotlin-lsp $v at $launcher")
            return installation
        } finally {
            Files.deleteIfExists(archive)
        }
    }

    private fun downloadArchive(url: String, target: Path) {
        try {
            fetcher.download(url, target)
        } catch (e: IOException) {
            throw LspDownloadFailedException(
                details = "Failed downloading $url: ${e.message}. $MANUAL_INSTALL_HINT",
                cause = e,
            )
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw LspDownloadFailedException(details = "Interrupted while downloading $url", cause = e)
        }
    }

    /** @return `true` when verified; `false` when no checksum is published. */
    private fun verifyChecksum(artifactUrl: String, archive: Path): Boolean {
        val sidecarUrl = "$artifactUrl.sha256"
        val expected = try {
            fetcher.fetchText(sidecarUrl)?.trim()?.split(WHITESPACE)?.firstOrNull()
        } catch (_: IOException) {
            null
        }
        if (expected.isNullOrBlank()) {
            onProgress("Warning: no published checksum at $sidecarUrl; skipping verification.")
            return false
        }
        val actual = sha256Hex(archive)
        if (!actual.equals(expected, ignoreCase = true)) {
            throw LspDownloadFailedException(
                details = "Checksum mismatch for $artifactUrl: expected $expected, got $actual",
            )
        }
        return true
    }

    private fun extract(archive: Path, destDir: Path) {
        val process = ProcessBuilder("tar", "-xzf", archive.toString(), "-C", destDir.toString())
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        val failure = try {
            when {
                !process.waitFor(EXTRACT_TIMEOUT_SECONDS, TimeUnit.SECONDS) -> {
                    process.destroyForcibly()
                    "tar extraction timed out after ${EXTRACT_TIMEOUT_SECONDS}s"
                }
                process.exitValue() != 0 -> "tar extraction failed: ${output.take(MAX_TAR_OUTPUT)}"
                else -> null
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            process.destroyForcibly()
            throw LspDownloadFailedException(details = "Interrupted while extracting the LSP archive", cause = e)
        }
        if (failure != null) throw LspDownloadFailedException(details = failure)
    }

    /** Same depth-3 launcher search as `scripts/fetch-kotlin-lsp.sh`. */
    private fun findLauncher(dir: Path): Path? =
        Files.walk(dir, LAUNCHER_SEARCH_DEPTH).use { paths ->
            paths
                .filter { it.fileName.toString() == LAUNCHER_NAME && Files.isRegularFile(it) }
                .findFirst()
                .orElse(null)
        }

    private fun artifactName(version: String): String {
        val os = osName.lowercase()
        if (!os.contains("linux") && !os.contains("mac") && !os.contains("darwin")) {
            throw LspDownloadFailedException(
                details = "Unsupported platform for automatic install: $osName/$osArch. $MANUAL_INSTALL_HINT",
            )
        }
        val arm = osArch.lowercase() in setOf("aarch64", "arm64")
        return if (arm) "kotlin-server-$version-aarch64.tar.gz" else "kotlin-server-$version.tar.gz"
    }

    private fun sha256Hex(file: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(file).use { input ->
            val buffer = ByteArray(DIGEST_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        /** Pinned default, kept in sync with `scripts/fetch-kotlin-lsp.sh`. */
        const val DEFAULT_LSP_VERSION: String = "262.8190.0"
        const val BASE_URL: String = "https://download-cdn.jetbrains.com/language-server/kotlin-server"
        const val LAUNCHER_NAME: String = "kotlin-lsp.sh"

        private const val MANUAL_INSTALL_HINT =
            "Install manually via 'brew install JetBrains/utils/kotlin-lsp' or scripts/fetch-kotlin-lsp.sh."
        private const val LAUNCHER_SEARCH_DEPTH = 3
        private const val EXTRACT_TIMEOUT_SECONDS = 300L
        private const val DIGEST_BUFFER_SIZE = 64 * 1024
        private const val MAX_TAR_OUTPUT = 500
        private val WHITESPACE = Regex("\\s+")
    }
}
