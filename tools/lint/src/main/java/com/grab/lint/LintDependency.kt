package com.grab.lint

import java.io.File

data class LintDependency(
    val name: String,
    val android: Boolean,
    val library: Boolean,
    val partialDir: File,
) {
    companion object {
        fun from(encodedString: String): LintDependency {
            val (name, android, library, partialResultsDir) = encodedString.split("^")
            // val partialResults = resolveSymlinks(File(partialResultsDir), workingDir)
            return LintDependency(
                name = name,
                android = android.toBoolean(),
                library = library.toBoolean(),
                partialDir = File(partialResultsDir)
            )
        }
    }
}