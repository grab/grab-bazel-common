package com.grab.lint

import java.io.File
import java.nio.file.Files

class ProjectXmlCreator {

    private fun moduleXml(
        name: String,
        android: Boolean,
        library: Boolean,
        partialResults: File
    ) = "<module name=\"$name\" android=\"$android\" library=\"$library\" partial-results-dir=\"$partialResults\">"

    fun create(
        name: String,
        android: Boolean,
        library: Boolean,
        partialResults: File,
        srcs: List<String>,
        resources: List<String>,
        classpath: List<String>,
        manifest: File?,
        mergedManifest: File?,
        dependencies: List<Dependency>
    ): File {
        val tempDir = Files.createTempDirectory("tmp").toFile()
        val projectXml = File(tempDir, "project.xml")
        val contents = buildString {
            appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
            appendLine("<project>")
            appendLine(moduleXml(name, android, library, partialResults))
            srcs.forEach { src ->
                appendLine("  <src file=\"$src\" test=\"false\" />")
            }
            resources.forEach { resource ->
                appendLine("  <resource file=\"$resource\" />")
            }
            classpath.forEach { entry ->
                appendLine("  <classpath jar=\"$entry\" />")
            }
            manifest?.let { manifest ->
                appendLine("  <manifest file=\"$manifest\" />")
            }
            mergedManifest?.let { mergedManifest ->
                appendLine("  <merged-manifest file=\"$mergedManifest\" />")
            }
            dependencies.forEach { dependency ->
                appendLine("  <dep module=\"$dependency\" />")
            }
            appendLine("</module>")
            dependencies.forEach { dependency ->
                appendLine(moduleXml(dependency.name, dependency.android, dependency.library, dependency.partialDir) + "</module>")
            }
            appendLine("</project>")
        }.also(::println)
        projectXml.writeText(contents)
        return projectXml
    }
}