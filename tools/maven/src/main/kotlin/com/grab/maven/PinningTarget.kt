package com.grab.maven

sealed class PinningTarget(open val mavenRepoName: String) {

    data class Unpinned(
        override val mavenRepoName: String
    ) : PinningTarget(mavenRepoName) {
        override fun toString() = "@unpinned_${mavenRepoName}//:pin"
    }

    data class Pinned(
        override val mavenRepoName: String
    ) : PinningTarget(mavenRepoName) {
        override fun toString() = "@${mavenRepoName}//:pin"
    }
}