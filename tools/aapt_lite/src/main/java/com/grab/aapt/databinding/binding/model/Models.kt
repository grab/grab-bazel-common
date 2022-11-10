package com.grab.aapt.databinding.binding.model

import com.grab.aapt.databinding.util.capitalize
import com.squareup.javapoet.TypeName
import java.io.File

/**
 * Type to represent binding type in a layout XML
 */
sealed class BindingType {
    object Variable : BindingType()
    object View : BindingType()
    data class IncludedLayout(
        val layoutName: String,
        /**
         * Denote that included layout's type was not found in either local or in dependencies
         */
        val layoutMissing: Boolean = false
    ) : BindingType()
}

/**
 * The type of binding and it's metadata
 */
data class Binding(
    val rawName: String,
    val typeName: TypeName,
    val name: String = rawName
        .split("_")
        .joinToString(
            separator = "",
            transform = String::capitalize
        ).let { Character.toLowerCase(it.first()) + it.substring(1) },
    val bindingType: BindingType
)

data class LayoutBindingData(
    val layoutName: String,
    val file: File,
    // Layout declarations in layout xml
    val bindings: List<Binding>,
    // Variable declarations in layout xml
    val bindables: List<Binding>
)