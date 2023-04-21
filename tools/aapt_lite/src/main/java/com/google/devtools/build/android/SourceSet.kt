package com.google.devtools.build.android

import java.io.File
import java.nio.file.Path

data class SourceSet(
    val resourceDirs: List<Path>,
    val assetDirs: List<Path>,
    val manifest: File
) {
    companion object {
        const val SOURCE_SET_FORMAT = "resources:assets:manifest"
        fun from(target: String, inputArg: String): SourceSet {
            val chunks = inputArg.split(":")
            require(chunks.size == 3) { "Invalid format, should be $SOURCE_SET_FORMAT" }
            return SourceSet(
                resourceDirs = listOf(File(target, chunks[0]).toPath()),
                assetDirs = listOf(File(target, chunks[1]).toPath()),
                manifest = File(target, chunks[2])
            )
        }
    }
}