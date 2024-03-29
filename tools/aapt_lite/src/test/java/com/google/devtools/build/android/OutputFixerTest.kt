package com.google.devtools.build.android

import com.google.devtools.build.android.OutputFixer.EMPTY_RES_CONTENT
import com.grab.test.BaseTest
import org.junit.Test
import kotlin.test.assertTrue

class OutputFixerTest : BaseTest() {

    @Test
    fun `assert merged directories does not contain qualifiers`() {
        val tmp = temporaryFolder.newFolder("tmp")
        buildTestRes(tmp) {
            "res/values-v4/strings.xml" {
                """<?xml version="1.0" encoding="UTF-8" standalone="no"?><resources/>"""
            }
            "res/values-in/strings.xml" {
                """<?xml version="1.0" encoding="UTF-8" standalone="no"?><resources/>"""
            }
            "res/values-sw219dp/strings.xml" {
                """<?xml version="1.0" encoding="UTF-8" standalone="no"?><resources/>"""
            }
        }
        OutputFixer.process(tmp, emptyList())
        assertTrue("Qualifiers are removed from merged directory") {
            tmp.walk()
                .filter { it.isDirectory }
                .all { it.name != "values-v4" }
        }
    }


    @Test
    fun `assert missing xml files are stubbed with empty resource file`() {
        val tmp = temporaryFolder.newFolder("tmp")
        OutputFixer.process(
            outputDir = tmp,
            declaredOutputs = sequenceOf("res/values/strings.xml", "res/values-in/strings.xml")
                .map { tmp.resolve(it) }
                .map { it.toString() }.toList()
        )
        assertTrue("Missing output files are stubbed with empty resources") {
            tmp.walk()
                .filter { it.isFile }
                .map { it.readText() }
                .all { it == EMPTY_RES_CONTENT }
        }
    }

    @Test
    fun `assert qualifiers are retained if provided output file paths already contain them`() {
        val tmp = temporaryFolder.newFolder("tmp")
        buildTestRes(tmp) {
            "res/values-v4/strings.xml" {
                """<?xml version="1.0" encoding="UTF-8" standalone="no"?><resources/>"""
            }
        }
        OutputFixer.process(
            outputDir = tmp,
            declaredOutputs = listOf("res/values-v4/strings.xml")
        )
        assertTrue("Qualifiers are retained in merged directory") {
            tmp.walk().any { it.name == "values-v4" && it.isDirectory }
        }
    }
}