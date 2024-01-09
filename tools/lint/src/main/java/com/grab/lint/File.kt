package com.grab.lint

import org.apache.commons.io.FileUtils
import java.io.File
import java.nio.file.Path

/**
 * Ensures files in [originalDir] are converted to real files instead of being symlinks. This is done by staging them in a temp dir
 * under [tmpDir] and copying back to [originalDir]
 *
 * This is needed since Lint is adamant on resolving and using canonical paths and that sometimes throws permissions errors
 */
fun resolveSymlinks(originalDir: File, tmpDir: Path): File = try {
    val tmp = tmpDir.resolve("partial_results_dir").toFile()
    FileUtils.copyDirectory(originalDir, tmp)
    FileUtils.deleteDirectory(originalDir)
    FileUtils.copyDirectory(tmp, originalDir)
    originalDir
} catch (e: Exception) {
    e.printStackTrace()
    originalDir
}