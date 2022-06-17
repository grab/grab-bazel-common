// Copyright 2014 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.grab.test

import org.junit.runners.Suite
import org.junit.runners.model.RunnerBuilder
import java.lang.reflect.Modifier

/**
 * A JUnit4 suite implementation that delegates the class finding to a `suite()` method on the
 * annotated class. To be used in combination with [TestSuiteBuilder].
 */
class CustomSuite
/**
 * Only called reflectively. Do not use programmatically.
 */
constructor(
    klass: Class<*>,
    builder: RunnerBuilder?
) : Suite(builder, klass, getClasses(klass)) {
    companion object {
        private fun getClasses(klass: Class<*>): Array<Class<*>> {
            val result = evalSuite(klass)
            return result.toTypedArray()
        }

        // unchecked cast to a generic type
        private fun evalSuite(klass: Class<*>): Set<Class<*>> {
            return try {
                val m = klass.getMethod("suite")
                check(Modifier.isStatic(m.modifiers)) { "suite() must be static" }
                @Suppress("UNCHECKED_CAST")
                m.invoke(null) as Set<Class<*>>
            } catch (e: Exception) {
                throw IllegalStateException(e)
            }
        }
    }
}