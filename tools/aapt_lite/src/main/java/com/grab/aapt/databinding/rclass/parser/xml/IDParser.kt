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
 * IDParser is supposed to parse values under nexted R class `id`
 *
 * What should be covered:
 * - enum
 */
class IDParser @Inject constructor() : ResourceFileParser {

    override fun parse(entry: XmlEntry): ParserResult {
        return ParserResult(setOf(RFieldEntry(Type.ID, entry.tagName, defaultResValue)), Type.ID)
    }
}