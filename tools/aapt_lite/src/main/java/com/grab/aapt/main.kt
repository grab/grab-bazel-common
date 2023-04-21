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

import com.grab.aapt.databinding.mapper.GenerateMapperCommand
import com.google.devtools.build.android.ResourceMergerCommand
import io.bazel.Worker

enum class Tool {
    AAPT_LITE {
        override fun call(args: Array<String>) {
            AaptLiteCommand().main(args)
        }
    },
    DATABINDING_MAPPER {
        override fun call(args: Array<String>) {
            GenerateMapperCommand().main(args)
        }
    },
    RESOURCE_MERGER {
        override fun call(args: Array<String>) {
            ResourceMergerCommand().main(args)
        }
    };

    abstract fun call(args: Array<String>)
}

fun main(args: Array<String>) {
    Worker.create(args) { cliArgs ->
        Tool.valueOf(cliArgs.first()).call(cliArgs.drop(1).toTypedArray())
    }.run()
}
