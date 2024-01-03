package com.grab.lint

import java.io.File
import java.nio.file.Path

class LintBaseline(
    private val workingDir: Path,
    private val orgBaselineFile: File?,
    private val updatedBaseline: File,
    private val verbose: Boolean,
    private val env: Env = Env.BazelEnv
) {
    fun prepare(): File {
        val tmpBaseline = workingDir.resolve("baseline.xml").toFile()
        if (orgBaselineFile?.exists() == true) {
            orgBaselineFile.copyTo(tmpBaseline)
        }
        return tmpBaseline
    }

    fun postProcess(newBaseline: File) {
        // Sanitize the updated baseline to baseline output
        if (verbose) println("Copying $newBaseline to $updatedBaseline")
        val calcExecRoot = execRootRegex()
        newBaseline.useLines { lines ->
            updatedBaseline.bufferedWriter().use { writer ->
                lines.forEach { line ->
                    writer.appendLine(sanitize(line, calcExecRoot))
                }
            }
        }
    }

    /**
     * It seems we can't simply rely on PWD to find the execroot as Lint baseline output may contain sandbox realized paths in them which
     * expands symlinks. For example, if the file is coming from another target's output it might contain a different sandbox root.
     * Especially the sandbox number might be different.
     *
     * To solve this we check for sandbox path and prepare a [Regex] that can replace any sort of path sandbox or not.
     */
    private fun execRootRegex(): Regex {
        val pwd = env.pwd
        val currDirName = File(pwd).name
        val regex = "-sandbox/(.*?)/execroot/$currDirName".toRegex()
        val sandboxDir = regex.find(pwd)?.groupValues?.firstNotNullOfOrNull { it.toIntOrNull() }
        val pattern = when {
            sandboxDir != null -> pwd.replace("/$sandboxDir/", "/(.*?)/")
            else -> pwd
        }
        return (pattern.removePrefix("/") + "/").toRegex()
    }

    private fun sanitize(line: String, calcExecRoot: Regex): String {
        return if ("file=\"" in line) {
            val suffix = if (line.endsWith(">")) "/>" else ""

            FILE_PATH_REGEX.find(line)
                ?.value
                ?.replace("\"", "") // Remove "
                ?.replace(env.tmpDir, "")
                ?.dropWhile { char -> char == '.' || char == '/' } // Clean ../
                ?.replace(calcExecRoot, "")
                ?.let { fixedPath ->
                    "${line.split("file=").first()}file=\"$fixedPath\"$suffix" // Retain indent and write file="updated path"
                } ?: line
        } else line
    }


    companion object {
        private val FILE_PATH_REGEX = """"([^"]+)"""".toRegex()
    }
}