package io.bazel

import io.bazel.streems.Streams
import io.bazel.streems.WorkerStreams
import io.bazel.value.WorkRequest
import io.bazel.value.WorkResponse
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlin.system.exitProcess
import java.nio.file.Files
import java.nio.file.FileSystems
import java.nio.charset.StandardCharsets

const val PERSISTENT_WORKER = "--persistent_worker"

interface Worker {
    fun run()

    companion object {
        fun create(args: Array<String>, action: (args: Array<String>) -> Unit): Worker {

            val work: Work = DefaultWork(action)
            val streams: Streams = Streams.DefaultStreams()

            return if (args.contains(PERSISTENT_WORKER)) {
                val workerStreams: WorkerStreams = WorkerStreams.DefaultWorkerStreams(streams)
                PersistentWorker(work, workerStreams, Schedulers.io())
            } else {
                SingleShotWorker(
                    work,
                    streams,
                    args
                )
            }
        }
    }
}

private val FLAG_FILE_REGEX = Regex("""^--flagfile=((.*)-(\d+).params)$""")

private fun commandLineArgs(args: Array<String>): Array<String> {
    return FLAG_FILE_REGEX.matchEntire(args.first())?.groups?.get(1)?.let {
        Files.readAllLines(FileSystems.getDefault().getPath(it.value), StandardCharsets.UTF_8)
    }?.toTypedArray() ?: arrayOf()
}

class PersistentWorker(
    private val work: Work,
    private val streams: WorkerStreams,
    private val scheduler: Scheduler
) : Worker {

    override fun run() {
        streams.request()
            .subscribeOn(scheduler)
            .parallel()
            .runOn(scheduler)
            .map { request: WorkRequest ->
                val workResult = work.execute(commandLineArgs(request.arguments!!))
                WorkResponse(workResult.statusCode, workResult.output, request.requestId)
            }
            .sequential()
            .observeOn(scheduler)
            .blockingSubscribe(streams.response())
    }
}

class SingleShotWorker(
    private val work: Work,
    private val streams: Streams,
    private val args: Array<String>
) : Worker {
    override fun run() {
        val result = work.execute(commandLineArgs(args))
        if (result is WorkResult.Failure) {
            streams.error.println(result.output)
        }
        exitProcess(result.statusCode)
    }
}