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
import androidx.databinding.InverseBindingAdapter
import org.junit.Test
import kotlin.test.assertTrue

private const val TEST_BIND = "testBind"

open class Node(var value: Int = 0) // Non-primitive object to emulate pass by reference

@BindingAdapter(TEST_BIND)
fun updateNode(node: Node, value: Int) {
    node.value = value
}

@InverseBindingAdapter(attribute = TEST_BIND)
fun getUpdateNode(node: Node, value: Int) {
    updateNode(node, value)
}

object ObjectBindingAdapter {
    @BindingAdapter(TEST_BIND)
    @JvmStatic
    fun objectUpdateNode(node: Node, value: Int) {
        updateNode(node, value)
    }
}

@BindingAdapter(TEST_BIND)
fun Node.extensionUpdateNode(value: Int) {
    updateNode(this, value)
}

@BindingAdapter(TEST_BIND)
fun Node.extensionUpdateNode(value: String) {
    updateNode(this, value.toInt())
}

@BindingAdapter(TEST_BIND)
// Method that has generics in type.
fun genericsUpdateNode(node: Node, items: List<String>) {
    updateNode(node, items.size)
}

@BindingAdapter(TEST_BIND)
// Method that has generics in type with variance
fun genericsUpdateNodeVariance(node: Node, items: List<*>) {
    updateNode(node, items.size)
}

@BindingAdapter(TEST_BIND)
fun lambdaUpdateNode(node: Node, value: () -> Int) {
    updateNode(node, value())
}

interface ValueSam {
    fun value(): Int
}

@BindingAdapter(TEST_BIND)
fun samUpdateNode(node: Node, value: ValueSam) {
    updateNode(node, value.value())
}


@BindingAdapter("bindRecyclerViewAdapter")
fun <T : Node> Node.genericsUpdateNode(node: T, value: Int) {
    updateNode(node, value)
}

class CompanionTest<T> {
    companion object Databinding {
        @BindingAdapter(TEST_BIND)
        @JvmStatic
        fun <T : Node> companionUpdateNode(node: T, value: Int) {
            updateNode(node, value)
        }
    }
}

class BindingAdapterProcessorTest {

    @Test
    fun `for simple binding adapters, assert java proxy forwards to Kotlin implementation`() {
        assertNodeUpdate(com_grab_pax_binding_processor_Binding_Adapter_Stub::updateNode)
    }

    @Test
    fun `for binding adapters inside Object, assert java proxy forwards to Kotlin implementation`() {
        assertNodeUpdate(com_grab_pax_binding_processor_Binding_Adapter_Stub::objectUpdateNode)
    }

    @Test
    fun `when binding adapter is used as an extension function, assert java proxy forwards to Kotlin implmentation`() {
        assertNodeUpdate(com_grab_pax_binding_processor_Binding_Adapter_Stub::extensionUpdateNode)
    }

    @Test
    fun `when binding adapter used duplicate method names, assert proxy forwards to correct Kotlin implmentation`() {
        assertNodeUpdate(com_grab_pax_binding_processor_Binding_Adapter_Stub::extensionUpdateNode)
        assertNodeUpdate { node, value ->
            com_grab_pax_binding_processor_Binding_Adapter_Stub
                .extensionUpdateNode(node, value.toString())
        }
    }

    @Test
    fun `when binding adapter uses generics in params types, proxy forwards to correct Kotlin implmentation`() {
        assertNodeUpdate { node, value ->
            com_grab_pax_binding_processor_Binding_Adapter_Stub
                .genericsUpdateNode(node, List(value, Int::toString))
        }
        assertNodeUpdate { node, value ->
            com_grab_pax_binding_processor_Binding_Adapter_Stub
                .genericsUpdateNodeVariance(node, List(value, Int::toString))
        }
    }

    @Test
    fun `when binding adapter uses lamdas, assert proxy forwards to correct kotlin implmenentation`() {
        assertNodeUpdate { node, value ->
            com_grab_pax_binding_processor_Binding_Adapter_Stub.lambdaUpdateNode(node) { value }
        }
    }

    @Test
    fun `when binding adapter uses sam, assert proxy forwards to correct Kotlin implementation`() {
        assertNodeUpdate { node, value ->
            com_grab_pax_binding_processor_Binding_Adapter_Stub.samUpdateNode(
                node,
                object : ValueSam {
                    override fun value() = value
                }
            )
        }
    }

    @Test
    fun `when binding adapter uses generics, assert proxy forwards to Kotlin implmentation`() {
        assertNodeUpdate { node, value ->
            com_grab_pax_binding_processor_Binding_Adapter_Stub.genericsUpdateNode(
                node,
                node,
                value
            )
        }
    }

    @Test
    fun `when inverse binding adapters are used, assert proxy forwards to Kotlin implementation`() {
        assertNodeUpdate(com_grab_pax_binding_processor_Binding_Adapter_Stub::getUpdateNode)
    }

    @Test
    fun `when binding adapters in named companion object, assert proxy forwards to Kotlin implementation`() {
        assertNodeUpdate(com_grab_pax_binding_processor_Binding_Adapter_Stub::companionUpdateNode)
    }

    private fun assertNodeUpdate(updateFunction: (Node, Int) -> Unit) {
        val node = Node()
        val value = 10
        updateFunction(node, value)
        assertTrue("Proxy method calculates value") {
            node.value == value
        }
    }
}