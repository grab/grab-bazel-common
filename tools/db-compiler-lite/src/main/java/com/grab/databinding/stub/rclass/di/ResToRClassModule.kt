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

package com.grab.databinding.stub.rclass.di

import com.grab.databinding.stub.common.OUTPUT
import com.grab.databinding.stub.rclass.generator.ResToRClassGenerator
import com.grab.databinding.stub.rclass.generator.ResToRClassGeneratorImpl
import com.grab.databinding.stub.rclass.parser.DefaultResToRParser
import com.grab.databinding.stub.rclass.parser.ParserType
import com.grab.databinding.stub.rclass.parser.ResToRParser
import com.grab.databinding.stub.rclass.parser.ResourceFileParser
import com.grab.databinding.stub.rclass.parser.xml.*
import dagger.Binds
import dagger.MapKey
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import java.io.File
import javax.inject.Named
import javax.inject.Singleton

@MapKey
annotation class ParserKey(val value: ParserType)

@Module
interface ResourceParserModule {
    @Binds
    @IntoMap
    @ParserKey(ParserType.STYLE_PARSER)
    fun StyleParser.styleParser(): ResourceFileParser

    @Binds
    @IntoMap
    @ParserKey(ParserType.STYLEABLE_PARSER)
    fun DeclareStyleableParser.declareStyleableParser(): ResourceFileParser

    @Binds
    @IntoMap
    @ParserKey(ParserType.ARRAY_PARSER)
    fun ArrayParser.arrayParser(): ResourceFileParser

    @Binds
    @IntoMap
    @ParserKey(ParserType.ID_PARSER)
    fun IDParser.idParser(): ResourceFileParser

    @Binds
    @IntoMap
    @ParserKey(ParserType.DEFAULT_PARSER)
    fun DefaultXmlParser.defaultXmlParser(): ResourceFileParser

    @Binds
    @Singleton
    fun DefaultResToRParser.defaultResToRParser(): ResToRParser
}

@Module(includes = [ResourceParserModule::class])
object ResToRClassModule {
    @JvmStatic
    @Provides
    @Singleton
    fun resToRClassGeneratorImpl(
        @Named(OUTPUT) dir: File?,
        resToRParser: ResToRParser
    ): ResToRClassGenerator {
        return ResToRClassGeneratorImpl(resToRParser, dir)
    }
}