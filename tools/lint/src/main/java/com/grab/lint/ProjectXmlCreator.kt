package com.grab.lint

import com.android.SdkConstants
import java.io.File
import java.nio.file.Path

class ProjectXmlCreator(
    private val workingDir: Path,
    private val projectXml: File,
) {

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
        partialResults: File,
        modelsDir: File,
        srcs: List<String>,
        resources: List<String>,
        classpath: List<String>,
        manifest: File?,
        mergedManifest: File?,
        dependencies: List<LintDependency>,
        verbose: Boolean
    ): File {
        val lintModelsDir = if (modelsDir.exists()) {
            // For android_binary's report step we need to reuse models from analyze action so just return it if it exists
            modelsDir
        } else LintModelCreator().create(
            compileSdkVersion = compileSdkVersion,
            library = library,
            projectName = name,
            minSdkVersion = "21", // TODO(arun) Pass from project
            targetSdkVersion = "34", // TODO(arun) Pass from project
            mergedManifest = mergedManifest,
            partialResultsDir = partialResults,
            modelsDir = modelsDir,
            srcs = srcs,
            resources = resources,
            manifest = manifest,
        )

        val contents = buildString {
            appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
            appendLine("<project>")
            appendLine(moduleXml(name, android, library, compileSdkVersion, partialResults, lintModelsDir))
            srcs.forEach { src ->
                appendLine("  <src file=\"$src\" test=\"false\" />")
            }
            //resources.forEach { resource ->
            //      appendLine("  <resource file=\"$resource\" />")
            //}
            // Certain detectors need res folder as input, eg: MissingTranslation
            resFolders(resources).forEach { resource ->
                appendLine("  <resource file=\"$resource\" />")
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