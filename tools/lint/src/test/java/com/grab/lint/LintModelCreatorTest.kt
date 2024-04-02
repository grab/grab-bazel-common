package com.grab.lint

import com.android.sdklib.AndroidVersion
import com.android.tools.lint.model.LintModelSerialization
import com.grab.cli.WorkingDirectory
import com.grab.test.BaseTest
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LintModelCreatorTest : BaseTest() {

    private lateinit var lintModelCreator: LintModelCreator
    private lateinit var workingDir: Path
    private lateinit var mergedManifest: Path
    private lateinit var partialResultsDir: Path
    private lateinit var modelsDir: File

    @Before
    fun setUp() {
        workingDir = WorkingDirectory(dir = temporaryFolder.newFolder("tmp").toPath()).dir
        lintModelCreator = LintModelCreator()
        mergedManifest = workingDir.resolve("AndroidManifest.xml").apply { writeText("") }
        partialResultsDir = workingDir.resolve("partial-results-dir").createDirectories()
        modelsDir = workingDir.resolve("models-dir").toFile()
    }

    @Test
    fun `assert created lint model xmls are parseable by lint model serialization`() {
        lintModelCreator.create(
            compileSdkVersion = "30",
            android = true,
            library = false,
            projectName = "//test",
            minSdkVersion = "21",
            targetSdkVersion = "34",
            mergedManifest = mergedManifest.toFile(),
            partialResultsDir = partialResultsDir.toFile(),
            modelsDir = modelsDir,
            srcs = emptyList(),
            resources = emptyList(),
            manifest = null,
            packageName = "com.android.lint",
            javaSourceLevel = "1.7",
            resConfigs = listOf("en", "id")
        )
        val lintModel = LintModelSerialization.readModule(
            source = modelsDir,
            readDependencies = true, // Lint serialization calls with true
        )
        assertNotNull(lintModel, "Lint model is parsed")
        assertTrue("Lint variant is parsed") { lintModel.variants.isNotEmpty() }
        val variant = lintModel.variants.first()
        assertEquals(AndroidVersion("21"), variant.minSdkVersion, "Min sdk version is parsed")
        assertEquals(AndroidVersion("34"), variant.targetSdkVersion, "Target sdk version is parsed")
        assertEquals(mergedManifest.toFile(), variant.mergedManifest, "Merged manifest is parsed")
        assertTrue("Parsed manifest file exists") { variant.mergedManifest?.exists() == true }
        assertEquals(partialResultsDir.toFile(), variant.partialResultsDir, "Partial results is parsed")
        assertEquals("com.android.lint", variant.`package`, "Package is parsed") //TODO(arun) Pass from bazel
        assertEquals(listOf("en", "id"), variant.resourceConfigurations, "ResConfigs are parsed")
    }

    @Test
    fun `assert create non android library modules don't have sdk versions`() {
        lintModelCreator.create(
            compileSdkVersion = "30",
            android = false,
            library = false,
            projectName = "//test",
            minSdkVersion = "21",
            targetSdkVersion = "34",
            mergedManifest = mergedManifest.toFile(),
            partialResultsDir = partialResultsDir.toFile(),
            modelsDir = modelsDir,
            srcs = emptyList(),
            resources = emptyList(),
            manifest = null,
            packageName = "com.android.lint",
            javaSourceLevel = "1.7",
            resConfigs = listOf("en", "id")
        )
        val variantXml = modelsDir.resolve("main.xml").readText()
        assertTrue("Does not contain minSdkVersion") { !variantXml.contains("minSdkVersion") }
        assertTrue("Does not contain targetSdkVersion") { !variantXml.contains("targetSdkVersion") }
    }
}