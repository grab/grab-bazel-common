package io.bazel.streems

import java.io.InputStream
import java.io.PrintStream

interface Streams {
    val input: InputStream
    val output: PrintStream
    val error: PrintStream

    class DefaultStreams : Streams {
        override val input: InputStream = System.`in`
        override val output: PrintStream = System.out
        override val error: PrintStream = System.err
    }
}