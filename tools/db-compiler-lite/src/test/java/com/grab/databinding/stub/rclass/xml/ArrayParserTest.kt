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
import com.grab.databinding.stub.rclass.parser.xml.ArrayParser
import com.grab.databinding.stub.util.SingleXmlEntry
import org.junit.Test
import kotlin.test.assertEquals


class ArrayParserTest {

    private val arrayParser: ResourceFileParser = ArrayParser()

    private val value = "0"

    @Test
    fun `parse array`() {
        val result = arrayParser.parse(
            SingleXmlEntry(
                "array_name",
                XmlTypeValues.ARRAY
            )
        )

        val exptectedValue = ParserResult(
            setOf(RFieldEntry(Type.ARRAY, "array_name", value)),
            Type.ARRAY
        )

        assertEquals(result, exptectedValue)
    }

    @Test
    fun `parse integer array`() {
        val result = arrayParser.parse(
            SingleXmlEntry(
                "int_array_name",
                XmlTypeValues.INTEGER_ARRAY
            )
        )

        val exptectedValue = ParserResult(
            setOf(RFieldEntry(Type.ARRAY, "int_array_name", value)),
            Type.ARRAY
        )

        assertEquals(result, exptectedValue)
    }

    @Test
    fun `parse string array`() {
        val result = arrayParser.parse(
            SingleXmlEntry(
                "string_array_name",
                XmlTypeValues.STRING_ARRAY
            )
        )

        val exptectedValue =
            ParserResult(setOf(RFieldEntry(Type.ARRAY, "string_array_name", value)), Type.ARRAY)

        assertEquals(result, exptectedValue)
    }
}