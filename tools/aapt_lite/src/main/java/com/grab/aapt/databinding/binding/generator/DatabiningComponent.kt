package com.grab.aapt.databinding.binding.generator

import com.grab.aapt.databinding.common.DATABINDING_PACKAGE
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import java.io.File
import javax.lang.model.element.Modifier

private val DatabindingComponent = ClassName.get(DATABINDING_PACKAGE, "DataBindingComponent")

fun generateDataBindingComponentInterface(outputDir: File) {
    TypeSpec.interfaceBuilder(DatabindingComponent)
        .addModifiers(Modifier.PUBLIC)
        .build()
        .let { type ->
            JavaFile.builder(DATABINDING_PACKAGE, type)
                .build()
                .writeTo(outputDir)
        }
}