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

import com.grab.databinding.stub.rclass.parser.ParserResult
import com.grab.databinding.stub.rclass.parser.RFieldEntry
import com.grab.databinding.stub.rclass.parser.ResourceFileParser
import com.grab.databinding.stub.rclass.parser.Type
import com.grab.databinding.stub.util.XmlEntry
import javax.inject.Inject

/**
 * StyleParser is supposed to parse style and replace "." to "_"
 * E.g.:Theme.App to Theme_App
 *
 * What should be covered:
 * - style
 */
class StyleParser @Inject constructor() : ResourceFileParser {

    override fun parse(entry: XmlEntry): ParserResult {
        val styleName = entry.tagName.replace(".", "_")
        return ParserResult(setOf(RFieldEntry(Type.STYLE, styleName, defaultResValue)), Type.STYLE)
    }
}