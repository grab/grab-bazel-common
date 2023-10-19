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

package com.grab.test

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import java.io.File

class GeneratorCommand : CliktCommand() {

    private val inputJarFile: File by option(
        "-i",
        "--input-jar",
        help = "Path to the input jar file"
    ).convert { File(it) }.required()

    private val outputJarFile: File by option(
        "-o",
        "--output-jar",
        help = "Path to the output jar file"
    ).convert { File(it) }.required()

    private val returnDefaultValues: Boolean by option(
        "-d",
        "--default-value",
        help = "Set the flag to true to make the methods return default values"
    ).convert { it == "true" }.default(true)

    override fun run() {
        MockableJarGenerator(returnDefaultValues).createMockableJar(inputJarFile, outputJarFile)
    }
}