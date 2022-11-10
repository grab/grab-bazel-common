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

package com.grab.aapt.databinding.rclass.parser.xml

import com.grab.aapt.databinding.rclass.parser.ParserResult
import com.grab.aapt.databinding.rclass.parser.RFieldEntry
import com.grab.aapt.databinding.rclass.parser.ResourceFileParser
import com.grab.aapt.databinding.rclass.parser.Type
import com.grab.aapt.databinding.rclass.parser.XmlEntry
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