package com.google.devtools.build.android

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split
import com.google.devtools.build.android.SourceSet.Companion.SOURCE_SET_FORMAT
import com.grab.aapt.databinding.util.commonPath
import java.io.File

class ResourceMergerCommand : CliktCommand() {

    private val target by option(
        "-t",
        "--target",
        help = "The target name"
    ).required()

    private val sourceSets by option(
        "-s",
        "--source-sets",
        help = "List of sources sets in the format $SOURCE_SET_FORMAT separated by `,`"
    ).split(",").default(emptyList())

    private val outputs by option(
        "-o",
        "--output",
        help = "The list of output files after performing resource merging"
    ).split(",").default(emptyList())

    override fun run() {
        val sourceSets = sourceSets.map { arg -> SourceSet.from(target, arg) }
        val outputPath = commonPath(*outputs.toTypedArray()).split("/res/").first()
        val outputDir = File(outputPath).apply {
            deleteRecursively()
            parentFile?.mkdirs()
        }
        ResourceMerger.merge(/* sourceSets = */ sourceSets, /* outputDir = */ outputDir)
        OutputFixer.process(outputDir = outputDir, declaredOutputs = outputs)
    }
}