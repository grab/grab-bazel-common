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

package com.grab.databinding.stub.rclass.parser.xml

import com.grab.databinding.stub.common.XmlEntry
import com.grab.databinding.stub.rclass.parser.ParserResult
import com.grab.databinding.stub.rclass.parser.RFieldEntry
import com.grab.databinding.stub.rclass.parser.ResourceFileParser
import com.grab.databinding.stub.rclass.parser.Type
import javax.inject.Inject

/**
 * ArrayParser is supposed to parse all available array under nested R class `array`
 * What should be covered:
 * - array
 * - string-array
 * - integer-array
 */
class ArrayParser @Inject constructor() : ResourceFileParser {

    override fun parse(entry: XmlEntry): ParserResult {
        return ParserResult(
            setOf(RFieldEntry(Type.ARRAY, entry.tagName, defaultResValue, false)),
            Type.ARRAY
        )
    }
}