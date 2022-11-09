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
import com.grab.aapt.databinding.rclass.parser.xml.IDParser
import com.grab.aapt.databinding.util.SingleXmlEntry
import org.junit.Test
import kotlin.test.assertEquals


class IDParserTest {

    private val idParser: ResourceFileParser = IDParser()

    private val value = "0"

    @Test
    fun `parse id`() {
        val result = idParser.parse(SingleXmlEntry("item_id", XmlTypeValues.ENUM))

        val exptectedValue = ParserResult(setOf(RFieldEntry(Type.ID, "item_id", value)), Type.ID)

        assertEquals(result, exptectedValue)
    }
}