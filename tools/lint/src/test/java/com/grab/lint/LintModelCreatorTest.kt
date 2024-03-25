package com.grab.lint

import com.android.sdklib.AndroidVersion
import com.android.tools.lint.model.LintModelSerialization
import com.grab.cli.WorkingDirectory
import com.grab.test.BaseTest
import org.junit.Before
import org.junit.Test
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LintModelCreatorTest : BaseTest() {

    private lateinit var lintModelCreator: LintModelCreator
    private lateinit var workingDir: Path

    @Before
    fun setUp() {
        workingDir = WorkingDirectory(dir = temporaryFolder.newFolder("tmp").toPath()).dir
        lintModelCreator = LintModelCreator()
    }

    @Test
    fun `assert created lint model xmls are parseable by lint model serialization`() {
        val mergedManifest = workingDir.resolve("AndroidManifest.xml").apply { writeText("") }
        val partialResultsDir = workingDir.resolve("partial-results-dir").createDirectories()
        val modelsDir = workingDir.resolve("models-dir").toFile()
        lintModelCreator.create(
            compileSdkVersion = "30",
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
}