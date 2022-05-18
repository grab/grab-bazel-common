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

import com.grab.databinding.stub.binding.BindingClassModule
import com.grab.databinding.stub.binding.generator.BindingClassGenerator
import com.grab.databinding.stub.binding.parser.BindingsParserModule
import com.grab.databinding.stub.binding.parser.LayoutBindingsParser
import com.grab.databinding.stub.brclass.BrClassGenerator
import com.grab.databinding.stub.brclass.BrClassModule
import com.grab.databinding.stub.common.*
import com.grab.databinding.stub.rclass.di.ResToRClassModule
import com.grab.databinding.stub.rclass.generator.RClassModule
import com.grab.databinding.stub.rclass.generator.ResToRClassGenerator
import dagger.BindsInstance
import dagger.Component
import java.io.File
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        RClassModule::class,
        BindingClassModule::class,
        BrClassModule::class,
        BindingsParserModule::class,
        ResToRClassModule::class,
        SrcJarPackageModule::class
    ]
)
interface BindingsStubComponent {
    fun layoutBindingsParser(): LayoutBindingsParser
    fun resToRClassGenerator(): ResToRClassGenerator
    fun bindingClassGenerator(): BindingClassGenerator
    fun brClassGenerator(): BrClassGenerator
    val srcJarPackager: SrcJarPackager

    @Component.Factory
    interface Factory {
        /**
         * Construct the [BindingsStubComponent] injector.
         *
         * @param outputDir The output dir root where the files should be generated
         * @param packageName The package name of the target for which stubs are to be generated
         * @param layoutFiles The list of all layout xmls
         * @param resourceFiles The list of all resource files for compilation
         * @param classInfos The list of databinding classInfo.zips from direct dependencies
         * @param rTxts The list of R.txts from direct dependencies.
         * @param nonTransitiveRClass Whether generated R class source jars should be namespaced
         * and not contain any transitive entries
         */
        fun create(
            @BindsInstance @Named(OUTPUT) outputDir: File?,
            @BindsInstance @Named(PACKAGE_NAME) packageName: String,
            @BindsInstance @Named(LAYOUT_FILES) layoutFiles: List<File>,
            @BindsInstance @Named(RES_FILES) resourceFiles: List<File>,
            @BindsInstance @Named(CLASS_INFOS) classInfos: List<File>,
            @BindsInstance @Named(R_TXTS) rTxts: List<File>,
            @BindsInstance @Named(NON_TRANSITIVE_R) nonTransitiveRClass: Boolean,
        ): BindingsStubComponent
    }
}