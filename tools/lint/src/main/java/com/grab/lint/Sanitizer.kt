package com.grab.lint

import java.io.File
import java.nio.file.Path
import kotlin.io.path.bufferedWriter

class Sanitizer(
    private val env: Env = Env.BazelEnv,
    private val tmpPath: Path
) {

    fun sanitize(file: File, output: File? = null) {
        val tmpFile = tmpPath.resolve("TMP_" + file.name)

        file.useLines { lines ->
            tmpFile.bufferedWriter().use { writer ->
                lines.forEach { line ->
                    writer.appendLine(sanitize(line, rootRegex))
                }
            }
        }

        val out = output ?: file
        tmpFile.toFile().copyTo(out, overwrite = true)
    }

    /**
     * It seems we can't simply rely on PWD to find the execroot as Lint baseline output may contain sandbox realized paths in them which
     * expands symlinks. For example, if the file is coming from another target's output it might contain a different sandbox root.
     * Especially the sandbox number might be different.
     *
     * To solve this we check for sandbox path and prepare a [Regex] that can replace any sort of path sandbox or not.
     */
    private val rootRegex: Regex by lazy {
        val pwd = env.pwd
        val currDirName = File(pwd).name
        val regex = "(-sandbox/(.*?)/execroot/$currDirName)|(/../../../../../)".toRegex()
        val sandboxDir = regex.find(pwd)?.groupValues?.firstNotNullOfOrNull { it.toIntOrNull() }
        val pattern = when {
            sandboxDir != null -> pwd.replace("/$sandboxDir/", "/(.*?)/")
            else -> pwd
        }
        (pattern.removePrefix("/") + "/").toRegex()
    }

    private fun sanitize(line: String, calcExecRoot: Regex): String {
        val target = if ("file=\"" in line) {
            "file="
        } else if ("message=\"" in line) {
            "message="
        } else {
            return line
        }

        val suffix = if (line.endsWith("/>")) "/>" else if (line.endsWith(">")) ">" else ""
        return PATH_REGEX.find(line)
            ?.value
            ?.replace("\"", "") // Remove "
            ?.replace(System.getenv(env.tmpDir) ?: "", "")
            ?.dropWhile { char -> char == '.' || char == '/' } // Clean ../
            ?.replace(calcExecRoot, "")
            ?.let { fixedPath ->
                "${line.split(target).first()}$target\"$fixedPath\"$suffix" // Retain indent and write file="updated path"
            } ?: line
    }

    companion object {
        private val PATH_REGEX = """"([^"]+)"""".toRegex()
    }
}