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

import com.grab.aapt.databinding.common.BaseBindingStubTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class ResToRFileIDParserTest : BaseBindingStubTest() {

    private lateinit var resToRParser: ResToRParser
    private val value = "0"

    @Before
    fun setUp() {
        resToRParser = DefaultResToRParser(emptyMap(), false)
    }

    @Test
    fun `assert image file names are parsed correctly`() {
        val listTemp = testResFiles(
            TestResFile("imagePNG.png", "", "/src/res/drawable/"),
            TestResFile("imageVEC.svg", "", "/src/res/drawable/"),
            TestResFile("animation.gif", "", "/src/res/anim/")
        )

        val result = resToRParser.parse(
            listTemp,
            emptyList()
        ) as MutableMap<Type, MutableSet<RFieldEntry>>

        val expectedDrawable = setOf(
            RFieldEntry(Type.DRAWABLE, "imagePNG", value),
            RFieldEntry(Type.DRAWABLE, "imageVEC", value)
        )

        val expectedAnim = setOf(RFieldEntry(Type.ANIM, "animation", value))

        assertEquals(
            mapOf(
                Type.DRAWABLE to expectedDrawable,
                Type.ANIM to expectedAnim
            ), result
        )
    }

    @Test
    fun `assert file names are parsed correctly`() {
        val listTemp = testResFiles(
            TestResFile(
                "activity_main.xml",
                contents = """
                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="match_parent">
                    </LinearLayout>
                    """.trimIndent(), path = "/src/res/layout/"
            ),

            TestResFile(
                "game_menu.xml",
                contents = """
                <?xml version="1.0" encoding="utf-8"?>
                <menu xmlns:android="http://schemas.android.com/apk/res/android">
                        <item android:id="@android:id/mask"/>
                </menu>
                """.trimIndent(), path = "/src/res/menu/"
            )
        )

        val result = resToRParser.parse(
            listTemp,
            emptyList()
        ) as MutableMap<Type, MutableSet<RFieldEntry>>

        val expectedLayout = setOf(RFieldEntry(Type.LAYOUT, "activity_main", value))

        val expectedMenu = setOf(RFieldEntry(Type.MENU, "game_menu", value))

        assertEquals(
            mapOf(
                Type.LAYOUT to expectedLayout,
                Type.MENU to expectedMenu
            ), result
        )
    }

    @Test
    fun `assert id name are parsed correctly along with file names`() {
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
            ),

            TestResFile(
                "game_menu.xml",
                contents = """
                <?xml version="1.0" encoding="utf-8"?>
                <menu xmlns:android="http://schemas.android.com/apk/res/android">
                    <item android:id="@+id/new_game"
                    android:title="@string/new_game"/>
                    <item android:id="@+id/help"
                    android:title="@string/help"/>
                </menu>
                """.trimIndent(), path = "/src/res/menu/"
            )
        )

        val result = resToRParser
            .parse(listTemp, emptyList()) as MutableMap<Type, MutableSet<RFieldEntry>>

        val expectedLayout = setOf(RFieldEntry(Type.LAYOUT, "activity_main", value))

        val expectedIDs = setOf(
            RFieldEntry(Type.ID, "time_display", value),
            RFieldEntry(Type.ID, "new_game", value),
            RFieldEntry(Type.ID, "help", value)
        )

        val expectedMenu = setOf(RFieldEntry(Type.MENU, "game_menu", value))

        assertEquals(
            mapOf(
                Type.LAYOUT to expectedLayout,
                Type.ID to expectedIDs,
                Type.MENU to expectedMenu
            ), result
        )
    }
}