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

package com.grab.aapt.databinding.binding.generator

import com.grab.aapt.databinding.binding.model.Binding
import com.grab.aapt.databinding.binding.model.BindingType
import com.grab.aapt.databinding.binding.model.LayoutBindingData
import com.grab.aapt.databinding.common.BASE_DIR
import com.grab.aapt.databinding.common.DATABINDING_PACKAGE
import com.grab.aapt.databinding.common.Generator
import com.grab.aapt.databinding.di.AaptScope
import com.grab.aapt.databinding.util.capitalize
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.MethodSpec
import com.squareup.javapoet.ParameterSpec
import com.squareup.javapoet.TypeName.BOOLEAN
import com.squareup.javapoet.TypeName.INT
import com.squareup.javapoet.TypeSpec
import java.io.File
import javax.inject.Inject
import javax.inject.Named
import javax.lang.model.element.Modifier.ABSTRACT
import javax.lang.model.element.Modifier.FINAL
import javax.lang.model.element.Modifier.PROTECTED
import javax.lang.model.element.Modifier.PUBLIC
import javax.lang.model.element.Modifier.STATIC

interface BindingClassGenerator : Generator {
    fun generate(
        packageName: String,
        layoutBindings: List<LayoutBindingData>
    ): File
}

@AaptScope
class DefaultBindingClassGenerator
@Inject
constructor(
    @Named(BASE_DIR)
    override val baseDir: File
) : BindingClassGenerator {
    companion object {
        private const val INFLATE_METHOD_NAME = "inflate"
        private const val BIND_METHOD_NAME = "bind"
        private const val INFLATER = "inflater"
        private const val ROOT = "root"
        private const val VIEW = "view"
        private const val ATTACH_TO_ROOT = "attachToRoot"
        private const val COMPONENT = "component"
        private const val RUNTIME_EXCEPTION = """throw new RuntimeException("Stub!")"""
        private const val VIEW_PACKAGE = "android.view"
    }

    private val objectClass = ClassName.get("java.lang", "Object")
    private val viewClass = ClassName.get(VIEW_PACKAGE, "View")
    private val viewGroupClass = ClassName.get(VIEW_PACKAGE, "ViewGroup")
    private val layoutInflaterClass = ClassName.get(VIEW_PACKAGE, "LayoutInflater")
    private val androidNonNull = ClassName.get("androidx.annotation", "NonNull")
    private val androidNullable = ClassName.get("androidx.annotation", "Nullable")
    private val bindableAnnotation = ClassName.get(DATABINDING_PACKAGE, "Bindable")
    private val viewDataBinding = ClassName.get(DATABINDING_PACKAGE, "ViewDataBinding")
    private val emptyBinding = LayoutBindingData(
        layoutName = "",
        file = File(""),
        bindables = emptyList(),
        bindings = emptyList()
    )

    /**
     * Binding types that are invalid and should not be considered during code generation.
     */
    private val invalidBindingTypes = listOf(
        "android.widget.fragment",
        "android.widget.layout"
    )

    /**
     * Calculate valid [LayoutBindingData]'s that needs a binding stub to be generated
     *
     * Criteria
     * * Layouts with `<layout>` tags only
     * * Layouts with <include> but their layout type could not be inferred from either local or dep context.
     * @param layoutBindings The original parsed layout data
     */
    fun calculateBindingsToGenerate(layoutBindings: List<LayoutBindingData>): Sequence<LayoutBindingData> {
        // If there are any binding of included type with their layout missing in both local and deps,
        // generate an empty binding to let the build pass instead of failing eagerly.
        val additionalLayoutBindings = layoutBindings
            .asSequence()
            .flatMap { it.bindings.asSequence() }
            .map(Binding::bindingType)
            .filterIsInstance<BindingType.IncludedLayout>()
            .filter(BindingType.IncludedLayout::layoutMissing) // Consider only bindings with missing layout
            .map(BindingType.IncludedLayout::layoutName)
            .map { layoutName -> emptyBinding.copy(layoutName = layoutName) } // Map them to empty bindings

        return (layoutBindings
            .asSequence()
            .filter { layoutBinding ->
                layoutBinding
                    .file
                    .useLines { lines -> lines.any { it.contains("<layout") } }
            } + additionalLayoutBindings).distinct()
    }

    override fun generate(packageName: String, layoutBindings: List<LayoutBindingData>): File {
        val outputDir = File(baseDir, DATABINDING_PACKAGE)
        // By default we generate androidx.databinding.DataBindingComponent
        generateDataBindingComponentInterface(outputDir)
        calculateBindingsToGenerate(layoutBindings)
            .forEach { layoutBinding ->
                val bindingClass = layoutBinding.layoutName
                val genPackageName = "$packageName.databinding"
                val bindingClassName = ClassName.get(genPackageName, bindingClass)

                val bindings = layoutBinding.bindings.filterNot { binding ->
                    binding.typeName.toString() in invalidBindingTypes
                }

                val fields = buildFields(bindings, layoutBinding.bindables)
                val methods = buildMethods(
                    bindingClassName,
                    bindings,
                    layoutBinding.bindables
                ).sortedBy(MethodSpec::name)

                TypeSpec.classBuilder(bindingClassName)
                    .superclass(viewDataBinding)
                    .addModifiers(ABSTRACT, PUBLIC)
                    .addMethods(methods)
                    .addFields(fields)
                    .build()
                    .let { type ->
                        JavaFile.builder(genPackageName, type)
                            .build()
                            .writeTo(outputDir)
                        logFile(genPackageName, type.name)
                    }
            }
        return outputDir
    }

    private fun buildFields(bindings: List<Binding>, bindables: List<Binding>): List<FieldSpec> {
        val bindingFields = bindings
            .asSequence()
            .map { binding ->
                FieldSpec.builder(binding.typeName, binding.name)
                    .addAnnotation(androidNonNull)
                    .addModifiers(PUBLIC, FINAL)
                    .build()
            }
        val bindableFields = bindables
            .asSequence()
            .map { bindable ->
                FieldSpec.builder(bindable.typeName, "m" + bindable.name.capitalize())
                    .addModifiers(PROTECTED)
                    .addAnnotation(bindableAnnotation)
                    .build()
            }
        return (bindingFields + bindableFields).sortedBy(FieldSpec::name).toList()
    }

    private fun buildMethods(
        bindingClassName: ClassName,
        bindings: List<Binding>,
        bindables: List<Binding>
    ): List<MethodSpec> {
        val bindingComponent = "_bindingComponent"
        val root = "_root"
        val localFieldCount = "_localFieldCount"
        val constructor = MethodSpec.constructorBuilder()
            .addParameter(objectClass, bindingComponent)
            .addParameter(viewClass, root)
            .addParameter(INT, localFieldCount)
            .addModifiers(PROTECTED)
            .addParameters(bindings.map { ParameterSpec.builder(it.typeName, it.name).build() })
            .addStatement(
                "super(\$L, \$L, \$L)",
                bindingComponent,
                root,
                localFieldCount
            ).also { builder ->
                bindings.forEach {
                    val name = it.name
                    builder.addStatement("this.$name = $name")
                }
            }.build()

        val inflateMethods = buildInflateMethods(bindingClassName)
        val bindMethods = buildBindMethods(bindingClassName)
        val bindableMethods = buildBindableMethods(bindables)
        return listOf(constructor) + bindableMethods + inflateMethods + bindMethods
    }

    private fun buildInflateMethods(
        bindingClassName: ClassName
    ): List<MethodSpec> = mutableListOf<MethodSpec>().apply {
        /*
         * @NonNull
         * public static <T> inflate(@NonNull LayoutInflater inflater) {}
         */
        val baseInflateMethodBuilder = MethodSpec
            .methodBuilder(INFLATE_METHOD_NAME)
            .addModifiers(STATIC, PUBLIC)
            .addParameter(layoutInflaterClass, INFLATER)
            .addStatement(RUNTIME_EXCEPTION)
            .returns(bindingClassName)
            .addAnnotation(androidNonNull)

        baseInflateMethodBuilder.build().let(::add)

        val inflateWithAttachRootBuilder = baseInflateMethodBuilder
            .addParameter(viewGroupClass, ROOT)
            .addParameter(BOOLEAN, ATTACH_TO_ROOT)

        inflateWithAttachRootBuilder.build().let(::add)

        inflateWithAttachRootBuilder
            .addParameter(objectClass, COMPONENT)
            .build()
            .let(::add)
    }

    private fun buildBindMethods(
        bindingClassName: ClassName
    ): List<MethodSpec> = mutableListOf<MethodSpec>().apply {
        val baseBindMethodBuilder = MethodSpec
            .methodBuilder(BIND_METHOD_NAME)
            .addModifiers(STATIC, PUBLIC)
            .addParameter(viewClass, VIEW)
            .addStatement(RUNTIME_EXCEPTION)
            .returns(bindingClassName)
            .addAnnotation(androidNonNull)

        baseBindMethodBuilder
            .build()
            .let(::add)

        baseBindMethodBuilder
            .addParameter(objectClass, COMPONENT)
            .build()
            .let(::add)
    }.toList()

    private fun buildBindableMethods(bindables: List<Binding>): List<MethodSpec> {
        return bindables.flatMap { bindable ->
            mutableListOf<MethodSpec>().apply {
                val name = bindable.name.capitalize()
                // Setter
                MethodSpec.methodBuilder("set$name")
                    .addModifiers(ABSTRACT, PUBLIC)
                    .addParameter(bindable.typeName, "var1")
                    .build()
                    .let(::add)
                // Getter
                MethodSpec.methodBuilder("get$name")
                    .addModifiers(PUBLIC)
                    .addStatement(RUNTIME_EXCEPTION)
                    .addAnnotation(androidNullable)
                    .returns(bindable.typeName)
                    .build()
                    .let(::add)
            }
        }
    }
}