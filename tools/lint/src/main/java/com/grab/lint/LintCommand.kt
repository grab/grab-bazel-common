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
        help = "Project descriptor XML"
    ).convert { File(it) }.required()

    private val lintConfig by option(
        "-l",
        "--lint-config",
        help = "Path to lint config"
    ).convert { File(it) }

    private val outputXml by option(
        "-o",
        "--output-xml",
        help = "Lint output xml"
    ).convert { File(it) }.required()

    override fun run() {
        val outputDir = File(".").toPath()
        val baselineFile = outputDir.resolve("baseline.xml")

        // TODO: Get this from rule itself
        val lintConfig = outputDir.resolve("lint.xml").writeLines(
            listOf(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<lint checkTestSources=\"true\">",
                "   <issue id=\"all\" severity=\"error\" />",
                "   <issue id=\"MissingSuperCall\" severity=\"error\" />",
                "</lint>"
            )
        )

        outputXml.createNewFile()

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