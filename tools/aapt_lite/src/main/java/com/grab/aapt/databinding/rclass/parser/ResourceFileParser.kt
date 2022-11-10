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

const val DEFAULT_VALUE = "0"

interface ResourceFileParser {
    /**
     * Stub ID that will be overriden by the final stage aapt runs
     */
    val defaultResValue: String
        get() = DEFAULT_VALUE

    fun parse(entry: XmlEntry): ParserResult
}

enum class ParserType {
    STYLE_PARSER,
    ARRAY_PARSER,
    STYLEABLE_PARSER,
    ID_PARSER,
    DEFAULT_PARSER
}

data class ParserResult(
    val rFields: Set<RFieldEntry>,
    val type: Type
)