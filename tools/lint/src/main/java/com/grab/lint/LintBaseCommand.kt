package com.grab.lint

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split
import com.grab.cli.WorkingDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Path

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
    ).flag(default = true)

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
        help = "Path fo Java home"
    ).required()

    protected val verbose by option(
        "-v",
        "--verbose",
    ).flag(default = false)

    protected val partialResults by option(
        "-pr",
        "--partial-results-dir",
    ).convert { File(it) }.required()

    protected val inputBaseline by option(
        "-b",
        "--baseline",
        help = "The lint baseline file"
    ).convert { File(it) }

    private val projectXml by option(
        "-p",
        "--project-xml",
    ).convert { File(it) }.required()

    private val compileSdkVersion: String? by option(
        "-cs",
        "--compile-sdk-version",
    )

    @OptIn(FlowPreview::class)
    override fun run() {
        prepareJdk()
        runBlocking {
            WorkingDirectory().use {
                val workingDir = it.dir
                val concurrency = Runtime.getRuntime().availableProcessors() / 2
                val partialResults = resolveSymlinks(partialResults, workingDir)

                val projectXml = if (!createProjectXml) projectXml else {
                    ProjectXmlCreator(projectXml).create(
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
                        dependencies = dependencies
                            .asFlow()
                            .flatMapMerge(concurrency) { dep ->
                                flow { emit(LintDependency.from(workingDir, dep)) }.flowOn(Dispatchers.IO)
                            }.toList(),
                        verbose = verbose
                    )
                }
                val lintBaselineHandler = LintBaselineHandler(workingDir, inputBaseline, verbose)
                val tmpBaseline = lintBaselineHandler.prepare()
                run(workingDir, projectXml, tmpBaseline, lintBaselineHandler)
            }
        }
    }

    abstract fun run(
        workingDir: Path,
        projectXml: File,
        tmpBaseline: File,
        lintBaselineHandler: LintBaselineHandler
    )

    /**
     * Create new project.xml at [projectXml].
     *
     * Required for non-sandbox modes since we can't rely on file system state when executing in non sandbox modes as previous results might
     * be still there.
     */
    abstract val createProjectXml: Boolean

    protected val defaultLintOptions
        get() = mutableListOf(
            "--config", lintConfig.toString(),
            "--stacktrace",
            //"--quiet",
            "--exitcode",
            "--offline", // Not a good practice to make bazel actions reach the network yet
            "--client-id", "test",
            "--jdk-home", jdkHome // Java home to use
        ).apply {
            System.getenv("ANDROID_HOME")?.let { // TODO(arun) Need to revisit this.
                add("--sdk-home")
                add(it)
            }
        }

    private fun prepareJdk() {
        // Prepare JDK
        // Lint uses $JAVA_HOME/release which is not provided by Bazel's JavaRuntimeInfo, so manually populate it
        // Only MODULES is populated here since Lint only seem to use that
        File(jdkHome, "release").writeText(
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