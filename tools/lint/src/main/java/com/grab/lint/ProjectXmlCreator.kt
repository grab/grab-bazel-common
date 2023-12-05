package com.grab.lint

import java.io.File
import java.nio.file.Files

class ProjectXmlCreator {

    private fun moduleXml(
        name: String,
        android: Boolean,
        library: Boolean,
        compileSdkVersion: String?,
        partialResults: File
    ) = """<module 
        |  name="$name" 
        |  android="$android" 
        |  library="$library" 
        |  partial-results-dir="$partialResults" 
        |  ${compileSdkVersion?.let { """compile-sdk-version="$compileSdkVersion"""" }}
        |  desugar="full">""".trimMargin()

    fun create(
        name: String,
        android: Boolean,
        library: Boolean,
        compileSdkVersion: String?,
        partialResults: File,
        srcs: List<String>,
        resources: List<String>,
        classpath: List<String>,
        manifest: File?,
        mergedManifest: File?,
        dependencies: List<Dependency>,
        verbose: Boolean
    ): File {
        val tempDir = Files.createTempDirectory("tmp").toFile()
        val projectXml = File(tempDir, "project.xml")
        val contents = buildString {
            appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
            appendLine("<project>")
            appendLine(moduleXml(name, android, library, compileSdkVersion, partialResults))
            srcs.forEach { src ->
                appendLine("  <src file=\"$src\" test=\"false\" />")
            }
            resources.forEach { resource ->
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
                        dependency.partialDir
                    ) + "</module>"
                )
            }
            appendLine("</project>")
        }.also {
            if (verbose) {
                println(it)
            }
        }
        projectXml.writeText(contents)
        return projectXml
    }
}