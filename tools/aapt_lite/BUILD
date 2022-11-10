load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("@grab_bazel_common//tools/test:test.bzl", "grab_kt_jvm_test")

grab_kt_jvm_test(
    name = "aapt_lite_test",
    srcs = glob([
        "src/test/java/**/*.kt",
    ]),
    deps = [
        "//tools/aapt_lite/src/main/java/com/grab/aapt",
        "@bazel_common_maven//:junit_junit",
        "@com_github_jetbrains_kotlin//:kotlin-test",
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
