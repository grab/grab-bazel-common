package com.google.devtools.build.android

import java.io.File

object OutputFixer {
    const val EMPTY_RES_CONTENT = """<?xml version="1.0" encoding="UTF-8" standalone="no"?><resources/>"""
    fun process(outputDir: File, declaredOutputs: List<String>) {
        val outputDirPath = outputDir.toPath()

        // Merged directories will have qualifiers added by bazel which will not match the path specified in declaredOutputs, manually
        // walk and remove the suffixes like v4, v13 etc from the resource bucket directories.
        outputDir.walk()
            .filter { it != outputDirPath }
            .filter { it.parentFile?.parentFile?.toPath() == outputDirPath }
            .filter { it.isDirectory && it.name.matches(Regex(".*-v\\d+$")) }
            .forEach { resBucket ->
                val newName = resBucket.name.split("-").dropLast(1).joinToString(separator = "-")
                resBucket.renameTo(File(resBucket.parent, newName))
            }

        // Empty resource files especially xmls are skipped in the merged directory, in order to satisfy bazel action output requirements
        // manually add empty resource files here.
        declaredOutputs
            .asSequence()
            .filter { it.endsWith(".xml") }
            .map { File(it) }
            .filter { !it.exists() }
            .forEach { file ->
                file.run {
                    parentFile?.mkdirs()
                    writeText(EMPTY_RES_CONTENT)
                }
            }
    }
}