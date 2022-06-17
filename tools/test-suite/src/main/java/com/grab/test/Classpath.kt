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

import com.google.common.reflect.ClassPath
import java.io.IOException


/**
 * A helper class to find all classes on the current classpath. This is used to automatically create
 * JUnit 3 and 4 test suites.
 */
object Classpath {

    /** Finds all classes that live in or below the given package.  */
    @Throws(ClassPathException::class)
    fun findClasses(packageName: String): Set<Class<*>> {
        val result: MutableSet<Class<*>> = LinkedHashSet()
        val packagePrefix = "$packageName.".replace('/', '.')
        try {
            for (ci in ClassPath.from(Classpath::class.java.classLoader).allClasses) {
                if (ci.name.startsWith(packagePrefix)) {
                    try {
                        result.add(ci.load())
                    } catch (unused: UnsatisfiedLinkError) {
                        // Ignore: we're most likely running on a different platform.
                    } catch (unused: NoClassDefFoundError) {
                    }
                }
            }
        } catch (e: IOException) {
            throw ClassPathException(e.message!!)
        }
        return result
    }

    /**
     * Base exception for any classpath related errors.
     */
    class ClassPathException(
        format: String,
        vararg args: Any?
    ) : Exception(String.format(format, *args))
}