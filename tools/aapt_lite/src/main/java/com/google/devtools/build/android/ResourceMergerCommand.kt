package com.google.devtools.build.android

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split
import com.google.devtools.build.android.SourceSet.Companion.SOURCE_SET_FORMAT
import com.grab.aapt.databinding.util.commonPath
import java.io.File

class ResourceMergerCommand : CliktCommand() {

    @Suppress("unused")
    private val label by option(
        "-l",
        "--label",
        help = "The label name that invokes this merger."
    )

    private val binary: Boolean by option(
        "-b",
        "--is-binary",
        help = "Whether the target is for binary"
    ).flag(default = false)

    private val target by option(
        "-t",
        "--target",
        help = "The target name, this will be used to decode source set paths"
    ).required()

    private val packageName by option(
        "-pn",
        "--package-name",
        help = "The target name, this will be used to decode source set paths"
    ).required()

    private val sourceSets by option(
        "-s",
        "--source-sets",
        help = "List of sources sets in the format $SOURCE_SET_FORMAT separated by `,`"
    ).split(",").default(emptyList())

    private val mergedManifestOutput by option(
        "-m",
        "--manifest",
        help = "The merged manifest output file"
    ).convert { File(it) }

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
        ResourceMerger.merge(
            /* isBinary = */ binary,
            /* sourceSets = */ sourceSets,
            /* outputDir = */ outputDir,
            /* mergeManifest = */ mergedManifestOutput
        )
        OutputFixer.process(outputDir = outputDir, declaredOutputs = outputs)
    }
}