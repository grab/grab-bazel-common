package com.grab.bazel

import java.io.File

interface BazelInvoker {
    val workingDir: String
    val bazelBinary: String

    fun build(targets: List<String>, vararg options: String): BazelResult

    fun run(targets: List<String>, vararg options: String): BazelResult

    operator fun invoke(command: () -> List<String>): BazelResult
}

data class BazelResult(
    val exitCode: Int,
    val logs: String
)

val BazelResult.isError get() = exitCode == 1

const val BAZELISK = "bazelisk"

object BazelInvokerFactory {
    fun create(
        workingDir: String,
        bazelBinary: String = BAZELISK
    ): BazelInvoker = DefaultBazelInvoker(workingDir, bazelBinary)
}

class DefaultBazelInvoker(
    override val workingDir: String,
    override val bazelBinary: String
) : BazelInvoker {

    private fun bazelCommand(command: List<String>): BazelResult {
        val process = ProcessBuilder()
            .directory(File(workingDir))
            .command(listOf(bazelBinary) + command)
            .redirectErrorStream(true)
            .start()
        val output = buildString {
            process.inputStream
                .reader()
                .forEachLine { line ->
                    appendLine(line)
                    println(line)
                }
        }
        process.waitFor()
        return BazelResult(
            exitCode = process.exitValue(),
            logs = output
        )
    }

    override fun build(
        targets: List<String>,
        vararg options: String
    ) = bazelCommand(listOf("build") + targets + options)

    override fun run(
        targets: List<String>,
        vararg options: String
    ) = bazelCommand(listOf("run") + targets + options)

    override fun invoke(
        command: () -> List<String>
    ) = bazelCommand(command())
}