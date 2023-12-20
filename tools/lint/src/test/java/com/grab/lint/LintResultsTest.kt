package com.grab.lint

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LintResultsTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var resultXml: File
    private lateinit var resultCode: File
    private lateinit var lintResults: LintResults

    @Before
    fun setup() {
        resultXml = temporaryFolder.newFile("result.xml")
        resultCode = temporaryFolder.newFile("result_code")
        lintResults = LintResults(resultCode, resultXml)
    }

    private fun assertResultCode(value: String, message: String) {
        assertTrue("Result created") { resultCode.exists() }
        assertEquals(value, resultCode.readText(), message)
    }

    @Test
    fun `test lint xml errors are parsed and exit code is calculated`() {
        resultXml.writeText(
            """
            <issues format="6" by="lint 8.0.2">

                <issue
                    id="LintBaseline"
                    severity="Information"
                    message="1 error and 7 warnings were filtered out because they are listed in the baseline file, baseline.xml&#xA;"
                    category="Lint"
                    priority="10"
                    summary="Baseline Issues"
                    explanation="Lint can be configured with a &quot;baseline&quot;; a set of current issues found in a codebase, which future runs of lint will silently ignore. Only new issues not found in the baseline are reported.&#xA;&#xA;Note that while opening files in the IDE, baseline issues are not filtered out; the purpose of baselines is to allow you to get started using lint and break the build on all newly introduced errors, without having to go back and fix the entire codebase up front. However, when you open up existing files you still want to be aware of and fix issues as you come across them.&#xA;&#xA;This issue type is used to emit two types of informational messages in reports: first, whether any issues were filtered out so you don&apos;t have a false sense of security if you forgot that you&apos;ve checked in a baseline file, and second, whether any issues in the baseline file appear to have been fixed such that you can stop filtering them out and get warned if the issues are re-introduced.">
                    <location
                        file="baseline.xml"/>
                </issue>

            </issues>
        """.trimIndent()
        )
        lintResults.process()
        assertResultCode("0", "Result is 0 when only baseline entry is there.")
    }

    @Test
    fun `assert when any other error apart from LintBaseLine are there, result code is 1`() {
        resultXml.writeText(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <issues format="6" by="lint 8.0.2">

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

                <issue
                    id="LongLogTag"
                    severity="Error"
                    message="The logging tag can be at most 23 characters, was 46 (adfdafdsafsdjfisfjgihdgh ddddfidsdsdohdgd i og)"
                    category="Correctness"
                    priority="5"
                    summary="Too Long Log Tags"
                    explanation="Log tags are only allowed to be at most 23 tag characters long.">
                    <location
                        file="../../../../../../private/var/tmp/_bazel_hello/db23b8d128409b6396481457deacb461/sandbox/darwin-sandbox/89/execroot/grazel/sample-android/src/main/java/com/grab/grazel/android/sample/ComposeActivity.kt"
                        line="34"
                        column="29"/>
                </issue>

            </issues>
        """.trimIndent()
        )
        lintResults.process()
        assertResultCode("1", "Result is 1 when additional errors are there")
    }

    @Test
    fun `assert when result file is malformed or does not exist, result is 1`() {
        lintResults.process()
        assertResultCode("1", "Result is 1 when file does not exist")
        resultXml.writeText(
            """
            <<<
        """.trimIndent()
        )
        assertResultCode("1", "Result is 1 when file is malformed")
    }
}