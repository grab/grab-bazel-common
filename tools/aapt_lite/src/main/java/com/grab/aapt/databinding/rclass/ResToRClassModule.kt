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

package com.grab.aapt.databinding.rclass

import com.grab.aapt.databinding.di.AaptScope
import com.grab.aapt.databinding.rclass.generator.DefaultResToRClassGenerator
import com.grab.aapt.databinding.rclass.generator.ResToRClassGenerator
import com.grab.aapt.databinding.rclass.parser.DefaultResToRParser
import com.grab.aapt.databinding.rclass.parser.ParserType
import com.grab.aapt.databinding.rclass.parser.ResToRParser
import com.grab.aapt.databinding.rclass.parser.ResourceFileParser
import com.grab.aapt.databinding.rclass.parser.xml.ArrayParser
import com.grab.aapt.databinding.rclass.parser.xml.DeclareStyleableParser
import com.grab.aapt.databinding.rclass.parser.xml.DefaultXmlParser
import com.grab.aapt.databinding.rclass.parser.xml.IDParser
import com.grab.aapt.databinding.rclass.parser.xml.StyleParser
import dagger.Binds
import dagger.MapKey
import dagger.Module
import dagger.multibindings.IntoMap

@MapKey
annotation class ParserKey(val value: ParserType)

@Module
interface ResourceParserModule {
    @Binds
    @IntoMap
    @ParserKey(ParserType.STYLE_PARSER)
    fun styleParser(impl: StyleParser): ResourceFileParser

    @Binds
    @IntoMap
    @ParserKey(ParserType.STYLEABLE_PARSER)
    fun declareStyleableParser(impl: DeclareStyleableParser): ResourceFileParser

    @Binds
    @IntoMap
    @ParserKey(ParserType.ARRAY_PARSER)
    fun arrayParser(impl: ArrayParser): ResourceFileParser

    @Binds
    @IntoMap
    @ParserKey(ParserType.ID_PARSER)
    fun idParser(impl: IDParser): ResourceFileParser

    @Binds
    @IntoMap
    @ParserKey(ParserType.DEFAULT_PARSER)
    fun defaultXmlParser(impl: DefaultXmlParser): ResourceFileParser

    @Binds
    @AaptScope
    fun defaultResToRParser(impl: DefaultResToRParser): ResToRParser

    @Binds
    @AaptScope
    fun resToRClassGenerator(impl: DefaultResToRClassGenerator): ResToRClassGenerator
}