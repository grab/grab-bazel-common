package com.grab.lint

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split
import com.grab.cli.WorkingDirectory
import java.io.File
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

    private val orgBaseline by option(
        "-b",
        "--baseline",
        help = "The lint baseline file"
    ).convert { File(it) }

    private val updatedBaseline by option(
        "-ub",
        "--updated-baseline",
        help = "The lint baseline file"
    ).convert { File(it) }.required()

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

    private val resultCode by option(
        "-rc",
        "--result-code",
        help = "File containing result status of running Lint"
    ).convert { File(it) }.required()

    private val jdkHome by option(
        "-j",
        "--jdk-home",
        help = "Path fo Java home"
    ).required()

    private val verbose by option(
        "-v",
        "--verbose",
    ).flag(default = false)

    override fun run() {
        WorkingDirectory().use { dir ->
            val workingDir = dir.dir

            val projectXml = ProjectXmlCreator(workingDir = workingDir).create(
                name = name,
                android = android,
                library = library,
                compileSdkVersion = compileSdkVersion,
                partialResults = partialResults,
                srcs = srcs,
                resources = resources,
                classpath = classpath,
                manifest = manifest,
                mergedManifest = mergedManifest,
                dependencies = dependencies.map(Dependency::from),
                verbose = verbose
            )

            val lintBaseline = LintBaseline(workingDir, orgBaseline, updatedBaseline, verbose)
            val tmpBaseline = lintBaseline.prepare()

            // Prepare JDK
            // Lint uses $JAVA_HOME/release which is not provided by Bazel's JavaRuntimeInfo, so manually populate it
            // Only MODULES is populated here since Lint only seem to use that
            prepareJdk()

            runLint(projectXml, tmpBaseline, analyzeOnly = true)
            val baseline = runLint(projectXml, tmpBaseline, analyzeOnly = false)
            lintBaseline.postProcess(baseline)

            processResults()
            LintResults(resultCodeFile = resultCode, lintResultsFile = outputXml).process()
        }
    }

    private fun processResults() {
        resultCode.writeText("0")
    }

    private fun runLint(projectXml: File, tmpBaseline: File, analyzeOnly: Boolean = false): File {
        LintCli().run(
            mutableListOf(
                "--project", projectXml.toString(),
                "--xml", outputXml.toString(),
                "--config", lintConfig.toString(),

                "--stacktrace",
                //"--quiet",
                "--exitcode",

                "--baseline", tmpBaseline.absolutePath,
                "--update-baseline", // Always update the baseline, so we can copy later if needed
                //"--missing-baseline-is-empty-baseline",

                "--offline", // Not a good practice to make bazel actions reach the network yet
                "--client-id", "test",

                "--jdk-home", jdkHome // Java home to use
            ).apply {
                if (analyzeOnly) {
                    add("--analyze-only")
                } else {
                    add("--report-only")
                }
                System.getenv("ANDROID_HOME")?.let { // TODO(arun) Need to revisit this.
                    add("--sdk-home")
                    add(it)
                }
            }.toTypedArray()
        )
        return tmpBaseline
    }

    private fun prepareJdk() {
        File(jdkHome, "release").writeText(
            ModuleLayer
                .boot()
                .modules()
                .joinToString(separator = " ", prefix = "MODULES=\"", postfix = "\"") { it.name }
        )
    }

    private fun logResults() {
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
}