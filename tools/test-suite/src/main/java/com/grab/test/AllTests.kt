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

import org.junit.runner.RunWith


/** Runs all tests in the classpath.  */
@RunWith(CustomSuite::class)
class AllTests {
    companion object {
        @JvmStatic
        fun suite(): Set<Class<*>> {
            return TestSuiteBuilder()
                .addPackageRecursive(getInjectedPackage())
                .create()
        }

        private fun getInjectedPackage(): List<String> = Class
            .forName("com.grab.test.TestPackageName")
            .fields.first { it.name == "PACKAGE_NAMES" }
            .get(null) as List<String>
    }
}
