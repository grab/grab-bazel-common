/*
 * Copyright (c) 2012-2023 Grab Taxi Holdings PTE LTD (GRAB), All Rights Reserved. NOTICE: All information contained herein is, and remains the property of GRAB. The intellectual and technical concepts contained herein are confidential, proprietary and controlled by GRAB and may be covered by patents, patents in process, and are protected by trade secret or copyright law.
 * You are strictly forbidden to copy, download, store (in any medium), transmit, disseminate, adapt or change this material in any way unless prior written permission is obtained from GRAB. Access to the source code contained herein is hereby forbidden to anyone except current GRAB employees or contractors with binding Confidentiality and Non-disclosure agreements explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication or disclosure of this source code, which includes information that is confidential and/or proprietary, and is a trade secret, of GRAB.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC PERFORMANCE, OR PUBLIC DISPLAY OF OR THROUGH USE OF THIS SOURCE CODE WITHOUT THE EXPRESS WRITTEN CONSENT OF GRAB IS STRICTLY PROHIBITED, AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES. THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS, OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT MAY DESCRIBE, IN WHOLE OR IN PART.
 */

package com.grab.buildconfig

import com.squareup.javapoet.FieldSpec
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeName
import com.squareup.javapoet.TypeSpec
import java.io.File
import java.lang.reflect.Type
import javax.lang.model.element.Modifier

class BuildConfigGenerator {

    private fun String.toPair(): Pair<String, String> {
        val keyValue = this.split("=")
        return keyValue[0] to keyValue[1]
    }

    private fun String.toFieldSpec(
        type: Type,
        initializerFormat: String = "\$S",
    ): FieldSpec = this
        .toPair()
        .let { (name, value) ->
            FieldSpec
                .builder(type, name)
                .initialize(
                    initializerFormat = initializerFormat,
                    value = value,
                )
                .build()
        }

    private fun String.toFieldSpec(
        type: TypeName,
        initializerFormat: String = "\$L",
    ): FieldSpec = this
        .toPair()
        .let { (name, value) ->
            FieldSpec
                .builder(type, name)
                .initialize(
                    initializerFormat = initializerFormat,
                    value = value,
                )
                .build()
        }

    private fun FieldSpec.Builder.initialize(
        value: String,
        initializerFormat: String,
    ): FieldSpec.Builder = this
        .addModifiers(
            Modifier.PUBLIC,
            Modifier.STATIC,
            Modifier.FINAL
        )
        .initializer(initializerFormat, value)

    fun generate(
        packageName: String,
        output: File? = null,
        strings: List<String>,
        booleans: List<String>,
        ints: List<String>,
        longs: List<String>,
    ) {
        val stringFields = strings
            .map { it.toFieldSpec(String::class.java) }

        val booleanFields = booleans
            .map {
                it.toFieldSpec(
                    type = TypeName.BOOLEAN,
                    initializerFormat = "Boolean.parseBoolean(\$S)",
                )
            }

        val intFields = ints
            .map { it.toFieldSpec(TypeName.INT) }

        val longFields = longs
            .map {
                it.toFieldSpec(
                    type = TypeName.LONG,
                    initializerFormat = "\$LL",
                )
            }

        TypeSpec
            .classBuilder("BuildConfig")
            .addModifiers(Modifier.FINAL, Modifier.PUBLIC)
            .addFields(stringFields)
            .addFields(booleanFields)
            .addFields(intFields)
            .addFields(longFields)
            .build()
            .let { typeSpec ->
                JavaFile
                    .builder(packageName, typeSpec)
                    .build()
                    .apply {
                        if (output != null) {
                            writeTo(output)
                        } else {
                            writeTo(System.out)
                        }
                    }
            }
    }
}