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

import com.grab.aapt.databinding.rclass.parser.ParentXmlEntry
import com.grab.aapt.databinding.rclass.parser.ParserResult
import com.grab.aapt.databinding.rclass.parser.RFieldEntry
import com.grab.aapt.databinding.rclass.parser.ResourceFileParser
import com.grab.aapt.databinding.rclass.parser.Type
import com.grab.aapt.databinding.rclass.parser.XmlEntry
import javax.inject.Inject

/**
 * DeclareStyleableParser is supposed to parse attribute <declare-styleable>
 * and nested children under nested R class `styleable`
 *
 * Parent attribute must be an array with size of nested children
 * e.g. if children size is 2 => public static final int[] StyleableParentName = { 0x00000000,0x00000000 };
 *
 * Children must include parent name as StyleableParentName_nestedAttributeName
 *
 * What should be covered:
 * - declare-styleables
 * - nested attributes
 */
class DeclareStyleableParser @Inject constructor() : ResourceFileParser {

    override fun parse(entry: XmlEntry): ParserResult {

        require(entry is ParentXmlEntry) { "Only instance of ParentXmlEntry could be used" }

        val rFields = mutableSetOf<RFieldEntry>()

        entry.children.forEach {
            val childStyleName = entry.tagName.replace(".", "_")
            val childName = "${childStyleName}_${it}"
            rFields.add(RFieldEntry(Type.STYLEABLE, childName, defaultResValue))
        }

        // Generate parent value with each subItem
        val styleableValue = rFields
            .joinToString(separator = ",") { defaultResValue }
            .let { "{ $it }" }

        // Add parent styleable
        val stylelableParentName = entry.tagName.replace(".", "_")
        rFields.add(RFieldEntry(Type.STYLEABLE, stylelableParentName, styleableValue, true))

        return ParserResult(rFields, Type.STYLEABLE)
    }
}