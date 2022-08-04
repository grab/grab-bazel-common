package io.bazel

import org.junit.Test
import kotlin.test.assertTrue


class WorkerTest {

    @Test
    fun `assert worker type is parsed from args`() {
        val worker = Worker.from(listOf("--version"))
        assertTrue("Default worker is parsed") { worker is DefaultWorker }
        val persistentWorker = Worker.from(listOf("--persistent_worker", "--version", "0.5"))
        assertTrue("Persistent worker is parsed") { persistentWorker is PersistentWorker }
    }
}