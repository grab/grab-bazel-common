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

import com.grab.aapt.databinding.binding.model.Binding
import com.grab.aapt.databinding.binding.model.BindingType
import com.grab.aapt.databinding.binding.model.LayoutBindingData
import com.grab.aapt.databinding.common.BaseBindingStubTest
import com.squareup.javapoet.ClassName
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Paths
import kotlin.test.assertTrue

class DefaultBrClassGeneratorTest : BaseBindingStubTest() {
    private lateinit var brClassGenerator: BrClassGenerator
    private lateinit var baseDir: File
    private lateinit var layoutBinding: LayoutBindingData
    private val packageName = "packageName"

    private fun generatedFileContents(outputDir: File) = File(
        outputDir,
        Paths.get(packageName, "BR.java").toString()
    ).readText()

    @Before
    fun setUp() {
        baseDir = temporaryFolder.newFolder()
        brClassGenerator = DefaultBrClassGenerator(baseDir)
        val className = ClassName.get(DefaultBrClassGeneratorTest::class.java)
        layoutBinding = LayoutBindingData(
            layoutName = "SimpleBinding",
            file = temporaryFolder.newFile("simple_binding.xml"),
            bindables = listOf(
                Binding(
                    rawName = "bindableName",
                    typeName = className,
                    bindingType = BindingType.Variable
                ),
                Binding(
                    rawName = "userscore_name",
                    typeName = className,
                    bindingType = BindingType.Variable
                ),
                Binding(
                    rawName = "userscore_name",
                    typeName = className,
                    bindingType = BindingType.Variable
                )
            ),
            bindings = emptyList()
        )
    }

    @Test
    fun `assert BR class fields are derived from layout data expressions`() {
        val output = brClassGenerator.generate(packageName, listOf(layoutBinding))
        assertTrue {
            generatedFileContents(output).contains("public static int _all = 0;")
        }
        assertTrue {
            generatedFileContents(output).contains("public static int bindableName = 0;")
        }
        assertTrue {
            generatedFileContents(output).contains("public static int userscore_name = 0;")
        }
    }

    @Test
    fun `assert BR class fields does not have duplicates`() {
        val output = brClassGenerator.generate(packageName, listOf(layoutBinding))
        assertTrue {
            generatedFileContents(output)
                .lineSequence()
                .map { it.trim() }
                .count { it == "public static int userscore_name = 0;" } == 1
        }
    }
}