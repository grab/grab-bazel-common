package com.grab.lint

import java.io.File

data class Dependency(
    val name: String,
    val android: Boolean,
    val library: Boolean,
    val partialDir: File,
) {
    companion object {
        fun from(encodedString: String): Dependency {
            val (name, android, library, partialResultsDir) = encodedString.split("^")
            return Dependency(name, android.toBoolean(), library.toBoolean(), File(partialResultsDir))
        }
    }
}