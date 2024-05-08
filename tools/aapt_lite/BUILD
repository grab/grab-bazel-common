load("@grab_bazel_common//rules:defs.bzl", "kotlin_test")

kotlin_test(
    name = "aapt_lite_test",
    srcs = glob([
        "src/test/java/**/*.kt",
    ]),
    deps = [
        "//tools/aapt_lite/src/main/java/com/grab/aapt",
        "//tools/test_utils",
        "@bazel_common_maven//:junit_junit",
        "@maven//:org_jetbrains_kotlin_kotlin_test",
    ],
)

java_binary(
    name = "aapt_lite",
    main_class = "com.grab.aapt.MainKt",
    visibility = [
        "//visibility:public",
    ],
    runtime_deps = [
        "//tools/aapt_lite/src/main/java/com/grab/aapt",
    ],
)
