package com.grab.lint

import java.io.File

data class Dependency(
    val name: String,
    val android: Boolean,
    val library: Boolean,
    val partialDir: File,
)