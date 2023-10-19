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

import com.grazel.generated.TestSuite
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Suite
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class TestSuiteGeneratorTest {

    @Test
    fun annotationProcessorShouldGenerateTestSuiteClassWithRunWithAnnotation() {
        val clazz = TestSuite::class.java
        val runWith = clazz.getAnnotation(RunWith::class.java)
        assertEquals(2, clazz.declaredAnnotations.size)
        assertEquals(Suite::class.java.name, runWith.value.qualifiedName)
    }

    @Test
    fun annotationProcessorShouldGenerateTestSuiteClassWithTestInfoProcessorTestAnnotation() {
        val clazz = TestSuite::class.java
        val suiteClasses = clazz.getAnnotation(Suite.SuiteClasses::class.java)
        assertEquals(2, suiteClasses.value.size)
        assertTrue {
            suiteClasses.value.any {
                it == DummyTestClass::class
            }
        }
        assertTrue {
            suiteClasses.value.any {
                it == TestSuiteGeneratorTest::class
            }
        }
    }
}


class DummyTestClass {
    @Test
    fun testFeatureA() {
    }

    @Test
    fun testFeatureB() {
    }
}