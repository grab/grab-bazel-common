package com.google.devtools.build.android

import java.io.File
import java.nio.file.Files

object OutputFixer {
    fun removeQualifiers(outputDir: File) {
        outputDir.walkBottomUp()
            .onEnter { file ->
                val isResBucket = file.parentFile?.parentFile?.path == outputDir.path
                when {
                    file.isDirectory && isResBucket -> {
                        val chunks = file.name.split("-")
                        if (chunks.size > 2) {
                            val newName = chunks.dropLast(1).joinToString("-")
                            Files.move(file.toPath(), file.parentFile.toPath().resolve(newName))
                        }
                        false
                    }

                    else -> true
                }
            }.toList()
    }
}