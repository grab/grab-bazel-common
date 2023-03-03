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

package com.grab.aapt

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.grab.aapt.databinding.util.WorkingDirectory
import java.io.File

class AaptLiteCommand : CliktCommand() {

    private val packageName by option(
        "-p",
        "--package",
        help = "Package name of R class"
    ).required()

    private val resources by option(
        "-res",
        "--resource-files",
        help = "List of resource files to produce R class from"
    ).split(",").default(emptyList())

    private val classInfos: List<String> by option(
        "-ci",
        "--class-infos",
        help = "List of databinding classInfo.zip files from dependencies"
    ).split(",").default(emptyList())

    private val rTxts: List<String> by option(
        "-rt",
        "--r-txts",
        help = "List of dependencies' R.txt files"
    ).split(",").default(emptyList())

    private val nonTransitiveRClass: Boolean by option(
        "-ntr",
        "--non-transitive-r-class",
        help = "When true, assumes R class is not transitive and only use local symbols"
    ).flag(default = false)

    private val rClassSrcJar by option(
        "-r",
        "--r-class-output",
        help = "The R class srcjar location where the R class will be written to"
    ).convert { File(it) }.required()

    private val stubClassJar by option(
        "-s",
        "--stubs-output",
        help = "The stubs srcjar location where the generated stubs will be written to"
    ).convert { File(it) }.required()

    override fun run() {
        val resourcesFiles = resources.map { path -> File(path) }
        val layoutFiles = resourcesFiles.filter { it.path.contains("/layout") }

        val classInfoZip = classInfos.map { File(it) }
        val depRTxts = rTxts.map { File(it) }

        WorkingDirectory().use {
            val command = DaggerAaptLiteComponent.factory().create(
                baseDir = it.dir.toFile(),
                packageName = packageName,
                resourceFiles = resourcesFiles,
                layoutFiles = layoutFiles,
                classInfos = classInfoZip,
                rTxts = depRTxts,
                nonTransitiveRClass = nonTransitiveRClass,
            )
            val layoutBindings = command.layoutBindingsParser().parse(packageName, layoutFiles)
            command.resToRClassGenerator().generate(packageName, resourcesFiles, depRTxts)

            val rClasses = command.brClassGenerator().generate(packageName, layoutBindings)
            command.srcJarPackager.packageSrcJar(inputDir = rClasses, outputFile = rClassSrcJar)

            val dataBindingClasses = command.bindingClassGenerator().generate(
                packageName,
                layoutBindings
            )
            command.srcJarPackager.packageSrcJar(
                inputDir = dataBindingClasses,
                outputFile = stubClassJar
            )
        }
    }
}