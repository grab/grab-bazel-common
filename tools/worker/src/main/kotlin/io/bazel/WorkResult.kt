package io.bazel

sealed class WorkResult(val output: String, val statusCode: Int) {
    class Success : WorkResult("", 0)
    class Failure(output: String) : WorkResult(output, 1)
}
