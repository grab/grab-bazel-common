package com.grab.aapt.databinding.common

import com.grab.aapt.AaptScope
import com.grab.aapt.databinding.util.jars.SourceJarCreator
import dagger.Binds
import dagger.Module
import java.io.File
import javax.inject.Inject
import kotlin.streams.asStream

/**
 * Utility to package a folder into srcjar
 */
interface SrcJarPackager {
    /**
     * @param inputDir Input dir to package
     * @param outputFile The output file where the srcjar will be written to.
     * @param verbose Debug logs from srcjar creator
     */
    fun packageSrcJar(
        inputDir: File,
        outputFile: File,
        verbose: Boolean = false
    )
}

@Module
interface SrcJarPackageModule {
    @Binds
    fun DefaultSrcJarPackager.packager(): SrcJarPackager
}

@AaptScope
class DefaultSrcJarPackager @Inject constructor() : SrcJarPackager {

    override fun packageSrcJar(inputDir: File, outputFile: File, verbose: Boolean) {
        outputFile.parentFile?.mkdirs()
        SourceJarCreator(path = outputFile.toPath(), verbose = verbose).apply {
            addSources(
                File(inputDir.toURI())
                    .walkTopDown()
                    .filter { it.isFile }
                    .map { it.toPath() }
                    .asStream()
            )
        }.execute()
    }
}