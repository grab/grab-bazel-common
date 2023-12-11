package com.grab.lint

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.readText
import com.android.tools.lint.Main as LintCli

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

    private val baseline by option(
        "-b",
        "--baseline",
        help = "The lint baseline file"
    ).convert { Paths.get(it) }

    private val updatedBaseline by option(
        "-ub",
        "--updated-baseline",
        help = "The lint baseline file"
    ).convert { Paths.get(it) }.required()

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
        val baseline = runLint(projectXml, analyzeOnly = false)

        // Copy the updated the baseline to baseline output
        if (verbose) println("Copying $baseline to $updatedBaseline")
        Files.copy(baseline, updatedBaseline, StandardCopyOption.REPLACE_EXISTING)

        if (verbose) {
            if (outputXml.exists()) println(outputXml.readText())
            if (partialResults.exists()) {
                partialResults.walkTopDown()
                    .filter { it.isFile }
                    .forEach { println("\t$it") }
            }
            if (updatedBaseline.exists()) println(updatedBaseline.readText())
        }
    }

    private fun runLint(projectXml: File, analyzeOnly: Boolean = false): Path {
        val workingDir = Files.createTempDirectory("lint")
        // Create a baseline file always
        val baseline = baseline ?: workingDir.resolve("tmp_baseline.xml").createFile()

        LintCli().run(
            mutableListOf(
                "--project", projectXml.toString(),
                "--xml", outputXml.toString(),
                "--config", lintConfig.toString(),
                "--update-baseline", // Always update the baseline, so we can copy later if needed
                "--offline", // Not a good practice to make bazel actions reach the network yet
                "--client-id", "test",
            ).apply {
                if (analyzeOnly) {
                    add("--analyze-only")
                } else {
                    add("--report-only")
                }
                add("--baseline")
                add(baseline.toString())
                System.getenv("ANDROID_HOME")?.let { // TODO(arun) Need to revisit this.
                    add("--sdk-home")
                    add(it)
                }
            }.toTypedArray()
        )
        return baseline
    }
}