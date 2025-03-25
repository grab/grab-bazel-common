package io.bazel.streems

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.bazel.value.WorkRequest
import io.bazel.value.WorkRequestJsonAdapter
import io.bazel.value.WorkResponse
import io.bazel.value.WorkResponseJsonAdapter
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.FlowableEmitter
import io.reactivex.rxjava3.core.FlowableOnSubscribe
import io.reactivex.rxjava3.functions.Consumer
import okio.BufferedSink
import okio.BufferedSource
import okio.buffer
import okio.sink
import okio.source
import java.io.IOException
import kotlin.IllegalStateException

/**
 *  Implementation adapted from https://github.com/buildfoundation/bazel_rules_detekt/blob/master/detekt/wrapper/src/main/java/io/buildfoundation/bazel/detekt/stream/Streams.java
 */
interface WorkerStreams {
    fun request(): Flowable<WorkRequest>
    fun response(): Consumer<WorkResponse>

    class DefaultWorkerStreams(private val streams: Streams) : WorkerStreams {

        private class WorkRequestSource(val streams: Streams) : FlowableOnSubscribe<WorkRequest> {
            private val requestSource: BufferedSource
            private val requestAdapter: JsonAdapter<WorkRequest>

            init {
                requestSource = streams.input.source().buffer()
                requestAdapter = Moshi.Builder()
                    .build()
                    .adapter(WorkRequest::class.java)
            }

            override fun subscribe(emitter: FlowableEmitter<WorkRequest>) {
                streams.use { io ->
                    io.redirectSystemStreams()
                    while (!emitter.isCancelled) {
                        try {
                            val request = requestAdapter.fromJson(requestSource)
                            if (request == null) {
                                emitter.onComplete()
                            } else {
                                emitter.onNext(request)
                            }
                        } catch (e: IOException) {
                            e.printStackTrace()
                            emitter.onComplete()
                        }
                    }
                }
            }
        }

        private class WorkResponseSink constructor(streams: Streams) :
            Consumer<WorkResponse> {
            private val responseSink: BufferedSink
            private val responseAdapter: JsonAdapter<WorkResponse>

            init {
                responseSink = streams.output.sink().buffer()
                responseAdapter = Moshi.Builder()
                    .build()
                    .adapter(WorkResponse::class.java)
            }

            override fun accept(response: WorkResponse) {
                try {
                    responseAdapter.toJson(responseSink, response)
                    responseSink.flush()
                } catch (ignored: IOException) {
                    ignored.printStackTrace()
                }
            }
        }

        override fun request(): Flowable<WorkRequest> {
            return Flowable.create(
                WorkRequestSource(
                    streams
                ), BackpressureStrategy.BUFFER
            )
        }

        override fun response(): Consumer<WorkResponse> {
            return WorkResponseSink(streams)
        }
    }
}
