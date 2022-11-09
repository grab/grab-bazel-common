/*
 * Copyright 2021 Grabtaxi Holdings PTE LTE (GRAB)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.grab.aapt.databinding.binding.store

import com.grab.aapt.databinding.common.BaseBindingStubTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CachingBindingClassJsonParserTest : BaseBindingStubTest() {

    private lateinit var cachingBindingClassJsonParser: CachingBindingClassJsonParser

    @Before
    fun setup() {
        cachingBindingClassJsonParser = CachingBindingClassJsonParser()
    }

    @Test
    fun `assert default json is parsed correctly`() {
        val jsonFile = temporaryFolder.newFile().apply {
            writeText(DEFAULT_JSON_CONTENT)
        }
        val results = cachingBindingClassJsonParser.parse(jsonFile)
        assertEquals(2, results.size)
        assertEquals(
            "com.grab.playground.clock.databinding.NodeClockBinding",
            results["node_clock"]
        )
        assertEquals(
            "com.grab.playground.clock.databinding.SimpleLayoutBinding",
            results["simple_layout"]
        )
    }

    @Test
    fun `assert malformed json throws error`() {
        val jsonFile = temporaryFolder.newFile().apply {
            writeText("malformed")
        }
        assertFailsWith<IllegalArgumentException> {
            cachingBindingClassJsonParser.parse(jsonFile)
        }
    }

    @Test
    fun `assert file contents are cached after first parse`() {
        val jsonFile = temporaryFolder.newFile().apply {
            writeText(DEFAULT_JSON_CONTENT)
        }
        val results = cachingBindingClassJsonParser.parse(jsonFile)
        // Delete the file
        jsonFile.delete()
        // Try requesting again
        assertEquals(results, cachingBindingClassJsonParser.parse(jsonFile))
    }

    companion object {
        val DEFAULT_JSON_CONTENT = """
                {
                  "mappings": {
                    "node_clock": {
                      "qualified_name": "com.grab.playground.clock.databinding.NodeClockBinding",
                      "module_package": "com.grab.playground.clock",
                      "variables": {
                        "vm": "com.grab.playground.clock.ClockViewModel"
                      },
                      "implementations": [
                        {
                          "tag": "layout/node_clock",
                          "merge": false,
                          "qualified_name": "com.grab.playground.clock.databinding.NodeClockBindingImpl"
                        }
                      ]
                    },
                    "simple_layout": {
                      "qualified_name": "com.grab.playground.clock.databinding.SimpleLayoutBinding",
                      "module_package": "com.grab.playground.clock",
                      "variables": {
                        "visibility": "int"
                      },
                      "implementations": [
                        {
                          "tag": "layout/simple_layout",
                          "merge": false,
                          "qualified_name": "com.grab.playground.clock.databinding.SimpleLayoutBindingImpl"
                        }
                      ]
                    }
                  }
                }
            """.trimIndent()
    }
}