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

package com.grab.databinding.stub.rclass.parser

import com.grab.databinding.stub.common.*
import com.grab.databinding.stub.util.*
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import javax.inject.Singleton

/**
 * Parse resource xml, files and dependencies R.txt into types representing R class.
 */
interface ResToRParser {
    fun parse(
        resources: List<File>,
        dependenciesRTxtContent: List<String>
    ): Map<Type, MutableSet<RFieldEntry>>
}

// Allow having id like "android:id="@id/my_id" and "android:id="@+id/my_id"
private const val ID_DEFINITION_PREFIX = "id/"
private const val ANDROID_ID = "android:id"
private const val TAG_TYPE = "type"

@Singleton
class ResToRParserImpl constructor(
    private val parsers: Map<ParserType, @JvmSuppressWildcards ResourceFileParser>
) : ResToRParser {

    companion object {
        private const val VALUE_INDEX = 3
        private const val NAME_INDEX = 2
        private const val TYPE_INDEX = 1
    }

    private val resources: MutableMap<Type, MutableSet<RFieldEntry>> = mutableMapOf()
    private val xpp = XmlPullParserFactory.newInstance().newPullParser()

    override fun parse(
        resources: List<File>,
        dependenciesRTxtContent: List<String>
    ): Map<Type, MutableSet<RFieldEntry>> {
        resources.forEach { resource ->
            collectRes(resource)
        }
        dependenciesRTxtContent.forEachIndexed { index, entry ->
            parseContent(index, entry)
        }
        return this.resources
    }

    private fun parseContent(index: Int, content: String) {
        val tokens = content.split(" ").map(String::trim)

        if (tokens.size == 1) {
            // Package aware R.txt can contain package names, exclude that from parsing
            return
        }

        val isArray = content.contains("[]")
        val name = tokens[NAME_INDEX]

        val id = if (!isArray) {
            DEFAULT_VALUE
        } else {
            // Parse format like
            // int[] styleable View { 0x00000000, 0x00000000, 0x00000000, 0x01010000, 0x010100da }
            tokens.subList(VALUE_INDEX + 1, tokens.size - 1).joinToString(
                separator = ", ",
                prefix = "{ ",
                postfix = " }"
            ) { DEFAULT_VALUE }
        }

        val type = Type.valueOf(tokens[TYPE_INDEX].toUpperCase())

        addIntResource(type, name, id, isArray)
    }

    private fun collectRes(file: File) {
        val dirname = file.parentFile.name

        if (dirname.startsWith("values".substringBefore("-"))) { // for lang values like "values-in"
            processValues(file)
        } else {
            collectIDs(file)
            processFileNamesInDirectory(file, dirname)
        }
    }

    private fun processValues(file: File) {
        xpp.setInput(file.inputStream(), null)

        xpp.events()
            .asSequence()
            .filter { xpp.attributesNameValue().isNotEmpty() }
            .filter { xpp.attributesNameValue().contains(NAME) }
            .forEach { _ ->
                val name = xpp.attributeName()

                val type = enumTypeValue(xpp.name).let {
                    getTypedItem(it, xpp.attributesNameValue()) ?: it
                }.apply {
                    // If item is not typed, we do not include it
                    if (this == XmlTypeValues.ITEM) return@forEach
                }

                parseIntoRField(type, name, xpp)
            }
    }

    private fun processFileNamesInDirectory(
        file: File,
        dirname: String
    ) { //dirname = "drawable" or "layout"
        // Check for drawable with type 9path
        val filename = file.nameWithoutExtension.substringBefore(".9")

        val type = Type.valueOf(
            dirname.toUpperCase().substringBefore("-")
        ) // for lang or resolution values like "drawables-hdpi"

        addIntResource(type, filename)
    }

    private fun collectIDs(file: File) {
        if (file.extension != "xml") return //process only xml files to get IDs

        xpp.setInput(file.inputStream(), null)
        xpp.events()
            .asSequence()
            .filter { xpp.attributesNameValue().contains(ANDROID_ID) }
            .map { xpp.attributesNameValue().get(ANDROID_ID) }
            .filterNotNull()
            .filter { !it.contains("@android:") }
            .map { it.substringAfter(ID_DEFINITION_PREFIX) }
            //do not include "@android:id/mask"
            .forEach { addIntResource(Type.ID, it) }
    }

    private fun addIntResource(
        type: Type,
        name: String,
        value: String = DEFAULT_VALUE,
        isArray: Boolean = false
    ) {
        resources.getOrPut(type) { mutableSetOf() }.apply {
            this.add(
                RFieldEntry(
                    type,
                    name,
                    value,
                    isArray
                )
            ) // ID will be overriden by the second stage aapt runs
            resources.put(type, this)
        }
    }

    private fun addResources(result: ParserResult) {
        (resources[result.type] ?: mutableSetOf()).apply {
            this.addAll(result.rFields)
            resources.put(result.type, this)
        }
    }

    private fun getTypedItem(type: XmlTypeValues, map: Map<String, String>): XmlTypeValues? {
        return if (type == XmlTypeValues.ITEM && map.containsKey(TAG_TYPE)) {
            map.get(TAG_TYPE)?.let { enumTypeValue(it) }
        } else null
    }

    private fun parseIntoRField(type: XmlTypeValues, name: String, xpp: XmlPullParser) {
        when (type) {
            XmlTypeValues.STYLE -> parse(ParserType.STYLE_PARSER, name, type)
            XmlTypeValues.ARRAY, XmlTypeValues.STRING_ARRAY, XmlTypeValues.INTEGER_ARRAY -> parse(
                ParserType.ARRAY_PARSER,
                name,
                type
            )
            XmlTypeValues.ENUM -> parse(ParserType.ID_PARSER, name, type)
            XmlTypeValues.DECLARE_STYLEABLE -> parseParentEntry(
                ParserType.STYLEABLE_PARSER,
                name,
                xpp
            )
            else -> parse(ParserType.DEFAULT_PARSER, name, type)
        }.apply {
            addResources(this)
        }
    }

    private fun parse(type: ParserType, name: String, typeValue: XmlTypeValues): ParserResult {
        return parsers[type]?.parse(SingleXmlEntry(name, typeValue))
            ?: throw NullPointerException("Missing implementation. Parser must not be null.")
    }

    private fun parseParentEntry(type: ParserType, name: String, xpp: XmlPullParser): ParserResult {
        val children = mutableListOf<String>()

        xpp.next() // Proceed to the next (child) node once we save parent

        while (xpp.name != XmlTypeValues.DECLARE_STYLEABLE.entry) {

            // Get next item
            xpp.next()

            // Prevent having null as a tag or "" as empty content
            if (xpp.name == null || xpp.attributesNameValue().values.isEmpty()) {
                continue
            }

            val childName = xpp.attributeName().replace(":", "_")
            // Add every child as a dependent for Parent node
            children.add(childName)

            // Add every child as a independent attribute as well
            parseIntoRField(XmlTypeValues.ATTR, childName, xpp)
        }

        return parsers.get(type)
            ?.parse(ParentXmlEntryImpl(name, XmlTypeValues.DECLARE_STYLEABLE, children))
            ?: throw NullPointerException("Missing implementation. Parser must not be null.")
    }
}
