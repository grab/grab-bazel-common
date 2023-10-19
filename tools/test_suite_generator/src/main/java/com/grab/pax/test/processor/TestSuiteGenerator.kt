/*
 * Copyright 2021 Grabtaxi Holdings Pte Ltd (GRAB)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



package com.grab.pax.test.processor

import com.google.auto.common.BasicAnnotationProcessor
import com.google.auto.service.AutoService
import com.google.common.collect.SetMultimap
import org.junit.Test
import javax.annotation.processing.Processor
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier.PUBLIC


interface TestInfoExporter {
    fun export(testData: Set<Element>)
}

@AutoService(Processor::class)
class TestSuiteGenerator : BasicAnnotationProcessor(),
    BasicAnnotationProcessor.ProcessingStep {

    private val testInfoExporter: TestInfoExporter by lazy { TestSuiteExporter(processingEnv) }

    override fun initSteps() = mutableListOf(this)

    private val supportedAnnotations = mutableSetOf(Test::class.java)

    override fun annotations() = supportedAnnotations

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latest()

    override fun process(elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>): MutableSet<out Element> {
        supportedAnnotations.flatMap(elementsByAnnotation::get)
            .asSequence()
            .filterIsInstance<ExecutableElement>()
            .filter { method -> method.modifiers.contains(PUBLIC) }
            .map { method -> method.enclosingElement }
            .toSet()
            .run { testInfoExporter.export(this) }
        return mutableSetOf()
    }
}


