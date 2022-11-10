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

import com.grab.aapt.databinding.di.AaptScope
import com.grab.aapt.databinding.binding.model.LayoutBindingData
import javax.inject.Inject

data class RFieldEntry(
    val type: Type,
    val name: String,
    val value: String,
    val isArray: Boolean = false
)

/**
 * Reference https://developer.android.com/reference/android/R
 *
 * Enum to collect all R types in R.java
 */
enum class Type(val entry: String) {
    ANIM("anim"),
    ANIMATOR("animator"),
    ARRAY("array"),
    ATTR("attr"),
    BOOL("bool"),
    COLOR("color"),
    DIMEN("dimen"),
    DRAWABLE("drawable"),
    FRACTION("fraction"),
    ID("id"),
    INTEGER("integer"),
    INTERPOLATOR("interpolator"),
    LAYOUT("layout"),
    MENU("menu"),
    MIPMAP("mipmap"),
    NAVIGATION("navigation"),
    PLURALS("plurals"),
    RAW("raw"),
    STRING("string"),
    STYLE("style"),
    STYLEABLE("styleable"),
    TRANSITION("transition"),
    FONT("font"),
    XML("xml")
}

/**
 * Enum to collect all types under scr/res/values
 */
enum class XmlTypeValues(val entry: String) {
    ARRAY("array"),
    ATTR("attr"),
    BOOL("bool"),
    COLOR("color"),
    DECLARE_STYLEABLE("declare-styleable"),
    DIMEN("dimen"),
    DRAWABLE("drawable"),
    ENUM("enum"),
    FRACTION("fraction"),
    ID("id"),
    INTEGER("integer"),
    INTEGER_ARRAY("integer-array"),
    ITEM("item"),
    PLURALS("plurals"),
    STRING("string"),
    STRING_ARRAY("string-array"),
    STYLE("style")
}

fun enumTypeValue(name: String): XmlTypeValues {
    return enumValues<XmlTypeValues>().first { it.entry == name }
}

data class RSubClass(
    val type: Type,
    val entries: List<RFieldEntry>
)

data class RClass(
    val packageName: String,
    val subclasses: List<RSubClass>
)

//TODO: remove if not used
interface RTxtParser {
    fun parse(
        packageName: String,
        content: List<String>,
        layoutBindings: List<LayoutBindingData>
    ): RClass
}

@AaptScope
class DefaultRTxtParser
@Inject
constructor() : RTxtParser {

    companion object {
        private const val VALUE_INDEX = 3
        private const val NAME_INDEX = 2
        private const val TYPE_INDEX = 1
    }

    private val types = enumValues<Type>()

    private fun parseStatement(statement: String): RFieldEntry {
        val tokens = statement.split(" ").map(String::trim)
        val isArray = statement.contains("[]")
        val name = tokens[NAME_INDEX]

        val value = if (!isArray) {
            tokens[VALUE_INDEX]
        } else {
            // Parse format like
            // int[] styleable View { 0x00000000, 0x00000000, 0x00000000, 0x01010000, 0x010100da }
            tokens.subList(VALUE_INDEX + 1, tokens.size - 1)
                .joinToString(
                    separator = ", ",
                    prefix = "{ ",
                    postfix = " }"
                ) { value -> value.trim { it == ',' } }
        }
        return RFieldEntry(
            type = types.first { it.entry == tokens[TYPE_INDEX] },
            name = name,
            value = value,
            isArray = isArray
        )
    }

    private fun parseLayoutNames(layoutBindings: List<LayoutBindingData>) =
        layoutBindings.asSequence()
            .map { it.file.path }
            .map {
                it.split("/").last()  // Take file name
                    .split(".").first() // Remove extension
            }.map { name ->
                RFieldEntry(
                    type = Type.LAYOUT,
                    name = name,
                    value = "0",
                    isArray = false
                )
            }.toList()

    private fun parseIdNames(layoutBindings: List<LayoutBindingData>) = layoutBindings.asSequence()
        .flatMap { it.bindings.asSequence() }
        .map { it.rawName }
        .distinct()
        .map { name ->
            RFieldEntry(
                type = Type.ID,
                name = name,
                value = "0",
                isArray = false
            )
        }.toList()

    override fun parse(
        packageName: String,
        content: List<String>,
        layoutBindings: List<LayoutBindingData>
    ): RClass {
        val fieldEntries = content.filter { it.isNotEmpty() }.map(this::parseStatement) +
                parseLayoutNames(layoutBindings) +
                parseIdNames(layoutBindings)
        val subclasses = fieldEntries
            .distinctBy { it.type.entry + it.name }
            .groupBy(RFieldEntry::type)
            .map { (type, entries) -> RSubClass(type, entries) }
        return RClass(packageName, subclasses)
    }
}
