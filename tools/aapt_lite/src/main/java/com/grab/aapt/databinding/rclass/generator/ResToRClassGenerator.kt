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

package com.grab.aapt.databinding.rclass.generator

import com.grab.aapt.databinding.common.BASE_DIR
import com.grab.aapt.databinding.common.Generator
import com.grab.aapt.databinding.common.R_CLASS_OUTPUT_DIR
import com.grab.aapt.databinding.di.AaptScope
import com.grab.aapt.databinding.rclass.parser.ResToRParser
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeName.INT
import com.squareup.javapoet.TypeSpec
import java.io.File
import javax.inject.Inject
import javax.inject.Named
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PUBLIC
import javax.lang.model.element.Modifier.STATIC

/**
 * [ResToRClassGenerator] takes list of resource files and dependencies R.txt directory and generates
 * R.java file containing merged data from resource files and R.txts from dependencies
 */
interface ResToRClassGenerator : Generator {
    /**
     * For given resources and R.txt metadata dir generates R.java by merging all the entries.
     *
     * @param packageName The package name of the R class
     * @param resources The list of resources for which R class content should be extracted from
     * @param rTxts The list of R.txt files from dependencies
     * @return The directory where the classes were written to
     */
    fun generate(packageName: String, resources: List<File>, rTxts: List<File>): File
}

@AaptScope
class DefaultResToRClassGenerator
@Inject
constructor(
    @Named(BASE_DIR)
    override val baseDir: File,
    private val resToRParser: ResToRParser,
) : ResToRClassGenerator {

    override fun generate(packageName: String, resources: List<File>, rTxts: List<File>): File {
        val outputDir = File(baseDir, R_CLASS_OUTPUT_DIR)
        val resourcesStore = resToRParser.parse(resources, rTxts.flatMap(File::readLines))

        val subclasses = mutableListOf<TypeSpec>()
        resourcesStore.keys.forEach { rClassType ->
            val fields = mutableListOf<FieldSpec>()

            resourcesStore[rClassType]
                ?.toSortedSet { a, b -> a.name.compareTo(b.name) }
                ?.forEach {
                    val type = when {
                        it.isArray -> ArrayTypeName.of(INT)
                        else -> INT
                    }
                    val modifiers = mutableListOf(PUBLIC, STATIC)
                        .apply { if (it.isArray) add(FINAL) }
                        .toTypedArray()

                    fields.add(
                        FieldSpec.builder(type, it.name)
                            .addModifiers(*modifiers)
                            .initializer(it.value)
                            .build()
                    )
                }

            subclasses.add(
                TypeSpec.classBuilder(rClassType.entry)
                    .addFields(fields)
                    .addModifiers(PUBLIC, STATIC, FINAL)
                    .build()
            )
        }

        TypeSpec.classBuilder("R")
            .addTypes(subclasses)
            .addModifiers(FINAL, PUBLIC)
            .build()
            .let { type ->
                JavaFile.builder(packageName, type)
                    .build()
                    .writeTo(outputDir)
                logFile(packageName, type.name)
            }
        return outputDir
    }
}
