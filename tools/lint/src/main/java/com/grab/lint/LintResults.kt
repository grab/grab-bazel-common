package com.grab.lint

import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class LintResults(
    val resultCodeFile: File,
    val lintResultsFile: File,
) {
    private fun NodeList.elements() = (0 until length).map { item(it) as Element }

    fun process() {
        try {
            val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val lintResults = documentBuilder.parse(lintResultsFile)

            val issues = lintResults.getElementsByTagName("issue")
            val hasErrors = issues.elements().any { it.getAttribute("id") != "LintBaseline" }
            resultCodeFile.writeText(if (hasErrors) "1" else "0")
        } catch (e: Exception) {
            e.printStackTrace()
            resultCodeFile.writeText("1")
        }
    }
}