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



package com.grab.pax.binding.processor

import androidx.databinding.BindingAdapter
import androidx.databinding.BindingConversion
import androidx.databinding.InverseBindingAdapter
import com.google.auto.common.BasicAnnotationProcessor
import com.google.auto.service.AutoService
import com.google.common.collect.SetMultimap
import com.squareup.javapoet.*
import javax.annotation.processing.Processor
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier.PUBLIC
import javax.lang.model.element.Modifier.STATIC
import javax.lang.model.element.VariableElement


@AutoService(Processor::class)
class BindingAdapterProcessor : BasicAnnotationProcessor(),
    BasicAnnotationProcessor.ProcessingStep {

    override fun initSteps() = mutableListOf(this)

    private val supportedAnnotations = mutableSetOf(
        BindingAdapter::class.java,
        InverseBindingAdapter::class.java,
        BindingConversion::class.java
    )

    override fun annotations() = supportedAnnotations

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latest()

    override fun process(elementsByAnnotation: SetMultimap<Class<out Annotation>, Element>): MutableSet<out Element> {
        supportedAnnotations.flatMap(elementsByAnnotation::get)
            .asSequence()
            .filterIsInstance<ExecutableElement>()
            .filter { method ->
                method.modifiers.contains(PUBLIC) && method.modifiers.contains(STATIC)
            }.groupBy { method -> processingEnv.elementUtils.getPackageOf(method).toString() }
            .mapValues { (_, methods) ->
                methods.mapNotNull { method ->
                    val params = method.parameters
                    val methodName = method.simpleName.toString()
                    val annotations = method.annotationMirrors
                    val parentClass = method.enclosingElement
                    val typeParams = method.typeParameters
                    val returns = TypeName.get(method.returnType)

                    if (!parentClass.simpleName.toString().endsWith(GeneratedSuffix)) {
                        MethodSpec.methodBuilder(methodName)
                            .addModifiers(PUBLIC, STATIC)
                            .returns(returns)
                            .addAnnotations(annotations.map { annotationMirror ->
                                AnnotationSpec.get(annotationMirror)
                            })
                            .addParameters(params.map { variableElement ->
                                ParameterSpec.get(variableElement)
                            })
                            .addStatement(
                                buildStatement(methodName, params, returns),
                                buildParentClassName(parentClass)
                            ).addTypeVariables(typeParams.map { TypeVariableName.get(it) })
                            .build()
                    } else null
                }
            }.filterValues { it.isNotEmpty() }
            .forEach { (packageName, generatedMethodSpecs) ->
                val classType = TypeSpec
                    .classBuilder(buildClassName(packageName))
                    .addModifiers(PUBLIC)
                    .addMethods(generatedMethodSpecs)
                    .build()
                JavaFile.builder(packageName, classType)
                    .build()
                    .writeTo(processingEnv.filer)
            }
        return mutableSetOf()
    }

    private fun buildClassName(packageName: String): String {
        return packageName.replace(".", "_") + GeneratedSuffix
    }

    private fun buildParentClassName(parentClass: Element): ClassName {
        val packageElement = processingEnv.elementUtils.getPackageOf(parentClass)
        return ClassName.get(
            packageElement.qualifiedName.toString(),
            parentClass.simpleName.toString() // Strip type params since we only access the class statically
        )
    }

    private fun buildStatement(
        methodName: String,
        params: List<VariableElement>,
        returns: TypeName
    ) = buildString {
        if (returns != TypeName.VOID) {
            append("return ")
        }
        append("\$T.") // Class
        append("$methodName(") // Method
        append(params  // Parameters
            .joinToString(separator = ", ") { it.simpleName.toString() }
            .replace(
                "$",
                "$$"
            ) // Cleanup for Kotlin extension functions as they will contain $this$
        )
        append(")")
    }

    companion object {
        private const val GeneratedSuffix = "_Binding_Adapter_Stub"
    }
}