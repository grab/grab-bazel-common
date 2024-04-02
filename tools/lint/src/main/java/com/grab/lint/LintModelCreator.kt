package com.grab.lint

import com.android.tools.lint.model.LintModelMavenName
import com.android.tools.lint.model.LintModelModuleType.APP
import com.android.tools.lint.model.LintModelModuleType.LIBRARY
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolute
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

class LintModelCreator(
    private val env: Env = Env.BazelEnv
) {
    /**
     * Create a module.xml and variant.xml and return the relative directory path
     */
    fun create(
        projectName: String,
        android: Boolean,
        library: Boolean,
        compileSdkVersion: String?,
        minSdkVersion: String,
        targetSdkVersion: String,
        partialResultsDir: File,
        modelsDir: File,
        srcs: List<String>,
        resources: List<String>,
        manifest: File?,
        mergedManifest: File?,
        resConfigs: List<String>,
        packageName: String?,
        javaSourceLevel: String = "1.8" // TODO pass from bazel
    ): File {
        val modelPath = modelsDir.toPath().createDirectories()
        val lintModelXml = modelPath.resolve("module.xml")
        val buildDir = modelPath.resolve("build").createDirectories() // Hack - if needed create a dedicate build folder
        val lintModuleType = when {
            library -> LIBRARY
            else -> APP
        }

        // Manually relativize path from models dir to exec root
        val pwd = Paths.get(env.pwd)
        val relativeProjectPath = modelPath.absolute().relativize(pwd.absolute())

        lintModelXml.writeText(
            """
            |<lint-module
            |   dir="$relativeProjectPath"
            |   name="$projectName"
            |   type="${lintModuleType.name}"
            |   maven="${LintModelMavenName.NON_MAVEN}"
            |   buildFolder="${buildDir.toFile()}"
            |   javaSourceLevel="$javaSourceLevel"
            |   compileTarget="$compileSdkVersion"
            |   neverShrinking="true">
            |   <lintOptions />
            |   <variant name="main"/>
            |</lint-module>
        """.trimMargin()
        )

        val variant = modelsDir.resolve("main.xml")
        variant.writeText(
            buildVariant(
                android = android,
                library = library,
                minSdkVersion = minSdkVersion,
                targetSdkVersion = targetSdkVersion,
                packageName = packageName,
                partialResultsDir = partialResultsDir,
                resConfigs = resConfigs,
                mergedManifest = mergedManifest,
                buildDir = buildDir,
                sourceProvider = buildSourceProvider(srcs, resources, manifest)
            )
        )

        val artifact = modelsDir.resolve("main-artifact-libraries.xml")
        artifact.writeText(
            """
            |<libraries>
            |</libraries>
        """.trimMargin()
        )
        val dependencies = modelsDir.resolve("main-artifact-dependencies.xml")
        dependencies.writeText(
            """
            |<dependencies>
            |</dependencies>
        """.trimMargin()
        )
        return modelsDir
    }

    private fun buildSourceProvider(
        srcs: List<String>,
        resources: List<String>,
        manifest: File?
    ): String {
        // TODO assets
        return """
            |<sourceProvider
            |    ${manifest?.let { """manifest="$it"""" } ?: ""}
            |    ${dirString(srcs)?.let { """javaDirectories="$it"""" } ?: ""}
            |    ${dirString(resources, isResources = true)?.let { """resDirectories="$it"""" } ?: ""}/>
        """.trimMargin()
    }


    @Suppress("UnnecessaryVariable", "UNUSED_PARAMETER")
    private fun buildVariant(
        android: Boolean,
        library: Boolean,
        minSdkVersion: String,
        targetSdkVersion: String,
        packageName: String?,
        partialResultsDir: File,
        resConfigs: List<String>,
        mergedManifest: File?,
        buildDir: Path,
        sourceProvider: String
    ): String {
        val sdkVersions = if (android && !library) """
            |minSdkVersion="$minSdkVersion"
            |    targetSdkVersion="$targetSdkVersion"
        """.trimMargin() else ""

        val resourceConfigurations = if (resConfigs.isNotEmpty()) """
            |resourceConfigurations="${resConfigs.joinToString(separator = ",")}"
        """.trimMargin() else ""

        val variant = """
        |<variant
        |    name="main"
        |    $sdkVersions
        |    debuggable="true"
        |    useSupportLibraryVectorDrawables="true" 
        |    ${packageName?.let { """package="$it"""" } ?: ""}
        |    partialResultsDir="$partialResultsDir"
        |    $resourceConfigurations
        |    mergedManifest="$mergedManifest">
        |    <buildFeatures />
        |    <sourceProviders>
        |    </sourceProviders>
        |    <artifact
        |      type="MAIN"
        |      classOutputs="${buildDir.resolve("classes").createDirectories()}"
        |      applicationId="$packageName">
        |    </artifact>
        |</variant>
        |""".trimMargin()
        return variant
    }

    /**
     * Provided a list of file paths return the common parent directory.
     * @param srcs List of file paths
     * @param isResources If the paths are for android resources
     */
    private fun dirString(srcs: List<String>, isResources: Boolean = false): String? {
        return when {
            srcs.isEmpty() -> null
            srcs.size == 1 -> {
                val filePath = Paths.get(srcs.first())
                if (isResources) { // If given src/res/values.xml, then return src/res/
                    filePath.parent.parent.toString()
                } else {
                    filePath.parent.toString()
                }
            }

            else -> commonParentDir(*srcs.toTypedArray())
        }
    }

    /**
     * Given a list of file paths like `/src/main`, /src/test`, will return the longest common path (`/src/`) among them.
     */
    private fun commonParentDir(vararg paths: String): String {
        var commonPath = ""
        val folders: Array<Array<String>> = Array(paths.size) {
            emptyArray()
        }
        for (i in paths.indices) {
            folders[i] = paths[i].split("/").toTypedArray()
        }
        for (j in folders[0].indices) {
            val s = folders[0][j]
            for (i in 1 until paths.size) {
                if (s != folders[i][j]) return commonPath
            }
            commonPath += "$s/"
        }
        return commonPath
    }
}
