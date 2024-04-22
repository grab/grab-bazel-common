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
import java.nio.file.Path
import java.util.stream.Collectors

abstract class LintBaseCommand : CliktCommand() {

    protected val name by option(
        "-n",
        "--name",
    ).required()

    protected val android: Boolean by option(
        "-a",
        "--android",
    ).flag(default = true)

    protected val library: Boolean by option(
        "-l",
        "--library",
    ).flag(default = false)

    protected val packageName: String? by option(
        "-pn",
        "--package-name",
    )

    protected val srcs by option(
        "-s",
        "--sources",
        help = "List of source files Kotlin or Java"
    ).split(",").default(emptyList())

    protected val resources by option(
        "-r",
        "--resource-files",
        help = "List of Android resources"
    ).split(",").default(emptyList())

    protected val classpath by option(
        "-c",
        "--classpath",
        help = "List of jars in the classpath"
    ).split(",").default(emptyList())

    protected val manifest by option(
        "-m",
        "--manifest",
        help = "Android manifest file"
    ).convert { File(it) }

    protected val mergedManifest by option(
        "-mm",
        "--merged-manifest",
        help = "Merged android manifest file"
    ).convert { File(it) }

    protected val dependencies by option(
        "-d",
        "--dependencies",
        help = "Dependency target names"
    ).split(",").default(emptyList())

    protected val lintConfig by option(
        "-lc",
        "--lint-config",
        help = "Path to lint config"
    ).convert { File(it) }.required()

    protected val jdkHome by option(
        "-j",
        "--jdk-home",
        help = "Path to Java home"
    ).required()

    protected val verbose by option(
        "-v",
        "--verbose",
    ).flag(default = false)

    protected val partialResults by option(
        "-pr",
        "--partial-results-dir",
        help = "The partial results directory generated during analyze step"
    ).convert { File(it) }.required()

    protected val modelsDir by option(
        "-mo",
        "--models-dir",
        help = "Lint models directory containing models.xml"
    ).convert { File(it) }.required()

    private val createModelsDir by option(
        "-mc",
        "--create-models-dir",
        help = "Whether to create a new lint models dir specified via --models-dir"
    ).flag(
        "--no-create-models-dir",
        default = true
    )

    protected val inputBaseline by option(
        "-b",
        "--baseline",
        help = "The lint baseline file"
    ).convert { File(it) }

    private val projectXml by option(
        "-p",
        "--project-xml",
        help = "Path to project.xml that will be created/reused when invoking Lint CLI"
    ).convert { File(it) }.required()

    private val createProjectXml by option(
        "-pc",
        "--create-project-xml",
        help = "Whether to create a new project.xml or reuse one specified in --project-xml"
    ).flag(
        "--no-create-project-xml",
        default = true
    )

    private val compileSdkVersion: String by option(
        "-cs",
        "--compile-sdk-version",
        help = "Compile SDK version of android modules"
    ).default("34")

    private val minSdkVersion: String by option(
        "-ms",
        "--min-sdk-version",
        help = "Min SDK version of android modules"
    ).default("1")

    private val targetSdkVersion: String by option(
        "-ts",
        "--target-sdk-version",
        help = "Target SDK version of android modules"
    ).default("34")

    private val resConfigs by option(
        "-rs",
        "--res-configs",
        help = "Res configuration filters for android modules"
    ).split(",").default(emptyList())

    protected val pathVariables by option(
        "--path-variables"
    ).default("${PWD}=${System.getenv(PWD)}")

    protected val aarDeps by option(
        "--aar_dirs"
    ).split(",").default(emptyList())

    protected val projectCustomLintRules by option(
        "--lint_checks"
    ).split(",").default(emptyList())

    override fun run() {
        preRun()
        prepareJdk()
        WorkingDirectory().use {
            val workingDir = it.dir
            val projectXml = if (!createProjectXml) projectXml else {
                ProjectXmlCreator(projectXml = projectXml).create(
                    name = name,
                    android = android,
                    library = library,
                    compileSdkVersion = compileSdkVersion,
                    minSdkVersion = minSdkVersion,
                    targetSdkVersion = targetSdkVersion,
                    partialResults = partialResults,
                    createModelsDir = createModelsDir,
                    modelsDir = modelsDir,
                    srcs = srcs,
                    resources = resources,
                    aarDeps = aarDeps,
                    projectCustomLintRules = projectCustomLintRules,
                    classpath = classpath,
                    packageName = packageName,
                    manifest = manifest,
                    mergedManifest = mergedManifest,
                    resConfigs = resConfigs,
                    dependencies = dependencies
                        .parallelStream()
                        .map(LintDependency::from)
                        .collect(Collectors.toList()),
                    verbose = verbose
                )
            }
            run(workingDir, projectXml, workingDir.tmpBaseLine())
        }
    }

    /**
     * Executes for before running any common logic
     */
    abstract fun preRun()

    abstract fun run(
        workingDir: Path,
        projectXml: File,
        tmpBaseline: File,
    )

    /**
     * Common options for Lint across different types of invocations, analyze or report.
     */
    protected val defaultLintOptions
        get() = mutableListOf(
            "--config", lintConfig.toString(),
            "--stacktrace",
            //"--quiet",
            "--exitcode",
            "--offline", // Not a good practice to make bazel actions reach the network yet
            "--client-id", "cli",
            "--jdk-home", jdkHome // Java home to use
        ).apply {
            System.getenv("ANDROID_HOME")?.let { // TODO(arun) Need to revisit this.
                add("--sdk-home")
                add(it)
            }
        }

    private fun Path.tmpBaseLine(): File {
        val tmpBaseline = resolve("baseline.xml").toFile()
        if (inputBaseline?.exists() == true) {
            inputBaseline?.copyTo(tmpBaseline)
        }
        return tmpBaseline
    }

    private fun prepareJdk() {
        // Prepare JDK
        // Lint uses $JAVA_HOME/release which is not provided by Bazel's JavaRuntimeInfo, so manually populate it
        // Only MODULES is populated here since Lint only seem to use that
        File(jdkHome, "release").also { release ->
            if (!release.exists()) {
                release.writeText(
                    ModuleLayer
                        .boot()
                        .modules()
                        .joinToString(
                            separator = " ",
                            prefix = "MODULES=\"",
                            postfix = "\"",
                            transform = Module::getName
                        )
                )
            }
        }
    }

    companion object {
        private const val PWD = "PWD"
    }
}