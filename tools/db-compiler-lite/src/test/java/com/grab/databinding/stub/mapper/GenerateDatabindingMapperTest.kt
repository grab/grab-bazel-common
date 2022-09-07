package com.grab.databinding.stub.mapper

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipFile
import kotlin.test.assertTrue

class GenerateDatabindingMapperTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `assert databinding mapper generated with package name`() {
        val output = temporaryFolder.newFile("output.srcjar")
        GenerateMapperCommand().main(
            arrayOf(
                "--package",
                "com.grab",
                "--output",
                output.absolutePath
            )
        )
        extractDatabindingMapper(output).readText().let { generatedContent ->
            generatedContent.contains("")
            assertTrue("Package name set") {
                generatedContent.contains("package com.grab;")
            }
            assertTrue("getDataBinder 1 generated") {
                generatedContent.contains(
                    """    @Override
    public ViewDataBinding getDataBinder(DataBindingComponent component, View view, int layoutId) {
        return null;
    }"""
                )
            }
            assertTrue("getDataBinder 2 generated") {
                generatedContent.contains(
                    """    @Override
    public ViewDataBinding getDataBinder(DataBindingComponent component, View[] views, int layoutId) {
        return null;
    }"""
                )
            }
            assertTrue("getLayoutId generated") {
                generatedContent.contains(
                    """    @Override
    public int getLayoutId(String tag) {
        return 0;
    }"""
                )
            }
            assertTrue("convertBrIdToString generated") {
                generatedContent.contains(
                    """    @Override
    public String convertBrIdToString(int localId) {
        return null;
    }"""
                )
            }
        }
    }

    private fun extractDatabindingMapper(outputJar: File?): File {
        ZipFile(outputJar).use { zip ->
            val mapper = zip.entries()
                .asSequence()
                .filter { it.name.contains("DataBinderMapperImpl.java") }
                .firstOrNull() ?: error("DataBinderMapperImpl was not generated")
            zip.getInputStream(mapper).buffered().use { input ->
                val extractedFile = temporaryFolder.newFile("DataBinderMapperImpl")
                extractedFile.writeText(input.reader().readText())
                return extractedFile
            }
        }
    }
}