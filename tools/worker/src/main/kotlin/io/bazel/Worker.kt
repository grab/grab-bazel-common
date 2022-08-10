package io.bazel

import com.google.devtools.build.lib.worker.WorkerProtocol.WorkRequest
import io.bazel.Worker.Companion.parseArgs
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.PrintStream
import java.io.PrintWriter
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.FileSystems
import java.nio.file.Files
import java.time.Duration
import kotlin.system.exitProcess

enum class Status(val exit: Int) {
    Success(0),
    Failure(1)
}

/**
 * Worker encapsulation to make it easier to run tools in either normal or persistent execution (including
 * multiplexing) modes.
 * In either cases [Worker.run] will be called with args parsed based on execution mode.
 *
 * Usage
 * ```kotlin
 * fun main(args: Array<String>) {
 *   Worker.from(args.toList()).run { args ->
 *     // Implementation
 *     Status.Success
 *   }
 * }
 * ```
 */
fun interface Worker {
    companion object {
        private const val PERSISTENT_WORKER = "--persistent_worker"
        private val FLAGFILE_REGEX = Regex("""^--flagfile=((.*)-(\d+).params)$""")

        fun parseArgs(args: List<String>): List<String>? {
            return FLAGFILE_REGEX.matchEntire(args.first())?.groups?.get(1)?.let {
                Files.readAllLines(FileSystems.getDefault().getPath(it.value), UTF_8)
            }
        }

        fun from(args: List<String>): Worker = when (PERSISTENT_WORKER) {
            in args -> PersistentWorker()
            else -> DefaultWorker(parseArgs(args.filter { it != PERSISTENT_WORKER }) ?: args)
        }
    }

    fun run(action: (args: List<String>) -> Status)
}

/**
 * Default worker implementation that simply executes the action
 */
class DefaultWorker(val args: List<String>) : Worker {
    override fun run(
        action: (args: List<String>) -> Status
    ) = exitProcess(action(args).exit)
}

/**
 * Persistent multiplexing worker implementation that delegates to [WorkRequestHandler] for handling
 * work requests and responses.
 */
class PersistentWorker : Worker {
    override fun run(action: (args: List<String>) -> Status) {
        val buf = ByteArrayOutputStream()
        val ps = PrintStream(buf, true)
        val realStdOut: PrintStream = System.out
        val realStdErr: PrintStream = System.err

        // Redirect all stdout and stderr output for logging.
        System.setOut(ps)
        System.setErr(ps)

        try {
            val workerHandler = WorkRequestHandler.WorkRequestHandlerBuilder(
                WorkRequestHandler.WorkRequestCallback { request: WorkRequest, pw: PrintWriter ->
                    processRequest(request.argumentsList, pw, buf, action)
                },
                realStdErr,
                ProtoWorkerMessageProcessor(System.`in`, realStdOut)
            ).setCpuUsageBeforeGc(Duration.ofSeconds(10)).build()
            workerHandler.processRequests()
        } catch (e: IOException) {
            e.printStackTrace(realStdErr)
            exitProcess(1)
        } finally {
            System.setOut(realStdOut)
            System.setErr(realStdErr)
        }
        exitProcess(0)
    }

    /**
     * Processes the request for the given args and writes the captured byte array buffer to the
     * WorkRequestHandler print writer.
     */
    private fun processRequest(
        args: List<String>,
        pw: PrintWriter,
        buf: ByteArrayOutputStream,
        action: (args: List<String>) -> Status
    ): Int {
        val exitCode = try {
            // Parse args from flagfile
            val actualArgs = parseArgs(args) ?: args
            // Process the actual request and grab the exit code
            action(actualArgs).exit
        } catch (e: Exception) {
            e.printStackTrace(pw)
            1
        } finally {
            // Write the captured buffer to the work response. We synchronize to avoid race conditions
            // while reading from and calling reset on the shared ByteArrayOutputStream.
            synchronized(buf) {
                val captured = buf.toString(UTF_8.name()).trim { it <= ' ' }
                buf.reset()
                pw.print(captured)
            }
        }
        return exitCode
    }
}