package io.bazel.value

import com.squareup.moshi.Json

data class WorkResponse(
    @Json(name = "exitCode")
    var exitCode: Int = 0,
    @Json(name = "output")
    var output: String? = null,
    @Json(name = "requestId")
    var requestId: Int = 0,
)