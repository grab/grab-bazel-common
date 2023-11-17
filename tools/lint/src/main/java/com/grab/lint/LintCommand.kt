package com.grab.lint

import com.android.tools.lint.LintCliFlags.ERRNO_CREATED_BASELINE
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
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

    private val baselineXml by option(
        "-b",
        "--baseline-xml",
    ).convert { File(it) }.default(
        File("baseline.xml")
    )

    override fun run() {
        runLint(analyzeOnly = true)
        val statusCode = runLint(analyzeOnly = false)

        when (statusCode) {
            ERRNO_CREATED_BASELINE -> println("A new baseline file was created at ${baselineXml.absoluteFile}")
            else -> throw ProgramResult(statusCode)
        }
    }

    private fun runLint(analyzeOnly: Boolean = false): Int {
        return LintCli().run(
            mutableListOf(
                "--project", this.projectXml.toString(),
                "--xml", this.outputXml.toString(),
                "--baseline", this.baselineXml.toString(),
                "--config", this.lintConfig.toString(),
                "--update-baseline",
                "--client-id", "test"
            ).apply {
                if (analyzeOnly) {
                    add("--analyze-only")
                }
            }.toTypedArray()
        )
    }
}
