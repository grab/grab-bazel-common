package com.google.devtools.build.android

import java.io.File
import java.nio.file.Path

data class SourceSet(
    val resourceDirs: List<Path>,
    val assetDirs: List<Path>,
    val manifest: File?
) {
    companion object {
        const val SOURCE_SET_FORMAT = "resources:assets:manifest"
        fun from(target: String, inputArg: String): SourceSet {
            val chunks = inputArg.split(":")
            require(chunks.size == 3) { "Invalid format, should be $SOURCE_SET_FORMAT" }
            val (res, assets, manifest) = chunks

            fun String.toPaths() = when {
                trim().isEmpty() -> emptyList<Path>()
                else -> listOf(File(target, this).toPath())
            }
            return SourceSet(
                resourceDirs = res.toPaths(),
                assetDirs = assets.toPaths(),
                manifest = if (manifest.trim().isNotEmpty() && File(target, manifest).exists()) {
                    File(target, manifest)
                } else null
            )
        }
    }
}