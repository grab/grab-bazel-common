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

package com.grab.databinding.stub.rclass.xml

import com.grab.databinding.stub.rclass.parser.*
import com.grab.databinding.stub.rclass.parser.xml.DefaultXmlParser
import com.grab.databinding.stub.util.SingleXmlEntry
import org.junit.Test
import kotlin.test.assertEquals


class DefaultXmlParserTest {

    private val defaultParser: ResourceFileParser = DefaultXmlParser()

    private val value = "0"

    @Test
    fun `parse string`() {
        val result = defaultParser.parse(SingleXmlEntry("app_name", XmlTypeValues.STRING))

        val exptectedValue = ParserResult(
            setOf(RFieldEntry(Type.STRING, "app_name", value)),
            Type.STRING
        )

        assertEquals(result, exptectedValue)
    }

    @Test
    fun `parse color`() {
        val result = defaultParser.parse(SingleXmlEntry("app_name", XmlTypeValues.COLOR))

        val exptectedValue = ParserResult(
            setOf(RFieldEntry(Type.COLOR, "app_name", value)),
            Type.COLOR
        )

        assertEquals(result, exptectedValue)
    }

    @Test
    fun `parse attribute`() {
        val result = defaultParser.parse(SingleXmlEntry("height", XmlTypeValues.ATTR))

        val exptectedValue = ParserResult(
            setOf(RFieldEntry(Type.ATTR, "height", value)),
            Type.ATTR
        )

        assertEquals(result, exptectedValue)
    }

    @Test
    fun `parse boolean`() {
        val result = defaultParser.parse(SingleXmlEntry("isTrue", XmlTypeValues.BOOL))

        val exptectedValue = ParserResult(
            setOf(RFieldEntry(Type.BOOL, "isTrue", value)),
            Type.BOOL
        )

        assertEquals(result, exptectedValue)
    }

    @Test
    fun `parse dimen`() {
        val result = defaultParser.parse(SingleXmlEntry("size_dp", XmlTypeValues.DIMEN))

        val exptectedValue = ParserResult(
            setOf(RFieldEntry(Type.DIMEN, "size_dp", value)),
            Type.DIMEN
        )

        assertEquals(result, exptectedValue)
    }

    @Test
    fun `parse integer`() {
        val result = defaultParser.parse(SingleXmlEntry("int_value", XmlTypeValues.INTEGER))

        val exptectedValue = ParserResult(
            setOf(RFieldEntry(Type.INTEGER, "int_value", value)),
            Type.INTEGER
        )

        assertEquals(result, exptectedValue)
    }
}