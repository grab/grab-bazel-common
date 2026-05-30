load("@grab_bazel_common//rules:defs.bzl", "kotlin_test")

kotlin_test(
    name = "aapt_lite_test",
    srcs = glob([
        "src/test/java/**/*.kt",
    ]),
    deps = [
        "//tools/aapt_lite/src/main/java/com/grab/aapt",
        "//tools/aapt_lite/src/main/java/com/google/devtools/build/android:resource",
        "//tools/aapt_lite/src/main/java/com/google/devtools/build/android:source_set",
        "//tools/aapt_lite/src/main/java/com/grab/aapt/databinding/binding",
        "//tools/aapt_lite/src/main/java/com/grab/aapt/databinding/binding/model",
        "//tools/aapt_lite/src/main/java/com/grab/aapt/databinding/brclass",
        "//tools/aapt_lite/src/main/java/com/grab/aapt/databinding/common",
        "//tools/aapt_lite/src/main/java/com/grab/aapt/databinding/mapper",
        "//tools/aapt_lite/src/main/java/com/grab/aapt/databinding/rclass",
        "//tools/aapt_lite/src/main/java/com/grab/aapt/databinding/util",
        "//tools/test_utils",
        "@bazel_common_maven//:com_github_ajalt_clikt",
        "@bazel_common_maven//:com_squareup_javapoet",
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
