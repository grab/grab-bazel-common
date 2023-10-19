package com.grab.lint

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import java.io.File
import kotlin.io.path.writeLines
import com.android.tools.lint.Main as LintCli

class LintCommand : CliktCommand() {

    private val projectXml by option(
        "-p",
        "--project-xml",
        help = "Project XML containing lint config"
    ).convert { File(it) }.required()

    private val lintConfig by option(
        "-l",
        "--lint-config",
        help = "Path to lint config "
    ).convert { File(it) }

    override fun run() {
        val outputDir = File(".").toPath()
        val baselineFile = outputDir.resolve("baseline.xml")
        val lintConfig = outputDir.resolve("lint.xml").writeLines(
            listOf(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<lint checkTestSources=\"true\">",
                "</lint>"
            )
        )
        val outputXml = outputDir.resolve("output.xml")
        val projectXml = outputDir.resolve("project.xml").writeLines(
            listOf(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n",
                "<project>\n",
                "</project>\n"
            )
        )

        val lintCli = LintCli()
        lintCli.run(
            arrayOf(
                "--project", projectXml.toString(),
                "--xml", outputXml.toString(),
                "--baseline", baselineFile.toString(),
                "--config", lintConfig.toString(),
                "--client-id", "test"
            )
        )
    }
}