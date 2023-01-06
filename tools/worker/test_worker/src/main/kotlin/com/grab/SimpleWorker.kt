package com.grab

import io.bazel.Worker

fun main(args: Array<String>) {
    Worker.create(args = args) {
        print(it.contentToString())
    }.run()
}