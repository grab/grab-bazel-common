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

import com.google.common.base.Predicate
import com.google.common.base.Predicates
import com.google.common.collect.Iterables
import com.google.common.collect.Sets
import java.lang.reflect.Modifier


/**
 * A collector for test classes, for both JUnit 3 and 4. To be used in combination with [ ].
 */
class TestSuiteBuilder {
    private val testClasses: MutableSet<Class<*>> = Sets.newTreeSet(TestClassNameComparator())
    private var matchClassPredicate: Predicate<Class<*>> = Predicates.alwaysTrue()

    /**
     * Adds the tests found (directly) in class `c` to the set of tests
     * this builder will search.
     */
    fun addTestClass(c: Class<*>): TestSuiteBuilder {
        testClasses.add(c)
        return this
    }

    /**
     * Adds all the test classes (top-level or nested) found in package
     * `pkgName` or its subpackages to the set of tests this builder will
     * search.
     */
    fun addPackageRecursive(pkgName: String): TestSuiteBuilder {
        for (c in getClassesRecursive(pkgName)) {
            addTestClass(c)
        }
        return this
    }

    private fun getClassesRecursive(pkgName: String): Set<Class<*>> {
        val result: MutableSet<Class<*>> = LinkedHashSet()
        try {
            for (clazz in Classpath.findClasses(pkgName)) {
                if (isTestClass(clazz)) {
                    result.add(clazz)
                }
            }
        } catch (e: Classpath.ClassPathException) {
            throw AssertionError("Cannot retrive classes: " + e.message)
        }
        return result
    }

    /**
     * Specifies a predicate returns false for classes we want to exclude.
     */
    fun matchClasses(predicate: Predicate<Class<*>>): TestSuiteBuilder {
        matchClassPredicate = predicate
        return this
    }

    /**
     * Creates and returns a TestSuite containing the tests from the given
     * classes and/or packages which matched the given tags.
     */
    fun create(): Set<Class<*>> {
        val result: MutableSet<Class<*>> = LinkedHashSet()
        for (testClass in Iterables.filter(testClasses, matchClassPredicate)) {
            result.add(testClass)
        }
        return result
    }

    private class TestClassNameComparator : Comparator<Class<*>?> {
        override fun compare(o1: Class<*>?, o2: Class<*>?): Int {
            return o1!!.name.compareTo(o2!!.name)
        }
    }

    companion object {
        /**
         * Determines if a given class is a test class.
         *
         * @param container class to test
         * @return `true` if the test is a test class.
         */
        private fun isTestClass(container: Class<*>): Boolean {
            return (containsTestMethods(container) && !Modifier.isAbstract(container.modifiers))
        }

        private fun containsTestMethods(container: Class<*>): Boolean {
            return try {
                container.methods.any {
                    it.annotations
                        .any { annotation ->
                            annotation
                                .annotationClass
                                .qualifiedName == "org.junit.Test"
                        }
                }
            } catch (e: ClassNotFoundException) {
                false
            } catch (e: NoClassDefFoundError) {
                false
            }
        }
    }
}
