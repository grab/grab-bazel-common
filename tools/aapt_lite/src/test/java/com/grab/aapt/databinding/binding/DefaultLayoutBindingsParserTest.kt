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

package com.grab.aapt.databinding.binding

import com.grab.aapt.databinding.binding.model.BindingType
import com.grab.aapt.databinding.binding.parser.DefaultLayoutBindingsParser
import com.grab.aapt.databinding.binding.store.LayoutTypeStore
import com.grab.aapt.databinding.binding.store.LocalModuleLayoutTypeStore
import com.grab.aapt.databinding.common.BaseBindingStubTest
import com.grab.aapt.databinding.util.toLayoutBindingName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DefaultLayoutBindingsParserTest : BaseBindingStubTest() {
    private lateinit var layoutBindingsParser: DefaultLayoutBindingsParser
    private val packageName = "test"
    private lateinit var testLayoutFiles: List<File>

    private object DummyLayoutTypeStore : LayoutTypeStore {
        override fun get(layoutName: String): TypeName? = ClassName
            .bestGuess(this::class.qualifiedName)
    }

    @Before
    fun setUp() {
        testLayoutFiles = testResFiles(
            TestResFile(
                name = "all.xml",
                contents = """
                    <layout xmlns:android="http://schemas.android.com/apk/res/android">
                        <LinearLayout
                            android:layout_width="match_parent"
                            android:id="@+id/time_display"
                            android:layout_height="match_parent">
                        </LinearLayout>
                        <LinearLayout
                            android:layout_width="match_parent"
                            android:layout_height="match_parent">
                        </LinearLayout>
                        <View
                            android:id="@id/createdId">
                        </View>
                        <View
                            android:id="@+id/newId">
                        </View>
                        <View
                            android:id="@+id/newId">
                        </View>
                        <include
                            android:id="@+id/unallocationLoading"
                            layout="@layout/unallocation_loading"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                        />
                        <data>
                            <variable
                                name="vm"
                                type="com.grab.aapt.databinding.stub.TestClass" />
                            <variable
                                name="vm_db"
                                type="Integer" />
                        </data>
                    </layout>
                """.trimIndent()
            ),
            TestResFile("empty.xml", ""),
            TestResFile("empty.xml", ""), // Duplicate
            TestResFile("unallocation_loading.xml", "") // For included layout
        )
        layoutBindingsParser = DefaultLayoutBindingsParser(
            localLayoutTypeStore = LocalModuleLayoutTypeStore(packageName, testLayoutFiles),
            depLayoutTypeStore = DummyLayoutTypeStore
        )
    }

    @Test
    fun `when duplicated files exist assert layout bindings are parsed correctly`() {
        val layoutBindings = layoutBindingsParser.parse(packageName, testLayoutFiles)
        assertEquals(3, layoutBindings.size, "Parsed binding does not have duplicates")

        assertTrue("Layout name is capitalized correctly") {
            layoutBindings.any { it.layoutName == "AllBinding" }
        }
    }

    @Test
    fun `assert bindables are parsed correctly`() {
        val layoutBindings = layoutBindingsParser.parse(packageName, testLayoutFiles)
        val allBindingLayout = layoutBindings.first()

        assertEquals(2, allBindingLayout.bindables.size, "Bindable parsed correctly")
        val vm = allBindingLayout.bindables.first()
        assertEquals("vm", vm.name)
        assertEquals("com.grab.aapt.databinding.stub.TestClass", vm.typeName.toString())
        assertEquals(BindingType.Variable, vm.bindingType)
        val vmDb = allBindingLayout.bindables[1]
        assertEquals("vmDb", vmDb.name)
        assertEquals("vm_db", vmDb.rawName)
        assertEquals("Integer", vmDb.typeName.toString())
        assertEquals(BindingType.Variable, vmDb.bindingType)
    }

    @Test
    fun `assert bindings are parsed correctly`() {
        val layoutBindings = layoutBindingsParser.parse(packageName, testLayoutFiles)
        val allBindingLayout = layoutBindings.first()

        val bindings = allBindingLayout.bindings
        assertEquals(3, bindings.size)

        assertTrue("Already created id are excluded") { bindings.none { it.name == "createdId" } }

        val timeDisplay = bindings.first()
        assertEquals("time_display", timeDisplay.rawName)
        assertEquals("android.widget.LinearLayout", timeDisplay.typeName.toString())
        assertEquals("timeDisplay", timeDisplay.name)
        assertEquals(BindingType.View, timeDisplay.bindingType)

        val newId = bindings[1]
        assertEquals("newId", newId.rawName)
        assertEquals("android.view.View", newId.typeName.toString())
        assertEquals("newId", newId.name)
        assertEquals(BindingType.View, newId.bindingType)

        val includedBinding = bindings[2]
        assertEquals("unallocationLoading", includedBinding.rawName)
        assertEquals(
            "test.databinding.UnallocationLoadingBinding",
            includedBinding.typeName.toString()
        )
        assertEquals("unallocationLoading", includedBinding.name)
        assertEquals(
            BindingType.IncludedLayout("UnallocationLoadingBinding"),
            includedBinding.bindingType
        )
    }

    @Test
    fun `assert imported types are parsed correctly`() {
        val layoutBindings = layoutBindingsParser.parse(
            packageName = packageName,
            layoutFiles = testResFiles(
                TestResFile(
                    name = "layout_with_imports.xml",
                    contents = """
                        <layout xmlns:android="http://schemas.android.com/apk/res/android">
                        <data>
                            <import type="com.grab.View"/>
                            <import type="com.grab.ViewGroup" alias="GroupedView"/>
                            <variable
                                name="view"
                                type="com.grab.View" />
                            <variable
                                name="view_group"
                                type="GroupedView" />
                        </data>
                    </layout>
                    """.trimIndent()
                )
            )
        )
        val bindable = layoutBindings.first().bindables

        val viewBindable = bindable.first()
        assertEquals("view", viewBindable.rawName)
        assertEquals("com.grab.View", viewBindable.typeName.toString())
        assertEquals("view", viewBindable.name)

        val viewGroupBindable = bindable[1]
        assertEquals("view_group", viewGroupBindable.rawName)
        assertEquals("com.grab.ViewGroup", viewGroupBindable.typeName.toString())
        assertEquals("viewGroup", viewGroupBindable.name)
    }

    @Test
    fun `assert inbuilt view types are parsed correctly`() {
        val layoutBindings = layoutBindingsParser.parse(
            packageName = packageName,
            layoutFiles = testResFiles(
                TestResFile(
                    name = "layout_with_android_view_types.xml",
                    contents = """
                    <layout xmlns:android="http://schemas.android.com/apk/res/android">
                        <ViewGroup
                            android:id="@+id/viewGroup">
                        </ViewGroup>
                        <View
                            android:id="@+id/view">
                        </View>
                        <ViewStub
                            android:id="@+id/viewStub">
                        </ViewStub>
                        <TextureView
                            android:id="@+id/textureView">
                        </TextureView>
                        <SurfaceView
                            android:id="@+id/surfaceView">
                        </SurfaceView>
                        <WebView
                            android:id="@+id/webView">
                        </WebView>
                    </layout>
                    """.trimIndent()
                )
            )
        ).first().bindings
        assertTrue {
            layoutBindings.size == 6
        }
        assertTrue {
            layoutBindings[0].typeName.toString() == "android.view.ViewGroup"
        }
        assertTrue {
            layoutBindings[1].typeName.toString() == "android.view.View"
        }
        assertTrue {
            layoutBindings[2].typeName.toString() == "androidx.databinding.ViewStubProxy"
        }
        assertTrue {
            layoutBindings[3].typeName.toString() == "android.view.TextureView"
        }
        assertTrue {
            layoutBindings[4].typeName.toString() == "android.view.SurfaceView"
        }
        assertTrue {
            layoutBindings[5].typeName.toString() == "android.webkit.WebView"
        }
    }


    @Test
    fun `assert primitive types in variables are parsed correctly`() {
        val layoutBindablesWithPrimitives = layoutBindingsParser.parse(
            packageName = packageName,
            layoutFiles = testResFiles(
                TestResFile(
                    name = "layout_with_primitives.xml",
                    contents = """
                    <layout xmlns:android="http://schemas.android.com/apk/res/android">
                        <data>
                            <variable
                                name="boolean"
                                type="boolean" />
                            <variable
                                name="boxed_integer"
                                type="Integer" /> 
                            <variable
                                name="float"
                                type="float" /> 
                        </data>
                    </layout>
                    """.trimIndent()
                )
            )
        ).first().bindables

        val booleanBinding = layoutBindablesWithPrimitives.first()
        val boolean = "boolean"
        assertEquals(boolean, booleanBinding.rawName)
        assertEquals(boolean, booleanBinding.typeName.toString())
        assertEquals(boolean, booleanBinding.name)

        val boxedInteger = layoutBindablesWithPrimitives[1]
        assertEquals("boxed_integer", boxedInteger.rawName)
        assertEquals("Integer", boxedInteger.typeName.toString())
        assertEquals("boxedInteger", boxedInteger.name)

        val floatBindidng = layoutBindablesWithPrimitives[2]
        val float = "float"
        assertEquals(float, floatBindidng.rawName)
        assertEquals(float, floatBindidng.typeName.toString())
        assertEquals(float, floatBindidng.name)
    }

    @Test
    fun `assert binding type is parsed correctly`() {
        assertEquals(
            BindingType.View,
            layoutBindingsParser.parseBindingType(
                "View",
                mapOf("android:id" to "android:id=\"@+id/new\"")
            )
        )
        assertFailsWith<IllegalStateException>("Missing @layout") {
            layoutBindingsParser.parseBindingType(
                "include",
                mapOf("android:id" to "android:id=\"@+id/new\"")
            )
        }
        assertEquals(
            BindingType.IncludedLayout("UnallocationLoadingBinding"),
            layoutBindingsParser.parseBindingType(
                "include",
                mapOf(
                    "android:id" to "android:id=\"@+id/new\"",
                    "layout" to "@layout/unallocation_loading"
                )
            )
        )
        assertFailsWith<IllegalStateException>("Missing android:id") {
            layoutBindingsParser.parseBindingType(
                "View",
                mapOf()
            )
        }
        // Test layout missing is propogated in result
        assertEquals(
            BindingType.IncludedLayout(
                "UnallocationLoadingBinding",
                layoutMissing = true
            ),
            layoutBindingsParser.parseBindingType(
                "include",
                mapOf(
                    "android:id" to "android:id=\"@+id/new\"",
                    "layout" to "@layout/unallocation_loading"
                ),
                layoutMissing = true
            )
        )
    }

    @Test
    fun `assert layout with cross module include layout is parsed correctly `() {
        testLayoutFiles = testResFiles(
            TestResFile(
                name = "layout_with_cross_module_import.xml",
                contents = """
                    <layout xmlns:android="http://schemas.android.com/apk/res/android">
                        <include
                            android:id="@+id/unallocationLoading"
                            layout="@layout/unallocation_loading"
                            android:layout_width="match_parent"
                            android:layout_height="wrap_content"
                        />
                    </layout>
                    """.trimIndent()
            )
        )
        val layoutName = "unallocation_loading"
        val layoutType = layoutName.toLayoutBindingName() // Dummy
        layoutBindingsParser = DefaultLayoutBindingsParser(
            localLayoutTypeStore = LocalModuleLayoutTypeStore(packageName, testLayoutFiles),
            depLayoutTypeStore = object : LayoutTypeStore {
                override fun get(layoutName: String): TypeName? {
                    return if (layoutName == layoutName) ClassName.bestGuess(layoutType) else {
                        error("Failed")
                    }
                }
            }
        )
        assertEquals(
            layoutType,
            layoutBindingsParser.parseIncludedLayoutType(layoutName).toString()
        )
    }
}