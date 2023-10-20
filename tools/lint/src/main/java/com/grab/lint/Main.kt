package com.grab.lint

import io.bazel.Worker

fun main(args: Array<String>) {
    Worker.create(args) { cliArgs ->
        LintCommand().main(cliArgs)
    }.run()
}