package com.grab.maven

import java.io.File

class WorkspaceUpdater(
    private val workspace: File
) {
    private val pinnedMavenInstallAttr = "pinned_maven_install ="
    private val pinBazelCommonArtifacts = "pin_bazel_common_dependencies()"

    fun pin() {
        with(workspace) {
            writeText(
                readText().replace(
                    "$pinnedMavenInstallAttr False",
                    "$pinnedMavenInstallAttr True"
                ).replace(
                    "#+pin_bazel_common_dependencies\\(\\)".toRegex(),
                    pinBazelCommonArtifacts
                )
            )
        }
    }

    fun unpin() {
        with(workspace) {
            writeText(
                readText().replace(
                    "$pinnedMavenInstallAttr True",
                    "$pinnedMavenInstallAttr False"
                ).replace(
                    "$pinBazelCommonArtifacts",
                    "#$pinBazelCommonArtifacts"
                )
            )
        }
    }
}