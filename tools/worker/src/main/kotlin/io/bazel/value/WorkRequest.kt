package io.bazel.value

import com.squareup.moshi.Json

data class WorkRequest(
    @Json(name = "arguments")
    var arguments: Array<String>? = null,

    @Json(name = "requestId")
    var requestId: Int = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WorkRequest

        if (arguments != null) {
            if (other.arguments == null) return false
            if (!arguments.contentEquals(other.arguments)) return false
        } else if (other.arguments != null) return false
        if (requestId != other.requestId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = arguments?.contentHashCode() ?: 0
        result = 31 * result + requestId
        return result
    }
}