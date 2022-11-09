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

package com.grab.aapt.databinding.binding.store

import com.grab.aapt.AaptScope
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonReader.Token
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import dagger.Binds
import dagger.Module
import java.io.File
import javax.inject.Inject
import kotlin.LazyThreadSafetyMode.NONE


typealias BindingClassJsonContents = Map</* Layout Name */String, /* Qualified Name */String>

/**
 * Contract for a class that can parse given databinding binding class file and extract [BindingClassJsonContents]s
 * from it.
 */
interface BindingClassJsonParser {
    /**
     * For a given [bindingClassJsonFile], parse and return [BindingClassJsonContents].
     */
    fun parse(bindingClassJsonFile: File): BindingClassJsonContents
}

@Module
interface BindingClassJsonParserModule {
    @Binds
    fun CachingBindingClassJsonParser.cachingParser(): BindingClassJsonParser
}

@AaptScope
class CachingBindingClassJsonParser
@Inject
constructor() : BindingClassJsonParser {

    private val cache = mutableMapOf</* File path */String, BindingClassJsonContents>()

    data class BindingClasses(
        val mappings: BindingClassJsonContents
    )

    class BindingClassesAdapter {
        @FromJson
        fun fromJson(reader: JsonReader): BindingClasses = reader.run {
            val mappings = mutableMapOf<String, String>()
            readObject {
                when (peek()) {
                    Token.NAME -> when (nextName()) {
                        "mappings" -> readObject {
                            val layoutName = nextName()
                            readObject {
                                if (nextName() == "qualified_name") {
                                    mappings[layoutName] = nextString()
                                } else skipValue()
                            }
                        }
                    }
                    else -> skipValue()
                }
            }
            BindingClasses(mappings)
        }

        @ToJson
        fun toJson(bindingClasses: BindingClasses): String {
            error("Parsing $bindingClasses to json not implemented")
        }
    }

    private val moshi by lazy(NONE) {
        Moshi.Builder()
            .add(BindingClassesAdapter())
            .build()
    }

    private val bindingClassAdapter by lazy(NONE) { moshi.adapter(BindingClasses::class.java) }

    override fun parse(bindingClassJsonFile: File): BindingClassJsonContents {
        return if (cache.containsKey(bindingClassJsonFile.path)) {
            cache.getValue(bindingClassJsonFile.path)
        } else {
            val supportedCharsets = sequenceOf(
                Charsets.UTF_16, // Bazel generated files are UTF-16
                Charsets.UTF_8
            )
            val mappings = supportedCharsets
                .map { charset ->
                    val fileContent = bindingClassJsonFile
                        .readText(charset)
                        .dropWhile { it != '{' }
                        .trim()
                    try {
                        bindingClassAdapter.fromJson(fileContent)!!.mappings
                    } catch (e: Exception) {
                        null
                    }
                }.firstOrNull { it != null }
                ?: throw IllegalArgumentException("Error parsing ${bindingClassJsonFile.path}")
            cache[bindingClassJsonFile.path] = mappings
            mappings
        }
    }
}

@DslMarker
annotation class JsonReaderScope

@JsonReaderScope
inline fun JsonReader.readObject(reader: JsonReader.() -> Unit) {
    beginObject()
    hasNext {
        reader(this)
    }
    endObject()
}

@JsonReaderScope
inline fun JsonReader.hasNext(reader: JsonReader.() -> Unit) {
    while (hasNext()) {
        reader(this)
    }
}
