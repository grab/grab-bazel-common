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

package com.grab.aapt.databinding.brclass

import com.grab.aapt.databinding.binding.model.LayoutBindingData
import com.grab.aapt.databinding.common.BASE_DIR
import com.grab.aapt.databinding.common.Generator
import com.grab.aapt.databinding.common.R_CLASS_OUTPUT_DIR
import com.grab.aapt.databinding.di.AaptScope
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeName.INT
import com.squareup.javapoet.TypeSpec
import dagger.Binds
import dagger.Module
import java.io.File
import javax.inject.Inject
import javax.inject.Named
import javax.lang.model.element.Modifier.PUBLIC
import javax.lang.model.element.Modifier.STATIC

interface BrClassGenerator : Generator {
    fun generate(packageName: String, layoutBindings: List<LayoutBindingData>): File
}

@Module
interface BrClassModule {
    @Binds
    fun provide(impl: DefaultBrClassGenerator): BrClassGenerator
}

@AaptScope
class DefaultBrClassGenerator
@Inject
constructor(
    @Named(BASE_DIR)
    override val baseDir: File
) : BrClassGenerator {

    companion object {
        private val DEFAULT_FIELDS = listOf(
            FieldSpec
                .builder(INT, "_all")
                .addModifiers(PUBLIC, STATIC)
                .initializer("0")
                .build()
        )
    }

    override fun generate(packageName: String, layoutBindings: List<LayoutBindingData>): File {
        val outputDir = File(baseDir, R_CLASS_OUTPUT_DIR)
        val fields = DEFAULT_FIELDS + layoutBindings
            .asSequence()
            .flatMap { it.bindables.asSequence() }
            .map { it.rawName }
            .distinct()
            .filter(String::isNotEmpty)
            .sorted()
            .map { name ->
                FieldSpec
                    .builder(INT, name)
                    .addModifiers(PUBLIC, STATIC)
                    .initializer("0")
                    .build()
            }

        val brType = TypeSpec.classBuilder("BR")
            .addModifiers(PUBLIC)
            .addFields(fields)
            .build()

        JavaFile.builder(packageName, brType)
            .build()
            .writeTo(outputDir)
        logFile(packageName, brType.name)
        return outputDir
    }
}