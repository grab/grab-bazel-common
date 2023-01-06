package io.bazel

import org.junit.Test
import kotlin.test.assertTrue

class WorkerTest {

    @Test
    fun `assert worker type is parsed from args`() {
        val worker = Worker.create(arrayOf("--version")) {}
        assertTrue("Default worker is parsed") { worker is SingleShotWorker }
        val persistentWorker = Worker.create(arrayOf("--persistent_worker", "--version", "0.5")) {}
        assertTrue("Persistent worker is parsed") { persistentWorker is PersistentWorker }
    }
}