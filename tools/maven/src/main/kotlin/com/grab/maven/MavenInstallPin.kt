package com.grab.maven

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.grab.bazel.BazelInvokerFactory
import com.grab.bazel.isError
import com.grab.maven.PinningTarget.Pinned
import java.io.File

class MavenInstallPin : CliktCommand() {

    private val mavenRepoName by option(
        "-m",
        "--maven_repo",
        help = "Name of the maven_install repo to run pinning"
    ).required()

    private val workingDir = System.getenv("BUILD_WORKSPACE_DIRECTORY") ?: error("Missing BUILD_WORKSPACE_DIRECTORY environment")

    private val bazelInvoker = BazelInvokerFactory.create(workingDir = workingDir)

    @Suppress("SameParameterValue")
    private fun file(name: String) = File(workingDir, name)

    override fun run() {
        val workspaceUpdater = WorkspaceUpdater(file("WORKSPACE"))
        println("Unpinning @$mavenRepoName, output:")
        workspaceUpdater.unpin()
        val result = bazelInvoker.run(listOf(Pinned(mavenRepoName).toString()))
        workspaceUpdater.pin()
        if (!result.isError) {
            println("Pinned artifacts successfully!")
        } else {
            println("Error occurred while pinning artifacts")
        }
    }
}

fun main(args: Array<String>) {
    MavenInstallPin().main(args)
}
