package com.grab.lint

import io.bazel.Worker

private enum class LintMode {
    ANALYZE {
        override fun call(args: Array<String>) {
            LintAnalyzeCommand().main(args)
        }
    },
    REPORT {
        override fun call(args: Array<String>) {
            LintReportCommand().main(args)
        }
    };

    abstract fun call(args: Array<String>)
}

fun main(args: Array<String>) {
    Worker.create(args) { cliArgs ->
        LintMode
            .valueOf(cliArgs.first())
            .call(cliArgs.drop(1).toTypedArray())
    }.run()
}