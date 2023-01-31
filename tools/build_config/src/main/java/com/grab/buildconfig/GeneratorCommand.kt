/*
 * Copyright (c) 2012-2023 Grab Taxi Holdings PTE LTD (GRAB), All Rights Reserved. NOTICE: All information contained herein is, and remains the property of GRAB. The intellectual and technical concepts contained herein are confidential, proprietary and controlled by GRAB and may be covered by patents, patents in process, and are protected by trade secret or copyright law.
 * You are strictly forbidden to copy, download, store (in any medium), transmit, disseminate, adapt or change this material in any way unless prior written permission is obtained from GRAB. Access to the source code contained herein is hereby forbidden to anyone except current GRAB employees or contractors with binding Confidentiality and Non-disclosure agreements explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication or disclosure of this source code, which includes information that is confidential and/or proprietary, and is a trade secret, of GRAB.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC PERFORMANCE, OR PUBLIC DISPLAY OF OR THROUGH USE OF THIS SOURCE CODE WITHOUT THE EXPRESS WRITTEN CONSENT OF GRAB IS STRICTLY PROHIBITED, AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES. THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS, OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT MAY DESCRIBE, IN WHOLE OR IN PART.
 */

package com.grab.buildconfig

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split
import java.io.File

class GeneratorCommand : CliktCommand() {

    private val packageName: String by option(
        "-p",
        "--package",
        help = "Package name of BuildConfig class"
    ).required()

    private val strings: List<String> by option(
        "-s",
        "--strings",
        help = "List of name=value to be added as String type fields",
    ).split(",").default(emptyList())

    private val booleans: List<String> by option(
        "-b",
        "--booleans",
        help = "List of name=value to be added as boolean type fields",
    ).split(",").default(emptyList())

    private val ints: List<String> by option(
        "-i",
        "--ints",
        help = "List of name=value to be added as int type fields",
    ).split(",").default(emptyList())

    private val longs: List<String> by option(
        "-l",
        "--longs",
        help = "List of name=value to be added as long type fields",
    ).split(",").default(emptyList())

    private val output by option(
        "-o",
        "--output",
        help = "The output directory that the generated BuildConfig class will be written to. " +
                "Will write to standard output if option not present",
    ).convert { File(it) }

    override fun run() {
        BuildConfigGenerator().generate(
            packageName = packageName,
            output = output,
            strings = strings,
            booleans = booleans,
            ints = ints,
            longs = longs,
        )
    }
}