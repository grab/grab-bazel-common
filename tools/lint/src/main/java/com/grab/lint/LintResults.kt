package com.grab.lint

import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class LintResults(
    private val name: String,
    private val lintResultsFile: File,
    private val elapsed: Long,
    private val resultCodeFile: File,
    private val outputJunitXml: File,
    private val failOnInformation: Boolean = true,
    private val failOnWarnings: Boolean = true,
) {
    fun process() {
        try {
            val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val lintResults = documentBuilder.parse(lintResultsFile)

            val issues = lintResults.getElementsByTagName("issue").elements()
            val errorIssues = issues.filter { issue ->
                val id = issue["id"]
                val message = issue["message"]
                val severity = issue["severity"]
                when (id) {
                    in ALLOWED_ISSUES -> ALLOWED_ISSUES[id].toString() !in message // Only fail if expected message does not match
                    else -> when (severity) {
                        "Fatal", "Error" -> true
                        "Warning" -> failOnWarnings
                        "Information" -> failOnInformation
                        else -> true
                    }
                }
            }
            val hasErrors = errorIssues.isNotEmpty()
            buildJunitReport(documentBuilder, errorIssues)
            resultCodeFile.writeText(if (hasErrors) "1" else "0")
        } catch (e: Exception) {
            Files.copy(
                lintResultsFile.toPath(),
                outputJunitXml.toPath(), StandardCopyOption.REPLACE_EXISTING
            )
            resultCodeFile.writeText("1")
        }
    }

    private fun buildJunitReport(documentBuilder: DocumentBuilder, errorIssues: List<Element>) {
        val junitDoc = documentBuilder.newDocument()
        val rootTestSuites = junitDoc.createElement("testsuites").also(junitDoc::appendChild)

        when {
            errorIssues.isEmpty() -> addSuccessfulTestCase(junitDoc, rootTestSuites)
            else -> errorIssues.groupBy { it["id"] }.forEach { (id, issues) ->
                val failures = issues.size.toString()
                val testSuite = junitDoc.createElement("testsuite").also {
                    it["name"] = id
                    it["tests"] = failures
                    it["failures"] = failures
                    it["time"] = TimeUnit.MILLISECONDS.toSeconds(elapsed).toString()
                }
                rootTestSuites.appendChild(testSuite)
                issues.forEach { issue -> mapIssueToTestCase(issue, junitDoc, testSuite) }
            }
        }

        try {
            FileWriter(outputJunitXml).use { fileWriter ->
                with(TransformerFactory.newInstance().newTransformer()) {
                    setOutputProperty(OutputKeys.INDENT, "yes")
                    setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
                    transform(DOMSource(junitDoc), StreamResult(fileWriter))
                }
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    private fun addSuccessfulTestCase(junitDoc: Document, rootTestSuites: Element) {
        val testSuite = junitDoc.createElement("testsuite").also {
            it["name"] = name
            it["tests"] = "1"
            it["time"] = TimeUnit.MILLISECONDS.toSeconds(elapsed).toString()
            it.appendChild(junitDoc.createElement("testcase").also { testcase ->
                testcase["name"] = "Android lint on $name"
                testcase["classname"] = name
            })
        }
        rootTestSuites.appendChild(testSuite)
    }

    private fun mapIssueToTestCase(
        issue: Element,
        junitDoc: Document,
        testSuite: Element
    ) {
        val message = issue["message"]
        val summary = issue["summary"]
        val location = issue.getElementsByTagName("location").elements().first()
        val file = location["file"].replace("../", "")
        val line = location["line"]
        val explanation = listOf(
            issue["explanation"],
            "\nFile: $file:$line",
            "Error line:",
            issue["errorLine1"],
            issue["errorLine2"]
        ).joinToString(separator = "\n")

        val testCase = junitDoc.createElement("testcase").also {
            it["name"] = message
            it["classname"] = summary
            it["file"] = file
            it["line"] = line
        }
        val failure = junitDoc.createElement("failure").also {
            it["message"] = explanation
        }
        testCase.appendChild(failure)
        testSuite.appendChild(testCase)
    }

    companion object {
        /**
         * Map of Android Lint Issue id to message text that is recognized as success.
         */
        private val ALLOWED_ISSUES = mapOf(
            "LintBaseline" to "filtered out because",
            "LintBaselineFixed" to "perhaps they have been fixed"
        )
    }
}