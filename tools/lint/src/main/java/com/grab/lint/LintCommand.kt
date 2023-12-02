package com.grab.lint

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split
import java.io.File
import kotlin.io.path.exists
import kotlin.io.path.readText
import com.android.tools.lint.Main as LintCli

data class Dependency(
    val name: String,
    val android: Boolean,
    val library: Boolean,
    val partialDir: File,
)

class LintCommand : CliktCommand() {

    private val name by option(
        "-n",
        "--name",
    ).required()

    private val android: Boolean by option(
        "-a",
        "--android",
    ).flag(default = true)

    private val compileSdkVersion: String? by option(
        "-cs",
        "--compile-sdk-version",
    )

    private val library: Boolean by option(
        "-l",
        "--library",
    ).flag(default = true)

    private val srcs by option(
        "-s",
        "--sources",
        help = "List of source files Kotlin or Java"
    ).split(",").default(emptyList())

    private val resources by option(
        "-r",
        "--resource-files",
        help = "List of Android resources"
    ).split(",").default(emptyList())

    private val classpath by option(
        "-c",
        "--classpath",
        help = "List of jars in the classpath"
    ).split(",").default(emptyList())

    private val manifest by option(
        "-m",
        "--manifest",
        help = "Android manifest file"
    ).convert { File(it) }

    private val mergedManifest by option(
        "-mm",
        "--merged-manifest",
        help = "Merged android manifest file"
    ).convert { File(it) }

    private val dependencies by option(
        "-d",
        "--dependencies",
        help = "Dependency target names"
    ).split(",").default(emptyList())

    private val lintConfig by option(
        "-lc",
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
        "--partial-results-dir",
    ).convert { File(it) }.required()

    private val verbose by option(
        "-v",
        "--verbose",
    ).flag(default = false)

    override fun run() {
        val projectXml = ProjectXmlCreator().create(
            name,
            android,
            library,
            compileSdkVersion,
            partialResults,
            srcs,
            resources,
            classpath,
            manifest,
            mergedManifest,
            dependencies.map { dependency ->
                val (name, android, library, partialResultsDir) = dependency.split("^")
                Dependency(name, android.toBoolean(), library.toBoolean(), File(partialResultsDir))
            },
            verbose
        )
        runLint(projectXml, analyzeOnly = true)
        runLint(projectXml, analyzeOnly = false)
    }

    private fun runLint(projectXml: File, analyzeOnly: Boolean = false) {
        val outputDir = File(".").toPath()
        val baselineFile = outputDir.resolve("baseline.xml")
        LintCli().run(
            mutableListOf(
                "--project", projectXml.toString(),
                "--xml", outputXml.toString(),
                "--baseline", baselineFile.toString(),
                "--config", lintConfig.toString(),
                "--update-baseline",
                "--client-id", "test"
            ).apply {
                if (analyzeOnly) {
                    add("--analyze-only")
                } /*else {
                    add("--report-only")
                }*/
                System.getenv("ANDROID_HOME")?.let {
                    add("--sdk-home")
                    add(it)
                }
            }.toTypedArray()
        )
        if (verbose) {
            if (outputXml.exists()) println(outputXml.readText())
            if (partialResults.exists()) partialResults.walkTopDown().forEach(::println)
            if (baselineFile.exists()) println(baselineFile.readText())
        }
    }
}