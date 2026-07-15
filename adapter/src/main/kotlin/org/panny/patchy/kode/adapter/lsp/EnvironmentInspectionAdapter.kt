package org.panny.patchy.kode.adapter.lsp

import org.panny.patchy.kode.adapter.config.JsonProjectConfigRepository
import org.panny.patchy.kode.domain.error.InvalidConfigException
import org.panny.patchy.kode.domain.port.EnvironmentInspectionPort
import org.panny.patchy.kode.domain.port.JavaRuntimeStatus
import org.panny.patchy.kode.domain.port.ProjectConfigStatus
import org.panny.patchy.kode.domain.valueobject.LspLocation
import org.panny.patchy.kode.domain.valueobject.LspSource
import org.panny.patchy.kode.domain.valueobject.ProjectRoot
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

private const val JAVA_PROBE_TIMEOUT_SECONDS = 10L

/**
 * Read-only environment probes behind `kode doctor`: `.kode.json` validity via
 * the existing config repository, LSP resolution via [LspBinaryResolver], and a
 * `java -version` process probe (the standalone kotlin-lsp launcher needs a JRE
 * even though `kode` itself ships as a native binary).
 */
class EnvironmentInspectionAdapter(
    private val configRepo: JsonProjectConfigRepository,
    private val resolver: LspBinaryResolver = LspBinaryResolver(),
    private val installStore: LspInstallStore = LspInstallStore(),
    private val javaProbe: () -> JavaRuntimeStatus = { probeJavaRuntime() },
) : EnvironmentInspectionPort {

    override fun projectConfig(cwd: Path): ProjectConfigStatus = try {
        val project = configRepo.load(ProjectRoot(cwd))
        if (project == null) ProjectConfigStatus.Missing else ProjectConfigStatus.Found(project.root.path)
    } catch (e: InvalidConfigException) {
        ProjectConfigStatus.Invalid(e.details ?: e.message)
    }

    override fun locateLsp(cwd: Path): LspLocation? {
        // A corrupt .kode.json must not mask the LSP probe; it is reported by
        // the project_config check instead.
        val configured = try {
            configRepo.loadLspPath(cwd)
        } catch (_: InvalidConfigException) {
            null
        }
        val location = resolver.locate(configured) ?: return null
        return if (location.source == LspSource.MANAGED) {
            location.copy(installedVersion = installStore.load()?.version)
        } else {
            location
        }
    }

    override fun javaRuntime(): JavaRuntimeStatus = javaProbe()
}

/** Run `java -version` (preferring `JAVA_HOME`) and parse the major version. */
internal fun probeJavaRuntime(env: (String) -> String? = System::getenv): JavaRuntimeStatus {
    val fromJavaHome = env("JAVA_HOME")?.takeIf { it.isNotBlank() }
        ?.let { Path.of(it, "bin", "java") }
        ?.takeIf { Files.isExecutable(it) }
        ?.toString()
    val java = fromJavaHome ?: "java"
    return try {
        val process = ProcessBuilder(java, "-version").redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        if (!process.waitFor(JAVA_PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return JavaRuntimeStatus.NotFound
        }
        parseJavaMajorVersion(output)?.let { JavaRuntimeStatus.Available(it) } ?: JavaRuntimeStatus.NotFound
    } catch (_: IOException) {
        JavaRuntimeStatus.NotFound
    } catch (e: InterruptedException) {
        Thread.currentThread().interrupt()
        JavaRuntimeStatus.NotFound
    }
}

/** Parse `java -version` output; legacy `1.x` versions map to `x` (e.g. 1.8 -> 8). */
internal fun parseJavaMajorVersion(versionOutput: String): Int? {
    val match = Regex("version \"(\\d+)(?:\\.(\\d+))?").find(versionOutput) ?: return null
    val major = match.groupValues[1].toInt()
    return if (major == 1) match.groupValues[2].toIntOrNull() else major
}
