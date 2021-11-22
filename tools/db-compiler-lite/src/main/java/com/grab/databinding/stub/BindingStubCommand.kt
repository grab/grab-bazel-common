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

package com.grab.databinding.stub

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import java.io.File

class BindingStubCommand : CliktCommand() {

    private val resources by option(
        "-res",
        "--resource-files",
        help = "List of module res files"
    ).split(",").default(emptyList())

    private val packageName by option(
        "-p",
        "--package",
        help = "Package name of R class"
    ).required()

    private val preferredOutputDir by option(
        "-o",
        "--output"
    ).convert { File(it) }

    private val databindingMetadataDir: File by option(
        "-dm",
        "--databinding-metadata",
        help = "Path to databinding metadata folder containing class-infos and R.txt from dependencies"
    ).convert { path ->
        val dir = File(path)
        when {
            dir.endsWith("r_txt") || dir.endsWith("class_infos") -> dir.parentFile
            else -> dir
        }
    }.required()

    override fun run() {
        val resourcesFiles = resources.map { path -> File(path) }
        val layoutFiles = resourcesFiles.filter { it.path.contains("/layout") }
        val classInfoDir = File(databindingMetadataDir, "class_infos")
        val rTxtDir = File(databindingMetadataDir, "r_txt")
        DaggerBindingsStubComponent
            .factory()
            .create(
                outputDir = preferredOutputDir,
                packageName = packageName,
                resourceFiles = resourcesFiles,
                layoutFiles = layoutFiles,
                classInfoDir = classInfoDir,
                rTxtDir = rTxtDir
            ).apply {
                val layoutBindings = layoutBindingsParser().parse(packageName, layoutFiles)
                resToRClassGenerator().generate(packageName, resourcesFiles, rTxtDir)
                brClassGenerator().generate(packageName, layoutBindings)
                bindingClassGenerator().generate(packageName, layoutBindings)
            }
    }
}