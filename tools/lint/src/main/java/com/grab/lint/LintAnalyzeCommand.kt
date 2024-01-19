package com.grab.lint

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import kotlin.io.path.isRegularFile
import com.android.tools.lint.Main as LintCli

class LintAnalyzeCommand : LintBaseCommand() {

    override val createProjectXml: Boolean = true

    override fun run(
        workingDir: Path,
        projectXml: File,
        tmpBaseline: File,
    ) {
        val cliArgs = (defaultLintOptions + listOf(
            //"--cache-dir", workingDir.resolve("cache").pathString,
            "--project", projectXml.toString(),
            "--analyze-only" // Only do analyze
        )).toTypedArray()
        LintCli().run(cliArgs)
        sanitizePartialResults()
    }

    private fun sanitizePartialResults() {
        Files.walk(partialResults.toPath())
            .filter { it.isRegularFile() }
            .collect(Collectors.toList())
            .parallelStream()
            .forEach { sanitizer.sanitize(it.toFile()) }
    }
}