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

package com.grab.databinding.stub.binding.store

import com.grab.databinding.stub.common.CLASS_INFO_DIR
import com.grab.databinding.stub.common.LAYOUT_FILES
import com.grab.databinding.stub.common.PACKAGE_NAME
import com.grab.databinding.stub.util.toLayoutBindingName
import com.squareup.javapoet.ClassName
import com.squareup.javapoet.TypeName
import dagger.Binds
import dagger.Module
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.util.function.BiPredicate
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.LazyThreadSafetyMode.NONE

/**
 * Store that is able to provide [TypeName] when given a layout name represented as simple string
 * i.e without `@layout/`.
 */
interface LayoutTypeStore {
    /**
     * For a given [layoutName], return the fully qualified class name as [TypeName] instance that
     * would be generated by databinding compiler.
     */
    operator fun get(layoutName: String): TypeName?
}

const val LOCAL = "local"
const val DEPS = "deps"

@Module(includes = [BindingClassJsonParserModule::class])
interface LayoutStoreModule {
    @Named(LOCAL)
    @Binds
    fun LocalModuleLayoutTypeStore.localStore(): LayoutTypeStore

    @Named(DEPS)
    @Binds
    fun DependenciesLayoutTypeStore.depsStore(): LayoutTypeStore
}

/**
 * [LayoutTypeStore] implementation that looks for generated [TypeName] for a layout in current module's
 * layout source file.
 *
 * This class infers the generated class name from the layout name itself. For example, for
 * `simple_layout`, the generated class name will be `<package-name>.databinding.SimpleLayoutBinding`
 */
@Singleton
class LocalModuleLayoutTypeStore
@Inject
constructor(
    @Named(PACKAGE_NAME) currentTargetPackageName: String,
    @Named(LAYOUT_FILES) layoutFiles: List<File>
) : LayoutTypeStore {

    /**
     * Caches all the layout name and their generated class name.
     */
    private val layoutTypeMap: Map</* layout name */String, TypeName> by lazy(NONE) {
        layoutFiles
            .asSequence()
            .map { file ->
                val key = file.name.split(".xml").first()
                val value = ClassName.get(
                    "$currentTargetPackageName.databinding",
                    key.toLayoutBindingName()
                )
                key to value
            }.toMap()
    }

    override fun get(layoutName: String) = layoutTypeMap[layoutName]
}

/**
 * [LayoutTypeStore] implementation that searches layout type for the given layout in the
 * given dependencies' [classInfoDir] directory.
 *
 * The implementation lazily parses the files on demand and utilizes caching to avoid doing
 * duplicating work.
 *
 * @param classInfoDir The directory from Bazel containing classInfo.zips of direct dependencies.
 * @param bindingClassJsonParser [BindingClassJsonParser] implementation that will be used to parse
 *                     contents of each binding class json file.
 */
@Singleton
class DependenciesLayoutTypeStore
@Inject
constructor(
    @Named(CLASS_INFO_DIR) private val classInfoDir: File,
    private val bindingClassJsonParser: BindingClassJsonParser,
) : LayoutTypeStore {

    private val zipFilePredicate = BiPredicate<Path, BasicFileAttributes> { path, attr ->
        attr.isRegularFile && path.toString().endsWith(".zip")
    }

    /**
     * The directory where the classInfo.zips will be extracted to
     */
    var extractionDir: File = Files.createTempDirectory("temp").toFile()

    /**
     * All classInfo.zip under [classInfoDir]
     */
    private val classInfoZips: List<File> by lazy(NONE) {
        // Bit inefficient to load all files eagerly, but we work with it now
        classInfoDir.walkTopDown()
            .filter { it.isFile }
            .filter { it.name.endsWith(".zip") }
            .sortedBy { Files.size(it.toPath()) }
            .toList()
    }

    /**
     * Stores classInfo.zip extraction result. This is used to avoid doing repeat extractions when
     * multiple request for extractions are received in [bindingClassJsonFiles]
     * Key: name of the classInfo zip file
     * Values: Binding class json files
     */
    private val classInfoZipContentCache = mutableMapOf<String, List<File>>()
        .withDefault { emptyList() }

    /**
     * Extract the classInfo.zip to [extractionDir] and return list of binding class json files in
     * it.
     * If the zip was already extracted, then return the last known result directly
     * from [classInfoZipContentCache]
     *
     * @param classInfoZip The class info zip that must be extracted
     */
    private fun bindingClassJsonFiles(classInfoZip: File): List<File> {
        if (classInfoZipContentCache.containsKey(classInfoZip.name)) {
            return classInfoZipContentCache.getValue(classInfoZip.name)
        } else {
            // Perform an extraction and cache the result
            val jsonFiles = mutableListOf<File>()
            ZipFile(classInfoZip).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    zip.getInputStream(entry).use { input ->
                        val dir = File(extractionDir, classInfoZip.nameWithoutExtension)
                        val extractedFile = File(dir, entry.name).apply { parentFile?.mkdirs() }
                        when {
                            entry.isDirectory -> extractedFile.mkdirs()
                            else -> {
                                extractedFile
                                    .also { jsonFiles.add(it) }
                                    .outputStream()
                                    .use { output -> input.copyTo(output) }
                            }
                        }
                    }
                }
            }
            classInfoZipContentCache[classInfoZip.name] = jsonFiles
            return jsonFiles
        }
    }

    /**
     * Cache already served requests for layout typename
     */
    private val layoutTypeCache = mutableMapOf<String, String>()

    override fun get(layoutName: String): TypeName? {
        return if (layoutTypeCache.containsKey(layoutName)) {
            ClassName.bestGuess(layoutTypeCache[layoutName])
        } else {
            // Iterate over all classInfo.zip contents and return result from their json
            classInfoZips.forEach {
                bindingClassJsonFiles(classInfoZip = it).forEach { jsonFile ->
                    // Iterate over each file, parsing and checking for the given layout name.
                    //
                    // If found, exit early and return the layout type.
                    // We iterate over file by file to avoid eagerly parsing all of them.
                    // It is responsibility of `bindingClassJsonParser` to cache duplicate requests
                    val parsedContents: Map<String, String> = bindingClassJsonParser.parse(jsonFile)
                    if (parsedContents.containsKey(layoutName)) {
                        // Cache the request for future access
                        layoutTypeCache[layoutName] = parsedContents.getValue(layoutName)
                        return ClassName.bestGuess(layoutTypeCache[layoutName])
                    }
                }
            }
            return null
        }
    }
}