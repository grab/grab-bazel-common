package com.google.devtools.build.android

import com.google.devtools.build.android.OutputFixer.EMPTY_RES_CONTENT
import com.grab.aapt.databinding.common.BaseBindingStubTest
import org.junit.Test
import java.nio.file.Files
import kotlin.test.assertTrue

class OutputFixerTest : BaseBindingStubTest() {

    @Test
    fun `assert merged directories does not contain qualifiers`() {
        val tmp = Files.createTempDirectory("tmp").toFile()
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
        val tmp = Files.createTempDirectory("missing").toFile()
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
}