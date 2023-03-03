package com.grab.aapt.databinding.util

import java.io.Closeable
import java.nio.file.Files
import java.nio.file.Path

class WorkingDirectory(
    val dir: Path = Files.createTempDirectory("tmp")
) : Closeable {
    override fun close() {
        Files.walk(dir).sorted(Comparator.reverseOrder()).forEach(Files::delete)
    }
}