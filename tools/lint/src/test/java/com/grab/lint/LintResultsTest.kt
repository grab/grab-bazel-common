package com.grab.lint

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LintResultsTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var lintResultXml: File
    private lateinit var resultCode: File
    private lateinit var outputJunitXml: File
    private lateinit var lintResults: LintResults

    fun setup(failOnInformation: Boolean = true, failOnWarnings: Boolean = true, block: () -> Unit) {
        lintResultXml = temporaryFolder.newFile("result.xml")
        resultCode = temporaryFolder.newFile("result_code")
        outputJunitXml = temporaryFolder.newFile("output.xml")
        lintResults = LintResults(
            name = "Test",
            lintResultsFile = lintResultXml,
            elapsed = 0L,
            resultCodeFile = resultCode,
            outputJunitXml = outputJunitXml,
            failOnInformation = failOnInformation,
            failOnWarnings = failOnWarnings
        )
        block()
    }

    private fun assertResultCode(value: String, message: String) {
        assertTrue("Result created") { resultCode.exists() }
        assertEquals(value, resultCode.readText(), message)
    }

    @Test
    fun `test empty lint xml is considered as pass`() = setup {
        lintResultXml.writeText(
            """
            <issues format="6" by="lint 8.0.2">
            </issues>
        """.trimIndent()
        )
        lintResults.process()
        assertResultCode("0", "Result is 0 for empty xml")
    }

    @Test
    fun `test lint xml errors are parsed and exit code is calculated`() = setup {
        lintResultXml.writeText(
            """
            <issues format="6" by="lint 8.0.2">

                $BASELINE_ISSUE
                
                $BASELINE_FIXED_ISSUE

            </issues>
        """.trimIndent()
        )
        lintResults.process()
        assertResultCode("0", "Result is 0 when only baseline entry is there.")
    }

    @Test
    fun `assert when Fail for Information severity is true, result code is 1`() = setup(failOnInformation = true) {
        lintResultXml.writeText(INFORMATION_ISSUE)
        lintResults.process()
        assertResultCode("1", "Result is 1 when additional errors are there")
    }

    @Test
    fun `assert when Fail for Information severity is false, result code is 0`() = setup(failOnInformation = false) {
        lintResultXml.writeText(INFORMATION_ISSUE)
        lintResults.process()
        assertResultCode("0", "Result is 0 when Fail for Information severity is false")
    }

    @Test
    fun `assert when Fail for Warnings severity is true, result code is 1`() = setup(failOnWarnings = true) {
        lintResultXml.writeText(WARNING_ISSUE)
        lintResults.process()
        assertResultCode("1", "Result is 1 when Fail for Warnings severity is true")
    }

    @Test
    fun `assert when Fail for Warning severity is false, result code is 0`() = setup(failOnWarnings = false) {
        lintResultXml.writeText(WARNING_ISSUE)
        lintResults.process()
        assertResultCode("0", "Result is 0 when Fail for Warning severity is false")
    }

    @Test
    fun `assert when any other error apart from LintBaseLine are there, result code is 1`() = setup {
        lintResultXml.writeText(
            """
           <issues format="6" by="lint 8.0.2">

                $BASELINE_ISSUE
                
                $LONGTAG_ISSUE

            </issues>
        """.trimIndent()
        )
        lintResults.process()
        assertResultCode("1", "Result is 1 when additional errors are there")
    }

    @Test
    fun `assert when result file is malformed or does not exist, result is 1`() = setup {
        lintResults.process()
        assertResultCode("1", "Result is 1 when file does not exist")
        lintResultXml.writeText(
            """
            <<<
        """.trimIndent()
        )
        assertResultCode("1", "Result is 1 when file is malformed")
    }


    private fun Document.assertNode(nodeName: String, vararg expectedAttributes: Pair<String, String>) {
        val givenNode = getElementsByTagName(nodeName)
        assertEquals(1, givenNode.length, "$nodeName is generated")
        val nodeElement = givenNode.item(0) as Element
        mapOf(*expectedAttributes).forEach { (attributeName, expected) ->
            assertEquals(expected, nodeElement.getAttribute(attributeName), "$nodeName's $attributeName is added")
        }
    }

    @Test
    fun `assert output junit xml is generated`() = setup {
        lintResultXml.writeText(INFORMATION_ISSUE)
        lintResults.process()
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(outputJunitXml).apply {
            assertNode(
                "testsuites",
                "name" to "Test",
                "tests" to "1",
                "time" to "0"
            )
            assertNode(
                "testsuite",
                "name" to "MissingApplicationIcon",
                "tests" to "1",
                "failures" to "1"
            )
            assertNode(
                "testcase",
                "name" to "Should explicitly set `android:icon`, there is no default",
                "classname" to "Missing application icon",
                "file" to "tests/android/binary/src/main/AndroidManifest.xml",
                "line" to "4",
            )
            assertNode(
                "failure",
                "message" to """You should set an icon for the application as whole because there is no default. This attribute must be set as a reference to a drawable resource containing the image (for example `@drawable/icon`).

File: tests/android/binary/src/main/AndroidManifest.xml:4
Error line:
    <application android:label="@string/app_name">
     ~~~~~~~~~~~"""
            )
        }
    }

    companion object {
        private val BASELINE_ISSUE = """
            <issue
                id="LintBaseline"
                severity="Information"
                message="7 warnings were filtered out because they are listed in the baseline file, baseline.xml&#xA;"
                category="Lint"
                priority="10"
                summary="Baseline Issues"
                explanation="Lint can be configured with a &quot;baseline&quot;; a set of current issues found in a codebase, which future runs of lint will silently ignore. Only new issues not found in the baseline are reported.&#xA;&#xA;Note that while opening files in the IDE, baseline issues are not filtered out; the purpose of baselines is to allow you to get started using lint and break the build on all newly introduced errors, without having to go back and fix the entire codebase up front. However, when you open up existing files you still want to be aware of and fix issues as you come across them.&#xA;&#xA;This issue type is used to emit two types of informational messages in reports: first, whether any issues were filtered out so you don&apos;t have a false sense of security if you forgot that you&apos;ve checked in a baseline file, and second, whether any issues in the baseline file appear to have been fixed such that you can stop filtering them out and get warned if the issues are re-introduced.">
                <location
                    file="baseline.xml"/>
            </issue>
        """.trimIndent()

        private val BASELINE_FIXED_ISSUE = """
            <issue
                id="LintBaselineFixed"
                severity="Information"
                message="2 errors/warnings were listed in the baseline file (baseline.xml) but not found in the project; perhaps they have been fixed? Unmatched issue types: LogNotTimberTest, LongLogTag"
                category="Lint"
                priority="10"
                summary="Baselined Issues Fixed"
                explanation="If a lint baseline describes a problem which is no longer reported, then the problem has either been fixed, or perhaps the issue type has been disabled. In any case, the entry can be removed from the baseline (such that if the issue is reintroduced at some point, lint will complain rather than just silently starting to match the old baseline entry again.)">
                <location
                    file="baseline.xml"/>
            </issue>
        """.trimIndent()

        private val LONGTAG_ISSUE = """
            <issue
                id="LongLogTag"
                severity="Error"
                message="The logging tag can be at most 23 characters, was 46 (SomeReallyLongTagForLintToWhine)"
                category="Correctness"
                priority="5"
                summary="Too Long Log Tags"
                explanation="Log tags are only allowed to be at most 23 tag characters long.">
                <location
                    file="../../../../../../private/var/tmp/_bazel_hello/db23b8d128409b6396481457deacb461/sandbox/darwin-sandbox/89/execroot/grazel/sample-android/src/main/java/com/grab/grazel/android/sample/ComposeActivity.kt"
                    line="34"
                    column="29"/>
            </issue>
        """.trimIndent()

        private val INFORMATION_ISSUE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <issues format="6" by="lint 8.0.2">
                <issue
                    id="MissingApplicationIcon"
                    severity="Information"
                    message="Should explicitly set `android:icon`, there is no default"
                    category="Usability:Icons"
                    priority="5"
                    summary="Missing application icon"
                    explanation="You should set an icon for the application as whole because there is no default. This attribute must be set as a reference to a drawable resource containing the image (for example `@drawable/icon`)."
                    url="https://developer.android.com/studio/publish/preparing#publishing-configure"
                    urls="https://developer.android.com/studio/publish/preparing#publishing-configure"
                    errorLine1="    &lt;application android:label=&quot;@string/app_name&quot;>"
                    errorLine2="     ~~~~~~~~~~~">
                    <location
                        file="../../../../../../../tests/android/binary/src/main/AndroidManifest.xml"
                        line="4"
                        column="6"/>
                </issue>
            </issues>
        """.trimIndent()

        private val WARNING_ISSUE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <issues format="6" by="lint 8.0.2">
                <issue
                    id="PrivateResource"
                    severity="Warning"
                    message="The resource `@color/material_blue_grey_950` is marked as private in material-1.2.1.aar"
                    category="Correctness"
                    priority="3"
                    summary="Using private resources"
                    explanation="Private resources should not be referenced; the may not be present everywhere, and even where they are they may disappear without notice.&#xA;&#xA;To fix this, copy the resource into your own project instead."
                    errorLine1="    val material_res = com.google.android.material.R.color.material_blue_grey_950"
                    errorLine2="                                                           ~~~~~~~~~~~~~~~~~~~~~~">
                    <location
                        file="../../../../../../../tests/android/binary/src/main/java/com/grab/test/TestActivity.kt"
                        line="9"
                        column="60"/>
                </issue>

            </issues>
        """.trimIndent()
    }
}