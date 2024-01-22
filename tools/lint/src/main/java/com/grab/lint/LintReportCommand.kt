package com.grab.lint

import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import java.io.File
import java.nio.file.Path
import kotlin.io.path.pathString
import com.android.tools.lint.Main as LintCli

class LintReportCommand : LintBaseCommand() {

    private val updatedBaseline by option(
        "-ub",
        "--updated-baseline",
        help = "The lint baseline file"
    ).convert { File(it) }.required()

    private val outputXml by option(
        "-o",
        "--output-xml",
        help = "Lint output xml"
    ).convert { File(it) }.required()

    private val resultCode by option(
        "-rc",
        "--result-code",
        help = "File containing result status of running Lint"
    ).convert { File(it) }.required()

    override fun run(
        workingDir: Path,
        projectXml: File,
        tmpBaseline: File,
    ) {
        val newBaseline = runLint(workingDir, projectXml, tmpBaseline)
        Sanitizer(tmpPath = workingDir).sanitize(newBaseline, updatedBaseline)
        LintResults(
            resultCodeFile = resultCode,
            lintResultsFile = outputXml
        ).process()
    }

    override val createProjectXml = false

    private fun runLint(workingDir: Path, projectXml: File, tmpBaseline: File): File {
        val cliArgs = (defaultLintOptions + listOf(
            "--project", projectXml.toString(),
            "--xml", outputXml.toString(),
            "--baseline", tmpBaseline.absolutePath,
            "--cache-dir", workingDir.resolve("cache").pathString,
            "--update-baseline", // Always update the baseline, so we can copy later if needed
            "--report-only" // Only do reporting
        )).toTypedArray()
        LintCli().run(cliArgs)
        return tmpBaseline
    }
}