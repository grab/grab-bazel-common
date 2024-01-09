package com.grab.lint

import com.grab.test.BaseTest
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LintAnalyzeCommandTest : BaseTest() {

    private lateinit var projectRoot: File
    private lateinit var inputRoot: File
    private lateinit var projectXml: File
    private lateinit var jdkHome: File
    private lateinit var lintFile: File
    private lateinit var partialResults: File

    @Before
    fun setup() {
        projectRoot = temporaryFolder.newFolder("root")
        inputRoot = temporaryFolder.newFolder("input")
        projectXml = inputRoot.resolve("project.xml")
        jdkHome = File(System.getProperty("java.home"))
        lintFile = inputRoot.resolve("lint.xml").apply {
            writeText(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <lint>
                </lint>
            """.trimIndent()
            )
        }
        partialResults = inputRoot.resolve("partial_results").apply {
            mkdirs()
        }
    }

    @Test
    fun `assert partial results are generated after running lint analyze`() {
        buildTestRes(projectRoot) {
            "res/values/strings.xml" {
                """
                    <resources>
                        <string name="app_name">Sample</string>
                    </resources>
                """.trimIndent()
            }
            "src/main/java/TestActivity.kt" {
                """
                    package com.grab.test

                    import android.app.Activity
                    import android.os.Bundle
                    import android.os.Parcelable
                    import kotlinx.parcelize.Parcelize

                    class TestActivity : Activity() {
                        override fun onCreate(savedInstanceState: Bundle?) {
                            android.util.Log.d("SomeReallyLongTagForLintToDetectAndWarn", "Log message")
                        }
                    }
                """.trimIndent()
            }
        }
        val allSources = projectRoot.walkBottomUp().filter { it.isFile }
        LintAnalyzeCommand().main(
            listOf(
                "--name", "test",
                "--android",
                "--library",
                "--verbose",
                "--sources",
                allSources.filter { it.name.endsWith("kt") }.joinToString(separator = ","),
                "--resource-files",
                allSources.filter { it.name.endsWith("xml") }.joinToString(separator = ","),
                "--jdk-home", jdkHome.toString(),
                "--lint-config", lintFile.toString(),
                "--partial-results-dir", partialResults.toString(),
                "--compile-sdk-version", "34",
                "--project-xml", projectXml.toString(),
            )
        )
        val partialResults = partialResults.walkTopDown().filter { it.isFile }.toList()
        assertEquals(1, partialResults.size, "Partial results are generated")
        assertEquals("lint-partial-all.xml", partialResults.first().name, "Partial all file is generated")
        assertEquals(
            """<?xml version="1.0" encoding="UTF-8"?>
<incidents format="6" by="lint 8.0.2" type="partial_results">
    <map id="UnusedResources">
        <location id="R.string.app_name"
            file="../root/res/values/strings.xml"
            line="2"
            column="13"
            startOffset="24"
            endLine="2"
            endColumn="28"
            endOffset="39"/>
        <entry
            name="model"
            string="string[app_name(D)];;;;"/>
    </map>

</incidents>
""",
            partialResults.first().readText(),
            "UnusedResources are flagged in partial results"
        )
        assertTrue("Project XML is generated") {
            projectXml.exists()
        }
    }
}