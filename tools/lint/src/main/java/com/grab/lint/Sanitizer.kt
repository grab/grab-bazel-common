package com.grab.lint

import java.io.File
import java.nio.file.Path
import kotlin.io.path.bufferedWriter

/**
 * Normalizes platform-dependent Bazel config directory names in lint XML output.
 *
 * Bazel 8 with `--android_platforms` produces output dirs like
 * `darwin_arm64-fastbuild-android-ST-d12a83e6cc99/bin/` on Mac and
 * `k8-fastbuild-android-ST-aaaaaaaaaaaa/bin/` on Linux. The platform prefix and the
 * transition hash leak into Lint baselines via relative paths (`../../../../<dir>/bin/...`),
 * causing baselines generated locally not to match the ones generated on CI.
 *
 * This sanitizer rewrites any such config-dir segment to a stable `bazel-out/bin/` form
 * for the `file="..."` and `message="..."` attributes in the XML.
 *
 * It also handles the older CPU-based form from Bazel 7 (`android-arm64-v8a-fastbuild/bin/`)
 * for compatibility with legacy baselines.
 */
class Sanitizer(private val tmpPath: Path) {

    fun sanitize(file: File, output: File? = null) {
        val tmpFile = tmpPath.resolve("TMP_" + file.name)
        file.useLines { lines ->
            tmpFile.bufferedWriter().use { writer ->
                lines.forEach { line ->
                    writer.appendLine(sanitizeLine(line))
                }
            }
        }
        val out = output ?: file
        tmpFile.toFile().copyTo(out, overwrite = true)
    }

    private fun sanitizeLine(line: String): String {
        if ("file=\"" !in line && "message=\"" !in line) return line
        return CONFIG_DIR_REGEX.replace(line, CANONICAL_PREFIX)
    }

    companion object {
        // Match: optional path prefix ending with "/", then a config dir segment, then "/bin/"
        //
        // Examples that match:
        //   ../../../../darwin_arm64-fastbuild-android-ST-d12a83e6cc99/bin/
        //   /abs/path/bazel-out/k8-fastbuild-android-ST-aaaaaaaaaaaa/bin/
        //   bazel-out/android-arm64-v8a-fastbuild/bin/   (Bazel 7 --fat_apk_cpu)
        //
        // The config dir segment shape:
        //   [a-z0-9_]+-fastbuild           (e.g. darwin_arm64, k8, android-arm64-v8a)
        //   (-android-ST-[a-f0-9]+)?       (optional transition hash for --android_platforms)
        private val CONFIG_DIR_REGEX = Regex(
            """(?:[^"\s]*?/)?[a-z0-9_]+(?:-[a-z0-9_]+)*-fastbuild(?:-android-ST-[a-f0-9]+)?/bin/"""
        )
        private const val CANONICAL_PREFIX = "bazel-out/bin/"
    }
}
