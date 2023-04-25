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

package com.grab.aapt.databinding.common

import okio.Path.Companion.toPath
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.*

abstract class BaseBindingStubTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    data class TestResFile(val name: String, val contents: String, val path: String = "")

    private val random = Random()

    protected fun testResFiles(vararg testResFiles: TestResFile): List<File> {
        return testResFiles.map { (fileName, contents, path) ->
            File(temporaryFolder.newFolder(random.nextInt().toString() + path), fileName).apply {
                delete()
                writeText(contents)
            }
        }
    }

    class TestResourceBuilder(private val root: File) {
        val files = mutableListOf<File>()
        operator fun String.invoke(content: () -> String) {
            File(root, this).apply {
                parentFile.mkdirs()
                writeText(content())
            }.let(files::add)
        }
    }

    fun buildTestRes(
        root: File = temporaryFolder.newFolder(random.nextInt().toString()),
        testResourceBuilder: TestResourceBuilder.() -> Unit
    ): List<File> = TestResourceBuilder(root = root).apply(testResourceBuilder).files
}