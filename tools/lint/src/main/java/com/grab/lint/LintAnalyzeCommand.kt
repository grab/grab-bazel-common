package com.grab.lint

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.pathString
import com.android.tools.lint.Main as LintCli

class LintAnalyzeCommand : LintBaseCommand() {

    override fun preRun() {
        // For analyze, always clear previous lint results
        partialResults.deleteRecursively()
        Files.createDirectories(partialResults.toPath())
    }

    override val createProjectXml = true

    override fun run(
        workingDir: Path,
        projectXml: File,
        tmpBaseline: File,
    ) {
        val cliArgs = (defaultLintOptions + listOf(
            "--cache-dir", workingDir.resolve("cache").pathString,
            "--project", projectXml.toString(),
            "--analyze-only" // Only do analyze
        )).toTypedArray()
        LintCli().run(cliArgs)
        postProcessPartialResults(workingDir)
    }

    private fun postProcessPartialResults(workingDir: Path) {
        Files.walk(partialResults.toPath())
            .filter { it.isRegularFile() }
            .collect(Collectors.toList())
            .parallelStream()
            .forEach { path ->
                if ("lint-definite-all.xml" in path.name) {
                    Files.delete(path)
                } /*else {
                    Sanitizer(tmpPath = workingDir).sanitize(path.toFile())
                }*/
            }
    }
}