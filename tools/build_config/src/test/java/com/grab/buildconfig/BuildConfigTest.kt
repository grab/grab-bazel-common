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

package com.grab.buildconfig

import com.grab.buildconfig.sample.BuildConfig
import org.junit.Test
import kotlin.test.assertEquals

class BuildConfigTest {

    @Test
    fun `assert default generated fields`() {
        assertEquals(
            "debug",
            BuildConfig.BUILD_TYPE,
            "Default build type is generated"
        )
        assertEquals(
            "VERSION_NAME",
            BuildConfig.VERSION_NAME,
            "Version name is generated"
        )
    }

    @Test
    fun `assert generated strings in build config`() {
        assertEquals(
            "Hello",
            BuildConfig.SIMPLE_STRING,
            "Simple string"
        )
        assertEquals(
            "\$\$\$\$\$\$\$\$\$\$\$\$\$",
            BuildConfig.WITH_DOLLAR,
            "String with Bazel make variable special characters"
        )
        assertEquals(
            "!@#\$%^&*()\$",
            BuildConfig.SPECIAL_CHARACTERS,
            "String with special characters"
        )
        assertEquals(
            "\\$ Hello",
            BuildConfig.FIELD_WITH_ESCAPED_DOLLAR,
            "String with existing escaped dollar"
        )
        assertEquals(
            "https://reward.com/hc/%1\$s/test",
            BuildConfig.REWARD_URL,
            "Reward url string"
        )
    }

    @Test
    fun `assert generated booleans in build config`() {
        assertEquals(true, BuildConfig.TRUE, "TRUE boolean is true")
        assertEquals(false, BuildConfig.FALSE, "FALSE boolean is false")
        assertEquals(false, BuildConfig.STRING_BOOLEAN, "Boolean specified as string is false")
    }

    @Test
    fun `assert generated ints in build config`() {
        assertEquals(0, BuildConfig.INT, "Generated int is 0")
    }

    @Test
    fun `assert generated longs in build config`() {
        assertEquals(123, BuildConfig.LONG, "Generated long is 0")
    }
}