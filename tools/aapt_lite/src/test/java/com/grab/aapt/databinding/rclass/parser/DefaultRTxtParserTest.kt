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

import com.grab.aapt.databinding.binding.model.Binding
import com.grab.aapt.databinding.binding.model.BindingType
import com.grab.aapt.databinding.binding.model.LayoutBindingData
import com.grab.aapt.databinding.rclass.parser.Type.ANIM
import com.grab.aapt.databinding.rclass.parser.Type.ANIMATOR
import com.grab.aapt.databinding.rclass.parser.Type.ARRAY
import com.grab.aapt.databinding.rclass.parser.Type.ATTR
import com.grab.aapt.databinding.rclass.parser.Type.BOOL
import com.grab.aapt.databinding.rclass.parser.Type.COLOR
import com.grab.aapt.databinding.rclass.parser.Type.DIMEN
import com.grab.aapt.databinding.rclass.parser.Type.DRAWABLE
import com.grab.aapt.databinding.rclass.parser.Type.FONT
import com.grab.aapt.databinding.rclass.parser.Type.ID
import com.grab.aapt.databinding.rclass.parser.Type.INTEGER
import com.grab.aapt.databinding.rclass.parser.Type.INTERPOLATOR
import com.grab.aapt.databinding.rclass.parser.Type.LAYOUT
import com.grab.aapt.databinding.rclass.parser.Type.MENU
import com.grab.aapt.databinding.rclass.parser.Type.MIPMAP
import com.grab.aapt.databinding.rclass.parser.Type.PLURALS
import com.grab.aapt.databinding.rclass.parser.Type.RAW
import com.grab.aapt.databinding.rclass.parser.Type.STRING
import com.grab.aapt.databinding.rclass.parser.Type.STYLE
import com.grab.aapt.databinding.rclass.parser.Type.STYLEABLE
import com.grab.aapt.databinding.rclass.parser.Type.TRANSITION
import com.grab.aapt.databinding.rclass.parser.Type.XML
import com.grab.test.BaseTest
import com.squareup.javapoet.ClassName
import org.junit.Before
import org.junit.Test
import kotlin.test.assertTrue

class DefaultRTxtParserTest : BaseTest() {

    private lateinit var rTxtParser: RTxtParser
    private lateinit var testLayoutBindings: List<LayoutBindingData>

    private val testPackage = "test"

    @Before
    fun setUp() {
        rTxtParser = DefaultRTxtParser()
        testLayoutBindings = listOf(
            LayoutBindingData(
                layoutName = "SimpleBinding",
                bindings = listOf(
                    Binding(
                        rawName = "id_something",
                        typeName = ClassName.get(DefaultRTxtParserTest::class.java),
                        bindingType = BindingType.Variable
                    )
                ),
                bindables = emptyList(),
                file = temporaryFolder.newFile("simple_binding.xml")
            )
        )
    }

    private fun parse(contents: String): RClass {
        return rTxtParser.parse(
            testPackage,
            contents.lines(),
            testLayoutBindings
        )
    }

    private fun RClass.assertEntry(
        type: Type,
        name: String,
        values: String,
        isArray: Boolean = false
    ) {
        assertTrue("Assert entry '$name' of type $type with $values") {
            subclasses.flatMap { rSubClass ->
                rSubClass.entries.filter { it.type == type }
            }.any { it == RFieldEntry(type, name, values, isArray) }
        }
    }

    @Test
    fun `assert simple r class types are parsed`() {
        val values = "0x00000000"
        parse(
            """
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
        ).apply {
            assertEntry(ANIM, "fade_up", values)
            assertEntry(ANIMATOR, "fade_up_down", values)
            assertEntry(ARRAY, "array", values)
            assertEntry(ATTR, "helperTextEnabled", values)
            assertEntry(BOOL, "enabled", values)
            assertEntry(COLOR, "primary_text", values)
            assertEntry(DIMEN, "design_tab", values)
            assertEntry(DRAWABLE, "abc_ic_voice", values)
            assertEntry(ID, "add", values)
            assertEntry(INTEGER, "add_value", values)
            assertEntry(INTERPOLATOR, "interpolator", values)
            assertEntry(MENU, "menu", values)
            assertEntry(MIPMAP, "mipmap", values)
            assertEntry(PLURALS, "plurals", values)
            assertEntry(RAW, "raw", values)
            assertEntry(STRING, "string", values)
            assertEntry(LAYOUT, "design_bottom", values)
            assertEntry(STYLE, "Base_Widget", values)
            assertEntry(STYLEABLE, "autoSizePresetSizes", "2")
            assertEntry(TRANSITION, "transition", values)
            assertEntry(FONT, "font", values)
            assertEntry(XML, "xml", values)
        }
    }

    @Test
    fun `assert stub r class types are parsed`() {
        val values = "0"
        parse("").apply {
            assertEntry(ID, "id_something", values)
            assertEntry(LAYOUT, "simple_binding", values)
        }
    }
}