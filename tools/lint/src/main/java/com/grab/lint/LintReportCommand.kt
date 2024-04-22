package com.grab.lint

import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import java.io.File
import java.nio.file.Path
import kotlin.io.path.pathString
import kotlin.system.measureTimeMillis
import com.android.tools.lint.Main as LintCli

class LintReportCommand : LintBaseCommand() {

    private val updatedBaseline by option(
        "-ub",
        "--updated-baseline",
        help = "The lint baseline file"
    ).convert { File(it) }.required()

    private val lintResultXml by option(
        "-o",
        "--output-xml",
        help = "Lint output xml"
    ).convert { File(it) }.required()

    private val outputJunitXml by option(
        "-oj",
        "--output-junit-xml",
        help = "Lint output in Junit format"
    ).convert { File(it) }.required()

    private val resultCode by option(
        "-rc",
        "--result-code",
        help = "File containing result status of running Lint"
    ).convert { File(it) }.required()

    private val failOnWarnings by option(
        "-fow",
        "--fail-on-warning",
        help = "exit code 1 if it find Lint issues with severity of Warning"
    ).convert { it.toBoolean() }.default(true)

    private val failOnInformation by option(
        "-foi",
        "--fail-on-information",
        help = "exit code 1 if it find Lint issues with severity of Information"
    ).convert { it.toBoolean() }.default(true)

    override fun run(
        workingDir: Path,
        projectXml: File,
        tmpBaseline: File,
    ) {
        val elapsed = measureTimeMillis {
            runLint(workingDir, projectXml, tmpBaseline, lintResultXml)
        }
        tmpBaseline.copyTo(updatedBaseline)
        LintResults(
            name = name,
            lintResultsFile = lintResultXml,
            elapsed = elapsed,
            resultCodeFile = resultCode,
            outputJunitXml = outputJunitXml,
            failOnInformation = failOnInformation,
            failOnWarnings = failOnWarnings,
        ).process()
    }

    override fun preRun() {
        // No-op
    }

    private fun runLint(workingDir: Path, projectXml: File, tmpBaseline: File, lintResultXml: File) {
        val cliArgs = (defaultLintOptions + listOf(
            "--project", projectXml.toString(),
            "--xml", lintResultXml.toString(),
            "--baseline", tmpBaseline.absolutePath,
            "--path-variables", pathVariables,
            "--cache-dir", workingDir.resolve("cache").pathString,
            "--update-baseline", // Always update the baseline, so we can copy later if needed
            "--report-only" // Only do reporting
        )).toTypedArray()
        LintCli().run(cliArgs)
    }
}