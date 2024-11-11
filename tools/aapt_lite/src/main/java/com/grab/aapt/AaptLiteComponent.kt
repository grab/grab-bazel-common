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

import com.grab.aapt.databinding.binding.BindingClassModule
import com.grab.aapt.databinding.binding.generator.BindingClassGenerator
import com.grab.aapt.databinding.binding.parser.BindingsParserModule
import com.grab.aapt.databinding.binding.parser.LayoutBindingsParser
import com.grab.aapt.databinding.brclass.BrClassGenerator
import com.grab.aapt.databinding.brclass.BrClassModule
import com.grab.aapt.databinding.common.BASE_DIR
import com.grab.aapt.databinding.common.CLASS_INFOS
import com.grab.aapt.databinding.common.LAYOUT_FILES
import com.grab.aapt.databinding.common.NON_TRANSITIVE_R
import com.grab.aapt.databinding.common.PACKAGE_NAME
import com.grab.aapt.databinding.common.RES_FILES
import com.grab.aapt.databinding.common.R_TXTS
import com.grab.aapt.databinding.di.AaptScope
import com.grab.aapt.databinding.rclass.ResourceParserModule
import com.grab.aapt.databinding.rclass.generator.RClassModule
import com.grab.aapt.databinding.rclass.generator.ResToRClassGenerator
import com.grab.aapt.databinding.util.jars.SrcJarPackageModule
import com.grab.aapt.databinding.util.jars.SrcJarPackager
import dagger.BindsInstance
import dagger.Component
import java.io.File
import javax.inject.Named

@AaptScope
@Component(
    modules = [
        RClassModule::class,
        BindingClassModule::class,
        BrClassModule::class,
        BindingsParserModule::class,
        ResourceParserModule::class,
        SrcJarPackageModule::class
    ]
)
interface AaptLiteComponent {
    val layoutBindingsParser: LayoutBindingsParser
    val resToRClassGenerator: ResToRClassGenerator
    val bindingClassGenerator: BindingClassGenerator
    val brClassGenerator: BrClassGenerator
    val srcJarPackager: SrcJarPackager

    @Component.Factory
    interface Factory {
        /**
         * Construct the [AaptLiteComponent] injector.
         *
         * @param baseDir The base directory where files will be written to
         * @param packageName The package name of the target for which stubs are to be generated
         * @param layoutFiles The list of all layout xmls
         * @param resourceFiles The list of all resource files for compilation
         * @param classInfos The list of databinding classInfo.zips from direct dependencies
         * @param rTxts The list of R.txts from direct dependencies.
         * @param nonTransitiveRClass Whether generated R class source jars should be namespaced
         * and not contain any transitive entries
         */
        fun create(
            @BindsInstance @Named(BASE_DIR) baseDir: File,
            @BindsInstance @Named(PACKAGE_NAME) packageName: String,
            @BindsInstance @Named(LAYOUT_FILES) layoutFiles: List<File>,
            @BindsInstance @Named(RES_FILES) resourceFiles: List<File>,
            @BindsInstance @Named(CLASS_INFOS) classInfos: List<File>,
            @BindsInstance @Named(R_TXTS) rTxts: List<File>,
            @BindsInstance @Named(NON_TRANSITIVE_R) nonTransitiveRClass: Boolean,
        ): AaptLiteComponent
    }
}