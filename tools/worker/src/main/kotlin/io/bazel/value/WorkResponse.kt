package io.bazel.value

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WorkResponse(
    var exitCode: Int = 0,
    var output: String? = null,
    var requestId: Int = 0,
)
