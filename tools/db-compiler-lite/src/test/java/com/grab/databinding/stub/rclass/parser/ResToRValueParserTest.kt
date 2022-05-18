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

package com.grab.databinding.stub.rclass.parser

import com.grab.databinding.stub.common.BaseBindingStubTest
import com.grab.databinding.stub.rclass.parser.xml.DefaultXmlParser
import com.grab.databinding.stub.rclass.parser.xml.StyleParser
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals


class ResToRValueParserTest : BaseBindingStubTest() {

    private lateinit var resToRParser: ResToRParser
    private val value = "0"

    @Before
    fun setUp() {
        val providedParsers = mapOf(
            ParserType.DEFAULT_PARSER to DefaultXmlParser(),
            ParserType.STYLE_PARSER to StyleParser()
        )
        resToRParser = DefaultResToRParser(providedParsers, false)
    }

    @Test
    fun `assert style values are parsed correctly`() {
        val listTemp = testResFiles(
            TestResFile(
                "themes.xml",
                contents = """
                <resources>
                    <style name="Theme.MyApplication" parent="Theme.MaterialComponents.DayNight.DarkActionBar">
                        <item name="colorPrimary">@color/purple_500</item>
                    </style>
                </resources>
                """.trimIndent(), path = "/src/res/values/"
            )
        )

        val result = resToRParser.parse(
            listTemp,
            emptyList()
        ) as MutableMap<Type, MutableSet<RFieldEntry>>
        val expectedStyle = setOf(RFieldEntry(Type.STYLE, "Theme_MyApplication", value))

        assertEquals(mapOf(Type.STYLE to expectedStyle), result)
    }

    @Test
    fun `assert item values are parsed correctly`() {
        val listTemp = testResFiles(
            // ITEMS
            TestResFile(
                "items.xml",
                contents = """
                <resources>
                        <item name="item_fraction" type="fraction">5%</item>
                        <item name="item_string" type="string">Hello</item>
                        <item name="item_boolean" type="bool">true</item>
                </resources>
                """.trimIndent(), path = "/src/res/values/"
            )
        )

        val result = resToRParser.parse(
            listTemp,
            emptyList()
        ) as MutableMap<Type, MutableSet<RFieldEntry>>
        val expectedFraction = setOf(RFieldEntry(Type.FRACTION, "item_fraction", value))
        val expectedString = setOf(RFieldEntry(Type.STRING, "item_string", value))
        val expectedBool = setOf(RFieldEntry(Type.BOOL, "item_boolean", value))

        assertEquals(
            mapOf(
                Type.STRING to expectedString,
                Type.FRACTION to expectedFraction,
                Type.BOOL to expectedBool
            ), result
        )

    }

    @Test
    fun `assert color values are parsed correctly`() {
        val listTemp = testResFiles(
            // COLORS
            TestResFile(
                "colors.xml",
                contents = """
                <?xml version="1.0" encoding="utf-8"?>
                <resources>
                    <color name="purple_200">#FFBB86FC</color>
                    <color name="purple_500">#FF6200EE</color>
                </resources>
                """.trimIndent(), path = "/src/res/values/"
            )
        )

        val result = resToRParser.parse(
            listTemp,
            emptyList()
        ) as MutableMap<Type, MutableSet<RFieldEntry>>
        val expectedColor = setOf(
            RFieldEntry(Type.COLOR, "purple_200", value),
            RFieldEntry(Type.COLOR, "purple_500", value)
        )

        assertEquals(mapOf(Type.COLOR to expectedColor), result)
    }

    @Test
    fun `assert dimen values are parsed correctly`() {
        val listTemp = testResFiles(
            // COLORS
            TestResFile(
                "dimen.xml",
                contents = """
                <?xml version="1.0" encoding="utf-8"?>
                <resources>
                    <dimen name="size_5dp">5dp</dimen>
                </resources>
                """.trimIndent(), path = "/src/res/values/"
            )
        )

        val result = resToRParser.parse(
            listTemp,
            emptyList()
        ) as MutableMap<Type, MutableSet<RFieldEntry>>
        val expectedDimen = setOf(RFieldEntry(Type.DIMEN, "size_5dp", value))

        assertEquals(mapOf(Type.DIMEN to expectedDimen), result)
    }

    @Test
    fun `assert plurals values are parsed correctly`() {
        val listTemp = testResFiles(
            // COLORS
            TestResFile(
                "plurals.xml",
                contents = """
                <?xml version="1.0" encoding="utf-8"?>
                <plurals name="proposal_plurals">
                        <item quantity="zero">No proposals</item>
                </plurals>
                """.trimIndent(), path = "/src/res/values/"
            )
        )

        val result = resToRParser.parse(
            listTemp,
            emptyList()
        ) as MutableMap<Type, MutableSet<RFieldEntry>>
        val expectedPlural = setOf(RFieldEntry(Type.PLURALS, "proposal_plurals", value))

        assertEquals(mapOf(Type.PLURALS to expectedPlural), result)
    }

    @Test
    fun `assert attr values are parsed correctly`() {
        val listTemp = testResFiles(
            // COLORS
            TestResFile(
                "attrs.xml",
                contents = """
                <?xml version="1.0" encoding="utf-8"?>
                <resources>
                        <attr name="value" format="string"/>
                </resources>
                """.trimIndent(), path = "/src/res/values/"
            )
        )

        val result = resToRParser.parse(
            listTemp,
            emptyList<String>()
        ) as MutableMap<Type, MutableSet<RFieldEntry>>
        val expectedAttrs = setOf(RFieldEntry(Type.ATTR, "value", value))

        assertEquals(mapOf(Type.ATTR to expectedAttrs), result)
    }

    @Test
    fun `assert string values are parsed correctly`() {
        val listTemp = testResFiles(
            // STRING
            TestResFile(
                "string.xml",
                contents = """
                    <resources>
                        <string name="app_name">Playground</string>
                    </resources>
                    """.trimIndent(),
                path = "/src/res/values/"
            ),

            // ITEMS
            TestResFile(
                "items.xml",
                contents = """
                <resources>
                        <item name="item_string" type="string">Hello Item</item>
                </resources>
                """.trimIndent(), path = "/src/res/values/"
            )

        )

        val result = resToRParser.parse(
            listTemp,
            emptyList()
        ) as MutableMap<Type, MutableSet<RFieldEntry>>
        val expectedString = setOf(
            RFieldEntry(Type.STRING, "app_name", value),
            RFieldEntry(Type.STRING, "item_string", value)
        )
        assertEquals(mapOf(Type.STRING to expectedString), result)
    }
}