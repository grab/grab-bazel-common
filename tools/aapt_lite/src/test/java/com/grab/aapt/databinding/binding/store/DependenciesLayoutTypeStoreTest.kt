package com.grab.aapt.databinding.binding.store

import com.grab.aapt.databinding.binding.store.CachingBindingClassJsonParserTest.Companion.DEFAULT_JSON_CONTENT
import com.grab.test.BaseTest

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import org.junit.Before
import org.junit.Test
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DependenciesLayoutTypeStoreTest : BaseTest() {

    private lateinit var dependenciesLayoutTypeStore: DependenciesLayoutTypeStore

    @Before
    fun setup() {
        val classInfoDir = temporaryFolder.newFolder("classInfoDir")
        dependenciesLayoutTypeStore = DependenciesLayoutTypeStore(
            baseDir = temporaryFolder.newFolder("base"),
            classInfoZips = listOf(
                createClassInfoZip(classInfoDir, "clock", DEFAULT_JSON_CONTENT),
                createClassInfoZip(classInfoDir, "clock2", DEFAULT_JSON_CONTENT)
            ),
            bindingClassJsonParser = CachingBindingClassJsonParser()
        )
    }

    /**
     * Create a classInfo.zip in `temporaryFolder` root with the name `$prefix_classInfo.zip` with
     * `jsonContents` for binding class json.
     *
     * @param directory The temporary directory where fake classInfos will be written to.
     * @param module The module name used to differentiate multiple classInfos.
     * @param jsonContents The contents of binding class jsons
     */
    private fun createClassInfoZip(directory: File, module: String, jsonContents: String): File {
        val bindingClassJsonName = "com.grab.playground_binding_classes"
        val bindingClassJson = File
            .createTempFile(bindingClassJsonName, ".json")
            .apply { writeText(jsonContents) }
        val output = File(File(directory, module), "classInfo.zip").apply {
            parentFile?.mkdirs()
            createNewFile()
        }
        ZipOutputStream(BufferedOutputStream(FileOutputStream(output))).use { zos ->
            ZipEntry("$bindingClassJsonName.json").also { zos.putNextEntry(it) }
            bindingClassJson.inputStream().copyTo(zos)
        }
        return output
    }

    private fun extractedFiles() = dependenciesLayoutTypeStore
        .extractionDir
        .walkTopDown()
        .filter { it.isFile }
        .toList()

    @Test
    fun `assert dependenciesLayoutTypeStore caches classInfoZip extractions`() {
        // Request random type name that does not exist
        dependenciesLayoutTypeStore["typename"]
        // Ensure contents classInfozip was extracted
        val extractedFiles = extractedFiles()
        assertTrue("Extracted 2 binding class json files initially") { extractedFiles.size == 2 }
        // Delete the files manually
        extractedFiles.forEach { it.delete() }
        dependenciesLayoutTypeStore["typename"]
        assertTrue("New request responds without extraction") {
            extractedFiles().isEmpty()
        }
    }

    @Test
    fun `assert valid request is cached`() {
        val extractedFiles = extractedFiles()
        assertTrue("Initially no files are extracted") { extractedFiles.isEmpty() }
        val type: TypeName? = dependenciesLayoutTypeStore["node_clock"]
        assertTrue("Type name is found") {
            type != null && (type as ClassName).packageName() == "com.grab.playground.clock.databinding"
        }
        assertTrue("Extraction stops on first found type") {
            extractedFiles().size == 1
        }
        extractedFiles().forEach { it.delete() }
        dependenciesLayoutTypeStore["node_clock"]
        assertNotNull("New request responds without extraction") {
            extractedFiles().isEmpty()
        }
    }
}