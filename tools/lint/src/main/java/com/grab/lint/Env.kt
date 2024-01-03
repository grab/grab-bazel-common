package com.grab.lint

interface Env {
    val tmpDir: String
    val pwd: String

    object BazelEnv : Env {
        override val tmpDir: String get() = System.getenv("TMP_DIR") ?: ""
        override val pwd: String get() = System.getenv("PWD") ?: "  "
    }
}