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

package com.grab.aapt.databinding.util

import com.grab.aapt.databinding.rclass.parser.XmlTypeValues

interface XmlEntry {
    /**
     * Get only first item for name based on current xpp step
     */
    val tagName: String

    /**
     * Get a type based on current xpp step
     */
    val type: XmlTypeValues
}

interface ParentXmlEntry : XmlEntry {
    /**
     * Get children nodes from a parent one
     */
    val children: List<String>
}

class SingleXmlEntry(
    override val tagName: String,
    override val type: XmlTypeValues
) : XmlEntry

class ParentXmlEntryImpl(
    override val tagName: String,
    override val type: XmlTypeValues,
    override val children: List<String>
) : ParentXmlEntry