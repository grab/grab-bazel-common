package com.grab

import io.bazel.Status
import io.bazel.Worker

fun main(args: Array<String>) {
    Worker.from(args = args.toList()).run {
        print(it)
        Status.Success
    }
}