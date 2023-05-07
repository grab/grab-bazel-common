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

            fun String.chunkToPaths() = when {
                trim().isEmpty() -> emptyList<Path>()
                else -> listOf(File(target, this).toPath())
            }

            return SourceSet(
                resourceDirs = chunks[0].chunkToPaths(),
                assetDirs = chunks[1].chunkToPaths(),
                manifest = File(target, chunks[2])
            )
        }
    }
}