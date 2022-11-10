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

import com.grab.aapt.databinding.binding.model.LayoutBindingData
import com.grab.aapt.databinding.common.BASE_DIR
import com.grab.aapt.databinding.common.Generator
import com.grab.aapt.databinding.common.R_CLASS_OUTPUT_DIR
import com.grab.aapt.databinding.di.AaptScope
import com.grab.aapt.databinding.rclass.parser.DefaultRTxtParser
import com.grab.aapt.databinding.rclass.parser.RFieldEntry
import com.grab.aapt.databinding.rclass.parser.RTxtParser
import com.squareup.javapoet.ArrayTypeName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeName.INT
import com.squareup.javapoet.TypeSpec
import dagger.Binds
import dagger.Module
import java.io.File
import javax.inject.Inject
import javax.inject.Named
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PUBLIC
import javax.lang.model.element.Modifier.STATIC

//TODO: remove if not used
interface RClassGenerator : Generator {
    fun generate(
        packageName: String,
        content: List<String>,
        layoutBindings: List<LayoutBindingData>
    )
}

@Module
interface RClassModule {
    @Binds
    fun DefaultRClassGenerator.rClassGenerator(): RClassGenerator

    @Binds
    fun DefaultRTxtParser.rTxtParser(): RTxtParser
}

@AaptScope
class DefaultRClassGenerator
@Inject
constructor(
    @Named(BASE_DIR)
    override val baseDir: File,
    private val rTxtParser: RTxtParser,
) : RClassGenerator {

    private fun RFieldEntry.toFieldSpec(): FieldSpec? {
        val typeName = when {
            isArray -> ArrayTypeName.of(INT)
            else -> INT
        }

        val modifiers = mutableListOf(PUBLIC, STATIC).apply { if (isArray) add(FINAL) }
            .toTypedArray()

        return FieldSpec
            .builder(typeName, name)
            .addModifiers(*modifiers)
            .initializer(value)
            .build()
    }

    override fun generate(
        packageName: String,
        content: List<String>,
        layoutBindings: List<LayoutBindingData>
    ) {
        val rClass = rTxtParser.parse(packageName, content, layoutBindings)
        val subclassTypeSpecs = rClass.subclasses
            .map { subclass ->
                TypeSpec.classBuilder(subclass.type.entry)
                    .addFields(subclass.entries.map { it.toFieldSpec() })
                    .addModifiers(PUBLIC, STATIC, FINAL)
                    .build()
            }

        TypeSpec.classBuilder("R")
            .addTypes(subclassTypeSpecs)
            .addModifiers(FINAL, PUBLIC)
            .build()
            .let { type ->
                JavaFile.builder(rClass.packageName, type)
                    .build()
                    .writeTo(File(baseDir, R_CLASS_OUTPUT_DIR))
                logFile(rClass.packageName, type.name)
            }
    }
}