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

package com.grab.aapt.databinding.rclass.xml

import com.grab.aapt.databinding.rclass.parser.*
import com.grab.aapt.databinding.rclass.parser.xml.StyleParser
import com.grab.aapt.databinding.rclass.parser.SingleXmlEntry
import org.junit.Test
import kotlin.test.assertEquals


class StyleParserTest {

    private val styleParser: ResourceFileParser = StyleParser()

    private val value = "0"

    @Test
    fun `parse style`() {
        val result =
            styleParser.parse(SingleXmlEntry("Theme.MaterialComponent", XmlTypeValues.STYLE))

        val exptectedValue = ParserResult(
            setOf(RFieldEntry(Type.STYLE, "Theme_MaterialComponent", value)),
            Type.STYLE
        )

        assertEquals(result, exptectedValue)
    }
}