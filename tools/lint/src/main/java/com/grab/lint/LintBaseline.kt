package com.grab.lint

import java.io.File
import java.nio.file.Path

class LintBaseline(
    private val workingDir: Path,
    private val baselineFile: File?,
    private val updatedBaseline: File,
    private val verbose: Boolean
) {
    fun prepare(): File {
        val tmpBaseline = workingDir.resolve("baseline.xml").toFile()
        if (baselineFile?.exists() == true) {
            baselineFile.copyTo(tmpBaseline)
        }
        return tmpBaseline
    }

    fun postProcess(newBaseline: File) {
        // Copy the updated the baseline to baseline output
        if (verbose) println("Copying $newBaseline to $updatedBaseline")
        newBaseline.copyTo(updatedBaseline, overwrite = true)
    }
}