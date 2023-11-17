package com.grab.lint

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import java.io.File
import com.android.tools.lint.Main as LintCli

class LintCommand : CliktCommand() {

    private val projectXml by option(
        "-p",
        "--project-xml",
        help = "Project descriptor XML"
    ).convert { File(it) }.required()

    private val lintConfig by option(
        "-l",
        "--lint-config",
        help = "Path to lint config"
    ).convert { File(it) }.required()

    private val outputXml by option(
        "-o",
        "--output-xml",
        help = "Lint output xml"
    ).convert { File(it) }.required()

    private val partialResults by option(
        "-pr",
        "--partial-results",
    ).convert { File(it) }.required()

    override fun run() {
        runLint(analyzeOnly = true)
        val statusCode = runLint(analyzeOnly = false)
        throw ProgramResult(statusCode)
    }

    private fun runLint(analyzeOnly: Boolean = false): Int {
        val outputDir = File(".").toPath()
        val baselineFile = outputDir.resolve("baseline.xml")
        return LintCli().run(
            mutableListOf(
                "--project", this.projectXml.toString(),
                "--xml", this.outputXml.toString(),
                "--baseline", baselineFile.toString(),
                "--config", this.lintConfig.toString(),
                "--update-baseline",
                "--client-id", "test"
            ).apply {
                if (analyzeOnly) {
                    add("--analyze-only")
                } /*else {
                    add("--report-only")
                }*/
            }.toTypedArray()
        )
    }
}
