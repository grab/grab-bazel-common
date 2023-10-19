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

package com.grab.pax.test.processor

import com.squareup.javapoet.AnnotationSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import org.junit.runner.RunWith
import org.junit.runners.Suite
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.Modifier


private const val DEFAULT_TEST_SUITE_CLASS = "TestSuite"
private const val DEFAULT_TEST_SUITE_PACKAGE = "com.grazel.generated"

class TestSuiteExporter(private val processingEnv: ProcessingEnvironment) : TestInfoExporter {
    override fun export(testData: Set<Element>) {
        val runWithAnnotationSpec = AnnotationSpec.builder(RunWith::class.java)
            .addMember("value", "\$T.class", Suite::class.java)
            .build()
        val suiteAnnotationSpec = AnnotationSpec.builder(Suite.SuiteClasses::class.java)
            .also {
                testData.forEach { clazz -> it.addMember("value", "\$T.class", clazz.asType()) }
            }.build()

        val classType = TypeSpec
            .classBuilder(DEFAULT_TEST_SUITE_CLASS)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(runWithAnnotationSpec)
            .addAnnotation(suiteAnnotationSpec)
            .build()
        JavaFile.builder(DEFAULT_TEST_SUITE_PACKAGE, classType)
            .build()
            .writeTo(processingEnv.filer)
    }
}