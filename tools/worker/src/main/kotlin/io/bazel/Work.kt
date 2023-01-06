package io.bazel

import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.Charset

interface Work {
    fun execute(args: Array<String>): WorkResult
}

class DefaultWork(val action: (args: Array<String>) -> Unit) : Work {

    private val charset: String = Charset.defaultCharset().name()

    override fun execute(args: Array<String>): WorkResult {
        val errorBuffer = ByteArrayOutputStream()
        val errorPrinter = PrintStream(BufferedOutputStream(errorBuffer))
        return try {
            action(args)
            WorkResult.Success()
        } catch (e: Exception) {
            e.printStackTrace(errorPrinter)
            errorPrinter.flush()

            val error = errorBuffer.toString(charset)
            WorkResult.Failure(error)
        } finally {
            errorPrinter.close()
        }
    }
}