package com.grab.lint

import com.android.SdkConstants
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.exists

class ProjectXmlCreator(
    private val projectXml: File,
) {

    private fun List<String>.lintJars(): Sequence<String> = asSequence()
        .map { Paths.get(it.split("^")[1]).resolve("lint.jar") }
        .filter { it.exists() }
        .map { it.toString() }

    private fun moduleXml(
        name: String,
        android: Boolean,
        library: Boolean,
        compileSdkVersion: String,
        partialResults: File,
        lintModelDir: File? = null,
    ) = """<module 
        |  name="$name" 
        |  android="$android" 
        |  library="$library" 
        |  partial-results-dir="$partialResults"
        |  ${lintModelDir?.let { """model="$lintModelDir"""" } ?: ""} 
        |  compile-sdk-version="$compileSdkVersion"
        |  desugar="full">""".trimMargin()

    fun create(
        name: String,
        android: Boolean,
        library: Boolean,
        compileSdkVersion: String,
        minSdkVersion: String,
        targetSdkVersion: String,
        partialResults: File,
        createModelsDir: Boolean,
        modelsDir: File,
        srcs: List<String>,
        resources: List<String>,
        aarDeps: List<String>,
        projectCustomLintRules: List<String>,
        classpath: List<String>,
        resConfigs: List<String>,
        packageName: String?,
        manifest: File?,
        mergedManifest: File?,
        dependencies: List<LintDependency>,
        verbose: Boolean
    ): File {
        val lintModelsDir = if (!createModelsDir) {
            modelsDir
        } else LintModelCreator().create(
            compileSdkVersion = compileSdkVersion,
            android = android,
            library = library,
            projectName = name,
            minSdkVersion = minSdkVersion,
            targetSdkVersion = targetSdkVersion,
            mergedManifest = mergedManifest,
            partialResultsDir = partialResults,
            modelsDir = modelsDir,
            srcs = srcs,
            resources = resources,
            packageName = packageName,
            manifest = manifest,
            resConfigs = resConfigs,
        )

        val contents = buildString {
            appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
            appendLine("<project>")
            appendLine(moduleXml(name, android, library, compileSdkVersion, partialResults, lintModelsDir))
            srcs.forEach { src ->
                appendLine("  <src file=\"$src\" test=\"false\" />")
            }
            // Certain detectors need res folder as input, eg: MissingTranslation
            resFolders(resources).forEach { resource ->
                appendLine("  <resource file=\"$resource\" />")
            }
            (projectCustomLintRules + aarDeps.lintJars()).forEach {
                appendLine("  <lint-checks jar=\"$it\" />")
            }
            aarDeps.forEach { aar ->
                val aarInfo = aar.split("^")
                appendLine("  <aar file=\"${aarInfo[0]}\" extracted=\"${aarInfo[1]}\" />")
            }
            manifest?.let { manifest ->
                appendLine("  <manifest file=\"$manifest\" />")
            }
            mergedManifest?.let { mergedManifest ->
                appendLine("  <merged-manifest file=\"$mergedManifest\" />")
            }
            classpath.forEach { entry ->
                appendLine("  <classpath jar=\"$entry\" />")
            }
            dependencies.forEach { dependency ->
                appendLine("  <dep module=\"${dependency.name}\" />")
            }
            appendLine("</module>")
            dependencies.forEach { dependency ->
                appendLine(
                    moduleXml(
                        dependency.name,
                        dependency.android,
                        dependency.library,
                        compileSdkVersion,
                        dependency.partialDir,
                        dependency.modelsDir
                    ) + "</module>"
                )
            }
            appendLine("</project>")
        }.also {
            if (verbose) {
                println(it)
            }
        }
        return projectXml.apply { bufferedWriter().use { it.write(contents) } }
    }

    /**
     * Given list of resources, return common `res` directory paths
     */
    private fun resFolders(resources: List<String>): List<String> {
        return resources
            .groupBy({ it.substringBeforeLast(SdkConstants.FD_RES + "/") }, { it })
            .keys
            .map { it + SdkConstants.FD_RES }
    }
}