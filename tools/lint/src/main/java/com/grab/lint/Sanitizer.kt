package com.grab.lint

import java.io.File
import java.nio.file.Path
import kotlin.io.path.bufferedWriter

class Sanitizer(
    private val tmpPath: Path
) {

    fun sanitize(file: File, output: File? = null) {
        val tmpFile = tmpPath.resolve("TMP_" + file.name)
        file.useLines { lines ->
            tmpFile.bufferedWriter().use { writer ->
                lines.forEach { line ->
                    writer.appendLine(sanitized(line))
                }
            }
        }

        val out = output ?: file
        tmpFile.toFile().copyTo(out, overwrite = true)
    }

    //message="Missing density variation folders in `../../../../../../private/var/tmp/_bazel_mohammad.khaleghi/f9bf0b8d7b1d5760a1f747cde05d58d9/execroot/pax_android/food-screen/food-cart/src/main/res`: drawable-hdpi, drawable-mdpi, drawable-xhdpi">
    private fun sanitized(line: String): String {
        var target = "file=\""
        var targetIndex = line.indexOf(target)
        if (targetIndex == -1 && line.contains("message=\"")) {
            target = "\'"
            targetIndex = line.indexOf(target)
        }
        return if (targetIndex != -1) {
            line.substring(0, targetIndex + target.length) +
                    line.substring(targetIndex + target.length)
                        .replace(System.getenv(PWD), "\$$PWD")
                        .dropWhile { char -> char == '.' || char == '/' }
        } else {
            line
        }
    }

    companion object {
        const val PWD = "PWD"
    }
}