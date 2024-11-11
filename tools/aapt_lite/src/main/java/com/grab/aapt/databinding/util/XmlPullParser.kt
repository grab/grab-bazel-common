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

import org.xmlpull.v1.XmlPullParser

const val NAME = "name"

/**
 * Iterates over the entire document until end is reached
 */
fun XmlPullParser.events() = object : Iterator<Int> {
    private var eventType = XmlPullParser.START_DOCUMENT
    override fun hasNext(): Boolean = eventType != XmlPullParser.END_DOCUMENT
    override fun next(): Int = this@events.next().also { eventType = it }
}

fun XmlPullParser.attributes() = (0 until attributeCount)

fun <T> XmlPullParser.attributes(
    valueMapper: XmlPullParser.(Int) -> T
) = attributes().map { valueMapper(this, it) }

/**
 * @return Map of XML node's name and values
 */
fun XmlPullParser.attributesNameValue() = attributes { index ->
    val name = getAttributeName(index).trim()
    val value = getAttributeValue(index).trim()
    name to value
}.toMap()

/**
 * Return value by using key "name" or the first value if it is null
 */
fun XmlPullParser.attributeName() = attributesNameValue()[NAME] ?: attributesNameValue().values.firstOrNull()