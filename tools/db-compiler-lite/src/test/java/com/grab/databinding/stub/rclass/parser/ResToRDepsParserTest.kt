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
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ResToRDepsParserTest : BaseBindingStubTest() {

    private lateinit var resToRParser: ResToRParser

    private val value = "0"

    @Before
    fun setUp() {
        resToRParser = DefaultResToRParser(emptyMap(), false)
    }

    @Test
    fun `assert R txt deps are parsed correctly`() {
        val value = "0"
        val contextRTxts = """
            int anim fade_up 0x00000000
            int animator fade_up_down 0x00000000
            int array array 0x00000000
            int attr helperTextEnabled 0x00000000
            int bool enabled 0x00000000
            int color primary_text 0x00000000
            int dimen design_tab 0x00000000
            int drawable abc_ic_voice 0x00000000
            int id add 0x00000000
            int integer add_value 0x00000000
            int interpolator interpolator 0x00000000
            int layout design_bottom 0x00000000
            int menu menu 0x00000000
            int mipmap mipmap 0x00000000
            int plurals plurals 0x00000000
            int raw raw 0x00000000
            int string string 0x00000000
            int style Base_Widget 0x00000000
            int styleable autoSizePresetSizes 2
            int transition transition 0x00000000
            int font font 0x00000000
            int xml xml 0x00000000
        """.trimIndent()

        val result = resToRParser.parse(
            emptyList(),
            contextRTxts.lines()
        )

        val expectedValues =
            mapOf(
                Type.ANIM to setOf(RFieldEntry(Type.ANIM, "fade_up", value)),
                Type.ANIMATOR to setOf(RFieldEntry(Type.ANIMATOR, "fade_up_down", value)),
                Type.ARRAY to setOf(RFieldEntry(Type.ARRAY, "array", value)),
                Type.ATTR to setOf(RFieldEntry(Type.ATTR, "helperTextEnabled", value)),
                Type.BOOL to setOf(RFieldEntry(Type.BOOL, "enabled", value)),
                Type.COLOR to setOf(RFieldEntry(Type.COLOR, "primary_text", value)),
                Type.DIMEN to setOf(RFieldEntry(Type.DIMEN, "design_tab", value)),
                Type.DRAWABLE to setOf(RFieldEntry(Type.DRAWABLE, "abc_ic_voice", value)),
                Type.ID to setOf(RFieldEntry(Type.ID, "add", value)),
                Type.INTEGER to setOf(RFieldEntry(Type.INTEGER, "add_value", value)),
                Type.INTERPOLATOR to setOf(RFieldEntry(Type.INTERPOLATOR, "interpolator", value)),
                Type.MENU to setOf(RFieldEntry(Type.MENU, "menu", value)),
                Type.MIPMAP to setOf(RFieldEntry(Type.MIPMAP, "mipmap", value)),
                Type.PLURALS to setOf(RFieldEntry(Type.PLURALS, "plurals", value)),
                Type.RAW to setOf(RFieldEntry(Type.RAW, "raw", value)),
                Type.STRING to setOf(RFieldEntry(Type.STRING, "string", value)),
                Type.LAYOUT to setOf(RFieldEntry(Type.LAYOUT, "design_bottom", value)),
                Type.STYLE to setOf(RFieldEntry(Type.STYLE, "Base_Widget", value)),
                Type.STYLEABLE to setOf(RFieldEntry(Type.STYLEABLE, "autoSizePresetSizes", value)),
                Type.TRANSITION to setOf(RFieldEntry(Type.TRANSITION, "transition", value)),
                Type.FONT to setOf(RFieldEntry(Type.FONT, "font", value)),
                Type.XML to setOf(RFieldEntry(Type.XML, "xml", value))
            )


        assertEquals(expectedValues, result)
    }

    @Test
    fun `assert R txt deps are parsed along with module res`() {
        val value = "0"
        val contextRTxts = """
            int id add 0x00000000
            int layout design_bottom 0x00000000
        """.trimIndent()

        val listTemp = testResFiles(
            TestResFile(
                "activity_main.xml",
                contents = """
                <LinearLayout
                    android:layout_width="match_parent"
                    android:id="@+id/time_display"
                    android:layout_height="match_parent">
                </LinearLayout>
                """.trimIndent(), path = "/src/res/layout/"
            )
        )

        val result = resToRParser.parse(
            listTemp,
            contextRTxts.lines()
        ) as MutableMap<Type, MutableSet<RFieldEntry>>

        val expectedIDs = setOf(
            RFieldEntry(Type.ID, "add", value),
            RFieldEntry(Type.ID, "time_display", value)
        )

        val expectedLayouts = setOf(
            RFieldEntry(Type.LAYOUT, "design_bottom", value),
            RFieldEntry(Type.LAYOUT, "activity_main", value)
        )

        assertEquals(
            mapOf(
                Type.ID to expectedIDs,
                Type.LAYOUT to expectedLayouts
            ), result
        )
    }

    @Test
    fun `assert non transitive R class with package aware R txt files gets parsed correctly`() {
        val resToRParser = DefaultResToRParser(emptyMap(), nonTransitiveRClass = true)
        val contextRTxts = """
            com.grab.bazel.common
            array array_strings
            bool your_colors
            attr payxValue
        """.trimIndent()
        val result = resToRParser.parse(
            emptyList(),
            contextRTxts.lines()
        )
        result.forEach { (_, rTypeEntries) ->
            assertTrue { rTypeEntries.isEmpty() }
        }
    }
}