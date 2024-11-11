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

package com.grab.aapt.databinding.rclass.parser


import com.grab.aapt.databinding.rclass.parser.xml.DeclareStyleableParser
import com.grab.aapt.databinding.rclass.parser.xml.DefaultXmlParser
import com.grab.test.BaseTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class ResToRStyleableValueParserTest : BaseTest() {

    private lateinit var resToRParser: ResToRParser
    private val value = "0"

    @Before
    fun setUp() {
        val providedParsers = mapOf(
            ParserType.STYLEABLE_PARSER to DeclareStyleableParser(),
            ParserType.DEFAULT_PARSER to DefaultXmlParser()
        )
        resToRParser = DefaultResToRParser(providedParsers, false)
    }

    @Test
    fun `assert styleable values are parsed correctly`() {
        val listTemp = testResFiles(
            TestResFile(
                "styleable.xml",
                contents = """
                <resources>
                    <declare-styleable name="StyleableView">
                        <attr name="colorAttr" />
                        <attr name="sizeAttr" />
                        <attr name="android:drawableTint" format="reference" />
                        <attr name="useCase" format="enum">  
                            <enum name="rating" value="0"/>
                            <enum name="postTransit" value="2"/>
                        </attr>
                    </declare-styleable>
                </resources>
                """.trimIndent(), path = "/src/res/values/"
            )
        )

        val result = resToRParser.parse(
            listTemp,
            emptyList()
        ) as MutableMap<Type, MutableSet<RFieldEntry>>
        val parentValue = "{ 0,0,0,0,0,0 }"
        val exptectedStyleable = setOf(
            RFieldEntry(Type.STYLEABLE, "StyleableView_colorAttr", value),
            RFieldEntry(Type.STYLEABLE, "StyleableView_sizeAttr", value),
            RFieldEntry(Type.STYLEABLE, "StyleableView_android_drawableTint", value),
            RFieldEntry(Type.STYLEABLE, "StyleableView_useCase", value),
            RFieldEntry(Type.STYLEABLE, "StyleableView_rating", value),
            RFieldEntry(Type.STYLEABLE, "StyleableView_postTransit", value),
            RFieldEntry(Type.STYLEABLE, "StyleableView", parentValue, isArray = true),
        )

        val exptectedAttrs = setOf(
            RFieldEntry(Type.ATTR, "colorAttr", value),
            RFieldEntry(Type.ATTR, "sizeAttr", value),
            RFieldEntry(Type.ATTR, "android_drawableTint", value),
            RFieldEntry(Type.ATTR, "useCase", value),
            RFieldEntry(Type.ATTR, "rating", value),
            RFieldEntry(Type.ATTR, "postTransit", value),
        )

        assertEquals(
            mapOf(
                Type.STYLEABLE to exptectedStyleable,
                Type.ATTR to exptectedAttrs
            ), result
        )
    }

    @Test
    fun `assert styleable values are parsed correctly (dot will be replaced with underscore)`() {
        val listTemp = testResFiles(
            TestResFile(
                "styleable.xml",
                contents = """
                <resources>
                    <declare-styleable name="Base.StyleableView">
                        <attr name="colorAttr" />
                        <attr name="sizeAttr" />
                    </declare-styleable>
                </resources>
                """.trimIndent(), path = "/src/res/values/"
            )
        )

        val result = resToRParser.parse(
            listTemp,
            emptyList()
        ) as MutableMap<Type, MutableSet<RFieldEntry>>
        val parentValue = "{ 0,0 }"
        val exptectedStyleable = setOf(
            RFieldEntry(Type.STYLEABLE, "Base_StyleableView", parentValue, isArray = true),
            RFieldEntry(Type.STYLEABLE, "Base_StyleableView_colorAttr", value),
            RFieldEntry(Type.STYLEABLE, "Base_StyleableView_sizeAttr", value)
        )

        val exptectedAttrs = setOf(
            RFieldEntry(Type.ATTR, "colorAttr", value),
            RFieldEntry(Type.ATTR, "sizeAttr", value)
        )

        assertEquals(
            mapOf(
                Type.STYLEABLE to exptectedStyleable,
                Type.ATTR to exptectedAttrs
            ), result
        )
    }
}
